/**
 * Zeitplaene: nachts neu starten, morgens sichern.
 *
 * Ein Backup, an das jemand denken muss, ist im Ernstfall nicht da. Und
 * ein Minecraft-Server, der wochenlang durchlaeuft, wird von selbst
 * langsamer. Beides erledigt hier eine Uhr.
 *
 * Absichtlich kein cron und kein Zeitplan-Format mit fuenf Feldern: Wer
 * das Panel benutzt, soll eine Uhrzeit eintippen und fertig. "04:00"
 * versteht jeder, "0 4 * * *" nicht.
 *
 * Die Uhr laeuft im Portal selbst und schaut jede halbe Minute nach. Auf
 * die Minute genau reicht voellig - ein Neustart, der um 04:00:20 statt
 * 04:00:00 kommt, stoert niemanden um vier Uhr morgens.
 */

import * as db from './db.js';
import * as prozess from './panel.js';
import * as sich from './sicherung.js';

/** Was zuletzt gelaufen ist: "art:serverId" -> "2026-08-18T04:00" */
const zuletzt = new Map();

/** Wie oft nachgesehen wird. */
const TAKT = 30_000;

let uhr = null;

/** "04:00" - oder leer, wenn nichts eingestellt werden soll. */
export function zeitOk(roh) {
  const text = String(roh || '').trim();
  if (!text) return '';
  const treffer = text.match(/^(\d{1,2}):(\d{2})$/);
  if (!treffer) return '';
  const stunde = Number(treffer[1]);
  const minute = Number(treffer[2]);
  if (stunde > 23 || minute > 59) return '';
  return `${String(stunde).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

const uhrzeit = (d) =>
  `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;

const marke = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  + `-${String(d.getDate()).padStart(2, '0')}T${uhrzeit(d)}`;

/**
 * Ist dieser Termin gerade faellig - und noch nicht gelaufen?
 *
 * Die Marke enthaelt das Datum, deshalb faellt derselbe Termin nur einmal
 * am Tag an, auch wenn die Uhr innerhalb derselben Minute zweimal
 * nachsieht.
 */
function faellig(art, serverId, eingestellt, jetzt) {
  if (!eingestellt || eingestellt !== uhrzeit(jetzt)) return false;
  const schluessel = `${art}:${serverId}`;
  if (zuletzt.get(schluessel) === marke(jetzt)) return false;
  zuletzt.set(schluessel, marke(jetzt));
  return true;
}

/**
 * Einmal nachsehen, was ansteht.
 *
 * Getrennt von der Uhr, damit der Test die Zeit selbst vorgeben kann,
 * statt bis vier Uhr morgens zu warten.
 */
export async function pruefe(jetzt = new Date(), melde = console.log) {
  const getan = [];

  for (const s of db.alleServer()) {
    // Ein Server in Pterodactyl wird dort verwaltet, ein archivierter
    // oder geloeschter gar nicht mehr.
    if (s.pterodactyl || s.status !== 'aktiv') continue;

    if (faellig('sicherung', s.id, s.sicherung_um, jetzt)) {
      const ergebnis = await sich.anlegen(s.id);
      const text = ergebnis.fehler
        ? 'Automatisches Backup fehlgeschlagen: ' + ergebnis.fehler
        : `Automatisches Backup: ${ergebnis.dateien} Dateien, ${ergebnis.groesse}`;
      db.protokolliere('Zeitplan', ergebnis.fehler ? 'Backup fehlgeschlagen' : 'Backup angelegt',
        `#${s.id} ${s.name} · ${text}`);
      melde(`  [Zeitplan] ${s.name}: ${text}`);
      getan.push({ art: 'sicherung', server: s.id, fehler: ergebnis.fehler || null });
    }

    if (faellig('neustart', s.id, s.neustart_um, jetzt)) {
      // Einen gestoppten Server nicht heimlich hochfahren: Wer ihn
      // abends ausgemacht hat, will ihn morgens nicht laufen sehen.
      if (!prozess.laeuft(s.id)) {
        db.protokolliere('Zeitplan', 'Neustart übersprungen',
          `#${s.id} ${s.name} · lief nicht`);
        getan.push({ art: 'neustart', server: s.id, uebersprungen: true });
      } else {
        const fehler = prozess.neustart(s);
        db.protokolliere('Zeitplan', fehler ? 'Neustart fehlgeschlagen' : 'Neustart',
          `#${s.id} ${s.name}` + (fehler ? ' · ' + fehler : ''));
        melde(`  [Zeitplan] ${s.name}: Neustart${fehler ? ' – ' + fehler : ''}`);
        getan.push({ art: 'neustart', server: s.id, fehler: fehler || null });
      }
    }
  }
  return getan;
}

export function starteUhr() {
  if (uhr) return uhr;
  uhr = setInterval(() => {
    pruefe().catch((fehler) => console.error('Zeitplan:', fehler.message));
  }, TAKT);
  uhr.unref();
  return uhr;
}

export function stoppeUhr() {
  if (uhr) clearInterval(uhr);
  uhr = null;
  zuletzt.clear();
}

/** Fuer die Anzeige: "heute 04:00" oder "morgen 04:00". */
export function naechster(eingestellt, jetzt = new Date()) {
  const zeit = zeitOk(eingestellt);
  if (!zeit) return '';
  return zeit > uhrzeit(jetzt) ? `heute ${zeit}` : `morgen ${zeit}`;
}
