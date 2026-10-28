# Start the demo. An appsettings.json next to this script overrides the built-in defaults.
$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

$dll = 'rabbitmq-dotnet-client.dll'
if (-not (Test-Path $dll)) {
    Write-Error "$dll not found, run bin\build.ps1 first"
    exit 1
}

& dotnet $dll @args
exit $LASTEXITCODE
