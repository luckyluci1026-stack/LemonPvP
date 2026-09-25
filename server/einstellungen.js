const path = require('node:path');
const { DATEN } = require('./pfade');
const { jsonLesen, jsonSchreiben, nacheinander } = require('./speicher');

const EINSTELLUNGEN_DATEI = path.join(DATEN, 'einstellungen.json');
const VERLAUF_DATEI = path.join(DATEN, 'verlauf.json');
const MAX_VERLAUF = 60;

async function sicherLesen(datei, ersatz) {
  try {
    return await jsonLesen(datei, ersatz);
  } catch {
    return ersatz;
  }
}

async function einstellungenLesen() {
  const gespeichert = await sicherLesen(EINSTELLUNGEN_DATEI, {});
  return {
    codeEditor: gespeichert.codeEditor === true,
  };
}

function einstellungenAendern(aenderungen) {
  return nacheinander(async () => {
    const einstellungen = await einstellungenLesen();
    if (typeof aenderungen.codeEditor === 'boolean') {
      einstellungen.codeEditor = aenderungen.codeEditor;
    }
    await jsonSchreiben(EINSTELLUNGEN_DATEI, einstellungen);
    return einstellungen;
  });
}

async function verlaufLesen() {
  const verlauf = await sicherLesen(VERLAUF_DATEI, []);
  return Array.isArray(verlauf) ? verlauf : [];
}

function verlaufEintragen(art, textZeile) {
  return nacheinander(async () => {
    const verlauf = await verlaufLesen();
    verlauf.unshift({ zeit: new Date().toISOString(), art, text: textZeile });
    await jsonSchreiben(VERLAUF_DATEI, verlauf.slice(0, MAX_VERLAUF));
  }).catch(fehler => {
    console.error('Verlauf konnte nicht gespeichert werden:', fehler.message);
  });
}

module.exports = {
  einstellungenLesen,
  einstellungenAendern,
  verlaufLesen,
  verlaufEintragen,
};
