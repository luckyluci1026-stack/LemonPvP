class Fehler extends Error {
  constructor(status, meldung) {
    super(meldung);
    this.status = status;
  }
}

function jsonSenden(antwort, status, daten, kopfzeilen = {}) {
  const koerper = JSON.stringify(daten);
  antwort.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(koerper),
    'Cache-Control': 'no-store',
    ...kopfzeilen,
  });
  antwort.end(koerper);
}

function textSenden(antwort, status, text, kopfzeilen = {}) {
  antwort.writeHead(status, {
    'Content-Type': 'text/plain; charset=utf-8',
    'Content-Length': Buffer.byteLength(text),
    ...kopfzeilen,
  });
  antwort.end(text);
}

function koerperLesen(anfrage, maxBytes) {
  return new Promise((fertig, abbruch) => {
    const laenge = Number(anfrage.headers['content-length'] || 0);
    if (laenge > maxBytes) {
      abbruch(new Fehler(413, 'Die Anfrage ist zu groß.'));
      anfrage.resume();
      return;
    }
    const stuecke = [];
    let gelesen = 0;
    anfrage.on('data', stueck => {
      gelesen += stueck.length;
      if (gelesen > maxBytes) {
        abbruch(new Fehler(413, 'Die Anfrage ist zu groß.'));
        anfrage.destroy();
        return;
      }
      stuecke.push(stueck);
    });
    anfrage.on('end', () => fertig(Buffer.concat(stuecke).toString('utf8')));
    anfrage.on('error', abbruch);
  });
}

async function jsonKoerper(anfrage, maxBytes = 1024 * 1024) {
  const text = await koerperLesen(anfrage, maxBytes);
  if (text.trim() === '') {
    return {};
  }
  try {
    const daten = JSON.parse(text);
    if (daten === null || typeof daten !== 'object' || Array.isArray(daten)) {
      throw new Error('kein Objekt');
    }
    return daten;
  } catch {
    throw new Fehler(400, 'Die Anfrage enthält kein gültiges JSON.');
  }
}

function cookiesLesen(anfrage) {
  const cookies = {};
  for (const teil of (anfrage.headers.cookie || '').split(';')) {
    const gleich = teil.indexOf('=');
    if (gleich > 0) {
      const name = teil.slice(0, gleich).trim();
      const wert = teil.slice(gleich + 1).trim();
      try {
        cookies[name] = decodeURIComponent(wert);
      } catch {
        cookies[name] = wert;
      }
    }
  }
  return cookies;
}

function istHttps(anfrage) {
  if (anfrage.socket.encrypted) {
    return true;
  }
  const weitergeleitet = String(anfrage.headers['x-forwarded-proto'] || '').split(',')[0].trim();
  return weitergeleitet === 'https';
}

module.exports = {
  Fehler,
  jsonSenden,
  textSenden,
  koerperLesen,
  jsonKoerper,
  cookiesLesen,
  istHttps,
};
