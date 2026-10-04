const fs = require('node:fs/promises');
const path = require('node:path');
const { WIKI_DATEN, WIKI_ORDNER } = require('./pfade');
const { jsonLesen, jsonSchreiben, wennGeaendertSchreiben, nacheinander } = require('./speicher');
const { Fehler } = require('./antwort');
const { text, attribut, vorsilbe, seitenKopf, aufruf, lesezeit, seite } = require('./vorlage');
const { symbolName, symbolBild } = require('./symbole');

const FARBEN = ['emerald', 'diamond', 'redstone', 'lapis', 'gold', 'amethyst'];
const EINRUECKUNG = '      ';
const SLUG_MUSTER = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const MAX_INHALT = 200000;

function slugAusText(wert) {
  return String(wert)
    .toLowerCase()
    .replace(/ä/g, 'ae')
    .replace(/ö/g, 'oe')
    .replace(/ü/g, 'ue')
    .replace(/ß/g, 'ss')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/&[a-z]+;|&#\d+;/g, ' ')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60)
    .replace(/-+$/, '');
}

function zeichenkette(wert) {
  return typeof wert === 'string' ? wert : '';
}

function einzeilig(wert) {
  return zeichenkette(wert).replace(/\s+/g, ' ').trim();
}

function dekodieren(html) {
  return html
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&');
}

function eingerueckt(inhalt) {
  return inhalt
    .split('\n')
    .map(zeile => (zeile === '' ? '' : EINRUECKUNG + zeile))
    .join('\n');
}

function ausgerueckt(inhalt) {
  const zeilen = zeichenkette(inhalt)
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map(zeile => zeile.replace(/\s+$/, ''));
  while (zeilen.length > 0 && zeilen[0] === '') {
    zeilen.shift();
  }
  while (zeilen.length > 0 && zeilen[zeilen.length - 1] === '') {
    zeilen.pop();
  }
  const einzuege = zeilen.filter(zeile => zeile !== '').map(zeile => zeile.match(/^ */)[0].length);
  const weg = einzuege.length > 0 ? Math.min(...einzuege) : 0;
  return zeilen.map(zeile => zeile.slice(weg)).join('\n');
}

function ueberschriftenMitId(html) {
  const vergeben = new Set();
  for (const treffer of html.matchAll(/\sid="([^"]*)"/g)) {
    vergeben.add(treffer[1]);
  }
  return html.replace(/<h2(\s[^>]*)?>([\s\S]*?)<\/h2>/gi, (ganz, attribute = '', innen) => {
    if (/\sid\s*=/i.test(attribute)) {
      return ganz;
    }
    const basis = slugAusText(innen.replace(/<[^>]+>/g, ' ')) || 'abschnitt';
    let id = basis;
    let nummer = 2;
    while (vergeben.has(id)) {
      id = `${basis}-${nummer}`;
      nummer += 1;
    }
    vergeben.add(id);
    return `<h2 id="${id}"${attribute}>${innen}</h2>`;
  });
}

function inhaltAufraeumen(roh) {
  return ueberschriftenMitId(ausgerueckt(roh));
}

function genutzteGruppen(daten) {
  return daten.gruppen.filter(gruppe => daten.artikel.some(artikel => artikel.kat === gruppe));
}

function artikelInGruppe(daten, gruppe) {
  return daten.artikel.filter(artikel => artikel.kat === gruppe);
}

function leseReihenfolge(daten) {
  return genutzteGruppen(daten).flatMap(gruppe => artikelInGruppe(daten, gruppe));
}

function wikiKarte(artikel, href) {
  const minuten = lesezeit(artikel.inhalt);
  return `        <a class="wiki-karte ${artikel.farbe} such-eintrag" href="${href}" data-stichworte="${attribut(artikel.stichworte)}">
          <div class="slot">${symbolBild(artikel.icon, '../')}</div>
          <p class="wiki-karte-titel">${text(artikel.titel)}</p>
          <p class="wiki-karte-text">${text(artikel.text)}</p>
          <p class="wiki-karte-meta"><i class="fa-regular fa-clock"></i>${minuten} Min. Lesezeit</p>
        </a>`;
}

function uebersichtSeite(daten) {
  const seitenPfad = 'wiki/';
  const suche = `
      <div class="suche">
        <i class="fa-solid fa-magnifying-glass"></i>
        <input type="search" placeholder="Artikel suchen, z. B. „Befehle“ oder „Reach“" aria-label="Wiki durchsuchen" data-such-bereich="wikiListe">
      </div>`;
  const gruppen = genutzteGruppen(daten).map(gruppe => {
    const artikel = artikelInGruppe(daten, gruppe);
    const karten = artikel.map(eintrag => wikiKarte(eintrag, `${eintrag.slug}/`)).join('\n');
    return `    <div class="gruppe such-gruppe">
      <div class="gruppe-kopf">
        <h2 class="gruppe-titel">${text(gruppe)}</h2>
        <span class="gruppe-anzahl">${artikel.length}</span>
      </div>
      <div class="wiki-karten">
${karten}
      </div>
    </div>`;
  }).join('\n');

  const inhalt = seitenKopf(seitenPfad, [['Wiki', 'wiki']], 'BuckSMP <span class="g">Wiki</span>', 'Anleitungen und Hintergründe rund um den Server – vom ersten Login bis zum Anti-Cheat.', '', suche) + `
<section class="sec such-bereich" id="wikiListe">
  <div class="wrap">
${gruppen}
    <div class="such-leer" hidden>
      <strong>Kein Artikel gefunden.</strong>
      Versuch es mit einem anderen Wort – oder schau ins <a href="../help/">Hilfe-Center</a>.
    </div>
  </div>
</section>
` + aufruf('Etwas fehlt?', 'Frag das <span class="g">Team</span>', 'Wenn du etwas nicht findest, hilft dir das Team gern weiter.', `      <a class="btn-p" href="../help/">
        <i class="fa-solid fa-circle-question"></i>
        Hilfe-Center
      </a>
      <a class="btn-o" href="../team/">
        <i class="fa-solid fa-users"></i>
        Zum Team
      </a>`);

  return seite(seitenPfad, 'wiki', 'Wiki – BuckSMP', 'Das Wiki von BuckSMP – Befehle, Duelle, Crossplay, Shop und wie unser Anti-Cheat funktioniert.', inhalt);
}

function seitenleiste(daten, aktuellerSlug) {
  return genutzteGruppen(daten).map(gruppe => {
    const links = artikelInGruppe(daten, gruppe).map(artikel => {
      const klasse = artikel.slug === aktuellerSlug ? ' class="aktiv"' : '';
      return `        <a href="../${artikel.slug}/"${klasse}>${text(artikel.kurz)}</a>`;
    }).join('\n');
    return `      <div class="wiki-nav-gruppe">
        <p class="wiki-nav-titel">${text(gruppe)}</p>
${links}
      </div>`;
  }).join('\n');
}

function artikelSeite(daten, reihenfolge, index) {
  const artikel = reihenfolge[index];
  const seitenPfad = `wiki/${artikel.slug}/`;
  const minuten = lesezeit(artikel.inhalt);
  const meta = `
      <div class="wiki-meta">
        <span class="wiki-kat ${artikel.farbe}">${text(artikel.kat)}</span>
        <span><i class="fa-regular fa-clock"></i>${minuten} Min. Lesezeit</span>
      </div>`;

  const vorher = index > 0 ? reihenfolge[index - 1] : null;
  const nachher = index + 1 < reihenfolge.length ? reihenfolge[index + 1] : null;
  const weiter = [];
  if (vorher) {
    weiter.push(`        <a class="vorheriger" href="../${vorher.slug}/"><span><i class="fa-solid fa-arrow-left"></i> Vorheriger Artikel</span><strong>${text(vorher.titel)}</strong></a>`);
  }
  if (nachher) {
    weiter.push(`        <a class="naechster" href="../${nachher.slug}/"><span>Nächster Artikel <i class="fa-solid fa-arrow-right"></i></span><strong>${text(nachher.titel)}</strong></a>`);
  }

  const inhalt = seitenKopf(seitenPfad, [['Wiki', 'wiki'], [text(artikel.titel), seitenPfad]], text(artikel.titel), text(artikel.text), '', meta) + `
<section class="sec">
  <div class="wrap wiki-layout">
    <article class="wiki-artikel text-inhalt">
${eingerueckt(artikel.inhalt)}

      <nav class="wiki-weiter" aria-label="Weitere Artikel">
${weiter.join('\n')}
      </nav>
    </article>
    <nav class="wiki-nav" aria-label="Alle Artikel">
${seitenleiste(daten, artikel.slug)}
    </nav>
    <aside class="wiki-toc">
      <p class="wiki-toc-titel">Auf dieser Seite</p>
      <nav aria-label="Inhaltsverzeichnis"></nav>
    </aside>
  </div>
</section>
`;

  return seite(seitenPfad, 'wiki', `${text(artikel.titel)} – Wiki – BuckSMP`, attribut(artikel.text), inhalt);
}

function weiterleitungSeite(daten, von, nach) {
  const seitenPfad = `wiki/${von}/`;
  const p = vorsilbe(seitenPfad);
  const zielArtikel = daten.artikel.find(artikel => artikel.slug === nach);
  const ziel = zielArtikel ? `../${zielArtikel.slug}/` : '../';
  const zielName = zielArtikel ? text(zielArtikel.titel) : 'Wiki';
  return `<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Weiterleitung – BuckSMP</title>
<meta http-equiv="refresh" content="0; url=${ziel}">
<link rel="canonical" href="${ziel}">
<link rel="icon" type="image/png" href="${p}assets/favicon.png">
<link rel="stylesheet" href="${p}assets/style.css">
</head>
<body>
<main class="weiterleitung">
  <p>Weiter zu <a href="${ziel}">${zielName}</a> …</p>
</main>
</body>
</html>
`;
}

function alleSeiten(daten) {
  const seiten = new Map();
  seiten.set('', uebersichtSeite(daten));
  const reihenfolge = leseReihenfolge(daten);
  reihenfolge.forEach((artikel, index) => {
    seiten.set(artikel.slug, artikelSeite(daten, reihenfolge, index));
  });
  for (const weiterleitung of daten.weiterleitungen) {
    seiten.set(weiterleitung.von, weiterleitungSeite(daten, weiterleitung.von, weiterleitung.nach));
  }
  return seiten;
}

function leer() {
  return { gruppen: [], artikel: [], weiterleitungen: [] };
}

function gruppenAufraeumen(daten) {
  for (const artikel of daten.artikel) {
    if (!daten.gruppen.includes(artikel.kat)) {
      daten.gruppen.push(artikel.kat);
    }
  }
  daten.gruppen = daten.gruppen.filter(gruppe => daten.artikel.some(artikel => artikel.kat === gruppe));
}

function weiterleitungenAufraeumen(daten) {
  const slugs = new Set(daten.artikel.map(artikel => artikel.slug));
  daten.weiterleitungen = daten.weiterleitungen
    .filter(weiterleitung => !slugs.has(weiterleitung.von))
    .map(weiterleitung => ({ von: weiterleitung.von, nach: slugs.has(weiterleitung.nach) ? weiterleitung.nach : '' }));
}

function datenBereinigen(roh) {
  const daten = leer();
  if (!roh || typeof roh !== 'object') {
    return daten;
  }
  const vergeben = new Set();

  for (const eintrag of Array.isArray(roh.artikel) ? roh.artikel : []) {
    const slug = zeichenkette(eintrag && eintrag.slug);
    if (!SLUG_MUSTER.test(slug) || vergeben.has(slug)) {
      continue;
    }
    vergeben.add(slug);
    const titel = zeichenkette(eintrag.titel) || slug;
    daten.artikel.push({
      slug,
      titel,
      kurz: zeichenkette(eintrag.kurz) || titel,
      kat: zeichenkette(eintrag.kat) || 'Allgemein',
      farbe: FARBEN.includes(eintrag.farbe) ? eintrag.farbe : 'diamond',
      icon: symbolName(zeichenkette(eintrag.icon)) || 'book',
      text: zeichenkette(eintrag.text),
      stichworte: zeichenkette(eintrag.stichworte),
      inhalt: zeichenkette(eintrag.inhalt),
    });
  }

  for (const gruppe of Array.isArray(roh.gruppen) ? roh.gruppen : []) {
    if (typeof gruppe === 'string' && gruppe !== '' && !daten.gruppen.includes(gruppe)) {
      daten.gruppen.push(gruppe);
    }
  }

  for (const eintrag of Array.isArray(roh.weiterleitungen) ? roh.weiterleitungen : []) {
    const von = zeichenkette(eintrag && eintrag.von);
    if (!SLUG_MUSTER.test(von) || vergeben.has(von)) {
      continue;
    }
    vergeben.add(von);
    daten.weiterleitungen.push({ von, nach: zeichenkette(eintrag.nach) });
  }

  gruppenAufraeumen(daten);
  weiterleitungenAufraeumen(daten);
  return daten;
}

async function ausSeitenLesen() {
  const daten = leer();
  let uebersicht;
  try {
    uebersicht = await fs.readFile(path.join(WIKI_ORDNER, 'index.html'), 'utf8');
  } catch {
    return daten;
  }

  let gruppe = 'Allgemein';
  const muster = /<h2 class="gruppe-titel">([^<]*)<\/h2>|<a class="wiki-karte ([a-z]+) such-eintrag" href="([a-z0-9-]+)\/" data-stichworte="([^"]*)">\s*<div class="slot">(?:<i class="fa-solid ([a-z0-9-]+)"><\/i>|<img src="\.\.\/assets\/mc\/([a-z0-9_]+)\.png"[^>]*>)<\/div>/g;
  for (const treffer of uebersicht.matchAll(muster)) {
    if (treffer[1] !== undefined) {
      gruppe = dekodieren(treffer[1]);
      continue;
    }
    const [, , farbe, slug, stichworte, altesSymbol, symbol] = treffer;
    let artikelSeiteHtml;
    try {
      artikelSeiteHtml = await fs.readFile(path.join(WIKI_ORDNER, slug, 'index.html'), 'utf8');
    } catch {
      continue;
    }
    const titel = /<h1 class="page-title">([\s\S]*?)<\/h1>/.exec(artikelSeiteHtml);
    const beschreibung = /<p class="page-sub">([\s\S]*?)<\/p>/.exec(artikelSeiteHtml);
    const kurz = new RegExp(`<a href="\\.\\./${slug}/" class="aktiv">([^<]*)</a>`).exec(artikelSeiteHtml);
    const inhalt = /<article class="wiki-artikel text-inhalt">\n([\s\S]*?)\n\n {6}<nav class="wiki-weiter"/.exec(artikelSeiteHtml);
    if (!titel || !inhalt) {
      continue;
    }
    daten.artikel.push({
      slug,
      titel: dekodieren(titel[1]),
      kurz: dekodieren(kurz ? kurz[1] : titel[1]),
      kat: gruppe,
      farbe,
      icon: symbolName(symbol || altesSymbol) || 'book',
      text: dekodieren(beschreibung ? beschreibung[1] : ''),
      stichworte: dekodieren(stichworte),
      inhalt: ausgerueckt(inhalt[1]),
    });
  }

  let eintraege = [];
  try {
    eintraege = await fs.readdir(WIKI_ORDNER, { withFileTypes: true });
  } catch {
    eintraege = [];
  }
  for (const eintrag of eintraege) {
    if (!eintrag.isDirectory() || !SLUG_MUSTER.test(eintrag.name)) {
      continue;
    }
    const html = await fs.readFile(path.join(WIKI_ORDNER, eintrag.name, 'index.html'), 'utf8').catch(() => '');
    const ziel = /<meta http-equiv="refresh" content="0; url=\.\.\/([a-z0-9-]*)\/?">/.exec(html);
    if (ziel) {
      daten.weiterleitungen.push({ von: eintrag.name, nach: ziel[1] });
    }
  }

  return datenBereinigen(daten);
}

async function wikiLesen() {
  let roh;
  try {
    roh = await jsonLesen(WIKI_DATEN, null);
  } catch {
    throw new Fehler(500, 'Die Datei daten/wiki.json ist beschädigt und konnte nicht gelesen werden.');
  }
  if (roh === null) {
    return ausSeitenLesen();
  }
  return datenBereinigen(roh);
}

function kopie(daten) {
  return JSON.parse(JSON.stringify(daten));
}

function ordnerVon(daten) {
  return [...daten.artikel.map(artikel => artikel.slug), ...daten.weiterleitungen.map(weiterleitung => weiterleitung.von)];
}

async function ordnerEntfernen(ordner) {
  if (!SLUG_MUSTER.test(ordner)) {
    return;
  }
  const verzeichnis = path.join(WIKI_ORDNER, ordner);
  await fs.rm(path.join(verzeichnis, 'index.html'), { force: true });
  try {
    await fs.rmdir(verzeichnis);
  } catch (fehler) {
    if (!['ENOENT', 'ENOTEMPTY', 'EEXIST'].includes(fehler.code)) {
      throw fehler;
    }
  }
}

async function seitenSchreiben(daten, vorher) {
  const seiten = alleSeiten(daten);
  let geschrieben = 0;
  for (const [ordner, html] of seiten) {
    if (await wennGeaendertSchreiben(path.join(WIKI_ORDNER, ordner, 'index.html'), html)) {
      geschrieben += 1;
    }
  }
  for (const ordner of vorher ? ordnerVon(vorher) : []) {
    if (!seiten.has(ordner)) {
      await ordnerEntfernen(ordner);
    }
  }
  return geschrieben;
}

function aendern(aenderung) {
  return nacheinander(async () => {
    const daten = await wikiLesen();
    const vorher = kopie(daten);
    const ergebnis = await aenderung(daten);
    gruppenAufraeumen(daten);
    weiterleitungenAufraeumen(daten);
    await jsonSchreiben(WIKI_DATEN, daten);
    await seitenSchreiben(daten, vorher);
    return { daten, ergebnis };
  });
}

function artikelAusEingabe(eingabe) {
  const quelle = eingabe && typeof eingabe === 'object' ? eingabe : {};
  const titel = einzeilig(quelle.titel);
  return {
    slug: einzeilig(quelle.slug).toLowerCase() || slugAusText(titel),
    titel,
    kurz: einzeilig(quelle.kurz) || titel,
    kat: einzeilig(quelle.kat),
    farbe: einzeilig(quelle.farbe),
    icon: symbolName(einzeilig(quelle.icon)),
    text: einzeilig(quelle.text),
    stichworte: einzeilig(quelle.stichworte),
    inhalt: inhaltAufraeumen(quelle.inhalt),
  };
}

function artikelFehler(artikel, daten, alterSlug) {
  if (artikel.titel === '') {
    return 'Der Artikel braucht einen Titel.';
  }
  if (artikel.titel.length > 80) {
    return 'Der Titel ist zu lang – höchstens 80 Zeichen.';
  }
  if (artikel.kurz.length > 40) {
    return 'Der Name in der Seitenleiste ist zu lang – höchstens 40 Zeichen.';
  }
  if (!SLUG_MUSTER.test(artikel.slug) || artikel.slug.length > 60) {
    return 'Die Adresse darf nur kleine Buchstaben ohne Umlaute, Zahlen und einzelne Bindestriche enthalten.';
  }
  if (artikel.slug !== alterSlug && daten.artikel.some(eintrag => eintrag.slug === artikel.slug)) {
    return `Die Adresse „${artikel.slug}“ gehört schon zu einem anderen Artikel.`;
  }
  if (artikel.kat === '') {
    return 'Bitte gib eine Kategorie an.';
  }
  if (artikel.kat.length > 40) {
    return 'Der Name der Kategorie ist zu lang – höchstens 40 Zeichen.';
  }
  if (!FARBEN.includes(artikel.farbe)) {
    return 'Bitte wähle eine Farbe aus.';
  }
  if (!artikel.icon) {
    return 'Bitte wähle ein Symbol aus der Liste aus.';
  }
  if (artikel.text === '') {
    return 'Bitte schreib eine kurze Beschreibung.';
  }
  if (artikel.text.length > 200) {
    return 'Die Beschreibung ist zu lang – höchstens 200 Zeichen.';
  }
  if (artikel.stichworte.length > 300) {
    return 'Die Stichworte sind zu lang – höchstens 300 Zeichen.';
  }
  if (artikel.inhalt === '') {
    return 'Der Artikel hat noch keinen Inhalt.';
  }
  if (artikel.inhalt.length > MAX_INHALT) {
    return 'Der Inhalt ist zu lang.';
  }
  return null;
}

function artikelSpeichern(eingabe, alterSlug) {
  return aendern(daten => {
    const index = alterSlug ? daten.artikel.findIndex(eintrag => eintrag.slug === alterSlug) : -1;
    if (alterSlug && index === -1) {
      throw new Fehler(404, 'Diesen Artikel gibt es nicht mehr. Lade die Seite neu.');
    }
    const artikel = artikelAusEingabe(eingabe);
    const fehler = artikelFehler(artikel, daten, alterSlug || null);
    if (fehler) {
      throw new Fehler(400, fehler);
    }

    if (index === -1) {
      daten.artikel.push(artikel);
    } else {
      daten.artikel[index] = artikel;
    }

    daten.weiterleitungen = daten.weiterleitungen.filter(weiterleitung => weiterleitung.von !== artikel.slug);
    if (alterSlug && alterSlug !== artikel.slug) {
      for (const weiterleitung of daten.weiterleitungen) {
        if (weiterleitung.nach === alterSlug) {
          weiterleitung.nach = artikel.slug;
        }
      }
      daten.weiterleitungen.push({ von: alterSlug, nach: artikel.slug });
    }
    return artikel;
  });
}

function artikelLoeschen(slug, nach) {
  return aendern(daten => {
    const artikel = daten.artikel.find(eintrag => eintrag.slug === slug);
    if (!artikel) {
      throw new Fehler(404, 'Diesen Artikel gibt es nicht mehr.');
    }
    if (nach !== null && nach !== '' && (nach === slug || !daten.artikel.some(eintrag => eintrag.slug === nach))) {
      throw new Fehler(400, 'Das Ziel der Weiterleitung gibt es nicht.');
    }
    daten.artikel = daten.artikel.filter(eintrag => eintrag !== artikel);
    if (nach !== null) {
      for (const weiterleitung of daten.weiterleitungen) {
        if (weiterleitung.nach === slug) {
          weiterleitung.nach = nach;
        }
      }
      daten.weiterleitungen.push({ von: slug, nach });
    }
    return artikel;
  });
}

function artikelVerschieben(slug, richtung) {
  return aendern(daten => {
    const artikel = daten.artikel.find(eintrag => eintrag.slug === slug);
    if (!artikel) {
      throw new Fehler(404, 'Diesen Artikel gibt es nicht mehr.');
    }
    const nachbarn = artikelInGruppe(daten, artikel.kat);
    const ziel = nachbarn[nachbarn.indexOf(artikel) + richtung];
    if (!ziel) {
      return;
    }
    const a = daten.artikel.indexOf(artikel);
    const b = daten.artikel.indexOf(ziel);
    [daten.artikel[a], daten.artikel[b]] = [daten.artikel[b], daten.artikel[a]];
  });
}

function gruppeVerschieben(name, richtung) {
  return aendern(daten => {
    const position = daten.gruppen.indexOf(name);
    if (position === -1) {
      throw new Fehler(404, 'Diese Kategorie gibt es nicht mehr.');
    }
    const ziel = position + richtung;
    if (ziel < 0 || ziel >= daten.gruppen.length) {
      return;
    }
    [daten.gruppen[position], daten.gruppen[ziel]] = [daten.gruppen[ziel], daten.gruppen[position]];
  });
}

function gruppeUmbenennen(alt, neu) {
  const name = einzeilig(neu);
  if (name === '' || name.length > 40) {
    throw new Fehler(400, 'Der Name der Kategorie muss 1 bis 40 Zeichen lang sein.');
  }
  return aendern(daten => {
    if (!daten.gruppen.includes(alt)) {
      throw new Fehler(404, 'Diese Kategorie gibt es nicht mehr.');
    }
    if (name !== alt && daten.gruppen.includes(name)) {
      throw new Fehler(409, `Die Kategorie „${name}“ gibt es schon.`);
    }
    daten.gruppen = daten.gruppen.map(gruppe => (gruppe === alt ? name : gruppe));
    for (const artikel of daten.artikel) {
      if (artikel.kat === alt) {
        artikel.kat = name;
      }
    }
  });
}

function weiterleitungSpeichern(von, nach) {
  const quelle = einzeilig(von).toLowerCase().replace(/^\/?(wiki\/)?|\/$/g, '');
  if (!SLUG_MUSTER.test(quelle) || quelle.length > 60) {
    throw new Fehler(400, 'Die alte Adresse darf nur kleine Buchstaben ohne Umlaute, Zahlen und einzelne Bindestriche enthalten.');
  }
  return aendern(daten => {
    if (daten.artikel.some(artikel => artikel.slug === quelle)) {
      throw new Fehler(409, `Unter /wiki/${quelle}/ steht schon ein Artikel.`);
    }
    if (nach !== '' && !daten.artikel.some(artikel => artikel.slug === nach)) {
      throw new Fehler(400, 'Das Ziel der Weiterleitung gibt es nicht.');
    }
    daten.weiterleitungen = daten.weiterleitungen.filter(weiterleitung => weiterleitung.von !== quelle);
    daten.weiterleitungen.push({ von: quelle, nach });
    return quelle;
  });
}

function weiterleitungLoeschen(von) {
  return aendern(daten => {
    const vorher = daten.weiterleitungen.length;
    daten.weiterleitungen = daten.weiterleitungen.filter(weiterleitung => weiterleitung.von !== von);
    if (daten.weiterleitungen.length === vorher) {
      throw new Fehler(404, 'Diese Weiterleitung gibt es nicht mehr.');
    }
  });
}

function artikelFuerVorschau(artikel) {
  const titel = artikel.titel || 'Neuer Artikel';
  return {
    ...artikel,
    slug: SLUG_MUSTER.test(artikel.slug) ? artikel.slug : 'vorschau',
    titel,
    kurz: artikel.kurz || titel,
    kat: artikel.kat || 'Allgemein',
    farbe: FARBEN.includes(artikel.farbe) ? artikel.farbe : 'diamond',
    icon: artikel.icon || 'book',
  };
}

async function vorschau(eingabe, alterSlug) {
  const daten = await wikiLesen();
  const artikel = artikelFuerVorschau(artikelAusEingabe(eingabe));
  const liste = daten.artikel
    .map(eintrag => (alterSlug && eintrag.slug === alterSlug ? artikel : eintrag))
    .filter(eintrag => eintrag === artikel || eintrag.slug !== artikel.slug);
  if (!liste.includes(artikel)) {
    liste.push(artikel);
  }
  daten.artikel = liste;
  gruppenAufraeumen(daten);
  const reihenfolge = leseReihenfolge(daten);
  return artikelSeite(daten, reihenfolge, reihenfolge.indexOf(artikel));
}

function startAbgleich() {
  return nacheinander(async () => {
    let roh;
    try {
      roh = await jsonLesen(WIKI_DATEN, null);
    } catch {
      return { fehler: 'Die Datei daten/wiki.json ist beschädigt – die Wiki-Seiten wurden nicht neu erzeugt.' };
    }
    const daten = roh === null ? await ausSeitenLesen() : datenBereinigen(roh);
    if (roh === null && daten.artikel.length === 0) {
      return { fehler: 'Es wurden keine Wiki-Artikel gefunden – daten/wiki.json wurde nicht angelegt.' };
    }
    await wennGeaendertSchreiben(WIKI_DATEN, `${JSON.stringify(daten, null, 2)}\n`);
    return { neu: roh === null, geaendert: await seitenSchreiben(daten, null) };
  });
}

function neuErzeugen() {
  return nacheinander(async () => {
    const daten = await wikiLesen();
    await jsonSchreiben(WIKI_DATEN, daten);
    return seitenSchreiben(daten, null);
  });
}

module.exports = {
  FARBEN,
  wikiLesen,
  ausSeitenLesen,
  alleSeiten,
  artikelSpeichern,
  artikelLoeschen,
  artikelVerschieben,
  gruppeVerschieben,
  gruppeUmbenennen,
  weiterleitungSpeichern,
  weiterleitungLoeschen,
  vorschau,
  neuErzeugen,
  startAbgleich,
};

if (require.main === module) {
  neuErzeugen().then(anzahl => {
    console.log(`Wiki neu erzeugt – ${anzahl} Seite(n) geändert.`);
  });
}
