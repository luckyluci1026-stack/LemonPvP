# LemonQueue

Kapazitäts-Warteschlange + Limbo-Routing für das LemonPvP-Netzwerk.
Velocity-Plugin mit animierten Farbverläufen (MiniMessage), rang-basierter
Priorität (LuckPerms) und FIFO.

## Wie es funktioniert

1. Spieler verbindet sich mit dem Proxy.
2. Ist der Zielserver (z. B. `lobby`) **nicht** voll → direkte Verbindung.
3. Ist er **voll** → Spieler wird in den **Limbo** geparkt und in die
   Warteschlange aufgenommen.
4. Sobald ein Platz frei wird, rückt der Spieler mit der höchsten Priorität
   (bzw. der am längsten wartet) nach.
5. Während des Wartens: animierte ActionBar mit Live-Position, Sound beim
   Aufrücken, Titel beim Eintritt.

Spieler mit `lemonqueue.bypass` überspringen die Warteschlange komplett.

## 1) NanoLimbo als Limbo-Server aufsetzen

LemonQueue parkt Spieler in einem **NanoLimbo**-Server — einem extrem
leichtgewichtigen Limbo, der das MC-Protokoll direkt spricht und problemlos
hunderte Spieler auf 0,25 Kernen / 512 MB RAM hält.

1. NanoLimbo herunterladen: https://github.com/Nan1t/NanoLimbo/releases
   (das fertige `NanoLimbo-x.x.x-all.jar`).
2. In Pterodactyl einen neuen Server (Generic Java / Forking VM Egg) anlegen,
   das JAR hochladen, **512 MB RAM / 25 % CPU** zuweisen.
3. `settings.yml` von NanoLimbo:

   ```yaml
   bind:
     ip: 0.0.0.0
     port: 25599          # frei wählbar
   maxPlayers: 500
   infoForwarding:
     type: MODERN          # WICHTIG bei Velocity: modern forwarding
     secret: "DEIN_VELOCITY_FORWARDING_SECRET"   # aus forwarding.secret
   ```

   > Das `secret` ist der Inhalt der Datei `forwarding.secret` deines
   > Velocity-Proxys. Muss exakt übereinstimmen, sonst lehnt das Limbo ab.

4. Server starten.

## 2) Limbo in Velocity registrieren

In `velocity.toml` unter `[servers]`:

```toml
[servers]
lobby   = "127.0.0.1:20437"
limbo   = "127.0.0.1:25599"
```

(Der Name `limbo` muss mit `limbo-server` in der LemonQueue-`config.yml`
übereinstimmen.)

## 3) LemonQueue installieren

1. `LemonQueue-1.0.0.jar` in den `plugins/`-Ordner des **Velocity-Proxys**
   legen (nicht auf einen Paper-Server!).
2. Proxy neu starten — beim ersten Start wird `plugins/lemonqueue/config.yml`
   erzeugt.
3. `config.yml` anpassen (Limits, Prioritäten) und `/lq reload` … bzw. Proxy
   neu starten.

## Sprache & Texte anpassen

In der `config.yml`:

```yaml
language: "de"      # "de" (Deutsch) oder "en" (English)
show-title: true    # animierten Title/Subtitle anzeigen (ActionBar ist immer an)
```

`language` schaltet **alle** eingebauten Texte um (ActionBar über der Hotbar,
Title/Subtitle, Warteschlangen-Meldungen, `/lq`-Befehle).

Jede einzelne Zeile lässt sich zusätzlich im `messages:`-Block per **MiniMessage**
überschreiben (eigene `<gradient>`, `<bold>`, `<color>` …). Leer (`""`) = der
eingebaute, animierte, fette Standard. Platzhalter: `{pos}`, `{total}`, `{eta}`,
`{target}`, `{server}` (sowie `{size}`/`{max}`/`{count}` in Admin-Zeilen).

```yaml
messages:
  # Text über der Hotbar (ActionBar) frei gestalten:
  actionbar-waiting: "<gradient:#fff700:#00ff7f><bold>Queue</bold></gradient> <gray>{pos}/{total} • {eta}</gray>"
  title: "<gradient:#fff700:#00ff7f><bold>In queue</bold></gradient>"
  subtitle: "<gray>{pos}/{total} • {eta}</gray>"
```

`{eta}` ist eine geschätzte Wartezeit, berechnet aus der tatsächlichen
Durchlaufrate der Warteschlange (gleitendes 60-Sekunden-Fenster).

## Commands

| Command | Rechte | Funktion |
|---|---|---|
| `/lq` (`/queue`) | alle | Eigene Position anzeigen |
| `/lq leave` | alle | Warteschlange verlassen (bleibt im Limbo) |
| `/lq admin` | `lemonqueue.admin` | Alle Warteschlangen + Größen |
| `/lq clear <server>` | `lemonqueue.admin` | Warteschlange leeren |
| `/lq reload` | `lemonqueue.admin` | config.yml neu laden (Texte, MOTD, Limits) |

## Permissions

| Permission | Wirkung |
|---|---|
| `lemonqueue.bypass` | Überspringt die Warteschlange komplett |
| `lemonqueue.admin` | Admin-Subcommands |
| `lemonqueue.priority.vip` … | Priorität laut `config.yml` (höher = weiter vorne) |

## Build

```bash
mvn -pl LemonQueue -am package
```

Ergebnis: `LemonQueue/target/LemonQueue-1.0.0.jar`
