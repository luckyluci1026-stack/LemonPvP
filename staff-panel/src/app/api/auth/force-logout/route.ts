import { NextRequest, NextResponse } from 'next/server'

export async function GET(req: NextRequest) {
  const res = NextResponse.redirect(new URL('/login?r=suspended', req.url))
  res.cookies.delete('lemon_session')
  return res
}
