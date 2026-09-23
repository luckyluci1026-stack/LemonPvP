// ============================================================
//  BuckSMP – gemeinsames Script fuer alle Seiten.
//  Jeder Block prueft zuerst, ob die noetigen Elemente auf der
//  aktuellen Seite ueberhaupt existieren - so kann diese eine Datei
//  von JEDER Seite eingebunden werden, ohne dass Konsole-Fehler
//  entstehen, nur weil z.B. das Aura-Moments-Grid auf der Startseite
//  gar nicht vorkommt.
// ============================================================

/* ---------------- Mobile-Nav (alle Seiten) ---------------- */
function toggleNav(btn) {
  const m = document.getElementById('mobMenu');
  if (!m) return;
  const o = m.classList.toggle('open');
  const s = btn.querySelectorAll('span');
  if (o) { s[0].style.transform='rotate(45deg) translate(5px,5px)'; s[1].style.opacity='0'; s[2].style.transform='rotate(-45deg) translate(5px,-5px)'; }
  else   { s.forEach(x => { x.style.transform=''; x.style.opacity=''; }); }
}
function closeNav() {
  const m = document.getElementById('mobMenu');
  if (!m) return;
  m.classList.remove('open');
  document.querySelectorAll('.ham span').forEach(x => { x.style.transform=''; x.style.opacity=''; });
}

/* ---------------- IP kopieren (Startseite) ---------------- */
const SERVER_IP = 'lemon-servers.de';
let toastTimer;
function copyIP() {
  navigator.clipboard.writeText(SERVER_IP).then(() => {
    const feed = document.getElementById('ipFeed');
    const ico  = document.getElementById('copyIco');
    const txt  = document.getElementById('copyTxt');
    if (feed) feed.classList.add('show');
    if (txt)  txt.textContent = 'Kopiert!';
    if (ico)  ico.innerHTML = '<polyline points="20 6 9 17 4 12" stroke="currentColor" stroke-width="2.5" fill="none"/>';
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toast.classList.remove('show');
      if (feed) feed.classList.remove('show');
      if (txt)  txt.textContent = 'Kopieren';
      if (ico)  ico.innerHTML = '<rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>';
    }, 2500);
  });
}

/* ---------------- Live-Serverstatus (Startseite) ----------------
 *
 * Echte Daten von api.mcsrvstat.us statt fest eingetragener Zahlen -
 * fragt Online/Offline + aktuelle Spieleranzahl fuer lemon-servers.de
 * ab und aktualisiert #liveStatus/#liveCount/#statN/#liveDot damit.
 * Die Versions-Pill (#verPill) bleibt bewusst der manuell gepflegte
 * Text aus dem HTML: die API liefert nur die Kern-Protokollversion
 * des Servers, nicht die per ViaVersion/ViaBackwards unterstuetzte
 * Bandbreite - ein Live-Ueberschreiben wuerde hier also eher weniger
 * korrekte Infos zeigen als die gepflegte Angabe.
 *
 * Schlaegt die Abfrage fehl (Netzwerk, API down, o.ae.), bleiben
 * einfach die im HTML hinterlegten Platzhalter-Werte stehen statt
 * irgendwas Falsches anzuzeigen.
 * -------------------------------------------------------------- */
(function liveServerStatus(){
  const elCount  = document.getElementById('liveCount');
  const elStatN  = document.getElementById('statN');
  const elStatus = document.getElementById('liveStatus');
  const elDot    = document.getElementById('liveDot');
  if (!elCount && !elStatN) return;

  const SERVER_HOST = 'lemon-servers.de';
  const ABFRAGE_TAKT_MS = 60000;

  async function aktualisieren(){
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    try {
      const res = await fetch(`https://api.mcsrvstat.us/3/${SERVER_HOST}`, { signal: controller.signal });
      if (!res.ok) return;
      const data = await res.json();
      const online = !!data.online;
      const spieler = online && data.players && typeof data.players.online === 'number'
        ? String(data.players.online) : (online ? '0' : '–');

      if (elStatus) elStatus.textContent = online ? 'online' : 'offline';
      if (elCount)  elCount.textContent  = spieler;
      if (elStatN)  elStatN.textContent  = spieler;
      if (elDot)    elDot.style.animationPlayState = online ? 'running' : 'paused';
      if (elDot)    elDot.style.background = online ? '' : 'var(--muted)';
    } catch (err) {
      // Fetch fehlgeschlagen/Timeout - Platzhalter aus dem HTML stehen lassen.
    } finally {
      clearTimeout(timeout);
    }
  }

  aktualisieren();
  setInterval(aktualisieren, ABFRAGE_TAKT_MS);
})();

/* ---------------- Top 10 Aura Moments (/top/10/aura-moments/) ----------------
 *
 * Neuen Moment hinzufuegen: NUR die Datei nach media/ hochladen, benannt
 * nach dem Platz - z.B. media/1.mp4 fuer Platz 1, media/7.jpg fuer
 * Platz 7 (siehe media/README.md). Mehr ist nicht noetig - diese Seite
 * probiert beim Laden fuer jeden Platz 1-10 selbst aus, ob es dafuer
 * ein Video (.mp4, zuerst versucht) oder ein Foto (.jpg) gibt, und
 * zeigt sonst einen Platzhalter. Kein Array, kein Code hier anfassen.
 *
 * Titel/Spieler/Beschreibung sind optional und rein kosmetisch - unten
 * im MOMENTS-Array nach Platz eintragen (Index 0 = Platz 1). Leer
 * lassen ist ok, dann steht nur "Platz N" da.
 * -------------------------------------------------------------------- */
(function auraMoments(){
  const grid = document.getElementById('momentsGrid');
  if (!grid) return;

  const RAENGE = 10;
  const MEDIA_ORDNER = 'media/';

  const MOMENTS = [
    {title:'', player:'', description:''}, // Platz 1
    {title:'', player:'', description:''}, // Platz 2
    {title:'', player:'', description:''}, // Platz 3
    {title:'', player:'', description:''}, // Platz 4
    {title:'', player:'', description:''}, // Platz 5
    {title:'', player:'', description:''}, // Platz 6
    {title:'', player:'', description:''}, // Platz 7
    {title:'', player:'', description:''}, // Platz 8
    {title:'', player:'', description:''}, // Platz 9
    {title:'', player:'', description:''}, // Platz 10
  ];

  const FALLBACK_THUMB = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 180'%3E%3Crect width='320' height='180' fill='%23161616'/%3E%3Ctext x='160' y='96' font-family='sans-serif' font-size='13' fill='%23555' text-anchor='middle'%3ENoch kein Clip hochgeladen%3C/text%3E%3C/svg%3E";

  function rankClass(i){ return i===0?'top1':i===1?'top2':i===2?'top3':''; }

  function karteBauen(rank, meta){
    const wrap = document.createElement('div');
    wrap.className = 'moment';
    wrap.tabIndex = 0;
    wrap.setAttribute('role', 'button');

    const thumb = document.createElement('div');
    thumb.className = 'moment-thumb';

    // Erst Video versuchen (media/{rank}.mp4) - klappt das nicht (kein
    // Treffer -> 'error'-Event, wie bei img.onerror), auf Foto
    // (media/{rank}.jpg) umschwenken, und wenn das auch fehlt, auf den
    // Platzhalter. wrap.dataset.media* haelt fest, was am Ende wirklich
    // da ist, fuers Oeffnen der Lightbox beim Klick.
    const video = document.createElement('video');
    video.muted = true; video.loop = true; video.playsInline = true; video.preload = 'metadata';
    video.addEventListener('loadedmetadata', () => {
      wrap.dataset.mediaType = 'video';
      wrap.dataset.mediaSrc = video.src;
    });
    video.addEventListener('mouseenter', () => video.play().catch(() => {}));
    video.addEventListener('mouseleave', () => { video.pause(); video.currentTime = 0; });
    video.addEventListener('error', () => {
      video.remove();
      const img = document.createElement('img');
      img.alt = meta.title || `Platz ${rank}`;
      img.addEventListener('load', () => {
        // Feuert auch fuer die Zuweisung von FALLBACK_THUMB unten (ist ja
        // auch ein "erfolgreiches Laden") - das darf NICHT als echtes
        // Foto durchgehen, sonst oeffnet die Lightbox spaeter leere
        // Platzhalter-Karten mit dem Platzhalterbild als "Inhalt".
        if (img.src === FALLBACK_THUMB) return;
        wrap.dataset.mediaType = 'image';
        wrap.dataset.mediaSrc = img.src;
      }, { once:true });
      img.addEventListener('error', () => {
        img.onerror = null;
        img.src = FALLBACK_THUMB;
      }, { once:true });
      img.src = `${MEDIA_ORDNER}${rank}.jpg`;
      thumb.prepend(img);
    }, { once:true });
    video.src = `${MEDIA_ORDNER}${rank}.mp4`;
    thumb.appendChild(video);

    const rankEl = document.createElement('div');
    rankEl.className = `moment-rank ${rankClass(rank - 1)}`;
    rankEl.textContent = `#${rank}`;
    thumb.appendChild(rankEl);

    const play = document.createElement('div');
    play.className = 'moment-play';
    play.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>';
    thumb.appendChild(play);

    wrap.appendChild(thumb);

    const body = document.createElement('div');
    body.className = 'moment-body';
    const titel = meta.title || `Platz ${rank}`;
    body.innerHTML = `
      <div class="moment-title"></div>
      ${meta.player ? '<div class="moment-player"></div>' : ''}
      ${meta.description ? '<p class="moment-desc"></p>' : ''}
    `;
    body.querySelector('.moment-title').textContent = titel;
    if (meta.player) body.querySelector('.moment-player').textContent = meta.player;
    if (meta.description) body.querySelector('.moment-desc').textContent = meta.description;
    wrap.appendChild(body);

    function oeffnen(){
      if (!wrap.dataset.mediaType) return; // noch kein Clip hochgeladen - nichts zu zeigen
      lightboxOeffnen(wrap.dataset.mediaType, wrap.dataset.mediaSrc, titel);
    }
    wrap.addEventListener('click', oeffnen);
    wrap.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); oeffnen(); }
    });

    return wrap;
  }

  for (let rank = 1; rank <= RAENGE; rank++) {
    grid.appendChild(karteBauen(rank, MOMENTS[rank - 1] || {}));
  }

  /* ---- Lightbox: Klick auf eine Karte spielt Video/Foto gross ab ---- */
  const lightbox = document.createElement('div');
  lightbox.className = 'lightbox';
  lightbox.innerHTML = `
    <div class="lightbox-inner">
      <button class="lightbox-close" aria-label="Schließen" type="button">&times;</button>
      <div class="lightbox-media"></div>
      <div class="lightbox-caption"></div>
    </div>
  `;
  document.body.appendChild(lightbox);
  const lbMedia = lightbox.querySelector('.lightbox-media');
  const lbCaption = lightbox.querySelector('.lightbox-caption');

  function lightboxOeffnen(typ, src, titel){
    lbMedia.innerHTML = '';
    if (typ === 'video') {
      const v = document.createElement('video');
      v.src = src; v.controls = true; v.autoplay = true; v.playsInline = true;
      lbMedia.appendChild(v);
    } else {
      const img = document.createElement('img');
      img.src = src; img.alt = titel;
      lbMedia.appendChild(img);
    }
    lbCaption.textContent = titel;
    lightbox.classList.add('open');
    document.body.style.overflow = 'hidden';
  }
  function lightboxSchliessen(){
    lightbox.classList.remove('open');
    lbMedia.innerHTML = ''; // Video wirklich stoppen, nicht nur verstecken
    document.body.style.overflow = '';
  }
  lightbox.querySelector('.lightbox-close').addEventListener('click', lightboxSchliessen);
  lightbox.addEventListener('click', e => { if (e.target === lightbox) lightboxSchliessen(); });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && lightbox.classList.contains('open')) lightboxSchliessen();
  });
})();
