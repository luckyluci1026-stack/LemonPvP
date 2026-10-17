## Summary

<!-- What does this change and why? Link related issues (e.g. "Fixes #123"). -->

## Affected versions

- [ ] 1.21.11
- [ ] 26.1.2
- [ ] 26.2
- [ ] 26.3
- [ ] runtime only (all versions)

## Tests

<!-- Name the level of every result. "It compiles" is not a test result. -->

- [ ] Unit / integration tests (`cd runtime && ./gradlew build`)
- [ ] Paper build (`applyAllPatches build` for the affected versions, locally or in CI)
- [ ] Live server test (a real server started and driven through its console)
- [ ] Not tested – reason:

## Checklist

- [ ] With `enabled: false` the server still behaves like plain Paper.
- [ ] Anything that loads or changes code stays opt-in, console-only by default and reports failures as failures.
- [ ] Fork patches were regenerated with `scripts/fork/generate_patches.py` (not edited by hand).
- [ ] Documentation updated (`docs/`, README) where behaviour changed.
- [ ] No secrets, tokens, passwords or open network ports added.
