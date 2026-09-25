const path = require('node:path');

const WURZEL = path.resolve(__dirname, '..');
const DATEN = path.join(WURZEL, 'daten');
const SICHERUNGEN = path.join(DATEN, 'sicherungen');
const WIKI_DATEN = path.join(DATEN, 'wiki.json');
const WIKI_ORDNER = path.join(WURZEL, 'wiki');
const MOMENTS_ORDNER = path.join(WURZEL, 'top', '10', 'aura-moments');
const MOMENTS_SKRIPT = path.join(MOMENTS_ORDNER, 'moments.js');
const MOMENTS_MEDIEN = path.join(MOMENTS_ORDNER, 'media');
const MOMENTS_DATEN = path.join(DATEN, 'moments.json');

const NIE_AUSLIEFERN = new Set([
  'daten',
  'server',
  'server.js',
  'node_modules',
  'index.zip',
  'bettersmp-suite',
  'lemon-hosting',
  'dist',
  'proxy',
  'texturepack',
  'tools',
]);

function teileVon(relativerPfad) {
  return relativerPfad.split('/').filter(teil => teil !== '');
}

function istSichererTeil(teil) {
  return teil !== ''
    && !teil.startsWith('.')
    && !teil.includes('\\')
    && !teil.includes(':')
    && !teil.includes('\0');
}

function relativVonWurzel(absoluterPfad) {
  return path.relative(WURZEL, absoluterPfad).split(path.sep).join('/');
}

module.exports = {
  WURZEL,
  DATEN,
  SICHERUNGEN,
  WIKI_DATEN,
  WIKI_ORDNER,
  MOMENTS_SKRIPT,
  MOMENTS_MEDIEN,
  MOMENTS_DATEN,
  NIE_AUSLIEFERN,
  teileVon,
  istSichererTeil,
  relativVonWurzel,
};
