#!/usr/bin/env pwsh
#Requires -Version 5.1
<#
.SYNOPSIS
    Cross-compile tjyj-api-service for ARM/x86 inside WSL.
.DESCRIPTION
    This script enters WSL, ensures Go is installed,
    then builds linux/windows binaries for amd64 and arm64,
    and packages linux binaries into tar.gz archives with an install script.
    Output goes to dist/ and release/.
.EXAMPLE
    .\build-wsl.ps1
    .\build-wsl.ps1 -Version 1.0.0
    .\build-wsl.ps1 -SudoPassword 'your_wsl_password'
#>

param(
    [string]$Version = "",
    [string]$ProjectRoot = "$PSScriptRoot",
    [string]$SudoPassword = ""
)

# Resolve full path because /mnt/c/... needs an absolute path
$ProjectRoot = Resolve-Path $ProjectRoot | Select-Object -ExpandProperty Path

# Convert Windows path to WSL path (D:\apusic\... -> /mnt/d/apusic/...)
$WslRoot = ($ProjectRoot -replace '^([A-Za-z]):', '/mnt/$1' -replace '\\', '/').ToLower()

$bashScriptTemplate = @'
set -e
cd "__WSLROOT__"

echo "=========================================="
echo " Building tjyj-api-service in WSL"
echo " Project root: __WSLROOT__"
echo "=========================================="

# Ensure Go binary path is present before we check
export PATH=/usr/local/go/bin:$HOME/go/bin:$PATH

# Use China-accessible module proxy and disable toolchain auto-download
export GOPROXY=https://goproxy.cn,direct
export GOSUMDB=off
export GOTOOLCHAIN=local

# ------------------ Install Go if missing or version too old ------------------
MIN_GO_VERSION="1.22"

version_ge() {
    # Return 0 if $1 >= $2
    [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | head -n1)" = "$2" ]
}

CURRENT_GO_VERSION=$(go version 2>/dev/null | awk '{print $3}' | sed 's/go//')

if [ -z "$CURRENT_GO_VERSION" ] || ! version_ge "$CURRENT_GO_VERSION" "$MIN_GO_VERSION"; then
    echo "[WSL] Installing Go >= $MIN_GO_VERSION (current: ${CURRENT_GO_VERSION:-none})..."

    GO_VERSION="1.22.12"
    GO_TARBALL="go${GO_VERSION}.linux-amd64.tar.gz"
    GO_MIRRORS=(
        "https://go.dev/dl"
        "https://mirrors.aliyun.com/golang"
    )

    download_go() {
        for mirror in "${GO_MIRRORS[@]}"; do
            echo "[WSL] Trying mirror: $mirror"
            rm -f "/tmp/$GO_TARBALL"
            if wget -q --timeout=60 --tries=2 "$mirror/$GO_TARBALL" -O "/tmp/$GO_TARBALL"; then
                if tar -tzf "/tmp/$GO_TARBALL" >/dev/null 2>&1; then
                    echo "[WSL] Downloaded valid tarball from $mirror"
                    return 0
                fi
                echo "[WSL] Tarball from $mirror is invalid"
            else
                echo "[WSL] Download from $mirror failed"
            fi
        done
        return 1
    }

    if ! download_go; then
        echo "[WSL] Falling back to apt install..."
        __SUDO__apt-get update
        __SUDO__apt-get install -y golang-go
    fi

    if [ -f "/tmp/$GO_TARBALL" ]; then
        __SUDO__rm -rf /usr/local/go
        __SUDO__tar -C /usr/local -xzf "/tmp/$GO_TARBALL"
    else
        echo "[WSL] No valid Go tarball available; apt install should have provided Go"
    fi
else
    echo "[WSL] Found Go $CURRENT_GO_VERSION (>= $MIN_GO_VERSION)"
fi

echo "[WSL] Go version: $(go version)"

# ------------------ Download dependencies ------------------
echo "[WSL] Downloading Go modules..."
go mod tidy

# ------------------ Clean previous builds ------------------
echo "[WSL] Cleaning previous builds..."
rm -rf dist
mkdir -p dist

# ------------------ Cross-compile ------------------
APP_NAME="tjyj-api-service"
VERSION="__VERSION__"
BUILD_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

declare -a TARGETS=(
    "linux amd64"
    "linux arm64"
    "windows amd64"
    "windows arm64"
)

for target in "${TARGETS[@]}"; do
    GOOS=$(echo "$target" | awk '{print $1}')
    GOARCH=$(echo "$target" | awk '{print $2}')
    OUTPUT="dist/${APP_NAME}_${GOOS}_${GOARCH}"
    if [ "$GOOS" = "windows" ]; then
        OUTPUT="${OUTPUT}.exe"
    fi

    echo "[WSL] Building $GOOS/$GOARCH -> $OUTPUT"
    CGO_ENABLED=0 GOOS="$GOOS" GOARCH="$GOARCH" \
        go build -ldflags "-s -w -X main.version=${VERSION} -X main.buildTime=${BUILD_TIME}" -o "$OUTPUT" .
done

# ------------------ Package Linux releases ------------------
echo "[WSL] Packaging Linux releases..."

rm -rf release
mkdir -p release

package_linux() {
    local arch=$1
    local binary=$2
    local dir="release/tjyj-api-service-linux-${arch}"
    rm -rf "$dir"
    mkdir -p "$dir"
    cp "dist/$binary" "$dir/"
    cp "systemd/env.conf" "$dir/"
    cp "systemd/tjyj-api-service.service" "$dir/"
    cp "scripts/install.sh" "$dir/"
    cp "scripts/upgrade.sh" "$dir/"
    chmod +x "$dir/install.sh"
    chmod +x "$dir/upgrade.sh"
    tar -czvf "release/tjyj-api-service-linux-${arch}.tar.gz" -C release "tjyj-api-service-linux-${arch}"
}

package_linux amd64 tjyj-api-service_linux_amd64
package_linux arm64 tjyj-api-service_linux_arm64

echo ""
echo "=========================================="
echo " Build complete!"
echo " Binaries: __WSLROOT__/dist"
echo " Packages: __WSLROOT__/release"
echo "=========================================="
ls -lh "__WSLROOT__/dist/"
echo ""
ls -lh "__WSLROOT__/release/"
'@

# Replace placeholders with actual values
$bashScript = $bashScriptTemplate.
    Replace('__WSLROOT__', $WslRoot).
    Replace('__VERSION__', $Version)

# Handle sudo password for non-interactive installs in WSL.
if ($SudoPassword) {
    $bashScript = $bashScript.Replace('__SUDO__', 'echo "$SUDO_PASSWORD" | sudo -S ')
} else {
    $bashScript = $bashScript.Replace('__SUDO__', 'sudo ')
}

# Write bash script to a temp file using UTF-8 without BOM.
$TempFile = Join-Path $env:TEMP "build-tjyj-api-service-wsl.sh"
[System.IO.File]::WriteAllText($TempFile, $bashScript, [System.Text.UTF8Encoding]::new($false))

# Convert temp file path to WSL path
$WslTempFile = ($TempFile -replace '^([A-Za-z]):', '/mnt/$1' -replace '\\', '/').ToLower()

Write-Host "[PowerShell] Entering WSL to build at: $WslRoot" -ForegroundColor Cyan

# Execute via temp file inside WSL
if ($SudoPassword) {
    $escapedPassword = $SudoPassword -replace "'", "'\''"
    wsl -e bash -c "SUDO_PASSWORD='$escapedPassword' bash '$WslTempFile'"
    $SudoPassword = $null
    $escapedPassword = $null
} else {
    wsl bash "$WslTempFile"
}

$exitCode = $LASTEXITCODE
Remove-Item -Path $TempFile -ErrorAction SilentlyContinue

if ($exitCode -ne 0) {
    Write-Host "[PowerShell] Build failed with exit code $exitCode" -ForegroundColor Red
    exit $exitCode
}

Write-Host "[PowerShell] Done. Check $ProjectRoot\dist and $ProjectRoot\release for artifacts." -ForegroundColor Green
