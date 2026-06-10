import { NextRequest, NextResponse } from 'next/server'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'
import { addAuditLog } from '@/lib/db'
import { executeCommand } from '@/lib/minecraft'

export async function POST(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.console')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { command } = await req.json() as { command: string }
  if (!command?.trim()) return NextResponse.json({ error: 'Befehl erforderlich.' }, { status: 400 })

  try {
    const result = await executeCommand(command.trim())
    addAuditLog({
      userId: session.id,
      userEmail: session.email,
      action: 'CONSOLE_CMD',
      details: command.trim(),
    })
    return NextResponse.json({ ok: true, output: result.output })
  } catch (e: unknown) {
    return NextResponse.json({ error: e instanceof Error ? e.message : 'Fehler' }, { status: 502 })
  }
}
