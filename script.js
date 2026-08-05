/* =====================================================================
   Ida Hack — script.js
   ---------------------------------------------------------------------
   AUFBAU DER DATEI
   1) CONFIG      – globale Einstellungen (Tempo, Verzögerungen …)
   2) FACTIONS    – ALLE Texte & Rätsel (5 x White Hat, 5 x Black Hat)
   3) STATE / DOM – Spielzustand und Element-Referenzen
   4) HELPER      – Sound, Typewriter, Effekte
   5) ENGINE      – Boot, Levelablauf, Auswertung, Ende

   👉 Zum Bearbeiten der Rätsel musst du NUR Abschnitt 2 anfassen.
      Struktur eines Levels:
      {
        title:   'Überschrift der Mission',
        target:  'Fiktives Ziel (wird als code angezeigt)',
        brief:   'Story-Text in der Missions-Karte',
        task:    'Die konkrete Frage',
        hint:    'Tipp, den der Spieler anfordern kann',
        answers: ['loesung', 'alternative'],   // Groß/Klein & Umlaute egal
        console: [ {t:'Text', c:'sys'} ... ],  // wird ins Terminal getippt
        success: [ {t:'Text', c:'ok'} ... ]    // Ausgabe bei richtiger Lösung
      }
      Mögliche Zeilen-Klassen (c):
      sys | cmd | dim | ok | err | warn | data | title | user
   ===================================================================== */

(() => {
'use strict';

/* =====================================================================
   1) CONFIG
   ===================================================================== */
const CONFIG = {
  totalLevels:     5,      // Anzahl Level pro Pfad
  typeSpeed:       12,     // ms pro Zeichen im Terminal
  typeSpeedFast:   6,      // ms pro Zeichen bei Erfolgs-/Fehlermeldungen
  lineDelay:       110,    // Pause zwischen zwei Zeilen
  bootLineDelay:   170,    // Pause zwischen zwei Boot-Zeilen
  nextLevelDelay:  1500,   // Wartezeit bis zum nächsten Level (Vorgabe: 1,5 s)
  errorFlashTime:  700,    // Dauer des roten Blinkens
  tracePerFail:    12,     // Spur-Anstieg pro Fehlversuch (%)
};

/* Spott-Sprüche bei falscher Antwort (rotieren) */
const DENIED_LINES = [
  'Prüfsumme stimmt nicht. Der Server lacht dich aus. 💅',
  'Nope. Das war so falsch, dass der Cursor rot geworden ist.',
  'Falscher Schlüssel — das Schloss bleibt zu.',
  'Nicht mal ansatzweise. Aber süß, dass du es versuchst. 💗',
  'Abgelehnt. Versuch es nochmal, Prinzessin.',
  'Zugriff verweigert. Der Log-Server hat mitgeschrieben.',
];

/* =====================================================================
   2) FACTIONS — Story & Rätsel  (HIER BEARBEITEN)
   ===================================================================== */
const FACTIONS = {

  /* ------------------------------------------------------------------
     PFAD A — WHITE HAT · Für die Polizei
     ------------------------------------------------------------------ */
  white: {
    id:      'white',
    name:    'White Hat',
    icon:    '🤍',
    role:    'Cyber-Abwehr · Einheit K-7',
    theme:   'theme-white',
    badge:   '🤍 WHITE HAT · K-7',
    prompt:  'k7@hackos:~$',
    termTitle: 'k7-abwehr — /einsatz/rosenkranz',

    /* Intro, das beim Start des Spiels ins Terminal getippt wird */
    intro: [
      {t:'[ SICHERER KANAL AUFGEBAUT ]', c:'title'},
      {t:'Willkommen zurück, Ida. Dienstausweis erkannt: K7-0413.', c:'sys'},
      {t:'Die Zelle „Rosenkranz“ hat heute Nacht drei Server bewegt.', c:'sys'},
      {t:'Deine Aufgabe: fünf Etappen bis zum Beweispaket. Sauber bleiben.', c:'cmd'},
      {t:'─────────────────────────────────────────────', c:'dim'},
    ],

    levels: [
      /* ---------- WHITE 1 ---------- */
      {
        title:  'Passwort des verdächtigen Servers',
        target: 'srv-13.rosenkranz.onion',
        brief:  'Wir haben den Laptop eines Kuriers beschlagnahmt. Auf dem Image liegt eine Notizdatei — der Verdächtige hat sein Serverpasswort „verschlüsselt“ notiert. Er hielt ROT13 für sicher.',
        task:   'Entschlüssele das Passwort und gib es ein.',
        hint:   'ROT13 verschiebt jeden Buchstaben um 13 Stellen: A↔N, B↔O, M↔Z. Das Ergebnis ist eine gelbe Frucht.',
        answers:['zitrone','lemon'],
        console:[
          {t:'$ forensics --mount beweis/laptop_kurier.img', c:'cmd'},
          {t:'3 Partitionen eingehängt … 1 Notizdatei wiederhergestellt.', c:'dim'},
          {t:'$ cat /home/kurier/notizen.txt', c:'cmd'},
          {t:'PW für srv-13 (sicherheitshalber in ROT13):', c:'sys'},
          {t:'MVGEBAR', c:'data'},
          {t:'! ROT13 = jeder Buchstabe wurde um 13 Stellen verschoben.', c:'warn'},
        ],
        success:[
          {t:'✔ Login akzeptiert. Shell auf srv-13 offen.', c:'ok'},
          {t:'Chat-Verläufe werden gesichert … 214 Dateien kopiert.', c:'sys'},
        ],
      },

      /* ---------- WHITE 2 ---------- */
      {
        title:  'Der versteckte Backdoor-Port',
        target: 'srv-13.rosenkranz.onion:?',
        brief:  'Auf dem Server läuft ein getarnter Fernwartungsdienst. Der Portscan zeigt ein auffälliges Muster: die geöffneten Ports folgen einer mathematischen Regel. Der Backdoor sitzt auf dem nächsten Wert der Reihe.',
        task:   'Welche Portnummer kommt als nächstes?',
        hint:   '2, 3, 5, 7, 11, 13 — Zahlen, die nur durch 1 und sich selbst teilbar sind. Die nächste Primzahl nach 13.',
        answers:['17','port 17'],
        console:[
          {t:'$ nmap -p- srv-13.rosenkranz.onion', c:'cmd'},
          {t:'offene Ports gefunden:', c:'sys'},
          {t:'2 · 3 · 5 · 7 · 11 · 13 · ???', c:'data'},
          {t:'Der Wartungsdienst hört auf dem nächsten Port der Reihe.', c:'sys'},
          {t:'! Muster erkennen, Port eintragen.', c:'warn'},
        ],
        success:[
          {t:'✔ Port 17 offen — versteckter SSH-Daemon identifiziert.', c:'ok'},
          {t:'Backdoor dokumentiert und für die Akte protokolliert.', c:'sys'},
        ],
      },

      /* ---------- WHITE 3 ---------- */
      {
        title:  'Die Signatur im Datenstrom',
        target: 'trafficdump_0413.pcap',
        brief:  'Im mitgeschnittenen Datenverkehr taucht alle 60 Sekunden dasselbe Paket auf — das Erkennungswort der Malware. Es steht dort als reiner Binärcode. Entschlüssele es, damit wir eine Erkennungsregel schreiben können.',
        task:   'Welches Wort steckt im Binärcode?',
        hint:   'Jede 8er-Gruppe ist ein Buchstabe im ASCII-Code. 01001000 = 72 = „H“. Vier Gruppen, vier Buchstaben.',
        answers:['hack'],
        console:[
          {t:'$ tcpdump -r trafficdump_0413.pcap --payload', c:'cmd'},
          {t:'Wiederkehrende Signatur im Payload:', c:'sys'},
          {t:'01001000 01000001 01000011 01001011', c:'data'},
          {t:'! ASCII-Tabelle: A=65 · H=72 · K=75', c:'warn'},
        ],
        success:[
          {t:'✔ Signatur dekodiert. Firewall-Regel wird ausgerollt …', c:'ok'},
          {t:'Alle Streifenwagen-Laptops sind jetzt immun. Nice. 💗', c:'sys'},
        ],
      },

      /* ---------- WHITE 4 ---------- */
      {
        title:  'Das Verhörprotokoll',
        target: 'akte/verhoer_2026-08-05.txt',
        brief:  'Drei Verdächtige hatten Zugriff auf den Admin-Rechner. Genau einer der drei sagt die Wahrheit — die anderen beiden lügen. Wer den Rechner benutzt hat, ist damit eindeutig bestimmt.',
        task:   'Wer war es? (Name eingeben)',
        hint:   'Ben und Clara widersprechen sich direkt — einer von beiden sagt also immer die Wahrheit. Damit muss Annas Aussage die Lüge sein.',
        answers:['anna'],
        console:[
          {t:'$ open akte/verhoer_2026-08-05.txt', c:'cmd'},
          {t:'ANNA : „Ich war es nicht.“', c:'data'},
          {t:'BEN  : „Clara war es.“', c:'data'},
          {t:'CLARA: „Ben lügt.“', c:'data'},
          {t:'! Genau EINE der drei Aussagen ist wahr.', c:'warn'},
        ],
        success:[
          {t:'✔ Logik bestätigt: Annas Aussage ist die einzige Lüge.', c:'ok'},
          {t:'Haftbefehl vorbereitet. Sie hatte den Admin-Zugang.', c:'sys'},
        ],
      },

      /* ---------- WHITE 5 ---------- */
      {
        title:  'Der letzte Schlüssel',
        target: 'beweispaket_rosenkranz.enc',
        brief:  'Das Beweispaket ist mit einer Passphrase gesichert. Die Anführerin des Syndikats hat sie als Rätsel im Quellcode versteckt — sie konnte es nie lassen, anzugeben.',
        task:   'Löse das Rätsel: Wie lautet die Passphrase?',
        hint:   'Es hat Tasten, aber keine Türen. Du benutzt es genau in diesem Moment.',
        answers:['tastatur','keyboard'],
        console:[
          {t:'$ decrypt beweispaket_rosenkranz.enc', c:'cmd'},
          {t:'Passphrase erforderlich. Hinweis im Header gefunden:', c:'sys'},
          {t:'„Ich habe Tasten, doch öffne keine Tür.', c:'data'},
          {t:'  Ich habe Raum, doch biete kein Zimmer.', c:'data'},
          {t:'  Du drückst mich täglich — und ich verrate dich.“', c:'data'},
          {t:'! Ein Wort. Du berührst es gerade.', c:'warn'},
        ],
        success:[
          {t:'✔ Passphrase korrekt. Beweispaket entschlüsselt.', c:'ok'},
          {t:'12.408 Dateien · 3 Serverstandorte · 1 Netzwerkplan.', c:'sys'},
        ],
      },
    ],

    ending: {
      icon:  '🛡️',
      title: 'MISSION ERFOLGREICH',
      sub:   'Das Netzwerk „Rosenkranz“ ist offline. Alle drei Standorte wurden zeitgleich gesichert — ohne einen einzigen Datenverlust.',
      log: [
        {t:'[ 04:13 ] Zugriff auf srv-13 protokolliert', c:'sys'},
        {t:'[ 04:26 ] Backdoor geschlossen, Signatur verteilt', c:'sys'},
        {t:'[ 05:02 ] Haftbefehl vollstreckt', c:'sys'},
        {t:'[ 05:44 ] Beweispaket an die Staatsanwaltschaft übergeben', c:'ok'},
        {t:'Einheit K-7 meldet: Fall geschlossen. 🤍', c:'title'},
      ],
    },
  },

  /* ------------------------------------------------------------------
     PFAD B — BLACK HAT · Gegen die Polizei
     ------------------------------------------------------------------ */
  black: {
    id:      'black',
    name:    'Black Hat',
    icon:    '🖤',
    role:    'Cyber-Syndikat · Zelle „Rosenkranz“',
    theme:   'theme-black',
    badge:   '🖤 BLACK HAT · ROSENKRANZ',
    prompt:  'gh0st@hackos:~#',
    termTitle: 'gh0st — /ops/polizei-archiv',

    intro: [
      {t:'[ 7 PROXYS AKTIV · IDENTITÄT VERSCHLEIERT ]', c:'title'},
      {t:'Guten Abend, Ida. Die Zelle wartet auf dein Signal.', c:'sys'},
      {t:'Ziel: das Zentralarchiv der Cyber-Einheit K-7.', c:'sys'},
      {t:'Fünf Schichten. Wenn du hängen bleibst, kennt uns niemand.', c:'cmd'},
      {t:'─────────────────────────────────────────────', c:'dim'},
    ],

    levels: [
      /* ---------- BLACK 1 ---------- */
      {
        title:  'Firewall des Polizei-Archivs umgehen',
        target: 'fw01.k7-archiv.gov',
        brief:  'Die Firewall rotiert ihre Wartungsports nach einem simplen Schema — typisch Behörde. Wir haben fünf Werte mitgeschnitten. Der offene Port ist der nächste in der Reihe.',
        task:   'Welche Portnummer ist als nächstes offen?',
        hint:   '2 → 4 → 8 → 16 → 32: jede Zahl ist das Doppelte der vorherigen.',
        answers:['64','port 64'],
        console:[
          {t:'# probe fw01.k7-archiv.gov --sequence', c:'cmd'},
          {t:'Wartungsports der letzten 5 Zyklen:', c:'sys'},
          {t:'2 · 4 · 8 · 16 · 32 · ???', c:'data'},
          {t:'! Nächster Zyklus startet in 40 Sekunden.', c:'warn'},
        ],
        success:[
          {t:'✔ Port 64 offen. Wir sind drin — ohne einen Alarm.', c:'ok'},
          {t:'Tunnel steht. Latenz 42 ms. Schön unauffällig.', c:'sys'},
        ],
      },

      /* ---------- BLACK 2 ---------- */
      {
        title:  'Das Codewort der Wachschicht',
        target: 'gateway.k7-archiv.gov',
        brief:  'Hinter der Firewall fragt ein Gateway nach dem Codewort der Nachtschicht. Wir haben es aus einem Funkspruch — verschlüsselt mit einer Caesar-Verschiebung um 3 Stellen nach vorne.',
        task:   'Wie lautet das entschlüsselte Codewort?',
        hint:   'Schiebe jeden Buchstaben um 3 Stellen ZURÜCK: S→P, R→O, O→L … Das Ergebnis ist genau die Behörde, die wir gerade ausrauben.',
        answers:['polizei','police'],
        console:[
          {t:'# intercept --funk kanal-7', c:'cmd'},
          {t:'Abgefangenes Codewort (Caesar +3):', c:'sys'},
          {t:'S R O L C H L', c:'data'},
          {t:'! Jeder Buchstabe wurde um 3 Stellen nach vorne verschoben.', c:'warn'},
          {t:'  Beispiel: A→D, B→E, X→A', c:'dim'},
        ],
        success:[
          {t:'✔ Codewort akzeptiert. Gateway hält uns für die Nachtschicht.', c:'ok'},
          {t:'Sie haben ihr eigenes Passwort im Klartext gefunkt. Süß.', c:'sys'},
        ],
      },

      /* ---------- BLACK 3 ---------- */
      {
        title:  'Der Name in der Datenbank',
        target: 'db-ermittlungen.k7-archiv.gov',
        brief:  'Wir sind in der Ermittlungsdatenbank. Der Deckname unserer Zelle steht als Tabellenname drin — die Polizei speichert ihn hexadezimal, damit ihn niemand beim Drüberscrollen sieht.',
        task:   'Welches Wort steht im Hex-Code?',
        hint:   'Jedes Hex-Paar ist ein ASCII-Zeichen: 0x53 = 83 = „S“, 0x59 = „Y“ … Acht Paare, acht Buchstaben.',
        answers:['syndikat','syndicate'],
        console:[
          {t:'# SELECT tabellenname FROM zielobjekte WHERE prio = 1;', c:'cmd'},
          {t:'1 Treffer:', c:'sys'},
          {t:'53 59 4E 44 49 4B 41 54', c:'data'},
          {t:'! Hexadezimal → ASCII. 0x41 = „A“.', c:'warn'},
        ],
        success:[
          {t:'✔ Treffer. Sie führen eine ganze Akte über uns.', c:'ok'},
          {t:'Tabelle markiert. Wird gleich … verschwinden.', c:'sys'},
        ],
      },

      /* ---------- BLACK 4 ---------- */
      {
        title:  'Der Code des Beweistresors',
        target: 'tresor-digital.k7-archiv.gov',
        brief:  'Die Beweismittel liegen in einem digitalen Tresor. Ein alter Beamter hat sich den Code „leicht merkbar“ gemacht und die Regeln dazu an seinen Monitor geklebt. Wir haben ein Foto.',
        task:   'Wie lautet der dreistellige Tresor-Code?',
        hint:   'Alle drei Ziffern sind gleich, zusammen ergeben sie 9. Also 3 × dieselbe Ziffer = 9.',
        answers:['333'],
        console:[
          {t:'# cat notiz_monitor.jpg.txt', c:'cmd'},
          {t:'Merkregeln für den Tresor-Code:', c:'sys'},
          {t:'① genau 3 Ziffern', c:'data'},
          {t:'② alle drei Ziffern sind identisch', c:'data'},
          {t:'③ die Quersumme ist 9', c:'data'},
          {t:'! Quersumme = alle Ziffern addiert.', c:'warn'},
        ],
        success:[
          {t:'✔ Tresor offen. 3 Jahre Ermittlungsakten in unseren Händen.', c:'ok'},
          {t:'Kopieren … 98 % … fertig. Originale werden geschreddert.', c:'sys'},
        ],
      },

      /* ---------- BLACK 5 ---------- */
      {
        title:  'Der Root-Schlüssel',
        target: 'root@k7-archiv.gov',
        brief:  'Letzte Schicht: das Root-Passwort des Archivs. Die Systemadministratorin hat es als Anagramm in ihre Signatur geschrieben — dieselben Buchstaben, nur durcheinander.',
        task:   'Bringe die Buchstaben in die richtige Reihenfolge.',
        hint:   'Acht Buchstaben. Du starrst gerade auf genau dieses Ding: es zeigt Text und blinkt.',
        answers:['terminal'],
        console:[
          {t:'# grep -i "signatur" /var/mail/admin', c:'cmd'},
          {t:'Signatur der Administratorin:', c:'sys'},
          {t:'„mein Passwort? steht doch da: MENTIRAL“', c:'data'},
          {t:'! Anagramm — dieselben Buchstaben, andere Reihenfolge.', c:'warn'},
        ],
        success:[
          {t:'✔ ROOT-ZUGRIFF ERTEILT. Wir besitzen dieses Archiv.', c:'ok'},
          {t:'Logs werden überschrieben … Spur gelöscht … 💗', c:'sys'},
        ],
      },
    ],

    ending: {
      icon:  '👑',
      title: 'SYSTEM ÜBERNOMMEN',
      sub:   'Das Zentralarchiv der Einheit K-7 gehört jetzt dir. Sie werden Monate brauchen, um zu merken, dass überhaupt jemand da war.',
      log: [
        {t:'[ 02:11 ] Firewall passiert — kein Alarm', c:'sys'},
        {t:'[ 02:38 ] Ermittlungsdatenbank kopiert', c:'sys'},
        {t:'[ 03:04 ] Beweistresor geleert', c:'sys'},
        {t:'[ 03:19 ] Root-Zugriff · Logs überschrieben', c:'ok'},
        {t:'Zelle „Rosenkranz“ meldet: niemand hat uns je gesehen. 🖤', c:'title'},
      ],
    },
  },
};

/* Boot-Zeilen des Startbildschirms (fraktionsunabhängig) */
const BOOT_LINES = [
  {t:'Ida HackOS v5.0 — bootloader', c:'title'},
  {t:'© 2026 Ida Systems · alle Herzen vorbehalten', c:'dim'},
  {t:'[ OK ] Kernel geladen', c:'ok'},
  {t:'[ OK ] Glitzer-Treiber initialisiert', c:'ok'},
  {t:'[ OK ] Herz-Firewall aktiv', c:'ok'},
  {t:'[ OK ] 7 Proxy-Knoten verbunden', c:'ok'},
  {t:'[ WARN ] Kaffee-Vorrat: 12 %', c:'warn'},
  {t:'Netzwerk-Sniffer kalibriert … 100 %', c:'sys'},
  {t:'Biometrie geprüft: BENUTZERIN = IDA ✔', c:'sys'},
  {t:'Zwei Aufträge liegen vor. Beide zahlen gut.', c:'cmd'},
  {t:'>> WÄHLE EINE SEITE <<', c:'title'},
];

/* =====================================================================
   3) STATE & DOM
   ===================================================================== */
const state = {
  faction:   null,   // 'white' | 'black'
  levelIdx:  0,      // 0-basiert
  wrong:     0,
  hints:     0,
  trace:     0,
  locked:    true,   // Eingabe gesperrt (während Animationen)
  busy:      false,
  skipTyping:false,
  startTime: 0,
  sound:     true,
  deniedIdx: 0,
  runId:     0,      // Generationszähler: bricht laufende Animationen nach Neustart ab
};

/* Läuft die Animation noch zur aktuellen Partie? */
const alive = (run) => run === state.runId;

const $ = (id) => document.getElementById(id);

const dom = {
  body:         document.body,
  screens: {
    boot: $('screenBoot'),
    game: $('screenGame'),
    end:  $('screenEnd'),
  },
  bootLog:      $('bootLog'),
  skipBootBtn:  $('skipBootBtn'),
  picker:       $('factionPicker'),

  factionBadge: $('factionBadge'),
  levelValue:   $('levelValue'),
  wrongValue:   $('wrongValue'),
  traceFill:    $('traceFill'),
  soundBtn:     $('soundBtn'),
  restartBtn:   $('restartBtn'),

  progressFill: $('progressFill'),
  progressNodes:$('progressNodes'),

  missionLevel: $('missionLevel'),
  missionStatus:$('missionStatus'),
  missionTitle: $('missionTitle'),
  missionTarget:$('missionTarget'),
  missionBrief: $('missionBrief'),
  missionTask:  $('missionTask'),
  hintBtn:      $('hintBtn'),
  hintBox:      $('hintBox'),
  hintText:     $('hintText'),

  terminal:     $('terminal'),
  terminalTitle:$('terminalTitle'),
  terminalBody: $('terminalBody'),
  hackForm:     $('hackForm'),
  answerInput:  $('answerInput'),
  hackBtn:      $('hackBtn'),
  prompt:       document.querySelector('.prompt'),

  endIcon:      $('endIcon'),
  endTitle:     $('endTitle'),
  endSub:       $('endSub'),
  endLog:       $('endLog'),
  endTime:      $('endTime'),
  endWrong:     $('endWrong'),
  endHints:     $('endHints'),
  endRank:      $('endRank'),
  againBtn:     $('againBtn'),
  switchBtn:    $('switchBtn'),

  sparkles:     $('sparkles'),
};

/* =====================================================================
   4) HELPER
   ===================================================================== */
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

/* ---- Antwort-Normalisierung: Groß/Klein, Umlaute, Leerzeichen egal ---- */
function normalize(str){
  return String(str)
    .toLowerCase()
    .replace(/ä/g,'ae').replace(/ö/g,'oe').replace(/ü/g,'ue').replace(/ß/g,'ss')
    .replace(/[^a-z0-9]/g,'');
}
function isCorrect(input, answers){
  const v = normalize(input);
  return v.length > 0 && answers.some(a => normalize(a) === v);
}

/* ---- Sound (WebAudio, keine externen Dateien) ---- */
const Sound = {
  ctx:null,
  ensure(){
    if(!state.sound) return null;
    if(!this.ctx){
      const AC = window.AudioContext || window.webkitAudioContext;
      if(!AC) return null;
      this.ctx = new AC();
    }
    if(this.ctx.state === 'suspended') this.ctx.resume();
    return this.ctx;
  },
  tone(freq, dur = .12, type = 'sine', vol = .06, delay = 0){
    const ctx = this.ensure();
    if(!ctx) return;
    const t0 = ctx.currentTime + delay;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, t0);
    gain.gain.setValueAtTime(0.0001, t0);
    gain.gain.exponentialRampToValueAtTime(vol, t0 + .015);
    gain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
    osc.connect(gain).connect(ctx.destination);
    osc.start(t0);
    osc.stop(t0 + dur + .05);
  },
  click(){ this.tone(880, .05, 'square', .03); },
  type(){  this.tone(1400 + Math.random()*260, .02, 'square', .012); },
  ok(){    [660, 880, 1320].forEach((f,i) => this.tone(f, .18, 'triangle', .07, i*.09)); },
  err(){   this.tone(150, .3, 'sawtooth', .07); this.tone(96, .38, 'square', .05, .06); },
  win(){   [523,659,784,1047,1319].forEach((f,i) => this.tone(f, .3, 'triangle', .07, i*.13)); },
};

/* ---- Typewriter ---- */
async function typeInto(el, text, speed){
  const scroller = el.closest('.terminal__body, .boot__log');
  for(let i = 0; i < text.length; i++){
    if(state.skipTyping){ el.textContent = text; break; }
    el.textContent += text[i];
    if(scroller) scroller.scrollTop = scroller.scrollHeight;
    if(i % 3 === 0) Sound.type();
    await sleep(speed);
  }
  if(scroller) scroller.scrollTop = scroller.scrollHeight;
}

/* Eine Zeile ins Ziel-Element schreiben (getippt) */
async function printLine(container, entry, speed = CONFIG.typeSpeed){
  const data = (typeof entry === 'string') ? {t:entry, c:'sys'} : entry;
  const div = document.createElement('div');
  div.className = 'line line--' + (data.c || 'sys');
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
  await typeInto(div, data.t, speed);
  return div;
}

/* Sofort schreiben, ohne Tipp-Animation */
function pushLine(container, entry){
  const data = (typeof entry === 'string') ? {t:entry, c:'sys'} : entry;
  const div = document.createElement('div');
  div.className = 'line line--' + (data.c || 'sys');
  div.textContent = data.t;
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
  return div;
}

/* Mehrere Zeilen nacheinander. Mit `run` bricht die Ausgabe ab,
   sobald eine neue Partie gestartet wurde. */
async function printLines(container, lines, speed = CONFIG.typeSpeed, gap = CONFIG.lineDelay, run = null){
  for(const line of lines){
    if(run !== null && !alive(run)) return false;
    await printLine(container, line, speed);
    await sleep(gap);
  }
  return run === null || alive(run);
}

/* ---- Optische Effekte ---- */
function flash(bad = false){
  const el = document.createElement('div');
  el.className = 'flash' + (bad ? ' flash--bad' : '');
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 800);
}

function confetti(count = 60){
  const chars = ['💖','✨','💜','🌸','💗','⭐','🩷','💫'];
  for(let i = 0; i < count; i++){
    const el = document.createElement('span');
    el.className = 'confetti';
    el.textContent = chars[Math.floor(Math.random()*chars.length)];
    el.style.left = Math.random()*100 + 'vw';
    el.style.fontSize = (12 + Math.random()*18) + 'px';
    el.style.animationDuration = (2.4 + Math.random()*2.4) + 's';
    el.style.animationDelay = (Math.random()*1.2) + 's';
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 6000);
  }
}

function initSparkles(n = 22){
  const chars = ['💖','✨','🌸','💜','·','💗','⋆'];
  for(let i = 0; i < n; i++){
    const el = document.createElement('span');
    el.className = 'sparkle';
    el.textContent = chars[Math.floor(Math.random()*chars.length)];
    el.style.left = Math.random()*100 + 'vw';
    el.style.fontSize = (9 + Math.random()*15) + 'px';
    el.style.animationDuration = (11 + Math.random()*16) + 's';
    el.style.animationDelay = (-Math.random()*24) + 's';
    dom.sparkles.appendChild(el);
  }
}

/* =====================================================================
   5) ENGINE
   ===================================================================== */

function showScreen(name){
  Object.values(dom.screens).forEach(s => s.classList.remove('is-active'));
  dom.screens[name].classList.add('is-active');
  window.scrollTo({top:0, behavior:'smooth'});
}

/* ---------- Boot ---------- */
async function runBoot(){
  dom.bootLog.innerHTML = '';
  dom.picker.hidden = true;
  state.skipTyping = false;

  for(const line of BOOT_LINES){
    await printLine(dom.bootLog, line, CONFIG.typeSpeed);
    await sleep(state.skipTyping ? 0 : CONFIG.bootLineDelay);
  }
  const caret = document.createElement('span');
  caret.className = 'caret';
  dom.bootLog.appendChild(caret);

  await sleep(state.skipTyping ? 100 : 400);
  revealPicker();
}

function revealPicker(){
  dom.skipBootBtn.hidden = true;
  dom.picker.hidden = false;
  dom.picker.scrollIntoView({behavior:'smooth', block:'nearest'});
}

/* ---------- Spielstart ---------- */
async function startGame(factionId){
  const f = FACTIONS[factionId];
  if(!f) return;

  state.faction   = factionId;
  state.levelIdx  = 0;
  state.wrong     = 0;
  state.hints     = 0;
  state.trace     = 0;
  state.startTime = Date.now();
  state.skipTyping= false;
  state.busy      = false;
  const run       = ++state.runId;   // alte Animationen ungültig machen

  dom.body.classList.remove('theme-white','theme-black');
  dom.body.classList.add(f.theme);

  dom.factionBadge.textContent  = f.badge;
  dom.terminalTitle.textContent = f.termTitle;
  dom.prompt.textContent        = f.prompt;
  dom.terminalBody.innerHTML    = '';
  dom.answerInput.value         = '';
  dom.terminal.classList.remove('is-error','is-success');

  buildProgressNodes(f);
  updateHud();
  showScreen('game');

  lockInput(true);
  if(!await printLines(dom.terminalBody, f.intro, CONFIG.typeSpeed, 140, run)) return;
  await sleep(250);
  if(!alive(run)) return;
  await loadLevel(0, run);
}

function buildProgressNodes(f){
  dom.progressNodes.innerHTML = '';
  f.levels.forEach((lvl, i) => {
    const node = document.createElement('div');
    node.className = 'node';
    node.innerHTML = '<b class="node__n"></b><span class="node__t"></span>';
    node.querySelector('.node__n').textContent = String(i+1);
    node.querySelector('.node__t').textContent = ' · ' + shortTitle(lvl.title);
    node.dataset.index = String(i);
    dom.progressNodes.appendChild(node);
  });
}
function shortTitle(t){
  return t.length > 22 ? t.slice(0,21).trim() + '…' : t;
}

/* ---------- Level laden ---------- */
async function loadLevel(idx, run = state.runId){
  if(!alive(run)) return;
  const f   = FACTIONS[state.faction];
  const lvl = f.levels[idx];
  state.levelIdx = idx;

  /* Missions-Karte füllen */
  dom.missionLevel.textContent  = 'Level ' + (idx+1) + ' / ' + CONFIG.totalLevels;
  dom.missionStatus.textContent = 'aktiv';
  dom.missionStatus.classList.remove('is-done');
  dom.missionTitle.textContent  = lvl.title;
  dom.missionTarget.textContent = lvl.target;
  dom.missionBrief.textContent  = lvl.brief;
  dom.missionTask.textContent   = lvl.task;

  dom.hintBox.hidden = true;
  dom.hintText.textContent = '';
  dom.hintBtn.disabled = false;
  dom.hintBtn.textContent = '💡 Hinweis anfordern';

  updateHud();

  /* Terminal-Ausgabe des Levels */
  lockInput(true);
  await printLine(dom.terminalBody, {
    t:'\n╭─ LEVEL ' + (idx+1) + ' ── ' + lvl.title.toUpperCase() + ' ─╮', c:'title'
  }, CONFIG.typeSpeedFast);
  await sleep(120);
  if(!alive(run)) return;
  if(!await printLines(dom.terminalBody, lvl.console, CONFIG.typeSpeed, CONFIG.lineDelay, run)) return;
  await printLine(dom.terminalBody, {t:'? ' + lvl.task, c:'cmd'}, CONFIG.typeSpeed);
  if(!alive(run)) return;

  lockInput(false);
  dom.answerInput.focus();
}

/* ---------- Eingabe ---------- */
function lockInput(locked){
  state.locked = locked;
  dom.answerInput.disabled = locked;
  dom.hackBtn.disabled = locked;
  if(!locked) dom.answerInput.focus();
}

async function submitAnswer(){
  if(state.locked || state.busy) return;
  const raw = dom.answerInput.value.trim();
  const f   = FACTIONS[state.faction];
  const lvl = f.levels[state.levelIdx];

  if(!raw){
    dom.answerInput.focus();
    return;
  }

  const run = state.runId;
  state.busy = true;
  lockInput(true);
  Sound.click();

  pushLine(dom.terminalBody, {t:f.prompt + ' ' + raw, c:'user'});
  dom.answerInput.value = '';

  if(isCorrect(raw, lvl.answers)) await handleCorrect(lvl, run);
  else                            await handleWrong(run);

  if(alive(run)) state.busy = false;
}

/* ---------- Falsche Antwort ---------- */
async function handleWrong(run = state.runId){
  state.wrong++;
  state.trace = Math.min(100, state.trace + CONFIG.tracePerFail);
  updateHud();

  Sound.err();
  flash(true);
  dom.terminal.classList.add('is-error');
  setTimeout(() => dom.terminal.classList.remove('is-error'), CONFIG.errorFlashTime);

  await printLine(dom.terminalBody, {t:'✖ ZUGRIFF VERWEIGERT', c:'err'}, CONFIG.typeSpeedFast);
  if(!alive(run)) return;
  await printLine(dom.terminalBody, {t:'  ' + DENIED_LINES[state.deniedIdx % DENIED_LINES.length], c:'dim'}, CONFIG.typeSpeedFast);
  state.deniedIdx++;
  if(!alive(run)) return;

  /* Spur-Warnungen — kosmetisch, man kann nicht verlieren */
  if(state.trace >= 100){
    await printLine(dom.terminalBody, {t:'⚠ RÜCKVERFOLGUNG BEI 100 % — Proxy-Kette wird neu gewürfelt …', c:'warn'}, CONFIG.typeSpeedFast);
    state.trace = 60;
    updateHud();
    await printLine(dom.terminalBody, {t:'  Neue Route steht. Spur teilweise verwischt. 💗', c:'sys'}, CONFIG.typeSpeedFast);
  } else if(state.trace >= 60){
    await printLine(dom.terminalBody, {t:'⚠ Rückverfolgung bei ' + state.trace + ' % — etwas schneller jetzt.', c:'warn'}, CONFIG.typeSpeedFast);
  }
  if(!alive(run)) return;

  lockInput(false);
  dom.answerInput.focus();
}

/* ---------- Richtige Antwort ---------- */
async function handleCorrect(lvl, run = state.runId){
  const f = FACTIONS[state.faction];

  Sound.ok();
  flash(false);
  dom.terminal.classList.add('is-success');

  await printLine(dom.terminalBody, {t:'✔ ZUGRIFF GEWÄHRT', c:'ok'}, CONFIG.typeSpeedFast);
  if(!alive(run)){ dom.terminal.classList.remove('is-success'); return; }
  if(!await printLines(dom.terminalBody, lvl.success, CONFIG.typeSpeedFast, 80, run)){
    dom.terminal.classList.remove('is-success');
    return;
  }

  /* Fortschritt */
  const done = state.levelIdx + 1;
  setProgress(done / CONFIG.totalLevels);
  markNode(state.levelIdx, 'is-done');
  dom.missionStatus.textContent = 'erledigt';
  dom.missionStatus.classList.add('is-done');

  await sleep(CONFIG.nextLevelDelay);   /* nächstes Level nach 1,5 Sekunden */
  dom.terminal.classList.remove('is-success');
  if(!alive(run)) return;

  if(done >= CONFIG.totalLevels){
    finishGame(f, run);
  } else {
    await printLine(dom.terminalBody, {t:'… lade nächste Etappe …', c:'dim'}, CONFIG.typeSpeedFast);
    await loadLevel(done, run);
  }
}

function setProgress(ratio){
  dom.progressFill.style.width = Math.round(ratio*100) + '%';
}
function markNode(idx, cls){
  const node = dom.progressNodes.querySelector('[data-index="' + idx + '"]');
  if(node){ node.classList.remove('is-active'); node.classList.add(cls); }
}

/* ---------- HUD ---------- */
function updateHud(){
  dom.levelValue.textContent = String(Math.min(state.levelIdx+1, CONFIG.totalLevels));
  dom.wrongValue.textContent = String(state.wrong);
  dom.traceFill.style.width  = state.trace + '%';
  setProgress(state.levelIdx / CONFIG.totalLevels);

  [...dom.progressNodes.children].forEach((node, i) => {
    node.classList.toggle('is-active', i === state.levelIdx && !node.classList.contains('is-done'));
  });
}

/* ---------- Ende ---------- */
async function finishGame(f, run = state.runId){
  const e = f.ending;
  const secs = Math.max(1, Math.round((Date.now() - state.startTime)/1000));
  const mm = Math.floor(secs/60), ss = String(secs%60).padStart(2,'0');

  dom.endIcon.textContent  = e.icon;
  dom.endTitle.textContent = e.title;
  dom.endSub.textContent   = e.sub;
  dom.endTime.textContent  = mm + ':' + ss;
  dom.endWrong.textContent = String(state.wrong);
  dom.endHints.textContent = String(state.hints);
  dom.endRank.textContent  = rank(state.wrong, state.hints);
  dom.endLog.innerHTML     = '';

  setProgress(1);
  showScreen('end');
  Sound.win();
  confetti(70);

  await printLines(dom.endLog, e.log, CONFIG.typeSpeedFast, 120, run);
}

function rank(wrong, hints){
  const penalty = wrong + hints*2;
  if(penalty === 0) return '👑 Ghost';
  if(penalty <= 2)  return '🏆 Elite';
  if(penalty <= 6)  return '💫 Profi';
  if(penalty <= 12) return '🌸 Talent';
  return '🐣 Rookie';
}

/* ---------- Zurücksetzen ---------- */
function resetToPicker(){
  state.runId++;                 // laufende Animationen der alten Partie abbrechen
  dom.body.classList.remove('theme-white','theme-black');
  state.faction = null;
  state.busy = false;
  state.locked = true;
  state.skipTyping = true;
  dom.terminal.classList.remove('is-error','is-success');
  dom.terminalBody.innerHTML = '';
  dom.progressNodes.innerHTML = '';
  setProgress(0);
  showScreen('boot');
  revealPicker();
}

/* =====================================================================
   6) EVENTS
   ===================================================================== */
dom.skipBootBtn.addEventListener('click', () => {
  state.skipTyping = true;
  Sound.click();
});

document.querySelectorAll('.fcard').forEach(card => {
  card.addEventListener('click', () => {
    Sound.ok();
    startGame(card.dataset.faction);
  });
});

dom.hackForm.addEventListener('submit', (e) => {
  e.preventDefault();
  submitAnswer();
});

dom.hintBtn.addEventListener('click', async () => {
  if(state.faction === null) return;
  const lvl = FACTIONS[state.faction].levels[state.levelIdx];
  if(!dom.hintBox.hidden) return;

  state.hints++;
  Sound.click();
  dom.hintText.textContent = lvl.hint;
  dom.hintBox.hidden = false;
  dom.hintBtn.disabled = true;
  dom.hintBtn.textContent = '💡 Hinweis genutzt';
  pushLine(dom.terminalBody, {t:'» Hinweis angefordert: ' + lvl.hint, c:'warn'});
});

dom.soundBtn.addEventListener('click', () => {
  state.sound = !state.sound;
  dom.soundBtn.textContent = state.sound ? '🔊' : '🔇';
  dom.soundBtn.classList.toggle('is-off', !state.sound);
  if(state.sound) Sound.click();
});

dom.restartBtn.addEventListener('click', () => {
  if(confirm('Mission abbrechen und zurück zur Fraktionswahl?')) resetToPicker();
});

dom.againBtn.addEventListener('click', () => {
  const f = state.faction;
  if(f) startGame(f); else resetToPicker();
});

dom.switchBtn.addEventListener('click', resetToPicker);

/* Enter im Eingabefeld → absenden (Form macht das bereits, hier nur Fokus-Komfort) */
document.addEventListener('keydown', (e) => {
  if(e.key === 'Enter' && dom.screens.game.classList.contains('is-active') && !state.locked){
    if(document.activeElement !== dom.answerInput) dom.answerInput.focus();
  }
});

/* =====================================================================
   7) START
   ===================================================================== */
initSparkles();
runBoot();

})();
