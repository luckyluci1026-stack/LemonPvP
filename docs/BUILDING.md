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
./gradlew applyAllPatches              # klont Paper @ paperRef, wendet Paper- und Lemon-Patches an
./gradlew build                        # kompiliert (inkl. Runtime) und führt Paper-Tests aus
./gradlew createPaperclipJar           # 26.x  → lemon-server/build/libs/lemon-paperclip-*.jar
./gradlew createMojmapPaperclipJar     # 1.21.11 → lemon-server/build/libs/lemon-paperclip-*.jar
```

Genau diese Befehle führt die CI (`.github/workflows/lemon-ci.yml`) pro Version aus. Mit
gesetzter Umgebungsvariable `BUILD_NUMBER` erhalten 26.x-Builds die Version
`<mc>.build.<n>-alpha`, sonst `<mc>.local-SNAPSHOT`; 1.21.11 nutzt `1.21.11-R0.1-SNAPSHOT`.
Die CI benennt Artefakte eindeutig: `lemon-<mc>-build.<run>-<commit>.jar` + `.sha256`.

## Runtime separat bauen und testen

```bash
cd runtime
./gradlew build                                            # runtime-api + runtime-core inkl. aller Runtime-Tests
./gradlew :runtime-paper:compileJava :example-module:assemble \
          -PpaperApiVersion=26.2.build.+ -PjavaRelease=25   # Adapter + Beispielmodul gegen Paper-API
```

`runtime-paper` wird im Fork **nicht** über diesen Build eingebunden, sondern per
`srcDir` direkt in den Server kompiliert; der separate Build prüft nur die Kompilierbarkeit
gegen die öffentliche Paper-API und erzeugt das Beispielmodul
(`runtime/example-module/build/libs/example-greeter.jar` + `.sha256`).

## Server starten

```bash
mkdir run && cd run
cp ../lemon-26.2-*.jar server.jar
echo "eula=true" > eula.txt            # nur nach Lesen/Akzeptieren der Minecraft EULA
java -Xms2G -Xmx4G -jar server.jar --nogui
```

Beim ersten Start entsteht `config/lemon-runtime.yml` mit `enabled: false`. Hot-Deployment
aktivieren: siehe `README.md` → „Schnellstart Hot-Deployment“.

## Was beim Build mit dem Code passiert

```
versions/<mc>/
├── build.gradle.kts / settings.gradle.kts / gradle.properties   (generiert: scripts/fork/scaffold_version.py)
├── lemon-api/build.gradle.kts.patch                              (generiert: scripts/fork/generate_patches.py)
├── lemon-server/build.gradle.kts.patch                           (generiert; fügt runtime/* als srcDir hinzu, setzt Marke)
└── lemon-server/paper-patches/files/src/main/java/org/bukkit/craftbukkit/CraftServer.java.patch   (3 Hook-Zeilen)
```

Nach `applyAllPatches` liegen die gepatchten Quellen in `paper-api/`, `paper-server/`,
`lemon-server/src/minecraft/` – diese Verzeichnisse sind in `.gitignore` und werden nie
eingecheckt.
