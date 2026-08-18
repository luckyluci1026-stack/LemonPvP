/**
 * Backups: Welt einpacken, herunterladen, zurueckspielen.
 *
 * Bei einem Panel fuer Schulklassen ist das die haeufigste Bitte
 * ueberhaupt - "unsere Welt ist kaputt, kannst du die von gestern
 * wiederholen". Deshalb soll es ein Knopf sein und kein Gang zum Admin.
 *
 * Die Backups liegen NEBEN dem Serverordner, nicht darin:
 *
 *   server/7/            der Server
 *   sicherungen/7/       seine Backups
 *
 * Laegen sie darin, packte das naechste Backup alle vorherigen mit ein,
 * und die Datei verdoppelte sich jedes Mal. Ausserdem zaehlten sie in der
 * Speicheranzeige mit, obwohl sie nicht zum Server gehoeren.
 */

import { mkdirSync, readdirSync, statSync, rmSync, existsSync } from 'node:fs';
import { join, resolve, basename } from 'node:path';
import { wurzel as serverWurzel, ordnerVon, laeuft, befehl } from './panel.js';
import { packe, entpacke, sammle } from './zip.js';
import { innerhalb, belegungVergessen, lesbareGroesse } from './dateien.js';

/** Wie viele Backups je Server aufgehoben werden. */
export const WIE_VIELE = 5;

export function backupOrdner(serverId) {
  const pfad = join(resolve(serverWurzel(), '..'), 'sicherungen', String(serverId));
  mkdirSync(pfad, { recursive: true });
  return pfad;
}

/**
 * Ein Dateiname, der nichts anderes sein kann als ein Backup.
 *
 * Der Name kommt spaeter aus einem Formular zurueck, um heruntergeladen
 * oder geloescht zu werden. Ein strenges Muster ist dort mehr wert als
 * jede nachtraegliche Pruefung.
 */
const MUSTER = /^\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}(-\d+)?\.zip$/;

export const nameOk = (name) => MUSTER.test(String(name || '')) ? String(name) : null;

/**
 * Ein Name aus Datum und Uhrzeit - und notfalls einer Nummer dahinter.
 *
 * Die Sekunde allein reicht nicht: Ein Doppelklick auf "Backup anlegen"
 * erzeugte sonst zweimal denselben Namen, und das zweite Backup
 * ueberschriebe das erste stillschweigend. Aufgefallen ist das beim
 * Ausprobieren - in einer Reihe von sieben Backups fehlte plötzlich eins.
 */
function neuerName(serverId) {
  const d = new Date();
  const z = (n) => String(n).padStart(2, '0');
  const stamm = `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`
              + `_${z(d.getHours())}-${z(d.getMinutes())}-${z(d.getSeconds())}`;
  const ordner = backupOrdner(serverId);
  if (!existsSync(join(ordner, stamm + '.zip'))) return stamm + '.zip';
  for (let n = 2; n < 100; n++) {
    if (!existsSync(join(ordner, `${stamm}-${n}.zip`))) return `${stamm}-${n}.zip`;
  }
  return `${stamm}-${Date.now() % 1000}.zip`;
}

export function liste(serverId) {
  const ordner = backupOrdner(serverId);
  const raus = [];
  for (const name of readdirSync(ordner)) {
    if (!MUSTER.test(name)) continue;
    try {
      const s = statSync(join(ordner, name));
      raus.push({
        name,
        groesse: s.size,
        lesbar: lesbareGroesse(s.size),
        wann: name.slice(0, 10).split('-').reverse().join('.')
            + ' um ' + name.slice(11, 19).replaceAll('-', ':'),
      });
    } catch { /* gerade geloescht */ }
  }
  return raus.sort((a, b) => b.name.localeCompare(a.name));
}

/**
 * Vor dem Packen die Welt auf die Platte zwingen.
 *
 * Minecraft haelt geaenderte Chunks im Arbeitsspeicher und schreibt sie
 * nur alle paar Minuten weg. Ein Backup mitten im Betrieb erwischt sonst
 * einen Stand von vorhin - oder, schlimmer, einen halb geschriebenen
 * Chunk. "save-all" erledigt das in einem Rutsch; danach kurz warten,
 * denn der Befehl kommt sofort zurueck, das Schreiben dauert.
 *
 * Absichtlich kein "save-off" davor: Wenn das Portal zwischendurch
 * abstuerzte, bliebe das Speichern dauerhaft aus, und niemand wuesste
 * warum. Ein paar Chunks Unschaerfe sind der bessere Preis.
 */
async function welteSpeichern(serverId) {
  if (!laeuft(serverId)) return false;
  befehl(serverId, 'save-all');
  await new Promise((fertig) => setTimeout(fertig, 3000));
  return true;
}

/**
 * Ein Backup anlegen.
 *
 * Laeuft der Server, wird vorher gespeichert und danach in der Meldung
 * gesagt, dass er lief - ganz sauber ist nur ein Backup bei gestopptem
 * Server. Verboten ist es trotzdem nicht: ein leicht schiefes Backup ist
 * besser als keines.
 */
export async function anlegen(serverId) {
  const lief = await welteSpeichern(serverId);
  const quelle = ordnerVon(serverId);
  const dateien = sammle(quelle);
  if (!dateien.length) return { fehler: 'Im Serverordner liegt noch nichts.' };

  const ziel = join(backupOrdner(serverId), neuerName(serverId));
  const fehler = packe(quelle, dateien, ziel);
  if (fehler) {
    try { rmSync(ziel, { force: true }); } catch { /* egal */ }
    return { fehler };
  }

  aufraeumen(serverId);
  const s = statSync(ziel);
  return {
    name: basename(ziel),
    dateien: dateien.length,
    groesse: lesbareGroesse(s.size),
    warnung: lief
      ? 'Der Server lief dabei – die Welt wurde vorher gespeichert, ganz '
        + 'sauber ist ein Backup aber nur bei gestopptem Server.' : '',
  };
}

/** Nur die letzten WIE_VIELE aufheben, damit die Platte nicht volllaeuft. */
function aufraeumen(serverId) {
  const alle = liste(serverId);
  for (const alt of alle.slice(WIE_VIELE)) {
    try { rmSync(join(backupOrdner(serverId), alt.name), { force: true }); } catch { /* egal */ }
  }
}

export function pfadVon(serverId, name) {
  const sauber = nameOk(name);
  if (!sauber) return null;
  const voll = join(backupOrdner(serverId), sauber);
  return existsSync(voll) ? voll : null;
}

export function loeschen(serverId, name) {
  const voll = pfadVon(serverId, name);
  if (!voll) return 'Dieses Backup gibt es nicht.';
  try {
    rmSync(voll, { force: true });
    return null;
  } catch (fehler) {
    return 'Konnte nicht gelöscht werden: ' + fehler.message;
  }
}

/**
 * Ein Backup zurueckspielen.
 *
 * Nur bei gestopptem Server - Dateien unter einem laufenden Minecraft
 * auszutauschen endet zuverlaessig in einer kaputten Welt.
 *
 * Die Zieldateien gehen durch denselben Einsperr-Test wie der
 * Dateimanager. Ein praepariertes Archiv mit `../../etc/passwd` im Namen
 * schreibt damit nichts ausserhalb des Serverordners, sondern wird
 * gezaehlt und uebersprungen.
 */
export function zurueckspielen(serverId, name) {
  if (laeuft(serverId)) {
    return { fehler: 'Stopp den Server, bevor du ein Backup zurückspielst.' };
  }
  const voll = pfadVon(serverId, name);
  if (!voll) return { fehler: 'Dieses Backup gibt es nicht.' };

  const ergebnis = entpacke(voll, (eintrag) => innerhalb(serverId, eintrag));
  if (typeof ergebnis === 'string') return { fehler: ergebnis };

  belegungVergessen(serverId);
  return {
    entpackt: ergebnis.entpackt,
    warnung: ergebnis.uebersprungen
      ? `${ergebnis.uebersprungen} Einträge im Archiv wurden übersprungen – `
        + 'sie wollten außerhalb des Serverordners landen.'
      : '',
  };
}
