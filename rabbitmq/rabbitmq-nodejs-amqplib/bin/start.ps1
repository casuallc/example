# Start the demo. A config.json next to this script overrides the built-in defaults.
$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Error "node not found, Node.js 18+ is required"
    exit 1
}
if (-not (Test-Path 'node_modules\amqplib')) {
    Write-Error "node_modules not found, run bin\build.ps1 first"
    exit 1
}

& node src\server.js --config config.json @args
exit $LASTEXITCODE
