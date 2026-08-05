import { config } from "../config.js";
import { one, many, query } from "../db.js";
import { requireAdmin } from "./auth.js";
import { hashPassword } from "../security.js";
import { listUser, serializeReport } from "../serialize.js";
import { poolStatus, aiAvailable } from "../ai.js";

export default async function adminRoutes(app) {
  /* ------------------------------ Übersicht ------------------------------- */
  app.get("/api/admin/stats", { preHandler: [requireAdmin] }, async () => {
    const [counts, openReports, aiToday] = await Promise.all([
      one(`SELECT
             COUNT(*) FILTER (WHERE role = 'student') AS students,
             COUNT(*) FILTER (WHERE role = 'teacher') AS teachers,
             COUNT(*) FILTER (WHERE role = 'admin')   AS admins,
             COUNT(*) FILTER (WHERE last_login > now() - interval '24 hours') AS active_today,
             COUNT(*) AS total
           FROM users`),
      one("SELECT COUNT(*) AS n FROM reports WHERE status = 'open'"),
      one(`SELECT COUNT(*) FILTER (WHERE ok) AS ok, COUNT(*) FILTER (WHERE NOT ok) AS failed
             FROM ai_usage WHERE created_at > now() - interval '24 hours'`),
    ]);
    return {
      users: {
        students: Number(counts.students), teachers: Number(counts.teachers),
        admins: Number(counts.admins), activeToday: Number(counts.active_today),
        total: Number(counts.total),
      },
      openReports: Number(openReports.n),
      ai: { available: aiAvailable(), ok24h: Number(aiToday.ok), failed24h: Number(aiToday.failed), pool: poolStatus() },
    };
  });

  /* ------------------------- Nutzer suchen/verwalten ---------------------- */
  app.get("/api/admin/users", { preHandler: [requireAdmin] }, async (request) => {
    const q = String(request.query?.q || "").trim();
    const limit = Math.min(200, Math.max(1, Number(request.query?.limit) || 50));
    const rows = await many(
      `SELECT u.*, COUNT(c.lesson_id) AS completed_count
         FROM users u
         LEFT JOIN completed_lessons c ON c.user_id = u.id
        WHERE ($1 = '' OR u.name ILIKE '%' || $1 || '%' OR u.email ILIKE '%' || $1 || '%')
        GROUP BY u.id
        ORDER BY u.created_at DESC
        LIMIT $2`,
      [q, limit]
    );
    return { users: rows.map(listUser) };
  });

  app.patch("/api/admin/users/:id", { preHandler: [requireAdmin] }, async (request, reply) => {
    const { name, email, role, disabled, emailVerified } = request.body || {};
    const target = await one("SELECT * FROM users WHERE id = $1", [request.params.id]);
    if (!target) return reply.code(404).send({ error: "Konto nicht gefunden." });

    // Der letzte verbleibende Admin darf sich nicht selbst herabstufen oder sperren.
    if (target.role === "admin" && (role && role !== "admin" || disabled === true)) {
      const { n } = await one("SELECT COUNT(*) AS n FROM users WHERE role = 'admin' AND NOT disabled");
      if (Number(n) <= 1) {
        return reply.code(409).send({ error: "Der letzte aktive Administrator kann nicht entfernt werden." });
      }
    }

    const sets = [];
    const values = [];
    let i = 1;
    if (name !== undefined) { sets.push(`name = $${i++}`); values.push(String(name).trim().slice(0, 80)); }
    if (email !== undefined) {
      const clean = String(email).trim();
      const clash = await one("SELECT id FROM users WHERE email_lower = lower($1) AND id <> $2", [clean, target.id]);
      if (clash) return reply.code(409).send({ error: "Diese E-Mail-Adresse ist bereits vergeben." });
      sets.push(`email = $${i++}`); values.push(clean);
    }
    if (role !== undefined) {
      if (!["student", "teacher", "admin"].includes(role)) {
        return reply.code(400).send({ error: "Ungültige Rolle." });
      }
      sets.push(`role = $${i++}`); values.push(role);
    }
    if (disabled !== undefined) { sets.push(`disabled = $${i++}`); values.push(!!disabled); }
    if (emailVerified !== undefined) { sets.push(`email_verified = $${i++}`); values.push(!!emailVerified); }
    if (!sets.length) return { ok: true };

    values.push(target.id);
    await query(`UPDATE users SET ${sets.join(", ")} WHERE id = $${i}`, values);
    // Gesperrte Konten sofort abmelden
    if (disabled === true) await query("DELETE FROM sessions WHERE user_id = $1", [target.id]);
    return { ok: true };
  });

  app.delete("/api/admin/users/:id", { preHandler: [requireAdmin] }, async (request, reply) => {
    if (request.params.id === request.user.id) {
      return reply.code(400).send({ error: "Du kannst dein eigenes Konto hier nicht löschen." });
    }
    const target = await one("SELECT role FROM users WHERE id = $1", [request.params.id]);
    if (!target) return reply.code(404).send({ error: "Konto nicht gefunden." });
    if (target.role === "admin") {
      const { n } = await one("SELECT COUNT(*) AS n FROM users WHERE role = 'admin' AND NOT disabled");
      if (Number(n) <= 1) return reply.code(409).send({ error: "Der letzte Administrator kann nicht gelöscht werden." });
    }
    await query("DELETE FROM users WHERE id = $1", [request.params.id]);
    return { ok: true };
  });

  /* ---------------------- Weiteren Administrator anlegen ------------------ */
  app.post("/api/admin/users", { preHandler: [requireAdmin] }, async (request, reply) => {
    const { name, email, password, role = "admin" } = request.body || {};
    if (!name?.trim() || !email?.trim() || !password) {
      return reply.code(400).send({ error: "Name, E-Mail und Passwort sind erforderlich." });
    }
    if (String(password).length < 8) {
      return reply.code(400).send({ error: "Das Passwort muss mindestens 8 Zeichen lang sein." });
    }
    if (!["student", "teacher", "admin"].includes(role)) {
      return reply.code(400).send({ error: "Ungültige Rolle." });
    }
    const clash = await one("SELECT id FROM users WHERE email_lower = lower($1)", [email.trim()]);
    if (clash) return reply.code(409).send({ error: "Diese E-Mail-Adresse ist bereits vergeben." });

    const hash = await hashPassword(password);
    const created = await one(
      `INSERT INTO users (role, name, email, password_hash, email_verified, avatar, storage_quota)
       VALUES ($1, $2, $3, $4, TRUE, $5, $6) RETURNING *`,
      [role, name.trim(), email.trim(), hash, role === "admin" ? "🛡️" : "🧑‍💻", config.storage.quotaBytes]
    );
    request.log.info({ actor: request.user.id, created: created.id, role }, "Konto durch Administrator angelegt");
    return { user: listUser({ ...created, completed_count: 0 }) };
  });

  /* ------------------------------ Meldungen ------------------------------- */
  app.get("/api/admin/reports", { preHandler: [requireAdmin] }, async (request) => {
    const status = request.query?.status;
    const rows = await many(
      `SELECT r.*, u.name AS reporter_name
         FROM reports r LEFT JOIN users u ON u.id = r.reporter_id
        WHERE ($1::text IS NULL OR r.status = $1)
        ORDER BY (r.status = 'open') DESC, r.created_at DESC
        LIMIT 200`,
      [status === "open" || status === "resolved" ? status : null]
    );
    return { reports: rows.map(serializeReport) };
  });

  app.patch("/api/admin/reports/:id", { preHandler: [requireAdmin] }, async (request, reply) => {
    const status = request.body?.status;
    if (!["open", "resolved"].includes(status)) {
      return reply.code(400).send({ error: "Ungültiger Status." });
    }
    const { rowCount } = await query(
      "UPDATE reports SET status = $1, resolved_at = CASE WHEN $1 = 'resolved' THEN now() ELSE NULL END WHERE id = $2",
      [status, request.params.id]
    );
    if (!rowCount) return reply.code(404).send({ error: "Meldung nicht gefunden." });
    return { ok: true };
  });

  app.delete("/api/admin/reports/:id", { preHandler: [requireAdmin] }, async (request, reply) => {
    const { rowCount } = await query("DELETE FROM reports WHERE id = $1", [request.params.id]);
    if (!rowCount) return reply.code(404).send({ error: "Meldung nicht gefunden." });
    return { ok: true };
  });
}
