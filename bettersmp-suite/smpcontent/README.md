# SMPContent

Eigene **Blöcke**, **Items** und **Spezialfähigkeiten** (Paper 1.21.11).

## Inhalt

**437 eigene Inhalte** und **5 Bosse**, alle mit Textur außer den Platzhaltern:

| Datei | Inhalt |
|---|---|
| `config.yml` | 24 Blöcke, 113 Items – Erze, Marmor, Laserschwerter, Tränke, Werkzeuge, Früchte, **Waffen**, **Fahrzeuge**, Fallschirm |
| `content/moebel.yml` | **50 Möbel** – drehbar, zum Draufsetzen, mit echten 3D-Formen |
| `content/platzhalter-bloecke.yml` | **100 Platzhalter-Blöcke**, absichtlich ohne Textur |
| `content/platzhalter-items.yml` | **150 Platzhalter-Items**, absichtlich ohne Textur |
| `fahrzeuge.yml` | **37 Fahrzeuge** – 25 Autos (darunter **15 Supersportwagen** in drei Familien), 8 Flieger, 3 Boote, 1 Lok |
| `animationen.yml` | **23 Teilchen-Animationen** (reine Formeln), eigene bauen |
| `bosse.yml` | **5 Bosse** mit Lebensleiste, Phasen und eigenen Angriffen |
| `FAEHIGKEITEN.txt` | alle Auslöser und Aktionen erklärt |

Was das Plugin außerdem kann: **Schleudersitz** mit Fallschirm, **Abstürze**
mit Explosion, **Tacho** über der Hotbar, **Hupe**, drehender **Rotor**, und
ein Bedrock-Pack, das es sich aus deinem Java-Pack selbst baut.

Alle `*.yml` aus `content/` werden mitgeladen – so bleibt die `config.yml`
übersichtlich und du kannst deine Sachen nach Thema trennen.

**Platzhalter** funktionieren sofort (geben, setzen, abbauen, Rezepte) und sehen
aus wie ihr Basis-Item, bis du eine Textur dazulegst. Kein lila-schwarzer
Fehlwürfel, keine Fehlermeldung.

## Wie komme ich an die Sachen?

**1. Craften** – jedes Teil hat ein Rezept (alle in der `config.yml` änderbar):

| Ergebnis | Rezept |
|---|---|
| Rubin | 4× Redstone + 1× Diamant (Kreuz) |
| Saphir | 4× Lapis + 1× Diamant (Kreuz) |
| Rubinblock / Saphirblock | 9× Rubin bzw. Saphir |
| Rubinerz / Saphirerz | 8× Stein + 1× Rubin/Saphir |
| Marmor (8×) | 8× Glatter Stein + 1× Quarz |
| Dunkler Marmor (8×) | 8× Blackstone + 1× Quarz |
| Neonlampe | 4× Glowstone-Staub + 1× Seelaterne |
| Münzhaufen | 9× Goldbarren |
| Magischer Staub (3×) | Lohenstaub + Glowstone-Staub + Redstone |
| Shop-Gutschein | 2× Papier + 1× Goldbarren |
| Schlüssel | 2× Goldbarren + 1× Eisenbarren |
| Rubinschwert | 2× Rubin + 1× Stock |
| Rubinspitzhacke | 3× Rubin + 2× Stöcke |

**2. `/smpcontent list`** – GUI mit allen Inhalten. Klick = 1 Stück,
Shift-Klick = 64 Stück. (Recht `smpcontent.admin`)

Das GUI hat **Seiten** (45 pro Seite), unten links/rechts blättern. Dazu:

- **Filter** (Trichter): Alles → Nur Blöcke → Nur Items → **Nur Möbel** →
  **Nur Fahrzeuge**. So findest du die 37 Fahrzeuge und die 74 Möbel, ohne
  ihre Ids zu kennen.
- **Suche**: `/smpcontent list laser` zeigt nur passende Einträge. Gesucht wird
  in der Id **und** im Anzeigenamen. Das Fernrohr im GUI hebt die Suche auf.
- Passt etwas nicht mehr ins Inventar, fällt es vor die Füße statt zu verschwinden.

**3. `/smpcontent give <Spieler> <Id> [Menge]`** – gezielt vergeben.

## Texturen sind schon dabei

**Updates kommen jetzt auch an.** Früher legte das Plugin die mitgelieferten
Bilder nur an, wenn die Datei noch fehlte. Eine verbesserte Textur aus einer
neuen Jar landete damit nie auf dem Server - man baute sein Pack neu und
bekam trotzdem die alten Bilder. Jetzt merkt sich das Plugin in
`.mitgeliefert.sha`, was es zuletzt selbst hingelegt hat:

| Datei | passiert |
|---|---|
| fehlt | wird angelegt |
| unverändert seit dem letzten Mal | wird auf den neuen Stand gebracht |
| von dir geändert | bleibt genau so, wie sie ist |

Im Log steht dann, wie viele Dateien nachgezogen wurden und wie viele deine
eigenen sind. Danach einmal `/smpcontent pack`, und das Pack ist aktuell.


Beim ersten Start legt das Plugin **277 fertige Texturen und 3D-Modelle** in
deinen Datenordner – **alles außer den Platzhaltern hat eine Textur**:

| | |
|---|---|
| 74 Blöcke | 50 Möbel, Erze, Marmor, Neonlampe, Münzhaufen, deine Stühle und Tische |
| 105 Items | 37 Fahrzeuge, Schwerter, Tränke, Werkzeuge, Früchte, Schlüssel, Münzen … |
| 98 Modelle | echte Formen statt Würfel |

Die Item-Formen kommen aus dem **Namen**, nicht aus dem Material: Bei dir
tragen 14 Items dieselbe Pferderüstung, die wären sonst alle gleich. Ein
`schluessel` wird ein Schlüssel, ein `blood_potion` eine Flasche, ein
`kirsche` eine Frucht – und die Farbe kommt aus dem Farb-Tag im Anzeigenamen,
`<red>Rubinschwert` ist also rot.

Alle Items bekommen eine **dunkle Kontur und Licht von oben links** – genau
das macht aus einem bunten Fleck ein Item, und genauso zeichnet Vanilla seine
Sachen. Die Kontur nimmt dabei die eigene Farbe des Items auf, nicht Schwarz,
sonst sieht alles rußig aus.

Geprüft mit einem echten Pack-Bau: **412 Einträge → 74 Blöcke, 88 Items,
81 eigene Modelle**, ein 266-KB-Pack mit 813 Dateien. Ohne Textur bleiben nur
die 250 Platzhalter, und das ist Absicht.

Die Möbel bekommen dabei **echte Formen** statt eines Würfels: Bänke haben
Beine und Lehne, Sessel Armlehnen und ein Polster, Kommoden Schubladen mit
Griffen, Tische eine Platte auf vier Beinen.

Die Fahrzeuge auch: Autos haben Wanne, Kabine und **vier Räder**, Flugzeuge
Rumpf, Tragflächen, Leitwerk und Triebwerke, Hubschrauber Kanzel, Heckausleger
und Kufen, die Lok Kessel, Schornstein und Fahrwerk. Alle liegen **längs zur
Fahrtrichtung** – quer gebaut würde ein Auto seitwärts fahren.

Der **Rotor dreht sich wirklich**: `rotor: 1.3` hängt ein eigenes Blatt über
das Fahrzeug, das im Stand langsam und unter Schub schnell kreist.
Jedes davon kommt mit **einer einzigen 16×16-Textur** aus – die UV-Ausschnitte
zeigen auf die passende Stelle der Seitenansicht, Räder und Streben auf den
dunklen Streifen.

**Deine eigenen Dateien werden nie überschrieben.** Legst du eine schönere
Textur unter demselben Namen ab, bleibt sie auch nach einem Update deine.
Erzeugt wurden sie mit `texturen.py` im Plugin-Ordner – dort kannst du jede
Form Pixel für Pixel ändern und neu erzeugen lassen.

## Eigenes Texturepack bauen (Blockbench)

SMPContent baut dir das Resource-Pack selbst – du brauchst kein ItemsAdder.

**Ordner (werden beim ersten Start angelegt):**

```
plugins/SMPContent/
├── textures/item/<id>.png      deine Item-Texturen
├── textures/block/<id>.png     deine Block-Texturen
├── models/item/<id>.json       optional: Blockbench-Modell (3D)
├── models/block/<id>.json      optional: Blockbench-Modell (3D)
└── output/SMPPack.zip          das fertige Pack
```

**Ablauf:**
1. Textur (16×16 PNG) in `textures/item/` legen – Dateiname = Id aus der `config.yml`
2. Optional in **Blockbench**: *Neu → Java Block/Item Model*, bauen, Textur-Pfad auf
   `smp:item/<id>` setzen, als *Java-Block/Item-Modell* nach `models/item/<id>.json` exportieren
3. Eintrag in der `config.yml` anlegen
4. Im Spiel: **`/smpcontent pack`**

Liegt ein Blockbench-Modell vor, benutzt das Pack **dein** Modell statt des
automatischen Standardmodells – so bekommst du echte 3D-Waffen und -Blöcke.
Zusätzliche PNGs in den Textur-Ordnern werden mitkopiert, falls dein Modell
mehrere Texturen nutzt. Anleitungen liegen als `LIESMICH.txt` und
`BLOCKBENCH.txt` in den Ordnern.

## Bedrock-Spieler

Zwei Wege, beide bauen dasselbe:

**`/smpcontent bedrock`** – nimmt das Pack, das in deiner `server.properties`
unter `resource-pack=` steht, und macht daraus das Bedrock-Pack. Damit ist es
egal, ob du deine Sachen mit diesem Plugin gebaut hast oder das Pack von Hand
zusammenstellst: Was deine Java-Spieler sehen, bekommen die Bedrock-Spieler.
Ist dort eine URL eingetragen, wird sie heruntergeladen – das läuft auf einem
Nebenthread, der Server ruckelt dabei nicht.

Gesucht wird der Reihe nach: dein Argument (`/smpcontent bedrock <URL|Datei>`),
`resource-pack` aus der `server.properties`, eine ZIP in
`plugins/SMPContent/pack/`, zuletzt das selbst gebaute `output/SMPPack.zip`.

Gelesen werden **alle drei** Bauweisen, die es für Packs gibt:

| im Pack | seit |
|---|---|
| `assets/smp/items/<id>.json` (`item_model`) | 1.21.4 – so baut dieses Plugin |
| `assets/minecraft/items/<material>.json` (`custom_model_data`) | 1.21.4 |
| `overrides` in `assets/minecraft/models/item/<material>.json` | das alte Format |

Bei `item_model` steht das Grundmaterial nicht im Pack – das holt sich das
Plugin aus deiner `config.yml`, zusammen mit der CustomModelData, an der
Geyser das Item erkennt. Elternmodelle werden mitgelesen.

Dein eigenes Pack (`SMPJavaPack1.21.11verbessert.zip`) ergibt damit
**37 Items mit 3D-Modell, 46 flache und 24 eigene Blöcke**.

**`/smpcontent pack`** – baut das Java-Pack aus deinen Ordnern und hängt das
Bedrock-Pack gleich mit dran.

```
plugins/SMPContent/output/
├── SMPPack.zip                        Java-Pack
├── bedrock/SMP-Bedrock-Pack.mcpack    Bedrock-Pack  →  Geyser/packs/
├── geyser/smp_items.json              Items         →  Geyser/custom_mappings/
└── geyser/smp_blocks.json             Blöcke        →  Geyser/custom_mappings/
```

Dazu in der `Geyser/config.yml`: `add-non-bedrock-items: true`, Geyser neu
starten. Bedrock-Spieler laden das Pack beim Verbinden.

Blockbench-Modelle werden dabei in Bedrock-Geometrie umgerechnet – Bedrock
spiegelt die X-Achse und legt den Nullpunkt in die Mitte, die UV-Koordinaten
werden auf die echte Texturgröße skaliert und die `display`-Werte werden zu
Bedrock-Animationen, damit das Item in der Hand richtig sitzt.

### Was Bedrock-Spieler von einem eigenen Block sehen

Die Feldnamen sind **gegen Geysers eigenen Leser geprüft**
(`BlockMappingsReader_v1` aus der Geyser-Jar), nicht geraten:

| Feld | wofür |
|---|---|
| `display_name` | der Name aus deiner `config.yml` statt der nackten Id |
| `destructible_by_mining` | Abbauzeit – **ohne das ist der Block unzerstörbar** |
| `material_instances` | Textur und Zeichenart (voller Würfel `opaque`, Möbel `alpha_test`) |
| `geometry` | die umgerechnete Blockbench-Form |
| `selection_box` / `collision_box` | folgen der echten Form – man steht vor dem Sofa, nicht darin |
| `light_emission` | leuchtende Möbel |

**Eigene Blöcke gehen auch.** Jeder Note-Block-Zustand wird auf einen echten
Bedrock-Block abgebildet – Erze, Marmor und Möbel sehen dort aus wie auf Java.
Ein voller Würfel bleibt ein normaler Block (schöneres Licht, weniger Last),
Möbel bekommen ihre eigene Geometrie. Blöcke **ohne Textur** werden
übersprungen und bleiben auf Bedrock Notenblöcke.

## Eigenes Item anlegen

In der `config.yml` unter `items:`. **Die Einrückung ist das Wichtigste:** der
Name mit genau **2 Leerzeichen**, seine Eigenschaften mit **4**. Keine Tabs.

```yaml
items:
  laserschwert_tuerkis:      # 2 Leerzeichen
    material: IRON_SWORD     # 4 Leerzeichen
    name: "<aqua>Laserschwert</aqua>"
    model-data: 8111
```

Ein Leerzeichen zu viel und YAML kann die ganze Datei nicht mehr lesen. Dann
sagt dir `/smpcontent reload` die Zeile, den Grund und zeigt die Stelle an –
die zuletzt funktionierenden Inhalte bleiben so lange aktiv.

Am Ende der `config.yml` steht eine kopierfertige Vorlage mit allen Feldern.

Danach: `/smpcontent reload`, Textur nach
`plugins/SMPContent/textures/item/<id>.png`, `/smpcontent pack`.
`model-data` muss eindeutig sein – nimm die nächste freie Zahl.

## Spezialfähigkeiten mit Animationen

Jedes Item kann Fähigkeiten bekommen – Auslöser, Abklingzeit, Aktionen:

```yaml
laserschwert_rot:
  material: IRON_SWORD
  abilities:
    - trigger: right-click        # right-click, left-click, hit, kill, held
      name: "Laserstrahl"
      cooldown: 6
      actions:
        - type: animation
          shape: beam
          particle: DUST
          color: "#FF3030"
          length: 14
          ticks: 8
        - type: damage-beam       # trifft alles auf der Blicklinie
          amount: 6.0
          range: 14
        - type: ignite
          seconds: 3
          target: victim
```

**Aktionen:** `animation`, `sound`, `potion`, `damage`, `damage-beam`, `heal`,
`launch`, `push`, `pull`, `lightning`, `ignite`, `explosion`, `message`.
Alles mit allen Werten steht in `FAEHIGKEITEN.txt`.

`explosion` zerstört standardmäßig **keine** Blöcke.

### Eigene Animationen

Sieben Formen sind eingebaut (`beam`, `ring`, `spiral`, `sphere`, `slash`,
`orbit`, `trail`). Eigene baust du in der `animationen.yml` mit Formeln:

```yaml
animations:
  meine_spirale:
    points: 16                            # Teilchen pro Tick
    x: "cos(a + t * 0.3) * (radius * p)"
    y: "p * 2"
    z: "sin(a + t * 0.3) * (radius * p)"
    relative-to: player                   # player | eyes | look
    follow: true                          # läuft mit, wenn du gehst
```

Verfügbar sind `t` (Tick), `p` (Fortschritt 0–1), `i` (Nummer des Teilchens),
`n`, `a` (Winkel), `ticks`, `radius`, `length` – dazu `sin cos tan sqrt abs min
max round floor pow random …` und `pi`, `e`, `tau`.

Der Name wird dann als `shape:` benutzt. **23 sind mitgeliefert** – keine
davon steckt im Plugin fest, jede ist genau diese drei Formeln und lässt sich
Zeile für Zeile ändern:

| | |
|---|---|
| **Kampf** | `laserschwert`, `lichtklinge`, `kreuzschlag`, `klingenwirbel`, `plasmapuls`, `blitzbogen`, `drachenatem` |
| **Zauber** | `runenkreis` (zwei gegenläufige Ringe unter dir), `portal`, `aura`, `bluete`, `doppelhelix` |
| **Wucht** | `meteor` (kommt von schräg oben), `einschlag`, `druckwelle`, `tornado`, `bohrer` |
| **Umgebung** | `sternenfall`, `funkenregen`, `fluegel`, `herz`, `doppelring`, `meine_spirale` |

Ein eigener Prüflauf rechnet alle 23 einmal komplett durch – jeden Tick, jeden
Punkt – und schlägt an, wenn eine Formel `NaN` liefert oder ein Teilchen
hundert Blöcke weit wegfliegt. Eine kaputte Klammer fällt damit auf, bevor sie
im Spiel auffällt.

### Fahrzeuge ziehen eine Spur

Schnelles Fahren sah aus wie langsames Fahren: Die Karosserie steht still im
Bild, nichts verrät das Tempo. Jetzt hängt an jedem Fahrzeug, was dazugehört –
Auspuff und aufgewirbelter Staub am Auto (in der Farbe des Bodens, über den du
fährst), Kondensstreifen an den Flügelspitzen und Nachbrenner beim Flieger,
Gischt am Boot, Dampf aus dem Schornstein der Lok. Gezeichnet wird nur bei
Bewegung und höchstens jeden zweiten Tick.

## Waffen & Werkzeuge

Jedes Item kann eigene Werte bekommen:

```yaml
ruby_sword:
  material: IRON_SWORD
  name: "<red>Rubinschwert</red>"
  attributes:
    attack_damage: 9.0
    attack_speed: -2.2
  durability: 1800
  enchants:
    sharpness: 3
  glow: true
```

Möglich sind alle Vanilla-Attribute (`attack_damage`, `attack_speed`, `armor`,
`movement_speed`, `max_health`, `knockback_resistance` …), eigene `durability`,
`unbreakable`, `enchants` und `glow`.

## Möbel: unbegrenzt viele, drehbar, zum Draufsetzen

Note-Block-Zustände sind irgendwann alle (575 Stück). Möbel gehen darum einen
anderen Weg: Sie sind **Anzeige-Objekte** auf einem unsichtbaren
Platzhalterblock. Davon gibt es **beliebig viele** – die Zahl deiner eigenen
Blöcke ist nach oben offen.

```yaml
blocks:
  eiche_sessel:
    name: "<gray>Eichen-Sessel"
    furniture:              # ab hier ist es ein Möbelstück - kein state nötig
      rotate: 8             # 0 = nie drehen, 4 = Himmelsrichtungen, 8, 16
      solid: true           # man kann draufstehen
      seat: true            # man kann sich draufsetzen
      seat-height: 0.55
      scale: 1.0            # Größe des Modells
      light: 0              # leuchtet (nur wenn solid: false)
```

- **Drehen**: Beim Setzen zeigt das Möbel dahin, wo du hinschaust – auf
  `rotate:` Schritte gerundet. Ein Notenblock kann das nicht, ein
  Anzeige-Objekt schon.
- **Sitzen**: Rechtsklick mit leerer Hand setzt dich hin, Schleichen steht
  wieder auf. Auf jedem Möbel sitzt nur einer.
- **Abbauen**: einmal draufhauen. Der Platzhalter darunter ist eine Barriere
  und damit vor Missgeschicken sicher; abgebaut wird über das Möbel selbst.
  Schutz-Plugins reden dabei ganz normal mit.
- Geht ein Modell doch einmal verloren (`/kill @e`), steht es beim nächsten
  Laden des Chunks von selbst wieder da – die Id liegt ja im Chunk.

### Bedrock und unbegrenzt – beides geht

Der `state:` daneben entscheidet, worauf das Möbel steht:

| | Platzhalter | Anzahl | Bedrock |
|---|---|---|---|
| **mit `state:`** | Notenblock, im Pack unsichtbar | 575 zusammen | sieht den echten Block |
| **ohne `state:`** | Barriere bzw. Licht | **unbegrenzt** | sieht nur das Objekt |

Beide drehen sich und beide kann man besitzen – der Unterschied ist nur, was
darunter liegt. Die 50 mitgelieferten Möbel haben einen `state:`, damit sie
auf Bedrock richtig aussehen. Brauchst du mehr als 575 eigene Blöcke, lass
den `state:` bei den neuen einfach weg.

Auf Java ist dieser Zustand im Pack ein Modell **ohne Quader, aber mit
genannter Textur** – du siehst also nur das gedrehte Anzeige-Objekt, nicht
den Notenblock darunter. Die Textur steht trotzdem drin, damit
`/smpcontent bedrock` sie beim Wiedereinlesen des Packs findet: Sonst gingen
genau die Möbel verloren, für die der ganze Aufwand gemacht wurde.

Nachgemessen auf einem laufenden Paper 1.21.11: Pack bauen → wieder einlesen
ergibt **74 von 74 Blöcken**.

## Fahrzeuge: Autos, Jets und Züge

In der `fahrzeuge.yml`. Gefahren wird mit den **normalen Bewegungstasten** –
Paper verrät sie dem Plugin, es braucht also keine Mod.

> **Wie sich ein Fahrzeug bewegt.** Es wird gesetzt, nicht geschubst. Das
> klingt nach einer Kleinigkeit, ist aber der Unterschied zwischen fahren und
> nicht fahren: Ein Rüstungsständer – und das ist ein Fahrzeug technisch –
> hat keine Stufenhöhe. Über `setVelocity()` bewegt, bleibt er an jeder
> Blockkante hängen, an jeder Stufe, an jedem Zaun. Deshalb rechnet das
> Plugin die Strecke selbst aus: in Vierteln vorwärts, damit man bei
> `speed: 2.6` nicht durch dünne Wände springt, vor einem Hindernis erst eine
> Stufe hoch, und mit eigener Schwerkraft. Steckt ein Fahrzeug doch einmal
> fest, hebt es sich bis zu vier Blöcke heraus, statt für immer eingemauert
> zu bleiben. Gemessen fährt jedes Fahrzeug jetzt genau sein `speed` –
> `0.85` sind 17 Blöcke/s, `2.45` sind 49.

```yaml
vehicles:
  sportwagen:
    art: auto          # auto | jet | zug
    item: auto_sport   # Id aus der config.yml, mit der man es hinstellt
    speed: 0.85
    power: 0.05        # wie schnell es beschleunigt
    turn: 5.0          # Grad pro Tick beim Lenken
    seats: 2
    fuel: "smp:kohle"  # leer = kein Sprit nötig
```

| | Steuerung |
|---|---|
| **Auto** | W Gas, S Bremse und rückwärts, A/D lenken. Fällt, und schafft eine Stufe. |
| **Boot** | Wie ein Auto, schwimmt aber oben auf. An Land kommt es kaum vorwärts. |
| **Jet** | W Schub, **Blickrichtung steuert die Nase**, Leertaste/Schleichen heben und senken gerade. |
| **Zug** | Fährt nur auf Schienen und folgt ihnen – auch um Kurven und bergauf. |

### Die mitgelieferten Sportwagen

Alle fahren nach denselben Regeln, sie fühlen sich nur anders an. Den
Charakter machen drei Werte: `speed` (Höchstgeschwindigkeit), `power`
(Antritt) und `turn` (wie eng es lenkt).

| Wagen | Tempo | Antritt | Lenkung | Plätze | wofür |
|---|---|---|---|---|---|
| **Roadster** | 0.80 | 0.07 | 7.5 | 2 | leicht und wendig, dreht fast auf der Stelle |
| **Sportwagen** | 0.85 | 0.05 | 5.0 | 2 | der Allrounder |
| **Muscle Car** | 1.00 | 0.09 | 3.0 | 2 | brutal geradeaus, träge in der Kurve |
| **Streifenwagen** | 1.05 | 0.08 | 5.5 | 4 | holt auch schnelle Leute ein |
| **Rennwagen** | 1.25 | 0.12 | 6.5 | 1 | Einsitzer für die Rennstrecke |
| **Hypercar** | 1.60 | 0.10 | 4.0 | 2 | Spitze, will lange Geraden |
| **Stier GT** | 1.75 | 0.15 | 5.5 | 2 | Mittelmotor-Schreihals, giftiger Antritt |
| **Veloce Hyper** | 2.30 | 0.11 | 2.6 | 2 | das schnellste Auto, schwerfällig in der Kurve |
| **Oldtimer** | 0.45 | 0.03 | 4.5 | 4 | gemütlich, für Spazierfahrten |

Dazu der Geländewagen und die Dampflok.

Viel `speed` **und** viel `turn` zusammen fühlt sich nervös an – deshalb lenkt
das Hypercar bewusst träger als der Roadster.

### Die 15 Supersportwagen

Fünfzehn Autos, die sich nur in der Farbe unterscheiden, wären fünfzehn Mal
dasselbe Auto. Deshalb gibt es **drei Familien** – jede mit eigener
Silhouette, eigener Karosserie und eigenem Fahrverhalten:

| Familie | Form | fährt sich |
|---|---|---|
| **Keil** | spitze Nase, gekapptes Heck, Spoilerkante | giftiger Antritt, sehr direkte Lenkung |
| **Rundheck** | Dachbogen fällt nach hinten weg, Entenbürzel | die besten Kurvenwerte im Spiel, oben herum nicht der schnellste |
| **Breitbau** | sehr lang und flach, Zweifarb-Linie | Endgeschwindigkeit ohne Ende, lenkt wie ein Schiff |

**Keil**

| Wagen | Tempo | Antritt | Lenkung | Plätze | wofür |
|---|---|---|---|---|---|
| **Countach** | 1.40 | 0.12 | 4.6 | 2 | der Älteste: langsamer, dafür genügsam |
| **Huracán** | 1.65 | 0.18 | 6.0 | 2 | kurzer Radstand, der wendigste Keil |
| **Diablo** | 1.72 | 0.15 | 5.6 | 2 | breit und ungezogen, bricht gern aus |
| **Stier GT** | 1.75 | 0.15 | 5.5 | 2 | der Hausmeister der Familie |
| **Aventador** | 1.80 | 0.16 | 5.2 | 2 | Zwölfzylinder, der Klassiker |
| **Revuelto** | 1.95 | 0.17 | 5.0 | 2 | Hybrid, stärkster Antritt der Familie |

**Rundheck**

| Wagen | Tempo | Antritt | Lenkung | Plätze | wofür |
|---|---|---|---|---|---|
| **911 Carrera** | 1.35 | 0.14 | 6.5 | 2 | fährt sich wie auf Schienen |
| **Taycan** | 1.45 | 0.22 | 6.0 | 4 | elektrisch: leise, volles Drehmoment ab Tick 1 |
| **911 GT3 RS** | 1.55 | 0.16 | 7.2 | 1 | `turn: 7.2` – die beste Lenkung im ganzen Plugin |
| **911 Turbo S** | 1.60 | 0.19 | 6.2 | 2 | Allrad, härtester Antritt der Familie |
| **917** | 1.70 | 0.15 | 5.8 | 1 | Rennwagen von 1970, Einsitzer, nichts für Anfänger |

**Breitbau**

| Wagen | Tempo | Antritt | Lenkung | Plätze | wofür |
|---|---|---|---|---|---|
| **Divo** | 2.15 | 0.12 | 3.4 | 2 | der handlichste Breitbau |
| **Veyron** | 2.20 | 0.11 | 2.8 | 2 | schwer, schnell, gutmütig |
| **Veloce Hyper** | 2.30 | 0.11 | 2.6 | 2 | das Hausmodell |
| **Bolide** | 2.35 | 0.15 | 4.0 | 1 | leicht: beschleunigt und lenkt besser, hält aber weniger aus |
| **Chiron** | 2.45 | 0.10 | 2.4 | 2 | sechzehn Zylinder, braucht eine sehr lange Gerade |
| **Tourbillon** | 2.60 | 0.13 | 2.6 | 2 | **das schnellste Fahrzeug überhaupt** |

Warum die schnellen so träge lenken: Bei `speed: 2.60` wäre alles über
`turn: 3` unfahrbar – ein Tick Lenkeinschlag wirft dich sonst quer. Wer lieber
Kurven fährt als Rekorde, nimmt den GT3 oder den Huracán. Alle drei Werte
stehen in der `fahrzeuge.yml` und sind deine.

> **Zu den Namen:** Die Wagen tragen die Namen, nach denen sie gebaut sind –
> das sind Marken anderer Leute, hier nur als Anzeigename im Spiel. Sie stehen
> in deiner `config.yml` und du kannst sie in einer Zeile ändern:
> `name: "<yellow>Mein Wagen"`. Die Ids (`auto_aventador` …) und alle
> Texturen sind selbst gemacht, keine Originaldateien.

### Flugzeuge, Jets und Hubschrauber

Geflogen wird **mit dem Kopf**: Wohin du schaust, da geht die Nase hin. Nach
oben ziehen steigt, nach unten drücken ist Sturzflug – wie stark, hängt vom
Schub ab. Was nach oben geht, fehlt vorne, du wirst im Steigflug also
langsamer. Leertaste und Schleichen heben und senken zusätzlich gerade, das
brauchst du zum Landen. Die Karosserie nickt sichtbar mit.

| Flieger | Tempo | Antritt | Lenkung | Plätze | |
|---|---|---|---|---|---|
| **Doppeldecker** | 0.75 | 0.06 | 5.5 | 2 | wendig, aber kein Renner |
| **Propellermaschine** | 0.90 | 0.05 | 3.5 | 2 | langsam und gutmütig, gut zum Üben |
| **Frachtflugzeug** | 1.10 | 0.03 | 1.0 | 8 | schwer, dreht wie ein Schiff |
| **Rennjet** | 1.40 | 0.06 | 3.0 | 1 | der Allrounder am Himmel |
| **Passagierjet** | 1.60 | 0.04 | 1.2 | 6 | groß und träge, für lange Strecken |
| **Kampfjet** | 2.20 | 0.14 | 4.5 | 1 | das schnellste Stück am Himmel |
| **Hubschrauber** | 0.70 | 0.05 | 6.0 | 4 | `hover` – bleibt stehen |
| **Rettungshubschrauber** | 0.85 | 0.06 | 5.0 | 6 | `hover` – bleibt stehen |

Der einzige echte Unterschied zwischen Flugzeug und Hubschrauber ist
`hover:` – ein Flugzeug sackt ohne Schub langsam durch, ein Hubschrauber
bleibt in der Luft stehen. Alles andere sind nur Zahlen.

### Schleudersitz und Fallschirm

**Zweimal schnell schleichen**, und es schießt dich aus dem Flugzeug – kurz
darauf geht der Schirm auf. Am Schirm sinkst du gemütlich und lenkst mit den
normalen Bewegungstasten; schräg gegen den Wind kommt man erstaunlich weit.

Damit der erste Schleicher nicht schon der Absprung ist, kann man **im Flug
nicht einfach aussteigen** – am Boden geht es normal.

```yaml
    eject:
      enabled: true
      window: 1.0          # so schnell müssen die zwei Schleicher kommen
      power: 1.5           # wie hoch es dich schleudert
      forward: 0.3         # wieviel davon nach vorne geht
      sound: entity.firework_rocket.launch
      parachute: fallschirm  # deine eigene Item-Id, leer = kein Schirm
      open-after: 8        # Ticks, bis der Schirm aufgeht
      fall-speed: 0.18     # wie schnell man sinkt
      drift: 0.09          # wie gut man lenken kann
```

Den **Fallschirm** gibt es auch als Item zum Craften (3× Wolle, 3× Faden).
Rechtsklick damit im freien Fall öffnet ihn ebenfalls – man braucht also kein
Flugzeug dafür.

### Abstürze

Gegen eine Wand fahren tut weh. Erkannt wird das daran, dass das Fahrzeug
**waagerecht** viel weniger weit gekommen ist, als es wollte. Nur waagerecht:
Vorher zählte die Senkrechte mit, und das hat Flieger reihenweise gesprengt –
wer im Stand die Leertaste drückte, wollte einen halben Block nach oben, kam
wegen des Bodens nicht vom Fleck, und das galt als Volltreffer. Man stieg
ein, drückte Leertaste, und der Jet explodierte unter einem. Für den
Sturzflug in den Boden gibt es weiter `hard-landing`, das die
Sinkgeschwindigkeit misst. Die erste Sekunde nach dem Hinstellen ist
außerdem Schonzeit.

```yaml
    crash:
      enabled: true
      min-speed: 0.45      # ab welchem Tempo es kracht
      damage: 7.0          # Schaden für alle Insassen
      explosion: 1.0       # Stärke, 0 = keine
      break-blocks: false  # Landschaft bleibt heil
      destroy: true        # Fahrzeug ist danach hin
      sound: entity.generic.explode
```

Dazu zwei Werte, die man leicht vergisst:

```yaml
      ram: 6.0             # Schaden für den, den du umfährst (0 = niemand)
      hard-landing: 0.9    # Sinkgeschwindigkeit, ab der ein Flieger zu Bruch geht
```

Gezählt wird **auch senkrecht**: Wer im Sturzflug in den Boden geht, kommt
waagerecht kaum vom Fleck – das wäre sonst gar kein Aufprall. Und wer zu hart
aufsetzt, landet nicht, sondern verunglückt; sanft aufsetzen ist eine
Landung.

Die mitgelieferten Werte sind abgestuft: Der Oldtimer bekommt eine Delle
(4 Schaden, keine Explosion, bleibt heil), der Kampfjet wird zum Feuerball
(14 Schaden, Explosion 4). Blöcke zerstört standardmäßig **nichts** – auf
einem Schulserver will niemand Krater in der Landschaft.

### Tacho

`hud: true` (Standard) zeigt über der Hotbar Tempo, bei Fliegern die Höhe und
bei Fahrzeugen mit Sprit den Füllstand. Angezeigt wird die **wirklich
gefahrene Strecke** in Blöcken pro Sekunde – nicht der eingestellte Wert. So
siehst du beim Einstellen sofort, ob eine Zahl hält, was sie verspricht.

Rechtsklick auf den Boden stellt es hin, Rechtsklick darauf steigt ein,
Schleichen steigt aus, Schleichen + Rechtsklick mit leerer Hand packt es
wieder ein. Nach einem Neustart stehen die Fahrzeuge noch da und fahren weiter.

**Die Leertaste hupt** – `horn: block.note_block.bit` beim Sportwagen,
`entity.ghast.warn` beim Schiffshorn der Yacht, `entity.ravager.roar` beim
Muscle Car. Bei Fliegern nicht, dort ist die Leertaste zum Steigen da.

**Drei Boote** sind dabei: Ruderboot (2 Plätze, gemütlich), Motorboot
(4 Plätze, schnell) und Yacht (8 Plätze, dreht wie ein Schiff).

## Schusswaffen

Die Aktion `projectile` macht aus jedem Item eine Waffe:

```yaml
pistole:
  material: IRON_HORSE_ARMOR
  abilities:
    - trigger: right-click
      cooldown: 0.4
      actions:
        - type: projectile
          entity: ARROW      # ARROW, SNOWBALL, FIREBALL, WIND_CHARGE, EGG ...
          speed: 3.6
          damage: 5.0
          spread: 0.6        # Streuung in Grad
          amount: 1          # 8 macht daraus eine Schrotflinte
          gravity: false     # fliegt geradeaus
          ammo: "smp:kugel"  # Munition, die verbraucht wird
          # bullet: false    # -> stattdessen ein klassischer Pfeil
```

| Waffe | Feuerrate | Schaden | wofür |
|---|---|---|---|
| **Pistole** | 0,4 s | 5 | der Allrounder |
| **Schrotflinte** | 1,5 s | 8 × 2,5 | auf kurze Entfernung |
| **Maschinenpistole** | 0,15 s | 3 | schnell, streut |
| **Minigun** | 0,1 s | 2 × 2,5 | frisst Munition wie nichts |
| **Scharfschützengewehr** | 2,0 s | 16 | ein Schuss, der sitzt, durchschlägt zwei |
| **Flammenwerfer** | 0,2 s | Feuer | kurze Reichweite, brennt nach |
| **Railgun** | 3,0 s | 13 | Strahl über 30 Blöcke, trifft alles auf der Linie |
| **Handgranate** | 1,2 s | Explosion | fliegt im Bogen |
| **Jagdbogen** | 0,8 s | – | echter Pfeil, leise, aufsammelbar |
| **Raketenwerfer** | 6,0 s | Explosion | wenn gar nichts mehr hilft |

Ist keine Munition da, klickt es nur. Kugeln macht man aus Eisen, für die
Railgun gibt es **Energiezellen**.

### Warum eine Kugel keine Pfeil ist

Ein Geschoss aus einer Fähigkeit ist standardmäßig eine **Kugel**: unsichtbar
unterwegs, mit feiner Leuchtspur, Mündungsfeuer vorn an der Waffe, und beim
Aufschlag verschwindet sie. Trifft sie etwas Lebendiges, gibt es ein
Trefferzeichen aus Funken und einen kurzen Ton für den Schützen. Das ist der
Standard, weil ein Pfeil zwei Dinge tut, die bei einer Waffe niemand will:

**Er bleibt liegen.** Eine Minute lang. Wer mit der Pistole auf den Boden
hält, steht danach vor einem Igel aus Pfeilen – im Kreativmodus am
deutlichsten, weil dort keine Munition verbraucht wird.

**Er macht bei schnellem Feuer keinen Schaden.** Das ist der wichtigere
Punkt. Nach einem Treffer ist ein Ziel **zehn Ticks unverwundbar**. Die
Pistole darf alle acht Ticks schießen, die Schrotflinte schickt acht Kugeln
auf einmal – über den normalen Pfeilweg kommt also jeder zweite Schuss gar
nicht an und von acht Schrotkugeln genau eine. Die Waffe fühlt sich an, als
mache sie überhaupt keinen Schaden. Eine Kugel hebt die Sperre auf und trägt
ihren Schaden selbst ein, deshalb zählt jeder Treffer.

Damit ist `damage` auch **genau das, was ankommt**. Vanilla rechnet
Pfeilschaden mal Fluggeschwindigkeit; aus `damage: 5.0` wurden bei
`speed: 3.6` achtzehn Punkte. Jetzt sind fünf auch fünf.

Gemessen auf dem Server: vier Pistolenschüsse im Abstand von 0,45 s auf einen
Zombie mit 100 Leben → 80,3 übrig, also 4×4,9. Vorher kam davon knapp die
Hälfte an, mit vollkommen unvorhersagbarem Wert.

Wer wirklich einen klassischen Pfeil will – sichtbar, liegenbleibend,
aufsammelbar –, schreibt `bullet: false` in die Aktion.

## Bosse

`/smpcontent boss <Id>` stellt einen hin – dorthin, wohin du schaust. Aus der
Konsole mit Koordinaten: `/smpcontent boss leerenwandler 100 64 -30`.

Ein Boss ist ein normales Mob mit aufgebohrten Werten, einer **Lebensleiste**
oben am Bildschirm und – das ist der eigentliche Punkt – **Angriffen, die
sich mit sinkendem Leben ändern**. Im Plugin steht kein einziger Boss fest,
alles liegt in der `bosse.yml`.

| Boss | Grundmob | Leben | Phasen | wofür |
|---|---|---|---|---|
| **Grabfürst** | Zombie | 300 | 3 | der Einstieg, ruft Untote |
| **Frostmonarch** | Stray | 260 | 2 | bleibt auf Abstand, friert dich fest |
| **Aschekönig** | Blaze | 220 | 2 | Feuerkugeln im Dauerlauf, heilt sich |
| **Steinkoloss** | Eisengolem | 500 | 2 | langsam, zäh, schlägt sehr hart |
| **Leerenwandler** | Witherskelett | 600 | 4 | der schwerste, kommt zu zweit besser |

### Phasen

`ab:` ist der Lebensanteil in Prozent, ab dem eine Phase gilt. Bei den
Schwellen 100 / 70 / 40 / 15 heißt das: von 100 bis 70 die erste, von 70 bis
40 die zweite und so weiter. Beim Übergang gibt es Ansage, Ton und eine
Animation.

Ein Boss, der sich heilt, rutscht dabei ausdrücklich **zurück** in die vorige
Phase – der Leerenwandler tut genau das in seinem letzten Abschnitt. Damit
das an der Schwelle nicht flackert, gibt es drei Sekunden Sperre zwischen
zwei Auftritten.

### Angriffe

Jeder Angriff hat eine eigene Abklingzeit und feuert von selbst, sobald
jemand nah genug ist:

| `typ:` | was passiert |
|---|---|
| `welle` | Ring um den Boss, wirft alle weg |
| `strahl` | Linie auf das Ziel zu, trifft alles darauf |
| `sprung` | springt zum Ziel |
| `zug` | zieht alle heran – kein Weglaufen |
| `meteor` | Einschläge um das Ziel herum, **mit Vorwarnung am Boden** |
| `diener` | ruft Helfer (gedeckelt auf zwölf, sonst geht der Server in die Knie) |
| `trank` | Effekt auf alle in Reichweite |
| `heilen` | heilt sich selbst |
| `geschoss` | Feuerkugeln |

Das Aussehen kommt aus der `animationen.yml`: `animation: einschlag` nimmt
genau die Form, die dort steht. Wer sie dort ändert, ändert damit auch, wie
der Boss zuschlägt – ohne eine Zeile Java.

Boss-Schaden umgeht wie eine Kugel die Unverwundbarkeitssperre. Zwei Angriffe
kurz hintereinander treffen deshalb auch zweimal, statt dass der zweite
verschluckt wird.

### Beute

Was fällt, steht unter `beute:` – Ids aus deiner `config.yml` oder normale
Materialien, jeweils mit Menge und Chance. Die Ausrüstung des Bosses fällt
**nie** zufällig herunter; es gibt genau das, was du einträgst. Seine Diener
verschwinden mit ihm.

Eine eigene Testreihe prüft die ganze Datei, bevor sie im Spiel auffällt:
Gibt es das Grundmob wirklich und ist es etwas Lebendiges? Kennt das Plugin
jeden Angriffstyp? Steht jede genannte Animation in der `animationen.yml`,
jedes Teilchen in Minecraft, jede Beute-Id in der `config.yml`? Und liefert
die Phasenauswahl bei jedem Lebensstand von 100 % bis 0 % genau eine Phase?
Der Prüflauf hat beim ersten Durchgang gleich einen echten Fehler gefunden –
die Phasensuche nahm die erste passende Schwelle statt der tiefsten, damit
wäre ein Boss nie über seine erste Phase hinausgekommen.

## Wie die Blöcke funktionieren

Minecraft erlaubt keine echten neuen Block-IDs über ein Resource-Pack. Die
gängige Lösung (die auch große Content-Plugins nutzen) sind **Note-Block-
Zustände**: Jeder eigene Block ist ein Notenblock mit einer festen
Instrument/Noten-Kombination, und das Texturepack lenkt genau diese
Kombination auf ein eigenes Modell.

**Der Zustand ist aber nur das Aussehen.** Wer der Block ist, steht zusätzlich
im Chunk gespeichert. Verändert also doch einmal etwas den Zustand (WorldEdit,
`/setblock`, ein anderes Plugin), erkennt das Plugin den Block weiterhin, gibt
beim Abbauen das richtige Item und stellt das Aussehen beim nächsten Laden des
Chunks von selbst wieder her. Kolben können eigene Blöcke nicht verschieben,
und Explosionen droppen das richtige Item.

### Drops einstellen

Ein Block lässt beim Abbauen standardmäßig sich selbst fallen. Für Erze ist das
falsch – die sollen Rohstoffe geben:

```yaml
ruby_ore:
  state: "minecraft:note_block[instrument=bit,note=1,powered=false]"
  drops:
    - item: "smp:ruby"      # Material oder "smp:id"
      amount: 1-2           # feste Zahl oder Bereich
      chance: 1.0           # 0.0 bis 1.0
      fortune: true         # Glücksbringer erhöht die Menge
  experience: 3             # Erfahrung beim Abbauen
```

**Behutsamkeit** gibt immer den Block selbst und keine Erfahrung – wie bei
Vanilla-Erzen. Mehrere Einträge unter `drops:` fallen alle zusammen.

Damit der Zustand hält, unterdrückt das Plugin für **genau diese Blöcke** drei
Vanilla-Verhalten: Instrumentwechsel durch den Block darunter, Umstimmen per
Rechtsklick und den Notenklang. **Normale Notenblöcke bleiben unberührt** –
sie funktionieren wie immer.

Beim Rechtsklick wird nur noch die *Block*-Interaktion gesperrt, nicht mehr das
ganze Event – vor einem eigenen Block kann man also weiter essen, einen Eimer
benutzen oder einen Bogen spannen.

> Die Zustände in der `config.yml` müssen exakt zu
> `texturepack/dist/blocks-states.yml` passen. Änderst du einen, ändere beide.

## Wichtig

Ohne installiertes Texturepack funktionieren alle Blöcke und Items ganz
normal – sie sehen dann nur aus wie ihr Basis-Item (Notenblock,
Amethystsplitter …). Das Pack liefert nur das Aussehen.

## Befehle & Rechte

| Befehl | was er tut |
|---|---|
| `/smpcontent list [Suche]` | GUI mit allen Inhalten, Klick = 1, Shift-Klick = 64 |
| `/smpcontent give <Spieler> <Id> [Menge]` | gezielt vergeben |
| `/smpcontent boss <Id> [x y z]` | einen Boss hinstellen |
| `/smpcontent pack` | Java-Pack bauen, danach gleich das Bedrock-Pack |
| `/smpcontent bedrock [URL\|Datei]` | nur das Bedrock-Pack, aus deinem eigenen Java-Pack |
| `/smpcontent reload` | config.yml, fahrzeuge.yml, bosse.yml und Texte neu einlesen |

Rechte: alles davon braucht `smpcontent.admin` (Standard: OP).
`smpcontent.place` darf eigene Blöcke setzen (Standard: alle).
