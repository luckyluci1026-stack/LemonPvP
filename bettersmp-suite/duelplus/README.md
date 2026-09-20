# DuelPlus

Duell-System mit echtem SMP-Loot, wie auf den großen Servern: **`/duel
<Spieler>`** (auf jedem Herkunftsserver nutzbar) oder - standardmäßig
nur in der Lobby - der Duell-Gegenstand am Spawn, dann kämpfen beide
mit ihrem echten SMP-Inventar auf einem eigenen, dritten Server -
**Duels**.

## Aufbau

**Ein** Plugin, installiert auf **drei** Servern - `server-name` und
`ist-arena-server` in der `config.yml` stellen pro Server ein, wie es
sich verhält:

| Server | `server-name` | `ist-arena-server` | `gegenstand.aktiv` | Rolle |
|---|---|---|---|---|
| SMP | `SMP` | `false` | `false` (Standard) | `/duel`, Herausforderungen |
| Lobby | `Lobby` | `false` | `true` (dort umstellen) | `/duel`, Gegenstand, Herausforderungen |
| Duels | `Duels` | `true` | - (ohne Wirkung) | Arenen, Kämpfe, Loot |

`gegenstand.aktiv` ist standardmäßig **aus** - der Gegenstand ist nur
eine Zusatz-Option, `/duel <Spieler>` funktioniert unabhängig davon
überall. Empfehlung: nur in der Lobby auf `true` stellen (thematisch
passend, genau wie SMPLobbys eigene Menü-Gegenstände) - auf dem
SMP-Server wirkt ein zusätzliches Schwert im Inventar eher störend.

Kein eigenes Velocity-Plugin nötig: Der Serverwechsel läuft über den
Standard-`BungeeCord`-Kanal, den Velocity auch versteht (gleiches
Vorbild wie SMPLobbys Server-Wähler) - es reicht ein Eintrag für
**Duels** in der `velocity.toml` unter `[servers]`.

## Voraussetzung: gemeinsame MariaDB

**Zwingend erforderlich.** DuelPlus muss Zustand (wer fordert wen
heraus, wessen Inventar wohin unterwegs ist, wer gerade auf welchem
Server ist) zwischen drei komplett getrennten Serverprozessen teilen -
eine lokale Datei könnte das grundsätzlich nicht. Ohne Verbindung
bleibt DuelPlus auf dem betroffenen Server wirkungslos (Meldung in der
Konsole, `/duelplus reload` versucht es erneut).

Läuft bereits eine MariaDB für BetterSMP (dessen `config.yml`,
`database.mariadb`), einfach dieselben Zugangsdaten in **jeder** der
drei `config.yml` eintragen - DuelPlus legt eigene Tabellen an
(`duelplus_*`), ohne BetterSMPs eigene zu berühren. Die Datenbank
selbst muss vorher existieren und von allen drei Servern aus
erreichbar sein (Netzwerk/Firewall).

## Ablauf eines Duells

1. **Herausfordern**: `/duel <Spieler>` (überall) oder Rechtsklick auf
   den Duell-Gegenstand, wo aktiviert (öffnet eine Spieler-Auswahl) -
   funktioniert auch serverübergreifend (Herausforderer auf dem SMP,
   Ziel in der Lobby).
2. Das Ziel bekommt eine klickbare Nachricht (`Annehmen` / `Ablehnen`),
   läuft nach `anfrage.timeout-sekunden` (Standard 60) von selbst ab.
3. Bei Annahme: **beide** Inventare (Hotbar, Rucksack, Rüstung,
   Offhand - **ohne** Enderkiste) werden geschnappschossen, beide
   Spieler zum Duels-Server geschickt.
4. Auf **Duels** angekommen: Inventar wird angewendet, sobald beide da
   sind, startet ein Countdown (`kampf.countdown-sekunden`, während
   dessen unverwundbar), dann beginnt der Kampf.
5. **Arena**: eine von mehreren automatisch erzeugten Vanilla-Welten
   mit einer flachen Steinplattform (fair für beide, unabhängig vom
   Landeplatz) und einer Worldborder. Bauen/Abbauen ist während des
   Duells erlaubt - danach wird die Arena **komplett zurückgerollt**
   (jede Blockänderung: Abbauen, Platzieren, Explosionen, Eimer,
   Flüssigkeiten, Feuer), damit sie für das nächste Duell wieder genau
   so aussieht wie vorher.
6. **Ein tödlicher Treffer wird abgefangen statt eines echten Todes** -
   der Verlierer sieht stattdessen eine **Todeskamera** (ein paar
   Sekunden Orbit um die Stelle), sein komplettes mitgebrachtes
   Inventar wird in **Shulker-Kisten verpackt und dort abgeworfen** -
   `loot.schutz-sekunden` (Standard 60) lang gehören sie exklusiv dem
   Gewinner, danach frei für alle.
7. Beide werden zurück auf ihren jeweiligen Herkunftsserver geschickt:
   der Gewinner mit seinem (überlebenden) Inventar, der Verlierer mit
   leerem Inventar.

**Verbindung während des eigenen Duells getrennt = automatische
Niederlage** (inklusive Loot-Verlust) - verhindert, sich durch
Abbrechen das eigene Inventar zu retten.

Während eines Duells sind **Enderkisten deaktiviert** und **alle
Befehle gesperrt** (`duelplus.command.bypass` fürs Team) - kein
Ausweichen über `/shop` oder Ähnliches.

## Befehle

```
/duel <Spieler>          - herausfordern
/duel accept <Spieler>   - annehmen
/duel decline <Spieler>  - ablehnen
/duelplus reload         - config.yml neu einlesen, DB-Verbindung neu aufbauen
```

## Rechte

- `duelplus.use` - `/duel` und der Gegenstand nutzen (Standard: an)
- `duelplus.command.bypass` - Befehle bleiben während eines eigenen
  Duells nutzbar, fürs Team (Standard: nur OP)
- `duelplus.admin` - `/duelplus reload` (Standard: nur OP)

## Bekannte Grenzen

- Fallende Blöcke (Sand/Kies) durch Schwerkraft werden vom Rollback
  nicht erfasst - eher kosmetisch, kein Stakes-Thema.
- Gräbt sich jemand durch die Plattform nach unten, landet er im
  natürlichen (unbearbeiteten) Gelände darunter - die
  Sturz-/Fallschaden-Abfangung greift trotzdem, es geht dabei kein
  Inventar verloren.
