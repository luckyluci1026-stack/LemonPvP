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

Siehe Abschnitt am Ende dieser Datei (wird mit Lauf-ID gepflegt).

## Noch nicht automatisiert

| Test | Grund |
|---|---|
| Echte Spieler-Events (Join-Nachricht des Beispielmoduls, Inventar-GUIs) auf dem Server | erfordert einen Test-Client/Bot; derzeit nur mit simuliertem Event-Bus (`TestPlatform`) getestet |
| `/reload` von Paper mit aktiven Modulen | nicht in den Server-Tests enthalten |
| Hot-Swap und Rollback auf dem echten Server mit fehlerhafter zweiter Version | nur in Ebene 1 getestet; Server-Test 2 testet `reload` mit derselben Version |
| Lasttests / MSPT, Langzeit-Speicherverhalten | keine reproduzierbare Lastumgebung |
| Kombination mit Velocity/BungeeCord, ViaVersion, Geyser/Floodgate, LuckPerms | nicht getestet (`docs/COMPATIBILITY.md`) |
| Windows/macOS | CI läuft nur auf Linux |
