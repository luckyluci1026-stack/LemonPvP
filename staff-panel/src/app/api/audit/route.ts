import { NextRequest, NextResponse } from 'next/server'
import db, { type AuditLog } from '@/lib/db'
import { getSessionFromRequest } from '@/lib/auth'

export async function GET(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (session.role !== 'SUPER_ADMIN' && session.role !== 'ADMIN') {
    return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
  }

  const logs = db.prepare(
    'SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 200'
  ).all() as AuditLog[]

  return NextResponse.json({ logs })
}
