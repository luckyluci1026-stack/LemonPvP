/**
 * Mindestanforderungen an Passwörter.
 *
 * Die Regeln sind bewusst dieselben wie im Browser (App.jsx). Geprüft wird
 * aber IMMER hier — die Prüfung im Formular ist nur eine Hilfe für die
 * Nutzerin, verlassen darf man sich darauf nicht.
 */

export const PASSWORD_MIN_LENGTH = 10;

// Die Klassiker aus jeder Leak-Liste. Kurz gehalten: die Längen- und
// Zeichenregeln fangen den Rest bereits ab.
const COMMON = new Set([
  "passwort", "password", "passwort1", "password1", "passwort123", "password123",
  "12345678", "123456789", "1234567890", "qwertzuiop", "qwertyuiop", "asdfghjkl",
  "hallo123", "willkommen", "willkommen1", "administrator", "superadmin",
  "letmein123", "iloveyou1", "sonnenschein", "fussball1", "geheim123",
  "passw0rt", "p@ssword", "p@ssw0rt", "abcd1234", "test1234", "start1234",
]);

/** Zeichenklassen, die ein Passwort enthalten kann. */
function classesOf(password) {
  return {
    lower: /[a-zäöüß]/.test(password),
    upper: /[A-ZÄÖÜ]/.test(password),
    digit: /[0-9]/.test(password),
    symbol: /[^A-Za-z0-9ÄÖÜäöüß]/.test(password),
  };
}

/**
 * Prüft ein Passwort. Gibt eine Liste konkreter Probleme zurück — leer
 * bedeutet: in Ordnung.
 *
 * @param {string} password
 * @param {{name?: string, email?: string}} context Name und E-Mail dürfen nicht im Passwort stehen.
 */
export function checkPassword(password, context = {}) {
  const value = String(password || "");
  const problems = [];

  if (value.length < PASSWORD_MIN_LENGTH) {
    problems.push(`Mindestens ${PASSWORD_MIN_LENGTH} Zeichen.`);
  }
  if (value.length > 200) {
    problems.push("Höchstens 200 Zeichen.");
  }

  const classes = classesOf(value);
  const count = Object.values(classes).filter(Boolean).length;
  if (count < 3) {
    problems.push("Mindestens drei von vier Arten: Kleinbuchstaben, Großbuchstaben, Ziffern, Sonderzeichen.");
  }

  const lower = value.toLowerCase();
  if (COMMON.has(lower)) {
    problems.push("Dieses Passwort ist zu bekannt — nimm ein anderes.");
  }
  // Nur eine einzige Zeichenart, egal wie lang ("aaaaaaaaaaaa", "111111111111")
  if (/^(.)\1+$/.test(value)) {
    problems.push("Ein einzelnes wiederholtes Zeichen ist kein Passwort.");
  }
  // Aufsteigende Tastaturreihen und Ziffernfolgen
  if (/(0123456789|123456789|abcdefgh|qwertz|qwerty|asdfgh)/i.test(lower)) {
    problems.push("Vermeide Tastaturmuster und fortlaufende Ziffern.");
  }

  const name = String(context.name || "").trim().toLowerCase();
  if (name.length >= 3 && lower.includes(name)) {
    problems.push("Dein Name darf nicht im Passwort stehen.");
  }
  const local = String(context.email || "").split("@")[0].trim().toLowerCase();
  if (local.length >= 3 && lower.includes(local)) {
    problems.push("Deine E-Mail-Adresse darf nicht im Passwort stehen.");
  }

  return { ok: problems.length === 0, problems };
}

/**
 * Wirft einen Fehler mit Statuscode 400, wenn das Passwort zu schwach ist.
 * Für den Einsatz direkt in den Routen.
 */
export function assertPassword(password, context = {}) {
  const { ok, problems } = checkPassword(password, context);
  if (!ok) {
    throw Object.assign(new Error(problems.join(" ")), { statusCode: 400 });
  }
}
