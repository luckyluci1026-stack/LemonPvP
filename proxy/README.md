# Proxy-Setup: eine Domain pro Server + Warteraum

So sieht das Ergebnis aus:

```
smp1.lemon-servers.de  ─┐
smp2.lemon-servers.de  ─┤                ┌─ smp1  (Paper, Port 25566)
lobby.lemon-servers.de ─┤                ├─ smp2  (Paper, Port 25567)
lemon-servers.de       ─┴─► Velocity ───┼─ lobby (Paper + SMPLobby, Port 25569)
   /hub, /lobby ──────────► (Port 25565)└─ limbo (NanoLimbo, Port 25568)
```

- Wer **smp1.lemon-servers.de** eingibt, landet **direkt** auf smp1 – ohne Lobby,
  ohne `/server`-Befehl. Genauso smp2.
- **`/hub`** bzw. **`/lobby`** bringt von überall zur Lobby (dem Server mit
  dem Plugin `SMPLobby` – siehe `bettersmp-suite/smplobby/README.md`).
  Genauso wer direkt **lobby.lemon-servers.de** eingibt.
- Stürzt ein SMP ab, fliegt niemand raus: der Spieler landet im **Warteraum**
  (NanoLimbo) und wird **automatisch zurückgeholt**, sobald sein Server wieder
  läuft. Wer über smp2 kam, kommt auch wieder auf smp2 – nicht auf smp1. Der
  Warteraum ist bewusst kein Ort zum Verweilen wie die Lobby – man landet nur
  dort, man geht nicht freiwillig hin.

Das erledigt das mitgelieferte Velocity-Plugin **SMPProxy** (`smpproxy/`).

---

## 1. Ordner anlegen

```
/opt/mc/
├── proxy/      Velocity
├── lobby/      Paper 1.21.11 + SMPLobby-1.0.0.jar
├── smp1/       Paper 1.21.11
├── smp2/       Paper 1.21.11
└── limbo/      NanoLimbo
```

## 2. Velocity einrichten

1. [Velocity](https://papermc.io/downloads/velocity) herunterladen, nach
   `proxy/` legen und **einmal starten** – dabei entstehen `velocity.toml` und
   `forwarding.secret`.
2. Die erzeugte `velocity.toml` durch die aus diesem Ordner ersetzen
   (Adressen und Ports anpassen). **`forwarding.secret` nicht löschen.**
3. `smpproxy/target/SMPProxy-1.0.0.jar` nach `proxy/plugins/` legen.
4. Velocity neu starten. Es entsteht `proxy/plugins/smpproxy/config.yml`.
5. Dort die Domains eintragen:

```yaml
domains:
  "smp1.lemon-servers.de": smp1
  "smp2.lemon-servers.de": smp2
  "lobby.lemon-servers.de": lobby
  "lemon-servers.de": smp1
default-server: smp1
limbo: limbo
hub-server: lobby
```

Die Namen rechts (`smp1`, `smp2`, `lobby`, `limbo`) müssen genau so in der
`velocity.toml` unter `[servers]` stehen.

## 3. Die Paper-Server umstellen

Auf **jedem** Paper-Server dahinter - das sind smp1, smp2 **und die Lobby**:

**`server.properties`**

```properties
online-mode=false
server-port=25566        # smp2: 25567, lobby: 25569
```

> `online-mode=false` ist hier **kein** Sicherheitsloch: Velocity prüft die
> Mojang-Accounts und reicht die geprüfte Identität weiter. Wichtig ist nur,
> dass die Ports der SMP-Server **nicht** von außen erreichbar sind
> (Firewall: nur 25565 offen lassen).

**`config/paper-global.yml`**

```yaml
proxies:
  velocity:
    enabled: true
    online-mode: true
    secret: 'HIER DEN INHALT VON proxy/forwarding.secret EINFÜGEN'
  bungee-cord:
    online-mode: true
```

Danach den Server neu starten.

Auf die Lobby kommt zusätzlich `bettersmp-suite/smplobby/target/SMPLobby-1.0.0.jar`
nach `plugins/` - siehe `bettersmp-suite/smplobby/README.md` für die
Einrichtung dort (Spawn setzen, Server-Wähler eintragen).

Optional (nur auf der Lobby, sonst nirgends): `bettersmp-suite/lobbylock/target/LobbyLock-1.0.0.jar`
sperrt zusätzlich Item-Drop, Inventar-Verschieben, Offhand-Tausch und
Türen/Falltüren/Schilder-Interaktion - eigenständig, unabhängig von
SMPLobby installierbar (siehe `bettersmp-suite/lobbylock/README.md`).

## 4. NanoLimbo als Warteraum

[NanoLimbo](https://github.com/Nan1t/NanoLimbo) herunterladen, nach `limbo/`
legen und einmal starten. In der erzeugten `settings.yml` sind genau drei
Stellen wichtig (die Schreibweise unterscheidet sich je nach NanoLimbo-Version
minimal – halte dich an die Namen, die in **deiner** Datei stehen):

```yaml
bind:
  ip: '0.0.0.0'
  port: 25568

infoForwarding:
  type: MODERN
  secret: 'DERSELBE WERT WIE IN forwarding.secret'

maxPlayers: 100
```

`type: MODERN` ist Pflicht – sonst passt die Weiterleitung nicht zu Velocity und
Spieler bekommen beim Wechsel einen Fehler.

## 5. DNS

Bei deinem Domain-Anbieter zwei Einträge auf die Server-IP:

| Typ | Name | Ziel |
|-----|------|------|
| A | `smp1` | deine Server-IP |
| A | `smp2` | deine Server-IP |
| A | `@` | deine Server-IP |

Beide Domains zeigen auf **dieselbe** IP und **denselben** Port (25565) – die
Aufteilung macht der Proxy anhand des Namens, den der Client mitschickt.

Läuft Velocity auf einem anderen Port als 25565, kommen zusätzlich SRV-Einträge
dazu (`_minecraft._tcp.smp1`).

---

## Bedrock-Spieler (Geyser)

Mit Proxy gehört **Geyser auf den Proxy**, nicht auf die SMP-Server:

1. `Geyser-Velocity.jar` und `floodgate-velocity.jar` nach `proxy/plugins/`.
2. In `proxy/plugins/Geyser-Velocity/config.yml`:
   `remote: auth-type: floodgate`.
3. In `proxy/plugins/floodgate/` entsteht `key.pem` – diese Datei nach
   `plugins/floodgate/` auf **jedem** SMP-Server kopieren.
4. Auf den SMP-Servern zusätzlich `floodgate-velocity`s Paper-Gegenstück
   (`floodgate-paper.jar`) installieren.

**EasyBedrock** lädt Geyser für einen **Einzelserver ohne Proxy** herunter. Mit
Proxy ist es nicht nötig – nimm dort die Velocity-Versionen von Geyser und
Floodgate von Hand. Die restlichen Plugins der Suite laufen unverändert weiter.

---

## Netzwerkbann: /netban

Wer hier gebannt wird, kommt gar nicht mehr rein - nicht nur von einem SMP
geflogen, sondern vom Proxy selbst abgewiesen, bevor er auch nur die Lobby zu
sehen bekommt. Und weil bei jedem neuen Verbindungsversuch dieselbe Prüfung
läuft, ist "kann nicht mehr reconnecten" keine Zusatzfunktion, sondern einfach
die Folge davon.

| Befehl | Wirkung |
|---|---|
| `/netban <Spieler> [Dauer] [Grund]` | Bannt vom ganzen Netzwerk. Dauer wie `30m 12h 7d 2w`, `perm` oder weglassen = für immer. Ist die Person gerade online, fliegt sie sofort raus. |
| `/netunban <Spieler>` | Hebt den Bann auf. |
| `/netbans` | Listet alle aktiven Banns. |
| `/netbaninfo <Spieler>` | Grund, wer gebannt hat, bis wann. |

Gebannt werden kann per Namen (auch offline - vorausgesetzt, die Person hat
sich diesem Netzwerk schon einmal verbunden, siehe `spieler.yml` unten) oder
per UUID. Das Recht dafür ist `smpproxy.ban` (Standard: kein Vergabe -
über eine Rechteverwaltung wie LuckPerms zuteilen).

**Das ist bewusst nicht `/ban`.** BetterSMP bringt auf dem SMP selbst schon
ein `/gban` mit (bei geteilter MariaDB sogar zwischen mehreren SMPs geteilt).
Ein am Proxy registrierter `/ban`-Befehl würde das schlucken, egal auf
welchem Server gerade getippt wird. `/netban` ist der stärkere, eigene Hebel
für "raus aus dem ganzen Netzwerk" - `/gban` bleibt die Werkzeugkiste des
SMPs für alles andere.

Zwei Dateien legt der Proxy dafür selbst an:

```
plugins/smpproxy/bans.yml     - wer gebannt ist, von wem, bis wann
plugins/smpproxy/spieler.yml  - jeder Name, der sich je verbunden hat -> UUID
```

## /offend und /punish: PunishPlus

Zusätzlich zu `/netban` gibt es **PunishPlus** (`punishplus/`) - ein
eigenes, zweites Velocity-Plugin für Sperren mit **vorgefertigten
Gründen samt eigener Dauer**, damit nicht jedes Mal von Hand eine Dauer
und ein Grundtext getippt werden müssen:

| Befehl | Wirkung |
|---|---|
| `/offend <Spieler> <Grund>` | Temporäre Sperre - Dauer kommt aus dem Grund (`bans.yml` von PunishPlus). |
| `/punish <Spieler> <Grund>` | Dauerhafte Sperre - unabhängig von der im Grund hinterlegten Dauer. |
| `/punishplus reload` | Liest `config.yml` und `bans.yml` von PunishPlus neu ein. |

Genau wie `/netban` wirkt das sofort netzwerkweit (gleicher `LoginEvent`-
Riegel), braucht aber **keine Dauer-Eingabe von Hand** - stattdessen
feste, vorgefertigte Gründe mit eigener Dauer aus PunishPlus' eigener
`bans.yml`. Weder `/offend` noch `/punish` lassen sich per Befehl
rückgängig machen (Absicht) - nur von Hand in `gesperrt.yml` editieren,
danach `/punishplus reload`.

`punishplus/target/PunishPlus-1.0.0.jar` kommt genau wie SMPProxy nach
`proxy/plugins/` - siehe `punishplus/README.md` für Rechte und Details.

## /rtp auch aus der Lobby

Steht in `config.yml` unter `rtp.redirect-server` ein Server, funktioniert
`/rtp` von überall im Netzwerk - auch aus der Lobby, wo es sonst keine Welt
zum Teleportieren gibt. Der Ablauf: Der Spieler wird zuerst auf den
eingetragenen SMP geschickt, und sobald die Verbindung steht, läuft dort
automatisch das echte `/rtp` von BetterRTP - mit Cooldown, Warmup und allem,
was BetterRTP sonst auch macht.

Wer schon auf einem Server mit eigenem `/rtp` steht (Liste unter
`rtp.passthrough-servers`), merkt von alldem nichts: Der Befehl geht
unverändert durch, wie bisher.

Voraussetzung: BetterRTP muss auf dem Zielserver installiert sein und aktuell
genug, um den Kanal `betterrtp:run` zu kennen (siehe dessen README).

## Testen

| Test | Erwartung |
|------|-----------|
| `smp1.lemon-servers.de` verbinden | direkt auf smp1 |
| `smp2.lemon-servers.de` verbinden | direkt auf smp2 |
| smp2 stoppen, dann verbinden | Warteraum + Hinweis „smp2 ist gerade nicht erreichbar" |
| smp2 wieder starten | nach ein paar Sekunden automatisch zurück auf smp2 |
| smp1 im Spiel abschießen (`stop`) | Warteraum statt Trennbildschirm |
| Auf smp1 jemanden mit `/gban` bannen | Ban-Screen wird angezeigt (**kein** Warteraum) |
| `/smpproxy status` | Liste aller Server mit online/offline |
| `/hub` oder `/lobby` (von smp1 aus) | Verbindet zur Lobby, nicht zum Warteraum |

Der letzte Punkt ist der wichtigste Unterschied zu einer reinen `try`-Liste in
der `velocity.toml`: SMPProxy pingt den Server beim Rauswurf noch einmal an.
Antwortet er, war es ein echter Kick (Bann, `/kick`) und der Spieler sieht den
Grund. Antwortet er nicht, ist er abgestürzt und der Spieler kommt in den
Warteraum.

## Dateien in diesem Ordner

```
proxy/
├── velocity.toml     fertige Velocity-Konfiguration
├── smpproxy/         Velocity-Plugin: Domain-Routing, Warteraum, /netban
│   └── .../ban/      Netzwerkbann: Speicher, Dauer-Parser
├── punishplus/       Velocity-Plugin: /offend + /punish mit vorgefertigten Gründen
│   └── .../store/    Sperren + Gründe-Katalog: Speicher, Dauer-Parser
└── README.md         diese Anleitung
```
