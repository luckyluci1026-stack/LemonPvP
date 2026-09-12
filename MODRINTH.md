# ⚡ BuckSMPAC — Fast Lag Free Anti Cheat

**Stop cheaters with real prediction — not guesswork.** BuckSMPAC simulates each
player's movement every tick and compares it to what their client actually
sends, catching hacks with very few false positives. Fully server-side, packet
driven, and built to stay light on your TPS.

---

## 🧠 Prediction-based detection

BuckSMPAC doesn't rely on flimsy thresholds. It runs a full movement-prediction
engine that recreates Minecraft physics server-side, so cheats that bend the
rules get caught precisely — Fly, Speed, NoFall, Timer, Reach, Hitbox,
KillAura/Aim, AutoClicker, Knockback, Scaffold/Place, NoSlow, Elytra exploits
and more.

## 🌈 Built-in gradient style (Farbverläufe)

Every alert, prefix and check name is rendered with fully configurable
MiniMessage gradients. Make BuckSMPAC match your server's colors in seconds — the
default is a clean yellow→green look. Hover over any alert to inspect the check,
violations and details.

## 🔨 Ban · Kick · Freeze — your call

A clean, documented punishment system. For every check group you decide what
happens and when:

- **Freeze** suspects (vanilla slowness/blindness, or your freeze plugin)
- **Kick** at a higher violation level
- **Ban** the obvious cheaters (vanilla or LiteBans/AdvancedBan, etc.)

Turn each punishment on or off by toggling a single config line. Plus Discord
webhook + proxy broadcast support out of the box.

## 🪶 Fast & lag free

Asynchronous, packet-based architecture keeps the impact on your server minimal,
even on busy networks. Folia and multi-version support included.

## 🌍 Localized

Ships with English and German messages and punishments — fully editable.

---

## 📥 Installation

1. Download `BuckSMPAC.jar`.
2. Drop it into your server's `plugins/` folder.
3. Restart, then tune `plugins/BuckSMPAC/` to taste.

**Compatibility:** Paper & forks • Folia supported • see version notes for
supported Minecraft versions.

---

<sub>Open source under GPLv3. Built on the GrimAC prediction engine — credit to the GrimAnticheat team (https://github.com/GrimAnticheat/Grim).</sub>
