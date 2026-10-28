#!/usr/bin/env bash
set -euo pipefail

# 安装 tjyj-api-service 为 systemd 服务并启动
# 必须以 root 运行

INSTALL_DIR=/opt/tjyj-api-service
SERVICE_NAME=tjyj-api-service
USER_NAME=tjyj

if [ "$EUID" -ne 0 ]; then
    echo "Please run as root (e.g. sudo ./install.sh)"
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

echo "Installing $BINARY to $INSTALL_DIR..."

# 创建运行用户（如果不存在）
if ! id -u "$USER_NAME" >/dev/null 2>&1; then
    useradd -r -s /bin/false "$USER_NAME"
    echo "Created user: $USER_NAME"
fi

# 创建安装目录并复制文件
mkdir -p "$INSTALL_DIR"
cp "$BINARY" "$INSTALL_DIR/tjyj-api-service"
cp env.conf "$INSTALL_DIR/env.conf"
cp tjyj-api-service.service /etc/systemd/system/
chmod +x "$INSTALL_DIR/tjyj-api-service"
chown -R "$USER_NAME:$USER_NAME" "$INSTALL_DIR"

# 加载并启动服务
systemctl daemon-reload
systemctl enable "$SERVICE_NAME"
systemctl restart "$SERVICE_NAME"

echo ""
echo "=========================================="
echo " Installation complete!"
echo " Service: $SERVICE_NAME"
echo " Install dir: $INSTALL_DIR"
echo "=========================================="
echo ""
echo "Service status:"
systemctl status "$SERVICE_NAME" --no-pager || true

echo ""
echo "View logs:"
echo "  journalctl -u $SERVICE_NAME -f"
