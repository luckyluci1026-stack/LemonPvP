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
unsichtbare Barriere-Box gegen Enderperlen-Fluchten, eine dritte
Version noch mit reinem Luft-Void unter der Plattform statt eines
richtigen Bodens mit Bedrock, eine vierte Version noch mit einer sehr
hoch gelegenen Plattform (Sturz bis zum Boden weit über 100 Blöcke)
statt der aktuellen, viel niedrigeren, und eine fünfte Version noch mit
kleinerer Worldborder-Größe und nur EINEM Boden-Design (statt der vier
strukturell unterschiedlichen Themes, siehe unten). Damit Arenen
stattdessen mit dem aktuellen Aufbau entstehen (siehe unten), müssen auf
dem **Duels-Server** einmalig die alten Weltordner gelöscht werden -
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
   echte Welt" schauen können). Reihum **vier strukturell komplett
   unterschiedliche Boden-Themes** (nicht nur andere Bloecke, andere
   Taktik), siehe `ArenaManager`:
   - **Kompass**: das urspruengliche Design - konzentrische Kreise,
     durchzogen von acht Speichen zu den Spawnpunkten, den zwei
     symmetrischen Deckungspfeilern und den vier Eck-Tuermen.
   - **Feuergraben**: ein Lavagraben trennt beide Haelften, nur an
     drei festen Stellen ueberbrueckt - reinfallen zaehlt sofort als
     Niederlage (kein fester Boden darunter), kein bisschen Schaden
     zum Durchlaufen.
   - **Eisarena**: rutschiges PACKED_ICE/BLUE_ICE (schmilzt anders als
     normales Eis NICHT) - veraendert das Bewegungsgefuehl im Kampf
     wirklich.
   - **Ozean-Tempel**: Prismarine-Boden mit einem flachen, begehbaren
     Wasserring nahe am Rand - bremst, schadet aber nicht.

   Alle vier teilen sich denselben Rahmen: vier hohe **Eck-Türme** (mit
   Zinnenkranz und Licht auf der Spitze), einen bewusst **nicht glatt
   abgeschnittenen** äußeren Rand (ein zerklüfteter, zufällig geformter
   Fels-Ansatz darunter - die Arena soll wie eine abgebrochene, im
   Nichts schwebende Kampf-Plattform wirken) und laufend aufsteigende
   Seelen-Partikel nahe am Rand während des Kampfes. Um das alles herum
   eine Worldborder.
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
   Platz und sehen sich an, bei jeder verbleibenden Sekunde ein kurzer
   Ton - sobald der Kampf beginnt, schlägt sichtbar/hörbar ein
   **Blitz** am Arena-Zentrum ein (nur Effekt, macht keinen Schaden und
   zündet nichts an) und beide sehen "KAMPF!" groß auf dem Bildschirm.
   Ab da läuft für beide eine **Boss-Bar**, die laufend zeigt, wie weit
   die Worldborder (`kampf.worldborder-schrumpfen`) noch bis
   `ziel-groesse` (Standard 10 Blöcke) schrumpft. Das Tempo passt sich
   laufend an: Solange getroffen wird, läuft es im normalen, langsamen
   Tempo (`dauer-sekunden`, Standard 270s, Boss-Bar blau) - fällt
   länger als `camping-nach-sekunden` (Standard 15s) kein Treffer,
   schaltet es auf das deutlich schnellere Camping-Tempo
   (`camping-dauer-sekunden`, Standard 60s, Boss-Bar gelb) um. Aktiver
   Kampf wird also nicht bestraft, reines Ausweichen/Verstecken schon.
   Erreicht die Grenze zum ersten Mal ihr Minimum, schlägt das Spiel
   einmalig in einen **"Plötzlicher Tod"**-Moment um: Boss-Bar wird rot
   und bleibt es, beide bekommen einen Warn-Titel samt Ton, und für die
   Dauer wird zusätzlich die eigentliche Boss-Musik eingespielt.

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
   in Haupt- oder Nebenhand rettet ganz normal wie in Vanilla. Jeder
   Treffer bekommt zusätzliches Partikel-Feedback am Opfer plus einen
   Bestätigungs-Ton für den Angreifer - der entscheidende Treffer
   zusätzlich einen größeren Partikel-Ausbruch und einen eigenen Ton
   für den Gewinner.
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
  niedrige Randmauer hinaus geworfen (z.B. durch eine Explosion), zählt
  das **sofort** als Niederlage ("Ring-Out") - kein spürbarer Sturz
  mehr vorher: `ArenaGuardListener.beimAbsturzUnterDieArena` löst schon
  aus, sobald die Y-Koordinate auch nur einen Hauch unter die normale
  Steh-Höhe der Plattform fällt, unabhängig vom tatsächlichen
  Sturzschaden, damit z.B. Federfall-Stiefel dabei kein Schlupfloch
  sind. Unter der Plattform liegt trotzdem kein echter Void, sondern
  ein einfacher Boden mit Bedrock ganz unten (bei Standardwerten rund
  25 Blöcke tiefer) - reines Sicherheitsnetz für den unwahrscheinlichen
  Fall, dass der Check doch einmal durchrutscht, wird im normalen Spiel
  aber nie erreicht.
