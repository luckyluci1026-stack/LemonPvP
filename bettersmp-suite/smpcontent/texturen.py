#!/usr/bin/env python3
"""
Texturen und 3D-Modelle für die mitgelieferten Inhalte
======================================================

Erzeugt alles prozedural - kein Grafikprogramm nötig:

  src/main/resources/assets/textures/block/<id>.png   50 Möbel
  src/main/resources/assets/textures/item/<id>.png    Fahrzeuge, Waffen, Schirm
  src/main/resources/assets/models/block/<id>.json    echte Möbelformen
  src/main/resources/assets/models/item/<id>.json     Autos, Flieger

Das Plugin legt diese Dateien beim ersten Start in
plugins/SMPContent/textures/ und models/ ab. Wer etwas schöner haben
will, überschreibt einfach die Datei - beim nächsten /smpcontent pack
ist die eigene drin.

    python3 texturen.py
"""

import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).parent
TEX = ROOT / "src/main/resources/assets/textures"
MOD = ROOT / "src/main/resources/assets/models"

S = 16  # Kantenlänge einer Textur


# ---------------------------------------------------------------- Werkzeug

def img():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(4))


def box(im, x0, y0, x1, y1, color):
    for x in range(max(0, x0), min(S, x1 + 1)):
        for y in range(max(0, y0), min(S, y1 + 1)):
            im.putpixel((x, y), color)


def line_h(im, y, x0, x1, color):
    box(im, x0, y, x1, y, color)


def grain(im, base, dark, seed):
    """Holzmaserung: senkrechte Adern mit ruhigem Rauschen."""
    rnd = random.Random(seed)
    for x in range(S):
        ader = rnd.random() < 0.28
        for y in range(S):
            t = (math.sin(x * 1.7 + y * 0.35 + seed) + 1) / 2
            farbe = mix(base, dark, 0.18 + t * 0.22)
            if ader:
                farbe = mix(farbe, dark, 0.35)
            if rnd.random() < 0.06:
                farbe = mix(farbe, dark, 0.3)
            im.putpixel((x, y), farbe)


def bevel(im, hell=0.30, dunkel=0.35):
    """Oben/links heller, unten/rechts dunkler - gibt Tiefe."""
    weiss = (255, 255, 255, 255)
    schwarz = (0, 0, 0, 255)
    for x in range(S):
        p = im.getpixel((x, 0))
        if p[3]:
            im.putpixel((x, 0), mix(p, weiss, hell))
        p = im.getpixel((x, S - 1))
        if p[3]:
            im.putpixel((x, S - 1), mix(p, schwarz, dunkel))
    for y in range(S):
        p = im.getpixel((0, y))
        if p[3]:
            im.putpixel((0, y), mix(p, weiss, hell * 0.7))
        p = im.getpixel((S - 1, y))
        if p[3]:
            im.putpixel((S - 1, y), mix(p, schwarz, dunkel * 0.7))


def save(im, ordner, name):
    ziel = ordner / f"{name}.png"
    ziel.parent.mkdir(parents=True, exist_ok=True)
    im.save(ziel)


def modell(ordner, name, elemente, textur, groesse=(16, 16), display=None):
    daten = {
        "texture_size": list(groesse),
        "textures": {"0": textur, "particle": textur},
        "elements": elemente,
    }
    if display:
        daten["display"] = display
    ziel = ordner / f"{name}.json"
    ziel.parent.mkdir(parents=True, exist_ok=True)
    ziel.write_text(json.dumps(daten, indent=2), encoding="utf-8")


def quader_uv(von, bis, uv):
    """Quader, bei dem alle Seiten denselben Ausschnitt der Textur zeigen.

    Bei einem Auto ist das genau richtig: die Seitenansicht gehört an die
    Seiten, das Dach nimmt den oberen Streifen, die Räder den dunklen.
    """
    faces = {seite: {"uv": list(uv), "texture": "#0"}
             for seite in ("north", "south", "east", "west", "up", "down")}
    return {"from": list(von), "to": list(bis), "faces": faces}


def quader(von, bis, seiten=("north", "south", "east", "west", "up", "down")):
    faces = {}
    for seite in seiten:
        # Grobe, aber ehrliche UV: die Fläche bekommt den passenden Ausschnitt
        if seite in ("up", "down"):
            uv = [von[0], von[2], bis[0], bis[2]]
        elif seite in ("north", "south"):
            uv = [von[0], 16 - bis[1], bis[0], 16 - von[1]]
        else:
            uv = [von[2], 16 - bis[1], bis[2], 16 - von[1]]
        uv = [max(0, min(16, v)) for v in uv]
        faces[seite] = {"uv": uv, "texture": "#0"}
    return {"from": list(von), "to": list(bis), "faces": faces}


# ---------------------------------------------------------------- Möbel

HOLZ = {
    "eiche": ("#B08B4F", "#7C5F32", "#D6B87E"),
    "fichte": ("#7A5B33", "#513A20", "#9C7A4C"),
    "birke": ("#D8CB9A", "#AC9D6C", "#EFE6C0"),
    "akazie": ("#C1662A", "#8E4718", "#E08B4A"),
    "dunkeleiche": ("#4C331C", "#2F1F10", "#6B4B2C"),
}

STOFF = {
    "eiche": "#7C4B4B", "fichte": "#3F5A6B", "birke": "#6B7C4B",
    "akazie": "#7C6B3F", "dunkeleiche": "#5A3F6B",
}


def moebel_textur(holz, art):
    base, dark, light = (hexc(c) for c in HOLZ[holz])
    im = img()
    grain(im, base, dark, seed=sum(ord(c) for c in holz + art))

    fuge = mix(base, dark, 0.85)      # eine Fuge ist fast schwarz
    glanz = mix(base, light, 0.75)    # ein Beschlag blitzt

    def knauf(x, y):
        if not (0 <= x < S and 0 <= y < S):
            return
        box(im, x, y, x + 1, y + 1, mix(base, dark, 0.9))
        im.putpixel((x, y), glanz)

    if art in ("bank", "zaunbank"):
        # Sitzlatten mit deutlichem Spalt dazwischen
        for y in (4, 9, 14):
            line_h(im, y, 0, 15, fuge)
            line_h(im, y - 1, 0, 15, mix(base, light, 0.3))
    elif art == "sessel":
        stoff = hexc(STOFF[holz])
        box(im, 2, 3, 13, 12, mix(stoff, dark, 0.3))
        box(im, 3, 4, 12, 11, stoff)
        box(im, 4, 5, 11, 10, mix(stoff, light, 0.22))
        # Knopf in der Mitte, wie bei einem gepolsterten Sessel
        box(im, 7, 7, 8, 8, mix(stoff, dark, 0.6))
    elif art == "nachttisch":
        line_h(im, 6, 1, 14, fuge)
        box(im, 1, 7, 14, 14, mix(base, dark, 0.18))
        knauf(7, 10)
    elif art == "kommode":
        for y in (2, 7, 12):
            line_h(im, y, 0, 15, fuge)
            line_h(im, y + 1, 0, 15, mix(base, light, 0.28))
            knauf(7, y + 3)
    elif art == "schrank":
        box(im, 7, 0, 8, 15, fuge)
        box(im, 0, 0, 15, 0, mix(base, light, 0.35))
        knauf(5, 7)
        knauf(10, 7)
    elif art == "truhe":
        box(im, 0, 5, 15, 6, fuge)
        box(im, 5, 4, 10, 9, mix(base, dark, 0.35))
        box(im, 6, 5, 9, 8, glanz)
        box(im, 7, 6, 8, 7, dark)
    elif art == "podest":
        box(im, 0, 0, 15, 1, mix(base, light, 0.4))
        box(im, 0, 14, 15, 15, mix(base, dark, 0.5))
        box(im, 2, 3, 13, 12, mix(base, light, 0.2))
        for x, y in ((2, 3), (13, 3), (2, 12), (13, 12)):
            im.putpixel((x, y), fuge)

    bevel(im)
    return im


MOEBEL_FORM = {
    # Art -> Liste von Quadern (von, bis)
    "bank": [((1, 5, 3), (15, 7, 13)), ((1, 7, 11), (15, 15, 13)),
             ((1, 0, 3), (3, 5, 5)), ((13, 0, 3), (15, 5, 5)),
             ((1, 0, 11), (3, 5, 13)), ((13, 0, 11), (15, 5, 13))],
    "zaunbank": [((1, 6, 4), (15, 8, 12)), ((2, 8, 11), (3, 14, 12)),
                 ((13, 8, 11), (14, 14, 12)), ((2, 12, 11), (14, 14, 12)),
                 ((1, 0, 4), (3, 6, 6)), ((13, 0, 4), (15, 6, 6)),
                 ((1, 0, 10), (3, 6, 12)), ((13, 0, 10), (15, 6, 12))],
    "sessel": [((3, 5, 3), (13, 7, 13)), ((3, 7, 11), (13, 14, 13)),
               ((3, 7, 3), (4, 11, 11)), ((12, 7, 3), (13, 11, 11)),
               ((3, 0, 3), (5, 5, 5)), ((11, 0, 3), (13, 5, 5)),
               ((3, 0, 11), (5, 5, 13)), ((11, 0, 11), (13, 5, 13))],
    "nachttisch": [((2, 10, 2), (14, 12, 14)), ((3, 1, 3), (13, 10, 13)),
                   ((2, 0, 2), (4, 1, 4)), ((12, 0, 2), (14, 1, 4)),
                   ((2, 0, 12), (4, 1, 14)), ((12, 0, 12), (14, 1, 14))],
    "kommode": [((1, 0, 2), (15, 13, 14)), ((1, 13, 1), (15, 15, 15))],
    "schrank": [((1, 0, 3), (15, 16, 13))],
    "truhe": [((1, 0, 2), (15, 9, 14)), ((1, 9, 2), (15, 12, 14))],
    "podest": [((0, 0, 0), (16, 4, 16))],
    "tisch": [((0, 12, 0), (16, 15, 16)), ((1, 0, 1), (4, 12, 4)),
              ((12, 0, 1), (15, 12, 4)), ((1, 0, 12), (4, 12, 15)),
              ((12, 0, 12), (15, 12, 15))],
}


def moebel():
    anzahl = 0
    for holz in HOLZ:
        for art in ("bank", "sessel", "nachttisch", "kommode", "schrank",
                    "truhe", "zaunbank", "podest"):
            name = f"{holz}_{art}"
            save(moebel_textur(holz, art), TEX / "block", name)
            form = MOEBEL_FORM.get(art)
            if form:
                modell(MOD / "block", name,
                       [quader(v, b) for v, b in form], f"smp:block/{name}")
            anzahl += 1
    return anzahl


# ------------------------------------------------------- Einzelne Möbel

def sonder_moebel():
    stuecke = {
        "steinbank": ("#8A8A8A", "#5E5E5E", "bank"),
        "stein_tisch": ("#8A8A8A", "#5E5E5E", "tisch"),
        "lampe_klein": ("#E8D48A", "#B79A45", None),
        "lampe_gross": ("#E8D48A", "#B79A45", None),
        "buecherstapel": ("#8B3A3A", "#5C2424", None),
        "teppichrolle": ("#9C4B6B", "#6B3049", "podest"),
        "blumenkasten": ("#6B4A2A", "#452F1A", "truhe"),
        "fass_deko": ("#7A5B33", "#4E3A20", None),
        "amboss_deko": ("#4A4A52", "#26262C", None),
        "kristallsaeule": ("#8FD8E8", "#4A93A8", None),
    }
    for name, (hell, dunkel, form) in stuecke.items():
        base, dark = hexc(hell), hexc(dunkel)
        im = img()
        grain(im, base, dark, seed=sum(ord(c) for c in name))
        if "fass" in name:
            for y in (2, 3, 12, 13):
                line_h(im, y, 0, 15, mix(base, dark, 0.75))
            box(im, 6, 6, 9, 9, mix(base, dark, 0.5))
        if "amboss" in name:
            box(im, 0, 1, 15, 4, mix(base, hexc("#FFFFFF"), 0.18))
            box(im, 4, 5, 11, 10, base)
            box(im, 2, 11, 13, 15, mix(base, dark, 0.4))
        if "teppich" in name:
            for y in range(0, 16, 3):
                line_h(im, y, 0, 15, mix(base, dark, 0.55))
        if "lampe" in name:
            box(im, 4, 4, 11, 11, mix(base, hexc("#FFFFFF"), 0.5))
        if "buecher" in name:
            for y, farbe in ((2, "#3A6B8B"), (6, "#8B3A3A"), (10, "#3A8B4A")):
                box(im, 1, y, 14, y + 2, hexc(farbe))
        if "kristall" in name:
            for x in range(S):
                box(im, x, 0, x, 15, mix(base, hexc("#FFFFFF"),
                                         0.25 + 0.2 * math.sin(x * 0.9)))
        bevel(im)
        save(im, TEX / "block", name)
        if form:
            modell(MOD / "block", name,
                   [quader(v, b) for v, b in MOEBEL_FORM[form]],
                   f"smp:block/{name}")
    return len(stuecke)


# ---------------------------------------------------------------- Items

FAHRZEUG_FARBEN = {
    "auto_sport": ("#C22B2B", "#7A1616", "#2A2A30"),
    "auto_roadster": ("#E0A020", "#9C6C10", "#2A2A30"),
    "auto_muscle": ("#8B1A1A", "#561010", "#2A2A30"),
    "auto_renn": ("#E8D020", "#A08C10", "#2A2A30"),
    "auto_hyper": ("#A050E0", "#6A2F9C", "#2A2A30"),
    "auto_oldtimer": ("#2F6B5A", "#1C443A", "#3A2A1A"),
    "auto_polizei": ("#2A4FA0", "#16306B", "#2A2A30"),
    "auto_gelaende": ("#4A6B32", "#2F4520", "#2A2A30"),
    "jet_renn": ("#B8C4D0", "#7A8794", "#3A4650"),
    "jet_kampf": ("#6B7280", "#454A54", "#2A2E36"),
    "jet_passagier": ("#E4E8EC", "#A8AEB6", "#2A4FA0"),
    "flugzeug_propeller": ("#E0C020", "#A08C10", "#7A5B33"),
    "flugzeug_doppeldecker": ("#C25A2B", "#7A3416", "#7A5B33"),
    "flugzeug_fracht": ("#4A6B32", "#2F4520", "#3A4650"),
    "heli_leicht": ("#2F8B9C", "#1C5A66", "#2A2E36"),
    "heli_rettung": ("#C22B2B", "#7A1616", "#E4E8EC"),
    "zug_lok": ("#4A4A52", "#26262C", "#8B3A3A"),
}

# Seitenansichten: . durchsichtig, K Karosserie, D dunkel, F Fenster/Akzent
AUTO = """
................
................
................
.....KKKKKK.....
....KKFFFFKK....
...KKKKKKKKKK...
..KKKKKKKKKKKK..
..DKKKKKKKKKKD..
..DDKKKKKKKKDD..
...DD.DDDD.DD...
....D.D..D.D....
................
................
................
................
................
"""

JET = """
................
................
.......K........
.......KK.......
.......KK.......
....KKKKKKKK....
...FKKKKKKKKF...
...KKKKKKKKKK...
.......KK.......
.......KK.......
......DKKD......
.....DD..DD.....
................
................
................
................
"""

PROP = """
................
................
.D..............
.D....KKKKK.....
.D...KKFFKKK....
.DKKKKKKKKKKKD..
.D...KKKKKK.KD..
.D....KKKK......
......KKKK......
.....DDDDDD.....
.......DD.......
................
................
................
................
................
"""

DOPPEL = """
................
................
..KKKKKKKKKKK...
..KKKKKKKKKKK...
.......KK.......
.D...KKKKKKK....
.D..KKFFKKKKKD..
.D...KKKKKKK.D..
..KKKKKKKKKKK...
..KKKKKKKKKKK...
.....DD..DD.....
................
................
................
................
................
"""

FRACHT = """
................
................
...KKKKKKKKKK...
..KKKKKKKKKKKD..
.KKFFKKKKKKKKD..
.KKKKKKKKKKKKK..
.KKKKKKKKKKKK...
..DDKKKKKKDD....
...D.DDDD.D.....
....DD..DD......
................
................
................
................
................
................
"""

HELI = """
................
..DDDDDDDDDDDD..
.......D........
.....KKKKKK.....
....KKFFFFKK....
...KKKKKKKKKKD..
...KKKKKKKKKKDD.
....KKKKKKKK....
.....D....D.....
....DDD..DDD....
................
................
................
................
................
................
"""

ZUG = """
................
...D............
...D............
..DDD...........
..KKKKKKKKKKK...
..KKKKKKKKKKK...
..KKKKFFFKKKK...
..KKKKFFFKKKK...
..KKKKKKKKKKK...
..DDDDDDDDDDD...
...DD..DD..DD...
...DD..DD..DD...
................
................
................
................
"""


def aus_form(form, palette):
    im = img()
    zeilen = [z for z in form.strip("\n").split("\n")]
    for y, zeile in enumerate(zeilen[:S]):
        for x, zeichen in enumerate(zeile[:S]):
            if zeichen in palette:
                im.putpixel((x, y), palette[zeichen])
    return im


def koerper(name):
    """
    Der Körper eines Fahrzeugs, aus wenigen Quadern.

    Die UV-Ausschnitte zeigen auf die passende Stelle der Seitenansicht:
    der Aufbau nimmt den hellen Streifen, die Räder den dunklen darunter.
    Dadurch braucht jedes Fahrzeug nur eine einzige 16x16-Textur.
    """
    hell = [3, 4, 13, 8]      # Karosserie
    dunkel = [3, 9, 6, 11]    # Räder, Fahrwerk, Streben
    dach = [5, 3, 11, 6]      # Fenster und Aufbau

    if name.startswith("auto_"):
        return [
            quader_uv((1, 3, 3), (15, 8, 13), hell),      # Wanne
            quader_uv((4, 8, 4), (12, 11, 12), dach),     # Kabine
            quader_uv((2, 0, 2), (5, 3, 5), dunkel),      # vier Räder
            quader_uv((11, 0, 2), (14, 3, 5), dunkel),
            quader_uv((2, 0, 11), (5, 3, 14), dunkel),
            quader_uv((11, 0, 11), (14, 3, 14), dunkel),
        ]
    if name.startswith("heli_"):
        return [
            quader_uv((4, 3, 2), (12, 10, 12), hell),     # Kanzel
            quader_uv((6, 5, 12), (10, 8, 16), hell),     # Heckausleger
            quader_uv((0, 12, 7), (16, 13, 9), dunkel),   # Rotor quer
            quader_uv((7, 12, 1), (9, 13, 15), dunkel),   # Rotor laengs
            quader_uv((7, 10, 7), (9, 12, 9), dunkel),    # Rotorkopf
            quader_uv((5, 0, 3), (6, 3, 11), dunkel),     # Kufen
            quader_uv((10, 0, 3), (11, 3, 11), dunkel),
        ]
    if name == "zug_lok":
        return [
            quader_uv((2, 3, 1), (14, 11, 15), hell),     # Kessel
            quader_uv((5, 11, 2), (8, 15, 5), dunkel),    # Schornstein
            quader_uv((2, 0, 1), (14, 3, 15), dunkel),    # Fahrwerk
        ]
    # Flugzeuge und Jets: Rumpf, Fluegel, Leitwerk
    lang = 15 if "passagier" in name or "fracht" in name else 13
    return [
        quader_uv((6, 4, 1), (10, 9, lang), hell),        # Rumpf
        quader_uv((0, 5, 5), (16, 6, 10), hell),          # Tragflaechen
        quader_uv((5, 5, lang - 3), (11, 6, lang), hell), # Hoehenruder
        quader_uv((7, 9, lang - 3), (9, 13, lang), dunkel),  # Seitenruder
        quader_uv((3, 3, 6), (5, 5, 9), dunkel),          # Triebwerke
        quader_uv((11, 3, 6), (13, 5, 9), dunkel),
    ]


def fahrzeug_items():
    for name, (hell, dunkel, akzent) in FAHRZEUG_FARBEN.items():
        if name.startswith("auto_"):
            form = AUTO
        elif name.startswith("heli_"):
            form = HELI
        elif name.startswith("jet_"):
            form = JET
        elif name == "zug_lok":
            form = ZUG
        elif "doppeldecker" in name:
            form = DOPPEL
        elif "fracht" in name:
            form = FRACHT
        else:
            form = PROP
        palette = {"K": hexc(hell), "D": hexc(dunkel), "F": hexc(akzent)}
        im = aus_form(form, palette)
        # Etwas Tiefe: untere Hälfte abdunkeln
        for x in range(S):
            for y in range(S // 2, S):
                p = im.getpixel((x, y))
                if p[3]:
                    im.putpixel((x, y), mix(p, (0, 0, 0, 255), 0.12))
        save(im, TEX / "item", name)

        modell(MOD / "item", name, koerper(name), f"smp:item/{name}",
               display={"thirdperson_righthand": {
                   "rotation": [0, 90, 0], "translation": [0, 1, 0],
                   "scale": [0.45, 0.45, 0.45]},
                   "gui": {"rotation": [30, 225, 0], "scale": [0.5, 0.5, 0.5]}})
    return len(FAHRZEUG_FARBEN)


SCHIRM = """
................
.....KKKKKK.....
...KKKKKKKKKK...
..KKKKKKKKKKKK..
.KKKKKKKKKKKKKK.
.KKFFKKKKKKFFKK.
..KK.K.KK.K.KK..
...D..D..D..D...
....D.D..D.D....
.....DD..DD.....
......D..D......
.......DD.......
.......DD.......
................
................
................
"""

PISTOLE = """
................
................
................
....KKKKKKKK....
....KKKKKKKKD...
....KKD.........
....KKD.........
...KKKD.........
...KKD..........
...KKD..........
....D...........
................
................
................
................
................
"""

FLINTE = """
................
................
..KKKKKKKKKKKD..
..KKKKKKKKKKKD..
..DDDKKD........
.....KKD........
.....KKD........
....KKKD........
....KKD.........
.....D..........
................
................
................
................
................
................
"""

WERFER = """
................
................
................
..KKKKKKKKKKKK..
..KFFKKKKKKKKK..
..KKKKKKKKKKKK..
..DDDDKKDDDDDD..
......KKD.......
.....KKKD.......
.....KKD........
......D.........
................
................
................
................
................
"""

KUGEL = """
................
................
................
................
......KK........
.....KKKK.......
.....KFFK.......
.....KKKK.......
......DD........
................
................
................
................
................
................
................
"""


def sonstige_items():
    stuecke = {
        "fallschirm": (SCHIRM, "#E8E8EC", "#9C9CA4", "#C22B2B"),
        "pistole": (PISTOLE, "#4A4A52", "#26262C", "#8B6B3A"),
        "schrotflinte": (FLINTE, "#5A4A3A", "#332720", "#4A4A52"),
        "raketenwerfer": (WERFER, "#3A4A3A", "#1F2A1F", "#C22B2B"),
        "kugel": (KUGEL, "#C8B060", "#8B7530", "#E8D890"),
    }
    for name, (form, hell, dunkel, akzent) in stuecke.items():
        palette = {"K": hexc(hell), "D": hexc(dunkel), "F": hexc(akzent)}
        save(aus_form(form, palette), TEX / "item", name)
    return len(stuecke)


# ---------------------------------------------------------------- Lauf

if __name__ == "__main__":
    a = moebel()
    b = sonder_moebel()
    c = fahrzeug_items()
    d = sonstige_items()
    print(f"{a + b} Möbel, {c} Fahrzeuge, {d} weitere Items")
    print(f"Texturen: {len(list((TEX / 'block').glob('*.png')))} Block, "
          f"{len(list((TEX / 'item').glob('*.png')))} Item")
    print(f"Modelle:  {len(list((MOD / 'block').glob('*.json')))} Block, "
          f"{len(list((MOD / 'item').glob('*.json')))} Item")
