import { NextRequest, NextResponse } from 'next/server'
import bcrypt from 'bcryptjs'
import db, { addAuditLog, type User } from '@/lib/db'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'

type Params = { params: Promise<{ id: string }> }

export async function PUT(req: NextRequest, { params }: Params) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.users')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { id } = await params
  const userId = parseInt(id, 10)
  if (Number.isNaN(userId)) return NextResponse.json({ error: 'Ungültige ID.' }, { status: 400 })

  const target = db.prepare('SELECT * FROM users WHERE id = ?').get(userId) as User | undefined
  if (!target) return NextResponse.json({ error: 'Benutzer nicht gefunden.' }, { status: 404 })

  // Only super admin can edit super admin
  if (target.role === 'SUPER_ADMIN' && session.role !== 'SUPER_ADMIN') {
    return NextResponse.json({ error: 'Nur Super-Admins können Super-Admin Accounts bearbeiten.' }, { status: 403 })
  }

  const body = await req.json() as {
    name?: string
    password?: string
    role?: string
    suspended?: boolean
    permissions?: string[]
    mustChangePassword?: boolean
  }

  // Prevent self-escalation: users cannot change their own role/permissions/suspension
  if (userId === session.id && (body.role !== undefined || body.permissions !== undefined || body.suspended !== undefined)) {
    return NextResponse.json({ error: 'Du kannst deine eigene Rolle, Rechte oder Sperre nicht ändern.' }, { status: 403 })
  }

  if (body.name !== undefined) {
    db.prepare('UPDATE users SET name = ?, updated_at = datetime(\'now\') WHERE id = ?').run(body.name, userId)
  }
  if (body.password !== undefined && body.password.length > 0) {
    if (body.password.length < 8) return NextResponse.json({ error: 'Passwort muss mindestens 8 Zeichen haben.' }, { status: 400 })
    if (body.password.length > 200) return NextResponse.json({ error: 'Passwort ist zu lang.' }, { status: 400 })
    const hash = await bcrypt.hash(body.password, 12)
    db.prepare('UPDATE users SET password = ?, updated_at = datetime(\'now\') WHERE id = ?').run(hash, userId)
    if (body.mustChangePassword !== undefined) {
      db.prepare('UPDATE users SET must_change_password = ?, updated_at = datetime(\'now\') WHERE id = ?').run(body.mustChangePassword ? 1 : 0, userId)
    }
  }
  if (body.role !== undefined) {
    const validRoles = ['SUPER_ADMIN', 'ADMIN', 'STAFF']
    if (!validRoles.includes(body.role)) {
      return NextResponse.json({ error: 'Ungültige Rolle.' }, { status: 400 })
    }
    if (body.role === 'SUPER_ADMIN' && session.role !== 'SUPER_ADMIN') {
      return NextResponse.json({ error: 'Keine Berechtigung für Super-Admin.' }, { status: 403 })
    }
    db.prepare('UPDATE users SET role = ?, updated_at = datetime(\'now\') WHERE id = ?').run(body.role, userId)
  }
  if (body.suspended !== undefined) {
    db.prepare('UPDATE users SET suspended = ?, updated_at = datetime(\'now\') WHERE id = ?').run(body.suspended ? 1 : 0, userId)
    addAuditLog({
      userId: session.id,
      userEmail: session.email,
      action: body.suspended ? 'SUSPEND_USER' : 'UNSUSPEND_USER',
      target: target.email,
    })
  }
  if (body.permissions !== undefined) {
    // Only SUPER_ADMIN can grant wildcard; others cannot grant permissions they don't hold
    if (session.role !== 'SUPER_ADMIN') {
      const actorPerms = new Set(session.permissions)
      if (body.permissions.includes('*') || body.permissions.some(p => !actorPerms.has(p))) {
        return NextResponse.json({ error: 'Du kannst keine Rechte vergeben, die du selbst nicht besitzt.' }, { status: 403 })
      }
    }
    db.prepare('UPDATE users SET permissions = ?, updated_at = datetime(\'now\') WHERE id = ?').run(JSON.stringify(body.permissions), userId)
  }

  addAuditLog({ userId: session.id, userEmail: session.email, action: 'UPDATE_USER', target: target.email })
  return NextResponse.json({ ok: true })
}

export async function DELETE(req: NextRequest, { params }: Params) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.users')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { id } = await params
  const userId = parseInt(id, 10)
  if (Number.isNaN(userId)) return NextResponse.json({ error: 'Ungültige ID.' }, { status: 400 })
  if (userId === session.id) return NextResponse.json({ error: 'Du kannst deinen eigenen Account nicht löschen.' }, { status: 400 })

  const target = db.prepare('SELECT * FROM users WHERE id = ?').get(userId) as User | undefined
  if (!target) return NextResponse.json({ error: 'Benutzer nicht gefunden.' }, { status: 404 })
  if (target.role === 'SUPER_ADMIN' && session.role !== 'SUPER_ADMIN') {
    return NextResponse.json({ error: 'Keine Berechtigung.' }, { status: 403 })
  }

  db.prepare('DELETE FROM users WHERE id = ?').run(userId)
  addAuditLog({ userId: session.id, userEmail: session.email, action: 'DELETE_USER', target: target.email })
  return NextResponse.json({ ok: true })
}
