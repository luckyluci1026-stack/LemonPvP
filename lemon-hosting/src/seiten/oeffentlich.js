/** Was jeder sehen darf: Pakete, Konfigurator, Regeln, Anmeldung. */
import { esc } from '../web.js';
import { seite, csrfFeld } from './layout.js';
import { PAKETE, ZUSATZ, DOMAINS, SOFTWARE, HARDWARE, ARCHIV_TAGE, rechne, euro }
  from '../preise.js';

export function start(nutzer) {
  const karten = Object.values(PAKETE).map((p) => `
    <article class="karte paket">
      <div class="klein leise">${p.zeichen} Paket</div>
      <h2>${esc(p.name)}</h2>
      <div class="preis zitrone">${euro(p.preis)}</div>
      <div class="klein leise">pro Monat · jederzeit kündbar</div>
      <ul>
        <li><strong>${p.cores}</strong> CPU-Cores</li>
        <li><strong>${p.ram} GB</strong> DDR5 RAM</li>
        <li><strong>${p.ssd} GB</strong> NVMe SSD</li>
        <li><strong>10 Gbit/s</strong> Uplink</li>
        <li>ca. <strong>${esc(p.spieler)}</strong> Spieler</li>
        <li>Subdomain ${p.subdomainInklusive
            ? '<strong>inklusive</strong>'
            : `<strong>+${euro(ZUSATZ.subdomain.preis)}</strong>`}</li>
        <li>FLFAC ${p.flfacInklusive
            ? '<strong>auf Anfrage kostenlos</strong>'
            : `<strong>+${euro(ZUSATZ.flfac.preis)}</strong>`}</li>
      </ul>
      <p class="klein leise abstand">${esc(p.beschreibung)}</p>
      <a class="knopf abstand" href="/konfigurator?paket=${p.id}">Auswählen</a>
    </article>`).join('');

  return seite({ titel: 'Pakete', nutzer, hier: '/', inhalt: `
    <h1>Minecraft Hosting für <span class="zitrone">Klasse, Freunde und Schulprojekte</span></h1>
    <p class="leise" style="max-width:60ch;margin-top:.6rem">
      Auf ${esc(HARDWARE.cpu)}, ${esc(HARDWARE.ram)}, ${esc(HARDWARE.ssd)}
      und ${esc(HARDWARE.uplink)} Uplink. Im Voraus bezahlt, jederzeit kündbar,
      keine Mindestlaufzeit.
    </p>
    <div class="gitter g3 abstand">${karten}</div>

    <h2 class="abstand">Zusatzleistungen</h2>
    <div class="gitter g2 abstand">
      ${gruppenKarten()}
      <article class="karte">
        <h3>Eigene Domain</h3>
        <p class="klein leise">Auf Anfrage, wenn die Domain noch frei ist.</p>
        <table class="abstand">
          ${DOMAINS.map((d) => `<tr><td class="mono">${esc(d.endung)}</td>
            <td class="zahl">ab ${d.abPreis} € / Jahr</td></tr>`).join('')}
        </table>
      </article>
    </div>

    <div class="karte abstand">
      <h3>Wie die Bezahlung läuft</h3>
      <p class="klein leise" style="margin-top:.5rem">
        Prepaid: Du bezahlst im Voraus, der Server läuft für den bezahlten
        Zeitraum. Kündigen kannst du jederzeit, es gibt keine Mindestlaufzeit.
        Bleibt eine Zahlung aus, wird der Server zunächst
        <strong>${ARCHIV_TAGE} Tage archiviert</strong> — in dieser Zeit kannst du
        ihn durch Nachzahlen wieder aktivieren. Danach wird er dauerhaft gelöscht.
      </p>
    </div>` });
}

function gruppenKarten() {
  const gruppen = {};
  for (const z of Object.values(ZUSATZ)) (gruppen[z.gruppe] ??= []).push(z);
  return Object.entries(gruppen).map(([name, liste]) => `
    <article class="karte">
      <h3>${esc(name)}</h3>
      <table class="abstand">
        ${liste.map((z) => `<tr>
          <td>${esc(z.name)}${z.hinweis
            ? `<div class="klein leise">${esc(z.hinweis)}</div>` : ''}</td>
          <td class="zahl">${euro(z.preis)}<span class="klein leise"> / ${
            z.art === 'einmalig' ? 'einmalig' : z.art === 'proWoche' ? 'Woche' : 'Monat'
          }</span></td></tr>`).join('')}
      </table>
    </article>`).join('');
}

export function konfigurator(nutzer, zeichen, vorauswahl = 'coal', meldung = '') {
  const paketWahl = Object.values(PAKETE).map((p) => `
    <label class="karte paket${p.id === vorauswahl ? ' gewaehlt' : ''}"
           style="cursor:pointer;display:block">
      <input type="radio" name="paket" value="${p.id}" data-preis="${p.preis}"
             ${p.id === vorauswahl ? 'checked' : ''}>
      <strong style="font-size:1.05rem">${p.zeichen} ${esc(p.name)}</strong>
      <div class="preis zitrone" style="font-size:1.5rem">${euro(p.preis)}</div>
      <div class="klein leise">${p.cores} Cores · ${p.ram} GB RAM · ${p.ssd} GB SSD</div>
      <div class="klein leise">ca. ${esc(p.spieler)} Spieler</div>
    </label>`).join('');

  const gruppen = {};
  for (const z of Object.values(ZUSATZ)) (gruppen[z.gruppe] ??= []).push(z);

  const zusatzFelder = Object.entries(gruppen).map(([name, liste]) => `
    <h3 class="abstand">${esc(name)}</h3>
    ${liste.map((z) => z.menge ? `
      <div class="feld" style="display:flex;align-items:center;gap:.8rem">
        <div style="flex:1">
          <strong>${esc(z.name)}</strong>
          <div class="klein leise">${euro(z.preis)} / ${
            z.art === 'einmalig' ? 'einmalig' : 'Monat'}${
            z.hinweis ? ' · ' + esc(z.hinweis) : ''}</div>
        </div>
        <input type="number" name="z_${z.id}" value="0" min="0" max="20"
               style="width:80px" data-preis="${z.preis}" data-art="${z.art}" data-id="${z.id}">
      </div>` : `
      <div class="feld">
        <label style="display:flex;align-items:flex-start;cursor:pointer">
          <input type="checkbox" name="z_${z.id}" value="1" style="margin-top:.3rem"
                 data-preis="${z.preis}" data-art="${z.art}" data-id="${z.id}">
          <span><strong>${esc(z.name)}</strong>
            <div class="klein leise">${euro(z.preis)} / ${
              z.art === 'einmalig' ? 'einmalig' : z.art === 'proWoche' ? 'Woche' : 'Monat'}${
              z.hinweis ? ' · ' + esc(z.hinweis) : ''}</div></span>
        </label>
      </div>`).join('')}`).join('');

  return seite({ titel: 'Server bestellen', nutzer, hier: '/konfigurator', inhalt: `
    <h1>Server zusammenstellen</h1>
    <p class="leise">Der Preis rechnet sich unten mit. Was du hier abschickst, ist
      eine <strong>Anfrage</strong> — das Lemon Hosting Team geht sie mit dir durch,
      bevor etwas eingerichtet wird.</p>
    ${meldung ? `<div class="hinweis warn abstand">${esc(meldung)}</div>` : ''}

    <form method="post" action="/konfigurator" id="konfig">
      ${csrfFeld(zeichen)}
      <h2 class="abstand">1. Paket</h2>
      <div class="gitter g3 abstand" id="pakete">${paketWahl}</div>

      <div class="gitter g2 abstand" style="align-items:start">
        <div class="karte">
          <h2>2. Zusatzleistungen</h2>
          ${zusatzFelder}
        </div>

        <div>
          <div class="karte summe">
            <h2>Preis</h2>
            <div id="posten" class="abstand"></div>
            <div class="summe-zeile summe-gesamt">
              <span>pro Monat</span><strong id="proMonat" class="zitrone">–</strong>
            </div>
            <div class="summe-zeile" id="einmaligZeile" style="display:none">
              <span class="leise">davon einmalig</span><span id="einmalig">–</span>
            </div>
            <div class="klein leise abstand" id="ausstattung"></div>
          </div>

          <div class="karte" style="margin-top:1rem">
            <h2>3. Deine Angaben</h2>
            <div class="feld-reihe">
              <div class="feld"><label>Vorname</label>
                <input name="vorname" value="${esc(nutzer?.vorname || '')}" required></div>
              <div class="feld"><label>Nachname</label>
                <input name="nachname" value="${esc(nutzer?.nachname || '')}" required></div>
            </div>
            <div class="feld-reihe">
              <div class="feld"><label>Klasse</label>
                <input name="klasse" value="${esc(nutzer?.klasse || '')}"></div>
              <div class="feld"><label>Minecraft-Name</label>
                <input name="mcname" value="${esc(nutzer?.mcname || '')}"></div>
            </div>
            <div class="feld"><label>Kontakt / Discord</label>
              <input name="kontakt" value="${esc(nutzer?.kontakt || '')}"></div>
            <div class="feld"><label>Servername</label>
              <input name="servername" placeholder="z. B. Klassenserver 7a" required></div>
            <div class="feld"><label>Gewünschte Subdomain</label>
              <input name="subdomain" placeholder="klasse7" class="mono">
              <div class="klein leise" style="margin-top:.3rem">wird zu
                <span class="mono">deinname.lemon-servers.de</span></div></div>
            <div class="feld"><label>Server-Software</label>
              <select name="software">
                ${SOFTWARE.map((s) => `<option${s === 'Paper' ? ' selected' : ''}>${esc(s)}</option>`).join('')}
              </select></div>
            <div class="feld"><label>Sonderwünsche</label>
              <textarea name="wunsch" rows="3"
                placeholder="Alles, was sonst noch wichtig ist"></textarea></div>
            <label style="display:flex;align-items:flex-start;margin-bottom:1rem;cursor:pointer">
              <input type="checkbox" name="regeln" required style="margin-top:.3rem">
              <span class="klein">Ich habe die <a href="/regeln" target="_blank">Nutzungsregeln</a>
                gelesen und akzeptiere sie.</span>
            </label>
            <button class="knopf" style="width:100%;justify-content:center">
              Anfrage abschicken
            </button>
          </div>
        </div>
      </div>
    </form>
    <script src="/konfig.js"></script>` });
}

export function regeln(nutzer) {
  return seite({ titel: 'Regeln', nutzer, hier: '/regeln', inhalt: `
    <div class="eng">
    <h1>Nutzungsregeln</h1>
    <p class="leise">Damit alle die gleichen Bedingungen haben und die Server
      zuverlässig laufen.</p>

    <div class="karte abstand"><h3>Preise &amp; Leistungen</h3>
      <p class="klein leise">Es gelten die Preise der aktuellen Preisliste. Alle
      Kunden bekommen dieselben Grundpreise. Sonderpreise oder zusätzliche
      Leistungen müssen vorher abgesprochen und <strong>schriftlich auf dem
      Bestellbogen</strong> festgehalten werden — mündliche Absprachen gelten
      nicht als zusätzliche Leistung.</p></div>

    <div class="karte"><h3>Prepaid</h3>
      <p class="klein leise">Lemon Hosting wird im Voraus bezahlt, beim
      Schulangebot per Bargeld. Der Server ist für den bezahlten Zeitraum
      freigeschaltet.</p></div>

    <div class="karte"><h3>Kündigung</h3>
      <p class="klein leise">Jederzeit möglich, keine Mindestlaufzeit.</p></div>

    <div class="karte"><h3>Wenn nicht bezahlt wird</h3>
      <p class="klein leise">Der Server wird zunächst <strong>${ARCHIV_TAGE} Tage
      archiviert</strong>. In dieser Zeit kann nachgezahlt werden. Danach kann er
      dauerhaft gelöscht werden — eine Wiederherstellung ist dann möglicherweise
      nicht mehr möglich.</p></div>

    <div class="karte"><h3>Serverbetrieb &amp; Wartung</h3>
      <p class="klein leise">Die Server laufen grundsätzlich 24/7. Kurze
      Offline-Zeiten können durch Crashs, Updates, Wartung oder Probleme mit
      Minecraft und Plugins entstehen. Alle 1–2 Wochen prüft das Team die
      Server, das dauert etwa 15–30 Minuten und wird nach Möglichkeit vorher
      angekündigt.</p></div>

    <div class="karte"><h3>Erlaubt</h3>
      <p class="klein leise">Klassenserver, Survival, PvP, Minigames,
      Schulprojekte und private Minecraft-Projekte.</p></div>

    <div class="karte"><h3>Nicht erlaubt</h3>
      <ul class="klein leise" style="margin:.5rem 0 0 1.1rem">
        <li>DDoS-Angriffe oder andere Angriffe auf Netzwerke und Server</li>
        <li>Angriffe auf andere Kunden oder deren Server</li>
        <li>absichtliche Überlastung der Infrastruktur</li>
        <li>Malware, Viren oder andere schädliche Software</li>
        <li>Ausnutzen von Sicherheitslücken zum Schaden anderer</li>
        <li>illegale Inhalte oder betrügerische Nutzung</li>
        <li>Weitergabe von Zugangsdaten an unbefugte Personen</li>
      </ul>
      <p class="klein leise abstand">Je nach Schwere kann ein Server vorübergehend
      gesperrt, dauerhaft gesperrt oder gelöscht werden. Bei schweren Verstößen
      auch sofort und ohne Ankündigung.</p></div>

    <div class="karte"><h3>Zugangsdaten</h3>
      <p class="klein leise">Jeder ist selbst dafür verantwortlich, seine
      Zugangsdaten sicher aufzubewahren. Sind sie bekannt geworden, sag dem
      Lemon Hosting Team Bescheid.</p></div>

    <div class="karte"><h3>Support</h3>
      <p class="klein leise">Bei Serverproblemen, Konfiguration, Plugins,
      Performance und FLFAC hilft das Team — im Rahmen der verfügbaren Zeiten.
      Das ist kein 24/7-Support.</p></div>
    </div>` });
}

export function anmelden(zeichen, meldung = '', name = '') {
  return seite({ titel: 'Anmelden', nutzer: null, inhalt: `
    <div class="eng" style="margin:3rem auto">
      <h1>Anmelden</h1>
      <p class="leise">Zugangsdaten bekommst du vom Lemon Hosting Team.</p>
      ${meldung ? `<div class="hinweis schlecht abstand">${esc(meldung)}</div>` : ''}
      <form method="post" action="/anmelden" class="karte abstand">
        ${csrfFeld(zeichen)}
        <div class="feld"><label>Benutzername</label>
          <input name="benutzername" value="${esc(name)}" autofocus required></div>
        <div class="feld"><label>Passwort</label>
          <input type="password" name="passwort" required></div>
        <button class="knopf" style="width:100%;justify-content:center">Anmelden</button>
      </form>
      <p class="klein leise abstand">Noch keinen Zugang? Stell dir erst
        <a href="/konfigurator">einen Server zusammen</a> — den Rest klären wir
        persönlich.</p>
    </div>` });
}

export function danke(nutzer, nummer, ergebnis) {
  return seite({ titel: 'Anfrage eingegangen', nutzer, inhalt: `
    <div class="eng" style="margin:3rem auto">
      <div class="hinweis info">
        <strong>Anfrage ${nummer} ist eingegangen.</strong>
      </div>
      <div class="karte">
        <h2>Wie es weitergeht</h2>
        <p class="klein leise abstand">Das Lemon Hosting Team geht die Anfrage mit
        dir durch, ihr füllt gemeinsam den Bestellbogen aus, und danach wird der
        Server eingerichtet. Bezahlt wird im Voraus.</p>
        <div class="summe-zeile summe-gesamt abstand">
          <span>voraussichtlich pro Monat</span>
          <strong class="zitrone">${euro(ergebnis.proMonat)}</strong>
        </div>
        ${ergebnis.einmalig > 0 ? `<div class="summe-zeile">
          <span class="leise">dazu einmalig</span><span>${euro(ergebnis.einmalig)}</span>
        </div>` : ''}
      </div>
      <a class="knopf stil2 abstand" href="/">Zurück zur Übersicht</a>
    </div>` });
}
