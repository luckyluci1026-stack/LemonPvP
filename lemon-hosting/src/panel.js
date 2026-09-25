/**
 * Das Panel: Minecraft-Server wirklich starten, stoppen und bedienen.
 *
 * Der Unterschied zu Pterodactyl ist die Aufteilung. Pterodactyl trennt
 * Panel und Daemon ("Wings"), damit ein Panel viele Maschinen steuern
 * kann. Hier laeuft alles auf einem Rechner, und das Portal startet die
 * Server direkt als Kindprozesse. Das ist viel weniger Technik - und
 * genau richtig, solange alle Server auf derselben Kiste liegen.
 *
 * Wer schon ein echtes Pterodactyl betreibt, laesst es einfach stehen:
 * ein Server kann im Portal eine Pterodactyl-Adresse hinterlegt haben,
 * dann verwaltet das Portal ihn nicht selbst, sondern verlinkt dorthin.
 *
 * Was ein Server ist:
 *
 *   server/<id>/            der Ordner, in dem alles liegt
 *   server/<id>/server.jar  die Serversoftware
 *   server/<id>/eula.txt    wird beim ersten Start angelegt
 *
 * Der Arbeitsspeicher kommt aus dem gebuchten Paket: Wer Coal mit 3,25 GB
 * hat, bekommt -Xmx3328M. Ein Kunde kann sich also nicht selbst mehr
 * geben, als er gebucht hat.
 */

import { spawn } from 'node:child_process';
import { mkdirSync, existsSync, writeFileSync, readFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { rechne } from './preise.js';
import * as docker from './docker.js';
import * as messung from './messung.js';
import * as startbefehl from './start.js';

/** serverId -> laufender Prozess samt Konsole */
const laufend = new Map();

/** Wie viele Konsolenzeilen wir uns merken. */
const PUFFER = 400;

export function wurzel() {
  return resolve(process.env.SERVER_DIR || 'server');
}

export function ordnerVon(serverId) {
  const pfad = join(wurzel(), String(serverId));
  mkdirSync(pfad, { recursive: true });
  return pfad;
}

/**
 * Ein laufender Server.
 *
 * "zuhoerer" sind offene Browserfenster, die die Konsole mitlesen. Sie
 * bekommen jede neue Zeile sofort geschickt.
 */
function neuerLauf() {
  return {
    prozess: null,
    zeilen: [],
    zuhoerer: new Set(),
    gestartet: null,
    status: 'gestoppt',
    spieler: new Set(),
    motor: null,
  };
}

/**
 * Wer ist online?
 *
 * Steht so in der Konsole - Minecraft meldet jeden Beitritt und jeden
 * Abgang. Die Alternative waere das Query- oder RCON-Protokoll: ein
 * zweiter Port, eine zweite Einstellung in server.properties und ein
 * Stueck Netzwerkcode. Fuer eine Namensliste ist das Mitlesen ehrlicher.
 *
 * Der Name kommt vor "joined the game" und ist nie laenger als 16
 * Zeichen - damit faellt auch ein Spieler heraus, der sich selbst
 * "xy joined the game" in den Chat schreibt: Chatzeilen stehen in
 * spitzen Klammern.
 */
const BEITRITT = /]: ([A-Za-z0-9_]{2,16}) joined the game/g;
const ABGANG = /]: ([A-Za-z0-9_]{2,16}) left the game/g;

/**
 * Ein Datenstueck von stdout kann mehrere Zeilen enthalten - beim Start
 * kommen schon mal fuenf auf einmal. Deshalb matchAll und nicht match:
 * sonst betraete von zwei gleichzeitig verbundenen Spielern nur einer
 * die Liste, und der andere waere fuer das Panel nie da gewesen.
 */
function merkeSpieler(l, text) {
  for (const treffer of text.matchAll(BEITRITT)) l.spieler.add(treffer[1]);
  for (const treffer of text.matchAll(ABGANG)) l.spieler.delete(treffer[1]);
}

function lauf(serverId) {
  if (!laufend.has(serverId)) laufend.set(serverId, neuerLauf());
  return laufend.get(serverId);
}

function schreibe(serverId, text, art = 'aus') {
  const l = lauf(serverId);
  for (const roh of String(text).split(/\r?\n/)) {
    // Leerzeilen entstehen fast nur an den Grenzen der Datenstuecke, mit
    // denen stdout ankommt - in der Konsole waeren sie nur Luecken.
    if (!roh.trim()) continue;
    const zeile = { zeit: Date.now(), art, text: roh };
    l.zeilen.push(zeile);
    if (l.zeilen.length > PUFFER) l.zeilen.shift();
    for (const zuhoerer of l.zuhoerer) {
      try { zuhoerer(zeile); } catch { /* Fenster ist weg */ }
    }
  }
}

// ------------------------------------------------------------------ Status

export function status(serverId) {
  const l = laufend.get(serverId);
  if (!l || !l.prozess) {
    return { status: 'gestoppt', seit: null, pid: null, laufzeit: 0,
             spieler: [], motor: null };
  }
  return {
    status: l.status,
    seit: l.gestartet,
    pid: l.prozess.pid,
    laufzeit: l.gestartet ? Math.floor((Date.now() - l.gestartet) / 1000) : 0,
    spieler: [...l.spieler].sort((a, b) => a.localeCompare(b)),
    motor: l.motor,
    verbrauch: messung.messung(serverId),
  };
}

/** Was gerade laeuft - fuer den Messtakt. */
export function laufende() {
  const raus = [];
  for (const [id, l] of laufend) {
    if (l.prozess) raus.push({ id, pid: l.prozess.pid, motor: l.motor });
  }
  return raus;
}

/** Den Messtakt anwerfen. Wird vom Portal und vom Daemon gerufen. */
export const starteMessung = () => messung.starteTicker(laufende);

export const laeuft = (serverId) => Boolean(laufend.get(serverId)?.prozess);

export function konsole(serverId) {
  return lauf(serverId).zeilen;
}

/** Ein Browserfenster haengt sich an die Konsole. Gibt das Abmelden zurueck. */
export function hoereZu(serverId, rueckruf) {
  const l = lauf(serverId);
  l.zuhoerer.add(rueckruf);
  return () => l.zuhoerer.delete(rueckruf);
}

// ------------------------------------------------------------------ Start

/**
 * Wieviel Speicher darf dieser Server haben?
 *
 * Direkt aus dem gebuchten Paket plus Zusatzleistungen - dieselbe
 * Rechnung wie auf dem Bestellbogen. Damit kann niemand sich selbst mehr
 * zuteilen, als er gebucht hat: Es gibt gar keine Stelle, an der man es
 * eintippen koennte.
 */
export function speicherMB(server) {
  const gb = rechne(server.paket, server.zusatz || {}).ausstattung?.ram || 1;
  return Math.max(512, Math.round(gb * 1024));
}

/** Welches Image dieser Server benutzt. */
export const bildVon = (server) => server.docker_bild || docker.STANDARD_BILD;

export function jarDa(serverId, jar = 'server.jar') {
  return existsSync(join(ordnerVon(serverId), jar));
}

export function eulaAngenommen(serverId) {
  const datei = join(ordnerVon(serverId), 'eula.txt');
  if (!existsSync(datei)) return false;
  return /eula\s*=\s*true/i.test(readFileSync(datei, 'utf8'));
}

export function eulaAnnehmen(serverId) {
  writeFileSync(join(ordnerVon(serverId), 'eula.txt'),
    '# Von Lemon Hosting im Panel angenommen\neula=true\n');
}

/**
 * Server starten.
 *
 * Gibt einen Text zurueck, wenn es nicht geht - der landet direkt in der
 * Oberflaeche. Kein Werfen, kein Stacktrace: Wer auf "Start" drueckt,
 * soll lesen koennen, was fehlt.
 */
export function starte(server) {
  const id = server.id;
  const l = lauf(id);
  if (l.prozess) return 'Der Server läuft schon.';

  const ordner = ordnerVon(id);
  const jar = startbefehl.jarOk(server.jar_datei) || startbefehl.STANDARD_JAR;
  if (!jarDa(id, jar)) {
    return jar === startbefehl.STANDARD_JAR
      ? 'Es liegt keine server.jar im Serverordner. Wähl unten eine '
        + 'Serversoftware aus oder lade sie unter "Dateien" hoch.'
      : `Die eingestellte Datei "${jar}" liegt nicht im Serverordner.`;
  }
  if (!eulaAngenommen(id)) {
    return 'Die Minecraft-EULA ist noch nicht angenommen. '
         + 'Der Knopf dafür steht über der Konsole.';
  }

  const mb = speicherMB(server);
  const aus = rechne(server.paket, server.zusatz || {}).ausstattung;
  // Der Port kommt als Startargument, nicht aus server.properties. So
  // stimmt er auch dann, wenn ein Kunde die Datei bearbeitet hat - und
  // zwei Server auf derselben Kiste kommen sich nicht ins Gehege.
  const port = Number(server.port) || 25565;

  // Die Argumente kommen aus der Startvorlage des Servers. Wer nichts
  // eingestellt hat, bekommt genau das, was frueher fest im Code stand.
  const argumenteFuer = (heap) =>
    startbefehl.baueArgumente(server, { speicherMB: mb, port, heapMB: heap });

  /**
   * Container oder direkt?
   *
   * Im Container sind die Grenzen echt - ein Server mit Speicherleck
   * trifft nur sich selbst. Ohne Docker laeuft es wie bisher, und das
   * Panel sagt es auch so; still das eine fuer das andere ausgeben waere
   * das Schlimmste.
   */
  const mitDocker = docker.vorhanden().geht && docker.bildDa(bildVon(server));
  let befehlsZeile;
  let argumente;
  let motor;

  if (mitDocker) {
    docker.raeumeAuf(id);
    const kennung = docker.eigeneKennung();
    const umgestellt = docker.richteRechteEin(ordner, kennung);
    if (umgestellt) schreibe(id, '[Panel] ' + umgestellt, 'panel');
    befehlsZeile = 'docker';
    argumente = docker.laufArgumente({
      serverId: id, ordner, speicherMB: mb, cores: aus?.cores || 1, port,
      bild: bildVon(server), argumente: argumenteFuer(docker.heapMB(mb)),
      nutzer: kennung, weitere: server.ports || [],
    });
    motor = 'docker';
  } else {
    befehlsZeile = 'java';
    argumente = argumenteFuer(mb);
    motor = 'java';
  }

  let prozess;
  try {
    prozess = spawn(befehlsZeile, argumente,
      { cwd: ordner, stdio: ['pipe', 'pipe', 'pipe'] });
  } catch (fehler) {
    return `${befehlsZeile} ließ sich nicht starten: ` + fehler.message;
  }

  l.prozess = prozess;
  l.status = 'startet';
  l.gestartet = Date.now();
  l.zeilen = [];
  l.spieler.clear();
  l.motor = motor;
  const extraPorts = (server.ports || []).map((p) => p.port);
  schreibe(id, motor === 'docker'
    ? `[Panel] Starte im Container: ${mb} MB fest, ${aus?.cores || 1} CPU-Kerne, `
      + `Port ${port}${extraPorts.length ? ' (+ ' + extraPorts.join(', ') + ')' : ''}. `
      + 'Die Grenzen setzt Docker, nicht die JVM.'
    : `[Panel] Starte auf Port ${port} mit ${mb} MB Arbeitsspeicher. `
      + '(Ohne Docker – die Speichergrenze ist nur eine JVM-Einstellung.)', 'panel');

  prozess.stdout.on('data', (stueck) => {
    const text = stueck.toString();
    schreibe(id, text);
    merkeSpieler(l, text);
    // Paper meldet "Done (12.3s)! For help, type help"
    if (l.status === 'startet' && /Done \(/.test(text)) {
      l.status = 'laeuft';
      schreibe(id, '[Panel] Der Server ist bereit.', 'panel');
    }
  });
  prozess.stderr.on('data', (stueck) => schreibe(id, stueck.toString(), 'fehler'));

  prozess.on('error', (fehler) => {
    schreibe(id, '[Panel] Java fehlt oder ließ sich nicht starten: '
      + fehler.message, 'fehler');
    l.prozess = null;
    l.status = 'gestoppt';
    l.spieler.clear();
  });

  prozess.on('exit', (code, signal) => {
    schreibe(id, `[Panel] Server beendet (${signal || 'Code ' + code}).`, 'panel');
    l.prozess = null;
    l.status = 'gestoppt';
    l.gestartet = null;
    l.spieler.clear();
  });

  return null;
}

/** Einen Befehl an die Serverkonsole schicken. */
export function befehl(serverId, text) {
  const l = laufend.get(serverId);
  if (!l || !l.prozess) return 'Der Server läuft nicht.';
  const sauber = String(text).replace(/[\r\n]/g, '').trim();
  if (!sauber) return null;
  schreibe(serverId, '> ' + sauber, 'eingabe');
  try {
    l.prozess.stdin.write(sauber + '\n');
  } catch (fehler) {
    return 'Befehl kam nicht an: ' + fehler.message;
  }
  return null;
}

/**
 * Sauber stoppen.
 *
 * Erst "stop" in die Konsole - dann speichert Minecraft die Welt selbst.
 * Wer stattdessen sofort abschiesst, riskiert kaputte Chunks. Nur wenn
 * nach der Frist noch etwas laeuft, wird nachgeholfen.
 */
export function stoppe(serverId, fristSekunden = 30) {
  const l = laufend.get(serverId);
  if (!l || !l.prozess) return 'Der Server läuft nicht.';
  l.status = 'stoppt';
  schreibe(serverId, '[Panel] Stoppe … die Welt wird gespeichert.', 'panel');
  befehl(serverId, 'stop');
  const prozess = l.prozess;
  const imContainer = l.motor === 'docker';
  setTimeout(() => {
    if (l.prozess !== prozess) return;
    schreibe(serverId, '[Panel] Reagiert nicht – wird jetzt beendet.', 'panel');
    // Beim Container hilft SIGKILL auf den docker-Aufruf nicht: Das
    // beendet nur den Client, der Server liefe im Container weiter.
    if (imContainer) docker.killeContainer(serverId);
    try { prozess.kill('SIGKILL'); } catch { /* schon weg */ }
  }, fristSekunden * 1000).unref();
  return null;
}

/** Neustart: stoppen, warten bis wirklich aus, dann wieder an. */
export function neustart(server) {
  const l = laufend.get(server.id);
  if (!l || !l.prozess) return starte(server);
  schreibe(server.id, '[Panel] Neustart …', 'panel');
  const fehler = stoppe(server.id);
  if (fehler) return fehler;
  const warte = setInterval(() => {
    if (!laufend.get(server.id)?.prozess) {
      clearInterval(warte);
      starte(server);
    }
  }, 500);
  warte.unref();
  return null;
}

/**
 * Beim Herunterfahren des Portals alle Server sauber stoppen.
 *
 * Gibt ein Versprechen zurueck, das erst faellig wird, wenn wirklich
 * keiner mehr laeuft. Wer das Portal mit Strg+C beendet, soll nicht als
 * Nebenwirkung halb gespeicherte Welten bekommen - also warten wir.
 *
 * @returns Anzahl der Server, die sich nicht mehr rechtzeitig gemeldet
 *          haben (die wurden von stoppe() hart beendet).
 */
export function alleStoppen(fristSekunden = 25) {
  const offen = [...laufend.entries()].filter(([, l]) => l.prozess);
  if (!offen.length) return Promise.resolve(0);
  for (const [id] of offen) stoppe(id, fristSekunden);

  return new Promise((fertig) => {
    const spaetestens = Date.now() + (fristSekunden + 5) * 1000;
    const takt = setInterval(() => {
      const noch = [...laufend.values()].filter((l) => l.prozess).length;
      if (noch === 0 || Date.now() > spaetestens) {
        clearInterval(takt);
        fertig(noch);
      }
    }, 300);
  });
}
