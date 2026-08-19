# Test

Zehn Reihen, zusammen 405 Prüfungen. Alle auf einmal:

```
test/alles.sh
```

Ein paar Prüfungen starten wirklich einen Server — sonst prüft man nur,
ob Knöpfe da sind, nicht ob dahinter etwas passiert. Dafür übersetzt das
Skript einmal `ersatzserver/Server.java`: ein Programm, das sich wie ein
Minecraft-Server verhält (`Done (…)` ausgibt, auf `stop` hört, Spieler
kommen und gehen lässt), aber in einer Sekunde oben ist statt in einer
halben Minute.

Dafür braucht es das **JDK** (`javac`), nicht nur die Java-Laufzeit.
Fehlt es, laufen alle Reihen trotzdem — nur die Teile, die wirklich
starten, werden übersprungen, und das Skript sagt auch welche
(405 Prüfungen mit JDK, 389 ohne).

Mit `ERSATZ_JAR=/pfad/paper.jar test/alles.sh` nimmt es stattdessen die
angegebene Jar. Dann läuft wirklich Minecraft — dauert länger, prüft
aber dasselbe.

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
