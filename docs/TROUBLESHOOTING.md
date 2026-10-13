# Troubleshooting

## Allgemein

| Symptom | Ursache | Lösung |
|---|---|---|
| `/fork` ist unbekannt | Runtime deaktiviert (Standard) | `config/lemon-runtime.yml`: `enabled: true`, Server neu starten |
| Log: „Hot deployment stays disabled because lemon-runtime.yml is invalid“ | Validierungsfehler in der Konfiguration | Fehlerliste im Log beheben (z. B. absoluter `module-directory`-Pfad) |
| `/fork module enable` → „runtime deployment is disabled (allow-runtime-deploy: false)“ | Laufzeit-Deployment ist aus | `allow-runtime-deploy: true` setzen und `/fork reload`, oder Modul in `startup-modules` eintragen und neu starten |
| „You do not have permission (lemon.runtime.module.manage)“ | Standard: niemand außer Konsole | Befehl in der Konsole ausführen oder Berechtigung gezielt vergeben |
| „operation 'enable' is already in progress for 'x'“ | parallele Operation | warten, dann erneut |
| Log: „/fork is already used by another command; use /lemon:fork instead“ | anderes Plugin nutzt `/fork` | `/lemon:fork …` verwenden |

## Modul wird nicht gefunden oder abgelehnt

`/fork module list` zeigt abgelehnte Dateien mit Grund.

| Meldung | Lösung |
|---|---|
| `missing lemon-module.yml` | Deskriptor im JAR-Wurzelverzeichnis ablegen |
| `invalid descriptor: …` | Feldliste in `docs/MODULE-FORMAT.md` prüfen; alle Fehler werden auf einmal gemeldet |
| `duplicate module id 'x' in 2 files` | alte JAR entfernen – bei Duplikaten werden **alle** Kandidaten abgelehnt |
| `unsafe file name` / `symbolic links are not accepted` / `path escapes its base directory` | JAR mit einfachem Namen direkt (ohne Symlink) in `lemon/modules/` ablegen |
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
| `ClassNotFoundException … (runtime internals are not part of the module API)` | Modul nutzt Runtime-Interna | nur `dev.lemonpvp.runtime.api.*` verwenden |
| `… (server internals are hidden (allow-server-internals: false))` | Modul nutzt `net.minecraft`/CraftBukkit | auf Paper-API umstellen oder bewusst `allow-server-internals: true` |
| `must declare the 'events' capability` | Fähigkeit fehlt im Deskriptor | `capabilities: [events, …]` ergänzen |
| Disable meldet „still running: async task #…“ und Unload wird verweigert | asynchrone Modularbeit endet nicht | Modul so ändern, dass Async-Tasks terminieren; später erneut `unload`; notfalls Neustart |
| `/fork status`: „A controlled server restart is recommended“ | Freigabe unvollständig oder Rollback gescheitert | Diagnose lesen, Server zum nächsten passenden Zeitpunkt neu starten |
| Diagnose: „class loader still reachable“ lange nach dem Entladen | Speicherleck im Modul (Threads, statische Referenzen, direkte Registrierungen) | Regeln in `docs/MODULE-FORMAT.md` befolgen; Heap-Dump analysieren |
| „blocked the main thread for N ms“ | langsames `onEnable`/`onDisable` | Datei-/Netzwerkarbeit in Async-Tasks verlagern |
| „timed out after 30 s“ | Hashing/Kopieren dauert zu lange (sehr große JAR, langsames Dateisystem) | `operation-timeout-seconds` erhöhen |

## Build

| Symptom | Lösung |
|---|---|
| `Could not resolve io.papermc.paper:…` / Verbindungsfehler zu Mojang | Netzwerkzugriff gemäß `docs/BUILDING.md` sicherstellen |
| `generate_patches.py`: „anchor for … found 0 times“ | Upstream hat sich geändert → `docs/UPSTREAM-SYNC.md` |
| `Project with path ':paper-checkstyle' could not be found` | Patches mit aktuellem Generator neu erzeugen (26.2+) |
| `Unsupported class file major version` | falsches JDK: 1.21.11 → 21, 26.x → 25 |

Logs und Audit: Server-Log (Präfix `[Lemon]`), `lemon/logs/audit.log`,
`/fork module diagnostics <id>`.
