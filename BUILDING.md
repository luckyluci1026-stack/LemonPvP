# Bauen der BetterSMP Suite

## Voraussetzungen

- **JDK 21**
- **Maven 3.9+**
- Internetzugang zu `repo.papermc.io` und `repo.maven.apache.org`

## Bauen

```bash
cd bettersmp-suite
mvn clean install
```

Das war's. Die fertigen Jars liegen danach in den jeweiligen `*/target/`-Ordnern:

```
bettersmp/target/BetterSMP-1.0.0.jar
betterrtp/target/BetterRTP-1.0.0.jar
lifesteal-plus/target/LifestealPlus-1.0.0.jar
easybedrock/target/EasyBedrock-1.0.0.jar
fastshop/target/FastShop-1.0.0.jar
```

Fertig gebaute Jars liegen außerdem direkt im Ordner `dist/`.

## Woher die Abhängigkeiten kommen

Die Paper-API und zwei ihrer Abhängigkeiten (`com.mojang:brigadier`,
`net.md-5:bungeecord-chat`) liegen **nicht** auf Maven Central, sondern im
PaperMC-Repository. Das ist in `bettersmp-suite/pom.xml` eingetragen:

```xml
<repositories>
    <repository>
        <id>papermc</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
        ...
    </repository>
</repositories>
```

Fehlt dieser Block, bricht der Build genau so ab:

```
[ERROR] dependency: com.mojang:brigadier:jar:1.3.10 (provided)
[ERROR]   ... was not found in https://repo.maven.apache.org/maven2
```

### Wenn Maven den Fehler zwischengespeichert hat

Maven merkt sich fehlgeschlagene Downloads eine Zeit lang
(*„resolution is not reattempted until the update interval has elapsed"*).
Nach dem Hinzufügen des Repositories deshalb einmal mit `-U` bauen, das
erzwingt einen neuen Versuch:

```bash
mvn -U clean install
```

Hilft das nicht, die betroffenen Ordner im lokalen Repository löschen:

```bash
rm -rf ~/.m2/repository/com/mojang/brigadier
rm -rf ~/.m2/repository/net/md-5/bungeecord-chat
rm -rf ~/.m2/repository/io/papermc/paper/paper-api
mvn -U clean install
```

## Andere Minecraft-Version

Die Paper-Version steht als Property im Parent-POM:

```xml
<paper.version>1.21.11-R0.1-SNAPSHOT</paper.version>
```

Passe sie an deine Serverversion an (z. B. `1.21.8-R0.1-SNAPSHOT`) und setze
`api-version` in den `plugin.yml`-Dateien entsprechend.

---

## Anhang: Bauen ohne Zugriff auf repo.papermc.io

Nur nötig, wenn das PaperMC-Repository in deinem Netz gesperrt ist. Dann baut
man die Paper-API einmalig aus den offiziellen Quellen ins lokale Repository.

**1. Brigadier (offizielle Mojang-Quellen)**

```bash
git clone --depth 1 https://github.com/Mojang/brigadier
cd brigadier
javac -encoding UTF-8 --release 21 -d out $(find src/main/java -name "*.java")
( cd out && jar cf ../brigadier-1.3.10.jar . )
mvn install:install-file -Dfile=brigadier-1.3.10.jar \
    -DgroupId=com.mojang -DartifactId=brigadier -Dversion=1.3.10 -Dpackaging=jar
cd ..
```

**2. Paper-API 1.21.11**

```bash
git clone --branch ver/1.21.11 --depth 1 --filter=blob:none --sparse \
    https://github.com/PaperMC/Paper paper-src
cd paper-src && git sparse-checkout set paper-api && cd ..

mkdir -p paper-api-build/src/main/java
cp -r paper-src/paper-api/src/main/java/.      paper-api-build/src/main/java/
cp -r paper-src/paper-api/src/generated/java/. paper-api-build/src/main/java/
cp -r paper-src/paper-api/src/main/resources   paper-api-build/src/main/
cp tools/paper-api-pom.xml paper-api-build/pom.xml

cd paper-api-build && mvn clean install -DskipTests
```

Danach `mvn clean install` in `bettersmp-suite` wie oben — Maven nimmt die
lokal installierten Artefakte.

## Texture-Pack bauen

Braucht **Python 3** und **Pillow** (`pip install Pillow`):

```bash
cd texturepack
python3 generate.py
```

Erzeugt `dist/SMP-Java-Pack.zip`, `dist/SMP-Bedrock-Pack.mcpack` und das
Geyser-Mapping. Details in `texturepack/README.md`.
