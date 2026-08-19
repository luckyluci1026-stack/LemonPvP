# Test

Zehn Reihen, zusammen 402 Prüfungen. Alle auf einmal:

```
ERSATZ_JAR=/pfad/zu/server.jar test/alles.sh
```

`ERSATZ_JAR` ist eine Jar, die sich wie ein Minecraft-Server verhält:
Sie gibt eine `Done (…)`-Zeile aus und hört auf `stop`. Ohne sie laufen
alle Reihen trotzdem durch, nur die Teile, die wirklich einen Server
starten, werden übersprungen. Eine echte Paper-Jar geht auch — sie
braucht nur länger.

Jede Reihe, die ein Portal braucht, bekommt ein eigenes, frisches. Ein
Test, der auf den Datenbankresten des vorherigen aufsetzt, geht
irgendwann grundlos kaputt, und man sucht dann am falschen Ende.

## Was die Reihen prüfen

| Reihe | worum es geht |
|---|---|
| `start.mjs` | Startbefehl und Flaggen. Der Kern ist `zerlege()`: Weil der Befehl nie durch eine Shell läuft, wird aus `; rm -rf /` ein Argument und keine zweite Anweisung. |
| `docker.mjs` | Die Argumente für `docker run` — Speichergrenze, CPU-Anteil, keine neuen Rechte. Startet am Ende wirklich einen Container, wenn Docker da ist. |
| `zeitplan.mjs` | Nächtlicher Neustart und Backup. Die Uhrzeit wird übergeben statt abgelesen, damit sich der Test nicht auf die Wanduhr verlässt. |
| `arten.mjs` | Serversoftware installieren. Ruft nicht papermc.io an, sondern einen kleinen Server nebenan — geprüft wird, was schiefgehen kann: eine Fehlerseite als `server.jar`, eine halbe Datei, eine zerstörte heile alte. |
| `durchklicken.mjs` | Das Portal einmal komplett wie ein Benutzer. Der wichtigste Block steht unten: ob ein Kunde an fremde Server kommt und ob man aus dem Dateimanager ausbrechen kann. |
| `teilen.mjs` | Unterbenutzer. Wer nur die Konsole darf, darf auch nur die Konsole — über jeden Weg, nicht nur über den Knopf, der ihm fehlt. |
| `api.mjs` | API-Zugänge, weitere Ports, Aktivität. Ein Schlüssel kann nie mehr als der Kunde, dem er gehört. |
| `zweifach.mjs` | Die Zwei-Faktor-Anmeldung. Der erste Teil rechnet die sechs Testwerte aus RFC 6238 nach — das ist der Nachweis, dass die Authenticator-Apps dieselben Zahlen sehen wie das Portal. Der zweite geht die Anmeldung durch, samt Ersatzcodes und dem Fall, dass jemand denselben Code zweimal benutzt. |
| `knoten.mjs` | Ein Server auf einer zweiten Maschine — echter Daemon auf Port 8391, alles über die Leitung. Am Ende wird der Daemon abgeschossen, damit man sieht, was das Panel dann sagt. |
| `umzug.mjs` | Umzug zwischen Maschinen, in beide Richtungen. Der Punkt: Scheitert die Übertragung, darf in der Datenbank nichts umgestellt sein — sonst zeigt das Panel ins Leere, obwohl alle Dateien noch da sind. |

## Einzeln

Die vier oberen brauchen nichts weiter:

```
node test/start.mjs
```

Die sechs unteren brauchen ein Portal auf Port 3111 und dessen
Startpasswort:

```
rm -f /tmp/portal-test.db*
PORT=3111 DB=/tmp/portal-test.db SERVER_DIR=/tmp/portal-server \
  node start.js > /tmp/portal.log 2>&1 &
sleep 2
ADMINPW=$(grep -oP 'Passwort\s+\K\S+' /tmp/portal.log) \
  SERVER_DIR=/tmp/portal-server node test/durchklicken.mjs
```
