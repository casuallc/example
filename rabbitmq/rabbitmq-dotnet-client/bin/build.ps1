# Build and assemble the distributable package into dist\, then zip everything
# (source code + published binaries + scripts + config) into dist\rabbitmq-dotnet-client-<version>.zip
$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

$version = ([regex]::Match((Get-Content rabbitmq-dotnet-client.csproj -Raw), '<Version>([^<]+)</Version>')).Groups[1].Value.Trim()
$artifact = "rabbitmq-dotnet-client-$version"

dotnet publish -c Release
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$publishDir = Get-ChildItem artifacts\publish\rabbitmq-dotnet-client -Directory -Filter 'release*' | Select-Object -First 1
if (-not $publishDir) {
    Write-Error "publish output not found under artifacts\publish\"
    exit 1
}

$dist = Join-Path (Get-Location) 'dist'
$pkg = Join-Path $dist $artifact
if (Test-Path $dist) { Remove-Item -Recurse -Force $dist }
New-Item -ItemType Directory $pkg | Out-Null

# Runnable part: published binaries + start scripts + config
Copy-Item -Recurse (Join-Path $publishDir.FullName '*') $pkg
Copy-Item bin\start.sh, bin\start.ps1, bin\stop.sh, bin\stop.ps1 $pkg
Copy-Item config\appsettings.json.example (Join-Path $pkg 'appsettings.json')

# Source part
Copy-Item rabbitmq-dotnet-client.csproj, Program.cs, README.md $pkg
Copy-Item -Recurse Rabbit, bin, config $pkg

$zipPath = Join-Path $dist "$artifact.zip"
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path $pkg -DestinationPath $zipPath

Write-Host "Build finished:"
Get-Item $zipPath | Select-Object Name, Length
