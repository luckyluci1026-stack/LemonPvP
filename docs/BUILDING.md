# Fork bauen

## Voraussetzungen

| Ziel | JDK | Netzwerkzugriff beim Build |
|---|---|---|
| `versions/1.21.11` | 21 | `repo.papermc.io`, `plugins.gradle.org`, Maven Central, `github.com` (Paper-Upstream), Mojang (`piston-meta`/`piston-data.mojang.com`, `libraries.minecraft.net`), `hub.spigotmc.org` (BuildData für 1.21.11) |
| `versions/26.1.2`, `26.2`, `26.3` | 25 | wie oben (ohne SpigotMC) |
| `runtime/` (Runtime-Kern + Tests) | 21 oder neuer | Maven Central, Gradle-Distribution |

Git muss mit Benutzername/E-Mail konfiguriert sein (Paperweight erzeugt Commits beim
Patchen): `git config --global user.name "…"; git config --global user.email "…"`.

## Server-JAR einer Version bauen

```bash
cd versions/26.2                       # bzw. 1.21.11, 26.1.2, 26.3
./gradlew applyAllPatches              # klont Paper @ paperRef, wendet Paper- und PaperSwap-Patches an
./gradlew build                        # kompiliert (inkl. Runtime) und führt Paper-Tests aus
./gradlew createPaperclipJar           # 26.x  → paperswap-server/build/libs/paperswap-paperclip-*.jar
./gradlew createMojmapPaperclipJar     # 1.21.11 → paperswap-server/build/libs/paperswap-paperclip-*.jar
```

Genau diese Befehle führt die CI (`.github/workflows/paperswap-ci.yml`) pro Version aus. Mit
gesetzter Umgebungsvariable `BUILD_NUMBER` erhalten 26.x-Builds die Version
`<mc>.build.<n>-alpha`, sonst `<mc>.local-SNAPSHOT`; 1.21.11 nutzt `1.21.11-R0.1-SNAPSHOT`.
Die CI benennt Artefakte eindeutig: `paperswap-<mc>-build.<run>-<commit>.jar` + `.sha256`.

## Runtime separat bauen und testen

```bash
cd runtime
./gradlew build                                            # runtime-api + runtime-core inkl. aller Runtime-Tests
./gradlew :runtime-paper:compileJava :example-module:assemble \
          -PpaperApiVersion=26.2.build.+ -PjavaRelease=25   # Adapter + Beispielmodul gegen Paper-API
```

Live-Patch-Agent und Test-Plugin:

```bash
./gradlew :livepatch-agent:jar        # → livepatch-agent/build/libs/paperswap-agent.jar
./gradlew :example-plugin:jar -PpaperApiVersion=26.2.build.+ -PjavaRelease=25   # → PaperSwapTestPlugin.jar (CI-Tests)
```

Unit-Tests der Runtime laufen mit `-javaagent` (wie ein Server). Auf einer anderen JVM,
z. B. JetBrains Runtime mit erweiterter Redefinition:
`./gradlew :runtime-core:test -PtestJava=/pfad/jbr/bin/java -PtestJvmArgs=-XX:+AllowEnhancedClassRedefinition`.

## Core-Patches bauen

```bash
cd versions/26.2
./gradlew :paperswap-server:livePatchBaseline                    # direkt nach dem Bauen der deployten JAR
# … Quellen ändern (paper-server/src/main/java, paperswap-server/src/minecraft/java) …
./gradlew :paperswap-server:createLivePatch -PpatchId=fix-tps -PpatchDescription="…" \
          -PserverJar=/pfad/zu/versions/26.2/paperswap-26.2.jar  # die JAR, die Paperclip auf dem Server schrieb (1.21.11: Pflicht)
# → paperswap-server/build/livepatch/fix-tps.swappatch + .sha256
```

Ablauf und Prüfungen auf dem Server: [`LIVE-UPDATES.md`](LIVE-UPDATES.md).

`runtime-paper` wird im Fork **nicht** über diesen Build eingebunden, sondern per
`srcDir` direkt in den Server kompiliert; der separate Build prüft nur die Kompilierbarkeit
gegen die öffentliche Paper-API und erzeugt das Beispielmodul
(`runtime/example-module/build/libs/example-greeter.jar` + `.sha256`).

## Server starten

```bash
mkdir run && cd run
cp ../paperswap-26.2-*.jar server.jar
echo "eula=true" > eula.txt            # nur nach Lesen/Akzeptieren der Minecraft EULA
java -Xms2G -Xmx4G -jar server.jar --nogui
```

Beim ersten Start entsteht `config/paperswap.yml` mit `enabled: false`. Hot-Deployment
aktivieren: siehe `README.md` → „Schnellstart Hot-Deployment“.

## Was beim Build mit dem Code passiert

```
versions/<mc>/
├── build.gradle.kts / settings.gradle.kts / gradle.properties   (generiert: scripts/fork/scaffold_version.py)
├── paperswap-api/build.gradle.kts.patch                              (generiert: scripts/fork/generate_patches.py)
├── paperswap-server/build.gradle.kts.patch                           (generiert; fügt runtime/* als srcDir hinzu, setzt Marke)
└── paperswap-server/paper-patches/files/src/main/java/org/bukkit/craftbukkit/CraftServer.java.patch   (3 Hook-Zeilen)
```

Nach `applyAllPatches` liegen die gepatchten Quellen in `paper-api/`, `paper-server/`,
`paperswap-server/src/minecraft/` – diese Verzeichnisse sind in `.gitignore` und werden nie
eingecheckt.
