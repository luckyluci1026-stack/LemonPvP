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
statt der aktuellen, viel niedrigeren, eine fünfte Version noch mit
kleinerer Worldborder-Größe, eine sechste Version noch mit mehreren
strukturell verschiedenen Boden-Themes (Lavagraben, Eis, Wasserring)
UND einer sichtbaren Randmauer, auf der man stehen/von der man
abrutschen konnte - beides durch Live-Tests als Fehlerquelle
identifiziert und wieder entfernt -, und eine siebte Version noch mit
der Plattform nur 1 Block über nacktem Bedrock (kein echtes Terrain
darunter) statt des aktuellen, mehrere Blöcke tiefen Erde/Stein/
Tiefenschiefer-Aufbaus (siehe unten). Damit Arenen stattdessen mit dem
aktuellen Aufbau entstehen (siehe unten), müssen auf
dem **Duels-Server** einmalig die alten Weltordner gelöscht werden -
Standard-Namen `duell_arena_1` bis `duell_arena_4` (siehe
`arenen.welt-praefix` / `arenen.anzahl`), bei gestopptem Server. Beim
nächsten Start entstehen sie automatisch neu. Das gilt auch für jedes
künftige Arena-Update dieser Art - nur ein Löschen der Weltordner
erzeugt sie wirklich neu, ein reines Plugin-Update oder
`/duelplus reload` reicht dafür nicht.

**Woran erkenne ich, ob es geklappt hat?** Im Server-Log beim Start
steht pro Arena entweder `NEU gebaut` (Weltordner war weg, Arena wurde
mit dem aktuellen Aufbau + der aktuellen `config.yml` neu erzeugt) oder
`aus vorhandener Welt geladen` (Weltordner war noch da - keine
Struktur-Änderung übernommen). Radius und Plattform-Höhe, mit denen
eine Arena tatsächlich gebaut wurde, merkt sich das Plugin zusätzlich
dauerhaft in `plugins/DuelPlus/arena-meta.yml` - **unabhängig davon**,
was später in der `config.yml` steht. Ändert man `worldborder-groesse`
oder die Terrain-Tiefen (`boden-tiefe`/`stein-tiefe`/`deepslate-tiefe`),
ohne den zugehörigen Weltordner zu löschen, bleibt die Arena also exakt
bei ihren ursprünglichen Werten (Plattform-Höhe, Worldborder-Größe,
Barriere-Box - alles bleibt zueinander konsistent), statt durch einen
live berechneten, nicht mehr passenden Wert kaputtzugehen. Nach `NEU
gebaut` füllt sich das Terrain unter der Plattform noch ein paar
Sekunden lang asynchron auf (siehe unten) - eine eigene Log-Zeile
`Terrain ... fertig aufgefuellt` meldet, wann das fertig ist.

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
   echte Welt" schauen können). Unter der begehbaren Steinplattform
   folgt luecken-los **echtes, grabbares Terrain** bis zum
   unzerstörbaren Bedrock hinunter: standardmäßig 7 Blöcke Erde, dann
   35 Blöcke Stein, dann 15 Blöcke Tiefenschiefer (`arenen.boden-tiefe`
   / `stein-tiefe` / `deepslate-tiefe`) - reißt eine Explosion
   (Endkristall/Anker) den Boden weg, fällt man in dieses Terrain
   statt in einen leeren Abgrund. Der Boden ist ein aufwendiges Muster
   aus **zwei konzentrischen Ringen** plus einer feinen Schachbrett-
   Textur dazwischen, überlagert von acht Speichen durch die Mitte (zu
   Spawnpunkten, Deckungspfeilern, Eck-Türmen) - alles aus
   gewöhnlichen, robusten Blöcken (keine Gefahren-Materialien wie
   Lava/Wasser/Eis). Reihum zwei verschiedene Materialpaletten (Stein,
   Tiefenschiefer), gleicher Aufbau. Dazu vier hohe **Eck-Türme** (mit
   Zinnenkranz und Licht auf der Spitze) und zwei Deckungspfeiler in
   der Mitte.

   Bewusst **keine sichtbare Randmauer** mehr: der äußere Abschluss ist
   ausschließlich eine unsichtbare, unzerstörbare **Barriere-Box**
   (vier Wände + Decke, bis exakt auf Bedrock-Niveau hinunter, kein
   Spalt) in der vollen Start-Größe der Arena - die Worldborder allein
   reicht nicht, weil eine Enderperle ihre sanfte Zurückdräng-Kollision
   instant überspringt (bekannter Vanilla-Kniff); die Barriere-Box
   stoppt das unabhängig vom aktuellen Border-Stand zuverlässig, ohne
   eine begehbare Kante zu bieten, auf der man stehen oder von der man
   unerwartet abrutschen/durchfallen könnte, UND verhindert, dass sich
   jemand seitlich durch das jetzt echte Terrain aus der Arena
   heraus gräbt.
   Bauen/Abbauen ist während des Duells erlaubt - danach wird die
   Arena **komplett zurückgerollt** (jede Blockänderung: Abbauen,
   Platzieren, Explosionen, Eimer, Flüssigkeiten, Feuer), damit sie
   für das nächste Duell wieder genau so aussieht wie vorher.
   Während des Kampfes steigen zusätzlich laufend Seelen-Partikel nahe
   am Rand auf.
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
Ausweichen über `/shop` oder Ähnliches. Die Befehlssperre gilt auch
noch direkt NACH dem Kampfende bis zur tatsächlichen Rückreise
(Todeskamera beim Verlierer, volles `loot.schutz-sekunden`-Fenster
beim Gewinner) - sonst könnte sich z.B. der Gewinner per `/spawn`
selbst wegteleportieren, bevor sein gewonnenes Inventar überhaupt in
die Datenbank geschrieben wurde (Loot wäre dann verloren, Arena/
Worldborder blieben für diese Runde hängen).

Für die Dauer des Kampfes steckt DuelPlus beide Duellanten zusätzlich
in ein eigenes Scoreboard-Team `duelplus_kampf` mit **Friendly Fire
erzwungen an** (jede Sekunde neu durchgesetzt, nicht nur einmal beim
Start). Grund: ein Spieler ist pro Scoreboard immer nur in höchstens
einem Team - packt ein anderes Plugin (z.B. TAB anhand der
LuckPerms-Gruppe fürs Tabliste-/Namensschild-Einfärben) beide
Duellanten schon in ein gemeinsames Team OHNE Friendly Fire, blockt
die Server-Engine Schaden zwischen ihnen komplett, BEVOR überhaupt ein
abfangbares Event entsteht - Hieb-Geräusch/-Animation bleiben dabei
client-seitig trotzdem sichtbar, es wirkt also wie "Treffer kommt an,
aber 0 Schaden". Das Team wird beim Kampfende (Sieg/Niederlage/
Unentschieden/Aufgabe) wieder verlassen.

## Befehle

```
/duel <Spieler>          - herausfordern
/duel <Spieler> accept   - annehmen
/duel <Spieler> decline  - ablehnen
/duel stats [Spieler]    - Sieg/Niederlage/Unentschieden-Statistik (ohne Angabe: die eigene;
                            das Ziel muss gerade ONLINE sein, egal auf welchem Server)
/duel top [Anzahl]       - Rangliste nach Siegen absteigend (Standard 10, maximal 15)
/duel watch <Spieler>    - einem laufenden Duell als Zuschauer beiwohnen (SPECTATOR-Modus) -
                            funktioniert von JEDEM Server aus, auch wenn das Duell auf dem
                            Duels-Server laeuft; das Ziel muss gerade ONLINE und in einem
                            AKTIVEN Duell sein
/duel unwatch            - Zuschauen beenden, zurueck auf den Herkunftsserver
/draw                    - Unentschieden vorschlagen (nur waehrend des eigenen Duells,
                            wirkt erst, wenn BEIDE es benutzen - keiner gewinnt/verliert,
                            jeder bekommt sein eigenes Inventar unveraendert zurueck)
/duelplus reload         - config.yml neu einlesen, DB-Verbindung neu aufbauen
```

Ein Zuschauer wird NIE in die eigentliche Duell-Session aufgenommen
(SPECTATOR-Modus uebernimmt Kollision/Schaden/Interaktion ohnehin
komplett) und landet automatisch wieder auf seinem Herkunftsserver,
sobald das beobachtete Duell endet.

Die Statistik zaehlt jeden Duell-Ausgang serverübergreifend (gemeinsame
MariaDB, siehe unten): ein normaler Sieg/eine Niederlage erhöht
`siege`/`niederlagen` des jeweiligen Spielers, ein `/draw` UND eine
automatische Aufgabe bei Inaktivitaet (siehe unten) zaehlen beide als
`unentschieden` fuer beide Beteiligten - beides endet ja ohne Sieger,
nur der Weg dorthin unterscheidet sich.

## Rechte

- `duelplus.use` - `/duel` und der Gegenstand nutzen (Standard: an)
- `duelplus.command.bypass` - Befehle bleiben während eines eigenen
  Duells nutzbar, fürs Team (Standard: nur OP)
- `duelplus.admin` - `/duelplus reload`, außerdem sieht man die
  Meldungen der Treffer-Prüfung im Chat (Standard: nur OP)

## Treffer-Prüfung (Schläge ohne Schaden, z. B. bei Bedrock-Spielern)

Auf dem Duels-Server prüft DuelPlus jeden Schlag zwischen den beiden
Duellanten (`kampf.treffer-pruefung`, Standard: an).

- **Beim Kampfstart und danach jede Sekunde** wird für beide sichergestellt:
  nicht unverwundbar, Survival, PvP in der Arena-Welt an, gemeinsames
  Friendly-Fire-Team (auch auf einem eigenen Scoreboard, falls ein anderes
  Plugin eins vergibt), Weltwechsel abgeschlossen und Welt als geladen
  markiert. War etwas davon kaputt, steht in der Konsole
  `Treffer-Pruefung: <Name> war nicht kampfbereit und wurde repariert: ...`.
- **Kommt ein Schlag ohne Schaden an**, obwohl er zählen müsste (also nicht
  in der kurzen Schutzzeit direkt nach einem Treffer und nicht mit einem
  Schild geblockt), repariert DuelPlus den Zustand sofort. Den genauen
  Grund schreibt es in die Konsole und schickt ihn Duellanten mit
  `duelplus.admin` in den Chat: z. B. unverwundbar markiert, falscher
  Spielmodus, Team ohne Friendly Fire, ein anderes Plugin hat den Schaden
  abgebrochen (mit Namen), oder Schläge kommen gar nicht erst beim Server an
  (dann blockiert sie ein Anticheat oder Paket-Plugin schon vorher).
- Jede Meldung kommt höchstens alle 5 Sekunden pro Spielerpaar, damit die
  Konsole nicht vollläuft.

## Bekannte Grenzen

- Fallende Blöcke (Sand/Kies) durch Schwerkraft werden vom Rollback
  nicht erfasst - eher kosmetisch, kein Stakes-Thema.
- Reißt eine Explosion (z.B. Endkristall/Anker) den Boden unter
  jemandem weg, fällt man in das echte Terrain darunter (Erde/Stein/
  Tiefenschiefer) statt in einen leeren Abgrund - genau wie in
  typischem Kristall-PvP, nur ohne den sofortigen K.o. eines reinen
  Luft-Void-Designs. Automatische Niederlage gibt es erst bei einem
  Sturz bis `arenen.todeslinie-y` (Standard Y=-100, 36 Blöcke unter dem
  unzerstörbaren Bedrock bei Y=-64) - im Normalfall völlig
  unerreichbar (`ArenaGuardListener.beimAbsturzUnterDieArena`). Die
  Arena steckt seitlich in einer unsichtbaren Barriere-Box bis auf
  Bedrock-Niveau (kein Spalt) - ein echtes Entkommen, seitlich oder
  nach unten, kommt gar nicht erst vor, die Todeslinie ist reines
  Sicherheitsnetz für den unwahrscheinlichen Fall, dass doch mal eine
  Lücke auftritt.
