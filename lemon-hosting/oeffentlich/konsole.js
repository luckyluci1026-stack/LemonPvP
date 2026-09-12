/**
 * Die Live-Konsole im Panel.
 *
 * Haengt sich per EventSource an /panel/<id>/konsole. Jede Zeile, die der
 * Minecraft-Server ausgibt, kommt sofort an - man kann beim Start also
 * wirklich zusehen. Die Alternative waere, die Seite alle zwei Sekunden
 * neu zu laden; das ist fuer den Rechner und die Augen gleich schlecht.
 *
 * Der Server schickt drei Arten von Nachrichten:
 *
 *   verlauf  alle gepufferten Zeilen auf einmal (beim Verbinden)
 *   zeile    eine neue Zeile
 *   status   laeuft / startet / stoppt / gestoppt, dazu Spieler und
 *            Verbrauch - alle paar Sekunden
 *
 * Weil "verlauf" bei jedem Verbinden kommt, sortiert sich ein
 * Verbindungsabbruch von selbst: EventSource baut neu auf, wir werfen den
 * alten Inhalt weg und bekommen den vollstaendigen Puffer zurueck. Ohne
 * das haette man nach jedem Aussetzer doppelte Zeilen.
 */

const kasten = document.getElementById('konsole');
const formular = document.getElementById('befehlform');
const feld = document.getElementById('befehl');
const marke = document.getElementById('statusmarke');
const id = window.SERVER_ID;

/** Wie viele Zeilen im Browser stehenbleiben, bevor oben welche wegfallen. */
const MAX_ZEILEN = 600;

// ------------------------------------------------------------------ Ausgabe

/**
 * Klebt die Ansicht unten fest?
 *
 * Nur dann wird nachgescrollt. Wer hochgescrollt hat, um etwas zu lesen,
 * soll nicht bei jeder neuen Zeile wieder nach unten gerissen werden.
 */
function unten() {
  return kasten.scrollHeight - kasten.scrollTop - kasten.clientHeight < 40;
}

/** Minecraft-Zeilen einfaerben, damit Fehler ins Auge springen. */
function stufe(text) {
  if (/\/(ERROR|FATAL)\]|Exception|\bERROR\b/.test(text)) return ' schlimm';
  if (/\/WARN\]|\bWARN\b/.test(text)) return ' warnung';
  return '';
}

function haengeAn(z) {
  const klebt = unten();
  const el = document.createElement('div');
  el.className = 'z ' + z.art + (z.art === 'aus' ? stufe(z.text) : '');
  el.textContent = z.text;
  kasten.appendChild(el);
  while (kasten.childElementCount > MAX_ZEILEN) kasten.removeChild(kasten.firstChild);
  if (klebt) kasten.scrollTop = kasten.scrollHeight;
}

function zeigeVerlauf(zeilen) {
  kasten.textContent = '';
  if (!zeilen.length) {
    const el = document.createElement('div');
    el.className = 'z panel';
    el.textContent = 'Der Server läuft gerade nicht. Drück oben auf „Starten“.';
    kasten.appendChild(el);
    return;
  }
  for (const z of zeilen) haengeAn(z);
  kasten.scrollTop = kasten.scrollHeight;
}

// ------------------------------------------------------------------ Status

const TEXTE = { laeuft: 'läuft', startet: 'startet …', stoppt: 'stoppt …',
                gestoppt: 'gestoppt' };
const FARBEN = { laeuft: 'aktiv', startet: 'archiviert', stoppt: 'archiviert',
                 gestoppt: 'geloescht' };

function laufzeit(sekunden) {
  if (!sekunden) return '–';
  const h = Math.floor(sekunden / 3600), m = Math.floor((sekunden % 3600) / 60);
  return h ? `${h} h ${m} min` : `${m} min ${sekunden % 60} s`;
}

/** 1536 -> "1,5 GB", 820 -> "820 MB" */
function ramText(mb) {
  return mb >= 1024 ? (mb / 1024).toFixed(2).replace('.', ',') + ' GB' : mb + ' MB';
}

function bytes(n) {
  if (n >= 1024 ** 3) return (n / 1024 ** 3).toFixed(1).replace('.', ',') + ' GB';
  if (n >= 1024 ** 2) return (n / 1024 ** 2).toFixed(1).replace('.', ',') + ' MB';
  if (n >= 1024) return Math.round(n / 1024) + ' KB';
  return n + ' B';
}

/**
 * CPU und Arbeitsspeicher, so wie Pterodactyl sie oben zeigt.
 *
 * Ohne Messwerte bleiben Striche stehen statt Nullen - "0 %" waere eine
 * Behauptung, "–" ist die Wahrheit.
 */
function zeigeVerbrauch(v, kerne, ramGrenzeMB) {
  const setz = (id, text) => {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
  };
  const balken = (id, anteil) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.style.width = Math.max(0, Math.min(100, anteil)).toFixed(1) + '%';
    el.parentElement.className = 'balken'
      + (anteil > 90 ? ' aus' : anteil > 75 ? ' knapp' : '');
  };

  if (!v) {
    setz('cpu', '–'); setz('ram', '–'); setz('netz', '–');
    balken('cpuBalken', 0); balken('ramBalken', 0);
    return;
  }

  // docker stats zaehlt 100 % je Kern - bei 1,25 Kernen sind also 125 %
  // das Maximum. Auf die gebuchten Kerne umgerechnet ist die Zahl das,
  // was man erwartet.
  const cpuAnteil = kerne > 0 ? (v.cpu / (kerne * 100)) * 100 : v.cpu;
  setz('cpu', v.cpu.toFixed(1).replace('.', ',') + ' %');
  balken('cpuBalken', cpuAnteil);

  setz('ram', ramText(v.ramMB));
  balken('ramBalken', ramGrenzeMB > 0 ? (v.ramMB / ramGrenzeMB) * 100 : 0);

  if (v.netEin || v.netAus) {
    setz('netz', `↓ ${bytes(v.netEin)}  ↑ ${bytes(v.netAus)}`);
  } else {
    setz('netz', v.quelle === 'proc' ? 'nicht gemessen' : '↓ 0 B  ↑ 0 B');
    const hinweis = document.getElementById('netzHinweis');
    if (hinweis && v.quelle === 'proc') {
      hinweis.textContent = 'nur im Container messbar';
    }
  }
}

function zeigeStatus(z) {
  if (marke) {
    marke.textContent = TEXTE[z.status] || z.status;
    marke.className = 'marke-punkt ' + (FARBEN[z.status] || 'geloescht');
  }
  const anzeige = (welche, wert) => {
    const el = document.getElementById(welche);
    if (el) el.textContent = wert;
  };
  anzeige('laufzeit', laufzeit(z.laufzeit));
  anzeige('pid', z.pid ? 'Prozess ' + z.pid : 'nicht gestartet');
  anzeige('spielerzahl', (z.spieler || []).length);
  zeigeVerbrauch(z.verbrauch, window.SERVER_CORES, window.SERVER_RAM_MB);

  // Namen als einzelne Elemente, nicht als HTML-Text: Ein Spielername
  // kommt vom Minecraft-Server, und der bekommt ihn vom Spieler.
  const liste = document.getElementById('spieler');
  if (liste) {
    liste.textContent = '';
    if (!(z.spieler || []).length) {
      const leer = document.createElement('span');
      leer.className = 'leise';
      leer.textContent = z.status === 'gestoppt'
        ? 'Der Server läuft nicht.' : 'Gerade ist niemand drauf.';
      liste.appendChild(leer);
    } else {
      for (const name of z.spieler) {
        const el = document.createElement('span');
        el.className = 'spieler';
        el.textContent = name;
        liste.appendChild(el);
      }
    }
  }

  // Knöpfe mitziehen, damit man nicht neu laden muss, nur um wieder
  // starten zu dürfen.
  const aus = z.status === 'gestoppt';
  for (const knopf of document.querySelectorAll('[data-was=start]')) knopf.disabled = !aus;
  for (const knopf of document.querySelectorAll('[data-was=stopp],[data-was=neustart]')) {
    knopf.disabled = aus;
  }
  if (feld) {
    feld.disabled = z.status !== 'laeuft' && z.status !== 'startet';
    const senden = formular && formular.querySelector('button');
    if (senden) senden.disabled = feld.disabled;
  }
}

// ------------------------------------------------------------------ Leitung

const quelle = new EventSource(`/panel/${id}/konsole`);
quelle.addEventListener('verlauf', (e) => zeigeVerlauf(JSON.parse(e.data)));
quelle.addEventListener('zeile', (e) => haengeAn(JSON.parse(e.data)));
quelle.addEventListener('status', (e) => zeigeStatus(JSON.parse(e.data)));

// ------------------------------------------------------------------ Befehle

/**
 * Befehlsverlauf auf der Pfeiltaste.
 *
 * Wer "say Hallo" tippt und dann merkt, dass ein Wort fehlt, will nicht
 * alles noch einmal schreiben. Genau wie in einem Terminal.
 */
const verlauf = [];
let stelle = 0;

if (formular) {
  formular.addEventListener('submit', async (e) => {
    e.preventDefault();
    const text = feld.value.trim();
    if (!text) return;
    feld.value = '';
    verlauf.push(text);
    stelle = verlauf.length;

    const daten = new URLSearchParams({ befehl: text, csrf: formular.csrf.value });
    try {
      const antwort = await fetch(`/panel/${id}/befehl`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: daten,
      });
      const ergebnis = await antwort.json();
      if (ergebnis.fehler) haengeAn({ art: 'fehler', text: '[Panel] ' + ergebnis.fehler });
    } catch {
      haengeAn({ art: 'fehler', text: '[Panel] Der Befehl kam nicht durch.' });
    }
  });

  feld.addEventListener('keydown', (e) => {
    if (e.key !== 'ArrowUp' && e.key !== 'ArrowDown') return;
    if (!verlauf.length) return;
    e.preventDefault();
    stelle += e.key === 'ArrowUp' ? -1 : 1;
    stelle = Math.max(0, Math.min(verlauf.length, stelle));
    feld.value = stelle === verlauf.length ? '' : verlauf[stelle];
    feld.setSelectionRange(feld.value.length, feld.value.length);
  });
}
