import { getSession, getUserById } from '@/lib/auth'
import { redirect } from 'next/navigation'
import Sidebar from '@/components/layout/Sidebar'
import Header from '@/components/layout/Header'

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession()
  if (!session) redirect('/login')

  // Re-check DB so suspension takes effect immediately without waiting for JWT expiry
  const dbUser = getUserById(session.id)
  if (!dbUser || dbUser.suspended) redirect('/api/auth/force-logout')

  return (
    <div className="flex h-screen overflow-hidden bg-dark-900">
      <Sidebar session={session} />
      <div className="flex flex-col flex-1 overflow-hidden">
        <Header session={session} />
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}
