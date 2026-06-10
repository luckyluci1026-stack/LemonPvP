'use client'

import { useState, useEffect, useCallback } from 'react'

type Mailbox = {
  username: string; name: string; quota: number; quota_used: number; active: number; created: string
}
type Alias = { id: number; address: string; goto: string; active: number; created: string }

type Tab = 'mailboxes' | 'aliases'
type Modal = 'new-mailbox' | 'new-alias' | 'edit' | null

export default function EmailsPage() {
  const [tab, setTab] = useState<Tab>('mailboxes')
  const [mailboxes, setMailboxes] = useState<Mailbox[]>([])
  const [aliases, setAliases] = useState<Alias[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState<Modal>(null)
  const [editTarget, setEditTarget] = useState<Mailbox | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const [mbRes, alRes] = await Promise.all([
        fetch('/api/emails?type=mailboxes'),
        fetch('/api/emails?type=aliases'),
      ])
      const mbData = await mbRes.json()
      const alData = await alRes.json()
      if (!mbRes.ok) setError(mbData.error)
      else { setMailboxes(mbData.mailboxes ?? []); setAliases(alData.aliases ?? []) }
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  async function deleteMailbox(email: string) {
    if (!confirm(`${email} wirklich löschen?`)) return
    const res = await fetch('/api/emails', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email }) })
    if (res.ok) load()
    else { const d = await res.json(); setError(d.error) }
  }

  async function deleteAlias(id: number, addr: string) {
    if (!confirm(`Alias ${addr} löschen?`)) return
    const res = await fetch('/api/emails', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ aliasId: id }) })
    if (res.ok) load()
    else { const d = await res.json(); setError(d.error) }
  }

  async function toggleMailbox(mb: Mailbox) {
    const res = await fetch('/api/emails', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: mb.username, active: mb.active !== 1 }),
    })
    if (res.ok) load()
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-white">E-Mail Verwaltung</h1>
          <p className="text-gray-500 text-sm mt-1">Mailboxen und Aliases für @lemonpvp.de</p>
        </div>
        <div className="flex gap-2">
          <button className="btn-primary btn-sm" onClick={() => setModal('new-mailbox')}>+ Mailbox</button>
          <button className="btn-secondary btn-sm" onClick={() => setModal('new-alias')}>+ Alias</button>
        </div>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4 text-red-400 text-sm">
          ⚠️ {error}
          {error.includes('nicht konfiguriert') && (
            <span> — Gehe zu <a href="/settings" className="underline">Einstellungen</a> um Mailcow zu konfigurieren.</span>
          )}
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-1 bg-dark-800 p-1 rounded-lg w-fit border border-dark-600">
        {(['mailboxes', 'aliases'] as Tab[]).map(t => (
          <button
            key={t}
            className={`px-4 py-1.5 rounded-md text-sm font-medium transition-all ${tab === t ? 'bg-lemon-500 text-dark-900' : 'text-gray-400 hover:text-gray-200'}`}
            onClick={() => setTab(t)}
          >
            {t === 'mailboxes' ? '📬 Mailboxen' : '↩️ Aliases'}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="text-gray-500 text-sm">Lade…</div>
      ) : tab === 'mailboxes' ? (
        <div className="table-wrap">
          <table>
            <thead><tr><th>E-Mail</th><th>Name</th><th>Speicher</th><th>Status</th><th>Aktionen</th></tr></thead>
            <tbody>
              {mailboxes.length === 0 ? (
                <tr><td colSpan={5} className="text-center text-gray-600 py-6">Keine Mailboxen vorhanden.</td></tr>
              ) : mailboxes.map(mb => (
                <tr key={mb.username}>
                  <td><span className="font-mono text-lemon-400 text-xs">{mb.username}</span></td>
                  <td>{mb.name}</td>
                  <td>
                    <div className="text-xs">
                      {Math.round(mb.quota_used / 1024)} / {Math.round(mb.quota / 1024)} MB
                      <div className="w-24 h-1 bg-dark-500 rounded-full mt-1">
                        <div
                          className="h-1 bg-lemon-500 rounded-full"
                          style={{ width: `${Math.min(100, (mb.quota_used / mb.quota) * 100)}%` }}
                        />
                      </div>
                    </div>
                  </td>
                  <td>
                    <span className={mb.active === 1 ? 'badge-green badge' : 'badge-red badge'}>
                      {mb.active === 1 ? 'Aktiv' : 'Gesperrt'}
                    </span>
                  </td>
                  <td>
                    <div className="flex gap-2">
                      <button className="btn-secondary btn-sm text-xs" onClick={() => { setEditTarget(mb); setModal('edit') }}>
                        ✏️
                      </button>
                      <button className="btn-secondary btn-sm text-xs" onClick={() => toggleMailbox(mb)}>
                        {mb.active === 1 ? '🚫' : '✅'}
                      </button>
                      <button className="btn-danger btn-sm text-xs" onClick={() => deleteMailbox(mb.username)}>
                        🗑️
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Alias</th><th>Weiterleitung an</th><th>Erstellt</th><th>Aktion</th></tr></thead>
            <tbody>
              {aliases.length === 0 ? (
                <tr><td colSpan={4} className="text-center text-gray-600 py-6">Keine Aliases vorhanden.</td></tr>
              ) : aliases.map(a => (
                <tr key={a.id}>
                  <td><span className="font-mono text-lemon-400 text-xs">{a.address}</span></td>
                  <td className="text-xs font-mono text-gray-400 max-w-xs truncate">{a.goto}</td>
                  <td className="text-xs text-gray-500">{new Date(a.created).toLocaleDateString('de-DE')}</td>
                  <td><button className="btn-danger btn-sm text-xs" onClick={() => deleteAlias(a.id, a.address)}>🗑️</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modal === 'new-mailbox' && <NewMailboxModal onClose={() => setModal(null)} onSave={load} />}
      {modal === 'new-alias' && <NewAliasModal onClose={() => setModal(null)} onSave={load} />}
      {modal === 'edit' && editTarget && (
        <EditMailboxModal mailbox={editTarget} onClose={() => setModal(null)} onSave={load} />
      )}
    </div>
  )
}

function ModalWrapper({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-dark-700 border border-dark-500 rounded-xl p-6 w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between mb-5">
          <h3 className="font-semibold text-white text-lg">{title}</h3>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-300">✕</button>
        </div>
        {children}
      </div>
    </div>
  )
}

function NewMailboxModal({ onClose, onSave }: { onClose: () => void; onSave: () => void }) {
  const [form, setForm] = useState({ localPart: '', name: '', password: '', quota: '2048' })
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)
  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement>) => setForm(f => ({ ...f, [k]: e.target.value }))

  async function submit() {
    setErr(''); setLoading(true)
    const res = await fetch('/api/emails', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: 'mailbox', ...form, quotaMb: Number(form.quota) }),
    })
    const data = await res.json()
    setLoading(false)
    if (!res.ok) { setErr(data.error); return }
    onSave(); onClose()
  }

  return (
    <ModalWrapper title="📬 Neue Mailbox erstellen" onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="label">Local-Part</label>
          <div className="flex items-center gap-1">
            <input className="input" value={form.localPart} onChange={set('localPart')} placeholder="support" />
            <span className="text-gray-500 text-sm whitespace-nowrap">@lemonpvp.de</span>
          </div>
        </div>
        <div>
          <label className="label">Anzeigename</label>
          <input className="input" value={form.name} onChange={set('name')} placeholder="Support Team" />
        </div>
        <div>
          <label className="label">Passwort</label>
          <input className="input" type="password" value={form.password} onChange={set('password')} placeholder="••••••••" />
        </div>
        <div>
          <label className="label">Speicher (MB)</label>
          <input className="input" type="number" value={form.quota} onChange={set('quota')} />
        </div>
        {err && <div className="text-red-400 text-sm">{err}</div>}
        <div className="flex gap-2 justify-end">
          <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
          <button className="btn-primary btn-sm" onClick={submit} disabled={loading}>
            {loading ? '…' : 'Erstellen'}
          </button>
        </div>
      </div>
    </ModalWrapper>
  )
}

function NewAliasModal({ onClose, onSave }: { onClose: () => void; onSave: () => void }) {
  const [address, setAddress] = useState('')
  const [goto, setGoto] = useState('')
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit() {
    setErr(''); setLoading(true)
    const res = await fetch('/api/emails', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: 'alias', address, goto }),
    })
    const data = await res.json(); setLoading(false)
    if (!res.ok) { setErr(data.error); return }
    onSave(); onClose()
  }

  return (
    <ModalWrapper title="↩️ Neuen Alias erstellen" onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="label">Alias-Adresse</label>
          <input className="input" value={address} onChange={e => setAddress(e.target.value)} placeholder="support@lemonpvp.de" />
        </div>
        <div>
          <label className="label">Weiterleitung an (kommagetrennt)</label>
          <input className="input" value={goto} onChange={e => setGoto(e.target.value)} placeholder="lemonightt@lemonpvp.de,admin@lemonpvp.de" />
        </div>
        {err && <div className="text-red-400 text-sm">{err}</div>}
        <div className="flex gap-2 justify-end">
          <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
          <button className="btn-primary btn-sm" onClick={submit} disabled={loading}>{loading ? '…' : 'Erstellen'}</button>
        </div>
      </div>
    </ModalWrapper>
  )
}

function EditMailboxModal({ mailbox, onClose, onSave }: { mailbox: Mailbox; onClose: () => void; onSave: () => void }) {
  const [name, setName] = useState(mailbox.name)
  const [password, setPassword] = useState('')
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit() {
    setErr(''); setLoading(true)
    const res = await fetch('/api/emails', {
      method: 'PATCH', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: mailbox.username, name, password: password || undefined }),
    })
    const data = await res.json(); setLoading(false)
    if (!res.ok) { setErr(data.error); return }
    onSave(); onClose()
  }

  return (
    <ModalWrapper title={`✏️ ${mailbox.username}`} onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="label">Anzeigename</label>
          <input className="input" value={name} onChange={e => setName(e.target.value)} />
        </div>
        <div>
          <label className="label">Neues Passwort (leer lassen = unverändert)</label>
          <input className="input" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" />
        </div>
        {err && <div className="text-red-400 text-sm">{err}</div>}
        <div className="flex gap-2 justify-end">
          <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
          <button className="btn-primary btn-sm" onClick={submit} disabled={loading}>{loading ? '…' : 'Speichern'}</button>
        </div>
      </div>
    </ModalWrapper>
  )
}
