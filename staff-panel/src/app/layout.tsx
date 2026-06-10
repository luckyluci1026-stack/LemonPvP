import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'LemonPvP Staff Panel',
  description: 'LemonPvP Staff Administration Panel',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="de">
      <body>{children}</body>
    </html>
  )
}
