import { NextRequest, NextResponse } from 'next/server'
import bcrypt from 'bcryptjs'
import db, { addAuditLog, type User } from '@/lib/db'
import { setSessionCookie, toSessionUser } from '@/lib/auth'

export async function POST(req: NextRequest) {
  const { email, password } = await req.json() as { email: string; password: string }

  if (!email || !password) {
    return NextResponse.json({ error: 'Email und Passwort erforderlich.' }, { status: 400 })
  }

  const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email.toLowerCase().trim()) as User | undefined
  if (!user) {
    return NextResponse.json({ error: 'Ungültige Zugangsdaten.' }, { status: 401 })
  }

  if (user.suspended) {
    return NextResponse.json({ error: 'Dein Account wurde gesperrt. Kontaktiere einen Admin.' }, { status: 403 })
  }

  const valid = await bcrypt.compare(password, user.password)
  if (!valid) {
    addAuditLog({ userEmail: email, action: 'LOGIN_FAIL', ip: req.headers.get('x-forwarded-for') ?? undefined })
    return NextResponse.json({ error: 'Ungültige Zugangsdaten.' }, { status: 401 })
  }

  const session = toSessionUser(user)
  await setSessionCookie(session)

  addAuditLog({
    userId: user.id,
    userEmail: user.email,
    action: 'LOGIN',
    ip: req.headers.get('x-forwarded-for') ?? undefined,
  })

  return NextResponse.json({ ok: true, user: { id: session.id, email: session.email, name: session.name, role: session.role } })
}
