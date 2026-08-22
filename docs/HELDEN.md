# Regelwerk

Alle Werte kommen aus `plugin/src/main/resources/config.yml` und lassen sich
dort aendern. Was im Original **Minecraft Helden** so vorkommt und was eine
Zugabe dieses Plugins ist, steht jeweils dabei.

---

## Herzen

- **Start: 3 eigene Herzen + 1 Link-Herz = 4.** Herzen sind gleichzeitig Leben
  und Maximalgesundheit — vier Herzen sind acht Lebenspunkte.
- **Herzen gehen nur an andere Spieler verloren.** Sturz, Lava, Mobs, Hunger,
  Ertrinken: du stirbst normal, respawnst, behaeltst aber alle Herzen. Das ist
  die Kernregel des Projekts (`hearts.pvp-only`).
- **Bei 0 Herzen bist du raus** — standardmaessig Zuschauermodus
  (`game.on-elimination`: `SPECTATOR`, `KICK` oder `NOTHING`).
- **Der Letzte mit Herzen gewinnt** und wird im Chat verkuendet.

Ein Kill zaehlt auch dann dem Angreifer, wenn das Opfer danach noch in die Lava
faellt — bis zu `hearts.pvp-credit-seconds` (Standard 15 s) nach dem letzten
Treffer.

| Einstellung | Standard | Bedeutung |
| --- | --- | --- |
| `hearts.start` | 3 | eigene Herzen |
| `hearts.link-heart` | true | zusaetzliches, geteiltes Herz |
| `hearts.max` | 6 | Obergrenze, falls Herzen wieder vergeben werden |
| `hearts.pvp-only` | true | nur Spieler nehmen Herzen ab |
| `hearts.pvp-credit-seconds` | 15 | Gutschriftfenster nach einem Treffer |

---

## Das Link-Herz

Sobald jemand auf sein **letztes** Herz faellt, wird ihm ein zufaelliger
Mitspieler zugelost. Ab da haengt das Herz an beiden:

```
Start:        ♥ ♥ ♥ + ♥link
nach 1 Kill:  ♥ ♥ + ♥link
nach 2 Kills: ♥ + ♥link
nach 3 Kills: ♥link          <- Partner wird zugelost und bekannt gegeben
nach 4 Kills: raus           + Partner verliert ebenfalls ein Herz
```

Die Bindung ist **einseitig**: sie kostet den Partner nur, wenn der Besitzer
faellt — nicht umgekehrt. Verliert der Partner dadurch sein eigenes letztes
Herz, greift dessen Link-Herz weiter; eine Kette kann also mehrere Spieler
mitreissen. Gegen Endlosschleifen (A haengt an B, B an A) ist die Kette
abgesichert, jeder Spieler verliert pro Kette hoechstens ein Herz.

Bei der Auslosung haben Online-Spieler Vorrang, und standardmaessig kann
niemand gleichzeitig Partner mehrerer Link-Herzen sein (`link.allow-multiple`).
Scheidet ein Partner aus, wird neu ausgelost (`link.reassign-on-partner-out`).

| Einstellung | Standard | Bedeutung |
| --- | --- | --- |
| `link.announce` | true | Partner oeffentlich bekanntgeben |
| `link.reassign-on-partner-out` | true | neu auslosen, wenn der Partner raus ist |
| `link.allow-multiple` | false | jemand kann Partner mehrerer Link-Herzen sein |

---

## Der Combat-Log-Dummy

Wer sich im Kampf ausloggt, verliert **nicht** sofort ein Herz. Stattdessen
bleibt eine Puppe mit seiner kompletten Ausruestung stehen — sichtbar
ausgeruestet, mit Namensschild:

- **Dummy wird getoetet** → der Besitzer verliert ein Herz, sein Inventar faellt
  am Ort des Dummys zu Boden. Der Killer bekommt den Kill gutgeschrieben.
- **Dummy ueberlebt die Laufzeit** (Standard 3 Minuten) → er verschwindet, dem
  Spieler passiert nichts.
- **Spieler kommt rechtzeitig zurueck** → die Puppe wird eingesammelt, er
  bekommt sein Inventar zurueck.

Damit Mobs die Puppe nicht abraeumen, duerfen ihr standardmaessig nur Spieler
schaden (`dummy.players-only`), sie hat keine KI, brennt nicht in der Sonne und
wird von Mobs nicht anvisiert. Puppen, die einen Serverabsturz ueberlebt haben,
werden beim Start erkannt und entfernt.

| Einstellung | Standard | Bedeutung |
| --- | --- | --- |
| `dummy.enabled` | true | Combat-Log-Puppen an |
| `dummy.lifetime-seconds` | 180 | wie lange die Puppe steht |
| `dummy.players-only` | true | nur Spieler duerfen ihr schaden |
| `dummy.name-format` | `&c%player% &7(Dummy)` | Namensschild |

---

## Kampf

- **Combat-Tag**: 15 s nach dem letzten Treffer gilt man als im Kampf — genau
  das Fenster, in dem ein Logout die Puppe ausloest.
- **Spawnschutz**: 5 s nach dem Respawn. Wer selbst zuschlaegt, verliert ihn
  sofort. *Zugabe dieses Plugins, im Original nicht bestaetigt — mit
  `combat.respawn-protection-seconds: 0` abschaltbar.*
- Ausgeschiedene Spieler nehmen keinen Schaden und teilen keinen aus.

---

## Artefakte

| Artefakt | Basisitem | CMD | Funktion |
| --- | --- | --- | --- |
| Herz | `RED_DYE` | 3001 | Rechtsklick: +1 ♥ bis zum Maximum |

Das Herz-Item ist **nicht** Teil des Originalregelwerks — es ist praktisch fuer
Comebacks und als Adminwerkzeug. Mit `special-items.heart: ""` schaltest du es
komplett ab. Eigene Artefakte legst du wie in
[SETUP.md](SETUP.md#4-eigene-artefakte-hinzufuegen) beschrieben an.

---

## Rechte

| Recht | Standard | Bedeutung |
| --- | --- | --- |
| `helden3.play` | alle | Alle Spielerbefehle |
| `helden3.command.hearts` / `.list` / `.stats` | alle | Einzelne Befehle |
| `helden3.admin` | OP | Alle Adminbefehle |

---

## Bedrock-Besonderheiten

Was das Plugin fuer Bedrock-Spieler anders macht:

- **Die Teilnehmerliste ist ein Kisten-GUI.** Geyser uebersetzt das automatisch,
  damit funktioniert dieselbe Oberflaeche auf beiden Editionen — ohne
  Floodgate-Forms und ohne zusaetzliche Dependency.
- **Hex-Farben** (`&#RRGGBB`) kann Bedrock nicht darstellen. Sie werden pro
  Empfaenger auf die naechstliegende der 16 klassischen Farben reduziert
  (`bedrock.strip-hex-colors`).
- **Scoreboard-Zeilen** werden ueber Scoreboard-Teams gesetzt statt neu
  registriert. Sonst flackert die Sidebar — auf Bedrock besonders sichtbar.
- **Herzanzeige**: weniger Maximalgesundheit heisst auf beiden Editionen einfach
  weniger Herzen in der Leiste, dafuer ist keine Sonderbehandlung noetig.
