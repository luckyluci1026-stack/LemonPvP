/**
 * Einen Server von einer Maschine auf eine andere umziehen.
 *
 * Getestet wird in beide Richtungen - hierhin und wieder weg - und mit
 * echten Dateien ueber eine echte Leitung. Der Punkt, auf den es
 * ankommt, steht weiter unten: Wenn die Uebertragung scheitert, darf in
 * der Datenbank nichts umgestellt sein. Sonst zeigt das Panel auf eine
 * Maschine, auf der nichts liegt, und der Server ist weg, obwohl alle
 * Dateien noch da sind.
 *
 *   ADMINPW=... node test/umzug.mjs
 */
import { spawn } from 'node:child_process';
import { mkdtempSync, rmSync, existsSync, readFileSync, mkdirSync,
         writeFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const BASIS = 'http://127.0.0.1:3111';
const DAEMON_PORT = 8392;
const ZEICHEN = 'test-zeichen-fuer-den-umzug-987654';
const arbeit = mkdtempSync(join(tmpdir(), 'umzug-'));
const HIER = process.env.SERVER_DIR || 'server';

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
const daemonOrdner = join(arbeit, 'daemon');
const daemon = spawn(process.execPath, ['daemon.js'], {
  env: { ...process.env, PORT: String(DAEMON_PORT), ZEICHEN,
         SERVER_DIR: join(daemonOrdner, 'server') },
  stdio: ['ignore', 'pipe', 'pipe'],
});
let ausgabe = '';
daemon.stdout.on('data', (d) => { ausgabe += d; });
daemon.stderr.on('data', (d) => { ausgabe += d; });

process.on('exit', () => {
  try { daemon.kill('SIGKILL'); } catch { /* schon weg */ }
  rmSync(arbeit, { recursive: true, force: true });
});

ok('Daemon steht', await bis(async () => {
  try {
    const a = await fetch(`http://127.0.0.1:${DAEMON_PORT}/hallo`,
      { headers: { 'X-Lemon-Zeichen': ZEICHEN } });
    return a.ok;
  } catch { return false; }
}, 20), `Port ${DAEMON_PORT}`);

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
const meldung = (a) => decodeURIComponent((a.ort || '').split('ok=')[1] || '');

await h('/');
let a = await s('/anmelden', { benutzername: 'admin', passwort: process.env.ADMINPW });
ok('Am Portal angemeldet', a.status === 302, a.ort);

a = await s('/admin/knoten', { name: 'Umzugsknoten',
  adresse: `http://127.0.0.1:${DAEMON_PORT}`, geheim: ZEICHEN });
ok('Knoten eingetragen', (a.ort || '').includes('ok='), meldung(a));

a = await h('/admin/server/neu');
const kundeId = (a.text.match(/name="kundeId"[\s\S]*?<option value="(\d+)"/) || [])[1];
const knotenNr = (a.text.match(/<option value="(\d+)"[^>]*>Umzugsknoten/) || [])[1];
ok('Der Knoten steht im Formular', Boolean(knotenNr), '#' + knotenNr);

// ------------------------------------------------- Ein Server mit Inhalt
a = await s('/admin/server/neu',
  { kundeId, name: 'Umzugsserver', paket: 'coal', software: 'Paper' });
const id = Number((a.ort || '').match(/server\/(\d+)/)?.[1]);
ok('Server hier angelegt', Number.isInteger(id), '#' + id);

// Ein frisch angelegter Server hat noch keine einzige Datei. Genau der
// Fall, in dem man sich beim Anlegen in der Maschine vertan hat - der
// darf nicht der einzige sein, den die Karte nicht kann.
a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: knotenNr });
ok('Ein leerer Server zieht auch um', meldung(a).includes('lag noch nichts'),
   meldung(a));
a = await h(`/admin/server/${id}`);
ok('Und liegt danach wirklich drüben', a.text.includes('läuft auf: Umzugsknoten'));
a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: '0' });
ok('Zurück geht er genauso', meldung(a).includes('lag noch nichts'), meldung(a));

const hierOrdner = join(HIER, String(id));
mkdirSync(join(hierOrdner, 'welt', 'region'), { recursive: true });
writeFileSync(join(hierOrdner, 'server.properties'), 'motd=Vor dem Umzug\n');
writeFileSync(join(hierOrdner, 'welt', 'level.dat'), Buffer.alloc(4096, 7));
writeFileSync(join(hierOrdner, 'welt', 'region', 'r.0.0.mca'), Buffer.alloc(64_000, 3));
mkdirSync(join(hierOrdner, 'plugins'), { recursive: true });
writeFileSync(join(hierOrdner, 'plugins', 'Ding.jar'), Buffer.alloc(1024, 9));

a = await h(`/admin/server/${id}`);
ok('Die Verwaltung bietet den Umzug an',
   a.text.includes('Umziehen') && a.text.includes('Jetzt umziehen'));
ok('Und nennt die Maschinen, die in Frage kommen', a.text.includes('Umzugsknoten'));

// -------------------------------------------------------- Hinüber
a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: knotenNr });
ok('Der Umzug wird angenommen', a.status === 302, meldung(a).slice(0, 70));
ok('Und sagt, wie viele Dateien angekommen sind', /\b4 Dateien/.test(meldung(a)),
   meldung(a));

const drueben = join(daemonOrdner, 'server', String(id));
ok('Die Welt liegt jetzt beim Daemon',
   existsSync(join(drueben, 'welt', 'region', 'r.0.0.mca')));
ok('Auch die kleinen Dateien', existsSync(join(drueben, 'server.properties'))
   && existsSync(join(drueben, 'plugins', 'Ding.jar')));
ok('Mit unverändertem Inhalt',
   readFileSync(join(drueben, 'server.properties'), 'utf8') === 'motd=Vor dem Umzug\n');
ok('Und in voller Größe',
   readFileSync(join(drueben, 'welt', 'region', 'r.0.0.mca')).length === 64_000);

ok('Die alten Dateien bleiben liegen', existsSync(join(hierOrdner, 'server.properties')),
   'löschen soll der Admin selbst');
ok('Darauf weist die Meldung auch hin', meldung(a).includes('alten Dateien'));

a = await h(`/admin/server/${id}`);
ok('Das Portal zeigt jetzt die neue Maschine', a.text.includes('läuft auf: Umzugsknoten'));
a = await h(`/panel/${id}/dateien`);
ok('Und die Dateiliste kommt von dort', a.text.includes('welt') && a.text.includes('plugins'));

// --------------------------------------- Zweimal in dieselbe Richtung
a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: knotenNr });
ok('Zweimal auf dieselbe Maschine geht nicht', meldung(a).includes('schon auf dieser'),
   meldung(a));

// ----------------------------------------------- Und wieder zurück
// Ein zweiter Server hier belegt inzwischen den Port - beim Zurückziehen
// muss das Portal einen freien nehmen, sonst binden zwei auf denselben.
a = await h(`/admin/server/${id}`);
const portVorher = Number((a.text.match(/name="port"[^>]*value="(\d+)"/) || [])[1]);
ok('Der Server hat einen Port', portVorher > 0, String(portVorher));

a = await s('/admin/server/neu',
  { kundeId, name: 'Platzhalter', paket: 'coal', software: 'Paper', port: String(portVorher) });
const zweite = Number((a.ort || '').match(/server\/(\d+)/)?.[1]);
ok('Ein zweiter Server belegt hier denselben Port', Number.isInteger(zweite),
   `#${zweite} auf ${portVorher}`);

a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: '0' });
const zurueck = meldung(a);
ok('Der Rückweg geht auch', a.status === 302 && zurueck.startsWith('Umgezogen'),
   zurueck.slice(0, 80));
ok('Der belegte Port wird gemerkt und getauscht',
   zurueck.includes('neuer Port'), zurueck);

a = await h(`/admin/server/${id}`);
const portNachher = Number((a.text.match(/name="port"[^>]*value="(\d+)"/) || [])[1]);
ok('Und steht so auch im Formular', portNachher !== portVorher && portNachher > 0,
   `${portVorher} → ${portNachher}`);
ok('Die Dateien sind wieder hier',
   existsSync(join(hierOrdner, 'welt', 'region', 'r.0.0.mca'))
   && readFileSync(join(hierOrdner, 'server.properties'), 'utf8') === 'motd=Vor dem Umzug\n');
ok('Der Daemon hat vorher ein Backup angelegt',
   existsSync(join(daemonOrdner, 'sicherungen', String(id)))
   && readdirSync(join(daemonOrdner, 'sicherungen', String(id))).length >= 1,
   'die Sicherheitskopie bleibt drüben liegen');
ok('Und die Meldung nennt sie beim Namen',
   /Sicherheitskopie \d{4}-\d{2}-\d{2}_[\d-]+\.zip/.test(zurueck), zurueck);

// --------------------------------------------- Ein laufender Server zieht nicht um
writeFileSync(join(hierOrdner, 'eula.txt'), 'eula=true\n');
if (process.env.ERSATZ_JAR) {
  writeFileSync(join(hierOrdner, 'server.jar'), readFileSync(process.env.ERSATZ_JAR));
  await s(`/panel/${id}/aktion`, { was: 'start' });
  const lief = await bis(async () => (await h(`/panel/${id}`)).text.includes('>läuft<'), 40);
  ok('Der Server läuft', lief);
  a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: knotenNr });
  ok('Ein laufender Server zieht nicht um', meldung(a).includes('Stopp'), meldung(a));
  a = await h(`/admin/server/${id}`);
  ok('Und bleibt in der Datenbank, wo er war', a.text.includes('läuft auf: dieser Rechner')
     || !a.text.includes('läuft auf: Umzugsknoten'));
  a = await h(`/admin/server/${id}`);
  ok('Die Karte sagt auch, warum gerade nicht', a.text.includes('Stopp ihn im Panel'));
  await s(`/panel/${id}/aktion`, { was: 'stopp' });
  await bis(async () => (await h(`/panel/${id}`)).text.includes('>gestoppt<'), 40);
} else {
  console.log('    (ohne ERSATZ_JAR: der laufende Fall wird übersprungen)');
}

// ------------------------------------- Wenn die Gegenseite ausfällt
daemon.kill('SIGKILL');
await warte(1000);

a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: knotenNr });
ok('Bei einer toten Maschine scheitert der Umzug mit Ansage',
   meldung(a).includes('Umzugsknoten') && meldung(a).includes('bleibt, wo er ist'),
   meldung(a));
a = await h(`/admin/server/${id}`);
ok('Und die Datenbank bleibt unangetastet',
   a.text.includes('läuft auf: dieser Rechner'),
   'der Server zeigt weiter dorthin, wo seine Dateien liegen');
ok('Die Dateien liegen auch noch hier',
   existsSync(join(hierOrdner, 'welt', 'region', 'r.0.0.mca')));

// -------------------------------------------------------- Kein Zielknoten
a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: '999' });
ok('Ein erfundener Zielknoten wird zu „hier" – und hier ist er schon',
   meldung(a).includes('schon auf dieser'), meldung(a));

// Ein Unterbenutzer oder Kunde hat hier nichts verloren.
glas.delete('sitzung');
await h('/');
a = await s(`/admin/server/${id}/umzug`, { zielKnotenId: knotenNr });
ok('Ohne Anmeldung kommt niemand an den Umzug',
   a.status === 302 && (a.ort || '').includes('/anmelden'), a.ort);

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
