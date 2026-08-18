/** Das HTML-Geruest, das um jede Seite herumliegt. */
import { esc } from '../web.js';

export function seite({ titel, nutzer, hier = '', inhalt, breit = false, kopfExtra = '' }) {
  const nav = [];
  nav.push(['/', 'Pakete']);
  nav.push(['/konfigurator', 'Server bestellen']);
  nav.push(['/regeln', 'Regeln']);
  if (nutzer) {
    nav.push(['/meine-server', 'Meine Server']);
    if (nutzer.rolle === 'admin') nav.push(['/admin', 'Verwaltung']);
  }

  return `<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(titel)} · Lemon Hosting</title>
<link rel="stylesheet" href="/stil.css">
<link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'><text y='26' font-size='26'>🍋</text></svg>">
${kopfExtra}
</head>
<body>
<header class="kopf"><div class="kopf-innen">
  <a class="marke" href="/">🍋 <span class="zitrone">Lemon Hosting</span></a>
  <nav class="nav">
    ${nav.map(([pfad, text]) =>
      `<a href="${pfad}"${pfad === hier ? ' class="hier"' : ''}>${esc(text)}</a>`).join('')}
    ${nutzer
      ? `<a href="/abmelden" title="${esc(nutzer.benutzername)}">Abmelden</a>`
      : '<a href="/anmelden">Anmelden</a>'}
  </nav>
</div></header>
<main${breit ? '' : ''}>
${inhalt}
</main>
<footer class="fuss">
  Lemon Hosting · Minecraft Hosting für Klassen, Freunde und kleine Communities.
  <br>Einfach. Fair. Transparent.
</footer>
</body>
</html>`;
}

/** Ein Statuspunkt: aktiv, archiviert, gelöscht. */
export function statusPunkt(status) {
  const text = { aktiv: 'aktiv', archiviert: 'archiviert', geloescht: 'gelöscht',
                 offen: 'offen', angenommen: 'angenommen', abgelehnt: 'abgelehnt' };
  return `<span class="marke-punkt ${esc(status)}">${esc(text[status] || status)}</span>`;
}

export const csrfFeld = (zeichen) =>
  `<input type="hidden" name="csrf" value="${esc(zeichen)}">`;
