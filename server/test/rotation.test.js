import { test } from "node:test";
import assert from "node:assert/strict";

// Die Rotation wird gegen einen simulierten Anbieter geprüft: Der erste Key
// läuft ins Rate-Limit, der zweite antwortet erfolgreich.
const calls = [];
const realFetch = globalThis.fetch;

test("Key-Rotation überspringt limitierte Keys", async () => {
  process.env.AI_PROVIDER = "gemini";
  process.env.GEMINI_API_KEYS = "KEY_A,KEY_B";
  const { generate, poolStatus } = await import("../src/ai.js?rot=1");

  globalThis.fetch = async (url) => {
    const key = new URL(url).searchParams.get("key");
    calls.push(key);
    // Wie eine echte Response: Der Code liest bei Fehlern den Text mit aus,
    // um die Meldung des Anbieters weiterzureichen.
    if (key === "KEY_A") {
      return { ok: false, status: 429, text: async () => "", json: async () => ({}) };
    }
    return {
      ok: true, status: 200,
      text: async () => "",
      json: async () => ({ candidates: [{ content: { parts: [{ text: "OK von B" }] } }] }),
    };
  };

  const result = await generate({ system: "s", user: "u", maxTokens: 10 });
  assert.equal(result.text, "OK von B");
  assert.ok(calls.includes("KEY_A"), "Erster Key wurde versucht");
  assert.ok(calls.includes("KEY_B"), "Auf zweiten Key gewechselt");

  const status = poolStatus();
  const a = status.gemini.find((k) => k.label.includes("KEY_A"));
  assert.equal(a.cooling, true, "Limitierter Key pausiert");
  assert.ok(a.secondsLeft > 0);

  // Nächster Aufruf geht direkt an den funktionierenden Key
  calls.length = 0;
  await generate({ system: "s", user: "u", maxTokens: 10 });
  assert.deepEqual(calls, ["KEY_B"], "Pausierter Key wird übersprungen");

  globalThis.fetch = realFetch;
});

/* ------------------------------ OpenRouter -------------------------------
   Ein Schlüssel allein reicht dort nicht: Ohne Modell-ID weiß der Dienst
   nicht, wen er fragen soll, und antwortet mit 404. Genau das war der
   wahrscheinlichste Stolperstein, weil Modell-IDs aus zweiter Hand oft nicht
   stimmen — deshalb ist das hier festgehalten. */
const { config } = await import("../src/config.js");
const { providerReady } = await import("../src/ai.js");

test("OpenRouter gilt erst mit Schlüssel UND Modell als bereit", () => {
  const keys = config.ai.openrouterKeys;
  const model = config.ai.openrouterModel;
  try {
    config.ai.openrouterKeys = [];
    config.ai.openrouterModel = "";
    assert.equal(providerReady("openrouter"), false, "ohne alles");

    config.ai.openrouterKeys = ["sk-test"];
    assert.equal(providerReady("openrouter"), false, "Schlüssel ohne Modell reicht nicht");

    config.ai.openrouterModel = "   ";
    assert.equal(providerReady("openrouter"), false, "Leerzeichen sind kein Modell");

    config.ai.openrouterModel = "anbieter/modell";
    assert.equal(providerReady("openrouter"), true, "mit beidem bereit");
  } finally {
    config.ai.openrouterKeys = keys;
    config.ai.openrouterModel = model;
  }
});

test("die Modell-ID hat die Form anbieter/modell", () => {
  // Ein Tippfehler hier führt zu einem 404, und die Prüfung fiele still auf
  // die lokale Analyse zurück. Die Form lässt sich wenigstens festhalten.
  assert.match(config.ai.openrouterModel, /^[\w.-]+\/[\w.:-]+$/,
    "erwartet wird etwas wie google/gemma-4-31b-it:free");
});

test("OPENROUTER_MODEL aus der Umgebung hat Vorrang", async () => {
  const vorher = process.env.OPENROUTER_MODEL;
  process.env.OPENROUTER_MODEL = "anbieter/eigenes-modell";
  const frisch = await import("../src/config.js?openrouter=1");
  assert.equal(frisch.config.ai.openrouterModel, "anbieter/eigenes-modell");
  if (vorher === undefined) delete process.env.OPENROUTER_MODEL;
  else process.env.OPENROUTER_MODEL = vorher;
});

/* -------------------- Systemanweisung: zweiter Anlauf ---------------------
   Nicht jedes Modell hinter der Google-Schnittstelle nimmt eine getrennte
   Systemanweisung an. Wo das scheitert, wird sie in den Text eingebettet —
   sonst stünde der Nutzer vor einem 400 ohne erkennbaren Grund. */
test("Google: abgelehnte Systemanweisung führt zum zweiten Anlauf", async () => {
  process.env.AI_PROVIDER = "gemini";
  process.env.GEMINI_API_KEYS = "KEY_SYS";
  const { generate } = await import("../src/ai.js?sys=1");

  const koerper = [];
  const echtesFetch = globalThis.fetch;
  globalThis.fetch = async (url, opts) => {
    const daten = JSON.parse(opts.body);
    koerper.push(daten);
    if (daten.systemInstruction) {
      return {
        ok: false, status: 400,
        text: async () => JSON.stringify({ error: { message: "Developer instruction is not enabled for models/x" } }),
        json: async () => ({}),
      };
    }
    return {
      ok: true, status: 200,
      text: async () => "",
      json: async () => ({ candidates: [{ content: { parts: [{ text: "OK ohne System" }] } }] }),
    };
  };

  const ergebnis = await generate({ system: "REGELN", user: "FRAGE", maxTokens: 10 });
  assert.equal(ergebnis.text, "OK ohne System");
  assert.equal(koerper.length, 2, "genau zwei Versuche");
  assert.ok(koerper[0].systemInstruction, "erster Versuch mit Systemanweisung");
  assert.ok(!koerper[1].systemInstruction, "zweiter Versuch ohne");
  assert.match(koerper[1].contents[0].parts[0].text, /REGELN[\s\S]*FRAGE/,
    "die Regeln stehen dann vorne im Text");

  globalThis.fetch = echtesFetch;
});

test("Google: ein anderer 400er wird nicht stillschweigend wiederholt", async () => {
  process.env.GEMINI_API_KEYS = "KEY_400";
  const { generate } = await import("../src/ai.js?vier=1");
  let versuche = 0;
  const echtesFetch = globalThis.fetch;
  globalThis.fetch = async () => {
    versuche++;
    return {
      ok: false, status: 400,
      text: async () => JSON.stringify({ error: { message: "API key not valid" } }),
      json: async () => ({}),
    };
  };
  await assert.rejects(
    () => generate({ system: "s", user: "u", maxTokens: 10 }),
    /API key not valid/,
    "die Meldung des Anbieters wird durchgereicht"
  );
  assert.equal(versuche, 1, "kein zweiter Anlauf bei anderer Ursache");
  globalThis.fetch = echtesFetch;
});

test("Groq gilt erst mit Schlüssel als bereit", () => {
  const keys = config.ai.groqKeys;
  try {
    config.ai.groqKeys = [];
    assert.equal(providerReady("groq"), false);
    config.ai.groqKeys = ["gsk_test"];
    assert.equal(providerReady("groq"), true);
  } finally {
    config.ai.groqKeys = keys;
  }
});

test("jede Rolle hat einen Anbieter", () => {
  for (const [name, rolle] of Object.entries(config.ai.roles)) {
    assert.ok(rolle.provider, `${name} ohne Anbieter`);
  }
  assert.ok(config.ai.verify.primaryProvider);
  assert.ok(config.ai.verify.fallbackProvider);
});
