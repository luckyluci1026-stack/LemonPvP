/**
 * Dateiverwaltung fuer einen Server.
 *
 * Der wichtigste Teil hier ist der Einsperr-Test in `innerhalb()`. Ohne
 * ihn koennte ein Kunde `../../etc/passwd` eintippen und waere ausserhalb
 * seines Ordners - bei einem Panel, auf dem mehrere Klassen ihre Server
 * haben, waere das der schlimmste denkbare Fehler. Deshalb wird jeder
 * Pfad aufgeloest und danach geprueft, ob er wirklich noch im Serverordner
 * liegt.
 */

import { readdirSync, statSync, readFileSync, writeFileSync, mkdirSync,
         rmSync, existsSync, createWriteStream, renameSync } from 'node:fs';
import { join, resolve, extname, dirname } from 'node:path';
import { ordnerVon } from './panel.js';

/** Was man im Editor gefahrlos oeffnen kann. */
const TEXT = new Set(['.yml', '.yaml', '.json', '.txt', '.properties', '.conf',
                      '.cfg', '.log', '.md', '.sh', '.bat', '.toml', '.ini', '.csv']);

/** Groesste Datei, die der Editor noch anfasst. */
const MAX_EDIT = 512 * 1024;

/**
 * Loest einen Pfad auf und stellt sicher, dass er im Serverordner bleibt.
 * Gibt null zurueck, wenn jemand ausbrechen will.
 */
export function innerhalb(serverId, pfad = '') {
  const wurzel = resolve(ordnerVon(serverId));
  // Fuehrende Trenner weg: "/etc/passwd" wuerde durch join() zwar nur zu
  // "<serverordner>/etc/passwd" und braeche damit nicht aus - aber es
  // legte einen Ordner an, den niemand wollte. Also gar nicht erst.
  const relativ = String(pfad || '').replace(/^[/\\]+/, '');
  const ziel = resolve(join(wurzel, relativ));
  // Der Vergleich braucht den Trenner am Ende, sonst wuerde "server/12"
  // auch "server/123" durchlassen.
  if (ziel !== wurzel && !ziel.startsWith(wurzel + '/') && !ziel.startsWith(wurzel + '\\')) {
    return null;
  }
  return ziel;
}

export const istText = (name) => TEXT.has(extname(name).toLowerCase());

export function liste(serverId, pfad = '') {
  const ordner = innerhalb(serverId, pfad);
  if (!ordner || !existsSync(ordner)) return null;
  const eintraege = [];
  for (const name of readdirSync(ordner)) {
    try {
      const s = statSync(join(ordner, name));
      eintraege.push({
        name,
        ordner: s.isDirectory(),
        groesse: s.size,
        geaendert: s.mtime.toISOString().slice(0, 16).replace('T', ' '),
        bearbeitbar: !s.isDirectory() && istText(name) && s.size <= MAX_EDIT,
      });
    } catch { /* verschwunden waehrend wir lesen */ }
  }
  // Ordner zuerst, dann alphabetisch
  eintraege.sort((a, b) => (b.ordner - a.ordner) || a.name.localeCompare(b.name));
  return eintraege;
}

export function lies(serverId, pfad) {
  const datei = innerhalb(serverId, pfad);
  if (!datei || !existsSync(datei)) return null;
  const s = statSync(datei);
  if (s.isDirectory() || s.size > MAX_EDIT) return null;
  return readFileSync(datei, 'utf8');
}

export function schreib(serverId, pfad, inhalt) {
  const datei = innerhalb(serverId, pfad);
  if (!datei) return 'Dieser Pfad liegt nicht im Serverordner.';
  try {
    mkdirSync(dirname(datei), { recursive: true });
    // Zeilenenden vereinheitlichen - der Browser schickt \r\n
    writeFileSync(datei, String(inhalt).replace(/\r\n/g, '\n'), 'utf8');
    belegungVergessen(serverId);
    return null;
  } catch (fehler) {
    return 'Konnte nicht gespeichert werden: ' + fehler.message;
  }
}

export function loesche(serverId, pfad) {
  const datei = innerhalb(serverId, pfad);
  if (!datei) return 'Dieser Pfad liegt nicht im Serverordner.';
  if (datei === resolve(ordnerVon(serverId))) return 'Der Serverordner selbst bleibt.';
  try {
    rmSync(datei, { recursive: true, force: true });
    belegungVergessen(serverId);
    return null;
  } catch (fehler) {
    return 'Konnte nicht gelöscht werden: ' + fehler.message;
  }
}

export function neuerOrdner(serverId, pfad) {
  const ziel = innerhalb(serverId, pfad);
  if (!ziel) return 'Dieser Pfad liegt nicht im Serverordner.';
  try {
    mkdirSync(ziel, { recursive: true });
    return null;
  } catch (fehler) {
    return 'Konnte nicht angelegt werden: ' + fehler.message;
  }
}

/**
 * Wie gross ist der Server insgesamt?
 *
 * Das laeuft ueber den ganzen Serverordner, und bei einer gewachsenen
 * Welt sind das schnell zehntausende Dateien. Auf jedem Seitenaufruf
 * waere das spuerbar - deshalb eine halbe Minute gemerkt. Genauer muss
 * eine Speicheranzeige nicht sein.
 */
const gemerkt = new Map();
const MERKDAUER = 30_000;

function summiere(ordner, tiefe = 0) {
  if (tiefe > 12) return 0;
  let summe = 0;
  let eintraege;
  try {
    eintraege = readdirSync(ordner, { withFileTypes: true });
  } catch {
    return 0;
  }
  for (const eintrag of eintraege) {
    const voll = join(ordner, eintrag.name);
    try {
      // Symlinks nicht verfolgen: sonst zaehlt ein Link auf "/" das
      // halbe Dateisystem mit und wir laufen im Kreis.
      if (eintrag.isSymbolicLink()) continue;
      summe += eintrag.isDirectory() ? summiere(voll, tiefe + 1) : statSync(voll).size;
    } catch { /* verschwunden waehrend wir lesen */ }
  }
  return summe;
}

export function belegung(serverId) {
  const alt = gemerkt.get(serverId);
  if (alt && Date.now() - alt.wann < MERKDAUER) return alt.summe;
  const summe = summiere(resolve(ordnerVon(serverId)));
  gemerkt.set(serverId, { wann: Date.now(), summe });
  return summe;
}

/** Nach einem Upload oder Loeschen stimmt der gemerkte Wert nicht mehr. */
export const belegungVergessen = (serverId) => gemerkt.delete(serverId);

/**
 * Einen Pfad aus der URL entschaerfen.
 *
 * Backslashes zu Schraegstrichen (Windows-Eingaben), doppelte Trenner
 * weg, keine leeren Stuecke. `innerhalb()` prueft danach noch einmal
 * richtig - das hier ist nur die Kosmetik davor.
 */
export function saeubere(pfad) {
  return String(pfad || '').replace(/\\/g, '/').split('/')
    .filter((teil) => teil && teil !== '.').join('/');
}

/** Ein Dateiname darf keine Ordner aufmachen. */
export function nameOk(name) {
  const sauber = String(name || '').trim();
  return sauber && !sauber.includes('/') && !sauber.includes('\\')
      && sauber !== '.' && sauber !== '..' ? sauber : null;
}

/** Groesste Datei, die per Upload durchgeht - eine Paper-Jar ist ~55 MB. */
export const MAX_UPLOAD = 512 * 1024 * 1024;

/**
 * Eine hochgeladene Datei stueckweise auf die Platte schreiben.
 *
 * Der Browser schickt sie als rohen Body, wir leiten sie direkt weiter.
 * Alles auf einmal in den Arbeitsspeicher zu laden ginge bei einer
 * 55-MB-Jar noch gut, bei einer Welt-Sicherung nicht mehr.
 *
 * Erst wird in eine `.teil`-Datei geschrieben und am Ende umbenannt.
 * Bricht die Verbindung ab, liegt also keine halbe server.jar herum, die
 * beim naechsten Start nur eine unverstaendliche Java-Meldung erzeugt.
 */
export function nimmDatei(serverId, pfad, name, anfrage) {
  return new Promise((fertig) => {
    const sauber = nameOk(name);
    if (!sauber) return fertig('Der Dateiname geht so nicht.');
    const ziel = innerhalb(serverId, saeubere(pfad) + '/' + sauber);
    if (!ziel) return fertig('Dieser Pfad liegt nicht im Serverordner.');

    const vorlaeufig = ziel + '.teil';
    try {
      mkdirSync(dirname(ziel), { recursive: true });
    } catch (fehler) {
      return fertig('Ordner nicht anlegbar: ' + fehler.message);
    }

    let bytes = 0;
    let abgebrochen = null;
    const strom = createWriteStream(vorlaeufig);

    const abbruch = (grund) => {
      if (abgebrochen) return;
      abgebrochen = grund;
      anfrage.unpipe(strom);
      strom.destroy();
      try { rmSync(vorlaeufig, { force: true }); } catch { /* egal */ }
      fertig(grund);
    };

    anfrage.on('data', (stueck) => {
      bytes += stueck.length;
      if (bytes > MAX_UPLOAD) abbruch('Die Datei ist zu groß (über 512 MB).');
    });
    anfrage.on('error', () => abbruch('Der Upload ist abgebrochen.'));
    strom.on('error', (fehler) => abbruch('Konnte nicht schreiben: ' + fehler.message));

    strom.on('finish', () => {
      if (abgebrochen) return;
      try {
        renameSync(vorlaeufig, ziel);
        belegungVergessen(serverId);
        fertig(null);
      } catch (fehler) {
        fertig('Konnte nicht ablegen: ' + fehler.message);
      }
    });

    anfrage.pipe(strom);
  });
}

export function lesbareGroesse(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB';
}
