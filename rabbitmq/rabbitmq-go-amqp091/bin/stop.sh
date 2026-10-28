#!/usr/bin/env bash
# Stop the demo by terminating the process listening on the HTTP port.
# Usage: ./stop.sh [port]  (default: read from the config file next to this
# script, fallback 8080; HTTP_PORT env var also works)
cd "$(dirname "$0")"

PORT="${1:-${HTTP_PORT:-}}"

# Best-effort port extraction from the config file next to this script.
if [ -z "$PORT" ] && [ -f config.json ]; then
  PORT=$(grep -o '"httpAddr"[^,}]*' config.json | grep -oE '[0-9]+' | tail -1)
fi
if [ -z "$PORT" ] && [ -f application.yml ]; then
  PORT=$(awk '/^server:/{s=1; next} s && /^[[:space:]]+port:/{print $2; exit}' application.yml)
fi
if [ -z "$PORT" ] && [ -f appsettings.json ]; then
  PORT=$(grep -o '"Url"[^,}]*' appsettings.json | grep -oE '[0-9]+' | tail -1)
fi
PORT="${PORT:-8080}"

is_windows=0
case "$OSTYPE" in msys*|cygwin*|win32*) is_windows=1 ;; esac
[ -n "$WINDIR" ] && is_windows=1

pids=""
if [ "$is_windows" = "1" ]; then
  pids=$(netstat -ano | grep -i "LISTENING" | grep -E "[:.]$PORT[[:space:]]" | awk '{print $NF}' | sort -u)
elif command -v lsof >/dev/null 2>&1; then
  pids=$(lsof -ti "tcp:$PORT" -sTCP:LISTEN 2>/dev/null)
elif command -v ss >/dev/null 2>&1; then
  pids=$(ss -ltnp 2>/dev/null | grep ":$PORT " | grep -o 'pid=[0-9]*' | cut -d= -f2 | sort -u)
elif command -v fuser >/dev/null 2>&1; then
  pids=$(fuser "$PORT/tcp" 2>/dev/null)
fi

if [ -z "$pids" ]; then
  echo "No process listening on port $PORT"
  exit 0
fi

for pid in $pids; do
  echo "Stopping PID $pid listening on port $PORT"
  if [ "$is_windows" = "1" ]; then
    taskkill //PID "$pid" //F >/dev/null 2>&1
  else
    kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null
  fi
done
