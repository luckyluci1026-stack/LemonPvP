/**
 * Der Webserver: alle Routen an einer Stelle.
 *
 * Zwei Regeln, die hier ueberall gelten:
 *
 *   1. Jede POST-Route prueft das CSRF-Zeichen. Ohne das koennte eine
 *      fremde Seite im Namen eines angemeldeten Admins Zahlungen
 *      eintragen oder Server loeschen.
 *   2. Preise werden nie aus dem Formular uebernommen, sondern immer neu
 *      aus preise.js gerechnet. Was der Browser schickt, ist Wunsch, kein
 *      Befehl.
 */
import { createServer } from 'node:http';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

import * as db from './db.js';
import { PAKETE, ZUSATZ, rechne, euro } from './preise.js';
import { Router, cookies, formular, sende, weiter, setzeCookie, loescheCookie,
         statisch, csrfWert, csrfNeu, csrfStimmt, esc } from './web.js';
import * as oeff from './seiten/oeffentlich.js';
import * as ks from './seiten/kunde.js';
import * as adm from './seiten/admin.js';
import * as dok from './seiten/dokumente.js';

const HIER = dirname(fileURLToPath(import.meta.url));
const OEFFENTLICH = join(HIER, '..', 'oeffentlich');

/** Aus einem Formular die z_-Felder als { zusatzId: menge } holen. */
function zusatzAus(daten) {
  const raus = {};
  for (const [schluessel, wert] of Object.entries(daten)) {
    if (!schluessel.startsWith('z_')) continue;
    const id = schluessel.slice(2);
    if (!ZUSATZ[id]) continue;
    const menge = ZUSATZ[id].menge
      ? Math.max(0, Math.min(20, Math.floor(Number(wert) || 0)))
      : (wert ? 1 : 0);
    if (menge > 0) raus[id] = menge;
  }
  return raus;
}

const wer = (n) => n ? `${n.benutzername}` : 'unbekannt';

export function baue() {
  const r = new Router();

  // ------------------------------------------------------------ oeffentlich
  r.get('/', (c) => sende(c.antwort, oeff.start(c.nutzer)));
  r.get('/regeln', (c) => sende(c.antwort, oeff.regeln(c.nutzer)));

  r.get('/preise.json', (c) => {
    c.antwort.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    c.antwort.end(JSON.stringify({ pakete: PAKETE, zusatz: ZUSATZ }));
  });

  r.get('/konfigurator', (c) => {
    const paket = c.url.searchParams.get('paket');
    sende(c.antwort, oeff.konfigurator(c.nutzer, c.csrf,
      PAKETE[paket] ? paket : 'coal'));
  });

  r.post('/konfigurator', async (c) => {
    const d = c.daten;
    if (!PAKETE[d.paket]) {
      return sende(c.antwort, oeff.konfigurator(c.nutzer, c.csrf, 'coal',
        'Bitte ein gültiges Paket auswählen.'));
    }
    if (!d.regeln) {
      return sende(c.antwort, oeff.konfigurator(c.nutzer, c.csrf, d.paket,
        'Bitte bestätige noch die Nutzungsregeln.'));
    }
    const zusatz = zusatzAus(d);
    const id = db.bestellungAnlegen({
      kundeId: c.nutzer?.id ?? null,
      vorname: d.vorname, nachname: d.nachname, klasse: d.klasse,
      mcname: d.mcname, kontakt: d.kontakt, servername: d.servername,
      subdomain: String(d.subdomain || '').toLowerCase().replace(/[^a-z0-9-]/g, ''),
      paket: d.paket, software: d.software, zusatz, wunsch: d.wunsch,
    });
    db.protokolliere(wer(c.nutzer) || 'Gast', 'Anfrage eingegangen',
      `#${id} · ${d.servername || '?'} · ${d.paket}`);
    sende(c.antwort, oeff.danke(c.nutzer, `#${id}`, rechne(d.paket, zusatz)));
  });

  // ------------------------------------------------------------ Anmeldung
  r.get('/anmelden', (c) =>
    c.nutzer ? weiter(c.antwort, '/meine-server')
             : sende(c.antwort, oeff.anmelden(c.csrf)));

  r.post('/anmelden', async (c) => {
    const d = c.daten;
    const k = db.kundePerName(d.benutzername);
    if (!k || !db.passtPasswort(String(d.passwort || ''), k.passwort, k.salz)) {
      return sende(c.antwort, oeff.anmelden(c.csrf,
        'Benutzername oder Passwort stimmt nicht.', d.benutzername));
    }
    const token = db.anmelden(k.id);
    db.protokolliere(k.benutzername, 'Angemeldet');
    weiter(c.antwort, k.rolle === 'admin' ? '/admin' : '/meine-server',
      { 'Set-Cookie': setzeCookie('sitzung', token) });
  });

  r.get('/abmelden', (c) => {
    if (c.token) db.abmelden(c.token);
    weiter(c.antwort, '/', { 'Set-Cookie': loescheCookie('sitzung') });
  });

  // ------------------------------------------------------------ Kunde
  r.get('/meine-server', (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    sende(c.antwort, ks.meineServer(c.nutzer, db.serverVonKunde(c.nutzer.id)));
  });

  r.get('/meine-server/:id', (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const s = db.server(Number(c.werte.id));
    // Ein Kunde sieht nur seine eigenen Server - sonst reicht das Raten
    // einer Nummer, um in fremde Zahlungen zu schauen.
    if (!s || (s.kunde_id !== c.nutzer.id && c.nutzer.rolle !== 'admin')) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Diesen Server gibt es nicht.'), 404);
    }
    sende(c.antwort, ks.serverDetail(c.nutzer, s));
  });

  // ------------------------------------------------------------ Verwaltung
  const nurAdmin = (fn) => (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    if (c.nutzer.rolle !== 'admin') {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Dafür fehlt dir die Berechtigung.'), 403);
    }
    return fn(c);
  };

  r.get('/admin', nurAdmin((c) => sende(c.antwort, adm.uebersicht(
    c.nutzer, c.csrf, db.alleServer(true), db.kunden(), db.bestellungen(true)))));

  r.get('/admin/kunden', nurAdmin((c) => sende(c.antwort,
    adm.kundenSeite(c.nutzer, c.csrf, db.kunden(), db.alleServer(true),
      c.url.searchParams.get('ok') || ''))));

  r.post('/admin/kunden', nurAdmin(async (c) => {
    const d = c.daten;
    const name = String(d.benutzername || '').toLowerCase().trim();
    if (!name || String(d.passwort || '').length < 8) {
      return sende(c.antwort, adm.kundenSeite(c.nutzer, c.csrf, db.kunden(),
        db.alleServer(true), 'Benutzername fehlt oder Passwort ist zu kurz.'));
    }
    if (db.kundePerName(name)) {
      return sende(c.antwort, adm.kundenSeite(c.nutzer, c.csrf, db.kunden(),
        db.alleServer(true), `Den Benutzernamen "${name}" gibt es schon.`));
    }
    const id = db.kundeAnlegen({ ...d, benutzername: name, rolle: 'kunde' });
    db.protokolliere(wer(c.nutzer), 'Kunde angelegt', `${name} (#${id})`);
    weiter(c.antwort, '/admin/kunden?ok=' + encodeURIComponent(`Kunde ${name} angelegt.`));
  }));

  r.get('/admin/kunde/:id', nurAdmin((c) => {
    const k = db.kunde(Number(c.werte.id));
    if (!k) return sende(c.antwort, fehlerSeite(c.nutzer, 'Kunde nicht gefunden.'), 404);
    sende(c.antwort, adm.kundeSeite(c.nutzer, c.csrf, k, db.serverVonKunde(k.id),
      c.url.searchParams.get('ok') || ''));
  }));

  r.post('/admin/kunde/:id', nurAdmin(async (c) => {
    const d = c.daten;
    const id = Number(c.werte.id);
    db.kundeAendern(id, d);
    db.protokolliere(wer(c.nutzer), 'Kundendaten geändert', `#${id}`);
    weiter(c.antwort, `/admin/kunde/${id}?ok=Gespeichert.`);
  }));

  r.post('/admin/kunde/:id/passwort', nurAdmin(async (c) => {
    const d = c.daten;
    const id = Number(c.werte.id);
    if (String(d.passwort || '').length < 8) {
      return weiter(c.antwort, `/admin/kunde/${id}?ok=Passwort zu kurz.`);
    }
    db.passwortSetzen(id, d.passwort);
    db.protokolliere(wer(c.nutzer), 'Passwort zurückgesetzt', `Kunde #${id}`);
    weiter(c.antwort, `/admin/kunde/${id}?ok=Passwort gesetzt, Kunde ist überall abgemeldet.`);
  }));

  r.get('/admin/server/neu', nurAdmin((c) =>
    sende(c.antwort, adm.serverBearbeiten(c.nutzer, c.csrf, null, db.kunden()))));

  r.post('/admin/server/neu', nurAdmin(async (c) => {
    const d = c.daten;
    if (!PAKETE[d.paket] || !d.name || !d.kundeId) {
      return sende(c.antwort, adm.serverBearbeiten(c.nutzer, c.csrf, null, db.kunden(),
        'Kunde, Name und Paket werden gebraucht.'));
    }
    const id = db.serverAnlegen({
      kundeId: Number(d.kundeId), name: d.name,
      subdomain: String(d.subdomain || '').toLowerCase().replace(/[^a-z0-9-]/g, ''),
      paket: d.paket, software: d.software, notiz: d.notiz, zusatz: zusatzAus(d),
    });
    db.protokolliere(wer(c.nutzer), 'Server angelegt', `${d.name} (#${id})`);
    weiter(c.antwort, `/admin/server/${id}`);
  }));

  r.get('/admin/server/:id', nurAdmin((c) => {
    const s = db.server(Number(c.werte.id));
    if (!s) return sende(c.antwort, fehlerSeite(c.nutzer, 'Server nicht gefunden.'), 404);
    sende(c.antwort, adm.serverBearbeiten(c.nutzer, c.csrf, s, db.kunden(),
      c.url.searchParams.get('ok') || ''));
  }));

  r.post('/admin/server/:id', nurAdmin(async (c) => {
    const d = c.daten;
    const id = Number(c.werte.id);
    db.serverAendern(id, {
      name: d.name,
      subdomain: String(d.subdomain || '').toLowerCase().replace(/[^a-z0-9-]/g, ''),
      paket: PAKETE[d.paket] ? d.paket : undefined,
      software: d.software, status: d.status, notiz: d.notiz,
    });
    db.zusatzSetzen(id, zusatzAus(d));
    db.protokolliere(wer(c.nutzer), 'Server geändert', `${d.name} (#${id})`);
    weiter(c.antwort, `/admin/server/${id}?ok=Gespeichert.`);
  }));

  r.post('/admin/server/:id/zahlung', nurAdmin(async (c) => {
    const d = c.daten;
    const id = Number(c.werte.id);
    const betrag = Math.max(0, Number(d.betrag) || 0);
    const tage = Math.max(1, Math.min(365, Math.floor(Number(d.tage) || 30)));
    const zeitraum = db.zahlungEintragen({
      serverId: id, betrag, tage, art: d.art,
      kassiertVon: d.kassiertVon || c.nutzer.benutzername, notiz: d.notiz,
    });
    if (!zeitraum) return weiter(c.antwort, '/admin');
    db.protokolliere(wer(c.nutzer), 'Zahlung eingetragen',
      `Server #${id} · ${euro(betrag)} · ${zeitraum.von} → ${zeitraum.bis}`);
    weiter(c.antwort, `/admin/server/${id}?ok=` + encodeURIComponent(
      `${euro(betrag)} eingetragen, bezahlt bis ${zeitraum.bis}.`));
  }));

  r.post('/admin/server/:id/loeschen', nurAdmin((c) => {
    const id = Number(c.werte.id);
    const s = db.server(id);
    db.serverLoeschen(id);
    db.protokolliere(wer(c.nutzer), 'Server gelöscht', `${s?.name || '?'} (#${id})`);
    weiter(c.antwort, `/admin/server/${id}?ok=` + encodeURIComponent(
      'Als gelöscht gekennzeichnet. Die Löschbestätigung kannst du jetzt drucken.'));
  }));

  r.get('/admin/anfrage/:id', nurAdmin((c) => {
    const b = db.bestellung(Number(c.werte.id));
    if (!b) return sende(c.antwort, fehlerSeite(c.nutzer, 'Anfrage nicht gefunden.'), 404);
    sende(c.antwort, adm.anfrageSeite(c.nutzer, c.csrf, b, db.kunden()));
  }));

  r.post('/admin/anfrage/:id/annehmen', nurAdmin(async (c) => {
    const d = c.daten;
    const b = db.bestellung(Number(c.werte.id));
    if (!b) return weiter(c.antwort, '/admin');

    let kundeId = Number(d.kundeId) || 0;
    if (!kundeId) {
      const name = String(d.benutzername || '').toLowerCase().trim();
      if (!name || String(d.passwort || '').length < 8 || db.kundePerName(name)) {
        return weiter(c.antwort, `/admin/anfrage/${b.id}`);
      }
      kundeId = db.kundeAnlegen({
        benutzername: name, passwort: d.passwort, vorname: b.vorname,
        nachname: b.nachname, klasse: b.klasse, mcname: b.mcname, kontakt: b.kontakt,
      });
      db.protokolliere(wer(c.nutzer), 'Kunde aus Anfrage angelegt', `${name} (#${kundeId})`);
    }

    const serverId = db.serverAnlegen({
      kundeId, name: b.servername || 'Server', subdomain: b.subdomain,
      paket: b.paket, software: b.software, notiz: b.wunsch, zusatz: b.zusatz,
    });
    db.bestellungStatus(b.id, 'angenommen');
    db.protokolliere(wer(c.nutzer), 'Anfrage angenommen',
      `#${b.id} → Server #${serverId}`);
    weiter(c.antwort, `/admin/server/${serverId}?ok=` + encodeURIComponent(
      'Server aus der Anfrage angelegt. Jetzt Bestellbogen drucken und Zahlung eintragen.'));
  }));

  r.post('/admin/anfrage/:id/ablehnen', nurAdmin((c) => {
    db.bestellungStatus(Number(c.werte.id), 'abgelehnt');
    db.protokolliere(wer(c.nutzer), 'Anfrage abgelehnt', `#${c.werte.id}`);
    weiter(c.antwort, '/admin');
  }));

  r.get('/admin/zahlungen', nurAdmin((c) => sende(c.antwort, adm.zahlungenSeite(c.nutzer))));
  r.get('/admin/protokoll', nurAdmin((c) => sende(c.antwort, adm.protokollSeite(c.nutzer))));

  // ------------------------------------------------------------ Dokumente
  const dokument = (bauer) => nurAdmin((c) => {
    const s = db.server(Number(c.werte.id));
    if (!s) return sende(c.antwort, fehlerSeite(c.nutzer, 'Server nicht gefunden.'), 404);
    sende(c.antwort, bauer(c.nutzer, s, db.kunde(s.kunde_id)));
  });
  r.get('/admin/server/:id/bestellbogen', dokument(dok.bestellbogen));
  r.get('/admin/server/:id/loeschbestaetigung', dokument(dok.loeschbestaetigung));

  return r;
}

function fehlerSeite(nutzer, text) {
  return `<!DOCTYPE html><html lang="de"><head>
    <meta charset="utf-8"><title>Fehler · Lemon Hosting</title>
    <link rel="stylesheet" href="/stil.css"></head><body>
    <main><div class="eng" style="margin:4rem auto">
      <h1>Das hat nicht geklappt</h1>
      <p class="leise abstand">${esc(text)}</p>
      <a class="knopf abstand" href="/">Zur Startseite</a>
    </div></main></body></html>`;
}

export function starte(port = 3000, datenbank = 'daten/portal.db') {
  db.oeffne(datenbank);
  const router = baue();

  const server = createServer(async (anfrage, antwort) => {
    try {
      const url = new URL(anfrage.url, `http://${anfrage.headers.host || 'localhost'}`);

      if (anfrage.method === 'GET' && await statisch(antwort, OEFFENTLICH, url.pathname)) {
        return;
      }

      const token = cookies(anfrage).sitzung;
      const nutzer = db.sitzung(token);

      // Jeder Besucher bekommt ein CSRF-Cookie, auch ohne Anmeldung -
      // sonst liesse sich kein einziges Formular abschicken.
      let csrf = csrfWert(anfrage);
      let csrfCookie = null;
      if (!csrf) {
        csrf = csrfNeu();
        csrfCookie = `csrf=${csrf}; Path=/; SameSite=Lax; Max-Age=86400`;
        antwort.setHeader('Set-Cookie', csrfCookie);
      }

      const treffer = router.finde(anfrage.method, url.pathname);
      if (!treffer) {
        return sende(antwort, fehlerSeite(nutzer, 'Diese Seite gibt es nicht.'), 404);
      }

      // Jede Aenderung braucht das Zeichen der eigenen Sitzung.
      let gelesen = {};
      if (anfrage.method === 'POST') {
        const daten = await formular(anfrage);
        if (!csrfStimmt(csrf, daten.csrf)) {
          return sende(antwort, fehlerSeite(nutzer,
            'Das Formular ist abgelaufen. Lade die Seite neu und versuch es noch einmal.'), 400);
        }
        // Der Body ist verbraucht - die Handler bekommen ihn fertig gelesen
        // ueber c.daten. Ein zweites formular() wuerde nur einen leeren
        // Stream vorfinden und stillschweigend leere Felder liefern.
        gelesen = daten;
      }

      await treffer.handler({ anfrage, antwort, url, werte: treffer.werte,
                              nutzer, token, csrf, daten: gelesen });
    } catch (fehler) {
      console.error('Fehler bei', anfrage.method, anfrage.url, '\n', fehler);
      if (!antwort.headersSent) sende(antwort, fehlerSeite(null, 'Interner Fehler.'), 500);
    }
  });

  // Ein belegter Port ist der haeufigste Stolperstein beim ersten
  // Ausprobieren - meistens laeuft das Portal schon in einem anderen
  // Fenster. Ein Stacktrace hilft da niemandem weiter.
  server.on('error', (fehler) => {
    if (fehler.code === 'EADDRINUSE') {
      console.error(`
  Port ${port} ist schon belegt.

  Läuft das Portal vielleicht noch in einem anderen Fenster? Dann dort mit
  Strg+C beenden. Oder nimm einfach einen anderen Port:

      PORT=${port + 1} node start.js
`);
    } else if (fehler.code === 'EACCES') {
      console.error(`
  Port ${port} darf nicht benutzt werden - Ports unter 1024 sind dem
  System vorbehalten. Nimm einen ab 1024:

      PORT=3000 node start.js
`);
    } else {
      console.error('  Der Server konnte nicht starten:', fehler.message);
    }
    process.exit(1);
  });

  server.listen(port, () => {
    console.log(`\n  🍋 Lemon Hosting Kundenportal`);
    console.log(`     läuft auf  http://localhost:${port}`);
    console.log(`     Datenbank  ${datenbank}\n`);
  });
  return server;
}
