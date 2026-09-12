/**
 * Der Startbefehl - was Pterodactyl im Adminbereich "Startup" nennt.
 *
 * Bisher standen die JVM-Flaggen fest im Code. Das reicht fuer den
 * Normalfall und wird sofort laestig, sobald jemand etwas anderes
 * braucht: eine aelter benannte Jar, Aikar's Flags in einer neueren
 * Fassung, `--nogui` weglassen, ein Velocity-Proxy ohne Weltordner.
 *
 * Deshalb steht der Befehl jetzt als Vorlage in der Datenbank, mit
 * Platzhaltern:
 *
 *   java {{FLAGGEN}} -jar {{JAR}} nogui --port {{PORT}}
 *
 * Wer nichts einstellt, bekommt genau das, was vorher fest im Code
 * stand - es aendert sich also nichts, solange niemand etwas aendert.
 *
 * Der wichtige Teil ist unten: `zerlege()` zerlegt die Zeile selbst,
 * statt sie an eine Shell zu geben. Ein `; rm -rf /` im Feld ist damit
 * ein Argument namens ";" und keine zweite Anweisung - der Server
 * bekaeme es zu sehen und wuesste nichts damit anzufangen. Genau so soll
 * es sein.
 */

/** Die Flaggen, die vorher fest im Code standen. */
export const STANDARD_FLAGGEN = [
  '-XX:+UseG1GC',
  '-XX:+ParallelRefProcEnabled',
  '-XX:MaxGCPauseMillis=200',
  '-XX:+UnlockExperimentalVMOptions',
  '-XX:+DisableExplicitGC',
  '-XX:+AlwaysPreTouch',
  '-Dfile.encoding=UTF-8',
].join(' ');

export const STANDARD_BEFEHL = 'java {{SPEICHER}} {{FLAGGEN}} -jar {{JAR}} nogui --port {{PORT}}';

export const STANDARD_JAR = 'server.jar';

/**
 * Eine Befehlszeile in Argumente zerlegen.
 *
 * Anfuehrungszeichen halten zusammen, was zusammengehoert - ein Pfad mit
 * Leerzeichen soll ein Argument bleiben. Alles andere trennt am
 * Leerraum. Was hier NICHT passiert: Ersetzungen, Umleitungen,
 * Verkettungen. Es gibt keine Shell, also gibt es auch nichts zu
 * missbrauchen.
 */
export function zerlege(zeile) {
  const raus = [];
  let stueck = '';
  let offen = null;     // ' oder " - oder nichts
  let hatWas = false;

  for (const zeichen of String(zeile || '')) {
    if (offen) {
      if (zeichen === offen) offen = null;
      else stueck += zeichen;
      continue;
    }
    if (zeichen === '"' || zeichen === "'") { offen = zeichen; hatWas = true; continue; }
    if (/\s/.test(zeichen)) {
      if (stueck || hatWas) { raus.push(stueck); stueck = ''; hatWas = false; }
      continue;
    }
    stueck += zeichen;
  }
  if (stueck || hatWas) raus.push(stueck);
  return raus;
}

/**
 * Ein Dateiname fuer die Jar - und sonst nichts.
 *
 * Der Wert wird zum Argument von `-jar`, also zu einem Pfad im
 * Serverordner. Ein `../` darin zeigte auf einen fremden Server.
 */
export function jarOk(roh) {
  const text = String(roh || '').trim();
  if (!text) return '';
  if (text.includes('/') || text.includes('\\') || text.includes('..')) return '';
  return /^[\w.@ +-]{1,80}$/.test(text) ? text : '';
}

/**
 * Die Flaggen entschaerfen.
 *
 * `-jar` gehoert nicht hierher: Wer es hier einschmuggelt, startete eine
 * andere Datei als die eingestellte. Und alles, was nach Umleitung oder
 * Verkettung aussieht, hat in einer Argumentliste nichts verloren - es
 * wuerde ohnehin nur als sinnloses Argument beim Server landen, aber
 * eine klare Absage ist ehrlicher als stilles Durchreichen.
 */
export function flaggenOk(roh) {
  const stuecke = zerlege(roh);
  const schlecht = stuecke.filter((s) =>
    s === '-jar' || /[;&|><`$]/.test(s));
  if (schlecht.length) {
    return { fehler: `Das geht hier nicht: ${schlecht.slice(0, 3).join(' ')}`
                   + ' – hier stehen nur Java-Optionen, keine Befehle.' };
  }
  if (stuecke.length > 60) {
    return { fehler: 'Das sind sehr viele Optionen – mehr als 60 nimmt das Panel nicht.' };
  }
  return { flaggen: stuecke.join(' ') };
}

/** Der Befehl muss mit `java` anfangen und die Platzhalter dabeihaben. */
export function befehlOk(roh) {
  const text = String(roh || '').trim();
  if (!text) return { befehl: '' };          // leer = Standard
  const stuecke = zerlege(text);
  if (stuecke[0] !== 'java') {
    return { fehler: 'Der Befehl muss mit „java" anfangen.' };
  }
  if (stuecke.some((s) => /[;&|><`]/.test(s))) {
    return { fehler: 'Zeichen wie ; & | > < gehören hier nicht hin – '
                   + 'der Befehl wird nicht von einer Shell ausgeführt.' };
  }
  if (!text.includes('{{JAR}}')) {
    return { fehler: 'Ohne {{JAR}} weiß niemand, welche Datei gestartet werden soll.' };
  }
  return { befehl: text };
}

/**
 * Aus Vorlage und Werten die fertige Argumentliste bauen.
 *
 * `java` selbst faellt vorne weg - das ist im Container der Befehl und
 * direkt der Programmname; die Argumente fangen dahinter an.
 */
export function baueArgumente(server, { speicherMB, port, heapMB }) {
  const vorlage = server.startbefehl || STANDARD_BEFEHL;
  const jar = jarOk(server.jar_datei) || STANDARD_JAR;
  const flaggen = server.start_flaggen ?? STANDARD_FLAGGEN;
  const heap = heapMB ?? speicherMB;

  // Platzhalter, die zu MEHREREN Argumenten werden - eine Liste von
  // Flaggen ist genau das.
  const listen = {
    '{{SPEICHER}}': [`-Xms${Math.min(heap, 512)}M`, `-Xmx${heap}M`],
    '{{FLAGGEN}}': zerlege(flaggen),
  };
  // Und die, die genau EIN Argument bleiben.
  const einzeln = {
    '{{JAR}}': jar,
    '{{PORT}}': String(port),
    '{{RAM}}': String(heap),
  };

  /**
   * Erst zerlegen, dann einsetzen - nicht umgekehrt.
   *
   * Andersherum wuerde aus einer Jar namens "mein server.jar" beim
   * Zerlegen wieder zweierlei: "mein" und "server.jar". Der Server
   * suchte dann nach einer Datei, die es nicht gibt, und niemand kaeme
   * darauf, dass das Leerzeichen schuld ist.
   */
  const raus = [];
  for (const stueck of zerlege(vorlage)) {
    if (listen[stueck]) { raus.push(...listen[stueck]); continue; }
    let wert = stueck;
    for (const [platz, ersatz] of Object.entries(einzeln)) {
      wert = wert.split(platz).join(ersatz);
    }
    // Ein Listen-Platzhalter mitten in einem Wort ergibt keine Liste
    // mehr - dann eben aneinandergehaengt. Kommt praktisch nicht vor,
    // soll aber auch nicht stumm verschwinden.
    for (const [platz, ersatz] of Object.entries(listen)) {
      wert = wert.split(platz).join(ersatz.join(' '));
    }
    raus.push(wert);
  }

  // Das führende "java" gehoert nicht in die Argumentliste.
  return raus[0] === 'java' ? raus.slice(1) : raus;
}

/** Wie der Befehl am Ende aussieht - fuer die Anzeige im Panel. */
export function vorschau(server, werte) {
  return ['java', ...baueArgumente(server, werte)].join(' ');
}
