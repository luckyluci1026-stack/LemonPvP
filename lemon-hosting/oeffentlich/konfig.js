/**
 * Live-Preis im Konfigurator.
 *
 * Wichtig: Das hier ist nur die Anzeige. Verbindlich gerechnet wird beim
 * Absenden noch einmal auf dem Server mit derselben Preisliste - ein
 * Browser kann man manipulieren, den Server nicht so leicht. Deshalb
 * kommen die Preise auch aus /preise.json und stehen nicht doppelt im
 * Code.
 */
(async () => {
  const preise = await (await fetch('/preise.json')).json();
  const form = document.getElementById('konfig');
  if (!form) return;

  const euro = (n) => `${Number(n).toFixed(2).replace('.', ',')} €`;

  function rechne() {
    const paketId = form.querySelector('input[name=paket]:checked')?.value;
    const paket = preise.pakete[paketId];
    if (!paket) return;

    const zeilen = [`<div class="summe-zeile"><span>Paket ${paket.name}</span>
                     <span>${euro(paket.preis)}</span></div>`];
    let proMonat = paket.preis, einmalig = 0;
    const aus = { cores: paket.cores, ram: paket.ram, ssd: paket.ssd };

    for (const feld of form.querySelectorAll('[data-id]')) {
      const z = preise.zusatz[feld.dataset.id];
      if (!z) continue;
      const menge = feld.type === 'checkbox' ? (feld.checked ? 1 : 0)
                                             : Math.max(0, parseInt(feld.value, 10) || 0);
      if (menge <= 0) continue;

      // Subdomain und FLFAC sind bei Coal und Diamond schon dabei
      const imPaket = (z.id === 'subdomain' && paket.subdomainInklusive)
                   || (z.id === 'flfac' && paket.flfacInklusive);
      const summe = imPaket ? 0 : Math.round(z.preis * menge * 100) / 100;

      if (z.art === 'einmalig') einmalig += summe;
      else if (z.art === 'proWoche') proMonat += summe * 4;
      else proMonat += summe;

      for (const [f, w] of Object.entries(z.wirkung || {})) {
        aus[f] = Math.round((aus[f] + w * menge) * 100) / 100;
      }

      zeilen.push(`<div class="summe-zeile">
        <span>${menge > 1 ? menge + '× ' : ''}${z.name}${
          imPaket ? ' <span class="klein leise">(im Paket)</span>' : ''}</span>
        <span>${summe === 0 ? '<span class="leise">0,00 €</span>' : euro(summe)}${
          z.art === 'proWoche' ? ' <span class="klein leise">/Wo</span>' : ''}</span></div>`);
    }

    document.getElementById('posten').innerHTML = zeilen.join('');
    document.getElementById('proMonat').textContent = euro(Math.round(proMonat * 100) / 100);
    const einmalZeile = document.getElementById('einmaligZeile');
    einmalZeile.style.display = einmalig > 0 ? '' : 'none';
    document.getElementById('einmalig').textContent = euro(einmalig);
    document.getElementById('ausstattung').textContent =
      `Danach: ${aus.cores} Cores · ${aus.ram} GB RAM · ${aus.ssd} GB SSD`;

    for (const karte of form.querySelectorAll('#pakete .paket')) {
      karte.classList.toggle('gewaehlt',
        karte.querySelector('input').value === paketId);
    }
  }

  form.addEventListener('input', rechne);
  form.addEventListener('change', rechne);
  rechne();
})();
