/**
 * Die Anbieterkette des Profi-Agenten.
 *
 * Kontingente sind bei jedem Anbieter anders geschnitten: Groq erlaubt viele
 * Anfragen pro Minute bei wenig Text, Cerebras umgekehrt. Läuft der erste in
 * sein Limit, soll der nächste übernehmen — aber nur bei Kontingent- und
 * Ausfallfehlern. Ein falscher Modellname muss ein Fehler bleiben, sonst
 * sucht die Kette reihum weiter und verdeckt einen Tippfehler.
 *
 * Braucht weder Server noch Datenbank.
 */
process.env.DATABASE_URL ||= "sqlite::memory:";
process.env.SESSION_SECRET ||= "nur-fuer-den-kettentest";
process.env.GROQ_API_KEYS ||= "gsk_test_eins";
process.env.CEREBRAS_API_KEYS ||= "csk_test_zwei";

import { test } from "node:test";
import assert from "node:assert/strict";

/* Dynamisch geladen, nicht als `import` oben: ESM wertet alle import-Zeilen
   VOR den Zuweisungen darüber aus. Die Schlüssel wären sonst noch nicht
   gesetzt, wenn config.js sie liest. */
const { ketteLesen, generateChain } = await import("../src/ai.js");

/** Ersetzt fetch für die Dauer eines Tests. */
async function mitAntworten(antworten, fn) {
  const echt = globalThis.fetch;
  const gerufen = [];
  let i = 0;
  globalThis.fetch = async (url, opts) => {
    const koerper = JSON.parse(opts?.body || "{}");
    gerufen.push({ url: String(url), model: koerper.model });
    const a = antworten[Math.min(i++, antworten.length - 1)];
    if (a.status >= 400) {
      return { ok: false, status: a.status, text: async () => a.text || "" };
    }
    return {
      ok: true, status: 200, text: async () => "",
      json: async () => ({ choices: [{ message: { content: a.text } }] }),
    };
  };
  try { return { ergebnis: await fn(), gerufen }; }
  finally { globalThis.fetch = echt; }
}

/* ------------------------------ Kette lesen ------------------------------ */

test("ein Eintrag ohne Modell nennt nur den Anbieter", () => {
  assert.deepEqual(ketteLesen("groq,cerebras"), [
    { provider: "groq", model: "" },
    { provider: "cerebras", model: "" },
  ]);
});

test("Anbieter und Modell werden getrennt", () => {
  assert.deepEqual(ketteLesen("groq:llama-3.1-8b-instant"), [
    { provider: "groq", model: "llama-3.1-8b-instant" },
  ]);
});

test("Leerzeichen und leere Einträge stören nicht", () => {
  assert.deepEqual(ketteLesen(" groq , , cerebras "), [
    { provider: "groq", model: "" },
    { provider: "cerebras", model: "" },
  ]);
  assert.deepEqual(ketteLesen(""), []);
  assert.deepEqual(ketteLesen(null), []);
});

test("auch ein fertiges Array wird angenommen", () => {
  assert.deepEqual(ketteLesen(["groq", "cerebras:gpt-oss-120b"]), [
    { provider: "groq", model: "" },
    { provider: "cerebras", model: "gpt-oss-120b" },
  ]);
});

/* ---------------------------- Der Wechsel -------------------------------- */

test("der erste Anbieter antwortet — der zweite wird nicht gefragt", async () => {
  const { ergebnis, gerufen } = await mitAntworten(
    [{ status: 200, text: "Fertig." }],
    () => generateChain({ kette: "groq,cerebras", system: "s", user: "u", maxTokens: 100 })
  );
  assert.equal(ergebnis.text, "Fertig.");
  assert.equal(ergebnis.provider, "groq");
  assert.equal(gerufen.length, 1);
  assert.match(gerufen[0].url, /groq\.com/);
});

test("bei Kontingent übernimmt der nächste", async () => {
  const { ergebnis, gerufen } = await mitAntworten(
    [{ status: 429 }, { status: 200, text: "Vom zweiten." }],
    () => generateChain({ kette: "groq,cerebras", system: "s", user: "u", maxTokens: 100 })
  );
  assert.equal(ergebnis.text, "Vom zweiten.");
  assert.equal(ergebnis.provider, "cerebras");
  assert.equal(gerufen.length, 2);
  assert.match(gerufen[1].url, /cerebras\.ai/);
});

test("auch ein Ausfall führt zum Wechsel", async () => {
  for (const status of [402, 403, 500, 503]) {
    const { ergebnis } = await mitAntworten(
      [{ status }, { status: 200, text: "ok" }],
      () => generateChain({ kette: "groq,cerebras", system: "s", user: "u", maxTokens: 100 })
    );
    assert.equal(ergebnis.provider, "cerebras", `bei ${status} wurde nicht gewechselt`);
  }
});

test("ein falscher Modellname wird gemeldet, nicht übergangen", async () => {
  /* Sonst probiert die Kette reihum weiter und der Tippfehler in der
     Konfiguration fällt nie auf. */
  await assert.rejects(
    () => mitAntworten(
      [{ status: 400, text: '{"error":{"message":"model not found"}}' }],
      () => generateChain({ kette: "groq,cerebras", system: "s", user: "u", maxTokens: 100 })
    ).then((r) => r.ergebnis),
    /400/
  );
});

test("das Modell aus der Kette wird mitgeschickt", async () => {
  const { gerufen } = await mitAntworten(
    [{ status: 200, text: "ok" }],
    () => generateChain({ kette: "cerebras:gpt-oss-120b", system: "s", user: "u", maxTokens: 100 })
  );
  assert.equal(gerufen[0].model, "gpt-oss-120b");
});

test("derselbe Anbieter darf zweimal in der Kette stehen", async () => {
  // Erst das große Modell, bei Limit dasselbe Haus mit dem kleinen.
  const { ergebnis, gerufen } = await mitAntworten(
    [{ status: 429 }, { status: 200, text: "ok" }],
    () => generateChain({
      kette: "groq:llama-3.3-70b-versatile,groq:llama-3.1-8b-instant",
      system: "s", user: "u", maxTokens: 100,
    })
  );
  assert.equal(ergebnis.text, "ok");
  assert.equal(gerufen.length, 2);
  assert.equal(gerufen[0].model, "llama-3.3-70b-versatile");
  assert.equal(gerufen[1].model, "llama-3.1-8b-instant");
});

test("ohne eingerichteten Anbieter gibt es eine klare Meldung", async () => {
  await assert.rejects(
    () => generateChain({ kette: "anthropic", system: "s", user: "u", maxTokens: 100 }),
    /Kein Anbieter bereit/
  );
});

test("scheitern alle, kommt der letzte Fehler zurück", async () => {
  await assert.rejects(
    () => mitAntworten(
      [{ status: 429 }, { status: 429 }],
      () => generateChain({ kette: "groq,cerebras", system: "s", user: "u", maxTokens: 100 })
    ).then((r) => r.ergebnis),
    /429/
  );
});
