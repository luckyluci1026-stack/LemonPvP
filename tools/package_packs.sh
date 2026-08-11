#!/usr/bin/env bash
#
# Packt beide Resourcepacks in auslieferbare Dateien.
#
#   dist/Helden3-Java.zip       -> server.properties / Webspace fuer Java-Spieler
#   dist/Helden3-Bedrock.mcpack -> Geyser/packs/ fuer Bedrock-Spieler
#
# Der SHA-1 am Ende gehoert in server.properties (resource-pack-sha1).

set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v zip >/dev/null 2>&1; then
  echo "Fehler: 'zip' ist nicht installiert." >&2
  exit 1
fi

mkdir -p dist
rm -f dist/Helden3-Java.zip dist/Helden3-Bedrock.mcpack

echo "==> Java-Pack"
( cd resourcepack/java && zip -r -q -X ../../dist/Helden3-Java.zip . -x '.*' -x '*/.*' )

echo "==> Bedrock-Pack"
( cd resourcepack/bedrock && zip -r -q -X ../../dist/Helden3-Bedrock.mcpack . -x '.*' -x '*/.*' )

echo
echo "Fertig:"
for file in dist/Helden3-Java.zip dist/Helden3-Bedrock.mcpack; do
  size=$(wc -c < "$file")
  if command -v sha1sum >/dev/null 2>&1; then
    hash=$(sha1sum "$file" | cut -d' ' -f1)
  else
    hash=$(shasum "$file" | cut -d' ' -f1)
  fi
  printf '  %-32s %7s Bytes  sha1=%s\n' "$file" "$size" "$hash"
done
echo
echo "Java:    resource-pack-sha1 in der server.properties auf den Java-Hash setzen."
echo "Bedrock: die .mcpack nach Geyser/packs/ kopieren und Geyser neu starten."
