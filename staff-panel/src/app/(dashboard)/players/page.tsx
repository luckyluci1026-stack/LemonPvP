'use client'

import { useState } from 'react'
import { useToast } from '@/components/ui/Toast'

type Player = {
  uuid: string; name: string; online: boolean; coins: number
  elo: Record<string, number>; rank: string; banned: boolean
  ban_reason?: string; ban_expires?: string | null
}

type Modal = 'ban' | 'unban' | 'coins' | 'rank' | null

export default function PlayersPage() {
  const { showToast } = useToast()
  const [search, setSearch] = useState('')
  const [player, setPlayer] = useState<Player | null>(null)
  const [loading, setLoading] = useState(false)
  const [modal, setModal] = useState<Modal>(null)

  async function fetchPlayer() {
    if (!search.trim()) return
    setLoading(true); setPlayer(null)
    try {
      const res = await fetch(`/api/minecraft/player/${encodeURIComponent(search.trim())}`)
      const data = await res.json()
      if (!res.ok) { showToast(data.error, 'error'); return }
      setPlayer(data.player)
    } finally { setLoading(false) }
  }

  async function doAction(action: string, body: Record<string, unknown>) {
    if (!player) return
    const res = await fetch(`/api/minecraft/player/${encodeURIComponent(player.name)}?action=${action}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
    })
    const data = await res.json()
    if (!res.ok) { showToast(data.error, 'error'); return }
    showToast('Aktion erfolgreich ausgeführt!')
    setModal(null)
    fetchPlayer()
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Spieler verwalten</h1>
        <p className="text-gray-500 text-sm mt-1">Spieler suchen und verwalten</p>
      </div>

      {/* Search */}
      <div className="card">
        <div className="flex gap-3">
          <input
            className="input"
            placeholder="Spielername eingeben…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && fetchPlayer()}
          />
          <button className="btn-primary whitespace-nowrap" onClick={fetchPlayer} disabled={loading}>
            {loading ? '…' : '🔍 Suchen'}
          </button>
        </div>
      </div>

      {/* Player card */}
      {player && (
        <div className="card space-y-5">
          <div className="flex items-start justify-between gap-4 flex-wrap">
            <div>
              <div className="flex items-center gap-3 flex-wrap">
                <h2 className="text-xl font-bold text-white">{player.name}</h2>
                <span className={`badge ${player.online ? 'badge-green' : 'badge-gray'}`}>
                  {player.online ? '● Online' : '○ Offline'}
                </span>
                {player.banned && <span className="badge badge-red">🔨 Gebannt</span>}
              </div>
              <div className="text-xs text-gray-500 mt-1 font-mono">{player.uuid}</div>
            </div>
            <div className="flex gap-2 flex-wrap">
              <button className="btn-secondary btn-sm" onClick={() => setModal('coins')}>💰 Coins</button>
              <button className="btn-secondary btn-sm" onClick={() => setModal('rank')}>🏆 Rang</button>
              {player.banned
                ? <button className="btn-secondary btn-sm" onClick={() => setModal('unban')}>✅ Entbannen</button>
                : <button className="btn-danger btn-sm" onClick={() => setModal('ban')}>🔨 Bannen</button>
              }
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <InfoCard label="Rang" value={player.rank || '–'} icon="🏆" />
            <InfoCard label="Coins" value={player.coins.toLocaleString('de-DE')} icon="💰" />
            <InfoCard
              label="Ban Status"
              value={player.banned ? player.ban_reason || 'Gebannt' : 'Sauber'}
              icon={player.banned ? '🔨' : '✅'}
            />
          </div>

          {Object.keys(player.elo).length > 0 && (
            <div>
              <div className="label mb-2">ELO</div>
              <div className="flex gap-2 flex-wrap">
                {Object.entries(player.elo).map(([mode, elo]) => (
                  <div key={mode} className="bg-dark-600 rounded-lg px-3 py-1.5 text-sm">
                    <span className="text-gray-400">{mode}: </span>
                    <span className="text-white font-semibold">{elo}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {player.banned && player.ban_reason && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-3">
              <div className="text-red-400 text-sm font-medium">Ban-Grund: {player.ban_reason}</div>
              {player.ban_expires && <div className="text-red-400/60 text-xs mt-1">Läuft ab: {player.ban_expires}</div>}
            </div>
          )}
        </div>
      )}

      {/* Modals */}
      {modal === 'ban' && (
        <BanModal onClose={() => setModal(null)} onConfirm={(reason, dur) => doAction('ban', { reason, duration: dur })} />
      )}
      {modal === 'unban' && (
        <ConfirmModal
          title="Spieler entbannen"
          msg={`Möchtest du ${player?.name} wirklich entbannen?`}
          onClose={() => setModal(null)}
          onConfirm={() => doAction('unban', {})}
        />
      )}
      {modal === 'coins' && (
        <CoinsModal onClose={() => setModal(null)} onConfirm={(amount, action) => doAction('coins', { amount, action })} />
      )}
      {modal === 'rank' && (
        <RankModal onClose={() => setModal(null)} onConfirm={rank => doAction('rank', { rank })} />
      )}
    </div>
  )
}

function InfoCard({ label, value, icon }: { label: string; value: string; icon: string }) {
  return (
    <div className="bg-dark-600 rounded-lg p-3">
      <div className="text-xs text-gray-500 mb-1">{icon} {label}</div>
      <div className="text-white font-semibold">{value}</div>
    </div>
  )
}

function BanModal({ onClose, onConfirm }: { onClose: () => void; onConfirm: (r: string, d: string) => void }) {
  const [reason, setReason] = useState('')
  const [duration, setDuration] = useState('permanent')
  return (
    <ModalWrapper title="🔨 Spieler bannen" onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="label">Grund</label>
          <input className="input" value={reason} onChange={e => setReason(e.target.value)} placeholder="z.B. Cheating" />
        </div>
        <div>
          <label className="label">Dauer</label>
          <select className="input" value={duration} onChange={e => setDuration(e.target.value)}>
            <option value="permanent">Permanent</option>
            <option value="1d">1 Tag</option>
            <option value="3d">3 Tage</option>
            <option value="7d">7 Tage</option>
            <option value="14d">14 Tage</option>
            <option value="30d">30 Tage</option>
          </select>
        </div>
        <div className="flex gap-2 justify-end">
          <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
          <button className="btn-danger btn-sm" onClick={() => onConfirm(reason || 'Kein Grund', duration)}>Bannen</button>
        </div>
      </div>
    </ModalWrapper>
  )
}

function CoinsModal({ onClose, onConfirm }: { onClose: () => void; onConfirm: (amount: number, action: string) => void }) {
  const [amount, setAmount] = useState('')
  const [action, setAction] = useState('add')
  return (
    <ModalWrapper title="💰 Coins verwalten" onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="label">Aktion</label>
          <select className="input" value={action} onChange={e => setAction(e.target.value)}>
            <option value="add">Hinzufügen</option>
            <option value="remove">Entfernen</option>
            <option value="set">Setzen auf</option>
          </select>
        </div>
        <div>
          <label className="label">Betrag</label>
          <input className="input" type="number" value={amount} onChange={e => setAmount(e.target.value)} placeholder="1000" />
        </div>
        <div className="flex gap-2 justify-end">
          <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
          <button className="btn-primary btn-sm" onClick={() => onConfirm(Number(amount), action)}>Bestätigen</button>
        </div>
      </div>
    </ModalWrapper>
  )
}

function RankModal({ onClose, onConfirm }: { onClose: () => void; onConfirm: (rank: string) => void }) {
  const [rank, setRank] = useState('')
  const RANKS = ['default', 'vip', 'vip+', 'mvp', 'mvp+', 'youtuber', 'helper', 'mod', 'srmod', 'admin', 'owner']
  return (
    <ModalWrapper title="🏆 Rang setzen" onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className="label">Rang</label>
          <select className="input" value={rank} onChange={e => setRank(e.target.value)}>
            <option value="">Rang wählen…</option>
            {RANKS.map(r => <option key={r} value={r}>{r}</option>)}
          </select>
        </div>
        <div className="flex gap-2 justify-end">
          <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
          <button className="btn-primary btn-sm" onClick={() => rank && onConfirm(rank)} disabled={!rank}>Setzen</button>
        </div>
      </div>
    </ModalWrapper>
  )
}

function ConfirmModal({ title, msg, onClose, onConfirm }: {
  title: string; msg: string; onClose: () => void; onConfirm: () => void
}) {
  return (
    <ModalWrapper title={title} onClose={onClose}>
      <p className="text-gray-300 text-sm mb-4">{msg}</p>
      <div className="flex gap-2 justify-end">
        <button className="btn-secondary btn-sm" onClick={onClose}>Abbrechen</button>
        <button className="btn-primary btn-sm" onClick={onConfirm}>Bestätigen</button>
      </div>
    </ModalWrapper>
  )
}

function ModalWrapper({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-dark-700 border border-dark-500 rounded-xl p-6 w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-white">{title}</h3>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-300 text-lg">✕</button>
        </div>
        {children}
      </div>
    </div>
  )
}
