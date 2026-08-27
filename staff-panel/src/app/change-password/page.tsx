'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

export default function ChangePasswordPage() {
  const router = useRouter()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (password.length < 8) { setError('Passwort muss mindestens 8 Zeichen haben.'); return }
    if (password !== confirm) { setError('Passwörter stimmen nicht überein.'); return }
    setError('')
    setLoading(true)
    try {
      const res = await fetch('/api/auth/change-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
      })
      const data = await res.json()
      if (!res.ok) { setError(data.error); return }
      router.push('/dashboard')
    } catch {
      setError('Verbindungsfehler. Bitte erneut versuchen.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-900 px-4">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-lemon-500/5 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-lemon-500/5 rounded-full blur-3xl" />
      </div>

      <div className="w-full max-w-md relative z-10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-lemon-500/10 border border-lemon-500/30 mb-4">
            <span className="text-3xl">🔐</span>
          </div>
          <h1 className="text-2xl font-bold text-white">Passwort setzen</h1>
          <p className="text-gray-500 text-sm mt-1 max-w-xs mx-auto">
            Dein Account erfordert ein neues Passwort, bevor du fortfahren kannst.
          </p>
        </div>

        <div className="card">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="label">Neues Passwort</label>
              <input
                type="password"
                className="input"
                placeholder="Mindestens 8 Zeichen"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                autoComplete="new-password"
                autoFocus
              />
              {password.length > 0 && password.length < 8 && (
                <p className="text-xs text-red-400 mt-1">Noch {8 - password.length} Zeichen</p>
              )}
              {password.length >= 8 && (
                <p className="text-xs text-emerald-400 mt-1">✓ Länge OK</p>
              )}
            </div>
            <div>
              <label className="label">Passwort bestätigen</label>
              <input
                type="password"
                className="input"
                placeholder="••••••••"
                value={confirm}
                onChange={e => setConfirm(e.target.value)}
                required
                autoComplete="new-password"
              />
              {confirm.length > 0 && confirm !== password && (
                <p className="text-xs text-red-400 mt-1">Passwörter stimmen nicht überein</p>
              )}
              {confirm.length > 0 && confirm === password && password.length >= 8 && (
                <p className="text-xs text-emerald-400 mt-1">✓ Passwörter stimmen überein</p>
              )}
            </div>

            {error && (
              <div className="bg-red-500/10 border border-red-500/30 rounded-lg px-4 py-3 text-red-400 text-sm">
                {error}
              </div>
            )}

            <button
              type="submit"
              className="btn-primary w-full py-2.5"
              disabled={loading || password.length < 8 || password !== confirm}
            >
              {loading ? 'Speichert…' : '🔐 Passwort setzen & weiter'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
