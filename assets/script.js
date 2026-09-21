// ============================================================
//  BuckSMP – gemeinsames Script fuer alle Seiten.
//  Jeder Block prueft zuerst, ob die noetigen Elemente auf der
//  aktuellen Seite ueberhaupt existieren - so kann diese eine Datei
//  von JEDER Seite eingebunden werden, ohne dass Konsole-Fehler
//  entstehen, nur weil z.B. die Leaderboard-Tabelle auf einer
//  anderen Seite gar nicht vorkommt.
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

/* ---------------- Leaderboard (Startseite) ---------------- */
(function leaderboard(){
  const body = document.getElementById('lbBody');
  if (!body) return;

  // Minecraft-Spielerkoepfe von minotar.net
  function head(name){return`<img src="https://minotar.net/helm/${name}/34" alt="${name}" style="width:34px;height:34px;border-radius:4px;display:block;image-rendering:pixelated" onerror="this.onerror=null;this.src='https://minotar.net/helm/Steve/34'">`}

  // Platzhalter-Daten, bis das Leaderboard live mit dem Server
  // synchronisiert wird (siehe .lb-foot-Text auf der Seite).
  const PLAYERS=[
    {name:'xNovaPvP',   elo:3420,kills:4820,deaths:574, wins:924,losses:210,kits:{Sword:{kills:1240,deaths:120,wins:310},UHC:{kills:890,deaths:88,wins:180},SMP:{kills:1100,deaths:180,wins:220},Mace:{kills:820,deaths:110,wins:144},Crystal:{kills:770,deaths:76,wins:70},Shield:{kills:600,deaths:60,wins:80}}},
    {name:'CrystalKing',elo:3180,kills:4100,deaths:578, wins:801,losses:198,kits:{Sword:{kills:900,deaths:100,wins:200},UHC:{kills:750,deaths:90,wins:160},SMP:{kills:980,deaths:155,wins:200},Mace:{kills:700,deaths:95,wins:130},Crystal:{kills:770,deaths:138,wins:111},Shield:{kills:400,deaths:60,wins:90}}},
    {name:'ShadowBlade', elo:2870,kills:3640,deaths:577, wins:650,losses:188,kits:{Sword:{kills:1100,deaths:140,wins:240},UHC:{kills:680,deaths:88,wins:130},SMP:{kills:780,deaths:140,wins:140},Mace:{kills:640,deaths:120,wins:90},Crystal:{kills:440,deaths:89,wins:50},Shield:{kills:300,deaths:55,wins:44}}},
    {name:'ZeroPing_',  elo:2540,kills:2970,deaths:512, wins:512,losses:160,kits:{Sword:{kills:800,deaths:100,wins:160},UHC:{kills:560,deaths:80,wins:100},SMP:{kills:700,deaths:130,wins:120},Mace:{kills:530,deaths:110,wins:82},Crystal:{kills:380,deaths:92,wins:50},Shield:{kills:220,deaths:50,wins:44}}},
    {name:'BuckLegend', elo:2210,kills:2490,deaths:479, wins:478,losses:144,kits:{Sword:{kills:700,deaths:90,wins:140},UHC:{kills:480,deaths:78,wins:90},SMP:{kills:580,deaths:120,wins:110},Mace:{kills:430,deaths:100,wins:80},Crystal:{kills:300,deaths:91,wins:58},Shield:{kills:120,deaths:44,wins:30}}},
    {name:'IronFist99', elo:1850,kills:1830,deaths:389, wins:390,losses:130,kits:{Sword:{kills:540,deaths:88,wins:120},UHC:{kills:340,deaths:65,wins:80},SMP:{kills:440,deaths:100,wins:90},Mace:{kills:310,deaths:80,wins:60},Crystal:{kills:200,deaths:56,wins:40},Shield:{kills:110,deaths:44,wins:26}}},
    {name:'SwiftArrow', elo:1340,kills:1100,deaths:282, wins:284,losses:110,kits:{Sword:{kills:330,deaths:70,wins:90},UHC:{kills:210,deaths:50,wins:60},SMP:{kills:270,deaths:80,wins:70},Mace:{kills:180,deaths:50,wins:40},Crystal:{kills:110,deaths:32,wins:24},Shield:{kills:70,deaths:28,wins:18}}},
    {name:'GreenKnight',elo: 980,kills: 590,deaths:190, wins:190,losses: 90,kits:{Sword:{kills:180,deaths:50,wins:60},UHC:{kills:110,deaths:38,wins:40},SMP:{kills:140,deaths:55,wins:44},Mace:{kills:90, deaths:30,wins:28},Crystal:{kills:70, deaths:17,wins:18},Shield:{kills:55,deaths:22,wins:16}}},
    {name:'StarDust44', elo: 620,kills: 226,deaths:103, wins:103,losses: 68,kits:{Sword:{kills:70, deaths:30,wins:34},UHC:{kills:42, deaths:22,wins:22},SMP:{kills:50, deaths:28,wins:24},Mace:{kills:38, deaths:16,wins:14},Crystal:{kills:26, deaths:7, wins:9},Shield:{kills:18,deaths:12,wins:6}}},
    {name:'NewcomerX',  elo: 320,kills:  31,deaths: 28, wins: 28,losses: 40,kits:{Sword:{kills:10, deaths:8, wins:10},UHC:{kills:6,  deaths:6, wins:6},SMP:{kills:8,  deaths:8, wins:7},Mace:{kills:5,  deaths:4, wins:3},Crystal:{kills:2,  deaths:2, wins:2},Shield:{kills:2,deaths:2,wins:2}}},
  ];
  const ISVG=(id)=>`<svg class="ic-kit" viewBox="0 0 40 40"><use href="#${id}"/></svg>`;
  const KITS=[
    {key:'Sword',  sv:ISVG('i-sword')  },
    {key:'UHC',    sv:ISVG('i-uhc')    },
    {key:'SMP',    sv:ISVG('i-smp')    },
    {key:'Mace',   sv:ISVG('i-mace')   },
    {key:'Crystal',sv:ISVG('i-crystal')},
    {key:'Shield', sv:ISVG('i-shield-kit')},
  ];

  function getRank(e){
    if(e>=3000)return{label:'Netherite',color:'#7a6060',bg:'rgba(122,96,96,.12)'};
    if(e>=1501)return{label:'Diamant',  color:'#4de8e8',bg:'rgba(77,232,232,.1)' };
    if(e>= 751)return{label:'Gold',     color:'#ffd700',bg:'rgba(255,215,0,.1)'  };
    if(e>= 501)return{label:'Eisen',    color:'#c8c8c8',bg:'rgba(200,200,200,.1)'};
    return            {label:'Kupfer',   color:'#e8783a',bg:'rgba(232,120,58,.1)' };
  }

  function kd(k,d){return d===0?k.toFixed(1):(k/d).toFixed(2)}

  function buildRow(p,i,globalIdx){
    const r=getRank(p.elo);
    const pc=globalIdx===0?'top1':globalIdx===1?'top2':globalIdx===2?'top3':'';
    const pos=globalIdx===0?'<svg width="18" height="18" viewBox="0 0 24 24" fill="#ffd700"><path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6z"/></svg>':globalIdx+1;
    const row=document.createElement('div');
    row.className='lb-row';
    row.innerHTML=`
      <div class="lb-pos ${pc}">${pos}</div>
      <div class="lb-player">
        <div class="lb-avatar">${head(p.name)}</div>
        <div class="lb-name">${p.name}</div>
      </div>
      <div class="lb-rank-cell">
        <span class="lb-badge" style="color:${r.color};border-color:${r.color}44;background:${r.bg}">${r.label}</span>
      </div>
      <div class="lb-elo">${p.elo}</div>
      <div class="lb-kd-val">${kd(p.kills,p.deaths)}</div>
      <div class="lb-wins-val">${p.wins}</div>`;
    row.addEventListener('click',()=>openModal(p));
    return row;
  }

  function renderLB(list){
    const empty=document.getElementById('lbEmpty');
    body.innerHTML='';
    if(!list.length){empty.style.display='block';return;}
    empty.style.display='none';
    list.forEach((p)=>body.appendChild(buildRow(p,0,PLAYERS.indexOf(p))));
  }

  renderLB(PLAYERS);

  window.filterLB=function(v){
    const q=v.trim().toLowerCase();
    document.getElementById('lbSearchClear').style.display=q?'block':'none';
    renderLB(q?PLAYERS.filter(p=>p.name.toLowerCase().includes(q)):PLAYERS);
  };
  window.clearSearch=function(){
    document.getElementById('lbSearch').value='';
    document.getElementById('lbSearchClear').style.display='none';
    renderLB(PLAYERS);
  };

  window.openModal=function(p){
    const r=getRank(p.elo);
    document.getElementById('mAvatar').innerHTML=`<img src="https://minotar.net/helm/${p.name}/56" alt="${p.name}" style="width:56px;height:56px;border-radius:8px;display:block;image-rendering:pixelated" onerror="this.onerror=null;this.src='https://minotar.net/helm/Steve/56'">`;
    document.getElementById('mName').textContent=p.name;
    document.getElementById('mBadge').innerHTML=`<span class="lb-badge" style="color:${r.color};border-color:${r.color}44;background:${r.bg}">${r.label}</span>`;
    document.getElementById('mElo').textContent=p.elo;
    document.getElementById('mKills').textContent=p.kills.toLocaleString();
    document.getElementById('mDeaths').textContent=p.deaths.toLocaleString();
    document.getElementById('mKD').textContent=kd(p.kills,p.deaths);
    document.getElementById('mWins').textContent=p.wins.toLocaleString();
    document.getElementById('mLosses').textContent=p.losses.toLocaleString();
    const kitsEl=document.getElementById('mKits');
    kitsEl.innerHTML='';
    const maxWins=Math.max(...KITS.map(k=>p.kits[k.key].wins));
    KITS.forEach(k=>{
      const ks=p.kits[k.key];
      const pct=maxWins?Math.round((ks.wins/maxWins)*100):0;
      kitsEl.innerHTML+=`<div class="modal-kit">
        <div class="modal-kit-icon">${k.sv}</div>
        <div class="modal-kit-name">${k.key}</div>
        <div class="modal-kit-stats">
          <span class="modal-kit-s"><strong>${ks.kills}</strong> Kills</span>
          <span class="modal-kit-s"><strong>${kd(ks.kills,ks.deaths)}</strong> K/D</span>
          <span class="modal-kit-s"><strong>${ks.wins}</strong> Wins</span>
        </div>
        <div class="modal-kit-bar-wrap">
          <div class="modal-kit-bar-bg"><div class="modal-kit-bar" style="width:${pct}%"></div></div>
        </div>
      </div>`;
    });
    document.getElementById('modalOverlay').classList.add('open');
    document.body.style.overflow='hidden';
  };
  window.closeModal=function(e){
    const overlay=document.getElementById('modalOverlay');
    if(e&&e.target!==overlay&&!e.target.classList.contains('modal-close'))return;
    overlay.classList.remove('open');
    document.body.style.overflow='';
  };
  document.addEventListener('keydown',e=>{
    const overlay=document.getElementById('modalOverlay');
    if(overlay&&e.key==='Escape')window.closeModal({target:overlay});
  });
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
