#!/usr/bin/env bash
# Build and assemble the distributable package into dist/, then zip everything
# (source code + published binaries + scripts + config) into dist/rabbitmq-dotnet-client-<version>.zip
set -e

cd "$(dirname "$0")/.."

VERSION=$(sed -n 's:.*<Version>\(.*\)</Version>.*:\1:p' rabbitmq-dotnet-client.csproj | head -1 | tr -d '[:space:]')
ARTIFACT=rabbitmq-dotnet-client-${VERSION}

dotnet publish -c Release

rm -rf dist
mkdir -p "dist/${ARTIFACT}"

PUBLISH_DIR=$(ls -d artifacts/publish/rabbitmq-dotnet-client/release* 2>/dev/null | head -1)
if [ -z "$PUBLISH_DIR" ]; then
  echo "ERROR: publish output not found under artifacts/publish/" >&2
  exit 1
fi

# Runnable part: published binaries + start scripts + config
cp -r "$PUBLISH_DIR"/. "dist/${ARTIFACT}/"
cp bin/start.sh bin/start.ps1 bin/stop.sh bin/stop.ps1 "dist/${ARTIFACT}/"
cp config/appsettings.json.example "dist/${ARTIFACT}/appsettings.json"

# Source part
cp rabbitmq-dotnet-client.csproj Program.cs README.md "dist/${ARTIFACT}/"
cp -r Rabbit bin config "dist/${ARTIFACT}/"

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
