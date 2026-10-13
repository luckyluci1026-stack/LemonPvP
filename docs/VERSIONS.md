# Versionsmatrix

Stand: 2026-10-13. Quellen und Herleitung: [`ANALYSIS.md`](ANALYSIS.md), Abschnitt 3.

| Eigenschaft | 1.21.11 | 26.1 (→ 26.1.2) | 26.2 | 26.3 |
|---|---|---|---|---|
| Minecraft-Version | 1.21.11 (veröffentlicht) | 26.1 veröffentlicht; Paper pflegt die Linie als **26.1.2** | 26.2 (veröffentlicht) | 26.3 (veröffentlicht laut Minecraft Wiki) |
| Paper-Upstream | Branch `ver/1.21.11`, Tag `1.21.11`; Commit `c5eb0790` | Branch `ver/26.1.2`, Tag `26.1.2`; Commit `e4e17fc9` | Branch `ver/26.2`, Tag `26.2`; Commit `58e6e45d` | Branch `main` (`mcVersion=26.3`), **kein Tag**; Commit `6829d9e6` |
| Paper-Status | veröffentlicht | veröffentlicht (26.1.2) | veröffentlicht | Entwicklungsstand; Download-Kanal nicht direkt verifizierbar |
| Fork-Verzeichnis (dieser Branch) | `versions/1.21.11` | `versions/26.1.2` | `versions/26.2` | `versions/26.3` |
| Empfohlener eigener Branch | `lemon/ver/1.21.11` | `lemon/ver/26.1.2` | `lemon/ver/26.2` | `lemon/main` |
| Java-Version | 21 | 25 | 25 | 25 |
| Build-System | Gradle 9.2.0, paperweight 2.0.0-beta.19 (patcher), Spigot-Mappings (BuildData) | Gradle 9.4.1, paperweight 2.0.0-beta.21 | Gradle 9.4.1, paperweight 2.0.0-beta.21 | Gradle 9.8.0, paperweight 2.0.0-beta.24 |
| Paperclip-Task | `createMojmapPaperclipJar` | `createPaperclipJar` | `createPaperclipJar` | `createPaperclipJar` |
| Adventure im Server | 4.26.1 | 4.26.1 | 5.2.0 | 5.2.0 |
| Status | **unterstützt** | **unterstützt** | **unterstützt** | **in Arbeit** (baut und läuft, aber Upstream ohne Release-Tag) |
| Tests (CI-Lauf #8) | Paper-Suite 9436 Tests / 0 Fehler; Server-Tests 1–3 ✔ | Paper-Suite 9512 / 0; Server-Tests 1–3 ✔ | Paper-Suite 9764 / 0; Server-Tests 1–3 ✔ | Paper-Suite 10548 / 0; Server-Tests 1–3 ✔ |

Eine Paper-Version „26.1“ ohne Patch-Suffix gibt es upstream nicht als eigenen Branch/Tag;
Lemon unterstützt deshalb 26.1.2. Server-JARs sind je Version getrennt; eine JAR für
mehrere Minecraft-Versionen gibt es nicht und ist nicht vorgesehen.

## Testergebnisse je Version

Details, Lauf-IDs und Startzeiten: [`TESTING.md`](TESTING.md), Abschnitt „Ergebnisse aus GitHub Actions“.
