#!/usr/bin/env node
/**
 * Lemon Hosting Kundenportal - starten.
 *
 *   node start.js                    auf Port 3000
 *   PORT=8080 node start.js          auf einem anderen Port
 *   DB=/pfad/portal.db node start.js andere Datenbankdatei
 *
 * Beim allerersten Start wird ein Admin-Zugang angelegt und einmal im
 * Terminal ausgegeben. Danach steht das Passwort nirgends mehr - notier
 * es dir und aendere es im Portal.
 */

// Node bringt SQLite erst ab 22.5 mit. Ohne diese Pruefung bekaeme man
// beim Start nur ein "Cannot find module 'node:sqlite'" um die Ohren und
// wuesste nicht, woran es liegt.
const [gross, klein] = process.versions.node.split('.').map(Number);
if (gross < 22 || (gross === 22 && klein < 5)) {
  console.error(`
  Dieses Portal braucht Node 22.5 oder neuer.
  Du hast Node ${process.versions.node}.

  Der Grund: die Datenbank läuft über das in Node eingebaute SQLite, und
  das gibt es erst ab 22.5. Dafür braucht das Portal sonst kein einziges
  Paket.

  Neuere Version holen:  https://nodejs.org
`);
  process.exit(1);
}

// Node meldet SQLite als "experimental". Das ist richtig, sieht im
// Terminal aber aus wie ein Fehler und verunsichert nur - deshalb still.
const warnenAlt = process.emitWarning;
process.emitWarning = (warnung, ...rest) => {
  if (String(warnung).includes('SQLite is an experimental feature')) return;
  return warnenAlt.call(process, warnung, ...rest);
};

const { starte } = await import('./src/server.js');
const db = await import('./src/db.js');
const { randomBytes } = await import('node:crypto');

const port = Number(process.env.PORT) || 3000;
const datei = process.env.DB || 'daten/portal.db';

const server = starte(port, datei);

// Der Admin-Zugang kommt bewusst ganz zum Schluss: Er ist das Einzige,
// was man sich merken muss, und soll deshalb die letzte Zeile im Terminal
// sein. Deshalb erst, wenn der Server wirklich lauscht - sonst steht er
// oben und scrollt weg.
server.on('listening', () => {
  if (db.kunden().length !== 0) return;
  const passwort = randomBytes(9).toString('base64url');
  db.kundeAnlegen({
    benutzername: 'admin', passwort, vorname: 'Lemon', nachname: 'Hosting',
    rolle: 'admin',
  });
  db.protokolliere('System', 'Admin-Zugang angelegt', 'erster Start');
  console.log('  ┌─────────────────────────────────────────────┐');
  console.log('  │  Erster Start – so kommst du rein:          │');
  console.log('  ├─────────────────────────────────────────────┤');
  console.log('  │  Benutzer   admin                           │');
  console.log(`  │  Passwort   ${passwort.padEnd(32)}│`);
  console.log('  └─────────────────────────────────────────────┘');
  console.log('  Notier es dir – es wird nicht noch einmal angezeigt.\n');
});
