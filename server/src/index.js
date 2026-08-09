import Fastify from "fastify";
import cookie from "@fastify/cookie";
import rateLimit from "@fastify/rate-limit";
import fastifyStatic from "@fastify/static";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { createRequire } from "node:module";

import { config, assertProductionSafety } from "./config.js";
import { migrate, cleanupSessions, one, query } from "./db.js";
import { hashToken } from "./security.js";
import authRoutes from "./routes/auth.js";
import appRoutes from "./routes/app.js";
import adminRoutes from "./routes/admin.js";
import aiRoutes from "./routes/ai.js";
import { warmUpOllama, checkOllama, ollamaInUse } from "./ai.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

// Lesbare Logs in der Entwicklung, aber nur wenn pino-pretty vorhanden ist —
// so bleibt die Produktionsinstallation ohne zusätzliche Abhängigkeit lauffähig.
function prettyTransport() {
  if (config.isProd) return undefined;
  try {
    createRequire(import.meta.url).resolve("pino-pretty");
    return { target: "pino-pretty" };
  } catch {
    return undefined;
  }
}

const app = Fastify({
  logger: {
    level: process.env.LOG_LEVEL || "info",
    transport: prettyTransport(),
  },
  trustProxy: config.trustProxy,   // hinter Cloudflare/nginx die echte IP nutzen
  bodyLimit: 2 * 1024 * 1024,
});

await app.register(cookie, { secret: config.session.secret });

// Manche Endpunkte brauchen keinen Body (z.B. Logout, 2FA-Einrichtung).
// Fastify lehnt einen leeren Body bei JSON-Content-Type sonst mit 400 ab.
app.addContentTypeParser("application/json", { parseAs: "string" }, (request, body, done) => {
  if (!body || !String(body).trim()) return done(null, {});
  try {
    done(null, JSON.parse(body));
  } catch (e) {
    done(Object.assign(new Error("Ungültiges JSON im Anfragetext."), { statusCode: 400 }), undefined);
  }
});

/* Die Anfragenbegrenzung lässt sich abschalten.
   Im Betrieb bleibt sie an — sie ist der Schutz gegen Ausprobieren von
   Passwörtern. Beim automatisierten Testen ist sie dagegen im Weg: Die
   Testdateien laufen nebeneinander und teilen sich dieselbe Absenderadresse,
   also auch dasselbe Kontingent. Ob die Anmeldung im 14. oder 16. Versuch
   passiert, hängt dann von der Zahl der Testdateien ab — und die Suite
   scheitert an sich selbst statt an einem Fehler im Code. */
if (config.security.rateLimit) await app.register(rateLimit, {
  global: true,
  max: 300,
  timeWindow: "1 minute",
  // Hinter Cloudflare identifiziert CF-Connecting-IP den echten Client
  keyGenerator: (req) => req.headers["cf-connecting-ip"] || req.ip,
  // statusCode muss mitgegeben werden, sonst kann der Fehler-Handler die
  // 429-Antwort nicht von einem echten Serverfehler unterscheiden.
  errorResponseBuilder: (req, context) => ({
    statusCode: 429,
    error: `Zu viele Anfragen. Bitte in ${Math.ceil(context.ttl / 1000)} Sekunden erneut versuchen.`,
  }),
});

/* ----------------------- Sitzung an jeder Anfrage ------------------------ */
// Hängt request.user an, wenn ein gültiges Sitzungs-Cookie vorliegt.
app.addHook("onRequest", async (request) => {
  request.user = null;
  const raw = request.cookies?.[config.session.cookieName];
  if (!raw) return;
  const session = await one(
    `SELECT s.token_hash, u.id, u.role, u.disabled
       FROM sessions s JOIN users u ON u.id = s.user_id
      WHERE s.token_hash = $1 AND s.expires_at > now()`,
    [hashToken(raw)]
  );
  if (session && !session.disabled) {
    request.user = { id: session.id, role: session.role };
  }
});

/* ------------------------------ Sicherheit ------------------------------- */
app.addHook("onSend", async (request, reply, payload) => {
  reply.header("X-Content-Type-Options", "nosniff");
  reply.header("Referrer-Policy", "strict-origin-when-cross-origin");
  reply.header("X-Frame-Options", "SAMEORIGIN");
  if (request.url.startsWith("/api/")) reply.header("Cache-Control", "no-store");
  return payload;
});

app.setErrorHandler((error, request, reply) => {
  // Fastify setzt reply.statusCode bereits passend (z.B. 429 bei Ratenbegrenzung),
  // auch wenn das Fehlerobjekt selbst keinen Code trägt.
  const status = error.statusCode || (reply.statusCode >= 400 ? reply.statusCode : 500);
  if (status >= 500) request.log.error({ err: error }, "Unbehandelter Fehler");
  // Interna niemals nach außen geben
  reply.code(status).send({
    error: status >= 500 ? "Interner Serverfehler." : (error.error || error.message),
  });
});

/* -------------------------------- Routen -------------------------------- */
app.get("/api/health", async () => {
  const db = await one("SELECT 1 AS ok").then(() => true).catch(() => false);
  return { ok: true, db, version: 1 };
});

await app.register(authRoutes);
await app.register(appRoutes);
await app.register(adminRoutes);
await app.register(aiRoutes);

/* --------------------------- Frontend ausliefern ------------------------- */
if (config.serveFrontend) {
  const root = resolve(__dirname, "..", config.frontendDir);
  await app.register(fastifyStatic, { root, index: ["index.html"] });
  // Unbekannte Pfade (keine API) auf die App leiten
  app.setNotFoundHandler((request, reply) => {
    if (request.url.startsWith("/api/")) {
      return reply.code(404).send({ error: "Unbekannter Endpunkt." });
    }
    return reply.sendFile("index.html");
  });
  app.log.info(`Frontend wird ausgeliefert aus ${root}`);
}

/* --------------------------------- Start -------------------------------- */
try {
  await migrate(app.log);
  assertProductionSafety(app.log);

  // Abgelaufene Sitzungen stündlich aufräumen
  const cleanup = setInterval(async () => {
    try {
      const n = await cleanupSessions();
      if (n) app.log.info(`${n} abgelaufene Sitzungen entfernt`);
    } catch (e) {
      app.log.warn({ err: e }, "Sitzungsbereinigung fehlgeschlagen");
    }
  }, 3600_000);
  cleanup.unref();

  await app.listen({ port: config.port, host: config.host });
  app.log.info(`KI-Anbieter: ${config.ai.provider}`);

  // Modell im Hintergrund vorladen — blockiert den Start nicht
  /* Auch ohne Vorladen einmal nachsehen, ob Ollama läuft — sonst gilt es als
     bereit, und jede offene Aufgabe wartet erst die Zeitüberschreitung ab. */
  if (ollamaInUse()) {
    if (config.ai.warmUp) warmUpOllama(app.log);
    else checkOllama(app.log);
  }
} catch (e) {
  app.log.error(e, "Serverstart fehlgeschlagen");
  process.exit(1);
}

/* ----------------------------- Sauber beenden ---------------------------- */
for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, async () => {
    app.log.info(`${signal} empfangen — Server wird beendet`);
    await app.close();
    process.exit(0);
  });
}
