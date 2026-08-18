/**
 * Das Noetigste fuer einen Webserver - ohne Framework.
 *
 * Node bringt alles mit, was hier gebraucht wird. Ein Framework waere
 * eine weitere Sache, die installiert, aktuell gehalten und verstanden
 * werden muss; fuer ein Portal mit zwanzig Seiten lohnt das nicht.
 */

import { readFile } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';
import { randomBytes } from 'node:crypto';

/** Text so einsetzen, dass ein < im Servernamen kein HTML wird. */
export function esc(wert) {
  return String(wert ?? '')
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

export function cookies(anfrage) {
  const raus = {};
  for (const teil of (anfrage.headers.cookie || '').split(';')) {
    const i = teil.indexOf('=');
    if (i > 0) raus[teil.slice(0, i).trim()] = decodeURIComponent(teil.slice(i + 1).trim());
  }
  return raus;
}

export async function formular(anfrage, maxBytes = 256 * 1024) {
  return new Promise((fertig, fehler) => {
    let roh = '';
    anfrage.on('data', (stueck) => {
      roh += stueck;
      if (roh.length > maxBytes) { anfrage.destroy(); fehler(new Error('Zu gross')); }
    });
    anfrage.on('end', () => {
      const daten = {};
      for (const [k, v] of new URLSearchParams(roh)) {
        // Mehrfach gesetzte Felder (Checkboxen) als Liste
        if (k in daten) daten[k] = [].concat(daten[k], v);
        else daten[k] = v;
      }
      fertig(daten);
    });
    anfrage.on('error', fehler);
  });
}

/**
 * Ein schon gesetztes Set-Cookie darf nicht verlorengehen.
 *
 * writeHead ersetzt, was vorher per setHeader gesetzt wurde. Beim
 * Anmelden werden aber zwei Kekse gebraucht: das CSRF-Cookie vom
 * Seitenaufruf und das neue Sitzungs-Cookie. Deshalb hier zusammenfuehren
 * statt ueberschreiben.
 */
function mitKeksen(antwort, kopf) {
  const vorher = antwort.getHeader('Set-Cookie');
  if (!vorher || !kopf['Set-Cookie']) return kopf;
  return { ...kopf, 'Set-Cookie': [].concat(vorher, kopf['Set-Cookie']) };
}

export function sende(antwort, html, status = 200, kopf = {}) {
  antwort.writeHead(status, mitKeksen(antwort, {
    'Content-Type': 'text/html; charset=utf-8',
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'same-origin',
    ...kopf,
  }));
  antwort.end(html);
}

export function weiter(antwort, ziel, kopf = {}) {
  antwort.writeHead(302, mitKeksen(antwort, { Location: ziel, ...kopf }));
  antwort.end();
}

export function setzeCookie(name, wert, tage = 14) {
  const ab = new Date(Date.now() + tage * 86400000).toUTCString();
  return `${name}=${encodeURIComponent(wert)}; Path=/; HttpOnly; SameSite=Lax; Expires=${ab}`;
}

export const loescheCookie = (name) =>
  `${name}=; Path=/; HttpOnly; SameSite=Lax; Expires=Thu, 01 Jan 1970 00:00:00 GMT`;

const TYPEN = {
  '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon',
};

export async function statisch(antwort, wurzel, pfad) {
  const sauber = normalize(pfad).replace(/^(\.\.[/\\])+/, '');
  const datei = join(wurzel, sauber);
  if (!datei.startsWith(wurzel)) return false;
  try {
    const inhalt = await readFile(datei);
    antwort.writeHead(200, {
      'Content-Type': TYPEN[extname(datei)] || 'application/octet-stream',
      'Cache-Control': 'public, max-age=300',
    });
    antwort.end(inhalt);
    return true;
  } catch {
    return false;
  }
}

/**
 * CSRF-Schutz nach dem Doppel-Absende-Verfahren.
 *
 * Jeder Besucher bekommt ein Cookie mit einer Zufallszahl, und dieselbe
 * Zahl steckt als verstecktes Feld in jedem Formular. Beim Absenden
 * muessen beide uebereinstimmen. Eine fremde Seite kann zwar ein Formular
 * an uns schicken, aber den Cookie-Wert nicht auslesen - damit faellt der
 * Versuch auf.
 *
 * Das Zeichen haengt bewusst NICHT an der Anmeldung: Konfigurator und
 * Anmeldeformular werden ja gerade von Leuten benutzt, die noch keine
 * Sitzung haben. Genau daran ist die erste Fassung gescheitert.
 */
export function csrfWert(anfrage) {
  return cookies(anfrage).csrf || '';
}

export const csrfNeu = () => randomBytes(24).toString('hex');

export function csrfStimmt(ausCookie, ausFormular) {
  if (!ausCookie || !ausFormular || ausCookie.length !== ausFormular.length) return false;
  // Zeichenweise vergleichen, ohne bei der ersten Abweichung abzubrechen
  let gleich = 0;
  for (let i = 0; i < ausCookie.length; i++) {
    gleich |= ausCookie.charCodeAt(i) ^ ausFormular.charCodeAt(i);
  }
  return gleich === 0;
}

/** Ein sehr einfacher Router: pfad -> handler, mit :platzhaltern. */
export class Router {
  constructor() { this.regeln = []; }

  auf(methode, muster, handler) {
    const teile = muster.split('/').filter(Boolean);
    this.regeln.push({ methode, teile, handler });
    return this;
  }

  get(muster, handler) { return this.auf('GET', muster, handler); }
  post(muster, handler) { return this.auf('POST', muster, handler); }

  finde(methode, pfad) {
    const teile = pfad.split('/').filter(Boolean);
    for (const regel of this.regeln) {
      if (regel.methode !== methode || regel.teile.length !== teile.length) continue;
      const werte = {};
      let passt = true;
      for (let i = 0; i < teile.length; i++) {
        const m = regel.teile[i];
        if (m.startsWith(':')) werte[m.slice(1)] = decodeURIComponent(teile[i]);
        else if (m !== teile[i]) { passt = false; break; }
      }
      if (passt) return { handler: regel.handler, werte };
    }
    return null;
  }
}
