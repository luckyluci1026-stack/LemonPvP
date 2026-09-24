const API = 'api/';

const BEREICHE = ['uebersicht', 'wiki', 'moments', 'code', 'einstellungen'];

const BEREICH_TITEL = {
  uebersicht: 'Übersicht',
  wiki: 'Wiki',
  moments: 'Aura Moments',
  code: 'Code',
  einstellungen: 'Einstellungen',
};

const FARB_NAMEN = {
  emerald: 'Smaragd',
  diamond: 'Diamant',
  redstone: 'Redstone',
  lapis: 'Lapis',
  gold: 'Gold',
  amethyst: 'Amethyst',
};

const SYMBOLE = [
  'fa-book', 'fa-book-open', 'fa-terminal', 'fa-hand-fist', 'fa-shield-halved', 'fa-coins',
  'fa-flag-checkered', 'fa-mobile-screen-button', 'fa-person-running', 'fa-crosshairs', 'fa-cube',
  'fa-scale-balanced', 'fa-house', 'fa-map', 'fa-compass', 'fa-gem', 'fa-hammer', 'fa-trophy',
  'fa-users', 'fa-circle-question', 'fa-lightbulb', 'fa-gear', 'fa-wheat-awn', 'fa-dragon',
  'fa-skull', 'fa-heart', 'fa-star', 'fa-fire', 'fa-bolt', 'fa-gamepad',
];

const SEITEN_LINKS = [
  ['Startseite', '../../'],
  ['Features', '../../features/'],
  ['Beitreten', '../../join/'],
  ['Regeln', '../../regeln/'],
  ['Team', '../../team/'],
  ['Aura Moments', '../../top/10/aura-moments/'],
  ['Hilfe-Center', '../../help/'],
  ['Wiki-Übersicht', '../'],
];

const WERKZEUGE = [
  { name: 'ueberschrift', symbol: 'fa-heading', text: 'Überschrift', vorne: '<h2>', hinten: '</h2>', platzhalter: 'Überschrift', block: true },
  { name: 'absatz', symbol: 'fa-paragraph', text: 'Absatz', vorne: '<p>', hinten: '</p>', platzhalter: 'Dein Text.', block: true },
  { name: 'einleitung', symbol: 'fa-align-left', text: 'Einleitung', vorne: '<p class="lead">', hinten: '</p>', platzhalter: 'Worum geht es in diesem Artikel?', block: true },
  { name: 'fett', symbol: 'fa-bold', text: 'Fett', vorne: '<strong>', hinten: '</strong>', platzhalter: 'wichtig' },
  { name: 'link', symbol: 'fa-link', text: 'Link' },
  { name: 'befehl', symbol: 'fa-terminal', text: 'Befehl', vorne: '<code class="cmd">', hinten: '</code>', platzhalter: '/befehl' },
  { name: 'liste', symbol: 'fa-list-ul', text: 'Liste', fertig: '<ul>\n  <li>Erster Punkt</li>\n  <li>Zweiter Punkt</li>\n</ul>', markieren: 'Erster Punkt', block: true },
  { name: 'schritte', symbol: 'fa-list-ol', text: 'Schritte', fertig: '<ol class="schritte">\n  <li>Erster Schritt</li>\n  <li>Zweiter Schritt</li>\n</ol>', markieren: 'Erster Schritt', block: true },
  { name: 'begriffe', symbol: 'fa-list-check', text: 'Begriffe', fertig: '<ul class="eintraege">\n  <li><strong>Begriff</strong><span>Erklärung</span></li>\n  <li><strong>Begriff</strong><span>Erklärung</span></li>\n</ul>', markieren: 'Begriff', block: true },
  { name: 'hinweis', symbol: 'fa-circle-info', text: 'Hinweis', vorne: '<div class="hinweis">\n  <i class="fa-solid fa-lightbulb"></i>\n  <p>', hinten: '</p>\n</div>', platzhalter: 'Ein hilfreicher Hinweis.', block: true },
  { name: 'tabelle', symbol: 'fa-table', text: 'Tabelle', fertig: '<div class="tabelle-wrap">\n  <table class="tabelle">\n    <thead><tr><th>Befehl</th><th>Was er macht</th></tr></thead>\n    <tbody>\n      <tr><td><code class="cmd">/befehl</code></td><td>Beschreibung</td></tr>\n      <tr><td><code class="cmd">/befehl</code></td><td>Beschreibung</td></tr>\n    </tbody>\n  </table>\n</div>', markieren: '/befehl', block: true },
];

const NEUER_INHALT = '<p class="lead">Worum geht es in diesem Artikel? Ein bis zwei Sätze.</p>\n\n<h2>Erste Überschrift</h2>\n<p>Hier kommt dein Text hin.</p>';

const VERLAUF_ARTEN = {
  wiki: ['fa-book-open', 'diamond'],
  moments: ['fa-film', 'gold'],
  code: ['fa-code', 'amethyst'],
  einstellungen: ['fa-gear', 'lapis'],
  anmeldung: ['fa-right-to-bracket', 'emerald'],
};

const DATEI_SYMBOLE = {
  html: 'fa-file-lines',
  css: 'fa-palette',
  js: 'fa-file-code',
};

const zustand = {
  angemeldet: false,
  codeEditor: false,
  wiki: null,
  editor: null,
  vorschauTimer: null,
  vorschauNummer: 0,
  moments: [],
  maxUploadMb: 300,
  entwuerfe: new Map(),
  uploads: new Set(),
  codeDateien: [],
  codeDatei: null,
  zeilenAnzahl: 0,
  tabVerlassen: false,
  verlassenErlaubt: false,
};

function holen(id) {
  return document.getElementById(id);
}

function el(tag, eigenschaften = {}, ...kinder) {
  const element = document.createElement(tag);
  for (const [name, wert] of Object.entries(eigenschaften)) {
    if (wert === undefined || wert === null || wert === false) {
      continue;
    }
    if (name === 'klasse') {
      element.className = wert;
    } else if (name === 'text') {
      element.textContent = wert;
    } else if (name === 'wert') {
      element.value = wert;
    } else if (name === 'bei') {
      for (const [ereignis, funktion] of Object.entries(wert)) {
        element.addEventListener(ereignis, funktion);
      }
    } else if (name === 'daten') {
      Object.assign(element.dataset, wert);
    } else {
      element.setAttribute(name, wert === true ? '' : String(wert));
    }
  }
  for (const kind of kinder.flat()) {
    if (kind !== null && kind !== undefined && kind !== false) {
      element.append(kind);
    }
  }
  return element;
}

function icon(name) {
  return el('i', { klasse: `fa-solid ${name}`, 'aria-hidden': 'true' });
}

function knopf({ text, symbol, klick, klasse = '', titel, aus = false }) {
  const element = el('button', {
    type: 'button',
    klasse: `knopf ${klasse}`.trim(),
    title: titel,
    'aria-label': text ? undefined : titel,
    disabled: aus,
    bei: { click: klick },
  }, symbol ? icon(symbol) : null, text ? el('span', { text }) : null);
  return element;
}

function feldMitName(name, eingabe, hilfe) {
  return el('label', { klasse: 'feld' },
    el('span', { klasse: 'feld-name', text: name }),
    eingabe,
    hilfe ? el('span', { klasse: 'feld-hilfe', text: hilfe }) : null);
}

function slugAusText(wert) {
  return String(wert)
    .toLowerCase()
    .replace(/ä/g, 'ae')
    .replace(/ö/g, 'oe')
    .replace(/ü/g, 'ue')
    .replace(/ß/g, 'ss')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60)
    .replace(/-+$/, '');
}

function groesseText(bytes) {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }
  return `${(bytes / (1024 * 1024)).toLocaleString('de-DE', { maximumFractionDigits: 1 })} MB`;
}

function datumText(wert) {
  return new Date(wert).toLocaleString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function vorWieLange(wert) {
  const sekunden = Math.round((Date.now() - new Date(wert).getTime()) / 1000);
  if (sekunden < 60) {
    return 'gerade eben';
  }
  const minuten = Math.round(sekunden / 60);
  if (minuten < 60) {
    return `vor ${minuten} ${minuten === 1 ? 'Minute' : 'Minuten'}`;
  }
  const stunden = Math.round(minuten / 60);
  if (stunden < 24) {
    return `vor ${stunden} ${stunden === 1 ? 'Stunde' : 'Stunden'}`;
  }
  return datumText(wert);
}

async function api(pfad, daten, methode) {
  const optionen = {
    method: methode || (daten === undefined ? 'GET' : 'POST'),
    headers: { 'X-BuckSMP-Admin': '1' },
    credentials: 'same-origin',
  };
  if (daten !== undefined) {
    optionen.headers['Content-Type'] = 'application/json';
    optionen.body = JSON.stringify(daten);
  }

  let antwort;
  try {
    antwort = await fetch(API + pfad, optionen);
  } catch {
    throw new Error('Keine Verbindung zum Server.');
  }

  let ergebnis = null;
  try {
    ergebnis = await antwort.json();
  } catch {
    ergebnis = null;
  }

  if (antwort.status === 401 && pfad !== 'anmelden' && pfad !== 'status') {
    abgemeldet();
    throw new Error('Deine Anmeldung ist abgelaufen. Bitte melde dich neu an.');
  }
  if (!antwort.ok || ergebnis === null) {
    const fehler = new Error((ergebnis && ergebnis.fehler) || `Unerwartete Antwort vom Server (${antwort.status}).`);
    fehler.status = antwort.status;
    throw fehler;
  }
  return ergebnis;
}

function melden(text, art = 'erfolg') {
  const symbole = { erfolg: 'fa-circle-check', fehler: 'fa-triangle-exclamation', info: 'fa-circle-info' };
  const meldung = el('div', { klasse: `meldung ${art}`, role: art === 'fehler' ? 'alert' : 'status' },
    icon(symbole[art]),
    el('span', { text }));
  holen('meldungen').append(meldung);
  setTimeout(() => {
    meldung.classList.add('weg');
    setTimeout(() => meldung.remove(), 320);
  }, art === 'fehler' ? 6500 : 3800);
}

function dialogZeigen({ titel, text, inhalt, ok = 'OK', gefahr = false, abbrechen = 'Abbrechen' }) {
  const dialog = holen('dialog');
  holen('dialogTitel').textContent = titel;
  const bereich = holen('dialogInhalt');
  bereich.replaceChildren();
  if (text) {
    bereich.append(el('p', { text }));
  }
  if (inhalt) {
    bereich.append(inhalt);
  }
  const okKnopf = holen('dialogOk');
  okKnopf.textContent = ok;
  okKnopf.className = gefahr ? 'knopf gefahr' : 'knopf haupt';
  const abbrechenKnopf = holen('dialogAbbrechen');
  abbrechenKnopf.textContent = abbrechen || '';
  abbrechenKnopf.hidden = !abbrechen;

  dialog.returnValue = '';
  dialog.showModal();
  return new Promise(fertig => {
    dialog.addEventListener('close', () => fertig(dialog.returnValue === 'ok'), { once: true });
  });
}

function ansichtWechseln(name) {
  for (const id of ['startbild', 'zugang', 'ohneServer', 'app']) {
    holen(id).hidden = id !== name;
  }
  if (name === 'zugang') {
    holen('anmeldePasswort').focus();
  }
}

async function starten() {
  ansichtWechseln('startbild');
  let status;
  try {
    status = await api('status');
  } catch {
    ansichtWechseln('ohneServer');
    return;
  }
  if (status.angemeldet) {
    angemeldet(status);
  } else {
    ansichtWechseln('zugang');
  }
}

function angemeldet(status) {
  zustand.angemeldet = true;
  codeEditorAnzeigen(status.codeEditor === true);
  ansichtWechseln('app');
  bereichZeigen();
}

function abgemeldet() {
  zustand.angemeldet = false;
  holen('anmeldePasswort').value = '';
  holen('anmeldeFehler').hidden = true;
  ansichtWechseln('zugang');
}

function codeEditorAnzeigen(an) {
  zustand.codeEditor = an;
  holen('menueCode').hidden = !an;
  holen('schalterCode').checked = an;
}

function bereichZeigen() {
  let name = location.hash.slice(1);
  if (!BEREICHE.includes(name) || (name === 'code' && !zustand.codeEditor)) {
    name = 'uebersicht';
  }
  for (const bereich of BEREICHE) {
    holen(`bereich-${bereich}`).hidden = bereich !== name;
  }
  for (const link of document.querySelectorAll('.menue a')) {
    const aktiv = link.dataset.bereich === name;
    link.classList.toggle('aktiv', aktiv);
    if (aktiv) {
      link.setAttribute('aria-current', 'page');
    } else {
      link.removeAttribute('aria-current');
    }
  }
  document.title = `${BEREICH_TITEL[name]} – Admin – BuckSMP`;

  if (name === 'uebersicht') {
    uebersichtLaden();
  } else if (name === 'wiki') {
    wikiLaden();
  } else if (name === 'moments') {
    momentsLaden();
  } else if (name === 'code') {
    codeLaden();
  } else if (name === 'einstellungen') {
    einstellungenLaden();
  }
}

async function uebersichtLaden() {
  try {
    const daten = await api('uebersicht');
    codeEditorAnzeigen(daten.codeEditor);
    kachelnZeichnen(daten);
    verlaufZeichnen(daten.verlauf);
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

function kachel({ ziel, symbol, farbe, titel, text, aktion, neuerTab }) {
  return el('a', { klasse: `kachel ${farbe}`, href: ziel, target: neuerTab ? '_blank' : undefined, rel: neuerTab ? 'noopener' : undefined },
    el('span', { klasse: 'slot' }, icon(symbol)),
    el('span', { klasse: 'kachel-inhalt' }, el('strong', { text: titel }), el('span', { text })),
    el('span', { klasse: 'kachel-aktion' }, aktion, icon('fa-arrow-right')));
}

function kachelnZeichnen(daten) {
  const kategorien = daten.gruppen === 1 ? '1 Kategorie' : `${daten.gruppen} Kategorien`;
  holen('uebersichtKacheln').replaceChildren(
    kachel({
      ziel: '#wiki',
      symbol: 'fa-book-open',
      farbe: 'diamond',
      titel: 'Wiki',
      text: `${daten.artikel} Artikel in ${kategorien}`,
      aktion: 'Artikel schreiben',
    }),
    kachel({
      ziel: '#moments',
      symbol: 'fa-film',
      farbe: 'gold',
      titel: 'Aura Moments',
      text: `${daten.clips} von ${daten.plaetze} Plätzen ${daten.clips === 1 ? 'hat' : 'haben'} einen Clip`,
      aktion: 'Clips verwalten',
    }),
    kachel({
      ziel: daten.codeEditor ? '#code' : '#einstellungen',
      symbol: 'fa-code',
      farbe: 'amethyst',
      titel: 'Code',
      text: daten.codeEditor ? 'Eingeschaltet – HTML, CSS und JavaScript bearbeiten' : 'Ausgeschaltet – in den Einstellungen freischalten',
      aktion: daten.codeEditor ? 'Code öffnen' : 'Zu den Einstellungen',
    }),
    kachel({
      ziel: '../',
      symbol: 'fa-house',
      farbe: 'emerald',
      titel: 'Website',
      text: 'Sieh dir deine Änderungen live an.',
      aktion: 'Website öffnen',
      neuerTab: true,
    }),
  );
}

function verlaufZeichnen(liste) {
  const ziel = holen('verlauf');
  if (!liste || liste.length === 0) {
    ziel.replaceChildren(el('li', { klasse: 'leer-text', text: 'Noch keine Änderungen.' }));
    return;
  }
  ziel.replaceChildren(...liste.map(eintrag => {
    const [symbol, farbe] = VERLAUF_ARTEN[eintrag.art] || ['fa-circle', 'diamond'];
    return el('li', { klasse: `verlauf-eintrag ${farbe}` },
      el('span', { klasse: 'verlauf-symbol' }, icon(symbol)),
      el('span', { text: eintrag.text }),
      el('time', { klasse: 'verlauf-zeit', datetime: eintrag.zeit, title: datumText(eintrag.zeit), text: vorWieLange(eintrag.zeit) }));
  }));
}

async function wikiLaden() {
  try {
    wikiUebernehmen(await api('wiki'));
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

function wikiUebernehmen(daten) {
  const ersterAufruf = !zustand.wiki;
  zustand.wiki = daten;
  if (ersterAufruf) {
    farbenAnlegen(daten.farben);
  }
  wikiZeichnen();
}

function wikiZeichnen() {
  const { gruppen, artikel } = zustand.wiki;
  const ziel = holen('wikiGruppen');
  if (artikel.length === 0) {
    ziel.replaceChildren(el('div', { klasse: 'karte' }, el('p', { klasse: 'leer-text', text: 'Noch keine Artikel. Leg mit „Neuer Artikel“ los!' })));
  } else {
    ziel.replaceChildren(...gruppen.map((gruppe, index) => gruppeKarte(gruppe, index, gruppen.length, artikel.filter(eintrag => eintrag.kat === gruppe))));
  }
  weiterleitungenZeichnen();
  holen('kategorien').replaceChildren(...gruppen.map(gruppe => el('option', { value: gruppe })));
}

function gruppeKarte(gruppe, index, anzahl, artikelListe) {
  return el('div', { klasse: 'karte wiki-gruppe' },
    el('div', { klasse: 'karte-kopf' },
      el('div', { klasse: 'gruppe-titelzeile' },
        el('h2', { klasse: 'karte-titel', text: gruppe }),
        el('span', { klasse: 'zaehler', text: artikelListe.length })),
      el('div', { klasse: 'knopf-reihe' },
        knopf({ symbol: 'fa-arrow-up', titel: 'Kategorie nach oben', klasse: 'symbol leise', aus: index === 0, klick: () => wikiVerschieben({ gruppe, richtung: -1 }) }),
        knopf({ symbol: 'fa-arrow-down', titel: 'Kategorie nach unten', klasse: 'symbol leise', aus: index === anzahl - 1, klick: () => wikiVerschieben({ gruppe, richtung: 1 }) }),
        knopf({ symbol: 'fa-pen', titel: 'Kategorie umbenennen', klasse: 'symbol leise', klick: () => gruppeUmbenennen(gruppe) }))),
    el('ul', { klasse: 'artikel-liste' }, ...artikelListe.map((artikel, position) => artikelZeile(artikel, position, artikelListe.length))));
}

function artikelZeile(artikel, position, anzahl) {
  return el('li', { klasse: 'artikel-zeile' },
    el('span', { klasse: `slot ${artikel.farbe}` }, icon(artikel.icon)),
    el('div', { klasse: 'artikel-info' },
      el('button', { type: 'button', klasse: 'artikel-titel', text: artikel.titel, bei: { click: () => editorOeffnen(artikel.slug) } }),
      el('span', { klasse: 'artikel-meta' }, el('code', { text: `/wiki/${artikel.slug}/` }), ` · ${artikel.minuten} Min. Lesezeit`)),
    el('div', { klasse: 'knopf-reihe' },
      knopf({ symbol: 'fa-arrow-up', titel: 'Nach oben', klasse: 'symbol leise', aus: position === 0, klick: () => wikiVerschieben({ slug: artikel.slug, richtung: -1 }) }),
      knopf({ symbol: 'fa-arrow-down', titel: 'Nach unten', klasse: 'symbol leise', aus: position === anzahl - 1, klick: () => wikiVerschieben({ slug: artikel.slug, richtung: 1 }) }),
      el('a', { klasse: 'knopf symbol leise', href: `../wiki/${artikel.slug}/`, target: '_blank', rel: 'noopener', title: 'Auf der Website ansehen', 'aria-label': 'Auf der Website ansehen' }, icon('fa-arrow-up-right-from-square')),
      knopf({ text: 'Bearbeiten', symbol: 'fa-pen', klasse: 'klein', klick: () => editorOeffnen(artikel.slug) }),
      knopf({ symbol: 'fa-trash', titel: 'Löschen', klasse: 'symbol gefahr', klick: () => artikelLoeschen(artikel) })));
}

function weiterleitungenZeichnen() {
  const { weiterleitungen, artikel } = zustand.wiki;
  const ziel = holen('weiterleitungen');
  if (weiterleitungen.length === 0) {
    ziel.replaceChildren(el('p', { klasse: 'leer-text', text: 'Noch keine Weiterleitungen.' }));
  } else {
    ziel.replaceChildren(...weiterleitungen.map(weiterleitung => {
      const zielArtikel = artikel.find(eintrag => eintrag.slug === weiterleitung.nach);
      return el('div', { klasse: 'weiterleitung' },
        el('code', { text: `/wiki/${weiterleitung.von}/` }),
        icon('fa-arrow-right-long'),
        el('span', { text: zielArtikel ? zielArtikel.titel : 'Wiki-Übersicht' }),
        knopf({ symbol: 'fa-trash', titel: 'Weiterleitung löschen', klasse: 'symbol leise', klick: () => weiterleitungLoeschen(weiterleitung.von) }));
    }));
  }

  const auswahl = holen('weiterleitungNach');
  const vorher = auswahl.value;
  auswahl.replaceChildren(
    el('option', { value: '', text: 'Wiki-Übersicht' }),
    ...artikel.map(eintrag => el('option', { value: eintrag.slug, text: eintrag.titel })));
  if ([...auswahl.options].some(option => option.value === vorher)) {
    auswahl.value = vorher;
  }
}

async function wikiAktion(pfad, daten, erfolg) {
  try {
    const ergebnis = await api(pfad, daten);
    wikiUebernehmen(ergebnis.wiki);
    if (erfolg) {
      melden(erfolg);
    }
    return ergebnis;
  } catch (fehler) {
    melden(fehler.message, 'fehler');
    return null;
  }
}

function wikiVerschieben(daten) {
  return wikiAktion('wiki/verschieben', daten);
}

async function gruppeUmbenennen(gruppe) {
  const eingabe = el('input', { klasse: 'eingabe', maxlength: 40, wert: gruppe });
  const ok = await dialogZeigen({
    titel: 'Kategorie umbenennen',
    inhalt: feldMitName('Neuer Name', eingabe, 'Alle Artikel dieser Kategorie ziehen mit um.'),
    ok: 'Umbenennen',
  });
  if (!ok || eingabe.value.trim() === '' || eingabe.value.trim() === gruppe) {
    return;
  }
  await wikiAktion('wiki/gruppe', { alt: gruppe, neu: eingabe.value }, 'Kategorie umbenannt.');
}

async function artikelLoeschen(artikel) {
  const auswahl = el('select', { klasse: 'eingabe' },
    el('option', { value: '', text: 'zur Wiki-Übersicht (empfohlen)' }),
    ...zustand.wiki.artikel
      .filter(eintrag => eintrag.slug !== artikel.slug)
      .map(eintrag => el('option', { value: eintrag.slug, text: `zum Artikel „${eintrag.titel}“` })),
    el('option', { value: '-', text: 'nirgendwohin – die Adresse ist danach weg' }));
  const ok = await dialogZeigen({
    titel: `„${artikel.titel}“ löschen?`,
    text: 'Der Artikel verschwindet sofort von der Website. Das lässt sich nicht rückgängig machen.',
    inhalt: feldMitName(`Wer /wiki/${artikel.slug}/ aufruft, kommt danach …`, auswahl),
    ok: 'Endgültig löschen',
    gefahr: true,
  });
  if (!ok) {
    return;
  }
  const nach = auswahl.value === '-' ? null : auswahl.value;
  const ergebnis = await wikiAktion('wiki/loeschen', { slug: artikel.slug, nach }, `„${artikel.titel}“ wurde gelöscht.`);
  if (ergebnis && zustand.editor && zustand.editor.alterSlug === artikel.slug) {
    editorZuklappen();
  }
}

async function weiterleitungLoeschen(von) {
  const ok = await dialogZeigen({
    titel: 'Weiterleitung löschen?',
    text: `Wer danach /wiki/${von}/ aufruft, bekommt „Seite nicht gefunden“ angezeigt.`,
    ok: 'Löschen',
    gefahr: true,
  });
  if (ok) {
    await wikiAktion('wiki/weiterleitung-loeschen', { von }, 'Weiterleitung gelöscht.');
  }
}

holen('weiterleitungFormular').addEventListener('submit', async event => {
  event.preventDefault();
  const von = holen('weiterleitungVon').value;
  const ergebnis = await wikiAktion('wiki/weiterleitung', { von, nach: holen('weiterleitungNach').value }, 'Weiterleitung angelegt.');
  if (ergebnis) {
    holen('weiterleitungVon').value = '';
  }
});

function farbenAnlegen(farben) {
  holen('farbWahl').replaceChildren(...farben.map(farbe => el('label', { klasse: `farbe ${farbe}` },
    el('input', { type: 'radio', name: 'farbe', value: farbe, bei: { change: () => farbeSetzen(farbe) } }),
    el('span', { klasse: 'farbe-punkt', 'aria-hidden': 'true' }),
    el('span', { text: FARB_NAMEN[farbe] || farbe }))));
}

function farbeSetzen(farbe) {
  for (const eingabe of document.querySelectorAll('input[name="farbe"]')) {
    eingabe.checked = eingabe.value === farbe;
    eingabe.parentElement.classList.toggle('gewaehlt', eingabe.checked);
  }
  holen('symbolVorschau').className = `slot ${farbe}`;
  editorGeaendertAnzeigen();
}

function gewaehlteFarbe() {
  const gewaehlt = document.querySelector('input[name="farbe"]:checked');
  return gewaehlt ? gewaehlt.value : '';
}

function symboleAnlegen() {
  holen('symbolListe').replaceChildren(...SYMBOLE.map(name => el('button', {
    type: 'button',
    klasse: 'symbol-knopf',
    title: name,
    'aria-label': name,
    daten: { symbol: name },
    bei: { click: () => symbolSetzen(name) },
  }, icon(name))));
}

function symbolSetzen(name) {
  holen('feldIcon').value = name;
  symbolVorschauZeigen();
}

function symbolVorschauZeigen() {
  const name = holen('feldIcon').value.trim();
  holen('symbolVorschau').replaceChildren(icon(/^fa-[a-z0-9-]+$/.test(name) ? name : 'fa-question'));
  for (const knopfElement of holen('symbolListe').children) {
    knopfElement.classList.toggle('aktiv', knopfElement.dataset.symbol === name);
  }
  editorGeaendertAnzeigen();
}

function editorWerte() {
  return {
    titel: holen('feldTitel').value,
    slug: holen('feldSlug').value,
    kurz: holen('feldKurz').value,
    kat: holen('feldKat').value,
    farbe: gewaehlteFarbe(),
    icon: holen('feldIcon').value,
    text: holen('feldText').value,
    stichworte: holen('feldStichworte').value,
    inhalt: holen('feldInhalt').value,
  };
}

function editorGeaendert() {
  return Boolean(zustand.editor) && JSON.stringify(editorWerte()) !== zustand.editor.gespeichert;
}

function editorGeaendertAnzeigen() {
  if (!zustand.editor) {
    return;
  }
  const status = holen('editorStatus');
  if (editorGeaendert()) {
    status.className = 'status ungespeichert';
    status.textContent = 'Nicht gespeichert';
  } else {
    status.className = 'status gespeichert';
    status.textContent = zustand.editor.alterSlug ? 'Alles gespeichert' : 'Noch nicht veröffentlicht';
  }
}

function slugHilfeZeigen() {
  const hilfe = holen('slugHilfe');
  const slug = holen('feldSlug').value.trim();
  if (!zustand.editor) {
    return;
  }
  if (!zustand.editor.alterSlug) {
    hilfe.textContent = zustand.editor.slugAutomatisch ? 'Wird automatisch aus dem Titel erzeugt.' : 'Nur kleine Buchstaben, Zahlen und Bindestriche.';
  } else if (slug !== zustand.editor.alterSlug) {
    hilfe.textContent = `Die alte Adresse /wiki/${zustand.editor.alterSlug}/ leitet danach automatisch hierher weiter.`;
  } else {
    hilfe.textContent = '';
  }
}

function editorFuellen(artikel) {
  const werte = artikel || {
    titel: '',
    slug: '',
    kurz: '',
    kat: zustand.wiki.gruppen[0] || '',
    farbe: 'diamond',
    icon: 'fa-book',
    text: '',
    stichworte: '',
    inhalt: NEUER_INHALT,
  };
  holen('feldTitel').value = werte.titel;
  holen('feldSlug').value = werte.slug;
  holen('feldKurz').value = artikel && artikel.kurz !== artikel.titel ? artikel.kurz : '';
  holen('feldKat').value = werte.kat;
  holen('feldIcon').value = werte.icon;
  holen('feldText').value = werte.text;
  holen('feldStichworte').value = werte.stichworte;
  holen('feldInhalt').value = werte.inhalt;
  farbeSetzen(werte.farbe);
  symbolVorschauZeigen();
}

function editorKopfAktualisieren() {
  const artikel = zustand.editor.alterSlug ? zustand.wiki.artikel.find(eintrag => eintrag.slug === zustand.editor.alterSlug) : null;
  holen('editorMarke').textContent = artikel ? 'Artikel bearbeiten' : 'Neuer Artikel';
  holen('editorTitel').textContent = artikel ? artikel.titel : 'Neuer Artikel';
  holen('editorAnsehen').hidden = !artikel;
  if (artikel) {
    holen('editorAnsehen').href = `../wiki/${artikel.slug}/`;
  }
}

async function editorOeffnen(slug) {
  if (zustand.editor && editorGeaendert() && zustand.editor.alterSlug !== slug) {
    const ok = await dialogZeigen({
      titel: 'Änderungen verwerfen?',
      text: 'Im geöffneten Artikel gibt es Änderungen, die noch nicht gespeichert sind.',
      ok: 'Verwerfen',
      gefahr: true,
    });
    if (!ok) {
      return;
    }
  }
  const artikel = slug ? zustand.wiki.artikel.find(eintrag => eintrag.slug === slug) : null;
  zustand.editor = { alterSlug: artikel ? artikel.slug : null, slugAutomatisch: !artikel, gespeichert: '' };
  editorFuellen(artikel);
  zustand.editor.gespeichert = JSON.stringify(editorWerte());
  editorKopfAktualisieren();
  slugHilfeZeigen();
  editorGeaendertAnzeigen();

  holen('wikiListe').hidden = true;
  holen('wikiEditor').hidden = false;
  if (location.hash !== '#wiki') {
    location.hash = 'wiki';
  }
  window.scrollTo(0, 0);
  if (!artikel) {
    holen('feldTitel').focus();
  }
  vorschauPlanen(0);
}

function editorZuklappen() {
  zustand.editor = null;
  clearTimeout(zustand.vorschauTimer);
  holen('wikiEditor').hidden = true;
  holen('wikiListe').hidden = false;
  holen('vorschau').removeAttribute('srcdoc');
  window.scrollTo(0, 0);
}

async function editorSchliessen() {
  if (editorGeaendert()) {
    const ok = await dialogZeigen({
      titel: 'Änderungen verwerfen?',
      text: 'Du hast Änderungen, die noch nicht gespeichert sind.',
      ok: 'Verwerfen',
      gefahr: true,
    });
    if (!ok) {
      return;
    }
  }
  editorZuklappen();
}

async function artikelSpeichern() {
  if (!zustand.editor || zustand.editor.speichert) {
    return;
  }
  const speichernKnopf = holen('editorSpeichern');
  zustand.editor.speichert = true;
  speichernKnopf.disabled = true;
  const warNeu = !zustand.editor.alterSlug;
  try {
    const ergebnis = await api('wiki/artikel', { alterSlug: zustand.editor.alterSlug, artikel: editorWerte() });
    wikiUebernehmen(ergebnis.wiki);
    const artikel = zustand.wiki.artikel.find(eintrag => eintrag.slug === ergebnis.slug);
    const inhaltFeld = holen('feldInhalt');
    const position = inhaltFeld.selectionStart;
    const scroll = inhaltFeld.scrollTop;
    zustand.editor.alterSlug = artikel.slug;
    zustand.editor.slugAutomatisch = false;
    editorFuellen(artikel);
    inhaltFeld.scrollTop = scroll;
    inhaltFeld.setSelectionRange(position, position);
    zustand.editor.gespeichert = JSON.stringify(editorWerte());
    editorKopfAktualisieren();
    slugHilfeZeigen();
    editorGeaendertAnzeigen();
    vorschauPlanen(0);
    melden(warNeu ? 'Artikel erstellt – er ist jetzt auf der Website.' : 'Gespeichert – die Änderungen sind jetzt live.');
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  } finally {
    if (zustand.editor) {
      zustand.editor.speichert = false;
    }
    speichernKnopf.disabled = false;
  }
}

function vorschauPlanen(verzoegerung = 650) {
  clearTimeout(zustand.vorschauTimer);
  zustand.vorschauTimer = setTimeout(vorschauLaden, verzoegerung);
}

async function vorschauLaden() {
  if (!zustand.editor || holen('wikiEditor').hidden || holen('schreibFlaeche').dataset.ansicht === 'schreiben') {
    return;
  }
  zustand.vorschauNummer += 1;
  const nummer = zustand.vorschauNummer;
  let html;
  try {
    ({ html } = await api('wiki/vorschau', { alterSlug: zustand.editor.alterSlug, artikel: editorWerte() }));
  } catch {
    return;
  }
  if (nummer !== zustand.vorschauNummer) {
    return;
  }
  const rahmen = holen('vorschau');
  let scroll = 0;
  try {
    scroll = rahmen.contentWindow.scrollY;
  } catch {
    scroll = 0;
  }
  rahmen.addEventListener('load', () => {
    try {
      rahmen.contentWindow.scrollTo({ top: scroll, behavior: 'instant' });
    } catch {
      return;
    }
  }, { once: true });
  const basis = new URL('../wiki/vorschau/', location.href).href;
  rahmen.srcdoc = html.replace('<head>', `<head>\n<base href="${basis}">`);
}

function ansichtSetzen(ansicht) {
  holen('schreibFlaeche').dataset.ansicht = ansicht;
  for (const knopfElement of document.querySelectorAll('[data-ansicht-wahl]')) {
    knopfElement.setAttribute('aria-pressed', String(knopfElement.dataset.ansichtWahl === ansicht));
  }
  try {
    localStorage.setItem('bucksmp-admin-ansicht', ansicht);
  } catch {
    return;
  }
  if (ansicht !== 'schreiben') {
    vorschauPlanen(0);
  }
}

function startAnsicht() {
  let gespeichert = null;
  try {
    gespeichert = localStorage.getItem('bucksmp-admin-ansicht');
  } catch {
    gespeichert = null;
  }
  if (['schreiben', 'geteilt', 'vorschau'].includes(gespeichert)) {
    return gespeichert;
  }
  return window.innerWidth >= 1200 ? 'geteilt' : 'schreiben';
}

function einfuegen(feld, text) {
  feld.focus();
  let geklappt = false;
  try {
    geklappt = document.execCommand('insertText', false, text);
  } catch {
    geklappt = false;
  }
  if (!geklappt) {
    feld.setRangeText(text, feld.selectionStart, feld.selectionEnd, 'end');
    feld.dispatchEvent(new Event('input', { bubbles: true }));
  }
}

function blockEnde(text, position) {
  const zeilenAnfang = text.lastIndexOf('\n', position - 1) + 1;
  const zeilenSchluss = text.indexOf('\n', position);
  const zeile = text.slice(zeilenAnfang, zeilenSchluss === -1 ? text.length : zeilenSchluss);
  if (zeile.trim() === '') {
    return position;
  }
  const leereZeile = text.indexOf('\n\n', position);
  return leereZeile === -1 ? text.length : leereZeile;
}

function werkzeugBenutzen(werkzeug) {
  if (werkzeug.name === 'link') {
    linkEinfuegen();
    return;
  }
  const feld = holen('feldInhalt');
  let start = feld.selectionStart;
  let ende = feld.selectionEnd;
  const markiert = feld.value.slice(start, ende);
  if (werkzeug.block && markiert === '') {
    start = blockEnde(feld.value, start);
    ende = start;
    feld.setSelectionRange(start, ende);
  }

  let davor = '';
  let danach = '';
  if (werkzeug.block) {
    const vorher = feld.value.slice(0, start);
    const nachher = feld.value.slice(ende);
    if (vorher !== '' && !vorher.endsWith('\n\n')) {
      davor = vorher.endsWith('\n') ? '\n' : '\n\n';
    }
    if (nachher !== '' && !nachher.startsWith('\n\n')) {
      danach = nachher.startsWith('\n') ? '\n' : '\n\n';
    }
  }

  let einfuegung;
  let auswahlStart;
  let auswahlEnde;
  if (werkzeug.fertig) {
    einfuegung = davor + werkzeug.fertig + danach;
    auswahlStart = start + davor.length + werkzeug.fertig.indexOf(werkzeug.markieren);
    auswahlEnde = auswahlStart + werkzeug.markieren.length;
  } else {
    const kern = markiert || werkzeug.platzhalter;
    einfuegung = davor + werkzeug.vorne + kern + werkzeug.hinten + danach;
    auswahlStart = start + davor.length + werkzeug.vorne.length;
    auswahlEnde = auswahlStart + kern.length;
  }
  einfuegen(feld, einfuegung);
  feld.setSelectionRange(auswahlStart, auswahlEnde);
}

async function linkEinfuegen() {
  const feld = holen('feldInhalt');
  const start = feld.selectionStart;
  const ende = feld.selectionEnd;
  const markiert = feld.value.slice(start, ende);

  const auswahl = el('select', { klasse: 'eingabe' },
    el('optgroup', { label: 'Wiki-Artikel' }, ...zustand.wiki.artikel.map(artikel => el('option', { value: `../${artikel.slug}/`, text: artikel.titel }))),
    el('optgroup', { label: 'Seiten der Website' }, ...SEITEN_LINKS.map(([name, ziel]) => el('option', { value: ziel, text: name }))),
    el('optgroup', { label: 'Andere' }, el('option', { value: 'eigen', text: 'Eigene Adresse …' })));
  const adresse = el('input', { klasse: 'eingabe', type: 'url', placeholder: 'https://…' });
  const adresseFeld = feldMitName('Adresse', adresse, 'Zum Beispiel https://www.minecraft.net/');
  adresseFeld.hidden = true;
  auswahl.addEventListener('change', () => {
    adresseFeld.hidden = auswahl.value !== 'eigen';
  });
  const linkText = el('input', { klasse: 'eingabe', placeholder: 'Leer lassen = Name des Ziels' });

  const ok = await dialogZeigen({
    titel: 'Link einfügen',
    inhalt: el('div', { klasse: 'dialog-inhalt' },
      feldMitName('Ziel', auswahl),
      adresseFeld,
      markiert ? el('p', { text: 'Der markierte Text wird zum Link.' }) : feldMitName('Linktext', linkText)),
    ok: 'Einfügen',
  });
  if (!ok) {
    return;
  }

  const ziel = auswahl.value === 'eigen' ? adresse.value.trim() : auswahl.value;
  if (!/^(https?:\/\/|mailto:|\/|\.{1,2}\/|#)/i.test(ziel)) {
    melden('Bitte gib eine Adresse an, die mit https:// beginnt.', 'fehler');
    return;
  }
  const extern = /^https?:\/\//i.test(ziel);
  const inhalt = markiert || linkText.value.trim() || auswahl.selectedOptions[0].textContent;
  const html = `<a href="${ziel.replace(/"/g, '&quot;')}"${extern ? ' target="_blank" rel="noopener"' : ''}>${inhalt}</a>`;
  feld.focus();
  feld.setSelectionRange(start, ende);
  einfuegen(feld, html);
}

function werkzeugeAnlegen() {
  holen('werkzeuge').replaceChildren(...WERKZEUGE.map(werkzeug => el('button', {
    type: 'button',
    klasse: 'werkzeug',
    title: werkzeug.text,
    bei: { click: () => werkzeugBenutzen(werkzeug) },
  }, icon(werkzeug.symbol), el('span', { text: werkzeug.text }))));
}

async function momentsLaden() {
  try {
    const daten = await api('moments');
    zustand.maxUploadMb = daten.maxUploadMb;
    zustand.moments = daten.moments;
    holen('momentsMax').textContent = daten.maxUploadMb;
    holen('momentsWarnung').hidden = !daten.lesefehler;
    momentsZeichnen();
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

function momentsZeichnen() {
  const liste = holen('momentListe');
  const vorhandene = new Map([...liste.children].map(karte => [Number(karte.dataset.platz), karte]));
  const neu = zustand.moments.map(moment => {
    if (zustand.uploads.has(moment.platz) && vorhandene.has(moment.platz)) {
      return vorhandene.get(moment.platz);
    }
    return momentKarte(moment);
  });
  liste.replaceChildren(...neu);
}

function kartenAktualisieren(plaetze) {
  for (const platz of plaetze) {
    if (zustand.uploads.has(platz)) {
      continue;
    }
    const alt = document.querySelector(`.moment-karte[data-platz="${platz}"]`);
    if (alt) {
      alt.replaceWith(momentKarte(zustand.moments[platz - 1]));
    }
  }
}

function platzKlasse(platz) {
  if (platz <= 3) {
    return `top${platz}`;
  }
  return '';
}

function medienAnzeigen(moment) {
  const medien = el('div', { klasse: 'moment-medien' });
  if (moment.medien) {
    const quelle = `../top/10/aura-moments/media/${moment.medien.datei}?v=${moment.medien.geaendert}`;
    if (moment.medien.art === 'video') {
      const video = el('video', { src: quelle, controls: true, playsinline: true, preload: 'metadata' });
      video.muted = true;
      video.addEventListener('error', () => {
        medien.append(el('p', { klasse: 'medien-hinweis', text: 'Dieser Browser kann das Video nicht abspielen. Tipp: als MP4 mit H.264 exportieren.' }));
      }, { once: true });
      medien.append(video);
    } else {
      medien.append(el('img', { src: quelle, alt: `Bild für Platz ${moment.platz}` }));
    }
  } else {
    medien.append(el('div', { klasse: 'moment-leer' },
      icon('fa-cloud-arrow-up'),
      el('strong', { text: 'Noch kein Clip' }),
      el('span', { text: 'Datei hierher ziehen oder „Hochladen“ klicken' })));
  }
  medien.append(el('span', { klasse: `moment-platz ${platzKlasse(moment.platz)}`, text: `#${moment.platz}` }));
  return medien;
}

function momentKarte(moment) {
  const platz = moment.platz;
  const entwurf = zustand.entwuerfe.get(platz);
  const werte = entwurf || moment;
  const karte = el('article', { klasse: 'karte moment-karte', daten: { platz } });

  const medien = medienAnzeigen(moment);
  const dateiWahl = el('input', {
    type: 'file',
    accept: 'video/mp4,video/quicktime,.mp4,.m4v,.mov,image/jpeg,image/png,image/webp,image/gif',
    hidden: true,
    bei: {
      change: () => {
        if (dateiWahl.files[0]) {
          dateiHochladen(platz, dateiWahl.files[0]);
        }
        dateiWahl.value = '';
      },
    },
  });

  medien.addEventListener('dragover', event => {
    event.preventDefault();
    medien.classList.add('ziehen');
  });
  medien.addEventListener('dragleave', () => medien.classList.remove('ziehen'));
  medien.addEventListener('drop', event => {
    event.preventDefault();
    medien.classList.remove('ziehen');
    const datei = event.dataTransfer.files[0];
    if (datei) {
      dateiHochladen(platz, datei);
    }
  });

  const titel = el('input', { klasse: 'eingabe', maxlength: 80, wert: werte.title, placeholder: 'z. B. Der Clutch des Jahres' });
  const spieler = el('input', { klasse: 'eingabe', maxlength: 40, wert: werte.player, placeholder: 'z. B. .erbse' });
  const beschreibung = el('textarea', { klasse: 'eingabe', maxlength: 300, rows: 2, placeholder: 'Was ist passiert?' });
  beschreibung.value = werte.description;

  const speichern = knopf({ text: 'Texte speichern', symbol: 'fa-floppy-disk', klasse: 'klein haupt', aus: !entwurf, klick: () => momentTexteSpeichern(platz) });
  const beiEingabe = () => {
    const neu = { title: titel.value, player: spieler.value, description: beschreibung.value };
    const gleich = neu.title === moment.title && neu.player === moment.player && neu.description === moment.description;
    if (gleich) {
      zustand.entwuerfe.delete(platz);
    } else {
      zustand.entwuerfe.set(platz, neu);
    }
    speichern.disabled = gleich;
  };
  for (const feld of [titel, spieler, beschreibung]) {
    feld.addEventListener('input', beiEingabe);
  }

  const info = moment.medien
    ? el('span', { klasse: 'moment-info' }, icon(moment.medien.art === 'video' ? 'fa-video' : 'fa-image'), el('strong', { text: moment.medien.art === 'video' ? 'Video' : 'Bild' }), groesseText(moment.medien.groesse))
    : el('span', { klasse: 'moment-info' }, icon('fa-film'), 'Noch kein Clip');

  karte.append(
    medien,
    el('div', { klasse: 'moment-koerper' },
      el('div', { klasse: 'moment-zeile' },
        info,
        el('div', { klasse: 'knopf-reihe' },
          knopf({ symbol: 'fa-arrow-up', titel: `Mit Platz ${platz - 1} tauschen`, klasse: 'symbol leise', aus: platz === 1, klick: () => momentTauschen(platz, platz - 1) }),
          knopf({ symbol: 'fa-arrow-down', titel: `Mit Platz ${platz + 1} tauschen`, klasse: 'symbol leise', aus: platz === zustand.moments.length, klick: () => momentTauschen(platz, platz + 1) }),
          knopf({ text: moment.medien ? 'Ersetzen' : 'Hochladen', symbol: 'fa-cloud-arrow-up', klasse: 'klein', klick: () => dateiWahl.click() }),
          knopf({ symbol: 'fa-trash', titel: 'Clip entfernen', klasse: 'symbol gefahr', aus: !moment.medien, klick: () => momentEntfernen(platz) }),
          dateiWahl)),
      el('div', { klasse: 'moment-felder' },
        feldMitName('Titel', titel),
        feldMitName('Spieler', spieler),
        el('label', { klasse: 'feld breit' }, el('span', { klasse: 'feld-name', text: 'Beschreibung' }), beschreibung)),
      el('div', { klasse: 'moment-fuss' }, speichern)));
  return karte;
}

async function momentTexteSpeichern(platz) {
  const entwurf = zustand.entwuerfe.get(platz);
  if (!entwurf) {
    return;
  }
  try {
    const daten = await api('moments/text', { platz, ...entwurf });
    zustand.entwuerfe.delete(platz);
    zustand.moments = daten.moments;
    kartenAktualisieren([platz]);
    melden(`Texte für Platz ${platz} gespeichert.`);
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

async function momentTauschen(a, b) {
  if (zustand.uploads.has(a) || zustand.uploads.has(b)) {
    melden('Warte kurz, bis der Upload fertig ist.', 'info');
    return;
  }
  try {
    const daten = await api('moments/tauschen', { a, b });
    const entwurfA = zustand.entwuerfe.get(a);
    const entwurfB = zustand.entwuerfe.get(b);
    zustand.entwuerfe.delete(a);
    zustand.entwuerfe.delete(b);
    if (entwurfA) {
      zustand.entwuerfe.set(b, entwurfA);
    }
    if (entwurfB) {
      zustand.entwuerfe.set(a, entwurfB);
    }
    zustand.moments = daten.moments;
    kartenAktualisieren([a, b]);
    const ziel = document.querySelector(`.moment-karte[data-platz="${b}"]`);
    if (ziel) {
      ziel.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

async function momentEntfernen(platz) {
  const ok = await dialogZeigen({
    titel: `Clip von Platz ${platz} entfernen?`,
    text: 'Die Datei wird vom Server gelöscht. Titel, Spieler und Beschreibung bleiben erhalten.',
    ok: 'Entfernen',
    gefahr: true,
  });
  if (!ok) {
    return;
  }
  try {
    const daten = await api('moments/entfernen', { platz });
    zustand.moments = daten.moments;
    kartenAktualisieren([platz]);
    melden(`Clip von Platz ${platz} entfernt.`);
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

async function alsJpg(datei) {
  let bild;
  try {
    bild = await createImageBitmap(datei);
  } catch {
    throw new Error('Dieses Bild kann dein Browser nicht öffnen. Versuch es mit JPG oder PNG.');
  }
  const faktor = Math.min(1, 1920 / bild.width, 1920 / bild.height);
  const breite = Math.max(1, Math.round(bild.width * faktor));
  const hoehe = Math.max(1, Math.round(bild.height * faktor));
  const leinwand = document.createElement('canvas');
  leinwand.width = breite;
  leinwand.height = hoehe;
  const zeichnen = leinwand.getContext('2d');
  zeichnen.fillStyle = '#0b0d10';
  zeichnen.fillRect(0, 0, breite, hoehe);
  zeichnen.drawImage(bild, 0, 0, breite, hoehe);
  bild.close();
  return new Promise((fertig, fehlgeschlagen) => {
    leinwand.toBlob(blob => {
      if (blob) {
        fertig(blob);
      } else {
        fehlgeschlagen(new Error('Das Bild konnte nicht umgewandelt werden.'));
      }
    }, 'image/jpeg', 0.9);
  });
}

function fortschrittZeigen(platz, anteil, text) {
  const karte = document.querySelector(`.moment-karte[data-platz="${platz}"]`);
  if (!karte) {
    return;
  }
  const medien = karte.querySelector('.moment-medien');
  let anzeige = medien.querySelector('.fortschritt');
  if (!anzeige) {
    anzeige = el('div', { klasse: 'fortschritt' },
      el('div', { klasse: 'fortschritt-bahn' }, el('div', { klasse: 'fortschritt-balken' })),
      el('span', { klasse: 'fortschritt-text' }));
    medien.append(anzeige);
    for (const knopfElement of karte.querySelectorAll('.moment-zeile button')) {
      knopfElement.disabled = true;
    }
  }
  anzeige.querySelector('.fortschritt-balken').style.width = `${Math.round(anteil * 100)}%`;
  anzeige.querySelector('.fortschritt-text').textContent = text || `${Math.round(anteil * 100)} %`;
}

function hochladenMitFortschritt(platz, inhalt, beiFortschritt) {
  return new Promise((fertig, fehlgeschlagen) => {
    const anfrage = new XMLHttpRequest();
    anfrage.open('PUT', `${API}moments/datei?platz=${platz}`);
    anfrage.setRequestHeader('X-BuckSMP-Admin', '1');
    anfrage.setRequestHeader('Content-Type', inhalt.type || 'application/octet-stream');
    anfrage.upload.addEventListener('progress', event => {
      if (event.lengthComputable) {
        beiFortschritt(event.loaded / event.total);
      }
    });
    anfrage.addEventListener('load', () => {
      let daten = null;
      try {
        daten = JSON.parse(anfrage.responseText);
      } catch {
        daten = null;
      }
      if (anfrage.status === 401) {
        abgemeldet();
        fehlgeschlagen(new Error('Deine Anmeldung ist abgelaufen. Bitte melde dich neu an.'));
      } else if (anfrage.status >= 200 && anfrage.status < 300 && daten) {
        fertig(daten);
      } else {
        fehlgeschlagen(new Error((daten && daten.fehler) || `Der Upload ist fehlgeschlagen (${anfrage.status}).`));
      }
    });
    anfrage.addEventListener('error', () => fehlgeschlagen(new Error('Der Upload ist fehlgeschlagen. Prüfe deine Verbindung.')));
    anfrage.send(inhalt);
  });
}

async function dateiHochladen(platz, datei) {
  if (zustand.uploads.has(platz)) {
    return;
  }
  const istBild = datei.type.startsWith('image/') || /\.(png|jpe?g|webp|gif|bmp|avif)$/i.test(datei.name);
  const istVideo = ['video/mp4', 'video/quicktime', 'video/x-m4v'].includes(datei.type) || /\.(mp4|m4v|mov)$/i.test(datei.name);
  if (!istBild && !istVideo) {
    melden(datei.type.startsWith('video/') ? 'Videos bitte als MP4 hochladen.' : 'Bitte wähle ein Video (MP4) oder ein Bild aus.', 'fehler');
    return;
  }

  zustand.uploads.add(platz);
  fortschrittZeigen(platz, 0, istBild ? 'Bild wird vorbereitet …' : '0 %');
  try {
    const inhalt = istBild ? await alsJpg(datei) : datei;
    if (inhalt.size > zustand.maxUploadMb * 1024 * 1024) {
      throw new Error(`Die Datei ist zu groß – höchstens ${zustand.maxUploadMb} MB.`);
    }
    const daten = await hochladenMitFortschritt(platz, inhalt, anteil => fortschrittZeigen(platz, anteil));
    zustand.moments = daten.moments;
    melden(`${istBild ? 'Bild' : 'Video'} für Platz ${platz} hochgeladen – es ist jetzt auf der Website.`);
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  } finally {
    zustand.uploads.delete(platz);
    kartenAktualisieren([platz]);
  }
}

async function codeLaden() {
  try {
    const { dateien } = await api('code/dateien');
    zustand.codeDateien = dateien;
    codeListeZeichnen();
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

function codeListeZeichnen() {
  const suche = holen('codeSuche').value.trim().toLowerCase();
  const liste = zustand.codeDateien.filter(datei => datei.pfad.toLowerCase().includes(suche));
  const ziel = holen('codeDateiListe');
  if (liste.length === 0) {
    ziel.replaceChildren(el('p', { klasse: 'leer-text', text: 'Keine Datei gefunden.' }));
    return;
  }
  ziel.replaceChildren(...liste.map(datei => {
    const teile = datei.pfad.split('/');
    const name = teile.pop();
    const endung = name.split('.').pop();
    const aktiv = zustand.codeDatei && zustand.codeDatei.pfad === datei.pfad;
    return el('button', {
      type: 'button',
      klasse: `code-datei${aktiv ? ' aktiv' : ''}`,
      title: datei.pfad,
      bei: { click: () => codeDateiOeffnen(datei.pfad) },
    },
    icon(DATEI_SYMBOLE[endung] || 'fa-file'),
    el('span', { klasse: 'code-datei-name', text: name }),
    el('span', { klasse: 'code-datei-ordner', text: teile.length ? teile.join('/') : 'Hauptordner' }));
  }));
}

function codeGeaendert() {
  return Boolean(zustand.codeDatei) && holen('codeText').value !== zustand.codeDatei.original;
}

function codeStatusAktualisieren() {
  const status = holen('codeStatus');
  const geaendert = codeGeaendert();
  if (!zustand.codeDatei) {
    status.className = 'status';
    status.textContent = 'Wähle links eine Datei aus.';
  } else if (geaendert) {
    status.className = 'status ungespeichert';
    status.textContent = 'Nicht gespeichert';
  } else {
    status.className = 'status gespeichert';
    status.textContent = 'Gespeichert';
  }
  holen('codeSpeichern').disabled = !zustand.codeDatei || !geaendert;
  holen('codeVerwerfen').disabled = !zustand.codeDatei || !geaendert;
}

function zeilenAktualisieren() {
  const text = holen('codeText');
  const anzahl = text.value.split('\n').length;
  if (anzahl !== zustand.zeilenAnzahl) {
    zustand.zeilenAnzahl = anzahl;
    holen('codeZeilen').textContent = Array.from({ length: anzahl }, (_, index) => index + 1).join('\n');
  }
  holen('codeZeilen').scrollTop = text.scrollTop;
}

async function codeDateiOeffnen(pfad) {
  if (zustand.codeDatei && zustand.codeDatei.pfad === pfad) {
    return;
  }
  if (codeGeaendert()) {
    const ok = await dialogZeigen({
      titel: 'Änderungen verwerfen?',
      text: `In ${zustand.codeDatei.pfad} gibt es Änderungen, die noch nicht gespeichert sind.`,
      ok: 'Verwerfen',
      gefahr: true,
    });
    if (!ok) {
      return;
    }
  }
  try {
    codeDateiAnzeigen(await api(`code/datei?pfad=${encodeURIComponent(pfad)}`));
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

function codeDateiAnzeigen(datei) {
  zustand.codeDatei = { pfad: datei.pfad, geaendert: datei.geaendert, original: datei.inhalt };
  const text = holen('codeText');
  text.disabled = false;
  text.value = datei.inhalt;
  text.scrollTop = 0;
  text.scrollLeft = 0;
  zeilenAktualisieren();
  holen('codePfad').textContent = datei.pfad;
  const ansehen = holen('codeAnsehen');
  ansehen.hidden = !datei.pfad.endsWith('.html');
  ansehen.href = `../${datei.pfad.replace(/index\.html$/, '')}`;
  sicherungenZeichnen(datei.sicherungen);
  codeStatusAktualisieren();
  codeListeZeichnen();
}

function codeZuruecksetzen() {
  zustand.codeDatei = null;
  const text = holen('codeText');
  text.value = '';
  text.disabled = true;
  zeilenAktualisieren();
  holen('codePfad').textContent = 'Keine Datei geöffnet';
  holen('codeAnsehen').hidden = true;
  sicherungenZeichnen([]);
  codeStatusAktualisieren();
}

async function codeSpeichern(erzwingen = false) {
  const datei = zustand.codeDatei;
  if (!datei || zustand.codeSpeichert) {
    return;
  }
  zustand.codeSpeichert = true;
  const inhalt = holen('codeText').value;
  try {
    const ergebnis = await api('code/datei', { pfad: datei.pfad, inhalt, geaendert: datei.geaendert, erzwingen });
    datei.geaendert = ergebnis.geaendert;
    datei.original = inhalt;
    sicherungenZeichnen(ergebnis.sicherungen);
    codeStatusAktualisieren();
    melden(ergebnis.unveraendert ? 'Es gab nichts zu speichern.' : 'Gespeichert – die Änderung ist jetzt live.', ergebnis.unveraendert ? 'info' : 'erfolg');
  } catch (fehler) {
    zustand.codeSpeichert = false;
    if (fehler.status === 409) {
      const ok = await dialogZeigen({
        titel: 'Datei wurde inzwischen geändert',
        text: 'Diese Datei wurde geändert, nachdem du sie geöffnet hast – zum Beispiel in einem anderen Tab. Wenn du trotzdem speicherst, wird der andere Stand überschrieben. Er landet aber vorher in den Sicherungen.',
        ok: 'Trotzdem speichern',
        gefahr: true,
      });
      if (ok) {
        await codeSpeichern(true);
      }
      return;
    }
    melden(fehler.message, 'fehler');
  } finally {
    zustand.codeSpeichert = false;
  }
}

async function codeVerwerfen() {
  if (!codeGeaendert()) {
    return;
  }
  const ok = await dialogZeigen({
    titel: 'Änderungen verwerfen?',
    text: 'Die Datei springt zurück auf den zuletzt gespeicherten Stand.',
    ok: 'Verwerfen',
    gefahr: true,
  });
  if (!ok) {
    return;
  }
  holen('codeText').value = zustand.codeDatei.original;
  zeilenAktualisieren();
  codeStatusAktualisieren();
}

function sicherungenZeichnen(liste) {
  holen('codeSicherungenAnzahl').textContent = liste.length;
  const ziel = holen('codeSicherungsListe');
  if (liste.length === 0) {
    ziel.replaceChildren(el('li', { klasse: 'leer-text', text: zustand.codeDatei ? 'Noch keine Sicherungen. Beim ersten Speichern wird automatisch eine angelegt.' : 'Öffne eine Datei, um ihre Sicherungen zu sehen.' }));
    return;
  }
  ziel.replaceChildren(...liste.map(sicherung => el('li', { klasse: 'sicherung' },
    icon('fa-clock-rotate-left'),
    el('span', { klasse: 'sicherung-zeit', text: `Stand vom ${datumText(sicherung.zeit)}` }),
    el('span', { klasse: 'sicherung-groesse', text: groesseText(sicherung.groesse) }),
    knopf({ text: 'Wiederherstellen', klasse: 'klein leise', klick: () => sicherungWiederherstellen(sicherung) }))));
}

async function sicherungWiederherstellen(sicherung) {
  const datei = zustand.codeDatei;
  if (!datei) {
    return;
  }
  const ok = await dialogZeigen({
    titel: 'Diesen Stand wiederherstellen?',
    text: `${datei.pfad} wird sofort auf den Stand vom ${datumText(sicherung.zeit)} zurückgesetzt – auch auf der Website. Der jetzige Stand wird vorher gesichert${codeGeaendert() ? ', ungespeicherte Änderungen gehen aber verloren' : ''}.`,
    ok: 'Wiederherstellen',
    gefahr: true,
  });
  if (!ok) {
    return;
  }
  try {
    const neu = await api('code/wiederherstellen', { pfad: datei.pfad, sicherung: sicherung.id });
    zustand.codeDatei = null;
    codeDateiAnzeigen(neu);
    melden('Wiederhergestellt – die Datei ist jetzt wieder auf diesem Stand.');
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

async function einstellungenLaden() {
  try {
    const einstellungen = await api('einstellungen');
    codeEditorAnzeigen(einstellungen.codeEditor);
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
}

function tabUndEscape(feld) {
  feld.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
      zustand.tabVerlassen = true;
      return;
    }
    if (event.key === 'Tab' && !event.ctrlKey && !event.metaKey && !event.altKey && !event.shiftKey) {
      if (zustand.tabVerlassen) {
        zustand.tabVerlassen = false;
        return;
      }
      event.preventDefault();
      einfuegen(feld, '  ');
      return;
    }
    zustand.tabVerlassen = false;
  });
}

function hatUngespeichertes() {
  return editorGeaendert() || codeGeaendert() || zustand.entwuerfe.size > 0 || zustand.uploads.size > 0;
}

holen('anmeldeFormular').addEventListener('submit', async event => {
  event.preventDefault();
  const fehlerFeld = holen('anmeldeFehler');
  const anmeldeKnopf = holen('anmeldeKnopf');
  fehlerFeld.hidden = true;
  anmeldeKnopf.disabled = true;
  try {
    const status = await api('anmelden', { passwort: holen('anmeldePasswort').value });
    holen('anmeldePasswort').value = '';
    angemeldet(status);
  } catch (fehler) {
    fehlerFeld.textContent = fehler.message;
    fehlerFeld.hidden = false;
    holen('anmeldePasswort').select();
  } finally {
    anmeldeKnopf.disabled = false;
  }
});

holen('erneutVersuchen').addEventListener('click', starten);

holen('abmelden').addEventListener('click', async () => {
  if (hatUngespeichertes()) {
    const ok = await dialogZeigen({
      titel: 'Wirklich abmelden?',
      text: 'Es gibt noch ungespeicherte Änderungen. Die gehen beim Abmelden verloren.',
      ok: 'Abmelden',
      gefahr: true,
    });
    if (!ok) {
      return;
    }
  }
  try {
    await api('abmelden', {});
  } catch {
    zustand.angemeldet = false;
  }
  zustand.verlassenErlaubt = true;
  location.replace('./');
});

holen('neuerArtikel').addEventListener('click', () => editorOeffnen(null));
holen('editorZurueck').addEventListener('click', editorSchliessen);
holen('editorSpeichern').addEventListener('click', artikelSpeichern);

holen('feldTitel').addEventListener('input', () => {
  if (zustand.editor && zustand.editor.slugAutomatisch) {
    holen('feldSlug').value = slugAusText(holen('feldTitel').value);
  }
  editorGeaendertAnzeigen();
  vorschauPlanen();
});

holen('feldSlug').addEventListener('input', () => {
  if (zustand.editor) {
    zustand.editor.slugAutomatisch = holen('feldSlug').value === '';
  }
  slugHilfeZeigen();
  editorGeaendertAnzeigen();
});

holen('feldSlug').addEventListener('change', () => {
  const feld = holen('feldSlug');
  feld.value = slugAusText(feld.value);
  if (zustand.editor && feld.value === '') {
    zustand.editor.slugAutomatisch = true;
    feld.value = slugAusText(holen('feldTitel').value);
  }
  slugHilfeZeigen();
  editorGeaendertAnzeigen();
  vorschauPlanen();
});

holen('feldIcon').addEventListener('input', () => {
  symbolVorschauZeigen();
  vorschauPlanen();
});

for (const id of ['feldKurz', 'feldKat', 'feldText', 'feldStichworte', 'feldInhalt']) {
  holen(id).addEventListener('input', () => {
    editorGeaendertAnzeigen();
    vorschauPlanen();
  });
}

holen('farbWahl').addEventListener('change', () => vorschauPlanen());

for (const knopfElement of document.querySelectorAll('[data-ansicht-wahl]')) {
  knopfElement.addEventListener('click', () => ansichtSetzen(knopfElement.dataset.ansichtWahl));
}

holen('codeSuche').addEventListener('input', codeListeZeichnen);
holen('codeText').addEventListener('input', () => {
  zeilenAktualisieren();
  codeStatusAktualisieren();
});
holen('codeText').addEventListener('scroll', () => {
  holen('codeZeilen').scrollTop = holen('codeText').scrollTop;
});
holen('codeSpeichern').addEventListener('click', () => codeSpeichern());
holen('codeVerwerfen').addEventListener('click', codeVerwerfen);

tabUndEscape(holen('codeText'));
tabUndEscape(holen('feldInhalt'));

holen('schalterCode').addEventListener('change', async () => {
  const schalter = holen('schalterCode');
  const an = schalter.checked;
  if (an) {
    const ok = await dialogZeigen({
      titel: 'Code-Editor einschalten?',
      text: 'Damit kannst du HTML, CSS und JavaScript der Website direkt ändern. Ein Fehler im Code kann Seiten kaputt machen – vor jedem Speichern wird aber automatisch eine Sicherung angelegt, die du wiederherstellen kannst.',
      ok: 'Einschalten',
    });
    if (!ok) {
      schalter.checked = false;
      return;
    }
  } else if (codeGeaendert()) {
    const ok = await dialogZeigen({
      titel: 'Code-Editor ausschalten?',
      text: 'Im Code-Editor gibt es ungespeicherte Änderungen. Die gehen verloren.',
      ok: 'Ausschalten',
      gefahr: true,
    });
    if (!ok) {
      schalter.checked = true;
      return;
    }
  }
  try {
    const neu = await api('einstellungen', { codeEditor: an });
    codeEditorAnzeigen(neu.codeEditor);
    if (!neu.codeEditor) {
      codeZuruecksetzen();
    }
    melden(neu.codeEditor ? 'Code-Editor eingeschaltet – du findest ihn links im Menü.' : 'Code-Editor ausgeschaltet.');
  } catch (fehler) {
    schalter.checked = !an;
    melden(fehler.message, 'fehler');
  }
});

holen('passwortFormular').addEventListener('submit', async event => {
  event.preventDefault();
  const alt = holen('passwortAlt').value;
  const neu = holen('passwortNeu').value;
  if (neu !== holen('passwortWiederholung').value) {
    melden('Die beiden neuen Passwörter sind nicht gleich.', 'fehler');
    return;
  }
  try {
    await api('passwort', { alt, neu });
    event.target.reset();
    melden('Passwort geändert. Alle anderen Geräte wurden abgemeldet.');
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
});

holen('ueberallAbmelden').addEventListener('click', async () => {
  const ok = await dialogZeigen({
    titel: 'Alle anderen Geräte abmelden?',
    text: 'Überall sonst musst du dich danach neu anmelden. Dieses Gerät bleibt angemeldet.',
    ok: 'Abmelden',
  });
  if (!ok) {
    return;
  }
  try {
    const { abgemeldet: anzahl } = await api('ueberall-abmelden', {});
    melden(anzahl === 0 ? 'Es war kein anderes Gerät angemeldet.' : `${anzahl} ${anzahl === 1 ? 'anderes Gerät' : 'andere Geräte'} abgemeldet.`);
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
});

holen('wikiNeuErzeugen').addEventListener('click', async () => {
  try {
    const { geaendert } = await api('wiki/neu-erzeugen', {});
    melden(geaendert === 0 ? 'Alle Wiki-Seiten waren schon aktuell.' : `${geaendert} Wiki-${geaendert === 1 ? 'Seite' : 'Seiten'} neu geschrieben.`);
  } catch (fehler) {
    melden(fehler.message, 'fehler');
  }
});

holen('dialogAbbrechen').addEventListener('click', () => holen('dialog').close('abbrechen'));

document.addEventListener('keydown', event => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    if (!holen('wikiEditor').hidden && !holen('bereich-wiki').hidden) {
      event.preventDefault();
      artikelSpeichern();
    } else if (!holen('bereich-code').hidden && zustand.codeDatei) {
      event.preventDefault();
      codeSpeichern();
    }
  }
});

window.addEventListener('hashchange', () => {
  if (zustand.angemeldet) {
    bereichZeigen();
  }
});

window.addEventListener('beforeunload', event => {
  if (!zustand.verlassenErlaubt && hatUngespeichertes()) {
    event.preventDefault();
    event.returnValue = '';
  }
});

werkzeugeAnlegen();
symboleAnlegen();
ansichtSetzen(startAnsicht());
sicherungenZeichnen([]);
starten();
