/* =====================================================================
   Ida Hack — script.js
   ---------------------------------------------------------------------
   AUFBAU DER DATEI
   1) CONFIG      – globale Einstellungen (Tempo, Verzögerungen …)
   2) FACTIONS    – ALLE Texte & Rätsel
   3) STATE / DOM – Spielzustand und Element-Referenzen
   4) HELPER      – Sound, Typewriter, Effekte
   5) ENGINE      – Boot, Levelablauf, Auswertung, Ende

   👉 Zum Bearbeiten der Rätsel musst du NUR Abschnitt 2 anfassen.

   RÄTSEL-POOL / ROTATION
   ----------------------
   Jede Fraktion hat `stages` — eine Liste von Schwierigkeitsstufen.
   Jede Stufe enthält MEHRERE Varianten. Beim Spielstart wird pro Stufe
   eine Variante zufällig gezogen, dadurch ist jede Partie anders.

     stages: [
       [ VarianteA, VarianteB, VarianteC ],   // Stufe 1 (leicht)
       [ VarianteA, VarianteB, VarianteC ],   // Stufe 2
       …
     ]

   Neue Variante = einfach ein weiteres Objekt in die Stufe legen.
   Neue Stufe    = ein weiteres Array anhängen (Levelanzahl passt sich
                   automatisch an, auch Fortschrittsbalken und Anzeige).

   Aufbau einer Variante:
     {
       title:   'Überschrift der Mission',
       target:  'Fiktives Ziel (wird als code angezeigt)',
       brief:   'Story-Text in der Missions-Karte',
       task:    'Die konkrete Frage',
       hint:    'Tipp, den der Spieler anfordern kann',
       answers: ['42', '42 minuten'],        // Groß/Klein & Umlaute egal
       console: [ {t:'Text', c:'sys'} … ],   // wird ins Terminal getippt
       success: [ {t:'Text', c:'ok'} … ]     // Ausgabe bei richtiger Lösung
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
  'Abgelehnt. Rechne nochmal nach, Prinzessin.',
  'Zugriff verweigert. Der Log-Server hat mitgeschrieben.',
];

/* =====================================================================
   2) FACTIONS — Story & Rätsel  (HIER BEARBEITEN)
   ===================================================================== */
const FACTIONS = {

  /* ==================================================================
     PFAD A — WHITE HAT · Für die Polizei
     ================================================================== */
  white: {
    id:      'white',
    name:    'White Hat',
    icon:    '🤍',
    role:    'Cyber-Abwehr · Einheit K-7',
    theme:   'theme-white',
    badge:   '🤍 WHITE HAT · K-7',
    prompt:  'k7@hackos:~$',
    termTitle: 'k7-abwehr — /einsatz/rosenkranz',

    intro: [
      {t:'[ SICHERER KANAL AUFGEBAUT ]', c:'title'},
      {t:'Willkommen zurück, Ida. Dienstausweis erkannt: K7-0413.', c:'sys'},
      {t:'Die Zelle „Rosenkranz“ hat heute Nacht drei Server bewegt.', c:'sys'},
      {t:'Fünf Etappen bis zum Beweispaket. Kopf einschalten.', c:'cmd'},
      {t:'─────────────────────────────────────────────', c:'dim'},
    ],

    stages: [

      /* ============ STUFE 1 — Aufwärmen ============ */
      [
        {
          title:  'Das Muster im Zugangslog',
          target: 'srv-13.rosenkranz.onion',
          brief:  'Der Verdächtige loggt sich nach einem festen Muster ein. Die Abstände zwischen seinen Zugriffen wachsen jedes Mal nach derselben Regel. Wenn wir den nächsten Wert kennen, sitzen wir schon da, bevor er kommt.',
          task:   'Welche Zahl kommt als nächstes?',
          hint:   'Jede Zahl ist das Doppelte der vorherigen: 3 → 6 → 12 → 24 → 48 → ?',
          answers:['96'],
          console:[
            {t:'$ auth-log --user verdaechtig --intervalle', c:'cmd'},
            {t:'Abstände zwischen den Logins (in Minuten):', c:'sys'},
            {t:'3 · 6 · 12 · 24 · 48 · ???', c:'data'},
            {t:'! Ein Muster, kein Zufall.', c:'warn'},
          ],
          success:[
            {t:'✔ Treffer. Beim nächsten Login warten wir auf ihn.', c:'ok'},
            {t:'Sitzung mitgeschnitten — 214 Chatverläufe gesichert.', c:'sys'},
          ],
        },
        {
          title:  'Der nächste Zeitstempel',
          target: 'kamera-04.hauptbahnhof',
          brief:  'Der Kurier taucht in den Kameraaufzeichnungen in exakt gleichen Abständen auf. Wir brauchen die Uhrzeit seines nächsten Auftritts, damit die Streife rechtzeitig am Gleis steht.',
          task:   'Wann taucht er das nächste Mal auf? (Format HH:MM)',
          hint:   'Von 14:03 auf 14:10 sind es 7 Minuten. Zähle einfach weiter: 14:24 + 7.',
          answers:['14:31','1431'],
          console:[
            {t:'$ cam-search kamera-04 --gesicht kurier', c:'cmd'},
            {t:'Erkannte Auftritte:', c:'sys'},
            {t:'14:03 · 14:10 · 14:17 · 14:24 · ??:??', c:'data'},
            {t:'! Immer derselbe Abstand.', c:'warn'},
          ],
          success:[
            {t:'✔ 14:31 bestätigt — Streife steht schon am Gleis.', c:'ok'},
            {t:'Übergabe beobachtet. Wir haben das Paket im Bild.', c:'sys'},
          ],
        },
        {
          title:  'Die verdeckte Aktennummer',
          target: 'archiv/akte_???.pdf',
          brief:  'Die Akte, die wir brauchen, ist nicht mit ihrem Namen gespeichert, sondern mit einer Nummer. Ein Kollege hat sie uns am Telefon als Rechenaufgabe durchgegeben — er traut der Leitung nicht.',
          task:   'Wie lautet die Aktennummer?',
          hint:   'Rückwärts rechnen: 45 − 12 = 33, und 33 geteilt durch 3.',
          answers:['11'],
          console:[
            {t:'$ notiz --abhoersicher', c:'cmd'},
            {t:'„Nimm die Aktennummer, nimm sie mal 3,', c:'data'},
            {t:'  rechne 12 dazu — dann steht da 45.“', c:'data'},
            {t:'! Gesucht ist die Zahl, mit der er angefangen hat.', c:'warn'},
          ],
          success:[
            {t:'✔ Akte 11 geöffnet. Genau die richtige.', c:'ok'},
            {t:'Drei Kontobewegungen führen direkt zur Zelle.', c:'sys'},
          ],
        },
      ],

      /* ============ STUFE 2 — Prozente & Ziffern ============ */
      [
        {
          title:  'Der Code der Asservatenkammer',
          target: 'schloss.asservate.k7',
          brief:  'Die beschlagnahmte Hardware liegt hinter einem Zahlenschloss. Der Kollege mit dem Code hat Urlaub — aber er hat die Regeln an sein Whiteboard geschrieben.',
          task:   'Wie lautet der dreistellige Code?',
          hint:   'Alle drei Ziffern sind gleich, zusammen ergeben sie 12. Also 3 × dieselbe Ziffer = 12.',
          answers:['444'],
          console:[
            {t:'$ foto whiteboard_kollege.jpg --text', c:'cmd'},
            {t:'① genau 3 Ziffern', c:'data'},
            {t:'② alle drei Ziffern sind identisch', c:'data'},
            {t:'③ die Quersumme ist 12', c:'data'},
            {t:'! Quersumme = alle Ziffern zusammengezählt.', c:'warn'},
          ],
          success:[
            {t:'✔ Schloss offen. Die Hardware liegt vor uns.', c:'ok'},
            {t:'Zwei Telefone, ein Laptop, ein sehr nervöser Verdächtiger.', c:'sys'},
          ],
        },
        {
          title:  'Die geretteten Dateien',
          target: 'image_laptop_kurier.dd',
          brief:  'Die Festplatte aus der Wohnung ist teilweise überschrieben. Der Bericht an die Staatsanwaltschaft braucht die Zahl der unbeschädigten Dateien — und zwar bis heute Abend.',
          task:   'Wie viele Dateien sind intakt?',
          hint:   'Beschädigt sind 25 %, also ist ein Viertel weg. Übrig bleiben drei Viertel von 480.',
          answers:['360'],
          console:[
            {t:'$ recover --scan image_laptop_kurier.dd', c:'cmd'},
            {t:'Gefundene Dateien gesamt: 480', c:'data'},
            {t:'Davon beschädigt: 25 %', c:'data'},
            {t:'! Wie viele sind übrig?', c:'warn'},
          ],
          success:[
            {t:'✔ 360 intakte Dateien im Bericht vermerkt.', c:'ok'},
            {t:'Darunter: die komplette Buchhaltung der Zelle.', c:'sys'},
          ],
        },
        {
          title:  'Das Kennzeichen des Fluchtwagens',
          target: 'fahndung/fluchtwagen',
          brief:  'Eine Zeugin hat sich die drei Ziffern des Kennzeichens nicht gemerkt — aber sie erinnert sich an Zusammenhänge zwischen ihnen. Für die Fahndung reicht uns das.',
          task:   'Wie lautet die dreistellige Zahl?',
          hint:   'Erste Ziffer 2, letzte doppelt so groß → 4. Die mittlere ist 2 + 4.',
          answers:['264'],
          console:[
            {t:'$ zeugenaussage --protokoll 88', c:'cmd'},
            {t:'„Die erste Ziffer war eine 2.', c:'data'},
            {t:'  Die letzte war doppelt so groß wie die erste.', c:'data'},
            {t:'  Die mittlere war die Summe der beiden anderen.“', c:'data'},
            {t:'! Drei Ziffern, eine eindeutige Lösung.', c:'warn'},
          ],
          success:[
            {t:'✔ Kennzeichen rekonstruiert. Fahndung läuft.', c:'ok'},
            {t:'Wagen 20 Minuten später an der Ausfahrt gestoppt.', c:'sys'},
          ],
        },
      ],

      /* ============ STUFE 3 — Textaufgaben & Logik ============ */
      [
        {
          title:  'Das Alter im Personalbogen',
          target: 'akte/verdaechtiger_kf',
          brief:  'Zwei Personen der Zelle sind gemeldet, aber nur mit Spitznamen. In einer abgehörten Nachricht reden sie über ihr Alter — daraus bekommen wir das Geburtsjahr und damit den echten Namen.',
          task:   'Wie alt ist der Verdächtige?',
          hint:   'Die Komplizin ist x Jahre alt, er 3x. Zusammen 4x = 48, also x = 12.',
          answers:['36'],
          console:[
            {t:'$ abhoer --transkript 0413', c:'cmd'},
            {t:'„Du bist dreimal so alt wie ich.“', c:'data'},
            {t:'„Und zusammen kommen wir auf 48 Jahre.“', c:'data'},
            {t:'! Gesucht: sein Alter.', c:'warn'},
          ],
          success:[
            {t:'✔ 36 Jahre — Geburtsjahr passt zu genau einem Namen.', c:'ok'},
            {t:'Identität bestätigt. Er ist kein Kurier, er ist der Kopf.', c:'sys'},
          ],
        },
        {
          title:  'Die Aufteilung der Beute',
          target: 'konto/verteilung.xlsx',
          brief:  'Wir haben die Überweisungen, aber ohne Namen. Wenn wir wissen, welcher Betrag an den Hacker ging, können wir das Konto eindeutig zuordnen.',
          task:   'Wie viel Euro bekam der Hacker?',
          hint:   'Späher = x, Fahrer = 2x, Hacker = 3x. Zusammen 6x = 1200, also x = 200.',
          answers:['600','600€','600 euro'],
          console:[
            {t:'$ open konto/verteilung.xlsx', c:'cmd'},
            {t:'Gesamtsumme: 1.200 €', c:'data'},
            {t:'Der Fahrer bekam doppelt so viel wie der Späher.', c:'data'},
            {t:'Der Hacker bekam so viel wie die beiden zusammen.', c:'data'},
            {t:'! Gesucht: der Anteil des Hackers.', c:'warn'},
          ],
          success:[
            {t:'✔ 600 € — genau eine Überweisung passt.', c:'ok'},
            {t:'Konto identifiziert, Inhaberin sitzt in Rotterdam.', c:'sys'},
          ],
        },
        {
          title:  'Das Verhörprotokoll',
          target: 'akte/verhoer_2026-08-05.txt',
          brief:  'Drei Verdächtige hatten Zugriff auf den Admin-Rechner. Genau einer der drei sagt die Wahrheit — die anderen beiden lügen. Damit ist eindeutig bestimmt, wer es war.',
          task:   'Wer war es? (Name eingeben)',
          hint:   'Ben und Clara widersprechen sich direkt — einer von beiden sagt also die Wahrheit. Damit muss Annas Aussage die Lüge sein.',
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
      ],

      /* ============ STUFE 4 — Mehrstufig ============ */
      [
        {
          title:  'Die Rechenzeit im Labor',
          target: 'labor/entschluesselung',
          brief:  'Das Labor knackt die Container-Datei mit vier Rechnern. Die Staatsanwältin will wissen, ob es schneller geht, wenn wir zwei weitere dazustellen — und zwar auf die Minute genau.',
          task:   'Wie lange brauchen 6 Rechner? (in Minuten)',
          hint:   '4 Rechner × 12 Minuten = 48 Rechner-Minuten Arbeit. Verteile die 48 auf 6 Rechner.',
          answers:['8','8 minuten','8min'],
          console:[
            {t:'$ cluster --status', c:'cmd'},
            {t:'4 Rechner brauchen für den Scan: 12 Minuten', c:'data'},
            {t:'Verfügbar wären: 6 Rechner', c:'data'},
            {t:'! Mehr Rechner = weniger Zeit. Wie viel weniger?', c:'warn'},
          ],
          success:[
            {t:'✔ 8 Minuten. Die Staatsanwältin wartet gern so lange.', c:'ok'},
            {t:'Container offen. Darin: der komplette Netzwerkplan.', c:'sys'},
          ],
        },
        {
          title:  'Die Verfolgung auf der A3',
          target: 'funk/streife-12',
          brief:  'Der Fluchtwagen ist auf der Autobahn, unsere Streife nimmt die Verfolgung auf. Die Leitstelle muss wissen, wann sie ihn hat — davon hängt ab, ob wir die Ausfahrt sperren.',
          task:   'Nach wie vielen Minuten hat die Streife ihn eingeholt?',
          hint:   'In 20 Minuten schafft der Flüchtige 30 km Vorsprung. Die Streife holt pro Stunde 30 km auf — also genau eine Stunde.',
          answers:['60','60 minuten','1 stunde','eine stunde'],
          console:[
            {t:'$ funk --kanal streife-12', c:'cmd'},
            {t:'Fluchtwagen: 90 km/h, seit 20 Minuten unterwegs', c:'data'},
            {t:'Streifenwagen: 120 km/h, startet jetzt', c:'data'},
            {t:'! Wann trifft die Streife auf den Fluchtwagen?', c:'warn'},
          ],
          success:[
            {t:'✔ Nach 60 Minuten — Ausfahrt wird rechtzeitig gesperrt.', c:'ok'},
            {t:'Fahrzeug gestoppt, Fahrer festgenommen, Laptop sichergestellt.', c:'sys'},
          ],
        },
        {
          title:  'Die Zahlenreihe des Kuriers',
          target: 'notizbuch_kurier.jpg',
          brief:  'Im Notizbuch des Kuriers steht eine Zahlenreihe, die er als Prüfsumme benutzt. Die letzte Zahl fehlt — sie ist der Zugangscode für das nächste Treffen.',
          task:   'Welche Zahl kommt als nächstes?',
          hint:   'Jede Zahl ist die Summe der beiden davor: 5 + 8 = 13, 8 + 13 = 21, also 13 + 21 = ?',
          answers:['34'],
          console:[
            {t:'$ ocr notizbuch_kurier.jpg', c:'cmd'},
            {t:'Erkannte Reihe:', c:'sys'},
            {t:'1 · 1 · 2 · 3 · 5 · 8 · 13 · 21 · ???', c:'data'},
            {t:'! Schau dir jeweils zwei benachbarte Zahlen an.', c:'warn'},
          ],
          success:[
            {t:'✔ 34 — der Code für das Treffen stimmt.', c:'ok'},
            {t:'Treffpunkt und Uhrzeit stehen. Wir sind vorher da.', c:'sys'},
          ],
        },
      ],

      /* ============ STUFE 5 — Finale ============ */
      [
        {
          title:  'Der Master-Code des Beweispakets',
          target: 'beweispaket_rosenkranz.enc',
          brief:  'Letzte Hürde: Das Beweispaket ist mit einem vierstelligen Code gesichert. Die Anführerin der Zelle hat die Regel dazu in ihre Signatur geschrieben — sie konnte es nie lassen, anzugeben.',
          task:   'Wie lautet der vierstellige Code?',
          hint:   'Start ist 3, dann immer +2: 3, 5, 7 und noch eine Ziffer.',
          answers:['3579'],
          console:[
            {t:'$ decrypt beweispaket_rosenkranz.enc', c:'cmd'},
            {t:'Hinweis im Dateikopf:', c:'sys'},
            {t:'„Vier Ziffern. Die erste ist die 3.', c:'data'},
            {t:'  Jede weitere ist um 2 größer als die davor.“', c:'data'},
            {t:'! Ein Versuch reicht, wenn du richtig rechnest.', c:'warn'},
          ],
          success:[
            {t:'✔ Code korrekt. Beweispaket entschlüsselt.', c:'ok'},
            {t:'12.408 Dateien · 3 Serverstandorte · 1 Netzwerkplan.', c:'sys'},
          ],
        },
        {
          title:  'Das Zahlenschloss im Serverraum',
          target: 'serverraum.k7-archiv',
          brief:  'Die Originalfestplatten liegen im Serverraum hinter einem zweistelligen Zahlenschloss. Die Regeln stehen auf einem Zettel, den die Zelle vergessen hat zu vernichten.',
          task:   'Wie lautet die zweistellige Zahl?',
          hint:   'Einerziffer = x, Zehnerziffer = 2x. Zusammen 3x = 12, also x = 4.',
          answers:['84'],
          console:[
            {t:'$ scan zettel_serverraum.jpg', c:'cmd'},
            {t:'„Zwei Ziffern.', c:'data'},
            {t:'  Zusammengezählt ergeben sie 12.', c:'data'},
            {t:'  Die vordere ist doppelt so groß wie die hintere.“', c:'data'},
            {t:'! Nur eine Zahl erfüllt beides.', c:'warn'},
          ],
          success:[
            {t:'✔ Schloss offen. Originalfestplatten gesichert.', c:'ok'},
            {t:'Beweiskette lückenlos — das hält vor jedem Gericht.', c:'sys'},
          ],
        },
        {
          title:  'Die letzte Rechnung',
          target: 'root@srv-13.rosenkranz',
          brief:  'Der Hauptserver fragt nach einer Zahl, bevor er die Protokolle herausgibt. Die Zelle hat sie als kleines Rechenspiel hinterlegt — vermutlich, damit die eigenen Leute sie nicht vergessen.',
          task:   'An welche Zahl war ursprünglich gedacht?',
          hint:   'Rückwärts: 15 × 2 = 30, dann 30 + 6 = 36, und 36 ÷ 4.',
          answers:['9'],
          console:[
            {t:'$ ssh root@srv-13.rosenkranz', c:'cmd'},
            {t:'Sicherheitsfrage:', c:'sys'},
            {t:'„Denk dir eine Zahl. Nimm sie mal 4.', c:'data'},
            {t:'  Zieh 6 ab. Teile durch 2. Heraus kommt 15.“', c:'data'},
            {t:'! Gesucht ist die Zahl vom Anfang.', c:'warn'},
          ],
          success:[
            {t:'✔ Antwort akzeptiert. Protokolle werden übertragen.', c:'ok'},
            {t:'Alles da: Namen, Konten, Treffpunkte. Feierabend.', c:'sys'},
          ],
        },
      ],

    ],

    ending: {
      icon:  '🛡️',
      title: 'MISSION ERFOLGREICH',
      sub:   'Das Netzwerk „Rosenkranz“ ist offline. Alle drei Standorte wurden zeitgleich gesichert — ohne einen einzigen Datenverlust.',
      log: [
        {t:'[ 04:13 ] Zugriff protokolliert', c:'sys'},
        {t:'[ 04:26 ] Asservate gesichert', c:'sys'},
        {t:'[ 05:02 ] Haftbefehl vollstreckt', c:'sys'},
        {t:'[ 05:44 ] Beweispaket an die Staatsanwaltschaft übergeben', c:'ok'},
        {t:'Einheit K-7 meldet: Fall geschlossen. 🤍', c:'title'},
      ],
    },
  },

  /* ==================================================================
     PFAD B — BLACK HAT · Gegen die Polizei
     ================================================================== */
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

    stages: [

      /* ============ STUFE 1 — Aufwärmen ============ */
      [
        {
          title:  'Der offene Wartungsport',
          target: 'fw01.k7-archiv.gov',
          brief:  'Die Firewall wechselt ihre Wartungsports nach einem simplen Schema — typisch Behörde. Wir haben fünf Werte mitgeschnitten. Der offene Port ist der nächste in der Reihe.',
          task:   'Welche Zahl kommt als nächstes?',
          hint:   'Jede Zahl ist das Doppelte der vorherigen: 2 → 4 → 8 → 16 → 32 → ?',
          answers:['64','port 64'],
          console:[
            {t:'# probe fw01.k7-archiv.gov --sequence', c:'cmd'},
            {t:'Wartungsports der letzten 5 Zyklen:', c:'sys'},
            {t:'2 · 4 · 8 · 16 · 32 · ???', c:'data'},
            {t:'! Nächster Zyklus startet in 40 Sekunden.', c:'warn'},
          ],
          success:[
            {t:'✔ Port offen. Wir sind drin — ohne einen Alarm.', c:'ok'},
            {t:'Tunnel steht. Latenz 42 ms. Schön unauffällig.', c:'sys'},
          ],
        },
        {
          title:  'Der nächste Wachwechsel',
          target: 'wachplan.k7-archiv',
          brief:  'Im Serverraum läuft alle paar Minuten jemand vorbei. Wir haben die letzten Runden mitgeschrieben. Wir brauchen den nächsten Zeitpunkt — dann haben wir freie Bahn.',
          task:   'Wann ist der nächste Wachwechsel? (Format HH:MM)',
          hint:   'Der Abstand beträgt 45 Minuten. 22:15 + 45 Minuten sind 23:00.',
          answers:['23:00','2300'],
          console:[
            {t:'# tail -f wachplan.k7-archiv', c:'cmd'},
            {t:'Protokollierte Rundgänge:', c:'sys'},
            {t:'20:45 · 21:30 · 22:15 · ??:??', c:'data'},
            {t:'! Immer derselbe Abstand.', c:'warn'},
          ],
          success:[
            {t:'✔ 23:00 bestätigt. Zwei Minuten davor sind wir raus.', c:'ok'},
            {t:'Kameraschleife läuft. Für sie sieht alles normal aus.', c:'sys'},
          ],
        },
        {
          title:  'Die Zahl aus dem Funkspruch',
          target: 'kanal-7.funk',
          brief:  'Der Nachtdienst gibt seine Codes nicht im Klartext durch — er verpackt sie als Rechenaufgabe. Für uns ist das eher eine Einladung als ein Hindernis.',
          task:   'Welche Zahl ist gemeint?',
          hint:   'Rückwärts rechnen: 31 + 5 = 36, und 36 geteilt durch 2.',
          answers:['18'],
          console:[
            {t:'# intercept --funk kanal-7', c:'cmd'},
            {t:'„Nimm die Zahl, verdopple sie,', c:'data'},
            {t:'  zieh 5 ab — dann hast du 31.“', c:'data'},
            {t:'! Gesucht ist die Zahl vom Anfang.', c:'warn'},
          ],
          success:[
            {t:'✔ Zahl akzeptiert. Sie funken ihre Codes im Klartext.', c:'ok'},
            {t:'Süß. Fast schon zu einfach.', c:'sys'},
          ],
        },
      ],

      /* ============ STUFE 2 — Prozente & Ziffern ============ */
      [
        {
          title:  'Der Code des Beweistresors',
          target: 'tresor-digital.k7-archiv.gov',
          brief:  'Die Beweismittel liegen in einem digitalen Tresor. Ein alter Beamter hat sich den Code „leicht merkbar“ gemacht und die Regeln an seinen Monitor geklebt. Wir haben ein Foto.',
          task:   'Wie lautet der dreistellige Code?',
          hint:   'Alle drei Ziffern sind gleich, zusammen ergeben sie 9. Also 3 × dieselbe Ziffer = 9.',
          answers:['333'],
          console:[
            {t:'# cat notiz_monitor.jpg.txt', c:'cmd'},
            {t:'① genau 3 Ziffern', c:'data'},
            {t:'② alle drei Ziffern sind identisch', c:'data'},
            {t:'③ die Quersumme ist 9', c:'data'},
            {t:'! Quersumme = alle Ziffern zusammengezählt.', c:'warn'},
          ],
          success:[
            {t:'✔ Tresor offen. Drei Jahre Ermittlungsakten.', c:'ok'},
            {t:'Kopieren … 98 % … fertig. Originale werden geschreddert.', c:'sys'},
          ],
        },
        {
          title:  'Die digitalisierten Akten',
          target: 'db-ermittlungen.k7-archiv.gov',
          brief:  'Nur ein Teil des Archivs liegt digital vor — der Rest steht als Papier im Keller. Wir müssen wissen, wie viele Akten wir überhaupt mitnehmen können, bevor der Tunnel zumacht.',
          task:   'Wie viele Akten sind digitalisiert?',
          hint:   '10 % von 640 sind 64. Also sind 30 % = 192 und 5 % = 32 — zusammen 35 %.',
          answers:['224'],
          console:[
            {t:'# SELECT count(*) FROM akten;', c:'cmd'},
            {t:'Akten gesamt: 640', c:'data'},
            {t:'Davon digitalisiert: 35 %', c:'data'},
            {t:'! Wie viele Akten sind das?', c:'warn'},
          ],
          success:[
            {t:'✔ 224 Akten übertragen. Der Rest verstaubt im Keller.', c:'ok'},
            {t:'Darunter drei, in denen unsere Zelle vorkommt.', c:'sys'},
          ],
        },
        {
          title:  'Die blinden Kameras',
          target: 'cctv.k7-archiv.gov',
          brief:  'Ein Teil der Überwachungskameras ist seit Monaten defekt — die Behörde hat kein Budget für Reparaturen. Der Prüfbericht will die Ausfallquote in Prozent. Wir auch, aber aus anderen Gründen.',
          task:   'Wie viel Prozent der Kameras sind blind?',
          hint:   '12 von 300. Rechne 12 geteilt durch 300, mal 100 — oder: 3 von 300 wären 1 %.',
          answers:['4','4%','4 prozent'],
          console:[
            {t:'# cctv --status --all', c:'cmd'},
            {t:'Kameras gesamt: 300', c:'data'},
            {t:'Ohne Signal: 12', c:'data'},
            {t:'! Ausfallquote in Prozent?', c:'warn'},
          ],
          success:[
            {t:'✔ 4 % blind — und wir wissen jetzt, welche.', c:'ok'},
            {t:'Route durch den Ostflügel geplant. Kein einziges Bild.', c:'sys'},
          ],
        },
      ],

      /* ============ STUFE 3 — Textaufgaben & Logik ============ */
      [
        {
          title:  'Die Scheine in der Asservatenkasse',
          target: 'asservate/bargeld',
          brief:  'In der Kasse liegt beschlagnahmtes Bargeld. Wir nehmen nur die großen Scheine mit — alles andere fällt zu sehr auf. Bevor wir zugreifen, wollen wir wissen, wie viele es sind.',
          task:   'Wie viele 100er-Scheine liegen in der Kasse?',
          hint:   '100er = x, 50er = 3x. Zusammen 4x = 24 Scheine, also x = 6.',
          answers:['6'],
          console:[
            {t:'# open asservate/bargeld --inventar', c:'cmd'},
            {t:'Scheine gesamt: 24', c:'data'},
            {t:'Es liegen dreimal so viele 50er wie 100er darin.', c:'data'},
            {t:'! Gesucht: die Anzahl der 100er.', c:'warn'},
          ],
          success:[
            {t:'✔ 6 Scheine. Genau so viele, wie in die Tasche passen.', c:'ok'},
            {t:'Inventarliste angepasst. Es fehlt offiziell nichts.', c:'sys'},
          ],
        },
        {
          title:  'Das Alter der Administratorin',
          target: 'personal/admin_k7',
          brief:  'Das Passwort der Administratorin enthält ihr Geburtsjahr — das ist bei ihr seit zehn Jahren so. In einem privaten Post schreibt sie über sich und ihre Tochter, ohne Zahlen zu nennen. Fast ohne.',
          task:   'Wie alt ist die Administratorin?',
          hint:   'Tochter = t, Mutter = 4t. In 5 Jahren: 4t + 5 = 3 × (t + 5). Daraus folgt t = 10.',
          answers:['40'],
          console:[
            {t:'# scrape social --user admin_k7', c:'cmd'},
            {t:'„Ich bin viermal so alt wie meine Tochter.“', c:'data'},
            {t:'„In fünf Jahren bin ich nur noch dreimal so alt.“', c:'data'},
            {t:'! Gesucht: ihr heutiges Alter.', c:'warn'},
          ],
          success:[
            {t:'✔ 40 — Geburtsjahr passt, Passwort sitzt.', c:'ok'},
            {t:'Sie sollte weniger über sich posten. Wirklich.', c:'sys'},
          ],
        },
        {
          title:  'Die Aussagen der Nachtschicht',
          target: 'protokoll/nachtschicht.log',
          brief:  'Genau einer der drei Wachleute war in der Nacht des Einbruchs im Serverraum — und nur einer der drei sagt in seinem Protokoll die Wahrheit. Wenn wir wissen, wer es war, wissen wir, wen wir erpressen können.',
          task:   'Wer war im Serverraum? (Name eingeben)',
          hint:   'Probier alle drei durch: Nimm an, Dario war es — wie viele Aussagen sind dann wahr? Dann Eva, dann Felix. Nur bei einem passt „genau eine wahre Aussage“.',
          answers:['felix'],
          console:[
            {t:'# cat protokoll/nachtschicht.log', c:'cmd'},
            {t:'DARIO: „Eva war im Serverraum.“', c:'data'},
            {t:'EVA  : „Ich war nicht im Serverraum.“', c:'data'},
            {t:'FELIX: „Ich war nicht im Serverraum.“', c:'data'},
            {t:'! Genau EINE der drei Aussagen ist wahr.', c:'warn'},
          ],
          success:[
            {t:'✔ Nur bei ihm passt die Rechnung auf.', c:'ok'},
            {t:'Er hat Schulden. Ab jetzt arbeitet er für uns.', c:'sys'},
          ],
        },
      ],

      /* ============ STUFE 4 — Mehrstufig ============ */
      [
        {
          title:  'Die Kopierzeit im Serverraum',
          target: 'kopierstation-02',
          brief:  'Wir spiegeln die beschlagnahmten Festplatten. Der Wachwechsel kommt, und wir müssen vorher fertig sein — also brauchen wir die Zeit auf die Minute genau.',
          task:   'Wie lange dauern 9 Festplatten? (in Minuten)',
          hint:   '42 Minuten geteilt durch 6 sind 7 Minuten pro Festplatte. Mal 9.',
          answers:['63','63 minuten','63min'],
          console:[
            {t:'# clone --status', c:'cmd'},
            {t:'6 Festplatten gespiegelt in: 42 Minuten', c:'data'},
            {t:'Noch anzuschließen: 9 Festplatten', c:'data'},
            {t:'! Gleiches Tempo. Wie lange dauert das?', c:'warn'},
          ],
          success:[
            {t:'✔ 63 Minuten — vier Minuten vor dem Wachwechsel fertig.', c:'ok'},
            {t:'Alles gespiegelt. Die Originale liegen unberührt da.', c:'sys'},
          ],
        },
        {
          title:  'Die Übergabe am Hafen',
          target: 'kurier/route-nord',
          brief:  'Unser Kurier ist mit den Daten losgefahren, aber er hat das falsche Paket dabei. Du musst ihn einholen, bevor er am Hafen ankommt — die Zelle will wissen, wie viel Zeit das kostet.',
          task:   'Nach wie vielen Minuten hast du ihn eingeholt?',
          hint:   'Du bist 30 km/h schneller. Die 15 km Vorsprung schrumpfen also mit 30 km/h — das ist eine halbe Stunde.',
          answers:['30','30 minuten','eine halbe stunde','halbe stunde'],
          console:[
            {t:'# track kurier/route-nord', c:'cmd'},
            {t:'Kurier: 15 km Vorsprung, fährt 60 km/h', c:'data'},
            {t:'Du: fährst 90 km/h, startest jetzt', c:'data'},
            {t:'! Wann bist du bei ihm?', c:'warn'},
          ],
          success:[
            {t:'✔ Nach 30 Minuten — kurz vor der Hafeneinfahrt.', c:'ok'},
            {t:'Paket getauscht. Er hat nichts gemerkt.', c:'sys'},
          ],
        },
        {
          title:  'Der Türcode im Ostflügel',
          target: 'tuer-e12.k7-archiv',
          brief:  'Die Tür zum Ostflügel wechselt ihren Code täglich nach einer Formel. Wir haben die Codes der letzten fünf Tage — der von heute fehlt uns noch.',
          task:   'Welche Zahl kommt als nächstes?',
          hint:   'Jede Zahl wird verdoppelt, dann kommt 1 dazu: 2 → 5 → 11 → 23 → 47 → ?',
          answers:['95'],
          console:[
            {t:'# log tuer-e12 --letzte 5', c:'cmd'},
            {t:'Codes der letzten Tage:', c:'sys'},
            {t:'2 · 5 · 11 · 23 · 47 · ???', c:'data'},
            {t:'! Verdoppeln reicht nicht ganz. Was fehlt?', c:'warn'},
          ],
          success:[
            {t:'✔ Tür offen. Kein Protokolleintrag, keine Kamera.', c:'ok'},
            {t:'Der Ostflügel gehört für die nächste Stunde uns.', c:'sys'},
          ],
        },
      ],

      /* ============ STUFE 5 — Finale ============ */
      [
        {
          title:  'Der Root-Code des Archivs',
          target: 'root@k7-archiv.gov',
          brief:  'Letzte Schicht: der Root-Zugang. Der Code ist vierstellig, und die Systemadministratorin hat sich die Regel dazu in ihre Signatur geschrieben — vermutlich als Gedächtnisstütze.',
          task:   'Wie lautet der vierstellige Code?',
          hint:   'Vier Zahlen hintereinander, absteigend: n, n−1, n−2, n−3. Zusammen 4n − 6 = 26, also n = 8.',
          answers:['8765'],
          console:[
            {t:'# grep -i "signatur" /var/mail/admin', c:'cmd'},
            {t:'„Vier Ziffern, absteigend, jede um 1 kleiner.', c:'data'},
            {t:'  Zusammengezählt ergeben sie 26.“', c:'data'},
            {t:'! Eine einzige Zahlenfolge passt.', c:'warn'},
          ],
          success:[
            {t:'✔ ROOT-ZUGRIFF ERTEILT. Wir besitzen dieses Archiv.', c:'ok'},
            {t:'Logs werden überschrieben … Spur gelöscht … 💗', c:'sys'},
          ],
        },
        {
          title:  'Das Zahlenrätsel der Admin',
          target: 'root@k7-archiv.gov',
          brief:  'Die Administratorin sichert den letzten Schlüssel mit einer zweistelligen Zahl. Sie hat sich zwei Eigenschaften notiert — zusammen gibt es genau eine Lösung.',
          task:   'Wie lautet die zweistellige Zahl?',
          hint:   'Probier die Zehnerziffern durch: bei 3 wäre die Einerziffer 6 — und 3 × 6 ist 18.',
          answers:['36'],
          console:[
            {t:'# cat /root/.merkzettel', c:'cmd'},
            {t:'„Zwei Ziffern.', c:'data'},
            {t:'  Die hintere ist um 3 größer als die vordere.', c:'data'},
            {t:'  Miteinander multipliziert ergeben sie 18.“', c:'data'},
            {t:'! Nur eine Zahl erfüllt beides.', c:'warn'},
          ],
          success:[
            {t:'✔ Schlüssel akzeptiert. Das Archiv gehört uns.', c:'ok'},
            {t:'Sie hätte einen Passwortmanager benutzen sollen.', c:'sys'},
          ],
        },
        {
          title:  'Die letzte Sicherheitsfrage',
          target: 'root@k7-archiv.gov',
          brief:  'Vor der Freigabe stellt das System eine Sicherheitsfrage. Sie ist als kleines Rechenspiel formuliert — offenbar hat sich hier jemand für besonders clever gehalten.',
          task:   'An welche Zahl war ursprünglich gedacht?',
          hint:   'Rückwärts: 21 + 4 = 25, dann 25 × 2 = 50, dann 50 − 8 = 42, und 42 ÷ 3.',
          answers:['14'],
          console:[
            {t:'# su - root', c:'cmd'},
            {t:'Sicherheitsfrage:', c:'sys'},
            {t:'„Denk dir eine Zahl. Nimm sie mal 3.', c:'data'},
            {t:'  Zähl 8 dazu. Halbiere. Zieh 4 ab. Heraus kommt 21.“', c:'data'},
            {t:'! Gesucht ist die Zahl vom Anfang.', c:'warn'},
          ],
          success:[
            {t:'✔ Antwort korrekt. Vollzugriff erteilt.', c:'ok'},
            {t:'Logs überschrieben. Wir waren nie hier. 🖤', c:'sys'},
          ],
        },
      ],

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
  {t:'Rätsel-Pool geladen … 30 Varianten bereit', c:'sys'},
  {t:'Biometrie geprüft: BENUTZERIN = IDA ✔', c:'sys'},
  {t:'Zwei Aufträge liegen vor. Beide zahlen gut.', c:'cmd'},
  {t:'>> WÄHLE EINE SEITE <<', c:'title'},
];

/* =====================================================================
   3) STATE & DOM
   ===================================================================== */
const state = {
  faction:   null,   // 'white' | 'black'
  levels:    [],     // die für DIESE Partie gezogenen Rätsel
  total:     0,      // Anzahl Level dieser Partie
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
  levelTotal:   $('levelTotal'),
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

/* ---- Zufällige Rätsel-Auswahl: pro Stufe eine Variante ---- */
function pickLevels(f){
  return f.stages.map(stage => stage[Math.floor(Math.random() * stage.length)]);
}
function poolSize(f){
  return f.stages.reduce((n, stage) => n + stage.length, 0);
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
  state.levels    = pickLevels(f);      // 🎲 neue Rätsel-Mischung
  state.total     = state.levels.length;
  state.levelIdx  = 0;
  state.wrong     = 0;
  state.hints     = 0;
  state.trace     = 0;
  state.startTime = Date.now();
  state.skipTyping= false;
  state.busy      = false;
  const run       = ++state.runId;      // alte Animationen ungültig machen

  dom.body.classList.remove('theme-white','theme-black');
  dom.body.classList.add(f.theme);

  dom.factionBadge.textContent  = f.badge;
  dom.terminalTitle.textContent = f.termTitle;
  dom.prompt.textContent        = f.prompt;
  dom.terminalBody.innerHTML    = '';
  dom.answerInput.value         = '';
  dom.levelTotal.textContent    = '/' + state.total;
  dom.terminal.classList.remove('is-error','is-success');

  buildProgressNodes();
  updateHud();
  showScreen('game');

  lockInput(true);
  if(!await printLines(dom.terminalBody, f.intro, CONFIG.typeSpeed, 140, run)) return;
  await printLine(dom.terminalBody, {
    t:'🎲 Rätselsatz neu gewürfelt — ' + state.total + ' von ' + poolSize(f) + ' Varianten gezogen.', c:'dim'
  }, CONFIG.typeSpeedFast);
  await sleep(250);
  if(!alive(run)) return;
  await loadLevel(0, run);
}

function buildProgressNodes(){
  dom.progressNodes.innerHTML = '';
  state.levels.forEach((lvl, i) => {
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
  const lvl = state.levels[idx];
  state.levelIdx = idx;

  /* Missions-Karte füllen */
  dom.missionLevel.textContent  = 'Level ' + (idx+1) + ' / ' + state.total;
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
  const lvl = state.levels[state.levelIdx];

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
  setProgress(done / state.total);
  markNode(state.levelIdx, 'is-done');
  dom.missionStatus.textContent = 'erledigt';
  dom.missionStatus.classList.add('is-done');

  await sleep(CONFIG.nextLevelDelay);   /* nächstes Level nach 1,5 Sekunden */
  dom.terminal.classList.remove('is-success');
  if(!alive(run)) return;

  if(done >= state.total){
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
  dom.levelValue.textContent = String(Math.min(state.levelIdx+1, state.total));
  dom.wrongValue.textContent = String(state.wrong);
  dom.traceFill.style.width  = state.trace + '%';
  setProgress(state.total ? state.levelIdx / state.total : 0);

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

dom.hintBtn.addEventListener('click', () => {
  if(state.faction === null) return;
  const lvl = state.levels[state.levelIdx];
  if(!lvl || !dom.hintBox.hidden) return;

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
  if(f) startGame(f); else resetToPicker();   // würfelt automatisch neue Rätsel
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
