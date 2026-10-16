# Lemon – Paper-Fork mit optionalen, kontrollierten Live-Updates

Lemon ist eine Paper-basierte Server-Software für Minecraft: Java Edition. Sie verhält sich
standardmäßig wie die jeweilige Paper-Version und bringt zusätzlich eine **optionale,
standardmäßig deaktivierte Runtime** mit. Sie kann einen laufenden Server auf drei klar
getrennten Wegen ändern (Details: [`docs/LIVE-UPDATES.md`](docs/LIVE-UPDATES.md)):

| | Was | Wie |
|---|---|---|
| **Module** | eigene Erweiterungen mit Lemon-API | laden, aktivieren, ersetzen (Hot-Swap mit Rollback), entfernen |
| **Plugins** | normale Paper/Bukkit-Plugins | Kompatibilitätsprüfung; Live-Update nur, wenn sicher möglich, sonst „Neustart nötig“ |
| **Core-Patches** | Code von Paper/Minecraft/Lemon im laufenden Prozess | JVM-Klassen-Redefinition über einen Java-Agenten; Standard-JVM: nur Methodenrümpfe |

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
* die Server-Marke (`Lemon` statt `Paper`);
* im Build zwei Gradle-Tasks für Core-Patches (`livePatchBaseline`, `createLivePatch`).
  Der Java-Agent `lemon-livepatch-agent.jar` ist eine separate Datei und nur aktiv, wenn
  der Server mit `-javaagent:` gestartet wird.

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

## Schnellstart Live-Updates (Plugins, Module, Core-Patches)

```
fork live status                     # was ist erlaubt, was ist aktiv
fork live scan                       # was liegt bereit (lemon/modules, plugins/update, lemon/patches/inbox)
fork live check <name>               # live möglich? (ohne Änderung)
fork live update <name>              # Plan anzeigen …
fork live update <name> confirm      # … und ausführen
fork live rollback <name> [confirm]  # vorherige Plugin-JAR / Bytecode vor dem Patch
fork live inspect <name>             # Ressourcen, Abhängigkeiten, Status, alte Versionen im Speicher
fork live update all [confirm]       # alle live-fähigen Plugins aus plugins/update nacheinander
fork live capabilities | diagnostics # was dieser Prozess kann / Gesamtzustand
fork live patch verify <id>          # laufenden Bytecode eines Patches zurücklesen und vergleichen
fork live history
```

Voraussetzungen (`plugin-updates.enabled`, `live-patching.enabled`, JVM-Option
`-javaagent:lemon-livepatch-agent.jar`), Schritt-für-Schritt-Anleitungen, was live geht
und was einen Neustart braucht: [`docs/LIVE-UPDATES.md`](docs/LIVE-UPDATES.md).

## Dokumentation

| Thema | Datei |
|---|---|
| Analyse (Phase 1), verifizierte Versionen, Risiken | [`docs/ANALYSIS.md`](docs/ANALYSIS.md) |
| Versionsmatrix und Teststatus | [`docs/VERSIONS.md`](docs/VERSIONS.md) |
| Fork bauen | [`docs/BUILDING.md`](docs/BUILDING.md) |
| Upstream-Synchronisierung, neue Versionen | [`docs/UPSTREAM-SYNC.md`](docs/UPSTREAM-SYNC.md) |
| **Live-Updates: Module, Plugins, Core-Patches – Anleitung, Grenzen, Workflow IntelliJ → Server** | [`docs/LIVE-UPDATES.md`](docs/LIVE-UPDATES.md) |
| Speer/Attribut-Swap 1.21.11: Analyse, Ursache, Fix, Live-Patch-Test | [`docs/SPEAR-COOLDOWN.md`](docs/SPEAR-COOLDOWN.md) |
| Hot-Deployment: Architektur, Abläufe, Konfiguration, Befehle | [`docs/HOT-DEPLOYMENT.md`](docs/HOT-DEPLOYMENT.md) |
| Modulformat und Modulentwicklung | [`docs/MODULE-FORMAT.md`](docs/MODULE-FORMAT.md) |
| **Testmodule** (Stress, Fehler, Abhängigkeiten, Leaks) zum Ausprobieren | [`docs/TEST-MODULES.md`](docs/TEST-MODULES.md) |
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
runtime/          Runtime (API, Kern, Paper-Adapter, Live-Patch-Agent, Beispielmodul, Test-Plugin) – eine Codebasis für alle Versionen
versions/<mc>/    Paperweight-Fork je Minecraft-Version (gepinnter Paper-Commit + Lemon-Patches)
scripts/fork/     Generatoren für Versionsprojekte und Patches
scripts/livepatch/ Gradle-Tasks für Core-Patches (in jede Server-Build-Datei eingebunden)
scripts/ci/       Server-Tests der CI (Module, Plugins, Core-Patches)
docs/             Dokumentation
index.html, index.zip   bereits vorhandene Website-Dateien (nicht Teil der Fork)
```
