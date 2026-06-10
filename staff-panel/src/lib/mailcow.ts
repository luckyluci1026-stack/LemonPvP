import { getSetting } from './db'

function cfg() {
  return {
    url: getSetting('mailcow_url').replace(/\/$/, ''),
    key: getSetting('mailcow_key'),
    domain: getSetting('domain') || 'lemonpvp.de',
  }
}

async function mcFetch(path: string, options: RequestInit = {}) {
  const { url, key } = cfg()
  if (!url || !key) throw new Error('Mailcow not configured. Go to Settings → Mailcow.')
  const res = await fetch(`${url}/api/v1${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': key,
      ...options.headers,
    },
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`Mailcow API error ${res.status}: ${text}`)
  }
  return res.json()
}

// ─── Mailbox ───────────────────────────────────────────────────────────────

export type Mailbox = {
  username: string    // full email, e.g. support@lemonpvp.de
  name: string
  quota: number
  quota_used: number
  active: number      // 1 = active, 0 = suspended
  created: string
  modified: string
}

export async function listMailboxes(): Promise<Mailbox[]> {
  const { domain } = cfg()
  return mcFetch(`/get/mailbox/all/${domain}`)
}

export async function getMailbox(email: string): Promise<Mailbox> {
  return mcFetch(`/get/mailbox/${encodeURIComponent(email)}`)
}

export async function createMailbox(params: {
  localPart: string
  name: string
  password: string
  quotaMb?: number
}): Promise<void> {
  const { domain } = cfg()
  await mcFetch('/add/mailbox', {
    method: 'POST',
    body: JSON.stringify({
      local_part:     params.localPart,
      domain:         domain,
      name:           params.name,
      password:       params.password,
      password2:      params.password,
      quota:          params.quotaMb ?? 2048,
      active:         '1',
      force_pw_update: '0',
    }),
  })
}

export async function updateMailbox(email: string, params: {
  name?: string
  password?: string
  active?: boolean
  quotaMb?: number
}): Promise<void> {
  const body: Record<string, unknown> = { items: [email] }
  if (params.name)     body.name = params.name
  if (params.quotaMb)  body.quota = params.quotaMb
  if (params.active !== undefined) body.active = params.active ? '1' : '0'
  await mcFetch('/edit/mailbox', { method: 'POST', body: JSON.stringify(body) })

  if (params.password) {
    await mcFetch('/edit/mailbox', {
      method: 'POST',
      body: JSON.stringify({
        items: [email],
        attr: { password: params.password, password2: params.password },
      }),
    })
  }
}

export async function deleteMailbox(email: string): Promise<void> {
  await mcFetch('/delete/mailbox', {
    method: 'POST',
    body: JSON.stringify([email]),
  })
}

// ─── Aliases ───────────────────────────────────────────────────────────────

export type Alias = {
  id: number
  address: string    // e.g. support@lemonpvp.de
  goto: string       // comma-separated target emails
  active: number
  created: string
}

export async function listAliases(): Promise<Alias[]> {
  const { domain } = cfg()
  return mcFetch(`/get/alias/all/${domain}`)
}

export async function createAlias(params: {
  address: string
  goto: string   // comma-separated list of target emails
}): Promise<void> {
  await mcFetch('/add/alias', {
    method: 'POST',
    body: JSON.stringify({
      address:    params.address,
      goto:       params.goto,
      active:     '1',
    }),
  })
}

export async function deleteAlias(id: number): Promise<void> {
  await mcFetch('/delete/alias', {
    method: 'POST',
    body: JSON.stringify([id]),
  })
}
