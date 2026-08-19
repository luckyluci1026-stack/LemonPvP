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
      docker_bild   TEXT NOT NULL DEFAULT '',
      knoten_id     INTEGER NOT NULL DEFAULT 0,
      art           TEXT NOT NULL DEFAULT '',
      mc_version    TEXT NOT NULL DEFAULT '',
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

    CREATE TABLE IF NOT EXISTS unterbenutzer (
      server_id     INTEGER NOT NULL REFERENCES server(id),
      kunde_id      INTEGER NOT NULL REFERENCES kunden(id),
      rechte        TEXT NOT NULL DEFAULT '',
      angelegt      TEXT NOT NULL,
      PRIMARY KEY (server_id, kunde_id)
    );

    CREATE TABLE IF NOT EXISTS knoten (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      name          TEXT NOT NULL,
      adresse       TEXT NOT NULL,
      geheim        TEXT NOT NULL,
      notiz         TEXT NOT NULL DEFAULT '',
      angelegt      TEXT NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_server_kunde ON server(kunde_id);
    CREATE INDEX IF NOT EXISTS idx_server_knoten ON server(knoten_id);
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
  for (const spalte of ['neustart_um', 'sicherung_um', 'docker_bild',
                        'art', 'mc_version']) {
    if (!spalten.includes(spalte)) {
      db.exec(`ALTER TABLE server ADD COLUMN ${spalte} TEXT NOT NULL DEFAULT ''`);
    }
  }
  if (!spalten.includes('knoten_id')) {
    db.exec('ALTER TABLE server ADD COLUMN knoten_id INTEGER NOT NULL DEFAULT 0');
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

/**
 * Der erste Port ab 25565, den auf diesem Knoten noch kein Server hat.
 *
 * Je Knoten gezaehlt: Zwei Server auf verschiedenen Maschinen duerfen
 * denselben Port haben, sie kommen sich ja nicht in die Quere.
 */
export function naechsterPort(knotenId = 0, ab = 25565) {
  const belegt = new Set(db.prepare('SELECT port FROM server WHERE knoten_id = ?')
    .all(knotenId).map((s) => s.port));
  let port = ab;
  while (belegt.has(port)) port++;
  return port;
}

// ---------------------------------------------------------------- Knoten

export function knotenAnlegen({ name, adresse, geheim, notiz = '' }) {
  const info = db.prepare(`INSERT INTO knoten (name, adresse, geheim, notiz, angelegt)
      VALUES (?, ?, ?, ?, ?)`).run(name, adresse, geheim, notiz, jetzt());
  return Number(info.lastInsertRowid);
}

export const knoten = (id) => db.prepare('SELECT * FROM knoten WHERE id = ?').get(id);
export const alleKnoten = () => db.prepare('SELECT * FROM knoten ORDER BY name').all();

export function knotenAendern(id, felder) {
  const erlaubt = ['name', 'adresse', 'geheim', 'notiz'];
  const setzen = gesetzte(erlaubt, felder);
  if (!setzen.length) return;
  db.prepare(`UPDATE knoten SET ${setzen.map((f) => `${f} = ?`).join(', ')} WHERE id = ?`)
    .run(...setzen.map((f) => felder[f]), id);
}

/**
 * Einen Knoten loeschen - aber nur, wenn kein Server mehr darauf liegt.
 *
 * Sonst zeigten Server auf eine Maschine, die es im Portal nicht mehr
 * gibt, und niemand kaeme mehr an sie heran.
 */
export function knotenLoeschen(id) {
  const drauf = db.prepare('SELECT COUNT(*) AS n FROM server WHERE knoten_id = ?').get(id);
  if (drauf.n > 0) return `Auf diesem Knoten liegen noch ${drauf.n} Server.`;
  db.prepare('DELETE FROM knoten WHERE id = ?').run(id);
  return null;
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
                                pterodactyl = '', port = 0, knotenId = 0,
                                notiz = '', zusatz = {} }) {
  const info = db.prepare(`INSERT INTO server
      (kunde_id, name, subdomain, paket, software, status, port, pterodactyl,
       knoten_id, angelegt, notiz)
      VALUES (?, ?, ?, ?, ?, 'aktiv', ?, ?, ?, ?, ?)`)
    .run(kundeId, name, subdomain, paket, software,
         port || naechsterPort(knotenId), pterodactyl, knotenId, jetzt(), notiz);
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
                   'pterodactyl', 'neustart_um', 'sicherung_um', 'docker_bild',
                   'knoten_id', 'art', 'mc_version', 'notiz'];
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
  // Freigaben mitnehmen: Ein geloeschter Server soll bei niemandem mehr
  // in der Liste stehen.
  db.prepare('DELETE FROM unterbenutzer WHERE server_id = ?').run(id);
}

// ------------------------------------------------------------ Unterbenutzer

/**
 * Wer darf ausser dem Besitzer an diesen Server?
 *
 * In einer Klasse verwaltet selten nur einer den Server. Ohne
 * Unterbenutzer muesste man das Passwort weitergeben - und dann kann der
 * andere auch die Rechnung sehen und den Server loeschen lassen.
 *
 * Die Rechte stehen als Liste in einem Textfeld. Eine eigene Tabelle
 * dafuer waere sauberer normalisiert und hier trotzdem Unfug: Es sind
 * eine Handvoll fester Worte, die immer zusammen gelesen werden.
 */
export function unterbenutzerSetzen(serverId, kundeId, rechte) {
  const text = [...new Set(rechte)].join(',');
  db.prepare(`INSERT INTO unterbenutzer (server_id, kunde_id, rechte, angelegt)
      VALUES (?, ?, ?, ?)
      ON CONFLICT(server_id, kunde_id) DO UPDATE SET rechte = excluded.rechte`)
    .run(serverId, kundeId, text, jetzt());
}

export const unterbenutzerWeg = (serverId, kundeId) =>
  db.prepare('DELETE FROM unterbenutzer WHERE server_id = ? AND kunde_id = ?')
    .run(serverId, kundeId);

export function unterbenutzer(serverId) {
  return db.prepare(`SELECT u.*, k.benutzername, k.vorname, k.nachname, k.klasse
                     FROM unterbenutzer u JOIN kunden k ON k.id = u.kunde_id
                     WHERE u.server_id = ? ORDER BY k.benutzername`)
    .all(serverId).map((u) => ({ ...u, rechte: u.rechte ? u.rechte.split(',') : [] }));
}

/** Die Rechte eines Kunden an einem Server - oder null, wenn er keine hat. */
export function rechteAn(serverId, kundeId) {
  const u = db.prepare('SELECT rechte FROM unterbenutzer WHERE server_id = ? AND kunde_id = ?')
    .get(serverId, kundeId);
  return u ? (u.rechte ? u.rechte.split(',') : []) : null;
}

/** Alle Server, an denen jemand als Unterbenutzer beteiligt ist. */
export function serverAlsUnterbenutzer(kundeId) {
  return db.prepare(`SELECT s.* FROM server s
                     JOIN unterbenutzer u ON u.server_id = s.id
                     WHERE u.kunde_id = ? AND s.status <> 'geloescht'
                     ORDER BY s.name`)
    .all(kundeId).map((s) => ({ ...s, zusatz: zusatzVon(s.id), geteilt: true }));
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
