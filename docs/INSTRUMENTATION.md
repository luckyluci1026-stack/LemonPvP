# JVM-Instrumentation: Klassen untersuchen, redefinieren und retransformieren

Lemon bringt `java.lang.instrument` direkt in den Paper-Fork: einen eigenen Java-Agenten,
eine Startdiagnose, eine Suche über alle geladenen Klassen, einen kontrollierten
`ClassFileTransformer` und zwei Wege, einen Patch in die laufende JVM zu bringen –
**Redefinition** und **Retransformation**. Alles läuft über die vorhandenen Bausteine des
Core-Live-Patchings ([`LIVE-UPDATES.md`](LIVE-UPDATES.md)): dasselbe Patch-Format
(`.lemonpatch`), dieselben Prüfungen, dieselbe Bestätigung, Sperre, Sicherung, Audit-Log
und Historie. Neu ist der Befehl `/fork instrumentation`.

> **Was Lemon nicht behauptet:** Es gibt kein universelles Live-Patching. Auf einer
> Standard-JVM ändern beide Wege nur **Methodenrümpfe** (plus Konstantenpool und
> Attribute). `isModifiableClass(...) == true` heißt nur, dass die JVM die Klasse
> grundsätzlich umdefinieren lässt – nicht, dass jede Änderung erlaubt ist. Ein normaler
> vollständiger Paper-Build ist kein Patch: angewendet werden nur Klassendateien, die das
> Patch-Werkzeug gegen die laufende Server-JAR gebaut und geprüft hat.

## 1. Aufbau

```
lemon-livepatch-agent.jar                 Server (Paperclip-Class-Loader)
 ├─ premain   (-javaagent, beim Start) ┐
 └─ agentmain (attach <pid>, später)   ┴─► Instrumentation ──► InstrumentationAccess (sucht den Agenten
                                                                im System-Class-Loader, auch nach einem Attach)
                                                                      │
             InstrumentationDiagnostics ◄── LivePatchService ─────────┤
             (Startprüfung, Status)          │  Plan → confirm → Sicherung → Hauptthread
                                             ├─ REDEFINE:    redefineClasses(ClassDefinition…)
                                             ├─ RETRANSFORM: PatchTransformer + retransformClasses(…)
                                             └─ Rücklesen:   AppliedBytecodeCheck (Recorder-Transformer)
             ClassCatalog ◄── getAllLoadedClasses (Suche, Komponenten, Inspect)
             /fork instrumentation …  und  /fork live patch …   (gleicher Service)
```

| Datei | Aufgabe |
|---|---|
| `runtime/livepatch-agent/…/LemonLivePatchAgent.java` | Agent (Version 3): `premain` und `agentmain`, hält nur die `Instrumentation` |
| `runtime/livepatch-agent/…/AttachLauncher.java` | `java -jar lemon-livepatch-agent.jar list` / `attach <pid>` (JDK-Attach-API) |
| `runtime/livepatch-agent/build.gradle.kts` | Manifest: `Premain-Class`, `Agent-Class`, `Main-Class`, `Can-Redefine-Classes`, `Can-Retransform-Classes` |
| `…/livepatch/InstrumentationAccess.java` | findet den Agenten; `LivePatchService` sucht erneut, solange keiner gefunden wurde |
| `…/livepatch/InstrumentationDiagnostics.java` | Startdiagnose: was die JVM kann, was fehlt und wie man es behebt |
| `…/livepatch/PatchTransformer.java` | Lemons einziger `ClassFileTransformer` (Retransformations-Patches) |
| `…/livepatch/Mechanism.java` | `REDEFINE` / `RETRANSFORM` |
| `…/livepatch/ClassCatalog.java` | Suche und Beschreibung geladener Klassen |
| `…/livepatch/LivePatchService.java`, `PatchPlanner.java` | Prüfung, Anwendung, Rollback, Rücklesen für beide Wege |
| `…/commands/InstrumentationCommands.java` | `/fork instrumentation …` |

## 2. Agent bauen und Server starten

**Bauen** (automatisch in CI, Artefakt `lemon-<mc>` enthält `lemon-livepatch-agent.jar` +
`.sha256`; lokal):

```
cd runtime && ./gradlew :livepatch-agent:jar
→ runtime/livepatch-agent/build/libs/lemon-livepatch-agent.jar
```

**Weg A – beim Start (empfohlen, auch für Produktion):** JAR ins Serververzeichnis, dann

```
java -Xms2G -Xmx4G -javaagent:lemon-livepatch-agent.jar -jar lemon-<mc>.jar --nogui
```

(Pterodactyl: `-javaagent:lemon-livepatch-agent.jar` im Startbefehl **vor** `-jar`.)
Für Patches zusätzlich in `config/lemon-runtime.yml` `live-patching.enabled: true`
(Neustart), siehe [`LIVE-UPDATES.md`](LIVE-UPDATES.md) §3.

**Weg B – in einen laufenden Server anhängen (Entwicklungsserver):**

```
java -jar lemon-livepatch-agent.jar list             # JVMs dieses Benutzers
java -jar lemon-livepatch-agent.jar attach <pid>     # lädt den Agenten (agentmain)
```

* Läuft als **derselbe Betriebssystem-Benutzer** wie der Server, über den lokalen
  Attach-Mechanismus des JDK (Datei-Socket, kein Netzwerkport). Der Befehl braucht ein JDK
  (Modul `jdk.attach`); der Server selbst darf auf einer JRE laufen.
* Nicht möglich, wenn der Server mit `-XX:+DisableAttachMechanism` läuft. In
  Pterodactyl-Containern hat man meist keine Shell als Serverbenutzer – dort Weg A.
* Java 21+ kann beim Anhängen eine Warnung ausgeben („A Java agent has been loaded
  dynamically …“, JEP 451); laut JEP 451 soll das dynamische Laden in einer künftigen
  Java-Version standardmäßig verboten werden. Gemessen: `EnableDynamicAgentLoading` steht auf
  OpenJDK 21 standardmäßig auf `true` (`{default}`); ein einfaches Testprogramm auf OpenJDK
  21.0.12 gab die Warnung ohne Option aus und mit `-XX:+EnableDynamicAgentLoading` nicht. Auf
  den CI-Paper-Servern (Java 21 und 25, CI-Lauf #41) stand keine Warnung im Serverlog – warum,
  ist nicht geklärt. `/fork instrumentation status` zeigt Wert und Herkunft des Flags
  (z. B. `dynamic attach: EnableDynamicAgentLoading=true (default)`). Wer dynamisches Laden
  ausdrücklich erlauben will, startet mit `-XX:+EnableDynamicAgentLoading`.
* Lemon bemerkt den Agenten beim nächsten Befehl ohne Neustart
  (`Live-patch agent found: agent 3 loaded via agentmain (dynamic attach)`).
* Klassen, die vor dem Anhängen geladen wurden, bekommt die JVM für Retransformationen aus
  ihrer internen Darstellung rekonstruiert (gleichwertig, nicht byte-identisch). Das
  funktioniert (Test `DynamicAttachTest`, Servertest 10), ist aber ein Grund mehr, in
  Produktion Weg A zu nehmen.

Lemon verändert die Produktions-JVM nicht: kein JVM-Wechsel, keine Flags werden gesetzt,
kein Debug-Port geöffnet. JDWP (IntelliJ-HotSwap) bleibt eine separate Option für lokale
Entwicklungsserver ([`LIVE-UPDATES.md`](LIVE-UPDATES.md) §5).

## 3. Prüfung beim Start

Beim Start der Runtime protokolliert Lemon (Präfix `Instrumentation:`), wenn
`live-patching.enabled` an ist oder ein Agent geladen ist:

* JVM, Java-Version; Agent geladen? wie (`-javaagent` / `agentmain`)? Version?
* `redefineClasses` / `retransformClasses` unterstützt?
* Anzahl geladener, änderbarer und versteckter Klassen (Arrays, primitive Typen und
  versteckte Klassen wie Lambdas sind nie änderbar)
* Standard-Redefinition oder `AllowEnhancedClassRedefinition=true`
* `EnableDynamicAgentLoading`, `DisableAttachMechanism`
* **Warnung**, wenn ein JDWP-Debug-Port nicht nur auf `127.0.0.1` lauscht
* für jede Lücke die Abhilfe, z. B. ohne Agent:
  `fix: add -javaagent:lemon-livepatch-agent.jar before -jar … or … attach <pid>`

Dieselben Punkte zeigt `/fork instrumentation status` jederzeit (dann auch mit dem Zustand
von Lemons Transformer).

## 4. Befehle

| Befehl | Recht | Wirkung |
|---|---|---|
| `/fork instrumentation status` | `lemon.runtime.live.view` | Startdiagnose (aktuell), Transformer, Klassen mit Retransformations-Patch, aktive Patches |
| `/fork instrumentation capabilities` | view (erste Probe: update) | was die JVM ändern kann, per Probe mit Wegwerf-Klassen, inkl. „change method bodies by retransformation (and restore them)“ |
| `/fork instrumentation classes <filter> [komponente] [seite]` | view | Suche in `getAllLoadedClasses()` |
| `/fork instrumentation inspect <klasse> [n]` | view | Details einer geladenen Klasse |
| `/fork instrumentation inspect <klasse> [n] verify` | `lemon.runtime.live.update` + `live-patching.enabled` | liest den laufenden Bytecode zurück und vergleicht ihn mit der Klassendatei (eine Retransformation) |
| `/fork instrumentation patch check <id> [redefine\|retransform]` | view | Plan ohne Änderung |
| `/fork instrumentation patch apply <id> [redefine\|retransform] [confirm]` | update | Plan → Bestätigung → Anwendung (Standard: `redefine`) |
| `/fork instrumentation patch rollback <id> [confirm]` | update | Patch zurücknehmen (Weg wie beim Anwenden) |
| `/fork instrumentation patch verify <id>` | update | laufenden Bytecode eines aktiven Patches zurücklesen |
| `/fork instrumentation history` | view | Patches dieses Prozesses mit Weg und Status |

`lemon.runtime.live.update` hat den Default `false` – ändernde Befehle gehen nur an der
Konsole. Ohne `live-patching.enabled: true` lehnt Lemon jede Änderung ab
(`live patching is disabled`), lesen geht trotzdem. Das Produktionsprofil wendet nur
Patches aus der Freigabeliste an; das Entwicklungsprofil gilt nur mit
`-Dlemon.livepatch.development=true`. `/fork live patch …` funktioniert weiter wie bisher
(immer `redefine`).

### Klassensuche

```
/fork instrumentation classes spigotmc.TicksPerSecond paper
/fork instrumentation classes org.bukkit.craftbukkit.*Command craftbukkit 2
/fork instrumentation classes * plugin
```

* Filter: Teil des Namens (ohne Groß-/Kleinschreibung), Muster mit `*` (ganzer Name) oder
  `*` für alle; erlaubt sind nur Buchstaben, Ziffern und `. _ $ - *`.
* Komponenten: `jdk`, `minecraft` (`net.minecraft`, `com.mojang`), `craftbukkit`, `paper`
  (`io.papermc.paper`, `com.destroystokyo.paper`, `org.spigotmc`, `ca.spottedleaf`),
  `bukkit` (API), `lemon` (`dev.lemonpvp` im Server-Class-Loader), `module` (Lemon-Modul,
  mit Modul-ID), `plugin` (mit Pluginname, erkannt am Class-Loader des Plugins), `agent`,
  `library` (übrige Klassen des Servers: Netty, Adventure, …), `other`.
* Ausgabe: 12 Zeilen pro Seite, Gesamtzahl, Dauer; je Klasse Komponente, `modifiable` /
  `NOT modifiable`, `hidden`, und ob Lemon sie geändert hat (`redefined by …`,
  `retransformed by …`). `isModifiableClass` wird nur für die angezeigten Zeilen gefragt.
  Array-Klassen werden nicht aufgeführt. Die Suche läuft auf dem Worker-Thread.
* Es werden nur **geladene** Klassen gefunden; Lemon lädt keine Klasse, nur um sie zu zeigen.

### Inspect

`/fork instrumentation inspect org.spigotmc.TicksPerSecondCommand` zeigt Art (Klasse,
Interface, Enum, Record, versteckt), Komponente, Class-Loader, JPMS-Modul, **Code-Source
so, wie der Class-Loader sie meldet** (bei Bootstrap-/versteckten Klassen keine),
`isModifiableClass` mit Begründung bei `false`, ob die JVM redefinieren/retransformieren
kann, die Klassendatei über den definierenden Class-Loader (Größe, Version, Methoden,
Felder) mit **SHA-256 – genau gegen diesen Stand muss ein Patch gebaut sein** – und Lemons
Änderung (Patch, Transformer-Schichten, wie oft geliefert, ggf. `DISPLACED`).
Mehrere Klassen gleichen Namens (verschiedene Class-Loader) werden nummeriert; `… <n>`
wählt eine aus.

Änderungen anderer Agenten oder Werkzeuge verfolgt Lemon nicht. `… inspect <klasse> verify`
macht sie sichtbar: Lemon liest den laufenden Bytecode zurück und vergleicht ihn
inhaltlich (Methodenrümpfe normalisiert, Konstantenpool-Reihenfolge egal) mit der
Klassendatei bzw. mit Lemons aktivem Patch.

## 5. Redefinition und Retransformation

Beide Wege verwenden dieselbe `.lemonpatch`-Datei (vollständige Klassendateien, siehe
[`LIVE-UPDATES.md`](LIVE-UPDATES.md) §7) – es gibt kein zweites Patch-Format.

| | `redefine` | `retransform` |
|---|---|---|
| JVM-Aufruf | `Instrumentation.redefineClasses(ClassDefinition…)` | `Instrumentation.retransformClasses(Class…)` |
| Woher die JVM die neuen Bytes hat | aus der `ClassDefinition` | die JVM liest **keine** Datei: sie reicht die initialen Bytes der Klasse durch alle retransformationsfähigen Transformer; Lemons `PatchTransformer` liefert dabei die Patch-Klassendatei aus dem Speicher |
| Spätere Retransformationen (andere Agenten, Lemons Rücklesen) | starten vom gepatchten Stand | starten vom Original; Lemons Transformer liefert den Patch **jedes Mal neu** |
| Rollback | erneute Redefinition mit den vorherigen Bytes | Patch aus dem Transformer nehmen und retransformieren – die JVM fällt auf die initialen Bytes (bzw. die Schicht darunter) zurück |
| Erlaubte Änderungen | was die Probes der JVM bestätigen (Standard: Methodenrümpfe; JBR mit `AllowEnhancedClassRedefinition` auch strukturell) | **nur Methodenrümpfe**, per echter Retransformations-Probe geprüft; **auf JVMs mit `AllowEnhancedClassRedefinition=true` gar nicht** (siehe §7) |
| Fremde Redefinition derselben Klasse | überschreibt den Patch (bemerkt `verify`) | der Transformer erkennt sie (anderer Ausgangs-Digest), liefert den Patch nicht mehr und meldet `DISPLACED` |
| Voraussetzung | `Can-Redefine-Classes` | zusätzlich `Can-Retransform-Classes` und `isRetransformClassesSupported()` |

Gemeinsam: alle Klassen eines Patches in **einem** JVM-Aufruf (laut JVMTI-Spezifikation alle
oder keine), auf dem Hauptthread zwischen zwei Ticks; laufende Aufrufe beenden die alte
Methodenversion, neue Aufrufe nutzen die neue; bestehende Objekte behalten ihre Felder;
statische Initialisierer laufen nicht erneut. Nach jedem Anwenden und Zurücknehmen liest
Lemon den laufenden Bytecode zurück; nur wenn er passt, heißt die Meldung
`applied and verified` / `rolled back and verified`.

Regeln, die Lemon zwischen den Wegen durchsetzt:

* Auf eine Klasse mit aktivem Retransformations-Patch wird **keine Redefinition**
  angewendet (sie würde ihn verdrängen): `the class runs retransformation patch [...]`.
* Retransformations-Patches derselben Klasse stapeln sich (Schichten); Rollback nur von
  oben nach unten. Ein Retransformations-Patch auf einem Redefinitions-Patch ist erlaubt;
  nach seinem Rollback läuft wieder die redefinierte Version.
* Die Bestätigung muss denselben Weg nennen wie der Plan.

**Wann welcher Weg?** `redefine` ist der Standard und der einzige Weg für strukturelle
Änderungen (nur mit JBR). `retransform` ist sinnvoll, wenn andere retransformierende Agenten
(Profiler, APM) im Prozess laufen oder wenn der Rollback garantiert auf die Klassendatei der
JAR zurückfallen soll.

### Der Transformer (`PatchTransformer`)

* Wird erst beim ersten Retransformations-Patch registriert, genau einmal, mit
  `addTransformer(transformer, true)`, und nur wenn `isRetransformClassesSupported()`.
* Handelt nie beim Laden einer Klasse (`classBeingRedefined == null` → `null`) und nie bei
  Klassen ohne Patch-Schicht (`null` = Bytes unverändert, ohne sie anzusehen).
* Liefert immer die vollständige Klassendatei der obersten Schicht – keine Änderung „auf“
  den eingehenden Bytes, also keine Doppel-Modifikation.
* Merkt sich beim ersten Mal den SHA-256 der eingehenden Bytes. Kommen später andere
  Bytes (Klasse wurde von außen redefiniert, oder ein Transformer davor liefert etwas
  anderes), wird der Patch nicht mehr geliefert und als `DISPLACED` markiert (Log-Warnung,
  Status, `verify` meldet `VERIFICATION FAILED`), statt die fremde Änderung zu überschreiben.
* Keine Rekursion (pro Thread), Fehler werden geloggt und lassen die Klasse unverändert.
* Reihenfolge: Die JVM ruft zuerst die nicht-retransformationsfähigen Transformer (deren
  Ergebnis wird bei Retransformation wiederverwendet), dann die retransformationsfähigen in
  Registrierungsreihenfolge. Lemons Transformer sieht also die Ausgabe früher registrierter
  fähiger Transformer, und später registrierte (z. B. Lemons Rücklese-Recorder) sehen den
  Patch.
* Nach jeder Anwendung prüft Lemon, dass der Transformer für **jede** Klasse geliefert hat;
  sonst wird der Patch wieder entfernt, erneut retransformiert und als `FAILED` gemeldet.

## 6. Patch-Workflow: von IntelliJ zur laufenden JVM

Ausführlich in [`LIVE-UPDATES.md`](LIVE-UPDATES.md) §4.3; kurz:

1. Server-JAR bauen und deployen, direkt danach `./gradlew :lemon-server:livePatchBaseline`.
2. Laufende JAR holen (`versions/<mc>/lemon-<mc>.jar`, bei 1.21.11 Pflicht).
3. In IntelliJ den Methodenrumpf ändern, dann
   `./gradlew :lemon-server:createLivePatch -PpatchId=<id> -PserverJar=<laufende JAR>`
   → `<id>.lemonpatch` + `.sha256` (nur geänderte Klassen; strukturelle Änderungen markiert).
4. `./gradlew :lemon-server:checkLivePatch -PpatchId=<id> -PserverJar=<laufende JAR>`
   (Integrität, Build-ID, Minecraft-Version, Ausgangs-Digest jeder Klasse).
5. Beide Dateien nach `lemon/patches/inbox/` (Produktion: Digest in die Freigabeliste).
6. `/fork instrumentation patch check <id> retransform` (oder `redefine`).
7. `/fork instrumentation patch apply <id> retransform` → `… confirm`.
8. Prüfen: `applied and verified`, dann den geänderten Codepfad im Spiel testen
   (`behaviour NOT validated by Lemon`), `/fork instrumentation inspect <klasse>`,
   `… patch verify <id>`, `… history`.
9. Zurück: `/fork instrumentation patch rollback <id>` → `… confirm`.

Was vor dem Anwenden geprüft wird (alles oder nichts): Integrität (`.sha256`, Freigabeliste,
Archivgrenzen), Build-ID und Minecraft-Version, je Klasse Server-Class-Loader, änderbar,
**laufender Bytecode = Ausgangsstand des Patches**, Art jeder Änderung und Fähigkeit der JVM
(Probe), Bytecode-Verifier (falls die JVM nicht selbst verifiziert), beim Weg `retransform`
zusätzlich „nur Methodenrümpfe“ und „nicht verdrängt“. Eine geänderte Patch-Datei zwischen
Plan und Bestätigung wird abgelehnt. Lemon verändert eine Patch-Datei nie, um sie passend
zu machen.

## 7. Strukturelle Änderungen

| Änderung | Standard-JVM `redefine` | `retransform` | JBR + `AllowEnhancedClassRedefinition`, `redefine` |
|---|---|---|---|
| Methodenrumpf, Konstante, Zeilennummern | ja | ja | ja |
| Methode/Konstruktor hinzufügen oder entfernen | nein | nein | laut Probe (CI: ja) |
| Feld hinzufügen/entfernen, Feldtyp | nein | nein | laut Probe (CI: ja) |
| Modifikatoren, Oberklasse, Interfaces | nein | nein | laut Probe |
| neue innere/anonyme Klasse (`NestMembers`) | nein | nein | nein (Lemon wendet es nie an) |
| neue Klasse in vorhandenem Paket | ja (`Lookup.defineClass`, bleibt bis Neustart) | ja | ja |
| mehrere abhängige Klassen | ja, ein Patch = ein JVM-Aufruf (alle oder keine) | ja | ja |

Abgelehnte Änderungen nennen den Grund, z. B. `method added: … (Lemon applies only
method-body changes by retransformation; use 'redefine' on a JVM with enhanced class
redefinition)` oder `this JVM cannot add methods/constructors`.

### DCEVM / JetBrains Runtime (nur Entwicklung)

DCEVM wird als eigenständige JVM nicht mehr gepflegt; ihre erweiterte Redefinition steckt in
der **JetBrains Runtime** (JBR) und wird mit `-XX:+AllowEnhancedClassRedefinition`
eingeschaltet. Lemon setzt das nie voraus und stellt keine JVM um. Auf einem
Entwicklungsserver: JBR installieren, Server mit
`<jbr>/bin/java -javaagent:lemon-livepatch-agent.jar -XX:+AllowEnhancedClassRedefinition -jar …`
starten, `/fork instrumentation capabilities` zeigt, was diese JVM tatsächlich kann.
Servertest 6 in CI prüft das mit JBR 21 und 25 (strukturelle Änderung angewendet). Bekannte
Einschränkungen aus CI:

* JBR hat ungültigen Bytecode bei der Redefinition ohne Fehler übernommen – Lemon verifiziert
  dort selbst vorher (Probe „reject invalid bytecode when redefining“).
* **Retransformation mit Änderung + späteres Entladen der Klasse bringt JBR zum Absturz.**
  `scripts/ci/RetransformRepro.java` führt jedes Muster in einer eigenen JVM aus (CI-Lauf #41,
  Job „Runtime core on JetBrains Runtime“): JBR 21.0.11 und 25.0.4.1 **mit**
  `-XX:+AllowEnhancedClassRedefinition` brachen im Szenario `modify-unload` mit
  `free(): double free detected in tcache 2` (Exit 134) ab; `recorder` (nur zurücklesen),
  `modify` (Klasse bleibt geladen) und `modify-then-redefine` liefen durch; **ohne** das Flag
  liefen alle vier Szenarien durch. In der Testsuite traf es den GC-Test, der Klassen-Loader
  entlädt. Deshalb verweigert Lemon auf JVMs mit `AllowEnhancedClassRedefinition=true`
  Retransformations-Patches (Planer, Transformer-Registrierung) und führt dort auch die
  Retransformations-Probe nicht aus („not probed: …“). Redefinition und das reine Zurücklesen
  bleiben verfügbar. Auf Standard-JVMs (Temurin, OpenJDK) betrifft das nichts.

## 8. Grenzen der JVM

* Laufende Methodenaufrufe beenden die alte Version; ein Patch wirkt ab dem nächsten Aufruf.
* Bereits ausgeführte Aktionen, statische Felder, Caches und Objekte werden weder durch einen
  Patch noch durch einen Rollback zurückgesetzt – ein Rollback spielt nur die vorherigen
  Klassendefinitionen ein.
* Statische Initialisierer laufen nach einem Patch nicht erneut.
* Inline-Konstanten (`static final`) in anderen Klassen ändern sich nur, wenn diese Klassen
  im Patch sind.
* Neu definierte Klassen bleiben bis zum Neustart geladen.
* Versteckte Klassen (Lambdas), Arrays und primitive Typen sind nie änderbar; Klassen des
  JDK lehnt Lemon ab.
* Patches gelten nur für den laufenden Prozess; dauerhaft wird eine Änderung mit einer neuen
  Server-JAR und Neustart.
* Jede Redefinition/Retransformation ist eine Safepoint-Operation (alle Java-Threads
  halten kurz an); gemessene Dauer steht in der Ausgabe.

## 9. Tests

Drei Ebenen, getrennt ausgewiesen:

* **Unit-Tests** (ohne JVM-Zustand): Komponenten-Zuordnung, Such-Syntax und Namensprüfung,
  JDWP-Erkennung (`ClassCatalogTest`, `InstrumentationDiagnosticsTest`).
* **Integrationstests** (JUnit, Test-JVM mit echtem `-javaagent`, echte Redefinition und
  Retransformation, „Server“-Klassen in eigenem Class-Loader – kein Paper-Server):
  `RetransformPatchTest` (anwenden, neuer Code läuft, spätere Retransformationen behalten
  den Patch, fremde Redefinition → `DISPLACED`, Schichten, auf Redefinition, strukturelle
  Ablehnung, alles-oder-nichts mit zwei Klassen, falscher Weg bei der Bestätigung,
  Retransformations-Probe), `ClassCatalogTest` (Suche nach Server-, Plugin-, Modul- und
  JDK-Klassen, Seiten, Inspect, Rücklese-Vergleich), `InstrumentationDiagnosticsTest`
  (Startprotokoll, Agent wird nach späterem Anhängen gefunden), `InstrumentationCommandsTest`
  (Rechte, Ausgaben, kompletter Zyklus über die Befehle), **`DynamicAttachTest`** (zwei echte
  JVM-Prozesse: Ziel-JVM ohne Agent, `java -jar lemon-livepatch-agent.jar attach <pid>`,
  `agentmain`, Retransformation einer vor dem Anhängen geladenen Klasse, zweite
  Retransformation, Entfernen).
* **Live-Server-Test** (CI, echter Lemon-Server je Minecraft-Version, Servertest 10,
  `scripts/ci/smoke-instrumentation.txt`): Server startet **ohne** `-javaagent`; Status und
  Klassensuche melden den fehlenden Agenten; Agent wird per `attach <pid>` angehängt;
  Klassensuche und Inspect auf `org.spigotmc.TicksPerSecondCommand` (Digest = Klassendatei
  der laufenden JAR); Probe; der in CI aus geänderten Paper-Quellen gebaute Patch `tps-text`
  wird per Retransformation angewendet, `/tps` zeigt den neuen Text, Rücklesen (selbst eine
  Retransformation) bestätigt den Patch, Redefinition derselben Klasse und ein struktureller
  Patch werden abgelehnt, Rollback, `/tps` wieder original, Audit-Log-Einträge.

Ergebnisse: siehe [`TESTING.md`](TESTING.md) (Abschnitt „Instrumentation“).

## 10. Erster Live-Patch – Schritt für Schritt (Entwicklungsserver)

1. Server mit Agent starten (Weg A) oder später anhängen (Weg B).
2. `config/lemon-runtime.yml`: `live-patching.enabled: true`, `profile: development`; JVM
   mit `-Dlemon.livepatch.development=true` starten (sonst gilt das Produktionsprofil mit
   Freigabeliste). Neustart.
3. Konsole: `fork instrumentation status` – alles `SUCCESS`? Sonst steht dort, was fehlt.
4. `fork instrumentation classes TicksPerSecond paper` → Klasse ist geladen und `modifiable`;
   `fork instrumentation inspect org.spigotmc.TicksPerSecondCommand` → SHA-256 notieren.
5. Auf dem Entwicklungsrechner: Baseline + laufende JAR wie in §6, eine Zeichenkette in
   `TicksPerSecondCommand.execute` ändern, `createLivePatch -PpatchId=tps-text …`,
   `checkLivePatch` → `APPLICABLE on a standard JVM`.
6. `tps-text.lemonpatch` + `.sha256` nach `lemon/patches/inbox/`.
7. `fork instrumentation patch check tps-text retransform` → `Applicable live without restart.`
8. `fork instrumentation patch apply tps-text retransform` → Plan lesen →
   `fork instrumentation patch apply tps-text retransform confirm`
   → `OK patch tps-text applied and verified`.
9. `tps` im Spiel oder an der Konsole → neuer Text. `fork instrumentation patch verify tps-text`.
10. Zurück: `fork instrumentation patch rollback tps-text` → `… confirm` → `tps` wieder alt.
