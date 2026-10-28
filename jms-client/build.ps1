param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$TargetDir = Join-Path $ProjectDir "target"
$LibDir = Join-Path $ProjectDir "lib"
$ConfigDir = Join-Path $ProjectDir "config"
$CertsDir = Join-Path $ProjectDir "certs"
$DistDir = Join-Path $ProjectDir "dist"
$PackageName = "jms-client-$Version"
$PackageDir = Join-Path $DistDir $PackageName
$ZipFile = Join-Path $DistDir "$PackageName.zip"

Write-Host "=== Building admq Client Package ===" -ForegroundColor Cyan
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
New-Item -ItemType Directory -Path (Join-Path $PackageDir "certs") | Out-Null

# Step 3: Copy files
Write-Host "`n[3/5] Copying files..." -ForegroundColor Yellow

# JARs
Copy-Item -Path "$LibDir\*.jar" -Destination (Join-Path $PackageDir "lib")
Copy-Item -Path "$TargetDir\jms-client-$Version.jar" -Destination (Join-Path $PackageDir "lib")

# Configs
Copy-Item -Path "$ConfigDir\*.properties" -Destination (Join-Path $PackageDir "config")

# Certificates (TLS / GMTLS)
Copy-Item -Path "$CertsDir\*" -Destination (Join-Path $PackageDir "certs") -Recurse

# Scripts
Copy-Item -Path "$ProjectDir\start.sh" -Destination (Join-Path $PackageDir "bin")
Copy-Item -Path "$ProjectDir\start.bat" -Destination (Join-Path $PackageDir "bin")

# README
Copy-Item -Path "$ProjectDir\README.md" -Destination $PackageDir

# Step 4: Create zip
Write-Host "`n[4/5] Creating zip archive..." -ForegroundColor Yellow
Compress-Archive -Path $PackageDir -DestinationPath $ZipFile -Force

# Step 5: Cleanup
Write-Host "`n[5/5] Cleaning up..." -ForegroundColor Yellow
Remove-Item -Recurse -Force $PackageDir

Write-Host "`n=== Build Complete ===" -ForegroundColor Green
Write-Host "Output: $ZipFile"
