/**
 * Ende-zu-Ende-Test gegen einen LAUFENDEN Server.
 *
 * Vorher in einem zweiten Terminal starten:
 *
 *   cd server
 *   rm -f data/test.db*
 *   npm run test:migrate
 *   npm run test:server
 *
 * Dann in einem anderen:  npm test
 *
 * Zwei Dinge setzen diese Skripte, die man leicht vergisst und deren
 * Fehlen sich als scheinbarer Fehler im Code zeigt:
 *
 *  - DATABASE_URL. Ohne sie greift dieser Testprozess auf die
 *    voreingestellte PostgreSQL-Adresse zu, nicht auf die Datenbank des
 *    Servers. Der before-Haken unten legt dann seinen Administrator
 *    woanders an, und jeder Test, der ihn braucht, bekommt eine 401.
 *  - --test-concurrency=1. Mehrere Testdateien räumen dieselben Tabellen
 *    leer; nebeneinander gestartet löscht die eine, was die andere gerade
 *    angelegt hat. Nacheinander ist es auch schnell genug.
 *
 * RATE_LIMIT=false ist ebenfalls gesetzt: Alle Testdateien kommen von
 * derselben Absenderadresse und teilen sich damit das Kontingent der
 * Anfragenbegrenzung. Mit ihr scheitert die Anmeldung ab einer gewissen
 * Zahl von Anfragen mit 429 — und zwar an der Suite selbst, nicht am Code.
 */
import { test, before } from "node:test";
import assert from "node:assert/strict";
import { totpCode, hashPassword } from "../src/security.js";
import { closeDb, query, usingSqlite } from "../src/db.js";

const BASE = "http://127.0.0.1:3111";
const ADMIN = { email: "admin@test.de", password: "Adminpass!7x" };

// Jeder Lauf startet mit einer definierten Datenlage, damit Tests nicht
// von Rückständen vorheriger Läufe abhängen.
before(async () => {
  if (usingSqlite) {
    // SQLite kennt kein TRUNCATE; Fremdschlüssel räumen den Rest auf.
    for (const t of ["ai_usage", "reports", "class_requests", "users"]) await query(`DELETE FROM ${t}`);
  } else {
    await query("TRUNCATE users, reports, ai_usage, class_requests RESTART IDENTITY CASCADE");
  }
  const hash = await hashPassword(ADMIN.password);
  await query(
    `INSERT INTO users (role, name, email, password_hash, email_verified, avatar)
     VALUES ('admin', 'Test Admin', $1, $2, TRUE, '')`,
    [ADMIN.email, hash]
  );
});
const jar = new Map();

function cookieHeader() {
  return [...jar.entries()].map(([k, v]) => `${k}=${v}`).join("; ");
}
function storeCookies(res) {
  const raw = res.headers.getSetCookie?.() || [];
  for (const c of raw) {
    const [pair] = c.split(";");
    const eq = pair.indexOf("=");
    jar.set(pair.slice(0, eq).trim(), pair.slice(eq + 1).trim());
  }
}
async function api(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: body === undefined
      ? { Cookie: cookieHeader() }
      : { "Content-Type": "application/json", Cookie: cookieHeader() },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  storeCookies(res);
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch { json = { raw: text }; }
  return { status: res.status, body: json };
}

const uniq = Date.now().toString(36);
const student = { name: "Test Schüler", email: `s_${uniq}@test.de`, password: "Supergeheim!7" };
let verificationCode = null;

test("Registrierung legt Konto an und startet Sitzung", async () => {
  const r = await api("POST", "/api/auth/register", { ...student, role: "student" });
  assert.equal(r.status, 200);
  assert.equal(r.body.user.email, student.email);
  assert.equal(r.body.user.emailVerified, false);
  assert.equal(r.body.user.role, "student");
  assert.ok(jar.has("ld_session"), "Sitzungs-Cookie wurde gesetzt");
  verificationCode = r.body.devVerificationCode;
  assert.ok(verificationCode, "Ohne SMTP wird der Code zurückgegeben");
});

test("Passwort wird niemals zurückgegeben", async () => {
  const r = await api("GET", "/api/auth/me");
  assert.equal(r.status, 200);
  const serialized = JSON.stringify(r.body);
  assert.ok(!serialized.includes("password"), "Kein Passwort-Feld in der Antwort");
  assert.ok(!serialized.includes("scrypt"), "Kein Hash in der Antwort");
  assert.ok(!serialized.includes("totp_secret"), "Kein TOTP-Geheimnis in der Antwort");
});

test("Doppelte Registrierung wird abgelehnt", async () => {
  const r = await api("POST", "/api/auth/register", { ...student, role: "student" });
  assert.equal(r.status, 409);
});

test("Zu schwaches Passwort wird abgelehnt", async () => {
  const r = await api("POST", "/api/auth/register", {
    name: "X", email: `kurz_${uniq}@test.de`, password: "1234", role: "student",
  });
  assert.equal(r.status, 400);
});

test("E-Mail-Verifizierung: falscher Code scheitert, richtiger klappt", async () => {
  const bad = await api("POST", "/api/auth/verify-email", { code: "000000" });
  assert.equal(bad.status, 400);
  const good = await api("POST", "/api/auth/verify-email", { code: verificationCode });
  assert.equal(good.status, 200);
  const me = await api("GET", "/api/auth/me");
  assert.equal(me.body.user.emailVerified, true);
});

test("Fortschritt: Lektion abschließen vergibt XP und Abzeichen", async () => {
  const r = await api("POST", "/api/progress/complete", {
    lessonId: "javascript_1_1", courseId: "javascript", xpReward: 75, badges: ["first_lesson"],
  });
  assert.equal(r.status, 200);
  assert.equal(r.body.isNew, true);
  assert.equal(r.body.user.xp, 75);
  assert.deepEqual(r.body.newBadges, ["first_lesson"]);
  assert.ok(r.body.user.completedLessons.includes("javascript_1_1"));
});

test("Dieselbe Lektion gibt kein zweites Mal XP", async () => {
  const r = await api("POST", "/api/progress/complete", {
    lessonId: "javascript_1_1", courseId: "javascript", xpReward: 75,
  });
  assert.equal(r.body.isNew, false);
  assert.equal(r.body.user.xp, 75, "XP bleibt unverändert");
});

test("XP-Betrag wird serverseitig begrenzt", async () => {
  const r = await api("POST", "/api/progress/complete", {
    lessonId: "javascript_1_2", xpReward: 999999,
  });
  assert.equal(r.body.user.xp, 75 + 500, "Auf 500 gedeckelt");
});

test("Ein schwacher Score senkt die XP für eine Aufgabe", async () => {
  const vorher = (await api("GET", "/api/auth/me")).body.user.xp;

  // Volle Punktzahl: volle 15 XP
  await api("POST", "/api/progress/xp", { amount: 15, score: 100 });
  const nachVoll = (await api("GET", "/api/auth/me")).body.user.xp;
  assert.equal(nachVoll - vorher, 15);

  // Die Hälfte richtig: die Hälfte der XP
  await api("POST", "/api/progress/xp", { amount: 15, score: 50 });
  const nachHalb = (await api("GET", "/api/auth/me")).body.user.xp;
  assert.equal(nachHalb - nachVoll, 8, "halber Score muss weniger geben");

  // Mit Tipp-Joker gedeckelt
  await api("POST", "/api/progress/xp", { amount: 15, score: 100, usedHint: true });
  const nachJoker = (await api("GET", "/api/auth/me")).body.user.xp;
  assert.equal(nachJoker - nachHalb, 6, "Joker muss deckeln");
});

test("Ein manipulierter Client bekommt trotzdem nicht mehr", async () => {
  const vorher = (await api("GET", "/api/auth/me")).body.user.xp;
  // Behauptet den vollen Betrag, meldet aber einen schwachen Score
  await api("POST", "/api/progress/xp", { amount: 100, score: 20 });
  const nachher = (await api("GET", "/api/auth/me")).body.user.xp;
  assert.equal(nachher - vorher, 3, "Server muss selbst rechnen");
});

test("Der Lektionsbonus richtet sich nach dem Durchschnitt", async () => {
  const vorher = (await api("GET", "/api/auth/me")).body.user.xp;
  // Vier Aufgaben mit im Schnitt 50 Punkten -> halber Bonus
  const r = await api("POST", "/api/progress/complete", {
    lessonId: "css_1_1", xpReward: 100, scoreSum: 200, taskCount: 4,
  });
  assert.equal(r.body.user.xp - vorher, 50);

  const zwischen = r.body.user.xp;
  // Fehlerfrei -> voller Bonus
  const r2 = await api("POST", "/api/progress/complete", {
    lessonId: "css_1_2", xpReward: 100, scoreSum: 400, taskCount: 4,
  });
  assert.equal(r2.body.user.xp - zwischen, 100);
});

test("Projekte: anlegen, lesen, löschen mit Speicherverrechnung", async () => {
  const created = await api("PUT", "/api/projects", {
    name: "Mein Projekt", html: "<h1>Hi</h1>", css: "body{}", js: "console.log(1)",
  });
  assert.equal(created.status, 200);
  const id = created.body.project.id;
  assert.ok(created.body.project.sizeBytes > 0);

  const list = await api("GET", "/api/projects");
  assert.equal(list.body.projects.length, 1);
  assert.equal(list.body.storage.used, created.body.project.sizeBytes);
  assert.ok(list.body.storage.quota > 0);

  const del = await api("DELETE", `/api/projects/${id}`);
  assert.equal(del.status, 200);
  const after = await api("GET", "/api/projects");
  assert.equal(after.body.projects.length, 0);
  assert.equal(after.body.storage.used, 0, "Speicher wurde freigegeben");
});

test("Fremde Projekte sind nicht löschbar", async () => {
  const r = await api("DELETE", "/api/projects/00000000-0000-0000-0000-000000000000");
  assert.equal(r.status, 404);
});

test("2FA: Einrichtung, Aktivierung und Login-Pflicht", async () => {
  const setup = await api("POST", "/api/auth/2fa/setup");
  assert.equal(setup.status, 200);
  assert.ok(setup.body.secret);
  assert.ok(setup.body.uri.startsWith("otpauth://totp/"));

  const wrong = await api("POST", "/api/auth/2fa/enable", { code: "000000" });
  assert.equal(wrong.status, 400);

  const ok = await api("POST", "/api/auth/2fa/enable", { code: totpCode(setup.body.secret) });
  assert.equal(ok.status, 200);

  await api("POST", "/api/auth/logout");
  jar.clear();

  const noCode = await api("POST", "/api/auth/login", { email: student.email, password: student.password });
  assert.equal(noCode.status, 401);
  assert.equal(noCode.body.need2fa, true);

  const withCode = await api("POST", "/api/auth/login", {
    email: student.email, password: student.password, totp: totpCode(setup.body.secret),
  });
  assert.equal(withCode.status, 200);
  assert.equal(withCode.body.user.twoFactorEnabled, true);
});

test("Login mit falschem Passwort scheitert", async () => {
  const saved = new Map(jar);
  jar.clear();
  const r = await api("POST", "/api/auth/login", { email: student.email, password: "FalschFalsch!7" });
  assert.equal(r.status, 401);
  assert.ok(!r.body.user);
  jar.clear();
  for (const [k, v] of saved) jar.set(k, v);
});

test("Ohne Anmeldung kein Zugriff auf geschützte Endpunkte", async () => {
  const saved = new Map(jar);
  jar.clear();
  for (const path of ["/api/auth/me", "/api/projects", "/api/admin/users"]) {
    const r = await api("GET", path);
    assert.equal(r.status, 401, `${path} muss 401 liefern`);
  }
  for (const [k, v] of saved) jar.set(k, v);
});

test("Schüler darf nicht auf Admin-Endpunkte", async () => {
  const r = await api("GET", "/api/admin/users");
  assert.equal(r.status, 403);
});

test("Meldung kann abgesetzt werden", async () => {
  const r = await api("POST", "/api/reports", {
    type: "ai_answer", lessonTitle: "JavaScript · Variablen",
    question: "Was ist const?", userAnswer: "<strong>", aiFeedback: "Falsch", reason: "War richtig",
  });
  assert.equal(r.status, 200);
});

test("Rangliste ist öffentlich und zeigt keine Geheimnisse", async () => {
  const saved = new Map(jar);
  jar.clear();
  const r = await api("GET", "/api/leaderboard");
  assert.equal(r.status, 200);
  assert.ok(Array.isArray(r.body.entries));
  assert.ok(!JSON.stringify(r.body).includes("scrypt"));
  for (const [k, v] of saved) jar.set(k, v);
});

test("Admin: anmelden, Nutzer sehen, Meldung bearbeiten", async () => {
  jar.clear();
  const login = await api("POST", "/api/auth/login", ADMIN);
  assert.equal(login.status, 200);
  assert.equal(login.body.user.role, "admin");

  const stats = await api("GET", "/api/admin/stats");
  assert.equal(stats.status, 200);
  assert.ok(stats.body.users.total >= 2);
  assert.ok(stats.body.openReports >= 1);

  const users = await api("GET", "/api/admin/users");
  assert.ok(users.body.users.length >= 2);
  assert.ok(!JSON.stringify(users.body).includes("scrypt"));

  const search = await api("GET", `/api/admin/users?q=${encodeURIComponent(student.email)}`);
  assert.equal(search.body.users.length, 1);
  const studentId = search.body.users[0].id;

  const patched = await api("PATCH", `/api/admin/users/${studentId}`, { name: "Umbenannt" });
  assert.equal(patched.status, 200);

  const reports = await api("GET", "/api/admin/reports?status=open");
  assert.ok(reports.body.reports.length >= 1);
  const resolved = await api("PATCH", `/api/admin/reports/${reports.body.reports[0].id}`, { status: "resolved" });
  assert.equal(resolved.status, 200);
});

test("Letzter Administrator kann nicht entfernt werden", async () => {
  const users = await api("GET", `/api/admin/users?q=${encodeURIComponent(ADMIN.email)}`);
  const adminId = users.body.users[0].id;
  const demote = await api("PATCH", `/api/admin/users/${adminId}`, { role: "student" });
  assert.equal(demote.status, 409, "Herabstufung wird verhindert");
});

test("Admin kann weiteren Admin anlegen", async () => {
  const r = await api("POST", "/api/admin/users", {
    name: "Zweiter Admin", email: `admin2_${uniq}@test.de`, password: "Nochngeheim!7", role: "admin",
  });
  assert.equal(r.status, 200);
  assert.equal(r.body.user.role, "admin");
});

test("KI-Status meldet fehlende Konfiguration", async () => {
  const r = await api("GET", "/api/ai/status");
  assert.equal(r.status, 200);
  assert.equal(r.body.available, false, "Ohne Keys keine KI");
  assert.equal(r.body.verify, false, "Ohne Keys auch keine Zweitmeinung");
  const assist = await api("POST", "/api/ai/assist", { messages: [{ role: "user", content: "hi" }] });
  assert.equal(assist.status, 503);
});

test("Ohne KI scheitert die Zweitmeinung sauber — die Lektion läuft lokal weiter", async () => {
  const r = await api("POST", "/api/ai/verify", {
    type: "code_write", question: "Schreibe eine Variable", answer: "const x = 1;",
    local: { score: 90, correct: true },
  });
  assert.equal(r.status, 503, "ohne Keys gibt es 503");
  assert.equal(r.body.tier, "local", "der Browser erfährt, dass lokal gewertet wird");
});

/* ---------------- Klasse beitreten: Anfrage und Freigabe ----------------- */
test("Lehrer-Code nachträglich: Anfrage, Freigabe, Zuordnung", async () => {
  // Eine Lehrkraft anlegen und ihren Code holen
  const lehrer = { name: "Frau Lehner", email: `lehrer_${uniq}@test.de`, password: "Unterricht!8k" };
  const reg = await api("POST", "/api/auth/register", { ...lehrer, role: "teacher", school: "Testschule" });
  assert.equal(reg.status, 200);
  const code = reg.body.schoolCode;
  assert.ok(code, "Lehrkraft bekommt einen Code");
  const lehrerCookies = new Map(jar);

  // Eine Schülerin ohne Lehrer-Code registrieren
  jar.clear();
  const schuelerin = { name: "Ida Klein", email: `schuelerin_${uniq}@test.de`, password: "Lernenist!9m" };
  const sreg = await api("POST", "/api/auth/register", { ...schuelerin, role: "student" });
  assert.equal(sreg.status, 200);
  assert.equal(sreg.body.user.teacherId, null, "zunächst keiner Klasse zugeordnet");
  const schuelerCookies = new Map(jar);

  // Falscher Code wird abgewiesen
  const falsch = await api("POST", "/api/class/request", { code: "GIBTSNICHT" });
  assert.equal(falsch.status, 404);

  // Richtiger Code erzeugt eine Anfrage — noch keine Zuordnung
  const anfrage = await api("POST", "/api/class/request", { code });
  assert.equal(anfrage.status, 200);
  assert.equal(anfrage.body.request.status, "pending");
  const nochNicht = await api("GET", "/api/auth/me");
  assert.equal(nochNicht.body.user.teacherId, null, "vor der Freigabe keine Zuordnung");

  const offen = await api("GET", "/api/class/request");
  assert.equal(offen.body.request.status, "pending");

  // Die Lehrkraft sieht die Anfrage
  jar.clear(); for (const [k, v] of lehrerCookies) jar.set(k, v);
  const liste = await api("GET", "/api/teacher/requests");
  assert.equal(liste.status, 200);
  assert.equal(liste.body.requests.length, 1);
  assert.equal(liste.body.requests[0].name, schuelerin.name);

  // …und bestätigt sie
  const ja = await api("POST", `/api/teacher/requests/${liste.body.requests[0].id}`, { approve: true });
  assert.equal(ja.status, 200);
  assert.equal(ja.body.approved, true);

  const klasse = await api("GET", "/api/teacher/students");
  assert.equal(klasse.body.students.length, 1);
  assert.equal(klasse.body.students[0].name, schuelerin.name);

  // Die Schülerin gehört jetzt zur Klasse, die Anfrage ist erledigt
  jar.clear(); for (const [k, v] of schuelerCookies) jar.set(k, v);
  const jetzt = await api("GET", "/api/auth/me");
  assert.ok(jetzt.body.user.teacherId, "nach der Freigabe zugeordnet");
  const keine = await api("GET", "/api/class/request");
  assert.equal(keine.body.request, null, "keine offene Anfrage mehr");
});

test("Eine abgelehnte Anfrage ordnet niemanden zu", async () => {
  const lehrer = { name: "Herr Ross", email: `lehrer2_${uniq}@test.de`, password: "Klassenraum!4z" };
  const reg = await api("POST", "/api/auth/register", { ...lehrer, role: "teacher", school: "Testschule" });
  const code = reg.body.schoolCode;
  const lehrerCookies = new Map(jar);

  jar.clear();
  await api("POST", "/api/auth/register", {
    name: "Ben Groß", email: `schueler2_${uniq}@test.de`, password: "Zweiterweg!6t", role: "student",
  });
  await api("POST", "/api/class/request", { code });

  jar.clear(); for (const [k, v] of lehrerCookies) jar.set(k, v);
  const liste = await api("GET", "/api/teacher/requests");
  const nein = await api("POST", `/api/teacher/requests/${liste.body.requests[0].id}`, { approve: false });
  assert.equal(nein.body.approved, false);
  const klasse = await api("GET", "/api/teacher/students");
  assert.equal(klasse.body.students.length, 0, "abgelehnt heißt nicht zugeordnet");
});

test("Aufräumen", async () => {
  await closeDb();
});
