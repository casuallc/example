#!/bin/bash

CONFIG_FILE=${1:-config/queue-send.properties}

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$SCRIPT_DIR/.." || exit 1

if [ ! -d "lib" ]; then
    echo "Error: lib directory not found."
    exit 1
fi

echo "Starting admq Test Client with config: $CONFIG_FILE"
java -cp "lib/*" com.apusic.admq.example.jms.Main "$CONFIG_FILE"
