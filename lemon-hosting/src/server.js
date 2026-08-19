/**
 * Der Webserver: alle Routen an einer Stelle.
 *
 * Drei Regeln, die hier ueberall gelten:
 *
 *   1. Jede POST-Route prueft das CSRF-Zeichen. Ohne das koennte eine
 *      fremde Seite im Namen eines Angemeldeten dessen Server stoppen
 *      oder Dateien loeschen.
 *   2. Preise werden nie aus dem Formular uebernommen, sondern immer neu
 *      aus preise.js gerechnet. Was der Browser schickt, ist Wunsch, kein
 *      Befehl.
 *   3. Jede Panel-Route geht durch `meinServer()`. Wer /panel/2 aufruft,
 *      ohne dass ihm Server 2 gehoert, kommt nicht rein - sonst saesse
 *      man mit einem geratenen Link in einer fremden Konsole.
 */
import { createServer } from 'node:http';
import { createReadStream, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, basename } from 'node:path';

import * as db from './db.js';
import { PAKETE, ZUSATZ, rechne } from './preise.js';
import { Router, cookies, formular, sende, weiter, setzeCookie, loescheCookie,
         statisch, csrfWert, csrfNeu, csrfStimmt, esc } from './web.js';
import * as prozess from './panel.js';
import * as dat from './dateien.js';
import * as sich from './sicherung.js';
import * as plan from './zeitplan.js';
import * as kat from './katalog.js';
import * as docker from './docker.js';
import * as wo from './wo.js';
import * as arten from './arten.js';
import * as sb from './start.js';
import * as api from './api.js';
import * as fern from './fern.js';
import * as umzug from './umzug.js';
import * as zwei from './zweifach.js';
import * as oeff from './seiten/oeffentlich.js';
import * as ks from './seiten/kunde.js';
import * as adm from './seiten/admin.js';
import * as pnl from './seiten/panel.js';
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

/**
 * Was ein Unterbenutzer duerfen kann.
 *
 * Absichtlich grob: Wer die Konsole hat, kann ohnehin `op` tippen. Fein
 * abgestufte Rechte gaeben nur ein Gefuehl von Sicherheit, das nicht
 * traegt. Was wirklich zaehlt: Der Besitzer bleibt allein zustaendig fuer
 * Freigaben und sieht als Einziger, was der Server kostet.
 */
const RECHTE = {
  konsole: 'Konsole lesen und Befehle schicken',
  steuern: 'Starten, stoppen, neu starten',
  dateien: 'Dateien ansehen und bearbeiten',
  backups: 'Backups anlegen und zurückspielen',
  plugins: 'Plugins und Serversoftware ändern',
};
export const RECHTE_LISTE = RECHTE;

const rechteAus = (daten) =>
  Object.keys(RECHTE).filter((r) => daten['r_' + r]);

/**
 * Eine hochgeladene Datei direkt zum Daemon weiterreichen.
 *
 * Der Datenstrom laeuft durch, ohne dass die Datei beim Portal
 * zwischenlandet - eine 55-MB-Jar belegt hier also keinen Speicher und
 * keine Platte.
 */
async function hochladenWeiter(c, s) {
  const knoten = wo.knotenVon(s);
  if (!knoten) return 'Der Knoten zu diesem Server ist nicht eingetragen.';
  try {
    const antwort = await fern.ruf(knoten,
      `/hochladen/${s.id}?p=${encodeURIComponent(c.url.searchParams.get('p') || '')}`
      + `&name=${encodeURIComponent(c.url.searchParams.get('name') || '')}`,
      { method: 'POST', body: c.anfrage, duplex: 'half', frist: 600000,
        headers: { 'Content-Type': 'application/octet-stream' } });
    const daten = await antwort.json().catch(() => ({}));
    return antwort.ok ? null : (daten.fehler || `Knoten antwortet mit ${antwort.status}`);
  } catch (fehler) {
    return 'Der Knoten war nicht erreichbar: ' + fehler.message;
  }
}

/** Ein Backup vom Daemon holen und an den Browser durchreichen. */
async function sicherungWeiter(c, s) {
  const knoten = wo.knotenVon(s);
  if (!knoten) {
    return sende(c.antwort, fehlerSeite(c.nutzer,
      'Der Knoten zu diesem Server ist nicht eingetragen.'), 404);
  }
  const name = String(c.url.searchParams.get('f') || '');
  try {
    const antwort = await fern.ruf(knoten,
      `/sicherung/${s.id}?f=${encodeURIComponent(name)}`, { frist: 600000 });
    if (!antwort.ok) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Dieses Backup gibt es nicht.'), 404);
    }
    c.antwort.writeHead(200, {
      'Content-Type': 'application/zip',
      'Content-Disposition': `attachment; filename="${s.subdomain || 'server' + s.id}-${name}"`,
      ...(antwort.headers.get('content-length')
        ? { 'Content-Length': antwort.headers.get('content-length') } : {}),
    });
    for await (const stueck of antwort.body) {
      if (!c.antwort.write(stueck)) {
        await new Promise((f) => c.antwort.once('drain', f));
      }
    }
    c.antwort.end();
  } catch (fehler) {
    if (!c.antwort.headersSent) {
      sende(c.antwort, fehlerSeite(c.nutzer,
        'Der Knoten war nicht erreichbar: ' + fehler.message), 502);
    }
  }
  return undefined;
}

/**
 * Eine Pterodactyl-Adresse annehmen - oder verwerfen.
 *
 * Der Wert landet spaeter in einem href. Ohne diese Pruefung koennte dort
 * `javascript:...` stehen, und ein Admin haette sich selbst eine Falle
 * gestellt. Nur http und https, sonst nichts.
 */
/**
 * Einen Port aus dem Formular annehmen - oder 0 fuer "such dir einen".
 *
 * Unter 1024 duerfte der Prozess ohnehin nicht binden, ueber 65535 gibt
 * es nicht. Alles andere waere ein Server, der beim Start kommentarlos
 * scheitert.
 */
/** Eine Knotennummer, die es auch gibt. 0 heisst "dieser Rechner". */
function knotenOk(roh) {
  const n = Math.floor(Number(roh) || 0);
  return n > 0 && db.knoten(n) ? n : 0;
}

function portOk(roh) {
  const n = Math.floor(Number(roh) || 0);
  return n >= 1024 && n <= 65535 ? n : 0;
}

/**
 * Ein Image-Name, der auch einer sein kann.
 *
 * Der Wert wird an `docker run` weitergereicht. Ein Leerzeichen darin
 * waere schon eine zweite Angabe, ein fuehrender Bindestrich ein
 * zusaetzliches Argument - beides gehoert hier nicht hin.
 */
/**
 * Die Startangaben aus dem Formular pruefen.
 *
 * Gibt entweder die Felder zurueck oder einen Text, der dem Admin sagt,
 * was daran nicht geht. Ein stilles Verwerfen waere hier das
 * Unangenehmste: Man aendert eine Flagge, es steht "Gespeichert." da,
 * und beim naechsten Start laeuft trotzdem alles wie vorher.
 */
function startAus(d) {
  const jar = String(d.jar_datei || '').trim();
  if (jar && !sb.jarOk(jar)) {
    return { fehler: `„${jar}" geht als Startdatei nicht – nur ein Dateiname `
                   + 'im Serverordner, ohne Pfad.' };
  }
  const flaggen = sb.flaggenOk(d.start_flaggen);
  if (flaggen.fehler) return { fehler: flaggen.fehler };
  const befehl = sb.befehlOk(d.startbefehl);
  if (befehl.fehler) return { fehler: befehl.fehler };
  return {
    jar_datei: jar,
    // Leeres Feld heisst "keine Flaggen", nicht "Vorgaben" - dafuer gibt
    // es NULL. Sonst koennte man die Flaggen nie ganz abschalten.
    start_flaggen: d.start_flaggen === undefined ? undefined
      : (String(d.start_flaggen).trim() === '' ? null : flaggen.flaggen),
    startbefehl: befehl.befehl,
  };
}

function bildOk(roh) {
  const text = String(roh || '').trim();
  if (!text) return '';
  return /^[a-z0-9][a-z0-9._\/-]*(:[a-zA-Z0-9._-]+)?$/.test(text) ? text : '';
}

function adresseOk(roh) {
  const text = String(roh || '').trim();
  if (!text) return '';
  try {
    const u = new URL(text);
    return (u.protocol === 'http:' || u.protocol === 'https:') ? u.href : '';
  } catch {
    return '';
  }
}

export function baue() {
  const r = new Router();

  const jsonRaus = (c, status, daten) => {
    c.antwort.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
    c.antwort.end(JSON.stringify(daten));
  };

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
  r.get('/anmelden', (c) => {
    if (c.nutzer) return weiter(c.antwort, '/meine-server');
    // Wer schon halb drin ist, soll nicht wieder beim Passwort landen -
    // sonst tippt er es ein zweites Mal und wundert sich, warum er
    // danach schon wieder nach dem Code gefragt wird.
    if (db.halbeSitzung(c.token)) return weiter(c.antwort, '/anmelden/code');
    sende(c.antwort, oeff.anmelden(c.csrf));
  });

  /** Wohin es nach dem Anmelden geht. */
  const startseite = (k) => k.rolle === 'admin' ? '/admin' : '/meine-server';

  r.post('/anmelden', async (c) => {
    const d = c.daten;
    const k = db.kundePerName(d.benutzername);
    if (!k || !db.passtPasswort(String(d.passwort || ''), k.passwort, k.salz)) {
      return sende(c.antwort, oeff.anmelden(c.csrf,
        'Benutzername oder Passwort stimmt nicht.', d.benutzername));
    }

    // Mit zweitem Faktor gibt es hier noch keine richtige Sitzung,
    // sondern nur eine halbe: Sie traegt den Namen, aber keine Rechte.
    if (db.zweifachAktiv(k)) {
      const halb = db.halbAnmelden(k.id);
      return sende(c.antwort, oeff.zweiterFaktor(c.csrf), 200,
        { 'Set-Cookie': setzeCookie('sitzung', halb) });
    }

    const token = db.anmelden(k.id);
    db.protokolliere(k.benutzername, 'Angemeldet');
    weiter(c.antwort, startseite(k), { 'Set-Cookie': setzeCookie('sitzung', token) });
  });

  r.get('/anmelden/code', (c) => {
    const k = db.halbeSitzung(c.token);
    if (!k) return weiter(c.antwort, '/anmelden');
    sende(c.antwort, oeff.zweiterFaktor(c.csrf, '',
      c.url.searchParams.get('ersatz') === '1'));
  });

  r.post('/anmelden/code', async (c) => {
    const k = db.halbeSitzung(c.token);
    if (!k) {
      return sende(c.antwort, oeff.anmelden(c.csrf,
        'Das hat zu lange gedauert – bitte noch einmal anmelden.'));
    }
    const ersatz = Boolean(c.daten.ersatz);
    const getippt = String(c.daten.code || '').trim();

    if (ersatz) {
      if (!db.ersatzcodeEinloesen(k.id, zwei.ersatzHash(getippt))) {
        db.protokolliere(k.benutzername, 'Ersatzcode falsch');
        return sende(c.antwort, oeff.zweiterFaktor(c.csrf,
          'Dieser Ersatzcode stimmt nicht oder ist schon verbraucht.', true));
      }
      db.sitzungGanz(c.token);
      const rest = db.ersatzcodesOffen(k.id);
      db.protokolliere(k.benutzername, 'Mit Ersatzcode angemeldet',
        `noch ${rest} übrig`);
      return weiter(c.antwort, startseite(k));
    }

    const schritt = zwei.pruefe(k.totp_geheim, getippt);
    if (schritt === null) {
      db.protokolliere(k.benutzername, 'Code falsch');
      return sende(c.antwort, oeff.zweiterFaktor(c.csrf,
        'Der Code stimmt nicht. Geht die Uhr des Handys richtig?'));
    }
    // Derselbe Code kein zweites Mal: Wer ihn ueber die Schulter
    // abliest, sitzt in diesen dreissig Sekunden noch daneben.
    if (schritt <= k.totp_schritt) {
      return sende(c.antwort, oeff.zweiterFaktor(c.csrf,
        'Diesen Code hast du gerade schon benutzt. Warte auf den nächsten.'));
    }
    db.totpSchritt(k.id, schritt);
    db.sitzungGanz(c.token);
    db.protokolliere(k.benutzername, 'Angemeldet', 'mit zweitem Faktor');
    weiter(c.antwort, startseite(k));
  });

  r.get('/abmelden', (c) => {
    if (c.token) db.abmelden(c.token);
    weiter(c.antwort, '/', { 'Set-Cookie': loescheCookie('sitzung') });
  });

  // ------------------------------------------------------------ Sicherheit
  /**
   * Passwort und zweiter Faktor.
   *
   * Der Einrichtungsweg hat drei Schritte, und der mittlere ist der
   * Grund dafuer: Erst wird ein Geheimnis abgelegt, das noch nicht gilt,
   * dann muss ein Code daraus stimmen, und erst danach wird
   * umgeschaltet. Wer die App falsch eintraegt, merkt es also, solange
   * er noch angemeldet ist - und nicht beim naechsten Anmelden, wenn es
   * zu spaet ist.
   */
  const sicherheitsSeite = (c, extra = {}) => {
    const k = db.kunde(c.nutzer.id);
    const vorbereitet = Boolean(k.totp_geheim) && !k.totp_seit;
    sende(c.antwort, ks.sicherheit(k, c.csrf, {
      an: db.zweifachAktiv(k),
      geheim: vorbereitet ? k.totp_geheim : '',
      link: vorbereitet ? zwei.link(k.benutzername, k.totp_geheim) : '',
      offen: db.ersatzcodesOffen(k.id),
      ...extra,
    }));
  };

  r.get('/sicherheit', (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    sicherheitsSeite(c, { ok: c.url.searchParams.get('ok') || '' });
  });

  r.post('/sicherheit/vorbereiten', async (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    if (db.zweifachAktiv(c.nutzer)) return weiter(c.antwort, '/sicherheit');
    db.totpVorbereiten(c.nutzer.id, zwei.neuesGeheimnis());
    weiter(c.antwort, '/sicherheit');
  });

  r.post('/sicherheit/an', async (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const k = db.kunde(c.nutzer.id);
    if (!k.totp_geheim) return weiter(c.antwort, '/sicherheit');
    if (db.zweifachAktiv(k)) return weiter(c.antwort, '/sicherheit');

    // Auch zum EINschalten das Passwort. Wer an einem offen stehenden
    // Browser sitzt, koennte sonst seine eigene App eintragen und den
    // Besitzer aussperren - und das laesst sich, anders als alles
    // andere, was er dort anrichten koennte, nur noch ueber den Admin
    // rueckgaengig machen.
    if (!db.passtPasswort(String(c.daten.passwort || ''), k.passwort, k.salz)) {
      return sicherheitsSeite(c, { meldung: 'Das Passwort stimmt nicht.' });
    }
    if (zwei.pruefe(k.totp_geheim, c.daten.code) === null) {
      return sicherheitsSeite(c, { meldung: 'Der Code stimmt nicht. '
        + 'Steht in der App wirklich dieser Schlüssel – und geht die Uhr richtig?' });
    }
    const codes = zwei.neueErsatzcodes();
    db.zweifachAn(k.id, codes.map(zwei.ersatzHash), c.token);
    db.protokolliere(k.benutzername, 'Zwei-Faktor eingeschaltet');
    sicherheitsSeite(c, { ersatzcodes: codes,
      ok: 'Ab jetzt fragt das Portal beim Anmelden nach dem Code.' });
  });

  r.post('/sicherheit/aus', async (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const k = db.kunde(c.nutzer.id);
    if (!db.passtPasswort(String(c.daten.passwort || ''), k.passwort, k.salz)) {
      return sicherheitsSeite(c, { meldung: 'Das Passwort stimmt nicht.' });
    }
    if (zwei.pruefe(k.totp_geheim, c.daten.code) === null) {
      return sicherheitsSeite(c, { meldung: 'Der Code stimmt nicht.' });
    }
    db.zweifachAus(k.id);
    db.protokolliere(k.benutzername, 'Zwei-Faktor abgeschaltet');
    weiter(c.antwort, '/sicherheit?ok=' + encodeURIComponent(
      'Abgeschaltet. Beim Anmelden reicht jetzt wieder das Passwort.'));
  });

  r.post('/sicherheit/ersatz', async (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const k = db.kunde(c.nutzer.id);
    if (!db.zweifachAktiv(k)) return weiter(c.antwort, '/sicherheit');
    if (!db.passtPasswort(String(c.daten.passwort || ''), k.passwort, k.salz)) {
      return sicherheitsSeite(c, { meldung: 'Das Passwort stimmt nicht.' });
    }
    const codes = zwei.neueErsatzcodes();
    db.ersatzcodesSetzen(k.id, codes.map(zwei.ersatzHash));
    db.protokolliere(k.benutzername, 'Ersatzcodes erneuert');
    sicherheitsSeite(c, { ersatzcodes: codes,
      ok: 'Neue Ersatzcodes. Die alten gelten nicht mehr.' });
  });

  r.post('/sicherheit/passwort', async (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const k = db.kunde(c.nutzer.id);
    const neu = String(c.daten.neu || '');
    if (!db.passtPasswort(String(c.daten.alt || ''), k.passwort, k.salz)) {
      return sicherheitsSeite(c, { meldung: 'Das bisherige Passwort stimmt nicht.' });
    }
    if (neu.length < 8) {
      return sicherheitsSeite(c, { meldung: 'Das neue Passwort ist zu kurz – '
        + 'mindestens acht Zeichen.' });
    }
    if (neu !== String(c.daten.neu2 || '')) {
      return sicherheitsSeite(c, { meldung: 'Die beiden neuen Passwörter '
        + 'sind nicht gleich.' });
    }
    db.passwortSetzen(k.id, neu);
    db.protokolliere(k.benutzername, 'Passwort geändert');
    weiter(c.antwort, '/anmelden', { 'Set-Cookie': loescheCookie('sitzung') });
  });

  // ------------------------------------------------------------ Kunde
  r.get('/meine-server', (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    sende(c.antwort, ks.meineServer(c.nutzer, [
      ...db.serverVonKunde(c.nutzer.id),
      ...db.serverAlsUnterbenutzer(c.nutzer.id),
    ]));
  });

  r.get('/meine-server/:id', (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const s = db.server(Number(c.werte.id));
    // Was der Server kostet, geht nur den Besitzer an - ein
    // Unterbenutzer darf das Panel bedienen, nicht die Rechnung sehen.
    if (!s || (s.kunde_id !== c.nutzer.id && c.nutzer.rolle !== 'admin')) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Diesen Server gibt es nicht.'), 404);
    }
    sende(c.antwort, ks.serverDetail(c.nutzer, s));
  });


  // ------------------------------------------------------------ API
  /**
   * Der Zugang fuer Skripte.
   *
   * Kein CSRF und kein Cookie: Ein Skript hat keinen Browser, aus dem
   * heraus jemand es hereinlegen koennte. Stattdessen der Schluessel im
   * Kopf `Authorization: Bearer lemon_…`.
   *
   * Was der Schluessel darf, entscheidet derselbe Code wie im Panel -
   * er ist ein anderer Weg herein, keine Abkuerzung an den Rechten
   * vorbei.
   */
  const mitSchluessel = (fn) => async (c) => {
    const kopf = String(c.anfrage.headers.authorization || '');
    const zugang = api.pruefe(kopf.replace(/^Bearer\s+/i, ''));
    if (!zugang) {
      return jsonRaus(c, 401, { fehler: 'Kein gültiger Schlüssel. Erwartet wird '
        + 'der Kopf "Authorization: Bearer lemon_…".' });
    }
    const nutzer = db.kunde(zugang.kunde_id);
    if (!nutzer) return jsonRaus(c, 401, { fehler: 'Der Zugang gehört niemandem mehr.' });
    return fn(c, nutzer);
  };

  /** Welche Server dieser Schluessel sehen darf - eigene und geteilte. */
  const serverFuer = (nutzer) => [
    ...db.serverVonKunde(nutzer.id),
    ...db.serverAlsUnterbenutzer(nutzer.id),
  ].filter((s) => s.status !== 'geloescht');

  r.get('/api/server', mitSchluessel((c, nutzer) => {
    jsonRaus(c, 200, {
      server: serverFuer(nutzer).map((s) =>
        api.serverAls(s, wo.zustand(s), wo.knotenName(s))),
    });
  }));

  r.get('/api/server/:id', mitSchluessel((c, nutzer) => {
    const s = serverFuer(nutzer).find((x) => x.id === Number(c.werte.id));
    if (!s) return jsonRaus(c, 404, { fehler: 'Diesen Server gibt es für dich nicht.' });
    jsonRaus(c, 200, api.serverAls(s, wo.zustand(s), wo.knotenName(s)));
  }));

  r.post('/api/server/:id/:was', mitSchluessel(async (c, nutzer) => {
    const s = serverFuer(nutzer).find((x) => x.id === Number(c.werte.id));
    if (!s) return jsonRaus(c, 404, { fehler: 'Diesen Server gibt es für dich nicht.' });

    // Dieselbe Rechtepruefung wie im Panel - ein Schluessel kann nie
    // mehr als der Kunde, dem er gehoert.
    const eigen = s.kunde_id === nutzer.id || nutzer.rolle === 'admin';
    const rechte = eigen ? null : (db.rechteAn(s.id, nutzer.id) || []);
    const braucht = c.werte.was === 'befehl' ? 'konsole' : 'steuern';
    if (rechte && !rechte.includes(braucht)) {
      return jsonRaus(c, 403, { fehler: `Dafür fehlt dir das Recht „${braucht}".` });
    }
    if (s.pterodactyl) {
      return jsonRaus(c, 409, { fehler: 'Dieser Server läuft in Pterodactyl.' });
    }

    if (c.werte.was === 'befehl') {
      const fehler = await wo.befehl(s, String(c.daten.befehl || ''));
      return jsonRaus(c, fehler ? 409 : 200, { ok: !fehler, fehler: fehler || null });
    }
    if (!['start', 'stopp', 'neustart'].includes(c.werte.was)) {
      return jsonRaus(c, 404, { fehler: 'Unbekannte Aktion.' });
    }
    if (c.werte.was === 'start' && s.status !== 'aktiv') {
      return jsonRaus(c, 409, { fehler: `Der Server ist ${s.status}.` });
    }
    const fehler = await wo.aktion(s, c.werte.was);
    db.protokolliere(nutzer.benutzername + ' (API)', 'Panel: ' + c.werte.was,
      `${s.name} (#${s.id})` + (fehler ? ' – ' + fehler : ''), s.id);
    jsonRaus(c, fehler ? 409 : 200, { ok: !fehler, fehler: fehler || null });
  }));

  // -------------------------------------------------- Zugaenge verwalten
  r.get('/zugaenge', (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    sende(c.antwort, ks.zugaenge(c.nutzer, c.csrf, db.zugaengeVon(c.nutzer.id),
      c.url.searchParams.get('neu') || '', c.url.searchParams.get('ok') || ''));
  });

  r.post('/zugaenge', (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const name = String(c.daten.name || '').trim().slice(0, 60) || 'ohne Namen';
    const schluessel = api.neuerSchluessel();
    db.zugangAnlegen(c.nutzer.id, name, api.hashe(schluessel));
    db.protokolliere(wer(c.nutzer), 'API-Zugang angelegt', name);
    // Der Schluessel geht einmal ueber die Adresszeile zurueck und steht
    // danach nirgends mehr - auch nicht in der Datenbank.
    weiter(c.antwort, '/zugaenge?neu=' + encodeURIComponent(schluessel));
  });

  r.post('/zugaenge/weg', (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    db.zugangWeg(c.nutzer.id, Number(c.daten.id) || 0);
    db.protokolliere(wer(c.nutzer), 'API-Zugang gelöscht', `#${c.daten.id}`);
    weiter(c.antwort, '/zugaenge?ok=Zugang gelöscht.');
  });

  // ------------------------------------------------------------ Panel
  /**
   * Der Tuersteher fuers Panel.
   *
   * Jede einzelne Panel-Route geht hier durch. Ohne diese Pruefung
   * koennte jeder Angemeldete /panel/2 aufrufen und saesse in einer
   * fremden Serverkonsole - mit Dateizugriff obendrauf. Das ist die
   * Stelle, an der ein Fehler richtig weh taete, deshalb steht sie an
   * genau einem Ort und nicht in jedem Handler nachgebaut.
   */
  const meinServer = (recht) => (fn) => (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const s = db.server(Number(c.werte.id));
    if (!s) return sende(c.antwort, fehlerSeite(c.nutzer, 'Diesen Server gibt es nicht.'), 404);

    const besitzer = s.kunde_id === c.nutzer.id || c.nutzer.rolle === 'admin';
    if (!besitzer) {
      // Ein Unterbenutzer darf herein, aber nur so weit, wie der Besitzer
      // ihn gelassen hat. Wer gar nicht eingetragen ist, bekommt 404 und
      // nicht 403 - er soll nicht einmal erfahren, dass es den Server gibt.
      const rechte = db.rechteAn(s.id, c.nutzer.id);
      if (!rechte) {
        return sende(c.antwort, fehlerSeite(c.nutzer, 'Diesen Server gibt es nicht.'), 404);
      }
      if (recht && !rechte.includes(recht)) {
        return sende(c.antwort, fehlerSeite(c.nutzer,
          'Dafür hat dich der Besitzer dieses Servers nicht freigeschaltet.'), 403);
      }
      c.rechte = rechte;
    }
    c.besitzer = besitzer;
    return fn(c, s);
  };

  /** Nur der Besitzer (oder ein Admin) - fuer Freigaben. */
  const nurBesitzer = (fn) => meinServer(null)((c, s) =>
    c.besitzer ? fn(c, s)
      : sende(c.antwort, fehlerSeite(c.nutzer,
          'Nur der Besitzer dieses Servers kann das.'), 403));

  /**
   * Wie oben, aber nur fuer Server, die das Portal selbst betreibt.
   *
   * Bei einem Server in Pterodactyl gibt es hier nichts zu starten und
   * keine Dateien zu bearbeiten - das macht Pterodactyl. Zwei Stellen,
   * die denselben Server anfassen duerfen, waeren ein Rezept fuer
   * kaputte Welten.
   */
  const eigenerServer = (recht) => (fn) => meinServer(recht)((c, s) => {
    if (s.pterodactyl) {
      return sende(c.antwort, fehlerSeite(c.nutzer,
        'Dieser Server läuft in Pterodactyl und wird dort verwaltet.'), 409);
    }
    return fn(c, s);
  });

  const zumPanel = (c, s, meldung) => weiter(c.antwort,
    `/panel/${s.id}` + (meldung ? '?m=' + encodeURIComponent(meldung) : ''));

  const zuDateien = (c, s, pfad, meldung = '', gut = false) => weiter(c.antwort,
    `/panel/${s.id}/dateien?p=${encodeURIComponent(pfad)}`
    + (meldung ? `&${gut ? 'ok' : 'm'}=` + encodeURIComponent(meldung) : ''));


  r.get('/panel/:id', meinServer(null)(async (c, s) => {
    if (s.pterodactyl) {
      return sende(c.antwort, pnl.fremdesPanel(c.nutzer, s, s.pterodactyl));
    }
    const gut = c.url.searchParams.get('ok') || '';
    // Die drei Abfragen gehen bei einem entfernten Server ueber die
    // Leitung - also nebeneinander statt hintereinander, sonst wartet
    // die Seite dreimal.
    const [plugins, sicherungen, belegt, versionen] = await Promise.all([
      wo.plugins(s), wo.sicherungen(s), wo.belegung(s),
      s.art ? wo.versionen(s, s.art) : Promise.resolve(null),
    ]);
    sende(c.antwort, pnl.panel(c.nutzer, s, wo.zustand(s), c.csrf,
      belegt, gut || c.url.searchParams.get('m') || '', Boolean(gut),
      sicherungen, plugins, wo.knotenName(s), versionen,
      { besitzer: c.besitzer, rechte: c.rechte || null,
        freigaben: c.besitzer ? db.unterbenutzer(s.id) : [], moeglich: RECHTE },
      db.protokollVonServer(s.id, 25)));
  }));

  r.post('/panel/:id/aktion', eigenerServer('steuern')(async (c, s) => {
    const was = c.daten.was;

    // Ein archivierter oder geloeschter Server startet nicht. Stoppen
    // darf man ihn trotzdem - sonst liefe ein gerade archivierter Server
    // ewig weiter.
    if (was === 'start' && s.status !== 'aktiv') {
      return zumPanel(c, s, `Dieser Server ist ${s.status} und lässt sich nicht starten.`);
    }

    const fehler = await wo.aktion(s, was);
    db.protokolliere(wer(c.nutzer),
      was === 'eula' ? 'EULA angenommen' : 'Panel: ' + was,
      `${s.name} (#${s.id})` + (fehler ? ' – ' + fehler : ''));
    zumPanel(c, s, fehler);
  }));

  r.post('/panel/:id/befehl', eigenerServer('konsole')(async (c, s) => {
    const text = String(c.daten.befehl || '');
    const fehler = await wo.befehl(s, text);
    if (!fehler) db.protokolliere(wer(c.nutzer), 'Konsolenbefehl', `#${s.id} · ${text}`, s.id);
    jsonRaus(c, fehler ? 409 : 200, { ok: !fehler, fehler });
  }));

  /**
   * Die Konsole als Server-Sent Events.
   *
   * Bleibt offen, solange das Browserfenster offen ist, und schiebt jede
   * neue Zeile durch. Websockets waeren die andere Moeglichkeit - dafuer
   * braeuchte es aber eine Bibliothek und einen zweiten Protokollpfad,
   * und geschickt wird ohnehin nur in eine Richtung.
   */
  r.get('/panel/:id/konsole', eigenerServer('konsole')(async (c, s) => {
    const a = c.antwort;

    // Liegt der Server woanders, reicht das Portal den Strom des Daemons
    // durch. Der Browser redet damit weiterhin nur mit dem Portal - der
    // Daemon muss gar nicht von aussen erreichbar sein.
    if (wo.istFern(s)) {
      const knoten = wo.knotenVon(s);
      if (!knoten) {
        return sende(c.antwort, fehlerSeite(c.nutzer,
          'Der Knoten zu diesem Server ist nicht eingetragen.'), 404);
      }
      a.writeHead(200, {
        'Content-Type': 'text/event-stream; charset=utf-8',
        'Cache-Control': 'no-cache, no-transform',
        Connection: 'keep-alive',
        'X-Accel-Buffering': 'no',
      });
      const abbruch = new AbortController();
      c.anfrage.on('close', () => abbruch.abort());
      try {
        const strom = await fern.ruf(knoten, `/konsole/${s.id}`,
          { signal: abbruch.signal, frist: 0 });
        for await (const stueck of strom.body) {
          if (!a.write(stueck)) await new Promise((f) => a.once('drain', f));
        }
      } catch {
        // Verbindung weg oder Knoten aus - der Browser baut von selbst neu auf.
        try {
          a.write('event: zeile\ndata: '
            + JSON.stringify({ art: 'fehler', zeit: Date.now(),
                text: '[Panel] Der Knoten ist gerade nicht erreichbar.' }) + '\n\n');
        } catch { /* schon zu */ }
      }
      return a.end();
    }
    a.writeHead(200, {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-transform',
      Connection: 'keep-alive',
      // Ohne das puffert ein davorstehendes nginx die Antwort, und es
      // kaeme kein einziges Ereignis an, bis der Server fertig ist.
      'X-Accel-Buffering': 'no',
    });

    const schick = (art, daten) => {
      try { a.write(`event: ${art}\ndata: ${JSON.stringify(daten)}\n\n`); }
      catch { /* Fenster ist weg */ }
    };

    schick('verlauf', prozess.konsole(s.id));
    schick('status', prozess.status(s.id));
    const abmelden = prozess.hoereZu(s.id, (zeile) => schick('zeile', zeile));

    // Der Takt haelt die Leitung wach und bringt nebenbei Knoepfe und
    // Laufzeit auf Stand, ohne dass jemand neu laden muss.
    const takt = setInterval(() => schick('status', prozess.status(s.id)), 4000);
    const schluss = () => { clearInterval(takt); abmelden(); };
    c.anfrage.on('close', schluss);
    c.anfrage.on('error', schluss);
    return undefined;
  }));

  // ------------------------------------------------------------ Dateien
  r.get('/panel/:id/dateien', eigenerServer('dateien')(async (c, s) => {
    const pfad = dat.saeubere(c.url.searchParams.get('p'));
    const eintraege = await wo.liste(s, pfad);
    if (!eintraege) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Diesen Ordner gibt es nicht.'), 404);
    }
    const ok = c.url.searchParams.get('ok') || '';
    sende(c.antwort, pnl.dateien(c.nutzer, s, pfad, eintraege, c.csrf,
      ok || c.url.searchParams.get('m') || '', Boolean(ok)));
  }));

  r.get('/panel/:id/bearbeiten', eigenerServer('dateien')(async (c, s) => {
    const pfad = dat.saeubere(c.url.searchParams.get('p'));
    const inhalt = await wo.lies(s, pfad);
    if (inhalt === null) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Diese Datei lässt sich hier nicht '
        + 'öffnen – sie ist zu groß oder kein Text.'), 404);
    }
    sende(c.antwort, pnl.bearbeiten(c.nutzer, s, pfad, inhalt, c.csrf));
  }));

  r.post('/panel/:id/speichern', eigenerServer('dateien')(async (c, s) => {
    const pfad = dat.saeubere(c.daten.p);
    const fehler = await wo.schreib(s, pfad, c.daten.inhalt ?? '');
    if (fehler) {
      return sende(c.antwort, pnl.bearbeiten(c.nutzer, s, pfad,
        String(c.daten.inhalt ?? ''), c.csrf, fehler));
    }
    db.protokolliere(wer(c.nutzer), 'Datei gespeichert', `#${s.id} · ${pfad}`, s.id);
    zuDateien(c, s, pfad.split('/').slice(0, -1).join('/'), `${pfad} gespeichert.`, true);
  }));

  r.post('/panel/:id/loeschen', eigenerServer('dateien')(async (c, s) => {
    const pfad = dat.saeubere(c.daten.p);
    const fehler = await wo.loesche(s, pfad);
    if (!fehler) db.protokolliere(wer(c.nutzer), 'Datei gelöscht', `#${s.id} · ${pfad}`, s.id);
    zuDateien(c, s, pfad.split('/').slice(0, -1).join('/'),
      fehler || `${pfad} gelöscht.`, !fehler);
  }));

  r.post('/panel/:id/neu', eigenerServer('dateien')(async (c, s) => {
    const pfad = dat.saeubere(c.daten.p);
    const name = dat.nameOk(c.daten.name);
    if (!name) return zuDateien(c, s, pfad, 'Der Dateiname geht so nicht.');
    const ziel = pfad ? pfad + '/' + name : name;
    const fehler = await wo.schreib(s, ziel, c.daten.inhalt ?? '');
    if (!fehler) db.protokolliere(wer(c.nutzer), 'Datei angelegt', `#${s.id} · ${ziel}`, s.id);
    zuDateien(c, s, pfad, fehler || `${name} angelegt.`, !fehler);
  }));

  r.post('/panel/:id/ordner', eigenerServer('dateien')(async (c, s) => {
    const pfad = dat.saeubere(c.daten.p);
    const name = dat.nameOk(c.daten.name);
    if (!name) return zuDateien(c, s, pfad, 'Der Ordnername geht so nicht.');
    const fehler = await wo.neuerOrdner(s, pfad ? pfad + '/' + name : name);
    zuDateien(c, s, pfad, fehler || `Ordner ${name} angelegt.`, !fehler);
  }));

  /**
   * Datei hochladen - roher Body, kein Formular.
   *
   * Deshalb `postRoh`: der Handler bekommt die Anfrage ungelesen und
   * leitet sie direkt auf die Platte weiter. Sonst muesste eine
   * 55-MB-Jar erst komplett in den Arbeitsspeicher.
   */
  r.postRoh('/panel/:id/hochladen', eigenerServer('dateien')(async (c, s) => {
    // Bei einem entfernten Server geht der Datenstrom direkt weiter zum
    // Daemon - die Datei liegt also nie zwischendurch beim Portal.
    const fehler = wo.istFern(s)
      ? await hochladenWeiter(c, s)
      : await dat.nimmDatei(s.id, c.url.searchParams.get('p'),
          c.url.searchParams.get('name'), c.anfrage);
    if (fehler) return jsonRaus(c, 400, { ok: false, fehler });
    db.protokolliere(wer(c.nutzer), 'Datei hochgeladen',
      `#${s.id} · ${c.url.searchParams.get('name')}`, s.id);
    jsonRaus(c, 200, { ok: true });
  }));


  // -------------------------------------------------- Zeitplan und Backups
  const zumPanelGut = (c, s, meldung) => weiter(c.antwort,
    `/panel/${s.id}?ok=` + encodeURIComponent(meldung));

  /**
   * Serverart auswaehlen oder installieren.
   *
   * Das Formular schickt sich beim Wechsel der Art selbst ab - dann steht
   * nur die neue Art drin und es wird gespeichert, damit die
   * Versionsliste dazu geladen werden kann. Erst der Knopf installiert
   * wirklich.
   */
  r.post('/panel/:id/software', eigenerServer('plugins')(async (c, s) => {
    const art = arten.artOk(c.daten.art);
    if (!art) {
      db.serverAendern(s.id, { art: '' });
      return zumPanel(c, s);
    }
    if (art !== s.art) db.serverAendern(s.id, { art });
    if (c.daten.was !== 'installieren') return zumPanel(c, s);

    const version = arten.versionOk(c.daten.version);
    if (!version) return zumPanel(c, s, 'Bitte eine Version auswählen.');

    const ergebnis = await wo.installiereArt(s, art, version);
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.serverAendern(s.id, { art, mc_version: version });
    db.protokolliere(wer(c.nutzer), 'Serversoftware installiert',
      `#${s.id} · ${ergebnis.art} ${ergebnis.version}`, s.id);
    zumPanelGut(c, s, `${ergebnis.art} ${ergebnis.version} installiert`
      + ` (${Math.round(ergebnis.groesse / 1024 / 1024)} MB).`
      + (prozess.laeuft(s.id) ? ' Wirkt beim nächsten Neustart.' : ''));
  }));

  r.post('/panel/:id/plugin/installieren', eigenerServer('plugins')(async (c, s) => {
    const ergebnis = await wo.plugin(s, c.daten.datei, 'rein');
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.protokolliere(wer(c.nutzer), 'Plugin installiert',
      `#${s.id} · ${c.daten.datei}` + (ergebnis.ersetzt ? ` (ersetzt ${ergebnis.ersetzt})` : ''));
    zumPanelGut(c, s, `${ergebnis.name} installiert – beim nächsten Neustart ist es da.`);
  }));

  r.post('/panel/:id/plugin/entfernen', eigenerServer('plugins')(async (c, s) => {
    const ergebnis = await wo.plugin(s, c.daten.datei, 'raus');
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.protokolliere(wer(c.nutzer), 'Plugin entfernt', `#${s.id} · ${c.daten.datei}`, s.id);
    zumPanelGut(c, s, `${ergebnis.name} entfernt – beim nächsten Neustart ist es weg.`);
  }));

  /**
   * Weitere Ports.
   *
   * Gehoert dem Besitzer, nicht dem Unterbenutzer: Ein Port ist eine
   * Tuer nach aussen, und wer sie aufmacht, sollte auch fuer den Server
   * geradestehen.
   */
  r.post('/panel/:id/port', nurBesitzer((c, s) => {
    const port = portOk(c.daten.port);
    if (!port) {
      return zumPanel(c, s, 'Ein Port zwischen 1024 und 65535, bitte.');
    }
    const protokoll = ['tcp', 'udp', 'beide'].includes(c.daten.protokoll)
      ? c.daten.protokoll : 'beide';
    const fehler = db.portDazu(s.id, port, protokoll, String(c.daten.notiz || '').slice(0, 60));
    if (fehler) return zumPanel(c, s, fehler);
    db.protokolliere(wer(c.nutzer), 'Port geöffnet', `#${s.id} · ${port}/${protokoll}`, s.id);
    zumPanelGut(c, s, `Port ${port} ist beim nächsten Start offen.`);
  }));

  r.post('/panel/:id/port-weg', nurBesitzer((c, s) => {
    db.portWeg(s.id, Number(c.daten.portId) || 0);
    db.protokolliere(wer(c.nutzer), 'Port geschlossen', `#${s.id}`, s.id);
    zumPanelGut(c, s, 'Port entfernt – wirkt beim nächsten Start.');
  }));

  r.post('/panel/:id/zeitplan', eigenerServer('steuern')((c, s) => {
    const neustart = plan.zeitOk(c.daten.neustart_um);
    const sicherung = plan.zeitOk(c.daten.sicherung_um);
    db.serverAendern(s.id, { neustart_um: neustart, sicherung_um: sicherung });
    db.protokolliere(wer(c.nutzer), 'Zeitplan geändert',
      `#${s.id} · Neustart ${neustart || '–'} · Backup ${sicherung || '–'}`, s.id);
    zumPanelGut(c, s, 'Zeitplan gespeichert.');
  }));

  r.post('/panel/:id/sicherung', eigenerServer('backups')(async (c, s) => {
    const ergebnis = await wo.sicherungAnlegen(s);
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.protokolliere(wer(c.nutzer), 'Backup angelegt',
      `#${s.id} · ${ergebnis.name} · ${ergebnis.groesse}`, s.id);
    zumPanelGut(c, s, `Backup angelegt: ${ergebnis.dateien} Dateien, `
      + `${ergebnis.groesse}.` + (ergebnis.warnung ? ' ' + ergebnis.warnung : ''));
  }));

  /**
   * Ein Backup herunterladen.
   *
   * Der Dateiname kommt aus der URL, muss aber dem Zeitstempelmuster
   * entsprechen - alles andere findet `pfadVon` gar nicht erst. Damit
   * fuehrt kein Umweg ueber diesen Namen an eine andere Datei.
   */
  r.get('/panel/:id/sicherung/laden', eigenerServer('backups')(async (c, s) => {
    if (wo.istFern(s)) return sicherungWeiter(c, s);
    const datei = sich.pfadVon(s.id, c.url.searchParams.get('f'));
    if (!datei) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Dieses Backup gibt es nicht.'), 404);
    }
    const name = `${s.subdomain || 'server' + s.id}-${basename(datei)}`;
    c.antwort.writeHead(200, {
      'Content-Type': 'application/zip',
      'Content-Length': statSync(datei).size,
      'Content-Disposition': `attachment; filename="${name}"`,
    });
    createReadStream(datei).pipe(c.antwort);
  }));

  r.post('/panel/:id/sicherung/loeschen', eigenerServer('backups')(async (c, s) => {
    const fehler = await wo.sicherungLoeschen(s, c.daten.f);
    if (fehler) return zumPanel(c, s, fehler);
    db.protokolliere(wer(c.nutzer), 'Backup gelöscht', `#${s.id} · ${c.daten.f}`, s.id);
    zumPanelGut(c, s, 'Backup gelöscht.');
  }));

  r.post('/panel/:id/sicherung/zurueck', eigenerServer('backups')(async (c, s) => {
    const ergebnis = await wo.sicherungZurueck(s, c.daten.f);
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.protokolliere(wer(c.nutzer), 'Backup zurückgespielt',
      `#${s.id} · ${c.daten.f} · ${ergebnis.entpackt} Dateien`, s.id);
    if (ergebnis.warnung) return zumPanel(c, s, ergebnis.warnung);
    zumPanelGut(c, s, `${ergebnis.entpackt} Dateien zurückgespielt.`);
  }));

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

  /**
   * Den zweiten Faktor eines Kunden abschalten.
   *
   * Der Notausgang, wenn Handy und Ersatzcodes beide weg sind. Ohne ihn
   * braeuchte es einen Griff in die Datenbank - und wer den einmal
   * gelernt hat, greift auch beim naechsten Mal dorthin.
   *
   * Einschalten kann ihn niemand fuer einen anderen: Dafuer braeuchte
   * man das Geheimnis in dessen App, und das gibt es hier nicht.
   */
  r.post('/admin/kunde/:id/zweifach-aus', nurAdmin(async (c) => {
    const id = Number(c.werte.id);
    const k = db.kunde(id);
    if (!k) return sende(c.antwort, fehlerSeite(c.nutzer, 'Kunde nicht gefunden.'), 404);
    db.zweifachAus(id);
    db.protokolliere(wer(c.nutzer), 'Zwei-Faktor abgeschaltet',
      `${k.benutzername} (#${id})`);
    weiter(c.antwort, `/admin/kunde/${id}?ok=` + encodeURIComponent(
      `Zwei-Faktor bei ${k.benutzername} abgeschaltet. Sag ihm, dass er ihn `
      + 'neu einrichten soll.'));
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
    sende(c.antwort, adm.serverBearbeiten(c.nutzer, c.csrf, null, db.kunden(),
      '', db.alleKnoten()))));

  r.post('/admin/server/neu', nurAdmin(async (c) => {
    const d = c.daten;
    if (!PAKETE[d.paket] || !d.name || !d.kundeId) {
      return sende(c.antwort, adm.serverBearbeiten(c.nutzer, c.csrf, null, db.kunden(),
        'Kunde, Name und Paket werden gebraucht.', db.alleKnoten()));
    }
    const id = db.serverAnlegen({
      kundeId: Number(d.kundeId), name: d.name,
      subdomain: String(d.subdomain || '').toLowerCase().replace(/[^a-z0-9-]/g, ''),
      paket: d.paket, software: d.software, notiz: d.notiz,
      pterodactyl: adresseOk(d.pterodactyl), port: portOk(d.port),
      knotenId: knotenOk(d.knotenId), zusatz: zusatzAus(d),
    });
    // Startangaben stehen erst nach dem Anlegen fest - der Datensatz
    // muss dafuer schon existieren.
    const start = startAus(d);
    if (!start.fehler) db.serverAendern(id, start);
    db.protokolliere(wer(c.nutzer), 'Server angelegt', `${d.name} (#${id})`);
    weiter(c.antwort, `/admin/server/${id}`);
  }));

  r.get('/admin/server/:id', nurAdmin((c) => {
    const s = db.server(Number(c.werte.id));
    if (!s) return sende(c.antwort, fehlerSeite(c.nutzer, 'Server nicht gefunden.'), 404);
    sende(c.antwort, adm.serverBearbeiten(c.nutzer, c.csrf, s, db.kunden(),
      c.url.searchParams.get('ok') || '', db.alleKnoten()));
  }));

  r.post('/admin/server/:id', nurAdmin(async (c) => {
    const d = c.daten;
    const id = Number(c.werte.id);
    const start = startAus(d);
    if (start.fehler) {
      return weiter(c.antwort, `/admin/server/${id}?ok=` + encodeURIComponent(start.fehler));
    }
    db.serverAendern(id, {
      ...start,
      name: d.name,
      subdomain: String(d.subdomain || '').toLowerCase().replace(/[^a-z0-9-]/g, ''),
      paket: PAKETE[d.paket] ? d.paket : undefined,
      software: d.software, status: d.status, notiz: d.notiz,
      pterodactyl: adresseOk(d.pterodactyl),
      port: portOk(d.port) || undefined,
      docker_bild: bildOk(d.docker_bild),
      knoten_id: knotenOk(d.knotenId),
    });
    db.zusatzSetzen(id, zusatzAus(d));
    db.protokolliere(wer(c.nutzer), 'Server geändert', `${d.name} (#${id})`);
    weiter(c.antwort, `/admin/server/${id}?ok=Gespeichert.`);
  }));

  /**
   * Einen Server auf eine andere Maschine umziehen.
   *
   * Die Arbeit steckt in umzug.js; hier wird nur geprueft, protokolliert
   * und danach in Worten gesagt, was passiert ist - vor allem, wo die
   * alten Dateien liegengeblieben sind.
   */
  r.post('/admin/server/:id/umzug', nurAdmin(async (c) => {
    const id = Number(c.werte.id);
    const s = db.server(id);
    if (!s) return sende(c.antwort, fehlerSeite(c.nutzer, 'Server nicht gefunden.'), 404);

    const ziel = knotenOk(c.daten.zielKnotenId);
    const e = await umzug.umziehen(s, ziel);
    if (e.fehler) {
      return weiter(c.antwort, `/admin/server/${id}?ok=` + encodeURIComponent(e.fehler));
    }

    const zielName = ziel ? db.knoten(ziel)?.name : 'diesen Rechner';
    db.protokolliere(wer(c.nutzer), 'Server umgezogen',
      `${s.name} (#${id}) → ${zielName}`, id);
    weiter(c.antwort, `/admin/server/${id}?ok=` + encodeURIComponent(
      `Umgezogen auf ${zielName}: ${e.entpackt} Dateien`
      + (e.uebersprungen ? `, ${e.uebersprungen} übersprungen` : '')
      + (e.portGeaendert ? `. Der Port war drüben belegt, neuer Port: ${e.port}` : '')
      + (e.streit?.length ? `. Achtung: ${e.streit.join(', ')} ist auf `
          + `${zielName} schon vergeben – die weiteren Ports musst du von Hand `
          + 'ändern, sonst startet der Container nicht' : '')
      + (e.sicherung ? `. Die Sicherheitskopie ${e.sicherung} liegt noch auf der alten Maschine` : '')
      + `. Die alten Dateien bleiben liegen (${umzug.alteStelle(s)}) – `
      + 'lösch sie erst, wenn drüben alles läuft.'));
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
      'Server aus der Anfrage angelegt. Jetzt den Bestellbogen ausdrucken.'));
  }));

  r.post('/admin/anfrage/:id/ablehnen', nurAdmin((c) => {
    db.bestellungStatus(Number(c.werte.id), 'abgelehnt');
    db.protokolliere(wer(c.nutzer), 'Anfrage abgelehnt', `#${c.werte.id}`);
    weiter(c.antwort, '/admin');
  }));

  r.get('/admin/protokoll', nurAdmin((c) => sende(c.antwort, adm.protokollSeite(c.nutzer))));



  // ------------------------------------------------------- Unterbenutzer
  r.post('/panel/:id/freigeben', nurBesitzer(async (c, s) => {
    const name = String(c.daten.benutzername || '').toLowerCase().trim();
    const k = db.kundePerName(name);
    if (!k) return zumPanel(c, s, `Einen Zugang „${name}" gibt es nicht.`);
    if (k.id === s.kunde_id) {
      return zumPanel(c, s, 'Dem Besitzer musst du nichts freigeben.');
    }
    const rechte = rechteAus(c.daten);
    if (!rechte.length) {
      return zumPanel(c, s, 'Kreuz wenigstens ein Recht an – sonst bringt die '
        + 'Freigabe nichts.');
    }
    db.unterbenutzerSetzen(s.id, k.id, rechte);
    db.protokolliere(wer(c.nutzer), 'Server freigegeben',
      `#${s.id} · für ${k.benutzername} · ${rechte.join(', ')}`, s.id);
    zumPanelGut(c, s, `${k.benutzername} darf jetzt mit.`);
  }));

  r.post('/panel/:id/freigabe-weg', nurBesitzer((c, s) => {
    const kundeId = Number(c.daten.kundeId) || 0;
    const k = db.kunde(kundeId);
    db.unterbenutzerWeg(s.id, kundeId);
    db.protokolliere(wer(c.nutzer), 'Freigabe entzogen',
      `#${s.id} · ${k?.benutzername || kundeId}`, s.id);
    zumPanelGut(c, s, `${k?.benutzername || 'Der Zugang'} kommt nicht mehr rein.`);
  }));

  // ------------------------------------------------------------ Knoten
  r.get('/admin/knoten', nurAdmin(async (c) => {
    const liste = db.alleKnoten();
    // Alle gleichzeitig anpingen - nacheinander dauerte bei drei toten
    // Knoten schon eine halbe Minute.
    const zustaende = await Promise.all(liste.map((k) => fern.pruefe(k)));
    sende(c.antwort, adm.knotenSeite(c.nutzer, c.csrf,
      liste.map((k, i) => ({ ...k, lauf: zustaende[i] })),
      db.alleServer(true), c.url.searchParams.get('ok') || '',
      c.url.searchParams.get('m') || ''));
  }));

  r.post('/admin/knoten', nurAdmin(async (c) => {
    const d = c.daten;
    const name = String(d.name || '').trim();
    const adresse = adresseOk(d.adresse);
    const geheim = String(d.geheim || '').trim();
    if (!name || !adresse || geheim.length < 16) {
      return weiter(c.antwort, '/admin/knoten?m=' + encodeURIComponent(
        'Name, eine http-Adresse und das Zeichen des Daemons werden gebraucht '
        + '– das Zeichen ist mindestens 16 Zeichen lang.'));
    }
    const id = db.knotenAnlegen({ name, adresse, geheim, notiz: d.notiz });
    db.protokolliere(wer(c.nutzer), 'Knoten angelegt', `${name} (#${id}) · ${adresse}`);
    weiter(c.antwort, '/admin/knoten?ok=' + encodeURIComponent(`Knoten ${name} angelegt.`));
  }));

  r.post('/admin/knoten/:id', nurAdmin(async (c) => {
    const d = c.daten;
    const id = Number(c.werte.id);
    db.knotenAendern(id, {
      name: d.name,
      adresse: adresseOk(d.adresse) || undefined,
      // Ein leeres Feld heisst "unverändert" - sonst loeschte ein
      // versehentliches Speichern das Zeichen und niemand käme mehr rein.
      geheim: String(d.geheim || '').trim() || undefined,
      notiz: d.notiz,
    });
    db.protokolliere(wer(c.nutzer), 'Knoten geändert', `#${id}`);
    weiter(c.antwort, '/admin/knoten?ok=Gespeichert.');
  }));

  r.post('/admin/knoten/:id/loeschen', nurAdmin((c) => {
    const id = Number(c.werte.id);
    const fehler = db.knotenLoeschen(id);
    if (fehler) return weiter(c.antwort, '/admin/knoten?m=' + encodeURIComponent(fehler));
    db.protokolliere(wer(c.nutzer), 'Knoten gelöscht', `#${id}`);
    weiter(c.antwort, '/admin/knoten?ok=Knoten gelöscht.');
  }));

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

      // Jede Aenderung braucht das Zeichen aus dem eigenen Cookie.
      //
      // Ausser bei der API: Die weist sich mit einem Schluessel im Kopf
      // aus, nicht mit einem Cookie. CSRF schuetzt davor, dass eine
      // fremde Seite den Browser eines Angemeldeten benutzt - ein
      // Skript hat aber keinen Browser, und ein Schluessel wird nicht
      // automatisch mitgeschickt. Die Pruefung ginge hier also ins
      // Leere und machte die API nur unbenutzbar.
      const istApi = url.pathname.startsWith('/api/');
      let gelesen = {};
      if (anfrage.method === 'POST' && istApi) {
        gelesen = await formular(anfrage).catch(() => ({}));
      } else if (anfrage.method === 'POST') {
        if (treffer.roh) {
          // Beim Datei-Upload ist der Body die Datei - das Zeichen steht
          // deshalb in der URL statt im Formular.
          if (!csrfStimmt(csrf, url.searchParams.get('csrf'))) {
            antwort.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
            return antwort.end(JSON.stringify({ ok: false,
              fehler: 'Die Seite ist abgelaufen. Lade sie neu.' }));
          }
        } else {
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
    console.log(`     Datenbank  ${datenbank}`);
    console.log(`     Server in  ${prozess.wurzel()}`);
    console.log(`     Plugins    ${kat.katalogOrdner()}`
      + ` (${kat.verfuegbar().length} im Katalog)`);
    const d = docker.vorhanden();
    console.log(d.geht
      ? `     Docker     ${d.version}` + (docker.bildDa()
          ? ` · Image ${docker.STANDARD_BILD} liegt bereit`
          : `\n                ⚠ Image ${docker.STANDARD_BILD} fehlt – hol es mit`
            + `\n                  docker pull ${docker.STANDARD_BILD}`
            + `\n                Bis dahin laufen die Server ohne Container.`)
      : `     Docker     ${d.grund}`
        + '\n                Server laufen direkt als Java-Prozess – das geht,'
        + '\n                nur ist die Speichergrenze dann keine echte Grenze,'
        + '\n                sondern nur eine Einstellung der JVM.');
    console.log('');
  });

  /**
   * Strg+C beendet nicht nur das Portal.
   *
   * An ihm haengen echte Minecraft-Server als Kindprozesse. Wuerde das
   * Portal einfach weggehen, riesse es sie mit - mitten im Schreiben,
   * mit allem was seit dem letzten Autosave passiert ist. Also erst
   * "stop" an alle, warten, dann selbst gehen.
   */
  let gehtGerade = false;
  const runterfahren = async (signal) => {
    if (gehtGerade) {
      console.log(`  Noch einmal ${signal} – dann eben sofort.`);
      return process.exit(1);
    }
    gehtGerade = true;
    server.close();
    console.log('\n  Fahre die Minecraft-Server herunter … (nochmal Strg+C bricht ab)');
    const haengen = await prozess.alleStoppen();
    if (haengen) console.log(`  ${haengen} Server musste hart beendet werden.`);
    console.log('  Fertig. Tschüss.');
    process.exit(0);
  };
  process.on('SIGINT', () => runterfahren('SIGINT'));
  process.on('SIGTERM', () => runterfahren('SIGTERM'));

  // Die Uhr fuer Neustarts und automatische Backups.
  plan.starteUhr();
  // Und der Ticker, der die anderen Maschinen nach ihrem Zustand fragt.
  fern.starteTicker();
  // Der Messtakt fuer CPU und Arbeitsspeicher der eigenen Server.
  prozess.starteMessung();

  return server;
}
