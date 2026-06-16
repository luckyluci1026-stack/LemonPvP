import { NextRequest, NextResponse } from 'next/server'
import db, { addAuditLog, type User } from '@/lib/db'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'

type Params = { params: Promise<{ id: string }> }

export async function PUT(req: NextRequest, { params }: Params) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.groups')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { id } = await params
  const groupId = parseInt(id, 10)
  if (isNaN(groupId)) return NextResponse.json({ error: 'Ungültige Gruppen-ID.' }, { status: 400 })

  const body = await req.json() as {
    name?: string; description?: string; permissions?: string[]
    addMember?: number; removeMember?: number
  }

  if (body.name !== undefined) {
    db.prepare('UPDATE groups SET name = ? WHERE id = ?').run(body.name, groupId)
  }
  if (body.description !== undefined) {
    db.prepare('UPDATE groups SET description = ? WHERE id = ?').run(body.description, groupId)
  }
  if (body.permissions !== undefined) {
    // Non-super-admins cannot grant permissions they don't hold
    if (session.role !== 'SUPER_ADMIN') {
      const actorPerms = new Set(session.permissions)
      if (body.permissions.includes('*') || body.permissions.some(p => !actorPerms.has(p))) {
        return NextResponse.json({ error: 'Du kannst keine Rechte vergeben, die du selbst nicht besitzt.' }, { status: 403 })
      }
    }
    db.prepare('UPDATE groups SET permissions = ? WHERE id = ?').run(JSON.stringify(body.permissions), groupId)
  }
  if (body.addMember !== undefined) {
    const memberId = Number(body.addMember)
    if (!Number.isInteger(memberId)) return NextResponse.json({ error: 'Ungültige User-ID.' }, { status: 400 })
    const exists = db.prepare('SELECT id FROM users WHERE id = ?').get(memberId) as User | undefined
    if (!exists) return NextResponse.json({ error: 'Benutzer nicht gefunden.' }, { status: 404 })
    db.prepare('INSERT OR IGNORE INTO group_members (user_id, group_id) VALUES (?, ?)').run(memberId, groupId)
  }
  if (body.removeMember !== undefined) {
    db.prepare('DELETE FROM group_members WHERE user_id = ? AND group_id = ?').run(body.removeMember, groupId)
  }

  addAuditLog({ userId: session.id, userEmail: session.email, action: 'UPDATE_GROUP', target: String(groupId) })
  return NextResponse.json({ ok: true })
}

export async function DELETE(req: NextRequest, { params }: Params) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.groups')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { id } = await params
  const delId = parseInt(id, 10)
  if (isNaN(delId)) return NextResponse.json({ error: 'Ungültige Gruppen-ID.' }, { status: 400 })
  db.prepare('DELETE FROM groups WHERE id = ?').run(delId)
  addAuditLog({ userId: session.id, userEmail: session.email, action: 'DELETE_GROUP', target: id })
  return NextResponse.json({ ok: true })
}
