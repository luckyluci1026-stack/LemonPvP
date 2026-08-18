/**
 * Klickt das Portal einmal durch wie ein echter Benutzer.
 *
 * Der wichtigste Block steht ganz unten: ob ein Kunde an fremde Server
 * kommt und ob sich aus dem Dateimanager ausbrechen laesst. Alles andere
 * waere aergerlich - das waere schlimm.
 */
const BASIS = 'http://127.0.0.1:3111';
const keksglas = new Map();
const kekse = () => [...keksglas].map(([k, v]) => `${k}=${v}`).join('; ');
let fehler = 0;

const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

async function hole(pfad, optionen = {}) {
  const a = await fetch(BASIS + pfad, {
    ...optionen, redirect: 'manual',
    headers: { ...(optionen.headers || {}),
               ...(keksglas.size ? { cookie: kekse() } : {}) },
  });
  for (const roh of a.headers.getSetCookie?.() || []) {
    const [name, ...rest] = roh.split(';')[0].split('=');
    keksglas.set(name, rest.join('='));
  }
  return { status: a.status, ort: a.headers.get('location'), text: await a.text() };
}

async function sende(pfad, felder) {
  // Das CSRF-Zeichen steht im Cookie - einmal irgendeine Seite geholt zu
  // haben reicht, um es zu bekommen.
  if (!keksglas.has('csrf')) await hole('/');
  const koerper = new URLSearchParams({ csrf: keksglas.get('csrf'), ...felder });
  return hole(pfad, { method: 'POST', body: koerper,
    headers: { 'content-type': 'application/x-www-form-urlencoded' } });
}

/** Rohen Body schicken - so laedt der Browser Dateien hoch. */
const schiebe = (pfad, inhalt) => hole(pfad, { method: 'POST', body: inhalt,
  headers: { 'content-type': 'application/octet-stream' } });

const anmelden = (name, passwort) => {
  keksglas.delete('sitzung');
  return sende('/anmelden', { benutzername: name, passwort });
};

// ------------------------------------------------------------- oeffentlich
let a = await hole('/');
ok('Startseite lädt', a.status === 200 && a.text.includes('Lemon Hosting'));
ok('Alle drei Pakete sichtbar',
   ['Wood', 'Coal', 'Diamond'].every((p) => a.text.includes(p)));
ok('Wood-Preis stimmt', a.text.includes('3,99 €'));

a = await hole('/preise.json');
ok('Preise als JSON', JSON.parse(a.text).pakete.coal.preis === 5.99);

a = await hole('/regeln');
ok('Regelseite lädt', a.status === 200 && a.text.includes('Prepaid'));

a = await hole('/meine-server');
ok('Ohne Anmeldung kein Kundenbereich', a.status === 302 && a.ort === '/anmelden');
a = await hole('/admin');
ok('Ohne Anmeldung keine Verwaltung', a.status === 302);
a = await hole('/panel/1');
ok('Ohne Anmeldung kein Panel', a.status === 302 && a.ort === '/anmelden');

a = await hole('/anmelden', { method: 'POST',
  body: new URLSearchParams({ benutzername: 'admin', passwort: 'x' }),
  headers: { 'content-type': 'application/x-www-form-urlencoded' } });
ok('POST ohne CSRF wird abgelehnt', a.status === 400);

// ------------------------------------------------------------- Anfrage
a = await sende('/konfigurator', {
  paket: 'wood', z_subdomain: '1', z_flfac: '1', z_ram1: '2',
  vorname: 'Lea', nachname: 'Berg', klasse: '8b', mcname: 'LeaB',
  kontakt: 'lea#1234', servername: 'Klassenserver 8b', subdomain: 'klasse8b',
  software: 'Paper', wunsch: 'Bitte mit Worldedit', regeln: '1',
});
ok('Anfrage angenommen', a.status === 200 && a.text.includes('eingegangen'));
ok('Preis in der Bestätigung', a.text.includes('6,54 €'),
   'Wood 3,99 + Subdomain 0,25 + FLFAC 0,30 + 2 GB 2,00');

a = await sende('/anmelden', { benutzername: 'admin', passwort: 'falsch' });
ok('Falsches Passwort wird abgelehnt', a.text.includes('stimmt nicht'));

a = await anmelden('admin', process.env.ADMINPW);
ok('Admin-Anmeldung', a.status === 302 && a.ort === '/admin', a.ort);

a = await hole('/admin');
ok('Verwaltung lädt', a.status === 200 && a.text.includes('Offene Anfragen'));
ok('Anfrage in der Übersicht', a.text.includes('Klassenserver 8b'));
ok('Verwaltung zeigt den Laufstatus', a.text.includes('Läuft gerade'));

a = await sende('/admin/anfrage/1/annehmen', {
  kundeId: '', benutzername: 'lea.berg', passwort: 'startpasswort1' });
ok('Anfrage angenommen → Server angelegt',
   a.status === 302 && /\/admin\/server\/\d+/.test(a.ort || ''), a.ort);
const serverId = Number((a.ort || '').match(/server\/(\d+)/)?.[1]);

a = await hole(`/admin/server/${serverId}`);
ok('Serverseite lädt', a.status === 200 && a.text.includes('Klassenserver 8b'));
ok('Zusatzleistungen übernommen', a.text.includes('value="2"'), '2 GB RAM');
ok('Preis auf der Serverseite', a.text.includes('6,54 €'));

// Ein zweiter Kunde mit eigenem Server - fuer die Zugriffsprobe unten.
await sende('/admin/kunden', { benutzername: 'tom.klein', passwort: 'anderespasswort1' });
a = await hole('/admin/server/neu');
const tomId = (a.text.match(/<option value="(\d+)"[^>]*>tom\.klein/) || [])[1];
a = await sende('/admin/server/neu', {
  kundeId: tomId, name: 'Toms Server', paket: 'coal', software: 'Paper' });
const fremdId = Number((a.ort || '').match(/server\/(\d+)/)?.[1]);
ok('Zweiter Server angelegt', Number.isInteger(fremdId), '#' + fremdId);

// Ports: jeder Server muss einen eigenen bekommen, sonst startet der
// zweite nie.
a = await hole(`/admin/server/${serverId}`);
const portA = Number((a.text.match(/name="port"[^>]*value="(\d+)"/) || [])[1]);
a = await hole(`/admin/server/${fremdId}`);
const portB = Number((a.text.match(/name="port"[^>]*value="(\d+)"/) || [])[1]);
ok('Erster Server bekam 25565', portA === 25565, String(portA));
ok('Zweiter Server bekam einen anderen Port', portB && portB !== portA,
   `${portA} / ${portB}`);

// ------------------------------------------------------------- Dokumente
a = await hole(`/admin/server/${serverId}/bestellbogen`);
ok('Bestellbogen druckbar', a.status === 200 && a.text.includes('Bestellbogen'));
ok('Bestellbogen ist vorausgefüllt',
   a.text.includes('Lea') && a.text.includes('Klassenserver 8b') && a.text.includes('klasse8b'));
ok('Bestellbogen kreuzt das richtige Paket an', /☑.*<strong>Wood/.test(a.text));
ok('Bestellbogen zeigt die Summe', a.text.includes('6,54 €'));
ok('Zahlungsfelder bleiben leer zum Ausfüllen',
   a.text.includes('Betrag erhalten') && a.text.includes('Kassiert von'));

a = await hole(`/admin/server/${serverId}/loeschbestaetigung`);
ok('Löschbestätigung druckbar', a.status === 200 && a.text.includes('LH-DEL-'));

// ------------------------------------------------------------- Panel
a = await anmelden('lea.berg', 'startpasswort1');
ok('Kunden-Anmeldung', a.status === 302 && a.ort === '/meine-server', a.ort);

a = await hole('/meine-server');
ok('Kunde sieht seinen Server', a.text.includes('Klassenserver 8b'));
ok('Kunde sieht seine Subdomain', a.text.includes('klasse8b.lemon-servers.de'));
ok('Panel-Knopf auf der Karte', a.text.includes(`href="/panel/${serverId}"`));

a = await hole(`/panel/${serverId}`);
ok('Panel lädt', a.status === 200 && a.text.includes('Konsole'));
ok('Panel meldet die fehlende server.jar', a.text.includes('fehlt noch die Serversoftware'));
ok('Panel zeigt den gebuchten Speicher', a.text.includes('4.50 GB'),
   'Wood 2,5 GB + 2 × 1 GB Zusatz-RAM');
ok('Konsolenskript eingebunden', a.text.includes('/konsole.js'));
// Das Panel muss sagen, woran man ist - eine harte Grenze im Container
// oder nur eine JVM-Einstellung. Beides ist in Ordnung, Schweigen nicht.
const imContainer = a.text.includes('Läuft im Container');
ok('Panel benennt die Betriebsart',
   imContainer || a.text.includes('Läuft ohne Container'),
   imContainer ? 'im Container' : 'ohne Container');
ok('Und sagt dazu, was das für den Speicher heißt',
   a.text.includes(imContainer ? 'harte Grenze im Container'
                               : 'nur eine JVM-Einstellung'));
ok('Panel nennt die Verbindungsadresse',
   a.text.includes('klasse8b.lemon-servers.de'),
   'Port 25565 wird weggelassen');
ok('Panel hat eine Spielerliste',
   a.text.includes('id="spieler"') && a.text.includes('id="spielerzahl"'));

a = await sende(`/panel/${serverId}/aktion`, { was: 'start' });
ok('Start ohne server.jar erklärt sich',
   a.status === 302 && decodeURIComponent(a.ort || '').includes('keine server.jar'), a.ort);

a = await sende(`/panel/${serverId}/befehl`, { befehl: 'say hallo' });
ok('Befehl an gestoppten Server wird abgelehnt',
   a.status === 409 && JSON.parse(a.text).fehler.includes('läuft nicht'));

// Konsole als Ereignisstrom: die erste Nachricht muss der Verlauf sein.
const strom = await fetch(`${BASIS}/panel/${serverId}/konsole`, { headers: { cookie: kekse() } });
const leser = strom.body.getReader();
const anfang = new TextDecoder().decode((await leser.read()).value);
ok('Konsole liefert einen Ereignisstrom',
   strom.headers.get('content-type').startsWith('text/event-stream'));
ok('Konsole schickt Verlauf und Status',
   anfang.includes('event: verlauf') && anfang.includes('event: status'));
await leser.cancel();

// ------------------------------------------------------------- Dateien
a = await hole(`/panel/${serverId}/dateien`);
ok('Dateiliste lädt', a.status === 200 && a.text.includes('Der Ordner ist leer'));

a = await sende(`/panel/${serverId}/neu`, {
  p: '', name: 'server.properties', inhalt: 'max-players=20\nmotd=Klasse 8b\n' });
ok('Datei angelegt', a.status === 302 && (a.ort || '').includes('ok='), a.ort);

a = await sende(`/panel/${serverId}/ordner`, { p: '', name: 'plugins' });
ok('Ordner angelegt', a.status === 302 && (a.ort || '').includes('ok='));

a = await hole(`/panel/${serverId}/dateien`);
ok('Beides steht in der Liste',
   a.text.includes('server.properties') && a.text.includes('plugins'));

a = await hole(`/panel/${serverId}/bearbeiten?p=server.properties`);
ok('Editor öffnet die Datei', a.status === 200 && a.text.includes('max-players=20'));

a = await sende(`/panel/${serverId}/speichern`,
  { p: 'server.properties', inhalt: 'max-players=40\r\nmotd=Neu\r\n' });
ok('Datei gespeichert', a.status === 302);
a = await hole(`/panel/${serverId}/bearbeiten?p=server.properties`);
ok('Änderung ist drin', a.text.includes('max-players=40'));
ok('Windows-Zeilenenden bereinigt', !a.text.includes('max-players=40\r'));

// Upload: roher Body, Zeichen in der URL
const zeichen = encodeURIComponent(keksglas.get('csrf'));
a = await schiebe(
  `/panel/${serverId}/hochladen?p=plugins&name=Test.jar&csrf=${zeichen}`, 'PKnicht-echt');
ok('Datei hochgeladen', a.status === 200 && JSON.parse(a.text).ok === true);
a = await hole(`/panel/${serverId}/dateien?p=plugins`);
ok('Hochgeladene Datei ist da', a.text.includes('Test.jar'));

a = await schiebe(`/panel/${serverId}/hochladen?p=&name=Boese.jar&csrf=falsch`, 'x');
ok('Upload ohne gültiges Zeichen wird abgelehnt', a.status === 400);

a = await sende(`/panel/${serverId}/loeschen`, { p: 'plugins/Test.jar' });
a = await hole(`/panel/${serverId}/dateien?p=plugins`);
ok('Datei gelöscht', !a.text.includes('Test.jar'));

a = await sende(`/panel/${serverId}/aktion`, { was: 'eula' });
a = await hole(`/panel/${serverId}/dateien`);
ok('EULA-Knopf legt eula.txt an', a.text.includes('eula.txt'));

// ------------------------------------------------------------- Plugins
a = await hole(`/panel/${serverId}`);
const imKatalog = [...a.text.matchAll(/name="datei" value="([^"]+)"/g)].map((m) => m[1]);
ok('Katalog zeigt Plugins an', imKatalog.length > 0, imKatalog.length + ' Stück');
ok('Mit Beschreibung', a.text.includes('Ränge mit Farbverlauf'));

const einPlugin = imKatalog.find((n) => n.startsWith('BetterSMP')) || imKatalog[0];
a = await sende(`/panel/${serverId}/plugin/installieren`, { datei: einPlugin });
ok('Plugin installiert', a.status === 302 && (a.ort || '').includes('ok='),
   decodeURIComponent(a.ort || '').slice(0, 60));

a = await hole(`/panel/${serverId}/dateien?p=plugins`);
ok('Die Jar liegt wirklich in plugins/', a.text.includes(einPlugin), einPlugin);
a = await hole(`/panel/${serverId}`);
ok('Panel zeigt es als installiert', a.text.includes('>drin<'));

// Ein Name, der nicht im Katalog steht, darf nichts bewirken.
a = await sende(`/panel/${serverId}/plugin/installieren`,
  { datei: '../../../etc/passwd' });
ok('Erfundenes Plugin wird abgelehnt',
   decodeURIComponent(a.ort || '').includes('nicht im Katalog'), a.ort);
a = await sende(`/panel/${serverId}/plugin/entfernen`, { datei: 'Gibtsnicht-1.0.jar' });
ok('Entfernen von Unbekanntem wird abgelehnt',
   decodeURIComponent(a.ort || '').includes('nicht im Katalog'));

a = await sende(`/panel/${serverId}/plugin/entfernen`, { datei: einPlugin });
ok('Plugin wieder entfernt', a.status === 302 && (a.ort || '').includes('ok='));
a = await hole(`/panel/${serverId}/dateien?p=plugins`);
ok('Und die Jar ist weg', !a.text.includes(einPlugin));

// ------------------------------------------------------------- Zeitplan
a = await hole(`/panel/${serverId}`);
ok('Panel hat einen Zeitplan-Bereich',
   a.text.includes('Jede Nacht neu starten um') && a.text.includes('Jeden Tag sichern um'));

a = await sende(`/panel/${serverId}/zeitplan`,
  { neustart_um: '04:00', sicherung_um: '3:30' });
ok('Zeitplan gespeichert', a.status === 302 && (a.ort || '').includes('ok='));
a = await hole(`/panel/${serverId}`);
ok('Uhrzeiten stehen im Formular',
   a.text.includes('value="04:00"') && a.text.includes('value="03:30"'),
   '3:30 wurde zu 03:30 ergänzt');
ok('Panel nennt den nächsten Termin', /nächster: (heute|morgen) 04:00/.test(a.text));

a = await sende(`/panel/${serverId}/zeitplan`,
  { neustart_um: '25:99', sicherung_um: 'irgendwann' });
a = await hole(`/panel/${serverId}`);
ok('Unsinnige Uhrzeiten werden verworfen',
   a.text.includes('kein automatischer Neustart')
   && a.text.includes('kein automatisches Backup'));

// ------------------------------------------------------------- Backups
a = await hole(`/panel/${serverId}`);
ok('Panel hat einen Backup-Bereich',
   a.text.includes('Backups') && a.text.includes('Noch kein Backup'));

a = await sende(`/panel/${serverId}/sicherung`, {});
ok('Backup angelegt', a.status === 302 && (a.ort || '').includes('ok='),
   decodeURIComponent(a.ort || '').slice(0, 70));

a = await hole(`/panel/${serverId}`);
const sicherung = (a.text.match(/f=(\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}(?:-\d+)?\.zip)/) || [])[1];
ok('Backup steht in der Liste', Boolean(sicherung), sicherung);

const zip = await fetch(`${BASIS}/panel/${serverId}/sicherung/laden?f=${sicherung}`,
  { headers: { cookie: kekse() } });
const rohZip = Buffer.from(await zip.arrayBuffer());
ok('Backup lässt sich herunterladen', zip.status === 200
   && zip.headers.get('content-type') === 'application/zip');
ok('Und es ist wirklich ein ZIP', rohZip.subarray(0, 2).toString() === 'PK',
   `${rohZip.length} Bytes`);
ok('Der Download hat einen sprechenden Namen',
   (zip.headers.get('content-disposition') || '').includes('klasse8b-'),
   zip.headers.get('content-disposition'));

// Ein erfundener Name darf nirgendwo hinführen.
a = await hole(`/panel/${serverId}/sicherung/laden?f=../../../etc/passwd`);
ok('Erfundener Backup-Name führt ins Leere', a.status === 404);
a = await hole(`/panel/${serverId}/sicherung/laden?f=server.properties`);
ok('Auch keine normale Datei über den Backup-Weg', a.status === 404);

// Zurückspielen: Datei kaputt machen, Backup einspielen, prüfen.
await sende(`/panel/${serverId}/speichern`,
  { p: 'server.properties', inhalt: 'kaputt\n' });
a = await sende(`/panel/${serverId}/sicherung/zurueck`, { f: sicherung });
ok('Backup zurückgespielt', a.status === 302 && (a.ort || '').includes('ok='),
   decodeURIComponent(a.ort || '').slice(0, 60));
a = await hole(`/panel/${serverId}/bearbeiten?p=server.properties`);
ok('Der alte Inhalt ist wieder da',
   a.text.includes('max-players=40') && !a.text.includes('kaputt'));

// ------------------------------------------------------- Ausbruchsversuche
for (const boese of ['../../etc/passwd', '..', `../${fremdId}/geheim`,
                     'welt/../../../hoppla']) {
  a = await sende(`/panel/${serverId}/neu`,
    { p: '', name: boese, inhalt: 'hier war ich' });
  ok(`Ausbruch abgewehrt: ${boese}`,
     decodeURIComponent(a.ort || '').includes('geht so nicht'),
     a.ort ? '' : 'Status ' + a.status);
}

a = await hole(`/panel/${serverId}/dateien?p=../../`);
ok('Ausbruch über die Ordnerliste greift nicht', a.status === 404, 'Status ' + a.status);
a = await hole(`/panel/${serverId}/dateien?p=plugins`);
ok('Echte Unterordner gehen weiter auf', a.status === 200);

a = await sende(`/panel/${serverId}/loeschen`, { p: '' });
ok('Der Serverordner selbst bleibt stehen',
   decodeURIComponent(a.ort || '').includes('selbst bleibt'), a.ort);

// ------------------------------------------------------- fremde Server
a = await hole(`/panel/${fremdId}`);
ok('Fremdes Panel bleibt zu', a.status === 404);
a = await hole(`/panel/${fremdId}/dateien`);
ok('Fremde Dateien bleiben zu', a.status === 404);
a = await sende(`/panel/${fremdId}/aktion`, { was: 'start' });
ok('Fremder Server lässt sich nicht starten', a.status === 404);
a = await schiebe(`/panel/${fremdId}/hochladen?p=&name=x.txt&csrf=${zeichen}`, 'x');
ok('In fremde Server lässt sich nichts hochladen', a.status === 404);
a = await hole('/admin');
ok('Kunde kommt nicht in die Verwaltung', a.status === 403);
a = await hole('/meine-server/9999');
ok('Unbekannter Server → 404', a.status === 404);

// ------------------------------------------------------- Pterodactyl
a = await anmelden('admin', process.env.ADMINPW);
await sende(`/admin/server/${serverId}`, {
  name: 'Klassenserver 8b', paket: 'wood', software: 'Paper', status: 'aktiv',
  subdomain: 'klasse8b', z_ram1: '2', z_subdomain: '1', z_flfac: '1',
  pterodactyl: 'https://panel.example.de/server/a1b2c3d4' });
a = await hole(`/panel/${serverId}`);
ok('Panel verlinkt nach Pterodactyl',
   a.text.includes('läuft in Pterodactyl')
   && a.text.includes('https://panel.example.de/server/a1b2c3d4'));
ok('Keine eigene Konsole für fremd verwaltete Server', !a.text.includes('id="konsole"'));
a = await sende(`/panel/${serverId}/aktion`, { was: 'start' });
ok('Start am Portal vorbei wird verweigert', a.status === 409, 'Status ' + a.status);
a = await hole(`/panel/${serverId}/dateien`);
ok('Auch die Dateien bleiben Pterodactyl überlassen', a.status === 409);

await sende(`/admin/server/${serverId}`, {
  name: 'Klassenserver 8b', paket: 'wood', software: 'Paper', status: 'aktiv',
  subdomain: 'klasse8b', z_ram1: '2', z_subdomain: '1', z_flfac: '1',
  pterodactyl: 'javascript:alert(1)' });
a = await hole(`/panel/${serverId}`);
ok('javascript:-Adresse wird verworfen',
   !a.text.includes('javascript:alert') && a.text.includes('id="konsole"'));

// ------------------------------------------------------- Admin sieht alles
a = await hole(`/panel/${serverId}`);
ok('Admin darf in jedes Panel', a.status === 200 && a.text.includes('Klassenserver 8b'));
a = await hole('/admin/protokoll');
ok('Protokoll führt die Panel-Aktionen',
   a.text.includes('Datei hochgeladen') && a.text.includes('EULA angenommen'));

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
