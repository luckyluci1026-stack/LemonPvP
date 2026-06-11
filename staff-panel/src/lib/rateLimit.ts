// Simple in-memory rate limiter. Resets on server restart, which is fine
// for a staff panel (low user count, short-lived server processes).

type Entry = { count: number; resetAt: number }
const store = new Map<string, Entry>()

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

// Clean up expired entries every 5 minutes so the map doesn't grow forever.
setInterval(() => {
  const now = Date.now()
  for (const [k, v] of store) {
    if (now > v.resetAt) store.delete(k)
  }
}, 5 * 60 * 1000)
