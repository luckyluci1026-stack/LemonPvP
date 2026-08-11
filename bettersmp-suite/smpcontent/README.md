# SMPContent

Eigene **Blöcke**, **Items** und **Spezialfähigkeiten** (Paper 1.21.11).

## Inhalt

| Datei | Inhalt |
|---|---|
| `config.yml` | 8 Blöcke, 7 Items (Rubin, Saphir, Rubinschwert …) |
| `content/laserschwerter.yml` | 10 Laserschwerter, Handy, Haltbarer Stock – **mit Fähigkeiten** |
| `content/platzhalter-bloecke.yml` | **100 Platzhalter-Blöcke**, noch ohne Textur |
| `content/platzhalter-items.yml` | **150 Platzhalter-Items**, noch ohne Textur |
| `animationen.yml` | eigene Teilchen-Animationen (Formeln) |
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

## Wie die Blöcke funktionieren

Minecraft erlaubt keine echten neuen Block-IDs über ein Resource-Pack. Die
gängige Lösung (die auch große Content-Plugins nutzen) sind **Note-Block-
Zustände**: Jeder eigene Block ist ein Notenblock mit einer festen
Instrument/Noten-Kombination, und das Texturepack lenkt genau diese
Kombination auf ein eigenes Modell.

Damit der Zustand hält, unterdrückt das Plugin für **genau diese Blöcke** drei
Vanilla-Verhalten: Instrumentwechsel durch den Block darunter, Umstimmen per
Rechtsklick und den Notenklang. **Normale Notenblöcke bleiben unberührt** –
sie funktionieren wie immer.

> Die Zustände in der `config.yml` müssen exakt zu
> `texturepack/dist/blocks-states.yml` passen. Änderst du einen, ändere beide.

## Wichtig

Ohne installiertes Texturepack funktionieren alle Blöcke und Items ganz
normal – sie sehen dann nur aus wie ihr Basis-Item (Notenblock,
Amethystsplitter …). Das Pack liefert nur das Aussehen.

## Befehle & Rechte

- `/smpcontent give|list|reload` – Recht `smpcontent.admin` (Standard: OP)
- `smpcontent.place` – darf eigene Blöcke setzen (Standard: alle)
