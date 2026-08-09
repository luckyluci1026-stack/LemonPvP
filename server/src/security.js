/**
 * Sicherheits-Bausteine ohne externe Abhängigkeiten:
 * Passwort-Hashing (scrypt), Sitzungs-Token und TOTP (RFC 6238).
 * Alles basiert auf dem eingebauten crypto-Modul von Node.
 */
import crypto from "node:crypto";
import { config } from "./config.js";

/* --------------------------- Passwort-Hashing --------------------------- */
// scrypt ist speicherhart und damit deutlich widerstandsfähiger gegen
// GPU-Angriffe als einfache Hashfunktionen. Parameter nach OWASP-Empfehlung.
// maxmem muss ausdrücklich angehoben werden: N=32768 braucht 128*N*r ≈ 33 MB,
// Nodes Standardgrenze liegt bei 32 MB.
const SCRYPT = { N: 2 ** 15, r: 8, p: 1, keylen: 64 };
const MAXMEM = 64 * 1024 * 1024;

export function hashPassword(password) {
  return new Promise((resolve, reject) => {
    const salt = crypto.randomBytes(16);
    crypto.scrypt(password, salt, SCRYPT.keylen, { ...SCRYPT, maxmem: MAXMEM }, (err, derived) => {
      if (err) return reject(err);
      resolve(`scrypt$${SCRYPT.N}$${SCRYPT.r}$${SCRYPT.p}$${salt.toString("base64")}$${derived.toString("base64")}`);
    });
  });
}

export function verifyPassword(password, stored) {
  return new Promise((resolve) => {
    if (typeof stored !== "string" || !stored.startsWith("scrypt$")) return resolve(false);
    const [, N, r, p, saltB64, hashB64] = stored.split("$");
    const salt = Buffer.from(saltB64, "base64");
    const expected = Buffer.from(hashB64, "base64");
    // Die Parameter stammen aus dem gespeicherten Hash, damit auch ältere
    // Einträge nach einer Parameteränderung weiterhin prüfbar bleiben.
    const opts = { N: Number(N), r: Number(r), p: Number(p), maxmem: MAXMEM };
    crypto.scrypt(password, salt, expected.length, opts, (err, derived) => {
      if (err) return resolve(false);
      // Zeitkonstanter Vergleich, damit die Laufzeit nichts verrät
      resolve(derived.length === expected.length && crypto.timingSafeEqual(derived, expected));
    });
  });
}

/* ---------------------------- Sitzungs-Token ---------------------------- */
// Der Klartext-Token geht nur an den Browser; gespeichert wird ausschließlich
// der Hash. Wer die Datenbank liest, kann damit keine Sitzung übernehmen.
export function createSessionToken() {
  const raw = crypto.randomBytes(32).toString("base64url");
  return { raw, hash: hashToken(raw) };
}

export function hashToken(raw) {
  return crypto.createHmac("sha256", config.session.secret).update(raw).digest("hex");
}

/* ------------------------------- Codes ---------------------------------- */
/** Sechsstelliger Zifferncode, gleichverteilt (kein Modulo-Bias). */
export function numericCode(digits = 6) {
  const max = 10 ** digits;
  let value;
  do {
    value = crypto.randomBytes(4).readUInt32BE(0);
  } while (value >= Math.floor(0xffffffff / max) * max);
  return String(value % max).padStart(digits, "0");
}

export function schoolCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // ohne verwechselbare Zeichen
  let out = "";
  for (let i = 0; i < 4; i++) out += chars[crypto.randomInt(chars.length)];
  return `LRND-${out}`;
}

export function timingSafeEqualStr(a, b) {
  const bufA = Buffer.from(String(a));
  const bufB = Buffer.from(String(b));
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

/* --------------------------- TOTP (RFC 6238) ---------------------------- */
const B32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

export function base32Encode(buffer) {
  let bits = 0, value = 0, out = "";
  for (const byte of buffer) {
    value = (value << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      out += B32_ALPHABET[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }
  if (bits > 0) out += B32_ALPHABET[(value << (5 - bits)) & 31];
  return out;
}

export function base32Decode(str) {
  const clean = String(str).toUpperCase().replace(/=+$/, "").replace(/\s/g, "");
  let bits = 0, value = 0;
  const out = [];
  for (const ch of clean) {
    const idx = B32_ALPHABET.indexOf(ch);
    if (idx === -1) continue;
    value = (value << 5) | idx;
    bits += 5;
    if (bits >= 8) {
      out.push((value >>> (bits - 8)) & 255);
      bits -= 8;
    }
  }
  return Buffer.from(out);
}

/** Erzeugt ein neues TOTP-Geheimnis (kompatibel mit gängigen Authenticator-Apps). */
export function generateTotpSecret() {
  return base32Encode(crypto.randomBytes(20));
}

export function totpCode(secret, timeStep = Math.floor(Date.now() / 1000 / 30)) {
  const key = base32Decode(secret);
  const counter = Buffer.alloc(8);
  counter.writeBigUInt64BE(BigInt(timeStep));
  const hmac = crypto.createHmac("sha1", key).update(counter).digest();
  const offset = hmac[hmac.length - 1] & 0x0f;
  const binary =
    ((hmac[offset] & 0x7f) << 24) |
    ((hmac[offset + 1] & 0xff) << 16) |
    ((hmac[offset + 2] & 0xff) << 8) |
    (hmac[offset + 3] & 0xff);
  return String(binary % 1_000_000).padStart(6, "0");
}

/**
 * Prüft einen Code und erlaubt ±1 Zeitfenster (30 s), damit leicht
 * abweichende Uhren auf dem Handy nicht zu Fehlschlägen führen.
 */
export function verifyTotp(secret, code, window = 1) {
  const clean = String(code || "").replace(/\s/g, "");
  if (!/^\d{6}$/.test(clean)) return false;
  const now = Math.floor(Date.now() / 1000 / 30);
  for (let drift = -window; drift <= window; drift++) {
    if (timingSafeEqualStr(totpCode(secret, now + drift), clean)) return true;
  }
  return false;
}

/** URI für QR-Codes in Authenticator-Apps. */
export function totpUri(secret, account, issuer = "LearnDeveloping") {
  const label = encodeURIComponent(`${issuer}:${account}`);
  const params = new URLSearchParams({ secret, issuer, algorithm: "SHA1", digits: "6", period: "30" });
  return `otpauth://totp/${label}?${params}`;
}
