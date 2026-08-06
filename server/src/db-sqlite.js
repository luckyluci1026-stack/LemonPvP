/**
 * SQLite-Variante für den lokalen Betrieb auf schwacher Hardware.
 *
 * Nutzt das in Node eingebaute node:sqlite — es muss also nichts installiert
 * und kein Datenbankdienst gestartet werden. Für die Entwicklung und zum
 * Ausprobieren unterwegs ideal; für den Produktivbetrieb bleibt PostgreSQL
 * die richtige Wahl (echte Nebenläufigkeit, Backups, Replikation).
 *
 * Die SQL-Anweisungen der Anwendung sind für PostgreSQL geschrieben. Diese
 * Schicht übersetzt sie so weit, dass dieselben Abfragen auch hier laufen.
 */
import { DatabaseSync } from "node:sqlite";
import { mkdirSync } from "node:fs";
import { dirname } from "node:path";
import crypto from "node:crypto";

let db = null;

export function openSqlite(file) {
  if (db) return db;
  if (file !== ":memory:") mkdirSync(dirname(file), { recursive: true });
  db = new DatabaseSync(file);
  db.exec("PRAGMA journal_mode = WAL");     // gleichzeitiges Lesen während Schreibvorgängen
  db.exec("PRAGMA foreign_keys = ON");
  db.exec("PRAGMA busy_timeout = 5000");
  return db;
}

/* --------------------------- Schema (SQLite) ---------------------------- */
const SCHEMA = `
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  role TEXT NOT NULL DEFAULT 'student' CHECK (role IN ('student','teacher','admin')),
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  email_lower TEXT GENERATED ALWAYS AS (lower(email)) STORED,
  password_hash TEXT NOT NULL,
  email_verified INTEGER NOT NULL DEFAULT 0,
  verification_code TEXT,
  verification_expires TEXT,
  reset_token_hash TEXT,
  reset_expires TEXT,
  league TEXT NOT NULL DEFAULT 'bronze',
  weekly_xp INTEGER NOT NULL DEFAULT 0,
  week_key TEXT,
  streak_freezes INTEGER NOT NULL DEFAULT 0,
  totp_secret TEXT,
  totp_enabled INTEGER NOT NULL DEFAULT 0,
  teacher_id TEXT REFERENCES users(id) ON DELETE SET NULL,
  school TEXT,
  school_code TEXT,
  xp INTEGER NOT NULL DEFAULT 0,
  streak INTEGER NOT NULL DEFAULT 0,
  last_active TEXT,
  current_course TEXT,
  avatar TEXT NOT NULL DEFAULT '🧑‍💻',
  avatar_config TEXT,
  storage_used INTEGER NOT NULL DEFAULT 0,
  storage_quota INTEGER NOT NULL DEFAULT 2684354560,
  disabled INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  last_login TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS users_email_key ON users (email_lower);
CREATE UNIQUE INDEX IF NOT EXISTS users_school_code_key ON users (upper(school_code)) WHERE school_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS completed_lessons (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  lesson_id TEXT NOT NULL,
  course_id TEXT,
  completed_at TEXT NOT NULL DEFAULT (datetime('now')),
  PRIMARY KEY (user_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS badges (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  badge_id TEXT NOT NULL,
  earned_at TEXT NOT NULL DEFAULT (datetime('now')),
  PRIMARY KEY (user_id, badge_id)
);

CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  files TEXT NOT NULL DEFAULT '[]',
  html TEXT NOT NULL DEFAULT '',
  css TEXT NOT NULL DEFAULT '',
  js TEXT NOT NULL DEFAULT '',
  size_bytes INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS reports (
  id TEXT PRIMARY KEY,
  reporter_id TEXT REFERENCES users(id) ON DELETE SET NULL,
  type TEXT NOT NULL DEFAULT 'ai_answer',
  lesson_title TEXT, question TEXT, user_answer TEXT, ai_feedback TEXT, reason TEXT,
  status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open','resolved')),
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  resolved_at TEXT
);

CREATE TABLE IF NOT EXISTS sessions (
  token_hash TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  user_agent TEXT, ip TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  expires_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS custom_lessons (
  id TEXT PRIMARY KEY,
  teacher_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  course_id TEXT NOT NULL DEFAULT 'html',
  level TEXT NOT NULL DEFAULT 'beginner',
  xp_reward INTEGER NOT NULL DEFAULT 50,
  theory TEXT NOT NULL DEFAULT '',
  tasks TEXT NOT NULL DEFAULT '[]',
  published INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS ai_usage (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT REFERENCES users(id) ON DELETE SET NULL,
  provider TEXT NOT NULL, key_label TEXT, kind TEXT NOT NULL,
  ok INTEGER NOT NULL, status_code INTEGER, duration_ms INTEGER,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
`;

/* --------------------- Übersetzung PostgreSQL -> SQLite ------------------ */
/**
 * Übersetzt Platzhalter und faltet dabei die Werte auf.
 *
 * PostgreSQL erlaubt, denselben Parameter mehrfach zu referenzieren
 * ($1 ... $1 ... $2). SQLite zählt dagegen jedes "?" einzeln — die Werteliste
 * muss deshalb in der Reihenfolge des Auftretens neu aufgebaut werden.
 */
function translatePlaceholders(sql, params = []) {
  const values = [];
  const out = sql.replace(/\$(\d+)/g, (_m, n) => {
    values.push(params[Number(n) - 1]);
    return "?";
  });
  return { sql: out, values };
}

function translate(sql) {
  let out = sql;
  // Zeitfunktionen — Intervalle zuerst, sonst ist das now() darin schon ersetzt
  out = out.replace(/\bnow\(\)\s*-\s*interval\s*'(\d+)\s*(hour|day|minute|second)s?'/gi,
    (_m, n, unit) => `datetime('now', '-${n} ${unit}s')`);
  out = out.replace(/\bnow\(\)\s*\+\s*interval\s*'(\d+)\s*(hour|day|minute|second)s?'/gi,
    (_m, n, unit) => `datetime('now', '+${n} ${unit}s')`);
  out = out.replace(/\bnow\(\)/gi, "datetime('now')");
  out = out.replace(/\bCURRENT_DATE\b/gi, "date('now')");
  // Typumwandlungen und Postgres-Eigenheiten
  out = out.replace(/::text/gi, "");
  out = out.replace(/\bILIKE\b/gi, "LIKE");
  out = out.replace(/\bFOR UPDATE\b/gi, "");
  out = out.replace(/\bGREATEST\(/gi, "MAX(");
  return out;
}

/** Wandelt Parameter in von SQLite unterstützte Typen. */
function bindParams(params = []) {
  return params.map((p) => {
    if (p === undefined || p === null) return null;
    if (typeof p === "boolean") return p ? 1 : 0;
    if (p instanceof Date) return p.toISOString().replace("T", " ").slice(0, 19);
    if (typeof p === "object") return JSON.stringify(p);
    return p;
  });
}

// Spalten, die in PostgreSQL boolesch sind — in SQLite liegen sie als 0/1 vor.
const BOOL_COLUMNS = new Set(["email_verified", "totp_enabled", "disabled", "ok"]);

function normalizeRow(row) {
  if (!row) return row;
  const out = {};
  for (const [k, v] of Object.entries(row)) {
    if (BOOL_COLUMNS.has(k)) out[k] = !!v;
    else if (k === "avatar_config" && typeof v === "string") {
      try { out[k] = JSON.parse(v); } catch { out[k] = null; }
    } else out[k] = v;
  }
  return out;
}

/** Führt eine Anweisung aus und liefert ein pg-kompatibles Ergebnis. */
export function sqliteQuery(text, params) {
  const expanded = translatePlaceholders(text, params);
  const sql = translate(expanded.sql);
  const values = bindParams(expanded.values);

  // gen_random_uuid() gibt es in SQLite nicht — IDs werden hier erzeugt.
  const needsId = /INSERT\s+INTO\s+(users|projects|reports)\b/i.test(sql) && !/\bid\b\s*,/i.test(sql.split("VALUES")[0] || "");
  let finalSql = sql;
  let finalValues = values;
  if (needsId) {
    const table = sql.match(/INSERT\s+INTO\s+(\w+)/i)[1];
    finalSql = sql.replace(new RegExp(`INSERT\\s+INTO\\s+${table}\\s*\\(`, "i"), `INSERT INTO ${table} (id, `)
                  .replace(/VALUES\s*\(/i, "VALUES (?, ");
    finalValues = [crypto.randomUUID(), ...values];
  }

  const stmt = db.prepare(finalSql);
  if (/^\s*(SELECT|WITH)/i.test(finalSql) || /RETURNING/i.test(finalSql)) {
    const rows = stmt.all(...finalValues).map(normalizeRow);
    return { rows, rowCount: rows.length };
  }
  const info = stmt.run(...finalValues);
  return { rows: [], rowCount: Number(info.changes || 0) };
}

// Nachträglich hinzugekommene Spalten. SQLite kennt kein
// "ADD COLUMN IF NOT EXISTS", daher wird der Bestand vorher abgefragt.
const LATER_COLUMNS = [
  ["users", "reset_token_hash", "TEXT"],
  ["users", "reset_expires", "TEXT"],
  ["users", "league", "TEXT NOT NULL DEFAULT 'bronze'"],
  ["users", "weekly_xp", "INTEGER NOT NULL DEFAULT 0"],
  ["users", "week_key", "TEXT"],
  ["users", "streak_freezes", "INTEGER NOT NULL DEFAULT 0"],
  ["projects", "files", "TEXT NOT NULL DEFAULT '[]'"],
  ["users", "spent_xp", "INTEGER NOT NULL DEFAULT 0"],
  ["users", "hints", "INTEGER NOT NULL DEFAULT 0"],
  ["users", "unlocks", "TEXT NOT NULL DEFAULT '[]'"],
  ["users", "boost_until", "INTEGER NOT NULL DEFAULT 0"],
];

export function sqliteMigrate() {
  db.exec(SCHEMA);
  for (const [table, column, type] of LATER_COLUMNS) {
    const existing = db.prepare(`PRAGMA table_info(${table})`).all().map((c) => c.name);
    if (!existing.includes(column)) {
      db.exec(`ALTER TABLE ${table} ADD COLUMN ${column} ${type}`);
    }
  }
}

/** Transaktionen — SQLite kennt keine verschachtelten, daher einfach gehalten. */
export function sqliteTransaction(fn) {
  db.exec("BEGIN");
  try {
    const client = { query: (t, p) => Promise.resolve(sqliteQuery(t, p)) };
    return Promise.resolve(fn(client)).then(
      (result) => { db.exec("COMMIT"); return result; },
      (err) => { db.exec("ROLLBACK"); throw err; }
    );
  } catch (e) {
    db.exec("ROLLBACK");
    throw e;
  }
}

export function sqliteClose() {
  if (db) { db.close(); db = null; }
}
