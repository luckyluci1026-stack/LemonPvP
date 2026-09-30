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

**5. Release-Countdown im Warteraum.** Steht in `release.zeit` ein Termin,
warten bis dahin alle im Warteraum (Limbo) statt auf dem SMP:

- Live-Countdown als Bossbar (in der letzten Stunde läuft sie ab), in der
  letzten Minute die Sekunden in der Actionbar, in den letzten Sekunden große
  Zahlen mit Ton, zum Release ein großer Titel mit Erfolgs-Sound.
- Bis dahin sind die Server aus `release.gesperrte-server` zu – auch über
  `/hub`, `/server`, `/ah`, `/rtp` und die Kurzbefehle. Wer neu reinkommt,
  landet automatisch im Warteraum.
- **Faire Wellen:** Zum Release geht es in Gruppen (`pro-welle`, alle
  `wellen-abstand-sekunden`) auf den `ziel-server`. Wer zuerst im Warteraum
  war, ist zuerst drin; jeder sieht in der Actionbar seinen Platz und die
  ungefähre Wartezeit. Vordrängeln per `/server` geht nicht.
- Läuft der Ziel-Server zur Release-Zeit noch nicht, wird gewartet, bis er
  erreichbar ist – niemand wird in einen toten Server geschickt.
- Klappt eine Verbindung nicht, kommt der Spieler in der nächsten Welle
  wieder vorne dran (höchstens 3 Versuche, danach darf er selbst).
- Ist der Warteraum selbst nicht erreichbar, sieht man beim Joinen einen
  Bildschirm mit Termin und Restzeit statt einer Fehlermeldung.
- Das Team (`smpproxy.release.bypass`) darf vorher schon überall hin.
- Nach dem Release ist alles wieder normal – auch nach einem Proxy-Neustart
  (`release.yml` merkt sich, dass der Release gelaufen ist).
- `/testrelease [Sekunden]` spielt den kompletten Ablauf als Probe nur für
  dich durch (Warteraum → Countdown → Titel → Welle → Ziel-Server). Der echte
  Release und alle anderen Spieler bleiben unberührt.

**6. Netzwerkweite Moderation.** `/regeln` und `/rules` zeigen überall dieselben
Regeln (auch das Regelbuch in der Lobby). `/msg` wird von AntiSwear auf dem
Server des Absenders geprüft, Stummschaltungen gelten im ganzen Netzwerk, und
Voice-Chat-Stummschaltungen sowie „Voice-Regeln akzeptiert" werden zwischen
allen Servern abgeglichen (`voice.yml`).

## Befehle & Rechte

| Befehl | Was es tut | Recht |
|---|---|---|
| `/hub`, `/lobby`, `/spawn` | zur Lobby; nach einem Duell sofort aus der Arena zurück (siehe unten) | alle |
| `/smp1`, `/smp2`, … | direkt auf den Server (aus `domains` erzeugt) | alle |
| `/msg`, `/r` | Privatnachricht über alle Server | alle |
| `/ah`, `/rtp` | Auktionshaus / RTP von jedem Server aus | alle |
| `/regeln`, `/rules` | Serverregeln | alle |
| `/release` | Countdown bis zum Release anzeigen | alle |
| `/release zeit <Datum> <Uhrzeit>` | Release-Termin setzen, z.B. `03.10.2026 18:00` | `smpproxy.release.admin` |
| `/release jetzt [Sekunden]` | Release gleich starten (Standard 10 s Countdown) | `smpproxy.release.admin` |
| `/release aus` | Countdown beenden, alles offen | `smpproxy.release.admin` |
| `/testrelease [Sekunden]`, `/testrelease stop` | Probe-Release nur für dich | `smpproxy.release.test` |
| – | vor dem Release schon überall hin dürfen | `smpproxy.release.bypass` |
| – | nie in der Einlass-Warteschlange warten | `smpproxy.einlass.bypass` |
| `/netban`, `/netunban`, `/netbans`, `/netbaninfo` | Netzwerkbann | `smpproxy.ban` |
| `/smpproxy status` | zeigt online/offline, Spielerzahl, Auslastung (ms/Tick, TPS) und Warteschlange je Server | `smpproxy.admin` |
| `/smpproxy reload` | `config.yml` neu einlesen | `smpproxy.admin` |

Rechte auf dem Proxy vergibt LuckPerms-Velocity (z.B.
`/lpv user <Name> permission set smpproxy.release.admin`). Aus der
Proxy-Konsole gehen alle Befehle ohne Rechte, außer `/testrelease` (braucht
einen Spieler).

`/server` und `/send` bringt Velocity selbst mit.

**Im Kampf und im Duell** sind `/hub`, `/lobby`, `/spawn`, `/server` und
die Kurzbefehle gesperrt (BetterSMP und DuelPlus melden das über den Kanal
`bettersmp:combat`). Ist ein Duell vorbei (Todeskamera, Loot einsammeln),
bringen genau diese Befehle einen sofort zurück: Der Proxy gibt sie dann als
`/duel verlassen` an den Duels-Server weiter, DuelPlus speichert das
Inventar samt liegengebliebenem Loot und schickt den Spieler auf seinen
Herkunftsserver. `/ah` und `/rtp` warten, bis man zurück ist.

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

```yaml
release:
  zeit: "2026-10-03 18:00"     # leer = kein Release geplant
  zeitzone: "Europe/Berlin"
  ziel-server: "SMP"
  gesperrte-server: ["SMP", "Lobby", "Duels"]
  pro-welle: 5                 # Spieler je Welle
  wellen-abstand-sekunden: 2
  finale-sekunden: 10          # große Zahlen + Ton
  erinnerung-minuten: 5        # Chat-Erinnerung im Warteraum, 0 = aus
```

Was per `/release` gesetzt wird, steht in `release.yml` und gilt vor
`release.zeit` – bis in der `config.yml` ein neuer Termin eingetragen wird.

```yaml
einlass:
  aktiv: true
  server: ["SMP"]          # diese Server bekommen neue Spieler nur gestaffelt
  pro-sekunde: 4           # so viele pro Sekunde (Kommazahlen gehen)
  langsamer-ab-mspt: 35    # ab hier nur noch die Hälfte
  pause-ab-mspt: 45        # ab hier niemand, bis es besser ist
  ausnahmen-von: ["Duels"] # wer von hier kommt, wartet nie
```

Der **Einlass** verhindert Lag, wenn viele gleichzeitig auf den SMP wollen:
nach einem Neustart (alle kommen aus dem Warteraum zurück), direkt nach dem
Proxy-Start oder wenn in der Lobby alle gleichzeitig auf SMP klicken. Wer
warten muss, sieht in der Actionbar seinen Platz und die Wartezeit und wird
automatisch verbunden – neue Verbindungen warten dafür im Warteraum. Die
Server melden über den Kanal `smpproxy:last` alle 2 Sekunden ihre Auslastung
(BetterSMP und SMPLobby machen das von selbst). Ist der SMP ausgelastet, kommt
nur noch die Hälfte oder vorübergehend niemand rein – das gilt auch für die
Release-Wellen. Rückkehrer aus einem Duell und das Team
(`smpproxy.einlass.bypass`) warten nie.

```yaml
bedrock:
  geyser-optimieren: true
```

Stellt beim Start in der Geyser-Config auf dem Proxy
`use-direct-connection: true` und `disable-compression: true` ein (wirkt nach
dem nächsten Neustart). Mehr zu Bedrock- und Java-Leistung steht in
`../../LEISTUNG.md`.

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
