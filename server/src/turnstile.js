import { config } from "./config.js";

const VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

/**
 * Prüft ein Turnstile-Token serverseitig. Erst diese Prüfung macht den
 * Botschutz wirksam — das Widget allein ist nur eine Hürde im Browser.
 * Ohne konfiguriertes Secret wird die Prüfung übersprungen.
 */
export async function verifyTurnstile(token, remoteIp) {
  if (!config.turnstile.enabled) return { ok: true, skipped: true };
  if (!token) return { ok: false, reason: "Kein Botschutz-Token übermittelt" };

  const body = new URLSearchParams({ secret: config.turnstile.secret, response: token });
  if (remoteIp) body.set("remoteip", remoteIp);

  try {
    const res = await fetch(VERIFY_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
      signal: AbortSignal.timeout(8000),
    });
    const data = await res.json();
    return data.success
      ? { ok: true }
      : { ok: false, reason: "Botschutz-Prüfung fehlgeschlagen", codes: data["error-codes"] };
  } catch (e) {
    // Ist Cloudflare nicht erreichbar, wird der Login nicht blockiert —
    // Verfügbarkeit geht hier vor, der Rest der Absicherung bleibt aktiv.
    return { ok: true, degraded: true, reason: e.message };
  }
}
