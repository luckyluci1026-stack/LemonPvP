# Troubleshooting

## Allgemein

| Symptom | Ursache | Lösung |
|---|---|---|
| `/fork` ist unbekannt | Runtime deaktiviert (Standard) | `config/paperswap.yml`: `enabled: true`, Server neu starten |
| Log: „Hot deployment stays disabled because paperswap.yml is invalid“ | Validierungsfehler in der Konfiguration | Fehlerliste im Log beheben (z. B. absoluter `module-directory`-Pfad) |
| `/fork module enable` → „runtime deployment is disabled (allow-runtime-deploy: false)“ | Laufzeit-Deployment ist aus | `allow-runtime-deploy: true` setzen und `/fork reload`, oder Modul in `startup-modules` eintragen und neu starten |
| „You do not have permission (paperswap.module.manage)“ | Standard: niemand außer Konsole | Befehl in der Konsole ausführen oder Berechtigung gezielt vergeben |
| „operation 'enable' is already in progress for 'x'“ | parallele Operation | warten, dann erneut |
| Log: „/fork is already used by another command; use /paperswap:fork instead“ | anderes Plugin nutzt `/fork` | `/paperswap:fork …` verwenden |

## Modul wird nicht gefunden oder abgelehnt

`/fork module list` zeigt abgelehnte Dateien mit Grund.

| Meldung | Lösung |
|---|---|
| `missing module.yml (or paperswap-module.yml)` | Deskriptor im JAR-Wurzelverzeichnis ablegen; ist es ein Bukkit-Plugin, gehört es nach `plugins/` |
| `contains both paperswap-module.yml and module.yml; keep only one` | nur einen Deskriptor behalten |
| `invalid descriptor: …` | Feldliste in `docs/MODULE-FORMAT.md` prüfen; alle Fehler werden auf einmal gemeldet |
| `duplicate module id 'x' in 2 files` | alte JAR entfernen – bei Duplikaten werden **alle** Kandidaten abgelehnt |
| `unsafe file name` / `symbolic links are not accepted` / `path escapes its base directory` | JAR mit einfachem Namen direkt (ohne Symlink) in `paperswap/modules/` ablegen |
| `missing x.jar.sha256 (verify-integrity is enabled)` | `sha256sum x.jar > x.jar.sha256` |
| `SHA-256 mismatch` | Upload unvollständig oder JAR geändert – Hash neu erzeugen, nachdem die Datei final ist |
| `digest … is not listed in trusted.txt` | Digest nach Prüfung in die Allow-List aufnehmen |
| `requires module API 2 but this runtime provides 1` | Modul für eine neuere Runtime gebaut |
| `supports Minecraft [26.*] but the server runs 1.21.11` | passende Modulversion verwenden |
| `requires module 'x' … which is not loaded` | Abhängigkeit zuerst aktivieren |
| `plugin dependency … not available` | Plugin installieren/aktivieren |
| `the jar of 'x' changed since it was activated` (beim Start) | bewusster Schutz: geänderte JARs werden nicht automatisch wieder aktiviert → `/fork module enable x` |

## Laufzeitprobleme

| Symptom | Ursache | Lösung |
|---|---|---|
| `FAILED enable x: … onEnable threw …` | Fehler im Modul | Stacktrace im Server-Log; alle Registrierungen wurden freigegeben; Modul korrigieren |
| „main class … is not defined by the module jar“ / „does not implement“ | falsche `main`-Angabe | Deskriptor korrigieren |
| `ClassNotFoundException … (runtime internals are not part of the module API)` | Modul nutzt Runtime-Interna | nur `dev.paperswap.runtime.api.*` verwenden |
| `… (server internals are hidden (allow-server-internals: false))` | Modul nutzt `net.minecraft`/CraftBukkit | auf Paper-API umstellen oder bewusst `allow-server-internals: true` |
| `must declare the 'events' capability` | Fähigkeit fehlt im Deskriptor | `capabilities: [events, …]` ergänzen |
| Disable meldet „still running: async task #…“ und Unload wird verweigert | asynchrone Modularbeit endet nicht | Modul so ändern, dass Async-Tasks terminieren; später erneut `unload`; notfalls Neustart |
| `/fork status`: „A controlled server restart is recommended“ | Freigabe unvollständig oder Rollback gescheitert | Diagnose lesen, Server zum nächsten passenden Zeitpunkt neu starten |
| Diagnose: „class loader still reachable“ lange nach dem Entladen | Speicherleck im Modul (Threads, statische Referenzen, direkte Registrierungen) | Regeln in `docs/MODULE-FORMAT.md` befolgen; Ursache finden: `jcmd <pid> GC.heap_dump leak.hprof`, dann `java scripts/ci/HeapPath.java leak.hprof dev.paperswap.runtime.loader.ModuleClassLoader closed` – zeigt die Referenzkette von einer GC-Wurzel bis zum Loader (Heap-Dump enthält Serverdaten, nicht weitergeben) |
| „blocked the main thread for N ms“ | langsames `onEnable`/`onDisable` | Datei-/Netzwerkarbeit in Async-Tasks verlagern |
| „timed out after 30 s“ | Hashing/Kopieren dauert zu lange (sehr große JAR, langsames Dateisystem) | `operation-timeout-seconds` erhöhen |

## Build

| Symptom | Lösung |
|---|---|
| `Could not resolve io.papermc.paper:…` / Verbindungsfehler zu Mojang | Netzwerkzugriff gemäß `docs/BUILDING.md` sicherstellen |
| `generate_patches.py`: „anchor for … found 0 times“ | Upstream hat sich geändert → `docs/UPSTREAM-SYNC.md` |
| `Project with path ':paper-checkstyle' could not be found` | Patches mit aktuellem Generator neu erzeugen (26.2+) |
| `Unsupported class file major version` | falsches JDK: 1.21.11 → 21, 26.x → 25 |
| `NoSuchFileException …/.git/objects/xx` in `run<Fork>Setup` | Git hat während des Kopierens im Hintergrund `gc --auto` ausgeführt; Befehl wiederholen. Die CI setzt dafür `gc.auto 0` und `maintenance.auto false` |

Logs und Audit: Server-Log (Präfix `[PaperSwap]`), `paperswap/logs/audit.log`,
`/fork module diagnostics <id>`.

## Live-Updates (`/fork live`)

| Meldung | Ursache | Lösung |
|---|---|---|
| `Agent: live-patch agent not loaded: start the JVM with -javaagent:paperswap-agent.jar` | Server ohne Agent gestartet | JVM-Option vor `-jar` ergänzen (Pterodactyl: Startbefehl), Neustart |
| `agent class is on the class path but was not started by the JVM` | Agent-JAR per `-cp` statt `-javaagent` | `-javaagent:` verwenden |
| `live patching is disabled (live-patching.enabled: false …)` | Core-Patches aus | `live-patching.enabled: true`, Neustart |
| `the production profile only applies patches listed in live-patching.trusted-hashes-file, which is not configured` | Produktionsprofil ohne Freigabeliste | Freigabeliste konfigurieren und Digest eintragen |
| `integrity check failed: SHA-256 mismatch` / `… is not listed in …` | `.sha256` passt nicht / Patch nicht freigegeben | Datei vollständig übertragen; Digest freigeben |
| `patch was built for … but the server is … (wrong build id …)` | Patch gegen eine andere Server-JAR gebaut | Basis = exakt die deployte JAR (`livePatchBaseline` direkt nach dem Bauen bzw. entpackte JAR vom Server holen) |
| `patch was built against … but the server runs …` | laufender Bytecode einer Klasse weicht ab | Patch gegen die deployte JAR neu bauen |
| `unsupported change(s): method added … (this JVM cannot add methods/constructors …)` | strukturelle Änderung auf Standard-JVM | Änderung auf Methodenrümpfe beschränken oder Neustart mit neuer JAR (oder JBR mit `-XX:+AllowEnhancedClassRedefinition`) |
| `the class file fails bytecode verification (…)` | ungültiger Bytecode (nur auf JVMs ohne eigene Verifikation geprüft) | Klassendateien neu kompilieren; kein Nachbearbeiten von Bytecode |
| `the class could not be verified before the redefinition …` | Klasse lässt sich außerhalb ihres Laufzeitpakets nicht verifizieren (JBR) | Neustart mit neuer JAR; auf Entwicklungsservern Entwicklungsprofil |
| `FAILED the JVM rejected the redefinition: …` | JVM hat abgelehnt; laut JVMTI wurde keine Klasse geändert | Meldung im Server-Log lesen; nichts zurückzurollen |
| `no plan of x is waiting for confirmation …` | `confirm` ohne vorherigen Plan, abgelaufen, oder anderer Akteur | zuerst `fork live update x` |
| Plugin: `needs a server restart` | Abhängige Plugins, Paper-Plugin, Lifecycle-API … (Gründe werden gelistet) | Neustart; Paper installiert die JAR aus `plugins/update/` beim Start |
| Plugin: `the update folder holds another jar of plugin X` | mehrere JARs desselben Plugins in `plugins/update/` | nur eine behalten |
| Plugin: `FAILED … the previous version … was restored and is enabled` | neue Version startete nicht (Fehler im Server-Log) | Plugin korrigieren; alte Version läuft weiter |
| Plugin: `restoring the previous version also failed … restart the server` | auch die alte Version startete nicht | alte JAR liegt wieder in `plugins/`; Server neu starten |
| `'x' is ambiguous ([MODULE, PLUGIN]); use a prefix` | gleicher Name in mehreren Bereichen | `plugin:x`, `module:x` oder `patch:x` |
| `not verified: … Can-Retransform-Classes missing` | alte Agent-JAR (Version 1) | `paperswap-agent.jar` dieses Builds verwenden (Startmeldung `live-patch agent 3 loaded`); der Patch selbst wurde angewendet |
| `VERIFICATION FAILED: the JVM runs bytecode that differs …` | die Klasse wurde nach dem Patch verändert (anderer Agent, IntelliJ-HotSwap) oder ein Agent instrumentiert sie | `fork live patch verify <id>`, Ursache klären; `fork live rollback <id>` stellt den Bytecode vor dem Patch her |
| Plugin: `WARNING: still registered for the unloaded version …` | nach dem Entladen waren noch Listener/Tasks/… der alten Instanz registriert | Update ist erfolgt; alte Version bleibt im Speicher – Neustart bei Gelegenheit, Plugin-Autor informieren |
| Plugin: `WARNING: threads of the unloaded version … are still running` | Plugin beendet eigene Threads/Executor nicht in `onDisable` | wie oben; `fork live inspect <Plugin>` zeigt den Stand |
| `checkLivePatch`: `NOT APPLICABLE to this server jar` | Patch passt nicht zur angegebenen Server-JAR (Build-ID, Klassen-Hash, Prüfsumme) | gegen die laufende JAR bauen (`-PserverJar`) |
