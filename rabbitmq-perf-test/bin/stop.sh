#!/bin/bash
# RabbitMQ 压测工具停止脚本

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
APP_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

APP_NAME="rabbitmq-perf-test"
PID_FILE="$APP_DIR/$APP_NAME.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "$APP_NAME is not running (no pid file)"
    exit 0
fi

PID=$(cat "$PID_FILE")
if [ -z "$PID" ] || ! kill -0 "$PID" 2>/dev/null; then
    echo "$APP_NAME is not running (stale pid file, removed)"
    rm -f "$PID_FILE"
    exit 0
fi

echo "Stopping $APP_NAME (pid=$PID) ..."
kill "$PID"

# 等待最多 30 秒优雅退出
for i in $(seq 1 30); do
    if ! kill -0 "$PID" 2>/dev/null; then
        rm -f "$PID_FILE"
        echo "$APP_NAME stopped"
        exit 0
    fi
    sleep 1
done

# 超时后强制 kill
echo "Graceful shutdown timeout, force killing ..."
kill -9 "$PID" 2>/dev/null
rm -f "$PID_FILE"
echo "$APP_NAME killed"
