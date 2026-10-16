# Live-Updates: Module, Plugins und Core-Patches

Lemon kann einen laufenden Server auf **drei getrennten Wegen** verändern. Sie haben
unterschiedliche Technik, Grenzen und Risiken und werden nie vermischt:

| Ziel | Was wird ausgetauscht | Technik | Rollback |
|---|---|---|---|
| **module** – Lemon-Runtime-Module | eine Modulinstanz (eigener Class-Loader) | alte Instanz deaktivieren, neue laden und aktivieren | automatisch, wenn die neue Version nicht startet |
| **plugin** – normale Paper/Bukkit-Plugins | ein Plugin (JAR) | Plugin entladen, neue JAR laden und aktivieren – **nur wenn die Kompatibilitätsprüfung das erlaubt** | automatisch bei Fehlschlag; manuell auf die vorherige JAR |
| **patch** – Core-Live-Patches | Bytecode bereits geladener Serverklassen (Paper, Minecraft, Lemon) | JVM-Klassen-Redefinition (`java.lang.instrument`) über einen Java-Agenten | Bytecode der vorherigen Version (nur im selben Prozess) |

Alle drei werden über **einen** Befehl bedient: `/fork live`. Ohne Präfix sucht Lemon den
Namen in allen drei Bereichen und verlangt bei Mehrdeutigkeit ein Präfix
(`module:`, `plugin:`, `patch:`).

> **Was Lemon nicht behauptet:** Es gibt kein „universelles“ Live-Patching. Auf einer
> Standard-JVM (Temurin, OpenJDK, Oracle) lassen sich nur **Methodenrümpfe** ändern.
> Strukturelle Änderungen (Methoden/Felder hinzufügen oder entfernen, Signaturen,
> Vererbung) brauchen eine JVM mit erweiterter Redefinition (JetBrains Runtime mit
> `-XX:+AllowEnhancedClassRedefinition`) – oder einen Neustart. Lemon prüft das vor jeder
> Anwendung durch echte Probe-Redefinitionen und lehnt ab, was die JVM nicht kann.

## 1. Befehle

| Befehl | Berechtigung | Wirkung |
|---|---|---|
| `/fork live status` | `lemon.runtime.live.view` (OP) | Übersicht: Module, Plugins, Core-Patches, letzte Aktionen |
| `/fork live scan` | view | was im Modulordner, in `plugins/update/` und im Patch-Ordner bereitliegt |
| `/fork live plugins` | view | installierte Plugins mit Status und „live möglich / nur Neustart“ |
| `/fork live modules` | view | Module im Modulordner mit Status |
| `/fork live inspect <name>` | view | Plugin: Version, Abhängigkeiten, **registrierte Ressourcen** (Listener, Scheduler-Tasks, Befehle, Services, Kanäle), live möglich?, entladene Versionen noch im Speicher?; Modul: Info + Diagnose; Patch: Status, Klassen, Sicherung |
| `/fork live check <name>` | view | Prüfung ohne Änderung: Kompatibilität (Plugin), Validierung (Modul), Änderungsplan (Patch) |
| `/fork live update <name>` | `lemon.runtime.live.update` (**niemand**, nur Konsole) | zeigt den Plan und wartet auf Bestätigung |
| `/fork live update <name> confirm` | update | führt den bestätigten Plan aus (innerhalb von 120 s) |
| `/fork live update all [confirm]` | update | alle Plugins in `plugins/update/`, die live gehen, nacheinander; stoppt beim ersten Fehlschlag (Module und Patches immer einzeln) |
| `/fork live rollback <name> [confirm]` | update | Plugin: vorherige JAR; Patch: Bytecode vor dem Patch |
| `/fork live history` | view | letzte 15 Aktionen dieses Serverprozesses (vollständig: `lemon/logs/audit.log`) |
| `/fork live capabilities` | view (Probe beim ersten Aufruf: update) | was in diesem Prozess live änderbar ist: Plugins, Module, Core-Patches (Fähigkeiten der JVM, per Probe ermittelt) |
| `/fork live diagnostics` | view | Agent/JVM, aktive Patches mit Prüfergebnis, entladene Plugin-/Modulversionen noch im Speicher, abgebrochene Staging-Dateien, letzte Fehlschläge |
| `/fork live patch status` | view | Agent, JVM, Build-ID, Profil, aktive Patches, Heap |
| `/fork live patch probe` | update | prüft mit Wegwerf-Klassen, welche Änderungsarten diese JVM kann |
| `/fork live patch verify <id>` | update | liest den laufenden Bytecode eines aktiven Patches aus der JVM zurück und vergleicht ihn mit dem Patch (siehe 6.1) |
| `/fork live patch inspect\|apply\|rollback <id> [confirm]` | wie `inspect`/`update`/`rollback` | Kurzformen mit festem Bereich `patch:` |

Weiterhin vorhanden (Details: [`HOT-DEPLOYMENT.md`](HOT-DEPLOYMENT.md)):
`/fork module list|info|validate|diagnostics|load|enable|disable|reload|unload|config|remove <id>`.

Ändernde Befehle (`update`, `rollback`, `probe`, `verify`) haben den Berechtigungs-Default `false`
und sind damit **nur über die Serverkonsole** möglich (RCON zählt nicht als Konsole).
Jede Änderung braucht zwei Schritte (Plan → `confirm`). Bestätigt wird genau der geprüfte
Stand: ändert sich die Datei zwischen Plan und Bestätigung (anderer SHA-256), wird
abgelehnt.

Einen Automatikmodus („neue Datei erkannt → sofort anwenden“) gibt es bewusst nicht: Lemon
führt nie eine Datei aus, nur weil sie in einem Ordner auftaucht. Die Automatisierung ist
`update all` (ein Plan, eine Bestätigung, dann nacheinander).

## 2. Was live geht und was einen Neustart braucht

| Änderung | Live? | Bedingung / Grund |
|---|---|---|
| Neue Version eines Lemon-Moduls | **ja** | keine aktivierten Module hängen davon ab; Hot-Swap mit automatischem Rollback |
| Konfiguration eines Moduls | **ja** | `ConfigurableModule`; `/fork module config <id>` |
| Neues Bukkit-Plugin (`plugin.yml`) installieren | **ja**, wenn die Prüfung „live“ ergibt | Abhängigkeiten installiert und aktiv, API-Version passt, keine Lifecycle-API |
| Bukkit-Plugin aktualisieren | **ja**, wenn die Prüfung „live“ ergibt | kein anderes aktives Plugin hängt davon ab (`depend`, `softdepend`, `loadbefore`), keine Lifecycle-API (alt und neu) |
| Plugin, von dem andere abhängen | nein → Neustart | andere Plugins halten Referenzen auf seine Klassen |
| Paper-Plugin (`paper-plugin.yml`) | nein → Neustart | Bootstrap-/Loader-Phase läuft nur beim Start |
| Plugin mit Paper-Lifecycle-API (z. B. Brigadier-Befehle über `LifecycleEvents.COMMANDS`) | nein → Neustart | Paper führt diese Handler nur beim Start aus |
| Methodenrumpf einer geladenen Serverklasse ändern (Paper, Minecraft, Lemon) | **ja** (Agent nötig) | Standard-JVM genügt |
| Konstante (`static final`) ändern | eingeschränkt | Feld behält den aktuellen Wert; Klassen, die die Konstante eingebettet haben, ändern sich nur, wenn sie im Patch enthalten sind |
| Methode/Konstruktor hinzufügen oder entfernen, Feld hinzufügen/entfernen, Modifikatoren, Oberklasse, Interfaces | nur mit JBR + `-XX:+AllowEnhancedClassRedefinition`, sonst Neustart | Standard-HotSpot lehnt das ab („attempted to add a method“ …) |
| Neue Klasse in einem bestehenden Paket | **ja** (Agent nötig) | wird vor der Redefinition definiert; bleibt bis zum Neustart geladen |
| Statischer Initialisierer (`<clinit>`) geändert | technisch ja, **ohne Wirkung** | läuft für eine bereits initialisierte Klasse nie erneut |
| Registries, Weltgenerierung, Netzwerkprotokoll, Datenformate | Neustart | Zustand ist beim Start festgelegt / an Sessions gebunden |
| Paper-Konfiguration, `server.properties` | Neustart (bzw. Paper-eigene Mechanismen) | nicht Teil von Lemon |

Zustand wird **nie** migriert: Objekte, statische Felder, Caches, laufende Tasks und
Threads behalten ihren Zustand. Ein Live-Patch ändert nur, welcher Code **ab dem nächsten
Aufruf** läuft. Bereits laufende Aufrufe (auch in anderen Threads) beenden sich mit dem
alten Code.

## 3. Voraussetzungen

| Bereich | Voraussetzung |
|---|---|
| alle | `config/lemon-runtime.yml`: `enabled: true` (Neustart) |
| Module | `allow-runtime-deploy: true` |
| Plugins | `plugin-updates.enabled: true` (sofort per `/fork reload`) |
| Core-Patches | `live-patching.enabled: true` (Neustart) **und** JVM-Option `-javaagent:lemon-livepatch-agent.jar` |
| Core-Patches, Produktion | `live-patching.trusted-hashes-file` gesetzt (Freigabeliste der Patch-Archive) |
| Core-Patches, strukturelle Änderungen | JetBrains Runtime (JBR) 21/25 mit `-XX:+AllowEnhancedClassRedefinition` |

```yaml
# config/lemon-runtime.yml (Ausschnitt)
enabled: true
allow-runtime-deploy: true          # Module
plugin-updates:
  enabled: true                     # Plugins
  trusted-hashes-file: ""           # optional: nur freigegebene Plugin-JARs
live-patching:
  enabled: true                     # Core-Patches
  profile: production               # production | development
  patch-directory: lemon/patches/inbox
  trusted-hashes-file: lemon/patches/approved.sha256
  confirm-timeout-seconds: 120
```

### Profile für Core-Patches

| | `production` (Standard) | `development` |
|---|---|---|
| Aktiv, wenn … | immer, außer die Bedingungen rechts sind erfüllt | `profile: development` **und** JVM-Start mit `-Dlemon.livepatch.development=true` |
| Freigabeliste | **Pflicht** – ohne Eintrag wird nichts angewendet | optional |
| `.sha256`-Datei | Pflicht | Pflicht |
| Build-ID des Servers unbekannt | Ablehnung | Warnung, nur Klassen-Hashes werden geprüft |
| Bestätigung (`confirm`) | Pflicht | Pflicht (es gibt keinen Automatikmodus) |

Steht in der Datei `development`, fehlt aber die JVM-Option, gilt `production` und
`/fork live patch status` sagt das. So kann eine kopierte Entwicklungs-Konfiguration einen
Produktionsserver nicht versehentlich lockern.

## 4. Schritt für Schritt

### 4.1 Plugin aktualisieren oder installieren

1. **Plugin bauen** (wie immer, z. B. `./gradlew build`).
2. **Datei ablegen:** neue JAR nach `plugins/update/` (SFTP). Pro Plugin genau **eine**
   JAR in diesem Ordner – Paper lädt beim Laden eines Plugins automatisch eine JAR aus
   `plugins/update/` mit demselben Plugin-Namen; liegen mehrere dort, verweigert Lemon.
   Optional `<jar>.sha256` daneben (wird dann geprüft).
3. **Kompatibilität prüfen:** `fork live check <Plugin>` – zeigt „can be updated live“,
   „needs a server restart“ oder „incompatible“ mit Gründen.
4. **Update ausführen:** `fork live update <Plugin>` → `fork live update <Plugin> confirm`.
   Ablauf: alte JAR (mit `.sha256`) nach `lemon/plugin-backups/<Plugin>/<Zeit>/` kopieren →
   neue JAR nach `lemon/plugin-staging/` kopieren und die Kopie erneut hashen (eine nach der
   Prüfung ausgetauschte Datei wird so nie geladen) → Plugin deaktivieren (Paper:
   `onDisable`, Tasks, Listener, Services, Kanäle, Class-Loader) → Lemon entfernt zusätzlich
   Befehle, Berechtigungen und den Plugin-Manager-Eintrag → geprüfte Kopie nach `plugins/`,
   Original aus `plugins/update/` entfernen → laden, Berechtigungen registrieren,
   aktivieren → Befehlsbaum der Spieler aktualisieren.
5. **Ergebnis prüfen:** Ausgabe `OK update of … done: 1.0.0 -> 1.0.1` mit
   `unloaded … (it had 1 listener, 1 scheduler task, 1 command; …)`. Danach prüft Lemon, ob
   für die **alte** Plugin-Instanz noch etwas registriert ist und ob Threads ihres
   Class-Loaders weiterlaufen; beides erscheint als `WARNING: …` (das Update ist trotzdem
   erfolgt, aber die alte Version bleibt im Speicher – bei Gelegenheit neu starten).
   `fork live inspect <Plugin>` zeigt später, ob der Class-Loader der alten Version
   eingesammelt wurde („class loader collected“) oder noch im Speicher ist. Lemon löst
   keine Garbage Collection aus; „still in memory“ direkt nach dem Update ist normal.
   Dann `fork live plugins` und das Plugin selbst testen. Schlägt Laden/Aktivieren fehl
   (oder lädt Paper eine andere Version), wird die alte JAR zurückgelegt und wieder
   geladen: `FAILED … the previous version … was restored and is enabled`.
   Zurück zur vorherigen Version: `fork live rollback <Plugin>` → `… confirm`. Ein weiterer
   Rollback geht eine Version weiter zurück; Sicherungen fehlgeschlagener Updates sind keine
   Rollback-Ziele. Die Sicherung wird über ihre `.sha256` geprüft (nicht über die
   Allow-List, auf der alte Versionen meist nicht stehen).

Ist ein Live-Update nicht möglich, bleibt die JAR in `plugins/update/`; Paper installiert
sie beim nächsten Start.

Mehrere Plugins auf einmal: alle JARs nach `plugins/update/`, dann `fork live update all`
(prüft jedes, listet „ready“/„skipped“) → `fork live update all confirm`. Nacheinander, mit
denselben Prüfungen und Wiederherstellungen wie einzeln; beim ersten Update, das nicht
gelingt, stoppt Lemon und nennt die nicht versuchten Plugins.

Was Lemon über ein Plugin **sieht** (Server-API): Event-Listener, Tasks des
Bukkit-Schedulers, Befehle, Services, Plugin-Messaging-Kanäle. **Nicht** sichtbar: Tasks
der Paper-Async-/Region-Scheduler (Paper beendet sie beim Deaktivieren), selbst gestartete
Threads/Executor (nur über den Thread-Check nach dem Entladen), Registrierungen in
APIs anderer Plugins (Paket-Listener, Platzhalter-Erweiterungen, Hooks …) und
Brigadier-Befehle über Papers Lifecycle-API. Deshalb gibt es kein „universell sicheres“
Plugin-Reload: Lemon tauscht nur Plugins live aus, bei denen die Prüfung das erlaubt, und
meldet, was nach dem Entladen übrig blieb.

### 4.2 Modul aktualisieren oder neu aktivieren

1. **Modul bauen** (Format: [`MODULE-FORMAT.md`](MODULE-FORMAT.md); `module.yml` genügt,
   z. B. nur `id`, `version`, `main`).
2. **Datei ablegen:** JAR + `<jar>.sha256` nach `lemon/modules/`.
3. **Prüfen:** `fork live check <modul-id>` (Validierung ohne Klassen zu laden).
4. **Ausführen:** `fork live update <modul-id>` → `fork live update <modul-id> confirm`
   (nicht geladen: aktivieren; geladen: Hot-Swap mit Rollback).
5. **Ergebnis prüfen:** `fork module diagnostics <modul-id>`, `fork live history`.
   Entfernen: `fork module remove <modul-id>` (verschiebt die Datei nach
   `lemon/modules-removed/`).

### 4.3 Core-Patch (Änderung an Paper/Minecraft/Lemon-Code)

1. **Basis festhalten:** direkt nach dem Bauen der Server-JAR, die deployt wird (gleicher
   Quellstand, noch keine Änderungen): `./gradlew :lemon-server:livePatchBaseline` (kopiert
   die Gradle-JAR nach `lemon-server/build/livepatch/baseline/baseline.jar`).
2. **Laufende Server-JAR holen:** Paperclip schreibt beim ersten Start die Server-JAR, die
   wirklich läuft, nach `versions/<mc>/lemon-<mc>.jar` im Serververzeichnis. Diese Datei per
   SFTP holen (z. B. nach `C:\lemon\lemon-<mc>.jar`). **Bei 1.21.11 ist das Pflicht:** die
   1.21.11-Paperclip erzeugt diese JAR aus Vanilla-JAR + Binär-Patches, und dabei sind die
   Klassendateien anders angeordnet als in der Gradle-JAR (CI-Lauf #12: 9962 von 10183
   Klassen byte-verschieden bei gleichem Inhalt). Bei 26.1.2/26.2/26.3 ist sie
   byte-identisch zur Gradle-JAR (CI-Lauf #12: alle Klassen identisch), `-PserverJar` schadet
   dort nicht.
3. **Code ändern und Patch bauen** (IntelliJ oder Konsole):
   `./gradlew :lemon-server:createLivePatch -PpatchId=fix-tps -PpatchDescription="…" -PserverJar=C:\lemon\lemon-<mc>.jar`
   (optional `-PdeployDir=<vorhandener Ordner>`: kopiert Patch + `.sha256` dorthin, z. B. in
   den `lemon/patches/inbox`-Ordner eines lokalen Testservers; angewendet oder freigegeben
   wird dadurch nichts).
   Das Werkzeug erkennt Änderungen byte-genau gegen `baseline.jar`, übernimmt die
   Prüfsummen aus der laufenden JAR und prüft, dass beide für jede Patch-Klasse
   inhaltlich gleich sind (sonst Abbruch: „--reference is not the build that runs on the
   server“). Es zeigt pro Klasse die Änderungen (strukturelle mit `[structural]`) und
   schreibt `lemon-server/build/livepatch/fix-tps.lemonpatch` + `.sha256`.
4. **Offline prüfen (vor dem Hochladen):**
   `./gradlew :lemon-server:checkLivePatch -PpatchId=fix-tps -PserverJar=C:\lemon\lemon-<mc>.jar`
   prüft Prüfsummendatei, Archiv, Build-ID, Minecraft-Version, den SHA-256 jeder ersetzten
   Klasse in der Server-JAR und die Art jeder Änderung. Ergebnis: `APPLICABLE on a standard
   JVM`, `APPLICABLE ONLY with enhanced class redefinition` (JBR nötig) oder
   `NOT APPLICABLE …` (Build schlägt fehl). Der Server prüft beim Anwenden alles erneut,
   zusätzlich seine JVM.
5. **Übertragen:** beide Dateien per SFTP nach `lemon/patches/inbox/`. In Produktion
   trägt die freigebende Person den SHA-256 in die Freigabeliste ein
   (`sha256sum fix-tps.lemonpatch >> lemon/patches/approved.sha256`).
6. **Prüfen und anwenden:** `fork live check fix-tps` zeigt den Plan
   (redefine/define/reject je Klasse, Änderungsarten, ob die JVM sie kann);
   `fork live update fix-tps` → `fork live update fix-tps confirm`.
7. **Ergebnis prüfen:** Meldung `OK patch fix-tps applied and verified` mit
   `<Klasse>: runs the expected bytecode (read back from the JVM)` (siehe 6.1) und
   `behaviour NOT validated by Lemon` – den geänderten Codepfad jetzt testen.
   `fork live inspect patch:fix-tps`, `fork live patch verify fix-tps` (erneut
   zurücklesen), `fork live history`. Rückgängig: `fork live rollback fix-tps`
   → `… confirm` (wird ebenfalls zurückgelesen: `rolled back and verified`). **Dauerhaft** wird eine Änderung erst mit einer neu gebauten Server-JAR
   und Neustart: Patches gelten nur für den laufenden Prozess.

Was der Server vor dem Anwenden prüft (jede fehlgeschlagene Prüfung lehnt den **ganzen**
Patch ab, bevor etwas geändert wird):

1. Live-Patching aktiviert, Agent geladen, JVM kann redefinieren.
2. Datei ≤ 16 MiB, nur `lemon-patch.yml` und `classes/**.class`, keine Pfadtricks,
   ≤ 256 Klassen, Digest jeder Klassendatei passt zum Manifest, jede Klassendatei ist
   gültig und heißt wie angegeben, keine Klassen aus `java.*`, `javax.*`, `jdk.*`,
   `sun.*`, `com.sun.*`, nicht der Live-Patch-Code selbst und nicht die Klassen, auf die
   sich seine Prüfungen stützen (`dev.lemonpvp.runtime.security.*`, `…config.*`).
3. `.sha256` passt zu genau den gelesenen Bytes; in Produktion steht der Digest in der
   Freigabeliste.
4. Ziel-Minecraft-Version und **Build-ID** (`Brand-Id/Implementation-Version/Build-Time`
   aus dem Manifest der Server-JAR) stimmen.
5. Je Klasse: vom Server-Class-Loader geladen bzw. ladbar, von der JVM als änderbar
   gemeldet, **laufender Bytecode hat genau den SHA-256, gegen den der Patch gebaut wurde**
   (oder ist die Version eines früheren Patches derselben Basis).
6. Je Änderung: Art bestimmt (Methodenrumpf, Methode hinzugefügt, Feld entfernt, …) und
   die nötige Fähigkeit dieser JVM per Probe bestätigt.
7. Bytecode-Verifikation: Standard-HotSpot verifiziert redefinierte Klassen selbst und
   lehnt ungültigen Bytecode mit `VerifyError` ab. **JetBrains Runtime mit
   `-XX:+AllowEnhancedClassRedefinition` hat in der CI (JBR 21.0.11 und 25.0.4.1)
   ungültigen Bytecode ohne Fehler übernommen.** Lemon prüft deshalb per Probe
   („reject invalid bytecode when redefining“), ob die JVM verifiziert; wenn nicht, läuft
   der JVM-Verifier vorher auf jeder Klasse des Patches in einem Wegwerf-Class-Loader
   (ohne statische Initialisierer). Ungültige Klassen ⇒ Ablehnung. Klassen, die sich so
   nicht prüfen lassen (z. B. paketprivate Oberklasse), lehnt das Produktionsprofil ab;
   das Entwicklungsprofil wendet sie mit Warnung an.

Anwendung: Sicherung der laufenden Klassendateien nach
`lemon/patches/backups/<id>/<Zeit>/` (+ `backup.txt` mit SHA-256), dann **ein**
`Instrumentation.redefineClasses`-Aufruf mit allen Klassen auf dem Hauptthread zwischen
zwei Ticks. Die JVM (JVMTI `RedefineClasses`) ändert dabei alle Klassen oder keine.

### 4.4 Diagnose

| Frage | Befehl |
|---|---|
| Was kann dieser Prozess überhaupt live ändern? | `fork live capabilities` |
| Was liegt bereit, was davon geht live? | `fork live scan` |
| Was hält ein Plugin gerade, wo ist es im Speicher geblieben? | `fork live inspect <Plugin>` |
| Läuft der Bytecode eines Patches noch? | `fork live patch verify <id>` |
| Gesamtbild inkl. letzter Fehlschläge | `fork live diagnostics` |
| Was ist wann passiert? | `fork live history`, vollständig `lemon/logs/audit.log` |

Häufige Meldungen: [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md).

### 4.5 Wann ein Neustart unvermeidbar ist

* Core-Änderung, die nicht nur Methodenrümpfe betrifft, auf einer Standard-JVM
  (`checkLivePatch`: `APPLICABLE ONLY with enhanced class redefinition`; Server: `reject …
  this JVM cannot …`).
* Änderungen, die Lemon nie live anwendet (Oberklasse auf Standard-JVM, Nest-/Sealed-/
  Record-Attribute, Klassen-Modifikatoren) – `NOT APPLICABLE live`.
* Änderungen an Code, der nur beim Start läuft (statische Initialisierer, Registries,
  Bootstrap, Weltgenerierung beim Laden) – technisch anwendbar, aber ohne Wirkung.
* Plugin mit abhängigen Plugins, Paper-Plugin (`paper-plugin.yml`), Plugin mit Paper-
  Lifecycle-API – `needs a server restart`.
* Dauerhaftigkeit: Jeder Core-Patch endet mit dem Prozess. Für den nächsten Start die
  Server-JAR neu bauen und deployen.

## 5. Entwicklungs-Workflow: IntelliJ (Windows) → Testserver (Ubuntu 24.04 / Pterodactyl)

```
IntelliJ (Windows)                         Testserver (Ubuntu, Pterodactyl)
───────────────────                        ─────────────────────────────────
1 versions/<mc>: applyAllPatches, build,
  create(Mojmap)PaperclipJar               ──SFTP──► server.jar (+ lemon-livepatch-agent.jar)
2 :lemon-server:livePatchBaseline                   Start mit -javaagent:lemon-livepatch-agent.jar
  laufende JAR holen                      ◄──SFTP── versions/<mc>/lemon-<mc>.jar (von Paperclip geschrieben)
3 Code ändern (paper-server/src/main/java,
  lemon-server/src/minecraft/java)
4 :lemon-server:createLivePatch -PpatchId=x
    -PserverJar=<geholte JAR>
  :lemon-server:checkLivePatch -PpatchId=x
    -PserverJar=<geholte JAR>  (offline)  ──SFTP──► lemon/patches/inbox/x.lemonpatch + .sha256
                                                    (Produktion: Digest in approved.sha256)
                                                    Konsole: fork live check x
                                                             fork live update x
                                                             fork live update x confirm
5 dauerhaft: neue JAR bauen, deployen, Neustart
```

* Unter Windows `gradlew.bat` statt `./gradlew`. Gebaut wird das Projekt
  `versions/<mc>/` (siehe [`BUILDING.md`](BUILDING.md)); IntelliJ öffnet es als
  Gradle-Projekt.
* Der Agent liegt im CI-Artefakt `lemon-<mc>` (`lemon-livepatch-agent.jar` + `.sha256`)
  oder entsteht lokal mit `cd runtime && ./gradlew :livepatch-agent:jar`
  (`runtime/livepatch-agent/build/libs/lemon-livepatch-agent.jar`).

### Pterodactyl

* **Startbefehl** (Server → Startup; das Egg muss Änderungen erlauben, sonst Admin):
  `-javaagent:lemon-livepatch-agent.jar` **vor** `-jar` einfügen, z. B.
  ```
  java -Xms128M -XX:MaxRAMPercentage=95.0 -javaagent:lemon-livepatch-agent.jar -jar {{SERVER_JARFILE}}
  ```
  Die Agent-JAR liegt im Serververzeichnis (`/home/container`). Für JBR zusätzlich
  `-XX:+AllowEnhancedClassRedefinition` und ein Docker-Image mit JetBrains Runtime
  (Standard-Images enthalten Temurin/OpenJDK – dort sind nur Methodenrümpfe möglich).
* Entwicklungsprofil nur auf dem Testserver: `-Dlemon.livepatch.development=true` im
  Startbefehl **und** `profile: development`.
* **Übertragung** per SFTP des Panels (Benutzer/Port stehen unter „Settings → SFTP
  Details“). Der Patch-Ordner nimmt nur Dateien entgegen – ausgeführt wird erst nach
  `check`/`update`/`confirm` an der Konsole. Es gibt keinen Upload-Endpunkt und keinen
  Netzwerkport von Lemon.
* `fork …`-Befehle in der Panel-Konsole eingeben (das ist die Serverkonsole).

### Alternative: IntelliJ-HotSwap über JDWP (nur lokaler Entwicklungsserver)

IntelliJ kann geänderte Klassen über den Debugger (JDWP) direkt einspielen. Das nutzt
dieselbe JVM-Redefinition (gleiche Grenzen), umgeht aber alle Lemon-Prüfungen, Sicherungen
und das Audit-Log. **JDWP hat keine Authentifizierung: Wer den Port erreicht, kann
beliebigen Code im Server ausführen.** Lemon öffnet keinen JDWP-Port. Wenn überhaupt, dann:

```
# nur Entwicklungsserver; Port NICHT im Panel als Allocation freigeben
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5005
```

Zugriff ausschließlich über einen SSH-Tunnel (`ssh -L 5005:127.0.0.1:5005 user@host`;
läuft der Server in einem Pterodactyl-Container, ist `127.0.0.1` der Container selbst –
dann an die Container-Adresse binden, den Port nicht veröffentlichen und den Tunnel auf
diese Adresse richten). Niemals auf Produktionsservern, niemals `address=*:5005` oder
`0.0.0.0` mit veröffentlichtem Port.

## 6. Synchronisation, Atomarität, Rollback – was garantiert ist und was nicht

* **Ein Patch, ein JVM-Aufruf.** Alle Klassen eines Patches gehen in einen
  `redefineClasses`-Aufruf. Lehnt die JVM eine Klasse ab (z. B. `VerifyError`), wird
  laut JVMTI-Spezifikation keine Klasse geändert. Neue Klassen werden vorher definiert;
  sie bleiben bei einem Fehlschlag geladen, aber unbenutzt (wird gemeldet).
* **Keine Tick-Atomarität über Threads hinweg.** Die Redefinition läuft auf dem
  Hauptthread zwischen zwei Scheduler-Aufgaben, also nicht mitten in Tick-Code des
  Hauptthreads. Andere Threads (Netzwerk, Chunk-I/O, async Tasks) können in diesem Moment
  in alten Methodenversionen stehen und beenden diese mit dem alten Code.
* **Kein Zustand wird repariert oder migriert.** Felder, Objekte, Caches behalten ihre
  Werte. Ein Patch, der neue Annahmen über bestehenden Zustand macht, kann falsch sein,
  obwohl er „technisch angewendet“ wurde – Lemon prüft Bytecode-Kompatibilität, nicht
  Verhalten.
* **Rollback = Bytecode-Rollback.** Nur der vorherige Bytecode wird wiederhergestellt
  (aus dem Speicher des Prozesses; die Sicherung auf der Platte dient der
  Nachvollziehbarkeit). Spielzustand, Weltänderungen, Datenbankeinträge, versendete
  Pakete werden **nicht** zurückgenommen. Rollback ist nur für die jeweils neueste Version
  einer Klasse möglich (LIFO) und nur, wenn die JVM die Rückänderung kann (bei
  strukturellen Patches also nur mit JBR).
* **Kein Überleben über Neustarts.** Nach einem Neustart läuft wieder die Server-JAR.
  Ein Patch-Datensatz eines früheren Prozesses kann nicht zurückgerollt werden.
* **Plugins:** Zwischen Entladen und Laden gibt es eine kurze Lücke (ein
  Hauptthread-Schritt); Paper selbst entlädt Plugins nicht offiziell. Lemon entfernt
  Befehle, Berechtigungen und den Plugin-Manager-Eintrag zusätzlich zu Papers
  `disablePlugin`. Objekte, Threads oder statische Zustände, die ein Plugin außerhalb der
  Bukkit-API angelegt hat, räumt niemand auf – deshalb die konservative Prüfung.
* **Fehlerfälle werden nie als Erfolg gemeldet.** Jede Ablehnung beginnt mit
  `NOT DONE`, jeder Fehlschlag mit `FAILED`; Details stehen im Server-Log und im Audit-Log.

### 6.1 Prüfung nach dem Anwenden (Rücklesen des Bytecodes)

Nach jeder Redefinition (Anwenden und Rollback) fragt Lemon die JVM nach der Klassendatei,
die sie **jetzt** ausführt (`Instrumentation.retransformClasses` mit einem Transformer, der
die Bytes nur mitschreibt und nie ändert; HotSpot rekonstruiert die Datei aus der geladenen
Klasse). Diese wird mit der angewendeten Klassendatei verglichen (Member, Modifikatoren,
Hierarchie, normalisierter Bytecode jeder Methode). Ergebnis:

| Meldung | Bedeutung |
|---|---|
| `applied and verified` / `verified: the JVM runs the expected bytecode` | die JVM führt für jede Klasse genau den Bytecode des Patches aus |
| `VERIFICATION FAILED … DIFFERS` | die JVM führt anderen Bytecode aus (z. B. hat ein anderer Agent die Klasse verändert) |
| `not verified: …` | Zurücklesen nicht möglich (alter Agent ohne `Can-Retransform-Classes`, JVM ohne Unterstützung) oder die Änderung ist im Bytecode nicht sichtbar (nur Debug-Infos/Annotationen) |

Grenzen: Belegt wird, **welcher Bytecode läuft** – nicht, dass er sich richtig verhält.
Beim Zurücklesen nummeriert HotSpot den Konstantenpool um und verbreitert dabei z. B.
`ldc` zu `ldc_w`; der Vergleich ist dagegen unempfindlich (Sprungziele als
Befehlsnummern), das ist durch einen Test mit echter Redefinition belegt. Das Zurücklesen
ist selbst eine Redefinition mit demselben Bytecode (ein weiterer kurzer Safepoint).
Ab Agent-Version 2 deklariert `lemon-livepatch-agent.jar` dafür
`Can-Retransform-Classes: true`; ein Transformer ist nur während des Zurücklesens
registriert.

## 7. Dateien

```
lemon/patches/inbox/<id>.lemonpatch (+ .sha256)  hochgeladene Core-Patches
lemon/patches/approved.sha256                    Freigabeliste (Produktion), Pfad konfigurierbar
lemon/patches/backups/<id>/<Zeit>/               Klassendateien vor dem Patch + backup.txt
plugins/update/                                  neue Plugin-JARs (Paper-Standardordner)
lemon/plugin-backups/<Plugin>/<Zeit>/            vorherige Plugin-JARs; UPDATE-FAILED.txt markiert Fehlversuche
lemon/plugin-staging/                            geprüfte Kopie während eines Updates (danach leer)
lemon/modules-removed/<Zeit>/                    mit /fork module remove entfernte Module
lemon/logs/audit.log                             alle Aktionen: Zeit | Akteur | Aktion | Ziel | OK/FAILED | Details
```

### Patch-Format (`.lemonpatch`)

ZIP mit `lemon-patch.yml` und `classes/<paket>/<Klasse>.class`:

```yaml
format: 1
id: "fix-tps"
description: "…"
created: "2026-10-14T12:00:00Z"
tool: "lemon-livepatch-tool 1"
target:
  build-id: "lemonpvp:lemon/26.2-123-abcdef0/1970-01-01T00:00:00Z"
  minecraft: "26.2"
classes:
  - name: "org.spigotmc.TicksPerSecondCommand"
    sha256: "…"          # neue Klassendatei
    base-sha256: "…"     # Klassendatei, gegen die gebaut wurde
  - name: "org.example.NewHelper"
    sha256: "…"
    new: true
```

Das Werkzeug ist auch ohne Gradle nutzbar (nur JDK, liegt in der Server-JAR):

```
java -cp <server-jar oder kompilierte Klassen> dev.lemonpvp.runtime.livepatch.tool.LivePatchTool create \
     --base <laufende Server-JAR> --classes <Klassenverzeichnis> [--reference <Gradle-JAR desselben Builds>] \
     --id fix-tps [--description …] [--out …]
java -cp <…> dev.lemonpvp.runtime.livepatch.tool.LivePatchTool check \
     --patch fix-tps.lemonpatch --server-jar <laufende Server-JAR>      # Exit 0 / 3 (nur JBR) / 1
```

## 8. Teststand

Was lokal, in der CI auf echten Paper-Servern und gar nicht getestet wurde, steht in
[`TESTING.md`](TESTING.md) (Abschnitt „Live-Updates“).
