# Changelog

All notable changes to PaperSwap are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). PaperSwap has no tagged release yet;
everything below is unreleased and can be built from source or taken from CI artifacts.

## [Unreleased]

### Changed

- **Renamed the project from "Lemon" to "PaperSwap"**: server brand and JAR names
  (`paperswap-<mc>.jar`), Java packages `dev.paperswap.*`, `config/paperswap.yml`, data folder
  `paperswap/`, agent `paperswap-agent.jar`, patch files `.swappatch`, permissions `paperswap.*`,
  Gradle projects `:paperswap-server` / `:paperswap-api`, new command alias `/paperswap`.
  Servers set up before the rename keep working: the old configuration and data folder are used
  (with a warning in the log and in `/fork status`) until they are renamed, `lemon-module.yml`
  descriptors are still recognised, and a server started with the old agent JAR is detected.
  Modules must be rebuilt against the new API package; permissions must be renamed.
  See [`docs/MIGRATION.md`](docs/MIGRATION.md).
- Open-source repository setup: English README (German: `README.de.md`), logo and banner,
  contributing guide, code of conduct, security policy, issue and pull request templates,
  `.editorconfig`.

### Added

- **JVM instrumentation** (`/fork instrumentation`): the agent can be loaded at start-up
  (`-javaagent`) or attached to a running server (`java -jar paperswap-agent.jar attach <pid>`);
  start-up diagnostics (agent, JVM capabilities, warning for a JDWP port that is not bound to
  localhost), class search over all loaded classes, class inspection, and core patches by
  controlled retransformation as an alternative to redefinition. On JVMs with
  `-XX:+AllowEnhancedClassRedefinition` retransformation patches are refused, because a
  JetBrains Runtime crash was reproduced in CI (`docs/INSTRUMENTATION.md`).
- **LemonPvP suite**: four example modules (stats, combat tag, killstreaks, scoreboard).
- **Test modules** that push the module runtime to its limits (stress, faults, dependencies,
  leaks) and server test 8 that runs them on every version.
- **Live updates** (`/fork live`): core patches built from changed Paper/Minecraft sources
  (`livePatchBaseline`, `createLivePatch`, `checkLivePatch`) and applied through the agent with
  plan, confirmation, read-back verification of the running bytecode and rollback; live plugin
  updates with compatibility check, resource report, leak check and restore of the previous JAR;
  `capabilities`, `diagnostics`, `inspect`, `update all`.
- **1.21.11**: attack cooldown reset as in vanilla for spear jabs, also tested as a live core
  patch (`docs/SPEAR-COOLDOWN.md`).
- **Hot-deployment runtime**: modules with their own class loaders, lifecycle, hot swap with
  rollback, dependencies, integrity checks (SHA-256 sidecar, allow-list, staged copies), audit log,
  `/fork` command and an example module.
- **Paperweight fork projects** for Minecraft 1.21.11, 26.1.2, 26.2 and 26.3 with CI that builds
  every version, runs Paper's own test suite and starts real servers for the server tests.

### Fixed

- Module tasks: the Bukkit scheduler no longer keeps unloaded modules alive.
- Core patches for 1.21.11, whose Paperclip writes a server JAR with a different class file
  layout: patches are built against the JAR that actually runs.
- Reading back the running bytecode never breaks an already applied patch.
