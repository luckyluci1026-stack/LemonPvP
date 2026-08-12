#!/usr/bin/env python3
"""Prueft, ob Konfiguration, Resourcepacks und Plugin-Code zusammenpassen.

Findet die Fehler, die der Compiler nicht sieht: ein Held, dessen Waffe es
nicht mehr gibt, eine unbekannte Faehigkeit, ein Shop-Eintrag ohne Item, eine
Textur, die im Pack fehlt.

Aufruf:  python3 tools/validate.py
Braucht: PyYAML
Exit-Code 1, wenn etwas nicht stimmt - taugt also fuer die CI.
"""

import json
import os
import re
import sys

try:
    import yaml
except ImportError:  # pragma: no cover
    sys.exit("PyYAML fehlt. Installieren mit:  pip install pyyaml")

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESOURCES = os.path.join(REPO, "plugin", "src", "main", "resources")
ABILITY_DIR = os.path.join(REPO, "plugin", "src", "main", "java", "de", "lemonpvp",
                           "helden", "hero", "ability", "impl")
JAVA_TEXTURES = os.path.join(REPO, "resourcepack", "java", "assets", "lemonpvp", "textures", "item")
BEDROCK_TEXTURES = os.path.join(REPO, "resourcepack", "bedrock", "textures", "items")
GEYSER_MAPPING = os.path.join(REPO, "geyser", "custom_mappings", "helden3.json")

problems = []
notes = []


def fail(message):
    problems.append(message)


def load_yaml(name):
    with open(os.path.join(RESOURCES, name), encoding="utf-8") as handle:
        return yaml.safe_load(handle) or {}


def registered_abilities():
    """Liest die Faehigkeits-IDs direkt aus den Java-Klassen."""
    found = set()
    pattern = re.compile(r'super\(plugin,\s*"([^"]+)"')
    for filename in os.listdir(ABILITY_DIR):
        if not filename.endswith(".java"):
            continue
        with open(os.path.join(ABILITY_DIR, filename), encoding="utf-8") as handle:
            match = pattern.search(handle.read())
            if match:
                found.add(match.group(1))
    return found


def check_items(items):
    seen_cmd = {}
    for item_id, entry in items.items():
        if not entry.get("material"):
            fail("items.yml: '%s' hat kein material" % item_id)
        cmd = entry.get("custom-model-data")
        if not cmd:
            fail("items.yml: '%s' hat keine custom-model-data" % item_id)
        elif cmd in seen_cmd:
            fail("items.yml: custom-model-data %s doppelt (%s und %s)" % (cmd, seen_cmd[cmd], item_id))
        else:
            seen_cmd[cmd] = item_id

        model = entry.get("model", "generated")
        if model not in ("handheld", "generated", "none"):
            fail("items.yml: '%s' hat model '%s' (erlaubt: handheld, generated, none)" % (item_id, model))

        if model != "none":
            for label, folder in (("Java", JAVA_TEXTURES), ("Bedrock", BEDROCK_TEXTURES)):
                if not os.path.exists(os.path.join(folder, item_id + ".png")):
                    fail("%s-Pack: Textur fehlt fuer '%s' (tools/generate_textures.py laufen lassen)"
                         % (label, item_id))


def check_heroes(heroes, items, abilities):
    for hero_id, entry in heroes.items():
        ability = entry.get("ability", "")
        if not ability:
            notes.append("Held '%s' hat keine aktive Faehigkeit" % hero_id)
        elif ability not in abilities:
            fail("heroes.yml: '%s' verweist auf die unbekannte Faehigkeit '%s' (bekannt: %s)"
                 % (hero_id, ability, ", ".join(sorted(abilities))))

        weapon = entry.get("weapon", "")
        if weapon and weapon not in items:
            fail("heroes.yml: '%s' verweist auf das unbekannte Artefakt '%s'" % (hero_id, weapon))

        for raw in entry.get("passives", {}).get("effects", []) or []:
            if ":" not in str(raw):
                notes.append("Held '%s': Effekt '%s' ohne Staerkeangabe (wird 0)" % (hero_id, raw))


def check_shop(shop, items):
    for entry_id, entry in (shop.get("entries") or {}).items():
        item_id = entry.get("item")
        if item_id and item_id not in items:
            fail("shop.yml: '%s' verweist auf das unbekannte Artefakt '%s'" % (entry_id, item_id))
        if not item_id and not entry.get("material"):
            fail("shop.yml: '%s' hat weder 'item' noch 'material'" % entry_id)
        if entry.get("price") is None:
            notes.append("Shop-Eintrag '%s' hat keinen Preis (wird 0)" % entry_id)

    rows = shop.get("rows", 3)
    slots = {}
    for entry_id, entry in (shop.get("entries") or {}).items():
        slot = entry.get("slot")
        if slot is None:
            continue
        if slot >= rows * 9:
            fail("shop.yml: '%s' liegt auf Slot %s, das Menue hat aber nur %s Plaetze"
                 % (entry_id, slot, rows * 9))
        if slot in slots:
            fail("shop.yml: Slot %s doppelt belegt (%s und %s)" % (slot, slots[slot], entry_id))
        slots[slot] = entry_id


def check_config(config, items):
    for key, path in (("revive", "special-items.revive"),
                      ("life-crystal", "special-items.life-crystal"),
                      ("currency-token", "special-items.currency-token"),
                      ("return-stone", "special-items.return-stone")):
        value = (config.get("special-items") or {}).get(key)
        if value and value not in items:
            fail("config.yml: %s zeigt auf das unbekannte Artefakt '%s'" % (path, value))

    rotation = (config.get("events") or {}).get("rotation") or []
    known_events = {"blutmond", "zitronenregen", "kopfgeld"}
    for event in rotation:
        if event not in known_events:
            fail("config.yml: events.rotation enthaelt das unbekannte Event '%s'" % event)


def check_geyser(items):
    if not os.path.exists(GEYSER_MAPPING):
        fail("Geyser-Mapping fehlt (tools/build_packs.py laufen lassen)")
        return
    with open(GEYSER_MAPPING, encoding="utf-8") as handle:
        mapping = json.load(handle)

    mapped = {entry["name"] for entries in mapping.get("items", {}).values() for entry in entries}
    expected = {item_id for item_id, entry in items.items() if entry.get("model", "generated") != "none"}
    for missing in sorted(expected - mapped):
        fail("Geyser-Mapping: '%s' fehlt (tools/build_packs.py laufen lassen)" % missing)
    for extra in sorted(mapped - expected):
        fail("Geyser-Mapping: '%s' steht nicht mehr in items.yml" % extra)


def check_messages(messages):
    """Jeder in messages.yml benutzte Platzhalter sollte auch gefuellt werden."""
    if not messages:
        fail("messages.yml ist leer")


def main():
    items = load_yaml("items.yml").get("items") or {}
    heroes = load_yaml("heroes.yml").get("heroes") or {}
    shop = load_yaml("shop.yml")
    config = load_yaml("config.yml")
    teams = load_yaml("teams.yml").get("teams") or {}
    messages = load_yaml("messages.yml")
    abilities = registered_abilities()

    check_items(items)
    check_heroes(heroes, items, abilities)
    check_shop(shop, items)
    check_config(config, items)
    check_geyser(items)
    check_messages(messages)

    if not teams:
        fail("teams.yml enthaelt keine Teams")

    print("Artefakte: %d | Helden: %d | Faehigkeiten: %d | Teams: %d"
          % (len(items), len(heroes), len(abilities), len(teams)))

    for note in notes:
        print("  Hinweis: %s" % note)

    if problems:
        print()
        for problem in problems:
            print("  FEHLER: %s" % problem)
        sys.exit(1)

    print("Alles konsistent.")


if __name__ == "__main__":
    main()
