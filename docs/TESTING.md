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
cd runtime && ./gradlew :runtime-core:test        # JDK 21.0.12, Linux
runtime-core: 53 tests, 0 failures, 0 errors, 0 skipped
```

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

## Noch nicht automatisiert

| Test | Grund |
|---|---|
| Echte Spieler-Events (Join-Nachricht des Beispielmoduls, Inventar-GUIs) auf dem Server | erfordert einen Test-Client/Bot; derzeit nur mit simuliertem Event-Bus (`TestPlatform`) getestet |
| Rollback auf dem echten Server mit fehlerhafter zweiter Version | nur in Ebene 1 getestet; Server-Test 2 testet den erfolgreichen Wechsel 1.0.0 → 1.0.1 |
| Freigabe entladener Class-Loader auf dem echten Server | nur in Ebene 1 (mit `System.gc()`); auf dem Server meldet die Diagnose direkt nach dem Entladen erwartungsgemäß „still reachable“ |
| Lasttests / MSPT, Langzeit-Speicherverhalten | keine reproduzierbare Lastumgebung |
| Kombination mit Velocity/BungeeCord, ViaVersion, Geyser/Floodgate, LuckPerms | nicht getestet (`docs/COMPATIBILITY.md`) |
| Windows/macOS | CI läuft nur auf Linux |
