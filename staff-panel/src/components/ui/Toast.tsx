'use client'

import { createContext, useCallback, useContext, useRef, useState } from 'react'

type ToastType = 'success' | 'error' | 'info'
type Toast = { id: number; type: ToastType; message: string }

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void
}

const ToastContext = createContext<ToastContextValue>({ showToast: () => {} })

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const counter = useRef(0)

  const showToast = useCallback((message: string, type: ToastType = 'success') => {
    const id = ++counter.current
    setToasts(t => [...t, { id, type, message }])
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 4000)
  }, [])

  const dismiss = (id: number) => setToasts(t => t.filter(x => x.id !== id))

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 pointer-events-none">
        {toasts.map(toast => (
          <div
            key={toast.id}
            onClick={() => dismiss(toast.id)}
            className={`
              pointer-events-auto flex items-start gap-3 px-4 py-3 rounded-xl shadow-lg
              border text-sm font-medium max-w-sm animate-slide-in cursor-pointer
              ${toast.type === 'success' ? 'bg-emerald-900/90 border-emerald-500/40 text-emerald-100' : ''}
              ${toast.type === 'error'   ? 'bg-red-900/90 border-red-500/40 text-red-100' : ''}
              ${toast.type === 'info'    ? 'bg-dark-700 border-dark-400 text-gray-200' : ''}
            `}
          >
            <span className="text-base leading-none mt-px">
              {toast.type === 'success' ? '✅' : toast.type === 'error' ? '❌' : 'ℹ️'}
            </span>
            <span className="flex-1">{toast.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
