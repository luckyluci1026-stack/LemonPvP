import { NextRequest, NextResponse } from 'next/server'
import bcrypt from 'bcryptjs'
import db, { addAuditLog } from '@/lib/db'
import { getSessionFromRequest, getUserById, toSessionUser, setSessionCookie } from '@/lib/auth'
import type { User } from '@/lib/db'

export async function POST(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const { password } = await req.json() as { password: string }

  if (!password || password.length < 8) {
    return NextResponse.json({ error: 'Passwort muss mindestens 8 Zeichen haben.' }, { status: 400 })
  }
  if (password.length > 200) {
    return NextResponse.json({ error: 'Passwort ist zu lang.' }, { status: 400 })
  }

  const current = getUserById(session.id) as User | undefined
  if (!current) return NextResponse.json({ error: 'Benutzer nicht gefunden.' }, { status: 404 })

  // Reject reusing the same password — otherwise a forced change is pointless.
  if (await bcrypt.compare(password, current.password)) {
    return NextResponse.json({ error: 'Bitte wähle ein neues Passwort, nicht das alte.' }, { status: 400 })
  }

  const hash = await bcrypt.hash(password, 12)
  db.prepare(`UPDATE users SET password = ?, must_change_password = 0, updated_at = datetime('now') WHERE id = ?`)
    .run(hash, session.id)

  addAuditLog({ userId: session.id, userEmail: session.email, action: 'CHANGE_PASSWORD' })

  const updated = getUserById(session.id) as User | undefined
  if (updated) await setSessionCookie(toSessionUser(updated))

  return NextResponse.json({ ok: true })
}
