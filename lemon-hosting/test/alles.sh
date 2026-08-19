#!/usr/bin/env bash
#
# Alle Testreihen der Reihe nach.
#
# Fuenf davon reden mit einem laufenden Portal. Jede bekommt ein eigenes,
# frisches: Ein Test, der auf Datenbankresten des vorherigen aufsetzt,
# geht irgendwann grundlos kaputt - und man sucht dann am falschen Ende.
#
#   test/alles.sh
#
# Ein paar Pruefungen starten wirklich einen Server. Dafuer wird
# test/ersatzserver/Server.java einmal uebersetzt - ein Programm, das
# sich wie ein Minecraft-Server verhaelt, aber in einer Sekunde oben ist.
# Dazu braucht es das JDK (`javac`); fehlt es, laufen alle Reihen
# trotzdem, nur diese Teile werden uebersprungen.
#
# Mit ERSATZ_JAR=/pfad/paper.jar nimmt das Skript stattdessen die
# angegebene Jar - dann laeuft wirklich Minecraft, dauert aber laenger.
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

# ------------------------------------------------------------- Ersatzserver
#
# Ohne ihn pruefen die Reihen nur, ob Knoepfe da sind - nicht, ob
# dahinter etwas passiert. Deshalb wird er hier gebaut, statt ihn vom
# Benutzer zu verlangen.
if [ -z "${ERSATZ_JAR:-}" ]; then
  if command -v javac > /dev/null && command -v jar > /dev/null; then
    bau="$ARBEIT/ersatzserver"
    mkdir -p "$bau"
    if javac -d "$bau" test/ersatzserver/Server.java 2> "$ARBEIT/javac.log" \
       && jar --create --file "$ARBEIT/server.jar" --main-class Server -C "$bau" . ; then
      ERSATZ_JAR="$ARBEIT/server.jar"
      echo "Ersatzserver gebaut: $ERSATZ_JAR"
    else
      echo "Ersatzserver liess sich nicht bauen:"
      sed 's/^/  /' "$ARBEIT/javac.log"
      echo "  Die Reihen laufen trotzdem - ohne die Teile, die starten."
    fi
  else
    echo "Kein javac gefunden (JDK, nicht nur JRE)."
    echo "  Die Reihen laufen trotzdem - ohne die Teile, die wirklich starten."
  fi
fi
export ERSATZ_JAR="${ERSATZ_JAR:-}"

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
