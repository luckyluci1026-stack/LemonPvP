# SMPContent

Eigene **Blöcke**, **Items** und **Spezialfähigkeiten** (Paper 1.21.11).

## Inhalt

| Datei | Inhalt |
|---|---|
| `config.yml` | 24 Blöcke, 62 Items – Erze, Marmor, Möbel, Laserschwerter, Tränke, Werkzeuge, Früchte |
| `content/platzhalter-bloecke.yml` | **100 Platzhalter-Blöcke**, noch ohne Textur |
| `content/platzhalter-items.yml` | **150 Platzhalter-Items**, noch ohne Textur |
| `animationen.yml` | 13 Teilchen-Animationen (Formeln), eigene bauen |
| `FAEHIGKEITEN.txt` | alle Auslöser und Aktionen erklärt |

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

- **Filter** (Trichter): Alles → Nur Blöcke → Nur Items
- **Suche**: `/smpcontent list laser` zeigt nur passende Einträge. Gesucht wird
  in der Id **und** im Anzeigenamen. Das Fernrohr im GUI hebt die Suche auf.
- Passt etwas nicht mehr ins Inventar, fällt es vor die Füße statt zu verschwinden.

**3. `/smpcontent give <Spieler> <Id> [Menge]`** – gezielt vergeben.

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

Auf Java ist dieser Zustand im Pack ein leeres Modell – du siehst also nur
das gedrehte Anzeige-Objekt, nicht den Notenblock darunter.

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
| **Jet** | W Schub, Leertaste steigen, Schleichen sinken. Ohne Schub sinkt er. |
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
Geländewagen, den Rennjet und die Dampflok.

Rechtsklick auf den Boden stellt es hin, Rechtsklick darauf steigt ein,
Schleichen steigt aus, Schleichen + Rechtsklick mit leerer Hand packt es
wieder ein. Nach einem Neustart stehen die Fahrzeuge noch da und fahren weiter.

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
