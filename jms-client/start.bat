@echo off
setlocal

if "%~1"=="" (
    set CONFIG_FILE=config\queue-send.properties
) else (
    set CONFIG_FILE=%~1
)

cd /d "%~dp0\.."

if not exist "lib\" (
    echo Error: lib directory not found.
    exit /b 1
)

echo Starting admq Test Client with config: %CONFIG_FILE%
java -cp "lib\*" com.apusic.admq.example.jms.Main "%CONFIG_FILE%"
