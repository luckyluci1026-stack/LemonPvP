import { NextRequest, NextResponse } from 'next/server'
import { verifyToken } from '@/lib/auth-edge'

const PUBLIC_PATHS = [
  '/login',
  '/api/auth/login',
  '/change-password',
  '/api/auth/change-password',
  '/api/auth/force-logout',
  '/api/public',
]

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl

  if (PUBLIC_PATHS.some(p => pathname.startsWith(p))) return NextResponse.next()
  if (pathname.startsWith('/_next') || pathname.startsWith('/favicon')) return NextResponse.next()

  // CSRF: reject cross-origin non-GET API requests (defense-in-depth on top of sameSite:strict)
  if (req.method !== 'GET' && pathname.startsWith('/api/')) {
    const origin = req.headers.get('origin')
    const host   = req.headers.get('host')
    if (origin && host) {
      try {
        if (new URL(origin).host !== host) {
          return new NextResponse(JSON.stringify({ error: 'Forbidden' }), { status: 403, headers: { 'content-type': 'application/json' } })
        }
      } catch {
        return new NextResponse(JSON.stringify({ error: 'Forbidden' }), { status: 403, headers: { 'content-type': 'application/json' } })
      }
    }
  }

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
