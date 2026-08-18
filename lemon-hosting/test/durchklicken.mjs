/** Klickt das Portal einmal durch wie ein echter Benutzer. */
const BASIS = 'http://127.0.0.1:3111';
const keksglas = new Map();
const kekse = () => [...keksglas].map(([k,v]) => `${k}=${v}`).join('; ');
let fehler = 0;

const ok = (t, b, extra='') => { console.log(`${b?'OK  ':'FEHLER'} ${t}${extra?' · '+extra:''}`); if(!b) fehler++; };

async function hole(pfad, optionen = {}) {
  const a = await fetch(BASIS + pfad, {
    ...optionen, redirect: 'manual',
    headers: { ...(optionen.headers||{}),
               ...(keksglas.size ? { cookie: kekse() } : {}) },
  });
  for (const roh of a.headers.getSetCookie?.() || []) {
    const [name, ...rest] = roh.split(';')[0].split('=');
    keksglas.set(name, rest.join('='));
  }
  return { status: a.status, ort: a.headers.get('location'), text: await a.text() };
}

const csrfAus = (html) => (html.match(/name="csrf" value="([^"]+)"/) || [])[1] || '';

async function sende(pfad, felder) {
  // Das CSRF-Zeichen steht im Cookie - einmal irgendeine Seite geholt zu
  // haben reicht, um es zu bekommen.
  if (!keksglas.has('csrf')) await hole('/');
  const koerper = new URLSearchParams({ csrf: keksglas.get('csrf'), ...felder });
  return hole(pfad, { method: 'POST', body: koerper,
    headers: { 'content-type': 'application/x-www-form-urlencoded' } });
}

// ---------------------------------------------------------------- Ablauf
let a = await hole('/');
ok('Startseite lädt', a.status === 200 && a.text.includes('Lemon Hosting'));
ok('Alle drei Pakete sichtbar',
   ['Wood','Coal','Diamond'].every((p) => a.text.includes(p)));
ok('Wood-Preis stimmt', a.text.includes('3,99 €'));

a = await hole('/preise.json');
const preise = JSON.parse(a.text);
ok('Preise als JSON', preise.pakete.coal.preis === 5.99);

a = await hole('/regeln');
ok('Regelseite lädt', a.status === 200 && a.text.includes('Prepaid'));

a = await hole('/meine-server');
ok('Ohne Anmeldung kein Kundenbereich', a.status === 302 && a.ort === '/anmelden');
a = await hole('/admin');
ok('Ohne Anmeldung keine Verwaltung', a.status === 302);

// CSRF muss greifen
a = await hole('/anmelden', { method: 'POST',
  body: new URLSearchParams({ benutzername: 'admin', passwort: 'x' }),
  headers: { 'content-type': 'application/x-www-form-urlencoded' } });
ok('POST ohne CSRF wird abgelehnt', a.status === 400);

// Anfrage aus dem Konfigurator
a = await sende('/konfigurator', {
  paket: 'wood', z_subdomain: '1', z_flfac: '1', z_ram1: '2',
  vorname: 'Lea', nachname: 'Berg', klasse: '8b', mcname: 'LeaB',
  kontakt: 'lea#1234', servername: 'Klassenserver 8b', subdomain: 'klasse8b',
  software: 'Paper', wunsch: 'Bitte mit Worldedit', regeln: '1',
});
ok('Anfrage angenommen', a.status === 200 && a.text.includes('eingegangen'));
ok('Preis in der Bestätigung', a.text.includes('6,54 €'),
   'Wood 3,99 + Subdomain 0,25 + FLFAC 0,30 + 2 GB 2,00');

// Falsches Passwort
a = await sende('/anmelden', { benutzername: 'admin', passwort: 'falsch' });
ok('Falsches Passwort wird abgelehnt', a.text.includes('stimmt nicht'));

// Richtig anmelden
a = await sende('/anmelden', { benutzername: 'admin', passwort: process.env.ADMINPW });
ok('Admin-Anmeldung', a.status === 302 && a.ort === '/admin', a.ort);

a = await hole('/admin');
ok('Verwaltung lädt', a.status === 200 && a.text.includes('Offene Anfragen'));
ok('Anfrage in der Übersicht', a.text.includes('Klassenserver 8b'));

// Anfrage annehmen -> Kunde + Server entstehen
a = await sende('/admin/anfrage/1/annehmen', {
  kundeId: '', benutzername: 'lea.berg', passwort: 'startpasswort1' });
ok('Anfrage angenommen → Server angelegt', a.status === 302 && /\/admin\/server\/\d+/.test(a.ort||''), a.ort);
const serverId = Number((a.ort || '').match(/server\/(\d+)/)?.[1]);

a = await hole(`/admin/server/${serverId}`);
ok('Serverseite lädt', a.status === 200 && a.text.includes('Klassenserver 8b'));
ok('Zusatzleistungen übernommen', a.text.includes('value="2"'), '2 GB RAM');
ok('Preis auf der Serverseite', a.text.includes('6,54 €'));

// Zahlung eintragen
a = await sende(`/admin/server/${serverId}/zahlung`, {
  betrag: '6.54', tage: '30', art: 'Bar', kassiertVon: 'Team', notiz: 'bar erhalten' });
ok('Zahlung eingetragen', a.status === 302 && (a.ort||'').includes('bezahlt%20bis'), a.ort);

// Nochmal zahlen -> muss ans Ende anschliessen, nicht bei heute
a = await sende(`/admin/server/${serverId}/zahlung`, { betrag: '6.54', tage: '30' });
a = await hole(`/admin/server/${serverId}`);
const bis = (a.text.match(/Bezahlt bis <strong>(\d{4}-\d{2}-\d{2})/) || [])[1];
const inTagen = Math.round((new Date(bis) - new Date(new Date().toISOString().slice(0,10))) / 86400000);
ok('Zweite Zahlung verlängert korrekt', inTagen === 60, `bezahlt bis ${bis} = ${inTagen} Tage`);

// Dokumente
a = await hole(`/admin/server/${serverId}/bestellbogen`);
ok('Bestellbogen druckbar', a.status === 200 && a.text.includes('Bestellbogen'));
ok('Bestellbogen ist vorausgefüllt',
   a.text.includes('Lea') && a.text.includes('Klassenserver 8b') && a.text.includes('klasse8b'));
ok('Bestellbogen kreuzt das richtige Paket an', /☑.*<strong>Wood/.test(a.text));
ok('Bestellbogen zeigt die Summe', a.text.includes('6,54 €'));

a = await hole(`/admin/server/${serverId}/loeschbestaetigung`);
ok('Löschbestätigung druckbar', a.status === 200 && a.text.includes('LH-DEL-'));

// Zahlungen + Protokoll
a = await hole('/admin/zahlungen');
ok('Zahlungsübersicht', a.status === 200 && a.text.includes('13,08 €'), 'Summe 2 × 6,54');
a = await hole('/admin/protokoll');
ok('Protokoll führt die Zahlung', a.text.includes('Zahlung eingetragen'));

// Als Kunde anmelden
keksglas.delete('sitzung');
a = await sende('/anmelden', { benutzername: 'lea.berg', passwort: 'startpasswort1' });
ok('Kunden-Anmeldung', a.status === 302 && a.ort === '/meine-server', a.ort);
a = await hole('/meine-server');
ok('Kunde sieht seinen Server', a.text.includes('Klassenserver 8b'));
ok('Kunde sieht seine Subdomain', a.text.includes('klasse8b.lemon-servers.de'));
a = await hole('/admin');
ok('Kunde kommt nicht in die Verwaltung', a.status === 403);
a = await hole(`/meine-server/${serverId}`);
ok('Kunde sieht seine Zahlungen', a.status === 200 && a.text.includes('Zahlungen'));

// Fremder Server bleibt fremd
a = await sende('/anmelden', { benutzername: 'admin', passwort: process.env.ADMINPW });
keksglas.delete('sitzung'); // wieder als Lea
a = await sende('/anmelden', { benutzername: 'lea.berg', passwort: 'startpasswort1' });
a = await hole('/meine-server/9999');
ok('Fremder/unbekannter Server → 404', a.status === 404);

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
