/**
 * Umstieg: eine alte Datenbank oeffnen, ohne dass etwas kaputtgeht.
 *
 * Diese Reihe gibt es, weil genau das einmal schiefgegangen ist. Der
 * Index `idx_server_knoten` stand im selben Block wie die Tabellen, also
 * VOR dem Nachruesten - auf einer frischen Datenbank ging das gut, weil
 * CREATE TABLE die Spalte gleich mitanlegt, und auf einer vorhandenen
 * startete das Portal mit "no such column: knoten_id" gar nicht mehr.
 *
 * Alle anderen Reihen legen sich eine frische Datenbank an. Sie konnten
 * das deshalb nicht finden - und werden es auch beim naechsten Mal nicht
 * koennen. Hier wird stattdessen jede Fassung nachgebaut, die es je
 * gab, mit Daten gefuellt und dann mit dem heutigen Code geoeffnet.
 *
 * Die alten Schemata stehen absichtlich als Text hier drin und werden
 * nicht aus der Git-Geschichte geholt: Ein Test, der auf bestimmte
 * Commits zeigt, geht kaputt, sobald jemand die Geschichte umschreibt -
 * und dann sucht man am falschen Ende.
 *
 *   node test/umstieg.mjs
 */
import { DatabaseSync } from 'node:sqlite';
import { mkdtempSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const arbeit = mkdtempSync(join(tmpdir(), 'umstieg-'));
process.on('exit', () => rmSync(arbeit, { recursive: true, force: true }));

let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

/** Was jede Fassung immer schon hatte. */
const GEMEINSAM = `
  CREATE TABLE kunden (
    id INTEGER PRIMARY KEY AUTOINCREMENT, benutzername TEXT NOT NULL UNIQUE,
    vorname TEXT NOT NULL DEFAULT '', nachname TEXT NOT NULL DEFAULT '',
    klasse TEXT NOT NULL DEFAULT '', mcname TEXT NOT NULL DEFAULT '',
    kontakt TEXT NOT NULL DEFAULT '', rolle TEXT NOT NULL DEFAULT 'kunde',
    passwort TEXT NOT NULL, salz TEXT NOT NULL, angelegt TEXT NOT NULL);
  CREATE TABLE server_zusatz (
    server_id INTEGER NOT NULL, zusatz TEXT NOT NULL,
    menge INTEGER NOT NULL DEFAULT 1, PRIMARY KEY (server_id, zusatz));
  CREATE TABLE bestellungen (
    id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER,
    vorname TEXT NOT NULL DEFAULT '', nachname TEXT NOT NULL DEFAULT '',
    klasse TEXT NOT NULL DEFAULT '', mcname TEXT NOT NULL DEFAULT '',
    kontakt TEXT NOT NULL DEFAULT '', servername TEXT NOT NULL DEFAULT '',
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', zusatz TEXT NOT NULL DEFAULT '{}',
    wunsch TEXT NOT NULL DEFAULT '', status TEXT NOT NULL DEFAULT 'offen',
    angelegt TEXT NOT NULL, erledigt_am TEXT);
  CREATE TABLE sitzungen (
    token TEXT PRIMARY KEY, kunde_id INTEGER NOT NULL, laeuft_ab TEXT NOT NULL);
  CREATE TABLE protokoll (
    id INTEGER PRIMARY KEY AUTOINCREMENT, wann TEXT NOT NULL, wer TEXT NOT NULL,
    was TEXT NOT NULL, details TEXT NOT NULL DEFAULT '');
`;

/**
 * Die Server-Tabelle, wie sie in den einzelnen Fassungen aussah.
 *
 * Der Reihe nach, wie sie entstanden sind. Die erste ist die
 * unangenehmste: Ihr fehlt am meisten, und sie hat mit `bezahlt_bis`
 * noch eine Spalte, die es heute gar nicht mehr gibt.
 */
const FASSUNGEN = [
  ['die allererste',
   `id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER NOT NULL, name TEXT NOT NULL,
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', status TEXT NOT NULL DEFAULT 'aktiv',
    bezahlt_bis TEXT, angelegt TEXT NOT NULL, geloescht_am TEXT,
    notiz TEXT NOT NULL DEFAULT ''`],
  ['mit Panel, ohne Zahlungen',
   `id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER NOT NULL, name TEXT NOT NULL,
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', status TEXT NOT NULL DEFAULT 'aktiv',
    pterodactyl TEXT NOT NULL DEFAULT '', angelegt TEXT NOT NULL, geloescht_am TEXT,
    notiz TEXT NOT NULL DEFAULT ''`],
  ['mit eigenem Port',
   `id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER NOT NULL, name TEXT NOT NULL,
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', status TEXT NOT NULL DEFAULT 'aktiv',
    port INTEGER NOT NULL DEFAULT 0, pterodactyl TEXT NOT NULL DEFAULT '',
    angelegt TEXT NOT NULL, geloescht_am TEXT, notiz TEXT NOT NULL DEFAULT ''`],
  ['mit Zeitplan',
   `id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER NOT NULL, name TEXT NOT NULL,
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', status TEXT NOT NULL DEFAULT 'aktiv',
    port INTEGER NOT NULL DEFAULT 0, pterodactyl TEXT NOT NULL DEFAULT '',
    neustart_um TEXT NOT NULL DEFAULT '', sicherung_um TEXT NOT NULL DEFAULT '',
    angelegt TEXT NOT NULL, geloescht_am TEXT, notiz TEXT NOT NULL DEFAULT ''`],
  ['mit Docker',
   `id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER NOT NULL, name TEXT NOT NULL,
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', status TEXT NOT NULL DEFAULT 'aktiv',
    port INTEGER NOT NULL DEFAULT 0, pterodactyl TEXT NOT NULL DEFAULT '',
    neustart_um TEXT NOT NULL DEFAULT '', sicherung_um TEXT NOT NULL DEFAULT '',
    docker_bild TEXT NOT NULL DEFAULT '', angelegt TEXT NOT NULL, geloescht_am TEXT,
    notiz TEXT NOT NULL DEFAULT ''`],
  ['mit Knoten',
   `id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER NOT NULL, name TEXT NOT NULL,
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', status TEXT NOT NULL DEFAULT 'aktiv',
    port INTEGER NOT NULL DEFAULT 0, pterodactyl TEXT NOT NULL DEFAULT '',
    neustart_um TEXT NOT NULL DEFAULT '', sicherung_um TEXT NOT NULL DEFAULT '',
    docker_bild TEXT NOT NULL DEFAULT '', knoten_id INTEGER NOT NULL DEFAULT 0,
    angelegt TEXT NOT NULL, geloescht_am TEXT, notiz TEXT NOT NULL DEFAULT ''`],
  ['mit Serversoftware',
   `id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER NOT NULL, name TEXT NOT NULL,
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', status TEXT NOT NULL DEFAULT 'aktiv',
    port INTEGER NOT NULL DEFAULT 0, pterodactyl TEXT NOT NULL DEFAULT '',
    neustart_um TEXT NOT NULL DEFAULT '', sicherung_um TEXT NOT NULL DEFAULT '',
    docker_bild TEXT NOT NULL DEFAULT '', knoten_id INTEGER NOT NULL DEFAULT 0,
    art TEXT NOT NULL DEFAULT '', mc_version TEXT NOT NULL DEFAULT '',
    angelegt TEXT NOT NULL, geloescht_am TEXT, notiz TEXT NOT NULL DEFAULT ''`],
  ['mit Startup-Flaggen',
   `id INTEGER PRIMARY KEY AUTOINCREMENT, kunde_id INTEGER NOT NULL, name TEXT NOT NULL,
    subdomain TEXT NOT NULL DEFAULT '', paket TEXT NOT NULL,
    software TEXT NOT NULL DEFAULT 'Paper', status TEXT NOT NULL DEFAULT 'aktiv',
    port INTEGER NOT NULL DEFAULT 0, pterodactyl TEXT NOT NULL DEFAULT '',
    neustart_um TEXT NOT NULL DEFAULT '', sicherung_um TEXT NOT NULL DEFAULT '',
    docker_bild TEXT NOT NULL DEFAULT '', knoten_id INTEGER NOT NULL DEFAULT 0,
    art TEXT NOT NULL DEFAULT '', mc_version TEXT NOT NULL DEFAULT '',
    startbefehl TEXT NOT NULL DEFAULT '', start_flaggen TEXT,
    jar_datei TEXT NOT NULL DEFAULT '', angelegt TEXT NOT NULL, geloescht_am TEXT,
    notiz TEXT NOT NULL DEFAULT ''`],
];

/** Was der heutige Code an der Server-Tabelle voraussetzt. */
const GEBRAUCHT = ['port', 'pterodactyl', 'neustart_um', 'sicherung_um', 'docker_bild',
                   'knoten_id', 'art', 'mc_version', 'startbefehl', 'start_flaggen',
                   'jar_datei'];

const db = await import('../src/db.js');

for (const [name, spalten] of FASSUNGEN) {
  const pfad = join(arbeit, name.replace(/\W+/g, '_') + '.db');

  // Die alte Datenbank nachbauen und mit Daten fuellen - leer umzuziehen
  // waere die halbe Probe.
  const alt = new DatabaseSync(pfad);
  alt.exec(GEMEINSAM);
  alt.exec(`CREATE TABLE server (${spalten});`);
  alt.prepare(`INSERT INTO kunden (benutzername, vorname, nachname, passwort, salz, angelegt)
               VALUES ('lea.berg', 'Lea', 'Berg', 'x', 'y', '2026-01-01T00:00:00.000Z')`).run();
  const felder = spalten.split(',').map((s) => s.trim().split(/\s+/)[0])
    .filter((f) => !['id', 'geloescht_am', 'bezahlt_bis', 'start_flaggen'].includes(f));
  const werte = felder.map((f) => f === 'kunde_id' ? 1
    : f === 'name' ? "'Klassenserver'" : f === 'paket' ? "'coal'"
    : f === 'angelegt' ? "'2026-01-01T00:00:00.000Z'"
    : f === 'port' ? 25565 : f === 'knoten_id' ? 0 : "''");
  alt.prepare(`INSERT INTO server (${felder.join(', ')}) VALUES (${werte.join(', ')})`).run();
  alt.close();

  // Und jetzt mit dem heutigen Code oeffnen.
  let aufgegangen = null;
  try { db.oeffne(pfad); } catch (f) { aufgegangen = f.message; }
  ok(`Fassung „${name}" lässt sich öffnen`, aufgegangen === null, aufgegangen || '');
  if (aufgegangen) continue;

  const da = db.roh().prepare('PRAGMA table_info(server)').all().map((s) => s.name);
  const fehlt = GEBRAUCHT.filter((s) => !da.includes(s));
  ok('  … und hat danach alle Spalten', fehlt.length === 0, fehlt.join(', ') || '');

  const s = db.server(1);
  ok('  … die Daten sind noch da', s?.name === 'Klassenserver',
     s ? `#${s.id} ${s.name}, Port ${s.port}` : 'Server weg!');
  ok('  … der Kunde auch', db.kundePerName('lea.berg')?.vorname === 'Lea');
  ok('  … und jeder Server hat einen Port', Number(s?.port) >= 1024, 'Port ' + s?.port);

  // Der Index, an dem es hing.
  const indizes = db.roh().prepare("SELECT name FROM sqlite_master WHERE type = 'index'")
    .all().map((i) => i.name);
  ok('  … der Knoten-Index steht', indizes.includes('idx_server_knoten'));

  // Zweimal oeffnen muss auch gehen - beim naechsten Start passiert
  // genau das.
  let nochmal = null;
  try { db.oeffne(pfad); } catch (f) { nochmal = f.message; }
  ok('  … und ein zweiter Start ändert nichts mehr', nochmal === null, nochmal || '');
}

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
