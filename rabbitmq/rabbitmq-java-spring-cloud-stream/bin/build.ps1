# Build and assemble the distributable package into dist\, then zip everything
# (source code + runnable jar + scripts + config) into dist\rabbitmq-java-spring-cloud-stream-<version>.zip
$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

$version = (mvn -q help:evaluate '-Dexpression=project.version' -DforceStdout | Select-Object -Last 1).Trim()
$artifact = "rabbitmq-java-spring-cloud-stream-$version"

mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$dist = Join-Path (Get-Location) 'dist'
$pkg = Join-Path $dist $artifact
if (Test-Path $dist) { Remove-Item -Recurse -Force $dist }
New-Item -ItemType Directory $pkg | Out-Null

# Runnable part: jar + start scripts + config (application.yml overrides built-in defaults)
Copy-Item target\rabbitmq-java-spring-cloud-stream-*.jar $pkg
Copy-Item bin\start.sh, bin\start.ps1, bin\stop.sh, bin\stop.ps1 $pkg
Copy-Item config\application.yml.example (Join-Path $pkg 'application.yml')

# Source part
Copy-Item pom.xml, README.md $pkg
Copy-Item -Recurse src, bin, config $pkg

$zipPath = Join-Path $dist "$artifact.zip"
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path $pkg -DestinationPath $zipPath

Write-Host "Build finished:"
Get-Item $zipPath | Select-Object Name, Length
