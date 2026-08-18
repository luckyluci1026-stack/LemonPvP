# 🍋 Lemon Hosting – Kundenportal

Ein Portal für das Minecraft-Hosting: Kunden sehen ihre Server und wissen,
bis wann bezahlt ist. Das Team sieht auf einen Blick, wer demnächst abläuft,
trägt Zahlungen ein und druckt Bestellbogen und Löschbestätigung
**vorausgefüllt** aus.

```
node start.js
```

Mehr braucht es nicht. **Keine npm-Pakete, kein Build.** Node 22 bringt
Webserver und SQLite mit, beides wird direkt benutzt. Beim ersten Start legt
das Portal einen Admin-Zugang an und zeigt das Passwort einmal im Terminal —
notier es dir, danach steht es nirgends mehr.

```
PORT=8080 node start.js          # anderer Port
DB=/pfad/portal.db node start.js # andere Datenbankdatei
```

## Lokal ausprobieren

**Voraussetzung:** Node 22.5 oder neuer. Prüfen mit `node --version`. Ist es
zu alt, sagt dir das Portal das beim Start — es holt sich SQLite aus Node
selbst, deshalb die Mindestversion.

```bash
git clone https://github.com/luckyluci1026-stack/LemonPvP.git
cd LemonPvP/lemon-hosting
git checkout claude/bettersmp-minecraft-plugin-rzjgz5
node start.js
```

Im Terminal steht dann die Adresse und — nur beim allerersten Start — der
Admin-Zugang:

```
  🍋 Lemon Hosting Kundenportal
     läuft auf  http://localhost:3000
     Datenbank  daten/portal.db

  ┌─────────────────────────────────────────────┐
  │  Erster Start – so kommst du rein:          │
  │  Benutzer   admin                           │
  │  Passwort   jtUYNeQVYA4u                    │
  └─────────────────────────────────────────────┘
```

Browser auf **http://localhost:3000**. Beenden mit **Strg+C**.

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
6. Auf der Serverseite rechts **Zahlung eintragen** — der Betrag ist schon
   vorgeschlagen. Danach steht oben *bezahlt bis* mit einem Balken.
   Trag die Zahlung noch einmal ein: der neue Zeitraum schließt an den
   alten an, statt bei heute anzufangen.
7. **Bestellbogen** öffnen — alles ausgefüllt, das richtige Paket
   angekreuzt. *Drucken* zeigt die Seite ohne Menü und Knöpfe.
8. Oben rechts **abmelden**, als der eben angelegte Kunde anmelden: jetzt
   siehst du dieselben Daten aus Kundensicht, ohne Verwaltung.

### Nochmal von vorn anfangen

Die Datenbank ist eine einzige Datei. Weg damit, und der nächste Start legt
alles neu an — inklusive neuem Admin-Passwort:

```bash
rm -rf daten
node start.js
```

### Anderer Port

Ist 3000 schon belegt, sagt das Portal es dir und schlägt einen anderen vor:

```bash
PORT=8080 node start.js
```

### Automatisch durchklicken lassen

```bash
rm -f /tmp/portal-test.db*
PORT=3111 DB=/tmp/portal-test.db node start.js > /tmp/portal.log 2>&1 &
sleep 2
ADMINPW=$(grep -oP 'Passwort\s+\K\S+' /tmp/portal.log) node test/durchklicken.mjs
```

33 Prüfungen: Anfrage abschicken, annehmen, Server anlegen, zweimal zahlen,
Dokumente drucken, als Kunde anmelden, an fremde Daten wollen.

## Was drin ist

**Für alle** – Pakete und Preise, ein Konfigurator mit Live-Preis, die
Nutzungsregeln.

**Für Kunden** – die eigenen Server mit Ausstattung, Subdomain und Status,
ein Balken der zeigt, wie lange noch bezahlt ist, alle eigenen Zahlungen.

**Für das Team** – eine Übersicht, die nach Dringlichkeit sortiert (überfällig
zuerst), Kunden- und Serververwaltung, Zahlungserfassung, alle Zahlungen,
ein Protokoll, und die beiden Papierdokumente zum Ausdrucken.

## Drei Entscheidungen, die den Unterschied machen

### Die Preisliste steht genau einmal

Alles in `src/preise.js` — Pakete, Zusatzleistungen, was bei welchem Paket
schon inklusive ist. Der Konfigurator rechnet damit, die Serververwaltung
rechnet damit, der gedruckte Bestellbogen nimmt dieselben Zahlen. Einen Preis
ändern heißt: eine Zahl in einer Datei ändern.

Das ist mehr wert, als es klingt. Eine Subdomain kostet bei **Wood** 0,25 €
und ist bei **Coal** und **Diamond** inklusive; dasselbe gilt für FLFAC. Genau
solche Sonderfälle verrechnet man von Hand — hier macht es eine Funktion, und
alle benutzen sie.

Der Browser rechnet im Konfigurator zwar mit, das ist aber nur die Anzeige.
Verbindlich gerechnet wird beim Absenden noch einmal auf dem Server.

### Verlängerung setzt am Ende an, nicht bei heute

Wer eine Woche zu spät bezahlt, verliert bei naiver Rechnung eine Woche. Hier
schließt der neue Zeitraum an den alten an:

```
1. Zahlung:  18.08. → 17.09.
2. Zahlung:  17.09. → 17.10.     (nicht ab heute)
```

### Jede Zahlung steht im Protokoll

Bei Prepaid mit Bargeld in der Schule ist Nachvollziehbarkeit das Wichtigste
überhaupt. Wer wann welchen Betrag kassiert hat, steht mit Zeitstempel und
Namen im Protokoll — auch wenn später jemand etwas korrigiert.

Aus demselben Grund werden Server nie wirklich aus der Datenbank geworfen,
sondern nur als gelöscht gekennzeichnet. Sonst könnte die Löschbestätigung
hinterher weder Servername noch Owner noch Zeitraum nennen.

## Die Papierdokumente

Unterschrieben wird weiter von Hand — das bleibt so. Was sich ändert: Name,
Klasse, Servername, Subdomain, Paket, alle Zusatzleistungen, die Preisspalte
und der bezahlte Zeitraum stehen beim Ausdrucken schon drin. Das richtige
Paket ist angekreuzt. Abgeschrieben wird nichts mehr, und die Summe stimmt,
weil sie aus derselben Preisliste kommt wie das Portal.

Zu finden auf jeder Serverseite in der Verwaltung.

## Aufbau

```
start.js              starten
src/preise.js         die Preisliste — hier änderst du Preise
src/db.js             SQLite: Kunden, Server, Zahlungen, Protokoll
src/web.js            HTTP-Kleinkram: Cookies, Formulare, CSRF, Router
src/server.js         alle Routen an einer Stelle
src/seiten/           die HTML-Seiten
oeffentlich/          CSS und das bisschen Browser-JavaScript
daten/portal.db       die Datenbank (nicht im Git)
```

## Sicherheit

Passwörter liegen als scrypt-Hash mit eigenem Salz. Sitzungen laufen über ein
httpOnly-Cookie. Jedes Formular trägt ein CSRF-Zeichen, das gegen ein Cookie
geprüft wird — ohne das könnte eine fremde Seite im Namen eines angemeldeten
Admins Zahlungen eintragen. Ein Kunde kommt nur an die eigenen Server; das
Raten einer Nummer bringt nichts.

**Bevor das öffentlich erreichbar ist**, gehören zwei Dinge davor: **HTTPS**
(z. B. Caddy oder nginx als Reverse Proxy) und ein **Backup** der
`daten/portal.db`. Ohne HTTPS gehen Passwörter im Klartext über das Netz.

## Test

`test/durchklicken.mjs` geht das Portal einmal komplett durch — Anfrage
abschicken, annehmen, Server anlegen, zweimal zahlen, Dokumente drucken, als
Kunde anmelden, an fremde Daten wollen. 33 Prüfungen.

## Was noch fehlt

Ehrlich gesagt: die eigentliche **Serversteuerung**. Start, Stopp, Neustart,
Konsole und Dateiverwaltung laufen bei euch über das Hosting-Panel, und das
Portal weiß davon nichts. Das hier ist die kaufmännische Seite — Kunden,
Verträge, Zahlungen, Papiere. Eine Anbindung ans Panel wäre der nächste
Schritt, dafür brauche ich aber Zugriff auf dessen API.
