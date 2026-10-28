#!/usr/bin/env bash
# Start the demo. An appsettings.json next to this script overrides the built-in defaults.
cd "$(dirname "$0")"

DLL=rabbitmq-dotnet-client.dll
if [ ! -f "$DLL" ]; then
  echo "ERROR: $DLL not found, run bin/build.sh first" >&2
  exit 1
fi

exec dotnet "$DLL" "$@"
