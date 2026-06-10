'use client'

import { useState, useRef, useEffect } from 'react'

type LogEntry = { id: number; type: 'input' | 'output' | 'error'; text: string; ts: string }

export default function ConsolePage() {
  const [command, setCommand] = useState('')
  const [logs, setLogs] = useState<LogEntry[]>([
    { id: 0, type: 'output', text: '🍋 LemonPvP Staff Console — Gib einen Befehl ein und drücke Enter.', ts: new Date().toLocaleTimeString('de-DE') },
  ])
  const [loading, setLoading] = useState(false)
  const [history, setHistory] = useState<string[]>([])
  const [histIdx, setHistIdx] = useState(-1)
  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [logs])

  async function send() {
    const cmd = command.trim()
    if (!cmd) return
    setHistory(h => [cmd, ...h.slice(0, 49)])
    setHistIdx(-1)
    setCommand('')
    setLoading(true)

    const ts = new Date().toLocaleTimeString('de-DE')
    const inputEntry: LogEntry = { id: Date.now(), type: 'input', text: `> ${cmd}`, ts }
    setLogs(l => [...l, inputEntry])

    try {
      const res = await fetch('/api/minecraft/console', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: cmd }),
      })
      const data = await res.json()
      const ts2 = new Date().toLocaleTimeString('de-DE')
      if (!res.ok) {
        setLogs(l => [...l, { id: Date.now() + 1, type: 'error', text: data.error, ts: ts2 }])
      } else {
        setLogs(l => [...l, { id: Date.now() + 1, type: 'output', text: data.output || '(kein Output)', ts: ts2 }])
      }
    } catch {
      setLogs(l => [...l, { id: Date.now() + 1, type: 'error', text: 'Verbindungsfehler zum Server.', ts: new Date().toLocaleTimeString('de-DE') }])
    } finally {
      setLoading(false)
    }
  }

  function handleKey(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') { send(); return }
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      const idx = Math.min(histIdx + 1, history.length - 1)
      setHistIdx(idx)
      setCommand(history[idx] ?? '')
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      const idx = Math.max(histIdx - 1, -1)
      setHistIdx(idx)
      setCommand(idx === -1 ? '' : history[idx])
    }
  }

  function clearLogs() {
    setLogs([{ id: Date.now(), type: 'output', text: 'Konsole geleert.', ts: new Date().toLocaleTimeString('de-DE') }])
  }

  return (
    <div className="flex flex-col h-full gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Server Konsole</h1>
          <p className="text-gray-500 text-sm mt-1">Befehle direkt an den Minecraft Server senden</p>
        </div>
        <button className="btn-secondary btn-sm" onClick={clearLogs}>🗑️ Leeren</button>
      </div>

      {/* Terminal */}
      <div
        className="flex-1 bg-dark-900 border border-dark-500 rounded-xl overflow-y-auto p-4 font-mono text-sm min-h-0"
        onClick={() => inputRef.current?.focus()}
      >
        {logs.map(l => (
          <div key={l.id} className={`mb-1 leading-relaxed ${
            l.type === 'input' ? 'text-lemon-400'
            : l.type === 'error' ? 'text-red-400'
            : 'text-gray-300'
          }`}>
            <span className="text-gray-600 mr-2 text-xs select-none">[{l.ts}]</span>
            {l.text}
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="bg-dark-800 border border-dark-500 rounded-xl p-3 flex gap-3">
        <span className="text-lemon-400 font-mono text-sm self-center select-none">❯</span>
        <input
          ref={inputRef}
          className="flex-1 bg-transparent border-none outline-none text-gray-200 font-mono text-sm placeholder:text-gray-700"
          placeholder="Befehl eingeben… (↑↓ für Verlauf)"
          value={command}
          onChange={e => setCommand(e.target.value)}
          onKeyDown={handleKey}
          disabled={loading}
          autoFocus
        />
        <button className="btn-primary btn-sm" onClick={send} disabled={loading || !command.trim()}>
          {loading ? '…' : 'Senden'}
        </button>
      </div>
    </div>
  )
}
