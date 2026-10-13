# Lemon – Paper-Fork mit optionalem, kontrolliertem Hot-Deployment

Lemon ist eine Paper-basierte Server-Software für Minecraft: Java Edition. Sie verhält sich
standardmäßig wie die jeweilige Paper-Version und bringt zusätzlich eine **optionale,
standardmäßig deaktivierte Runtime** mit, die eigene Erweiterungen (**Module**) zur Laufzeit
kontrolliert laden, aktivieren, deaktivieren und ersetzen kann.

> Lemon ist **kein offizielles Paper-Projekt** und steht in keiner Verbindung zu PaperMC,
> Mojang oder Microsoft. Lizenz: GPL-3.0 – siehe [`NOTICE.md`](NOTICE.md) und [`LICENSE`](LICENSE).

## Unterstützte Versionen

| Minecraft | Paper-Upstream | JDK | Status |
|---|---|---|---|
| 1.21.11 | `ver/1.21.11` @ `c5eb079` | 21 | unterstützt |
| 26.1.2 (Linie 26.1) | `ver/26.1.2` @ `e4e17fc` | 25 | unterstützt |
| 26.2 | `ver/26.2` @ `58e6e45` | 25 | unterstützt |
| 26.3 | `main` @ `6829d9e` (kein Paper-Release-Tag) | 25 | in Arbeit (Upstream-Entwicklungsstand) |

Details, Testergebnisse und Begründungen: [`docs/VERSIONS.md`](docs/VERSIONS.md).
Jede Version ist ein eigenes Build-Artefakt – es gibt keine „Universal-JAR“.

## Was Lemon ändert (gegenüber Paper)

* drei einzeilige Hooks in `CraftServer` (nach dem Aktivieren der Plugins, vor dem
  Deaktivieren, vor `/reload`), die bei deaktivierter Runtime nur eine Konfigurationsdatei lesen;
* die Runtime-Klassen im Server-JAR (`dev.lemonpvp.runtime.*`);
* die Server-Marke (`Lemon` statt `Paper`).

Keine Änderungen am Minecraft-Code, am Tick-Ablauf, am Netzwerk oder an Paper-Befehlen.

## Schnellstart Server

Fertige JARs entstehen in der CI (`Actions → Lemon CI → Artefakt lemon-<version>`) oder lokal:

```bash
cd versions/26.2
./gradlew applyAllPatches build createPaperclipJar   # 1.21.11: createMojmapPaperclipJar
java -jar lemon-server/build/libs/lemon-paperclip-*.jar --nogui
```

Voraussetzungen und Netzwerkzugriffe: [`docs/BUILDING.md`](docs/BUILDING.md).

## Schnellstart Hot-Deployment

1. `config/lemon-runtime.yml` (wird beim ersten Start angelegt):
   ```yaml
   enabled: true
   allow-runtime-deploy: true   # nur auf Test-/Entwicklungsservern dauerhaft
   ```
2. Server neu starten. Modul ablegen:
   ```bash
   cp example-greeter.jar example-greeter.jar.sha256 lemon/modules/
   ```
3. In der **Konsole**:
   ```
   fork module list
   fork module enable example-greeter
   fork module diagnostics example-greeter
   fork module reload example-greeter     # Hot-Swap mit Rollback
   fork module config example-greeter     # Konfiguration ohne Neustart
   fork module disable example-greeter
   ```

Ändernde Befehle sind standardmäßig **nur in der Serverkonsole** erlaubt
(`lemon.runtime.module.manage` hat den Default `false`; RCON zählt nicht als Konsole).

## Dokumentation

| Thema | Datei |
|---|---|
| Analyse (Phase 1), verifizierte Versionen, Risiken | [`docs/ANALYSIS.md`](docs/ANALYSIS.md) |
| Versionsmatrix und Teststatus | [`docs/VERSIONS.md`](docs/VERSIONS.md) |
| Fork bauen | [`docs/BUILDING.md`](docs/BUILDING.md) |
| Upstream-Synchronisierung, neue Versionen | [`docs/UPSTREAM-SYNC.md`](docs/UPSTREAM-SYNC.md) |
| Hot-Deployment: Architektur, Abläufe, Konfiguration, Befehle | [`docs/HOT-DEPLOYMENT.md`](docs/HOT-DEPLOYMENT.md) |
| Modulformat und Modulentwicklung | [`docs/MODULE-FORMAT.md`](docs/MODULE-FORMAT.md) |
| Capability-Matrix / Neustartanforderungen | [`docs/RESTART-REQUIREMENTS.md`](docs/RESTART-REQUIREMENTS.md) |
| Sicherheit | [`docs/SECURITY.md`](docs/SECURITY.md) |
| Kompatibilität (Proxy, ViaVersion, Geyser …) | [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md) |
| Bekannte Einschränkungen | [`docs/LIMITATIONS.md`](docs/LIMITATIONS.md) |
| Troubleshooting | [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md) |
| Tests und Ergebnisse | [`docs/TESTING.md`](docs/TESTING.md) |
| Performance-Messungen | [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) |
| Beispiele (Konfigurationen, Deskriptor, Allow-List) | [`docs/examples/`](docs/examples) |
| Mitwirken | [`CONTRIBUTING.md`](CONTRIBUTING.md) |

## Repository-Struktur

```
runtime/          Hot-Deployment-Runtime (API, Kern, Paper-Adapter, Beispielmodul) – eine Codebasis für alle Versionen
versions/<mc>/    Paperweight-Fork je Minecraft-Version (gepinnter Paper-Commit + Lemon-Patches)
scripts/fork/     Generatoren für Versionsprojekte und Patches
scripts/ci/       Server-Starttests
docs/             Dokumentation
index.html, index.zip   bereits vorhandene Website-Dateien (nicht Teil der Fork)
```
