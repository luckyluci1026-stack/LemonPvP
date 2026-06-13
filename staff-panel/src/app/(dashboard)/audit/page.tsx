'use client'

import { useEffect, useState, useCallback } from 'react'
import type { AuditLog } from '@/lib/db'

const ACTION_LABELS: Record<string, { icon: string; label: string }> = {
  LOGIN:           { icon: '🔐', label: 'Login' },
  LOGIN_FAIL:      { icon: '❌', label: 'Login fehlgeschlagen' },
  CHANGE_PASSWORD: { icon: '🔑', label: 'Passwort geändert' },
  CREATE_USER:     { icon: '👤', label: 'Account erstellt' },
  UPDATE_USER:     { icon: '✏️', label: 'Account bearbeitet' },
  DELETE_USER:     { icon: '🗑️', label: 'Account gelöscht' },
  SUSPEND_USER:    { icon: '🚫', label: 'Account gesperrt' },
  UNSUSPEND_USER:  { icon: '✅', label: 'Account entsperrt' },
  CREATE_GROUP:    { icon: '🏷️', label: 'Gruppe erstellt' },
  UPDATE_GROUP:    { icon: '✏️', label: 'Gruppe bearbeitet' },
  DELETE_GROUP:    { icon: '🗑️', label: 'Gruppe gelöscht' },
  CREATE_MAILBOX:  { icon: '📧', label: 'Mailbox erstellt' },
  UPDATE_MAILBOX:  { icon: '✏️', label: 'Mailbox bearbeitet' },
  DELETE_MAILBOX:  { icon: '🗑️', label: 'Mailbox gelöscht' },
  CREATE_ALIAS:    { icon: '📨', label: 'Alias erstellt' },
  DELETE_ALIAS:    { icon: '🗑️', label: 'Alias gelöscht' },
  PLAYER_BAN:      { icon: '🔨', label: 'Spieler gebannt' },
  PLAYER_UNBAN:    { icon: '✅', label: 'Spieler entbannt' },
  PLAYER_MUTE:     { icon: '🔇', label: 'Spieler stummgeschaltet' },
  PLAYER_UNMUTE:   { icon: '🔊', label: 'Spieler entstummt' },
  PLAYER_COINS:    { icon: '💰', label: 'Coins geändert' },
  PLAYER_RANK:     { icon: '🏆', label: 'Rang gesetzt' },
  CONSOLE_CMD:     { icon: '💻', label: 'Konsolenbefehl' },
  UPDATE_SETTINGS: { icon: '⚙️', label: 'Einstellungen geändert' },
}

const PAGE_SIZE = 50

export default function AuditPage() {
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [filtered, setFiltered] = useState<AuditLog[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [actionFilter, setActionFilter] = useState('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    fetch('/api/audit?limit=1000')
      .then(r => r.json())
      .then(d => {
        if (d.error) { setError(d.error); return }
        setLogs(d.logs ?? [])
      })
      .catch(() => setError('Fehler beim Laden der Logs.'))
      .finally(() => setLoading(false))
  }, [])

  const applyFilters = useCallback(() => {
    let result = logs
    if (actionFilter) result = result.filter(l => l.action === actionFilter)
    if (search.trim()) {
      const q = search.trim().toLowerCase()
      result = result.filter(l =>
        l.user_email?.toLowerCase().includes(q) ||
        l.target?.toLowerCase().includes(q) ||
        l.details?.toLowerCase().includes(q) ||
        l.action.toLowerCase().includes(q)
      )
    }
    setFiltered(result)
    setPage(0)
  }, [logs, search, actionFilter])

  useEffect(() => { applyFilters() }, [applyFilters])

  const pages = Math.ceil(filtered.length / PAGE_SIZE)
  const slice = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const uniqueActions = Array.from(new Set(logs.map(l => l.action))).sort()

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-white">Audit-Log</h1>
        <p className="text-gray-500 text-sm mt-1">Alle Aktionen im Staff Panel</p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <input
          className="input max-w-xs"
          placeholder="Suchen (User, Ziel, Aktion)…"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <select
          className="input max-w-xs"
          value={actionFilter}
          onChange={e => setActionFilter(e.target.value)}
        >
          <option value="">Alle Aktionen</option>
          {uniqueActions.map(a => (
            <option key={a} value={a}>
              {ACTION_LABELS[a]?.label ?? a}
            </option>
          ))}
        </select>
        <div className="flex items-center text-sm text-gray-500">
          {filtered.length} Einträge
        </div>
      </div>

      {error && <div className="text-red-400 text-sm">{error}</div>}

      {loading ? (
        <div className="text-gray-500 text-sm">Laden…</div>
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Zeit</th>
                  <th>Aktion</th>
                  <th>Staff</th>
                  <th>Ziel</th>
                  <th>Details</th>
                  <th>IP</th>
                </tr>
              </thead>
              <tbody>
                {slice.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center text-gray-600 py-6">
                      Keine Einträge gefunden.
                    </td>
                  </tr>
                ) : slice.map(log => {
                  const meta = ACTION_LABELS[log.action]
                  return (
                    <tr key={log.id}>
                      <td className="text-xs text-gray-500 whitespace-nowrap">
                        {new Date(log.created_at).toLocaleString('de-DE')}
                      </td>
                      <td>
                        <span className="inline-flex items-center gap-1.5">
                          <span>{meta?.icon ?? '📌'}</span>
                          <span className="text-gray-200 text-xs font-medium">
                            {meta?.label ?? log.action}
                          </span>
                        </span>
                      </td>
                      <td className="text-xs text-gray-400">{log.user_email ?? '–'}</td>
                      <td className="text-xs">
                        {log.target ? (
                          <span className="badge badge-gray">{log.target}</span>
                        ) : '–'}
                      </td>
                      <td className="text-xs text-gray-500 max-w-xs truncate" title={log.details ?? ''}>
                        {log.details ?? '–'}
                      </td>
                      <td className="text-xs text-gray-600">{log.ip ?? '–'}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {pages > 1 && (
            <div className="flex items-center gap-2 justify-end">
              <button
                className="btn-secondary btn-sm"
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                ← Zurück
              </button>
              <span className="text-sm text-gray-400">
                Seite {page + 1} von {pages}
              </span>
              <button
                className="btn-secondary btn-sm"
                onClick={() => setPage(p => Math.min(pages - 1, p + 1))}
                disabled={page >= pages - 1}
              >
                Weiter →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
