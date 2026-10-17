"""Generates the Swappie poses (512x512 SVG) from shared parts.

Usage: python3 assets/swappie/make_poses.py assets/swappie
"""
import math
import pathlib
import sys

OUT = pathlib.Path(sys.argv[1])
OUT.mkdir(parents=True, exist_ok=True)

DARK = "#1e293b"
INK = "#0f172a"
PAPER = "#f8fafc"
FOLD = "#cbd5e1"
CHEEK = "#fb7185"
WHITE = "#ffffff"

BODY = "M164 120 H316 L368 172 V384 A22 22 0 0 1 346 406 H164 A22 22 0 0 1 142 384 V142 A22 22 0 0 1 164 120 Z"
FOLD_PATH = "M316 120 V156 A16 16 0 0 0 332 172 H368 Z"
LEGS = ["M214 404 L208 446", "M298 404 L304 446"]
FEET = [(200, 452), (312, 452)]


class Svg:
    def __init__(self):
        self.outline = []   # white sticker outline, drawn first
        self.back = []      # behind the body
        self.front = []     # after the body and face

    def line(self, d, layer, width=14, color=DARK, outline=True):
        if outline:
            self.outline.append(f'<path d="{d}" fill="none" stroke="{WHITE}" stroke-width="{width + 12}" stroke-linecap="round" stroke-linejoin="round"/>')
        layer.append(f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"/>')

    def hand(self, x, y, layer):
        self.outline.append(f'<circle cx="{x}" cy="{y}" r="28" fill="{WHITE}"/>')
        layer.append(f'<circle cx="{x}" cy="{y}" r="17" fill="{PAPER}" stroke="{DARK}" stroke-width="10"/>')

    def shape(self, svg_outline, svg_normal, layer):
        if svg_outline:
            self.outline.append(svg_outline)
        layer.append(svg_normal)


def eyes(dx=0, dy=0, hx=-6, hy=-9):
    return (f'<ellipse cx="{216 + dx}" cy="{236 + dy}" rx="17" ry="22" fill="{INK}"/>'
            f'<ellipse cx="{296 + dx}" cy="{236 + dy}" rx="17" ry="22" fill="{INK}"/>'
            f'<circle cx="{216 + dx + hx}" cy="{236 + dy + hy}" r="6" fill="{WHITE}"/>'
            f'<circle cx="{296 + dx + hx}" cy="{236 + dy + hy}" r="6" fill="{WHITE}"/>')


def cheeks(opacity=0.55):
    return (f'<ellipse cx="186" cy="278" rx="17" ry="10" fill="{CHEEK}" opacity="{opacity}"/>'
            f'<ellipse cx="326" cy="278" rx="17" ry="10" fill="{CHEEK}" opacity="{opacity}"/>')


def stroke(d, w=9, color=INK):
    return f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{w}" stroke-linecap="round" stroke-linejoin="round"/>'


FACES = {
    "happy": eyes() + cheeks()
    + f'<path d="M228 272 Q256 312 284 272 Z" fill="{INK}" stroke="{INK}" stroke-width="6" stroke-linejoin="round"/>'
    + f'<path d="M243 293 Q256 284 269 293 Q262 302 256 302 Q250 302 243 293 Z" fill="{CHEEK}"/>',
    "joy": stroke("M198 244 Q216 216 234 244", 10) + stroke("M278 244 Q296 216 314 244", 10) + cheeks(0.7)
    + f'<path d="M222 268 Q256 324 290 268 Z" fill="{INK}" stroke="{INK}" stroke-width="6" stroke-linejoin="round"/>'
    + f'<path d="M240 298 Q256 286 272 298 Q264 309 256 309 Q248 309 240 298 Z" fill="{CHEEK}"/>',
    "think": eyes(dx=6, dy=-4, hx=-4, hy=-11) + cheeks(0.4)
    + stroke("M204 200 Q220 196 236 200", 8) + stroke("M286 190 Q304 178 320 188", 8)
    + stroke("M240 290 Q256 284 276 292", 8),
    "focus": eyes() + cheeks()
    + stroke("M230 276 Q256 300 282 276", 9)
    + f'<path d="M266 286 Q284 284 282 300 Q280 310 270 306 Q262 300 266 286 Z" fill="{CHEEK}" stroke="{INK}" stroke-width="4"/>',
    "confident": eyes() + cheeks()
    + stroke("M228 274 Q256 302 284 274", 9),
    "sleep": stroke("M200 238 Q216 252 232 238", 9) + stroke("M280 238 Q296 252 312 238", 9) + cheeks(0.45)
    + f'<ellipse cx="256" cy="286" rx="9" ry="11" fill="{INK}"/>',
    "determined": eyes(dy=4) + cheeks(0.45)
    + stroke("M196 204 L234 218", 9) + stroke("M316 204 L278 218", 9)
    + f'<path d="M226 274 Q256 306 286 274 Z" fill="{INK}" stroke="{INK}" stroke-width="6" stroke-linejoin="round"/>'
    + f'<path d="M234 278 H278" stroke="{WHITE}" stroke-width="6" stroke-linecap="round"/>',
}


def base(svg):
    for d in LEGS:
        svg.line(d, svg.back)
    for x, y in FEET:
        svg.shape(f'<ellipse cx="{x}" cy="{y}" rx="32" ry="18" fill="{WHITE}"/>',
                  f'<ellipse cx="{x}" cy="{y}" rx="26" ry="12" fill="{DARK}"/>', svg.back)


def body_svg(text_lines=True):
    s = (f'<path d="{BODY}" fill="{PAPER}" stroke="{DARK}" stroke-width="10" stroke-linejoin="round"/>'
         f'<path d="{FOLD_PATH}" fill="{FOLD}" stroke="{DARK}" stroke-width="10" stroke-linejoin="round"/>')
    if text_lines:
        s += (f'<g stroke="{FOLD}" stroke-width="8" stroke-linecap="round"><path d="M182 344 H330"/><path d="M182 368 H286"/></g>')
    return s


def rot(x, y, deg, ox, oy):
    a = math.radians(deg)
    return (ox + x * math.cos(a) - y * math.sin(a), oy + x * math.sin(a) + y * math.cos(a))


GRAD = ('<linearGradient id="{id}" x1="0" y1="0" x2="1" y2="1">'
        '<stop offset="0" stop-color="#fde047"/><stop offset="1" stop-color="#22c55e"/></linearGradient>')


def render(name, title, desc, svg, face, text_lines=True, defs="", extra_front=""):
    sid = f"swappie-{name}"
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512" role="img" aria-labelledby="{sid}-t {sid}-d">',
        f'  <title id="{sid}-t">{title}</title>',
        f'  <desc id="{sid}-d">{desc}</desc>',
        f'  <defs>{GRAD.format(id=sid + "-swap")}{defs}</defs>',
        '  <g>' + "".join(svg.outline) + f'<path d="{BODY}" fill="{WHITE}" stroke="{WHITE}" stroke-width="22" stroke-linejoin="round"/></g>',
        '  <g>' + "".join(svg.back) + '</g>',
        '  ' + body_svg(text_lines),
        '  ' + face,
        '  <g>' + "".join(svg.front) + extra_front + '</g>',
        '</svg>',
    ]
    text = "\n".join(parts).replace("url(#swap)", f"url(#{sid}-swap)")
    (OUT / f"{sid}.svg").write_text(text + "\n")


# --- wave (character only) --------------------------------------------------------
s = Svg(); base(s)
s.line("M146 296 Q112 270 104 222", s.back); s.hand(102, 210, s.back)
s.line("M366 300 Q398 322 404 356", s.back); s.hand(406, 366, s.back)
render("wave", "Swappie waving", "Swappie, the PaperSwap mascot, waving hello", s, FACES["happy"])

# --- cheer: both arms up, confetti --------------------------------------------------
s = Svg(); base(s)
s.line("M146 286 Q106 254 106 188", s.back); s.hand(106, 174, s.back)
s.line("M366 286 Q406 254 406 188", s.back); s.hand(406, 174, s.back)
confetti = ""
pieces = [(70, 120, 20, "#fde047"), (128, 70, -30, "#22c55e"), (206, 54, 15, "#60a5fa"), (306, 50, -20, "#f472b6"),
          (380, 78, 35, "#fb923c"), (452, 118, -15, "#22c55e"), (52, 214, 40, "#f472b6"), (462, 226, -35, "#fde047"),
          (256, 30, 60, "#22c55e"), (160, 100, 70, "#fb923c"), (352, 104, -60, "#60a5fa")]
for x, y, r, c in pieces:
    confetti += f'<rect x="{x - 8}" y="{y - 4}" width="16" height="8" rx="2" fill="{c}" transform="rotate({r} {x} {y})"/>'
for x, y, c in [(96, 64, "#60a5fa"), (420, 54, "#f472b6"), (30, 160, "#22c55e"), (486, 170, "#60a5fa")]:
    confetti += f'<circle cx="{x}" cy="{y}" r="6" fill="{c}"/>'
render("cheer", "Swappie cheering", "Swappie, the PaperSwap mascot, cheering with both arms up under confetti",
       s, FACES["joy"], extra_front=confetti)

# --- think: hand on chin, question bubble -------------------------------------------
s = Svg(); base(s)
s.line("M146 300 Q114 322 108 356", s.back); s.hand(106, 366, s.back)
s.line("M368 310 Q340 354 280 332", s.front, outline=False); s.hand(268, 326, s.front)
bubble = (f'<circle cx="384" cy="150" r="8" fill="{WHITE}" stroke="{DARK}" stroke-width="6"/>'
          f'<circle cx="406" cy="118" r="12" fill="{WHITE}" stroke="{DARK}" stroke-width="6"/>'
          f'<circle cx="440" cy="72" r="38" fill="{WHITE}" stroke="{DARK}" stroke-width="8"/>'
          + stroke("M427 62 Q429 47 442 47 Q455 47 455 60 Q455 70 442 75 L442 84", 8, "url(#swap)")
          + f'<circle cx="442" cy="96" r="5.5" fill="#22c55e"/>')
render("think", "Swappie thinking", "Swappie, the PaperSwap mascot, thinking with a hand on the chin and a question mark",
       s, FACES["think"], extra_front=bubble)

# --- wrench: core patches ----------------------------------------------------------
s = Svg(); base(s)
s.line("M146 300 Q114 322 108 356", s.back); s.hand(106, 366, s.back)
s.line("M366 296 Q412 290 418 240", s.back)
gx, gy, ang = 418, 226, 25
wrench_defs = ('<mask id="swappie-wrench-jaw"><rect x="-60" y="-160" width="120" height="220" fill="#fff"/>'
               '<rect x="-12" y="-160" width="24" height="44" rx="3" fill="#000"/></mask>')
wrench_shape = (f'<g mask="url(#swappie-wrench-jaw)"><circle cx="0" cy="-112" r="30"/>'
                f'<rect x="-11" y="-100" width="22" height="140" rx="11"/></g>')
s.outline.append(f'<g transform="translate({gx} {gy}) rotate({ang})" fill="{WHITE}" stroke="{WHITE}" stroke-width="20" stroke-linejoin="round">{wrench_shape}</g>')
s.back.append(f'<g transform="translate({gx} {gy}) rotate({ang})" fill="#94a3b8" stroke="{DARK}" stroke-width="8" stroke-linejoin="round">{wrench_shape}</g>'
              f'<g transform="translate({gx} {gy}) rotate({ang})"><path d="M-3 -90 V20" stroke="#cbd5e1" stroke-width="5" stroke-linecap="round"/></g>')
s.hand(gx, gy, s.back)
sparks = ""
hx, hy = rot(0, -112, ang, gx, gy)
for a in (-60, -15, 30):
    x1, y1 = hx + 46 * math.cos(math.radians(a - 90)), hy + 46 * math.sin(math.radians(a - 90))
    x2, y2 = hx + 62 * math.cos(math.radians(a - 90)), hy + 62 * math.sin(math.radians(a - 90))
    sparks += stroke(f"M{x1:.1f} {y1:.1f} L{x2:.1f} {y2:.1f}", 7, "url(#swap)")
render("wrench", "Swappie with a wrench", "Swappie, the PaperSwap mascot, holding up a wrench: core patches",
       s, FACES["focus"], defs=wrench_defs, extra_front=sparks)

# --- shield: safe by default ---------------------------------------------------------
s = Svg(); base(s)
s.line("M366 300 Q398 322 404 356", s.back); s.hand(406, 366, s.back)
s.line("M146 300 Q120 312 118 330", s.back)
sx, sy = 124, 344
shield_path = "M0 -58 L52 -42 V8 C52 40 28 60 0 70 C-28 60 -52 40 -52 8 V-42 Z"
shield = (f'<g transform="translate({sx} {sy})">'
          f'<path d="{shield_path}" fill="url(#swappie-shield-fill)" stroke="{DARK}" stroke-width="8" stroke-linejoin="round"/>'
          f'<path d="M-36 -32 L0 -44 V58 C-20 50 -38 34 -38 8 Z" fill="{WHITE}" opacity="0.18"/>'
          + stroke("M-22 6 L-6 22 L24 -12", 11, WHITE) + '</g>')
shield_defs = ('<linearGradient id="swappie-shield-fill" x1="0" y1="0" x2="0" y2="1">'
               '<stop offset="0" stop-color="#4ade80"/><stop offset="1" stop-color="#16a34a"/></linearGradient>')
s.outline.append(f'<g transform="translate({sx} {sy})"><path d="{shield_path}" fill="{WHITE}" stroke="{WHITE}" stroke-width="22" stroke-linejoin="round"/></g>')
render("shield", "Swappie with a shield", "Swappie, the PaperSwap mascot, holding a shield with a check mark: safe by default",
       s, FACES["confident"], defs=shield_defs, extra_front=shield)

# --- sleep: off by default ----------------------------------------------------------
s = Svg(); base(s)
s.line("M146 304 Q118 330 116 362", s.back); s.hand(116, 374, s.back)
s.line("M366 304 Q394 330 396 362", s.back); s.hand(396, 374, s.back)
cap = "M152 128 Q210 52 360 44 Q318 76 304 122 Q228 106 152 128 Z"
s.outline.append(f'<path d="{cap}" fill="{WHITE}" stroke="{WHITE}" stroke-width="22" stroke-linejoin="round"/>'
                 f'<circle cx="362" cy="44" r="28" fill="{WHITE}"/>')
nightcap = (f'<path d="{cap}" fill="#6366f1" stroke="{DARK}" stroke-width="8" stroke-linejoin="round"/>'
            f'<path d="M196 92 Q230 70 268 66" fill="none" stroke="{WHITE}" stroke-width="6" stroke-linecap="round" opacity="0.35"/>'
            f'<path d="M146 134 Q228 108 310 126" fill="none" stroke="{DARK}" stroke-width="30" stroke-linecap="round"/>'
            f'<path d="M146 134 Q228 108 310 126" fill="none" stroke="{PAPER}" stroke-width="16" stroke-linecap="round"/>'
            f'<circle cx="362" cy="44" r="16" fill="{PAPER}" stroke="{DARK}" stroke-width="8"/>')
zs = ""
for x, y, size in ((402, 168, 22), (430, 126, 30), (452, 74, 38)):
    zs += stroke(f"M{x} {y} h{size} l{-size} {size} h{size}", 7, "url(#swap)")
render("sleep", "Swappie sleeping", "Swappie, the PaperSwap mascot, asleep in a night cap: the runtime is off by default",
       s, FACES["sleep"], extra_front=nightcap + zs)

# --- sword: PvP -------------------------------------------------------------------
s = Svg(); base(s)
s.line("M146 300 Q114 322 108 356", s.back); s.hand(106, 366, s.back)
s.line("M366 296 Q412 290 418 240", s.back)
gx, gy, ang = 418, 226, 20
blade = "M-14 -14 V-150 L0 -176 L14 -150 V-14 Z"
sword_outline = (f'<g transform="translate({gx} {gy}) rotate({ang})" fill="{WHITE}" stroke="{WHITE}" stroke-width="20" stroke-linejoin="round">'
                 f'<path d="{blade}"/><rect x="-38" y="-16" width="76" height="16" rx="8"/><rect x="-9" y="-2" width="18" height="44" rx="6"/>'
                 f'<circle cx="0" cy="46" r="11"/></g>')
sword = (f'<g transform="translate({gx} {gy}) rotate({ang})" stroke="{DARK}" stroke-width="7" stroke-linejoin="round">'
         f'<rect x="-9" y="-2" width="18" height="44" rx="6" fill="#92400e"/>'
         f'<circle cx="0" cy="46" r="11" fill="#f59e0b"/>'
         f'<path d="{blade}" fill="#e2e8f0"/>'
         f'<path d="M0 -24 V-150" stroke="#94a3b8" stroke-width="5" stroke-linecap="round"/>'
         f'<rect x="-38" y="-16" width="76" height="16" rx="8" fill="#f59e0b"/></g>')
s.outline.append(sword_outline)
s.back.append(sword)
s.hand(gx, gy, s.back)
tip = rot(0, -150, ang, gx, gy)
shine = stroke(f"M{tip[0] + 22:.1f} {tip[1] - 6:.1f} l14 -4", 6, "url(#swap)") + stroke(f"M{tip[0] + 18:.1f} {tip[1] + 14:.1f} l14 6", 6, "url(#swap)")
render("sword", "Swappie with a sword", "Swappie, the PaperSwap mascot, with a raised sword and a determined look: PvP",
       s, FACES["determined"], extra_front=shine)

print("written:", sorted(p.name for p in OUT.glob("*.svg")))
