# SMPContent

Eigene **Blöcke**, **Items** und **Spezialfähigkeiten** (Paper 1.21.11).

## Inhalt

**412 eigene Inhalte**, alle mit Textur außer den Platzhaltern:

| Datei | Inhalt |
|---|---|
| `config.yml` | 24 Blöcke, 88 Items – Erze, Marmor, Laserschwerter, Tränke, Werkzeuge, Früchte, **Waffen**, **Fahrzeuge**, Fallschirm |
| `content/moebel.yml` | **50 Möbel** – drehbar, zum Draufsetzen, mit echten 3D-Formen |
| `content/platzhalter-bloecke.yml` | **100 Platzhalter-Blöcke**, absichtlich ohne Textur |
| `content/platzhalter-items.yml` | **150 Platzhalter-Items**, absichtlich ohne Textur |
| `fahrzeuge.yml` | **20 Fahrzeuge** – 8 Autos, 8 Flieger, 3 Boote, 1 Lok |
| `animationen.yml` | 13 Teilchen-Animationen (Formeln), eigene bauen |
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
  **Nur Fahrzeuge**. So findest du die 20 Fahrzeuge und die 74 Möbel, ohne
  ihre Ids zu kennen.
- **Suche**: `/smpcontent list laser` zeigt nur passende Einträge. Gesucht wird
  in der Id **und** im Anzeigenamen. Das Fernrohr im GUI hebt die Suche auf.
- Passt etwas nicht mehr ins Inventar, fällt es vor die Füße statt zu verschwinden.

**3. `/smpcontent give <Spieler> <Id> [Menge]`** – gezielt vergeben.

## Texturen sind schon dabei

Beim ersten Start legt das Plugin **237 fertige Texturen und 3D-Modelle** in
deinen Datenordner – **alles außer den Platzhaltern hat eine Textur**:

| | |
|---|---|
| 74 Blöcke | 50 Möbel, Erze, Marmor, Neonlampe, Münzhaufen, deine Stühle und Tische |
| 85 Items | 17 Fahrzeuge, Schwerter, Tränke, Werkzeuge, Früchte, Schlüssel, Münzen … |
| 78 Modelle | echte Formen statt Würfel |

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

Der Name wird dann als `shape:` benutzt. Mitgeliefert als Beispiele: Spirale,
Herz, Doppelring, Druckwelle, Tornado, Bohrer, Flügel, Funkenregen.

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
| **Oldtimer** | 0.45 | 0.03 | 4.5 | 4 | gemütlich, für Spazierfahrten |

Viel `speed` **und** viel `turn` zusammen fühlt sich nervös an – deshalb lenkt
das Hypercar bewusst träger als der Roadster. Dazu gibt es weiterhin den
Geländewagen und die Dampflok.

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
viel weniger weit gekommen ist, als es wollte:

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
```

Mitgeliefert sind **Pistole**, **Schrotflinte** (acht Kugeln plus Rückstoß)
und **Raketenwerfer**. Ist keine Munition da, klickt es nur.

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

- `/smpcontent give|list|reload` – Recht `smpcontent.admin` (Standard: OP)
- `smpcontent.place` – darf eigene Blöcke setzen (Standard: alle)
