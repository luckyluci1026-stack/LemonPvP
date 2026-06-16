import { NextRequest, NextResponse } from 'next/server'
import { verifyToken } from '@/lib/auth-edge'

const PUBLIC_PATHS = [
  '/login',
  '/api/auth/login',
  '/change-password',
  '/api/auth/change-password',
  '/api/auth/logout',
  '/api/auth/force-logout',
  '/api/public',
]

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl

  if (PUBLIC_PATHS.some(p => pathname.startsWith(p))) return NextResponse.next()
  if (pathname.startsWith('/_next') || pathname.startsWith('/favicon')) return NextResponse.next()

  const token = req.cookies.get('lemon_session')?.value
  if (!token) {
    if (pathname.startsWith('/api/')) return new NextResponse(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: { 'content-type': 'application/json' } })
    return NextResponse.redirect(new URL('/login', req.url))
  }

  const session = await verifyToken(token)
  if (!session) {
    const res = pathname.startsWith('/api/')
      ? new NextResponse(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: { 'content-type': 'application/json' } })
      : NextResponse.redirect(new URL('/login', req.url))
    res.cookies.delete('lemon_session')
    return res
  }

  if (session.mustChangePassword) {
    if (pathname.startsWith('/api/')) {
      return new NextResponse(JSON.stringify({ error: 'Passwortänderung erforderlich.' }), { status: 403, headers: { 'content-type': 'application/json' } })
    }
    return NextResponse.redirect(new URL('/change-password', req.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
