#!/usr/bin/env bash
# Start the demo. A config.json next to this script overrides the built-in defaults.
cd "$(dirname "$0")"

if ! command -v node >/dev/null 2>&1; then
  echo "ERROR: node not found, Node.js 18+ is required" >&2
  exit 1
fi
if [ ! -d node_modules/amqplib ]; then
  echo "ERROR: node_modules not found, run bin/build.sh first" >&2
  exit 1
fi

exec node src/server.js --config config.json "$@"
