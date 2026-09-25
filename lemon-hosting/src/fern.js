/**
 * Server auf einer anderen Maschine bedienen.
 *
 * Pterodactyl trennt Panel und Daemon ("Wings"), damit ein Panel viele
 * Rechner steuern kann. Genau das ist hier drin: Auf jeder weiteren
 * Maschine laeuft `daemon.js`, das Portal redet per HTTP mit ihm.
 *
 * Das Modul bildet dieselben Aufrufe nach, die `panel.js`, `dateien.js`
 * und `sicherung.js` lokal anbieten - nur eben ueber die Leitung. Damit
 * muss der Rest des Portals kaum wissen, ob ein Server nebenan oder
 * woanders laeuft.
 *
 * Ein Punkt ist wichtiger als alles andere: Der **Zustand** muss
 * synchron abrufbar sein. Die Seiten rendern `zustand(server)` mitten im
 * HTML, und daraus einen await zu machen haette jede Seite angefasst.
 * Deshalb fragt ein Ticker die Knoten regelmaessig ab und legt die
 * Antwort in einen Zwischenspeicher; die Seiten lesen von dort.
 */

import * as db from './db.js';

/** knoten:server -> { zustand, wann } */
const zwischen = new Map();

/** Wie alt eine Antwort sein darf, bevor sie als unbekannt gilt. */
const HALTBAR = 20_000;

/** Wie oft nachgefragt wird. */
const TAKT = 4000;

let ticker = null;

const schluessel = (knotenId, serverId) => `${knotenId}:${serverId}`;

const UNBEKANNT = { status: 'unbekannt', seit: null, pid: null, laufzeit: 0,
                    spieler: [], motor: null };

/**
 * Eine Anfrage an einen Knoten.
 *
 * Der Zeitpunkt ist wichtig: Ohne Frist bliebe das Portal haengen, wenn
 * eine Maschine nicht antwortet - und mit ihm jede Seite, die gerade auf
 * diesen Knoten wartet.
 */
export async function ruf(knoten, pfad, optionen = {}) {
  const { frist, signal, trotzdem, ...rest } = optionen;

  // Kurze Frist fuer die kleinen Abfragen: Eine Panel-Seite macht
  // mehrere davon, und bei einer ausgefallenen Maschine soll sie nicht
  // minutenlang haengen. Backups und Uploads geben laengere Fristen mit.
  //
  // `frist: 0` heisst ausdruecklich "keine Frist" - die Konsole ist eine
  // Dauerverbindung, die stundenlang offen bleiben soll. Mit `|| 6000`
  // waere sie alle sechs Sekunden abgerissen, und im Browser haette es
  // ausgesehen, als kaeme der ferne Server nicht zur Ruhe.
  const zeit = frist === 0 ? null : AbortSignal.timeout(frist || 6000);
  const zusammen = signal && zeit ? AbortSignal.any([signal, zeit]) : (signal || zeit);

  return fetch(knoten.adresse.replace(/\/+$/, '') + pfad, {
    ...rest,
    ...(zusammen ? { signal: zusammen } : {}),
    headers: {
      'X-Lemon-Zeichen': knoten.geheim,
      ...(optionen.headers || {}),
    },
  });
}

/**
 * Wann ein Knoten zuletzt nicht erreichbar war.
 *
 * Ohne das wartet jede einzelne Anfrage die volle Frist ab, und eine
 * Panel-Seite mit drei Abfragen haengt bei einer ausgefallenen Maschine
 * eine gefuehlte Ewigkeit. Nach einem Fehlschlag wird deshalb ein paar
 * Sekunden lang sofort abgesagt, statt es jedes Mal neu zu versuchen.
 */
const letzterFehlschlag = new Map();
const SCHONFRIST = 5000;

const merkeAus = (knoten) => letzterFehlschlag.set(knoten.id, Date.now());
const geradeAus = (knoten) =>
  Date.now() - (letzterFehlschlag.get(knoten.id) || 0) < SCHONFRIST;

/** Fuer Tests: die Ausfall-Notiz wieder vergessen. */
export const vergissAusfaelle = () => letzterFehlschlag.clear();

/**
 * Eine Anfrage, die niemals wirft.
 *
 * Ein ausgefallener Knoten ist ein normaler Betriebszustand, kein
 * Programmfehler. Wuerde hier eine Ausnahme durchgehen, landete der
 * Kunde auf einer Fehlerseite und wuesste nicht, dass schlicht die
 * andere Maschine aus ist.
 */
async function jsonRuf(knoten, pfad, optionen = {}) {
  if (geradeAus(knoten) && !optionen.trotzdem) {
    return { fehler: `${knoten.name} ist gerade nicht erreichbar.` };
  }
  let antwort;
  try {
    antwort = await ruf(knoten, pfad, optionen);
  } catch (fehler) {
    merkeAus(knoten);
    const grund = fehler.name === 'TimeoutError' || fehler.name === 'AbortError'
      ? 'antwortet nicht' : fehler.message;
    return { fehler: `${knoten.name} ist nicht erreichbar: ${grund}` };
  }

  const text = await antwort.text().catch(() => '');
  let daten = null;
  try { daten = JSON.parse(text); } catch { /* kein JSON */ }
  if (!antwort.ok) {
    if (antwort.status === 401) {
      return { fehler: `Das Zeichen für ${knoten.name} stimmt nicht.` };
    }
    return { fehler: daten?.fehler || `${knoten.name} antwortet mit ${antwort.status}` };
  }
  letzterFehlschlag.delete(knoten.id);
  return daten ?? {};
}

// ------------------------------------------------------------------ Zustand

/**
 * Der zuletzt bekannte Zustand - ohne zu warten.
 *
 * Ist die Antwort zu alt oder war noch keine da, kommt "unbekannt"
 * zurueck. Das ist ehrlicher als "gestoppt": Wir wissen es schlicht
 * nicht, und das Panel sagt es auch so.
 */
export function zustand(server) {
  const eintrag = zwischen.get(schluessel(server.knoten_id, server.id));
  if (!eintrag || Date.now() - eintrag.wann > HALTBAR) return { ...UNBEKANNT };
  return eintrag.zustand;
}

/** Einen Knoten abfragen und den Zwischenspeicher auffrischen. */
export async function frischAuf(knoten, serverIds) {
  if (!serverIds.length) return;
  try {
    const daten = await jsonRuf(knoten, '/zustaende?ids=' + serverIds.join(','),
      { frist: 8000, trotzdem: true });
    if (daten.fehler) throw new Error(daten.fehler);
    for (const id of serverIds) {
      zwischen.set(schluessel(knoten.id, id), {
        zustand: daten[id] || { ...UNBEKANNT },
        wann: Date.now(),
      });
    }
  } catch {
    // Nicht erreichbar: die alten Werte laufen von selbst ab und werden
    // dann zu "unbekannt". Ein sofortiges Leeren waere unruhiger.
  }
}

/** Alle Knoten der Reihe nach abfragen. */
export async function frischeAlle() {
  const knoten = db.alleKnoten();
  if (!knoten.length) return;
  const server = db.alleServer();
  await Promise.all(knoten.map((k) => frischAuf(k,
    server.filter((s) => s.knoten_id === k.id && !s.pterodactyl).map((s) => s.id))));
}

export function starteTicker() {
  if (ticker) return ticker;
  ticker = setInterval(() => { frischeAlle().catch(() => {}); }, TAKT);
  ticker.unref();
  return ticker;
}

export function stoppeTicker() {
  if (ticker) clearInterval(ticker);
  ticker = null;
  zwischen.clear();
}

// ------------------------------------------------------------------ Steuern

/**
 * Start, Stopp, Neustart, EULA.
 *
 * Der Serverdatensatz geht mit: Der Daemon fuehrt keine eigene
 * Datenbank. Er weiss nur, was Ordner und Prozesse sind - wieviel
 * Speicher jemand gebucht hat, sagt ihm das Portal bei jedem Aufruf.
 * Damit gibt es genau eine Stelle, an der Pakete und Preise stehen.
 */
export async function aktion(knoten, server, was) {
  const daten = await jsonRuf(knoten, `/aktion/${server.id}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ was, server }),
  });
  return daten.fehler || null;
}

export async function befehl(knoten, server, text) {
  const daten = await jsonRuf(knoten, `/befehl/${server.id}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ befehl: text }),
  });
  return daten.fehler || null;
}

// ------------------------------------------------------------------ Dateien

export async function liste(knoten, server, pfad) {
  const daten = await jsonRuf(knoten,
    `/dateien/${server.id}?p=${encodeURIComponent(pfad)}`);
  return daten.fehler ? null : daten.eintraege;
}

export async function lies(knoten, server, pfad) {
  const daten = await jsonRuf(knoten,
    `/lies/${server.id}?p=${encodeURIComponent(pfad)}`);
  return daten.fehler ? null : daten.inhalt;
}

const schreibRuf = (knoten, server, pfad, koerper) => jsonRuf(knoten,
  `/${pfad}/${server.id}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(koerper),
  });

export async function schreib(knoten, server, pfad, inhalt) {
  return (await schreibRuf(knoten, server, 'schreib', { p: pfad, inhalt })).fehler || null;
}
export async function loesche(knoten, server, pfad) {
  return (await schreibRuf(knoten, server, 'loesche', { p: pfad })).fehler || null;
}
export async function neuerOrdner(knoten, server, pfad) {
  return (await schreibRuf(knoten, server, 'ordner', { p: pfad })).fehler || null;
}

export async function belegung(knoten, server) {
  const daten = await jsonRuf(knoten, `/belegung/${server.id}`);
  return daten.fehler ? 0 : (daten.bytes || 0);
}

// ------------------------------------------------------------------ Backups

export async function sicherungen(knoten, server) {
  const daten = await jsonRuf(knoten, `/sicherungen/${server.id}`);
  return daten.fehler ? [] : (daten.liste || []);
}

export async function sicherungAnlegen(knoten, server) {
  return jsonRuf(knoten, `/sicherung/${server.id}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ server }),
    frist: 300000,   // ein grosses Backup dauert
  });
}

export async function sicherungLoeschen(knoten, server, name) {
  return (await schreibRuf(knoten, server, 'sicherung-weg', { f: name })).fehler || null;
}

export async function sicherungZurueck(knoten, server, name) {
  return jsonRuf(knoten, `/sicherung-zurueck/${server.id}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ f: name }),
    frist: 300000,
  });
}

// ------------------------------------------------------------------ Plugins

export async function plugins(knoten, server) {
  const daten = await jsonRuf(knoten, `/plugins/${server.id}`);
  return daten.fehler ? [] : (daten.liste || []);
}

export async function plugin(knoten, server, datei, art) {
  return jsonRuf(knoten, `/plugin/${server.id}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ datei, art }),
  });
}

// ---------------------------------------------------------------- Serverart

export async function versionen(knoten, artId) {
  return jsonRuf(knoten, `/versionen/0?art=${encodeURIComponent(artId)}`,
    { frist: 25000 });
}

export async function installiereArt(knoten, server, artId, version) {
  return jsonRuf(knoten, `/installiere/${server.id}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ art: artId, version }),
    frist: 600000,
  });
}

// ------------------------------------------------------------------ Prueben

/** Antwortet der Knoten - und stimmt das Zeichen? */
export async function pruefe(knoten) {
  try {
    const antwort = await ruf(knoten, '/hallo', { frist: 6000 });
    if (antwort.status === 401) return { geht: false, grund: 'Das Zeichen stimmt nicht.' };
    if (!antwort.ok) return { geht: false, grund: `Antwortet mit ${antwort.status}` };
    const daten = await antwort.json();
    return { geht: true, ...daten };
  } catch (fehler) {
    return { geht: false, grund: fehler.message };
  }
}
