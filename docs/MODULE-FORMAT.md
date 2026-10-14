# Modulformat und Modulentwicklung

Ein Lemon-Modul ist eine **JAR-Datei** mit einer Datei `module.yml` (gleichwertig:
`lemon-module.yml`; nur eine von beiden) im Wurzelverzeichnis und einer Hauptklasse, die
`dev.lemonpvp.runtime.api.RuntimeModule` implementiert. Neben der JAR liegt eine
`<jar>.sha256`-Datei (Integrität). Das kürzeste gültige Modul braucht drei Zeilen:

```yaml
id: my-module
version: 1.0.0
main: org.example.mymodule.Main
```

Ohne Angabe gelten: `api-version` = API-Version der Runtime, `minecraft-versions: ["*"]`
(jede Version), alle `capabilities`. Kern-Änderungen sind für neue Module nie nötig.

Ein Modul wird **nie** automatisch aktiviert, nur weil seine JAR im Modulverzeichnis
liegt. Aktivierung erfolgt ausschließlich über `/fork module enable <id>` (wenn
`allow-runtime-deploy: true`) oder `startup-modules` in `lemon-runtime.yml`.

## 1. `module.yml` / `lemon-module.yml`

```yaml
id: event-tools                 # Pflicht. [a-z][a-z0-9_-]{1,47}, eindeutig, nicht reserviert
version: 1.4.0                  # Pflicht. Semantic Versioning (MAJOR.MINOR.PATCH[-pre][+build])
name: Event Tools               # optional, Standard = id (max. 64 Zeichen)
description: Werkzeuge für Events   # optional (max. 512 Zeichen)
authors: [Alice, Bob]           # optional (Entwicklerangabe)
main: org.example.events.EventTools  # Pflicht. Voll qualifiziert, in einem eigenen Paket
api-version: 1                  # optional, Standard = API-Version der Runtime (1)
minecraft-versions: ["1.21.11", "26.*"]   # optional, Standard ["*"]. Exakt, Präfix ".*" oder "*"
capabilities: [events, commands, scheduler, services]  # optional; ohne Angabe: alle, mit Angabe: nur diese
permissions:                    # deklarierte Berechtigungen, Präfix muss "<id>." sein
  event-tools.start:
    description: Startet ein Event
    default: op                 # true | false | op | not-op
dependencies:                   # andere Lemon-Module
  - id: core-lib
    version: ">=2.0.0 <3.0.0"   # *, =, >, >=, <, <=, ^, ~ (UND-verknüpft)
  - id: stats
    optional: true              # fehlt sie, startet das Modul trotzdem (mit Warnung)
plugin-dependencies: [LuckPerms]  # Server-Plugins, deren Klassen das Modul nutzt (müssen aktiv sein)
load-order: 10                  # Start-Reihenfolge bei gleicher Abhängigkeitsebene (kleiner = früher)
resources: [config.yml, lang/en.yml]  # beim ersten Laden ins Datenverzeichnis kopiert (nie überschrieben)
```

| Feld | Pflicht | Prüfung |
|---|---|---|
| `id` | ja | Muster, reservierte IDs (`minecraft`, `bukkit`, `spigot`, `paper`, `papermc`, `craftbukkit`, `lemon`, `runtime`, `fork`) |
| `version` | ja | SemVer |
| `main` | ja | voll qualifizierter Klassenname; nicht in `java.*`, `javax.*`, `jdk.*`, `sun.*`, `dev.lemonpvp.runtime.*`, `org.bukkit.*`, `io.papermc.*`, `net.minecraft.*` |
| `api-version` | nein (Standard: Runtime-API) | ≥ 1, ≤ Runtime-API und in `allowed-api-versions` |
| `minecraft-versions` | nein (Standard: `["*"]`) | muss die laufende Version treffen; eine leere Liste ist ein Fehler |
| `capabilities` | nein (Standard: alle) | nur `events`, `commands`, `scheduler`, `services`; ist die Liste angegeben, verweigert die Fassade alles andere |
| `permissions` | nein | Knoten müssen mit `<id>.` beginnen; bereits existierende Knoten werden nicht überschrieben |
| `dependencies` | nein | gültige IDs/Bereiche, keine Selbstabhängigkeit, keine Duplikate; Zyklen werden beim Start erkannt |
| `resources` | nein | sichere relative Pfade (kein `..`, kein absoluter Pfad, kein `\`), max. 1 MiB je Datei |
| unbekannte Schlüssel | – | Fehler (Tippfehler sollen auffallen) |

Grenzen: Deskriptor ≤ 64 KiB, JAR ≤ 64 MiB, Listen ≤ 64 Einträge. YAML wird mit
SnakeYAML `SafeConstructor` gelesen (keine Objekterzeugung, keine doppelten Schlüssel).

### Integritätsinformationen

Ein Hash *innerhalb* der JAR wäre wertlos (wer die JAR ändert, ändert auch den Hash).
Integrität wird deshalb **außerhalb** der JAR festgehalten:

* `<jar>.sha256` im `sha256sum`-Format (`<hex>  <dateiname>`) – Pflicht bei
  `verify-integrity: true`;
* optional eine Allow-List (`trusted-hashes-file`) mit freigegebenen SHA-256-Werten;
* der Digest jeder aktivierten Version steht in Audit-Log, Diagnose und Laufzeitstatus.

```bash
sha256sum my-module.jar > my-module.jar.sha256
```

## 2. Programmiermodell

```java
public final class EventTools implements RuntimeModule, ConfigurableModule {
    @Override
    public void onEnable(ModuleContext ctx) {
        PaperModulePlatform paper = ctx.platform(PaperModulePlatform.class);
        paper.events().listen(PlayerJoinEvent.class, e -> { /* ... */ });
        paper.commands().register(CommandSpec.of("event").withPermission("event-tools.start"),
                (sender, label, args) -> { /* ... */ return true; });
        paper.scheduler().runTimer(this::tick, 20, 20);
        ExecutorService pool = ctx.resources().track("db pool", Executors.newFixedThreadPool(2)); // ExecutorService ist AutoCloseable (Java 19+)
    }
    @Override public void onDisable(ModuleContext ctx) { /* Zustand sichern */ }
    @Override public List<String> validateConfig(ConfigSnapshot c) { return List.of(); }
    @Override public void onConfigReload(ConfigSnapshot c) { /* neue Werte übernehmen */ }
}
```

| Methode | Wann | Regeln |
|---|---|---|
| Konstruktor (public, ohne Argumente) | beim Laden, Hauptthread | keine Seiteneffekte |
| `onLoad(ctx)` | einmal nach dem Instanziieren | keine Registrierungen (Ressourcen/Plattform sind noch gesperrt); die Runtime darf ein geladenes Modul verwerfen, ohne es je zu aktivieren |
| `onEnable(ctx)` | bei jeder Aktivierung (auch nach Rollback) | alle Registrierungen über `ctx`; Exception = Aktivierung abgebrochen, alles Registrierte wird freigegeben |
| `onDisable(ctx)` | bei jeder Deaktivierung | eigene Threads beenden; Registrierungen gibt die Runtime selbst frei |
| `validateConfig` / `onConfigReload` | `/fork module config <id>` | nur bei `ConfigurableModule` |

### Kontext (`ModuleContext`)

| Methode | Zweck |
|---|---|
| `descriptor()` | eigene Metadaten |
| `logger()` | Logger mit `[id]`-Präfix |
| `dataDirectory()` | `lemon/module-data/<id>/` |
| `config()` | letzte gültige `config.yml` als unveränderlicher `ConfigSnapshot` |
| `resources()` | eigene Ressourcen zur automatischen Freigabe registrieren |
| `services()` | Services anbieten (`services`-Fähigkeit) / finden |
| `runtime()` | Runtime-, API-, Minecraft-Version, Servermarke |
| `platform(PaperModulePlatform.class)` | Paper-Fassade (nur während `ENABLING`/`ENABLED`) |
| `isActive()` | für spät eintreffende asynchrone Callbacks |

### Regeln für entladbare Module

1. Alles über `ctx` registrieren – nie direkt `Bukkit.getPluginManager().registerEvents(...)`
   oder `Bukkit.getScheduler()`; diese Registrierungen kann die Runtime nicht entfernen.
2. Keine statischen Felder, die von außen (Server, Plugins, andere Module) referenziert
   werden; keine Modulobjekte in fremde Registries/Caches legen.
3. Eigene Threads/Executors in `onDisable` beenden oder über `ctx.resources().track(...)`
   registrieren. `ThreadLocal`s in fremden Threads vermeiden.
4. Asynchrone Arbeit muss terminieren; solange sie läuft, wird das Modul nicht entladen.
5. Weltzugriffe nur auf dem Hauptthread (`scheduler().run(...)`).
6. Spielereingaben in Nachrichten nur als `Placeholder.unparsed(...)` einsetzen.

## 3. Bauen

Gradle (Beispiel, siehe `runtime/example-module/build.gradle.kts`):

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    compileOnly(files("libs/runtime-api.jar", "libs/runtime-paper-api.jar")) // oder Projekt-/Maven-Abhängigkeit
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } } // 21 für 1.21.11
```

Die Runtime-API-JARs entstehen mit `cd runtime && ./gradlew :runtime-api:jar
:runtime-paper-api:jar -PpaperApiVersion=<version>` in `runtime/*/build/libs/`.
Abhängigkeiten, die der Server bereits mitbringt (Adventure, Gson, Guava, SnakeYAML),
nicht einbetten. Eigene Bibliotheken in die JAR schattieren und relocaten.

## 4. Installieren und aktivieren

```bash
cp event-tools.jar event-tools.jar.sha256 <server>/lemon/modules/
```
```
/fork module list                 # Datei erscheint als "not loaded"
/fork module validate event-tools # alle Prüfungen ohne Klassen zu laden
/fork module enable event-tools   # laden + aktivieren
/fork module diagnostics event-tools
```

Update: neue JAR + neue `.sha256` ablegen (gleiche ID, neue Version), dann
`/fork module reload event-tools`. Konfigurationsänderung: `lemon/module-data/event-tools/config.yml`
bearbeiten, `/fork module config event-tools`.

Vollständiges Beispiel: `runtime/example-module` (Join-Nachricht, `/greet`, Timer,
Konfigurations-Reload) und `docs/examples/`.
