# Speer und Attribut-Swap: Analyse, Ursache, Fix (Minecraft 1.21.11)

Ausgangspunkt: „Fehler beim Attribut-Swappen mit einem Speer“, vermutete Ursache: Paper.
Genaue Schritte zum Nachstellen lagen nicht vor. Untersucht wurde deshalb der gesamte Weg
eines Speer-Angriffs und eines Attribut-Swaps im **gepatchten** Server-Quellcode aller vier
Versionen (Auszüge per CI-Workflow `source-inspect`, siehe unten). Zeilennummern beziehen
sich auf `versions/1.21.11/paperswap-server/src/minecraft/java` nach `applyAllPatches` (ohne den
PaperSwap-Fix).

## Ergebnis in Kürze

| | Was | Wo | Versionen | Art |
|---|---|---|---|---|
| **U1** | Die Angriffs-Abklingzeit des Spielers wird **während der Schadensverarbeitung** zurückgesetzt statt danach (CraftBukkit-Änderung, von Paper übernommen) | `LivingEntity.actuallyHurt` Z. 2454–2467, `Player.onAttack` Z. 1977–1980 | **nur 1.21.11** (Paper `c5eb079`) | Paper-Fehler, upstream behoben in Paper `b80b604` (#13856, 03.05.2026); in 26.1.2 `e4e17fc`, 26.2 `58e6e45`, 26.3 `6829d9e` enthalten (per `git merge-base --is-ancestor` geprüft) |
| **U2** | Paper aktualisiert Attribute und setzt die Abklingzeit bei einem Slotwechsel **sofort** zurück | `ServerGamePacketListenerImpl.handleSetCarriedItem` Z. 2303 → `Player.detectEquipmentUpdates` (Paper) Z. 1984–2000 | alle | **kein Fehler**, Paper-Einstellung `unsupported-settings.update-equipment-on-player-actions` (Standard `true`) |

U1 ist im Code nachgewiesen und mit echten Angriffen im laufenden Server gemessen
(CombatProbe, siehe „Tests“). Es betrifft den Speer direkt und damit auch Attribut-Swaps
auf einen Speer. Ob genau das der gemeldete Fehler ist, lässt sich ohne die genauen
Schritte des Meldenden nicht sagen; U2 ist die zweite Stelle, an der sich Paper beim
Attribut-Swap anders verhält als Vanilla.

Kein PaperSwap-Code ist beteiligt (PaperSwap berührt Kampf-Code nur durch diesen Fix), und kein
Plugin: gemessen wurde auf einem Server ohne andere Plugins als dem Mess-Plugin.

## Ablauf eines Speer-Stichs (1.21.11)

1. Client sendet `ServerboundPlayerActionPacket` mit Aktion `STAB` →
   `ServerGamePacketListenerImpl.handlePlayerAction`, `case STAB` (Z. 1916–1931):
   `cannotAttackWithItem(item, 5)` prüft die Mindest-Aufladung, dann
   `PiercingWeapon.attack(player, MAINHAND)`.
2. `PiercingWeapon.attack` (`world/item/component/PiercingWeapon.java` Z. 73–93):
   `f = getAttributeValue(ATTACK_DAMAGE)`, dann **für jedes getroffene Ziel**
   `stabAttack(slot, ziel, f, …)`, **danach** `entity.onAttack()`.
3. `Player.stabAttack` (Z. 1289–1344): Verzauberungsbonus × `getAttackStrengthScale(0.5)`,
   Grundschaden × `baseDamageScaleFactor()` = `0.2 + s² · 0.8` (Z. 1283–1286), dann
   `target.hurtOrSimulate(...)` → `LivingEntity.hurtServer` → `actuallyHurt`.
4. **Vanilla:** `Player.onAttack()` setzt nach der Schleife `attackStrengthTicker = 0`.
   **Paper 1.21.11:** `onAttack()` setzt nichts zurück (auskommentiert, Z. 1978); stattdessen
   setzt `actuallyHurt` (Z. 2454–2467) die Abklingzeit zurück, **sobald ein Spieler einem
   Ziel Schaden zufügt** (Ausnahme: Spieler lädt gerade einen Speer-Ansturm,
   `KINETIC_WEAPON`).

Folgen von U1 in 1.21.11 (die ersten beiden und der Attribut-Swap sind mit CombatProbe
gemessen, der dritte ist aus dem Code abgeleitet):

* **Mehrere Ziele in einem Stich:** Nach dem ersten Ziel ist die Abklingzeit 0; jedes weitere
  Ziel wird mit Angriffsstärke ≈ 0 berechnet (≈ 20 % Grundschaden, kaum
  Verzauberungsbonus). Vanilla: alle Ziele gleich.
* **Stich ohne Treffer** (oder Treffer ohne Schaden, z. B. abgebrochenes Schadensereignis):
  Vanilla setzt die Abklingzeit zurück, Paper 1.21.11 nicht.
* **Projektile eines Spielers** (Pfeil, Dreizack …), die Schaden anrichten, setzen in
  Paper 1.21.11 die Nahkampf-Abklingzeit zurück (Quelle des Schadens ist der Spieler);
  Vanilla nicht.
* **Attribut-Swap auf einen Speer** (Schwert gewählt, im selben Tick auf den Speer wechseln
  und stechen): der Stich nutzt – wie in Vanilla – noch den Angriffsschaden des Schwerts;
  das zweite Ziel bekommt in 1.21.11 trotzdem nur ≈ 20 %.

## Attribut-Swap und Paper (U2)

Vanilla verarbeitet Pakete vor dem Spieler-Tick. Ein Slotwechsel und ein Angriff im selben
Tick nutzen daher die Attribute des **vorherigen** Gegenstands und dessen
Abklingzeit; erst `Player.tick` (Z. 290–299) bemerkt den Wechsel und setzt die Abklingzeit
bei anderem Gegenstandstyp zurück. Paper ruft dagegen standardmäßig nach dem Slotwechsel
sofort `detectEquipmentUpdates()` auf (`handleSetCarriedItem` Z. 2303), was Attribute
übernimmt **und** die Abklingzeit zurücksetzt. Damit ist Attribut-Swapping auf Paper
standardmäßig ausgeschaltet – für alle Waffen und alle Versionen. Das ist eine bewusste
Paper-Einstellung, kein Fehler, und PaperSwap ändert sie nicht. Wer Vanilla-Verhalten will:

```yaml
# config/paper-global.yml
unsupported-settings:
  update-equipment-on-player-actions: false
```

(Paper führt die Option unter „unsupported settings“; Neustart nötig.)

## Fix (nur 1.21.11)

Rückportierung von Paper `b80b604` als PaperSwap-Quellpatch
(`versions/1.21.11/paperswap-server/minecraft-patches/sources/…/LivingEntity.java.patch` und
`…/player/Player.java.patch`):

* `LivingEntity.actuallyHurt`: kein Zurücksetzen der Abklingzeit mehr.
* `Player.attack`: setzt die Abklingzeit an der Vanilla-Stelle zurück (vor dem Schaden) und
  löst dabei wie upstream `PlayerAttackEntityCooldownResetEvent` aus.
* `Player.onAttack`: setzt die Abklingzeit zurück (wird von `PiercingWeapon.attack` nach allen
  Zielen aufgerufen).

Es werden nur **Methodenrümpfe** geändert (keine neue Methode, anders als upstream
`onAttack(Entity)`), damit der Fix auch als Live-Patch auf einer Standard-JVM anwendbar ist.
Verhalten danach wie Paper 26.x: `PlayerAttackEntityCooldownResetEvent` kommt einmal je
normalem Angriff, nicht bei Speer-Stichen und nicht bei Projektilen (das Event ist in Paper
26.x als veraltet markiert). 26.1.2, 26.2 und 26.3 brauchen keinen PaperSwap-Patch.

## Tests

**CombatProbe** (`runtime/combat-probe`, nur Test/CI): ein serverseitiger Testspieler
(`ServerPlayer` ohne Client, Pakete werden verworfen) führt echte Angriffe auf Schweine mit
1000 Lebenspunkten aus und ruft dieselben Methoden auf wie die Paket-Handler:

| Szenario | Paper 1.21.11 (ohne Fix), erwartet | gemessen mit Fix (Lauf #20, ausgelieferte JAR, Servertest 7) |
|---|---|---|
| Eisenspeer-Stich durch zwei Ziele | zweites Ziel ≈ 20 % | 3,000 / 3,000 (Verhältnis 1,00), Abklingzeit danach 0 |
| Speer-Stich ohne Ziel | Abklingzeit bleibt | 100 → 0 (zurückgesetzt) |
| Eisenschwert-Schlag | Schaden, Abklingzeit 0, 1 Event | 6,000, 100 → 0, 1 Event |
| Swap Eisenschwert → Eisenspeer ohne Ausrüstungs-Update | zweites Ziel ≈ 20 % | 6,000 / 6,000 (Schwert-Angriffsschaden, wie Vanilla) |
| Swap mit Papers Ausrüstungs-Update | beide schwach (U2) | 0,602 / 0,602 (U2, unverändert) |

Die Messung **ohne** Fix und der Live-Patch stehen im CI-Job `spear-hotswap` (siehe
[`TESTING.md`](TESTING.md)).

**CI-Job `spear-hotswap`** (1.21.11): baut den Server **ohne** Fix, misst (Fehler
reproduziert), baut den Fix mit `createLivePatch` aus den korrigierten Quellen gegen die
laufende Server-JAR, prüft ihn offline (`checkLivePatch`), wendet ihn im **laufenden**
Server an (`Player` + `LivingEntity`, JVM-Redefinition), liest den laufenden Bytecode
zurück, misst erneut, prüft `/tps` und Server-List-Ping, rollt zurück und misst ein drittes
Mal. **Fork-Job, Servertest 7:** die ausgelieferte 1.21.11-JAR (mit Fix) misst
Vanilla-Verhalten. Ergebnisse: [`TESTING.md`](TESTING.md).

## Auf dem eigenen Testserver prüfen

**Dauerhaft:** Server-JAR aus diesem Stand bauen (bzw. CI-Artefakt `paperswap-1.21.11`),
deployen, neu starten.

**Live, ohne Neustart** (Server läuft mit einer älteren PaperSwap-1.21.11-JAR und
`-javaagent:paperswap-agent.jar`):

1. Den Quellstand bauen, aus dem die laufende JAR stammt, und
   `./gradlew :paperswap-server:livePatchBaseline` ausführen; laufende JAR
   (`versions/1.21.11/paperswap-1.21.11.jar` im Serververzeichnis) holen.
2. Die beiden Patch-Dateien des Fixes nach
   `versions/1.21.11/paperswap-server/minecraft-patches/sources/net/minecraft/world/entity/`
   kopieren, `./gradlew applyAllPatches`.
3. `./gradlew :paperswap-server:createLivePatch -PpatchId=spear-cooldown -PserverJar=<geholte JAR>`
   → erwartet: `changed net.minecraft.world.entity.LivingEntity`,
   `changed net.minecraft.world.entity.player.Player`, nur „method body changed“.
4. `./gradlew :paperswap-server:checkLivePatch -PpatchId=spear-cooldown -PserverJar=<geholte JAR>`
   → `APPLICABLE on a standard JVM`.
5. Hochladen nach `paperswap/patches/inbox/` (+ Freigabe in `approved.sha256`), dann an der
   Konsole `fork live check spear-cooldown`, `fork live update spear-cooldown`,
   `… confirm` → `applied and verified`.
6. Im Spiel: zwei Mobs hintereinander (Abstand ≈ 1 Block) in Speer-Reichweite, voll
   aufgeladen zustechen: beide erhalten gleichen Schaden. Für Attribut-Swap zusätzlich
   `update-equipment-on-player-actions: false` (siehe oben).
7. Zurück: `fork live rollback spear-cooldown` → `… confirm`.

Ein im CI gebauter Patch passt **nicht** auf eine eigene JAR (andere Build-ID); der Server
lehnt ihn mit „wrong build id“ ab.
