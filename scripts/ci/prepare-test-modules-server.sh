#!/usr/bin/env bash
# Prepares a server directory for the test-module server test (limits, faults, dependencies,
# hot-swap churn, leaks; see scripts/ci/smoke-test-modules.txt and docs/TEST-MODULES.md).
# Usage: prepare-test-modules-server.sh <work-dir> <test-modules-dir>
#   <test-modules-dir>: output of ./gradlew :test-modules:collect (jars, .sha256, variants/2.0.0/)
set -euo pipefail
WORK="$1"
MODULES="$2"
mkdir -p "$WORK/config" "$WORK/lemon/modules" "$WORK/originals" "$WORK/variants"
cat > "$WORK/config/lemon-runtime.yml" <<'YAML'
config-version: 1
enabled: true
allow-runtime-deploy: true
verify-integrity: true
# The test enables and swaps modules itself; nothing is re-activated at startup.
persist-runtime-state: false
# Low on purpose: test-chaos blocks onEnable for 5.5 s to trigger the slow-callback warning.
operation-timeout-seconds: 5
YAML
cp "$MODULES"/test-*.jar "$MODULES"/test-*.jar.sha256 "$WORK/lemon/modules/"
cp "$MODULES"/test-*.jar "$MODULES"/test-*.jar.sha256 "$WORK/originals/"
cp -r "$MODULES/variants/." "$WORK/variants/"
(cd "$WORK/lemon/modules" && sha256sum -c ./*.sha256)
(cd "$WORK/variants/2.0.0" && sha256sum -c ./*.sha256)
