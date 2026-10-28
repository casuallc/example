# Start the demo. A config.json next to this script overrides the built-in defaults.
$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

$bin = 'rabbitmq-go-amqp091.exe'
if (-not (Test-Path $bin)) { $bin = 'rabbitmq-go-amqp091' }
if (-not (Test-Path $bin)) {
    Write-Error "rabbitmq-go-amqp091 binary not found, run bin\build.ps1 first"
    exit 1
}

& (Join-Path (Get-Location) $bin) @args
exit $LASTEXITCODE
