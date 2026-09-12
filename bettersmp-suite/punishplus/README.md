# PunishPlus

`/offend` (temporär) und `/punish` (immer dauerhaft) - beide mit
vorgefertigten Gründen aus `bans.yml`, die ihre Dauer gleich mitbringen.
Eigenständig, mit einer statischen API für andere Plugins.

## Befehle

```
/offend <Spieler> <Grund>   - temporäre Sperre, Dauer kommt aus dem Grund
/punish <Spieler> <Grund>   - dauerhafte Sperre, unabhängig von der Dauer im Grund
```

Beide Befehle lassen sich **nicht** per Befehl rückgängig machen - das
ist Absicht, kein fehlendes Feature. Ein Fehlgriff lässt sich nur von
Hand in `gesperrt.yml` korrigieren, danach `/punishplus reload`.

`bans.yml` (im Plugin-Ordner) legt die Gründe fest:

```yaml
gruende:
  CHEATEN:
    text: "Cheaten / Hacks"
    dauer: "30d"      # gilt nur für /offend - /punish ist immer dauerhaft
```

Zielspieler müssen online sein (wie bei den meisten anderen Befehlen der
Suite, z.B. `/freeze`) - ohne das ließe sich `punishplus.exempt` bei
Offline-Spielern gar nicht zuverlässig prüfen.

## Von anderen Plugins nutzen

`punishplus` ist als `provided`-Dependency in der Suite hinterlegt
(`bettersmp-suite/pom.xml`, `dependencyManagement`). Einfach in der
eigenen `pom.xml` referenzieren:

```xml
<dependency>
    <groupId>de.lemonpvp</groupId>
    <artifactId>punishplus</artifactId>
    <scope>provided</scope>
</dependency>
```

Und aufrufen:

```java
boolean gesperrt = PunishPlusApi.punish(spieler, "CHEATEN", "AntiCheat");
```

Läuft PunishPlus nicht auf dem Server, liefert die API einfach `false`
statt eine Exception zu werfen (gleiches Vorbild wie `BetterSMPApi`).

## Rechte
- `punishplus.offend` (Standard: op)
- `punishplus.punish` (Standard: op)
- `punishplus.exempt` - kann weder ge-offended noch ge-punished werden (Standard: aus)
- `punishplus.admin` - `/punishplus reload` (Standard: op)

## Speicherung: pro Server oder netzwerkweit

Steuert `config.yml` → `database.enabled`:

- **`false` (Standard)** - eigene `gesperrt.yml` je Server. Läuft sofort,
  ohne Einrichtung. Eine Sperre auf Server A gilt nicht auf Server B.
- **`true`** - alle Sperren landen in einer gemeinsamen MariaDB-Tabelle
  (`punishplus_gesperrt`). `/offend` und `/punish` wirken dann sofort auf
  jedem Server, der auf dieselbe Datenbank zeigt. Dafür müssen
  `host`/`port`/`database`/`user`/`password` in der `config.yml` auf
  **jedem** Server, auf dem PunishPlus läuft, identisch eingetragen sein.

  Läuft bereits eine MariaDB für BetterSMP (`bettersmp/config.yml`,
  `database.mariadb`), lassen sich dieselben Zugangsdaten und dieselbe
  Datenbank wiederverwenden - PunishPlus legt seine eigene Tabelle darin
  an, ganz ohne Kollision mit BetterSMPs eigenen Tabellen.

  Schlägt die Verbindung beim Start fehl, bleibt PunishPlus auf diesem
  Server wirkungslos (Meldung in der Konsole) statt den Server zu
  blockieren - also vor der Umstellung aller Server erst einmal testen.

  Der MariaDB-Treiber wird nur bei `database.enabled: true` tatsächlich
  gebraucht und kommt über Papers eigenen `libraries`-Mechanismus in der
  `plugin.yml` - keine zusätzliche Installation nötig.

Ein `/punishplus reload` liest `config.yml` und `bans.yml` neu ein,
wechselt aber nicht live zwischen den beiden Speicherarten - dafür
einmal den Server neu starten.
