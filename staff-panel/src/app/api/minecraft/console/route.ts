import { NextRequest, NextResponse } from 'next/server'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'
import { addAuditLog } from '@/lib/db'
import { executeCommand } from '@/lib/minecraft'

// Commands that could crash or destabilize the server — blocked for all staff.
// SUPER_ADMIN can still run them by SSHing to the server directly.
const BLOCKED = /^(stop|restart|reload|op|deop|whitelist\s+(add|remove)|ban-ip|pardon-ip)\b/i

export async function POST(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.console')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { command } = await req.json() as { command: string }
  const cmd = command?.trim()
  if (!cmd) return NextResponse.json({ error: 'Befehl erforderlich.' }, { status: 400 })
  if (cmd.length > 512) return NextResponse.json({ error: 'Befehl zu lang (max. 512 Zeichen).' }, { status: 400 })

  if (BLOCKED.test(cmd)) {
    return NextResponse.json(
      { error: `Der Befehl "${cmd.split(' ')[0]}" ist aus Sicherheitsgründen im Panel gesperrt.` },
      { status: 403 }
    )
  }

  try {
    const result = await executeCommand(cmd)
    addAuditLog({
      userId: session.id,
      userEmail: session.email,
      action: 'CONSOLE_CMD',
      details: cmd,
    })
    return NextResponse.json({ ok: true, output: result.output })
  } catch (e: unknown) {
    return NextResponse.json({ error: e instanceof Error ? e.message : 'Fehler' }, { status: 502 })
  }
}
