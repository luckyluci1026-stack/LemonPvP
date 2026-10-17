# LemonPvP

Dateien des LemonPvP-Servers.

| Pfad | Inhalt |
|---|---|
| `index.html`, `index.zip` | Website |
| [`pvp-suite/`](pvp-suite/README.md) | **LemonPvP-Suite**: vier Module für [PaperSwap](https://github.com/paperswapstudios-official/PaperSwap) – Statistiken mit Elo, Combat-Tag, Killstreaks und Scoreboard |

## LemonPvP-Suite

Die Suite läuft auf einem PaperSwap-Server (Minecraft 1.21.11 oder 26.x) und lässt sich dort im
laufenden Betrieb laden, tauschen und entladen. Installation, Befehle, Konfiguration und Tests:
[`pvp-suite/README.md`](pvp-suite/README.md).

Bauen (braucht einen Checkout von PaperSwap neben diesem Repository):

```bash
git clone https://github.com/paperswapstudios-official/PaperSwap ../PaperSwap
cd pvp-suite
./gradlew collect -PpaperApiVersion=1.21.11-R0.1-SNAPSHOT
```

Ergebnis: `pvp-suite/build/modules/pvp-*.jar` + `.sha256`. In GitHub Actions baut der Workflow
**LemonPvP suite** dieselben Module für 1.21.11, 26.1.2, 26.2 und 26.3. Solange PaperSwap privat
ist, braucht er ein Repository-Secret `PAPERSWAP_TOKEN` mit Lesezugriff auf PaperSwap.

## Lizenz

GPL-3.0, siehe [`LICENSE`](LICENSE).
