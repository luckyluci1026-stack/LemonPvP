# Testmodule: die Modul-Runtime an ihre Grenzen bringen

Vier Module zum Ausprobieren auf einem **Testserver**. Sie gehören nicht auf einen
Produktivserver: `test-stress` blockiert auf Befehl den Hauptthread für Sekunden,
`test-chaos` wirft absichtlich Fehler und lässt auf Befehl einen Thread laufen.

| Modul | Prüft | Befehl |
|---|---|---|
| `test-stress` | tausende Listener, Timer und Befehle, Event-Stürme, Speicher, Async-Tasks, Hot-Swap unter Last, Zustand über Swaps hinweg | `/stress` |
| `test-chaos` | Fehler in Befehl, Listener und Task; Fehler in `onLoad`/`onEnable`/`onDisable`; Rollback; abgelehnte Konfiguration; langsames `onEnable`; laufende Async-Arbeit blockiert das Entladen; nicht beendeter Thread (Leak) | `/chaos` |
| `test-library` | stellt einen Service (`CounterService`) bereit | `/testlib` |
| `test-consumer` | nutzt den Service; Abhängigkeit `test-library >=1.0.0 <2.0.0` | `/testcount [name]` |

Quellcode: `runtime/test-modules/` (`stress`, `chaos`, `library`, `consumer`).

## 1. Herunterladen

GitHub → **Actions** → Lauf von „Lemon CI“ → unten **Artifacts** →
**`test-modules-<version>`** (gleiche Version wie dein Server, z. B. `test-modules-1.21.11`).
Inhalt:

```
test-stress.jar    test-stress.jar.sha256
test-chaos.jar     test-chaos.jar.sha256
test-library.jar   test-library.jar.sha256
test-consumer.jar  test-consumer.jar.sha256
variants/2.0.0/    dieselben Module mit Version 2.0.0 (für Hot-Swap und Versionsbereiche)
```

Selbst bauen: `cd runtime && ./gradlew :test-modules:collect -PpaperApiVersion=<Paper-API-Version>`
→ `runtime/test-modules/build/modules/`.

## 2. Installieren

Wie beim Beispielmodul (Server mit Lemon-JAR, `config/lemon-runtime.yml` mit
`enabled: true` und `allow-runtime-deploy: true`, danach Neustart):

1. Gewünschte Module **jeweils mit ihrer `.sha256`-Datei** nach `lemon/modules/` hochladen.
2. Konsole: `fork module list` zeigt sie als `not loaded`.
3. Aktivieren: `fork module enable test-stress` (bzw. `test-chaos`, `test-library`,
   `test-consumer`).

Die Befehle der Module stehen Operatoren und der Konsole zur Verfügung
(`test-stress.use` usw., Standard `op`). Die `fork module …`-Befehle gehen standardmäßig nur
in der Konsole.

## 3. `test-stress`

Jede Zeile endet mit `- OK`, `- FAILED` oder `- REFUSED` (eine Obergrenze des Moduls ist
erreicht, nichts wurde geändert).

| Befehl | Was passiert |
|---|---|
| `stress all` | alle Szenarien mit den Mengen aus `config.yml` (Standard: 2000 Listener, 10 000 Events, 200 Befehle, 64 MiB, 500 Async-Tasks, 500 Timer) |
| `stress listeners [n]` | `n` Listener auf ein eigenes Event (alle Prioritäten), max. 50 000 |
| `stress events [n]` | feuert `n` Events; prüft, dass jeder Listener jedes Event bekommen hat; Zeit pro Handler-Aufruf. Max. 50 Mio. Handler-Aufrufe pro Befehl |
| `stress tasks [n]` | `n` Timer (jeden Tick), misst 40 Ticks: liefen alle in jedem Tick? |
| `stress commands [n]` | `n` Befehle registrieren, Befehlsbaum an Spieler senden, jeden einmal ausführen |
| `stress memory [MiB]` | belegt Speicher (max. 1024 MiB und max. die Hälfte des freien Heaps) |
| `stress async [n]` | `n` Async-Tasks (je 64 KiB hashen), zurück auf den Hauptthread; prüft, dass alle einmaligen Task-Handles danach freigegeben sind |
| `stress clear` | alles vorzeitig freigeben (prüft die Zählung) |
| `stress status` | Version, **Generation**, gehaltene Registrierungen, Heap, Tick-Zeit |

**Generation** zählt jede Aktivierung über Hot-Swaps und Neustarts hinweg (gespeichert in
`lemon/module-data/test-stress/state.properties`). Nach `fork module reload test-stress` muss
sie um 1 steigen: Der Beweis, dass die neue Instanz den Zustand der alten vorgefunden hat.

**Testablauf (Beispiel):**

```
fork module enable test-stress
stress all                              # 6 Zeilen "- OK" (Tasks und Async kommen 1-3 s später)
fork module diagnostics test-stress     # Tracked resources: listener=2000, command=201, task=500, ...
fork module reload test-stress          # Hot-Swap mit allem registriert
stress status                           # generation +1, held: 0 ... (neue Instanz startet leer)
fork module diagnostics test-stress     # alte Instanz: "Unloaded ... class loader still reachable"
                                        # bis zur nächsten vollen GC, danach "collected"
```

Hot-Swap auf eine andere Version: `variants/2.0.0/test-stress.jar` + `.sha256` über die Dateien in
`lemon/modules/` kopieren, `fork module reload test-stress` → `switched test-stress@1.0.0 ->
test-stress@2.0.0`.

## 4. `test-chaos`

Laufzeitfehler (alle werden mit Modul-ID protokolliert; der Server läuft weiter):

| Befehl | Erwartung |
|---|---|
| `chaos ping` | `alive, version …, enable #N of this instance` (`#2` = dieselbe Instanz wurde erneut aktiviert, z. B. nach einem Rollback) |
| `chaos throw-command` | Log: `Unhandled exception in command /chaos of module 'test-chaos'` |
| `chaos throw-listener [n]` | ein Listener wirft, der normale Listener bekommt trotzdem jedes Event: `saw n/n - OK` |
| `chaos throw-task` | Log: `Unhandled exception in task of module 'test-chaos'` |
| `chaos block-async <s>` | ein Async-Task blockiert `s` Sekunden; `fork module unload test-chaos` wird so lange **abgelehnt** (das Modul ist danach deaktiviert, aber noch geladen) |
| `chaos leak-thread <s>` | startet einen Thread, der beim Deaktivieren absichtlich **nicht** beendet wird; nach `fork module unload test-chaos` meldet `fork live diagnostics` `still in memory, threads: [test-chaos-leaked-thread]`, bis der Thread nach `s` Sekunden endet |
| `chaos status` | aktuelle Fehlerschalter aus `config.yml` |

`fork module diagnostics test-chaos` zählt die Fehler: `Callbacks: … N failed`.

Lebenszyklus-Fehler über `lemon/module-data/test-chaos/config.yml`:

| Schalter | Anwenden | Erwartung |
|---|---|---|
| `fail-on-enable: true` | `fork module reload test-chaos` | `FAILED reload … failed to enable; rolled back to test-chaos@1.0.0`, mit Grund; `chaos ping` → `enable #2` (alte Instanz läuft wieder) |
| `fail-on-load: true` | `fork module reload test-chaos` | `FAILED reload … loading failed …`; die laufende Version wird gar nicht angefasst |
| `enable-delay-ms: 99999` | `fork module config test-chaos` | `configuration rejected; the previous configuration stays active` (erlaubt 0–10000) |
| `enable-delay-ms: 5500` | `fork module reload test-chaos` | Warnung `onEnable of test-chaos blocked the main thread for … ms`, wenn größer als `operation-timeout-seconds` |
| `fail-on-disable: true` | `fork module config test-chaos`, dann `fork module disable test-chaos` | `OK disable` mit `onDisable threw …`; Diagnose: `Tracked resources: none` (alles trotzdem freigegeben) |

Danach Schalter zurück auf `false`/`0` setzen und `fork module config test-chaos`.

## 5. `test-library` und `test-consumer`

```
fork module enable test-consumer   # FAILED: requires module 'test-library' (>=1.0.0 <2.0.0) which is not loaded
fork module enable test-library
fork module enable test-consumer
testcount alpha                    # TESTCOUNT alpha = 1 (CounterService of test-library 1.0.0)
testlib                            # TESTLIB 1.0.0: counters {alpha=1}
fork module disable test-library   # FAILED: enabled modules depend on it: [test-consumer]
fork module reload test-library    # FAILED: while loaded modules depend on it
fork module unload test-consumer
# variants/2.0.0/test-library.jar + .sha256 nach lemon/modules/ kopieren
fork module reload test-library    # switched test-library@1.0.0 -> test-library@2.0.0
fork module enable test-consumer   # FAILED: module 'test-library' 2.0.0 does not satisfy >=1.0.0 <2.0.0
```

Die Zähler liegen im Speicher der Library: Nach einem Reload der Library beginnen sie bei 0.
Die Runtime überträgt keinen Modulzustand. Wer Zustand behalten will, muss ihn selbst
speichern (wie `test-stress` mit der Generation).

## 6. Automatischer Test (CI, Servertest 8)

`scripts/ci/smoke-test-modules.txt` fährt auf einem echten Server jeder Version
(1.21.11, 26.1.2, 26.2, 26.3) alle Punkte oben ab, u. a.:

* fehlende und falsche `.sha256` → `validate` lehnt ab;
* Abhängigkeiten, Service, Schutz vor Disable/Reload der Library, Versionsbereich 2.0.0;
* `stress all` 12-mal, dazwischen 10 Hot-Swaps mit allem registriert (zuletzt auf 2.0.0),
  Generation am Ende 12; 20 000 zusätzliche Listener, 22 Mio. Handler-Aufrufe, Grenzen;
* alle `test-chaos`-Fehlerfälle aus Abschnitt 4;
* zum Schluss alles entladen, **volle GC** (`jcmd <pid> GC.run`), dann muss für jede der 21
  entladenen Modulinstanzen `class loader collected` gemeldet werden und nichts mehr
  `still in memory` sein. Bleibt ein Loader übrig, schreibt der Test einen Heap-Dump und
  `scripts/ci/HeapPath.java` gibt die Referenzkette von der GC-Wurzel bis zum Loader aus.

Ergebnisse: [`TESTING.md`](TESTING.md).

## 7. Was die Testmodule in der Runtime gefunden haben

Beim Bau und in den ersten CI-Läufen fielen vier Schwächen der Runtime auf; sie sind
behoben und mit Tests abgesichert:

1. **Einmalige Tasks blieben bis zum Deaktivieren registriert.** Jeder `run`/`runLater`/
   `runAsync`-Aufruf eines Moduls hinterließ einen Eintrag im Ressourcen-Tracker (und die
   Task-ID einer Async-Task), bis das Modul deaktiviert wurde. Ein Modul, das z. B. pro Event
   einen Task plant, wuchs damit unbegrenzt. Jetzt geben einmalige Tasks ihren Eintrag nach
   dem Lauf frei (`ModuleSchedulerImpl`); Timer bleiben bis zum Schließen registriert.
   Prüfung: `stress async` (`0 still tracked after running`).
2. **Einzelnes Freigeben war quadratisch.** Der Tracker suchte jeden Eintrag linear;
   50 000 einzeln freigegebene Listener kosteten 1 345 ms auf dem Hauptthread (lokal
   gemessen), jetzt 5 ms. Test: `ResourceTrackerTest.releasingManyResourcesOneByOneStaysFast`.
3. **Fehlende Gründe in Fehlermeldungen.** Ein fehlgeschlagener Hot-Swap meldete nur
   „rolled back“, nicht warum; ein wegen laufender Async-Arbeit abgelehntes `unload`
   verschwieg, dass das Modul dabei bereits deaktiviert wurde (und merkte es für den Neustart
   weiter als aktiv vor). Jetzt stehen Grund bzw. Zustand im Ergebnis, und der Zustand wird
   gespeichert. Tests: `hotSwapRollsBack…`, `refusedUnloadOfAnEnabledModuleSaysItWasDisabledAndPersistsThat`.
4. **Der Bukkit-Scheduler hielt ein entladenes Modul im Speicher.** CI-Lauf #26: nach dem
   Entladen aller Module und voller GC war auf allen vier Versionen genau ein Class-Loader
   noch erreichbar (`test-chaos`). Die Heap-Analyse in Lauf #28 zeigte den Weg:
   `Server thread → CraftServer.scheduler → CraftAsyncScheduler.head → CraftAsyncTask →
   Task-Hülle der Runtime → Modul-Lambda → Klasse → Class-Loader`. Bukkit behält die zuletzt
   eingereichte Aufgabe als Kopf seiner Warteschlange, bis die nächste kommt; die Hülle der
   Runtime zeigte noch auf den Modulcode. Jetzt lässt die Hülle Modulcode, Callback-Guard und
   Abschluss los, sobald die Aufgabe beendet oder freigegeben ist. Lauf #32: auf allen vier
   Versionen 21 von 21 Loadern nach der GC eingesammelt.

Ein Fehler lag im Test selbst: `stress commands` zählte die Befehle zuerst innerhalb von
`/stress`. Ein Befehl, der während eines anderen Befehls ausgeführt wird, wird von Minecraft
(seit 1.20.3) nur eingereiht und läuft danach – gezählt wurden 0/200. Jetzt werden die
Befehle im nächsten Tick aus einem Task heraus ausgeführt und gezählt.

## 8. Grenzen

* Die Testmodule prüfen die Modul-Runtime, nicht Core-Live-Patches oder Plugin-Updates
  (dafür: `LIVE-UPDATES.md`, CI-Servertests 4–7).
* Ohne Spieler auf dem CI-Server werden Befehlsbäume an 0 Spieler gesendet; auf einem
  Testserver mit Spielern misst `stress commands` auch das.
* „class loader collected“ erscheint erst nach einer vollen Garbage Collection. Lemon löst
  nie selbst eine aus; direkt nach dem Entladen ist „still reachable“ normal.
