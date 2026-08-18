#!/usr/bin/env node
/**
 * Lemon Hosting Kundenportal - starten.
 *
 *   node start.js                 auf Port 3000
 *   PORT=8080 node start.js       auf einem anderen Port
 *
 * Beim allerersten Start wird ein Admin-Zugang angelegt und einmal im
 * Terminal ausgegeben. Danach steht das Passwort nirgends mehr - notier
 * es dir und aendere es im Portal.
 */
import { starte } from './src/server.js';
import * as db from './src/db.js';
import { randomBytes } from 'node:crypto';

const port = Number(process.env.PORT) || 3000;
const datei = process.env.DB || 'daten/portal.db';

starte(port, datei);

if (db.kunden().length === 0) {
  const passwort = randomBytes(9).toString('base64url');
  db.kundeAnlegen({
    benutzername: 'admin', passwort, vorname: 'Lemon', nachname: 'Hosting',
    rolle: 'admin',
  });
  db.protokolliere('System', 'Admin-Zugang angelegt', 'erster Start');
  console.log('  ┌──────────────────────────────────────────┐');
  console.log('  │  Erster Start - Admin-Zugang angelegt    │');
  console.log('  ├──────────────────────────────────────────┤');
  console.log(`  │  Benutzer:  admin                        │`);
  console.log(`  │  Passwort:  ${passwort.padEnd(28)} │`);
  console.log('  └──────────────────────────────────────────┘');
  console.log('  Notier es dir - es wird nicht noch einmal angezeigt.\n');
}
