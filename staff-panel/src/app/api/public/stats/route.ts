import { NextResponse } from 'next/server'
import { getSetting } from '@/lib/db'

export async function GET() {
  const mcApiUrl = getSetting('mc_api_url')
  const mcApiKey = getSetting('mc_api_key')

  if (!mcApiUrl || !mcApiKey) {
    return NextResponse.json({ online: 0, max: 0, tps: 20, uptime_seconds: 0, version: '' })
  }

  try {
    const res = await fetch(`${mcApiUrl}/api/server/stats`, {
      headers: { Authorization: `Bearer ${mcApiKey}` },
      signal: AbortSignal.timeout(5000),
      cache: 'no-store',
    })
    if (!res.ok) throw new Error('API error')
    const data = await res.json()
    return NextResponse.json(data.stats ?? { online: 0, max: 0, tps: 20, uptime_seconds: 0, version: '' })
  } catch {
    return NextResponse.json({ online: 0, max: 0, tps: 20, uptime_seconds: 0, version: '' })
  }
}
