import { test, before } from "node:test";
import assert from "node:assert/strict";
import { hashPassword } from "../src/security.js";
import { closeDb, query, usingSqlite } from "../src/db.js";

const BASE = "http://127.0.0.1:3111";
const jar = new Map();

function cookieHeader() { return [...jar.entries()].map(([k, v]) => `${k}=${v}`).join("; "); }
function storeCookies(res) {
  for (const c of res.headers.getSetCookie?.() || []) {
    const [pair] = c.split(";"); const eq = pair.indexOf("=");
    jar.set(pair.slice(0, eq).trim(), pair.slice(eq + 1).trim());
  }
}
async function api(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: body === undefined ? { Cookie: cookieHeader() } : { "Content-Type": "application/json", Cookie: cookieHeader() },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  storeCookies(res);
  const text = await res.text();
  let json = null; try { json = text ? JSON.parse(text) : null; } catch { json = { raw: text }; }
  return { status: res.status, body: json };
}

const uniq = Date.now().toString(36);
const user = { name: "Reset Tester", email: `reset_${uniq}@test.de`, password: "altespasswort123" };
let token = null;

before(async () => {
  if (usingSqlite) { for (const t of ["ai_usage", "reports", "users"]) await query(`DELETE FROM ${t}`); }
  else await query("TRUNCATE users, reports, ai_usage RESTART IDENTITY CASCADE");
});

test("Konto anlegen und abmelden", async () => {
  const r = await api("POST", "/api/auth/register", { ...user, role: "student" });
  assert.equal(r.status, 200);
  await api("POST", "/api/auth/logout");
  jar.clear();
});

test("Unbekannte Adresse verrät nichts", async () => {
  const r = await api("POST", "/api/auth/forgot-password", { email: `gibtsnicht_${uniq}@test.de` });
  assert.equal(r.status, 200);
  assert.match(r.body.message, /Falls ein Konto/);
  assert.equal(r.body.devResetToken, undefined, "Kein Token für unbekannte Adresse");
});

test("Bekannte Adresse liefert Token (ohne SMTP)", async () => {
  const r = await api("POST", "/api/auth/forgot-password", { email: user.email });
  assert.equal(r.status, 200);
  // Die Nachricht ist identisch zur unbekannten Adresse — keine Preisgabe.
  assert.match(r.body.message, /Falls ein Konto/);
  token = r.body.devResetToken;
  assert.ok(token, "Ohne Mailversand wird das Token zurückgegeben");
});

test("Ungültiges Token wird abgelehnt", async () => {
  const r = await api("POST", "/api/auth/reset-password", { token: "quatsch", newPassword: "neuespasswort123" });
  assert.equal(r.status, 400);
});

test("Zu kurzes Passwort wird abgelehnt", async () => {
  const r = await api("POST", "/api/auth/reset-password", { token, newPassword: "kurz" });
  assert.equal(r.status, 400);
});

test("Gültiges Token setzt das Passwort neu", async () => {
  const r = await api("POST", "/api/auth/reset-password", { token, newPassword: "neuespasswort123" });
  assert.equal(r.status, 200);
});

test("Altes Passwort funktioniert nicht mehr", async () => {
  jar.clear();
  const r = await api("POST", "/api/auth/login", { email: user.email, password: user.password });
  assert.equal(r.status, 401);
});

test("Neues Passwort funktioniert", async () => {
  jar.clear();
  const r = await api("POST", "/api/auth/login", { email: user.email, password: "neuespasswort123" });
  assert.equal(r.status, 200);
  assert.equal(r.body.user.email, user.email);
});

test("Token ist nach Einlösung verbraucht", async () => {
  const r = await api("POST", "/api/auth/reset-password", { token, newPassword: "nochmalanders123" });
  assert.equal(r.status, 400, "Einmal-Token darf nicht erneut gelten");
});

test("Abgelaufenes Token wird abgelehnt", async () => {
  const fresh = await api("POST", "/api/auth/forgot-password", { email: user.email });
  const t = fresh.body.devResetToken;
  // Ablaufzeitpunkt in die Vergangenheit setzen
  await query("UPDATE users SET reset_expires = $1 WHERE email_lower = lower($2)",
    [new Date(Date.now() - 1000), user.email]);
  const r = await api("POST", "/api/auth/reset-password", { token: t, newPassword: "wiederanders123" });
  assert.equal(r.status, 400);
});

test("Zurücksetzen beendet alle bestehenden Sitzungen", async () => {
  jar.clear();
  await api("POST", "/api/auth/login", { email: user.email, password: "neuespasswort123" });
  const before = await api("GET", "/api/auth/me");
  assert.equal(before.status, 200, "vorher angemeldet");

  const req = await api("POST", "/api/auth/forgot-password", { email: user.email });
  await api("POST", "/api/auth/reset-password", { token: req.body.devResetToken, newPassword: "ganzneuespw123" });

  const after = await api("GET", "/api/auth/me");
  assert.equal(after.status, 401, "Sitzung wurde beendet");
});

test("Aufräumen", async () => { await closeDb(); });
