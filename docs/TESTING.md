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

Lauf **#7** (Run-ID `38410473981`, Commit `b635b6b`), Runner `ubuntu-latest`. Werte sind aus den
Job-Logs abgelesen, nicht geschätzt.

| Prüfung | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| Runtime-Kern (JDK 21) | 53 Tests, 0 Fehler (versionsunabhängig) | ← | ← | ← |
| Adapter + Beispielmodul gegen Paper-API kompiliert | ✔ | ✔ | ✔ | ✔ |
| `applyAllPatches` | ✔ | ✔ | ✘ ¹ | ✔ |
| `build` inkl. Paper-Testsuite (API + Server) | 9436 Tests, 0 Fehler, 24 übersprungen | 9512 Tests, 0 Fehler, 24 übersprungen | – ¹ | 10548 Tests, 0 Fehler, 88 übersprungen |
| Paperclip-JAR | ✔ (`createMojmapPaperclipJar`) | ✔ | – ¹ | ✔ |
| Server-Test 1 (Runtime aus) | ✔ | ✔ | – ¹ | ✔ |
| Server-Test 2 (Lebenszyklus, Hot-Swap 1.0.0 → 1.0.1, Befehl nach `disable` entfernt) | ✔ | ✔ | – ¹ | ✔ |
| Server-Test 3 (Neustart-Persistenz, Paper-`/reload` mit aktivem Modul) | ✔ | ✔ | – ¹ | ✔ |

¹ 26.2 scheiterte in Lauf #7 innerhalb von paperweight (`runLemonSetup`:
`NoSuchFileException …/.git/objects/18`), bevor Lemon-Code gebaut wurde; die Dateien unter
`versions/26.2` waren gegenüber Lauf #6 unverändert, in dem derselbe Schritt, der Build mit
Paper-Testsuite, die Paperclip-JAR und Server-Test 1 erfolgreich waren. Ursache vermutlich ein
Hintergrund-`git gc`; die CI deaktiviert das seitdem. Bestätigung steht im nächsten Lauf aus.

Lauf #6 (Run-ID `38408355569`, Commit `282b208`) hatte für **alle vier** Versionen Patches,
Build mit Paper-Testsuite, Paperclip und Server-Test 1 erfolgreich; Server-Test 2 schlug dort
überall fehl, weil die Konsole `lemon.runtime.module.manage` nicht hatte (behoben in `b635b6b`).

### Server-Starttests (Lauf #7)

Wanduhrzeit = Prozessstart bis `Done`-Zeile (0,2 s Abfrageintervall); `Done (…)` = Papers eigene
Angabe. Jeweils **eine** Messung auf einer geteilten CI-VM, frische Welt außer beim Neustart.

| Version | Runtime aus (Test 1) | Runtime an, 1 Modul installiert (Test 2) | Neustart mit wiederhergestelltem Modul (Test 3, Teil 2, Welt vorhanden) |
|---|---|---|---|
| 1.21.11 | 21074 ms / `Done (17.978s)` | 19462 ms / `Done (16.521s)` | 9195 ms / `Done (8.831s)` |
| 26.1.2 | 18330 ms / `Done (15.445s)` | 17725 ms / `Done (14.944s)` | 7773 ms / `Done (7.321s)` |
| 26.3 | 20669 ms / `Done (16.706s)` | 17604 ms / `Done (13.925s)` | 10288 ms / `Done (9.661s)` |

Die Unterschiede zwischen „aus“ und „an“ liegen innerhalb der Streuung (Test 2 startete jeweils
nach Test 1 auf demselben Runner, mit warmem Dateisystem-Cache). Ein Startzeit-Overhead der
Runtime ist mit diesen Einzelmessungen **weder nachgewiesen noch ausgeschlossen**.

## Noch nicht automatisiert

| Test | Grund |
|---|---|
| Echte Spieler-Events (Join-Nachricht des Beispielmoduls, Inventar-GUIs) auf dem Server | erfordert einen Test-Client/Bot; derzeit nur mit simuliertem Event-Bus (`TestPlatform`) getestet |
| Rollback auf dem echten Server mit fehlerhafter zweiter Version | nur in Ebene 1 getestet; Server-Test 2 testet den erfolgreichen Wechsel 1.0.0 → 1.0.1 |
| Lasttests / MSPT, Langzeit-Speicherverhalten | keine reproduzierbare Lastumgebung |
| Kombination mit Velocity/BungeeCord, ViaVersion, Geyser/Floodgate, LuckPerms | nicht getestet (`docs/COMPATIBILITY.md`) |
| Windows/macOS | CI läuft nur auf Linux |
