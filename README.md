# Helden 3 — das LemonPvP YouTuber-Projekt

Ein Minecraft-Projekt im Stil einer YouTuber-Season: Heldenklassen mit eigenen
Faehigkeiten, Fraktionen, ein Lebenssystem mit Wiederbelebung, Server-Events und
eigene Artefakte mit eigener Optik.

**Java- und Bedrock-Spieler spielen zusammen.** Der Server laeuft als
Paper-Server, Bedrock-Spieler kommen ueber [Geyser](https://geysermc.org) dazu.
Das Repository enthaelt deshalb beides: das Plugin *und* zwei aufeinander
abgestimmte Texturepacks (Java + Bedrock) samt Geyser-Mapping.

```
LemonPvP/
├── plugin/        Paper-Plugin (Java 21, Maven)
├── resourcepack/
│   ├── java/      Resourcepack fuer Java-Spieler (CustomModelData)
│   └── bedrock/   Resourcepack fuer Bedrock-Spieler (.mcpack)
├── geyser/        Geyser-Mapping, das beide Seiten verbindet
├── tools/         Texturen erzeugen, Packs bauen, Packs zippen
├── docs/          Einrichtung und Spielinhalte
└── index.html     Die Projektwebseite
```

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

Danach:

| Datei | Ziel |
| --- | --- |
| `plugin/target/Helden3-3.0.0.jar` | `plugins/` des Paper-Servers |
| `dist/Helden3-Java.zip` | Webspace, dann in `server.properties` verlinken |
| `dist/Helden3-Bedrock.mcpack` | `plugins/Geyser-Spigot/packs/` |
| `geyser/custom_mappings/helden3.json` | `plugins/Geyser-Spigot/custom_mappings/` |

Die ausfuehrliche Anleitung inklusive Geyser- und Floodgate-Einstellungen steht
in **[docs/SETUP.md](docs/SETUP.md)**.

---

## Was drin ist

### Sechs Helden

| Held | Rolle | Faehigkeit | Waffe |
| --- | --- | --- | --- |
| Krieger | Frontlinie, viel Leben | Wirbelsturm (20 s) | Zitronenklinge |
| Bogenschuetze | Distanz | Pfeilhagel (25 s) | Sturmbogen |
| Assassine | Burst, wenig Ruestung | Schattenschritt (18 s) | Schattendolch |
| Magier | Feuer auf Distanz | Feuerball (12 s) | Feuerstab |
| Heiler | Support | Heilkreis (30 s) | Heilerstab |
| Waechter | Tank | Schildwall (30 s) | Bollwerk-Schild |

Faehigkeit ausloesen: **Schleichen + Rechtsklick mit der Heldenwaffe** oder
`/faehigkeit`. Das Schleichen sorgt dafuer, dass Bogen und Schild ganz normal
benutzbar bleiben — umstellbar ueber `hero.ability-trigger`. Auf Bedrock ist der
Rechtsklick das lange Antippen bzw. die Nutzen-Taste, die Steuerung ist also
auf beiden Editionen dieselbe.

### Projektmechaniken

- **Leben** — jeder startet mit 3. Jeder Tod kostet eins, bei 0 ist man
  *gefallen* und wird Zuschauer.
- **Wiederbelebung** — ein Mitspieler bringt dich mit einem **Heldenherz**
  (`/wiederbeleben <spieler>`) zurueck.
- **Fraktionen** — Zitronen, Limetten, Blutorangen. Kein Friendly Fire,
  Teamchat ueber `/team chat`, Namensschilder in Teamfarbe.
- **Combat-Log** — wer im Kampf ausloggt, verliert ein Leben und laesst sein
  Inventar zurueck.
- **Events** — Blutmond (mehr Schaden), Zitronenregen (Loot am Spawn),
  Kopfgeld (Belohnung auf einen Spieler). Laufen automatisch in Rotation.
- **Zitronen** — die Waehrung. Gibt es fuer Kills, Assists und Kopfgelder,
  ausgeben im `/shop`.
- **Scoreboard** — Held, Team, Leben, Kills, Serie, Zitronen und das laufende
  Event, flackerfrei auch auf Bedrock.

Alle Zahlen, Namen und Texte stehen in den YAML-Dateien und lassen sich ohne
Neustart mit `/helden3 reload` nachziehen.

### Zehn Artefakte

Zitronenklinge, Sturmbogen, Schattendolch, Feuerstab, Heilerstab,
Bollwerk-Schild, Heldenherz, Lebenskristall, Zitrone, Rueckkehrstein.

Jedes hat eine eigene 16×16-Textur, die in **beiden** Packs steckt — Java ueber
`CustomModelData`, Bedrock ueber das Geyser-Mapping. Details und die Liste der
Spielinhalte: **[docs/HELDEN.md](docs/HELDEN.md)**.

---

## Befehle

| Befehl | Wirkung |
| --- | --- |
| `/held [name]` | Heldenauswahl oeffnen oder direkt wechseln |
| `/faehigkeit` | Faehigkeit ausloesen (Alternative zum Rechtsklick) |
| `/team [info\|liste\|chat <text>]` | Teaminfo, Mitgliederliste, Teamchat |
| `/leben [spieler]` | Verbleibende Leben |
| `/wiederbeleben <spieler>` | Gefallenen mit einem Heldenherz zurueckholen |
| `/stats [spieler]` | Statistik |
| `/shop` | Zitronen-Shop |
| `/helden3 …` | Adminwerkzeuge (siehe unten) |

Admin (`helden3.admin`):

```
/helden3 reload                        Konfiguration neu laden
/helden3 status                        Registrierte Inhalte anzeigen
/helden3 item <id> [spieler] [anzahl]  Artefakt geben
/helden3 held <spieler> <held>         Held setzen
/helden3 team <spieler> <team>         Team setzen
/helden3 leben <spieler> <anzahl>      Leben setzen
/helden3 coins <spieler> <anzahl>      Kontostand setzen
/helden3 event <id|stop>               Event starten oder beenden
/helden3 spawn                         Projektspawn auf deine Position setzen
```

---

## Konfiguration

| Datei | Inhalt |
| --- | --- |
| `config.yml` | Leben, Kampf, Wirtschaft, Scoreboard, Events, Bedrock-Optionen |
| `heroes.yml` | Heldenklassen: Kit, Passivwerte, Faehigkeit, Waffe |
| `items.yml` | Artefakte: Material, CustomModelData, Bedrock-Identifier, Lore |
| `teams.yml` | Fraktionen, Farben, Teamspawns |
| `shop.yml` | Angebote und Preise im `/shop` |
| `messages.yml` | Saemtliche Texte |

`items.yml` ist zugleich die Quelle fuer beide Resourcepacks: `tools/build_packs.py`
liest sie und schreibt Java-Modelle, Bedrock-Texturenliste und Geyser-Mapping.
Deshalb koennen Plugin und Packs nicht auseinanderlaufen.

---

## Kompatibilitaet

- **Server:** Paper 1.21.x (laeuft auch auf Spigot — es werden nur stabile
  Bukkit-APIs benutzt), Java 21
- **Bedrock:** Geyser + Floodgate. Floodgate ist optional; ohne es erkennt das
  Plugin Bedrock-Spieler ueber die Floodgate-UUID-Form bzw. das Namensprefix.
- **Ohne Geyser** laeuft alles ganz normal als reines Java-Projekt.

Das Plugin bindet Geyser/Floodgate **nicht** als Dependency ein — die Erkennung
laeuft ueber Reflection, damit der Server auch ohne die Plugins sauber startet.
