function toggleNav(btn) {
  const menu = document.getElementById('mobMenu');
  if (!menu) {
    return;
  }
  const isOpen = menu.classList.toggle('open');
  const bars = btn.querySelectorAll('span');
  if (isOpen) {
    bars[0].style.transform = 'rotate(45deg) translate(5px,5px)';
    bars[1].style.opacity = '0';
    bars[2].style.transform = 'rotate(-45deg) translate(5px,-5px)';
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

const SERVER_IP = 'bucksmp.de';
let toastTimer;

function copyIP() {
  navigator.clipboard.writeText(SERVER_IP).then(() => {
    const feed = document.getElementById('ipFeed');
    const icon = document.getElementById('copyIco');
    const label = document.getElementById('copyTxt');

    if (feed) {
      feed.classList.add('show');
    }
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
      if (feed) {
        feed.classList.remove('show');
      }
      if (label) {
        label.textContent = 'Kopieren';
      }
      if (icon) {
        icon.className = 'fa-regular fa-copy';
      }
    }, 2500);
  });
}

(function liveServerStatus() {
  const countEl = document.getElementById('liveCount');
  const statNEl = document.getElementById('statN');
  const statusEl = document.getElementById('liveStatus');
  const dotEl = document.getElementById('liveDot');

  if (!countEl && !statNEl) {
    return;
  }

  const SERVER_HOST = 'bucksmp.de';
  const REFRESH_MS = 60000;

  async function refresh() {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    try {
      const response = await fetch(`https://api.mcsrvstat.us/3/${SERVER_HOST}`, { signal: controller.signal });
      if (!response.ok) {
        return;
      }
      const data = await response.json();
      const isOnline = !!data.online;
      const playerCount = isOnline && data.players && typeof data.players.online === 'number'
        ? String(data.players.online)
        : (isOnline ? '0' : '–');

      if (statusEl) {
        statusEl.textContent = isOnline ? 'online' : 'offline';
      }
      if (countEl) {
        countEl.textContent = playerCount;
      }
      if (statNEl) {
        statNEl.textContent = playerCount;
      }
      if (dotEl) {
        dotEl.style.animationPlayState = isOnline ? 'running' : 'paused';
        dotEl.style.background = isOnline ? '' : 'var(--muted)';
      }
    } catch (error) {
      return;
    } finally {
      clearTimeout(timeout);
    }
  }

  refresh();
  setInterval(refresh, REFRESH_MS);
})();

(function auraMoments() {
  const grid = document.getElementById('momentsGrid');
  if (!grid) {
    return;
  }

  const TOTAL_PLAETZE = 10;
  const MEDIA_ORDNER = 'media/';
  const FALLBACK_THUMB = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 180'%3E%3Crect width='320' height='180' fill='%23161616'/%3E%3Ctext x='160' y='96' font-family='sans-serif' font-size='13' fill='%23555' text-anchor='middle'%3ENoch kein Clip hochgeladen%3C/text%3E%3C/svg%3E";

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

  function rankClass(index) {
    if (index === 0) {
      return 'top1';
    }
    if (index === 1) {
      return 'top2';
    }
    if (index === 2) {
      return 'top3';
    }
    return '';
  }

  function karteBauen(rank, meta) {
    const wrap = document.createElement('div');
    wrap.className = 'moment';
    wrap.tabIndex = 0;
    wrap.setAttribute('role', 'button');

    const thumb = document.createElement('div');
    thumb.className = 'moment-thumb';

    const video = document.createElement('video');
    video.muted = true;
    video.loop = true;
    video.playsInline = true;
    video.preload = 'metadata';

    video.addEventListener('loadedmetadata', () => {
      wrap.dataset.mediaType = 'video';
      wrap.dataset.mediaSrc = video.src;
    });
    video.addEventListener('mouseenter', () => video.play().catch(() => {}));
    video.addEventListener('mouseleave', () => {
      video.pause();
      video.currentTime = 0;
    });
    video.addEventListener('error', () => {
      video.remove();

      const img = document.createElement('img');
      img.alt = meta.title || `Platz ${rank}`;

      img.addEventListener('load', () => {
        if (img.src === FALLBACK_THUMB) {
          return;
        }
        wrap.dataset.mediaType = 'image';
        wrap.dataset.mediaSrc = img.src;
      }, { once: true });

      img.addEventListener('error', () => {
        img.onerror = null;
        img.src = FALLBACK_THUMB;
      }, { once: true });

      img.src = `${MEDIA_ORDNER}${rank}.jpg`;
      thumb.prepend(img);
    }, { once: true });

    video.src = `${MEDIA_ORDNER}${rank}.mp4`;
    thumb.appendChild(video);

    const rankBadge = document.createElement('div');
    rankBadge.className = `moment-rank ${rankClass(rank - 1)}`;
    rankBadge.textContent = `#${rank}`;
    thumb.appendChild(rankBadge);

    const playOverlay = document.createElement('div');
    playOverlay.className = 'moment-play';
    playOverlay.innerHTML = '<i class="fa-solid fa-play"></i>';
    thumb.appendChild(playOverlay);

    wrap.appendChild(thumb);

    const body = document.createElement('div');
    body.className = 'moment-body';

    const titel = meta.title || `Platz ${rank}`;
    const titleEl = document.createElement('div');
    titleEl.className = 'moment-title';
    titleEl.textContent = titel;
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

    wrap.appendChild(body);

    function oeffnen() {
      if (!wrap.dataset.mediaType) {
        return;
      }
      lightboxOeffnen(wrap.dataset.mediaType, wrap.dataset.mediaSrc, titel);
    }

    wrap.addEventListener('click', oeffnen);
    wrap.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        oeffnen();
      }
    });

    return wrap;
  }

  for (let rank = 1; rank <= TOTAL_PLAETZE; rank++) {
    grid.appendChild(karteBauen(rank, MOMENTS[rank - 1] || {}));
  }

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

  const lightboxMedia = lightbox.querySelector('.lightbox-media');
  const lightboxCaption = lightbox.querySelector('.lightbox-caption');

  function lightboxOeffnen(typ, src, titel) {
    lightboxMedia.innerHTML = '';
    if (typ === 'video') {
      const videoEl = document.createElement('video');
      videoEl.src = src;
      videoEl.controls = true;
      videoEl.autoplay = true;
      videoEl.playsInline = true;
      lightboxMedia.appendChild(videoEl);
    } else {
      const imgEl = document.createElement('img');
      imgEl.src = src;
      imgEl.alt = titel;
      lightboxMedia.appendChild(imgEl);
    }
    lightboxCaption.textContent = titel;
    lightbox.classList.add('open');
    document.body.style.overflow = 'hidden';
  }

  function lightboxSchliessen() {
    lightbox.classList.remove('open');
    lightboxMedia.innerHTML = '';
    document.body.style.overflow = '';
  }

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
})();

(function teamGrid() {
  const grid = document.getElementById('teamGrid');
  if (!grid) {
    return;
  }

  const MEDIA_ORDNER = 'media/';

  const TEAM = [
    { id: 'lemon', name: 'Lemonightt', rolle: 'Owner' },
    { id: 'pneu', name: '.Pneuer13', rolle: 'Owner' },
    { id: 'owner', name: 'Catze99', rolle: 'Owner' },
    { id: 'manager', name: 'Noah', rolle: 'Manager' },
    { id: 'admin', name: 'Nani0000', rolle: 'Admin' },
    { id: 'mod', name: '.Colixiander', rolle: 'Moderator' },
    { id: 'sup', name: '.erbse', rolle: 'Supporter' },
  ];

  function karteBauen(mitglied) {
    const card = document.createElement('div');
    card.className = 'team-card';

    const avatar = document.createElement('div');
    avatar.className = 'team-avatar';
    avatar.innerHTML = '<i class="fa-solid fa-user"></i>';

    const img = document.createElement('img');
    img.alt = mitglied.name;

    img.addEventListener('load', () => avatar.replaceChildren(img), { once: true });
    img.addEventListener('error', () => {
      if (img.dataset.stufe === 'png') {
        return;
      }
      img.dataset.stufe = 'png';
      img.src = `${MEDIA_ORDNER}${mitglied.id}.png`;
    });

    img.src = `${MEDIA_ORDNER}${mitglied.id}.jpg`;
    card.appendChild(avatar);

    const nameEl = document.createElement('div');
    nameEl.className = 'team-name';
    nameEl.textContent = mitglied.name;
    card.appendChild(nameEl);

    const rolleEl = document.createElement('div');
    rolleEl.className = 'team-role';
    rolleEl.textContent = mitglied.rolle;
    card.appendChild(rolleEl);

    return card;
  }

  TEAM.forEach(mitglied => grid.appendChild(karteBauen(mitglied)));
})();
