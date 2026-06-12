import Database from 'better-sqlite3'
import bcrypt from 'bcryptjs'
import path from 'path'
import fs from 'fs'

const DB_PATH = path.join(process.cwd(), 'data', 'panel.db')

// Ensure data directory exists
const dataDir = path.dirname(DB_PATH)
if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir, { recursive: true })

// Use globalThis to keep a single instance across hot reloads and build phases
declare global {
  // eslint-disable-next-line no-var
  var __db: InstanceType<typeof Database> | undefined
}

const db: InstanceType<typeof Database> = global.__db ?? new Database(DB_PATH)
global.__db = db
db.pragma('journal_mode = WAL')
db.pragma('foreign_keys = ON')

// ─── Schema ────────────────────────────────────────────────────────────────

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    email       TEXT    UNIQUE NOT NULL,
    password    TEXT    NOT NULL,
    name        TEXT    NOT NULL,
    role        TEXT    NOT NULL DEFAULT 'STAFF',
    suspended   INTEGER NOT NULL DEFAULT 0,
    permissions TEXT    NOT NULL DEFAULT '[]',
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS groups (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    UNIQUE NOT NULL,
    description TEXT,
    permissions TEXT    NOT NULL DEFAULT '[]',
    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS group_members (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id  INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    UNIQUE(user_id, group_id)
  );

  CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
  );

  CREATE TABLE IF NOT EXISTS audit_logs (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    user_email TEXT,
    action     TEXT    NOT NULL,
    target     TEXT,
    details    TEXT,
    ip         TEXT,
    created_at TEXT    NOT NULL DEFAULT (datetime('now'))
  );
`)

// ─── Migrations ────────────────────────────────────────────────────────────

try { db.exec('ALTER TABLE users ADD COLUMN must_change_password INTEGER NOT NULL DEFAULT 0') } catch { /* already exists */ }

// ─── Seed admin account ────────────────────────────────────────────────────

// Seed admin account idempotently
const existingAdmin = db.prepare('SELECT id FROM users WHERE email = ?').get('lemonightt@lemonpvp.de')
if (!existingAdmin) {
  const hash = bcrypt.hashSync('LemonLemon', 12)
  db.prepare(`
    INSERT OR IGNORE INTO users (email, password, name, role, permissions)
    VALUES (?, ?, ?, 'SUPER_ADMIN', '["*"]')
  `).run('lemonightt@lemonpvp.de', hash, 'LemonPvP Admin')
}

// ─── Default settings ──────────────────────────────────────────────────────

const defaultSettings: Record<string, string> = {
  mailcow_url:   '',
  mailcow_key:   '',
  mc_api_url:    'http://localhost:8080',
  mc_api_key:    '',
  domain:        'lemonpvp.de',
  panel_name:    'LemonPvP Staff',
}

const insertSetting = db.prepare('INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)')
for (const [k, v] of Object.entries(defaultSettings)) {
  insertSetting.run(k, v)
}

// ─── Helpers ───────────────────────────────────────────────────────────────

export type User = {
  id: number
  email: string
  password: string
  name: string
  role: 'SUPER_ADMIN' | 'ADMIN' | 'STAFF'
  suspended: number
  must_change_password: number
  permissions: string
  created_at: string
  updated_at: string
}

export type Group = {
  id: number
  name: string
  description: string | null
  permissions: string
  created_at: string
}

export type AuditLog = {
  id: number
  user_id: number | null
  user_email: string | null
  action: string
  target: string | null
  details: string | null
  ip: string | null
  created_at: string
}

export function getSetting(key: string): string {
  const row = db.prepare('SELECT value FROM settings WHERE key = ?').get(key) as { value: string } | undefined
  return row?.value ?? ''
}

export function setSetting(key: string, value: string): void {
  db.prepare('INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)').run(key, value)
}

export function getAllSettings(): Record<string, string> {
  const rows = db.prepare('SELECT key, value FROM settings').all() as { key: string; value: string }[]
  return Object.fromEntries(rows.map(r => [r.key, r.value]))
}

export function addAuditLog(params: {
  userId?: number | null
  userEmail?: string | null
  action: string
  target?: string
  details?: string
  ip?: string
}): void {
  db.prepare(`
    INSERT INTO audit_logs (user_id, user_email, action, target, details, ip)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(
    params.userId ?? null,
    params.userEmail ?? null,
    params.action,
    params.target ?? null,
    params.details ?? null,
    params.ip ?? null,
  )
}

export default db
