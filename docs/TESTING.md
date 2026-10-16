# Tests

Es gibt drei Testebenen:

1. **Runtime-Unit- und Integrationstests** (`runtime/runtime-core`, JUnit 5) – plattformneutral,
   mit echten, zur Testzeit kompilierten Modul-JARs, echten Class-Loadern und einem
   simulierten Server-Hauptthread (`TestPlatform`). Lokal und in CI (JDK 21) ausgeführt.
2. **Paper-Build je Version** (CI) – `applyAllPatches` + `build` führt die **Paper-eigene
   Testsuite** gegen den Lemon-Server (inkl. kompilierter Runtime) aus; zusätzlich werden
   Paper-Adapter und Beispielmodul gegen die öffentliche Paper-API jeder Version kompiliert.
3. **Server-Tests** (CI, `scripts/ci/smoke-test.sh`) – der gebaute Paperclip-Server wird
   echt gestartet, über die Konsole bedient und gestoppt.

## Zuordnung der geforderten Szenarien

| Szenario | Test | Ebene |
|---|---|---|
| Hot-Deployment deaktiviert | `BootstrapTest.disabledByDefaultAndCompletelyInactive`, `invalidConfigurationKeepsTheRuntimeDisabled`; Server-Test 1 (`smoke-disabled.txt`) | 1, 3 |
| Gültiges Modul laden | `ModuleLifecycleIntegrationTest.validModuleGoesThroughTheWholeLifecycleAndReleasesEverything`; Server-Test 2 | 1, 3 |
| Ungültiges Modul ablehnen | `invalidModulesAreRejectedWithReasons`, `DescriptorParserTest.*` | 1 |
| Inkompatible API-Version ablehnen | `incompatibleApiAndMinecraftVersionsAreRejected` | 1 |
| Fehlende Abhängigkeit erkennen | `dependenciesAreResolvedAndProtected` | 1 |
| Doppelte Modul-ID verhindern | `duplicateModuleIdsAreRefused` | 1 |
| Modul aktivieren und deaktivieren | `validModuleGoes…`; Server-Test 2 | 1, 3 |
| Wiederholte Aktivierung verhindern | `repeatedAndConcurrentActivationIsPrevented`, `LifecycleUnitTest.onlyDocumentedTransitionsAreAllowed` | 1 |
| Modulwechsel bei fehlgeschlagener Initialisierung | `hotSwapRollsBackWhenTheNewVersionFailsAndSwitchesWhenItWorks`, `hotSwapKeepsTheOldVersionUntouchedWhenTheNewOneCannotLoad` | 1 |
| Listener- und Task-Bereinigung | `validModuleGoes…` (Listener/Tasks/Befehle/Ressourcen = 0 nach Disable), `failingEnableReleasesPartialRegistrations…`, `ResourceTrackerTest.*`; Server-Test 2 (`/greet` antwortet nach `disable` nicht mehr: `@count 2`) | 1, 3 |
| Modulfehler ohne Absturz | `failingEnableReleasesPartialRegistrationsAndServerKeepsRunning` (Exception in `onEnable`, Konstruktor, statischem Initialisierer, `onLoad`) | 1 |
| Neustartverhalten und Persistenz | `runtimeActivationsAreRestoredAfterRestartOnlyForUnchangedJars`, `withoutRuntimeDeployOnlyStartupModulesAreActivated`, `shutdownStopsModulesInReverseDependencyOrder`; Server-Test 3 (`smoke-persist-*.txt`) | 1, 3 |
| Berechtigungsprüfung der Befehle | `AdminCommandTest.permissionsAreCheckedForEverySubcommand`, `tabCompletionRespectsPermissions` | 1 |
| Manipulierte Modulpfade | `manipulatedIdsAndPathsAreRejected`, `SecurityTest.*`, `AdminCommandTest.argumentsAreValidated` | 1 |
| Korrekte Versionserkennung | `VersionTest.*`, `versionPatternsFollowTheRunningServer`; Server-Test 2 (`fork version`) | 1, 3 |
| Nicht verfügbarer optionaler Netzwerkadapter | `optionalNetworkAdapterMayBeAbsent` | 1 |
| Integrität (Sidecar, Allow-List, TOCTOU) | `integrityChecksRejectTamperedOrUnlistedJars`, `stagedCopyIsolatesTheRunningModuleFromFileChanges`, `SecurityTest.sidecarAndAllowListAreEnforced` | 1 |
| Atomare Konfiguration mit Fallback | `configurationReloadIsAtomicAndFallsBackToTheLastValidConfig`; Server-Test 2 (`fork module config`) | 1, 3 |
| Class-Loader-Isolation / Entladbarkeit | `runtimeInternalsAreHiddenFromModules`, `unloadedClassLoadersBecomeCollectable` | 1 |
| Laufende Async-Arbeit blockiert Entladen | `runningAsyncWorkPreventsUnloadUntilItFinishes` | 1 |
| Audit-Log nicht fälschbar | `SecurityTest.auditLogCannotBeForgedWithLineBreaks` | 1 |
| Doku-Beispiele gültig | `DescriptorParserTest.documentationExampleDescriptorIsValid`, `exampleModuleDescriptorIsValid`, `RuntimeConfigLoaderTest.documentationExampleConfigIsValid` | 1 |
| Standard-Paper-Verhalten | Paper-Testsuite im Fork-Build jeder Version | 2 |

## Lokal ausgeführt (diese Arbeitssitzung)

```
cd runtime && ./gradlew :runtime-core:test        # OpenJDK 21.0.12 (Ubuntu), Linux, Tests mit -javaagent
runtime-core: 93 tests, 0 failures, 0 errors, 0 skipped
```

(Stand nach den Live-Update-Arbeiten; der Referenzlauf #8 unten zählte noch 53 Tests.)

Paper-Builds und Server-Tests konnten lokal nicht ausgeführt werden (Netzwerk, siehe
`docs/ANALYSIS.md`); ihre Ergebnisse stammen aus GitHub Actions (unten).

## Ergebnisse aus GitHub Actions

Referenzlauf **#8** (Run-ID `38412839373`, Commit `4eff375`), Runner `ubuntu-latest`, alle 9 Jobs
erfolgreich. Werte sind aus den Job-Logs abgelesen, nicht geschätzt.

| Prüfung | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| Runtime-Kern (JDK 21, versionsunabhängig) | 53 Tests, 0 Fehler | ← | ← | ← |
| Adapter + Beispielmodul gegen Paper-API kompiliert (`-Werror`) | ✔ | ✔ | ✔ | ✔ |
| `applyAllPatches` (Paper + 3 Lemon-Patches) | ✔ | ✔ | ✔ | ✔ |
| `build` inkl. Paper-Testsuite (API + Server, mit kompilierter Runtime) | 9436 Tests, 0 Fehler, 25 übersprungen | 9512 Tests, 0 Fehler, 24 übersprungen | 9764 Tests, 0 Fehler, 24 übersprungen | 10548 Tests, 0 Fehler, 88 übersprungen |
| Paperclip-JAR | ✔ (`createMojmapPaperclipJar`) | ✔ | ✔ | ✔ |
| Server-Test 1 (Runtime aus) | ✔ | ✔ | ✔ | ✔ |
| Server-Test 2 (Lebenszyklus, Konfig-Reload, Hot-Swap 1.0.0 → 1.0.1, Befehl nach `disable` entfernt) | ✔ | ✔ | ✔ | ✔ |
| Server-Test 3 (Neustart-Persistenz, Paper-`/reload` mit aktivem Modul) | ✔ | ✔ | ✔ | ✔ |
| Assertions der Server-Tests (`@expect`/`@reject`/`@count`) | 32/32 | 32/32 | 32/32 | 32/32 |

Auszug aus dem Server-Log (26.2, Server-Test 2), wie er im Job-Log steht:

```
[example-greeter] Example greeter enabled on Lemon 26.2
OK enable example-greeter: example-greeter@1.0.0 enabled (164 ms)
  defined 2 classes off the main thread
Hello CONSOLE!
  Tracked resources: {task=1, command=1, listener=1}
OK reload example-greeter: switched example-greeter@1.0.0 -> example-greeter@1.0.1 (154 ms)
OK disable example-greeter: example-greeter@1.0.1 disabled (51 ms)
Unknown or incomplete command. See below for error      (/greet nach disable)
OK unload example-greeter: example-greeter@1.0.1 unloaded (class loader closed; …)
Unloaded 1.0.1 at …: class loader still reachable (not yet collected or leaked)
```

Die letzte Zeile ist erwartbar: unmittelbar nach dem Entladen hat noch keine Garbage Collection
stattgefunden. Dass entladene Class-Loader tatsächlich freigegeben werden, ist nur in Ebene 1
(`unloadedClassLoadersBecomeCollectable`, mit `System.gc()`) nachgewiesen, **nicht** auf dem
echten Server.

Frühere Läufe: #6 (`38408355569`) – alle Builds + Paper-Testsuiten grün, Server-Test 2 überall
rot, weil die Konsole `lemon.runtime.module.manage` nicht hatte (behoben in `b635b6b`). #7
(`38410473981`) – 1.21.11, 26.1.2, 26.3 komplett grün; 26.2 scheiterte einmal innerhalb von
paperweight (`runLemonSetup`: `NoSuchFileException …/.git/objects/18`) bei unveränderten
Eingaben; seitdem deaktiviert die CI Gits automatische Speicherbereinigung, Lauf #8 war grün.

### Server-Starttests (Lauf #8)

Wanduhrzeit = Prozessstart bis `Done`-Zeile (0,2 s Abfrageintervall); `Done (…)` = Papers eigene
Angabe. Jeweils **eine** Messung auf einer geteilten CI-VM; Tests 1, 2 und 3a mit frischer Welt,
Test 3b als Neustart mit vorhandener Welt.

| Version | Test 1: Runtime aus | Test 2: Runtime an, Modul installiert, nicht aktiv | Test 3a: Runtime an | Test 3b: Neustart, Modul wird automatisch aktiviert |
|---|---|---|---|---|
| 1.21.11 | 29471 ms / `Done (25.542s)` | 26555 ms / `Done (22.692s)` | 27100 ms / `Done (22.908s)` | 12516 ms / `Done (11.838s)` |
| 26.1.2 | 15473 ms / `Done (12.844s)` | 14042 ms / `Done (11.431s)` | 13639 ms / `Done (11.005s)` | 6339 ms / `Done (5.949s)` |
| 26.2 | 23621 ms / `Done (18.709s)` | 23219 ms / `Done (18.220s)` | 22216 ms / `Done (17.360s)` | 10033 ms / `Done (9.291s)` |
| 26.3 | 21322 ms / `Done (16.761s)` | 19755 ms / `Done (15.366s)` | 18682 ms / `Done (14.378s)` | 11332 ms / `Done (10.780s)` |

Dieselbe Version schwankt zwischen Läufen um mehrere Sekunden (1.21.11 Test 1: 21074 ms in
Lauf #7, 29471 ms in Lauf #8). Die Unterschiede zwischen „aus“ und „an“ liegen innerhalb dieser
Streuung; ein Startzeit-Overhead der Runtime ist mit diesen Einzelmessungen **weder
nachgewiesen noch ausgeschlossen**. Gemessene Laufzeiten einzelner Operationen auf dem Server
(26.2): `enable` 164 ms, `reload` (Hot-Swap) 154 ms, `disable` 51 ms, `unload` 51 ms – jeweils
inklusive Worker-Phase (I/O, Hash, Klassendefinition). Davon lief auf dem Hauptthread laut
`/fork module diagnostics` der Enable-Schritt (Plattform-Bindung + `onEnable`) mit 9 ms.

## Live-Updates: Core-Patches, Plugins, Module (CI-Lauf #11)

Referenz: Lauf **#11** (Run-ID `38581855422`, Commit `4dcd97f`). Werte aus den Job-Logs
abgelesen. Ebenen: (1) Unit-/Integrationstests mit echtem Agent (`-javaagent`, echte
`redefineClasses`-Aufrufe), (3) Server-Tests 4–6 auf dem gebauten Paperclip-Server.

**Kein Test lief auf dem Ubuntu-24.04-/Pterodactyl-Server des Nutzers** – dazu gab es in
dieser Arbeit keinen Zugang. Ein Spieler-Login (echter Client) wurde nirgends getestet.

| Prüfung | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| Unit-Tests `runtime-core`, Temurin 21.0.12 | 93/93 ✔ | ← | ← | ← |
| Unit-Tests auf JBR 21.0.11 (b1163.116) und 25.0.4.1 (b635.72) mit `-XX:+AllowEnhancedClassRedefinition` | 93/93 ✔ (beide) | ← | ← | ← |
| Test-Plugin + Paper-Adapter gegen Paper-API kompiliert | ✔ | ✔ | ✔ | ✔ |
| Patch aus geänderter Paper-Quelle gebaut (`livePatchBaseline` + `createLivePatch`, genau 1 Klasse) | ✔ | ✔ | ✔ | ✔ |
| Server-Test 4: Core-Patch, Temurin, Produktionsprofil | **✘ siehe unten** | ✔ | ✔ | ✔ |
| Server-Test 5: Plugin-Updates | nicht ausgeführt (nach Test 4 übersprungen) | ✔ | ✔ | ✔ |
| Server-Test 6: struktureller Patch auf JBR 25 bzw. 21 | nicht ausgeführt | ✔ (JBR 25) | ✔ (JBR 25) | ✔ (JBR 25) |
| Hauptthread-Dauer `redefineClasses`, 1 Klasse, Temurin (Anwenden / Rollback) | – | 48,8 / 51,8 ms | 64,2 / 62,5 ms | 52,0 / 52,4 ms |
| dasselbe, struktureller Patch auf JBR 25 | – | 277,9 / 268,6 ms | 321,4 / 290,7 ms | 436,1 / 414,5 ms |

**1.21.11, Server-Test 4:** Lemon lehnte jeden Patch korrekt ab, bevor etwas geändert wurde
(„patch was built against 06a44802b59c but the server runs e0a076b9c2e0“): Die
1.21.11-Paperclip-JAR baut die Server-JAR beim ersten Start aus der Vanilla-JAR und
Binär-Patches; deren `TicksPerSecondCommand` hat nicht denselben Bytecode wie die
Gradle-`jar`-Ausgabe, die `livePatchBaseline` kopiert. Für 1.21.11 ist der dokumentierte
Gradle-Weg damit **nicht** funktionsfähig; Ursache und Abhilfe: siehe Abschnitt unten,
sobald die Diagnose (Lauf #12) ausgewertet ist.

### Pflichttests Core-Patches

Server = Server-Test 4/6 (`scripts/ci/smoke-livepatch*.txt`), Unit =
`LivePatchIntegrationTest`/`PreVerifierTest`/`ClassDiffTest`/`PatchArchiveTest`.
„Neustart nötig“ bezieht sich auf das Testergebnis.

| # | Test | Vorbedingungen | Schritte | Erwartet | Tatsächlich | Neustart nötig | Grenzen |
|---|---|---|---|---|---|---|---|
| 1 | Methodenrumpf ändern | Agent, `live-patching.enabled`, Freigabeliste | Literal in `TicksPerSecondCommand.execute` geändert → `createLivePatch` → `update` + `confirm` | `OK patch tps-text applied` | Server (26.x): angewendet, 1 Klasse, 49–64 ms; Unit: ✔ | nein | Verhalten wird nicht validiert |
| 2 | bereits geladene Paper-Klasse | wie 1 | `/tps` vor dem Patch ausgeführt (Klasse geladen und initialisiert) | Redefinition einer geladenen Klasse | Server (26.x): ✔ | nein | – |
| 3 | Methode hinzufügen, Standard-JVM | Temurin 21/25 | `tps-structural`: `check`, `update` | Ablehnung vor jeder Änderung | Server (26.x): `reject …`, „this JVM cannot add methods/constructors“, `NOT DONE`; 1.21.11: ebenfalls abgelehnt, dort aber wegen der Bytecode-Abweichung (s. o.); Probe „no add methods“ in allen 4 Versionen | ja (neue JAR) | – |
| 4 | dasselbe mit erweiterter JVM | JBR 25.0.4.1 + Flag | `tps-structural` anwenden, `/tps`, Rollback | angewendet, neuer Code läuft | Server (26.x, JBR 25): `TPS (structural) …` einmal, Rollback ✔; Unit auf JBR 21+25 ✔. Probe: Oberklassenwechsel auch auf JBR **nicht** möglich | nein | 270–440 ms Pause; JBR 21 nur in Unit-Tests |
| 5 | ungültige Klassendatei | – | `tps-garbage` (Klasse durch Text ersetzt, Manifest-Digest passend) | Ablehnung | Server (alle 4): `NOT DONE: class … is not a valid class file: not a class file (bad magic number)`; Unit ✔ | – | – |
| 6 | falsche Prüfsumme / Build-ID | – | `tps-bad-checksum` (falsche `.sha256`), `tps-wrong-build` | Ablehnung | Server (alle 4): `SHA-256 mismatch`, `wrong build id`; Unit ✔ | – | – |
| 7 | Multi-Klassen-Patch, eine Klasse nicht passend | – | `tps-mixed`: TPS + `RestartCommand` mit falschem Basis-Digest | ganzer Patch abgelehnt | Server (alle 4): `NOT DONE patch tps-mixed cannot be applied`, `/tps` unverändert | – | – |
| 8 | Fehler während der Anwendung + Diagnose | Standard-JVM | `tps-atomic`: TPS + `RestartCommand` mit `max_stack 0` → `confirm` | JVM lehnt ab, keine Klasse geändert, Meldung + Audit | Server (26.x): `FAILED the JVM rejected the redefinition: java.lang.VerifyError …`, `/tps` danach unverändert, Audit `FAILED`, Historie `apply FAILED`. Auf JBR lehnt Lemons Vorab-Verifikation schon im Plan ab (Unit auf JBR ✔) | nein | – |
| 9 | Serverprozess / Spielerverbindung bleiben | – | nach dem Patch Server-List-Ping (`scripts/ci/slp.py`), weitere Befehle, sauberer Stopp | Server antwortet | Server (26.x): `SLP ok … ping 0.7–1.9 ms`, Stopp normal | – | **kein Spieler-Login getestet** |
| 10 | geänderte Methode führt neuen Code aus | wie 1 | `/tps` nach dem Patch | neuer Text | Server (26.x): `TPS (live patch) from last …` zweimal, danach Rollback | – | – |
| 11 | Logs, Tick, Tasks, Befehle, Speicher | wie 1 | `/tps` (Tick-Rate), `patch status` (Heap), weitere `/fork`-Befehle über den Scheduler | normal | Server (26.x): TPS 20.0 nach dem Patch, Befehle/Scheduler funktionieren, Heap angezeigt, keine `Unhandled exception` | – | keine Lastmessung, kein Langzeittest |
| 12 | vorherigen Bytecode wiederherstellen | Patch aktiv | Sicherung geprüft (`backup.txt` enthält den SHA-256 der Basis-Klasse), `rollback` + `confirm`, `/tps` | Originaltext | Server (26.x): Sicherung passt, `OK patch tps-text rolled back`, Originaltext, `Active patches: 0` | nein | nur Bytecode; Spielzustand nicht |

### Plugin-Live-Updates (Server-Test 5, `scripts/ci/smoke-plugins.txt`, 26.1.2/26.2/26.3 ✔)

| Fall | Erwartet | Tatsächlich (26.2, gleichlautend in 26.1.2 und 26.3) |
|---|---|---|
| Keine Plugin-JAR (`NotAPlugin.jar`) | „incompatible“ | `plugin NotAPlugin.jar: incompatible` |
| Neuere API (`api-version 99.0`) | „incompatible“, kein Laden | `plugin needs api-version 99.0 but the server runs 26.2`, `NOT DONE …` |
| Update 1.0.0 → 1.0.1 | live, Task/Listener sauber übergeben | `OK update of LemonTestPlugin done: 1.0.0 -> 1.0.1`; `/lemontest`: 1 Task, 1 Listener |
| Update auf Version, die in `onEnable` wirft | alte Version zurück | `FAILED … the previous version 1.0.1 was restored and is enabled`; `/lemontest` antwortet von 1.0.1 |
| Rollback | vorherige JAR (fehlgeschlagene Versuche ausgenommen) | `OK rollback of LemonTestPlugin done: 1.0.1 -> 1.0.0` |
| Neues Plugin mit `depend` | live installiert | `OK install of LemonTestAddon done: - -> 1.0.0` |
| Update eines Plugins mit aktivem Abhängigen | Neustart nötig | `restart only (used by [LemonTestAddon])`, „enabled plugins depend on LemonTestPlugin“ |
| Audit/Historie | Einträge | `plugin-update … OK`, `… FAILED`, `plugin-rollback … OK`, `plugin-install … OK` |

Nicht getestet: Plugins mit eigenen Threads/Datenbankverbindungen, Paper-Plugins (nur die
Ablehnungsregel ist unit-getestet), Spieler mit offenen Plugin-Inventaren, 1.21.11.

### Module (Task „Live-Update-System“)

Lebenszyklus, Hot-Swap und Neustart-Persistenz: Server-Tests 2 und 3 in allen vier
Versionen ✔ (Lauf #11); `module.yml` mit Standardwerten und `fork module remove`:
Unit-Tests (`ModuleSimplicityTest`).

## Noch nicht automatisiert

| Test | Grund |
|---|---|
| Echte Spieler-Events (Join-Nachricht des Beispielmoduls, Inventar-GUIs) auf dem Server | erfordert einen Test-Client/Bot; derzeit nur mit simuliertem Event-Bus (`TestPlatform`) getestet |
| Rollback auf dem echten Server mit fehlerhafter zweiter Version | nur in Ebene 1 getestet; Server-Test 2 testet den erfolgreichen Wechsel 1.0.0 → 1.0.1 |
| Freigabe entladener Class-Loader auf dem echten Server | nur in Ebene 1 (mit `System.gc()`); auf dem Server meldet die Diagnose direkt nach dem Entladen erwartungsgemäß „still reachable“ |
| Lasttests / MSPT, Langzeit-Speicherverhalten | keine reproduzierbare Lastumgebung |
| Kombination mit Velocity/BungeeCord, ViaVersion, Geyser/Floodgate, LuckPerms | nicht getestet (`docs/COMPATIBILITY.md`) |
| Windows/macOS | CI läuft nur auf Linux |
