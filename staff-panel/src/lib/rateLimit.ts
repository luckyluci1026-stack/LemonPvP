// Simple in-memory rate limiter. Guarded on globalThis so a single instance
// is shared across Next.js hot-reloads and module re-evaluations.

type Entry = { count: number; resetAt: number }

const g = globalThis as typeof globalThis & { __rlStore?: Map<string, Entry>; __rlTimer?: ReturnType<typeof setInterval> }
const store: Map<string, Entry> = g.__rlStore ??= new Map()

if (!g.__rlTimer) {
  const t = setInterval(() => {
    const now = Date.now()
    for (const [k, v] of store) {
      if (now > v.resetAt) store.delete(k)
    }
  }, 5 * 60 * 1000)
  // Don't hold the event loop open for cleanup alone
  if (typeof t === 'object' && 'unref' in t) (t as NodeJS.Timeout).unref()
  g.__rlTimer = t
}

export function rateLimit(key: string, limit: number, windowMs: number): boolean {
  const now = Date.now()
  const entry = store.get(key)
  if (!entry || now > entry.resetAt) {
    store.set(key, { count: 1, resetAt: now + windowMs })
    return true
  }
  if (entry.count >= limit) return false
  entry.count++
  return true
}
