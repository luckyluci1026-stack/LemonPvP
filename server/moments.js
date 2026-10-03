const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const vm = require('node:vm');
const crypto = require('node:crypto');
const { pipeline } = require('node:stream/promises');
const { MOMENTS_SKRIPT, MOMENTS_MEDIEN, MOMENTS_DATEN } = require('./pfade');
const { jsonLesen, jsonSchreiben, wennGeaendertSchreiben, umbenennenWennDa, nacheinander } = require('./speicher');
const { Fehler } = require('./antwort');

const ANZAHL = 10;
const MAX_UPLOAD_MB = Math.max(1, Number(process.env.MAX_UPLOAD_MB) || 300);
const MAX_UPLOAD = MAX_UPLOAD_MB * 1024 * 1024;
const LAENGEN = { title: 80, player: 40, description: 300 };
const ENDUNGEN = { video: 'mp4', bild: 'jpg' };
const BILD_MARKEN = ['heic', 'heix', 'heim', 'heis', 'hevc', 'hevx', 'mif1', 'msf1', 'avif', 'avis'];

function leererMoment() {
  return { title: '', player: '', description: '' };
}

function einzeilig(wert) {
  return typeof wert === 'string' ? wert.replace(/\s+/g, ' ').trim() : '';
}

function platzPruefen(wert) {
  const platz = Number(wert);
  if (!Number.isInteger(platz) || platz < 1 || platz > ANZAHL) {
    throw new Fehler(400, 'Diesen Platz gibt es nicht.');
  }
  return platz;
}

async function statOderNull(datei) {
  try {
    return await fsp.stat(datei);
  } catch {
    return null;
  }
}

function listeBereinigen(roh) {
  return Array.from({ length: ANZAHL }, (_, index) => {
    const eintrag = Array.isArray(roh) ? roh[index] : null;
    if (!eintrag || typeof eintrag !== 'object') {
      return leererMoment();
    }
    return {
      title: einzeilig(eintrag.title).slice(0, LAENGEN.title),
      player: einzeilig(eintrag.player).slice(0, LAENGEN.player),
      description: einzeilig(eintrag.description).slice(0, LAENGEN.description),
    };
  });
}

async function ausSkriptLesen() {
  let code;
  try {
    code = await fsp.readFile(MOMENTS_SKRIPT, 'utf8');
  } catch (fehler) {
    if (fehler.code === 'ENOENT') {
      return { liste: listeBereinigen([]), lesefehler: false };
    }
    throw fehler;
  }
  try {
    const ergebnis = vm.runInNewContext(`${code}\n;MOMENTS;`, {}, { timeout: 200 });
    if (Array.isArray(ergebnis)) {
      return { liste: listeBereinigen(ergebnis), lesefehler: false };
    }
  } catch {
    return { liste: listeBereinigen([]), lesefehler: true };
  }
  return { liste: listeBereinigen([]), lesefehler: true };
}

async function texteLesen() {
  let gespeichert;
  try {
    gespeichert = await jsonLesen(MOMENTS_DATEN, null);
  } catch {
    return { liste: listeBereinigen([]), lesefehler: true };
  }
  if (gespeichert === null) {
    return ausSkriptLesen();
  }
  return { liste: listeBereinigen(gespeichert), lesefehler: false };
}

function jsText(wert) {
  const geschuetzt = wert
    .replace(/\\/g, '\\\\')
    .replace(/'/g, "\\'")
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n')
    .replace(/\u2028/g, '\\u2028')
    .replace(/\u2029/g, '\\u2029');
  return `'${geschuetzt}'`;
}

function skriptAus(liste) {
  const zeilen = liste.map(moment => `  { title: ${jsText(moment.title)}, player: ${jsText(moment.player)}, description: ${jsText(moment.description)} },`);
  return `const MOMENTS = [\n${zeilen.join('\n')}\n];\n`;
}

async function texteSchreiben(liste) {
  await jsonSchreiben(MOMENTS_DATEN, liste);
  await wennGeaendertSchreiben(MOMENTS_SKRIPT, skriptAus(liste));
}

async function medienVon(platz) {
  for (const [art, endung] of Object.entries(ENDUNGEN)) {
    const datei = `${platz}.${endung}`;
    const info = await statOderNull(path.join(MOMENTS_MEDIEN, datei));
    if (info && info.isFile()) {
      return { art, datei, groesse: info.size, geaendert: Math.floor(info.mtimeMs) };
    }
  }
  return null;
}

async function uebersicht() {
  const { liste, lesefehler } = await texteLesen();
  const moments = [];
  for (let platz = 1; platz <= ANZAHL; platz += 1) {
    moments.push({ platz, ...liste[platz - 1], medien: await medienVon(platz) });
  }
  return { moments, lesefehler, maxUploadMb: MAX_UPLOAD_MB };
}

function textSpeichern(platzWert, eingabe) {
  const platz = platzPruefen(platzWert);
  const moment = {
    title: einzeilig(eingabe.title),
    player: einzeilig(eingabe.player),
    description: einzeilig(eingabe.description),
  };
  if (moment.title.length > LAENGEN.title) {
    throw new Fehler(400, `Der Titel ist zu lang – höchstens ${LAENGEN.title} Zeichen.`);
  }
  if (moment.player.length > LAENGEN.player) {
    throw new Fehler(400, `Der Spielername ist zu lang – höchstens ${LAENGEN.player} Zeichen.`);
  }
  if (moment.description.length > LAENGEN.description) {
    throw new Fehler(400, `Die Beschreibung ist zu lang – höchstens ${LAENGEN.description} Zeichen.`);
  }
  return nacheinander(async () => {
    const { liste } = await texteLesen();
    liste[platz - 1] = moment;
    await texteSchreiben(liste);
    return { platz, ...moment };
  });
}

async function anfangLesen(datei) {
  const handle = await fsp.open(datei, 'r');
  try {
    const puffer = Buffer.alloc(16);
    const { bytesRead } = await handle.read(puffer, 0, 16, 0);
    return puffer.subarray(0, bytesRead);
  } finally {
    await handle.close();
  }
}

function artErkennen(anfang) {
  if (anfang.length >= 3 && anfang[0] === 0xff && anfang[1] === 0xd8 && anfang[2] === 0xff) {
    return 'bild';
  }
  if (anfang.length >= 12) {
    const box = anfang.toString('latin1', 4, 8);
    const marke = anfang.toString('latin1', 8, 12);
    if (box === 'ftyp' && BILD_MARKEN.includes(marke)) {
      return null;
    }
    if (['ftyp', 'moov', 'mdat', 'wide', 'free', 'skip'].includes(box)) {
      return 'video';
    }
  }
  return null;
}

async function hochladen(anfrage, platzWert) {
  const platz = platzPruefen(platzWert);
  const laenge = Number(anfrage.headers['content-length']);
  if (!Number.isFinite(laenge) || laenge <= 0) {
    throw new Fehler(411, 'Die Datei ist leer.');
  }
  if (laenge > MAX_UPLOAD) {
    throw new Fehler(413, `Die Datei ist zu groß – höchstens ${MAX_UPLOAD_MB} MB.`);
  }

  await fsp.mkdir(MOMENTS_MEDIEN, { recursive: true });
  const zwischendatei = path.join(MOMENTS_MEDIEN, `.upload-${crypto.randomBytes(8).toString('hex')}.tmp`);
  try {
    await pipeline(anfrage, fs.createWriteStream(zwischendatei));
    const info = await fsp.stat(zwischendatei);
    if (info.size !== laenge) {
      throw new Fehler(400, 'Der Upload wurde nicht vollständig übertragen.');
    }
    const art = artErkennen(await anfangLesen(zwischendatei));
    if (!art) {
      throw new Fehler(415, 'Das ist weder ein MP4-Video noch ein JPG-Bild.');
    }
    return await nacheinander(async () => {
      await fsp.rename(zwischendatei, path.join(MOMENTS_MEDIEN, `${platz}.${ENDUNGEN[art]}`));
      for (const [andereArt, endung] of Object.entries(ENDUNGEN)) {
        if (andereArt !== art) {
          await fsp.rm(path.join(MOMENTS_MEDIEN, `${platz}.${endung}`), { force: true });
        }
      }
      return medienVon(platz);
    });
  } finally {
    await fsp.rm(zwischendatei, { force: true });
  }
}

function entfernen(platzWert) {
  const platz = platzPruefen(platzWert);
  return nacheinander(async () => {
    let entfernt = false;
    for (const endung of Object.values(ENDUNGEN)) {
      const datei = path.join(MOMENTS_MEDIEN, `${platz}.${endung}`);
      if (await statOderNull(datei)) {
        await fsp.rm(datei, { force: true });
        entfernt = true;
      }
    }
    if (!entfernt) {
      throw new Fehler(404, 'Für diesen Platz gibt es keinen Clip.');
    }
    return platz;
  });
}

function tauschen(aWert, bWert) {
  const a = platzPruefen(aWert);
  const b = platzPruefen(bWert);
  if (a === b) {
    throw new Fehler(400, 'Zum Tauschen braucht es zwei verschiedene Plätze.');
  }
  return nacheinander(async () => {
    const { liste } = await texteLesen();
    [liste[a - 1], liste[b - 1]] = [liste[b - 1], liste[a - 1]];

    const marke = crypto.randomBytes(6).toString('hex');
    for (const endung of Object.values(ENDUNGEN)) {
      const dateiA = path.join(MOMENTS_MEDIEN, `${a}.${endung}`);
      const dateiB = path.join(MOMENTS_MEDIEN, `${b}.${endung}`);
      const zwischen = path.join(MOMENTS_MEDIEN, `.tausch-${marke}.${endung}`);
      await umbenennenWennDa(dateiA, zwischen);
      await umbenennenWennDa(dateiB, dateiA);
      await umbenennenWennDa(zwischen, dateiB);
    }

    await texteSchreiben(liste);
  });
}

function startAbgleich() {
  return nacheinander(async () => {
    try {
      for (const name of await fsp.readdir(MOMENTS_MEDIEN)) {
        if (name.startsWith('.upload-') || name.startsWith('.tausch-')) {
          await fsp.rm(path.join(MOMENTS_MEDIEN, name), { force: true });
        }
      }
    } catch {
      await fsp.mkdir(MOMENTS_MEDIEN, { recursive: true });
    }
    const { liste, lesefehler } = await texteLesen();
    if (lesefehler) {
      return false;
    }
    await texteSchreiben(liste);
    return true;
  });
}

module.exports = {
  ANZAHL,
  startAbgleich,
  uebersicht,
  textSpeichern,
  hochladen,
  entfernen,
  tauschen,
};
