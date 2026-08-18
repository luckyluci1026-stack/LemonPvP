/**
 * Die Datenbank.
 *
 * SQLite, und zwar das in Node eingebaute - dadurch braucht das Portal
 * kein einziges npm-Paket. Starten, laeuft.
 *
 * Zwei Dinge sind hier bewusst so gebaut:
 *
 * 1. Jede Zahlung landet zusaetzlich im Protokoll, mit Zeitpunkt und
 *    Person. Bei Bargeld in der Schule ist Nachvollziehbarkeit das
 *    Wichtigste ueberhaupt - wer wann was kassiert hat, muss man spaeter
 *    noch nachlesen koennen, auch wenn jemand einen Betrag korrigiert.
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
      bezahlt_bis   TEXT,
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

    CREATE TABLE IF NOT EXISTS zahlungen (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      server_id     INTEGER NOT NULL REFERENCES server(id),
      betrag        REAL NOT NULL,
      von           TEXT NOT NULL,
      bis           TEXT NOT NULL,
      art           TEXT NOT NULL DEFAULT 'Bar',
      kassiert_von  TEXT NOT NULL DEFAULT '',
      erfasst_am    TEXT NOT NULL,
      notiz         TEXT NOT NULL DEFAULT ''
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
    CREATE INDEX IF NOT EXISTS idx_zahlung_server ON zahlungen(server_id);
  `);
}

export const jetzt = () => new Date().toISOString();
export const heute = () => new Date().toISOString().slice(0, 10);

/** Tage zwischen heute und einem ISO-Datum. Negativ = liegt zurueck. */
export function tageBis(datum) {
  if (!datum) return null;
  const ziel = new Date(datum + 'T00:00:00Z').getTime();
  const jetztTag = new Date(heute() + 'T00:00:00Z').getTime();
  return Math.round((ziel - jetztTag) / 86400000);
}

export function plusTage(datum, tage) {
  const d = new Date((datum || heute()) + 'T00:00:00Z');
  d.setUTCDate(d.getUTCDate() + tage);
  return d.toISOString().slice(0, 10);
}

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

export function kundeAendern(id, felder) {
  const erlaubt = ['vorname', 'nachname', 'klasse', 'mcname', 'kontakt', 'rolle'];
  const setzen = erlaubt.filter((f) => f in felder);
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
                                bezahltBis = null, notiz = '', zusatz = {} }) {
  const info = db.prepare(`INSERT INTO server
      (kunde_id, name, subdomain, paket, software, status, bezahlt_bis, angelegt, notiz)
      VALUES (?, ?, ?, ?, ?, 'aktiv', ?, ?, ?)`)
    .run(kundeId, name, subdomain, paket, software, bezahltBis, jetzt(), notiz);
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
  const erlaubt = ['name', 'subdomain', 'paket', 'software', 'status', 'bezahlt_bis', 'notiz'];
  const setzen = erlaubt.filter((f) => f in felder);
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

// ---------------------------------------------------------------- Zahlungen

/**
 * Eine Zahlung eintragen.
 *
 * Der Zeitraum haengt an "bezahlt_bis": Wer verlaengert, faengt dort an,
 * wo der letzte Zeitraum aufhoert - nicht heute. Sonst verschenkt man bei
 * jeder verspaeteten Zahlung ein paar Tage.
 */
export function zahlungEintragen({ serverId, betrag, tage = 30, art = 'Bar',
                                   kassiertVon = '', notiz = '' }) {
  const s = server(serverId);
  if (!s) return null;
  const start = s.bezahlt_bis && tageBis(s.bezahlt_bis) > 0 ? s.bezahlt_bis : heute();
  const bis = plusTage(start, tage);
  db.prepare(`INSERT INTO zahlungen
      (server_id, betrag, von, bis, art, kassiert_von, erfasst_am, notiz)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
    .run(serverId, betrag, start, bis, art, kassiertVon, jetzt(), notiz);
  db.prepare("UPDATE server SET bezahlt_bis = ?, status = CASE WHEN status = 'archiviert' "
           + "THEN 'aktiv' ELSE status END WHERE id = ?").run(bis, serverId);
  return { von: start, bis };
}

export const zahlungenVon = (serverId) =>
  db.prepare('SELECT * FROM zahlungen WHERE server_id = ? ORDER BY id DESC').all(serverId);

export const alleZahlungen = (grenze = 200) =>
  db.prepare(`SELECT z.*, s.name AS servername, k.benutzername
              FROM zahlungen z
              JOIN server s ON s.id = z.server_id
              JOIN kunden k ON k.id = s.kunde_id
              ORDER BY z.id DESC LIMIT ?`).all(grenze);

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
