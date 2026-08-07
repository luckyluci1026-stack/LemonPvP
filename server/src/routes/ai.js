import { config } from "../config.js";
import { query } from "../db.js";
import { requireAuth } from "./auth.js";
import { generate, aiAvailable, poolStatus, providerReady, parseVerdict,
         ASSISTANT_SYSTEM_PROMPT, VERIFY_SYSTEM_PROMPT } from "../ai.js";

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
    // Steht die Zweitmeinung für offene Aufgaben bereit? Der Browser fragt
    // sonst gar nicht erst an.
    verify: config.ai.verify.enabled
      && (providerReady(config.ai.verify.primaryProvider) || providerReady(config.ai.verify.fallbackProvider)),
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

  /* ------------------ Zweitmeinung zu einer Lösung ------------------------
     Der Browser hat bereits lokal bewertet und schickt sein Ergebnis mit. Die
     KI schaut nur noch einmal drüber — und darf das lokale Urteil nur in
     engen Grenzen verschieben. Antwortet sie nicht oder unsinnig, bleibt es
     beim lokalen Ergebnis; die Lektion läuft also auch ohne KI weiter.
     --------------------------------------------------------------------- */
  app.post("/api/ai/verify", {
    preHandler: [requireAuth],
    config: { rateLimit: { max: config.ai.perUserPerMinute, timeWindow: "1 minute" } },
  }, async (request, reply) => {
    const v = config.ai.verify;
    if (!v.enabled) return reply.code(503).send({ error: "Die Antwortprüfung per KI ist abgeschaltet.", tier: "local" });

    const { question = "", answer = "", language = "", concepts = [], type = "", local = {} } = request.body || {};
    if (!String(answer).trim()) return reply.code(400).send({ error: "answer fehlt." });

    /* Stufenwahl. Gezählt wird über die vorhandene Nutzungstabelle, damit es
       keine zweite Buchführung braucht. */
    let mine = 0, all = 0;
    try {
      const counts = await query(
        // Bewusst ohne FILTER-Klausel — so läuft die Abfrage auf PostgreSQL
        // und im schlanken SQLite-Modus gleichermaßen.
        `SELECT
           SUM(CASE WHEN user_id = $1 THEN 1 ELSE 0 END) AS mine,
           COUNT(*)                                      AS all_users
         FROM ai_usage
         WHERE kind = 'verify' AND ok = TRUE AND created_at > now() - INTERVAL '1 day'`,
        [request.user.id]
      );
      mine = Number(counts.rows[0]?.mine || 0);
      all = Number(counts.rows[0]?.all_users || 0);
    } catch (e) {
      request.log.warn({ err: e }, "KI-Kontingent konnte nicht gelesen werden");
    }

    if (mine >= v.maxPerDay || all >= v.globalPerDay) {
      // Wer das ausreizt, prüft nicht mehr, sondern probiert etwas aus.
      return reply.code(429).send({ error: "Tageskontingent für die KI-Prüfung erreicht.", tier: "local" });
    }

    // Bis zum Kontingent das gute Modell, danach automatisch das günstige.
    const wanted = mine < v.primaryPerDay ? v.primaryProvider : v.fallbackProvider;
    const provider = providerReady(wanted) ? wanted
      : providerReady(v.fallbackProvider) ? v.fallbackProvider
      : providerReady(config.ai.provider) ? config.ai.provider : null;
    if (!provider) return reply.code(503).send({ error: "Serverseitig ist keine KI konfiguriert.", tier: "local" });

    const expected = (Array.isArray(concepts) ? concepts : [])
      .map((c) => (Array.isArray(c) ? c[0] : c)).filter(Boolean).slice(0, 12).join(", ");
    const userPrompt = [
      `Aufgabentyp: ${String(type).slice(0, 40) || "offen"}`,
      `Sprache: ${String(language).slice(0, 30) || "unbekannt"}`,
      `Aufgabenstellung: ${String(question).slice(0, 1200)}`,
      expected ? `Erwartete Bausteine: ${expected}` : "",
      `Vorbewertung der lokalen Analyse: ${Number(local.score) || 0}/100, ${local.correct ? "bestanden" : "nicht bestanden"}`,
      "",
      "--- Eingereichte Antwort ---",
      String(answer).slice(0, 4000),
    ].filter(Boolean).join("\n");

    let result;
    try {
      result = await generate({ system: VERIFY_SYSTEM_PROMPT, user: userPrompt, maxTokens: 320, provider });
    } catch (e) {
      await record(request, "verify", null, false, e.status);
      return reply.code(502).send({ error: "Die KI ist momentan nicht erreichbar.", tier: "local" });
    }

    const parsed = parseVerdict(result.text);
    if (!parsed) {
      await record(request, "verify", result, false, 502);
      return reply.code(502).send({ error: "Unerwartete Antwort der KI.", tier: "local" });
    }
    await record(request, "verify", result, true, 200);
    return {
      ...parsed,
      provider,
      tier: provider === v.primaryProvider ? "primary" : "fallback",
      remaining: Math.max(0, v.maxPerDay - mine - 1),
    };
  });

  /* --------------------- Zustand des Key-Pools (Admin) -------------------- */
  app.get("/api/ai/pool", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "admin") return reply.code(403).send({ error: "Nur für Administratoren." });
    return poolStatus();
  });
}
