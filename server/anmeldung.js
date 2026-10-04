const crypto = require('node:crypto');
const path = require('node:path');
const { promisify } = require('node:util');
const { DATEN } = require('./pfade');
const { jsonLesen, jsonSchreiben } = require('./speicher');
const { Fehler, cookiesLesen, istHttps } = require('./antwort');

const scrypt = promisify(crypto.scrypt);

const ZUGANG_DATEI = path.join(DATEN, 'zugang.json');
const COOKIE_NAME = 'bucksmp_admin';
const COOKIE_PFAD = '/admin/';
const SITZUNG_DAUER = 12 * 60 * 60 * 1000;
const MAX_FEHLVERSUCHE = 5;
const SPERRZEIT = 15 * 60 * 1000;
const PASSWORT_ZEICHEN = 'abcdefghjkmnpqrstuvwxyz23456789';

const sitzungen = new Map();
const fehlversuche = new Map();

function zufallsPasswort() {
  const gruppen = [];
  for (let gruppe = 0; gruppe < 4; gruppe += 1) {
    let teil = '';
    for (let zeichen = 0; zeichen < 4; zeichen += 1) {
      teil += PASSWORT_ZEICHEN[crypto.randomInt(PASSWORT_ZEICHEN.length)];
    }
    gruppen.push(teil);
  }
  return gruppen.join('-');
}

async function hashBerechnen(passwort, salz) {
  const hash = await scrypt(passwort.normalize('NFC'), salz, 64);
  return hash.toString('hex');
}

async function zugangLesen() {
  try {
    const zugang = await jsonLesen(ZUGANG_DATEI, null);
    if (zugang && typeof zugang.salz === 'string' && typeof zugang.hash === 'string') {
      return zugang;
    }
  } catch {
    return null;
  }
  return null;
}

async function passwortSetzen(passwort) {
  const salz = crypto.randomBytes(16).toString('hex');
  const hash = await hashBerechnen(passwort, salz);
  await jsonSchreiben(ZUGANG_DATEI, { salz, hash, geaendert: new Date().toISOString() });
}

async function zugangEinrichten() {
  if (await zugangLesen()) {
    return null;
  }
  const vorgabe = process.env.ADMIN_PASSWORT || '';
  if (vorgabe.length >= 8) {
    await passwortSetzen(vorgabe);
    return { ausUmgebung: true };
  }
  const passwort = zufallsPasswort();
  await passwortSetzen(passwort);
  return { passwort, vorgabeZuKurz: vorgabe !== '' };
}

async function passwortStimmt(passwort) {
  const zugang = await zugangLesen();
  if (!zugang || typeof passwort !== 'string' || passwort === '' || passwort.length > 200) {
    return false;
  }
  const versuch = Buffer.from(await hashBerechnen(passwort, zugang.salz), 'hex');
  const echt = Buffer.from(zugang.hash, 'hex');
  return versuch.length === echt.length && crypto.timingSafeEqual(versuch, echt);
}

function absenderVon(anfrage) {
  if (process.env.HINTER_PROXY) {
    const weitergeleitet = String(anfrage.headers['x-forwarded-for'] || '').split(',')[0].trim();
    if (weitergeleitet) {
      return weitergeleitet;
    }
  }
  return anfrage.socket.remoteAddress || 'unbekannt';
}

function wartezeit(absender) {
  const eintrag = fehlversuche.get(absender);
  if (!eintrag || eintrag.gesperrtBis <= Date.now()) {
    return 0;
  }
  return eintrag.gesperrtBis - Date.now();
}

function fehlversuchMerken(absender) {
  const jetzt = Date.now();
  let eintrag = fehlversuche.get(absender);
  if (!eintrag || eintrag.seit + SPERRZEIT < jetzt) {
    eintrag = { anzahl: 0, seit: jetzt, gesperrtBis: 0 };
  }
  eintrag.anzahl += 1;
  if (eintrag.anzahl >= MAX_FEHLVERSUCHE) {
    eintrag.gesperrtBis = jetzt + SPERRZEIT;
    eintrag.anzahl = 0;
    eintrag.seit = jetzt;
  }
  fehlversuche.set(absender, eintrag);
}

function sperreBeachten(absender) {
  const warten = wartezeit(absender);
  if (warten > 0) {
    const minuten = Math.ceil(warten / 60000);
    throw new Fehler(429, `Zu viele falsche Versuche. Bitte warte ${minuten} ${minuten === 1 ? 'Minute' : 'Minuten'}.`);
  }
}

function sitzungsCookie(token, sicher) {
  const teile = [`${COOKIE_NAME}=${token}`, `Path=${COOKIE_PFAD}`, 'HttpOnly', 'SameSite=Strict'];
  if (sicher) {
    teile.push('Secure');
  }
  return teile.join('; ');
}

function abmeldeCookie(anfrage) {
  const teile = [`${COOKIE_NAME}=`, `Path=${COOKIE_PFAD}`, 'HttpOnly', 'SameSite=Strict', 'Max-Age=0'];
  if (istHttps(anfrage)) {
    teile.push('Secure');
  }
  return teile.join('; ');
}

async function anmelden(anfrage, passwort) {
  const absender = absenderVon(anfrage);
  sperreBeachten(absender);
  if (!(await passwortStimmt(passwort))) {
    fehlversuchMerken(absender);
    throw new Fehler(401, 'Das Passwort stimmt nicht.');
  }
  fehlversuche.delete(absender);
  const token = crypto.randomBytes(32).toString('hex');
  sitzungen.set(token, { ablauf: Date.now() + SITZUNG_DAUER });
  return sitzungsCookie(token, istHttps(anfrage));
}

function sitzungVon(anfrage) {
  const token = cookiesLesen(anfrage)[COOKIE_NAME];
  if (!token) {
    return null;
  }
  const sitzung = sitzungen.get(token);
  if (!sitzung) {
    return null;
  }
  if (sitzung.ablauf < Date.now()) {
    sitzungen.delete(token);
    return null;
  }
  sitzung.ablauf = Date.now() + SITZUNG_DAUER;
  return token;
}

function abmelden(token) {
  sitzungen.delete(token);
}

function andereAbmelden(behalten) {
  let anzahl = 0;
  for (const token of [...sitzungen.keys()]) {
    if (token !== behalten) {
      sitzungen.delete(token);
      anzahl += 1;
    }
  }
  return anzahl;
}

async function passwortAendern(anfrage, token, altesPasswort, neuesPasswort) {
  if (typeof neuesPasswort !== 'string' || neuesPasswort.length < 8) {
    throw new Fehler(400, 'Das neue Passwort muss mindestens 8 Zeichen lang sein.');
  }
  if (neuesPasswort.length > 200) {
    throw new Fehler(400, 'Das neue Passwort ist zu lang.');
  }
  const absender = absenderVon(anfrage);
  sperreBeachten(absender);
  if (!(await passwortStimmt(altesPasswort))) {
    fehlversuchMerken(absender);
    throw new Fehler(403, 'Das aktuelle Passwort stimmt nicht.');
  }
  await passwortSetzen(neuesPasswort);
  andereAbmelden(token);
}

function aufraeumen() {
  const jetzt = Date.now();
  for (const [token, sitzung] of sitzungen) {
    if (sitzung.ablauf < jetzt) {
      sitzungen.delete(token);
    }
  }
  for (const [absender, eintrag] of fehlversuche) {
    if (eintrag.gesperrtBis <= jetzt && eintrag.seit + SPERRZEIT < jetzt) {
      fehlversuche.delete(absender);
    }
  }
}

setInterval(aufraeumen, 10 * 60 * 1000).unref();

module.exports = {
  zugangEinrichten,
  anmelden,
  abmeldeCookie,
  sitzungVon,
  abmelden,
  andereAbmelden,
  passwortAendern,
};
