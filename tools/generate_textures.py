#!/usr/bin/env python3
"""Erzeugt die 16x16-Item-Texturen fuer beide Resourcepacks.

Keine Abhaengigkeiten - die PNGs werden direkt geschrieben (zlib gehoert zur
Standardbibliothek). Die Pixelbilder stehen als ASCII-Art in ART, damit man sie
im Diff lesen und bearbeiten kann.

Aufruf:  python3 tools/generate_textures.py
Ziel:    resourcepack/java/assets/lemonpvp/textures/item/<id>.png
         resourcepack/bedrock/textures/items/<id>.png
"""

import os
import struct
import zlib

SIZE = 16

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA_OUT = os.path.join(REPO, "resourcepack", "java", "assets", "lemonpvp", "textures", "item")
BEDROCK_OUT = os.path.join(REPO, "resourcepack", "bedrock", "textures", "items")
BEDROCK_ICON = os.path.join(REPO, "resourcepack", "bedrock", "pack_icon.png")
JAVA_ICON = os.path.join(REPO, "resourcepack", "java", "pack.png")

# ---------------------------------------------------------------- Pixelbilder
# '.' ist immer transparent, alle anderen Zeichen kommen aus der Palette.

HEART = """
................
...aaa....aaa...
..ahhha..ahhha..
.ahhhhhaahhhhha.
.ahhhhhhhhhhhha.
.ahhhhwhhhhhhha.
.ahhhwwhhhhhhha.
..ahhwhhhhhhha..
...ahhhhhhhha...
....ahhhhhha....
.....ahhhha.....
......ahha......
.......aa.......
................
................
................
"""

# --------------------------------------------------------------- Item-Palette
# a = dunkle Kontur, h = Herzflaeche, w = Glanzlicht

ITEMS = {
    "herz": (HEART, {
        "a": (0x7A, 0x10, 0x18),
        "h": (0xE8, 0x3A, 0x46),
        "w": (0xFF, 0xB0, 0xB8),
    }),
}


def parse(art, palette):
    """ASCII-Art -> 16x16 RGBA-Pixelmatrix."""
    rows = art.strip("\n").split("\n")
    grid = []
    for y in range(SIZE):
        line = rows[y] if y < len(rows) else ""
        row = []
        for x in range(SIZE):
            char = line[x] if x < len(line) else "."
            if char == "." or char not in palette:
                row.append((0, 0, 0, 0))
            else:
                red, green, blue = palette[char]
                row.append((red, green, blue, 255))
        grid.append(row)
    return grid


def write_png(path, grid):
    height = len(grid)
    width = len(grid[0])

    raw = bytearray()
    for row in grid:
        raw.append(0)  # Filtertyp "None"
        for red, green, blue, alpha in row:
            raw += bytes((red, green, blue, alpha))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png)


def scale(grid, factor):
    scaled = []
    for row in grid:
        big_row = []
        for pixel in row:
            big_row.extend([pixel] * factor)
        for _ in range(factor):
            scaled.append(list(big_row))
    return scaled


def on_background(grid, background):
    """Legt ein Bild auf einen deckenden Hintergrund - fuer die Pack-Icons."""
    out = []
    for row in grid:
        new_row = []
        for red, green, blue, alpha in row:
            if alpha == 0:
                new_row.append(background + (255,))
            else:
                new_row.append((red, green, blue, 255))
        out.append(new_row)
    return out


def main():
    written = 0
    for item_id, (art, palette) in ITEMS.items():
        grid = parse(art, palette)
        write_png(os.path.join(JAVA_OUT, item_id + ".png"), grid)
        write_png(os.path.join(BEDROCK_OUT, item_id + ".png"), grid)
        written += 2

    icon = on_background(scale(parse(HEART, ITEMS["herz"][1]), 8), (0x0C, 0x0C, 0x0C))
    write_png(BEDROCK_ICON, icon)
    write_png(JAVA_ICON, icon)
    written += 2

    print("PNG-Dateien geschrieben: %d" % written)


if __name__ == "__main__":
    main()
