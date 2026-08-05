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
