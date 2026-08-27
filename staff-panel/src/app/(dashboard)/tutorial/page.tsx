export default function TutorialPage() {
  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">📖 Tutorial für neue Staff-Mitglieder</h1>
        <p className="text-gray-500 text-sm mt-1">Alles was du wissen musst, um das Staff Panel zu benutzen</p>
      </div>

      <Step number={1} title="Anmeldung">
        <p>Melde dich mit deiner Staff-E-Mail und dem Passwort an, das dir ein Admin gegeben hat. Bei Problemen wende dich an einen Admin über Discord oder direkt ingame.</p>
        <Tip>Dein Account wurde von einem Admin erstellt. Du kannst dein Passwort NICHT selbst zurücksetzen — bitte einen Admin.</Tip>
      </Step>

      <Step number={2} title="Dashboard – Übersicht">
        <p>Das Dashboard zeigt dir auf einen Blick:</p>
        <ul className="list-disc list-inside text-gray-400 text-sm space-y-1 mt-2">
          <li><strong className="text-gray-200">Online Spieler</strong> — Wie viele Spieler gerade auf dem Server sind und die aktuelle TPS</li>
          <li><strong className="text-gray-200">Server Uptime</strong> — Wie lange der Server bereits läuft</li>
          <li><strong className="text-gray-200">Staff Accounts</strong> — Anzahl der registrierten Staff-Mitglieder</li>
          <li><strong className="text-gray-200">Letzte Aktivitäten</strong> — Alle Aktionen der letzten Zeit (wer was gemacht hat)</li>
        </ul>
      </Step>

      <Step number={3} title="Spieler verwalten">
        <p>Unter <strong className="text-gray-200">⚔️ Spieler</strong> kannst du nach Spielern suchen und verschiedene Aktionen durchführen:</p>
        <div className="mt-3 space-y-2">
          <ActionItem icon="🔨" label="Bannen" desc="Spieler temporär oder permanent bannen. Gib immer einen Grund an!" />
          <ActionItem icon="✅" label="Entbannen" desc="Gebannte Spieler wieder freischalten." />
          <ActionItem icon="🔇" label="Stummschalten" desc="Spieler temporär oder permanent stumm schalten. Wählbare Dauer: 1h, 6h, 1d, 3d, 7d, 30d oder permanent." />
          <ActionItem icon="🔊" label="Entstummen" desc="Stummgeschaltete Spieler wieder zum Schreiben freischalten." />
          <ActionItem icon="💰" label="Coins" desc="Coins hinzufügen, abziehen oder auf einen Wert setzen." />
          <ActionItem icon="🏆" label="Rang" desc="Einem Spieler einen Rang zuweisen (z.B. VIP, MVP)." />
        </div>
        <Tip>Nutze deine Rechte verantwortungsvoll. Jede Aktion wird geloggt und ist für Admins sichtbar!</Tip>
      </Step>

      <Step number={4} title="Konsole (nur für Admins)">
        <p>Die <strong className="text-gray-200">💻 Konsole</strong> erlaubt es dir, direkte Minecraft-Befehle auszuführen. Nur Accounts mit der Berechtigung <code className="bg-dark-600 px-1 rounded text-lemon-400 text-xs">admin.console</code> haben Zugriff.</p>
        <ul className="list-disc list-inside text-gray-400 text-sm space-y-1 mt-2">
          <li>Tippe einen Befehl ein und drücke <kbd className="bg-dark-600 px-1.5 py-0.5 rounded text-xs">Enter</kbd></li>
          <li>Mit <kbd className="bg-dark-600 px-1.5 py-0.5 rounded text-xs">↑</kbd> / <kbd className="bg-dark-600 px-1.5 py-0.5 rounded text-xs">↓</kbd> kannst du die Befehlshistorie durchblättern</li>
          <li>Beispiele: <code className="bg-dark-600 px-1 rounded text-lemon-400 text-xs">list</code>, <code className="bg-dark-600 px-1 rounded text-lemon-400 text-xs">say Hallo!</code>, <code className="bg-dark-600 px-1 rounded text-lemon-400 text-xs">tp Spieler 0 100 0</code></li>
        </ul>
      </Step>

      <Step number={5} title="E-Mails verwalten (nur für Berechtigte)">
        <p>Unter <strong className="text-gray-200">📧 E-Mails</strong> kannst du Mailboxen und Aliases für <code className="bg-dark-600 px-1 rounded text-lemon-400 text-xs">@lemonpvp.de</code> verwalten.</p>
        <div className="mt-3 space-y-2">
          <ActionItem icon="📬" label="Mailbox" desc="Eine richtige E-Mail-Adresse mit Postfach (z.B. support@lemonpvp.de)" />
          <ActionItem icon="↩️" label="Alias" desc="Eine Weiterleitungsadresse, die E-Mails an eine andere Adresse schickt" />
        </div>
        <Tip>Mailcow muss erst in den Einstellungen konfiguriert werden. Frag einen Super-Admin wenn es nicht funktioniert.</Tip>
      </Step>

      <Step number={6} title="Berechtigungen verstehen">
        <p>Jeder Account hat bestimmte Berechtigungen. Admins können diese vergeben:</p>
        <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-2">
          {[
            { perm: 'player.view', desc: 'Spieler-Info ansehen' },
            { perm: 'player.ban', desc: 'Spieler bannen' },
            { perm: 'player.unban', desc: 'Spieler entbannen' },
            { perm: 'player.mute', desc: 'Spieler stumm schalten / entstummen' },
            { perm: 'player.coins', desc: 'Coins vergeben/entfernen' },
            { perm: 'player.rank', desc: 'Ränge setzen' },
            { perm: 'admin.emails', desc: 'E-Mails verwalten' },
            { perm: 'admin.console', desc: 'Konsole nutzen' },
            { perm: 'admin.users', desc: 'Staff-Accounts verwalten' },
            { perm: 'admin.groups', desc: 'Gruppen verwalten' },
          ].map(p => (
            <div key={p.perm} className="flex items-center gap-2 text-sm">
              <code className="badge badge-blue font-mono text-xs">{p.perm}</code>
              <span className="text-gray-400">{p.desc}</span>
            </div>
          ))}
        </div>
      </Step>

      <Step number={7} title="Regeln für Staff-Mitglieder">
        <div className="space-y-2">
          {[
            '🔒 Teile dein Passwort niemals mit anderen Personen.',
            '📝 Gib bei Bans immer einen klaren Grund an.',
            '⚖️ Nutze deine Rechte fair und nach den Server-Regeln.',
            '🔍 Alle Aktionen werden geloggt — sei transparent.',
            '💬 Bei Unsicherheiten frag immer einen Admin.',
            '🚫 Missbrauch von Rechten führt sofort zur Sperrung.',
          ].map((rule, i) => (
            <div key={i} className="flex gap-3 p-3 bg-dark-800 rounded-lg border border-dark-600">
              <span className="text-sm text-gray-300">{rule}</span>
            </div>
          ))}
        </div>
      </Step>

      <div className="card bg-lemon-500/5 border-lemon-500/20">
        <h3 className="font-semibold text-lemon-400 mb-2">🍋 Herzlich Willkommen im LemonPvP Team!</h3>
        <p className="text-gray-400 text-sm">
          Du bist jetzt Teil eines engagierten Teams. Gemeinsam sorgen wir dafür, dass LemonPvP eine faire und
          spaßige Umgebung für alle Spieler bleibt. Bei Fragen stehen dir alle Admins gerne zur Verfügung.
        </p>
      </div>
    </div>
  )
}

function Step({ number, title, children }: { number: number; title: string; children: React.ReactNode }) {
  return (
    <div className="card space-y-3">
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-full bg-lemon-500 flex items-center justify-center text-dark-900 font-bold text-sm flex-shrink-0">
          {number}
        </div>
        <h2 className="font-semibold text-white text-lg">{title}</h2>
      </div>
      <div className="text-gray-400 text-sm leading-relaxed ml-11">{children}</div>
    </div>
  )
}

function Tip({ children }: { children: React.ReactNode }) {
  return (
    <div className="mt-3 bg-lemon-500/10 border border-lemon-500/20 rounded-lg px-3 py-2 text-sm text-lemon-400">
      💡 {children}
    </div>
  )
}

function ActionItem({ icon, label, desc }: { icon: string; label: string; desc: string }) {
  return (
    <div className="flex items-start gap-3 p-2 rounded-lg bg-dark-800 border border-dark-600">
      <span className="text-lg leading-none mt-0.5">{icon}</span>
      <div>
        <div className="text-sm font-medium text-gray-200">{label}</div>
        <div className="text-xs text-gray-500">{desc}</div>
      </div>
    </div>
  )
}
