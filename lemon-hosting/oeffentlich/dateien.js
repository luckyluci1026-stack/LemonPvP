/**
 * Dateien hochladen - mit Fortschritt.
 *
 * Die Datei wird roh als Body geschickt, Name und Ordner stehen in der
 * URL. Das spart einen multipart-Parser auf der Serverseite, und vor
 * allem laesst es sich streamen: eine 55-MB-Paper-Jar landet Stueck fuer
 * Stueck auf der Platte, statt erst komplett in den Arbeitsspeicher zu
 * wandern.
 *
 * XMLHttpRequest statt fetch, weil nur das einen brauchbaren
 * Upload-Fortschritt meldet. Bei einer Server-Jar ueber eine
 * Schul-Leitung ist ein Balken kein Luxus.
 */

const zone = document.getElementById('ablage');
if (zone) {
  const wahl = document.getElementById('dateiwahl');
  const balken = document.getElementById('fortschritt');
  const text = document.getElementById('fortschrittText');
  const pfad = zone.dataset.pfad || '';
  const id = zone.dataset.server;
  const zeichen = zone.dataset.csrf;

  const warteschlange = [];
  let laeuft = false;

  function melde(nachricht, schlimm = false) {
    text.textContent = nachricht;
    text.className = 'klein ' + (schlimm ? 'schlimm' : 'leise');
  }

  function naechste() {
    if (laeuft) return;
    const datei = warteschlange.shift();
    if (!datei) {
      // Fertig - die Liste neu holen, damit die neuen Dateien dastehen.
      melde('Fertig. Lade die Liste neu …');
      return location.reload();
    }
    laeuft = true;

    const anfrage = new XMLHttpRequest();
    const ziel = `/panel/${id}/hochladen`
      + `?p=${encodeURIComponent(pfad)}`
      + `&name=${encodeURIComponent(datei.name)}`
      + `&csrf=${encodeURIComponent(zeichen)}`;
    anfrage.open('POST', ziel);

    anfrage.upload.addEventListener('progress', (e) => {
      if (!e.lengthComputable) return;
      const anteil = (e.loaded / e.total) * 100;
      balken.firstElementChild.style.width = anteil.toFixed(1) + '%';
      melde(`${datei.name} — ${anteil.toFixed(0)} %`);
    });

    anfrage.addEventListener('load', () => {
      laeuft = false;
      if (anfrage.status !== 200) {
        let grund = 'Fehler ' + anfrage.status;
        try { grund = JSON.parse(anfrage.responseText).fehler || grund; } catch { /* egal */ }
        balken.classList.add('aus');
        return melde(`${datei.name}: ${grund}`, true);
      }
      naechste();
    });

    anfrage.addEventListener('error', () => {
      laeuft = false;
      balken.classList.add('aus');
      melde(`${datei.name} kam nicht durch. Verbindung weg?`, true);
    });

    anfrage.send(datei);
  }

  function nimm(dateien) {
    if (!dateien || !dateien.length) return;
    balken.parentElement.hidden = false;
    balken.classList.remove('aus');
    for (const d of dateien) warteschlange.push(d);
    melde(`${warteschlange.length} Datei${warteschlange.length === 1 ? '' : 'en'} …`);
    naechste();
  }

  wahl.addEventListener('change', () => nimm(wahl.files));

  // Ziehen und fallen lassen. Ohne preventDefault oeffnet der Browser die
  // Datei einfach in einem neuen Tab - das ist der haeufigste Grund,
  // warum Drag&Drop "nicht funktioniert".
  for (const art of ['dragenter', 'dragover']) {
    zone.addEventListener(art, (e) => { e.preventDefault(); zone.classList.add('drueber'); });
  }
  for (const art of ['dragleave', 'drop']) {
    zone.addEventListener(art, (e) => { e.preventDefault(); zone.classList.remove('drueber'); });
  }
  zone.addEventListener('drop', (e) => nimm(e.dataTransfer.files));
}
