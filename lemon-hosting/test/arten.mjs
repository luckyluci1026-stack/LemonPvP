/**
 * Prueft das Installieren der Serversoftware.
 *
 * Die Hersteller-APIs werden hier nicht angerufen - ein Test, der von
 * papermc.io abhaengt, schlaegt irgendwann fehl, ohne dass jemand etwas
 * kaputt gemacht hat. Stattdessen bekommt `ARTEN` eine eigene Art, die
 * auf einen kleinen Server nebenan zeigt. Der Weg dahinter ist derselbe:
 * herunterladen, Groesse pruefen, umbenennen.
 *
 * Was damit wirklich geprueft wird, ist das, was schiefgehen kann - eine
 * Fehlerseite als server.jar abzulegen, eine halbe Datei zurueckzulassen
 * oder die heile alte zu zerstoeren.
 */
import { createServer } from 'node:http';
import { mkdtempSync, rmSync, existsSync, statSync, writeFileSync,
         readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const arbeit = mkdtempSync(join(tmpdir(), 'arten-'));
process.env.SERVER_DIR = join(arbeit, 'server');

const arten = await import('../src/arten.js');
const prozess = await import('../src/panel.js');

let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

// -------------------------------------------------------------- Pruefungen
ok('Bekannte Arten werden angenommen',
   arten.artOk('paper') === 'paper' && arten.artOk('purpur') === 'purpur');
ok('Unbekannte nicht',
   ['', 'gibtsnicht', '../etc', null].every((x) => arten.artOk(x) === ''));
ok('Vier Arten stehen zur Auswahl', arten.artenListe().length === 4,
   arten.artenListe().map((a) => a.name).join(', '));
ok('Paper ist als empfohlen markiert',
   arten.artenListe().find((a) => a.id === 'paper')?.empfohlen === true);

// Die Version landet in einer URL - da darf nichts durchrutschen.
ok('Gültige Versionen gehen durch',
   ['1.21.11', '1.20', '24w14a', '3.4.0-SNAPSHOT'].every((v) => arten.versionOk(v) === v));
ok('Wegwerf-Versionen fallen raus',
   ['../../etc/passwd', 'a b', '', 'x'.repeat(40), '/1.21', '?x=1', '../']
     .every((v) => arten.versionOk(v) === ''),
   'Pfadangaben und Leerzeichen');

// ------------------------------------------------------------ Testquelle
const echteJar = Buffer.alloc(300 * 1024, 0x50);   // gross genug
let liefere = () => ({ status: 200, koerper: echteJar });

const quelle = createServer((anfrage, antwort) => {
  const d = liefere(anfrage);
  antwort.writeHead(d.status, { 'Content-Type': 'application/java-archive' });
  antwort.end(d.koerper);
});
await new Promise((f) => quelle.listen(8395, f));

arten.ARTEN.probe = {
  id: 'probe', name: 'Probe',
  beschreibung: 'Nur für den Test.',
  versionen: async () => ['2.0', '1.0'],
  datei: async (v) => `http://127.0.0.1:8395/probe-${v}.jar`,
};

const ordner = prozess.ordnerVon(1);
const jarPfad = join(ordner, 'server.jar');

// ------------------------------------------------------------ Installieren
let ergebnis = await arten.installiere(1, 'probe', '1.0');
ok('Installieren legt die server.jar ab', !ergebnis.fehler && existsSync(jarPfad),
   ergebnis.fehler || `${Math.round(ergebnis.groesse / 1024)} KB`);
ok('Und sie ist vollständig', statSync(jarPfad).size === echteJar.length);
ok('Keine .teil-Datei bleibt liegen',
   !readdirSync(ordner).some((n) => n.endsWith('.teil')), readdirSync(ordner).join(', '));

// ------------------------------------------- Eine Fehlerseite ist keine Jar
writeFileSync(jarPfad, 'alte heile Datei');
liefere = () => ({ status: 200, koerper: Buffer.from('<html>404 Not Found</html>') });
ergebnis = await arten.installiere(1, 'probe', '1.0');
ok('Eine winzige Antwort wird als Fehlerseite erkannt',
   Boolean(ergebnis.fehler) && ergebnis.fehler.includes('Fehlerseite'), ergebnis.fehler);
ok('Und die alte Datei bleibt heil',
   readFileSync(jarPfad, 'utf8') === 'alte heile Datei');
ok('Auch hier bleibt keine .teil-Datei liegen',
   !readdirSync(ordner).some((n) => n.endsWith('.teil')), readdirSync(ordner).join(', '));

// -------------------------------------------------------- Fehler vom Server
liefere = () => ({ status: 503, koerper: Buffer.from('kaputt') });
ergebnis = await arten.installiere(1, 'probe', '1.0');
ok('Ein Fehler des Herstellers wird gemeldet',
   Boolean(ergebnis.fehler) && ergebnis.fehler.includes('503'), ergebnis.fehler);
ok('Die alte Datei ist immer noch da',
   readFileSync(jarPfad, 'utf8') === 'alte heile Datei');

// ------------------------------------------------------- Unsinnige Eingaben
ergebnis = await arten.installiere(1, 'gibtsnicht', '1.0');
ok('Unbekannte Art wird abgelehnt', ergebnis.fehler?.includes('gibt es nicht'));
ergebnis = await arten.installiere(1, 'probe', '../../etc/passwd');
ok('Unsinnige Version wird abgelehnt', ergebnis.fehler?.includes('Version'),
   ergebnis.fehler);

// ------------------------------------------------------------- Versionsliste
const liste = await arten.versionen('probe');
ok('Versionsliste kommt an', liste.liste?.length === 2, (liste.liste || []).join(', '));
ok('Und wird gemerkt', (await arten.versionen('probe')).liste.length === 2);
ok('Unbekannte Art hat keine Liste',
   Boolean((await arten.versionen('nixda')).fehler));

// Wenn die Quelle ausfaellt, bleibt die gemerkte Liste stehen
arten.ARTEN.probe.versionen = async () => { throw new Error('Netz weg'); };
arten.vergissVersionen();
const ohne = await arten.versionen('probe');
ok('Ohne Netz und ohne gemerkte Liste kommt eine Meldung',
   Boolean(ohne.fehler) && ohne.fehler.includes('nicht erreichbar'), ohne.fehler);

quelle.close();
rmSync(arbeit, { recursive: true, force: true });
console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
