#!/usr/bin/env python3
"""Prueft, ob Konfiguration, Resourcepacks und Plugin-Code zusammenpassen.

Findet die Fehler, die der Compiler nicht sieht: ein Artefakt ohne Textur, eine
custom-model-data doppelt vergeben, ein Verweis in der config.yml auf ein Item,
das es nicht mehr gibt, oder ein Geyser-Mapping, das nicht neu gebaut wurde.

Aufruf:  python3 tools/validate.py
Braucht: PyYAML
Exit-Code 1, wenn etwas nicht stimmt - taugt also fuer die CI.
"""

import json
import os
import sys

try:
    import yaml
except ImportError:  # pragma: no cover
    sys.exit("PyYAML fehlt. Installieren mit:  pip install pyyaml")

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESOURCES = os.path.join(REPO, "plugin", "src", "main", "resources")
JAVA_TEXTURES = os.path.join(REPO, "resourcepack", "java", "assets", "lemonpvp", "textures", "item")
BEDROCK_TEXTURES = os.path.join(REPO, "resourcepack", "bedrock", "textures", "items")
GEYSER_MAPPING = os.path.join(REPO, "geyser", "custom_mappings", "helden3.json")

# Platzhalter, die das Plugin im Scoreboard fuellt.
HUD_PLACEHOLDERS = {
    "%player%", "%hearts%", "%link%", "%kills%", "%deaths%", "%natural%",
    "%status%", "%alive%", "%participants%", "%online%",
}

problems = []
notes = []


def fail(message):
    problems.append(message)


def load_yaml(name):
    with open(os.path.join(RESOURCES, name), encoding="utf-8") as handle:
        return yaml.safe_load(handle) or {}


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


def check_hearts(config):
    hearts = config.get("hearts") or {}
    start = hearts.get("start", 3)
    maximum = hearts.get("max", 6)
    total = start + (1 if hearts.get("link-heart", True) else 0)

    if start < 1:
        fail("config.yml: hearts.start muss mindestens 1 sein")
    if maximum < total:
        fail("config.yml: hearts.max (%s) liegt unter den Startherzen (%s)" % (maximum, total))
    if not hearts.get("pvp-only", True):
        notes.append("hearts.pvp-only ist aus - dann kosten auch Sturz und Lava ein Herz "
                     "(im Original nicht so)")

    game = config.get("game") or {}
    if game.get("on-elimination", "SPECTATOR") not in ("SPECTATOR", "KICK", "NOTHING"):
        fail("config.yml: game.on-elimination muss SPECTATOR, KICK oder NOTHING sein")


def check_special_items(config, items):
    heart_item = (config.get("special-items") or {}).get("heart")
    if heart_item and heart_item not in items:
        fail("config.yml: special-items.heart zeigt auf das unbekannte Artefakt '%s'" % heart_item)
    if not heart_item:
        notes.append("special-items.heart ist leer - Herz-Items sind damit abgeschaltet")


def check_hud(config):
    for line in (config.get("hud") or {}).get("lines", []) or []:
        for token in line.split("%")[1::2]:
            placeholder = "%" + token + "%"
            if placeholder not in HUD_PLACEHOLDERS:
                fail("config.yml: hud.lines nutzt den unbekannten Platzhalter %s" % placeholder)


def check_geyser(items):
    if not os.path.exists(GEYSER_MAPPING):
        fail("Geyser-Mapping fehlt (tools/build_packs.py laufen lassen)")
        return
    with open(GEYSER_MAPPING, encoding="utf-8") as handle:
        mapping = json.load(handle)

    mapped = {entry["name"] for entries in mapping.get("items", {}).values() for entry in entries}
    expected = {item_id for item_id, entry in items.items()
                if entry.get("model", "generated") != "none"}
    for missing in sorted(expected - mapped):
        fail("Geyser-Mapping: '%s' fehlt (tools/build_packs.py laufen lassen)" % missing)
    for extra in sorted(mapped - expected):
        fail("Geyser-Mapping: '%s' steht nicht mehr in items.yml" % extra)


def check_messages(messages):
    """Ein paar Schluessel muss es geben, sonst stehen im Chat Fehlermeldungen."""
    required = [
        "hearts.own", "hearts.lost", "hearts.safe-death",
        "link.assigned-owner", "link.assigned-partner", "link.chain-loss", "link.none",
        "dummy.spawned", "dummy.killed", "dummy.rescued",
        "game.eliminated-broadcast", "game.winner", "game.status-alive",
        "combat.death-pvp", "combat.death-natural",
    ]
    for path in required:
        node = messages
        for part in path.split("."):
            node = node.get(part) if isinstance(node, dict) else None
            if node is None:
                fail("messages.yml: Schluessel '%s' fehlt" % path)
                break


def main():
    items = load_yaml("items.yml").get("items") or {}
    config = load_yaml("config.yml")
    messages = load_yaml("messages.yml")

    check_items(items)
    check_hearts(config)
    check_special_items(config, items)
    check_hud(config)
    check_geyser(items)
    check_messages(messages)

    hearts = config.get("hearts") or {}
    print("Artefakte: %d | Startherzen: %d%s"
          % (len(items),
             hearts.get("start", 3) + (1 if hearts.get("link-heart", True) else 0),
             " (inkl. Link-Herz)" if hearts.get("link-heart", True) else ""))

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
