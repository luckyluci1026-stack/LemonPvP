# Beiträge zu Lemon

## Grundregeln

1. **Standard-Paper bleibt Standard-Paper.** Änderungen an Paper-/Minecraft-Code nur, wenn
   es technisch keine Alternative gibt; dann als kleinster möglicher, dokumentierter Patch
   (siehe `docs/UPSTREAM-SYNC.md`, Abschnitt „Warum der CraftServer-Patch nötig ist“).
2. **Keine Hintergrundaktivität bei `enabled: false`.** Jede neue Funktion der Runtime muss
   das einhalten (`BootstrapTest`).
3. **Nichts behaupten, was nicht geprüft ist.** Dokumentation unterscheidet zwischen
   getesteten und ungetesteten Aussagen.
4. **Keine Geheimnisse** (Schlüssel, Tokens, Passwörter) im Repository oder in der CI.

## Projektstruktur

| Pfad | Inhalt |
|---|---|
| `runtime/runtime-api` | öffentliche Modul-API (plattformneutral) |
| `runtime/runtime-core` | Runtime-Kern + Tests (lokal ohne Paper baubar) |
| `runtime/runtime-paper-api` | Paper-spezifische Modul-API |
| `runtime/runtime-paper` | Paper-Adapter (wird in den Server kompiliert) |
| `runtime/example-module` | Beispielmodul |
| `versions/<mc>/` | Paperweight-Fork-Projekt je Minecraft-Version |
| `scripts/fork/` | Generatoren für Versionsprojekte und Patches |
| `scripts/ci/` | Server-Smoke-/Integrationstests |
| `docs/` | Dokumentation |

## Entwicklungsablauf

```bash
cd runtime && ./gradlew build                       # Kern + Tests (JDK 21+)
cd runtime && ./gradlew :runtime-paper:compileJava :example-module:assemble -PpaperApiVersion=26.2.build.+ -PjavaRelease=25
cd versions/26.2 && ./gradlew applyAllPatches build createPaperclipJar
```

* Runtime-Code muss mit **Java 21** (1.21.11) kompilieren; der Produktionscode des Kerns
  wird mit `-Xlint:all -Werror` übersetzt.
* Der Adapter darf nur Paper-API verwenden, die in **allen** unterstützten Versionen
  existiert (Adventure 4.26 **und** 5.2). Die CI kompiliert ihn gegen jede Version.
* Neue Lebenszyklus- oder Sicherheitslogik braucht Tests in `runtime-core`
  (Integrationstests mit echten Modul-JARs: `ModuleLifecycleIntegrationTest`).
* Fork-Patches werden **nur** über `scripts/fork/generate_patches.py` geändert, nicht von
  Hand; danach `applyAllPatches` für alle betroffenen Versionen ausführen lassen (CI).

## Code-Stil

* Java: 4 Leerzeichen, `final` für nicht vererbbare Klassen, Javadoc an öffentlichen Typen,
  keine Wildcard-Imports im Produktionscode, `this.` für Felder.
* Keine Reflection in Server-Interna; keine `net.minecraft`-/CraftBukkit-Imports in der Runtime
  (Ausnahme: die drei Hook-Zeilen in `CraftServer`).
* Kommentare auf Englisch, Dokumentation unter `docs/` auf Deutsch.

## Commits und CI

* Kleine, nachvollziehbare Commits mit Begründung im Text.
* Die CI (`Lemon CI`) muss grün sein: Runtime-Tests, Adapter/Beispielmodul je Version,
  Fork-Build mit Paper-Tests, Server-Tests (Runtime aus / an / Neustart).
* Artefakte werden nur aus erfolgreichen Läufen hochgeladen.

## Lizenz

Mit einem Beitrag stimmst du zu, dass er unter der GPL-3.0 (`LICENSE`) veröffentlicht wird.
Siehe `NOTICE.md`.
