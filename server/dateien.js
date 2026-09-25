const fs = require('node:fs');
const fsp = require('node:fs/promises');
const path = require('node:path');
const { pipeline } = require('node:stream/promises');
const { WURZEL, NIE_AUSLIEFERN, istSichererTeil } = require('./pfade');
const { textSenden } = require('./antwort');
const { link, seitenKopf, seite } = require('./vorlage');

const DATEITYPEN = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.mp4': 'video/mp4',
  '.webm': 'video/webm',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
};

const LANGE_ZWISCHENSPEICHERN = ['assets/fonts/', 'assets/vendor/'];

let echteWurzel = null;

function wurzelEcht() {
  if (!echteWurzel) {
    echteWurzel = fs.realpathSync(WURZEL);
  }
  return echteWurzel;
}

function teileAusUrl(urlPfad) {
  let entschluesselt;
  try {
    entschluesselt = decodeURIComponent(urlPfad);
  } catch {
    return null;
  }
  const teile = entschluesselt.split('/').filter(teil => teil !== '');
  if (!teile.every(istSichererTeil)) {
    return null;
  }
  if (teile.length > 0 && NIE_AUSLIEFERN.has(teile[0])) {
    return null;
  }
  return teile;
}

async function statOderNull(datei) {
  try {
    return await fsp.stat(datei);
  } catch {
    return null;
  }
}

function bereichLesen(kopf, groesse) {
  const treffer = /^bytes=(\d*)-(\d*)$/.exec(String(kopf).trim());
  if (!treffer) {
    return null;
  }
  const [, von, bis] = treffer;
  if (von === '' && bis === '') {
    return 'ungueltig';
  }
  let start;
  let ende;
  if (von === '') {
    const laenge = Number(bis);
    if (laenge === 0) {
      return 'ungueltig';
    }
    start = Math.max(0, groesse - laenge);
    ende = groesse - 1;
  } else {
    start = Number(von);
    ende = bis === '' ? groesse - 1 : Math.min(Number(bis), groesse - 1);
  }
  if (start >= groesse || start > ende) {
    return 'ungueltig';
  }
  return { start, ende };
}

function fehlerSeite(urlPfad) {
  const tiefe = Math.max(0, (urlPfad.match(/\//g) || []).length - 1);
  const seitenPfad = 'x/'.repeat(tiefe);
  const ziele = [
    ['emerald', 'fa-house', 'Startseite', 'Alles über BuckSMP auf einen Blick.', 'start'],
    ['diamond', 'fa-book-open', 'Wiki', 'Anleitungen, Befehle und Hintergründe.', 'wiki'],
    ['gold', 'fa-circle-question', 'Hilfe-Center', 'Antworten auf die häufigsten Fragen.', 'help'],
  ];
  const karten = ziele.map(([farbe, icon, name, beschreibung, ziel]) => `      <a class="thema ${farbe}" href="${link(seitenPfad, ziel)}">
        <div class="slot"><i class="fa-solid ${icon}"></i></div>
        <div>
          <p class="thema-name">${name} <i class="fa-solid fa-arrow-right"></i></p>
          <p class="thema-desc">${beschreibung}</p>
        </div>
      </a>`).join('\n');
  const inhalt = seitenKopf(seitenPfad, [['Seite nicht gefunden', seitenPfad]], 'Seite nicht <span class="g">gefunden</span>', 'Diese Seite gibt es nicht – vielleicht hat sich die Adresse geändert. Hier geht es weiter:') + `
<section class="sec">
  <div class="wrap">
    <div class="themen">
${karten}
    </div>
  </div>
</section>
`;
  return seite(seitenPfad, '', 'Seite nicht gefunden – BuckSMP', 'Diese Seite gibt es auf BuckSMP nicht.', inhalt);
}

function nichtGefunden(anfrage, antwort, urlPfad) {
  const endung = path.extname(urlPfad).toLowerCase();
  const willSeite = (endung === '' || endung === '.html') && String(anfrage.headers.accept || '').includes('text/html');
  if (!willSeite) {
    textSenden(antwort, 404, 'Nicht gefunden', { 'Cache-Control': 'no-cache' });
    return;
  }
  const html = fehlerSeite(urlPfad);
  antwort.writeHead(404, {
    'Content-Type': 'text/html; charset=utf-8',
    'Content-Length': Buffer.byteLength(html),
    'Cache-Control': 'no-cache',
  });
  antwort.end(anfrage.method === 'HEAD' ? undefined : html);
}

async function dateiAusliefern(anfrage, antwort, url) {
  const teile = teileAusUrl(url.pathname);
  if (!teile) {
    nichtGefunden(anfrage, antwort, url.pathname);
    return;
  }

  let datei = path.join(WURZEL, ...teile);
  let info = await statOderNull(datei);
  if (info && info.isDirectory()) {
    if (!url.pathname.endsWith('/')) {
      antwort.writeHead(301, { Location: `${url.pathname}/${url.search}` });
      antwort.end();
      return;
    }
    datei = path.join(datei, 'index.html');
    info = await statOderNull(datei);
  }

  const typ = DATEITYPEN[path.extname(datei).toLowerCase()];
  if (!info || !info.isFile() || !typ) {
    nichtGefunden(anfrage, antwort, url.pathname);
    return;
  }

  let echterPfad;
  try {
    echterPfad = await fsp.realpath(datei);
  } catch {
    nichtGefunden(anfrage, antwort, url.pathname);
    return;
  }
  if (!echterPfad.startsWith(wurzelEcht() + path.sep)) {
    nichtGefunden(anfrage, antwort, url.pathname);
    return;
  }

  const relativ = teile.join('/');
  const etag = `W/"${info.size.toString(16)}-${Math.floor(info.mtimeMs).toString(16)}"`;
  const kopf = {
    'Content-Type': typ,
    'Last-Modified': info.mtime.toUTCString(),
    ETag: etag,
    'Cache-Control': LANGE_ZWISCHENSPEICHERN.some(anfang => relativ.startsWith(anfang)) ? 'public, max-age=604800' : 'no-cache',
    'Accept-Ranges': 'bytes',
  };

  if (anfrage.headers['if-none-match'] === etag) {
    antwort.writeHead(304, kopf);
    antwort.end();
    return;
  }

  let bereich = anfrage.headers.range ? bereichLesen(anfrage.headers.range, info.size) : null;
  if (bereich && anfrage.headers['if-range'] && anfrage.headers['if-range'] !== etag) {
    bereich = null;
  }
  if (bereich === 'ungueltig') {
    antwort.writeHead(416, { 'Content-Range': `bytes */${info.size}` });
    antwort.end();
    return;
  }

  let start = 0;
  let ende = info.size - 1;
  let status = 200;
  if (bereich) {
    start = bereich.start;
    ende = bereich.ende;
    status = 206;
    kopf['Content-Range'] = `bytes ${start}-${ende}/${info.size}`;
  }
  kopf['Content-Length'] = info.size === 0 ? 0 : ende - start + 1;
  antwort.writeHead(status, kopf);

  if (anfrage.method === 'HEAD' || info.size === 0) {
    antwort.end();
    return;
  }
  try {
    await pipeline(fs.createReadStream(datei, { start, end: ende }), antwort);
  } catch {
    antwort.destroy();
  }
}

module.exports = {
  dateiAusliefern,
  nichtGefunden,
};
