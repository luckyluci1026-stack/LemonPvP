/**
 * Unterbenutzer: Wer darf was am Server eines anderen?
 *
 * Das Wichtigste steht unten: Ein Unterbenutzer darf genau das, was
 * angekreuzt wurde - und nichts daneben, auch nicht durch direktes
 * Aufrufen einer Adresse. Und er sieht nie, was der Server kostet.
 *
 *   ADMINPW=... node test/teilen.mjs
 */
const BASIS = 'http://127.0.0.1:3111';
let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

/** Jeder Benutzer bekommt sein eigenes Keksglas - wie ein eigener Browser. */
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
  return { h, s, anmelden, glas };
}

// ------------------------------------------------------------------ Aufbau
const admin = browser();
await admin.h('/');
let a = await admin.s('/anmelden', { benutzername: 'admin', passwort: process.env.ADMINPW });
ok('Admin angemeldet', a.status === 302);

await admin.s('/admin/kunden', { benutzername: 'lea.berg', passwort: 'passwortvonlea1' });
await admin.s('/admin/kunden', { benutzername: 'tom.klein', passwort: 'passwortvontom1' });
await admin.s('/admin/kunden', { benutzername: 'mia.fremd', passwort: 'passwortvonmia1' });

a = await admin.h('/admin/server/neu');
const leaId = (a.text.match(/<option value="(\d+)"[^>]*>lea\.berg/) || [])[1];
a = await admin.s('/admin/server/neu', {
  kundeId: leaId, name: 'Klassenserver', paket: 'coal', software: 'Paper' });
const id = Number((a.ort || '').match(/server\/(\d+)/)?.[1]);
ok('Server für Lea angelegt', Number.isInteger(id), '#' + id);

const lea = browser();
const tom = browser();
const mia = browser();
await lea.anmelden('lea.berg', 'passwortvonlea1');
await tom.anmelden('tom.klein', 'passwortvontom1');
await mia.anmelden('mia.fremd', 'passwortvonmia1');

// ------------------------------------------------------- Vor der Freigabe
a = await tom.h(`/panel/${id}`);
ok('Ohne Freigabe: der Server existiert für Tom nicht', a.status === 404);
a = await tom.h('/meine-server');
ok('Und steht auch nicht in seiner Liste', !a.text.includes('Klassenserver'));

a = await lea.h(`/panel/${id}`);
ok('Lea sieht den Freigabe-Bereich', a.text.includes('Wer darf mit?'));

// --------------------------------------------------------------- Freigeben
a = await lea.s(`/panel/${id}/freigeben`, { benutzername: 'gibtsnicht' });
ok('Ein unbekannter Zugang wird abgelehnt',
   decodeURIComponent(a.ort || '').includes('gibt es nicht'), a.ort);
a = await lea.s(`/panel/${id}/freigeben`, { benutzername: 'tom.klein' });
ok('Ohne angekreuztes Recht passiert nichts',
   decodeURIComponent(a.ort || '').includes('wenigstens ein Recht'));
a = await lea.s(`/panel/${id}/freigeben`, { benutzername: 'lea.berg', r_konsole: '1' });
ok('Sich selbst freizugeben ist sinnlos und wird gesagt',
   decodeURIComponent(a.ort || '').includes('Besitzer'));

a = await lea.s(`/panel/${id}/freigeben`,
  { benutzername: 'tom.klein', r_konsole: '1', r_dateien: '1' });
ok('Tom bekommt Konsole und Dateien',
   a.status === 302 && (a.ort || '').includes('ok='), decodeURIComponent(a.ort || ''));

// ------------------------------------------------------- Was Tom nun darf
a = await tom.h('/meine-server');
ok('Der Server steht jetzt in Toms Liste', a.text.includes('Klassenserver'));
ok('Und ist als geteilt gekennzeichnet', a.text.includes('>geteilt<'));
ok('Ohne Link auf die Kostenübersicht', !a.text.includes(`/meine-server/${id}`));

a = await tom.h(`/panel/${id}`);
ok('Tom kommt ins Panel', a.status === 200);
ok('Und sieht, dass es nicht seiner ist', a.text.includes('gehört jemand anderem'));
ok('Er sieht die Konsole', a.text.includes('id="konsole"'));
ok('Und die Befehlszeile', a.text.includes('id="befehlform"'));
ok('Aber keine Start-Knöpfe', !a.text.includes('data-was="start"'));
ok('Keinen Backup-Bereich', !a.text.includes('Backup jetzt anlegen'));
ok('Keinen Zeitplan', !a.text.includes('Jede Nacht neu starten'));
ok('Keine Serversoftware-Auswahl', !a.text.includes('id="software"'));
ok('Und keine Freigabeverwaltung', !a.text.includes('Wer darf mit?'));

a = await tom.h(`/panel/${id}/dateien`);
ok('Dateien darf er', a.status === 200);
a = await tom.s(`/panel/${id}/neu`, { p: '', name: 'notiz.txt', inhalt: 'von Tom' });
ok('Und auch anlegen', a.status === 302 && (a.ort || '').includes('ok='));

// ------------------------------------- Was er nicht darf, auch nicht direkt
a = await tom.s(`/panel/${id}/aktion`, { was: 'start' });
ok('Starten ist gesperrt – auch über die Adresse direkt', a.status === 403,
   'Status ' + a.status);
a = await tom.s(`/panel/${id}/sicherung`);
ok('Backup anlegen ist gesperrt', a.status === 403);
a = await tom.s(`/panel/${id}/plugin/installieren`, { datei: 'BetterSMP-1.0.0.jar' });
ok('Plugins sind gesperrt', a.status === 403);
a = await tom.s(`/panel/${id}/zeitplan`, { neustart_um: '04:00' });
ok('Zeitplan ist gesperrt', a.status === 403);
a = await tom.s(`/panel/${id}/freigeben`, { benutzername: 'mia.fremd', r_konsole: '1' });
ok('Weiterverschenken kann er nicht', a.status === 403);
a = await tom.h(`/meine-server/${id}`);
ok('Und die Kosten sieht er nicht', a.status === 404);

// ---------------------------------------------------------- Rechte ändern
a = await lea.s(`/panel/${id}/freigeben`,
  { benutzername: 'tom.klein', r_konsole: '1', r_steuern: '1' });
ok('Lea ändert die Rechte', a.status === 302 && (a.ort || '').includes('ok='));
a = await tom.s(`/panel/${id}/aktion`, { was: 'eula' });
ok('Jetzt darf Tom steuern', a.status === 302 && !String(a.ort).includes('403'));
a = await tom.h(`/panel/${id}/dateien`);
ok('Dafür sind die Dateien jetzt zu', a.status === 403);

// ------------------------------------------------------------ Dritte bleiben draußen
a = await mia.h(`/panel/${id}`);
ok('Mia kommt gar nicht rein', a.status === 404);
a = await mia.s(`/panel/${id}/befehl`, { befehl: 'op mia' });
ok('Auch nicht über einen Befehl', a.status === 404);

// ------------------------------------------------------------- Entziehen
a = await lea.h(`/panel/${id}`);
const tomId = (a.text.match(/name="kundeId" value="(\d+)"/) || [])[1];
a = await lea.s(`/panel/${id}/freigabe-weg`, { kundeId: tomId });
ok('Lea entzieht die Freigabe', a.status === 302 && (a.ort || '').includes('ok='));
a = await tom.h(`/panel/${id}`);
ok('Tom ist wieder draußen', a.status === 404);
a = await tom.h('/meine-server');
ok('Und der Server ist aus seiner Liste weg', !a.text.includes('Klassenserver'));

// --------------------------------------------------- Admin kommt überall hin
a = await admin.h(`/panel/${id}`);
ok('Der Admin darf weiterhin überall hinein', a.status === 200);

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
