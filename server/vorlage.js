const WHATSAPP = 'https://whatsapp.com/channel/0029VbE22M2JENxzB754C70C';

const NAV_PUNKTE = [
  ['start', 'Startseite'],
  ['features', 'Features'],
  ['moments', 'Aura Moments'],
  ['team', 'Team'],
  ['wiki', 'Wiki'],
  ['regeln', 'Regeln'],
  ['help', 'Hilfe'],
];

const PFADE = {
  start: '',
  features: 'features/',
  moments: 'top/10/aura-moments/',
  team: 'team/',
  wiki: 'wiki/',
  regeln: 'regeln/',
  help: 'help/',
  join: 'join/',
  impressum: 'impressum/',
  datenschutz: 'datenschutz/',
  agb: 'agb/',
};

function text(wert) {
  return String(wert)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function attribut(wert) {
  return text(wert).replace(/"/g, '&quot;');
}

function vorsilbe(seitenPfad) {
  return '../'.repeat((seitenPfad.match(/\//g) || []).length);
}

function link(seitenPfad, ziel, anker = '') {
  const zielPfad = ziel in PFADE ? PFADE[ziel] : ziel;
  if (zielPfad === seitenPfad) {
    return anker ? `#${anker}` : './';
  }
  let basis = vorsilbe(seitenPfad) + zielPfad;
  if (basis === '') {
    basis = './';
  }
  return anker ? `${basis}#${anker}` : basis;
}

function kopf(seitenPfad, titel, beschreibung) {
  const p = vorsilbe(seitenPfad);
  return `<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${titel}</title>
<meta name="description" content="${beschreibung}">
<meta name="theme-color" content="#07080a">
<link rel="icon" type="image/png" href="${p}assets/favicon.png">
<link rel="apple-touch-icon" href="${p}assets/apple-touch-icon.png">
<link rel="stylesheet" href="${p}assets/vendor/fontawesome/css/all.min.css">
<link rel="stylesheet" href="${p}assets/style.css">
</head>
<body>
`;
}

function navigation(seitenPfad, aktiv) {
  const p = vorsilbe(seitenPfad);
  const punkte = [];
  const mobil = [];
  for (const [schluessel, name] of NAV_PUNKTE) {
    const klasse = schluessel === aktiv ? ' class="active"' : '';
    punkte.push(`      <li><a href="${link(seitenPfad, schluessel)}"${klasse}>${name}</a></li>`);
    mobil.push(`  <a href="${link(seitenPfad, schluessel)}"${klasse} onclick="closeNav()">${name} <i class="fa-solid fa-chevron-right"></i></a>`);
  }
  const joinKlasse = aktiv === 'join' ? ' class="active"' : '';
  mobil.push(`  <a href="${link(seitenPfad, 'join')}"${joinKlasse} onclick="closeNav()">Beitreten <i class="fa-solid fa-chevron-right"></i></a>`);
  mobil.push(`  <a href="${WHATSAPP}" target="_blank" rel="noopener">WhatsApp Channel <i class="fa-solid fa-arrow-up-right-from-square"></i></a>`);
  return `
<nav class="nav">
  <div class="nav-inner">
    <a class="nav-logo" href="${link(seitenPfad, 'start')}">
      <img src="${p}assets/logo.webp" alt="" width="34" height="34">
      <span>BuckSMP</span>
    </a>
    <ul class="nav-links">
${punkte.join('\n')}
    </ul>
    <div class="nav-right">
      <a class="btn-wa" href="${WHATSAPP}" target="_blank" rel="noopener" aria-label="WhatsApp Channel">
        <i class="fa-brands fa-whatsapp"></i>
        <span>WhatsApp</span>
      </a>
      <a class="btn-join" href="${link(seitenPfad, 'join')}">
        <i class="fa-solid fa-play"></i>
        Join
      </a>
      <button class="ham" type="button" onclick="toggleNav(this)" aria-label="Menü öffnen"><span></span><span></span><span></span></button>
    </div>
  </div>
</nav>

<div class="mob-menu" id="mobMenu">
${mobil.join('\n')}
</div>
`;
}

function fussbereich(seitenPfad, skripte = []) {
  const p = vorsilbe(seitenPfad);
  const extra = skripte.map(skript => `<script src="${p}${skript}" defer></script>\n`).join('');
  return `
<footer class="footer">
  <div class="wrap footer-top">
    <div class="footer-brand">
      <a class="nav-logo" href="${link(seitenPfad, 'start')}">
        <img src="${p}assets/logo.webp" alt="" width="34" height="34">
        <span>BuckSMP</span>
      </a>
      <p>Der private Minecraft-Server vom Gymnasium Buckhorn. Survival, Duelle und Crossplay für Java und Bedrock.</p>
      <button class="footer-ip" type="button" onclick="copyIP()">
        BuckSMP.de
        <i class="fa-regular fa-copy"></i>
      </button>
    </div>
    <div class="footer-col">
      <h4>Server</h4>
      <a href="${link(seitenPfad, 'features')}">Features</a>
      <a href="${link(seitenPfad, 'join')}">Beitreten</a>
      <a href="${link(seitenPfad, 'regeln')}">Regeln</a>
      <a href="${link(seitenPfad, 'team')}">Team</a>
    </div>
    <div class="footer-col">
      <h4>Wissen</h4>
      <a href="${link(seitenPfad, 'wiki')}">Wiki</a>
      <a href="${link(seitenPfad, 'wiki/befehle/')}">Alle Befehle</a>
      <a href="${link(seitenPfad, 'wiki/duels/')}">Duelle</a>
      <a href="${link(seitenPfad, 'wiki/anticheat/')}">Anti-Cheat</a>
    </div>
    <div class="footer-col">
      <h4>Hilfe</h4>
      <a href="${link(seitenPfad, 'help')}">Hilfe-Center</a>
      <a href="${link(seitenPfad, 'help', 'verbindung')}">Verbindungsprobleme</a>
      <a href="${link(seitenPfad, 'help', 'melden')}">Spieler melden</a>
      <a href="${WHATSAPP}" target="_blank" rel="noopener">WhatsApp Channel <i class="fa-solid fa-arrow-up-right-from-square"></i></a>
    </div>
  </div>
  <div class="wrap footer-bottom">
    <p>© 2026 BuckSMP · Nicht offiziell mit Mojang / Microsoft assoziiert</p>
    <div class="footer-recht">
      <a href="${link(seitenPfad, 'impressum')}">Impressum</a>
      <a href="${link(seitenPfad, 'datenschutz')}">Datenschutz</a>
      <a href="${link(seitenPfad, 'agb')}">AGB</a>
      <a href="#">Nach oben <i class="fa-solid fa-arrow-up"></i></a>
    </div>
  </div>
</footer>

<div id="toast">✓ BuckSMP.de kopiert!</div>

${extra}<script src="${p}assets/script.js" defer></script>
</body>
</html>
`;
}

function brotkrumen(seitenPfad, spur) {
  const teile = [`<a href="${link(seitenPfad, 'start')}">Startseite</a>`];
  for (const [name, ziel] of spur.slice(0, -1)) {
    teile.push('<span>/</span>');
    teile.push(`<a href="${link(seitenPfad, ziel)}">${name}</a>`);
  }
  teile.push('<span>/</span>');
  teile.push(`<span>${spur[spur.length - 1][0]}</span>`);
  return `<nav class="crumbs" aria-label="Brotkrumen">
        ${teile.join('\n        ')}
      </nav>`;
}

function seitenKopf(seitenPfad, spur, titelHtml, untertitel, extra = '', danach = '') {
  const reihe = extra ? ' page-head-row' : '';
  return `
<header class="page-head">
  <div class="pixel-grid"></div>
  <div class="wrap${reihe}">
    <div>
      ${brotkrumen(seitenPfad, spur)}
      <h1 class="page-title">${titelHtml}</h1>
      <p class="page-sub">${untertitel}</p>${danach}
    </div>${extra}
  </div>
</header>
`;
}

function aufruf(etikett, titelHtml, untertitel, knoepfe) {
  return `
<section class="cta">
  <div class="pixel-grid"></div>
  <div class="wrap">
    <p class="cta-tag">${etikett}</p>
    <h2 class="cta-h">${titelHtml}</h2>
    <p class="cta-sub">${untertitel}</p>
    <div class="cta-btns">
${knoepfe}
    </div>
  </div>
</section>
`;
}

function lesezeit(html) {
  const woerter = html.replace(/<[^>]+>/g, ' ').split(/\s+/).filter(wort => wort !== '');
  return Math.max(1, Math.ceil(woerter.length / 180));
}

function seite(seitenPfad, aktiv, titel, beschreibung, inhalt, skripte = []) {
  const html = kopf(seitenPfad, titel, beschreibung) + navigation(seitenPfad, aktiv) + inhalt + fussbereich(seitenPfad, skripte);
  return html.replace(/\n{3,}/g, '\n\n');
}

module.exports = {
  text,
  attribut,
  vorsilbe,
  link,
  seitenKopf,
  aufruf,
  lesezeit,
  seite,
};
