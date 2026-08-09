#!/usr/bin/env python3
"""
SMP Texture-Pack Generator
==========================

Erzeugt aus diesem einen Skript BEIDE Resource-Packs:

  build/java/     -> Java-Resourcepack (Ordner)
  build/bedrock/  -> Bedrock-Resourcepack (Ordner)
  dist/SMP-Java-Pack.zip
  dist/SMP-Bedrock-Pack.mcpack
  dist/geyser/smp_items.json   -> Geyser-Mapping fuer die Custom-Items

Alles wird prozedural gezeichnet - du kannst jede Form unten in den
SHAPE-Strings direkt bearbeiten und einfach neu generieren:

    python3 generate.py

Zeichen-Format: Jede Form ist ein Raster aus Zeilen. Ein Zeichen = ein Pixel.
'.' ist transparent, alle anderen Zeichen sind Schluessel aus der jeweiligen
Palette. So kannst du Pixel fuer Pixel malen, ohne Grafikprogramm.
"""

import json
import shutil
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).parent
BUILD = ROOT / "build"
DIST = ROOT / "dist"

NAMESPACE = "smp"
PACK_NAME = "SMP Pack"

# Java-Packformat 1.21.9-1.21.11 -> 64 (supported_formats deckt einen Bereich ab)
JAVA_PACK_FORMAT = 64
JAVA_FORMAT_RANGE = [46, 99]

# ---------------------------------------------------------------------------
#  Hilfsfunktionen zum Zeichnen
# ---------------------------------------------------------------------------


def draw(shape, palette, scale=1):
    """Baut ein RGBA-Bild aus einem Zeichen-Raster."""
    rows = [r for r in shape.strip("\n").split("\n")]
    height = len(rows)
    width = max(len(r) for r in rows)
    img = Image.new("RGBA", (width * scale, height * scale), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "." or ch == " ":
                continue
            color = palette.get(ch)
            if color is None:
                continue
            for sy in range(scale):
                for sx in range(scale):
                    px[x * scale + sx, y * scale + sy] = color
    return img


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def gradient_fill(img, start, end, mode="diagonal"):
    """Faerbt alle nicht-transparenten Pixel mit einem Farbverlauf ein.

    mode: "horizontal", "vertical" oder "diagonal".
    Der Alphakanal (und damit die Form + Kanten) bleibt erhalten.
    """
    w, h = img.size
    px = img.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if mode == "vertical":
                t = y / max(1, h - 1)
            elif mode == "horizontal":
                t = x / max(1, w - 1)
            else:
                t = (x + y) / max(1, (w - 1) + (h - 1))
            nr, ng, nb = lerp(start, end, t)
            px[x, y] = (nr, ng, nb, a)
    return img


def boost(color, factor):
    """Hellt eine Farbe auf (factor > 1) oder dunkelt sie ab (factor < 1)."""
    return tuple(max(0, min(255, round(c * factor))) for c in color[:3])


def shade(img, mask_shape, palette, scale):
    """Legt eine zweite Ebene (z.B. Outline/Glanz) ueber ein Bild."""
    overlay = draw(mask_shape, palette, scale)
    img.alpha_composite(overlay)
    return img


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


# ---------------------------------------------------------------------------
#  1) UI-Icons  (Codepoints U+E100 - U+E10F)
#     16x16 gemalt, im Atlas auf 32x32 skaliert.
# ---------------------------------------------------------------------------

ICON_PALETTE = {
    "K": hexc("1A1A22"),          # Outline dunkel
    "k": hexc("2E2E3A"),          # Outline weich
    "W": hexc("FFFFFF"),
    "w": hexc("D8DCE6"),
    "G": hexc("FFD75A"),          # Gold hell
    "g": hexc("E0A21C"),          # Gold dunkel
    "y": hexc("FFF3B0"),          # Gold Glanz
    "R": hexc("FF4D5E"),          # Rot hell
    "r": hexc("B3132A"),          # Rot dunkel
    "p": hexc("FF9AA6"),          # Rot Glanz
    "E": hexc("5CE07A"),          # Gruen hell
    "e": hexc("1E8F3C"),          # Gruen dunkel
    "B": hexc("5AC8FF"),          # Blau hell
    "b": hexc("1C6FB0"),          # Blau dunkel
    "C": hexc("7A5230"),          # Braun
    "c": hexc("4A2F17"),          # Braun dunkel
    "S": hexc("C9D1DC"),          # Silber
    "s": hexc("7C8797"),          # Silber dunkel
}

ICONS = {
    # ---- Muenze -------------------------------------------------
    "coin": """
.....KKKKKK.....
...KKggggggKK...
..KgGGGGGGGGgK..
.KgGGyyyyyyGGgK.
.KgGyyGGGGyyGgK.
KgGGyGGggGGyGGgK
KgGGyGgKKgGyGGgK
KgGGyGgKKgGyGGgK
KgGGyGgKKgGyGGgK
KgGGyGGggGGyGGgK
.KgGyyGGGGyyGgK.
.KgGGyyyyyyGGgK.
..KgGGGGGGGGgK..
...KKggggggKK...
.....KKKKKK.....
................
""",
    # ---- Herz ---------------------------------------------------
    "heart": """
................
..KKKK....KKKK..
.KrRRRK..KrRRRK.
KrRppRRKKRRppRrK
KRppRRRRRRRRppRK
KRpRRRRRRRRRRpRK
KRRRRRRRRRRRRRRK
KrRRRRRRRRRRRRrK
.KrRRRRRRRRRRrK.
..KrRRRRRRRRrK..
...KrRRRRRRrK...
....KrRRRRrK....
.....KrRRrK.....
......KrrK......
.......KK.......
................
""",
    # ---- Stern --------------------------------------------------
    "star": """
.......KK.......
......KGGK......
......KGGK......
.....KGyyGK.....
.....KGyyGK.....
KKKKKKGyyGKKKKKK
KGGGGGGyyGGGGGGK
.KGyyyyyyyyyyGK.
..KGGyyyyyyGGK..
...KGGyyyyGGK...
...KGGyyyyGGK...
..KGGyK..KyGGK..
..KGGK....KGGK..
.KGGK......KGGK.
.KKK........KKK.
................
""",
    # ---- Krone --------------------------------------------------
    "crown": """
................
..K..........K..
.KGK...KK...KGK.
.KGK..KGGK..KGK.
KGyGK.KGGK.KGyGK
KGyGKKGyyGKKGyGK
KGyGKKGyyGKKGyGK
KGyGGGGyyGGGGyGK
KGyyGGGyyGGGyyGK
KGGyyyyyyyyyyGGK
KgGGGGGGGGGGGGgK
KgGRGGGBGGGRGGgK
KgGGGGGGGGGGGGgK
KKggggggggggggKK
..KKKKKKKKKKKK..
................
""",
    # ---- Einkaufswagen ------------------------------------------
    "cart": """
................
KKK.............
KWWK............
.KWK............
.KWWKKKKKKKKKK..
.KWWWWWWWWWWWKK.
..KWGGGGGGGGWK..
..KWGGGGGGGGWK..
..KWGGGGGGGGWK..
..KWWWWWWWWWWK..
...KWWWWWWWWK...
....KKKKKKKK....
....K......K....
...KWWK..KWWK...
...KWWK..KWWK...
....KK....KK....
""",
    # ---- Haken --------------------------------------------------
    "check": """
................
..............K.
.............KEK
............KEEK
...........KEEK.
..........KEEK..
.K.......KEEK...
KEK.....KEEK....
KEEK...KEEK.....
.KEEK.KEEK......
..KEEKEEK.......
...KEEEEK.......
....KEEK........
.....KK.........
................
................
""",
    # ---- Kreuz --------------------------------------------------
    "cross": """
................
.KK........KK...
KRRK......KRRK..
KRRRK....KRRRK..
.KRRRK..KRRRK...
..KRRRKKRRRK....
...KRRRRRRK.....
....KRRRRK......
....KRRRRK......
...KRRRRRRK.....
..KRRRKKRRRK....
.KRRRK..KRRRK...
KRRRK....KRRRK..
KRRK......KRRK..
.KK........KK...
................
""",
    # ---- Warnung ------------------------------------------------
    "warning": """
.......KK.......
.......KK.......
......KGGK......
......KGGK......
.....KGyyGK.....
.....KGyKGK.....
....KGGyKGGK....
....KGGyKGGK....
...KGGGyKGGGK...
...KGGGKKGGGK...
..KGGGGGGGGGGK..
..KGGGGKKGGGGK..
.KGGGGGKKGGGGGK.
.KGGGGGGGGGGGGK.
KKKKKKKKKKKKKKKK
................
""",
    # ---- Pfeil rechts -------------------------------------------
    "arrow_right": """
................
.......KK.......
.......KWK......
.......KWWK.....
KKKKKKKKWWWK....
KWWWWWWWWWWWK...
KWWWWWWWWWWWWK..
KWWWWWWWWWWWWWK.
KWWWWWWWWWWWWK..
KWWWWWWWWWWWK...
KKKKKKKKWWWK....
.......KWWK.....
.......KWK......
.......KK.......
................
................
""",
    # ---- Pfeil links --------------------------------------------
    "arrow_left": """
................
.......KK.......
......KWK.......
.....KWWK.......
....KWWWKKKKKKKK
...KWWWWWWWWWWWK
..KWWWWWWWWWWWWK
.KWWWWWWWWWWWWWK
..KWWWWWWWWWWWWK
...KWWWWWWWWWWWK
....KWWWKKKKKKKK
.....KWWK.......
......KWK.......
.......KK.......
................
................
""",
    # ---- Edelstein ----------------------------------------------
    "gem": """
................
...KKKKKKKKKK...
..KBBBBBBBBBBK..
.KBWWBBBBBBBBBK.
KBWWBBBBBBBBBBBK
KBWBBBBBBBBBBBBK
.KBBBBBBBBBBBBK.
.KbBBBBBBBBBBbK.
..KbBBBBBBBBbK..
...KbBBBBBBbK...
....KbBBBBbK....
.....KbBBbK.....
......KbbK......
.......KK.......
................
................
""",
    # ---- Uhr ----------------------------------------------------
    "clock": """
.....KKKKKK.....
...KKSSSSSSKK...
..KSSWWWWWWSSK..
.KSSWWWWWWWWSSK.
.KSWWWWKWWWWWSK.
KSSWWWWKWWWWWSSK
KSWWWWWKWWWWWWSK
KSWWWWWKKKKWWWSK
KSWWWWWWWWWWWWSK
KSSWWWWWWWWWWSSK
.KSWWWWWWWWWWSK.
.KSSWWWWWWWWSSK.
..KSSWWWWWWSSK..
...KKSSSSSSKK...
.....KKKKKK.....
................
""",
    # ---- Schloss ------------------------------------------------
    "lock": """
................
.....KKKKKK.....
....KSSSSSSK....
...KSSKKKKSSK...
...KSKK..KKSK...
...KSK....KSK...
.KKKSK....KSKKK.
KGGGGGGGGGGGGGGK
KGyyGGGGGGGGyyGK
KGyGGGGKKGGGGyGK
KGGGGGKKKKGGGGGK
KGGGGGKKKKGGGGGK
KGGGGGGKKGGGGGGK
KGGGGGGKKGGGGGGK
KKgggggggggggKKK
..KKKKKKKKKKK...
""",
    # ---- Geldbeutel ---------------------------------------------
    "bag": """
................
......KKKK......
.....KCCCCK.....
....KCcccccK....
...KKCCCCCCKK...
..KCCCCCCCCCCK..
.KCCCCCCCCCCCCK.
KCCCGGGGGGGGCCCK
KCCGGyyyyyyGGCCK
KCCGyGGGGGGyGCCK
KCCGyGGKKGGyGCCK
KCCGyGGKKGGyGCCK
KCCGGyyyyyyGGCCK
.KCCCGGGGGGCCCK.
..KCCCCCCCCCCK..
...KKKKKKKKKK...
""",
    # ---- Schwert ------------------------------------------------
    "sword": """
.............KK.
............KSK.
...........KSWK.
..........KSWK..
.........KSWK...
........KSWK....
.......KSWK.....
..KKK.KSWK......
.KGGGKSWK.......
.KGGGSWK........
..KGKWK.........
...KCK..........
...KCK..........
..KCCK..........
..KCK...........
..KK............
""",
    # ---- Spitzhacke ---------------------------------------------
    "pickaxe": """
..KKK.....KKK...
.KSSSKKKKKSSSK..
KSSSSSSSSSSSSSK.
.KKKSSSSSSSKKK..
....KKKCKKK.....
.......KCK......
.......KCK......
......KCK.......
......KCK.......
.....KCK........
.....KCK........
....KCK.........
....KCK.........
...KCK..........
...KK...........
................
""",
}

ICON_ORDER = [
    "coin", "heart", "star", "crown", "cart", "check", "cross", "warning",
    "arrow_right", "arrow_left", "gem", "clock", "lock", "bag", "sword", "pickaxe",
]

# ---------------------------------------------------------------------------
#  2) Rang-Abzeichen mit Farbverlauf  (Codepoints U+E110 - U+E118)
#     Das ist die Bedrock-Loesung: Ein Bild traegt echte Farbverlaeufe,
#     unabhaengig davon, welche Farbcodes der Client unterstuetzt.
# ---------------------------------------------------------------------------

# Schild-Silhouette: 'F' = Flaeche (bekommt den Verlauf), 'K' = Rand,
# 'H' = Glanzlicht.
SHIELD = """
..KKKKKKKKKKKK..
.KFFFFFFFFFFFFK.
KFHHFFFFFFFFFFFK
KFHFFFFFFFFFFFFK
KFFFFFFFFFFFFFFK
KFFFFFFFFFFFFFFK
KFFFFFFFFFFFFFFK
KFFFFFFFFFFFFFFK
KFFFFFFFFFFFFFFK
.KFFFFFFFFFFFFK.
.KFFFFFFFFFFFFK.
..KFFFFFFFFFFK..
...KFFFFFFFFK...
....KFFFFFFK....
.....KFFFFK.....
......KKKK......
"""

# Sterne-Aufsatz fuer Owner-Raenge
STAR_OVERLAY = """
................
................
................
................
.......KK.......
......KWWK......
....KKKWWKKK....
....KWWWWWWK....
.....KWWWWK.....
.....KWWWWK.....
....KWWKKWWK....
....KWK..KWK....
................
................
................
................
"""

RANKS = {
    "owner1": ("#FFFB00", "#00FF00", True),
    "owner2": ("#00008B", "#00BFFF", True),
    "owner3": ("#FFB300", "#FF0000", True),
    "owner4": ("#1E90FF", "#8A2BE2", True),
    "owner5": ("#40E0D0", "#7CFC00", True),
    "admin":  ("#FF4D4D", "#B30000", False),
    "mod":    ("#00FF7F", "#00B34A", False),
    "sup":    ("#00E5FF", "#0091EA", False),
    "member": ("#C9D1DC", "#7C8797", False),
}

RANK_ORDER = list(RANKS.keys())


def build_rank_badge(start_hex, end_hex, with_star, scale):
    base = draw(SHIELD, {"F": hexc("FFFFFF"), "K": hexc("1A1A22"),
                         "H": hexc("FFFFFF")}, scale)
    # Nur die Flaeche einfaerben: Rand separat neu druebermalen.
    # Start wird leicht aufgehellt, Ende leicht abgedunkelt - so ist der
    # Verlauf auch auf der kleinen Schildflaeche deutlich sichtbar.
    gradient_fill(base, boost(hexc(start_hex), 1.25), boost(hexc(end_hex), 0.72),
                  mode="diagonal")
    outline = draw(SHIELD.replace("F", ".").replace("H", "."),
                   {"K": hexc("1A1A22")}, scale)
    base.alpha_composite(outline)
    glow = draw(SHIELD.replace("F", ".").replace("K", "."),
                {"H": hexc("FFFFFF", 130)}, scale)
    base.alpha_composite(glow)
    if with_star:
        base.alpha_composite(draw(STAR_OVERLAY,
                                  {"W": hexc("FFFFFF", 235), "K": hexc("1A1A22", 200)},
                                  scale))
    return base


# ---------------------------------------------------------------------------
#  3) Custom-Items (16x16, echte Item-Texturen)
# ---------------------------------------------------------------------------

ITEM_PALETTE = dict(ICON_PALETTE)
ITEM_PALETTE.update({
    "M": hexc("FF2D55"),   # Herzkristall hell
    "m": hexc("8E0B25"),   # Herzkristall dunkel
    "n": hexc("FF8FA8"),   # Herzkristall Glanz
    "T": hexc("48E08A"),   # Totem gruen
    "t": hexc("1B7A45"),   # Totem dunkel
    "O": hexc("F2C14E"),   # Totem gold
})

CUSTOM_ITEMS = {
    # Lifesteal-Herz
    "heart": """
................
..KKKK...KKKK...
.KnnnnK.KnnnnK..
KnnMMMnKnMMMMnK.
KnMMMMMMMMMMMnK.
KMMMMMMMMMMMMMK.
KMMMMMMMMMMMMMK.
.KMMMMMMMMMMMK..
.KmMMMMMMMMMmK..
..KmMMMMMMMmK...
...KmMMMMMmK....
....KmMMMmK.....
.....KmMmK......
......KmK.......
.......K........
................
""",
    # Wiederbelebungs-Totem
    "revive_totem": """
......KKKK......
.....KOOOOK.....
....KOTTTTOK....
...KOTTTTTTOK...
...KOTKTTKTOK...
...KOTTTTTTOK...
...KOTKTTKTOK...
...KOTTKKTTOK...
..KOOTTTTTTOOK..
.KOTTTTTTTTTTOK.
KOTTTTTTTTTTTTOK
KOTTOOOOOOOOTTOK
.KOTTTTTTTTTTOK.
..KOOTTTTTTOOK..
...KKOOOOOOKK...
.....KKKKKK.....
""",
    # Shop-Muenze
    "coin": """
.....KKKKKK.....
...KKggggggKK...
..KgGGGGGGGGgK..
.KgGyyyyyyyyGgK.
.KgGyGGGGGGyGgK.
KgGyGGyyyyGGyGgK
KgGyGyGGGGyGyGgK
KgGyGyGGGGyGyGgK
KgGyGyGGGGyGyGgK
KgGyGGyyyyGGyGgK
.KgGyGGGGGGyGgK.
.KgGyyyyyyyyGgK.
..KgGGGGGGGGgK..
...KKggggggKK...
.....KKKKKK.....
................
""",
}

# Basis-Material + Geyser-Icon je Custom-Item
ITEM_BASE = {
    "heart":        {"vanilla": "minecraft:nether_star",     "cmd": 8101},
    "revive_totem": {"vanilla": "minecraft:totem_of_undying", "cmd": 8102},
    "coin":         {"vanilla": "minecraft:sunflower",        "cmd": 8103},
}


# ---------------------------------------------------------------------------
#  Atlas bauen
# ---------------------------------------------------------------------------

CELL = 32          # Pixel pro Glyphe im Atlas
GRID = 16          # 16x16 Zellen -> 256 Glyphen (U+E100 - U+E1FF)
ATLAS_SIZE = CELL * GRID


def build_atlas():
    """Ein Atlas fuer Java UND Bedrock: identische Codepoints, identisches Bild."""
    atlas = Image.new("RGBA", (ATLAS_SIZE, ATLAS_SIZE), (0, 0, 0, 0))
    mapping = {}

    def place(index, img, name):
        col, row = index % GRID, index // GRID
        atlas.alpha_composite(img, (col * CELL, row * CELL))
        mapping[name] = 0xE100 + index

    for i, name in enumerate(ICON_ORDER):
        place(i, draw(ICONS[name], ICON_PALETTE, CELL // 16), name)

    for i, name in enumerate(RANK_ORDER):
        start, end, star = RANKS[name]
        place(16 + i, build_rank_badge(start, end, star, CELL // 16), "rank_" + name)

    return atlas, mapping


# ---------------------------------------------------------------------------
#  Java-Pack
# ---------------------------------------------------------------------------


def write_java(atlas, mapping):
    root = BUILD / "java"
    if root.exists():
        shutil.rmtree(root)
    (root / f"assets/{NAMESPACE}/textures/font").mkdir(parents=True, exist_ok=True)
    (root / f"assets/{NAMESPACE}/textures/item").mkdir(parents=True, exist_ok=True)
    (root / f"assets/{NAMESPACE}/models/item").mkdir(parents=True, exist_ok=True)
    (root / f"assets/{NAMESPACE}/items").mkdir(parents=True, exist_ok=True)
    (root / "assets/minecraft/font").mkdir(parents=True, exist_ok=True)

    atlas.save(root / f"assets/{NAMESPACE}/textures/font/icons.png")

    # pack.mcmeta
    (root / "pack.mcmeta").write_text(json.dumps({
        "pack": {
            "pack_format": JAVA_PACK_FORMAT,
            "supported_formats": JAVA_FORMAT_RANGE,
            "description": "§6SMP Pack §8- §7Icons, Rang-Abzeichen & Custom-Items",
        }
    }, indent=2), encoding="utf-8")

    # Glyphen in die Standardschrift einhaengen -> ueberall nutzbar
    rows = []
    for r in range(GRID):
        row = "".join(chr(0xE100 + r * GRID + c) for c in range(GRID))
        rows.append(row)
    # WICHTIG: Diese Datei ersetzt die Standardschrift komplett. Vanilla listet
    # hier ZWEI Referenzen - "include/space" definiert die Breite des
    # Leerzeichens, "include/default" die eigentlichen Buchstaben. Fehlt die
    # Space-Referenz, haben Leerzeichen im ganzen Spiel keine Breite mehr!
    (root / "assets/minecraft/font/default.json").write_text(json.dumps({
        "providers": [
            {"type": "reference", "id": "minecraft:include/space"},
            {"type": "reference", "id": "minecraft:include/default"},
            {
                "type": "bitmap",
                "file": f"{NAMESPACE}:font/icons.png",
                "ascent": 8,
                "height": 10,
                "chars": rows,
            },
        ]
    }, indent=2, ensure_ascii=False), encoding="utf-8")

    # Custom-Items: Textur + Modell + Item-Definition (1.21.4+ item_model)
    for name, shape in CUSTOM_ITEMS.items():
        draw(shape, ITEM_PALETTE, 1).save(
            root / f"assets/{NAMESPACE}/textures/item/{name}.png")
        (root / f"assets/{NAMESPACE}/models/item/{name}.json").write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{NAMESPACE}:item/{name}"},
        }, indent=2), encoding="utf-8")
        (root / f"assets/{NAMESPACE}/items/{name}.json").write_text(json.dumps({
            "model": {"type": "minecraft:model", "model": f"{NAMESPACE}:item/{name}"}
        }, indent=2), encoding="utf-8")

    make_pack_icon().save(root / "pack.png")
    return root


# ---------------------------------------------------------------------------
#  Bedrock-Pack
# ---------------------------------------------------------------------------

# Feste UUIDs, damit Updates das Pack ersetzen statt zu duplizieren.
BEDROCK_HEADER_UUID = "6c1f9a20-4b7c-4c6e-9d2a-51e0a1c73b01"
BEDROCK_MODULE_UUID = "6c1f9a20-4b7c-4c6e-9d2a-51e0a1c73b02"


def write_bedrock(atlas):
    root = BUILD / "bedrock"
    if root.exists():
        shutil.rmtree(root)
    (root / "font").mkdir(parents=True, exist_ok=True)
    (root / "textures/items").mkdir(parents=True, exist_ok=True)

    # Bedrock laedt Glyphen aus font/glyph_<HIGHBYTE>.png - gleiche Codepoints
    # wie im Java-Pack, also exakt dasselbe Bild.
    atlas.save(root / "font/glyph_E1.png")

    (root / "manifest.json").write_text(json.dumps({
        "format_version": 2,
        "header": {
            "name": PACK_NAME,
            "description": "Icons, Rang-Abzeichen & Custom-Items",
            "uuid": BEDROCK_HEADER_UUID,
            "version": [1, 0, 0],
            "min_engine_version": [1, 20, 0],
        },
        "modules": [{
            "type": "resources",
            "uuid": BEDROCK_MODULE_UUID,
            "version": [1, 0, 0],
        }],
    }, indent=2), encoding="utf-8")

    textures = {}
    for name, shape in CUSTOM_ITEMS.items():
        draw(shape, ITEM_PALETTE, 1).save(root / f"textures/items/{name}.png")
        textures[f"{NAMESPACE}_{name}"] = {"textures": f"textures/items/{name}"}

    (root / "textures/item_texture.json").write_text(json.dumps({
        "resource_pack_name": NAMESPACE,
        "texture_name": "atlas.items",
        "texture_data": textures,
    }, indent=2), encoding="utf-8")

    make_pack_icon().save(root / "pack_icon.png")
    return root


def write_geyser_mapping():
    out = DIST / "geyser"
    out.mkdir(parents=True, exist_ok=True)
    items = {}
    for name, info in ITEM_BASE.items():
        items.setdefault(info["vanilla"], []).append({
            "name": name,
            "display_name": name,
            "allow_offhand": True,
            "icon": f"{NAMESPACE}_{name}",
            "custom_model_data": info["cmd"],
        })
    (out / "smp_items.json").write_text(json.dumps({
        "format_version": "1",
        "items": items,
    }, indent=2), encoding="utf-8")
    return out


def make_pack_icon():
    icon = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    bg = Image.new("RGBA", (128, 128), hexc("22242E"))
    icon.alpha_composite(bg)
    coin = draw(ICONS["coin"], ICON_PALETTE, 6)
    icon.alpha_composite(coin, ((128 - coin.width) // 2, (128 - coin.height) // 2))
    return icon


# ---------------------------------------------------------------------------
#  Packen
# ---------------------------------------------------------------------------


def zip_dir(folder: Path, target: Path):
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists():
        target.unlink()
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as zf:
        for path in sorted(folder.rglob("*")):
            if path.is_file():
                zf.write(path, path.relative_to(folder).as_posix())
    return target


def write_glyph_reference(mapping):
    """Schreibt eine Liste aller Glyphen - praktisch fuer die Plugin-Configs."""
    lines = [
        "# SMP Pack - Glyphen-Uebersicht",
        "# Diese Zeichen kannst du direkt in Configs/Nachrichten einfuegen.",
        "",
    ]
    for name, cp in mapping.items():
        lines.append(f"{name:<16} U+{cp:04X}   {chr(cp)}")
    (DIST / "glyphen.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")

    (DIST / "glyphs.json").write_text(json.dumps(
        {name: f"\\u{cp:04X}" for name, cp in mapping.items()},
        indent=2, ensure_ascii=False), encoding="utf-8")


def main():
    DIST.mkdir(parents=True, exist_ok=True)
    atlas, mapping = build_atlas()

    java_dir = write_java(atlas, mapping)
    bedrock_dir = write_bedrock(atlas)
    geyser_dir = write_geyser_mapping()
    write_glyph_reference(mapping)

    zip_dir(java_dir, DIST / "SMP-Java-Pack.zip")
    zip_dir(bedrock_dir, DIST / "SMP-Bedrock-Pack.mcpack")

    print("Fertig!")
    print(f"  Java-Pack:    {DIST / 'SMP-Java-Pack.zip'}")
    print(f"  Bedrock-Pack: {DIST / 'SMP-Bedrock-Pack.mcpack'}")
    print(f"  Geyser-Map:   {geyser_dir / 'smp_items.json'}")
    print(f"  Glyphen:      {DIST / 'glyphen.txt'}")
    print(f"\n  {len(ICON_ORDER)} Icons, {len(RANK_ORDER)} Rang-Abzeichen, "
          f"{len(CUSTOM_ITEMS)} Custom-Items")


if __name__ == "__main__":
    main()
