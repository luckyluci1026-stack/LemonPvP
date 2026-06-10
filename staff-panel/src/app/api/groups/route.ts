import { NextRequest, NextResponse } from 'next/server'
import db, { addAuditLog, type Group } from '@/lib/db'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'

export async function GET(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const groups = db.prepare('SELECT * FROM groups ORDER BY name').all() as Group[]
  const membersRaw = db.prepare(`
    SELECT gm.group_id, u.id, u.name, u.email FROM group_members gm
    JOIN users u ON u.id = gm.user_id
  `).all() as { group_id: number; id: number; name: string; email: string }[]

  const memberMap: Record<number, { id: number; name: string; email: string }[]> = {}
  for (const m of membersRaw) {
    if (!memberMap[m.group_id]) memberMap[m.group_id] = []
    memberMap[m.group_id].push({ id: m.id, name: m.name, email: m.email })
  }

  return NextResponse.json({
    groups: groups.map(g => ({
      ...g,
      permissions: JSON.parse(g.permissions),
      members: memberMap[g.id] ?? [],
    })),
  })
}

export async function POST(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.groups')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { name, description, permissions } = await req.json() as {
    name: string; description?: string; permissions: string[]
  }

  if (!name) return NextResponse.json({ error: 'Name erforderlich.' }, { status: 400 })

  try {
    const result = db.prepare(`
      INSERT INTO groups (name, description, permissions) VALUES (?, ?, ?)
    `).run(name.trim(), description?.trim() ?? null, JSON.stringify(permissions ?? []))
    addAuditLog({ userId: session.id, userEmail: session.email, action: 'CREATE_GROUP', target: name })
    return NextResponse.json({ ok: true, id: result.lastInsertRowid })
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    if (msg.includes('UNIQUE')) return NextResponse.json({ error: 'Gruppenname bereits vergeben.' }, { status: 409 })
    throw e
  }
}
