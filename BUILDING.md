# Bauen der BetterSMP Suite

Die Plugins bauen gegen die **offizielle PaperMC-API 1.21.11**. Da diese API
nicht als fertiges Artefakt in Maven Central liegt, wird sie einmalig aus den
offiziellen Quellen kompiliert und ins lokale Maven-Repository installiert.

## Voraussetzungen

- **JDK 21**
- **Maven 3.9+**
- **git**
- Internetzugang zu Maven Central (für die Bibliotheken der Paper-API)

## Schritt 1 – Brigadier bauen (Mojang, offiziell)

Die Paper-API benötigt `com.mojang:brigadier`. Aus den offiziellen Mojang-Quellen:

```bash
git clone --depth 1 https://github.com/Mojang/brigadier
cd brigadier
javac -encoding UTF-8 --release 21 -d out $(find src/main/java -name "*.java")
( cd out && jar cf ../brigadier-1.3.10.jar . )
mvn install:install-file -Dfile=brigadier-1.3.10.jar \
    -DgroupId=com.mojang -DartifactId=brigadier -Dversion=1.3.10 -Dpackaging=jar
cd ..
```

## Schritt 2 – Paper-API 1.21.11 bauen (offiziell)

```bash
git clone --branch ver/1.21.11 --depth 1 --filter=blob:none --sparse \
    https://github.com/PaperMC/Paper paper-src
cd paper-src
git sparse-checkout set paper-api
```

Die zusammengeführten Quellen (`paper-api/src/main/java` **und**
`paper-api/src/generated/java`) werden mit dem beiliegenden POM
(`tools/paper-api-pom.xml`) gebaut:

```bash
# Quellen einsammeln
mkdir -p paper-api-build/src/main/java
cp -r paper-src/paper-api/src/main/java/.      paper-api-build/src/main/java/
cp -r paper-src/paper-api/src/generated/java/. paper-api-build/src/main/java/
cp -r paper-src/paper-api/src/main/resources   paper-api-build/src/main/
cp tools/paper-api-pom.xml paper-api-build/pom.xml

cd paper-api-build
mvn clean install -DskipTests
```

Damit liegt `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT` im lokalen Repo.

## Schritt 3 – Die Suite bauen

```bash
cd bettersmp-suite
mvn clean install -DskipTests
```

Fertige Jars: `bettersmp-suite/*/target/*-1.0.0.jar` → kopiert nach `dist/`.

## Hinweis zum Agent-Proxy

Läuft der Build hinter einem HTTPS-Proxy (z. B. in einer CI-Sandbox), muss
Maven den Proxy kennen. Das Skript `gen-mvn-settings.sh` schreibt eine passende
`~/.m2/settings.xml` aus der Umgebungsvariable `$HTTPS_PROXY`:

```bash
./gen-mvn-settings.sh
```

Auf einem normalen Rechner mit direktem Internetzugang ist das nicht nötig.
