import pg from "pg";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { config } from "./config.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

export const pool = new pg.Pool({
  connectionString: config.db.connectionString,
  max: config.db.max,
  idleTimeoutMillis: 30000,
});

export function query(text, params) {
  return pool.query(text, params);
}

/** Liefert genau eine Zeile oder null. */
export async function one(text, params) {
  const { rows } = await pool.query(text, params);
  return rows[0] || null;
}

export async function many(text, params) {
  const { rows } = await pool.query(text, params);
  return rows;
}

/** Führt mehrere Anweisungen in einer Transaktion aus. */
export async function transaction(fn) {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const result = await fn(client);
    await client.query("COMMIT");
    return result;
  } catch (e) {
    await client.query("ROLLBACK");
    throw e;
  } finally {
    client.release();
  }
}

/** Legt fehlende Tabellen an (idempotent). */
export async function migrate(log) {
  const sql = await readFile(join(__dirname, "schema.sql"), "utf8");
  await pool.query(sql);
  log?.info("Datenbankschema ist aktuell");
}

/** Entfernt abgelaufene Sitzungen — wird periodisch aufgerufen. */
export async function cleanupSessions() {
  const { rowCount } = await pool.query("DELETE FROM sessions WHERE expires_at < now()");
  return rowCount;
}
