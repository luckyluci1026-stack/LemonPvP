import { config } from "../config.js";
import { one, many, query, transaction } from "../db.js";
import {
  hashPassword, verifyPassword, createSessionToken, hashToken,
  numericCode, schoolCode, generateTotpSecret, verifyTotp, totpUri,
} from "../security.js";
import { verifyTurnstile } from "../turnstile.js";
import { checkPassword } from "../password.js";
import { sendMail, verificationMail, passwordChangedMail, passwordResetMail } from "../mailer.js";
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
    const strength = checkPassword(password, { name, email });
    if (!strength.ok) {
      return reply.code(400).send({ error: strength.problems.join(" "), passwordProblems: strength.problems });
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

  /* -------------------------- Passwort vergessen ------------------------ */
  // Antwortet immer gleich, egal ob die Adresse existiert — sonst ließe sich
  // darüber herausfinden, wer hier ein Konto hat.
  app.post("/api/auth/forgot-password", {
    config: { rateLimit: { max: 5, timeWindow: "1 hour" } },
  }, async (request) => {
    const email = String(request.body?.email || "").trim();
    const generic = { ok: true, message: "Falls ein Konto zu dieser Adresse existiert, wurde eine E-Mail verschickt." };
    if (!EMAIL_RE.test(email)) return generic;

    const user = await one("SELECT * FROM users WHERE email_lower = lower($1)", [email]);
    if (!user || user.disabled) {
      request.log.info({ email }, "Passwort-Anfrage für unbekannte oder gesperrte Adresse");
      return generic;
    }

    const { raw, hash } = createSessionToken();     // gleiche Erzeugung, gleicher Schutz
    await query(
      "UPDATE users SET reset_token_hash = $1, reset_expires = $2 WHERE id = $3",
      [hash, new Date(Date.now() + 3600_000), user.id]      // eine Stunde gültig
    );

    const link = `${config.publicUrl}/?reset=${raw}`;
    const delivery = await sendMail(request.log, { to: user.email, ...passwordResetMail(user.name, link, raw) });

    // Ohne Mailversand wird das Token zurückgegeben, damit sich der Ablauf
    // auch lokal ohne SMTP testen lässt.
    return delivery.delivered ? generic : { ...generic, devResetToken: raw };
  });

  app.post("/api/auth/reset-password", {
    config: { rateLimit: { max: 10, timeWindow: "1 hour" } },
  }, async (request, reply) => {
    const { token, newPassword } = request.body || {};
    if (!token) return reply.code(400).send({ error: "Kein Token übermittelt." });

    const user = await one(
      "SELECT * FROM users WHERE reset_token_hash = $1 AND reset_expires > now()",
      [hashToken(String(token))]
    );
    if (!user) {
      return reply.code(400).send({ error: "Der Link ist ungültig oder abgelaufen. Fordere einen neuen an." });
    }
    const strength = checkPassword(newPassword, { name: user.name, email: user.email });
    if (!strength.ok) {
      return reply.code(400).send({ error: strength.problems.join(" "), passwordProblems: strength.problems });
    }

    const hash = await hashPassword(newPassword);
    await transaction(async (client) => {
      await client.query(
        "UPDATE users SET password_hash = $1, reset_token_hash = NULL, reset_expires = NULL WHERE id = $2",
        [hash, user.id]
      );
      // Alle bestehenden Sitzungen beenden — falls jemand Fremdes drin war.
      await client.query("DELETE FROM sessions WHERE user_id = $1", [user.id]);
    });

    await sendMail(request.log, { to: user.email, ...passwordChangedMail(user.name) });
    request.log.info({ userId: user.id }, "Passwort zurückgesetzt");
    return { ok: true };
  });

  /* ---------------------------- Passwort ändern ------------------------- */
  app.post("/api/auth/change-password", { preHandler: [requireAuth] }, async (request, reply) => {
    const { currentPassword, newPassword } = request.body || {};
    const user = await one("SELECT * FROM users WHERE id = $1", [request.user.id]);
    if (!(await verifyPassword(String(currentPassword || ""), user.password_hash))) {
      return reply.code(401).send({ error: "Das aktuelle Passwort ist falsch." });
    }
    const strength = checkPassword(newPassword, { name: user.name, email: user.email });
    if (!strength.ok) {
      return reply.code(400).send({ error: strength.problems.join(" "), passwordProblems: strength.problems });
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

const dayKey = (d) => new Date(d).toISOString().slice(0, 10);

/** Kalenderwoche als Schlüssel, Wochenstart ist Montag. */
export function weekKey(date = new Date()) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  const week = Math.ceil(((d - new Date(d.getFullYear(), 0, 1)) / 86400_000 + 1) / 7);
  return `${d.getFullYear()}-KW${String(week).padStart(2, "0")}`;
}

/**
 * Zählt den Tages-Streak hoch bzw. setzt ihn zurück. Wurde genau ein Tag
 * verpasst und ist ein Streak-Schutz vorhanden, wird dieser eingelöst.
 */
export async function updateStreak(user) {
  const today = dayKey(Date.now());
  const last = user.last_active ? dayKey(user.last_active) : null;
  if (last === today) return { streak: user.streak, usedFreeze: false };

  let streak = 1;
  let freezes = user.streak_freezes || 0;
  let usedFreeze = false;

  if (last) {
    const gap = Math.round((new Date(today) - new Date(last)) / 86400_000);
    if (gap === 1) streak = user.streak + 1;
    else if (gap === 2 && freezes > 0) { streak = user.streak + 1; freezes -= 1; usedFreeze = true; }
  }

  await query(
    "UPDATE users SET streak = $1, last_active = CURRENT_DATE, streak_freezes = $2 WHERE id = $3",
    [streak, freezes, user.id]
  );
  return { streak, usedFreeze };
}

/** Setzt die Wochenwertung zurück und wendet Auf-/Abstieg an. */
const LEAGUE_ORDER = ["bronze", "silber", "gold", "platin", "diamant", "meister"];
const PROMOTE_TOP = 3;
const RELEGATE_BOTTOM = 3;

export async function rolloverLeague(user) {
  const current = weekKey();
  if (user.week_key === current) return user;

  let league = user.league || "bronze";
  const idx = LEAGUE_ORDER.indexOf(league);

  // Nur werten, wenn in der Vorwoche tatsächlich gelernt wurde
  if (user.week_key && (user.weekly_xp || 0) > 0) {
    const field = await many(
      `SELECT id, weekly_xp FROM users
        WHERE league = $1 AND week_key = $2 AND role = 'student' AND NOT disabled
        ORDER BY weekly_xp DESC`,
      [league, user.week_key]
    );
    const rank = field.findIndex((f) => f.id === user.id) + 1;
    if (rank > 0) {
      if (rank <= PROMOTE_TOP && idx < LEAGUE_ORDER.length - 1) league = LEAGUE_ORDER[idx + 1];
      else if (field.length > RELEGATE_BOTTOM && rank > field.length - RELEGATE_BOTTOM && idx > 0) league = LEAGUE_ORDER[idx - 1];
    }
  }

  await query("UPDATE users SET league = $1, weekly_xp = 0, week_key = $2 WHERE id = $3",
    [league, current, user.id]);
  return { ...user, league, weekly_xp: 0, week_key: current };
}
