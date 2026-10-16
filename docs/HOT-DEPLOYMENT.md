# Hot-Deployment-Runtime

Die Lemon-Runtime lädt, aktiviert, deaktiviert und ersetzt **ausgewählte Erweiterungen
(Module)** kontrolliert zur Laufzeit. Sie ist **standardmäßig deaktiviert**. Dieses
Dokument beschreibt Architektur, Abläufe, Threading, Konfiguration und Befehle.

> Begriffsklärung: Modul-Hot-Deployment ist **kein JVM-Hot-Swap**. Lemon lädt ein Modul in
> einen eigenen Class-Loader, aktiviert es, und beim Austausch wird die alte Instanz
> deaktiviert und ihr Class-Loader geschlossen. Alte Klassen verschwinden erst aus der JVM,
> wenn der Garbage Collector sie freigeben kann. Getrennt davon gibt es Live-Updates
> normaler Plugins (mit Kompatibilitätsprüfung) und Core-Patches (echte JVM-Redefinition
> von Serverklassen, nur Methodenrümpfe auf Standard-JVMs) – beides beschreibt
> [`LIVE-UPDATES.md`](LIVE-UPDATES.md).

## 1. Architektur

```
            Paper-Server (unverändert, bis auf 3 Hook-Zeilen in CraftServer)
  ┌─────────────────────────────────────────────────────────────────────────┐
  │ CraftServer.enablePlugins(POSTWORLD) ─┐   disablePlugins() ─┐  reload() ─┐
  │                                       ▼                     ▼            ▼
  │                      dev.lemonpvp.runtime.paper.LemonRuntimeHooks (catch-all)
  │                                       │
  │  runtime-paper (Adapter)              ▼
  │   PaperRuntime ── RuntimeHostPlugin (interner Owner, nicht im PluginManager)
  │     │            PaperPlatform  ── Bukkit-Scheduler (Hauptthread), Logger
  │     │            PaperBinding   ── ModuleEvents / ModuleCommands / ModuleScheduler
  │     │            ForkCommand    ── /fork (MiniMessage-Vorlagen, unparsed Platzhalter)
  │     ▼
  │  runtime-core (plattformneutral, lokal getestet)
  │   LemonRuntime ─ ModuleManager ─┬─ ModulePreparer   (Worker: Scan, Hash, Staging, Preload)
  │                                 ├─ ModuleLifecycle  (Hauptthread: onLoad/onEnable/onDisable)
  │                                 ├─ ModuleSwapper    (Hot-Swap + Rollback)
  │                                 ├─ DependencyResolver
  │                                 └─ ResourceTracker / CallbackGuard / ServiceHub
  │   security: PathGuard, IntegrityVerifier, AuditLog   config: RuntimeConfig, YamlSupport
  │   loader: ModuleScanner, ModuleClassLoader, ArtifactStager, ClassPreloader
  │   metrics: ModuleDiagnostics, LeakDetector, RuntimeHealth, RuntimeMetrics
  │   state: RuntimeStateStore   commands: AdminCommand (plattformneutral)
  │     ▲
  │  runtime-api / runtime-paper-api (einzige Pakete, die Module sehen dürfen)
  └─────────────────────────────────────────────────────────────────────────┘
```

| Paket | Inhalt |
|---|---|
| `dev.lemonpvp.runtime.api` | Modul-SPI: `RuntimeModule`, `ModuleContext`, `ConfigurableModule`, `ModuleDescriptor`, `ResourceScope`, `ServiceRegistry`, `ConfigSnapshot` |
| `dev.lemonpvp.runtime.api.paper` | Paper-Fassade: `PaperModulePlatform`, `ModuleEvents`, `ModuleCommands`, `ModuleScheduler`, `CommandSpec` |
| `dev.lemonpvp.runtime.loader` | Scanner, Class-Loader, Staging, Preloading |
| `dev.lemonpvp.runtime.lifecycle` | Zustandsmaschine, Manager, Swapper, Abhängigkeiten |
| `dev.lemonpvp.runtime.security` | Pfadprüfung, Integrität, Audit-Log |
| `dev.lemonpvp.runtime.config` | Runtime- und Modulkonfiguration |
| `dev.lemonpvp.runtime.metrics` | Diagnose, Leak-Erkennung, Health |
| `dev.lemonpvp.runtime.commands` | `/fork`-Logik (plattformneutral) |
| `dev.lemonpvp.runtime.paper` | Paper-Adapter (im Server, für Module unsichtbar) |

## 2. Lebenszyklus

```
DISCOVERED ─► VALIDATED ─► LOADED ─► ENABLING ─► ENABLED ─► DISABLING ─► DISABLED ─► UNLOADED
                                ▲                                            │
                                └──────────────── (erneut aktivieren) ◄──────┘
jeder Zwischenzustand ─► FAILED ─► UNLOADED
```

Erlaubte Übergänge stehen in `ModuleStateMachine`; jeder andere Übergang wird mit einer
Exception abgelehnt (getestet). Eine Instanz im Zustand `FAILED` wird nie wieder
aktiviert, sondern beim nächsten `enable`/`load` durch eine frische Instanz ersetzt.

| Zustand | Bedeutung |
|---|---|
| DISCOVERED | JAR gefunden, Deskriptor gelesen |
| VALIDATED | Integrität, Kompatibilität, Abhängigkeiten geprüft, JAR gestaged, Klassen definiert |
| LOADED | Hauptklasse instanziiert, `onLoad` erfolgreich – noch keine Registrierungen |
| ENABLING / ENABLED | `onEnable` läuft / Modul aktiv |
| DISABLING / DISABLED | `onDisable` und Ressourcenfreigabe laufen / inaktiv, Klassen noch geladen |
| UNLOADED | Class-Loader geschlossen, Referenzen der Runtime verworfen (terminal) |
| FAILED | ein Schritt schlug fehl; Details in `/fork module diagnostics <id>` |

## 3. Abläufe

### Laden / Aktivieren (`/fork module enable <id>`)

| # | Schritt | Thread |
|---|---|---|
| 1 | Operationssperre für die Modul-ID setzen (zweite Operation wird sofort abgelehnt) | aufrufend |
| 2 | Modulverzeichnis scannen, Deskriptor validieren, Duplikate ablehnen | Worker |
| 3 | API-Version (`allowed-api-versions`) und Minecraft-Version prüfen | Worker |
| 4 | SHA-256 berechnen, `.sha256`-Datei und ggf. Allow-List prüfen | Worker |
| 5 | JAR nach `lemon/staging/` kopieren, Kopie erneut hashen (TOCTOU-Schutz) | Worker |
| 6 | Datenverzeichnis anlegen, deklarierte Ressourcen kopieren (nie überschreiben), `config.yml` parsen | Worker |
| 7 | Abhängigkeiten auflösen (Versionen, Zustände), Plugin-Abhängigkeiten prüfen | Hauptthread |
| 8 | Class-Loader erzeugen, alle Klassen **ohne Initialisierung** definieren | Worker |
| 9 | Hauptklasse instanziieren, `onLoad` | Hauptthread |
| 10 | Ressourcen-Tracker + Callback-Guard anlegen, Berechtigungen registrieren, `onEnable` | Hauptthread |
| 11 | Bei Fehler: alle bis dahin registrierten Ressourcen freigeben, Zustand `FAILED` | Hauptthread |
| 12 | Aktivierung persistieren (`persist-runtime-state`), Audit-Log schreiben | Worker |

### Deaktivieren

1. Abgelehnt, solange **aktivierte** Module davon abhängen.
2. Callback-Guard sperren → bereits eingeplante Events/Tasks erreichen das Modul nicht mehr.
3. `onDisable` (Fehler werden protokolliert, die Freigabe läuft trotzdem).
4. Alle Ressourcen in umgekehrter Reihenfolge freigeben: Tasks abbrechen, Listener
   abmelden, Befehle entfernen (+ Befehlsbaum der Spieler aktualisieren), Services
   abmelden, eigene `AutoCloseable`s schließen, Berechtigungen entfernen.
5. Gab es Freigabefehler → Zustand `FAILED` + Neustartempfehlung; sonst `DISABLED`.
6. Noch laufende asynchrone Arbeit wird gemeldet; `unload` wird verweigert, bis sie endet.

### Hot-Swap (`/fork module reload <id>`)

1. Neue Version vollständig vorbereiten (Schritte 2–9 oben). Fehler hier → **alte Version
   bleibt unverändert aktiv**.
2. In **einer** Hauptthread-Aufgabe: alte Version deaktivieren, neue aktivieren.
   Synchrone Events und Scheduler-Tasks laufen auf demselben Thread und sehen keine Lücke;
   **asynchrone Events (z. B. Chat, Pre-Login) können die Lücke sehen.**
3. Schlägt `onEnable` der neuen Version fehl: deren Ressourcen freigeben, alte Version
   erneut aktivieren (**Rollback**). Misslingt auch das, ist das Modul inaktiv und ein
   Neustart wird empfohlen.
4. Erfolgreich: alte Instanz entladen. Läuft dort noch asynchrone Arbeit, wird das Entladen
   später automatisch wiederholt (`/fork status` zeigt das an).

Ein echter atomarer Wechsel (beide Versionen gleichzeitig aktiv) wird bewusst **nicht**
angeboten: doppelte Befehle, Listener und Schreibzugriffe wären nicht allgemein sicher.
Rollback stellt nur den Zustand **innerhalb der Runtime** wieder her – Weltänderungen,
Datenbankschreibvorgänge oder versendete Nachrichten der neuen Version bleiben bestehen.

Module, von denen andere geladene Module abhängen, können nicht ersetzt oder entladen
werden (deren Class-Loader verweist auf die alten Klassen). Abhängige zuerst entladen.

### Konfiguration neu laden (`/fork module config <id>`) – Kategorie A

`config.yml` wird auf dem Worker geparst, dann auf dem Hauptthread von
`ConfigurableModule.validateConfig` geprüft und **atomar** ausgetauscht; anschließend
`onConfigReload`. Ungültiges YAML, eine abgelehnte Validierung oder eine Exception in
`onConfigReload` lassen die **letzte gültige Konfiguration** aktiv.

### Server-Start, `/reload`, Stopp

* Start: Hooks lesen `config/lemon-runtime.yml`. Ist `enabled: false`, passiert sonst
  nichts. Andernfalls werden `startup-modules` und persistierte Aktivierungen (nur bei
  unverändertem SHA-256) ab dem ersten Tick nacheinander in Abhängigkeitsreihenfolge
  aktiviert.
* `/reload` (Paper): Runtime wird vorher vollständig gestoppt und danach neu gestartet.
* Stopp: Module werden synchron in umgekehrter Abhängigkeitsreihenfolge deaktiviert und
  entladen, bevor Paper die Plugins deaktiviert. Abschließend entfernt die Runtime als
  Sicherheitsnetz alle Listener und Tasks ihres internen Owners.

## 4. Threading-Regeln

| Arbeit | Thread |
|---|---|
| Dateisystem, Hashing, YAML-Parsing, Klassendefinition | ein einzelner Worker-Thread `Lemon-Runtime-Worker` (existiert nur, solange Arbeit ansteht; 30 s Leerlauf-Timeout) |
| Modulcode (`onLoad`, `onEnable`, `onDisable`, `validateConfig`, `onConfigReload`), Zustandsänderungen | Server-Hauptthread über den Bukkit-Scheduler |
| Event-Handler, Befehle, Tasks der Module | wie von Paper vorgegeben; jeder Aufruf läuft durch den `CallbackGuard` |

* Pro Modul-ID läuft höchstens **eine** Operation; parallele Anfragen werden abgelehnt,
  nicht eingereiht (keine Deadlocks durch Warteschlangen).
* Asynchrone Phasen haben einen Timeout (`operation-timeout-seconds`). Hauptthread-
  Callbacks können nicht sicher unterbrochen werden; dauern sie länger, wird das gemeldet.
* Der Hauptthread blockiert nie auf den Worker (nur Futures, keine `get()`-Aufrufe).
  Ausnahme: beim Server-Stopp wird bis zu 5 s auf das Ende des Workers gewartet.
* Kein Code der Runtime läuft im Tick, solange keine Operation stattfindet. Es gibt keine
  periodischen Tasks und keine Dateisystem-Überwachung.

## 5. Ressourcenverwaltung

Pro Aktivierung erhält ein Modul einen `ResourceTracker`. Alles, was über die Fassade
registriert wird, landet dort mit Kategorie (`listener`, `command`, `task`, `service`,
`custom`). Freigabe erfolgt LIFO; ein fehlschlagender Eintrag hält die übrigen nicht auf.
Registrierungen nach dem Schließen werden sofort wieder freigegeben und mit
`IllegalStateException` abgelehnt („late registration“).

| Ressource | Registrierung | Freigabe |
|---|---|---|
| Listener (Lambda) | `PluginManager.registerEvent(..., host, ...)` mit eigenem Token-Listener | `HandlerList.unregisterAll(token)` |
| Listener (`@EventHandler`) | je Methode `registerEvent` mit `EventExecutor.create` + Guard | `HandlerList.unregisterAll(listener)` |
| Befehl | `CommandMap.register(label, <modul-id>, cmd)`; Kurzlabel/Aliasse nur, wenn frei | alle `knownCommands`-Einträge des Befehls entfernen, `unregister`, `Player#updateCommands()` |
| Task | `BukkitScheduler.runTask*(host, guard(task))` | `BukkitTask#cancel()`; laufende Async-Tasks werden über `isCurrentlyRunning` gemeldet |
| Service | `ServiceHub` | Eintrag entfernen (inkl. Schlüssel, damit keine `Class`-Referenz bleibt) |
| Berechtigungen aus dem Deskriptor | `PluginManager.addPermission` (nur wenn noch nicht vorhanden) | `removePermission` |
| Eigene Ressourcen | `context.resources().track(...)` / `onCleanup(...)` | `close()` / Runnable |

Was **nicht** erfasst werden kann (und deshalb Neustart-relevant ist): direkt über die
Bukkit-API registrierte Listener/Tasks, vom Modul selbst gestartete Threads,
`ThreadLocal`s in fremden Threads, statische Referenzen aus Server/Plugins auf
Modulklassen, Registrierungen in Paper-Registries. Siehe `docs/LIMITATIONS.md`.

## 6. Class-Loader-Isolation

Siehe `ModuleClassLoader`:

1. Gesperrte Pakete: `dev.lemonpvp.runtime.*` außer `dev.lemonpvp.runtime.api.*`; bei
   `allow-server-internals: false` zusätzlich `net.minecraft.*`, `org.bukkit.craftbukkit.*`.
2. Parent-first (Server): JDK, Paper/Bukkit-API, mitgelieferte Bibliotheken, Runtime-API.
   Eine vom Modul mitgelieferte Bibliothek wird ignoriert, wenn der Server sie bereits hat.
3. Eigene JAR. 4. JARs deklarierter Modulabhängigkeiten (nur deren eigene Klassen).
5. Class-Loader deklarierter Plugin-Abhängigkeiten.

Ressourcen (z. B. `config.yml`) werden zuerst im Modul-JAR gesucht. Der Thread-Context-
Class-Loader wird für jeden Modulaufruf gesetzt und danach zurückgesetzt.

**Diese Isolation ist keine Sicherheits-Sandbox** (siehe `docs/SECURITY.md`).

Nach `unload` beobachtet der `LeakDetector` den Class-Loader per `WeakReference` und
zeigt in `/fork module diagnostics <id>`, ob er bereits eingesammelt wurde und welche
lebenden Threads ihn noch referenzieren. Die Runtime löst selbst nie eine GC aus.

## 7. Konfiguration (`config/lemon-runtime.yml`)

Alle Optionen werden ausgewertet (getestet in `RuntimeConfigLoaderTest`). Ungültige Werte
→ Runtime bleibt deaktiviert (beim Start) bzw. alte Konfiguration bleibt aktiv
(`/fork reload`).

| Option | Standard | Wirkung | Änderung zur Laufzeit |
|---|---|---|---|
| `config-version` | 1 | Format-Version | – |
| `enabled` | `false` | Hauptschalter; `false` = keine Threads, Scans, Befehle, Listener | Neustart |
| `module-directory` | `lemon/modules` | Modul-JARs; relativer, sicherer Pfad im Serververzeichnis | Neustart |
| `allow-runtime-deploy` | `false` | erlaubt `load/enable/disable/reload/unload` im Betrieb | sofort |
| `startup-modules` | `[]` | beim Start aktivierte Module | Neustart |
| `persist-runtime-state` | `true` | zur Laufzeit aktivierte Module nach Neustart wiederherstellen (nur bei gleichem SHA-256) | sofort |
| `verify-integrity` | `true` | `<jar>.sha256` muss existieren und passen | sofort |
| `trusted-hashes-file` | `""` | Allow-List erlaubter SHA-256-Werte | sofort |
| `log-module-lifecycle` | `true` | Lebenszyklus im Server-Log (Audit-Log immer) | sofort |
| `max-module-count` | 32 | Obergrenze gleichzeitig geladener Module | sofort |
| `operation-timeout-seconds` | 30 | Timeout asynchroner Phasen / Schwelle für „langsam“ | sofort |
| `allowed-api-versions` | `[1]` | akzeptierte Modul-API-Versionen | sofort |
| `allow-server-internals` | `false` | `net.minecraft`/CraftBukkit für Module sichtbar | für danach geladene Module |
| `plugin-updates.enabled` | `false` | erlaubt `/fork live update\|rollback` für Plugins (Prüfen geht immer) | sofort |
| `plugin-updates.trusted-hashes-file` | `""` | Allow-List erlaubter Plugin-JARs | sofort |
| `live-patching.enabled` | `false` | erlaubt Core-Patches (braucht zusätzlich `-javaagent:lemon-livepatch-agent.jar`) | Neustart |
| `live-patching.profile` | `production` | `production` \| `development` (Letzteres nur mit `-Dlemon.livepatch.development=true`) | Neustart |
| `live-patching.patch-directory` | `lemon/patches/inbox` | Ablage für `<id>.lemonpatch` + `.sha256` | Neustart |
| `live-patching.trusted-hashes-file` | `""` | Freigabeliste der Patch-Archive (in Produktion Pflicht) | sofort |
| `live-patching.confirm-timeout-seconds` | 120 | Zeit zwischen `update` und `update … confirm` bei Core-Patches (Plugins und Module: immer 120 s) | sofort |

Beispiel: `docs/examples/lemon-runtime.yml`. Live-Updates: [`LIVE-UPDATES.md`](LIVE-UPDATES.md).

## 8. Dateistruktur

```
config/lemon-runtime.yml                Runtime-Konfiguration (neben Paper-Konfigurationen)
lemon/modules/<x>.jar + <x>.jar.sha256  Modul-Artefakte (vom Admin bereitgestellt)
lemon/module-data/<id>/                 Modulkonfiguration (config.yml) und -daten
lemon/staging/                          geprüfte private JAR-Kopien (temporär; beim Stopp geleert)
lemon/logs/audit.log                    Audit-Log
lemon/state/runtime-state.properties    zur Laufzeit aktivierte Module (+ SHA-256)
lemon/modules-removed/<Zeit>/           mit /fork module remove entfernte Module
lemon/patches/…, lemon/plugin-backups/… Core-Patches und Plugin-Sicherungen (siehe LIVE-UPDATES.md)
```

Versionsspezifische Dateien entstehen nicht; Module deklarieren ihre unterstützten
Minecraft-Versionen im Deskriptor.

## 9. Befehle und Berechtigungen

`/fork` (Namespace-Form `/lemon:fork`) wird nur registriert, wenn die Runtime aktiv ist,
und überschreibt keinen bestehenden Befehl.

| Befehl | Berechtigung | Wirkung |
|---|---|---|
| `/fork`, `info`, `version`, `status` | `lemon.runtime.command` (OP) | Anzeige |
| `/fork reload` | `lemon.runtime.command.reload` (**niemand**, nur Konsole/explizit) | `lemon-runtime.yml` neu laden |
| `/fork module list` | `lemon.runtime.module.view` (OP) | Verzeichnis scannen + Status |
| `/fork module info\|validate\|diagnostics <id>` | `lemon.runtime.module.view` (OP) | Anzeige / Prüfung ohne Klassen zu laden |
| `/fork module load\|enable\|disable\|reload\|unload\|config\|remove <id>` | `lemon.runtime.module.manage` (**niemand**, nur Konsole/explizit) | ändert ausgeführten Code; `remove` entlädt und verschiebt die JAR nach `lemon/modules-removed/` |
| `/fork live status\|scan\|plugins\|modules\|history\|check <name>`, `/fork live patch status` | `lemon.runtime.live.view` (OP) | Anzeige / Prüfung (siehe `LIVE-UPDATES.md`) |
| `/fork live update\|rollback <name> [confirm]`, `/fork live patch probe` | `lemon.runtime.live.update` (**niemand**, nur Konsole/explizit) | Plugins, Module, Core-Patches ändern |

Argumente werden validiert (Modul-IDs nur `[a-z][a-z0-9_-]{1,47}`, keine Pfade).
Ausgaben nutzen feste MiniMessage-Vorlagen; dynamischer Text wird mit
`Placeholder.unparsed` eingesetzt und nie als Markup interpretiert. Jede ändernde Aktion
landet im Audit-Log (`Zeit | Akteur | Aktion | Ziel | OK/FAILED | Details`).
