# LobbyLock

Kleines, eigenständiges Plugin: sperrt Item-Drop, Inventar-Verschieben,
Offhand-Tausch sowie das Interagieren mit Türen/Falltüren/Schildern.
Absichtlich **nicht** Teil von SMPLobby - lässt sich dazu-installieren,
ohne ein bestehendes Plugin zu ersetzen oder neu zu bauen.

## Features
- Item-Drop sperren (`block-drop`)
- Inventar-Verschieben sperren, Klicks **und** Ziehen (`block-inventory-move`)
  - Greift nur im eigenen Inventar - andere GUIs (z.B. der Serverwähler
    von SMPLobby) bleiben unberührt.
- Offhand-Tausch sperren (`block-offhand-swap`)
- Türen/Falltüren/Schilder-Interaktion sperren, jede Art einzeln
  (`block-doors`, `block-trapdoors`, `block-signs`) - erkennt alle
  Holzarten/Varianten über den Materialnamen, keine feste Liste.
- `worlds:` - leer = überall auf diesem Server (Normalfall für einen
  dedizierten Lobby-Server), sonst nur in den genannten Welten
- `lobbylock.bypass` (Standard: **nur OP**) umgeht alle Sperren und
  darf `/lobbylock` (Neuladen) nutzen
- Kurze Actionbar-Erinnerung bei blockierten Versuchen, mit Cooldown
  gegen Spam

## Befehle
- `/lobbylock` - Konfiguration neu laden (`lobbylock.bypass`)

Konfiguration: `config.yml`. Keine Abhängigkeiten außer der Paper-API.
