/**
 * Der Plugin-Katalog.
 *
 * Ein Kunde soll seine Plugins nicht suchen, herunterladen und hochladen
 * muessen. Was im Katalogordner liegt, steht im Panel zur Auswahl - ein
 * Klick, und die Jar liegt in `plugins/`.
 *
 * Wo der Katalog liegt, sagt KATALOG_DIR. In diesem Repo bietet sich der
 * dist-Ordner an, in dem die fertigen Plugins schon stehen:
 *
 *   KATALOG_DIR=../dist node start.js
 *
 * Sicherheitshalber wird nie ein Name aus dem Formular auf die Platte
 * losgelassen. Eingehende Namen werden gegen die Liste des Katalogs
 * geprueft, und nur was dort wirklich steht, wird kopiert. Ein
 * `../../etc/passwd` findet in dieser Liste keine Entsprechung und faellt
 * damit heraus, bevor irgendein Pfad daraus gebaut wird.
 */

import { readdirSync, existsSync, copyFileSync, statSync, readFileSync,
         mkdirSync, rmSync } from 'node:fs';
import { join, resolve, basename } from 'node:path';
import { ordnerVon } from './panel.js';
import { belegungVergessen, lesbareGroesse } from './dateien.js';

/** Enthaelt der Ordner ueberhaupt Jars? */
function hatJars(ordner) {
  try {
    return readdirSync(ordner).some((n) => n.toLowerCase().endsWith('.jar'));
  } catch {
    return false;
  }
}

/**
 * Welcher Ordner ist der Katalog?
 *
 * KATALOG_DIR gewinnt immer. Sonst der eigene `katalog`-Ordner, sobald
 * Jars darin liegen - und wenn nicht, das `dist` des Repos, wo die
 * fertigen Plugins ohnehin stehen. Damit ist der Katalog beim ersten
 * Start nicht leer, ohne dass jemand etwas einstellen muss.
 *
 * Welcher es geworden ist, sagt das Portal beim Start. Raten muss also
 * niemand.
 */
export function katalogOrdner() {
  if (process.env.KATALOG_DIR) return resolve(process.env.KATALOG_DIR);
  const eigen = resolve('katalog');
  if (hatJars(eigen)) return eigen;
  const dist = resolve('..', 'dist');
  if (hatJars(dist)) return dist;
  return eigen;
}

/**
 * Beschreibungen aus katalog.json, falls vorhanden.
 *
 * Ohne die Datei funktioniert alles genauso - dann steht eben nur der
 * Dateiname da. Wer eine Jar in den Ordner legt, soll sie sofort
 * auswaehlen koennen und nicht erst irgendwo eintragen muessen.
 */
function beschreibungen() {
  // Erst die mitgelieferten Texte, dann die aus dem Katalogordner
  // daruebergelegt. So stehen die Beschreibungen auch dann da, wenn die
  // Jars woanders liegen als die JSON-Datei.
  const raus = {};
  for (const datei of [resolve('katalog', 'katalog.json'),
                       join(katalogOrdner(), 'katalog.json')]) {
    if (!existsSync(datei)) continue;
    try {
      Object.assign(raus, JSON.parse(readFileSync(datei, 'utf8')));
    } catch { /* kaputte JSON ignorieren, lieber ohne Text als gar nicht */ }
  }
  return raus;
}

/** "BetterSMP-1.0.0.jar" -> "BetterSMP" */
const stammVon = (datei) => basename(datei, '.jar').replace(/-\d[\d.]*$/, '');

export function verfuegbar() {
  const ordner = katalogOrdner();
  if (!existsSync(ordner)) return [];
  const texte = beschreibungen();
  const raus = [];
  for (const name of readdirSync(ordner)) {
    if (!name.toLowerCase().endsWith('.jar')) continue;
    try {
      const s = statSync(join(ordner, name));
      const stamm = stammVon(name);
      raus.push({
        datei: name,
        stamm,
        name: texte[stamm]?.name || stamm,
        beschreibung: texte[stamm]?.beschreibung || '',
        groesse: lesbareGroesse(s.size),
      });
    } catch { /* verschwunden */ }
  }
  return raus.sort((a, b) => a.name.localeCompare(b.name));
}

export function pluginOrdner(serverId) {
  const pfad = join(ordnerVon(serverId), 'plugins');
  mkdirSync(pfad, { recursive: true });
  return pfad;
}

/**
 * Welche Jars liegen beim Server - nach Stamm, damit Versionen passen.
 *
 * Liest nur, legt nichts an: Die Funktion laeuft bei jedem Aufruf des
 * Panels, und das blosse Ansehen einer Seite soll keinen Ordner
 * erzeugen. Angelegt wird erst beim Installieren.
 */
export function installiert(serverId) {
  const ordner = join(ordnerVon(serverId), 'plugins');
  const raus = new Map();
  try {
    for (const name of readdirSync(ordner)) {
      if (name.toLowerCase().endsWith('.jar')) raus.set(stammVon(name), name);
    }
  } catch { /* Ordner gibt es noch nicht - dann eben nichts installiert */ }
  return raus;
}

/**
 * Ein Plugin aus dem Katalog installieren.
 *
 * `datei` muss genau so im Katalog stehen - sonst passiert nichts.
 */
export function installiere(serverId, datei) {
  const eintrag = verfuegbar().find((p) => p.datei === String(datei));
  if (!eintrag) return { fehler: 'Dieses Plugin steht nicht im Katalog.' };

  // Eine aeltere Version desselben Plugins muss weg, sonst laedt Bukkit
  // beide und beschwert sich ueber doppelte Befehle.
  const alt = installiert(serverId).get(eintrag.stamm);
  try {
    if (alt && alt !== eintrag.datei) {
      rmSync(join(pluginOrdner(serverId), alt), { force: true });
    }
    copyFileSync(join(katalogOrdner(), eintrag.datei),
                 join(pluginOrdner(serverId), eintrag.datei));
    belegungVergessen(serverId);
    return { name: eintrag.name, ersetzt: alt && alt !== eintrag.datei ? alt : null };
  } catch (fehler) {
    return { fehler: 'Konnte nicht installiert werden: ' + fehler.message };
  }
}

export function entferne(serverId, datei) {
  const eintrag = verfuegbar().find((p) => p.datei === String(datei));
  if (!eintrag) return { fehler: 'Dieses Plugin steht nicht im Katalog.' };
  const da = installiert(serverId).get(eintrag.stamm);
  if (!da) return { fehler: 'Dieses Plugin ist gar nicht installiert.' };
  try {
    rmSync(join(pluginOrdner(serverId), da), { force: true });
    belegungVergessen(serverId);
    return { name: eintrag.name };
  } catch (fehler) {
    return { fehler: 'Konnte nicht entfernt werden: ' + fehler.message };
  }
}
