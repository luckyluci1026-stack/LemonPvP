import { NextRequest, NextResponse } from 'next/server'
import { getAllSettings, setSetting } from '@/lib/db'
import { getSessionFromRequest } from '@/lib/auth'

export async function GET(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (session.role !== 'SUPER_ADMIN') return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const settings = getAllSettings()
  // Mask sensitive values for display
  const safe = { ...settings }
  if (safe.mailcow_key) safe.mailcow_key = '••••••••'
  if (safe.mc_api_key)  safe.mc_api_key  = '••••••••'
  return NextResponse.json({ settings: safe })
}

export async function POST(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  if (session.role !== 'SUPER_ADMIN') return NextResponse.json({ error: 'Forbidden' }, { status: 403 })

  const body = await req.json() as Record<string, string>
  const allowed = ['mailcow_url', 'mailcow_key', 'mc_api_url', 'mc_api_key', 'domain', 'panel_name']

  for (const [k, v] of Object.entries(body)) {
    if (allowed.includes(k)) setSetting(k, v)
  }

  return NextResponse.json({ ok: true })
}
