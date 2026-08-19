#!/usr/bin/env bash
#
# Alle Testreihen der Reihe nach.
#
# Fuenf davon reden mit einem laufenden Portal. Jede bekommt ein eigenes,
# frisches: Ein Test, der auf Datenbankresten des vorherigen aufsetzt,
# geht irgendwann grundlos kaputt - und man sucht dann am falschen Ende.
#
#   ERSATZ_JAR=/pfad/server.jar test/alles.sh
#
# ERSATZ_JAR ist eine Jar, die sich wie ein Minecraft-Server verhaelt
# (eine "Done"-Zeile ausgibt und auf `stop` hoert). Ohne sie werden die
# Teile uebersprungen, die wirklich etwas starten.
#
# Beendet wird ueber die gemerkte Prozessnummer, nicht ueber `pkill -f`.
# Ein Muster, das auf die Kommandozeile passt, passt naemlich auch auf
# die Shell, die es gerade ausfuehrt - und die bringt sich dann selbst um.

set -u
cd "$(dirname "$0")/.."

ARBEIT=$(mktemp -d)
PORTAL=0
aufraeumen() {
  [ "$PORTAL" -gt 0 ] && kill "$PORTAL" 2>/dev/null
  rm -rf "$ARBEIT"
}
trap aufraeumen EXIT

FEHLGESCHLAGEN=()

# Ein Portal hochfahren; setzt PORTAL und PW.
starte_portal() {
  local wo="$1"
  mkdir -p "$wo"
  env DB="$wo/portal.db" SERVER_DIR="$wo/server" PORT=3111 \
    node start.js > "$wo/portal.log" 2>&1 < /dev/null &
  PORTAL=$!
  for _ in $(seq 60); do
    grep -q 'Passwort' "$wo/portal.log" 2>/dev/null && break
    sleep 0.25
  done
  PW=$(grep -oP 'Passwort\s+\K\S+' "$wo/portal.log" | head -1)
}

stoppe_portal() {
  [ "$PORTAL" -gt 0 ] || return 0
  kill "$PORTAL" 2>/dev/null
  wait "$PORTAL" 2>/dev/null
  PORTAL=0
}

lauf() {
  local name="$1"; shift
  echo
  echo "=== $name ==="
  if "$@"; then :; else FEHLGESCHLAGEN+=("$name"); fi
}

# --- ohne Portal ---------------------------------------------------------
lauf start    node test/start.mjs
lauf docker   node test/docker.mjs
lauf zeitplan node test/zeitplan.mjs
lauf arten    node test/arten.mjs

# --- mit Portal ----------------------------------------------------------
for reihe in durchklicken teilen api zweifach knoten umzug; do
  wo="$ARBEIT/$reihe"
  starte_portal "$wo"
  if [ -z "${PW:-}" ]; then
    echo; echo "=== $reihe ==="; echo "  Das Portal ist nicht hochgekommen:"
    tail -5 "$wo/portal.log"
    FEHLGESCHLAGEN+=("$reihe")
    stoppe_portal
    continue
  fi
  lauf "$reihe" env ADMINPW="$PW" SERVER_DIR="$wo/server" \
       ERSATZ_JAR="${ERSATZ_JAR:-}" node "test/$reihe.mjs"
  stoppe_portal
done

echo
if [ ${#FEHLGESCHLAGEN[@]} -eq 0 ]; then
  echo "ALLES GRUEN"
else
  echo "Fehlgeschlagen: ${FEHLGESCHLAGEN[*]}"
  exit 1
fi
