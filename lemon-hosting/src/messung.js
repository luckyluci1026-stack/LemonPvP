/**
 * Was ein Server gerade verbraucht - CPU, Arbeitsspeicher, Netz.
 *
 * In Pterodactyl stehen diese Zahlen ganz oben ueber der Konsole, und
 * das ist die richtige Stelle: Wer wissen will, warum sein Server ruckelt,
 * schaut zuerst dorthin.
 *
 * Zwei Wege, je nach Betriebsart:
 *
 *   Container  `docker stats` fragt einmal fuer alle auf einen Schlag
 *   direkt     /proc/<pid> unter Linux
 *
 * Unter Windows ohne Docker gibt es kein /proc - dort bleiben die Zahlen
 * leer. Lieber gar keine Angabe als eine erfundene.
 *
 * Gemessen wird nicht bei jedem Seitenaufruf, sondern von einem Ticker
 * alle paar Sekunden. `docker stats` braucht spuerbar Zeit, und die
 * Zahlen aendern sich ohnehin langsamer, als jemand hinschauen kann.
 */

import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import * as docker from './docker.js';

/** serverId -> { cpu, ramMB, ramProzent, netEin, netAus, wann } */
const werte = new Map();

/** Wie oft gemessen wird. */
const TAKT = 5000;

/** Wie alt eine Messung sein darf, bevor sie verschwindet. */
const HALTBAR = 20_000;

let ticker = null;

export function messung(serverId) {
  const w = werte.get(serverId);
  if (!w || Date.now() - w.wann > HALTBAR) return null;
  return w;
}

export const vergiss = () => werte.clear();

// ------------------------------------------------------------------ Docker

/** "18.04MiB / 512MiB" -> 18.04 */
function mibAus(text) {
  const treffer = String(text || '').match(/^([\d.]+)\s*([KMGT]?i?B)/i);
  if (!treffer) return 0;
  const zahl = Number(treffer[1]);
  const einheit = treffer[2].toUpperCase();
  if (einheit.startsWith('G')) return zahl * 1024;
  if (einheit.startsWith('K')) return zahl / 1024;
  if (einheit.startsWith('B')) return zahl / 1024 / 1024;
  return zahl;
}

/** "0B / 126B" -> { ein: 0, aus: 126 } in Bytes */
function netzAus(text) {
  const teile = String(text || '').split('/');
  const bytes = (s) => {
    const t = String(s || '').trim().match(/^([\d.]+)\s*([kMGT]?i?B)?/i);
    if (!t) return 0;
    const zahl = Number(t[1]);
    const e = (t[2] || 'B').toUpperCase();
    if (e.startsWith('G')) return zahl * 1024 ** 3;
    if (e.startsWith('M')) return zahl * 1024 ** 2;
    if (e.startsWith('K')) return zahl * 1024;
    return zahl;
  };
  return { ein: bytes(teile[0]), aus: bytes(teile[1]) };
}

/**
 * Alle Container auf einmal abfragen.
 *
 * `docker stats` je Server einzeln aufzurufen dauerte bei zehn Servern
 * laenger als der Messtakt - dann kaeme das Portal nie hinterher. Ein
 * Aufruf liefert alle Zeilen.
 */
function ausDocker() {
  return new Promise((fertig) => {
    let roh = '';
    const lauf = spawn('docker',
      ['stats', '--no-stream', '--format', '{{json .}}'],
      { stdio: ['ignore', 'pipe', 'ignore'] });
    const frist = setTimeout(() => { try { lauf.kill('SIGKILL'); } catch { /* weg */ } }, 12000);
    lauf.stdout.on('data', (s) => { roh += s; });
    lauf.on('error', () => { clearTimeout(frist); fertig(); });
    lauf.on('close', () => {
      clearTimeout(frist);
      for (const zeile of roh.split('\n')) {
        if (!zeile.trim()) continue;
        let d;
        try { d = JSON.parse(zeile); } catch { continue; }
        const treffer = String(d.Name || '').match(/^lemon-server-(\d+)$/);
        if (!treffer) continue;
        const netz = netzAus(d.NetIO);
        werte.set(Number(treffer[1]), {
          cpu: Number(String(d.CPUPerc || '').replace('%', '')) || 0,
          ramMB: Math.round(mibAus(d.MemUsage)),
          ramProzent: Number(String(d.MemPerc || '').replace('%', '')) || 0,
          netEin: netz.ein,
          netAus: netz.aus,
          prozesse: Number(d.PIDs) || 0,
          quelle: 'docker',
          wann: Date.now(),
        });
      }
      fertig();
    });
  });
}

// -------------------------------------------------------------------- /proc

/** Wie viele CPU-Ticks der Prozess zuletzt hatte - fuer die Differenz. */
const vorherTicks = new Map();

/**
 * Ein Prozess samt Kindern unter Linux.
 *
 * Beim direkten Betrieb ist der gestartete Prozess `java` selbst, hat
 * also keine nennenswerten Kinder - deshalb reicht /proc/<pid>. Beim
 * Container waere es der docker-Aufruf, aber dort misst ohnehin Docker.
 */
function ausProc(serverId, pid) {
  try {
    const stat = readFileSync(`/proc/${pid}/stat`, 'utf8');
    // Der Name in Klammern kann Leerzeichen enthalten - deshalb hinter
    // der letzten Klammer weiterzaehlen, nicht stumpf splitten.
    const nachName = stat.slice(stat.lastIndexOf(')') + 2).split(' ');
    const utime = Number(nachName[11]);
    const stime = Number(nachName[12]);
    const ticks = utime + stime;

    const status = readFileSync(`/proc/${pid}/status`, 'utf8');
    const rss = Number((status.match(/VmRSS:\s+(\d+)/) || [])[1] || 0); // kB

    const jetzt = Date.now();
    const vorher = vorherTicks.get(serverId);
    vorherTicks.set(serverId, { ticks, wann: jetzt });

    let cpu = 0;
    if (vorher && jetzt > vorher.wann) {
      // 100 Ticks je Sekunde ist unter Linux der Normalfall.
      const sekunden = (jetzt - vorher.wann) / 1000;
      cpu = ((ticks - vorher.ticks) / 100 / sekunden) * 100;
    }

    werte.set(serverId, {
      cpu: Math.max(0, Math.round(cpu * 10) / 10),
      ramMB: Math.round(rss / 1024),
      ramProzent: 0,
      netEin: 0, netAus: 0, prozesse: 0,
      quelle: 'proc',
      wann: jetzt,
    });
  } catch {
    // Prozess ist weg oder kein /proc (Windows) - dann eben keine Zahlen.
  }
}

// ------------------------------------------------------------------ Ticker

/**
 * Einmal messen.
 *
 * `laufende` ist eine Liste { id, pid, motor } - die kommt aus panel.js,
 * damit dieses Modul nichts ueber Prozessverwaltung wissen muss.
 */
export async function miss(laufende) {
  const container = laufende.filter((l) => l.motor === 'docker');
  const direkt = laufende.filter((l) => l.motor !== 'docker' && l.pid);

  for (const l of direkt) ausProc(l.id, l.pid);
  if (container.length && docker.vorhanden().geht) await ausDocker();

  // Was nicht mehr laeuft, soll auch nicht mehr dastehen.
  const lebt = new Set(laufende.map((l) => l.id));
  for (const id of [...werte.keys()]) if (!lebt.has(id)) werte.delete(id);
  for (const id of [...vorherTicks.keys()]) if (!lebt.has(id)) vorherTicks.delete(id);
}

export function starteTicker(laufendeHolen) {
  if (ticker) return ticker;
  ticker = setInterval(() => {
    miss(laufendeHolen()).catch(() => {});
  }, TAKT);
  ticker.unref();
  return ticker;
}

export function stoppeTicker() {
  if (ticker) clearInterval(ticker);
  ticker = null;
  werte.clear();
  vorherTicks.clear();
}
