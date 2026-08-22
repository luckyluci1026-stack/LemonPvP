# Helden 3 — LemonPvP

Ein Minecraft-Projekt nach dem Regelwerk von **Minecraft Helden**: jeder startet
mit drei Herzen plus einem geteilten Link-Herz. Herzen verlierst du
ausschliesslich an andere Spieler — Sturz, Lava, Mobs und Hunger kosten nichts.
Bei null Herzen bist du raus. Der Letzte im Rennen gewinnt.

**Java- und Bedrock-Spieler spielen zusammen.** Der Server laeuft als
Paper-Server, Bedrock-Spieler kommen ueber [Geyser](https://geysermc.org) dazu.

```
LemonPvP/
├── plugin/        Paper-Plugin (Java 21, Maven)
├── resourcepack/
│   ├── java/      Resourcepack fuer Java-Spieler (CustomModelData)
│   └── bedrock/   Resourcepack fuer Bedrock-Spieler (.mcpack)
├── geyser/        Geyser-Mapping, das beide Seiten verbindet
├── tools/         Texturen erzeugen, Packs bauen, Konfiguration pruefen
├── docs/          Einrichtung und Regelwerk
└── index.html     Die Projektwebseite
```

---

## Die Regeln

| | |
| --- | --- |
| **Start** | 3 eigene Herzen + 1 Link-Herz = 4 Herzen (8 Lebenspunkte) |
| **Herzverlust** | nur durch andere Spieler. Sturz, Lava, Mobs, Hunger: folgenlos |
| **Link-Herz** | wer auf dem letzten Herz steht, teilt es mit einem zufaellig zugelosten Mitspieler. Faellt er, verliert der Partner ebenfalls ein Herz |
| **Combat-Log** | wer im Kampf ausloggt, hinterlaesst einen **Dummy** mit seiner Ausruestung. Wird der getoetet, sind Herz und Loot weg — ueberlebt er, ist nichts passiert |
| **Ausscheiden** | bei 0 Herzen Zuschauermodus (einstellbar) |
| **Sieg** | der letzte Spieler mit Herzen |

Herzen sind gleichzeitig Leben **und** Maximalgesundheit: wer noch zwei Herzen
hat, laeuft mit vier Lebenspunkten herum. Jeder Kampf zaehlt.

---

## Schnellstart

```bash
# 1. Plugin bauen  ->  plugin/target/Helden3-3.0.0.jar
cd plugin && mvn clean package && cd ..

# 2. Texturen und Packs erzeugen (nur noetig, wenn du items.yml aenderst)
python3 tools/generate_textures.py
python3 tools/build_packs.py          # braucht PyYAML

# 3. Packs zippen  ->  dist/Helden3-Java.zip und dist/Helden3-Bedrock.mcpack
./tools/package_packs.sh
```

| Datei | Ziel |
| --- | --- |
| `plugin/target/Helden3-3.0.0.jar` | `plugins/` des Paper-Servers |
| `dist/Helden3-Java.zip` | Webspace, dann in `server.properties` verlinken |
| `dist/Helden3-Bedrock.mcpack` | `plugins/Geyser-Spigot/packs/` |
| `geyser/custom_mappings/helden3.json` | `plugins/Geyser-Spigot/custom_mappings/` |

Ausfuehrlich in **[docs/SETUP.md](docs/SETUP.md)**, das komplette Regelwerk in
**[docs/HELDEN.md](docs/HELDEN.md)**.

---

## Befehle

| Befehl | Wirkung |
| --- | --- |
| `/herzen [spieler]` | Verbleibende Herzen (und ob gerade ein eigener Dummy steht) |
| `/teilnehmer` | Uebersicht: wer hat wie viele Herzen, wer haengt an wem, wer ist raus |
| `/stats [spieler]` | Herzen, Link-Partner, Kills, Tode |
| `/helden3 …` | Adminwerkzeuge |

Admin (`helden3.admin`):

```
/helden3 reload                              Konfiguration neu laden
/helden3 status                              Regelwerk und Zaehlerstand
/helden3 herzen <spieler> <set|add|remove> <n>
/helden3 link <spieler> <partner|clear>      Link-Herz von Hand setzen
/helden3 raus <spieler>                      Spieler ausscheiden lassen
/helden3 zurueck <spieler>                   Spieler zurueckholen
/helden3 dummy clear                         Alle Dummies entfernen
/helden3 item <id> [spieler] [anzahl]        Artefakt geben
/helden3 reset bestaetigen                   Alle Herzen zuruecksetzen (neue Season)
/helden3 spawn                               Projektspawn setzen
```

---

## Konfiguration

| Datei | Inhalt |
| --- | --- |
| `config.yml` | Herzen, Link-Herz, Dummy, Elimination, Kampf, Scoreboard, Bedrock |
| `items.yml` | Artefakte: Material, CustomModelData, Bedrock-Identifier, Lore |
| `messages.yml` | Saemtliche Texte |

`items.yml` ist zugleich die Quelle fuer beide Resourcepacks: `tools/build_packs.py`
liest sie und schreibt Java-Modelle, Bedrock-Texturenliste und Geyser-Mapping —
Plugin und Packs koennen also nicht auseinanderlaufen. `tools/validate.py` prueft
das Ganze gegen und taugt fuer die CI.

---

## Kompatibilitaet

- **Server:** Paper 1.21.x (laeuft auch auf Spigot — es werden nur stabile
  Bukkit-APIs benutzt), Java 21
- **Bedrock:** Geyser + Floodgate. Floodgate ist optional; ohne es erkennt das
  Plugin Bedrock-Spieler ueber die Floodgate-UUID-Form bzw. das Namensprefix.
- **Ohne Geyser** laeuft alles ganz normal als reines Java-Projekt.

Das Plugin bindet Geyser/Floodgate **nicht** als Dependency ein — die Erkennung
laeuft ueber Reflection, damit der Server auch ohne die Plugins sauber startet.
Auch die Maximalgesundheit wird versionsunabhaengig gesetzt: das zustaendige
Attribut hiess je nach Minecraft-Version anders, deshalb sucht
`heart/HealthCompat` es zur Laufzeit.
