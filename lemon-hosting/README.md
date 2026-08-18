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
