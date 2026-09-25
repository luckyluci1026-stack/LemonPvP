const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');

async function dateiSchreiben(ziel, inhalt) {
  await fs.mkdir(path.dirname(ziel), { recursive: true });
  const zwischendatei = path.join(path.dirname(ziel), `.${path.basename(ziel)}.${crypto.randomBytes(6).toString('hex')}.tmp`);
  await fs.writeFile(zwischendatei, inhalt);
  try {
    await fs.rename(zwischendatei, ziel);
  } catch (fehler) {
    if (fehler.code !== 'EPERM' && fehler.code !== 'EACCES' && fehler.code !== 'EBUSY') {
      await fs.rm(zwischendatei, { force: true });
      throw fehler;
    }
    await fs.writeFile(ziel, inhalt);
    await fs.rm(zwischendatei, { force: true });
  }
}

async function wennGeaendertSchreiben(ziel, inhalt) {
  try {
    const vorher = await fs.readFile(ziel, 'utf8');
    if (vorher === inhalt) {
      return false;
    }
  } catch (fehler) {
    if (fehler.code !== 'ENOENT') {
      throw fehler;
    }
  }
  await dateiSchreiben(ziel, inhalt);
  return true;
}

async function jsonLesen(datei, ersatz) {
  let inhalt;
  try {
    inhalt = await fs.readFile(datei, 'utf8');
  } catch (fehler) {
    if (fehler.code === 'ENOENT') {
      return ersatz;
    }
    throw fehler;
  }
  return JSON.parse(inhalt);
}

async function jsonSchreiben(datei, daten) {
  await dateiSchreiben(datei, `${JSON.stringify(daten, null, 2)}\n`);
}

async function umbenennenWennDa(von, nach) {
  try {
    await fs.rename(von, nach);
    return true;
  } catch (fehler) {
    if (fehler.code === 'ENOENT') {
      return false;
    }
    throw fehler;
  }
}

let letzteAufgabe = Promise.resolve();

function nacheinander(aufgabe) {
  const ergebnis = letzteAufgabe.then(() => aufgabe());
  letzteAufgabe = ergebnis.catch(() => {});
  return ergebnis;
}

module.exports = {
  dateiSchreiben,
  wennGeaendertSchreiben,
  jsonLesen,
  jsonSchreiben,
  umbenennenWennDa,
  nacheinander,
};
