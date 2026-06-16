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
      .ld-skeleton { background: linear-gradient(90deg,#141D35 25%,#1A2540 50%,#141D35 75%); background-size: 200% 100%; animation: ld-pulse 1.5s ease-in-out infinite; }
      .ld-spin { animation: ld-spin 1s linear infinite; }
      textarea, input { outline: none; }
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

const LESSON_CONTENT = {
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
      headers: { "Content-Type": "application/json" },
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
  const regex = /(`[^`]+`)|(\*\*[^*]+\*\*)|(\*[^*]+\*)/g;
  let last = 0, m, i = 0;
  while ((m = regex.exec(text))) {
    if (m.index > last) parts.push(text.slice(last, m.index));
    if (m[1]) parts.push(<code key={kp + "c" + i} className="font-code text-[13px] px-1.5 py-0.5 rounded bg-[#0A0E1A] border border-[#1E2D4A] text-[#4F8EF7]">{m[1].slice(1, -1)}</code>);
    else if (m[2]) parts.push(<strong key={kp + "b" + i} className="font-semibold text-[#E8EDF5]">{m[2].slice(2, -2)}</strong>);
    else if (m[3]) parts.push(<em key={kp + "i" + i} className="italic text-[#8A9BC0]">{m[3].slice(1, -1)}</em>);
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
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const me = currentUser ? users.find((u) => u.id === currentUser) : null;

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
    pushToast, showXP, addXP, completeLesson, sidebarOpen, setSidebarOpen,
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
            <button className="lg:hidden text-[#8A9BC0]" onClick={() => setSidebarOpen(true)}><Menu size={22} /></button>
            <div className="hidden lg:block text-sm text-[#8A9BC0] capitalize">{view === "course" ? "Kurs" : view === "teacher" ? "Lehrer-Bereich" : view}</div>
            <div className="flex items-center gap-3">
              {me.role === "student" && (
                <>
                  <span className="hidden sm:flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full bg-[#141D35] border border-[#1E2D4A]"><Star size={14} className="text-[#F7C948]" /><span className="font-semibold text-[#F7C948]">{me.xp.toLocaleString("de-DE")}</span></span>
                  <span className="flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full bg-[#141D35] border border-[#1E2D4A]"><Flame size={14} className="text-[#F59E0B]" /><span className="font-semibold">{me.streak}</span></span>
                </>
              )}
              <button onClick={() => navigate("profile")} className="text-2xl w-10 h-10 rounded-full bg-[#141D35] border border-[#1E2D4A] hover:border-[#2A3F6F] flex items-center justify-center transition-colors">{me.avatar}</button>
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
  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-3xl font-bold">Meine Kurse</h1>
        <p className="text-[#8A9BC0] mt-1">Wähle eine Sprache und leg los.</p>
      </div>
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {COURSES.map((c) => <CourseCard key={c.id} course={c} user={me} onOpen={openCourse} />)}
      </div>
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
function Profile({ ctx }) {
  const { me, pushToast } = ctx;
  const isStudent = me.role === "student";
  const lvl = isStudent ? getLevelInfo(me.xp) : null;
  const copy = (txt) => { try { navigator.clipboard.writeText(txt); } catch (e) {} pushToast("success", "In Zwischenablage kopiert!"); };
  return (
    <div className="space-y-6 max-w-3xl">
      <Card className="p-6">
        <div className="flex flex-col sm:flex-row items-center gap-5">
          <div className="text-6xl w-24 h-24 rounded-2xl bg-[#0A0E1A] border border-[#1E2D4A] flex items-center justify-center">{me.avatar}</div>
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
  const { selectedLesson, navigate, openCourse, me, addXP, showXP, completeLesson, pushToast } = ctx;
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
      setTimeout(() => { showXP(lesson.xpReward); }, 200);
      pushToast("success", `Lektion abgeschlossen! +${lesson.xpReward} XP 🎉`);
    } else {
      pushToast("info", "Lektion bereits abgeschlossen.");
    }
    openCourse(lesson._course.id);
  };

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
                <div className="rounded-lg overflow-hidden border border-[#1E2D4A] bg-[#0A0E1A]" style={{ borderLeft: "3px solid #4F8EF7" }}>
                  <div className="flex items-center justify-between px-3 py-1.5 bg-[#0F1629] border-b border-[#1E2D4A]"><span className="font-code text-[11px] text-[#4A5A7A] uppercase">{lesson._course.id}</span><Code2 size={13} className="text-[#4A5A7A]" /></div>
                  <textarea value={answers[task.id] || ""} onChange={(e) => setAns(e.target.value)} spellCheck={false} rows={7}
                    className="w-full bg-transparent p-3 font-code text-[13px] text-[#C9D6F0] resize-y leading-relaxed" placeholder="// Dein Code …" />
                </div>
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
            {result && task.aiCheck && <AIFeedback result={result} />}

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








