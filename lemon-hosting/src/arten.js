/**
 * Serverarten - was Pterodactyl "Eggs" nennt.
 *
 * Bisher musste ein Kunde seine `server.jar` selbst suchen, herunterladen
 * und hochladen. Das ist der laestigste Schritt ueberhaupt und der erste,
 * an dem jemand haengenbleibt. Hier waehlt man stattdessen Art und
 * Version aus, und das Panel holt die Datei selbst.
 *
 * Jede Art weiss zwei Dinge: welche Versionen es gibt und wo die Datei
 * zu einer Version liegt. Beides wird bei den Herstellern erfragt, nicht
 * einprogrammiert - eine fest eingetragene Liste waere nach zwei Monaten
 * veraltet.
 *
 * Wo kein Netz ist, ist auch keine Installation: Ohne Zugriff auf die
 * Hersteller-APIs sagt das Panel das deutlich, statt eine halbe Datei
 * abzulegen. Der Weg ueber "Dateien hochladen" bleibt daneben bestehen.
 */

import { createWriteStream, renameSync, rmSync, existsSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import { ordnerVon } from './panel.js';
import { belegungVergessen } from './dateien.js';

/** Wie lange auf einen Hersteller gewartet wird. */
const FRIST = 20000;

/** Groesste Datei, die wir annehmen - eine Server-Jar ist ~55 MB. */
const MAX = 512 * 1024 * 1024;

async function holeJson(adresse) {
  const antwort = await fetch(adresse, { signal: AbortSignal.timeout(FRIST) });
  if (!antwort.ok) throw new Error(`${adresse} antwortet mit ${antwort.status}`);
  return antwort.json();
}

/**
 * Die Arten.
 *
 * `versionen()` gibt die neuesten zuerst zurueck - niemand sucht 1.8.
 * `datei(version)` liefert die Adresse der fertigen Jar.
 */
export const ARTEN = {
  paper: {
    id: 'paper',
    name: 'Paper',
    beschreibung: 'Der Standard für Plugin-Server. Schnell, verträgt sich mit '
                + 'Spigot- und Bukkit-Plugins.',
    empfohlen: true,
    async versionen() {
      const d = await holeJson('https://api.papermc.io/v2/projects/paper');
      return [...d.versions].reverse();
    },
    async datei(version) {
      const d = await holeJson(
        `https://api.papermc.io/v2/projects/paper/versions/${version}/builds`);
      const gut = d.builds.filter((b) => b.channel === 'default');
      const bau = (gut.length ? gut : d.builds).at(-1);
      if (!bau) throw new Error(`Für ${version} gibt es keinen fertigen Build.`);
      const name = bau.downloads.application.name;
      return `https://api.papermc.io/v2/projects/paper/versions/${version}`
           + `/builds/${bau.build}/downloads/${name}`;
    },
  },

  purpur: {
    id: 'purpur',
    name: 'Purpur',
    beschreibung: 'Paper mit vielen zusätzlichen Einstellungen — für Server, '
                + 'die am Spielgefühl schrauben wollen.',
    async versionen() {
      const d = await holeJson('https://api.purpurmc.org/v2/purpur');
      return [...d.versions].reverse();
    },
    async datei(version) {
      return `https://api.purpurmc.org/v2/purpur/${version}/latest/download`;
    },
  },

  velocity: {
    id: 'velocity',
    name: 'Velocity',
    beschreibung: 'Kein Spielserver, sondern ein Proxy davor — verbindet mehrere '
                + 'Server zu einem Netzwerk.',
    async versionen() {
      const d = await holeJson('https://api.papermc.io/v2/projects/velocity');
      return [...d.versions].reverse();
    },
    async datei(version) {
      const d = await holeJson(
        `https://api.papermc.io/v2/projects/velocity/versions/${version}/builds`);
      const bau = d.builds.at(-1);
      if (!bau) throw new Error(`Für ${version} gibt es keinen fertigen Build.`);
      const name = bau.downloads.application.name;
      return `https://api.papermc.io/v2/projects/velocity/versions/${version}`
           + `/builds/${bau.build}/downloads/${name}`;
    },
  },

  vanilla: {
    id: 'vanilla',
    name: 'Vanilla',
    beschreibung: 'Minecraft wie von Mojang. Keine Plugins — dafür genau das '
                + 'Verhalten des Originals.',
    async versionen() {
      const d = await holeJson(
        'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json');
      return d.versions.filter((v) => v.type === 'release').map((v) => v.id);
    },
    async datei(version) {
      const d = await holeJson(
        'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json');
      const eintrag = d.versions.find((v) => v.id === version);
      if (!eintrag) throw new Error(`Version ${version} kennt Mojang nicht.`);
      const einzeln = await holeJson(eintrag.url);
      const server = einzeln.downloads?.server?.url;
      if (!server) throw new Error(`Für ${version} bietet Mojang keinen Server an.`);
      return server;
    },
  },
};

export const artenListe = () => Object.values(ARTEN);
export const artOk = (id) => (ARTEN[String(id || '')] ? String(id) : '');

/** Eine Version, die auch eine sein kann - der Wert geht in eine URL. */
export const versionOk = (roh) =>
  /^[A-Za-z0-9][A-Za-z0-9._-]{0,31}$/.test(String(roh || '')) ? String(roh) : '';

/**
 * Welche Versionen gibt es?
 *
 * Wird beim Anzeigen der Seite gebraucht und deshalb kurz gemerkt - sonst
 * fragt jeder Seitenaufruf beim Hersteller nach.
 */
const gemerkt = new Map();
const MERKDAUER = 10 * 60 * 1000;

export async function versionen(artId) {
  const art = ARTEN[artId];
  if (!art) return { fehler: 'Diese Serverart gibt es nicht.' };
  const alt = gemerkt.get(artId);
  if (alt && Date.now() - alt.wann < MERKDAUER) return { liste: alt.liste };
  try {
    const liste = await art.versionen();
    gemerkt.set(artId, { liste, wann: Date.now() });
    return { liste };
  } catch (fehler) {
    if (alt) return { liste: alt.liste, veraltet: true };
    return { fehler: `Die Versionsliste von ${art.name} war nicht erreichbar: `
                   + fehler.message };
  }
}

export const vergissVersionen = () => gemerkt.clear();

/**
 * Die Serversoftware herunterladen und ablegen.
 *
 * Erst in eine `.teil`-Datei, am Ende umbenannt: Bricht die Leitung ab,
 * liegt keine halbe Jar herum, die beim Start nur eine unverstaendliche
 * Java-Meldung erzeugt. Und die alte bleibt bis zuletzt heil - wer eine
 * Aktualisierung anstoesst und dabei die Verbindung verliert, steht sonst
 * ohne Server da.
 */
export async function installiere(serverId, artId, version) {
  const art = ARTEN[artOk(artId)];
  if (!art) return { fehler: 'Diese Serverart gibt es nicht.' };
  const v = versionOk(version);
  if (!v) return { fehler: 'Diese Version sieht nicht wie eine Version aus.' };

  let adresse;
  try {
    adresse = await art.datei(v);
  } catch (fehler) {
    return { fehler: `${art.name} ${v} ließ sich nicht nachschlagen: ${fehler.message}` };
  }

  const ordner = ordnerVon(serverId);
  const ziel = join(ordner, 'server.jar');
  const vorlaeufig = ziel + '.teil';

  try {
    const antwort = await fetch(adresse, { signal: AbortSignal.timeout(600000) });
    if (!antwort.ok) {
      return { fehler: `Der Download antwortet mit ${antwort.status}.` };
    }
    const laenge = Number(antwort.headers.get('content-length') || 0);
    if (laenge > MAX) {
      return { fehler: 'Die Datei ist unerwartet groß – da stimmt etwas nicht.' };
    }

    await pipeline(Readable.fromWeb(antwort.body), createWriteStream(vorlaeufig));

    const groesse = statSync(vorlaeufig).size;
    // Eine Server-Jar ist immer mehrere Megabyte. Kommt weniger an, war
    // es eine Fehlerseite - die als server.jar abzulegen waere die
    // unfreundlichste Art zu scheitern.
    if (groesse < 100 * 1024) {
      rmSync(vorlaeufig, { force: true });
      return { fehler: 'Der Download war nur wenige Kilobyte groß – vermutlich '
                     + 'eine Fehlerseite statt der Serversoftware.' };
    }

    renameSync(vorlaeufig, ziel);
    belegungVergessen(serverId);
    return { art: art.name, version: v, groesse };
  } catch (fehler) {
    if (existsSync(vorlaeufig)) rmSync(vorlaeufig, { force: true });
    const grund = fehler.name === 'TimeoutError'
      ? 'Der Hersteller hat zu lange gebraucht.' : fehler.message;
    return { fehler: `Der Download ist fehlgeschlagen: ${grund}` };
  }
}
