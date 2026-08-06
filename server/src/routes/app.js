import { config } from "../config.js";
import { one, many, query, transaction } from "../db.js";
import { requireAuth, updateStreak, rolloverLeague, weekKey } from "./auth.js";
import { loadFullUser, listUser, serializeProject } from "../serialize.js";

const MAX_NAME = 80;

function byteLength(...parts) {
  return parts.reduce((sum, p) => sum + Buffer.byteLength(String(p || ""), "utf8"), 0);
}

export default async function appRoutes(app) {
  /* ----------------------------- Profil ---------------------------------- */
  app.patch("/api/profile", { preHandler: [requireAuth] }, async (request, reply) => {
    const { name, avatar, avatarConfig, currentCourse } = request.body || {};
    const sets = [];
    const values = [];
    let i = 1;

    if (name !== undefined) {
      const trimmed = String(name).trim();
      if (!trimmed || trimmed.length > MAX_NAME) {
        return reply.code(400).send({ error: "Der Name muss zwischen 1 und 80 Zeichen lang sein." });
      }
      sets.push(`name = $${i++}`); values.push(trimmed);
    }
    if (avatar !== undefined) { sets.push(`avatar = $${i++}`); values.push(String(avatar).slice(0, 16)); }
    if (avatarConfig !== undefined) { sets.push(`avatar_config = $${i++}`); values.push(avatarConfig ? JSON.stringify(avatarConfig) : null); }
    if (currentCourse !== undefined) { sets.push(`current_course = $${i++}`); values.push(currentCourse ? String(currentCourse).slice(0, 40) : null); }

    if (!sets.length) return { user: await loadFullUser(request.user.id) };
    values.push(request.user.id);
    await query(`UPDATE users SET ${sets.join(", ")} WHERE id = $${i}`, values);
    return { user: await loadFullUser(request.user.id) };
  });

  /* --------------------------- Fortschritt -------------------------------- */
  // Eine Lektion abschließen. XP und Abzeichen werden serverseitig vergeben,
  // damit der Fortschritt nicht im Browser manipuliert werden kann.
  app.post("/api/progress/complete", { preHandler: [requireAuth] }, async (request, reply) => {
    const { lessonId, courseId, xpReward = 0, badges = [] } = request.body || {};
    if (!lessonId) return reply.code(400).send({ error: "lessonId fehlt." });

    const xp = Math.max(0, Math.min(500, Number(xpReward) || 0));   // Obergrenze gegen Manipulation

    // Wochenwechsel VOR dem Gutschreiben abhandeln — sonst würde der
    // Rollover die soeben vergebenen Wochen-XP wieder auf null setzen.
    const beforeUser = await one("SELECT * FROM users WHERE id = $1", [request.user.id]);
    await rolloverLeague(beforeUser);

    const result = await transaction(async (client) => {
      const inserted = await client.query(
        `INSERT INTO completed_lessons (user_id, lesson_id, course_id)
         VALUES ($1, $2, $3) ON CONFLICT DO NOTHING RETURNING lesson_id`,
        [request.user.id, String(lessonId).slice(0, 60), courseId ? String(courseId).slice(0, 40) : null]
      );
      const isNew = inserted.rowCount > 0;
      if (isNew && xp > 0) {
        await client.query("UPDATE users SET xp = xp + $1, weekly_xp = weekly_xp + $1 WHERE id = $2", [xp, request.user.id]);
      }
      const earned = [];
      for (const badge of (Array.isArray(badges) ? badges : []).slice(0, 10)) {
        const r = await client.query(
          `INSERT INTO badges (user_id, badge_id) VALUES ($1, $2)
           ON CONFLICT DO NOTHING RETURNING badge_id`,
          [request.user.id, String(badge).slice(0, 40)]
        );
        if (r.rowCount > 0) earned.push(badge);
      }
      return { isNew, newBadges: earned };
    });

    const user = await one("SELECT * FROM users WHERE id = $1", [request.user.id]);
    const streakInfo = await updateStreak(user);
    return { ...result, ...streakInfo, user: await loadFullUser(request.user.id) };
  });

  // XP für einzelne richtige Aufgaben
  app.post("/api/progress/xp", { preHandler: [requireAuth] }, async (request, reply) => {
    const amount = Math.max(0, Math.min(100, Number(request.body?.amount) || 0));
    if (!amount) return reply.code(400).send({ error: "Ungültiger XP-Betrag." });
    const before = await one("SELECT * FROM users WHERE id = $1", [request.user.id]);
    await rolloverLeague(before);
    await query("UPDATE users SET xp = xp + $1, weekly_xp = weekly_xp + $1 WHERE id = $2", [amount, request.user.id]);
    return { user: await loadFullUser(request.user.id) };
  });

  // Streak-Schutz kaufen — Preis und Obergrenze werden serverseitig geprüft.
  app.post("/api/progress/streak-freeze", { preHandler: [requireAuth] }, async (request, reply) => {
    const FREEZE_COST = 200;
    const FREEZE_MAX = 3;
    try {
      await transaction(async (client) => {
        const { rows: [user] } = await client.query(
          "SELECT xp, streak_freezes FROM users WHERE id = $1 FOR UPDATE", [request.user.id]);
        if ((user.streak_freezes || 0) >= FREEZE_MAX) {
          throw Object.assign(new Error(`Mehr als ${FREEZE_MAX} Schutzschilde kannst du nicht halten.`), { statusCode: 409 });
        }
        if (user.xp < FREEZE_COST) {
          throw Object.assign(new Error("Dafür reichen deine XP nicht."), { statusCode: 402 });
        }
        await client.query(
          "UPDATE users SET xp = xp - $1, streak_freezes = streak_freezes + 1 WHERE id = $2",
          [FREEZE_COST, request.user.id]);
      });
      return { user: await loadFullUser(request.user.id) };
    } catch (e) {
      if (e.statusCode) return reply.code(e.statusCode).send({ error: e.message });
      throw e;
    }
  });

  /* ----------------------------- Rangliste -------------------------------- */
  app.get("/api/leaderboard", async (request) => {
    const limit = Math.min(100, Math.max(1, Number(request.query?.limit) || 50));
    const rows = await many(
      `SELECT u.*, COUNT(c.lesson_id) AS completed_count
         FROM users u
         LEFT JOIN completed_lessons c ON c.user_id = u.id
        WHERE u.role = 'student' AND NOT u.disabled
        GROUP BY u.id
        ORDER BY u.xp DESC, u.streak DESC
        LIMIT $1`,
      [limit]
    );
    return { entries: rows.map(listUser) };
  });

  /* ------------------------- Lehrer: Klassenübersicht --------------------- */
  app.get("/api/teacher/students", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "teacher") {
      return reply.code(403).send({ error: "Nur für Lehrkräfte." });
    }
    // Bewusst zwei einfache Abfragen statt einer mit Array-Aggregation:
    // array_agg gibt es nur in PostgreSQL, so läuft es auch auf SQLite.
    const rows = await many(
      `SELECT u.*, COUNT(c.lesson_id) AS completed_count
         FROM users u
         LEFT JOIN completed_lessons c ON c.user_id = u.id
        WHERE u.teacher_id = $1
        GROUP BY u.id
        ORDER BY u.name`,
      [request.user.id]
    );
    const lessons = await many(
      `SELECT c.user_id, c.lesson_id
         FROM completed_lessons c
         JOIN users u ON u.id = c.user_id
        WHERE u.teacher_id = $1`,
      [request.user.id]
    );
    const byUser = new Map();
    for (const l of lessons) {
      if (!byUser.has(l.user_id)) byUser.set(l.user_id, []);
      byUser.get(l.user_id).push(l.lesson_id);
    }
    return { students: rows.map((r) => listUser({ ...r, completed_lessons: byUser.get(r.id) || [] })) };
  });

  /* ------------------------------ Projekte -------------------------------- */
  app.get("/api/projects", { preHandler: [requireAuth] }, async (request) => {
    const rows = await many(
      "SELECT * FROM projects WHERE user_id = $1 ORDER BY updated_at DESC",
      [request.user.id]
    );
    const user = await one("SELECT storage_used, storage_quota FROM users WHERE id = $1", [request.user.id]);
    return {
      projects: rows.map(serializeProject),
      storage: { used: Number(user.storage_used), quota: Number(user.storage_quota) },
    };
  });

  // Anlegen oder aktualisieren; das Kontingent wird serverseitig durchgesetzt.
  app.put("/api/projects/:id?", { preHandler: [requireAuth] }, async (request, reply) => {
    const { name, html = "", css = "", js = "" } = request.body || {};
    if (!name?.trim()) return reply.code(400).send({ error: "Ein Projektname ist erforderlich." });

    const size = byteLength(html, css, js);
    if (size > config.storage.maxProjectBytes) {
      return reply.code(413).send({ error: "Dieses Projekt ist zu groß." });
    }

    const id = request.params.id || null;
    try {
      const project = await transaction(async (client) => {
        const { rows: [user] } = await client.query(
          "SELECT storage_used, storage_quota FROM users WHERE id = $1 FOR UPDATE",
          [request.user.id]
        );

        let previous = 0;
        if (id) {
          const { rows: [existing] } = await client.query(
            "SELECT size_bytes FROM projects WHERE id = $1 AND user_id = $2",
            [id, request.user.id]
          );
          if (!existing) throw Object.assign(new Error("Projekt nicht gefunden."), { statusCode: 404 });
          previous = Number(existing.size_bytes);
        }

        const nextUsed = Number(user.storage_used) - previous + size;
        if (nextUsed > Number(user.storage_quota)) {
          throw Object.assign(new Error("Speicherkontingent erschöpft — lösche zuerst ein Projekt."), { statusCode: 507 });
        }

        const saved = id
          ? await client.query(
              `UPDATE projects SET name = $1, html = $2, css = $3, js = $4, size_bytes = $5, updated_at = now()
               WHERE id = $6 AND user_id = $7 RETURNING *`,
              [name.trim().slice(0, 120), html, css, js, size, id, request.user.id])
          : await client.query(
              `INSERT INTO projects (user_id, name, html, css, js, size_bytes)
               VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
              [request.user.id, name.trim().slice(0, 120), html, css, js, size]);

        await client.query("UPDATE users SET storage_used = $1 WHERE id = $2", [nextUsed, request.user.id]);
        return saved.rows[0];
      });
      return { project: serializeProject(project) };
    } catch (e) {
      if (e.statusCode) return reply.code(e.statusCode).send({ error: e.message });
      throw e;
    }
  });

  app.delete("/api/projects/:id", { preHandler: [requireAuth] }, async (request, reply) => {
    const removed = await transaction(async (client) => {
      const { rows: [project] } = await client.query(
        "DELETE FROM projects WHERE id = $1 AND user_id = $2 RETURNING size_bytes",
        [request.params.id, request.user.id]
      );
      if (!project) return null;
      await client.query(
        "UPDATE users SET storage_used = GREATEST(0, storage_used - $1) WHERE id = $2",
        [Number(project.size_bytes), request.user.id]
      );
      return project;
    });
    if (!removed) return reply.code(404).send({ error: "Projekt nicht gefunden." });
    return { ok: true };
  });

  /* ------------------------------ Meldungen ------------------------------- */
  app.post("/api/reports", {
    preHandler: [requireAuth],
    config: { rateLimit: { max: 20, timeWindow: "1 hour" } },
  }, async (request) => {
    const { type = "ai_answer", lessonTitle, question, userAnswer, aiFeedback, reason } = request.body || {};
    const cut = (s, n) => (s ? String(s).slice(0, n) : null);
    await query(
      `INSERT INTO reports (reporter_id, type, lesson_title, question, user_answer, ai_feedback, reason)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [request.user.id, String(type).slice(0, 40), cut(lessonTitle, 200), cut(question, 1000),
       cut(userAnswer, 2000), cut(aiFeedback, 2000), cut(reason, 1000)]
    );
    return { ok: true };
  });
}
