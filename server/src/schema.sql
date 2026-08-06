-- LearnDeveloping — Datenbankschema (PostgreSQL)
-- Wird beim Start automatisch angewendet; alle Anweisungen sind idempotent.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  role                TEXT NOT NULL DEFAULT 'student'
                        CHECK (role IN ('student', 'teacher', 'admin')),
  name                TEXT NOT NULL,
  email               TEXT NOT NULL,
  email_lower         TEXT GENERATED ALWAYS AS (lower(email)) STORED,
  password_hash       TEXT NOT NULL,

  email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
  verification_code   TEXT,
  verification_expires TIMESTAMPTZ,

  -- Passwort-Zurücksetzen: gespeichert wird nur der Hash des Tokens
  reset_token_hash    TEXT,
  reset_expires       TIMESTAMPTZ,

  -- Ligen und Streak-Schutz
  league              TEXT NOT NULL DEFAULT 'bronze',
  weekly_xp           INTEGER NOT NULL DEFAULT 0 CHECK (weekly_xp >= 0),
  week_key            TEXT,
  streak_freezes      INTEGER NOT NULL DEFAULT 0 CHECK (streak_freezes >= 0),

  totp_secret         TEXT,
  totp_enabled        BOOLEAN NOT NULL DEFAULT FALSE,

  teacher_id          UUID REFERENCES users(id) ON DELETE SET NULL,
  school              TEXT,
  school_code         TEXT,

  xp                  INTEGER NOT NULL DEFAULT 0 CHECK (xp >= 0),
  streak              INTEGER NOT NULL DEFAULT 0 CHECK (streak >= 0),
  last_active         DATE,
  current_course      TEXT,

  avatar              TEXT NOT NULL DEFAULT '🧑‍💻',
  avatar_config       JSONB,

  storage_used        BIGINT NOT NULL DEFAULT 0 CHECK (storage_used >= 0),
  storage_quota       BIGINT NOT NULL DEFAULT 2684354560,  -- 2,5 GiB

  disabled            BOOLEAN NOT NULL DEFAULT FALSE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login          TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS users_email_key ON users (email_lower);
CREATE UNIQUE INDEX IF NOT EXISTS users_school_code_key
  ON users (upper(school_code)) WHERE school_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS users_teacher_idx ON users (teacher_id);

-- Abgeschlossene Lektionen
CREATE TABLE IF NOT EXISTS completed_lessons (
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  lesson_id    TEXT NOT NULL,
  course_id    TEXT,
  completed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, lesson_id)
);
CREATE INDEX IF NOT EXISTS completed_lessons_user_idx ON completed_lessons (user_id);

-- Verdiente Abzeichen
CREATE TABLE IF NOT EXISTS badges (
  user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  badge_id  TEXT NOT NULL,
  earned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, badge_id)
);

-- IDE-Projekte
CREATE TABLE IF NOT EXISTS projects (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name       TEXT NOT NULL,
  -- Ein Projekt besteht aus beliebig vielen Dateien (JSON: [{name, content}]).
  -- html/css/js bleiben für ältere Datenbestände erhalten und werden beim
  -- Speichern mitgeführt, damit nichts verloren geht.
  files      TEXT NOT NULL DEFAULT '[]',
  html       TEXT NOT NULL DEFAULT '',
  css        TEXT NOT NULL DEFAULT '',
  js         TEXT NOT NULL DEFAULT '',
  size_bytes BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS projects_user_idx ON projects (user_id, updated_at DESC);

-- Gemeldete Inhalte (z.B. fragwürdige KI-Bewertungen)
CREATE TABLE IF NOT EXISTS reports (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id  UUID REFERENCES users(id) ON DELETE SET NULL,
  type         TEXT NOT NULL DEFAULT 'ai_answer',
  lesson_title TEXT,
  question     TEXT,
  user_answer  TEXT,
  ai_feedback  TEXT,
  reason       TEXT,
  status       TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'resolved')),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS reports_status_idx ON reports (status, created_at DESC);

-- Anmelde-Sitzungen (Token wird nur als Hash gespeichert)
CREATE TABLE IF NOT EXISTS sessions (
  token_hash TEXT PRIMARY KEY,
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  user_agent TEXT,
  ip         TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS sessions_user_idx ON sessions (user_id);
CREATE INDEX IF NOT EXISTS sessions_expiry_idx ON sessions (expires_at);

-- Nutzungsstatistik des KI-Proxys (für Monitoring der Key-Rotation)
CREATE TABLE IF NOT EXISTS ai_usage (
  id          BIGSERIAL PRIMARY KEY,
  user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
  provider    TEXT NOT NULL,
  key_label   TEXT,
  kind        TEXT NOT NULL,
  ok          BOOLEAN NOT NULL,
  status_code INTEGER,
  duration_ms INTEGER,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ai_usage_time_idx ON ai_usage (created_at DESC);

-- ---------------------------------------------------------------------------
-- Nachträgliche Spalten für bereits bestehende Installationen.
-- CREATE TABLE IF NOT EXISTS oben greift bei vorhandenen Tabellen nicht.
-- ---------------------------------------------------------------------------
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_hash TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_expires TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS league TEXT NOT NULL DEFAULT 'bronze';
ALTER TABLE users ADD COLUMN IF NOT EXISTS weekly_xp INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS week_key TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS streak_freezes INTEGER NOT NULL DEFAULT 0;


-- Eigene Lektionen, die Lehrkräfte selbst anlegen
CREATE TABLE IF NOT EXISTS custom_lessons (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title       TEXT NOT NULL,
  course_id   TEXT NOT NULL DEFAULT 'html',
  level       TEXT NOT NULL DEFAULT 'beginner',
  xp_reward   INTEGER NOT NULL DEFAULT 50 CHECK (xp_reward >= 0),
  theory      TEXT NOT NULL DEFAULT '',
  tasks       TEXT NOT NULL DEFAULT '[]',
  published   BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS custom_lessons_teacher_idx ON custom_lessons (teacher_id, updated_at DESC);

-- Spalten, die erst nachträglich hinzugekommen sind (PostgreSQL).
-- Für SQLite erledigt das db-sqlite.js über LATER_COLUMNS.
ALTER TABLE projects ADD COLUMN IF NOT EXISTS files TEXT NOT NULL DEFAULT '[]';

-- XP-Shop: ausgegebene XP, Tipp-Joker, Freischaltungen, Doppel-XP
ALTER TABLE users ADD COLUMN IF NOT EXISTS spent_xp INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS hints INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS unlocks TEXT NOT NULL DEFAULT '[]';
ALTER TABLE users ADD COLUMN IF NOT EXISTS boost_until BIGINT NOT NULL DEFAULT 0;
