#!/bin/bash
# 构建 rabbitmq-perf-test 的 tar.gz 发布包（Linux）
# 用法: ./build.sh [version]

set -e

VERSION=${1:-1.0.0}
PROJECT_DIR=$(cd "$(dirname "$0")" && pwd)
DIST_DIR="$PROJECT_DIR/dist"
PACKAGE_NAME="rabbitmq-perf-test-$VERSION"
PACKAGE_DIR="$DIST_DIR/$PACKAGE_NAME"
TAR_FILE="$DIST_DIR/$PACKAGE_NAME.tar.gz"

echo "=== Building rabbitmq-perf-test Package ==="
echo "Version: $VERSION"

echo "[1/4] Compiling with Maven..."
mvn clean package -DskipTests -f "$PROJECT_DIR/pom.xml"

echo "[2/4] Preparing package directory..."
rm -rf "$DIST_DIR"
mkdir -p "$PACKAGE_DIR/bin" "$PACKAGE_DIR/lib" "$PACKAGE_DIR/config" "$PACKAGE_DIR/logs"

echo "[3/4] Copying files..."
cp "$PROJECT_DIR/target/rabbitmq-perf-test-$VERSION-jar-with-dependencies.jar" "$PACKAGE_DIR/lib/"
cp "$PROJECT_DIR/config/"*.properties "$PACKAGE_DIR/config/"
cp "$PROJECT_DIR/bin/start.sh" "$PROJECT_DIR/bin/stop.sh" "$PACKAGE_DIR/bin/"
cp "$PROJECT_DIR/README.md" "$PACKAGE_DIR/"
chmod +x "$PACKAGE_DIR/bin/"*.sh

echo "[4/4] Creating tar.gz archive..."
tar -czf "$TAR_FILE" -C "$DIST_DIR" "$PACKAGE_NAME"
rm -rf "$PACKAGE_DIR"

echo "=== Build Complete ==="
echo "Output: $TAR_FILE"
