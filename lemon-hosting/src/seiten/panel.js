/**
 * Das Panel - die Seite, wegen der es das Portal ueberhaupt gibt.
 *
 * Ein Kunde kommt hier rein und kann seinen Server starten, stoppen, in
 * die Konsole schauen, Befehle tippen und an die Dateien. Alles andere
 * im Portal ist Verwaltung drumherum.
 *
 * Zwei Betriebsarten:
 *
 *   - Der Normalfall: das Portal startet den Server selbst.
 *   - Hat der Server eine Pterodactyl-Adresse hinterlegt, laeuft er dort.
 *     Dann zeigt das Panel keine Knoepfe, sondern verlinkt hinueber -
 *     zwei Stellen, die denselben Server starten duerfen, waeren ein
 *     Rezept fuer kaputte Welten.
 *
 * Die Konsole wird nicht hier gefuellt, sondern live von konsole.js -
 * deshalb steht das <pre> fast leer da und bekommt nur eine id.
 */
import { esc } from '../web.js';
import { seite, csrfFeld } from './layout.js';
import { PAKETE, rechne } from '../preise.js';
import { speicherMB, jarDa, eulaAngenommen, bildVon } from '../panel.js';
import { vorhanden as dockerDa, bildDa } from '../docker.js';
import { lesbareGroesse } from '../dateien.js';
import { WIE_VIELE } from '../sicherung.js';
import { artenListe } from '../arten.js';
import { naechster } from '../zeitplan.js';

const TEXTE = { laeuft: 'läuft', startet: 'startet …', stoppt: 'stoppt …',
                gestoppt: 'gestoppt', unbekannt: 'nicht erreichbar' };
const FARBEN = { laeuft: 'aktiv', startet: 'archiviert', stoppt: 'archiviert',
                 gestoppt: 'geloescht', unbekannt: 'archiviert' };

function statusAnzeige(zustand) {
  return `<span id="statusmarke" class="marke-punkt ${FARBEN[zustand.status] || 'geloescht'}"
    >${esc(TEXTE[zustand.status] || zustand.status)}</span>`;
}

function laufzeit(sekunden) {
  if (!sekunden) return '–';
  const h = Math.floor(sekunden / 3600), m = Math.floor((sekunden % 3600) / 60);
  return h ? `${h} h ${m} min` : `${m} min ${sekunden % 60} s`;
}

/**
 * Die drei Knoepfe.
 *
 * `data-was` steht dran, damit konsole.js sie beim Statuswechsel scharf
 * bzw. stumpf schalten kann, ohne die Seite neu zu laden.
 */
const knoepfe = (s, zustand, zeichen) => {
  const aus = zustand.status === 'gestoppt';
  return `<form method="post" action="/panel/${s.id}/aktion" class="reihe">
    ${csrfFeld(zeichen)}
    <button name="was" value="start" data-was="start" class="knopf klein"
      ${aus ? '' : 'disabled'}>▶ Starten</button>
    <button name="was" value="stopp" data-was="stopp" class="knopf stil2 klein"
      ${aus ? 'disabled' : ''}>■ Stoppen</button>
    <button name="was" value="neustart" data-was="neustart" class="knopf stil2 klein"
      ${aus ? 'disabled' : ''}>↻ Neustart</button>
  </form>`;
};

/**
 * Woran verbindet man sich?
 *
 * Die Subdomain wenn es eine gibt, sonst der Rechnername. Den Port immer
 * dazu, ausser er ist der Standard - ":25565" tippt sowieso niemand, und
 * bei allen anderen ist er das Entscheidende.
 */
export function adresse(s) {
  const wirt = s.subdomain
    ? `${s.subdomain}.lemon-servers.de`
    : (process.env.SERVER_HOST || 'dein-rechner');
  return Number(s.port) === 25565 || !s.port ? wirt : `${wirt}:${s.port}`;
}

/** Kopfzeile mit Name, Subdomain und Statuspunkt - auf beiden Panelarten gleich. */
const kopf = (s, rechts) => `
  <a class="klein leise" href="/meine-server">← Meine Server</a>
  <div class="zwischen abstand">
    <div>
      <h1>${esc(s.name)}</h1>
      <div class="mono klein leise">${esc(adresse(s))}</div>
    </div>
    ${rechts}
  </div>`;

/**
 * Server liegt in Pterodactyl - hier gibt es nichts zu bedienen.
 *
 * Absichtlich keine Konsole und keine Dateien: Was Pterodactyl verwaltet,
 * verwaltet Pterodactyl. Das Portal ist hier nur die Eingangstuer.
 */
export function fremdesPanel(nutzer, s, ziel) {
  const aus = rechne(s.paket, s.zusatz).ausstattung;

  return seite({ titel: s.name, nutzer, hier: '/meine-server', inhalt: `
    ${kopf(s, '<span class="marke-punkt aktiv">Pterodactyl</span>')}

    <div class="karte abstand">
      <h2>Dieser Server läuft in Pterodactyl</h2>
      <p class="leise abstand">Start, Stopp, Konsole und Dateien machst du
        dort. Das Portal verwaltet ihn nicht selbst — zwei Stellen, die
        denselben Server starten dürfen, wären ein Rezept für kaputte
        Welten.</p>
      <a class="knopf abstand" href="${esc(ziel)}" target="_blank" rel="noopener">
        In Pterodactyl öffnen ↗</a>
    </div>

    <div class="gitter g4 abstand">
      <div class="karte"><div class="klein leise">Paket</div>
        <strong>${PAKETE[s.paket]
          ? PAKETE[s.paket].zeichen + ' ' + esc(PAKETE[s.paket].name) : esc(s.paket)}</strong></div>
      <div class="karte"><div class="klein leise">CPU</div>
        <strong>${aus.cores} Cores</strong></div>
      <div class="karte"><div class="klein leise">Arbeitsspeicher</div>
        <strong>${aus.ram} GB</strong></div>
      <div class="karte"><div class="klein leise">Speicherplatz</div>
        <strong>${aus.ssd} GB</strong></div>
    </div>

    <a class="knopf stil2 abstand" href="/meine-server/${s.id}">Was ist gebucht?</a>` });
}

export function panel(nutzer, s, zustand, zeichen, belegt, meldung = '',
                      gut = false, sicherungen = [], plugins = [],
                      knotenName = 'dieser Rechner', versionen = null) {
  const paket = PAKETE[s.paket];
  // Laeuft der Server gerade, zaehlt was tatsaechlich laeuft; sonst was
  // beim naechsten Start passieren wuerde.
  // Bei einem entfernten Server sagt uns nur der Daemon, wie er laeuft -
  // vorab raten waere geraten. Ohne Antwort steht deshalb nichts da.
  const fern = Number(s.knoten_id) > 0;
  const container = zustand.motor
    ? zustand.motor === 'docker'
    : (fern ? null : (dockerDa().geht && bildDa(bildVon(s))));
  const aus = rechne(s.paket, s.zusatz).ausstattung;
  const bereit = jarDa(s.id);
  const eula = eulaAngenommen(s.id);
  const platz = aus.ssd * 1073741824;
  const anteil = platz ? Math.min(100, (belegt / platz) * 100) : 0;
  const eingabeAus = zustand.status !== 'laeuft' && zustand.status !== 'startet';

  return seite({ titel: s.name, nutzer, hier: '/meine-server', inhalt: `
    ${kopf(s, statusAnzeige(zustand))}
    ${meldung ? `<div class="hinweis ${gut ? 'info' : 'warn'} abstand">${
      esc(meldung)}</div>` : ''}
    ${s.status !== 'aktiv' ? `<div class="hinweis schlecht abstand">
      Dieser Server ist <strong>${esc(s.status)}</strong> und lässt sich nicht
      starten. Sprich das Team an.</div>` : ''}

    ${!bereit ? `<div class="hinweis warn abstand">
      <strong>Es fehlt noch die Serversoftware.</strong> Wähl unten unter
      <a href="#software">Serversoftware</a> eine Art und Version aus — das
      Panel lädt sie selbst. Oder lade unter
      <a href="/panel/${s.id}/dateien">Dateien</a> eine eigene
      <span class="mono">server.jar</span> hoch.</div>` : ''}
    ${bereit && !eula ? `<div class="hinweis warn abstand">
      <form method="post" action="/panel/${s.id}/aktion" class="reihe">
        ${csrfFeld(zeichen)}
        <span style="flex:1">Vor dem ersten Start muss die
          <a href="https://aka.ms/MinecraftEULA" target="_blank" rel="noopener"
            >Minecraft-EULA</a> angenommen werden — das schreibt Mojang so vor.</span>
        <button name="was" value="eula" class="knopf klein">Ich akzeptiere die EULA</button>
      </form></div>` : ''}

    <div class="gitter g4 abstand">
      <div class="karte"><div class="klein leise">Paket</div>
        <strong>${paket ? paket.zeichen + ' ' + esc(paket.name) : esc(s.paket)}</strong>
        <div class="klein leise">${aus.cores} CPU-Cores</div></div>
      <div class="karte"><div class="klein leise">Arbeitsspeicher</div>
        <strong>${(speicherMB(s) / 1024).toFixed(2)} GB</strong>
        <div class="klein leise">${container === null ? esc(knotenName)
          : container ? 'harte Grenze im Container' : 'nur eine JVM-Einstellung'}</div></div>
      <div class="karte"><div class="klein leise">Speicherplatz</div>
        <strong>${lesbareGroesse(belegt)}</strong>
        <div class="klein leise">von ${aus.ssd} GB</div>
        <div class="balken ${anteil > 90 ? 'aus' : anteil > 75 ? 'knapp' : ''}"
          ><i style="width:${anteil.toFixed(1)}%"></i></div></div>
      <div class="karte"><div class="klein leise">Läuft seit</div>
        <strong id="laufzeit">${laufzeit(zustand.laufzeit)}</strong>
        <div class="klein leise" id="pid">${
          zustand.pid ? 'Prozess ' + zustand.pid : 'nicht gestartet'}</div></div>
    </div>

    <div class="gitter g3 abstand" id="verbrauch">
      <div class="karte"><div class="klein leise">CPU-Auslastung</div>
        <strong id="cpu">–</strong>
        <div class="klein leise">von ${aus.cores} Cores</div>
        <div class="balken"><i id="cpuBalken" style="width:0"></i></div></div>
      <div class="karte"><div class="klein leise">Arbeitsspeicher in Benutzung</div>
        <strong id="ram">–</strong>
        <div class="klein leise">von ${(speicherMB(s) / 1024).toFixed(2)} GB</div>
        <div class="balken"><i id="ramBalken" style="width:0"></i></div></div>
      <div class="karte"><div class="klein leise">Netzwerk</div>
        <strong id="netz">–</strong>
        <div class="klein leise" id="netzHinweis">seit dem Start</div></div>
    </div>

    <div class="karte abstand">
      <div class="zwischen">
        <div>
          <div class="klein leise">Zum Verbinden in Minecraft</div>
          <strong class="mono" style="font-size:1.05rem">${esc(adresse(s))}</strong>
        </div>
        <div style="text-align:right">
          <div class="klein leise">Online</div>
          <strong id="spielerzahl">${zustand.spieler?.length || 0}</strong>
        </div>
      </div>
      <div class="klein abstand" id="spieler">${
        zustand.spieler?.length
          ? zustand.spieler.map((n) => `<span class="spieler">${esc(n)}</span>`).join('')
          : '<span class="leise">Gerade ist niemand drauf.</span>'}</div>
    </div>

    <div class="karte abstand">
      <div class="zwischen">
        <h2>Konsole</h2>
        <div class="reihe">
          ${knoepfe(s, zustand, zeichen)}
          <a class="knopf stil2 klein" href="/panel/${s.id}/dateien">📁 Dateien</a>
        </div>
      </div>

      <pre id="konsole" class="konsole abstand">Verbinde …</pre>

      <form id="befehlform" class="reihe" style="margin-top:.7rem"
            action="/panel/${s.id}/befehl" method="post">
        ${csrfFeld(zeichen)}
        <input name="befehl" id="befehl" class="mono" autocomplete="off"
               placeholder="Befehl eingeben, z. B. say Hallo   ·   ↑ holt den letzten"
               style="flex:1" ${eingabeAus ? 'disabled' : ''}>
        <button class="knopf klein" ${eingabeAus ? 'disabled' : ''}>Senden</button>
      </form>
      <p class="klein leise" style="margin-top:.6rem">
        Befehle gehen direkt in die Serverkonsole — ohne führenden Schrägstrich,
        also <span class="mono">op DeinName</span>, nicht <span class="mono">/op</span>.
      </p>
    </div>
    ${fern ? `<div class="hinweis info abstand">
      <strong>Läuft auf ${esc(knotenName)}.</strong> Das Portal steuert diesen
      Server über den Daemon auf der anderen Maschine — Konsole, Dateien und
      Backups gehen hier durch, liegen aber dort.
      ${zustand.status === 'unbekannt'
        ? ' <strong>Der Knoten meldet sich gerade nicht</strong> – läuft dort'
          + ' <span class="mono">node daemon.js</span>?' : ''}
    </div>` : ''}

    ${container === null ? '' : `<div class="hinweis ${container ? 'info' : 'warn'} abstand">
      ${container
        ? `<strong>Läuft im Container.</strong> ${
            (speicherMB(s) / 1024).toFixed(2)} GB Arbeitsspeicher und
           ${aus.cores} CPU-Kerne sind hart begrenzt — dieser Server kann die
           anderen nicht ausbremsen. Image:
           <span class="mono">${esc(bildVon(s))}</span>`
        : `<strong>Läuft ohne Container.</strong> Der Arbeitsspeicher ist nur
           eine Einstellung der JVM, keine Grenze — ein Server mit
           Speicherleck zieht die ganze Maschine mit runter. Mit Docker und
           dem Image <span class="mono">${esc(bildVon(s))}</span> wäre die
           Grenze echt.`}
    </div>`}

    <div class="karte abstand" id="software">
      <div class="zwischen">
        <h2>Serversoftware</h2>
        ${s.art ? `<span class="klein leise">${
          esc(artenListe().find((a) => a.id === s.art)?.name || s.art)}${
          s.mc_version ? ' ' + esc(s.mc_version) : ''} installiert</span>` : ''}
      </div>
      <p class="klein leise" style="margin-top:.5rem">
        Art und Version auswählen, das Panel holt die
        <span class="mono">server.jar</span> beim Hersteller. Die alte wird erst
        ersetzt, wenn die neue vollständig da ist.
        ${bereit ? ' Nach dem Wechsel einmal <strong>Neustart</strong> drücken.' : ''}</p>

      <form method="post" action="/panel/${s.id}/software" class="abstand">
        ${csrfFeld(zeichen)}
        <div class="feld-reihe">
          <div class="feld"><label>Art</label>
            <select name="art" onchange="this.form.submit()">
              <option value="">— auswählen —</option>
              ${artenListe().map((a) => `<option value="${a.id}"${
                s.art === a.id ? ' selected' : ''}>${esc(a.name)}${
                a.empfohlen ? ' (empfohlen)' : ''}</option>`).join('')}
            </select>
            ${s.art ? `<p class="klein leise" style="margin-top:.3rem">${
              esc(artenListe().find((a) => a.id === s.art)?.beschreibung || '')}</p>` : ''}
          </div>
          <div class="feld"><label>Version</label>
            ${versionen?.liste?.length ? `<select name="version">
              ${versionen.liste.slice(0, 60).map((v) => `<option${
                s.mc_version === v ? ' selected' : ''}>${esc(v)}</option>`).join('')}
            </select>` : `<input name="version" placeholder="1.21.11"
              value="${esc(s.mc_version || '')}">`}
            ${versionen?.fehler ? `<p class="klein leise" style="margin-top:.3rem">${
              esc(versionen.fehler)} Du kannst die Version von Hand eintippen.</p>`
              : versionen?.veraltet ? '<p class="klein leise" style="margin-top:.3rem">'
                + 'Ältere Liste – der Hersteller war gerade nicht erreichbar.</p>' : ''}
          </div>
        </div>
        <button name="was" value="installieren" class="knopf"
          ${s.art ? '' : 'disabled'}>${bereit ? 'Neu installieren' : 'Installieren'}</button>
        ${bereit ? `<span class="klein leise" style="margin-left:.6rem">
          Ersetzt nur die <span class="mono">server.jar</span> – Welt, Plugins
          und Einstellungen bleiben.</span>` : ''}
      </form>
    </div>

    <div class="karte abstand">
      <div class="zwischen">
        <h2>Plugins</h2>
        <span class="klein leise">${plugins.filter((p) => p.da).length} von ${
          plugins.length} installiert</span>
      </div>
      ${plugins.length ? `<table class="abstand">
        ${plugins.map((p) => `<tr>
          <td><strong>${esc(p.name)}</strong>
            ${p.beschreibung ? `<div class="klein leise">${esc(p.beschreibung)}</div>` : ''}
            <div class="klein leiser mono">${esc(p.datei)} · ${esc(p.groesse)}</div></td>
          <td class="zahl" style="white-space:nowrap">
            ${p.da ? `<form method="post" action="/panel/${s.id}/plugin/entfernen">
                ${csrfFeld(zeichen)}
                <input type="hidden" name="datei" value="${esc(p.datei)}">
                <span class="marke-punkt aktiv" style="margin-right:.4rem">drin</span>
                <button class="knopf gefahr klein">Entfernen</button></form>`
              : `<form method="post" action="/panel/${s.id}/plugin/installieren">
                ${csrfFeld(zeichen)}
                <input type="hidden" name="datei" value="${esc(p.datei)}">
                <button class="knopf klein">Installieren</button></form>`}
          </td></tr>`).join('')}
      </table>
      <p class="klein leise abstand">Nach dem Installieren oder Entfernen einmal
        <strong>Neustart</strong> drücken — Plugins werden nur beim Start
        geladen.</p>`
      : `<p class="leise klein abstand">Im Katalog liegen keine Plugins.
         Leg <span class="mono">.jar</span>-Dateien in den Katalogordner oder
         setz <span class="mono">KATALOG_DIR</span>.</p>`}
    </div>

    <div class="karte abstand">
      <div class="zwischen">
        <h2>Zeitplan</h2>
        <span class="klein leise">läuft, solange das Portal läuft</span>
      </div>
      <form method="post" action="/panel/${s.id}/zeitplan" class="abstand">
        ${csrfFeld(zeichen)}
        <div class="feld-reihe">
          <div class="feld"><label>Jede Nacht neu starten um</label>
            <input name="neustart_um" type="time" value="${esc(s.neustart_um || '')}">
            <div class="klein leise" style="margin-top:.3rem">${
              s.neustart_um ? 'nächster: ' + esc(naechster(s.neustart_um))
                            : 'leer lassen = kein automatischer Neustart'}</div></div>
          <div class="feld"><label>Jeden Tag sichern um</label>
            <input name="sicherung_um" type="time" value="${esc(s.sicherung_um || '')}">
            <div class="klein leise" style="margin-top:.3rem">${
              s.sicherung_um ? 'nächstes: ' + esc(naechster(s.sicherung_um))
                             : 'leer lassen = kein automatisches Backup'}</div></div>
        </div>
        <button class="knopf klein">Zeitplan speichern</button>
        <p class="klein leise" style="margin-top:.6rem">
          Ein Neustart wird übersprungen, wenn der Server ohnehin aus ist —
          er wird nicht heimlich hochgefahren. Vor einem automatischen Backup
          schickt das Panel <span class="mono">save-all</span>, damit die Welt
          wirklich auf der Platte steht.</p>
      </form>
    </div>

    <div class="karte abstand">
      <div class="zwischen">
        <h2>Backups</h2>
        <form method="post" action="/panel/${s.id}/sicherung">
          ${csrfFeld(zeichen)}
          <button class="knopf klein">💾 Backup jetzt anlegen</button>
        </form>
      </div>
      <p class="klein leise" style="margin-top:.5rem">
        Packt den ganzen Serverordner in eine ZIP-Datei. Die letzten
        ${WIE_VIELE} werden aufgehoben, ältere fallen von selbst weg.
        Fürs Zurückspielen muss der Server gestoppt sein.</p>

      ${sicherungen.length ? `<table class="abstand">
        <tr><th>Wann</th><th class="zahl">Größe</th><th></th></tr>
        ${sicherungen.map((b) => `<tr>
          <td>${esc(b.wann)}</td>
          <td class="zahl klein">${esc(b.lesbar)}</td>
          <td class="zahl"><div class="reihe" style="justify-content:flex-end">
            <a class="knopf stil2 klein"
               href="/panel/${s.id}/sicherung/laden?f=${encodeURIComponent(b.name)}"
               >Herunterladen</a>
            <form method="post" action="/panel/${s.id}/sicherung/zurueck"
                  onsubmit="return confirm('Backup vom ${esc(b.wann)} zurückspielen? Neuere Dateien mit gleichem Namen werden überschrieben.')">
              ${csrfFeld(zeichen)}
              <input type="hidden" name="f" value="${esc(b.name)}">
              <button class="knopf stil2 klein" ${
                zustand.status === 'gestoppt' ? '' : 'disabled'}>Zurückspielen</button>
            </form>
            <form method="post" action="/panel/${s.id}/sicherung/loeschen"
                  onsubmit="return confirm('Dieses Backup löschen?')">
              ${csrfFeld(zeichen)}
              <input type="hidden" name="f" value="${esc(b.name)}">
              <button class="knopf gefahr klein">Löschen</button>
            </form>
          </div></td>
        </tr>`).join('')}
      </table>` : '<p class="leise klein abstand">Noch kein Backup vorhanden.</p>'}
    </div>

    <script>window.SERVER_ID = ${s.id};
      window.SERVER_CORES = ${aus.cores};
      window.SERVER_RAM_MB = ${speicherMB(s)};</script>
    <script src="/konsole.js"></script>` });
}

// ------------------------------------------------------------------ Dateien

function brotkrumen(s, teile) {
  const stuecke = [`<a href="/panel/${s.id}/dateien">${esc(s.name)}</a>`];
  teile.forEach((t, i) => {
    stuecke.push(`<a href="/panel/${s.id}/dateien?p=${
      encodeURIComponent(teile.slice(0, i + 1).join('/'))}">${esc(t)}</a>`);
  });
  return stuecke.join('<span class="leiser">/</span>');
}

export function dateien(nutzer, s, pfad, eintraege, zeichen, meldung = '', gut = false) {
  const teile = pfad ? pfad.split('/').filter(Boolean) : [];

  const hoch = teile.length
    ? `<tr><td colspan="4"><a href="/panel/${s.id}/dateien?p=${
        encodeURIComponent(teile.slice(0, -1).join('/'))}">↰ eine Ebene höher</a></td></tr>`
    : '';

  const zeile = (e) => {
    const voll = pfad ? pfad + '/' + e.name : e.name;
    const link = e.ordner
      ? `/panel/${s.id}/dateien?p=${encodeURIComponent(voll)}`
      : e.bearbeitbar ? `/panel/${s.id}/bearbeiten?p=${encodeURIComponent(voll)}` : null;
    return `<tr>
      <td><div class="name">${e.ordner ? '📁' : '📄'}
        ${link ? `<a href="${link}">${esc(e.name)}</a>` : esc(e.name)}</div></td>
      <td class="zahl klein">${e.ordner ? '–' : lesbareGroesse(e.groesse)}</td>
      <td class="klein leise">${esc(e.geaendert)}</td>
      <td class="zahl"><form method="post" action="/panel/${s.id}/loeschen"
            onsubmit="return confirm('${esc(e.name)} wirklich löschen?')">
        ${csrfFeld(zeichen)}
        <input type="hidden" name="p" value="${esc(voll)}">
        <button class="knopf gefahr klein">Löschen</button></form></td>
    </tr>`;
  };

  return seite({ titel: 'Dateien · ' + s.name, nutzer, hier: '/meine-server', inhalt: `
    <a class="klein leise" href="/panel/${s.id}">← Panel</a>
    <h1 class="abstand">Dateien</h1>
    <div class="pfadleiste klein abstand">${brotkrumen(s, teile)}</div>
    ${meldung ? `<div class="hinweis ${gut ? 'info' : 'warn'} abstand">${
      esc(meldung)}</div>` : ''}

    <div class="karte abstand"><table>
      <tr><th>Name</th><th class="zahl">Größe</th><th>Geändert</th><th></th></tr>
      ${hoch}
      ${eintraege.map(zeile).join('')
        || '<tr><td colspan="4" class="leise">Der Ordner ist leer.</td></tr>'}
    </table></div>

    <div class="karte abstand" id="ablage" data-server="${s.id}"
         data-pfad="${esc(pfad)}" data-csrf="${esc(zeichen)}">
      <div class="ablage">
        <strong>Dateien hierher ziehen</strong>
        <div class="klein leise" style="margin:.3rem 0 .8rem">
          oder auswählen — auch mehrere auf einmal. Die
          <span class="mono">server.jar</span> gehört in den obersten Ordner,
          Plugins nach <span class="mono">plugins/</span>.</div>
        <label class="knopf stil2 klein" style="display:inline-flex;margin:0">
          Datei auswählen<input type="file" id="dateiwahl" multiple></label>
      </div>
      <div hidden style="margin-top:.9rem">
        <div class="balken" id="fortschritt"><i style="width:0"></i></div>
        <div class="klein leise" id="fortschrittText" style="margin-top:.4rem"></div>
      </div>
    </div>

    <div class="gitter g2 abstand">
      <form method="post" action="/panel/${s.id}/neu" class="karte">
        ${csrfFeld(zeichen)}
        <input type="hidden" name="p" value="${esc(pfad)}">
        <h2>Textdatei anlegen</h2>
        <div class="feld abstand"><label>Dateiname</label>
          <input name="name" class="mono" placeholder="server.properties" required></div>
        <div class="feld"><label>Inhalt</label>
          <textarea name="inhalt" rows="5" class="mono"></textarea></div>
        <button class="knopf">Anlegen</button>
      </form>
      <form method="post" action="/panel/${s.id}/ordner" class="karte">
        ${csrfFeld(zeichen)}
        <input type="hidden" name="p" value="${esc(pfad)}">
        <h2>Ordner anlegen</h2>
        <div class="feld abstand"><label>Name</label>
          <input name="name" class="mono" placeholder="plugins" required></div>
        <button class="knopf stil2">Anlegen</button>
      </form>
    </div>
    <script src="/dateien.js"></script>` });
}

export function bearbeiten(nutzer, s, pfad, inhalt, zeichen, meldung = '', gut = false) {
  const zurueck = `/panel/${s.id}/dateien?p=${
    encodeURIComponent(pfad.split('/').slice(0, -1).join('/'))}`;

  return seite({ titel: pfad, nutzer, hier: '/meine-server', inhalt: `
    <a class="klein leise" href="${zurueck}">← Dateien</a>
    <h1 class="abstand mono" style="font-size:1.25rem">${esc(pfad)}</h1>
    ${meldung ? `<div class="hinweis ${gut ? 'info' : 'warn'} abstand">${
      esc(meldung)}</div>` : ''}
    <form method="post" action="/panel/${s.id}/speichern" class="karte abstand">
      ${csrfFeld(zeichen)}
      <input type="hidden" name="p" value="${esc(pfad)}">
      <textarea name="inhalt" rows="24" class="mono" spellcheck="false"
        style="line-height:1.55">${esc(inhalt)}</textarea>
      <div class="reihe abstand">
        <button class="knopf">Speichern</button>
        <a class="knopf stil2" href="${zurueck}">Abbrechen</a>
        <span class="klein leise">Änderungen an Konfigurationsdateien wirken
          erst nach einem Neustart des Servers.</span>
      </div>
    </form>` });
}
