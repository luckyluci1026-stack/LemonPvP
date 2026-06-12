import { NextRequest, NextResponse } from 'next/server'
import bcrypt from 'bcryptjs'
import db, { addAuditLog, type User } from '@/lib/db'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'

export async function GET(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.users')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const users = db.prepare(`
    SELECT id, email, name, role, suspended, permissions, created_at FROM users ORDER BY created_at DESC
  `).all() as Omit<User, 'password'>[]

  return NextResponse.json({ users })
}

export async function POST(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.users')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { email, password, name, role, permissions } = await req.json() as {
    email: string; password: string; name: string; role: string; permissions: string[]
  }

  if (!email || !password || !name) {
    return NextResponse.json({ error: 'Email, Passwort und Name erforderlich.' }, { status: 400 })
  }

  const validRoles = ['SUPER_ADMIN', 'ADMIN', 'STAFF']
  const assignedRole = role && validRoles.includes(role) ? role : 'STAFF'
  // Super admin can only be set by existing super admin
  if (assignedRole === 'SUPER_ADMIN' && session.role !== 'SUPER_ADMIN') {
    return NextResponse.json({ error: 'Nur Super-Admins können Super-Admins erstellen.' }, { status: 403 })
  }

  const hash = await bcrypt.hash(password, 12)
  const stmt = db.prepare(`
    INSERT INTO users (email, password, name, role, permissions, must_change_password)
    VALUES (?, ?, ?, ?, ?, 1)
  `)

  try {
    const result = stmt.run(
      email.toLowerCase().trim(),
      hash,
      name.trim(),
      assignedRole,
      JSON.stringify(permissions || [])
    )
    addAuditLog({
      userId: session.id,
      userEmail: session.email,
      action: 'CREATE_USER',
      target: email,
    })
    return NextResponse.json({ ok: true, id: result.lastInsertRowid })
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    if (msg.includes('UNIQUE')) return NextResponse.json({ error: 'Email bereits vergeben.' }, { status: 409 })
    throw e
  }
}
