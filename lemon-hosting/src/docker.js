/**
 * Server in Containern laufen lassen - wie Pterodactyl.
 *
 * Ohne Container ist "4,25 GB RAM" eine Zahl auf der Rechnung und sonst
 * nichts: Ein Server mit einem Speicherleck zieht die ganze Maschine
 * runter, und alle anderen Klassen sitzen im Lag. Mit `--memory` ist die
 * Grenze echt - wer sie reisst, trifft nur sich selbst.
 *
 * Genauso beim Rest: `--cpus` teilt die Rechenzeit, `--pids-limit`
 * verhindert Fork-Bomben, `--user` sorgt dafuer, dass im Container
 * niemand root ist, und `-v` gibt dem Server nur seinen eigenen Ordner
 * zu sehen statt der ganzen Platte.
 *
 * Der grosse Unterschied zu Pterodactyl bleibt: Das hier spricht nicht
 * mit der Docker-API, sondern ruft `docker run` auf und redet ueber
 * stdin und stdout mit dem Server - genau wie vorher mit `java`. Damit
 * aendert sich am Panel fast nichts, nur der Befehl davor.
 *
 * Ist Docker nicht da, laeuft alles wie bisher direkt als Java-Prozess.
 * Das Panel sagt dann, in welcher Betriebsart ein Server laeuft; still
 * das eine fuer das andere ausgeben waere das Schlimmste.
 */

import { spawnSync } from 'node:child_process';
import { statSync, chownSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

/** Wie das Standard-Image heisst. Minecraft 1.21 braucht Java 21. */
export const STANDARD_BILD = process.env.DOCKER_IMAGE || 'eclipse-temurin:21-jre';

/** Mehr Prozesse braucht kein Minecraft-Server - das stoppt Fork-Bomben. */
const PIDS = 512;

let gemerkt = null;

/**
 * Laesst sich Docker benutzen?
 *
 * Einmal geprueft und gemerkt - `docker info` dauert spuerbar, und die
 * Antwort aendert sich im Betrieb praktisch nie. DOCKER=aus schaltet die
 * Betriebsart ab, ohne dass man Docker deinstallieren muss.
 *
 * `grund` ist so formuliert, dass es hinter dem Wort "Docker" steht:
 * "Docker ist nicht installiert". Sonst stuende dort zweimal Docker.
 */
export function vorhanden() {
  if (gemerkt !== null) return gemerkt;
  if (/^(aus|nein|0|off|false)$/i.test(process.env.DOCKER || '')) {
    gemerkt = { geht: false, grund: 'ist per DOCKER=aus abgeschaltet' };
    return gemerkt;
  }
  try {
    const lauf = spawnSync('docker', ['info', '--format', '{{.ServerVersion}}'],
      { encoding: 'utf8', timeout: 8000 });
    if (lauf.status === 0) {
      gemerkt = { geht: true, version: (lauf.stdout || '').trim() };
    } else {
      gemerkt = { geht: false, grund: grundAus(lauf) };
    }
  } catch (fehler) {
    gemerkt = { geht: false, grund: fehler.message };
  }
  return gemerkt;
}

/**
 * Warum ging es nicht - in einem Satz, mit dem man etwas anfangen kann.
 *
 * spawnSync wirft nicht, wenn das Programm fehlt, sondern legt den
 * Fehler in `.error` und laesst stderr leer. Genau das hat hier anfangs
 * "unbekannter Fehler" ergeben - die nutzloseste aller Meldungen, und
 * ausgerechnet in dem Fall, der am haeufigsten vorkommt: Docker ist gar
 * nicht installiert.
 */
function grundAus(lauf) {
  if (lauf.error?.code === 'ENOENT') {
    return 'ist nicht installiert';
  }
  if (lauf.error?.code === 'ETIMEDOUT') {
    return 'antwortet nicht – läuft der Dienst?';
  }
  const stderr = (lauf.stderr || '').split('\n')
    .map((z) => z.trim()).filter(Boolean)[0] || '';
  // Docker Desktop meldet sich so, wenn es installiert ist, aber nicht laeuft.
  if (/cannot find the file|pipe.*dockerDesktop|daemon is not running|connect: /i.test(stderr)) {
    return 'ist installiert, läuft aber nicht – starte Docker Desktop';
  }
  if (stderr) return stderr.slice(0, 120);
  return lauf.error?.message?.slice(0, 120) || 'meldet sich nicht';
}

/** Fuer Tests: die gemerkte Antwort wieder vergessen. */
export const vergiss = () => { gemerkt = null; };

export function bildDa(bild = STANDARD_BILD) {
  try {
    const lauf = spawnSync('docker', ['image', 'inspect', bild, '--format', '{{.Id}}'],
      { encoding: 'utf8', timeout: 8000 });
    return lauf.status === 0;
  } catch {
    return false;
  }
}

export const behaelterName = (serverId) => `lemon-server-${serverId}`;

/**
 * Reste eines fruehreren Laufs wegraeumen.
 *
 * Wird das Portal hart abgeschossen, ueberlebt der Container. Beim
 * naechsten Start scheiterte `docker run` dann an einem Namen, den es
 * schon gibt - mit einer Meldung, die niemand mit dem Absturz von
 * gestern in Verbindung bringt.
 */
export function raeumeAuf(serverId) {
  try {
    spawnSync('docker', ['rm', '-f', behaelterName(serverId)],
      { timeout: 15000, stdio: 'ignore' });
  } catch { /* war nichts da */ }
}

/** Laeuft fuer diesen Server gerade ein Container? */
export function laeuftContainer(serverId) {
  try {
    const lauf = spawnSync('docker', ['ps', '-q', '--filter', 'name=^/'
      + behaelterName(serverId) + '$'], { encoding: 'utf8', timeout: 8000 });
    return Boolean((lauf.stdout || '').trim());
  } catch {
    return false;
  }
}

/** Wenn der Server auf "stop" nicht hoert: den Container abschiessen. */
export function killeContainer(serverId) {
  try {
    spawnSync('docker', ['kill', behaelterName(serverId)],
      { timeout: 15000, stdio: 'ignore' });
  } catch { /* schon weg */ }
}

/**
 * Wieviel Heap darf die JVM im Container bekommen?
 *
 * Nicht die ganze Containergrenze: Eine JVM braucht neben dem Heap noch
 * Metaspace, Threadstacks und Direktpuffer. Wer beides gleichsetzt,
 * bekommt einen Server, den der Kernel ohne Vorwarnung abschiesst -
 * "exit code 137" und kein Wort in der Konsole, warum. 85 Prozent lassen
 * genug Luft.
 */
export const heapMB = (mb) => Math.max(512, Math.floor(mb * 0.85));

/**
 * Die Argumente fuer `docker run`.
 *
 * Als eigene Funktion, damit ein Test sie pruefen kann, ohne dass ein
 * Container starten muss.
 */
export function laufArgumente({ serverId, ordner, speicherMB, cores, port,
                                bild = STANDARD_BILD, argumente: javaArgumente = [],
                                nutzer = null }) {
  const argumente = [
    'run', '--rm', '-i',
    '--name', behaelterName(serverId),
    '--hostname', `server-${serverId}`,
    // Nur der eigene Ordner ist zu sehen - nicht die Platte, nicht die
    // Server der anderen Klassen.
    '-v', `${ordner}:/data`,
    '-w', '/data',
    // Harte Speichergrenze. memory-swap gleich memory heisst: kein Swap,
    // sonst waere die Grenze nur eine Empfehlung.
    '--memory', `${speicherMB}m`,
    '--memory-swap', `${speicherMB}m`,
    '--pids-limit', String(PIDS),
    // Ein Minecraft-Server darf keine neuen Rechte erwerben.
    '--security-opt', 'no-new-privileges',
    '-p', `${port}:${port}`,
  ];

  if (cores > 0) argumente.push('--cpus', String(cores));
  // Dieselbe Kennung wie das Portal: Dann gehoeren die Dateien im
  // gemounteten Ordner hinterher niemand Fremdem, und der Dateimanager
  // kann sie weiter bearbeiten.
  if (nutzer) argumente.push('--user', nutzer);

  // Der Startbefehl kommt fertig zerlegt herein - siehe start.js. Damit
  // laeuft er nie durch eine Shell, weder hier noch im Container.
  argumente.push(bild, 'java', ...javaArgumente);
  return argumente;
}

/**
 * Unter welcher Kennung soll der Server im Container laufen?
 *
 * Im Normalfall unter derselben wie das Portal - dann gehoeren die
 * Dateien im gemounteten Ordner hinterher niemand Fremdem, und der
 * Dateimanager kann sie weiter bearbeiten.
 *
 * Laeuft das Portal aber selbst als root, waere das genau der Fehler,
 * den Container verhindern sollen: ein Minecraft-Server mit root-Rechten
 * auf einem gemounteten Ordner. Dann lieber eine gewoehnliche Kennung -
 * DOCKER_USER, sonst 1000:1000. Der Serverordner wird beim Start
 * passend umgeschrieben, sonst duerfte dort niemand schreiben.
 */
export function eigeneKennung() {
  // Unter Windows gibt es das nicht; dort regelt Docker Desktop die
  // Rechte selbst und --user wuerde nur stoeren.
  if (typeof process.getuid !== 'function') return null;
  const uid = process.getuid();
  if (uid !== 0) return `${uid}:${process.getgid()}`;
  return process.env.DOCKER_USER || '1000:1000';
}

function schreibeUmRekursiv(pfad, uid, gid, tiefe = 0) {
  if (tiefe > 12) return;
  try {
    chownSync(pfad, uid, gid);
    for (const e of readdirSync(pfad, { withFileTypes: true })) {
      if (e.isSymbolicLink()) continue;
      const voll = join(pfad, e.name);
      if (e.isDirectory()) schreibeUmRekursiv(voll, uid, gid, tiefe + 1);
      else { try { chownSync(voll, uid, gid); } catch { /* egal */ } }
    }
  } catch { /* kein Recht oder verschwunden */ }
}

/**
 * Dem Serverordner die Kennung geben, unter der der Container laeuft.
 *
 * Sonst startet der Server und darf seine eigene Welt nicht schreiben -
 * mit einer Java-Meldung, die niemand mit Dateirechten in Verbindung
 * bringt. Laeuft nur, wenn die Kennung nicht ohnehin schon stimmt; der
 * Normalfall kostet also einen einzigen stat-Aufruf.
 */
export function richteRechteEin(ordner, kennung) {
  if (!kennung || typeof process.getuid !== 'function') return null;
  if (process.getuid() !== 0) return null;      // nur root darf chown
  const [uid, gid] = kennung.split(':').map(Number);
  try {
    const s = statSync(ordner);
    if (s.uid === uid && s.gid === gid) return null;
    schreibeUmRekursiv(ordner, uid, gid);
    return `Serverordner auf Kennung ${kennung} umgestellt.`;
  } catch (fehler) {
    return 'Rechte am Serverordner ließen sich nicht setzen: ' + fehler.message;
  }
}
