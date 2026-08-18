/**
 * Die Verwaltung.
 *
 * Der wichtigste Bildschirm ist die Uebersicht: was laeuft gerade, was
 * ist neu angefragt, wer hat ueberhaupt einen Server. Bezahlt wird in
 * der Schule von Hand - das Portal fuehrt darueber bewusst kein Konto.
 */
import { esc } from '../web.js';
import { seite, statusPunkt, csrfFeld } from './layout.js';
import { PAKETE, ZUSATZ, SOFTWARE, rechne, euro } from '../preise.js';
import { protokollListe } from '../db.js';
import { status as prozessStatus } from '../panel.js';

const LAUFTEXT = { laeuft: 'läuft', startet: 'startet …', stoppt: 'stoppt …',
                   gestoppt: 'gestoppt' };
const LAUFFARBE = { laeuft: 'aktiv', startet: 'archiviert', stoppt: 'archiviert',
                    gestoppt: 'geloescht' };

/** Wo laeuft der Server - bei uns oder in Pterodactyl? */
const laufMarke = (s) => {
  if (s.pterodactyl) return '<span class="marke-punkt aktiv">Pterodactyl</span>';
  const z = prozessStatus(s.id);
  return `<span class="marke-punkt ${LAUFFARBE[z.status] || 'geloescht'}">${
    esc(LAUFTEXT[z.status] || z.status)}</span>${z.pid
    ? `<div class="klein leise mono">PID ${z.pid}</div>` : ''}`;
};

export function uebersicht(nutzer, zeichen, server, kunden, offene) {
  const lebend = server.filter((s) => s.status !== 'geloescht');
  const monatlich = lebend
    .filter((s) => s.status === 'aktiv')
    .reduce((summe, s) => summe + rechne(s.paket, s.zusatz).proMonat, 0);

  const anzahlLaufend = lebend.filter(
    (s) => s.pterodactyl || prozessStatus(s.id).status !== 'gestoppt').length;

  const zeile = (s) => {
    const kunde = kunden.find((k) => k.id === s.kunde_id);
    const aus = rechne(s.paket, s.zusatz).ausstattung;
    return `<tr>
      <td><a href="/admin/server/${s.id}"><strong>${esc(s.name)}</strong></a>
        <div class="klein leise">${kunde ? esc(kunde.vorname + ' ' + kunde.nachname).trim()
          || esc(kunde.benutzername) : '?'}${kunde?.klasse ? ' · ' + esc(kunde.klasse) : ''}</div></td>
      <td>${PAKETE[s.paket]?.zeichen || ''} ${esc(PAKETE[s.paket]?.name || s.paket)}
        <div class="klein leise">${aus.ram} GB · ${aus.cores} Cores</div></td>
      <td>${statusPunkt(s.status)}</td>
      <td>${laufMarke(s)}</td>
      <td class="zahl">${euro(rechne(s.paket, s.zusatz).proMonat)}</td>
      <td class="zahl reihe" style="justify-content:flex-end">
        <a class="knopf klein" href="/panel/${s.id}">Panel</a>
        <a class="knopf klein stil2" href="/admin/server/${s.id}">Bearbeiten</a></td>
    </tr>`;
  };

  return seite({ titel: 'Verwaltung', nutzer, hier: '/admin', inhalt: `
    <h1>Verwaltung</h1>
    <div class="gitter g4 abstand">
      <div class="karte"><div class="klein leise">Server aktiv</div>
        <div class="preis zitrone" style="font-size:1.8rem">${
          lebend.filter((s) => s.status === 'aktiv').length}</div></div>
      <div class="karte"><div class="klein leise">Läuft gerade</div>
        <div class="preis" style="font-size:1.8rem;color:var(--gut)">${anzahlLaufend}</div>
        <div class="klein leise">von ${lebend.length}</div></div>
      <div class="karte"><div class="klein leise">Kunden</div>
        <div class="preis" style="font-size:1.8rem">${kunden.length}</div></div>
      <div class="karte"><div class="klein leise">Laufende Einnahmen</div>
        <div class="preis zitrone" style="font-size:1.8rem">${euro(monatlich)}</div>
        <div class="klein leise">pro Monat, wenn alle zahlen</div></div>
    </div>

    ${offene.length ? `<div class="karte abstand">
      <div class="zwischen"><h2>Offene Anfragen</h2>
        <span class="marke-punkt offen">${offene.length}</span></div>
      <table class="abstand">
        <tr><th>Nr.</th><th>Wer</th><th>Server</th><th>Paket</th><th class="zahl">Preis</th><th></th></tr>
        ${offene.map((b) => `<tr>
          <td class="mono">#${b.id}</td>
          <td>${esc((b.vorname + ' ' + b.nachname).trim() || '–')}
            <div class="klein leise">${esc(b.klasse)}${
              b.mcname ? ' · ' + esc(b.mcname) : ''}</div></td>
          <td>${esc(b.servername || '–')}
            ${b.subdomain ? `<div class="mono klein leise">${esc(b.subdomain)}</div>` : ''}</td>
          <td>${esc(PAKETE[b.paket]?.name || b.paket)}</td>
          <td class="zahl">${euro(rechne(b.paket, b.zusatz).proMonat)}</td>
          <td><a class="knopf klein" href="/admin/anfrage/${b.id}">Ansehen</a></td>
        </tr>`).join('')}
      </table></div>` : ''}

    <div class="karte abstand">
      <div class="zwischen"><h2>Alle Server</h2>
        <div class="reihe">
          <a class="knopf klein" href="/admin/server/neu">Server anlegen</a>
          <a class="knopf klein stil2" href="/admin/kunden">Kunden</a>
          <a class="knopf klein stil2" href="/admin/protokoll">Protokoll</a>
        </div></div>
      <table class="abstand">
        <tr><th>Server</th><th>Paket</th><th>Status</th><th>Läuft</th>
            <th class="zahl">Monat</th><th></th></tr>
        ${lebend.map(zeile).join('')
          || '<tr><td colspan="6" class="leise">Noch keine Server.</td></tr>'}
      </table>
    </div>

    <p class="klein leise abstand">Bezahlt wird in der Schule, bar und gegen
      Bestellbogen. Das Portal führt darüber keine Buchhaltung — es verwaltet
      die Server, nicht das Geld.</p>` });
}

export function serverBearbeiten(nutzer, zeichen, s, kunden, meldung = '') {
  const neu = !s;
  const zusatz = s?.zusatz || {};
  const ergebnis = s ? rechne(s.paket, zusatz) : null;

  const gruppen = {};
  for (const z of Object.values(ZUSATZ)) (gruppen[z.gruppe] ??= []).push(z);

  return seite({ titel: neu ? 'Server anlegen' : s.name, nutzer, hier: '/admin', inhalt: `
    <a class="klein leise" href="/admin">← Verwaltung</a>
    <div class="zwischen abstand">
      <h1>${neu ? 'Server anlegen' : esc(s.name)}</h1>
      ${neu ? '' : statusPunkt(s.status)}
    </div>
    ${meldung ? `<div class="hinweis info abstand">${esc(meldung)}</div>` : ''}

    <div class="gitter g2" style="align-items:start">
      <form method="post" action="${neu ? '/admin/server/neu' : `/admin/server/${s.id}`}"
            class="karte">
        ${csrfFeld(zeichen)}
        <h2>Stammdaten</h2>
        <div class="feld abstand"><label>Kunde</label>
          <select name="kundeId" required>
            ${kunden.map((k) => `<option value="${k.id}"${
              s && s.kunde_id === k.id ? ' selected' : ''}>${
              esc((k.vorname + ' ' + k.nachname).trim() || k.benutzername)}${
              k.klasse ? ' (' + esc(k.klasse) + ')' : ''}</option>`).join('')}
          </select></div>
        <div class="feld"><label>Servername</label>
          <input name="name" value="${esc(s?.name || '')}" required></div>
        <div class="feld-reihe">
          <div class="feld"><label>Subdomain</label>
            <input name="subdomain" value="${esc(s?.subdomain || '')}" class="mono"></div>
          <div class="feld"><label>Software</label>
            <select name="software">${SOFTWARE.map((x) =>
              `<option${s?.software === x ? ' selected' : ''}>${esc(x)}</option>`).join('')}</select></div>
        </div>
        <div class="feld-reihe">
          <div class="feld"><label>Paket</label>
            <select name="paket" required>${Object.values(PAKETE).map((p) =>
              `<option value="${p.id}"${s?.paket === p.id ? ' selected' : ''}>${
                esc(p.name)} — ${euro(p.preis)}</option>`).join('')}</select></div>
          <div class="feld"><label>Docker-Image</label>
            <input name="docker_bild" class="mono" value="${esc(s?.docker_bild || '')}"
                   placeholder="leer = eclipse-temurin:21-jre"></div>
          <div class="feld"><label>Port</label>
            <input name="port" type="number" min="1024" max="65535"
                   value="${s?.port || ''}" class="mono"
                   placeholder="wird beim Anlegen vergeben"></div>
          <div class="feld"><label>Status</label>
            <select name="status">${['aktiv', 'archiviert'].map((x) =>
              `<option value="${x}"${s?.status === x ? ' selected' : ''}>${esc(x)}</option>`).join('')}</select></div>
        </div>
        <div class="feld"><label>Pterodactyl-Adresse (optional)</label>
          <input name="pterodactyl" class="mono" value="${esc(s?.pterodactyl || '')}"
                 placeholder="https://panel.example.de/server/a1b2c3d4">
          <p class="klein leise" style="margin-top:.3rem">Steht hier etwas, läuft
            der Server in Pterodactyl. Das Portal verlinkt dann nur dorthin und
            startet ihn nicht selbst.</p></div>
        <div class="feld"><label>Notiz (intern)</label>
          <textarea name="notiz" rows="2">${esc(s?.notiz || '')}</textarea></div>

        <h2 class="abstand">Zusatzleistungen</h2>
        ${Object.entries(gruppen).map(([gruppe, liste]) => `
          <div class="klein leise abstand">${esc(gruppe)}</div>
          ${liste.map((z) => z.menge ? `
            <div class="feld" style="display:flex;align-items:center;gap:.8rem">
              <div style="flex:1"><strong>${esc(z.name)}</strong>
                <span class="klein leise"> ${euro(z.preis)}</span></div>
              <input type="number" name="z_${z.id}" min="0" max="20" style="width:80px"
                     value="${zusatz[z.id] || 0}"></div>` : `
            <label class="feld" style="display:flex;align-items:center;cursor:pointer">
              <input type="checkbox" name="z_${z.id}" value="1"${zusatz[z.id] ? ' checked' : ''}>
              <span><strong>${esc(z.name)}</strong>
                <span class="klein leise"> ${euro(z.preis)}</span></span></label>`).join('')}
        `).join('')}
        <button class="knopf abstand" style="width:100%;justify-content:center">
          ${neu ? 'Server anlegen' : 'Änderungen speichern'}</button>
      </form>

      <div>
        ${neu ? '' : `
        <div class="karte">
          <div class="zwischen"><h2>Panel</h2>${laufMarke(s)}</div>
          <p class="klein leise abstand">Konsole, Start und Stopp, Dateien —
            dieselbe Ansicht, die auch der Kunde sieht.</p>
          <div class="reihe abstand">
            <a class="knopf" href="/panel/${s.id}">Panel öffnen</a>
            ${s.pterodactyl ? '' :
              `<a class="knopf stil2" href="/panel/${s.id}/dateien">Dateien</a>`}
          </div>
        </div>

        <div class="karte" style="margin-top:1rem">
          <h2>Kosten</h2>
          <table class="abstand">
            ${ergebnis.posten.map((p) => `<tr>
              <td>${p.menge > 1 ? p.menge + '× ' : ''}${esc(p.name)}${
                p.imPaket ? ' <span class="klein leise">(im Paket)</span>' : ''}</td>
              <td class="zahl">${euro(p.summe)}</td></tr>`).join('')}
          </table>
          <div class="summe-zeile summe-gesamt">
            <span>pro Monat</span><strong class="zitrone">${euro(ergebnis.proMonat)}</strong></div>
          ${ergebnis.einmalig > 0 ? `<div class="summe-zeile">
            <span class="leise">einmalig</span><span>${euro(ergebnis.einmalig)}</span></div>` : ''}
          <div class="klein leise abstand">
            ${ergebnis.ausstattung.cores} Cores ·
            ${ergebnis.ausstattung.ram} GB RAM ·
            ${ergebnis.ausstattung.ssd} GB SSD</div>
        </div>

        <div class="karte" style="margin-top:1rem">
          <h2>Dokumente</h2>
          <div class="reihe abstand">
            <a class="knopf stil2 klein" href="/admin/server/${s.id}/bestellbogen"
               target="_blank">Bestellbogen</a>
            <a class="knopf stil2 klein" href="/admin/server/${s.id}/loeschbestaetigung"
               target="_blank">Löschbestätigung</a>
          </div>
          <p class="klein leise abstand">Beide Dokumente sind mit den erfassten
            Daten vorausgefüllt und lassen sich direkt ausdrucken.</p>
        </div>

        <div class="karte" style="margin-top:1rem">
          <h2>Server löschen</h2>
          <p class="klein leise">Der Server wird als gelöscht gekennzeichnet und
            verschwindet aus der Übersicht. Die Daten bleiben im Portal, damit die
            Löschbestätigung später noch Servername, Owner und Zeitraum nennen kann.</p>
          <form method="post" action="/admin/server/${s.id}/loeschen" class="abstand"
                onsubmit="return confirm('${esc(s.name)} wirklich als gelöscht kennzeichnen?')">
            ${csrfFeld(zeichen)}
            <button class="knopf gefahr klein">Als gelöscht kennzeichnen</button>
          </form>
        </div>`}
      </div>
    </div>` });
}

export function kundenSeite(nutzer, zeichen, kunden, server, meldung = '') {
  return seite({ titel: 'Kunden', nutzer, hier: '/admin', inhalt: `
    <a class="klein leise" href="/admin">← Verwaltung</a>
    <h1 class="abstand">Kunden</h1>
    ${meldung ? `<div class="hinweis info abstand">${esc(meldung)}</div>` : ''}

    <div class="gitter g2" style="align-items:start">
      <div class="karte">
        <h2>Alle Kunden</h2>
        <table class="abstand">
          <tr><th>Name</th><th>Klasse</th><th>Minecraft</th><th class="zahl">Server</th><th></th></tr>
          ${kunden.map((k) => `<tr>
            <td><strong>${esc((k.vorname + ' ' + k.nachname).trim() || '–')}</strong>
              <div class="mono klein leise">${esc(k.benutzername)}${
                k.rolle === 'admin' ? ' · Admin' : ''}</div></td>
            <td>${esc(k.klasse || '–')}</td>
            <td class="klein">${esc(k.mcname || '–')}</td>
            <td class="zahl">${server.filter((s) => s.kunde_id === k.id
              && s.status !== 'geloescht').length}</td>
            <td><a class="knopf klein stil2" href="/admin/kunde/${k.id}">Öffnen</a></td>
          </tr>`).join('')}
        </table>
      </div>

      <form method="post" action="/admin/kunden" class="karte">
        ${csrfFeld(zeichen)}
        <h2>Kunde anlegen</h2>
        <div class="feld-reihe abstand">
          <div class="feld"><label>Vorname</label><input name="vorname"></div>
          <div class="feld"><label>Nachname</label><input name="nachname"></div>
        </div>
        <div class="feld-reihe">
          <div class="feld"><label>Klasse</label><input name="klasse"></div>
          <div class="feld"><label>Minecraft-Name</label><input name="mcname"></div>
        </div>
        <div class="feld"><label>Kontakt / Discord</label><input name="kontakt"></div>
        <div class="feld"><label>Benutzername fürs Portal</label>
          <input name="benutzername" required class="mono"></div>
        <div class="feld"><label>Startpasswort</label>
          <input name="passwort" required minlength="8">
          <div class="klein leise" style="margin-top:.3rem">Mindestens 8 Zeichen.
            Gib es persönlich weiter, nicht per Chat.</div></div>
        <button class="knopf" style="width:100%;justify-content:center">Kunde anlegen</button>
      </form>
    </div>` });
}

export function kundeSeite(nutzer, zeichen, k, server, meldung = '') {
  return seite({ titel: k.benutzername, nutzer, hier: '/admin', inhalt: `
    <a class="klein leise" href="/admin/kunden">← Kunden</a>
    <h1 class="abstand">${esc((k.vorname + ' ' + k.nachname).trim() || k.benutzername)}</h1>
    ${meldung ? `<div class="hinweis info abstand">${esc(meldung)}</div>` : ''}

    <div class="gitter g2" style="align-items:start">
      <form method="post" action="/admin/kunde/${k.id}" class="karte">
        ${csrfFeld(zeichen)}
        <h2>Daten</h2>
        <div class="feld-reihe abstand">
          <div class="feld"><label>Vorname</label>
            <input name="vorname" value="${esc(k.vorname)}"></div>
          <div class="feld"><label>Nachname</label>
            <input name="nachname" value="${esc(k.nachname)}"></div>
        </div>
        <div class="feld-reihe">
          <div class="feld"><label>Klasse</label>
            <input name="klasse" value="${esc(k.klasse)}"></div>
          <div class="feld"><label>Minecraft-Name</label>
            <input name="mcname" value="${esc(k.mcname)}"></div>
        </div>
        <div class="feld"><label>Kontakt / Discord</label>
          <input name="kontakt" value="${esc(k.kontakt)}"></div>
        <div class="feld"><label>Rolle</label>
          <select name="rolle">
            <option value="kunde"${k.rolle === 'kunde' ? ' selected' : ''}>Kunde</option>
            <option value="admin"${k.rolle === 'admin' ? ' selected' : ''}>Admin</option>
          </select></div>
        <button class="knopf">Speichern</button>
      </form>

      <div>
        <div class="karte">
          <h2>Server</h2>
          ${server.length ? `<table class="abstand">
            ${server.map((s) => `<tr>
              <td><a href="/admin/server/${s.id}">${esc(s.name)}</a></td>
              <td>${statusPunkt(s.status)}</td>
              <td>${laufMarke(s)}</td>
              <td class="zahl"><a class="knopf klein stil2"
                href="/panel/${s.id}">Panel</a></td></tr>`).join('')}
          </table>` : '<p class="leise klein abstand">Noch keine Server.</p>'}
        </div>
        <form method="post" action="/admin/kunde/${k.id}/passwort" class="karte"
              style="margin-top:1rem">
          ${csrfFeld(zeichen)}
          <h2>Passwort zurücksetzen</h2>
          <div class="feld abstand"><label>Neues Passwort</label>
            <input name="passwort" required minlength="8"></div>
          <p class="klein leise" style="margin-bottom:.8rem">Setzt das Passwort neu
            und meldet den Kunden überall ab.</p>
          <button class="knopf stil2">Passwort setzen</button>
        </form>
      </div>
    </div>` });
}

export function anfrageSeite(nutzer, zeichen, b, kunden) {
  const ergebnis = rechne(b.paket, b.zusatz);
  return seite({ titel: `Anfrage #${b.id}`, nutzer, hier: '/admin', inhalt: `
    <a class="klein leise" href="/admin">← Verwaltung</a>
    <div class="zwischen abstand">
      <h1>Anfrage #${b.id}</h1>${statusPunkt(b.status)}
    </div>

    <div class="gitter g2" style="align-items:start">
      <div class="karte">
        <h2>Angaben</h2>
        <table class="abstand">
          <tr><td class="leise">Name</td><td>${esc((b.vorname + ' ' + b.nachname).trim() || '–')}</td></tr>
          <tr><td class="leise">Klasse</td><td>${esc(b.klasse || '–')}</td></tr>
          <tr><td class="leise">Minecraft</td><td>${esc(b.mcname || '–')}</td></tr>
          <tr><td class="leise">Kontakt</td><td>${esc(b.kontakt || '–')}</td></tr>
          <tr><td class="leise">Servername</td><td>${esc(b.servername || '–')}</td></tr>
          <tr><td class="leise">Subdomain</td><td class="mono">${esc(b.subdomain || '–')}</td></tr>
          <tr><td class="leise">Software</td><td>${esc(b.software)}</td></tr>
          <tr><td class="leise">Eingegangen</td><td class="klein">${esc(b.angelegt.slice(0, 16).replace('T', ' '))}</td></tr>
        </table>
        ${b.wunsch ? `<div class="abstand"><div class="klein leise">Sonderwünsche</div>
          <div class="klein">${esc(b.wunsch)}</div></div>` : ''}
      </div>

      <div>
        <div class="karte">
          <h2>Gewünschte Leistung</h2>
          <table class="abstand">
            ${ergebnis.posten.map((p) => `<tr>
              <td>${p.menge > 1 ? p.menge + '× ' : ''}${esc(p.name)}${
                p.imPaket ? ' <span class="klein leise">(im Paket)</span>' : ''}</td>
              <td class="zahl">${euro(p.summe)}</td></tr>`).join('')}
          </table>
          <div class="summe-zeile summe-gesamt">
            <span>pro Monat</span><strong class="zitrone">${euro(ergebnis.proMonat)}</strong></div>
        </div>

        ${b.status === 'offen' ? `
        <form method="post" action="/admin/anfrage/${b.id}/annehmen" class="karte"
              style="margin-top:1rem">
          ${csrfFeld(zeichen)}
          <h2>Annehmen und Server anlegen</h2>
          <div class="feld abstand"><label>Auf welchen Kunden?</label>
            <select name="kundeId" required>
              <option value="">— neuen Kunden anlegen —</option>
              ${kunden.map((k) => `<option value="${k.id}"${
                b.kunde_id === k.id ? ' selected' : ''}>${
                esc((k.vorname + ' ' + k.nachname).trim() || k.benutzername)}</option>`).join('')}
            </select></div>
          <div class="feld"><label>Benutzername (nur bei neuem Kunden)</label>
            <input name="benutzername" class="mono"
                   value="${esc((b.vorname + '.' + b.nachname).toLowerCase().replace(/[^a-z0-9.]/g, ''))}"></div>
          <div class="feld"><label>Startpasswort (nur bei neuem Kunden)</label>
            <input name="passwort" minlength="8"></div>
          <button class="knopf" style="width:100%;justify-content:center">
            Annehmen und Server anlegen</button>
        </form>
        <form method="post" action="/admin/anfrage/${b.id}/ablehnen" class="karte"
              style="margin-top:1rem">
          ${csrfFeld(zeichen)}
          <button class="knopf gefahr klein">Anfrage ablehnen</button>
        </form>` : ''}
      </div>
    </div>` });
}

export function protokollSeite(nutzer) {
  const liste = protokollListe(200);
  return seite({ titel: 'Protokoll', nutzer, hier: '/admin', inhalt: `
    <a class="klein leise" href="/admin">← Verwaltung</a>
    <h1 class="abstand">Protokoll</h1>
    <p class="leise">Jede Änderung an Servern und Kunden landet hier, dazu jeder
      Start, Stopp und Konsolenbefehl — damit später nachvollziehbar ist,
      wer wann was gemacht hat.</p>
    <div class="karte abstand"><table>
      <tr><th>Wann</th><th>Wer</th><th>Was</th><th>Details</th></tr>
      ${liste.map((p) => `<tr>
        <td class="klein mono">${esc(p.wann.slice(0, 16).replace('T', ' '))}</td>
        <td class="klein">${esc(p.wer)}</td>
        <td>${esc(p.was)}</td>
        <td class="klein leise">${esc(p.details)}</td></tr>`).join('')
        || '<tr><td colspan="4" class="leise">Noch nichts passiert.</td></tr>'}
    </table></div>` });
}
