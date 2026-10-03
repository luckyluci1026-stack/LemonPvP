/**
 * Prueft die Container-Betriebsart.
 *
 * Der erste Teil braucht kein Docker: Die Argumente fuer `docker run`
 * baut eine eigene Funktion, und die laesst sich pruefen, ohne dass
 * irgendetwas startet. Genau dort steckt das, was leicht falsch waere -
 * eine vergessene Speichergrenze faellt sonst erst auf, wenn eine
 * Klasse den Server der anderen ausbremst.
 *
 * Der zweite Teil startet wirklich einen Container und wird
 * uebersprungen, wenn Docker fehlt.
 */
import { spawnSync } from 'node:child_process';

const docker = await import('../src/docker.js');

let fehler = 0;
const ok = (t, b, extra = '') => {
  console.log(`${b ? 'OK  ' : 'FEHLER'} ${t}${extra ? ' · ' + extra : ''}`);
  if (!b) fehler++;
};

// ------------------------------------------------------------- Argumente
const args = docker.laufArgumente({
  serverId: 7,
  ordner: '/srv/lemon/server/7',
  speicherMB: 3328,
  cores: 1.25,
  port: 25566,
  bild: 'eclipse-temurin:21-jre',
  // Der Startbefehl kommt fertig zerlegt aus start.js - docker.js setzt
  // ihn nur noch hinter das Image.
  argumente: ['-Xmx2828M', '-XX:+UseG1GC', '-jar', 'server.jar',
              'nogui', '--port', '25566'],
  nutzer: '1000:1000',
});
const paar = (name) => args[args.indexOf(name) + 1];
const hat = (name) => args.includes(name);

ok('Container bekommt einen eindeutigen Namen',
   paar('--name') === 'lemon-server-7', paar('--name'));
ok('Speichergrenze wird gesetzt', paar('--memory') === '3328m', paar('--memory'));
ok('Kein Swap – sonst wäre die Grenze nur eine Empfehlung',
   paar('--memory-swap') === '3328m', paar('--memory-swap'));
ok('CPU-Grenze wird gesetzt', paar('--cpus') === '1.25', paar('--cpus'));
ok('Prozesszahl ist begrenzt', paar('--pids-limit') === '512');
ok('Keine neuen Rechte erwerbbar', paar('--security-opt') === 'no-new-privileges');
ok('Nicht als root', paar('--user') === '1000:1000');
ok('Nur der eigene Ordner ist eingebunden',
   paar('-v') === '/srv/lemon/server/7:/data' && paar('-w') === '/data');
ok('Port wird durchgereicht', paar('-p') === '25566:25566');
ok('Container räumt sich selbst auf', hat('--rm'));
ok('stdin bleibt offen – sonst käme kein Befehl an', hat('-i'));
ok('Der Startbefehl steht hinter dem Image',
   args.join(' ').endsWith(
     'eclipse-temurin:21-jre java -Xmx2828M -XX:+UseG1GC '
     + '-jar server.jar nogui --port 25566'),
   args.slice(-9).join(' '));
ok('Und wird nicht noch einmal zerlegt oder ergänzt',
   args.filter((a) => a === '-jar').length === 1,
   'genau ein -jar in der Zeile');

const ohneKennung = docker.laufArgumente({
  serverId: 1, ordner: '/x', speicherMB: 1024, cores: 1, port: 25565,
  argumente: ['-jar', 'server.jar'], nutzer: null });
ok('Ohne Kennung (Windows) fehlt --user ganz', !ohneKennung.includes('--user'));

// ------------------------------------------------------------------ Heap
ok('Heap lässt der JVM Luft neben dem Container-Limit',
   docker.heapMB(4096) === 3481, docker.heapMB(4096) + ' MB von 4096');
ok('Bei kleinen Servern bleibt ein Mindestwert',
   docker.heapMB(512) === 512, docker.heapMB(512) + ' MB');

// --------------------------------------------------------------- Abschalten
process.env.DOCKER = 'aus';
docker.vergiss();
ok('DOCKER=aus schaltet die Betriebsart ab', docker.vorhanden().geht === false,
   'Docker ' + docker.vorhanden().grund);
ok('Und der Grund lässt sich hinter „Docker" lesen',
   !/^Docker/i.test(docker.vorhanden().grund), docker.vorhanden().grund);
delete process.env.DOCKER;
docker.vergiss();

// ------------------------------------------------------------- Mit Docker
const da = docker.vorhanden();
if (!da.geht) {
  console.log(`     (Container-Probe übersprungen – Docker nicht da: ${da.grund})`);
} else if (!docker.bildDa()) {
  console.log(`     (Container-Probe übersprungen – Image ${docker.STANDARD_BILD} fehlt)`);
} else {
  ok('Docker meldet sich', Boolean(da.version), 'Version ' + da.version);

  // Die Grenze absichtlich reissen: 64 MB Container, 512 MB Heap.
  //
  // AlwaysPreTouch ist hier der Punkt - ohne den Flag fasst die JVM den
  // Heap gar nicht an und laeuft auch in einem viel zu kleinen Container
  // durch. Erst wer den Speicher wirklich belegt, merkt die Grenze. Das
  // Panel startet mit genau diesem Flag, die Probe passt also.
  const zuEng = spawnSync('docker', ['run', '--rm', '--memory', '64m',
    '--memory-swap', '64m', docker.STANDARD_BILD,
    'java', '-Xms512M', '-Xmx512M', '-XX:+AlwaysPreTouch', '-version'],
    { encoding: 'utf8', timeout: 60000 });
  ok('Die Speichergrenze greift wirklich', zuEng.status === 137,
     `512 MB Heap in 64 MB Container → exit ${zuEng.status} (137 = abgeschossen)`);

  const passt = spawnSync('docker', ['run', '--rm', '--memory', '1g',
    '--memory-swap', '1g', docker.STANDARD_BILD,
    'java', '-Xms512M', '-Xmx512M', '-XX:+AlwaysPreTouch', '-version'],
    { encoding: 'utf8', timeout: 60000 });
  ok('Und lässt in Ruhe, wer sich daran hält', passt.status === 0,
     '512 MB Heap in 1 GB Container läuft durch');

  const sieht = spawnSync('docker', ['run', '--rm', '--memory', '1g',
    docker.STANDARD_BILD, 'java', '-XX:+PrintFlagsFinal', '-version'],
    { encoding: 'utf8', timeout: 60000 });
  const heap = Number((sieht.stdout.match(/ MaxHeapSize\s+=\s+(\d+)/) || [])[1] || 0);
  ok('Die JVM richtet sich nach dem Container, nicht nach der Maschine',
     heap > 0 && heap <= 512 * 1024 * 1024,
     Math.round(heap / 1024 / 1024) + ' MB Heap bei 1 GB Container');
}

console.log(fehler ? `\n${fehler} Fehler` : '\nALLES GRUEN');
process.exit(fehler ? 1 : 0);
