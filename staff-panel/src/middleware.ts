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

  const token = req.cookies.get('lemon_session')?.value
  if (!token) return NextResponse.redirect(new URL('/login', req.url))

  const session = await verifyToken(token)
  if (!session) {
    const res = NextResponse.redirect(new URL('/login', req.url))
    res.cookies.delete('lemon_session')
    return res
  }

  if (session.mustChangePassword) {
    return NextResponse.redirect(new URL('/change-password', req.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
