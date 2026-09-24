const http = require('node:http');
const { dateiAusliefern } = require('./server/dateien');
const { apiBearbeiten } = require('./server/api');
const { zugangEinrichten } = require('./server/anmeldung');
const { textSenden } = require('./server/antwort');
const wiki = require('./server/wiki');
const moments = require('./server/moments');

const PORT = Number(process.env.PORT || process.env.SERVER_PORT || 8080);

const ADMIN_REGELN = [
  "default-src 'self'",
  "img-src 'self' data: blob:",
  "media-src 'self' blob:",
  "style-src 'self' 'unsafe-inline'",
  "script-src 'self'",
  "frame-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'self'",
].join('; ');

function sicherheitsKopfzeilen(antwort) {
  antwort.setHeader('X-Content-Type-Options', 'nosniff');
  antwort.setHeader('X-Frame-Options', 'SAMEORIGIN');
  antwort.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
}

async function anfrageBearbeiten(anfrage, antwort) {
  sicherheitsKopfzeilen(antwort);

  const adresse = anfrage.url.startsWith('//') ? `/${anfrage.url.replace(/^\/+/, '')}` : anfrage.url;
  let url;
  try {
    url = new URL(adresse, 'http://localhost');
  } catch {
    textSenden(antwort, 400, 'Ungültige Adresse');
    return;
  }

  if (url.pathname.startsWith('/admin/api/')) {
    antwort.setHeader('X-Robots-Tag', 'noindex');
    await apiBearbeiten(anfrage, antwort, url);
    return;
  }

  if (anfrage.method !== 'GET' && anfrage.method !== 'HEAD') {
    textSenden(antwort, 405, 'Methode nicht erlaubt', { Allow: 'GET, HEAD' });
    return;
  }

  if (url.pathname === '/admin' || url.pathname.startsWith('/admin/')) {
    antwort.setHeader('X-Robots-Tag', 'noindex, nofollow');
    antwort.setHeader('Content-Security-Policy', ADMIN_REGELN);
  }

  await dateiAusliefern(anfrage, antwort, url);
}

async function datenAbgleichen() {
  try {
    const ergebnis = await wiki.startAbgleich();
    if (ergebnis.fehler) {
      console.warn(`Wiki: ${ergebnis.fehler}`);
    } else {
      if (ergebnis.neu) {
        console.log('Wiki: Die Artikel wurden aus den vorhandenen Seiten übernommen und in daten/wiki.json gespeichert.');
      }
      if (ergebnis.geaendert > 0) {
        console.log(`Wiki: ${ergebnis.geaendert} Seite(n) aus daten/wiki.json wiederhergestellt.`);
      }
    }
  } catch (fehler) {
    console.warn(`Wiki konnte beim Start nicht abgeglichen werden: ${fehler.message}`);
  }
  try {
    if (!(await moments.startAbgleich())) {
      console.warn('Aura Moments: Die gespeicherten Texte konnten nicht gelesen werden und wurden nicht verändert.');
    }
  } catch (fehler) {
    console.warn(`Aura Moments konnten beim Start nicht abgeglichen werden: ${fehler.message}`);
  }
}

async function starten() {
  const hauptversion = Number(process.versions.node.split('.')[0]);
  if (hauptversion < 18) {
    console.error(`Node ${process.versions.node} ist zu alt. Bitte Node 18 oder neuer benutzen – am besten Node 22.`);
    process.exit(1);
  }

  const zugang = await zugangEinrichten();
  await datenAbgleichen();

  const server = http.createServer((anfrage, antwort) => {
    anfrageBearbeiten(anfrage, antwort).catch(fehler => {
      console.error(fehler);
      if (!antwort.headersSent) {
        textSenden(antwort, 500, 'Interner Fehler');
      } else {
        antwort.destroy();
      }
    });
  });
  server.requestTimeout = 60 * 60 * 1000;

  server.on('error', fehler => {
    if (fehler.code === 'EADDRINUSE') {
      console.error(`Port ${PORT} ist schon belegt. Starte mit einem anderen Port, zum Beispiel: PORT=8081 node server.js`);
    } else {
      console.error(fehler);
    }
    process.exit(1);
  });

  server.listen(PORT, () => {
    console.log(`BuckSMP-Website läuft auf Port ${PORT}: http://localhost:${PORT}/`);
    console.log(`Admin-Panel: http://localhost:${PORT}/admin/`);
    if (zugang && zugang.ausUmgebung) {
      console.log('Das Admin-Passwort wurde aus ADMIN_PASSWORT übernommen.');
    }
    if (zugang && zugang.passwort) {
      if (zugang.vorgabeZuKurz) {
        console.log('ADMIN_PASSWORT ist kürzer als 8 Zeichen und wurde deshalb nicht benutzt.');
      }
      console.log('');
      console.log(`  Neues Admin-Passwort: ${zugang.passwort}`);
      console.log('  Notier es dir – es wird nur dieses eine Mal angezeigt.');
      console.log('  Ändern kannst du es im Admin-Panel unter „Einstellungen“.');
      console.log('');
    }
  });
}

starten();
