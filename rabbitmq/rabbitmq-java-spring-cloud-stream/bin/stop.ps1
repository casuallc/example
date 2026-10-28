# Stop the demo by terminating the process listening on the HTTP port.
# Usage: .\stop.ps1 [-Port 8080]  (default: read from the config file next to
# this script, fallback 8080; HTTP_PORT env var also works)
param([int]$Port = 0)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if ($Port -eq 0 -and $env:HTTP_PORT) { $Port = [int]$env:HTTP_PORT }

# Best-effort port extraction from the config file next to this script.
if ($Port -eq 0 -and (Test-Path 'config.json')) {
    $m = Select-String -Path 'config.json' -Pattern '"httpAddr"\s*:\s*"[^"]*:(\d+)"' | Select-Object -First 1
    if ($m) { $Port = [int]$m.Matches[0].Groups[1].Value }
}
if ($Port -eq 0 -and (Test-Path 'application.yml')) {
    $inServer = $false
    foreach ($line in Get-Content 'application.yml') {
        if ($line -match '^server:') { $inServer = $true; continue }
        if ($line -match '^\S') { $inServer = $false }
        if ($inServer -and $line -match '^\s+port:\s*(\d+)') { $Port = [int]$Matches[1]; break }
    }
}
if ($Port -eq 0 -and (Test-Path 'appsettings.json')) {
    $m = Select-String -Path 'appsettings.json' -Pattern '"Url"\s*:\s*"[^"]*:(\d+)"' | Select-Object -First 1
    if ($m) { $Port = [int]$m.Matches[0].Groups[1].Value }
}
if ($Port -eq 0) { $Port = 8080 }

$pids = @()
try {
    $pids = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction Stop |
        Select-Object -ExpandProperty OwningProcess -Unique)
} catch {
    $pids = @(netstat -ano | Select-String 'LISTENING' |
        Where-Object { $_ -match "[:.]$Port\s" } |
        ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique)
}

if ($pids.Count -eq 0) {
    Write-Host "No process listening on port $Port"
    exit 0
}
foreach ($procId in $pids) {
    Write-Host "Stopping PID $procId listening on port $Port"
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
}
