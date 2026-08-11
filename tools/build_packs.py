#!/usr/bin/env python3
"""Baut Java-Resourcepack, Bedrock-Resourcepack und Geyser-Mapping aus items.yml.

items.yml ist die einzige Quelle der Wahrheit: material, custom-model-data,
bedrock-identifier, display und model werden von hier gelesen und in alle drei
Ziele geschrieben. So koennen Plugin und Packs nicht auseinanderlaufen.

Aufruf:  python3 tools/build_packs.py
Braucht: PyYAML  (pip install pyyaml)

Die erzeugten Dateien sind eingecheckt - dieses Skript muss also nur laufen,
wenn du items.yml aenderst.
"""

import json
import os
import re
import sys

try:
    import yaml
except ImportError:  # pragma: no cover - reiner Bedienhinweis
    sys.exit("PyYAML fehlt. Installieren mit:  pip install pyyaml")

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEMS_YML = os.path.join(REPO, "plugin", "src", "main", "resources", "items.yml")

JAVA_ROOT = os.path.join(REPO, "resourcepack", "java")
BEDROCK_ROOT = os.path.join(REPO, "resourcepack", "bedrock")
GEYSER_ROOT = os.path.join(REPO, "geyser", "custom_mappings")

# Namespace unserer eigenen Java-Assets.
NAMESPACE = "lemonpvp"
# Namespace, unter dem Geyser Custom-Items auf Bedrock registriert.
BEDROCK_NAMESPACE = "geyser_custom"

# 1.21.4 = 46. supported_formats haelt das Pack auch auf aelteren/neueren
# Versionen ladbar, statt bei jedem Update eine Warnung zu werfen.
JAVA_PACK_FORMAT = 46
JAVA_PACK_FORMAT_MIN = 32
JAVA_PACK_FORMAT_MAX = 99

# Feste UUIDs - beim Aendern halten Bedrock-Clients das Pack fuer ein neues.
BEDROCK_HEADER_UUID = "6c1e2f40-9b3a-4d7e-8f21-5a0d3c7b1e94"
BEDROCK_MODULE_UUID = "b83a17c5-4e62-4a19-9d0f-2c6b8e4f7a13"
BEDROCK_VERSION = [3, 0, 0]

COLOR_CODES = re.compile(r"&(#[0-9A-Fa-f]{6}|[0-9A-Fa-fk-orK-OR])")

# Basisitems mit Sonderlocken im Vanilla-Modell. Wenn wir die stumpf
# ueberschreiben, verlieren ALLE Items dieses Typs auf dem Server ihr Aussehen -
# deshalb bauen wir das Vanilla-Verhalten hier nach.
#
# Der Bogen behaelt seine Spannanimation: die Vanilla-Overrides stehen vor
# unserem custom_model_data-Eintrag, unser Artefakt gewinnt nur fuer die eigene
# CMD.
LEGACY_VANILLA_OVERRIDES = {
    "bow": [
        {"predicate": {"pulling": 1}, "model": "minecraft:item/bow_pulling_0"},
        {"predicate": {"pulling": 1, "pull": 0.65}, "model": "minecraft:item/bow_pulling_1"},
        {"predicate": {"pulling": 1, "pull": 0.9}, "model": "minecraft:item/bow_pulling_2"},
    ],
}

# Dasselbe fuer das Item-Definitions-Format ab 1.21.4: unser range_dispatch
# faellt auf die Vanilla-Definition zurueck statt auf ein nacktes Modell.
MODERN_VANILLA_FALLBACK = {
    "bow": {
        "type": "minecraft:condition",
        "property": "minecraft:using_item",
        "on_false": {"type": "minecraft:model", "model": "minecraft:item/bow"},
        "on_true": {
            "type": "minecraft:range_dispatch",
            "property": "minecraft:use_duration",
            "scale": 0.05,
            "fallback": {"type": "minecraft:model", "model": "minecraft:item/bow_pulling_0"},
            "entries": [
                {"threshold": 0.65, "model": {"type": "minecraft:model", "model": "minecraft:item/bow_pulling_1"}},
                {"threshold": 0.9, "model": {"type": "minecraft:model", "model": "minecraft:item/bow_pulling_2"}},
            ],
        },
    },
}


def strip_colors(text):
    return COLOR_CODES.sub("", text or "").strip()


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def write_text(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)


def load_items():
    with open(ITEMS_YML, encoding="utf-8") as handle:
        raw = yaml.safe_load(handle)

    items = []
    for item_id, entry in (raw.get("items") or {}).items():
        material = str(entry["material"]).lower()
        items.append({
            "id": item_id,
            "material": material,
            "cmd": int(entry.get("custom-model-data", 0)),
            "model": entry.get("model", "generated"),
            "display": strip_colors(entry.get("display", item_id)),
            "bedrock": entry.get("bedrock-identifier", "%s:%s" % (BEDROCK_NAMESPACE, item_id)),
        })

    missing = [i["id"] for i in items if i["cmd"] <= 0]
    if missing:
        sys.exit("Diese Artefakte haben keine custom-model-data: %s" % ", ".join(missing))

    duplicates = {}
    for item in items:
        duplicates.setdefault(item["cmd"], []).append(item["id"])
    clashes = {cmd: ids for cmd, ids in duplicates.items() if len(ids) > 1}
    if clashes:
        sys.exit("Doppelte custom-model-data: %s" % clashes)

    return items


# ------------------------------------------------------------------ Java-Pack
def build_java(items):
    write_json(os.path.join(JAVA_ROOT, "pack.mcmeta"), {
        "pack": {
            "pack_format": JAVA_PACK_FORMAT,
            "supported_formats": {
                "min_inclusive": JAVA_PACK_FORMAT_MIN,
                "max_inclusive": JAVA_PACK_FORMAT_MAX,
            },
            "description": "Helden 3 - LemonPvP Projekt-Pack (Java Edition)",
        }
    })

    # Ein Modell je Artefakt.
    for item in items:
        parent = "minecraft:item/handheld" if item["model"] == "handheld" else "minecraft:item/generated"
        write_json(
            os.path.join(JAVA_ROOT, "assets", NAMESPACE, "models", "item", item["id"] + ".json"),
            {"parent": parent, "textures": {"layer0": "%s:item/%s" % (NAMESPACE, item["id"])}})

    # Artefakte nach Basisitem gruppieren - mehrere koennen dasselbe Item nutzen.
    by_material = {}
    for item in items:
        if item["model"] == "none":
            continue
        by_material.setdefault(item["material"], []).append(item)

    for material, entries in by_material.items():
        entries.sort(key=lambda entry: entry["cmd"])

        custom_overrides = [
            {
                "predicate": {"custom_model_data": entry["cmd"]},
                "model": "%s:item/%s" % (NAMESPACE, entry["id"]),
            }
            for entry in entries
        ]

        # a) Klassische Overrides - Minecraft 1.14 bis 1.21.3.
        write_json(
            os.path.join(JAVA_ROOT, "assets", "minecraft", "models", "item", material + ".json"),
            {
                "parent": "minecraft:item/handheld" if any(e["model"] == "handheld" for e in entries)
                          else "minecraft:item/generated",
                "textures": {"layer0": "minecraft:item/" + material},
                "overrides": LEGACY_VANILLA_OVERRIDES.get(material, []) + custom_overrides,
            })

        # b) Item-Definitionen - ab Minecraft 1.21.4.
        fallback = MODERN_VANILLA_FALLBACK.get(
            material, {"type": "minecraft:model", "model": "minecraft:item/" + material})
        write_json(
            os.path.join(JAVA_ROOT, "assets", "minecraft", "items", material + ".json"),
            {
                "model": {
                    "type": "minecraft:range_dispatch",
                    "property": "minecraft:custom_model_data",
                    "fallback": fallback,
                    "entries": [
                        {
                            "threshold": entry["cmd"],
                            "model": {
                                "type": "minecraft:model",
                                "model": "%s:item/%s" % (NAMESPACE, entry["id"]),
                            },
                        }
                        for entry in entries
                    ],
                }
            })

    return len(by_material)


# --------------------------------------------------------------- Bedrock-Pack
def build_bedrock(items):
    write_json(os.path.join(BEDROCK_ROOT, "manifest.json"), {
        "format_version": 2,
        "header": {
            "name": "Helden 3 - LemonPvP",
            "description": "Artefakte des Helden-3-Projekts fuer Bedrock-Spieler (via Geyser).",
            "uuid": BEDROCK_HEADER_UUID,
            "version": BEDROCK_VERSION,
            "min_engine_version": [1, 20, 0],
        },
        "modules": [
            {
                "type": "resources",
                "uuid": BEDROCK_MODULE_UUID,
                "version": BEDROCK_VERSION,
            }
        ],
    })

    custom = [item for item in items if item["model"] != "none"]

    write_json(os.path.join(BEDROCK_ROOT, "textures", "item_texture.json"), {
        "resource_pack_name": "helden3",
        "texture_name": "atlas.items",
        "texture_data": {
            item["id"]: {"textures": "textures/items/" + item["id"]}
            for item in custom
        },
    })

    write_json(os.path.join(BEDROCK_ROOT, "texts", "languages.json"), ["de_DE", "en_US"])

    # Bedrock akzeptiert beide Schluesselformen - wir schreiben sicherheitshalber
    # beide, damit der Name auch nach Geyser-Updates steht.
    for language in ("de_DE", "en_US"):
        lines = ["## Helden 3 - Artefaktnamen", ""]
        for item in custom:
            identifier = item["bedrock"]
            lines.append("item.%s=%s" % (identifier, item["display"]))
            lines.append("item.%s.name=%s" % (identifier, item["display"]))
        write_text(os.path.join(BEDROCK_ROOT, "texts", language + ".lang"), "\n".join(lines) + "\n")

    return len(custom)


# ------------------------------------------------------------- Geyser-Mapping
def build_geyser(items):
    mapping = {}
    for item in items:
        if item["model"] == "none":
            # Kein Custom-Modell: Bedrock soll das echte Vanilla-Item behalten,
            # sonst verliert z. B. ein Schild seine Blockfunktion.
            continue
        mapping.setdefault("minecraft:" + item["material"], []).append({
            "name": item["id"],
            "display_name": item["display"],
            "icon": item["id"],
            "custom_model_data": item["cmd"],
            "allow_offhand": True,
            "display_handheld": item["model"] == "handheld",
            "texture_size": 16,
        })

    write_json(os.path.join(GEYSER_ROOT, "helden3.json"), {
        "format_version": "1",
        "items": mapping,
    })
    return len(mapping)


def main():
    items = load_items()
    materials = build_java(items)
    bedrock = build_bedrock(items)
    geyser = build_geyser(items)

    print("Artefakte:            %d" % len(items))
    print("Java-Basisitems:      %d" % materials)
    print("Bedrock-Texturen:     %d" % bedrock)
    print("Geyser-Basisitems:    %d" % geyser)
    print()
    print("Nicht vergessen: python3 tools/generate_textures.py fuer die PNGs.")


if __name__ == "__main__":
    main()
