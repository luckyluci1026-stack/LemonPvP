# Bedrock-Spieler sollen die 3D-Sachen sehen

Geyser kann eigenen Items ein Bedrock-Aussehen geben. Dafür braucht es
zwei Dinge, und beide erzeugt `java2bedrock.py` aus **deinem** Java-Pack.

## Bauen

```bash
cd texturepack
python3 java2bedrock.py /pfad/zu/SMPJavaPack.zip
```

Braucht nur Python 3, keine Zusatzbibliothek. Ergebnis in `dist/bedrock/`:

| Datei | wohin |
|---|---|
| `SMP-Bedrock-Pack.mcpack` | `Geyser/packs/` |
| `smp_items.json` | `Geyser/custom_mappings/` |

Danach in der `Geyser/config.yml`:

```yaml
add-non-bedrock-items: true
```

Geyser neu starten. Bedrock-Spieler laden das Pack beim Verbinden.

## Was umgewandelt wird

- **3D-Modelle**: Jeder Quader aus deinem Blockbench-Modell wird in
  Bedrock-Geometrie übersetzt. Bedrock spiegelt die X-Achse und legt den
  Nullpunkt in die Mitte – das rechnet das Skript um, ebenso die UV-Koordinaten
  und Drehungen.
- **Haltung in der Hand**: Die `display`-Werte aus Blockbench werden zu
  Bedrock-Animationen, damit das Item in erster und dritter Person richtig sitzt.
- **Flache Items** (ohne `elements`) bekommen nur die Textur – Bedrock zeichnet
  sie wie gewohnt.

## Was NICHT geht

**Eigene Blöcke.** Die Möbel, Erze und Marmorblöcke sind Note-Block-Zustände.
Geyser bildet so etwas nicht als Custom-Item ab – dafür bräuchte es Geysers
Custom-Blocks, die eine ganz andere Definition verlangen. Bedrock-Spieler sehen
diese Blöcke deshalb als normale Notenblöcke. Alles funktioniert (setzen,
abbauen, Drops), nur die Textur fehlt.

Die Items dagegen – Laserschwerter, Tränke, Werkzeuge, Früchte – sehen auf
Bedrock genauso aus wie auf Java.

## Ungetestet

Ich konnte das Pack nicht an einem laufenden Geyser prüfen. Die Umrechnung ist
nachgerechnet (ein Quader `from [8,0,8] to [9,32,9]` wird korrekt zu
`size [1,32,1]`, `origin [-1,0,0]`), aber ob jedes Modell in der Hand perfekt
sitzt, siehst du erst im Spiel. Falls etwas schief hängt: die Werte stehen in
`animations/<id>.animation.json` im Pack und lassen sich dort nachjustieren.
