# Servertest der LemonPvP-Suite

`smoke-pvp.txt` startet einen echten PaperSwap-Server, lädt die vier Module und prüft
Abhängigkeiten, simulierte Kills, Konfiguration, Hot-Swap, Persistenz und das Entladen.
Das Testskript selbst (`scripts/ci/smoke-test.sh`) gehört zu PaperSwap.

```bash
# 1. Module bauen (siehe ../README.md)  -> pvp-suite/build/modules/
# 2. PaperSwap-Server-JAR besorgen (CI-Artefakt paperswap-<version> oder selbst gebaut)
./prepare-pvp-server.sh /tmp/pvp-test ../build/modules
../../../PaperSwap/scripts/ci/smoke-test.sh /pfad/zu/paperswap-<mc>.jar /tmp/pvp-test ./smoke-pvp.txt
```

Der Test braucht Java 21 (1.21.11) bzw. 25 (26.x); die `eula.txt` legt `smoke-test.sh` an. Er lief bis zum Umzug der Suite in der CI von PaperSwap
(Servertest 9) und ist dort nicht mehr enthalten.
