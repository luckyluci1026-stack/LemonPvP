/**
 * Die Weiche: liegt dieser Server hier oder auf einer anderen Maschine?
 *
 * Der Rest des Portals soll das moeglichst wenig merken. Deshalb bietet
 * dieses Modul fuer jede Aktion genau einen Aufruf an, und drinnen wird
 * entschieden - lokal die Module direkt, entfernt ueber `fern.js`.
 *
 * `knoten_id` = 0 heisst "hier". Das ist der Normalfall und bleibt es
 * auch: Wer nur einen Rechner hat, merkt von der ganzen Mechanik nichts.
 *
 * Alle Aufrufe sind async - auch die lokalen. Sonst haette der
 * aufrufende Code zwei Formen zu behandeln, und genau daran gehen solche
 * Weichen kaputt. Die einzige Ausnahme ist `zustand()`: Der wird mitten
 * im HTML gebraucht und kommt deshalb aus dem Zwischenspeicher.
 */

import * as db from './db.js';
import * as prozess from './panel.js';
import * as dat from './dateien.js';
import * as sich from './sicherung.js';
import * as kat from './katalog.js';
import * as arten from './arten.js';
import * as fern from './fern.js';

export const istFern = (server) => Number(server.knoten_id) > 0;

/** Den Knoten zu einem Server holen - oder null, wenn er hier liegt. */
export function knotenVon(server) {
  if (!istFern(server)) return null;
  return db.knoten(server.knoten_id) || null;
}

/**
 * Wenn ein Server auf einen Knoten zeigt, den es nicht mehr gibt.
 *
 * Kommt vor, wenn jemand am Portal herumraeumt. Lieber eine klare
 * Meldung als ein Absturz mitten im Rendern.
 */
const FEHLT = 'Der Knoten zu diesem Server ist nicht eingetragen.';

// ------------------------------------------------------------------ Zustand

export function zustand(server) {
  if (!istFern(server)) return prozess.status(server.id);
  return fern.zustand(server);
}

// ------------------------------------------------------------------ Steuern

export async function aktion(server, was) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return FEHLT;

  if (knoten) return fern.aktion(knoten, server, was);

  if (was === 'eula') { prozess.eulaAnnehmen(server.id); return null; }
  const machen = {
    start: () => prozess.starte(server),
    stopp: () => prozess.stoppe(server.id),
    neustart: () => prozess.neustart(server),
  }[was];
  return machen ? machen() : 'Unbekannte Aktion.';
}

export async function befehl(server, text) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return FEHLT;
  return knoten ? fern.befehl(knoten, server, text) : prozess.befehl(server.id, text);
}

// ------------------------------------------------------------------ Dateien

export async function liste(server, pfad) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return null;
  return knoten ? fern.liste(knoten, server, pfad) : dat.liste(server.id, pfad);
}

export async function lies(server, pfad) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return null;
  return knoten ? fern.lies(knoten, server, pfad) : dat.lies(server.id, pfad);
}

export async function schreib(server, pfad, inhalt) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return FEHLT;
  return knoten ? fern.schreib(knoten, server, pfad, inhalt)
                : dat.schreib(server.id, pfad, inhalt);
}

export async function loesche(server, pfad) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return FEHLT;
  return knoten ? fern.loesche(knoten, server, pfad) : dat.loesche(server.id, pfad);
}

export async function neuerOrdner(server, pfad) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return FEHLT;
  return knoten ? fern.neuerOrdner(knoten, server, pfad)
                : dat.neuerOrdner(server.id, pfad);
}

export async function belegung(server) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return 0;
  return knoten ? fern.belegung(knoten, server) : dat.belegung(server.id);
}

// ------------------------------------------------------------------ Backups

export async function sicherungen(server) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return [];
  return knoten ? fern.sicherungen(knoten, server) : sich.liste(server.id);
}

export async function sicherungAnlegen(server) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return { fehler: FEHLT };
  return knoten ? fern.sicherungAnlegen(knoten, server) : sich.anlegen(server.id);
}

export async function sicherungLoeschen(server, name) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return FEHLT;
  return knoten ? fern.sicherungLoeschen(knoten, server, name)
                : sich.loeschen(server.id, name);
}

export async function sicherungZurueck(server, name) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return { fehler: FEHLT };
  return knoten ? fern.sicherungZurueck(knoten, server, name)
                : sich.zurueckspielen(server.id, name);
}

// ------------------------------------------------------------------ Plugins

export async function plugins(server) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return [];
  if (knoten) return fern.plugins(knoten, server);
  const da = kat.installiert(server.id);
  return kat.verfuegbar().map((p) => ({ ...p, da: da.has(p.stamm) }));
}

export async function plugin(server, datei, art) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return { fehler: FEHLT };
  if (knoten) return fern.plugin(knoten, server, datei, art);
  return art === 'raus' ? kat.entferne(server.id, datei)
                        : kat.installiere(server.id, datei);
}

// ------------------------------------------------------------------ Serverart

export async function versionen(server, artId) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return { fehler: FEHLT };
  return knoten ? fern.versionen(knoten, artId) : arten.versionen(artId);
}

export async function installiereArt(server, artId, version) {
  const knoten = knotenVon(server);
  if (istFern(server) && !knoten) return { fehler: FEHLT };
  return knoten ? fern.installiereArt(knoten, server, artId, version)
                : arten.installiere(server.id, artId, version);
}

/** Wie der Knoten heisst - fuer die Anzeige. */
export function knotenName(server) {
  if (!istFern(server)) return 'dieser Rechner';
  return knotenVon(server)?.name || `Knoten #${server.knoten_id} (fehlt)`;
}
