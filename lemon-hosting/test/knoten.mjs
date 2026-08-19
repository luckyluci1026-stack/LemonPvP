/**
 * Ein Server auf einer anderen Maschine - hier auf demselben Rechner,
 * aber ueber dieselbe Leitung wie im Ernstfall.
 *
 * Gestartet werden zwei Prozesse: das Portal auf 3111 und ein Daemon auf
 * 8391 mit eigenem Serverordner. Dann wird der Knoten im Portal
 * eingetragen und ein Server darauf gelegt - und alles, was das Panel
 * kann, muss ueber die Leitung genauso gehen.
 *
 *   ADMINPW=... ERSATZ_JAR=/pfad/server.jar node test/knoten.mjs
 */
import { spawn } from 'node:child_process';
import { mkdtempSync, rmSync, existsSync, readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const BASIS = 'http://127.0.0.1:3111';
const DAEMON_PORT = 8391;
const arbeit = mkdtempSync(join(tmpdir(), 'knoten-'));

let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};
const warte = (ms) => new Promise((f) => setTimeout(f, ms));
const bis = async (p, sek) => {
  const ende = Date.now() + sek * 1000;
  while (Date.now() < ende) { if (await p()) return true; await warte(400); }
  return false;
};

// ------------------------------------------------------------------ Daemon
const ZEICHEN = 'test-zeichen-fuer-den-knoten-1234';
const daemonOrdner = join(arbeit, 'daemon');
const daemon = spawn(process.execPath, ['daemon.js'], {
  env: { ...process.env, PORT: String(DAEMON_PORT), ZEICHEN,
         SERVER_DIR: join(daemonOrdner, 'server') },
  stdio: ['ignore', 'pipe', 'pipe'],
});
let daemonAusgabe = '';
daemon.stdout.on('data', (d) => { daemonAusgabe += d; });
daemon.stderr.on('data', (d) => { daemonAusgabe += d; });

const aufraeumen = () => {
  try { daemon.kill('SIGKILL'); } catch { /* schon weg */ }
  rmSync(arbeit, { recursive: true, force: true });
};
process.on('exit', aufraeumen);

ok('Daemon startet', await bis(async () => {
  try {
    const a = await fetch(`http://127.0.0.1:${DAEMON_PORT}/hallo`,
      { headers: { 'X-Lemon-Zeichen': ZEICHEN } });
    return a.ok;
  } catch { return false; }
}, 20), daemonAusgabe.split('\n').find((z) => z.includes('hört auf'))?.trim());

// Ohne Zeichen kommt niemand rein.
let a = await fetch(`http://127.0.0.1:${DAEMON_PORT}/hallo`);
ok('Ohne Zeichen: abgewiesen', a.status === 401);
a = await fetch(`http://127.0.0.1:${DAEMON_PORT}/hallo`,
  { headers: { 'X-Lemon-Zeichen': 'falsch' } });
ok('Mit falschem Zeichen: abgewiesen', a.status === 401);
a = await fetch(`http://127.0.0.1:${DAEMON_PORT}/zustaende?ids=1`,
  { headers: { 'X-Lemon-Zeichen': 'x'.repeat(ZEICHEN.length) } });
ok('Auch bei gleicher Länge', a.status === 401);

// ------------------------------------------------------------------ Portal
const glas = new Map();
const kekse = () => [...glas].map(([k, v]) => `${k}=${v}`).join('; ');
async function h(p, o = {}) {
  const antwort = await fetch(BASIS + p, { ...o, redirect: 'manual',
    headers: { ...(o.headers || {}), ...(glas.size ? { cookie: kekse() } : {}) } });
  for (const roh of antwort.headers.getSetCookie?.() || []) {
    const [n, ...v] = roh.split(';')[0].split('='); glas.set(n, v.join('='));
  }
  return { status: antwort.status, ort: antwort.headers.get('location'),
           text: await antwort.text() };
}
const s = (p, f = {}) => h(p, { method: 'POST',
  body: new URLSearchParams({ csrf: glas.get('csrf'), ...f }),
  headers: { 'content-type': 'application/x-www-form-urlencoded' } });

await h('/');
a = await s('/anmelden', { benutzername: 'admin', passwort: process.env.ADMINPW });
ok('Am Portal angemeldet', a.status === 302, a.ort);

// -------------------------------------------------------------- Knoten
a = await s('/admin/knoten', { name: 'Serverraum 2',
  adresse: `http://127.0.0.1:${DAEMON_PORT}`, geheim: ZEICHEN, notiz: 'Testknoten' });
ok('Knoten eingetragen', a.status === 302 && (a.ort || '').includes('ok='),
   decodeURIComponent(a.ort || ''));

a = await h('/admin/knoten');
ok('Knoten meldet sich als erreichbar', a.text.includes('erreichbar')
   && !a.text.includes('nicht erreichbar'));
ok('Und sagt, was er kann', /Node \d+\.\d+/.test(a.text),
   (a.text.match(/Node [\d.]+[^<]*/) || [])[0]);

a = await s('/admin/knoten', { name: 'Kaputt', adresse: 'http://127.0.0.1:9', geheim: ZEICHEN });
a = await h('/admin/knoten');
ok('Ein toter Knoten wird als solcher gezeigt', a.text.includes('nicht erreichbar'));

a = await s('/admin/knoten', { name: 'Ohne Zeichen', adresse: 'http://127.0.0.1:1' });
ok('Knoten ohne Zeichen wird abgelehnt',
   decodeURIComponent(a.ort || '').includes('Zeichen'), a.ort);

// -------------------------------------------------- Server auf dem Knoten
a = await h('/admin/server/neu');
ok('Das Formular bietet den Knoten an', a.text.includes('Serverraum 2'));
const kundeId = (a.text.match(/name="kundeId"[\s\S]*?<option value="(\d+)"/) || [])[1];
const knotenId = (a.text.match(/name="knotenId"[\s\S]*?<option value="(\d+)"/g) || []).length;
const knotenNr = (a.text.match(/<option value="(\d+)"[^>]*>Serverraum 2/) || [])[1];

a = await s('/admin/server/neu', { kundeId, name: 'Ferner Server',
  paket: 'coal', software: 'Paper', knotenId: knotenNr });
const id = Number((a.ort || '').match(/server\/(\d+)/)?.[1]);
ok('Server auf dem Knoten angelegt', Number.isInteger(id), '#' + id);

a = await h(`/admin/server/${id}`);
ok('Verwaltung zeigt die Maschine', a.text.includes('läuft auf: Serverraum 2'));

// ---------------------------------------------------------------- Panel
a = await h(`/panel/${id}`);
ok('Panel öffnet sich', a.status === 200 && a.text.includes('Konsole'));
ok('Und sagt, wo der Server liegt', a.text.includes('Läuft auf Serverraum 2'));

// Datei über die Leitung anlegen und wiederfinden
a = await s(`/panel/${id}/neu`, { p: '', name: 'server.properties',
  inhalt: 'motd=Ferner Server\n' });
ok('Datei über die Leitung angelegt', a.status === 302 && (a.ort || '').includes('ok='));
ok('Sie liegt beim Daemon, nicht beim Portal',
   existsSync(join(daemonOrdner, 'server', String(id), 'server.properties')),
   join('daemon/server', String(id), 'server.properties'));

a = await h(`/panel/${id}/dateien`);
ok('Dateiliste kommt vom Knoten', a.text.includes('server.properties'));
a = await h(`/panel/${id}/bearbeiten?p=server.properties`);
ok('Editor liest über die Leitung', a.text.includes('motd=Ferner Server'));

// Upload durchreichen. Ohne Ersatzserver nehmen wir irgendeine Datei -
// geprueft wird hier, dass der Strom ankommt, nicht was drinsteht.
const zeichenCsrf = encodeURIComponent(glas.get('csrf'));
const jar = readFileSync(process.env.ERSATZ_JAR || 'daemon.js');
a = await h(`/panel/${id}/hochladen?p=&name=server.jar&csrf=${zeichenCsrf}`,
  { method: 'POST', body: jar, headers: { 'content-type': 'application/octet-stream' } });
ok('Upload wird durchgereicht', a.status === 200, `${jar.length} Bytes`);
ok('Und landet beim Daemon',
   existsSync(join(daemonOrdner, 'server', String(id), 'server.jar')));

// Plugins vom Katalog des Knotens
a = await h(`/panel/${id}`);
const katalog = [...a.text.matchAll(/name="datei" value="([^"]+)"/g)].map((m) => m[1]);
ok('Plugin-Katalog kommt vom Knoten', katalog.length > 0, katalog.length + ' Stück');
if (katalog.length) {
  a = await s(`/panel/${id}/plugin/installieren`, { datei: katalog[0] });
  ok('Plugin über die Leitung installiert', (a.ort || '').includes('ok='));
  ok('Die Jar liegt beim Daemon',
     existsSync(join(daemonOrdner, 'server', String(id), 'plugins', katalog[0])));
}

// ------------------------------------------------------------ Starten
if (process.env.ERSATZ_JAR) {
  await s(`/panel/${id}/aktion`, { was: 'eula' });
  a = await s(`/panel/${id}/aktion`, { was: 'start' });
  ok('Start über die Leitung angenommen',
     a.status === 302 && !decodeURIComponent(a.ort || '').includes('?m='),
     decodeURIComponent(a.ort || ''));

  // Der Zustand kommt aus dem Zwischenspeicher - der Ticker braucht einen Moment.
  ok('Portal merkt, dass der ferne Server läuft', await bis(async () => {
    const seite = await h(`/panel/${id}`);
    return seite.text.includes('>läuft<');
  }, 40));

  // Konsole: das Portal reicht den Strom des Daemons durch
  const strom = await fetch(`${BASIS}/panel/${id}/konsole`, { headers: { cookie: kekse() } });
  const leser = strom.body.getReader();
  const anfang = new TextDecoder().decode((await leser.read()).value);
  ok('Konsole wird durchgereicht',
     anfang.includes('event: verlauf') && anfang.includes('Starting minecraft server'),
     'Startzeilen des fernen Servers');
  await leser.cancel().catch(() => {});

  a = await s(`/panel/${id}/befehl`, { befehl: 'say hallo aus der ferne' });
  ok('Befehl geht über die Leitung', a.status === 200 && JSON.parse(a.text).ok);
} else {
  console.log('    (ohne Ersatzserver: Starten, Konsole und Befehl übersprungen)');
}

// ------------------------------------------------------------- Backup
a = await s(`/panel/${id}/sicherung`);
ok('Backup auf dem Knoten angelegt', (a.ort || '').includes('ok='),
   decodeURIComponent(a.ort || '').slice(0, 60));
ok('Es liegt beim Daemon',
   existsSync(join(daemonOrdner, 'sicherungen', String(id)))
   && readdirSync(join(daemonOrdner, 'sicherungen', String(id))).length === 1);

a = await h(`/panel/${id}`);
const sicherung = (a.text.match(/f=([\d-]+_[\d-]+(?:-\d+)?\.zip)/) || [])[1];
const zip = await fetch(`${BASIS}/panel/${id}/sicherung/laden?f=${sicherung}`,
  { headers: { cookie: kekse() } });
const rohZip = Buffer.from(await zip.arrayBuffer());
ok('Backup wird durchgereicht', zip.status === 200
   && rohZip.subarray(0, 2).toString() === 'PK', `${rohZip.length} Bytes`);

// ------------------------------------------------------------- Stoppen
if (process.env.ERSATZ_JAR) {
  await s(`/panel/${id}/aktion`, { was: 'stopp' });
  ok('Ferner Server gestoppt', await bis(async () => {
    const seite = await h(`/panel/${id}`);
    return seite.text.includes('>gestoppt<');
  }, 40));
}

// ------------------------------------------------- Knoten fällt aus
daemon.kill('SIGKILL');
await warte(1500);
ok('Fällt der Knoten aus, sagt das Panel das',
   await bis(async () => {
     const seite = await h(`/panel/${id}`);
     return seite.text.includes('nicht erreichbar')
         || seite.text.includes('meldet sich gerade nicht');
   }, 40));
a = await s(`/panel/${id}/aktion`, { was: 'start' });
ok('Und ein Start scheitert mit Ansage',
   decodeURIComponent(a.ort || '').includes('m='), decodeURIComponent(a.ort || ''));

// Ein Knoten mit Servern darf nicht verschwinden
a = await s(`/admin/knoten/${knotenNr}/loeschen`);
ok('Ein belegter Knoten lässt sich nicht löschen',
   decodeURIComponent(a.ort || '').includes('liegen noch'), decodeURIComponent(a.ort || ''));

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
