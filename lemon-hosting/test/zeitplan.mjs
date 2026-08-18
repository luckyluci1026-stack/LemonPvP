/**
 * Prueft den Zeitplan, ohne bis vier Uhr morgens zu warten.
 *
 * `pruefe()` nimmt die Zeit als Argument - deshalb laesst sich hier
 * einfach behaupten, es sei 04:00, und nachsehen, was passiert.
 */
import { mkdtempSync, rmSync, writeFileSync, mkdirSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const ordner = mkdtempSync(join(tmpdir(), 'zeitplan-'));
process.env.SERVER_DIR = join(ordner, 'server');

const db = await import('../src/db.js');
const plan = await import('../src/zeitplan.js');
const prozess = await import('../src/panel.js');

db.oeffne(join(ordner, 'test.db'));

let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

const um = (hhmm) => {
  const [h, m] = hhmm.split(':').map(Number);
  const d = new Date(2026, 7, 18, h, m, 0);
  return d;
};

// ---------------------------------------------------------------- Uhrzeiten
ok('Gültige Uhrzeit wird übernommen', plan.zeitOk('04:00') === '04:00');
ok('Führende Null wird ergänzt', plan.zeitOk('4:05') === '04:05', plan.zeitOk('4:05'));
ok('Unsinn wird verworfen',
   ['25:00', '12:60', 'morgen', '4', '04:0', '-1:00', ''].every((x) => plan.zeitOk(x) === ''));
ok('Nächster Termin heute oder morgen',
   plan.naechster('23:00', um('08:00')) === 'heute 23:00'
   && plan.naechster('04:00', um('08:00')) === 'morgen 04:00');

// ------------------------------------------------------------------ Aufbau
const kundeId = db.kundeAnlegen({ benutzername: 'lea', passwort: 'geheimgeheim' });
const a = db.serverAnlegen({ kundeId, name: 'Mit Plan', paket: 'coal' });
const b = db.serverAnlegen({ kundeId, name: 'Ohne Plan', paket: 'coal' });
const c = db.serverAnlegen({ kundeId, name: 'In Pterodactyl', paket: 'coal',
  pterodactyl: 'https://panel.example.de/server/x' });

for (const id of [a, b, c]) {
  mkdirSync(prozess.ordnerVon(id), { recursive: true });
  writeFileSync(join(prozess.ordnerVon(id), 'server.properties'), 'motd=Test\n');
}
db.serverAendern(a, { neustart_um: '04:00', sicherung_um: '03:00' });
db.serverAendern(c, { sicherung_um: '03:00' });

const still = () => {};

// ---------------------------------------------------------------- Backups
let getan = await plan.pruefe(um('02:59'), still);
ok('Eine Minute vorher passiert nichts', getan.length === 0);

getan = await plan.pruefe(um('03:00'), still);
const sicherungen = getan.filter((g) => g.art === 'sicherung');
ok('Um 03:00 wird gesichert', sicherungen.length === 1 && !sicherungen[0].fehler,
   JSON.stringify(getan));
ok('Nur der Server mit Zeitplan', sicherungen[0]?.server === a);
ok('Der Pterodactyl-Server wird in Ruhe gelassen',
   !getan.some((g) => g.server === c));

getan = await plan.pruefe(um('03:00'), still);
ok('In derselben Minute nicht noch einmal', getan.length === 0);

const dateien = readdirSync(join(ordner, 'sicherungen', String(a)));
ok('Das Backup liegt wirklich da', dateien.length === 1, dateien.join(', '));

// -------------------------------------------------------------- Neustarts
getan = await plan.pruefe(um('04:00'), still);
const neustarts = getan.filter((g) => g.art === 'neustart');
ok('Um 04:00 steht der Neustart an', neustarts.length === 1);
ok('Ein gestoppter Server wird nicht heimlich hochgefahren',
   neustarts[0].uebersprungen === true);
ok('Und das steht im Protokoll',
   db.protokollListe(20).some((p) => p.was === 'Neustart übersprungen'));

// ------------------------------------- Neustart bei laufendem Server
// Ein Ersatz-Server, der sich wie Minecraft verhaelt: meldet "Done (…)"
// und geht auf "stop" von selbst.
const jar = process.env.ERSATZ_JAR;
if (jar) {
  const { copyFileSync } = await import('node:fs');
  copyFileSync(jar, join(prozess.ordnerVon(a), 'server.jar'));
  writeFileSync(join(prozess.ordnerVon(a), 'eula.txt'), 'eula=true\n');
  prozess.starte(db.server(a));
  const bis = async (p, sek) => {
    const ende = Date.now() + sek * 1000;
    while (Date.now() < ende) {
      if (p()) return true;
      await new Promise((f) => setTimeout(f, 200));
    }
    return false;
  };
  ok('Ersatzserver läuft', await bis(() => prozess.status(a).status === 'laeuft', 30));
  const pidVorher = prozess.status(a).pid;

  getan = await plan.pruefe(new Date(2026, 7, 19, 4, 0, 0), still);
  ok('Zeitplan startet den laufenden Server neu',
     getan.some((g) => g.art === 'neustart' && !g.uebersprungen && !g.fehler));
  ok('Danach läuft ein anderer Prozess',
     await bis(() => prozess.status(a).status === 'laeuft'
                  && prozess.status(a).pid !== pidVorher, 40),
     `${pidVorher} → ${prozess.status(a).pid}`);
  await prozess.alleStoppen(5);
} else {
  console.log('     (Neustart-Probe übersprungen – ERSATZ_JAR nicht gesetzt)');
}

// ---------------------------------------------------------- Am naechsten Tag
const morgen = new Date(2026, 7, 19, 3, 0, 0);
getan = await plan.pruefe(morgen, still);
ok('Am nächsten Tag wieder', getan.filter((g) => g.art === 'sicherung').length === 1);

// ------------------------------------------------------- Archivierter Server
db.serverAendern(a, { status: 'archiviert' });
getan = await plan.pruefe(new Date(2026, 7, 20, 3, 0, 0), still);
ok('Ein archivierter Server wird nicht gesichert', getan.length === 0);

rmSync(ordner, { recursive: true, force: true });
console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
