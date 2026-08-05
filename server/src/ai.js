/**
 * KI-Proxy mit Key-Rotation.
 *
 * Der entscheidende Unterschied zur Browser-Variante: Die API-Keys liegen
 * ausschließlich hier auf dem Server. Der Browser sieht sie nie, kann sie
 * also weder auslesen noch missbrauchen.
 *
 * Mehrere Keys werden reihum genutzt. Antwortet ein Key mit 429 (Rate-Limit)
 * oder 403/402 (Kontingent erschöpft), pausiert er und der nächste übernimmt.
 */
import { config } from "./config.js";

/* ----------------------------- Key-Pool -------------------------------- */
const cooldowns = new Map();      // Key -> Zeitpunkt, ab dem er wieder nutzbar ist
const cursors = new Map();        // Provider -> Rundlauf-Zeiger

function keysFor(provider) {
  if (provider === "gemini") return config.ai.geminiKeys;
  if (provider === "anthropic") return config.ai.anthropicKeys;
  return [];
}

function label(key, index) {
  return `#${index + 1}:${String(key).slice(0, 6)}`;
}

function pickKey(provider) {
  const keys = keysFor(provider);
  if (!keys.length) return null;
  const now = Date.now();
  const ready = keys.filter((k) => (cooldowns.get(k) || 0) <= now);
  const pool = ready.length ? ready : keys;   // alle pausiert? Dann trotzdem versuchen
  const cursor = cursors.get(provider) || 0;
  const key = pool[cursor % pool.length];
  cursors.set(provider, (cursor + 1) % pool.length);
  return { key, index: keys.indexOf(key) };
}

function coolDown(key, seconds) {
  cooldowns.set(key, Date.now() + seconds * 1000);
}

/** Zustand aller Keys — für das Admin-Dashboard. */
export function poolStatus() {
  const now = Date.now();
  const build = (provider) =>
    keysFor(provider).map((k, i) => ({
      label: label(k, i),
      cooling: (cooldowns.get(k) || 0) > now,
      secondsLeft: Math.max(0, Math.ceil(((cooldowns.get(k) || 0) - now) / 1000)),
    }));
  return {
    provider: config.ai.provider,
    gemini: build("gemini"),
    anthropic: build("anthropic"),
    ollama: config.ai.provider === "ollama" ? { url: config.ai.ollamaUrl, model: config.ai.ollamaModel } : null,
  };
}

export function aiAvailable() {
  const p = config.ai.provider;
  if (p === "ollama") return true;
  return keysFor(p).length > 0;
}

/* --------------------------- Anbieter-Aufrufe --------------------------- */
class ProviderError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function callGemini(key, system, user, maxTokens) {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${config.ai.geminiModel}:generateContent?key=${encodeURIComponent(key)}`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: system }] },
      contents: [{ role: "user", parts: [{ text: user }] }],
      generationConfig: { maxOutputTokens: maxTokens, temperature: 0.3 },
    }),
    signal: AbortSignal.timeout(config.ai.requestTimeoutMs),
  });
  if (!res.ok) throw new ProviderError(`Gemini ${res.status}`, res.status);
  const data = await res.json();
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) throw new ProviderError("Gemini lieferte keine Antwort", 502);
  return text;
}

async function callAnthropic(key, system, user, maxTokens) {
  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "anthropic-version": "2023-06-01",
      "x-api-key": key,
    },
    body: JSON.stringify({
      model: config.ai.anthropicModel,
      max_tokens: maxTokens,
      system,
      messages: [{ role: "user", content: user }],
    }),
    signal: AbortSignal.timeout(config.ai.requestTimeoutMs),
  });
  if (!res.ok) throw new ProviderError(`Anthropic ${res.status}`, res.status);
  const data = await res.json();
  const text = data?.content?.[0]?.text;
  if (!text) throw new ProviderError("Anthropic lieferte keine Antwort", 502);
  return text;
}

async function callOllama(system, user, maxTokens) {
  const res = await fetch(`${config.ai.ollamaUrl}/api/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      model: config.ai.ollamaModel,
      stream: false,
      options: { temperature: 0.3, num_predict: maxTokens },
      messages: [
        { role: "system", content: system },
        { role: "user", content: user },
      ],
    }),
    signal: AbortSignal.timeout(config.ai.requestTimeoutMs),
  });
  if (!res.ok) throw new ProviderError(`Ollama ${res.status}`, res.status);
  const data = await res.json();
  const text = data?.message?.content;
  if (!text) throw new ProviderError("Ollama lieferte keine Antwort", 502);
  return text;
}

/**
 * Führt einen Aufruf aus und wechselt bei Limits automatisch den Key.
 * Gibt zusätzlich zurück, welcher Key genutzt wurde (für die Statistik).
 */
export async function generate({ system, user, maxTokens = 1000 }) {
  const provider = config.ai.provider;
  const started = Date.now();

  if (provider === "ollama") {
    const text = await callOllama(system, user, maxTokens);
    return { text, provider, keyLabel: "ollama", durationMs: Date.now() - started };
  }

  const keys = keysFor(provider);
  if (!keys.length) throw new ProviderError("Kein API-Key konfiguriert", 503);

  let lastError;
  const attempts = Math.min(keys.length, 5);
  for (let i = 0; i < attempts; i++) {
    const picked = pickKey(provider);
    if (!picked) break;
    try {
      const text = provider === "gemini"
        ? await callGemini(picked.key, system, user, maxTokens)
        : await callAnthropic(picked.key, system, user, maxTokens);
      return { text, provider, keyLabel: label(picked.key, picked.index), durationMs: Date.now() - started };
    } catch (e) {
      lastError = e;
      if (e.status === 429) coolDown(picked.key, config.ai.rateLimitCooldownSec);
      else if (e.status === 403 || e.status === 402) coolDown(picked.key, config.ai.quotaCooldownSec);
      else break;  // andere Fehler betreffen alle Keys gleichermaßen
    }
  }
  throw lastError || new ProviderError("KI-Aufruf fehlgeschlagen", 502);
}

/* ------------------------------- Prompts -------------------------------- */
export function parseJsonAnswer(text) {
  const cleaned = String(text).replace(/```json|```/g, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  return JSON.parse(start >= 0 && end > start ? cleaned.slice(start, end + 1) : cleaned);
}

export const CHECK_SYSTEM_PROMPT = `Du bist ein freundlicher aber präziser Programmier-Lehrer.
Du bewertest Antworten von Schülern die Programmieren lernen.

DEINE AUFGABE:
- Analysiere die Antwort auf Korrektheit
- Gib konstruktives, ermutigendes Feedback
- Erkläre was richtig/falsch ist
- Bei Code: Prüfe ob das Konzept verstanden wurde, nicht nur syntaktische Korrektheit
- Halte die Antwort kurz und klar (max 3-4 Sätze)
- Antworte IMMER auf Deutsch
- Fang nie mit "Ich" an

ANTWORTE NUR IN DIESEM JSON FORMAT (keine anderen Zeichen davor oder danach):
{
  "correct": true/false,
  "score": 0-100,
  "feedback": "Dein Feedback hier",
  "hint": "Optional: Tipp falls falsch",
  "praise": "Kurzes Lob falls richtig"
}`;

export const DEBUG_SYSTEM_PROMPT = `Du bist ein erfahrener Web-Entwickler und hilfst beim Debuggen von HTML/CSS/JavaScript.

DEINE AUFGABE:
- Finde echte Fehler (Syntax, Logik, häufige Stolperfallen)
- Erkläre jeden Fund kurz und verständlich auf Deutsch
- Schlage eine konkrete Lösung vor
- Wenn alles in Ordnung ist, sag das ehrlich und gib höchstens Verbesserungstipps

ANTWORTE NUR IN DIESEM JSON FORMAT (keine anderen Zeichen davor oder danach):
{
  "summary": "Kurze Gesamteinschätzung in 1-2 Sätzen",
  "issues": [
    { "severity": "error"|"warning"|"info", "where": "html"|"css"|"js", "title": "Kurzer Titel", "detail": "Erklärung", "fix": "Konkreter Lösungsvorschlag" }
  ]
}`;

export function buildCheckPrompt({ language, lessonTitle, question, expectedConcepts, answer }) {
  return `Sprache: ${language}
Lektion: ${lessonTitle}
Aufgabe: ${question}
${expectedConcepts?.length ? `Erwartete Konzepte: ${expectedConcepts.join(", ")}` : ""}
Schüler-Antwort: ${answer}

Bitte bewerte diese Antwort.`;
}

export function buildDebugPrompt({ html, css, js }) {
  return `HTML:\n${html || "(leer)"}\n\nCSS:\n${css || "(leer)"}\n\nJavaScript:\n${js || "(leer)"}\n\nBitte analysiere diesen Code.`;
}
