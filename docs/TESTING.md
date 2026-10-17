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
runtime-core: 102 tests, 0 failures, 0 errors, 0 skipped
```

(Stand nach den Erweiterungen Rücklesen/Plugin-Ressourcen/Offline-Prüfung; der Referenzlauf
#8 unten zählte noch 53 Tests. In CI: Temurin 21 sowie JBR 21 und 25 mit
`-XX:+AllowEnhancedClassRedefinition`, Läufe #17 und #18 grün.)

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

## Live-Updates: Core-Patches, Plugins, Module (CI-Läufe #11 und #13)

Referenz: Lauf **#11** (Run-ID `38581855422`, Commit `4dcd97f`) für 26.1.2/26.2/26.3 und Lauf
**#13** (Run-ID `38857590269`, Commit `272e8df`, alle 11 Jobs grün) für 1.21.11. Werte aus den
Job-Logs abgelesen. Ebenen: (1) Unit-/Integrationstests mit echtem Agent (`-javaagent`, echte
`redefineClasses`-Aufrufe), (3) Server-Tests 4–6 auf dem gebauten Paperclip-Server.

**Kein Test lief auf dem Ubuntu-24.04-/Pterodactyl-Server des Nutzers** – dazu gab es in
dieser Arbeit keinen Zugang. Ein Spieler-Login (echter Client) wurde nirgends getestet.

| Prüfung | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| Unit-Tests `runtime-core`, Temurin 21.0.12 (Lauf #11) | 93/93 ✔ | ← | ← | ← |
| Unit-Tests auf JBR 21.0.11 (b1163.116) und 25.0.4.1 (b635.72) mit `-XX:+AllowEnhancedClassRedefinition` (Lauf #11) | 93/93 ✔ (beide) | ← | ← | ← |
| Test-Plugin + Paper-Adapter gegen Paper-API kompiliert | ✔ | ✔ | ✔ | ✔ |
| Patch aus geänderter Paper-Quelle gebaut (genau 1 Klasse) | ✔ (#13, mit `-PserverJar`) | ✔ | ✔ | ✔ |
| Server-Test 4: Core-Patch, Temurin, Produktionsprofil | ✔ (#13) | ✔ | ✔ | ✔ |
| Server-Test 5: Plugin-Updates | ✔ (#12, #13) | ✔ | ✔ | ✔ |
| Server-Test 6: struktureller Patch auf JBR | ✔ (#13, JBR 21) | ✔ (JBR 25) | ✔ (JBR 25) | ✔ (JBR 25) |
| Hauptthread-Dauer `redefineClasses`, 1 Klasse, Temurin (Anwenden / Rollback) | 52,4 / 51,4 ms | 48,8 / 51,8 ms | 64,2 / 62,5 ms | 52,0 / 52,4 ms |
| dasselbe, struktureller Patch auf JBR | 233,7 / 205,9 ms (JBR 21) | 277,9 / 268,6 ms | 321,4 / 290,7 ms | 436,1 / 414,5 ms |

**1.21.11 – Befund und Korrektur:** In Lauf #11/#12 lehnte Lemon auf 1.21.11 jeden Patch
korrekt ab, bevor etwas geändert wurde („patch was built against 06a44802b59c but the server
runs e0a076b9c2e0“). Die Diagnose (Lauf #12) zeigte: Die 1.21.11-Paperclip-JAR erzeugt die
Server-JAR beim Start aus Vanilla-JAR + Binär-Patches, und darin sind 9962 von 10183 Klassen
byte-verschieden von der Gradle-JAR – gleiche Größe, nur der Konstantenpool ist anders
angeordnet. Bei 26.1.2/26.2/26.3 sind beide identisch (10378/10554/10928 Klassen). Seit
`272e8df` baut das Patch-Werkzeug gegen die laufende JAR (`--base` / `-PserverJar`) und
erkennt Änderungen gegen die Gradle-JAR (`--reference`); Lauf #13 bestätigt das auf
1.21.11 mit allen Assertions von Server-Test 4–6 (0 Fehler).

### Pflichttests Core-Patches

Server = Server-Test 4/6 (`scripts/ci/smoke-livepatch*.txt`), Unit =
`LivePatchIntegrationTest`/`PreVerifierTest`/`ClassDiffTest`/`PatchArchiveTest`.
„Neustart nötig“ bezieht sich auf das Testergebnis.

| # | Test | Vorbedingungen | Schritte | Erwartet | Tatsächlich | Neustart nötig | Grenzen |
|---|---|---|---|---|---|---|---|
| 1 | Methodenrumpf ändern | Agent, `live-patching.enabled`, Freigabeliste | Literal in `TicksPerSecondCommand.execute` geändert → `createLivePatch` → `update` + `confirm` | `OK patch tps-text applied` | Server (alle 4): angewendet, 1 Klasse, 49–64 ms; Unit: ✔ | nein | Verhalten wird nicht validiert |
| 2 | bereits geladene Paper-Klasse | wie 1 | `/tps` vor dem Patch ausgeführt (Klasse geladen und initialisiert) | Redefinition einer geladenen Klasse | Server (alle 4): ✔ | nein | – |
| 3 | Methode hinzufügen, Standard-JVM | Temurin 21/25 | `tps-structural`: `check`, `update` | Ablehnung vor jeder Änderung | Server (alle 4, 1.21.11 ab Lauf #13): `reject …`, „this JVM cannot add methods/constructors“, `NOT DONE`; Probe „no add methods“ | ja (neue JAR) | – |
| 4 | dasselbe mit erweiterter JVM | JBR 21.0.11 bzw. 25.0.4.1 + `-XX:+AllowEnhancedClassRedefinition` | `tps-structural` anwenden, `/tps`, Rollback | angewendet, neuer Code läuft | Server (26.x auf JBR 25, 1.21.11 auf JBR 21): `TPS (structural) …` einmal, Rollback ✔; Unit auf JBR 21+25 ✔. Probe: Oberklassenwechsel auch auf JBR **nicht** möglich | nein | 200–440 ms Pause |
| 5 | ungültige Klassendatei | – | `tps-garbage` (Klasse durch Text ersetzt, Manifest-Digest passend) | Ablehnung | Server (alle 4): `NOT DONE: class … is not a valid class file: not a class file (bad magic number)`; Unit ✔ | – | – |
| 6 | falsche Prüfsumme / Build-ID | – | `tps-bad-checksum` (falsche `.sha256`), `tps-wrong-build` | Ablehnung | Server (alle 4): `SHA-256 mismatch`, `wrong build id`; Unit ✔ | – | – |
| 7 | Multi-Klassen-Patch, eine Klasse nicht passend | – | `tps-mixed`: TPS + `RestartCommand` mit falschem Basis-Digest | ganzer Patch abgelehnt | Server (alle 4): `NOT DONE patch tps-mixed cannot be applied`, `/tps` unverändert | – | – |
| 8 | Fehler während der Anwendung + Diagnose | Standard-JVM | `tps-atomic`: TPS + `RestartCommand` mit `max_stack 0` → `confirm` | JVM lehnt ab, keine Klasse geändert, Meldung + Audit | Server (alle 4): `FAILED the JVM rejected the redefinition: java.lang.VerifyError …`, `/tps` danach unverändert, Audit `FAILED`, Historie `apply FAILED`. Auf JBR lehnt Lemons Vorab-Verifikation schon im Plan ab (Unit auf JBR ✔) | nein | – |
| 9 | Serverprozess / Spielerverbindung bleiben | – | nach dem Patch Server-List-Ping (`scripts/ci/slp.py`), weitere Befehle, sauberer Stopp | Server antwortet | Server (alle 4): `SLP ok … ping 0.7–2.3 ms`, Stopp normal | – | **kein Spieler-Login getestet** |
| 10 | geänderte Methode führt neuen Code aus | wie 1 | `/tps` nach dem Patch | neuer Text | Server (alle 4): `TPS (live patch) from last …` zweimal, danach Rollback | – | – |
| 11 | Logs, Tick, Tasks, Befehle, Speicher | wie 1 | `/tps` (Tick-Rate), `patch status` (Heap), weitere `/fork`-Befehle über den Scheduler | normal | Server (alle 4): TPS 20.0 nach dem Patch, Befehle/Scheduler funktionieren, Heap angezeigt, keine `Unhandled exception` | – | keine Lastmessung, kein Langzeittest |
| 12 | vorherigen Bytecode wiederherstellen | Patch aktiv | Sicherung geprüft (`backup.txt` enthält den SHA-256 der Basis-Klasse), `rollback` + `confirm`, `/tps` | Originaltext | Server (alle 4): Sicherung passt, `OK patch tps-text rolled back`, Originaltext, `Active patches: 0` | nein | nur Bytecode; Spielzustand nicht |

### Plugin-Live-Updates (Server-Test 5, `scripts/ci/smoke-plugins.txt`, alle 4 Versionen ✔)

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
Ablehnungsregel ist unit-getestet), Spieler mit offenen Plugin-Inventaren.

### Module (Task „Live-Update-System“)

Lebenszyklus, Hot-Swap und Neustart-Persistenz: Server-Tests 2 und 3 in allen vier
Versionen ✔ (Läufe #11–#13); `module.yml` mit Standardwerten, `fork module remove` und die
an den geprüften SHA-256 gebundene Bestätigung von `fork live update <modul>`: Unit-Tests
(`ModuleSimplicityTest`). `fork live update <modul>` selbst lief nicht auf einem Server.

### Rücklesen, Plugin-Ressourcen, Offline-Prüfung, neue Befehle (Läufe #17 `6079a06` und #18 `6f3959e`)

Beide Läufe: alle 11 Jobs grün, alle vier Versionen.

| Prüfung | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| Core-Patch angewendet **und zurückgelesen** (`applied and verified`), `patch verify`, Rollback `rolled back and verified` (Temurin) | ✔ | ✔ | ✔ | ✔ |
| dasselbe für den strukturellen Patch auf JBR (`applied and verified`) | ✔ (JBR 21) | ✔ (JBR 25) | ✔ (JBR 25) | ✔ (JBR 25) |
| Lauf #18: Redefinition / Zurücklesen, 1 Klasse, Temurin | 54,0 / 49,2 ms | 71,0 / 73,0 ms | 57,4 / 56,6 ms | 51,4 / 52,4 ms |
| Lauf #18: dasselbe, struktureller Patch, JBR | 226,6 / 249,0 ms | 455,5 / 486,8 ms | 334,4 / 353,4 ms | 628,9 / 578,8 ms |
| `checkLivePatch`: Body-Patch `APPLICABLE on a standard JVM`, struktureller `APPLICABLE ONLY with enhanced…`, Patch für fremde Build-ID abgelehnt (Gradle-Fehler) | ✔ | ✔ | ✔ | ✔ |
| `createLivePatch -PdeployDir`: Patch + `.sha256` kopiert | ✔ | ✔ | ✔ | ✔ |
| `fork live inspect LemonTestPlugin`: `Registered now: 1 listener, 1 scheduler task, 1 command` | ✔ | ✔ | ✔ | ✔ |
| Update meldet `it had 1 listener, 1 scheduler task, 1 command`, **keine** Reste/Threads der alten Version (`@reject WARNING: …`) | ✔ | ✔ | ✔ | ✔ |
| `inspect` nach dem Update: `Unloaded version 1.0.0 … class loader still in memory (not collected yet …)` | ✔ | ✔ | ✔ | ✔ |
| `fork live capabilities`, `fork live diagnostics`, `fork live inspect patch:tps-text` | ✔ | ✔ | ✔ | ✔ |

Dass der Class-Loader der alten Plugin-Version später eingesammelt wird, wurde auf dem
Server nicht beobachtet (Lemon löst keine GC aus, der Test endet Sekunden danach).

### Speer-Fix als echter Core-Live-Patch (1.21.11, CI-Job `spear-hotswap`)

Server **ohne** Fix gebaut und gestartet (Temurin 21, `-javaagent`, Produktionsprofil),
CombatProbe misst echte Angriffe eines serverseitigen Testspielers; der Patch
`spear-cooldown` wurde mit `createLivePatch -PserverJar=…` aus den korrigierten Quellen
gebaut (2 Klassen: `LivingEntity`, `Player`; 23 Klassen ohne Bytecode-Änderung
übersprungen), `checkLivePatch`: `APPLICABLE on a standard JVM`.

| Messung (Lauf #21) | vor dem Patch | nach dem Live-Patch | nach dem Rollback |
|---|---|---|---|
| Speer-Stich, zwei Ziele | 3,000 / **0,602** (0,20) | 3,000 / 3,000 (1,00) | 3,000 / 0,602 |
| Speer-Stich ohne Ziel, Abklingzeit 100 → | **100** (nicht zurückgesetzt) | 0 | 100 |
| Schwertschlag | 6,000, → 0, 1 Event | 6,000, → 0, 1 Event | 6,000, → 0, 1 Event |
| Swap Schwert → Speer ohne Ausrüstungs-Update | 6,000 / **1,208** | 6,000 / 6,000 | 6,000 / 1,208 |
| Swap mit Papers Ausrüstungs-Update | 0,602 / 0,602 | 0,602 / 0,602 | 0,602 / 0,602 |
| `/tps`, Server-List-Ping | – | ✔ | ✔ |

Redefinition beider Klassen auf dem Hauptthread: 200,3 ms; Rollback 66,7 ms. In Lauf #21
meldete das Zurücklesen für `Player` fälschlich eine Abweichung (`ConstantValue` eines
`final`-Instanzfelds, das die JVM ignoriert); behoben, mit einem Test, der den Fehlalarm
ohne die Korrektur reproduziert.

**Lauf #23 (`9db3eb3`, alle 12 Jobs grün):** dieselben Messwerte vor / nach / nach Rollback
wie oben; `OK patch spear-cooldown applied and verified` (beide Klassen „runs the expected
bytecode (read back from the JVM)“), `patch verify` ✔, `rolled back and verified`;
Redefinition 267,4 ms, Zurücklesen 127,8 ms, Rollback 72,3 ms; `/tps` 19,4 bzw. 19,7,
Server-List-Ping nach Patch und Rollback beantwortet. Fork-Job 1.21.11, Servertest 7: die
ausgelieferte JAR misst `VANILLA COOLDOWN, sword OK`.

### Testmodule: Grenzen, Fehler, Abhängigkeiten, Leaks (Servertest 8, Lauf #32 `d2f9503`)

Alle 12 Jobs grün. Ablauf und Module: [`TEST-MODULES.md`](TEST-MODULES.md),
`scripts/ci/smoke-test-modules.txt`. Auf jeder Version bestanden: fehlende/falsche
`.sha256` abgelehnt; Abhängigkeit, Service, Schutz vor Disable/Reload der Library,
Versionsbereich (Library 2.0.0 abgelehnt); 12× `stress all` mit 10 Hot-Swaps unter Last
(Generation am Ende 12, Version 2.0.0); alle `test-chaos`-Fehler (Rollback mit Grund,
abgebrochenes `onLoad`, abgelehnte Konfiguration, langsames `onEnable` gemeldet, werfendes
`onDisable` mit `Tracked resources: none`, `unload` bei laufender Async-Arbeit abgelehnt,
geleakter Thread gemeldet); nach dem Entladen aller Module und `jcmd GC.run`: **21 von 21**
Class-Loadern eingesammelt, keiner mehr `still in memory`.

| Messung (erster Durchlauf) | 1.21.11 | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|---|
| 10 000 Events × 2000 Listener (20 Mio. Handler-Aufrufe) | 521,6 ms (26 ns) | 355,1 ms (17 ns) | 419,1 ms (20 ns) | 364,5 ms (18 ns) |
| +20 000 Listener registrieren | 141,2 ms | 84,9 ms | 59,3 ms | 100,0 ms |
| 1000 Events × 22 000 Listener | 528,6 ms (24 ns) | 380,3 ms (17 ns) | 676,6 ms (30 ns) | 382,1 ms (17 ns) |
| 22 700 Registrierungen einzeln freigeben (`stress clear`) | 808,1 ms | 563,6 ms | 827,1 ms | 763,3 ms |
| 200 Befehle registrieren / ausführen | 7,6 / 18,9 ms | 5,0 / 6,5 ms | 4,0 / 6,8 ms | 8,5 / 17,5 ms |
| 500 Async-Tasks hin und zurück, Handles danach frei | 976,5 ms, 0 übrig | 67,9 ms, 0 übrig | 84,5 ms, 0 übrig | 98,0 ms, 0 übrig |
| 500 Timer × 40 Ticks: Läufe / erwartet, Tick-Mittel | 20 000 / 20 000, 7,64 ms | 20 000 / 20 000, 4,49 ms | 20 000 / 20 000, 5,27 ms | 20 000 / 20 000, 4,87 ms |

GitHub-Runner (geteilte VMs): Zeiten schwanken zwischen Läufen; sie zeigen Größenordnungen,
keine Benchmarks. Der Anteil der Runtime am Freigeben ist klein (Tracker: 50 000 einzelne
Freigaben in 5 ms, lokal gemessen); den Rest verbringt der Server beim Abmelden – laut Code
durchsucht `HandlerList.unregisterAll` je Listener alle Handler-Listen (nicht getrennt gemessen).

Auf dem Weg dahin (Läufe #26–#30) gefunden und behoben: ein Bukkit-Scheduler, der über den
Kopf seiner Warteschlange ein entladenes Modul festhielt (per Heap-Analyse
`scripts/ci/HeapPath.java` nachgewiesen), sowie zwei Fehler in den Tests selbst (verschachtelte
Befehle werden eingereiht; `sed | head` unter `pipefail`). Details: `TEST-MODULES.md`,
Abschnitt 7.

### LemonPvP-Suite (Servertest 9, Lauf #36 `cdcbbbe`)

Alle 12 Jobs grün. Die vier Module (`docs/PVP-SUITE.md`) werden in jedem Paper-API-Job
gegen die echte API gebaut; vorher laufen ihre 22 Unit-Tests (`:pvp-suite:collect` hängt
von `test` ab). Servertest 9 (`scripts/ci/smoke-pvp.txt`) bestand auf allen vier Versionen:
`pvp-killstreaks` ohne `pvp-core` abgelehnt; simulierte Kills mit exakt erwarteten Werten
(Alice: `Kills 5 · Tode 1 · K/D 5.00`, `Rating 1045 (#1)`; Bob `K/D 0.20`; Top-Liste);
Ansagen für 3er- und 5er-Serie, keine für 4, „Serie beendet“; abgelehnte Konfiguration
(`k-factor 500`, unbekannter Trankeffekt `flying` – geprüft gegen die Effekt-Registry des
Servers); Hot-Swap von `pvp-killstreaks` (die neue Instanz sagt die nächste Serie an);
`disable pvp-core` verweigert, solange Module es nutzen; nach Reload von `pvp-core` sind alle
4 Spieler wieder da (`stats.tsv` geprüft); nach dem Entladen und `jcmd GC.run` **6 von 6**
Class-Loadern eingesammelt. Nicht automatisiert: alles, was einen echten Spieler braucht
(Treffer, Actionbar, Titel, Sounds, Effekte, Seitenleiste).

### JVM-Instrumentation (Servertest 10, Läufe #41 `6b99bf6` und #43 `6a1d4b5`)

Lauf #43: **alle 12 Jobs grün.** Ebenen (Details: [`INSTRUMENTATION.md`](INSTRUMENTATION.md) §9):

* **Unit- und Integrationstests** (`:runtime-core:test`, Test-JVM mit echtem `-javaagent`):
  Temurin 21.0.12: 134 Tests, 0 Fehler, 1 übersprungen (der Test, der nur auf JVMs mit
  erweiterter Redefinition gilt). JBR 21.0.11 und JBR 25.0.4.1 mit
  `-XX:+AllowEnhancedClassRedefinition`: je 134 Tests, 0 Fehler, 5 übersprungen (die
  Retransformations-Tests); dort bestanden `enhancedRedefinitionJvmRefusesRetransformationPatches`
  und `DynamicAttachTest` (zwei echte JVM-Prozesse: Attach per Launcher, `agentmain`,
  Retransformation einer vorher geladenen Klasse, zweite Retransformation, Entfernen).
* **JBR-Befund** (Diagnose-Schritt `RetransformRepro.java`, je Szenario eigene JVM, in Lauf #41
  und #43 identisch): mit `-XX:+AllowEnhancedClassRedefinition` bricht `modify-unload` (Klasse
  von einem Transformer bei der Retransformation verändert, danach entladen) mit
  `free(): double free detected in tcache 2` ab (Exit 134); `recorder`, `modify`,
  `modify-then-redefine` laufen durch; ohne das Flag laufen alle vier durch. In Lauf #39/#41
  riss das die Test-JVM im GC-Test mit. Folge: Lemon verweigert Retransformations-Patches auf
  solchen JVMs (Servertest 6 prüft die Meldung „not probed: …“).
* **Live-Server-Test 10** (`scripts/ci/smoke-instrumentation.txt`, alle vier Versionen ✔ in
  Lauf #43): Server **ohne** `-javaagent` gestartet; Status und Klassensuche melden den
  fehlenden Agenten mit Abhilfe (`… attach <pid>`); `java -jar lemon-livepatch-agent.jar attach
  <pid>` → `[Lemon] live-patch agent 3 loaded (agentmain (dynamic attach))`, Lemon findet ihn
  ohne Neustart; gezählt: 28 627 (1.21.11) bis 30 116 (26.3) geladene Klassen, davon
  15 827–16 630 änderbar und 12 799–13 485 versteckt (Zählung 10–14 ms); Klassensuche
  `org.spigotmc.TicksPerSecondCommand - paper - modifiable`; Inspect mit Code-Source
  `…/versions/<mc>/lemon-<mc>.jar` und dem SHA-256 der Klassendatei, der per `@shell` mit der
  Klassendatei der laufenden JAR übereinstimmt; Probe „yes change method bodies by
  retransformation (and restore them)“; Patch `tps-text` per Retransformation:
  `OK patch tps-text applied and verified` (JVM-Retransformation 48–60 ms auf dem Hauptthread,
  Rücklesen 46–63 ms), `/tps` zeigt zweimal den neuen Text; Rücklesen per
  `inspect … verify` (selbst eine Retransformation) und `patch verify` bestätigen den Patch;
  Redefinition derselben Klasse und der strukturelle Patch per Retransformation werden
  abgelehnt; Rollback `rolled back and verified`, `/tps` wieder original; Audit-Log-Einträge
  geprüft. In Lauf #41 war nur die Erwartung an die JEP-451-Warnung falsch (keine Warnung im
  Serverlog); das Manifest der Server-JAR enthält nur `Main-Class` (Lauf #43).

## Noch nicht automatisiert

| Test | Grund |
|---|---|
| Echte Spieler-Events (Join-Nachricht des Beispielmoduls, Inventar-GUIs) auf dem Server | erfordert einen Test-Client/Bot; derzeit nur mit simuliertem Event-Bus (`TestPlatform`) getestet |
| Lasttests mit echten Spielern / MSPT unter Spielerlast, Langzeit-Speicherverhalten | keine reproduzierbare Lastumgebung; Servertest 8 misst Last durch Module (Listener, Events, Timer, Befehle, Async) ohne Spieler |
| Kombination mit Velocity/BungeeCord, ViaVersion, Geyser/Floodgate, LuckPerms | nicht getestet (`docs/COMPATIBILITY.md`) |
| Windows/macOS | CI läuft nur auf Linux |
