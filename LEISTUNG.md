# Leistung: viele Spieler gleichzeitig, Java und Bedrock

Diese Seite erklärt, was die Plugins schon von selbst gegen Lag tun und welche
Server-Einstellungen du einmal setzen solltest. Alles ist für ungefähr 100 bis
150 Spieler auf dem SMP gedacht.

## Was schon automatisch passiert

| Wo | Was |
|---|---|
| Proxy (SMPProxy) | **Einlass-Warteschlange**: Wollen viele gleichzeitig auf den SMP (Start nach einem Neustart, Release, alle klicken in der Lobby auf SMP), kommen höchstens 4 pro Sekunde rein. Wer wartet, sieht seinen Platz in der Actionbar und wird automatisch verbunden. |
| Proxy (SMPProxy) | **Lastmeldung**: SMP und Lobby melden alle 2 Sekunden ihre Auslastung. Ab 35 ms pro Tick kommt nur noch die Hälfte rein, ab 45 ms pausiert der Einlass, bis es wieder ruhiger ist. Auch die Release-Wellen richten sich danach. |
| Proxy (SMPProxy) | **Geyser**: Direktverbindung ohne doppelte Kompression wird beim Start eingestellt – ein Bedrock-Spieler kostet dann kaum mehr als ein Java-Spieler. |
| SMP (BetterSMP) | **Dynamische Sichtweite**: Wird der Server langsam, sinkt die Sichtweite für alle kurz um je einen Chunk (nie unter 5) und steigt wieder, sobald es 30 Sekunden ruhig ist. |
| SMP (BetterSMP) | **Bedrock-Sichtweite 6**: weniger Chunks für Handys und Konsolen und weniger Arbeit für Geyser. Einstellbar unter `leistung.bedrock-sichtweite`. |
| SMP (BetterSMP) | **Nametags und Scoreboard** schicken nur echte Änderungen. Beim Beitreten kommt die fertige Tafel in einem Rutsch. |
| SMP (BetterRTP) | **RTP-Reihe**: höchstens 3 Suchen gleichzeitig, der Rest wartet kurz mit Platzanzeige. |
| SMP (BetterSMP, DuelPlus) | Datenbank-Schreibvorgänge und Backups sind über die Zeit verteilt und laufen nur bei Änderungen. |

Mit **`/bettersmp leistung`** siehst du auf dem SMP, welche der Einstellungen
unten noch fehlen. Beim Start steht dasselbe in der Konsole.
**`/smpproxy status`** zeigt für jeden Server die aktuelle Auslastung und wie
viele gerade in der Warteschlange stehen.

## Vor dem Release

1. **Welt vorgenerieren.** Das Erzeugen neuer Chunks ist beim Start die größte
   Last – vor allem, wenn alle gleichzeitig `/rtp` machen. Mit dem Plugin
   [Chunky](https://modrinth.com/plugin/chunky) auf dem SMP:
   ```
   /chunky world world
   /chunky center 0 0
   /chunky radius 5000
   /chunky start
   ```
   Das dauert je nach Rechner einige Stunden – rechtzeitig starten.
   Setz danach in `plugins/BetterRTP/config.yml` den `max-radius` auf denselben
   Wert (5000), dann landet beim Start niemand in noch nicht erzeugtem Gelände.
2. Den Ablauf mit **`/testrelease`** einmal durchspielen.
3. **`/bettersmp leistung`** auf dem SMP ausführen, bis alles grün ist.
4. [spark](https://spark.lucko.me/) auf den SMP legen. Wenn es hakt:
   `/spark profiler start`, eine Minute warten, `/spark profiler stop` – der
   Link zeigt genau, was die Zeit frisst.

## Einstellungen auf dem SMP

**Empfohlen** heißt: bringt spürbar etwas, ohne dass Spieler es merken.
**Optional** ändert das Spielgefühl leicht und ist Geschmackssache.

### `server.properties`

| Einstellung | Wert | Warum |
|---|---|---|
| `view-distance` | `8` bis `10` | Jeder Chunk mehr kostet bei vielen Spielern spürbar. Empfohlen. |
| `simulation-distance` | `6` | So weit laufen Mobs, Pflanzen und Farmen um jeden Spieler. Empfohlen. |
| `network-compression-threshold` | `-1` | Hinter Velocity auf demselben Rechner komprimiert schon der Proxy – sonst passiert es doppelt. Empfohlen (nur wenn Proxy und Server im selben Rechenzentrum stehen). |
| `sync-chunk-writes` | `false` | Chunks werden im Hintergrund gespeichert. Empfohlen. |
| `entity-broadcast-range-percentage` | `75` | Tiere und Mobs werden etwas später sichtbar – spart vor allem Bedrock-Spielern Arbeit. Optional. |

### `config/paper-world-defaults.yml`

```yaml
misc:
  redstone-implementation: ALTERNATE_CURRENT   # empfohlen: viel schnelleres Redstone
environment:
  optimize-explosions: true                    # empfohlen: TNT/Creeper gleich, nur schneller
chunks:
  max-auto-save-chunks-per-tick: 8             # empfohlen: Autosave verteilt statt ruckartig
  entity-per-chunk-save-limit:                 # empfohlen: Schutz vor Lag-Maschinen
    arrow: 16
    experience_orb: 16
    snowball: 8
    ender_pearl: 8
    fireball: 8
    small_fireball: 8
    egg: 8
collisions:
  max-entity-collisions: 2                     # optional: weniger Rechenaufwand in Mob-Farmen
entities:
  spawning:
    per-player-mob-spawns: true                # empfohlen (Standard)
    despawn-ranges:                            # optional: Monster verschwinden näher am Spieler
      monster:
        soft: 30
        hard: 56
tick-rates:
  grass-spread: 4                              # optional
  mob-spawner: 2                               # optional
```

### `spigot.yml` (unter `world-settings.default`)

```yaml
entity-activation-range:     # optional: weiter entfernte Mobs denken seltener
  animals: 16
  monsters: 24
  raiders: 48
  misc: 8
  water: 8
  villagers: 16
  flying-monsters: 48
```

### `bukkit.yml`

```yaml
spawn-limits:                # optional: etwas weniger Mobs gleichzeitig
  monsters: 50
  animals: 8
  water-animals: 3
  water-ambient: 5
  ambient: 1
```

### Lobby und Duels

Beide Welten sind klein. `view-distance=6` und `simulation-distance=4` reichen
und lassen mehr Luft für den SMP, wenn alles auf einem Rechner läuft.

## Startbefehl (Arbeitsspeicher und Java)

Immer **Java 21**, `-Xms` und `-Xmx` gleich groß. Für den SMP mit 100 bis 150
Spielern 8 bis 10 GB, Lobby 2 bis 3 GB, Duels 3 bis 4 GB.

**Paper-Server** (Beispiel 10 GB, sogenannte Aikar-Flags):

```
java -Xms10G -Xmx10G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -jar paper.jar --nogui
```

Ab 12 GB stattdessen `-XX:G1NewSizePercent=40 -XX:G1MaxNewSizePercent=50
-XX:G1HeapRegionSize=16M -XX:G1ReservePercent=15
-XX:InitiatingHeapOccupancyPercent=20`.

**Velocity** mit Geyser (2 GB, ab etwa 50 Bedrock-Spielern 3 GB):

```
java -Xms2G -Xmx2G -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:+UnlockExperimentalVMOptions -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch -XX:MaxInlineLevel=15 -jar velocity.jar
```

Bei einem Hosting-Panel gehören die Flags in das Feld für die
Startparameter.

## Velocity (`velocity.toml`, Abschnitt `[advanced]`)

| Einstellung | Wert | Warum |
|---|---|---|
| `compression-threshold` | `256` | Standard, passt. |
| `compression-level` | `-1` | Standard. Wenn der Proxy-Rechner knapp an Rechenleistung ist: `4` (etwas mehr Datenverkehr, deutlich weniger Rechenzeit). |
| `login-ratelimit` | `1000` | Wie lange nach einem Login von **derselben IP-Adresse** der nächste warten muss. Standard sind 3 Sekunden – an einer Schule oder bei Geschwistern teilen sich viele Spieler eine Adresse. `1000` lässt eine Anmeldung pro Sekunde und Adresse zu. |

## Bedrock (Geyser auf dem Proxy)

- `use-direct-connection: true` und `disable-compression: true` stellt
  SMPProxy beim Start selbst ein (`bedrock.geyser-optimieren` in der
  SMPProxy-Config). Es wirkt nach dem nächsten Proxy-Neustart.
- Stehen auf dem Server viele Spielerköpfe als Deko herum, entlastet
  `max-visible-custom-skulls: 64` und `custom-skull-render-distance: 24` in der
  Geyser-Config schwache Handys.
- Die Sichtweite für Bedrock-Spieler stellst du in der BetterSMP-Config unter
  `leistung.bedrock-sichtweite` ein (Standard 6, 0 = wie Java).
