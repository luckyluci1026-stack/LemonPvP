import nodemailer from "nodemailer";
import { config } from "./config.js";

let transport = null;

function getTransport() {
  if (!config.mail.enabled) return null;
  if (!transport) {
    transport = nodemailer.createTransport({
      host: config.mail.host,
      port: config.mail.port,
      secure: config.mail.secure,
      auth: config.mail.user ? { user: config.mail.user, pass: config.mail.pass } : undefined,
    });
  }
  return transport;
}

/**
 * Versendet eine Mail. Ist SMTP nicht konfiguriert, wird der Inhalt nur
 * geloggt — die Anwendung bleibt so auch ohne Mailserver benutzbar.
 */
export async function sendMail(log, { to, subject, text, html }) {
  const t = getTransport();
  if (!t) {
    log.warn({ to, subject }, `SMTP deaktiviert — Mail nicht versendet. Inhalt: ${text}`);
    return { delivered: false };
  }
  try {
    await t.sendMail({ from: config.mail.from, to, subject, text, html });
    log.info({ to, subject }, "Mail versendet");
    return { delivered: true };
  } catch (e) {
    log.error({ err: e, to }, "Mailversand fehlgeschlagen");
    return { delivered: false, error: e.message };
  }
}

const shell = (title, body) => `<!doctype html>
<html lang="de"><body style="margin:0;background:#0A0E1A;font-family:Inter,system-ui,sans-serif;color:#E8EDF5">
  <div style="max-width:520px;margin:0 auto;padding:32px 24px">
    <div style="font-size:20px;font-weight:700;margin-bottom:24px">Learn<span style="color:#4F8EF7">Developing</span></div>
    <div style="background:#141D35;border:1px solid #1E2D4A;border-radius:12px;padding:24px">
      <h1 style="margin:0 0 12px;font-size:20px">${title}</h1>
      ${body}
    </div>
    <p style="color:#4A5A7A;font-size:12px;margin-top:20px">
      Diese Nachricht wurde automatisch versendet. Fragen? ${config.mail.supportAddress}
    </p>
  </div>
</body></html>`;

export function verificationMail(name, code) {
  return {
    subject: "Bestätige deine E-Mail-Adresse",
    text: `Hallo ${name},\n\ndein Bestätigungscode lautet: ${code}\n\nDer Code ist 24 Stunden gültig.\n\nFalls du dich nicht registriert hast, ignoriere diese Nachricht.`,
    html: shell("Bestätige deine E-Mail-Adresse", `
      <p style="color:#8A9BC0;line-height:1.6;margin:0 0 16px">Hallo ${name}, gib diesen Code in der App ein:</p>
      <div style="background:#0A0E1A;border:1px solid #2A3F6F;border-radius:12px;padding:16px;text-align:center;
                  font-size:30px;font-weight:800;letter-spacing:6px;color:#4F8EF7;font-family:monospace">${code}</div>
      <p style="color:#4A5A7A;font-size:13px;margin:16px 0 0">Gültig für 24 Stunden. Falls du dich nicht registriert hast, ignoriere diese Nachricht.</p>`),
  };
}

export function passwordResetMail(name, link, token) {
  return {
    subject: "Passwort zurücksetzen",
    text: `Hallo ${name},\n\nüber diesen Link setzt du dein Passwort neu:\n${link}\n\nFalls der Link nicht funktioniert, gib diesen Code in der App ein:\n${token}\n\nDer Link ist eine Stunde gültig. Hast du das nicht angefordert, ignoriere diese Nachricht — dein Passwort bleibt unverändert.`,
    html: shell("Passwort zurücksetzen", `
      <p style="color:#8A9BC0;line-height:1.6;margin:0 0 16px">Hallo ${name}, hier kannst du ein neues Passwort vergeben:</p>
      <p style="margin:0 0 16px">
        <a href="${link}" style="display:inline-block;background:linear-gradient(135deg,#4F8EF7,#7C3AED);
           color:#fff;text-decoration:none;padding:12px 24px;border-radius:8px;font-weight:600">Neues Passwort vergeben</a>
      </p>
      <p style="color:#4A5A7A;font-size:13px;margin:0 0 8px">Falls der Knopf nicht funktioniert, nutze diesen Code:</p>
      <div style="background:#0A0E1A;border:1px solid #2A3F6F;border-radius:8px;padding:12px;
                  font-family:monospace;font-size:12px;color:#4F8EF7;word-break:break-all">${token}</div>
      <p style="color:#4A5A7A;font-size:13px;margin:16px 0 0">
        Gültig für eine Stunde. Hast du das nicht angefordert, ignoriere diese Nachricht — dein Passwort bleibt unverändert.
      </p>`),
  };
}

export function passwordChangedMail(name) {
  return {
    subject: "Dein Passwort wurde geändert",
    text: `Hallo ${name},\n\ndas Passwort deines Kontos wurde soeben geändert.\n\nWarst du das nicht, melde dich umgehend bei ${config.mail.supportAddress}.`,
    html: shell("Dein Passwort wurde geändert", `
      <p style="color:#8A9BC0;line-height:1.6;margin:0">Hallo ${name}, das Passwort deines Kontos wurde soeben geändert.</p>
      <p style="color:#F59E0B;line-height:1.6;margin:12px 0 0">Warst du das nicht, melde dich umgehend bei ${config.mail.supportAddress}.</p>`),
  };
}
