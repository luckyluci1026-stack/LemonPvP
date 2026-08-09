import pg from "pg";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join, resolve } from "node:path";
import { config } from "./config.js";
import { openSqlite, sqliteQuery, sqliteMigrate, sqliteTransaction, sqliteClose } from "./db-sqlite.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

/**
 * Zwei Datenbank-Varianten:
 *
 *  - PostgreSQL (Standard) für den Produktivbetrieb
 *  - SQLite über node:sqlite, wenn DATABASE_URL mit "sqlite:" beginnt —
 *    braucht keine Installation und läuft auch auf schwacher Hardware.
 */
export const usingSqlite = config.db.connectionString.startsWith("sqlite:");

let pool = null;

if (usingSqlite) {
  const path = config.db.connectionString.slice("sqlite:".length) || "./data/learndeveloping.db";
  openSqlite(path === ":memory:" ? path : resolve(__dirname, "..", path));
} else {
  pool = new pg.Pool({
    connectionString: config.db.connectionString,
    max: config.db.max,
    idleTimeoutMillis: 30000,
  });
}

export { pool };

export function query(text, params) {
  if (usingSqlite) return Promise.resolve(sqliteQuery(text, params));
  return pool.query(text, params);
}

/** Liefert genau eine Zeile oder null. */
export async function one(text, params) {
  const { rows } = await query(text, params);
  return rows[0] || null;
}

export async function many(text, params) {
  const { rows } = await query(text, params);
  return rows;
}

/** Führt mehrere Anweisungen in einer Transaktion aus. */
export async function transaction(fn) {
  if (usingSqlite) return sqliteTransaction(fn);
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
  if (usingSqlite) {
    sqliteMigrate();
    log?.info("Datenbankschema ist aktuell (SQLite)");
    return;
  }
  const sql = await readFile(join(__dirname, "schema.sql"), "utf8");
  await pool.query(sql);
  log?.info("Datenbankschema ist aktuell (PostgreSQL)");
}

/** Entfernt abgelaufene Sitzungen — wird periodisch aufgerufen. */
export async function cleanupSessions() {
  const { rowCount } = await query("DELETE FROM sessions WHERE expires_at < now()");
  return rowCount;
}

export async function closeDb() {
  if (usingSqlite) sqliteClose();
  else if (pool) await pool.end();
}
