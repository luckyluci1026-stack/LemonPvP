/**
 * API-Zugänge, weitere Ports und die Aktivität je Server.
 *
 * Der Kern steht in der Mitte: Ein Schlüssel kann nie mehr als der
 * Kunde, dem er gehört. Er ist ein anderer Weg herein, keine Abkürzung
 * an den Rechten vorbei.
 *
 *   ADMINPW=... node test/api.mjs
 */
const BASIS = 'http://127.0.0.1:3111';
let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

function browser() {
  const glas = new Map();
  const kekse = () => [...glas].map(([k, v]) => `${k}=${v}`).join('; ');
  const h = async (p, o = {}) => {
    const a = await fetch(BASIS + p, { ...o, redirect: 'manual',
      headers: { ...(o.headers || {}), ...(glas.size ? { cookie: kekse() } : {}) } });
    for (const roh of a.headers.getSetCookie?.() || []) {
      const [n, ...v] = roh.split(';')[0].split('='); glas.set(n, v.join('='));
    }
    return { status: a.status, ort: a.headers.get('location'), text: await a.text() };
  };
  const s = (p, f = {}) => h(p, { method: 'POST',
    body: new URLSearchParams({ csrf: glas.get('csrf'), ...f }),
    headers: { 'content-type': 'application/x-www-form-urlencoded' } });
  const anmelden = async (name, pw) => { glas.delete('sitzung'); await h('/');
    return s('/anmelden', { benutzername: name, passwort: pw }); };
  return { h, s, anmelden };
}

/** Ein Aufruf, wie ihn ein Skript machen würde - ohne Cookie, mit Schlüssel. */
async function api(pfad, schluessel, optionen = {}) {
  const a = await fetch(BASIS + pfad, {
    ...optionen,
    headers: { ...(optionen.headers || {}),
               ...(schluessel ? { authorization: 'Bearer ' + schluessel } : {}) },
  });
  const text = await a.text();
  let daten = null;
  try { daten = JSON.parse(text); } catch { /* kein JSON */ }
  return { status: a.status, daten, text };
}

// ------------------------------------------------------------------ Aufbau
const admin = browser();
await admin.h('/');
await admin.s('/anmelden', { benutzername: 'admin', passwort: process.env.ADMINPW });
await admin.s('/admin/kunden', { benutzername: 'lea.berg', passwort: 'passwortvonlea1' });
await admin.s('/admin/kunden', { benutzername: 'tom.klein', passwort: 'passwortvontom1' });

let a = await admin.h('/admin/server/neu');
const leaId = (a.text.match(/<option value="(\d+)"[^>]*>lea\.berg/) || [])[1];
a = await admin.s('/admin/server/neu', {
  kundeId: leaId, name: 'Klassenserver', paket: 'coal', software: 'Paper' });
const id = Number((a.ort || '').match(/server\/(\d+)/)?.[1]);
ok('Server angelegt', Number.isInteger(id), '#' + id);

const lea = browser();
const tom = browser();
await lea.anmelden('lea.berg', 'passwortvonlea1');
await tom.anmelden('tom.klein', 'passwortvontom1');

// ------------------------------------------------------------ Weitere Ports
a = await lea.h(`/panel/${id}`);
ok('Panel hat einen Netzwerk-Bereich',
   a.text.includes('Netzwerk') && a.text.includes('der Hauptport'));

a = await lea.s(`/panel/${id}/port`,
  { port: '19132', protokoll: 'udp', notiz: 'Bedrock über Geyser' });
ok('Ein Port lässt sich öffnen', a.status === 302 && (a.ort || '').includes('ok='),
   decodeURIComponent(a.ort || ''));
a = await lea.h(`/panel/${id}`);
ok('Er steht in der Liste', a.text.includes('19132') && a.text.includes('Bedrock über Geyser'));
ok('Mit dem richtigen Protokoll', /19132[\s\S]{0,220}UDP/.test(a.text));

a = await lea.s(`/panel/${id}/port`, { port: '19132' });
ok('Derselbe Port geht nicht zweimal',
   decodeURIComponent(a.ort || '').includes('schon vergeben'), decodeURIComponent(a.ort || ''));
a = await lea.s(`/panel/${id}/port`, { port: String(25565) });
ok('Auch der Hauptport ist belegt',
   decodeURIComponent(a.ort || '').includes('schon vergeben'));
a = await lea.s(`/panel/${id}/port`, { port: '80' });
ok('Ports unter 1024 werden abgelehnt',
   decodeURIComponent(a.ort || '').includes('zwischen 1024'));

a = await lea.s(`/panel/${id}/freigeben`,
  { benutzername: 'tom.klein', r_konsole: '1', r_steuern: '1' });
a = await tom.s(`/panel/${id}/port`, { port: '25599' });
ok('Ein Unterbenutzer darf keine Ports öffnen', a.status === 403, 'Status ' + a.status);

// ------------------------------------------------------------- Aktivität
a = await lea.h(`/panel/${id}`);
ok('Panel hat einen Aktivitätsbereich', a.text.includes('Aktivität'));
ok('Und zeigt, was gerade passiert ist',
   a.text.includes('Port geöffnet') && a.text.includes('Server freigegeben'));
ok('Mit dem Namen dessen, der es war', a.text.includes('lea.berg'));

// ------------------------------------------------------------- Zugänge
a = await lea.h('/zugaenge');
ok('Die Zugangsseite lädt', a.status === 200 && a.text.includes('API-Zugänge'));
ok('Noch ohne Zugang', a.text.includes('Noch kein Zugang'));

a = await lea.s('/zugaenge', { name: 'Discord-Bot der 8b' });
const schluessel = decodeURIComponent((a.ort || '').match(/neu=(.+)$/)?.[1] || '');
ok('Ein Schlüssel wird erzeugt', schluessel.startsWith('lemon_'),
   schluessel.slice(0, 14) + '…');
a = await lea.h('/zugaenge?neu=' + encodeURIComponent(schluessel));
ok('Er steht einmal auf der Seite', a.text.includes(schluessel));
a = await lea.h('/zugaenge');
ok('Und danach nicht mehr', !a.text.includes(schluessel));
ok('Der Zugang selbst bleibt sichtbar', a.text.includes('Discord-Bot der 8b'));

// ------------------------------------------------------------- API nutzen
a = await api('/api/server', null);
ok('Ohne Schlüssel: abgewiesen', a.status === 401, a.daten?.fehler);
a = await api('/api/server', 'lemon_falsch');
ok('Mit falschem Schlüssel: abgewiesen', a.status === 401);
a = await api('/api/server', 'garkeinvorsatz');
ok('Ohne den Vorsatz auch', a.status === 401);

a = await api('/api/server', schluessel);
ok('Mit Schlüssel kommt die Serverliste',
   a.status === 200 && a.daten.server.length === 1, JSON.stringify(a.daten).slice(0, 90));
ok('Mit den Angaben, die zählen',
   a.daten.server[0].name === 'Klassenserver'
   && a.daten.server[0].laeuft === 'gestoppt'
   && a.daten.server[0].port === 25565);

a = await api(`/api/server/${id}`, schluessel);
ok('Ein einzelner Server geht auch', a.status === 200 && a.daten.id === id);
a = await api('/api/server/9999', schluessel);
ok('Ein fremder nicht', a.status === 404);

a = await api(`/api/server/${id}/start`, schluessel, { method: 'POST' });
ok('Start ohne server.jar meldet den Grund',
   a.status === 409 && a.daten.fehler.includes('server.jar'), a.daten?.fehler);
a = await api(`/api/server/${id}/fliegen`, schluessel, { method: 'POST' });
ok('Eine erfundene Aktion wird abgelehnt', a.status === 404);

// Zuletzt-benutzt wird mitgeschrieben
a = await lea.h('/zugaenge');
ok('Das Portal merkt sich die letzte Benutzung', !a.text.includes('noch nie'));

// ---------------------------------------- Ein Schlüssel kann nie mehr als sein Kunde
a = await tom.s('/zugaenge', { name: 'Toms Bot' });
const tomSchluessel = decodeURIComponent((a.ort || '').match(/neu=(.+)$/)?.[1] || '');
a = await api('/api/server', tomSchluessel);
ok('Tom sieht über die API seinen geteilten Server',
   a.status === 200 && a.daten.server.length === 1);
a = await api(`/api/server/${id}/start`, tomSchluessel, { method: 'POST' });
ok('Und darf ihn steuern, weil Lea das erlaubt hat',
   a.status === 409 && a.daten.fehler.includes('server.jar'),
   'scheitert an der fehlenden Jar, nicht am Recht');

// Recht entziehen -> der Schlüssel kann es auch nicht mehr
a = await lea.h(`/panel/${id}`);
const tomKundeId = (a.text.match(/name="kundeId" value="(\d+)"/) || [])[1];
await lea.s(`/panel/${id}/freigeben`, { benutzername: 'tom.klein', r_konsole: '1' });
a = await api(`/api/server/${id}/start`, tomSchluessel, { method: 'POST' });
ok('Nimmt Lea das Recht, kann der Schlüssel es auch nicht mehr',
   a.status === 403 && a.daten.fehler.includes('steuern'), a.daten?.fehler);
a = await api(`/api/server/${id}/befehl`, tomSchluessel,
  { method: 'POST', headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: 'befehl=say hallo' });
ok('Konsole darf er weiterhin', a.status === 409 && a.daten.fehler.includes('läuft nicht'),
   a.daten?.fehler);

await lea.s(`/panel/${id}/freigabe-weg`, { kundeId: tomKundeId });
a = await api('/api/server', tomSchluessel);
ok('Nach dem Entzug sieht Toms Schlüssel nichts mehr',
   a.status === 200 && a.daten.server.length === 0);

// ------------------------------------------------------------- Löschen
a = await lea.h('/zugaenge');
const zugangId = (a.text.match(/name="id" value="(\d+)"/) || [])[1];
await lea.s('/zugaenge/weg', { id: zugangId });
a = await api('/api/server', schluessel);
ok('Ein gelöschter Schlüssel kommt nicht mehr rein', a.status === 401);

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
