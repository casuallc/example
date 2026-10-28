#!/usr/bin/env bash
# Start the demo. A config.json next to this script overrides the built-in defaults.
cd "$(dirname "$0")"

BIN=./rabbitmq-go-amqp091
if [ ! -f "$BIN" ] && [ -f "${BIN}.exe" ]; then
  BIN="${BIN}.exe"
fi
if [ ! -f "$BIN" ]; then
  echo "ERROR: rabbitmq-go-amqp091 binary not found, run bin/build.sh first" >&2
  exit 1
fi

exec "$BIN" "$@"
