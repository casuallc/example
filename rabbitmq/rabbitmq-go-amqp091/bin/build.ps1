# Build and assemble the distributable package into dist\, then zip everything
# (source code + binary + scripts + config) into dist\rabbitmq-go-amqp091-<version>.zip
$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

$version = '1.0.0'
$artifact = "rabbitmq-go-amqp091-$version"
$binName = 'rabbitmq-go-amqp091.exe'

$env:CGO_ENABLED = '0'
go build -trimpath -o $binName .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$dist = Join-Path (Get-Location) 'dist'
$pkg = Join-Path $dist $artifact
if (Test-Path $dist) { Remove-Item -Recurse -Force $dist }
New-Item -ItemType Directory $pkg | Out-Null

# Runnable part: binary + start scripts + config
Copy-Item $binName $pkg
Copy-Item bin\start.sh, bin\start.ps1, bin\stop.sh, bin\stop.ps1 $pkg
Copy-Item config\config.json.example (Join-Path $pkg 'config.json')

# Source part
Copy-Item go.mod, go.sum, main.go, README.md $pkg
Copy-Item -Recurse rabbit, bin, config $pkg

$zipPath = Join-Path $dist "$artifact.zip"
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path $pkg -DestinationPath $zipPath

Write-Host "Build finished:"
Get-Item $zipPath | Select-Object Name, Length
