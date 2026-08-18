# 🍋 Lemon Hosting – Kundenportal mit Panel

Kunden melden sich an und **verwalten ihren Minecraft-Server selbst**: starten,
stoppen, neu starten, live in die Konsole schauen, Befehle tippen, Dateien
hochladen und bearbeiten. Wie Pterodactyl, nur viel kleiner — und ohne
Datenbankserver, Daemon oder Docker.

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
PORT=8080 node start.js               # anderer Port
DB=/pfad/portal.db node start.js      # andere Datenbankdatei
SERVER_DIR=/pfad/server node start.js # wo die Minecraft-Server liegen
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
9. Zurück ins Panel → **EULA akzeptieren** → **▶ Starten**. Die Konsole
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

**74 Prüfungen**, davon allein 15 dafür, dass niemand an fremde Server kommt
und dass man aus dem Dateimanager nicht ausbrechen kann.

## Was drin ist

**Für alle** – Pakete und Preise, ein Konfigurator mit Live-Preis, die
Nutzungsregeln.

**Für Kunden** – das **Panel**: Start, Stopp, Neustart, Live-Konsole mit
Befehlseingabe, Dateiverwaltung mit Editor und Upload, Speicheranzeige. Dazu
eine Übersicht, was gebucht ist und was es kostet.

**Für das Team** – eine Übersicht mit dem Laufstatus jedes Servers, Kunden-
und Serververwaltung, ein Protokoll über alles, was passiert ist, und die
beiden Papierdokumente zum Ausdrucken. Admins kommen in jedes Panel.

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

Pterodactyl trennt Panel und Daemon („Wings"), damit ein Panel viele
Maschinen steuern kann. Hier läuft alles auf einem Rechner, und das Portal
startet die Server direkt. Das ist deutlich weniger Technik — und genau
richtig, solange alle Server auf derselben Kiste liegen.

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
src/web.js            HTTP-Kleinkram: Cookies, Formulare, CSRF, Router
src/server.js         alle Routen an einer Stelle
src/seiten/           die HTML-Seiten
oeffentlich/          CSS und das bisschen Browser-JavaScript
  konsole.js          Live-Konsole (EventSource)
  dateien.js          Upload mit Fortschritt
daten/portal.db       die Datenbank (nicht im Git)
server/<id>/          die Minecraft-Server (nicht im Git)
```

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
2. Ein **Backup** von `daten/portal.db` und `server/`.
3. Ein **eigener Benutzer** für das Portal. Wer im Panel eine Datei
   bearbeiten darf, arbeitet mit den Rechten des Portalprozesses — das sollte
   nicht `root` sein.

## Test

`test/durchklicken.mjs` geht das Portal einmal komplett durch — Anfrage
abschicken, annehmen, Server anlegen, Dokumente drucken, ins Panel, Dateien
anlegen, bearbeiten, hochladen, löschen, Konsole anzapfen, ausbrechen wollen,
an fremde Server wollen, Pterodactyl-Übergabe. **74 Prüfungen.**

Der Lebenslauf eines Serverprozesses — starten, `Done (…)` erkennen, Befehl
durchreichen, neu starten, sauber stoppen — wurde zusätzlich am laufenden
Portal geprüft: mit einem echten Paper 1.21.11 (119 Konsolenzeilen live, `say`
durchgereicht, neuer Prozess nach Neustart) und mit einem kleinen Java-Programm
als Serverersatz, das dieselben Ausgaben macht.

## Was noch fehlt

- **Backups** auf Knopfdruck (Welt als Zip herunterladen).
- **Subdomains** werden erfasst, aber nicht automatisch im DNS eingetragen.
- **Zeitpläne** — z. B. jede Nacht neu starten.
- Ein **Speicher-Limit**, das wirklich greift. Die Anzeige stimmt, aber wer
  über sein Kontingent hinausschreibt, wird bisher nur angezeigt, nicht
  gebremst.
