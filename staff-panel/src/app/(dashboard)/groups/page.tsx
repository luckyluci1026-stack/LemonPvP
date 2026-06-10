'use client'

import { useState, useEffect, useCallback } from 'react'

type Group = {
  id: number; name: string; description: string | null
  permissions: string[]; members: { id: number; name: string; email: string }[]
  created_at: string
}

export default function GroupsPage() {
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<'create' | 'edit' | null>(null)
  const [editGroup, setEditGroup] = useState<Group | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    const res = await fetch('/api/groups')
    if (res.ok) { const d = await res.json(); setGroups(d.groups) }
    setLoading(false)
  }, [])

  useEffect(() => { load() }, [load])

  async function deleteGroup(g: Group) {
    if (!confirm(`Gruppe "${g.name}" löschen?`)) return
    const res = await fetch(`/api/groups/${g.id}`, { method: 'DELETE' })
    if (!res.ok) { const d = await res.json(); setError(d.error); return }
    load()
  }

  async function removeMember(groupId: number, userId: number) {
    await fetch(`/api/groups/${groupId}`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ removeMember: userId }),
    })
    load()
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-white">Gruppen</h1>
          <p className="text-gray-500 text-sm mt-1">Gruppen für Berechtigungen und E-Mail-Routing verwalten</p>
        </div>
        <button className="btn-primary btn-sm" onClick={() => { setEditGroup(null); setModal('create') }}>
          + Gruppe erstellen
        </button>
      </div>

      {error && <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-3 text-red-400 text-sm">{error}</div>}

      {loading ? <div className="text-gray-500 text-sm">Lade…</div> : groups.length === 0 ? (
        <div className="card text-center text-gray-600 py-12">
          Noch keine Gruppen erstellt. Erstelle deine erste Gruppe!
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {groups.map(g => (
            <div key={g.id} className="card space-y-4">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-white">{g.name}</h3>
                  {g.description && <p className="text-gray-500 text-sm mt-0.5">{g.description}</p>}
                </div>
                <div className="flex gap-2 flex-shrink-0">
                  <button className="btn-secondary btn-sm text-xs" onClick={() => { setEditGroup(g); setModal('edit') }}>✏️</button>
                  <button className="btn-danger btn-sm text-xs" onClick={() => deleteGroup(g)}>🗑️</button>
                </div>
              </div>

              {g.permissions.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {g.permissions.map(p => (
                    <span key={p} className="badge badge-blue">{p}</span>
                  ))}
                </div>
              )}

              <div>
                <div className="text-xs text-gray-500 mb-2">Mitglieder ({g.members.length})</div>
                {g.members.length === 0 ? (
                  <div className="text-xs text-gray-600">Keine Mitglieder</div>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {g.members.map(m => (
                      <div key={m.id} className="flex items-center gap-1.5 bg-dark-600 rounded-lg px-2 py-1">
                        <div className="w-5 h-5 rounded-full bg-lemon-500/20 border border-lemon-500/30 flex items-center justify-center text-lemon-400 text-xs font-bold">
                          {m.name.charAt(0).toUpperCase()}
                        </div>
                        <span className="text-xs text-gray-300">{m.name}</span>
                        <button
                          className="text-gray-600 hover:text-red-400 text-xs ml-0.5"
                          onClick={() => removeMember(g.id, m.id)}
                        >✕</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {modal === 'create' && <GroupModal group={null} onClose={() => setModal(null)} onSave={load} />}
      {modal === 'edit' && editGroup && <GroupModal group={editGroup} onClose={() => setModal(null)} onSave={load} />}
    </div>
  )
}

function GroupModal({ group, onClose, onSave }: { group: Group | null; onClose: () => void; onSave: () => void }) {
  const [name, setName] = useState(group?.name ?? '')
  const [description, setDescription] = useState(group?.description ?? '')
  const [memberEmail, setMemberEmail] = useState('')
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit() {
    setErr(''); setLoading(true)
    const body = { name, description }
    const res = await fetch(group ? `/api/groups/${group.id}` : '/api/groups', {
      method: group ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    const data = await res.json(); setLoading(false)
    if (!res.ok) { setErr(data.error); return }
    onSave(); onClose()
  }

  async function addMember() {
    if (!group || !memberEmail.trim()) return
    // look up user by email
    const users = await fetch('/api/users').then(r => r.json())
    const found = users.users?.find((u: { email: string; id: number }) => u.email === memberEmail.trim())
    if (!found) { setErr('Benutzer nicht gefunden.'); return }
    await fetch(`/api/groups/${group.id}`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ addMember: found.id }),
    })
    setMemberEmail('')
    onSave()
  }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-dark-700 border border-dark-500 rounded-xl p-6 w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between mb-5">
          <h3 className="font-semibold text-white">{group ? '✏️ Gruppe bearbeiten' : '🏷️ Neue Gruppe'}</h3>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-300">✕</button>
        </div>
        <div className="space-y-4">
          <div>
            <label className="label">Name</label>
            <input className="input" value={name} onChange={e => setName(e.target.value)} placeholder="z.B. Support Team" />
          </div>
          <div>
            <label className="label">Beschreibung</label>
            <input className="input" value={description} onChange={e => setDescription(e.target.value)} placeholder="Optional" />
          </div>

          {group && (
            <div>
              <label className="label">Mitglied hinzufügen (E-Mail)</label>
              <div className="flex gap-2">
                <input className="input" value={memberEmail} onChange={e => setMemberEmail(e.target.value)} placeholder="name@lemonpvp.de" />
                <button className="btn-secondary btn-sm whitespace-nowrap" onClick={addMember}>+ Hinzufügen</button>
              </div>
            </div>
          )}

          {err && <div className="text-red-400 text-sm">{err}</div>}
          <div className="flex gap-2 justify-end">
            <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
            <button className="btn-primary btn-sm" onClick={submit} disabled={loading}>
              {loading ? '…' : group ? 'Speichern' : 'Erstellen'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
