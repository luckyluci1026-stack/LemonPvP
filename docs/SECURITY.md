# Sicherheit

Hot-Deployment bedeutet: **ausführbarer Java-Code wird zur Laufzeit in den Serverprozess
geladen.** Ein Modul hat praktisch dieselben Rechte wie der Server selbst (Dateisystem,
Netzwerk, Spielerdaten, Datenbank-Zugangsdaten, Reflection auf alle Objekte).

## Grundsätze

1. **Eine Signatur oder ein Hash macht bösartigen Code nicht sicher.** SHA-256-Prüfung
   und Allow-List stellen nur sicher, dass *genau die Datei* läuft, die jemand freigegeben
   hat. Ob diese Datei vertrauenswürdig ist, entscheidet ausschließlich, **wer** Module
   bauen, hochladen, freigeben und aktivieren darf.
2. **Classloader-Isolation ist keine Sandbox.** Die Sperre von Runtime- und
   Server-Interna verhindert versehentliche Abhängigkeiten, nicht absichtlichen Zugriff:
   über Reflection auf Server-Objekte, `Unsafe`, JNI, Dateizugriffe oder das Starten von
   Prozessen kann ein Modul jede Grenze umgehen. Der Java-`SecurityManager` ist ab JDK 24
   dauerhaft deaktiviert (JEP 486) und in JDK 21 zur Entfernung vorgesehen; er wird nicht
   verwendet.
3. Behandle das Recht, Dateien in `lemon/modules/`, die `.sha256`-Dateien, die Allow-List
   oder `config/lemon-runtime.yml` zu schreiben, **wie Shell-Zugriff auf den Server**.

## Umgesetzte Schutzmaßnahmen

| Bedrohung | Maßnahme | Ort |
|---|---|---|
| Unbeabsichtigte Aktivierung | Runtime standardmäßig aus (`enabled: false`); Laufzeit-Deployment zusätzlich aus (`allow-runtime-deploy: false`); JAR im Ordner wird nie automatisch aktiviert | `RuntimeConfig`, `ModuleManager.guarded` |
| Ungültige Konfiguration aktiviert etwas | Fehlerhafte `lemon-runtime.yml` ⇒ Runtime bleibt aus; `/fork reload` mit Fehlern ⇒ alte Konfiguration bleibt | `LemonRuntime.bootstrap`, `reloadConfig` |
| Unberechtigte Bedienung | Ändernde Befehle brauchen `lemon.runtime.module.manage` (Standard: **niemand**, nur Konsole); Konfig-Reload `lemon.runtime.command.reload` (Standard: niemand). Die Serverkonsole (`ConsoleCommandSender`) darf alle `/fork`-Befehle; RCON zählt **nicht** als Konsole | `PaperRuntime`, `ForkCommand`, `AdminCommand` |
| Path Traversal über Befehle | Befehle akzeptieren nur Modul-IDs (`[a-z][a-z0-9_-]{1,47}`), nie Pfade | `ModuleIds`, `AdminCommand` |
| Path Traversal über Deskriptoren (Zip-Slip) | `resources` nur als sichere relative Pfade; Auflösung mit `PathGuard.resolveInside` inkl. Symlink-Prüfung; Größenlimit beim Kopieren | `DescriptorParser`, `ModulePreparer`, `PathGuard` |
| Manipulierte Dateinamen / Symlinks im Modulordner | nur sichere Dateinamen, keine Symlinks, Datei muss im Modulordner liegen | `ModuleScanner`, `PathGuard` |
| Gefährliche Konfigurationspfade | `module-directory` und `trusted-hashes-file` nur als sichere relative Pfade im Serververzeichnis | `RuntimeConfigLoader` |
| Beschädigte / unvollständige Uploads | `<jar>.sha256` muss passen (`verify-integrity`) | `IntegrityVerifier` |
| Austausch einer freigegebenen Datei | optionale Allow-List (`trusted-hashes-file`); persistierte Aktivierungen werden nur bei identischem SHA-256 wiederhergestellt | `IntegrityVerifier`, `ModuleManager.activateStartupModules` |
| Time-of-check/time-of-use | geprüfte JAR wird nach `lemon/staging/` kopiert, die Kopie erneut gehasht; geladen wird nur die Kopie | `ArtifactStager` |
| Doppelte / mehrdeutige Module | doppelte IDs ⇒ **alle** Kandidaten abgelehnt | `ModuleScanner` |
| Zip-Bomben / riesige Dateien | JAR ≤ 64 MiB, Deskriptor ≤ 64 KiB, Ressourcen ≤ 1 MiB, YAML ≤ 1 MiB, Alias-/Tiefenlimits | `ModuleScanner`, `LimitedInputStream`, `YamlSupport` |
| YAML-Deserialisierungsangriffe | SnakeYAML `SafeConstructor`, keine doppelten Schlüssel | `YamlSupport` |
| Überschreiben fremder Befehle/Berechtigungen | Kurzlabels und Aliasse nur, wenn frei; Berechtigungsknoten müssen mit `<id>.` beginnen und werden nie überschrieben | `ModuleCommandsImpl`, `DescriptorParser`, `PaperBinding` |
| Markup-/Format-Injection in Ausgaben | feste MiniMessage-Vorlagen, Werte via `Placeholder.unparsed` | `ForkCommand` |
| Log-Injection / gefälschte Audit-Einträge | Steuerzeichen und `|` in Feldern werden ersetzt | `AuditLog`, `PathGuard.sanitize` |
| Fehler eines Moduls bringt Server zum Absturz | jeder Aufruf in Modulcode ist gekapselt; Hooks fangen alle Exceptions | `CallbackGuard`, `ModuleLifecycle`, `LemonRuntimeHooks` |
| Heruntergeladener Code aus unsicheren Quellen | Die Runtime lädt **nichts** aus dem Netz; sie liest nur lokale Dateien | – |
| Eingebettete Geheimnisse | keine Schlüssel, Passwörter oder Tokens im Code oder in der CI | – |

## Empfehlungen für den Betrieb

* `allow-runtime-deploy` nur auf Entwicklungs-/Testservern dauerhaft aktivieren; in
  Produktion `startup-modules` + Neustart bevorzugen oder nur kurzzeitig freischalten.
* `trusted-hashes-file` in Produktion verwenden und den Freigabeprozess (Build in CI,
  Review, Hash in die Allow-List) von den Personen trennen, die Dateien hochladen.
* `lemon.runtime.module.manage` niemals an Spielergruppen vergeben. Aktionen über die
  Konsole oder ein abgesichertes Admin-Konto ausführen.
* `lemon/logs/audit.log` sichern und überwachen.
* Dateirechte: der Serverbenutzer braucht Schreibrechte für `lemon/`, Uploads sollten von
  einem anderen Benutzer kommen, der `config/` nicht schreiben kann.

## Melden von Sicherheitsproblemen

Sicherheitsprobleme der Runtime bitte vertraulich an die Repository-Inhaber melden, nicht
als öffentliches Issue. Probleme in Paper selbst betreffen PaperMC (siehe deren
`SECURITY.md`), sofern sie auch in Upstream-Paper auftreten.
