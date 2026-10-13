# Neustartanforderungen und Capability-Matrix

Lemon versucht **nicht**, jede Änderung zur Laufzeit umzusetzen. Diese Matrix legt fest,
was sofort wirkt, was kontrolliert hot-deploybar ist und was einen Neustart braucht.

Legende:
**A – sofort aktivierbar** (Daten/Konfiguration, atomarer Wechsel mit Fallback) ·
**B – kontrolliert hot-deploybar** (Modulwechsel über die Runtime, mit Rollback) ·
**C1 – Teilneustart** (Modul entladen und neu laden bzw. `/fork reload`) ·
**C2 – vollständiger Serverneustart**

## Kategorie A – Konfiguration und Daten

| Änderung | Einstufung | Wie |
|---|---|---|
| Modulkonfiguration (`lemon/module-data/<id>/config.yml`) | A (wenn das Modul `ConfigurableModule` implementiert), sonst C1 | `/fork module config <id>`; ungültig ⇒ letzte gültige bleibt |
| Nachrichten/Texte eines Moduls | A, sofern aus der Modulkonfiguration gelesen (Beispiel: `example-greeter`) | wie oben |
| Feature-Flags, zeitgesteuerte Aktivierungen, Spawn-/Event-Regeln | A, sofern das Modul sie als Daten aus seiner Konfiguration liest und ein bereits laufender Handler sie auswertet | wie oben |
| GUI-Definitionen | A nur, wenn das GUI-System des Moduls Definitionen als Daten lädt; Lemon stellt kein eigenes GUI-System bereit | modulabhängig |
| `allow-runtime-deploy`, `verify-integrity`, `trusted-hashes-file`, `log-module-lifecycle`, `max-module-count`, `operation-timeout-seconds`, `allowed-api-versions`, `persist-runtime-state` | A | `/fork reload` |
| `allow-server-internals` | A für danach geladene Module | `/fork reload`, dann Module neu laden |
| `enabled`, `module-directory`, `startup-modules` | C2 | Neustart (wird von `/fork reload` gemeldet) |

## Kategorie B – kontrolliertes Modul-Hot-Deployment

| Änderung | Einstufung | Bedingungen |
|---|---|---|
| Neue Modulversion (eigene Klassen) | B | `/fork module reload <id>`; keine geladenen Module hängen davon ab; neue Version kompatibel (API, Minecraft, Abhängigkeiten) |
| Eigene Befehle | B | über `ModuleCommands`; Kurzlabel nur, wenn frei |
| Eigene Listener | B | über `ModuleEvents` |
| Eigene Scheduler-Aufgaben | B | über `ModuleScheduler`; laufende Async-Tasks verzögern das Entladen |
| Eigene Services | B | über `ServiceRegistry`; Konsumenten müssen abhängige Module sein |
| Eigene GUI-Handler (Inventar-Klicks etc.) | B | als Listener registrieren; offene Inventare des alten Moduls sollten in `onDisable` geschlossen werden |
| Neues Modul hinzufügen / Modul entfernen | B | `enable` / `unload` |
| Modul, von dem andere abhängen | C1 | Abhängige entladen, Modul neu laden, Abhängige wieder aktivieren |

## Kategorie C – Neustart erforderlich

| Änderung | Einstufung | Grund |
|---|---|---|
| Minecraft-Kern, Paper-/CraftBukkit-Code, Lemon-Runtime selbst | C2 | im Server-JAR; keine sichere Laufzeit-Neudefinition |
| Bukkit-/Paper-Plugins | C2 (Plugin-Neuladen ist nicht Teil von Lemon) | Paper rät von `/reload` ab |
| Änderungen an bereits geladenen Klassen (Signaturen, Felder) ohne neue Modulinstanz | C1 (Modul-Reload) bzw. C2 (Serverklassen) | JVM-Redefinition nur eingeschränkt; Lemon nutzt sie nicht |
| Registries (Blöcke, Items, Biome, Datapack-Registries) | C2 | Paper friert Registries beim Start ein |
| Weltformat, Weltgenerierung, Dimensionen | C2 | Welt- und Chunkzustand |
| Netzwerkprotokoll, Packet-Handling | C2 | laufende Spieler-Sessions |
| Brigadier-Befehle über Paper-Lifecycle-API | C2 / Paper-`/reload` | Paper erlaubt Registrierung nur beim Start/Reload |
| Modul hinterließ unvollständig freigegebene Ressourcen (`FAILED` nach Disable, Rollback gescheitert) | C2 empfohlen | `/fork status` zeigt „A controlled server restart is recommended“ |
| Asynchrone Modularbeit endet nicht | C2 empfohlen | Threads können nicht sicher beendet werden |
| Änderungen, für die kein sauberer Rollback möglich ist (externe Datenbanken, versendete Nachrichten, Weltänderungen) | – | Lemon kann solche Effekte nie zurückrollen; Module müssen idempotent/abwärtskompatibel migrieren |
