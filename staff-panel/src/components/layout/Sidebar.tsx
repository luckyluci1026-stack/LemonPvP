'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import type { SessionUser } from '@/lib/auth'
import { clsx } from 'clsx'

const NAV = [
  { href: '/dashboard', icon: '📊', label: 'Dashboard',      perm: null },
  { href: '/players',   icon: '⚔️',  label: 'Spieler',        perm: 'player.view' },
  { href: '/console',   icon: '💻',  label: 'Konsole',         perm: 'admin.console' },
  { href: '/emails',    icon: '📧',  label: 'E-Mails',         perm: 'admin.emails' },
  { href: '/users',     icon: '👥',  label: 'Staff Accounts',  perm: 'admin.users' },
  { href: '/groups',    icon: '🏷️',  label: 'Gruppen',         perm: 'admin.groups' },
  { href: '/audit',     icon: '📋',  label: 'Audit-Log',       perm: null, adminOnly: true },
  { href: '/tutorial',  icon: '📖',  label: 'Tutorial',        perm: null },
  { href: '/settings',  icon: '⚙️',  label: 'Einstellungen',   perm: null, superAdminOnly: true },
]

function hasAccess(session: SessionUser, perm: string | null, adminOnly?: boolean, superAdminOnly?: boolean) {
  if (superAdminOnly && session.role !== 'SUPER_ADMIN') return false
  if (adminOnly && session.role !== 'SUPER_ADMIN' && session.role !== 'ADMIN') return false
  if (!perm) return true
  if (session.role === 'SUPER_ADMIN') return true
  return session.permissions.includes('*') || session.permissions.includes(perm)
}

export default function Sidebar({ session }: { session: SessionUser }) {
  const pathname = usePathname()

  return (
    <aside className="w-60 flex-shrink-0 bg-dark-800 border-r border-dark-600 flex flex-col">
      {/* Logo */}
      <div className="px-4 py-5 border-b border-dark-600">
        <div className="flex items-center gap-3">
          <span className="text-2xl">🍋</span>
          <div>
            <div className="font-bold text-white text-sm">LemonPvP</div>
            <div className="text-xs text-gray-500">Staff Panel</div>
          </div>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {NAV.filter(n => hasAccess(session, n.perm ?? null, n.adminOnly, n.superAdminOnly)).map(item => (
          <Link
            key={item.href}
            href={item.href}
            className={clsx(
              'sidebar-link',
              pathname === item.href && 'active'
            )}
          >
            <span className="text-base leading-none">{item.icon}</span>
            {item.label}
          </Link>
        ))}
      </nav>

      {/* User info */}
      <div className="px-4 py-4 border-t border-dark-600">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-lemon-500/20 border border-lemon-500/30 flex items-center justify-center text-lemon-400 font-bold text-sm flex-shrink-0">
            {session.name.charAt(0).toUpperCase()}
          </div>
          <div className="min-w-0">
            <div className="text-sm font-medium text-gray-200 truncate">{session.name}</div>
            <div className="text-xs text-gray-500 truncate">{session.role === 'SUPER_ADMIN' ? '👑 Super Admin' : session.role === 'ADMIN' ? '🔑 Admin' : '🎯 Staff'}</div>
          </div>
        </div>
      </div>
    </aside>
  )
}
