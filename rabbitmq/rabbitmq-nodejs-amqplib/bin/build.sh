#!/usr/bin/env bash
# Install dependencies and assemble the distributable package into dist/, then
# zip everything (source + node_modules + scripts + config) into
# dist/rabbitmq-nodejs-amqplib-<version>.zip
set -e

cd "$(dirname "$0")/.."

VERSION="1.0.0"
ARTIFACT=rabbitmq-nodejs-amqplib-${VERSION}

# Install production dependencies (generates package-lock.json on first run).
npm ci --omit=dev 2>/dev/null || npm install --omit=dev

rm -rf dist
mkdir -p "dist/${ARTIFACT}"

# Runnable part: node_modules + start scripts + config
cp -r node_modules "dist/${ARTIFACT}/"
cp bin/start.sh bin/start.ps1 bin/stop.sh bin/stop.ps1 "dist/${ARTIFACT}/"
cp config/config.json.example "dist/${ARTIFACT}/config.json"

# Source part
cp package.json README.md "dist/${ARTIFACT}/"
[ -f package-lock.json ] && cp package-lock.json "dist/${ARTIFACT}/"
cp -r src bin config "dist/${ARTIFACT}/"

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
