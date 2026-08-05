import { config } from "../config.js";
import { query } from "../db.js";
import { requireAuth } from "./auth.js";
import {
  generate, parseJsonAnswer, aiAvailable, poolStatus,
  CHECK_SYSTEM_PROMPT, DEBUG_SYSTEM_PROMPT, buildCheckPrompt, buildDebugPrompt,
} from "../ai.js";

const MAX_ANSWER = 8000;
const MAX_CODE = 20000;

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

  /* -------------------- Antwort auf eine Aufgabe bewerten ----------------- */
  app.post("/api/ai/check", {
    preHandler: [requireAuth],
    config: { rateLimit: { max: config.ai.perUserPerMinute, timeWindow: "1 minute" } },
  }, async (request, reply) => {
    if (!aiAvailable()) {
      return reply.code(503).send({ error: "Serverseitig ist keine KI konfiguriert.", fallbackToLocal: true });
    }
    const { language, lessonTitle, question, expectedConcepts, answer } = request.body || {};
    if (!question || !answer) {
      return reply.code(400).send({ error: "question und answer sind erforderlich." });
    }

    const prompt = buildCheckPrompt({
      language: String(language || "Programmierung").slice(0, 40),
      lessonTitle: String(lessonTitle || "").slice(0, 200),
      question: String(question).slice(0, 2000),
      expectedConcepts: Array.isArray(expectedConcepts) ? expectedConcepts.slice(0, 20).map(String) : [],
      answer: String(answer).slice(0, MAX_ANSWER),
    });

    let result;
    try {
      result = await generate({ system: CHECK_SYSTEM_PROMPT, user: prompt, maxTokens: 1000 });
    } catch (e) {
      await record(request, "check", null, false, e.status);
      request.log.warn({ err: e }, "KI-Bewertung fehlgeschlagen");
      // Das Frontend nutzt in diesem Fall seine eigene lokale Analyse.
      return reply.code(502).send({ error: "Die KI ist momentan nicht erreichbar.", fallbackToLocal: true });
    }

    try {
      const parsed = parseJsonAnswer(result.text);
      await record(request, "check", result, true, 200);
      return {
        correct: !!parsed.correct,
        score: Math.max(0, Math.min(100, Number(parsed.score) || 0)),
        feedback: String(parsed.feedback || "").slice(0, 1500),
        hint: String(parsed.hint || "").slice(0, 1000),
        praise: String(parsed.praise || "").slice(0, 500),
        offline: false,
        provider: result.provider,
      };
    } catch (e) {
      await record(request, "check", result, false, 502);
      return reply.code(502).send({ error: "Die KI-Antwort war unlesbar.", fallbackToLocal: true });
    }
  });

  /* --------------------------- Code debuggen ------------------------------ */
  app.post("/api/ai/debug", {
    preHandler: [requireAuth],
    config: { rateLimit: { max: Math.max(5, Math.floor(config.ai.perUserPerMinute / 2)), timeWindow: "1 minute" } },
  }, async (request, reply) => {
    if (!aiAvailable()) {
      return reply.code(503).send({ error: "Serverseitig ist keine KI konfiguriert.", fallbackToLocal: true });
    }
    const { html = "", css = "", js = "" } = request.body || {};
    const prompt = buildDebugPrompt({
      html: String(html).slice(0, MAX_CODE),
      css: String(css).slice(0, MAX_CODE),
      js: String(js).slice(0, MAX_CODE),
    });

    let result;
    try {
      result = await generate({ system: DEBUG_SYSTEM_PROMPT, user: prompt, maxTokens: 1500 });
    } catch (e) {
      await record(request, "debug", null, false, e.status);
      return reply.code(502).send({ error: "Die KI ist momentan nicht erreichbar.", fallbackToLocal: true });
    }

    try {
      const parsed = parseJsonAnswer(result.text);
      await record(request, "debug", result, true, 200);
      const issues = Array.isArray(parsed.issues) ? parsed.issues.slice(0, 25).map((i) => ({
        severity: ["error", "warning", "info"].includes(i.severity) ? i.severity : "info",
        where: ["html", "css", "js"].includes(i.where) ? i.where : undefined,
        title: String(i.title || "").slice(0, 200),
        detail: String(i.detail || "").slice(0, 800),
        fix: String(i.fix || "").slice(0, 800),
      })) : [];
      return {
        summary: String(parsed.summary || "").slice(0, 800),
        issues,
        offline: false,
        provider: result.provider,
      };
    } catch (e) {
      await record(request, "debug", result, false, 502);
      return reply.code(502).send({ error: "Die KI-Antwort war unlesbar.", fallbackToLocal: true });
    }
  });

  /* --------------------- Zustand des Key-Pools (Admin) -------------------- */
  app.get("/api/ai/pool", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "admin") return reply.code(403).send({ error: "Nur für Administratoren." });
    return poolStatus();
  });
}
