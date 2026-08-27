'use client'

import { useState, useEffect } from 'react'
import { useToast } from '@/components/ui/Toast'

export default function SettingsPage() {
  const { showToast } = useToast()
  const [form, setForm] = useState({
    mailcow_url: '', mailcow_key: '', mc_api_url: '', mc_api_key: '',
    domain: '', panel_name: '',
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    fetch('/api/settings')
      .then(r => r.json())
      .then(d => {
        if (d.settings) {
          setForm({
            mailcow_url: d.settings.mailcow_url ?? '',
            mailcow_key: '',  // never pre-fill masked key
            mc_api_url:  d.settings.mc_api_url ?? '',
            mc_api_key:  '',
            domain:      d.settings.domain ?? '',
            panel_name:  d.settings.panel_name ?? '',
          })
        }
        setLoading(false)
      })
  }, [])

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  async function save() {
    setSaving(true)
    // Only send non-empty key fields
    const body: Record<string, string> = {
      mailcow_url: form.mailcow_url,
      mc_api_url:  form.mc_api_url,
      domain:      form.domain,
      panel_name:  form.panel_name,
    }
    if (form.mailcow_key) body.mailcow_key = form.mailcow_key
    if (form.mc_api_key)  body.mc_api_key  = form.mc_api_key

    const res = await fetch('/api/settings', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    setSaving(false)
    if (res.ok) showToast('Einstellungen gespeichert!')
    else { const d = await res.json(); showToast(d.error ?? 'Fehler beim Speichern.', 'error') }
  }

  if (loading) return <div className="text-gray-500 text-sm">Lade…</div>

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Einstellungen</h1>
        <p className="text-gray-500 text-sm mt-1">Panel-Konfiguration und API-Verbindungen</p>
      </div>

      {/* General */}
      <Section title="⚙️ Allgemein">
        <Field label="Panel Name">
          <input className="input" value={form.panel_name} onChange={set('panel_name')} />
        </Field>
        <Field label="Standard Domain">
          <input className="input" value={form.domain} onChange={set('domain')} placeholder="lemonpvp.de" />
        </Field>
      </Section>

      {/* Mailcow */}
      <Section title="📧 Mailcow API" sub="Für E-Mail Verwaltung. Mailcow Admin → API → Create API Key">
        <Field label="Mailcow URL">
          <input className="input" value={form.mailcow_url} onChange={set('mailcow_url')} placeholder="https://mail.lemonpvp.de" />
        </Field>
        <Field label="API Key (leer lassen um alten zu behalten)">
          <input className="input" type="password" value={form.mailcow_key} onChange={set('mailcow_key')} placeholder="••••••••" />
        </Field>
      </Section>

      {/* Minecraft */}
      <Section title="⛏️ Minecraft API" sub="LemonCore Plugin HTTP API (Port 8080)">
        <Field label="API URL">
          <input className="input" value={form.mc_api_url} onChange={set('mc_api_url')} placeholder="http://localhost:8080" />
        </Field>
        <Field label="API Key (leer lassen um alten zu behalten)">
          <input className="input" type="password" value={form.mc_api_key} onChange={set('mc_api_key')} placeholder="••••••••" />
        </Field>
        <div className="text-xs text-gray-500 bg-dark-800 rounded-lg p-3 border border-dark-500">
          <div className="font-medium text-gray-400 mb-1">Plugin Konfiguration (config.yml):</div>
          <pre className="font-mono text-gray-500">{`http-api:
  enabled: true
  port: 8080
  key: YOUR_API_KEY_HERE`}</pre>
        </div>
      </Section>

      <button className="btn-primary" onClick={save} disabled={saving}>
        {saving ? 'Speichert…' : '💾 Speichern'}
      </button>
    </div>
  )
}

function Section({ title, sub, children }: { title: string; sub?: string; children: React.ReactNode }) {
  return (
    <div className="card space-y-4">
      <div>
        <h2 className="font-semibold text-white">{title}</h2>
        {sub && <p className="text-xs text-gray-500 mt-0.5">{sub}</p>}
      </div>
      {children}
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="label">{label}</label>
      {children}
    </div>
  )
}
