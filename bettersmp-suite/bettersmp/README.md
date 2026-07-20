# BetterSMP

SMP-Kernplugin (Paper 1.21.11). Neutral gehalten – setze deinen Server-Namen
einmal über `brand` in der `config.yml`, er erscheint überall als `%brand%`.

## Features
- **Chat** im MiniMessage-Stil mit LuckPerms-Prefix/Suffix, PlaceholderAPI,
  klickbaren Links und `@Erwähnungen`.
- **NoChatReports**: Chat als System-Nachricht (nicht meldbar) + optionaler
  `server.properties`-Patch.
- **AntiCombatLog** mit `PlayerCombatLogEvent`-API (für Lifesteal+).
- **Ban-/Mute-System** mit eigenem, **bedrock-freundlichem Ban-Screen**:
  - `/gban <Spieler> <Grund>` – Gründe (feste Dauer + Screen) in `bans.yml`
  - `/gunban <Spieler>`
  - `/gmute <Spieler> <Grund>` – Gründe in `mutes.yml`
  - `/gunmute <Spieler>`
  - Neue Gründe einfach ergänzen (Dauer wie `30m`, `7d`, `perm`).
- **Datenbank**: MariaDB (wenn konfiguriert) oder automatisch SQLite. Speichert
  Stats, Bans und Mutes. JDBC-Treiber lädt Paper via `libraries:` zur Laufzeit.
- **/stats [Spieler]** – Kills, Tode, K/D, Mob-Kills, Spielzeit, Geld, Rang.
- **Ränge** (`/bettersmp ranks`): Owner in 5 Gradient-Looks, Admin (rot), Mod, Sup,
  default – als LuckPerms-Gruppen mit MiniMessage-Gradient-Prefixen.
- **Nametags**: Gradient-Prefix über dem Kopf + Tab, Name weiß. Liest den
  LuckPerms-Prefix live, **jeder neue LuckPerms-Rang wirkt sofort**. Auch für
  Bedrock/Geyser sauber. TAB-Nametags werden dafür deaktiviert.
- **Scoreboard** (Sidebar) frei konfigurierbar in `scoreboard.yml`
  (Geld, Ping, Spieler, TPS, Kills, K/D, Spielzeit, PlaceholderAPI …).
- **Auto-Installer** + fertige Configs für EssentialsX, LuckPerms, Vault,
  PlaceholderAPI und TAB (deutsche EssentialsX-Nachrichten, **ohne Kits**).
- **/settings-GUI** zum Live-Umschalten der Module.

## Wichtige Configs
- `config.yml` – `brand`, Datenbank, Chat, Combat, Join/Quit, Nametags, Scoreboard, Installer
- `bans.yml` / `mutes.yml` – Gründe, Dauern, Ban-Screen
- `scoreboard.yml` – Sidebar-Zeilen
- `messages.yml` – alle Texte (MiniMessage **und** Legacy-Farbcodes)

## Rechte (Auszug)
`bettersmp.ban`, `bettersmp.mute`, `bettersmp.stats`, `bettersmp.settings`,
`bettersmp.ban.exempt`, `bettersmp.mute.exempt`, `bettersmp.chat.format`.
