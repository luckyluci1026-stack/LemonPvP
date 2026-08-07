import React, { useState, useEffect, useRef, useCallback } from "react";

/* ----------------------------- Icon-System ------------------------------
   Font Awesome (Free, via CDN in index.html) statt lucide-react.
   FaIcon(name) erzeugt eine Komponente mit identischer API wie zuvor
   (Prop "size", "className"), damit alle bestehenden Stellen im Code
   unverändert bleiben — nur das Rendering wechselt auf <i class="fa-..."/>.
   ------------------------------------------------------------------------- */
function FaIcon(name, style = "solid") {
  const Icon = ({ size = 16, className = "", title }) => (
    <i
      aria-hidden={title ? undefined : "true"}
      title={title}
      className={`fa-${style} fa-${name} ${className}`}
      style={{ fontSize: size, width: size, display: "inline-flex", alignItems: "center", justifyContent: "center", lineHeight: 1, flexShrink: 0 }}
    />
  );
  return Icon;
}

const Code2 = FaIcon("code");
const Sparkles = FaIcon("wand-magic-sparkles");
const Zap = FaIcon("bolt");
const Flame = FaIcon("fire");
const Star = FaIcon("star");
const Shield = FaIcon("shield-halved");
const Trophy = FaIcon("trophy");
const CheckCircle2 = FaIcon("circle-check");
const XCircle = FaIcon("circle-xmark");
const BookOpen = FaIcon("book-open");
const GraduationCap = FaIcon("graduation-cap");
const Users = FaIcon("users");
const LogOut = FaIcon("right-from-bracket");
const ChevronRight = FaIcon("chevron-right");
const ChevronDown = FaIcon("chevron-down");
const ChevronLeft = FaIcon("chevron-left");
const Lock = FaIcon("lock");
const Copy = FaIcon("copy");
const Check = FaIcon("check");
const ArrowRight = FaIcon("arrow-right");
const ArrowLeft = FaIcon("arrow-left");
const Home = FaIcon("house");
const User = FaIcon("user");
const Award = FaIcon("award");
const Bot = FaIcon("robot");
const Send = FaIcon("paper-plane");
const Loader2 = FaIcon("spinner");
const Menu = FaIcon("bars");
const X = FaIcon("xmark");
const Mail = FaIcon("envelope");
const Eye = FaIcon("eye");
const Layers = FaIcon("layer-group");
const BarChart3 = FaIcon("chart-column");
const Clock = FaIcon("clock");
const Play = FaIcon("play");
const Plus = FaIcon("plus");
const Globe = FaIcon("globe");
const Palette = FaIcon("palette");
const Database = FaIcon("database");
const Terminal = FaIcon("terminal");
const Rocket = FaIcon("rocket");
const Brain = FaIcon("brain");
const PenLine = FaIcon("pen");
const ListChecks = FaIcon("list-check");
const TrendingUp = FaIcon("arrow-trend-up");
const Crown = FaIcon("crown");
const Medal = FaIcon("medal");
const KeyRound = FaIcon("key");
const AtSign = FaIcon("at");
const Cpu = FaIcon("microchip");
const Coffee = FaIcon("mug-hot");
const LayoutDashboard = FaIcon("table-columns");
const Settings = FaIcon("gear");
const Trash2 = FaIcon("trash-can");
const Info = FaIcon("circle-info");
const Scale = FaIcon("scale-balanced");
const FileText = FaIcon("file-lines");
const ShieldCheck = FaIcon("shield-halved");
const UserRoundPlus = FaIcon("user-plus");
const Flag = FaIcon("flag");
const Bug = FaIcon("bug");
const Wand = FaIcon("wand-magic-sparkles");
const ExternalLink = FaIcon("arrow-up-right-from-square");
const Download = FaIcon("download");
const MapIcon = FaIcon("map");
const VolumeOn = FaIcon("volume-high");
const VolumeOff = FaIcon("volume-xmark");
const FilePlus = FaIcon("file-circle-plus");
const FolderTree = FaIcon("folder-tree");
const FileCode = FaIcon("file-code");
const Save = FaIcon("floppy-disk");
const Expand = FaIcon("expand");
const Compress = FaIcon("compress");
const PenSquare = FaIcon("pen-to-square");
const Sparkles2 = FaIcon("wand-sparkles");
const ClipboardList = FaIcon("clipboard-list");
const Forward = FaIcon("forward");
const ChevronUp = FaIcon("chevron-up");
const Store = FaIcon("store");
const Lightbulb = FaIcon("lightbulb");
const Palette2 = FaIcon("swatchbook");
const Snowflake = FaIcon("snowflake");

/* =========================================================================
   LearnDeveloping — learndeveloping.com
   Eine Single-File Lernplattform (React + Tailwind + Font Awesome)
   ========================================================================= */

const GRADIENT = "linear-gradient(135deg, #4F8EF7, #7C3AED)";

/* ---------------------------- Global Styles ---------------------------- */
function GlobalStyles() {
  return (
    <style dangerouslySetInnerHTML={{ __html: `
      @import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;700&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');
      * { box-sizing: border-box; }
      html, body, #root { height: 100%; }
      body { margin: 0; background: #0A0E1A; color: #E8EDF5; font-family: 'Inter', system-ui, sans-serif; }
      .font-display { font-family: 'Space Grotesk', sans-serif; }
      .font-code { font-family: 'JetBrains Mono', monospace; }
      .ld-gradient-text { background: ${GRADIENT}; -webkit-background-clip: text; background-clip: text; color: transparent; }
      ::-webkit-scrollbar { width: 10px; height: 10px; }
      ::-webkit-scrollbar-track { background: #0A0E1A; }
      ::-webkit-scrollbar-thumb { background: #1E2D4A; border-radius: 6px; }
      ::-webkit-scrollbar-thumb:hover { background: #2A3F6F; }
      @keyframes ld-scroll-up { from { transform: translateY(0); } to { transform: translateY(-50%); } }
      @keyframes ld-fade-up { 0% { opacity: 0; transform: translateY(16px) scale(.9); } 20% { opacity: 1; transform: translateY(0) scale(1); } 80% { opacity: 1; } 100% { opacity: 0; transform: translateY(-24px) scale(1); } }
      @keyframes ld-toast-in { from { opacity: 0; transform: translateX(40px); } to { opacity: 1; transform: translateX(0); } }
      @keyframes ld-pulse { 0%,100% { opacity: .5; } 50% { opacity: 1; } }
      @keyframes ld-spin { to { transform: rotate(360deg); } }
      @keyframes ld-grow { from { width: 0; } }
      @keyframes ld-pop { 0% { transform: scale(.8); opacity: 0; } 60% { transform: scale(1.05); } 100% { transform: scale(1); opacity: 1; } }
      @keyframes ld-confetti { 0% { transform: translateY(0) rotateZ(0deg); opacity: 1; } 100% { transform: translateY(105vh) rotateZ(720deg); opacity: 0; } }
      @keyframes ld-shimmer { to { background-position: -200% 0; } }
      .ld-skeleton { background: linear-gradient(90deg,#141D35 25%,#1A2540 50%,#141D35 75%); background-size: 200% 100%; animation: ld-pulse 1.5s ease-in-out infinite; border-radius: 8px; }
      .ld-spin { animation: ld-spin 1s linear infinite; }
      .ld-pop { animation: ld-pop .3s ease-out; }
      textarea, input { outline: none; }
      :focus-visible { outline: 2px solid #4F8EF7; outline-offset: 2px; border-radius: 6px; }
      button { -webkit-tap-highlight-color: transparent; }
      @media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation-duration: .001ms !important; transition-duration: .001ms !important; } }
    `}} />
  );
}

/* ============================== Klänge ==================================
   Sämtliche Töne entstehen im Browser selbst (Web Audio API) — es gibt keine
   Audiodateien, nichts wird nachgeladen, und es sind keine fremden Klänge
   im Spiel. Jeder Ton besteht aus Oszillatoren mit kurzen Hüllkurven; das
   klingt sauber, kostet praktisch nichts und funktioniert offline.

   Browser starten Audio erst nach einer Nutzerinteraktion. Der Klangkontext
   wird deshalb erst beim ersten Abspielen erzeugt und, falls angehalten,
   wieder aufgeweckt.
   ======================================================================== */
const SOUND_STORAGE = "learndeveloping_sound";

const audio = {
  ctx: null,
  volume: 0.3,
  enabled: (() => {
    try { return localStorage.getItem(SOUND_STORAGE) !== "off"; } catch (e) { return true; }
  })(),
  ensure() {
    if (!this.enabled) return null;
    try {
      const Ctx = window.AudioContext || window.webkitAudioContext;
      if (!Ctx) return null;
      if (!this.ctx) this.ctx = new Ctx();
      if (this.ctx.state === "suspended") this.ctx.resume();
      return this.ctx;
    } catch (e) { return null; }
  },
  setEnabled(on) {
    this.enabled = !!on;
    try { localStorage.setItem(SOUND_STORAGE, on ? "on" : "off"); } catch (e) {}
    if (on) { const c = this.ensure(); if (c) SOUNDS.click(c); }
  },
};

/**
 * Ein einzelner Ton. Das kurze Ein- und das weiche Ausblenden verhindern das
 * typische Knacken, das bei hart geschalteten Oszillatoren entsteht.
 */
function tone(ctx, { freq, at = 0, dur = 0.18, type = "sine", gain = 1, glide = null }) {
  const t0 = ctx.currentTime + at;
  const osc = ctx.createOscillator();
  const amp = ctx.createGain();
  osc.type = type;
  osc.frequency.setValueAtTime(freq, t0);
  if (glide) osc.frequency.exponentialRampToValueAtTime(Math.max(20, glide), t0 + dur);
  const peak = Math.max(0.0001, gain * audio.volume);
  amp.gain.setValueAtTime(0.0001, t0);
  amp.gain.exponentialRampToValueAtTime(peak, t0 + 0.012);
  amp.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
  osc.connect(amp);
  amp.connect(ctx.destination);
  osc.start(t0);
  osc.stop(t0 + dur + 0.03);
}

/** Gefiltertes Rauschen — gibt Klicks und Fehlertönen ihren Körper. */
function noise(ctx, { at = 0, dur = 0.09, gain = 0.3, freq = 1400, q = 1 }) {
  const t0 = ctx.currentTime + at;
  const frames = Math.max(1, Math.floor(ctx.sampleRate * dur));
  const buffer = ctx.createBuffer(1, frames, ctx.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < frames; i++) data[i] = (Math.random() * 2 - 1) * (1 - i / frames);
  const src = ctx.createBufferSource();
  src.buffer = buffer;
  const filter = ctx.createBiquadFilter();
  filter.type = "bandpass";
  filter.frequency.value = freq;
  filter.Q.value = q;
  const amp = ctx.createGain();
  amp.gain.setValueAtTime(Math.max(0.0001, gain * audio.volume), t0);
  amp.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
  src.connect(filter);
  filter.connect(amp);
  amp.connect(ctx.destination);
  src.start(t0);
  src.stop(t0 + dur);
}

// Die Klangbibliothek. Frequenzen orientieren sich an Dur-Intervallen —
// aufwärts klingt nach Erfolg, abwärts nach Fehlschlag.
const SOUNDS = {
  // Zwei aufsteigende Terzen: kurz, freundlich, nicht aufdringlich
  correct: (c) => {
    tone(c, { freq: 587.33, dur: 0.11, type: "triangle", gain: 0.55 });               // D5
    tone(c, { freq: 880.00, at: 0.085, dur: 0.20, type: "triangle", gain: 0.5 });     // A5
  },
  // Weicher, fallender Ton — deutlich, aber ohne Bestrafungscharakter
  wrong: (c) => {
    tone(c, { freq: 311.13, dur: 0.22, type: "sine", gain: 0.45, glide: 196 });
    noise(c, { at: 0.01, dur: 0.07, gain: 0.12, freq: 500 });
  },
  // Dur-Dreiklang aufwärts mit Oktavschluss
  levelUp: (c) => {
    const notes = [523.25, 659.25, 783.99, 1046.5];                                    // C-E-G-C
    notes.forEach((f, i) => tone(c, { freq: f, at: i * 0.075, dur: 0.3, type: "triangle", gain: 0.45 }));
  },
  // Fanfare zum Lektionsabschluss
  lessonComplete: (c) => {
    const notes = [523.25, 659.25, 783.99, 1046.5, 1318.5];
    notes.forEach((f, i) => tone(c, { freq: f, at: i * 0.06, dur: 0.35, type: "triangle", gain: 0.4 }));
    tone(c, { freq: 261.63, at: 0.06, dur: 0.5, type: "sine", gain: 0.25 });
  },
  // Heller Glockenschlag für ein neues Abzeichen
  badge: (c) => {
    tone(c, { freq: 1174.7, dur: 0.5, type: "sine", gain: 0.35 });
    tone(c, { freq: 1567.98, at: 0.05, dur: 0.45, type: "sine", gain: 0.22 });
  },
  // Leises Ploppen bei jedem XP-Gewinn
  xp: (c) => tone(c, { freq: 1046.5, dur: 0.09, type: "sine", gain: 0.3, glide: 1318.5 }),
  // Warmer Doppelton für die Serie
  streak: (c) => {
    tone(c, { freq: 440, dur: 0.13, type: "triangle", gain: 0.4 });
    tone(c, { freq: 659.25, at: 0.1, dur: 0.24, type: "triangle", gain: 0.4 });
  },
  // Kurzer Bestätigungston beim Speichern
  save: (c) => tone(c, { freq: 783.99, dur: 0.12, type: "sine", gain: 0.32, glide: 1046.5 }),
  // Trockener Klick für Schaltflächen
  click: (c) => noise(c, { dur: 0.045, gain: 0.16, freq: 2400, q: 2 }),
  // Fehlermeldung: zwei tiefe Impulse
  error: (c) => {
    tone(c, { freq: 220, dur: 0.1, type: "square", gain: 0.18 });
    tone(c, { freq: 185, at: 0.11, dur: 0.14, type: "square", gain: 0.18 });
  },
};

/** Spielt einen Klang, falls Töne eingeschaltet sind. Schlägt nie fehl. */
function playSound(name) {
  const ctx = audio.ensure();
  if (!ctx) return;
  const make = SOUNDS[name];
  if (!make) return;
  try { make(ctx); } catch (e) {}
}

/* ------------------------------ Levels --------------------------------- */
const LEVELS = [
  { level: 1, name: "Rookie", xp: 0 },
  { level: 2, name: "Beginner", xp: 200 },
  { level: 3, name: "Learner", xp: 500 },
  { level: 4, name: "Coder", xp: 1000 },
  { level: 5, name: "Developer", xp: 2000 },
  { level: 6, name: "Hacker", xp: 3500 },
  { level: 7, name: "Pro Coder", xp: 5000 },
  { level: 8, name: "Expert", xp: 7000 },
  { level: 9, name: "Master", xp: 8500 },
  { level: 10, name: "Senior Dev", xp: 10000 },
  { level: 11, name: "Lead Dev", xp: 13000 },
  { level: 12, name: "Principal", xp: 16000 },
  { level: 13, name: "Guru", xp: 20000 },
  { level: 14, name: "Sage", xp: 24000 },
  { level: 15, name: "Ninja", xp: 29000 },
  { level: 16, name: "Legend", xp: 34000 },
  { level: 17, name: "Mythic", xp: 40000 },
  { level: 18, name: "Grandmaster", xp: 44000 },
  { level: 19, name: "Code Master", xp: 47000 },
  { level: 20, name: "Code Wizard", xp: 50000 },
];

function getLevelInfo(xp) {
  let cur = LEVELS[0];
  for (const l of LEVELS) if (xp >= l.xp) cur = l;
  const next = LEVELS.find((l) => l.xp > xp) || null;
  const base = cur.xp;
  const span = next ? next.xp - base : 1;
  const pct = next ? Math.min(100, Math.round(((xp - base) / span) * 100)) : 100;
  return { ...cur, next, pct, toNext: next ? next.xp - xp : 0 };
}

/* ------------------------------ Badges --------------------------------- */
const BADGES = {
  first_lesson: { icon: "ziel", color: "#10B981", label: "Erste Lektion", desc: "Erste Lektion abgeschlossen" },
  week_warrior: { icon: "flamme", color: "#F59E0B", label: "7-Tage Streak", desc: "Eine Woche am Stück gelernt" },
  js_beginner: { icon: "javascript", color: "#F7C948", label: "JS Starter", desc: "10 JavaScript-Lektionen" },
  course_complete: { icon: "crown", color: "#F7C948", label: "Kurs-Meister", desc: "Einen ganzen Kurs abgeschlossen" },
  ai_master: { icon: "feedback", color: "#7C3AED", label: "KI-Master", desc: "KI-Score über 95%" },
  mid_wizard: { icon: "stern", color: "#4F8EF7", label: "Mid Wizard", desc: "Level 10 erreicht" },
};

/* ------------------------------ Difficulty ----------------------------- */
const DIFF = {
  beginner: { label: "Anfänger", color: "#10B981" },
  intermediate: { label: "Mittel", color: "#4F8EF7" },
  advanced: { label: "Fortgeschritten", color: "#F59E0B" },
  expert: { label: "Experte", color: "#7C3AED" },
};
const XP_BY_LEVEL = { beginner: 50, intermediate: 75, advanced: 100, expert: 125 };

/* ------------------------------ Courses -------------------------------- */
// Helper: baut Module/Lektionen mit IDs wie  courseId_<modulnr>_<lektionnr>
function buildCourse(id, name, icon, color, description, mods) {
  let total = 0;
  // Die Erweiterungsmodule sind Teil desselben Kurses und werden hinten
  // angehängt — die IDs zählen dadurch einfach weiter.
  const allMods = [...mods, ...(COURSE_EXTENSIONS[id] || [])];
  const modules = allMods.map((m, mi) => {
    const lessons = m.lessons.map((title, li) => {
      total++;
      return {
        id: `${id}_${mi + 1}_${li + 1}`,
        title,
        level: m.level,
        xpReward: XP_BY_LEVEL[m.level],
      };
    });
    return { id: `${id}_m${mi + 1}`, title: m.title, level: m.level, lessons };
  });
  return { id, name, icon, color, description, modules, totalLessons: total };
}

/* ------------------- Erweiterung des Lehrplans ---------------------------
   Diese Module setzen die Kurse fort: von den Werkzeugen des Alltags über
   Testen und Sicherheit bis zu den Themen, nach denen im Vorstellungsgespräch
   gefragt wird. Sie hängen sich hinten an die vorhandenen Module an.
   ------------------------------------------------------------------------- */
const COURSE_EXTENSIONS = {
  html: [
    { title: "Interaktive HTML-Elemente", level: "intermediate", lessons: ["Modale Dialoge mit <dialog>, showModal() und dem returnValue", "Akkordeons ohne JavaScript: details, summary und exklusive name-Gruppen", "Popover API: popovertarget, Light-Dismiss und der Top Layer", "Drag & Drop mit draggable, dragover und dem DataTransfer-Objekt", "Inline-Editoren mit contenteditable und plaintext-only absichern"] },
    { title: "HTML-Werkzeuge und Build", level: "intermediate", lessons: ["Markup prüfen: W3C-Validator und html-validate als CI-Gate", "Emmet-Abkürzungen und Prettier-Formatierung im Editor", "HTML minifizieren mit html-minifier-terser und Whitespace-Fallen", "Wiederverwendbare Partials und Layouts mit Nunjucks und Eleventy"] },
    { title: "HTML-E-Mails bauen", level: "intermediate", lessons: ["E-Mail-Layouts mit verschachtelten Tabellen und VML-Fallbacks für Outlook", "Inline-CSS erzeugen: Juice, Premailer und <style>-Stripping umgehen", "Bulletproof Buttons und Preheader-Text bei blockierten Bildern", "Dark Mode in Mail-Clients: prefers-color-scheme und invertierte Logos", "E-Mails testen mit Litmus, Email on Acid und MailHog"] },
    { title: "Parser und Rendering", level: "advanced", lessons: ["Tokenizer und Baumaufbau: wie Browser HTML zu DOM verarbeiten", "Tag-Soup debuggen: implizite Elemente und Fehlerkorrektur des Parsers", "Void-Elemente, Self-Closing-Slash und die hartnäckigen XHTML-Mythen", "SVG und MathML als Foreign Content inline einbetten", "Skriptausführung steuern: defer, async und type=module im Vergleich"] },
    { title: "Ladeverhalten optimieren", level: "advanced", lessons: ["loading=lazy, decoding=async und fetchpriority sinnvoll kombinieren", "Layout Shifts vermeiden: width, height und aspect-ratio im Markup", "Resource Hints richtig setzen: preload, preconnect, dns-prefetch, modulepreload", "Speculation Rules: Prefetch und Prerender per JSON-Skript steuern", "Critical Rendering Path im Performance-Panel und mit Lighthouse analysieren"] },
    { title: "Sicheres Markup", level: "advanced", lessons: ["XSS-Vektoren im Markup: innerHTML, javascript:-URLs und on*-Attribute", "Nutzereingaben rendern: Sanitizer API und DOMPurify im Vergleich", "iframes härten mit sandbox, allow und Permissions Policy", "rel=noopener, noreferrer und referrerpolicy für externe Links wählen", "Subresource Integrity und CSP-Nonces im HTML verankern"] },
    { title: "Web Components", level: "expert", lessons: ["Custom Elements definieren: Lifecycle-Callbacks und observedAttributes", "Shadow DOM: Kapselung, Slots und Styling über ::part und ::slotted", "<template> klonen statt HTML-Strings zusammenzubauen", "Declarative Shadow DOM für Server-Side Rendering und Hydration", "Form-Associated Custom Elements mit ElementInternals bauen"] },
  ],
  css: [
    { title: "Typografie im Web", level: "intermediate", lessons: ["Webfonts einbinden: @font-face, woff2 und Subsetting", "Ladeverhalten steuern: font-display, preload und FOUT vermeiden", "Variable Fonts mit font-variation-settings feinjustieren", "OpenType-Features: Ligaturen, Kapitälchen und Tabellenziffern", "Lesetypografie: Zeilenlänge, hyphens und Umbruchkontrolle"] },
    { title: "Farbe und Kontrast", level: "intermediate", lessons: ["Farbräume vergleichen: sRGB, HSL, LCH und OKLCH", "Paletten ableiten mit color-mix() und relativer Farbsyntax", "Verläufe steuern: conic, radial und Interpolationsräume", "WCAG-Kontrastwerte messen und Farbfehlsichtigkeit einplanen"] },
    { title: "Grafikeffekte und Masken", level: "intermediate", lessons: ["filter und backdrop-filter für Unschärfe- und Glaseffekte", "Freistellen mit clip-path und Textumfluss per shape-outside", "Verlaufs- und Bildmasken mit mask-image umsetzen", "Blend Modes: mix-blend-mode und background-blend-mode"] },
    { title: "Formulare und Bedienelemente", level: "intermediate", lessons: ["Native Controls stylen mit appearance und accent-color", "Checkboxen, Radios und Selects zugänglich nachbauen", "dialog und popover gestalten inklusive ::backdrop", "Sichtbarer Tastaturfokus, Klickflächen und forced-colors-Modus"] },
    { title: "CSS-Architektur", level: "advanced", lessons: ["Spezifität berechnen und !important-Spiralen auflösen", "BEM in großen Codebasen konsequent durchziehen", "ITCSS: Stylesheets in Schichten und Ordner organisieren", "Utility-First mit Tailwind gegen Komponenten-CSS abwägen", "Styles kapseln im Shadow DOM: :host, ::slotted und ::part"] },
    { title: "Tooling und Ökosystem", level: "advanced", lessons: ["Sass produktiv: Partials, Mixins und @use statt @import", "PostCSS-Pipeline mit Autoprefixer und postcss-preset-env", "Stylelint konfigurieren und in der CI erzwingen", "Ungenutztes CSS aufspüren mit DevTools Coverage und PurgeCSS"] },
    { title: "Performance und Rendering", level: "advanced", lessons: ["Kritisches CSS extrahieren und Render-Blocking auflösen", "Reflow und Repaint im Performance-Panel analysieren", "Lange Seiten beschleunigen mit content-visibility und contain", "GPU-Layer und will-change gezielt dosieren"] },
    { title: "Debugging und Tests", level: "expert", lessons: ["z-index-Fehler über Stacking Contexts erklären und beheben", "Ursachen für horizontales Scrollen systematisch eingrenzen", "Cross-Browser-Bugs mit Feature Queries und Fallbacks absichern", "Visuelle Regressionstests mit Playwright-Screenshots automatisieren"] },
  ],
  javascript: [
    { title: "Daten und Formate", level: "intermediate", lessons: ["Map, Set und WeakMap statt Objekt-Lookups einsetzen", "Rundungsfehler bei Fließkommazahlen erkennen und BigInt nutzen", "Datum, Zeitzonen und relative Zeiten mit Intl formatieren", "JSON-Sonderfälle mit replacer, reviver und structuredClone lösen"] },
    { title: "Reguläre Ausdrücke", level: "intermediate", lessons: ["Zeichenklassen, Quantifizierer und Anker sicher lesen", "Gruppen, benannte Gruppen und Backreferences nutzen", "Text umbauen mit matchAll und replace-Callbacks", "Catastrophic Backtracking erkennen und ReDoS entschärfen"] },
    { title: "Testen in JavaScript", level: "intermediate", lessons: ["Vitest einrichten: erster Unit-Test und Watch-Modus", "Spies, Mocks und Fake Timer für Zeit und Zufall", "HTTP-Antworten mit Mock Service Worker simulieren", "DOM-Verhalten mit Testing Library statt Interna prüfen", "End-to-End-Tests mit Playwright und Testläufe in CI"] },
    { title: "Event Loop und Nebenläufigkeit", level: "advanced", lessons: ["Call Stack, Task Queue und Microtask Queue nachvollziehen", "setTimeout, queueMicrotask und requestAnimationFrame abgrenzen", "Generatoren und yield für pausierbare Abläufe", "Datenströme mit asynchronen Iteratoren und for await...of lesen", "Rechenlast mit Web Workers vom Main Thread nehmen"] },
    { title: "Debugging und Diagnose", level: "advanced", lessons: ["Conditional Breakpoints und Watch Expressions in den DevTools", "Source Maps erzeugen und Produktions-Stacktraces lesen", "Speicherlecks über Heap Snapshots und Detached DOM Nodes finden", "Laufzeitfehler mit Sentry erfassen, gruppieren und entrauschen"] },
    { title: "Node.js im Alltag", level: "advanced", lessons: ["CLI-Werkzeuge mit process.argv, stdin und Exit-Codes bauen", "Große Dateien mit Streams und Backpressure verarbeiten", "REST-API mit Express: Routing, Middleware und Fehler-Handler", "Konfiguration über Umgebungsvariablen und .env-Dateien trennen"] },
    { title: "Build und Ökosystem", level: "advanced", lessons: ["package.json verstehen: Skripte, exports und peerDependencies", "Semantic Versioning, Lockfiles und Dependency-Updates im Team", "Vite konfigurieren: Dev-Server, Aliase und Produktions-Build", "ESLint und Prettier als verbindlichen Team-Standard einrichten"] },
    { title: "Sicherheit im Web", level: "expert", lessons: ["XSS über innerHTML verhindern und mit DOMPurify sanitizen", "Content Security Policy aufsetzen und Verstöße auswerten", "Sessions, Cookies, SameSite und CSRF-Token absichern", "Supply-Chain-Risiken mit npm audit und Lockfile-Prüfung senken"] },
  ],
  typescript: [
    { title: "Compiler im Griff", level: "intermediate", lessons: ["strict-Modus schrittweise einführen: von noImplicitAny bis strictNullChecks", "target, lib und Downlevel-Ausgabe für Node und Browser wählen", "Typfehler lesen und beheben: TS2322, TS2345, TS7006 im Alltag", "Inkrementelle Builds und Project References im Monorepo", "Typprüfung von der Transpilation trennen: tsc --noEmit neben esbuild"] },
    { title: "Bestandscode migrieren", level: "intermediate", lessons: ["allowJs und checkJs aktivieren und Fehlerflut eindämmen", "Typen per JSDoc annotieren, ohne Dateien umzuschreiben", "any-Fundstellen aufspüren und durch unknown ersetzen", "@ts-expect-error statt @ts-ignore als sichtbares Migrations-Schuldenbuch"] },
    { title: "Asynchron und robust", level: "intermediate", lessons: ["Rückgabetypen von async-Funktionen und await korrekt modellieren", "catch liefert unknown: Fehlerobjekte typsicher auswerten", "Result-Typen statt geworfener Exceptions für erwartbare Fehler", "Async Iteratoren für paginierte APIs typisieren"] },
    { title: "Daten an Systemgrenzen", level: "advanced", lessons: ["JSON.parse liefert any: warum Typen an der Systemgrenze enden", "Laufzeitvalidierung mit Zod und Typen per z.infer ableiten", "Branded Types für IDs, E-Mail-Adressen und Geldbeträge", "API-Typen aus OpenAPI-Schemas generieren und aktuell halten"] },
    { title: "Tests und Linting", level: "advanced", lessons: ["Unit-Tests mit Vitest in einem TypeScript-Projekt schreiben", "Mocks und Spies typisieren mit vi.mocked und Partial-Stubs", "Typen selbst testen: expectTypeOf und erwartete Typfehler", "typescript-eslint: typbasierte Regeln wie no-floating-promises nutzen"] },
    { title: "Typen auf Expertenniveau", level: "advanced", lessons: ["Template Literal Types für Routenpfade und Event-Namen", "Variadische Tupel-Typen für Wrapper- und Curry-Funktionen", "Rekursive Typen für verschachtelte Objektpfade wie \"user.address.city\"", "satisfies statt as: prüfen, ohne Literaltypen zu verlieren"] },
    { title: "Backend und Ökosystem", level: "advanced", lessons: ["Express-Handler, Middleware und Router typsicher aufbauen", "Fremde Bibliothekstypen per Modul-Augmentation erweitern", "Datenbankzugriffe mit Prisma-generierten Typen absichern", "End-to-End-Typsicherheit zwischen Client und Server mit tRPC"] },
    { title: "Ausliefern und optimieren", level: "expert", lessons: ["Compiler-Laufzeit messen mit --diagnostics und --generateTrace", "Langsame Typen entschärfen und Instanziierungstiefe begrenzen", "Pakete dual ausliefern: exports-Feld für ESM und CommonJS", "Typ-Regressionen in der CI abfangen mit arethetypeswrong und API Extractor"] },
  ],
  react: [
    { title: "Refs und Portale", level: "intermediate", lessons: ["useRef auf DOM-Knoten: Fokus beim Öffnen eines Dialogs setzen", "Veränderliche Werte in Refs: Timer-IDs und Vorgängerwerte ohne Re-Render halten", "forwardRef und useImperativeHandle für eigene Input-Komponenten", "createPortal und useLayoutEffect: Overlays ohne Flackern positionieren"] },
    { title: "Barrierefreie Interaktion", level: "intermediate", lessons: ["Fokus-Falle und Escape-Handling in einem Modal bauen", "Roving Tabindex für Menüs, Tab-Leisten und Comboboxen", "ARIA-Live-Regionen für Toasts und Ladezustände", "a11y-Fehler aufspüren mit eslint-plugin-jsx-a11y und axe DevTools"] },
    { title: "Fehler und Resilienz", level: "intermediate", lessons: ["Error Boundaries mit react-error-boundary und Reset-Keys", "Warum Fehler in async Handlern von keiner Boundary gefangen werden", "Laufende Requests abbrechen mit AbortController beim Unmount", "Produktionsfehler auswerten: Sentry mit Source Maps und Release-Tags"] },
    { title: "Suspense und Nebenläufigkeit", level: "advanced", lessons: ["React.lazy: routenbasiertes Code-Splitting und Chunk-Ladefehler", "Verschachtelte Suspense-Grenzen und die Reihenfolge von Skeletons", "useTransition gegen eingefrorene Eingaben beim Ansichtswechsel", "useDeferredValue für teure Filterlisten mit tausenden Einträgen", "useSyncExternalStore: externe Stores ohne Tearing anbinden"] },
    { title: "Server-State mit Query", level: "advanced", lessons: ["TanStack Query: Query-Keys, staleTime und Refetch-Verhalten verstehen", "Optimistische Updates mit sauberem Rollback im Fehlerfall", "Endlos-Listen mit useInfiniteQuery und Cursor-Pagination", "Prefetching beim Hover und gezielte Cache-Invalidierung nach Mutationen"] },
    { title: "React mit TypeScript", level: "advanced", lessons: ["Props typisieren mit Discriminated Unions statt optionaler Flags", "ComponentProps und ElementRef für Wrapper um HTML-Elemente", "Polymorphe Komponenten über die as-Prop und Generics", "API-Antworten zur Laufzeit prüfen mit Zod und abgeleiteten Typen"] },
    { title: "Sicherheit im Frontend", level: "advanced", lessons: ["XSS über dangerouslySetInnerHTML: Sanitizing mit DOMPurify", "Auth-Tokens speichern: httpOnly-Cookie statt localStorage", "Content Security Policy mit Nonces für ein Vite-Build einrichten", "Supply-Chain-Risiken prüfen: npm audit, Lockfiles und Postinstall-Skripte"] },
    { title: "React Server Components", level: "expert", lessons: ["Die \"use client\"-Grenze: was auf dem Server bleibt und was nicht", "Datenladen im App Router ohne Request-Wasserfälle", "Server Actions für Mutationen und revalidatePath", "Hydration-Mismatch-Fehler reproduzieren und beheben"] },
  ],
  vue: [
    { title: "Formulare & Validierung", level: "intermediate", lessons: ["Schema-Validierung mit Zod und VeeValidate einbinden", "Fehlermeldungen barrierefrei ausgeben mit aria-invalid und aria-describedby", "Mehrstufige Formulare mit geteiltem Zustand und Entwurfs-Speicherung", "Server-Validierungsfehler (HTTP 422) auf einzelne Felder zurückspielen"] },
    { title: "Datenabruf & Caching", level: "intermediate", lessons: ["Axios-Instanz mit Basis-URL, Interceptors und normalisierten Fehlern", "Race Conditions bei schnellen Eingaben mit AbortController verhindern", "Caching, Invalidierung und Refetch mit TanStack Query for Vue", "Optimistische Updates mit Rollback nach fehlgeschlagenem Request"] },
    { title: "Eigene Composables", level: "intermediate", lessons: ["Composable-Parameter flexibel annehmen mit MaybeRefOrGetter und toValue", "Timer und Event-Listener zuverlässig aufräumen statt Speicherlecks", "VueUse produktiv nutzen: useLocalStorage, useIntersectionObserver, useDebounceFn", "Composables ohne Komponente isoliert ausführen mit effectScope"] },
    { title: "Fortgeschrittene Komponentenmuster", level: "advanced", lessons: ["provide/inject typsicher mit InjectionKey statt String-Schlüsseln", "Modals und Tooltips mit Teleport inklusive Fokus-Falle", "Code-Splitting auf Komponentenebene mit defineAsyncComponent und Suspense", "Eigene Direktiven schreiben: v-click-outside und v-autofocus"] },
    { title: "TypeScript mit Vue", level: "advanced", lessons: ["Props und Emits typisieren mit defineProps<T> und defineEmits<T>", "Generische Komponenten mit generic=\"T\" für Listen und Tabellen", "Slots, Template-Refs und defineExpose typsicher machen", "Typprüfung im Build erzwingen mit vue-tsc in der CI-Pipeline"] },
    { title: "Testen von Vue-Apps", level: "advanced", lessons: ["Testsetup mit Vitest, jsdom und Vue Test Utils aufsetzen", "Komponenten aus Nutzersicht testen mit Testing Library statt Interna", "HTTP-Antworten realistisch mocken mit Mock Service Worker", "Pinia-Stores und Router-Abhängigkeiten in Tests isolieren", "End-to-End-Tests mit Playwright: Selektoren, Fixtures, flaky Tests entschärfen"] },
    { title: "Performance & Profiling", level: "advanced", lessons: ["Renderzeiten messen mit Vue DevTools Timeline und Component Inspector", "Unnötige Re-Renders vermeiden mit v-memo, stabilen Keys und shallowRef", "Lange Listen darstellen mit Virtual Scrolling statt DOM-Flut", "Bundle-Größe analysieren mit rollup-plugin-visualizer und Route-Splitting"] },
    { title: "Sicherheit & Auslieferung", level: "expert", lessons: ["XSS über v-html verhindern mit DOMPurify und Content Security Policy", "Auth-Tokens sicher halten: httpOnly-Cookies, Refresh-Flow, sauberer Logout", "Vite-Produktionsbuild härten: Umgebungsvariablen, Sourcemaps, Cache-Header", "Hydration-Mismatch bei SSR mit Nuxt erkennen und beheben"] },
  ],
  python: [
    { title: "Werkzeuge und Umgebung", level: "intermediate", lessons: ["Pakete verwalten mit pip, pipx und requirements.txt", "Virtuelle Umgebungen mit venv sauber trennen", "Projekte strukturieren mit pyproject.toml", "Code formatieren mit black und sortieren mit isort", "Statische Prüfung mit ruff und mypy"] },
    { title: "Datenstrukturen vertiefen", level: "intermediate", lessons: ["collections: defaultdict, Counter und deque", "namedtuple und dataclass für Wertobjekte", "Sortieren mit key-Funktionen und operator.itemgetter", "Mengenoperationen: Schnitt, Vereinigung, Differenz", "Slicing-Tricks und flache gegen tiefe Kopien"] },
    { title: "Dateien und Formate", level: "intermediate", lessons: ["pathlib statt os.path verwenden", "CSV lesen und schreiben mit dem csv-Modul", "JSON serialisieren, inklusive eigener Encoder", "Konfiguration aus YAML und .env laden", "Große Dateien zeilenweise streamen statt einlesen"] },
    { title: "Fehler und Protokolle", level: "advanced", lessons: ["Eigene Ausnahmeklassen entwerfen", "try/except/else/finally richtig kombinieren", "logging konfigurieren statt print zu benutzen", "Tracebacks lesen und mit pdb schrittweise debuggen", "Kontextmanager mit with und contextlib bauen"] },
    { title: "Testen und Qualität", level: "advanced", lessons: ["pytest: Fixtures, Parametrisierung und Marker", "Mocking mit unittest.mock und monkeypatch", "Testabdeckung messen mit coverage", "Doctests und Beispiele in der Dokumentation", "Tests automatisch laufen lassen in der CI"] },
    { title: "Nebenläufigkeit", level: "advanced", lessons: ["threading und das Global Interpreter Lock verstehen", "multiprocessing für rechenintensive Aufgaben", "asyncio: Tasks, Gather und Timeouts", "Nebenläufige HTTP-Anfragen mit httpx", "Warteschlangen und Producer-Consumer-Muster"] },
    { title: "Web und Daten", level: "expert", lessons: ["Web-API mit FastAPI und Pydantic bauen", "Datenbanken ansprechen mit SQLAlchemy", "Webseiten auslesen mit requests und BeautifulSoup", "pandas: DataFrames filtern, gruppieren, zusammenführen", "Diagramme erzeugen mit matplotlib"] },
    { title: "Auslieferung", level: "expert", lessons: ["Skripte als Kommandozeilenwerkzeug mit argparse und Typer", "Pakete bauen und auf PyPI veröffentlichen", "Anwendungen in Docker-Containern ausliefern", "Performance messen mit timeit und cProfile"] },
  ],
  java: [
    { title: "Werkzeuge und Build", level: "intermediate", lessons: ["Maven: pom.xml, Abhängigkeiten und Lebenszyklus", "Gradle als Alternative kennenlernen", "Projektstruktur nach Konvention aufbauen", "Javadoc schreiben und generieren", "Code-Analyse mit SpotBugs und Checkstyle"] },
    { title: "Moderne Sprachmittel", level: "intermediate", lessons: ["var, Text Blocks und verbesserte switch-Ausdrücke", "Records für unveränderliche Datenklassen", "Sealed Classes und Pattern Matching für instanceof", "Optional richtig einsetzen statt null zurückzugeben", "Enums mit Feldern, Methoden und Konstruktoren"] },
    { title: "Collections vertiefen", level: "intermediate", lessons: ["equals und hashCode korrekt implementieren", "Comparable und Comparator zum Sortieren", "Iterator, Iterable und die erweiterte for-Schleife", "TreeMap, LinkedHashMap und ihre Reihenfolgen", "Unveränderliche Sammlungen mit List.of und Collectors"] },
    { title: "Streams und Funktionales", level: "advanced", lessons: ["Stream-Pipelines: filter, map, reduce", "Collectors: groupingBy, joining, partitioningBy", "Eigene funktionale Schnittstellen definieren", "Method References und ihre vier Formen", "Parallele Streams und wann sie schaden"] },
    { title: "Testen", level: "advanced", lessons: ["JUnit 5: Assertions, Lifecycle und verschachtelte Tests", "Parametrisierte Tests und dynamische Tests", "Mockito: Mocks, Stubs und Verifikation", "Integrationstests mit Testcontainers", "Testabdeckung mit JaCoCo auswerten"] },
    { title: "Nebenläufigkeit vertiefen", level: "advanced", lessons: ["ExecutorService und Thread-Pools", "CompletableFuture für asynchrone Ketten", "synchronized, volatile und das Java Memory Model", "Atomare Klassen und nebenläufige Sammlungen", "Virtual Threads und strukturierte Nebenläufigkeit"] },
    { title: "Spring in der Praxis", level: "expert", lessons: ["Dependency Injection und Bean-Lebenszyklus", "REST-Controller mit Spring Web bauen", "Datenzugriff mit Spring Data JPA", "Konfiguration über Profile und application.yml", "Absichern mit Spring Security"] },
    { title: "Betrieb", level: "expert", lessons: ["Speicherverwaltung und Garbage Collection verstehen", "Anwendungen profilieren mit JFR und VisualVM", "Java-Anwendungen in Containern betreiben"] },
  ],
  kotlin: [
    { title: "Werkzeuge", level: "intermediate", lessons: ["Gradle Kotlin DSL für den Build nutzen", "Multiplattform-Projekte einrichten", "ktlint und detekt als Qualitätsschranke", "Dokumentation mit KDoc und Dokka"] },
    { title: "Typen und Ausdrücke", level: "intermediate", lessons: ["Typinferenz und explizite Typen abwägen", "Destrukturierung und Component-Funktionen", "Operator Overloading mit operator fun", "Infix-Funktionen für lesbare APIs", "Inline-Klassen für typsichere Wrapper"] },
    { title: "Sammlungen", level: "intermediate", lessons: ["Sequences gegen Listen: verzögerte Auswertung", "groupBy, associate und fold in der Praxis", "Veränderliche und unveränderliche Sammlungen trennen", "Eigene Iteratoren und Ranges bauen"] },
    { title: "Fehlerbehandlung", level: "advanced", lessons: ["Result und runCatching statt Ausnahmen", "Eigene Ausnahmen und require/check/assert", "Null-Sicherheit über Modulgrenzen hinweg", "Plattformtypen bei Java-Interoperabilität"] },
    { title: "Coroutines vertiefen", level: "advanced", lessons: ["Scopes, Jobs und strukturierte Nebenläufigkeit", "Dispatcher wählen: Default, IO und Main", "Abbruch, Timeouts und Aufräumen", "Flow: cold streams, Operatoren und Backpressure", "StateFlow und SharedFlow für Zustände"] },
    { title: "Testen", level: "advanced", lessons: ["Unit-Tests mit kotlin.test und JUnit 5", "Coroutines testen mit runTest und TestDispatcher", "MockK für Mocks in Kotlin", "Property-based Testing mit Kotest"] },
    { title: "Interoperabilität und Android", level: "expert", lessons: ["Kotlin und Java im selben Projekt mischen", "Serialisierung mit kotlinx.serialization", "Jetpack Compose: Zustand und Recomposition", "Room und Retrofit anbinden"] },
  ],
  c: [
    { title: "Werkzeugkette", level: "intermediate", lessons: ["Übersetzungsschritte: Präprozessor, Compiler, Linker", "Mehrere Übersetzungseinheiten mit make bauen", "Compiler-Warnungen ernst nehmen: -Wall -Wextra", "Debuggen mit gdb: Breakpoints und Backtraces", "Speicherfehler finden mit Valgrind und ASan"] },
    { title: "Speicher genau verstehen", level: "intermediate", lessons: ["Stack, Heap und statischer Speicher im Vergleich", "Zeiger auf Zeiger und Zeigerarithmetik", "Arrays, Zerfall zu Zeigern und sizeof-Fallen", "Ausrichtung, Padding und struct-Größen", "const, volatile und restrict richtig einsetzen"] },
    { title: "Zeichenketten sicher", level: "advanced", lessons: ["Pufferüberläufe verstehen und vermeiden", "strncpy, snprintf und ihre Tücken", "Eigene sichere String-Funktionen schreiben", "Zeichensätze, UTF-8 und mehrere Bytes je Zeichen"] },
    { title: "Datenstrukturen selbst bauen", level: "advanced", lessons: ["Dynamisches Array mit realloc", "Doppelt verkettete Liste", "Hashtabelle mit Kollisionsbehandlung", "Binärer Suchbaum und Traversierungen", "Stack und Queue mit Ringpuffer"] },
    { title: "Systemnahe Programmierung", level: "advanced", lessons: ["Dateideskriptoren, open, read und write", "Prozesse mit fork und exec starten", "Signale behandeln", "Interprozesskommunikation über Pipes", "Speicher abbilden mit mmap"] },
    { title: "Qualität und Portabilität", level: "expert", lessons: ["Unit-Tests in C mit Unity oder Check", "Undefiniertes Verhalten erkennen und meiden", "Portabler Code über Compiler und Plattformen", "Bibliotheken bauen: statisch und dynamisch"] },
  ],
  cpp: [
    { title: "Moderne Werkzeuge", level: "intermediate", lessons: ["CMake: Ziele, Bibliotheken und Installation", "Paketverwaltung mit vcpkg oder Conan", "Sanitizer und clang-tidy in den Build einbinden", "Debuggen mit gdb und lldb"] },
    { title: "Wertesemantik", level: "intermediate", lessons: ["Rule of Zero, Three und Five", "Kopier- und Verschiebekonstruktoren schreiben", "RAII als Grundprinzip verstehen", "explicit, default und delete gezielt einsetzen", "constexpr und Berechnungen zur Übersetzungszeit"] },
    { title: "STL im Detail", level: "advanced", lessons: ["Container wählen: vector, deque, list, map", "Iteratoren-Kategorien und ihre Bedeutung", "Algorithmen: sort, transform, accumulate", "Lambdas, Captures und std::function", "Ranges und Views in C++20"] },
    { title: "Templates vertiefen", level: "advanced", lessons: ["Funktions- und Klassentemplates schreiben", "Spezialisierung und Überladungsauflösung", "Variadic Templates und Parameter Packs", "Concepts für lesbare Fehlermeldungen", "SFINAE verstehen und ersetzen"] },
    { title: "Nebenläufigkeit", level: "advanced", lessons: ["std::thread, join und detach", "mutex, lock_guard und scoped_lock", "condition_variable für Warteschlangen", "atomic und Speicherordnungen", "async, future und promise"] },
    { title: "Fehler und Tests", level: "expert", lessons: ["Ausnahmen gegen Fehlercodes abwägen", "noexcept und Ausnahmesicherheit", "Unit-Tests mit GoogleTest oder Catch2", "Benchmarking mit Google Benchmark"] },
  ],
  go: [
    { title: "Werkzeuge", level: "intermediate", lessons: ["Module, go.mod und Versionierung", "go vet, staticcheck und gofmt", "Abhängigkeiten aktualisieren und ersetzen", "Programme bauen für andere Plattformen"] },
    { title: "Sprachdetails", level: "intermediate", lessons: ["Slices im Detail: Kapazität, Aliasing, copy", "Maps, Iterationsreihenfolge und Nullwerte", "defer, panic und recover richtig einsetzen", "Methoden auf Werten gegen Zeigerempfänger", "Embedding statt Vererbung"] },
    { title: "Fehlerbehandlung", level: "advanced", lessons: ["Fehler umschließen mit %w und errors.Is", "Eigene Fehlertypen mit errors.As", "Sentinel-Fehler und ihre Grenzen", "Fehler protokollieren gegen Fehler zurückgeben"] },
    { title: "Nebenläufigkeit vertiefen", level: "advanced", lessons: ["Gepufferte gegen ungepufferte Kanäle", "sync.WaitGroup, Mutex und Once", "context für Abbruch und Fristen", "Muster: Fan-Out, Fan-In, Pipeline", "Wettlaufsituationen finden mit dem Race Detector"] },
    { title: "Web-Dienste", level: "advanced", lessons: ["HTTP-Server mit net/http und Routing", "Middleware-Ketten selbst schreiben", "JSON kodieren und dekodieren mit Tags", "Datenbanken mit database/sql ansprechen", "Konfiguration und Graceful Shutdown"] },
    { title: "Testen und Betrieb", level: "expert", lessons: ["Tabellengetriebene Tests schreiben", "httptest für Handler-Tests", "Benchmarks und Profiling mit pprof", "Go-Programme in schlanken Containern ausliefern"] },
  ],
  rust: [
    { title: "Werkzeuge", level: "intermediate", lessons: ["Cargo: Workspaces, Features und Profile", "clippy und rustfmt in den Alltag einbauen", "Dokumentation mit rustdoc und Doctests", "Abhängigkeiten prüfen mit cargo audit"] },
    { title: "Typen und Traits", level: "intermediate", lessons: ["Trait-Objekte gegen Generics abwägen", "Standard-Traits: From, Into, Display, Debug", "Iteratoren selbst implementieren", "Operatorüberladung über Traits", "Default, Clone und Copy richtig ableiten"] },
    { title: "Fehlerbehandlung", level: "advanced", lessons: ["Eigene Fehlertypen mit thiserror", "Fehler zusammenführen mit anyhow", "Der ?-Operator über Fehlertypen hinweg", "panic gegen Result: wann was angebracht ist"] },
    { title: "Lebensdauern vertiefen", level: "advanced", lessons: ["Lebensdauer-Annotationen lesen und schreiben", "Elision-Regeln verstehen", "Structs mit Referenzen halten", "Borrow-Checker-Fehler systematisch auflösen"] },
    { title: "Nebenläufigkeit", level: "advanced", lessons: ["Send und Sync verstehen", "Arc, Mutex und RwLock kombinieren", "Kanäle mit std::sync::mpsc", "async/await mit tokio", "Streams und Aufgaben planen"] },
    { title: "Praxis", level: "expert", lessons: ["Makros: deklarativ und prozedural", "Serialisierung mit serde", "Kommandozeilenwerkzeuge mit clap", "Web-Dienste mit axum", "Unsafe kapseln und begründen"] },
  ],
  php: [
    { title: "Moderne Sprachmittel", level: "intermediate", lessons: ["Typdeklarationen, Union Types und never", "Named Arguments und Konstruktor-Promotion", "Enums und readonly-Eigenschaften", "match-Ausdruck gegen switch", "Nullsafe-Operator und Null Coalescing"] },
    { title: "Werkzeuge", level: "intermediate", lessons: ["Composer: Autoloading, Skripte und Versionen", "PSR-Standards und Code-Stil mit PHP-CS-Fixer", "Statische Analyse mit PHPStan", "Debuggen mit Xdebug"] },
    { title: "Architektur", level: "advanced", lessons: ["Namespaces und PSR-4 sauber aufsetzen", "Dependency Injection ohne Framework", "Interfaces und Traits sinnvoll einsetzen", "Schichten trennen: Controller, Service, Repository"] },
    { title: "Datenbanken", level: "advanced", lessons: ["PDO: Prepared Statements und Transaktionen", "Migrationen und Schemaverwaltung", "N+1-Abfragen erkennen und vermeiden", "Verbindungen und Fehler robust behandeln"] },
    { title: "Sicherheit", level: "advanced", lessons: ["Passwörter mit password_hash speichern", "Sessions absichern und fixieren verhindern", "CSRF-Token und SameSite-Cookies", "Dateiuploads gefahrlos entgegennehmen", "Content Security Policy in PHP setzen"] },
    { title: "Testen und Betrieb", level: "expert", lessons: ["PHPUnit: Tests, Datenanbieter und Mocks", "HTTP-Tests gegen die eigene API", "Caching mit Redis und OPcache", "Warteschlangen und Hintergrundjobs"] },
  ],
  sql: [
    { title: "Abfragen vertiefen", level: "intermediate", lessons: ["CASE-Ausdrücke für bedingte Spalten", "Mengenoperationen: UNION, INTERSECT, EXCEPT", "NULL-Logik und COALESCE richtig einsetzen", "Datums- und Zeitfunktionen", "Zeichenketten aufbereiten und suchen"] },
    { title: "Fortgeschrittene Auswertung", level: "advanced", lessons: ["Fensterfunktionen: ROW_NUMBER, RANK, LAG", "Laufende Summen und gleitende Mittel", "Common Table Expressions mit WITH", "Rekursive Abfragen für Hierarchien", "PIVOT-artige Auswertungen bauen"] },
    { title: "Datenmodellierung", level: "advanced", lessons: ["Normalformen und wann man sie bricht", "Fremdschlüssel und referenzielle Integrität", "Constraints: CHECK, UNIQUE, NOT NULL", "Datentypen richtig wählen", "Beziehungen 1:n und n:m umsetzen"] },
    { title: "Leistung", level: "advanced", lessons: ["Ausführungspläne lesen mit EXPLAIN", "Indizes entwerfen: zusammengesetzt und abdeckend", "Warum ein Index manchmal ignoriert wird", "Statistiken, Kardinalität und Schätzfehler", "Langsame Abfragen systematisch eingrenzen"] },
    { title: "Transaktionen und Betrieb", level: "expert", lessons: ["Isolationsstufen und ihre Anomalien", "Sperren, Deadlocks und wie man sie auflöst", "Sicherungen und Wiederherstellung planen", "Berechtigungen und Rollen vergeben", "Migrationen ohne Ausfallzeit"] },
  ],
};

const COURSES = [
  buildCourse("html", "HTML", "🌐", "#E34C26", "Die Sprache des Webs — von der ersten Seite bis Accessibility.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Was ist HTML? Dein erstes Dokument", "Überschriften, Absätze & Text-Formatierung", "Links & Bilder", "Listen (ul, ol, li)"] },
    { title: "Struktur", level: "intermediate", lessons: ["Tabellen erstellen", "Formulare & Inputs", "Semantisches HTML", "HTML5 Multimedia (video, audio)"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Meta-Tags & SEO-Basics", "Accessibility (ARIA, Alt-Texte)"] },
  ]),
  buildCourse("css", "CSS", "🎨", "#264DE4", "Style deinen Code — Layout, Animationen & modernes CSS.", [
    { title: "Grundlagen", level: "beginner", lessons: ["CSS einbinden, Selektoren & Eigenschaften", "Farben, Hintergründe & Schriften", "Das Box-Model", "Display & Visibility"] },
    { title: "Layout", level: "intermediate", lessons: ["Flexbox — Grundlagen", "Flexbox — Advanced", "CSS Grid", "Responsive Design & Media Queries"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["CSS Animationen & Transitions", "CSS Variablen & Custom Properties", "Pseudo-Klassen & Pseudo-Elemente", "Modern CSS (Nesting, Container Queries)"] },
  ]),
  buildCourse("javascript", "JavaScript", "⚡", "#F7C948", "Bring Leben ins Web — von Variablen bis Async/Await.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Variablen & Datentypen", "Datentypen & Operatoren", "Bedingungen (if/else, switch)", "Schleifen (for, while, forEach)", "Funktionen"] },
    { title: "Intermediate", level: "intermediate", lessons: ["Arrays & Array-Methoden", "Objekte & JSON", "DOM-Manipulation", "Events & Event-Listener", "Fehlerbehandlung (try/catch)"] },
    { title: "Advanced", level: "advanced", lessons: ["ES6+ Features", "Promises & Async/Await", "Fetch API & REST-Requests", "Klassen & OOP in JS", "Module (import/export)"] },
    { title: "Expert", level: "expert", lessons: ["Closures & Scope", "Prototypen & Vererbung", "Performance-Optimierung"] },
  ]),
  buildCourse("java", "Java", "☕", "#B07219", "Objektorientierung meistern — von Hello World bis Spring.", [
    { title: "Grundlagen", level: "beginner", lessons: ['Java Setup & "Hello World"', "Variablen & primitive Datentypen", "Operatoren & Ausdrücke", "Bedingungen & Schleifen", "Arrays anlegen und durchlaufen"] },
    { title: "OOP", level: "intermediate", lessons: ["Klassen & Objekte", "Konstruktoren & this", "Vererbung & super", "Interfaces & abstrakte Klassen", "Packages & Import"] },
    { title: "Advanced", level: "advanced", lessons: ["Collections (ArrayList, HashMap)", "Generics: typsichere Container", "Exception Handling", "File I/O", "Lambda & Streams"] },
    { title: "Expert", level: "expert", lessons: ["Multithreading & Concurrency", "Design Patterns", "Spring Framework Einführung"] },
  ]),
  buildCourse("python", "Python", "🐍", "#3572A5", "Der einfache Einstieg — bis hin zu Data Science.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Python installieren & Hello World", "Variablen & Datentypen", "Strings & String-Methoden", "Listen, Tupel, Sets", "Dictionaries"] },
    { title: "Intermediate", level: "intermediate", lessons: ["Funktionen & Parameter", "Module & pip", "Datei-Operationen", "OOP in Python", "List Comprehensions"] },
    { title: "Advanced", level: "advanced", lessons: ["Decorators: Funktionen umhüllen", "Generators & Iterators", "Fehlerbehandlung", "Virtual Environments", "APIs mit requests"] },
    { title: "Expert", level: "expert", lessons: ["Async Python (asyncio)", "Testing mit pytest", "Data Science Einführung (numpy/pandas)"] },
  ]),
  buildCourse("sql", "SQL", "🗄️", "#E38C00", "Datenbanken verstehen — Abfragen, Joins & Performance.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Was sind Datenbanken?", "SELECT & FROM", "WHERE & Bedingungen", "ORDER BY & LIMIT"] },
    { title: "Intermediate", level: "intermediate", lessons: ["INSERT, UPDATE, DELETE", "CREATE TABLE & Datentypen", "JOINs (INNER, LEFT, RIGHT)", "Aggregat-Funktionen", "GROUP BY & HAVING"] },
    { title: "Advanced", level: "advanced", lessons: ["Subqueries: Abfragen in Abfragen", "Indizes und ihre Wirkung auf die Laufzeit", "Transaktionen: alles oder nichts", "Views & Stored Procedures"] },
  ]),
  buildCourse("cpp", "C++", "⚙️", "#00599C", "Nah am Metal — Pointer, Templates & Systemnähe.", [
    { title: "Grundlagen", level: "beginner", lessons: ["C++ Grundstruktur & Kompilierung", "Variablen, Typen, Ein/Ausgabe", "Operatoren & Ausdrücke", "Kontrollstrukturen"] },
    { title: "Intermediate", level: "intermediate", lessons: ["Funktionen & Überladen", "Arrays & Strings", "Pointer & Referenzen", "Klassen & OOP", "Vererbung & Polymorphismus"] },
    { title: "Advanced", level: "advanced", lessons: ["Templates: Code für viele Typen", "STL (vector, map, algorithm)", "Speicherverwaltung (new/delete)", "Smart Pointer"] },
    { title: "Expert", level: "expert", lessons: ["Move Semantics & Rvalue", "Multithreading (std::thread)", "Systemnahe Programmierung"] },
  ]),
  buildCourse("c", "C", "🔩", "#5C6BC0", "Die Mutter aller Sprachen — Speicher, Pointer & Systemnähe.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Aufbau eines C-Programms & Kompilieren", "Variablen & Datentypen", "Operatoren & Ausdrücke", "Kontrollstrukturen", "Funktionen"] },
    { title: "Speicher & Daten", level: "intermediate", lessons: ["Arrays anlegen und durchlaufen", "Zeiger (Pointer) verstehen", "Strings in C", "Structs & Unions", "Dynamische Speicherverwaltung (malloc/free)"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Datei-Ein-/Ausgabe", "Präprozessor & Makros", "Modularisierung & Header", "Verkettete Listen"] },
    { title: "Expert", level: "expert", lessons: ["Bit-Operationen", "Funktionszeiger", "Systemnahe Programmierung & Syscalls"] },
  ]),
  buildCourse("typescript", "TypeScript", "🛡️", "#3178C6", "JavaScript mit Typsicherheit — weniger Bugs, besserer Code.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Was ist TypeScript? Setup & tsc", "Basistypen & Type Annotations", "Arrays, Tupel & Enums", "Funktionen typisieren"] },
    { title: "Typsystem", level: "intermediate", lessons: ["Interfaces & Type Aliases", "Union & Intersection Types", "Optional & Readonly", "Type Narrowing & Guards", "Klassen in TypeScript"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Generics: typsichere Container", "Utility Types (Partial, Pick, Omit)", "Module & Namespaces", "Typisierung von APIs"] },
    { title: "Expert", level: "expert", lessons: ["Conditional Types", "Mapped Types & Template Literal Types", "Declaration Files (.d.ts)"] },
  ]),
  buildCourse("react", "React", "⚛️", "#61DAFB", "Moderne Benutzeroberflächen mit Komponenten & Hooks.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Was ist React? Erste Komponente", "JSX verstehen", "Props & Komponenten-Komposition", "State mit useState", "Events behandeln"] },
    { title: "Hooks & Logik", level: "intermediate", lessons: ["Listen & Keys", "Bedingtes Rendering", "useEffect & Seiteneffekte", "Formulare & kontrollierte Inputs", "Eigene Hooks schreiben"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Context API: Daten ohne Prop-Drilling", "useReducer & komplexer State", "Performance (memo, useMemo, useCallback)", "Daten laden & Fehlerbehandlung"] },
    { title: "Expert", level: "expert", lessons: ["React Router & Navigation", "Testing mit React Testing Library", "Patterns & Architektur größerer Apps"] },
  ]),
  buildCourse("vue", "Vue", "💚", "#42B883", "Das progressive Framework — sanfter Einstieg, volle Power.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Vue einbinden & erste App", "Template-Syntax & Interpolation", "Direktiven (v-if, v-for, v-bind)", "Events mit v-on", "Reaktivität mit ref & reactive"] },
    { title: "Komponenten", level: "intermediate", lessons: ["Komponenten & Props", "Emits & Kommunikation", "Slots: Inhalte in Komponenten einsetzen", "Computed & Watch", "Formulare mit v-model"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Composition API vertiefen", "Lifecycle Hooks", "Vue Router", "State Management mit Pinia"] },
  ]),
  buildCourse("go", "Go", "🐹", "#00ADD8", "Einfach, schnell, nebenläufig — die Sprache der Cloud.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Go installieren & Hello World", "Variablen, Typen & Konstanten", "Kontrollstrukturen", "Funktionen & Mehrfachrückgabe", "Arrays, Slices & Maps"] },
    { title: "Strukturen", level: "intermediate", lessons: ["Structs & Methoden", "Interfaces", "Fehlerbehandlung mit error", "Packages & Module", "Zeiger in Go"] },
    { title: "Nebenläufigkeit", level: "advanced", lessons: ["Goroutines: nebenläufig ohne Threads", "Channels: Daten zwischen Goroutines", "select & sync", "Testing in Go"] },
    { title: "Expert", level: "expert", lessons: ["HTTP-Server bauen", "Context & Timeouts", "Performance & Profiling"] },
  ]),
  buildCourse("kotlin", "Kotlin", "🟣", "#7F52FF", "Modernes JVM — prägnant, sicher, Android-first.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Kotlin Setup & Hello World", "val, var & Datentypen", "Null-Sicherheit verstehen", "Kontrollfluss & when", "Funktionen & Default-Parameter"] },
    { title: "OOP & Funktional", level: "intermediate", lessons: ["Klassen & Konstruktoren", "Data Classes", "Vererbung & Interfaces", "Collections & Lambdas", "Extension Functions"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Sealed Classes & Pattern Matching", "Generics: typsichere Container", "Coroutines — Grundlagen", "Scope Functions (let, apply, run)"] },
    { title: "Expert", level: "expert", lessons: ["Coroutines & Flow vertiefen", "DSLs bauen", "Android-Grundlagen mit Kotlin"] },
  ]),
  buildCourse("rust", "Rust", "🦀", "#DEA584", "Sicher, schnell, ohne Garbage Collector.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Rust installieren & Cargo", "Variablen & Mutability", "Datentypen & Tupel", "Kontrollfluss", "Funktionen"] },
    { title: "Ownership", level: "intermediate", lessons: ["Ownership verstehen", "Borrowing und Referenzen verstehen", "Slices: Ausschnitte ohne Kopie", "Structs & Methoden", "Enums & Pattern Matching"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Fehlerbehandlung mit Result & Option", "Generics & Traits", "Lifetimes: wie lange Referenzen gelten", "Collections (Vec, HashMap)", "Module & Crates"] },
    { title: "Expert", level: "expert", lessons: ["Smart Pointer (Box, Rc, RefCell)", "Nebenläufigkeit & Threads", "Unsafe Rust & FFI"] },
  ]),
  buildCourse("php", "PHP", "🐘", "#777BB4", "Das Rückgrat des Webs — Server-Logik & Datenbanken.", [
    { title: "Grundlagen", level: "beginner", lessons: ["PHP einrichten & erste Ausgabe", "Variablen & Datentypen", "Operatoren & Strings", "Kontrollstrukturen", "Funktionen"] },
    { title: "Web & Daten", level: "intermediate", lessons: ["Arrays & Array-Funktionen", "Formulare verarbeiten (GET/POST)", "Sessions & Cookies", "Dateien lesen & schreiben", "Fehlerbehandlung"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["OOP in PHP", "Datenbanken mit PDO", "Sicherheit (SQL-Injection, XSS)", "Composer & Autoloading"] },
    { title: "Expert", level: "expert", lessons: ["REST-APIs bauen", "Laravel Einführung", "Performance & Caching"] },
  ]),
];

const TOTAL_LESSONS = COURSES.reduce((sum, c) => sum + c.totalLessons, 0);
const courseById = (id) => COURSES.find((c) => c.id === id);
const allLessonsOf = (course) => course.modules.flatMap((m) => m.lessons);
function findLessonMeta(lessonId) {
  for (const c of COURSES)
    for (const m of c.modules)
      for (const l of m.lessons)
        if (l.id === lessonId) return { course: c, module: m, lesson: l };
  return null;
}

/* ------------------------------ Konten --------------------------------- */
// 2,5 GB simuliertes Speicherkontingent pro Account für Playground-Projekte.
// Echtes localStorage fasst real nur wenige MB — dies ist eine UX-Anzeige/Obergrenze,
// keine tatsächliche Festplatten-Reservierung (dafür bräuchte es einen Server).
const STORAGE_QUOTA_BYTES = 2.5 * 1024 * 1024 * 1024;

// Es gibt keine vorgefertigten Konten. Wer die Plattform nutzt, legt sich ein
// eigenes an — in der Rangliste und in der Verwaltung tauchen ausschließlich
// echte Konten auf.

/* --------------------------- Lesson Content ---------------------------- */
const JS_LESSON_1_THEORY = `# Variablen in JavaScript

Variablen sind wie **Schachteln** — du legst Werte hinein und kannst später darauf zugreifen.

## Variablen erstellen

In modernem JavaScript gibt es zwei Hauptwege:

\`\`\`javascript
let name = "Alice";        // Kann später geändert werden
const alter = 17;          // Kann NICHT geändert werden
\`\`\`

> 💡 **Faustregel:** Nutze immer \`const\` — außer du weißt, dass der Wert sich ändern soll.

## Datentypen

JavaScript kennt diese grundlegenden Typen:

| Typ | Beispiel | Beschreibung |
|-----|----------|--------------|
| String | \`"Hallo"\` | Text in Anführungszeichen |
| Number | \`42\` oder \`3.14\` | Zahlen |
| Boolean | \`true\` / \`false\` | Ja oder Nein |
| null | \`null\` | Absichtlich leer |
| undefined | \`undefined\` | Noch kein Wert |

### Typ prüfen

Mit \`typeof\` kannst du den Typ ermitteln:

\`\`\`javascript
console.log(typeof "Hallo");  // "string"
console.log(typeof 42);       // "number"
console.log(typeof true);     // "boolean"
\`\`\``;

const BASE_LESSONS = {
  javascript_1_1: {
    estimatedMinutes: 15,
    theory: JS_LESSON_1_THEORY,
    tasks: [
      {
        id: "t1", type: "multiple_choice",
        question: "Was ist der Unterschied zwischen `let` und `const`?",
        options: ["Es gibt keinen Unterschied", "let kann geändert werden, const nicht", "const ist schneller als let", "let ist veraltet"],
        correctAnswer: 1,
        explanation: "let erlaubt es, den Wert später zu ändern. const erstellt eine Konstante — der Wert kann nach der Zuweisung nicht mehr geändert werden.",
        aiCheck: false,
      },
      {
        id: "t2", type: "multiple_choice",
        question: "Was gibt `typeof 3.14` zurück?",
        options: ["float", "decimal", "number", "integer"],
        correctAnswer: 2,
        explanation: "JavaScript kennt kein 'float' oder 'integer' — alle Zahlen sind vom Typ 'number'.",
        aiCheck: false,
      },
      {
        id: "t3", type: "code_write",
        question: "Erstelle eine Variable `lieblingsfarbe` und weise ihr einen Farbnamen als String zu. Nutze `const`.",
        starterCode: "// Schreib deinen Code hier:\n\n",
        expectedConcepts: ["const", "lieblingsfarbe", "string", "="],
        aiCheck: true,
      },
      {
        id: "t4", type: "fill_blank",
        question: "Fülle die Lücken aus:",
        template: "Um eine veränderliche Variable zu erstellen, nutze ___. Für unveränderliche Werte nutze ___.",
        blanks: ["let", "const"],
        aiCheck: false,
      },
      {
        id: "t5", type: "explain",
        question: "Erkläre in eigenen Worten: Warum ist es sinnvoll, lieber `const` als `let` zu verwenden, wenn der Wert sich nicht ändert?",
        aiCheck: true,
      },
    ],
  },
  html_1_1: {
    estimatedMinutes: 12,
    theory: `# Was ist HTML?

**HTML** (HyperText Markup Language) ist das **Skelett** jeder Webseite. Es beschreibt die *Struktur* — nicht das Aussehen (das macht CSS).

## Dein erstes Dokument

\`\`\`html
<!DOCTYPE html>
<html lang="de">
  <head>
    <title>Meine Seite</title>
  </head>
  <body>
    <h1>Hallo Welt!</h1>
  </body>
</html>
\`\`\`

> 💡 Jedes HTML-Element besteht aus einem **Start-Tag** \`<p>\`, Inhalt und einem **End-Tag** \`</p>\`.

## Die wichtigsten Bereiche

| Tag | Bedeutung |
|-----|-----------|
| \`<head>\` | Metadaten (unsichtbar) |
| \`<body>\` | Sichtbarer Inhalt |
| \`<h1>\` | Hauptüberschrift |`,
    tasks: [
      {
        id: "h1", type: "multiple_choice",
        question: "Wofür steht HTML?",
        options: ["HyperText Markup Language", "High Tech Modern Language", "Home Tool Markup Language", "Hyperlink Text Language"],
        correctAnswer: 0,
        explanation: "HTML steht für HyperText Markup Language — die Auszeichnungssprache des Webs.",
        aiCheck: false,
      },
      {
        id: "h2", type: "multiple_choice",
        question: "In welchem Bereich steht der sichtbare Inhalt?",
        options: ["<head>", "<title>", "<body>", "<meta>"],
        correctAnswer: 2,
        explanation: "Der sichtbare Inhalt der Seite gehört in den <body>.",
        aiCheck: false,
      },
      {
        id: "h3", type: "code_write",
        question: "Schreibe eine HTML-Überschrift erster Ordnung mit dem Text „Willkommen“.",
        starterCode: "<!-- Deine Überschrift hier -->\n",
        expectedConcepts: ["h1", "Willkommen", "</h1>"],
        aiCheck: true,
      },
      {
        id: "h4", type: "explain",
        question: "Erkläre kurz, warum man die Struktur (HTML) und das Aussehen (CSS) voneinander trennt.",
        aiCheck: true,
      },
    ],
  },
};

// Von Fachautoren erstellte Lektionsinhalte (Modul 1 aller Sprachen + JS-Vertiefung)
const EXTRA_LESSONS = {
  "cpp_1_1": {
    "estimatedMinutes": 15,
    "theory": "# C++ Grundstruktur & Kompilierung\n\nC++ ist eine **kompilierte** Sprache: Du schreibst Quellcode, ein Compiler übersetzt ihn in Maschinencode, und erst danach läuft das Programm. Das unterscheidet C++ von Sprachen wie Python, die direkt interpretiert werden.\n\n## Das kleinste lauffähige Programm\n\nJedes C++-Programm braucht eine Funktion namens `main`. Sie ist der **Einstiegspunkt** – hier startet die Ausführung.\n\n```cpp\n#include <iostream>\n\nint main() {\n    std::cout << \"Hallo Welt!\" << std::endl;\n    return 0;\n}\n```\n\n## Die Bestandteile\n\n| Zeile | Bedeutung |\n|-------|-----------|\n| `#include <iostream>` | bindet die Bibliothek für Ein-/Ausgabe ein |\n| `int main()` | die Hauptfunktion, gibt einen `int` zurück |\n| `std::cout` | gibt Text auf der Konsole aus |\n| `return 0;` | meldet dem Betriebssystem: alles ok |\n\nDie Zeile `#include` ist eine **Präprozessor-Direktive**. Sie wird vor dem eigentlichen Kompilieren verarbeitet und macht Funktionen wie `std::cout` verfügbar.\n\n## Kompilieren mit g++\n\nAuf der Kommandozeile übersetzt du den Code so:\n\n```cpp\n// im Terminal:\n// g++ hallo.cpp -o hallo\n// ./hallo\n```\n\nMit `-o hallo` legst du den Namen der ausführbaren Datei fest. Ohne `-o` heißt sie standardmäßig `a.out`.\n\n> 💡 Ein `return 0;` am Ende von `main` signalisiert Erfolg. Ein Wert ungleich 0 bedeutet üblicherweise, dass ein Fehler aufgetreten ist.",
    "tasks": [
      {
        "id": "c11_1",
        "type": "multiple_choice",
        "question": "Wie heißt die Funktion, in der jedes C++-Programm startet?",
        "options": [
          "start()",
          "main()",
          "begin()",
          "run()"
        ],
        "correctAnswer": 1,
        "explanation": "Die Funktion main() ist der festgelegte Einstiegspunkt jedes C++-Programms.",
        "aiCheck": false
      },
      {
        "id": "c11_2",
        "type": "multiple_choice",
        "question": "Was bewirkt die Zeile #include <iostream>?",
        "options": [
          "Sie startet das Programm sofort.",
          "Sie bindet die Bibliothek für Ein- und Ausgabe ein.",
          "Sie kompiliert den Code automatisch.",
          "Sie definiert die Funktion main()."
        ],
        "correctAnswer": 1,
        "explanation": "iostream stellt unter anderem std::cout und std::cin bereit.",
        "aiCheck": false
      },
      {
        "id": "c11_3",
        "type": "multiple_choice",
        "question": "Welcher g++-Befehl erzeugt eine ausführbare Datei mit dem Namen 'app'?",
        "options": [
          "g++ app.cpp",
          "g++ -run app.cpp",
          "g++ app.cpp -o app",
          "g++ app -o app.cpp"
        ],
        "correctAnswer": 2,
        "explanation": "Mit -o app legst du den Namen der Ausgabedatei fest. Ohne -o entsteht a.out.",
        "aiCheck": false
      },
      {
        "id": "c11_4",
        "type": "code_write",
        "question": "Schreibe ein vollständiges C++-Programm, das den Text 'Mein erstes Programm' auf der Konsole ausgibt. Vergiss den include, die main-Funktion und return 0 nicht.",
        "starterCode": "#include <iostream>\n\nint main() {\n    // dein Code hier\n    return 0;\n}\n",
        "expectedConcepts": [
          "#include <iostream>",
          "int main()",
          "std::cout",
          "return 0"
        ],
        "aiCheck": true
      },
      {
        "id": "c11_5",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Die Direktive ___ bindet eine Bibliothek ein, und am Ende von main steht ___ um Erfolg zu melden.",
        "blanks": [
          "#include",
          "return 0;"
        ],
        "aiCheck": false
      }
    ]
  },
  "cpp_1_2": {
    "estimatedMinutes": 15,
    "theory": "# Variablen, Typen, Ein- und Ausgabe\n\nEine **Variable** ist ein benannter Speicherplatz für einen Wert. In C++ musst du beim Anlegen immer den **Datentyp** angeben – die Sprache ist *statisch typisiert*.\n\n## Grundlegende Datentypen\n\n| Typ | Beispiel | Beschreibung |\n|-----|----------|--------------|\n| `int` | `int alter = 25;` | Ganzzahlen |\n| `double` | `double preis = 3.99;` | Kommazahlen |\n| `char` | `char buchstabe = 'A';` | einzelnes Zeichen |\n| `bool` | `bool aktiv = true;` | Wahrheitswert |\n| `std::string` | `std::string name = \"Lea\";` | Zeichenkette |\n\nFür `std::string` brauchst du zusätzlich `#include <string>`.\n\n## Ausgabe mit std::cout\n\nMit dem Operator `<<` schiebst du Werte in den Ausgabestrom:\n\n```cpp\n#include <iostream>\n#include <string>\n\nint main() {\n    std::string name = \"Lea\";\n    int alter = 25;\n    std::cout << \"Name: \" << name << \", Alter: \" << alter << std::endl;\n    return 0;\n}\n```\n\n## Eingabe mit std::cin\n\nMit `std::cin` und dem Operator `>>` liest du eine Eingabe vom Benutzer in eine Variable:\n\n```cpp\nint zahl;\nstd::cout << \"Gib eine Zahl ein: \";\nstd::cin >> zahl;\n```\n\nBeachte die **Richtung der Pfeile**: Bei der Ausgabe zeigen sie zu `cout` hin (`<<`), bei der Eingabe von `cin` weg in die Variable (`>>`).\n\n> 💡 `std::endl` fügt einen Zeilenumbruch ein und leert zusätzlich den Ausgabepuffer. Für reinen Umbruch ist auch `\"\\n\"` möglich und oft schneller.",
    "tasks": [
      {
        "id": "c12_1",
        "type": "multiple_choice",
        "question": "Welcher Datentyp eignet sich am besten für den Wert 3.99?",
        "options": [
          "int",
          "char",
          "double",
          "bool"
        ],
        "correctAnswer": 2,
        "explanation": "double speichert Kommazahlen. int könnte nur 3 speichern, der Nachkommateil ginge verloren.",
        "aiCheck": false
      },
      {
        "id": "c12_2",
        "type": "multiple_choice",
        "question": "Mit welchem Operator liest std::cin eine Eingabe in eine Variable?",
        "options": [
          "<<",
          ">>",
          "==",
          "->"
        ],
        "correctAnswer": 1,
        "explanation": "std::cin verwendet >> (Extraktion). std::cout verwendet << (Insertion).",
        "aiCheck": false
      },
      {
        "id": "c12_3",
        "type": "multiple_choice",
        "question": "Welche Anweisung deklariert korrekt eine Zeichenkette?",
        "options": [
          "string name = Lea;",
          "std::string name = \"Lea\";",
          "int name = \"Lea\";",
          "char name = \"Lea\";"
        ],
        "correctAnswer": 1,
        "explanation": "Ein std::string-Literal steht in doppelten Anführungszeichen. Einzelne Zeichen mit char nutzen einfache Anführungszeichen.",
        "aiCheck": false
      },
      {
        "id": "c12_4",
        "type": "code_write",
        "question": "Deklariere eine int-Variable 'alter' mit dem Wert 30 und eine std::string-Variable 'name' mit dem Wert 'Tom'. Gib danach beide mit std::cout in einer Zeile aus.",
        "starterCode": "#include <iostream>\n#include <string>\n\nint main() {\n    // dein Code hier\n    return 0;\n}\n",
        "expectedConcepts": [
          "int alter",
          "std::string",
          "std::cout",
          "<<"
        ],
        "aiCheck": true
      },
      {
        "id": "c12_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen std::cout und std::cin und welche Pfeil-Operatoren jeweils dazugehören.",
        "aiCheck": true
      }
    ]
  },
  "cpp_1_3": {
    "estimatedMinutes": 15,
    "theory": "# Operatoren & Ausdrücke\n\nEin **Ausdruck** kombiniert Werte und Variablen mit **Operatoren** zu einem neuen Wert. C++ kennt viele Kategorien von Operatoren.\n\n## Arithmetische Operatoren\n\n| Operator | Bedeutung | Beispiel | Ergebnis |\n|----------|-----------|----------|----------|\n| `+` | Addition | `3 + 4` | `7` |\n| `-` | Subtraktion | `5 - 2` | `3` |\n| `*` | Multiplikation | `6 * 2` | `12` |\n| `/` | Division | `7 / 2` | `3` |\n| `%` | Rest (Modulo) | `7 % 2` | `1` |\n\nAchtung: Bei zwei `int`-Operanden ist `/` eine **Ganzzahldivision** – `7 / 2` ergibt `3`, nicht `3.5`. Erst wenn ein `double` beteiligt ist (z. B. `7.0 / 2`), erhältst du `3.5`.\n\n## Vergleichs- und logische Operatoren\n\n```cpp\nbool a = (5 > 3);      // true\nbool b = (4 == 4);     // true, == prüft Gleichheit\nbool c = (a && b);     // true, logisches UND\nbool d = (a || false); // true, logisches ODER\nbool e = !a;           // false, Negation\n```\n\nVerwechsle `=` (Zuweisung) nicht mit `==` (Vergleich)!\n\n## Kurzschreibweisen\n\n```cpp\nint x = 10;\nx += 5;   // x = x + 5  -> 15\nx++;      // x = x + 1  -> 16\nx *= 2;   // x = x * 2  -> 32\n```\n\n> 💡 Der Modulo-Operator `%` ist sehr nützlich, um zu prüfen, ob eine Zahl gerade ist: `zahl % 2 == 0` ist genau dann `true`, wenn `zahl` gerade ist.",
    "tasks": [
      {
        "id": "c13_1",
        "type": "multiple_choice",
        "question": "Was ergibt der Ausdruck 7 / 2, wenn beide Werte vom Typ int sind?",
        "options": [
          "3.5",
          "3",
          "4",
          "1"
        ],
        "correctAnswer": 1,
        "explanation": "Die Division zweier int-Werte ist eine Ganzzahldivision: der Nachkommateil wird abgeschnitten, das Ergebnis ist 3.",
        "aiCheck": false
      },
      {
        "id": "c13_2",
        "type": "multiple_choice",
        "question": "Was ergibt 7 % 2?",
        "options": [
          "0",
          "1",
          "3",
          "3.5"
        ],
        "correctAnswer": 1,
        "explanation": "Der Modulo-Operator liefert den Rest der Division. 7 geteilt durch 2 ist 3 Rest 1.",
        "aiCheck": false
      },
      {
        "id": "c13_3",
        "type": "multiple_choice",
        "question": "Welcher Operator prüft, ob zwei Werte gleich sind?",
        "options": [
          "=",
          "==",
          "!=",
          "=>"
        ],
        "correctAnswer": 1,
        "explanation": "== ist der Vergleichsoperator. Ein einzelnes = ist die Zuweisung und ein häufiger Anfängerfehler.",
        "aiCheck": false
      },
      {
        "id": "c13_4",
        "type": "code_write",
        "question": "Lies zwei int-Zahlen mit std::cin ein und gib ihre Summe sowie ihren Rest bei Division (Modulo) mit std::cout aus.",
        "starterCode": "#include <iostream>\n\nint main() {\n    int a, b;\n    // einlesen und ausgeben\n    return 0;\n}\n",
        "expectedConcepts": [
          "std::cin",
          "+",
          "%",
          "std::cout"
        ],
        "aiCheck": true
      },
      {
        "id": "c13_5",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Der Operator ___ liefert den Rest einer Division, und ___ ist die Kurzschreibweise für x = x + 1.",
        "blanks": [
          "%",
          "x++"
        ],
        "aiCheck": false
      }
    ]
  },
  "cpp_1_4": {
    "estimatedMinutes": 15,
    "theory": "# Kontrollstrukturen\n\nMit **Kontrollstrukturen** steuerst du, *welcher* Code ausgeführt wird und *wie oft*. Ohne sie liefe ein Programm einfach von oben nach unten durch.\n\n## Verzweigung mit if / else\n\n```cpp\nint alter = 17;\nif (alter >= 18) {\n    std::cout << \"volljaehrig\";\n} else {\n    std::cout << \"minderjaehrig\";\n}\n```\n\nDie **Bedingung** in den runden Klammern muss einen Wahrheitswert ergeben. Ist sie `true`, läuft der `if`-Block, sonst der `else`-Block. Für mehrere Fälle hängst du `else if` an.\n\n## Die while-Schleife\n\nEine `while`-Schleife wiederholt ihren Block, **solange** die Bedingung `true` ist:\n\n```cpp\nint i = 0;\nwhile (i < 3) {\n    std::cout << i << \" \";\n    i++;            // ohne diese Zeile: Endlosschleife!\n}\n// Ausgabe: 0 1 2\n```\n\n## Die for-Schleife\n\nDie `for`-Schleife bündelt Start, Bedingung und Schritt in einer Zeile:\n\n```cpp\nfor (int i = 0; i < 3; i++) {\n    std::cout << i << \" \";\n}\n// Ausgabe: 0 1 2\n```\n\n| Teil | Beispiel | Wann läuft er? |\n|------|----------|----------------|\n| Initialisierung | `int i = 0` | einmal am Anfang |\n| Bedingung | `i < 3` | vor jedem Durchlauf |\n| Schritt | `i++` | nach jedem Durchlauf |\n\n> 💡 Nutze `for`, wenn die Anzahl der Wiederholungen bekannt ist, und `while`, wenn sie von einer Bedingung abhängt, deren Ende du noch nicht kennst.",
    "tasks": [
      {
        "id": "c14_1",
        "type": "multiple_choice",
        "question": "Wie oft wird der Block dieser Schleife ausgeführt: for (int i = 0; i < 3; i++)?",
        "options": [
          "2 mal",
          "3 mal",
          "4 mal",
          "unendlich oft"
        ],
        "correctAnswer": 1,
        "explanation": "i nimmt die Werte 0, 1 und 2 an. Bei i == 3 ist die Bedingung false, daher 3 Durchläufe.",
        "aiCheck": false
      },
      {
        "id": "c14_2",
        "type": "multiple_choice",
        "question": "Was passiert, wenn man in einer while-Schleife die Zählvariable nie verändert?",
        "options": [
          "Die Schleife läuft genau einmal.",
          "Der Compiler meldet einen Fehler.",
          "Es entsteht eine Endlosschleife.",
          "Die Schleife wird übersprungen."
        ],
        "correctAnswer": 2,
        "explanation": "Bleibt die Bedingung dauerhaft true, wird der Block endlos wiederholt - eine Endlosschleife.",
        "aiCheck": false
      },
      {
        "id": "c14_3",
        "type": "multiple_choice",
        "question": "Welcher Block läuft, wenn die if-Bedingung false ergibt?",
        "options": [
          "der if-Block",
          "der else-Block",
          "beide Blöcke",
          "kein Block je wieder"
        ],
        "correctAnswer": 1,
        "explanation": "Ist die Bedingung false, wird der else-Block ausgefuehrt (sofern vorhanden).",
        "aiCheck": false
      },
      {
        "id": "c14_4",
        "type": "code_write",
        "question": "Schreibe eine for-Schleife, die die Zahlen von 1 bis 5 (einschließlich) mit std::cout ausgibt.",
        "starterCode": "#include <iostream>\n\nint main() {\n    // for-Schleife hier\n    return 0;\n}\n",
        "expectedConcepts": [
          "for",
          "std::cout",
          "i <= 5",
          "i++"
        ],
        "aiCheck": true
      },
      {
        "id": "c14_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, wann du eine for-Schleife und wann du eine while-Schleife verwenden würdest.",
        "aiCheck": true
      }
    ]
  },
  "css_1_1": {
    "estimatedMinutes": 10,
    "theory": "# CSS einbinden, Selektoren & Eigenschaften\n\n**CSS** (Cascading Style Sheets) bestimmt, wie HTML-Elemente aussehen. Es gibt drei Wege, CSS einzubinden:\n\n## Die drei Einbindungsarten\n\n| Methode | Wo? | Beispiel |\n| --- | --- | --- |\n| **inline** | direkt am Element | `<p style=\"color: red;\">` |\n| **internal** | im `<style>`-Tag im `<head>` | `<style> p { color: red; } </style>` |\n| **external** | eigene `.css`-Datei | `<link rel=\"stylesheet\" href=\"style.css\">` |\n\nDie **externe** Variante ist meist die beste Wahl: Sie trennt Inhalt (HTML) von Gestaltung (CSS) und lässt sich auf mehreren Seiten wiederverwenden.\n\n## Selektoren & Eigenschaften\n\nEine CSS-Regel besteht aus einem **Selektor** und einem **Deklarationsblock** mit `Eigenschaft: Wert;`-Paaren.\n\n```css\n/* Element-Selektor: alle <p> */\np {\n  color: navy;\n  font-size: 16px;\n}\n\n/* Klassen-Selektor: alle Elemente mit class=\"hinweis\" */\n.hinweis {\n  background-color: yellow;\n}\n\n/* ID-Selektor: das Element mit id=\"header\" */\n#header {\n  text-align: center;\n}\n```\n\n- **Element-Selektor**: `p`, `h1`, `div` – wählt nach Tag-Name.\n- **Klassen-Selektor**: beginnt mit `.` – wiederverwendbar, mehrfach pro Seite.\n- **ID-Selektor**: beginnt mit `#` – sollte **nur einmal** pro Seite vorkommen.\n\n> 💡 Eine `class` darfst du beliebig oft verwenden, eine `id` muss eindeutig sein. Klassen sind im Alltag deutlich häufiger.",
    "tasks": [
      {
        "id": "c11_1",
        "type": "multiple_choice",
        "question": "Mit welchem Zeichen beginnt ein Klassen-Selektor in CSS?",
        "options": [
          "#",
          ".",
          "@",
          "*"
        ],
        "correctAnswer": 1,
        "explanation": "Klassen-Selektoren beginnen mit einem Punkt (.), ID-Selektoren mit einer Raute (#).",
        "aiCheck": false
      },
      {
        "id": "c11_2",
        "type": "multiple_choice",
        "question": "Welche Einbindungsart trennt Inhalt und Gestaltung am besten und ist wiederverwendbar?",
        "options": [
          "inline über das style-Attribut",
          "internal im style-Tag",
          "external über eine .css-Datei",
          "Es gibt keinen Unterschied"
        ],
        "correctAnswer": 2,
        "explanation": "Externe CSS-Dateien werden per <link> eingebunden, trennen HTML von CSS und lassen sich auf vielen Seiten nutzen.",
        "aiCheck": false
      },
      {
        "id": "c11_3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Ein ID-Selektor beginnt mit ___ und darf pro Seite nur ___ vorkommen.",
        "blanks": [
          "#",
          ["einmal", "1", "einmal pro Seite", "genau einmal", "ein Mal"]
        ],
        "aiCheck": false
      },
      {
        "id": "c11_4",
        "type": "code_write",
        "question": "Schreibe eine CSS-Regel, die allen <p>-Elementen die Schriftfarbe blau (color) gibt.",
        "starterCode": "/* Element-Selektor fuer p */\n",
        "expectedConcepts": [
          "p",
          "color",
          "blue",
          "{"
        ],
        "aiCheck": true
      },
      {
        "id": "c11_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen einem Klassen-Selektor und einem ID-Selektor.",
        "aiCheck": true
      }
    ]
  },
  "css_1_2": {
    "estimatedMinutes": 11,
    "theory": "# Farben, Hintergründe & Schriften\n\nCSS bietet viele Möglichkeiten, Farben und Schriften festzulegen. Das macht eine Seite lesbar und ansprechend.\n\n## Farben angeben\n\nFarben lassen sich auf mehrere Arten definieren:\n\n| Format | Beispiel | Bedeutung |\n| --- | --- | --- |\n| **Name** | `red` | benannte Farbe |\n| **Hex** | `#ff0000` | Rot-Grün-Blau als Hexadezimal |\n| **RGB** | `rgb(255, 0, 0)` | Rot-Grün-Blau 0–255 |\n| **RGBA** | `rgba(255, 0, 0, 0.5)` | RGB + Transparenz (Alpha) |\n\nBei Hex-Werten stehen je zwei Zeichen für Rot, Grün und Blau: `#ff0000` ist reines Rot.\n\n## Hintergründe & Schriften\n\n```css\nbody {\n  background-color: #f0f0f0;\n  color: #333333;\n  font-family: Arial, sans-serif;\n  font-size: 16px;\n}\n\nh1 {\n  color: rgb(0, 102, 204);\n  font-size: 2rem;\n}\n```\n\n- `color` setzt die **Textfarbe**.\n- `background-color` setzt die **Hintergrundfarbe**.\n- `font-family` legt die **Schriftart** fest; mit Kommas gibst du Alternativen an (Fallback).\n- `font-size` bestimmt die **Schriftgröße**, z. B. in `px` oder `rem`.\n\n> 💡 Gib bei `font-family` immer eine allgemeine Familie wie `sans-serif` als letzte Option an. Falls keine der vorderen Schriften verfügbar ist, nutzt der Browser eine passende Standardschrift.",
    "tasks": [
      {
        "id": "c12_1",
        "type": "multiple_choice",
        "question": "Welcher Hex-Wert steht für reines Rot?",
        "options": [
          "#00ff00",
          "#0000ff",
          "#ff0000",
          "#ffffff"
        ],
        "correctAnswer": 2,
        "explanation": "Bei Hex stehen die ersten beiden Stellen für Rot. #ff0000 bedeutet maximaler Rotanteil, kein Grün, kein Blau.",
        "aiCheck": false
      },
      {
        "id": "c12_2",
        "type": "multiple_choice",
        "question": "Wofür ist die vierte Zahl bei rgba(255, 0, 0, 0.5) zuständig?",
        "options": [
          "für die Helligkeit",
          "für die Transparenz (Alpha)",
          "für den Grauwert",
          "für die Schriftgröße"
        ],
        "correctAnswer": 1,
        "explanation": "Der vierte Wert (Alpha) steuert die Deckkraft: 0 ist komplett durchsichtig, 1 ist komplett deckend.",
        "aiCheck": false
      },
      {
        "id": "c12_3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Die Eigenschaft ___ setzt die Textfarbe, ___ setzt die Hintergrundfarbe.",
        "blanks": [
          "color",
          "background-color"
        ],
        "aiCheck": false
      },
      {
        "id": "c12_4",
        "type": "code_write",
        "question": "Schreibe eine CSS-Regel für body mit hellgrauem Hintergrund (#f0f0f0) und der Schriftart Arial mit sans-serif als Fallback.",
        "starterCode": "body {\n  /* dein Code */\n}\n",
        "expectedConcepts": [
          "background-color",
          "#f0f0f0",
          "font-family",
          "sans-serif"
        ],
        "aiCheck": true
      },
      {
        "id": "c12_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, warum man bei font-family mehrere Schriften durch Kommas getrennt angibt.",
        "aiCheck": true
      }
    ]
  },
  "css_1_3": {
    "estimatedMinutes": 12,
    "theory": "# Das Box-Model\n\nJedes HTML-Element ist im Layout eine rechteckige **Box**. Das **Box-Model** beschreibt, aus welchen Schichten diese Box besteht – von innen nach außen.\n\n## Die vier Schichten\n\n| Schicht | Eigenschaft | Bedeutung |\n| --- | --- | --- |\n| **Content** | `width`, `height` | der eigentliche Inhalt |\n| **Padding** | `padding` | Innenabstand zum Rand |\n| **Border** | `border` | der Rahmen |\n| **Margin** | `margin` | Außenabstand zu anderen Elementen |\n\n```css\n.box {\n  width: 200px;\n  padding: 20px;\n  border: 2px solid black;\n  margin: 10px;\n}\n```\n\n## box-sizing\n\nStandardmäßig (`box-sizing: content-box`) addiert der Browser Padding und Border **zur** `width` dazu. Die Box oben ist also tatsächlich 200 + 2·20 + 2·2 = **244px** breit.\n\nMit `box-sizing: border-box` zählen Padding und Border **in** die angegebene Breite hinein – die Box bleibt 200px breit:\n\n```css\n* {\n  box-sizing: border-box;\n}\n```\n\n> 💡 Viele Entwickler setzen `box-sizing: border-box` global auf alle Elementen (`*`). Das macht Größen viel leichter berechenbar, weil `width` dann die komplette sichtbare Breite ist.\n\nDu kannst Abstände auch einzeln angeben, z. B. `margin-top`, `padding-left`. Bei `margin: 10px 20px;` gilt der erste Wert für oben/unten, der zweite für links/rechts.",
    "tasks": [
      {
        "id": "c13_1",
        "type": "multiple_choice",
        "question": "Welche Eigenschaft erzeugt den Innenabstand zwischen Inhalt und Rahmen?",
        "options": [
          "margin",
          "padding",
          "border",
          "spacing"
        ],
        "correctAnswer": 1,
        "explanation": "padding ist der Innenabstand. margin ist der Außenabstand zu anderen Elementen.",
        "aiCheck": false
      },
      {
        "id": "c13_2",
        "type": "multiple_choice",
        "question": "Eine Box hat width: 200px, padding: 20px und border: 2px solid black bei box-sizing: content-box. Wie breit ist sie sichtbar?",
        "options": [
          "200px",
          "222px",
          "244px",
          "240px"
        ],
        "correctAnswer": 2,
        "explanation": "Bei content-box gilt: 200 + 2*20 (padding) + 2*2 (border) = 244px.",
        "aiCheck": false
      },
      {
        "id": "c13_3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Der Außenabstand heißt ___, der Rahmen heißt ___.",
        "blanks": [
          "margin",
          "border"
        ],
        "aiCheck": false
      },
      {
        "id": "c13_4",
        "type": "code_write",
        "question": "Schreibe eine Regel für die Klasse .karte mit 16px Innenabstand (padding) und einem 1px breiten, durchgezogenen, grauen Rahmen (border).",
        "starterCode": ".karte {\n  /* dein Code */\n}\n",
        "expectedConcepts": [
          "padding",
          "16px",
          "border",
          "solid"
        ],
        "aiCheck": true
      },
      {
        "id": "c13_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, was box-sizing: border-box bewirkt und warum es praktisch ist.",
        "aiCheck": true
      }
    ]
  },
  "css_1_4": {
    "estimatedMinutes": 10,
    "theory": "# Display & Visibility\n\nDie Eigenschaft `display` bestimmt, **wie** ein Element im Layout fließt. Sie ist eine der wichtigsten Eigenschaften in CSS.\n\n## Die wichtigsten display-Werte\n\n| Wert | Verhalten |\n| --- | --- |\n| `block` | nimmt die volle Breite ein, beginnt in neuer Zeile (z. B. `<div>`, `<p>`) |\n| `inline` | fließt im Text, `width`/`height` wirken **nicht** (z. B. `<span>`, `<a>`) |\n| `inline-block` | fließt wie inline, akzeptiert aber `width`/`height` |\n| `none` | Element wird **komplett entfernt**, nimmt keinen Platz ein |\n\n```css\n.menue-eintrag {\n  display: inline-block;\n  width: 120px;\n  padding: 8px;\n}\n\n.versteckt {\n  display: none;\n}\n```\n\n## display: none vs. visibility: hidden\n\nBeide verstecken ein Element, aber unterschiedlich:\n\n```css\n.weg   { display: none; }       /* Platz verschwindet komplett */\n.unsichtbar { visibility: hidden; } /* Platz bleibt leer reserviert */\n```\n\n- `display: none` entfernt das Element aus dem Layout – es hinterlässt **keine Lücke**.\n- `visibility: hidden` macht das Element unsichtbar, der **Platz bleibt** aber erhalten.\n\n> 💡 Merke: `display: none` ist wie \"gibt es nicht\", `visibility: hidden` ist wie \"ist da, aber durchsichtig\". Für ein- und ausklappbare Menüs nutzt man meist `display: none`.",
    "tasks": [
      {
        "id": "c14_1",
        "type": "multiple_choice",
        "question": "Welcher display-Wert lässt ein Element im Text fließen UND akzeptiert width/height?",
        "options": [
          "block",
          "inline",
          "inline-block",
          "none"
        ],
        "correctAnswer": 2,
        "explanation": "inline-block kombiniert beides: Es fließt wie inline, respektiert aber width und height.",
        "aiCheck": false
      },
      {
        "id": "c14_2",
        "type": "multiple_choice",
        "question": "Was ist der Unterschied zwischen display: none und visibility: hidden?",
        "options": [
          "Es gibt keinen Unterschied",
          "display: none entfernt den Platz, visibility: hidden behält ihn",
          "visibility: hidden entfernt den Platz, display: none behält ihn",
          "Beide löschen das Element aus dem HTML"
        ],
        "correctAnswer": 1,
        "explanation": "display: none nimmt keinen Platz mehr ein, während visibility: hidden den reservierten Platz als leere Lücke behält.",
        "aiCheck": false
      },
      {
        "id": "c14_3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Ein ___-Element nimmt die volle Breite ein, ein ___-Element fließt im Text.",
        "blanks": [
          "block",
          "inline"
        ],
        "aiCheck": false
      },
      {
        "id": "c14_4",
        "type": "code_write",
        "question": "Schreibe eine Regel für die Klasse .versteckt, die das Element komplett aus dem Layout entfernt (kein Platz reserviert).",
        "starterCode": ".versteckt {\n  /* dein Code */\n}\n",
        "expectedConcepts": [
          "display",
          "none"
        ],
        "aiCheck": true
      },
      {
        "id": "c14_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen einem block- und einem inline-Element.",
        "aiCheck": true
      }
    ]
  },
  "html_1_2": {
    "estimatedMinutes": 12,
    "theory": "# Überschriften, Absätze & Text-Formatierung\n\nIn dieser Lektion lernst du, wie du Text in HTML strukturierst. Das sind die wichtigsten Bausteine fast jeder Webseite.\n\n## Überschriften (h1 bis h6)\n\nHTML kennt **sechs Überschriftenebenen**: von `<h1>` (die wichtigste) bis `<h6>` (die unwichtigste). Pro Seite solltest du nur **eine** `<h1>` verwenden, denn sie beschreibt das Hauptthema.\n\n| Tag    | Bedeutung            | Größe (Standard) |\n|--------|----------------------|------------------|\n| `h1`   | Hauptüberschrift     | sehr groß        |\n| `h2`   | Unterüberschrift     | groß             |\n| `h3`   | Abschnitts­titel      | mittel           |\n| `h6`   | kleinste Überschrift | klein            |\n\n## Absätze und Zeilenumbrüche\n\nFür Fließtext nutzt du den Absatz `<p>`. Ein Browser fügt vor und nach einem Absatz automatisch etwas Abstand ein. Brauchst du nur einen Zeilenumbruch ohne Abstand, verwendest du `<br>` – ein leeres Element ohne schließenden Tag.\n\n## Text hervorheben\n\n- `<strong>` macht Text **wichtig** (meist fett dargestellt).\n- `<em>` betont Text *(meist kursiv dargestellt)*.\n\n```html\n<h1>Mein Blog</h1>\n<h2>Erster Beitrag</h2>\n<p>Das ist <strong>sehr wichtig</strong>.<br>\nUnd das ist <em>betont</em>.</p>\n```\n\n> 💡 Tipp: Wähle Überschriften nach ihrer **Bedeutung**, nicht nach der Größe! Für reine Optik benutzt du später CSS.",
    "tasks": [
      {
        "id": "h12_1",
        "type": "multiple_choice",
        "question": "Welches Tag steht für die wichtigste Überschrift einer Seite?",
        "options": [
          "<h6>",
          "<head>",
          "<h1>",
          "<p>"
        ],
        "correctAnswer": 2,
        "explanation": "<h1> ist die ranghöchste Überschrift und sollte pro Seite nur einmal vorkommen.",
        "aiCheck": false
      },
      {
        "id": "h12_2",
        "type": "multiple_choice",
        "question": "Womit erzeugst du einen einfachen Zeilenumbruch ohne zusätzlichen Abstand?",
        "options": [
          "<p>",
          "<br>",
          "<em>",
          "<h2>"
        ],
        "correctAnswer": 1,
        "explanation": "<br> ist ein leeres Element und erzeugt nur einen Zeilenumbruch, während <p> einen ganzen Absatz mit Abstand bildet.",
        "aiCheck": false
      },
      {
        "id": "h12_3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Mit ___ markierst du wichtigen Text und mit ___ betonst du Text.",
        "blanks": [
          "strong",
          "em"
        ],
        "aiCheck": false
      },
      {
        "id": "h12_4",
        "type": "code_write",
        "question": "Schreibe eine Hauptüberschrift mit dem Text \"Willkommen\" und darunter einen Absatz, in dem das Wort \"toll\" mit strong hervorgehoben ist.",
        "starterCode": "<!-- Deine Überschrift und dein Absatz hier -->\n",
        "expectedConcepts": [
          "<h1>",
          "</h1>",
          "<p>",
          "</p>",
          "<strong>",
          "</strong>"
        ],
        "aiCheck": true
      },
      {
        "id": "h12_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen <strong> und <em>.",
        "aiCheck": true
      }
    ]
  },
  "html_1_3": {
    "estimatedMinutes": 12,
    "theory": "# Links & Bilder\n\nDas Web lebt von Verlinkungen und Bildern. In dieser Lektion lernst du, wie du beides in HTML einbindest.\n\n## Links mit dem a-Element\n\nEin Link (Hyperlink) wird mit dem `<a>`-Tag erstellt. Das wichtigste Attribut ist `href` (steht für *hypertext reference*) – es gibt das Ziel des Links an.\n\n```html\n<a href=\"https://example.com\">Zu example.com</a>\n```\n\nMit dem Attribut `target=\"_blank\"` öffnet sich der Link in einem **neuen Tab**:\n\n```html\n<a href=\"https://example.com\" target=\"_blank\">In neuem Tab öffnen</a>\n```\n\n## Bilder mit dem img-Element\n\nBilder bindest du mit `<img>` ein. Es ist ein leeres Element (kein schließender Tag) und hat zwei wichtige Attribute:\n\n| Attribut | Bedeutung                                            |\n|----------|------------------------------------------------------|\n| `src`    | Quelle (Pfad oder URL) des Bildes                    |\n| `alt`    | Alternativtext, falls das Bild nicht lädt            |\n\n```html\n<img src=\"katze.jpg\" alt=\"Eine schlafende Katze\">\n```\n\nDer `alt`-Text ist sehr wichtig: Screenreader lesen ihn vor, und er erscheint, wenn das Bild nicht geladen werden kann.\n\n> 💡 Tipp: Vergiss niemals das `alt`-Attribut! Es macht deine Seite **barrierefrei** und hilft auch Suchmaschinen, das Bild zu verstehen.",
    "tasks": [
      {
        "id": "h13_1",
        "type": "multiple_choice",
        "question": "Welches Attribut gibt das Ziel eines Links an?",
        "options": [
          "src",
          "alt",
          "href",
          "target"
        ],
        "correctAnswer": 2,
        "explanation": "Das href-Attribut des <a>-Tags enthält die Adresse, zu der der Link führt.",
        "aiCheck": false
      },
      {
        "id": "h13_2",
        "type": "multiple_choice",
        "question": "Wofür ist das alt-Attribut bei <img> da?",
        "options": [
          "Es gibt die Bildbreite an.",
          "Es liefert einen Alternativtext, wenn das Bild nicht lädt.",
          "Es verlinkt das Bild.",
          "Es bestimmt die Bildfarbe."
        ],
        "correctAnswer": 1,
        "explanation": "alt beschreibt das Bild als Text – wichtig für Barrierefreiheit und falls das Bild nicht geladen werden kann.",
        "aiCheck": false
      },
      {
        "id": "h13_3",
        "type": "multiple_choice",
        "question": "Wie öffnest du einen Link in einem neuen Tab?",
        "options": [
          "Mit href=\"_blank\"",
          "Mit target=\"_blank\"",
          "Mit new=\"tab\"",
          "Mit open=\"new\""
        ],
        "correctAnswer": 1,
        "explanation": "Das Attribut target=\"_blank\" sorgt dafür, dass der Link in einem neuen Tab geöffnet wird.",
        "aiCheck": false
      },
      {
        "id": "h13_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Das <img>-Tag braucht ___ für die Bildquelle und ___ für den Alternativtext.",
        "blanks": [
          "src",
          "alt"
        ],
        "aiCheck": false
      },
      {
        "id": "h13_5",
        "type": "code_write",
        "question": "Erstelle einen Link zu \"https://wikipedia.org\" mit dem Text \"Wikipedia\" und binde darunter ein Bild \"logo.png\" mit dem Alternativtext \"Logo\" ein.",
        "starterCode": "<!-- Dein Link und dein Bild hier -->\n",
        "expectedConcepts": [
          "<a href=",
          "</a>",
          "<img",
          "src=",
          "alt="
        ],
        "aiCheck": true
      }
    ]
  },
  "html_1_4": {
    "estimatedMinutes": 12,
    "theory": "# Listen (ul, ol, li)\n\nListen helfen dir, Inhalte übersichtlich aufzuzählen. HTML kennt zwei Haupttypen von Listen.\n\n## Ungeordnete Listen\n\nEine **ungeordnete** Liste (`<ul>` für *unordered list*) zeigt Punkte ohne feste Reihenfolge – meist mit Aufzählungszeichen (Bullets). Jeder Eintrag steht in einem `<li>` (*list item*).\n\n```html\n<ul>\n  <li>Äpfel</li>\n  <li>Bananen</li>\n  <li>Kirschen</li>\n</ul>\n```\n\n## Geordnete Listen\n\nEine **geordnete** Liste (`<ol>` für *ordered list*) nummeriert die Einträge automatisch. Auch hier nutzt du `<li>` für jeden Punkt.\n\n```html\n<ol>\n  <li>Wasser kochen</li>\n  <li>Teebeutel einlegen</li>\n  <li>Genießen</li>\n</ol>\n```\n\n| Tag   | Bedeutung           | Darstellung      |\n|-------|---------------------|------------------|\n| `ul`  | ungeordnete Liste   | Punkte (•)       |\n| `ol`  | geordnete Liste     | Zahlen (1, 2, 3) |\n| `li`  | Listeneintrag       | ein Punkt        |\n\n## Verschachtelte Listen\n\nDu kannst eine Liste **in** einem `<li>` einer anderen Liste platzieren:\n\n```html\n<ul>\n  <li>Obst\n    <ul>\n      <li>Apfel</li>\n    </ul>\n  </li>\n</ul>\n```\n\n> 💡 Tipp: Nimm `<ol>`, wenn die **Reihenfolge zählt** (z. B. ein Rezept), und `<ul>`, wenn sie egal ist.",
    "tasks": [
      {
        "id": "h14_1",
        "type": "multiple_choice",
        "question": "Welches Tag erzeugt eine nummerierte (geordnete) Liste?",
        "options": [
          "<ul>",
          "<ol>",
          "<li>",
          "<list>"
        ],
        "correctAnswer": 1,
        "explanation": "<ol> steht für 'ordered list' und nummeriert die Einträge automatisch.",
        "aiCheck": false
      },
      {
        "id": "h14_2",
        "type": "multiple_choice",
        "question": "In welches Tag schreibst du einen einzelnen Listeneintrag?",
        "options": [
          "<li>",
          "<item>",
          "<ul>",
          "<p>"
        ],
        "correctAnswer": 0,
        "explanation": "Jeder Eintrag einer Liste steht in einem <li>-Element (list item).",
        "aiCheck": false
      },
      {
        "id": "h14_3",
        "type": "multiple_choice",
        "question": "Wann ist eine <ol> sinnvoller als eine <ul>?",
        "options": [
          "Wenn die Reihenfolge der Einträge wichtig ist.",
          "Wenn die Liste kurz ist.",
          "Wenn man Bilder einfügen will.",
          "Niemals, beide sind identisch."
        ],
        "correctAnswer": 0,
        "explanation": "Eine geordnete Liste (<ol>) zeigt eine klare Reihenfolge, etwa bei einer Schritt-für-Schritt-Anleitung.",
        "aiCheck": false
      },
      {
        "id": "h14_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Eine ungeordnete Liste beginnt mit ___ und jeder Punkt steht in einem ___.",
        "blanks": [
          "ul",
          "li"
        ],
        "aiCheck": false
      },
      {
        "id": "h14_5",
        "type": "code_write",
        "question": "Erstelle eine ungeordnete Liste mit den drei Einträgen \"Rot\", \"Grün\" und \"Blau\".",
        "starterCode": "<!-- Deine Liste hier -->\n",
        "expectedConcepts": [
          "<ul>",
          "</ul>",
          "<li>",
          "</li>"
        ],
        "aiCheck": true
      }
    ]
  },
  "java_1_1": {
    "estimatedMinutes": 15,
    "theory": "# Java Setup & Hello World\n\nWillkommen bei **Java**! Java ist eine objektorientierte Programmiersprache, die plattformunabhängig läuft. Dein Code wird vom **Compiler** (`javac`) in *Bytecode* übersetzt, den die **JVM** (Java Virtual Machine) auf jedem Betriebssystem ausführen kann.\n\n## Dein erstes Programm\n\nIn Java lebt jeder Code innerhalb einer **Klasse**. Der Dateiname muss exakt dem Klassennamen entsprechen, also `HelloWorld.java`.\n\n```java\npublic class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println(\"Hallo Welt!\");\n    }\n}\n```\n\nDie Methode `main` ist der **Einstiegspunkt**: Hier startet die Ausführung. Mit `System.out.println(...)` gibst du Text auf der Konsole aus und springst danach in eine neue Zeile. Jede Anweisung endet mit einem **Semikolon** `;`.\n\n## Die Bestandteile von main\n\n| Teil | Bedeutung |\n|------|-----------|\n| `public` | Von überall sichtbar |\n| `static` | Ohne Objekt aufrufbar |\n| `void` | Kein Rückgabewert |\n| `String[] args` | Kommandozeilen-Argumente |\n\n> 💡 Merksatz: `public static void main(String[] args)` ist die exakte Signatur, die die JVM sucht. Schon ein Tippfehler verhindert den Start.\n\n## Kompilieren & Ausführen\n\nDu übersetzt mit `javac HelloWorld.java` und startest mit `java HelloWorld` (ohne `.class`-Endung). Der Unterschied zu `println` ist `print`: Letzteres gibt **ohne** Zeilenumbruch aus.",
    "tasks": [
      {
        "id": "j11_1",
        "type": "multiple_choice",
        "question": "Welche Methode ist der Einstiegspunkt eines Java-Programms?",
        "options": [
          "start()",
          "public static void main(String[] args)",
          "void run()",
          "begin()"
        ],
        "correctAnswer": 1,
        "explanation": "Die JVM sucht exakt nach der Signatur public static void main(String[] args).",
        "aiCheck": false
      },
      {
        "id": "j11_2",
        "type": "multiple_choice",
        "question": "Womit muss jede Anweisung in Java enden?",
        "options": [
          "Mit einem Punkt .",
          "Mit einem Doppelpunkt :",
          "Mit einem Semikolon ;",
          "Mit einem Komma ,"
        ],
        "correctAnswer": 2,
        "explanation": "Jede Anweisung wird in Java mit einem Semikolon abgeschlossen.",
        "aiCheck": false
      },
      {
        "id": "j11_3",
        "type": "code_write",
        "question": "Schreibe eine Klasse Greeting mit einer main-Methode, die 'Willkommen!' auf der Konsole ausgibt.",
        "starterCode": "public class Greeting {\n    // Schreibe hier die main-Methode\n}\n",
        "expectedConcepts": [
          "public static void main",
          "System.out.println",
          "Willkommen!"
        ],
        "aiCheck": true
      },
      {
        "id": "j11_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Mit ___ wird Text MIT Zeilenumbruch ausgegeben, mit ___ OHNE Zeilenumbruch.",
        "blanks": [
          "System.out.println",
          "System.out.print"
        ],
        "aiCheck": false
      },
      {
        "id": "j11_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, was die Schlüsselwörter static und void in der main-Methode bedeuten.",
        "aiCheck": true
      }
    ]
  },
  "java_1_2": {
    "estimatedMinutes": 15,
    "theory": "# Variablen & primitive Datentypen\n\nEine **Variable** ist ein benannter Speicherplatz für einen Wert. Java ist **statisch typisiert**: Du musst beim Anlegen den **Datentyp** angeben. Das Format lautet `Typ name = wert;`.\n\n## Die wichtigsten Typen\n\n```java\nint alter = 30;\ndouble preis = 9.99;\nboolean istAktiv = true;\nchar buchstabe = 'A';\nString name = \"Lemon\";\n```\n\nBeachte: `char` nutzt **einfache** Anführungszeichen (`'A'`), `String` dagegen **doppelte** (`\"Lemon\"`).\n\n## Übersicht\n\n| Typ | Beispiel | Beschreibung |\n|------|----------|--------------|\n| `int` | `42` | Ganzzahl |\n| `double` | `3.14` | Kommazahl |\n| `boolean` | `true` / `false` | Wahrheitswert |\n| `char` | `'X'` | Einzelnes Zeichen |\n| `String` | `\"Text\"` | Zeichenkette |\n\n`String` ist streng genommen **kein** primitiver Typ, sondern eine Klasse, wird aber wie ein Grundtyp benutzt.\n\n> 💡 Tipp: Wähle `int` für ganze Zahlen und `double` für alles mit Nachkommastellen. Eine ganze Zahl wie `5` als Kommazahl schreibst du `5.0`.\n\n## Variablen verwenden\n\n```java\nint x = 10;\nx = x + 5;\nSystem.out.println(x); // gibt 15 aus\n```\n\nMit `final` machst du eine Variable zu einer **Konstanten**, die sich nicht mehr ändern lässt: `final int MAX = 100;`.",
    "tasks": [
      {
        "id": "j12_1",
        "type": "multiple_choice",
        "question": "Welcher Datentyp eignet sich am besten für die Zahl 3.14?",
        "options": [
          "int",
          "double",
          "boolean",
          "char"
        ],
        "correctAnswer": 1,
        "explanation": "double speichert Kommazahlen; int kann nur ganze Zahlen speichern.",
        "aiCheck": false
      },
      {
        "id": "j12_2",
        "type": "multiple_choice",
        "question": "Welche Deklaration eines char ist KORREKT?",
        "options": [
          "char c = \"A\";",
          "char c = 'A';",
          "char c = A;",
          "char c = (A);"
        ],
        "correctAnswer": 1,
        "explanation": "Ein char wird mit einfachen Anführungszeichen geschrieben: 'A'. Doppelte gehören zu String.",
        "aiCheck": false
      },
      {
        "id": "j12_3",
        "type": "code_write",
        "question": "Deklariere eine int-Variable 'alter' mit Wert 25 und eine String-Variable 'name' mit deinem Namen. Gib beide mit System.out.println aus.",
        "starterCode": "public class Vars {\n    public static void main(String[] args) {\n        // Deine Variablen hier\n    }\n}\n",
        "expectedConcepts": [
          "int alter",
          "String name",
          "=",
          "System.out.println"
        ],
        "aiCheck": true
      },
      {
        "id": "j12_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Der Typ ___ speichert true oder false, und der Typ ___ speichert ganze Zahlen.",
        "blanks": [
          "boolean",
          "int"
        ],
        "aiCheck": false
      },
      {
        "id": "j12_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen char und String.",
        "aiCheck": true
      }
    ]
  },
  "java_1_3": {
    "estimatedMinutes": 15,
    "theory": "# Operatoren & Ausdrücke\n\nMit **Operatoren** verknüpfst du Werte zu **Ausdrücken**, die ein Ergebnis liefern. Java kennt arithmetische, Vergleichs- und logische Operatoren.\n\n## Arithmetik\n\n```java\nint a = 10, b = 3;\nSystem.out.println(a + b); // 13\nSystem.out.println(a - b); // 7\nSystem.out.println(a * b); // 30\nSystem.out.println(a / b); // 3  (Ganzzahldivision!)\nSystem.out.println(a % b); // 1  (Rest / Modulo)\n```\n\nAchtung: `10 / 3` ergibt `3`, weil beide Operanden `int` sind. Für `3.33...` brauchst du `double`: `10.0 / 3`.\n\n## Vergleich & Logik\n\n| Operator | Bedeutung |\n|----------|-----------|\n| `==` | gleich |\n| `!=` | ungleich |\n| `>` `<` | größer / kleiner |\n| `&&` | logisches UND |\n| `\\|\\|` | logisches ODER |\n| `!` | Negation |\n\n```java\nboolean ergebnis = (5 > 3) && (2 < 1); // false\n```\n\nVergleichsoperatoren liefern immer einen `boolean`.\n\n> 💡 Verwechsle nicht `=` (Zuweisung) mit `==` (Vergleich). `x = 5` setzt den Wert, `x == 5` prüft auf Gleichheit.\n\n## Kurzformen\n\nStatt `x = x + 1` schreibst du `x += 1` oder kurz `x++`. Diese **Inkrement**- und **Dekrement**-Operatoren (`++`, `--`) sind besonders in Schleifen praktisch.",
    "tasks": [
      {
        "id": "j13_1",
        "type": "multiple_choice",
        "question": "Was ergibt der Ausdruck 10 % 3 in Java?",
        "options": [
          "3",
          "1",
          "0",
          "3.33"
        ],
        "correctAnswer": 1,
        "explanation": "Der Modulo-Operator % liefert den Rest der Division: 10 / 3 = 3 Rest 1.",
        "aiCheck": false
      },
      {
        "id": "j13_2",
        "type": "multiple_choice",
        "question": "Was ergibt (5 > 3) && (2 > 4)?",
        "options": [
          "true",
          "false",
          "5",
          "ein Fehler"
        ],
        "correctAnswer": 1,
        "explanation": "Das logische UND ist nur true, wenn beide Seiten true sind. 2 > 4 ist false, also false.",
        "aiCheck": false
      },
      {
        "id": "j13_3",
        "type": "code_write",
        "question": "Berechne die Summe und das Produkt von 7 und 4 in zwei Variablen und gib beide Ergebnisse aus.",
        "starterCode": "public class Mathe {\n    public static void main(String[] args) {\n        int a = 7;\n        int b = 4;\n        // Summe und Produkt berechnen und ausgeben\n    }\n}\n",
        "expectedConcepts": [
          "+",
          "*",
          "System.out.println"
        ],
        "aiCheck": true
      },
      {
        "id": "j13_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Der Operator ___ vergleicht auf Gleichheit, während ___ einen Wert zuweist.",
        "blanks": [
          "==",
          "="
        ],
        "aiCheck": false
      },
      {
        "id": "j13_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, warum 10 / 3 in Java das Ergebnis 3 liefert und nicht 3.33.",
        "aiCheck": true
      }
    ]
  },
  "java_1_4": {
    "estimatedMinutes": 15,
    "theory": "# Bedingungen & Schleifen\n\nProgramme treffen **Entscheidungen** und wiederholen Aufgaben. Dafür gibt es Bedingungen (`if`/`else`) und Schleifen (`for`, `while`).\n\n## Bedingungen\n\n```java\nint note = 2;\nif (note == 1) {\n    System.out.println(\"Sehr gut!\");\n} else if (note <= 3) {\n    System.out.println(\"Gut bestanden.\");\n} else {\n    System.out.println(\"Geht noch besser.\");\n}\n```\n\nDie Bedingung in den Klammern muss einen `boolean` ergeben. Nur der **erste** zutreffende Block wird ausgeführt.\n\n## Die for-Schleife\n\n```java\nfor (int i = 0; i < 5; i++) {\n    System.out.println(\"Durchlauf \" + i);\n}\n```\n\nSie besteht aus drei Teilen: **Initialisierung**, **Bedingung** und **Schrittweite**.\n\n## Die while-Schleife\n\n```java\nint zahl = 3;\nwhile (zahl > 0) {\n    System.out.println(zahl);\n    zahl--;\n}\n```\n\n| Schleife | Wann nutzen? |\n|----------|--------------|\n| `for` | Anzahl der Durchläufe bekannt |\n| `while` | Wiederholung bis Bedingung false |\n\n> 💡 Vorsicht vor **Endlosschleifen**: Vergisst du, die Bedingung irgendwann auf `false` zu setzen, läuft die Schleife ewig. Sorge dafür, dass sich der Zustand ändert (z.B. `zahl--`).\n\nMit `break` brichst du eine Schleife sofort ab, mit `continue` springst du zum nächsten Durchlauf.",
    "tasks": [
      {
        "id": "j14_1",
        "type": "multiple_choice",
        "question": "Wie oft wird der Rumpf von 'for (int i = 0; i < 5; i++)' ausgeführt?",
        "options": [
          "4 mal",
          "5 mal",
          "6 mal",
          "Unendlich oft"
        ],
        "correctAnswer": 1,
        "explanation": "i läuft von 0 bis 4, das sind 5 Durchläufe (0,1,2,3,4).",
        "aiCheck": false
      },
      {
        "id": "j14_2",
        "type": "multiple_choice",
        "question": "Welche Schleife eignet sich, wenn die Anzahl der Durchläufe schon vorher feststeht?",
        "options": [
          "while",
          "for",
          "do-while",
          "if"
        ],
        "correctAnswer": 1,
        "explanation": "Die for-Schleife eignet sich, wenn die Anzahl der Wiederholungen bekannt ist.",
        "aiCheck": false
      },
      {
        "id": "j14_3",
        "type": "code_write",
        "question": "Schreibe eine for-Schleife, die die Zahlen von 1 bis 5 jeweils in einer eigenen Zeile ausgibt.",
        "starterCode": "public class Zaehlen {\n    public static void main(String[] args) {\n        // for-Schleife von 1 bis 5\n    }\n}\n",
        "expectedConcepts": [
          "for",
          "i <= 5",
          "System.out.println"
        ],
        "aiCheck": true
      },
      {
        "id": "j14_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Mit ___ bricht man eine Schleife komplett ab, mit ___ springt man zum nächsten Durchlauf.",
        "blanks": [
          "break",
          "continue"
        ],
        "aiCheck": false
      },
      {
        "id": "j14_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, wie eine Endlosschleife entsteht und wie man sie vermeidet.",
        "aiCheck": true
      }
    ]
  },
  "java_1_5": {
    "estimatedMinutes": 15,
    "theory": "# Arrays\n\nEin **Array** speichert mehrere Werte **desselben Typs** unter einem Namen. Die Größe ist nach dem Anlegen **fest**.\n\n## Array anlegen\n\n```java\nint[] zahlen = {10, 20, 30};\nString[] namen = new String[3];\nnamen[0] = \"Lemon\";\n```\n\nDu kannst ein Array direkt mit Werten füllen oder mit `new Typ[länge]` eine leere Hülle erzeugen. Die Elemente erreichst du über einen **Index**, der bei **0** beginnt.\n\n## Zugriff & Länge\n\n```java\nint[] werte = {5, 8, 13};\nSystem.out.println(werte[0]);     // 5\nSystem.out.println(werte.length); // 3\n```\n\n| Ausdruck | Bedeutung |\n|----------|-----------|\n| `arr[0]` | erstes Element |\n| `arr[arr.length - 1]` | letztes Element |\n| `arr.length` | Anzahl Elemente |\n\n> 💡 Ein Zugriff außerhalb der Grenzen, z.B. `werte[3]` bei nur 3 Elementen, löst eine `ArrayIndexOutOfBoundsException` aus. Gültige Indizes gehen von `0` bis `length - 1`.\n\n## Durchlaufen\n\n```java\nint[] werte = {5, 8, 13};\nfor (int i = 0; i < werte.length; i++) {\n    System.out.println(werte[i]);\n}\n// Alternative: for-each\nfor (int w : werte) {\n    System.out.println(w);\n}\n```\n\nDie **for-each**-Schleife ist kompakter, wenn du nur die Werte (und nicht den Index) brauchst.",
    "tasks": [
      {
        "id": "j15_1",
        "type": "multiple_choice",
        "question": "Mit welchem Index spricht man das ERSTE Element eines Arrays an?",
        "options": [
          "1",
          "0",
          "-1",
          "arr.first"
        ],
        "correctAnswer": 1,
        "explanation": "Array-Indizes beginnen in Java bei 0. Das erste Element ist arr[0].",
        "aiCheck": false
      },
      {
        "id": "j15_2",
        "type": "multiple_choice",
        "question": "Was passiert bei int[] a = {1,2,3}; und dem Zugriff a[3]?",
        "options": [
          "Gibt 3 zurück",
          "Gibt 0 zurück",
          "ArrayIndexOutOfBoundsException",
          "Gibt null zurück"
        ],
        "correctAnswer": 2,
        "explanation": "Gültige Indizes sind 0 bis length-1, also 0..2. a[3] wirft eine ArrayIndexOutOfBoundsException.",
        "aiCheck": false
      },
      {
        "id": "j15_3",
        "type": "code_write",
        "question": "Lege ein int-Array mit den Werten 4, 8, 15 an und gib alle Elemente mit einer Schleife aus.",
        "starterCode": "public class Arrays {\n    public static void main(String[] args) {\n        int[] zahlen = {4, 8, 15};\n        // Schleife über das Array\n    }\n}\n",
        "expectedConcepts": [
          "for",
          "length",
          "zahlen[",
          "System.out.println"
        ],
        "aiCheck": true
      },
      {
        "id": "j15_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Die Anzahl der Elemente liest man mit ___ aus, und der gültige Index reicht von 0 bis ___.",
        "blanks": [
          "arr.length",
          "length - 1"
        ],
        "aiCheck": false
      },
      {
        "id": "j15_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Vorteil einer for-each-Schleife gegenüber einer klassischen for-Schleife beim Durchlaufen eines Arrays.",
        "aiCheck": true
      }
    ]
  },
  "javascript_1_2": {
    "estimatedMinutes": 16,
    "theory": "# Datentypen & Operatoren\n\nIn JavaScript verarbeitest du ständig Werte: Zahlen, Texte und Wahrheitswerte. Mit **Operatoren** verknüpfst, vergleichst und kombinierst du diese Werte.\n\n## Arithmetische Operatoren\n\nSie rechnen mit Zahlen vom Typ `number`:\n\n```javascript\nconsole.log(10 + 3);   // 13\nconsole.log(10 - 3);   // 7\nconsole.log(10 * 3);   // 30\nconsole.log(10 / 3);   // 3.3333333333333335\nconsole.log(10 % 3);   // 1  (Rest der Division, \"Modulo\")\nconsole.log(2 ** 3);   // 8  (Potenz)\n```\n\n## Vergleichsoperatoren\n\nSie liefern immer einen `boolean` (`true` oder `false`).\n\n| Operator | Bedeutung            | Beispiel        | Ergebnis |\n|----------|----------------------|-----------------|----------|\n| `===`    | strikt gleich        | `3 === \"3\"`     | `false`  |\n| `==`     | locker gleich        | `3 == \"3\"`      | `true`   |\n| `!==`    | strikt ungleich      | `5 !== 5`       | `false`  |\n| `>` `<`  | größer / kleiner     | `5 > 2`         | `true`   |\n\nDer Unterschied zwischen `==` und `===` ist wichtig: `==` wandelt Typen vorher um (**Type Coercion**), `===` vergleicht Wert **und** Typ.\n\n> 💡 **Faustregel:** Nutze fast immer `===` und `!==`. So vermeidest du überraschende Fehler durch automatische Typumwandlung.\n\n## Logische Operatoren\n\nSie verknüpfen Wahrheitswerte:\n\n```javascript\nconsole.log(true && false); // false  (UND: beide müssen true sein)\nconsole.log(true || false); // true   (ODER: einer reicht)\nconsole.log(!true);         // false  (NICHT: kehrt um)\n```\n\nSo baust du komplexe Bedingungen, etwa \"Alter über 18 **und** Ticket vorhanden\".",
    "tasks": [
      {
        "id": "dt1",
        "type": "multiple_choice",
        "question": "Was gibt `console.log(17 % 5)` aus?",
        "options": [
          "3",
          "2",
          "3.4",
          "12"
        ],
        "correctAnswer": 1,
        "explanation": "Der Modulo-Operator % liefert den Rest der Division: 17 geteilt durch 5 ist 3 Rest 2. Also ist das Ergebnis 2.",
        "aiCheck": false
      },
      {
        "id": "dt2",
        "type": "multiple_choice",
        "question": "Was gibt `console.log(3 === \"3\")` aus?",
        "options": [
          "true",
          "false",
          "3",
          "Ein Fehler"
        ],
        "correctAnswer": 1,
        "explanation": "=== vergleicht Wert UND Typ. Die Zahl 3 und der String \"3\" haben unterschiedliche Typen, daher ist das Ergebnis false.",
        "aiCheck": false
      },
      {
        "id": "dt3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Der ___-Operator (&&) ist nur true, wenn beide Seiten true sind. Der ___-Operator (||) ist true, sobald eine Seite true ist.",
        "blanks": [
          "UND",
          "ODER"
        ],
        "aiCheck": false
      },
      {
        "id": "dt4",
        "type": "code_write",
        "question": "Erstelle zwei Konstanten `a` (Wert 8) und `b` (Wert 3). Gib mit `console.log` aus, ob `a` strikt größer als `b` ist.",
        "starterCode": "// Schreib deinen Code hier:\n\n",
        "expectedConcepts": [
          "const",
          ">",
          "console.log"
        ],
        "aiCheck": true
      },
      {
        "id": "dt5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen `==` und `===`. Warum solltest du meist `===` verwenden?",
        "aiCheck": true
      }
    ]
  },
  "javascript_1_3": {
    "estimatedMinutes": 15,
    "theory": "# Bedingungen: if/else und switch\n\nProgramme müssen Entscheidungen treffen. Mit **Bedingungen** führst du Code nur dann aus, wenn ein Ausdruck `true` ist.\n\n## Die if/else-Anweisung\n\n```javascript\nconst alter = 17;\n\nif (alter >= 18) {\n  console.log(\"Du bist volljährig.\");\n} else if (alter >= 16) {\n  console.log(\"Fast volljährig.\");\n} else {\n  console.log(\"Noch minderjährig.\");\n}\n// Ausgabe: \"Fast volljährig.\"\n```\n\nGeprüft wird von oben nach unten. Der **erste** Block, dessen Bedingung `true` ist, läuft – der Rest wird übersprungen.\n\n## Truthy und Falsy\n\nBedingungen müssen kein echtes `boolean` sein. JavaScript wertet Werte als **truthy** oder **falsy** aus.\n\n| Falsy-Werte        | Truthy (Beispiele) |\n|--------------------|--------------------|\n| `false`, `0`       | `\"text\"`           |\n| `\"\"` (leerer String) | `42`             |\n| `null`, `undefined` | `[]`, `{}`        |\n| `NaN`              | `-1`               |\n\n## Die switch-Anweisung\n\nPraktisch, wenn du **eine** Variable gegen viele feste Werte prüfst:\n\n```javascript\nconst tag = \"Mo\";\n\nswitch (tag) {\n  case \"Sa\":\n  case \"So\":\n    console.log(\"Wochenende!\");\n    break;\n  default:\n    console.log(\"Arbeitstag.\");\n}\n// Ausgabe: \"Arbeitstag.\"\n```\n\n> 💡 **Wichtig:** Vergiss das `break` nicht! Ohne `break` läuft die Ausführung in den nächsten `case` hinein (\"Fall-through\"). Das ist eine häufige Fehlerquelle.\n\n`switch` vergleicht übrigens mit `===`, also strikt nach Wert und Typ.",
    "tasks": [
      {
        "id": "bd1",
        "type": "multiple_choice",
        "question": "Was wird ausgegeben? `const x = 0; if (x) { console.log(\"A\"); } else { console.log(\"B\"); }`",
        "options": [
          "A",
          "B",
          "0",
          "Nichts"
        ],
        "correctAnswer": 1,
        "explanation": "Die Zahl 0 ist ein falsy-Wert. Die if-Bedingung ist also nicht erfüllt, daher läuft der else-Block und gibt \"B\" aus.",
        "aiCheck": false
      },
      {
        "id": "bd2",
        "type": "multiple_choice",
        "question": "Was passiert in einem `switch`, wenn das `break` in einem `case` fehlt?",
        "options": [
          "Ein Syntaxfehler",
          "Der Code im nächsten case wird auch ausgeführt",
          "Der default-Block wird übersprungen",
          "Die Schleife startet neu"
        ],
        "correctAnswer": 1,
        "explanation": "Ohne break gibt es einen Fall-through: Die Ausführung läuft in die folgenden case-Blöcke weiter, bis ein break oder das Ende erreicht wird.",
        "aiCheck": false
      },
      {
        "id": "bd3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Eine ___-Anweisung führt Code aus, wenn eine Bedingung true ist. Der ___-Block läuft, wenn keine vorherige Bedingung zutrifft.",
        "blanks": [
          "if",
          "else"
        ],
        "aiCheck": false
      },
      {
        "id": "bd4",
        "type": "code_write",
        "question": "Schreibe eine `if/else`-Bedingung, die eine Variable `note` (Zahl) prüft: Ist sie kleiner oder gleich 4, gib \"bestanden\" aus, sonst \"durchgefallen\".",
        "starterCode": "const note = 3;\n// Schreib deine Bedingung hier:\n\n",
        "expectedConcepts": [
          "if",
          "else",
          "<=",
          "console.log"
        ],
        "aiCheck": true
      },
      {
        "id": "bd5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, wann du eher eine `switch`-Anweisung statt vieler `else if` verwenden würdest.",
        "aiCheck": true
      }
    ]
  },
  "javascript_1_4": {
    "estimatedMinutes": 16,
    "theory": "# Schleifen: for, while und forEach\n\nMit **Schleifen** wiederholst du Code, ohne ihn mehrfach zu schreiben. Das ist eines der mächtigsten Werkzeuge der Programmierung.\n\n## Die for-Schleife\n\nIdeal, wenn du die Anzahl der Durchläufe kennst:\n\n```javascript\nfor (let i = 0; i < 3; i++) {\n  console.log(\"Durchlauf \" + i);\n}\n// Durchlauf 0\n// Durchlauf 1\n// Durchlauf 2\n```\n\nSie besteht aus drei Teilen: **Startwert** (`let i = 0`), **Bedingung** (`i < 3`) und **Schritt** (`i++`).\n\n## Die while-Schleife\n\nLäuft, solange eine Bedingung `true` ist – die Anzahl ist vorher oft unbekannt:\n\n```javascript\nlet zahl = 1;\nwhile (zahl <= 3) {\n  console.log(zahl);\n  zahl++;\n}\n// 1, 2, 3\n```\n\n> 💡 **Vorsicht vor Endlosschleifen!** Wenn die Bedingung nie `false` wird (z. B. weil du `zahl++` vergisst), läuft das Programm ewig und friert ein.\n\n## forEach für Arrays\n\nUm über jedes Element eines Arrays zu gehen, ist `forEach` am elegantesten:\n\n```javascript\nconst farben = [\"rot\", \"grün\", \"blau\"];\nfarben.forEach((farbe, index) => {\n  console.log(index + \": \" + farbe);\n});\n// 0: rot, 1: grün, 2: blau\n```\n\n## Vergleich\n\n| Schleife   | Wann verwenden?                          |\n|------------|------------------------------------------|\n| `for`      | feste Anzahl an Durchläufen              |\n| `while`    | unbekannte Anzahl, bedingt abhängig      |\n| `forEach`  | jedes Element eines Arrays durchgehen    |\n\nMit `break` brichst du eine Schleife vorzeitig ab, mit `continue` überspringst du den aktuellen Durchlauf.",
    "tasks": [
      {
        "id": "sl1",
        "type": "multiple_choice",
        "question": "Wie oft läuft der Schleifenkörper? `for (let i = 0; i < 5; i++) { ... }`",
        "options": [
          "4 Mal",
          "5 Mal",
          "6 Mal",
          "Unendlich oft"
        ],
        "correctAnswer": 1,
        "explanation": "i startet bei 0 und läuft solange i < 5, also für die Werte 0, 1, 2, 3, 4. Das sind genau 5 Durchläufe.",
        "aiCheck": false
      },
      {
        "id": "sl2",
        "type": "multiple_choice",
        "question": "Welche Gefahr besteht bei einer `while`-Schleife besonders?",
        "options": [
          "Sie ist immer langsamer als for",
          "Eine Endlosschleife, wenn die Bedingung nie false wird",
          "Sie kann keine Zahlen verarbeiten",
          "Sie funktioniert nur mit Arrays"
        ],
        "correctAnswer": 1,
        "explanation": "Wenn die Abbruchbedingung nie false wird (z. B. weil der Zähler nicht erhöht wird), läuft die while-Schleife endlos weiter.",
        "aiCheck": false
      },
      {
        "id": "sl3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Die ___-Schleife eignet sich für eine feste Anzahl an Durchläufen, die ___-Schleife läuft, solange eine Bedingung wahr ist, und ___ geht über jedes Element eines Arrays.",
        "blanks": [
          "for",
          "while",
          "forEach"
        ],
        "aiCheck": false
      },
      {
        "id": "sl4",
        "type": "code_write",
        "question": "Schreibe eine `for`-Schleife, die die Zahlen von 1 bis 10 (einschließlich) mit `console.log` ausgibt.",
        "starterCode": "// Schreib deine Schleife hier:\n\n",
        "expectedConcepts": [
          "for",
          "let",
          "i++",
          "console.log"
        ],
        "aiCheck": true
      },
      {
        "id": "sl5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen einer `for`-Schleife und einer `while`-Schleife.",
        "aiCheck": true
      }
    ]
  },
  "javascript_1_5": {
    "estimatedMinutes": 17,
    "theory": "# Funktionen\n\nEine **Funktion** ist ein wiederverwendbarer Codeblock mit einem Namen. Du definierst Logik einmal und rufst sie beliebig oft auf – das hält Code übersichtlich und vermeidet Wiederholung.\n\n## Deklaration und Aufruf\n\n```javascript\nfunction begruessen(name) {\n  return \"Hallo, \" + name + \"!\";\n}\n\nconst nachricht = begruessen(\"Lena\");\nconsole.log(nachricht); // \"Hallo, Lena!\"\n```\n\n- `name` ist ein **Parameter** (Platzhalter).\n- `\"Lena\"` ist das **Argument** (der echte Wert beim Aufruf).\n- `return` gibt einen Wert zurück. Ohne `return` liefert die Funktion `undefined`.\n\n## Mehrere Parameter\n\n```javascript\nfunction addiere(a, b) {\n  return a + b;\n}\nconsole.log(addiere(4, 7)); // 11\n```\n\n## Arrow-Funktionen\n\nEine kürzere Schreibweise, besonders beliebt für kleine Funktionen:\n\n```javascript\nconst quadrat = (x) => {\n  return x * x;\n};\n\n// Noch kürzer: bei einem einzigen Ausdruck darf return entfallen\nconst verdoppeln = (x) => x * 2;\n\nconsole.log(quadrat(5));    // 25\nconsole.log(verdoppeln(5)); // 10\n```\n\n> 💡 Bei einer Arrow-Funktion mit nur einer Zeile kannst du die geschweiften Klammern **und** das `return` weglassen – der Ausdruck wird automatisch zurückgegeben.\n\n## Überblick\n\n| Begriff      | Bedeutung                                  |\n|--------------|--------------------------------------------|\n| Parameter    | Platzhalter in der Definition              |\n| Argument     | tatsächlicher Wert beim Aufruf             |\n| `return`     | gibt ein Ergebnis zurück                   |\n| Arrow `=>`   | kompakte Funktionssyntax                   |\n\nFunktionen sind die Bausteine, mit denen du größere Programme strukturierst.",
    "tasks": [
      {
        "id": "fn1",
        "type": "multiple_choice",
        "question": "Was gibt eine Funktion zurück, wenn sie kein `return` enthält?",
        "options": [
          "null",
          "0",
          "undefined",
          "Einen Fehler"
        ],
        "correctAnswer": 2,
        "explanation": "Ohne explizites return liefert eine Funktion automatisch undefined zurück.",
        "aiCheck": false
      },
      {
        "id": "fn2",
        "type": "multiple_choice",
        "question": "Welche Schreibweise ist eine korrekte Arrow-Funktion, die x verdoppelt?",
        "options": [
          "const f = x => x * 2;",
          "function => (x) { x * 2 }",
          "const f = (x) -> x * 2;",
          "arrow f(x) { return x*2 }"
        ],
        "correctAnswer": 0,
        "explanation": "const f = x => x * 2; ist gültig: Bei einem einzigen Parameter und einem einzigen Ausdruck darf der Ausdruck ohne Klammern und ohne return stehen.",
        "aiCheck": false
      },
      {
        "id": "fn3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Ein ___ ist ein Platzhalter in der Funktionsdefinition, während das ___ der echte Wert beim Aufruf ist. Mit ___ gibt eine Funktion einen Wert zurück.",
        "blanks": [
          "Parameter",
          "Argument",
          "return"
        ],
        "aiCheck": false
      },
      {
        "id": "fn4",
        "type": "code_write",
        "question": "Schreibe eine Funktion `multipliziere`, die zwei Zahlen als Parameter nimmt und ihr Produkt mit `return` zurückgibt.",
        "starterCode": "// Schreib deine Funktion hier:\n\n",
        "expectedConcepts": [
          "function",
          "return",
          "*"
        ],
        "aiCheck": true
      },
      {
        "id": "fn5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, warum Funktionen nützlich sind und was der Unterschied zwischen einem Parameter und einem Argument ist.",
        "aiCheck": true
      }
    ]
  },
  "javascript_2_1": {
    "estimatedMinutes": 17,
    "theory": "# Arrays & Array-Methoden\n\nEin **Array** ist eine geordnete Liste von Werten. Du speicherst damit mehrere Daten unter einem Namen – jedes Element hat einen **Index**, der bei `0` beginnt.\n\n```javascript\nconst fruechte = [\"Apfel\", \"Banane\", \"Kirsche\"];\nconsole.log(fruechte[0]);     // \"Apfel\"\nconsole.log(fruechte.length); // 3\n```\n\n## Elemente hinzufügen: push\n\n`push` hängt ein Element ans Ende an und verändert das Array direkt:\n\n```javascript\nconst zahlen = [1, 2];\nzahlen.push(3);\nconsole.log(zahlen); // [1, 2, 3]\n```\n\n## map: jedes Element umwandeln\n\n`map` erzeugt ein **neues** Array, indem es eine Funktion auf jedes Element anwendet:\n\n```javascript\nconst zahlen = [1, 2, 3];\nconst verdoppelt = zahlen.map((n) => n * 2);\nconsole.log(verdoppelt); // [2, 4, 6]\nconsole.log(zahlen);     // [1, 2, 3] bleibt unverändert\n```\n\n## filter: Elemente auswählen\n\n`filter` behält nur die Elemente, für die die Funktion `true` zurückgibt:\n\n```javascript\nconst zahlen = [1, 2, 3, 4, 5];\nconst gerade = zahlen.filter((n) => n % 2 === 0);\nconsole.log(gerade); // [2, 4]\n```\n\n> 💡 `map` und `filter` verändern das Original **nicht** – sie geben ein neues Array zurück. `push` dagegen verändert das bestehende Array.\n\n## Übersicht\n\n| Methode    | Zweck                          | Verändert Original? |\n|------------|--------------------------------|---------------------|\n| `push`     | Element ans Ende anhängen       | Ja                  |\n| `map`      | jedes Element umwandeln         | Nein                |\n| `filter`   | Elemente nach Bedingung wählen  | Nein                |\n| `length`   | Anzahl der Elemente (Property)  | –                   |",
    "tasks": [
      {
        "id": "ar1",
        "type": "multiple_choice",
        "question": "Was gibt `[\"a\", \"b\", \"c\"][1]` zurück?",
        "options": [
          "\"a\"",
          "\"b\"",
          "\"c\"",
          "1"
        ],
        "correctAnswer": 1,
        "explanation": "Arrays sind 0-basiert: Index 0 ist \"a\", Index 1 ist \"b\". Daher liefert [1] den Wert \"b\".",
        "aiCheck": false
      },
      {
        "id": "ar2",
        "type": "multiple_choice",
        "question": "Was ist das Ergebnis von `[1, 2, 3].map((n) => n + 1)`?",
        "options": [
          "[1, 2, 3]",
          "[2, 3, 4]",
          "6",
          "[1, 2, 3, 1]"
        ],
        "correctAnswer": 1,
        "explanation": "map wendet die Funktion auf jedes Element an und gibt ein neues Array zurück: 1+1, 2+1, 3+1 ergibt [2, 3, 4].",
        "aiCheck": false
      },
      {
        "id": "ar3",
        "type": "multiple_choice",
        "question": "Welche Methode verändert das ursprüngliche Array?",
        "options": [
          "map",
          "filter",
          "push",
          "Keine davon"
        ],
        "correctAnswer": 2,
        "explanation": "push hängt ein Element ans bestehende Array an und verändert es dadurch. map und filter geben dagegen ein neues Array zurück.",
        "aiCheck": false
      },
      {
        "id": "ar4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Mit ___ hängst du ein Element ans Ende eines Arrays. Mit der Property ___ erfährst du die Anzahl der Elemente.",
        "blanks": [
          "push",
          "length"
        ],
        "aiCheck": false
      },
      {
        "id": "ar5",
        "type": "code_write",
        "question": "Gegeben ist `const preise = [10, 25, 5, 40]`. Erzeuge mit `filter` ein neues Array `teuer`, das nur Preise über 20 enthält, und gib es aus.",
        "starterCode": "const preise = [10, 25, 5, 40];\n// Schreib deinen Code hier:\n\n",
        "expectedConcepts": [
          "filter",
          "=>",
          ">",
          "console.log"
        ],
        "aiCheck": true
      },
      {
        "id": "ar6",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen `map` und `filter`.",
        "aiCheck": true
      }
    ]
  },
  "javascript_2_2": {
    "estimatedMinutes": 17,
    "theory": "# Objekte & JSON\n\nEin **Objekt** speichert Daten als **Schlüssel-Wert-Paare** (Properties). Während ein Array eine Liste ist, beschreibt ein Objekt eine \"Sache\" mit benannten Eigenschaften.\n\n```javascript\nconst person = {\n  name: \"Mia\",\n  alter: 28,\n  istAdmin: false,\n};\n```\n\n## Zugriff auf Properties\n\nEs gibt zwei Wege:\n\n```javascript\nconsole.log(person.name);      // \"Mia\"  (Punkt-Notation)\nconsole.log(person[\"alter\"]);  // 28     (Klammer-Notation)\n\nperson.alter = 29;             // Wert ändern\nperson.stadt = \"Berlin\";       // neue Property hinzufügen\n```\n\n> 💡 Die Punkt-Notation ist kürzer und üblicher. Die Klammer-Notation brauchst du, wenn der Schlüssel in einer Variablen steckt oder Sonderzeichen enthält.\n\n## JSON\n\n**JSON** (JavaScript Object Notation) ist ein Textformat zum Austausch von Daten, z. B. mit einem Server. Es sieht aus wie ein JS-Objekt, ist aber ein **String**.\n\n```javascript\nconst obj = { name: \"Mia\", alter: 28 };\n\n// Objekt -> JSON-String\nconst text = JSON.stringify(obj);\nconsole.log(text); // '{\"name\":\"Mia\",\"alter\":28}'\n\n// JSON-String -> Objekt\nconst zurueck = JSON.parse(text);\nconsole.log(zurueck.name); // \"Mia\"\n```\n\n## Übersicht\n\n| Funktion           | Eingabe        | Ausgabe        |\n|--------------------|----------------|----------------|\n| `JSON.stringify`   | Objekt         | JSON-String    |\n| `JSON.parse`       | JSON-String    | Objekt         |\n\nMerke: In JSON müssen alle Schlüssel in **doppelten** Anführungszeichen stehen, und es sind keine Funktionen erlaubt – nur Daten.",
    "tasks": [
      {
        "id": "ob1",
        "type": "multiple_choice",
        "question": "Wie greifst du auf die Property `name` des Objekts `user` zu?",
        "options": [
          "user->name",
          "user.name",
          "user[name]",
          "name(user)"
        ],
        "correctAnswer": 1,
        "explanation": "Mit der Punkt-Notation user.name greifst du auf die Property zu. user[name] wäre nur korrekt, wenn name eine Variable mit dem Schlüsselnamen wäre.",
        "aiCheck": false
      },
      {
        "id": "ob2",
        "type": "multiple_choice",
        "question": "Was macht `JSON.parse('{\"x\":5}')`?",
        "options": [
          "Gibt den String unverändert zurück",
          "Wandelt den JSON-String in ein Objekt um",
          "Wandelt ein Objekt in einen String um",
          "Erzeugt einen Fehler"
        ],
        "correctAnswer": 1,
        "explanation": "JSON.parse wandelt einen JSON-String in ein JavaScript-Objekt um. Hier entsteht das Objekt { x: 5 }.",
        "aiCheck": false
      },
      {
        "id": "ob3",
        "type": "multiple_choice",
        "question": "Was liefert `JSON.stringify({ a: 1 })`?",
        "options": [
          "{ a: 1 }",
          "'{\"a\":1}'",
          "[a, 1]",
          "1"
        ],
        "correctAnswer": 1,
        "explanation": "JSON.stringify wandelt das Objekt in einen JSON-String um. Schlüssel werden dabei in doppelte Anführungszeichen gesetzt: '{\"a\":1}'.",
        "aiCheck": false
      },
      {
        "id": "ob4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Mit ___ wandelst du ein Objekt in einen JSON-String um. Mit ___ machst du aus einem JSON-String wieder ein Objekt.",
        "blanks": [
          "JSON.stringify",
          "JSON.parse"
        ],
        "aiCheck": false
      },
      {
        "id": "ob5",
        "type": "code_write",
        "question": "Erstelle ein Objekt `buch` mit den Properties `titel` (String) und `seiten` (Zahl). Gib danach den Titel mit `console.log` aus.",
        "starterCode": "// Schreib deinen Code hier:\n\n",
        "expectedConcepts": [
          "const",
          "{",
          "titel",
          "console.log"
        ],
        "aiCheck": true
      },
      {
        "id": "ob6",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, was JSON ist und wozu man `JSON.stringify` und `JSON.parse` braucht.",
        "aiCheck": true
      }
    ]
  },
  "python_1_1": {
    "estimatedMinutes": 14,
    "theory": "# Python installieren & Hello World\n\nWillkommen zu deinem ersten Schritt mit **Python**! Python ist eine der beliebtesten Programmiersprachen, weil sie gut lesbar und einfach zu lernen ist.\n\n## Python installieren\n\nLade Python von [python.org](https://www.python.org) herunter. Prüfe danach im Terminal, ob alles funktioniert:\n\n```bash\npython --version\n```\n\nDu solltest etwas wie `Python 3.12.0` sehen.\n\n## Dein erstes Programm\n\nMit der Funktion `print()` gibst du Text auf dem Bildschirm aus:\n\n```python\nprint(\"Hello, World!\")\n# Diese Zeile ist ein Kommentar und wird ignoriert\nprint(\"Ich lerne Python\")\n```\n\nDie Ausgabe lautet:\n\n```\nHello, World!\nIch lerne Python\n```\n\n## Kommentare\n\nKommentare beginnen mit `#` und werden von Python **nicht** ausgeführt. Sie dienen als Notizen für Menschen.\n\n| Schreibweise | Bedeutung |\n| --- | --- |\n| `# Text` | Einzeiliger Kommentar |\n| `print(\"Hi\")` | Gibt `Hi` aus |\n| `print()` | Gibt eine Leerzeile aus |\n\n> 💡 **Tipp:** Der Text in `print()` muss in Anführungszeichen stehen (`\"...\"` oder `'...'`). Vergisst du sie, behandelt Python den Text als Variablennamen und es kommt zu einem Fehler.\n\nMehrere Werte kannst du durch Kommas getrennt ausgeben. Python fügt automatisch ein Leerzeichen ein:\n\n```python\nprint(\"Alter:\", 25)\n# Ausgabe: Alter: 25\n```",
    "tasks": [
      {
        "id": "p11_1",
        "type": "multiple_choice",
        "question": "Was gibt `print(\"Hallo\")` aus?",
        "options": [
          "\"Hallo\"",
          "Hallo",
          "print(Hallo)",
          "Ein Fehler"
        ],
        "correctAnswer": 1,
        "explanation": "print() gibt den Text ohne die Anführungszeichen aus, also genau: Hallo",
        "aiCheck": false
      },
      {
        "id": "p11_2",
        "type": "multiple_choice",
        "question": "Womit beginnt ein einzeiliger Kommentar in Python?",
        "options": [
          "//",
          "<!--",
          "#",
          "/*"
        ],
        "correctAnswer": 2,
        "explanation": "In Python leitet das Zeichen # einen Kommentar ein, der bis zum Zeilenende reicht.",
        "aiCheck": false
      },
      {
        "id": "p11_3",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Die Funktion ___ gibt Text aus, und ein Kommentar beginnt mit dem Zeichen ___.",
        "blanks": [
          "print",
          "#"
        ],
        "aiCheck": false
      },
      {
        "id": "p11_4",
        "type": "code_write",
        "question": "Schreibe ein Programm, das mit print() den Text 'Hallo Welt' ausgibt und darüber einen Kommentar enthält, der das Programm beschreibt.",
        "starterCode": "# Schreibe deinen Code hier\n",
        "expectedConcepts": [
          "print",
          "Kommentar",
          "String"
        ],
        "aiCheck": true
      },
      {
        "id": "p11_5",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, warum man Kommentare in seinem Code verwendet, obwohl Python sie ignoriert.",
        "aiCheck": true
      }
    ]
  },
  "python_1_2": {
    "estimatedMinutes": 14,
    "theory": "# Variablen & Datentypen\n\nEine **Variable** ist ein Name, unter dem du einen Wert speicherst. In Python erstellst du sie mit dem Zuweisungsoperator `=`.\n\n```python\nname = \"Anna\"\nalter = 25\ngroesse = 1.72\nist_student = True\n```\n\n## Die wichtigsten Datentypen\n\nPython erkennt den Datentyp automatisch anhand des Werts. Mit `type()` kannst du ihn überprüfen:\n\n```python\nprint(type(alter))   # <class 'int'>\nprint(type(groesse)) # <class 'float'>\nprint(type(name))    # <class 'str'>\nprint(type(ist_student)) # <class 'bool'>\n```\n\n| Typ | Beschreibung | Beispiel |\n| --- | --- | --- |\n| `int` | Ganze Zahl | `42` |\n| `float` | Kommazahl | `3.14` |\n| `str` | Zeichenkette (Text) | `\"Hallo\"` |\n| `bool` | Wahrheitswert | `True` / `False` |\n\n## Typen umwandeln\n\nDu kannst Werte zwischen Typen konvertieren:\n\n```python\nzahl = int(\"10\")     # str -> int, ergibt 10\ntext = str(99)       # int -> str, ergibt \"99\"\nkomma = float(3)     # int -> float, ergibt 3.0\n```\n\n> 💡 **Achtung:** Variablennamen sind in Python **case-sensitive**. `Alter` und `alter` sind zwei verschiedene Variablen!\n\nEin häufiger Fehler ist das Verwechseln von `int` und `str`. `5 + 3` ergibt `8`, aber `\"5\" + \"3\"` ergibt `\"53\"`, weil Strings aneinandergehängt werden.",
    "tasks": [
      {
        "id": "p12_1",
        "type": "multiple_choice",
        "question": "Welchen Datentyp hat der Wert `3.14` in Python?",
        "options": [
          "int",
          "float",
          "str",
          "bool"
        ],
        "correctAnswer": 1,
        "explanation": "Zahlen mit Nachkommastellen sind vom Typ float (Fließkommazahl).",
        "aiCheck": false
      },
      {
        "id": "p12_2",
        "type": "multiple_choice",
        "question": "Was gibt `print(\"5\" + \"3\")` aus?",
        "options": [
          "8",
          "53",
          "\"53\"",
          "Ein Fehler"
        ],
        "correctAnswer": 1,
        "explanation": "Beide Werte sind Strings, daher werden sie aneinandergehängt (Konkatenation): 53",
        "aiCheck": false
      },
      {
        "id": "p12_3",
        "type": "multiple_choice",
        "question": "Was gibt `type(True)` zurück?",
        "options": [
          "<class 'int'>",
          "<class 'bool'>",
          "<class 'str'>",
          "<class 'true'>"
        ],
        "correctAnswer": 1,
        "explanation": "True und False sind Wahrheitswerte vom Typ bool.",
        "aiCheck": false
      },
      {
        "id": "p12_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Mit der Funktion ___ prüft man den Datentyp, und ganze Zahlen haben den Typ ___.",
        "blanks": [
          "type",
          "int"
        ],
        "aiCheck": false
      },
      {
        "id": "p12_5",
        "type": "code_write",
        "question": "Erstelle drei Variablen: einen Namen (str), ein Alter (int) und eine Groesse (float). Gib anschliessend den Datentyp jeder Variable mit type() aus.",
        "starterCode": "# Erstelle deine Variablen hier\n",
        "expectedConcepts": [
          "Variable",
          "type",
          "int",
          "float",
          "str"
        ],
        "aiCheck": true
      }
    ]
  },
  "python_1_3": {
    "estimatedMinutes": 14,
    "theory": "# Strings & String-Methoden\n\nEin **String** (`str`) ist eine Zeichenkette. Jedes Zeichen hat eine Position, einen sogenannten **Index**, beginnend bei `0`.\n\n```python\nwort = \"Python\"\nprint(wort[0])   # P\nprint(wort[-1])  # n (letztes Zeichen)\n```\n\n## Slicing\n\nMit `[start:ende]` schneidest du Teile heraus. Der Endindex ist **nicht** enthalten:\n\n```python\nwort = \"Python\"\nprint(wort[0:3])  # Pyt\nprint(wort[2:])   # thon\nprint(wort[:2])   # Py\n```\n\n## Nützliche String-Methoden\n\n| Methode | Beschreibung | Beispiel |\n| --- | --- | --- |\n| `.upper()` | Grossbuchstaben | `\"hi\".upper()` → `\"HI\"` |\n| `.lower()` | Kleinbuchstaben | `\"HI\".lower()` → `\"hi\"` |\n| `.strip()` | Leerzeichen entfernen | `\" hi \".strip()` → `\"hi\"` |\n| `.replace(a, b)` | Ersetzen | `\"ab\".replace(\"a\",\"x\")` → `\"xb\"` |\n| `len(s)` | Länge | `len(\"hi\")` → `2` |\n\n## f-Strings\n\nMit einem **f-String** fügst du Variablen direkt in Text ein. Stelle dem String einfach ein `f` voran und schreibe die Variable in geschweifte Klammern:\n\n```python\nname = \"Anna\"\nalter = 25\nprint(f\"{name} ist {alter} Jahre alt.\")\n# Anna ist 25 Jahre alt.\n```\n\n> 💡 **Tipp:** Strings sind in Python **unveränderlich** (immutable). Methoden wie `.upper()` ändern den Original-String nicht, sondern geben einen **neuen** String zurück.",
    "tasks": [
      {
        "id": "p13_1",
        "type": "multiple_choice",
        "question": "Was gibt `\"Python\"[1:4]` zurück?",
        "options": [
          "Pyt",
          "yth",
          "ytho",
          "Pyth"
        ],
        "correctAnswer": 1,
        "explanation": "Slicing startet bei Index 1 (y) und endet vor Index 4. Also y, t, h = 'yth'.",
        "aiCheck": false
      },
      {
        "id": "p13_2",
        "type": "multiple_choice",
        "question": "Was gibt `\"hallo\".upper()` zurück?",
        "options": [
          "hallo",
          "Hallo",
          "HALLO",
          "hALLO"
        ],
        "correctAnswer": 2,
        "explanation": ".upper() wandelt alle Zeichen in Grossbuchstaben um: HALLO",
        "aiCheck": false
      },
      {
        "id": "p13_3",
        "type": "multiple_choice",
        "question": "Welche Ausgabe erzeugt `name = \"Tim\"; print(f\"Hi {name}\")`?",
        "options": [
          "Hi {name}",
          "Hi Tim",
          "f Hi Tim",
          "Hi name"
        ],
        "correctAnswer": 1,
        "explanation": "Ein f-String ersetzt {name} durch den Wert der Variable, also Tim.",
        "aiCheck": false
      },
      {
        "id": "p13_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Der erste Index eines Strings ist ___, und ein f-String beginnt mit dem Buchstaben ___.",
        "blanks": [
          "0",
          "f"
        ],
        "aiCheck": false
      },
      {
        "id": "p13_5",
        "type": "code_write",
        "question": "Speichere deinen Vornamen in einer Variable. Gib ihn in Grossbuchstaben aus und erstelle dann mit einem f-String den Satz '<Name> hat X Buchstaben', wobei X die Laenge des Namens ist.",
        "starterCode": "name = \"DeinName\"\n",
        "expectedConcepts": [
          "upper",
          "f-string",
          "len"
        ],
        "aiCheck": true
      }
    ]
  },
  "python_1_4": {
    "estimatedMinutes": 14,
    "theory": "# Listen, Tupel, Sets\n\nPython bietet mehrere Möglichkeiten, mehrere Werte in einer Variable zu speichern. Die drei wichtigsten sind **Liste**, **Tupel** und **Set**.\n\n## Listen\n\nEine **Liste** ist geordnet und **veränderbar**. Du erstellst sie mit eckigen Klammern `[]`:\n\n```python\nfarben = [\"rot\", \"grün\", \"blau\"]\nfarben.append(\"gelb\")   # hinzufügen\nprint(farben[0])        # rot\nfarben[1] = \"schwarz\"   # ändern\n```\n\n## Tupel\n\nEin **Tupel** ist geordnet, aber **unveränderbar** (immutable). Du erstellst es mit runden Klammern `()`:\n\n```python\npunkt = (10, 20)\nprint(punkt[0])  # 10\n# punkt[0] = 5  -> Fehler! Tupel sind unveränderbar\n```\n\n## Sets\n\nEin **Set** ist eine ungeordnete Sammlung **ohne Duplikate**. Du erstellst es mit geschweiften Klammern `{}`:\n\n```python\nzahlen = {1, 2, 2, 3}\nprint(zahlen)  # {1, 2, 3} – Duplikat entfernt\n```\n\n## Vergleich\n\n| Typ | Klammern | Geordnet | Veränderbar | Duplikate |\n| --- | --- | --- | --- | --- |\n| Liste | `[]` | Ja | Ja | Ja |\n| Tupel | `()` | Ja | Nein | Ja |\n| Set | `{}` | Nein | Ja | Nein |\n\n> 💡 **Merke:** Nutze eine **Liste**, wenn sich die Daten ändern können, ein **Tupel** für feste Werte (z. B. Koordinaten) und ein **Set**, wenn du eindeutige Werte brauchst.\n\nMit `len()` ermittelst du bei allen dreien die Anzahl der Elemente.",
    "tasks": [
      {
        "id": "p14_1",
        "type": "multiple_choice",
        "question": "Welcher Datentyp ist geordnet UND veränderbar?",
        "options": [
          "Tupel",
          "Set",
          "Liste",
          "String"
        ],
        "correctAnswer": 2,
        "explanation": "Eine Liste ist geordnet (Indexzugriff möglich) und veränderbar (Elemente hinzufügen/ändern).",
        "aiCheck": false
      },
      {
        "id": "p14_2",
        "type": "multiple_choice",
        "question": "Was gibt `print({1, 2, 2, 3})` aus?",
        "options": [
          "{1, 2, 2, 3}",
          "{1, 2, 3}",
          "[1, 2, 3]",
          "Ein Fehler"
        ],
        "correctAnswer": 1,
        "explanation": "Ein Set entfernt Duplikate, daher bleibt {1, 2, 3} übrig.",
        "aiCheck": false
      },
      {
        "id": "p14_3",
        "type": "multiple_choice",
        "question": "Welche Klammern verwendet man, um ein Tupel zu erstellen?",
        "options": [
          "Eckige Klammern []",
          "Runde Klammern ()",
          "Geschweifte Klammern {}",
          "Spitze Klammern <>"
        ],
        "correctAnswer": 1,
        "explanation": "Tupel werden mit runden Klammern () erstellt, z. B. (10, 20).",
        "aiCheck": false
      },
      {
        "id": "p14_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Eine Liste ist ___ (kann geändert werden), während ein Tupel ___ ist.",
        "blanks": [
          "veränderbar",
          "unveränderbar"
        ],
        "aiCheck": false
      },
      {
        "id": "p14_5",
        "type": "code_write",
        "question": "Erstelle eine Liste mit drei Tiernamen, füge mit append() ein viertes Tier hinzu und gib anschliessend mit len() die Anzahl der Tiere aus.",
        "starterCode": "# Erstelle deine Liste hier\n",
        "expectedConcepts": [
          "Liste",
          "append",
          "len"
        ],
        "aiCheck": true
      }
    ]
  },
  "python_1_5": {
    "estimatedMinutes": 14,
    "theory": "# Dictionaries\n\nEin **Dictionary** (`dict`) speichert Daten als **Schlüssel-Wert-Paare** (key-value). Statt über einen Index greifst du über den Schlüssel auf den Wert zu.\n\n```python\nperson = {\n    \"name\": \"Anna\",\n    \"alter\": 25,\n    \"stadt\": \"Berlin\"\n}\nprint(person[\"name\"])  # Anna\n```\n\n## Werte hinzufügen und ändern\n\n```python\nperson[\"beruf\"] = \"Lehrerin\"  # neues Paar hinzufügen\nperson[\"alter\"] = 26          # Wert ändern\n```\n\n## Sicherer Zugriff mit get()\n\nGreifst du auf einen nicht vorhandenen Schlüssel mit `[]` zu, entsteht ein `KeyError`. Die Methode `.get()` gibt stattdessen `None` (oder einen Standardwert) zurück:\n\n```python\nprint(person.get(\"email\"))            # None\nprint(person.get(\"email\", \"fehlt\"))   # fehlt\n```\n\n## Wichtige Methoden\n\n| Methode | Beschreibung |\n| --- | --- |\n| `.keys()` | Alle Schlüssel |\n| `.values()` | Alle Werte |\n| `.items()` | Alle Paare als Tupel |\n| `.get(k)` | Wert sicher abrufen |\n\nDu kannst über ein Dictionary auch iterieren:\n\n```python\nfor schluessel, wert in person.items():\n    print(schluessel, \"->\", wert)\n```\n\n> 💡 **Merke:** Schlüssel müssen **eindeutig** und unveränderbar sein (z. B. Strings oder Zahlen). Wird ein Schlüssel doppelt vergeben, überschreibt der letzte Wert den vorherigen.",
    "tasks": [
      {
        "id": "p15_1",
        "type": "multiple_choice",
        "question": "Wie greift man auf den Wert des Schlüssels `\"name\"` in einem Dictionary `d` zu?",
        "options": [
          "d.name",
          "d[\"name\"]",
          "d(0)",
          "d->name"
        ],
        "correctAnswer": 1,
        "explanation": "Auf Werte greift man über den Schlüssel in eckigen Klammern zu: d[\"name\"].",
        "aiCheck": false
      },
      {
        "id": "p15_2",
        "type": "multiple_choice",
        "question": "Was passiert bei `d[\"x\"]`, wenn der Schlüssel \"x\" nicht existiert?",
        "options": [
          "Gibt None zurück",
          "Gibt 0 zurück",
          "Es entsteht ein KeyError",
          "Erstellt den Schlüssel"
        ],
        "correctAnswer": 2,
        "explanation": "Der Zugriff mit [] auf einen nicht vorhandenen Schlüssel löst einen KeyError aus. .get() wäre sicherer.",
        "aiCheck": false
      },
      {
        "id": "p15_3",
        "type": "multiple_choice",
        "question": "Welche Methode gibt alle Schlüssel-Wert-Paare zurück?",
        "options": [
          ".keys()",
          ".values()",
          ".items()",
          ".pairs()"
        ],
        "correctAnswer": 2,
        "explanation": ".items() liefert alle Paare als (Schlüssel, Wert)-Tupel.",
        "aiCheck": false
      },
      {
        "id": "p15_4",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus:",
        "template": "Ein Dictionary speichert Daten als ___-Wert-Paare und wird mit ___ Klammern erstellt.",
        "blanks": [
          "Schlüssel",
          "geschweiften"
        ],
        "aiCheck": false
      },
      {
        "id": "p15_5",
        "type": "code_write",
        "question": "Erstelle ein Dictionary für ein Auto mit den Schlüsseln 'marke', 'modell' und 'baujahr'. Füge danach den Schlüssel 'farbe' hinzu und gib die Marke des Autos aus.",
        "starterCode": "# Erstelle dein Dictionary hier\n",
        "expectedConcepts": [
          "dict",
          "Schlüssel",
          "Zugriff",
          "hinzufügen"
        ],
        "aiCheck": true
      }
    ]
  },
  "sql_1_1": {
    "estimatedMinutes": 12,
    "theory": "# Was sind Datenbanken?\n\nEine **Datenbank** ist ein organisierter Speicher für Daten. In einer **relationalen Datenbank** werden Daten in **Tabellen** abgelegt – ganz ähnlich wie in einer Tabellenkalkulation.\n\n## Tabellen, Zeilen und Spalten\n\nEine **Tabelle** besteht aus **Spalten** (engl. *columns*) und **Zeilen** (engl. *rows*).\n\n- Jede **Spalte** beschreibt ein Merkmal, z. B. `name` oder `email`.\n- Jede **Zeile** ist ein einzelner Datensatz, z. B. ein konkreter Kunde.\n\nHier ein Beispiel für eine Tabelle `kunden`:\n\n| id | name      | stadt     |\n|----|-----------|-----------|\n| 1  | Anna      | Berlin    |\n| 2  | Ben       | Hamburg   |\n| 3  | Clara     | München   |\n\n## Der Primärschlüssel\n\nJede Tabelle sollte einen **Primärschlüssel** (engl. *primary key*) besitzen. Das ist eine Spalte, deren Wert jede Zeile **eindeutig** identifiziert. Oben ist das die Spalte `id`.\n\n```sql\nCREATE TABLE kunden (\n  id INTEGER PRIMARY KEY,\n  name TEXT,\n  stadt TEXT\n);\n```\n\nEin Primärschlüssel darf **nicht doppelt** vorkommen und **nicht leer** (NULL) sein.\n\n> 💡 Merke: Der Primärschlüssel ist wie eine Ausweisnummer – kein zweiter Datensatz darf denselben Wert haben.\n\nSo weißt du immer genau, welche Zeile gemeint ist, selbst wenn zwei Kunden denselben Namen tragen.",
    "tasks": [
      {
        "id": "s11_q1",
        "type": "multiple_choice",
        "question": "Was beschreibt eine Spalte in einer Tabelle?",
        "options": [
          "Einen einzelnen Datensatz",
          "Ein Merkmal/Attribut der Daten, z. B. name",
          "Die gesamte Datenbank",
          "Eine Verbindung zwischen zwei Tabellen"
        ],
        "correctAnswer": 1,
        "explanation": "Eine Spalte steht für ein Merkmal wie name oder stadt. Ein einzelner Datensatz ist dagegen eine Zeile.",
        "aiCheck": false
      },
      {
        "id": "s11_q2",
        "type": "multiple_choice",
        "question": "Welche Eigenschaft hat ein Primärschlüssel?",
        "options": [
          "Er darf sich beliebig oft wiederholen",
          "Er identifiziert jede Zeile eindeutig",
          "Er muss immer eine Textspalte sein",
          "Er darf NULL sein"
        ],
        "correctAnswer": 1,
        "explanation": "Ein Primärschlüssel identifiziert jede Zeile eindeutig und darf weder doppelt noch NULL sein.",
        "aiCheck": false
      },
      {
        "id": "s11_fb1",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus: Eine ___ besteht aus Spalten und ___.",
        "template": "Eine ___ besteht aus Spalten und ___.",
        "blanks": [
          "Tabelle",
          "Zeilen"
        ],
        "aiCheck": false
      },
      {
        "id": "s11_ex1",
        "type": "explain",
        "question": "Erkläre in eigenen Worten, warum ein Primärschlüssel wichtig ist, wenn zwei Kunden denselben Namen haben.",
        "aiCheck": true
      },
      {
        "id": "s11_cw1",
        "type": "code_write",
        "question": "Schreibe eine CREATE TABLE Anweisung für eine Tabelle produkte mit den Spalten id (Primärschlüssel), name und preis.",
        "starterCode": "-- Lege die Tabelle produkte an\n",
        "expectedConcepts": [
          "create table",
          "primary key",
          "produkte"
        ],
        "aiCheck": true
      }
    ]
  },
  "sql_1_2": {
    "estimatedMinutes": 12,
    "theory": "# SELECT & FROM\n\nMit **SELECT** liest du Daten aus einer Datenbank. Es ist der wichtigste Befehl in SQL und der erste, den du wirklich brauchst.\n\n## Der Aufbau\n\nEine einfache Abfrage besteht aus zwei Teilen:\n\n- **SELECT** gibt an, **welche Spalten** du sehen willst.\n- **FROM** gibt an, **aus welcher Tabelle** die Daten kommen.\n\n```sql\nSELECT name, stadt\nFROM kunden;\n```\n\nDiese Abfrage liefert nur die Spalten `name` und `stadt` aus der Tabelle `kunden`.\n\n## Alle Spalten mit *\n\nMöchtest du **alle Spalten** sehen, benutze den Stern `*`:\n\n```sql\nSELECT * FROM kunden;\n```\n\nBeispiel-Datensätze der Tabelle `kunden`:\n\n| id | name  | stadt    |\n|----|-------|----------|\n| 1  | Anna  | Berlin   |\n| 2  | Ben   | Hamburg  |\n\n## Wichtige Details\n\n- Jede Anweisung endet mit einem **Semikolon** `;`.\n- SQL-Schlüsselwörter werden oft **GROSS** geschrieben, das ist aber nur Stil – `select` funktioniert auch.\n- Die Reihenfolge der Spalten nach `SELECT` bestimmt die Reihenfolge in der Ausgabe.\n\n> 💡 Tipp: Benutze `*` zum schnellen Stöbern, aber wähle in echten Programmen lieber gezielt die Spalten aus, die du wirklich brauchst – das ist schneller und klarer.",
    "tasks": [
      {
        "id": "s12_q1",
        "type": "multiple_choice",
        "question": "Welcher Teil der Abfrage gibt an, aus welcher Tabelle die Daten kommen?",
        "options": [
          "SELECT",
          "WHERE",
          "FROM",
          "ORDER BY"
        ],
        "correctAnswer": 2,
        "explanation": "FROM bestimmt die Quelltabelle. SELECT bestimmt die Spalten.",
        "aiCheck": false
      },
      {
        "id": "s12_q2",
        "type": "multiple_choice",
        "question": "Was bewirkt SELECT * FROM kunden;?",
        "options": [
          "Es löscht alle Kunden",
          "Es gibt alle Spalten aller Zeilen der Tabelle kunden zurück",
          "Es gibt nur die Spalte name zurück",
          "Es erstellt die Tabelle kunden"
        ],
        "correctAnswer": 1,
        "explanation": "Der Stern * wählt alle Spalten, und ohne WHERE werden alle Zeilen zurückgegeben.",
        "aiCheck": false
      },
      {
        "id": "s12_fb1",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus, um alle Spalten aus kunden zu wählen:",
        "template": "___ * ___ kunden;",
        "blanks": [
          "SELECT",
          "FROM"
        ],
        "aiCheck": false
      },
      {
        "id": "s12_cw1",
        "type": "code_write",
        "question": "Schreibe eine Abfrage, die nur die Spalten name und stadt aus der Tabelle kunden zurückgibt.",
        "starterCode": "-- Wähle name und stadt aus kunden\n",
        "expectedConcepts": [
          "select",
          "from",
          "kunden",
          "name",
          "stadt"
        ],
        "aiCheck": true
      },
      {
        "id": "s12_ex1",
        "type": "explain",
        "question": "Erkläre in eigenen Worten den Unterschied zwischen SELECT * und der gezielten Auswahl einzelner Spalten.",
        "aiCheck": true
      }
    ]
  },
  "sql_1_3": {
    "estimatedMinutes": 12,
    "theory": "# WHERE & Bedingungen\n\nMit **WHERE** filterst du Zeilen. Nur Zeilen, für die die Bedingung **wahr** ist, kommen in das Ergebnis.\n\n## Vergleichsoperatoren\n\n```sql\nSELECT name, alter\nFROM kunden\nWHERE alter > 18;\n```\n\nDie wichtigsten Operatoren:\n\n| Operator | Bedeutung           | Beispiel           |\n|----------|---------------------|--------------------|\n| `=`      | gleich              | `stadt = 'Berlin'` |\n| `>`      | größer als          | `alter > 18`       |\n| `<`      | kleiner als         | `preis < 100`      |\n| `<>`     | ungleich            | `stadt <> 'Bonn'`  |\n\n> 💡 Achtung: In SQL bedeutet ein einzelnes `=` \"ist gleich\" (kein `==` wie in vielen Programmiersprachen). Texte stehen in **einfachen Anführungszeichen**: `'Berlin'`.\n\n## AND und OR\n\nMehrere Bedingungen verknüpfst du mit **AND** (beide müssen wahr sein) oder **OR** (mindestens eine muss wahr sein):\n\n```sql\nSELECT name FROM kunden\nWHERE stadt = 'Berlin' AND alter > 30;\n\nSELECT name FROM kunden\nWHERE stadt = 'Berlin' OR stadt = 'Hamburg';\n```\n\n## Mustersuche mit LIKE\n\n`LIKE` sucht nach **Textmustern**. Das Prozentzeichen `%` steht für beliebig viele Zeichen:\n\n```sql\nSELECT name FROM kunden\nWHERE name LIKE 'A%';\n```\n\nDas findet alle Namen, die mit **A** beginnen, z. B. *Anna* oder *Anton*. `'%a'` findet Namen, die auf *a* enden, und `'%nn%'` findet *nn* an beliebiger Stelle.",
    "tasks": [
      {
        "id": "s13_q1",
        "type": "multiple_choice",
        "question": "Welche Bedingung findet alle Kunden aus Berlin?",
        "options": [
          "WHERE stadt == Berlin",
          "WHERE stadt = 'Berlin'",
          "WHERE stadt LIKE Berlin",
          "WHERE stadt > 'Berlin'"
        ],
        "correctAnswer": 1,
        "explanation": "In SQL prüft man Gleichheit mit einem einzelnen = und Texte stehen in einfachen Anführungszeichen.",
        "aiCheck": false
      },
      {
        "id": "s13_q2",
        "type": "multiple_choice",
        "question": "Was bewirkt WHERE name LIKE 'A%'?",
        "options": [
          "Findet Namen, die genau 'A%' lauten",
          "Findet Namen, die mit A beginnen",
          "Findet Namen, die mit A enden",
          "Findet Namen, die ein A in der Mitte haben"
        ],
        "correctAnswer": 1,
        "explanation": "Das % nach dem A steht für beliebig viele Zeichen danach, also alle Namen, die mit A beginnen.",
        "aiCheck": false
      },
      {
        "id": "s13_q3",
        "type": "multiple_choice",
        "question": "Wann liefert eine mit AND verknüpfte Bedingung eine Zeile zurück?",
        "options": [
          "Wenn mindestens eine Teilbedingung wahr ist",
          "Wenn beide Teilbedingungen wahr sind",
          "Immer, AND wird ignoriert",
          "Nur wenn beide Teilbedingungen falsch sind"
        ],
        "correctAnswer": 1,
        "explanation": "Bei AND müssen alle verknüpften Bedingungen wahr sein. Bei OR genügt eine.",
        "aiCheck": false
      },
      {
        "id": "s13_cw1",
        "type": "code_write",
        "question": "Schreibe eine Abfrage, die name und stadt aller Kunden zurückgibt, die in Berlin wohnen UND älter als 30 sind (Spalte alter).",
        "starterCode": "-- Filtere kunden nach stadt und alter\n",
        "expectedConcepts": [
          "select",
          "from",
          "where",
          "and"
        ],
        "aiCheck": true
      },
      {
        "id": "s13_fb1",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus, um Kunden aus Berlin ODER Hamburg zu finden:",
        "template": "SELECT name FROM kunden WHERE stadt = 'Berlin' ___ stadt ___ 'Hamburg';",
        "blanks": [
          "OR",
          "="
        ],
        "aiCheck": false
      }
    ]
  },
  "sql_1_4": {
    "estimatedMinutes": 12,
    "theory": "# ORDER BY & LIMIT\n\nStandardmäßig kommen die Zeilen einer Abfrage in **keiner garantierten Reihenfolge** zurück. Mit **ORDER BY** sortierst du das Ergebnis.\n\n## Sortieren mit ORDER BY\n\n```sql\nSELECT name, preis\nFROM produkte\nORDER BY preis;\n```\n\n- **ASC** (*ascending*) = aufsteigend (klein nach groß). Das ist der **Standard**.\n- **DESC** (*descending*) = absteigend (groß nach klein).\n\n```sql\nSELECT name, preis\nFROM produkte\nORDER BY preis DESC;\n```\n\nBeispieldaten der Tabelle `produkte`:\n\n| id | name    | preis |\n|----|---------|-------|\n| 1  | Apfel   | 2     |\n| 2  | Birne   | 3     |\n| 3  | Kirsche | 5     |\n\nSortiert nach `preis DESC` käme zuerst *Kirsche*, dann *Birne*, dann *Apfel*.\n\n## Begrenzen mit LIMIT\n\n**LIMIT** begrenzt die Anzahl der zurückgegebenen Zeilen:\n\n```sql\nSELECT name, preis\nFROM produkte\nORDER BY preis DESC\nLIMIT 1;\n```\n\nDiese Abfrage liefert das **teuerste Produkt** – sortieren absteigend nach Preis und nur die erste Zeile nehmen.\n\n> 💡 Reihenfolge merken: `SELECT` → `FROM` → `WHERE` → `ORDER BY` → `LIMIT`. Diese Reihenfolge ist fest vorgeschrieben.\n\nSo kombinierst du Sortierung und Begrenzung, um z. B. die \"Top 3\" oder den günstigsten Eintrag zu finden.",
    "tasks": [
      {
        "id": "s14_q1",
        "type": "multiple_choice",
        "question": "Was ist die Standard-Sortierrichtung von ORDER BY?",
        "options": [
          "DESC (absteigend)",
          "ASC (aufsteigend)",
          "Zufällig",
          "Es gibt keine Standardrichtung"
        ],
        "correctAnswer": 1,
        "explanation": "Ohne Angabe sortiert ORDER BY aufsteigend (ASC), also von klein nach groß.",
        "aiCheck": false
      },
      {
        "id": "s14_q2",
        "type": "multiple_choice",
        "question": "Welche Abfrage liefert das teuerste Produkt?",
        "options": [
          "SELECT name FROM produkte ORDER BY preis ASC LIMIT 1;",
          "SELECT name FROM produkte ORDER BY preis DESC LIMIT 1;",
          "SELECT name FROM produkte LIMIT 1;",
          "SELECT name FROM produkte WHERE preis = teuer;"
        ],
        "correctAnswer": 1,
        "explanation": "Absteigend nach preis sortieren (DESC) und mit LIMIT 1 nur die erste, also teuerste Zeile nehmen.",
        "aiCheck": false
      },
      {
        "id": "s14_q3",
        "type": "multiple_choice",
        "question": "Welche Reihenfolge der Klauseln ist korrekt?",
        "options": [
          "SELECT ... ORDER BY ... FROM ... LIMIT ...",
          "SELECT ... FROM ... ORDER BY ... LIMIT ...",
          "FROM ... SELECT ... LIMIT ... ORDER BY ...",
          "SELECT ... LIMIT ... FROM ... ORDER BY ..."
        ],
        "correctAnswer": 1,
        "explanation": "Die feste Reihenfolge ist SELECT, FROM, (WHERE), ORDER BY, LIMIT.",
        "aiCheck": false
      },
      {
        "id": "s14_cw1",
        "type": "code_write",
        "question": "Schreibe eine Abfrage, die die 3 günstigsten Produkte (name, preis) aus der Tabelle produkte zurückgibt.",
        "starterCode": "-- Günstigste 3 Produkte\n",
        "expectedConcepts": [
          "select",
          "from",
          "order by",
          "limit"
        ],
        "aiCheck": true
      },
      {
        "id": "s14_fb1",
        "type": "fill_blank",
        "question": "Fülle die Lücken aus, um Produkte nach Preis absteigend zu sortieren und nur 5 zu zeigen:",
        "template": "SELECT name FROM produkte ORDER BY preis ___ ___ 5;",
        "blanks": [
          "DESC",
          "LIMIT"
        ],
        "aiCheck": false
      }
    ]
  }
};

/* ------------------- Fünf ausgeschriebene Lektionen -----------------------
   Handgeschrieben statt generiert: eigene Theorie, eigene Aufgaben, eigene
   Prüfkriterien. Sie decken die Stellen ab, an denen erfahrungsgemäß die
   meisten hängen bleiben.
   ------------------------------------------------------------------------- */
const WRITTEN_LESSONS = {
  html_2_2: {
    estimatedMinutes: 15,
    theory: `# Formulare & Inputs

Ein Formular ist die Stelle, an der eine Webseite zuhört statt zu erzählen. Anmeldung, Suche, Bestellung, Kommentar — alles Formulare.

## Der Rahmen: das form-Element

Alles, was zusammen abgeschickt werden soll, steht in einem \`<form>\`:

\`\`\`html
<form action="/anmelden" method="post">
  <!-- Felder kommen hier hinein -->
</form>
\`\`\`

| Attribut | Bedeutung |
|----------|-----------|
| \`action\` | Wohin die Daten geschickt werden |
| \`method\` | \`get\` (in der Adresszeile sichtbar) oder \`post\` (im Rumpf der Anfrage) |

> ⚠️ Für alles Vertrauliche — Passwörter, Adressen — immer \`post\`. Bei \`get\` landen die Daten in der URL, im Verlauf und in Server-Logs.

## Eingabefelder

Das \`<input>\`-Element ist ein leeres Element: Es hat keinen schließenden Tag. Was es tut, entscheidet \`type\`:

| type | Wofür | Was der Browser dazutut |
|------|-------|--------------------------|
| \`text\` | Freier Text | nichts Besonderes |
| \`email\` | E-Mail-Adresse | prüft grob das Format, zeigt @-Tastatur |
| \`password\` | Passwort | zeigt Punkte statt Zeichen |
| \`number\` | Zahl | Pfeile zum Hoch- und Runterzählen |
| \`checkbox\` | Ja/Nein | Kästchen zum Ankreuzen |
| \`radio\` | Eins aus mehreren | Gruppe über gleichen \`name\` |
| \`date\` | Datum | Kalender |

\`\`\`html
<input type="email" name="mail" placeholder="du@beispiel.de" required>
\`\`\`

Wichtig ist \`name\`: **Ohne \`name\` wird ein Feld nicht mitgeschickt.** Das ist der häufigste Anfängerfehler bei Formularen.

## Beschriftungen mit label

Jedes Feld braucht eine Beschriftung. Und zwar nicht als loser Text daneben, sondern mit \`<label>\`:

\`\`\`html
<label for="mail">E-Mail-Adresse</label>
<input type="email" id="mail" name="mail">
\`\`\`

Das \`for\` des Labels zeigt auf die \`id\` des Feldes. Der Nutzen ist doppelt:

- Ein Klick auf die Beschriftung setzt den Cursor ins Feld — die Trefferfläche wird größer.
- Screenreader lesen vor, wozu das Feld gehört. Ohne Label ist ein Formular für blinde Nutzerinnen und Nutzer praktisch unbenutzbar.

## Mehrzeilig und Auswahl

\`\`\`html
<textarea name="nachricht" rows="5"></textarea>

<select name="land">
  <option value="de">Deutschland</option>
  <option value="at">Österreich</option>
</select>
\`\`\`

\`<textarea>\` hat — anders als \`<input>\` — einen schließenden Tag, und der Startwert steht dazwischen, nicht in einem \`value\`-Attribut.

## Absenden

\`\`\`html
<button type="submit">Absenden</button>
\`\`\`

Steht ein \`<button>\` in einem Formular ohne \`type\`, ist er automatisch ein Absende-Knopf. Wer das nicht will, schreibt \`type="button"\`.

## Prüfen im Browser

\`required\`, \`minlength\`, \`maxlength\`, \`min\`, \`max\` und \`pattern\` lässt der Browser selbst prüfen — ohne eine Zeile JavaScript.

> ⚠️ Diese Prüfung ist Bequemlichkeit, keine Sicherheit. Sie lässt sich in zehn Sekunden umgehen. **Auf dem Server muss immer erneut geprüft werden.**`,
    tasks: [
      {
        id: "hf1", type: "multiple_choice",
        question: "Ein Eingabefeld wird beim Absenden nicht mitgeschickt. Was fehlt am wahrscheinlichsten?",
        options: ["Das `id`-Attribut", "Das `name`-Attribut", "Das `placeholder`-Attribut", "Das `class`-Attribut"],
        correctAnswer: 1,
        explanation: "Nur Felder mit `name` werden übertragen — `id` dient der Verknüpfung mit dem Label, nicht dem Absenden.",
        aiCheck: false,
      },
      {
        id: "hf2", type: "multiple_choice",
        question: "Warum sollte ein Anmeldeformular `method=\"post\"` verwenden?",
        options: [
          "Weil post schneller ist",
          "Weil die Daten sonst in der Adresszeile und im Verlauf landen",
          "Weil get keine Formulare unterstützt",
          "Weil post automatisch verschlüsselt",
        ],
        correctAnswer: 1,
        explanation: "Bei `get` stehen die Werte in der URL — sichtbar im Verlauf, in Lesezeichen und in Server-Logs. Verschlüsselt wird dadurch nichts, das macht HTTPS.",
        aiCheck: false,
      },
      {
        id: "hf3", type: "fill_blank",
        question: "Fülle die Lücken aus:",
        template: "Eine Beschriftung schreibst du mit ___ und verbindest sie über das Attribut ___ mit der id des Feldes.",
        blanks: [["label", "<label>"], ["for", 'for=']],
        aiCheck: false,
      },
      {
        id: "hf4", type: "code_write",
        question: "Schreibe ein Formular mit `method=\"post\"`, das ein beschriftetes E-Mail-Feld (id und name jeweils `mail`) und einen Absende-Knopf enthält.",
        starterCode: "",
        expectedConcepts: ["<form", "method=", "<label", "for=", "<input", 'type="email"', "name=", "<button"],
        aiCheck: false,
      },
      {
        id: "hf5", type: "explain",
        question: "Erkläre kurz, warum die Prüfung mit `required` im Browser nicht ausreicht.",
        expectedConcepts: ["Server", "umgehen", "Sicherheit"],
        aiCheck: false,
      },
    ],
  },

  css_2_1: {
    estimatedMinutes: 15,
    theory: `# Flexbox — Grundlagen

Vor Flexbox war das Ausrichten von Elementen in CSS eine Sammlung von Tricks. Heute sind es drei Zeilen.

## Der Grundgedanke

Flexbox arbeitet mit **einem Container und seinen direkten Kindern**. Der Container bekommt \`display: flex\`, die Kinder ordnen sich daraufhin entlang einer Achse an:

\`\`\`css
.container {
  display: flex;
}
\`\`\`

Ab diesem Moment stehen die Kinder **nebeneinander** statt untereinander — auch \`<div>\`-Elemente, die sonst jede Zeile für sich beanspruchen.

## Die zwei Achsen

Das ist der Punkt, an dem Flexbox klickt oder eben nicht:

- Die **Hauptachse** (main axis) verläuft in Richtung von \`flex-direction\`. Standard ist \`row\`, also von links nach rechts.
- Die **Querachse** (cross axis) steht senkrecht dazu.

\`\`\`css
.container {
  display: flex;
  flex-direction: row;     /* row | row-reverse | column | column-reverse */
}
\`\`\`

Setzt du \`flex-direction: column\`, tauschen die Achsen ihre Richtung — und damit auch die Wirkung der beiden folgenden Eigenschaften.

## Ausrichten entlang der Hauptachse

\`justify-content\` verteilt den Platz **in Richtung der Hauptachse**:

| Wert | Wirkung |
|------|---------|
| \`flex-start\` | alles an den Anfang (Standard) |
| \`center\` | alles in die Mitte |
| \`flex-end\` | alles ans Ende |
| \`space-between\` | erstes und letztes außen, Rest gleichmäßig dazwischen |
| \`space-around\` | gleicher Abstand um jedes Element |
| \`space-evenly\` | überall exakt gleicher Abstand |

## Ausrichten entlang der Querachse

\`align-items\` richtet **quer** aus:

| Wert | Wirkung |
|------|---------|
| \`stretch\` | Kinder füllen die Höhe (Standard) |
| \`center\` | mittig |
| \`flex-start\` | oben |
| \`flex-end\` | unten |

## Der Klassiker: mittig zentrieren

Beides zusammen ergibt die Antwort auf die meistgestellte CSS-Frage überhaupt:

\`\`\`css
.container {
  display: flex;
  justify-content: center;   /* waagerecht */
  align-items: center;       /* senkrecht */
  height: 100vh;
}
\`\`\`

> 💡 Ohne eine Höhe am Container gibt es senkrecht nichts zu zentrieren — der Container ist dann genau so hoch wie sein Inhalt.

## Abstände und Umbruch

\`\`\`css
.container {
  display: flex;
  gap: 16px;          /* Abstand zwischen den Kindern */
  flex-wrap: wrap;    /* umbrechen statt zusammenquetschen */
}
\`\`\`

\`gap\` ersetzt die alte Bastelei mit \`margin\` am letzten Element — es setzt Abstände **zwischen** den Kindern, aber nicht außen.

## Wachsen und schrumpfen

Am Kind, nicht am Container:

\`\`\`css
.hauptteil { flex: 1; }     /* nimmt sich den restlichen Platz */
.seitenleiste { flex: 0 0 240px; }  /* bleibt bei 240px */
\`\`\`

\`flex: 1\` ist die Kurzform für "wachse, schrumpfe, Grundbreite egal" — damit baut man Layouts, die sich von selbst anpassen.`,
    tasks: [
      {
        id: "cf1", type: "multiple_choice",
        question: "Welche Eigenschaft richtet die Kinder entlang der Hauptachse aus?",
        options: ["align-items", "justify-content", "flex-wrap", "align-content"],
        correctAnswer: 1,
        explanation: "`justify-content` wirkt auf die Hauptachse, `align-items` auf die Querachse.",
        aiCheck: false,
      },
      {
        id: "cf2", type: "multiple_choice",
        question: "Was passiert bei `flex-direction: column` mit `justify-content: center`?",
        options: [
          "Es zentriert weiterhin waagerecht",
          "Es zentriert jetzt senkrecht, weil die Hauptachse nach unten zeigt",
          "Es hat keine Wirkung mehr",
          "Es kehrt die Reihenfolge um",
        ],
        correctAnswer: 1,
        explanation: "`justify-content` folgt immer der Hauptachse — und die zeigt bei `column` nach unten.",
        aiCheck: false,
      },
      {
        id: "cf3", type: "fill_blank",
        question: "Fülle die Lücken aus:",
        template: "Ein Flex-Container entsteht mit display: ___. Den Abstand zwischen den Kindern setzt du mit ___.",
        blanks: ["flex", "gap"],
        aiCheck: false,
      },
      {
        id: "cf4", type: "code_write",
        question: "Schreibe eine Regel für `.box`, die den Inhalt waagerecht und senkrecht zentriert und 20px Abstand zwischen den Kindern lässt.",
        starterCode: ".box {\n  \n}",
        expectedConcepts: ["display", "flex", "justify-content", "center", "align-items", "gap"],
        aiCheck: false,
      },
      {
        id: "cf5", type: "explain",
        question: "Erkläre kurz, warum `align-items: center` ohne Höhe am Container oft nichts sichtbar bewirkt.",
        expectedConcepts: ["Höhe", "Inhalt", "Platz"],
        aiCheck: false,
      },
    ],
  },

  javascript_2_3: {
    estimatedMinutes: 15,
    theory: `# DOM-Manipulation

Der Browser baut aus deinem HTML einen Baum aus Objekten: das **DOM** (Document Object Model). JavaScript kann diesen Baum lesen und verändern — und genau dadurch wird aus einer Seite eine Anwendung.

## Elemente finden

\`\`\`javascript
const titel = document.getElementById("titel");
const ersteBox = document.querySelector(".box");
const alleBoxen = document.querySelectorAll(".box");
\`\`\`

| Methode | Findet | Gibt zurück |
|---------|--------|-------------|
| \`getElementById\` | genau eine id | ein Element oder \`null\` |
| \`querySelector\` | den ersten Treffer eines CSS-Selektors | ein Element oder \`null\` |
| \`querySelectorAll\` | alle Treffer | eine Liste (kein echtes Array) |

\`querySelector\` versteht jeden CSS-Selektor: \`"#titel"\`, \`".box"\`, \`"ul > li:first-child"\`.

> ⚠️ Findet der Browser nichts, kommt \`null\` zurück — und der nächste Zugriff darauf wirft \`Cannot read properties of null\`. Das ist der häufigste Fehler überhaupt beim DOM. Meist stimmt nur der Name nicht.

## Inhalt ändern

\`\`\`javascript
titel.textContent = "Neuer Text";
box.innerHTML = "<strong>Fett</strong>";
\`\`\`

Der Unterschied ist wichtig:

- \`textContent\` setzt **Text**. Zeichen wie \`<\` bleiben Zeichen.
- \`innerHTML\` setzt **HTML**. Der Browser wertet es aus.

Stammt der Inhalt von Nutzereingaben, gehört \`textContent\` benutzt. Sonst kann jemand \`<script>\` einschleusen — das nennt sich **XSS** und ist eine der häufigsten Sicherheitslücken im Web.

## Klassen und Stile

\`\`\`javascript
box.classList.add("aktiv");
box.classList.remove("versteckt");
box.classList.toggle("offen");
box.style.color = "red";
\`\`\`

Die Regel dahinter: **Aussehen gehört ins CSS.** Statt einzelne Stile per JavaScript zu setzen, schaltest du besser eine Klasse um und beschreibst das Aussehen im Stylesheet. Das bleibt lesbar, wenn die Seite wächst.

## Elemente erzeugen

\`\`\`javascript
const li = document.createElement("li");
li.textContent = "Neuer Eintrag";
document.querySelector("ul").appendChild(li);
\`\`\`

Drei Schritte, immer dieselben: erzeugen, füllen, einhängen. Vor dem \`appendChild\` existiert das Element zwar, steht aber nirgends auf der Seite.

## Attribute

\`\`\`javascript
bild.setAttribute("alt", "Ein Foto");
const ziel = link.getAttribute("href");
input.value = "";                 // Formularfelder über .value
\`\`\`

## Ein vollständiges Beispiel

\`\`\`javascript
const liste = document.querySelector("#liste");
const feld = document.querySelector("#eingabe");

document.querySelector("#hinzufuegen").addEventListener("click", () => {
  if (!feld.value.trim()) return;
  const eintrag = document.createElement("li");
  eintrag.textContent = feld.value;
  liste.appendChild(eintrag);
  feld.value = "";
});
\`\`\`

> 💡 Steht dein \`<script>\` im \`<head>\`, existieren die Elemente beim Ausführen noch nicht. Entweder das Skript ans Ende des \`<body>\` setzen oder \`defer\` benutzen.`,
    tasks: [
      {
        id: "jd1", type: "multiple_choice",
        question: "Warum ist `textContent` bei Nutzereingaben die bessere Wahl als `innerHTML`?",
        options: [
          "Es ist schneller zu tippen",
          "Es verhindert, dass eingeschleustes HTML ausgeführt wird",
          "Es funktioniert in mehr Browsern",
          "Es formatiert den Text automatisch",
        ],
        correctAnswer: 1,
        explanation: "`innerHTML` wertet HTML aus — bei fremden Eingaben öffnet das die Tür für XSS. `textContent` behandelt alles als reinen Text.",
        aiCheck: false,
      },
      {
        id: "jd2", type: "multiple_choice",
        question: "`document.getElementById(\"box\")` liefert `null`. Was ist die wahrscheinlichste Ursache?",
        options: [
          "Die Seite hat kein CSS",
          "Es gibt kein Element mit dieser id — oder das Skript läuft, bevor es existiert",
          "getElementById ist veraltet",
          "Man muss eine Raute voranstellen: getElementById(\"#box\")",
        ],
        correctAnswer: 1,
        explanation: "Entweder der Name stimmt nicht, oder das Skript läuft zu früh. Eine Raute gehört bei `getElementById` gerade NICHT davor — die braucht nur `querySelector`.",
        aiCheck: false,
      },
      {
        id: "jd3", type: "fill_blank",
        question: "Fülle die Lücken aus:",
        template: "Ein neues Element erzeugst du mit document.___(\"li\") und hängst es mit ___ in den Baum ein.",
        blanks: [["createElement", "createElement()"], ["appendChild", "appendChild()", "append"]],
        aiCheck: false,
      },
      {
        id: "jd4", type: "code_write",
        question: "Hole das Element mit der id `titel` und setze seinen Text auf `Hallo`. Nutze dafür `textContent`.",
        starterCode: "",
        expectedConcepts: [["document.getElementById", "document.querySelector"], "titel", "textContent", "Hallo"],
        aiCheck: false,
      },
      {
        id: "jd5", type: "explain",
        question: "Erkläre kurz, warum man das Aussehen besser über `classList` steuert als über `element.style`.",
        expectedConcepts: ["CSS", "Klasse", "trennen"],
        aiCheck: false,
      },
    ],
  },

  python_2_1: {
    estimatedMinutes: 15,
    theory: `# Funktionen & Parameter

Eine Funktion ist ein Stück Code mit einem Namen. Das klingt banal, ändert aber alles: Was einen Namen hat, kann man wiederverwenden, einzeln testen und später austauschen.

## Definieren und aufrufen

\`\`\`python
def begruessung(name):
    print(f"Hallo, {name}!")

begruessung("Anna")
\`\`\`

Drei Dinge sind Pflicht:

1. Das Schlüsselwort \`def\`
2. Der **Doppelpunkt** am Ende der Zeile
3. Die **Einrückung** des Rumpfes — vier Leerzeichen sind Konvention

Vergisst du den Doppelpunkt, meldet Python \`SyntaxError\`. Vergisst du die Einrückung, \`IndentationError\`. Beides sind die häufigsten Anfängerfehler in Python.

## Rückgabewerte

\`\`\`python
def addiere(a, b):
    return a + b

summe = addiere(3, 4)   # 7
\`\`\`

\`return\` beendet die Funktion sofort. Steht kein \`return\` da, gibt Python automatisch \`None\` zurück.

> ⚠️ \`print\` und \`return\` sind nicht dasselbe. \`print\` schreibt etwas auf den Bildschirm, \`return\` gibt einen Wert zurück, mit dem weitergerechnet werden kann. Eine Funktion, die nur druckt, lässt sich nicht weiterverwenden.

## Standardwerte

\`\`\`python
def begruessung(name, gruss="Hallo"):
    print(f"{gruss}, {name}!")

begruessung("Anna")              # Hallo, Anna!
begruessung("Ben", "Moin")       # Moin, Ben!
\`\`\`

Parameter mit Standardwert müssen **hinter** denen ohne stehen.

> ⚠️ Nimm niemals eine Liste als Standardwert (\`def f(x=[])\`). Sie wird nur **einmal** erzeugt und bleibt zwischen den Aufrufen bestehen — ein Klassiker unter den schwer zu findenden Fehlern. Nimm \`None\` und lege die Liste im Rumpf an.

## Benannte Argumente

\`\`\`python
def rechteck(breite, hoehe):
    return breite * hoehe

rechteck(hoehe=3, breite=4)    # Reihenfolge egal
\`\`\`

Das lohnt sich besonders bei Wahrheitswerten: \`sortiere(daten, absteigend=True)\` liest sich deutlich besser als \`sortiere(daten, True)\`.

## Beliebig viele Argumente

\`\`\`python
def summe(*zahlen):
    return sum(zahlen)

summe(1, 2, 3)      # 6

def info(**angaben):
    for schluessel, wert in angaben.items():
        print(schluessel, wert)

info(name="Anna", alter=17)
\`\`\`

\`*args\` sammelt Einzelwerte in einem Tupel, \`**kwargs\` benannte Argumente in einem Dictionary.

## Docstrings

\`\`\`python
def flaeche(radius):
    """Berechnet die Kreisfläche für den gegebenen Radius."""
    return 3.14159 * radius ** 2
\`\`\`

Der Text direkt unter der Definition ist die eingebaute Dokumentation. \`help(flaeche)\` zeigt ihn an — deutlich praktischer als ein Kommentar daneben.

## Sichtbarkeit

Was in einer Funktion entsteht, existiert nur dort:

\`\`\`python
def f():
    x = 5
    print(x)     # geht

f()
print(x)         # NameError: name 'x' is not defined
\`\`\`

Das ist Absicht: Funktionen sollen sich nicht gegenseitig in die Quere kommen.`,
    tasks: [
      {
        id: "pf1", type: "multiple_choice",
        question: "Was gibt eine Funktion ohne `return` zurück?",
        options: ["0", "None", "Eine leere Zeichenkette", "Sie wirft einen Fehler"],
        correctAnswer: 1,
        explanation: "Ohne `return` liefert Python automatisch `None`.",
        aiCheck: false,
      },
      {
        id: "pf2", type: "multiple_choice",
        question: "Warum ist `def merke(eintrag, liste=[])` eine schlechte Idee?",
        options: [
          "Listen sind als Parameter nicht erlaubt",
          "Die Liste wird nur einmal erzeugt und behält Werte zwischen den Aufrufen",
          "Es ist langsamer als ein Tupel",
          "Der Standardwert wird ignoriert",
        ],
        correctAnswer: 1,
        explanation: "Standardwerte werden einmal bei der Definition ausgewertet. Eine veränderbare Liste sammelt dadurch über alle Aufrufe hinweg Werte an.",
        aiCheck: false,
      },
      {
        id: "pf3", type: "fill_blank",
        question: "Fülle die Lücken aus:",
        template: "Eine Funktion beginnt mit ___, endet die Kopfzeile mit einem ___ und der Rumpf muss ___ sein.",
        blanks: ["def", ["Doppelpunkt", ":"], ["eingerückt", "eingerueckt", "einrücken"]],
        aiCheck: false,
      },
      {
        id: "pf4", type: "code_write",
        question: "Schreibe eine Funktion `verdopple`, die eine Zahl entgegennimmt und das Doppelte **zurückgibt** (nicht ausgibt).",
        starterCode: "",
        expectedConcepts: ["def", "verdopple", "return"],
        aiCheck: false,
      },
      {
        id: "pf5", type: "explain",
        question: "Erkläre kurz den Unterschied zwischen `print` und `return`.",
        expectedConcepts: ["Bildschirm", "Wert", "weiterverwenden"],
        aiCheck: false,
      },
    ],
  },

  sql_2_3: {
    estimatedMinutes: 16,
    theory: `# JOINs (INNER, LEFT, RIGHT)

Daten liegen in Datenbanken selten in einer einzigen Tabelle. Kunden hier, Bestellungen dort — verbunden über eine gemeinsame Spalte. Ein JOIN führt sie wieder zusammen.

## Die Ausgangslage

\`\`\`
kunden                      bestellungen
+----+----------+           +----+-----------+--------+
| id | name     |           | id | kunden_id | betrag |
+----+----------+           +----+-----------+--------+
| 1  | Anna     |           | 10 | 1         | 49.90  |
| 2  | Ben      |           | 11 | 1         | 12.00  |
| 3  | Clara    |           | 12 | 2         | 99.00  |
+----+----------+           +----+-----------+--------+
\`\`\`

Clara hat nichts bestellt. Das wird gleich wichtig.

## INNER JOIN — nur was zusammenpasst

\`\`\`sql
SELECT kunden.name, bestellungen.betrag
FROM kunden
INNER JOIN bestellungen ON kunden.id = bestellungen.kunden_id;
\`\`\`

Ergebnis:

| name | betrag |
|------|--------|
| Anna | 49.90 |
| Anna | 12.00 |
| Ben  | 99.00 |

**Clara fehlt.** Ein INNER JOIN liefert nur Zeilen, für die es auf **beiden** Seiten einen Treffer gibt. Anna erscheint zweimal, weil sie zwei Bestellungen hat — eine Zeile je Kombination.

Der Teil nach \`ON\` ist die Bedingung, über die verbunden wird. Sie ist keine Formalie: Lässt man sie weg, kombiniert die Datenbank **jede** Zeile mit jeder — bei 1.000 × 1.000 Zeilen sind das eine Million Ergebniszeilen.

## LEFT JOIN — alles von links

\`\`\`sql
SELECT kunden.name, bestellungen.betrag
FROM kunden
LEFT JOIN bestellungen ON kunden.id = bestellungen.kunden_id;
\`\`\`

| name  | betrag |
|-------|--------|
| Anna  | 49.90 |
| Anna  | 12.00 |
| Ben   | 99.00 |
| Clara | NULL |

Alle Zeilen der **linken** Tabelle bleiben erhalten. Wo rechts nichts passt, steht \`NULL\`.

Damit beantwortet man Fragen wie „Wer hat noch **nie** bestellt?":

\`\`\`sql
SELECT kunden.name
FROM kunden
LEFT JOIN bestellungen ON kunden.id = bestellungen.kunden_id
WHERE bestellungen.id IS NULL;
\`\`\`

> ⚠️ Auf \`NULL\` prüft man mit \`IS NULL\`, niemals mit \`= NULL\`. Ein Vergleich mit \`NULL\` ergibt weder wahr noch falsch — er ergibt „unbekannt", und die Zeile fällt heraus.

## RIGHT JOIN — alles von rechts

Dasselbe in die andere Richtung: Alle Zeilen der rechten Tabelle bleiben erhalten. In der Praxis selten, weil man die Tabellen genauso gut tauschen und einen LEFT JOIN schreiben kann — das liest sich für die meisten leichter.

## Kurznamen sparen Tipparbeit

\`\`\`sql
SELECT k.name, SUM(b.betrag) AS umsatz
FROM kunden AS k
LEFT JOIN bestellungen AS b ON k.id = b.kunden_id
GROUP BY k.id, k.name
ORDER BY umsatz DESC;
\`\`\`

## Die Reihenfolge der Auswertung

Nicht die Schreibreihenfolge zählt, sondern diese:

1. \`FROM\` und \`JOIN\` — Tabellen verbinden
2. \`WHERE\` — Zeilen aussortieren
3. \`GROUP BY\` — zusammenfassen
4. \`HAVING\` — Gruppen aussortieren
5. \`SELECT\` — Spalten auswählen
6. \`ORDER BY\` — sortieren

Daraus folgt eine wichtige Regel: Eine Bedingung auf die rechte Tabelle gehört bei einem LEFT JOIN in die \`ON\`-Klausel, nicht ins \`WHERE\`. Steht sie im \`WHERE\`, filtert sie die \`NULL\`-Zeilen wieder heraus — und aus dem LEFT JOIN wird stillschweigend ein INNER JOIN.`,
    tasks: [
      {
        id: "sj1", type: "multiple_choice",
        question: "Welche Kunden fehlen im Ergebnis eines INNER JOIN mit Bestellungen?",
        options: [
          "Die mit den meisten Bestellungen",
          "Die ohne jede Bestellung",
          "Keine, INNER JOIN zeigt immer alle",
          "Die mit mehr als einer Bestellung",
        ],
        correctAnswer: 1,
        explanation: "Ein INNER JOIN behält nur Zeilen mit Treffer auf beiden Seiten — wer nichts bestellt hat, fällt heraus.",
        aiCheck: false,
      },
      {
        id: "sj2", type: "multiple_choice",
        question: "Wie findest du Kunden ohne Bestellung?",
        options: [
          "INNER JOIN mit WHERE betrag = 0",
          "LEFT JOIN und WHERE bestellungen.id IS NULL",
          "RIGHT JOIN ohne ON",
          "SELECT DISTINCT auf die Kundentabelle",
        ],
        correctAnswer: 1,
        explanation: "Der LEFT JOIN behält alle Kunden; wo rechts nichts passt, steht NULL — genau danach filtert man.",
        aiCheck: false,
      },
      {
        id: "sj3", type: "fill_blank",
        question: "Fülle die Lücken aus:",
        template: "Die Verbindungsbedingung steht nach dem Schlüsselwort ___. Auf fehlende Werte prüfst du mit ___.",
        blanks: [["ON", "on"], ["IS NULL", "is null"]],
        aiCheck: false,
      },
      {
        id: "sj4", type: "code_write",
        question: "Hole die Namen aller Kunden zusammen mit dem Betrag ihrer Bestellungen — auch die Kunden ohne Bestellung sollen erscheinen.",
        starterCode: "",
        expectedConcepts: ["SELECT", "FROM", "kunden", "LEFT", "JOIN", "bestellungen", "ON"],
        aiCheck: false,
      },
      {
        id: "sj5", type: "explain",
        question: "Erkläre kurz, warum eine Bedingung auf die rechte Tabelle im WHERE aus einem LEFT JOIN faktisch einen INNER JOIN macht.",
        expectedConcepts: ["NULL", "filtert", "Reihenfolge"],
        aiCheck: false,
      },
    ],
  },
};

// Alle handgemachten Inhalte zusammenführen
const LESSON_CONTENT = { ...EXTRA_LESSONS, ...BASE_LESSONS, ...WRITTEN_LESSONS };

/* ------------------- Übungen für Lektionen ohne Inhalt --------------------
   Nicht jede Lektion hat handgeschriebene Aufgaben. Statt überall dieselbe
   Erklär-Aufgabe zu stellen, gibt es hier je Sprache echte Übungen:
   ein Lückentext zur Syntax und eine kleine Code-Aufgabe. Das ist deutlich
   näher am Programmieren als „Erkläre in eigenen Worten“.
   ------------------------------------------------------------------------- */
const COURSE_PRACTICE = {
  html: {
    blank: { template: "Ein Absatz steht zwischen ___ und ___.", blanks: ["<p>", "</p>"] },
    code: { question: "Schreibe eine Überschrift erster Ordnung mit dem Text **Hallo**.", concepts: ["<h1>", "</h1>", "Hallo"] },
    mc: { question: "Wofür steht das `alt`-Attribut bei einem Bild?",
      options: ["Für die Bildgröße", "Für einen Alternativtext, wenn das Bild fehlt", "Für die Ausrichtung", "Für den Dateipfad"],
      correct: 1, why: "Der Alternativtext beschreibt das Bild — für Screenreader und wenn das Bild nicht lädt." },
  },
  css: {
    blank: { template: "Die Textfarbe setzt du mit ___, den Hintergrund mit ___.", blanks: ["color", ["background", "background-color"]] },
    code: { question: "Gib allen Absätzen die Schriftgröße `16px`.", concepts: ["p", "font-size", "16px"] },
    mc: { question: "Welcher Selektor spricht die Klasse `box` an?",
      options: ["#box", ".box", "box", "*box"], correct: 1, why: "Klassen beginnen mit einem Punkt, IDs mit einer Raute." },
  },
  javascript: {
    blank: { template: "Eine Konstante deklarierst du mit ___, eine änderbare Variable mit ___.", blanks: ["const", "let"] },
    code: { question: "Lege eine Konstante `name` mit deinem Namen an und gib sie in der Konsole aus.", concepts: ["const", "name", "console.log"] },
    mc: { question: "Was gibt `typeof 42` zurück?",
      options: ['"integer"', '"number"', '"float"', '"42"'], correct: 1, why: "JavaScript kennt nur einen Zahlentyp: number." },
  },
  typescript: {
    blank: { template: "Ein Typ wird nach einem ___ notiert, z.B. `alter: number`. Ein eigener Typ entsteht mit ___.", blanks: [":", ["type", "interface"]] },
    code: { question: "Schreibe eine Funktion `verdopple`, die eine `number` entgegennimmt und eine `number` zurückgibt.", concepts: ["function", "verdopple", "number", "return"] },
    mc: { question: "Was bewirkt `strict` in der tsconfig?",
      options: ["Schnellere Kompilierung", "Strengere Typprüfungen", "Kleinere Ausgabedateien", "Automatisches Formatieren"],
      correct: 1, why: "strict aktiviert unter anderem strikte Null-Prüfungen — das fängt viele Fehler früh ab." },
  },
  react: {
    blank: { template: "Zustand bekommst du mit ___, Seiteneffekte mit ___.", blanks: ["useState", "useEffect"] },
    code: { question: "Schreibe eine Komponente `Hallo`, die `<h1>Hallo</h1>` zurückgibt.", concepts: ["function", "Hallo", "return", "<h1>"] },
    mc: { question: "Warum braucht jede Liste in React ein `key`?",
      options: ["Für die Sortierung", "Damit React Elemente wiedererkennt", "Für CSS-Klassen", "Es ist optional"],
      correct: 1, why: "Der Key sagt React, welches Element welches ist — sonst wird beim Aktualisieren zu viel neu gebaut." },
  },
  vue: {
    blank: { template: "Eine Bedingung schreibst du mit ___, eine Schleife mit ___.", blanks: ["v-if", "v-for"] },
    code: { question: "Binde die Variable `titel` in einer Überschrift aus (Interpolation).", concepts: ["<h1>", "{{", "titel", "}}"] },
    mc: { question: "Wofür steht `v-model`?",
      options: ["Nur Ausgabe", "Zweiwege-Bindung an ein Formularfeld", "Ein Datenbankmodell", "Ein Styling-Helfer"],
      correct: 1, why: "v-model verbindet Feld und Daten in beide Richtungen." },
  },
  python: {
    blank: { template: "Eine Funktion beginnt mit ___, ausgegeben wird mit ___.", blanks: ["def", ["print", "print()"]] },
    code: { question: "Schreibe eine Funktion `begruessung`, die `Hallo` ausgibt.", concepts: ["def", "begruessung", "print"] },
    mc: { question: "Wodurch werden Blöcke in Python abgegrenzt?",
      options: ["Geschweifte Klammern", "Einrückung", "Semikolons", "begin/end"],
      correct: 1, why: "Python nutzt die Einrückung — deshalb ist sie dort nicht nur Kosmetik." },
  },
  java: {
    blank: { template: "Die Einstiegsmethode heißt ___ und liegt in einer ___.", blanks: ["main", ["Klasse", "class"]] },
    code: { question: "Schreibe eine Klasse `Start` mit einer `main`-Methode, die `Hallo` ausgibt.", concepts: ["class", "Start", "main", "System.out.println"] },
    mc: { question: "Was bedeutet `static` bei einer Methode?",
      options: ["Sie ist unveränderlich", "Sie gehört zur Klasse, nicht zum Objekt", "Sie ist privat", "Sie läuft schneller"],
      correct: 1, why: "Statische Methoden rufst du ohne Objekt auf — deshalb ist main statisch." },
  },
  kotlin: {
    blank: { template: "Unveränderlich deklarierst du mit ___, veränderbar mit ___.", blanks: ["val", "var"] },
    code: { question: "Schreibe eine Funktion `gruss`, die `Hallo` ausgibt.", concepts: ["fun", "gruss", "println"] },
    mc: { question: "Was bedeutet der Typ `String?`",
      options: ["Ein Text-Array", "Ein Text, der auch null sein darf", "Ein optionaler Parameter", "Ein Zeichen"],
      correct: 1, why: "Das Fragezeichen erlaubt null — Kotlins Null-Sicherheit macht das sichtbar." },
  },
  c: {
    blank: { template: "Ein- und Ausgabe bindest du mit `#include` ___ ein, ausgegeben wird mit ___.", blanks: [["<stdio.h>", "stdio.h"], ["printf", "printf()"]] },
    code: { question: "Schreibe ein vollständiges Programm, das `Hallo` ausgibt.", concepts: ["#include", "int main", "printf", "return"] },
    mc: { question: "Was steht in einem Zeiger?",
      options: ["Eine Kopie des Wertes", "Eine Speicheradresse", "Der Datentyp", "Die Größe in Bytes"],
      correct: 1, why: "Ein Zeiger speichert die Adresse — über sie kommst du an den Wert." },
  },
  cpp: {
    blank: { template: "Text gibst du mit ___ aus, eingelesen wird mit ___.", blanks: [["std::cout", "cout"], ["std::cin", "cin"]] },
    code: { question: "Schreibe ein Programm, das `Hallo` auf der Konsole ausgibt.", concepts: ["#include", "int main", ["cout", "std::cout"], "return"] },
    mc: { question: "Wofür stehen Smart Pointer?",
      options: ["Schnellere Zeiger", "Automatische Speicherfreigabe", "Zeiger auf Funktionen", "Zeiger mit Typprüfung"],
      correct: 1, why: "unique_ptr und shared_ptr geben den Speicher selbst wieder frei." },
  },
  go: {
    blank: { template: "Eine Funktion beginnt mit ___, ein Paket wird mit ___ deklariert.", blanks: ["func", "package"] },
    code: { question: "Schreibe ein Programm im Paket `main`, das `Hallo` ausgibt.", concepts: ["package", "func main", ["fmt.Println", "Println"]] },
    mc: { question: "Was startet `go func() { ... }()`?",
      options: ["Einen neuen Prozess", "Eine Goroutine", "Einen Thread-Pool", "Eine Endlosschleife"],
      correct: 1, why: "Goroutinen sind sehr leichtgewichtig — davon laufen problemlos Tausende." },
  },
  rust: {
    blank: { template: "Eine Funktion beginnt mit ___, veränderbar wird eine Variable mit ___.", blanks: ["fn", "mut"] },
    code: { question: "Schreibe ein Programm, das `Hallo` ausgibt.", concepts: ["fn main", ["println!", "println"]] },
    mc: { question: "Was besagt Ownership?",
      options: ["Jeder Wert hat genau einen Eigentümer", "Werte sind immer unveränderlich", "Speicher wird nie freigegeben", "Nur Funktionen besitzen Werte"],
      correct: 0, why: "Genau ein Eigentümer — endet dessen Gültigkeit, wird der Speicher freigegeben." },
  },
  php: {
    blank: { template: "Ein PHP-Block beginnt mit ___, Variablen beginnen mit ___.", blanks: ["<?php", "$"] },
    code: { question: "Gib den Text `Hallo` mit PHP aus.", concepts: ["<?php", ["echo", "print"], "Hallo"] },
    mc: { question: "Womit verhinderst du SQL-Injection am zuverlässigsten?",
      options: ["Eingaben kürzen", "Vorbereitete Anweisungen (Prepared Statements)", "Anführungszeichen verdoppeln", "Kleinbuchstaben erzwingen"],
      correct: 1, why: "Prepared Statements trennen Befehl und Daten — dann kann Eingabe kein Befehl mehr werden." },
  },
  sql: {
    blank: { template: "Spalten wählst du mit ___ aus, die Tabelle folgt nach ___.", blanks: ["SELECT", "FROM"] },
    code: { question: "Hole alle Spalten aus der Tabelle `kunden`.", concepts: ["SELECT", "FROM", "kunden"] },
    mc: { question: "Was macht ein `INNER JOIN`?",
      options: ["Alle Zeilen beider Tabellen", "Nur Zeilen mit Treffer in beiden Tabellen", "Nur die linke Tabelle", "Er entfernt Duplikate"],
      correct: 1, why: "Ohne Treffer auf beiden Seiten fällt die Zeile heraus." },
  },
};

const DEFAULT_PRACTICE_SET = {
  blank: { template: "Ein Kommentar dient dazu, Code zu ___ — ausgeführt wird er ___.", blanks: [["erklären", "beschreiben", "dokumentieren"], ["nicht", "nie"]] },
  code: { question: "Schreibe eine kleine, lauffähige Zeile Code zu diesem Thema.", concepts: [] },
  mc: { question: "Was hilft beim Lernen einer Programmiersprache am meisten?",
    options: ["Nur lesen", "Selbst schreiben und ausprobieren", "Videos ansehen", "Auswendig lernen"],
    correct: 1, why: "Programmieren lernt man durch Programmieren." },
};

/* --------------------- Weitere Übungssätze je Sprache ---------------------
   Damit nicht jede generierte Lektion dieselben drei Aufgaben zeigt, gibt es
   je Sprache mehrere Sätze. Verteilt werden sie über `lessonHash`, also
   stabil: dieselbe Lektion zeigt immer dieselben Aufgaben.
   ------------------------------------------------------------------------- */
const PRACTICE_BANK = {
  html: [
    {
      blank: { template: "Eine ungeordnete Liste steht in ___, jeder Eintrag in ___.", blanks: [["<ul>", "ul"], ["<li>", "li"]] },
      code: { question: "Schreibe eine Liste mit den beiden Einträgen **Apfel** und **Birne**.", concepts: ["<ul>", "</ul>", "<li>", "</li>", "Apfel", "Birne"] },
      mc: { question: "Was unterscheidet `<ol>` von `<ul>`?",
        options: ["<ol> ist nummeriert, <ul> nicht", "<ol> darf nur drei Einträge haben", "<ul> ist veraltet", "Es gibt keinen Unterschied"],
        correct: 0, why: "`ol` steht für ordered list — die Einträge werden durchnummeriert." },
    },
    {
      blank: { template: "Der sichtbare Inhalt steht im ___, Titel und Meta-Angaben im ___.", blanks: [["<body>", "body"], ["<head>", "head"]] },
      code: { question: "Schreibe ein vollständiges HTML-Dokument mit dem Titel **Meine Seite**.", concepts: ["<!DOCTYPE", "<html", "<head>", "<title>", "Meine Seite", "<body>"] },
      mc: { question: "Wozu dient `<meta charset=\"UTF-8\">`?",
        options: ["Es setzt die Schriftart", "Es legt die Zeichenkodierung fest", "Es aktiviert JavaScript", "Es beschleunigt das Laden"],
        correct: 1, why: "Ohne die richtige Kodierung erscheinen Umlaute als kaputte Zeichen." },
    },
    {
      blank: { template: "Ein Bereich mit eigener Bedeutung heißt ___, die Hauptnavigation ___.", blanks: [["<section>", "section"], ["<nav>", "nav"]] },
      code: { question: "Schreibe eine Navigation mit einem Link zur Startseite.", concepts: ["<nav>", "</nav>", "<a", "href="] },
      mc: { question: "Warum ist `<nav>` besser als `<div class=\"nav\">`?",
        options: ["Es ist kürzer zu tippen", "Screenreader und Suchmaschinen erkennen die Bedeutung", "Es lädt schneller", "Es braucht kein CSS"],
        correct: 1, why: "Semantische Elemente tragen ihre Bedeutung im Namen — davon profitiert vor allem die Barrierefreiheit." },
    },
  ],

  css: [
    {
      blank: { template: "Innenabstand setzt du mit ___, Außenabstand mit ___.", blanks: ["padding", "margin"] },
      code: { question: "Gib der Klasse `karte` 16px Innenabstand und einen 1px breiten grauen Rahmen.", concepts: [".karte", "padding", "16px", "border", "1px"] },
      mc: { question: "Was zählt bei `box-sizing: border-box` zur angegebenen Breite?",
        options: ["Nur der Inhalt", "Inhalt, Innenabstand und Rahmen", "Nur der Inhalt und der Rahmen", "Auch der Außenabstand"],
        correct: 1, why: "Mit border-box bleibt die angegebene Breite die tatsächliche — Padding und Border fressen sie nicht auf." },
    },
    {
      blank: { template: "Ein Element aus dem Fluss nimmst du mit position: ___, am Fenster verankert wird es mit position: ___.", blanks: ["absolute", "fixed"] },
      code: { question: "Positioniere `.hinweis` fest 20px vom oberen und rechten Rand.", concepts: [".hinweis", "position", "fixed", "top", "right", "20px"] },
      mc: { question: "Worauf bezieht sich `position: absolute`?",
        options: ["Immer auf das Fenster", "Auf den nächsten positionierten Vorfahren", "Auf das body-Element", "Auf das direkte Elternelement"],
        correct: 1, why: "Gesucht wird der nächste Vorfahre, dessen position nicht static ist — sonst der Ursprung des Dokuments." },
    },
    {
      blank: { template: "Ein Raster erzeugst du mit display: ___, die Spalten legst du mit ___ fest.", blanks: ["grid", "grid-template-columns"] },
      code: { question: "Baue mit `.raster` drei gleich breite Spalten mit 12px Abstand.", concepts: [".raster", "display", "grid", "grid-template-columns", "gap"] },
      mc: { question: "Was bedeutet die Einheit `1fr` im Grid?",
        options: ["Ein festes Pixelmaß", "Einen Anteil am freien Platz", "Eine Prozentangabe", "Die Schriftgröße"],
        correct: 1, why: "fr steht für fraction — der übrige Platz wird nach diesen Anteilen verteilt." },
    },
  ],

  javascript: [
    {
      blank: { template: "Über ein Array läufst du mit ___, ein neues Array entsteht mit ___.", blanks: [["forEach", "for"], "map"] },
      code: { question: "Verdopple jede Zahl im Array `zahlen` und speichere das Ergebnis in `doppelt`.", concepts: ["zahlen", "map", "doppelt", ["const", "let"]] },
      mc: { question: "Was gibt `[1,2,3].filter(n => n > 1)` zurück?",
        options: ["true", "[2, 3]", "2", "[1]"],
        correct: 1, why: "filter liefert ein neues Array mit allen Elementen, für die die Funktion true ergibt." },
    },
    {
      blank: { template: "Eine Bedingung schreibst du mit ___, den Gegenfall mit ___.", blanks: ["if", "else"] },
      code: { question: "Prüfe, ob `alter` mindestens 18 ist, und gib „volljährig“ oder „minderjährig“ aus.", concepts: ["if", "alter", "18", "else", "console.log"] },
      mc: { question: "Warum sollte man `===` statt `==` verwenden?",
        options: ["Es ist schneller", "Es vergleicht ohne stillschweigende Typumwandlung", "Es funktioniert auch mit Objekten", "== ist veraltet"],
        correct: 1, why: '`"1" == 1` ist true, `"1" === 1` ist false — die Typumwandlung von == überrascht regelmäßig.' },
    },
    {
      blank: { template: "Auf ein Ereignis reagierst du mit ___, ein Element findest du mit ___.", blanks: ["addEventListener", ["querySelector", "getElementById"]] },
      code: { question: "Reagiere auf einen Klick auf das Element mit der id `knopf` und gib „Hallo“ aus.", concepts: [["document.querySelector", "document.getElementById"], "knopf", "addEventListener", "click", "console.log", "Hallo"] },
      mc: { question: "Was macht `event.preventDefault()`?",
        options: ["Es stoppt das Skript", "Es verhindert die Standardaktion des Browsers", "Es löscht das Element", "Es hält die Ereignisweitergabe an"],
        correct: 1, why: "Bei einem Formular verhindert es etwa das Neuladen der Seite. Die Weitergabe stoppt stopPropagation." },
    },
  ],

  typescript: [
    {
      blank: { template: "Ein Array aus Zahlen schreibst du als ___, ein Wert, der fehlen darf, bekommt ein ___ hinter den Namen.", blanks: [["number[]", "Array<number>"], "?"] },
      code: { question: "Schreibe ein Interface `Person` mit `name: string` und optionalem `alter: number`.", concepts: ["interface", "Person", "name", "string", "alter", "number"] },
      mc: { question: "Was ist der Unterschied zwischen `unknown` und `any`?",
        options: ["Keiner", "unknown erzwingt eine Prüfung vor der Nutzung", "any ist sicherer", "unknown gibt es nur in Klassen"],
        correct: 1, why: "any schaltet die Prüfung ab, unknown zwingt dich, den Typ erst einzugrenzen." },
    },
    {
      blank: { template: "Mehrere erlaubte Typen verbindest du mit ___, einen eigenen Namen vergibst du mit ___.", blanks: ["|", ["type", "interface"]] },
      code: { question: "Definiere einen Typ `Status`, der nur `\"offen\"` oder `\"fertig\"` sein darf.", concepts: ["type", "Status", "offen", "fertig", "|"] },
      mc: { question: "Wozu dient `as const`?",
        options: ["Es macht Variablen schneller", "Es macht Werte unveränderlich und den Typ so eng wie möglich", "Es erzwingt eine Klasse", "Es entfernt Typen zur Laufzeit"],
        correct: 1, why: 'Ohne as const wird `\"offen\"` zu string verallgemeinert — mit bleibt es der exakte Wert.' },
    },
  ],

  react: [
    {
      blank: { template: "Eine Liste braucht je Element ein ___, damit React die Elemente wiedererkennt.", blanks: ["key"] },
      code: { question: "Rendere aus dem Array `namen` eine Liste von `<li>`-Elementen mit key.", concepts: ["namen", "map", "<li", "key", "{"] },
      mc: { question: "Warum darf man den Array-Index selten als key nehmen?",
        options: ["Er ist zu lang", "Beim Umsortieren zeigt er auf das falsche Element", "React verbietet Zahlen", "Er ist nicht eindeutig genug für CSS"],
        correct: 1, why: "Ändert sich die Reihenfolge, wandert der Index — React ordnet dann Zustand dem falschen Eintrag zu." },
    },
    {
      blank: { template: "Werte von außen heißen ___, eigener Zustand entsteht mit ___.", blanks: ["props", "useState"] },
      code: { question: "Schreibe eine Komponente `Zaehler` mit einem Zustand `wert`, der bei Klick um eins steigt.", concepts: ["function", "Zaehler", "useState", "wert", "onClick", "return"] },
      mc: { question: "Wann läuft `useEffect(fn, [])`?",
        options: ["Bei jedem Rendern", "Genau einmal nach dem ersten Rendern", "Nie", "Nur beim Entfernen"],
        correct: 1, why: "Eine leere Abhängigkeitsliste bedeutet: keine Abhängigkeit ändert sich je, also nur einmal." },
    },
  ],

  vue: [
    {
      blank: { template: "Einen Wert bindest du an ein Attribut mit ___, an ein Formularfeld mit ___.", blanks: [[":", "v-bind"], "v-model"] },
      code: { question: "Binde die Variable `text` an ein Eingabefeld.", concepts: ["<input", "v-model", "text"] },
      mc: { question: "Was macht `computed` gegenüber einer normalen Methode?",
        options: ["Nichts", "Es merkt sich das Ergebnis, bis sich eine Abhängigkeit ändert", "Es läuft asynchron", "Es kann keine Werte zurückgeben"],
        correct: 1, why: "Computed-Werte werden zwischengespeichert und nur bei Bedarf neu berechnet." },
    },
    {
      blank: { template: "Eine Liste rendest du mit ___, eine Bedingung mit ___.", blanks: ["v-for", "v-if"] },
      code: { question: "Gib jeden Eintrag aus `punkte` als Listenelement aus.", concepts: ["<li", "v-for", "punkte", ":key"] },
      mc: { question: "Warum sollte man `v-if` und `v-for` nicht am selben Element benutzen?",
        options: ["Es ist verboten", "Die Auswertungsreihenfolge führt zu unerwartetem Verhalten", "Es ist zu lang", "v-if kennt keine Listen"],
        correct: 1, why: "Besser ein umschließendes template mit v-if oder vorher filtern." },
    },
  ],

  python: [
    {
      blank: { template: "Über eine Liste läufst du mit ___, die Länge bekommst du mit ___.", blanks: [["for", "for-Schleife"], ["len", "len()"]] },
      code: { question: "Gib jeden Eintrag der Liste `namen` einzeln aus.", concepts: ["for", "namen", "print"] },
      mc: { question: "Was ergibt `list(range(3))`?",
        options: ["[1, 2, 3]", "[0, 1, 2]", "[0, 1, 2, 3]", "3"],
        correct: 1, why: "range beginnt bei 0 und endet vor der angegebenen Zahl." },
    },
    {
      blank: { template: "Ein Wörterbuch schreibst du mit geschweiften ___, auf einen Wert greifst du über den ___ zu.", blanks: [["Klammern", "{}"], ["Schlüssel", "Key"]] },
      code: { question: "Lege ein Dictionary `person` mit dem Schlüssel `name` an und gib den Wert aus.", concepts: ["person", "name", "print", "{"] },
      mc: { question: "Was passiert bei `d[\"fehlt\"]`, wenn der Schlüssel nicht existiert?",
        options: ["Es kommt None zurück", "Es wird ein KeyError ausgelöst", "Der Schlüssel wird angelegt", "Es kommt 0 zurück"],
        correct: 1, why: "Wer einen Standardwert will, nimmt `d.get(\"fehlt\", 0)`." },
    },
    {
      blank: { template: "Eine Klasse beginnt mit ___, der Konstruktor heißt ___.", blanks: ["class", ["__init__", "init"]] },
      code: { question: "Schreibe eine Klasse `Hund` mit einem Konstruktor, der `name` speichert.", concepts: ["class", "Hund", "def", "__init__", "self", "name"] },
      mc: { question: "Wofür steht `self` in einer Methode?",
        options: ["Für die Klasse selbst", "Für das konkrete Objekt", "Für das Modul", "Es ist optional und bedeutungslos"],
        correct: 1, why: "self ist die Instanz, auf der die Methode aufgerufen wurde." },
    },
  ],

  java: [
    {
      blank: { template: "Eine Liste deklarierst du als ___, hinzugefügt wird mit ___.", blanks: [["List", "ArrayList"], ["add", "add()"]] },
      code: { question: "Lege eine `ArrayList<String>` namens `namen` an und füge `\"Anna\"` hinzu.", concepts: ["ArrayList", "String", "namen", "add", "Anna"] },
      mc: { question: "Was ist der Unterschied zwischen `int` und `Integer`?",
        options: ["Keiner", "int ist ein primitiver Typ, Integer ein Objekt", "Integer ist schneller", "int kann null sein"],
        correct: 1, why: "Nur Integer kann null sein — genau daraus entstehen NullPointerExceptions beim Auspacken." },
    },
    {
      blank: { template: "Eine Schleife über alle Elemente schreibst du mit ___, verglichen werden Zeichenketten mit ___.", blanks: [["for", "for-each"], ["equals", "equals()"]] },
      code: { question: "Gib jedes Element des Arrays `zahlen` mit einer for-Schleife aus.", concepts: ["for", "zahlen", "System.out.println"] },
      mc: { question: "Warum vergleicht man Strings nicht mit `==`?",
        options: ["Es ist langsamer", "== vergleicht die Referenz, nicht den Inhalt", "== gibt es für Strings nicht", "Es ist nur Stilfrage"],
        correct: 1, why: "Zwei gleich aussehende Strings können unterschiedliche Objekte sein — equals vergleicht den Inhalt." },
    },
  ],

  kotlin: [
    {
      blank: { template: "Eine Datenklasse deklarierst du mit ___, sicher auf null zugreifen kannst du mit ___.", blanks: [["data class", "data"], ["?.", "?"]] },
      code: { question: "Schreibe eine Data Class `Punkt` mit den Feldern `x` und `y` vom Typ Int.", concepts: ["data", "class", "Punkt", "val", "Int"] },
      mc: { question: "Was liefert der Elvis-Operator `?:`",
        options: ["Den linken Wert oder, falls null, den rechten", "Immer den rechten Wert", "Eine Ausnahme bei null", "Einen Booleschen Wert"],
        correct: 0, why: "`name ?: \"unbekannt\"` liefert den Ersatzwert nur, wenn links null steht." },
    },
    {
      blank: { template: "Eine Verzweigung über viele Fälle schreibst du mit ___, eine Schleife über eine Liste mit ___.", blanks: ["when", ["for", "forEach"]] },
      code: { question: "Gib jeden Eintrag der Liste `woerter` aus.", concepts: ["for", "woerter", "println"] },
      mc: { question: "Was bedeutet `lateinit`?",
        options: ["Der Wert ist unveränderlich", "Die Initialisierung erfolgt später, aber vor der ersten Nutzung", "Der Typ wird erst zur Laufzeit bestimmt", "Die Variable ist privat"],
        correct: 1, why: "Zugriff vor der Initialisierung wirft eine UninitializedPropertyAccessException." },
    },
  ],

  c: [
    {
      blank: { template: "Eine Schleife mit Zähler schreibst du mit ___, eine Zeichenkette endet mit dem Zeichen ___.", blanks: ["for", ["\\0", "0"]] },
      code: { question: "Gib die Zahlen 0 bis 4 mit einer for-Schleife aus.", concepts: ["for", "printf", "int"] },
      mc: { question: "Was liefert `sizeof(arr)` für einen Array-Parameter einer Funktion?",
        options: ["Die Größe des ganzen Arrays", "Die Größe eines Zeigers", "Die Anzahl der Elemente", "Immer 1"],
        correct: 1, why: "Beim Übergeben zerfällt das Array zu einem Zeiger — die Länge muss man mitgeben." },
    },
    {
      blank: { template: "Speicher forderst du mit ___ an und gibst ihn mit ___ wieder frei.", blanks: [["malloc", "malloc()"], ["free", "free()"]] },
      code: { question: "Fordere Speicher für 10 int-Werte an und gib ihn danach wieder frei.", concepts: ["malloc", "int", "free"] },
      mc: { question: "Was ist ein hängender Zeiger (dangling pointer)?",
        options: ["Ein Zeiger auf NULL", "Ein Zeiger auf bereits freigegebenen Speicher", "Ein nicht initialisierter Zähler", "Ein Zeiger auf den Stack"],
        correct: 1, why: "Nach free zeigt er auf fremdes Gebiet — Zugriffe sind undefiniertes Verhalten." },
    },
  ],

  cpp: [
    {
      blank: { template: "Ein dynamisches Array aus der Standardbibliothek heißt ___, hinten angehängt wird mit ___.", blanks: [["vector", "std::vector"], ["push_back", "push_back()"]] },
      code: { question: "Lege einen `std::vector<int>` namens `zahlen` an und füge die 5 hinzu.", concepts: [["vector", "std::vector"], "zahlen", "push_back"] },
      mc: { question: "Wofür steht RAII?",
        options: ["Ein Entwurfsmuster für Threads", "Ressourcen werden an die Lebensdauer eines Objekts gebunden", "Ein Compiler-Schalter", "Eine Namenskonvention"],
        correct: 1, why: "Der Destruktor gibt frei, was der Konstruktor geholt hat — deshalb braucht man selten delete." },
    },
    {
      blank: { template: "Einen alleinigen Besitzer eines Zeigers modellierst du mit ___, geteilten Besitz mit ___.", blanks: [["unique_ptr", "std::unique_ptr"], ["shared_ptr", "std::shared_ptr"]] },
      code: { question: "Gib den Text `Hallo` mit std::cout auf der Konsole aus.", concepts: ["#include", "int main", ["cout", "std::cout"], "Hallo"] },
      mc: { question: "Was macht `std::move`?",
        options: ["Es verschiebt Speicher physisch", "Es markiert einen Wert als verschiebbar", "Es kopiert schneller", "Es löscht das Objekt"],
        correct: 1, why: "move castet zu einer Rvalue-Referenz — verschoben wird erst im Move-Konstruktor." },
    },
  ],

  go: [
    {
      blank: { template: "Eine kurze Deklaration schreibst du mit ___, ein Element hängst du an einen Slice mit ___.", blanks: [":=", ["append", "append()"]] },
      code: { question: "Lege einen Slice `zahlen` an und hänge die 3 an.", concepts: ["zahlen", ":=", "append"] },
      mc: { question: "Was ist der Nullwert eines Slice?",
        options: ["Ein leerer Slice", "nil", "Ein Slice mit einem Element", "Ein Fehler"],
        correct: 1, why: "Ein nil-Slice verhält sich bei len und append wie ein leerer — das ist Absicht." },
    },
    {
      blank: { template: "Einen Fehler prüfst du mit ___, aufgeschoben ausgeführt wird mit ___.", blanks: [["err", "if err != nil"], "defer"] },
      code: { question: "Schreibe eine Funktion `teile`, die zwei ints und einen error zurückgibt.", concepts: ["func", "teile", "int", "error", "return"] },
      mc: { question: "Wann läuft eine mit `defer` registrierte Funktion?",
        options: ["Sofort", "Beim Verlassen der umgebenden Funktion", "Am Programmende", "Nur bei einem panic"],
        correct: 1, why: "Auch bei einem panic — deshalb eignet sich defer zum Aufräumen." },
    },
  ],

  rust: [
    {
      blank: { template: "Ein veränderbares Vec legst du mit ___ an, angehängt wird mit ___.", blanks: [["let mut", "mut"], ["push", "push()"]] },
      code: { question: "Lege ein veränderbares `Vec<i32>` namens `zahlen` an und füge die 7 hinzu.", concepts: ["let", "mut", "zahlen", "Vec", "push"] },
      mc: { question: "Wie viele veränderbare Referenzen auf denselben Wert darf es gleichzeitig geben?",
        options: ["Beliebig viele", "Genau eine", "Zwei", "Keine"],
        correct: 1, why: "Entweder eine veränderbare oder beliebig viele unveränderbare — nie beides zugleich." },
    },
    {
      blank: { template: "Ein Ergebnis mit Fehlerfall hat den Typ ___, ein möglicherweise fehlender Wert den Typ ___.", blanks: ["Result", "Option"] },
      code: { question: "Schreibe eine Funktion `laenge`, die einen &str nimmt und die Länge als usize zurückgibt.", concepts: ["fn", "laenge", "usize", "len"] },
      mc: { question: "Was macht der `?`-Operator?",
        options: ["Er ignoriert Fehler", "Er gibt den Fehler an die aufrufende Funktion weiter", "Er bricht das Programm ab", "Er wandelt in Option um"],
        correct: 1, why: "Bei Ok läuft es weiter, bei Err wird sofort zurückgegeben — das spart verschachtelte match-Blöcke." },
    },
  ],

  php: [
    {
      blank: { template: "Ein assoziatives Array liest du über den ___ aus, ausgegeben wird mit ___.", blanks: [["Schlüssel", "Key"], ["echo", "print"]] },
      code: { question: "Lege ein Array `person` mit dem Schlüssel `name` an und gib den Wert aus.", concepts: ["<?php", "person", "name", ["echo", "print"]] },
      mc: { question: "Wozu dient `htmlspecialchars`?",
        options: ["Es kürzt Texte", "Es maskiert Sonderzeichen und verhindert XSS", "Es prüft die Rechtschreibung", "Es kodiert URLs"],
        correct: 1, why: "Aus `<script>` wird harmloser Text statt eines ausgeführten Skripts." },
    },
    {
      blank: { template: "Eine Schleife über ein Array schreibst du mit ___, eine Funktion definierst du mit ___.", blanks: ["foreach", "function"] },
      code: { question: "Schreibe eine Funktion `gruss`, die einen Namen entgegennimmt und zurückgibt.", concepts: ["function", "gruss", "return"] },
      mc: { question: "Warum sind Prepared Statements sicherer als zusammengesetzte SQL-Strings?",
        options: ["Sie sind kürzer", "Befehl und Daten bleiben getrennt", "Sie sind schneller", "Sie prüfen die Rechtschreibung"],
        correct: 1, why: "Eingaben können dadurch nie zu ausführbarem SQL werden." },
    },
  ],

  sql: [
    {
      blank: { template: "Zeilen filterst du mit ___, sortiert wird mit ___.", blanks: ["WHERE", ["ORDER BY", "ORDER"]] },
      code: { question: "Hole Name und Preis aus `artikel`, nur wenn der Preis über 10 liegt.", concepts: ["SELECT", "name", "preis", "FROM", "artikel", "WHERE", "10"] },
      mc: { question: "Wo steht die Bedingung für Gruppen — im WHERE oder im HAVING?",
        options: ["Im WHERE", "Im HAVING", "In beiden gleichzeitig", "Im SELECT"],
        correct: 1, why: "WHERE filtert einzelne Zeilen vor der Gruppierung, HAVING die fertigen Gruppen." },
    },
    {
      blank: { template: "Die Anzahl der Zeilen liefert ___, die Summe einer Spalte ___.", blanks: [["COUNT", "COUNT(*)"], ["SUM", "SUM()"]] },
      code: { question: "Zähle, wie viele Zeilen die Tabelle `kunden` hat.", concepts: ["SELECT", "COUNT", "FROM", "kunden"] },
      mc: { question: "Was zählt `COUNT(spalte)` im Unterschied zu `COUNT(*)`?",
        options: ["Beides ist gleich", "Nur Zeilen, in denen die Spalte nicht NULL ist", "Nur eindeutige Werte", "Nur die erste Zeile"],
        correct: 1, why: "NULL-Werte werden übersprungen — für alle Zeilen nimmt man COUNT(*)." },
    },
  ],
};

/** Alle Übungssätze einer Sprache — der handgeschriebene zuerst. */
function practiceSets(courseId) {
  const base = COURSE_PRACTICE[courseId];
  const extra = PRACTICE_BANK[courseId] || [];
  const all = [...(base ? [base] : []), ...extra];
  return all.length ? all : [DEFAULT_PRACTICE_SET];
}

/** Kleiner, stabiler Hash — damit dieselbe Lektion immer dieselben Aufgaben hat. */
function lessonHash(id) {
  let h = 0;
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) >>> 0;
  return h;
}

// Fallback-Lektion, falls keine handgemachten Inhalte vorliegen
function buildFallbackLesson(course, meta) {
  const title = meta.lesson.title;
  const hash = lessonHash(meta.lesson.id);
  const sets = practiceSets(course.id);
  const practice = sets[hash % sets.length];

  const tasks = [
    {
      id: "g1", type: "fill_blank",
      question: "Fülle die Lücken aus:",
      template: practice.blank.template,
      blanks: practice.blank.blanks,
      aiCheck: false,
    },
    {
      id: "g2", type: "code_write",
      question: practice.code.question,
      starterCode: "",
      expectedConcepts: practice.code.concepts,
      aiCheck: false,
    },
    {
      id: "g3", type: "multiple_choice",
      question: practice.mc.question,
      options: practice.mc.options,
      correctAnswer: practice.mc.correct,
      explanation: practice.mc.why,
      aiCheck: false,
    },
  ];

  // Nur in etwa jeder vierten Lektion kommt eine Erklär-Aufgabe dazu —
  // sie hat ihren Platz, soll aber nicht das Bild bestimmen.
  if (hash % 4 === 0) {
    // Die Begriffe aus dem Titel sind der Themenanker: eine Antwort, die
    // keinen davon aufgreift, handelt von etwas anderem.
    const titleTerms = title
      .split(/[^A-Za-zÄÖÜäöüß]+/)
      .filter((w) => w.length > 3 && !STOPWORDS_DE.has(w.toLowerCase()))
      .slice(0, 3);
    tasks.push({
      id: "g4", type: "explain",
      question: `Erkläre kurz in eigenen Worten, wozu „${title}“ gut ist.`,
      expectedConcepts: titleTerms,
      aiCheck: false,
    });
  }

  return {
    estimatedMinutes: 10,
    theory: `# ${title}

Willkommen zu dieser Lektion im Kurs **${course.name}**.

In diesem Abschnitt vertiefst du das Thema **„${title}“**. Lies die Konzepte aufmerksam, baue die Beispiele selbst nach und übe mit den Aufgaben rechts.

> 💡 **Tipp:** Aktives Ausprobieren bringt dich beim Programmieren am schnellsten voran. Schreib Code mit, statt ihn nur zu lesen.

## Lernziele

- Die Kernideen hinter *${title}* verstehen
- Die Syntax sicher schreiben können
- Eigene kleine Beispiele bauen`,
    tasks,
  };
}

function getFullLesson(lessonId) {
  const meta = findLessonMeta(lessonId);
  if (!meta) return null;
  const content = LESSON_CONTENT[lessonId] || buildFallbackLesson(meta.course, meta);
  return { ...meta.lesson, courseId: meta.course.id, ...content, _course: meta.course, _module: meta.module };
}

/* ------------------------ KI-Bewertungs-System ------------------------- */
// Ohne eigenen Anthropic-API-Key kann der Browser die API nicht direkt
// erreichen (CORS + Auth). Mit Key wird "anthropic-dangerous-direct-browser-access"
// gesetzt, was Anthropic offiziell für genau diesen Client-seitigen Anwendungsfall
// unterstützt. Genutzt wird das nur vom Assistenten im Code-Editor.
// Unterstützte KI-Anbieter. Gemini hat ein kostenloses Kontingent, Claude ist
// kostenpflichtig, liefert aber die besseren Bewertungen.
const AI_PROVIDERS = {
  gemini: {
    label: "Google Gemini",
    badge: "Kostenlos verfügbar",
    keyPlaceholder: "AIza…",
    keyUrl: "https://aistudio.google.com/app/apikey",
    keyUrlLabel: "aistudio.google.com",
    note: "Kostenloses Kontingent (Gemini Flash): ca. 15 Anfragen pro Minute je Key. Mehrere Keys eintragen — sie werden automatisch abwechselnd genutzt.",
    multiKey: true,
  },
  ollama: {
    label: "Eigener Server (Ollama)",
    badge: "Selbst gehostet",
    keyPlaceholder: "http://localhost:11434",
    keyUrl: "https://ollama.com/download",
    keyUrlLabel: "ollama.com",
    note: "Läuft komplett auf deiner eigenen Hardware — keine Kosten, keine Limits, keine Daten an Dritte. Statt eines Keys trägst du die Server-Adresse ein.",
    multiKey: false,
    isLocal: true,
  },
  anthropic: {
    label: "Anthropic Claude",
    badge: "Kostenpflichtig",
    keyPlaceholder: "sk-ant-…",
    keyUrl: "https://console.anthropic.com/settings/keys",
    keyUrlLabel: "console.anthropic.com",
    note: "Beste Qualität, rechnet aber pro Nutzung ab (Bruchteile eines Cents pro Bewertung) — kein Gratis-Tarif.",
    multiKey: true,
  },
};

const OLLAMA_DEFAULT_URL = "http://localhost:11434";
const OLLAMA_DEFAULT_MODEL = "qwen2.5-coder:3b";

/* ------------------------- Key-Pool mit Rotation -------------------------
   Mehrere API-Keys werden reihum genutzt. Läuft ein Key ins Rate-Limit (429)
   oder ist sein Kontingent erschöpft (403), wird er für eine Weile pausiert
   und der nächste Key übernimmt. So summieren sich die Gratis-Kontingente
   mehrerer Konten zu einem gemeinsamen Durchsatz.
   ------------------------------------------------------------------------- */
const keyCooldowns = new Map();   // key -> Zeitpunkt, ab dem er wieder nutzbar ist
let keyCursor = 0;

function availableKeys(keys) {
  const now = Date.now();
  const free = keys.filter((k) => (keyCooldowns.get(k) || 0) <= now);
  return free.length ? free : keys; // alle pausiert? Dann trotzdem versuchen.
}

function nextKey(keys) {
  const pool = availableKeys(keys);
  const key = pool[keyCursor % pool.length];
  keyCursor = (keyCursor + 1) % Math.max(pool.length, 1);
  return key;
}

function coolDownKey(key, seconds = 60) {
  keyCooldowns.set(key, Date.now() + seconds * 1000);
}

function keyPoolStatus(keys) {
  const now = Date.now();
  return (keys || []).map((k) => ({
    masked: k.length > 10 ? k.slice(0, 6) + "…" + k.slice(-4) : k,
    cooling: (keyCooldowns.get(k) || 0) > now,
    secondsLeft: Math.max(0, Math.ceil(((keyCooldowns.get(k) || 0) - now) / 1000)),
  }));
}

async function callProviderOnce(provider, key, systemPrompt, userPrompt, maxTokens, ollamaModel) {
  if (provider === "ollama") {
    const base = (key || OLLAMA_DEFAULT_URL).replace(/\/+$/, "");
    const res = await fetch(`${base}/api/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        model: ollamaModel || OLLAMA_DEFAULT_MODEL,
        stream: false,
        options: { temperature: 0.3, num_predict: maxTokens },
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userPrompt },
        ],
      }),
    });
    if (!res.ok) throw Object.assign(new Error("Ollama " + res.status), { status: res.status });
    const data = await res.json();
    return data.message.content;
  }

  if (provider === "gemini") {
    const res = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${encodeURIComponent(key)}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: systemPrompt }] },
          contents: [{ role: "user", parts: [{ text: userPrompt }] }],
          generationConfig: { maxOutputTokens: maxTokens, temperature: 0.3 },
        }),
      }
    );
    if (!res.ok) throw Object.assign(new Error("Gemini " + res.status), { status: res.status });
    const data = await res.json();
    return data.candidates[0].content.parts[0].text;
  }

  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "anthropic-version": "2023-06-01",
      "x-api-key": key,
      "anthropic-dangerous-direct-browser-access": "true",
    },
    body: JSON.stringify({
      model: "claude-sonnet-4-6",
      max_tokens: maxTokens,
      system: systemPrompt,
      messages: [{ role: "user", content: userPrompt }],
    }),
  });
  if (!res.ok) throw Object.assign(new Error("Anthropic " + res.status), { status: res.status });
  const data = await res.json();
  return data.content[0].text;
}

// Ruft den gewählten Anbieter auf; probiert bei Limits automatisch weitere Keys.
async function callAI(provider, keys, systemPrompt, userPrompt, maxTokens = 1000, ollamaModel) {
  const pool = (Array.isArray(keys) ? keys : [keys]).filter(Boolean);
  if (provider === "ollama") {
    return callProviderOnce("ollama", pool[0] || OLLAMA_DEFAULT_URL, systemPrompt, userPrompt, maxTokens, ollamaModel);
  }
  if (!pool.length) throw new Error("Kein API-Key hinterlegt");

  let lastError;
  const attempts = Math.min(pool.length, 5);
  for (let i = 0; i < attempts; i++) {
    const key = nextKey(pool);
    try {
      return await callProviderOnce(provider, key, systemPrompt, userPrompt, maxTokens, ollamaModel);
    } catch (e) {
      lastError = e;
      // 429 = Rate-Limit, 403/402 = Kontingent erschöpft -> Key pausieren, nächsten nehmen
      if (e.status === 429) coolDownKey(key, 65);
      else if (e.status === 403 || e.status === 402) coolDownKey(key, 600);
      else break; // andere Fehler (z.B. ungültige Anfrage) betreffen alle Keys gleich
    }
  }
  throw lastError;
}

function parseAIJson(text) {
  const cleaned = text.replace(/```json|```/g, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  return JSON.parse(start >= 0 && end > start ? cleaned.slice(start, end + 1) : cleaned);
}

/* ---------------------- KI-Assistent (nur im Code-Editor) ----------------
   Lektionen werden ausschließlich lokal bewertet — sofort und kostenlos.
   Die KI sitzt stattdessen als Gesprächspartner im Editor, wo eine Antwortzeit
   von ein paar Sekunden völlig in Ordnung ist.
   ------------------------------------------------------------------------- */
const ASSISTANT_SYSTEM_PROMPT = `Du bist ein hilfsbereiter Programmier-Assistent in einem Code-Editor.
Der Nutzer lernt gerade programmieren.

DEINE ARBEITSWEISE:
- Antworte immer auf Deutsch
- Fasse dich kurz und konkret (höchstens 6 Sätze, außer es wird ausdrücklich mehr verlangt)
- Zeige Code in Markdown-Codeblöcken mit Sprachangabe
- Erkläre das Warum, nicht nur das Wie
- Wenn Code fehlerhaft ist: nenne die Ursache und zeige die korrigierte Stelle
- Erfinde nichts — sag es, wenn du etwas nicht sicher weißt
- Fang nie mit "Ich" an`;

function buildAssistantContext({ html, css, js }) {
  const part = (label, code) => {
    const trimmed = String(code || "").trim();
    return trimmed ? `\n--- ${label} ---\n${trimmed.slice(0, 6000)}` : "";
  };
  const ctx = part("HTML", html) + part("CSS", css) + part("JavaScript", js);
  return ctx ? `Aktueller Code im Editor:${ctx}` : "Der Editor ist noch leer.";
}

/**
 * Schickt eine Nachricht an den gewählten Anbieter und liefert reinen Text
 * zurück. Fällt der Dienst aus, wird eine verständliche Meldung erzeugt
 * statt einer technischen Fehlermeldung.
 */
async function askAssistant(messages, code, aiCfg = {}) {
  const { keys = [], provider = "gemini", ollamaModel, useServer } = aiCfg;

  const history = messages
    .slice(-8)                                   // Kontext knapp halten — spart Zeit und Kontingent
    .map((m) => `${m.role === "user" ? "Nutzer" : "Assistent"}: ${m.content}`)
    .join("\n\n");
  const userPrompt = `${buildAssistantContext(code)}\n\n--- Verlauf ---\n${history}`;

  if (useServer) {
    const res = await api.post("/api/ai/assist", { messages: messages.slice(-8), code });
    return res.reply;
  }

  const hasAccess = provider === "ollama" || keys.length > 0;
  if (!hasAccess) throw new Error("Kein KI-Zugang eingerichtet.");
  return callAI(provider, keys, ASSISTANT_SYSTEM_PROMPT, userPrompt, 900, ollamaModel);
}
/* =========================================================================
   LD-Analyzer — die lokale Analyse-Engine
   =========================================================================

   Bewertet Antworten vollständig im Browser: kein Netzwerk, keine Kosten,
   Ergebnis in wenigen Millisekunden. Sie ist der einzige Prüfweg für
   Lektionen — die KI sitzt stattdessen als Assistent im Code-Editor, wo
   Wartezeit vertretbar ist.

   Der Ablauf in vier Schritten:

     1. Tokenisieren   — Kommentare und Zeichenketten werden entfernt, damit
                         ein Kommentar wie "// nutze const" nicht als Lösung
                         durchgeht. Die Literale bleiben separat erhalten.
     2. Strukturieren  — Deklarationen, Funktionen, Aufrufe, Klammer-Balance
                         und Einrückung werden erfasst.
     3. Abgleichen     — Erwartete Konzepte werden gegen diese Struktur
                         geprüft, nicht gegen den Rohtext. Dadurch lassen sich
                         Beinahe-Treffer erkennen (falsches Schlüsselwort,
                         Tippfehler im Namen, falsche Groß-/Kleinschreibung).
     4. Bewerten       — Punktzahl plus konkret formuliertes Feedback.

   ========================================================================= */

function normalizeAlnum(s) { return (s || "").toLowerCase().replace(/[^a-z0-9äöüß]+/g, ""); }

/* -------------------------- Sprachprofile ------------------------------- */
const GENERIC_PROFILE = {
  label: "Code",
  lineComment: ["//"],
  blockComment: [["/*", "*/"]],
  stringDelims: ['"', "'"],
  blockStyle: "braces",
  declare: [],
  funcDef: [],
  print: [],
  pitfalls: [],
};

const LANG_PROFILES = {
  javascript: {
    label: "JavaScript",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'", "`"],
    blockStyle: "braces",
    declare: [/\b(const|let|var)\s+([A-Za-z_$][\w$]*)/g],
    funcDef: [/\bfunction\s+([A-Za-z_$][\w$]*)/g, /\b([A-Za-z_$][\w$]*)\s*=\s*(?:async\s*)?\([^)]*\)\s*=>/g],
    print: ["console.log"],
    pitfalls: [
      { re: /[^=!<>]==[^=]/, severity: "warning", title: "Lockerer Vergleich",
        hint: "`==` vergleicht mit Typumwandlung. Nutze `===` für einen strikten Vergleich." },
      { re: /\bvar\s+/, severity: "info", title: "`var` ist veraltet",
        hint: "Nutze `let` (veränderlich) oder `const` (fest)." },
    ],
  },
  typescript: {
    label: "TypeScript",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'", "`"],
    blockStyle: "braces",
    declare: [/\b(const|let|var)\s+([A-Za-z_$][\w$]*)/g],
    funcDef: [/\bfunction\s+([A-Za-z_$][\w$]*)/g, /\b([A-Za-z_$][\w$]*)\s*=\s*(?:async\s*)?\([^)]*\)\s*(?::[^=]+)?=>/g],
    print: ["console.log"],
    pitfalls: [
      { re: /:\s*any\b/, severity: "info", title: "`any` umgeht die Typprüfung",
        hint: "Ein genauerer Typ bringt dir die Sicherheit zurück, für die du TypeScript nutzt." },
    ],
  },
  react: {
    label: "React",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'", "`"],
    blockStyle: "braces",
    declare: [/\b(const|let|var)\s+([A-Za-z_$][\w$]*)/g],
    funcDef: [/\bfunction\s+([A-Z][\w$]*)/g, /\b([A-Z][\w$]*)\s*=\s*\([^)]*\)\s*=>/g],
    print: ["console.log"],
    pitfalls: [
      { re: /\bclass=/, severity: "warning", title: "In JSX heißt es `className`",
        hint: "`class` ist in JavaScript reserviert — React nutzt deshalb `className`." },
    ],
  },
  vue: {
    label: "Vue",
    lineComment: ["//"], blockComment: [["/*", "*/"], ["<!--", "-->"]],
    stringDelims: ['"', "'", "`"],
    blockStyle: "braces",
    declare: [/\b(const|let|var)\s+([A-Za-z_$][\w$]*)/g],
    funcDef: [/\bfunction\s+([A-Za-z_$][\w$]*)/g],
    print: ["console.log"],
    pitfalls: [],
  },
  python: {
    label: "Python",
    lineComment: ["#"], blockComment: [['"""', '"""'], ["'''", "'''"]],
    stringDelims: ['"', "'"],
    blockStyle: "indent",
    declare: [/^\s*([A-Za-z_]\w*)\s*=(?!=)/gm],
    funcDef: [/\bdef\s+([A-Za-z_]\w*)/g],
    print: ["print"],
    pitfalls: [
      { re: /\bdef\s+\w+\s*\([^)]*\)\s*[^:\s]/, severity: "error", title: "Doppelpunkt fehlt",
        hint: "Nach der Parameterliste einer Funktion muss ein `:` stehen." },
      { re: /\b(if|for|while|else)\b[^\n:]*$/m, severity: "warning", title: "Möglicherweise fehlt ein `:`",
        hint: "Kontrollstrukturen in Python enden mit einem Doppelpunkt." },
    ],
  },
  java: {
    label: "Java",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'"],
    blockStyle: "braces", needsSemicolon: true,
    declare: [/\b(int|double|float|long|boolean|char|String|var)\s+([A-Za-z_]\w*)\s*[=;]/g],
    funcDef: [/\b(?:public|private|protected)?\s*(?:static\s+)?[\w<>\[\]]+\s+([A-Za-z_]\w*)\s*\([^)]*\)\s*\{/g],
    print: ["System.out.println", "System.out.print"],
    pitfalls: [
      { re: /"[^"]*"\s*==\s*"/, severity: "warning", title: "Zeichenketten mit `==` verglichen",
        hint: "In Java vergleicht `==` die Referenz. Nutze `.equals()` für den Inhalt." },
    ],
  },
  kotlin: {
    label: "Kotlin",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'"],
    blockStyle: "braces",
    declare: [/\b(val|var)\s+([A-Za-z_]\w*)/g],
    funcDef: [/\bfun\s+([A-Za-z_]\w*)/g],
    print: ["println", "print"],
    pitfalls: [
      { re: /!!/, severity: "warning", title: "`!!` umgeht die Null-Sicherheit",
        hint: "Nutze lieber `?.` oder `?:`, sonst kann es zur Laufzeit knallen." },
    ],
  },
  cpp: {
    label: "C++",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'"],
    blockStyle: "braces", needsSemicolon: true,
    declare: [/\b(int|double|float|char|bool|auto|string|std::string)\s+([A-Za-z_]\w*)/g],
    funcDef: [/\b[\w:<>]+\s+([A-Za-z_]\w*)\s*\([^)]*\)\s*\{/g],
    print: ["std::cout", "cout", "printf"],
    pitfalls: [
      { re: /\bgets\s*\(/, severity: "error", title: "`gets` ist unsicher",
        hint: "Nutze `std::getline` — `gets` kann den Puffer überschreiben." },
    ],
  },
  c: {
    label: "C",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'"],
    blockStyle: "braces", needsSemicolon: true,
    declare: [/\b(int|double|float|char|long|short|unsigned)\s+\*?([A-Za-z_]\w*)/g],
    funcDef: [/\b[\w*]+\s+([A-Za-z_]\w*)\s*\([^)]*\)\s*\{/g],
    print: ["printf", "puts"],
    pitfalls: [
      { re: /\bmalloc\s*\(/, severity: "info", title: "Speicher wieder freigeben",
        hint: "Zu jedem `malloc` gehört ein `free`." },
    ],
  },
  go: {
    label: "Go",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "`", "'"],
    blockStyle: "braces",
    declare: [/\b(var)\s+([A-Za-z_]\w*)/g, /\b([A-Za-z_]\w*)\s*:=/g],
    funcDef: [/\bfunc\s+([A-Za-z_]\w*)/g],
    print: ["fmt.Println", "fmt.Printf", "println"],
    pitfalls: [
      { re: /\b_\s*,\s*_\s*:?=/, severity: "info", title: "Rückgabewerte verworfen",
        hint: "Prüfe zumindest den Fehlerwert, statt ihn zu ignorieren." },
    ],
  },
  rust: {
    label: "Rust",
    lineComment: ["//"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'"],
    blockStyle: "braces",
    declare: [/\blet\s+(?:mut\s+)?([A-Za-z_]\w*)/g],
    funcDef: [/\bfn\s+([A-Za-z_]\w*)/g],
    print: ["println!", "print!"],
    pitfalls: [
      { re: /\.unwrap\(\)/, severity: "info", title: "`unwrap()` bricht bei Fehlern ab",
        hint: "In echtem Code besser `match` oder `?` verwenden." },
    ],
  },
  php: {
    label: "PHP",
    lineComment: ["//", "#"], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'"],
    blockStyle: "braces", needsSemicolon: true,
    declare: [/\$([A-Za-z_]\w*)\s*=(?!=)/g],
    funcDef: [/\bfunction\s+([A-Za-z_]\w*)/g],
    print: ["echo", "print_r", "var_dump"],
    pitfalls: [
      { re: /\$_(GET|POST)\[[^\]]+\]\s*(?!.*(?:htmlspecialchars|filter_|intval))/, severity: "warning",
        title: "Nutzereingabe ungeprüft verwendet",
        hint: "Eingaben immer prüfen oder maskieren — sonst drohen XSS und SQL-Injection." },
    ],
  },
  sql: {
    label: "SQL",
    lineComment: ["--"], blockComment: [["/*", "*/"]],
    stringDelims: ["'", '"'],
    blockStyle: "none",
    declare: [], funcDef: [],
    print: [],
    pitfalls: [
      { re: /\bDELETE\s+FROM\s+\w+\s*;?\s*$/i, severity: "warning", title: "DELETE ohne WHERE",
        hint: "Ohne `WHERE` löscht das alle Zeilen der Tabelle." },
      { re: /\bSELECT\s+\*/i, severity: "info", title: "`SELECT *` überträgt alle Spalten",
        hint: "Benenne die Spalten, die du wirklich brauchst." },
    ],
  },
  html: {
    label: "HTML",
    lineComment: [], blockComment: [["<!--", "-->"]],
    stringDelims: ['"', "'"],
    blockStyle: "tags",
    declare: [], funcDef: [], print: [],
    pitfalls: [
      { re: /<img(?![^>]*\balt=)/i, severity: "warning", title: "Bild ohne `alt`",
        hint: "Ein Alternativtext ist für Screenreader und bei fehlendem Bild wichtig." },
      { re: /<a(?![^>]*\bhref=)/i, severity: "warning", title: "Link ohne `href`",
        hint: "Ohne `href` ist ein `<a>` kein anklickbarer Link." },
    ],
  },
  css: {
    label: "CSS",
    lineComment: [], blockComment: [["/*", "*/"]],
    stringDelims: ['"', "'"],
    blockStyle: "braces",
    declare: [/([-\w]+)\s*:\s*[^;}]+/g],
    funcDef: [], print: [],
    pitfalls: [
      { re: /!important/, severity: "info", title: "`!important` vermeiden",
        hint: "Es überschreibt alles und macht spätere Anpassungen schwer." },
      { re: /:\s*[^;}\n]+\n\s*[-\w]+\s*:/, severity: "warning", title: "Semikolon fehlt vermutlich",
        hint: "Jede CSS-Deklaration endet mit `;`." },
    ],
  },
};

function profileFor(langId) {
  return LANG_PROFILES[langId] || GENERIC_PROFILE;
}

/* --------------------------- Tokenisierung ------------------------------ */
/**
 * Entfernt Kommentare und Zeichenketten, merkt sich deren Inhalt aber.
 * Länge und Zeilenstruktur bleiben erhalten, damit Positionsangaben stimmen.
 */
function tokenize(code, profile) {
  const src = String(code || "");
  let out = "";
  const strings = [];
  const comments = [];
  let i = 0;

  const startsWith = (s) => src.startsWith(s, i);

  while (i < src.length) {
    // Blockkommentare
    let matchedBlock = false;
    for (const [open, close] of profile.blockComment || []) {
      if (startsWith(open)) {
        const end = src.indexOf(close, i + open.length);
        const stop = end === -1 ? src.length : end + close.length;
        const body = src.slice(i, stop);
        comments.push(body);
        out += body.replace(/[^\n]/g, " ");   // Zeilenumbrüche behalten
        i = stop;
        matchedBlock = true;
        break;
      }
    }
    if (matchedBlock) continue;

    // Zeilenkommentare
    let matchedLine = false;
    for (const marker of profile.lineComment || []) {
      if (startsWith(marker)) {
        const end = src.indexOf("\n", i);
        const stop = end === -1 ? src.length : end;
        comments.push(src.slice(i, stop));
        out += " ".repeat(stop - i);
        i = stop;
        matchedLine = true;
        break;
      }
    }
    if (matchedLine) continue;

    // Zeichenketten
    const delim = (profile.stringDelims || []).find((d) => startsWith(d));
    if (delim) {
      let j = i + delim.length;
      let body = "";
      while (j < src.length) {
        if (src[j] === "\\") { body += src[j + 1] || ""; j += 2; continue; }
        if (src.startsWith(delim, j)) break;
        body += src[j];
        j++;
      }
      strings.push(body);
      const stop = Math.min(src.length, j + delim.length);
      // Leere Hülle behalten, damit "hier steht ein String" erkennbar bleibt
      out += delim + delim + " ".repeat(Math.max(0, stop - i - 2));
      i = stop;
      continue;
    }

    out += src[i];
    i++;
  }

  return { stripped: out, strings, comments };
}

/* ------------------------- Strukturanalyse ------------------------------ */
function collectMatches(regexes, text, nameGroup) {
  const found = [];
  for (const re of regexes || []) {
    const rx = new RegExp(re.source, re.flags.includes("g") ? re.flags : re.flags + "g");
    let m;
    while ((m = rx.exec(text))) {
      // Der Name steht je nach Muster in Gruppe 1 oder 2
      const name = m[nameGroup] || m[2] || m[1];
      if (name) found.push({ name, keyword: m[2] ? m[1] : null, index: m.index });
      if (m.index === rx.lastIndex) rx.lastIndex++;   // Endlosschleife verhindern
    }
  }
  return found;
}

function bracketBalance(text) {
  const pairs = { "{": "}", "(": ")", "[": "]" };
  const closing = { "}": "{", ")": "(", "]": "[" };
  const stack = [];
  const problems = [];
  for (const ch of text) {
    if (pairs[ch]) stack.push(ch);
    else if (closing[ch]) {
      if (!stack.length) { problems.push({ kind: "extraClose", ch }); continue; }
      const top = stack.pop();
      if (top !== closing[ch]) problems.push({ kind: "mismatch", expected: pairs[top], got: ch });
    }
  }
  stack.forEach((ch) => problems.push({ kind: "unclosed", ch, expected: pairs[ch] }));
  return { ok: problems.length === 0, problems };
}

function tagBalance(html) {
  const voids = new Set(["br", "hr", "img", "input", "meta", "link", "source", "area", "base", "col", "embed", "track", "wbr"]);
  const stack = [];
  const problems = [];
  const rx = /<\/?([a-zA-Z][\w-]*)[^>]*?(\/?)>/g;
  let m;
  while ((m = rx.exec(html))) {
    const [full, name, selfClose] = m;
    const tag = name.toLowerCase();
    if (voids.has(tag) || selfClose === "/") continue;
    if (full.startsWith("</")) {
      const idx = stack.lastIndexOf(tag);
      if (idx === -1) problems.push({ kind: "extraClose", tag });
      else stack.splice(idx, 1);
    } else stack.push(tag);
  }
  stack.forEach((tag) => problems.push({ kind: "unclosed", tag }));
  return { ok: problems.length === 0, problems };
}

/**
 * Führt die Analyse durch und liefert eine strukturierte Sicht auf den Code.
 */
function analyzeCode(raw, langId) {
  const profile = profileFor(langId);
  const { stripped, strings, comments } = tokenize(raw, profile);
  const codeOnly = stripped.replace(/\s/g, "");

  const declarations = collectMatches(profile.declare, stripped, 2);
  const functions = collectMatches(profile.funcDef, stripped, 1);
  const identifiers = [...new Set((stripped.match(/[A-Za-z_$][\w$]*/g) || []))];
  const calls = [...new Set((stripped.match(/([A-Za-z_$][\w$.:!]*)\s*\(/g) || []).map((s) => s.replace(/\s*\($/, "")))];

  const structure = profile.blockStyle === "tags" ? tagBalance(stripped)
    : profile.blockStyle === "none" ? { ok: true, problems: [] }
    : bracketBalance(stripped);

  const issues = [];
  for (const p of profile.pitfalls || []) {
    if (p.re.test(stripped)) issues.push({ severity: p.severity, title: p.title, hint: p.hint });
  }

  return {
    raw, stripped, strings, comments, profile,
    isEmpty: codeOnly.length === 0,
    hasOnlyComments: codeOnly.length === 0 && comments.length > 0,
    lines: raw.split("\n").filter((l) => l.trim()).length,
    declarations, functions, identifiers, calls,
    structure, issues,
    hasString: strings.length > 0,
    hasNumber: /\d/.test(stripped),
  };
}

/* ------------------------- Konzept-Abgleich ------------------------------ */
// Sammelbegriffe, die je nach Sprache unterschiedlich aussehen.
const CONCEPT_GROUPS = {
  variable: { any: ["let", "const", "var", "val", "int", "def", "$", ":=", "="], kind: "declaration" },
  "let oder const": { any: ["let", "const"], kind: "keyword" },
  string: { kind: "string" },
  text: { kind: "string" },
  zahl: { kind: "number" },
  number: { kind: "number" },
  funktion: { any: ["function", "def", "=>", "func", "fun", "fn"], kind: "function" },
  function: { any: ["function", "def", "=>", "func", "fun", "fn"], kind: "function" },
  // Kommentare werden beim Tokenisieren entfernt — dafür gibt es eine eigene
  // Prüfung gegen die gesammelten Kommentare, sonst wäre so eine Aufgabe
  // grundsätzlich nicht lösbar.
  kommentar: { kind: "comment" },
  comment: { kind: "comment" },
  schleife: { any: ["for", "while", "foreach", "map", "loop"], kind: "keyword" },
  bedingung: { any: ["if", "switch", "match", "when"], kind: "keyword" },
  ausgabe: { any: ["console.log", "print", "println", "cout", "echo", "printf", "fmt.println"], kind: "call" },
};

const HTML_TAGS = new Set(["html","head","body","title","meta","link","script","style","div","span","p","a","img","ul","ol","li","h1","h2","h3","h4","h5","h6","table","tr","td","th","thead","tbody","form","input","button","label","select","option","textarea","header","footer","main","section","article","nav","aside","video","audio","source","br","hr","strong","em","b","i","u","small","code","pre","blockquote","figure","figcaption","canvas","iframe","details","summary"]);
const HTML_VOID_TAGS = new Set(["br","hr","img","input","meta","link","source","area","base","col","embed","track","wbr"]);

// Schlüsselwörter, die eine Aufgabe ausdrücklich verlangen kann. Fehlt eines,
// ist die Lösung nicht "fast richtig", sondern verfehlt die Aufgabenstellung.
const ESSENTIAL_KEYWORDS = new Set([
  "const","let","var","val","function","def","fn","fun","func","class","return",
  "if","else","for","while","switch","match","import","export","async","await",
  "select","from","where","insert","update","delete","join","group","order",
  "int","double","float","boolean","char","string","void","public","static",
]);

/** Levenshtein-Distanz — erkennt Tippfehler in Bezeichnern. */
function editDistance(a, b) {
  if (a === b) return 0;
  if (!a.length || !b.length) return Math.max(a.length, b.length);
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    const cur = [i];
    for (let j = 1; j <= b.length; j++) {
      cur[j] = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
    }
    prev = cur;
  }
  return prev[b.length];
}

/**
 * Prüft ein erwartetes Konzept gegen die Analyse.
 * Liefert nicht nur ja/nein, sondern auch, *warum* es knapp verfehlt wurde —
 * daraus entsteht das konkrete Feedback.
 */
function checkConcept(concept, analysis) {
  // Mehrere zulässige Schreibweisen — `["cout", "std::cout"]`. Eine genügt.
  if (Array.isArray(concept)) {
    const tried = concept.map((variant) => checkConcept(variant, analysis));
    const hit = tried.find((r) => r.hit);
    if (hit) return { ...hit, concept: concept[0] };
    return { ...(tried[0] || { hit: false }), concept: concept[0] };
  }
  const c = String(concept).trim();
  const lc = c.toLowerCase();
  const code = analysis.stripped;
  const lower = code.toLowerCase();

  // 1. Kommentar-Zeichen der jeweiligen Sprache (#, //, --) werden beim
  //    Tokenisieren entfernt und müssen daher gegen die gesammelten
  //    Kommentare geprüft werden.
  const commentMarkers = [
    ...(analysis.profile.lineComment || []),
    ...(analysis.profile.blockComment || []).map(([open]) => open),
  ];
  if (commentMarkers.includes(c)) {
    return { hit: analysis.comments.length > 0, concept: c, kind: "comment",
      why: analysis.comments.length ? null : `Es fehlt ein Kommentar (beginnt mit \`${c}\`).` };
  }

  // 2. Operatoren und Symbole
  if (/^[=+\-*/<>!%&|.;:()[\]{}]+$/.test(c)) {
    return { hit: code.includes(c), concept: c, kind: "operator" };
  }

  // 3. Sammelbegriffe
  const group = CONCEPT_GROUPS[lc];
  if (group) {
    if (group.kind === "comment") {
      const hasComment = analysis.comments.some((k) => k.replace(/^\W+/, "").trim().length > 0);
      const marker = (analysis.profile.lineComment || [])[0] || "//";
      return { hit: hasComment, concept: c, kind: "comment", essential: true,
        why: hasComment ? null : `Es fehlt ein Kommentar — in ${analysis.profile.label} beginnt er mit \`${marker}\`.` };
    }
    if (group.kind === "string") {
      return { hit: analysis.hasString, concept: c, kind: "string", essential: true,
        why: analysis.hasString ? null : "Es fehlt eine Zeichenkette in Anführungszeichen." };
    }
    if (group.kind === "number") {
      return { hit: analysis.hasNumber, concept: c, kind: "number", essential: true,
        why: analysis.hasNumber ? null : "Es fehlt eine Zahl." };
    }
    const hit = (group.any || []).some((a) => lower.includes(a.toLowerCase()));
    return { hit, concept: c, kind: group.kind,
      why: hit ? null : `Kein passendes Sprachmittel gefunden (erwartet z.B. ${group.any.slice(0, 3).join(", ")}).` };
  }

  // 4. HTML-Tags — nur, wenn es wirklich ein Tag-Name ist. Sonst würde ein
  //    erwarteter Textinhalt wie "Willkommen" fälschlich als <willkommen>
  //    gesucht.
  if (analysis.profile.blockStyle === "tags") {
    const tag = lc.replace(/[<>/]/g, "").trim();
    // Ein Tag ist nur, was auch wirklich wie ein Tag aussieht: `<p>`, `</a>`,
    // `<img`. Bruchstücke wie `<a href=` oder `src=` sind Attribute — die
    // werden weiter unten als Text gesucht, sonst würde daraus die unsinnige
    // Meldung „Das Element <a href=> fehlt“.
    const isTagSyntax = /^<\/?[a-zA-Z][\w:-]*\s*\/?>?$/.test(c);
    if (isTagSyntax || HTML_TAGS.has(tag)) {
      const opened = new RegExp(`<${tag}[\\s>]`, "i").test(code);
      const closed = new RegExp(`</${tag}>`, "i").test(code);
      const isVoid = HTML_VOID_TAGS.has(tag);
      if (opened && (closed || isVoid)) return { hit: true, concept: c, kind: "tag", essential: true };
      if (opened && !closed) return { hit: false, concept: c, kind: "tag", essential: true, why: `<${tag}> wird geöffnet, aber nie geschlossen.` };
      return { hit: false, concept: c, kind: "tag", essential: true, why: `Das Element <${tag}> fehlt.` };
    }
    // Kein Tag -> Attributschreibweise oder erwarteter Textinhalt
    const inText = analysis.raw.toLowerCase().includes(lc);
    const isMarkup = /[<>=]/.test(c);           // `src=`, `<a href=` … gehört zwingend dazu
    return { hit: inText, concept: c, kind: isMarkup ? "markup" : "text", essential: isMarkup,
      why: inText ? null : isMarkup ? `\`${c}\` fehlt noch.` : `Der Text „${c}“ kommt nicht vor.` };
  }

  // 5. Bezeichner und Schlüsselwörter — als ganzes Wort.
  //    Beides gilt als wesentlich: Wer `const` verlangt, meint nicht `let`,
  //    und ein geforderter Variablenname ist keine Nebensache.
  if (/^[\w$äöüß.!:]+$/i.test(c)) {
    const isKeyword = ESSENTIAL_KEYWORDS.has(lc);
    const escaped = c.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    if (new RegExp(`(^|[^\\w$])${escaped}([^\\w$]|$)`, "i").test(code)) {
      // Treffer — aber stimmt die Groß-/Kleinschreibung?
      const exact = new RegExp(`(^|[^\\w$])${escaped}([^\\w$]|$)`).test(code);
      return { hit: true, concept: c, kind: isKeyword ? "keyword" : "identifier", essential: true,
        note: exact ? null : `Achte auf die Groß- und Kleinschreibung: erwartet wird \`${c}\`.` };
    }
    // Steht der erwartete Text in einer Zeichenkette? Beim Zerlegen werden
    // Zeichenketten herausgenommen — ein erwarteter Ausgabetext wie `Hallo`
    // muss deshalb dort gesucht werden.
    if (analysis.strings.some((str) => str.toLowerCase().includes(lc))) {
      return { hit: true, concept: c, kind: "literal", essential: true };
    }

    // Beinahe-Treffer? Dann ist es vermutlich ein Tippfehler.
    const near = analysis.identifiers.find((id) => {
      const d = editDistance(id.toLowerCase(), lc);
      return d > 0 && d <= Math.max(1, Math.floor(lc.length / 4));
    });
    // Wurde stattdessen ein verwandtes Schlüsselwort verwendet?
    let why;
    if (isKeyword) {
      const used = [...ESSENTIAL_KEYWORDS].find((k) => k !== lc &&
        new RegExp(`(^|[^\\w$])${k}([^\\w$]|$)`, "i").test(code));
      why = used
        ? `Du hast \`${used}\` verwendet — die Aufgabe verlangt \`${c}\`.`
        : `Das Schlüsselwort \`${c}\` fehlt.`;
    } else {
      why = near
        ? `Du hast \`${near}\` geschrieben — erwartet wird \`${c}\`.`
        : `\`${c}\` kommt in deiner Lösung nicht vor.`;
    }
    return { hit: false, concept: c, kind: isKeyword ? "keyword" : "identifier", essential: true, why };
  }

  // 6. Mehrwort-Beschreibung: es genügt ein sinntragender Teil
  const parts = lc.split(/\s+oder\s+|[\s,]+/).filter((t) => t.length > 1);
  const hit = parts.length ? parts.some((p) => lower.includes(p)) : lower.includes(lc);
  return { hit, concept: c, kind: "phrase" };
}

/* ---------------------- Bewertung: Code-Aufgaben -------------------------- */
function evaluateCode(task, answer, langId) {
  const analysis = analyzeCode(answer, langId);
  const label = analysis.profile.label;

  if (analysis.isEmpty) {
    return {
      correct: false, score: 0, offline: true,
      feedback: analysis.hasOnlyComments
        ? "Bisher stehen dort nur Kommentare — der eigentliche Code fehlt noch."
        : "Es ist noch kein Code vorhanden.",
      hint: "Schreib deine Lösung als echten Code, nicht als Kommentar.",
      praise: "",
    };
  }

  // Struktur zuerst: Ohne gültige Klammerung ist alles andere hinfällig.
  if (!analysis.structure.ok) {
    const p = analysis.structure.problems[0];
    let detail;
    if (p.tag) detail = p.kind === "unclosed" ? `<${p.tag}> wird nie geschlossen.` : `</${p.tag}> steht ohne passendes öffnendes Element.`;
    else if (p.kind === "unclosed") detail = `Eine öffnende \`${p.ch}\` hat keine passende \`${p.expected}\`.`;
    else if (p.kind === "extraClose") detail = `\`${p.ch}\` schließt etwas, das nie geöffnet wurde.`;
    else detail = `Erwartet wurde \`${p.expected}\`, gefunden \`${p.got}\`.`;
    return {
      correct: false, score: 25, offline: true,
      feedback: `Der Aufbau stimmt noch nicht: ${detail}`,
      hint: "Prüfe, ob jede geöffnete Klammer wieder geschlossen wird.",
      praise: "",
    };
  }

  const concepts = task.expectedConcepts || [];
  const results = concepts.map((c) => checkConcept(c, analysis));
  const hits = results.filter((r) => r.hit);
  const misses = results.filter((r) => !r.hit);
  const notes = results.map((r) => r.note).filter(Boolean);

  const coverage = concepts.length ? hits.length / concepts.length : 0.85;
  const blockingIssues = analysis.issues.filter((i) => i.severity === "error");
  // Ein ausdrücklich verlangtes Schlüsselwort oder ein geforderter Bezeichner
  // ist keine Nebensache — fehlt er, ist die Aufgabe nicht gelöst, auch wenn
  // rechnerisch genug andere Bausteine da wären.
  const missingEssential = misses.filter((m) => m.essential);

  let score = Math.round(coverage * 80 + 20);       // Struktur ist bereits in Ordnung
  if (blockingIssues.length) score = Math.min(score, 55);
  if (missingEssential.length) score = Math.min(score, 50);
  if (notes.length) score = Math.max(0, score - 5);
  score = Math.max(0, Math.min(100, score));

  const correct = coverage >= 0.6 && !blockingIssues.length && !missingEssential.length;

  // Feedback so konkret wie möglich formulieren
  let feedback, hint = "";
  if (blockingIssues.length) {
    feedback = `${blockingIssues[0].title}: ${blockingIssues[0].hint}`;
    hint = misses.length ? `Außerdem fehlt noch: ${misses.map((m) => m.concept).join(", ")}.` : "";
  } else if (correct && !misses.length) {
    const parts = [];
    if (analysis.declarations.length) {
      const d = analysis.declarations[0];
      parts.push(d.keyword ? `\`${d.keyword} ${d.name}\` ist korrekt deklariert` : `\`${d.name}\` ist gesetzt`);
    }
    if (analysis.functions.length) parts.push(`die Funktion \`${analysis.functions[0].name}\` ist definiert`);
    feedback = parts.length
      ? `Stark — ${parts.join(" und ")}. Die Lösung enthält alle erwarteten Bausteine und ist sauber aufgebaut.`
      : `Stark! Alle ${concepts.length} erwarteten Bausteine sind vorhanden und der ${label}-Code ist sauber aufgebaut.`;
  } else if (correct) {
    feedback = `Gut gelöst — ${hits.length} von ${concepts.length} Bausteinen sind da.`;
    hint = misses[0].why || `Für die volle Punktzahl fehlt noch: ${misses.map((m) => m.concept).join(", ")}.`;
  } else {
    // Wesentliche Lücken zuerst — sie erklären den Fehlschlag am besten.
    const explained = missingEssential.find((m) => m.why) || misses.find((m) => m.why) || misses[0];
    feedback = explained?.why || "Da fehlen noch wesentliche Teile der Lösung.";
    const rest = misses.filter((m) => m !== explained);
    hint = rest.length ? `Es fehlt außerdem: ${rest.map((m) => m.concept).join(", ")}.` : "Schau dir die Theorie links noch einmal an.";
  }

  // Stilhinweise anhängen, ohne die Bewertung zu kippen
  const styleNote = analysis.issues.find((i) => i.severity !== "error");
  if (correct && styleNote && !hint) hint = `${styleNote.title}: ${styleNote.hint}`;
  if (notes.length && !hint) hint = notes[0];

  return {
    correct, score, offline: true, feedback, hint,
    praise: correct ? (score >= 95 ? "Vorbildlich umgesetzt." : "Sauber umgesetzt.") : "",
    details: { concepts: results, issues: analysis.issues, declarations: analysis.declarations },
  };
}

/* --------------------- Bewertung: Lückentext -----------------------------
   Eine Lücke darf mehrere richtige Lösungen haben: steht in `blanks` ein
   Array, zählt jede Schreibweise darin. Zusätzlich gelten Zahlwörter und
   Ziffern als gleichwertig — wer „1“ statt „einmal“ schreibt, meint dasselbe.
   ------------------------------------------------------------------------- */
const NUMBER_WORDS = {
  null: "0", kein: "0", keine: "0",
  ein: "1", eine: "1", eins: "1", einmal: "1", einmalig: "1", genaueinmal: "1",
  zwei: "2", zweimal: "2", drei: "3", dreimal: "3", vier: "4", viermal: "4",
  fünf: "5", fuenf: "5", sechs: "6", sieben: "7", acht: "8", neun: "9", zehn: "10",
  beliebig: "*", beliebigoft: "*", mehrfach: "*", mehrmals: "*",
};

/** Vergleichsform einer Lücke: Groß-/Kleinschreibung, Satzzeichen und
 *  Zahlwörter werden vereinheitlicht. Reine Sonderzeichen bleiben erhalten. */
function normBlank(value) {
  const raw = String(value).trim().toLowerCase();
  const alnum = normalizeAlnum(raw);
  const key = alnum || raw;                    // "#" bleibt "#"
  return NUMBER_WORDS[key] || key;
}

function evaluateFillBlank(task, answers) {
  const blanks = task.blanks || [];
  const results = blanks.map((expected, i) => {
    const accepted = (Array.isArray(expected) ? expected : [expected]).map((v) => String(v).trim()).filter(Boolean);
    const target = accepted[0] || "";
    const given = String(answers?.[i] || "").trim();
    if (!given) return { ok: false, expected: target, given, reason: "leer" };

    // 1. Buchstabengetreu (nur Groß-/Kleinschreibung darf abweichen)
    const exact = accepted.find((a) => a.toLowerCase() === given.toLowerCase());
    if (exact) return { ok: true, expected: exact, given, caseOff: given !== exact };

    // 2. Tolerant: Klammern, Anführungszeichen, Satzzeichen und Zahlwörter.
    //    <strong> == strong, print() == print, einmal == 1.
    const givenNorm = normBlank(given);
    const match = accepted.find((a) => { const n = normBlank(a); return n && n === givenNorm; });
    if (match) return { ok: true, expected: match, given, formatted: true };

    // 3. Tippfehler? Gemessen an der Variante, die am besten passt.
    const best = accepted.reduce((acc, a) => {
      const d = editDistance(given.toLowerCase(), a.toLowerCase());
      return d < acc.d ? { d, a } : acc;
    }, { d: Infinity, a: target });
    return { ok: false, expected: target, given, alternatives: accepted.slice(1), typo: best.d <= Math.max(1, Math.floor(best.a.length / 4)) };
  });

  const hits = results.filter((r) => r.ok).length;
  const total = blanks.length;
  const correct = hits === total && total > 0;
  const score = total ? Math.round((hits / total) * 100) : 0;

  let feedback, hint = "", solutionHint = "";
  if (correct) {
    feedback = "Alle Lücken korrekt ausgefüllt!";
    const caseOff = results.find((r) => r.caseOff);
    if (caseOff) hint = `Kleinigkeit: Üblich ist die Schreibweise \`${caseOff.expected}\`.`;
  } else {
    const typo = results.find((r) => !r.ok && r.typo);
    const empty = results.filter((r) => !r.ok && r.reason === "leer");
    const wrong = results.map((r, i) => ({ ...r, index: i })).filter((r) => !r.ok);
    feedback = `${hits} von ${total} Lücken stimmen.`;

    // Die Lösung wird nicht sofort verraten — sonst rät man sich durch.
    // Stattdessen gibt es einen Anhaltspunkt; die Lektion blendet die
    // Auflösung erst nach mehreren Versuchen ein.
    solutionHint = `Richtig wäre: ${wrong.map((r) =>
      r.alternatives?.length ? `${r.expected} (auch ${r.alternatives.slice(0, 2).join(" / ")})` : r.expected
    ).join(", ")}.`;

    if (typo) {
      hint = `Fast — bei \`${typo.given}\` stimmt nur die Schreibweise noch nicht.`;
    } else if (empty.length === total) {
      hint = "Fülle zuerst alle Lücken aus.";
    } else {
      hint = wrong.map((r) => {
        const word = String(r.expected);
        return `Lücke ${r.index + 1}: beginnt mit „${word[0]}“ und hat ${word.length} Zeichen.`;
      }).join(" ");
    }
  }

  return { correct, score, offline: true, feedback, hint, solutionHint, praise: correct ? "Sauber gelöst!" : "" };
}

/* --------------------- Bewertung: Freitext ------------------------------- */
// Leichte deutsche Normalisierung: häufige Endungen abschneiden, damit
// "Variablen", "Variable" und "Variablen-" als dasselbe gelten.
function stemDe(word) {
  let w = String(word).toLowerCase().replace(/[^a-zäöüß]/g, "");
  for (const suffix of ["ungen", "enden", "ende", "erne", "ern", "end", "en", "er", "es", "em", "e", "n", "s"]) {
    if (w.length > suffix.length + 3 && w.endsWith(suffix)) { w = w.slice(0, -suffix.length); break; }
  }
  return w;
}

const REASONING_WORDS = /\b(weil|da|denn|damit|dadurch|sodass|so dass|deshalb|daher|somit|folglich|verhindert|ermöglicht|schützt|sorgt|bewirkt|bedeutet|führt dazu|vermeidet|garantiert)\b/i;
const EXAMPLE_WORDS = /\b(zum beispiel|z\.?b\.?|etwa|beispielsweise|wie etwa)\b/i;

/* Wer einen Vorteil benennt, begründet damit — auch ohne „weil“.
   „Es ist übersichtlicher“ ist eine Antwort auf ein Warum. */
// Achtung: `\b` funktioniert vor Umlauten NICHT — `\w` kennt kein „ü“, also
// gibt es zwischen Leerzeichen und „ü“ keine Wortgrenze. Deshalb wird der
// Wortanfang hier ausdrücklich über die erlaubten Trennzeichen beschrieben.
const BENEFIT_WORDS = /(?:^|[^A-Za-zÄÖÜäöüß])(übersichtlich|uebersichtlich|übersicht|lesbar|wartbar|wiederverwend|austauschbar|verständlich|verstaendlich|einfach|schnell|langsam|sicher|unsicher|fehleranfällig|robust|getrennt|trennung|unabhängig|unabhaengig|flexibel|struktur|ordnung|sauber|doppelt|redundan|effizien|performan|barrierefrei|zugänglich|eindeutig|konsistent|klar|übersichtlicher|aufwand|spart|spare|zeitspar)/i;

// Füllwörter zählen nicht als Inhalt — sonst gälte „damit das dann so ist“
// als ebenso gehaltvoll wie eine echte Begründung.
const STOPWORDS_DE = new Set([
  "aber", "also", "auch", "beim", "dann", "dass", "denn", "dies", "diese", "eine", "einen",
  "einer", "eines", "etwas", "immer", "kann", "können", "mann", "mehr", "muss", "nicht",
  "noch", "oder", "sehr", "sein", "sich", "sind", "über", "viel", "wenn", "werden", "wird",
  "damit", "dadurch", "deshalb", "daher", "somit", "weil", "sodass", "man", "wurde",
]);

/* --------------------- Ist das überhaupt ein Wort? -----------------------
   Tastaturgeklapper („gvsudfjsnvuf“) soll keine Punkte bekommen. Der
   entscheidende Hinweis sind die Konsonantenpaare: Deutsch erlaubt nur
   bestimmte Kombinationen, und am Wortanfang noch weniger. „fgsugugj“
   scheitert daran sofort, „Wartbarkeit“ nicht.
   ------------------------------------------------------------------------- */
// Konsonantenpaare, die im Deutschen (und in gängigen Lehnwörtern) vorkommen.
const CONSONANT_PAIRS = new Set([
  "bl", "br", "bs", "bt", "ch", "ck", "cl", "cr", "cs", "ct", "dg", "dr", "dt",
  "fl", "fr", "ft", "gh", "gl", "gn", "gr", "gs", "gt", "hl", "hm", "hn", "hr", "ht",
  "kl", "kn", "kr", "ks", "kt", "ld", "lf", "lg", "lk", "ll", "lm", "ln", "lp", "ls",
  "lt", "lv", "lz", "mb", "md", "mm", "mp", "ms", "mt", "nd", "nf", "ng", "nk", "nn",
  "ns", "nt", "nz", "pf", "ph", "pl", "pr", "ps", "pt", "rb", "rc", "rd", "rf", "rg",
  "rh", "rk", "rl", "rm", "rn", "rp", "rr", "rs", "rt", "rv", "rz", "sc", "sh", "sk",
  "sl", "sm", "sn", "sp", "ss", "st", "sz", "tb", "th", "tl", "tr", "ts", "tt", "tw",
  "tz", "vl", "vr", "wl", "wr", "zt", "zw", "ßt",
  // Fugen in zusammengesetzten Wörtern: Schlüsselwort, höchst, Halbwissen …
  "hs", "hw", "lw", "nw", "rw", "nh", "lh", "mh", "zd", "lb", "lc", "mf", "nb", "nm",
  "dl", "dn", "dm", "tm", "tn", "fs", "gd", "bd", "pp", "bb", "dd", "ff", "gg", "kk",
]);

// Womit ein deutsches Wort beginnen darf, wenn es mit mehreren Konsonanten anfängt.
const VALID_ONSETS = new Set([
  "bl", "br", "ch", "chr", "cl", "cr", "dr", "dw", "fl", "fr", "gl", "gn", "gr",
  "kl", "kn", "kr", "kw", "pf", "ph", "pl", "pr", "ps", "qu", "rh", "sc", "sch",
  "schl", "schm", "schn", "schr", "schw", "sh", "sk", "sl", "sm", "sn", "sp", "spl",
  "spr", "st", "str", "sw", "th", "tr", "tsch", "tw", "vl", "vr", "wr", "zw",
]);

const VOWELS_DE = /[aeiouäöüy]/;

function looksLikeWord(word) {
  const w = String(word).toLowerCase().replace(/[^a-zäöüß]/g, "");
  if (w.length < 2 || w.length > 22) return false;

  const vowels = (w.match(/[aeiouäöüy]/g) || []).length;
  if (!vowels) return false;
  const ratio = vowels / w.length;
  // „Herbst“ hat nur einen Vokal auf sechs Buchstaben — die Grenze muss das aushalten.
  if (ratio < 0.15 || ratio > 0.85) return false;
  if (/(.)\1\1/.test(w)) return false;                      // „aaa“ gibt es nicht

  // Konsonantengruppen einsammeln
  const groups = w.split(VOWELS_DE).filter(Boolean);
  if (groups.some((g) => g.length > 5)) return false;   // „Primärschlüssel“: rschl

  // Am Wortanfang ist Deutsch besonders streng: „fg…“ oder „gv…“ gibt es nicht.
  if (!VOWELS_DE.test(w[0])) {
    const onset = groups[0];
    if (onset.length > 1 && !VALID_ONSETS.has(onset) && !VALID_ONSETS.has(onset.slice(0, 3)) && !VALID_ONSETS.has(onset.slice(0, 2))) {
      return false;
    }
  }

  // Im Wortinneren darf einmal eine ungewöhnliche Fuge stehen (Komposita wie
  // „Wartbarkeit“), zweimal nicht mehr.
  let odd = 0;
  for (const group of groups) {
    for (let i = 0; i + 1 < group.length; i++) {
      if (!CONSONANT_PAIRS.has(group.slice(i, i + 2))) odd++;
    }
  }
  if (odd > 1) return false;
  if (odd === 1 && ratio < 0.22) return false;
  return true;
}

function evaluateExplanation(task, answer) {
  const text = String(answer || "").trim();
  const words = text.split(/\s+/).filter(Boolean);
  const sentences = text.split(/[.!?]+/).map((s) => s.trim()).filter((s) => s.length > 3);

  if (words.length < 3) {
    return { correct: false, score: 0, offline: true,
      feedback: "Da steht noch fast nichts.",
      hint: "Schreib mindestens ein bis zwei vollständige Sätze in eigenen Worten.", praise: "" };
  }

  // Zufallsbuchstaben zuerst abfangen — sonst bekämen sie eine Rückmeldung,
  // die so klingt, als hätte jemand inhaltlich etwas geschrieben.
  const realWords = words.filter(looksLikeWord);
  if (realWords.length / words.length < 0.5) {
    return { correct: false, score: 0, offline: true,
      feedback: "Das ergibt noch keinen lesbaren Text.",
      hint: "Schreib deine Erklärung bitte in ganzen deutschen Sätzen.", praise: "" };
  }

  // Erwartete Begriffe: aus expectedConcepts und den Code-Spans der Frage
  const fromQuestion = (task.question || "").match(/`([^`]+)`/g) || [];
  // Von der Aufgabenstellerin gesetzte Begriffe wiegen schwerer als die, die
  // ohnehin in der Frage stehen — wer `required` abschreibt, hat nichts gezeigt.
  const authored = [...new Set((task.expectedConcepts || [])
    .map((c) => String(Array.isArray(c) ? c[0] : c).toLowerCase().trim())
    .filter((c) => c.length > 1))];
  const expected = [...new Set([
    ...authored,
    ...fromQuestion.map((s) => s.replace(/`/g, "")),
  ].map((s) => String(s).toLowerCase().trim()).filter((s) => s.length > 1))];

  const answerStems = new Set(words.map(stemDe).filter(Boolean));
  const answerLower = text.toLowerCase();
  const covered = expected.filter((c) =>
    answerLower.includes(c) || answerStems.has(stemDe(c)) ||
    c.split(/\s+/).every((part) => answerStems.has(stemDe(part)))
  );
  const missing = expected.filter((c) => !covered.includes(c));
  const coverage = expected.length ? covered.length / expected.length : null;

  const hasConnective = REASONING_WORDS.test(text);
  // Einen Vorteil zu benennen ist ebenfalls eine Begründung.
  const hasBenefit = BENEFIT_WORDS.test(text);
  const hasReasoning = hasConnective || hasBenefit;
  const hasExample = EXAMPLE_WORDS.test(text);
  // Fragt die Aufgabe ausdrücklich nach dem Warum, reicht eine reine
  // Beschreibung nicht aus — dann ist die Begründung der Kern der Antwort.
  const wantsReason = /\b(warum|wieso|weshalb|begründe|aus welchem grund)\b/i.test(task.question || "");
  // Steht „kurz“ in der Frage, darf die Antwort auch kurz sein.
  const wantsBrief = /\b(kurz|knapp|in einem satz|in eigenen worten kurz|stichpunkt)/i.test(task.question || "");

  // Inhaltstragende Wörter: alles außer Füllwörtern und sehr kurzen Wörtern.
  // Sie messen Gehalt deutlich besser als die bloße Wortzahl.
  const contentWords = [...answerStems].filter((w) => w.length > 3 && !STOPWORDS_DE.has(w));

  // Wurde die Frage nur abgeschrieben? Das zeigt kein Verständnis. Entscheidend
  // ist der ANTEIL übernommener Wörter — eine kurze eigenständige Antwort
  // („damit es übersichtlicher ist“) darf hier nicht hängenbleiben.
  const questionWords = new Set((task.question || "").toLowerCase().split(/\s+/).map(stemDe).filter((w) => w.length > 3));
  const ownWords = contentWords.filter((w) => !questionWords.has(w));
  const borrowedWords = contentWords.filter((w) => questionWords.has(w));
  const borrowedShare = contentWords.length ? 1 - ownWords.length / contentWords.length : 0;
  const copiedFromQuestion = questionWords.size > 3 && contentWords.length >= 4
    && borrowedShare >= 0.8 && ownWords.length < 2;

  /* --------------------------- Themenbezug -------------------------------
     Der entscheidende Punkt: Eine Antwort muss etwas mit der Frage zu tun
     haben. „weil ich heute Pizza bestellen will“ ist grammatisch einwandfrei,
     enthält ein „weil“ und besteht aus echten Wörtern — trotzdem ist es keine
     Antwort. Erkannt wird das über drei Anker:
       • ein erwarteter Fachbegriff kommt vor,
       • ein Wort aus der Frage kommt vor (Themenwort),
       • oder es wird ein Vorteil benannt (bei Warum-Fragen).
     Fehlt alles drei, ist die Antwort am Thema vorbei.
     --------------------------------------------------------------------- */
  const topicAnchors = covered.length + borrowedWords.length + (hasBenefit ? 1 : 0);
  const offTopic = topicAnchors === 0;

  // Hat die Aufgabe ausdrücklich Fachbegriffe genannt und kommt keiner davon
  // vor, geht die Antwort am Kern vorbei — egal wie flüssig sie klingt.
  const coveredAuthored = authored.filter((c) => covered.includes(c));
  const missesAllConcepts = authored.length >= 1 && coveredAuthored.length === 0;

  if (offTopic) {
    return { correct: false, score: 0, offline: true,
      feedback: "Das beantwortet die Frage nicht.",
      hint: "Beziehe dich auf das Thema der Aufgabe — nutze die Begriffe, um die es geht.",
      praise: "" };
  }

  let score = 0;
  score += Math.min(25, contentWords.length * 6);                // Gehalt
  score += Math.min(10, words.length * 0.8);                     // Ausführlichkeit
  score += sentences.length >= 2 ? 12 : 6;                       // Satzbau
  score += hasReasoning ? 25 : 0;                                // Begründung
  score += hasExample ? 5 : 0;                                   // Beispiel
  score += coverage === null ? 20 : Math.round(coverage * 25);   // Fachbegriffe
  if (copiedFromQuestion) score = Math.min(score, 35);
  if (wantsReason && !hasReasoning) score = Math.min(score, 50);
  if (missesAllConcepts) score = Math.min(score, 45);
  score = Math.max(0, Math.min(100, Math.round(score)));

  const minWords = wantsBrief ? 4 : 6;
  const minContent = wantsBrief ? 1 : 2;
  const correct = score >= 55 && words.length >= minWords && contentWords.length >= minContent
    && !copiedFromQuestion && !missesAllConcepts
    && !(wantsReason && !hasReasoning);

  let feedback, hint = "";
  if (copiedFromQuestion) {
    feedback = "Das ist im Wesentlichen die Frage in anderer Reihenfolge.";
    hint = "Erkläre es mit eigenen Worten — was passiert da, und warum?";
  } else if (missesAllConcepts) {
    feedback = "Die Antwort geht am Kern der Frage vorbei.";
    hint = `Es geht um ${authored.slice(0, 2).map((m) => `\`${m}\``).join(" und ")} — darauf solltest du eingehen.`;
  } else if (wantsReason && !hasReasoning) {
    feedback = "Du beschreibst korrekt, was passiert — die Frage zielt aber auf die Begründung.";
    hint = "Ergänze das „Warum“, zum Beispiel mit „weil …“ oder „dadurch …“.";
  } else if (correct && score >= 85) {
    feedback = hasReasoning
      ? "Sehr gute Erklärung — du benennst nicht nur das Was, sondern begründest auch das Warum."
      : "Sehr gute, ausführliche Erklärung mit den passenden Fachbegriffen.";
  } else if (correct && score >= 70) {
    feedback = "Solide Erklärung — die Kernidee sitzt.";
    if (missing.length) hint = `Noch treffender wird es mit ${missing.slice(0, 2).map((m) => `\`${m}\``).join(" und ")}.`;
    else if (!hasReasoning) hint = "Eine kurze Begründung („weil …“) würde es abrunden.";
  } else if (correct) {
    feedback = hasReasoning
      ? "Das ist ein echter Grund — die Kernidee hast du verstanden."
      : "Die Grundidee hast du verstanden.";
    hint = hasReasoning ? "Noch konkreter wird es, wenn du sagst, was genau dadurch besser wird." : "Ergänze das „Warum“ — zum Beispiel mit „weil …“.";
  } else if (words.length < minWords || contentWords.length < minContent) {
    feedback = hasReasoning
      ? "Die Richtung stimmt, die Antwort ist aber noch sehr knapp."
      : "Die Erklärung ist noch zu knapp, um dein Verständnis zu zeigen.";
    hint = "Ein vollständiger Satz mit einem konkreten Grund reicht schon.";
  } else if (!hasReasoning) {
    feedback = "Du beschreibst, was passiert — es fehlt aber die Begründung.";
    hint = "Erkläre auch, *warum* es so ist (z.B. mit „weil …“ oder „dadurch …“).";
  } else {
    feedback = "Die Richtung stimmt, aber es fehlen noch die zentralen Begriffe.";
    hint = missing.length ? `Gehe auf ${missing.slice(0, 2).map((m) => `\`${m}\``).join(" und ")} ein.` : "Werde etwas konkreter.";
  }

  return {
    correct, score, offline: true, feedback, hint,
    praise: correct ? (score >= 85 ? "Klar auf den Punkt gebracht." : "Verständlich erklärt.") : "",
    details: { covered, missing, hasReasoning, topicAnchors, words: words.length },
  };
}

/* ------------------------- Öffentlicher Einstieg -------------------------- */
/**
 * Bewertet eine Antwort — vollständig lokal, ohne Netzwerk.
 * Dies ist der einzige Prüfweg für Lektionen.
 */
function analyzeAnswer(task, userAnswer, langId) {
  if (task.type === "fill_blank") return evaluateFillBlank(task, task._blankAnswers || userAnswer);
  if (task.type === "code_write") return evaluateCode(task, userAnswer, langId);
  return evaluateExplanation(task, userAnswer);
}

/* -------------------- Code-Prüfung für den Editor ------------------------ */
/**
 * Analysiert HTML, CSS und JavaScript gemeinsam — für die Fehlerprüfung
 * im Code-Editor. Nutzt dieselben Sprachprofile.
 */
function analyzeProject({ html, css, js }) {
  const issues = [];
  const add = (where, severity, title, detail, fix) => issues.push({ where, severity, title, detail, fix });

  for (const [where, code] of [["html", html], ["css", css], ["js", js]]) {
    if (!String(code || "").trim()) continue;
    const langId = where === "js" ? "javascript" : where;
    const a = analyzeCode(code, langId);

    if (!a.structure.ok) {
      for (const p of a.structure.problems.slice(0, 3)) {
        if (p.tag) {
          add(where, "error", `<${p.tag}> nicht geschlossen`,
            p.kind === "unclosed" ? `Das Element <${p.tag}> wird geöffnet, aber nie geschlossen.` : `</${p.tag}> hat kein passendes öffnendes Element.`,
            p.kind === "unclosed" ? `Ergänze </${p.tag}>.` : `Entferne das überzählige </${p.tag}>.`);
        } else if (p.kind === "unclosed") {
          add(where, "error", `Klammer \`${p.ch}\` nicht geschlossen`, `Eine öffnende \`${p.ch}\` hat keine passende \`${p.expected}\`.`, `Ergänze \`${p.expected}\`.`);
        } else if (p.kind === "extraClose") {
          add(where, "error", `Überzählige \`${p.ch}\``, `\`${p.ch}\` schließt etwas, das nie geöffnet wurde.`, `Entferne die Klammer oder ergänze die passende Öffnung.`);
        } else {
          add(where, "error", "Klammern falsch verschachtelt", `Erwartet wurde \`${p.expected}\`, gefunden \`${p.got}\`.`, "Prüfe die Reihenfolge der Klammern.");
        }
      }
    }
    for (const iss of a.issues) add(where, iss.severity, iss.title, iss.hint, iss.hint);
  }

  // Verbindungen zwischen den Dateien prüfen — das findet echte Fehler.
  const jsCode = String(js || "");
  const htmlCode = String(html || "");
  const idRefs = [...jsCode.matchAll(/getElementById\(\s*["'`]([^"'`]+)["'`]\s*\)/g)].map((m) => m[1]);
  for (const id of [...new Set(idRefs)]) {
    if (!new RegExp(`id\\s*=\\s*["']${id}["']`).test(htmlCode)) {
      add("js", "error", `Element \`#${id}\` existiert nicht`,
        `Das Skript sucht ein Element mit der id "${id}", im HTML gibt es keines.`,
        `Ergänze im HTML ein Element mit id="${id}" — oder korrigiere den Namen im Skript.`);
    }
  }
  const classRefs = [...jsCode.matchAll(/querySelector(?:All)?\(\s*["'`]\.([\w-]+)["'`]\s*\)/g)].map((m) => m[1]);
  for (const cls of [...new Set(classRefs)]) {
    if (!new RegExp(`class\\s*=\\s*["'][^"']*\\b${cls}\\b`).test(htmlCode)) {
      add("js", "warning", `Klasse \`.${cls}\` nicht im HTML gefunden`,
        `Das Skript sucht Elemente mit der Klasse "${cls}".`,
        `Vergib die Klasse im HTML oder passe den Selektor an.`);
    }
  }

  const errors = issues.filter((i) => i.severity === "error").length;
  const warnings = issues.filter((i) => i.severity === "warning").length;
  return {
    offline: true,
    summary: issues.length === 0
      ? "Keine Probleme gefunden — der Code ist strukturell sauber."
      : `${issues.length} Hinweis${issues.length === 1 ? "" : "e"}${errors ? `, davon ${errors} kritisch` : warnings ? `, davon ${warnings} Warnung${warnings === 1 ? "" : "en"}` : ""}.`,
    issues,
  };
}

/* ========================= Reusable UI ============================= */

/* ========================== Eigene Symbole ===============================
   Emoji sehen auf jedem Betriebssystem anders aus: unter Windows anders als
   unter macOS, unter Android wieder anders. Für eine Oberfläche, die überall
   gleich aussehen soll, taugen sie deshalb nicht.

   Diese Symbole sind selbst gezeichnet — schlichte geometrische Formen, die
   auch bei 16 Pixeln noch lesbar sind. Sie sind bewusst KEINE Nachbauten der
   offiziellen Sprachlogos, sondern eigene Marken in der jeweiligen Farbe.
   ========================================================================= */
function LdIcon({ name, size = 24, color = "currentColor", className = "", title }) {
  const s = size;
  const common = {
    width: s, height: s, viewBox: "0 0 24 24",
    fill: "none", stroke: color, strokeWidth: 1.9,
    strokeLinecap: "round", strokeLinejoin: "round",
    className, "aria-hidden": title ? undefined : true,
    style: { display: "block", flexShrink: 0 },
  };
  const glyphs = {
    /* ------------------------------ Sprachen ---------------------------- */
    // Spitze Klammern mit Schrägstrich — das Zeichen für Auszeichnungssprache
    html: <><path d="M8 6 3 12l5 6" /><path d="M16 6l5 6-5 6" /><path d="M13.5 4l-3 16" /></>,
    // Pinselstrich
    css: <><path d="M5 19c1.5-3 3-4 5-4 3 0 3-3 3-5 0-3 2-5 5-5" /><circle cx="6" cy="18" r="2.2" fill={color} stroke="none" /></>,
    // Blitz
    javascript: <path d="M13 2 5 13h5l-1 9 9-12h-5l1-8Z" />,
    // Schild mit Haken
    typescript: <><path d="M12 3 5 6v6c0 4 3 7 7 9 4-2 7-5 7-9V6l-7-3Z" /><path d="M9 12l2.2 2.2L15.5 10" /></>,
    // Atom
    react: <><circle cx="12" cy="12" r="2" fill={color} stroke="none" /><ellipse cx="12" cy="12" rx="9.5" ry="4" /><ellipse cx="12" cy="12" rx="9.5" ry="4" transform="rotate(60 12 12)" /><ellipse cx="12" cy="12" rx="9.5" ry="4" transform="rotate(120 12 12)" /></>,
    // V aus zwei Winkeln
    vue: <><path d="M2.5 5h4l5.5 10L17.5 5h4L12 21 2.5 5Z" /><path d="M8 5h2.5l1.5 3 1.5-3H16" /></>,
    // Zwei ineinandergreifende Bögen
    python: <><path d="M12 3c-3.3 0-4.5 1.4-4.5 3.5V9h4.5" /><path d="M7.5 9H5.2C3.4 9 2.5 10.4 2.5 12.5S3.4 16 5.2 16h2.3v-2.5c0-2 1.2-3.5 4.5-3.5" /><path d="M12 21c3.3 0 4.5-1.4 4.5-3.5V15H12" /><path d="M16.5 15h2.3c1.8 0 2.7-1.4 2.7-3.5S20.6 8 18.8 8h-2.3v2.5c0 2-1.2 3.5-4.5 3.5" /></>,
    // Tasse mit Dampf
    java: <><path d="M4 11h13v5a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4v-5Z" /><path d="M17 12h1.5a2.5 2.5 0 0 1 0 5H17" /><path d="M8 3c-1 1.2-1 2.3 0 3.5M12 2.5c-1.2 1.4-1.2 2.7 0 4" /></>,
    // Quadrat mit diagonaler Teilung
    kotlin: <><rect x="3.5" y="3.5" width="17" height="17" rx="2.5" /><path d="M20.5 3.5 3.5 20.5M12 3.5 3.5 12" /></>,
    // Offener Ring
    c: <path d="M18 7a7.5 7.5 0 1 0 0 10" />,
    // Ring mit Pluszeichen
    cpp: <><path d="M13.5 7.5a6 6 0 1 0 0 9" /><path d="M17 9v5M14.5 11.5h5" /></>,
    // Kreis mit zwei Punkten und Spur
    go: <><circle cx="13" cy="12" r="7" /><circle cx="11" cy="10.5" r="1" fill={color} stroke="none" /><circle cx="15" cy="10.5" r="1" fill={color} stroke="none" /><path d="M6 9.5H2M6 14.5H3.5" /></>,
    // Zahnradring
    rust: <><circle cx="12" cy="12" r="6" /><circle cx="12" cy="12" r="2.4" /><path d="M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5 19 19M19 5l-1.5 1.5M6.5 17.5 5 19" /></>,
    // Ellipse mit Balken
    php: <><ellipse cx="12" cy="12" rx="10" ry="6.5" /><path d="M7.5 14.5 9 9.5h1.8c1 0 1.5.6 1.2 1.6-.3 1-1 1.5-2 1.5H8.6" /><path d="M14 14.5 15.5 9.5h1.8c1 0 1.5.6 1.2 1.6-.3 1-1 1.5-2 1.5h-1.4" /></>,
    // Datenbankzylinder
    sql: <><ellipse cx="12" cy="6" rx="7.5" ry="3" /><path d="M4.5 6v12c0 1.7 3.4 3 7.5 3s7.5-1.3 7.5-3V6" /><path d="M4.5 12c0 1.7 3.4 3 7.5 3s7.5-1.3 7.5-3" /></>,

    /* -------------------------------- Ligen ------------------------------ */
    medal: <><circle cx="12" cy="14.5" r="6" /><path d="M8.5 9 6 2.5h12L15.5 9" /></>,
    diamond: <><path d="M12 2.5 21.5 12 12 21.5 2.5 12 12 2.5Z" /><path d="M7.2 12h9.6M12 7.2v9.6" /></>,
    gem: <><path d="M6 3h12l4 6-10 12L2 9l4-6Z" /><path d="M2 9h20M9 3l-3 6 6 12 6-12-3-6" /></>,
    crown: <><path d="M3 8l3.5 4L12 5l5.5 7L21 8v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8Z" /><circle cx="12" cy="15" r="1.3" fill={color} stroke="none" /></>,

    /* ------------------------------- Schritte ---------------------------- */
    // Aufgeschlagenes Buch
    theorie: <><path d="M12 6.5C10 4.8 7.5 4 4 4v13c3.5 0 6 .8 8 2.5 2-1.7 4.5-2.5 8-2.5V4c-3.5 0-6 .8-8 2.5Z" /><path d="M12 6.5v13" /></>,
    // Stift auf Blatt
    aufgabe: <><path d="M5 3h9l5 5v6" /><path d="M14 3v5h5" /><path d="M19.5 15.5 13 22H9.5v-3.5l6.5-6.5a1.8 1.8 0 0 1 2.5 0l1 1a1.8 1.8 0 0 1 0 2.5Z" /></>,
    // Sprechblase mit Haken
    feedback: <><path d="M21 12a8 8 0 0 1-8 8H8l-5 3 1.5-4.5A8 8 0 1 1 21 12Z" /><path d="M8.5 11.5 11 14l4.5-4.5" /></>,

    /* ------------------------------ Abzeichen ---------------------------- */
    flamme: <><path d="M12 2c1.5 4 5 5.5 5 10a5 5 0 0 1-10 0c0-1.5.5-2.5 1.5-3.5C9 10.5 10 8 12 2Z" /><path d="M12 21a3 3 0 0 0 3-3c0-1.5-1.5-2.5-3-4.5-1.5 2-3 3-3 4.5a3 3 0 0 0 3 3Z" /></>,
    stern: <path d="m12 2.8 2.9 5.9 6.5.9-4.7 4.6 1.1 6.5-5.8-3-5.8 3 1.1-6.5L2.6 9.6l6.5-.9L12 2.8Z" />,
    ziel: <><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="5" /><circle cx="12" cy="12" r="1.4" fill={color} stroke="none" /></>,
    raute: <><path d="M12 2.5 21.5 12 12 21.5 2.5 12 12 2.5Z" /></>,
  };

  return (
    <svg {...common} role={title ? "img" : undefined}>
      {title && <title>{title}</title>}
      {glyphs[name] || glyphs.raute}
    </svg>
  );
}

// Welches Symbol gehört zu welchem Kurs
const COURSE_ICONS = {
  html: "html", css: "css", javascript: "javascript", typescript: "typescript",
  react: "react", vue: "vue", python: "python", java: "java", kotlin: "kotlin",
  c: "c", cpp: "cpp", go: "go", rust: "rust", php: "php", sql: "sql",
};

/** Das Symbol eines Kurses in seiner Farbe. */
function CourseIcon({ course, size = 24, className = "" }) {
  if (!course) return null;
  return (
    <LdIcon name={COURSE_ICONS[course.id] || "raute"} size={size} color={course.color}
      className={className} title={course.name} />
  );
}

// Ligen: Symbol und Farbe kommen aus LEAGUES
const LEAGUE_ICONS = {
  bronze: "medal", silber: "medal", gold: "medal",
  platin: "diamond", diamant: "gem", meister: "crown",
};

function LeagueIcon({ league, size = 24, className = "" }) {
  const l = typeof league === "string" ? leagueById(league) : league;
  if (!l) return null;
  return <LdIcon name={LEAGUE_ICONS[l.id] || "medal"} size={size} color={l.color} className={className} title={l.name} />;
}


function renderInline(text, kp) {
  const parts = [];
  const regex = /(`[^`]+`)|(\*\*[^*]+\*\*)|(\*[^*]+\*)|(\[[^\]]+\]\([^)]+\))/g;
  let last = 0, m, i = 0;
  while ((m = regex.exec(text))) {
    if (m.index > last) parts.push(text.slice(last, m.index));
    if (m[1]) parts.push(<code key={kp + "c" + i} className="font-code text-[13px] px-1.5 py-0.5 rounded bg-[#0A0E1A] border border-[#1E2D4A] text-[#4F8EF7]">{m[1].slice(1, -1)}</code>);
    else if (m[2]) parts.push(<strong key={kp + "b" + i} className="font-semibold text-[#E8EDF5]">{m[2].slice(2, -2)}</strong>);
    else if (m[3]) parts.push(<em key={kp + "i" + i} className="italic text-[#8A9BC0]">{m[3].slice(1, -1)}</em>);
    else if (m[4]) { const lm = m[4].match(/\[([^\]]+)\]\(([^)]+)\)/); parts.push(<a key={kp + "a" + i} href={lm[2]} target="_blank" rel="noopener noreferrer" className="text-[#4F8EF7] underline decoration-[#4F8EF7]/40 hover:decoration-[#4F8EF7]">{lm[1]}</a>); }
    last = regex.lastIndex; i++;
  }
  if (last < text.length) parts.push(text.slice(last));
  return parts;
}

function CodeBlock({ code, lang }) {
  const [copied, setCopied] = useState(false);
  const copy = () => { try { navigator.clipboard.writeText(code); } catch (e) {} setCopied(true); setTimeout(() => setCopied(false), 1200); };
  return (
    <div className="my-4 rounded-xl overflow-hidden border border-[#1E2D4A] bg-[#0A0E1A]" style={{ borderLeft: "3px solid #4F8EF7" }}>
      <div className="flex items-center justify-between px-3 py-1.5 bg-[#0F1629] border-b border-[#1E2D4A]">
        <span className="font-code text-[11px] text-[#4A5A7A] uppercase tracking-wider">{lang || "code"}</span>
        <button onClick={copy} className="flex items-center gap-1 text-[11px] text-[#8A9BC0] hover:text-[#E8EDF5] transition-colors">
          {copied ? <Check size={13} className="text-[#10B981]" /> : <Copy size={13} />}{copied ? "Kopiert" : "Copy"}
        </button>
      </div>
      <pre className="p-3.5 overflow-x-auto"><code className="font-code text-[13px] leading-relaxed text-[#C9D6F0] whitespace-pre">{code}</code></pre>
    </div>
  );
}

function Markdown({ text }) {
  const lines = (text || "").split("\n");
  const blocks = [];
  let i = 0, key = 0;
  while (i < lines.length) {
    let line = lines[i];
    // Code fence
    if (line.trim().startsWith("```")) {
      const lang = line.trim().slice(3).trim();
      const buf = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith("```")) { buf.push(lines[i]); i++; }
      i++;
      blocks.push(<CodeBlock key={key++} code={buf.join("\n")} lang={lang} />);
      continue;
    }
    // Table
    if (line.trim().startsWith("|") && i + 1 < lines.length && /^\s*\|?[\s:|-]+\|?\s*$/.test(lines[i + 1]) && lines[i + 1].includes("-")) {
      const header = line.split("|").map((c) => c.trim()).filter((c, idx, a) => !(idx === 0 && c === "") && !(idx === a.length - 1 && c === ""));
      i += 2;
      const rows = [];
      while (i < lines.length && lines[i].trim().startsWith("|")) {
        rows.push(lines[i].split("|").map((c) => c.trim()).filter((c, idx, a) => !(idx === 0 && c === "") && !(idx === a.length - 1 && c === "")));
        i++;
      }
      blocks.push(
        <div key={key++} className="my-4 overflow-x-auto rounded-xl border border-[#1E2D4A]">
          <table className="w-full text-sm">
            <thead><tr className="bg-[#0F1629]">{header.map((h, hi) => <th key={hi} className="text-left px-3 py-2 font-semibold text-[#E8EDF5] border-b border-[#1E2D4A]">{renderInline(h, "th" + hi)}</th>)}</tr></thead>
            <tbody>{rows.map((r, ri) => <tr key={ri} className="border-b border-[#1E2D4A]/50 last:border-0">{r.map((c, ci) => <td key={ci} className="px-3 py-2 text-[#8A9BC0]">{renderInline(c, "td" + ri + ci)}</td>)}</tr>)}</tbody>
          </table>
        </div>
      );
      continue;
    }
    // Blockquote
    if (line.trim().startsWith(">")) {
      const buf = [];
      while (i < lines.length && lines[i].trim().startsWith(">")) { buf.push(lines[i].replace(/^\s*>\s?/, "")); i++; }
      blocks.push(<blockquote key={key++} className="my-4 pl-4 py-2 border-l-2 border-[#4F8EF7] bg-[#4F8EF7]/5 rounded-r-lg text-[#C9D6F0]">{renderInline(buf.join(" "), "bq" + key)}</blockquote>);
      continue;
    }
    // Headings
    if (/^#{1,6}\s/.test(line)) {
      const m = line.match(/^(#{1,6})\s+(.*)$/);
      const lvl = m[1].length;
      const cls = lvl === 1 ? "font-display text-2xl font-bold mt-2 mb-3 text-[#E8EDF5]" : lvl === 2 ? "font-display text-xl font-bold mt-5 mb-2 text-[#E8EDF5]" : "font-display text-base font-semibold mt-4 mb-2 text-[#C9D6F0]";
      blocks.push(React.createElement(`h${Math.min(lvl, 4)}`, { key: key++, className: cls }, renderInline(m[2], "h" + key)));
      i++;
      continue;
    }
    // Lists
    if (/^\s*([-*]|\d+\.)\s/.test(line)) {
      const items = [];
      const ordered = /^\s*\d+\.\s/.test(line);
      while (i < lines.length && /^\s*([-*]|\d+\.)\s/.test(lines[i])) {
        items.push(lines[i].replace(/^\s*([-*]|\d+\.)\s/, "")); i++;
      }
      blocks.push(React.createElement(ordered ? "ol" : "ul", { key: key++, className: `my-3 ml-5 space-y-1 ${ordered ? "list-decimal" : "list-disc"} text-[#8A9BC0] marker:text-[#4F8EF7]` },
        items.map((it, idx) => <li key={idx} className="pl-1">{renderInline(it, "li" + idx)}</li>)));
      continue;
    }
    // Empty
    if (line.trim() === "") { i++; continue; }
    // Paragraph
    blocks.push(<p key={key++} className="my-3 leading-relaxed text-[#8A9BC0]">{renderInline(line, "p" + key)}</p>);
    i++;
  }
  return <div>{blocks}</div>;
}

function ProgressBar({ value, max, className = "", height = "h-2" }) {
  const pct = max ? Math.min(100, Math.round((value / max) * 100)) : 0;
  return (
    <div className={`w-full ${height} rounded-full bg-[#1A2540] overflow-hidden ${className}`}>
      <div className={`${height} rounded-full transition-all duration-700`} style={{ width: pct + "%", background: GRADIENT }} />
    </div>
  );
}

function DifficultyBadge({ level }) {
  const d = DIFF[level] || DIFF.beginner;
  return <span className="text-[11px] font-medium px-2 py-0.5 rounded-full" style={{ color: d.color, background: d.color + "22" }}>{d.label}</span>;
}

function Btn({ children, onClick, variant = "primary", className = "", icon: Icon, disabled, type, size = "md", ariaLabel }) {
  const base = "inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed";
  const sz = size === "sm" ? "text-sm px-3 py-1.5" : size === "lg" ? "text-base px-6 py-3.5" : "text-sm px-4 py-2.5";
  const styles = {
    primary: "text-white shadow-lg hover:opacity-90",
    secondary: "bg-transparent border border-[#2A3F6F] text-[#E8EDF5] hover:border-[#4F8EF7] hover:bg-white/5",
    ghost: "bg-transparent text-[#8A9BC0] hover:text-[#E8EDF5] hover:bg-white/5",
    danger: "bg-[#EF4444]/10 text-[#EF4444] border border-[#EF4444]/30 hover:bg-[#EF4444]/20",
  };
  return (
    <button type={type} disabled={disabled} onClick={onClick} aria-label={ariaLabel}
      className={`${base} ${sz} ${styles[variant]} ${className}`}
      style={variant === "primary" ? { background: GRADIENT } : undefined}>
      {Icon && <Icon size={size === "lg" ? 20 : 16} />}{children}
    </button>
  );
}

function Card({ children, className = "", onClick, hover }) {
  return (
    <div onClick={onClick}
      className={`bg-[#141D35] border border-[#1E2D4A] rounded-xl ${hover ? "hover:border-[#2A3F6F] transition-all duration-300" : ""} ${onClick ? "cursor-pointer" : ""} ${className}`}>
      {children}
    </div>
  );
}

/* Animated terminal background for the landing hero */
const TERMINAL_LINES = [
  "const learn = (lang) => `${lang} mastered`;",
  "function helloWorld() { return '👋'; }",
  "for (let i = 0; i < lessons.length; i++) {",
  "  await checkAnswer(lessons[i]);",
  "}",
  "class Developer extends Human {",
  "  constructor(name) { super(name); this.xp = 0; }",
  "  levelUp() { this.xp += 50; }",
  "}",
  "SELECT * FROM students WHERE streak > 7;",
  "print('Python ist einfach 🐍')",
  "public static void main(String[] args) {}",
  "#include <iostream>",
  "std::cout << \"C++ nah am Metal\" << std::endl;",
  "git commit -m 'feat: lerne jeden Tag'",
  "npm run build && deploy()",
  "const xp = streak * multiplier + bonus;",
  ".hero { background: linear-gradient(135deg,#4F8EF7,#7C3AED); }",
  "if (score > 90) award('ai_master');",
  "export default function App() { return <Learn /> }",
];

function TerminalBackground() {
  const doubled = [...TERMINAL_LINES, ...TERMINAL_LINES];
  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none select-none" aria-hidden>
      <div className="absolute inset-0 grid grid-cols-2 gap-12 px-8" style={{ opacity: 0.07 }}>
        {[0, 1].map((col) => (
          <div key={col} className="relative overflow-hidden">
            <div style={{ animation: `ld-scroll-up ${col ? 38 : 30}s linear infinite` }}>
              {doubled.map((l, i) => (
                <div key={i} className="font-code text-sm text-[#4F8EF7] py-1.5 whitespace-nowrap">
                  <span className="text-[#4A5A7A] mr-3">{String((i % TERMINAL_LINES.length) + 1).padStart(2, "0")}</span>{l}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
      <div className="absolute inset-0" style={{ background: "radial-gradient(ellipse at center, transparent 0%, #0A0E1A 75%)" }} />
    </div>
  );
}

function Logo({ size = "md", onClick }) {
  const txt = size === "lg" ? "text-2xl" : "text-lg";
  const ic = size === "lg" ? 28 : 22;
  return (
    <div onClick={onClick} className={`flex items-center gap-2 ${onClick ? "cursor-pointer" : ""}`}>
      <div className="flex items-center justify-center rounded-lg p-1.5" style={{ background: GRADIENT }}>
        <Code2 size={ic} className="text-white" />
      </div>
      <span className={`font-display font-bold ${txt} text-[#E8EDF5]`}>Learn<span className="ld-gradient-text">Developing</span></span>
    </div>
  );
}

/* Toast notifications */
function Toasts({ toasts }) {
  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 max-w-[340px]">
      {toasts.map((t) => {
        const cfg = {
          success: { icon: CheckCircle2, color: "#10B981" },
          error: { icon: XCircle, color: "#EF4444" },
          badge: { icon: Award, color: "#F7C948" },
          info: { icon: Sparkles, color: "#4F8EF7" },
        }[t.type] || { icon: Sparkles, color: "#4F8EF7" };
        const Icon = cfg.icon;
        return (
          <div key={t.id} style={{ animation: "ld-toast-in .25s ease-out" }}
            className="flex items-center gap-3 bg-[#141D35] border border-[#1E2D4A] rounded-xl px-4 py-3 shadow-2xl">
            <Icon size={20} style={{ color: cfg.color }} className="shrink-0" />
            <span className="text-sm text-[#E8EDF5]">{t.msg}</span>
          </div>
        );
      })}
    </div>
  );
}

/* XP popup overlay */
function XPPopup({ amount }) {
  return (
    <div className="fixed inset-0 z-[90] flex items-center justify-center pointer-events-none">
      <div style={{ animation: "ld-fade-up 1.6s ease-out forwards" }}
        className="flex items-center gap-2 font-display font-black text-4xl" >
        <span style={{ color: "#F7C948" }}>+{amount} XP</span>
        <Star size={36} className="fill-[#F7C948] text-[#F7C948]" />
      </div>
    </div>
  );
}

/* Code-Editor mit Zeilennummern + Tab-Support */
function CodeEditor({ value, onChange, disabled, lang }) {
  const ref = useRef(null);
  const gutterRef = useRef(null);
  const lines = (value || "").split("\n");
  const onKeyDown = (e) => {
    if (e.key === "Tab") {
      e.preventDefault();
      const el = e.target;
      const s = el.selectionStart, end = el.selectionEnd;
      const next = value.slice(0, s) + "  " + value.slice(end);
      onChange(next);
      requestAnimationFrame(() => { el.selectionStart = el.selectionEnd = s + 2; });
    }
  };
  const syncScroll = (e) => { if (gutterRef.current) gutterRef.current.scrollTop = e.target.scrollTop; };
  return (
    <div className="rounded-lg overflow-hidden border border-[#1E2D4A] bg-[#0A0E1A]" style={{ borderLeft: "3px solid #4F8EF7" }}>
      <div className="flex items-center justify-between px-3 py-1.5 bg-[#0F1629] border-b border-[#1E2D4A]">
        <div className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-[#EF4444]/60" /><span className="w-2.5 h-2.5 rounded-full bg-[#F59E0B]/60" /><span className="w-2.5 h-2.5 rounded-full bg-[#10B981]/60" />
        </div>
        <span className="font-code text-[11px] text-[#4A5A7A] uppercase">{lang}</span>
      </div>
      <div className="flex max-h-72 overflow-hidden">
        <div ref={gutterRef} className="select-none overflow-hidden py-3 pl-3 pr-2 text-right font-code text-[13px] leading-relaxed text-[#4A5A7A] bg-[#0A0E1A] border-r border-[#1E2D4A]/60" aria-hidden>
          {lines.map((_, i) => <div key={i}>{i + 1}</div>)}
        </div>
        <textarea ref={ref} value={value} onChange={(e) => onChange(e.target.value)} onKeyDown={onKeyDown} onScroll={syncScroll}
          disabled={disabled} spellCheck={false} rows={Math.max(7, lines.length)} aria-label="Code-Editor"
          className="flex-1 bg-transparent p-3 font-code text-[13px] text-[#C9D6F0] resize-none leading-relaxed overflow-auto" placeholder="// Dein Code …" />
      </div>
    </div>
  );
}

/* Der eingebaute Editor für Code-Aufgaben — mit einfachem Fallback */
const MONACO_LANG = { javascript: "javascript", html: "html", css: "css", java: "java", python: "python", sql: "sql", cpp: "cpp" };
const MONACO_VS = "https://cdn.jsdelivr.net/npm/monaco-editor@0.52.2/min/vs";
let monacoConfigured = false;

// Plain-Function-Wrapper (KEINE verschachtelte Komponente -> der Editor bleibt erhalten)
function editorChrome(courseId, label, right, children) {
  const ext = { javascript: "js", python: "py", java: "java", html: "html", css: "css", sql: "sql", cpp: "cpp" }[courseId] || "txt";
  return (
    <div className="rounded-lg overflow-hidden border border-[#1E2D4A] bg-[#0A0E1A]" style={{ borderLeft: "3px solid #4F8EF7" }}>
      <div className="flex items-center justify-between px-3 py-1.5 bg-[#0F1629] border-b border-[#1E2D4A]">
        <div className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-[#EF4444]/60" /><span className="w-2.5 h-2.5 rounded-full bg-[#F59E0B]/60" /><span className="w-2.5 h-2.5 rounded-full bg-[#10B981]/60" />
          <span className="ml-2 font-code text-[11px] text-[#4A5A7A]">solution.{ext}</span>
        </div>
        <span className="font-code text-[10px] text-[#4A5A7A] flex items-center gap-1">{right}</span>
      </div>
      {children}
    </div>
  );
}

/* Emmet-Unterstützung: `!` + Tab erzeugt ein HTML-Grundgerüst, `ul>li*3`
   erzeugt Listen usw. Wird nur einmal je Editor-Instanz registriert. */
let emmetRegistered = false;
async function enableEmmet(monaco) {
  if (emmetRegistered) return;
  emmetRegistered = true;
  try {
    const emmet = await import("emmet-monaco-es");
    emmet.emmetHTML(monaco, ["html"]);
    emmet.emmetCSS(monaco, ["css", "scss", "less"]);
    emmet.emmetJSX(monaco, ["javascript", "typescript"]);
  } catch (e) {
    emmetRegistered = false;   // beim nächsten Versuch erneut probieren
  }
}

/* --------------------- Tags automatisch schließen ------------------------
   Der Editorkern bringt nur die Klammer-Automatik mit — das Gegenstück zu
   `<h1>` fehlt. Also bauen wir es hier selbst:

   • Tippt man das `>` eines öffnenden Tags, entsteht `</tag>` dahinter und
     der Cursor bleibt dazwischen stehen.
   • Tippt man `</`, wird der zuletzt geöffnete Tag ergänzt.

   Leere Elemente (`<br>`, `<img>` …) und selbstschließende Tags (`<br/>`)
   bleiben unangetastet.
   ------------------------------------------------------------------------- */
const VOID_TAGS = new Set([
  "area", "base", "br", "col", "embed", "hr", "img", "input",
  "link", "meta", "param", "source", "track", "wbr", "!doctype",
]);

// Sprachen mit Tag-Syntax. TypeScript fehlt bewusst: dort wäre `Array<string>`
// nicht von einem Tag zu unterscheiden.
const TAG_LANGS = new Set(["html", "xml", "php", "vue", "handlebars", "markdown", "javascript"]);

const TAG_SCAN = /<\/?([a-zA-Z][\w:.-]*)((?:"[^"]*"|'[^']*'|[^'">])*?)(\/?)>/g;

/** Innerster noch offener Tag im Text vor dem Cursor — oder null. */
function openTagBefore(text) {
  const stack = [];
  TAG_SCAN.lastIndex = 0;
  let m;
  while ((m = TAG_SCAN.exec(text))) {
    const [full, name, , selfClose] = m;
    const lower = name.toLowerCase();
    if (full[1] === "/") {
      // Von hinten den passenden offenen Tag entfernen — so stören
      // unsauber verschachtelte Stellen die Erkennung nicht.
      for (let i = stack.length - 1; i >= 0; i--) {
        if (stack[i].toLowerCase() === lower) { stack.splice(i, 1); break; }
      }
    } else if (!selfClose && !VOID_TAGS.has(lower)) {
      stack.push(name);
    }
  }
  return stack.length ? stack[stack.length - 1] : null;
}

/** Prüft, was nach dem Tippen eines Zeichens ergänzt werden soll. */
function tagCompletion(before, after, typed) {
  if (typed === ">") {
    const m = /<([a-zA-Z][\w:.-]*)((?:"[^"]*"|'[^']*'|[^'">])*)>$/.exec(before);
    if (!m) return null;
    if (m[2].trimEnd().endsWith("/")) return null;          // <br/> schließt sich selbst
    if (VOID_TAGS.has(m[1].toLowerCase())) return null;
    const closing = `</${m[1]}>`;
    if (after.startsWith(closing)) return null;             // steht schon da
    return { insert: closing, caretOffset: 0 };
  }
  if (typed === "/") {
    if (!before.endsWith("</")) return null;
    const open = openTagBefore(before.slice(0, -2));
    if (!open) return null;
    return { insert: `${open}>`, caretOffset: open.length + 1 };
  }
  return null;
}

function enableAutoCloseTags(editor, monaco) {
  let busy = false;
  return editor.onDidChangeModelContent((event) => {
    if (busy || event.changes.length !== 1) return;
    const change = event.changes[0];
    if (change.rangeLength !== 0) return;                   // nur echtes Tippen
    const model = editor.getModel();
    const pos = editor.getPosition();
    if (!model || !pos) return;
    if (!TAG_LANGS.has(model.getLanguageId ? model.getLanguageId() : "")) return;

    const before = model.getValueInRange({
      startLineNumber: 1, startColumn: 1,
      endLineNumber: pos.lineNumber, endColumn: pos.column,
    });
    const line = model.getLineContent(pos.lineNumber);
    const completion = tagCompletion(before, line.slice(pos.column - 1), change.text);
    if (!completion) return;

    busy = true;
    try {
      const at = new monaco.Range(pos.lineNumber, pos.column, pos.lineNumber, pos.column);
      const caret = pos.column + completion.caretOffset;
      editor.executeEdits("ld-autoclose-tag", [{ range: at, text: completion.insert }],
        [new monaco.Selection(pos.lineNumber, caret, pos.lineNumber, caret)]);
    } finally {
      busy = false;
    }
  });
}

function LdCodeEditor({
  value, onChange, disabled, courseId, label, height = "280px",
  showMinimap = false, onCursor, language, path, onReady, chrome = true,
  fontSize = 13, wordWrap = "off", theme = "ld-dark",
}) {
  const [mod, setMod] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let alive = true;
    import("@monaco-editor/react")
      .then((m) => {
        try { if (!monacoConfigured && m.loader) { m.loader.config({ paths: { vs: MONACO_VS } }); monacoConfigured = true; } } catch (e) {}
        if (alive) setMod(() => m);
      })
      .catch(() => { if (alive) setFailed(true); });
    return () => { alive = false; };
  }, []);

  // CDN nicht erreichbar → klassischer Editor mit Zeilennummern
  if (failed) return <CodeEditor value={value} onChange={onChange} disabled={disabled} lang={label} />;

  if (!mod) {
    const skeleton = (
      <div className="p-3 space-y-2">
        <div className="ld-skeleton h-3 w-2/3" /><div className="ld-skeleton h-3 w-1/2" /><div className="ld-skeleton h-3 w-3/4" /><div className="ld-skeleton h-3 w-1/3" />
      </div>
    );
    return chrome
      ? editorChrome(courseId, label, <><Loader2 size={11} className="ld-spin" />Editor lädt …</>, skeleton)
      : skeleton;
  }

  const Editor = mod.default;
  const beforeMount = (monaco) => {
    monaco.editor.defineTheme("ld-dark", {
      base: "vs-dark", inherit: true, rules: [],
      colors: {
        "editor.background": "#0A0E1A",
        "editorGutter.background": "#0A0E1A",
        "editorLineNumber.foreground": "#4A5A7A",
        "editorLineNumber.activeForeground": "#8A9BC0",
        "editor.lineHighlightBackground": "#141D35",
        "editor.selectionBackground": "#2A3F6F",
        "editorCursor.foreground": "#4F8EF7",
        "editorIndentGuide.background1": "#1E2D4A",
      },
    });
  };
  const onMount = (editor, monaco) => {
    enableEmmet(monaco);
    enableAutoCloseTags(editor, monaco);       // <h1> ergänzt </h1>
    if (onCursor) {
      const report = () => {
        const p = editor.getPosition();
        if (p) onCursor({ line: p.lineNumber, column: p.column });
      };
      editor.onDidChangeCursorPosition(report);
      report();
    }
    if (onReady) onReady(editor, monaco);
  };

  const editorEl = (
    <Editor
      height={height}
      path={path}
      language={language || MONACO_LANG[courseId] || "plaintext"}
      theme={theme}
      value={value}
      beforeMount={beforeMount}
      onMount={onMount}
      onChange={(v) => onChange(v == null ? "" : v)}
      loading={<div className="p-4 text-sm text-[#8A9BC0] flex items-center gap-2"><Loader2 size={14} className="ld-spin" />Editor wird vorbereitet …</div>}
      options={{
        readOnly: disabled,
        fontSize, fontFamily: "'JetBrains Mono', monospace", fontLigatures: true,
        wordWrap,
        minimap: { enabled: showMinimap, renderCharacters: false },
        scrollBeyondLastLine: false, automaticLayout: true,
        padding: { top: 10, bottom: 10 }, tabSize: 2, lineNumbersMinChars: 3,
        renderLineHighlight: "all",
        smoothScrolling: true, cursorBlinking: "smooth", cursorSmoothCaretAnimation: "on",
        roundedSelection: true,
        scrollbar: { verticalScrollbarSize: 10, horizontalScrollbarSize: 10 },

        // Komfort wie in einer Desktop-IDE
        bracketPairColorization: { enabled: true },
        guides: { bracketPairs: true, indentation: true, highlightActiveIndentation: true },
        autoClosingBrackets: "languageDefined",
        autoClosingQuotes: "languageDefined",
        autoSurround: "languageDefined",
        autoIndent: "full",
        formatOnPaste: true,
        formatOnType: true,
        linkedEditing: true,               // öffnendes und schließendes Tag zusammen umbenennen
        matchBrackets: "always",
        occurrencesHighlight: "singleFile",
        selectionHighlight: true,
        folding: true, showFoldingControls: "mouseover",
        stickyScroll: { enabled: true },
        suggestOnTriggerCharacters: true,
        quickSuggestions: { other: true, comments: false, strings: true },
        wordBasedSuggestions: "allDocuments",
        tabCompletion: "on",
        snippetSuggestions: "top",
        parameterHints: { enabled: true },
        multiCursorModifier: "alt",
        mouseWheelZoom: true,
        dragAndDrop: true,
        links: true,
      }}
    />
  );

  // In der IDE steckt der Editor bereits in einem eigenen Rahmen — dort wäre
  // die kleine Fensterleiste doppelt gemoppelt.
  return chrome ? editorChrome(courseId, label, <>LearnDeveloping&nbsp;Editor</>, editorEl) : editorEl;
}


/* Konfetti-Feier bei abgeschlossener Lektion */
function Confetti() {
  const colors = ["#4F8EF7", "#7C3AED", "#F7C948", "#10B981", "#EF4444", "#F59E0B"];
  const pieces = Array.from({ length: 80 });
  return (
    <div className="fixed inset-0 z-[95] pointer-events-none overflow-hidden" aria-hidden>
      {pieces.map((_, i) => {
        const left = Math.random() * 100;
        const delay = Math.random() * 0.4;
        const dur = 1.6 + Math.random() * 1.4;
        const size = 6 + Math.random() * 8;
        const c = colors[i % colors.length];
        return (
          <span key={i} style={{
            position: "absolute", top: "-20px", left: left + "%", width: size, height: size * 0.5,
            background: c, borderRadius: 2, opacity: 0,
            animation: `ld-confetti ${dur}s ${delay}s ease-in forwards`,
          }} />
        );
      })}
    </div>
  );
}

/* Skeleton-Loader für laufende Prüfungen */
function SkeletonFeedback() {
  return (
    <Card className="p-5 mt-5">
      <div className="flex items-center gap-2 mb-3 pb-3 border-b border-[#1E2D4A]">
        <Bot size={18} className="text-[#7C3AED]" />
        <span className="font-display font-bold">Bewertung</span>
        <span className="ml-auto flex items-center gap-1.5 text-xs text-[#8A9BC0]"><Loader2 size={13} className="ld-spin" />analysiert deinen Code …</span>
      </div>
      <div className="space-y-2.5">
        <div className="ld-skeleton h-4 w-1/3" />
        <div className="ld-skeleton h-3 w-full" />
        <div className="ld-skeleton h-3 w-11/12" />
        <div className="ld-skeleton h-3 w-2/3" />
      </div>
    </Card>
  );
}

/* KI-Einstellungen: Anbieter wählen, mehrere Keys pflegen, Pool-Status sehen */
function AiSettingsModal({ ctx }) {
  const { apiKeys, setApiKeys, aiProvider, setAiProvider, ollamaModel, setOllamaModel, closeAiSettings, pushToast } = ctx;
  const [provider, setProvider] = useState(aiProvider || "gemini");
  const [keys, setKeys] = useState(() => (apiKeys.length ? [...apiKeys] : [""]));
  const [model, setModel] = useState(ollamaModel || OLLAMA_DEFAULT_MODEL);
  const [show, setShow] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState(null);
  const cfg = AI_PROVIDERS[provider];
  const status = keyPoolStatus(apiKeys);

  const setKeyAt = (i, v) => setKeys((ks) => ks.map((k, idx) => (idx === i ? v : k)));
  const addKey = () => setKeys((ks) => [...ks, ""]);
  const removeKey = (i) => setKeys((ks) => (ks.length > 1 ? ks.filter((_, idx) => idx !== i) : [""]));

  const save = () => {
    const clean = keys.map((k) => k.trim()).filter(Boolean);
    setAiProvider(provider);
    setOllamaModel(model.trim() || OLLAMA_DEFAULT_MODEL);
    setApiKeys(clean);
    const usable = provider === "ollama" || clean.length > 0;
    pushToast(usable ? "success" : "info",
      provider === "ollama" ? `Eigener Server aktiv (${model}).`
        : clean.length ? `${clean.length} Key${clean.length === 1 ? "" : "s"} gespeichert — Bewertung über ${cfg.label}.`
        : "Kein Key — es läuft die lokale Analyse.");
    closeAiSettings();
  };

  const testConnection = async () => {
    setTesting(true); setTestResult(null);
    const clean = keys.map((k) => k.trim()).filter(Boolean);
    try {
      const txt = await callAI(provider, provider === "ollama" ? [clean[0] || OLLAMA_DEFAULT_URL] : clean,
        "Antworte mit exakt einem Wort.", "Sage: OK", 20, model);
      setTestResult({ ok: true, msg: `Verbindung steht. Antwort: „${String(txt).trim().slice(0, 40)}"` });
    } catch (e) {
      setTestResult({ ok: false, msg: `Fehlgeschlagen: ${e.message}${provider === "ollama" ? " — läuft Ollama und ist OLLAMA_ORIGINS gesetzt?" : ""}` });
    }
    setTesting(false);
  };

  return (
    <div className="fixed inset-0 z-[110] flex items-center justify-center p-4 overflow-y-auto" onClick={closeAiSettings}>
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
      <Card className="relative z-10 p-7 max-w-lg w-full my-8" onClick={(e) => e.stopPropagation()}>
        <button onClick={closeAiSettings} aria-label="Schließen" className="absolute top-4 right-4 text-[#8A9BC0] hover:text-[#E8EDF5]"><X size={20} /></button>
        <div className="flex items-center gap-3 mb-3">
          <div className="w-11 h-11 rounded-lg flex items-center justify-center shrink-0 bg-[#7C3AED]/15"><Bot size={22} className="text-[#7C3AED]" /></div>
          <h3 className="font-display text-xl font-bold">KI-Einstellungen</h3>
        </div>
        <p className="text-sm text-[#8A9BC0] mb-4 leading-relaxed">
          <strong className="text-[#E8EDF5]">Optional.</strong> Ohne Anbieter läuft die eingebaute lokale Analyse — sie prüft Struktur, Konzepte und Begründungen direkt im Browser.
        </p>

        <div className="grid grid-cols-3 gap-2 mb-4">
          {Object.entries(AI_PROVIDERS).map(([id, p]) => (
            <button key={id} onClick={() => {
                setProvider(id); setTestResult(null);
                // Beim Wechsel passende Feldwerte setzen: Ollama erwartet eine URL,
                // die Cloud-Anbieter einen Key — sonst landet der Key im Adressfeld.
                setKeys((ks) => {
                  const first = (ks[0] || "").trim();
                  if (id === "ollama") return [/^https?:\/\//i.test(first) ? first : OLLAMA_DEFAULT_URL];
                  return /^https?:\/\//i.test(first) ? [""] : ks;
                });
              }}
              className={`text-left p-2.5 rounded-lg border transition-all ${provider === id ? "border-[#4F8EF7] bg-[#4F8EF7]/10" : "border-[#1E2D4A] hover:border-[#2A3F6F]"}`}>
              <div className="font-medium text-xs text-[#E8EDF5] mb-1 leading-tight">{p.label}</div>
              <div className={`text-[9px] px-1.5 py-0.5 rounded-full inline-block ${id === "gemini" ? "bg-[#10B981]/15 text-[#10B981]" : id === "ollama" ? "bg-[#4F8EF7]/15 text-[#4F8EF7]" : "bg-[#F59E0B]/15 text-[#F59E0B]"}`}>{p.badge}</div>
            </button>
          ))}
        </div>

        <p className="text-xs text-[#8A9BC0] mb-4 leading-relaxed p-2.5 rounded-lg bg-[#0A0E1A] border border-[#1E2D4A]">{cfg.note}</p>

        {provider === "ollama" ? (
          <>
            <label className="block text-sm text-[#8A9BC0] mb-1.5">Server-Adresse</label>
            <input value={keys[0] || ""} onChange={(e) => setKeyAt(0, e.target.value)} placeholder={OLLAMA_DEFAULT_URL}
              className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 mb-3 font-code text-sm text-[#E8EDF5]" />
            <label className="block text-sm text-[#8A9BC0] mb-1.5">Modell</label>
            <input value={model} onChange={(e) => setModel(e.target.value)} placeholder={OLLAMA_DEFAULT_MODEL}
              className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 mb-2 font-code text-sm text-[#E8EDF5]" />
            <p className="text-xs text-[#4A5A7A] mb-4 leading-relaxed">
              Für CPU-Server empfehlen sich kleine Modelle: <code className="font-code text-[#4F8EF7]">qwen2.5-coder:3b</code> oder <code className="font-code text-[#4F8EF7]">llama3.2:3b</code>.
              Damit der Browser zugreifen darf, Ollama mit <code className="font-code text-[#4F8EF7]">OLLAMA_ORIGINS=*</code> starten.
            </p>
          </>
        ) : (
          <>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-sm text-[#8A9BC0]">{cfg.label} API-Keys</label>
              <button onClick={() => setShow((s) => !s)} className="text-xs text-[#8A9BC0] hover:text-[#E8EDF5]">{show ? "Verbergen" : "Anzeigen"}</button>
            </div>
            <div className="space-y-2 mb-2">
              {keys.map((k, i) => (
                <div key={i} className="relative flex gap-2">
                  <div className="relative flex-1">
                    <KeyRound size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#4A5A7A]" />
                    <input type={show ? "text" : "password"} value={k} onChange={(e) => setKeyAt(i, e.target.value)} placeholder={`${cfg.keyPlaceholder} (Key ${i + 1})`}
                      className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 pl-8 font-code text-xs text-[#E8EDF5] placeholder:text-[#4A5A7A]" />
                  </div>
                  {keys.length > 1 && <button onClick={() => removeKey(i)} aria-label="Key entfernen" className="px-2 text-[#8A9BC0] hover:text-[#EF4444]"><Trash2 size={15} /></button>}
                </div>
              ))}
            </div>
            <button onClick={addKey} className="text-xs text-[#4F8EF7] hover:underline flex items-center gap-1.5 mb-3"><Plus size={12} />Weiteren Key hinzufügen</button>
            <p className="text-xs text-[#4A5A7A] mb-3 leading-relaxed">
              Mehrere Keys (auch aus verschiedenen Konten) werden abwechselnd genutzt. Läuft einer ins Limit, übernimmt automatisch der nächste — so vervielfacht sich dein Gratis-Kontingent. Keys erstellen:{" "}
              <a href={cfg.keyUrl} target="_blank" rel="noopener noreferrer" className="text-[#4F8EF7] underline">{cfg.keyUrlLabel}</a>.
            </p>
            {status.length > 0 && (
              <div className="mb-3 p-2.5 rounded-lg bg-[#0A0E1A] border border-[#1E2D4A]">
                <p className="text-[11px] text-[#8A9BC0] mb-1.5">Key-Pool ({status.length} aktiv):</p>
                <div className="flex flex-wrap gap-1.5">
                  {status.map((s, i) => (
                    <span key={i} className={`text-[10px] font-code px-2 py-0.5 rounded-full ${s.cooling ? "bg-[#F59E0B]/15 text-[#F59E0B]" : "bg-[#10B981]/15 text-[#10B981]"}`}>
                      {s.masked}{s.cooling ? ` · pausiert ${s.secondsLeft}s` : " · bereit"}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </>
        )}

        {testResult && (
          <p className={`text-xs mb-3 p-2.5 rounded-lg ${testResult.ok ? "bg-[#10B981]/10 text-[#10B981]" : "bg-[#EF4444]/10 text-[#EF4444]"}`}>{testResult.msg}</p>
        )}

        <div className="flex flex-wrap gap-2">
          <Btn variant="secondary" icon={testing ? undefined : Zap} onClick={testConnection} disabled={testing}>
            {testing ? <><Loader2 size={14} className="ld-spin" />Teste …</> : "Verbindung testen"}
          </Btn>
          {apiKeys.length > 0 && <Btn variant="danger" icon={Trash2} onClick={() => { setKeys([""]); setApiKeys([]); pushToast("info", "Keys entfernt."); closeAiSettings(); }}>Alle löschen</Btn>}
          <Btn className="flex-1" icon={Check} onClick={save}>Speichern</Btn>
        </div>
      </Card>
    </div>
  );
}

/* E-Mail-Verifizierung (simuliert — kein Mailserver vorhanden) */
function EmailVerifyModal({ ctx }) {
  const { me, verifyEmail, closeEmailVerify, serverVerificationCode, pushToast } = ctx;
  const [code, setCode] = useState("");
  const [resent, setResent] = useState(null);
  // Mit Server kommt der Code per Mail; nur ohne Mailversand wird er angezeigt.
  const shownCode = ctx.backend ? (resent || serverVerificationCode) : me?.verificationCode;

  const resend = async () => {
    try {
      const res = await api.post("/api/auth/resend-verification");
      if (res.devVerificationCode) setResent(res.devVerificationCode);
      pushToast("success", res.devVerificationCode ? "Neuer Code erzeugt." : "Neuer Code wurde versendet.");
    } catch (e) { pushToast("error", e.message); }
  };

  if (!me) return null;
  return (
    <div className="fixed inset-0 z-[110] flex items-center justify-center p-4" onClick={closeEmailVerify}>
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
      <Card className="relative z-10 p-7 max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <button onClick={closeEmailVerify} aria-label="Schließen" className="absolute top-4 right-4 text-[#8A9BC0] hover:text-[#E8EDF5]"><X size={20} /></button>
        <div className="flex items-center gap-3 mb-3">
          <div className="w-11 h-11 rounded-lg flex items-center justify-center shrink-0 bg-[#4F8EF7]/15"><Mail size={22} className="text-[#4F8EF7]" /></div>
          <h3 className="font-display text-xl font-bold">E-Mail bestätigen</h3>
        </div>
        {shownCode ? (
          <>
            <p className="text-sm text-[#8A9BC0] mb-4 leading-relaxed">
              {ctx.backend
                ? <>Auf diesem Server ist kein Mailversand eingerichtet — sonst ginge der Code an <strong className="text-[#E8EDF5]">{me.email}</strong>. Zum Testen steht er hier:</>
                : <>Ohne Server gibt es keinen Mailversand — in einer produktiven Umgebung würde dieser Code an <strong className="text-[#E8EDF5]">{me.email}</strong> gesendet. Zum Testen steht er hier:</>}
            </p>
            <div className="p-4 rounded-xl bg-[#0A0E1A] border border-[#2A3F6F] mb-4 text-center">
              <span className="font-display font-black text-3xl ld-gradient-text font-code">{shownCode}</span>
            </div>
          </>
        ) : (
          <p className="text-sm text-[#8A9BC0] mb-4 leading-relaxed">
            Wir haben dir einen sechsstelligen Code an <strong className="text-[#E8EDF5]">{me.email}</strong> geschickt. Gib ihn hier ein.
          </p>
        )}
        <label className="block text-sm text-[#8A9BC0] mb-1.5">Code eingeben</label>
        <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="123456" maxLength={6}
          onKeyDown={(e) => { if (e.key === "Enter") verifyEmail(code); }}
          className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 mb-4 font-code text-center text-lg tracking-widest text-[#E8EDF5]" />
        <Btn className="w-full" icon={Check} onClick={() => verifyEmail(code)}>Bestätigen</Btn>
        {ctx.backend && (
          <button onClick={resend} className="w-full mt-3 text-xs text-[#8A9BC0] hover:text-[#E8EDF5]">
            Code erneut senden
          </button>
        )}
      </Card>
    </div>
  );
}

/* 2FA-Einrichtung.
   Mit Server: echtes TOTP-Geheimnis für Authenticator-Apps, das durch Eingabe
   eines gültigen Codes bestätigt werden muss.
   Ohne Server: statischer Code, da kein Zeitgeber-Backend vorhanden ist. */
function TwoFactorSetupModal({ ctx }) {
  const { twoFactorSetupCode, closeTwoFactorSetup, confirm2FA } = ctx;
  const [code, setCode] = useState("");
  const setup = twoFactorSetupCode || {};
  const needsConfirm = !!setup.needsConfirm;

  return (
    <div className="fixed inset-0 z-[110] flex items-center justify-center p-4 overflow-y-auto" onClick={closeTwoFactorSetup}>
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
      <Card className="relative z-10 p-7 max-w-md w-full my-8" onClick={(e) => e.stopPropagation()}>
        <button onClick={closeTwoFactorSetup} aria-label="Schließen" className="absolute top-4 right-4 text-[#8A9BC0] hover:text-[#E8EDF5]"><X size={20} /></button>
        <div className="flex items-center gap-3 mb-3">
          <div className="w-11 h-11 rounded-lg flex items-center justify-center shrink-0 bg-[#10B981]/15"><ShieldCheck size={22} className="text-[#10B981]" /></div>
          <h3 className="font-display text-xl font-bold">{needsConfirm ? "2FA einrichten" : "2FA aktiviert"}</h3>
        </div>

        {needsConfirm ? (
          <>
            <p className="text-sm text-[#8A9BC0] mb-4 leading-relaxed">
              Trage dieses Geheimnis in deiner Authenticator-App ein (z.B. Aegis, 1Password, Google Authenticator) und bestätige dann mit dem angezeigten Code.
            </p>
            <div className="p-4 rounded-xl bg-[#0A0E1A] border border-[#2A3F6F] mb-2 text-center">
              <span className="font-code text-lg text-[#4F8EF7] break-all">{setup.secret}</span>
            </div>
            <p className="text-xs text-[#4A5A7A] mb-4 break-all">
              Oder per Link: <span className="font-code text-[#8A9BC0]">{setup.uri}</span>
            </p>
            <label className="block text-sm text-[#8A9BC0] mb-1.5">Code aus der App</label>
            <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="123456" maxLength={6}
              onKeyDown={(e) => { if (e.key === "Enter") confirm2FA(code); }}
              className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 mb-4 font-code text-center text-lg tracking-widest text-[#E8EDF5]" />
            <Btn className="w-full" icon={Check} onClick={() => confirm2FA(code)}>2FA aktivieren</Btn>
          </>
        ) : (
          <>
            <p className="text-sm text-[#8A9BC0] mb-4 leading-relaxed">
              Merke dir diesen Code — du brauchst ihn ab sofort bei jedem Login zusätzlich zum Passwort. Ohne Server kann kein zeitbasierter Authenticator angebunden werden, daher bleibt der Code fest.
            </p>
            <div className="p-4 rounded-xl bg-[#0A0E1A] border border-[#2A3F6F] mb-2 text-center">
              <span className="font-display font-black text-3xl ld-gradient-text font-code">{setup.code}</span>
            </div>
            <p className="text-xs text-[#4A5A7A] mb-5">Du kannst 2FA jederzeit im Profil wieder deaktivieren.</p>
            <Btn className="w-full" icon={Check} onClick={closeTwoFactorSetup}>Verstanden</Btn>
          </>
        )}
      </Card>
    </div>
  );
}

/* ============================ Main App ============================= */
/* ------------------------- Passwort-Anforderungen ------------------------
   Dieselben Regeln wie im Backend (server/src/password.js). Hier dienen sie
   als Hilfe beim Tippen — verbindlich geprüft wird immer auf dem Server.
   ------------------------------------------------------------------------- */
const PASSWORD_MIN_LENGTH = 10;

const COMMON_PASSWORDS = new Set([
  "passwort", "password", "passwort1", "password1", "passwort123", "password123",
  "12345678", "123456789", "1234567890", "qwertzuiop", "qwertyuiop", "asdfghjkl",
  "hallo123", "willkommen", "willkommen1", "administrator", "superadmin",
  "letmein123", "iloveyou1", "sonnenschein", "fussball1", "geheim123",
  "passw0rt", "p@ssword", "p@ssw0rt", "abcd1234", "test1234", "start1234",
]);

/**
 * Liefert die einzelnen Anforderungen mit ihrem Erfüllungsstand — daraus
 * wird die Checkliste unter dem Eingabefeld gebaut.
 */
function passwordRules(password, { name = "", email = "" } = {}) {
  const value = String(password || "");
  const lower = value.toLowerCase();
  const classes = [
    /[a-zäöüß]/.test(value), /[A-ZÄÖÜ]/.test(value),
    /[0-9]/.test(value), /[^A-Za-z0-9ÄÖÜäöüß]/.test(value),
  ].filter(Boolean).length;

  const ownName = String(name).trim().toLowerCase();
  const local = String(email).split("@")[0].trim().toLowerCase();

  return [
    { label: `Mindestens ${PASSWORD_MIN_LENGTH} Zeichen`, ok: value.length >= PASSWORD_MIN_LENGTH },
    { label: "Drei von vier: Klein-, Großbuchstaben, Ziffern, Sonderzeichen", ok: classes >= 3 },
    { label: "Kein bekanntes Standardpasswort und kein Tastaturmuster",
      ok: value.length > 0 && !COMMON_PASSWORDS.has(lower) && !/^(.)\1+$/.test(value)
        && !/(0123456789|123456789|abcdefgh|qwertz|qwerty|asdfgh)/i.test(lower) },
    { label: "Enthält weder deinen Namen noch deine E-Mail-Adresse",
      ok: value.length > 0
        && !(ownName.length >= 3 && lower.includes(ownName))
        && !(local.length >= 3 && lower.includes(local)) },
  ];
}

function passwordOk(password, context) {
  return passwordRules(password, context).every((r) => r.ok);
}

/** Checkliste unter einem Passwortfeld. Erscheint erst beim Tippen. */
function PasswordHints({ password, name, email }) {
  if (!password) return null;
  const rules = passwordRules(password, { name, email });
  const met = rules.filter((r) => r.ok).length;
  const tone = met === rules.length ? "#10B981" : met >= 3 ? "#F59E0B" : "#EF4444";
  return (
    <div className="mt-2">
      <div className="flex gap-1 mb-2">
        {rules.map((r, i) => (
          <span key={i} className="h-1 flex-1 rounded-full transition-colors"
            style={{ background: i < met ? tone : "#1E2D4A" }} />
        ))}
      </div>
      <ul className="space-y-0.5">
        {rules.map((r, i) => (
          <li key={i} className={`flex items-start gap-1.5 text-[11px] ${r.ok ? "text-[#10B981]" : "text-[#8A9BC0]"}`}>
            {r.ok ? <Check size={10} className="mt-0.5 shrink-0" /> : <X size={10} className="mt-0.5 shrink-0 text-[#4A5A7A]" />}
            <span>{r.label}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/* -------------- Passwörter im lokalen Modus (ohne Server) -----------------
   Ohne Backend liegen die Konten im Speicher des Browsers. Was dort steht,
   sieht jeder, der F12 drückt — deshalb steht dort **kein Passwort**, sondern
   nur ein Hash mit zufälligem Salz (PBKDF2-SHA256).

   Das ersetzt keinen Server: Wer den Browser bedient, ist ohnehin angemeldet.
   Aber es verhindert, dass ein Passwort im Klartext herumliegt — und Menschen
   verwenden Passwörter nun einmal mehrfach.
   ------------------------------------------------------------------------- */
const PBKDF2_ITERATIONS = 210_000;

const toHex = (buffer) => [...new Uint8Array(buffer)].map((b) => b.toString(16).padStart(2, "0")).join("");

function subtleCrypto() {
  try { return window.crypto?.subtle || null; } catch (e) { return null; }
}

async function hashLocalPassword(password, saltHex) {
  const subtle = subtleCrypto();
  if (!subtle) return null;                       // kein sicherer Kontext -> siehe unten
  const salt = saltHex
    ? Uint8Array.from(saltHex.match(/.{2}/g).map((h) => parseInt(h, 16)))
    : window.crypto.getRandomValues(new Uint8Array(16));
  const key = await subtle.importKey("raw", new TextEncoder().encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await subtle.deriveBits(
    { name: "PBKDF2", salt, iterations: PBKDF2_ITERATIONS, hash: "SHA-256" }, key, 256);
  return { salt: toHex(salt), hash: toHex(bits) };
}

/** Prüft ein Passwort gegen ein lokal gespeichertes Konto. */
async function verifyLocalPassword(password, user) {
  if (user?.passwordHash && user?.passwordSalt) {
    const derived = await hashLocalPassword(password, user.passwordSalt);
    return !!derived && derived.hash === user.passwordHash;
  }
  // Altbestand aus früheren Versionen: einmalig im Klartext vergleichen.
  // Beim nächsten Speichern wird daraus ein Hash (siehe upgradeLocalPassword).
  return !!user?.password && user.password === password;
}

/** Erzeugt die Felder, die für ein Konto gespeichert werden. */
async function localPasswordFields(password) {
  const derived = await hashLocalPassword(password);
  // Ohne WebCrypto (etwa über file:// geöffnet) bleibt nur der alte Weg —
  // dann sagen wir es wenigstens offen im Log.
  if (!derived) {
    console.warn("LearnDeveloping: Kein sicherer Kontext — Passwort kann lokal nicht gehasht werden. Bitte über http://localhost öffnen.");
    return { password };
  }
  return { passwordSalt: derived.salt, passwordHash: derived.hash };
}

function genSchoolCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "";
  for (let i = 0; i < 4; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return "LRND-" + s;
}
const uid = () => "u_" + Math.random().toString(36).slice(2, 9);

/* ============================ API-Client =================================
   Die App läuft in zwei Betriebsarten:

   1. MIT Server — erkannt über /api/health. Konten, Fortschritt, Projekte und
      KI-Aufrufe laufen dann über das Backend. Passwörter werden dort gehasht,
      API-Keys bleiben auf dem Server, das Speicherkontingent wird durchgesetzt.

   2. OHNE Server — alles bleibt lokal im Browser (localStorage), so wie bisher.
      Praktisch zum Ausprobieren, ohne etwas installieren zu müssen.

   Die Erkennung passiert einmalig beim Start; schlägt sie fehl, wird
   automatisch der lokale Modus genutzt.
   ========================================================================= */
class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.status = status;
    this.data = data || {};
  }
}

const api = {
  available: false,
  aiAvailable: false,

  async request(method, path, body) {
    const res = await fetch(path, {
      method,
      credentials: "same-origin",              // Sitzungs-Cookie mitsenden
      headers: body === undefined ? {} : { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }
    if (!res.ok) throw new ApiError(data?.error || `Fehler ${res.status}`, res.status, data);
    return data;
  },

  get(path) { return this.request("GET", path); },
  post(path, body) { return this.request("POST", path, body); },
  patch(path, body) { return this.request("PATCH", path, body); },
  put(path, body) { return this.request("PUT", path, body); },
  del(path) { return this.request("DELETE", path); },

  /** Einmalige Erkennung beim Start. */
  async probe() {
    try {
      const health = await Promise.race([
        this.get("/api/health"),
        new Promise((_, rej) => setTimeout(() => rej(new Error("timeout")), 3000)),
      ]);
      this.available = !!health?.ok;
    } catch (e) {
      this.available = false;
    }
    if (this.available) {
      try {
        const status = await this.get("/api/ai/status");
        this.aiAvailable = !!status?.available;
      } catch (e) { this.aiAvailable = false; }
    }
    return this.available;
  },
};

/**
 * Übersetzt einen Nutzer aus dem Backend in die Form, die die Oberfläche
 * erwartet (dort heißen einige Felder anders und werden direkt gerendert).
 */
function fromApiUser(u) {
  if (!u) return null;
  return {
    ...u,
    completedLessons: u.completedLessons || [],
    badges: u.badges || [],
    playground: [],                       // Projekte werden separat geladen
    joinedAt: u.joinedAt ? new Date(u.joinedAt).toLocaleDateString("de-DE") : "—",
    lastLogin: u.lastLogin ? new Date(u.lastLogin).toLocaleDateString("de-DE") : "Jetzt",
  };
}

/* --------------------- Lokale Speicherung (Browser) ---------------------
   Ohne Backend überlebt der Fortschritt einen Reload im localStorage des
   Browsers und verlässt das Gerät nie. Gast-Sitzungen (isGuest) werden
   bewusst NICHT gespeichert.
   ------------------------------------------------------------------------- */
const STORAGE_KEY = "learndeveloping_v1";
const API_KEY_STORAGE = "learndeveloping_ai_key";          // alt (Einzel-Key)
const API_KEYS_STORAGE = "learndeveloping_ai_keys";        // neu (Key-Pool)
const AI_PROVIDER_STORAGE = "learndeveloping_ai_provider";
const OLLAMA_MODEL_STORAGE = "learndeveloping_ollama_model";

function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed || !Array.isArray(parsed.users)) return null;
    return parsed;
  } catch (e) { return null; }
}

function savePersisted(users, currentUser, reports) {
  try {
    // Was im localStorage steht, ist mit F12 einsehbar. Deshalb landen dort
    // weder Klartext-Passwörter noch Einmal-Codes.
    const persistUsers = users.filter((u) => !u.isGuest).map((u) => {
      const { password, verificationCode, twoFactorCode, ...safe } = u;
      return safe.passwordHash ? safe : { ...safe, password };   // Altbestand nicht wegwerfen
    });
    const stillLoggedIn = users.find((u) => u.id === currentUser);
    const persistCurrent = stillLoggedIn && !stillLoggedIn.isGuest ? currentUser : null;
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ users: persistUsers, currentUser: persistCurrent, reports: reports || [] }));
  } catch (e) {}
}

// Mehrere Keys werden als JSON-Array abgelegt; ein alter Einzel-Key wird
// beim ersten Laden automatisch übernommen.
function loadApiKeys() {
  try {
    const raw = localStorage.getItem(API_KEYS_STORAGE);
    if (raw) { const arr = JSON.parse(raw); if (Array.isArray(arr)) return arr.filter(Boolean); }
    const legacy = localStorage.getItem(API_KEY_STORAGE);
    return legacy ? [legacy] : [];
  } catch (e) { return []; }
}
function loadAiProvider() {
  try { return localStorage.getItem(AI_PROVIDER_STORAGE) || "gemini"; } catch (e) { return "gemini"; }
}
function loadOllamaModel() {
  try { return localStorage.getItem(OLLAMA_MODEL_STORAGE) || OLLAMA_DEFAULT_MODEL; } catch (e) { return OLLAMA_DEFAULT_MODEL; }
}

function roleHome(role) { return role === "teacher" ? "teacher" : role === "admin" ? "admin" : "dashboard"; }

/**
 * Darf dieses Konto die Verwaltung sehen? Neben der Rolle „admin“ gilt das für
 * das Konto, das diese Installation angelegt hat (ohne Server gibt es sonst
 * überhaupt keinen Verwaltungszugang, weil es keine vorgefertigten Konten
 * mehr gibt).
 */
function canAdmin(user) {
  return !!user && !user.isGuest && (user.role === "admin" || user.isOwner === true);
}

/* ----------------------------- Tages-Streak ------------------------------
   Ohne Server wird die Serie hier gepflegt (mit Server übernimmt das die
   Datenbank). Gezählt werden Kalendertage, nicht 24-Stunden-Abstände — wer
   abends und am nächsten Morgen lernt, hat zwei Tage.
   ------------------------------------------------------------------------- */
function todayKey(date = new Date()) {
  // Lokales Datum, nicht UTC — sonst springt die Serie je nach Zeitzone falsch.
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function daysBetween(fromKey, toKey) {
  const a = new Date(fromKey + "T00:00:00");
  const b = new Date(toKey + "T00:00:00");
  return Math.round((b - a) / 86400_000);
}

/**
 * Schreibt die Serie fort. Wurde genau ein Tag verpasst und ist ein
 * Streak-Schutz vorhanden, wird dieser eingelöst und die Serie läuft weiter.
 */
function advanceStreak(user) {
  const today = todayKey();
  const last = user.lastActive || null;
  const freezes = user.streakFreezes || 0;

  if (last === today) {
    return { streak: user.streak || 1, lastActive: today, grew: false, freezes, usedFreeze: false };
  }
  if (!last) {
    return { streak: 1, lastActive: today, grew: true, freezes, usedFreeze: false };
  }

  const gap = daysBetween(last, today);
  if (gap === 1) {
    return { streak: (user.streak || 0) + 1, lastActive: today, grew: true, freezes, usedFreeze: false };
  }
  // Genau ein Tag ausgelassen und Schutz vorhanden -> Serie bleibt bestehen
  if (gap === 2 && freezes > 0) {
    return { streak: (user.streak || 0) + 1, lastActive: today, grew: true, freezes: freezes - 1, usedFreeze: true };
  }
  return { streak: 1, lastActive: today, grew: true, freezes, usedFreeze: false, broken: (user.streak || 0) > 1 };
}

/* ------------------------------- Ligen -----------------------------------
   Jede Woche zählt neu: Gesammelte XP der laufenden Woche bestimmen die
   Platzierung. Wer oben landet, steigt auf, wer unten bleibt, ab.
   ------------------------------------------------------------------------- */
const LEAGUES = [
  { id: "bronze",  name: "Bronze", color: "#B08D57" },
  { id: "silber",  name: "Silber", color: "#A8B3C4" },
  { id: "gold",    name: "Gold", color: "#F7C948" },
  { id: "platin",  name: "Platin", color: "#4F8EF7" },
  { id: "diamant", name: "Diamant", color: "#7C3AED" },
  { id: "meister", name: "Meister", color: "#EF4444" },
];

const PROMOTE_TOP = 3;      // beste drei steigen auf
const RELEGATE_BOTTOM = 3;  // schlechteste drei steigen ab

function leagueById(id) { return LEAGUES.find((l) => l.id === id) || LEAGUES[0]; }

/** Kalenderwoche als Schlüssel, z.B. "2026-KW24" — Wochenstart ist Montag. */
function weekKey(date = new Date()) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));   // auf Montag zurück
  return `${d.getFullYear()}-KW${String(Math.ceil(((d - new Date(d.getFullYear(), 0, 1)) / 86400_000 + 1) / 7)).padStart(2, "0")}`;
}

/** Wie viele Tage bleiben bis zum Wochenwechsel (Montag 00:00). */
function daysLeftInWeek(date = new Date()) {
  const dayOfWeek = (date.getDay() + 6) % 7;         // Montag = 0
  return 7 - dayOfWeek;
}

/**
 * Setzt die Wochenwertung zurück, wenn eine neue Woche begonnen hat, und
 * wendet dabei Auf- bzw. Abstieg an.
 */
function rolloverLeague(user, rank, fieldSize) {
  const current = weekKey();
  if (user.weekKey === current) return user;

  let leagueId = user.league || "bronze";
  const idx = LEAGUES.findIndex((l) => l.id === leagueId);
  // Nur werten, wenn in der Vorwoche überhaupt gelernt wurde
  if (user.weekKey && (user.weeklyXp || 0) > 0 && rank != null) {
    if (rank <= PROMOTE_TOP && idx < LEAGUES.length - 1) leagueId = LEAGUES[idx + 1].id;
    else if (fieldSize > RELEGATE_BOTTOM && rank > fieldSize - RELEGATE_BOTTOM && idx > 0) leagueId = LEAGUES[idx - 1].id;
  }
  return { ...user, league: leagueId, weeklyXp: 0, weekKey: current };
}

// Kosten für einen Streak-Schutz und wie viele man höchstens halten kann
const FREEZE_COST_XP = 200;
const FREEZE_MAX = 3;

/**
 * Die letzten sieben Tage mit echten Wochentagen. Aktiv sind die Tage, die
 * von der laufenden Serie abgedeckt werden — rückwärts ab dem letzten
 * aktiven Tag.
 */
function streakWeek(user) {
  const streak = Math.max(0, user?.streak || 0);
  const last = user?.lastActive;
  const yesterday = todayKey(new Date(Date.now() - 86400_000));

  let endOffset = null;                       // wie viele Tage der letzte aktive Tag her ist
  if (last === todayKey()) endOffset = 0;
  else if (last === yesterday) endOffset = 1;
  else if (!last && streak > 0) endOffset = 0; // Altbestand ohne Datum

  const out = [];
  for (let ago = 6; ago >= 0; ago--) {
    const date = new Date(Date.now() - ago * 86400_000);
    out.push({
      label: date.toLocaleDateString("de-DE", { weekday: "short" }).slice(0, 2),
      active: endOffset !== null && ago >= endOffset && ago < endOffset + streak,
      isToday: ago === 0,
    });
  }
  return out;
}

export default function App() {
  const [view, setView] = useState(() => {
    // Kommt jemand über den Link aus der Passwort-Mail, gleich dorthin springen.
    try {
      if (new URLSearchParams(window.location.search).get("reset")) return "reset-password";
    } catch (e) {}
    const persisted = loadPersisted();
    if (!persisted?.currentUser) return "landing";
    const u = (persisted.users || []).find((x) => x.id === persisted.currentUser);
    return u ? roleHome(u.role) : "landing";
  });
  const [users, setUsers] = useState(() => loadPersisted()?.users || []);
  const [currentUser, setCurrentUser] = useState(() => loadPersisted()?.currentUser || null);
  const [reports, setReports] = useState(() => loadPersisted()?.reports || []);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [selectedLesson, setSelectedLesson] = useState(null);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [toasts, setToasts] = useState([]);
  const [xpPopup, setXpPopup] = useState(null);
  const [confetti, setConfetti] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [apiKeys, setApiKeysState] = useState(loadApiKeys);
  const [aiProvider, setAiProviderState] = useState(loadAiProvider);
  const [ollamaModel, setOllamaModelState] = useState(loadOllamaModel);
  const [aiSettingsOpen, setAiSettingsOpen] = useState(false);
  const [soundOn, setSoundOn] = useState(() => audio.enabled);
  const [remoteLessons, setRemoteLessons] = useState({ mine: [], fromTeacher: [] });
  const [emailVerifyOpen, setEmailVerifyOpen] = useState(false);
  const [pending2FA, setPending2FA] = useState(null);
  const [twoFactorSetupCode, setTwoFactorSetupCode] = useState(null);
  const [playgroundOpenId, setPlaygroundOpenId] = useState(null);
  const [placementTarget, setPlacementTarget] = useState(null);
  // Betriebsart: null = wird noch erkannt, true = Server, false = lokal
  const [backend, setBackend] = useState(null);
  const [booting, setBooting] = useState(true);
  // Vom Server geliefert, wenn kein Mailversand konfiguriert ist
  const [serverVerificationCode, setServerVerificationCode] = useState(null);

  const me = currentUser ? users.find((u) => u.id === currentUser) : null;

  // Beim Start prüfen, ob ein Server erreichbar ist, und ggf. die
  // bestehende Sitzung wiederherstellen.
  useEffect(() => {
    let alive = true;
    // Wer über den Link aus der Passwort-Mail kommt, soll dort bleiben —
    // die Sitzungswiederherstellung darf diese Ansicht nicht überschreiben.
    let inResetFlow = false;
    try { inResetFlow = !!new URLSearchParams(window.location.search).get("reset"); } catch (e) {}

    (async () => {
      const hasServer = await api.probe();
      if (!alive) return;
      setBackend(hasServer);
      if (hasServer) {
        try {
          const { user } = await api.get("/api/auth/me");
          if (!alive) return;
          const mapped = fromApiUser(user);
          setUsers([mapped]);
          setCurrentUser(mapped.id);
          if (!inResetFlow) setView(roleHome(mapped.role));
        } catch (e) {
          // Nicht angemeldet — Startseite bleibt stehen
          if (alive) {
            setUsers([]); setCurrentUser(null);
            if (!inResetFlow) setView("landing");
          }
        }
      }
      if (alive) setBooting(false);
    })();
    return () => { alive = false; };
  }, []);

  // Ohne Server: Fortschritt lokal sichern (Gäste ausgenommen)
  useEffect(() => {
    if (backend) return;
    savePersisted(users, currentUser, reports);
  }, [users, currentUser, reports, backend]);

  /** Lädt den aktuellen Nutzer neu — nach serverseitigen Änderungen. */
  const refreshMe = useCallback(async () => {
    if (!api.available) return null;
    try {
      const { user } = await api.get("/api/auth/me");
      const mapped = fromApiUser(user);
      setUsers((us) => {
        const rest = us.filter((u) => u.id !== mapped.id);
        return [...rest, mapped];
      });
      return mapped;
    } catch (e) { return null; }
  }, []);

  const setApiKeys = useCallback((keys) => {
    const clean = (keys || []).map((k) => String(k).trim()).filter(Boolean);
    setApiKeysState(clean);
    try {
      localStorage.setItem(API_KEYS_STORAGE, JSON.stringify(clean));
      localStorage.removeItem(API_KEY_STORAGE); // Altbestand aufräumen
    } catch (e) {}
  }, []);
  const setAiProvider = useCallback((p) => {
    setAiProviderState(p);
    try { localStorage.setItem(AI_PROVIDER_STORAGE, p); } catch (e) {}
  }, []);
  const setOllamaModel = useCallback((m) => {
    setOllamaModelState(m);
    try { localStorage.setItem(OLLAMA_MODEL_STORAGE, m); } catch (e) {}
  }, []);
  // Gebündelte KI-Konfiguration für alle Aufrufstellen
  // Mit Server läuft die KI über das Backend (Keys bleiben dort), ohne Server
  // über die im Browser hinterlegten Keys.
  const aiConfig = { keys: apiKeys, provider: aiProvider, ollamaModel, useServer: backend && api.aiAvailable };
  const aiReady = (backend && api.aiAvailable) || aiProvider === "ollama" || apiKeys.length > 0;
  // KI-Zugänge sind Betreibersache: API-Keys gehören nicht in die Hände der
  // Lernenden. Sichtbar ist der Dialog deshalb nur für die Verwaltung.
  const aiConfigurable = canAdmin(me);
  const openAiSettings = useCallback(() => {
    if (!canAdmin(me)) return;
    setAiSettingsOpen(true);
  }, [me]);
  const closeAiSettings = useCallback(() => setAiSettingsOpen(false), []);
  const toggleSound = useCallback(() => {
    const next = !audio.enabled;
    audio.setEnabled(next);       // spielt beim Einschalten einen kurzen Klick
    setSoundOn(next);
  }, []);
  const openEmailVerify = useCallback(() => setEmailVerifyOpen(true), []);
  const closeEmailVerify = useCallback(() => setEmailVerifyOpen(false), []);

  const celebrate = useCallback(() => { setConfetti(true); setTimeout(() => setConfetti(false), 3200); }, []);

  const pushToast = useCallback((type, msg) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t, { id, type, msg }]);
    if (type === "badge") playSound("badge");
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3200);
  }, []);

  // 2FA: Aktivieren erzeugt einen einmaligen 6-stelligen Code, der (weil kein
  // Authenticator-Backend existiert) direkt im UI angezeigt wird.
  const enable2FA = useCallback(async () => {
    if (!me) return;
    if (api.available) {
      try {
        // Der Server erzeugt ein echtes TOTP-Geheimnis für Authenticator-Apps.
        const { secret, uri } = await api.post("/api/auth/2fa/setup");
        setTwoFactorSetupCode({ secret, uri, needsConfirm: true });
      } catch (e) { pushToast("error", e.message); }
      return;
    }
    const code = String(Math.floor(100000 + Math.random() * 900000));
    setUsers((us) => us.map((u) => u.id === me.id ? { ...u, twoFactorEnabled: true, twoFactorCode: code } : u));
    setTwoFactorSetupCode({ code });
  }, [me, pushToast]);

  /** Schließt die 2FA-Einrichtung ab (nur im Server-Betrieb nötig). */
  const confirm2FA = useCallback(async (code) => {
    try {
      await api.post("/api/auth/2fa/enable", { code });
      await refreshMe();
      setTwoFactorSetupCode(null);
      pushToast("success", "2FA ist aktiviert.");
      return true;
    } catch (e) {
      pushToast("error", e.message);
      return false;
    }
  }, [pushToast, refreshMe]);

  const disable2FA = useCallback(async (password) => {
    if (!me) return;
    if (api.available) {
      try {
        await api.post("/api/auth/2fa/disable", { password });
        await refreshMe();
        pushToast("info", "2FA deaktiviert.");
      } catch (e) { pushToast("error", e.message); }
      return;
    }
    setUsers((us) => us.map((u) => u.id === me.id ? { ...u, twoFactorEnabled: false, twoFactorCode: null } : u));
    pushToast("info", "2FA deaktiviert.");
  }, [me, pushToast, refreshMe]);
  const closeTwoFactorSetup = useCallback(() => setTwoFactorSetupCode(null), []);

  /** Eigenes Passwort ändern — das alte muss stimmen. */
  const changePassword = useCallback(async (currentPassword, newPassword) => {
    if (!me || me.isGuest) { pushToast("error", "Dafür brauchst du ein Konto."); return false; }
    if (!passwordOk(newPassword, { name: me.name, email: me.email })) {
      pushToast("error", "Das neue Passwort erfüllt die Mindestanforderungen nicht.");
      return false;
    }
    if (api.available) {
      try {
        await api.post("/api/auth/change-password", { currentPassword, newPassword });
        pushToast("success", "Passwort geändert — andere Anmeldungen wurden beendet.");
        return true;
      } catch (e) { pushToast("error", e.message); return false; }
    }
    if (!(await verifyLocalPassword(currentPassword, me))) { pushToast("error", "Das aktuelle Passwort ist falsch."); return false; }
    const fields = await localPasswordFields(newPassword);
    setUsers((us) => us.map((u) => u.id === me.id ? { ...u, password: undefined, ...fields } : u));
    pushToast("success", "Passwort geändert.");
    return true;
  }, [me, pushToast]);

  /** Administratoren setzen das Passwort eines beliebigen Kontos neu. */
  const adminSetPassword = useCallback(async (userId, newPassword) => {
    if (!canAdmin(me)) return false;
    const target = users.find((u) => u.id === userId);
    if (!passwordOk(newPassword, { name: target?.name, email: target?.email })) {
      pushToast("error", "Das Passwort erfüllt die Mindestanforderungen nicht.");
      return false;
    }
    if (api.available) {
      try {
        await api.patch(`/api/admin/users/${userId}`, { password: newPassword });
        pushToast("success", "Passwort gesetzt — das Konto wurde überall abgemeldet.");
        return true;
      } catch (e) { pushToast("error", e.message); return false; }
    }
    const fields = await localPasswordFields(newPassword);
    setUsers((us) => us.map((u) => u.id === userId ? { ...u, password: undefined, ...fields } : u));
    pushToast("success", "Passwort gesetzt.");
    return true;
  }, [me, users, pushToast]);

  /* ---------------------- Eigene Lektionen (Level) ------------------------
     Lehrkräfte legen eigene Level an; ihre Klasse sieht die veröffentlichten.
     Ohne Server liegen sie im Konto der Lehrkraft im selben Browser.
     ---------------------------------------------------------------------- */
  const loadCustomLessons = useCallback(async () => {
    if (!api.available || !me || me.isGuest) return;
    try {
      if (me.role === "teacher") {
        const { lessons } = await api.get("/api/lessons/mine");
        setRemoteLessons((r) => ({ ...r, mine: lessons || [] }));
      } else if (me.teacherId) {
        const { lessons } = await api.get("/api/lessons");
        setRemoteLessons((r) => ({ ...r, fromTeacher: lessons || [] }));
      }
    } catch (e) { /* ohne eigene Level ist die Seite trotzdem benutzbar */ }
  }, [me]);

  useEffect(() => { loadCustomLessons(); }, [loadCustomLessons]);

  const myLessons = api.available ? remoteLessons.mine : (me?.customLessons || []);
  const lessonsFromTeacher = api.available
    ? remoteLessons.fromTeacher
    : (users.find((u) => u.id === me?.teacherId)?.customLessons || []).filter((l) => l.published);

  const saveCustomLesson = useCallback(async (lesson) => {
    if (!me || me.role !== "teacher") return false;
    if (api.available) {
      try {
        const path = lesson.id ? `/api/lessons/${lesson.id}` : "/api/lessons";
        await api.put(path, lesson);
        await loadCustomLessons();
        pushToast("success", lesson.published ? "Level veröffentlicht." : "Entwurf gesichert.");
        return true;
      } catch (e) { pushToast("error", e.message); return false; }
    }
    const id = lesson.id || "cl_" + Math.random().toString(36).slice(2, 9);
    setUsers((us) => us.map((u) => {
      if (u.id !== me.id) return u;
      const existing = u.customLessons || [];
      const next = existing.some((l) => l.id === id)
        ? existing.map((l) => (l.id === id ? { ...lesson, id } : l))
        : [...existing, { ...lesson, id }];
      return { ...u, customLessons: next };
    }));
    pushToast("success", lesson.published ? "Level veröffentlicht." : "Entwurf gesichert.");
    return true;
  }, [me, pushToast, loadCustomLessons]);

  const deleteCustomLesson = useCallback(async (id) => {
    if (!me || me.role !== "teacher") return;
    if (api.available) {
      try { await api.del(`/api/lessons/${id}`); await loadCustomLessons(); pushToast("info", "Level gelöscht."); }
      catch (e) { pushToast("error", e.message); }
      return;
    }
    setUsers((us) => us.map((u) => u.id === me.id
      ? { ...u, customLessons: (u.customLessons || []).filter((l) => l.id !== id) }
      : u));
    pushToast("info", "Level gelöscht.");
  }, [me, pushToast, loadCustomLessons]);

  /**
   * Findet eine Lektion — eigene Level zuerst, dann die eingebauten Kurse.
   * Eigene Level bekommen dieselbe Form wie eingebaute, damit die
   * Lektionsansicht sie ohne Sonderbehandlung darstellen kann.
   */
  const findLesson = useCallback((lessonId) => {
    const custom = [...myLessons, ...lessonsFromTeacher].find((l) => l.id === lessonId);
    if (custom) {
      const course = courseById(custom.courseId) || COURSES[0];
      return {
        ...custom,
        estimatedMinutes: Math.max(5, custom.tasks.length * 3),
        _course: course,
        _module: { title: "Eigenes Level", level: custom.level },
        isCustom: true,
      };
    }
    return getFullLesson(lessonId);
  }, [myLessons, lessonsFromTeacher]);

  /** Lädt die Projekte des angemeldeten Nutzers vom Server. */
  const loadProjects = useCallback(async () => {
    if (!api.available || !me || me.isGuest) return;
    try {
      const { projects, storage } = await api.get("/api/projects");
      setUsers((us) => us.map((u) => u.id === me.id
        ? { ...u, playground: projects, storageUsed: storage.used, storageQuota: storage.quota }
        : u));
    } catch (e) {}
  }, [me]);

  // Playground: Projekte speichern/löschen. Mit Server wird das Kontingent
  // dort durchgesetzt, ohne Server lokal nachgebildet.
  // Ein Projekt besteht aus beliebig vielen Dateien. `silent` unterdrückt die
  // Rückmeldung — so stört das automatische Speichern alle 30 Sekunden nicht.
  const savePlaygroundProject = useCallback(async (project, { silent = false } = {}) => {
    if (!me) return false;
    const files = (project.files || []).map((f) => ({ name: f.name, content: f.content || "" }));
    if (api.available && !me.isGuest) {
      try {
        const path = project.id && !String(project.id).startsWith("u_")
          ? `/api/projects/${project.id}` : "/api/projects";
        await api.put(path, { name: project.name, files });
        await loadProjects();
        if (!silent) pushToast("success", `Projekt „${project.name}“ gespeichert.`);
        return true;
      } catch (e) {
        pushToast("error", e.message);
        return false;
      }
    }
    const sizeBytes = projectBytes(files);
    const others = (me.playground || []).filter((p) => p.id !== project.id);
    const usedByOthers = others.reduce((sum, p) => sum + (p.sizeBytes || 0), 0);
    if (usedByOthers + sizeBytes > STORAGE_QUOTA_BYTES) {
      pushToast("error", "Speicherkontingent (2,5 GB) erreicht — lösche ein Projekt, um Platz zu schaffen.");
      return false;
    }
    const saved = { id: project.id, name: project.name, files, sizeBytes, updatedAt: "Jetzt" };
    setUsers((us) => us.map((u) => u.id === me.id ? { ...u, playground: [...others, saved] } : u));
    if (!silent) pushToast("success", `Projekt „${project.name}“ gespeichert.`);
    return true;
  }, [me, pushToast, loadProjects]);

  const deletePlaygroundProject = useCallback(async (id) => {
    if (!me) return;
    if (api.available && !me.isGuest) {
      try { await api.del(`/api/projects/${id}`); await loadProjects(); }
      catch (e) { pushToast("error", e.message); }
      return;
    }
    setUsers((us) => us.map((u) => u.id === me.id ? { ...u, playground: (u.playground || []).filter((p) => p.id !== id) } : u));
  }, [me, pushToast, loadProjects]);

  // Melden: KI-Antworten (oder andere Inhalte) für die Admin-Prüfung markieren
  const reportContent = useCallback(async (payload) => {
    if (api.available && me && !me.isGuest) {
      try {
        await api.post("/api/reports", payload);
        pushToast("success", "Danke — dein Hinweis wurde gemeldet.");
      } catch (e) { pushToast("error", e.message); }
      return;
    }
    const report = {
      id: uid(), createdAt: "Jetzt", status: "open",
      reporterId: me ? me.id : null, reporterName: me ? me.name : "Unbekannt",
      ...payload,
    };
    setReports((rs) => [report, ...rs]);
    pushToast("success", "Danke — dein Hinweis wurde gemeldet.");
  }, [me, pushToast]);
  const resolveReport = useCallback((id) => setReports((rs) => rs.map((r) => r.id === id ? { ...r, status: "resolved" } : r)), []);
  const deleteReport = useCallback((id) => setReports((rs) => rs.filter((r) => r.id !== id)), []);

  // Admin: Nutzer verwalten + weitere Admins erstellen
  const adminUpdateUser = useCallback((userId, patch) => {
    setUsers((us) => us.map((u) => u.id === userId ? { ...u, ...patch } : u));
    pushToast("success", "Nutzer aktualisiert.");
  }, [pushToast]);
  const adminDeleteUser = useCallback((userId) => {
    setUsers((us) => us.filter((u) => u.id !== userId));
    pushToast("info", "Account gelöscht.");
  }, [pushToast]);
  const adminCreateAdmin = useCallback(async (form) => {
    if (!form.name || !form.email || !form.password) { pushToast("error", "Bitte alle Felder ausfüllen."); return false; }
    if (!passwordOk(form.password, { name: form.name, email: form.email })) {
      pushToast("error", "Das Passwort erfüllt die Mindestanforderungen nicht.");
      return false;
    }
    if (users.some((u) => !u.isGuest && u.email.toLowerCase() === form.email.trim().toLowerCase())) { pushToast("error", "E-Mail bereits vergeben."); return false; }
    const newAdmin = {
      id: uid(), role: "admin", name: form.name, email: form.email,
      ...(await localPasswordFields(form.password)),
      createdAt: "Heute", avatar: "🛡️", emailVerified: true, twoFactorEnabled: false,
    };
    setUsers((us) => [...us, newAdmin]);
    pushToast("success", `Admin-Account für ${form.name} erstellt.`);
    return true;
  }, [users, pushToast]);

  const showXP = useCallback((amount) => {
    setXpPopup(amount);
    setTimeout(() => setXpPopup(null), 1600);
  }, []);

  const navigate = useCallback((v) => { setView(v); setSidebarOpen(false); window.scrollTo(0, 0); }, []);

  const completeLogin = (u) => {
    setCurrentUser(u.id);
    pushToast("success", `Willkommen zurück, ${u.name.split(" ")[0]}!`);
    navigate(u.role === "teacher" ? "teacher" : u.role === "admin" ? "admin" : "dashboard");
  };

  const login = async (email, password, turnstileToken) => {
    if (api.available) {
      try {
        const { user } = await api.post("/api/auth/login", { email, password, turnstileToken });
        const mapped = fromApiUser(user);
        setUsers([mapped]);
        completeLogin(mapped);
        return true;
      } catch (e) {
        if (e.data?.need2fa) {
          // Zugangsdaten für den zweiten Schritt vormerken (nur im Speicher)
          setPending2FA({ email, password, turnstileToken });
          pushToast("info", "2FA aktiv — bitte gib deinen Code ein.");
          return "2fa";
        }
        pushToast("error", e.message);
        return false;
      }
    }
    const candidate = users.find((x) => !x.isGuest && x.email.toLowerCase() === email.trim().toLowerCase());
    const u = candidate && (await verifyLocalPassword(password, candidate)) ? candidate : null;
    if (u && !u.passwordHash) {
      // Altbestand: beim ersten erfolgreichen Login auf einen Hash umstellen.
      const fields = await localPasswordFields(password);
      setUsers((us) => us.map((x) => (x.id === u.id ? { ...x, password: undefined, ...fields } : x)));
    }
    if (!u) {
      pushToast("error", users.some((x) => !x.isGuest)
        ? "E-Mail oder Passwort falsch."
        : "In diesem Browser gibt es noch kein Konto — registriere dich zuerst.");
      return false;
    }
    if (u.twoFactorEnabled) {
      setPending2FA(u.id);
      pushToast("info", "2FA aktiv — bitte gib deinen Code ein.");
      return "2fa";
    }
    completeLogin(u);
    return true;
  };

  const verify2FALogin = async (code) => {
    if (api.available) {
      const pending = pending2FA || {};
      try {
        const { user } = await api.post("/api/auth/login", { ...pending, totp: code });
        const mapped = fromApiUser(user);
        setUsers([mapped]);
        setPending2FA(null);
        completeLogin(mapped);
        return true;
      } catch (e) {
        pushToast("error", e.message);
        return false;
      }
    }
    const u = users.find((x) => x.id === pending2FA);
    if (!u) return false;
    if ((code || "").trim() !== u.twoFactorCode) { pushToast("error", "2FA-Code ist falsch."); return false; }
    setPending2FA(null);
    completeLogin(u);
    return true;
  };
  const cancel2FALogin = () => setPending2FA(null);

  const register = async (form) => {
    if (!form.name || !form.email || !form.password) { pushToast("error", "Bitte alle Pflichtfelder ausfüllen."); return false; }
    if (form.password !== form.confirm) { pushToast("error", "Passwörter stimmen nicht überein."); return false; }
    if (!passwordOk(form.password, { name: form.name, email: form.email })) {
      pushToast("error", "Das Passwort erfüllt die Mindestanforderungen nicht.");
      return false;
    }

    if (api.available) {
      try {
        const res = await api.post("/api/auth/register", {
          name: form.name, email: form.email, password: form.password,
          role: form.role, teacherCode: form.teacherCode, school: form.school,
          turnstileToken: form.turnstileToken,
        });
        const mapped = fromApiUser(res.user);
        setUsers([mapped]);
        setCurrentUser(mapped.id);
        if (res.teacherHint) pushToast("error", res.teacherHint);
        if (res.schoolCode) pushToast("info", `Dein Schul-Code: ${res.schoolCode}`);
        // Ohne Mailversand liefert der Server den Code zurück, damit die
        // Bestätigung auch ohne SMTP getestet werden kann.
        if (res.devVerificationCode) setServerVerificationCode(res.devVerificationCode);
        pushToast("success", `Account erstellt — los geht's, ${form.name.split(" ")[0]}!`);
        navigate(roleHome(mapped.role));
        return true;
      } catch (e) {
        pushToast("error", e.message);
        return false;
      }
    }

    if (users.some((u) => !u.isGuest && u.email.toLowerCase() === form.email.trim().toLowerCase())) { pushToast("error", "E-Mail ist bereits registriert."); return false; }
    const id = uid();
    const guest = me && me.isGuest ? me : null; // Gast-Fortschritt beim Registrieren übernehmen
    const verificationCode = String(Math.floor(100000 + Math.random() * 900000));
    const pw = await localPasswordFields(form.password);
    let newUser;
    if (form.role === "teacher") {
      const code = genSchoolCode();
      newUser = {
        id, role: "teacher", name: form.name, email: form.email, ...pw, school: form.school || "—", schoolCode: code,
        students: [], createdAt: "Heute", avatar: guest ? guest.avatar : "👨‍🏫",
        emailVerified: false, verificationCode, twoFactorEnabled: false, playground: [],
      };
      pushToast("info", `Dein Schul-Code: ${code}`);
    } else {
      let teacherId = null;
      if (form.teacherCode) {
        const t = users.find((u) => u.role === "teacher" && u.schoolCode.toLowerCase() === form.teacherCode.trim().toLowerCase());
        if (t) { teacherId = t.id; setUsers((us) => us.map((u) => u.id === t.id ? { ...u, students: [...u.students, id] } : u)); }
        else pushToast("error", "Lehrer-Code nicht gefunden — du lernst erstmal selbstständig.");
      }
      newUser = {
        id, role: "student", name: form.name, email: form.email, ...pw, teacherId,
        xp: guest ? guest.xp : 0, streak: guest ? guest.streak : 1,
        completedLessons: guest ? guest.completedLessons : [], currentCourse: guest ? guest.currentCourse : null,
        joinedAt: "Heute", lastLogin: "Jetzt", avatar: guest ? guest.avatar : "🧑‍💻", badges: guest ? guest.badges : [],
        emailVerified: false, verificationCode, twoFactorEnabled: false, playground: [],
      };
    }
    // Ohne Server gibt es keine vorgefertigten Konten. Damit die Verwaltung
    // trotzdem erreichbar bleibt, wird das allererste Konto zum Inhaber
    // dieser Installation.
    if (!users.some((u) => !u.isGuest)) {
      newUser.isOwner = true;
      setTimeout(() => pushToast("info", "Du bist das erste Konto — dir gehört diese Installation samt Verwaltung."), 800);
    }
    setUsers((us) => [...us.filter((u) => !(guest && u.id === guest.id)), newUser]);
    setCurrentUser(id);
    pushToast("success", guest ? `Account erstellt — dein Gast-Fortschritt wurde übernommen!` : `Account erstellt — los geht's, ${form.name.split(" ")[0]}!`);
    navigate(form.role === "teacher" ? "teacher" : "dashboard");
    return true;
  };

  const verifyEmail = async (code) => {
    if (!me) return false;
    if (api.available) {
      try {
        await api.post("/api/auth/verify-email", { code });
        await refreshMe();
        setServerVerificationCode(null);
        pushToast("success", "E-Mail-Adresse bestätigt!");
        closeEmailVerify();
        return true;
      } catch (e) {
        pushToast("error", e.message);
        return false;
      }
    }
    if ((code || "").trim() !== me.verificationCode) { pushToast("error", "Code stimmt nicht überein."); return false; }
    setUsers((us) => us.map((u) => u.id === me.id ? { ...u, emailVerified: true, verificationCode: null } : u));
    pushToast("success", "E-Mail-Adresse bestätigt!");
    closeEmailVerify();
    return true;
  };

  const logout = async () => {
    if (api.available) { try { await api.post("/api/auth/logout"); } catch (e) {} }
    setCurrentUser(null);
    setUsers((us) => (api.available ? [] : us));
    navigate("landing");
    pushToast("info", "Abgemeldet. Bis bald!");
  };

  const continueAsGuest = () => {
    const id = "guest_" + uid();
    const guestUser = {
      id, role: "student", name: "Gast", email: "", password: "", teacherId: null,
      xp: 0, streak: 1, completedLessons: [], currentCourse: null,
      joinedAt: "Heute", lastLogin: "Jetzt", avatar: "🧑‍💻", badges: [], isGuest: true,
    };
    setUsers((us) => [...us, guestUser]);
    setCurrentUser(id);
    pushToast("info", "Du lernst als Gast — dein Fortschritt wird NICHT gespeichert.");
    navigate("dashboard");
  };

  const openCourse = (courseId) => {
    setSelectedCourse(courseId);
    setUsers((us) => us.map((u) => u.id === currentUser ? { ...u, currentCourse: courseId } : u));
    navigate("course");
  };
  const openLesson = (lessonId) => { setSelectedLesson(lessonId); navigate("lesson"); };

  // XP / Lektion abschließen
  const addXP = useCallback((rawAmount, meta = {}) => {
    // Doppelte XP aus dem Shop wirken auf jede Gutschrift.
    const amount = boostActive(me) ? rawAmount * 2 : rawAmount;
    // Aufstieg erkennen, bevor die XP verbucht werden — dann gibt es Fanfare
    // statt des üblichen kurzen Plopp-Tons.
    if (me && !Number.isNaN(me.xp)) {
      const before = getLevelInfo(me.xp);
      const after = getLevelInfo(me.xp + amount);
      if (after.level > before.level) {
        playSound("levelUp");
        setTimeout(() => pushToast("badge", `Level ${after.level} erreicht — ${after.name}!`), 300);
      } else {
        playSound("xp");
      }
    }
    // Optimistisch anzeigen, damit die Oberfläche sofort reagiert …
    setUsers((us) => us.map((u) => {
      if (u.id !== currentUser) return u;
      const rolled = rolloverLeague(u, null, 0);
      return { ...rolled, xp: rolled.xp + amount, weeklyXp: (rolled.weeklyXp || 0) + amount };
    }));
    // … und serverseitig verbuchen, wo der Wert manipulationssicher liegt.
    if (api.available && me && !me.isGuest) {
      api.post("/api/progress/xp", { amount: rawAmount, attempts: meta.attempts, usedHint: meta.usedHint })
        .then(({ user }) => setUsers((us) => us.map((u) => u.id === user.id ? fromApiUser(user) : u)))
        .catch(() => {});
    }
  }, [currentUser, me, pushToast]);

  /**
   * Einen Artikel im XP-Shop kaufen. Ohne Server wird alles lokal verrechnet,
   * mit Server prüft dieser Preis und Obergrenzen noch einmal nach.
   */
  const buyShopItem = useCallback(async (itemId) => {
    if (!me) return false;
    const item = shopItemById(itemId);
    if (!item) return false;
    if (me.isGuest) { pushToast("error", "Als Gast kannst du nichts kaufen — erstelle ein Konto."); return false; }
    if (xpBalance(me) < item.price) {
      pushToast("error", `Dafür fehlen dir noch ${item.price - xpBalance(me)} XP.`);
      return false;
    }
    if (item.kind === "unlock" && hasUnlock(me, item.id)) { pushToast("info", "Das hast du schon."); return false; }
    if (item.kind === "stack" && ownedCount(me, item) >= item.max) {
      pushToast("info", `Mehr als ${item.max} kannst du davon nicht halten.`);
      return false;
    }

    if (api.available) {
      try {
        const { user } = await api.post("/api/shop/buy", { itemId });
        setUsers((us) => us.map((u) => (u.id === user.id ? fromApiUser(user) : u)));
        pushToast("success", `${item.name} gekauft.`);
        return true;
      } catch (e) { pushToast("error", e.message); return false; }
    }

    setUsers((us) => us.map((u) => {
      if (u.id !== me.id) return u;
      const next = { ...u, spentXp: (u.spentXp || 0) + item.price };
      if (item.id === "streak_freeze") next.streakFreezes = (u.streakFreezes || 0) + 1;
      else if (item.id === "hint") next.hints = (u.hints || 0) + 1;
      else if (item.kind === "unlock") next.unlocks = [...(u.unlocks || []), item.id];
      else if (item.kind === "timed") {
        const from = Math.max(Date.now(), u.boostUntil || 0);
        next.boostUntil = from + item.hours * 3600_000;
      }
      return next;
    }));
    pushToast("success", `${item.name} gekauft.`);
    return true;
  }, [me, pushToast]);

  /** Einen Tipp-Joker einlösen. */
  const useHint = useCallback(async () => {
    if (!me || (me.hints || 0) <= 0) return false;
    if (api.available) {
      try {
        const { user } = await api.post("/api/shop/use-hint");
        setUsers((us) => us.map((u) => (u.id === user.id ? fromApiUser(user) : u)));
        return true;
      } catch (e) { pushToast("error", e.message); return false; }
    }
    setUsers((us) => us.map((u) => (u.id === me.id ? { ...u, hints: Math.max(0, (u.hints || 0) - 1) } : u)));
    return true;
  }, [me, pushToast]);

  /** Streak-Schutz kaufen — derselbe Weg wie alles andere im Shop. */
  const buyStreakFreeze = useCallback(() => buyShopItem("streak_freeze"), [buyShopItem]);

  const completeLesson = useCallback(async (lessonId, bonusXp, stats = {}) => {
    if (api.available && me && !me.isGuest) {
      const completed = [...(me.completedLessons || []), lessonId];
      try {
        const res = await api.post("/api/progress/complete", {
          lessonId,
          courseId: findLessonMeta(lessonId)?.course.id,
          xpReward: bonusXp,
          firstTry: stats.firstTry,
          taskCount: stats.taskCount,
          badges: earnedBadgesFor(completed, me.xp + bonusXp),
        });
        const mapped = fromApiUser(res.user);
        setUsers((us) => us.map((u) => u.id === mapped.id ? mapped : u));
        (res.newBadges || []).forEach((b) => {
          if (BADGES[b]) setTimeout(() => pushToast("badge", `Neues Abzeichen: ${BADGES[b].label}!`), 400);
        });
        return;
      } catch (e) {
        pushToast("error", "Fortschritt konnte nicht gespeichert werden.");
        return;
      }
    }

    setUsers((us) => us.map((u) => {
      if (u.id !== currentUser) return u;
      if (u.completedLessons.includes(lessonId)) return u;
      const completed = [...u.completedLessons, lessonId];
      const badges = [...u.badges];
      const newlyEarned = [];
      if (!badges.includes("first_lesson")) { badges.push("first_lesson"); newlyEarned.push("first_lesson"); }
      const jsDone = completed.filter((id) => id.startsWith("javascript_")).length;
      if (jsDone >= 10 && !badges.includes("js_beginner")) { badges.push("js_beginner"); newlyEarned.push("js_beginner"); }
      const newXp = u.xp + bonusXp;
      if (getLevelInfo(newXp).level >= 10 && !badges.includes("mid_wizard")) { badges.push("mid_wizard"); newlyEarned.push("mid_wizard"); }
      // Kurs komplett?
      const meta = findLessonMeta(lessonId);
      if (meta) {
        const allIds = allLessonsOf(meta.course).map((l) => l.id);
        if (allIds.every((id) => completed.includes(id)) && !badges.includes("course_complete")) { badges.push("course_complete"); newlyEarned.push("course_complete"); }
      }
      // Tages-Streak fortschreiben: heute schon gelernt zählt nicht doppelt,
      // gestern gelernt zählt hoch, sonst beginnt die Serie neu.
      const { streak, lastActive, grew, freezes, usedFreeze, broken } = advanceStreak(u);
      if (grew) {
        if (streak === 7 && !badges.includes("week_warrior")) { badges.push("week_warrior"); newlyEarned.push("week_warrior"); }
        if (usedFreeze) setTimeout(() => pushToast("info", `Streak-Schutz eingelöst — deine Serie läuft weiter! 🧊`), 600);
        else if (broken) setTimeout(() => pushToast("info", "Neue Serie gestartet — dranbleiben lohnt sich!"), 600);
        else setTimeout(() => pushToast("success", `${streak} Tage in Folge! 🔥`), 600);
      }

      // Wochenwertung für die Liga mitführen
      const rolled = rolloverLeague(u, null, 0);

      newlyEarned.forEach((b) => setTimeout(() => pushToast("badge", `Neues Abzeichen: ${BADGES[b].label}!`), 400));
      return {
        ...rolled, completedLessons: completed, xp: newXp, badges,
        streak, lastActive, streakFreezes: freezes,
        weeklyXp: (rolled.weeklyXp || 0) + bonusXp,
      };
    }));
  }, [currentUser, pushToast, me]);

  const ctx = {
    view, navigate, users, me, setUsers, currentUser,
    selectedCourse, openCourse, selectedLesson, openLesson,
    selectedStudent, setSelectedStudent, login, register, logout, continueAsGuest,
    pushToast, showXP, addXP, completeLesson, celebrate, buyStreakFreeze, sidebarOpen, setSidebarOpen,
    apiKeys, setApiKeys, aiProvider, setAiProvider, ollamaModel, setOllamaModel, aiConfig, aiReady, aiConfigurable, aiSettingsOpen, openAiSettings, closeAiSettings,
    soundOn, toggleSound,
    backend, booting, refreshMe, loadProjects, serverVerificationCode, confirm2FA,
    pending2FA, verify2FALogin, cancel2FALogin,
    emailVerifyOpen, openEmailVerify, closeEmailVerify, verifyEmail,
    enable2FA, disable2FA, twoFactorSetupCode, closeTwoFactorSetup,
    savePlaygroundProject, deletePlaygroundProject, playgroundOpenId, setPlaygroundOpenId,
    myLessons, lessonsFromTeacher, saveCustomLesson, deleteCustomLesson, findLesson,
    placementTarget, setPlacementTarget,
    buyShopItem, useHint,
    reports, reportContent, resolveReport, deleteReport,
    adminUpdateUser, adminDeleteUser, adminCreateAdmin, adminSetPassword, changePassword,
  };

  const LEGAL_VIEWS = ["agb", "impressum", "datenschutz", "kontakt", "ueber-uns"];

  // Solange die Betriebsart geprüft wird, kurz einen Ladezustand zeigen —
  // sonst würde eine bestehende Server-Sitzung kurz als "abgemeldet" aufblitzen.
  if (booting) {
    return (
      <div className="min-h-screen bg-[#0A0E1A] flex flex-col items-center justify-center gap-4">
        <GlobalStyles />
        <Loader2 size={32} className="ld-spin text-[#4F8EF7]" />
        <p className="text-sm text-[#8A9BC0]">LearnDeveloping wird geladen …</p>
      </div>
    );
  }

  let screen = null;
  if (view === "landing") screen = <Landing ctx={ctx} />;
  else if (view === "login" || view === "register") screen = <AuthScreen ctx={ctx} mode={view} />;
  else if (view === "forgot-password") screen = <ForgotPassword ctx={ctx} />;
  else if (view === "reset-password") screen = <ResetPassword ctx={ctx} />;
  else if (PUBLIC_PAGES.includes(view)) screen = <InfoPage ctx={ctx} page={view} />;
  else if (LEGAL_VIEWS.includes(view)) screen = <LegalPage ctx={ctx} page={view} />;
  else if (view === "lesson") screen = <LessonView ctx={ctx} />;
  else if (view === "placement") screen = <PlacementTest ctx={ctx} />;
  else screen = <AppShell ctx={ctx}>{
    view === "dashboard" ? <StudentDashboard ctx={ctx} /> :
    view === "teacher" ? <TeacherDashboard ctx={ctx} /> :
    view === "admin" ? (canAdmin(me) ? <AdminDashboard ctx={ctx} /> : null) :
    view === "playground" ? <Playground ctx={ctx} /> :
    view === "lesson-editor" ? (me.role === "teacher" ? <LessonEditor ctx={ctx} /> : null) :
    view === "courses" ? <CoursesOverview ctx={ctx} /> :
    view === "course" ? <CourseView ctx={ctx} /> :
    view === "leaderboard" ? <Leaderboard ctx={ctx} /> :
    view === "shop" ? <Shop ctx={ctx} /> :
    view === "profile" ? <Profile ctx={ctx} /> : null
  }</AppShell>;

  return (
    <div className="min-h-screen bg-[#0A0E1A] text-[#E8EDF5]">
      <GlobalStyles />
      {screen}
      <Toasts toasts={toasts} />
      {xpPopup != null && <XPPopup amount={xpPopup} />}
      {confetti && <Confetti />}
      <CookieNotice ctx={ctx} />
      {aiSettingsOpen && aiConfigurable && <AiSettingsModal ctx={ctx} />}
      {emailVerifyOpen && <EmailVerifyModal ctx={ctx} />}
      {twoFactorSetupCode && <TwoFactorSetupModal ctx={ctx} />}
    </div>
  );
}

/* ============================ Landing ============================== */
/* ======================= Eigene Seiten für die Navigation =================
   Jeder Punkt in der oberen Leiste führt auf eine eigene Seite mit richtigem
   Inhalt — nicht bloß auf einen Ankerpunkt weiter unten. Der Text steht als
   Markdown hier, damit er sich pflegen lässt wie ein Dokument.
   ========================================================================= */
const PUBLIC_PAGES = ["kurse", "ide", "features", "schulen", "preise", "ueber-uns"];

const PUBLIC_NAV = [
  { v: "kurse", label: "Kurse", icon: BookOpen },
  { v: "ide", label: "IDE", icon: Code2 },
  { v: "features", label: "Features", icon: Sparkles },
  { v: "schulen", label: "Für Schulen", icon: GraduationCap },
  { v: "preise", label: "Preise", icon: Star },
  { v: "ueber-uns", label: "Über uns", icon: Info },
];

/** Kopfleiste und Fußzeile für alle öffentlichen Seiten. */
function PublicShell({ ctx, children }) {
  const { navigate, view } = ctx;
  return (
    <div className="min-h-screen flex flex-col">
      <nav className="fixed top-0 inset-x-0 z-50 backdrop-blur-md bg-[#0A0E1A]/85 border-b border-[#1E2D4A]">
        <div className="max-w-6xl mx-auto px-5 h-16 flex items-center justify-between gap-4">
          <Logo onClick={() => navigate("landing")} />
          <div className="hidden lg:flex items-center gap-6 text-sm text-[#8A9BC0]">
            {PUBLIC_NAV.map((n) => (
              <button key={n.v} onClick={() => navigate(n.v)}
                className={`transition-colors flex items-center gap-1.5 ${view === n.v ? "text-[#4F8EF7]" : "hover:text-[#E8EDF5]"}`}>
                <n.icon size={13} />{n.label}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Btn variant="ghost" size="sm" onClick={() => navigate("login")}>Anmelden</Btn>
            <Btn size="sm" icon={Rocket} onClick={() => navigate("register")}>Jetzt starten</Btn>
          </div>
        </div>
        <div className="lg:hidden flex items-center gap-4 px-5 pb-2.5 overflow-x-auto text-xs text-[#8A9BC0]">
          {PUBLIC_NAV.map((n) => (
            <button key={n.v} onClick={() => navigate(n.v)}
              className={`whitespace-nowrap ${view === n.v ? "text-[#4F8EF7]" : "hover:text-[#E8EDF5]"}`}>{n.label}</button>
          ))}
        </div>
      </nav>

      <main className="flex-1 pt-24 pb-16">{children}</main>

      <footer className="border-t border-[#1E2D4A] py-10">
        <div className="max-w-6xl mx-auto px-5 flex flex-col md:flex-row items-center justify-between gap-4">
          <div>
            <Logo />
            <p className="text-sm text-[#4A5A7A] mt-2">Code lernen. Richtig lernen.</p>
          </div>
          <div className="flex flex-wrap justify-center gap-x-6 gap-y-1 text-sm text-[#8A9BC0]">
            <button onClick={() => navigate("ueber-uns")} className="hover:text-[#E8EDF5]">Über uns</button>
            <button onClick={() => navigate("agb")} className="hover:text-[#E8EDF5]">AGB</button>
            <button onClick={() => navigate("impressum")} className="hover:text-[#E8EDF5]">Impressum</button>
            <button onClick={() => navigate("datenschutz")} className="hover:text-[#E8EDF5]">Datenschutz</button>
            <button onClick={() => navigate("kontakt")} className="hover:text-[#E8EDF5]">Kontakt</button>
          </div>
        </div>
      </footer>
    </div>
  );
}

/* ------------------------------ Der Inhalt ------------------------------- */
const PAGE_CONTENT = {
  kurse: {
    icon: BookOpen, color: "#4F8EF7",
    title: "Kurse",
    lead: "Fünfzehn Sprachen, vom ersten Tag bis zu den Themen, nach denen im Vorstellungsgespräch gefragt wird.",
    body: `## Wie ein Kurs aufgebaut ist

Jeder Kurs besteht aus **Modulen**, jedes Modul aus **Lektionen**. Ein Modul öffnet sich erst, wenn das vorherige vollständig ist — nicht um dich zu bremsen, sondern weil die Themen aufeinander aufbauen. Wer Schleifen überspringt, scheitert später an Arrays.

Eine Lektion hat immer denselben Aufbau:

1. **Theorie** links — kurz gehalten, mit Beispielen, die du direkt nachbauen kannst.
2. **Aufgaben** rechts — Lückentexte, echte Code-Aufgaben, Multiple Choice und gelegentlich eine Erklärung in eigenen Worten.
3. **Sofortige Bewertung** — jede Antwort wird im Browser geprüft. Ohne Wartezeit, ohne Netzwerk.

Abgeschlossen ist eine Lektion erst, wenn **alle** Aufgaben richtig sind. Durchklicken bringt keine XP.

## Die vier Aufgabentypen

| Typ | Was du tust | Wie geprüft wird |
|-----|-------------|------------------|
| Multiple Choice | Eine von mehreren Antworten wählen | Direkter Vergleich |
| Lückentext | Fehlende Begriffe einsetzen | Schreibweisen, Zahlwörter und Satzzeichen werden toleriert |
| Code schreiben | Echten Code im Editor tippen | Struktur, Klammern, erwartete Bausteine, typische Stolperfallen |
| Erklären | Ein Konzept in eigenen Worten | Fachbegriffe, Begründung, Eigenständigkeit |

## Womit du anfangen solltest

- **Noch nie programmiert?** Beginne mit **HTML**. Du siehst sofort ein Ergebnis, und das hält bei der Stange.
- **Webseiten sollen schön aussehen?** Danach **CSS** — Box-Model, Flexbox, Grid.
- **Etwas soll passieren, wenn man klickt?** **JavaScript**. Der größte Kurs, und der mit dem breitesten Einsatzgebiet.
- **Schule oder Studium?** Oft **Java** oder **Python**. Python ist der sanftere Einstieg.
- **Es soll schnell sein?** **C**, **C++**, **Rust** oder **Go**.
- **Daten auswerten?** **SQL** — der Kurs, der sich am schnellsten auszahlt.

## Wie lange dauert das?

Eine Lektion kostet zehn bis fünfzehn Minuten. Ein Modul hat vier bis fünf Lektionen, ein Kurs drei bis vier Module. Wer täglich eine Lektion schafft, ist in gut zwei Wochen durch einen Kurs — und hat dabei mehr behalten als bei einem Wochenendmarathon.

> 💡 Die Serie (Streak) zählt Kalendertage, nicht Stunden. Abends eine Lektion und am nächsten Morgen die nächste sind zwei Tage.`,
    showCourses: true,
  },

  ide: {
    icon: Code2, color: "#7C3AED",
    title: "Die IDE",
    lead: "Ein echter Editor im Browser — derselbe, der in Visual Studio Code arbeitet.",
    body: `## Was hier drinsteckt

Der **LearnDeveloping Editor** ist eine vollwertige Entwicklungsumgebung, kein Textfeld mit Syntaxfarben. Er bringt mit, was man zum Arbeiten wirklich braucht:

- Klammerpaare in Farbe, Einrückungslinien, Sticky Scroll und Code-Faltung
- Automatisches Schließen von Klammern, Anführungszeichen — und von **HTML-Tags**: Tippst du \`<h1>\`, entsteht \`</h1>\` von selbst
- **Emmet**: \`!\` + Tab erzeugt ein komplettes HTML-Grundgerüst, \`ul>li*3\` eine Liste
- Verknüpftes Bearbeiten: Änderst du \`<div>\`, ändert sich der schließende Tag mit
- Mehrfachcursor mit **Alt**, Zoom mit **Strg + Mausrad**, Zeilenumbruch und Minimap zuschaltbar

## Dateien statt drei Kästen

Ein Projekt ist ein Dateibaum, kein Formular mit drei Feldern. Du legst Dateien für **jede** unterstützte Sprache an — \`index.html\`, \`style.css\`, \`main.py\`, \`Main.java\`, \`server.go\`. Die Endung bestimmt die Sprache, und damit Hervorhebung, Vervollständigung und Prüfung.

Der Editor startet **leer**. Kein Beispielcode, den du erst wegräumen musst.

## Was wirklich läuft

- **HTML, CSS, JavaScript** laufen live in der Vorschau. Verweise zwischen deinen Dateien werden aufgelöst: aus \`<link href="style.css">\` wird der echte Inhalt, wie auf einem Webserver.
- **Python** läuft ebenfalls — echtes CPython, nach WebAssembly übersetzt. Beim ersten Start werden rund 10 MB geladen, danach ist es sofort da. Was nicht geht: \`input()\` und Netzwerkzugriffe.
- **Alle anderen Sprachen** kannst du schreiben, prüfen lassen und herunterladen. Ausführen musst du sie auf deinem Rechner — die Konsole zeigt dir den passenden Befehl dafür.

Das ist die ehrliche Antwort: Ein Knopf, der so tut, als würde er Java kompilieren, hilft niemandem.

## Speichern und Mitnehmen

- **Alle 30 Sekunden** wird automatisch gespeichert, \`Strg+S\` jederzeit.
- **Vorschau in einem eigenen Tab**, der bei jeder Änderung neu lädt — die Scrollposition bleibt erhalten.
- **Herunterladen** als ZIP mit allen Dateien, oder als eine einzige HTML-Datei mit eingebettetem CSS und JavaScript.
- **2,5 GB** Speicher pro Konto für deine Projekte.

## Der Assistent

Rechts sitzt ein KI-Assistent, der deinen Code kennt. Er erklärt, sucht Fehler und schlägt Verbesserungen vor. Das ist die **einzige** Stelle mit KI — Aufgaben werden immer lokal geprüft, damit die Bewertung sofort da ist und nichts kostet.

> 💡 Vollbild schaltet die IDE auf die ganze Fensterfläche. Esc bringt dich zurück.`,
  },

  features: {
    icon: Sparkles, color: "#F7C948",
    title: "Features",
    lead: "Was diese Plattform kann — und warum sie es so macht.",
    body: `## Bewertung ohne Wartezeit

Antworten werden im Browser geprüft, nicht auf einem Server. Das Ergebnis ist **sofort** da, kostet nichts und funktioniert auch offline.

Die Prüfung ist kein Textvergleich. Für Code wird der Quelltext zerlegt: Kommentare und Zeichenketten werden getrennt, Klammern und Tags auf Ausgeglichenheit geprüft, Deklarationen und Funktionsaufrufe gesammelt. Erst danach wird geschaut, ob die erwarteten Bausteine da sind — und ob typische Stolperfallen der Sprache vorkommen, etwa \`==\` statt \`===\` oder ein fehlender Doppelpunkt in Python.

Bei Freitext wird geprüft, ob wirklich eine Begründung dasteht, ob die Fachbegriffe vorkommen und ob die Antwort eigenständig formuliert ist. Tastaturgeklapper fällt durch: \`fgsugugj\` kann kein deutsches Wort sein.

## Ehrliches Feedback

Eine falsche Antwort verrät nicht sofort die Lösung. Zuerst kommt ein Anhaltspunkt — der erste Buchstabe, die Länge, der fehlende Baustein. Die Auflösung gibt es nach drei Versuchen oder gegen einen Tipp-Joker aus dem Shop.

Und es gibt keinen Weg an einer Aufgabe vorbei: Eine Lektion gilt erst als abgeschlossen, wenn jede Aufgabe richtig ist.

## Motivation, die nicht nervt

- **XP und 20 Level** vom Rookie bis zum Code Wizard
- **Tagesserie** nach Kalendertagen, mit Schutzschilden aus dem Shop
- **Sechs Wochenligen** von Bronze bis Meister — die besten drei steigen auf, die letzten drei ab
- **Abzeichen** für Meilensteine
- **XP-Shop**: Schutzschilde, Tipp-Joker, doppelte XP, Avatar-Extras, helles Editor-Design

Die XP, die du ausgibst, zählen weiter für dein Level. Einkaufen kostet dich also keinen Fortschritt.

## Klang

Alle Töne entstehen im Browser selbst — richtig, falsch, Aufstieg, Abzeichen, gespeichert. Keine Audiodateien, nichts wird nachgeladen. Ein Schalter oben rechts schaltet sie ab.

## Dein Konto

- Registrierung mit E-Mail-Bestätigung, optional mit **Zwei-Faktor-Authentifizierung** (TOTP, kompatibel mit jeder Authenticator-App)
- **Mindestanforderungen an Passwörter**, live geprüft beim Tippen
- **Passwort vergessen** über einen einmaligen Link mit Ablaufzeit
- **Ohne Anmeldung testen** — als Gast, dann wird allerdings nichts gespeichert

## Ohne Server nutzbar

Die Plattform läuft auch ganz ohne Backend: Dann bleibt alles im Browser, im lokalen Speicher, und verlässt dein Gerät nie. Mit Backend liegen Konten, Fortschritt und Projekte in einer Datenbank, Passwörter als scrypt-Hash, Sitzungen als HMAC-Hash.`,
  },

  schulen: {
    icon: GraduationCap, color: "#10B981",
    title: "Für Schulen",
    lead: "Eine Klasse in fünf Minuten eingerichtet — ohne Lizenzen, ohne Installation, ohne Kosten.",
    body: `## So läuft es ab

1. Sie legen ein **Lehrer-Konto** an. Dabei entsteht automatisch ein **Schul-Code** wie \`LRND-4K9M\`.
2. Sie geben den Code in der Klasse weiter.
3. Ihre Schülerinnen und Schüler tragen ihn bei der Registrierung ein — fertig. Sie erscheinen in Ihrer Übersicht.

Es braucht keine Installation. Ein Browser genügt, auch auf alten Geräten und auf Tablets.

## Was Sie sehen

Die Klassenübersicht zeigt für jede Person:

- Wie viele Lektionen abgeschlossen sind, und in welchem Kurs
- Gesammelte XP, Level und aktuelle Tagesserie
- Wann zuletzt gearbeitet wurde
- Eine Detailansicht mit dem Fortschritt je Kurs

Damit sehen Sie auf einen Blick, wer hängt und wer vorausläuft — ohne Hefte einzusammeln.

## Eigene Level bauen

Sie sind nicht auf die mitgelieferten Kurse festgelegt. Im Bereich **Eigene Level** schreiben Sie eigene Lektionen:

- **Theorie** als Markdown — Überschriften, Listen, Tabellen, Codeblöcke
- **Aufgaben** in denselben vier Formen wie in den eingebauten Kursen
- **Sprache wählbar** — dadurch wird Code passend geprüft
- **XP festlegen**, Schwierigkeitsgrad setzen
- **Entwurf oder veröffentlicht** — Entwürfe sieht nur Sie

Veröffentlichte Level erscheinen bei allen, die Ihren Schul-Code genutzt haben, und werden mit derselben Analyse geprüft wie alles andere.

## Datenschutz

- Es werden nur Name und E-Mail-Adresse erhoben — sonst nichts.
- Betreiben Sie die Plattform selbst, bleiben alle Daten auf Ihrem Server.
- Ohne Backend verlassen die Daten das Gerät überhaupt nicht.
- Für die Aufgabenprüfung wird **keine KI** verwendet. Es werden also auch keine Schülerantworten an Dritte gesendet.
- Der KI-Assistent in der IDE ist optional und wird von der Administration eingerichtet, nicht von den Lernenden.

## Was es kostet

Nichts. Keine Lizenzgebühren, keine Schülerzahlgrenze, keine Testphase, die abläuft.

> 📧 Fragen zum Einsatz an einer Schule? Schreiben Sie an **contact@learndeveloping.com**.`,
  },

  preise: {
    icon: Star, color: "#F59E0B",
    title: "Preise",
    lead: "Es gibt keine. Das ist die ganze Seite — aber hier steht, warum.",
    body: `## Kostenlos heißt kostenlos

Alle Kurse, alle Lektionen, die IDE, der Fortschritt, die Ligen, der Shop: frei zugänglich. Keine Kreditkarte, kein Abo, keine Testphase, die nach 14 Tagen zuschlägt, keine Lektion, die plötzlich ein Schloss trägt.

Es gibt auch keine Werbung und keinen Weiterverkauf von Daten.

## Warum das geht

Weil die teuren Teile hier nicht teuer sind:

- **Die Aufgabenprüfung läuft im Browser.** Sie kostet keinen Serveraufruf und kein KI-Kontingent. Das ist der Grund, warum sie sofort antwortet — und zugleich der Grund, warum sie nichts kostet.
- **Der Editor wird ausgeliefert, nicht berechnet.** Er läuft vollständig in deinem Browser; unser Server hat damit keine Arbeit.
- **Der Rest ist ein kleiner Server** mit einer Datenbank. Das ist kein Rechenzentrum.

## Der XP-Shop

Im Shop gibst du **XP** aus, die du dir erspielt hast — kein Geld. Es gibt keine Möglichkeit, XP zu kaufen, und keine Absicht, eine zu schaffen. Wer schneller vorankommen will, löst Aufgaben.

## Selbst betreiben

Die Plattform lässt sich vollständig selbst hosten: Node.js, dazu PostgreSQL oder SQLite. Für einen Schulserver reicht sehr wenig. Ohne Backend läuft sie sogar ganz ohne Installation im Browser.

## Und der KI-Assistent?

Der Assistent in der IDE ist der einzige Teil, der Rechenzeit außerhalb kostet. Deshalb wird er von der Administration eingerichtet — entweder mit einem kostenlosen Kontingent bei einem Anbieter oder mit einem eigenen Server. Lernende müssen dafür nichts einrichten und nichts bezahlen.

Und wenn er nicht eingerichtet ist, funktioniert alles andere trotzdem: Die Prüfung der Aufgaben hängt nicht daran.`,
  },

  "ueber-uns": {
    icon: Info, color: "#0EA5E9",
    title: "Über uns",
    lead: "Warum es LearnDeveloping gibt und wie es gebaut ist.",
    body: `## Die Idee

Programmieren lernt man nicht durch Zuschauen. Videos wirken beim Ansehen einleuchtend und sind eine Woche später weg. Was bleibt, ist das, was man selbst getippt hat — und zwar dann, wenn jemand einem sagt, was daran noch nicht stimmt.

Genau das soll diese Plattform sein: **schreiben, sofort geprüft bekommen, weitermachen.** Kein Warten auf eine Antwort, kein Ladebalken zwischen Frage und Rückmeldung.

## Was uns wichtig ist

**Sofort statt irgendwann.** Die Bewertung passiert im Browser. Kein Server, keine Warteschlange, kein Kontingent. Das ist der Grund, warum die Antwort da ist, bevor du die Hand von der Tastatur nimmst.

**Ehrlich statt gefällig.** Eine falsche Antwort wird als falsch bewertet — auch die, die auf den ersten Blick fast richtig aussieht. Und eine Lektion gilt erst als geschafft, wenn wirklich alles stimmt. Ein Fortschrittsbalken, der lügt, hilft niemandem.

**Erklären statt abhaken.** Statt „Falsch" steht da, welcher Baustein fehlt, welche Klammer nicht geschlossen ist oder welches Schlüsselwort verwechselt wurde. Die Lösung kommt aber nicht sofort — erst ein Anhaltspunkt, dann die Auflösung.

**Ohne Hürden.** Keine Kosten, keine Kreditkarte, keine Installation. Ausprobieren geht sogar ohne Konto.

## Wie es gebaut ist

Die Oberfläche ist React. Der **LearnDeveloping Editor** ist eine ausgewachsene Entwicklungsumgebung im Browser — mit Sprachserver, Faltung, Mehrfachcursor und allem, was man von einer Desktop-IDE erwartet. Python läuft als echtes CPython, nach WebAssembly übersetzt.

Die Prüfung ist eine eigene Analyse-Engine mit Sprachprofilen für fünfzehn Sprachen. Sie zerlegt den Quelltext, trennt Kommentare und Zeichenketten ab, prüft Klammern und Tags auf Ausgeglichenheit, sammelt Deklarationen und Aufrufe — und vergleicht das Ergebnis mit dem, was die Aufgabe verlangt. Dazu kommt eine Prüfung auf typische Stolperfallen je Sprache.

Das Backend ist Node.js mit Fastify und läuft wahlweise auf PostgreSQL oder SQLite. Passwörter werden mit scrypt gehasht, Sitzungstoken nur als HMAC-Hash gespeichert. Die Plattform funktioniert auch **ganz ohne Backend** — dann bleibt alles im Browser.

## Wo KI vorkommt — und wo nicht

**Nicht** bei der Bewertung von Aufgaben. Das ist eine bewusste Entscheidung: KI-Antworten dauern Sekunden, kosten Geld und sind nicht reproduzierbar. Für eine Lernplattform, die sofort antworten soll, ist das die falsche Technik.

**Doch** im Editor: Dort sitzt ein Assistent, der deinen Code erklärt, Fehler sucht und Verbesserungen vorschlägt. Dort sind ein paar Sekunden in Ordnung, weil du fragst, statt zu warten. Eingerichtet wird er von der Administration — Lernende sehen keine Zugangsdaten.

## Kontakt

- Allgemeine Anfragen: **contact@learndeveloping.com**
- Hilfe und Fehlermeldungen: **support@learndeveloping.com**

Rückmeldungen zu Aufgaben, die falsch bewertet wurden, sind besonders willkommen — in jeder Bewertung gibt es dafür einen Melden-Knopf. Mehrere Verbesserungen an der Prüfung stammen genau daher.`,
  },
};

function InfoPage({ ctx, page }) {
  const { navigate } = ctx;
  const content = PAGE_CONTENT[page];
  if (!content) return null;
  const Icon = content.icon;

  return (
    <PublicShell ctx={ctx}>
      <div className="max-w-3xl mx-auto px-5">
        <button onClick={() => navigate("landing")} className="flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5] mb-6">
          <ChevronLeft size={16} />Zur Startseite
        </button>

        <div className="flex items-start gap-4 mb-4">
          <span className="w-14 h-14 rounded-2xl flex items-center justify-center shrink-0"
            style={{ background: content.color + "1F", color: content.color }}>
            <Icon size={26} />
          </span>
          <div>
            <h1 className="font-display text-4xl font-extrabold">{content.title}</h1>
            <p className="text-lg text-[#8A9BC0] mt-1 leading-relaxed">{content.lead}</p>
          </div>
        </div>

        <Card className="p-7 mt-8"><Markdown text={content.body} /></Card>

        {content.showCourses && (
          <div className="mt-8">
            <h2 className="font-display text-2xl font-bold mb-4">Alle {COURSES.length} Kurse</h2>
            <div className="grid sm:grid-cols-2 gap-3">
              {COURSES.map((c) => (
                <Card key={c.id} hover onClick={() => navigate("register")} className="p-4 flex items-start gap-3">
                  <CourseIcon course={c} size={26} className="shrink-0" />
                  <div className="min-w-0">
                    <p className="font-display font-bold">{c.name}</p>
                    <p className="text-xs text-[#8A9BC0] leading-relaxed">{c.description}</p>
                    <p className="text-[11px] text-[#4A5A7A] mt-1">
                      {c.totalLessons} Lektionen · {c.modules.length} Module
                    </p>
                  </div>
                </Card>
              ))}
            </div>
          </div>
        )}

        <Card className="p-7 mt-8 text-center">
          <h2 className="font-display text-2xl font-bold mb-2">Am besten selbst ausprobieren</h2>
          <p className="text-[#8A9BC0] mb-5">Ohne Anmeldung testbar — dein Fortschritt wird dann allerdings nicht gespeichert.</p>
          <div className="flex flex-wrap justify-center gap-3">
            <Btn icon={Rocket} onClick={() => navigate("register")}>Konto erstellen</Btn>
            <Btn variant="secondary" icon={ArrowRight} onClick={() => navigate("login")}>Anmelden</Btn>
          </div>
        </Card>
      </div>
    </PublicShell>
  );
}

function Landing({ ctx }) {
  const { navigate } = ctx;
  return (
    <div>
      {/* Top nav */}
      <nav className="fixed top-0 inset-x-0 z-50 backdrop-blur-md bg-[#0A0E1A]/80 border-b border-[#1E2D4A]">
        <div className="max-w-6xl mx-auto px-5 h-16 flex items-center justify-between gap-4">
          <Logo onClick={() => navigate("landing")} />
          <div className="hidden lg:flex items-center gap-6 text-sm text-[#8A9BC0]">
            {PUBLIC_NAV.map((n) => (
              <button key={n.v} onClick={() => navigate(n.v)} className="hover:text-[#E8EDF5] transition-colors flex items-center gap-1.5">
                <n.icon size={13} />{n.label}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Btn variant="ghost" size="sm" onClick={() => navigate("login")}>Anmelden</Btn>
            <Btn size="sm" icon={Rocket} onClick={() => navigate("register")}>Jetzt starten</Btn>
          </div>
        </div>
        {/* Mobile Navigation */}
        <div className="lg:hidden flex items-center gap-4 px-5 pb-2.5 overflow-x-auto text-xs text-[#8A9BC0]">
          {PUBLIC_NAV.map((n) => (
            <button key={n.v} onClick={() => navigate(n.v)} className="whitespace-nowrap hover:text-[#E8EDF5]">{n.label}</button>
          ))}
        </div>
      </nav>

      {/* Hero */}
      <section className="relative min-h-screen flex items-center justify-center px-5 pt-16">
        <TerminalBackground />
        <div className="relative z-10 text-center max-w-3xl mx-auto">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-sm mb-7 border border-white/10 bg-white/5 backdrop-blur-sm">
            <Sparkles size={15} className="text-[#F7C948]" /><span className="text-[#C9D6F0]">KI-gestütztes Lernen</span>
          </div>
          <h1 className="font-display font-black text-5xl md:text-6xl leading-[1.05] mb-5">
            <span className="text-[#E8EDF5]">Code lernen.</span><br />
            <span className="ld-gradient-text">Richtig lernen.</span>
          </h1>
          <p className="text-lg text-[#8A9BC0] max-w-xl mx-auto mb-9">
            Von HTML bis Rust — {COURSES.length} Sprachen, strukturiert aufgebaut, mit KI-Feedback direkt zu deinem Code.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 mb-5">
            <Btn size="lg" icon={ArrowRight} onClick={() => navigate("register")}>Jetzt kostenlos starten</Btn>
            <Btn size="lg" variant="secondary" icon={UserRoundPlus} onClick={ctx.continueAsGuest}>Ohne Anmeldung testen</Btn>
          </div>
          <p className="text-xs text-[#4A5A7A] mb-8">Als Gast wird dein Fortschritt nicht gespeichert — mit Account schon.</p>
          <div className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm text-[#4A5A7A]">
            <span className="flex items-center gap-1.5"><Check size={14} className="text-[#10B981]" />Kostenlos starten</span>
            <span className="flex items-center gap-1.5"><Check size={14} className="text-[#10B981]" />Keine Kreditkarte</span>
            <span className="flex items-center gap-1.5"><Check size={14} className="text-[#10B981]" />Ohne Anmeldung testbar</span>
          </div>
        </div>
      </section>

      {/* Kennzahlen */}
      <section className="border-y border-[#1E2D4A] bg-[#0F1629]">
        <div className="max-w-5xl mx-auto px-5 py-10 grid grid-cols-2 md:grid-cols-4 gap-6 text-center">
          {[
            [COURSES.length + "+", "Programmiersprachen"],
            [TOTAL_LESSONS + "+", "Lektionen"],
            ["4", "Aufgaben-Typen"],
            ["100%", "Kostenlos starten"],
          ].map(([v, l], i) => (
            <div key={i}>
              <div className="font-display text-3xl font-extrabold ld-gradient-text mb-1">{v}</div>
              <div className="text-sm text-[#8A9BC0]">{l}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Kurse */}
      <section id="kurse" className="max-w-6xl mx-auto px-5 py-20">
        <div className="text-center mb-12">
          <h2 className="font-display text-4xl font-extrabold mb-3">{COURSES.length} Sprachen. <span className="ld-gradient-text">Ein Ziel.</span></h2>
          <p className="text-[#8A9BC0] text-lg">Strukturierte Kurse von Anfänger bis Experte — Web, Systeme, Mobile und mehr.</p>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
          {COURSES.map((c) => (
            <Card key={c.id} hover onClick={() => navigate("register")} className="p-5 group relative overflow-hidden">
              <div className="mb-3"><CourseIcon course={c} size={38} /></div>
              <h3 className="font-display text-lg font-bold mb-1">{c.name}</h3>
              <p className="text-sm text-[#8A9BC0] mb-4 leading-snug line-clamp-2">{c.description}</p>
              <div className="flex items-center gap-1.5 text-xs text-[#4A5A7A]"><BookOpen size={13} />{c.totalLessons} Lektionen</div>
              <div className="absolute bottom-0 inset-x-0 h-1" style={{ background: c.color }} />
            </Card>
          ))}
        </div>
      </section>

      {/* Wie es funktioniert */}
      <section id="features" className="bg-[#0F1629] border-y border-[#1E2D4A] py-20">
        <div className="max-w-6xl mx-auto px-5">
          <h2 className="font-display text-4xl font-extrabold text-center mb-3">So funktioniert's</h2>
          <p className="text-[#8A9BC0] text-lg text-center mb-12">Lernen, üben, sofort Feedback bekommen.</p>
          <div className="grid md:grid-cols-3 gap-6">
            {[
              { icon: "theorie", color: "#4F8EF7", t: "Lerne die Theorie", d: "Klare Erklärungen mit echten Code-Beispielen — kein trockenes Geschwafel." },
              { icon: "aufgabe", color: "#F7C948", t: "Löse Aufgaben", d: "Multiple Choice, Code schreiben, Lückentext und freies Erklären." },
              { icon: "feedback", color: "#10B981", t: "Bekomm Feedback", d: "Sofortige, konstruktive Rückmeldung zu jeder Antwort und jedem Code." },
            ].map((f, i) => (
              <Card key={i} className="p-7 text-center">
                <div className="w-14 h-14 mx-auto mb-4 rounded-2xl flex items-center justify-center"
                  style={{ background: f.color + "1A" }}>
                  <LdIcon name={f.icon} size={28} color={f.color} />
                </div>
                <h3 className="font-display text-xl font-bold mb-2">{f.t}</h3>
                <p className="text-[#8A9BC0] leading-relaxed">{f.d}</p>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Alle Features */}
      <section className="max-w-6xl mx-auto px-5 py-20">
        <div className="text-center mb-12">
          <h2 className="font-display text-4xl font-extrabold mb-3">Alles drin, was du <span className="ld-gradient-text">brauchst</span></h2>
          <p className="text-[#8A9BC0] text-lg">Ein Werkzeug statt zehn — und keines davon halb.</p>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[
            { icon: Code2, color: "#4F8EF7", t: "Echter Profi-Editor im Browser",
              d: "Kein Textfeld mit Farben, sondern eine ausgewachsene Entwicklungsumgebung: Emmet, automatisches Tag-Schließen, Mehrfachcursor, Sticky Scroll, Minimap, Vollbild." },
            { icon: FolderTree, color: "#7C3AED", t: "Dateien für jede Sprache",
              d: "Ein echter Dateibaum statt drei Kästen. Anlegen, umbenennen, löschen — die Endung bestimmt Hervorhebung und Prüfung." },
            { icon: Play, color: "#10B981", t: "Ausführen, nicht simulieren",
              d: "HTML, CSS und JS laufen live in der Vorschau. Python läuft als echtes CPython über WebAssembly — mit Konsolenausgabe." },
            { icon: Save, color: "#F59E0B", t: "Speichert von selbst",
              d: "Alle 30 Sekunden, dazu Strg+S. Herunterladen als ZIP oder als eine einzige HTML-Datei. 2,5 GB pro Konto." },
            { icon: Trophy, color: "#F7C948", t: "XP, Ligen & Shop",
              d: "20 Level, Wochenligen mit Auf- und Abstieg, Tagesserie mit Schutzschild — und ein Shop, in dem XP wirklich etwas bewirken." },
            { icon: Users, color: "#0EA5E9", t: "Für Schulen & Klassen",
              d: "Lehrkräfte verwalten ihre Klasse per Schul-Code, sehen jeden Fortschritt und bauen eigene Level." },
          ].map((f, i) => (
            <Card key={i} hover className="p-6">
              <div className="w-11 h-11 rounded-lg flex items-center justify-center mb-4" style={{ background: f.color + "22" }}>
                <f.icon size={22} style={{ color: f.color }} />
              </div>
              <h3 className="font-display text-lg font-bold mb-2">{f.t}</h3>
              <p className="text-sm text-[#8A9BC0] leading-relaxed">{f.d}</p>
            </Card>
          ))}
        </div>
      </section>

      {/* IDE-Highlight */}
      <section id="ide" className="bg-[#0F1629] border-y border-[#1E2D4A] py-20">
        <div className="max-w-5xl mx-auto px-5 grid md:grid-cols-2 gap-10 items-center">
          <div>
            <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs mb-4 bg-[#4F8EF7]/15 text-[#4F8EF7]"><Code2 size={13} />Integrierte IDE</span>
            <h2 className="font-display text-3xl font-extrabold mb-4">Eine <span className="ld-gradient-text">Highend-IDE</span>, kein Spielzeug.</h2>
            <p className="text-[#8A9BC0] leading-relaxed mb-5">
              Dateibaum, Registerkarten, Emmet, automatisches Tag-Schließen, Mehrfachcursor, Vollbild —
              alles, was man zum Arbeiten braucht, ohne irgendetwas zu installieren.
            </p>
            <ul className="space-y-2 mb-6 text-[#8A9BC0] text-sm">
              {[
                "Vollwertige Entwicklungsumgebung, direkt im Browser",
                "Dateien für jede der 15 Sprachen anlegen",
                "Vorschau in Echtzeit, auch in eigenem Tab",
                "Python läuft wirklich — echtes CPython im Browser",
                "Autospeichern alle 30 Sekunden, ZIP-Download",
              ].map((x, i) => (
                <li key={i} className="flex items-start gap-2"><CheckCircle2 size={16} className="text-[#10B981] mt-0.5 shrink-0" />{x}</li>
              ))}
            </ul>
            <Btn icon={ArrowRight} onClick={() => navigate("register")}>Editor ausprobieren</Btn>
          </div>
          <Card className="p-0 overflow-hidden">
            <div className="flex items-center gap-1.5 px-3 py-2 bg-[#0A0E1A] border-b border-[#1E2D4A]">
              <span className="w-2.5 h-2.5 rounded-full bg-[#EF4444]/60" /><span className="w-2.5 h-2.5 rounded-full bg-[#F59E0B]/60" /><span className="w-2.5 h-2.5 rounded-full bg-[#10B981]/60" />
              <span className="ml-2 font-code text-[11px] text-[#4A5A7A]">index.html</span>
              <span className="ml-auto font-code text-[10px] text-[#4A5A7A]">LearnDeveloping&nbsp;Editor</span>
            </div>
            <pre className="p-4 font-code text-[12px] leading-relaxed text-[#C9D6F0] overflow-x-auto"><code>{`<h1>Hallo Welt!</h1>
<button id="btn">Klick mich</button>

<style>
  button {
    background: linear-gradient(
      135deg, #4F8EF7, #7C3AED);
    border-radius: 8px;
  }
</style>`}</code></pre>
            <div className="px-4 py-3 border-t border-[#1E2D4A] flex items-center gap-2 text-xs text-[#10B981]">
              <Eye size={13} />Vorschau aktualisiert sich beim Tippen — auch im zweiten Tab
            </div>
          </Card>
        </div>
      </section>

      {/* Rollen */}
      <section id="rollen" className="max-w-5xl mx-auto px-5 py-20">
        <div className="grid md:grid-cols-2 gap-6">
          <Card className="p-8">
            <div className="w-14 h-14 mb-4 rounded-2xl flex items-center justify-center bg-[#4F8EF7]/10">
              <GraduationCap size={28} className="text-[#4F8EF7]" />
            </div>
            <h3 className="font-display text-2xl font-bold mb-3">Für Schüler</h3>
            <ul className="space-y-2 mb-6 text-[#8A9BC0]">
              {[`Lerne ${COURSES.length} Sprachen in deinem Tempo`, "Sammle XP, Level & Abzeichen", "Sofortige Rückmeldung zu jedem Code", "Halte deine Serie am Leben"].map((x, i) => (
                <li key={i} className="flex items-start gap-2"><CheckCircle2 size={18} className="text-[#10B981] mt-0.5 shrink-0" />{x}</li>
              ))}
            </ul>
            <Btn icon={GraduationCap} onClick={() => navigate("register")}>Als Schüler starten</Btn>
          </Card>
          <Card className="p-8">
            <div className="w-14 h-14 mb-4 rounded-2xl flex items-center justify-center bg-[#10B981]/10">
              <Users size={28} className="text-[#10B981]" />
            </div>
            <h3 className="font-display text-2xl font-bold mb-3">Für Lehrer</h3>
            <ul className="space-y-2 mb-6 text-[#8A9BC0]">
              {["Eigener Schul-Code für deine Klasse", "Fortschritt aller Schüler im Blick", "Sieh XP, Level & letzte Aktivität", "Schüler in Sekunden einladen"].map((x, i) => (
                <li key={i} className="flex items-start gap-2"><CheckCircle2 size={18} className="text-[#4F8EF7] mt-0.5 shrink-0" />{x}</li>
              ))}
            </ul>
            <Btn variant="secondary" icon={Users} onClick={() => navigate("register")}>Lehrer-Account erstellen</Btn>
          </Card>
        </div>
      </section>

      {/* Alles kostenlos — ohne Preistabelle, weil es nichts zu vergleichen gibt */}
      <section id="preise" className="bg-[#0F1629] border-y border-[#1E2D4A] py-20">
        <div className="max-w-3xl mx-auto px-5 text-center">
          <h2 className="font-display text-4xl font-extrabold mb-3">Einfach <span className="ld-gradient-text">kostenlos</span></h2>
          <p className="text-[#8A9BC0] text-lg mb-8">
            Alle {COURSES.length} Sprachen, alle {TOTAL_LESSONS}+ Lektionen, die IDE und der Fortschritt —
            ohne Paywall, ohne Kreditkarte, ohne Abo.
          </p>
          <div className="flex flex-wrap justify-center gap-x-6 gap-y-2 text-sm text-[#8A9BC0] mb-8">
            {["Keine Kreditkarte", "Kein Abo", "Ohne Anmeldung testbar", "Werbefrei"].map((x, i) => (
              <span key={i} className="flex items-center gap-1.5"><CheckCircle2 size={15} className="text-[#10B981]" />{x}</span>
            ))}
          </div>
          <Btn size="lg" icon={Rocket} onClick={() => navigate("register")}>Jetzt starten</Btn>
        </div>
      </section>

      {/* So wird geprüft — der Kern des Produkts, sichtbar gemacht */}
      <section className="max-w-6xl mx-auto px-5 py-20">
        <div className="text-center mb-12">
          <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs mb-4 bg-[#10B981]/15 text-[#10B981]">
            <Zap size={13} />Ohne Wartezeit
          </span>
          <h2 className="font-display text-4xl font-extrabold mb-3">
            So sieht eine <span className="ld-gradient-text">Bewertung</span> aus
          </h2>
          <p className="text-[#8A9BC0] text-lg max-w-2xl mx-auto">
            Kein „Falsch". Sondern: welcher Baustein fehlt, welche Klammer offen ist, welches Schlüsselwort verwechselt wurde.
          </p>
        </div>

        <div className="grid lg:grid-cols-2 gap-6 items-start">
          <Card className="p-0 overflow-hidden">
            <div className="px-5 py-3 border-b border-[#1E2D4A] flex items-center gap-2">
              <PenLine size={15} className="text-[#4F8EF7]" />
              <span className="text-sm font-medium">Aufgabe</span>
              <span className="ml-auto flex items-center gap-1 text-xs text-[#F7C948]"><Star size={12} />15 XP</span>
            </div>
            <div className="p-5">
              <p className="text-sm text-[#E8EDF5] mb-4">
                Lege eine Konstante <code className="font-code text-[13px] px-1.5 py-0.5 rounded bg-[#0A0E1A] border border-[#1E2D4A] text-[#4F8EF7]">name</code> an
                und gib sie in der Konsole aus.
              </p>
              <div className="rounded-lg border border-[#1E2D4A] bg-[#0A0E1A] overflow-hidden" style={{ borderLeft: "3px solid #4F8EF7" }}>
                <div className="flex items-center gap-1.5 px-3 py-1.5 bg-[#0F1629] border-b border-[#1E2D4A]">
                  <span className="w-2.5 h-2.5 rounded-full bg-[#EF4444]/60" />
                  <span className="w-2.5 h-2.5 rounded-full bg-[#F59E0B]/60" />
                  <span className="w-2.5 h-2.5 rounded-full bg-[#10B981]/60" />
                  <span className="ml-2 font-code text-[11px] text-[#4A5A7A]">loesung.js</span>
                </div>
                <pre className="p-4 font-code text-[12.5px] leading-relaxed text-[#C9D6F0] overflow-x-auto"><code>{`let name = "Anna"
console.log(name)`}</code></pre>
              </div>
            </div>
          </Card>

          <Card className="p-5">
            <div className="flex items-center gap-2 mb-4 pb-3 border-b border-[#1E2D4A]">
              <ListChecks size={17} className="text-[#4F8EF7]" />
              <span className="font-display font-bold">Bewertung</span>
              <span className="ml-auto text-[10px] px-2 py-0.5 rounded-full bg-[#10B981]/15 text-[#10B981]">in 14 ms geprüft</span>
            </div>
            <p className="flex items-center gap-2 font-medium text-[#EF4444] mb-3">
              <XCircle size={17} />Noch nicht ganz <span className="text-sm font-normal text-[#8A9BC0]">(Score: 50/100)</span>
            </p>
            <p className="text-sm text-[#C9D6F0] mb-2">
              Du hast <code className="font-code text-[12px] text-[#F59E0B]">let</code> verwendet — die Aufgabe verlangt <code className="font-code text-[12px] text-[#10B981]">const</code>.
            </p>
            <p className="text-sm text-[#F59E0B] flex items-start gap-2 mb-4">
              <Lightbulb size={14} className="mt-0.5 shrink-0" />
              <span>Ein Wert, der sich nicht ändert, gehört in eine Konstante.</span>
            </p>
            <div className="flex flex-wrap gap-1.5">
              {[["const", false], ["name", true], ["console.log", true]].map(([c, ok]) => (
                <span key={c} className={`text-[11px] font-code px-2 py-0.5 rounded-full flex items-center gap-1 ${ok ? "bg-[#10B981]/15 text-[#10B981]" : "bg-[#EF4444]/15 text-[#EF4444]"}`}>
                  {ok ? <Check size={10} /> : <X size={10} />}{c}
                </span>
              ))}
            </div>
            <p className="text-[11px] text-[#4A5A7A] mt-4 pt-3 border-t border-[#1E2D4A] leading-relaxed">
              Diese Prüfung lief im Browser — ohne Server, ohne KI, ohne Kosten.
              Deshalb steht das Ergebnis da, bevor du die Hand von der Tastatur nimmst.
            </p>
          </Card>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-10">
          {[
            { icon: Layers, color: "#4F8EF7", t: "Zerlegen", d: "Kommentare und Zeichenketten werden abgetrennt, damit nur echter Code geprüft wird." },
            { icon: Brain, color: "#7C3AED", t: "Struktur prüfen", d: "Klammern, Blöcke und Tags müssen aufgehen — sonst ist alles Weitere hinfällig." },
            { icon: ListChecks, color: "#10B981", t: "Bausteine suchen", d: "Deklarationen, Aufrufe und Schlüsselwörter werden mit der Aufgabe abgeglichen." },
            { icon: Bug, color: "#F59E0B", t: "Stolperfallen", d: "Typische Fehler je Sprache: fehlender Doppelpunkt, == statt ===, malloc ohne free." },
          ].map((f, i) => (
            <Card key={i} className="p-5">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center mb-3" style={{ background: f.color + "1A" }}>
                <f.icon size={19} style={{ color: f.color }} />
              </div>
              <p className="font-display font-bold mb-1.5">{i + 1}. {f.t}</p>
              <p className="text-xs text-[#8A9BC0] leading-relaxed">{f.d}</p>
            </Card>
          ))}
        </div>
      </section>

      {/* Lernweg */}
      <section className="bg-[#0F1629] border-y border-[#1E2D4A] py-20">
        <div className="max-w-5xl mx-auto px-5">
          <div className="text-center mb-12">
            <h2 className="font-display text-4xl font-extrabold mb-3">Von null zur <span className="ld-gradient-text">eigenen Seite</span></h2>
            <p className="text-[#8A9BC0] text-lg">Ein Weg, der aufeinander aufbaut — statt zehn angefangener Tutorials.</p>
          </div>
          <div className="space-y-3">
            {[
              { n: "01", t: "HTML", d: "Struktur einer Seite: Überschriften, Listen, Links, Bilder, Formulare.", w: "2 Wochen", c: "#E34C26" },
              { n: "02", t: "CSS", d: "Aussehen und Layout: Box-Model, Flexbox, Grid, responsive Design.", w: "3 Wochen", c: "#264DE4" },
              { n: "03", t: "JavaScript", d: "Interaktion: Variablen, Schleifen, DOM, Events, Fetch, Async.", w: "6 Wochen", c: "#F7C948" },
              { n: "04", t: "Eigenes Projekt", d: "In der IDE bauen, testen, herunterladen — und wirklich veröffentlichen.", w: "offen", c: "#10B981" },
            ].map((s, i, arr) => (
              <div key={s.n} className="flex gap-4">
                <div className="flex flex-col items-center shrink-0">
                  <span className="w-11 h-11 rounded-xl flex items-center justify-center font-display font-bold text-sm"
                    style={{ background: s.c + "1F", color: s.c }}>{s.n}</span>
                  {i < arr.length - 1 && <span className="w-px flex-1 bg-[#1E2D4A] my-1" />}
                </div>
                <Card className="flex-1 p-5 mb-1">
                  <div className="flex flex-wrap items-center gap-3 mb-1">
                    <p className="font-display text-lg font-bold">{s.t}</p>
                    <span className="text-[11px] px-2 py-0.5 rounded-full bg-[#141D35] text-[#8A9BC0]">ca. {s.w}</span>
                  </div>
                  <p className="text-sm text-[#8A9BC0] leading-relaxed">{s.d}</p>
                </Card>
              </div>
            ))}
          </div>
          <p className="text-center text-sm text-[#4A5A7A] mt-8">
            Kannst du einen Teil schon? Der Einstufungstest überspringt ganze Module — ein Versuch je Aufgabe, alles muss stimmen.
          </p>
        </div>
      </section>

      {/* Dranbleiben */}
      <section className="max-w-6xl mx-auto px-5 py-20">
        <div className="text-center mb-12">
          <h2 className="font-display text-4xl font-extrabold mb-3">Dranbleiben, ohne sich zu <span className="ld-gradient-text">zwingen</span></h2>
          <p className="text-[#8A9BC0] text-lg max-w-2xl mx-auto">
            Programmieren lernt man in kleinen Portionen über Wochen — nicht an einem Wochenende.
            Alles hier ist darauf gebaut, morgen wiederzukommen.
          </p>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            { icon: "flamme", color: "#F59E0B", t: "Tagesserie", d: "Gezählt werden Kalendertage. Wer abends und am nächsten Morgen lernt, hat zwei." },
            { icon: "medal", color: "#F7C948", t: "Wochenliga", d: "Sechs Ligen von Bronze bis Meister. Die besten drei steigen auf, die letzten drei ab." },
            { icon: "stern", color: "#4F8EF7", t: "20 Level", d: "Vom Rookie zum Code Wizard. XP gibt es nur für wirklich gelöste Aufgaben." },
            { icon: "crown", color: "#7C3AED", t: "XP-Shop", d: "Schutzschilde, Tipp-Joker, doppelte XP — bezahlt wird mit XP, nie mit Geld." },
          ].map((f, i) => (
            <Card key={i} hover className="p-6 text-center">
              <div className="w-14 h-14 mx-auto mb-4 rounded-2xl flex items-center justify-center" style={{ background: f.color + "1A" }}>
                <LdIcon name={f.icon} size={26} color={f.color} />
              </div>
              <p className="font-display font-bold mb-1.5">{f.t}</p>
              <p className="text-xs text-[#8A9BC0] leading-relaxed">{f.d}</p>
            </Card>
          ))}
        </div>

        <Card className="p-6 mt-6">
          <div className="flex flex-col sm:flex-row items-center gap-6">
            <div className="flex items-center gap-2 shrink-0">
              {streakWeek({ streak: 5, lastActive: todayKey() }).map((d, i) => (
                <div key={i} className="text-center">
                  <div className={`w-9 h-9 rounded-xl flex items-center justify-center mb-1 ${d.active ? "bg-[#F59E0B]/20" : "bg-[#141D35]"}`}>
                    <LdIcon name="flamme" size={16} color={d.active ? "#F59E0B" : "#2A3F6F"} />
                  </div>
                  <span className="text-[10px] text-[#4A5A7A]">{d.label}</span>
                </div>
              ))}
            </div>
            <div className="flex-1 text-center sm:text-left">
              <p className="font-display font-bold mb-1">Fünf Tage am Stück</p>
              <p className="text-sm text-[#8A9BC0] leading-relaxed">
                Verpasst du einen Tag, rettet ein Schutzschild aus dem Shop deine Serie — einmal.
                Danach fängst du wieder bei eins an. Das ist der Punkt.
              </p>
            </div>
          </div>
        </Card>
      </section>

      {/* Vergleich */}
      <section className="bg-[#0F1629] border-y border-[#1E2D4A] py-20">
        <div className="max-w-4xl mx-auto px-5">
          <div className="text-center mb-12">
            <h2 className="font-display text-4xl font-extrabold mb-3">Warum nicht einfach <span className="ld-gradient-text">Videos</span>?</h2>
            <p className="text-[#8A9BC0] text-lg">Weil Zuschauen sich nach Lernen anfühlt, ohne eines zu sein.</p>
          </div>
          <Card className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[560px]">
              <thead className="border-b border-[#1E2D4A]">
                <tr className="text-[#8A9BC0]">
                  <th className="px-5 py-3 font-medium"></th>
                  <th className="px-5 py-3 font-medium">Video-Kurs</th>
                  <th className="px-5 py-3 font-medium">Buch</th>
                  <th className="px-5 py-3 font-display font-bold text-[#4F8EF7]">LearnDeveloping</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1E2D4A]">
                {[
                  ["Selbst tippen", "selten", "abtippen", "immer"],
                  ["Rückmeldung", "keine", "keine", "sofort, in Millisekunden"],
                  ["Fehler erklärt", "nein", "nein", "welcher Baustein fehlt"],
                  ["Fortschritt sichtbar", "Prozent des Videos", "Seitenzahl", "gelöste Aufgaben"],
                  ["Editor dabei", "nein", "nein", "vollwertige IDE"],
                  ["Kosten", "20–200 €", "30–60 €", "0 €"],
                ].map((row, i) => (
                  <tr key={i}>
                    <td className="px-5 py-3 text-[#E8EDF5] font-medium">{row[0]}</td>
                    <td className="px-5 py-3 text-[#4A5A7A]">{row[1]}</td>
                    <td className="px-5 py-3 text-[#4A5A7A]">{row[2]}</td>
                    <td className="px-5 py-3 text-[#10B981]">{row[3]}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
          <p className="text-center text-xs text-[#4A5A7A] mt-5 leading-relaxed">
            Videos und Bücher sind nicht schlecht — sie sind nur etwas anderes. Sie erklären.
            Hier schreibst du.
          </p>
        </div>
      </section>

      {/* Häufige Fragen */}
      <section className="max-w-3xl mx-auto px-5 py-20">
        <div className="text-center mb-10">
          <h2 className="font-display text-4xl font-extrabold mb-3">Häufige Fragen</h2>
          <p className="text-[#8A9BC0] text-lg">Kurz und ohne Marketing.</p>
        </div>
        <div className="space-y-3">
          {[
            ["Brauche ich Vorkenntnisse?", "Nein. Der HTML-Kurs beginnt bei der Frage, was ein Tag überhaupt ist. Wer schon etwas kann, überspringt Module per Einstufungstest."],
            ["Muss ich etwas installieren?", "Nein. Editor, Vorschau und Prüfung laufen im Browser. Python führt die Seite sogar selbst aus — ohne Installation."],
            ["Kostet das wirklich nichts?", "Ja. Keine Kreditkarte, kein Abo, keine Testphase, keine Werbung. Die Prüfung läuft auf deinem Gerät und kostet uns deshalb nichts."],
            ["Kann ich ohne Konto starten?", "Ja, als Gast. Dann wird allerdings nichts gespeichert — Fortschritt, Serie und Projekte sind beim Neuladen weg."],
            ["Wie werden meine Antworten geprüft?", "Von einer Analyse, die den Code zerlegt: Kommentare und Zeichenketten abtrennen, Klammern prüfen, Deklarationen und Aufrufe einsammeln, mit der Aufgabe abgleichen. Keine KI, kein Netzwerk."],
            ["Werden meine Daten weitergegeben?", "Nein. Ohne Konto verlässt nichts dein Gerät. Mit Konto liegen Name, E-Mail und Fortschritt auf dem Server — sonst nichts."],
            ["Kann ich das im Unterricht einsetzen?", "Ja. Lehrkräfte legen mit einem Schul-Code eine Klasse an, sehen jeden Fortschritt und schreiben eigene Level."],
            ["Was ist, wenn eine Aufgabe falsch bewertet wird?", "Unter jeder Bewertung gibt es einen Melden-Knopf. Mehrere Verbesserungen an der Prüfung stammen genau daher."],
          ].map(([q, a], i) => (
            <details key={i} className="group">
              <summary className="cursor-pointer list-none">
                <Card className="p-4 flex items-center gap-3">
                  <ChevronRight size={15} className="text-[#4F8EF7] shrink-0 transition-transform group-open:rotate-90" />
                  <span className="font-medium text-[#E8EDF5]">{q}</span>
                </Card>
              </summary>
              <p className="text-sm text-[#8A9BC0] leading-relaxed px-4 py-3 -mt-1">{a}</p>
            </details>
          ))}
        </div>
      </section>

      {/* Abschluss */}
      <section className="relative py-24 overflow-hidden">
        <div className="absolute inset-0" style={{ background: "radial-gradient(60% 60% at 50% 40%, rgba(79,142,247,0.10), transparent 70%)" }} />
        <div className="relative max-w-2xl mx-auto px-5 text-center">
          <h2 className="font-display text-4xl sm:text-5xl font-extrabold mb-4">
            Die erste Zeile schreibt sich <span className="ld-gradient-text">nicht von allein</span>.
          </h2>
          <p className="text-[#8A9BC0] text-lg mb-8 leading-relaxed">
            {TOTAL_LESSONS} Lektionen, {COURSES.length} Sprachen, eine vollwertige Entwicklungsumgebung.
            Kostenlos, ohne Installation, ohne Konto zum Ausprobieren.
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            <Btn size="lg" icon={Rocket} onClick={() => navigate("register")}>Jetzt kostenlos starten</Btn>
            <Btn size="lg" variant="secondary" icon={Code2} onClick={() => navigate("ide")}>Editor ansehen</Btn>
          </div>
          <p className="text-xs text-[#4A5A7A] mt-6">Keine Kreditkarte · Kein Abo · Jederzeit löschbar</p>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-[#1E2D4A] py-10">
        <div className="max-w-6xl mx-auto px-5 flex flex-col md:flex-row items-center justify-between gap-4">
          <div>
            <Logo />
            <p className="text-sm text-[#4A5A7A] mt-2">Code lernen. Richtig lernen.</p>
          </div>
          <div className="flex flex-wrap justify-center gap-x-6 gap-y-1 text-sm text-[#8A9BC0]">
            <button onClick={() => navigate("ueber-uns")} className="hover:text-[#E8EDF5]">Über uns</button>
            <button onClick={() => navigate("agb")} className="hover:text-[#E8EDF5]">AGB</button>
            <button onClick={() => navigate("datenschutz")} className="hover:text-[#E8EDF5]">Datenschutz</button>
            <button onClick={() => navigate("impressum")} className="hover:text-[#E8EDF5]">Impressum</button>
            <button onClick={() => navigate("kontakt")} className="hover:text-[#E8EDF5]">Kontakt</button>
          </div>
          <p className="text-xs text-[#4A5A7A]">© 2026 LearnDeveloping</p>
        </div>
      </footer>
    </div>
  );
}

/* ============================ Auth ================================= */
/* ---------------------- Cloudflare Turnstile (Botschutz) ------------------
   Wird nur angezeigt, wenn in index.html ein Site-Key hinterlegt ist
   (window.__TURNSTILE_SITE_KEY__). Ohne Key bleibt alles wie bisher nutzbar,
   damit die App auch lokal ohne Cloudflare läuft.
   ------------------------------------------------------------------------- */
function turnstileSiteKey() {
  try { return (typeof window !== "undefined" && window.__TURNSTILE_SITE_KEY__) || ""; } catch (e) { return ""; }
}

function Turnstile({ onVerify }) {
  const ref = useRef(null);
  const widgetId = useRef(null);
  const siteKey = turnstileSiteKey();

  useEffect(() => {
    if (!siteKey || !ref.current) return;
    let cancelled = false;
    const render = () => {
      if (cancelled || !window.turnstile || !ref.current || widgetId.current !== null) return;
      widgetId.current = window.turnstile.render(ref.current, {
        sitekey: siteKey,
        theme: "dark",
        callback: (token) => onVerify && onVerify(token),
        "expired-callback": () => onVerify && onVerify(null),
        "error-callback": () => onVerify && onVerify(null),
      });
    };
    if (window.turnstile) render();
    else {
      const timer = setInterval(() => { if (window.turnstile) { clearInterval(timer); render(); } }, 200);
      setTimeout(() => clearInterval(timer), 10000);
      return () => { cancelled = true; clearInterval(timer); };
    }
    return () => {
      cancelled = true;
      try { if (widgetId.current !== null && window.turnstile) window.turnstile.remove(widgetId.current); } catch (e) {}
      widgetId.current = null;
    };
  }, [siteKey]);

  if (!siteKey) return null;
  return (
    <div>
      <div ref={ref} />
      <p className="text-[10px] text-[#4A5A7A] mt-1.5 flex items-center gap-1"><ShieldCheck size={11} />Botschutz durch Cloudflare Turnstile</p>
    </div>
  );
}

function Field({ label, icon: Icon, ...props }) {
  return (
    <div>
      <label className="block text-sm text-[#8A9BC0] mb-1.5">{label}</label>
      <div className="relative">
        {Icon && <Icon size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#4A5A7A]" />}
        <input {...props}
          className={`w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 ${Icon ? "pl-9" : ""} text-[#E8EDF5] placeholder:text-[#4A5A7A] transition-colors`} />
      </div>
    </div>
  );
}

/* ---------------------- Passwort vergessen ------------------------------- */
// Gemeinsamer Rahmen für beide Schritte, damit sie wie die Anmeldung aussehen.
function AuthShell({ ctx, icon: Icon, iconColor, title, subtitle, children, footer }) {
  return (
    <div className="min-h-screen flex items-center justify-center px-5 py-12 relative">
      <TerminalBackground />
      <div className="relative z-10 w-full max-w-md">
        <div className="flex justify-center mb-6"><Logo size="lg" onClick={() => ctx.navigate("landing")} /></div>
        <div className="bg-[#0F1629] border border-[#1E2D4A] rounded-2xl p-8 shadow-2xl">
          <div className="w-14 h-14 rounded-2xl mx-auto mb-4 flex items-center justify-center" style={{ background: iconColor + "26" }}>
            <Icon size={28} style={{ color: iconColor }} />
          </div>
          <h2 className="font-display text-xl font-bold text-center mb-2">{title}</h2>
          <p className="text-sm text-[#8A9BC0] text-center mb-6 leading-relaxed">{subtitle}</p>
          {children}
        </div>
        {footer}
      </div>
    </div>
  );
}

function ForgotPassword({ ctx }) {
  const { navigate, pushToast, backend } = ctx;
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState(null);   // { devToken? }

  const submit = async () => {
    if (!email.trim()) { pushToast("error", "Bitte gib deine E-Mail-Adresse ein."); return; }
    setBusy(true);
    try {
      const res = await api.post("/api/auth/forgot-password", { email: email.trim() });
      setSent({ devToken: res.devResetToken });
    } catch (e) {
      pushToast("error", e.message);
    } finally {
      setBusy(false);
    }
  };

  // Ohne Server gibt es keine Konten und damit auch nichts zurückzusetzen.
  if (!backend) {
    return (
      <AuthShell ctx={ctx} icon={Info} iconColor="#F59E0B"
        title="Nur mit Server verfügbar"
        subtitle="Ohne angebundenen Server werden Konten nur in diesem Browser gehalten — ein Zurücksetzen per E-Mail gibt es dort nicht."
        footer={<button onClick={() => navigate("login")} className="mt-5 mx-auto flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]"><ChevronLeft size={15} />Zurück zur Anmeldung</button>}>
        <p className="text-xs text-[#4A5A7A] text-center leading-relaxed">
          Lösche notfalls die gespeicherten Daten deines Browsers und lege ein neues Konto an.
        </p>
      </AuthShell>
    );
  }

  if (sent) {
    return (
      <AuthShell ctx={ctx} icon={Mail} iconColor="#10B981"
        title="E-Mail unterwegs"
        subtitle="Falls ein Konto zu dieser Adresse existiert, haben wir dir einen Link zum Zurücksetzen geschickt. Er ist eine Stunde gültig."
        footer={<button onClick={() => navigate("login")} className="mt-5 mx-auto flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]"><ChevronLeft size={15} />Zurück zur Anmeldung</button>}>
        {sent.devToken && (
          <div className="mb-4">
            <p className="text-xs text-[#8A9BC0] mb-2">
              Auf diesem Server ist kein Mailversand eingerichtet — nutze diesen Code direkt:
            </p>
            <div className="p-3 rounded-lg bg-[#0A0E1A] border border-[#2A3F6F] font-code text-[11px] text-[#4F8EF7] break-all">
              {sent.devToken}
            </div>
          </div>
        )}
        <Btn className="w-full" icon={ArrowRight} onClick={() => navigate("reset-password")}>
          Weiter zum Zurücksetzen
        </Btn>
      </AuthShell>
    );
  }

  return (
    <AuthShell ctx={ctx} icon={KeyRound} iconColor="#4F8EF7"
      title="Passwort vergessen?"
      subtitle="Gib deine E-Mail-Adresse ein. Wir schicken dir einen Link, mit dem du ein neues Passwort vergeben kannst."
      footer={<button onClick={() => navigate("login")} className="mt-5 mx-auto flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]"><ChevronLeft size={15} />Zurück zur Anmeldung</button>}>
      <Field label="E-Mail" icon={AtSign} type="email" value={email} onChange={(e) => setEmail(e.target.value)}
        placeholder="du@beispiel.de" onKeyDown={(e) => { if (e.key === "Enter") submit(); }} />
      <Btn className="w-full mt-4" size="lg" disabled={busy} icon={busy ? undefined : Send} onClick={submit}>
        {busy ? <><Loader2 size={16} className="ld-spin" />Wird gesendet …</> : "Link anfordern"}
      </Btn>
    </AuthShell>
  );
}

function ResetPassword({ ctx }) {
  const { navigate, pushToast } = ctx;
  // Kommt der Nutzer über den Link aus der Mail, steht das Token in der Adresse.
  const [token, setToken] = useState(() => {
    try { return new URLSearchParams(window.location.search).get("reset") || ""; } catch (e) { return ""; }
  });
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    if (!token.trim()) { pushToast("error", "Bitte trage den Code aus der E-Mail ein."); return; }
    if (!passwordOk(password)) { pushToast("error", "Das Passwort erfüllt die Anforderungen noch nicht."); return; }
    if (password !== confirm) { pushToast("error", "Die Passwörter stimmen nicht überein."); return; }
    setBusy(true);
    try {
      await api.post("/api/auth/reset-password", { token: token.trim(), newPassword: password });
      pushToast("success", "Passwort geändert — du kannst dich jetzt anmelden.");
      // Token aus der Adresszeile entfernen, damit es nicht im Verlauf bleibt
      try { window.history.replaceState({}, "", window.location.pathname); } catch (e) {}
      navigate("login");
    } catch (e) {
      pushToast("error", e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell ctx={ctx} icon={ShieldCheck} iconColor="#10B981"
      title="Neues Passwort vergeben"
      subtitle="Trage den Code aus der E-Mail ein und wähle ein neues Passwort."
      footer={<button onClick={() => navigate("login")} className="mt-5 mx-auto flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]"><ChevronLeft size={15} />Zurück zur Anmeldung</button>}>
      <div className="space-y-4">
        <div>
          <label className="block text-sm text-[#8A9BC0] mb-1.5">Code aus der E-Mail</label>
          <input value={token} onChange={(e) => setToken(e.target.value)} placeholder="Code einfügen"
            className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 font-code text-xs text-[#E8EDF5] placeholder:text-[#4A5A7A]" />
        </div>
        <div>
          <Field label="Neues Passwort" icon={KeyRound} type="password" value={password}
            onChange={(e) => setPassword(e.target.value)} placeholder={`mindestens ${PASSWORD_MIN_LENGTH} Zeichen`} />
          <PasswordHints password={password} />
        </div>
        <Field label="Passwort bestätigen" icon={KeyRound} type="password" value={confirm}
          onChange={(e) => setConfirm(e.target.value)} placeholder="••••••••"
          onKeyDown={(e) => { if (e.key === "Enter") submit(); }} />
        <Btn className="w-full" size="lg" disabled={busy} icon={busy ? undefined : Check} onClick={submit}>
          {busy ? <><Loader2 size={16} className="ld-spin" />Wird gespeichert …</> : "Passwort speichern"}
        </Btn>
        <p className="text-[11px] text-[#4A5A7A] text-center leading-relaxed">
          Zur Sicherheit werden dabei alle bestehenden Anmeldungen beendet.
        </p>
      </div>
    </AuthShell>
  );
}

function TwoFactorLoginStep({ ctx }) {
  const { navigate, verify2FALogin, cancel2FALogin } = ctx;
  const [code, setCode] = useState("");
  return (
    <div className="min-h-screen flex items-center justify-center px-5 py-12 relative">
      <TerminalBackground />
      <div className="relative z-10 w-full max-w-md">
        <div className="flex justify-center mb-6"><Logo size="lg" onClick={() => navigate("landing")} /></div>
        <div className="bg-[#0F1629] border border-[#1E2D4A] rounded-2xl p-8 shadow-2xl text-center">
          <div className="w-14 h-14 rounded-2xl mx-auto mb-4 flex items-center justify-center bg-[#10B981]/15"><ShieldCheck size={28} className="text-[#10B981]" /></div>
          <h2 className="font-display text-xl font-bold mb-2">Zwei-Faktor-Code eingeben</h2>
          <p className="text-sm text-[#8A9BC0] mb-6">Dieser Account ist mit 2FA geschützt. Gib deinen 6-stelligen Code ein.</p>
          <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="123456" maxLength={6}
            onKeyDown={(e) => { if (e.key === "Enter") verify2FALogin(code); }}
            className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 mb-4 font-code text-center text-lg tracking-widest text-[#E8EDF5]" />
          <Btn className="w-full mb-3" icon={Check} onClick={() => verify2FALogin(code)}>Bestätigen</Btn>
          <button onClick={cancel2FALogin} className="text-sm text-[#8A9BC0] hover:text-[#E8EDF5]">Abbrechen</button>
        </div>
      </div>
    </div>
  );
}

function AuthScreen({ ctx, mode }) {
  const { navigate, login, register, pending2FA, pushToast } = ctx;
  const isLogin = mode === "login";
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "", role: "student", teacherCode: "", school: "" });
  const [captcha, setCaptcha] = useState(null);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const captchaRequired = !!turnstileSiteKey();

  const submit = () => {
    if (captchaRequired && !captcha) { pushToast("error", "Bitte bestätige zuerst, dass du kein Bot bist."); return; }
    if (isLogin) { login(form.email, form.password, captcha); return; }
    if (!passwordOk(form.password, { name: form.name, email: form.email })) {
      pushToast("error", "Bitte wähle ein stärkeres Passwort — die Anforderungen stehen unter dem Feld.");
      return;
    }
    register({ ...form, turnstileToken: captcha });
  };

  if (pending2FA) return <TwoFactorLoginStep ctx={ctx} />;

  return (
    <div className="min-h-screen flex items-center justify-center px-5 py-12 relative">
      <TerminalBackground />
      <div className="relative z-10 w-full max-w-md">
        <div className="flex justify-center mb-6"><Logo size="lg" onClick={() => navigate("landing")} /></div>
        <div className="bg-[#0F1629] border border-[#1E2D4A] rounded-2xl p-8 shadow-2xl">
          {/* Tabs */}
          <div className="flex p-1 bg-[#0A0E1A] rounded-lg mb-6">
            <button onClick={() => navigate("login")} className={`flex-1 py-2 rounded-md text-sm font-medium transition-all ${isLogin ? "text-white" : "text-[#8A9BC0]"}`} style={isLogin ? { background: GRADIENT } : undefined}>Anmelden</button>
            <button onClick={() => navigate("register")} className={`flex-1 py-2 rounded-md text-sm font-medium transition-all ${!isLogin ? "text-white" : "text-[#8A9BC0]"}`} style={!isLogin ? { background: GRADIENT } : undefined}>Registrieren</button>
          </div>

          <div className="space-y-4">
            {!isLogin && <Field label="Name" icon={User} value={form.name} onChange={set("name")} placeholder="Max Mustermann" />}
            <Field label="E-Mail" icon={AtSign} type="email" value={form.email} onChange={set("email")} placeholder="du@beispiel.de" />
            <div>
              <Field label="Passwort" icon={KeyRound} type="password" value={form.password} onChange={set("password")}
                placeholder={isLogin ? "••••••••" : `mindestens ${PASSWORD_MIN_LENGTH} Zeichen`}
                onKeyDown={(e) => { if (e.key === "Enter" && isLogin) submit(); }} />
              {!isLogin && <PasswordHints password={form.password} name={form.name} email={form.email} />}
            </div>

            {!isLogin && (
              <>
                <Field label="Passwort bestätigen" icon={KeyRound} type="password" value={form.confirm} onChange={set("confirm")} placeholder="••••••••" />
                <div>
                  <label className="block text-sm text-[#8A9BC0] mb-2">Ich bin…</label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { v: "student", icon: GraduationCap, color: "#4F8EF7", t: "Schüler", d: "Lernen mit sofortiger Rückmeldung" },
                      { v: "teacher", icon: Users, color: "#10B981", t: "Lehrer", d: "Klasse verwalten, eigene Level bauen" },
                    ].map((r) => (
                      <button key={r.v} onClick={() => setForm((f) => ({ ...f, role: r.v }))}
                        className={`text-left p-3 rounded-lg border transition-all ${form.role === r.v ? "border-[#4F8EF7] bg-[#4F8EF7]/10" : "border-[#1E2D4A] hover:border-[#2A3F6F]"}`}>
                        <div className="mb-1.5"><r.icon size={22} style={{ color: r.color }} /></div>
                        <div className="font-medium text-sm text-[#E8EDF5]">{r.t}</div>
                        <div className="text-[11px] text-[#8A9BC0]">{r.d}</div>
                      </button>
                    ))}
                  </div>
                </div>
                {form.role === "student" && <Field label="Lehrer-Code (optional)" icon={Users} value={form.teacherCode} onChange={set("teacherCode")} placeholder="z.B. LRND-4K9M" />}
                {form.role === "teacher" && <Field label="Schule / Institution" icon={GraduationCap} value={form.school} onChange={set("school")} placeholder="z.B. Gymnasium Berlin" />}
              </>
            )}

            <Turnstile onVerify={setCaptcha} />

            <Btn className="w-full" size="lg" onClick={submit} icon={isLogin ? ArrowRight : Rocket}>
              {isLogin ? "Anmelden" : "Account erstellen"}
            </Btn>

            {isLogin && (
              <button onClick={() => navigate("forgot-password")}
                className="w-full text-center text-xs text-[#8A9BC0] hover:text-[#4F8EF7]">
                Passwort vergessen?
              </button>
            )}
          </div>

          {!isLogin && (
            <p className="text-[11px] text-[#4A5A7A] mt-4 leading-relaxed">
              Mit der Registrierung akzeptierst du unsere{" "}
              <button onClick={() => navigate("agb")} className="text-[#4F8EF7] hover:underline">AGB</button> und{" "}
              <button onClick={() => navigate("datenschutz")} className="text-[#4F8EF7] hover:underline">Datenschutzerklärung</button>.
            </p>
          )}

          <div className="mt-6 pt-5 border-t border-[#1E2D4A] text-center">
            <button onClick={ctx.continueAsGuest} className="text-sm text-[#8A9BC0] hover:text-[#E8EDF5] flex items-center justify-center gap-1.5 mx-auto">
              <UserRoundPlus size={15} />Ohne Anmeldung als Gast testen
            </button>
            <p className="text-[11px] text-[#4A5A7A] mt-1">Gast-Fortschritt wird nicht gespeichert.</p>
          </div>
        </div>
        <button onClick={() => navigate("landing")} className="mt-5 mx-auto flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]">
          <ChevronLeft size={15} /> Zurück zur Startseite
        </button>
      </div>
    </div>
  );
}

/* ========================= Rechtliche Seiten ======================== */
const LEGAL_META = {
  agb: { title: "AGB", icon: Scale, subtitle: "Allgemeine Geschäftsbedingungen" },
  impressum: { title: "Impressum", icon: FileText, subtitle: "Angaben gemäß § 5 TMG" },
  datenschutz: { title: "Datenschutz", icon: ShieldCheck, subtitle: "Datenschutzerklärung" },
  kontakt: { title: "Kontakt", icon: Mail, subtitle: "So erreichst du uns" },
  "ueber-uns": { title: "Über uns", icon: Users, subtitle: "Wer wir sind" },
};

const LEGAL_CONTENT = {
  impressum: `# Impressum

Angaben gemäß § 5 TMG

## Anbieter

[Firmenname / Betreibername]
[Straße und Hausnummer]
[PLZ und Ort]
[Land]

## Kontakt

**Telefon:** [Telefonnummer]
**E-Mail:** contact@learndeveloping.com

## Vertretungsberechtigt

[Name der vertretungsberechtigten Person]

## Registereintrag

**Eintragung im:** [Handelsregister, Amtsgericht]
**Registernummer:** [HRB-Nummer]

## Umsatzsteuer-ID

Umsatzsteuer-Identifikationsnummer gemäß § 27a Umsatzsteuergesetz: [USt-IdNr.]

## Streitschlichtung

Die Europäische Kommission stellt eine Plattform zur Online-Streitbeilegung (OS) bereit: [ec.europa.eu/consumers/odr](https://ec.europa.eu/consumers/odr/). Unsere E-Mail-Adresse findest du oben im Impressum. Wir sind nicht verpflichtet und nicht bereit, an Streitbeilegungsverfahren vor einer Verbraucherschlichtungsstelle teilzunehmen.

> 💡 **Hinweis:** Dies ist eine Vorlage. Bitte die eckigen Platzhalter durch die echten Angaben des Betreibers ersetzen, bevor die Seite live geht.`,

  datenschutz: `# Datenschutzerklärung

## 1. Verantwortlicher

[Firmenname]
[Anschrift]
E-Mail: contact@learndeveloping.com

## 2. Welche Daten werden verarbeitet?

Bei der Registrierung erhebt LearnDeveloping folgende Angaben: Name, E-Mail-Adresse und — bei Schüler-Accounts optional — ein Lehrer-Code. Fortschrittsdaten wie XP, abgeschlossene Lektionen und Abzeichen werden während der Nutzung erfasst.

> 💡 **Technischer Hinweis:** Alle Daten werden ausschließlich lokal in deinem Browser gespeichert (\`localStorage\`) — es gibt keine Server-Datenbank. Wenn du als **Gast** lernst, wird gar nichts gespeichert; deine Daten verschwinden beim Schließen des Tabs.

## 3. KI-gestützte Bewertung

Bei Code-, Lückentext- und Freitextaufgaben wird deine Antwort zur Bewertung an einen KI-Dienst (Anthropic Claude) übermittelt — aber nur, wenn du selbst einen API-Key in den KI-Einstellungen hinterlegt hast. Übermittelt werden dabei die Aufgabenstellung, der Kursname und deine Antwort, direkt von deinem Browser an Anthropic. Ohne hinterlegten Key läuft eine rein lokale Prüfung, es werden keine Daten übertragen.

## 4. Zweck der Verarbeitung

- Bereitstellung des Lern-Accounts und Fortschritts-Trackings
- KI-gestützte Bewertung deiner Aufgaben (optional, mit eigenem Key)
- Anzeige in der Rangliste (Name, Avatar, XP) gegenüber anderen Nutzern derselben Plattform

## 5. Rechtsgrundlage

Die Verarbeitung erfolgt zur Erfüllung des Nutzungsvertrags (Art. 6 Abs. 1 lit. b DSGVO).

## 6. Deine Rechte

Du hast das Recht auf Auskunft, Berichtigung, Löschung, Einschränkung der Verarbeitung, Datenübertragbarkeit und Widerspruch. Da alle Daten lokal in deinem Browser liegen, kannst du sie jederzeit selbst löschen (Browser-Speicher leeren) oder dich an support@learndeveloping.com wenden.

## 7. Speicherdauer

| Datenart | Speicherort | Speicherdauer |
|---|---|---|
| Accountdaten | localStorage (dein Browser) | Bis zur Löschung |
| Fortschrittsdaten | localStorage (dein Browser) | Bis zur Löschung |
| Gast-Sitzung | Nur Arbeitsspeicher | Bis zum Schließen des Tabs |
| API-Key (optional) | localStorage (dein Browser) | Bis zur Entfernung |

> 💡 **Hinweis:** Dies ist eine Vorlage. Bitte an die tatsächliche Datenverarbeitung und Infrastruktur anpassen, bevor die Seite live geht.`,

  agb: `# Allgemeine Geschäftsbedingungen (AGB)

## § 1 Geltungsbereich

Diese AGB gelten für die Nutzung der Lernplattform LearnDeveloping ("Plattform"), betrieben von [Firmenname], für alle Schüler- und Lehrer-Accounts sowie Gast-Sitzungen.

## § 2 Vertragsschluss

Mit Abschluss der Registrierung kommt ein Nutzungsvertrag zwischen dir und [Firmenname] zustande. Die Nutzung der Basisfunktionen ist kostenlos. Eine Nutzung als Gast (ohne Registrierung) ist ohne Vertragsschluss möglich; in diesem Fall wird kein Fortschritt gespeichert.

## § 3 Leistungsbeschreibung

LearnDeveloping stellt interaktive Lerninhalte zu sieben Programmiersprachen bereit, inklusive optionaler KI-gestützter Bewertung von Aufgaben (bei hinterlegtem eigenen API-Key). Ein Anspruch auf ununterbrochene Verfügbarkeit besteht nicht.

## § 4 Pflichten der Nutzer

- Wahrheitsgemäße Angaben bei der Registrierung
- Keine missbräuchliche Nutzung des Lehrer-/Schul-Codes
- Sorgfältiger Umgang mit einem selbst hinterlegten KI-API-Key

## § 5 Schüler- und Lehrer-Accounts

Lehrer erhalten einen Schul-Code, über den sich Schüler ihrem Kurs zuordnen können. Lehrer können den Fortschritt der ihnen zugeordneten Schüler einsehen.

## § 6 Geistiges Eigentum

Alle Kursinhalte, Texte und Grafiken sind urheberrechtlich geschützt und dürfen nur im Rahmen der bestimmungsgemäßen Nutzung verwendet werden.

## § 7 Haftung

[Firmenname] haftet nur für Vorsatz und grobe Fahrlässigkeit, soweit gesetzlich zulässig. Für die Richtigkeit KI-generierter Bewertungen wird keine Gewähr übernommen.

## § 8 Kündigung

Nutzer können ihren Account jederzeit selbst löschen (lokale Daten im Browser entfernen) oder [Firmenname] um Löschung bitten.

## § 9 Änderungen der AGB

Änderungen werden den Nutzern rechtzeitig mitgeteilt. Mit fortgesetzter Nutzung nach Änderung gelten die neuen AGB als akzeptiert.

## § 10 Schlussbestimmungen

Es gilt das Recht der Bundesrepublik Deutschland. Gerichtsstand ist, soweit gesetzlich zulässig, [Ort].

> 💡 **Hinweis:** Dies ist eine Vorlage und ersetzt keine Rechtsberatung. Bitte vor dem Livegang von einer Rechtsanwältin/einem Rechtsanwalt prüfen lassen.`,

  kontakt: `# Kontakt

Wir freuen uns über dein Feedback, Fragen oder Kooperationsanfragen.

## So erreichst du uns

**E-Mail:** contact@learndeveloping.com
**Telefon:** [Telefonnummer]
**Adresse:** [Anschrift]

## Support für Schulen

Für Fragen zur Lehrer-Registrierung, Schul-Codes oder technischen Problemen wende dich an: support@learndeveloping.com

> 💡 Antwortzeit in der Regel innerhalb von 1–2 Werktagen.`,

  "ueber-uns": `# Über uns

LearnDeveloping wurde mit einem einfachen Ziel gegründet: Programmieren lernen soll **modern, verständlich und mit echtem Feedback** funktionieren — nicht mit trockenen Videos oder endlosen Dokumentationen.

## Unsere Mission

Wir glauben, dass jede:r Programmieren lernen kann, wenn die Erklärungen klar sind und Fehler sofort verständlich zurückgemeldet werden. Deshalb kombinieren wir strukturierte Kurse mit optionalem KI-gestütztem Feedback zu jeder Aufgabe.

## Für Schulen

Neben Einzellernenden richtet sich LearnDeveloping gezielt an Schulen und Lehrkräfte, die ihren Schüler:innen einen strukturierten Einstieg in die Programmierung ermöglichen möchten — inklusive Fortschritts-Überblick für die Lehrkraft.

> 💡 **Hinweis:** Dies ist eine Platzhalter-Seite für die Produktvorstellung und kann durch echte Unternehmens-/Teaminformationen ersetzt werden.`,
};

/* =========================== Cookie-Hinweis ==============================
   Ehrlich statt aufdringlich: Diese Seite setzt **keine** Werbe- oder
   Analyse-Cookies. Gespeichert wird nur, was für den Betrieb nötig ist —
   der Anmelde-Cookie und dein Fortschritt im lokalen Speicher.

   Für rein technisch notwendige Speicherung braucht es keine Einwilligung,
   wohl aber eine Information. Genau das ist dieser Hinweis: er erklärt, was
   gespeichert wird, und verschwindet auf Knopfdruck endgültig.
   ========================================================================= */
const COOKIE_NOTICE_KEY = "learndeveloping_cookie_notice";

function cookieNoticeSeen() {
  try { return localStorage.getItem(COOKIE_NOTICE_KEY) === "v1"; } catch (e) { return true; }
}

function CookieNotice({ ctx }) {
  const [open, setOpen] = useState(() => !cookieNoticeSeen());
  const [details, setDetails] = useState(false);
  if (!open) return null;

  const close = () => {
    try { localStorage.setItem(COOKIE_NOTICE_KEY, "v1"); } catch (e) {}
    setOpen(false);
  };

  const rows = [
    { name: "ld_session", art: "Cookie", zweck: "Hält dich angemeldet. Nur mit Konto und nur bei aktivem Server.", dauer: "30 Tage" },
    { name: "learndeveloping_v1", art: "Lokaler Speicher", zweck: "Dein Fortschritt, wenn kein Server genutzt wird.", dauer: "bis du ihn löschst" },
    { name: "learndeveloping_sound", art: "Lokaler Speicher", zweck: "Ob Töne ein- oder ausgeschaltet sind.", dauer: "bis du ihn löschst" },
    { name: "learndeveloping_cookie_notice", art: "Lokaler Speicher", zweck: "Merkt sich, dass du diesen Hinweis gelesen hast.", dauer: "bis du ihn löschst" },
  ];

  return (
    <div className="fixed inset-x-0 bottom-0 z-[90] p-3 sm:p-5" role="dialog" aria-label="Hinweis zu Cookies">
      <div className="max-w-3xl mx-auto bg-[#0F1629] border border-[#1E2D4A] rounded-2xl shadow-2xl overflow-hidden">
        <div className="p-5 flex flex-col sm:flex-row items-start gap-4">
          <span className="w-11 h-11 rounded-xl bg-[#4F8EF7]/10 flex items-center justify-center shrink-0">
            <ShieldCheck size={20} className="text-[#4F8EF7]" />
          </span>
          <div className="flex-1 min-w-0">
            <p className="font-display font-bold mb-1">Nur das Nötigste wird gespeichert</p>
            <p className="text-sm text-[#8A9BC0] leading-relaxed">
              Kein Tracking, keine Werbung, keine Weitergabe an Dritte. Gespeichert wird ausschließlich,
              was die Seite zum Funktionieren braucht: dein Anmeldestatus und dein Lernfortschritt.
              Ohne Konto verlässt überhaupt nichts dein Gerät.
            </p>
            <button onClick={() => setDetails((d) => !d)}
              className="text-xs text-[#4F8EF7] hover:underline mt-2 flex items-center gap-1">
              <ChevronRight size={10} className={details ? "rotate-90 transition-transform" : "transition-transform"} />
              {details ? "Details ausblenden" : "Was genau gespeichert wird"}
            </button>
          </div>
          <div className="flex flex-wrap gap-2 shrink-0">
            <Btn size="sm" variant="ghost" onClick={() => { close(); ctx.navigate("datenschutz"); }}>Datenschutz</Btn>
            <Btn size="sm" icon={Check} onClick={close}>Verstanden</Btn>
          </div>
        </div>

        {details && (
          <div className="border-t border-[#1E2D4A] overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#0A0E1A] text-[#4A5A7A]">
                <tr>
                  <th className="px-4 py-2 font-medium">Name</th>
                  <th className="px-4 py-2 font-medium">Art</th>
                  <th className="px-4 py-2 font-medium">Zweck</th>
                  <th className="px-4 py-2 font-medium">Dauer</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1E2D4A]">
                {rows.map((r) => (
                  <tr key={r.name}>
                    <td className="px-4 py-2 font-code text-[#C9D6F0] whitespace-nowrap">{r.name}</td>
                    <td className="px-4 py-2 text-[#8A9BC0] whitespace-nowrap">{r.art}</td>
                    <td className="px-4 py-2 text-[#8A9BC0]">{r.zweck}</td>
                    <td className="px-4 py-2 text-[#4A5A7A] whitespace-nowrap">{r.dauer}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="px-4 py-3 text-[11px] text-[#4A5A7A] leading-relaxed">
              Es gibt hier nichts abzulehnen: Ohne diese Einträge kannst du dich nicht anmelden und dein
              Fortschritt ginge bei jedem Neuladen verloren. Löschen kannst du alles jederzeit über die
              Einstellungen deines Browsers.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

function LegalPage({ ctx, page }) {
  const { navigate } = ctx;
  const meta = LEGAL_META[page] || LEGAL_META.impressum;
  const Icon = meta.icon;
  return (
    <div className="min-h-screen relative">
      <TerminalBackground />
      <header className="sticky top-0 z-40 backdrop-blur-md bg-[#0A0E1A]/90 border-b border-[#1E2D4A]">
        <div className="max-w-3xl mx-auto px-4 lg:px-6 h-14 flex items-center gap-3">
          <button onClick={() => navigate("landing")} className="flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]"><ArrowLeft size={16} />Zurück</button>
          <Logo size="sm" onClick={() => navigate("landing")} />
        </div>
      </header>
      <div className="relative z-10 max-w-3xl mx-auto px-5 py-10">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-11 h-11 rounded-lg flex items-center justify-center shrink-0 bg-[#4F8EF7]/15"><Icon size={22} className="text-[#4F8EF7]" /></div>
          <div>
            <h1 className="font-display text-2xl font-bold">{meta.title}</h1>
            <p className="text-sm text-[#8A9BC0]">{meta.subtitle}</p>
          </div>
        </div>
        <Card className="p-6 lg:p-8">
          <Markdown text={LEGAL_CONTENT[page] || LEGAL_CONTENT.impressum} />
        </Card>
        <div className="flex flex-wrap gap-x-5 gap-y-2 mt-6 text-sm text-[#8A9BC0]">
          {Object.keys(LEGAL_META).filter((k) => k !== page).map((k) => (
            <button key={k} onClick={() => navigate(k)} className="hover:text-[#E8EDF5] hover:underline">{LEGAL_META[k].title}</button>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ============================ App Shell =========================== */
function AppShell({ ctx, children }) {
  const { me, view, navigate, logout, sidebarOpen, setSidebarOpen, aiReady, aiConfigurable, openAiSettings, openEmailVerify, reports, soundOn, toggleSound } = ctx;
  if (!me) return null;
  const lvl = me.role === "student" ? getLevelInfo(me.xp) : null;
  const isAdmin = me.role === "admin";
  const hasAdminAccess = canAdmin(me);
  const openReports = (reports || []).filter((r) => r.status === "open").length;

  const studentNav = [
    { v: "dashboard", label: "Übersicht", icon: Home },
    { v: "courses", label: "Meine Kurse", icon: BookOpen },
    { v: "playground", label: "IDE", icon: Code2 },
    { v: "leaderboard", label: "Rangliste", icon: Trophy },
    { v: "shop", label: "XP-Shop", icon: Store },
    { v: "profile", label: "Profil", icon: User },
  ];
  const teacherNav = [
    { v: "teacher", label: "Übersicht", icon: LayoutDashboard },
    { v: "lesson-editor", label: "Eigene Level", icon: Sparkles2 },
    { v: "playground", label: "IDE", icon: Code2 },
    { v: "leaderboard", label: "Rangliste", icon: Trophy },
    { v: "profile", label: "Profil", icon: User },
  ];
  const adminNav = [
    { v: "admin", label: "Admin-Bereich", icon: Shield },
    { v: "profile", label: "Profil", icon: User },
  ];
  const baseNav = isAdmin ? adminNav : me.role === "teacher" ? teacherNav : studentNav;
  // Inhaber dieser Installation erreichen die Verwaltung zusätzlich zu ihrer Rolle.
  const nav = hasAdminAccess && !isAdmin
    ? [...baseNav.slice(0, -1), { v: "admin", label: "Verwaltung", icon: Shield }, baseNav[baseNav.length - 1]]
    : baseNav;
  const activeMatch = (v) => view === v || (v === "courses" && view === "course");

  const SidebarInner = (
    <div className="flex flex-col h-full">
      <div className="px-5 py-4 border-b border-[#1E2D4A]"><Logo /></div>
      <nav className="flex-1 px-3 py-4 space-y-1">
        {nav.map((n) => (
          <button key={n.v} onClick={() => navigate(n.v)}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all ${activeMatch(n.v) ? "bg-[#4F8EF7]/10 text-[#4F8EF7] font-medium" : "text-[#8A9BC0] hover:text-[#E8EDF5] hover:bg-white/5"}`}>
            <n.icon size={18} />{n.label}
          </button>
        ))}
      </nav>
      {me.role === "student" && (
        <div className="px-4 py-4 border-t border-[#1E2D4A] space-y-3">
          <div className="flex items-center justify-between text-sm">
            <span className="flex items-center gap-1.5 text-[#8A9BC0]"><Flame size={15} className="text-[#F59E0B]" />Streak</span>
            <span className="font-semibold text-[#E8EDF5]">{me.streak} 🔥</span>
          </div>
          <div className="flex items-center justify-between text-sm">
            <span className="flex items-center gap-1.5 text-[#8A9BC0]"><Star size={15} className="text-[#F7C948]" />XP</span>
            <span className="font-semibold text-[#F7C948]">{me.xp.toLocaleString("de-DE")}</span>
          </div>
          <ProgressBar value={lvl.pct} max={100} />
          <p className="text-[11px] text-[#4A5A7A] text-center">Level {lvl.level} · {lvl.name}</p>
        </div>
      )}
      <button onClick={logout} className="m-3 flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-[#8A9BC0] hover:text-[#EF4444] hover:bg-[#EF4444]/10 transition-all">
        <LogOut size={18} />Abmelden
      </button>
    </div>
  );

  return (
    <div className="min-h-screen flex">
      {/* Desktop sidebar */}
      <aside className="hidden lg:flex w-60 shrink-0 border-r border-[#1E2D4A] bg-[#0F1629] flex-col fixed inset-y-0">{SidebarInner}</aside>

      {/* Mobile drawer */}
      {sidebarOpen && (
        <div className="lg:hidden fixed inset-0 z-50">
          <div className="absolute inset-0 bg-black/60" onClick={() => setSidebarOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-64 bg-[#0F1629] border-r border-[#1E2D4A]">{SidebarInner}</aside>
        </div>
      )}

      <div className="flex-1 lg:ml-60 min-w-0 pb-20 lg:pb-0">
        {/* Top bar */}
        <header className="sticky top-0 z-40 backdrop-blur-md bg-[#0A0E1A]/85 border-b border-[#1E2D4A]">
          <div className="px-4 lg:px-8 h-16 flex items-center justify-between">
            <button className="lg:hidden text-[#8A9BC0]" onClick={() => setSidebarOpen(true)} aria-label="Menü öffnen"><Menu size={22} /></button>
            <div className="hidden lg:block text-sm text-[#8A9BC0] capitalize">{view === "course" ? "Kurs" : view === "teacher" ? "Lehrer-Bereich" : view}</div>
            <div className="flex items-center gap-3">
              {me.role === "student" && (
                <>
                  <button onClick={() => navigate("shop")} title={`${xpBalance(me).toLocaleString("de-DE")} XP zum Ausgeben — insgesamt ${me.xp.toLocaleString("de-DE")} verdient`}
                    className="hidden sm:flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#F7C948]/50 transition-colors">
                    <Star size={14} className="text-[#F7C948]" /><span className="font-semibold text-[#F7C948]">{xpBalance(me).toLocaleString("de-DE")}</span>
                    {boostActive(me) && <Rocket size={11} className="text-[#EF4444]" title="Doppelte XP aktiv" />}
                  </button>
                  <span className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full bg-[#141D35] border border-[#1E2D4A]"><Flame size={14} className="text-[#F59E0B]" /><span className="font-semibold">{me.streak}</span></span>
                </>
              )}
              {hasAdminAccess && openReports > 0 && (
                <button onClick={() => navigate("admin")} className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full bg-[#EF4444]/10 border border-[#EF4444]/30 text-[#EF4444]">
                  <FileText size={14} />{openReports} offene Meldung{openReports === 1 ? "" : "en"}
                </button>
              )}
              {aiConfigurable && (
                <button onClick={openAiSettings} aria-label="KI-Einstellungen" title={aiReady ? "KI verbunden" : "KI einrichten"}
                  className="relative w-10 h-10 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#2A3F6F] flex items-center justify-center transition-colors">
                  <Settings size={16} className="text-[#8A9BC0]" />
                  <span className={`absolute top-1 right-1 w-2 h-2 rounded-full ${aiReady ? "bg-[#10B981]" : "bg-[#4A5A7A]"}`} />
                </button>
              )}
              <button onClick={toggleSound} aria-label={soundOn ? "Töne ausschalten" : "Töne einschalten"}
                title={soundOn ? "Töne ausschalten" : "Töne einschalten"}
                className="w-10 h-10 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#2A3F6F] flex items-center justify-center transition-colors">
                {soundOn ? <VolumeOn size={15} className="text-[#8A9BC0]" /> : <VolumeOff size={15} className="text-[#4A5A7A]" />}
              </button>
              <button onClick={() => navigate("profile")} aria-label="Profil" className="w-10 h-10 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#2A3F6F] flex items-center justify-center transition-colors overflow-hidden"><UserAvatar user={me} size={38} /></button>
            </div>
          </div>
        </header>
        <main className={view === "playground"
          ? "px-3 lg:px-5 py-4"                      // die IDE bekommt die volle Breite
          : "px-4 lg:px-8 py-6 max-w-6xl mx-auto"}>
          {me.isGuest && (
            <div className="mb-5 flex flex-wrap items-center gap-3 px-4 py-3 rounded-xl bg-[#F59E0B]/10 border border-[#F59E0B]/30">
              <Info size={16} className="text-[#F59E0B] shrink-0" />
              <p className="text-sm text-[#C9D6F0] flex-1">Du lernst als Gast — dein Fortschritt wird <strong>nicht gespeichert</strong>.</p>
              <Btn size="sm" variant="secondary" icon={UserRoundPlus} onClick={() => navigate("register")}>Account erstellen &amp; Fortschritt sichern</Btn>
            </div>
          )}
          {!me.isGuest && me.emailVerified === false && (
            <div className="mb-5 flex flex-wrap items-center gap-3 px-4 py-3 rounded-xl bg-[#4F8EF7]/10 border border-[#4F8EF7]/30">
              <Mail size={16} className="text-[#4F8EF7] shrink-0" />
              <p className="text-sm text-[#C9D6F0] flex-1">Bitte bestätige deine E-Mail-Adresse.</p>
              <Btn size="sm" variant="secondary" icon={Check} onClick={openEmailVerify}>Jetzt bestätigen</Btn>
            </div>
          )}
          {children}
        </main>
      </div>

      {/* Mobile bottom nav */}
      <nav className="lg:hidden fixed bottom-0 inset-x-0 z-40 bg-[#0F1629] border-t border-[#1E2D4A] flex">
        {nav.map((n) => (
          <button key={n.v} onClick={() => navigate(n.v)}
            className={`flex-1 flex flex-col items-center gap-0.5 py-2.5 text-[10px] ${activeMatch(n.v) ? "text-[#4F8EF7]" : "text-[#8A9BC0]"}`}>
            <n.icon size={20} />{n.label}
          </button>
        ))}
      </nav>
    </div>
  );
}

/* ===================== Student Dashboard ========================== */
/**
 * Zeigt den letzten Login lesbar an. Ohne Server stehen dort bereits Texte
 * wie "Heute", mit Server ein Zeitstempel aus der Datenbank.
 */
function formatLastSeen(value) {
  if (!value) return "—";
  const d = new Date(value);
  if (isNaN(d)) return String(value);          // bereits ein Text
  const minutes = Math.floor((Date.now() - d.getTime()) / 60000);
  if (minutes < 1) return "Gerade eben";
  if (minutes < 60) return `Vor ${minutes} Min.`;
  if (minutes < 60 * 24 && d.toDateString() === new Date().toDateString()) return `Vor ${Math.floor(minutes / 60)} Std.`;
  const yesterday = new Date(Date.now() - 86400_000);
  if (d.toDateString() === yesterday.toDateString()) return "Gestern";
  return d.toLocaleDateString("de-DE");
}

function courseProgress(course, user) {
  const ids = allLessonsOf(course).map((l) => l.id);
  const done = ids.filter((id) => user.completedLessons.includes(id)).length;
  return { done, total: ids.length, pct: ids.length ? Math.round((done / ids.length) * 100) : 0 };
}

function StatCard({ icon: Icon, label, value, color }) {
  return (
    <Card className="p-4 flex items-center gap-3">
      <div className="w-11 h-11 rounded-lg flex items-center justify-center shrink-0" style={{ background: color + "22" }}>
        <Icon size={22} style={{ color }} />
      </div>
      <div className="min-w-0">
        <div className="font-display text-xl font-bold leading-none" style={{ color }}>{value}</div>
        <div className="text-xs text-[#8A9BC0] mt-1 truncate">{label}</div>
      </div>
    </Card>
  );
}

function CourseCard({ course, user, onOpen, locked }) {
  const p = courseProgress(course, user);
  const started = p.done > 0;
  return (
    <Card hover className="p-5 relative overflow-hidden flex flex-col">
      <div className="flex items-start justify-between mb-3">
        <CourseIcon course={course} size={40} />
        <DifficultyBadge level={course.modules[0].level} />
      </div>
      <h3 className="font-display text-lg font-bold mb-1">{course.name}</h3>
      <p className="text-sm text-[#8A9BC0] mb-4 leading-snug line-clamp-2 flex-1">{course.description}</p>
      <div className="mb-3">
        <div className="flex justify-between text-xs text-[#8A9BC0] mb-1.5"><span>{p.done} von {p.total} Lektionen</span><span>{p.pct}%</span></div>
        <ProgressBar value={p.done} max={p.total} />
      </div>
      {locked ? (
        <Btn variant="secondary" size="sm" icon={Lock} disabled>Gesperrt</Btn>
      ) : (
        <Btn variant={started ? "primary" : "secondary"} size="sm" icon={started ? Play : ArrowRight} onClick={() => onOpen(course.id)}>
          {started ? "Weitermachen" : "Starten"}
        </Btn>
      )}
      <div className="absolute bottom-0 inset-x-0 h-1" style={{ background: course.color }} />
    </Card>
  );
}

function StudentDashboard({ ctx }) {
  const { me, openCourse, navigate, openLesson } = ctx;
  const lvl = getLevelInfo(me.xp);
  const hour = new Date().getHours();
  const greet = hour < 11 ? "Guten Morgen" : hour < 18 ? "Hallo" : "Guten Abend";
  const cur = me.currentCourse ? courseById(me.currentCourse) : null;
  const curP = cur ? courseProgress(cur, me) : null;
  // nächste offene Lektion im aktuellen Kurs
  const nextLesson = cur ? allLessonsOf(cur).find((l) => !me.completedLessons.includes(l.id)) : null;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-3xl font-bold">{greet}, {me.name.split(" ")[0]}! 👋</h1>
        <p className="text-[#8A9BC0] mt-1">Bereit, heute etwas Neues zu lernen?</p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatCard icon={Star} label="Gesamt-XP" value={me.xp.toLocaleString("de-DE")} color="#F7C948" />
        <StatCard icon={Shield} label={`Level · ${lvl.name}`} value={lvl.level} color="#4F8EF7" />
        <StatCard icon={Flame} label="Tage-Streak" value={`${me.streak} 🔥`} color="#F59E0B" />
        <StatCard icon={CheckCircle2} label="Lektionen" value={me.completedLessons.length} color="#10B981" />
      </div>

      {/* Streak-Woche + Level-Fortschritt */}
      <Card className="p-5">
        <div className="flex flex-col sm:flex-row sm:items-center gap-5">
          <div className="flex-1">
            <div className="flex items-center justify-between mb-3">
              <p className="text-sm font-medium flex items-center gap-1.5"><Flame size={15} className="text-[#F59E0B]" />Deine Lern-Woche</p>
              {(me.streakFreezes || 0) > 0 && (
                <span className="text-[11px] px-2 py-0.5 rounded-full bg-[#4F8EF7]/15 text-[#4F8EF7] flex items-center gap-1"
                  title="Schützt deine Serie an einem verpassten Tag">
                  🧊 {me.streakFreezes} Schutz
                </span>
              )}
            </div>
            <div className="flex gap-2">
              {streakWeek(me).map((d, i) => (
                <div key={i} className="flex flex-col items-center gap-1.5">
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-sm ${d.active ? "" : "bg-[#1A2540] text-[#4A5A7A]"} ${d.isToday && !d.active ? "ring-1 ring-[#2A3F6F]" : ""}`}
                    style={d.active ? { background: GRADIENT } : undefined}>{d.active ? "🔥" : ""}</div>
                  <span className={`text-[10px] ${d.isToday ? "text-[#E8EDF5] font-medium" : "text-[#8A9BC0]"}`}>{d.label}</span>
                </div>
              ))}
            </div>
            {me.lastActive !== todayKey() && (
              <p className="text-[11px] text-[#8A9BC0] mt-2">Heute noch nichts gelernt — eine Lektion hält deine Serie am Leben.</p>
            )}
            {(me.streakFreezes || 0) < FREEZE_MAX && (
              <button onClick={ctx.buyStreakFreeze}
                disabled={me.xp < FREEZE_COST_XP}
                className="mt-2 text-[11px] text-[#4F8EF7] hover:underline disabled:text-[#4A5A7A] disabled:no-underline disabled:cursor-not-allowed flex items-center gap-1">
                🧊 Streak-Schutz kaufen ({FREEZE_COST_XP} XP)
                {me.xp < FREEZE_COST_XP && <span className="text-[#4A5A7A]">— noch {FREEZE_COST_XP - me.xp} XP nötig</span>}
              </button>
            )}
          </div>
          <div className="sm:w-72">
            <div className="flex justify-between text-xs mb-1.5"><span className="text-[#8A9BC0]">Bis Level {lvl.next ? lvl.next.level : lvl.level}</span><span className="text-[#F7C948]">{lvl.next ? `noch ${lvl.toNext.toLocaleString("de-DE")} XP` : "Max-Level!"}</span></div>
            <ProgressBar value={lvl.pct} max={100} height="h-3" />
          </div>
        </div>
      </Card>

      {/* Aktueller Kurs */}
      {cur && (
        <Card className="p-6 relative overflow-hidden">
          <div className="flex flex-col sm:flex-row sm:items-center gap-5">
            <CourseIcon course={cur} size={48} />
            <div className="flex-1 min-w-0">
              <p className="text-xs text-[#8A9BC0] uppercase tracking-wider mb-1">Aktueller Kurs</p>
              <h2 className="font-display text-2xl font-bold mb-2">{cur.name}</h2>
              <ProgressBar value={curP.done} max={curP.total} />
              <p className="text-sm text-[#8A9BC0] mt-1.5">{curP.done} von {curP.total} Lektionen · {curP.pct}%</p>
            </div>
            <div className="flex flex-col gap-2">
              {nextLesson
                ? <Btn icon={Play} onClick={() => openLesson(nextLesson.id)}>Weitermachen</Btn>
                : <Btn icon={Trophy} variant="secondary" onClick={() => openCourse(cur.id)}>Kurs ansehen</Btn>}
              <Btn variant="ghost" size="sm" onClick={() => openCourse(cur.id)}>Alle Lektionen</Btn>
            </div>
          </div>
        </Card>
      )}

      {/* Kurs-Grid */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-xl font-bold">Alle Kurse</h2>
          <button onClick={() => navigate("courses")} className="text-sm text-[#4F8EF7] hover:underline flex items-center gap-1">Alle ansehen <ChevronRight size={15} /></button>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {COURSES.slice(0, 6).map((c) => <CourseCard key={c.id} course={c} user={me} onOpen={openCourse} />)}
        </div>
      </div>

      {/* Letzte Aktivität */}
      <div>
        <h2 className="font-display text-xl font-bold mb-4">Letzte Aktivität</h2>
        {me.completedLessons.length === 0 ? (
          <Card className="p-10 text-center">
            <BookOpen size={40} className="mx-auto text-[#4A5A7A] mb-3" />
            <p className="text-[#8A9BC0]">Noch keine Lektionen abgeschlossen.</p>
            <div className="mt-4 flex justify-center"><Btn icon={Rocket} onClick={() => navigate("courses")}>Erste Lektion starten</Btn></div>
          </Card>
        ) : (
          <Card className="divide-y divide-[#1E2D4A]">
            {[...me.completedLessons].slice(-5).reverse().map((id) => {
              const m = findLessonMeta(id);
              if (!m) return null;
              return (
                <div key={id} className="flex items-center gap-3 p-4">
                  <CourseIcon course={m.course} size={26} />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{m.lesson.title}</p>
                    <p className="text-xs text-[#8A9BC0]">{m.course.name} · {m.module.title}</p>
                  </div>
                  <span className="flex items-center gap-1 text-xs text-[#10B981]"><CheckCircle2 size={14} />Abgeschlossen</span>
                </div>
              );
            })}
          </Card>
        )}
      </div>
    </div>
  );
}

function CoursesOverview({ ctx }) {
  const { me, openCourse } = ctx;
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("all");
  const q = query.trim().toLowerCase();
  const filtered = COURSES.filter((c) => {
    const matchesQuery = !q || c.name.toLowerCase().includes(q) || c.description.toLowerCase().includes(q);
    const matchesFilter = filter === "all" || c.modules.some((m) => m.level === filter);
    return matchesQuery && matchesFilter;
  });
  const filters = [["all", "Alle"], ["beginner", "Anfänger"], ["intermediate", "Mittel"], ["advanced", "Fortgeschritten"], ["expert", "Experte"]];
  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-bold">Meine Kurse</h1>
        <p className="text-[#8A9BC0] mt-1">Wähle eine Sprache und leg los.</p>
      </div>
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Eye size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#4A5A7A]" />
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Kurs suchen …" aria-label="Kurs suchen"
            className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 pl-9 text-sm text-[#E8EDF5] placeholder:text-[#4A5A7A] transition-colors" />
        </div>
        <div className="flex flex-wrap gap-1.5">
          {filters.map(([v, label]) => (
            <button key={v} onClick={() => setFilter(v)}
              className={`px-3 py-2 rounded-lg text-xs font-medium transition-all border ${filter === v ? "border-[#4F8EF7] bg-[#4F8EF7]/10 text-[#4F8EF7]" : "border-[#1E2D4A] text-[#8A9BC0] hover:border-[#2A3F6F]"}`}>{label}</button>
          ))}
        </div>
      </div>
      {filtered.length === 0 ? (
        <Card className="p-10 text-center">
          <Eye size={36} className="mx-auto text-[#4A5A7A] mb-3" />
          <p className="text-[#8A9BC0]">Kein Kurs gefunden. Versuch einen anderen Suchbegriff.</p>
        </Card>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((c) => <CourseCard key={c.id} course={c} user={me} onOpen={openCourse} />)}
        </div>
      )}
    </div>
  );
}

/* ========================= Course View ============================ */
function DonutProgress({ pct, color = "#4F8EF7", size = 96 }) {
  return (
    <div className="relative" style={{ width: size, height: size }}>
      <div className="absolute inset-0 rounded-full" style={{ background: `conic-gradient(${color} ${pct * 3.6}deg, #1A2540 0deg)` }} />
      <div className="absolute inset-[10px] rounded-full bg-[#141D35] flex items-center justify-center">
        <span className="font-display font-bold text-xl">{pct}%</span>
      </div>
    </div>
  );
}

function CourseView({ ctx }) {
  const { selectedCourse, me, navigate, openLesson, setPlacementTarget } = ctx;
  const course = courseById(selectedCourse);
  const [open, setOpen] = useState(() => {
    // erstes nicht-fertiges Modul offen
    const init = {};
    course.modules.forEach((m, i) => { init[m.id] = i === 0; });
    return init;
  });
  if (!course) return null;
  const p = courseProgress(course, me);

  // Modul gesperrt, wenn vorheriges Modul nicht vollständig
  const moduleUnlocked = (idx) => {
    if (idx === 0) return true;
    const prev = course.modules[idx - 1];
    return prev.lessons.every((l) => me.completedLessons.includes(l.id));
  };
  // erste offene Lektion (aktuell)
  const currentId = allLessonsOf(course).find((l) => !me.completedLessons.includes(l.id))?.id;

  return (
    <div className="space-y-6">
      <button onClick={() => navigate("courses")} className="flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]"><ChevronLeft size={16} />Zurück zu Kursen</button>

      <Card className="p-6 relative overflow-hidden">
        <div className="flex flex-col sm:flex-row items-center gap-6">
          <CourseIcon course={course} size={60} />
          <div className="flex-1 text-center sm:text-left">
            <h1 className="font-display text-3xl font-bold">{course.name}</h1>
            <p className="text-[#8A9BC0] mt-1 mb-2">{course.description}</p>
            <div className="flex items-center justify-center sm:justify-start gap-3 text-sm text-[#8A9BC0]">
              <span className="flex items-center gap-1"><BookOpen size={14} />{course.totalLessons} Lektionen</span>
              <span className="flex items-center gap-1"><Layers size={14} />{course.modules.length} Module</span>
            </div>
          </div>
          <DonutProgress pct={p.pct} color={course.color} />
        </div>
        <div className="absolute bottom-0 inset-x-0 h-1" style={{ background: course.color }} />
      </Card>

      <div className="space-y-3">
        {course.modules.map((mod, idx) => {
          const unlocked = moduleUnlocked(idx);
          const mp = mod.lessons.filter((l) => me.completedLessons.includes(l.id)).length;
          const isOpen = open[mod.id] && unlocked;
          return (
            <Card key={mod.id} className="overflow-hidden">
              <button onClick={() => unlocked && setOpen((o) => ({ ...o, [mod.id]: !o[mod.id] }))}
                className="w-full flex items-center gap-3 p-4 text-left">
                {unlocked ? (isOpen ? <ChevronDown size={18} className="text-[#8A9BC0]" /> : <ChevronRight size={18} className="text-[#8A9BC0]" />) : <Lock size={18} className="text-[#4A5A7A]" />}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-display font-bold">Modul {idx + 1}: {mod.title}</span>
                    <DifficultyBadge level={mod.level} />
                  </div>
                  {!unlocked && <p className="text-xs text-[#4A5A7A] mt-0.5 flex items-center gap-1"><Lock size={11} />Schließe Modul {idx} ab, um es freizuschalten</p>}
                </div>
                <div className="hidden sm:flex items-center gap-3 w-40">
                  <ProgressBar value={mp} max={mod.lessons.length} />
                  <span className="text-xs text-[#8A9BC0] whitespace-nowrap">{mp}/{mod.lessons.length}</span>
                </div>
              </button>
              {isOpen && mp < mod.lessons.length && (
                <div className="border-t border-[#1E2D4A] px-4 py-3 flex flex-wrap items-center gap-3 bg-[#4F8EF7]/5">
                  <Forward size={15} className="text-[#4F8EF7] shrink-0" />
                  <p className="text-xs text-[#8A9BC0] flex-1 min-w-[200px]">
                    Kannst du das schon? Bestehe den Einstufungstest und überspring das ganze Modul —
                    ein Versuch je Aufgabe, alles muss stimmen, dafür gibt es {placementXp(mod)} XP.
                  </p>
                  <Btn size="sm" variant="secondary" icon={Forward}
                    onClick={() => { setPlacementTarget({ courseId: course.id, moduleId: mod.id }); navigate("placement"); }}>
                    Test machen
                  </Btn>
                </div>
              )}
              {isOpen && (
                <div className="border-t border-[#1E2D4A] divide-y divide-[#1E2D4A]/60">
                  {mod.lessons.map((l) => {
                    const done = me.completedLessons.includes(l.id);
                    const isCurrent = l.id === currentId;
                    return (
                      <button key={l.id} onClick={() => openLesson(l.id)}
                        className="w-full flex items-center gap-3 px-4 py-3 hover:bg-white/5 transition-colors text-left">
                        {done ? <CheckCircle2 size={18} className="text-[#10B981] shrink-0" />
                          : isCurrent ? <span className="w-[18px] h-[18px] rounded-full bg-[#4F8EF7] shrink-0 flex items-center justify-center"><Play size={10} className="text-white fill-white" /></span>
                          : <span className="w-[18px] h-[18px] rounded-full border-2 border-[#2A3F6F] shrink-0" />}
                        <span className={`flex-1 text-sm ${done ? "text-[#8A9BC0]" : "text-[#E8EDF5]"} ${isCurrent ? "font-medium" : ""}`}>{l.title}{isCurrent && <span className="text-[#4F8EF7] text-xs ml-2">← Aktuell</span>}</span>
                        <span className="flex items-center gap-1 text-xs text-[#F7C948]"><Star size={12} />{l.xpReward} XP</span>
                        <ChevronRight size={15} className="text-[#4A5A7A]" />
                      </button>
                    );
                  })}
                </div>
              )}
            </Card>
          );
        })}
      </div>
    </div>
  );
}

/* ========================= Leaderboard ============================ */
function Leaderboard({ ctx }) {
  const { users, me, backend } = ctx;
  // Mit Server werden alle Lernenden verglichen, nicht nur die in diesem Browser.
  const [remote, setRemote] = useState({ entries: [], loading: !!backend });

  useEffect(() => {
    if (!backend) return;
    let alive = true;
    api.get("/api/leaderboard?limit=50")
      .then((r) => { if (alive) setRemote({ entries: r.entries || [], loading: false }); })
      .catch(() => { if (alive) setRemote({ entries: [], loading: false }); });
    return () => { alive = false; };
  }, [backend]);

  // Wochenwertung: nur wer in dieser Woche gelernt hat, taucht in der Liga auf
  const [mode, setMode] = useState("league");
  const myLeague = leagueById(me.league || "bronze");
  const all = backend ? remote.entries : users.filter((u) => u.role === "student");

  const leagueField = all
    .filter((s) => (s.league || "bronze") === myLeague.id && s.weekKey === weekKey())
    .sort((a, b) => (b.weeklyXp || 0) - (a.weeklyXp || 0));
  const allTime = [...all].sort((a, b) => b.xp - a.xp);
  const students = mode === "league" ? leagueField : allTime;
  const medal = ["🥇", "🥈", "🥉"];
  const daysLeft = daysLeftInWeek();

  if (backend && remote.loading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="font-display text-3xl font-bold flex items-center gap-2"><Trophy className="text-[#F7C948]" />Rangliste</h1>
          <p className="text-[#8A9BC0] mt-1">Die fleißigsten Lernenden der Plattform.</p>
        </div>
        <Card className="p-4 space-y-3">
          {[0, 1, 2, 3, 4].map((i) => <div key={i} className="ld-skeleton h-12" />)}
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-bold flex items-center gap-2"><Trophy className="text-[#F7C948]" />Rangliste</h1>
        <p className="text-[#8A9BC0] mt-1">Jede Woche zählt neu — sammle XP und steig auf.</p>
      </div>

      {/* Liga-Übersicht */}
      <Card className="p-5 relative overflow-hidden">
        <div className="flex flex-col sm:flex-row sm:items-center gap-5">
          <LeagueIcon league={myLeague} size={48} />
          <div className="flex-1">
            <p className="text-xs text-[#8A9BC0] uppercase tracking-wider mb-1">Deine Liga</p>
            <h2 className="font-display text-2xl font-bold" style={{ color: myLeague.color }}>{myLeague.name}</h2>
            <p className="text-sm text-[#8A9BC0] mt-1">
              {me.weekKey === weekKey() && (me.weeklyXp || 0) > 0
                ? <>Diese Woche <strong className="text-[#F7C948]">{(me.weeklyXp || 0).toLocaleString("de-DE")} XP</strong> gesammelt</>
                : "Diese Woche noch keine XP — leg los!"}
            </p>
          </div>
          <div className="text-center sm:text-right">
            <p className="text-sm text-[#E8EDF5] font-medium">Noch {daysLeft} {daysLeft === 1 ? "Tag" : "Tage"}</p>
            <p className="text-xs text-[#8A9BC0]">bis zur Wertung</p>
          </div>
        </div>
        <div className="flex items-center gap-1 mt-4 pt-4 border-t border-[#1E2D4A] overflow-x-auto">
          {LEAGUES.map((l, i) => (
            <div key={l.id} className={`flex items-center gap-1 px-2 py-1 rounded-full text-[11px] whitespace-nowrap ${l.id === myLeague.id ? "font-medium" : "opacity-40"}`}
              style={l.id === myLeague.id ? { background: l.color + "22", color: l.color } : { color: "#8A9BC0" }}>
              <LeagueIcon league={l} size={13} />{l.name}
            </div>
          ))}
        </div>
        <div className="absolute bottom-0 inset-x-0 h-1" style={{ background: myLeague.color }} />
      </Card>

      <div className="flex p-1 bg-[#0A0E1A] rounded-lg max-w-xs">
        <button onClick={() => setMode("league")}
          className={`flex-1 py-2 rounded-md text-sm font-medium transition-all ${mode === "league" ? "text-white" : "text-[#8A9BC0]"}`}
          style={mode === "league" ? { background: GRADIENT } : undefined}>Diese Woche</button>
        <button onClick={() => setMode("all")}
          className={`flex-1 py-2 rounded-md text-sm font-medium transition-all ${mode === "all" ? "text-white" : "text-[#8A9BC0]"}`}
          style={mode === "all" ? { background: GRADIENT } : undefined}>Gesamt</button>
      </div>

      {students.length === 0 ? (
        <Card className="p-10 text-center">
          <Trophy size={36} className="mx-auto text-[#4A5A7A] mb-3" />
          <p className="text-[#8A9BC0]">In dieser Woche hat in deiner Liga noch niemand XP gesammelt.</p>
          <p className="text-sm text-[#4A5A7A] mt-1">Sei die erste Person — schließ eine Lektion ab.</p>
        </Card>
      ) : (
        <Card className="divide-y divide-[#1E2D4A]">
          {students.map((s, i) => {
            const lvl = getLevelInfo(s.xp);
            const isMe = s.id === me.id;
            const promoting = mode === "league" && i < PROMOTE_TOP;
            const relegating = mode === "league" && students.length > RELEGATE_BOTTOM && i >= students.length - RELEGATE_BOTTOM;
            return (
              <div key={s.id} className={`flex items-center gap-4 p-4 ${isMe ? "bg-[#4F8EF7]/10" : ""}`}>
                <div className="w-8 text-center font-display font-bold text-lg">{i < 3 ? medal[i] : <span className="text-[#4A5A7A]">{i + 1}</span>}</div>
                <div className="w-9 h-9 rounded-lg overflow-hidden flex items-center justify-center shrink-0"><UserAvatar user={s} size={36} /></div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium truncate">
                    {s.name}{isMe && <span className="text-xs text-[#4F8EF7] ml-2">(Du)</span>}
                    {promoting && <span className="text-[10px] text-[#10B981] ml-2">↑ Aufstieg</span>}
                    {relegating && <span className="text-[10px] text-[#EF4444] ml-2">↓ Abstieg</span>}
                  </p>
                  <p className="text-xs text-[#8A9BC0]">Level {lvl.level} · {lvl.name}</p>
                </div>
                <div className="hidden sm:flex items-center gap-1 text-sm text-[#F59E0B]"><Flame size={14} />{s.streak}</div>
                <div className="flex items-center gap-1.5 font-semibold text-[#F7C948] w-24 justify-end">
                  <Star size={15} />{(mode === "league" ? (s.weeklyXp || 0) : s.xp).toLocaleString("de-DE")}
                </div>
              </div>
            );
          })}
        </Card>
      )}

      {mode === "league" && students.length > 0 && (
        <p className="text-xs text-[#4A5A7A] text-center">
          Die besten {PROMOTE_TOP} steigen auf, die letzten {RELEGATE_BOTTOM} steigen ab.
        </p>
      )}
    </div>
  );
}

/* =========================== Profile ============================== */
const AVATARS = ["🧑‍💻", "👩‍💻", "👨‍🎓", "👩‍🎓", "🦸", "🦹", "🧙", "🥷", "🤖", "👾", "🐱", "🦊", "🐼", "🦁", "🚀", "⚡"];

/* ---------------------- Avatar-Creator (Charakter-Builder) ----------------
   Ein zusammengesetzter SVG-Charakter statt eines festen Emojis. Die Auswahl
   wird als Objekt im Nutzer gespeichert (avatarConfig) und überall gerendert,
   wo bisher das Emoji stand.
   ------------------------------------------------------------------------- */
const AV_SKIN = ["#F5D0A9", "#EBBE94", "#D2A06A", "#A9714B", "#7A4B2A", "#5C3317"];
const AV_HAIR_COLOR = ["#2C1B18", "#4A2C1A", "#8B5A2B", "#D4A017", "#E8E3DB", "#4F8EF7", "#7C3AED", "#EF4444", "#10B981"];
const AV_BG = ["#4F8EF7", "#7C3AED", "#10B981", "#F59E0B", "#EF4444", "#0EA5E9", "#EC4899", "#64748B"];
const AV_HAIR_STYLES = ["kurz", "lang", "locken", "dutt", "glatze", "irokese"];
const AV_EYES = ["normal", "gluecklich", "cool", "sternchen", "zwinkern"];
const AV_ACCESSORY = ["keine", "brille", "sonnenbrille", "kopfhoerer", "muetze"];

// Erst nach dem Kauf von „Avatar-Extras“ im XP-Shop verfügbar
const AV_HAIR_STYLES_EXTRA = ["zoepfe", "afro", "undercut"];
const AV_EYES_EXTRA = ["schlafend", "herzen"];
const AV_ACCESSORY_EXTRA = ["krone", "headset", "maske"];
const AV_HAIR_COLOR_EXTRA = ["#EC4899", "#06B6D4", "#84CC16", "#F97316"];
const AV_BG_EXTRA = ["#111827", "#DB2777", "#059669", "#7C2D12"];

const DEFAULT_AVATAR_CONFIG = {
  skin: AV_SKIN[0], hairColor: AV_HAIR_COLOR[0], hairStyle: "kurz",
  eyes: "normal", accessory: "keine", bg: AV_BG[0],
};

function CharacterAvatar({ config, size = 64 }) {
  const c = { ...DEFAULT_AVATAR_CONFIG, ...(config || {}) };
  const s = size;
  return (
    <svg width={s} height={s} viewBox="0 0 100 100" style={{ display: "block", borderRadius: "22%" }} aria-hidden>
      <rect width="100" height="100" rx="22" fill={c.bg} />
      <circle cx="50" cy="96" r="30" fill={c.skin} opacity="0.95" />
      {/* Haare hinten (lang) */}
      {c.hairStyle === "lang" && <path d="M20 52 Q18 88 30 92 L70 92 Q82 88 80 52 Z" fill={c.hairColor} />}
      {/* Kopf */}
      <circle cx="50" cy="50" r="27" fill={c.skin} />
      {/* Ohren */}
      <circle cx="23" cy="52" r="5" fill={c.skin} />
      <circle cx="77" cy="52" r="5" fill={c.skin} />
      {/* Frisuren */}
      {c.hairStyle === "kurz" && <path d="M23 46 Q26 22 50 22 Q74 22 77 46 Q72 32 50 32 Q28 32 23 46 Z" fill={c.hairColor} />}
      {c.hairStyle === "lang" && <path d="M23 48 Q24 22 50 22 Q76 22 77 48 Q70 30 50 30 Q30 30 23 48 Z" fill={c.hairColor} />}
      {c.hairStyle === "locken" && (
        <g fill={c.hairColor}>
          <circle cx="32" cy="30" r="10" /><circle cx="45" cy="24" r="11" /><circle cx="58" cy="24" r="11" /><circle cx="69" cy="31" r="10" />
        </g>
      )}
      {c.hairStyle === "dutt" && (
        <g fill={c.hairColor}>
          <circle cx="50" cy="17" r="9" />
          <path d="M24 46 Q26 24 50 24 Q74 24 76 46 Q70 33 50 33 Q30 33 24 46 Z" />
        </g>
      )}
      {c.hairStyle === "irokese" && <path d="M42 24 Q50 6 58 24 Q54 18 50 18 Q46 18 42 24 Z" fill={c.hairColor} transform="scale(1.6 1) translate(-19 0)" />}
      {c.hairStyle === "zoepfe" && (
        <g fill={c.hairColor}>
          <path d="M24 46 Q26 22 50 22 Q74 22 76 46 Q70 32 50 32 Q30 32 24 46 Z" />
          <circle cx="20" cy="58" r="7" /><circle cx="18" cy="70" r="6" />
          <circle cx="80" cy="58" r="7" /><circle cx="82" cy="70" r="6" />
        </g>
      )}
      {c.hairStyle === "afro" && (
        <g fill={c.hairColor}>
          <circle cx="50" cy="30" r="24" />
          <circle cx="28" cy="40" r="12" /><circle cx="72" cy="40" r="12" />
          <circle cx="50" cy="50" r="20" fill={c.skin} />
        </g>
      )}
      {c.hairStyle === "undercut" && (
        <g fill={c.hairColor}>
          <path d="M25 40 Q28 20 50 20 Q72 20 75 40 Q66 28 50 28 Q34 28 25 40 Z" />
          <path d="M25 40 h50 v4 h-50 Z" opacity="0.5" />
        </g>
      )}
      {/* Augen */}
      {c.eyes === "normal" && <g fill="#1B2436"><circle cx="40" cy="50" r="4" /><circle cx="60" cy="50" r="4" /></g>}
      {c.eyes === "gluecklich" && <g stroke="#1B2436" strokeWidth="3" fill="none" strokeLinecap="round"><path d="M35 52 Q40 46 45 52" /><path d="M55 52 Q60 46 65 52" /></g>}
      {c.eyes === "cool" && <g fill="#1B2436"><rect x="35" y="47" width="11" height="5" rx="2" /><rect x="54" y="47" width="11" height="5" rx="2" /></g>}
      {c.eyes === "sternchen" && <g fill="#F7C948"><path d="M40 45 l1.6 4.4 4.4 1.6 -4.4 1.6 -1.6 4.4 -1.6 -4.4 -4.4 -1.6 4.4 -1.6 Z" /><path d="M60 45 l1.6 4.4 4.4 1.6 -4.4 1.6 -1.6 4.4 -1.6 -4.4 -4.4 -1.6 4.4 -1.6 Z" /></g>}
      {c.eyes === "zwinkern" && <g fill="#1B2436"><circle cx="40" cy="50" r="4" /><path d="M55 51 Q60 46 65 51" stroke="#1B2436" strokeWidth="3" fill="none" strokeLinecap="round" /></g>}
      {c.eyes === "schlafend" && (
        <g stroke="#1B2436" strokeWidth="3" fill="none" strokeLinecap="round"><path d="M35 50 Q40 55 45 50" /><path d="M55 50 Q60 55 65 50" /></g>
      )}
      {c.eyes === "herzen" && (
        <g fill="#EF4444">
          <path d="M40 46 a3.5 3.5 0 0 1 6 3 l-6 6 -6 -6 a3.5 3.5 0 0 1 6 -3 Z" transform="translate(-3 0)" />
          <path d="M60 46 a3.5 3.5 0 0 1 6 3 l-6 6 -6 -6 a3.5 3.5 0 0 1 6 -3 Z" transform="translate(-3 0)" />
        </g>
      )}
      {/* Mund */}
      <path d="M43 62 Q50 69 57 62" stroke="#1B2436" strokeWidth="3" fill="none" strokeLinecap="round" />
      {/* Accessoires */}
      {c.accessory === "brille" && (
        <g stroke="#1B2436" strokeWidth="2.5" fill="none">
          <circle cx="40" cy="50" r="9" /><circle cx="60" cy="50" r="9" /><path d="M49 50 h2" />
        </g>
      )}
      {c.accessory === "sonnenbrille" && (
        <g><rect x="30" y="44" width="18" height="11" rx="3" fill="#1B2436" /><rect x="52" y="44" width="18" height="11" rx="3" fill="#1B2436" /><path d="M48 48 h4" stroke="#1B2436" strokeWidth="3" /></g>
      )}
      {c.accessory === "kopfhoerer" && (
        <g fill="#1B2436"><path d="M22 48 a28 28 0 0 1 56 0" stroke="#1B2436" strokeWidth="5" fill="none" /><rect x="16" y="46" width="10" height="16" rx="4" /><rect x="74" y="46" width="10" height="16" rx="4" /></g>
      )}
      {c.accessory === "muetze" && (
        <g><path d="M22 40 Q26 16 50 16 Q74 16 78 40 Z" fill="#EF4444" /><rect x="18" y="38" width="64" height="7" rx="3.5" fill="#B91C1C" /></g>
      )}
      {c.accessory === "krone" && (
        <g><path d="M30 26 L36 12 L43 22 L50 8 L57 22 L64 12 L70 26 Z" fill="#F7C948" stroke="#B8860B" strokeWidth="1.5" />
          <circle cx="50" cy="16" r="2.5" fill="#EF4444" /></g>
      )}
      {c.accessory === "headset" && (
        <g>
          <path d="M22 48 a28 28 0 0 1 56 0" stroke="#1B2436" strokeWidth="5" fill="none" />
          <rect x="16" y="46" width="10" height="16" rx="4" fill="#1B2436" />
          <rect x="74" y="46" width="10" height="16" rx="4" fill="#1B2436" />
          <path d="M74 60 Q64 70 56 66" stroke="#1B2436" strokeWidth="3" fill="none" strokeLinecap="round" />
          <circle cx="55" cy="66" r="3" fill="#4F8EF7" />
        </g>
      )}
      {c.accessory === "maske" && (
        <g><rect x="32" y="56" width="36" height="18" rx="6" fill="#E8EDF5" stroke="#8A9BC0" strokeWidth="1.5" />
          <path d="M32 60 L22 54 M68 60 L78 54" stroke="#8A9BC0" strokeWidth="2" /></g>
      )}
    </svg>
  );
}

// Zeigt entweder den gebauten Charakter oder (Alt-Accounts) das Emoji.
function UserAvatar({ user, size = 40 }) {
  if (user?.avatarConfig) return <CharacterAvatar config={user.avatarConfig} size={size} />;
  return <span style={{ fontSize: size * 0.62, lineHeight: 1 }}>{user?.avatar || "🧑‍💻"}</span>;
}

function AvatarCreator({ value, onChange, extras = false }) {
  // Mit den Avatar-Extras aus dem Shop stehen mehr Frisuren, Augen,
  // Accessoires und Farben zur Auswahl.
  const hairStyles = extras ? [...AV_HAIR_STYLES, ...AV_HAIR_STYLES_EXTRA] : AV_HAIR_STYLES;
  const eyeStyles = extras ? [...AV_EYES, ...AV_EYES_EXTRA] : AV_EYES;
  const accessories = extras ? [...AV_ACCESSORY, ...AV_ACCESSORY_EXTRA] : AV_ACCESSORY;
  const hairColors = extras ? [...AV_HAIR_COLOR, ...AV_HAIR_COLOR_EXTRA] : AV_HAIR_COLOR;
  const backgrounds = extras ? [...AV_BG, ...AV_BG_EXTRA] : AV_BG;

  const cfg = { ...DEFAULT_AVATAR_CONFIG, ...(value || {}) };
  const set = (k, v) => onChange({ ...cfg, [k]: v });
  const Swatches = ({ label, colors, field }) => (
    <div>
      <p className="text-xs text-[#8A9BC0] mb-1.5">{label}</p>
      <div className="flex flex-wrap gap-1.5">
        {colors.map((col) => (
          <button key={col} onClick={() => set(field, col)} aria-label={label + " " + col}
            className={`w-7 h-7 rounded-lg border-2 transition-all ${cfg[field] === col ? "border-[#4F8EF7] scale-110" : "border-transparent"}`}
            style={{ background: col }} />
        ))}
      </div>
    </div>
  );
  const Options = ({ label, options, field, labels }) => (
    <div>
      <p className="text-xs text-[#8A9BC0] mb-1.5">{label}</p>
      <div className="flex flex-wrap gap-1.5">
        {options.map((o) => (
          <button key={o} onClick={() => set(field, o)}
            className={`px-2.5 py-1 rounded-lg text-xs border transition-all ${cfg[field] === o ? "border-[#4F8EF7] bg-[#4F8EF7]/10 text-[#4F8EF7]" : "border-[#1E2D4A] text-[#8A9BC0] hover:border-[#2A3F6F]"}`}>
            {labels?.[o] || o}
          </button>
        ))}
      </div>
    </div>
  );
  const pick = (list) => list[Math.floor(Math.random() * list.length)];
  const randomize = () => onChange({
    skin: pick(AV_SKIN), hairColor: pick(hairColors), hairStyle: pick(hairStyles),
    eyes: pick(eyeStyles), accessory: pick(accessories), bg: pick(backgrounds),
  });

  return (
    <div className="flex flex-col sm:flex-row gap-6">
      <div className="flex flex-col items-center gap-3 shrink-0">
        <CharacterAvatar config={cfg} size={120} />
        <Btn size="sm" variant="secondary" icon={Wand} onClick={randomize}>Zufällig</Btn>
      </div>
      <div className="flex-1 space-y-3">
        <Swatches label="Hautton" colors={AV_SKIN} field="skin" />
        <Options label="Frisur" options={hairStyles} field="hairStyle"
          labels={{ kurz: "Kurz", lang: "Lang", locken: "Locken", dutt: "Dutt", glatze: "Glatze", irokese: "Irokese",
                    zoepfe: "Zöpfe", afro: "Afro", undercut: "Undercut" }} />
        <Swatches label="Haarfarbe" colors={hairColors} field="hairColor" />
        <Options label="Augen" options={eyeStyles} field="eyes"
          labels={{ normal: "Normal", gluecklich: "Fröhlich", cool: "Cool", sternchen: "Sterne", zwinkern: "Zwinkern",
                    schlafend: "Verschlafen", herzen: "Herzen" }} />
        <Options label="Accessoire" options={accessories} field="accessory"
          labels={{ keine: "Keins", brille: "Brille", sonnenbrille: "Sonnenbrille", kopfhoerer: "Kopfhörer", muetze: "Mütze",
                    krone: "Krone", headset: "Headset", maske: "Maske" }} />
        <Swatches label="Hintergrund" colors={backgrounds} field="bg" />
        {!extras && (
          <p className="text-[11px] text-[#4A5A7A] pt-1">
            Mehr Frisuren, Augen und Accessoires gibt es als <span className="text-[#7C3AED]">Avatar-Extras</span> im XP-Shop.
          </p>
        )}
      </div>
    </div>
  );
}
function Profile({ ctx }) {
  const { me, pushToast, setUsers, enable2FA, disable2FA, openEmailVerify, changePassword } = ctx;
  const isStudent = me.role === "student";
  const isTeacher = me.role === "teacher";
  const lvl = isStudent ? getLevelInfo(me.xp) : null;
  const [picker, setPicker] = useState(false);
  const [disable2FAOpen, setDisable2FAOpen] = useState(false);
  const [disablePw, setDisablePw] = useState("");
  const [pwOpen, setPwOpen] = useState(false);
  const [pwCurrent, setPwCurrent] = useState("");
  const [pwNext, setPwNext] = useState("");
  const [pwConfirm, setPwConfirm] = useState("");

  const submitPassword = async () => {
    if (pwNext !== pwConfirm) { pushToast("error", "Die neuen Passwörter stimmen nicht überein."); return; }
    const ok = await changePassword(pwCurrent, pwNext);
    if (ok) { setPwOpen(false); setPwCurrent(""); setPwNext(""); setPwConfirm(""); }
  };

  const copy = (txt) => { try { navigator.clipboard.writeText(txt); } catch (e) {} pushToast("success", "In Zwischenablage kopiert!"); };
  const setAvatar = (a) => { setUsers((us) => us.map((u) => u.id === me.id ? { ...u, avatar: a, avatarConfig: null } : u)); setPicker(false); pushToast("success", "Avatar aktualisiert!"); };
  const setAvatarConfig = (conf) => setUsers((us) => us.map((u) => u.id === me.id ? { ...u, avatarConfig: conf } : u));
  const roleLabel = { student: "Schüler", teacher: "Lehrer", admin: "Administrator" }[me.role] || me.role;
  const RoleIcon = me.role === "student" ? GraduationCap : me.role === "admin" ? Shield : Users;
  return (
    <div className="space-y-6 max-w-3xl">
      <Card className="p-6">
        <div className="flex flex-col sm:flex-row items-center gap-5">
          <button onClick={() => setPicker((p) => !p)} aria-label="Avatar ändern"
            className="relative w-24 h-24 rounded-2xl bg-[#0A0E1A] border border-[#1E2D4A] hover:border-[#4F8EF7] flex items-center justify-center transition-colors group overflow-hidden">
            <UserAvatar user={me} size={94} />
            <span className="absolute bottom-1 right-1 w-6 h-6 rounded-full bg-[#141D35] border border-[#1E2D4A] flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"><PenLine size={12} className="text-[#4F8EF7]" /></span>
          </button>
          <div className="flex-1 text-center sm:text-left">
            <h1 className="font-display text-2xl font-bold">{me.name}</h1>
            <p className="text-[#8A9BC0]">{me.email}</p>
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 mt-2">
              <span className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full bg-[#4F8EF7]/15 text-[#4F8EF7]">
                <RoleIcon size={13} />{roleLabel}
              </span>
              {!me.isGuest && (
                me.emailVerified
                  ? <span className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full bg-[#10B981]/15 text-[#10B981]"><CheckCircle2 size={13} />E-Mail bestätigt</span>
                  : <button onClick={openEmailVerify} className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full bg-[#F59E0B]/15 text-[#F59E0B] hover:bg-[#F59E0B]/25"><Mail size={13} />E-Mail bestätigen</button>
              )}
              {me.twoFactorEnabled && <span className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full bg-[#7C3AED]/15 text-[#7C3AED]"><ShieldCheck size={13} />2FA aktiv</span>}
            </div>
          </div>
          {isStudent && (
            <div className="text-center">
              <DonutProgress pct={lvl.pct} />
              <p className="text-xs text-[#8A9BC0] mt-2">Level {lvl.level} · {lvl.name}</p>
            </div>
          )}
        </div>
        {picker && (
          <div className="mt-5 pt-5 border-t border-[#1E2D4A]">
            <div className="flex items-center justify-between mb-4">
              <p className="text-sm font-medium text-[#E8EDF5]">Deinen Charakter gestalten</p>
              <Btn size="sm" icon={Check} onClick={() => { setPicker(false); pushToast("success", "Avatar gespeichert!"); }}>Fertig</Btn>
            </div>
            <AvatarCreator extras={hasUnlock(me, "avatar_extras")} value={me.avatarConfig} onChange={setAvatarConfig} />
            <div className="mt-5 pt-4 border-t border-[#1E2D4A]">
              <p className="text-xs text-[#8A9BC0] mb-2">Oder ein Emoji verwenden:</p>
              <div className="flex flex-wrap gap-2">
                {AVATARS.map((a) => (
                  <button key={a} onClick={() => setAvatar(a)} aria-label={`Avatar ${a}`}
                    className={`text-xl w-10 h-10 rounded-xl flex items-center justify-center transition-all ${!me.avatarConfig && me.avatar === a ? "bg-[#4F8EF7]/15 border border-[#4F8EF7]" : "bg-[#0A0E1A] border border-[#1E2D4A] hover:border-[#2A3F6F]"}`}>{a}</button>
                ))}
              </div>
            </div>
          </div>
        )}
      </Card>

      {isStudent ? (
        <>
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <StatCard icon={Star} label="Gesamt-XP" value={me.xp.toLocaleString("de-DE")} color="#F7C948" />
            <StatCard icon={Shield} label="Level" value={lvl.level} color="#4F8EF7" />
            <StatCard icon={Flame} label="Streak" value={`${me.streak} 🔥`} color="#F59E0B" />
            <StatCard icon={CheckCircle2} label="Lektionen" value={me.completedLessons.length} color="#10B981" />
          </div>
          {lvl.next && (
            <Card className="p-5">
              <div className="flex justify-between text-sm mb-2"><span className="text-[#8A9BC0]">Fortschritt zu Level {lvl.next.level} · {lvl.next.name}</span><span className="text-[#F7C948]">noch {lvl.toNext.toLocaleString("de-DE")} XP</span></div>
              <ProgressBar value={lvl.pct} max={100} height="h-3" />
            </Card>
          )}
          <div>
            <h2 className="font-display text-xl font-bold mb-3 flex items-center gap-2"><Award className="text-[#F7C948]" size={20} />Abzeichen</h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {Object.entries(BADGES).map(([id, b]) => {
                const owned = me.badges.includes(id);
                return (
                  <Card key={id} className={`p-4 text-center ${owned ? "" : "opacity-40"}`}>
                    <div className="flex justify-center mb-2"><LdIcon name={b.icon} size={28} color={b.color} /></div>
                    <p className="text-sm font-medium">{b.label}</p>
                    <p className="text-[11px] text-[#8A9BC0]">{owned ? b.desc : "Noch nicht erreicht"}</p>
                  </Card>
                );
              })}
            </div>
          </div>
        </>
      ) : isTeacher ? (
        <div className="grid sm:grid-cols-2 gap-4">
          <StatCard icon={Users} label="Schüler" value={me.students.length} color="#4F8EF7" />
          <Card className="p-4">
            <p className="text-xs text-[#8A9BC0] mb-1">Dein Schul-Code</p>
            <button onClick={() => copy(me.schoolCode)} className="flex items-center gap-2 font-display font-bold text-xl text-[#F7C948]">{me.schoolCode}<Copy size={16} className="text-[#8A9BC0]" /></button>
            <p className="text-xs text-[#4A5A7A] mt-1">{me.school}</p>
          </Card>
        </div>
      ) : null}

      {/* Sicherheit */}
      {!me.isGuest && (
        <div>
          <h2 className="font-display text-xl font-bold mb-3 flex items-center gap-2"><ShieldCheck className="text-[#10B981]" size={20} />Sicherheit</h2>
          <Card className="divide-y divide-[#1E2D4A]">
            <div className="flex flex-wrap items-center gap-3 p-4">
              <Mail size={18} className="text-[#4F8EF7] shrink-0" />
              <div className="flex-1 min-w-[180px]">
                <p className="text-sm font-medium">E-Mail-Verifizierung</p>
                <p className="text-xs text-[#8A9BC0]">{me.emailVerified ? "Deine E-Mail-Adresse ist bestätigt." : "Bestätige deine Adresse, um deinen Account abzusichern."}</p>
              </div>
              {me.emailVerified
                ? <span className="text-xs text-[#10B981] flex items-center gap-1"><CheckCircle2 size={14} />Bestätigt</span>
                : <Btn size="sm" variant="secondary" icon={Check} onClick={openEmailVerify}>Bestätigen</Btn>}
            </div>
            <div className="flex flex-wrap items-center gap-3 p-4">
              <KeyRound size={18} className="text-[#F59E0B] shrink-0" />
              <div className="flex-1 min-w-[180px]">
                <p className="text-sm font-medium">Passwort</p>
                <p className="text-xs text-[#8A9BC0]">Ändere dein Passwort — dabei werden alle anderen Anmeldungen beendet.</p>
              </div>
              <Btn size="sm" variant="secondary" icon={PenSquare} onClick={() => setPwOpen((v) => !v)}>
                {pwOpen ? "Abbrechen" : "Ändern"}
              </Btn>
            </div>
            {pwOpen && (
              <div className="p-4 bg-[#0A0E1A] space-y-3">
                <input type="password" value={pwCurrent} onChange={(e) => setPwCurrent(e.target.value)} placeholder="Aktuelles Passwort"
                  className="w-full bg-[#141D35] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 text-sm text-[#E8EDF5]" />
                <div>
                  <input type="password" value={pwNext} onChange={(e) => setPwNext(e.target.value)} placeholder="Neues Passwort"
                    className="w-full bg-[#141D35] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 text-sm text-[#E8EDF5]" />
                  <PasswordHints password={pwNext} name={me.name} email={me.email} />
                </div>
                <input type="password" value={pwConfirm} onChange={(e) => setPwConfirm(e.target.value)} placeholder="Neues Passwort bestätigen"
                  onKeyDown={(e) => { if (e.key === "Enter") submitPassword(); }}
                  className="w-full bg-[#141D35] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 text-sm text-[#E8EDF5]" />
                <div className="flex gap-2">
                  <Btn size="sm" icon={Check} onClick={submitPassword}
                    disabled={!pwCurrent || !passwordOk(pwNext, { name: me.name, email: me.email }) || pwNext !== pwConfirm}>
                    Passwort speichern
                  </Btn>
                  <Btn size="sm" variant="ghost" onClick={() => { setPwOpen(false); setPwCurrent(""); setPwNext(""); setPwConfirm(""); }}>Abbrechen</Btn>
                </div>
              </div>
            )}
            <div className="flex flex-wrap items-center gap-3 p-4">
              <ShieldCheck size={18} className="text-[#7C3AED] shrink-0" />
              <div className="flex-1 min-w-[180px]">
                <p className="text-sm font-medium">Zwei-Faktor-Authentifizierung (2FA)</p>
                <p className="text-xs text-[#8A9BC0]">{me.twoFactorEnabled ? "Beim Login wird zusätzlich dein 2FA-Code abgefragt." : "Zusätzlicher Schutz: Login nur mit Passwort und Code."}</p>
              </div>
              {me.twoFactorEnabled
                ? <Btn size="sm" variant="danger" icon={X} onClick={() => setDisable2FAOpen(true)}>Deaktivieren</Btn>
                : <Btn size="sm" icon={ShieldCheck} onClick={enable2FA}>Aktivieren</Btn>}
            </div>
            {disable2FAOpen && (
              <div className="p-4 bg-[#0A0E1A]">
                <p className="text-xs text-[#8A9BC0] mb-2">
                  {ctx.backend ? "Bestätige mit deinem Passwort, um 2FA zu deaktivieren:" : "2FA wirklich deaktivieren?"}
                </p>
                {ctx.backend && (
                  <input type="password" value={disablePw} onChange={(e) => setDisablePw(e.target.value)} placeholder="Passwort"
                    className="w-full bg-[#141D35] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 mb-2 text-sm text-[#E8EDF5]" />
                )}
                <div className="flex gap-2">
                  <Btn size="sm" variant="danger" icon={X}
                    onClick={async () => { await disable2FA(disablePw); setDisable2FAOpen(false); setDisablePw(""); }}>
                    Deaktivieren
                  </Btn>
                  <Btn size="sm" variant="ghost" onClick={() => { setDisable2FAOpen(false); setDisablePw(""); }}>Abbrechen</Btn>
                </div>
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  );
}

/* ====================== Teacher Dashboard ========================= */
function TeacherDashboard({ ctx }) {
  const { me, users, backend, pushToast } = ctx;
  const [invite, setInvite] = useState(false);
  const [detail, setDetail] = useState(null);
  // Mit Server kommt die Klassenliste aus der Datenbank — der lokale Zustand
  // kennt nur die Konten dieses Browsers.
  const [remote, setRemote] = useState({ students: [], loading: !!backend });

  useEffect(() => {
    if (!backend) return;
    let alive = true;
    api.get("/api/teacher/students")
      .then((r) => { if (alive) setRemote({ students: r.students || [], loading: false }); })
      .catch((e) => { if (alive) { setRemote({ students: [], loading: false }); pushToast("error", e.message); } });
    return () => { alive = false; };
  }, [backend, pushToast]);

  const students = backend
    ? remote.students
    : users.filter((u) => u.role === "student" && ((me.students || []).includes(u.id) || u.teacherId === me.id));

  const avgProgress = students.length
    ? Math.round(students.reduce((acc, s) => {
        const c = s.currentCourse ? courseById(s.currentCourse) : COURSES[0];
        return acc + (c ? courseProgress(c, s).pct : 0);
      }, 0) / students.length)
    : 0;
  const activeToday = students.filter((s) => {
    if (!backend) return s.lastLogin === "Heute" || s.lastLogin === "Jetzt";
    // Der Server liefert einen Zeitstempel — heute aktiv heißt: seit Mitternacht
    if (!s.lastLogin) return false;
    const d = new Date(s.lastLogin);
    return !isNaN(d) && d.toDateString() === new Date().toDateString();
  }).length;
  const copy = (txt) => { try { navigator.clipboard.writeText(txt); } catch (e) {} pushToast("success", "Schul-Code kopiert!"); };
  const detailStudent = detail ? students.find((s) => s.id === detail) : null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="font-display text-3xl font-bold">Lehrer-Bereich</h1>
          <p className="text-[#8A9BC0] mt-1">{me.school}</p>
        </div>
        <Btn icon={Plus} onClick={() => setInvite(true)}>Schüler einladen</Btn>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatCard icon={Users} label="Schüler" value={students.length} color="#4F8EF7" />
        <StatCard icon={TrendingUp} label="Ø Fortschritt" value={`${avgProgress}%`} color="#10B981" />
        <StatCard icon={Flame} label="Heute aktiv" value={activeToday} color="#F59E0B" />
        <Card className="p-4">
          <p className="text-xs text-[#8A9BC0] mb-1">Schul-Code</p>
          <button onClick={() => copy(me.schoolCode)} className="flex items-center gap-2 font-display font-bold text-lg text-[#F7C948]">{me.schoolCode}<Copy size={15} className="text-[#8A9BC0]" /></button>
        </Card>
      </div>

      <div>
        <h2 className="font-display text-xl font-bold mb-4">Schüler</h2>
        {students.length === 0 ? (
          <Card className="p-10 text-center">
            <Users size={40} className="mx-auto text-[#4A5A7A] mb-3" />
            <p className="text-[#8A9BC0] mb-4">Noch keine Schüler. Teile deinen Code, um Schüler einzuladen.</p>
            <div className="flex justify-center"><Btn icon={Plus} onClick={() => setInvite(true)}>Schüler einladen</Btn></div>
          </Card>
        ) : (
          <Card className="overflow-x-auto">
            <table className="w-full text-sm min-w-[640px]">
              <thead>
                <tr className="text-left text-[#8A9BC0] border-b border-[#1E2D4A]">
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Kurs</th>
                  <th className="px-4 py-3 font-medium">Fortschritt</th>
                  <th className="px-4 py-3 font-medium">XP</th>
                  <th className="px-4 py-3 font-medium">Letzter Login</th>
                  <th className="px-4 py-3 font-medium"></th>
                </tr>
              </thead>
              <tbody>
                {students.map((s) => {
                  const c = s.currentCourse ? courseById(s.currentCourse) : null;
                  const p = c ? courseProgress(c, s) : { done: 0, total: 0, pct: 0 };
                  const lvl = getLevelInfo(s.xp);
                  return (
                    <tr key={s.id} className="border-b border-[#1E2D4A]/50 last:border-0 hover:bg-white/5 cursor-pointer" onClick={() => setDetail(s.id)}>
                      <td className="px-4 py-3"><div className="flex items-center gap-2"><span className="w-8 h-8 rounded-lg overflow-hidden flex items-center justify-center shrink-0"><UserAvatar user={s} size={32} /></span><div><div className="font-medium text-[#E8EDF5]">{s.name}</div><div className="text-xs text-[#4A5A7A]">Lvl {lvl.level}</div></div></div></td>
                      <td className="px-4 py-3">{c ? <span className="flex items-center gap-1.5"><CourseIcon course={c} size={15} />{c.name}</span> : <span className="text-[#4A5A7A]">—</span>}</td>
                      <td className="px-4 py-3"><div className="flex items-center gap-2 w-32"><ProgressBar value={p.done} max={p.total || 1} /><span className="text-xs text-[#8A9BC0] whitespace-nowrap">{p.pct}%</span></div></td>
                      <td className="px-4 py-3"><span className="flex items-center gap-1 text-[#F7C948]"><Star size={13} />{s.xp.toLocaleString("de-DE")}</span></td>
                      <td className="px-4 py-3 text-[#8A9BC0]">{formatLastSeen(s.lastLogin)}</td>
                      <td className="px-4 py-3 text-right"><Eye size={16} className="text-[#4A5A7A] inline" /></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </Card>
        )}
      </div>

      {/* Schüler-Detail */}
      {detailStudent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" onClick={() => setDetail(null)}>
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
          <Card className="relative z-10 p-6 max-w-md w-full" onClick={(e) => e.stopPropagation()}>
            <button onClick={() => setDetail(null)} className="absolute top-4 right-4 text-[#8A9BC0] hover:text-[#E8EDF5]"><X size={20} /></button>
            <div className="flex items-center gap-3 mb-4">
              <span className="w-12 h-12 rounded-xl overflow-hidden flex items-center justify-center shrink-0"><UserAvatar user={detailStudent} size={48} /></span>
              <div><h3 className="font-display text-xl font-bold">{detailStudent.name}</h3><p className="text-sm text-[#8A9BC0]">{detailStudent.email}</p></div>
            </div>
            <div className="grid grid-cols-3 gap-2 mb-4">
              <div className="text-center p-2 rounded-lg bg-[#0A0E1A]"><div className="text-[#F7C948] font-bold">{detailStudent.xp}</div><div className="text-[10px] text-[#8A9BC0]">XP</div></div>
              <div className="text-center p-2 rounded-lg bg-[#0A0E1A]"><div className="text-[#4F8EF7] font-bold">{getLevelInfo(detailStudent.xp).level}</div><div className="text-[10px] text-[#8A9BC0]">Level</div></div>
              <div className="text-center p-2 rounded-lg bg-[#0A0E1A]"><div className="text-[#F59E0B] font-bold">{detailStudent.streak} 🔥</div><div className="text-[10px] text-[#8A9BC0]">Streak</div></div>
            </div>
            <p className="text-sm text-[#8A9BC0] mb-2">Fortschritt pro Kurs:</p>
            <div className="space-y-2 max-h-52 overflow-y-auto">
              {COURSES.map((c) => {
                const p = courseProgress(c, detailStudent);
                if (p.done === 0) return null;
                return (
                  <div key={c.id} className="flex items-center gap-2">
                    <span className="w-6 flex justify-center"><CourseIcon course={c} size={18} /></span>
                    <span className="text-sm w-24 truncate">{c.name}</span>
                    <ProgressBar value={p.done} max={p.total} />
                    <span className="text-xs text-[#8A9BC0] w-12 text-right">{p.done}/{p.total}</span>
                  </div>
                );
              })}
              {COURSES.every((c) => courseProgress(c, detailStudent).done === 0) && <p className="text-sm text-[#4A5A7A]">Noch keine Lektionen abgeschlossen.</p>}
            </div>
          </Card>
        </div>
      )}

      {/* Einladen-Modal */}
      {invite && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" onClick={() => setInvite(false)}>
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
          <Card className="relative z-10 p-7 max-w-sm w-full text-center" onClick={(e) => e.stopPropagation()}>
            <button onClick={() => setInvite(false)} className="absolute top-4 right-4 text-[#8A9BC0] hover:text-[#E8EDF5]"><X size={20} /></button>
            <Users size={36} className="mx-auto text-[#4F8EF7] mb-3" />
            <h3 className="font-display text-xl font-bold mb-2">Schüler einladen</h3>
            <p className="text-sm text-[#8A9BC0] mb-5">Deine Schüler geben diesen Code bei der Registrierung ein:</p>
            <div className="p-4 rounded-xl bg-[#0A0E1A] border border-[#2A3F6F] mb-4">
              <span className="font-display font-black text-3xl ld-gradient-text">{me.schoolCode}</span>
            </div>
            <Btn className="w-full" icon={Copy} onClick={() => copy(me.schoolCode)}>Code kopieren</Btn>
          </Card>
        </div>
      )}
    </div>
  );
}

/* ========================= Code-Playground ========================= */
function formatBytes(bytes) {
  if (!bytes) return "0 KB";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(2) + " MB";
}

/**
 * Baut aus den drei Bereichen eine fertige Seite.
 *
 * Emmets `!` erzeugt ein vollständiges HTML-Gerüst. Steht im HTML-Bereich
 * bereits ein ganzes Dokument, wird CSS und JS dort hineingesetzt statt es
 * ein zweites Mal einzupacken — sonst entstünde verschachteltes HTML.
 */
function composeDocument({ html = "", css = "", js = "", title = "Meine Seite", extraScript = "" }) {
  const styleTag = css.trim() ? `<style>\n${css}\n</style>` : "";
  const scriptTag = js.trim() ? `<script>\n${js}\n<\/script>` : "";
  const bridgeTag = extraScript ? `<script>${extraScript}<\/script>` : "";
  const source = String(html);

  const isFullDocument = /<html[\s>]/i.test(source) || /<!doctype\s+html/i.test(source);
  if (isFullDocument) {
    let out = source;
    // Styles ans Ende des Kopfbereichs, Skripte ans Ende des Körpers
    if (/<\/head>/i.test(out)) out = out.replace(/<\/head>/i, `${styleTag}\n</head>`);
    else if (/<html[^>]*>/i.test(out)) out = out.replace(/(<html[^>]*>)/i, `$1\n<head>${styleTag}</head>`);
    else out = styleTag + out;

    if (/<\/body>/i.test(out)) out = out.replace(/<\/body>/i, `${bridgeTag}\n${scriptTag}\n</body>`);
    else out += `\n${bridgeTag}\n${scriptTag}`;
    return out;
  }

  return `<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${String(title).replace(/[<>]/g, "")}</title>
${styleTag}
</head>
<body>
${source}
${bridgeTag}
${scriptTag}
</body>
</html>`;
}

/* ========================= Dateien im Projekt =============================
   Die IDE arbeitet nicht mehr mit drei festen Bereichen, sondern mit einem
   echten Dateibaum. Jede Datei bringt ihre Sprache über die Endung mit —
   Der Editor bekommt daraus Syntaxhervorhebung, Faltung und Autovervollständigung.
   ========================================================================= */
const FILE_TYPES = [
  { ext: "html", lang: "html",       label: "HTML",        color: "#E34C26", runs: true },
  { ext: "css",  lang: "css",        label: "CSS",         color: "#264DE4", runs: true },
  { ext: "js",   lang: "javascript", label: "JavaScript",  color: "#F7C948", runs: true },
  { ext: "jsx",  lang: "javascript", label: "React (JSX)", color: "#61DAFB" },
  { ext: "ts",   lang: "typescript", label: "TypeScript",  color: "#3178C6" },
  { ext: "tsx",  lang: "typescript", label: "React (TSX)", color: "#3178C6" },
  { ext: "vue",  lang: "html",       label: "Vue",         color: "#42B883" },
  { ext: "py",   lang: "python",     label: "Python",      color: "#3572A5" },
  { ext: "java", lang: "java",       label: "Java",        color: "#B07219" },
  { ext: "kt",   lang: "kotlin",     label: "Kotlin",      color: "#7F52FF" },
  { ext: "c",    lang: "c",          label: "C",           color: "#5C6BC0" },
  { ext: "h",    lang: "c",          label: "C-Header",    color: "#5C6BC0" },
  { ext: "cpp",  lang: "cpp",        label: "C++",         color: "#00599C" },
  { ext: "hpp",  lang: "cpp",        label: "C++-Header",  color: "#00599C" },
  { ext: "cs",   lang: "csharp",     label: "C#",          color: "#178600" },
  { ext: "go",   lang: "go",         label: "Go",          color: "#00ADD8" },
  { ext: "rs",   lang: "rust",       label: "Rust",        color: "#DEA584" },
  { ext: "php",  lang: "php",        label: "PHP",         color: "#777BB4" },
  { ext: "rb",   lang: "ruby",       label: "Ruby",        color: "#CC342D" },
  { ext: "swift", lang: "swift",     label: "Swift",       color: "#F05138" },
  { ext: "sql",  lang: "sql",        label: "SQL",         color: "#E38C00" },
  { ext: "sh",   lang: "shell",      label: "Shell",       color: "#89E051" },
  { ext: "json", lang: "json",       label: "JSON",        color: "#8A9BC0" },
  { ext: "yml",  lang: "yaml",       label: "YAML",        color: "#8A9BC0" },
  { ext: "xml",  lang: "xml",        label: "XML",         color: "#8A9BC0" },
  { ext: "md",   lang: "markdown",   label: "Markdown",    color: "#8A9BC0" },
  { ext: "txt",  lang: "plaintext",  label: "Text",        color: "#4A5A7A" },
];

const FALLBACK_TYPE = { ext: "txt", lang: "plaintext", label: "Text", color: "#4A5A7A" };

function extOf(name) {
  const m = /\.([A-Za-z0-9]+)$/.exec(String(name || ""));
  return m ? m[1].toLowerCase() : "";
}
function fileTypeOf(name) {
  return FILE_TYPES.find((t) => t.ext === extOf(name)) || FALLBACK_TYPE;
}
function langOf(name) { return fileTypeOf(name).lang; }

/** Läuft diese Datei im Browser? Alles andere lässt sich schreiben und
 *  herunterladen, aber nicht ausführen — das sagen wir offen. */
function runsInBrowser(name) { return !!fileTypeOf(name).runs; }

const newFileId = () => "f_" + Math.random().toString(36).slice(2, 9);

/** Sorgt für einen eindeutigen Dateinamen im Projekt. */
function uniqueFileName(files, wanted) {
  const base = String(wanted || "").trim() || "unbenannt.txt";
  if (!files.some((f) => f.name.toLowerCase() === base.toLowerCase())) return base;
  const dot = base.lastIndexOf(".");
  const stem = dot > 0 ? base.slice(0, dot) : base;
  const ext = dot > 0 ? base.slice(dot) : "";
  for (let i = 2; i < 999; i++) {
    const candidate = `${stem}-${i}${ext}`;
    if (!files.some((f) => f.name.toLowerCase() === candidate.toLowerCase())) return candidate;
  }
  return `${stem}-${Date.now()}${ext}`;
}

/** Prüft einen Dateinamen. Gibt null zurück, wenn alles in Ordnung ist. */
function fileNameError(name, files, exceptId) {
  const trimmed = String(name || "").trim();
  if (!trimmed) return "Bitte gib einen Dateinamen ein.";
  if (trimmed.length > 60) return "Der Name ist zu lang (höchstens 60 Zeichen).";
  if (/[\\/:*?"<>|]/.test(trimmed)) return "Diese Zeichen sind im Dateinamen nicht erlaubt: \\ / : * ? \" < > |";
  if (files.some((f) => f.id !== exceptId && f.name.toLowerCase() === trimmed.toLowerCase())) {
    return "Eine Datei mit diesem Namen gibt es schon.";
  }
  return null;
}

/** Bringt gespeicherte Projekte auf die Dateistruktur — auch die alten,
 *  die noch aus genau drei Feldern (html/css/js) bestanden. */
function normalizeProject(project) {
  if (!project) return null;
  if (Array.isArray(project.files)) {
    return {
      ...project,
      files: project.files
        .filter((f) => f && typeof f.name === "string" && f.name.trim())
        .map((f) => ({ id: f.id || newFileId(), name: f.name, content: String(f.content ?? "") })),
    };
  }
  const legacy = [];
  if (project.html) legacy.push({ id: newFileId(), name: "index.html", content: project.html });
  if (project.css) legacy.push({ id: newFileId(), name: "style.css", content: project.css });
  if (project.js) legacy.push({ id: newFileId(), name: "script.js", content: project.js });
  return { ...project, files: legacy };
}

function projectBytes(files) {
  try {
    return new Blob(files.map((f) => f.name + (f.content || ""))).size;
  } catch (e) {
    return files.reduce((sum, f) => sum + (f.name.length + (f.content || "").length), 0);
  }
}

/* ---------------------- Aus Dateien eine Seite bauen ---------------------
   Die Vorschau löst Verweise innerhalb des Projekts auf: aus
   `<link href="style.css">` wird der Inhalt der Datei, aus
   `<script src="app.js">` ebenso. Damit verhält sich die Vorschau wie ein
   echter Webserver, ohne dass es einen gibt.
   ------------------------------------------------------------------------- */
function findFile(files, name) {
  const clean = String(name || "").replace(/^\.?\//, "").split(/[?#]/)[0].toLowerCase();
  return files.find((f) => f.name.toLowerCase() === clean) || null;
}

/** Die Datei, mit der die Vorschau startet. */
function entryFile(files) {
  return findFile(files, "index.html") || files.find((f) => extOf(f.name) === "html") || null;
}

function buildProjectPage(files, { title = "Meine Seite", extraScript = "", autoInclude = true } = {}) {
  const entry = entryFile(files);
  if (!entry) return null;

  let html = String(entry.content || "");
  const usedCss = new Set();
  const usedJs = new Set();

  // <link rel="stylesheet" href="…"> durch den echten Inhalt ersetzen
  html = html.replace(/<link\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>/gi, (tag, href) => {
    if (!/stylesheet/i.test(tag) && !/\.css($|[?#])/i.test(href)) return tag;
    const file = findFile(files, href);
    if (!file) return tag;                                   // externe URL unangetastet lassen
    usedCss.add(file.id);
    return `<style data-from="${file.name}">\n${file.content}\n</style>`;
  });

  // <script src="…"> ebenso
  html = html.replace(/<script\b([^>]*)\ssrc\s*=\s*["']([^"']+)["']([^>]*)><\/script>/gi, (tag, pre, src) => {
    const file = findFile(files, src);
    if (!file) return tag;
    usedJs.add(file.id);
    return `<script data-from="${file.name}">\n${file.content}\n<\/script>`;
  });

  // Nicht verlinkte CSS-/JS-Dateien optional automatisch einbinden. Für den
  // Einstieg ist das bequem; wer es selbst verlinkt, merkt keinen Unterschied.
  const extraCss = autoInclude
    ? files.filter((f) => extOf(f.name) === "css" && !usedCss.has(f.id)).map((f) => f.content).join("\n\n")
    : "";
  const extraJs = autoInclude
    ? files.filter((f) => extOf(f.name) === "js" && !usedJs.has(f.id)).map((f) => f.content).join("\n\n")
    : "";

  return composeDocument({ html, css: extraCss, js: extraJs, title, extraScript });
}

/* ------------------- KI-Assistent im Code-Editor -------------------------
   Der einzige Ort, an dem eine KI zum Einsatz kommt. Hier ist eine Antwortzeit
   von ein paar Sekunden unproblematisch — anders als bei der Aufgabenprüfung,
   die deshalb rein lokal läuft.
   ------------------------------------------------------------------------- */
const ASSISTANT_QUICK_ACTIONS = [
  { label: "Code erklären", icon: BookOpen, prompt: "Erkläre mir kurz, was mein Code macht." },
  { label: "Fehler finden", icon: Bug, prompt: "Finde Fehler in meinem Code und erkläre sie." },
  { label: "Verbessern", icon: Sparkles, prompt: "Wie kann ich meinen Code verbessern? Gib mir 2-3 konkrete Vorschläge." },
];

/** Rendert eine Assistenten-Antwort mit Codeblöcken. */
function AssistantMessage({ content }) {
  const parts = String(content).split(/```(\w*)\n?([\s\S]*?)```/g);
  const out = [];
  for (let i = 0; i < parts.length; i += 3) {
    const text = parts[i];
    if (text?.trim()) {
      out.push(
        <div key={`t${i}`} className="whitespace-pre-wrap leading-relaxed">
          {text.split("\n").map((line, li) => <div key={li}>{renderInline(line, `a${i}_${li}`)}</div>)}
        </div>
      );
    }
    const lang = parts[i + 1];
    const code = parts[i + 2];
    if (code != null) out.push(<CodeBlock key={`c${i}`} code={code.replace(/\n$/, "")} lang={lang || "code"} />);
  }
  return <div className="space-y-1">{out}</div>;
}

function AssistantPanel({ ctx, code }) {
  const { aiConfig, aiReady, openAiSettings, pushToast } = ctx;
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const scrollRef = useRef(null);

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, busy]);

  const send = async (text) => {
    const question = String(text ?? input).trim();
    if (!question || busy) return;
    const next = [...messages, { role: "user", content: question }];
    setMessages(next);
    setInput("");
    setBusy(true);
    try {
      const reply = await askAssistant(next, code, aiConfig);
      setMessages([...next, { role: "assistant", content: reply }]);
    } catch (e) {
      setMessages([...next, {
        role: "assistant", error: true,
        content: `Die KI ist gerade nicht erreichbar (${e.message}). Prüfe deine Einstellungen — die Aufgabenprüfung funktioniert davon unabhängig weiter.`,
      }]);
    } finally {
      setBusy(false);
    }
  };

  if (!aiReady) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-center p-6 gap-3">
        <Bot size={32} className="text-[#4A5A7A]" />
        <p className="text-sm text-[#8A9BC0]">Der KI-Assistent ist noch nicht verfügbar.</p>
        <p className="text-xs text-[#4A5A7A] max-w-xs leading-relaxed">
          Er kann deinen Code erklären, Fehler finden und Verbesserungen vorschlagen.
          {ctx.aiConfigurable
            ? " Richte dafür einen Anbieter ein."
            : " Die Zugänge richtet die Administration ein — die Fehlerprüfung im Editor läuft davon unabhängig."}
        </p>
        {ctx.aiConfigurable && <Btn size="sm" icon={Settings} onClick={openAiSettings}>KI einrichten</Btn>}
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      <div ref={scrollRef} className="flex-1 overflow-y-auto p-3 space-y-3 text-[13px]">
        {messages.length === 0 && (
          <div className="text-center py-4">
            <Bot size={26} className="text-[#7C3AED] mx-auto mb-2" />
            <p className="text-sm text-[#C9D6F0] mb-1">Frag mich zu deinem Code</p>
            <p className="text-xs text-[#4A5A7A] mb-4">Ich sehe, was gerade im Editor steht.</p>
            <div className="flex flex-col gap-1.5">
              {ASSISTANT_QUICK_ACTIONS.map((a) => (
                <button key={a.label} onClick={() => send(a.prompt)}
                  className="flex items-center gap-2 px-3 py-2 rounded-lg text-xs text-left border border-[#1E2D4A] hover:border-[#4F8EF7] hover:bg-[#4F8EF7]/5 text-[#C9D6F0] transition-all">
                  <a.icon size={13} className="text-[#4F8EF7] shrink-0" />{a.label}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m, i) => (
          <div key={i} className={m.role === "user" ? "flex justify-end" : ""}>
            {m.role === "user" ? (
              <div className="max-w-[85%] px-3 py-2 rounded-xl rounded-br-sm text-white text-[13px]" style={{ background: GRADIENT }}>
                {m.content}
              </div>
            ) : (
              <div className={`px-3 py-2 rounded-xl rounded-bl-sm border ${m.error ? "border-[#EF4444]/30 bg-[#EF4444]/5 text-[#C9D6F0]" : "border-[#1E2D4A] bg-[#141D35] text-[#C9D6F0]"}`}>
                <div className="flex items-center gap-1.5 mb-1.5 text-[10px] text-[#8A9BC0]">
                  <Bot size={11} className="text-[#7C3AED]" />Assistent
                </div>
                <AssistantMessage content={m.content} />
              </div>
            )}
          </div>
        ))}

        {busy && (
          <div className="px-3 py-2 rounded-xl rounded-bl-sm border border-[#1E2D4A] bg-[#141D35] inline-flex items-center gap-2 text-xs text-[#8A9BC0]">
            <Loader2 size={12} className="ld-spin" />denkt nach …
          </div>
        )}
      </div>

      <div className="border-t border-[#1E2D4A] p-2">
        <div className="flex gap-2">
          <input value={input} onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(); } }}
            placeholder="Frage zu deinem Code …" disabled={busy}
            className="flex-1 bg-[#141D35] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg px-3 py-2 text-[13px] text-[#E8EDF5] placeholder:text-[#4A5A7A] disabled:opacity-50" />
          <Btn size="sm" ariaLabel="Frage senden" icon={busy ? undefined : Send} onClick={() => send()} disabled={busy || !input.trim()}>
            {busy ? <Loader2 size={14} className="ld-spin" /> : ""}
          </Btn>
        </div>
        {messages.length > 0 && (
          <button onClick={() => setMessages([])} className="text-[10px] text-[#4A5A7A] hover:text-[#8A9BC0] mt-1.5">
            Verlauf löschen
          </button>
        )}
      </div>
    </div>
  );
}

/* ============================== ZIP-Datei ================================
   Ein Projekt mit mehreren Dateien lässt sich nur sinnvoll als Archiv
   herunterladen. Hier steht ein minimaler ZIP-Schreiber (Methode „gespeichert“,
   also ohne Kompression) — das sind wenige Zeilen und spart eine Bibliothek.
   ========================================================================= */
const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
    table[i] = c >>> 0;
  }
  return table;
})();

function crc32(bytes) {
  let c = 0xFFFFFFFF;
  for (let i = 0; i < bytes.length; i++) c = CRC_TABLE[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
  return (c ^ 0xFFFFFFFF) >>> 0;
}

/** Baut ein ZIP-Archiv aus [{ name, content }] und gibt einen Blob zurück. */
function buildZip(entries) {
  const enc = new TextEncoder();
  const chunks = [];
  const central = [];
  let offset = 0;

  const push = (arr) => { chunks.push(arr); offset += arr.length; };
  const u16 = (v) => [v & 0xFF, (v >>> 8) & 0xFF];
  const u32 = (v) => [v & 0xFF, (v >>> 8) & 0xFF, (v >>> 16) & 0xFF, (v >>> 24) & 0xFF];

  for (const entry of entries) {
    const nameBytes = enc.encode(entry.name);
    const data = enc.encode(String(entry.content ?? ""));
    const crc = crc32(data);
    const localOffset = offset;

    push(new Uint8Array([
      0x50, 0x4B, 0x03, 0x04,        // Signatur „lokaler Dateikopf“
      20, 0,                          // benötigte Version
      ...u16(0x0800),                 // Flag: Dateiname ist UTF-8
      ...u16(0),                      // Methode 0 = gespeichert
      ...u16(0), ...u16(0),           // Uhrzeit/Datum (fest, damit gleich bleibt, was gleich ist)
      ...u32(crc), ...u32(data.length), ...u32(data.length),
      ...u16(nameBytes.length), ...u16(0),
    ]));
    push(nameBytes);
    push(data);

    central.push(new Uint8Array([
      0x50, 0x4B, 0x01, 0x02,
      20, 0, 20, 0,
      ...u16(0x0800), ...u16(0),
      ...u16(0), ...u16(0),
      ...u32(crc), ...u32(data.length), ...u32(data.length),
      ...u16(nameBytes.length), ...u16(0), ...u16(0),
      ...u16(0), ...u16(0), ...u32(0),
      ...u32(localOffset),
      ...nameBytes,
    ]));
  }

  const centralStart = offset;
  central.forEach(push);
  const centralSize = offset - centralStart;

  push(new Uint8Array([
    0x50, 0x4B, 0x05, 0x06,
    ...u16(0), ...u16(0),
    ...u16(entries.length), ...u16(entries.length),
    ...u32(centralSize), ...u32(centralStart),
    ...u16(0),
  ]));

  return new Blob(chunks, { type: "application/zip" });
}

/** Löst einen Download im Browser aus. */
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function safeSlug(text, fallback) {
  const slug = String(text || "").toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
  return slug || fallback;
}

/* ========================= Python im Browser =============================
   Python läuft hier wirklich — nicht simuliert. Pyodide ist das echte CPython,
   nach WebAssembly übersetzt. Es wird erst geladen, wenn jemand zum ersten Mal
   auf „Ausführen“ drückt (rund 10 MB), und bleibt danach im Speicher.

   Was nicht geht: `input()` (es gibt keine Eingabezeile), Netzwerkzugriffe und
   Pakete, die C-Erweiterungen brauchen. Das sagen wir offen, statt es zu
   verschweigen.
   ========================================================================= */
const PYODIDE_URL = "https://cdn.jsdelivr.net/pyodide/v0.26.4/full/";
let pyodidePromise = null;

function getPyodide() {
  if (!pyodidePromise) {
    pyodidePromise = (async () => {
      const mod = await import(/* @vite-ignore */ `${PYODIDE_URL}pyodide.mjs`);
      return mod.loadPyodide({ indexURL: PYODIDE_URL });
    })();
    // Bei einem Fehlschlag darf der nächste Versuch es erneut probieren.
    pyodidePromise.catch(() => { pyodidePromise = null; });
  }
  return pyodidePromise;
}

// Für alles, was der Browser nicht ausführen kann: der Befehl für den eigenen
// Rechner. Das ist ehrlicher als ein Knopf, der nichts tut.
const LOCAL_RUN_HINTS = {
  java: (f) => `javac ${f} && java ${f.replace(/\.java$/, "")}`,
  kt: (f) => `kotlinc ${f} -include-runtime -d app.jar && java -jar app.jar`,
  c: (f) => `gcc ${f} -o programm && ./programm`,
  cpp: (f) => `g++ ${f} -o programm && ./programm`,
  cs: (f) => `dotnet run`,
  go: (f) => `go run ${f}`,
  rs: (f) => `rustc ${f} && ./${f.replace(/\.rs$/, "")}`,
  php: (f) => `php ${f}`,
  rb: (f) => `ruby ${f}`,
  swift: (f) => `swift ${f}`,
  sh: (f) => `bash ${f}`,
  sql: () => `In einer Datenbank ausführen, z.B. psql oder DB Browser for SQLite`,
  ts: (f) => `npx tsx ${f}`,
  tsx: (f) => `npm run dev  (Vite/Next)`,
  jsx: (f) => `npm run dev  (Vite/Next)`,
  vue: () => `npm run dev  (Vite)`,
};

/* =============================== Die IDE =================================
   Aufbau wie in einer Desktop-IDE: links der Datei-Explorer, in der Mitte der Editor
   mit Registerkarten, rechts Vorschau, Konsole, Probleme und der KI-Assistent.

   Wichtige Eigenschaften:
   • Start ohne Beispielcode — es steht nichts im Weg.
   • Dateien für jede unterstützte Sprache anlegen, umbenennen, löschen.
   • Alle 30 Sekunden wird automatisch gespeichert (Strg+S jederzeit).
   • Vollbild schaltet die Umgebung auf die ganze Fensterfläche.
   ========================================================================= */
const AUTOSAVE_MS = 30_000;

/** Die Datei-Vorlagen, die beim Anlegen als Startpunkt dienen. */
const FILE_TEMPLATES = {
  html: '<!DOCTYPE html>\n<html lang="de">\n<head>\n  <meta charset="UTF-8">\n  <title>Meine Seite</title>\n</head>\n<body>\n  \n</body>\n</html>\n',
  py: '',
  java: '',
};

function NewFileDialog({ files, onCreate, onClose }) {
  const [type, setType] = useState(FILE_TYPES[0]);
  const [name, setName] = useState("index.html");
  const [touched, setTouched] = useState(false);
  const error = touched ? fileNameError(name, files) : null;

  const pick = (t) => {
    setType(t);
    // Solange der Name nicht selbst angefasst wurde, passt er sich der Sprache an.
    if (!touched) {
      const suggestion = t.ext === "html" ? "index.html"
        : t.ext === "css" ? "style.css"
        : t.ext === "js" ? "script.js"
        : `main.${t.ext}`;
      setName(suggestion);
    }
  };

  const create = () => {
    const problem = fileNameError(name, files);
    if (problem) { setTouched(true); return; }
    onCreate(name.trim());
  };

  return (
    <div className="fixed inset-0 z-[80] bg-black/70 backdrop-blur-sm flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-[#0F1629] border border-[#1E2D4A] rounded-2xl w-full max-w-lg p-6" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-display text-lg font-bold flex items-center gap-2"><FilePlus className="text-[#4F8EF7]" />Neue Datei</h3>
          <button onClick={onClose} aria-label="Schließen" className="text-[#8A9BC0] hover:text-[#E8EDF5]"><X size={18} /></button>
        </div>

        <p className="text-xs text-[#8A9BC0] mb-2">Sprache wählen</p>
        <div className="grid grid-cols-3 sm:grid-cols-4 gap-1.5 max-h-52 overflow-y-auto mb-4 pr-1">
          {FILE_TYPES.map((t) => (
            <button key={t.ext} onClick={() => pick(t)}
              className={`px-2 py-2 rounded-lg border text-left transition-all ${type.ext === t.ext ? "border-[#4F8EF7] bg-[#4F8EF7]/10" : "border-[#1E2D4A] hover:border-[#2A3F6F]"}`}>
              <span className="block w-2 h-2 rounded-full mb-1" style={{ background: t.color }} />
              <span className="block text-[11px] font-medium text-[#E8EDF5] truncate">{t.label}</span>
              <span className="block font-code text-[10px] text-[#4A5A7A]">.{t.ext}</span>
            </button>
          ))}
        </div>

        <label className="block text-xs text-[#8A9BC0] mb-1.5">Dateiname</label>
        <input value={name} autoFocus
          onChange={(e) => { setName(e.target.value); setTouched(true); }}
          onKeyDown={(e) => { if (e.key === "Enter") create(); if (e.key === "Escape") onClose(); }}
          className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 font-code text-sm text-[#E8EDF5]" />
        {error && <p className="text-xs text-[#EF4444] mt-1.5">{error}</p>}

        <div className="flex gap-2 mt-5">
          <Btn className="flex-1" icon={Check} onClick={create}>Anlegen</Btn>
          <Btn variant="ghost" onClick={onClose}>Abbrechen</Btn>
        </div>
      </div>
    </div>
  );
}

function Playground({ ctx }) {
  const { me, savePlaygroundProject, deletePlaygroundProject, playgroundOpenId, setPlaygroundOpenId, pushToast } = ctx;

  const [name, setName] = useState("Mein Projekt");
  const [files, setFiles] = useState([]);              // bewusst leer — kein Beispielcode
  const [activeId, setActiveId] = useState(null);
  const [tabs, setTabs] = useState([]);                // geöffnete Dateien (IDs)
  const [projectId, setProjectId] = useState(null);

  const [srcDoc, setSrcDoc] = useState("");
  const [logs, setLogs] = useState([]);
  const [rightTab, setRightTab] = useState("preview");
  const [cursor, setCursor] = useState({ line: 1, column: 1 });
  const [minimap, setMinimap] = useState(true);
  const [wrap, setWrap] = useState(false);
  const [explorerOpen, setExplorerOpen] = useState(true);
  const [panelOpen, setPanelOpen] = useState(true);
  const [fullscreen, setFullscreen] = useState(false);
  const [autoInclude, setAutoInclude] = useState(true);
  const [previewLive, setPreviewLive] = useState(false);
  const [problems, setProblems] = useState(null);
  const [newFileOpen, setNewFileOpen] = useState(false);
  const [renamingId, setRenamingId] = useState(null);
  const [renameValue, setRenameValue] = useState("");
  const [dirty, setDirty] = useState(false);
  const [savedAt, setSavedAt] = useState(null);
  const [saving, setSaving] = useState(false);
  const [projectsOpen, setProjectsOpen] = useState(false);
  const [running, setRunning] = useState(false);
  const [lightTheme, setLightTheme] = useState(false);
  const canLightTheme = hasUnlock(me, "light_editor");

  const previewWin = useRef(null);
  const saveRef = useRef(null);
  const dirtyRef = useRef(false);
  useEffect(() => { dirtyRef.current = dirty; }, [dirty]);

  const projects = (me?.playground || []).map(normalizeProject);
  const activeFile = files.find((f) => f.id === activeId) || null;

  /* ------------------------------ Dateien -------------------------------- */
  const openFile = (id) => {
    setActiveId(id);
    setTabs((t) => (t.includes(id) ? t : [...t, id]));
  };

  const createFile = (fileName) => {
    const finalName = uniqueFileName(files, fileName);
    const file = { id: newFileId(), name: finalName, content: FILE_TEMPLATES[extOf(finalName)] ?? "" };
    setFiles((f) => [...f, file]);
    setDirty(true);
    setNewFileOpen(false);
    openFile(file.id);
    playSound("click");
  };

  const updateActive = (content) => {
    if (!activeId) return;
    setFiles((f) => f.map((x) => (x.id === activeId ? { ...x, content } : x)));
    setDirty(true);
  };

  const removeFile = (id) => {
    const file = files.find((f) => f.id === id);
    if (!file) return;
    if (!window.confirm(`„${file.name}“ wirklich löschen?`)) return;
    setFiles((f) => f.filter((x) => x.id !== id));
    setTabs((t) => t.filter((x) => x !== id));
    setDirty(true);
    if (activeId === id) {
      const rest = files.filter((f) => f.id !== id);
      setActiveId(rest.length ? rest[0].id : null);
    }
  };

  const commitRename = () => {
    const problem = fileNameError(renameValue, files, renamingId);
    if (problem) { pushToast("error", problem); return; }
    setFiles((f) => f.map((x) => (x.id === renamingId ? { ...x, name: renameValue.trim() } : x)));
    setDirty(true);
    setRenamingId(null);
  };

  const closeTab = (id) => {
    setTabs((t) => t.filter((x) => x !== id));
    if (activeId === id) {
      const rest = tabs.filter((x) => x !== id);
      setActiveId(rest.length ? rest[rest.length - 1] : null);
    }
  };

  /* ------------------------------ Speichern ------------------------------ */
  const doSave = useCallback(async (silent = false) => {
    if (!me) return false;
    if (me.isGuest) {
      if (!silent) pushToast("info", "Als Gast wird nichts gespeichert — erstelle ein Konto, um deine Projekte zu behalten.");
      return false;
    }
    if (!files.length) {
      if (!silent) pushToast("info", "Leg zuerst eine Datei an.");
      return false;
    }
    setSaving(true);
    const id = projectId || uid();
    const ok = await savePlaygroundProject({ id, name: name.trim() || "Unbenannt", files }, { silent });
    setSaving(false);
    if (ok) {
      setProjectId(id);
      setDirty(false);
      setSavedAt(new Date());
      if (!silent) playSound("save");
    }
    return ok;
  }, [me, files, name, projectId, savePlaygroundProject, pushToast]);

  useEffect(() => { saveRef.current = doSave; }, [doSave]);

  // Automatisch speichern — alle 30 Sekunden, aber nur wenn sich etwas geändert hat.
  useEffect(() => {
    if (!me || me.isGuest) return;
    const timer = setInterval(() => {
      if (dirtyRef.current) saveRef.current?.(true);
    }, AUTOSAVE_MS);
    return () => clearInterval(timer);
  }, [me]);

  // Strg+S bzw. Cmd+S, auch außerhalb des Editors
  useEffect(() => {
    const onKey = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "s") {
        e.preventDefault();
        saveRef.current?.();
      } else if (e.key === "Escape" && fullscreen) {
        setFullscreen(false);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [fullscreen]);

  // Beim Verlassen der Seite noch schnell sichern
  useEffect(() => {
    const onLeave = (e) => {
      if (!dirtyRef.current || !me || me.isGuest) return;
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", onLeave);
    return () => window.removeEventListener("beforeunload", onLeave);
  }, [me]);

  /* ------------------------------ Projekte ------------------------------- */
  const loadProject = (project) => {
    const normalized = normalizeProject(project);
    setProjectId(normalized.id);
    setName(normalized.name);
    setFiles(normalized.files);
    setTabs(normalized.files.slice(0, 3).map((f) => f.id));
    setActiveId(normalized.files[0]?.id || null);
    setDirty(false);
    setSavedAt(null);
    setProjectsOpen(false);
    pushToast("info", `Projekt „${normalized.name}“ geladen.`);
  };

  const newProject = () => {
    if (dirty && !window.confirm("Es gibt ungespeicherte Änderungen. Trotzdem ein neues Projekt beginnen?")) return;
    setProjectId(null);
    setName("Mein Projekt");
    setFiles([]);
    setTabs([]);
    setActiveId(null);
    setDirty(false);
    setSavedAt(null);
    setProblems(null);
    setLogs([]);
    // Ein leeres Projekt sieht aus wie „nichts passiert“. Deshalb sagen wir es
    // und öffnen gleich den Dialog für die erste Datei.
    pushToast("info", "Neues Projekt begonnen.");
    setNewFileOpen(true);
    playSound("click");
  };

  useEffect(() => {
    if (!playgroundOpenId || !me) return;
    const p = (me.playground || []).find((x) => x.id === playgroundOpenId);
    if (p) loadProject(p);
    setPlaygroundOpenId(null);
  }, [playgroundOpenId, me]);

  useEffect(() => { ctx.loadProjects?.(); }, []);

  /* ------------------------------ Vorschau ------------------------------- */
  const entry = entryFile(files);

  const buildPage = useCallback(
    (extraScript = "") => buildProjectPage(files, { title: name || "Meine Seite", extraScript, autoInclude }),
    [files, name, autoInclude]
  );

  useEffect(() => {
    const t = setTimeout(() => {
      setLogs([]);
      const bridge = `
        (function () {
          var send = function (level, args) {
            try {
              parent.postMessage({ __ldConsole: true, level: level, text: Array.prototype.map.call(args, function (a) {
                try { return typeof a === "object" ? JSON.stringify(a) : String(a); } catch (e) { return String(a); }
              }).join(" ") }, "*");
            } catch (e) {}
          };
          ["log", "info", "warn", "error"].forEach(function (lvl) {
            var orig = console[lvl];
            console[lvl] = function () { send(lvl, arguments); if (orig) orig.apply(console, arguments); };
          });
          window.addEventListener("error", function (e) { send("error", [e.message + " (Zeile " + e.lineno + ")"]); });
          window.addEventListener("unhandledrejection", function (e) { send("error", ["Unbehandelte Promise-Ablehnung: " + e.reason]); });
        })();
      `;
      setSrcDoc(buildPage(bridge) || "");
    }, 350);
    return () => clearTimeout(t);
  }, [buildPage]);

  useEffect(() => {
    const onMessage = (e) => {
      if (!e.data || !e.data.__ldConsole) return;
      setLogs((l) => [...l.slice(-199), { level: e.data.level, text: e.data.text }]);
    };
    window.addEventListener("message", onMessage);
    return () => window.removeEventListener("message", onMessage);
  }, []);

  const writeToPreviewWindow = useCallback((win) => {
    if (!win || win.closed) return false;
    const page = buildPage();
    if (!page) return false;
    try {
      const y = win.scrollY || 0;
      win.document.open();
      win.document.write(page);
      win.document.close();
      win.scrollTo(0, y);
      return true;
    } catch (e) {
      return false;
    }
  }, [buildPage]);

  const openPreviewTab = () => {
    if (!entry) { pushToast("error", "Für die Vorschau braucht es eine HTML-Datei."); return; }
    const existing = previewWin.current;
    if (existing && !existing.closed) {
      writeToPreviewWindow(existing);
      existing.focus();
      pushToast("info", "Vorschau aktualisiert.");
      return;
    }
    const win = window.open("", "ld-preview");
    if (!win) { pushToast("error", "Der Browser hat das Fenster blockiert — erlaube Pop-ups für diese Seite."); return; }
    previewWin.current = win;
    writeToPreviewWindow(win);
    setPreviewLive(true);
    pushToast("success", "Vorschau geöffnet — sie aktualisiert sich bei jeder Änderung.");
  };

  useEffect(() => {
    if (!previewLive) return;
    const t = setTimeout(() => {
      const win = previewWin.current;
      if (!win || win.closed) { setPreviewLive(false); previewWin.current = null; return; }
      writeToPreviewWindow(win);
    }, 400);
    return () => clearTimeout(t);
  }, [previewLive, writeToPreviewWindow]);

  useEffect(() => () => {
    const win = previewWin.current;
    if (win && !win.closed) win.close();
  }, []);

  /* ---------------------------- Herunterladen ---------------------------- */
  const downloadProject = () => {
    if (!files.length) { pushToast("info", "Es gibt noch nichts zum Herunterladen."); return; }
    const slug = safeSlug(name, "projekt");
    if (files.length === 1) {
      downloadBlob(new Blob([files[0].content], { type: "text/plain;charset=utf-8" }), files[0].name);
      pushToast("success", `${files[0].name} heruntergeladen.`);
      return;
    }
    downloadBlob(buildZip(files.map((f) => ({ name: f.name, content: f.content }))), `${slug}.zip`);
    pushToast("success", `${slug}.zip mit ${files.length} Dateien heruntergeladen.`);
  };

  const downloadSinglePage = () => {
    const page = buildPage();
    if (!page) { pushToast("error", "Dafür braucht es eine HTML-Datei."); return; }
    const slug = safeSlug(name, "meine-seite");
    downloadBlob(new Blob([page], { type: "text/html;charset=utf-8" }), `${slug}.html`);
    pushToast("success", `${slug}.html heruntergeladen — alles in einer Datei.`);
  };

  /* ------------------------------ Ausführen ------------------------------
     Python läuft echt im Browser (Pyodide). Für alle anderen Sprachen gibt es
     den passenden Befehl für den eigenen Rechner — mit einem Knopf, der so
     tut als ob, wäre niemandem geholfen.
     --------------------------------------------------------------------- */
  const log = (level, text) => setLogs((l) => [...l.slice(-199), { level, text }]);

  const runFile = async () => {
    if (!activeFile || running) return;
    const ext = extOf(activeFile.name);
    setRightTab("console");
    setPanelOpen(true);

    if (ext === "html") { openPreviewTab(); return; }
    if (ext === "css" || ext === "js") {
      log("info", "HTML, CSS und JavaScript laufen dauerhaft in der Vorschau — dort siehst du das Ergebnis sofort.");
      setRightTab("preview");
      return;
    }
    if (ext !== "py") {
      const hint = LOCAL_RUN_HINTS[ext];
      log("info", `${fileTypeOf(activeFile.name).label} kann der Browser nicht ausführen — das braucht die Sprache auf deinem Rechner.`);
      if (hint) log("info", `Auf deinem Rechner: ${hint(activeFile.name)}`);
      log("info", "Lade das Projekt herunter (Knopf oben) und führe es dort aus. Prüfen und der KI-Assistent funktionieren hier trotzdem.");
      return;
    }

    setRunning(true);
    log("info", `▶ ${activeFile.name}`);
    try {
      if (!pyodidePromise) log("info", "Python-Laufzeit wird geladen — beim ersten Mal etwa 10 MB.");
      const py = await getPyodide();
      py.setStdout({ batched: (text) => log("log", text) });
      py.setStderr({ batched: (text) => log("error", text) });
      // Andere Python-Dateien des Projekts als Module bereitstellen,
      // damit `import helfer` funktioniert.
      for (const file of files) {
        if (extOf(file.name) === "py" && file.id !== activeFile.id) {
          py.FS.writeFile(file.name, file.content, { encoding: "utf8" });
        }
      }
      await py.runPythonAsync(activeFile.content);
      log("info", "✓ Ausführung beendet");
      playSound("correct");
    } catch (e) {
      log("error", String(e?.message || e));
      playSound("wrong");
    } finally {
      setRunning(false);
    }
  };

  /* ----------------------------- Fehlerprüfung --------------------------- */
  const runCheck = () => {
    const res = analyzeProject({
      html: files.filter((f) => extOf(f.name) === "html").map((f) => f.content).join("\n"),
      css: files.filter((f) => extOf(f.name) === "css").map((f) => f.content).join("\n"),
      js: files.filter((f) => extOf(f.name) === "js").map((f) => f.content).join("\n"),
    });
    setProblems(res);
    setRightTab("problems");
    setPanelOpen(true);
    playSound(res.issues.length ? "wrong" : "correct");
  };

  /* ------------------------------ Speicher ------------------------------- */
  const quotaBytes = me?.storageQuota || STORAGE_QUOTA_BYTES;
  const usedBytes = me?.storageUsed != null && ctx.backend
    ? me.storageUsed
    : projects.reduce((sum, p) => sum + (p.sizeBytes || 0), 0);
  const quotaPct = Math.min(100, (usedBytes / quotaBytes) * 100);

  const saveLabel = me?.isGuest
    ? "Gast — wird nicht gespeichert"
    : saving ? "Speichert …"
    : dirty ? "Nicht gespeicherte Änderungen"
    : savedAt ? `Gespeichert ${savedAt.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })}`
    : projectId ? "Gespeichert" : "Noch nicht gespeichert";

  /* ------------------------------ Bausteine ------------------------------ */
  const toolButton = (icon, label, onClick, active) => (
    <button onClick={onClick} title={label} aria-label={label}
      className={`w-8 h-8 rounded-lg border flex items-center justify-center transition-all shrink-0 ${active ? "border-[#4F8EF7] text-[#4F8EF7] bg-[#4F8EF7]/10" : "border-[#1E2D4A] text-[#8A9BC0] hover:border-[#2A3F6F] hover:text-[#E8EDF5]"}`}>
      {icon}
    </button>
  );

  const explorer = (
    <div className="flex flex-col h-full bg-[#0B1120] border border-[#1E2D4A] rounded-xl overflow-hidden">
      <div className="flex items-center gap-2 px-3 py-2 border-b border-[#1E2D4A]">
        <FolderTree size={13} className="text-[#8A9BC0]" />
        <span className="text-[11px] uppercase tracking-wider text-[#8A9BC0] flex-1">Explorer</span>
        <button onClick={() => setNewFileOpen(true)} title="Neue Datei" aria-label="Neue Datei"
          className="text-[#8A9BC0] hover:text-[#4F8EF7]"><FilePlus size={14} /></button>
      </div>

      <input value={name} onChange={(e) => { setName(e.target.value); setDirty(true); }} placeholder="Projektname"
        className="mx-3 my-2 bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg px-2.5 py-1.5 text-xs font-medium text-[#E8EDF5]" />

      <div className="flex-1 overflow-y-auto px-2 pb-2">
        {files.length === 0 ? (
          <p className="text-[11px] text-[#4A5A7A] px-2 py-3 leading-relaxed">
            Noch keine Datei. Leg eine an — für jede Sprache, die du brauchst.
          </p>
        ) : files.map((f) => {
          const type = fileTypeOf(f.name);
          const isActive = f.id === activeId;
          return (
            <div key={f.id}
              className={`group flex items-center gap-2 px-2 py-1.5 rounded-lg cursor-pointer text-[13px] ${isActive ? "bg-[#4F8EF7]/12 text-[#E8EDF5]" : "text-[#8A9BC0] hover:bg-white/5"}`}
              onClick={() => openFile(f.id)}>
              <span className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: type.color }} />
              {renamingId === f.id ? (
                <input value={renameValue} autoFocus
                  onChange={(e) => setRenameValue(e.target.value)}
                  onBlur={commitRename}
                  onKeyDown={(e) => { if (e.key === "Enter") commitRename(); if (e.key === "Escape") setRenamingId(null); }}
                  onClick={(e) => e.stopPropagation()}
                  className="flex-1 min-w-0 bg-[#0A0E1A] border border-[#4F8EF7] rounded px-1 font-code text-[12px] text-[#E8EDF5]" />
              ) : (
                <span className="flex-1 truncate font-code">{f.name}</span>
              )}
              <button onClick={(e) => { e.stopPropagation(); setRenamingId(f.id); setRenameValue(f.name); }}
                aria-label={`${f.name} umbenennen`} title="Umbenennen"
                className="opacity-0 group-hover:opacity-100 text-[#4A5A7A] hover:text-[#4F8EF7]"><PenSquare size={11} /></button>
              <button onClick={(e) => { e.stopPropagation(); removeFile(f.id); }}
                aria-label={`${f.name} löschen`} title="Löschen"
                className="opacity-0 group-hover:opacity-100 text-[#4A5A7A] hover:text-[#EF4444]"><Trash2 size={11} /></button>
            </div>
          );
        })}
      </div>

      <div className="border-t border-[#1E2D4A]">
        <button onClick={() => setProjectsOpen((v) => !v)}
          className="w-full flex items-center gap-2 px-3 py-2 text-[11px] uppercase tracking-wider text-[#8A9BC0] hover:text-[#E8EDF5]">
          <ChevronRight size={10} className={projectsOpen ? "rotate-90 transition-transform" : "transition-transform"} />
          Projekte<span className="ml-auto text-[#4A5A7A]">{projects.length}</span>
        </button>
        {projectsOpen && (
          <div className="max-h-40 overflow-y-auto px-2 pb-2">
            {projects.length === 0 ? (
              <p className="text-[11px] text-[#4A5A7A] px-2 py-2">Noch nichts gespeichert.</p>
            ) : projects.map((p) => (
              <div key={p.id} className="group flex items-center gap-2 px-2 py-1.5 rounded-lg text-[12px] text-[#8A9BC0] hover:bg-white/5 cursor-pointer"
                onClick={() => loadProject(p)}>
                <Code2 size={11} className="text-[#4F8EF7] shrink-0" />
                <span className="flex-1 truncate">{p.name}</span>
                <span className="text-[10px] text-[#4A5A7A]">{(p.files || []).length}</span>
                <button onClick={(e) => { e.stopPropagation(); deletePlaygroundProject(p.id); }}
                  aria-label={`${p.name} löschen`} className="opacity-0 group-hover:opacity-100 text-[#4A5A7A] hover:text-[#EF4444]"><Trash2 size={11} /></button>
              </div>
            ))}
          </div>
        )}
        <div className="px-3 pb-3 pt-1">
          <div className="flex items-center justify-between text-[10px] text-[#4A5A7A] mb-1">
            <span>{formatBytes(usedBytes)} von 2,5 GB</span>
            <span>{projects.length} Projekt{projects.length === 1 ? "" : "e"}</span>
          </div>
          <ProgressBar value={quotaPct} max={100} height="h-1" />
        </div>
      </div>
    </div>
  );

  const editorArea = (
    <div className="flex flex-col h-full min-w-0 bg-[#0B1120] border border-[#1E2D4A] rounded-xl overflow-hidden">
      {/* Registerkarten */}
      <div className="flex items-stretch border-b border-[#1E2D4A] overflow-x-auto shrink-0">
        {tabs.length === 0 && <div className="px-3 py-2 text-[11px] text-[#4A5A7A]">Keine Datei geöffnet</div>}
        {tabs.map((id) => {
          const f = files.find((x) => x.id === id);
          if (!f) return null;
          const type = fileTypeOf(f.name);
          const isActive = id === activeId;
          return (
            <div key={id} onClick={() => setActiveId(id)}
              className={`group flex items-center gap-2 px-3 py-2 cursor-pointer border-r border-[#1E2D4A] whitespace-nowrap ${isActive ? "bg-[#0A0E1A] text-[#E8EDF5]" : "text-[#8A9BC0] hover:text-[#E8EDF5]"}`}
              style={isActive ? { boxShadow: "inset 0 2px 0 " + type.color } : undefined}>
              <span className="w-1.5 h-1.5 rounded-full" style={{ background: type.color }} />
              <span className="font-code text-[12px]">{f.name}</span>
              <button onClick={(e) => { e.stopPropagation(); closeTab(id); }} aria-label={`${f.name} schließen`}
                className="text-[#4A5A7A] hover:text-[#EF4444] opacity-0 group-hover:opacity-100"><X size={10} /></button>
            </div>
          );
        })}
        <button onClick={() => setNewFileOpen(true)} aria-label="Neue Datei"
          className="px-3 text-[#4A5A7A] hover:text-[#4F8EF7]"><Plus size={12} /></button>
      </div>

      {/* Editor */}
      <div className="flex-1 min-h-0">
        {activeFile ? (
          <LdCodeEditor
            key={activeFile.id}
            value={activeFile.content}
            onChange={updateActive}
            path={activeFile.name}
            language={langOf(activeFile.name)}
            height="100%"
            chrome={false}
            theme={lightTheme && canLightTheme ? "vs" : "ld-dark"}
            showMinimap={minimap}
            wordWrap={wrap ? "on" : "off"}
            onCursor={setCursor}
            onReady={(editor, monaco) => {
              editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => saveRef.current?.());
            }}
          />
        ) : (
          <div className="h-full flex flex-col items-center justify-center gap-3 text-center px-6">
            <FileCode size={34} className="text-[#2A3F6F]" />
            <p className="text-sm text-[#8A9BC0]">Der Editor ist leer — genau wie er sein soll.</p>
            <p className="text-xs text-[#4A5A7A] max-w-sm leading-relaxed">
              Leg eine Datei an und leg los. HTML, CSS und JavaScript laufen direkt in der Vorschau,
              Python führst du mit einem Klick aus. Alle anderen Sprachen kannst du schreiben,
              prüfen lassen und herunterladen.
            </p>
            <Btn size="sm" icon={FilePlus} onClick={() => setNewFileOpen(true)}>Erste Datei anlegen</Btn>
          </div>
        )}
      </div>

      {/* Statusleiste wie in einer Desktop-IDE */}
      <div className="flex items-center gap-x-4 gap-y-1 px-3 py-1.5 border-t border-[#1E2D4A] text-[11px] text-[#4A5A7A] font-code overflow-x-auto shrink-0">
        <span>Zeile {cursor.line}, Spalte {cursor.column}</span>
        {activeFile && <span>{activeFile.content.split("\n").length} Zeilen</span>}
        <span>UTF-8</span>
        <span>Leerzeichen: 2</span>
        {activeFile && <span className="text-[#8A9BC0]">{fileTypeOf(activeFile.name).label}</span>}
        {activeFile && langOf(activeFile.name) === "html" && (
          <span className="text-[#4F8EF7]">Emmet: <kbd className="px-1 rounded bg-[#141D35]">!</kbd> + Tab</span>
        )}
        <span className={`ml-auto flex items-center gap-1.5 ${dirty ? "text-[#F59E0B]" : "text-[#10B981]"}`}>
          {saving ? <Loader2 size={10} className="ld-spin" /> : <span className="w-1.5 h-1.5 rounded-full" style={{ background: "currentColor" }} />}
          {saveLabel}
        </span>
      </div>
    </div>
  );

  const panelTabs = [
    ["preview", "Vorschau", Eye],
    ["console", "Konsole", Terminal],
    ["problems", "Probleme", Bug],
    ["assistant", "KI", Bot],
  ];

  const panel = (
    <div className="flex flex-col h-full min-w-0 bg-[#0B1120] border border-[#1E2D4A] rounded-xl overflow-hidden">
      <div className="flex items-center gap-1 px-2 py-1.5 border-b border-[#1E2D4A] shrink-0">
        {panelTabs.map(([v, label, Icon]) => (
          <button key={v} onClick={() => setRightTab(v)}
            className={`px-2.5 py-1 rounded-md text-[11px] font-medium transition-all flex items-center gap-1.5 ${rightTab === v ? "text-white" : "text-[#8A9BC0] hover:text-[#E8EDF5]"}`}
            style={rightTab === v ? { background: GRADIENT } : undefined}>
            <Icon size={11} />{label}
            {v === "console" && logs.length > 0 && (
              <span className={`text-[9px] px-1.5 rounded-full ${logs.some((l) => l.level === "error") ? "bg-[#EF4444]/25 text-[#EF4444]" : "bg-white/20"}`}>{logs.length}</span>
            )}
            {v === "problems" && problems?.issues?.length > 0 && (
              <span className="text-[9px] px-1.5 rounded-full bg-[#F59E0B]/25 text-[#F59E0B]">{problems.issues.length}</span>
            )}
          </button>
        ))}
        <div className="ml-auto flex items-center gap-1">
          {rightTab === "preview" && entry && (
            <label className="flex items-center gap-1 text-[10px] text-[#4A5A7A] cursor-pointer" title="CSS- und JS-Dateien ohne Verweis automatisch einbinden">
              <input type="checkbox" checked={autoInclude} onChange={(e) => setAutoInclude(e.target.checked)} className="accent-[#4F8EF7]" />
              auto
            </label>
          )}
          {rightTab === "console" && logs.length > 0 && (
            <button onClick={() => setLogs([])} className="text-[10px] text-[#8A9BC0] hover:text-[#E8EDF5] flex items-center gap-1"><Trash2 size={10} />Leeren</button>
          )}
          <button onClick={() => setPanelOpen(false)} aria-label="Bereich schließen" className="text-[#4A5A7A] hover:text-[#E8EDF5]"><X size={12} /></button>
        </div>
      </div>

      <div className={`flex-1 min-h-0 ${rightTab === "preview" && entry ? "bg-white" : "bg-[#0A0E1A]"}`}>
        {rightTab === "preview" ? (
          entry ? (
            <iframe title="Live-Vorschau" srcDoc={srcDoc} sandbox="allow-scripts allow-modals" className="w-full h-full border-0" />
          ) : (
            <div className="h-full flex flex-col items-center justify-center text-center gap-2 px-6">
              <Globe size={28} className="text-[#2A3F6F]" />
              <p className="text-sm text-[#8A9BC0]">Keine Vorschau möglich.</p>
              <p className="text-xs text-[#4A5A7A] max-w-xs leading-relaxed">
                Der Browser kann nur HTML, CSS und JavaScript ausführen. Leg eine{" "}
                <span className="font-code text-[#4F8EF7]">index.html</span> an, um deine Seite hier zu sehen.
                {files.some((f) => extOf(f.name) === "py")
                  ? " Python führst du mit „Ausführen“ aus — die Ausgabe erscheint in der Konsole."
                  : files.length > 0 && !files.some((f) => runsInBrowser(f.name))
                    ? " Diese Sprache kannst du hier schreiben, prüfen lassen und herunterladen — ausführen musst du sie auf deinem Rechner."
                    : ""}
              </p>
            </div>
          )
        ) : rightTab === "assistant" ? (
          <AssistantPanel ctx={ctx} code={{
            html: files.filter((f) => extOf(f.name) === "html").map((f) => f.content).join("\n"),
            css: files.filter((f) => extOf(f.name) === "css").map((f) => f.content).join("\n"),
            js: files.filter((f) => !["html", "css"].includes(extOf(f.name))).map((f) => `/* ${f.name} */\n${f.content}`).join("\n\n"),
          }} />
        ) : rightTab === "problems" ? (
          <div className="h-full overflow-y-auto p-3 text-[12px]">
            {!problems ? (
              <div className="text-center py-6">
                <Bug size={24} className="mx-auto text-[#2A3F6F] mb-2" />
                <p className="text-[#4A5A7A] mb-3">Noch nicht geprüft.</p>
                <Btn size="sm" variant="secondary" icon={Bug} onClick={runCheck}>Jetzt prüfen</Btn>
              </div>
            ) : problems.issues.length === 0 ? (
              <p className="text-[#10B981] flex items-center gap-2"><CheckCircle2 size={14} />{problems.summary}</p>
            ) : (
              <div className="space-y-2">
                <p className="text-[#8A9BC0] mb-2">{problems.summary}</p>
                {problems.issues.map((iss, i) => {
                  const sev = { error: ["#EF4444", "Fehler"], warning: ["#F59E0B", "Warnung"], info: ["#4F8EF7", "Hinweis"] }[iss.severity] || ["#8A9BC0", "Hinweis"];
                  return (
                    <div key={i} className="p-2.5 rounded-lg bg-[#0F1629] border border-[#1E2D4A]">
                      <div className="flex flex-wrap items-center gap-2 mb-1">
                        <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full" style={{ color: sev[0], background: sev[0] + "22" }}>{sev[1]}</span>
                        {iss.where && <span className="font-code text-[10px] uppercase text-[#4A5A7A]">{iss.where}</span>}
                        <span className="text-[#E8EDF5]">{iss.title}</span>
                      </div>
                      {iss.detail && <p className="text-[11px] text-[#8A9BC0]">{iss.detail}</p>}
                      {iss.fix && <p className="text-[11px] text-[#10B981] mt-0.5">💡 {iss.fix}</p>}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ) : (
          <div className="h-full overflow-y-auto p-3 font-code text-[12px] leading-relaxed">
            {logs.length === 0 ? (
              <p className="text-[#4A5A7A]">
                Noch keine Ausgaben. <span className="text-[#4F8EF7]">console.log(...)</span> im JavaScript landet hier —
                und Python führst du mit <span className="text-[#4F8EF7]">Ausführen</span> direkt aus.
              </p>
            ) : logs.map((l, i) => {
              const color = l.level === "error" ? "#EF4444" : l.level === "warn" ? "#F59E0B" : l.level === "info" ? "#4F8EF7" : "#C9D6F0";
              return (
                <div key={i} className="flex gap-2 py-0.5 border-b border-[#1E2D4A]/40 last:border-0">
                  <span className="text-[#4A5A7A] shrink-0">{l.level === "error" ? "✕" : l.level === "warn" ? "!" : "›"}</span>
                  <span style={{ color }} className="whitespace-pre-wrap break-all">{l.text}</span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );

  /* -------------------------------- Aufbau ------------------------------- */
  const shell = (
    <div className={fullscreen
      ? "fixed inset-0 z-[70] bg-[#0A0E1A] p-3 flex flex-col gap-2"
      : "flex flex-col gap-2"}
      style={fullscreen ? undefined : { height: "calc(100vh - 8.5rem)", minHeight: 520 }}>

      {/* Werkzeugleiste */}
      <div className="flex items-center gap-2 flex-wrap shrink-0">
        <div className="flex items-center gap-2 mr-1">
          <Code2 className="text-[#4F8EF7]" size={18} />
          <span className="font-display font-bold">IDE</span>
        </div>

        {toolButton(<FolderTree size={13} />, explorerOpen ? "Explorer ausblenden" : "Explorer einblenden", () => setExplorerOpen((v) => !v), explorerOpen)}
        {toolButton(<FilePlus size={13} />, "Neue Datei anlegen", () => setNewFileOpen(true))}

        <div className="w-px h-6 bg-[#1E2D4A] mx-0.5" />

        <Btn size="sm" icon={saving ? undefined : Save} onClick={() => doSave()} disabled={saving || !files.length}>
          {saving ? <><Loader2 size={13} className="ld-spin" />Speichert …</> : "Speichern"}
        </Btn>
        <Btn size="sm" variant="secondary" icon={Plus} onClick={newProject}>Neues Projekt</Btn>

        <div className="w-px h-6 bg-[#1E2D4A] mx-0.5" />

        <Btn size="sm" icon={running ? undefined : Play} onClick={runFile} disabled={!activeFile || running}
          variant={activeFile && extOf(activeFile.name) === "py" ? "primary" : "secondary"}>
          {running ? <><Loader2 size={13} className="ld-spin" />Läuft …</> : "Ausführen"}
        </Btn>
        <Btn size="sm" variant="secondary" icon={ExternalLink} onClick={openPreviewTab}>
          {previewLive ? "Vorschau zeigen" : "Neuer Tab"}
        </Btn>
        <Btn size="sm" variant="secondary" icon={Download} onClick={downloadProject}>
          {files.length > 1 ? "ZIP" : "Download"}
        </Btn>
        {entry && toolButton(<FileCode size={13} />, "Als eine HTML-Datei herunterladen", downloadSinglePage)}
        <Btn size="sm" variant="secondary" icon={Bug} onClick={runCheck}>Prüfen</Btn>

        <div className="ml-auto flex items-center gap-2">
          {canLightTheme && toolButton(<Palette2 size={13} />, lightTheme ? "Dunkles Design" : "Helles Design", () => setLightTheme((v) => !v), lightTheme)}
          {toolButton(<MapIcon size={13} />, minimap ? "Minimap ausblenden" : "Minimap einblenden", () => setMinimap((v) => !v), minimap)}
          {toolButton(<ClipboardList size={13} />, wrap ? "Zeilenumbruch aus" : "Zeilenumbruch an", () => setWrap((v) => !v), wrap)}
          {toolButton(<Eye size={13} />, panelOpen ? "Seitenbereich ausblenden" : "Seitenbereich einblenden", () => setPanelOpen((v) => !v), panelOpen)}
          {toolButton(fullscreen ? <Compress size={13} /> : <Expand size={13} />, fullscreen ? "Vollbild verlassen (Esc)" : "Vollbild", () => setFullscreen((v) => !v), fullscreen)}
        </div>
      </div>

      {previewLive && (
        <div className="flex flex-wrap items-center gap-3 px-3 py-1.5 rounded-lg bg-[#10B981]/10 border border-[#10B981]/30 shrink-0">
          <span className="w-2 h-2 rounded-full bg-[#10B981]" style={{ animation: "ld-pulse 2s ease-in-out infinite" }} />
          <p className="text-xs text-[#C9D6F0] flex-1">Vorschau-Tab ist verbunden — er lädt bei jeder Änderung neu.</p>
          <button onClick={() => {
            const w = previewWin.current;
            if (w && !w.closed) w.close();
            previewWin.current = null;
            setPreviewLive(false);
          }} className="text-[11px] text-[#8A9BC0] hover:text-[#EF4444]">Trennen</button>
        </div>
      )}

      {/* Arbeitsfläche */}
      <div className="flex-1 min-h-0 flex gap-2">
        {explorerOpen && <div className="w-52 shrink-0 hidden md:block">{explorer}</div>}
        <div className="flex-1 min-w-0">{editorArea}</div>
        {panelOpen && <div className="flex-1 min-w-0 hidden lg:block max-w-[46%]">{panel}</div>}
      </div>

      {/* Auf kleinen Bildschirmen liegt der Bereich unter dem Editor */}
      {panelOpen && (
        <div className="lg:hidden shrink-0" style={{ height: 260 }}>{panel}</div>
      )}

      {newFileOpen && (
        <NewFileDialog files={files} onCreate={createFile} onClose={() => setNewFileOpen(false)} />
      )}
    </div>
  );

  return shell;
}

/* ============================== XP-Shop ==================================
   Gesammelte XP sollen sich auch ausgeben lassen. Jeder Artikel hier hat eine
   echte Wirkung — nichts ist bloße Zierde ohne Funktion.

   Die Preise stehen bewusst an einer Stelle und werden serverseitig noch
   einmal geprüft: sonst könnte man sich im Browser beliebig beschenken.
   ========================================================================= */
const SHOP_ITEMS = [
  {
    id: "streak_freeze", price: 200, icon: Snowflake, color: "#4F8EF7",
    name: "Streak-Schutz", kind: "stack", max: 3,
    short: "Rettet deine Serie, wenn du einen Tag verpasst.",
    detail: "Wird automatisch eingelöst, sobald genau ein Tag fehlt. Mehr als drei kannst du nicht halten.",
  },
  {
    id: "hint", price: 75, icon: Lightbulb, color: "#F7C948",
    name: "Tipp-Joker", kind: "stack", max: 20,
    short: "Zeigt in einer Aufgabe sofort die Auflösung.",
    detail: "Normalerweise kommt die Lösung erst nach drei Fehlversuchen — mit einem Joker sofort.",
  },
  {
    id: "xp_boost", price: 500, icon: Rocket, color: "#EF4444",
    name: "Doppelte XP (24 Stunden)", kind: "timed", hours: 24,
    short: "Einen Tag lang zählt jede Aufgabe doppelt.",
    detail: "Läuft ab dem Kauf. Ein zweiter Kauf hängt weitere 24 Stunden an.",
  },
  {
    id: "avatar_extras", price: 600, icon: Palette2, color: "#7C3AED",
    name: "Avatar-Extras", kind: "unlock",
    short: "Zusätzliche Frisuren, Accessoires und Hintergründe.",
    detail: "Krone, Kopfhörer mit Mikrofon, Verlaufshintergründe und mehr — dauerhaft freigeschaltet.",
  },
  {
    id: "light_editor", price: 400, icon: Palette2, color: "#10B981",
    name: "Helles Editor-Design", kind: "unlock",
    short: "Umschalter für ein helles Farbschema in der IDE.",
    detail: "Praktisch bei Tageslicht. Der Umschalter erscheint danach in der Werkzeugleiste der IDE.",
  },
];

const shopItemById = (id) => SHOP_ITEMS.find((i) => i.id === id);

/** Der Besitzstand eines Kontos in einer einheitlichen Form. */
function shopState(user) {
  return {
    streak_freeze: user?.streakFreezes || 0,
    hint: user?.hints || 0,
    unlocks: user?.unlocks || [],
    boostUntil: user?.boostUntil || 0,
  };
}

/**
 * Ausgeben senkt nicht das Level: `xp` bleibt die Lebensleistung, `spentXp`
 * merkt sich, was davon schon ausgegeben wurde. Guthaben ist die Differenz.
 */
function xpBalance(user) {
  return Math.max(0, (user?.xp || 0) - (user?.spentXp || 0));
}

function boostActive(user) {
  return (user?.boostUntil || 0) > Date.now();
}

function hasUnlock(user, id) {
  return (user?.unlocks || []).includes(id);
}

/** Wie viel besitzt man von einem Artikel schon? */
function ownedCount(user, item) {
  const state = shopState(user);
  if (item.kind === "stack") return state[item.id] || 0;
  if (item.kind === "unlock") return state.unlocks.includes(item.id) ? 1 : 0;
  return boostActive(user) ? 1 : 0;
}

function formatRemaining(ms) {
  const minutes = Math.max(0, Math.round(ms / 60000));
  if (minutes < 60) return `${minutes} Min`;
  const hours = Math.floor(minutes / 60);
  return `${hours} Std ${minutes % 60} Min`;
}

function Shop({ ctx }) {
  const { me, buyShopItem, pushToast } = ctx;
  const [busy, setBusy] = useState(null);

  const buy = async (item) => {
    setBusy(item.id);
    const ok = await buyShopItem(item.id);
    setBusy(null);
    if (ok) playSound("badge");
  };

  const boostLeft = boostActive(me) ? me.boostUntil - Date.now() : 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-bold flex items-center gap-2"><Store className="text-[#F7C948]" />XP-Shop</h1>
          <p className="text-[#8A9BC0] mt-1">Gesammelte XP ausgeben — jeder Artikel wirkt sich wirklich aus.</p>
        </div>
        <Card className="px-5 py-3">
          <p className="text-xs text-[#8A9BC0]">Dein Guthaben</p>
          <p className="font-display text-2xl font-bold text-[#F7C948] flex items-center gap-2">
            <Star size={18} />{xpBalance(me).toLocaleString("de-DE")} XP
          </p>
          <p className="text-[11px] text-[#4A5A7A]">insgesamt {(me.xp || 0).toLocaleString("de-DE")} XP verdient</p>
        </Card>
      </div>

      {boostLeft > 0 && (
        <div className="flex flex-wrap items-center gap-3 px-4 py-2.5 rounded-xl bg-[#EF4444]/10 border border-[#EF4444]/30">
          <Rocket size={16} className="text-[#EF4444]" />
          <p className="text-sm text-[#C9D6F0] flex-1">
            Doppelte XP sind aktiv — noch <strong>{formatRemaining(boostLeft)}</strong>.
          </p>
        </div>
      )}

      <div className="grid sm:grid-cols-2 gap-4">
        {SHOP_ITEMS.map((item) => {
          const owned = ownedCount(me, item);
          const maxed = item.kind === "unlock" ? owned > 0 : item.max ? owned >= item.max : false;
          const affordable = xpBalance(me) >= item.price;
          const Icon = item.icon;
          return (
            <Card key={item.id} className="p-5 flex flex-col">
              <div className="flex items-start gap-3 mb-3">
                <span className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0"
                  style={{ background: item.color + "1F", color: item.color }}>
                  <Icon size={18} />
                </span>
                <div className="flex-1 min-w-0">
                  <p className="font-display font-bold">{item.name}</p>
                  <p className="text-xs text-[#8A9BC0]">{item.short}</p>
                </div>
                {item.kind === "stack" && owned > 0 && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-[#10B981]/15 text-[#10B981] shrink-0">{owned}×</span>
                )}
                {item.kind === "unlock" && owned > 0 && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-[#10B981]/15 text-[#10B981] shrink-0">Freigeschaltet</span>
                )}
              </div>

              <p className="text-xs text-[#4A5A7A] leading-relaxed flex-1">{item.detail}</p>

              <div className="flex items-center gap-3 mt-4 pt-3 border-t border-[#1E2D4A]">
                <span className="flex items-center gap-1.5 font-semibold text-[#F7C948]"><Star size={14} />{item.price}</span>
                <span className="flex-1" />
                {maxed ? (
                  <span className="text-xs text-[#8A9BC0]">{item.kind === "unlock" ? "Gehört dir" : "Maximum erreicht"}</span>
                ) : (
                  <Btn size="sm" icon={busy === item.id ? undefined : Store} disabled={!affordable || busy === item.id}
                    onClick={() => buy(item)}>
                    {busy === item.id ? <><Loader2 size={13} className="ld-spin" />Kauft …</>
                      : affordable ? "Kaufen" : `${item.price - xpBalance(me)} XP fehlen`}
                  </Btn>
                )}
              </div>
            </Card>
          );
        })}
      </div>

      <Card className="p-5">
        <h2 className="font-display font-bold mb-2 flex items-center gap-2"><Info size={16} className="text-[#4F8EF7]" />XP verdienen</h2>
        <ul className="text-sm text-[#8A9BC0] space-y-1.5 leading-relaxed">
          <li>• Jede richtig gelöste Aufgabe bringt {TASK_XP} XP.</li>
          <li>• Für eine abgeschlossene Lektion kommen je nach Schwierigkeit 50 bis 150 XP dazu.</li>
          <li>• Mit doppelten XP zählt beides zweifach.</li>
          <li>• Ausgeben senkt nur dein Guthaben — dein Level und die Rangliste bleiben davon unberührt.</li>
        </ul>
      </Card>
    </div>
  );
}

/* ==================== Eigene Level (Lehrkräfte) ==========================
   Lehrkräfte bauen hier eigene Lektionen: Theorie als Markdown und dazu
   Aufgaben in denselben vier Formen, die auch die mitgelieferten Kurse
   nutzen. Geprüft wird später mit derselben lokalen Analyse — dadurch
   verhalten sich eigene Level exakt wie die eingebauten.
   ========================================================================= */
const TASK_TYPE_LABELS = {
  multiple_choice: "Multiple Choice",
  fill_blank: "Lückentext",
  code_write: "Code schreiben",
  explain: "Erklären",
};

const LEVEL_LABELS = {
  beginner: "Einsteiger", intermediate: "Fortgeschritten",
  advanced: "Profi", expert: "Experte",
};

function emptyTask(type, index) {
  const base = { id: `t${index + 1}`, type, question: "" };
  if (type === "multiple_choice") return { ...base, options: ["", ""], correctAnswer: 0, explanation: "" };
  if (type === "fill_blank") return { ...base, question: "Fülle die Lücken aus:", template: "", blanks: [] };
  if (type === "code_write") return { ...base, starterCode: "", expectedConcepts: [] };
  return { ...base, expectedConcepts: [] };
}

function emptyLesson() {
  return {
    id: null, title: "", courseId: "html", level: "beginner",
    xpReward: 50, theory: "", tasks: [], published: false,
  };
}

/** Findet Probleme, bevor gespeichert wird — dieselben Regeln wie im Backend. */
function lessonProblems(lesson) {
  const problems = [];
  if (!lesson.title.trim()) problems.push("Die Lektion braucht einen Titel.");
  lesson.tasks.forEach((task, i) => {
    const nr = i + 1;
    if (!task.question.trim()) problems.push(`Aufgabe ${nr}: Die Frage fehlt.`);
    if (task.type === "multiple_choice") {
      const options = task.options.filter((o) => o.trim());
      if (options.length < 2) problems.push(`Aufgabe ${nr}: Mindestens zwei Antwortmöglichkeiten.`);
      if (task.correctAnswer >= task.options.length || !task.options[task.correctAnswer]?.trim()) {
        problems.push(`Aufgabe ${nr}: Markiere die richtige Antwort.`);
      }
    }
    if (task.type === "fill_blank") {
      const gaps = (task.template || "").split("___").length - 1;
      if (!gaps) problems.push(`Aufgabe ${nr}: Im Satz fehlt mindestens eine Lücke (___).`);
      else if (gaps !== task.blanks.length) problems.push(`Aufgabe ${nr}: ${gaps} Lücken, aber ${task.blanks.length} Lösungen.`);
      else if (task.blanks.some((b) => !String(b).trim())) problems.push(`Aufgabe ${nr}: Eine Lösung ist leer.`);
    }
    if (task.type === "code_write" && !task.expectedConcepts.filter((c) => String(c).trim()).length) {
      problems.push(`Aufgabe ${nr}: Trag ein, was im Code vorkommen muss.`);
    }
  });
  if (lesson.published && !lesson.tasks.length) problems.push("Zum Veröffentlichen braucht es mindestens eine Aufgabe.");
  return problems;
}

/** Ein kleines beschriftetes Textfeld. */
function EditorField({ label, hint, children }) {
  return (
    <div>
      <label className="block text-xs text-[#8A9BC0] mb-1.5">{label}</label>
      {children}
      {hint && <p className="text-[11px] text-[#4A5A7A] mt-1">{hint}</p>}
    </div>
  );
}

const inputClass = "w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 text-sm text-[#E8EDF5]";

function TaskEditor({ task, index, onChange, onRemove, onMove, courseId }) {
  const set = (patch) => onChange({ ...task, ...patch });

  // Lückentext: die Zahl der Lösungen folgt automatisch den ___ im Satz.
  const syncBlanks = (template) => {
    const gaps = template.split("___").length - 1;
    const blanks = Array.from({ length: gaps }, (_, i) => task.blanks[i] ?? "");
    set({ template, blanks });
  };

  return (
    <Card className="p-4">
      <div className="flex flex-wrap items-center gap-2 mb-3">
        <span className="w-6 h-6 rounded-full bg-[#4F8EF7]/15 text-[#4F8EF7] text-xs flex items-center justify-center font-medium shrink-0">{index + 1}</span>
        <select value={task.type} onChange={(e) => onChange(emptyTask(e.target.value, index))}
          className="bg-[#0A0E1A] border border-[#1E2D4A] rounded-lg px-2 py-1.5 text-xs text-[#E8EDF5]">
          {Object.entries(TASK_TYPE_LABELS).map(([v, label]) => <option key={v} value={v}>{label}</option>)}
        </select>
        <div className="ml-auto flex items-center gap-1">
          <button onClick={() => onMove(-1)} aria-label="Nach oben" title="Nach oben"
            className="w-7 h-7 rounded-lg border border-[#1E2D4A] text-[#8A9BC0] hover:text-[#E8EDF5] flex items-center justify-center"><ChevronUp size={12} /></button>
          <button onClick={() => onMove(1)} aria-label="Nach unten" title="Nach unten"
            className="w-7 h-7 rounded-lg border border-[#1E2D4A] text-[#8A9BC0] hover:text-[#E8EDF5] flex items-center justify-center"><ChevronDown size={12} /></button>
          <button onClick={onRemove} aria-label="Aufgabe löschen" title="Löschen"
            className="w-7 h-7 rounded-lg border border-[#1E2D4A] text-[#8A9BC0] hover:text-[#EF4444] flex items-center justify-center"><Trash2 size={12} /></button>
        </div>
      </div>

      <div className="space-y-3">
        <EditorField label="Frage">
          <textarea value={task.question} onChange={(e) => set({ question: e.target.value })} rows={2}
            placeholder="Was sollen deine Schülerinnen und Schüler beantworten?" className={inputClass} />
        </EditorField>

        {task.type === "multiple_choice" && (
          <>
            <EditorField label="Antwortmöglichkeiten" hint="Klick auf den Kreis, um die richtige zu markieren.">
              <div className="space-y-2">
                {task.options.map((opt, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <button onClick={() => set({ correctAnswer: i })} aria-label={`Antwort ${i + 1} ist richtig`}
                      className={`w-5 h-5 rounded-full border-2 shrink-0 flex items-center justify-center ${task.correctAnswer === i ? "border-[#10B981] bg-[#10B981]/20" : "border-[#2A3F6F]"}`}>
                      {task.correctAnswer === i && <Check size={10} className="text-[#10B981]" />}
                    </button>
                    <input value={opt} placeholder={`Antwort ${i + 1}`} className={inputClass}
                      onChange={(e) => set({ options: Object.assign([...task.options], { [i]: e.target.value }) })} />
                    {task.options.length > 2 && (
                      <button onClick={() => set({
                        options: task.options.filter((_, k) => k !== i),
                        correctAnswer: task.correctAnswer > i ? task.correctAnswer - 1 : Math.min(task.correctAnswer, task.options.length - 2),
                      })} aria-label={`Antwort ${i + 1} entfernen`} className="text-[#4A5A7A] hover:text-[#EF4444] shrink-0"><X size={13} /></button>
                    )}
                  </div>
                ))}
              </div>
              {task.options.length < 6 && (
                <button onClick={() => set({ options: [...task.options, ""] })}
                  className="mt-2 text-xs text-[#4F8EF7] hover:underline flex items-center gap-1"><Plus size={11} />Antwort hinzufügen</button>
              )}
            </EditorField>
            <EditorField label="Erklärung (erscheint nach der richtigen Antwort)">
              <input value={task.explanation} onChange={(e) => set({ explanation: e.target.value })}
                placeholder="Warum ist das richtig?" className={inputClass} />
            </EditorField>
          </>
        )}

        {task.type === "fill_blank" && (
          <>
            <EditorField label="Satz mit Lücken" hint="Schreib ___ (drei Unterstriche) an jede Stelle, die ausgefüllt werden soll.">
              <textarea value={task.template} onChange={(e) => syncBlanks(e.target.value)} rows={2}
                placeholder="Eine ___ besteht aus Spalten und ___." className={`${inputClass} font-code`} />
            </EditorField>
            {task.blanks.length > 0 && (
              <EditorField label="Lösungen" hint="Mehrere zulässige Schreibweisen mit Komma trennen — „Tabelle, Relation“.">
                <div className="space-y-2">
                  {task.blanks.map((b, i) => (
                    <div key={i} className="flex items-center gap-2">
                      <span className="text-xs text-[#4A5A7A] w-14 shrink-0">Lücke {i + 1}</span>
                      <input value={Array.isArray(b) ? b.join(", ") : b} className={`${inputClass} font-code`}
                        onChange={(e) => {
                          const parts = e.target.value.split(",").map((v) => v.trim()).filter(Boolean);
                          const value = parts.length > 1 ? parts : e.target.value;
                          set({ blanks: Object.assign([...task.blanks], { [i]: value }) });
                        }} />
                    </div>
                  ))}
                </div>
              </EditorField>
            )}
          </>
        )}

        {task.type === "code_write" && (
          <>
            <EditorField label="Vorgegebener Code (optional)">
              <LdCodeEditor value={task.starterCode} onChange={(v) => set({ starterCode: v })}
                courseId={courseId} language={MONACO_LANG[courseId] || "plaintext"} label="Vorlage" height="120px" />
            </EditorField>
            <EditorField label="Das muss im Code vorkommen"
              hint="Mit Komma trennen. Alternativen in einer Zeile mit „|“ — etwa „cout|std::cout“.">
              <input value={(task.expectedConcepts || []).map((c) => (Array.isArray(c) ? c.join("|") : c)).join(", ")}
                onChange={(e) => set({
                  expectedConcepts: e.target.value.split(",").map((c) => c.trim()).filter(Boolean)
                    .map((c) => (c.includes("|") ? c.split("|").map((v) => v.trim()).filter(Boolean) : c)),
                })}
                placeholder="const, console.log, name" className={`${inputClass} font-code`} />
            </EditorField>
          </>
        )}

        {task.type === "explain" && (
          <EditorField label="Erwartete Fachbegriffe (optional)" hint="Mit Komma trennen — sie fließen in die Bewertung ein.">
            <input value={(task.expectedConcepts || []).join(", ")}
              onChange={(e) => set({ expectedConcepts: e.target.value.split(",").map((c) => c.trim()).filter(Boolean) })}
              placeholder="Primärschlüssel, eindeutig" className={inputClass} />
          </EditorField>
        )}
      </div>
    </Card>
  );
}

function LessonEditor({ ctx }) {
  const { me, myLessons, saveCustomLesson, deleteCustomLesson, pushToast, navigate, openLesson } = ctx;
  const [draft, setDraft] = useState(null);           // null = Übersicht
  const [saving, setSaving] = useState(false);

  const problems = draft ? lessonProblems(draft) : [];

  const startNew = () => setDraft(emptyLesson());
  const edit = (lesson) => setDraft({ ...lesson, tasks: lesson.tasks.map((t) => ({ ...t })) });

  const save = async (publish) => {
    const next = { ...draft, published: publish ?? draft.published };
    const found = lessonProblems(next);
    if (found.length) { pushToast("error", found[0]); return; }
    setSaving(true);
    const ok = await saveCustomLesson(next);
    setSaving(false);
    if (ok) { playSound("save"); setDraft(null); }
  };

  /* ------------------------------ Übersicht ------------------------------ */
  if (!draft) {
    return (
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="font-display text-3xl font-bold flex items-center gap-2"><Sparkles2 className="text-[#7C3AED]" />Eigene Level</h1>
            <p className="text-[#8A9BC0] mt-1">Baue Lektionen für deine Klasse — sie werden genauso geprüft wie die eingebauten.</p>
          </div>
          <Btn icon={Plus} onClick={startNew}>Neues Level</Btn>
        </div>

        {myLessons.length === 0 ? (
          <Card className="p-10 text-center">
            <Sparkles2 size={36} className="mx-auto text-[#2A3F6F] mb-3" />
            <p className="text-[#8A9BC0]">Noch kein eigenes Level.</p>
            <p className="text-sm text-[#4A5A7A] mt-1 mb-4">Theorie schreiben, Aufgaben anlegen, veröffentlichen — fertig.</p>
            <Btn icon={Plus} onClick={startNew}>Erstes Level anlegen</Btn>
          </Card>
        ) : (
          <Card className="divide-y divide-[#1E2D4A]">
            {myLessons.map((lesson) => {
              const course = courseById(lesson.courseId);
              return (
                <div key={lesson.id} className="flex flex-wrap items-center gap-3 p-4">
                  <span className="text-2xl shrink-0">{course?.icon || "📘"}</span>
                  <div className="flex-1 min-w-[180px]">
                    <p className="font-medium flex items-center gap-2 flex-wrap">
                      {lesson.title}
                      {lesson.published
                        ? <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#10B981]/15 text-[#10B981]">Veröffentlicht</span>
                        : <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#F59E0B]/15 text-[#F59E0B]">Entwurf</span>}
                    </p>
                    <p className="text-xs text-[#8A9BC0]">
                      {course?.name || lesson.courseId} · {LEVEL_LABELS[lesson.level] || lesson.level} ·{" "}
                      {lesson.tasks.length} Aufgabe{lesson.tasks.length === 1 ? "" : "n"} · {lesson.xpReward} XP
                    </p>
                  </div>
                  <Btn size="sm" variant="ghost" icon={Eye} onClick={() => openLesson(lesson.id)}>Ansehen</Btn>
                  <Btn size="sm" variant="secondary" icon={PenSquare} onClick={() => edit(lesson)}>Bearbeiten</Btn>
                  <Btn size="sm" variant="danger" icon={Trash2}
                    onClick={() => { if (window.confirm(`„${lesson.title}“ wirklich löschen?`)) deleteCustomLesson(lesson.id); }} />
                </div>
              );
            })}
          </Card>
        )}

        <Card className="p-5">
          <h2 className="font-display font-bold mb-2 flex items-center gap-2"><Info size={16} className="text-[#4F8EF7]" />So funktioniert es</h2>
          <ul className="text-sm text-[#8A9BC0] space-y-1.5 leading-relaxed">
            <li>• Ein Level besteht aus einem Theorieteil (Markdown) und beliebig vielen Aufgaben.</li>
            <li>• Veröffentlichte Level erscheinen bei allen, die deinen Schul-Code <span className="font-code text-[#F7C948]">{me.schoolCode}</span> genutzt haben.</li>
            <li>• Entwürfe sieht nur du — so kannst du in Ruhe vorbereiten.</li>
            <li>• Geprüft wird lokal im Browser: sofort, ohne Wartezeit und ohne KI.</li>
          </ul>
        </Card>
      </div>
    );
  }

  /* ------------------------------- Editor -------------------------------- */
  const setDraftField = (patch) => setDraft((d) => ({ ...d, ...patch }));

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button onClick={() => setDraft(null)} className="flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]">
          <ChevronLeft size={16} />Zurück zur Übersicht
        </button>
        <div className="flex flex-wrap gap-2">
          <Btn variant="secondary" size="sm" icon={Save} disabled={saving} onClick={() => save(false)}>Als Entwurf sichern</Btn>
          <Btn size="sm" icon={saving ? undefined : Check} disabled={saving || problems.length > 0} onClick={() => save(true)}>
            {saving ? <><Loader2 size={14} className="ld-spin" />Speichert …</> : "Veröffentlichen"}
          </Btn>
        </div>
      </div>

      {problems.length > 0 && (
        <Card className="p-4 border-[#F59E0B]/40">
          <p className="text-sm font-medium text-[#F59E0B] mb-1.5 flex items-center gap-2"><Info size={14} />Vor dem Veröffentlichen noch offen</p>
          <ul className="text-xs text-[#C9D6F0] space-y-0.5">
            {problems.map((p, i) => <li key={i}>• {p}</li>)}
          </ul>
        </Card>
      )}

      <Card className="p-5 space-y-4">
        <EditorField label="Titel">
          <input value={draft.title} onChange={(e) => setDraftField({ title: e.target.value })}
            placeholder="z.B. Primärschlüssel verstehen" className={inputClass} />
        </EditorField>

        <div className="grid sm:grid-cols-3 gap-3">
          <EditorField label="Sprache" hint="Bestimmt, wie Code geprüft wird.">
            <select value={draft.courseId} onChange={(e) => setDraftField({ courseId: e.target.value })} className={inputClass}>
              {COURSES.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </EditorField>
          <EditorField label="Schwierigkeit">
            <select value={draft.level} onChange={(e) => setDraftField({ level: e.target.value })} className={inputClass}>
              {Object.entries(LEVEL_LABELS).map(([v, label]) => <option key={v} value={v}>{label}</option>)}
            </select>
          </EditorField>
          <EditorField label="XP für den Abschluss">
            <input type="number" min="0" max="500" value={draft.xpReward}
              onChange={(e) => setDraftField({ xpReward: Math.max(0, Math.min(500, Number(e.target.value) || 0)) })}
              className={inputClass} />
          </EditorField>
        </div>

        <EditorField label="Theorie" hint="Markdown: # Überschrift, **fett**, `code`, ```sprache für Codeblöcke.">
          <textarea value={draft.theory} onChange={(e) => setDraftField({ theory: e.target.value })} rows={10}
            placeholder={"# Überschrift\n\nErkläre hier das Thema.\n\n```sql\nSELECT * FROM kunden;\n```"}
            className={`${inputClass} font-code text-[13px] leading-relaxed resize-y`} />
        </EditorField>
      </Card>

      <div className="flex items-center justify-between">
        <h2 className="font-display text-lg font-bold flex items-center gap-2"><ListChecks size={18} className="text-[#4F8EF7]" />Aufgaben</h2>
        <span className="text-xs text-[#8A9BC0]">{draft.tasks.length} von 20</span>
      </div>

      <div className="space-y-3">
        {draft.tasks.map((task, i) => (
          <TaskEditor key={i} task={task} index={i} courseId={draft.courseId}
            onChange={(next) => setDraftField({ tasks: Object.assign([...draft.tasks], { [i]: next }) })}
            onRemove={() => setDraftField({ tasks: draft.tasks.filter((_, k) => k !== i) })}
            onMove={(delta) => {
              const target = i + delta;
              if (target < 0 || target >= draft.tasks.length) return;
              const tasks = [...draft.tasks];
              [tasks[i], tasks[target]] = [tasks[target], tasks[i]];
              setDraftField({ tasks });
            }} />
        ))}
      </div>

      {draft.tasks.length < 20 && (
        <div className="flex flex-wrap gap-2">
          {Object.entries(TASK_TYPE_LABELS).map(([type, label]) => (
            <Btn key={type} size="sm" variant="secondary" icon={Plus}
              onClick={() => setDraftField({ tasks: [...draft.tasks, emptyTask(type, draft.tasks.length)] })}>
              {label}
            </Btn>
          ))}
        </div>
      )}

      {/* Vorschau der Theorie */}
      {draft.theory.trim() && (
        <div>
          <h2 className="font-display text-lg font-bold mb-3 flex items-center gap-2"><Eye size={18} className="text-[#8A9BC0]" />Vorschau</h2>
          <Card className="p-6"><Markdown text={draft.theory} /></Card>
        </div>
      )}
    </div>
  );
}

/* ========================= Admin-Dashboard ========================= */
function AdminDashboard({ ctx }) {
  const { me, users, reports, backend, adminUpdateUser, adminDeleteUser, adminCreateAdmin, adminSetPassword, resolveReport, deleteReport, pushToast } = ctx;
  const [tab, setTab] = useState("users");
  const [query, setQuery] = useState("");
  const [editUser, setEditUser] = useState(null);
  const [editForm, setEditForm] = useState(null);
  const [newAdminOpen, setNewAdminOpen] = useState(false);
  const [newAdminForm, setNewAdminForm] = useState({ name: "", email: "", password: "" });
  // Mit Server kommen Nutzer und Meldungen aus der Datenbank statt aus dem
  // lokalen Zustand — dort stünden nur die Konten dieses Browsers.
  const [remote, setRemote] = useState({ users: [], reports: [], stats: null, loading: !!backend });

  const reload = useCallback(async (search) => {
    if (!backend) return;
    setRemote((r) => ({ ...r, loading: true }));
    try {
      const [u, rep, st] = await Promise.all([
        api.get(`/api/admin/users${search ? `?q=${encodeURIComponent(search)}` : ""}`),
        api.get("/api/admin/reports"),
        api.get("/api/admin/stats").catch(() => null),
      ]);
      setRemote({ users: u.users || [], reports: rep.reports || [], stats: st, loading: false });
    } catch (e) {
      setRemote((r) => ({ ...r, loading: false }));
      pushToast("error", e.message);
    }
  }, [backend, pushToast]);

  useEffect(() => { reload(); }, [reload]);

  // Suche serverseitig ausführen, aber erst nach kurzer Pause beim Tippen.
  useEffect(() => {
    if (!backend) return;
    const t = setTimeout(() => reload(query.trim()), 300);
    return () => clearTimeout(t);
  }, [query, backend, reload]);

  const q = query.trim().toLowerCase();
  const list = backend
    ? remote.users
    : users.filter((u) => !u.isGuest && (!q || u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)));
  const allReports = backend ? remote.reports : reports;
  const openReports = allReports.filter((r) => r.status === "open");
  const resolvedReports = allReports.filter((r) => r.status !== "open");

  const [newPw, setNewPw] = useState("");

  const openEdit = (u) => { setEditUser(u); setEditForm({ name: u.name, email: u.email, role: u.role }); setNewPw(""); };

  // Administratoren dürfen jedes Passwort neu setzen — das Konto wird dabei
  // überall abgemeldet, damit ein übernommenes Konto sofort dicht ist.
  const applyNewPassword = async () => {
    if (backend) {
      try {
        await api.patch(`/api/admin/users/${editUser.id}`, { password: newPw });
        pushToast("success", "Passwort gesetzt — das Konto wurde überall abgemeldet.");
        setNewPw("");
      } catch (e) { pushToast("error", e.message); }
      return;
    }
    const ok = await adminSetPassword(editUser.id, newPw);
    if (ok) setNewPw("");
  };

  const saveEdit = async () => {
    if (backend) {
      try {
        await api.patch(`/api/admin/users/${editUser.id}`, editForm);
        pushToast("success", "Nutzer aktualisiert.");
        setEditUser(null);
        await reload(query.trim());
      } catch (e) { pushToast("error", e.message); }
      return;
    }
    adminUpdateUser(editUser.id, editForm);
    setEditUser(null);
  };

  const removeUser = async (id) => {
    if (backend) {
      try {
        await api.del(`/api/admin/users/${id}`);
        pushToast("info", "Konto gelöscht.");
        await reload(query.trim());
      } catch (e) { pushToast("error", e.message); }
      return;
    }
    adminDeleteUser(id);
  };

  const createAdmin = async () => {
    if (backend) {
      try {
        await api.post("/api/admin/users", { ...newAdminForm, role: "admin" });
        pushToast("success", `Admin-Konto für ${newAdminForm.name} erstellt.`);
        setNewAdminOpen(false);
        setNewAdminForm({ name: "", email: "", password: "" });
        await reload(query.trim());
      } catch (e) { pushToast("error", e.message); }
      return;
    }
    if (await adminCreateAdmin(newAdminForm)) {
      setNewAdminOpen(false);
      setNewAdminForm({ name: "", email: "", password: "" });
    }
  };

  const setReportStatus = async (id, status) => {
    if (backend) {
      try { await api.patch(`/api/admin/reports/${id}`, { status }); await reload(query.trim()); }
      catch (e) { pushToast("error", e.message); }
      return;
    }
    resolveReport(id);
  };

  const removeReport = async (id) => {
    if (backend) {
      try { await api.del(`/api/admin/reports/${id}`); await reload(query.trim()); }
      catch (e) { pushToast("error", e.message); }
      return;
    }
    deleteReport(id);
  };

  const roleBadge = (role) => {
    const map = { admin: ["#7C3AED", "Admin"], teacher: ["#4F8EF7", "Lehrer"], student: ["#10B981", "Schüler"] };
    const [color, label] = map[role] || ["#8A9BC0", role];
    return <span className="text-[11px] font-medium px-2 py-0.5 rounded-full" style={{ color, background: color + "22" }}>{label}</span>;
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-bold flex items-center gap-2"><Shield className="text-[#7C3AED]" />Admin-Bereich</h1>
        <p className="text-[#8A9BC0] mt-1">Angemeldet als {me.name}</p>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <StatCard icon={Users} label="Registrierte Accounts" value={backend ? (remote.stats?.users.total ?? list.length) : users.filter((u) => !u.isGuest).length} color="#4F8EF7" />
        <StatCard icon={Shield} label="Admins" value={backend ? (remote.stats?.users.admins ?? list.filter((u) => u.role === "admin").length) : users.filter((u) => u.role === "admin").length} color="#7C3AED" />
        <StatCard icon={FileText} label="Offene Meldungen" value={openReports.length} color="#EF4444" />
      </div>

      <div className="flex p-1 bg-[#0A0E1A] rounded-lg max-w-sm">
        <button onClick={() => setTab("users")} className={`flex-1 py-2 rounded-md text-sm font-medium transition-all ${tab === "users" ? "text-white" : "text-[#8A9BC0]"}`} style={tab === "users" ? { background: GRADIENT } : undefined}>Nutzer</button>
        <button onClick={() => setTab("reports")} className={`flex-1 py-2 rounded-md text-sm font-medium transition-all ${tab === "reports" ? "text-white" : "text-[#8A9BC0]"}`} style={tab === "reports" ? { background: GRADIENT } : undefined}>Meldungen{openReports.length > 0 ? ` (${openReports.length})` : ""}</button>
      </div>

      {tab === "users" ? (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-3">
            <div className="relative flex-1 min-w-[200px]">
              <Eye size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#4A5A7A]" />
              <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Nutzer suchen (Name oder E-Mail) …"
                className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 pl-9 text-sm text-[#E8EDF5] placeholder:text-[#4A5A7A]" />
            </div>
            <Btn icon={UserRoundPlus} onClick={() => setNewAdminOpen(true)}>Admin erstellen</Btn>
          </div>
          <Card className="overflow-x-auto">
            <table className="w-full text-sm min-w-[600px]">
              <thead><tr className="text-left text-[#8A9BC0] border-b border-[#1E2D4A]">
                <th className="px-4 py-3 font-medium">Name</th><th className="px-4 py-3 font-medium">E-Mail</th>
                <th className="px-4 py-3 font-medium">Rolle</th><th className="px-4 py-3 font-medium"></th>
              </tr></thead>
              <tbody>
                {list.map((u) => (
                  <tr key={u.id} className="border-b border-[#1E2D4A]/50 last:border-0 hover:bg-white/5">
                    <td className="px-4 py-3"><span className="flex items-center gap-2"><span className="w-7 h-7 rounded-lg overflow-hidden flex items-center justify-center shrink-0"><UserAvatar user={u} size={28} /></span><span className="font-medium">{u.name}</span></span></td>
                    <td className="px-4 py-3 text-[#8A9BC0]">{u.email}</td>
                    <td className="px-4 py-3">{roleBadge(u.role)}</td>
                    <td className="px-4 py-3 text-right space-x-2 whitespace-nowrap">
                      <button onClick={() => openEdit(u)} className="text-[#4F8EF7] hover:underline text-xs">Bearbeiten</button>
                      {u.id !== me.id && <button onClick={() => removeUser(u.id)} className="text-[#EF4444] hover:underline text-xs">Löschen</button>}
                    </td>
                  </tr>
                ))}
                {list.length === 0 && <tr><td colSpan={4} className="px-4 py-8 text-center text-[#4A5A7A]">Keine Nutzer gefunden.</td></tr>}
              </tbody>
            </table>
          </Card>
        </div>
      ) : (
        <div className="space-y-3">
          {reports.length === 0 && (
            <Card className="p-10 text-center"><FileText size={36} className="mx-auto text-[#4A5A7A] mb-3" /><p className="text-[#8A9BC0]">Noch keine Meldungen.</p></Card>
          )}
          {[...openReports, ...resolvedReports].map((r) => (
            <Card key={r.id} className={`p-4 ${r.status === "open" ? "" : "opacity-60"}`}>
              <div className="flex items-start justify-between gap-3 mb-2">
                <div>
                  <p className="text-sm font-medium">{r.type === "ai_answer" ? "KI-Antwort gemeldet" : "Inhalt gemeldet"}</p>
                  <p className="text-xs text-[#8A9BC0]">von {r.reporterName} · {r.createdAt} · {r.lessonTitle || ""}</p>
                </div>
                <span className={`text-[11px] px-2 py-0.5 rounded-full ${r.status === "open" ? "bg-[#F59E0B]/15 text-[#F59E0B]" : "bg-[#10B981]/15 text-[#10B981]"}`}>{r.status === "open" ? "Offen" : "Erledigt"}</span>
              </div>
              {r.question && <p className="text-xs text-[#8A9BC0] mb-1"><strong className="text-[#C9D6F0]">Aufgabe:</strong> {r.question}</p>}
              {r.userAnswer && <p className="text-xs text-[#8A9BC0] mb-1"><strong className="text-[#C9D6F0]">Antwort:</strong> {r.userAnswer}</p>}
              {r.aiFeedback && <p className="text-xs text-[#8A9BC0] mb-1"><strong className="text-[#C9D6F0]">KI-Feedback:</strong> {r.aiFeedback}</p>}
              {r.reason && <p className="text-xs text-[#8A9BC0] mb-2"><strong className="text-[#C9D6F0]">Grund:</strong> {r.reason}</p>}
              <div className="flex gap-2 mt-2">
                {r.status === "open" && <Btn size="sm" variant="secondary" icon={Check} onClick={() => setReportStatus(r.id, "resolved")}>Als erledigt markieren</Btn>}
                <Btn size="sm" variant="danger" icon={Trash2} onClick={() => removeReport(r.id)}>Löschen</Btn>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Nutzer bearbeiten */}
      {editUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" onClick={() => setEditUser(null)}>
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
          <Card className="relative z-10 p-6 max-w-sm w-full" onClick={(e) => e.stopPropagation()}>
            <button onClick={() => setEditUser(null)} className="absolute top-4 right-4 text-[#8A9BC0] hover:text-[#E8EDF5]"><X size={20} /></button>
            <h3 className="font-display text-lg font-bold mb-4">Nutzer bearbeiten</h3>
            <div className="space-y-3">
              <Field label="Name" value={editForm.name} onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))} />
              <Field label="E-Mail" value={editForm.email} onChange={(e) => setEditForm((f) => ({ ...f, email: e.target.value }))} />
              <div>
                <label className="block text-sm text-[#8A9BC0] mb-1.5">Rolle</label>
                <select value={editForm.role} onChange={(e) => setEditForm((f) => ({ ...f, role: e.target.value }))}
                  className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 text-[#E8EDF5]">
                  <option value="student">Schüler</option>
                  <option value="teacher">Lehrer</option>
                  <option value="admin">Admin</option>
                </select>
              </div>
            </div>
            <Btn className="w-full mt-4" icon={Check} onClick={saveEdit}>Speichern</Btn>

            <div className="mt-5 pt-4 border-t border-[#1E2D4A]">
              <p className="text-sm font-medium flex items-center gap-2 mb-1"><KeyRound size={15} className="text-[#F59E0B]" />Passwort neu setzen</p>
              <p className="text-xs text-[#8A9BC0] mb-2">
                Das alte Passwort wird nicht gebraucht. Alle Anmeldungen dieses Kontos werden beendet.
              </p>
              <input type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)} placeholder="Neues Passwort"
                className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 text-sm text-[#E8EDF5]" />
              <PasswordHints password={newPw} name={editForm.name} email={editForm.email} />
              <Btn size="sm" variant="secondary" className="mt-3" icon={KeyRound} onClick={applyNewPassword}
                disabled={!passwordOk(newPw, { name: editForm.name, email: editForm.email })}>
                Passwort setzen
              </Btn>
            </div>
          </Card>
        </div>
      )}

      {/* Neuen Admin erstellen */}
      {newAdminOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4" onClick={() => setNewAdminOpen(false)}>
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
          <Card className="relative z-10 p-6 max-w-sm w-full" onClick={(e) => e.stopPropagation()}>
            <button onClick={() => setNewAdminOpen(false)} className="absolute top-4 right-4 text-[#8A9BC0] hover:text-[#E8EDF5]"><X size={20} /></button>
            <h3 className="font-display text-lg font-bold mb-4 flex items-center gap-2"><Shield className="text-[#7C3AED]" size={18} />Neuen Admin erstellen</h3>
            <div className="space-y-3">
              <Field label="Name" value={newAdminForm.name} onChange={(e) => setNewAdminForm((f) => ({ ...f, name: e.target.value }))} />
              <Field label="E-Mail" value={newAdminForm.email} onChange={(e) => setNewAdminForm((f) => ({ ...f, email: e.target.value }))} />
              <div>
                <Field label="Passwort" type="password" value={newAdminForm.password} onChange={(e) => setNewAdminForm((f) => ({ ...f, password: e.target.value }))} />
                <PasswordHints password={newAdminForm.password} name={newAdminForm.name} email={newAdminForm.email} />
              </div>
            </div>
            <Btn className="w-full mt-4" icon={Check} onClick={createAdmin}>Admin erstellen</Btn>
          </Card>
        </div>
      )}
    </div>
  );
}

/* ========================= Einstufungstest ===============================
   Wer den Stoff eines Moduls schon kann, soll ihn nicht durchklicken müssen.
   Der Test zieht je Lektion eine Aufgabe und schaltet bei Erfolg das ganze
   Modul frei.

   Die Regeln sind bewusst streng:
   • **Ein** Versuch je Aufgabe. Kein Nachbessern — sonst wäre es kein Test.
   • **Alles** muss stimmen. Wer etwas überspringt, sollte es wirklich können.
   • Es gibt **halbe XP**. Die Lektionen zählen als abgeschlossen, aber geübt
     hat man sie nicht.
   • Bei Nichtbestehen wird **keine Lösung verraten** — sonst könnte man sich
     durch Wiederholen durchprobieren.
   ========================================================================= */
const PLACEMENT_XP_SHARE = 0.5;      // halbe XP fürs Überspringen
const PLACEMENT_MAX_TASKS = 6;

/**
 * Stellt den Test für ein Modul zusammen: je Lektion eine Aufgabe, dabei
 * bevorzugt solche, bei denen man wirklich etwas schreiben muss.
 */
function buildPlacementTest(course, mod) {
  const RANK = { code_write: 0, fill_blank: 1, explain: 2, multiple_choice: 3 };
  const picked = [];

  for (const meta of mod.lessons) {
    const lesson = getFullLesson(meta.id);
    if (!lesson?.tasks?.length) continue;
    const best = [...lesson.tasks].sort((a, b) => (RANK[a.type] ?? 9) - (RANK[b.type] ?? 9))[0];
    picked.push({ ...best, id: `${meta.id}__${best.id}`, _lessonId: meta.id, _lessonTitle: meta.title });
    if (picked.length >= PLACEMENT_MAX_TASKS) break;
  }
  return picked;
}

/** Wie viele XP das Überspringen einbringt. */
function placementXp(mod) {
  return Math.max(1, Math.round(mod.lessons.reduce((sum, l) => sum + (l.xpReward || 0), 0) * PLACEMENT_XP_SHARE));
}

function PlacementTest({ ctx }) {
  const { placementTarget, setPlacementTarget, me, navigate, openCourse, completeLesson, celebrate, pushToast } = ctx;

  const course = placementTarget ? courseById(placementTarget.courseId) : null;
  const mod = course ? course.modules.find((m) => m.id === placementTarget.moduleId) : null;

  const [tasks] = useState(() => (mod ? buildPlacementTest(course, mod) : []));
  const [idx, setIdx] = useState(0);
  const [answers, setAnswers] = useState({});
  const [locked, setLocked] = useState({});      // Aufgabe -> richtig/falsch, nur einmal
  const [done, setDone] = useState(false);
  const [saving, setSaving] = useState(false);

  if (!course || !mod || !tasks.length) return null;

  const task = tasks[idx];
  const isLast = idx === tasks.length - 1;
  const answered = locked[task.id] !== undefined;
  const correctCount = Object.values(locked).filter(Boolean).length;
  const allAnswered = tasks.every((t) => locked[t.id] !== undefined);
  const passed = allAnswered && correctCount === tasks.length;
  const reward = placementXp(mod);

  const setAns = (val) => setAnswers((a) => ({ ...a, [task.id]: val }));

  const check = () => {
    if (answered) return;
    const ans = answers[task.id];
    let ok = false;

    if (task.type === "multiple_choice") {
      if (ans == null) { pushToast("error", "Bitte wähle eine Antwort."); return; }
      ok = ans === task.correctAnswer;
    } else if (task.type === "fill_blank") {
      if (!(ans || []).some((v) => (v || "").trim())) { pushToast("error", "Bitte fülle die Lücken aus."); return; }
      ok = evaluateFillBlank(task, ans).correct;
    } else {
      if (!String(ans || "").trim()) { pushToast("error", "Bitte gib zuerst eine Antwort ein."); return; }
      ok = analyzeAnswer(task, ans, course.id).correct;
    }

    setLocked((l) => ({ ...l, [task.id]: ok }));
    playSound(ok ? "correct" : "wrong");
    // Im Test gibt es keine Rückmeldung zum Inhalt — nur, ob es gezählt hat.
    pushToast(ok ? "success" : "error", ok ? "Gezählt." : "Diese Aufgabe zählt als nicht bestanden.");
  };

  const finish = async () => {
    setDone(true);
    if (!passed) {
      playSound("wrong");
      return;
    }
    setSaving(true);
    // Jede Lektion des Moduls einzeln gutschreiben — mit halber Belohnung.
    const open = mod.lessons.filter((l) => !me.completedLessons.includes(l.id));
    for (const l of open) {
      await completeLesson(l.id, Math.max(1, Math.round((l.xpReward || 0) * PLACEMENT_XP_SHARE)), {
        firstTry: 1, taskCount: 1,
      });
    }
    setSaving(false);
    playSound("lessonComplete");
    celebrate();
    pushToast("success", `Bestanden! ${open.length} Lektion${open.length === 1 ? "" : "en"} freigeschaltet.`);
  };

  const leave = () => { setPlacementTarget(null); openCourse(course.id); };

  /* ------------------------------ Ergebnis ------------------------------- */
  if (done) {
    return (
      <div className="min-h-screen bg-[#0A0E1A] flex items-center justify-center px-5 py-12">
        <Card className="p-8 max-w-lg w-full text-center">
          <div className="flex justify-center mb-4">
            <LdIcon name={passed ? "crown" : "ziel"} size={44} color={passed ? "#10B981" : "#F59E0B"} />
          </div>
          <h1 className="font-display text-2xl font-bold mb-2">
            {passed ? "Bestanden!" : "Noch nicht bestanden"}
          </h1>
          <p className="text-[#8A9BC0] mb-1">
            {correctCount} von {tasks.length} Aufgaben richtig.
          </p>
          <p className="text-sm text-[#4A5A7A] mb-6 leading-relaxed">
            {passed
              ? `Modul „${mod.title}“ ist freigeschaltet — du hast ${reward} XP bekommen (die Hälfte, weil du die Übungen übersprungen hast).`
              : "Zum Überspringen muss jede Aufgabe sitzen. Arbeite das Modul durch — danach kannst du es jederzeit erneut versuchen."}
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            <Btn icon={ArrowRight} disabled={saving} onClick={leave}>
              {saving ? <><Loader2 size={15} className="ld-spin" />Speichert …</> : "Zum Kurs"}
            </Btn>
            {!passed && (
              <Btn variant="secondary" icon={Play} onClick={() => { setDone(false); setIdx(0); setLocked({}); setAnswers({}); }}>
                Test wiederholen
              </Btn>
            )}
          </div>
        </Card>
      </div>
    );
  }

  /* -------------------------------- Test --------------------------------- */
  return (
    <div className="min-h-screen bg-[#0A0E1A]">
      <header className="sticky top-0 z-40 backdrop-blur-md bg-[#0A0E1A]/90 border-b border-[#1E2D4A]">
        <div className="max-w-3xl mx-auto px-4 lg:px-6 h-14 flex items-center gap-3">
          <button onClick={leave} className="flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5] shrink-0">
            <ArrowLeft size={16} /><span className="hidden sm:inline">Abbrechen</span>
          </button>
          <div className="flex-1 min-w-0 text-sm text-[#8A9BC0] truncate flex items-center gap-1.5">
            <CourseIcon course={course} size={15} />
            <span className="truncate">Einstufungstest · {mod.title}</span>
          </div>
          <span className="text-sm text-[#8A9BC0] shrink-0">{idx + 1} / {tasks.length}</span>
        </div>
        <div className="h-1 bg-[#1A2540]">
          <div className="h-1 transition-all duration-500"
            style={{ width: (Object.keys(locked).length / tasks.length) * 100 + "%", background: GRADIENT }} />
        </div>
      </header>

      <div className="max-w-3xl mx-auto px-4 lg:px-6 py-6 space-y-5">
        <Card className="p-4 flex flex-wrap items-center gap-3 border-[#F59E0B]/30">
          <LdIcon name="ziel" size={18} color="#F59E0B" />
          <p className="text-sm text-[#C9D6F0] flex-1">
            <strong>Ein Versuch je Aufgabe.</strong> Alles muss stimmen — dafür sparst du dir {mod.lessons.length} Lektionen und bekommst {reward} XP.
          </p>
        </Card>

        <Card className="p-5">
          <p className="text-[11px] uppercase tracking-wider text-[#4A5A7A] mb-2">Aus: {task._lessonTitle}</p>
          <p className="font-medium text-[#E8EDF5] mb-4">{renderInline(task.question, "pq")}</p>

          {task.type === "multiple_choice" && (
            <div className="space-y-2">
              {task.options.map((opt, i) => {
                const sel = answers[task.id] === i;
                return (
                  <button key={i} disabled={answered} onClick={() => setAns(i)}
                    className={`w-full text-left px-4 py-3 rounded-lg border transition-all flex items-center gap-3 ${
                      sel ? "border-[#4F8EF7] bg-[#4F8EF7]/10" : "border-[#1E2D4A] hover:border-[#2A3F6F]"}`}>
                    <span className="w-6 h-6 rounded-full border border-current text-[#8A9BC0] flex items-center justify-center text-xs shrink-0">
                      {String.fromCharCode(65 + i)}
                    </span>
                    <span className="text-sm text-[#E8EDF5] flex-1">{opt}</span>
                  </button>
                );
              })}
            </div>
          )}

          {task.type === "code_write" && (
            <LdCodeEditor value={answers[task.id] || ""} onChange={setAns} disabled={answered}
              courseId={course.id} label={course.name} height="320px" wordWrap="on" />
          )}

          {task.type === "fill_blank" && (
            <div className="text-sm leading-loose text-[#C9D6F0]">
              {task.template.split("___").map((seg, i) => (
                <React.Fragment key={i}>
                  {seg}
                  {i < task.blanks.length && (
                    <input value={answers[task.id]?.[i] || ""} disabled={answered}
                      onChange={(e) => setAns(Object.assign([...(answers[task.id] || [])], { [i]: e.target.value }))}
                      className="inline-block w-24 mx-1 px-2 py-0.5 rounded bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] font-code text-center text-[#4F8EF7]"
                      placeholder="…" />
                  )}
                </React.Fragment>
              ))}
            </div>
          )}

          {task.type === "explain" && (
            <textarea value={answers[task.id] || ""} onChange={(e) => setAns(e.target.value)} rows={5} disabled={answered}
              className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 text-sm text-[#E8EDF5] resize-y leading-relaxed"
              placeholder="Schreibe deine Erklärung …" />
          )}

          <div className="mt-4 flex gap-2">
            {!answered
              ? <Btn className="flex-1" icon={Send} onClick={check}>Antwort abgeben</Btn>
              : isLast
                ? <Btn className="flex-1" icon={Trophy} onClick={finish}>Test auswerten</Btn>
                : <Btn className="flex-1" icon={ArrowRight} onClick={() => setIdx((i) => i + 1)}>Nächste Aufgabe</Btn>}
          </div>

          {answered && (
            <p className="mt-3 text-center text-xs text-[#4A5A7A]">
              Abgegeben. Im Test gibt es keine Auflösung — das Ergebnis siehst du am Ende.
            </p>
          )}
        </Card>

        <div className="flex items-center justify-center gap-1.5">
          {tasks.map((t, i) => (
            <span key={t.id}
              className={`h-2 rounded-full transition-all ${
                i === idx ? "w-6 bg-[#4F8EF7]"
                : locked[t.id] !== undefined ? "w-2 bg-[#8A9BC0]"
                : "w-2 bg-[#2A3F6F]"}`} />
          ))}
        </div>
      </div>
    </div>
  );
}

/* =========================== Lesson View ========================== */
const TASK_XP = 15;

/* --------------------- XP nach Anzahl der Versuche ------------------------
   Wer eine Aufgabe im ersten Anlauf löst, bekommt die volle Belohnung. Jeder
   weitere Versuch senkt sie — aber nie auf null: Wer es beim vierten Mal
   versteht, hat es trotzdem verstanden.

   Dieselben Zahlen stehen im Backend (server/src/routes/app.js). Der Server
   rechnet selbst nach und deckelt das Ergebnis; der Browser kann sich
   dadurch nicht mehr geben, als ehrlich möglich wäre.
   ------------------------------------------------------------------------- */
const ATTEMPT_FACTORS = [1, 0.7, 0.5, 0.3];      // 1., 2., 3., ab dem 4. Versuch
const HINT_FACTOR_CAP = 0.4;                      // mit Tipp-Joker höchstens 40 %

/** XP für eine gelöste Aufgabe. */
function taskXpFor(attempts, usedHint = false) {
  const index = Math.min(Math.max(1, Number(attempts) || 1), ATTEMPT_FACTORS.length) - 1;
  const factor = usedHint ? Math.min(ATTEMPT_FACTORS[index], HINT_FACTOR_CAP) : ATTEMPT_FACTORS[index];
  return Math.max(1, Math.round(TASK_XP * factor));
}

/**
 * Anteil des Lektionsbonus. Volle Punkte gibt es nur, wenn jede Aufgabe im
 * ersten Anlauf saß; darunter bleibt mindestens die Hälfte.
 */
function lessonBonusFactor(firstTry, total) {
  if (!total) return 1;
  const share = Math.max(0, Math.min(1, firstTry / total));
  return 0.5 + 0.5 * share;
}

function lessonXpFor(base, firstTry, total) {
  return Math.max(1, Math.round((Number(base) || 0) * lessonBonusFactor(firstTry, total)));
}

/** Kurze Begründung für die Anzeige — „2. Versuch“, „mit Tipp-Joker“. */
function attemptLabel(attempts, usedHint) {
  if (usedHint) return "mit Tipp-Joker";
  if (attempts <= 1) return "erster Versuch";
  return `${attempts}. Versuch`;
}

function AIFeedback({ result, ctx, reportPayload }) {
  const [reportOpen, setReportOpen] = useState(false);
  const [reason, setReason] = useState("");
  if (!result) return null;
  const good = result.correct;
  const submitReport = () => {
    ctx.reportContent({ type: "ai_answer", reason: reason.trim() || "Kein Grund angegeben", ...reportPayload, aiFeedback: result.feedback });
    setReportOpen(false); setReason("");
  };
  return (
    <Card className="p-5 mt-5" >
      <div className="flex items-center gap-2 mb-3 pb-3 border-b border-[#1E2D4A]">
        <ListChecks size={18} className="text-[#10B981]" />
        <span className="font-display font-bold">Bewertung</span>
        <span className="ml-auto text-[10px] px-2 py-0.5 rounded-full bg-[#10B981]/15 text-[#10B981]">Sofort geprüft</span>
      </div>
      <div className="flex items-center gap-2 mb-3">
        {good ? <CheckCircle2 size={20} className="text-[#10B981]" /> : <XCircle size={20} className="text-[#EF4444]" />}
        <span className={`font-semibold ${good ? "text-[#10B981]" : "text-[#EF4444]"}`}>{good ? "Richtig!" : "Noch nicht ganz"}</span>
        {typeof result.score === "number" && <span className="text-sm text-[#8A9BC0]">(Score: {result.score}/100)</span>}
      </div>
      <p className="text-sm text-[#C9D6F0] leading-relaxed mb-2">{renderInline(result.feedback || "", "fb")}</p>
      {good && result.praise && <p className="text-sm text-[#10B981] mb-2">🎉 {result.praise}</p>}
      {!good && result.hint && <p className="text-sm text-[#F59E0B] flex items-start gap-1.5 mb-2"><span>💡</span><span>{renderInline(result.hint, "hint")}</span></p>}
      {good && result.hint && <p className="text-xs text-[#8A9BC0] flex items-start gap-1.5 mb-2"><span>💡</span><span>{renderInline(result.hint, "hint2")}</span></p>}

      {/* Aufschlüsselung der geprüften Bausteine — macht nachvollziehbar,
          warum die Bewertung so ausfällt. */}
      {result.details?.concepts?.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mt-3">
          {result.details.concepts.map((c, i) => (
            <span key={i} className={`text-[10px] font-code px-2 py-0.5 rounded-full flex items-center gap-1 ${c.hit ? "bg-[#10B981]/15 text-[#10B981]" : "bg-[#EF4444]/15 text-[#EF4444]"}`}>
              {c.hit ? <Check size={9} /> : <X size={9} />}{c.concept}
            </span>
          ))}
        </div>
      )}

      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-3 pt-3 border-t border-[#1E2D4A]">
        {ctx && !reportOpen && (
          <button onClick={() => setReportOpen(true)} className="text-xs text-[#8A9BC0] hover:text-[#EF4444] flex items-center gap-1.5">
            <Flag size={12} />Diese Bewertung melden
          </button>
        )}
        {ctx && (
          <button onClick={() => ctx.navigate("playground")} className="text-xs text-[#4F8EF7] hover:underline flex items-center gap-1.5">
            <Bot size={12} />Im Editor mit der KI besprechen
          </button>
        )}
      </div>

      {reportOpen && (
        <div className="mt-3 p-3 rounded-lg bg-[#0A0E1A] border border-[#1E2D4A]">
          <p className="text-xs text-[#8A9BC0] mb-2">Was stimmt mit dieser Bewertung nicht?</p>
          <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={2} placeholder="z.B. Meine Antwort war eigentlich richtig …"
            className="w-full bg-[#141D35] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2 text-xs text-[#E8EDF5] resize-y mb-2" />
          <div className="flex gap-2">
            <Btn size="sm" variant="danger" icon={Flag} onClick={submitReport}>Melden</Btn>
            <Btn size="sm" variant="ghost" onClick={() => setReportOpen(false)}>Abbrechen</Btn>
          </div>
        </div>
      )}
    </Card>
  );
}

function LessonView({ ctx }) {
  const { selectedLesson, navigate, openCourse, me, addXP, showXP, completeLesson, celebrate, pushToast, logout, findLesson, useHint } = ctx;
  const lesson = findLesson(selectedLesson);
  const [idx, setIdx] = useState(0);
  const [answers, setAnswers] = useState({});
  const [results, setResults] = useState({});
  const [rewarded, setRewarded] = useState({});
  const [attempts, setAttempts] = useState({});
  const [hintUsed, setHintUsed] = useState({});
  const [aiLoading, setAiLoading] = useState(false);
  const alreadyDone = lesson ? me.completedLessons.includes(lesson.id) : false;

  // Antworten initialisieren, wenn Lektion wechselt
  useEffect(() => {
    if (!lesson) return;
    const init = {};
    lesson.tasks.forEach((t) => {
      if (t.type === "code_write") init[t.id] = t.starterCode || "";
      else if (t.type === "fill_blank") init[t.id] = (t.blanks || []).map(() => "");
      else if (t.type === "multiple_choice") init[t.id] = null;
      else init[t.id] = "";
    });
    setAnswers(init); setResults({}); setRewarded({}); setAttempts({}); setHintUsed({}); setIdx(0);
  }, [selectedLesson]);

  if (!lesson) return null;
  const task = lesson.tasks[idx];
  const result = results[task.id];
  const isLast = idx === lesson.tasks.length - 1;
  // Abgeschlossen ist eine Lektion erst, wenn jede Aufgabe RICHTIG beantwortet
  // wurde — eine bloß geprüfte falsche Antwort reicht nicht.
  const isSolved = (t) => !!results[t.id]?.correct;
  const allSolved = lesson.tasks.every(isSolved);
  const openCount = lesson.tasks.filter((t) => !isSolved(t)).length;

  const setAns = (val) => setAnswers((a) => ({ ...a, [task.id]: val }));

  // Gelöst = richtig geprüft. Eine falsche Antwort sperrt nichts — man
  // korrigiert direkt im Feld und drückt erneut auf „Prüfen“.
  const solved = !!result?.correct;
  const wasWrong = !!result && !result.correct;

  // Tipp-Joker aus dem Shop: zeigt die Auflösung sofort statt erst nach
  // drei Fehlversuchen.
  const canSpendHint = wasWrong && !!result.solutionHint && result.hint !== result.solutionHint && (me.hints || 0) > 0;
  const spendHint = async () => {
    if (!canSpendHint) return;
    const ok = await useHint();
    if (!ok) return;
    setResults((r) => ({ ...r, [task.id]: { ...result, hint: result.solutionHint } }));
    setHintUsed((h) => ({ ...h, [task.id]: true }));
    playSound("badge");
  };

  /**
   * Schreibt die XP für eine gelöste Aufgabe gut — einmal je Aufgabe und
   * abhängig davon, im wievielten Anlauf sie saß.
   */
  const reward = (tid, tries) => {
    if (rewarded[tid]) return 0;
    const usedHint = !!hintUsed[tid];
    const amount = taskXpFor(tries, usedHint);
    setRewarded((r) => ({ ...r, [tid]: true }));
    addXP(amount, { attempts: tries, usedHint });
    showXP(amount);
    return amount;
  };

  // Was es beim nächsten richtigen Versuch gäbe — das steht sichtbar dabei,
  // damit niemand raten muss, was ein weiterer Fehlversuch kostet.
  const pendingReward = taskXpFor((attempts[task.id] || 0) + 1, !!hintUsed[task.id]);

  const submit = async () => {
    const ans = answers[task.id];
    if (task.type === "multiple_choice") {
      if (ans == null) { pushToast("error", "Bitte wähle eine Antwort."); return; }
      const correct = ans === task.correctAnswer;
      const mcTries = (attempts[task.id] || 0) + 1;
      setAttempts((a) => ({ ...a, [task.id]: mcTries }));
      setResults((r) => ({ ...r, [task.id]: { correct, score: correct ? 100 : 0, feedback: task.explanation } }));
      playSound(correct ? "correct" : "wrong");
      if (correct) {
        const gained = reward(task.id, mcTries);
        pushToast("success", `Richtig! +${gained} XP (${attemptLabel(mcTries, false)})`);
      } else {
        pushToast("error", "Nicht ganz — versuch es nochmal!");
      }
      return;
    }

    // Alle anderen Aufgabentypen (Lückentext, Code, Erklären) werden geprüft —
    // vollständig lokal — die KI ist daran nicht beteiligt.
    let checkTask = task, checkAnswer = ans;
    if (task.type === "fill_blank") {
      if (!(ans || []).some((v) => (v || "").trim())) { pushToast("error", "Bitte fülle mindestens eine Lücke aus."); return; }
      const filled = task.template.split("___").reduce((acc, seg, i) => acc + seg + (i < task.blanks.length ? `[${(ans[i] || "").trim() || "___"}]` : ""), "");
      checkTask = { ...task, expectedConcepts: task.blanks, _blankAnswers: ans, question: `${task.question} Satz: "${filled}"` };
      checkAnswer = filled;
    } else if (!String(ans || "").trim()) {
      pushToast("error", "Bitte gib zuerst eine Antwort ein."); return;
    }

    // Lektionen werden ausschließlich lokal bewertet: sofort, kostenlos und
    // ohne Netzwerk. Die KI sitzt stattdessen als Assistent im Code-Editor.
    const res = analyzeAnswer(checkTask, checkAnswer, lesson._course.id);
    const tries = (attempts[task.id] || 0) + 1;
    setAttempts((a) => ({ ...a, [task.id]: tries }));
    // Nach drei Fehlversuchen darf die Lösung stehen — vorher gibt es nur
    // Anhaltspunkte, damit man nicht einfach durchprobiert.
    const shown = !res.correct && res.solutionHint && tries >= 3
      ? { ...res, hint: res.solutionHint }
      : res;
    setResults((r) => ({ ...r, [task.id]: shown }));
    playSound(res.correct ? "correct" : "wrong");

    if (res.correct) {
      const gained = reward(task.id, tries);
      pushToast("success", `Richtig! +${gained} XP (${attemptLabel(tries, !!hintUsed[task.id])})`);
      if (res.score >= 95 && !me.badges.includes("ai_master")) {
        setTimeout(() => pushToast("badge", `Neues Abzeichen: ${BADGES.ai_master.label}!`), 400);
      }
    } else {
      pushToast("error", "Versuch es nochmal — du schaffst das!");
    }
  };


  // Eigene Level gehören zu keinem eingebauten Kurs — von dort geht es zurück
  // zur Übersicht statt in einen Kurs.
  const leaveLesson = () => {
    if (lesson.isCustom) navigate(me.role === "teacher" ? "lesson-editor" : "dashboard");
    else openCourse(lesson._course.id);
  };

  // Wie viele Aufgaben saßen im ersten Anlauf — ohne Joker?
  const firstTryCount = lesson.tasks.filter((t) => (attempts[t.id] || 0) <= 1 && !hintUsed[t.id]).length;
  const lessonBonus = lessonXpFor(lesson.xpReward, firstTryCount, lesson.tasks.length);

  const finish = () => {
    // Ohne gelöste Aufgaben gibt es keine XP — sonst könnte man sich die
    // Belohnung durch bloßes Weiterklicken abholen.
    if (!allSolved) {
      const next = lesson.tasks.findIndex((t) => !isSolved(t));
      pushToast("error", `Noch ${openCount} Aufgabe${openCount === 1 ? "" : "n"} offen — die musst du zuerst lösen.`);
      if (next >= 0) {
        const openId = lesson.tasks[next].id;
        setResults((r) => { const n = { ...r }; delete n[openId]; return n; });   // erneut versuchen
        setIdx(next);
      }
      return;
    }
    if (!alreadyDone) {
      const total = lesson.tasks.length;
      completeLesson(lesson.id, lessonBonus, { firstTry: firstTryCount, taskCount: total });
      playSound("lessonComplete");
      celebrate();
      setTimeout(() => { showXP(lessonBonus); }, 200);
      pushToast("success", lessonBonus === lesson.xpReward
        ? `Fehlerfrei! Volle ${lessonBonus} XP 🎉`
        : `Lektion abgeschlossen! +${lessonBonus} von ${lesson.xpReward} XP — ${firstTryCount} von ${total} im ersten Anlauf.`);
      setTimeout(leaveLesson, 1400);
    } else {
      pushToast("info", "Lektion bereits abgeschlossen.");
      leaveLesson();
    }
  };

  // Tastatur-Shortcuts: 1–4 wählt Multiple-Choice-Option, Enter prüft/weiter
  useEffect(() => {
    const onKey = (e) => {
      const tag = (e.target.tagName || "").toLowerCase();
      if (tag === "textarea" || tag === "input") return;
      if (task.type === "multiple_choice" && !solved && /^[1-9]$/.test(e.key)) {
        const n = parseInt(e.key, 10) - 1;
        if (n < task.options.length) { e.preventDefault(); setAns(n); }
      } else if (e.key === "Enter") {
        e.preventDefault();
        if (!solved && !aiLoading) submit();
        else if (solved) { isLast ? finish() : setIdx((i) => i + 1); }
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  return (
    <div className="min-h-screen bg-[#0A0E1A]">
      {/* Header */}
      <header className="sticky top-0 z-40 backdrop-blur-md bg-[#0A0E1A]/90 border-b border-[#1E2D4A]">
        <div className="max-w-6xl mx-auto px-4 lg:px-6 h-14 flex items-center gap-3">
          <button onClick={leaveLesson} className="flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5] shrink-0"><ArrowLeft size={16} /><span className="hidden sm:inline">Zurück</span></button>
          <div className="flex-1 min-w-0 text-sm text-[#8A9BC0] truncate flex items-center gap-1.5">
            <CourseIcon course={lesson._course} size={15} />
            <span>{lesson.isCustom ? "Eigenes Level" : lesson._course.name}</span>
            <span className="mx-1 text-[#4A5A7A]">/</span><span className="text-[#E8EDF5] truncate">{lesson.title}</span>
          </div>
          <span className="hidden sm:inline text-sm text-[#8A9BC0] shrink-0 whitespace-nowrap">Aufgabe {idx + 1} von {lesson.tasks.length}</span>
          <button onClick={logout} aria-label="Abmelden" title="Abmelden"
            className="shrink-0 w-8 h-8 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#EF4444] hover:text-[#EF4444] flex items-center justify-center text-[#8A9BC0]">
            <LogOut size={14} />
          </button>
        </div>
        <div className="h-1 bg-[#1A2540]"><div className="h-1 transition-all duration-500" style={{ width: (lesson.tasks.filter(isSolved).length / lesson.tasks.length) * 100 + "%", background: GRADIENT }} /></div>
      </header>

      <div className="max-w-7xl mx-auto px-4 lg:px-6 py-6 grid lg:grid-cols-5 gap-6">
        {/* Theorie */}
        <div className="lg:col-span-2">
          <div className="flex items-center gap-2 mb-3 text-[#8A9BC0]"><BookOpen size={18} /><span className="font-display font-bold text-[#E8EDF5]">Theorie</span><DifficultyBadge level={lesson.level} /><span className="ml-auto flex items-center gap-1 text-xs"><Clock size={13} />{lesson.estimatedMinutes} Min</span></div>
          <Card className="p-6"><Markdown text={lesson.theory} /></Card>
        </div>

        {/* Aufgabe */}
        <div className="lg:col-span-3">
          <div className="lg:sticky lg:top-20">
            <div className="flex items-center gap-2 mb-3 text-[#8A9BC0]"><PenLine size={18} /><span className="font-display font-bold text-[#E8EDF5]">Aufgabe</span><span className="ml-auto flex items-center gap-1 text-xs text-[#F7C948]"><Star size={13} />{lesson.xpReward} XP</span></div>
            <Card className="p-5">
              <p className="font-medium text-[#E8EDF5] mb-4">{renderInline(task.question, "q")}</p>

              {/* Multiple Choice */}
              {task.type === "multiple_choice" && (
                <div className="space-y-2">
                  {task.options.map((opt, i) => {
                    const sel = answers[task.id] === i;
                    // Nach einer falschen Antwort bleibt die Auswahl offen — man
                    // wählt einfach neu, statt erst „Nochmal“ drücken zu müssen.
                    // Die richtige Lösung wird dabei bewusst nicht verraten.
                    const isCorrect = i === task.correctAnswer;
                    let cls = "border-[#1E2D4A] hover:border-[#2A3F6F]";
                    if (solved && isCorrect) cls = "border-[#10B981] bg-[#10B981]/10";
                    else if (wasWrong && sel) cls = "border-[#EF4444] bg-[#EF4444]/10";
                    else if (sel) cls = "border-[#4F8EF7] bg-[#4F8EF7]/10";
                    return (
                      <button key={i} disabled={solved} onClick={() => setAns(i)}
                        className={`w-full text-left px-4 py-3 rounded-lg border transition-all flex items-center gap-3 ${cls}`}>
                        <span className="w-6 h-6 rounded-full border border-current text-[#8A9BC0] flex items-center justify-center text-xs shrink-0">{String.fromCharCode(65 + i)}</span>
                        <span className="text-sm text-[#E8EDF5] flex-1">{opt}</span>
                        {solved && isCorrect && <CheckCircle2 size={18} className="text-[#10B981]" />}
                        {wasWrong && sel && <XCircle size={18} className="text-[#EF4444]" />}
                      </button>
                    );
                  })}
                </div>
              )}

              {/* Code schreiben — im eingebauten Editor */}
              {task.type === "code_write" && (
                <LdCodeEditor value={answers[task.id] || ""} onChange={setAns} disabled={solved}
                  courseId={lesson._course.id} label={lesson._course.name} height="420px" wordWrap="on" />
              )}

              {/* Lückentext */}
              {task.type === "fill_blank" && (
                <div className="text-sm leading-loose text-[#C9D6F0]">
                  {task.template.split("___").map((seg, i) => (
                    <React.Fragment key={i}>
                      {seg}
                      {i < task.blanks.length && (
                        <input value={answers[task.id]?.[i] || ""} disabled={solved}
                          onChange={(e) => setAns(Object.assign([...(answers[task.id] || [])], { [i]: e.target.value }))}
                          className="inline-block w-24 mx-1 px-2 py-0.5 rounded bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] font-code text-center text-[#4F8EF7]" placeholder="…" />
                      )}
                    </React.Fragment>
                  ))}
                </div>
              )}

              {/* Erklären */}
              {task.type === "explain" && (
                <textarea value={answers[task.id] || ""} onChange={(e) => setAns(e.target.value)} rows={5}
                  className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-3 text-sm text-[#E8EDF5] resize-y leading-relaxed" placeholder="Schreibe deine Erklärung …" />
              )}

              {/* Aktionen */}
              <div className="mt-4 flex gap-2">
                {!solved && (
                  <Btn className="flex-1" onClick={submit} disabled={aiLoading} icon={aiLoading ? undefined : Send}>
                    {aiLoading ? <><Loader2 size={16} className="ld-spin" />Prüft …</> : wasWrong ? "Erneut prüfen" : "Prüfen"}
                  </Btn>
                )}
                {solved && (isLast
                  ? (allSolved
                      ? <Btn className="flex-1" onClick={finish} icon={Trophy}>Lektion abschließen</Btn>
                      : <Btn className="flex-1" variant="secondary" onClick={finish} icon={ArrowRight}>
                          Noch {openCount} Aufgabe{openCount === 1 ? "" : "n"} offen
                        </Btn>)
                  : <Btn className="flex-1" onClick={() => setIdx((i) => i + 1)} icon={ArrowRight}>Nächste Aufgabe</Btn>)}
              </div>

              {!solved && (
                <p className="mt-2 text-center text-[11px] text-[#4A5A7A]">
                  {wasWrong
                    ? <>Bei richtiger Antwort jetzt <span className="text-[#F7C948]">+{pendingReward} XP</span> statt {TASK_XP} — jeder Fehlversuch kostet.</>
                    : <>Richtig im ersten Anlauf: <span className="text-[#F7C948]">+{TASK_XP} XP</span></>}
                </p>
              )}

              {canSpendHint && (
                <button onClick={spendHint}
                  className="mt-3 w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg border border-[#F7C948]/40 bg-[#F7C948]/10 text-xs text-[#F7C948] hover:bg-[#F7C948]/15">
                  <Lightbulb size={13} />Tipp-Joker einlösen — Lösung sofort zeigen, dafür höchstens {Math.round(HINT_FACTOR_CAP * 100)} % XP ({me.hints} übrig)
                </button>
              )}

              {/* MC-Feedback; Lückentext, Code und Erklären erscheinen in der Bewertungskarte unten */}
              {result && task.type === "multiple_choice" && (
                <div className={`mt-4 p-3 rounded-lg text-sm ${result.correct ? "bg-[#10B981]/10 text-[#10B981]" : "bg-[#EF4444]/10 text-[#C9D6F0]"}`}>
                  <div className="flex items-center gap-1.5 font-medium mb-1">{result.correct ? <CheckCircle2 size={15} /> : <XCircle size={15} className="text-[#EF4444]" />}{result.correct ? "Richtig!" : "Leider falsch"}</div>
                  {/* Die Erklärung verrät die Lösung — deshalb erst, wenn sie stimmt. */}
                  <p className="text-[#C9D6F0]">{result.correct ? result.feedback : "Wähl eine andere Antwort und prüfe erneut."}</p>
                </div>
              )}
            </Card>

            {/* KI-Feedback (Lückentext, Code, Erklären) */}
            {/* Der Platzhalter erscheint nur, solange noch gar kein Ergebnis
                vorliegt — sonst würde er das Sofortergebnis verdecken. */}
            {aiLoading && !result && task.type !== "multiple_choice" && <SkeletonFeedback />}
            {result && task.type !== "multiple_choice" && (
              <AIFeedback result={result} ctx={ctx} reportPayload={{
                lessonTitle: `${lesson._course.name} · ${lesson.title}`,
                question: task.question,
                userAnswer: String(Array.isArray(answers[task.id]) ? answers[task.id].join(", ") : (answers[task.id] || "")).slice(0, 400),
              }} />
            )}

            {/* Tastatur-Hinweis */}
            <p className="text-center text-[11px] text-[#4A5A7A] mt-3">
              {task.type === "multiple_choice" && !solved ? "Tipp: Tasten 1–4 zum Wählen · " : ""}<kbd className="font-code px-1 py-0.5 rounded bg-[#141D35] border border-[#1E2D4A]">Enter</kbd> zum {solved ? "Weiter" : "Prüfen"}
            </p>

            {/* Task-Navigation Punkte */}
            <div className="flex items-center justify-center gap-1.5 mt-5">
              {lesson.tasks.map((t, i) => (
                <button key={t.id} onClick={() => setIdx(i)}
                  className={`h-2 rounded-full transition-all ${i === idx ? "w-6 bg-[#4F8EF7]" : isSolved(t) ? "w-2 bg-[#10B981]" : results[t.id] ? "w-2 bg-[#EF4444]" : "w-2 bg-[#2A3F6F]"}`}
                  title={`Aufgabe ${i + 1}${isSolved(t) ? " — gelöst" : results[t.id] ? " — noch offen" : ""}`} />
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}








