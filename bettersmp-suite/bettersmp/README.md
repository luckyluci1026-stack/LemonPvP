# BetterSMP

SMP-Kernplugin für LemonPvP (Paper 1.21.11).

## Features
- **Chat im LPC-MiniMessage-Stil** mit LuckPerms-Prefix/Suffix, PlaceholderAPI,
  klickbaren Links und `@Erwähnungen` (Ping + Sound).
- **NoChatReports**: Chat wird als System-Nachricht gesendet (nicht meldbar);
  optional wird `enforce-secure-profile=false` in der `server.properties` gesetzt.
- **AntiCombatLog**: Kampf-Tag bei PvP, gesperrte Befehle im Kampf, Elytra-Sperre,
  Bestrafung (Tod) beim Ausloggen im Kampf – inkl. `PlayerCombatLogEvent`-API für
  andere Plugins (z. B. Lifesteal+).
- **Join/Quit/MOTD** und Erst-Join-Willkommenstitel.
- **Auto-Installer**: lädt EssentialsX, LuckPerms, Vault, PlaceholderAPI und TAB
  beim Start von den offiziellen Quellen und legt fertige, schöne Configs an
  (deutsche EssentialsX-Nachrichten, TAB-Tablist mit LuckPerms-Prefixen).
- **`/settings`-GUI**: schaltet alle Module bequem per Klick an/aus.

## Befehle
- `/settings` – Einstellungs-GUI (Permission `bettersmp.settings`)
- `/bettersmp status|install|update|ranks|deployconfigs|reload` (`bettersmp.admin`)

## Wichtige Permissions
- `bettersmp.chat.format` – darf Farben/MiniMessage im Chat nutzen
- `bettersmp.combat.bypass` – wird im PvP nicht getaggt

Konfiguration: `config.yml` (Chat-Formate, Kampf, Join/Quit, Installer) und
`messages.yml`. Alle Texte verstehen MiniMessage **und** Legacy-Farbcodes (`&7`, `&#FFD75A`).
