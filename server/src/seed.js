/**
 * Legt den ersten Administrator an.
 *
 *   node src/seed.js --email admin@example.com --password "..." --name "Admin"
 *
 * Ohne Argumente wird ein zufälliges Passwort erzeugt und einmalig ausgegeben.
 */
import crypto from "node:crypto";
import { config } from "./config.js";
import { pool, one, migrate } from "./db.js";
import { hashPassword } from "./security.js";

function arg(name, fallback) {
  const i = process.argv.indexOf(`--${name}`);
  return i > -1 && process.argv[i + 1] ? process.argv[i + 1] : fallback;
}

const log = { info: (m) => console.log(m), warn: (m) => console.warn(m) };

const email = arg("email", "admin@learndeveloping.com");
const name = arg("name", "Administrator");
const generated = !arg("password");
const password = arg("password", crypto.randomBytes(12).toString("base64url"));

await migrate(log);

const existing = await one("SELECT id, role FROM users WHERE email_lower = lower($1)", [email]);
if (existing) {
  console.error(`\n✗ Es existiert bereits ein Konto mit ${email} (Rolle: ${existing.role}).`);
  console.error("  Nutze eine andere Adresse oder ändere die Rolle im Admin-Bereich.\n");
  await pool.end();
  process.exit(1);
}

const hash = await hashPassword(password);
const user = await one(
  `INSERT INTO users (role, name, email, password_hash, email_verified, avatar, storage_quota)
   VALUES ('admin', $1, $2, $3, TRUE, '🛡️', $4) RETURNING id, email`,
  [name, email, hash, config.storage.quotaBytes]
);

console.log(`\n✓ Administrator angelegt`);
console.log(`  E-Mail:   ${user.email}`);
if (generated) {
  console.log(`  Passwort: ${password}`);
  console.log(`\n  Dieses Passwort wird nur jetzt angezeigt — bitte sicher notieren.`);
}
console.log("");

await pool.end();
