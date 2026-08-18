/**
 * Was ein angemeldeter Kunde sieht.
 *
 * Eine Karte pro Server, und auf jeder der Knopf, um den es geht: ins
 * Panel. Die Details darunter - Paket, Leistung, Preis - sind zum
 * Nachschauen da, nicht zum Arbeiten.
 */
import { esc } from '../web.js';
import { seite, statusPunkt } from './layout.js';
import { PAKETE, ZUSATZ, rechne, euro, ARCHIV_TAGE } from '../preise.js';
import { bestellungenVonKunde } from '../db.js';
import { status as prozessStatus } from '../panel.js';
import { adresse } from './panel.js';

const TEXTE = { laeuft: 'läuft', startet: 'startet …', stoppt: 'stoppt …',
                gestoppt: 'gestoppt' };
const FARBEN = { laeuft: 'aktiv', startet: 'archiviert', stoppt: 'archiviert',
                 gestoppt: 'geloescht' };

export function meineServer(nutzer, server) {
  if (!server.length) {
    return seite({ titel: 'Meine Server', nutzer, hier: '/meine-server', inhalt: `
      <h1>Meine Server</h1>
      <div class="karte abstand">
        <p class="leise">Hier ist noch nichts. Sobald ein Server für dich
        eingerichtet ist, siehst du ihn an dieser Stelle — mit Konsole,
        Start-Knopf und Dateien.</p>
        <a class="knopf abstand" href="/konfigurator">Server zusammenstellen</a>
      </div>` });
  }

  const karten = server.map((s) => {
    const paket = PAKETE[s.paket];
    const ergebnis = rechne(s.paket, s.zusatz);
    const lauf = prozessStatus(s.id);
    const zusatzListe = Object.entries(s.zusatz)
      .map(([id, menge]) => ZUSATZ[id] ? `${menge > 1 ? menge + '× ' : ''}${ZUSATZ[id].name}` : id);

    let warnung = '';
    if (s.status === 'archiviert') {
      warnung = `<div class="hinweis warn">Dieser Server ist <strong>archiviert</strong>
        und lässt sich nicht starten. Sprich das Team an — ohne Klärung wird er
        nach ${ARCHIV_TAGE} Tagen gelöscht.</div>`;
    } else if (s.status === 'geloescht') {
      warnung = `<div class="hinweis schlecht">Dieser Server wurde am
        ${esc(s.geloescht_am || '–')} gelöscht.</div>`;
    }

    // Läuft der Server in Pterodactyl, sagt der Punkt oben rechts das -
    // ein "gestoppt" wäre dort schlicht gelogen, weil das Portal es gar
    // nicht wissen kann.
    const punkt = s.pterodactyl
      ? '<span class="marke-punkt aktiv">Pterodactyl</span>'
      : `<span class="marke-punkt ${FARBEN[lauf.status] || 'geloescht'}">${
          esc(TEXTE[lauf.status] || lauf.status)}</span>`;

    return `<article class="karte">
      <div class="zwischen">
        <div>
          <h2>${esc(s.name)}</h2>
          <div class="mono klein leise">${esc(adresse(s))}</div>
        </div>
        <div class="reihe">
          ${!s.pterodactyl && lauf.spieler?.length
            ? `<span class="klein leise">${lauf.spieler.length} online</span>` : ''}
          ${punkt}
          ${s.status !== 'aktiv' ? statusPunkt(s.status) : ''}
        </div>
      </div>
      ${warnung ? `<div class="abstand">${warnung}</div>` : ''}
      <div class="gitter g4 abstand">
        <div><div class="klein leise">Paket</div>
          <strong>${paket ? paket.zeichen + ' ' + esc(paket.name) : esc(s.paket)}</strong></div>
        <div><div class="klein leise">CPU</div>
          <strong>${ergebnis.ausstattung?.cores ?? '–'} Cores</strong></div>
        <div><div class="klein leise">RAM</div>
          <strong>${ergebnis.ausstattung?.ram ?? '–'} GB</strong></div>
        <div><div class="klein leise">Speicher</div>
          <strong>${ergebnis.ausstattung?.ssd ?? '–'} GB</strong></div>
      </div>
      ${zusatzListe.length ? `<div class="abstand">
        <div class="klein leise">Zusatzleistungen</div>
        <div class="klein">${zusatzListe.map(esc).join(' · ')}</div></div>` : ''}
      <div class="reihe abstand">
        <a class="knopf" href="/panel/${s.id}">Panel öffnen</a>
        ${s.pterodactyl ? '' :
          `<a class="knopf stil2" href="/panel/${s.id}/dateien">Dateien</a>`}
        <a class="knopf stil2 klein" href="/meine-server/${s.id}">Was ist gebucht?</a>
      </div>
    </article>`;
  }).join('');

  const anfragen = bestellungenVonKunde(nutzer.id).filter((b) => b.status === 'offen');

  return seite({ titel: 'Meine Server', nutzer, hier: '/meine-server', inhalt: `
    <h1>Meine Server</h1>
    ${anfragen.length ? `<div class="hinweis info abstand">
      ${anfragen.length} offene Anfrage${anfragen.length === 1 ? '' : 'n'} —
      das Team meldet sich bei dir.</div>` : ''}
    <div class="gitter abstand">${karten}</div>` });
}

/** Die Nachschlagseite: was gebucht ist und was es kostet. */
export function serverDetail(nutzer, s) {
  const ergebnis = rechne(s.paket, s.zusatz);
  const paket = PAKETE[s.paket];

  return seite({ titel: s.name, nutzer, hier: '/meine-server', inhalt: `
    <a class="klein leise" href="/meine-server">← Meine Server</a>
    <div class="zwischen abstand">
      <h1>${esc(s.name)}</h1>${statusPunkt(s.status)}
    </div>

    <div class="gitter g2 abstand" style="align-items:start">
      <div class="karte">
        <h2>Gebucht</h2>
        <table class="abstand">
          <tr><td class="leise">Paket</td>
            <td>${paket ? paket.zeichen + ' ' + esc(paket.name) : esc(s.paket)}</td></tr>
          <tr><td class="leise">Software</td><td>${esc(s.software)}</td></tr>
          <tr><td class="leise">Adresse</td>
            <td class="mono">${esc(adresse(s))}</td></tr>
          <tr><td class="leise">CPU</td><td>${ergebnis.ausstattung?.cores} Cores</td></tr>
          <tr><td class="leise">Arbeitsspeicher</td><td>${ergebnis.ausstattung?.ram} GB</td></tr>
          <tr><td class="leise">Speicherplatz</td><td>${ergebnis.ausstattung?.ssd} GB</td></tr>
          <tr><td class="leise">Angelegt</td>
            <td class="klein">${esc(String(s.angelegt).slice(0, 10))}</td></tr>
        </table>
        <a class="knopf abstand" href="/panel/${s.id}">Panel öffnen</a>
      </div>

      <div class="karte">
        <h2>Was dieser Server kostet</h2>
        <table class="abstand">
          ${ergebnis.posten.map((p) => `<tr>
            <td>${p.menge > 1 ? p.menge + '× ' : ''}${esc(p.name)}${
              p.imPaket ? ' <span class="klein leise">(im Paket enthalten)</span>' : ''}</td>
            <td class="zahl">${p.summe === 0 ? '<span class="leise">0,00 €</span>' : euro(p.summe)}
              <span class="klein leise">/ ${esc(p.takt)}</span></td>
          </tr>`).join('')}
        </table>
        <div class="summe-zeile summe-gesamt">
          <span>pro Monat</span><strong class="zitrone">${euro(ergebnis.proMonat)}</strong>
        </div>
        ${ergebnis.einmalig > 0 ? `<div class="summe-zeile">
          <span class="leise">dazu einmalig</span><span>${euro(ergebnis.einmalig)}</span>
        </div>` : ''}
        <p class="klein leise abstand">Bezahlt wird in der Schule, bar und im
          Voraus — gegen den Bestellbogen. Das Portal führt darüber
          absichtlich keine Buchhaltung.</p>
      </div>
    </div>` });
}
