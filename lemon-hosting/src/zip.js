/**
 * Ein kleiner ZIP-Packer und -Entpacker.
 *
 * Warum selbst geschrieben: Das Portal kommt ohne ein einziges npm-Paket
 * aus, und das soll so bleiben - eine Abhaengigkeit, die in zwei Jahren
 * ein Sicherheitsupdate braucht, ist fuer ein Schulprojekt ein schlechter
 * Tausch. Node bringt zlib mit; was fehlt, ist nur der Behaelter drumherum,
 * und der ist ueberschaubar.
 *
 * ZIP statt tar.gz, weil Windows ZIP von sich aus oeffnet. Wer sein
 * Backup herunterlaedt, soll doppelklicken koennen.
 *
 * Was hier bewusst NICHT drin ist: Zip64. Damit sind 65535 Dateien und
 * 4 GB die Grenze. Beides wird beim Packen geprueft und sagt sonst
 * deutlich Bescheid - lieber eine klare Absage als ein Archiv, das sich
 * hinterher nicht oeffnen laesst.
 */

import { deflateRawSync, inflateRawSync } from 'node:zlib';
import { openSync, readSync, closeSync, writeSync, statSync, readdirSync,
         mkdirSync, unlinkSync } from 'node:fs';
import { join, dirname, relative, sep } from 'node:path';

export const MAX_EINTRAEGE = 65535;
export const MAX_GROESSE = 4 * 1024 * 1024 * 1024 - 1;

// ------------------------------------------------------------------ CRC32

const TABELLE = (() => {
  const t = new Int32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[i] = c;
  }
  return t;
})();

function crc32(puffer) {
  let c = -1;
  for (let i = 0; i < puffer.length; i++) c = TABELLE[(c ^ puffer[i]) & 0xff] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
}

// -------------------------------------------------------------- DOS-Zeit

/** ZIP speichert die Uhrzeit im Format von MS-DOS. Sekunden nur gerade. */
function dosZeit(datum) {
  const jahr = Math.max(1980, datum.getFullYear());
  return {
    zeit: (datum.getHours() << 11) | (datum.getMinutes() << 5)
        | (Math.floor(datum.getSeconds() / 2)),
    datum: ((jahr - 1980) << 9) | ((datum.getMonth() + 1) << 5) | datum.getDate(),
  };
}

// ---------------------------------------------------------------- Packen

/** Alle Dateien unter `wurzel` einsammeln, ausser was `ausser` aussortiert. */
export function sammle(wurzel, ausser = () => false, unter = wurzel, raus = []) {
  let eintraege;
  try {
    eintraege = readdirSync(unter, { withFileTypes: true });
  } catch {
    return raus;
  }
  for (const e of eintraege) {
    const voll = join(unter, e.name);
    const rel = relative(wurzel, voll).split(sep).join('/');
    if (ausser(rel)) continue;
    // Symlinks nicht verfolgen - sonst packt ein Link auf "/" das halbe
    // Dateisystem mit ein.
    if (e.isSymbolicLink()) continue;
    if (e.isDirectory()) sammle(wurzel, ausser, voll, raus);
    else if (e.isFile()) raus.push(rel);
  }
  return raus;
}

/**
 * Packt Dateien in ein ZIP.
 *
 * Jede Datei wird einzeln gelesen und komprimiert - der Arbeitsspeicher
 * muss also nie mehr fassen als die groesste Einzeldatei. Bei einer
 * Minecraft-Welt sind das ein paar Megabyte pro Region.
 *
 * @returns null wenn alles gut ging, sonst ein Text fuer die Oberflaeche
 */
export function packe(wurzel, dateien, zielPfad) {
  if (dateien.length > MAX_EINTRAEGE) {
    return `Das sind ${dateien.length} Dateien – mehr als ${MAX_EINTRAEGE} passen `
         + 'nicht in ein einfaches ZIP.';
  }

  const ziel = openSync(zielPfad, 'w');
  const verzeichnis = [];
  let versatz = 0;

  const schreibe = (puffer) => {
    writeSync(ziel, puffer);
    versatz += puffer.length;
  };

  try {
    for (const name of dateien) {
      const voll = join(wurzel, name);
      let roh;
      try {
        roh = readAll(voll);
      } catch {
        continue; // verschwunden waehrend wir packen
      }
      const gepackt = deflateRawSync(roh, { level: 6 });
      const pruefsumme = crc32(roh);
      const { zeit, datum } = dosZeit(statSync(voll).mtime);
      const nameRoh = Buffer.from(name, 'utf8');
      const anfang = versatz;

      const kopf = Buffer.alloc(30);
      kopf.writeUInt32LE(0x04034b50, 0);
      kopf.writeUInt16LE(20, 4);            // benoetigte Version
      kopf.writeUInt16LE(0x0800, 6);        // Bit 11: Name ist UTF-8
      kopf.writeUInt16LE(8, 8);             // Verfahren: deflate
      kopf.writeUInt16LE(zeit, 10);
      kopf.writeUInt16LE(datum, 12);
      kopf.writeUInt32LE(pruefsumme, 14);
      kopf.writeUInt32LE(gepackt.length, 18);
      kopf.writeUInt32LE(roh.length, 22);
      kopf.writeUInt16LE(nameRoh.length, 26);
      schreibe(kopf);
      schreibe(nameRoh);
      schreibe(gepackt);

      verzeichnis.push({ name: nameRoh, pruefsumme, gepackt: gepackt.length,
                         roh: roh.length, zeit, datum, anfang });

      if (versatz > MAX_GROESSE) {
        closeSync(ziel);
        try { unlinkSync(zielPfad); } catch { /* egal */ }
        return 'Der Server ist größer als 4 GB – so viel passt nicht in ein '
             + 'einfaches ZIP. Lösch alte Welten oder hol die Dateien direkt '
             + 'vom Rechner.';
      }
    }

    const verzeichnisAnfang = versatz;
    for (const e of verzeichnis) {
      const z = Buffer.alloc(46);
      z.writeUInt32LE(0x02014b50, 0);
      z.writeUInt16LE(20, 4);               // erstellt von
      z.writeUInt16LE(20, 6);               // benoetigte Version
      z.writeUInt16LE(0x0800, 8);
      z.writeUInt16LE(8, 10);
      z.writeUInt16LE(e.zeit, 12);
      z.writeUInt16LE(e.datum, 14);
      z.writeUInt32LE(e.pruefsumme, 16);
      z.writeUInt32LE(e.gepackt, 20);
      z.writeUInt32LE(e.roh, 24);
      z.writeUInt16LE(e.name.length, 28);
      z.writeUInt32LE(e.anfang, 42);
      schreibe(z);
      schreibe(e.name);
    }

    const ende = Buffer.alloc(22);
    ende.writeUInt32LE(0x06054b50, 0);
    ende.writeUInt16LE(verzeichnis.length, 8);
    ende.writeUInt16LE(verzeichnis.length, 10);
    ende.writeUInt32LE(versatz - verzeichnisAnfang, 12);
    ende.writeUInt32LE(verzeichnisAnfang, 16);
    schreibe(ende);
    return null;
  } catch (fehler) {
    return 'Das Backup ließ sich nicht schreiben: ' + fehler.message;
  } finally {
    try { closeSync(ziel); } catch { /* schon zu */ }
  }
}

function readAll(pfad) {
  const fd = openSync(pfad, 'r');
  try {
    const groesse = statSync(pfad).size;
    const puffer = Buffer.alloc(groesse);
    let gelesen = 0;
    while (gelesen < groesse) {
      const n = readSync(fd, puffer, gelesen, groesse - gelesen, gelesen);
      if (n <= 0) break;
      gelesen += n;
    }
    return puffer.subarray(0, gelesen);
  } finally {
    closeSync(fd);
  }
}

// -------------------------------------------------------------- Entpacken

function lies(fd, laenge, von) {
  const puffer = Buffer.alloc(laenge);
  readSync(fd, puffer, 0, laenge, von);
  return puffer;
}

/** Liest das zentrale Verzeichnis - die Inhaltsangabe am Ende der Datei. */
export function inhalt(zipPfad) {
  const fd = openSync(zipPfad, 'r');
  try {
    const groesse = statSync(zipPfad).size;
    // Das Schlussstueck steht ganz hinten, kann aber einen Kommentar
    // hinter sich haben - deshalb rueckwaerts suchen.
    const schwanz = lies(fd, Math.min(groesse, 66000), Math.max(0, groesse - 66000));
    let ende = -1;
    for (let i = schwanz.length - 22; i >= 0; i--) {
      if (schwanz.readUInt32LE(i) === 0x06054b50) { ende = i; break; }
    }
    if (ende < 0) return null;

    const anzahl = schwanz.readUInt16LE(ende + 10);
    let stelle = schwanz.readUInt32LE(ende + 16);
    const eintraege = [];
    for (let i = 0; i < anzahl; i++) {
      const kopf = lies(fd, 46, stelle);
      if (kopf.readUInt32LE(0) !== 0x02014b50) break;
      const nameLaenge = kopf.readUInt16LE(28);
      const extraLaenge = kopf.readUInt16LE(30);
      const kommentar = kopf.readUInt16LE(32);
      eintraege.push({
        name: lies(fd, nameLaenge, stelle + 46).toString('utf8'),
        verfahren: kopf.readUInt16LE(10),
        gepackt: kopf.readUInt32LE(20),
        roh: kopf.readUInt32LE(24),
        anfang: kopf.readUInt32LE(42),
      });
      stelle += 46 + nameLaenge + extraLaenge + kommentar;
    }
    return eintraege;
  } catch {
    return null;
  } finally {
    closeSync(fd);
  }
}

/**
 * Packt ein ZIP aus.
 *
 * `zielFuer` bekommt den Namen aus dem Archiv und gibt den Pfad zurueck,
 * an den geschrieben werden darf - oder null. Genau dort haengt die
 * Sicherheit: Ein Archiv kann `../../etc/passwd` als Namen enthalten
 * ("Zip Slip"), und ein Entpacker, der das glaubt, schreibt ausserhalb.
 * Deshalb entscheidet nicht diese Funktion, wohin etwas darf, sondern
 * der Aufrufer mit seinem Einsperr-Test.
 *
 * @returns { entpackt, uebersprungen } oder ein String bei Fehlern
 */
export function entpacke(zipPfad, zielFuer) {
  const eintraege = inhalt(zipPfad);
  if (!eintraege) return 'Das ist keine lesbare ZIP-Datei.';

  const fd = openSync(zipPfad, 'r');
  let entpackt = 0;
  let uebersprungen = 0;
  try {
    for (const e of eintraege) {
      if (e.name.endsWith('/')) continue;
      const ziel = zielFuer(e.name);
      if (!ziel) { uebersprungen++; continue; }

      // Der lokale Kopf sagt, wie lang Name und Zusatzfeld hier sind -
      // die koennen von denen im Verzeichnis abweichen.
      const kopf = lies(fd, 30, e.anfang);
      if (kopf.readUInt32LE(0) !== 0x04034b50) { uebersprungen++; continue; }
      const datenAb = e.anfang + 30 + kopf.readUInt16LE(26) + kopf.readUInt16LE(28);
      const gepackt = lies(fd, e.gepackt, datenAb);

      let roh;
      try {
        roh = e.verfahren === 0 ? gepackt : inflateRawSync(gepackt);
      } catch {
        uebersprungen++;
        continue;
      }

      mkdirSync(dirname(ziel), { recursive: true });
      const aus = openSync(ziel, 'w');
      try { writeSync(aus, roh); } finally { closeSync(aus); }
      entpackt++;
    }
    return { entpackt, uebersprungen };
  } catch (fehler) {
    return 'Das Archiv ließ sich nicht auspacken: ' + fehler.message;
  } finally {
    closeSync(fd);
  }
}
