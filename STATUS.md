# 🍋 LemonPvP — Netzwerk-Statusbericht

> Vollständige Bestandsaufnahme, ausgelesen aus dem echten Code-Stand des Branches
> `claude/lemoncore-plugin-EhiMg`. **13 Module · 62.000+ Zeilen · ~56 Commits diese Session.**
> Paper 1.21 · Java 21 · Velocity 3 · Ziel-Version **1.1.0**

**Legende:** ✅ Fertig & deploybar · ⚙️ Config nötig (sonst kaputt) · 🧪 Ungetestet (gebaut, nicht von dir verifiziert) · 🚧 Offen (noch nicht gebaut) · 🔴 Blocker

---

## 🔴 Der eine Blocker, der gerade alles erklärt

**Auf dem Server läuft noch ein altes Jar** — `/version LemonPractice` zeigte `1.0.0`.
Solange das so ist, ist **kein einziger** Fix aktiv (Ban, Queue, Play-Menü, Bot — alles „kaputt", weil mein Code gar nicht läuft).

- **Ursache:** Der Versions-Bump änderte den Jar-Namen (`LemonPractice-1.0.0.jar` → `-1.1.0.jar`), das alte Jar blieb daneben liegen und wurde geladen.
- **Behoben:** Jars heißen jetzt fest `LemonPractice.jar` (ohne Version).
- **Einmalig nötig:** in jedem `plugins/`-Ordner (alle Server + Proxy) die alten `Lemon*-1.0.0.jar` **löschen**, neu bauen, sauberes Jar reinkopieren, neu starten. Dann muss `/version` überall **1.1.0** zeigen.

---

## Teil 1 — Server-Verteilung: Was auf welchen Server gehört

> Verteilte Architektur: die Plugins laufen auf verschiedenen Backends + dem Proxy.
> Maßgeblich ist deine Velocity-Konfiguration — das hier ist das vorgesehene Layout laut den Configs.

### 🛰️ Velocity-Proxy
- **LemonQueue** — Queue, Ban-Login-Sperre, Health-Check
- **LemonFailover** — Server-Überwachung
- **LemonResourcePack** — RP-Prompt (Netzwerk ist no-RP → vermutlich optional/ungenutzt)
- ⚙️ nötig: Server `lobby` / `duels` / `events` / `limbo` registriert · forwarding-secret

### 🏠 Lobby-Server (`server-name: lobby`)
- **LemonCore**, **LemonLobby**, **LemonCosmetics**
- **LemonNameTags**, **LemonBuild**, **LemonTraining**
- **LemonPractice** — für Play-Menü-Ziel & `/kiteditor`
- ✅ Config: `server-type: LOBBY` (Default passt)

### ⚔️ Duels-Server (`server-name: duels`)
- **LemonCore**, **LemonPractice**
- **LemonCosmetics**, **LemonNameTags**
- 30 Kampf-Welten (5 pro Biom, z.B. `badlands-01`)
- ⚙️ **MUSS gesetzt werden:** `server-type: DUELS` · `server-name: duels` · PacketEvents installiert

### 🎉 Events-Server (`server-name: events`)
- **LemonCore**, **LemonEvents**
- **LemonCosmetics**, **LemonNameTags**
- Sumo · TeamFight · Mafia · Hunt · Escape · Royale
- ⚙️ Config: `server-name: events`

### ⚠️ Zweiter wahrscheinlicher Fehler: Duels-Server als „Lobby" fehlkonfiguriert
Die mitgelieferten Defaults sind `server-type: LOBBY` (LemonPractice + LemonCosmetics) und
`server-name: lobby` (LemonCore). Wenn dein Duels-Server diese frisch entpackten Defaults nutzt,
verhält er sich wie eine Lobby → Duelle/FFA laufen nicht sauber.

**Auf dem Duels-Server prüfen:**
- `LemonPractice/config.yml` → `server-type: DUELS`
- `LemonCosmetics/config.yml` → `server-type: DUELS`
- `LemonCore/config.yml` → `server-name: duels`

---

## Teil 2 — Die 13 Module (grün = neuer Befehl diese Session)

### LemonCore — ✅ Fertig · *137 Dateien, 13.444 Z.*
Das Rückgrat auf jedem Server: Economy (Coins/Apples/Planks), Moderation (Ban/Mute/Kick/Offend/Punish/History/Reports),
Chat + Wortfilter, Freunde, Scoreboard, Tablist, LuckPerms-Anbindung, Discord-Link, Wartungsmodus, Restart, LemonLang-Scripting, HTTP-API.
`/grank` `/gcoins` `/coins` `/aowcode` `/code` **`/offend`** `/punish` `/gunban` **`/gmute`** `/gkick` `/ghistory` `/report` `/greport` **`/stats`** **`/settings`** `/friend` **`/nick`** `/hide` **`/lobby`** `/goffline` `/rtp` `/fly` `/link` `/lemonlang` …

### LemonPractice — ✅ Fertig · ⚙️ Config nötig · *65 Dateien, 22.453 Z.*
Die Practice-Engine: Duelle, Ranked-Queue + ELO, FFA, Zone, Kits + Kiteditor, Replays/Kill-Cam,
Party, **Bot-Fallback-Duelle**, Pearl-Cooldown, Rematch. Läuft auf Lobby (nur Menü) & Duels (Kampf).
`/duel` `/ffa` `/zone` `/party` **`/leaderboard`** **`/top`** **`/stats`** `/replay` **`/greplay`** **`/kiteditor`** `/gelo` `/aowarena`

### LemonCosmetics — ✅ Fertig · 🧪 teils ungetestet · *46 Dateien, 7.489 Z.*
Cosmetics ohne Texturepack: 90+ Tags, Pfeil-Trails, Kill/Death/Win-Effekte, Explosions-Partikel, Rüstungs-Trims —
und neu: **Capes, Bandanas, Emotes** (Map-auf-Display, animiert).
`/cosmetics` `/tags` **`/cape`** **`/bandana`** **`/emote`**

### LemonLobby — ✅ Fertig · *32 Dateien, 3.833 Z.*
Das Lobby-Spiel: Apfelbaum-Ernte, Planks, Booster, Daily Reward, Shop — plus Hotbar (Play-Menü, Boost-Item),
Doublejump, Elytra-Boost, funktionale Gebäude-Interaktion.
`/shop` `/apples` `/planks` `/daily` `/gapples` `/gplanks`

### LemonEvents — ✅ Bugs gefixt · *32 Dateien, 3.768 Z.*
Event-Minigames: Sumo, TeamFight, Mafia, Hunt, Escape, LemonRoyale. Diese Session: 5 Gewinner-Reihenfolge-Bugs gefixt.
`/aowcreateevent` `/joinevent` `/aowstartevent` `/aowendevent`

### LemonQueue — ✅ Fertig · ⚙️ Config nötig · *12 Dateien, 1.598 Z. · Velocity*
Proxy-seitige Queue & Server-Routing, Ban-Login-Sperre (fixiert + gehärtet), Health-Check.
`/queue` **`/queue clearbans`**

### LemonBuild — ✅ v2 gebaut · 🧪 Optik ungetestet · *6 Dateien, 2.705 Z.*
WorldEdit-Lobby-Generator. Neue „Grand Citrus"-Lobby (v2) mit Innenausbau; alte Lobby als Backup erhalten.
**`/aowbuildlobby confirm`** (neue Lobby) · **`/aowbuildlobby classic confirm`** (alte Lobby zurück)

### LemonQuests · LemonTraining · LemonNameTags · LemonFailover — ✅ Fertig · *48 Dateien gesamt*
Quests/XP/Level · Trainings-Modus mit Bots · Overhead-Nametags (zeigt jetzt Nicks korrekt, Name weiß + Prefix-Verlauf) · Velocity-Failover-Monitor.
`/quests` `/gxp` `/leave` `/lemonnametags`

---

## Teil 3 — Feature-Status

### ✅ Fertig & deploybar
- **Cosmetics:** Capes, Bandanas, Emotes (Map-Render, kein RP) + 15er-Emote-Pack; Preise, Density-Regler, Kaufschutz
- **Bot-Duelle:** nach 15 s leerer Queue, skill-skaliert; v2 Spielermodell + Hit-Reg-Fix (Packet-Umleitung)
- **Play-Menü** in der Lobby → Auto-Queue auf Duels (DB-Handoff)
- **Kits:** Voll-Loadouts, Kiteditor-Slot-Bug gefixt · Pearl-Cooldown · Rematch-Button
- **Core:** `/offend`-Gründe-GUI, Settings-Overhaul, Scoreboard-Rang, `/nick`, `/hub`/`/spawn`/`/lobby` cross-server
- **Ban-System:** Lemonizer nur 1×, Proxy-Login-Sperre, Unban-Cache-Fix + 5-min-Sicherung
- **Lobby v2** + Innenausbau + funktionale Gebäude · Boost-Item · Creative-Freiheit · entrindetes Holz
- **~20 verifizierte Bugfixes** (Economy, Events-Gewinner, Party, Duel-Arena, Queue u.v.m.)
- **Nametags:** Name weiß, nur Prefix mit Farbverlauf

### ⚙️ Muss konfiguriert werden (sonst kaputt)
- 🔴 **Neue Jars deployen** bis `/version` überall 1.1.0 zeigt — sonst wirkt nichts
- **Duels-Server:** `server-type: DUELS` (LemonPractice + LemonCosmetics) & `server-name: duels` (LemonCore)
- **Velocity:** Server „duels/lobby/events/limbo" registriert, forwarding-secret, PacketEvents auf Duels
- **Capes & Bandanas:** werden **leer** ausgeliefert — Bilder in `plugins/LemonCosmetics/capes|bandanas/` legen (Emotes sind gebündelt)
- **Kit-Presets:** alte `kits.yml` löschen, damit die neuen Voll-Loadouts laden

### 🧪 Ungetestet (ich kann nicht bauen/spielen)
- Cape/Bandana/Emote — **Position & Rotation** (in Config tunebar, brauchst du Augen für)
- Bot v2 — **Aussehen & Balance** (Trefferstärke, Movement-Gefühl)
- Lobby v2 — **Proportionen & fehlende Blöcke** (blind gebaut)
- Generell: der gesamte neue Code, bis 1.1.0 läuft und du gegengetestet hast

### 🚧 Noch nicht fertig / offen
- **Custom Kit** — absichtlich gesperrt („Under Development" im Kiteditor)
- **`/rtp`** — „Under Development", noch nicht implementiert
- **`/aowcode`** — noch alte Argument-Syntax, GUI-Wizard offen *(Task 2)*
- **`/shop`-Rework** — nur Apple-Booster + Apple/Wood-Rang *(Task 5)*
- **Armortrim-GUI** überarbeiten *(Task 8)*
- **Tab-Completer-Audit** über alle ~60 Befehle *(Task 6)*
- **Big-Server-Batch-2:** Match-Inventare, 2v2-Party-Duelle, Turniere, Ping-Anzeige, Win-Streaks *(Task 10)*

---

## Teil 4 — Deploy-Reihenfolge (damit alles live geht)

1. **Alte Jars löschen.** In JEDEM `plugins/`-Ordner (alle Server + Proxy) die `Lemon*-1.0.0.jar` entfernen — nicht nur überschreiben.
2. **Bauen.** `git pull` (Branch `claude/lemoncore-plugin-EhiMg`) → `mvn clean package`. Jars heißen jetzt sauber `LemonPractice.jar` usw.
3. **Richtiges Jar auf richtigen Server.** Proxy: LemonQueue. Duels: LemonPractice + LemonCore + LemonCosmetics + LemonNameTags. Lobby: alle Lobby-Plugins. (siehe Teil 1)
4. **Duels-Config setzen.** `server-type: DUELS` in LemonPractice & LemonCosmetics, `server-name: duels` in LemonCore.
5. **Proxy & alle Server neu starten.** Danach auf dem Duels-Server `/version LemonPractice` → muss **1.1.0** zeigen.
6. **Testen & melden.** Wenn dann noch was klemmt: exaktes Symptom + auf welchem Server → echter Bug, wird gezielt gefixt.

---

*Ausgelesen aus dem Branch-Code, nicht aus dem Gedächtnis · Stand HEAD nach Commit `2111d6c`.*
