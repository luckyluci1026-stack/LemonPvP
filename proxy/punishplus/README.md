# PunishPlus

Velocity-Plugin: **`/offend`** (temporär) und **`/punish`** (immer
dauerhaft) - beide mit vorgefertigten Gründen aus `bans.yml`, die ihre
Dauer gleich mitbringen.

## Warum am Proxy statt auf dem SMP?

Eine Sperre soll das **ganze Netzwerk** betreffen - jeden Server,
inklusive Lobby - nicht nur den einen Server, auf dem der Befehl
getippt wurde. Am Proxy ist das einfach: Es läuft nur **ein** Prozess
für das gesamte Netzwerk, `LoginEvent` prüft dort schon, bevor überhaupt
ein Server gewählt wird. Keine Datenbank nötig, kein Abgleich zwischen
mehreren Servern - eine einzige Datei (`gesperrt.yml`) reicht.

Genau wie bei SMPProxys eigenem `/netban`: **bewusst nicht** in SMPProxy
eingebaut, sondern ein eigenes, neues Plugin - bestehende Plugins werden
nicht angefasst, nur neue hinzugefügt.

## Befehle

```
/offend <Spieler> <Grund>   - temporäre Sperre, Dauer kommt aus dem Grund
/punish <Spieler> <Grund>   - dauerhafte Sperre, unabhängig von der Dauer im Grund
/punishplus reload          - config.yml und bans.yml neu einlesen
```

Zielspieler müssen **online** sein - ohne das ließe sich
`punishplus.exempt` bei Offline-Spielern gar nicht zuverlässig prüfen.

Beide Befehle lassen sich **nicht** per Befehl rückgängig machen - das
ist Absicht, kein fehlendes Feature. Ein Fehlgriff lässt sich nur von
Hand in `gesperrt.yml` korrigieren, danach `/punishplus reload`.

`bans.yml` (im Plugin-Ordner, `plugins/punishplus/bans.yml`) legt die
Gründe fest:

```yaml
gruende:
  CHEATEN:
    text: "Cheaten / Hacks"
    dauer: "30d"      # gilt nur für /offend - /punish ist immer dauerhaft
```

Eigene Gründe ergänzen oder anpassen, danach `/punishplus reload`.

## Rechte

Wie bei `smpproxy.ban` gibt es am Proxy keinen `default: op` - Rechte
über eine Rechteverwaltung wie LuckPerms zuteilen.

| Recht | Wirkung |
|---|---|
| `punishplus.offend` | `/offend` benutzen |
| `punishplus.punish` | `/punish` benutzen |
| `punishplus.exempt` | kann weder ge-offended noch ge-punished werden |
| `punishplus.admin` | `/punishplus reload` |

## Von anderen Plugins nutzen

`PunishPlusApi` steht anderen Velocity-Plugins **im selben Proxy-Prozess**
zur Verfügung:

```java
boolean gesperrt = PunishPlusApi.punish(spieler, "CHEATEN", "AntiCheat");
```

Läuft PunishPlus nicht, liefert die API einfach `false` statt eine
Exception zu werfen. Anders als bei den Paper-Plugins der Suite gibt es
hier keinen gemeinsamen Maven-Reactor: Ein anderes Velocity-Plugin muss
`punishplus` einmal selbst bauen (`mvn install` in diesem Ordner) und
dann als `provided`-Abhängigkeit einbinden.

## Bauen

Braucht **JDK 21**, **Maven** und Zugriff auf `repo.papermc.io`
(dort liegt die Velocity-API):

```bash
cd proxy/punishplus
mvn clean package
```

Ergebnis: `target/PunishPlus-1.0.0.jar` → nach `proxy/plugins/`.

SnakeYAML wird mit ins Jar gepackt (umbenannt, damit es mit nichts
kollidiert), sonst hat das Plugin keine Abhängigkeiten.

## Einbau

`target/PunishPlus-1.0.0.jar` nach `proxy/plugins/` legen und Velocity
neu starten - siehe `../README.md` für das komplette Proxy-Setup.
