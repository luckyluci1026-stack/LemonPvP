# Upstream-Synchronisierung und Versionspflege

## Prinzip

Jede Version in `versions/<mc>/` ist auf **einen exakten Paper-Commit** gepinnt
(`paperRef` in `gradle.properties`). Lemon-Änderungen bestehen ausschließlich aus:

1. `lemon-server/build.gradle.kts.patch` – Fork-Registrierung (Template), Runtime-Quellen
   als `srcDir`, eigene Marke, Projektverweise;
2. `lemon-api/build.gradle.kts.patch` – Quellpfade (Template) und, ab 26.2, Entfernen der
   Upstream-Checkstyle-Verdrahtung;
3. `lemon-server/paper-patches/files/.../CraftServer.java.patch` – drei Hook-Zeilen.

Alle drei werden von `scripts/fork/generate_patches.py` aus den Upstream-Dateien des
gepinnten Commits **erzeugt**.

Ausnahme, nur 1.21.11: `lemon-server/minecraft-patches/sources/net/minecraft/world/entity/`
(`LivingEntity.java.patch`, `player/Player.java.patch`) – handgeschriebene Rückportierung von
Paper `b80b604` (Abklingzeit bei Speer-Stichen, siehe [`SPEAR-COOLDOWN.md`](SPEAR-COOLDOWN.md)).
Bei einem Upstream-Update von `ver/1.21.11` prüfen, ob der Fix dort angekommen ist
(`git merge-base --is-ancestor b80b604 <neuer Commit>` bzw. die Zeilen in
`LivingEntity.actuallyHurt`); dann die beiden Dateien löschen, sonst nach `applyAllPatches`
die Prüfung im CI-Schritt „Lemon Minecraft source patches applied“ beachten. Jede Änderung ist an genau eine Ankerzeile gebunden; fehlt
oder verdoppelt sich ein Anker upstream, bricht das Skript mit einer klaren Meldung ab.

### Warum der CraftServer-Patch nötig ist

| | |
|---|---|
| Warum | Paper bietet keine öffentliche API, die eine eingebaute Komponente nach dem Aktivieren aller Plugins, vor dem Deaktivieren beim Stopp und vor `/reload` aufruft. Ein Plugin kann sich selbst nicht vor `/reload` stoppen lassen, bevor Paper Listener/Befehle global löscht. |
| Betroffene Klasse | `org.bukkit.craftbukkit.CraftServer` (Paper-eigener Code, keine Minecraft-Klasse) |
| Auswirkung auf Standard-Paper | je Aufruf ein statischer Methodenaufruf; bei `enabled: false` wird nur `config/lemon-runtime.yml` gelesen (bzw. einmalig erzeugt) und eine Logzeile geschrieben. Alle Hooks fangen jede Exception. |
| Neu anwenden | `generate_patches.py` mit dem neuen Commit ausführen; Anker sind die Zeilen `this.syncCommands();` im `POSTWORLD`-Zweig von `enablePlugins`, der Rumpf von `disablePlugins()` und `this.commandMap.clearCommands(); // Paper - Move command reloading up` in `reload()` |

## Ablauf: Upstream-Update innerhalb einer Version (z. B. neue Paper-Commits auf `ver/26.2`)

```bash
git clone https://github.com/PaperMC/Paper /path/to/Paper     # einmalig
git -C /path/to/Paper fetch origin
NEW=$(git -C /path/to/Paper rev-parse origin/ver/26.2)

# 1) paperRef aktualisieren (Wrapper/Build-Dateien bei Bedarf mit neu erzeugen)
python3 scripts/fork/scaffold_version.py --paper-checkout /path/to/Paper --ref "$NEW" \
        --branch ver/26.2 --mc 26.2 --java 25 --paperweight <Version aus Paper/build.gradle.kts>
# 2) Patches neu erzeugen
python3 scripts/fork/generate_patches.py --paper-checkout /path/to/Paper --ref "$NEW" --version-dir versions/26.2
# 3) bauen und testen
cd versions/26.2 && ./gradlew applyAllPatches build createPaperclipJar
```

Für 1.21.11 zusätzlich `--legacy-versioning` und `--java 21`; der Paperclip-Task heißt dort
`createMojmapPaperclipJar`.

## Ablauf: neue Minecraft-Version portieren (z. B. 26.4)

1. Prüfen, ob Paper einen Branch/Tag dafür hat (`git ls-remote --heads --tags`).
2. `scaffold_version.py` mit dem neuen Commit, JDK und paperweight-Version aus Paper's
   `build.gradle.kts` (nicht raten).
3. `generate_patches.py`. Bricht es ab, den betroffenen Anker in der neuen Upstream-Datei
   suchen, die Ersetzung in `generate_patches.py` anpassen (ggf. versionsabhängig) und
   erneut laufen lassen.
4. Matrix in `.github/workflows/lemon-ci.yml` (Jobs `paper-modules` und `fork`) und
   `docs/VERSIONS.md` erweitern.
5. Runtime-Kompatibilität: `paper-modules`-Job zeigt, ob Adapter und Beispielmodul gegen
   die neue Paper-API kompilieren. API-Brüche im Adapter klein und versionsneutral lösen.

## Konfliktbehandlung

| Situation | Vorgehen |
|---|---|
| `generate_patches.py`: „anchor … found 0 times“ | Upstream hat die Zeile geändert. Neue Stelle mit gleicher Semantik suchen, Anker anpassen, Diff prüfen. |
| `applyAllPatches` schlägt bei einem Lemon-Patch fehl | Patch passt nicht zum gepinnten Commit → Patches für **diesen** Commit neu erzeugen (Schritt 2). |
| Paper-Build-Datei nutzt neue Projekte/Plugins (Beispiel 26.2: `:paper-checkstyle`) | In `generate_patches.py` behandeln (siehe unten) und dokumentieren. |
| Runtime kompiliert nicht gegen neue Paper-API | Adapter in `runtime/runtime-paper` anpassen; Kern bleibt unberührt. |

Paperweight bietet zusätzlich eigene Tasks zum Bearbeiten von Patches im Git-Workflow
(im CI-Log verifiziert, Root-Projekt): `applyAllPatches`, `applyPaperPatches`,
`applyPaperSingleFilePatches`, `applyPaperApiPatches`, `rebuildPaperPatches`,
`rebuildPaperSingleFilePatches`, `rebuildPaperApiPatches`, `fixupPaperApiFilePatches`.
Server-seitige Tasks (`applyAllServerPatches` u. a.) existieren im Projekt `:lemon-server`
nach dem ersten `applyAllPatches`; die CI listet sie im Schritt „List server patch tasks“.
Für Lemons Build-Datei-Patches ist der Generator die maßgebliche Quelle – manuell mit
`rebuildPaperSingleFilePatches` erzeugte Patches würden beim nächsten Generatorlauf
überschrieben.

## Bekannte Upstream-Anpassungen

| Version | Anpassung | Grund |
|---|---|---|
| alle | Template-Änderungen (Fork-Registrierung, `srcDir`s, `:lemon-api`) | offizieller Fork-Workflow |
| alle | `Brand-Id`/`Brand-Name` → `lemonpvp:lemon` / `Lemon` | Fork darf sich nicht als Paper ausgeben |
| 26.2, 26.3 | `paper-checkstyle`-Plugin, `paperCheckstyle {}`-Block und `checkstyle(project(":paper-checkstyle"))` aus `paper-api/build.gradle.kts` entfernt; Javadoc-Tag `apiNote` als `options.tags("apiNote:a:API Note:")` beibehalten | `:paper-checkstyle` existiert in Forks nicht; Lemon ändert keine API-Quellen |

## Ein Verzeichnis als eigenen Branch abspalten

Die gewünschte Struktur „ein Branch je Version“ lässt sich aus den Verzeichnissen
herstellen, ohne Historie zu verlieren:

```bash
git subtree split --prefix=versions/26.2 -b ver/26.2-fork-only
```

Weil die Server-Builds `../../../runtime` als Quellverzeichnis einbinden, braucht ein
abgespaltener Branch zusätzlich eine Kopie von `runtime/` (bzw. einen angepassten Pfad in
`generate_patches.py`, Konstante `RUNTIME_ROOT`).
