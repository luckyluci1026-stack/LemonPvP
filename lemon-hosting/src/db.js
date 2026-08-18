/**
 * Die Datenbank.
 *
 * SQLite, und zwar das in Node eingebaute - dadurch braucht das Portal
 * kein einziges npm-Paket. Starten, laeuft.
 *
 * Zwei Dinge sind hier bewusst so gebaut:
 *
 * 1. Das Portal fuehrt keine Buchhaltung. Bezahlt wird in der Schule,
 *    von Hand, gegen Bestellbogen - deshalb gibt es hier keine Betraege
 *    und keine Zahlungstabelle. Was das Portal kann, ist Server
 *    verwalten und laufen lassen; was Geld kostet, steht auf Papier.
 *
 * 2. Server werden nie wirklich aus der Tabelle geloescht, sondern
 *    bekommen den Status "geloescht". Sonst waere die Loeschbestaetigung
 *    ein Dokument ueber etwas, das es nicht mehr gibt.
 */

import { DatabaseSync } from 'node:sqlite';
import { randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';

let db;

export function oeffne(pfad) {
  mkdirSync(dirname(pfad), { recursive: true });
  db = new DatabaseSync(pfad);
  db.exec('PRAGMA journal_mode = WAL');
  db.exec('PRAGMA foreign_keys = ON');
  schema();
  return db;
}

function schema() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS kunden (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      benutzername  TEXT NOT NULL UNIQUE,
      vorname       TEXT NOT NULL DEFAULT '',
      nachname      TEXT NOT NULL DEFAULT '',
      klasse        TEXT NOT NULL DEFAULT '',
      mcname        TEXT NOT NULL DEFAULT '',
      kontakt       TEXT NOT NULL DEFAULT '',
      rolle         TEXT NOT NULL DEFAULT 'kunde',
      passwort      TEXT NOT NULL,
      salz          TEXT NOT NULL,
      angelegt      TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS server (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      kunde_id      INTEGER NOT NULL REFERENCES kunden(id),
      name          TEXT NOT NULL,
      subdomain     TEXT NOT NULL DEFAULT '',
      paket         TEXT NOT NULL,
      software      TEXT NOT NULL DEFAULT 'Paper',
      status        TEXT NOT NULL DEFAULT 'aktiv',
      port          INTEGER NOT NULL DEFAULT 0,
      pterodactyl   TEXT NOT NULL DEFAULT '',
      neustart_um   TEXT NOT NULL DEFAULT '',
      sicherung_um  TEXT NOT NULL DEFAULT '',
      angelegt      TEXT NOT NULL,
      geloescht_am  TEXT,
      notiz         TEXT NOT NULL DEFAULT ''
    );

    CREATE TABLE IF NOT EXISTS server_zusatz (
      server_id     INTEGER NOT NULL REFERENCES server(id),
      zusatz        TEXT NOT NULL,
      menge         INTEGER NOT NULL DEFAULT 1,
      PRIMARY KEY (server_id, zusatz)
    );

    CREATE TABLE IF NOT EXISTS bestellungen (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      kunde_id      INTEGER REFERENCES kunden(id),
      vorname       TEXT NOT NULL DEFAULT '',
      nachname      TEXT NOT NULL DEFAULT '',
      klasse        TEXT NOT NULL DEFAULT '',
      mcname        TEXT NOT NULL DEFAULT '',
      kontakt       TEXT NOT NULL DEFAULT '',
      servername    TEXT NOT NULL DEFAULT '',
      subdomain     TEXT NOT NULL DEFAULT '',
      paket         TEXT NOT NULL,
      software      TEXT NOT NULL DEFAULT 'Paper',
      zusatz        TEXT NOT NULL DEFAULT '{}',
      wunsch        TEXT NOT NULL DEFAULT '',
      status        TEXT NOT NULL DEFAULT 'offen',
      angelegt      TEXT NOT NULL,
      erledigt_am   TEXT
    );

    CREATE TABLE IF NOT EXISTS sitzungen (
      token         TEXT PRIMARY KEY,
      kunde_id      INTEGER NOT NULL REFERENCES kunden(id),
      laeuft_ab     TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS protokoll (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      wann          TEXT NOT NULL,
      wer           TEXT NOT NULL,
      was           TEXT NOT NULL,
      details       TEXT NOT NULL DEFAULT ''
    );

    CREATE INDEX IF NOT EXISTS idx_server_kunde ON server(kunde_id);
  `);
  nachruesten();
}

/**
 * Spalten nachziehen, die es in aelteren Datenbanken noch nicht gab.
 *
 * CREATE TABLE IF NOT EXISTS aendert eine vorhandene Tabelle nicht - wer
 * das Portal schon benutzt hat, braeuchte sonst eine neue Datenbank.
 */
function nachruesten() {
  const spalten = db.prepare('PRAGMA table_info(server)').all().map((s) => s.name);
  if (!spalten.includes('pterodactyl')) {
    db.exec("ALTER TABLE server ADD COLUMN pterodactyl TEXT NOT NULL DEFAULT ''");
  }
  for (const spalte of ['neustart_um', 'sicherung_um']) {
    if (!spalten.includes(spalte)) {
      db.exec(`ALTER TABLE server ADD COLUMN ${spalte} TEXT NOT NULL DEFAULT ''`);
    }
  }
  if (!spalten.includes('port')) {
    db.exec('ALTER TABLE server ADD COLUMN port INTEGER NOT NULL DEFAULT 0');
    // Vorhandene Server bekommen der Reihe nach einen Port, sonst
    // wollten nach dem Update alle auf dieselbe 25565.
    for (const s of db.prepare('SELECT id FROM server ORDER BY id').all()) {
      db.prepare('UPDATE server SET port = ? WHERE id = ?').run(naechsterPort(), s.id);
    }
  }
}

/** Der erste Port ab 25565, den noch kein Server hat. */
export function naechsterPort(ab = 25565) {
  const belegt = new Set(db.prepare('SELECT port FROM server').all().map((s) => s.port));
  let port = ab;
  while (belegt.has(port)) port++;
  return port;
}

export const jetzt = () => new Date().toISOString();
export const heute = () => new Date().toISOString().slice(0, 10);

// ---------------------------------------------------------------- Passwoerter

export function hashe(passwort) {
  const salz = randomBytes(16).toString('hex');
  const hash = scryptSync(passwort, salz, 64).toString('hex');
  return { hash, salz };
}

export function passtPasswort(passwort, hash, salz) {
  try {
    const versuch = scryptSync(passwort, salz, 64);
    const echt = Buffer.from(hash, 'hex');
    return versuch.length === echt.length && timingSafeEqual(versuch, echt);
  } catch {
    return false;
  }
}

// ---------------------------------------------------------------- Protokoll

export function protokolliere(wer, was, details = '') {
  db.prepare('INSERT INTO protokoll (wann, wer, was, details) VALUES (?, ?, ?, ?)')
    .run(jetzt(), wer, was, details);
}

export function protokollListe(grenze = 100) {
  return db.prepare('SELECT * FROM protokoll ORDER BY id DESC LIMIT ?').all(grenze);
}

// ---------------------------------------------------------------- Kunden

export function kundeAnlegen({ benutzername, passwort, vorname = '', nachname = '',
                               klasse = '', mcname = '', kontakt = '', rolle = 'kunde' }) {
  const { hash, salz } = hashe(passwort);
  const info = db.prepare(`INSERT INTO kunden
      (benutzername, vorname, nachname, klasse, mcname, kontakt, rolle, passwort, salz, angelegt)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`)
    .run(benutzername.toLowerCase().trim(), vorname, nachname, klasse, mcname,
         kontakt, rolle, hash, salz, jetzt());
  return Number(info.lastInsertRowid);
}

export const kundePerName = (name) =>
  db.prepare('SELECT * FROM kunden WHERE benutzername = ?').get(String(name || '').toLowerCase().trim());
export const kunde = (id) => db.prepare('SELECT * FROM kunden WHERE id = ?').get(id);
export const kunden = () =>
  db.prepare('SELECT * FROM kunden ORDER BY nachname, vorname, benutzername').all();

/**
 * Nur die Felder anfassen, die wirklich mitgeschickt wurden.
 *
 * `f in felder` reicht dafuer nicht: Die Routen reichen ihre Werte als
 * `notiz: d.notiz` durch, und fehlt das Feld im Formular, steht dort
 * `undefined` - der Schluessel existiert trotzdem. SQLite kann damit
 * nichts anfangen und wirft, statt die Spalte einfach zu lassen. Im
 * Browser faellt das nie auf, weil ein Textfeld immer wenigstens einen
 * leeren String schickt; bei einer unvollstaendigen Anfrage sehr wohl.
 */
const gesetzte = (erlaubt, felder) =>
  erlaubt.filter((f) => felder[f] !== undefined && felder[f] !== null);

export function kundeAendern(id, felder) {
  const erlaubt = ['vorname', 'nachname', 'klasse', 'mcname', 'kontakt', 'rolle'];
  const setzen = gesetzte(erlaubt, felder);
  if (!setzen.length) return;
  db.prepare(`UPDATE kunden SET ${setzen.map((f) => `${f} = ?`).join(', ')} WHERE id = ?`)
    .run(...setzen.map((f) => felder[f]), id);
}

export function passwortSetzen(id, passwort) {
  const { hash, salz } = hashe(passwort);
  db.prepare('UPDATE kunden SET passwort = ?, salz = ? WHERE id = ?').run(hash, salz, id);
  db.prepare('DELETE FROM sitzungen WHERE kunde_id = ?').run(id);
}

// ---------------------------------------------------------------- Sitzungen

export function anmelden(kundeId, tage = 14) {
  const token = randomBytes(32).toString('hex');
  const ab = new Date();
  ab.setDate(ab.getDate() + tage);
  db.prepare('INSERT INTO sitzungen (token, kunde_id, laeuft_ab) VALUES (?, ?, ?)')
    .run(token, kundeId, ab.toISOString());
  return token;
}

export function sitzung(token) {
  if (!token) return null;
  const s = db.prepare('SELECT * FROM sitzungen WHERE token = ?').get(token);
  if (!s) return null;
  if (new Date(s.laeuft_ab) < new Date()) {
    db.prepare('DELETE FROM sitzungen WHERE token = ?').run(token);
    return null;
  }
  return kunde(s.kunde_id);
}

export const abmelden = (token) =>
  db.prepare('DELETE FROM sitzungen WHERE token = ?').run(token);

// ---------------------------------------------------------------- Server

export function serverAnlegen({ kundeId, name, subdomain = '', paket, software = 'Paper',
                                pterodactyl = '', port = 0, notiz = '', zusatz = {} }) {
  const info = db.prepare(`INSERT INTO server
      (kunde_id, name, subdomain, paket, software, status, port, pterodactyl, angelegt, notiz)
      VALUES (?, ?, ?, ?, ?, 'aktiv', ?, ?, ?, ?)`)
    .run(kundeId, name, subdomain, paket, software,
         port || naechsterPort(), pterodactyl, jetzt(), notiz);
  const id = Number(info.lastInsertRowid);
  zusatzSetzen(id, zusatz);
  return id;
}

export function zusatzSetzen(serverId, zusatz) {
  db.prepare('DELETE FROM server_zusatz WHERE server_id = ?').run(serverId);
  const einfuegen = db.prepare(
    'INSERT INTO server_zusatz (server_id, zusatz, menge) VALUES (?, ?, ?)');
  for (const [id, menge] of Object.entries(zusatz || {})) {
    const n = Math.floor(Number(menge) || 0);
    if (n > 0) einfuegen.run(serverId, id, n);
  }
}

export function zusatzVon(serverId) {
  const raus = {};
  for (const z of db.prepare('SELECT zusatz, menge FROM server_zusatz WHERE server_id = ?')
                     .all(serverId)) {
    raus[z.zusatz] = z.menge;
  }
  return raus;
}

export const server = (id) => {
  const s = db.prepare('SELECT * FROM server WHERE id = ?').get(id);
  return s ? { ...s, zusatz: zusatzVon(s.id) } : null;
};

export function serverVonKunde(kundeId) {
  return db.prepare('SELECT * FROM server WHERE kunde_id = ? ORDER BY status, name')
    .all(kundeId).map((s) => ({ ...s, zusatz: zusatzVon(s.id) }));
}

export function alleServer(mitGeloeschten = false) {
  const sql = mitGeloeschten
    ? 'SELECT * FROM server ORDER BY status, name'
    : "SELECT * FROM server WHERE status <> 'geloescht' ORDER BY status, name";
  return db.prepare(sql).all().map((s) => ({ ...s, zusatz: zusatzVon(s.id) }));
}

export function serverAendern(id, felder) {
  const erlaubt = ['name', 'subdomain', 'paket', 'software', 'status', 'port',
                   'pterodactyl', 'neustart_um', 'sicherung_um', 'notiz'];
  const setzen = gesetzte(erlaubt, felder);
  if (!setzen.length) return;
  db.prepare(`UPDATE server SET ${setzen.map((f) => `${f} = ?`).join(', ')} WHERE id = ?`)
    .run(...setzen.map((f) => felder[f]), id);
}

/**
 * Server als geloescht kennzeichnen - nicht aus der Tabelle werfen.
 *
 * Die Loeschbestaetigung muss hinterher noch Servername, Owner und
 * Zeitraum nennen koennen. Ein wirklich geloeschter Datensatz koennte das
 * nicht.
 */
export function serverLoeschen(id) {
  db.prepare("UPDATE server SET status = 'geloescht', geloescht_am = ? WHERE id = ?")
    .run(heute(), id);
}

// ---------------------------------------------------------------- Bestellungen

export function bestellungAnlegen(daten) {
  const info = db.prepare(`INSERT INTO bestellungen
      (kunde_id, vorname, nachname, klasse, mcname, kontakt, servername, subdomain,
       paket, software, zusatz, wunsch, status, angelegt)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'offen', ?)`)
    .run(daten.kundeId ?? null, daten.vorname || '', daten.nachname || '', daten.klasse || '',
         daten.mcname || '', daten.kontakt || '', daten.servername || '', daten.subdomain || '',
         daten.paket, daten.software || 'Paper', JSON.stringify(daten.zusatz || {}),
         daten.wunsch || '', jetzt());
  return Number(info.lastInsertRowid);
}

const bestellungAus = (b) => b && { ...b, zusatz: JSON.parse(b.zusatz || '{}') };

export const bestellung = (id) =>
  bestellungAus(db.prepare('SELECT * FROM bestellungen WHERE id = ?').get(id));

export const bestellungen = (nurOffene = false) =>
  db.prepare(`SELECT * FROM bestellungen ${nurOffene ? "WHERE status = 'offen'" : ''}
              ORDER BY id DESC`).all().map(bestellungAus);

export const bestellungenVonKunde = (kundeId) =>
  db.prepare('SELECT * FROM bestellungen WHERE kunde_id = ? ORDER BY id DESC')
    .all(kundeId).map(bestellungAus);

export const bestellungStatus = (id, status) =>
  db.prepare('UPDATE bestellungen SET status = ?, erledigt_am = ? WHERE id = ?')
    .run(status, heute(), id);

export const roh = () => db;
