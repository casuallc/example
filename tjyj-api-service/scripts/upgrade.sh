#!/usr/bin/env bash
set -euo pipefail

# 升级 tjyj-api-service：保留现有配置，只替换二进制并重启服务
# 必须以 root 运行

INSTALL_DIR=/opt/tjyj-api-service
SERVICE_NAME=tjyj-api-service
USER_NAME=tjyj

if [ "$EUID" -ne 0 ]; then
    echo "Please run as root (e.g. sudo ./upgrade.sh)"
    exit 1
fi

ARCH=$(uname -m)
case "$ARCH" in
    x86_64)
        BINARY="tjyj-api-service_linux_amd64"
        ;;
    aarch64)
        BINARY="tjyj-api-service_linux_arm64"
        ;;
    *)
        echo "Unsupported architecture: $ARCH"
        exit 1
        ;;
esac

if [ ! -f "$BINARY" ]; then
    echo "Binary not found: $BINARY"
    echo "This package may not match your CPU architecture."
    exit 1
fi

echo "Stopping $SERVICE_NAME..."
systemctl stop "$SERVICE_NAME" || true

echo "Backing up current binary..."
if [ -f "$INSTALL_DIR/tjyj-api-service" ]; then
    cp "$INSTALL_DIR/tjyj-api-service" "$INSTALL_DIR/tjyj-api-service.bak.$(date +%Y%m%d%H%M%S)"
fi

echo "Installing new binary..."
cp "$BINARY" "$INSTALL_DIR/tjyj-api-service"
chmod +x "$INSTALL_DIR/tjyj-api-service"
chown "$USER_NAME:$USER_NAME" "$INSTALL_DIR/tjyj-api-service"

# 如果 service 文件有更新，一并替换
echo "Updating systemd service file..."
cp tjyj-api-service.service /etc/systemd/system/
systemctl daemon-reload

echo "Starting $SERVICE_NAME..."
systemctl start "$SERVICE_NAME"

echo ""
echo "=========================================="
echo " Upgrade complete!"
echo " Service: $SERVICE_NAME"
echo " Install dir: $INSTALL_DIR"
echo "=========================================="
echo ""
echo "Service status:"
systemctl status "$SERVICE_NAME" --no-pager || true

echo ""
echo "View logs:"
echo "  journalctl -u $SERVICE_NAME -f"
echo ""
echo "Note: env.conf is preserved. If new config variables were added,"
echo "      update $INSTALL_DIR/env.conf manually and restart the service."
