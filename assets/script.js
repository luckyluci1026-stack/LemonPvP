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

/* ---------------- Live-Zaehler (Startseite) ---------------- */
(function liveCounter(){
  if (!document.getElementById('liveCount') && !document.getElementById('statN')) return;
  let cnt = 247;
  setInterval(() => {
    cnt += Math.floor(Math.random() * 6) - 3;
    cnt = Math.max(180, Math.min(320, cnt));
    ['liveCount','statN'].forEach(id => { const el = document.getElementById(id); if (el) el.textContent = cnt; });
  }, 5000);
})();

/* ---------------- Top 10 Aura Moments (/top/10/aura-moments/) ----------------
 *
 * Neuen Moment hinzufuegen: einfach einen weiteren Eintrag unten ins
 * MOMENTS-Array kopieren und die Felder anpassen:
 *   rank        - Platz 1-10 (bestimmt nur die Gold/Silber/Bronze-Faerbung
 *                 bei 1-3, die Reihenfolge kommt von der Position im Array)
 *   title       - Titel des Moments
 *   player      - wer die Aktion gemacht hat
 *   description - kurze Beschreibung, 1-2 Saetze reichen
 *   thumbnail   - Bild-URL fuers Vorschaubild (z.B. ein hochgeladener
 *                 Screenshot/Thumbnail, auch relative Pfade gehen)
 *   link        - wohin der Klick fuehrt (z.B. YouTube/Twitter/Discord-Link
 *                 zum vollen Clip) - "#" wenn noch kein Clip verlinkt ist
 * -------------------------------------------------------------------- */
(function auraMoments(){
  const grid = document.getElementById('momentsGrid');
  if (!grid) return;

  const MOMENTS=[
    {title:'Platzhalter-Titel #1', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #2', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #3', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #4', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #5', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #6', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #7', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #8', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #9', player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
    {title:'Platzhalter-Titel #10',player:'SpielerName', description:'Kurze Beschreibung des Moments hier eintragen.', thumbnail:'', link:'#'},
  ];

  const FALLBACK_THUMB = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 180'%3E%3Crect width='320' height='180' fill='%23161616'/%3E%3Ctext x='160' y='96' font-family='sans-serif' font-size='14' fill='%23555' text-anchor='middle'%3EKein Bild hinterlegt%3C/text%3E%3C/svg%3E";

  function rankClass(i){ return i===0?'top1':i===1?'top2':i===2?'top3':''; }

  grid.innerHTML = MOMENTS.map((m,i)=>`
    <a class="moment" href="${m.link}" target="${m.link&&m.link!=='#'?'_blank':'_self'}" rel="noopener">
      <div class="moment-thumb">
        <img src="${m.thumbnail||FALLBACK_THUMB}" alt="${m.title}" onerror="this.onerror=null;this.src='${FALLBACK_THUMB}'">
        <div class="moment-rank ${rankClass(i)}">#${i+1}</div>
        <div class="moment-play">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
        </div>
      </div>
      <div class="moment-body">
        <div class="moment-title">${m.title}</div>
        <div class="moment-player">${m.player}</div>
        <p class="moment-desc">${m.description}</p>
      </div>
    </a>
  `).join('');
})();
