#!/usr/bin/env bash
# Build and assemble the distributable package into dist/, then zip everything
# (source code + runnable jar + scripts + config) into dist/rabbitmq-java-springboot-<version>.zip
set -e

cd "$(dirname "$0")/.."

VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null | tail -1)
ARTIFACT=rabbitmq-java-springboot-${VERSION}

mvn clean package -DskipTests

rm -rf dist
mkdir -p "dist/${ARTIFACT}"

# Runnable part: jar + start scripts + config (application.yml overrides built-in defaults)
cp target/rabbitmq-java-springboot-*.jar "dist/${ARTIFACT}/"
cp bin/start.sh bin/start.ps1 bin/stop.sh bin/stop.ps1 "dist/${ARTIFACT}/"
cp config/application.yml.example "dist/${ARTIFACT}/application.yml"

# Source part
cp pom.xml README.md "dist/${ARTIFACT}/"
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
