# DBWipe

Notfallwerkzeug: **nur über die Serverkonsole** nutzbar (kein Spieler,
auch kein OP), löscht nach drei Warnungen und einem Passwort **ALLE
Tabellen** der konfigurierten Datenbank - nicht nur die eines
bestimmten Plugins, wirklich jede Tabelle, die dort gerade steht.
Vorher wird automatisch ein vollständiges Backup als `.tar.gz`
erstellt.

## Ablauf

```
/dbwipe            -> Warnung 1/3
/dbwipe            -> Warnung 2/3
/dbwipe            -> Warnung 3/3 (verlangt jetzt ein Passwort)
/dbwipe <Passwort>  -> führt aus: erst Backup, DANACH löschen
```

`/dbwipe cancel` bricht jederzeit ab. Zwischen den Schritten sind
**60 Sekunden** Zeit - danach fängt es von vorne bei Warnung 1 an.

Jeder Schritt landet im Server-Log (nicht nur im Konsolenfenster), so
bleibt nachvollziehbar, wann und ob DBWipe benutzt wurde.

## Was genau passiert

1. **Backup zuerst, immer.** Jede Tabelle der Datenbank wird als
   eigene `.sql`-Datei gedumpt (`SHOW CREATE TABLE` für die Struktur,
   `INSERT INTO ... VALUES (...)` für jede Zeile), alle zusammen in
   ein `.tar.gz` unter `plugins/DBWipe/backups/db_backup_<Zeitstempel>.tar.gz`
   gepackt.
2. **Erst wenn das Backup nachweislich existiert** (Datei da, nicht
   leer), geht es weiter - schlägt das Backup aus irgendeinem Grund
   fehl, wird **gar nichts** gelöscht, der Vorgang bricht komplett ab.
3. **Danach** werden wirklich alle Tabellen gelöscht (`DROP TABLE`,
   mit kurzzeitig deaktivierten Fremdschlüssel-Prüfungen, falls
   welche existieren).

Das TAR-Format wird ohne zusätzliche Bibliothek selbst geschrieben
(einfaches USTAR-Format) - vor dem Einbau gegen eine echte
Rundreise-Extraktion getestet, damit das Backup im Ernstfall auch
wirklich entpackbar ist.

## Einrichtung

`config.yml`:

```yaml
passwort: "DelDB26!"

database:
  host: "127.0.0.1"
  port: 3306
  database: "bettersmp"
  user: "root"
  password: ""
```

Dieselbe Datenbank wie der Rest der Suite (z.B. BetterSMPs
`config.yml`, `database.mariadb`) - DBWipe hat selbst keine eigenen
Tabellen, sichert/löscht aber alle, die es dort vorfindet (BetterSMP,
DuelPlus, alles).

**Zum Passwort:** Es wird als reines Befehlsargument eingegeben
(`/dbwipe DeinPasswort`) und landet dadurch zwangsläufig in der
Server-Konsolen-Historie und im Log - wie jedes andere per Konsole
eingegebene Passwort auch. Regelmäßig wechseln.

## Warum keine Rechteverwaltung (`permission:`)?

Der Schutz ist absichtlich **nicht** über eine Bukkit-Permission
gelöst, sondern direkt im Code geprüft: die Konsole hat ohnehin
automatisch jede Rechtigung, eine `permission:`-Angabe würde also
niemanden wirklich aussperren. Der echte Riegel ist die Prüfung
"kommt das wirklich von der Konsole" - die schließt auch OP-Spieler
zuverlässig aus, eine Berechtigung könnte das nicht.

Keine Abhängigkeiten außer der Paper-API und dem MariaDB-Treiber (wie
bei jedem anderen Plugin der Suite mit Datenbank-Anbindung).
