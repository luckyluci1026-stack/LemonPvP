# Spielinhalte

Referenz zu allem, was in Helden 3 steckt. Alle Werte kommen aus den
YAML-Dateien in `plugin/src/main/resources/` und lassen sich dort aendern.

---

## Die Helden

Werte aus `heroes.yml`. "Schaden aus/ein" sind Multiplikatoren auf verursachten
bzw. erlittenen Schaden.

| Held | Passive Effekte | Schaden aus | Schaden ein | Faehigkeit | Abklingzeit |
| --- | --- | --- | --- | --- | --- |
| **Krieger** | Health Boost I, Resistance I | ×1,10 | ×1,00 | Wirbelsturm | 20 s |
| **Bogenschuetze** | Speed I | ×1,00 (+25 % mit Pfeilen) | ×1,10 | Pfeilhagel | 25 s |
| **Assassine** | Speed II, Jump Boost I | ×1,25 | ×1,20 | Schattenschritt | 18 s |
| **Magier** | Fire Resistance I | ×1,00 | ×1,15 | Feuerball | 12 s |
| **Heiler** | Regeneration I | ×0,85 | ×1,00 | Heilkreis | 30 s |
| **Waechter** | Health Boost III, Resistance II, Slowness I | ×0,80 | ×0,75 | Schildwall | 30 s |

Passive Effekte werden alle `hero.passive-refresh-seconds` Sekunden erneuert
(Standard 5 s, Wirkdauer 8 s — damit nichts flackert).

**Startausruestung gibt es nicht.** Ein Held bringt nur seine Passivwerte und
seine Faehigkeit mit — das Inventar bleibt bei Auswahl, Respawn und
Wiederbelebung unangetastet. Die in der Tabelle genannte Waffe ist der
Gegenstand, mit dem sich die Faehigkeit ausloesen laesst; besorgen muss man sie
sich selbst (Beute, Shop oder `/helden3 item <id>`). Ohne Waffe bleibt
`/faehigkeit` — das funktioniert immer.

### Faehigkeiten im Detail

| Faehigkeit | Wirkung |
| --- | --- |
| **Wirbelsturm** | 7 Schaden auf alle Gegner im Umkreis von 4 Bloecken, dazu Rueckstoss |
| **Pfeilhagel** | 9 kritische Pfeile als Salve, je 80 % Pfeilschaden |
| **Schattenschritt** | Sprung nach vorn, danach 4 s unsichtbar und Speed II |
| **Feuerball** | Kleiner Feuerball — zuendet an, beschaedigt aber keine Bloecke |
| **Heilkreis** | Sofortheilung plus 5 s Regeneration fuer alle Teamkollegen im Umkreis von 6 Bloecken |
| **Schildwall** | 8 s Resistance III und Absorption II, stoesst alle Gegner im Umkreis von 5 Bloecken weg |

Ausgeloest wird per **Schleichen + Rechtsklick mit der Heldenwaffe** oder ueber
`/faehigkeit`. Auf Bedrock ist der Rechtsklick das lange Antippen bzw. die
Nutzen-Taste.

Warum Schleichen? Bogen und Schild haben auf dem Rechtsklick schon eine
Funktion — ohne das Schleichen wuerde der Bogenschuetze bei jedem Schuss seinen
Pfeilhagel zuenden. Loest die Faehigkeit aus, wird der normale Rechtsklick
unterdrueckt, sonst bleibt er wie gewohnt. Mit `hero.ability-trigger: RIGHT_CLICK`
laesst sich das auf den einfachen Rechtsklick umstellen.

Gefallene Spieler koennen keine Faehigkeiten einsetzen.

Geheilt wird bewusst ueber den Effekt `INSTANT_HEALTH` statt ueber
`setHealth` — so kuemmert sich der Server um das Lebensmaximum, und das Plugin
muss die zwischen den Versionen wandernde Attribut-API nicht anfassen.

---

## Die Artefakte

Aus `items.yml`. Die `CustomModelData` steuert das Java-Pack, der
Bedrock-Identifier das Geyser-Mapping.

| Artefakt | Basisitem | CMD | Modell | Funktion |
| --- | --- | --- | --- | --- |
| Zitronenklinge | `DIAMOND_SWORD` | 3001 | handheld | Waffe Krieger, Schaerfe III |
| Sturmbogen | `BOW` | 3002 | generated | Waffe Bogenschuetze, Power II |
| Schattendolch | `IRON_SWORD` | 3003 | handheld | Waffe Assassine, Schaerfe II |
| Feuerstab | `BLAZE_ROD` | 3004 | handheld | Waffe Magier |
| Heilerstab | `STICK` | 3005 | handheld | Waffe Heiler |
| Bollwerk-Schild | `SHIELD` | 3006 | none | Waffe Waechter, behaelt die Vanilla-Optik |
| Heldenherz | `NETHER_STAR` | 3007 | generated | Wiederbelebung |
| Lebenskristall | `AMETHYST_SHARD` | 3008 | generated | Rechtsklick: +1 Leben |
| Zitrone | `GOLD_NUGGET` | 3009 | generated | Rechtsklick: +50 Zitronen |
| Rueckkehrstein | `ECHO_SHARD` | 3010 | generated | Rechtsklick: Teleport zum Spawn, nicht im Kampf |

`model: none` heisst "kein Custom-Modell". Warum das Schild so behandelt wird,
steht in [SETUP.md](SETUP.md#model-none).

---

## Leben und Wiederbelebung

- Start: **3 Leben**, Maximum **6**.
- Jeder Tod kostet ein Leben. Bei **0** ist man *gefallen* und wird Zuschauer
  (einstellbar: `SPECTATOR`, `KICK`, `NOTHING`).
- Ein Mitspieler holt dich mit `/wiederbeleben <spieler>` zurueck. Das kostet
  ihn **ein Heldenherz** und **250 Zitronen**.
- Nach der Rueckkehr: 1 Leben und 15 s Schutz.
- Lebenskristalle geben ein Leben zurueck, bis zum Maximum.

Gefallene Spieler nehmen weder Schaden noch koennen sie welchen austeilen und
tauchen nicht als Kopfgeldziel auf.

---

## Kampf

- **Friendly Fire** im Team ist aus (`combat.team-friendly-fire`).
- **Combat-Tag**: 15 s nach dem letzten Treffer. Wer in dieser Zeit ausloggt,
  verliert ein Leben und laesst sein Inventar am Ort zurueck — der letzte
  Angreifer bekommt den Kill gutgeschrieben.
- **Spawnschutz**: 10 s nach dem Respawn. Wer selbst zuschlaegt, verliert ihn
  sofort.
- **Assists**: Wer dem Opfer in den letzten 20 s Schaden gemacht hat, bekommt
  15 Zitronen.
- **Killstreaks** werden ab 3 Kills angekuendigt; wer eine Serie beendet, wird
  ebenfalls genannt.

---

## Events

Laufen automatisch alle **45 Minuten** in Rotation, mit 30 s Vorwarnung. Per
`/helden3 event <id>` auch von Hand.

| Event | Dauer | Wirkung |
| --- | --- | --- |
| **Blutmond** | 10 min | +20 % PvP-Schaden fuer alle, es wird Nacht |
| **Zitronenregen** | 3 min | Alle 10 s fallen 3 Zitronen im Umkreis von 12 Bloecken um den Spawn |
| **Kopfgeld** | 15 min | Auf einen zufaelligen Spieler werden 1000 Zitronen ausgesetzt |

Ein Event darf ablehnen — Kopfgeld startet zum Beispiel nicht mit weniger als
zwei Spielern. Dann rueckt automatisch das naechste Event der Rotation nach.

---

## Zitronen

| Quelle | Betrag |
| --- | --- |
| Startguthaben | 100 |
| Kill | 50 + 10 je Kill der laufenden Serie |
| Assist | 15 |
| Kopfgeld | 1000 |
| Zitrone einloesen | 50 |

Ausgegeben wird im `/shop` (`shop.yml`): Heldenherz 1500, Lebenskristall 2500,
Rueckkehrstein 400, dazu Goldaepfel, Pfeile, Steak, Enderperlen und TNT.

---

## Teams

Drei Fraktionen aus `teams.yml`: **Zitronen** (gelb), **Limetten** (gruen),
**Blutorangen** (rot). Neue Spieler landen automatisch im kleinsten Team.

Namensschilder und Tablist werden in Teamfarbe eingefaerbt
(`hud.color-nametags`), der Teamchat laeuft ueber `/team chat <text>`.

---

## Rechte

| Recht | Standard | Bedeutung |
| --- | --- | --- |
| `helden3.play` | alle | Alle Spielerbefehle |
| `helden3.command.held` … `.shop` | alle | Einzelne Befehle |
| `helden3.hero.change.anytime` | OP | Held jederzeit wechseln |
| `helden3.admin` | OP | Alle Adminbefehle |

Ohne `helden3.hero.change.anytime` gilt `hero.change-cooldown-seconds`
(Standard 1 Stunde). `-1` erlaubt jederzeit zu wechseln, `0` sperrt den Wechsel
nach der ersten Wahl komplett — praktisch, wenn die Klassen fuer eine Season
feststehen sollen.

---

## Bedrock-Besonderheiten

Was das Plugin fuer Bedrock-Spieler anders macht:

- **Oberflaechen sind Kisten-GUIs.** Geyser uebersetzt die automatisch, damit
  funktioniert dieselbe Heldenauswahl auf beiden Editionen — ohne
  Floodgate-Forms und ohne zusaetzliche Dependency.
- **Hex-Farben** (`&#RRGGBB`) kann Bedrock nicht darstellen. Sie werden pro
  Empfaenger auf die naechstliegende der 16 klassischen Farben reduziert
  (`bedrock.strip-hex-colors`).
- **Scoreboard-Zeilen** werden ueber Scoreboard-Teams gesetzt statt neu
  registriert. Sonst flackert die Sidebar — auf Bedrock besonders sichtbar.
- **Steuerungshinweis** beim Join erklaert, dass langes Antippen dem Rechtsklick
  entspricht (`bedrock.send-controls-hint`).
- **Das Heldenmenue** oeffnet erst 1,5 s nach dem Join — sonst verschluckt der
  Client es.
