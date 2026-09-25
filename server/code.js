const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const { WURZEL, SICHERUNGEN, NIE_AUSLIEFERN, istSichererTeil } = require('./pfade');
const { dateiSchreiben, nacheinander } = require('./speicher');
const { Fehler } = require('./antwort');

const BEARBEITBAR = new Set(['.html', '.css', '.js']);
const NICHT_IM_EDITOR = new Set([...NIE_AUSLIEFERN, 'admin', 'wiki']);
const NICHT_IM_EDITOR_ANFANG = ['assets/vendor/', 'assets/fonts/'];
const NICHT_IM_EDITOR_DATEIEN = new Set(['top/10/aura-moments/moments.js']);
const MAX_GROESSE = 2 * 1024 * 1024;
const MAX_SICHERUNGEN = 20;
const MAX_DATEIEN = 800;
const SICHERUNG_MUSTER = /^\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}-\d{3}Z\.bak$/;

function istBearbeitbar(relativerPfad) {
  const teile = relativerPfad.split('/');
  if (!teile.every(istSichererTeil)) {
    return false;
  }
  if (NICHT_IM_EDITOR.has(teile[0])) {
    return false;
  }
  if (NICHT_IM_EDITOR_ANFANG.some(anfang => relativerPfad.startsWith(anfang)) || NICHT_IM_EDITOR_DATEIEN.has(relativerPfad)) {
    return false;
  }
  return BEARBEITBAR.has(path.extname(relativerPfad).toLowerCase());
}

async function dateiPfad(relativerPfad) {
  if (typeof relativerPfad !== 'string' || relativerPfad.length > 300 || !istBearbeitbar(relativerPfad)) {
    throw new Fehler(400, 'Diese Datei kann im Code-Editor nicht bearbeitet werden.');
  }
  const datei = path.join(WURZEL, ...relativerPfad.split('/'));
  let echterPfad;
  try {
    echterPfad = await fsp.realpath(datei);
  } catch {
    throw new Fehler(404, 'Diese Datei gibt es nicht.');
  }
  if (!echterPfad.startsWith(fs.realpathSync(WURZEL) + path.sep)) {
    throw new Fehler(400, 'Diese Datei kann im Code-Editor nicht bearbeitet werden.');
  }
  return datei;
}

async function dateienAuflisten() {
  const gefunden = [];

  async function durchsuchen(ordner, relativ, tiefe) {
    if (tiefe > 8 || gefunden.length >= MAX_DATEIEN) {
      return;
    }
    let eintraege;
    try {
      eintraege = await fsp.readdir(ordner, { withFileTypes: true });
    } catch {
      return;
    }
    eintraege.sort((a, b) => a.name.localeCompare(b.name, 'de'));
    for (const eintrag of eintraege) {
      const pfad = relativ ? `${relativ}/${eintrag.name}` : eintrag.name;
      if (!istSichererTeil(eintrag.name)) {
        continue;
      }
      if (eintrag.isDirectory()) {
        if (tiefe === 0 && NICHT_IM_EDITOR.has(eintrag.name)) {
          continue;
        }
        if (NICHT_IM_EDITOR_ANFANG.some(anfang => `${pfad}/`.startsWith(anfang))) {
          continue;
        }
        await durchsuchen(path.join(ordner, eintrag.name), pfad, tiefe + 1);
      } else if (eintrag.isFile() && istBearbeitbar(pfad)) {
        const info = await fsp.stat(path.join(ordner, eintrag.name));
        gefunden.push({ pfad, groesse: info.size, geaendert: Math.floor(info.mtimeMs) });
      }
    }
  }

  await durchsuchen(WURZEL, '', 0);
  return gefunden;
}

function sicherungsOrdner(relativerPfad) {
  return path.join(SICHERUNGEN, relativerPfad.split('/').join('__'));
}

async function sicherungenAuflisten(relativerPfad) {
  let namen;
  try {
    namen = await fsp.readdir(sicherungsOrdner(relativerPfad));
  } catch {
    return [];
  }
  const liste = [];
  for (const name of namen.filter(eintrag => SICHERUNG_MUSTER.test(eintrag)).sort().reverse()) {
    const info = await fsp.stat(path.join(sicherungsOrdner(relativerPfad), name));
    const zeit = name.replace(/\.bak$/, '').replace(/T(\d{2})-(\d{2})-(\d{2})-(\d{3})Z$/, 'T$1:$2:$3.$4Z');
    liste.push({ id: name, zeit, groesse: info.size });
  }
  return liste;
}

async function sicherungAnlegen(relativerPfad, inhalt) {
  const ordner = sicherungsOrdner(relativerPfad);
  await fsp.mkdir(ordner, { recursive: true });
  const name = `${new Date().toISOString().replace(/[:.]/g, '-')}.bak`;
  await fsp.writeFile(path.join(ordner, name), inhalt);
  const alle = (await fsp.readdir(ordner)).filter(eintrag => SICHERUNG_MUSTER.test(eintrag)).sort();
  for (const alt of alle.slice(0, Math.max(0, alle.length - MAX_SICHERUNGEN))) {
    await fsp.rm(path.join(ordner, alt), { force: true });
  }
}

async function dateiLesen(relativerPfad) {
  const datei = await dateiPfad(relativerPfad);
  const info = await fsp.stat(datei);
  if (info.size > MAX_GROESSE) {
    throw new Fehler(413, 'Diese Datei ist zu groß für den Editor.');
  }
  return {
    pfad: relativerPfad,
    inhalt: await fsp.readFile(datei, 'utf8'),
    geaendert: Math.floor(info.mtimeMs),
    sicherungen: await sicherungenAuflisten(relativerPfad),
  };
}

function dateiSpeichern(relativerPfad, inhalt, geaendert, erzwingen) {
  if (typeof inhalt !== 'string') {
    throw new Fehler(400, 'Der Inhalt fehlt.');
  }
  if (Buffer.byteLength(inhalt) > MAX_GROESSE) {
    throw new Fehler(413, 'Der Inhalt ist zu groß.');
  }
  return nacheinander(async () => {
    const datei = await dateiPfad(relativerPfad);
    const info = await fsp.stat(datei);
    if (!erzwingen && typeof geaendert === 'number' && Math.floor(info.mtimeMs) !== geaendert) {
      throw new Fehler(409, 'Die Datei wurde inzwischen an anderer Stelle geändert.');
    }
    const vorher = await fsp.readFile(datei, 'utf8');
    if (vorher !== inhalt) {
      await sicherungAnlegen(relativerPfad, vorher);
      await dateiSchreiben(datei, inhalt);
    }
    const neu = await fsp.stat(datei);
    return {
      pfad: relativerPfad,
      geaendert: Math.floor(neu.mtimeMs),
      unveraendert: vorher === inhalt,
      sicherungen: await sicherungenAuflisten(relativerPfad),
    };
  });
}

function sicherungWiederherstellen(relativerPfad, sicherung) {
  if (typeof sicherung !== 'string' || !SICHERUNG_MUSTER.test(sicherung)) {
    throw new Fehler(400, 'Diese Sicherung gibt es nicht.');
  }
  return nacheinander(async () => {
    const datei = await dateiPfad(relativerPfad);
    let alterStand;
    try {
      alterStand = await fsp.readFile(path.join(sicherungsOrdner(relativerPfad), sicherung), 'utf8');
    } catch {
      throw new Fehler(404, 'Diese Sicherung gibt es nicht mehr.');
    }
    const aktuell = await fsp.readFile(datei, 'utf8');
    if (aktuell !== alterStand) {
      await sicherungAnlegen(relativerPfad, aktuell);
      await dateiSchreiben(datei, alterStand);
    }
  });
}

module.exports = {
  dateienAuflisten,
  dateiLesen,
  dateiSpeichern,
  sicherungWiederherstellen,
};
