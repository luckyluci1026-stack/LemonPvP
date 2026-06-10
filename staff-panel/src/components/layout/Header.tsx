'use client'

import { useRouter } from 'next/navigation'
import type { SessionUser } from '@/lib/auth'

export default function Header({ session }: { session: SessionUser }) {
  const router = useRouter()

  async function logout() {
    await fetch('/api/auth/logout', { method: 'POST' })
    router.push('/login')
  }

  return (
    <header className="h-14 bg-dark-800 border-b border-dark-600 flex items-center justify-between px-6 flex-shrink-0">
      <div className="text-gray-400 text-sm">
        Willkommen zurück, <span className="text-gray-200 font-medium">{session.name}</span>
      </div>
      <div className="flex items-center gap-3">
        <span className="text-xs text-gray-600 hidden sm:block">{session.email}</span>
        <button
          onClick={logout}
          className="text-gray-500 hover:text-gray-300 text-sm transition-colors px-3 py-1.5 rounded-lg hover:bg-dark-600"
        >
          Abmelden
        </button>
      </div>
    </header>
  )
}
