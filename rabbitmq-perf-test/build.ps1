param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$TargetDir = Join-Path $ProjectDir "target"
$DistDir = Join-Path $ProjectDir "dist"
$PackageName = "rabbitmq-perf-test-$Version"
$PackageDir = Join-Path $DistDir $PackageName
$TarFile = Join-Path $DistDir "$PackageName.tar.gz"

Write-Host "=== Building rabbitmq-perf-test Package ===" -ForegroundColor Cyan
Write-Host "Version: $Version"

# Step 1: Compile with Maven
Write-Host "`n[1/5] Compiling with Maven..." -ForegroundColor Yellow
& mvn clean package -DskipTests -f "$ProjectDir\pom.xml"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed"
    exit 1
}

# Step 2: Clean and create package directory
Write-Host "`n[2/5] Preparing package directory..." -ForegroundColor Yellow
if (Test-Path $DistDir) {
    Remove-Item -Recurse -Force $DistDir
}
New-Item -ItemType Directory -Path $PackageDir | Out-Null
New-Item -ItemType Directory -Path (Join-Path $PackageDir "bin") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $PackageDir "lib") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $PackageDir "config") | Out-Null
New-Item -ItemType Directory -Path (Join-Path $PackageDir "logs") | Out-Null

# Step 3: Copy files
Write-Host "`n[3/5] Copying files..." -ForegroundColor Yellow

# Fat JAR
Copy-Item -Path "$TargetDir\rabbitmq-perf-test-$Version-jar-with-dependencies.jar" -Destination (Join-Path $PackageDir "lib")

# Config
Copy-Item -Path "$ProjectDir\config\*.properties" -Destination (Join-Path $PackageDir "config")

# Scripts（统一转为 LF 行尾，避免 Linux 上执行报错）
foreach ($script in @("start.sh", "stop.sh")) {
    $src = Join-Path $ProjectDir "bin\$script"
    $dst = Join-Path $PackageDir "bin\$script"
    $content = [System.IO.File]::ReadAllText($src) -replace "`r`n", "`n"
    [System.IO.File]::WriteAllText($dst, $content, (New-Object System.Text.UTF8Encoding $false))
}

# README
Copy-Item -Path "$ProjectDir\README.md" -Destination $PackageDir

# Step 4: Create tar.gz（使用 Windows 自带的 bsdtar）
Write-Host "`n[4/5] Creating tar.gz archive..." -ForegroundColor Yellow
& tar -czf "$TarFile" -C "$DistDir" "$PackageName"
if ($LASTEXITCODE -ne 0) {
    Write-Error "tar failed"
    exit 1
}

# Step 5: Cleanup
Write-Host "`n[5/5] Cleaning up..." -ForegroundColor Yellow
Remove-Item -Recurse -Force $PackageDir

Write-Host "`n=== Build Complete ===" -ForegroundColor Green
Write-Host "Output: $TarFile"
