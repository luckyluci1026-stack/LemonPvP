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
Server einmal starten. Dabei entsteht `plugins/Helden3/` mit
`config.yml`, `heroes.yml`, `items.yml`, `teams.yml`, `shop.yml`,
`messages.yml` und dem Ordner `data/`.

> Der Build laedt die Paper-API von `repo.papermc.io`. Auf einem Rechner ohne
> Internetzugang schlaegt er fehl — das ist kein Fehler im Projekt.

Danach im Spiel:

```
/helden3 spawn      Projektspawn auf die eigene Position setzen
/helden3 status     pruefen, ob Helden, Artefakte und Teams geladen wurden
```

Teamspawns setzt du in `teams.yml` (Weltname plus Koordinaten). Bleibt ein
Teamspawn leer, respawnen alle am Projektspawn.

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
- das **Mapping** sagt Geyser, dass `diamond_sword` mit `CustomModelData 3001`
  auf Bedrock die Zitronenklinge ist.

Fehlt das Mapping, sehen Bedrock-Spieler ein normales Diamantschwert — spielbar,
aber ohne die Projektoptik.

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

1. Pixelbild in `tools/generate_textures.py` unter `ART` ergaenzen und in
   `ITEMS` mit einer Farbpalette eintragen.
2. Eintrag in `plugin/src/main/resources/items.yml` anlegen — mit **neuer**
   `custom-model-data` und `model: handheld` oder `model: generated`.
3. Erzeugen und packen:

```bash
python3 tools/generate_textures.py
python3 tools/build_packs.py      # pip install pyyaml
./tools/package_packs.sh
```

`build_packs.py` schreibt daraus Java-Modelle, `item_texture.json`, die
Bedrock-Sprachdateien und das Geyser-Mapping. Doppelte oder fehlende
`custom-model-data` bricht das Skript mit einer Meldung ab.

### `model: none`

Manche Vanilla-Items haben ein Sondermodell, das man nicht ersetzen darf. Das
Schild ist so ein Fall: es ist 3D, hat eine Blockanimation, und wuerden wir es
ueberschreiben, saehen **alle** Schilde auf dem Server kaputt aus — auf Bedrock
liesse sich damit ausserdem nicht mehr blocken. Deshalb steht beim
Bollwerk-Schild `model: none`: es behaelt die Vanilla-Optik und bekommt nur
Namen, Lore und die Schildwall-Faehigkeit.

Der Bogen ist der andere Sonderfall — dort baut `build_packs.py` die
Vanilla-Spannanimation nach, damit normale Boegen sie behalten. Der Sturmbogen
selbst zeigt immer seine eigene Textur.

---

## 5. Ohne Bedrock

Ohne Geyser und Floodgate laeuft alles ganz normal als Java-Projekt. Das Plugin
bindet beide **nicht** als Dependency ein, sondern spricht Floodgate ueber
Reflection an — fehlt es, ist einfach nie jemand "Bedrock".

---

## Problemsuche

| Symptom | Ursache |
| --- | --- |
| Java-Spieler sehen Vanilla-Items | Pack nicht geladen, oder `resource-pack-sha1` passt nicht zur Datei |
| Bedrock-Spieler sehen Vanilla-Items | Mapping fehlt in `custom_mappings/`, oder Geyser wurde nicht neu gestartet |
| Bedrock-Spieler sehen kaputte Farbcodes | `bedrock.strip-hex-colors: true` setzen |
| `/helden3 status` zeigt `Floodgate: nein` | Floodgate fehlt oder ist aelter als die API — Fallback greift trotzdem |
| Kein Scoreboard | `hud.enabled: true`, danach `/helden3 reload` |
| Faehigkeit reagiert nicht | Schleichen + Rechtsklick mit der Heldenwaffe — oder `/faehigkeit` nutzen |
| `Held 'x' verweist auf die unbekannte Faehigkeit 'y'` | `ability:` in `heroes.yml` gegen `/helden3 status` pruefen |
| Kit bleibt leer | `item:` in `heroes.yml` zeigt auf eine ID, die es in `items.yml` nicht gibt |
