#!/usr/bin/env bash
# Starts a built Lemon paperclip jar, waits for the server to finish starting,
# optionally sends console commands, stops it and checks the log.
#
# Usage: scripts/ci/smoke-test.sh <server-jar> <work-dir> [command-file]
#
#   command-file: optional file with one console command per line. Lines of the
#                 form "@expect <regex>" assert that the log matches <regex>
#                 (checked after all commands were executed); "@reject <regex>"
#                 asserts that it does NOT match; "@sleep <seconds>" pauses.
#
# Exit code is non-zero if the server does not start within STARTUP_TIMEOUT
# seconds, does not stop within STOP_TIMEOUT seconds, or an assertion fails.
set -euo pipefail

JAR="$(realpath "$1")"
WORK="$2"
COMMANDS="${3:-}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-600}"
STOP_TIMEOUT="${STOP_TIMEOUT:-180}"
JAVA_BIN="${JAVA_BIN:-java}"

mkdir -p "$WORK"
cd "$WORK"
echo "eula=true" > eula.txt
# Keep the test world small and the server offline; nothing here is required by Lemon.
cat > server.properties <<'EOF'
online-mode=false
server-port=25599
view-distance=4
simulation-distance=4
spawn-protection=0
EOF

FIFO="$(mktemp -u)"
mkfifo "$FIFO"
exec 3<>"$FIFO"

START_NS=$(date +%s%N)
"$JAVA_BIN" -Xms1G -Xmx2G -jar "$JAR" --nogui <&3 > server.log 2>&1 &
PID=$!
echo "server pid $PID"

send() { echo "$1" >&3; }

cleanup() {
  if kill -0 "$PID" 2>/dev/null; then kill -9 "$PID" || true; fi
  rm -f "$FIFO"
}
trap cleanup EXIT

waited=0
until grep -qE 'Done \([0-9.,]+s\)!' server.log; do
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "::error::server process exited during startup"; tail -n 200 server.log; exit 1
  fi
  if [ "$waited" -ge "$STARTUP_TIMEOUT" ]; then
    echo "::error::server did not start within ${STARTUP_TIMEOUT}s"; tail -n 200 server.log; exit 1
  fi
  sleep 2; waited=$((waited + 2))
done
READY_NS=$(date +%s%N)
echo "startup wall time: $(( (READY_NS - START_NS) / 1000000 )) ms (process start until 'Done' line)"
grep -E 'Done \(' server.log | tail -n 1

declare -a EXPECT=() REJECT=()
if [ -n "$COMMANDS" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      ''|'#'*) ;;
      '@expect '*) EXPECT+=("${line#@expect }") ;;
      '@reject '*) REJECT+=("${line#@reject }") ;;
      '@sleep '*) sleep "${line#@sleep }" ;;
      *) echo "> $line"; send "$line"; sleep 1 ;;
    esac
  done < "$COMMANDS"
  sleep 3
fi

send "stop"
waited=0
while kill -0 "$PID" 2>/dev/null; do
  if [ "$waited" -ge "$STOP_TIMEOUT" ]; then
    echo "::error::server did not stop within ${STOP_TIMEOUT}s"; tail -n 200 server.log; exit 1
  fi
  sleep 1; waited=$((waited + 1))
done
wait "$PID" || true

status=0
for re in "${EXPECT[@]+"${EXPECT[@]}"}"; do
  if grep -qE -- "$re" server.log; then echo "ok   expect: $re"; else echo "::error::missing expected log line: $re"; status=1; fi
done
for re in "${REJECT[@]+"${REJECT[@]}"}"; do
  if grep -qE -- "$re" server.log; then echo "::error::unexpected log line: $re"; grep -nE -- "$re" server.log | head -n 20; status=1; else echo "ok   reject: $re"; fi
done
if [ "$status" -ne 0 ]; then tail -n 300 server.log; fi
exit "$status"
