import { getSetting } from './db'

function cfg() {
  return {
    url: getSetting('mc_api_url').replace(/\/$/, '') || 'http://localhost:8080',
    key: getSetting('mc_api_key'),
  }
}

async function mcFetch(path: string, options: RequestInit = {}) {
  const { url, key } = cfg()
  const res = await fetch(`${url}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${key}`,
      ...options.headers,
    },
    signal: AbortSignal.timeout(8000),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`MC API ${res.status}: ${text}`)
  }
  return res.json()
}

// ─── Types ─────────────────────────────────────────────────────────────────

export type McPlayer = {
  uuid: string
  name: string
  online: boolean
  coins: number
  elo: Record<string, number>
  rank: string
  banned: boolean
  ban_reason?: string
  ban_expires?: string | null
}

export type McStats = {
  online: number
  max: number
  tps: number
  uptime_seconds: number
  version: string
}

// ─── API calls ─────────────────────────────────────────────────────────────

export async function getPlayer(name: string): Promise<McPlayer> {
  return mcFetch(`/api/player/${encodeURIComponent(name)}`)
}

export async function banPlayer(name: string, reason: string, duration: string): Promise<void> {
  await mcFetch(`/api/player/${encodeURIComponent(name)}/ban`, {
    method: 'POST',
    body: JSON.stringify({ reason, duration }),
  })
}

export async function unbanPlayer(name: string): Promise<void> {
  await mcFetch(`/api/player/${encodeURIComponent(name)}/unban`, {
    method: 'POST',
  })
}

export async function setCoins(name: string, amount: number, action: 'add' | 'remove' | 'set'): Promise<void> {
  await mcFetch(`/api/player/${encodeURIComponent(name)}/coins`, {
    method: 'POST',
    body: JSON.stringify({ amount, action }),
  })
}

export async function setRank(name: string, rank: string): Promise<void> {
  await mcFetch(`/api/player/${encodeURIComponent(name)}/rank`, {
    method: 'POST',
    body: JSON.stringify({ rank }),
  })
}

export async function getStats(): Promise<McStats> {
  return mcFetch('/api/server/stats')
}

export async function executeCommand(command: string): Promise<{ output: string }> {
  return mcFetch('/api/console', {
    method: 'POST',
    body: JSON.stringify({ command }),
  })
}
