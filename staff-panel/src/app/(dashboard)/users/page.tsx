'use client'

import { useState, useEffect, useCallback } from 'react'
import { useToast } from '@/components/ui/Toast'

type User = {
  id: number; email: string; name: string; role: string
  suspended: number; permissions: string; created_at: string
}
type Modal = 'create' | 'edit' | null

const ALL_PERMS = [
  { key: 'admin.emails', label: '📧 E-Mails verwalten' },
  { key: 'admin.users', label: '👥 Staff verwalten' },
  { key: 'admin.groups', label: '🏷️ Gruppen verwalten' },
  { key: 'admin.console', label: '💻 Konsole' },
  { key: 'player.view', label: '🔍 Spieler ansehen' },
  { key: 'player.ban', label: '🔨 Spieler bannen' },
  { key: 'player.unban', label: '✅ Spieler entbannen' },
  { key: 'player.mute', label: '🔇 Spieler stumm schalten' },
  { key: 'player.coins', label: '💰 Coins verwalten' },
  { key: 'player.rank', label: '🏆 Ränge verwalten' },
]

export default function UsersPage() {
  const { showToast } = useToast()
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<Modal>(null)
  const [editUser, setEditUser] = useState<User | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    const res = await fetch('/api/users')
    if (res.ok) { const d = await res.json(); setUsers(d.users) }
    setLoading(false)
  }, [])

  useEffect(() => { load() }, [load])

  async function toggleSuspend(u: User) {
    const res = await fetch(`/api/users/${u.id}`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ suspended: !u.suspended }),
    })
    if (!res.ok) { const d = await res.json(); showToast(d.error, 'error'); return }
    load()
  }

  async function deleteUser(u: User) {
    if (!confirm(`${u.name} (${u.email}) wirklich löschen?`)) return
    const res = await fetch(`/api/users/${u.id}`, { method: 'DELETE' })
    if (!res.ok) { const d = await res.json(); showToast(d.error, 'error'); return }
    showToast(`${u.name} wurde gelöscht.`)
    load()
  }

  const roleColor = (role: string) => {
    if (role === 'SUPER_ADMIN') return 'badge-yellow'
    if (role === 'ADMIN') return 'badge-blue'
    return 'badge-gray'
  }

  const roleLabel = (role: string) => {
    if (role === 'SUPER_ADMIN') return '👑 Super Admin'
    if (role === 'ADMIN') return '🔑 Admin'
    return '🎯 Staff'
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-white">Staff Accounts</h1>
          <p className="text-gray-500 text-sm mt-1">Accounts verwalten und Berechtigungen setzen</p>
        </div>
        <button className="btn-primary btn-sm" onClick={() => { setEditUser(null); setModal('create') }}>
          + Account erstellen
        </button>
      </div>

      {loading ? <div className="text-gray-500 text-sm">Lade…</div> : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Name / E-Mail</th><th>Rolle</th><th>Berechtigungen</th><th>Status</th><th>Erstellt</th><th>Aktionen</th></tr>
            </thead>
            <tbody>
              {users.map(u => {
                const perms: string[] = JSON.parse(u.permissions)
                return (
                  <tr key={u.id}>
                    <td>
                      <div className="font-medium text-gray-100">{u.name}</div>
                      <div className="text-xs text-gray-500">{u.email}</div>
                    </td>
                    <td><span className={`badge ${roleColor(u.role)}`}>{roleLabel(u.role)}</span></td>
                    <td>
                      <div className="flex flex-wrap gap-1">
                        {perms.includes('*') ? (
                          <span className="badge badge-yellow">Alle Rechte</span>
                        ) : perms.length === 0 ? (
                          <span className="text-gray-600 text-xs">Keine</span>
                        ) : (
                          perms.slice(0, 3).map(p => (
                            <span key={p} className="badge badge-gray text-xs">{p}</span>
                          ))
                        )}
                        {perms.length > 3 && <span className="badge badge-gray">+{perms.length - 3}</span>}
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${u.suspended ? 'badge-red' : 'badge-green'}`}>
                        {u.suspended ? '🚫 Gesperrt' : '✅ Aktiv'}
                      </span>
                    </td>
                    <td className="text-xs text-gray-500">{new Date(u.created_at).toLocaleDateString('de-DE')}</td>
                    <td>
                      <div className="flex gap-1.5">
                        <button className="btn-secondary btn-sm text-xs" onClick={() => { setEditUser(u); setModal('edit') }}>✏️</button>
                        <button className="btn-secondary btn-sm text-xs" onClick={() => toggleSuspend(u)}>
                          {u.suspended ? '✅' : '🚫'}
                        </button>
                        <button className="btn-danger btn-sm text-xs" onClick={() => deleteUser(u)}>🗑️</button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {modal === 'create' && <UserModal user={null} onClose={() => setModal(null)} onSave={load} />}
      {modal === 'edit' && editUser && <UserModal user={editUser} onClose={() => setModal(null)} onSave={load} />}
    </div>
  )
}

function UserModal({ user, onClose, onSave }: { user: User | null; onClose: () => void; onSave: () => void }) {
  const editing = !!user
  const [form, setForm] = useState({
    name: user?.name ?? '',
    email: user?.email ?? '',
    password: '',
    role: user?.role ?? 'STAFF',
    permissions: user ? (JSON.parse(user.permissions) as string[]) : [] as string[],
  })
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit() {
    setErr(''); setLoading(true)
    const body = editing
      ? { name: form.name, password: form.password || undefined, role: form.role, permissions: form.permissions }
      : { ...form }
    const res = await fetch(editing ? `/api/users/${user!.id}` : '/api/users', {
      method: editing ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    const data = await res.json(); setLoading(false)
    if (!res.ok) { setErr(data.error); return }
    onSave(); onClose()
  }

  function togglePerm(perm: string) {
    setForm(f => ({
      ...f,
      permissions: f.permissions.includes(perm)
        ? f.permissions.filter(p => p !== perm)
        : [...f.permissions, perm],
    }))
  }

  const isAllPerms = form.role === 'SUPER_ADMIN' || form.permissions.includes('*')

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-dark-700 border border-dark-500 rounded-xl p-6 w-full max-w-lg shadow-2xl max-h-screen overflow-y-auto">
        <div className="flex items-center justify-between mb-5">
          <h3 className="font-semibold text-white text-lg">{editing ? '✏️ Account bearbeiten' : '👤 Account erstellen'}</h3>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-300">✕</button>
        </div>

        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">Name</label>
              <input className="input" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div>
              <label className="label">Rolle</label>
              <select className="input" value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
                <option value="STAFF">🎯 Staff</option>
                <option value="ADMIN">🔑 Admin</option>
                <option value="SUPER_ADMIN">👑 Super Admin</option>
              </select>
            </div>
          </div>

          {!editing && (
            <div>
              <label className="label">E-Mail</label>
              <input className="input" type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} placeholder="name@lemonpvp.de" />
            </div>
          )}

          <div>
            <label className="label">{editing ? 'Neues Passwort (leer = unverändert)' : 'Passwort'}</label>
            <input className="input" type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} placeholder="••••••••" />
          </div>

          <div>
            <label className="label">Berechtigungen</label>
            {isAllPerms ? (
              <div className="bg-lemon-500/10 border border-lemon-500/20 rounded-lg p-3 text-lemon-400 text-sm">
                👑 Alle Berechtigungen (Super Admin / Wildcard)
              </div>
            ) : (
              <div className="grid grid-cols-1 gap-2 max-h-48 overflow-y-auto">
                {ALL_PERMS.map(p => (
                  <label key={p.key} className="flex items-center gap-3 p-2 rounded-lg hover:bg-dark-600 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={form.permissions.includes(p.key)}
                      onChange={() => togglePerm(p.key)}
                      className="accent-lemon-500"
                    />
                    <span className="text-sm text-gray-300">{p.label}</span>
                  </label>
                ))}
                <label className="flex items-center gap-3 p-2 rounded-lg hover:bg-dark-600 cursor-pointer border border-lemon-500/20">
                  <input
                    type="checkbox"
                    checked={form.permissions.includes('*')}
                    onChange={() => togglePerm('*')}
                    className="accent-lemon-500"
                  />
                  <span className="text-sm text-lemon-400 font-medium">⭐ Alle Rechte (Wildcard)</span>
                </label>
              </div>
            )}
          </div>

          {err && <div className="text-red-400 text-sm">{err}</div>}

          <div className="flex gap-2 justify-end pt-2">
            <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
            <button className="btn-primary btn-sm" onClick={submit} disabled={loading}>
              {loading ? '…' : editing ? 'Speichern' : 'Erstellen'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
