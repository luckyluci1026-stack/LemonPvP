import { NextRequest, NextResponse } from 'next/server'
import { getSessionFromRequest } from '@/lib/auth'
import { getStats } from '@/lib/minecraft'

export async function GET(req: NextRequest) {
  const session = await getSessionFromRequest(req)
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })

  try {
    const stats = await getStats()
    return NextResponse.json({ stats })
  } catch {
    return NextResponse.json({ stats: null, error: 'Server nicht erreichbar' })
  }
}
