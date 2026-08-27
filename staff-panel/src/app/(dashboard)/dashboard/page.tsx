import { getSession } from '@/lib/auth'
import db from '@/lib/db'
import type { AuditLog } from '@/lib/db'
import DashboardClient from './DashboardClient'

export default async function DashboardPage() {
  const session = await getSession()

  const userCount = (db.prepare('SELECT COUNT(*) as c FROM users').get() as { c: number }).c
  const isAdmin = session!.role === 'SUPER_ADMIN' || session!.role === 'ADMIN'
  const recentLogs = isAdmin
    ? db.prepare('SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 10').all() as AuditLog[]
    : []

  return (
    <DashboardClient
      session={session!}
      userCount={userCount}
      recentLogs={recentLogs}
    />
  )
}
