const SERVER_IP = 'bucksmp.de';

const TEAM = [
  { id: 'lemon', name: 'Lemonightt', rolle: 'Owner' },
  { id: 'pneu', name: '.Pneuer13', rolle: 'Owner' },
  { id: 'owner', name: 'Catze99', rolle: 'Owner' },
  { id: 'manager', name: 'Noah', rolle: 'Manager' },
  { id: 'admin', name: 'Nani0000', rolle: 'Admin' },
  { id: 'mod', name: '.Colixiander', rolle: 'Moderator' },
  { id: 'sup', name: '.erbse', rolle: 'Supporter' },
];

const ROLLEN_FARBE = {
  Owner: 'gold',
  Admin: 'redstone',
  Manager: 'lapis',
  Moderator: 'emerald',
  Supporter: 'amethyst',
};

const MOMENTS = [
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
  { title: '', player: '', description: '' },
];

function toggleNav(btn) {
  const menu = document.getElementById('mobMenu');
  if (!menu) {
    return;
  }
  const isOpen = menu.classList.toggle('open');
  const bars = btn.querySelectorAll('span');
  if (isOpen) {
    bars[0].style.transform = 'translateY(7px) rotate(45deg)';
    bars[1].style.opacity = '0';
    bars[2].style.transform = 'translateY(-7px) rotate(-45deg)';
  } else {
    bars.forEach(bar => {
      bar.style.transform = '';
      bar.style.opacity = '';
    });
  }
}

function closeNav() {
  const menu = document.getElementById('mobMenu');
  if (!menu) {
    return;
  }
  menu.classList.remove('open');
  document.querySelectorAll('.ham span').forEach(bar => {
    bar.style.transform = '';
    bar.style.opacity = '';
  });
}

function textKopieren(text) {
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(text);
  }

  const feld = document.createElement('textarea');
  feld.value = text;
  feld.style.position = 'fixed';
  feld.style.opacity = '0';
  document.body.appendChild(feld);
  feld.select();
  document.execCommand('copy');
  feld.remove();
  return Promise.resolve();
}

let toastTimer;

function copyIP() {
  textKopieren(SERVER_IP).then(() => {
    const icon = document.getElementById('copyIco');
    const label = document.getElementById('copyTxt');

    if (label) {
      label.textContent = 'Kopiert!';
    }
    if (icon) {
      icon.className = 'fa-solid fa-check';
    }

    const toast = document.getElementById('toast');
    if (!toast) {
      return;
    }
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toast.classList.remove('show');
      if (label) {
        label.textContent = 'Kopieren';
      }
      if (icon) {
        icon.className = 'fa-regular fa-copy';
      }
    }, 2500);
  });
}

(function tabsSteuern() {
  const knoepfe = Array.from(document.querySelectorAll('.tab'));
  if (knoepfe.length === 0) {
    return;
  }

  function tabZeigen(gewaehlterKnopf) {
    knoepfe.forEach(knopf => {
      const istGewaehlt = knopf === gewaehlterKnopf;
      knopf.classList.toggle('aktiv', istGewaehlt);
      knopf.setAttribute('aria-selected', istGewaehlt ? 'true' : 'false');

      const panel = document.getElementById(knopf.dataset.ziel);
      if (panel) {
        panel.hidden = !istGewaehlt;
      }
    });
  }

  knoepfe.forEach(knopf => {
    knopf.addEventListener('click', () => tabZeigen(knopf));
  });

  const ankerAusDerAdresse = window.location.hash.replace('#', '');
  const startKnopf = knoepfe.find(knopf => knopf.dataset.anker === ankerAusDerAdresse);
  if (startKnopf) {
    tabZeigen(startKnopf);
  }
})();

function teamBildSuchen(mitglied, mediaOrdner, wennGefunden) {
  const bild = document.createElement('img');
  bild.alt = mitglied.name;

  bild.addEventListener('load', () => wennGefunden(bild), { once: true });
  bild.addEventListener('error', () => {
    if (bild.dataset.stufe === 'png') {
      return;
    }
    bild.dataset.stufe = 'png';
    bild.src = `${mediaOrdner}${mitglied.id}.png`;
  });

  bild.src = `${mediaOrdner}${mitglied.id}.jpg`;
}

function farbeFuerRolle(rolle) {
  return ROLLEN_FARBE[rolle] || 'diamond';
}

function istBedrockSpieler(mitglied) {
  return mitglied.name.startsWith('.');
}

function teamKarteBauen(mitglied, mediaOrdner) {
  const karte = document.createElement('article');
  karte.className = `team-card ${farbeFuerRolle(mitglied.rolle)}`;

  const banner = document.createElement('div');
  banner.className = 'team-banner';
  banner.innerHTML = '<i class="fa-solid fa-user"></i>';
  teamBildSuchen(mitglied, mediaOrdner, bild => banner.replaceChildren(bild));
  karte.appendChild(banner);

  const info = document.createElement('div');
  info.className = 'team-info';

  const name = document.createElement('h3');
  name.className = 'team-name';
  name.textContent = mitglied.name;
  info.appendChild(name);

  const meta = document.createElement('div');
  meta.className = 'team-meta';

  const rolle = document.createElement('span');
  rolle.className = 'team-role';
  rolle.textContent = mitglied.rolle;
  meta.appendChild(rolle);

  const edition = document.createElement('span');
  edition.className = 'team-edition';
  if (istBedrockSpieler(mitglied)) {
    edition.innerHTML = '<i class="fa-solid fa-mobile-screen-button"></i>Bedrock';
  } else {
    edition.innerHTML = '<i class="fa-solid fa-desktop"></i>Java';
  }
  meta.appendChild(edition);

  info.appendChild(meta);
  karte.appendChild(info);
  return karte;
}

(function teamSeite() {
  const ziel = document.getElementById('teamGrid');
  if (!ziel) {
    return;
  }

  const mediaOrdner = ziel.dataset.media || 'media/';
  const owner = TEAM.filter(mitglied => mitglied.rolle === 'Owner');
  const restlichesTeam = TEAM.filter(mitglied => mitglied.rolle !== 'Owner');

  function gruppeBauen(titel, mitglieder) {
    if (mitglieder.length === 0) {
      return;
    }

    const gruppe = document.createElement('section');
    gruppe.className = 'team-gruppe';

    const kopf = document.createElement('div');
    kopf.className = 'team-gruppe-kopf';

    const ueberschrift = document.createElement('h2');
    ueberschrift.className = 'team-gruppe-titel';
    ueberschrift.textContent = titel;
    kopf.appendChild(ueberschrift);

    const anzahl = document.createElement('span');
    anzahl.className = 'team-gruppe-anzahl';
    anzahl.textContent = mitglieder.length;
    kopf.appendChild(anzahl);

    gruppe.appendChild(kopf);

    const raster = document.createElement('div');
    raster.className = 'team-grid';
    mitglieder.forEach(mitglied => raster.appendChild(teamKarteBauen(mitglied, mediaOrdner)));
    gruppe.appendChild(raster);

    ziel.appendChild(gruppe);
  }

  gruppeBauen('Owner', owner);
  gruppeBauen('Team', restlichesTeam);
})();

(function teamVorschau() {
  const ziel = document.getElementById('teamVorschau');
  if (!ziel) {
    return;
  }

  const mediaOrdner = ziel.dataset.media || 'team/media/';
  const teamSeite = ziel.dataset.link || 'team/';

  TEAM.forEach(mitglied => {
    const karte = document.createElement('a');
    karte.className = `team-mini ${farbeFuerRolle(mitglied.rolle)}`;
    karte.href = teamSeite;

    const bildRahmen = document.createElement('div');
    bildRahmen.className = 'team-mini-bild';
    bildRahmen.innerHTML = '<i class="fa-solid fa-user"></i>';
    teamBildSuchen(mitglied, mediaOrdner, bild => bildRahmen.replaceChildren(bild));
    karte.appendChild(bildRahmen);

    const name = document.createElement('span');
    name.className = 'team-mini-name';
    name.textContent = mitglied.name;
    karte.appendChild(name);

    const rolle = document.createElement('span');
    rolle.className = 'team-mini-rolle';
    rolle.textContent = mitglied.rolle;
    karte.appendChild(rolle);

    ziel.appendChild(karte);
  });
})();

let lightbox = null;

function lightboxHolen() {
  if (lightbox) {
    return lightbox;
  }

  lightbox = document.createElement('div');
  lightbox.className = 'lightbox';
  lightbox.innerHTML = `
    <div class="lightbox-inner">
      <button class="lightbox-close" aria-label="Schließen" type="button">&times;</button>
      <div class="lightbox-media"></div>
      <div class="lightbox-caption"></div>
    </div>
  `;
  document.body.appendChild(lightbox);

  lightbox.querySelector('.lightbox-close').addEventListener('click', lightboxSchliessen);
  lightbox.addEventListener('click', event => {
    if (event.target === lightbox) {
      lightboxSchliessen();
    }
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && lightbox.classList.contains('open')) {
      lightboxSchliessen();
    }
  });

  return lightbox;
}

function lightboxOeffnen(typ, src, titel) {
  const box = lightboxHolen();
  const medienFeld = box.querySelector('.lightbox-media');
  medienFeld.innerHTML = '';

  if (typ === 'video') {
    const videoEl = document.createElement('video');
    videoEl.src = src;
    videoEl.controls = true;
    videoEl.autoplay = true;
    videoEl.playsInline = true;
    medienFeld.appendChild(videoEl);
  } else {
    const imgEl = document.createElement('img');
    imgEl.src = src;
    imgEl.alt = titel;
    medienFeld.appendChild(imgEl);
  }

  box.querySelector('.lightbox-caption').textContent = titel;
  box.classList.add('open');
  document.body.style.overflow = 'hidden';
}

function lightboxSchliessen() {
  if (!lightbox) {
    return;
  }
  lightbox.classList.remove('open');
  lightbox.querySelector('.lightbox-media').innerHTML = '';
  document.body.style.overflow = '';
}

function rankClass(rank) {
  if (rank === 1) {
    return 'top1';
  }
  if (rank === 2) {
    return 'top2';
  }
  if (rank === 3) {
    return 'top3';
  }
  return '';
}

function momentKarteBauen(rank, meta, mediaOrdner, groesse) {
  const karte = document.createElement('div');
  karte.className = 'moment ohne-medien';
  if (groesse) {
    karte.classList.add(groesse);
  }
  karte.tabIndex = 0;
  karte.setAttribute('role', 'button');

  const titel = meta.title || `Platz ${rank}`;

  const thumb = document.createElement('div');
  thumb.className = 'moment-thumb';

  function medienGefunden(typ, src) {
    karte.dataset.mediaType = typ;
    karte.dataset.mediaSrc = src;
    karte.classList.remove('ohne-medien');
  }

  function leerAnzeigen() {
    const leer = document.createElement('div');
    leer.className = 'moment-leer';
    leer.innerHTML = '<i class="fa-solid fa-film"></i><span>Clip folgt</span>';
    thumb.prepend(leer);
  }

  const video = document.createElement('video');
  video.muted = true;
  video.loop = true;
  video.playsInline = true;
  video.preload = 'metadata';

  video.addEventListener('loadedmetadata', () => medienGefunden('video', video.src));
  video.addEventListener('error', () => {
    video.remove();

    const bild = document.createElement('img');
    bild.alt = titel;
    bild.addEventListener('load', () => medienGefunden('image', bild.src), { once: true });
    bild.addEventListener('error', () => {
      bild.remove();
      leerAnzeigen();
    }, { once: true });
    bild.src = `${mediaOrdner}${rank}.jpg`;
    thumb.prepend(bild);
  }, { once: true });

  video.src = `${mediaOrdner}${rank}.mp4`;
  thumb.appendChild(video);

  karte.addEventListener('mouseenter', () => {
    if (karte.dataset.mediaType === 'video') {
      video.play().catch(() => {});
    }
  });
  karte.addEventListener('mouseleave', () => {
    if (karte.dataset.mediaType === 'video') {
      video.pause();
      video.currentTime = 0;
    }
  });

  const rankBadge = document.createElement('div');
  rankBadge.className = `moment-rank ${rankClass(rank)}`;
  rankBadge.textContent = `#${rank}`;
  thumb.appendChild(rankBadge);

  const playOverlay = document.createElement('div');
  playOverlay.className = 'moment-play';
  playOverlay.innerHTML = '<i class="fa-solid fa-play"></i>';
  thumb.appendChild(playOverlay);

  karte.appendChild(thumb);

  const body = document.createElement('div');
  body.className = 'moment-body';

  const platz = document.createElement('span');
  platz.className = 'moment-platz';
  platz.textContent = `Platz ${rank}`;
  body.appendChild(platz);

  const titleEl = document.createElement('h3');
  titleEl.className = 'moment-title';
  titleEl.textContent = meta.title || 'Wird noch gekürt';
  body.appendChild(titleEl);

  if (meta.player) {
    const playerEl = document.createElement('div');
    playerEl.className = 'moment-player';
    playerEl.textContent = meta.player;
    body.appendChild(playerEl);
  }

  if (meta.description) {
    const descEl = document.createElement('p');
    descEl.className = 'moment-desc';
    descEl.textContent = meta.description;
    body.appendChild(descEl);
  }

  karte.appendChild(body);

  function oeffnen() {
    if (!karte.dataset.mediaType) {
      return;
    }
    lightboxOeffnen(karte.dataset.mediaType, karte.dataset.mediaSrc, titel);
  }

  karte.addEventListener('click', oeffnen);
  karte.addEventListener('keydown', event => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      oeffnen();
    }
  });

  return karte;
}

(function auraMoments() {
  const grid = document.getElementById('momentsGrid');
  if (!grid) {
    return;
  }

  const mediaOrdner = grid.dataset.media || 'media/';

  MOMENTS.forEach((meta, index) => {
    const rank = index + 1;

    if (rank === 4) {
      const trenner = document.createElement('div');
      trenner.className = 'moments-trenner';
      trenner.textContent = `Platz 4 – ${MOMENTS.length}`;
      grid.appendChild(trenner);
    }

    let groesse = '';
    if (rank === 1) {
      groesse = 'gross';
    } else if (rank <= 3) {
      groesse = 'podest';
    }

    grid.appendChild(momentKarteBauen(rank, meta, mediaOrdner, groesse));
  });
})();

(function auraVorschau() {
  const grid = document.getElementById('momentsVorschau');
  if (!grid) {
    return;
  }

  const mediaOrdner = grid.dataset.media || 'top/10/aura-moments/media/';
  const anzahl = Number(grid.dataset.anzahl) || 3;

  MOMENTS.slice(0, anzahl).forEach((meta, index) => {
    grid.appendChild(momentKarteBauen(index + 1, meta, mediaOrdner, ''));
  });
})();
