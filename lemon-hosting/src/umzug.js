/**
 * Einen Server auf eine andere Maschine umziehen.
 *
 * Bisher ging das nur von Hand: stoppen, Ordner kopieren, im Portal den
 * Knoten umstellen, Port pruefen. Vier Schritte, von denen man den
 * dritten vergisst - und dann zeigt das Panel auf eine Maschine, auf der
 * nichts liegt.
 *
 * Der Ablauf hier ist immer derselbe, egal in welche Richtung:
 *
 *   1. Der Server muss aus sein. Dateien unter einem laufenden Minecraft
 *      wegzukopieren endet zuverlaessig in einer kaputten Welt.
 *   2. Alles einpacken - beim fernen Knoten ueber ein Backup, das dort
 *      liegenbleibt. Das ist die Sicherheitskopie, falls unterwegs etwas
 *      schiefgeht.
 *   3. Auf der Zielmaschine auspacken.
 *   4. Erst danach Knoten und Port in der Datenbank aendern.
 *
 * Die Reihenfolge ist der Punkt: Die Datenbank wird zuletzt angefasst.
 * Bricht der Umzug vorher ab, zeigt der Server weiter auf die alte
 * Maschine, wo alles noch liegt - und niemand steht vor einem Eintrag,
 * der ins Leere zeigt.
 *
 * Die alten Dateien bleiben stehen. Sie von selbst zu loeschen waere die
 * eine Sache, die sich nicht zurueckholen laesst; das macht der Admin
 * lieber selbst, wenn er gesehen hat, dass drueben alles laeuft.
 */

import { createWriteStream, createReadStream, mkdtempSync, rmSync,
         statSync, existsSync } from 'node:fs';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import * as db from './db.js';
import * as prozess from './panel.js';
import * as dat from './dateien.js';
import * as fern from './fern.js';
import * as wo from './wo.js';
import { packe, entpacke, sammle } from './zip.js';

/**
 * Den Server einpacken - und sagen, wo das Paket liegt.
 *
 * Liegt er hier, wird direkt gepackt. Liegt er woanders, laesst das
 * Portal den Daemon ein Backup anlegen und holt es. Das Backup bleibt
 * drueben liegen: Wenn beim Umzug etwas schiefgeht, ist es genau das,
 * was man dann braucht.
 */
async function packeEin(server, arbeitsordner) {
  const ziel = join(arbeitsordner, `umzug-${server.id}.zip`);

  if (!wo.istFern(server)) {
    const quelle = prozess.ordnerVon(server.id);
    const dateien = sammle(quelle);
    if (!dateien.length) return { fehler: 'Im Serverordner liegt nichts.' };
    const fehler = packe(quelle, dateien, ziel);
    return fehler ? { fehler } : { pfad: ziel, dateien: dateien.length };
  }

  const knoten = wo.knotenVon(server);
  if (!knoten) return { fehler: 'Der Knoten dieses Servers ist nicht eingetragen.' };

  const gepackt = await fern.sicherungAnlegen(knoten, server);
  if (gepackt.fehler) return { fehler: 'Auf der alten Maschine: ' + gepackt.fehler };

  try {
    const antwort = await fern.ruf(knoten,
      `/sicherung/${server.id}?f=${encodeURIComponent(gepackt.name)}`, { frist: 0 });
    if (!antwort.ok) {
      return { fehler: `Das Paket ließ sich nicht holen (${antwort.status}).` };
    }
    await pipeline(Readable.fromWeb(antwort.body), createWriteStream(ziel));
    return { pfad: ziel, dateien: gepackt.dateien, sicherung: gepackt.name };
  } catch (fehler) {
    return { fehler: 'Beim Holen des Pakets: ' + fehler.message };
  }
}

/** Das Paket auf der Zielmaschine auspacken. */
async function packeAus(serverId, zielKnotenId, zipPfad) {
  if (!zielKnotenId) {
    const ergebnis = entpacke(zipPfad, (eintrag) => dat.innerhalb(serverId, eintrag));
    if (typeof ergebnis === 'string') return { fehler: ergebnis };
    dat.belegungVergessen(serverId);
    return ergebnis;
  }

  const knoten = db.knoten(zielKnotenId);
  if (!knoten) return { fehler: 'Den Zielknoten gibt es nicht.' };
  try {
    const antwort = await fern.ruf(knoten, `/umzug/${serverId}`, {
      method: 'POST',
      body: createReadStream(zipPfad),
      duplex: 'half',
      frist: 0,
      headers: { 'Content-Type': 'application/zip' },
    });
    const daten = await antwort.json().catch(() => ({}));
    if (!antwort.ok) {
      return { fehler: daten.fehler || `Die neue Maschine antwortet mit ${antwort.status}.` };
    }
    return daten;
  } catch (fehler) {
    return { fehler: 'Beim Übertragen: ' + fehler.message };
  }
}

/**
 * Der Umzug.
 *
 * @returns { fehler } oder { entpackt, port, sicherung, groesse }
 */
export async function umziehen(server, zielKnotenId) {
  const von = Number(server.knoten_id) || 0;
  const nach = Number(zielKnotenId) || 0;
  if (von === nach) return { fehler: 'Der Server liegt schon auf dieser Maschine.' };
  if (server.pterodactyl) {
    return { fehler: 'Dieser Server wird von Pterodactyl verwaltet – dort umziehen.' };
  }
  if (nach && !db.knoten(nach)) return { fehler: 'Den Zielknoten gibt es nicht.' };

  // Bei einer fernen Maschine nicht dem Zwischenspeicher glauben: Der
  // ist bis zu vier Sekunden alt, und "lief eben noch nicht" ist beim
  // Wegkopieren von Weltdateien die falsche Auskunft. Also nachfragen.
  const alterKnoten = wo.knotenVon(server);
  if (alterKnoten) await fern.frischAuf(alterKnoten, [server.id]);

  const zustand = wo.zustand(server);
  if (zustand.status !== 'gestoppt') {
    return { fehler: zustand.status === 'unbekannt'
      ? 'Die alte Maschine meldet sich nicht – solange weiß niemand, ob der '
        + 'Server dort noch läuft.'
      : 'Stopp den Server, bevor er umzieht.' };
  }

  const arbeit = mkdtempSync(join(tmpdir(), 'lemon-umzug-'));
  try {
    const paket = await packeEin(server, arbeit);
    if (paket.fehler) return paket;
    const groesse = statSync(paket.pfad).size;

    const ausgepackt = await packeAus(server.id, nach, paket.pfad);
    if (ausgepackt.fehler) return ausgepackt;

    // Erst jetzt die Datenbank: Bis hierher zeigt der Server noch auf die
    // alte Maschine, wo alles unveraendert liegt.
    const belegt = db.belegtePorts(nach);
    const port = belegt.has(server.port) ? db.naechsterPort(nach) : server.port;

    /**
     * Den Hauptport darf das Portal umstellen - er geht als `--port` an
     * den Server und als `-p` an Docker, beides kommt aus der Datenbank.
     *
     * Die weiteren Ports nicht: Auf welchem Port Geyser oder Dynmap
     * horcht, steht in deren eigener Konfigurationsdatei im
     * Serverordner. Eine stille Umnummerierung hier hiesse, dass das
     * Panel 19133 durchreicht und das Plugin weiter auf 19132 wartet -
     * und niemand faende, warum plotzlich keiner mehr hereinkommt.
     * Also nur melden, aendern muss der Admin.
     */
    const streit = db.portsVon(server.id)
      .filter((p) => belegt.has(p.port))
      .map((p) => p.port);

    db.serverAendern(server.id, { knoten_id: nach, port });

    return {
      entpackt: ausgepackt.entpackt ?? paket.dateien,
      uebersprungen: ausgepackt.uebersprungen || 0,
      port,
      portGeaendert: port !== server.port,
      streit,
      sicherung: paket.sicherung || null,
      groesse,
    };
  } finally {
    rmSync(arbeit, { recursive: true, force: true });
  }
}

/** Wo die alten Dateien liegenbleiben - fuer den Hinweis danach. */
export function alteStelle(server) {
  if (!Number(server.knoten_id)) {
    const ordner = join(prozess.wurzel(), String(server.id));
    return existsSync(ordner) ? ordner : 'auf diesem Rechner';
  }
  return `auf ${db.knoten(server.knoten_id)?.name || 'der alten Maschine'}`;
}
