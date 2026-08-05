import { config } from "../config.js";
import { one, many, query, transaction } from "../db.js";
import {
  hashPassword, verifyPassword, createSessionToken, hashToken,
  numericCode, schoolCode, generateTotpSecret, verifyTotp, totpUri,
} from "../security.js";
import { verifyTurnstile } from "../turnstile.js";
import { sendMail, verificationMail, passwordChangedMail } from "../mailer.js";
import { publicUser, loadFullUser } from "../serialize.js";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

function setSessionCookie(reply, token) {
  reply.setCookie(config.session.cookieName, token, {
    httpOnly: true,                 // für JavaScript im Browser unsichtbar
    sameSite: "lax",                // schützt vor Cross-Site-Request-Forgery
    secure: config.isProd,          // nur über HTTPS ausliefern
    path: "/",
    maxAge: config.session.ttlDays * 24 * 60 * 60,
  });
}

async function startSession(reply, request, userId) {
  const { raw, hash } = createSessionToken();
  const expires = new Date(Date.now() + config.session.ttlDays * 86400_000);
  await query(
    `INSERT INTO sessions (token_hash, user_id, user_agent, ip, expires_at)
     VALUES ($1, $2, $3, $4, $5)`,
    [hash, userId, String(request.headers["user-agent"] || "").slice(0, 300), request.ip, expires]
  );
  await query("UPDATE users SET last_login = now() WHERE id = $1", [userId]);
  setSessionCookie(reply, raw);
}

export default async function authRoutes(app) {
  /* ------------------------------ Registrierung ------------------------- */
  app.post("/api/auth/register", {
    config: { rateLimit: { max: 10, timeWindow: "1 hour" } },
  }, async (request, reply) => {
    const { name, email, password, role = "student", teacherCode, school, turnstileToken } = request.body || {};

    const captcha = await verifyTurnstile(turnstileToken, request.ip);
    if (!captcha.ok) return reply.code(400).send({ error: captcha.reason });

    if (!name?.trim() || !email?.trim() || !password) {
      return reply.code(400).send({ error: "Name, E-Mail und Passwort sind erforderlich." });
    }
    if (!EMAIL_RE.test(email.trim())) {
      return reply.code(400).send({ error: "Bitte gib eine gültige E-Mail-Adresse an." });
    }
    if (String(password).length < 8) {
      return reply.code(400).send({ error: "Das Passwort muss mindestens 8 Zeichen lang sein." });
    }
    if (!["student", "teacher"].includes(role)) {
      return reply.code(400).send({ error: "Ungültige Rolle." });
    }

    const existing = await one("SELECT id FROM users WHERE email_lower = lower($1)", [email.trim()]);
    if (existing) return reply.code(409).send({ error: "Diese E-Mail-Adresse ist bereits registriert." });

    let teacherId = null;
    let teacherHint = null;
    if (role === "student" && teacherCode?.trim()) {
      const teacher = await one(
        "SELECT id FROM users WHERE role = 'teacher' AND upper(school_code) = upper($1)",
        [teacherCode.trim()]
      );
      if (teacher) teacherId = teacher.id;
      else teacherHint = "Lehrer-Code nicht gefunden — du lernst zunächst selbstständig.";
    }

    const passwordHash = await hashPassword(password);
    const code = numericCode(6);
    const expires = new Date(Date.now() + 24 * 3600_000);
    const myCode = role === "teacher" ? schoolCode() : null;

    const user = await one(
      `INSERT INTO users (role, name, email, password_hash, verification_code, verification_expires,
                          teacher_id, school, school_code, storage_quota, streak, last_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 1, CURRENT_DATE)
       RETURNING *`,
      [role, name.trim(), email.trim(), passwordHash, code, expires,
       teacherId, school?.trim() || null, myCode, config.storage.quotaBytes]
    );

    const mail = verificationMail(user.name, code);
    const delivery = await sendMail(request.log, { to: user.email, ...mail });

    await startSession(reply, request, user.id);
    return {
      user: publicUser(user, { completedLessons: [], badges: [] }),
      teacherHint,
      // Ohne Mailversand wird der Code zurückgegeben, damit lokale Tests möglich bleiben.
      devVerificationCode: delivery.delivered ? undefined : code,
      schoolCode: myCode || undefined,
    };
  });

  /* --------------------------------- Login ------------------------------ */
  app.post("/api/auth/login", {
    config: { rateLimit: { max: 15, timeWindow: "10 minutes" } },
  }, async (request, reply) => {
    const { email, password, totp, turnstileToken } = request.body || {};

    const captcha = await verifyTurnstile(turnstileToken, request.ip);
    if (!captcha.ok) return reply.code(400).send({ error: captcha.reason });

    const user = await one("SELECT * FROM users WHERE email_lower = lower($1)", [String(email || "").trim()]);
    // Auch ohne Treffer wird gehasht, damit die Antwortzeit nichts verrät.
    const ok = user
      ? await verifyPassword(String(password || ""), user.password_hash)
      : await verifyPassword("dummy", "scrypt$32768$8$1$AAAAAAAAAAAAAAAAAAAAAA==$AAAA");
    if (!user || !ok) return reply.code(401).send({ error: "E-Mail oder Passwort ist falsch." });
    if (user.disabled) return reply.code(403).send({ error: "Dieses Konto wurde deaktiviert." });

    if (user.totp_enabled) {
      if (!totp) return reply.code(401).send({ error: "2FA-Code erforderlich.", need2fa: true });
      if (!verifyTotp(user.totp_secret, totp)) {
        return reply.code(401).send({ error: "Der 2FA-Code ist ungültig.", need2fa: true });
      }
    }

    await updateStreak(user);
    await startSession(reply, request, user.id);
    return { user: await loadFullUser(user.id) };
  });

  /* -------------------------------- Logout ------------------------------ */
  app.post("/api/auth/logout", async (request, reply) => {
    const raw = request.cookies?.[config.session.cookieName];
    if (raw) await query("DELETE FROM sessions WHERE token_hash = $1", [hashToken(raw)]);
    reply.clearCookie(config.session.cookieName, { path: "/" });
    return { ok: true };
  });

  /* ---------------------------- Aktuelles Konto ------------------------- */
  app.get("/api/auth/me", async (request, reply) => {
    if (!request.user) return reply.code(401).send({ error: "Nicht angemeldet." });
    return { user: await loadFullUser(request.user.id) };
  });

  /* -------------------------- E-Mail bestätigen ------------------------- */
  app.post("/api/auth/verify-email", { preHandler: [requireAuth] }, async (request, reply) => {
    const { code } = request.body || {};
    const user = await one("SELECT * FROM users WHERE id = $1", [request.user.id]);
    if (user.email_verified) return { ok: true, alreadyVerified: true };
    if (!user.verification_code || !user.verification_expires || new Date(user.verification_expires) < new Date()) {
      return reply.code(400).send({ error: "Der Code ist abgelaufen. Fordere einen neuen an." });
    }
    if (String(code || "").trim() !== user.verification_code) {
      return reply.code(400).send({ error: "Der Code stimmt nicht." });
    }
    await query(
      "UPDATE users SET email_verified = TRUE, verification_code = NULL, verification_expires = NULL WHERE id = $1",
      [user.id]
    );
    return { ok: true };
  });

  app.post("/api/auth/resend-verification", {
    preHandler: [requireAuth],
    config: { rateLimit: { max: 5, timeWindow: "1 hour" } },
  }, async (request) => {
    const user = await one("SELECT * FROM users WHERE id = $1", [request.user.id]);
    if (user.email_verified) return { ok: true, alreadyVerified: true };
    const code = numericCode(6);
    await query(
      "UPDATE users SET verification_code = $1, verification_expires = $2 WHERE id = $3",
      [code, new Date(Date.now() + 24 * 3600_000), user.id]
    );
    const delivery = await sendMail(request.log, { to: user.email, ...verificationMail(user.name, code) });
    return { ok: true, devVerificationCode: delivery.delivered ? undefined : code };
  });

  /* ---------------------------------- 2FA ------------------------------- */
  // Einrichtung: Geheimnis erzeugen, aber erst nach Bestätigung aktivieren.
  app.post("/api/auth/2fa/setup", { preHandler: [requireAuth] }, async (request) => {
    const secret = generateTotpSecret();
    await query("UPDATE users SET totp_secret = $1, totp_enabled = FALSE WHERE id = $2", [secret, request.user.id]);
    const user = await one("SELECT email FROM users WHERE id = $1", [request.user.id]);
    return { secret, uri: totpUri(secret, user.email) };
  });

  app.post("/api/auth/2fa/enable", { preHandler: [requireAuth] }, async (request, reply) => {
    const { code } = request.body || {};
    const user = await one("SELECT totp_secret FROM users WHERE id = $1", [request.user.id]);
    if (!user?.totp_secret) return reply.code(400).send({ error: "Richte 2FA zuerst ein." });
    if (!verifyTotp(user.totp_secret, code)) {
      return reply.code(400).send({ error: "Der Code ist ungültig. Prüfe die Uhrzeit deines Geräts." });
    }
    await query("UPDATE users SET totp_enabled = TRUE WHERE id = $1", [request.user.id]);
    return { ok: true };
  });

  app.post("/api/auth/2fa/disable", { preHandler: [requireAuth] }, async (request, reply) => {
    const { password } = request.body || {};
    const user = await one("SELECT password_hash FROM users WHERE id = $1", [request.user.id]);
    if (!(await verifyPassword(String(password || ""), user.password_hash))) {
      return reply.code(401).send({ error: "Passwort ist falsch." });
    }
    await query("UPDATE users SET totp_enabled = FALSE, totp_secret = NULL WHERE id = $1", [request.user.id]);
    return { ok: true };
  });

  /* ---------------------------- Passwort ändern ------------------------- */
  app.post("/api/auth/change-password", { preHandler: [requireAuth] }, async (request, reply) => {
    const { currentPassword, newPassword } = request.body || {};
    if (String(newPassword || "").length < 8) {
      return reply.code(400).send({ error: "Das neue Passwort muss mindestens 8 Zeichen lang sein." });
    }
    const user = await one("SELECT * FROM users WHERE id = $1", [request.user.id]);
    if (!(await verifyPassword(String(currentPassword || ""), user.password_hash))) {
      return reply.code(401).send({ error: "Das aktuelle Passwort ist falsch." });
    }
    const hash = await hashPassword(newPassword);
    await transaction(async (client) => {
      await client.query("UPDATE users SET password_hash = $1 WHERE id = $2", [hash, user.id]);
      // Alle anderen Sitzungen beenden
      const current = request.cookies?.[config.session.cookieName];
      await client.query("DELETE FROM sessions WHERE user_id = $1 AND token_hash <> $2",
        [user.id, current ? hashToken(current) : ""]);
    });
    await sendMail(request.log, { to: user.email, ...passwordChangedMail(user.name) });
    return { ok: true };
  });
}

/* ------------------------------- Helfer --------------------------------- */
export async function requireAuth(request, reply) {
  if (!request.user) return reply.code(401).send({ error: "Nicht angemeldet." });
}

export async function requireAdmin(request, reply) {
  if (!request.user) return reply.code(401).send({ error: "Nicht angemeldet." });
  if (request.user.role !== "admin") return reply.code(403).send({ error: "Nur für Administratoren." });
}

/** Zählt den Tages-Streak hoch bzw. setzt ihn zurück. */
export async function updateStreak(user) {
  const today = new Date().toISOString().slice(0, 10);
  const last = user.last_active ? new Date(user.last_active).toISOString().slice(0, 10) : null;
  if (last === today) return user.streak;

  const yesterday = new Date(Date.now() - 86400_000).toISOString().slice(0, 10);
  const streak = last === yesterday ? user.streak + 1 : 1;
  await query("UPDATE users SET streak = $1, last_active = CURRENT_DATE WHERE id = $2", [streak, user.id]);
  return streak;
}
