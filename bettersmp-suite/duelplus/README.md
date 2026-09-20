# DuelPlus

Duell-System mit echtem SMP-Loot, wie auf den großen Servern: **`/duel
<Spieler>`** (auf jedem Herkunftsserver nutzbar) oder - standardmäßig
nur in der Lobby - der Duell-Gegenstand am Spawn, dann kämpfen beide
mit ihrem echten SMP-Inventar auf einem eigenen, dritten Server -
**Duels**.

## Aufbau

**Ein** Plugin, installiert auf **drei** Servern - `server-name`,
`ist-arena-server` und `ist-loot-quelle` in der `config.yml` stellen
pro Server ein, wie es sich verhält:

| Server | `server-name` | `ist-arena-server` | `ist-loot-quelle` | `gegenstand.aktiv` | Rolle |
|---|---|---|---|---|---|
| SMP | `SMP` | `false` | `true` | `false` (Standard) | `/duel`, Herausforderungen, **echtes Loot** |
| Lobby | `Lobby` | `false` | `false` | `true` (dort umstellen) | `/duel`, Gegenstand, Herausforderungen |
| Duels | `Duels` | `true` | `false` (ohne Wirkung) | - (ohne Wirkung) | Arenen, Kämpfe, Loot |

`gegenstand.aktiv` ist standardmäßig **aus** - der Gegenstand ist nur
eine Zusatz-Option, `/duel <Spieler>` funktioniert unabhängig davon
überall. Empfehlung: nur in der Lobby auf `true` stellen (thematisch
passend, genau wie SMPLobbys eigene Menü-Gegenstände) - auf dem
SMP-Server wirkt ein zusätzliches Schwert im Inventar eher störend.

`ist-loot-quelle: true` gehört auf **genau einen** Server - den mit
dem "echten" Loot (in den meisten Setups: SMP). Nur dort wird das
Inventar laufend in die Datenbank gespiegelt. Ohne das würde eine
Herausforderung, die von einem ANDEREN Server aus angenommen wird
(z.B. in der Lobby), fälschlich das dortige, meist leere/andere
Live-Inventar mitschicken, statt des echten SMP-Inventars. Auf allen
anderen Servern muss diese Zeile `false` sein (Standard).

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

## Wichtig nach einem Update von einer älteren DuelPlus-Version

Die Arena-Welten werden nur **beim allerersten Erzeugen** aufgebaut -
eine ältere Version hat dafür noch echtes Vanilla-Gelände verwendet
(daher konnte man am Rand der Plattform in die "echte" Welt
darunter/darum schauen), eine etwas neuere Version noch ohne die
unsichtbare Barriere-Box gegen Enderperlen-Fluchten, und eine dritte
Version noch mit reinem Luft-Void unter der Plattform statt eines
richtigen Bodens mit Bedrock. Damit Arenen stattdessen mit dem
aktuellen Aufbau entstehen (siehe unten), müssen auf dem
**Duels-Server** einmalig die alten Weltordner gelöscht werden -
Standard-Namen `duell_arena_1` bis `duell_arena_4` (siehe
`arenen.welt-praefix` / `arenen.anzahl`), bei gestopptem Server. Beim
nächsten Start entstehen sie automatisch neu. Das gilt auch für jedes
künftige Arena-Update dieser Art - nur ein Löschen der Weltordner
erzeugt sie wirklich neu, ein reines Plugin-Update oder
`/duelplus reload` reicht dafür nicht.

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
5. **Arena**: eine von mehreren automatisch erzeugten Welten **ohne
   echte Vanilla-Terraingenerierung** (man soll am Rand nicht in "die
   echte Welt" schauen können) mit einer flachen,
   dekorierten Plattform (jede Arena mit eigener Optik: Stein,
   Tiefenschiefer, Sandstein oder Schwarzstein) mit einem
   Kompassmuster als Boden - konzentrische Kreise, durchzogen von acht
   Speichen zu den Spawnpunkten, den zwei symmetrischen
   Deckungspfeilern und den vier Eck-Türmen - und einer Worldborder.
   Zusätzlich zur (schrumpfenden) Worldborder steckt die ganze Arena in
   einer unsichtbaren, unzerstörbaren Barriere-Box in der vollen
   Start-Größe (vier Wände + Decke) - die Worldborder allein reicht
   nicht, weil eine Enderperle ihre sanfte Zurückdräng-Kollision instant
   überspringt (bekannter Vanilla-Kniff); die Barriere-Box stoppt das
   unabhängig vom aktuellen Border-Stand zuverlässig.
   Bauen/Abbauen ist während des Duells erlaubt - danach wird die
   Arena **komplett zurückgerollt** (jede Blockänderung: Abbauen,
   Platzieren, Explosionen, Eimer, Flüssigkeiten, Feuer), damit sie
   für das nächste Duell wieder genau so aussieht wie vorher.
   Während des Countdowns (siehe Punkt 4) stehen beide fest an ihrem
   Platz und sehen sich an - sobald der Kampf beginnt, schrumpft die
   Worldborder (`kampf.worldborder-schrumpfen`) in Richtung `ziel-groesse`
   (Standard 10 Blöcke). Das Tempo passt sich laufend an: Solange
   getroffen wird, läuft es im normalen, langsamen Tempo
   (`dauer-sekunden`, Standard 270s) - fällt länger als
   `camping-nach-sekunden` (Standard 15s) kein Treffer, schaltet es auf
   das deutlich schnellere Camping-Tempo (`camping-dauer-sekunden`,
   Standard 60s) um. Aktiver Kampf wird also nicht bestraft, reines
   Ausweichen/Verstecken schon.

   Zusätzliche Absicherung gegen (auch unfreiwillige, z.B. AFK) totale
   Untätigkeit: Fällt `kampf.aufgabe-bei-inaktivitaet.warnung-nach-minuten`
   (Standard 10) lang **gar kein** Treffer, warnt eine Chat-Nachricht
   beide. Fällt danach nochmal `frist-danach-minuten` (Standard 5) lang
   keiner, wird die Runde automatisch aufgegeben - **ohne Sieger**,
   aber beide verlieren `inventar-verlust-anteil` (Standard 1/5) ihres
   Inventars (zufällig ausgewählte, belegte Fächer, ersatzlos - kein
   Loot für den jeweils anderen). Jeder einzelne Treffer setzt diese
   Uhr komplett zurück, ganz gleich wie weit sie schon gelaufen war -
   ein normal geführter Kampf ist davon nie betroffen. Lässt sich mit
   `aktiv: false` komplett abschalten.
6. **Ein tödlicher Treffer wird abgefangen statt eines echten Todes** -
   der Verlierer sieht stattdessen eine **Todeskamera** (ein paar
   Sekunden Orbit um die Stelle), sein komplettes mitgebrachtes
   Inventar wird in **Shulker-Kisten verpackt und dort abgeworfen** -
   `loot.schutz-sekunden` (Standard 60) lang gehören sie exklusiv dem
   Gewinner, danach frei für alle. Ein **Totem der Unsterblichkeit**
   in Haupt- oder Nebenhand rettet ganz normal wie in Vanilla.
7. Der Verlierer geht kurz danach zurück auf seinen Herkunftsserver,
   mit leerem Inventar. Der Gewinner bleibt bewusst **die volle
   `loot.schutz-sekunden`-Zeit** in der Arena (in der Zeit unverwundbar
   - kein nachträglicher Schaden mehr möglich) und erst DANN geht es
   für ihn zurück, mit allem, was er bis dahin eingesammelt hat -
   sonst wäre das exklusive Loot-Zeitfenster nutzlos, weil er längst
   weg wäre, bevor er es überhaupt selbst aufheben könnte.

Sieg, Niederlage und Unentschieden werden zusätzlich zur Chat-Nachricht
**groß auf dem Bildschirm** angezeigt (Title/Subtitle) - und zwar
**sofort im Moment des Ausgangs**, nicht erst nach der (beim Gewinner
teils viel späteren) Rückreise.

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
/draw                    - Unentschieden vorschlagen (nur waehrend des eigenen Duells,
                            wirkt erst, wenn BEIDE es benutzen - keiner gewinnt/verliert,
                            jeder bekommt sein eigenes Inventar unveraendert zurueck)
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
- Gräbt sich jemand durch die Plattform nach unten oder wird über die
  niedrige Randmauer hinaus geworfen (z.B. durch eine Explosion), fällt
  er weit - unter der Plattform liegt kein Void, sondern ein einfacher
  Boden mit Bedrock ganz unten, aber bis dahin ist es ein sehr tiefer
  Sturz. Das zählt wie jeder andere tödliche Treffer als Niederlage
  ("Ring-Out") - sobald klar zu weit unterhalb der Plattform, unabhängig
  vom tatsächlichen Sturzschaden (siehe
  `ArenaGuardListener.beimAbsturzUnterDieArena`), damit z.B.
  Federfall-Stiefel dabei kein Schlupfloch sind.
