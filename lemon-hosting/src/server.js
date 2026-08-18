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
    // einer Nummer, um in fremde Unterlagen zu schauen.
    if (!s || (s.kunde_id !== c.nutzer.id && c.nutzer.rolle !== 'admin')) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Diesen Server gibt es nicht.'), 404);
    }
    sende(c.antwort, ks.serverDetail(c.nutzer, s));
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
  const meinServer = (fn) => (c) => {
    if (!c.nutzer) return weiter(c.antwort, '/anmelden');
    const s = db.server(Number(c.werte.id));
    if (!s || (s.kunde_id !== c.nutzer.id && c.nutzer.rolle !== 'admin')) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Diesen Server gibt es nicht.'), 404);
    }
    return fn(c, s);
  };

  /**
   * Wie oben, aber nur fuer Server, die das Portal selbst betreibt.
   *
   * Bei einem Server in Pterodactyl gibt es hier nichts zu starten und
   * keine Dateien zu bearbeiten - das macht Pterodactyl. Zwei Stellen,
   * die denselben Server anfassen duerfen, waeren ein Rezept fuer
   * kaputte Welten.
   */
  const eigenerServer = (fn) => meinServer((c, s) => {
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

  const jsonRaus = (c, status, daten) => {
    c.antwort.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
    c.antwort.end(JSON.stringify(daten));
  };

  r.get('/panel/:id', meinServer((c, s) => {
    if (s.pterodactyl) {
      return sende(c.antwort, pnl.fremdesPanel(c.nutzer, s, s.pterodactyl));
    }
    const gut = c.url.searchParams.get('ok') || '';
    // Katalog und installierte Plugins zusammenfuehren, damit die Seite
    // je Zeile weiss, ob "Installieren" oder "Entfernen" drangehoert.
    const da = kat.installiert(s.id);
    const plugins = kat.verfuegbar().map((p) => ({ ...p, da: da.has(p.stamm) }));
    sende(c.antwort, pnl.panel(c.nutzer, s, prozess.status(s.id), c.csrf,
      dat.belegung(s.id), gut || c.url.searchParams.get('m') || '', Boolean(gut),
      sich.liste(s.id), plugins));
  }));

  r.post('/panel/:id/aktion', eigenerServer((c, s) => {
    const was = c.daten.was;

    if (was === 'eula') {
      prozess.eulaAnnehmen(s.id);
      db.protokolliere(wer(c.nutzer), 'EULA angenommen', `${s.name} (#${s.id})`);
      return zumPanel(c, s);
    }

    // Ein archivierter oder geloeschter Server startet nicht. Stoppen
    // darf man ihn trotzdem - sonst liefe ein gerade archivierter Server
    // ewig weiter.
    if (was === 'start' && s.status !== 'aktiv') {
      return zumPanel(c, s, `Dieser Server ist ${s.status} und lässt sich nicht starten.`);
    }

    const machen = {
      start: () => prozess.starte(s),
      stopp: () => prozess.stoppe(s.id),
      neustart: () => prozess.neustart(s),
    }[was];
    if (!machen) return zumPanel(c, s, 'Unbekannte Aktion.');

    const fehler = machen();
    db.protokolliere(wer(c.nutzer), 'Panel: ' + was,
      `${s.name} (#${s.id})` + (fehler ? ' – ' + fehler : ''));
    zumPanel(c, s, fehler);
  }));

  r.post('/panel/:id/befehl', eigenerServer((c, s) => {
    const text = String(c.daten.befehl || '');
    const fehler = prozess.befehl(s.id, text);
    if (!fehler) db.protokolliere(wer(c.nutzer), 'Konsolenbefehl', `#${s.id} · ${text}`);
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
  r.get('/panel/:id/konsole', eigenerServer((c, s) => {
    const a = c.antwort;
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
  }));

  // ------------------------------------------------------------ Dateien
  r.get('/panel/:id/dateien', eigenerServer((c, s) => {
    const pfad = dat.saeubere(c.url.searchParams.get('p'));
    const eintraege = dat.liste(s.id, pfad);
    if (!eintraege) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Diesen Ordner gibt es nicht.'), 404);
    }
    const ok = c.url.searchParams.get('ok') || '';
    sende(c.antwort, pnl.dateien(c.nutzer, s, pfad, eintraege, c.csrf,
      ok || c.url.searchParams.get('m') || '', Boolean(ok)));
  }));

  r.get('/panel/:id/bearbeiten', eigenerServer((c, s) => {
    const pfad = dat.saeubere(c.url.searchParams.get('p'));
    const inhalt = dat.lies(s.id, pfad);
    if (inhalt === null) {
      return sende(c.antwort, fehlerSeite(c.nutzer, 'Diese Datei lässt sich hier nicht '
        + 'öffnen – sie ist zu groß oder kein Text.'), 404);
    }
    sende(c.antwort, pnl.bearbeiten(c.nutzer, s, pfad, inhalt, c.csrf));
  }));

  r.post('/panel/:id/speichern', eigenerServer((c, s) => {
    const pfad = dat.saeubere(c.daten.p);
    const fehler = dat.schreib(s.id, pfad, c.daten.inhalt ?? '');
    if (fehler) {
      return sende(c.antwort, pnl.bearbeiten(c.nutzer, s, pfad,
        String(c.daten.inhalt ?? ''), c.csrf, fehler));
    }
    db.protokolliere(wer(c.nutzer), 'Datei gespeichert', `#${s.id} · ${pfad}`);
    zuDateien(c, s, pfad.split('/').slice(0, -1).join('/'), `${pfad} gespeichert.`, true);
  }));

  r.post('/panel/:id/loeschen', eigenerServer((c, s) => {
    const pfad = dat.saeubere(c.daten.p);
    const fehler = dat.loesche(s.id, pfad);
    if (!fehler) db.protokolliere(wer(c.nutzer), 'Datei gelöscht', `#${s.id} · ${pfad}`);
    zuDateien(c, s, pfad.split('/').slice(0, -1).join('/'),
      fehler || `${pfad} gelöscht.`, !fehler);
  }));

  r.post('/panel/:id/neu', eigenerServer((c, s) => {
    const pfad = dat.saeubere(c.daten.p);
    const name = dat.nameOk(c.daten.name);
    if (!name) return zuDateien(c, s, pfad, 'Der Dateiname geht so nicht.');
    const ziel = pfad ? pfad + '/' + name : name;
    const fehler = dat.schreib(s.id, ziel, c.daten.inhalt ?? '');
    if (!fehler) db.protokolliere(wer(c.nutzer), 'Datei angelegt', `#${s.id} · ${ziel}`);
    zuDateien(c, s, pfad, fehler || `${name} angelegt.`, !fehler);
  }));

  r.post('/panel/:id/ordner', eigenerServer((c, s) => {
    const pfad = dat.saeubere(c.daten.p);
    const name = dat.nameOk(c.daten.name);
    if (!name) return zuDateien(c, s, pfad, 'Der Ordnername geht so nicht.');
    const fehler = dat.neuerOrdner(s.id, pfad ? pfad + '/' + name : name);
    zuDateien(c, s, pfad, fehler || `Ordner ${name} angelegt.`, !fehler);
  }));

  /**
   * Datei hochladen - roher Body, kein Formular.
   *
   * Deshalb `postRoh`: der Handler bekommt die Anfrage ungelesen und
   * leitet sie direkt auf die Platte weiter. Sonst muesste eine
   * 55-MB-Jar erst komplett in den Arbeitsspeicher.
   */
  r.postRoh('/panel/:id/hochladen', eigenerServer(async (c, s) => {
    const fehler = await dat.nimmDatei(s.id, c.url.searchParams.get('p'),
      c.url.searchParams.get('name'), c.anfrage);
    if (fehler) return jsonRaus(c, 400, { ok: false, fehler });
    db.protokolliere(wer(c.nutzer), 'Datei hochgeladen',
      `#${s.id} · ${c.url.searchParams.get('name')}`);
    jsonRaus(c, 200, { ok: true });
  }));


  // -------------------------------------------------- Zeitplan und Backups
  const zumPanelGut = (c, s, meldung) => weiter(c.antwort,
    `/panel/${s.id}?ok=` + encodeURIComponent(meldung));

  r.post('/panel/:id/plugin/installieren', eigenerServer((c, s) => {
    const ergebnis = kat.installiere(s.id, c.daten.datei);
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.protokolliere(wer(c.nutzer), 'Plugin installiert',
      `#${s.id} · ${c.daten.datei}` + (ergebnis.ersetzt ? ` (ersetzt ${ergebnis.ersetzt})` : ''));
    zumPanelGut(c, s, `${ergebnis.name} installiert – beim nächsten Neustart ist es da.`);
  }));

  r.post('/panel/:id/plugin/entfernen', eigenerServer((c, s) => {
    const ergebnis = kat.entferne(s.id, c.daten.datei);
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.protokolliere(wer(c.nutzer), 'Plugin entfernt', `#${s.id} · ${c.daten.datei}`);
    zumPanelGut(c, s, `${ergebnis.name} entfernt – beim nächsten Neustart ist es weg.`);
  }));

  r.post('/panel/:id/zeitplan', eigenerServer((c, s) => {
    const neustart = plan.zeitOk(c.daten.neustart_um);
    const sicherung = plan.zeitOk(c.daten.sicherung_um);
    db.serverAendern(s.id, { neustart_um: neustart, sicherung_um: sicherung });
    db.protokolliere(wer(c.nutzer), 'Zeitplan geändert',
      `#${s.id} · Neustart ${neustart || '–'} · Backup ${sicherung || '–'}`);
    zumPanelGut(c, s, 'Zeitplan gespeichert.');
  }));

  r.post('/panel/:id/sicherung', eigenerServer(async (c, s) => {
    const ergebnis = await sich.anlegen(s.id);
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.protokolliere(wer(c.nutzer), 'Backup angelegt',
      `#${s.id} · ${ergebnis.name} · ${ergebnis.groesse}`);
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
  r.get('/panel/:id/sicherung/laden', eigenerServer((c, s) => {
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

  r.post('/panel/:id/sicherung/loeschen', eigenerServer((c, s) => {
    const fehler = sich.loeschen(s.id, c.daten.f);
    if (fehler) return zumPanel(c, s, fehler);
    db.protokolliere(wer(c.nutzer), 'Backup gelöscht', `#${s.id} · ${c.daten.f}`);
    zumPanelGut(c, s, 'Backup gelöscht.');
  }));

  r.post('/panel/:id/sicherung/zurueck', eigenerServer((c, s) => {
    const ergebnis = sich.zurueckspielen(s.id, c.daten.f);
    if (ergebnis.fehler) return zumPanel(c, s, ergebnis.fehler);
    db.protokolliere(wer(c.nutzer), 'Backup zurückgespielt',
      `#${s.id} · ${c.daten.f} · ${ergebnis.entpackt} Dateien`);
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
      paket: d.paket, software: d.software, notiz: d.notiz,
      pterodactyl: adresseOk(d.pterodactyl), port: portOk(d.port),
      zusatz: zusatzAus(d),
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
      pterodactyl: adresseOk(d.pterodactyl),
      port: portOk(d.port) || undefined,
      docker_bild: bildOk(d.docker_bild),
    });
    db.zusatzSetzen(id, zusatzAus(d));
    db.protokolliere(wer(c.nutzer), 'Server geändert', `${d.name} (#${id})`);
    weiter(c.antwort, `/admin/server/${id}?ok=Gespeichert.`);
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
      let gelesen = {};
      if (anfrage.method === 'POST') {
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
      : `     Docker     nicht verfügbar (${d.grund})`
        + `\n                Server laufen direkt als Java-Prozess – die`
        + `\n                Speichergrenze ist dann nur eine JVM-Einstellung.`);
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

  return server;
}
