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
  first_lesson: { emoji: "🎯", label: "Erste Lektion", desc: "Erste Lektion abgeschlossen" },
  week_warrior: { emoji: "🔥", label: "7-Tage Streak", desc: "Eine Woche am Stück gelernt" },
  js_beginner: { emoji: "⚡", label: "JS Starter", desc: "10 JavaScript-Lektionen" },
  course_complete: { emoji: "🏆", label: "Kurs-Meister", desc: "Einen ganzen Kurs abgeschlossen" },
  ai_master: { emoji: "🤖", label: "KI-Master", desc: "KI-Score über 95%" },
  mid_wizard: { emoji: "🌟", label: "Mid Wizard", desc: "Level 10 erreicht" },
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
  const modules = mods.map((m, mi) => {
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
    { title: "Grundlagen", level: "beginner", lessons: ['Java Setup & "Hello World"', "Variablen & primitive Datentypen", "Operatoren & Ausdrücke", "Bedingungen & Schleifen", "Arrays"] },
    { title: "OOP", level: "intermediate", lessons: ["Klassen & Objekte", "Konstruktoren & this", "Vererbung & super", "Interfaces & abstrakte Klassen", "Packages & Import"] },
    { title: "Advanced", level: "advanced", lessons: ["Collections (ArrayList, HashMap)", "Generics", "Exception Handling", "File I/O", "Lambda & Streams"] },
    { title: "Expert", level: "expert", lessons: ["Multithreading & Concurrency", "Design Patterns", "Spring Framework Einführung"] },
  ]),
  buildCourse("python", "Python", "🐍", "#3572A5", "Der einfache Einstieg — bis hin zu Data Science.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Python installieren & Hello World", "Variablen & Datentypen", "Strings & String-Methoden", "Listen, Tupel, Sets", "Dictionaries"] },
    { title: "Intermediate", level: "intermediate", lessons: ["Funktionen & Parameter", "Module & pip", "Datei-Operationen", "OOP in Python", "List Comprehensions"] },
    { title: "Advanced", level: "advanced", lessons: ["Decorators", "Generators & Iterators", "Fehlerbehandlung", "Virtual Environments", "APIs mit requests"] },
    { title: "Expert", level: "expert", lessons: ["Async Python (asyncio)", "Testing mit pytest", "Data Science Einführung (numpy/pandas)"] },
  ]),
  buildCourse("sql", "SQL", "🗄️", "#E38C00", "Datenbanken verstehen — Abfragen, Joins & Performance.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Was sind Datenbanken?", "SELECT & FROM", "WHERE & Bedingungen", "ORDER BY & LIMIT"] },
    { title: "Intermediate", level: "intermediate", lessons: ["INSERT, UPDATE, DELETE", "CREATE TABLE & Datentypen", "JOINs (INNER, LEFT, RIGHT)", "Aggregat-Funktionen", "GROUP BY & HAVING"] },
    { title: "Advanced", level: "advanced", lessons: ["Subqueries", "Indizes & Performance", "Transaktionen", "Views & Stored Procedures"] },
  ]),
  buildCourse("cpp", "C++", "⚙️", "#00599C", "Nah am Metal — Pointer, Templates & Systemnähe.", [
    { title: "Grundlagen", level: "beginner", lessons: ["C++ Grundstruktur & Kompilierung", "Variablen, Typen, Ein/Ausgabe", "Operatoren & Ausdrücke", "Kontrollstrukturen"] },
    { title: "Intermediate", level: "intermediate", lessons: ["Funktionen & Überladen", "Arrays & Strings", "Pointer & Referenzen", "Klassen & OOP", "Vererbung & Polymorphismus"] },
    { title: "Advanced", level: "advanced", lessons: ["Templates", "STL (vector, map, algorithm)", "Speicherverwaltung (new/delete)", "Smart Pointer"] },
    { title: "Expert", level: "expert", lessons: ["Move Semantics & Rvalue", "Multithreading (std::thread)", "Systemnahe Programmierung"] },
  ]),
  buildCourse("c", "C", "🔩", "#5C6BC0", "Die Mutter aller Sprachen — Speicher, Pointer & Systemnähe.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Aufbau eines C-Programms & Kompilieren", "Variablen & Datentypen", "Operatoren & Ausdrücke", "Kontrollstrukturen", "Funktionen"] },
    { title: "Speicher & Daten", level: "intermediate", lessons: ["Arrays", "Zeiger (Pointer) verstehen", "Strings in C", "Structs & Unions", "Dynamische Speicherverwaltung (malloc/free)"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Datei-Ein-/Ausgabe", "Präprozessor & Makros", "Modularisierung & Header", "Verkettete Listen"] },
    { title: "Expert", level: "expert", lessons: ["Bit-Operationen", "Funktionszeiger", "Systemnahe Programmierung & Syscalls"] },
  ]),
  buildCourse("typescript", "TypeScript", "🛡️", "#3178C6", "JavaScript mit Typsicherheit — weniger Bugs, besserer Code.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Was ist TypeScript? Setup & tsc", "Basistypen & Type Annotations", "Arrays, Tupel & Enums", "Funktionen typisieren"] },
    { title: "Typsystem", level: "intermediate", lessons: ["Interfaces & Type Aliases", "Union & Intersection Types", "Optional & Readonly", "Type Narrowing & Guards", "Klassen in TypeScript"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Generics", "Utility Types (Partial, Pick, Omit)", "Module & Namespaces", "Typisierung von APIs"] },
    { title: "Expert", level: "expert", lessons: ["Conditional Types", "Mapped Types & Template Literal Types", "Declaration Files (.d.ts)"] },
  ]),
  buildCourse("react", "React", "⚛️", "#61DAFB", "Moderne Benutzeroberflächen mit Komponenten & Hooks.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Was ist React? Erste Komponente", "JSX verstehen", "Props & Komponenten-Komposition", "State mit useState", "Events behandeln"] },
    { title: "Hooks & Logik", level: "intermediate", lessons: ["Listen & Keys", "Bedingtes Rendering", "useEffect & Seiteneffekte", "Formulare & kontrollierte Inputs", "Eigene Hooks schreiben"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Context API", "useReducer & komplexer State", "Performance (memo, useMemo, useCallback)", "Daten laden & Fehlerbehandlung"] },
    { title: "Expert", level: "expert", lessons: ["React Router & Navigation", "Testing mit React Testing Library", "Patterns & Architektur größerer Apps"] },
  ]),
  buildCourse("vue", "Vue", "💚", "#42B883", "Das progressive Framework — sanfter Einstieg, volle Power.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Vue einbinden & erste App", "Template-Syntax & Interpolation", "Direktiven (v-if, v-for, v-bind)", "Events mit v-on", "Reaktivität mit ref & reactive"] },
    { title: "Komponenten", level: "intermediate", lessons: ["Komponenten & Props", "Emits & Kommunikation", "Slots", "Computed & Watch", "Formulare mit v-model"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Composition API vertiefen", "Lifecycle Hooks", "Vue Router", "State Management mit Pinia"] },
  ]),
  buildCourse("go", "Go", "🐹", "#00ADD8", "Einfach, schnell, nebenläufig — die Sprache der Cloud.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Go installieren & Hello World", "Variablen, Typen & Konstanten", "Kontrollstrukturen", "Funktionen & Mehrfachrückgabe", "Arrays, Slices & Maps"] },
    { title: "Strukturen", level: "intermediate", lessons: ["Structs & Methoden", "Interfaces", "Fehlerbehandlung mit error", "Packages & Module", "Zeiger in Go"] },
    { title: "Nebenläufigkeit", level: "advanced", lessons: ["Goroutines", "Channels", "select & sync", "Testing in Go"] },
    { title: "Expert", level: "expert", lessons: ["HTTP-Server bauen", "Context & Timeouts", "Performance & Profiling"] },
  ]),
  buildCourse("kotlin", "Kotlin", "🟣", "#7F52FF", "Modernes JVM — prägnant, sicher, Android-first.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Kotlin Setup & Hello World", "val, var & Datentypen", "Null-Sicherheit verstehen", "Kontrollfluss & when", "Funktionen & Default-Parameter"] },
    { title: "OOP & Funktional", level: "intermediate", lessons: ["Klassen & Konstruktoren", "Data Classes", "Vererbung & Interfaces", "Collections & Lambdas", "Extension Functions"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Sealed Classes & Pattern Matching", "Generics", "Coroutines — Grundlagen", "Scope Functions (let, apply, run)"] },
    { title: "Expert", level: "expert", lessons: ["Coroutines & Flow vertiefen", "DSLs bauen", "Android-Grundlagen mit Kotlin"] },
  ]),
  buildCourse("rust", "Rust", "🦀", "#DEA584", "Sicher, schnell, ohne Garbage Collector.", [
    { title: "Grundlagen", level: "beginner", lessons: ["Rust installieren & Cargo", "Variablen & Mutability", "Datentypen & Tupel", "Kontrollfluss", "Funktionen"] },
    { title: "Ownership", level: "intermediate", lessons: ["Ownership verstehen", "Borrowing & Referenzen", "Slices", "Structs & Methoden", "Enums & Pattern Matching"] },
    { title: "Fortgeschritten", level: "advanced", lessons: ["Fehlerbehandlung mit Result & Option", "Generics & Traits", "Lifetimes", "Collections (Vec, HashMap)", "Module & Crates"] },
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

/* ----------------------------- Demo Users ------------------------------ */
// 2,5 GB simuliertes Speicherkontingent pro Account für Playground-Projekte.
// Echtes localStorage fasst real nur wenige MB — dies ist eine UX-Anzeige/Obergrenze,
// keine tatsächliche Festplatten-Reservierung (dafür bräuchte es einen Server).
const STORAGE_QUOTA_BYTES = 2.5 * 1024 * 1024 * 1024;

const DEMO_USERS = [
  {
    id: "teacher_1", role: "teacher", name: "Prof. Anna Schmidt", email: "lehrer@demo.de",
    password: "lehrer123", school: "Gymnasium Berlin-Mitte", schoolCode: "LRND-4K9M",
    students: ["student_1", "student_2", "student_3"], createdAt: "2024-01-01", avatar: "👩‍🏫",
    emailVerified: true, twoFactorEnabled: false, playground: [],
  },
  {
    id: "student_1", role: "student", name: "Max Müller", email: "max@demo.de", password: "schueler123",
    teacherId: "teacher_1", xp: 1250, streak: 7, completedLessons: ["html_1_1", "html_1_2", "html_1_3", "javascript_1_1", "javascript_1_2"],
    currentCourse: "javascript", joinedAt: "2024-01-15", lastLogin: "Heute", avatar: "🧑‍💻", badges: ["first_lesson", "week_warrior"],
    emailVerified: true, twoFactorEnabled: false, playground: [],
  },
  {
    id: "student_2", role: "student", name: "Sarah Becker", email: "sarah@demo.de", password: "schueler123",
    teacherId: "teacher_1", xp: 870, streak: 3, completedLessons: ["html_1_1", "html_1_2", "css_1_1"],
    currentCourse: "html", joinedAt: "2024-02-02", lastLogin: "Vor 2 Std.", avatar: "👩‍💻", badges: ["first_lesson"],
    emailVerified: true, twoFactorEnabled: false, playground: [],
  },
  {
    id: "student_3", role: "student", name: "Tom Weber", email: "tom@demo.de", password: "schueler123",
    teacherId: "teacher_1", xp: 2100, streak: 14,
    completedLessons: ["html_1_1", "html_1_2", "html_1_3", "html_1_4", "html_2_1", "javascript_1_1", "javascript_1_2", "javascript_1_3"],
    currentCourse: "javascript", joinedAt: "2024-01-08", lastLogin: "Gestern", avatar: "👨‍🎓", badges: ["first_lesson", "week_warrior", "js_beginner"],
    emailVerified: true, twoFactorEnabled: false, playground: [],
  },
  {
    id: "admin_1", role: "admin", name: "System-Administrator", email: "admin@demo.de", password: "admin123",
    createdAt: "2024-01-01", avatar: "🛡️", emailVerified: true, twoFactorEnabled: false,
  },
];

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
          "einmal"
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

// Alle handgemachten Inhalte zusammenführen
const LESSON_CONTENT = { ...EXTRA_LESSONS, ...BASE_LESSONS };

// Fallback-Lektion, falls keine handgemachten Inhalte vorliegen
function buildFallbackLesson(course, meta) {
  const title = meta.lesson.title;
  return {
    estimatedMinutes: 10,
    theory: `# ${title}

Willkommen zu dieser Lektion im Kurs **${course.name}** ${course.icon}.

In diesem Abschnitt vertiefst du das Thema **„${title}“**. Lies die Konzepte aufmerksam, baue die Beispiele selbst nach und teste dein Verständnis mit den Aufgaben rechts.

> 💡 **Tipp:** Aktives Ausprobieren bringt dich beim Programmieren am schnellsten voran. Schreib Code mit, statt ihn nur zu lesen.

## Lernziele

- Die Kernideen hinter *${title}* verstehen
- Eigene kleine Beispiele formulieren
- Das Gelernte in eigenen Worten erklären können`,
    tasks: [
      {
        id: "g1", type: "multiple_choice",
        question: `Wie lernst du das Thema „${title}“ am effektivsten?`,
        options: [
          "Nur die Überschriften überfliegen",
          "Theorie lesen, Beispiele selbst nachbauen und üben",
          "Direkt zur nächsten Lektion springen",
          "Die Aufgaben überspringen",
        ],
        correctAnswer: 1,
        explanation: "Aktives Üben und das Nachbauen von Beispielen festigt dein Wissen am besten.",
        aiCheck: false,
      },
      {
        id: "g2", type: "explain",
        question: `Erkläre in eigenen Worten, was du unter „${title}“ verstehst und warum es im ${course.name}-Kontext wichtig ist.`,
        aiCheck: true,
      },
    ],
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
// unterstützt. Ohne Key läuft automatisch die lokale Offline-Heuristik.
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
  const c = String(concept).trim();
  const lc = c.toLowerCase();
  const code = analysis.stripped;
  const lower = code.toLowerCase();

  // 1. Operatoren und Symbole
  if (/^[=+\-*/<>!%&|.;:()[\]{}]+$/.test(c)) {
    return { hit: code.includes(c), concept: c, kind: "operator" };
  }

  // 2. Sammelbegriffe
  const group = CONCEPT_GROUPS[lc];
  if (group) {
    if (group.kind === "string") {
      return { hit: analysis.hasString, concept: c, kind: "string",
        why: analysis.hasString ? null : "Es fehlt eine Zeichenkette in Anführungszeichen." };
    }
    if (group.kind === "number") {
      return { hit: analysis.hasNumber, concept: c, kind: "number",
        why: analysis.hasNumber ? null : "Es fehlt eine Zahl." };
    }
    const hit = (group.any || []).some((a) => lower.includes(a.toLowerCase()));
    return { hit, concept: c, kind: group.kind,
      why: hit ? null : `Kein passendes Sprachmittel gefunden (erwartet z.B. ${group.any.slice(0, 3).join(", ")}).` };
  }

  // 3. HTML-Tags — nur, wenn es wirklich ein Tag-Name ist. Sonst würde ein
  //    erwarteter Textinhalt wie "Willkommen" fälschlich als <willkommen>
  //    gesucht.
  if (analysis.profile.blockStyle === "tags") {
    const tag = lc.replace(/[<>/]/g, "");
    const isTagSyntax = /^<\/?\w/.test(c);      // ausdrücklich als <tag> geschrieben
    if (isTagSyntax || HTML_TAGS.has(tag)) {
      const opened = new RegExp(`<${tag}[\\s>]`, "i").test(code);
      const closed = new RegExp(`</${tag}>`, "i").test(code);
      const isVoid = HTML_VOID_TAGS.has(tag);
      if (opened && (closed || isVoid)) return { hit: true, concept: c, kind: "tag", essential: true };
      if (opened && !closed) return { hit: false, concept: c, kind: "tag", essential: true, why: `<${tag}> wird geöffnet, aber nie geschlossen.` };
      return { hit: false, concept: c, kind: "tag", essential: true, why: `Das Element <${tag}> fehlt.` };
    }
    // Kein Tag -> erwarteter Textinhalt
    const inText = analysis.raw.toLowerCase().includes(lc);
    return { hit: inText, concept: c, kind: "text", essential: false,
      why: inText ? null : `Der Text „${c}“ kommt nicht vor.` };
  }

  // 4. Bezeichner und Schlüsselwörter — als ganzes Wort.
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

  // 5. Mehrwort-Beschreibung: es genügt ein sinntragender Teil
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

/* --------------------- Bewertung: Lückentext ----------------------------- */
function evaluateFillBlank(task, answers) {
  const blanks = task.blanks || [];
  const results = blanks.map((expected, i) => {
    const given = String(answers?.[i] || "").trim();
    const target = String(expected).trim();
    if (!given) return { ok: false, expected: target, given, reason: "leer" };
    if (given.toLowerCase() === target.toLowerCase()) {
      return { ok: true, expected: target, given, caseOff: given !== target };
    }
    // Klammern, Anführungszeichen und Satzzeichen tolerieren: <strong> == strong
    if (normalizeAlnum(given) === normalizeAlnum(target)) {
      return { ok: true, expected: target, given, formatted: true };
    }
    // Tippfehler?
    const d = editDistance(given.toLowerCase(), target.toLowerCase());
    return { ok: false, expected: target, given, typo: d <= Math.max(1, Math.floor(target.length / 4)) };
  });

  const hits = results.filter((r) => r.ok).length;
  const total = blanks.length;
  const correct = hits === total && total > 0;
  const score = total ? Math.round((hits / total) * 100) : 0;

  let feedback, hint = "";
  if (correct) {
    feedback = "Alle Lücken korrekt ausgefüllt!";
    const caseOff = results.find((r) => r.caseOff);
    if (caseOff) hint = `Kleinigkeit: Üblich ist die Schreibweise \`${caseOff.expected}\`.`;
  } else {
    const typo = results.find((r) => !r.ok && r.typo);
    const empty = results.filter((r) => !r.ok && r.reason === "leer");
    feedback = `${hits} von ${total} Lücken stimmen.`;
    if (typo) hint = `Fast: Du hast \`${typo.given}\` geschrieben, richtig wäre \`${typo.expected}\`.`;
    else if (empty.length === total) hint = "Fülle zuerst alle Lücken aus.";
    else hint = `Richtig wäre: ${results.filter((r) => !r.ok).map((r) => r.expected).join(", ")}.`;
  }

  return { correct, score, offline: true, feedback, hint, praise: correct ? "Sauber gelöst!" : "" };
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

function evaluateExplanation(task, answer) {
  const text = String(answer || "").trim();
  const words = text.split(/\s+/).filter(Boolean);
  const sentences = text.split(/[.!?]+/).map((s) => s.trim()).filter((s) => s.length > 3);

  if (words.length < 3) {
    return { correct: false, score: 0, offline: true,
      feedback: "Da steht noch fast nichts.",
      hint: "Schreib mindestens ein bis zwei vollständige Sätze in eigenen Worten.", praise: "" };
  }

  // Erwartete Begriffe: aus expectedConcepts und den Code-Spans der Frage
  const fromQuestion = (task.question || "").match(/`([^`]+)`/g) || [];
  const expected = [...new Set([
    ...(task.expectedConcepts || []),
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

  const hasReasoning = REASONING_WORDS.test(text);
  const hasExample = EXAMPLE_WORDS.test(text);
  // Fragt die Aufgabe ausdrücklich nach dem Warum, reicht eine reine
  // Beschreibung nicht aus — dann ist die Begründung der Kern der Antwort.
  const wantsReason = /\b(warum|wieso|weshalb|begründe|aus welchem grund)\b/i.test(task.question || "");

  // Wurde die Frage nur abgeschrieben? Dann zeigt das kein Verständnis.
  const questionWords = new Set((task.question || "").toLowerCase().split(/\s+/).map(stemDe).filter((w) => w.length > 3));
  const ownWords = [...answerStems].filter((w) => w.length > 3 && !questionWords.has(w));
  const copiedFromQuestion = questionWords.size > 3 && ownWords.length < 3;

  let score = 0;
  score += Math.min(30, words.length * 2.5);                    // Ausführlichkeit
  score += sentences.length >= 2 ? 15 : sentences.length * 8;   // Satzbau
  score += hasReasoning ? 25 : 0;                                // Begründung
  score += hasExample ? 5 : 0;                                   // Beispiel
  score += coverage === null ? 20 : Math.round(coverage * 25);   // Fachbegriffe
  if (copiedFromQuestion) score = Math.min(score, 35);
  if (wantsReason && !hasReasoning) score = Math.min(score, 50);
  score = Math.max(0, Math.min(100, Math.round(score)));

  const correct = score >= 55 && words.length >= 6 && !copiedFromQuestion
    && !(wantsReason && !hasReasoning);

  let feedback, hint = "";
  if (copiedFromQuestion) {
    feedback = "Das ist im Wesentlichen die Frage in anderer Reihenfolge.";
    hint = "Erkläre es mit eigenen Worten — was passiert da, und warum?";
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
    feedback = "Die Grundidee hast du verstanden.";
    hint = hasReasoning ? "Etwas ausführlicher wäre noch besser." : "Ergänze das „Warum“ — zum Beispiel mit „weil …“.";
  } else if (words.length < 6) {
    feedback = "Die Erklärung ist noch zu knapp, um dein Verständnis zu zeigen.";
    hint = "Ein bis zwei vollständige Sätze reichen schon.";
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
    details: { covered, missing, hasReasoning, words: words.length },
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

/* VS-Code-Editor (Monaco) für Code-Aufgaben — mit Fallback auf CodeEditor */
const MONACO_LANG = { javascript: "javascript", html: "html", css: "css", java: "java", python: "python", sql: "sql", cpp: "cpp" };
const MONACO_VS = "https://cdn.jsdelivr.net/npm/monaco-editor@0.52.2/min/vs";
let monacoConfigured = false;

// Plain-Function-Wrapper (KEINE verschachtelte Komponente -> Monaco bleibt erhalten)
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

function MonacoCodeEditor({ value, onChange, disabled, courseId, label }) {
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

  if (!mod) return editorChrome(courseId, label, <><Loader2 size={11} className="ld-spin" />Editor lädt …</>,
    <div className="p-3 space-y-2">
      <div className="ld-skeleton h-3 w-2/3" /><div className="ld-skeleton h-3 w-1/2" /><div className="ld-skeleton h-3 w-3/4" /><div className="ld-skeleton h-3 w-1/3" />
    </div>
  );

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
  return editorChrome(courseId, label, <>VS&nbsp;Code · Monaco</>,
    <Editor
      height="280px"
      language={MONACO_LANG[courseId] || "plaintext"}
      theme="ld-dark"
      value={value}
      beforeMount={beforeMount}
      onChange={(v) => onChange(v == null ? "" : v)}
      loading={<div className="p-4 text-sm text-[#8A9BC0] flex items-center gap-2"><Loader2 size={14} className="ld-spin" />Editor wird vorbereitet …</div>}
      options={{
        readOnly: disabled, fontSize: 13, fontFamily: "'JetBrains Mono', monospace", fontLigatures: true,
        minimap: { enabled: false }, scrollBeyondLastLine: false, automaticLayout: true,
        padding: { top: 10, bottom: 10 }, tabSize: 2, lineNumbersMinChars: 3, renderLineHighlight: "line",
        smoothScrolling: true, cursorBlinking: "smooth", roundedSelection: true, scrollbar: { verticalScrollbarSize: 8, horizontalScrollbarSize: 8 },
      }}
    />
  );
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

/* Skeleton-Loader für KI-Bewertung */
function SkeletonFeedback() {
  return (
    <Card className="p-5 mt-5">
      <div className="flex items-center gap-2 mb-3 pb-3 border-b border-[#1E2D4A]">
        <Bot size={18} className="text-[#7C3AED]" />
        <span className="font-display font-bold">KI-Bewertung</span>
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
    const persistUsers = users.filter((u) => !u.isGuest);
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

function mergeWithDemo(persistedUsers) {
  const map = new Map(DEMO_USERS.map((u) => [u.id, u]));
  (persistedUsers || []).forEach((u) => map.set(u.id, u));
  return Array.from(map.values());
}

function roleHome(role) { return role === "teacher" ? "teacher" : role === "admin" ? "admin" : "dashboard"; }

export default function App() {
  const [view, setView] = useState(() => {
    const persisted = loadPersisted();
    if (!persisted?.currentUser) return "landing";
    const u = mergeWithDemo(persisted.users).find((x) => x.id === persisted.currentUser);
    return u ? roleHome(u.role) : "landing";
  });
  const [users, setUsers] = useState(() => mergeWithDemo(loadPersisted()?.users));
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
  const [emailVerifyOpen, setEmailVerifyOpen] = useState(false);
  const [pending2FA, setPending2FA] = useState(null);
  const [twoFactorSetupCode, setTwoFactorSetupCode] = useState(null);
  const [playgroundOpenId, setPlaygroundOpenId] = useState(null);
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
          setView(roleHome(mapped.role));
        } catch (e) {
          // Nicht angemeldet — Startseite bleibt stehen
          if (alive) { setUsers([]); setCurrentUser(null); setView("landing"); }
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
  const openAiSettings = useCallback(() => setAiSettingsOpen(true), []);
  const closeAiSettings = useCallback(() => setAiSettingsOpen(false), []);
  const openEmailVerify = useCallback(() => setEmailVerifyOpen(true), []);
  const closeEmailVerify = useCallback(() => setEmailVerifyOpen(false), []);

  const celebrate = useCallback(() => { setConfetti(true); setTimeout(() => setConfetti(false), 3200); }, []);

  const pushToast = useCallback((type, msg) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t, { id, type, msg }]);
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
  const savePlaygroundProject = useCallback(async (project) => {
    if (!me) return false;
    if (api.available && !me.isGuest) {
      try {
        const path = project.id && !String(project.id).startsWith("u_")
          ? `/api/projects/${project.id}` : "/api/projects";
        await api.put(path, { name: project.name, html: project.html, css: project.css, js: project.js });
        await loadProjects();
        pushToast("success", `Projekt „${project.name}“ gespeichert.`);
        return true;
      } catch (e) {
        pushToast("error", e.message);
        return false;
      }
    }
    const sizeBytes = new Blob([project.html || "", project.css || "", project.js || ""]).size;
    const others = (me.playground || []).filter((p) => p.id !== project.id);
    const usedByOthers = others.reduce((sum, p) => sum + (p.sizeBytes || 0), 0);
    if (usedByOthers + sizeBytes > STORAGE_QUOTA_BYTES) {
      pushToast("error", "Speicherkontingent (2,5 GB) erreicht — lösche ein Projekt, um Platz zu schaffen.");
      return false;
    }
    const saved = { ...project, sizeBytes, updatedAt: "Jetzt" };
    setUsers((us) => us.map((u) => u.id === me.id ? { ...u, playground: [...others, saved] } : u));
    pushToast("success", `Projekt „${project.name}“ gespeichert.`);
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
  const adminCreateAdmin = useCallback((form) => {
    if (!form.name || !form.email || !form.password) { pushToast("error", "Bitte alle Felder ausfüllen."); return false; }
    if (users.some((u) => !u.isGuest && u.email.toLowerCase() === form.email.trim().toLowerCase())) { pushToast("error", "E-Mail bereits vergeben."); return false; }
    const newAdmin = {
      id: uid(), role: "admin", name: form.name, email: form.email, password: form.password,
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
    const u = users.find((x) => x.email.toLowerCase() === email.trim().toLowerCase() && x.password === password);
    if (!u) { pushToast("error", "E-Mail oder Passwort falsch."); return false; }
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
    let newUser;
    if (form.role === "teacher") {
      const code = genSchoolCode();
      newUser = {
        id, role: "teacher", name: form.name, email: form.email, password: form.password, school: form.school || "—", schoolCode: code,
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
        id, role: "student", name: form.name, email: form.email, password: form.password, teacherId,
        xp: guest ? guest.xp : 0, streak: guest ? guest.streak : 1,
        completedLessons: guest ? guest.completedLessons : [], currentCourse: guest ? guest.currentCourse : null,
        joinedAt: "Heute", lastLogin: "Jetzt", avatar: guest ? guest.avatar : "🧑‍💻", badges: guest ? guest.badges : [],
        emailVerified: false, verificationCode, twoFactorEnabled: false, playground: [],
      };
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
  const addXP = useCallback((amount) => {
    // Optimistisch anzeigen, damit die Oberfläche sofort reagiert …
    setUsers((us) => us.map((u) => u.id === currentUser ? { ...u, xp: u.xp + amount } : u));
    // … und serverseitig verbuchen, wo der Wert manipulationssicher liegt.
    if (api.available && me && !me.isGuest) {
      api.post("/api/progress/xp", { amount })
        .then(({ user }) => setUsers((us) => us.map((u) => u.id === user.id ? fromApiUser(user) : u)))
        .catch(() => {});
    }
  }, [currentUser, me]);

  /** Ermittelt neu verdiente Abzeichen für den aktuellen Stand. */
  const earnedBadgesFor = (completed, xp) => {
    const out = ["first_lesson"];
    if (completed.filter((id) => id.startsWith("javascript_")).length >= 10) out.push("js_beginner");
    if (getLevelInfo(xp).level >= 10) out.push("mid_wizard");
    const meta = findLessonMeta(completed[completed.length - 1]);
    if (meta) {
      const allIds = allLessonsOf(meta.course).map((l) => l.id);
      if (allIds.every((id) => completed.includes(id))) out.push("course_complete");
    }
    return out;
  };

  const completeLesson = useCallback(async (lessonId, bonusXp) => {
    if (api.available && me && !me.isGuest) {
      const completed = [...(me.completedLessons || []), lessonId];
      try {
        const res = await api.post("/api/progress/complete", {
          lessonId,
          courseId: findLessonMeta(lessonId)?.course.id,
          xpReward: bonusXp,
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
      newlyEarned.forEach((b) => setTimeout(() => pushToast("badge", `Neues Abzeichen: ${BADGES[b].label}!`), 400));
      return { ...u, completedLessons: completed, xp: newXp, badges };
    }));
  }, [currentUser, pushToast, me]);

  const ctx = {
    view, navigate, users, me, setUsers, currentUser,
    selectedCourse, openCourse, selectedLesson, openLesson,
    selectedStudent, setSelectedStudent, login, register, logout, continueAsGuest,
    pushToast, showXP, addXP, completeLesson, celebrate, sidebarOpen, setSidebarOpen,
    apiKeys, setApiKeys, aiProvider, setAiProvider, ollamaModel, setOllamaModel, aiConfig, aiReady, aiSettingsOpen, openAiSettings, closeAiSettings,
    backend, booting, refreshMe, loadProjects, serverVerificationCode, confirm2FA,
    pending2FA, verify2FALogin, cancel2FALogin,
    emailVerifyOpen, openEmailVerify, closeEmailVerify, verifyEmail,
    enable2FA, disable2FA, twoFactorSetupCode, closeTwoFactorSetup,
    savePlaygroundProject, deletePlaygroundProject, playgroundOpenId, setPlaygroundOpenId,
    reports, reportContent, resolveReport, deleteReport,
    adminUpdateUser, adminDeleteUser, adminCreateAdmin,
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
  else if (LEGAL_VIEWS.includes(view)) screen = <LegalPage ctx={ctx} page={view} />;
  else if (view === "lesson") screen = <LessonView ctx={ctx} />;
  else screen = <AppShell ctx={ctx}>{
    view === "dashboard" ? <StudentDashboard ctx={ctx} /> :
    view === "teacher" ? <TeacherDashboard ctx={ctx} /> :
    view === "admin" ? <AdminDashboard ctx={ctx} /> :
    view === "playground" ? <Playground ctx={ctx} /> :
    view === "courses" ? <CoursesOverview ctx={ctx} /> :
    view === "course" ? <CourseView ctx={ctx} /> :
    view === "leaderboard" ? <Leaderboard ctx={ctx} /> :
    view === "profile" ? <Profile ctx={ctx} /> : null
  }</AppShell>;

  return (
    <div className="min-h-screen bg-[#0A0E1A] text-[#E8EDF5]">
      <GlobalStyles />
      {screen}
      <Toasts toasts={toasts} />
      {xpPopup != null && <XPPopup amount={xpPopup} />}
      {confetti && <Confetti />}
      {aiSettingsOpen && <AiSettingsModal ctx={ctx} />}
      {emailVerifyOpen && <EmailVerifyModal ctx={ctx} />}
      {twoFactorSetupCode && <TwoFactorSetupModal ctx={ctx} />}
    </div>
  );
}

/* ============================ Landing ============================== */
function Landing({ ctx }) {
  const { navigate } = ctx;
  return (
    <div>
      {/* Top nav */}
      <nav className="fixed top-0 inset-x-0 z-50 backdrop-blur-md bg-[#0A0E1A]/80 border-b border-[#1E2D4A]">
        <div className="max-w-6xl mx-auto px-5 h-16 flex items-center justify-between gap-4">
          <Logo onClick={() => navigate("landing")} />
          <div className="hidden lg:flex items-center gap-6 text-sm text-[#8A9BC0]">
            <a href="#kurse" className="hover:text-[#E8EDF5] transition-colors flex items-center gap-1.5"><BookOpen size={13} />Kurse</a>
            <a href="#ide" className="hover:text-[#E8EDF5] transition-colors flex items-center gap-1.5"><Code2 size={13} />IDE</a>
            <a href="#features" className="hover:text-[#E8EDF5] transition-colors flex items-center gap-1.5"><Sparkles size={13} />Features</a>
            <a href="#rollen" className="hover:text-[#E8EDF5] transition-colors flex items-center gap-1.5"><GraduationCap size={13} />Für Schulen</a>
            <a href="#preise" className="hover:text-[#E8EDF5] transition-colors flex items-center gap-1.5"><Star size={13} />Preise</a>
            <button onClick={() => navigate("ueber-uns")} className="hover:text-[#E8EDF5] transition-colors flex items-center gap-1.5"><Info size={13} />Über uns</button>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Btn variant="ghost" size="sm" onClick={() => navigate("login")}>Anmelden</Btn>
            <Btn size="sm" icon={Rocket} onClick={() => navigate("register")}>Jetzt starten</Btn>
          </div>
        </div>
        {/* Mobile Navigation */}
        <div className="lg:hidden flex items-center gap-4 px-5 pb-2.5 overflow-x-auto text-xs text-[#8A9BC0]">
          <a href="#kurse" className="whitespace-nowrap hover:text-[#E8EDF5]">Kurse</a>
          <a href="#ide" className="whitespace-nowrap hover:text-[#E8EDF5]">IDE</a>
          <a href="#features" className="whitespace-nowrap hover:text-[#E8EDF5]">Features</a>
          <a href="#rollen" className="whitespace-nowrap hover:text-[#E8EDF5]">Für Schulen</a>
          <a href="#preise" className="whitespace-nowrap hover:text-[#E8EDF5]">Preise</a>
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
              <div className="text-4xl mb-3">{c.icon}</div>
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
              { emoji: "📖", t: "Lerne die Theorie", d: "Klare Erklärungen mit echten Code-Beispielen — kein trockenes Geschwafel." },
              { emoji: "✍️", t: "Löse Aufgaben", d: "Multiple Choice, Code schreiben, Lückentext und freies Erklären." },
              { emoji: "🤖", t: "Bekomm Feedback", d: "Sofortige, konstruktive Rückmeldung zu jeder Antwort und jedem Code." },
            ].map((f, i) => (
              <Card key={i} className="p-7 text-center">
                <div className="text-4xl mb-4">{f.emoji}</div>
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
          <p className="text-[#8A9BC0] text-lg">Eine Plattform statt zehn Tools.</p>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[
            { icon: Code2, color: "#4F8EF7", t: "VS-Code-Editor im Browser", d: "Echter Monaco-Editor mit Syntax-Highlighting, Zeilennummern und Auto-Einrückung." },
            { icon: Eye, color: "#10B981", t: "Live-Vorschau", d: "Schreib HTML, CSS & JS und sieh das Ergebnis in Echtzeit — ohne Setup." },
            { icon: Bug, color: "#7C3AED", t: "KI-Code-Debugging", d: "Lass deinen Code analysieren: Fehler finden, verstehen und beheben." },
            { icon: Trophy, color: "#F7C948", t: "XP, Level & Abzeichen", d: "20 Level, Streaks und Abzeichen halten dich am Ball." },
            { icon: Users, color: "#4F8EF7", t: "Für Schulen & Klassen", d: "Lehrer verwalten ihre Klasse per Schul-Code und sehen jeden Fortschritt." },
            { icon: ShieldCheck, color: "#10B981", t: "Sicherer Account", d: "E-Mail-Bestätigung und optionale Zwei-Faktor-Authentifizierung." },
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
            <h2 className="font-display text-3xl font-extrabold mb-4">Nicht nur lernen — <span className="ld-gradient-text">bauen</span>.</h2>
            <p className="text-[#8A9BC0] leading-relaxed mb-5">
              Im integrierten Code-Editor schreibst du frei HTML, CSS und JavaScript. Deine Seite wird live gerendert, während du tippst. Speichere deine Projekte und lass sie von der KI debuggen.
            </p>
            <ul className="space-y-2 mb-6 text-[#8A9BC0] text-sm">
              {["Echtzeit-Vorschau ohne Speichern", "Projekte sichern und weiterarbeiten", "KI findet Fehler in deinem Code"].map((x, i) => (
                <li key={i} className="flex items-start gap-2"><CheckCircle2 size={16} className="text-[#10B981] mt-0.5 shrink-0" />{x}</li>
              ))}
            </ul>
            <Btn icon={ArrowRight} onClick={() => navigate("register")}>Editor ausprobieren</Btn>
          </div>
          <Card className="p-0 overflow-hidden">
            <div className="flex items-center gap-1.5 px-3 py-2 bg-[#0A0E1A] border-b border-[#1E2D4A]">
              <span className="w-2.5 h-2.5 rounded-full bg-[#EF4444]/60" /><span className="w-2.5 h-2.5 rounded-full bg-[#F59E0B]/60" /><span className="w-2.5 h-2.5 rounded-full bg-[#10B981]/60" />
              <span className="ml-2 font-code text-[11px] text-[#4A5A7A]">solution.html</span>
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
              <Eye size={13} />Live-Vorschau aktualisiert sich automatisch
            </div>
          </Card>
        </div>
      </section>

      {/* Rollen */}
      <section id="rollen" className="max-w-5xl mx-auto px-5 py-20">
        <div className="grid md:grid-cols-2 gap-6">
          <Card className="p-8">
            <div className="text-4xl mb-4">🎓</div>
            <h3 className="font-display text-2xl font-bold mb-3">Für Schüler</h3>
            <ul className="space-y-2 mb-6 text-[#8A9BC0]">
              {["Lerne 7 Sprachen in deinem Tempo", "Sammle XP, Level & Abzeichen", "KI-Feedback zu jedem Code", "Halte deinen Streak am Leben 🔥"].map((x, i) => (
                <li key={i} className="flex items-start gap-2"><CheckCircle2 size={18} className="text-[#10B981] mt-0.5 shrink-0" />{x}</li>
              ))}
            </ul>
            <Btn icon={GraduationCap} onClick={() => navigate("register")}>Als Schüler starten</Btn>
          </Card>
          <Card className="p-8">
            <div className="text-4xl mb-4">👨‍🏫</div>
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

      {/* Preise */}
      <section id="preise" className="bg-[#0F1629] border-y border-[#1E2D4A] py-20">
        <div className="max-w-4xl mx-auto px-5">
          <div className="text-center mb-12">
            <h2 className="font-display text-4xl font-extrabold mb-3">Einfach <span className="ld-gradient-text">kostenlos</span></h2>
            <p className="text-[#8A9BC0] text-lg">Alle Lerninhalte sind frei zugänglich. Keine Paywall, keine Kreditkarte.</p>
          </div>
          <div className="grid md:grid-cols-2 gap-6">
            <Card className="p-8 relative overflow-hidden">
              <h3 className="font-display text-xl font-bold mb-1">Kostenlos</h3>
              <p className="font-display text-4xl font-black mb-1">0 €</p>
              <p className="text-sm text-[#8A9BC0] mb-6">für immer</p>
              <ul className="space-y-2 mb-6 text-sm text-[#8A9BC0]">
                {[`Alle ${COURSES.length} Sprachen & ${TOTAL_LESSONS}+ Lektionen`, "IDE mit Live-Vorschau", "Lokale Code-Analyse", "XP, Level & Abzeichen", "Ohne Anmeldung testbar"].map((x, i) => (
                  <li key={i} className="flex items-start gap-2"><CheckCircle2 size={16} className="text-[#10B981] mt-0.5 shrink-0" />{x}</li>
                ))}
              </ul>
              <Btn className="w-full" icon={Rocket} onClick={() => navigate("register")}>Jetzt starten</Btn>
            </Card>
            <Card className="p-8 relative overflow-hidden border-[#4F8EF7]/40">
              <span className="absolute top-4 right-4 text-[10px] px-2 py-0.5 rounded-full bg-[#4F8EF7]/15 text-[#4F8EF7]">Optional</span>
              <h3 className="font-display text-xl font-bold mb-1">Mit eigener KI</h3>
              <p className="font-display text-4xl font-black mb-1">0 €<span className="text-base font-normal text-[#8A9BC0]">*</span></p>
              <p className="text-sm text-[#8A9BC0] mb-6">*eigener Anbieter-Zugang</p>
              <ul className="space-y-2 mb-6 text-sm text-[#8A9BC0]">
                {["Alles aus Kostenlos", "KI-Bewertung deiner Antworten", "KI-Code-Debugging", "Gemini: kostenloses Kontingent", "Oder eigener Server via Ollama"].map((x, i) => (
                  <li key={i} className="flex items-start gap-2"><CheckCircle2 size={16} className="text-[#4F8EF7] mt-0.5 shrink-0" />{x}</li>
                ))}
              </ul>
              <Btn variant="secondary" className="w-full" icon={Bot} onClick={() => navigate("register")}>Mehr erfahren</Btn>
            </Card>
          </div>
          <p className="text-center text-xs text-[#4A5A7A] mt-6">
            Die Plattform selbst kostet nichts. Für KI-Bewertung nutzt du deinen eigenen Zugang — bei Google Gemini gibt es dafür ein kostenloses Kontingent.
          </p>
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
    if (isLogin) login(form.email, form.password, captcha);
    else register({ ...form, turnstileToken: captcha });
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
            <Field label="Passwort" icon={KeyRound} type="password" value={form.password} onChange={set("password")} placeholder="••••••••"
              onKeyDown={(e) => { if (e.key === "Enter" && isLogin) submit(); }} />

            {!isLogin && (
              <>
                <Field label="Passwort bestätigen" icon={KeyRound} type="password" value={form.confirm} onChange={set("confirm")} placeholder="••••••••" />
                <div>
                  <label className="block text-sm text-[#8A9BC0] mb-2">Ich bin…</label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { v: "student", emoji: "🎓", t: "Schüler", d: "Lerne mit KI-Feedback" },
                      { v: "teacher", emoji: "👨‍🏫", t: "Lehrer", d: "Verwalte Schüler" },
                    ].map((r) => (
                      <button key={r.v} onClick={() => setForm((f) => ({ ...f, role: r.v }))}
                        className={`text-left p-3 rounded-lg border transition-all ${form.role === r.v ? "border-[#4F8EF7] bg-[#4F8EF7]/10" : "border-[#1E2D4A] hover:border-[#2A3F6F]"}`}>
                        <div className="text-2xl mb-1">{r.emoji}</div>
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
  const { me, view, navigate, logout, sidebarOpen, setSidebarOpen, aiReady, openAiSettings, openEmailVerify, reports } = ctx;
  if (!me) return null;
  const lvl = me.role === "student" ? getLevelInfo(me.xp) : null;
  const isAdmin = me.role === "admin";
  const openReports = (reports || []).filter((r) => r.status === "open").length;

  const studentNav = [
    { v: "dashboard", label: "Übersicht", icon: Home },
    { v: "courses", label: "Meine Kurse", icon: BookOpen },
    { v: "playground", label: "IDE", icon: Code2 },
    { v: "leaderboard", label: "Rangliste", icon: Trophy },
    { v: "profile", label: "Profil", icon: User },
  ];
  const teacherNav = [
    { v: "teacher", label: "Übersicht", icon: LayoutDashboard },
    { v: "playground", label: "IDE", icon: Code2 },
    { v: "leaderboard", label: "Rangliste", icon: Trophy },
    { v: "profile", label: "Profil", icon: User },
  ];
  const adminNav = [
    { v: "admin", label: "Admin-Bereich", icon: Shield },
    { v: "profile", label: "Profil", icon: User },
  ];
  const nav = isAdmin ? adminNav : me.role === "teacher" ? teacherNav : studentNav;
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
                  <span className="hidden sm:flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full bg-[#141D35] border border-[#1E2D4A]"><Star size={14} className="text-[#F7C948]" /><span className="font-semibold text-[#F7C948]">{me.xp.toLocaleString("de-DE")}</span></span>
                  <span className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full bg-[#141D35] border border-[#1E2D4A]"><Flame size={14} className="text-[#F59E0B]" /><span className="font-semibold">{me.streak}</span></span>
                </>
              )}
              {isAdmin && openReports > 0 && (
                <button onClick={() => navigate("admin")} className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full bg-[#EF4444]/10 border border-[#EF4444]/30 text-[#EF4444]">
                  <FileText size={14} />{openReports} offene Meldung{openReports === 1 ? "" : "en"}
                </button>
              )}
              {!isAdmin && (
                <button onClick={openAiSettings} aria-label="KI-Einstellungen" title={aiReady ? "KI verbunden" : "KI einrichten"}
                  className="relative w-10 h-10 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#2A3F6F] flex items-center justify-center transition-colors">
                  <Settings size={16} className="text-[#8A9BC0]" />
                  <span className={`absolute top-1 right-1 w-2 h-2 rounded-full ${aiReady ? "bg-[#10B981]" : "bg-[#4A5A7A]"}`} />
                </button>
              )}
              <button onClick={() => navigate("profile")} aria-label="Profil" className="w-10 h-10 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#2A3F6F] flex items-center justify-center transition-colors overflow-hidden"><UserAvatar user={me} size={38} /></button>
            </div>
          </div>
        </header>
        <main className="px-4 lg:px-8 py-6 max-w-6xl mx-auto">
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
        <div className="text-4xl">{course.icon}</div>
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
            <p className="text-sm font-medium mb-3 flex items-center gap-1.5"><Flame size={15} className="text-[#F59E0B]" />Deine Lern-Woche</p>
            <div className="flex gap-2">
              {["Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"].map((d, i) => {
                const lit = i >= 7 - Math.min(me.streak, 7);
                return (
                  <div key={d} className="flex flex-col items-center gap-1.5">
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-sm ${lit ? "" : "bg-[#1A2540] text-[#4A5A7A]"}`} style={lit ? { background: GRADIENT } : undefined}>{lit ? "🔥" : ""}</div>
                    <span className="text-[10px] text-[#8A9BC0]">{d}</span>
                  </div>
                );
              })}
            </div>
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
            <div className="text-5xl">{cur.icon}</div>
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
                  <div className="text-2xl">{m.course.icon}</div>
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
  const { selectedCourse, me, navigate, openLesson } = ctx;
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
          <div className="text-6xl">{course.icon}</div>
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
                  {!unlocked && <p className="text-xs text-[#4A5A7A] mt-0.5">🔒 Schließe Modul {idx} ab, um es freizuschalten</p>}
                </div>
                <div className="hidden sm:flex items-center gap-3 w-40">
                  <ProgressBar value={mp} max={mod.lessons.length} />
                  <span className="text-xs text-[#8A9BC0] whitespace-nowrap">{mp}/{mod.lessons.length}</span>
                </div>
              </button>
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
  const { users, me } = ctx;
  const students = users.filter((u) => u.role === "student").sort((a, b) => b.xp - a.xp);
  const medal = ["🥇", "🥈", "🥉"];
  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-bold flex items-center gap-2"><Trophy className="text-[#F7C948]" />Rangliste</h1>
        <p className="text-[#8A9BC0] mt-1">Die fleißigsten Lernenden der Plattform.</p>
      </div>
      <Card className="divide-y divide-[#1E2D4A]">
        {students.map((s, i) => {
          const lvl = getLevelInfo(s.xp);
          const isMe = s.id === me.id;
          return (
            <div key={s.id} className={`flex items-center gap-4 p-4 ${isMe ? "bg-[#4F8EF7]/10" : ""}`}>
              <div className="w-8 text-center font-display font-bold text-lg">{i < 3 ? medal[i] : <span className="text-[#4A5A7A]">{i + 1}</span>}</div>
              <div className="w-9 h-9 rounded-lg overflow-hidden flex items-center justify-center shrink-0"><UserAvatar user={s} size={36} /></div>
              <div className="flex-1 min-w-0">
                <p className="font-medium truncate">{s.name}{isMe && <span className="text-xs text-[#4F8EF7] ml-2">(Du)</span>}</p>
                <p className="text-xs text-[#8A9BC0]">Level {lvl.level} · {lvl.name}</p>
              </div>
              <div className="hidden sm:flex items-center gap-1 text-sm text-[#F59E0B]"><Flame size={14} />{s.streak}</div>
              <div className="flex items-center gap-1.5 font-semibold text-[#F7C948] w-24 justify-end"><Star size={15} />{s.xp.toLocaleString("de-DE")}</div>
            </div>
          );
        })}
      </Card>
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
      {/* Augen */}
      {c.eyes === "normal" && <g fill="#1B2436"><circle cx="40" cy="50" r="4" /><circle cx="60" cy="50" r="4" /></g>}
      {c.eyes === "gluecklich" && <g stroke="#1B2436" strokeWidth="3" fill="none" strokeLinecap="round"><path d="M35 52 Q40 46 45 52" /><path d="M55 52 Q60 46 65 52" /></g>}
      {c.eyes === "cool" && <g fill="#1B2436"><rect x="35" y="47" width="11" height="5" rx="2" /><rect x="54" y="47" width="11" height="5" rx="2" /></g>}
      {c.eyes === "sternchen" && <g fill="#F7C948"><path d="M40 45 l1.6 4.4 4.4 1.6 -4.4 1.6 -1.6 4.4 -1.6 -4.4 -4.4 -1.6 4.4 -1.6 Z" /><path d="M60 45 l1.6 4.4 4.4 1.6 -4.4 1.6 -1.6 4.4 -1.6 -4.4 -4.4 -1.6 4.4 -1.6 Z" /></g>}
      {c.eyes === "zwinkern" && <g fill="#1B2436"><circle cx="40" cy="50" r="4" /><path d="M55 51 Q60 46 65 51" stroke="#1B2436" strokeWidth="3" fill="none" strokeLinecap="round" /></g>}
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
    </svg>
  );
}

// Zeigt entweder den gebauten Charakter oder (Alt-Accounts) das Emoji.
function UserAvatar({ user, size = 40 }) {
  if (user?.avatarConfig) return <CharacterAvatar config={user.avatarConfig} size={size} />;
  return <span style={{ fontSize: size * 0.62, lineHeight: 1 }}>{user?.avatar || "🧑‍💻"}</span>;
}

function AvatarCreator({ value, onChange }) {
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
  const randomize = () => onChange({
    skin: AV_SKIN[Math.floor(Math.random() * AV_SKIN.length)],
    hairColor: AV_HAIR_COLOR[Math.floor(Math.random() * AV_HAIR_COLOR.length)],
    hairStyle: AV_HAIR_STYLES[Math.floor(Math.random() * AV_HAIR_STYLES.length)],
    eyes: AV_EYES[Math.floor(Math.random() * AV_EYES.length)],
    accessory: AV_ACCESSORY[Math.floor(Math.random() * AV_ACCESSORY.length)],
    bg: AV_BG[Math.floor(Math.random() * AV_BG.length)],
  });

  return (
    <div className="flex flex-col sm:flex-row gap-6">
      <div className="flex flex-col items-center gap-3 shrink-0">
        <CharacterAvatar config={cfg} size={120} />
        <Btn size="sm" variant="secondary" icon={Wand} onClick={randomize}>Zufällig</Btn>
      </div>
      <div className="flex-1 space-y-3">
        <Swatches label="Hautton" colors={AV_SKIN} field="skin" />
        <Options label="Frisur" options={AV_HAIR_STYLES} field="hairStyle"
          labels={{ kurz: "Kurz", lang: "Lang", locken: "Locken", dutt: "Dutt", glatze: "Glatze", irokese: "Irokese" }} />
        <Swatches label="Haarfarbe" colors={AV_HAIR_COLOR} field="hairColor" />
        <Options label="Augen" options={AV_EYES} field="eyes"
          labels={{ normal: "Normal", gluecklich: "Fröhlich", cool: "Cool", sternchen: "Sterne", zwinkern: "Zwinkern" }} />
        <Options label="Accessoire" options={AV_ACCESSORY} field="accessory"
          labels={{ keine: "Keins", brille: "Brille", sonnenbrille: "Sonnenbrille", kopfhoerer: "Kopfhörer", muetze: "Mütze" }} />
        <Swatches label="Hintergrund" colors={AV_BG} field="bg" />
      </div>
    </div>
  );
}
function Profile({ ctx }) {
  const { me, pushToast, setUsers, enable2FA, disable2FA, openEmailVerify } = ctx;
  const isStudent = me.role === "student";
  const isTeacher = me.role === "teacher";
  const lvl = isStudent ? getLevelInfo(me.xp) : null;
  const [picker, setPicker] = useState(false);
  const [disable2FAOpen, setDisable2FAOpen] = useState(false);
  const [disablePw, setDisablePw] = useState("");
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
            <AvatarCreator value={me.avatarConfig} onChange={setAvatarConfig} />
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
                    <div className="text-3xl mb-1">{b.emoji}</div>
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
  const { me, users, pushToast } = ctx;
  const [invite, setInvite] = useState(false);
  const [detail, setDetail] = useState(null);
  const students = users.filter((u) => u.role === "student" && (me.students.includes(u.id) || u.teacherId === me.id));
  const avgProgress = students.length
    ? Math.round(students.reduce((acc, s) => {
        const c = s.currentCourse ? courseById(s.currentCourse) : COURSES[0];
        return acc + (c ? courseProgress(c, s).pct : 0);
      }, 0) / students.length)
    : 0;
  const activeToday = students.filter((s) => s.lastLogin === "Heute" || s.lastLogin === "Jetzt").length;
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
                      <td className="px-4 py-3">{c ? <span className="flex items-center gap-1.5">{c.icon} {c.name}</span> : <span className="text-[#4A5A7A]">—</span>}</td>
                      <td className="px-4 py-3"><div className="flex items-center gap-2 w-32"><ProgressBar value={p.done} max={p.total || 1} /><span className="text-xs text-[#8A9BC0] whitespace-nowrap">{p.pct}%</span></div></td>
                      <td className="px-4 py-3"><span className="flex items-center gap-1 text-[#F7C948]"><Star size={13} />{s.xp.toLocaleString("de-DE")}</span></td>
                      <td className="px-4 py-3 text-[#8A9BC0]">{s.lastLogin}</td>
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
                    <span className="text-lg w-6">{c.icon}</span>
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

const PLAYGROUND_STARTER = {
  html: `<h1>Hallo Welt!</h1>\n<p>Schreib hier deinen eigenen Code — die Vorschau aktualisiert sich in Echtzeit.</p>\n<button id="btn">Klick mich</button>`,
  css: `body {\n  font-family: sans-serif;\n  background: #0A0E1A;\n  color: #E8EDF5;\n  padding: 2rem;\n}\nbutton {\n  background: linear-gradient(135deg, #4F8EF7, #7C3AED);\n  border: none;\n  color: white;\n  padding: 10px 18px;\n  border-radius: 8px;\n  cursor: pointer;\n}`,
  js: `document.getElementById('btn').addEventListener('click', () => {\n  alert('Live-Vorschau funktioniert! 🎉');\n});`,
};

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
        <p className="text-sm text-[#8A9BC0]">Noch kein KI-Zugang eingerichtet.</p>
        <p className="text-xs text-[#4A5A7A] max-w-xs leading-relaxed">
          Der Assistent kann deinen Code erklären, Fehler finden und Verbesserungen vorschlagen.
          Nutze dafür Google Gemini (kostenloses Kontingent) oder einen eigenen Ollama-Server.
        </p>
        <Btn size="sm" icon={Settings} onClick={openAiSettings}>KI einrichten</Btn>
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

function Playground({ ctx }) {
  const { me, savePlaygroundProject, deletePlaygroundProject, playgroundOpenId, setPlaygroundOpenId, pushToast, aiConfig, openAiSettings } = ctx;
  const [tab, setTab] = useState("html");
  const [name, setName] = useState("Mein Projekt");
  const [html, setHtml] = useState(PLAYGROUND_STARTER.html);
  const [css, setCss] = useState(PLAYGROUND_STARTER.css);
  const [js, setJs] = useState(PLAYGROUND_STARTER.js);
  const [srcDoc, setSrcDoc] = useState("");
  const [projectId, setProjectId] = useState(null);
  const [debugResult, setDebugResult] = useState(null);
  const [debugLoading, setDebugLoading] = useState(false);
  const [logs, setLogs] = useState([]);
  const [rightTab, setRightTab] = useState("preview");

  const runDebug = async () => {
    setDebugLoading(true);
    setDebugResult(null);
    const res = analyzeProject({ html, css, js });
    setDebugResult(res);
    setDebugLoading(false);
  };

  // Echtzeit-Vorschau: kurz debounced, damit nicht bei jedem Tastendruck neu gerendert wird.
  // In die Seite wird eine kleine Brücke injiziert, die console-Ausgaben und
  // Laufzeitfehler an die IDE zurückmeldet.
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
      setSrcDoc(`<!DOCTYPE html><html><head><style>${css}</style></head><body>${html}<script>${bridge}<\/script><script>${js}<\/script></body></html>`);
    }, 350);
    return () => clearTimeout(t);
  }, [html, css, js]);

  // Konsolen-Ausgaben aus der Vorschau einsammeln
  useEffect(() => {
    const onMessage = (e) => {
      if (!e.data || !e.data.__ldConsole) return;
      setLogs((l) => [...l.slice(-99), { level: e.data.level, text: e.data.text }]);
    };
    window.addEventListener("message", onMessage);
    return () => window.removeEventListener("message", onMessage);
  }, []);

  useEffect(() => {
    if (!playgroundOpenId || !me) return;
    const p = (me.playground || []).find((x) => x.id === playgroundOpenId);
    if (p) { setProjectId(p.id); setName(p.name); setHtml(p.html); setCss(p.css); setJs(p.js); }
    setPlaygroundOpenId(null);
  }, [playgroundOpenId]);

  // Projekte einmalig vom Server holen, sobald die IDE geöffnet wird
  useEffect(() => { ctx.loadProjects?.(); }, []);

  const projects = me?.playground || [];
  // Mit Server sind Verbrauch und Kontingent verbindlich, ohne Server errechnet.
  const quotaBytes = me?.storageQuota || STORAGE_QUOTA_BYTES;
  const usedBytes = me?.storageUsed != null && ctx.backend
    ? me.storageUsed
    : projects.reduce((sum, p) => sum + (p.sizeBytes || 0), 0);
  const quotaPct = Math.min(100, (usedBytes / quotaBytes) * 100);

  const save = () => {
    const id = projectId || uid();
    const ok = savePlaygroundProject({ id, name: name || "Unbenannt", html, css, js });
    if (ok) setProjectId(id);
  };
  const newProject = () => { setProjectId(null); setName("Mein Projekt"); setHtml(PLAYGROUND_STARTER.html); setCss(PLAYGROUND_STARTER.css); setJs(PLAYGROUND_STARTER.js); };
  const load = (p) => { setProjectId(p.id); setName(p.name); setHtml(p.html); setCss(p.css); setJs(p.js); pushToast("info", `Projekt „${p.name}“ geladen.`); };

  const tabs = [["html", "HTML", "html"], ["css", "CSS", "css"], ["js", "JavaScript", "javascript"]];
  const codeFor = { html, css, js };
  const setterFor = { html: setHtml, css: setCss, js: setJs };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-bold flex items-center gap-2"><Code2 className="text-[#4F8EF7]" />IDE</h1>
          <p className="text-[#8A9BC0] mt-1">Freestyle coden — HTML, CSS &amp; JS mit Live-Vorschau in Echtzeit.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Btn variant="secondary" size="sm" icon={Plus} onClick={newProject}>Neu</Btn>
          <Btn variant="secondary" size="sm" icon={debugLoading ? undefined : Bug} onClick={runDebug} disabled={debugLoading}>
            {debugLoading ? <><Loader2 size={14} className="ld-spin" />Analysiert …</> : "Mit KI debuggen"}
          </Btn>
          <Btn size="sm" icon={Check} onClick={save}>Speichern</Btn>
        </div>
      </div>

      <Card className="p-4">
        <div className="flex items-center justify-between text-xs mb-1.5">
          <span className="text-[#8A9BC0]">Speicher: {formatBytes(usedBytes)} von 2,5 GB verwendet {me?.isGuest && "(Gast — wird nicht gespeichert)"}</span>
          <span className="text-[#4A5A7A]">{projects.length} Projekt{projects.length === 1 ? "" : "e"}</span>
        </div>
        <ProgressBar value={quotaPct} max={100} />
      </Card>

      <div className="grid lg:grid-cols-2 gap-4">
        <Card className="p-4">
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Projektname"
            className="w-full bg-[#0A0E1A] border border-[#1E2D4A] focus:border-[#4F8EF7] rounded-lg p-2.5 mb-3 text-sm font-medium text-[#E8EDF5]" />
          <div className="flex p-1 bg-[#0A0E1A] rounded-lg mb-3">
            {tabs.map(([v, label]) => (
              <button key={v} onClick={() => setTab(v)}
                className={`flex-1 py-1.5 rounded-md text-xs font-medium transition-all ${tab === v ? "text-white" : "text-[#8A9BC0]"}`}
                style={tab === v ? { background: GRADIENT } : undefined}>{label}</button>
            ))}
          </div>
          <MonacoCodeEditor value={codeFor[tab]} onChange={setterFor[tab]} disabled={false} courseId={tabs.find((t) => t[0] === tab)[2]} label={tab.toUpperCase()} />
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="flex p-1 bg-[#0A0E1A] rounded-lg">
              <button onClick={() => setRightTab("preview")}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all flex items-center gap-1.5 ${rightTab === "preview" ? "text-white" : "text-[#8A9BC0]"}`}
                style={rightTab === "preview" ? { background: GRADIENT } : undefined}><Eye size={12} />Vorschau</button>
              <button onClick={() => setRightTab("console")}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all flex items-center gap-1.5 ${rightTab === "console" ? "text-white" : "text-[#8A9BC0]"}`}
                style={rightTab === "console" ? { background: GRADIENT } : undefined}>
                <Terminal size={12} />Konsole
                {logs.length > 0 && <span className={`text-[9px] px-1.5 rounded-full ${logs.some((l) => l.level === "error") ? "bg-[#EF4444]/25 text-[#EF4444]" : "bg-white/20"}`}>{logs.length}</span>}
              </button>
              <button onClick={() => setRightTab("assistant")}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all flex items-center gap-1.5 ${rightTab === "assistant" ? "text-white" : "text-[#8A9BC0]"}`}
                style={rightTab === "assistant" ? { background: GRADIENT } : undefined}>
                <Bot size={12} />KI
              </button>
            </div>
            {rightTab === "console" && logs.length > 0 && (
              <button onClick={() => setLogs([])} className="text-xs text-[#8A9BC0] hover:text-[#E8EDF5] flex items-center gap-1"><Trash2 size={11} />Leeren</button>
            )}
          </div>
          <div className={`rounded-lg overflow-hidden border border-[#1E2D4A] ${rightTab === "preview" ? "bg-white" : "bg-[#0A0E1A]"}`} style={{ height: 340 }}>
            {rightTab === "preview" ? (
              <iframe title="Live-Vorschau" srcDoc={srcDoc} sandbox="allow-scripts allow-modals" className="w-full h-full border-0" />
            ) : rightTab === "assistant" ? (
              <AssistantPanel ctx={ctx} code={{ html, css, js }} />
            ) : (
              <div className="h-full overflow-y-auto p-3 font-code text-[12px] leading-relaxed">
                {logs.length === 0 ? (
                  <p className="text-[#4A5A7A]">Noch keine Ausgaben. Nutze <span className="text-[#4F8EF7]">console.log(...)</span> in deinem JavaScript.</p>
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
        </Card>
      </div>

      {/* KI-Debug-Ergebnis */}
      {debugLoading && (
        <Card className="p-5">
          <div className="flex items-center gap-2 mb-3 pb-3 border-b border-[#1E2D4A]">
            <Bug size={18} className="text-[#7C3AED]" /><span className="font-display font-bold">KI-Debugging</span>
            <span className="ml-auto flex items-center gap-1.5 text-xs text-[#8A9BC0]"><Loader2 size={13} className="ld-spin" />analysiert deinen Code …</span>
          </div>
          <div className="space-y-2.5"><div className="ld-skeleton h-4 w-1/3" /><div className="ld-skeleton h-3 w-full" /><div className="ld-skeleton h-3 w-4/5" /></div>
        </Card>
      )}
      {debugResult && !debugLoading && (
        <Card className="p-5">
          <div className="flex items-center gap-2 mb-3 pb-3 border-b border-[#1E2D4A]">
            <Bug size={18} className="text-[#7C3AED]" /><span className="font-display font-bold">KI-Debugging</span>
            {debugResult.offline && <span className="ml-auto text-[10px] px-2 py-0.5 rounded-full bg-[#F59E0B]/15 text-[#F59E0B]">Offline-Analyse</span>}
          </div>
          <p className="text-sm text-[#C9D6F0] mb-4">{debugResult.summary}</p>
          {(debugResult.issues || []).length === 0 ? (
            <p className="text-sm text-[#10B981] flex items-center gap-2"><CheckCircle2 size={16} />Keine Probleme gefunden.</p>
          ) : (
            <div className="space-y-2">
              {debugResult.issues.map((iss, i) => {
                const sev = { error: ["#EF4444", "Fehler"], warning: ["#F59E0B", "Warnung"], info: ["#4F8EF7", "Hinweis"] }[iss.severity] || ["#8A9BC0", "Hinweis"];
                return (
                  <div key={i} className="p-3 rounded-lg bg-[#0A0E1A] border border-[#1E2D4A]">
                    <div className="flex flex-wrap items-center gap-2 mb-1">
                      <span className="text-[11px] font-medium px-2 py-0.5 rounded-full" style={{ color: sev[0], background: sev[0] + "22" }}>{sev[1]}</span>
                      {iss.where && <span className="text-[10px] font-code uppercase text-[#4A5A7A]">{iss.where}</span>}
                      <span className="text-sm font-medium text-[#E8EDF5]">{iss.title}</span>
                    </div>
                    {iss.detail && <p className="text-xs text-[#8A9BC0] mb-1">{iss.detail}</p>}
                    {iss.fix && <p className="text-xs text-[#10B981] flex items-start gap-1.5"><span>💡</span><span>{iss.fix}</span></p>}
                  </div>
                );
              })}
            </div>
          )}
          {debugResult.offline && (
            <button onClick={openAiSettings} className="text-xs text-[#4F8EF7] hover:underline flex items-center gap-1.5 mt-3">
              <Settings size={12} />Für tiefere Analyse: echte KI aktivieren
            </button>
          )}
        </Card>
      )}

      {projects.length > 0 && (
        <div>
          <h2 className="font-display text-lg font-bold mb-3">Gespeicherte Projekte</h2>
          <Card className="divide-y divide-[#1E2D4A]">
            {projects.map((p) => (
              <div key={p.id} className="flex items-center gap-3 p-4">
                <Code2 size={18} className="text-[#4F8EF7] shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{p.name}</p>
                  <p className="text-xs text-[#8A9BC0]">{formatBytes(p.sizeBytes)} · {p.updatedAt}</p>
                </div>
                <Btn variant="ghost" size="sm" icon={Play} onClick={() => load(p)}>Laden</Btn>
                <Btn variant="danger" size="sm" icon={Trash2} onClick={() => deletePlaygroundProject(p.id)} />
              </div>
            ))}
          </Card>
        </div>
      )}
    </div>
  );
}

/* ========================= Admin-Dashboard ========================= */
function AdminDashboard({ ctx }) {
  const { me, users, reports, adminUpdateUser, adminDeleteUser, adminCreateAdmin, resolveReport, deleteReport, pushToast } = ctx;
  const [tab, setTab] = useState("users");
  const [query, setQuery] = useState("");
  const [editUser, setEditUser] = useState(null);
  const [editForm, setEditForm] = useState(null);
  const [newAdminOpen, setNewAdminOpen] = useState(false);
  const [newAdminForm, setNewAdminForm] = useState({ name: "", email: "", password: "" });

  const q = query.trim().toLowerCase();
  const list = users.filter((u) => !u.isGuest && (!q || u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)));
  const openReports = reports.filter((r) => r.status === "open");
  const resolvedReports = reports.filter((r) => r.status !== "open");

  const openEdit = (u) => { setEditUser(u); setEditForm({ name: u.name, email: u.email, role: u.role }); };
  const saveEdit = () => { adminUpdateUser(editUser.id, editForm); setEditUser(null); };

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
        <StatCard icon={Users} label="Registrierte Accounts" value={users.filter((u) => !u.isGuest).length} color="#4F8EF7" />
        <StatCard icon={Shield} label="Admins" value={users.filter((u) => u.role === "admin").length} color="#7C3AED" />
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
                      {u.id !== me.id && <button onClick={() => adminDeleteUser(u.id)} className="text-[#EF4444] hover:underline text-xs">Löschen</button>}
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
                {r.status === "open" && <Btn size="sm" variant="secondary" icon={Check} onClick={() => resolveReport(r.id)}>Als erledigt markieren</Btn>}
                <Btn size="sm" variant="danger" icon={Trash2} onClick={() => deleteReport(r.id)}>Löschen</Btn>
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
              <Field label="Passwort" type="password" value={newAdminForm.password} onChange={(e) => setNewAdminForm((f) => ({ ...f, password: e.target.value }))} />
            </div>
            <Btn className="w-full mt-4" icon={Check} onClick={() => { if (adminCreateAdmin(newAdminForm)) { setNewAdminOpen(false); setNewAdminForm({ name: "", email: "", password: "" }); } }}>Admin erstellen</Btn>
          </Card>
        </div>
      )}
    </div>
  );
}

/* =========================== Lesson View ========================== */
const TASK_XP = 15;

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
  const { selectedLesson, navigate, openCourse, me, addXP, showXP, completeLesson, celebrate, pushToast, logout, aiConfig, aiReady, openAiSettings } = ctx;
  const lesson = getFullLesson(selectedLesson);
  const [idx, setIdx] = useState(0);
  const [answers, setAnswers] = useState({});
  const [results, setResults] = useState({});
  const [rewarded, setRewarded] = useState({});
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
    setAnswers(init); setResults({}); setRewarded({}); setIdx(0);
  }, [selectedLesson]);

  if (!lesson) return null;
  const task = lesson.tasks[idx];
  const result = results[task.id];
  const isLast = idx === lesson.tasks.length - 1;
  const allChecked = lesson.tasks.every((t) => results[t.id]);

  const setAns = (val) => setAnswers((a) => ({ ...a, [task.id]: val }));

  const reward = (tid) => {
    if (rewarded[tid]) return;
    setRewarded((r) => ({ ...r, [tid]: true }));
    addXP(TASK_XP); showXP(TASK_XP);
  };

  const submit = async () => {
    const ans = answers[task.id];
    if (task.type === "multiple_choice") {
      if (ans == null) { pushToast("error", "Bitte wähle eine Antwort."); return; }
      const correct = ans === task.correctAnswer;
      setResults((r) => ({ ...r, [task.id]: { correct, score: correct ? 100 : 0, feedback: task.explanation } }));
      if (correct) { reward(task.id); pushToast("success", `Richtig! +${TASK_XP} XP`); } else pushToast("error", "Nicht ganz — versuch es nochmal!");
      return;
    }

    // Alle anderen Aufgabentypen (Lückentext, Code, Erklären) werden geprüft —
    // per echter KI, wenn ein API-Key hinterlegt ist, sonst per Offline-Heuristik.
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
    setResults((r) => ({ ...r, [task.id]: res }));

    if (res.correct) {
      reward(task.id);
      pushToast("success", `Richtig! +${TASK_XP} XP`);
      if (res.score >= 95 && !me.badges.includes("ai_master")) {
        setTimeout(() => pushToast("badge", `Neues Abzeichen: ${BADGES.ai_master.label}!`), 400);
      }
    } else {
      pushToast("error", "Versuch es nochmal — du schaffst das!");
    }
  };

  const retry = () => setResults((r) => { const n = { ...r }; delete n[task.id]; return n; });

  const finish = () => {
    if (!alreadyDone) {
      completeLesson(lesson.id, lesson.xpReward);
      celebrate();
      setTimeout(() => { showXP(lesson.xpReward); }, 200);
      pushToast("success", `Lektion abgeschlossen! +${lesson.xpReward} XP 🎉`);
      setTimeout(() => openCourse(lesson._course.id), 1400);
    } else {
      pushToast("info", "Lektion bereits abgeschlossen.");
      openCourse(lesson._course.id);
    }
  };

  // Tastatur-Shortcuts: 1–4 wählt Multiple-Choice-Option, Enter prüft/weiter
  useEffect(() => {
    const onKey = (e) => {
      const tag = (e.target.tagName || "").toLowerCase();
      if (tag === "textarea" || tag === "input") return;
      if (task.type === "multiple_choice" && !result && /^[1-9]$/.test(e.key)) {
        const n = parseInt(e.key, 10) - 1;
        if (n < task.options.length) { e.preventDefault(); setAns(n); }
      } else if (e.key === "Enter") {
        e.preventDefault();
        if (!result && !aiLoading) submit();
        else if (result) { isLast ? finish() : setIdx((i) => i + 1); }
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
          <button onClick={() => openCourse(lesson._course.id)} className="flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5] shrink-0"><ArrowLeft size={16} /><span className="hidden sm:inline">Zurück</span></button>
          <div className="flex-1 min-w-0 text-sm text-[#8A9BC0] truncate">
            <span>{lesson._course.icon} {lesson._course.name}</span><span className="mx-2 text-[#4A5A7A]">/</span><span className="text-[#E8EDF5]">{lesson.title}</span>
          </div>
          <span className="hidden sm:inline text-sm text-[#8A9BC0] shrink-0 whitespace-nowrap">Aufgabe {idx + 1} von {lesson.tasks.length}</span>
          <button onClick={openAiSettings} aria-label="KI-Einstellungen" title={aiReady ? "KI verbunden" : "KI einrichten"}
            className="shrink-0 w-8 h-8 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#2A3F6F] flex items-center justify-center relative">
            <Settings size={14} className="text-[#8A9BC0]" />
            <span className={`absolute top-0.5 right-0.5 w-1.5 h-1.5 rounded-full ${aiReady ? "bg-[#10B981]" : "bg-[#4A5A7A]"}`} />
          </button>
          <button onClick={logout} aria-label="Abmelden" title="Abmelden"
            className="shrink-0 w-8 h-8 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#EF4444] hover:text-[#EF4444] flex items-center justify-center text-[#8A9BC0]">
            <LogOut size={14} />
          </button>
        </div>
        <div className="h-1 bg-[#1A2540]"><div className="h-1 transition-all duration-500" style={{ width: ((idx + (result ? 1 : 0)) / lesson.tasks.length) * 100 + "%", background: GRADIENT }} /></div>
      </header>

      <div className="max-w-6xl mx-auto px-4 lg:px-6 py-6 grid lg:grid-cols-5 gap-6">
        {/* Theorie */}
        <div className="lg:col-span-3">
          <div className="flex items-center gap-2 mb-3 text-[#8A9BC0]"><BookOpen size={18} /><span className="font-display font-bold text-[#E8EDF5]">Theorie</span><DifficultyBadge level={lesson.level} /><span className="ml-auto flex items-center gap-1 text-xs"><Clock size={13} />{lesson.estimatedMinutes} Min</span></div>
          <Card className="p-6"><Markdown text={lesson.theory} /></Card>
        </div>

        {/* Aufgabe */}
        <div className="lg:col-span-2">
          <div className="lg:sticky lg:top-20">
            <div className="flex items-center gap-2 mb-3 text-[#8A9BC0]"><PenLine size={18} /><span className="font-display font-bold text-[#E8EDF5]">Aufgabe</span><span className="ml-auto flex items-center gap-1 text-xs text-[#F7C948]"><Star size={13} />{lesson.xpReward} XP</span></div>
            <Card className="p-5">
              <p className="font-medium text-[#E8EDF5] mb-4">{renderInline(task.question, "q")}</p>

              {/* Multiple Choice */}
              {task.type === "multiple_choice" && (
                <div className="space-y-2">
                  {task.options.map((opt, i) => {
                    const sel = answers[task.id] === i;
                    const checked = !!result;
                    const isCorrect = i === task.correctAnswer;
                    let cls = "border-[#1E2D4A] hover:border-[#2A3F6F]";
                    if (checked && isCorrect) cls = "border-[#10B981] bg-[#10B981]/10";
                    else if (checked && sel && !isCorrect) cls = "border-[#EF4444] bg-[#EF4444]/10";
                    else if (sel) cls = "border-[#4F8EF7] bg-[#4F8EF7]/10";
                    return (
                      <button key={i} disabled={checked} onClick={() => setAns(i)}
                        className={`w-full text-left px-4 py-3 rounded-lg border transition-all flex items-center gap-3 ${cls}`}>
                        <span className="w-6 h-6 rounded-full border border-current text-[#8A9BC0] flex items-center justify-center text-xs shrink-0">{String.fromCharCode(65 + i)}</span>
                        <span className="text-sm text-[#E8EDF5] flex-1">{opt}</span>
                        {checked && isCorrect && <CheckCircle2 size={18} className="text-[#10B981]" />}
                        {checked && sel && !isCorrect && <XCircle size={18} className="text-[#EF4444]" />}
                      </button>
                    );
                  })}
                </div>
              )}

              {/* Code schreiben — VS-Code-Editor (Monaco) */}
              {task.type === "code_write" && (
                <MonacoCodeEditor value={answers[task.id] || ""} onChange={setAns} disabled={!!result} courseId={lesson._course.id} label={lesson._course.name} />
              )}

              {/* Lückentext */}
              {task.type === "fill_blank" && (
                <div className="text-sm leading-loose text-[#C9D6F0]">
                  {task.template.split("___").map((seg, i) => (
                    <React.Fragment key={i}>
                      {seg}
                      {i < task.blanks.length && (
                        <input value={answers[task.id]?.[i] || ""} disabled={!!result}
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
                {!result && (
                  <Btn className="flex-1" onClick={submit} disabled={aiLoading} icon={aiLoading ? undefined : Send}>
                    {aiLoading ? <><Loader2 size={16} className="ld-spin" />Prüft …</> : "Prüfen"}
                  </Btn>
                )}
                {result && !result.correct && <Btn variant="secondary" onClick={retry} icon={ArrowLeft}>Nochmal</Btn>}
                {result && (isLast
                  ? <Btn className="flex-1" onClick={finish} icon={Trophy}>Lektion abschließen</Btn>
                  : <Btn className="flex-1" onClick={() => setIdx((i) => i + 1)} icon={ArrowRight}>Nächste Aufgabe</Btn>)}
              </div>

              {/* MC Feedback (Lückentext/Code/Erklären laufen über die KI-Bewertung unten) */}
              {result && task.type === "multiple_choice" && (
                <div className={`mt-4 p-3 rounded-lg text-sm ${result.correct ? "bg-[#10B981]/10 text-[#10B981]" : "bg-[#EF4444]/10 text-[#C9D6F0]"}`}>
                  <div className="flex items-center gap-1.5 font-medium mb-1">{result.correct ? <CheckCircle2 size={15} /> : <XCircle size={15} className="text-[#EF4444]" />}{result.correct ? "Richtig!" : "Leider falsch"}</div>
                  <p className="text-[#C9D6F0]">{result.feedback}</p>
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
              {task.type === "multiple_choice" ? "Tipp: Tasten 1–4 zum Wählen · " : ""}<kbd className="font-code px-1 py-0.5 rounded bg-[#141D35] border border-[#1E2D4A]">Enter</kbd> zum {result ? "Weiter" : "Prüfen"}
            </p>

            {/* Task-Navigation Punkte */}
            <div className="flex items-center justify-center gap-1.5 mt-5">
              {lesson.tasks.map((t, i) => (
                <button key={t.id} onClick={() => setIdx(i)}
                  className={`h-2 rounded-full transition-all ${i === idx ? "w-6 bg-[#4F8EF7]" : results[t.id] ? "w-2 bg-[#10B981]" : "w-2 bg-[#2A3F6F]"}`} title={`Aufgabe ${i + 1}`} />
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}








