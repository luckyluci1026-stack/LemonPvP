/**
 * Zentrale Konfiguration. Alle Werte kommen aus Umgebungsvariablen,
 * damit keine Geheimnisse im Quellcode landen.
 */
import { readFileSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

// Schlanker .env-Loader — spart eine Abhängigkeit. Bereits gesetzte
// Umgebungsvariablen haben Vorrang (wichtig für systemd und Container).
(function loadDotEnv() {
  const envPath = join(dirname(fileURLToPath(import.meta.url)), "..", ".env");
  if (!existsSync(envPath)) return;
  for (const line of readFileSync(envPath, "utf8").split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eq = trimmed.indexOf("=");
    if (eq < 1) continue;
    const key = trimmed.slice(0, eq).trim();
    if (process.env[key] !== undefined) continue;
    let value = trimmed.slice(eq + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    process.env[key] = value;
  }
})();

function required(name, fallback) {
  const v = process.env[name] ?? fallback;
  if (v === undefined || v === "") {
    throw new Error(`Umgebungsvariable ${name} fehlt — siehe .env.example`);
  }
  return v;
}

function bool(name, fallback = false) {
  const v = process.env[name];
  if (v === undefined) return fallback;
  return /^(1|true|yes|on)$/i.test(v);
}

function int(name, fallback) {
  const v = parseInt(process.env[name] ?? "", 10);
  return Number.isFinite(v) ? v : fallback;
}

// Mehrere Keys können komma- oder zeilengetrennt hinterlegt werden.
function keyList(name) {
  return (process.env[name] || "")
    .split(/[,\n]/)
    .map((s) => s.trim())
    .filter(Boolean);
}

const isProd = process.env.NODE_ENV === "production";

export const config = {
  isProd,
  port: int("PORT", 3000),
  host: process.env.HOST || "0.0.0.0",

  // Öffentliche Adresse — wird für Links in E-Mails gebraucht
  publicUrl: (process.env.PUBLIC_URL || "http://localhost:3000").replace(/\/+$/, ""),

  // Wird die App hinter Cloudflare/nginx betrieben, liefert der Proxy die echte IP
  trustProxy: bool("TRUST_PROXY", true),

  // Statisches Frontend mit ausliefern (index.html + App.jsx)
  serveFrontend: bool("SERVE_FRONTEND", true),
  frontendDir: process.env.FRONTEND_DIR || "..",

  db: {
    connectionString: required("DATABASE_URL", "postgres://localhost:5432/learndeveloping"),
    max: int("DB_POOL_MAX", 10),
  },

  session: {
    // Zum Signieren der Session-Token. In Produktion zwingend setzen.
    secret: required("SESSION_SECRET", isProd ? undefined : "dev-only-insecure-secret"),
    cookieName: "ld_session",
    ttlDays: int("SESSION_TTL_DAYS", 30),
  },

  mail: {
    enabled: bool("SMTP_ENABLED", false),
    host: process.env.SMTP_HOST || "",
    port: int("SMTP_PORT", 587),
    secure: bool("SMTP_SECURE", false),
    user: process.env.SMTP_USER || "",
    pass: process.env.SMTP_PASS || "",
    from: process.env.MAIL_FROM || "LearnDeveloping <noreply@learndeveloping.com>",
    supportAddress: process.env.SUPPORT_MAIL || "support@learndeveloping.com",
  },

  turnstile: {
    // Ohne Secret wird die Prüfung übersprungen (praktisch für lokale Entwicklung)
    secret: process.env.TURNSTILE_SECRET || "",
    get enabled() { return !!this.secret; },
  },

  ai: {
    // Die Keys bleiben ausschließlich hier auf dem Server.
    provider: process.env.AI_PROVIDER || "gemini",
    geminiKeys: keyList("GEMINI_API_KEYS"),
    anthropicKeys: keyList("ANTHROPIC_API_KEYS"),
    /* OpenRouter bündelt viele Anbieter hinter einer OpenAI-kompatiblen
       Schnittstelle. Die Vorgabe ist ein kostenloses Modell mit sehr großem
       Kontextfenster (262k Token).

       Der Katalog ändert sich laufend — läuft die Prüfung plötzlich immer
       lokal durch, ist ein 404 wegen einer nicht mehr existierenden ID der
       erste Verdacht. `npm run ai:test` sagt genau das. Die aktuell gültigen
       IDs stehen auf openrouter.ai/models. */
    openrouterKeys: keyList("OPENROUTER_API_KEYS"),
    openrouterModel: process.env.OPENROUTER_MODEL || "google/gemma-4-31b-it:free",
    ollamaUrl: (process.env.OLLAMA_URL || "http://127.0.0.1:11434").replace(/\/+$/, ""),
    ollamaModel: process.env.OLLAMA_MODEL || "qwen2.5-coder:3b",
    // Modell im Speicher halten, statt es bei jedem Aufruf neu zu laden
    ollamaKeepAlive: process.env.OLLAMA_KEEP_ALIVE || "30m",
    // Kurze Antworten und kleiner Kontext sparen auf schwacher Hardware am meisten
    ollamaMaxTokens: int("OLLAMA_MAX_TOKENS", 300),
    ollamaContext: int("OLLAMA_CONTEXT", 2048),
    ollamaThreads: int("OLLAMA_THREADS", 0),   // 0 = Ollama entscheidet
    warmUp: bool("AI_WARMUP", true),
    /* Gemma 4 läuft über dieselbe Google-Schnittstelle wie Gemini und
       unterstützt dort auch systemInstruction — der Aufruf unten ist deshalb
       für beide derselbe. Wer lieber ein Gemini-Modell möchte, trägt in
       GEMINI_MODEL z.B. `gemini-2.0-flash` ein. */
    geminiModel: process.env.GEMINI_MODEL || "gemma-4-31b-it",
    anthropicModel: process.env.ANTHROPIC_MODEL || "claude-sonnet-4-6",
    // Pausen nach Limit-Antworten
    rateLimitCooldownSec: int("AI_COOLDOWN_RATE_LIMIT", 65),
    quotaCooldownSec: int("AI_COOLDOWN_QUOTA", 600),
    requestTimeoutMs: int("AI_TIMEOUT_MS", 60000),
    // Anfragen pro Nutzer und Minute
    perUserPerMinute: int("AI_PER_USER_PER_MINUTE", 20),

    /* ------------------- Antwortprüfung: Stufen und Bremse ----------------
       Drei Stufen, damit weder eine Rechnung noch ein Rate-Limit überrascht:

         1. Der eingestellte Hauptanbieter (Vorgabe: Google mit eigenem Key).
         2. Ist das Tageskontingent eines Nutzers ausgereizt, übernimmt der
            Ersatzanbieter.
         3. Darüber hinaus bewertet nur noch die lokale Analyse — die Lektion
            läuft normal weiter, es gibt lediglich keine Zweitmeinung mehr.

       Die Zahlen sind bewusst niedrig angesetzt. Kostenlose Kontingente sind
       eng: Gemma über OpenRouter erlaubt etwa 20 Anfragen pro Minute und 200
       pro Tag — und zwar pro KONTO, nicht pro Nutzer. Deshalb ist die globale
       Notbremse wichtiger als die persönliche. */
    verify: {
      enabled: bool("AI_VERIFY_ENABLED", true),
      // Welcher Anbieter zuerst gefragt wird …
      primaryProvider: process.env.AI_VERIFY_PRIMARY || "gemini",
      // … und wohin es geht, wenn jemand das Kontingent ausreizt.
      fallbackProvider: process.env.AI_VERIFY_FALLBACK || "openrouter",
      // Prüfungen pro Nutzer und Tag beim Hauptanbieter
      primaryPerDay: int("AI_VERIFY_PRIMARY_PER_DAY", 60),
      // Danach nur noch der Ersatzanbieter — bis zu dieser Grenze
      maxPerDay: int("AI_VERIFY_MAX_PER_DAY", 150),
      // Notbremse über alle Nutzer hinweg. Bei einem kostenlosen Kontingent
      // von 200 Anfragen am Tag hier deutlich darunter bleiben.
      globalPerDay: int("AI_VERIFY_GLOBAL_PER_DAY", 180),
      timeoutMs: int("AI_VERIFY_TIMEOUT_MS", 12000),
    },
  },

  storage: {
    quotaBytes: int("STORAGE_QUOTA_BYTES", 1 * 1024 * 1024 * 1024),
    maxProjectBytes: int("MAX_PROJECT_BYTES", 5 * 1024 * 1024),
  },
};

export function assertProductionSafety(log) {
  if (!config.isProd) return;
  const problems = [];
  if (config.session.secret === "dev-only-insecure-secret") problems.push("SESSION_SECRET ist nicht gesetzt");
  if (!config.turnstile.enabled) problems.push("TURNSTILE_SECRET fehlt — Registrierung ist ungeschützt");
  if (!config.mail.enabled) problems.push("SMTP ist deaktiviert — Verifizierungscodes werden nur geloggt");
  problems.forEach((p) => log.warn(`Produktionshinweis: ${p}`));
}
