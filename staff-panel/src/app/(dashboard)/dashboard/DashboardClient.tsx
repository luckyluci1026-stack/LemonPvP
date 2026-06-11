'use client'

import { useEffect, useState, useCallback } from 'react'
import type { SessionUser } from '@/lib/auth'
import type { AuditLog } from '@/lib/db'

type Stats = {
  online: number; max: number; tps: number; uptime_seconds: number; version: string
} | null

const ACTION_LABELS: Record<string, string> = {
  LOGIN: '🔐 Login',
  LOGIN_FAIL: '❌ Login fehlgeschlagen',
  CREATE_USER: '👤 Account erstellt',
  UPDATE_USER: '✏️ Account bearbeitet',
  DELETE_USER: '🗑️ Account gelöscht',
  SUSPEND_USER: '🚫 Account gesperrt',
  UNSUSPEND_USER: '✅ Account entsperrt',
  CREATE_GROUP: '🏷️ Gruppe erstellt',
  UPDATE_GROUP: '✏️ Gruppe bearbeitet',
  DELETE_GROUP: '🗑️ Gruppe gelöscht',
  CREATE_MAILBOX: '📧 Mailbox erstellt',
  UPDATE_MAILBOX: '✏️ Mailbox bearbeitet',
  DELETE_MAILBOX: '🗑️ Mailbox gelöscht',
  CREATE_ALIAS: '📨 Alias erstellt',
  DELETE_ALIAS: '🗑️ Alias gelöscht',
  PLAYER_BAN: '🔨 Spieler gebannt',
  PLAYER_UNBAN: '✅ Spieler entbannt',
  PLAYER_MUTE: '🔇 Spieler stummgeschaltet',
  PLAYER_UNMUTE: '🔊 Spieler entstummt',
  PLAYER_COINS: '💰 Coins geändert',
  PLAYER_RANK: '🏆 Rang gesetzt',
  CONSOLE_CMD: '💻 Konsolenbefehl',
}

function formatUptime(s: number) {
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  return `${h}h ${m}m`
}

export default function DashboardClient({
  session, userCount, recentLogs
}: {
  session: SessionUser
  userCount: number
  recentLogs: AuditLog[]
}) {
  const [stats, setStats] = useState<Stats>(null)
  const [statsError, setStatsError] = useState(false)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)

  const fetchStats = useCallback(() => {
    fetch('/api/minecraft/stats')
      .then(r => r.json())
      .then(d => {
        if (d.stats) { setStats(d.stats); setStatsError(false); setLastUpdated(new Date()) }
        else setStatsError(true)
      })
      .catch(() => setStatsError(true))
  }, [])

  useEffect(() => {
    fetchStats()
    const interval = setInterval(fetchStats, 30_000)
    return () => clearInterval(interval)
  }, [fetchStats])

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between flex-wrap gap-2">
        <div>
          <h1 className="text-2xl font-bold text-white">Dashboard</h1>
          <p className="text-gray-500 text-sm mt-1">Übersicht aller wichtigen Informationen</p>
        </div>
        <div className="flex items-center gap-2">
          {lastUpdated && (
            <span className="text-xs text-gray-600">
              Aktualisiert {lastUpdated.toLocaleTimeString('de-DE')}
            </span>
          )}
          <button
            className="btn-secondary btn-sm text-xs"
            onClick={fetchStats}
          >
            ↻ Aktualisieren
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon="⚔️"
          label="Online Spieler"
          value={statsError ? '–' : stats ? `${stats.online} / ${stats.max}` : '…'}
          sub={stats ? `TPS: ${stats.tps.toFixed(1)}` : ''}
          color="green"
        />
        <StatCard
          icon="🕐"
          label="Server Uptime"
          value={stats ? formatUptime(stats.uptime_seconds) : statsError ? '–' : '…'}
          sub={stats?.version ?? ''}
          color="blue"
        />
        <StatCard
          icon="👥"
          label="Staff Accounts"
          value={String(userCount)}
          sub="Gesamt im Panel"
          color="yellow"
        />
        <StatCard
          icon="📋"
          label="Letzte Aktion"
          value={recentLogs[0] ? ACTION_LABELS[recentLogs[0].action]?.split(' ').slice(1).join(' ') ?? recentLogs[0].action : '–'}
          sub={recentLogs[0]?.user_email ?? ''}
          color="purple"
        />
      </div>

      {/* Recent Activity */}
      <div className="card">
        <h2 className="text-lg font-semibold text-white mb-4">Letzte Aktivitäten</h2>
        {recentLogs.length === 0 ? (
          <p className="text-gray-600 text-sm">Noch keine Aktivitäten.</p>
        ) : (
          <div className="space-y-2">
            {recentLogs.map(log => (
              <div key={log.id} className="flex items-start gap-3 py-2 border-b border-dark-600 last:border-0">
                <span className="text-base leading-none mt-0.5">{ACTION_LABELS[log.action]?.split(' ')[0] ?? '📌'}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-gray-200 text-sm font-medium">
                      {ACTION_LABELS[log.action]?.split(' ').slice(1).join(' ') ?? log.action}
                    </span>
                    {log.target && (
                      <span className="badge badge-gray">{log.target}</span>
                    )}
                  </div>
                  <div className="text-xs text-gray-600 mt-0.5 flex gap-2">
                    <span>{log.user_email ?? 'System'}</span>
                    <span>·</span>
                    <span>{new Date(log.created_at).toLocaleString('de-DE')}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, sub, color }: {
  icon: string; label: string; value: string; sub: string
  color: 'green' | 'blue' | 'yellow' | 'purple'
}) {
  const colors = {
    green:  'bg-emerald-500/10 border-emerald-500/20 text-emerald-400',
    blue:   'bg-blue-500/10 border-blue-500/20 text-blue-400',
    yellow: 'bg-lemon-500/10 border-lemon-500/20 text-lemon-400',
    purple: 'bg-purple-500/10 border-purple-500/20 text-purple-400',
  }
  return (
    <div className={`card ${colors[color]} border`}>
      <div className="flex items-start justify-between mb-2">
        <span className="text-2xl">{icon}</span>
      </div>
      <div className="text-2xl font-bold text-white mt-1">{value}</div>
      <div className="text-xs font-medium mt-1">{label}</div>
      {sub && <div className="text-xs opacity-60 mt-0.5">{sub}</div>}
    </div>
  )
}
