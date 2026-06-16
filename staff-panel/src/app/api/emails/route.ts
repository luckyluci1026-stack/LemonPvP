import { NextRequest, NextResponse } from 'next/server'
import { getSessionFromRequest, hasPermission } from '@/lib/auth'
import { addAuditLog } from '@/lib/db'
import * as mc from '@/lib/mailcow'
import { rateLimit } from '@/lib/rateLimit'

export async function GET(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.emails')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const url = new URL(req.url)
  const type = url.searchParams.get('type') ?? 'mailboxes'

  try {
    if (type === 'aliases') {
      const aliases = await mc.listAliases()
      return NextResponse.json({ aliases })
    }
    const mailboxes = await mc.listMailboxes()
    return NextResponse.json({ mailboxes })
  } catch (e: unknown) {
    return NextResponse.json({ error: e instanceof Error ? e.message : 'Mailcow-Fehler' }, { status: 502 })
  }
}

export async function POST(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.emails')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  if (!rateLimit(`email:${session.id}`, 10, 60 * 1000)) {
    return NextResponse.json({ error: 'Zu viele Aktionen in kurzer Zeit. Bitte kurz warten.' }, { status: 429 })
  }

  const body = await req.json() as {
    type: 'mailbox' | 'alias'
    localPart?: string
    name?: string
    password?: string
    quotaMb?: number
    address?: string
    goto?: string
  }

  try {
    if (body.type === 'alias') {
      if (!body.address || !body.goto) return NextResponse.json({ error: 'Adresse und Ziel erforderlich.' }, { status: 400 })
      await mc.createAlias({ address: body.address, goto: body.goto })
      addAuditLog({ userId: session.id, userEmail: session.email, action: 'CREATE_ALIAS', target: body.address })
    } else {
      if (!body.localPart || !body.name || !body.password) {
        return NextResponse.json({ error: 'Local-Part, Name und Passwort erforderlich.' }, { status: 400 })
      }
      await mc.createMailbox({ localPart: body.localPart, name: body.name, password: body.password, quotaMb: body.quotaMb })
      addAuditLog({ userId: session.id, userEmail: session.email, action: 'CREATE_MAILBOX', target: body.localPart })
    }
    return NextResponse.json({ ok: true })
  } catch (e: unknown) {
    return NextResponse.json({ error: e instanceof Error ? e.message : 'Fehler' }, { status: 500 })
  }
}

export async function PATCH(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.emails')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const body = await req.json() as {
    email: string; name?: string; password?: string; active?: boolean; quotaMb?: number
  }

  try {
    await mc.updateMailbox(body.email, { name: body.name, password: body.password, active: body.active, quotaMb: body.quotaMb })
    addAuditLog({ userId: session.id, userEmail: session.email, action: 'UPDATE_MAILBOX', target: body.email })
    return NextResponse.json({ ok: true })
  } catch (e: unknown) {
    return NextResponse.json({ error: e instanceof Error ? e.message : 'Fehler' }, { status: 500 })
  }
}

export async function DELETE(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (!hasPermission(session, 'admin.emails')) return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const { email, aliasId } = await req.json() as { email?: string; aliasId?: number }
  if (aliasId === undefined && !email) {
    return NextResponse.json({ error: 'email oder aliasId erforderlich.' }, { status: 400 })
  }
  try {
    if (aliasId !== undefined) {
      await mc.deleteAlias(aliasId)
      addAuditLog({ userId: session.id, userEmail: session.email, action: 'DELETE_ALIAS', target: String(aliasId) })
    } else {
      await mc.deleteMailbox(email!)
      addAuditLog({ userId: session.id, userEmail: session.email, action: 'DELETE_MAILBOX', target: email! })
    }
    return NextResponse.json({ ok: true })
  } catch (e: unknown) {
    return NextResponse.json({ error: e instanceof Error ? e.message : 'Fehler' }, { status: 500 })
  }
}
