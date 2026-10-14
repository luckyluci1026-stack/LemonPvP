# Bekannte Einschränkungen

## Hot-Deployment

1. **Modul-Hot-Deployment ist kein JVM-Hot-Swap.** Lemon tauscht Modulinstanzen aus.
   Plugins und Serverklassen haben eigene, enger begrenzte Wege (siehe „Live-Updates“).
2. **Klassen bleiben bis zur Garbage Collection im Speicher.** Ein geschlossener
   Class-Loader wird nur eingesammelt, wenn nichts mehr auf ihn verweist. Häufige Ursachen
   für Lecks: eigene Threads, `ThreadLocal`s in fremden Threads, statische Referenzen
   aus Server/Plugins, direkt (an der Runtime vorbei) registrierte Listener/Tasks,
   JDBC-Treiber, die sich im `DriverManager` registrieren, Logger-Handler, Shutdown-Hooks.
   `/fork module diagnostics <id>` zeigt, ob alte Class-Loader eingesammelt wurden.
3. **Kein atomarer Wechsel.** Alte und neue Version laufen nie gleichzeitig; der Wechsel
   erfolgt innerhalb eines Hauptthread-Ticks. Asynchrone Events können die Lücke sehen.
4. **Rollback nur innerhalb der Runtime.** Welt-, Datenbank- oder Netzwerkeffekte der neuen
   Version werden nicht zurückgenommen.
5. **Hauptthread-Callbacks sind nicht unterbrechbar.** Ein `onEnable`, das hängt, blockiert
   den Server. Lemon meldet langsame Callbacks, kann sie aber nicht abbrechen.
6. **Laufende asynchrone Arbeit** kann nicht beendet werden; Entladen wird bis zu ihrem Ende
   verweigert bzw. beim Hot-Swap verzögert.
7. **Abhängige Module** blockieren Reload/Unload ihrer Abhängigkeiten (Class-Loader-Bindung).
8. **Befehle** werden über die Bukkit-`CommandMap` registriert (Paper markiert sie ab 26.3
   als *obsolete*). Paper-Brigadier-Befehle mit voller Argumentsyntax sind für Module nicht
   verfügbar, weil Paper diese nur beim Start registriert.
9. **Bundle-Bibliotheken:** Parent-first-Laden bedeutet, dass vom Server mitgelieferte
   Bibliotheksversionen gewinnen. Module müssen eigene Abhängigkeiten relocaten.
10. **Folia wird nicht unterstützt** (regionalisiertes Threading; Lemon basiert auf Paper).
11. **Permission-Defaults** von Modulberechtigungen werden nur registriert, wenn der Knoten
    noch nicht existiert.
12. **Startreihenfolge:** Startmodule werden ab dem ersten Tick aktiviert, d. h. kurz nach
    „Done“. Spieler, die exakt in diesem Moment beitreten, erreichen Join-Listener ggf. nicht.
13. **`/reload` (Paper)** stoppt und startet die Runtime vollständig; Module werden dabei
    deaktiviert und (gemäß Konfiguration/Persistenz) wieder aktiviert.
14. **Keine Sicherheits-Sandbox** – siehe `docs/SECURITY.md`.

## Live-Updates (Plugins, Core-Patches)

1. **Kein universelles Live-Patching.** Standard-JVMs (Temurin/OpenJDK/Oracle) ändern nur
   Methodenrümpfe. Strukturelle Änderungen gehen nur mit JetBrains Runtime und
   `-XX:+AllowEnhancedClassRedefinition` – in der CI nur auf JBR 21.0.11 und 25.0.4.1 geprüft.
2. **JBR verifiziert redefinierte Klassen nicht** (in der CI beobachtet). Lemon führt den
   Verifier deshalb selbst vorher aus; Klassen, die sich außerhalb ihres Laufzeitpakets
   nicht prüfen lassen, lehnt das Produktionsprofil ab.
3. **Kein Zustandsabgleich.** Bestehende Objekte, statische Felder, Caches, Threads und
   laufende Aufrufe behalten Zustand bzw. alten Code. Statische Initialisierer laufen nicht
   erneut. Konstanten, die andere Klassen eingebettet haben, ändern sich dort nicht.
4. **Keine Tick-Atomarität über Threads.** Die Redefinition läuft auf dem Hauptthread
   zwischen zwei Scheduler-Aufgaben; andere Threads können alten Code zu Ende ausführen.
5. **Rollback nur für Bytecode** und nur im selben Prozess; Patches überleben keinen
   Neustart. Rollback in umgekehrter Reihenfolge (LIFO) pro Klasse.
6. **Neue Klassen bleiben geladen**, auch nach Rollback oder fehlgeschlagener Anwendung.
7. **Kurze Pause aller Java-Threads** bei jeder Redefinition (JVM-Safepoint); mit
   erweiterter Redefinition je nach Heap länger.
8. **Plugins:** Paper hat keine offizielle Entlade-Funktion. Lemon nutzt dazu interne
   Felder von Papers Plugin-Manager (in allen vier Versionen identisch; fehlen sie, meldet
   Lemon „restart only“). Zustand, den ein Plugin außerhalb der Bukkit-API angelegt hat
   (eigene Threads, statische Referenzen in anderen Plugins, JDBC-Treiber …), wird nicht
   aufgeräumt. Paper-Plugins, Plugins mit Lifecycle-API und Plugins mit aktiven
   Abhängigen werden nie live getauscht. Spieler mit offenen Inventaren/GUIs eines
   Plugins sehen nach dem Tausch ggf. ein veraltetes Fenster.
9. **Kein Spieler-Login in der CI.** Die Server-Tests prüfen nach Patches Konsole, Ticks
   (`/tps`), Server-List-Ping und Heap; eine echte Spielerverbindung wurde nicht getestet.

## Fork / Versionen

1. **Ein Verzeichnis pro Version statt eines Branches pro Version.** In dieser Arbeitssitzung
   durfte nur ein Branch beschrieben werden. Die Verzeichnisse sind so aufgebaut, dass sie
   per `git subtree split` in eigene Branches überführt werden können (`docs/UPSTREAM-SYNC.md`).
2. **26.3** basiert auf Paper-`main` ohne Release-Tag; Upstream-Änderungen sind dort häufiger.
3. **Upstream-Checkstyle** für die API wird im Fork für 26.2/26.3 entfernt (der Fork ändert
   keine API-Quellen); siehe `docs/UPSTREAM-SYNC.md`.
4. **Lokaler Build** benötigt Zugriff auf `repo.papermc.io`, Mojang-Server und GitHub; in der
   Entwicklungsumgebung dieser Arbeit war das nicht gegeben – Paper-Builds wurden in GitHub
   Actions ausgeführt und ausgewertet.
5. **Kompatibilität** mit ViaVersion, Geyser/Floodgate, Velocity/BungeeCord wurde nicht
   getestet (`docs/COMPATIBILITY.md`).
