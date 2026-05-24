#!/usr/bin/env python3
"""Generate all LemonPvP resource pack PNG textures."""
import os
from PIL import Image, ImageDraw

BASE = "/home/user/LemonPvP/LemonPvP-ResourcePack"
IT = f"{BASE}/assets/lemonpvp/textures/item"
FT = f"{BASE}/assets/lemonpvp/textures/font"
for d in [IT, FT]: os.makedirs(d, exist_ok=True)

T = (0,0,0,0)
W = (255,255,255,255)
BK = (20,20,20,255)
DARK = (18,14,24,255)
OR = (255,120,20,255)
RD = (220,40,40,255)
DRD = (160,0,0,255)
YL = (255,251,0,255)
GD = (255,215,0,255)
DGD = (180,130,0,255)
GR = (0,200,50,255)
DGR = (0,100,20,255)
LGR = (0,255,68,255)
BL = (30,80,220,255)
LBL = (100,180,255,255)
CY = (0,220,220,255)
PU = (150,0,220,255)
PK = (220,50,180,255)
GY = (130,130,130,255)
LGY = (200,200,200,255)
BE = (220,195,150,255)
BR = (140,90,40,255)

def n(): return Image.new("RGBA",(16,16),T)
def bg(c): i=n(); ImageDraw.Draw(i).rectangle([0,0,15,15],fill=c); return i
def p(i,x,y,c):
    if 0<=x<i.width and 0<=y<i.height: i.putpixel((x,y),c)
def fill(i,x0,y0,x1,y1,c): ImageDraw.Draw(i).rectangle([x0,y0,x1,y1],fill=c)
def s(i,path): i.save(path); print(f"  {os.path.basename(path)}")

# ── PACK ICON 64×64 ──────────────────────────────────────────────────────────
pk = Image.new("RGBA",(64,64),(0,140,30,255))
d = ImageDraw.Draw(pk)
d.rectangle([8,10,14,50],fill=GD)          # L vertical
d.rectangle([8,44,28,50],fill=GD)          # L base
d.rectangle([32,10,38,50],fill=GD)         # P vertical
d.rectangle([32,10,52,16],fill=GD)         # P top
d.rectangle([32,28,52,34],fill=GD)         # P mid
d.arc([44,10,58,34],0,360,fill=GD,width=4) # P arc
s(pk, f"{BASE}/pack.png")

# ── SMALL CAPS FONT 156×7 ────────────────────────────────────────────────────
GL = {
    'a':["01110","10001","11111","10001","10001","00000","00000"],
    'b':["11110","10001","11110","10001","11110","00000","00000"],
    'c':["01111","10000","10000","10000","01111","00000","00000"],
    'd':["11100","10010","10001","10010","11100","00000","00000"],
    'e':["11111","10000","11110","10000","11111","00000","00000"],
    'f':["11111","10000","11100","10000","10000","00000","00000"],
    'g':["01111","10000","10011","10001","01111","00000","00000"],
    'h':["10001","10001","11111","10001","10001","00000","00000"],
    'i':["11111","00100","00100","00100","11111","00000","00000"],
    'j':["00111","00010","00010","10010","01100","00000","00000"],
    'k':["10010","10100","11000","10100","10010","00000","00000"],
    'l':["10000","10000","10000","10000","11111","00000","00000"],
    'm':["10001","11011","10101","10001","10001","00000","00000"],
    'n':["10001","11001","10101","10011","10001","00000","00000"],
    'o':["01110","10001","10001","10001","01110","00000","00000"],
    'p':["11110","10001","11110","10000","10000","00000","00000"],
    'q':["01110","10001","10001","10011","01111","00000","00000"],
    'r':["11110","10001","11110","10010","10001","00000","00000"],
    's':["01111","10000","01110","00001","11110","00000","00000"],
    't':["11111","00100","00100","00100","00100","00000","00000"],
    'u':["10001","10001","10001","10001","01110","00000","00000"],
    'v':["10001","10001","10001","01010","00100","00000","00000"],
    'w':["10001","10001","10101","11011","10001","00000","00000"],
    'x':["10001","01010","00100","01010","10001","00000","00000"],
    'y':["10001","10001","01111","00001","11110","00000","00000"],
    'z':["11111","00010","00100","01000","11111","00000","00000"],
}
fi = Image.new("RGBA",(156,7),T)
for i,ch in enumerate("abcdefghijklmnopqrstuvwxyz"):
    xb=i*6
    for ry,row in enumerate(GL[ch]):
        if ry>=7: break
        for cx,bit in enumerate(row):
            if bit=='1': fi.putpixel((xb+cx,ry),W)
s(fi, f"{FT}/lemon_caps.png")

# ── KILL EFFECTS (blaze_powder, 16×16) ───────────────────────────────────────
# 3001 Fire Swarm
i=bg(DARK); d=ImageDraw.Draw(i)
d.ellipse([4,3,12,11],fill=OR); d.ellipse([6,5,10,9],fill=(255,200,0,255))
for x,y,c in [(3,7,RD),(13,7,OR),(8,2,RD),(8,13,OR),(3,3,OR),(13,3,RD),(3,12,RD),(13,12,OR)]:
    p(i,x,y,c)
p(i,8,7,(255,255,120,255))
s(i, f"{IT}/kill_fire_swarm.png")

# 3002 Spook Swarm
i=bg(DARK); d=ImageDraw.Draw(i)
d.polygon([(8,2),(11,4),(11,9),(9,11),(7,11),(5,9),(5,4)],fill=CY)
p(i,7,6,DARK);p(i,9,6,DARK);p(i,7,7,DARK);p(i,9,7,DARK)
for x,y in [(2,2),(13,3),(1,11),(14,12),(8,14)]:p(i,x,y,CY)
s(i, f"{IT}/kill_spook_swarm.png")

# 3003 Totem Explosion
i=bg(DARK); d=ImageDraw.Draw(i)
for pts in [[(8,8),(8,1)],[(8,8),(8,15)],[(8,8),(1,8)],[(8,8),(15,8)],
            [(8,8),(3,3)],[(8,8),(13,3)],[(8,8),(3,13)],[(8,8),(13,13)]]:
    d.line(pts,fill=GD,width=1)
d.ellipse([5,5,11,11],fill=DGD)
p(i,7,7,GR);p(i,9,7,GR);p(i,7,9,GR);p(i,9,9,GR)
s(i, f"{IT}/kill_totem_explosion.png")

# 3004 Golden Gap
i=bg(DARK); d=ImageDraw.Draw(i)
d.ellipse([3,4,13,14],fill=DGD); d.ellipse([4,5,12,13],fill=(200,160,20,255))
d.line([(8,2),(8,4)],fill=GR,width=1);p(i,7,3,GR);p(i,9,3,GR)
p(i,5,7,W);p(i,6,7,(255,240,150,255))
s(i, f"{IT}/kill_golden_gap.png")

# ── ARROW TRAILS (arrow, 16×16) ───────────────────────────────────────────────
def arrow_base(c1,c2):
    i=n(); d=ImageDraw.Draw(i)
    d.line([(2,13),(11,4)],fill=LGY,width=1)
    d.polygon([(11,4),(14,2),(12,5)],fill=LGY)
    for k in range(3): p(i,3+k,13-k,(150,120,80,255))
    for x,y in [(4,12),(6,10),(8,8),(10,6),(12,4)]:
        p(i,x-2,y,c1); p(i,x,y+2,c2)
    return i

s(arrow_base(YL,YL), f"{IT}/trail_lemon.png")
s(arrow_base(LGR,LGR), f"{IT}/trail_emerald.png")
s(arrow_base(OR,RD), f"{IT}/trail_flame.png")

# ── QUEST TEXTURES (paper, 16×16) ────────────────────────────────────────────
def paper(bgc=(245,240,230,255)):
    i=n(); d=ImageDraw.Draw(i)
    d.rectangle([2,1,13,14],fill=bgc,outline=(180,170,150,255),width=1)
    d.polygon([(11,1),(13,3),(11,3)],fill=(200,190,170,255))
    return i,d

# 4001 Easy
i,d=paper(); ck=(0,180,40,255)
d.line([(5,9),(7,12)],fill=ck,width=2); d.line([(7,12),(11,5)],fill=ck,width=2)
s(i,f"{IT}/quest_easy.png")

# 4002 Medium
i,d=paper()
d.polygon([(7,4),(8,7),(11,7),(9,9),(10,12),(7,10),(4,12),(5,9),(3,7),(6,7)],fill=(220,200,0,255))
s(i,f"{IT}/quest_medium.png")

# 4003 Hard
i,d=paper()
d.rectangle([7,4,8,10],fill=(220,120,0,255)); p(i,7,12,(220,120,0,255));p(i,8,12,(220,120,0,255))
s(i,f"{IT}/quest_hard.png")

# 4004 Expert
i,d=paper((40,30,50,255))
d.ellipse([4,4,11,11],fill=(200,50,50,255))
for x,y in [(6,7),(6,8),(9,7),(9,8),(7,9),(8,9)]: p(i,x,y,BK)
d.rectangle([4,11,11,13],fill=(180,40,40,255))
for x in [5,7,9,11]: p(i,x,12,BK)
s(i,f"{IT}/quest_expert.png")

# ── STATS TEXTURES (16×16) ────────────────────────────────────────────────────
# 5001 Kills
i=bg((20,30,50,255)); d=ImageDraw.Draw(i)
d.line([(12,2),(4,10)],fill=LGY,width=2)
d.line([(7,7),(5,9)],fill=(100,80,50,255),width=2)
d.line([(4,10),(2,12)],fill=BR,width=1)
for x,y in [(11,10),(12,11),(10,11),(11,11),(11,12)]: p(i,x,y,RD)
s(i,f"{IT}/stats_kills.png")

# 5002 Deaths
i=bg((30,20,30,255)); d=ImageDraw.Draw(i)
d.ellipse([3,2,13,12],fill=(180,160,160,255))
for x,y in [(5,6),(5,7),(6,7),(9,6),(9,7),(10,7)]: p(i,x,y,RD)
p(i,7,9,BK);p(i,8,9,BK)
d.rectangle([4,12,12,14],fill=(160,140,140,255))
for x in [5,7,9,11]: p(i,x,13,BK)
s(i,f"{IT}/stats_deaths.png")

# 5003 ELO
i=bg((10,10,30,255)); d=ImageDraw.Draw(i)
d.polygon([(8,1),(9,5),(12,2),(10,6),(14,7),(10,9),(12,13),(9,10),(8,14),(7,10),(4,13),(6,9),(2,7),(6,6),(4,2),(7,5)],fill=GD)
d.ellipse([5,5,11,11],fill=(255,200,50,255))
for x,y in [(6,7),(7,7),(9,7),(10,7)]: p(i,x,y,BL)
p(i,8,8,W)
s(i,f"{IT}/stats_elo.png")

# 5004 Coins
i=bg((40,30,10,255)); d=ImageDraw.Draw(i)
d.ellipse([2,2,14,14],fill=GD); d.ellipse([3,3,13,13],fill=(230,180,0,255))
lc=(150,100,0,255)
d.rectangle([6,5,8,12],fill=lc); d.rectangle([6,10,11,12],fill=lc)
s(i,f"{IT}/stats_coins.png")

# ── DUEL TEXTURES (16×16) ─────────────────────────────────────────────────────
# 6001 Sword
i=bg((20,40,100,255)); d=ImageDraw.Draw(i)
d.line([(12,2),(4,10)],fill=W,width=2)
d.line([(7,7),(5,9)],fill=(180,140,80,255),width=2)
d.line([(4,10),(2,12)],fill=BR,width=1)
s(i,f"{IT}/duel_sword.png")

# 6002 Shield
i=bg((50,30,20,255)); d=ImageDraw.Draw(i)
d.polygon([(3,2),(13,2),(13,10),(8,15),(3,10)],fill=(100,70,40,255))
d.polygon([(4,3),(12,3),(12,10),(8,14),(4,10)],fill=(130,90,50,255))
d.rectangle([6,5,8,12],fill=GD); d.rectangle([6,10,11,12],fill=GD)
s(i,f"{IT}/duel_shield.png")

# 6003 Crystal
i=bg((10,5,20,255)); d=ImageDraw.Draw(i)
d.polygon([(8,1),(13,6),(10,14),(6,14),(3,6)],fill=PU)
d.polygon([(8,2),(12,6),(9,13),(7,13),(4,6)],fill=PK)
p(i,6,5,W);p(i,7,4,(255,200,255,255))
s(i,f"{IT}/duel_crystal.png")

# 6004 Mace
i=bg((30,20,10,255)); d=ImageDraw.Draw(i)
d.line([(8,14),(8,10)],fill=BR,width=2)
d.ellipse([4,4,12,12],fill=(100,80,60,255))
d.polygon([(8,1),(9,5),(7,5)],fill=GD)
d.polygon([(15,8),(11,7),(11,9)],fill=GD)
d.polygon([(1,8),(5,7),(5,9)],fill=GD)
d.polygon([(8,15),(7,11),(9,11)],fill=GD)
s(i,f"{IT}/duel_mace.png")

# 6005 UHC
i=bg((30,15,5,255)); d=ImageDraw.Draw(i)
d.ellipse([2,4,14,14],fill=(180,130,0,255)); d.ellipse([3,5,13,13],fill=(220,160,10,255))
d.line([(8,2),(8,4)],fill=GR,width=1)
hc=(220,30,30,255)
for x,y in [(7,7),(8,7),(6,8),(7,8),(8,8),(9,8),(6,9),(7,9),(8,9),(9,9),(7,10),(8,10),(8,11)]:
    p(i,x,y,hc)
s(i,f"{IT}/duel_uhc.png")

# 6006 SMP
i=n(); d=ImageDraw.Draw(i)
d.rectangle([1,5,14,14],fill=(130,80,40,255))
d.rectangle([1,5,14,7],fill=(60,160,30,255))
for x,y in [(11,5),(10,6),(9,7),(9,8)]: p(i,x,y,LGY)
p(i,9,8,BR)
s(i,f"{IT}/duel_smp.png")

# 6007 Spear Mace
i=bg((25,15,10,255)); d=ImageDraw.Draw(i)
d.line([(8,14),(8,10)],fill=BR,width=2)
d.ellipse([4,5,12,11],fill=(100,80,60,255))
d.polygon([(8,1),(6,5),(10,5)],fill=GY)
d.line([(8,1),(8,5)],fill=LGY,width=1)
s(i,f"{IT}/duel_spear_mace.png")

# 6008 Cart
i=bg((30,20,10,255)); d=ImageDraw.Draw(i)
d.rectangle([2,5,14,11],fill=(100,90,80,255))
d.rectangle([3,6,13,10],fill=(70,65,60,255))
d.ellipse([2,10,6,14],fill=(60,55,50,255))
d.ellipse([10,10,14,14],fill=(60,55,50,255))
d.line([(10,7),(6,10)],fill=LGY,width=1)
p(i,8,8,(150,120,80,255))
s(i,f"{IT}/duel_cart.png")

# 6009 Unavailable
i=bg((20,20,25,255)); d=ImageDraw.Draw(i)
d.line([(3,3),(13,13)],fill=(120,120,120,255),width=2)
d.line([(13,3),(3,13)],fill=(120,120,120,255),width=2)
s(i,f"{IT}/duel_unavailable.png")

# ── TAG EMOJI SPRITE SHEET 208×16 (13 emojis) ────────────────────────────────
def em(): return Image.new("RGBA",(16,16),T)
tags=Image.new("RGBA",(208,16),T)

def tag(idx,fn):
    e=fn()
    tags.paste(e,(idx*16,0))

# E001 Developing: monitor + cursor
def mk_developing():
    e=em(); d=ImageDraw.Draw(e)
    d.rectangle([1,2,14,11],outline=CY,width=1)
    d.rectangle([2,3,13,10],fill=(20,40,60,255))
    d.polygon([(6,5),(6,9),(10,7)],fill=CY)
    d.rectangle([5,11,10,12],fill=CY); d.rectangle([3,12,12,13],fill=CY)
    return e
tag(0, mk_developing)

# E002 Money: banknote with $
def mk_money():
    e=em(); d=ImageDraw.Draw(e)
    nc=(0,160,80,255)
    d.rectangle([1,4,14,11],fill=nc,outline=(0,100,40,255),width=1)
    # $ symbol
    for y in [5,7,9]: p(e,7,y,W);p(e,8,y,W)
    p(e,8,4,W);p(e,8,10,W)
    for x in [6,9]: p(e,x,6,W);p(e,x,8,W)
    return e
tag(1, mk_money)

# E003 Winner: trophy
def mk_winner():
    e=em(); d=ImageDraw.Draw(e)
    d.ellipse([3,2,12,9],fill=GD)
    d.rectangle([5,8,10,12],fill=GD)
    d.rectangle([3,12,12,14],fill=GD)
    p(e,7,4,W);p(e,8,4,W);p(e,7,5,W);p(e,8,5,W)
    p(e,6,5,W);p(e,9,5,W)
    return e
tag(2, mk_winner)

# E004 Veteran: shield+sword
def mk_veteran():
    e=em(); d=ImageDraw.Draw(e)
    d.polygon([(3,2),(12,2),(12,9),(7,14),(3,9)],fill=GY)
    d.polygon([(4,3),(11,3),(11,9),(7,13),(4,9)],fill=LGY)
    d.line([(7,4),(7,11)],fill=BK,width=1)
    d.line([(5,6),(9,6)],fill=BK,width=1)
    return e
tag(3, mk_veteran)

# E005 Lemon
def mk_lemon():
    e=em(); d=ImageDraw.Draw(e)
    d.ellipse([3,4,13,13],fill=YL); d.ellipse([4,5,12,12],fill=(255,235,0,255))
    d.ellipse([7,2,12,6],fill=DGR)
    p(e,8,3,GR);p(e,9,3,GR)
    return e
tag(4, mk_lemon)

# E006 Elite: crown
def mk_elite():
    e=em(); d=ImageDraw.Draw(e)
    d.rectangle([2,8,13,12],fill=GD)
    d.polygon([(2,8),(2,4),(5,7),(7,2),(9,7),(12,4),(13,8)],fill=GD)
    d.rectangle([2,12,13,14],fill=DGD)
    p(e,7,4,RD);p(e,7,5,RD)
    return e
tag(5, mk_elite)

# E007 Ghost
def mk_ghost():
    e=em(); d=ImageDraw.Draw(e)
    gc=(240,240,250,255)
    d.ellipse([4,1,12,9],fill=gc)
    d.rectangle([4,6,12,13],fill=gc)
    for x in [4,6,8,10,12]: p(e,x,14,gc)
    p(e,6,5,BK);p(e,6,6,BK);p(e,9,5,BK);p(e,9,6,BK)
    return e
tag(6, mk_ghost)

# E008 Toxic: poison bottle
def mk_toxic():
    e=em(); d=ImageDraw.Draw(e)
    bc=(0,180,40,255)
    d.rectangle([5,5,11,14],fill=bc)
    d.rectangle([6,3,10,5],fill=BR)
    d.rectangle([6,2,10,3],fill=bc)
    p(e,7,7,BK);p(e,9,7,BK)
    p(e,7,9,BK);p(e,8,9,BK);p(e,9,9,BK)
    return e
tag(7, mk_toxic)

# E009 Nova: 8-pointed star
def mk_nova():
    e=em(); d=ImageDraw.Draw(e)
    d.polygon([(8,1),(9,5),(12,2),(10,6),(14,8),(10,9),(12,13),(9,11),(8,15),(7,11),(4,13),(6,9),(2,8),(6,6),(4,2),(7,5)],fill=PU)
    d.ellipse([5,5,11,11],fill=PK)
    p(e,8,8,W)
    return e
tag(8, mk_nova)

# E00A Frost: snowflake
def mk_frost():
    e=em(); d=ImageDraw.Draw(e)
    fc=(170,240,255,255)
    d.line([(8,1),(8,15)],fill=fc,width=1)
    d.line([(1,8),(15,8)],fill=fc,width=1)
    d.line([(3,3),(13,13)],fill=fc,width=1)
    d.line([(13,3),(3,13)],fill=fc,width=1)
    for x,y in [(6,3),(10,3),(6,13),(10,13),(3,6),(13,6),(3,10),(13,10)]: p(e,x,y,fc)
    p(e,8,8,W)
    return e
tag(9, mk_frost)

# E00B Inferno: flame
def mk_inferno():
    e=em(); d=ImageDraw.Draw(e)
    d.polygon([(8,1),(11,5),(13,10),(10,14),(6,14),(3,10),(5,5)],fill=RD)
    d.polygon([(8,3),(10,7),(11,11),(8,13),(5,11),(6,7)],fill=OR)
    d.polygon([(8,6),(9,9),(8,12),(7,9)],fill=YL)
    return e
tag(10, mk_inferno)

# E00C Cosmic: planet with ring
def mk_cosmic():
    e=em(); d=ImageDraw.Draw(e)
    d.ellipse([4,3,12,11],fill=(30,30,150,255))
    d.arc([1,5,15,13],start=0,end=180,fill=PU,width=2)
    d.arc([2,6,14,12],start=180,end=360,fill=(100,0,200,180),width=2)
    p(e,6,6,LBL);p(e,8,5,(80,80,200,255))
    return e
tag(11, mk_cosmic)

# E00D Staff: hammer
def mk_staff():
    e=em(); d=ImageDraw.Draw(e)
    d.line([(4,12),(12,4)],fill=BR,width=2)
    d.rectangle([9,2,14,7],fill=GY)
    d.rectangle([9,3,13,6],fill=LGY)
    return e
tag(12, mk_staff)

s(tags, f"{FT}/lemon_tags.png")
print("Done! All textures generated.")
