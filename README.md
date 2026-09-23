<div align="center">
 <h1>⚡ BuckSMPAC — Fast Lag Free Anti Cheat</h1>
 <p><i>A prediction-based anticheat for Paper, with custom features and gradient styling — built for LemonPvP.</i></p>
</div>

---

BuckSMPAC is a high-accuracy, fully server-side anticheat. It keeps a complete
movement-prediction engine (the same approach the best anticheats use to
simulate the player's physics every tick) and layers LemonPvP's own branding,
gradient interface and a detailed, ready-to-use punishment system on top.

## ✨ What's inside

- 🧠 **Real prediction engine** — movement is simulated and compared against
  what the client claims, instead of relying on guessy thresholds.
- 🌈 **Gradient styling (Farbverläufe)** — the prefix, alerts and check names use
  configurable MiniMessage gradients. Default is the LemonPvP `#fffb00 → #00ff00`
  look, set in `common/src/main/resources/messages/`.
- 🔨 **Detailed Ban / Kick / Freeze config** — every check group has a documented
  punishment ladder in `common/src/main/resources/punishments/`. Turn a
  punishment on/off by (un)commenting a single line. Includes vanilla freeze
  (slowness/blindness), freeze-plugin and ban-plugin examples.
- 🪶 **Lag friendly** — async, packet-driven design.
- 🌍 **Localized** — English and German messages/punishments rebranded to BuckSMPAC.
- 🧩 **Folia + multi-version support** inherited from the upstream engine.

## 🛠️ Building

This is a Gradle project.

```bash
./gradlew :bukkit:build
```

The plugin jar is produced under `bukkit/build/libs/`. Building requires a JDK
that satisfies the toolchain in `bukkit/build.gradle.kts` and network access to
the PacketEvents / Paper / Grim Maven repositories declared in the build files.

> ℹ️ Note: this repository contains the full engine source. Internal package
> names (`ac.grim.grimac.*`), permissions (`grim.*`) and the in-game command
> (`/grim`) are intentionally left as-is so the engine keeps working; the
> user-facing identity (plugin name **BuckSMPAC**, prefix, alerts, punishments) is
> what has been rebranded.

## ⚙️ Configuration

After the first start, files appear in `plugins/BuckSMPAC/`:

- `messages/<lang>.yml` — branding, gradients, alert format
- `punishments/<lang>.yml` — the Ban / Kick / Freeze ladder
- `config/<lang>.yml` — engine settings & check toggles

Run `/grim reload` after editing.

---

## 📜 License & Credits

BuckSMPAC is a fork of **GrimAC** and is therefore licensed under the
**GNU General Public License v3 (GPLv3)** — see [`LICENSE`](LICENSE). The full
corresponding source is included in this repository, as the license requires.

**Original project / prediction engine:** GrimAC by the GrimAnticheat team —
<https://github.com/GrimAnticheat/Grim>. All credit for the underlying engine
goes to them. BuckSMPAC only adds branding, gradient styling and configuration on
top.
