# 🍋 Lemon Hosting – Kundenportal mit Panel

Kunden melden sich an und **verwalten ihren Minecraft-Server selbst**: starten,
stoppen, neu starten, live in die Konsole schauen, Befehle tippen, Dateien
hochladen und bearbeiten. Wie Pterodactyl, nur viel kleiner — und ohne
Datenbankserver und ohne Daemon.

Server können auf **mehreren Maschinen** liegen: Auf jeder weiteren läuft
`daemon.js`, das Portal steuert sie über HTTP. Wer nur einen Rechner hat,
merkt davon nichts — das ist der Normalfall und bleibt es.

Jeder Server läuft **in seinem eigenen Docker-Container**, wenn Docker da ist:
Arbeitsspeicher und CPU sind dann hart begrenzt, der Server sieht nur seinen
eigenen Ordner und läuft nicht als root. Ohne Docker läuft alles genauso, nur
direkt als Java-Prozess — das Panel sagt dazu, woran man ist.

Wer schon ein echtes Pterodactyl betreibt, lässt es stehen: Trägt ein Admin
beim Server eine Pterodactyl-Adresse ein, verlinkt das Portal nur dorthin und
fasst den Server nicht selbst an. Beides nebeneinander geht also.

Bezahlt wird **nicht** im Portal. Das passiert in der Schule, bar, gegen den
Bestellbogen, den das Portal vorausgefüllt ausdruckt.

```
node start.js
```

Mehr braucht es nicht. **Keine npm-Pakete, kein Build.** Node 22 bringt
Webserver und SQLite mit, beides wird direkt benutzt. Beim ersten Start legt
das Portal einen Admin-Zugang an und zeigt das Passwort einmal im Terminal —
notier es dir, danach steht es nirgends mehr.

```
PORT=8080 node start.js                 # anderer Port fürs Portal
DB=/pfad/portal.db node start.js        # andere Datenbankdatei
SERVER_DIR=/pfad/server node start.js   # wo die Minecraft-Server liegen
SERVER_HOST=mc.schule.de node start.js  # Adresse, die Kunden angezeigt wird
KATALOG_DIR=/pfad/zu/jars node start.js # woher die Plugins kommen
DOCKER=aus node start.js                # Container-Betrieb abschalten
DOCKER_IMAGE=eclipse-temurin:17-jre \
  node start.js                         # anderes Image (ältere Minecraft-Versionen)
```

## Lokal ausprobieren

**Voraussetzung:** Node 22.5 oder neuer. Prüfen mit `node --version`. Ist es
zu alt, sagt dir das Portal das beim Start — es holt sich SQLite aus Node
selbst, deshalb die Mindestversion. Für die Server selbst brauchst du
außerdem **Java 21** (`java -version`).

Kommt `node wurde nicht als Name eines Cmdlet … erkannt`, ist Node noch gar
nicht installiert:

| System | Befehl |
|---|---|
| **Windows** | `winget install OpenJS.NodeJS.LTS` |
| **macOS** | `brew install node` |
| **Debian/Ubuntu** | `curl -fsSL https://deb.nodesource.com/setup_22.x \| sudo -E bash - && sudo apt install -y nodejs` |

Sonst geht auch der Installer von [nodejs.org](https://nodejs.org) — nimm die
**LTS**-Variante.

**Danach das Terminal einmal schließen und neu öffnen.** Sonst kennt es den
Befehl `node` immer noch nicht; der Suchpfad wird erst beim Start eines neuen
Fensters gelesen. Das ist der häufigste Folgestolperer nach der Installation.

**Wichtig:** Das Portal liegt auf dem Branch
`claude/bettersmp-minecraft-plugin-rzjgz5`, nicht auf `main`. Nach dem Klonen
landest du auf `main`, und dort gibt es den Ordner `lemon-hosting/` noch gar
nicht. Also **erst den Branch wechseln, dann in den Ordner**:

```bash
git clone https://github.com/luckyluci1026-stack/LemonPvP.git
cd LemonPvP
git checkout claude/bettersmp-minecraft-plugin-rzjgz5
cd lemon-hosting
node start.js
```

Läuft genauso unter Windows in PowerShell — `git` und `node` können dort mit
`/` im Pfad umgehen.

Im Terminal steht dann die Adresse und — nur beim allerersten Start — der
Admin-Zugang:

```
  🍋 Lemon Hosting Kundenportal
     läuft auf  http://localhost:3000
     Datenbank  daten/portal.db
     Server in  /home/du/lemon-hosting/server

  ┌─────────────────────────────────────────────┐
  │  Erster Start – so kommst du rein:          │
  │  Benutzer   admin                           │
  │  Passwort   jtUYNeQVYA4u                    │
  └─────────────────────────────────────────────┘
```

Browser auf **http://localhost:3000**. Beenden mit **Strg+C** — dabei
bekommen alle laufenden Minecraft-Server erst ein `stop` und Zeit, ihre Welt
zu speichern.

Falls etwas schiefgeht:

| Meldung | was zu tun ist |
|---|---|
| `Der Pfad … lemon-hosting kann nicht gefunden werden` | Du bist noch auf `main`. `cd LemonPvP`, dann `git checkout claude/bettersmp-minecraft-plugin-rzjgz5` |
| `Cannot find module … start.js` | Du bist im falschen Ordner. `cd LemonPvP/lemon-hosting` |
| `not a git repository` | Du bist außerhalb des geklonten Ordners. `cd LemonPvP` |
| `"node" wurde nicht als Name eines Cmdlet … erkannt` | Node ist nicht installiert oder das Terminal kennt es noch nicht — siehe oben |
| `Port 3000 ist schon belegt` | Läuft noch in einem anderen Fenster — dort Strg+C, oder `PORT=3001 node start.js` |
| Im Panel: `Java fehlt oder ließ sich nicht starten` | Java 21 installieren, Terminal neu öffnen, Portal neu starten |

### Ein Rundgang, der alles zeigt

1. **Pakete** ansehen — die Preise kommen aus `src/preise.js`.
2. **Server bestellen** → Paket *Wood*, dann *Subdomain* und *FLFAC*
   ankreuzen. Rechts läuft der Preis mit: 3,99 + 0,25 + 0,30 = **4,54 €**.
   Jetzt oben auf *Coal* umstellen — beide Posten springen auf **0,00 €
   (im Paket)**, die Summe bleibt bei 5,99 €. Genau der Sonderfall, den man
   von Hand falsch rechnet.
3. Formular unten ausfüllen, Regeln ankreuzen, **abschicken**.
4. **Anmelden** als `admin` mit dem Passwort von oben.
5. In der **Verwaltung** steht die Anfrage → *Ansehen* → *Annehmen*.
   Benutzername und Startpasswort für den Kunden vergeben (mindestens 8
   Zeichen). Daraus entstehen Kunde und Server auf einmal.
6. **Bestellbogen** öffnen — alles ausgefüllt, das richtige Paket
   angekreuzt, die Zahlungsfelder absichtlich leer zum Ausfüllen.
   *Drucken* zeigt die Seite ohne Menü und Knöpfe.
7. Oben rechts **abmelden**, als der eben angelegte Kunde anmelden.
8. **Panel öffnen** → *Dateien* → eine **server.jar** hochladen
   ([papermc.io](https://papermc.io/downloads), ziehen und fallen lassen).
9. Im Panel unter **Plugins** ein paar auswählen — `BetterSMP` und
   `SMPContent` zum Beispiel. Dann **EULA akzeptieren** → **▶ Starten**. Die Konsole
   füllt sich live, der Punkt oben rechts springt von *gestoppt* über
   *startet …* auf *läuft*.
10. Unten `say Hallo` eintippen → steht sofort in der Konsole. **↑** holt
    den letzten Befehl zurück, wie in einem Terminal.
11. **■ Stoppen** — die Welt wird gespeichert, dann geht der Prozess aus.

### Nochmal von vorn anfangen

Die Datenbank ist eine einzige Datei, die Server liegen in einem Ordner
daneben:

```bash
rm -rf daten server                          # Linux und macOS
Remove-Item -Recurse daten, server           # Windows PowerShell
node start.js
```

### Automatisch durchklicken lassen

Linux und macOS:

```bash
rm -rf /tmp/portal-test*
PORT=3111 DB=/tmp/portal-test.db SERVER_DIR=/tmp/portal-test-server \
  node start.js > /tmp/portal.log 2>&1 &
sleep 2
ADMINPW=$(grep -oP 'Passwort\s+\K\S+' /tmp/portal.log) node test/durchklicken.mjs
```

Windows PowerShell:

```powershell
Remove-Item -ErrorAction Ignore -Recurse test-portal.db*, test-server
$env:PORT=3111; $env:DB="test-portal.db"; $env:SERVER_DIR="test-server"
Start-Process node -ArgumentList "start.js" -RedirectStandardOutput portal.log -NoNewWindow
Start-Sleep 2
$env:ADMINPW = (Select-String -Path portal.log -Pattern 'Passwort\s+(\S+)').Matches[0].Groups[1].Value
node test/durchklicken.mjs
```

**102 Prüfungen**, davon allein 19 dafür, dass niemand an fremde Server kommt
und dass man aus dem Dateimanager nicht ausbrechen kann.

## Was drin ist

**Für alle** – Pakete und Preise, ein Konfigurator mit Live-Preis, die
Nutzungsregeln.

**Für Kunden** – das **Panel**: Start, Stopp, Neustart, Live-Konsole mit
Befehlseingabe, **CPU- und Speicherverbrauch live**, Serversoftware per Klick
installieren, Dateiverwaltung mit Editor und Upload, Plugins aus dem Katalog,
Backups auf Knopfdruck, Zeitplan für Neustart und Sicherung, Spielerliste.
Und den Server mit anderen **teilen**, ohne das Passwort weiterzugeben.
Dazu **Zwei-Faktor-Anmeldung** mit einer Authenticator-App.

**Für das Team** – eine Übersicht mit dem Laufstatus jedes Servers, Kunden-
und Serververwaltung, ein Protokoll über alles, was passiert ist, und die
beiden Papierdokumente zum Ausdrucken. Admins kommen in jedes Panel und
können einen Server **auf eine andere Maschine umziehen**.

## Container: wo die Grenzen echt werden

Ohne Container ist „4,25 GB RAM" eine Zahl auf der Rechnung und sonst nichts.
Ein Server mit einem Speicherleck zieht die ganze Maschine runter, und alle
anderen Klassen sitzen im Lag. Mit Docker gilt:

| Was | Wie | Wirkung |
|---|---|---|
| Arbeitsspeicher | `--memory` + `--memory-swap` gleich | Wer die Grenze reißt, wird abgeschossen — und nur er |
| CPU | `--cpus` aus dem Paket | Coal bekommt 1,25 Kerne, egal wie viele die Maschine hat |
| Prozesse | `--pids-limit 512` | Eine Fork-Bombe trifft nur den eigenen Container |
| Dateien | `-v <ordner>:/data` | Der Server sieht seinen Ordner, nicht die Platte |
| Rechte | `--user`, `no-new-privileges` | Kein root im Container |
| Netz | `-p <port>:<port>` | Nur der eigene Port ist von außen offen |

Nachgemessen statt behauptet: In einem 64-MB-Container beendet der Kernel eine
JVM mit 512 MB Heap mit **Exit 137**; in einem 1-GB-Container läuft dieselbe
JVM durch. Und die JVM sieht das Container-Limit, nicht die Maschine — bei
1 GB Container rechnet sie mit 256 MB Heap, obwohl der Rechner viel mehr hat.

### Warum der Heap kleiner ist als der Container

`-Xmx` bekommt **85 %** der Containergrenze, nicht 100 %. Eine JVM braucht
neben dem Heap noch Metaspace, Threadstacks und Direktpuffer. Wer beides
gleichsetzt, bekommt einen Server, den der Kernel ohne Vorwarnung abschießt —
„exit code 137" und kein Wort in der Konsole, warum. Das ist der häufigste
Fehler in selbstgebauten Panels.

### Nicht als root

Läuft das Portal selbst als `root`, wäre `--user 0:0` genau der Fehler, den
Container verhindern sollen. Dann nimmt das Panel stattdessen `1000:1000`
(oder `DOCKER_USER`) und schreibt den Serverordner einmal auf diese Kennung
um — sonst dürfte der Server seine eigene Welt nicht speichern.

Läuft das Portal als normaler Benutzer, bekommt der Container dessen Kennung.
Dann gehören die Dateien hinterher niemand Fremdem und der Dateimanager kann
sie weiter bearbeiten.

### Vorbereiten

```bash
docker pull eclipse-temurin:21-jre
```

Beim Start steht im Terminal, woran man ist:

```
     Docker     29.3.1 · Image eclipse-temurin:21-jre liegt bereit
```

Fehlt das Image oder Docker, sagt das Portal das genauso deutlich und läuft
ohne Container weiter. Für ältere Minecraft-Versionen, die Java 17 brauchen,
trägst du bei dem Server in der Verwaltung ein anderes Image ein.

### Der Unterschied zu Pterodactyl

Pterodactyl spricht mit der Docker-API. Hier ruft das Portal schlicht
`docker run` auf und redet über stdin und stdout mit dem Server — genau wie
vorher mit `java`. Am Panel ändert sich dadurch fast nichts, nur der Befehl
davor.

## Mehrere Maschinen: Knoten

Reicht eine Kiste nicht mehr, kommt eine zweite dazu. Auf ihr läuft der
**Daemon** — das Gegenstück zu Pterodactyls „Wings":

```bash
# auf der anderen Maschine, im selben Ordner wie das Portal
node daemon.js
```

Beim ersten Start steht dort Adresse und Zeichen. Beides trägst du im Portal
unter **Verwaltung → Knoten** ein. Danach kannst du beim Anlegen eines Servers
auswählen, auf welcher Maschine er liegen soll.

Ab da ist für den Kunden nichts anders: Konsole, Dateien, Backups und Plugins
sehen genauso aus, laufen aber über die Leitung. Der Browser redet weiterhin
**nur mit dem Portal** — der Daemon muss von außen gar nicht erreichbar sein.

### Drei Entscheidungen, die das tragen

**Der Daemon führt keine Datenbank.** Er weiß nur, was Ordner und Prozesse
sind; wieviel Speicher jemand gebucht hat, schickt das Portal bei jedem Aufruf
mit. Damit stehen Pakete und Preise weiter an genau einer Stelle — in
`src/preise.js`, so wie vorher.

**Der Daemon benutzt dieselben Module.** `panel.js`, `dateien.js` und
`sicherung.js` laufen dort unverändert; der Daemon ist nur eine HTTP-Hülle
darum. Deshalb kann ein Knoten alles, was das Portal lokal auch kann, ohne
dass es zweimal geschrieben wäre.

**`wo.js` ist die Weiche.** Eine Funktion je Aktion, drinnen wird entschieden:
`knoten_id = 0` heißt „hier". Der Rest des Portals muss kaum wissen, ob ein
Server nebenan oder woanders steht.

### Wenn ein Knoten ausfällt

Der Zustand kommt aus einem Zwischenspeicher, den ein Ticker alle vier
Sekunden auffrischt — die Seiten rendern ihn mitten im HTML, daraus ein
`await` zu machen hätte jede Seite angefasst. Ist die Antwort älter als
20 Sekunden, steht **„nicht erreichbar"** da. Das ist ehrlicher als
„gestoppt": Wir wissen es dann schlicht nicht.

Ein Start oder ein Befehl an eine tote Maschine scheitert mit Ansage, nicht
mit einer Fehlerseite. Und nach einem Fehlschlag wird ein paar Sekunden lang
sofort abgesagt, statt jedes Mal die volle Frist abzuwarten — sonst hinge eine
Panel-Seite mit drei Abfragen bei einer ausgefallenen Maschine spürbar lange.

### Sicherheit

Der Daemon hat **keine Benutzerverwaltung**, nur ein Zeichen, das beim ersten
Start erzeugt und in `daten/daemon.json` abgelegt wird. Jede Anfrage muss es
im Kopf `X-Lemon-Zeichen` mitbringen; verglichen wird zeichenweise ohne
Abbruch.

Er gehört deshalb **ins interne Netz oder hinter einen Reverse-Proxy mit
HTTPS** — sonst geht das Zeichen im Klartext über die Leitung.

### Umziehen

Ein Server muss nicht bleiben, wo er angelegt wurde. In der Verwaltung steht
unter jedem Server eine Karte **Umziehen**: Maschine auswählen, Knopf drücken.

Der Ablauf ist immer derselbe, egal in welche Richtung:

1. Der Server muss **aus** sein. Dateien unter einem laufenden Minecraft
   wegzukopieren endet zuverlässig in einer kaputten Welt. Bei einem fernen
   Server wird dafür nicht dem Zwischenspeicher geglaubt, sondern direkt
   nachgefragt — der ist bis zu vier Sekunden alt, und „lief eben noch nicht"
   ist hier die falsche Auskunft.
2. Alles einpacken. Liegt der Server auf einem Knoten, geschieht das über ein
   **Backup, das dort liegenbleibt** — das ist die Sicherheitskopie, falls
   unterwegs etwas schiefgeht.
3. Auf der Zielmaschine auspacken, durch denselben Einsperr-Test wie beim
   Zurückspielen eines Backups.
4. **Erst danach** Knoten und Port in der Datenbank ändern.

Die Reihenfolge ist der Punkt. Die Datenbank wird zuletzt angefasst: Bricht
der Umzug vorher ab — tote Maschine, volle Platte, gekapptes Netz —, zeigt der
Server weiter auf die alte Maschine, wo alles noch unverändert liegt. Ein
Eintrag, der ins Leere zeigt, wäre der schlimmere Ausgang, weil dann niemand
mehr sieht, wo die Dateien eigentlich sind.

Zwei Dinge macht das Portal bewusst **nicht** von selbst:

**Die alten Dateien löschen.** Das ist die eine Sache, die sich nicht
zurückholen lässt. Nach dem Umzug steht in der Meldung, wo sie liegen; weg
macht sie der Admin, wenn er gesehen hat, dass drüben alles läuft.

**Die weiteren Ports umnummerieren.** Den Hauptport darf das Portal ändern —
er geht als `--port` an den Server und als `-p` an Docker, beides kommt aus
der Datenbank; ist er drüben belegt, wird der nächste freie genommen und das
in der Meldung gesagt. Bei den weiteren Ports geht das nicht: Auf welchem Port
Geyser oder Dynmap horcht, steht in deren eigener Konfigurationsdatei im
Serverordner. Eine stille Umnummerierung hieße, dass das Panel 19133
durchreicht und das Plugin weiter auf 19132 wartet — und niemand fände,
warum plötzlich keiner mehr hereinkommt. Also nur melden.

## Serversoftware: auswählen statt hochladen

Im Panel steht **Art** und **Version**, das Panel holt die `server.jar` beim
Hersteller:

| Art | woher |
|---|---|
| **Paper** | api.papermc.io – der Standard für Plugin-Server |
| **Purpur** | api.purpurmc.org – Paper mit mehr Stellschrauben |
| **Velocity** | api.papermc.io – der Proxy davor, kein Spielserver |
| **Vanilla** | Mojang – ohne Plugins, dafür das Original |

Beim Herunterladen gehen drei Dinge schief, und alle drei sind abgefangen:
Eine **Fehlerseite statt der Jar** (unter 100 KB → die alte Datei bleibt heil),
ein **Fehler des Herstellers**, und ein **Abbruch mittendrin** (erst `.teil`,
dann umbenennen). Die Versionslisten kommen von den Herstellern, nicht aus dem
Code — eine eingetragene Liste wäre nach zwei Monaten veraltet.

Wer eine eigene Jar hat, lädt sie weiterhin unter *Dateien* hoch.

## Startup: eigene Flaggen und Parameter

Im Adminbereich steht bei jedem Server ein **Startup**-Abschnitt — Startdatei,
Java-Flaggen, Startbefehl. Darunter die **Vorschau**, wie der Server wirklich
gestartet wird. Leer lassen heißt: die eingebauten Vorgaben, es ändert sich
also nichts, solange niemand etwas ändert.

```
java {{SPEICHER}} {{FLAGGEN}} -jar {{JAR}} nogui --port {{PORT}}
```

`{{SPEICHER}}` und `{{FLAGGEN}}` werden zu **mehreren** Argumenten,
`{{JAR}}`, `{{PORT}}` und `{{RAM}}` zu genau **einem**. Der Unterschied ist
nicht kosmetisch: Eine Jar namens `mein server.jar` wäre sonst beim Zerlegen
wieder zweierlei geworden, und niemand hätte das Leerzeichen verdächtigt.

### Es gibt keine Shell

Der Befehl wird selbst zerlegt und direkt gestartet — nie über `sh -c`. Ein
`; rm -rf /` im Flaggenfeld wird damit zu einem Argument namens `;`, nicht zu
einer zweiten Anweisung. Trotzdem lehnt das Panel solche Eingaben ab: Eine
klare Absage ist ehrlicher als stilles Durchreichen. Ebenso ein `-jar` in den
Flaggen (das startete eine andere Datei) und Pfade in der Startdatei (die
zeigten auf einen fremden Server).

## Netzwerk: mehr als ein Port

Ein Minecraft-Server braucht oft mehrere Ports. **Geyser** lässt
Bedrock-Spieler über `UDP 19132` herein, **Dynmap** zeigt eine Karte auf einem
Webport, Voice-Chat-Plugins wollen ihren eigenen. Im Container kommt von außen
nur durch, was ausdrücklich dasteht — deshalb trägt man sie im Panel unter
*Netzwerk* ein, mit Protokoll (TCP, UDP oder beides).

Ports werden **je Maschine** gezählt: Zwei Server auf verschiedenen Knoten
dürfen denselben Port haben, auf derselben Kiste nicht. Ports unter 1024 lehnt
das Panel ab — die darf ein gewöhnlicher Prozess ohnehin nicht öffnen.

Das ist Besitzersache, nicht Unterbenutzersache: Ein Port ist eine Tür nach
außen, und wer sie aufmacht, sollte auch für den Server geradestehen.

## Aktivität

Jeder Server hat seine eigene Liste: wer wann was gemacht hat — auch, was der
Zeitplan von allein erledigt hat. Wer wissen will, warum sein Server heute
Nacht neu gestartet ist, liest das an seinem Server nach und nicht in einer
Liste über alle Klassen.

Ältere Einträge, die noch keine Servernummer tragen, werden über den
Detailtext mitgefunden — die Liste fängt also nicht bei der Umstellung an.

## API für Skripte

Ein Discord-Bot, der den Serverstatus meldet. Eine Zeile, die vor der
Doppelstunde alle Klassenserver hochfährt. Dafür gibt es unter **API** Zugänge:

```bash
curl -H "Authorization: Bearer lemon_…" http://localhost:3000/api/server
curl -X POST -H "Authorization: Bearer lemon_…" \
     http://localhost:3000/api/server/1/start
```

| Aufruf | was er tut |
|---|---|
| `GET /api/server` | alle, die dieser Zugang sehen darf |
| `GET /api/server/:id` | einer, mit Verbrauch und Spielerliste |
| `POST /api/server/:id/start` | starten |
| `POST /api/server/:id/stopp` | stoppen |
| `POST /api/server/:id/neustart` | neu starten |
| `POST /api/server/:id/befehl` | Feld `befehl` |

**Ein Schlüssel kann nie mehr als der Kunde, dem er gehört.** Er ist ein
anderer Weg herein, keine Abkürzung an den Rechten vorbei — dieselbe Prüfung
wie im Panel. Nimmt der Besitzer einem Unterbenutzer das Recht *steuern*, kann
dessen Schlüssel den Server im selben Moment nicht mehr starten.

Der Schlüssel steht **genau einmal** da, direkt nach dem Anlegen. Danach liegt
in der Datenbank nur sein Hash. Das ist unbequemer, als ihn nachschlagen zu
können, und genau richtig: Ein Schlüssel, den das Portal noch kennt, ist einer,
den jemand aus dem Portal holen kann.

Die API läuft **ohne CSRF-Prüfung**, und das ist kein Versehen: CSRF schützt
davor, dass eine fremde Seite den Browser eines Angemeldeten benutzt. Ein
Skript hat keinen Browser, und ein Schlüssel im Kopf wird nicht automatisch
mitgeschickt. Die Prüfung ginge ins Leere und machte die API nur unbenutzbar.

## Wer darf mit? Unterbenutzer

In einer Klasse verwaltet selten nur einer den Server. Statt das Passwort
weiterzugeben, trägt der Besitzer einen Zugang ein und kreuzt an, was der darf:
Konsole, Steuern, Dateien, Backups, Plugins.

Die Rechte sind absichtlich grob — wer die Konsole hat, kann ohnehin `op`
tippen, und fein abgestufte Rechte gäben nur ein Gefühl von Sicherheit, das
nicht trägt. Was wirklich zählt, ist getrennt: **Freigaben und Kosten bleiben
allein beim Besitzer.**

Geprüft wird an genau einer Stelle. Wer nicht eingetragen ist, bekommt 404
statt 403 — er soll nicht einmal erfahren, dass es den Server gibt. Wer ein
Recht nicht hat, bekommt 403, auch beim direkten Aufruf der Adresse: Die
Oberfläche blendet die Bereiche zwar aus, aber darauf verlässt sich nichts.

## Das Panel

Ein Server ist einfach ein Ordner:

```
server/1/            alles zu Server #1
server/1/server.jar  die Serversoftware (Paper, Purpur, …)
server/1/eula.txt    legt der EULA-Knopf an
server/1/plugins/    Plugins
```

Beim Start wird `java` als Kindprozess gestartet, `stdout` läuft in einen
Ringpuffer und von dort per **Server-Sent Events** in jedes offene
Browserfenster. Befehle gehen den umgekehrten Weg in `stdin`.

### Jeder Server hat seinen eigenen Port

Der erste bekommt 25565, der nächste 25566, und so weiter — vergeben beim
Anlegen, änderbar in der Verwaltung. Ohne das würde der zweite Server beim
Start kommentarlos an „Address already in use" scheitern, und niemand wüsste
warum.

Der Port geht als Startargument an Java (`--port`), nicht über
`server.properties`. So stimmt er auch dann noch, wenn jemand die Datei im
Editor angefasst hat. Im Panel steht oben, was man in Minecraft eintippt:
`klasse8b.lemon-servers.de:25566`. Ohne Subdomain nimmt das Portal
`SERVER_HOST`.

### Backups sind ein Knopf, keine Bitte

„Unsere Welt ist kaputt, kannst du die von gestern wiederholen" ist bei
Schulservern die häufigste Frage überhaupt. Deshalb steht im Panel ein Knopf:
packt den Serverordner in eine ZIP-Datei, hebt die letzten fünf auf und wirft
ältere weg. Herunterladen, zurückspielen und löschen geht daneben.

Die Backups liegen **neben** dem Serverordner (`sicherungen/7/`), nicht darin —
sonst packte jedes Backup alle vorherigen mit ein und die Datei verdoppelte
sich jedes Mal.

Den ZIP-Packer schreibt `src/zip.js` selbst, mit dem `zlib` aus Node. Eine
npm-Abhängigkeit, die in zwei Jahren ein Sicherheitsupdate braucht, wäre für
ein Schulprojekt ein schlechter Tausch. ZIP und nicht tar.gz, weil Windows ZIP
von sich aus öffnet.

Beim **Zurückspielen** gehen alle Dateinamen aus dem Archiv durch denselben
Einsperr-Test wie der Dateimanager. Ein präpariertes ZIP mit `../../etc/passwd`
darin („Zip Slip") schreibt nichts außerhalb des Serverordners — die Einträge
werden gezählt und übersprungen. Zurückspielen geht außerdem nur bei
gestopptem Server.

Grenzen, die das Portal deutlich sagt statt still zu scheitern: 65535 Dateien
und 4 GB pro Archiv (kein Zip64).

### Plugins per Klick statt per Upload

Im Panel steht eine Liste: **BetterSMP**, **BetterRTP**, **Lifesteal+**,
**EasyBedrock**, **FastShop**, **SMPContent**, **SMPProxy** — jeweils mit
kurzer Beschreibung und einem Knopf. Installieren kopiert die Jar nach
`plugins/`, Entfernen wirft sie raus. Beides wirkt beim nächsten Neustart,
und genau das steht auch dran.

Woher die Jars kommen, sucht das Portal selbst: erst `KATALOG_DIR`, sonst der
eigene `katalog/`-Ordner, sonst das `dist/` dieses Repos — dort liegen die
fertigen Plugins ohnehin. Welcher Ordner es geworden ist, steht beim Start im
Terminal, damit niemand raten muss.

```
     Plugins    /home/du/LemonPvP/dist (7 im Katalog)
```

Eine ältere Version desselben Plugins wird beim Installieren entfernt — sonst
lädt Bukkit beide und beschwert sich über doppelte Befehle. Der Name aus dem
Formular wird nie zu einem Pfad zusammengebaut, sondern gegen die Katalogliste
geprüft; was dort nicht steht, passiert nicht.

Beschreibungen stehen in `katalog/katalog.json`, Schlüssel ist der Dateiname
ohne Version (`BetterSMP-1.0.0.jar` → `BetterSMP`). Eine Jar ohne Eintrag
erscheint trotzdem, dann eben nur mit ihrem Dateinamen.

### Zeitplan: eine Uhrzeit, kein Cron-Ausdruck

Zwei Felder im Panel: *Jede Nacht neu starten um* und *Jeden Tag sichern um*.
Leer lassen heißt aus. Kein `0 4 * * *` — „04:00" versteht jeder, der
Fünf-Felder-Ausdruck nicht.

Zwei Entscheidungen, die dahinterstehen:

- Ein **gestoppter Server wird nicht heimlich hochgefahren**. Wer ihn abends
  ausgemacht hat, will ihn morgens nicht laufen sehen — der Neustart wird
  übersprungen und das im Protokoll vermerkt.
- Vor jedem Backup geht **`save-all`** an den laufenden Server, dann drei
  Sekunden Pause. Minecraft hält geänderte Chunks im Arbeitsspeicher; ohne das
  sichert man einen Stand von vorhin. Absichtlich kein `save-off` davor: Stürzt
  das Portal dazwischen ab, bliebe das Speichern dauerhaft aus, und niemand
  wüsste warum.

Die Uhr läuft im Portal mit und schaut jede halbe Minute nach — sie arbeitet
also nur, solange das Portal läuft.

### Wer online ist, steht in der Konsole

Minecraft meldet jeden Beitritt und jeden Abgang, also liest das Panel einfach
mit und führt daraus eine Namensliste — live, ohne Neuladen. Die Alternative
wäre Query oder RCON: ein zweiter Port, eine weitere Einstellung und ein Stück
Netzwerkcode. Für eine Namensliste ist Mitlesen ehrlicher.

Chatzeilen zählen nicht mit: Wer `Tom joined the game` in den Chat schreibt,
steht in der Konsole in spitzen Klammern und fällt durchs Raster.

## Neben Pterodactyl betreiben

Beides gleichzeitig geht, pro Server entschieden. In der Verwaltung steht bei
jedem Server das Feld **Pterodactyl-Adresse**:

- **leer** → das Portal betreibt den Server selbst, mit Konsole und Dateien.
- **ausgefüllt** (z. B. `https://panel.example.de/server/a1b2c3d4`) → der
  Server läuft in Pterodactyl. Das Panel zeigt dann keine Knöpfe, sondern
  einen Link dorthin.

Im zweiten Fall verweigert das Portal Start, Stopp und Dateizugriff auch
dann, wenn jemand die URL direkt aufruft. Zwei Stellen, die denselben Server
starten dürfen, wären ein Rezept für kaputte Welten — deshalb entscheidet
genau ein Feld, wer zuständig ist.

Nur `http` und `https` werden übernommen. Ein `javascript:…` in dem Feld
landete sonst in einem Link, und ein Admin hätte sich selbst eine Falle
gestellt.

## Vier Entscheidungen, die den Unterschied machen

### Der Arbeitsspeicher kommt aus dem gebuchten Paket

`-Xmx` wird nicht konfiguriert, sondern **gerechnet**: Paket-RAM plus
gebuchte Zusatzleistungen, dieselbe Funktion wie auf dem Bestellbogen. Wer
Wood mit 2,5 GB und zweimal *1 GB extra* hat, bekommt `-Xmx4608M`. Niemand
kann sich selbst mehr zuteilen, als er gebucht hat, weil es gar keine Stelle
gibt, an der man es eintippen könnte.

### Der Dateimanager ist eingesperrt

Jeder Pfad wird aufgelöst und danach geprüft, ob er wirklich noch im
Serverordner liegt (`innerhalb()` in `src/dateien.js`). `../../etc/passwd`,
`..`, `../7/welt` — alles endet mit einer Absage, nicht mit einer Datei.
Bei einem Panel, auf dem mehrere Klassen ihre Server haben, wäre das der
schlimmste denkbare Fehler, deshalb steht die Prüfung an genau einer Stelle
und wird vom Test gezielt angegriffen.

Genauso beim Zugriff: **jede** Panel-Route geht durch `meinServer()`. Wer
`/panel/2` aufruft, ohne dass ihm Server 2 gehört, bekommt 404 — Panel,
Konsole, Dateien und Upload gleichermaßen.

### Gestoppt wird mit `stop`, nicht mit `kill`

Erst geht `stop` in die Konsole, dann speichert Minecraft die Welt selbst.
Nur wer nach 30 Sekunden immer noch nicht weg ist, wird hart beendet. Das
gilt auch für **Strg+C** am Portal: es fährt erst alle Server herunter und
geht dann selbst.

### Die Preisliste steht genau einmal

Alles in `src/preise.js` — Pakete, Zusatzleistungen, was bei welchem Paket
schon inklusive ist. Der Konfigurator rechnet damit, die Serververwaltung
rechnet damit, der gedruckte Bestellbogen nimmt dieselben Zahlen, und das
Panel leitet daraus den Arbeitsspeicher ab. Einen Preis ändern heißt: eine
Zahl in einer Datei ändern.

Eine Subdomain kostet bei **Wood** 0,25 € und ist bei **Coal** und
**Diamond** inklusive; dasselbe gilt für FLFAC. Genau solche Sonderfälle
verrechnet man von Hand — hier macht es eine Funktion, und alle benutzen sie.

## Kein Geld im Portal

Kassiert wird in der Schule, von Hand. Das Portal führt darüber **bewusst
kein Konto**: keine Beträge, keine Zahlungstabelle, kein „bezahlt bis". Was
es kann, ist Server verwalten und laufen lassen.

Auf dem **Bestellbogen** bleiben die Felder *Betrag erhalten*, *Zeitraum* und
*Kassiert von* leer zum Ausfüllen — dafür ist der Bogen da. Alles andere
steht vorausgefüllt drin: Name, Klasse, Servername, Subdomain, Paket, alle
Zusatzleistungen und die Preisspalte, mit angekreuztem Paket. Abgeschrieben
wird nichts mehr, und die Summe stimmt, weil sie aus derselben Preisliste
kommt wie das Portal.

Server werden nie wirklich aus der Datenbank geworfen, sondern nur als
gelöscht gekennzeichnet. Sonst könnte die **Löschbestätigung** hinterher
weder Servername noch Owner nennen.

Beide Dokumente stehen auf jeder Serverseite in der Verwaltung.

## Aufbau

```
start.js              starten
src/preise.js         die Preisliste — hier änderst du Preise
src/db.js             SQLite: Kunden, Server, Bestellungen, Protokoll
src/panel.js          Minecraft-Prozesse: starten, stoppen, Konsole
src/dateien.js        Dateiverwaltung samt Einsperr-Test
src/sicherung.js      Backups anlegen, herunterladen, zurückspielen
src/zeitplan.js       die Uhr für Neustart und automatische Sicherung
src/katalog.js        Plugins aus einem Ordner anbieten und installieren
src/docker.js         Container: Grenzen setzen, aufräumen, abschießen
src/messung.js        CPU, Arbeitsspeicher und Netz messen
src/arten.js          Serversoftware bei den Herstellern holen
src/start.js          Startbefehl: Vorlage, Platzhalter, Zerlegung
src/api.js            Schlüssel für Skripte
src/zweifach.js       TOTP: der zweite Faktor beim Anmelden
src/wo.js             die Weiche: hier oder auf einer anderen Maschine?
src/fern.js           mit einem Daemon reden
src/umzug.js          einen Server auf eine andere Maschine bringen
daemon.js             läuft auf jeder weiteren Maschine
katalog/              Plugin-Jars und ihre Beschreibungen
src/zip.js            ZIP packen und entpacken, ohne npm-Paket
src/web.js            HTTP-Kleinkram: Cookies, Formulare, CSRF, Router
src/server.js         alle Routen an einer Stelle
src/seiten/           die HTML-Seiten
oeffentlich/          CSS und das bisschen Browser-JavaScript
  konsole.js          Live-Konsole (EventSource)
  dateien.js          Upload mit Fortschritt
test/alles.sh         alle zehn Testreihen nacheinander
daten/portal.db       die Datenbank (nicht im Git)
server/<id>/          die Minecraft-Server (nicht im Git)
sicherungen/<id>/     die Backups (nicht im Git)
```

## Zwei-Faktor-Anmeldung

Ein Passwort, das in der Schule einmal über die Schulter mitgelesen wurde, ist
weg — und wer sich am Portal anmeldet, kann die Server ganzer Klassen löschen.
Unter **Sicherheit** schaltet jeder Kunde für sich einen zweiten Faktor ein.

Das Verfahren ist **TOTP nach RFC 6238**, dasselbe, das Google Authenticator,
Aegis, 2FAS und der Rest sprechen: ein gemeinsames Geheimnis, die Uhrzeit in
Dreißig-Sekunden-Schritten, HMAC-SHA1 darüber, sechs Ziffern herausschneiden.
Alles davon steckt in Node schon drin — es kommt kein Paket dazu.

Dass die Apps dieselben Zahlen sehen werden, ist nicht behauptet, sondern
nachgerechnet: `test/zweifach.mjs` prüft die **sechs Testwerte aus dem RFC
selbst**, bis `T=20000000000` — also auch jenseits von 2³², wo ein Zähler in
32 Bit überliefe.

**Einrichten hat drei Schritte, und der mittlere ist der Grund dafür:** Erst
liegt ein Geheimnis da, das noch nicht gilt, dann muss ein Code daraus
stimmen, und erst danach wird umgeschaltet. Wer die App falsch einträgt, merkt
es also, solange er noch angemeldet ist — und nicht beim nächsten Anmelden,
wenn es zu spät ist. Das Passwort wird dabei mitgefragt, damit niemand an
einem offen stehenden Browser den Besitzer aussperrt.

| Fall | Was passiert |
|---|---|
| Handy geht eine halbe Minute falsch | Ein Schritt Spielraum in jede Richtung — der Code geht trotzdem. Zwei Schritte nicht mehr, sonst gölte ein abgefangener Code zu lange. |
| Jemand liest den Code über die Schulter ab | Derselbe Zeitschritt wird kein zweites Mal angenommen. In diesen dreißig Sekunden sitzt derjenige noch daneben. |
| Handy weg | **Acht Ersatzcodes**, beim Einschalten einmal angezeigt. Jeder gilt genau einmal; das Portal zählt mit, wie viele noch offen sind. |
| Handy *und* Ersatzcodes weg | Der Admin schaltet den zweiten Faktor in der Kundenverwaltung ab. Einschalten kann er ihn für niemanden — dafür bräuchte er das Geheimnis in dessen App. |

Zwischen Passwort und Code gibt es eine **halbe Anmeldung**: eine Sitzung, die
den Namen trägt, aber keine Rechte. Sie läuft nach zehn Minuten ab, und jede
Seite, die nach dem angemeldeten Nutzer fragt, übersieht sie.

Was der zweite Faktor **nicht** schützt: die API-Schlüssel. Die sind selbst
schon ein Geheimnis und werden von Skripten benutzt, die kein Handy haben —
sie kommen weiter ohne Code herein. Steht auch so auf der Seite.

## Sicherheit

Passwörter liegen als scrypt-Hash mit eigenem Salz. Sitzungen laufen über ein
httpOnly-Cookie. Jedes Formular trägt ein CSRF-Zeichen, das gegen ein Cookie
geprüft wird — ohne das könnte eine fremde Seite im Namen eines Angemeldeten
dessen Server stoppen oder Dateien löschen. Beim Upload steht dasselbe
Zeichen in der URL, weil dort der Body die Datei ist.

Jeder Start, Stopp und Konsolenbefehl landet im Protokoll, mit Zeitpunkt und
Person.

**Bevor das öffentlich erreichbar ist**, gehören drei Dinge davor:

1. **HTTPS** (Caddy oder nginx als Reverse Proxy) — ohne das gehen Passwörter
   im Klartext über das Netz. Bei nginx `proxy_buffering off;` für
   `/panel/*/konsole`, sonst kommt die Konsole nur ruckweise an.
2. Ein **Backup** von `daten/portal.db`, `server/` und `sicherungen/` — die
   Backups im Panel liegen auf derselben Platte und helfen nicht, wenn die
   ausfällt.
3. Ein **eigener Benutzer** für das Portal. Wer im Panel eine Datei
   bearbeiten darf, arbeitet mit den Rechten des Portalprozesses — das sollte
   nicht `root` sein.
4. **Docker**, wenn mehrere Klassen auf derselben Maschine liegen. Ohne
   Container kann ein einzelner Server alle anderen lahmlegen.

## Test

Zehn Reihen, zusammen **402 Prüfungen**. Alle auf einmal:

```bash
ERSATZ_JAR=/pfad/zu/server.jar test/alles.sh
```

Jede Reihe, die ein Portal braucht, bekommt ein eigenes, frisches — ein Test,
der auf den Datenbankresten des vorherigen aufsetzt, geht irgendwann grundlos
kaputt, und man sucht dann am falschen Ende. Einzelheiten in
[`test/README.md`](test/README.md).

`test/durchklicken.mjs` geht das Portal einmal komplett durch — Anfrage
abschicken, annehmen, Server anlegen, Dokumente drucken, ins Panel, Dateien
anlegen, bearbeiten, hochladen, löschen, Konsole anzapfen, ausbrechen wollen,
an fremde Server wollen, Pterodactyl-Übergabe, Portvergabe, Backup anlegen,
herunterladen und zurückspielen, Zeitplan setzen, Plugins installieren und
entfernen. **102 Prüfungen** — und sie laufen in beiden Betriebsarten durch,
mit Container und ohne.

`test/zeitplan.mjs` prüft die Uhr, ohne bis vier Uhr morgens zu warten:
`pruefe()` nimmt die Zeit als Argument. **18 Prüfungen** — dass um 02:59 nichts
passiert, um 03:00 genau einmal gesichert wird, ein Pterodactyl-Server in Ruhe
gelassen wird, ein gestoppter Server nicht anspringt und ein laufender
tatsächlich mit neuer Prozessnummer zurückkommt.

```bash
node test/zeitplan.mjs
```

`test/docker.mjs` prüft die Container-Betriebsart. Der erste Teil braucht kein
Docker: Die Argumente für `docker run` baut eine eigene Funktion, und genau
dort steckt das, was leicht falsch wäre — eine vergessene Speichergrenze fällt
sonst erst auf, wenn eine Klasse die andere ausbremst. Der zweite Teil startet
wirklich Container und misst nach, ob die Grenzen greifen; ohne Docker wird er
übersprungen. **19 Prüfungen.**

```bash
node test/docker.mjs
```

`test/knoten.mjs` startet einen echten Daemon auf Port 8391 mit eigenem
Serverordner, trägt ihn als Knoten ein und fährt alles über die Leitung:
Datei anlegen, Upload, Plugin installieren, starten, Konsole mitlesen,
Backup, stoppen. Zum Schluss wird der Daemon abgeschossen — dann muss das
Panel „nicht erreichbar" sagen statt abzustürzen. **35 Prüfungen.**

```bash
ADMINPW=... ERSATZ_JAR=/pfad/server.jar node test/knoten.mjs
```

Dazu drei kleinere, die ohne laufendes Portal auskommen:

```bash
node test/start.mjs     # Startbefehl: 33 Prüfungen
node test/arten.mjs     # Serversoftware: 20 Prüfungen
node test/zeitplan.mjs  # Zeitplan: 18 Prüfungen
```

Und einer, der ein laufendes Portal braucht:

```bash
ADMINPW=... node test/teilen.mjs   # Unterbenutzer: 38 Prüfungen
ADMINPW=... node test/api.mjs      # API, Ports, Aktivität: 34 Prüfungen
```

Zusammen **302 Prüfungen**.

`test/start.mjs` weist nach, dass `; rm -rf / && curl x | sh` im Flaggenfeld
zu Argumenten wird und nicht zu Befehlen — das ist die Prüfung, auf die es
dort ankommt. `test/arten.mjs` ruft keine Hersteller-API an (ein Test, der von
papermc.io abhängt, schlägt irgendwann fehl, ohne dass jemand etwas kaputt
gemacht hat), sondern hängt eine eigene Art an eine lokale Quelle.

Der Lebenslauf eines Serverprozesses — starten, `Done (…)` erkennen, Port
durchreichen, Befehl schicken, Spieler zählen, neu starten, sauber stoppen —
wurde zusätzlich am laufenden Portal geprüft: mit einem echten Paper 1.21.11
(119 Konsolenzeilen live, `say` durchgereicht, neuer Prozess nach Neustart)
und mit einem kleinen Java-Programm als Serverersatz, das dieselben Ausgaben
macht.

## Was Pterodactyl hat und das hier nicht

**MySQL-Datenbanken pro Server.** Pterodactyl legt auf Knopfdruck eine
Datenbank samt Benutzer an. Dafür bräuchte es einen MySQL-Server und einen
Treiber — und das Portal kommt bewusst ohne ein einziges npm-Paket aus. Ein
selbst geschriebener MySQL-Client wäre machbar, aber ich könnte ihn hier nicht
gegen einen echten Server prüfen, und ungeprüfte Protokoll-Implementierungen
sind genau die Sorte Code, die später kaputtgeht. Für die Plugins in diesem
Repo reicht ohnehin SQLite.

**Grafische QR-Codes** beim Einrichten der Zwei-Faktor-Anmeldung. Das
Geheimnis steht in Vierergruppen zum Abtippen da, dazu der
`otpauth://`-Link — jede App nimmt beides. Einen QR-Code zu erzeugen wäre
nicht schwer; ich hätte hier nur kein Werkzeug gehabt, um nachzusehen, ob der
erzeugte auch wirklich lesbar ist, und ein QR-Code, den die App nicht annimmt,
ist schlimmer als keiner: Man sucht dann am falschen Ende.

## Was noch fehlt

- **Backups außer Haus** — sie liegen auf derselben Platte wie die Server.
- **Subdomains** werden erfasst, aber nicht automatisch im DNS eingetragen —
  auch der SRV-Eintrag, der den Port versteckt, muss von Hand gesetzt werden.
- Ein **Speicher-Limit**, das wirklich greift. Die Anzeige stimmt, aber wer
  über sein Kontingent hinausschreibt, wird bisher nur angezeigt, nicht
  gebremst.

Der ZIP-Packer wurde gegen fremde Entpacker geprüft: Ein Archiv aus
`src/zip.js` besteht Pythons `zipfile.testzip()` und `unzip -t`, und der
Inhalt kommt byteweise identisch wieder heraus — auch bei Umlauten im
Dateinamen. Umgekehrt wird ein mit Python gebautes Angriffs-Archiv beim
Zurückspielen korrekt abgewehrt.
