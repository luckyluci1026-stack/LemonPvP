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
#  Prozedurale Textur-Helfer
#  Statt jeden Pixel von Hand zu malen, erzeugen diese Funktionen echte
#  Struktur: Rauschen fuer Stein, Facetten fuer Kristalle, Lichtkanten fuer
#  bearbeitete Bloecke. Lichtrichtung ist wie in Minecraft oben links.
# ---------------------------------------------------------------------------

import random


def value_noise(size, seed, cells=4, jitter=0.22):
    """Weiches Rauschen (bilinear interpoliertes Zufallsgitter + Koernung)."""
    rng = random.Random(seed)
    grid = [[rng.random() for _ in range(cells + 1)] for _ in range(cells + 1)]
    out = [[0.0] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            fx = x / size * cells
            fy = y / size * cells
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            a = grid[y0][x0] * (1 - tx) + grid[y0][x0 + 1] * tx
            b = grid[y0 + 1][x0] * (1 - tx) + grid[y0 + 1][x0 + 1] * tx
            v = a * (1 - ty) + b * ty + (rng.random() - 0.5) * jitter
            out[y][x] = min(1.0, max(0.0, v))
    return out


def from_noise(size, shades, seed, cells=4, jitter=0.22):
    """Baut eine Flaeche, indem Rauschwerte auf eine Farbtreppe abgebildet werden."""
    noise = value_noise(size, seed, cells, jitter)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    n = len(shades)
    for y in range(size):
        for x in range(size):
            px[x, y] = shades[min(n - 1, int(noise[y][x] * n))]
    return img


def bevel(img, light=70, dark=80, width=1):
    """Lichtkante oben/links, Schattenkante unten/rechts - lässt Blöcke plastisch wirken."""
    w, h = img.size
    px = img.load()
    for i in range(width):
        for x in range(i, w - i):
            px[x, i] = blend(px[x, i], (255, 255, 255), light)
            px[x, h - 1 - i] = blend(px[x, h - 1 - i], (0, 0, 0), dark)
        for y in range(i, h - i):
            px[i, y] = blend(px[i, y], (255, 255, 255), light)
            px[w - 1 - i, y] = blend(px[w - 1 - i, y], (0, 0, 0), dark)
    return img


def blend(pixel, target, amount):
    """Mischt einen Pixel zu 'amount'/255 Richtung target."""
    r, g, b, a = pixel
    t = amount / 255.0
    return (round(r + (target[0] - r) * t),
            round(g + (target[1] - g) * t),
            round(b + (target[2] - b) * t), a)


# Ein kleiner Kristall mit Glanzpunkt - wird in Erze gestreut.
GEM_BLOB = [
    "..XX..",
    ".XHHX.",
    "XHHMMX",
    "XHMMSX",
    ".XMSX.",
    "..XX..",
]


def scatter_gems(img, colors, seed, count=5):
    """Streut Kristall-Cluster mit Licht und Schatten in eine Steinflaeche."""
    outline, high, mid, shadow = colors
    rng = random.Random(seed + 7777)
    size = img.size[0]
    px = img.load()
    spots = []
    tries = 0
    while len(spots) < count and tries < 200:
        tries += 1
        cx, cy = rng.randrange(size), rng.randrange(size)
        if all(abs(cx - a) + abs(cy - b) > 5 for a, b in spots):
            spots.append((cx, cy))
    palette = {"X": outline, "H": high, "M": mid, "S": shadow}
    for cx, cy in spots:
        for dy, row in enumerate(GEM_BLOB):
            for dx, ch in enumerate(row):
                if ch == ".":
                    continue
                # Modulo -> Cluster laufen sauber ueber die Kante (nahtlos kachelbar)
                x = (cx + dx) % size
                y = (cy + dy) % size
                px[x, y] = palette[ch]
    return img


def crystal_surface(size, shades, seed):
    """Kristalline Flaeche: Rauschen plus ein paar helle Facettenkanten."""
    img = from_noise(size, shades, seed, cells=3, jitter=0.18)
    rng = random.Random(seed + 31)
    px = img.load()
    bright = shades[-1]
    for _ in range(6):
        x, y = rng.randrange(size), rng.randrange(size)
        length = rng.randint(2, 5)
        dx, dy = rng.choice([(1, 1), (1, -1)])
        for i in range(length):
            px[(x + dx * i) % size, (y + dy * i) % size] = bright
    return img


def veined(size, base_shades, vein_color, seed, veins=3):
    """Glatte Flaeche mit geschwungenen Adern - fuer Marmor."""
    img = from_noise(size, base_shades, seed, cells=3, jitter=0.10)
    rng = random.Random(seed + 99)
    px = img.load()
    for _ in range(veins):
        x = float(rng.randrange(size))
        y = float(rng.randrange(size))
        # Feste Grundrichtung + sanfte Drift -> lange, ruhige Adern statt Zickzack
        angle = rng.uniform(0.2, 1.35) * rng.choice([1, -1])
        for _ in range(rng.randint(14, 22)):
            ix, iy = int(x) % size, int(y) % size
            px[ix, iy] = vein_color
            # weicher Saum, damit die Ader nicht wie ein Kratzer wirkt
            px[(ix + 1) % size, iy] = blend(px[(ix + 1) % size, iy], vein_color[:3], 70)
            px[ix, (iy + 1) % size] = blend(px[ix, (iy + 1) % size], vein_color[:3], 45)
            angle += rng.uniform(-0.18, 0.18)
            x += 1.0
            y += angle * 0.5
    return img


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

# --- Zweite Icon-Reihe (U+E120 - U+E12F) ---
ICONS2 = {
    # Kaufen: Wagen mit Pfeil hinein
    "buy": """
................
.....KKKKKK.....
....KEEEEEEK....
...KEEEEEEEEK...
..KEEEKWWKEEEK..
..KEEEKWWKEEEK..
..KEKWWWWWWKEK..
..KEEKWWWWKEEK..
..KEEEKWWKEEEK..
..KEEEEKKEEEEK..
...KEEEEEEEEK...
...KEEEEEEEEK...
....KEEEEEEK....
.....KKKKKK.....
................
................
""",
    # Verkaufen: Muenze mit Pfeil heraus
    "sell": """
.......KK.......
......KWWK......
.....KWWWWK.....
....KWWWWWWK....
...KKKKWWKKKK...
.....KKWWKK.....
................
...KKKKKKKKKK...
..KggggggggggK..
.KgGGGGGGGGGGgK.
KgGGyyyyyyyyGGgK
KgGGGGGGGGGGGGgK
.KgGGGGGGGGGGgK.
..KggggggggggK..
...KKKKKKKKKK...
................
""",
    # Schild
    "shield": """
..KKKKKKKKKKKK..
.KBBBBBBBBBBBBK.
KBWWBBBBBBBBBBBK
KBWBBBBBBBBBBBBK
KBBBBBWWWWBBBBBK
KBBBBWWWWWWBBBBK
KBBBBBWWWWBBBBBK
KBBBBBBWWBBBBBBK
KBBBBBBWWBBBBBBK
.KBBBBBBBBBBBBK.
.KbBBBBBBBBBBbK.
..KbBBBBBBBBbK..
...KbBBBBBBbK...
....KbBBBBbK....
.....KbbbbK.....
......KKKK......
""",
    # Trank
    "potion": """
......KKKK......
......KWWK......
......KWWK......
.....KKWWKK.....
....KWWWWWWK....
...KWWWWWWWWK...
..KWWRRRRRRWWK..
.KWWRRRRRRRRWWK.
.KWRRRRRRRRRRWK.
KWRRRRppRRRRRRWK
KWRRRRppRRRRRRWK
KWRRRRRRRRRRRRWK
.KWRRRRRRRRRRWK.
.KWWRRRRRRRRWWK.
..KWWWWWWWWWWK..
...KKKKKKKKKK...
""",
    # Pokal
    "trophy": """
.KKKKKKKKKKKKKK.
.KGGGGGGGGGGGGK.
KKGyyyyyyyyyyGKK
KGKGGGGGGGGGGKGK
KGKGGGGGGGGGGKGK
KGKGGGGGGGGGGKGK
KGKKGGGGGGGGKKGK
.KKKGGGGGGGGKKK.
...KKGGGGGGKK...
.....KGGGGK.....
......KGGK......
......KGGK......
....KKGGGGKK....
...KGGGGGGGGK...
...KggggggggK...
...KKKKKKKKKK...
""",
    # Geschenk
    "gift": """
......KKKK......
.....KRRRRK.....
..KKKKRRRRKKKK..
.KRRRRRRRRRRRRK.
KRRRRRRWWRRRRRRK
KRRRRRRWWRRRRRRK
KKKKKKKWWKKKKKKK
KGGGGGGWWGGGGGGK
KGGGGGGWWGGGGGGK
.KGGGGGWWGGGGGK.
.KGGGGGWWGGGGGK.
.KGGGGGWWGGGGGK.
.KGGGGGWWGGGGGK.
.KGGGGGWWGGGGGK.
.KKKKKKKKKKKKKK.
................
""",
    # Ticket / Gutschein
    "ticket": """
................
.KKKKKKKKKKKKKK.
KGGGGGGGGGGGGGGK
KGyyyyyyyyyyyyGK
KGyKKKKKKKKKKyGK
KGyKGGGGGGGGKyGK
KKyKGGGGGGGGKyKK
.KyKGGGGGGGGKyK.
KKyKGGGGGGGGKyKK
KGyKKKKKKKKKKyGK
KGyyyyyyyyyyyyGK
KGGGGGGGGGGGGGGK
.KKKKKKKKKKKKKK.
................
................
................
""",
    # Totenkopf
    "skull": """
...KKKKKKKKKK...
..KWWWWWWWWWWK..
.KWWWWWWWWWWWWK.
KWWWWWWWWWWWWWWK
KWWKKKWWKKKWWWWK
KWKKKKKWKKKKKWWK
KWKKKKKWKKKKKWWK
KWWKKKWWKKKWWWWK
KWWWWWWWWWWWWWWK
KWWWWWKKWWWWWWWK
.KWWWWWWWWWWWWK.
..KWKWKWKWKWWK..
..KWKWKWKWKWWK..
...KKKKKKKKKK...
................
................
""",
    # Feuer
    "fire": """
.......KK.......
......KGGK......
......KGGK......
.....KGGyGK.....
.....KGyyGK.....
....KGGyyGGK....
....KGyyyyGK....
...KGGyRRyGGK...
...KGyRRRRyGK...
..KGGyRRRRyGGK..
..KGyRRRRRRyGK..
..KGyRRRRRRyGK..
...KGyRRRRyGK...
...KGGyyyyGGK...
....KKGGGGKK....
......KKKK......
""",
    # Blatt
    "leaf": """
..............K.
.............KEK
...........KEEEK
.........KEEEEEK
.......KEEEEEEEK
......KEEEEEEEEK
.....KEEEeEEEEEK
....KEEEeeEEEEK.
...KEEEeeeEEEK..
..KEEEeeeEEEK...
..KEEeeeEEEK....
.KEEeeeEEEK.....
.KEeeeEEEK......
KEeeeEEKK.......
KeeeKKK.........
KKK.............
""",
}

ICONS2_ORDER = ["buy", "sell", "shield", "potion", "trophy",
                "gift", "ticket", "skull", "fire", "leaf"]

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

CUSTOM_ITEMS.update({
    # Rubin
    "ruby": """
.......KK.......
......KnMK......
.....KnMMMK.....
....KnMMMMMK....
...KnMMMMMMMK...
..KnMMMMMMMMMK..
.KnMMMMMMMMMMMK.
KnMMMMMMMMMMMMMK
.KmMMMMMMMMMMmK.
..KmMMMMMMMMmK..
...KmMMMMMMmK...
....KmMMMMmK....
.....KmMMmK.....
......KmmK......
.......KK.......
................
""",
    # Saphir
    "sapphire": """
.......KK.......
......KWBK......
.....KWBBBK.....
....KWBBBBBK....
...KWBBBBBBBK...
..KWBBBBBBBBBK..
.KWBBBBBBBBBBBK.
KWBBBBBBBBBBBBBK
.KbBBBBBBBBBBbK.
..KbBBBBBBBBbK..
...KbBBBBBBbK...
....KbBBBBbK....
.....KbBBbK.....
......KbbK......
.......KK.......
................
""",
    # Magischer Staub
    "magic_dust": """
................
....K.....K.....
...KWK...KWK....
....K..K..K.....
......KWK.......
...K...K...K....
..KWK.....KWK...
...K..KKK..K....
.....KBBBK......
....KBWWWBK.....
...KBWWWWWBK....
...KBWWWWWBK....
....KBWWWBK.....
.....KBBBK......
......KKK.......
................
""",
    # Shop-Gutschein
    "voucher": """
................
.KKKKKKKKKKKKKK.
KWWWWWWWWWWWWWWK
KWKKKKKKKKKKKKWK
KWKGGGGGGGGGGKWK
KWKGyyyyyyyyGKWK
KWKGyKKKKKKyGKWK
KWKGyKGGGGKyGKWK
KWKGyKGGGGKyGKWK
KWKGyKKKKKKyGKWK
KWKGyyyyyyyyGKWK
KWKGGGGGGGGGGKWK
KWKKKKKKKKKKKKWK
KWWWWWWWWWWWWWWK
.KKKKKKKKKKKKKK.
................
""",
    # Schlüssel
    "key": """
....KKKK........
...KGGGGK.......
..KGGyyGGK......
..KGyKKyGK......
..KGyKKyGK......
..KGGyyGGK......
...KGGGGK.......
....KGGK........
....KGGK........
....KGGK........
....KGGKKK......
....KGGGGK......
....KGGKKK......
....KGGKK.......
....KGGGK.......
....KKKK........
""",
    # Rubin-Schwert
    "ruby_sword": """
.............KK.
............KMK.
...........KMnK.
..........KMnK..
.........KMnK...
........KMnK....
.......KMnK.....
..KKK.KMnK......
.KGGGKMnK.......
.KGGGMnK........
..KGKnK.........
...KCK..........
...KCK..........
..KCCK..........
..KCK...........
..KK............
""",
    # Rubin-Spitzhacke
    "ruby_pickaxe": """
..KKK.....KKK...
.KMMMKKKKKMMMK..
KMMMMMMMMMMMMMK.
.KKKMMMMMMMKKK..
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
})

# Basis-Material + Geyser-Icon je Custom-Item
ITEM_BASE = {
    "heart":        {"vanilla": "minecraft:nether_star",      "cmd": 8101},
    "revive_totem": {"vanilla": "minecraft:totem_of_undying", "cmd": 8102},
    "coin":         {"vanilla": "minecraft:sunflower",        "cmd": 8103},
    "ruby":         {"vanilla": "minecraft:amethyst_shard",   "cmd": 8104},
    "sapphire":     {"vanilla": "minecraft:amethyst_shard",   "cmd": 8105},
    "magic_dust":   {"vanilla": "minecraft:glowstone_dust",   "cmd": 8106},
    "voucher":      {"vanilla": "minecraft:paper",            "cmd": 8107},
    "key":          {"vanilla": "minecraft:tripwire_hook",    "cmd": 8108},
    "ruby_sword":   {"vanilla": "minecraft:iron_sword",       "cmd": 8109},
    "ruby_pickaxe": {"vanilla": "minecraft:iron_pickaxe",     "cmd": 8110},
}

# ---------------------------------------------------------------------------
#  4) Custom-Bloecke
#     Java setzt sie ueber Note-Block-Zustaende um (die Technik, die auch
#     grosse Content-Plugins nutzen). Jeder Block bekommt eine eigene
#     Kombination aus Instrument + Note; das Plugin haelt sie stabil.
# ---------------------------------------------------------------------------

BLOCK_PALETTE = dict(ITEM_PALETTE)
BLOCK_PALETTE.update({
    "1": hexc("6E6E76"),   # Stein hell
    "2": hexc("5A5A62"),   # Stein mittel
    "3": hexc("4A4A52"),   # Stein dunkel
    "4": hexc("E8E6E0"),   # Marmor hell
    "5": hexc("D2CFC6"),   # Marmor mittel
    "6": hexc("BAB6AB"),   # Marmor Ader
    "7": hexc("34343C"),   # Dunkelmarmor hell
    "8": hexc("26262C"),   # Dunkelmarmor dunkel
    "9": hexc("1A1A1F"),   # Dunkelmarmor Ader
    "L": hexc("7CFCFF"),   # Neon hell
    "l": hexc("2AA8C4"),   # Neon dunkel
})

# Farbtreppen je Material (dunkel -> hell). Mehr Stufen = mehr Tiefe.
STONE_SHADES = [hexc("3E3E46"), hexc("4A4A52"), hexc("56565E"), hexc("62626A"), hexc("6E6E76")]
RUBY_SHADES = [hexc("6E0A1C"), hexc("A31230"), hexc("D62245"), hexc("F5476A"), hexc("FF8098")]
SAPP_SHADES = [hexc("0B3A6E"), hexc("1160A8"), hexc("1E88D6"), hexc("46B0F5"), hexc("8ED6FF")]
MARBLE_SHADES = [hexc("C9C5BA"), hexc("D8D4CA"), hexc("E4E1D8"), hexc("EFEDE6"), hexc("F7F6F1")]
DARKM_SHADES = [hexc("2A2A33"), hexc("33333E"), hexc("3D3D49"), hexc("484855"), hexc("545463")]
GOLD_SHADES = [hexc("8A5B0F"), hexc("BE8517"), hexc("E0A81C"), hexc("FFD75A"), hexc("FFF0AE")]

# Kristallfarben fuer Erz-Einschluesse: (Rand, Glanz, Mitte, Schatten)
RUBY_GEM = (hexc("4A0713"), hexc("FF8098"), hexc("E0224A"), hexc("8E0F24"))
SAPP_GEM = (hexc("06284D"), hexc("8ED6FF"), hexc("1E88D6"), hexc("0E4C86"))


def block_ruby_ore():
    img = from_noise(16, STONE_SHADES, seed=101, cells=4)
    return scatter_gems(img, RUBY_GEM, seed=101, count=5)


def block_sapphire_ore():
    img = from_noise(16, STONE_SHADES, seed=202, cells=4)
    return scatter_gems(img, SAPP_GEM, seed=202, count=5)


def block_ruby_block():
    return bevel(crystal_surface(16, RUBY_SHADES, seed=303), light=60, dark=70)


def block_sapphire_block():
    return bevel(crystal_surface(16, SAPP_SHADES, seed=404), light=60, dark=70)


def block_marble():
    return bevel(veined(16, MARBLE_SHADES, hexc("A8A296"), seed=505, veins=3),
                 light=55, dark=45)


def block_dark_marble():
    return bevel(veined(16, DARKM_SHADES, hexc("13131A"), seed=606, veins=3),
                 light=50, dark=60)


def block_neon_lamp():
    """Dunkler Rahmen mit leuchtender Mitte."""
    img = from_noise(16, [hexc("1A1A22"), hexc("22222C"), hexc("2A2A36")], seed=707, cells=2)
    px = img.load()
    core = [hexc("1E7F98"), hexc("2AA8C4"), hexc("55D2E8"), hexc("7CFCFF")]
    for y in range(16):
        for x in range(16):
            # Abstand zum Rand bestimmt die Helligkeit -> weiches Leuchten
            d = min(x, y, 15 - x, 15 - y)
            if d >= 2:
                img.load()[x, y] = core[min(len(core) - 1, d - 2)]
    return bevel(img, light=40, dark=90)


def block_coin_pile():
    """Gestapelte Muenzen mit Licht von oben links."""
    img = from_noise(16, [hexc("6B4708"), hexc("7A520C")], seed=808, cells=2)
    px = img.load()
    coin = [
        "..XXX..",
        ".XHHHX.",
        "XHHMMMX",
        "XHMMMSX",
        "XMMMSSX",
        ".XSSSX.",
        "..XXX..",
    ]
    palette = {"X": hexc("5A3B06"), "H": hexc("FFF0AE"),
               "M": hexc("FFD75A"), "S": hexc("BE8517")}
    for cx, cy in [(0, 0), (8, 3), (3, 8), (11, 10), (6, 13)]:
        for dy, row in enumerate(coin):
            for dx, ch in enumerate(row):
                if ch == ".":
                    continue
                px[(cx + dx) % 16, (cy + dy) % 16] = palette[ch]
    return img


BLOCK_BUILDERS = {
    "ruby_ore": block_ruby_ore,
    "sapphire_ore": block_sapphire_ore,
    "ruby_block": block_ruby_block,
    "sapphire_block": block_sapphire_block,
    "marble": block_marble,
    "dark_marble": block_dark_marble,
    "neon_lamp": block_neon_lamp,
    "coin_pile": block_coin_pile,
}

# Note-Block-Zustand je Block. Instrument + Note ergeben die Variante.
# Das Plugin haelt diese Zustaende stabil (kein Umstimmen durch Rechtsklick,
# kein Instrumentwechsel durch den Block darunter).
BLOCK_STATES = {
    "ruby_ore":       {"instrument": "bit",           "note": 1,  "powered": False},
    "sapphire_ore":   {"instrument": "bit",           "note": 2,  "powered": False},
    "ruby_block":     {"instrument": "bit",           "note": 3,  "powered": False},
    "sapphire_block": {"instrument": "bit",           "note": 4,  "powered": False},
    "marble":         {"instrument": "bit",           "note": 5,  "powered": False},
    "dark_marble":    {"instrument": "bit",           "note": 6,  "powered": False},
    "neon_lamp":      {"instrument": "bit",           "note": 7,  "powered": False},
    "coin_pile":      {"instrument": "bit",           "note": 8,  "powered": False},
}

# Alle Note-Block-Instrumente in 1.21 - fuer die vollstaendige Blockstate-Datei
INSTRUMENTS = [
    "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar",
    "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit",
    "banjo", "pling", "zombie", "skeleton", "creeper", "dragon",
    "wither_skeleton", "piglin", "custom_head",
]


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

    # Zweite Icon-Reihe ab U+E120
    for i, name in enumerate(ICONS2_ORDER):
        place(32 + i, draw(ICONS2[name], ICON_PALETTE, CELL // 16), name)

    return atlas, mapping


def build_noteblock_states():
    """Vollstaendige note_block.json.

    WICHTIG: Diese Datei ersetzt die Blockstates des Notenblocks komplett.
    Jede nicht aufgefuehrte Kombination wuerde als fehlende Textur erscheinen -
    deshalb werden ALLE Instrument/Noten/Powered-Kombinationen erzeugt und nur
    die belegten auf eigene Modelle gelenkt.
    """
    lookup = {}
    for name, st in BLOCK_STATES.items():
        key = (st["instrument"], st["note"], bool(st["powered"]))
        lookup[key] = f"{NAMESPACE}:block/{name}"

    variants = {}
    for instrument in INSTRUMENTS:
        for note in range(0, 25):
            for powered in (False, True):
                key = f"instrument={instrument},note={note},powered={str(powered).lower()}"
                model = lookup.get((instrument, note, powered),
                                   "minecraft:block/note_block")
                variants[key] = {"model": model}
    return {"variants": variants}


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

    # ---- Custom-Bloecke ----
    (root / f"assets/{NAMESPACE}/textures/block").mkdir(parents=True, exist_ok=True)
    (root / f"assets/{NAMESPACE}/models/block").mkdir(parents=True, exist_ok=True)
    (root / "assets/minecraft/blockstates").mkdir(parents=True, exist_ok=True)

    for name, builder in BLOCK_BUILDERS.items():
        builder().save(root / f"assets/{NAMESPACE}/textures/block/{name}.png")
        # Wuerfelmodell mit derselben Textur auf allen Seiten
        (root / f"assets/{NAMESPACE}/models/block/{name}.json").write_text(json.dumps({
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"{NAMESPACE}:block/{name}"},
        }, indent=2), encoding="utf-8")
        # Das Item in der Hand zeigt denselben Wuerfel
        (root / f"assets/{NAMESPACE}/models/item/{name}.json").write_text(json.dumps({
            "parent": f"{NAMESPACE}:block/{name}"
        }, indent=2), encoding="utf-8")
        (root / f"assets/{NAMESPACE}/items/{name}.json").write_text(json.dumps({
            "model": {"type": "minecraft:model", "model": f"{NAMESPACE}:block/{name}"}
        }, indent=2), encoding="utf-8")

    (root / "assets/minecraft/blockstates/note_block.json").write_text(
        json.dumps(build_noteblock_states(), indent=1), encoding="utf-8")

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

    # Block-Texturen fuer Bedrock (fuer Geysers Custom-Block-Unterstuetzung)
    (root / "textures/blocks").mkdir(parents=True, exist_ok=True)
    block_textures = {}
    for name, builder in BLOCK_BUILDERS.items():
        builder().save(root / f"textures/blocks/{name}.png")
        block_textures[f"{NAMESPACE}_{name}"] = {"textures": f"textures/blocks/{name}"}
    (root / "textures/terrain_texture.json").write_text(json.dumps({
        "resource_pack_name": NAMESPACE,
        "texture_name": "atlas.terrain",
        "padding": 8,
        "num_mip_levels": 4,
        "texture_data": block_textures,
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


def write_block_reference():
    """Schreibt die Blockzustaende als YAML - direkt nutzbar in SMPContent."""
    lines = ["# Von generate.py erzeugt - Zustaende muessen zum Texturepack passen!",
             "blocks:"]
    for name, st in BLOCK_STATES.items():
        lines.append(f"  {name}:")
        lines.append(f"    instrument: {st['instrument']}")
        lines.append(f"    note: {st['note']}")
        lines.append(f"    powered: {str(st['powered']).lower()}")
    (DIST / "blocks-states.yml").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    DIST.mkdir(parents=True, exist_ok=True)
    atlas, mapping = build_atlas()

    java_dir = write_java(atlas, mapping)
    bedrock_dir = write_bedrock(atlas)
    geyser_dir = write_geyser_mapping()
    write_glyph_reference(mapping)

    write_block_reference()

    zip_dir(java_dir, DIST / "SMP-Java-Pack.zip")
    zip_dir(bedrock_dir, DIST / "SMP-Bedrock-Pack.mcpack")

    print("Fertig!")
    print(f"  Java-Pack:    {DIST / 'SMP-Java-Pack.zip'}")
    print(f"  Bedrock-Pack: {DIST / 'SMP-Bedrock-Pack.mcpack'}")
    print(f"  Geyser-Map:   {geyser_dir / 'smp_items.json'}")
    print(f"  Glyphen:      {DIST / 'glyphen.txt'}")
    print(f"\n  {len(ICON_ORDER) + len(ICONS2_ORDER)} Icons, "
          f"{len(RANK_ORDER)} Rang-Abzeichen, {len(CUSTOM_ITEMS)} Custom-Items, "
          f"{len(BLOCK_BUILDERS)} Custom-Bloecke")


if __name__ == "__main__":
    main()
