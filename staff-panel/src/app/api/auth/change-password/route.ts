import { NextRequest, NextResponse } from 'next/server'
import bcrypt from 'bcryptjs'
import db from '@/lib/db'
import { getSessionFromRequest, getUserById, toSessionUser, setSessionCookie } from '@/lib/auth'
import type { User } from '@/lib/db'

export async function POST(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const { password } = await req.json() as { password: string }

  if (!password || password.length < 8) {
    return NextResponse.json({ error: 'Passwort muss mindestens 8 Zeichen haben.' }, { status: 400 })
  }

  const hash = await bcrypt.hash(password, 12)
  db.prepare(`UPDATE users SET password = ?, must_change_password = 0, updated_at = datetime('now') WHERE id = ?`)
    .run(hash, session.id)

  const dbUser = getUserById(session.id) as User | undefined
  if (dbUser) await setSessionCookie(toSessionUser(dbUser))

  return NextResponse.json({ ok: true })
}
