# SMPProxy

Velocity-Plugin: **Eine Domain pro Server** und ein **Warteraum**, falls ein
Server abstürzt.

## Was es macht

**1. Domain → Server.** Der Client schickt beim Verbinden mit, welche Adresse
eingegeben wurde. SMPProxy schaut in seiner Liste nach und setzt den Zielserver:

```yaml
domains:
  "smp1.lemon-servers.de": smp1
  "smp2.lemon-servers.de": smp2
  "*.lemon-servers.de": smp1      # Stern = alle übrigen Subdomains
default-server: smp1
```

Ein genauer Treffer geht immer vor einem Stern-Eintrag. Groß-/Kleinschreibung,
ein Punkt am Ende und der Zusatz von Forge-Clients werden ignoriert.

**2. Zielserver aus → Warteraum statt Trennbildschirm.** Ist smp2 offline,
kommt der Spieler in den Limbo und bekommt einen Hinweis.

**3. Absturz oder echter Kick?** Fliegt jemand mitten im Spiel raus, pingt
SMPProxy den Server sofort noch einmal an:

| Server antwortet | Bedeutung | Reaktion |
|---|---|---|
| ja | echter Kick (Bann, `/kick`) | Spieler sieht den Grund |
| nein | Absturz / Neustart | Spieler kommt in den Warteraum |

Damit bleibt der Ban-Screen von BetterSMP sichtbar – ein reines
`try = [...]` in der `velocity.toml` würde auch Gebannte in den Limbo schieben.

**4. Automatisch zurück.** Alle paar Sekunden werden alle Server angepingt.
Läuft ein Server wieder **stabil** (standardmäßig 3 erfolgreiche Pings
hintereinander), holt SMPProxy alle Spieler zurück, die wegen genau diesem
Server im Warteraum sitzen. Wer über `smp2.lemon-servers.de` kam, landet auch
wieder auf smp2.

## Befehle & Rechte

| Befehl | Was es tut | Recht |
|---|---|---|
| `/hub`, `/lobby`, `/limbo` | in den Warteraum | alle |
| `/smp1`, `/smp2`, … | direkt auf den Server (aus `domains` erzeugt) | alle |
| `/smpproxy status` | zeigt online/offline + Spielerzahl je Server | `smpproxy.admin` |
| `/smpproxy reload` | `config.yml` neu einlesen | `smpproxy.admin` |

`/server` und `/send` bringt Velocity selbst mit.

## Konfiguration

`plugins/smpproxy/config.yml` – wird beim ersten Start angelegt. Alle Texte sind
**MiniMessage**, Gradients funktionieren also genauso wie in den SMP-Plugins.

```yaml
monitor:
  interval: 5          # Sekunden zwischen den Pings
  timeout: 3           # ab wann ein Server als offline gilt
  check-on-start: true

auto-return:
  enabled: true
  confirmations: 3     # so viele gute Pings, bevor zurückgeschickt wird
  cooldown: 5          # Sekunden zwischen zwei Versuchen pro Spieler
  show-notice: true

commands:
  hub-enabled: true
  hub-aliases: ["hub", "lobby", "limbo"]
  server-shortcuts: true
```

`confirmations: 3` verhindert, dass Spieler in einen gerade erst startenden
Server laufen und sofort wieder rausfliegen.

## Bauen

Braucht **JDK 21**, **Maven** und Zugriff auf `repo.papermc.io`
(dort liegt die Velocity-API):

```bash
cd proxy/smpproxy
mvn clean package
```

Ergebnis: `target/SMPProxy-1.0.0.jar` → nach `proxy/plugins/`.

SnakeYAML wird mit ins Jar gepackt (umbenannt, damit es mit nichts kollidiert),
sonst hat das Plugin keine Abhängigkeiten.

## Einbau

Siehe `../README.md` – dort steht das komplette Setup mit Velocity, den beiden
Paper-Servern, NanoLimbo, DNS und Geyser.
