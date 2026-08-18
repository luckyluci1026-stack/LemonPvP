/**
 * Die Preisliste - und zwar an genau einer Stelle.
 *
 * Alles, was im Portal einen Preis hat, steht hier: die drei Pakete, die
 * Zusatzleistungen, die Einmalkosten. Der Konfigurator rechnet damit, die
 * Serververwaltung rechnet damit, und der gedruckte Bestellbogen nimmt
 * dieselben Zahlen. Wenn du einen Preis aenderst, aenderst du ihn hier -
 * und nirgendwo sonst.
 *
 * Grundlage: die Preisliste und der Zusatzleistungs-Zettel von Lemon
 * Hosting. Betraege in Euro pro Monat, sofern nicht anders vermerkt.
 */

/** Die drei Serverpakete. */
export const PAKETE = {
  wood: {
    id: 'wood',
    name: 'Wood',
    zeichen: '🪵',
    preis: 3.99,
    cores: 0.75,
    ram: 2.5,
    ssd: 7.5,
    spieler: '5–10',
    // Bei Wood kosten Subdomain und Anti-Cheat extra - das ist der
    // einzige Unterschied in der Abrechnung zwischen den Paketen.
    subdomainInklusive: false,
    flfacInklusive: false,
    beschreibung: 'Der Einstieg. Reicht für eine Klasse, die zusammen baut.',
  },
  coal: {
    id: 'coal',
    name: 'Coal',
    zeichen: '⛏️',
    preis: 5.99,
    cores: 1.25,
    ram: 3.25,
    ssd: 10,
    spieler: '8–18',
    subdomainInklusive: true,
    flfacInklusive: true,
    beschreibung: 'Für Server, auf denen regelmäßig etwas los ist.',
  },
  diamond: {
    id: 'diamond',
    name: 'Diamond',
    zeichen: '💎',
    preis: 8.99,
    cores: 1.5,
    ram: 4.25,
    ssd: 15,
    spieler: '10–25',
    subdomainInklusive: true,
    flfacInklusive: true,
    beschreibung: 'Wenn mehrere Klassen oder eine kleine Community draufspielen.',
  },
};

/**
 * Zusatzleistungen.
 *
 * "proMonat" laeuft mit der Serverlaufzeit mit, "einmalig" faellt einmal
 * an. "menge: true" heisst, man kann mehrere davon buchen.
 */
export const ZUSATZ = {
  ram1: { id: 'ram1', name: '+1 GB RAM', preis: 1.00, art: 'proMonat', menge: true,
          gruppe: 'Hardware', wirkung: { ram: 1 } },
  ram2: { id: 'ram2', name: '+2 GB RAM', preis: 2.00, art: 'proMonat', menge: true,
          gruppe: 'Hardware', wirkung: { ram: 2 } },
  core1: { id: 'core1', name: '+1 CPU-Core', preis: 3.00, art: 'proMonat', menge: true,
           gruppe: 'Hardware', wirkung: { cores: 1 } },
  ssd10: { id: 'ssd10', name: '+10 GB NVMe SSD', preis: 1.00, art: 'proMonat', menge: true,
           gruppe: 'Hardware', wirkung: { ssd: 10 } },

  subdomain: { id: 'subdomain', name: 'Subdomain', preis: 0.25, art: 'proMonat', menge: false,
               gruppe: 'Netzwerk',
               hinweis: 'Bei Coal und Diamond schon im Paket enthalten.' },
  subdomainExtra: { id: 'subdomainExtra', name: 'Weitere Subdomain', preis: 0.25,
                    art: 'proMonat', menge: true, gruppe: 'Netzwerk' },
  eigeneIp: { id: 'eigeneIp', name: 'Eigene IP', preis: 1.00, art: 'proMonat', menge: false,
              gruppe: 'Netzwerk' },

  flfac: { id: 'flfac', name: 'FLFAC Anti-Cheat', preis: 0.30, art: 'proMonat', menge: false,
           gruppe: 'Weitere Leistungen',
           hinweis: 'Bei Coal und Diamond auf Anfrage kostenlos.' },
  devServer: { id: 'devServer', name: 'Developer Server', preis: 0.50, art: 'proWoche',
               menge: false, gruppe: 'Weitere Leistungen',
               hinweis: '0,50–0,75 € pro Woche, je nach Umfang.' },
  reset: { id: 'reset', name: 'Server Reset', preis: 0.50, art: 'einmalig', menge: true,
           gruppe: 'Weitere Leistungen' },
  plugin: { id: 'plugin', name: 'Eigenes Plugin', preis: 5.00, art: 'einmalig', menge: false,
            gruppe: 'Weitere Leistungen',
            hinweis: '5–25 € je nach Umfang. Der Preis wird vor Beginn genannt.' },
};

/** Eigene Domains - Jahrespreise, nur auf Anfrage. */
export const DOMAINS = [
  { endung: '.de', abPreis: 10 },
  { endung: '.com', abPreis: 30 },
  { endung: '.net', abPreis: 35 },
  { endung: '.dev', abPreis: 45 },
];

export const SOFTWARE = ['Vanilla', 'Paper', 'Purpur', 'Spigot'];

/** Wie lange ein unbezahlter Server archiviert bleibt, bevor er weg ist. */
export const ARCHIV_TAGE = 7;

export const HARDWARE = {
  cpu: 'AMD Ryzen 9 9950X @ bis zu 5,7 GHz',
  ram: 'DDR5 RAM 5600 MHz',
  ssd: 'NVMe SSD',
  uplink: '10 Gbit/s',
};

/**
 * Rechnet eine Bestellung durch.
 *
 * Der Knackpunkt sind die Leistungen, die je nach Paket schon drin sind:
 * Eine Subdomain kostet bei Wood 0,25 €, bei Coal und Diamond nichts.
 * Dasselbe bei FLFAC. Wer das von Hand rechnet, verrechnet sich - deshalb
 * macht es hier eine Funktion, und alle benutzen sie.
 *
 * @param paketId  'wood' | 'coal' | 'diamond'
 * @param gewaehlt { zusatzId: menge }  - Menge 0 oder fehlend = nicht gebucht
 * @returns { posten[], proMonat, einmalig, ausstattung }
 */
export function rechne(paketId, gewaehlt = {}) {
  const paket = PAKETE[paketId];
  if (!paket) {
    return { posten: [], proMonat: 0, einmalig: 0, ausstattung: null, fehler: 'Unbekanntes Paket' };
  }

  const posten = [];
  let proMonat = paket.preis;
  let einmalig = 0;
  const ausstattung = { cores: paket.cores, ram: paket.ram, ssd: paket.ssd };

  posten.push({
    art: 'paket', name: `Paket ${paket.name}`, menge: 1,
    einzel: paket.preis, summe: paket.preis, takt: 'Monat',
  });

  for (const [id, rohMenge] of Object.entries(gewaehlt)) {
    const zusatz = ZUSATZ[id];
    const menge = zusatz && zusatz.menge ? Math.max(0, Math.floor(Number(rohMenge) || 0))
                                         : (rohMenge ? 1 : 0);
    if (!zusatz || menge <= 0) continue;

    // Schon im Paket drin? Dann steht es auf der Rechnung, kostet aber 0.
    const imPaket = (id === 'subdomain' && paket.subdomainInklusive)
                 || (id === 'flfac' && paket.flfacInklusive);
    const einzel = imPaket ? 0 : zusatz.preis;
    const summe = Math.round(einzel * menge * 100) / 100;

    if (zusatz.art === 'einmalig') {
      einmalig += summe;
    } else if (zusatz.art === 'proWoche') {
      // Wochenposten schlagen wir mit 4 Wochen auf den Monat um, damit
      // unten eine ehrliche Monatssumme steht.
      proMonat += summe * 4;
    } else {
      proMonat += summe;
    }

    if (zusatz.wirkung) {
      for (const [feld, wert] of Object.entries(zusatz.wirkung)) {
        ausstattung[feld] = Math.round((ausstattung[feld] + wert * menge) * 100) / 100;
      }
    }

    posten.push({
      art: 'zusatz', id, name: zusatz.name, menge,
      einzel, summe,
      takt: zusatz.art === 'einmalig' ? 'einmalig'
          : zusatz.art === 'proWoche' ? 'Woche' : 'Monat',
      imPaket,
      hinweis: zusatz.hinweis || '',
    });
  }

  return {
    posten,
    proMonat: Math.round(proMonat * 100) / 100,
    einmalig: Math.round(einmalig * 100) / 100,
    ausstattung,
  };
}

/** 3.99 -> "3,99 €" */
export function euro(betrag) {
  return `${Number(betrag).toFixed(2).replace('.', ',')} €`;
}
