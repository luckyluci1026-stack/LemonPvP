#!/usr/bin/env bash
# Prepares a server directory for the core live patch test: runtime and live patching enabled
# (production profile with an allow-list), every CI patch in the patch directory.
# Usage: prepare-livepatch-server.sh <work-dir> <patch-dir> <baseline-jar>
set -euo pipefail
WORK="$1"
PATCHES="$2"
BASE="$3"
HERE="$(cd "$(dirname "$0")" && pwd)"
INBOX="$WORK/lemon/patches/inbox"
mkdir -p "$WORK/config" "$INBOX"
cat > "$WORK/config/lemon-runtime.yml" <<'YAML'
config-version: 1
enabled: true
live-patching:
  enabled: true
  profile: production
  trusted-hashes-file: lemon/patches/approved.sha256
YAML
cp "$PATCHES"/*.lemonpatch "$PATCHES"/*.lemonpatch.sha256 "$INBOX/"
# The allow-list approves every CI patch by its real digest; the bad-checksum patch is still
# refused because its sidecar does not match.
(cd "$INBOX" && sha256sum ./*.lemonpatch | sed 's#\./##') > "$WORK/lemon/patches/approved.sha256"
cat "$WORK/lemon/patches/approved.sha256"
# Digest of the class file that runs before the patch (checked against the backup later).
unzip -p "$BASE" org/spigotmc/TicksPerSecondCommand.class | sha256sum | cut -d' ' -f1 > "$WORK/expected-tps-base.txt"
cp "$HERE/slp.py" "$WORK/slp.py"
