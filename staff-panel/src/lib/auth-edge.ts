// Edge-Runtime-compatible JWT utilities (no Node.js modules imported)
import { SignJWT, jwtVerify } from 'jose'

function getSecret(): Uint8Array {
  const s = process.env.JWT_SECRET
  if (!s || s.length < 32) throw new Error('JWT_SECRET must be set (>=32 chars) in .env.local')
  return new TextEncoder().encode(s)
}

export type SessionUser = {
  id: number
  email: string
  name: string
  role: string
  permissions: string[]
  mustChangePassword?: boolean
}

export async function verifyToken(token: string): Promise<SessionUser | null> {
  try {
    const { payload } = await jwtVerify(token, getSecret())
    const p = payload as Partial<SessionUser>
    if (typeof p.id !== 'number' || typeof p.role !== 'string' || !Array.isArray(p.permissions)) return null
    return p as SessionUser
  } catch {
    return null
  }
}

export async function createToken(user: SessionUser): Promise<string> {
  return new SignJWT({ ...user })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt()
    .setExpirationTime('12h')
    .sign(getSecret())
}
