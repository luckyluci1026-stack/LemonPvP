import { one, many } from "./db.js";

/**
 * Wandelt eine Datenbankzeile in das Format um, das das Frontend erwartet.
 * Geheimnisse (Passwort-Hash, TOTP-Secret, Verifizierungscode) werden dabei
 * konsequent entfernt — diese Funktion ist die einzige Stelle, an der
 * Nutzerdaten den Server verlassen.
 */
export function publicUser(row, extra = {}) {
  if (!row) return null;
  return {
    id: row.id,
    role: row.role,
    name: row.name,
    email: row.email,
    emailVerified: row.email_verified,
    twoFactorEnabled: row.totp_enabled,
    teacherId: row.teacher_id || null,
    school: row.school || null,
    schoolCode: row.school_code || null,
    xp: row.xp,
    streak: row.streak,
    lastActive: row.last_active ? String(row.last_active).slice(0, 10) : null,
    streakFreezes: row.streak_freezes || 0,
    league: row.league || "bronze",
    weeklyXp: row.weekly_xp || 0,
    weekKey: row.week_key || null,
    currentCourse: row.current_course || null,
    avatar: row.avatar,
    avatarConfig: row.avatar_config || null,
    storageUsed: Number(row.storage_used || 0),
    storageQuota: Number(row.storage_quota || 0),
    disabled: row.disabled,
    joinedAt: row.created_at,
    lastLogin: row.last_login,
    completedLessons: extra.completedLessons || [],
    badges: extra.badges || [],
  };
}

/** Lädt einen Nutzer samt Lektionen und Abzeichen. */
export async function loadFullUser(userId) {
  const row = await one("SELECT * FROM users WHERE id = $1", [userId]);
  if (!row) return null;
  const [lessons, badges] = await Promise.all([
    many("SELECT lesson_id FROM completed_lessons WHERE user_id = $1", [userId]),
    many("SELECT badge_id FROM badges WHERE user_id = $1", [userId]),
  ]);
  return publicUser(row, {
    completedLessons: lessons.map((l) => l.lesson_id),
    badges: badges.map((b) => b.badge_id),
  });
}

/** Kompakte Darstellung für Listen (Rangliste, Lehrer-Übersicht). */
export function listUser(row) {
  return {
    id: row.id,
    role: row.role,
    name: row.name,
    email: row.email,
    xp: row.xp,
    streak: row.streak,
    league: row.league || "bronze",
    weeklyXp: row.weekly_xp || 0,
    weekKey: row.week_key || null,
    avatar: row.avatar,
    avatarConfig: row.avatar_config || null,
    currentCourse: row.current_course || null,
    lastLogin: row.last_login,
    emailVerified: row.email_verified,
    twoFactorEnabled: row.totp_enabled,
    disabled: row.disabled,
    completedCount: Number(row.completed_count || 0),
    completedLessons: row.completed_lessons || [],
  };
}

export function serializeProject(row) {
  return {
    id: row.id,
    name: row.name,
    html: row.html,
    css: row.css,
    js: row.js,
    sizeBytes: Number(row.size_bytes || 0),
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

export function serializeReport(row) {
  return {
    id: row.id,
    type: row.type,
    reporterId: row.reporter_id,
    reporterName: row.reporter_name || "Gelöschtes Konto",
    lessonTitle: row.lesson_title,
    question: row.question,
    userAnswer: row.user_answer,
    aiFeedback: row.ai_feedback,
    reason: row.reason,
    status: row.status,
    createdAt: row.created_at,
  };
}
