import { config } from "../config.js";
import { one, many, query, transaction } from "../db.js";
import { requireAuth, updateStreak, rolloverLeague, weekKey } from "./auth.js";
import { loadFullUser, listUser, serializeProject, serializeLesson } from "../serialize.js";

const MAX_NAME = 80;

/* ----------------------- XP nach Ergebnis ---------------------------------
   Dieselben Zahlen wie im Browser (App.jsx). Entscheidend ist, dass HIER
   gerechnet wird: Der Client meldet nur den erreichten Score. Lügt er,
   bekommt er höchstens so viel wie bei ehrlicher Meldung — mehr als die volle
   Belohnung ist auf keinem Weg möglich.

   Es gibt genau einen Versuch je Aufgabe; die Belohnung richtet sich danach,
   wie viel der Aufgabe gelöst wurde.
   ------------------------------------------------------------------------- */
const TASK_XP = 15;
const HINT_FACTOR_CAP = 0.4;

function taskXpFor(score, usedHint) {
  const share = Math.max(0, Math.min(1, (Number(score) || 0) / 100));
  const factor = usedHint ? Math.min(share, HINT_FACTOR_CAP) : share;
  const xp = Math.round(TASK_XP * factor);
  return share > 0 ? Math.max(1, xp) : 0;
}

function lessonXpFor(base, scoreSum, total) {
  if (!total) return base;
  const avg = Math.max(0, Math.min(100, (Number(scoreSum) || 0) / Number(total)));
  return Math.max(0, Math.round(base * (avg / 100)));
}
const MAX_FILES = 100;          // ein Projekt, kein Dateisystem
const MAX_TASKS = 20;           // Aufgaben je selbst erstellter Lektion

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
    const { lessonId, courseId, xpReward = 0, badges = [], scoreSum, taskCount } = request.body || {};
    if (!lessonId) return reply.code(400).send({ error: "lessonId fehlt." });

    const claimed = Math.max(0, Math.min(500, Number(xpReward) || 0));   // Obergrenze gegen Manipulation
    // Der Lektionsbonus richtet sich nach dem Durchschnitt aller Aufgaben.
    // Gemeldet wird die Summe der Scores; gerechnet wird hier.
    const xp = taskCount
      ? Math.min(claimed, lessonXpFor(claimed, scoreSum, taskCount))
      : claimed;

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
        const reward = Number(beforeUser.boost_until || 0) > Date.now() ? xp * 2 : xp;
        await client.query("UPDATE users SET xp = xp + $1, weekly_xp = weekly_xp + $1 WHERE id = $2", [reward, request.user.id]);
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

  // XP für einzelne richtige Aufgaben. Der Doppel-XP-Kauf wird HIER angewandt,
  // nicht im Browser — sonst könnte man sich den Faktor selbst setzen.
  app.post("/api/progress/xp", { preHandler: [requireAuth] }, async (request, reply) => {
    const claimed = Math.max(0, Math.min(100, Number(request.body?.amount) || 0));
    if (!claimed) return reply.code(400).send({ error: "Ungültiger XP-Betrag." });
    // Der Abzug für eine unvollständige Lösung wird serverseitig gerechnet
    // und zusätzlich gegen den gemeldeten Betrag gedeckelt.
    const earned = taskXpFor(request.body?.score, !!request.body?.usedHint);
    const base = Math.min(claimed, earned);
    const before = await one("SELECT * FROM users WHERE id = $1", [request.user.id]);
    await rolloverLeague(before);
    const amount = Number(before.boost_until || 0) > Date.now() ? base * 2 : base;
    await query("UPDATE users SET xp = xp + $1, weekly_xp = weekly_xp + $1 WHERE id = $2", [amount, request.user.id]);
    return { user: await loadFullUser(request.user.id) };
  });

  /* ------------------------------- XP-Shop --------------------------------
     Preise und Obergrenzen stehen hier — im Browser lässt sich beides
     verändern, hier nicht. Ausgeben senkt nur `spent_xp`; `xp` bleibt stehen,
     damit Level und Rangliste unberührt bleiben.
     ---------------------------------------------------------------------- */
  const SHOP = {
    streak_freeze: { price: 200, kind: "stack", column: "streak_freezes", max: 3 },
    hint:          { price: 75,  kind: "stack", column: "hints",          max: 20 },
    xp_boost:      { price: 500, kind: "timed", hours: 24 },
    avatar_extras: { price: 600, kind: "unlock" },
    light_editor:  { price: 400, kind: "unlock" },
  };

  app.post("/api/shop/buy", { preHandler: [requireAuth] }, async (request, reply) => {
    const item = SHOP[String(request.body?.itemId || "")];
    if (!item) return reply.code(400).send({ error: "Diesen Artikel gibt es nicht." });

    try {
      await transaction(async (client) => {
        const { rows: [user] } = await client.query(
          "SELECT xp, spent_xp, hints, streak_freezes, unlocks, boost_until FROM users WHERE id = $1 FOR UPDATE",
          [request.user.id]
        );
        const balance = Number(user.xp || 0) - Number(user.spent_xp || 0);
        if (balance < item.price) {
          throw Object.assign(new Error(`Dafür fehlen dir noch ${item.price - balance} XP.`), { statusCode: 402 });
        }

        if (item.kind === "stack") {
          const owned = Number(user[item.column] || 0);
          if (owned >= item.max) {
            throw Object.assign(new Error(`Mehr als ${item.max} kannst du davon nicht halten.`), { statusCode: 409 });
          }
          await client.query(
            `UPDATE users SET spent_xp = spent_xp + $1, ${item.column} = ${item.column} + 1 WHERE id = $2`,
            [item.price, request.user.id]
          );
        } else if (item.kind === "unlock") {
          let unlocks = [];
          try { unlocks = JSON.parse(user.unlocks || "[]"); } catch (e) { unlocks = []; }
          if (unlocks.includes(request.body.itemId)) {
            throw Object.assign(new Error("Das hast du schon."), { statusCode: 409 });
          }
          unlocks.push(request.body.itemId);
          await client.query("UPDATE users SET spent_xp = spent_xp + $1, unlocks = $2 WHERE id = $3",
            [item.price, JSON.stringify(unlocks), request.user.id]);
        } else {
          // Ein zweiter Kauf hängt weitere Stunden an eine laufende Zeit an.
          const from = Math.max(Date.now(), Number(user.boost_until || 0));
          await client.query("UPDATE users SET spent_xp = spent_xp + $1, boost_until = $2 WHERE id = $3",
            [item.price, from + item.hours * 3600000, request.user.id]);
        }
      });
      return { user: await loadFullUser(request.user.id) };
    } catch (e) {
      if (e.statusCode) return reply.code(e.statusCode).send({ error: e.message });
      throw e;
    }
  });

  app.post("/api/shop/use-hint", { preHandler: [requireAuth] }, async (request, reply) => {
    const changed = await one(
      "UPDATE users SET hints = hints - 1 WHERE id = $1 AND hints > 0 RETURNING id",
      [request.user.id]
    );
    if (!changed) return reply.code(409).send({ error: "Du hast keinen Tipp-Joker mehr." });
    return { user: await loadFullUser(request.user.id) };
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

  /* --------------- Klasse: nachträglich beitreten (mit Freigabe) ----------
     Der Lehrer-Code allein verbindet nichts. Wer ihn eingibt, stellt eine
     Anfrage — erst mit der Bestätigung der Lehrkraft sieht sie Fortschritt
     und eigene Level werden freigeschaltet. Das schützt beide Seiten: Niemand
     landet ungefragt in einer fremden Klasse, und eine Lehrkraft bekommt
     keine fremden Schülerinnen und Schüler untergeschoben.
     --------------------------------------------------------------------- */
  app.post("/api/class/request", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "student") {
      return reply.code(403).send({ error: "Nur Schüler-Accounts können einer Klasse beitreten." });
    }
    const code = String(request.body?.code || "").trim();
    if (!code) return reply.code(400).send({ error: "Bitte gib einen Lehrer-Code ein." });

    const teacher = await one(
      "SELECT id, name, school FROM users WHERE role = 'teacher' AND upper(school_code) = upper($1) AND NOT disabled",
      [code]
    );
    if (!teacher) return reply.code(404).send({ error: "Diesen Lehrer-Code gibt es nicht." });
    if (teacher.id === request.user.teacher_id) {
      return reply.code(409).send({ error: "Du gehörst bereits zu dieser Klasse." });
    }

    // Eine offene Anfrage genügt — eine zweite ersetzt die erste.
    await query("UPDATE class_requests SET status = 'withdrawn', decided_at = now() WHERE student_id = $1 AND status = 'pending'", [request.user.id]);
    const row = await one(
      "INSERT INTO class_requests (student_id, teacher_id) VALUES ($1, $2) RETURNING *",
      [request.user.id, teacher.id]
    );
    return { request: { id: row.id, teacherName: teacher.name, school: teacher.school, status: "pending" } };
  });

  /** Der eigene Stand: läuft gerade eine Anfrage? */
  app.get("/api/class/request", { preHandler: [requireAuth] }, async (request) => {
    const row = await one(
      `SELECT r.id, r.status, u.name AS teacher_name, u.school
         FROM class_requests r JOIN users u ON u.id = r.teacher_id
        WHERE r.student_id = $1 AND r.status = 'pending'`,
      [request.user.id]
    );
    return { request: row ? { id: row.id, status: row.status, teacherName: row.teacher_name, school: row.school } : null };
  });

  /** Eine laufende Anfrage zurückziehen. */
  app.delete("/api/class/request", { preHandler: [requireAuth] }, async (request) => {
    await query("UPDATE class_requests SET status = 'withdrawn', decided_at = now() WHERE student_id = $1 AND status = 'pending'", [request.user.id]);
    return { ok: true };
  });

  /** Offene Anfragen an mich als Lehrkraft. */
  app.get("/api/teacher/requests", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "teacher") return reply.code(403).send({ error: "Nur für Lehrkräfte." });
    const rows = await many(
      `SELECT r.id, r.created_at, u.id AS student_id, u.name, u.email, u.xp
         FROM class_requests r JOIN users u ON u.id = r.student_id
        WHERE r.teacher_id = $1 AND r.status = 'pending'
        ORDER BY r.created_at`,
      [request.user.id]
    );
    return {
      requests: rows.map((r) => ({
        id: r.id, studentId: r.student_id, name: r.name, email: r.email,
        xp: Number(r.xp) || 0, createdAt: r.created_at,
      })),
    };
  });

  /** Anfrage bestätigen oder ablehnen. */
  app.post("/api/teacher/requests/:id", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "teacher") return reply.code(403).send({ error: "Nur für Lehrkräfte." });
    const approve = request.body?.approve === true;
    const row = await one(
      "SELECT * FROM class_requests WHERE id = $1 AND teacher_id = $2 AND status = 'pending'",
      [request.params.id, request.user.id]
    );
    if (!row) return reply.code(404).send({ error: "Diese Anfrage gibt es nicht mehr." });

    await query(
      "UPDATE class_requests SET status = $1, decided_at = now() WHERE id = $2",
      [approve ? "approved" : "rejected", row.id]
    );
    if (approve) {
      await query("UPDATE users SET teacher_id = $1 WHERE id = $2", [request.user.id, row.student_id]);
    }
    return { ok: true, approved: approve };
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
    const body = request.body || {};
    const { name } = body;
    if (!name?.trim()) return reply.code(400).send({ error: "Ein Projektname ist erforderlich." });

    // Neue Clients schicken eine Dateiliste, ältere die drei festen Felder.
    const incoming = Array.isArray(body.files) && body.files.length
      ? body.files
      : [
          ...(body.html ? [{ name: "index.html", content: body.html }] : []),
          ...(body.css ? [{ name: "style.css", content: body.css }] : []),
          ...(body.js ? [{ name: "script.js", content: body.js }] : []),
        ];

    if (incoming.length > MAX_FILES) {
      return reply.code(400).send({ error: `Ein Projekt darf höchstens ${MAX_FILES} Dateien enthalten.` });
    }
    const seen = new Set();
    const files = [];
    for (const raw of incoming) {
      const fileName = String(raw?.name || "").trim();
      if (!fileName) return reply.code(400).send({ error: "Jede Datei braucht einen Namen." });
      if (fileName.length > 60 || /[\\/:*?"<>|]/.test(fileName)) {
        return reply.code(400).send({ error: `Ungültiger Dateiname: ${fileName}` });
      }
      const key = fileName.toLowerCase();
      if (seen.has(key)) return reply.code(400).send({ error: `Doppelter Dateiname: ${fileName}` });
      seen.add(key);
      files.push({ name: fileName, content: String(raw?.content ?? "") });
    }

    const serialized = JSON.stringify(files);
    const size = byteLength(serialized);
    if (size > config.storage.maxProjectBytes) {
      return reply.code(413).send({ error: "Dieses Projekt ist zu groß." });
    }

    // html/css/js weiterhin befüllen — so bleiben ältere Datenbestände lesbar.
    const pick = (ext) => files.filter((f) => f.name.toLowerCase().endsWith(ext)).map((f) => f.content).join("\n");
    const html = pick(".html");
    const css = pick(".css");
    const js = pick(".js");

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
              `UPDATE projects SET name = $1, files = $2, html = $3, css = $4, js = $5, size_bytes = $6, updated_at = now()
               WHERE id = $7 AND user_id = $8 RETURNING *`,
              [name.trim().slice(0, 120), serialized, html, css, js, size, id, request.user.id])
          : await client.query(
              `INSERT INTO projects (user_id, name, files, html, css, js, size_bytes)
               VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *`,
              [request.user.id, name.trim().slice(0, 120), serialized, html, css, js, size]);

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

  /* ------------------- Eigene Lektionen (Lehrkräfte) ---------------------- */
  // Lehrkräfte legen eigene Level an; ihre Schülerinnen und Schüler sehen sie,
  // sobald sie veröffentlicht sind.
  const TASK_TYPES = ["multiple_choice", "fill_blank", "code_write", "explain"];

  /** Prüft und säubert die Aufgabenliste einer selbst erstellten Lektion. */
  function sanitizeTasks(input) {
    if (!Array.isArray(input)) throw Object.assign(new Error("Aufgaben fehlen."), { statusCode: 400 });
    if (input.length > MAX_TASKS) {
      throw Object.assign(new Error(`Höchstens ${MAX_TASKS} Aufgaben je Lektion.`), { statusCode: 400 });
    }
    return input.map((task, index) => {
      const type = TASK_TYPES.includes(task?.type) ? task.type : "multiple_choice";
      const question = String(task?.question || "").trim().slice(0, 500);
      if (!question) {
        throw Object.assign(new Error(`Aufgabe ${index + 1} hat keine Frage.`), { statusCode: 400 });
      }
      const base = { id: String(task?.id || `t${index + 1}`).slice(0, 20), type, question };

      if (type === "multiple_choice") {
        const options = (Array.isArray(task.options) ? task.options : []).slice(0, 6)
          .map((o) => String(o).slice(0, 200)).filter(Boolean);
        if (options.length < 2) {
          throw Object.assign(new Error(`Aufgabe ${index + 1} braucht mindestens zwei Antwortmöglichkeiten.`), { statusCode: 400 });
        }
        const correct = Number(task.correctAnswer);
        if (!Number.isInteger(correct) || correct < 0 || correct >= options.length) {
          throw Object.assign(new Error(`Aufgabe ${index + 1}: Bitte die richtige Antwort markieren.`), { statusCode: 400 });
        }
        return { ...base, options, correctAnswer: correct, explanation: String(task.explanation || "").slice(0, 500) };
      }
      if (type === "fill_blank") {
        const template = String(task.template || "").slice(0, 1000);
        const blanks = (Array.isArray(task.blanks) ? task.blanks : []).slice(0, 8)
          .map((b) => (Array.isArray(b) ? b.slice(0, 6).map((v) => String(v).slice(0, 100)) : String(b).slice(0, 100)));
        const gaps = template.split("___").length - 1;
        if (!gaps || gaps !== blanks.length) {
          throw Object.assign(new Error(`Aufgabe ${index + 1}: Für jede Lücke (___) braucht es genau eine Lösung.`), { statusCode: 400 });
        }
        return { ...base, template, blanks };
      }
      if (type === "code_write") {
        const concepts = (Array.isArray(task.expectedConcepts) ? task.expectedConcepts : []).slice(0, 12)
          .map((c) => (Array.isArray(c) ? c.slice(0, 5).map((v) => String(v).slice(0, 60)) : String(c).slice(0, 60)));
        return { ...base, starterCode: String(task.starterCode || "").slice(0, 2000), expectedConcepts: concepts };
      }
      return { ...base, expectedConcepts: (Array.isArray(task.expectedConcepts) ? task.expectedConcepts : []).slice(0, 12).map((c) => String(c).slice(0, 60)) };
    });
  }

  // Eigene Lektionen der Lehrkraft
  app.get("/api/lessons/mine", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "teacher") return reply.code(403).send({ error: "Nur für Lehrkräfte." });
    const rows = await many("SELECT * FROM custom_lessons WHERE teacher_id = $1 ORDER BY updated_at DESC", [request.user.id]);
    return { lessons: rows.map(serializeLesson) };
  });

  // Lektionen, die für mich sichtbar sind: die meiner Lehrkraft
  app.get("/api/lessons", { preHandler: [requireAuth] }, async (request) => {
    if (!request.user.teacher_id) return { lessons: [] };
    const rows = await many(
      `SELECT l.*, u.name AS teacher_name
         FROM custom_lessons l JOIN users u ON u.id = l.teacher_id
        WHERE l.teacher_id = $1 AND l.published
        ORDER BY l.created_at`,
      [request.user.teacher_id]
    );
    return { lessons: rows.map(serializeLesson) };
  });

  app.put("/api/lessons/:id?", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "teacher") return reply.code(403).send({ error: "Nur für Lehrkräfte." });
    const { title, courseId = "html", level = "beginner", xpReward = 50, theory = "", tasks, published = false } = request.body || {};
    if (!String(title || "").trim()) return reply.code(400).send({ error: "Die Lektion braucht einen Titel." });

    let cleanTasks;
    try { cleanTasks = sanitizeTasks(tasks); }
    catch (e) { return reply.code(e.statusCode || 400).send({ error: e.message }); }
    if (published && !cleanTasks.length) {
      return reply.code(400).send({ error: "Eine veröffentlichte Lektion braucht mindestens eine Aufgabe." });
    }

    const values = [
      String(title).trim().slice(0, 120),
      String(courseId).slice(0, 40),
      ["beginner", "intermediate", "advanced", "expert"].includes(level) ? level : "beginner",
      Math.max(0, Math.min(500, Number(xpReward) || 0)),
      String(theory).slice(0, 20000),
      JSON.stringify(cleanTasks),
      !!published,
    ];

    const id = request.params.id || null;
    const row = id
      ? await one(
          `UPDATE custom_lessons SET title = $1, course_id = $2, level = $3, xp_reward = $4,
                  theory = $5, tasks = $6, published = $7, updated_at = now()
            WHERE id = $8 AND teacher_id = $9 RETURNING *`,
          [...values, id, request.user.id])
      : await one(
          `INSERT INTO custom_lessons (title, course_id, level, xp_reward, theory, tasks, published, teacher_id)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
          [...values, request.user.id]);

    if (!row) return reply.code(404).send({ error: "Lektion nicht gefunden." });
    return { lesson: serializeLesson(row) };
  });

  app.delete("/api/lessons/:id", { preHandler: [requireAuth] }, async (request, reply) => {
    if (request.user.role !== "teacher") return reply.code(403).send({ error: "Nur für Lehrkräfte." });
    const row = await one("DELETE FROM custom_lessons WHERE id = $1 AND teacher_id = $2 RETURNING id", [request.params.id, request.user.id]);
    if (!row) return reply.code(404).send({ error: "Lektion nicht gefunden." });
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
