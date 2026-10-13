# Phase 1 – Analyse (Stand: 2026-10-13)

Diese Datei dokumentiert, was **vor** der Implementierung geprüft wurde, mit welchen
Quellen, und welche Schlüsse daraus gezogen wurden. Aussagen ohne Quelle wurden nicht
getroffen.

## 1. Ausgangszustand des Repositorys

| Prüfung | Ergebnis |
|---|---|
| Repository | `luckyluci1026-stack/LemonPvP`, Arbeits-Branch `claude/paper-fork-hot-reload-2jxsar` |
| Inhalt beim Start | nur `index.html` (Website „LemonPvP – Minecraft Server“) und `index.zip` (dieselbe Datei gepackt), 2 Commits |
| Andere Remote-Branches | `main`, `bucksmpac`, `FLFAC*`, mehrere `claude/*` – unabhängige Projekte (Plugins, Website), keine Paper-Fork |
| Build-System | keines vorhanden |
| Schlussfolgerung | Leeres Fork-Projekt; die vorhandenen Website-Dateien bleiben unverändert, die Fork wird daneben aufgebaut |

## 2. Entwicklungsumgebung (lokal)

| Prüfung | Ergebnis |
|---|---|
| JDK | nur OpenJDK 21.0.12 installiert, kein JDK 25 |
| Gradle (System) | 8.14.3; die Projekte nutzen den Gradle-Wrapper |
| Netzwerk | `repo.papermc.io`, `fill.papermc.io`, `api.papermc.io`, `piston-meta/piston-data.mojang.com`, `libraries.minecraft.net`, `api.github.com` sind über den Proxy **gesperrt (HTTP 403)**; Git-Lesezugriff auf öffentliche GitHub-Repos funktioniert; Gradle Plugin Portal erreichbar; Maven Central erreichbar, aber zeitweise gedrosselt (HTTP 429) |
| Konsequenz | Ein Paper-/Paperweight-Build ist **lokal unmöglich** (Mojang-Artefakte und PaperMC-Maven fehlen). Alle Paper-Builds, Paper-Tests und Server-Starttests laufen deshalb in **GitHub Actions**; deren Ergebnisse wurden über die GitHub-API ausgewertet. Die plattformneutrale Runtime wird zusätzlich lokal gebaut und getestet. |

## 3. Verifizierte Versionen

Quelle für Paper: Git-Klon von `https://github.com/PaperMC/Paper` (Branches, Tags,
`gradle.properties`, `build.gradle.kts`, `gradle-wrapper.properties` je Branch).

| Angefragte Version | Minecraft veröffentlicht? | Paper-Stand | Einordnung |
|---|---|---|---|
| 1.21.11 | ja (Paper-Tag `1.21.11` existiert) | Branch `ver/1.21.11`, Tag `1.21.11` | veröffentlichte Paper-Version |
| 26.1 | ja – laut Drittquellen am 24.03.2026 erschienen; Mojang folgte mit 26.1.1/26.1.2 | **kein** Paper-Branch/Tag `26.1`. Paper-History: Commit „26.1“, danach „Update Vanilla base to 26.1.2 – No actual server changes aside from the usual data version bumps“, danach „Promote build channel to stable“. Branch `ver/26.1.2`, Tag `26.1.2` | Die 26.1-Linie wird von Paper als **26.1.2** gepflegt; Lemon zielt deshalb auf 26.1.2 |
| 26.2 | ja (Paper-Tag `26.2`, PaperMC-News „26.2“) | Branch `ver/26.2`, Tag `26.2` | veröffentlichte Paper-Version |
| 26.3 | ja – laut Minecraft Wiki am 15.09.2026 erschienen | Paper-`main` hat `mcVersion=26.3`, `channel=STABLE` in `gradle.properties`; **kein** Tag `26.3` | Upstream-Entwicklungsstand. Der Kanal im Paper-Download-Dienst konnte nicht direkt geprüft werden (Dienst gesperrt); eine Drittquelle nannte „Beta seit 28.09.“. Lemon führt 26.3 deshalb als **„in Arbeit / Upstream-main“** |

Weitere Quellen (nur ergänzend, nicht als alleinige Grundlage):
[Minecraft Wiki – Java Edition 26.3](https://minecraft.wiki/w/Java_Edition_26.3),
[PaperMC News 26.2](https://papermc.io/news/26-2/),
[minecraft.net – 26.1 Pre-Release 1](https://minecraft.net/en-us/article/minecraft-26-1-pre-release-1).

### Gepinnte Upstream-Commits

| Version | Paper-Branch | Commit (`paperRef`) | Commit-Datum |
|---|---|---|---|
| 1.21.11 | `ver/1.21.11` | `c5eb0790f199da6c38d0a650e1e5cd5415b28185` | 2026-05-11 |
| 26.1.2 | `ver/26.1.2` | `e4e17fc90d31c3dca6de8bebc87c741749f8f3df` | 2026-07-06 |
| 26.2 | `ver/26.2` | `58e6e45d67c998909581f9079189ef51eb6f1267` | 2026-10-09 |
| 26.3 | `main` | `6829d9e6cb4f09ce5f34035f774e2b4b19f164dd` | 2026-10-13 |

### Build-Anforderungen je Version (aus den Paper-Build-Dateien)

| Version | JDK (Toolchain / `options.release`) | Gradle-Wrapper | paperweight |
|---|---|---|---|
| 1.21.11 | 21 | 9.2.0 | `io.papermc.paperweight.core` 2.0.0-beta.19 |
| 26.1.2 | 25 | 9.4.1 | 2.0.0-beta.21 |
| 26.2 | 25 | 9.4.1 | 2.0.0-beta.21 |
| 26.3 | 25 | 9.8.0 | 2.0.0-beta.24 |

`io.papermc.paperweight.patcher` 2.0.0-beta.19/.21/.24 sind im Gradle Plugin Portal
vorhanden (Metadaten geprüft). Paperclip-Tasks laut Paper-CI: `createMojmapPaperclipJar`
(1.21.11) bzw. `createPaperclipJar` (26.x).

## 4. Offizieller Fork-Workflow

Grundlage ist das offizielle Template `PaperMC/paperweight-examples`, Branch `v2-fork`
(Commits „Update to 1.21.11“ `b8d7e6b` und „Update to 26.2“ `fdb5a3f`):

* Root-Projekt mit `io.papermc.paperweight.patcher`, `upstreams.paper { ref = paperRef }`;
* `patchFile` für `paper-server/build.gradle.kts` und `paper-api/build.gradle.kts`;
* `patchDir("paperApi")` für API-Dateipatches, `forks.register(...)` + `activeFork` im
  Server-Build-Patch für Server-Dateipatches (`<fork>-server/paper-patches`) und
  Minecraft-Patches (`<fork>-server/minecraft-patches`);
* Tasks im Template-CI: `applyAllPatches`, `build`.

Abweichungen von Paper ≥ 26.2, die das Template (Stand `fdb5a3f`) noch nicht abdeckt und
die beim ersten CI-Lauf auffielen: `paper-api` wendet das Plugin
`io.papermc.paperweight.paper-checkstyle` an und hängt von `:paper-checkstyle` ab. Siehe
`docs/UPSTREAM-SYNC.md` („Bekannte Upstream-Anpassungen“).

## 5. Relevante Paper-Interna (Quellcode-Analyse)

| Thema | Befund | Folge für das Design |
|---|---|---|
| Lebenszyklus | `CraftServer.enablePlugins(POSTWORLD)` (Start und `/reload`), `CraftServer.disablePlugins()` (Stop), `CraftServer.reload()` | drei einzeilige Hooks genügen; keine Minecraft-Patches, kein Eingriff in den Tick |
| Gebündelte Komponenten | Paper bündelt spark als `SparksFly` mit eigenem Befehl über `CommandMap` | Präzedenzfall für eine eingebaute, optionale Komponente |
| Event-Bus | `PaperEventManager.registerEvent(...)` verlangt nur `plugin.isEnabled()`; `callEvent` überspringt Listener deaktivierter Owner; `EventExecutorFactory` nutzt Hidden Classes (nicht stark referenziert) | Registrierung über einen internen Owner möglich; Executors verhindern kein Entladen |
| Scheduler | `CraftScheduler` prüft `isEnabled()`, nutzt `getName()`, `getLogger()`, `getDescription()` des Owners | interner Owner (`RuntimeHostPlugin`) reicht |
| Befehle | `knownCommands` ist eine `BukkitBrigForwardingMap` auf den Brigadier-Dispatcher; `SimpleCommandMap.register` registriert immer `prefix:label`, das kurze Label nur ohne Konflikt; `remove` entfernt den Brigadier-Knoten | Laufzeit-Registrierung und saubere Entfernung möglich; Kurzlabels werden vorab auf Konflikte geprüft |
| Brigadier-Lifecycle-API | Registrierung nur während Start/Reload | für Hot-Deployment ungeeignet → `CommandMap` (in 26.3 `@ApiStatus.Obsolete`, nicht zur Entfernung markiert) |
| Bibliotheken | SnakeYAML 2.2 in allen vier Versionen; Adventure 4.26.1 (1.21.11, 26.1.2) bzw. 5.2.0 (26.2, 26.3) | Runtime nutzt nur SnakeYAML-SafeConstructor und in beiden Adventure-Versionen vorhandene MiniMessage-API (per `javap` geprüft) |
| Marke | `ServerBuildInfoImpl` liest `Brand-Id`/`Brand-Name` aus dem Manifest | Fork setzt eigene Marke, um nicht als Paper aufzutreten |

## 6. Lizenz

Paper ist GPL-3.0 (geerbt von Bukkit/CraftBukkit/Spigot), einzelne Autoren stellen ihre
Beiträge unter MIT (`LICENSE.md` von Paper). Eine Fork ist abgeleitetes Werk → GPL-3.0.
Details: `NOTICE.md`.

## 7. Architekturentscheidungen und Risiken

| Entscheidung | Begründung | Risiko / Gegenmaßnahme |
|---|---|---|
| Eine Gradle-Build-Struktur pro Version in `versions/<mc>/` statt Branch je Version | In dieser Sitzung darf nur auf einen Branch gepusht werden; Paper braucht je Version eigenes JDK/Gradle/paperweight | Verzeichnisse sind 1:1 als eigene Branches abspaltbar (`docs/UPSTREAM-SYNC.md`) |
| Runtime-Quellen werden per `srcDir` in den Server kompiliert | keine zusätzlichen Paper-Patches, eine Codebasis für alle Versionen | Runtime muss mit Java 21 **und** 25 sowie Adventure 4/5 kompilieren → CI prüft alle vier Versionen |
| Plattformneutraler Kern (`runtime-core`) ohne Paper-Abhängigkeit | lokal testbar, versionsunabhängig | Paper-Adapter separat gegen jede Paper-API geprüft |
| Hot-Swap als „stop-then-start in einem Tick“ mit Rollback statt paralleler Instanzen | doppelte Befehle/Listener wären unsicher | asynchrone Events können die Lücke sehen → dokumentiert |
| Hot-Deployment standardmäßig aus, zweistufige Freigabe (`enabled`, `allow-runtime-deploy`) | sicherheitskritisch | – |
| Patch-Generator mit verankerten Ersetzungen | reproduzierbar, bricht bei Upstream-Änderungen laut ab | bei Konflikt manuelle Portierung (dokumentiert) |
