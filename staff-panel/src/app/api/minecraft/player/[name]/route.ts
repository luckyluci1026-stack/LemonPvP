import { NextRequest, NextResponse } from 'next/server'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'
import { addAuditLog } from '@/lib/db'
import * as mc from '@/lib/minecraft'

type Params = { params: Promise<{ name: string }> }

export async function GET(req: NextRequest, { params }: Params) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'player.view')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { name } = await params
  try {
    const player = await mc.getPlayer(name)
    return NextResponse.json({ player })
  } catch (e: unknown) {
    return NextResponse.json({ error: e instanceof Error ? e.message : 'Fehler' }, { status: 502 })
  }
}

export async function POST(req: NextRequest, { params }: Params) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  const { name } = await params
  const url = new URL(req.url)
  const action = url.searchParams.get('action')
  const body = await req.json().catch(() => ({})) as Record<string, unknown>

  try {
    switch (action) {
      case 'ban': {
        if (!hasPermission(session, 'player.ban')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
        await mc.banPlayer(name, String(body.reason ?? 'Kein Grund angegeben'), String(body.duration ?? 'permanent'))
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_BAN', target: name, details: String(body.reason) })
        break
      }
      case 'unban': {
        if (!hasPermission(session, 'player.unban')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
        await mc.unbanPlayer(name)
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_UNBAN', target: name })
        break
      }
      case 'coins': {
        if (!hasPermission(session, 'player.coins')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
        await mc.setCoins(name, Number(body.amount), (body.action as 'add' | 'remove' | 'set') ?? 'add')
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_COINS', target: name, details: `${body.action} ${body.amount}` })
        break
      }
      case 'rank': {
        if (!hasPermission(session, 'player.rank')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
        await mc.setRank(name, String(body.rank))
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_RANK', target: name, details: String(body.rank) })
        break
      }
      default:
        return NextResponse.json({ error: 'Unbekannte Aktion.' }, { status: 400 })
    }
    return NextResponse.json({ ok: true })
  } catch (e: unknown) {
    return NextResponse.json({ error: e instanceof Error ? e.message : 'Fehler' }, { status: 502 })
  }
}
