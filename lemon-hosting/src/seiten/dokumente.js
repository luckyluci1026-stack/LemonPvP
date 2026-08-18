/**
 * Die Papierdokumente - vorausgefuellt.
 *
 * Bestellbogen und Loeschbestaetigung gibt es bei Lemon Hosting auf
 * Papier, mit Unterschrift. Das bleibt auch so; unterschrieben wird von
 * Hand. Was das Portal aendert: Alles, was es schon weiss - Name, Klasse,
 * Paket, Zusatzleistungen, Preise - steht beim Ausdrucken bereits drin.
 * Abgeschrieben wird nichts mehr, und die Preisspalte stimmt, weil sie
 * aus derselben Preisliste kommt wie das Portal.
 *
 * Die Felder rund ums Geld bleiben absichtlich leer. Kassiert wird in
 * der Schule, per Hand, und genau dafuer ist der Bogen da - das Portal
 * soll gar nicht erst so tun, als fuehre es Buch.
 */
import { esc } from '../web.js';
import { seite } from './layout.js';
import { PAKETE, ZUSATZ, SOFTWARE, rechne, euro, ARCHIV_TAGE } from '../preise.js';

const linie = (inhalt = '') =>
  `<div class="linie">${inhalt ? esc(inhalt) : ''}</div>`;

const kaestchen = (an, text) =>
  `<div style="margin:.25rem 0">${an ? '☑' : '☐'} ${text}</div>`;

const druckKnopf = `<div class="nicht-drucken" style="text-align:center;margin:1rem">
  <button class="knopf" onclick="print()">Drucken</button></div>`;

export function bestellbogen(nutzer, s, kunde) {
  const ergebnis = rechne(s.paket, s.zusatz);
  const paket = PAKETE[s.paket];

  const zusatzZeilen = ergebnis.posten
    .filter((p) => p.art === 'zusatz')
    .map((p) => `<tr>
      <td>${p.menge > 1 ? p.menge + '× ' : ''}${esc(p.name)}${
        p.imPaket ? ' <span style="color:#666">(im Paket enthalten)</span>' : ''}</td>
      <td style="text-align:right">${euro(p.summe)} / ${esc(p.takt)}</td></tr>`).join('');

  return seite({ titel: 'Bestellbogen', nutzer, kopfExtra: '', inhalt: `
    ${druckKnopf}
    <div class="druck">
      <div class="druck-kopf">
        <div style="font-size:1.6rem;font-weight:800">🍋 LEMON HOSTING</div>
        <div style="color:#555">Minecraft Server – Bestellbogen</div>
      </div>

      <h2>1. Kundendaten</h2>
      <table>
        <tr><td style="width:38%;color:#555">Vorname</td><td>${linie(kunde.vorname)}</td></tr>
        <tr><td style="color:#555">Nachname</td><td>${linie(kunde.nachname)}</td></tr>
        <tr><td style="color:#555">Klasse</td><td>${linie(kunde.klasse)}</td></tr>
        <tr><td style="color:#555">Minecraft-Name</td><td>${linie(kunde.mcname)}</td></tr>
        <tr><td style="color:#555">Kontakt / Discord</td><td>${linie(kunde.kontakt)}</td></tr>
      </table>

      <h2>2. Server</h2>
      <table>
        <tr><td style="width:38%;color:#555">Servername</td><td>${linie(s.name)}</td></tr>
        <tr><td style="color:#555">Subdomain</td>
          <td>${linie(s.subdomain ? s.subdomain + '.lemon-servers.de' : '')}</td></tr>
        <tr><td style="color:#555">Server-ID</td><td>${linie('#' + s.id)}</td></tr>
      </table>

      <h2>3. Serverpaket</h2>
      ${Object.values(PAKETE).map((p) => kaestchen(p.id === s.paket,
        `<strong>${esc(p.name)}</strong> – ${euro(p.preis)} / Monat
         <span style="color:#666">(${p.cores} Cores, ${p.ram} GB RAM, ${p.ssd} GB SSD,
         Subdomain ${p.subdomainInklusive ? 'inklusive' : '+' + euro(ZUSATZ.subdomain.preis)},
         FLFAC ${p.flfacInklusive ? 'auf Anfrage kostenlos' : '+' + euro(ZUSATZ.flfac.preis)})</span>`
      )).join('')}

      <h2>4. Zusatzleistungen</h2>
      ${zusatzZeilen
        ? `<table>${zusatzZeilen}</table>`
        : '<p style="color:#666">Keine Zusatzleistungen gebucht.</p>'}

      <h2>5. Server-Software</h2>
      ${SOFTWARE.map((x) => kaestchen(x === s.software, esc(x))).join('')}
      ${kaestchen(!SOFTWARE.includes(s.software), 'Sonstige: ' + esc(s.software || ''))}

      <h2>6. Zahlung</h2>
      <table>
        <tr><td style="width:38%;color:#555">Zahlungsart</td>
          <td>${linie('Barzahlung / Prepaid')}</td></tr>
        <tr><td style="color:#555">Betrag erhalten</td><td>${linie()}</td></tr>
        <tr><td style="color:#555">Bezahlt für den Zeitraum</td><td>${linie()}</td></tr>
        <tr><td style="color:#555">Kassiert von</td><td>${linie()}</td></tr>
      </table>

      <h2>7. Preisübersicht</h2>
      <table>
        <tr><td>Serverpaket ${esc(paket?.name || s.paket)}</td>
          <td style="text-align:right">${euro(paket?.preis || 0)} / Monat</td></tr>
        <tr><td>Zusatzleistungen</td>
          <td style="text-align:right">${
            euro(Math.round((ergebnis.proMonat - (paket?.preis || 0)) * 100) / 100)} / Monat</td></tr>
        <tr><td>Einmalige Kosten</td>
          <td style="text-align:right">${euro(ergebnis.einmalig)}</td></tr>
        <tr style="border-top:2px solid #111">
          <td style="font-weight:800;font-size:1.15rem;padding-top:.7rem">Gesamt pro Monat</td>
          <td style="text-align:right;font-weight:800;font-size:1.15rem;padding-top:.7rem">
            ${euro(ergebnis.proMonat)}</td></tr>
      </table>
      <p style="color:#666;font-size:.85rem">Enthaltene Ausstattung nach Zusatzleistungen:
        ${ergebnis.ausstattung.cores} CPU-Cores ·
        ${ergebnis.ausstattung.ram} GB RAM ·
        ${ergebnis.ausstattung.ssd} GB NVMe SSD · 10 Gbit/s Uplink</p>

      <h2>8. Wichtige Informationen</h2>
      ${['Der Server läuft grundsätzlich 24/7.',
         'Alle 1–2 Wochen kann eine Wartung von ca. 15–30 Minuten stattfinden.',
         `Bei Zahlungsverzug wird der Server für ${ARCHIV_TAGE} Tage archiviert.`,
         `Nach Ablauf der ${ARCHIV_TAGE} Tage kann der Server dauerhaft gelöscht werden.`,
         'Der Server kann jederzeit gekündigt werden.',
         'Die angegebenen Spielerzahlen sind Richtwerte.',
         'Die Nutzungsregeln wurden gelesen.'].map((t) => kaestchen(false, esc(t))).join('')}

      <h2>9. Interne Nutzungsregeln</h2>
      <p style="font-size:.9rem">Mir ist bekannt, dass unter anderem folgende Dinge
        nicht erlaubt sind: DDoS-Angriffe, Angriffe auf andere Server, absichtliche
        Überlastung der Infrastruktur, Malware oder Viren, absichtliche Beschädigung
        anderer Server sowie das Ausnutzen von Sicherheitslücken zum Schaden anderer.</p>
      ${kaestchen(false, '<strong>Ich akzeptiere die internen Nutzungsregeln.</strong>')}

      <h2>10. Sondervereinbarungen</h2>
      ${s.notiz ? `<p style="font-size:.9rem">${esc(s.notiz)}</p>` : ''}
      ${linie()}${linie()}${linie()}
      <p style="font-size:.85rem;color:#666"><strong>Wichtig:</strong> Nicht
        eingetragene mündliche Sondervereinbarungen gelten nicht als zusätzliche
        Leistung.</p>

      <h2>11. Bestätigung</h2>
      <table style="width:100%">
        <tr>
          <td style="width:50%;vertical-align:top;padding-right:1.5rem">
            <strong>Kunde</strong>
            <div style="color:#555;font-size:.85rem;margin-top:.8rem">Name</div>
            ${linie((kunde.vorname + ' ' + kunde.nachname).trim())}
            <div style="color:#555;font-size:.85rem">Datum</div>${linie()}
            <div style="color:#555;font-size:.85rem">Unterschrift</div>${linie()}
          </td>
          <td style="width:50%;vertical-align:top">
            <strong>Lemon Hosting Team</strong>
            <div style="color:#555;font-size:.85rem;margin-top:.8rem">Name</div>${linie()}
            <div style="color:#555;font-size:.85rem">Datum</div>${linie()}
            <div style="color:#555;font-size:.85rem">Unterschrift</div>${linie()}
          </td>
        </tr>
      </table>

      <p style="text-align:center;color:#666;margin-top:2rem;font-size:.85rem">
        🍋 Lemon Hosting · Einfach. Fair. Transparent.</p>
    </div>
    ${druckKnopf}` });
}

export function loeschbestaetigung(nutzer, s, kunde) {
  const geloescht = s.status === 'geloescht';

  return seite({ titel: 'Löschbestätigung', nutzer, inhalt: `
    ${druckKnopf}
    <div class="druck">
      <div class="druck-kopf">
        <div style="font-size:1.6rem;font-weight:800">🍋 LEMON HOSTING</div>
        <div style="color:#555">Bestätigung der Serverlöschung</div>
      </div>

      <table>
        <tr><td style="width:38%;color:#555">Dokument-Nr.</td>
          <td>${linie('LH-DEL-' + String(s.id).padStart(4, '0'))}</td></tr>
        <tr><td style="color:#555">Löschungsdatum</td>
          <td>${linie(s.geloescht_am || '')}</td></tr>
      </table>

      <h2>Serverinformationen</h2>
      <table>
        <tr><td style="width:38%;color:#555">Servername</td><td>${linie(s.name)}</td></tr>
        <tr><td style="color:#555">Server-ID</td><td>${linie('#' + s.id)}</td></tr>
        <tr><td style="color:#555">Subdomain</td>
          <td>${linie(s.subdomain ? s.subdomain + '.lemon-servers.de' : '—')}</td></tr>
        <tr><td style="color:#555">Server-Owner</td>
          <td>${linie((kunde.vorname + ' ' + kunde.nachname).trim() || kunde.benutzername)}</td></tr>
        <tr><td style="color:#555">Klasse</td><td>${linie(kunde.klasse)}</td></tr>
      </table>

      <h2>Löschung</h2>
      <p style="font-size:.92rem">Hiermit wird bestätigt, dass der oben genannte
        Minecraft-Server durch <strong>Lemon Hosting</strong> gelöscht wurde.</p>
      ${['Server gestoppt', 'Server aus dem Hosting-System entfernt',
         'Serverdaten gelöscht', 'Server nicht mehr erreichbar']
        .map((t) => kaestchen(geloescht, esc(t))).join('')}

      <p style="font-size:.9rem;margin-top:.8rem">Entfernt wurden: Minecraft-Welt,
        Serverdateien, Plugins, Konfigurationen, Servereinstellungen und weitere zum
        Server gehörende Daten.</p>
      <p style="font-size:.85rem;color:#666"><strong>Wichtig:</strong> Die Löschung ist
        grundsätzlich dauerhaft. Eine Wiederherstellung kann nicht garantiert werden.</p>

      <h2>Backup</h2>
      ${['kein Backup erstellt', 'ein Backup erstellt',
         'ein Backup an den Owner übergeben',
         'ein Backup auf Wunsch des Owners nicht erstellt']
        .map((t) => kaestchen(false, esc(t))).join('')}
      ${linie()}

      <h2>Grund der Löschung</h2>
      ${['Löschung auf Wunsch des Owners', 'Ende des Projekts',
         'Server wurde nicht mehr benötigt',
         'Zahlungsverzug nach Ablauf der Archivierungsfrist',
         'Verstoß gegen die internen Nutzungsregeln', 'Sonstiger Grund']
        .map((t) => kaestchen(false, esc(t))).join('')}
      ${linie()}

      <h2>Zahlungsstatus</h2>
      <table>
        <tr><td style="width:38%;color:#555">Letzter bezahlter Zeitraum</td>
          <td>${linie()}</td></tr>
      </table>
      ${kaestchen(false, 'Keine offene Zahlung')}
      ${kaestchen(false, 'Offene Zahlung – Betrag: ______________ €')}

      <h2>Technische Entfernung</h2>
      <table>
        <tr><td style="width:60%">Server aus Panel entfernt</td>
          <td>☐ Ja &nbsp; ☐ Nein</td></tr>
        <tr><td>Subdomain entfernt</td>
          <td>☐ Ja &nbsp; ☐ Nein &nbsp; ${s.subdomain ? '' : '☑'} Nicht vorhanden</td></tr>
        <tr><td>Eigene IP entfernt</td>
          <td>☐ Ja &nbsp; ☐ Nein &nbsp; ${s.zusatz?.eigeneIp ? '' : '☑'} Nicht vorhanden</td></tr>
        <tr><td>FLFAC entfernt</td>
          <td>☐ Ja &nbsp; ☐ Nein &nbsp; ${s.zusatz?.flfac ? '' : '☑'} Nicht eingerichtet</td></tr>
      </table>

      <h2>Bestätigung durch Lemon Hosting</h2>
      <div style="color:#555;font-size:.85rem">Durchgeführt von</div>${linie()}
      <div style="color:#555;font-size:.85rem">Datum</div>${linie()}
      <div style="color:#555;font-size:.85rem">Unterschrift Lemon Hosting</div>${linie()}

      <h2>Übergabe an den Owner</h2>
      <div style="color:#555;font-size:.85rem">Übergeben am</div>${linie()}
      <div style="color:#555;font-size:.85rem">Übergeben an</div>
      ${linie((kunde.vorname + ' ' + kunde.nachname).trim())}
      <div style="color:#555;font-size:.85rem">Unterschrift des Owners</div>${linie()}

      <p style="text-align:center;color:#666;margin-top:2rem;font-size:.85rem">
        🍋 Lemon Hosting · Vorgang abgeschlossen ·
        Dokument-Nr. LH-DEL-${String(s.id).padStart(4, '0')}</p>
    </div>
    ${druckKnopf}` });
}
