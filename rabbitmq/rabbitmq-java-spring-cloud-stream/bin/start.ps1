# Start the demo. An application.yml next to this script overrides the built-in defaults
# (Spring Boot picks up .\application.yml from the working directory automatically).
$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

$jar = Get-ChildItem rabbitmq-java-spring-cloud-stream-*.jar | Select-Object -First 1
if (-not $jar) {
    Write-Error "no rabbitmq-java-spring-cloud-stream-*.jar found, run bin\build.ps1 first"
    exit 1
}

if (-not $env:JAVA_OPTS) { $env:JAVA_OPTS = '-Xmx512m' }

& java ($env:JAVA_OPTS -split ' ') -jar $jar.FullName @args
exit $LASTEXITCODE
