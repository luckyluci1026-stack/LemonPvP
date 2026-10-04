# SMP Texture-Pack

Ein Pack für **beide Plattformen** — Java **und** Bedrock. Alle Grafiken werden
von `generate.py` prozedural gezeichnet, es gibt also keine Binärdateien, die du
nicht bearbeiten könntest.

```bash
cd texturepack
python3 generate.py
```

Ergebnis in `dist/`:

| Datei | Wofür |
|---|---|
| `SMP-Java-Pack.zip` | Java-Spieler (in `resourcepacks/` oder als Server-Pack) |
| `SMP-Bedrock-Pack.mcpack` | Bedrock-Spieler (Doppelklick importiert es) |
| `geyser/smp_items.json` | Geyser-Mapping für die Custom-Items |
| `glyphen.txt` / `glyphs.json` | Übersicht aller Icon-Zeichen |

## Was drin ist

**16 Icons** (Münze, Herz, Stern, Krone, Einkaufswagen, Haken, Kreuz, Warnung,
Pfeile, Edelstein, Uhr, Schloss, Geldbeutel, Schwert, Spitzhacke) auf den
Codepoints `U+E100`–`U+E10F`.

**9 Rang-Abzeichen mit echtem Farbverlauf** auf `U+E110`–`U+E118`:
Owner 1-5, Admin, Mod, Sup, Member.

**3 Custom-Items** mit eigenen Texturen: Herz, Wiederbelebungs-Totem, Münze.

## Wichtig zum Thema „Gradients auf Bedrock"

Damit du nicht in eine Falle läufst — die ehrliche technische Lage:

**Bedrock kann keine RGB-/Hex-Farben in Text darstellen.** Das ist eine
Einschränkung des Bedrock-Clients selbst, kein Geyser-Problem, und **kein
Resource-Pack der Welt ändert das.** Ein `<gradient:#FFFB00:#00FF00>Owner</gradient>`
wird von Geyser zwangsläufig auf die nächstgelegene der ~16 Legacy-Farben
heruntergerechnet.

**Deshalb der Trick in diesem Pack:** Ein Farbverlauf, der als **Bild** vorliegt,
ist kein Text mehr — und Bilder haben volle Farbtiefe, auf jeder Plattform. Die
Rang-Abzeichen sind genau das: kleine Schild-Grafiken mit echtem Verlauf, die als
Schriftzeichen eingebunden sind. Ein Bedrock-Spieler sieht sie **pixelgenau
identisch** zu einem Java-Spieler.

Praktisch heißt das: Setz vor den Rang-Namen das passende Abzeichen, dann sieht
jeder den Farbverlauf — auch wenn der Rang-*Text* auf Bedrock einfarbig bleibt.

```
/lp group owner1 meta setprefix 1000 " <gradient:#FFFB00:#00FF00>Owner</gradient> "
```

Java-Spieler sehen: Abzeichen mit Verlauf **+** Text mit Verlauf.
Bedrock-Spieler sehen: Abzeichen mit Verlauf **+** Text einfarbig.

## Einbau

### Java
Server-Pack in `server.properties`:
```properties
resource-pack=https://dein-host/SMP-Java-Pack.zip
resource-pack-sha1=<sha1 der zip>
```
Oder die Zip einfach in den `resourcepacks/`-Ordner des Clients.

### Bedrock (Geyser)
1. `SMP-Bedrock-Pack.mcpack` nach `plugins/Geyser-Spigot/packs/` legen.
2. Geyser neu laden — er schickt es Bedrock-Spielern automatisch.

### Custom-Items auf Bedrock
`geyser/smp_items.json` nach `plugins/Geyser-Spigot/custom_mappings/` kopieren
und Geyser neu starten.

> Geysers Custom-Item-Format hat sich über die Versionen mehrfach geändert. Die
> mitgelieferte Mapping-Datei nutzt das etablierte `custom_model_data`-Format.
> Falls deine Geyser-Version meckert, sagt sie in der Konsole welches Feld sie
> erwartet — das ist dann eine Ein-Zeilen-Anpassung.

## Icons in Configs verwenden

In FastShop stehen die Icons als `%g:coin%`, `%g:cart%`, `%g:check%` usw. zur
Verfügung (siehe `glyphs`-Abschnitt in der `config.yml`). In allen anderen
Configs kannst du die Zeichen direkt aus `dist/glyphen.txt` einfügen oder die
``-Schreibweise nutzen.

## Selbst zeichnen

Jede Grafik ist im Generator ein Zeichen-Raster — ein Buchstabe = ein Pixel:

```python
"coin": """
.....KKKKKK.....
...KKggggggKK...
..KgGGGGGGGGgK..
...
"""
```

`.` ist transparent, alle anderen Buchstaben sind Farben aus der Palette
darüber (`K` = dunkler Rand, `G` = Gold hell, `g` = Gold dunkel …). Ändere die
Zeichen oder die Palette, führe `python3 generate.py` aus — fertig. Für die
Rang-Abzeichen reicht es, in `RANKS` die beiden Hex-Farben zu tauschen.
