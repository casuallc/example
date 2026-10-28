#!/usr/bin/env bash
# Build and assemble the distributable package into dist/, then zip everything
# (source code + binary + scripts + config) into dist/rabbitmq-go-amqp091-<version>.zip
# Set GOOS/GOARCH to cross-compile, e.g. GOOS=linux GOARCH=amd64 bin/build.sh
set -e

cd "$(dirname "$0")/.."

VERSION="1.0.0"
ARTIFACT=rabbitmq-go-amqp091-${VERSION}

TARGET_GOOS=${GOOS:-$(go env GOOS)}
TARGET_GOARCH=${GOARCH:-$(go env GOARCH)}
BIN_NAME=rabbitmq-go-amqp091
if [ "$TARGET_GOOS" = "windows" ]; then
  BIN_NAME="${BIN_NAME}.exe"
fi

GOOS=$TARGET_GOOS GOARCH=$TARGET_GOARCH CGO_ENABLED=0 go build -trimpath -o "$BIN_NAME" .

rm -rf dist
mkdir -p "dist/${ARTIFACT}"

# Runnable part: binary + start scripts + config
cp "$BIN_NAME" "dist/${ARTIFACT}/"
cp bin/start.sh bin/start.ps1 bin/stop.sh bin/stop.ps1 "dist/${ARTIFACT}/"
cp config/config.json.example "dist/${ARTIFACT}/config.json"

# Source part
cp go.mod go.sum main.go README.md "dist/${ARTIFACT}/"
cp -r rabbit bin config "dist/${ARTIFACT}/"

cd dist
rm -f "${ARTIFACT}.zip"
if command -v zip >/dev/null 2>&1; then
  zip -qr "${ARTIFACT}.zip" "${ARTIFACT}"
else
  powershell -NoProfile -Command "Compress-Archive -Path '${ARTIFACT}' -DestinationPath '${ARTIFACT}.zip' -Force"
fi

echo "Build finished:"
ls -lh "${ARTIFACT}.zip"
echo "Contents:"
if command -v unzip >/dev/null 2>&1; then
  unzip -l "${ARTIFACT}.zip"
fi
