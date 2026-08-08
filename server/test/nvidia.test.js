/**
 * NVIDIA NIM als weiteres Glied der Kette.
 *
 * Über hundert Modelle hinter einer OpenAI-kompatiblen Adresse. Die
 * Besonderheit gegenüber Groq oder Cerebras: Es gibt kein sinnvolles
 * Standardmodell. Der Katalog ändert sich laufend, und eine erfundene ID
 * liefert eine 404 — die sieht aus wie ein Schlüsselproblem und ist keins.
 * Deshalb gilt NVIDIA erst als bereit, wenn eine Modell-ID eingetragen ist.
 *
 * Braucht weder Server noch Datenbank.
 */
process.env.DATABASE_URL ||= "sqlite::memory:";
process.env.SESSION_SECRET ||= "nur-fuer-den-nvidia-test";

import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

const { ketteLesen } = await import("../src/ai.js");

const AI_JS = fs.readFileSync(new URL("../src/ai.js", import.meta.url), "utf8");
const CONFIG_JS = fs.readFileSync(new URL("../src/config.js", import.meta.url), "utf8");

/** Lädt config und ai.js in einem eigenen Prozess mit gesetzten Werten. */
async function mitUmgebung(werte) {
  const { execFileSync } = await import("node:child_process");
  const code = `
    const { providerReady, modelOf } = await import("${new URL("../src/ai.js", import.meta.url).pathname}");
    process.stdout.write(JSON.stringify({
      bereit: providerReady("nvidia"),
      modell: modelOf("nvidia"),
    }));
  `;
  const raus = execFileSync(process.execPath, ["--input-type=module", "-e", code], {
    env: {
      ...process.env,
      DATABASE_URL: "sqlite::memory:",
      SESSION_SECRET: "test",
      NVIDIA_API_KEYS: "",
      NVIDIA_MODEL: "",
      ...werte,
    },
    encoding: "utf8",
    stdio: ["ignore", "pipe", "ignore"],
  });
  return JSON.parse(raus);
}

test("die Adresse ist nachgeschlagen, nicht geraten", () => {
  assert.match(AI_JS, /https:\/\/integrate\.api\.nvidia\.com\/v1\/chat\/completions/);
});

test("es gibt bewusst kein Standardmodell", () => {
  /* Eine Vorgabe im Code wäre hier schädlich: Sie wäre bald veraltet, und
     der Fehler sähe aus wie ein Schlüsselproblem. */
  assert.match(CONFIG_JS, /nvidiaModel: process\.env\.NVIDIA_MODEL \|\| ""/);
});

test("ohne Schlüssel ist NVIDIA nicht bereit", async () => {
  const r = await mitUmgebung({});
  assert.equal(r.bereit, false);
});

test("mit Schlüssel, aber ohne Modell ebenfalls nicht", async () => {
  const r = await mitUmgebung({ NVIDIA_API_KEYS: "nvapi-test" });
  assert.equal(r.bereit, false, "ohne Modell-ID würde jede Anfrage mit 404 enden");
});

test("mit Schlüssel und Modell ist es bereit", async () => {
  const r = await mitUmgebung({ NVIDIA_API_KEYS: "nvapi-test", NVIDIA_MODEL: "z-ai/glm-5.2" });
  assert.equal(r.bereit, true);
  assert.equal(r.modell, "z-ai/glm-5.2");
});

test("NVIDIA lässt sich in die Kette schreiben", () => {
  assert.deepEqual(ketteLesen("groq,nvidia:z-ai/glm-5.2,cerebras"), [
    { provider: "groq", model: "" },
    // Der Doppelpunkt trennt nur einmal — der Modellname darf selbst einen
    // Schrägstrich enthalten.
    { provider: "nvidia", model: "z-ai/glm-5.2" },
    { provider: "cerebras", model: "" },
  ]);
});

test("NVIDIA steht überall, wo Anbieter aufgezählt werden", () => {
  assert.match(AI_JS, /if \(provider === "nvidia"\) return config\.ai\.nvidiaKeys;/);
  assert.match(AI_JS, /if \(provider === "nvidia"\) return config\.ai\.nvidiaModel;/);
  assert.match(AI_JS, /nvidia: build\("nvidia"\)/, "im Admin-Bereich fehlt der Schlüsselzustand");

  const werkzeug = fs.readFileSync(new URL("../tools/ai-check.js", import.meta.url), "utf8");
  assert.match(werkzeug, /"nvidia"/, "npm run ai:test kennt NVIDIA nicht");
  assert.match(werkzeug, /NVIDIA_MODEL fehlt/, "der Hinweis auf die fehlende ID fehlt");
});

test("die Beispielkonfiguration warnt vor geratenen IDs", () => {
  for (const name of [".env.example", ".env.local.example"]) {
    const text = fs.readFileSync(new URL(`../${name}`, import.meta.url), "utf8");
    assert.match(text, /NVIDIA_API_KEYS=/, `${name}: Schlüssel fehlt`);
    assert.match(text, /NVIDIA_MODEL=/, `${name}: Modell fehlt`);
    assert.match(text, /build\.nvidia\.com/, `${name}: die Quelle der IDs fehlt`);
  }
});

test("kein Schlüssel liegt versehentlich im Projekt", () => {
  for (const name of ["src/config.js", "src/ai.js", ".env.example", ".env.local.example"]) {
    const text = fs.readFileSync(new URL(`../${name}`, import.meta.url), "utf8");
    assert.ok(!/nvapi-[A-Za-z0-9_-]{10,}/.test(text), `${name} enthält einen Schlüssel`);
  }
});
