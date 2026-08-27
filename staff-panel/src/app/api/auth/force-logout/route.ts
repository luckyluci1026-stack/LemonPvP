import { NextResponse } from 'next/server'
import { clearSessionCookie } from '@/lib/auth'

// POST-only to prevent CSRF logout via <img src="..."> from cross-origin pages.
export async function POST() {
  await clearSessionCookie()
  return NextResponse.json({ ok: true })
}
