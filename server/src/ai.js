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
  if (provider === "openrouter") return config.ai.openrouterKeys;
  if (provider === "groq") return config.ai.groqKeys;
  if (provider === "cerebras") return config.ai.cerebrasKeys;
  if (provider === "nvidia") return config.ai.nvidiaKeys;
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
    openrouter: build("openrouter"),
    openrouterModel: config.ai.openrouterModel || null,
    groq: build("groq"),
    groqModel: config.ai.groqModel || null,
    cerebras: build("cerebras"),
    cerebrasModel: config.ai.cerebrasModel || null,
    nvidia: build("nvidia"),
    nvidiaModel: config.ai.nvidiaModel || null,
    ollama: ollamaInUse()
      ? { url: config.ai.ollamaUrl, model: config.ai.ollamaModel, reachable: ollamaErreichbar }
      : null,
  };
}

export function aiAvailable() {
  return providerReady(config.ai.provider);
}

/* --------------------------- Anbieter-Aufrufe --------------------------- */
class ProviderError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

/**
 * Google (Gemini und Gemma).
 *
 * Ein Stolperstein, der viel Zeit kosten kann: Nicht jedes Modell hinter
 * dieser Schnittstelle nimmt eine getrennte Systemanweisung entgegen. Wo das
 * nicht geht, kommt ein 400 mit „system_instruction is not enabled" oder
 * „Developer instruction is not enabled" zurück.
 *
 * Statt das zu erraten, wird es ausprobiert: erst mit Systemanweisung, und
 * wenn genau daran scheitert, noch einmal mit der Anweisung vorne im Text.
 * Das Ergebnis ist dasselbe, und es funktioniert mit jedem Modell.
 */
async function callGemini(key, system, user, maxTokens, model) {
  const name = model || config.ai.geminiModel;
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${name}:generateContent?key=${encodeURIComponent(key)}`;

  const anfrage = (mitSystem) => ({
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      ...(mitSystem ? { systemInstruction: { parts: [{ text: system }] } } : {}),
      contents: [{
        role: "user",
        parts: [{ text: mitSystem ? user : `${system}\n\n---\n\n${user}` }],
      }],
      generationConfig: { maxOutputTokens: maxTokens, temperature: 0.3 },
    }),
    signal: AbortSignal.timeout(config.ai.requestTimeoutMs),
  });

  let res = await fetch(url, anfrage(true));
  if (res.status === 400) {
    const grund = await res.text().catch(() => "");
    if (/system.?instruction|developer instruction/i.test(grund)) {
      // Dieses Modell kennt keine getrennte Systemanweisung — zweiter Anlauf.
      res = await fetch(url, anfrage(false));
    } else {
      throw new ProviderError(`Google 400: ${kurz(grund) || "Anfrage abgelehnt"}`, 400);
    }
  }
  if (!res.ok) {
    const grund = await res.text().catch(() => "");
    throw new ProviderError(`Google ${res.status}${grund ? `: ${kurz(grund)}` : ""}`, res.status);
  }
  const data = await res.json();
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) {
    const grund = data?.candidates?.[0]?.finishReason;
    throw new ProviderError(`Google lieferte keine Antwort${grund ? ` (${grund})` : ""}`, 502);
  }
  return text;
}

/** Die Fehlermeldung des Anbieters auf das Wesentliche kürzen. */
function kurz(rohtext) {
  try {
    const daten = JSON.parse(rohtext);
    return String(daten?.error?.message || "").slice(0, 200);
  } catch (e) {
    return String(rohtext).replace(/\s+/g, " ").slice(0, 200);
  }
}

/**
 * Listet die Modelle, die dieser Schlüssel tatsächlich benutzen darf.
 *
 * Das ist bei Problemen die einzige verlässliche Auskunft: Was hier nicht
 * steht, gibt es für diesen Zugang nicht — egal was in irgendeiner Liste
 * behauptet wird.
 */
export async function listGoogleModels(key) {
  const res = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models?pageSize=200&key=${encodeURIComponent(key)}`,
    { signal: AbortSignal.timeout(config.ai.requestTimeoutMs) }
  );
  if (!res.ok) {
    const grund = await res.text().catch(() => "");
    throw new ProviderError(`Google ${res.status}${grund ? `: ${kurz(grund)}` : ""}`, res.status);
  }
  const daten = await res.json();
  return (daten.models || [])
    .filter((m) => (m.supportedGenerationMethods || []).includes("generateContent"))
    .map((m) => ({
      id: String(m.name || "").replace(/^models\//, ""),
      label: m.displayName || "",
      input: m.inputTokenLimit || 0,
      output: m.outputTokenLimit || 0,
    }));
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

/* ------------------ Anbieter mit OpenAI-kompatibler Schnittstelle ---------
   OpenRouter, Groq und Cerebras sprechen alle dasselbe Protokoll. Der Aufruf
   stand dreimal fast gleich im Code; jetzt einmal, mit den Unterschieden als
   Angabe.

   Ein Punkt gehört erklärt: `gpt-oss` bei Cerebras ist ein Modell, das laut
   denkt. Sein Gedankengang kommt in einem EIGENEN Feld zurück (`reasoning`),
   nicht im Inhalt. Gelesen wird deshalb ausschließlich `content` — sonst
   stünde im Editor wieder das Protokoll statt der Antwort.
   ------------------------------------------------------------------------ */
const OPENAI_KOMPATIBEL = {
  openrouter: {
    label: "OpenRouter",
    url: "https://openrouter.ai/api/v1/chat/completions",
    modell: () => config.ai.openrouterModel,
    // OpenRouter nutzt beides für die Zuordnung im Konto — rein optional.
    kopf: () => ({ "HTTP-Referer": config.publicUrl, "X-Title": "LearnDeveloping" }),
  },
  groq: {
    label: "Groq",
    url: "https://api.groq.com/openai/v1/chat/completions",
    modell: () => config.ai.groqModel,
  },
  nvidia: {
    label: "NVIDIA NIM",
    url: "https://integrate.api.nvidia.com/v1/chat/completions",
    modell: () => config.ai.nvidiaModel,
  },
  cerebras: {
    label: "Cerebras",
    url: "https://api.cerebras.ai/v1/chat/completions",
    modell: () => config.ai.cerebrasModel,
    /* gpt-oss denkt so ausführlich, wie man es einstellt. Für einen
       Assistenten im Editor ist „low" richtig: Die Antwort kommt schneller,
       und es wird weniger Kontingent für Nachdenken verbraucht, das ohnehin
       niemand zu sehen bekommt. */
    zusatz: () => ({ reasoning_effort: config.ai.cerebrasReasoning }),
  },
};

async function callOpenAiKompatibel(anbieter, key, system, user, maxTokens, model) {
  const cfg = OPENAI_KOMPATIBEL[anbieter];
  const res = await fetch(cfg.url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${key}`,
      ...(cfg.kopf ? cfg.kopf() : {}),
    },
    body: JSON.stringify({
      model: model || cfg.modell(),
      max_tokens: maxTokens,
      temperature: 0.3,
      messages: [
        { role: "system", content: system },
        { role: "user", content: user },
      ],
      ...(cfg.zusatz ? cfg.zusatz() : {}),
    }),
    signal: AbortSignal.timeout(config.ai.requestTimeoutMs),
  });
  if (!res.ok) {
    const grund = await res.text().catch(() => "");
    throw new ProviderError(`${cfg.label} ${res.status}${grund ? `: ${kurz(grund)}` : ""}`, res.status);
  }
  const data = await res.json();
  // Manche Modelle melden einen Fehler mit Status 200 im Rumpf.
  if (data?.error) {
    throw new ProviderError(`${cfg.label}: ${data.error.message || "Fehler"}`, data.error.code || 502);
  }
  // Bewusst nur `content` — ein etwaiges `reasoning` bleibt liegen.
  const text = data?.choices?.[0]?.message?.content;
  if (!text) throw new ProviderError(`${cfg.label} lieferte keine Antwort`, 502);
  return text;
}

async function callOllama(system, user, maxTokens, model) {
  const res = await fetch(`${config.ai.ollamaUrl}/api/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      model: model || config.ai.ollamaModel,
      stream: false,
      // Auf schwacher Hardware zählt jede Einsparung:
      // keep_alive hält das Modell geladen (sonst kostet jeder Aufruf das
      // erneute Einlesen von mehreren hundert MB), num_predict begrenzt die
      // Antwortlänge, num_ctx hält den Kontext klein.
      keep_alive: config.ai.ollamaKeepAlive,
      options: {
        temperature: 0.3,
        num_predict: Math.min(maxTokens, config.ai.ollamaMaxTokens),
        num_ctx: config.ai.ollamaContext,
        num_thread: config.ai.ollamaThreads || undefined,
      },
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
 * Lädt das Modell vorab in den Speicher, damit die erste echte Anfrage nicht
 * auf das Einlesen warten muss. Fehler werden bewusst ignoriert — läuft kein
 * Ollama, ist das kein Grund den Serverstart abzubrechen.
 */
/**
 * Kurzer Blick, ob Ollama überhaupt antwortet — ohne ein Modell zu laden.
 * Wird genutzt, wenn das Vorladen abgeschaltet ist.
 */
export async function checkOllama(log) {
  try {
    const res = await fetch(`${config.ai.ollamaUrl}/api/tags`, { signal: AbortSignal.timeout(3000) });
    setOllamaReachable(res.ok);
    if (res.ok) log?.info(`Ollama erreichbar unter ${config.ai.ollamaUrl}`);
    else log?.warn(`Ollama antwortet mit ${res.status} — die Bewertung läuft rein lokal weiter`);
  } catch (e) {
    setOllamaReachable(false);
    log?.warn(`Ollama nicht erreichbar (${e.message}) — die Bewertung läuft rein lokal weiter`);
  }
}

/** Wird Ollama irgendwo gebraucht — als allgemeiner Anbieter oder in einer Rolle? */
export function ollamaInUse() {
  const r = config.ai.roles;
  /* Der Disponent gehört ausdrücklich dazu. Fehlte er hier, würde beim Start
     nie geprüft, ob Ollama überhaupt läuft — und `providerReady("ollama")`
     meldete „bereit", weil unbekannt nicht als „aus" gilt. Der Status hätte
     dann einen Disponenten angezeigt, den es gar nicht gibt. */
  return [config.ai.provider, r.assist.provider, r.assistPro.provider,
    r.dispatch.enabled ? r.dispatch.provider : null,
    config.ai.verify.primaryProvider, config.ai.verify.fallbackProvider].includes("ollama");
}

export async function warmUpOllama(log) {
  if (!ollamaInUse()) return;
  try {
    await fetch(`${config.ai.ollamaUrl}/api/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        model: config.ai.ollamaModel,
        stream: false,
        keep_alive: config.ai.ollamaKeepAlive,
        options: { num_predict: 1 },
        messages: [{ role: "user", content: "ok" }],
      }),
      signal: AbortSignal.timeout(120000),
    });
    setOllamaReachable(true);
    log?.info(`Ollama-Modell ${config.ai.ollamaModel} ist geladen`);
  } catch (e) {
    // Wichtig: Ab jetzt gilt Ollama als nicht bereit. Sonst würde bei jeder
    // offenen Aufgabe erst die volle Zeitüberschreitung abgewartet.
    setOllamaReachable(false);
    log?.warn(`Ollama nicht erreichbar (${e.message}) — die Bewertung läuft rein lokal weiter`);
  }
}

/**
 * Führt einen Aufruf aus und wechselt bei Limits automatisch den Key.
 * Gibt zusätzlich zurück, welcher Key genutzt wurde (für die Statistik).
 */
export async function generate({ system, user, maxTokens = 1000, provider: forced, model }) {
  const provider = forced || config.ai.provider;
  const started = Date.now();

  if (provider === "ollama") {
    try {
      const text = await callOllama(system, user, maxTokens, model);
      setOllamaReachable(true);
      return { text, provider, model: model || config.ai.ollamaModel, keyLabel: "ollama", durationMs: Date.now() - started };
    } catch (e) {
      // Nicht erreichbar heißt: beim nächsten Mal gar nicht erst fragen.
      if (/fetch failed|ECONNREFUSED|ENOTFOUND|timeout|aborted/i.test(String(e.message))) {
        setOllamaReachable(false);
      }
      throw e;
    }
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
        ? await callGemini(picked.key, system, user, maxTokens, model)
        : OPENAI_KOMPATIBEL[provider]
        ? await callOpenAiKompatibel(provider, picked.key, system, user, maxTokens, model)
        : await callAnthropic(picked.key, system, user, maxTokens);
      return { text, provider, model: model || modelOf(provider), keyLabel: label(picked.key, picked.index), durationMs: Date.now() - started };
    } catch (e) {
      lastError = e;
      if (e.status === 429) coolDown(picked.key, config.ai.rateLimitCooldownSec);
      else if (e.status === 403 || e.status === 402) coolDown(picked.key, config.ai.quotaCooldownSec);
      else break;  // andere Fehler betreffen alle Keys gleichermaßen
    }
  }
  throw lastError || new ProviderError("KI-Aufruf fehlgeschlagen", 502);
}

/**
 * Steht dieser Anbieter bereit? Wird für die Stufenwahl der Antwortprüfung
 * gebraucht: erst das gute Modell, bei viel Betrieb das günstige.
 */
/** Das eingestellte Modell eines Anbieters. */
/* ---------------------------- Der Disponent -------------------------------
   Ein kleines Modell auf eigener Hardware liest die Anfrage zuerst und
   entscheidet, wer sie bearbeitet. Der Prompt ist bewusst eng gehalten:
   Kleine Modelle halten sich an eine Vorgabe umso besser, je weniger
   Spielraum sie lässt.
   ------------------------------------------------------------------------- */
export const DISPATCH_SYSTEM_PROMPT = `Du bist der Disponent eines Programmier-Teams in einem Code-Editor.

Du beantwortest die Anfrage NICHT. Du entscheidest nur, wer sie bearbeitet,
und schreibst dazu einen kurzen Auftrag.

Zur Wahl stehen drei:
- "lokal": du selbst. Für kurze Fragen, Erklärungen in ein bis drei Sätzen,
  Tippfehler, kleine Korrekturen an einer einzelnen Stelle.
- "standard": das mittlere Modell. Für normale Fragen, Fehlersuche im Code
  und Änderungen an einer Datei.
- "profi": das große Modell. Für ganze Dateien, mehrere Dateien auf einmal,
  Umbauten und schwierige Fehler.

Antworte mit GENAU einem JSON-Objekt und sonst nichts — kein Text davor,
keiner danach, keine Codeblöcke:

{"ziel": "standard", "auftrag": "…", "grund": "…"}

Regeln:
- "ziel" ist genau eines von: lokal, standard, profi
- "auftrag": ein bis zwei Sätze auf Deutsch, was zu tun ist. KEIN Code.
- "grund": höchstens acht Wörter.`;

const ZIELE = new Set(["lokal", "standard", "profi"]);

/** Liest die Entscheidung des Disponenten. Unbrauchbares ergibt null. */
export function parseDispatch(text) {
  const roh = String(text || "");
  // Manche Modelle verpacken das JSON trotz Vorgabe in einen Codeblock.
  const ohneZaun = roh.replace(/```(?:json)?/gi, "");
  const start = ohneZaun.indexOf("{");
  const ende = ohneZaun.lastIndexOf("}");
  if (start < 0 || ende <= start) return null;
  let daten;
  try { daten = JSON.parse(ohneZaun.slice(start, ende + 1)); } catch { return null; }
  const ziel = String(daten?.ziel || "").toLowerCase().trim();
  if (!ZIELE.has(ziel)) return null;
  return {
    ziel,
    auftrag: String(daten.auftrag || "").slice(0, 400).trim(),
    grund: String(daten.grund || "").slice(0, 120).trim(),
  };
}

/**
 * Grobe Schätzung des Verbrauchs.
 *
 * Ein echter Tokenizer je Anbieter wäre genauer, aber jeder zählt anders,
 * und für eine Bremse genügt die Größenordnung: rund vier Zeichen ergeben
 * ein Token. Lieber leicht überschätzen als jemanden versehentlich
 * durchlassen — deshalb wird aufgerundet.
 */
export function schaetzeTokens(...texte) {
  const zeichen = texte.filter(Boolean).map(String).join("").length;
  return Math.ceil(zeichen / 4);
}

/* ------------------------- Mehrere Anbieter nacheinander ------------------
   Kontingente sind bei jedem Anbieter anders geschnitten. Groq erlaubt viele
   Anfragen pro Minute, aber wenig Text; Cerebras umgekehrt: viel Text, dafür
   weniger Anfragen. Wer nur einen einträgt, steht bei dessen Limit still,
   obwohl der andere frei wäre.

   Deshalb eine Kette. Ein Eintrag ist entweder nur der Anbieter (`groq`) oder
   Anbieter und Modell (`groq:llama-3.3-70b-versatile`) — so lässt sich
   derselbe Anbieter mit einem kleineren Modell ein zweites Mal in die Kette
   stellen, wenn das große sein Limit erreicht hat.

   Gewechselt wird nur bei Kontingent- und Ausfallfehlern. Ein 400 wegen eines
   falschen Modellnamens bleibt ein Fehler und wird gemeldet — sonst sucht die
   Kette reihum weiter und verdeckt einen Tippfehler in der Konfiguration.
   ------------------------------------------------------------------------ */
const WEITER_BEI = new Set([408, 402, 403, 429, 500, 502, 503, 504]);

/** Zerlegt „groq:llama-3.3-70b" in Anbieter und Modell. */
export function ketteLesen(eintraege) {
  return (Array.isArray(eintraege) ? eintraege : String(eintraege || "").split(","))
    .map((e) => String(e).trim())
    .filter(Boolean)
    .map((e) => {
      const i = e.indexOf(":");
      return i > 0
        ? { provider: e.slice(0, i).trim(), model: e.slice(i + 1).trim() }
        : { provider: e, model: "" };
    });
}

export async function generateChain({ kette, system, user, maxTokens }) {
  const bereit = ketteLesen(kette).filter((g) => providerReady(g.provider));
  if (!bereit.length) throw new ProviderError("Kein Anbieter bereit", 503);

  let letzter;
  for (const glied of bereit) {
    try {
      const r = await generate({
        system, user, maxTokens,
        provider: glied.provider,
        model: glied.model || undefined,
      });
      return { ...r, kette: bereit.map((g) => g.provider) };
    } catch (e) {
      letzter = e;
      const ausfall = WEITER_BEI.has(e.status)
        || /fetch failed|ECONNREFUSED|ENOTFOUND|timeout|aborted/i.test(String(e.message));
      if (!ausfall) throw e;
    }
  }
  throw letzter;
}

export function modelOf(provider) {
  if (provider === "gemini") return config.ai.geminiModel;
  if (provider === "anthropic") return config.ai.anthropicModel;
  if (provider === "openrouter") return config.ai.openrouterModel;
  if (provider === "groq") return config.ai.groqModel;
  if (provider === "cerebras") return config.ai.cerebrasModel;
  if (provider === "nvidia") return config.ai.nvidiaModel;
  if (provider === "ollama") return config.ai.ollamaModel;
  return "";
}

/* Ollama läuft auf dem eigenen Rechner — oder eben nicht. Anders als bei
   einem Schlüssel lässt sich das nicht am Wert ablesen, sondern nur durch
   Anfragen. Ohne diese Unterscheidung gälte Ollama immer als bereit, und
   jede offene Aufgabe liefe erst in die Zeitüberschreitung, bevor lokal
   bewertet wird. Beim Serverstart wird deshalb einmal nachgesehen, und ein
   fehlgeschlagener Aufruf merkt sich das.

   null = noch nicht geprüft (im Zweifel versuchen wir es) */
let ollamaErreichbar = null;

export function setOllamaReachable(wert) { ollamaErreichbar = wert; }
export function isOllamaReachable() { return ollamaErreichbar; }

export function providerReady(provider) {
  if (provider === "ollama") return ollamaErreichbar !== false;
  // Bei OpenRouter genügt der Schlüssel nicht: Ohne Modell-ID weiß der
  // Dienst nicht, wen er fragen soll, und antwortet mit 404.
  /* Bei OpenRouter und NVIDIA genügt der Schlüssel nicht: Beide bündeln
     hunderte Modelle hinter einer Adresse. Ohne Modell-ID weiß der Dienst
     nicht, wen er fragen soll, und antwortet mit 404. Eine Vorgabe im Code
     wäre hier falsch — die Kataloge ändern sich laufend, und eine erfundene
     ID kostet nur Suchzeit. */
  if (provider === "openrouter") {
    return keysFor(provider).length > 0 && !!String(config.ai.openrouterModel || "").trim();
  }
  if (provider === "nvidia") {
    return keysFor(provider).length > 0 && !!String(config.ai.nvidiaModel || "").trim();
  }
  return keysFor(provider).length > 0;
}

/**
 * Liest das Urteil aus der Modellantwort. Modelle packen JSON gerne in einen
 * Codeblock oder schreiben einen Satz davor — beides wird toleriert. Was
 * danach nicht plausibel ist, wird verworfen; dann bleibt es beim lokalen
 * Ergebnis, statt eine erfundene Bewertung anzuzeigen.
 */
export function parseVerdict(text) {
  const raw = String(text || "");
  const match = raw.match(/\{[\s\S]*\}/);
  if (!match) return null;
  let data;
  try { data = JSON.parse(match[0]); } catch (e) { return null; }
  if (!data || typeof data !== "object") return null;

  const score = Number(data.score);
  if (!Number.isFinite(score)) return null;
  const feedback = String(data.feedback || "").trim();
  if (!feedback) return null;

  return {
    score: Math.max(0, Math.min(100, Math.round(score))),
    correct: data.correct === true,
    feedback: feedback.slice(0, 600),
    hint: String(data.hint || "").trim().slice(0, 400),
  };
}

/* ------------------------------- Prompts -------------------------------- */
/* Wortgleich mit der Fassung in App.jsx — beide Wege müssen dieselbe
   Antwort erzeugen. Die Trennlinie ist der Kern: Wer denken will, darf davor
   denken; gelesen wird nur, was danach kommt. Das ist verlässlicher als der
   Versuch, Denken zu verbieten — der Versuch endete damit, dass das Modell
   die Verbote als Liste zurückgab. */
const ANTWORT_MARKER = "===ANTWORT===";

export const ASSISTANT_SYSTEM_PROMPT = `Du bist der Programmier-Agent in einem Code-Editor.
Du schreibst auf Deutsch. Der Nutzer lernt gerade programmieren.

AUFTRAG ("baue", "erstelle", "schreib mir", "mach"):
Liefere vollständige, lauffähige Dateien. Jede Datei als eigener Codeblock,
Dateiname in der ersten Zeile:

\`\`\`html datei=index.html
<!DOCTYPE html>
…
\`\`\`

Danach höchstens drei Sätze dazu, was du gebaut hast.

FRAGE:
Kurz und konkret, höchstens sechs Sätze. Code in Codeblöcken mit
Sprachangabe. Erkläre das Warum, nicht nur das Wie.

SO ANTWORTEST DU:
Schreibe die Zeile

${ANTWORT_MARKER}

und danach die Antwort für den Nutzer. Was davor steht, sieht niemand — dort
darfst du überlegen, so lange du willst. Nach der Zeile steht nur noch das
Ergebnis: auf Deutsch, ohne Notizen, ohne Wiederholung dieser Anweisungen.`;

/* ------------------- Zweitmeinung zu einer Lösung -------------------------
   Der Prüfer im Browser erkennt Struktur zuverlässig, aber nicht, ob eine
   Lösung inhaltlich das Richtige tut. Bei offenen Aufgaben („schreib den
   Code", „erkläre …") holt der Server deshalb eine zweite Meinung ein.

   Wichtig: Das Modell entscheidet nicht allein. Es liefert Score, Urteil und
   Begründung, und der Server begrenzt, wie weit das vom lokalen Ergebnis
   abweichen darf. So kann eine überfreundliche Antwort niemanden durchwinken,
   der nichts geschrieben hat.
   ------------------------------------------------------------------------- */
export const VERIFY_SYSTEM_PROMPT = `Du bewertest Lösungen von Programmier-Anfängerinnen und -Anfängern auf einer deutschen Lernplattform.

Du bekommst: die Aufgabenstellung, die erwarteten Bausteine, die Sprache und die eingereichte Antwort.

BEWERTE STRENG, ABER FAIR:
- Punkte gibt es nur, wenn die Antwort die Aufgabe tatsächlich löst.
- Einzelne Wörter oder Stichworte untereinander sind KEINE Lösung — 0 Punkte.
- Abgeschriebene Aufgabenstellungen sind KEINE Lösung — 0 Punkte.
- Kleine Schönheitsfehler (fehlendes Semikolon, andere Variablennamen, andere
  Formulierung) sind kein Grund für einen Abzug, solange die Lösung stimmt.
- Bei Erklärungen zählt der Inhalt, nicht die Wortzahl.

ANTWORTE AUSSCHLIESSLICH ALS JSON, ohne Codeblock, in genau dieser Form:
{"score": 0-100, "correct": true|false, "feedback": "ein bis zwei Sätze auf Deutsch", "hint": "ein konkreter nächster Schritt auf Deutsch"}

"correct" ist nur dann true, wenn die Aufgabe wirklich gelöst wurde.
"feedback" spricht die lernende Person direkt an und benennt konkret, was stimmt oder fehlt.
Erfinde keine Fehler, die nicht da sind.`;

