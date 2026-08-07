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
    if (key === "KEY_A") return { ok: false, status: 429, json: async () => ({}) };
    return {
      ok: true, status: 200,
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

test("im Code steht keine geratene Modell-ID als Vorgabe", () => {
  // Der Katalog von OpenRouter ändert sich laufend. Eine fest verdrahtete ID
  // wäre irgendwann falsch und würde still auf die lokale Analyse zurückfallen.
  assert.equal(process.env.OPENROUTER_MODEL || "", config.ai.openrouterModel,
    "openrouterModel darf ausschließlich aus der Umgebung kommen");
});
