# Install dependencies and assemble the distributable package into dist\, then
# zip everything (source + node_modules + scripts + config) into
# dist\rabbitmq-nodejs-amqplib-<version>.zip
$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

$version = '1.0.0'
$artifact = "rabbitmq-nodejs-amqplib-$version"

# Install production dependencies (generates package-lock.json on first run).
npm ci --omit=dev
if ($LASTEXITCODE -ne 0) {
    npm install --omit=dev
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$dist = Join-Path (Get-Location) 'dist'
$pkg = Join-Path $dist $artifact
if (Test-Path $dist) { Remove-Item -Recurse -Force $dist }
New-Item -ItemType Directory $pkg | Out-Null

# Runnable part: node_modules + start scripts + config
Copy-Item -Recurse node_modules $pkg
Copy-Item bin\start.sh, bin\start.ps1, bin\stop.sh, bin\stop.ps1 $pkg
Copy-Item config\config.json.example (Join-Path $pkg 'config.json')

# Source part
Copy-Item package.json, README.md $pkg
if (Test-Path package-lock.json) { Copy-Item package-lock.json $pkg }
Copy-Item -Recurse src, bin, config $pkg

$zipPath = Join-Path $dist "$artifact.zip"
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path $pkg -DestinationPath $zipPath

Write-Host "Build finished:"
Get-Item $zipPath | Select-Object Name, Length
