# Security Policy

PaperSwap loads and changes executable code inside a running Minecraft server. We take reports
about it seriously.

## Supported versions

PaperSwap has no tagged releases yet. Security fixes go into the default branch and apply to all
four supported Minecraft versions (1.21.11, 26.1.2, 26.2, 26.3). Please test against the latest
commit before reporting.

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

1. Use GitHub's private vulnerability reporting:
   [**Report a vulnerability**](https://github.com/luckyluci1026-stack/LemonPvP/security/advisories/new)
   (repository tab *Security* → *Report a vulnerability*).
2. If that button is not available, open an issue that only asks for a private contact – without
   any details of the problem.

Helpful information: affected Minecraft version and PaperSwap commit, JVM and JVM flags, the
relevant part of `config/paperswap.yml`, steps to reproduce and what an attacker would need
(console access? write access to `paperswap/modules/`? a player account?).

The maintainers aim to acknowledge reports promptly, keep you informed and credit you in the
advisory unless you prefer otherwise. Please allow a reasonable amount of time for a fix before
disclosing the issue.

## Scope

In scope: the PaperSwap runtime (`runtime/`), the agent (`paperswap-agent.jar`), the patch
format and tools (`.swappatch`, `scripts/`), the fork patches (`versions/`) and the CI workflow.

Out of scope:

- vulnerabilities that also exist in upstream Paper – please report them to
  [PaperMC](https://github.com/PaperMC/Paper/security);
- "a trusted operator can run code": anyone who may write to `paperswap/modules/`, the allow-list,
  `config/paperswap.yml` or the server console can, by design, run arbitrary code. A hash or an
  allow-list proves *which* file runs, not that it is harmless, and class-loader isolation is not a
  sandbox.

## Security model in short

- The runtime is **off by default**; runtime deployment, plugin updates and core patches each have
  their own switch.
- Code-changing commands need permissions that default to **nobody** – they run from the server
  console only (RCON is not the console); plugin updates and core patches also require an
  explicit `confirm` after a plan.
- PaperSwap opens **no network port** and downloads nothing. The agent can be attached through the
  JDK's local attach mechanism (same OS user only). An exposed JDWP debug port is reported as a
  warning.
- Modules (by default) and core patches need a matching SHA-256 file; in the production profile
  core patches must also be on the allow-list.
- Changes are recorded in `paperswap/logs/audit.log`.

The full threat model, implemented protections and operating recommendations are documented in
[`docs/SECURITY.md`](docs/SECURITY.md) (German).
