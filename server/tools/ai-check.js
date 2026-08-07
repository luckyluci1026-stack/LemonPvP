/**
 * Lokaler Test der KI-Anbindung.
 *
 *   npm run ai:test              — prüft den eingestellten Anbieter
 *   npm run ai:test -- gemini    — prüft gezielt einen Anbieter
 *   npm run ai:test -- alle      — prüft jeden, für den etwas hinterlegt ist
 *
 * Der Aufruf geht durch dieselbe Kette wie im Betrieb: derselbe Systemprompt,
 * dieselbe Auswertung der Antwort. Wenn es hier funktioniert, funktioniert es
 * auch in der Lektion — und wenn nicht, steht hier, woran es liegt.
 *
 * Es wird KEIN Server und KEINE Datenbank gebraucht.
 */
/* Dieser Test braucht weder Datenbank noch Sitzungen. Damit er ohne jede
   Vorbereitung läuft, werden die beiden Pflichtwerte hier belegt, falls sie
   fehlen — an die echte .env rührt das nicht. */
process.env.DATABASE_URL ||= "sqlite::memory:";
process.env.SESSION_SECRET ||= "nur-fuer-den-ki-test";

const { config } = await import("../src/config.js");
const { generate, providerReady, parseVerdict, VERIFY_SYSTEM_PROMPT } = await import("../src/ai.js");

const ALLE = ["gemini", "anthropic", "openrouter", "ollama"];

/* Zwei Proben: eine richtige und eine, die nur so aussieht. Ein Modell, das
   beide gleich bewertet, taugt für die Prüfung nicht — genau das ist der
   Fehler, den wir vorher fangen wollen. */
const PROBEN = [
  {
    name: "richtige Lösung",
    aufgabe: "Schreibe eine Funktion `verdopple`, die eine Zahl entgegennimmt und das Doppelte zurückgibt.",
    antwort: "def verdopple(zahl):\n    return zahl * 2",
    erwartet: true,
  },
  {
    name: "lose Wörter statt Code",
    aufgabe: "Schreibe eine Funktion `verdopple`, die eine Zahl entgegennimmt und das Doppelte zurückgibt.",
    antwort: "def\nverdopple\nreturn",
    erwartet: false,
  },
];

function maskiere(key) {
  const s = String(key);
  return s.length <= 10 ? "***" : `${s.slice(0, 6)}…${s.slice(-4)}`;
}

function modellVon(provider) {
  if (provider === "gemini") return config.ai.geminiModel;
  if (provider === "anthropic") return config.ai.anthropicModel;
  if (provider === "openrouter") return config.ai.openrouterModel;
  if (provider === "ollama") return `${config.ai.ollamaModel} @ ${config.ai.ollamaUrl}`;
  return "—";
}

function keysVon(provider) {
  if (provider === "gemini") return config.ai.geminiKeys;
  if (provider === "anthropic") return config.ai.anthropicKeys;
  if (provider === "openrouter") return config.ai.openrouterKeys;
  return [];
}

/** Übersetzt die üblichen Fehler in einen Satz, der weiterhilft. */
function rat(provider, fehler) {
  const status = fehler?.status;
  const text = String(fehler?.message || fehler);
  if (status === 400) {
    // Google antwortet auf beides mit 400: auf einen unbrauchbaren Schlüssel
    // und auf ein Modell, das es nicht gibt. Deshalb beide Möglichkeiten.
    return "Die Anfrage wurde abgelehnt (400). Zwei häufige Ursachen:\n"
      + "    (a) Der Schlüssel ist unvollständig oder ungültig — neu kopieren von aistudio.google.com/app/apikey.\n"
      + `    (b) Das Modell "${modellVon(provider)}" gibt es unter diesem Namen nicht — Schreibweise auf ai.google.dev prüfen.`;
  }
  if (status === 401 || status === 403) {
    return "Der Schlüssel wurde nicht akzeptiert (401/403). Ist er vollständig kopiert und für diese\n"
      + "    API freigeschaltet? Google-Keys gibt es unter aistudio.google.com/app/apikey.";
  }
  if (status === 404) {
    return `Das Modell "${modellVon(provider)}" gibt es unter diesem Namen nicht (404).\n`
      + "    Schreibweise nachschlagen: bei Google unter ai.google.dev, bei OpenRouter unter openrouter.ai/models.";
  }
  if (status === 429) {
    return "Kontingent erschöpft (429). Kurz warten — im Betrieb wechselt die Rotation automatisch\n"
      + "    auf den nächsten Schlüssel bzw. den Ersatzanbieter.";
  }
  if (/fetch failed|ECONNREFUSED|ENOTFOUND/i.test(text)) {
    return provider === "ollama"
      ? `Keine Verbindung zu ${config.ai.ollamaUrl}. Läuft \`ollama serve\`?`
      : "Kein Netzwerkzugang zum Anbieter. Firewall oder Proxy?";
  }
  if (/timeout|aborted/i.test(text)) {
    return `Zeitüberschreitung nach ${config.ai.requestTimeoutMs} ms. Bei großen Modellen auf schwacher\n`
      + "    Hardware normal — AI_TIMEOUT_MS erhöhen oder ein kleineres Modell nehmen.";
  }
  return "Unerwarteter Fehler. Die vollständige Meldung steht oben.";
}

async function pruefe(provider) {
  console.log(`\n${"─".repeat(64)}`);
  console.log(`Anbieter:  ${provider}`);
  console.log(`Modell:    ${modellVon(provider)}`);
  const keys = keysVon(provider);
  if (provider !== "ollama") {
    console.log(`Schlüssel: ${keys.length ? keys.map(maskiere).join(", ") : "— keiner hinterlegt —"}`);
  }

  if (!providerReady(provider)) {
    console.log("\n  ÜBERSPRUNGEN — nicht einsatzbereit.");
    if (provider !== "ollama" && !keys.length) {
      console.log(`  Trage ${provider.toUpperCase()}_API_KEYS in server/.env ein.`);
    } else if (provider === "openrouter") {
      console.log("  OPENROUTER_MODEL fehlt.");
    }
    return null;
  }

  let alleRichtig = true;
  for (const probe of PROBEN) {
    const prompt = [
      "Aufgabentyp: code_write",
      "Sprache: Python",
      `Aufgabenstellung: ${probe.aufgabe}`,
      "Erwartete Bausteine: def, verdopple, return",
      `Vorbewertung der lokalen Analyse: ${probe.erwartet ? 90 : 0}/100, ${probe.erwartet ? "bestanden" : "nicht bestanden"}`,
      "",
      "--- Eingereichte Antwort ---",
      probe.antwort,
    ].join("\n");

    const start = Date.now();
    let ergebnis;
    try {
      ergebnis = await generate({ system: VERIFY_SYSTEM_PROMPT, user: prompt, maxTokens: 320, provider });
    } catch (e) {
      console.log(`\n  ${probe.name}: FEHLGESCHLAGEN nach ${Date.now() - start} ms`);
      console.log(`    ${e.message}`);
      console.log(`    ${rat(provider, e)}`);
      return false;
    }
    const dauer = Date.now() - start;
    const urteil = parseVerdict(ergebnis.text);

    console.log(`\n  ${probe.name}  (${dauer} ms)`);
    if (!urteil) {
      console.log("    ANTWORT NICHT VERWERTBAR — es kam kein gültiges JSON zurück.");
      console.log(`    Rohtext: ${String(ergebnis.text).slice(0, 300).replace(/\n/g, " ")}`);
      console.log("    Im Betrieb zählt dann die lokale Bewertung. Ein anderes Modell hilft meist.");
      alleRichtig = false;
      continue;
    }
    const passt = urteil.correct === probe.erwartet;
    console.log(`    Urteil:   ${urteil.correct ? "bestanden" : "nicht bestanden"} (${urteil.score}/100)  ${passt ? "wie erwartet" : "ABWEICHUNG"}`);
    console.log(`    Feedback: ${urteil.feedback.slice(0, 120)}`);
    if (!passt) alleRichtig = false;
  }
  return alleRichtig;
}

async function main() {
  const wunsch = (process.argv[2] || "").toLowerCase();
  const liste = wunsch === "alle" || wunsch === "all"
    ? ALLE
    : wunsch
      ? [wunsch]
      : [config.ai.verify.primaryProvider, config.ai.verify.fallbackProvider].filter((v, i, a) => a.indexOf(v) === i);

  if (wunsch && wunsch !== "alle" && wunsch !== "all" && !ALLE.includes(wunsch)) {
    console.error(`Unbekannter Anbieter "${wunsch}". Möglich: ${ALLE.join(", ")}, alle`);
    process.exit(1);
  }

  console.log("KI-Prüfung — lokaler Test");
  console.log(`Hauptanbieter: ${config.ai.verify.primaryProvider}   Ersatz: ${config.ai.verify.fallbackProvider}`);
  console.log(`Kontingent: ${config.ai.verify.primaryPerDay}/Nutzer/Tag beim Hauptanbieter,`
    + ` ${config.ai.verify.maxPerDay} insgesamt, ${config.ai.verify.globalPerDay} über alle Nutzer`);

  const ergebnisse = [];
  for (const provider of liste) ergebnisse.push([provider, await pruefe(provider)]);

  console.log(`\n${"─".repeat(64)}`);
  const bereit = ergebnisse.filter(([, r]) => r === true);
  const kaputt = ergebnisse.filter(([, r]) => r === false);
  const offen = ergebnisse.filter(([, r]) => r === null);

  if (bereit.length) console.log(`Einsatzbereit: ${bereit.map(([p]) => p).join(", ")}`);
  if (kaputt.length) console.log(`Mit Problemen: ${kaputt.map(([p]) => p).join(", ")}`);
  if (offen.length) console.log(`Nicht eingerichtet: ${offen.map(([p]) => p).join(", ")}`);
  if (!bereit.length) {
    console.log("\nOhne einsatzbereiten Anbieter bewertet die Plattform ausschließlich lokal.");
    console.log("Das ist kein Fehler — offene Aufgaben bekommen dann nur keine Zweitmeinung.");
  }
  process.exit(kaputt.length ? 1 : 0);
}

main().catch((e) => {
  console.error("Abbruch:", e.message);
  process.exit(1);
});
