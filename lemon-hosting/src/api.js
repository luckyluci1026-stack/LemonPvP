/**
 * Die API - fuer Skripte statt fuer Browser.
 *
 * Pterodactyl hat eine Client-API, mit der man Server aus einem Skript
 * heraus startet, stoppt und abfragt. Genau dafuer ist das hier: ein
 * Discord-Bot, der `/serverstatus` beantwortet, ein Skript, das vor der
 * Doppelstunde alle Klassenserver hochfaehrt, ein Kontrollskript, das
 * meldet, wenn einer aus ist.
 *
 * Ein Zugang gehoert immer einem Kunden und kann nur, was dieser Kunde
 * auch koennte. Er ist kein zweiter Weg an der Rechtevergabe vorbei,
 * sondern derselbe Weg mit einem anderen Schluessel.
 *
 * Der Schluessel steht genau einmal da - beim Anlegen. Danach liegt in
 * der Datenbank nur noch sein Hash. Wer ihn verliert, legt einen neuen
 * an; das ist unbequemer, als ihn nachschlagen zu koennen, und genau
 * richtig: Ein Schluessel, den das Portal noch kennt, ist ein
 * Schluessel, den jemand aus dem Portal holen kann.
 */

import { randomBytes, createHash, timingSafeEqual } from 'node:crypto';
import * as db from './db.js';

/** Woran man einen Lemon-Schluessel erkennt. */
const VORSATZ = 'lemon_';

export function neuerSchluessel() {
  return VORSATZ + randomBytes(24).toString('base64url');
}

/**
 * Der Schluessel wird gehasht abgelegt - wie ein Passwort.
 *
 * Anders als bei Passwoertern reicht hier ein einfacher SHA-256 ohne
 * Salz: Der Schluessel ist selbst schon 24 zufaellige Bytes, da bringt
 * ein langsames Verfahren nichts zu erraten, was nicht ohnehin
 * aussichtslos waere.
 */
export const hashe = (schluessel) =>
  createHash('sha256').update(String(schluessel)).digest('hex');

export function pruefe(schluessel) {
  const text = String(schluessel || '');
  if (!text.startsWith(VORSATZ)) return null;
  const hash = hashe(text);
  for (const z of db.alleZugaenge()) {
    const a = Buffer.from(z.hash);
    const b = Buffer.from(hash);
    if (a.length === b.length && timingSafeEqual(a, b)) {
      db.zugangBenutzt(z.id);
      return z;
    }
  }
  return null;
}

/** Was ein Server nach aussen ueber sich sagt. */
export function serverAls(s, zustand, knoten) {
  return {
    id: s.id,
    name: s.name,
    status: s.status,
    laeuft: zustand.status,
    seit: zustand.seit,
    laufzeit: zustand.laufzeit,
    spieler: zustand.spieler || [],
    verbrauch: zustand.verbrauch
      ? { cpu: zustand.verbrauch.cpu, ramMB: zustand.verbrauch.ramMB }
      : null,
    adresse: s.subdomain ? `${s.subdomain}.lemon-servers.de` : null,
    port: s.port,
    paket: s.paket,
    art: s.art || null,
    version: s.mc_version || null,
    knoten: knoten || null,
  };
}
