# ReportPlus

GUI-gestütztes `/report` (ersetzt BetterSMPs Text-Version) und neues
`/bugreport`, mit eigenen Warteschlangen-GUIs fürs Team. Eigenständig -
kein Neubau von BetterSMP nötig, siehe `loadbefore` in `plugin.yml`.

## Features

### `/report [Spieler]`
- Ohne Namen: GUI mit allen Online-Spielern (Kopf anklicken statt Namen tippen).
- Mit Namen: direkt weiter zur Grund-Auswahl.
- Grund per GUI (Cheaten, Griefing, Beleidigung, Werbung, Sonstiges - frei
  in `config.yml` anpassbar).
- Cooldown pro Melder, landet in `reports.yml`, Broadcast an alle mit
  `bettersmp.report.receive`.
- **Überschreibt BetterSMPs `/report`** (`loadbefore: [BetterSMP]` in
  `plugin.yml` sorgt dafür, dass dieses Plugin den Befehlsnamen zuerst
  bekommt). Nutzt bewusst dieselben Rechte (`bettersmp.report`,
  `bettersmp.report.receive`) - in LuckPerms muss nichts nachgezogen werden.

### `/bugreport`
- Kategorie per GUI (Shop, PvP, Lobby, Performance, Sonstiges).
- Beschreibung wird danach per Chat abgefragt (eine Zeile, "abbrechen"
  zum Abbrechen) - fängt genau diese eine Nachricht ab, bevor sie im
  Chat sichtbar wird.
- Landet in `bugreports.yml`, Broadcast an alle mit `bettersmp.bugreport.receive`.

### Team-GUIs
- `/reports` - paginierte Liste offener Meldungen (Kopf des Gemeldeten,
  Melder, Grund, Alter). Klick öffnet die Detailansicht: zum Gemeldeten
  teleportieren (falls online), als erledigt markieren, verwerfen.
- `/bugreports` - dieselbe Liste für Bugmeldungen, mit vollem Text in
  der Detailansicht statt Teleport.

## Befehle
- `/report [Spieler]` (`bettersmp.report`, Standard: alle)
- `/bugreport` (`bettersmp.bugreport`, Standard: alle)
- `/reports` (`bettersmp.report.receive`, Standard: op)
- `/bugreports` (`bettersmp.bugreport.receive`, Standard: op)
- `/reportplus` - Konfiguration neu laden (`reportplus.admin`, Standard: op)

Konfiguration: `config.yml` (Kategorien, Cooldowns), `messages.yml`.
Speicherung in `reports.yml` / `bugreports.yml` im Plugin-Ordner - keine
Datenbank nötig. Keine Abhängigkeiten außer der Paper-API.
