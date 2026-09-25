/**
 * Zwei-Faktor-Anmeldung mit einer Authenticator-App.
 *
 * Warum ueberhaupt: Ein Passwort, das in der Schule einmal ueber die
 * Schulter mitgelesen wurde, ist weg - und wer sich am Portal anmeldet,
 * kann die Server ganzer Klassen loeschen. Der zweite Faktor macht aus
 * "jemand kennt mein Passwort" wieder "jemand kennt mein Passwort, hat
 * aber mein Handy nicht".
 *
 * Das Verfahren ist TOTP nach RFC 6238, dasselbe, das Google
 * Authenticator, Aegis, 2FAS und der Rest sprechen. Es ist erfreulich
 * klein: ein gemeinsames Geheimnis, die Uhrzeit in Dreissig-Sekunden-
 * Schritten, HMAC-SHA1 darueber, sechs Ziffern herausschneiden. Alles
 * davon steckt in Node schon drin, es kommt kein Paket dazu.
 *
 * Nachgeprueft wird das mit den Testwerten aus dem RFC selbst
 * (test/zweifach.mjs). Wer die trifft, spricht dasselbe wie die Apps -
 * das ist mehr wert als jede Zusicherung im Kommentar.
 *
 * Was hier bewusst NICHT drin ist: ein QR-Code. Einen zu erzeugen waere
 * nicht schwer, aber ich haette hier kein Werkzeug gehabt, um
 * nachzusehen, ob der erzeugte auch wirklich lesbar ist - und ein
 * QR-Code, den die App nicht annimmt, ist schlimmer als keiner, weil man
 * dann am falschen Ende sucht. Stattdessen steht das Geheimnis in
 * lesbaren Vierergruppen da und laesst sich eintippen; jede App kann
 * das. Der `otpauth://`-Link ist auch da - wer ihn auf dem Handy
 * antippt, springt direkt in die App.
 */

import { createHmac, randomBytes, timingSafeEqual, createHash } from 'node:crypto';

/** Wie lange ein Code gilt. Dreissig Sekunden ist, was alle Apps nehmen. */
export const SCHRITT = 30;

/** Wie viele Ziffern. Sechs ist der Normalfall. */
export const ZIFFERN = 6;

/**
 * Wie weit die Uhren auseinandergehen duerfen.
 *
 * Ein Schritt in jede Richtung, also anderthalb Minuten Spielraum. Ohne
 * das scheitert jeder, dessen Handy eine halbe Minute vorgeht - und der
 * Fehler ist von aussen nicht zu erkennen, weil der Code ja "richtig"
 * aussieht. Mehr als einen Schritt zu erlauben verlaengert nur das
 * Zeitfenster, in dem ein abgefangener Code noch etwas taugt.
 */
export const SPIELRAUM = 1;

// ------------------------------------------------------------------ Base32

const ABC = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

/**
 * Base32 nach RFC 4648 - das Alphabet, das die Apps erwarten.
 *
 * Ohne Fuellzeichen: In einem `otpauth://`-Link waeren die `=` am Ende
 * nur Ballast, und alle Apps kommen ohne aus.
 */
export function base32Kodiere(bytes) {
  let bits = 0;
  let wert = 0;
  let raus = '';
  for (const b of bytes) {
    wert = (wert << 8) | b;
    bits += 8;
    while (bits >= 5) {
      raus += ABC[(wert >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }
  if (bits > 0) raus += ABC[(wert << (5 - bits)) & 31];
  return raus;
}

/**
 * Zurueck zu Bytes.
 *
 * Leerzeichen und Bindestriche fliegen raus: Das Geheimnis steht in der
 * Oberflaeche in Vierergruppen, und wer es abtippt, tippt die Luecken
 * mit. Kleinbuchstaben ebenso - auf dem Handy ist die Umschalttaste weit
 * weg. Beides abzulehnen waere formal richtig und praktisch nur laestig.
 */
export function base32Dekodiere(text) {
  const sauber = String(text || '').toUpperCase().replace(/[\s-]|=+$/g, '');
  let bits = 0;
  let wert = 0;
  const raus = [];
  for (const zeichen of sauber) {
    const stelle = ABC.indexOf(zeichen);
    if (stelle < 0) return null;
    wert = (wert << 5) | stelle;
    bits += 5;
    if (bits >= 8) {
      raus.push((wert >>> (bits - 8)) & 255);
      bits -= 8;
    }
  }
  return Buffer.from(raus);
}

/**
 * Ein neues Geheimnis.
 *
 * 20 Bytes, weil HMAC-SHA1 intern mit 160 Bit arbeitet - mehr brächte
 * nichts, weniger waere schwaecher als das Verfahren selbst.
 */
export const neuesGeheimnis = () => base32Kodiere(randomBytes(20));

/** Zum Abtippen: Vierergruppen lesen sich deutlich leichter. */
export const lesbar = (geheim) => String(geheim).match(/.{1,4}/g)?.join(' ') || '';

// --------------------------------------------------------------------- Code

/**
 * Der Code zu einem Zaehler - HOTP nach RFC 4226.
 *
 * Der Zaehler geht als acht Bytes in einen HMAC-SHA1. Aus dem Ergebnis
 * sagt das letzte Halbbyte, wo die vier Bytes stehen, die zaehlen
 * ("dynamic truncation"); das oberste Bit davon faellt weg, damit
 * Sprachen ohne vorzeichenlose Zahlen nicht ins Negative rutschen.
 */
export function hotp(geheim, zaehler, ziffern = ZIFFERN) {
  const schluessel = base32Dekodiere(geheim);
  if (!schluessel || !schluessel.length) return null;

  const puffer = Buffer.alloc(8);
  puffer.writeBigUInt64BE(BigInt(zaehler));

  const hmac = createHmac('sha1', schluessel).update(puffer).digest();
  const ab = hmac[hmac.length - 1] & 0x0f;
  const zahl = ((hmac[ab] & 0x7f) << 24) | (hmac[ab + 1] << 16)
             | (hmac[ab + 2] << 8) | hmac[ab + 3];

  return String(zahl % 10 ** ziffern).padStart(ziffern, '0');
}

/** Welcher Zeitschritt gerade laeuft. */
export const schrittVon = (sekunden) => Math.floor(sekunden / SCHRITT);

/** Der Code, der gerade gilt. */
export function code(geheim, sekunden = Math.floor(Date.now() / 1000),
                     ziffern = ZIFFERN) {
  return hotp(geheim, schrittVon(sekunden), ziffern);
}

/**
 * Stimmt der eingetippte Code?
 *
 * @returns den getroffenen Zeitschritt, oder null
 *
 * Warum der Schritt zurueckkommt und nicht nur true: Der Aufrufer legt
 * ihn ab und lehnt denselben Schritt beim naechsten Mal ab. Sonst
 * koennte ein Code, den jemand von der Schulter abliest, dreissig
 * Sekunden lang ein zweites Mal benutzt werden - und genau in diesen
 * dreissig Sekunden sitzt derjenige noch daneben.
 */
export function pruefe(geheim, eingabe, sekunden = Math.floor(Date.now() / 1000)) {
  const getippt = String(eingabe || '').replace(/\s/g, '');
  if (!/^\d{6,8}$/.test(getippt)) return null;

  const jetzt = schrittVon(sekunden);
  for (let v = -SPIELRAUM; v <= SPIELRAUM; v++) {
    const soll = hotp(geheim, jetzt + v, getippt.length);
    if (!soll) return null;
    // Zeichenweise ohne Abbruch: Ein Vergleich, der beim ersten
    // Unterschied aufhoert, verraet ueber die Dauer, wie viele Ziffern
    // schon stimmten. Bei sechs Ziffern ist das Ausnutzen dieser
    // Information reichlich theoretisch - aber es kostet nichts.
    const a = Buffer.from(soll);
    const b = Buffer.from(getippt);
    if (a.length === b.length && timingSafeEqual(a, b)) return jetzt + v;
  }
  return null;
}

// ------------------------------------------------------------------- Link

/**
 * Der `otpauth://`-Link, den die Apps verstehen.
 *
 * Der Herausgeber steht zweimal drin - einmal im Pfad, einmal als
 * Parameter. Das sieht doppelt gemoppelt aus und ist es auch; die
 * aelteren Apps lesen nur das eine, die neueren nur das andere.
 */
export function link(benutzername, geheim, herausgeber = 'Lemon Hosting') {
  const wer = `${herausgeber}:${benutzername}`;
  return `otpauth://totp/${encodeURIComponent(wer)}`
       + `?secret=${geheim}`
       + `&issuer=${encodeURIComponent(herausgeber)}`
       + `&algorithm=SHA1&digits=${ZIFFERN}&period=${SCHRITT}`;
}

// ---------------------------------------------------- Ersatzcodes

/** Wie viele Ersatzcodes es gibt. */
export const WIE_VIELE_ERSATZ = 8;

/**
 * Ersatzcodes fuer den Fall, dass das Handy weg ist.
 *
 * Ohne sie waere ein verlorenes Handy ein verlorenes Konto, und das
 * Portal braeuchte fuer jeden Verlust einen Admin, der von Hand in der
 * Datenbank aufraeumt. Jeder Code gilt genau einmal.
 *
 * Abgelegt werden nur die Hashes - wie beim API-Schluessel. Ein
 * einfacher SHA-256 reicht: Der Code ist selbst schon zufaellig genug,
 * da bringt ein langsames Verfahren nichts zu erraten, was ohnehin
 * aussichtslos waere.
 */
export function neueErsatzcodes(wieViele = WIE_VIELE_ERSATZ) {
  const raus = [];
  for (let i = 0; i < wieViele; i++) {
    // Zehn Stellen aus dem Base32-Alphabet, in zwei Gruppen. Keine
    // Buchstaben, die man mit Ziffern verwechselt - 0/O und 1/I sind in
    // diesem Alphabet ohnehin nicht beide drin.
    const roh = base32Kodiere(randomBytes(7)).slice(0, 10);
    raus.push(roh.slice(0, 5) + '-' + roh.slice(5));
  }
  return raus;
}

export const ersatzHash = (code) =>
  createHash('sha256').update(String(code).toUpperCase().replace(/\s/g, '')).digest('hex');
