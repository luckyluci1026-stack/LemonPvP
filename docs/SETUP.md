# Einrichtung

Anleitung fuer einen Paper-Server, auf dem Java- **und** Bedrock-Spieler
zusammen Helden 3 spielen.

## Voraussetzungen

| | |
| --- | --- |
| Server | Paper 1.21.x (Spigot geht auch) |
| Java | 21 |
| Bauen | Maven 3.8+, Python 3.8+ (nur fuer die Packs), `zip` |
| Bedrock | Geyser-Spigot + Floodgate (beides optional) |

---

## 1. Plugin bauen und installieren

```bash
cd plugin
mvn clean package
```

Ergebnis: `plugin/target/Helden3-3.0.0.jar` → nach `plugins/` kopieren und den
Server einmal starten. Dabei entsteht `plugins/Helden3/` mit `config.yml`,
`items.yml`, `messages.yml` und dem Ordner `data/`.

> Der Build laedt die Paper-API von `repo.papermc.io`. Auf einem Rechner ohne
> Internetzugang schlaegt er fehl — das ist kein Fehler im Projekt.

Danach im Spiel:

```
/helden3 spawn      Projektspawn auf die eigene Position setzen
/helden3 status     Regelwerk und Zaehlerstand pruefen
```

### Season starten

Alle Teilnehmer einmal joinen lassen, dann:

```
/helden3 reset bestaetigen
```

Das setzt alle Herzen auf den Startwert, hebt Eliminierungen auf und loest alle
Link-Herzen. Die Bestaetigung ist Absicht — sonst loescht ein Vertipper die
laufende Season.

---

## 2. Bedrock anbinden (Geyser + Floodgate)

### 2.1 Plugins

`Geyser-Spigot.jar` und `floodgate-spigot.jar` nach `plugins/`, Server neu
starten.

### 2.2 Namensprefix abgleichen

Floodgate haengt Bedrock-Namen ein Prefix an (Standard `.`). Derselbe Wert muss
in unserer `config.yml` stehen, damit die Erkennung auch ohne Floodgate-API
greift:

```yaml
bedrock:
  detect-floodgate: true
  username-prefix: "."      # identisch mit Floodgate
```

Laeuft Floodgate, nutzt das Plugin dessen API (`/helden3 status` zeigt
`Floodgate: ja`). Sonst greift der Fallback ueber die Floodgate-UUID-Form und
das Prefix.

### 2.3 Bedrock-Pack ausliefern

```bash
./tools/package_packs.sh
cp dist/Helden3-Bedrock.mcpack           plugins/Geyser-Spigot/packs/
cp geyser/custom_mappings/helden3.json   plugins/Geyser-Spigot/custom_mappings/
```

Geyser neu starten. Legt deine Geyser-Version den Ordner `custom_mappings`
nicht selbst an, erstelle ihn von Hand. Ob deine Version das Laden von
Custom-Items noch ueber eine Option in der Geyser-`config.yml` steuert, steht in
der [Geyser-Dokumentation](https://geysermc.org/wiki/geyser/custom-items/).

Beides wird gebraucht:

- die **.mcpack** liefert die Texturen an den Bedrock-Client,
- das **Mapping** sagt Geyser, dass `red_dye` mit `CustomModelData 3001` auf
  Bedrock das Herz-Item ist.

Fehlt das Mapping, sehen Bedrock-Spieler roten Farbstoff — spielbar, aber ohne
die Projektoptik. Am Regelwerk aendert das nichts.

---

## 3. Java-Pack ausliefern

`dist/Helden3-Java.zip` auf einen Webspace legen und in `server.properties`
eintragen:

```properties
resource-pack=https://deine-domain.de/Helden3-Java.zip
resource-pack-sha1=<sha1 aus tools/package_packs.sh>
resource-pack-prompt=Helden 3 braucht dieses Pack fuer die Artefakte.
require-resource-pack=true
```

Den SHA-1 gibt `tools/package_packs.sh` am Ende aus. **Nach jeder Aenderung am
Pack neu setzen**, sonst laden die Clients die alte Version aus dem Cache.

Das Pack bringt beide Modellformate mit und laeuft dadurch von 1.14 bis heute:

- `assets/minecraft/models/item/…` — klassische Overrides (bis 1.21.3)
- `assets/minecraft/items/…` — Item-Definitionen (ab 1.21.4)

---

## 4. Eigene Artefakte hinzufuegen

1. Pixelbild in `tools/generate_textures.py` ergaenzen und in `ITEMS` mit einer
   Farbpalette eintragen.
2. Eintrag in `plugin/src/main/resources/items.yml` anlegen — mit **neuer**
   `custom-model-data` und `model: handheld` oder `model: generated`.
3. Erzeugen, bauen, pruefen:

```bash
python3 tools/generate_textures.py
python3 tools/build_packs.py      # pip install pyyaml
python3 tools/validate.py
./tools/package_packs.sh
```

`build_packs.py` schreibt daraus Java-Modelle, `item_texture.json`, die
Bedrock-Sprachdateien und das Geyser-Mapping. Doppelte oder fehlende
`custom-model-data` bricht das Skript mit einer Meldung ab.

### `model: none`

Manche Vanilla-Items haben ein Sondermodell, das man nicht ersetzen darf — ein
Schild etwa ist 3D und hat eine Blockanimation. Wuerden wir es ueberschreiben,
saehen **alle** Schilde auf dem Server kaputt aus, und auf Bedrock liesse sich
damit nicht mehr blocken. Fuer solche Basisitems setzt du `model: none`: das
Artefakt behaelt die Vanilla-Optik und bekommt nur Namen und Lore.

Der Bogen ist der andere Sonderfall — dort baut `build_packs.py` die
Vanilla-Spannanimation nach, damit normale Boegen sie behalten.

---

## 5. Ohne Bedrock

Ohne Geyser und Floodgate laeuft alles ganz normal als Java-Projekt. Das Plugin
bindet beide **nicht** als Dependency ein, sondern spricht Floodgate ueber
Reflection an — fehlt es, ist einfach nie jemand "Bedrock".

---

## Problemsuche

| Symptom | Ursache |
| --- | --- |
| Herzen aendern sich nicht | `hearts.pvp-only` steht auf `true` — Sturz, Lava und Mobs kosten absichtlich nichts. Nur Spielerkills zaehlen |
| Herzleiste zeigt weiter 10 Herzen | Ein anderes Plugin setzt die Maximalgesundheit ebenfalls. `/helden3 status` zeigt die gespeicherten Herzen |
| Kein Link-Partner zugelost | Es war kein anderer Spieler mit Herzen verfuegbar — die Meldung dazu steht im Chat des Betroffenen |
| Dummy bleibt stehen | `dummy.lifetime-seconds` abwarten oder `/helden3 dummy clear` |
| Dummy droppt nichts | Er ist abgelaufen statt getoetet worden — dann behaelt der Spieler alles |
| Java-Spieler sehen Vanilla-Items | Pack nicht geladen, oder `resource-pack-sha1` passt nicht zur Datei |
| Bedrock-Spieler sehen Vanilla-Items | Mapping fehlt in `custom_mappings/`, oder Geyser wurde nicht neu gestartet |
| Bedrock-Spieler sehen kaputte Farbcodes | `bedrock.strip-hex-colors: true` setzen |
| `/helden3 status` zeigt `Floodgate: nein` | Floodgate fehlt oder ist aelter als die API — Fallback greift trotzdem |
| Kein Scoreboard | `hud.enabled: true`, danach `/helden3 reload` |
