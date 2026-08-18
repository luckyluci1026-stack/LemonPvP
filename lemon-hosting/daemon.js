#!/usr/bin/env node
/**
 * Der Lemon-Daemon - das Gegenstueck zu Pterodactyls "Wings".
 *
 * Laeuft auf jeder Maschine, auf der Minecraft-Server stehen sollen, und
 * macht dort die eigentliche Arbeit: Prozesse starten, Konsole
 * durchreichen, Dateien verwalten, Backups packen. Das Portal selbst
 * kann dann auf einem ganz anderen Rechner stehen.
 *
 *   node daemon.js
 *
 * Beim ersten Start wird ein Zeichen erzeugt und einmal ausgegeben - das
 * traegst du im Portal unter "Knoten" ein. Danach steht es nur noch in
 * daten/daemon.json.
 *
 *   PORT=8390            worauf der Daemon hoert
 *   SERVER_DIR=/pfad     wo die Server liegen
 *   ZEICHEN=...          Zeichen fest vorgeben statt erzeugen
 *
 * Der Daemon fuehrt bewusst **keine Datenbank**. Er weiss nur, was
 * Ordner und Prozesse sind; wieviel Speicher jemand gebucht hat, sagt
 * ihm das Portal bei jedem Aufruf mit. Damit gibt es genau eine Stelle,
 * an der Pakete und Preise stehen - im Portal.
 *
 * Sicherheit: Jede Anfrage muss das Zeichen im Kopf `X-Lemon-Zeichen`
 * mitbringen, verglichen wird zeichenweise ohne Abbruch. Der Daemon
 * gehoert **nicht** ins offene Netz - er hat keine Benutzerverwaltung,
 * sondern genau ein Zeichen. Ins interne Netz oder hinter einen
 * Reverse-Proxy mit HTTPS, sonst geht das Zeichen im Klartext mit.
 */

const [gross, klein] = process.versions.node.split('.').map(Number);
if (gross < 22 || (gross === 22 && klein < 5)) {
  console.error(`\n  Der Daemon braucht Node 22.5 oder neuer, hier läuft ${process.versions.node}.\n`);
  process.exit(1);
}

const warnenAlt = process.emitWarning;
process.emitWarning = (warnung, ...rest) => {
  if (String(warnung).includes('SQLite is an experimental feature')) return;
  return warnenAlt.call(process, warnung, ...rest);
};

const { createServer } = await import('node:http');
const { createReadStream, statSync, mkdirSync, existsSync,
        readFileSync, writeFileSync } = await import('node:fs');
const { randomBytes, timingSafeEqual } = await import('node:crypto');
const { join, basename } = await import('node:path');

const prozess = await import('./src/panel.js');
const dat = await import('./src/dateien.js');
const sich = await import('./src/sicherung.js');
const kat = await import('./src/katalog.js');
const docker = await import('./src/docker.js');

const port = Number(process.env.PORT) || 8390;

// ------------------------------------------------------------------ Zeichen

/**
 * Das Zeichen bleibt zwischen zwei Starts gleich.
 *
 * Ein neues bei jedem Start hiesse, dass das Portal nach jedem Neustart
 * des Daemons nicht mehr hereinkaeme - und niemand wuesste sofort, warum.
 */
function zeichenHolen() {
  if (process.env.ZEICHEN) return { zeichen: process.env.ZEICHEN, neu: false };
  const datei = join('daten', 'daemon.json');
  try {
    if (existsSync(datei)) {
      return { zeichen: JSON.parse(readFileSync(datei, 'utf8')).zeichen, neu: false };
    }
  } catch { /* kaputt - dann eben neu */ }
  const zeichen = randomBytes(24).toString('hex');
  mkdirSync('daten', { recursive: true });
  writeFileSync(datei, JSON.stringify({ zeichen }, null, 2));
  return { zeichen, neu: true };
}

const { zeichen: ZEICHEN, neu: zeichenIstNeu } = zeichenHolen();

function zeichenStimmt(mitgebracht) {
  const a = Buffer.from(String(mitgebracht || ''));
  const b = Buffer.from(ZEICHEN);
  return a.length === b.length && timingSafeEqual(a, b);
}

// ------------------------------------------------------------------ Antworten

const json = (antwort, daten, status = 200) => {
  antwort.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
  antwort.end(JSON.stringify(daten));
};

async function koerper(anfrage, maxBytes = 1024 * 1024) {
  return new Promise((fertig, schief) => {
    let roh = '';
    anfrage.on('data', (stueck) => {
      roh += stueck;
      if (roh.length > maxBytes) { anfrage.destroy(); schief(new Error('Zu groß')); }
    });
    anfrage.on('end', () => {
      try { fertig(roh ? JSON.parse(roh) : {}); } catch { fertig({}); }
    });
    anfrage.on('error', schief);
  });
}

// ------------------------------------------------------------------ Server

const daemon = createServer(async (anfrage, antwort) => {
  const url = new URL(anfrage.url, 'http://daemon');
  const teile = url.pathname.split('/').filter(Boolean);
  const was = teile[0] || '';
  const id = Number(teile[1]);

  try {
    if (!zeichenStimmt(anfrage.headers['x-lemon-zeichen'])) {
      return json(antwort, { fehler: 'Zeichen stimmt nicht.' }, 401);
    }

    // -------------------------------------------------------------- Hallo
    if (was === 'hallo') {
      const d = docker.vorhanden();
      return json(antwort, {
        name: 'Lemon-Daemon',
        node: process.versions.node,
        docker: d.geht ? d.version : null,
        dockerGrund: d.geht ? null : d.grund,
        bild: docker.STANDARD_BILD,
        bildDa: d.geht ? docker.bildDa() : false,
        serverOrdner: prozess.wurzel(),
        katalog: kat.katalogOrdner(),
        plugins: kat.verfuegbar().length,
      });
    }

    // ------------------------------------------------------------ Zustaende
    if (was === 'zustaende') {
      const raus = {};
      for (const roh of (url.searchParams.get('ids') || '').split(',')) {
        const n = Number(roh);
        if (Number.isInteger(n) && n > 0) raus[n] = prozess.status(n);
      }
      return json(antwort, raus);
    }

    if (!Number.isInteger(id) || id <= 0) {
      return json(antwort, { fehler: 'Server-Nummer fehlt.' }, 400);
    }

    // -------------------------------------------------------------- Konsole
    if (was === 'konsole') {
      antwort.writeHead(200, {
        'Content-Type': 'text/event-stream; charset=utf-8',
        'Cache-Control': 'no-cache, no-transform',
        Connection: 'keep-alive',
        'X-Accel-Buffering': 'no',
      });
      const schick = (art, daten) => {
        try { antwort.write(`event: ${art}\ndata: ${JSON.stringify(daten)}\n\n`); }
        catch { /* Leitung weg */ }
      };
      schick('verlauf', prozess.konsole(id));
      schick('status', prozess.status(id));
      const abmelden = prozess.hoereZu(id, (zeile) => schick('zeile', zeile));
      const takt = setInterval(() => schick('status', prozess.status(id)), 4000);
      const schluss = () => { clearInterval(takt); abmelden(); };
      anfrage.on('close', schluss);
      anfrage.on('error', schluss);
      return undefined;
    }

    // ------------------------------------------------------------- Steuern
    if (was === 'aktion') {
      const d = await koerper(anfrage);
      const server = { ...(d.server || {}), id };
      if (d.was === 'eula') { prozess.eulaAnnehmen(id); return json(antwort, { ok: true }); }
      const machen = {
        start: () => prozess.starte(server),
        stopp: () => prozess.stoppe(id),
        neustart: () => prozess.neustart(server),
      }[d.was];
      if (!machen) return json(antwort, { fehler: 'Unbekannte Aktion.' }, 400);
      const fehler = machen();
      return json(antwort, fehler ? { fehler } : { ok: true });
    }

    if (was === 'befehl') {
      const d = await koerper(anfrage);
      const fehler = prozess.befehl(id, d.befehl || '');
      return json(antwort, fehler ? { fehler } : { ok: true }, fehler ? 409 : 200);
    }

    // ------------------------------------------------------------- Dateien
    if (was === 'dateien') {
      const eintraege = dat.liste(id, dat.saeubere(url.searchParams.get('p')));
      return eintraege
        ? json(antwort, { eintraege })
        : json(antwort, { fehler: 'Diesen Ordner gibt es nicht.' }, 404);
    }

    if (was === 'lies') {
      const inhalt = dat.lies(id, dat.saeubere(url.searchParams.get('p')));
      return inhalt === null
        ? json(antwort, { fehler: 'Nicht lesbar.' }, 404)
        : json(antwort, { inhalt });
    }

    if (was === 'schreib') {
      const d = await koerper(anfrage, 4 * 1024 * 1024);
      const fehler = dat.schreib(id, dat.saeubere(d.p), d.inhalt ?? '');
      return json(antwort, fehler ? { fehler } : { ok: true }, fehler ? 400 : 200);
    }

    if (was === 'loesche') {
      const d = await koerper(anfrage);
      const fehler = dat.loesche(id, dat.saeubere(d.p));
      return json(antwort, fehler ? { fehler } : { ok: true }, fehler ? 400 : 200);
    }

    if (was === 'ordner') {
      const d = await koerper(anfrage);
      const fehler = dat.neuerOrdner(id, dat.saeubere(d.p));
      return json(antwort, fehler ? { fehler } : { ok: true }, fehler ? 400 : 200);
    }

    if (was === 'hochladen') {
      const fehler = await dat.nimmDatei(id, url.searchParams.get('p'),
        url.searchParams.get('name'), anfrage);
      return json(antwort, fehler ? { fehler } : { ok: true }, fehler ? 400 : 200);
    }

    if (was === 'belegung') return json(antwort, { bytes: dat.belegung(id) });

    // ------------------------------------------------------------- Backups
    if (was === 'sicherungen') return json(antwort, { liste: sich.liste(id) });

    if (was === 'sicherung' && anfrage.method === 'POST') {
      const ergebnis = await sich.anlegen(id);
      return json(antwort, ergebnis, ergebnis.fehler ? 400 : 200);
    }

    if (was === 'sicherung') {
      const datei = sich.pfadVon(id, url.searchParams.get('f'));
      if (!datei) return json(antwort, { fehler: 'Gibt es nicht.' }, 404);
      antwort.writeHead(200, {
        'Content-Type': 'application/zip',
        'Content-Length': statSync(datei).size,
        'Content-Disposition': `attachment; filename="${basename(datei)}"`,
      });
      return createReadStream(datei).pipe(antwort);
    }

    if (was === 'sicherung-weg') {
      const d = await koerper(anfrage);
      const fehler = sich.loeschen(id, d.f);
      return json(antwort, fehler ? { fehler } : { ok: true }, fehler ? 400 : 200);
    }

    if (was === 'sicherung-zurueck') {
      const d = await koerper(anfrage);
      const ergebnis = sich.zurueckspielen(id, d.f);
      return json(antwort, ergebnis, ergebnis.fehler ? 400 : 200);
    }

    // ------------------------------------------------------------- Plugins
    if (was === 'plugins') {
      const da = kat.installiert(id);
      return json(antwort, {
        liste: kat.verfuegbar().map((p) => ({ ...p, da: da.has(p.stamm) })),
      });
    }

    if (was === 'plugin') {
      const d = await koerper(anfrage);
      const ergebnis = d.art === 'raus'
        ? kat.entferne(id, d.datei)
        : kat.installiere(id, d.datei);
      return json(antwort, ergebnis, ergebnis.fehler ? 400 : 200);
    }

    return json(antwort, { fehler: 'Unbekannter Aufruf.' }, 404);
  } catch (fehler) {
    console.error('Fehler bei', anfrage.method, anfrage.url, '\n', fehler);
    if (!antwort.headersSent) json(antwort, { fehler: 'Interner Fehler.' }, 500);
  }
});

daemon.on('error', (fehler) => {
  console.error(fehler.code === 'EADDRINUSE'
    ? `\n  Port ${port} ist schon belegt. Nimm einen anderen:  PORT=${port + 1} node daemon.js\n`
    : '  Der Daemon konnte nicht starten: ' + fehler.message);
  process.exit(1);
});

daemon.listen(port, () => {
  const d = docker.vorhanden();
  console.log(`\n  🍋 Lemon-Daemon`);
  console.log(`     hört auf   Port ${port}`);
  console.log(`     Server in  ${prozess.wurzel()}`);
  console.log(`     Plugins    ${kat.katalogOrdner()} (${kat.verfuegbar().length} im Katalog)`);
  console.log(`     Docker     ${d.geht ? d.version + (docker.bildDa()
    ? ` · Image ${docker.STANDARD_BILD} liegt bereit`
    : ` · ⚠ Image ${docker.STANDARD_BILD} fehlt`) : 'nicht verfügbar'}`);

  if (zeichenIstNeu) {
    console.log('\n  ┌────────────────────────────────────────────────────────────────┐');
    console.log('  │  Neuer Knoten – trag das im Portal unter „Knoten" ein:         │');
    console.log('  └────────────────────────────────────────────────────────────────┘');
    console.log(`     Adresse   http://<diese-maschine>:${port}`);
    console.log(`     Zeichen   ${ZEICHEN}`);
    console.log('\n     Es steht auch in daten/daemon.json – aber nirgends sonst.');
  } else {
    console.log('\n     Zeichen aus daten/daemon.json geladen.');
  }
  console.log('\n     Achtung: Der Daemon hat keine Benutzerverwaltung, nur dieses');
  console.log('     eine Zeichen. Er gehört ins interne Netz oder hinter einen');
  console.log('     Reverse-Proxy mit HTTPS – sonst geht es im Klartext mit.\n');
});

let gehtGerade = false;
const runterfahren = async (signal) => {
  if (gehtGerade) { console.log(`  Noch einmal ${signal} – dann sofort.`); process.exit(1); }
  gehtGerade = true;
  daemon.close();
  console.log('\n  Fahre die Minecraft-Server herunter …');
  const haengen = await prozess.alleStoppen();
  if (haengen) console.log(`  ${haengen} Server musste hart beendet werden.`);
  console.log('  Fertig.');
  process.exit(0);
};
process.on('SIGINT', () => runterfahren('SIGINT'));
process.on('SIGTERM', () => runterfahren('SIGTERM'));
