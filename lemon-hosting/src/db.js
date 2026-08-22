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
      totp_geheim   TEXT,
      totp_seit     TEXT,
      totp_schritt  INTEGER NOT NULL DEFAULT 0,
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
      startbefehl   TEXT NOT NULL DEFAULT '',
      start_flaggen TEXT,
      jar_datei     TEXT NOT NULL DEFAULT '',
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
      laeuft_ab     TEXT NOT NULL,
      halb          INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS ersatzcode (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      kunde_id      INTEGER NOT NULL REFERENCES kunden(id),
      hash          TEXT NOT NULL,
      benutzt_am    TEXT
    );

    CREATE TABLE IF NOT EXISTS protokoll (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      wann          TEXT NOT NULL,
      wer           TEXT NOT NULL,
      was           TEXT NOT NULL,
      details       TEXT NOT NULL DEFAULT '',
      server_id     INTEGER
    );

    CREATE TABLE IF NOT EXISTS zugang (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      kunde_id      INTEGER NOT NULL REFERENCES kunden(id),
      name          TEXT NOT NULL DEFAULT '',
      hash          TEXT NOT NULL,
      angelegt      TEXT NOT NULL,
      zuletzt       TEXT
    );

    CREATE TABLE IF NOT EXISTS server_port (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      server_id     INTEGER NOT NULL REFERENCES server(id),
      port          INTEGER NOT NULL,
      protokoll     TEXT NOT NULL DEFAULT 'beide',
      notiz         TEXT NOT NULL DEFAULT ''
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

  `);

  nachruesten();

  /**
   * Die Indizes zuletzt - nach dem Nachruesten.
   *
   * Standen sie oben im selben Block, ging es auf einer frischen
   * Datenbank gut (CREATE TABLE legt die Spalte ja gleich mit an) und
   * auf einer vorhandenen kaputt: CREATE TABLE IF NOT EXISTS ruehrt eine
   * bestehende Tabelle nicht an, also fehlte `knoten_id` noch, wenn der
   * Index darauf angelegt werden sollte - und das Portal startete mit
   * "no such column: knoten_id" gar nicht mehr.
   *
   * Aufgefallen ist das erst bei jemandem, der das Portal schon benutzt
   * hatte. Genau deshalb prueft test/umstieg.mjs jetzt jede alte
   * Fassung, statt nur den frischen Fall.
   */
  db.exec(`
    CREATE INDEX IF NOT EXISTS idx_server_kunde ON server(kunde_id);
    CREATE INDEX IF NOT EXISTS idx_server_knoten ON server(knoten_id);
  `);
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
                        'art', 'mc_version', 'startbefehl', 'jar_datei']) {
    if (!spalten.includes(spalte)) {
      db.exec(`ALTER TABLE server ADD COLUMN ${spalte} TEXT NOT NULL DEFAULT ''`);
    }
  }
  // start_flaggen darf ausdruecklich NULL sein: leer heisst "keine
  // Flaggen", NULL heisst "die Standardflaggen". Ohne den Unterschied
  // koennte man die Flaggen nie ganz abschalten.
  if (!spalten.includes('start_flaggen')) {
    db.exec('ALTER TABLE server ADD COLUMN start_flaggen TEXT');
  }
  const kundenSpalten = db.prepare('PRAGMA table_info(kunden)').all().map((s) => s.name);
  if (!kundenSpalten.includes('totp_geheim')) {
    // NULL heisst "kein zweiter Faktor". Ein leerer Text waere hier
    // zweideutig - man saehe nicht, ob jemand ihn nie eingerichtet oder
    // gerade abgeschaltet hat.
    db.exec('ALTER TABLE kunden ADD COLUMN totp_geheim TEXT');
    db.exec('ALTER TABLE kunden ADD COLUMN totp_seit TEXT');
    db.exec('ALTER TABLE kunden ADD COLUMN totp_schritt INTEGER NOT NULL DEFAULT 0');
  }
  const sitzungsSpalten = db.prepare('PRAGMA table_info(sitzungen)').all()
    .map((s) => s.name);
  if (!sitzungsSpalten.includes('halb')) {
    db.exec('ALTER TABLE sitzungen ADD COLUMN halb INTEGER NOT NULL DEFAULT 0');
  }

  const protokollSpalten = db.prepare('PRAGMA table_info(protokoll)').all()
    .map((s) => s.name);
  if (!protokollSpalten.includes('server_id')) {
    db.exec('ALTER TABLE protokoll ADD COLUMN server_id INTEGER');
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
  const belegt = belegtePorts(knotenId);
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

/**
 * Etwas ins Protokoll schreiben.
 *
 * `serverId` ist neu und optional: Damit landet derselbe Eintrag sowohl
 * in der Gesamtliste des Teams als auch in der Aktivitaet des einzelnen
 * Servers. Wer wissen will, warum sein Server gestern nachts neu
 * gestartet ist, soll das an seinem Server nachlesen koennen und nicht
 * in einer Liste ueber alle Klassen.
 */
export function protokolliere(wer, was, details = '', serverId = null) {
  db.prepare(`INSERT INTO protokoll (wann, wer, was, details, server_id)
              VALUES (?, ?, ?, ?, ?)`)
    .run(jetzt(), wer, was, details, serverId);
}

export function protokollListe(grenze = 100) {
  return db.prepare('SELECT * FROM protokoll ORDER BY id DESC LIMIT ?').all(grenze);
}

/**
 * Was an diesem Server passiert ist.
 *
 * Alte Eintraege haben noch keine server_id - fuer die wird zusaetzlich
 * im Detailtext nach "#<id>" gesucht. So sind auch die Sachen von
 * vorgestern noch zu sehen, statt dass die Liste bei der Umstellung
 * anfaengt.
 */
export function protokollVonServer(serverId, grenze = 60) {
  return db.prepare(`SELECT * FROM protokoll
                     WHERE server_id = ?
                        OR (server_id IS NULL AND details LIKE ?)
                     ORDER BY id DESC LIMIT ?`)
    .all(serverId, `%#${serverId} %`, grenze);
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
 *
 * `null` wird dagegen ausdruecklich durchgelassen: Bei den Startflaggen
 * ist es der Unterschied zwischen "keine Flaggen" (leerer String) und
 * "die Vorgaben" (NULL). Es mit auszufiltern hiess, dass sich die
 * Vorgaben nie wieder herstellen liessen.
 */
const gesetzte = (erlaubt, felder) =>
  erlaubt.filter((f) => felder[f] !== undefined);

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

/**
 * Eine halbe Anmeldung: Das Passwort stimmt, der zweite Faktor fehlt.
 *
 * Sie steht in derselben Tabelle, ist aber durch `halb = 1` fuer alles
 * gesperrt, was Rechte braucht - jede Frage nach dem angemeldeten Nutzer
 * geht durch `sitzung()`, und die uebersieht halbe. So gibt es keinen
 * zweiten Ort, an dem Anmeldungen liegen, und keinen Zustand im
 * Arbeitsspeicher, der bei einem Neustart mitten in der Anmeldung
 * verschwindet.
 *
 * Zehn Minuten: lang genug, um das Handy zu suchen, kurz genug, dass ein
 * halb angemeldeter Rechner nicht den ganzen Tag so dasteht.
 */
export function halbAnmelden(kundeId, minuten = 10) {
  const token = randomBytes(32).toString('hex');
  const ab = new Date(Date.now() + minuten * 60_000);
  db.prepare(`INSERT INTO sitzungen (token, kunde_id, laeuft_ab, halb)
              VALUES (?, ?, ?, 1)`).run(token, kundeId, ab.toISOString());
  return token;
}

function sitzungsZeile(token) {
  if (!token) return null;
  const s = db.prepare('SELECT * FROM sitzungen WHERE token = ?').get(token);
  if (!s) return null;
  if (new Date(s.laeuft_ab) < new Date()) {
    db.prepare('DELETE FROM sitzungen WHERE token = ?').run(token);
    return null;
  }
  return s;
}

export function sitzung(token) {
  const s = sitzungsZeile(token);
  return s && !s.halb ? kunde(s.kunde_id) : null;
}

/** Wer mitten in der Anmeldung steckt - Passwort ja, Code noch nicht. */
export function halbeSitzung(token) {
  const s = sitzungsZeile(token);
  return s && s.halb ? kunde(s.kunde_id) : null;
}

/** Der Code stimmte: aus der halben Anmeldung eine richtige machen. */
export function sitzungGanz(token, tage = 14) {
  const ab = new Date();
  ab.setDate(ab.getDate() + tage);
  db.prepare('UPDATE sitzungen SET halb = 0, laeuft_ab = ? WHERE token = ?')
    .run(ab.toISOString(), token);
}

export const abmelden = (token) =>
  db.prepare('DELETE FROM sitzungen WHERE token = ?').run(token);

// ------------------------------------------------------- Zweiter Faktor

/**
 * Ob der zweite Faktor wirklich scharf ist.
 *
 * Zwei Spalten, drei Zustaende: kein Geheimnis heisst "nie eingerichtet",
 * Geheimnis ohne Datum heisst "gerade dabei, aber noch nicht bestaetigt",
 * beides heisst "an". Der mittlere Zustand ist der wichtige - solange er
 * gilt, darf die Anmeldung noch nicht danach fragen, sonst sperrt sich
 * jemand aus, der die App nur halb eingerichtet hat.
 */
export const zweifachAktiv = (k) => Boolean(k?.totp_geheim && k?.totp_seit);

/** Ein Geheimnis ablegen, das noch bestaetigt werden muss. */
export const totpVorbereiten = (kundeId, geheim) =>
  db.prepare(`UPDATE kunden SET totp_geheim = ?, totp_seit = NULL, totp_schritt = 0
              WHERE id = ?`).run(geheim, kundeId);

/**
 * Den zweiten Faktor scharf schalten.
 *
 * Alle anderen Sitzungen fliegen raus - nur die, in der man gerade
 * sitzt, bleibt. Wer den zweiten Faktor einrichtet, tut das, weil ihm
 * sein Konto wichtig ist; dann sollen die Browser, in denen er
 * irgendwann einmal angemeldet blieb, neu durch die Anmeldung. Sich
 * dabei selbst hinauszuwerfen waere nur verwirrend.
 */
export function zweifachAn(kundeId, hashes, ausserToken = '') {
  db.prepare('UPDATE kunden SET totp_seit = ?, totp_schritt = 0 WHERE id = ?')
    .run(jetzt(), kundeId);
  ersatzcodesSetzen(kundeId, hashes);
  db.prepare('DELETE FROM sitzungen WHERE kunde_id = ? AND token != ?')
    .run(kundeId, ausserToken);
}

export function zweifachAus(kundeId) {
  db.prepare(`UPDATE kunden SET totp_geheim = NULL, totp_seit = NULL,
              totp_schritt = 0 WHERE id = ?`).run(kundeId);
  db.prepare('DELETE FROM ersatzcode WHERE kunde_id = ?').run(kundeId);
}

/**
 * Den zuletzt benutzten Zeitschritt merken.
 *
 * Damit derselbe Code kein zweites Mal geht. Wer ihn ueber die Schulter
 * abliest, hat sonst dreissig Sekunden, in denen er ihn selbst
 * eintippen kann - und in diesen dreissig Sekunden sitzt er noch daneben.
 */
export const totpSchritt = (kundeId, schritt) =>
  db.prepare('UPDATE kunden SET totp_schritt = ? WHERE id = ?').run(schritt, kundeId);

export function ersatzcodesSetzen(kundeId, hashes) {
  db.prepare('DELETE FROM ersatzcode WHERE kunde_id = ?').run(kundeId);
  const rein = db.prepare('INSERT INTO ersatzcode (kunde_id, hash) VALUES (?, ?)');
  for (const h of hashes) rein.run(kundeId, h);
}

/** Wie viele Ersatzcodes noch offen sind. */
export const ersatzcodesOffen = (kundeId) =>
  db.prepare('SELECT COUNT(*) AS n FROM ersatzcode WHERE kunde_id = ? AND benutzt_am IS NULL')
    .get(kundeId).n;

/**
 * Einen Ersatzcode einloesen - genau einmal.
 *
 * Er wird nicht geloescht, sondern als benutzt markiert. So sieht man
 * spaeter noch, dass einer verbraucht wurde, ohne dass er wieder ginge.
 */
export function ersatzcodeEinloesen(kundeId, hash) {
  const z = db.prepare(`SELECT id FROM ersatzcode
                        WHERE kunde_id = ? AND hash = ? AND benutzt_am IS NULL`)
    .get(kundeId, hash);
  if (!z) return false;
  db.prepare('UPDATE ersatzcode SET benutzt_am = ? WHERE id = ?').run(jetzt(), z.id);
  return true;
}

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
  return s ? { ...s, zusatz: zusatzVon(s.id), ports: portsVon(s.id) } : null;
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
                   'knoten_id', 'art', 'mc_version', 'startbefehl',
                   'start_flaggen', 'jar_datei', 'notiz'];
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
  db.prepare('DELETE FROM server_port WHERE server_id = ?').run(id);
}

// ---------------------------------------------------------------- Zugaenge

export function zugangAnlegen(kundeId, name, hash) {
  const info = db.prepare(`INSERT INTO zugang (kunde_id, name, hash, angelegt)
      VALUES (?, ?, ?, ?)`).run(kundeId, name, hash, jetzt());
  return Number(info.lastInsertRowid);
}

export const alleZugaenge = () => db.prepare('SELECT * FROM zugang').all();

export const zugaengeVon = (kundeId) =>
  db.prepare('SELECT id, name, angelegt, zuletzt FROM zugang WHERE kunde_id = ? ORDER BY id')
    .all(kundeId);

export const zugangWeg = (kundeId, id) =>
  db.prepare('DELETE FROM zugang WHERE kunde_id = ? AND id = ?').run(kundeId, id);

export const zugangBenutzt = (id) =>
  db.prepare('UPDATE zugang SET zuletzt = ? WHERE id = ?').run(jetzt(), id);

// ------------------------------------------------------------ Weitere Ports

/**
 * Ein Minecraft-Server braucht oft mehr als einen Port.
 *
 * Geyser laesst Bedrock-Spieler ueber UDP 19132 herein, Dynmap zeigt
 * eine Karte auf einem Webport, Voice-Chat-Plugins wollen ihren eigenen.
 * In Pterodactyl heisst das "Allocations" - ohne so etwas kaeme man an
 * diese Dienste von aussen gar nicht heran, weil der Container nur den
 * einen Port durchreicht.
 */
export const portsVon = (serverId) =>
  db.prepare('SELECT * FROM server_port WHERE server_id = ? ORDER BY port')
    .all(serverId);

/** Alle Ports, die auf einem Knoten schon vergeben sind - Haupt und Extra. */
export function belegtePorts(knotenId = 0) {
  const haupt = db.prepare('SELECT port FROM server WHERE knoten_id = ?')
    .all(knotenId).map((s) => s.port);
  const extra = db.prepare(`SELECT p.port FROM server_port p
                            JOIN server s ON s.id = p.server_id
                            WHERE s.knoten_id = ?`).all(knotenId).map((p) => p.port);
  return new Set([...haupt, ...extra]);
}

export function portDazu(serverId, port, protokoll = 'beide', notiz = '') {
  const s = server(serverId);
  if (!s) return 'Diesen Server gibt es nicht.';
  if (belegtePorts(s.knoten_id).has(port)) {
    return `Port ${port} ist auf dieser Maschine schon vergeben.`;
  }
  db.prepare(`INSERT INTO server_port (server_id, port, protokoll, notiz)
              VALUES (?, ?, ?, ?)`).run(serverId, port, protokoll, notiz);
  return null;
}

export const portWeg = (serverId, id) =>
  db.prepare('DELETE FROM server_port WHERE server_id = ? AND id = ?').run(serverId, id);

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
