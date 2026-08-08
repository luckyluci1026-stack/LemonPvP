import { config } from "../config.js";
import { query } from "../db.js";
import { requireAuth } from "./auth.js";
import { generate, aiAvailable, poolStatus, providerReady, parseVerdict, parseDispatch,
         schaetzeTokens, ASSISTANT_SYSTEM_PROMPT, VERIFY_SYSTEM_PROMPT,
         DISPATCH_SYSTEM_PROMPT } from "../ai.js";

async function record(request, kind, result, ok, statusCode, tokens = 0) {
  try {
    await query(
      `INSERT INTO ai_usage (user_id, provider, key_label, kind, ok, status_code, duration_ms, tokens)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
      [request.user?.id || null, config.ai.provider, result?.keyLabel || null, kind, ok,
       statusCode || null, result?.durationMs || null, Math.max(0, Math.round(tokens) || 0)]
    );
  } catch (e) {
    request.log.warn({ err: e }, "KI-Statistik konnte nicht gespeichert werden");
  }
}

/* ------------------- Sitzungsbremse für den Agenten ----------------------
   Eine Minutenbremse allein reicht nicht: Wer sie einhält, kann trotzdem
   stundenlang Anfragen stellen und das Tageskontingent für alle anderen
   aufbrauchen. Gezählt wird deshalb in einem gleitenden Fenster — Anfragen
   und Token.

   Die Stundenzahl wird in die Abfrage eingesetzt statt gebunden, weil die
   SQLite-Übersetzung nur feste Intervalle kennt. Sie stammt aus der
   Konfiguration und wird vorher auf eine ganze Zahl zwischen 1 und 24
   begrenzt — sie kann also nichts anderes sein als eine Zahl.
   ------------------------------------------------------------------------ */
const AGENT_ARTEN = "('assist', 'assist_pro', 'dispatch')";

async function agentVerbrauch(request) {
  const stunden = Math.max(1, Math.min(24, Math.round(config.ai.agent.windowHours) || 4));
  try {
    const r = await query(
      `SELECT COUNT(*) AS anfragen, COALESCE(SUM(tokens), 0) AS tokens
         FROM ai_usage
        WHERE user_id = $1 AND ok = TRUE AND kind IN ${AGENT_ARTEN}
          AND created_at > now() - interval '${stunden} hours'`,
      [request.user.id]
    );
    return {
      anfragen: Number(r.rows[0]?.anfragen || 0),
      tokens: Number(r.rows[0]?.tokens || 0),
      stunden,
    };
  } catch (e) {
    /* Lässt sich der Verbrauch nicht lesen, wird nicht blockiert. Eine
       kaputte Statistik darf niemandem den Agenten wegnehmen. */
    request.log.warn({ err: e }, "Agent-Kontingent konnte nicht gelesen werden");
    return { anfragen: 0, tokens: 0, stunden, unbekannt: true };
  }
}

export default async function aiRoutes(app) {
  /** Meldet, ob serverseitig eine KI bereitsteht. */
  app.get("/api/ai/status", async () => ({
    available: providerReady(config.ai.roles.assist.provider) || aiAvailable(),
    provider: config.ai.provider,
    // Der stärkere Assistent wird nur angeboten, wenn er auch bereitsteht.
    pro: providerReady(config.ai.roles.assistPro.provider),
    // Steht die Zweitmeinung für offene Aufgaben bereit? Der Browser fragt
    // sonst gar nicht erst an.
    // Läuft ein Modell auf eigener Hardware, das die Anfragen verteilt?
    dispatch: config.ai.roles.dispatch.enabled
      && providerReady(config.ai.roles.dispatch.provider),
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
    /* Zwei Stufen: der normale Assistent und — auf ausdrücklichen Wunsch —
       der stärkere. Der Profi-Agent hängt an einem knappen Token-Kontingent
       pro Minute, deshalb bekommt er weniger Verlauf und weniger Code mit.
       Steht er nicht bereit, übernimmt still der normale. */
    const willPro = request.body?.pro === true;
    let rolle = willPro && providerReady(config.ai.roles.assistPro.provider)
      ? config.ai.roles.assistPro
      : config.ai.roles.assist;
    let anbieter = providerReady(rolle.provider) ? rolle.provider
      : providerReady(config.ai.provider) ? config.ai.provider : null;
    const disponent = config.ai.roles.dispatch;
    // Ohne fertigen Anbieter kann immer noch der Disponent selbst arbeiten.
    if (!anbieter && !(disponent.enabled && providerReady(disponent.provider))) {
      return reply.code(503).send({ error: "Serverseitig ist keine KI konfiguriert." });
    }

    const { messages = [], code = {} } = request.body || {};
    if (!Array.isArray(messages) || !messages.length) {
      return reply.code(400).send({ error: "messages fehlt." });
    }

    /* Sitzungsbremse: erst prüfen, dann arbeiten. Die Meldung nennt die
       Restzeit, damit niemand raten muss, wann es weitergeht. */
    const grenze = config.ai.agent;
    const verbrauch = await agentVerbrauch(request);
    if (!verbrauch.unbekannt) {
      const zuViele = verbrauch.anfragen >= grenze.maxRequests;
      const zuVielText = verbrauch.tokens >= grenze.maxTokens;
      if (zuViele || zuVielText) {
        return reply.code(429).send({
          error: zuViele
            ? `Kontingent erreicht: ${grenze.maxRequests} Anfragen an den Agenten in ${verbrauch.stunden} Stunden.`
            : `Kontingent erreicht: rund ${grenze.maxTokens} Token in ${verbrauch.stunden} Stunden.`,
          hinweis: "Alles andere läuft weiter — die Aufgabenprüfung hängt nicht am Agenten.",
          verbrauch: { anfragen: verbrauch.anfragen, tokens: verbrauch.tokens },
          grenze: { anfragen: grenze.maxRequests, tokens: grenze.maxTokens, stunden: verbrauch.stunden },
        });
      }
    }

    const istPro = rolle === config.ai.roles.assistPro;
    const codeLimit = istPro ? 2500 : 6000;
    const verlaufLimit = istPro ? (rolle.historyLimit || 4) : 8;
    /* Ein Bauauftrag soll vollständige Dateien liefern. In die Länge einer
       Antwort auf eine Frage passt keine ganze Seite — sie bricht dann
       mitten im HTML ab. */
    const istBau = request.body?.build === true;

    const context = ["html", "css", "js"]
      .map((k) => {
        const body = String(code[k] || "").trim();
        return body ? `\n--- ${k.toUpperCase()} ---\n${body.slice(0, codeLimit)}` : "";
      })
      .join("");
    const history = messages.slice(-verlaufLimit)
      .map((m) => `${m.role === "user" ? "Nutzer" : "Assistent"}: ${String(m.content || "").slice(0, istPro ? 1500 : 4000)}`)
      .join("\n\n");
    let userPrompt = `${context ? `Aktueller Code im Editor:${context}` : "Der Editor ist noch leer."}\n\n--- Verlauf ---\n${history}`;

    /* ------------------------ Der Disponent zuerst ----------------------
       Steht ein Modell auf eigener Hardware bereit, liest es die Anfrage und
       entscheidet, wer sie bearbeitet. Schlägt das fehl — nicht erreichbar,
       unbrauchbare Antwort —, bleibt es bei der bisherigen Wahl. Der Agent
       fällt dadurch nie aus.
       ------------------------------------------------------------------ */
    let entscheidung = null;
    let dispatchTokens = 0;
    if (disponent.enabled && providerReady(disponent.provider) && !willPro) {
      const letzte = String(messages[messages.length - 1]?.content || "").slice(0, 800);
      const lage = ["html", "css", "js"]
        .map((k) => `${k}: ${String(code[k] || "").trim().length} Zeichen`).join(", ");
      const frage = `Anfrage: ${letzte}\n\nStand im Editor: ${lage}`;
      try {
        const d = await generate({
          system: DISPATCH_SYSTEM_PROMPT, user: frage,
          maxTokens: disponent.maxTokens, provider: disponent.provider,
          model: disponent.model || undefined,
        });
        dispatchTokens = schaetzeTokens(DISPATCH_SYSTEM_PROMPT, frage, d.text);
        entscheidung = parseDispatch(d.text);
        await record(request, "dispatch", d, !!entscheidung, 200, dispatchTokens);
      } catch (e) {
        request.log.warn({ err: e }, "Disponent nicht erreichbar — direkter Weg");
      }
    }

    if (entscheidung) {
      if (entscheidung.ziel === "profi" && providerReady(config.ai.roles.assistPro.provider)) {
        rolle = config.ai.roles.assistPro;
        anbieter = rolle.provider;
      } else if (entscheidung.ziel === "lokal") {
        // Der Disponent macht es selbst — das verlässt das eigene Netz nicht.
        rolle = { provider: disponent.provider, model: disponent.model, maxTokens: disponent.localMaxTokens };
        anbieter = disponent.provider;
      }
      if (entscheidung.auftrag) {
        userPrompt = `Auftrag der Einsatzleitung: ${entscheidung.auftrag}\n\n${userPrompt}`;
      }
    }
    if (!anbieter) {
      return reply.code(503).send({ error: "Serverseitig ist keine KI konfiguriert." });
    }
    const istLokal = anbieter === disponent.provider && entscheidung?.ziel === "lokal";
    const istProJetzt = rolle === config.ai.roles.assistPro;

    let result;
    try {
      result = await generate({
        system: ASSISTANT_SYSTEM_PROMPT,
        user: userPrompt,
        maxTokens: istLokal ? disponent.localMaxTokens
          : istBau
            ? (istProJetzt ? Math.max(rolle.maxTokens || 700, 1600) : 2400)
            : (istProJetzt ? (rolle.maxTokens || 700) : 900),
        provider: anbieter,
        model: rolle.model || undefined,
      });
    } catch (e) {
      await record(request, istProJetzt ? "assist_pro" : "assist", null, false, e.status);
      return reply.code(502).send({ error: "Die KI ist momentan nicht erreichbar." });
    }
    const verbraucht = schaetzeTokens(ASSISTANT_SYSTEM_PROMPT, userPrompt, result.text);
    await record(request, istProJetzt ? "assist_pro" : "assist", result, true, 200, verbraucht);
    return {
      reply: String(result.text).slice(0, 8000),
      provider: result.provider,
      model: result.model || null,
      pro: istProJetzt,
      // Wer hat entschieden und warum — der Editor zeigt das an.
      disponiert: entscheidung
        ? { ziel: entscheidung.ziel, grund: entscheidung.grund }
        : null,
      // Damit der Editor anzeigen kann, wie viel vom Kontingent noch da ist.
      kontingent: {
        anfragen: verbrauch.anfragen + (entscheidung ? 2 : 1),
        maxAnfragen: grenze.maxRequests,
        tokens: verbrauch.tokens + verbraucht + dispatchTokens,
        maxTokens: grenze.maxTokens,
        stunden: verbrauch.stunden,
      },
    };
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

    // Bis zum Kontingent der Hauptanbieter, danach automatisch der Ersatz.
    const istHaupt = mine < v.primaryPerDay;
    const wanted = istHaupt ? v.primaryProvider : v.fallbackProvider;
    const provider = providerReady(wanted) ? wanted
      : providerReady(v.fallbackProvider) ? v.fallbackProvider
      : providerReady(config.ai.provider) ? config.ai.provider : null;
    if (!provider) return reply.code(503).send({ error: "Serverseitig ist keine KI konfiguriert.", tier: "local" });
    const modell = (provider === v.primaryProvider ? v.primaryModel : v.fallbackModel) || undefined;

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
      result = await generate({ system: VERIFY_SYSTEM_PROMPT, user: userPrompt, maxTokens: 320, provider, model: modell });
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
      model: result.model || null,
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
