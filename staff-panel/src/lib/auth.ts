// Full auth utilities (Node.js runtime only — do NOT import in middleware)
import { cookies } from 'next/headers'
import { NextRequest } from 'next/server'
import db, { type User } from './db'
import { createToken, verifyToken, type SessionUser } from './auth-edge'

export type { SessionUser }
export { createToken, verifyToken }

const COOKIE_NAME = 'lemon_session'

// ─── Cookie helpers ────────────────────────────────────────────────────────

export async function setSessionCookie(user: SessionUser): Promise<void> {
  const token = await createToken(user)
  const store = await cookies()
  store.set(COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 60 * 60 * 12,
    path: '/',
  })
}

export async function clearSessionCookie(): Promise<void> {
  const store = await cookies()
  store.delete(COOKIE_NAME)
}

export async function getSession(): Promise<SessionUser | null> {
  const store = await cookies()
  const token = store.get(COOKIE_NAME)?.value
  if (!token) return null
  return verifyToken(token)
}

// ─── Request-level auth (for API routes) ──────────────────────────────────

export async function getSessionFromRequest(req: NextRequest): Promise<SessionUser | null> {
  const token = req.cookies.get(COOKIE_NAME)?.value
  if (!token) return null
  return verifyToken(token)
}

// ─── Permission check ──────────────────────────────────────────────────────

export function hasPermission(user: SessionUser, permission: string): boolean {
  if (user.role === 'SUPER_ADMIN') return true
  return user.permissions.includes('*') || user.permissions.includes(permission)
}

// ─── Load full user from DB ────────────────────────────────────────────────

export function getUserById(id: number): User | undefined {
  return db.prepare('SELECT * FROM users WHERE id = ?').get(id) as User | undefined
}

export function toSessionUser(u: User): SessionUser {
  return {
    id: u.id,
    email: u.email,
    name: u.name,
    role: u.role,
    permissions: JSON.parse(u.permissions) as string[],
    mustChangePassword: u.must_change_password === 1,
  }
}
