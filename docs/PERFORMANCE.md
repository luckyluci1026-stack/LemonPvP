# Performance

## Designentscheidungen

| Anforderung | Umsetzung |
|---|---|
| Keine Hintergrundaktivität bei deaktivierter Runtime | `enabled: false` ⇒ kein Thread, kein Scan, kein Listener, kein Befehl, kein Task; nur einmalig `config/lemon-runtime.yml` lesen (getestet: `BootstrapTest`) |
| Keine periodischen Tasks / Dateisystem-Scans | Scans nur auf Befehl (`/fork module list|enable|reload|validate`) bzw. beim Start, wenn Module aktiviert werden sollen |
| Kein Eingriff in den Tick | keine Minecraft-Patches; die drei Hooks laufen nur bei Start, `/reload` und Stopp |
| Keine blockierende I/O im Tick-Thread | Scannen, Hashen, Kopieren, YAML-Parsen, Klassendefinition auf dem Worker-Thread |
| Geringer Speicherbedarf | Ein Worker-Thread mit 30 s Leerlauf-Timeout (keine Threads im Ruhezustand), Zähler und kleine Maps; kein Classpath-Scanning |
| Kein synchronisierter kritischer Pfad | Event-/Task-Aufrufe der Module laufen durch `CallbackGuard` (ein `AtomicBoolean`-Read, zwei Atomic-Inkremente, Setzen/Zurücksetzen des Context-Class-Loaders) |

## Messungen

**Methode:** `scripts/ci/smoke-test.sh` misst die Wanduhrzeit vom Prozessstart bis zur
Paper-Meldung `Done (…)!` und protokolliert zusätzlich Papers eigene Angabe `Done (Xs)`.
Jeder Wert ist **eine einzelne Messung** auf einem GitHub-Actions-Runner (`ubuntu-latest`,
geteilte VM, Temurin-JDK der jeweiligen Version, `-Xms1G -Xmx2G`, frische Welt,
`view-distance=4`). Schwankungen von mehreren Sekunden zwischen Läufen sind auf solchen
Runnern normal; die Zahlen eignen sich nur für eine grobe Einordnung, **nicht** als Benchmark.

Die konkreten Werte stehen – mit Lauf-ID und Version – in [`docs/TESTING.md`](TESTING.md),
Abschnitt „Server-Starttests“, weil sie aus denselben CI-Läufen stammen.

Nicht gemessen wurden: Tick-Zeiten (MSPT) unter Last, Speicherverbrauch über längere Zeit
und die Kosten vieler gleichzeitiger Module. Dafür wären reproduzierbare Lastszenarien
(Bots/Spieler) auf dedizierter Hardware nötig.
