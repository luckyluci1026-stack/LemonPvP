import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Code2, Sparkles, Zap, Flame, Star, Shield, Trophy, CheckCircle2, XCircle,
  BookOpen, GraduationCap, Users, LogOut, ChevronRight, ChevronDown, ChevronLeft,
  Lock, Copy, Check, ArrowRight, ArrowLeft, Home, User, Award, Bot, Send, Loader2,
  Menu, X, Mail, Eye, Layers, BarChart3, Clock, Play, Plus, Globe, Palette,
  Database, Terminal, Rocket, Brain, PenLine, ListChecks, TrendingUp, Crown,
  Medal, KeyRound, AtSign, Cpu, Coffee, LayoutDashboard
} from "lucide-react";

/* =========================================================================
   LearnDeveloping — learndeveloping.com
   Eine Single-File Lernplattform (React + Tailwind + lucide-react)
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
];

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
const DEMO_USERS = [
  {
    id: "teacher_1", role: "teacher", name: "Prof. Anna Schmidt", email: "lehrer@demo.de",
    password: "lehrer123", school: "Gymnasium Berlin-Mitte", schoolCode: "LRND-4K9M",
    students: ["student_1", "student_2", "student_3"], createdAt: "2024-01-01", avatar: "👩‍🏫",
  },
  {
    id: "student_1", role: "student", name: "Max Müller", email: "max@demo.de", password: "schueler123",
    teacherId: "teacher_1", xp: 1250, streak: 7, completedLessons: ["html_1_1", "html_1_2", "html_1_3", "javascript_1_1", "javascript_1_2"],
    currentCourse: "javascript", joinedAt: "2024-01-15", lastLogin: "Heute", avatar: "🧑‍💻", badges: ["first_lesson", "week_warrior"],
  },
  {
    id: "student_2", role: "student", name: "Sarah Becker", email: "sarah@demo.de", password: "schueler123",
    teacherId: "teacher_1", xp: 870, streak: 3, completedLessons: ["html_1_1", "html_1_2", "css_1_1"],
    currentCourse: "html", joinedAt: "2024-02-02", lastLogin: "Vor 2 Std.", avatar: "👩‍💻", badges: ["first_lesson"],
  },
  {
    id: "student_3", role: "student", name: "Tom Weber", email: "tom@demo.de", password: "schueler123",
    teacherId: "teacher_1", xp: 2100, streak: 14,
    completedLessons: ["html_1_1", "html_1_2", "html_1_3", "html_1_4", "html_2_1", "javascript_1_1", "javascript_1_2", "javascript_1_3"],
    currentCourse: "javascript", joinedAt: "2024-01-08", lastLogin: "Gestern", avatar: "👨‍🎓", badges: ["first_lesson", "week_warrior", "js_beginner"],
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
async function checkAnswerWithAI(task, userAnswer, language, lessonTitle) {
  const systemPrompt = `Du bist ein freundlicher aber präziser Programmier-Lehrer.
Du bewertest Antworten von Schülern die Programmieren lernen.

DEINE AUFGABE:
- Analysiere die Antwort auf Korrektheit
- Gib konstruktives, ermutigendes Feedback
- Erkläre was richtig/falsch ist
- Bei Code: Prüfe ob das Konzept verstanden wurde, nicht nur syntaktische Korrektheit
- Halte die Antwort kurz und klar (max 3-4 Sätze)
- Antworte IMMER auf Deutsch
- Fang nie mit "Ich" an

ANTWORTE NUR IN DIESEM JSON FORMAT (keine anderen Zeichen davor oder danach):
{
  "correct": true/false,
  "score": 0-100,
  "feedback": "Dein Feedback hier",
  "hint": "Optional: Tipp falls falsch",
  "praise": "Kurzes Lob falls richtig"
}`;

  const userPrompt = `Sprache: ${language}
Lektion: ${lessonTitle}
Aufgabe: ${task.question}
${task.expectedConcepts ? `Erwartete Konzepte: ${task.expectedConcepts.join(", ")}` : ""}
Schüler-Antwort: ${userAnswer}

Bitte bewerte diese Antwort.`;

  try {
    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: { "Content-Type": "application/json", "anthropic-version": "2023-06-01" },
      body: JSON.stringify({
        model: "claude-sonnet-4-6",
        max_tokens: 1000,
        system: systemPrompt,
        messages: [{ role: "user", content: userPrompt }],
      }),
    });
    if (!response.ok) throw new Error("API " + response.status);
    const data = await response.json();
    const text = data.content[0].text;
    const parsed = JSON.parse(text.replace(/```json|```/g, "").trim());
    return { ...parsed, offline: false };
  } catch (e) {
    // Graceful Fallback: lokale Heuristik, damit die Plattform auch ohne
    // erreichbare API nutzbar bleibt.
    return heuristicCheck(task, userAnswer);
  }
}

function heuristicCheck(task, userAnswer) {
  const ans = (userAnswer || "").trim();
  const low = ans.toLowerCase();
  if (task.type === "code_write") {
    const concepts = task.expectedConcepts || [];
    let matched = 0;
    const missing = [];
    for (const c of concepts) {
      const lc = c.toLowerCase();
      let hit;
      if (/^[=+\-*/<>!%&|]+$/.test(c.trim())) hit = ans.includes(c.trim());            // Operatoren
      else if (lc.includes("string")) hit = /["'`]/.test(ans);                          // String-Literal
      else if (lc.includes("number") || lc === "zahl") hit = /\d/.test(ans);            // Zahl
      else {
        const tokens = lc.split(/\s+oder\s+|\s+/).filter((t) => t.length > 1);
        hit = low.includes(lc) || tokens.some((t) => low.includes(t));
      }
      if (hit) matched++;
      else missing.push(c);
    }
    const score = concepts.length ? Math.round((matched / concepts.length) * 100) : (ans ? 80 : 0);
    const correct = score >= 60 && ans.length > 0;
    return {
      correct, score, offline: true,
      feedback: correct
        ? `Stark! Deine Lösung enthält die erwarteten Bausteine (${matched}/${concepts.length}).`
        : `Fast! Es fehlen noch wichtige Bausteine in deiner Lösung.`,
      hint: correct ? "" : `Achte auf: ${missing.join(", ")}.`,
      praise: correct ? "Sauber umgesetzt." : "",
    };
  }
  // explain / Freitext
  const words = ans.split(/\s+/).filter(Boolean).length;
  const correct = words >= 8;
  const score = Math.max(0, Math.min(100, words * 8));
  return {
    correct, score, offline: true,
    feedback: correct
      ? "Gute, durchdachte Erklärung — du bringst die Kernidee klar auf den Punkt."
      : "Deine Erklärung ist noch sehr kurz. Geh etwas mehr ins Detail.",
    hint: correct ? "" : "Beschreibe das „Warum“ in 1–2 vollständigen Sätzen.",
    praise: correct ? "Verständlich erklärt." : "",
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

function Btn({ children, onClick, variant = "primary", className = "", icon: Icon, disabled, type, size = "md" }) {
  const base = "inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed";
  const sz = size === "sm" ? "text-sm px-3 py-1.5" : size === "lg" ? "text-base px-6 py-3.5" : "text-sm px-4 py-2.5";
  const styles = {
    primary: "text-white shadow-lg hover:opacity-90",
    secondary: "bg-transparent border border-[#2A3F6F] text-[#E8EDF5] hover:border-[#4F8EF7] hover:bg-white/5",
    ghost: "bg-transparent text-[#8A9BC0] hover:text-[#E8EDF5] hover:bg-white/5",
    danger: "bg-[#EF4444]/10 text-[#EF4444] border border-[#EF4444]/30 hover:bg-[#EF4444]/20",
  };
  return (
    <button type={type} disabled={disabled} onClick={onClick}
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

/* ============================ Main App ============================= */
function genSchoolCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "";
  for (let i = 0; i < 4; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return "LRND-" + s;
}
const uid = () => "u_" + Math.random().toString(36).slice(2, 9);

export default function App() {
  const [view, setView] = useState("landing");
  const [users, setUsers] = useState(DEMO_USERS);
  const [currentUser, setCurrentUser] = useState(null);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [selectedLesson, setSelectedLesson] = useState(null);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [toasts, setToasts] = useState([]);
  const [xpPopup, setXpPopup] = useState(null);
  const [confetti, setConfetti] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const me = currentUser ? users.find((u) => u.id === currentUser) : null;

  const celebrate = useCallback(() => { setConfetti(true); setTimeout(() => setConfetti(false), 3200); }, []);

  const pushToast = useCallback((type, msg) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t, { id, type, msg }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3200);
  }, []);

  const showXP = useCallback((amount) => {
    setXpPopup(amount);
    setTimeout(() => setXpPopup(null), 1600);
  }, []);

  const navigate = useCallback((v) => { setView(v); setSidebarOpen(false); window.scrollTo(0, 0); }, []);

  const login = (email, password) => {
    const u = users.find((x) => x.email.toLowerCase() === email.trim().toLowerCase() && x.password === password);
    if (!u) { pushToast("error", "E-Mail oder Passwort falsch."); return false; }
    setCurrentUser(u.id);
    pushToast("success", `Willkommen zurück, ${u.name.split(" ")[0]}!`);
    navigate(u.role === "teacher" ? "teacher" : "dashboard");
    return true;
  };

  const register = (form) => {
    if (!form.name || !form.email || !form.password) { pushToast("error", "Bitte alle Pflichtfelder ausfüllen."); return false; }
    if (form.password !== form.confirm) { pushToast("error", "Passwörter stimmen nicht überein."); return false; }
    if (users.some((u) => u.email.toLowerCase() === form.email.trim().toLowerCase())) { pushToast("error", "E-Mail ist bereits registriert."); return false; }
    const id = uid();
    let newUser;
    if (form.role === "teacher") {
      const code = genSchoolCode();
      newUser = { id, role: "teacher", name: form.name, email: form.email, password: form.password, school: form.school || "—", schoolCode: code, students: [], createdAt: "Heute", avatar: "👨‍🏫" };
      pushToast("info", `Dein Schul-Code: ${code}`);
    } else {
      let teacherId = null;
      if (form.teacherCode) {
        const t = users.find((u) => u.role === "teacher" && u.schoolCode.toLowerCase() === form.teacherCode.trim().toLowerCase());
        if (t) { teacherId = t.id; setUsers((us) => us.map((u) => u.id === t.id ? { ...u, students: [...u.students, id] } : u)); }
        else pushToast("error", "Lehrer-Code nicht gefunden — du lernst erstmal selbstständig.");
      }
      newUser = { id, role: "student", name: form.name, email: form.email, password: form.password, teacherId, xp: 0, streak: 1, completedLessons: [], currentCourse: null, joinedAt: "Heute", lastLogin: "Jetzt", avatar: "🧑‍💻", badges: [] };
    }
    setUsers((us) => [...us, newUser]);
    setCurrentUser(id);
    pushToast("success", `Account erstellt — los geht's, ${form.name.split(" ")[0]}!`);
    navigate(form.role === "teacher" ? "teacher" : "dashboard");
    return true;
  };

  const logout = () => { setCurrentUser(null); navigate("landing"); pushToast("info", "Abgemeldet. Bis bald!"); };

  const openCourse = (courseId) => {
    setSelectedCourse(courseId);
    setUsers((us) => us.map((u) => u.id === currentUser ? { ...u, currentCourse: courseId } : u));
    navigate("course");
  };
  const openLesson = (lessonId) => { setSelectedLesson(lessonId); navigate("lesson"); };

  // XP / Lektion abschließen
  const addXP = useCallback((amount) => {
    setUsers((us) => us.map((u) => u.id === currentUser ? { ...u, xp: u.xp + amount } : u));
  }, [currentUser]);

  const completeLesson = useCallback((lessonId, bonusXp) => {
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
  }, [currentUser, pushToast]);

  const ctx = {
    view, navigate, users, me, setUsers, currentUser,
    selectedCourse, openCourse, selectedLesson, openLesson,
    selectedStudent, setSelectedStudent, login, register, logout,
    pushToast, showXP, addXP, completeLesson, celebrate, sidebarOpen, setSidebarOpen,
  };

  let screen = null;
  if (view === "landing") screen = <Landing ctx={ctx} />;
  else if (view === "login" || view === "register") screen = <AuthScreen ctx={ctx} mode={view} />;
  else if (view === "lesson") screen = <LessonView ctx={ctx} />;
  else screen = <AppShell ctx={ctx}>{
    view === "dashboard" ? <StudentDashboard ctx={ctx} /> :
    view === "teacher" ? <TeacherDashboard ctx={ctx} /> :
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
        <div className="max-w-6xl mx-auto px-5 h-16 flex items-center justify-between">
          <Logo onClick={() => navigate("landing")} />
          <div className="hidden md:flex items-center gap-7 text-sm text-[#8A9BC0]">
            <a href="#kurse" className="hover:text-[#E8EDF5] transition-colors">Kurse</a>
            <a href="#features" className="hover:text-[#E8EDF5] transition-colors">Features</a>
            <a href="#rollen" className="hover:text-[#E8EDF5] transition-colors">Für Schulen</a>
          </div>
          <div className="flex items-center gap-3">
            <Btn variant="ghost" size="sm" onClick={() => navigate("login")}>Anmelden</Btn>
            <Btn size="sm" icon={Rocket} onClick={() => navigate("register")}>Jetzt starten</Btn>
          </div>
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
            Von HTML bis C++ — strukturiert, modern und mit KI-Feedback direkt zu deinem Code.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 mb-8">
            <Btn size="lg" icon={ArrowRight} onClick={() => navigate("register")}>Jetzt kostenlos starten</Btn>
            <Btn size="lg" variant="secondary" icon={Eye} onClick={() => ctx.login("max@demo.de", "schueler123")}>Demo ansehen</Btn>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm text-[#4A5A7A]">
            <span className="flex items-center gap-1.5"><Check size={14} className="text-[#10B981]" />Kostenlos starten</span>
            <span className="flex items-center gap-1.5"><Check size={14} className="text-[#10B981]" />Keine Kreditkarte</span>
            <span className="flex items-center gap-1.5"><Check size={14} className="text-[#10B981]" />KI-Feedback</span>
          </div>
        </div>
      </section>

      {/* Kurse */}
      <section id="kurse" className="max-w-6xl mx-auto px-5 py-20">
        <div className="text-center mb-12">
          <h2 className="font-display text-4xl font-extrabold mb-3">7 Sprachen. <span className="ld-gradient-text">Ein Ziel.</span></h2>
          <p className="text-[#8A9BC0] text-lg">Strukturierte Kurse von Anfänger bis Experte.</p>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {COURSES.map((c) => (
            <Card key={c.id} hover onClick={() => navigate("register")} className="p-5 group relative overflow-hidden">
              <div className="text-4xl mb-3">{c.icon}</div>
              <h3 className="font-display text-lg font-bold mb-1">{c.name}</h3>
              <p className="text-sm text-[#8A9BC0] mb-4 leading-snug">{c.description}</p>
              <div className="flex items-center gap-1.5 text-xs text-[#4A5A7A]"><BookOpen size={13} />{c.totalLessons} Lektionen</div>
              <div className="absolute bottom-0 inset-x-0 h-1" style={{ background: c.color }} />
            </Card>
          ))}
        </div>
      </section>

      {/* Wie es funktioniert */}
      <section id="features" className="bg-[#0F1629] border-y border-[#1E2D4A] py-20">
        <div className="max-w-6xl mx-auto px-5">
          <h2 className="font-display text-4xl font-extrabold text-center mb-12">So funktioniert's</h2>
          <div className="grid md:grid-cols-3 gap-6">
            {[
              { icon: BookOpen, emoji: "📖", t: "Lerne die Theorie", d: "Klare Erklärungen mit echten Code-Beispielen — kein trockenes Geschwafel." },
              { icon: PenLine, emoji: "✍️", t: "Löse Aufgaben", d: "Multiple Choice, Code schreiben, Lückentext und freies Erklären." },
              { icon: Bot, emoji: "🤖", t: "KI prüft dich", d: "Sofortiges, konstruktives Feedback zu deinem Code von Claude AI." },
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

      {/* Footer */}
      <footer className="border-t border-[#1E2D4A] py-10">
        <div className="max-w-6xl mx-auto px-5 flex flex-col md:flex-row items-center justify-between gap-4">
          <div>
            <Logo />
            <p className="text-sm text-[#4A5A7A] mt-2">Code lernen. Richtig lernen.</p>
          </div>
          <div className="flex gap-6 text-sm text-[#8A9BC0]">
            <a href="#" className="hover:text-[#E8EDF5]">Über uns</a>
            <a href="#" className="hover:text-[#E8EDF5]">Datenschutz</a>
            <a href="#" className="hover:text-[#E8EDF5]">Kontakt</a>
          </div>
          <p className="text-xs text-[#4A5A7A]">© 2026 LearnDeveloping</p>
        </div>
      </footer>
    </div>
  );
}

/* ============================ Auth ================================= */
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

function AuthScreen({ ctx, mode }) {
  const { navigate, login, register } = ctx;
  const isLogin = mode === "login";
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "", role: "student", teacherCode: "", school: "" });
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = () => { if (isLogin) login(form.email, form.password); else register(form); };

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

            <Btn className="w-full" size="lg" onClick={submit} icon={isLogin ? ArrowRight : Rocket}>
              {isLogin ? "Anmelden" : "Account erstellen"}
            </Btn>
          </div>

          {isLogin && (
            <div className="mt-6 pt-5 border-t border-[#1E2D4A] text-sm">
              <p className="text-[#4A5A7A] mb-2 font-medium">Demo-Zugänge:</p>
              <div className="space-y-1 text-[#8A9BC0]">
                <p className="flex items-center gap-2">👨‍🏫 <span className="font-code text-xs">lehrer@demo.de / lehrer123</span></p>
                <p className="flex items-center gap-2">🎓 <span className="font-code text-xs">max@demo.de / schueler123</span></p>
              </div>
            </div>
          )}
        </div>
        <button onClick={() => navigate("landing")} className="mt-5 mx-auto flex items-center gap-1.5 text-sm text-[#8A9BC0] hover:text-[#E8EDF5]">
          <ChevronLeft size={15} /> Zurück zur Startseite
        </button>
      </div>
    </div>
  );
}

/* ============================ App Shell =========================== */
function AppShell({ ctx, children }) {
  const { me, view, navigate, logout, sidebarOpen, setSidebarOpen } = ctx;
  if (!me) return null;
  const lvl = me.role === "student" ? getLevelInfo(me.xp) : null;

  const studentNav = [
    { v: "dashboard", label: "Übersicht", icon: Home },
    { v: "courses", label: "Meine Kurse", icon: BookOpen },
    { v: "leaderboard", label: "Rangliste", icon: Trophy },
    { v: "profile", label: "Profil", icon: User },
  ];
  const teacherNav = [
    { v: "teacher", label: "Übersicht", icon: LayoutDashboard },
    { v: "leaderboard", label: "Rangliste", icon: Trophy },
    { v: "profile", label: "Profil", icon: User },
  ];
  const nav = me.role === "teacher" ? teacherNav : studentNav;
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
              <button onClick={() => navigate("profile")} aria-label="Profil" className="text-2xl w-10 h-10 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#2A3F6F] flex items-center justify-center transition-colors">{me.avatar}</button>
            </div>
          </div>
        </header>
        <main className="px-4 lg:px-8 py-6 max-w-6xl mx-auto">{children}</main>
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
              <div className="text-2xl">{s.avatar}</div>
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
function Profile({ ctx }) {
  const { me, pushToast, setUsers } = ctx;
  const isStudent = me.role === "student";
  const lvl = isStudent ? getLevelInfo(me.xp) : null;
  const [picker, setPicker] = useState(false);
  const copy = (txt) => { try { navigator.clipboard.writeText(txt); } catch (e) {} pushToast("success", "In Zwischenablage kopiert!"); };
  const setAvatar = (a) => { setUsers((us) => us.map((u) => u.id === me.id ? { ...u, avatar: a } : u)); setPicker(false); pushToast("success", "Avatar aktualisiert!"); };
  return (
    <div className="space-y-6 max-w-3xl">
      <Card className="p-6">
        <div className="flex flex-col sm:flex-row items-center gap-5">
          <button onClick={() => setPicker((p) => !p)} aria-label="Avatar ändern"
            className="relative text-6xl w-24 h-24 rounded-2xl bg-[#0A0E1A] border border-[#1E2D4A] hover:border-[#4F8EF7] flex items-center justify-center transition-colors group">
            {me.avatar}
            <span className="absolute bottom-1 right-1 w-6 h-6 rounded-full bg-[#141D35] border border-[#1E2D4A] flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"><PenLine size={12} className="text-[#4F8EF7]" /></span>
          </button>
          <div className="flex-1 text-center sm:text-left">
            <h1 className="font-display text-2xl font-bold">{me.name}</h1>
            <p className="text-[#8A9BC0]">{me.email}</p>
            <span className="inline-flex items-center gap-1.5 mt-2 text-xs px-2.5 py-1 rounded-full bg-[#4F8EF7]/15 text-[#4F8EF7]">
              {isStudent ? <GraduationCap size={13} /> : <Users size={13} />}{isStudent ? "Schüler" : "Lehrer"}
            </span>
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
            <p className="text-sm text-[#8A9BC0] mb-3">Wähle deinen Avatar:</p>
            <div className="flex flex-wrap gap-2">
              {AVATARS.map((a) => (
                <button key={a} onClick={() => setAvatar(a)} aria-label={`Avatar ${a}`}
                  className={`text-2xl w-12 h-12 rounded-xl flex items-center justify-center transition-all ${me.avatar === a ? "bg-[#4F8EF7]/15 border border-[#4F8EF7]" : "bg-[#0A0E1A] border border-[#1E2D4A] hover:border-[#2A3F6F]"}`}>{a}</button>
              ))}
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
      ) : (
        <div className="grid sm:grid-cols-2 gap-4">
          <StatCard icon={Users} label="Schüler" value={me.students.length} color="#4F8EF7" />
          <Card className="p-4">
            <p className="text-xs text-[#8A9BC0] mb-1">Dein Schul-Code</p>
            <button onClick={() => copy(me.schoolCode)} className="flex items-center gap-2 font-display font-bold text-xl text-[#F7C948]">{me.schoolCode}<Copy size={16} className="text-[#8A9BC0]" /></button>
            <p className="text-xs text-[#4A5A7A] mt-1">{me.school}</p>
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
                      <td className="px-4 py-3"><div className="flex items-center gap-2"><span className="text-xl">{s.avatar}</span><div><div className="font-medium text-[#E8EDF5]">{s.name}</div><div className="text-xs text-[#4A5A7A]">Lvl {lvl.level}</div></div></div></td>
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
              <span className="text-4xl">{detailStudent.avatar}</span>
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

/* =========================== Lesson View ========================== */
const TASK_XP = 15;

function AIFeedback({ result }) {
  if (!result) return null;
  const good = result.correct;
  return (
    <Card className="p-5 mt-5" >
      <div className="flex items-center gap-2 mb-3 pb-3 border-b border-[#1E2D4A]">
        <Bot size={18} className="text-[#7C3AED]" />
        <span className="font-display font-bold">KI-Bewertung</span>
        {result.offline && <span className="ml-auto text-[10px] px-2 py-0.5 rounded-full bg-[#F59E0B]/15 text-[#F59E0B]">Offline-Heuristik</span>}
      </div>
      <div className="flex items-center gap-2 mb-3">
        {good ? <CheckCircle2 size={20} className="text-[#10B981]" /> : <XCircle size={20} className="text-[#EF4444]" />}
        <span className={`font-semibold ${good ? "text-[#10B981]" : "text-[#EF4444]"}`}>{good ? "Richtig!" : "Noch nicht ganz"}</span>
        {typeof result.score === "number" && <span className="text-sm text-[#8A9BC0]">(Score: {result.score}/100)</span>}
      </div>
      <p className="text-sm text-[#C9D6F0] leading-relaxed mb-2">{result.feedback}</p>
      {good && result.praise && <p className="text-sm text-[#10B981] mb-2">🎉 {result.praise}</p>}
      {!good && result.hint && <p className="text-sm text-[#F59E0B] flex items-start gap-1.5"><span>💡</span><span>{result.hint}</span></p>}
    </Card>
  );
}

function LessonView({ ctx }) {
  const { selectedLesson, navigate, openCourse, me, addXP, showXP, completeLesson, celebrate, pushToast } = ctx;
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
    } else if (task.type === "fill_blank") {
      const correct = task.blanks.every((b, i) => (ans?.[i] || "").trim().toLowerCase() === b.toLowerCase());
      setResults((r) => ({ ...r, [task.id]: { correct, score: correct ? 100 : 0, feedback: correct ? "Alle Lücken korrekt ausgefüllt!" : `Richtig wäre: ${task.blanks.join(", ")}.` } }));
      if (correct) { reward(task.id); pushToast("success", `Richtig! +${TASK_XP} XP`); } else pushToast("error", "Nicht ganz — schau nochmal hin!");
    } else {
      // KI-geprüft
      if (!String(ans || "").trim()) { pushToast("error", "Bitte gib zuerst eine Antwort ein."); return; }
      setAiLoading(true);
      const res = await checkAnswerWithAI(task, ans, lesson._course.name, lesson.title);
      setAiLoading(false);
      setResults((r) => ({ ...r, [task.id]: res }));
      if (res.correct) { reward(task.id); pushToast("success", `Gut gemacht! +${TASK_XP} XP`); if (res.score >= 95 && !me.badges.includes("ai_master")) setTimeout(() => pushToast("badge", `Neues Abzeichen: ${BADGES.ai_master.label}!`), 400); }
      else pushToast("error", "Versuch es nochmal — du schaffst das!");
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
          <span className="text-sm text-[#8A9BC0] shrink-0 whitespace-nowrap">Aufgabe {idx + 1} von {lesson.tasks.length}</span>
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

              {/* Code schreiben */}
              {task.type === "code_write" && (
                <CodeEditor value={answers[task.id] || ""} onChange={setAns} disabled={!!result} lang={lesson._course.name} />
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
                  <Btn className="flex-1" onClick={submit} disabled={aiLoading} icon={aiLoading ? undefined : (task.aiCheck ? Bot : Send)}>
                    {aiLoading ? <><Loader2 size={16} className="ld-spin" />KI prüft …</> : task.aiCheck ? "Mit KI prüfen" : "Antwort prüfen"}
                  </Btn>
                )}
                {result && !result.correct && <Btn variant="secondary" onClick={retry} icon={ArrowLeft}>Nochmal</Btn>}
                {result && (isLast
                  ? <Btn className="flex-1" onClick={finish} icon={Trophy}>Lektion abschließen</Btn>
                  : <Btn className="flex-1" onClick={() => setIdx((i) => i + 1)} icon={ArrowRight}>Nächste Aufgabe</Btn>)}
              </div>

              {/* MC/Fill Feedback */}
              {result && (task.type === "multiple_choice" || task.type === "fill_blank") && (
                <div className={`mt-4 p-3 rounded-lg text-sm ${result.correct ? "bg-[#10B981]/10 text-[#10B981]" : "bg-[#EF4444]/10 text-[#C9D6F0]"}`}>
                  <div className="flex items-center gap-1.5 font-medium mb-1">{result.correct ? <CheckCircle2 size={15} /> : <XCircle size={15} className="text-[#EF4444]" />}{result.correct ? "Richtig!" : "Leider falsch"}</div>
                  <p className="text-[#C9D6F0]">{result.feedback}</p>
                </div>
              )}
            </Card>

            {/* KI-Feedback */}
            {aiLoading && task.aiCheck && <SkeletonFeedback />}
            {result && task.aiCheck && <AIFeedback result={result} />}

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








