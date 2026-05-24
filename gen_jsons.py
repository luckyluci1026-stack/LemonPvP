#!/usr/bin/env python3
"""Generate all resource pack JSON files."""
import json, os

BASE = "/home/user/LemonPvP/LemonPvP-ResourcePack"
MC_ITEMS = f"{BASE}/assets/minecraft/items"
LP_MODELS = f"{BASE}/assets/lemonpvp/models/item"

def wj(path, data):
    with open(path,'w') as f: json.dump(data,f,indent=2)
    print(f"  {os.path.basename(path)}")

def model(tex): return {"parent":"minecraft:item/generated","textures":{"layer0":f"lemonpvp:item/{tex}"}}

def select(fallback, cases):
    """Build minecraft:select model with custom_model_data cases."""
    return {"model":{"type":"minecraft:select","property":"minecraft:custom_model_data",
        "cases":[{"when":cmd,"model":{"type":"minecraft:model","model":f"lemonpvp:item/{mdl}"}} for cmd,mdl in cases],
        "fallback":{"type":"minecraft:model","model":f"minecraft:item/{fallback}"}}}

# ── 2D item models ────────────────────────────────────────────────────────────
for tex in ["kill_fire_swarm","kill_spook_swarm","kill_totem_explosion","kill_golden_gap",
            "trail_lemon","trail_emerald","trail_flame",
            "quest_easy","quest_medium","quest_hard","quest_expert",
            "stats_kills","stats_deaths","stats_elo","stats_coins",
            "duel_sword","duel_shield","duel_crystal","duel_mace","duel_uhc",
            "duel_smp","duel_spear_mace","duel_cart","duel_unavailable"]:
    wj(f"{LP_MODELS}/{tex}.json", model(tex))

# ── Minecraft item overrides ──────────────────────────────────────────────────
wj(f"{MC_ITEMS}/blaze_powder.json", select("blaze_powder",[
    (3001,"kill_fire_swarm"),(3002,"kill_spook_swarm"),
    (3003,"kill_totem_explosion"),(3004,"kill_golden_gap")]))

wj(f"{MC_ITEMS}/arrow.json", select("arrow",[
    (3005,"trail_lemon"),(3006,"trail_emerald"),(3007,"trail_flame")]))

wj(f"{MC_ITEMS}/paper.json", select("paper",[
    (4001,"quest_easy"),(4002,"quest_medium"),
    (4003,"quest_hard"),(4004,"quest_expert")]))

wj(f"{MC_ITEMS}/iron_sword.json", select("iron_sword",[
    (5001,"stats_kills"),(6001,"duel_sword")]))

wj(f"{MC_ITEMS}/skeleton_skull.json", select("skeleton_skull",[(5002,"stats_deaths")]))
wj(f"{MC_ITEMS}/nether_star.json",    select("nether_star",   [(5003,"stats_elo")]))
wj(f"{MC_ITEMS}/gold_nugget.json",    select("gold_nugget",   [(5004,"stats_coins")]))

wj(f"{MC_ITEMS}/shield.json",      select("shield",     [(6002,"duel_shield")]))
wj(f"{MC_ITEMS}/end_crystal.json", select("end_crystal",[(6003,"duel_crystal")]))
wj(f"{MC_ITEMS}/mace.json",        select("mace",       [(6004,"duel_mace"),(6007,"duel_spear_mace")]))
wj(f"{MC_ITEMS}/golden_apple.json",select("golden_apple",[(6005,"duel_uhc")]))
wj(f"{MC_ITEMS}/grass_block.json", select("grass_block", [(6006,"duel_smp")]))
wj(f"{MC_ITEMS}/minecart.json",    select("minecart",   [(6008,"duel_cart")]))
wj(f"{MC_ITEMS}/barrier.json",     select("barrier",    [(6009,"duel_unavailable")]))

# ── Font definitions ──────────────────────────────────────────────────────────
FONT_DIR = f"{BASE}/assets/lemonpvp/font"
os.makedirs(FONT_DIR, exist_ok=True)

# Small caps font (lemonpvp:default)
wj(f"{FONT_DIR}/default.json",{"providers":[{
    "type":"bitmap","file":"lemonpvp:font/lemon_caps.png",
    "height":7,"ascent":6,"chars":["abcdefghijklmnopqrstuvwxyz"]}]})

# Tag emoji font (lemonpvp:tags) — 13 chars ..
wj(f"{FONT_DIR}/tags.json",{"providers":[{
    "type":"bitmap","file":"lemonpvp:font/lemon_tags.png",
    "height":16,"ascent":14,
    "chars":[""]}]})

# minecraft:default font override (keeps vanilla, adds our small caps + tag emojis)
MC_FONT_DIR = f"{BASE}/assets/minecraft/font"
os.makedirs(MC_FONT_DIR, exist_ok=True)
wj(f"{MC_FONT_DIR}/default.json",{"providers":[
    {"type":"reference","id":"minecraft:include/space"},
    {"type":"reference","id":"minecraft:include/default"},
    {"type":"reference","id":"minecraft:include/unifont"},
    {"type":"bitmap","file":"lemonpvp:font/lemon_caps.png","height":7,"ascent":6,
     "chars":["abcdefghijklmnopqrstuvwxyz"]},
    {"type":"bitmap","file":"lemonpvp:font/lemon_tags.png","height":16,"ascent":14,
     "chars":[""]}
]})

print("Done!")
