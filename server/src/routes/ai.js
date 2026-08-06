import { config } from "../config.js";
import { query } from "../db.js";
import { requireAuth } from "./auth.js";
import { generate, aiAvailable, poolStatus, ASSISTANT_SYSTEM_PROMPT } from "../ai.js";

async function record(request, kind, result, ok, statusCode) {
  try {
    await query(
      `INSERT INTO ai_usage (user_id, provider, key_label, kind, ok, status_code, duration_ms)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [request.user?.id || null, config.ai.provider, result?.keyLabel || null, kind, ok, statusCode || null, result?.durationMs || null]
    );
  } catch (e) {
    request.log.warn({ err: e }, "KI-Statistik konnte nicht gespeichert werden");
  }
}

export default async function aiRoutes(app) {
  /** Meldet, ob serverseitig eine KI bereitsteht. */
  app.get("/api/ai/status", async () => ({
    available: aiAvailable(),
    provider: config.ai.provider,
  }));


  /* ------------------- Assistent im Code-Editor --------------------------- */
  // Der einzige KI-Endpunkt, der im normalen Betrieb genutzt wird — Lektionen
  // werden ausschließlich lokal im Browser bewertet.
  app.post("/api/ai/assist", {
    preHandler: [requireAuth],
    config: { rateLimit: { max: config.ai.perUserPerMinute, timeWindow: "1 minute" } },
  }, async (request, reply) => {
    if (!aiAvailable()) {
      return reply.code(503).send({ error: "Serverseitig ist keine KI konfiguriert." });
    }
    const { messages = [], code = {} } = request.body || {};
    if (!Array.isArray(messages) || !messages.length) {
      return reply.code(400).send({ error: "messages fehlt." });
    }

    const context = ["html", "css", "js"]
      .map((k) => {
        const body = String(code[k] || "").trim();
        return body ? `\n--- ${k.toUpperCase()} ---\n${body.slice(0, 6000)}` : "";
      })
      .join("");
    const history = messages.slice(-8)
      .map((m) => `${m.role === "user" ? "Nutzer" : "Assistent"}: ${String(m.content || "").slice(0, 4000)}`)
      .join("\n\n");
    const userPrompt = `${context ? `Aktueller Code im Editor:${context}` : "Der Editor ist noch leer."}\n\n--- Verlauf ---\n${history}`;

    let result;
    try {
      result = await generate({ system: ASSISTANT_SYSTEM_PROMPT, user: userPrompt, maxTokens: 900 });
    } catch (e) {
      await record(request, "assist", null, false, e.status);
      return reply.code(502).send({ error: "Die KI ist momentan nicht erreichbar." });
    }
    await record(request, "assist", result, true, 200);
    return { reply: String(result.text).slice(0, 8000), provider: result.provider };
  });

  /* --------------------- Zustand des Key-Pools (Admin) -------------------- */
  app.get("/api/ai/pool", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "admin") return reply.code(403).send({ error: "Nur für Administratoren." });
    return poolStatus();
  });
}
