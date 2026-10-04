#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Macht aus deinem Java-Resourcepack ein Bedrock-Pack - inklusive der
3D-Modelle. Bedrock-Spieler sehen die eigenen Items dann genauso wie
Java-Spieler.

Was dabei herauskommt:

  dist/bedrock/SMP-Bedrock-Pack.mcpack   -> Geyser/packs/
  dist/bedrock/smp_items.json            -> Geyser/custom_mappings/

Aufruf:

  python3 java2bedrock.py <Java-Pack.zip> [SMPContent-config.yml ...]

Wie es funktioniert
-------------------
Geyser kann eigenen Items ein Bedrock-Aussehen geben. Dafuer braucht es
zwei Dinge: eine Zuordnung (welches Java-Item mit welchem Modell wird zu
welchem Bedrock-Item) und ein Bedrock-Pack mit Textur und Geometrie.

Ein Java-Modell besteht aus Quadern ("elements") mit from/to und UV je
Seite. Bedrock kennt dieselbe Idee, benutzt aber ein anderes
Koordinatensystem: die X-Achse ist gespiegelt und der Nullpunkt liegt in
der Mitte. Genau das rechnet convert_geometry() um.

Modelle mit "parent" (also flache Standard-Items) brauchen keine
Geometrie - dort reicht die Textur, Bedrock zeichnet sie wie gewohnt flach.
"""

import json
import os
import shutil
import sys
import uuid
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
OUT = HERE / "dist" / "bedrock"
WORK = HERE / "build" / "bedrock"

NAMESPACE = "smp"
PACK_NAME = "SMP 3D-Pack"
PACK_DESCRIPTION = "Eigene Items mit 3D-Modellen fuer Bedrock"


# ----------------------------------------------------------------------
#  Java-Modell -> Bedrock-Geometrie
# ----------------------------------------------------------------------

# Java-Seitenname -> Bedrock-Seitenname. Norden und Sueden tauschen,
# weil die X-Achse gespiegelt wird.
FACE_MAP = {
    "north": "north",
    "south": "south",
    "east": "west",
    "west": "east",
    "up": "up",
    "down": "down",
}


def convert_geometry(model, identifier, tex_w, tex_h):
    """Baut aus einem Java-Modell eine Bedrock-Geometrie."""
    cubes = []
    for element in model.get("elements", []):
        f = element.get("from", [0, 0, 0])
        t = element.get("to", [16, 16, 16])
        size = [t[0] - f[0], t[1] - f[1], t[2] - f[2]]

        # Bedrock spiegelt X und hat den Nullpunkt in der Mitte des Blocks.
        origin = [-t[0] + 8, f[1], f[2] - 8]

        cube = {"origin": [round(v, 4) for v in origin],
                "size": [round(v, 4) for v in size]}

        rotation = element.get("rotation")
        if rotation and rotation.get("angle"):
            axis = rotation.get("axis", "y")
            angle = float(rotation["angle"])
            pivot = rotation.get("origin", [8, 8, 8])
            # Auch hier X spiegeln; um X/Y dreht sich das Vorzeichen mit
            cube["pivot"] = [round(-pivot[0] + 8, 4), round(pivot[1], 4),
                             round(pivot[2] - 8, 4)]
            cube["rotation"] = {
                "x": [angle if axis == "x" else 0, 0, 0],
                "y": [0, -angle if axis == "y" else 0, 0],
                "z": [0, 0, -angle if axis == "z" else 0],
            }[axis] if axis in ("x", "y", "z") else [0, 0, 0]
            if axis == "x":
                cube["rotation"] = [angle, 0, 0]
            elif axis == "y":
                cube["rotation"] = [0, -angle, 0]
            else:
                cube["rotation"] = [0, 0, -angle]

        faces = element.get("faces", {})
        uv = {}
        for java_face, data in faces.items():
            target = FACE_MAP.get(java_face)
            if target is None or "uv" not in data:
                continue
            u1, v1, u2, v2 = data["uv"]
            # Java-UV liegt in 0..16 und bezieht sich auf die ganze Textur
            sx, sy = tex_w / 16.0, tex_h / 16.0
            u, v = min(u1, u2) * sx, min(v1, v2) * sy
            du, dv = abs(u2 - u1) * sx, abs(v2 - v1) * sy
            uv[target] = {"uv": [round(u, 4), round(v, 4)],
                          "uv_size": [round(du, 4), round(dv, 4)]}
        if uv:
            cube["uv"] = uv
        cubes.append(cube)

    return {
        "format_version": "1.16.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{identifier}",
                "texture_width": tex_w,
                "texture_height": tex_h,
                "visible_bounds_width": 3,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 0.75, 0],
            },
            "bones": [{"name": "root", "pivot": [0, 0, 0], "cubes": cubes}],
        }],
    }


def attachable(name, identifier, texture_path):
    """Damit Bedrock das Modell in der Hand zeigt."""
    return {
        "format_version": "1.10.0",
        "minecraft:attachable": {
            "description": {
                "identifier": f"geyser_custom:{name}",
                "materials": {"default": "entity_alphatest",
                              "enchanted": "entity_alphatest_glint"},
                "textures": {"default": texture_path,
                             "enchanted": "textures/misc/enchanted_item_glint"},
                "geometry": {"default": f"geometry.{identifier}"},
                "render_controllers": [
                    {"controllers.render.item_default": "query.owner_identifier"}
                ],
                "scripts": {"pre_animation": [
                    "v.main_hand = c.item_slot == 'main_hand';",
                    "v.off_hand = c.item_slot == 'off_hand';",
                    "v.head = c.item_slot == 'head';",
                ]},
                "animations": {
                    "thirdperson_main_hand": f"animation.{identifier}.thirdperson_main_hand",
                    "thirdperson_off_hand": f"animation.{identifier}.thirdperson_off_hand",
                    "firstperson_main_hand": f"animation.{identifier}.firstperson_main_hand",
                    "firstperson_off_hand": f"animation.{identifier}.firstperson",
                },
                "animate": [
                    {"thirdperson_main_hand": "v.main_hand && !c.is_first_person"},
                    {"thirdperson_off_hand": "v.off_hand && !c.is_first_person"},
                    {"firstperson_main_hand": "v.main_hand && c.is_first_person"},
                    {"firstperson_off_hand": "v.off_hand && c.is_first_person"},
                ],
            }
        },
    }


def animations(identifier, display):
    """Uebernimmt die Display-Einstellungen aus Blockbench."""

    def part(entry, default_scale=1.0):
        d = display.get(entry, {}) if display else {}
        rot = d.get("rotation", [0, 0, 0])
        tr = d.get("translation", [0, 0, 0])
        sc = d.get("scale", [default_scale] * 3)
        return {
            # Bedrock rechnet in anderen Einheiten und spiegelt X
            "rotation": [round(rot[0], 3), round(-rot[1], 3), round(-rot[2], 3)],
            "position": [round(-tr[0], 3), round(tr[1], 3), round(tr[2], 3)],
            "scale": [round(s, 4) for s in sc],
        }

    return {
        "format_version": "1.8.0",
        "animations": {
            f"animation.{identifier}.thirdperson_main_hand": {
                "loop": True,
                "bones": {"root": part("thirdperson_righthand")},
            },
            f"animation.{identifier}.thirdperson_off_hand": {
                "loop": True,
                "bones": {"root": part("thirdperson_lefthand")},
            },
            f"animation.{identifier}.firstperson_main_hand": {
                "loop": True,
                "bones": {"root": part("firstperson_righthand")},
            },
            f"animation.{identifier}.firstperson": {
                "loop": True,
                "bones": {"root": part("firstperson_lefthand")},
            },
        },
    }


# ----------------------------------------------------------------------
#  Pack bauen
# ----------------------------------------------------------------------

def png_size(path):
    """Breite und Hoehe einer PNG lesen - ohne Zusatzbibliothek."""
    with open(path, "rb") as fh:
        head = fh.read(26)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        return 16, 16
    return int.from_bytes(head[16:20], "big"), int.from_bytes(head[20:24], "big")


def build(java_pack, configs):
    if WORK.exists():
        shutil.rmtree(WORK)
    src = WORK / "java"
    src.mkdir(parents=True)
    with zipfile.ZipFile(java_pack) as zf:
        zf.extractall(src)

    assets = src / "assets" / NAMESPACE
    items_dir = assets / "items"
    models_dir = assets / "models" / "item"
    textures_dir = assets / "textures" / "item"
    if not items_dir.is_dir():
        print(f"FEHLER: {items_dir} fehlt - ist das ein SMP-Java-Pack?")
        return 1

    # Welches Java-Basisitem und welche model-data gehoert zu welcher Id?
    entries = {}
    for cfg in configs:
        entries.update(read_config(cfg))

    root = WORK / "pack"
    (root / "textures" / "items").mkdir(parents=True)
    (root / "models" / "entity").mkdir(parents=True)
    (root / "attachables").mkdir(parents=True)
    (root / "animations").mkdir(parents=True)

    item_texture = {"resource_pack_name": "smp", "texture_name": "atlas.items",
                    "texture_data": {}}
    mappings = {}
    flat, solid, skipped = 0, 0, []

    for item_file in sorted(items_dir.glob("*.json")):
        name = item_file.stem
        entry = entries.get(name)
        if entry is None:
            skipped.append(name)
            continue

        texture = textures_dir / f"{name}.png"
        if not texture.exists():
            skipped.append(name)
            continue

        # Textur uebernehmen
        shutil.copy(texture, root / "textures" / "items" / f"{name}.png")
        item_texture["texture_data"][name] = {"textures": f"textures/items/{name}"}

        identifier = f"{NAMESPACE}_{name}"
        model_file = models_dir / f"{name}.json"
        model = json.loads(model_file.read_text(encoding="utf-8")) \
            if model_file.exists() else {}

        has_3d = bool(model.get("elements"))
        if has_3d:
            tex_w, tex_h = png_size(texture)
            size = model.get("texture_size")
            if size:
                tex_w, tex_h = int(size[0]), int(size[1])
            geo = convert_geometry(model, identifier, tex_w, tex_h)
            write(root / "models" / "entity" / f"{identifier}.geo.json", geo)
            write(root / "attachables" / f"{identifier}.attachable.json",
                  attachable(name, identifier, f"textures/items/{name}"))
            write(root / "animations" / f"{identifier}.animation.json",
                  animations(identifier, model.get("display", {})))
            solid += 1
        else:
            flat += 1

        mappings.setdefault(entry["material"], []).append({
            "name": name,
            "custom_model_data": entry["model_data"],
            "icon": name,
            "allow_offhand": True,
            "display_handheld": has_3d,
        })

    write(root / "textures" / "item_texture.json", item_texture)
    write(root / "manifest.json", {
        "format_version": 2,
        "header": {"name": PACK_NAME, "description": PACK_DESCRIPTION,
                   "uuid": str(uuid.uuid5(uuid.NAMESPACE_DNS, "smp.bedrock.header")),
                   "version": [1, 0, 0], "min_engine_version": [1, 21, 0]},
        "modules": [{"type": "resources",
                     "uuid": str(uuid.uuid5(uuid.NAMESPACE_DNS, "smp.bedrock.module")),
                     "version": [1, 0, 0]}],
    })

    OUT.mkdir(parents=True, exist_ok=True)
    mcpack = OUT / "SMP-Bedrock-Pack.mcpack"
    zip_dir(root, mcpack)
    write(OUT / "smp_items.json", {"format_version": "2", "items": mappings})

    total = sum(len(v) for v in mappings.values())
    print(f"Bedrock-Pack: {mcpack}")
    print(f"  {solid} Items mit echtem 3D-Modell")
    print(f"  {flat} flache Items (nur Textur)")
    print(f"  {total} Eintraege in der Geyser-Zuordnung")
    if skipped:
        print(f"  {len(skipped)} uebersprungen (keine Textur oder nicht in der Config): "
              + ", ".join(sorted(skipped)[:8]) + (" ..." if len(skipped) > 8 else ""))
    print(f"\nGeyser-Zuordnung: {OUT / 'smp_items.json'}")
    shutil.rmtree(WORK)
    return 0


def read_config(path):
    """Liest Id -> Basisitem und model-data aus einer SMPContent-Config."""
    import re
    out = {}
    current, material, model_data = None, None, None
    for line in Path(path).read_text(encoding="utf-8").split("\n"):
        m = re.match(r"^  ([a-z0-9_]+):\s*$", line)
        if m:
            if current and material and model_data:
                out[current] = {"material": f"minecraft:{material.lower()}",
                                "model_data": model_data}
            current, material, model_data = m.group(1), None, None
            continue
        m = re.match(r"^    material:\s*(\S+)", line)
        if m:
            material = m.group(1).strip('"\'')
        m = re.match(r"^    model-data:\s*(\d+)", line)
        if m:
            model_data = int(m.group(1))
    if current and material and model_data:
        out[current] = {"material": f"minecraft:{material.lower()}",
                        "model_data": model_data}
    return out


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def zip_dir(folder, target):
    if target.exists():
        target.unlink()
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as zf:
        for file in sorted(folder.rglob("*")):
            if file.is_file():
                zf.write(file, file.relative_to(folder).as_posix())


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    default_cfg = (HERE.parent / "bettersmp-suite" / "smpcontent"
                   / "src" / "main" / "resources" / "config.yml")
    cfgs = sys.argv[2:] or ([str(default_cfg)] if default_cfg.exists() else [])
    sys.exit(build(sys.argv[1], cfgs))
