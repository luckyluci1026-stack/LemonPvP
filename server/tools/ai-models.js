/**
 * Zeigt, welche Modelle dein Google-Schlüssel wirklich benutzen darf.
 *
 *     npm run ai:models
 *
 * Das ist bei Problemen die einzige verlässliche Auskunft. Was hier nicht
 * steht, gibt es für diesen Zugang nicht — unabhängig davon, was auf einer
 * Webseite, in einem Forum oder von einem Chatbot behauptet wird. Und die
 * Schreibweise zählt: `gemma-4-31b-it` ist etwas anderes als `gemma-4-31b`.
 *
 * Weder Server noch Datenbank nötig.
 */
process.env.DATABASE_URL ||= "sqlite::memory:";
process.env.SESSION_SECRET ||= "nur-fuer-die-modell-liste";

const { config } = await import("../src/config.js");
const { listGoogleModels } = await import("../src/ai.js");

const filter = (process.argv[2] || "").toLowerCase();

if (!config.ai.geminiKeys.length) {
  console.error("Kein Google-Schlüssel hinterlegt.");
  console.error("Trage GEMINI_API_KEYS in server/.env ein — Schlüssel gibt es unter");
  console.error("https://aistudio.google.com/app/apikey");
  process.exit(1);
}

const key = config.ai.geminiKeys[0];
console.log(`Schlüssel: ${key.slice(0, 6)}…${key.slice(-4)}`);
console.log(`Eingestellt ist: ${config.ai.geminiModel}\n`);

let modelle;
try {
  modelle = await listGoogleModels(key);
} catch (e) {
  console.error(`Abruf fehlgeschlagen: ${e.message}`);
  if (e.status === 400 || e.status === 401 || e.status === 403) {
    console.error("\nDer Schlüssel wurde nicht akzeptiert. Vollständig kopiert?");
    console.error("Ein Google-API-Schlüssel beginnt mit „AIza\" und ist rund 39 Zeichen lang.");
  }
  process.exit(1);
}

const gezeigt = filter ? modelle.filter((m) => m.id.toLowerCase().includes(filter)) : modelle;
const breite = Math.max(20, ...gezeigt.map((m) => m.id.length));

console.log(`${"Modell-ID".padEnd(breite)}  Kontext   Ausgabe`);
console.log("─".repeat(breite + 20));
for (const m of gezeigt) {
  const marke = m.id === config.ai.geminiModel ? " ← eingestellt" : "";
  console.log(`${m.id.padEnd(breite)}  ${String(m.input).padStart(7)}  ${String(m.output).padStart(7)}${marke}`);
}

console.log(`\n${gezeigt.length} von ${modelle.length} Modellen${filter ? ` (Filter „${filter}")` : ""}`);

const gemma = modelle.filter((m) => /gemma/i.test(m.id));
if (gemma.length) {
  console.log(`\nGemma-Modelle für diesen Zugang: ${gemma.map((m) => m.id).join(", ")}`);
} else {
  console.log("\nDieser Zugang kennt kein Gemma-Modell.");
}

const passt = modelle.some((m) => m.id === config.ai.geminiModel);
if (!passt) {
  console.log(`\nACHTUNG: „${config.ai.geminiModel}" steht nicht in der Liste.`);
  const nah = modelle
    .map((m) => m.id)
    .filter((id) => id.split("-")[0] === config.ai.geminiModel.split("-")[0]);
  if (nah.length) console.log(`Gemeint sein könnte: ${nah.slice(0, 5).join(", ")}`);
  console.log("Trage die richtige Schreibweise als GEMINI_MODEL in server/.env ein.");
  process.exit(1);
}
console.log(`\n„${config.ai.geminiModel}" ist verfügbar. Weiter mit: npm run ai:test -- gemini`);
