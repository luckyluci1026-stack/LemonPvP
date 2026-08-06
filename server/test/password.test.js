import test from "node:test";
import assert from "node:assert/strict";
import { checkPassword, PASSWORD_MIN_LENGTH } from "../src/password.js";

test("zu kurze Passwörter werden abgelehnt", () => {
  const r = checkPassword("Ab1!x");
  assert.equal(r.ok, false);
  assert.ok(r.problems.some((p) => p.includes(String(PASSWORD_MIN_LENGTH))));
});

test("nur Kleinbuchstaben reichen nicht", () => {
  assert.equal(checkPassword("abcdefghijklm").ok, false);
});

test("drei Zeichenarten genügen", () => {
  assert.equal(checkPassword("Blauwal7Sofa").ok, true);
});

test("bekannte Passwörter werden abgelehnt", () => {
  assert.equal(checkPassword("passwort123").ok, false);
  assert.equal(checkPassword("Passwort123").ok, false);
});

test("Tastaturmuster werden abgelehnt", () => {
  assert.equal(checkPassword("Qwertz12345!").ok, false);
  assert.equal(checkPassword("Abc123456789").ok, false);
});

test("wiederholte Zeichen sind kein Passwort", () => {
  assert.equal(checkPassword("aaaaaaaaaaaa").ok, false);
});

test("Name und E-Mail dürfen nicht im Passwort stehen", () => {
  assert.equal(checkPassword("Mueller99!x", { name: "mueller" }).ok, false);
  assert.equal(checkPassword("Xmaxmuster9!", { email: "maxmuster@example.de" }).ok, false);
  assert.equal(checkPassword("Blauwal7Sofa", { name: "Anna", email: "anna@example.de" }).ok, true);
});

test("sehr lange Passwörter werden begrenzt", () => {
  assert.equal(checkPassword("A1!" + "x".repeat(250)).ok, false);
});
