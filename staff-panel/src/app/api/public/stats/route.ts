import { NextResponse } from 'next/server'
import { getSetting } from '@/lib/db'

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET',
}

export async function OPTIONS() {
  return new NextResponse(null, { status: 204, headers: CORS })
}

export async function GET() {
  const mcApiUrl = getSetting('mc_api_url')
  const mcApiKey = getSetting('mc_api_key')

  if (!mcApiUrl || !mcApiKey) {
    return NextResponse.json({ online: 0, max: 0, tps: 20, uptime_seconds: 0, version: '' }, { headers: CORS })
  }

  try {
    const res = await fetch(`${mcApiUrl}/api/server/stats`, {
      headers: { Authorization: `Bearer ${mcApiKey}` },
      signal: AbortSignal.timeout(5000),
      cache: 'no-store',
    })
    if (!res.ok) throw new Error('API error')
    const data = await res.json()
    return NextResponse.json(data.stats ?? { online: 0, max: 0, tps: 20, uptime_seconds: 0, version: '' }, { headers: CORS })
  } catch {
    return NextResponse.json({ online: 0, max: 0, tps: 20, uptime_seconds: 0, version: '' }, { headers: CORS })
  }
}
