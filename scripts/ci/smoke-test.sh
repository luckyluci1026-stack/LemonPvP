#!/usr/bin/env bash
# Starts a built PaperSwap paperclip jar, waits for the server to finish starting,
# optionally sends console commands, stops it and checks the log.
#
# Usage: scripts/ci/smoke-test.sh <server-jar> <work-dir> [command-file]
#
# Environment: JAVA_BIN (default java), EXTRA_JVM_ARGS (e.g. -javaagent:...; split on spaces),
#              STARTUP_TIMEOUT, STOP_TIMEOUT.
#
#   command-file: optional file with one console command per line. Lines of the
#                 form "@expect <regex>" assert that the log matches <regex>
#                 (checked after all commands were executed); "@reject <regex>"
#                 asserts that it does NOT match; "@count <n> <regex>" asserts
#                 exactly n matching lines; "@sleep <seconds>" pauses;
#                 "@shell <command>" runs a shell command in the work directory
#                 (a non-zero exit counts as a failed assertion; the test goes on;
#                 $SERVER_PID is the server's process id, e.g. for jcmd; $SCRIPT_DIR is
#                 the directory of this script).
#
# Exit code is non-zero if the server does not start within STARTUP_TIMEOUT
# seconds, does not stop within STOP_TIMEOUT seconds, or an assertion fails.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$(realpath "$1")"
WORK="$2"
COMMANDS="${3:-}"
if [ -n "$COMMANDS" ]; then COMMANDS="$(realpath "$COMMANDS")"; fi
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-600}"
STOP_TIMEOUT="${STOP_TIMEOUT:-180}"
JAVA_BIN="${JAVA_BIN:-java}"
read -r -a EXTRA <<< "${EXTRA_JVM_ARGS:-}"

mkdir -p "$WORK"
cd "$WORK"
echo "eula=true" > eula.txt
# Keep the test world small and the server offline; nothing here is required by PaperSwap.
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
"$JAVA_BIN" -Xms1G -Xmx2G "${EXTRA[@]+"${EXTRA[@]}"}" -jar "$JAR" --nogui <&3 > server.log 2>&1 &
PID=$!
echo "server pid $PID: $JAVA_BIN -Xms1G -Xmx2G ${EXTRA_JVM_ARGS:-} -jar $(basename "$JAR") --nogui"

send() { echo "$1" >&3; }

cleanup() {
  if kill -0 "$PID" 2>/dev/null; then kill -9 "$PID" || true; fi
  rm -f "$FIFO"
}
trap cleanup EXIT

DEADLINE=$((SECONDS + STARTUP_TIMEOUT))
until grep -qE 'Done \([0-9.,]+s\)!' server.log; do
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "::error::server process exited during startup"; tail -n 200 server.log; exit 1
  fi
  if [ "$SECONDS" -ge "$DEADLINE" ]; then
    echo "::error::server did not start within ${STARTUP_TIMEOUT}s"; tail -n 200 server.log; exit 1
  fi
  sleep 0.2
done
READY_NS=$(date +%s%N)
echo "startup wall time: $(( (READY_NS - START_NS) / 1000000 )) ms (process start until 'Done' line, 0.2 s polling)"
grep -E 'Done \(' server.log | tail -n 1

declare -a EXPECT=() REJECT=() COUNT=() SHELL_FAILED=()
if [ -n "$COMMANDS" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      ''|'#'*) ;;
      '@expect '*) EXPECT+=("${line#@expect }") ;;
      '@reject '*) REJECT+=("${line#@reject }") ;;
      '@count '*) COUNT+=("${line#@count }") ;;
      '@sleep '*) sleep "${line#@sleep }" ;;
      '@shell '*) echo "$ ${line#@shell }"; SERVER_PID="$PID" SCRIPT_DIR="$SCRIPT_DIR" bash -c "${line#@shell }" || SHELL_FAILED+=("${line#@shell }") ;;
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
for cmd in "${SHELL_FAILED[@]+"${SHELL_FAILED[@]}"}"; do
  echo "::error::shell check failed: $cmd"; status=1
done
for re in "${EXPECT[@]+"${EXPECT[@]}"}"; do
  if grep -qE -- "$re" server.log; then echo "ok   expect: $re"; else echo "::error::missing expected log line: $re"; status=1; fi
done
for re in "${REJECT[@]+"${REJECT[@]}"}"; do
  if grep -qE -- "$re" server.log; then echo "::error::unexpected log line: $re"; grep -nE -- "$re" server.log | head -n 20; status=1; else echo "ok   reject: $re"; fi
done
for spec in "${COUNT[@]+"${COUNT[@]}"}"; do
  want="${spec%% *}"; re="${spec#* }"
  got=$(grep -cE -- "$re" server.log || true)
  if [ "$got" -eq "$want" ]; then echo "ok   count $want: $re"; else echo "::error::expected $want lines matching '$re' but found $got"; status=1; fi
done
if [ "$status" -ne 0 ]; then
  tail -n 300 server.log
else
  echo "--- server log after startup (max. 400 lines) ---"
  # awk instead of "sed | head": head closing the pipe makes sed fail (exit 4) under pipefail.
  awk '/Done \(/ { f = 1 } f && n++ < 400' server.log
fi
exit "$status"
