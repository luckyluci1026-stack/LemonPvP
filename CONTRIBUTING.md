# Contributing to PaperSwap

Thanks for wanting to help! Bug reports, test results from your server, documentation,
translations and code are all welcome. Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

> Deutschsprachige Beiträge sind genauso willkommen – Issues und Pull Requests dürfen auf Deutsch
> oder Englisch geschrieben werden.

## Ground rules

1. **Plain Paper stays plain Paper.** Change Paper or Minecraft code only when there is no
   technical alternative, and then as the smallest possible, documented patch
   (see [`docs/UPSTREAM-SYNC.md`](docs/UPSTREAM-SYNC.md), "Warum der CraftServer-Patch nötig ist").
2. **No background activity with `enabled: false`.** Every runtime feature must respect this
   (`BootstrapTest`).
3. **Never claim what was not checked.** Documentation, commit messages and PR descriptions
   distinguish tested from untested statements, and name the test level (see below).
4. **A failed operation is never reported as success.** Partial results are reported as partial.
5. **Dangerous operations stay opt-in.** Anything that loads or changes code needs a config switch,
   a permission that defaults to nobody and an explicit `confirm`.
6. **No secrets** (keys, tokens, passwords) in the repository or in CI, and no network ports or
   upload endpoints opened by PaperSwap.

## Reporting bugs and requesting features

- Use the [issue forms](https://github.com/luckyluci1026-stack/LemonPvP/issues/new/choose). For bugs, include the Minecraft version, the
  PaperSwap commit or build, the JVM (`java -version`), JVM flags and the output of
  `fork version` and `fork live diagnostics`.
- Remove IP addresses, player data and anything secret from logs before posting.
- Bugs that also happen on plain Paper belong to [PaperMC](https://github.com/PaperMC/Paper/issues).
- **Security issues:** please do not open a public issue – see [`SECURITY.md`](SECURITY.md).

## Project layout

| Path | Contents |
|---|---|
| `runtime/runtime-api` | public module API (platform-neutral) |
| `runtime/runtime-core` | runtime core + tests (builds without Paper) |
| `runtime/runtime-paper-api` | Paper-specific module API |
| `runtime/runtime-paper` | Paper adapter (compiled into the server) |
| `runtime/livepatch-agent` | `paperswap-agent.jar` (`-javaagent` / `attach <pid>`) |
| `runtime/example-module`, `runtime/test-modules`, `runtime/pvp-suite` | example, test and PvP modules |
| `runtime/example-plugin`, `runtime/combat-probe` | plugins used by the server tests |
| `versions/<mc>/` | paperweight fork project per Minecraft version |
| `scripts/fork/` | generators for version projects and patches |
| `scripts/livepatch/` | Gradle tasks for core patches |
| `scripts/ci/` | live server tests and CI helpers |
| `docs/` | documentation |

## Development workflow

```bash
cd runtime && ./gradlew build                     # core + all runtime tests (JDK 21+)
cd runtime && ./gradlew :runtime-paper:compileJava :example-module:assemble \
                        -PpaperApiVersion=26.2.build.+ -PjavaRelease=25
cd versions/26.2 && ./gradlew applyAllPatches build createPaperclipJar
```

- Runtime code must compile with **Java 21** (for 1.21.11); the core's production code is
  compiled with `-Xlint:all -Werror`.
- The adapter may only use Paper API that exists in **all** supported versions (Adventure 4.26
  **and** 5.2). CI compiles it against every version.
- New lifecycle or security logic needs tests in `runtime-core` (integration tests with real
  module JARs: `ModuleLifecycleIntegrationTest`).
- Fork patches are changed **only** through `scripts/fork/generate_patches.py`, never by hand;
  then let CI run `applyAllPatches` for every affected version.
- Prerequisites and network access for fork builds: [`docs/BUILDING.md`](docs/BUILDING.md).

## Test levels

Name the level whenever you report a result:

| Level | Example |
|---|---|
| Unit / integration tests | `./gradlew :runtime-core:test` |
| Paper build | `applyAllPatches` + `build` (runs Paper's own test suite) |
| Live server test | a real server started and driven through its console (`scripts/ci/smoke-test.sh`) |

"It compiles" is not a test result, and a unit test is not a live server test.

## Code style

- Java: 4 spaces, `final` for classes not designed for inheritance, Javadoc on public types, no
  wildcard imports in production code, `this.` for fields.
- No reflection into server internals and no `net.minecraft`/CraftBukkit imports in the runtime
  (exceptions are documented: the three hook lines in `CraftServer`, the plugin unloading in the
  Paper adapter).
- Code comments in English. The documentation under `docs/` is currently German; English
  translations are welcome.
- [`.editorconfig`](.editorconfig) holds the basic formatting rules.

## Commits, pull requests and CI

- Small, focused commits with the reason in the message.
- Fill in the pull request template, including which test levels you ran.
- `PaperSwap CI` must be green: runtime tests (Temurin and JetBrains Runtime), adapter and modules
  for every version, fork build with Paper's tests and the live server tests.
- Artifacts are only uploaded from successful runs.

## License

By contributing you agree that your contribution is published under the GPL-3.0
([`LICENSE`](LICENSE)). See [`NOTICE.md`](NOTICE.md).
