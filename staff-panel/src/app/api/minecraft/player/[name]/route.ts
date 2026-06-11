import { NextRequest, NextResponse } from 'next/server'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'
import { addAuditLog } from '@/lib/db'
import * as mc from '@/lib/minecraft'

// permissions map for player actions
const ACTION_PERMS: Record<string, string> = {
  ban: 'player.ban', unban: 'player.unban',
  mute: 'player.mute', unmute: 'player.mute',
  coins: 'player.coins', rank: 'player.rank',
}

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
        const reason = typeof body.reason === 'string' && body.reason.trim() ? body.reason.trim() : 'Kein Grund angegeben'
        const duration = typeof body.duration === 'string' && body.duration.trim() ? body.duration.trim() : 'permanent'
        await mc.banPlayer(name, reason, duration)
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_BAN', target: name, details: reason })
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
        const amount = Number(body.amount)
        if (!Number.isFinite(amount)) return NextResponse.json({ error: 'amount muss eine Zahl sein.' }, { status: 400 })
        const coinAction = ['add', 'remove', 'set'].includes(String(body.action)) ? (body.action as 'add' | 'remove' | 'set') : 'add'
        await mc.setCoins(name, amount, coinAction)
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_COINS', target: name, details: `${coinAction} ${amount}` })
        break
      }
      case 'rank': {
        if (!hasPermission(session, 'player.rank')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
        const rank = typeof body.rank === 'string' && body.rank.trim() ? body.rank.trim() : null
        if (!rank) return NextResponse.json({ error: 'rank darf nicht leer sein.' }, { status: 400 })
        await mc.setRank(name, rank)
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_RANK', target: name, details: rank })
        break
      }
      case 'mute': {
        if (!hasPermission(session, 'player.mute')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
        const reason = typeof body.reason === 'string' && body.reason.trim() ? body.reason.trim() : 'Kein Grund angegeben'
        const duration = typeof body.duration === 'string' && body.duration.trim() ? body.duration.trim() : 'permanent'
        await mc.mutePlayer(name, reason, duration)
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_MUTE', target: name, details: reason })
        break
      }
      case 'unmute': {
        if (!hasPermission(session, 'player.mute')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })
        await mc.unmutePlayer(name)
        addAuditLog({ userId: session.id, userEmail: session.email, action: 'PLAYER_UNMUTE', target: name })
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
