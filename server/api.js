const { Fehler, jsonSenden, jsonKoerper } = require('./antwort');
const anmeldung = require('./anmeldung');
const einstellungen = require('./einstellungen');
const wiki = require('./wiki');
const moments = require('./moments');
const code = require('./code');
const { lesezeit } = require('./vorlage');

const OHNE_ANMELDUNG = new Set(['GET /admin/api/status', 'POST /admin/api/anmelden']);
const GROSSE_ANFRAGEN = new Set(['POST /admin/api/code/datei']);

function zeichen(wert) {
  return typeof wert === 'string' ? wert : '';
}

function richtungVon(wert) {
  if (wert !== -1 && wert !== 1) {
    throw new Fehler(400, 'Ungültige Richtung.');
  }
  return wert;
}

function merken(art, text) {
  einstellungen.verlaufEintragen(art, text);
}

function wikiAntwort(daten) {
  return {
    gruppen: daten.gruppen,
    artikel: daten.artikel.map(artikel => ({ ...artikel, minuten: lesezeit(artikel.inhalt) })),
    weiterleitungen: daten.weiterleitungen,
    farben: wiki.FARBEN,
  };
}

function nurMitCodeEditor(aktion) {
  return async kontext => {
    const { codeEditor } = await einstellungen.einstellungenLesen();
    if (!codeEditor) {
      throw new Fehler(403, 'Der Code-Editor ist ausgeschaltet. Du kannst ihn unter „Einstellungen“ einschalten.');
    }
    return aktion(kontext);
  };
}

const ROUTEN = {
  'GET /admin/api/status': async ({ token }) => {
    if (!token) {
      return { angemeldet: false };
    }
    const { codeEditor } = await einstellungen.einstellungenLesen();
    return { angemeldet: true, codeEditor };
  },

  'POST /admin/api/anmelden': async ({ anfrage, daten, cookieSetzen }) => {
    cookieSetzen(await anmeldung.anmelden(anfrage, daten.passwort));
    merken('anmeldung', 'Im Admin-Panel angemeldet');
    const { codeEditor } = await einstellungen.einstellungenLesen();
    return { angemeldet: true, codeEditor };
  },

  'POST /admin/api/abmelden': async ({ anfrage, token, cookieSetzen }) => {
    anmeldung.abmelden(token);
    cookieSetzen(anmeldung.abmeldeCookie(anfrage));
    return { angemeldet: false };
  },

  'GET /admin/api/uebersicht': async () => {
    const [wikiDaten, momentDaten, eingestellt, verlauf] = await Promise.all([
      wiki.wikiLesen(),
      moments.uebersicht(),
      einstellungen.einstellungenLesen(),
      einstellungen.verlaufLesen(),
    ]);
    return {
      artikel: wikiDaten.artikel.length,
      gruppen: wikiDaten.gruppen.length,
      weiterleitungen: wikiDaten.weiterleitungen.length,
      clips: momentDaten.moments.filter(moment => moment.medien).length,
      plaetze: momentDaten.moments.length,
      codeEditor: eingestellt.codeEditor,
      verlauf: verlauf.slice(0, 12),
    };
  },

  'GET /admin/api/wiki': async () => wikiAntwort(await wiki.wikiLesen()),

  'POST /admin/api/wiki/artikel': async ({ daten }) => {
    const alterSlug = zeichen(daten.alterSlug) || null;
    const { daten: neu, ergebnis } = await wiki.artikelSpeichern(daten.artikel, alterSlug);
    merken('wiki', `Artikel „${ergebnis.titel}“ ${alterSlug ? 'gespeichert' : 'erstellt'}`);
    return { wiki: wikiAntwort(neu), slug: ergebnis.slug };
  },

  'POST /admin/api/wiki/loeschen': async ({ daten }) => {
    const nach = daten.nach === null ? null : zeichen(daten.nach);
    const { daten: neu, ergebnis } = await wiki.artikelLoeschen(zeichen(daten.slug), nach);
    merken('wiki', `Artikel „${ergebnis.titel}“ gelöscht`);
    return { wiki: wikiAntwort(neu) };
  },

  'POST /admin/api/wiki/verschieben': async ({ daten }) => {
    const richtung = richtungVon(daten.richtung);
    const { daten: neu } = typeof daten.gruppe === 'string'
      ? await wiki.gruppeVerschieben(daten.gruppe, richtung)
      : await wiki.artikelVerschieben(zeichen(daten.slug), richtung);
    return { wiki: wikiAntwort(neu) };
  },

  'POST /admin/api/wiki/gruppe': async ({ daten }) => {
    const { daten: neu } = await wiki.gruppeUmbenennen(zeichen(daten.alt), zeichen(daten.neu));
    merken('wiki', `Kategorie „${zeichen(daten.alt)}“ in „${zeichen(daten.neu).trim()}“ umbenannt`);
    return { wiki: wikiAntwort(neu) };
  },

  'POST /admin/api/wiki/weiterleitung': async ({ daten }) => {
    const { daten: neu, ergebnis } = await wiki.weiterleitungSpeichern(zeichen(daten.von), zeichen(daten.nach));
    merken('wiki', `Weiterleitung /wiki/${ergebnis}/ angelegt`);
    return { wiki: wikiAntwort(neu) };
  },

  'POST /admin/api/wiki/weiterleitung-loeschen': async ({ daten }) => {
    const { daten: neu } = await wiki.weiterleitungLoeschen(zeichen(daten.von));
    merken('wiki', `Weiterleitung /wiki/${zeichen(daten.von)}/ gelöscht`);
    return { wiki: wikiAntwort(neu) };
  },

  'POST /admin/api/wiki/vorschau': async ({ daten }) => ({
    html: await wiki.vorschau(daten.artikel, zeichen(daten.alterSlug) || null),
  }),

  'POST /admin/api/wiki/neu-erzeugen': async () => {
    const geaendert = await wiki.neuErzeugen();
    merken('wiki', 'Alle Wiki-Seiten neu erzeugt');
    return { geaendert };
  },

  'GET /admin/api/moments': async () => moments.uebersicht(),

  'POST /admin/api/moments/text': async ({ daten }) => {
    const moment = await moments.textSpeichern(daten.platz, daten);
    merken('moments', `Texte für Platz ${moment.platz} gespeichert`);
    return moments.uebersicht();
  },

  'PUT /admin/api/moments/datei': async ({ anfrage, url }) => {
    const platz = url.searchParams.get('platz');
    const medien = await moments.hochladen(anfrage, platz);
    merken('moments', `${medien && medien.art === 'bild' ? 'Bild' : 'Video'} für Platz ${platz} hochgeladen`);
    return moments.uebersicht();
  },

  'POST /admin/api/moments/entfernen': async ({ daten }) => {
    const platz = await moments.entfernen(daten.platz);
    merken('moments', `Clip von Platz ${platz} entfernt`);
    return moments.uebersicht();
  },

  'POST /admin/api/moments/tauschen': async ({ daten }) => {
    await moments.tauschen(daten.a, daten.b);
    merken('moments', `Platz ${daten.a} und Platz ${daten.b} getauscht`);
    return moments.uebersicht();
  },

  'GET /admin/api/einstellungen': async () => einstellungen.einstellungenLesen(),

  'POST /admin/api/einstellungen': async ({ daten }) => {
    if (typeof daten.codeEditor !== 'boolean') {
      throw new Fehler(400, 'Ungültige Einstellung.');
    }
    const neu = await einstellungen.einstellungenAendern({ codeEditor: daten.codeEditor });
    merken('einstellungen', `Code-Editor ${neu.codeEditor ? 'eingeschaltet' : 'ausgeschaltet'}`);
    return neu;
  },

  'POST /admin/api/passwort': async ({ anfrage, token, daten }) => {
    await anmeldung.passwortAendern(anfrage, token, daten.alt, daten.neu);
    merken('einstellungen', 'Admin-Passwort geändert');
    return { ok: true };
  },

  'POST /admin/api/ueberall-abmelden': async ({ token }) => {
    const abgemeldet = anmeldung.andereAbmelden(token);
    merken('einstellungen', 'Alle anderen Geräte abgemeldet');
    return { abgemeldet };
  },

  'GET /admin/api/code/dateien': nurMitCodeEditor(async () => ({
    dateien: await code.dateienAuflisten(),
  })),

  'GET /admin/api/code/datei': nurMitCodeEditor(async ({ url }) => code.dateiLesen(url.searchParams.get('pfad'))),

  'POST /admin/api/code/datei': nurMitCodeEditor(async ({ daten }) => {
    const ergebnis = await code.dateiSpeichern(zeichen(daten.pfad), daten.inhalt, daten.geaendert, daten.erzwingen === true);
    if (!ergebnis.unveraendert) {
      merken('code', `Datei ${ergebnis.pfad} gespeichert`);
    }
    return ergebnis;
  }),

  'POST /admin/api/code/wiederherstellen': nurMitCodeEditor(async ({ daten }) => {
    await code.sicherungWiederherstellen(zeichen(daten.pfad), daten.sicherung);
    merken('code', `Datei ${zeichen(daten.pfad)} aus einer Sicherung wiederhergestellt`);
    return code.dateiLesen(zeichen(daten.pfad));
  }),
};

async function apiBearbeiten(anfrage, antwort, url) {
  const schluessel = `${anfrage.method} ${url.pathname}`;
  const kopfzeilen = {};
  try {
    if (anfrage.headers['x-bucksmp-admin'] !== '1') {
      throw new Fehler(403, 'Anfrage abgelehnt.');
    }
    const route = ROUTEN[schluessel];
    if (!route) {
      throw new Fehler(404, 'Diese Aktion gibt es nicht.');
    }
    const token = anmeldung.sitzungVon(anfrage);
    if (!token && !OHNE_ANMELDUNG.has(schluessel)) {
      throw new Fehler(401, 'Bitte melde dich an.');
    }
    const daten = anfrage.method === 'POST'
      ? await jsonKoerper(anfrage, GROSSE_ANFRAGEN.has(schluessel) ? 3 * 1024 * 1024 : 1024 * 1024)
      : {};
    const ergebnis = await route({
      anfrage,
      url,
      token,
      daten,
      cookieSetzen: cookie => {
        kopfzeilen['Set-Cookie'] = cookie;
      },
    });
    jsonSenden(antwort, 200, ergebnis === undefined ? { ok: true } : ergebnis, kopfzeilen);
  } catch (fehler) {
    if (antwort.headersSent || !antwort.socket || antwort.socket.destroyed) {
      return;
    }
    const extra = anfrage.complete ? {} : { Connection: 'close' };
    if (fehler instanceof Fehler) {
      jsonSenden(antwort, fehler.status, { fehler: fehler.message }, extra);
    } else {
      console.error(fehler);
      jsonSenden(antwort, 500, { fehler: 'Auf dem Server ist ein Fehler passiert.' }, extra);
    }
  }
}

module.exports = {
  apiBearbeiten,
};
