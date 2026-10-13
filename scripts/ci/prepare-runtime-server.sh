#!/usr/bin/env bash
# Prepares a server directory with hot deployment enabled and the example module installed.
# Usage: prepare-runtime-server.sh <work-dir> <example-module-dir>
set -euo pipefail
WORK="$1"
MODULE_DIR="$2"
mkdir -p "$WORK/config" "$WORK/lemon/modules"
cat > "$WORK/config/lemon-runtime.yml" <<'YAML'
config-version: 1
enabled: true
allow-runtime-deploy: true
verify-integrity: true
YAML
cp "$MODULE_DIR/example-greeter.jar" "$MODULE_DIR/example-greeter.jar.sha256" "$WORK/lemon/modules/"
(cd "$WORK/lemon/modules" && sha256sum -c example-greeter.jar.sha256)
# Second build of the same module with a different version for the hot-swap test.
python3 "$(dirname "$0")/make-module-variant.py" "$MODULE_DIR/example-greeter.jar" "$WORK/variant/example-greeter.jar" 1.0.1
