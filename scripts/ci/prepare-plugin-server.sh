#!/usr/bin/env bash
# Prepares a server directory for the live plugin update test: runtime enabled, plugin updates
# enabled, LemonTestPlugin 1.0.0 installed, other versions in <work>/variants/ (copied into
# plugins/update/ by the command file while the server runs).
# Usage: prepare-plugin-server.sh <work-dir> <example-plugin-dir>
set -euo pipefail
WORK="$1"
PLUGIN="$2/LemonTestPlugin.jar"
HERE="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$WORK/config" "$WORK/plugins/update" "$WORK/variants"
cat > "$WORK/config/lemon-runtime.yml" <<'YAML'
config-version: 1
enabled: true
plugin-updates:
  enabled: true
YAML
cp "$PLUGIN" "$WORK/plugins/LemonTestPlugin.jar"
python3 "$HERE/make-plugin-variant.py" "$PLUGIN" "$WORK/variants/1.0.1/LemonTestPlugin.jar" version 1.0.1
python3 "$HERE/make-plugin-variant.py" "$PLUGIN" "$WORK/variants/broken/LemonTestPlugin.jar" version 1.0.2-broken
python3 "$HERE/make-plugin-variant.py" "$PLUGIN" "$WORK/variants/future/LemonTestPlugin.jar" api-version 99.0
python3 "$HERE/make-plugin-variant.py" "$PLUGIN" "$WORK/variants/addon/LemonTestAddon.jar" addon
printf 'this is not a zip file\n' > "$WORK/variants/NotAPlugin.jar"
