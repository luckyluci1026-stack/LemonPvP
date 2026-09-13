/**
 * Die Zwei-Faktor-Anmeldung.
 *
 * Zwei Teile. Der erste rechnet nach, ob das Verfahren stimmt - mit den
 * Testwerten aus RFC 6238 selbst. Das ist der Beweis, dass die
 * Authenticator-Apps dieselben Zahlen sehen werden wie das Portal; alles
 * andere waere eine Behauptung, die man erst mit dem Handy in der Hand
 * widerlegt.
 *
 * Der zweite geht die Anmeldung durch, wie sie wirklich stattfindet:
 * Passwort, Code, drin. Und die Faelle, die zaehlen - falscher Code,
 * derselbe Code zweimal, Ersatzcode, und ob man mit halber Anmeldung
 * schon irgendwo hinkommt.
 *
 *   ADMINPW=... node test/zweifach.mjs
 */
const z = await import('../src/zweifach.js');

const BASIS = 'http://127.0.0.1:3111';
let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

// ======================================================== Teil 1: Rechnen

// RFC 6238, Anhang B. Geheimnis "12345678901234567890" als ASCII, SHA-1.
const RFC = z.base32Kodiere(Buffer.from('12345678901234567890'));
ok('Base32 stimmt mit dem RFC überein',
   RFC === 'GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ', RFC);

const VEKTOREN = [
  [59, '94287082'], [1111111109, '07081804'], [1111111111, '14050471'],
  [1234567890, '89005924'], [2000000000, '69279037'], [20000000000, '65353130'],
];
let alle = true;
for (const [t, soll] of VEKTOREN) {
  const ist = z.code(RFC, t, 8);
  if (ist !== soll) { alle = false; console.log(`     T=${t}: ${ist}, erwartet ${soll}`); }
}
ok('Alle sechs Testwerte aus RFC 6238 stimmen', alle,
   `bis T=${VEKTOREN.at(-1)[0]} — also auch jenseits von 2³²`);

ok('Sechsstellig sind es die letzten sechs Ziffern',
   z.code(RFC, 59) === '287082', z.code(RFC, 59));

// Base32 hin und zurück
const geheim = z.neuesGeheimnis();
ok('Ein neues Geheimnis ist 32 Zeichen lang', geheim.length === 32, geheim.slice(0, 8) + '…');
ok('Und besteht nur aus dem Base32-Alphabet', /^[A-Z2-7]+$/.test(geheim));
ok('Hin und zurück ergibt dasselbe',
   z.base32Kodiere(z.base32Dekodiere(geheim)) === geheim);
ok('Abgetippte Leerzeichen stören nicht',
   z.base32Kodiere(z.base32Dekodiere(z.lesbar(geheim))) === geheim, z.lesbar(geheim).slice(0, 14) + '…');
ok('Kleinbuchstaben auch nicht',
   z.base32Kodiere(z.base32Dekodiere(geheim.toLowerCase())) === geheim);
ok('Ein falsches Zeichen wird abgelehnt', z.base32Dekodiere('ABC!DEF') === null);

// Der Spielraum bei ungenauen Uhren
const jetzt = 1700000000;
ok('Der Code der laufenden Minute geht durch',
   z.pruefe(geheim, z.code(geheim, jetzt), jetzt) === z.schrittVon(jetzt));
ok('Einer aus dem Schritt davor auch',
   z.pruefe(geheim, z.code(geheim, jetzt - 30), jetzt) !== null,
   'ein Handy, das eine halbe Minute nachgeht');
ok('Und einer aus dem Schritt danach',
   z.pruefe(geheim, z.code(geheim, jetzt + 30), jetzt) !== null);
ok('Zwei Schritte daneben nicht mehr',
   z.pruefe(geheim, z.code(geheim, jetzt + 90), jetzt) === null,
   'sonst gölte ein abgefangener Code zu lange');
ok('Ein falscher Code geht nicht', z.pruefe(geheim, '000000', jetzt) === null
   || z.code(geheim, jetzt) === '000000');
ok('Buchstaben auch nicht', z.pruefe(geheim, 'abcdef', jetzt) === null);
ok('Und nichts auch nicht',
   z.pruefe(geheim, '', jetzt) === null && z.pruefe(geheim, null, jetzt) === null);

// Der zurückgegebene Schritt ist das, was den Zweitgebrauch verhindert
const s1 = z.pruefe(geheim, z.code(geheim, jetzt), jetzt);
const s2 = z.pruefe(geheim, z.code(geheim, jetzt + 30), jetzt + 30);
ok('Der Schritt kommt zurück und wächst', s2 === s1 + 1, `${s1} → ${s2}`);

// Der Link
const link = z.link('lea.berg', geheim);
ok('Der otpauth-Link fängt richtig an', link.startsWith('otpauth://totp/'));
ok('Er trägt Geheimnis, Herausgeber und Verfahren',
   link.includes('secret=' + geheim) && link.includes('issuer=Lemon%20Hosting')
   && link.includes('algorithm=SHA1') && link.includes('digits=6')
   && link.includes('period=30'), link.slice(0, 60) + '…');
ok('Der Kontoname steht kodiert im Pfad', link.includes('Lemon%20Hosting%3Alea.berg'));

// Ersatzcodes
const codes = z.neueErsatzcodes();
ok('Es gibt acht Ersatzcodes', codes.length === 8, codes[0]);
ok('Alle verschieden', new Set(codes).size === 8);
ok('Und in einer Form, die man abtippen kann',
   codes.every((c) => /^[A-Z2-7]{5}-[A-Z2-7]{5}$/.test(c)), codes[0]);
ok('Der Hash ist unabhängig von Groß- und Kleinschreibung',
   z.ersatzHash(codes[0]) === z.ersatzHash(codes[0].toLowerCase()));

// ==================================================== Teil 2: Die Anmeldung
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
  return { h, s, glas };
}

const admin = browser();
await admin.h('/');
let a = await admin.s('/anmelden', { benutzername: 'admin', passwort: process.env.ADMINPW });
ok('Als Admin angemeldet', a.status === 302, a.ort);
await admin.s('/admin/kunden', { benutzername: 'nina.roth', passwort: 'passwortvonnina1' });

const nina = browser();
await nina.h('/');
a = await nina.s('/anmelden', { benutzername: 'nina.roth', passwort: 'passwortvonnina1' });
ok('Ohne zweiten Faktor geht es direkt hinein', a.status === 302, a.ort);

a = await nina.h('/sicherheit');
ok('Die Sicherheitsseite lädt', a.status === 200 && a.text.includes('Zwei-Faktor'));
ok('Und sagt, dass er aus ist', a.text.includes('Einrichten'));

// -------------------------------------------------------- Einschalten
a = await nina.s('/sicherheit/vorbereiten');
a = await nina.h('/sicherheit');
const meins = (a.text.match(/<pre class="konsole"[^>]*>([A-Z2-7]{32})<\/pre>/) || [])[1];
ok('Das Geheimnis steht zum Abtippen da', Boolean(meins), meins?.slice(0, 8) + '…');
ok('Der otpauth-Link auch', a.text.includes('otpauth://totp/'));
ok('Solange es nicht bestätigt ist, gilt es nicht', a.text.includes('Einschalten')
   && !a.text.includes('Abschalten'), 'noch kein zweiter Faktor');

a = await nina.s('/sicherheit/an', { code: '000000', passwort: 'passwortvonnina1' });
ok('Ein falscher Code schaltet nicht ein',
   a.text.includes('Der Code stimmt nicht'));
a = await nina.s('/sicherheit/an', { code: z.code(meins), passwort: 'falsch' });
ok('Ohne Passwort auch nicht', a.text.includes('Passwort stimmt nicht'),
   'sonst sperrt ein offen stehender Browser den Besitzer aus');

a = await nina.s('/sicherheit/an',
  { code: z.code(meins), passwort: 'passwortvonnina1' });
ok('Mit beidem schon', a.text.includes('fragt das Portal beim Anmelden'));
const ersatz = [...a.text.matchAll(/([A-Z2-7]{5}-[A-Z2-7]{5})/g)].map((m) => m[1]);
ok('Und die Ersatzcodes stehen einmal da', ersatz.length === 8, ersatz[0]);

a = await nina.h('/sicherheit');
ok('Danach sind sie weg', !a.text.includes(ersatz[0]));
ok('Der zweite Faktor steht auf an',
   a.text.includes('Abschalten') && a.text.includes('8 Ersatzcodes offen'));

// ---------------------------------------------------- Anmelden mit Code
const zweit = browser();
await zweit.h('/');
a = await zweit.s('/anmelden', { benutzername: 'nina.roth', passwort: 'passwortvonnina1' });
ok('Das Passwort allein führt jetzt nicht mehr hinein',
   a.status === 200 && a.text.includes('Noch ein Schritt'), 'Status ' + a.status);

a = await zweit.h('/meine-server');
ok('Halb angemeldet kommt man nirgendwohin',
   a.status === 302 && (a.ort || '').includes('/anmelden'), a.ort);
a = await zweit.h('/anmelden');
ok('Und landet wieder beim Code, nicht beim Passwort',
   (a.ort || '') === '/anmelden/code', a.ort);

a = await zweit.s('/anmelden/code', { code: '000000' });
ok('Ein falscher Code kommt nicht durch', a.text.includes('Der Code stimmt nicht'));
a = await zweit.h('/meine-server');
ok('Auch danach nicht', (a.ort || '').includes('/anmelden'));

const guter = z.code(meins);
a = await zweit.s('/anmelden/code', { code: guter });
ok('Der richtige Code führt hinein', a.status === 302 && a.ort === '/meine-server', a.ort);
a = await zweit.h('/meine-server');
ok('Und jetzt geht die Seite auch', a.status === 200);

// ------------------------------------------- Derselbe Code kein zweites Mal
const dritt = browser();
await dritt.h('/');
await dritt.s('/anmelden', { benutzername: 'nina.roth', passwort: 'passwortvonnina1' });
a = await dritt.s('/anmelden/code', { code: guter });
ok('Derselbe Code geht kein zweites Mal',
   a.status === 200 && a.text.includes('schon benutzt'),
   'wer ihn über die Schulter abliest, sitzt noch daneben');

// ------------------------------------------------------- Ersatzcode
a = await dritt.s('/anmelden/code', { code: ersatz[0], ersatz: '1' });
ok('Mit einem Ersatzcode kommt man herein', a.status === 302 && a.ort === '/meine-server',
   a.ort);
const viert = browser();
await viert.h('/');
await viert.s('/anmelden', { benutzername: 'nina.roth', passwort: 'passwortvonnina1' });
a = await viert.s('/anmelden/code', { code: ersatz[0], ersatz: '1' });
ok('Derselbe Ersatzcode aber nur einmal',
   a.status === 200 && a.text.includes('schon verbraucht'));
a = await viert.s('/anmelden/code', { code: ersatz[1], ersatz: '1' });
ok('Der nächste geht wieder', a.status === 302, a.ort);

a = await nina.h('/sicherheit');
ok('Und das Portal zählt mit', a.text.includes('6 Ersatzcodes offen'),
   (a.text.match(/\d+ Ersatzcodes? offen/) || [])[0]);

// -------------------------------------------------- Neue Ersatzcodes
a = await nina.s('/sicherheit/ersatz', { passwort: 'falsch' });
ok('Ohne Passwort keine neuen Ersatzcodes', a.text.includes('Passwort stimmt nicht'));
a = await nina.s('/sicherheit/ersatz', { passwort: 'passwortvonnina1' });
const frisch = [...a.text.matchAll(/([A-Z2-7]{5}-[A-Z2-7]{5})/g)].map((m) => m[1]);
ok('Mit Passwort schon', frisch.length === 8 && frisch[0] !== ersatz[0]);

const fuenft = browser();
await fuenft.h('/');
await fuenft.s('/anmelden', { benutzername: 'nina.roth', passwort: 'passwortvonnina1' });
a = await fuenft.s('/anmelden/code', { code: ersatz[2], ersatz: '1' });
ok('Die alten Ersatzcodes gelten nicht mehr', a.status === 200,
   'sonst brächte das Erneuern nichts');

// ------------------------------------------------- Der Admin als Notausgang
a = await admin.h('/admin/kunden');
const ninaId = (a.text.match(/href="\/admin\/kunde\/(\d+)"[\s\S]{0,300}?nina\.roth/)
   || a.text.match(/nina\.roth[\s\S]{0,300}?href="\/admin\/kunde\/(\d+)"/) || [])[1];
a = await admin.h(`/admin/kunde/${ninaId}`);
ok('Die Verwaltung zeigt den zweiten Faktor', a.text.includes('Zwei-Faktor')
   && a.text.includes('Abschalten'), '#' + ninaId);

a = await admin.s(`/admin/kunde/${ninaId}/zweifach-aus`);
ok('Der Admin kann ihn abschalten',
   decodeURIComponent(a.ort || '').includes('abgeschaltet'),
   decodeURIComponent(a.ort || ''));

const sechst = browser();
await sechst.h('/');
a = await sechst.s('/anmelden', { benutzername: 'nina.roth', passwort: 'passwortvonnina1' });
ok('Danach reicht das Passwort wieder', a.status === 302 && a.ort === '/meine-server', a.ort);

// -------------------------------------------------- Selbst abschalten
await sechst.s('/sicherheit/vorbereiten');
a = await sechst.h('/sicherheit');
const zweitesGeheim = (a.text.match(/<pre class="konsole"[^>]*>([A-Z2-7]{32})<\/pre>/) || [])[1];
await sechst.s('/sicherheit/an',
  { code: z.code(zweitesGeheim), passwort: 'passwortvonnina1' });
a = await sechst.h('/sicherheit');
ok('Neu eingerichtet', a.text.includes('Abschalten'));

a = await sechst.s('/sicherheit/aus',
  { passwort: 'passwortvonnina1', code: '000000' });
ok('Ohne gültigen Code bleibt er an', a.text.includes('Der Code stimmt nicht'));
a = await sechst.s('/sicherheit/aus', { passwort: 'falsch', code: z.code(zweitesGeheim) });
ok('Ohne Passwort auch', a.text.includes('Passwort stimmt nicht'));
a = await sechst.s('/sicherheit/aus',
  { passwort: 'passwortvonnina1', code: z.code(zweitesGeheim, Math.floor(Date.now() / 1000) + 30) });
ok('Mit beidem geht es aus', decodeURIComponent(a.ort || '').includes('Abgeschaltet'),
   decodeURIComponent(a.ort || ''));

// -------------------------------------------------- Passwort ändern
a = await sechst.s('/sicherheit/passwort',
  { alt: 'falsch', neu: 'neuespasswort1', neu2: 'neuespasswort1' });
ok('Ohne das alte Passwort keine Änderung', a.text.includes('bisherige Passwort stimmt nicht'));
a = await sechst.s('/sicherheit/passwort',
  { alt: 'passwortvonnina1', neu: 'kurz', neu2: 'kurz' });
ok('Ein zu kurzes wird abgelehnt', a.text.includes('zu kurz'));
a = await sechst.s('/sicherheit/passwort',
  { alt: 'passwortvonnina1', neu: 'neuespasswort1', neu2: 'anderes123456' });
ok('Zwei verschiedene auch', a.text.includes('nicht gleich'));
a = await sechst.s('/sicherheit/passwort',
  { alt: 'passwortvonnina1', neu: 'neuespasswort1', neu2: 'neuespasswort1' });
ok('Sonst wird geändert und abgemeldet', a.status === 302 && a.ort === '/anmelden', a.ort);

const siebt = browser();
await siebt.h('/');
a = await siebt.s('/anmelden', { benutzername: 'nina.roth', passwort: 'neuespasswort1' });
ok('Mit dem neuen Passwort geht es', a.status === 302 && a.ort === '/meine-server');
a = await siebt.h('/meine-server');
ok('Und die alte Sitzung ist tot', (await sechst.h('/meine-server')).status === 302);

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
