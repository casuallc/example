#!/bin/bash
# RabbitMQ 压测工具启动脚本（后台运行）
# 用法:
#   bin/start.sh                      使用 config/config.properties 启动
#   bin/start.sh key=value ...        启动并覆盖配置，如: bin/start.sh queueCount=10000 producerRate=50000

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
APP_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$APP_DIR" || exit 1

APP_NAME="rabbitmq-perf-test"
CONFIG_FILE="config/config.properties"
PID_FILE="$APP_DIR/$APP_NAME.pid"
LOG_DIR="$APP_DIR/logs"
OUT_LOG="$LOG_DIR/$APP_NAME.out"

JAR_FILE=$(ls lib/$APP_NAME-*-jar-with-dependencies.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "Error: $APP_NAME jar not found under lib/"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: config file not found: $CONFIG_FILE"
    exit 1
fi

# 已在运行则退出
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        echo "$APP_NAME is already running (pid=$OLD_PID)"
        exit 1
    fi
    rm -f "$PID_FILE"
fi

mkdir -p "$LOG_DIR"

# 定位 java：优先 JAVA_HOME，其次 PATH
if [ -n "$JAVA_HOME" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVA_BIN" ]; then
        echo "Error: JAVA_HOME is set but $JAVA_BIN not found or not executable"
        exit 1
    fi
else
    JAVA_BIN=$(command -v java 2>/dev/null)
    if [ -z "$JAVA_BIN" ]; then
        echo "Error: java not found. Please set JAVA_HOME or add java to PATH"
        exit 1
    fi
fi

# JVM 参数可通过环境变量覆盖
JAVA_OPTS=${JAVA_OPTS:-"-Xmx2g"}

echo "Starting $APP_NAME ..."
echo "  java   : $JAVA_BIN"
echo "  jar    : $JAR_FILE"
echo "  config : $CONFIG_FILE"
[ $# -gt 0 ] && echo "  args   : $*"

nohup "$JAVA_BIN" $JAVA_OPTS -jar "$JAR_FILE" --config="$CONFIG_FILE" "$@" >> "$OUT_LOG" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"

sleep 2
if kill -0 "$PID" 2>/dev/null; then
    echo "$APP_NAME started (pid=$PID)"
    echo "  stdout : $OUT_LOG"
    echo "  stop   : bin/stop.sh"
else
    echo "Error: $APP_NAME failed to start, see $OUT_LOG"
    rm -f "$PID_FILE"
    exit 1
fi
