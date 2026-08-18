# Test

`durchklicken.mjs` geht das Portal einmal komplett durch wie ein echter
Benutzer: Startseite, Anfrage abschicken, anmelden, Anfrage annehmen,
Server anlegen, zweimal Zahlung eintragen, Dokumente drucken, als Kunde
anmelden, an fremde Daten wollen.

```
rm -f /tmp/portal-test.db*
PORT=3111 DB=/tmp/portal-test.db node start.js > /tmp/portal.log 2>&1 &
sleep 2
ADMINPW=$(grep -oP 'Passwort:\s+\K\S+' /tmp/portal.log) node test/durchklicken.mjs
```
