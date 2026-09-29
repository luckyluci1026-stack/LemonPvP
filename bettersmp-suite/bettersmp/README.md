# BetterSMP

SMP-Kernplugin (Paper 1.21.11). Neutral gehalten – setze deinen Server-Namen
einmal über `brand` in der `config.yml`, er erscheint überall als `%brand%`.

## Features
- **Chat** im MiniMessage-Stil mit LuckPerms-Prefix/Suffix, PlaceholderAPI,
  klickbaren Links und `@Erwähnungen`. Jede Nachricht geht zusätzlich an
  SMPProxy (Kanal `bettersmp:chat`), der sie in der Lobby anzeigt - und
  Nachrichten aus der Lobby kommen umgekehrt auf dem SMP an.
- **NoChatReports**: Chat als System-Nachricht (nicht meldbar) + optionaler
  `server.properties`-Patch.
- **AntiCombatLog** mit `PlayerCombatLogEvent`-API (für Lifesteal+).
  Im Kampf sind auch `/spawn`, `/hub`, `/lobby` und `/server` gesperrt:
  BetterSMP meldet den Kampf an SMPProxy (Kanal `bettersmp:combat`), der
  diese Proxy-Befehle dann blockt.
- **Ban-/Mute-System** mit eigenem, **bedrock-freundlichem Ban-Screen**:
  - `/gban <Spieler> <Grund>` – Gründe (feste Dauer + Screen) in `bans.yml`
  - `/gunban <Spieler>`
  - `/gmute <Spieler> <Grund>` – Gründe in `mutes.yml`
  - `/gunmute <Spieler>`
  - Neue Gründe einfach ergänzen (Dauer wie `30m`, `7d`, `perm`).
  - Ein Mute gilt im ganzen Netzwerk: BetterSMP meldet ihn an SMPProxy
    (Kanal `bettersmp:mute`), der dann auch Lobby-Chat, Duell-Chat und
    `/msg` blockt.
- **Datenbank**: MariaDB (wenn konfiguriert) oder automatisch SQLite. Speichert
  Stats, Bans und Mutes. JDBC-Treiber lädt Paper via `libraries:` zur Laufzeit.
- **Inventar-/Enderkisten-Backup** (`backup` in `config.yml`): sichert alle
  15 Sekunden (einstellbar) asynchron Inventar + Enderkiste jedes
  Online-Spielers in eine **komplett eigene** zweite Datenbank (eigene
  SQLite-Datei standardmäßig, optional eigene MariaDB) - unabhängig von der
  Haupt-Datenbank, damit ein Problem dort diese Sicherung nicht mitreißt.
  Klappt die eigene Backup-MariaDB mal nicht (falsche Zugangsdaten, Server
  down), fällt auch dieses Backup automatisch auf die lokale SQLite-Datei
  zurück, statt komplett auszusetzen - dasselbe Prinzip wie bei der
  Haupt-Datenbank. Immer nur der letzte Stand, kein Verlauf. Wiederherstellen für einen
  ONLINE Spieler: `/bettersmp backup restore <Spieler>`, Status prüfen:
  `/bettersmp backup status <Spieler>`.
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
- **/freeze <Spieler>**: friert jemanden ein (Position gesperrt, Bauen/Abbauen/
  Schaden/Wegwerfen gesperrt, Reden bleibt möglich) - für die Minute vor
  einem `/gban`, in der man erst reden will. Erinnert per Actionbar 1x/Sekunde
  daran (`freeze.actionbar` in config.yml), damit "warum bewege ich mich
  nicht" nicht die einzige Rückmeldung ist. **/freeze** ohne Ziel zeigt dem
  Team, wer gerade eingefroren ist, seit wann und ob noch online.
  Eingefroren gehen auch `/spawn`, `/lobby` und `/server` nicht (Sperre über
  SMPProxy, wie im Kampf).
- **/tutorial** (auch `/anleitung`): Menü mit allen wichtigen Themen für
  neue Spieler - Erste Schritte, Geld & Shop, Auktionshaus, Teleportieren,
  Duelle, Kämpfen, Chat, Regeln. Klick auf ein Thema schreibt die Erklärung
  in den Chat, `/tutorial 3` zeigt Thema 3 direkt. Beim allerersten Beitreten
  öffnet es sich nach ein paar Sekunden von selbst. Alles steht in
  `tutorial.yml`; ein Thema mit `befehl: ah` erscheint nur, wenn es `/ah` auf
  dem Server gibt.
- **Beitreten/Verlassen**: Mit `join-quit.netzwerk: true` (Standard) kommt
  "ist dem Server beigetreten" von SMPProxy - einmal fürs ganze Netzwerk,
  nicht bei jedem Wechsel zwischen Lobby, SMP und Duels. Mit `false` gelten
  wieder die eigenen Texte unter `join-quit`.
- **/report <Spieler> <Grund>**: für alle, mit Cooldown. Landet live bei
  jedem mit `bettersmp.report.receive` und in `reports.log`.
- **Serien-Ansagen im PvP**: Meilensteine (3, 5, 10 ...) und "Serie beendet"
  - reine Stimmung, keine Belohnung, nichts überlebt einen Neustart. Die
  laufende Serie steht auch auf dem Scoreboard (`%streak%`).
- **/daily**: eine Kleinigkeit fürs Wiederkommen, mit Bonus für aufeinander-
  folgende Tage (Kalendertag-genau, kein 24-Stunden-Timer zum Austricksen).
  Wer beim Join noch nicht abgeholt hat, bekommt kurz nach der MOTD einen
  Hinweis (`daily-reward.join-reminder` in config.yml, Standard an).
- **/spawn, /setspawn**: eigener Serverspawn, unabhängig von Essentials.
  Ohne `death-redirect` respawnt man dort statt am zufälligen Bett.
- **Tod → Lobby** (`death-redirect` in `config.yml`, Standard AN mit
  `server: "Lobby"`): nach dem Respawnen automatisch zurück auf den
  Lobby-Server, statt am Bett/Weltspawn weiterzuspielen. Braucht einen Proxy
  mit Kanal `BungeeCord` (bei Velocity Standard).

## Wichtige Configs
- `config.yml` – `brand`, Datenbank, Inventar-/Enderkisten-Backup, Chat, Combat, Join/Quit, Nametags, Scoreboard, Installer
- `bans.yml` / `mutes.yml` – Gründe, Dauern, Ban-Screen
- `scoreboard.yml` – Sidebar-Zeilen (Kills, K/D usw. werden nur aus der
  Datenbank geholt, wenn sie auch auf dem Board stehen)
- `tutorial.yml` – Themen, Texte und Plätze im `/tutorial`-Menü
- `messages.yml` – alle Texte (MiniMessage **und** Legacy-Farbcodes)

## Rechte (Auszug)
`bettersmp.ban`, `bettersmp.mute`, `bettersmp.stats`, `bettersmp.settings`,
`bettersmp.ban.exempt`, `bettersmp.mute.exempt`, `bettersmp.chat.format`,
`bettersmp.tutorial` (Standard: alle).
