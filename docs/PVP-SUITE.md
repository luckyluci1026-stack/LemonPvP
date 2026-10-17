# LemonPvP-Suite

Vier Lemon-Module, die zusammen ein PvP-System ergeben. Jedes Modul lässt sich einzeln
aktivieren, austauschen und abschalten, ohne den Server neu zu starten.

| Modul | Was es macht | Befehle |
|---|---|---|
| `pvp-core` | zählt Kills und Tode, Serien und ein Elo-Rating, speichert alles | `/stats [Spieler]`, `/top [Anzahl]`, `/pvp …` (Admin) |
| `pvp-combattag` | „im Kampf“ nach jedem Treffer: Countdown in der Actionbar, Tod beim Ausloggen, gesperrte Befehle | `/combat [Spieler]` (`/ct`) |
| `pvp-killstreaks` | Ansage bei Serien (Chat, Titel, Sound), Effekt-Belohnungen, Blitz-Effekt, „Serie beendet!“ | – |
| `pvp-scoreboard` | Seitenleiste mit Kills, Toden, K/D, Serie, Rating, Rang; Kampf-Countdown | `/sb` (an/aus) |

`pvp-combattag`, `pvp-killstreaks` und `pvp-scoreboard` brauchen `pvp-core`. Das
Scoreboard zeigt den Kampf-Countdown nur, wenn `pvp-combattag` läuft – ohne geht es auch.

## Installieren

1. Artefakt **`pvp-suite-<version>`** aus GitHub Actions laden (Lauf von „Lemon CI“ →
   Artifacts) oder selbst bauen: `cd runtime && ./gradlew :pvp-suite:collect -PpaperApiVersion=<…>`
   → `runtime/pvp-suite/build/modules/`.
2. Alle vier JARs **mit** ihren `.sha256`-Dateien nach `lemon/modules/`.
3. Konsole (Reihenfolge: zuerst der Core):
   ```
   fork module enable pvp-core
   fork module enable pvp-combattag
   fork module enable pvp-killstreaks
   fork module enable pvp-scoreboard
   ```
   Dauerhaft beim Serverstart: `startup-modules: [pvp-core, pvp-combattag, pvp-killstreaks, pvp-scoreboard]`
   in `config/lemon-runtime.yml` (die Runtime startet sie in der richtigen Reihenfolge).

## Ausprobieren ohne zweiten Spieler

`/pvp sim kill <Killer> <Opfer>` zählt einen Kill genau wie ein echter (gleicher Code-Weg).
Für Namen, die nicht online sind, wird die Offline-UUID verwendet.

```
pvp sim kill Alice Bob      (5×)   → "Alice hat eine 3er-Killstreak!", "… 5er-Killstreak!"
pvp sim kill Bob Alice             → "Bob hat die 5er-Serie von Alice beendet!"
stats Alice                        → Kills 5 · Tode 1 · K/D 5.00 / Serie 0 · beste 5 · Rating 1045 (#1)
top
pvp reset all confirm              → Testdaten wieder löschen
```

Mit deinem eigenen Namen (online) siehst du Titel, Sound, Effekte und das Scoreboard live.

## Befehle und Rechte

| Befehl | Recht | Standard |
|---|---|---|
| `/stats [Spieler]`, `/top [1-20]` | `pvp-core.use` | alle |
| `/pvp save`, `/pvp reset <Spieler>`, `/pvp reset all confirm`, `/pvp sim kill <A> <B>`, `/pvp sim death <A>` | `pvp-core.admin` | OP |
| `/combat [Spieler]` | `pvp-combattag.use` | alle |
| (kein Tod beim Ausloggen, keine gesperrten Befehle) | `pvp-combattag.bypass` | niemand |
| `/sb` | `pvp-scoreboard.use` | alle |

Jeder Befehl geht auch mit Präfix, z. B. `/pvp-core:stats`, falls ein anderes Plugin
`/stats` schon belegt.

## Konfiguration

Jedes Modul hat seine `config.yml` in `lemon/module-data/<modul>/`. Nach dem Bearbeiten:
`fork module config <modul>` – ungültige Werte werden abgelehnt, die alte Konfiguration
bleibt aktiv. Texte sind [MiniMessage](https://docs.advntr.dev/minimessage/format.html)
(`<red>`, `<bold>`, `<gradient:#ff0:#f80>` …).

* `pvp-core`: `k-factor` (wie stark ein Kill das Rating ändert), `autosave-minutes`, Nachrichten.
* `pvp-combattag`: `tag-seconds`, `punish-logout`, `blocked-commands`, Nachrichten.
* `pvp-killstreaks`: `milestones`, `repeat-every`, `shutdown-min-streak`, `big-streak`,
  `lightning`, `rewards` (`"Effekt Stufe Sekunden"`, z. B. `"speed 2 20"`), Nachrichten.
* `pvp-scoreboard`: `refresh-ticks`, `title`, `lines` (Platzhalter `<kills>`, `<deaths>`,
  `<kd>`, `<streak>`, `<best>`, `<rating>`, `<rank>`, `<player>`, `<online>`), `combat-line`.

Statistiken: `lemon/module-data/pvp-core/stats.tsv` (eine Zeile pro Spieler, Tab-getrennt).
Gespeichert wird alle `autosave-minutes`, bei `/pvp save` und beim Deaktivieren.

## Wie es funktioniert (für Java-Lernende)

```
                     PvpStatsService (Interface)          PvpKillEvent (Bukkit-Event)
pvp-core  ──────────────────┬──────────────────────────────────────┬──────────────
                            │ services().find(...)                 │ events().listen(...)
              pvp-scoreboard ◄─────────────────────────┐          ├──► pvp-killstreaks
                            ▲ services().find(...)      │          └──► pvp-scoreboard
pvp-combattag ─── provide(CombatTagService) ───────────┘
```

* **Interfaces als Vertrag** (`dev.lemonpvp.pvp.api`): `PvpStatsService` und
  `CombatTagService` liegen im Core. Die anderen Module kennen nur die Interfaces, nicht die
  Klassen dahinter. `pvp-combattag` stellt seine Implementierung mit
  `services().provide(CombatTagService.class, this.tags::remaining)` bereit – eine
  Methodenreferenz reicht, weil das Interface nur eine abstrakte Methode hat.
* **Records** für unveränderliche Daten: `PlayerStats(id, name, kills, …)`. Nach jedem Kill
  entsteht ein neues Objekt, nichts wird heimlich verändert.
* **Eigene Events**: Der Core feuert nach jedem Kill ein `PvpKillEvent`. Killstreaks und
  Scoreboard hören darauf, statt selbst Tode zu zählen – alle sehen dieselben Zahlen.
* **Reine Logik ohne Server**: `Elo`, `StatsBook`, `StatsFile`, `CombatTags`,
  `StreakRules` benutzen keine Bukkit-Klassen. Deshalb gibt es für sie JUnit-Tests
  (`src/test/java`), die ohne Minecraft laufen. `CombatTags` bekommt die Uhr als
  `LongSupplier` übergeben – im Test „springt“ die Zeit, ohne zu warten.
* **Aufräumen**: Alles läuft über den Modul-Kontext; beim Deaktivieren entfernt die Runtime
  Listener, Befehle, Timer und Services. Was nur das Modul weiß, meldet es selbst an –
  das Scoreboard z. B. `resources().onCleanup("restore …", this::removeAll)`, damit alle
  Spieler beim Abschalten oder Austauschen wieder das normale Scoreboard sehen.
* **Speichern ohne Ruckeln**: Der Core kopiert die Statistik auf dem Hauptthread
  (`snapshot()`) und schreibt die Datei auf einem anderen Thread. Eine Sperre
  (`synchronized`) und eine laufende Nummer verhindern, dass ein älterer Stand einen
  neueren überschreibt. Geschrieben wird in eine `.tmp`-Datei, die dann die alte ersetzt.

Code: `runtime/pvp-suite/<modul>/src/main/java/dev/lemonpvp/pvp/…` – guter Einstieg:
`StatsBook.java` (Regeln), dann `CoreModule.java` (Anbindung an den Server).

## Getestet

* **Unit-Tests** (JUnit, laufen bei jedem Build): Elo, Kills/Serien/Rang, Speichern und
  Laden inklusive kaputter Zeilen, Kampf-Timer, Befehlserkennung, Serien-Regeln und
  Belohnungen – 22 Tests.
* **Servertest 9** (CI, alle vier Versionen, `scripts/ci/smoke-pvp.txt`): Abhängigkeiten,
  simulierte Kills mit exakten Erwartungswerten (Rating 1045, Rang #1 …), Ansagen,
  abgelehnte Konfiguration (inklusive unbekanntem Trankeffekt aus der Server-Registry),
  Hot-Swap von `pvp-killstreaks`, Statistik übersteht einen Reload von `pvp-core`, und nach
  dem Entladen und voller GC ist kein Modul mehr im Speicher. Lauf #36: auf allen vier
  Versionen bestanden (Details in [`TESTING.md`](TESTING.md)).
* **Nicht automatisch getestet**: alles, was einen echten Spieler braucht (Treffer,
  Actionbar, Titel, Sounds, Effekte, Seitenleiste, Tod beim Ausloggen). Der Code dafür nutzt
  nur Paper-API, die gegen beide Versionen ohne Warnungen kompiliert; gesehen hat sie aber
  noch niemand auf einem Server – bitte ausprobieren und melden.
