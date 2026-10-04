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
  diese Proxy-Befehle dann blockt. Gesperrte Befehle werden über ihren echten
  Namen erkannt, also auch Kurzformen wie `/espawn`, `/call`, `/tpyes` oder
  `/essentials:tpa` (gilt genauso fürs Einfrieren). Im Kampf teleportiert
  auch kein Befehl mehr weg, z. B. eine vorher geschickte und jetzt
  angenommene `/tpa` (`combat.block-teleport`). Enderperlen gehen weiter.
- **Homes** (`/home`, `/sethome`, `/delhome`): 3 Plätze pro Spieler
  (`homes.anzahl`, 1 bis 7) mit Menü. `/home` öffnet das Menü: Bett anklicken
  teleportiert, Rechtsklick verschiebt das Home an die eigene Position, darunter
  „Löschen“ mit Rückfrage, freie Plätze setzt ein Klick. `/home <Nummer|Name>`
  teleportiert direkt, `/sethome [Nummer|Name]` setzt (ohne Angabe den ersten
  freien Platz), `/delhome <Nummer|Name>` löscht. Vor dem Teleport 3 Sekunden
  stillhalten (`homes.aufwaermen-sekunden`, Team mit `bettersmp.homes.sofort`
  sofort). Bewegen oder Schaden bricht ab, im Kampf und eingefroren geht es
  nicht, und ist das Home zugebaut oder steht über Lava, sucht BetterSMP den
  nächsten sicheren Platz darüber oder darunter. Gespeichert wird pro Spieler
  in `plugins/BetterSMP/homes/<UUID>.yml`.
  **Umstieg von Essentials:** Beim ersten Join übernimmt BetterSMP das alte
  Essentials-Home automatisch als Home 1 (bis zu 3 alte Homes, Namen bleiben),
  der Spieler bekommt dazu einen kurzen Hinweis. `/home`, `/homes`, `/sethome`
  und `/delhome` landen dabei immer bei BetterSMP, nicht bei Essentials.
  Mit `homes.enabled: false` übernimmt wieder Essentials.
- **Standard-Rechte**: Beim Start trägt BetterSMP alles, was normale Spieler
  brauchen, selbst in die LuckPerms-Gruppe `default` ein (Voice-Chat, Homes,
  `/tpa` & Co., Geld, Shop, Auktionshaus, `/rtp`, Duelle, `/report` …, Liste
  unter `standard-rechte` in der `config.yml`). Fehlendes kommt dazu, nichts
  wird gelöscht. Steht ein Recht dort ausdrücklich auf `false`, bleibt es so,
  und in der Konsole steht ein Hinweis. Abgeschaltet werden die alten
  Essentials-Homes, Essentials-`/msg` (das `/msg` vom Proxy prüft Filter
  und Mutes) und Essentials-`/sell` (`/esell` würde sonst mit den eigenen
  Essentials-Preisen am Shop vorbei verkaufen, verkauft wird nur über FastShop).
  Die Team-Gruppen (`sup`, `mod`, `admin`, `owner1`–`owner5`)
  erben automatisch alles von `default`. `/bettersmp ranks` macht dasselbe
  sofort.
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
  SQLite läuft im WAL-Modus (schnelle Schreibzugriffe, neben `data.db` liegen
  deshalb `data.db-wal` und `data.db-shm` – beim sauberen Stoppen werden sie
  zusammengeführt). Beim Herunterfahren wird alles noch Ausstehende
  geschrieben, bevor die Verbindung zugeht. Fehlen die Tabellen (z. B. nach
  `/dbwipe`), legt BetterSMP sie beim nächsten Zugriff selbst neu an und
  wiederholt den Vorgang - ein Ban direkt nach dem Wipe geht nicht verloren.
  Das gilt auch für die Backup-Datenbank.
- **Inventar-/Enderkisten-Backup** (`backup` in `config.yml`): sichert alle
  15 Sekunden (einstellbar) asynchron Inventar + Enderkiste jedes
  Online-Spielers in eine **komplett eigene** zweite Datenbank (eigene
  SQLite-Datei standardmäßig, optional eigene MariaDB) - unabhängig von der
  Haupt-Datenbank, damit ein Problem dort diese Sicherung nicht mitreißt.
  Die Spieler werden über die 15 Sekunden verteilt statt alle im selben Tick,
  unverändertes Inventar wird nicht neu geschrieben (spätestens alle 10
  Minuten trotzdem), und beim Stoppen gibt es eine letzte Sicherung.
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
  Bedrock/Geyser sauber. TAB-Nametags werden dafür deaktiviert. Es werden nur
  echte Änderungen verschickt: bei 40 Spielern ein Takt ohne Rangwechsel
  0 statt 4.800 Team-Pakete, ein neuer Prefix genau ein Paket pro Spieler.
- **Scoreboard** (Sidebar) frei konfigurierbar in `scoreboard.yml`
  (Geld, Ping, Spieler, TPS, Kills, K/D, Spielzeit, PlaceholderAPI …).
  Auch hier geht nur eine Zeile raus, deren Text sich wirklich geändert hat
  (z. B. der eigene Ping), gleiche Zeilen werden nur einmal pro Takt
  übersetzt. Beim Beitreten wird die Tafel fertig gebaut und in einem Rutsch
  geschickt (bei 40 Spielern 211 statt rund 350 Pakete).
- **Leistung** (`leistung` in `config.yml`):
  - **Bedrock-Sichtweite** (Standard 6): Bedrock-Spieler bekommen weniger
    Chunks – weniger Arbeit für Handys, Konsolen und Geyser. `0` = wie Java.
  - **Dynamische Sichtweite**: Liegt der Server über 45 ms pro Tick (z. B. wenn
    beim Start viele gleichzeitig kommen), sinkt die Sichtweite für alle um je
    einen Chunk (nie unter 5) und steigt wieder, sobald es 30 Sekunden lang
    unter 30 ms bleibt. Die Welt-Einstellung selbst wird nicht angefasst.
  - **Lastmeldung**: meldet SMPProxy alle 2 Sekunden die Auslastung, damit
    der Einlass bei Andrang bremst.
  - **`/bettersmp leistung`** prüft `server.properties`, die Paper-Configs und
    den Startbefehl und sagt, was noch bremst (steht beim Start auch in der
    Konsole). Die Empfehlungen im Detail: `../../LEISTUNG.md`.
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
  öffnet es sich nach ein paar Sekunden von selbst. Bei den ersten 3 Joins auf
  dem SMP steht im Chat der Hinweis auf `/tutorial` (`join-hinweis-anzahl` in
  `tutorial.yml`, gezählt in den Spielerdaten des SMP). Alles steht in
  `tutorial.yml`; ein Thema mit `befehl: ah` erscheint nur, wenn es `/ah` auf
  dem Server gibt.
- **Beitreten/Verlassen**: Mit `join-quit.netzwerk: true` (Standard) kommt
  "ist dem Server beigetreten" von SMPProxy - einmal fürs ganze Netzwerk,
  nicht bei jedem Wechsel zwischen Lobby, SMP und Duels. Mit `false` gelten
  wieder die eigenen Texte unter `join-quit`.
- **/report <Spieler> <Grund>**: für alle, mit Cooldown. Landet live bei
  jedem mit `bettersmp.report.receive` und in `reports.log`. Der Spieler
  muss nicht auf diesem Server sein: letzte Duell-Gegner (von DuelPlus,
  2 Stunden) und alle, die schon einmal hier waren, lassen sich auch auf
  einem anderen Server oder offline melden. Bedrock-Namen gehen mit und
  ohne Punkt.
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

- **Anti-Xray** (`anti-xray` in `config.yml`): Paper hat ein sehr gutes,
  kostenloses Anti-Xray schon eingebaut, es ist nur aus. BetterSMP schaltet
  es beim Start einmal für jede Oberwelt und jeden Nether ein (in deren
  `paper-world.yml`, Sicherung als `.vor-anti-xray`). Modus 3: Erze, die man
  nicht sehen kann, schickt der Server gar nicht an den Client, stattdessen
  falsche Erze in Schichten. Mit X-Ray sieht man nur Unsinn, und es kostet
  weniger Daten als Modus 2. Dazu kommt der Seed-Schutz: In neuen Chunks
  lassen sich Erze nicht mehr aus dem Welt-Seed berechnen (gegen
  Erz-Finder-Mods). BetterSMP richtet das schon beim Laden ein, bevor Paper
  die Welten lädt: **Es wirkt ab dem ersten Start mit der neuen Version**, ein
  zweiter Neustart ist nicht nötig. Nur Welten, die erst später dazukommen,
  brauchen einen Neustart (Admins bekommen dann einen Hinweis). Eigene
  Einstellungen in den Paper-Dateien bleiben unangetastet, eingerichtet wird
  pro Welt nur einmal (gemerkt in `anti-xray.yml`).
- **X-Ray-Alarm**: Wer sich in 20 Minuten zu 4 versteckten Diamant-Adern
  oder Antikem Schrott gräbt und dabei im Schnitt höchstens 40 Blöcke pro
  Fund abbaut, wird dem Team gemeldet (`bettersmp.xray.alarm`, mit `[Hin]`
  und `[Details]` zum Anklicken, dazu Konsole und `xray-alarme.log`). Es
  zählt nur, wozu sich der Spieler selbst hingegraben hat. Erze in Höhlen,
  von TNT/Betten freigesprengter Schrott und was ein Freund freigelegt hat,
  zählen nicht. Eine ganze Ader zählt einmal. Normales Abbauen liegt bei 150
  Blöcken und mehr pro Fund. `/xray` listet die auffälligsten Miner seit dem
  Serverstart, auch langsame ohne Alarm. `/xray <Spieler>` zeigt Details.
  Die Team-Gruppen (`standard-rechte.erben`) bekommen beide Rechte
  automatisch.

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
`bettersmp.tutorial` (Standard: alle), `bettersmp.homes` (Standard: alle),
`bettersmp.homes.sofort` (Standard: OP), `bettersmp.xray` und
`bettersmp.xray.alarm` (Standard: OP und die Team-Gruppen).

`bettersmp.chat.format` erlaubt Farben, Verläufe und Fett/Kursiv im Chat,
aber keine Klick-, Hover- oder sonstigen Sonder-Tags und keinen
Verwirrt-Text (`&k`). So kann niemand Befehle in Chatnachrichten verstecken.
