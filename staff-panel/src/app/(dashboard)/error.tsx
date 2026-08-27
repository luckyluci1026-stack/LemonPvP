'use client'

import { useEffect } from 'react'

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    console.error('[Dashboard Error]', error)
  }, [error])

  return (
    <div className="flex flex-col items-center justify-center h-full gap-4 text-center">
      <span className="text-5xl">⚠️</span>
      <h2 className="text-xl font-bold text-white">Ein Fehler ist aufgetreten</h2>
      <p className="text-gray-500 text-sm max-w-md">
        {error.message || 'Unbekannter Fehler. Bitte versuche es erneut.'}
      </p>
      <button className="btn-primary" onClick={reset}>
        Erneut versuchen
      </button>
    </div>
  )
}
