/** Was ein angemeldeter Kunde sieht. */
import { esc } from '../web.js';
import { seite, statusPunkt, restBalken } from './layout.js';
import { PAKETE, ZUSATZ, rechne, euro, ARCHIV_TAGE } from '../preise.js';
import { tageBis, zahlungenVon, bestellungenVonKunde } from '../db.js';

export function meineServer(nutzer, server) {
  if (!server.length) {
    return seite({ titel: 'Meine Server', nutzer, hier: '/meine-server', inhalt: `
      <h1>Meine Server</h1>
      <div class="karte abstand">
        <p class="leise">Hier ist noch nichts. Sobald ein Server für dich
        eingerichtet ist, siehst du ihn an dieser Stelle.</p>
        <a class="knopf abstand" href="/konfigurator">Server zusammenstellen</a>
      </div>` });
  }

  const karten = server.map((s) => {
    const paket = PAKETE[s.paket];
    const ergebnis = rechne(s.paket, s.zusatz);
    const tage = tageBis(s.bezahlt_bis);
    const zusatzListe = Object.entries(s.zusatz)
      .map(([id, menge]) => ZUSATZ[id] ? `${menge > 1 ? menge + '× ' : ''}${ZUSATZ[id].name}` : id);

    let warnung = '';
    if (s.status === 'archiviert') {
      warnung = `<div class="hinweis schlecht">Dieser Server ist <strong>archiviert</strong>.
        Sobald die offene Zahlung beglichen ist, läuft er wieder. Ohne Zahlung
        wird er nach ${ARCHIV_TAGE} Tagen dauerhaft gelöscht.</div>`;
    } else if (s.status === 'geloescht') {
      warnung = `<div class="hinweis schlecht">Dieser Server wurde am
        ${esc(s.geloescht_am || '–')} gelöscht.</div>`;
    } else if (tage !== null && tage <= 0) {
      warnung = `<div class="hinweis schlecht">Der bezahlte Zeitraum ist abgelaufen.
        Melde dich beim Team, damit der Server weiterläuft.</div>`;
    } else if (tage !== null && tage <= 7) {
      warnung = `<div class="hinweis warn">Nur noch <strong>${tage} Tag${
        tage === 1 ? '' : 'e'}</strong> bezahlt. Denk ans Verlängern.</div>`;
    }

    return `<article class="karte">
      <div class="zwischen">
        <div>
          <h2>${esc(s.name)}</h2>
          ${s.subdomain
            ? `<div class="mono klein leise">${esc(s.subdomain)}.lemon-servers.de</div>`
            : '<div class="klein leise">keine Subdomain</div>'}
        </div>
        ${statusPunkt(s.status)}
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
      <div class="gitter g2 abstand">
        <div>
          <div class="klein leise">Bezahlt bis</div>
          <strong>${esc(s.bezahlt_bis || '–')}</strong>
          ${restBalken(tage)}
        </div>
        <div>
          <div class="klein leise">Kosten</div>
          <strong>${euro(ergebnis.proMonat)} / Monat</strong>
          <div class="klein leise">Software: ${esc(s.software)}</div>
        </div>
      </div>
      ${zusatzListe.length ? `<div class="abstand">
        <div class="klein leise">Zusatzleistungen</div>
        <div class="klein">${zusatzListe.map(esc).join(' · ')}</div></div>` : ''}
      <a class="knopf stil2 klein abstand" href="/meine-server/${s.id}">Zahlungen ansehen</a>
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

export function serverDetail(nutzer, s) {
  const zahlungen = zahlungenVon(s.id);
  const ergebnis = rechne(s.paket, s.zusatz);

  return seite({ titel: s.name, nutzer, hier: '/meine-server', inhalt: `
    <a class="klein leise" href="/meine-server">← Meine Server</a>
    <div class="zwischen abstand">
      <h1>${esc(s.name)}</h1>${statusPunkt(s.status)}
    </div>

    <div class="karte abstand">
      <h2>Zahlungen</h2>
      ${zahlungen.length ? `<table class="abstand">
        <tr><th>Zeitraum</th><th>Art</th><th class="zahl">Betrag</th><th>Erfasst</th></tr>
        ${zahlungen.map((z) => `<tr>
          <td class="mono">${esc(z.von)} → ${esc(z.bis)}</td>
          <td>${esc(z.art)}</td>
          <td class="zahl">${euro(z.betrag)}</td>
          <td class="klein leise">${esc(z.erfasst_am.slice(0, 10))}${
            z.kassiert_von ? ' · ' + esc(z.kassiert_von) : ''}</td>
        </tr>`).join('')}
      </table>` : '<p class="leise klein abstand">Noch keine Zahlung erfasst.</p>'}
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
    </div>` });
}
