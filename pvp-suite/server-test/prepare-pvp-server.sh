#!/usr/bin/env bash
# Prepares a server directory for the LemonPvP suite server test (scripts/ci/smoke-pvp.txt).
# Usage: prepare-pvp-server.sh <work-dir> <pvp-suite-dir>
#   <pvp-suite-dir>: output of ./gradlew :pvp-suite:collect (pvp-*.jar + .sha256)
set -euo pipefail
WORK="$1"
MODULES="$2"
mkdir -p "$WORK/config" "$WORK/paperswap/modules"
cat > "$WORK/config/paperswap.yml" <<'YAML'
config-version: 1
enabled: true
allow-runtime-deploy: true
verify-integrity: true
persist-runtime-state: false
YAML
cp "$MODULES"/pvp-*.jar "$MODULES"/pvp-*.jar.sha256 "$WORK/paperswap/modules/"
(cd "$WORK/paperswap/modules" && sha256sum -c ./*.sha256)
