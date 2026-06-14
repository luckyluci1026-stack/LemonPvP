# FLFAC — Fast Lag Free Anti Cheat

> A GrimAC-inspired, event-driven anticheat for **Paper** with a custom check
> suite and signature **gradient styling** (Farbverläufe). Built for LemonPvP.

FLFAC takes the spirit of GrimAC — a serious, server-side anticheat — and ships
it as a lightweight, self-contained Paper plugin with extra quality-of-life
features and a unique look. No ProtocolLib, no external dependencies: it hooks
straight into Bukkit/Paper events so it stays **fast and lag free**.

---

## ✨ Features

### Custom features on top of "just another AC"
- **Gradient everything (Farbverläufe).** Prefix, player names, check names and
  accents are all rendered with configurable MiniMessage gradients. Default is
  the LemonPvP yellow→green brand (`#fffb00 → #00ff00`).
- **Lag-free guard.** Heavy checks are automatically skipped while the server
  TPS is below a configurable threshold, so the AC never makes lag worse.
- **Lag compensation.** Ping is factored into combat checks (reach) to crush
  false positives for high-ping players.
- **Animated gradient console banner** on startup.
- **Live alert & verbose system** with hover details and click-to-inspect.
- **Per-check punishment ladders** (global or per-check) run from console.
- **Violation decay** so legit players are never punished for old noise.
- **Rich `/flfac` command** with tab completion and a player violation profile.

### Checks
| Category | Checks |
|----------|--------|
| Combat   | Reach, Hitbox, KillAura, AutoClicker, Velocity (anti-knockback) |
| Movement | Speed, Fly, NoFall, Jesus (water-walk), Timer |
| World    | Scaffold, Nuker, FastBreak |
| Player   | BadPackets |

Every check is individually toggleable and tunable in `config.yml`
(thresholds, alert level, max violation level, punishment ladder).

---

## 🚀 Commands & Permissions

```
/flfac                  → help
/flfac alerts           → toggle cheat alerts        (flfac.alerts)
/flfac verbose          → toggle verbose output      (flfac.verbose)
/flfac info <player>    → show a player's violations (flfac.command)
/flfac checks           → list all checks + state     (flfac.command)
/flfac reset <player>   → reset violations            (flfac.admin)
/flfac reload           → reload config               (flfac.admin)
```
Aliases: `/fac`, `/ac`, `/anticheat`.

| Permission       | Purpose                              | Default |
|------------------|--------------------------------------|---------|
| `flfac.command`  | Use `/flfac`                         | op      |
| `flfac.alerts`   | Receive cheat alerts                 | op      |
| `flfac.verbose`  | Receive verbose debug output         | op      |
| `flfac.admin`    | Reload / reset                       | op      |
| `flfac.bypass`   | Bypass **all** checks                | false   |
| `flfac.*`        | Everything                           | op      |

---

## 🛠️ Building

Requires **JDK 21** and Maven. FLFAC builds against the Paper API, so your build
machine needs access to `https://repo.papermc.io`.

```bash
mvn clean package
```

The compiled plugin lands in `target/FLFAC-1.0.0.jar`. Drop it into your
server's `plugins/` folder (Paper **1.20.x – 1.21.x**, Java 21) and restart.

---

## ⚙️ Configuration

All colors accept hex values and are combined into MiniMessage gradients.
The most important knobs:

```yaml
branding:
  prefix-gradient: ["#fffb00", "#aaff00", "#00ff00"]  # FLFAC brand
performance:
  min-tps: 16.0          # skip heavy checks below this TPS
  lag-compensation: true # add ping into reach etc.
checks:
  reach:
    enabled: true
    max-reach: 3.05
    alert-vl: 4
```

See `src/main/resources/config.yml` for the fully documented defaults.

---

## ⚠️ Notes & honest scope

FLFAC is **inspired by** GrimAC but is an independent implementation. It does
**not** replicate GrimAC's full client-prediction engine; instead it uses
robust, well-tuned heuristics driven by Bukkit/Paper events. Thresholds default
to conservative values to keep false positives low — tune them for your server.

Made with ⚡ by **LemonPvP**.
