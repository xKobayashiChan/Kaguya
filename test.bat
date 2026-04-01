@echo off
title LexiysAddons - Build and Run
cd /d "%~dp0"

echo ========================================
echo   LexiysAddons Build ^& Test
echo ========================================
echo.

echo [1/3] Cleaning old jars from run/mods...
if exist "run\mods\lexiyaddons*.jar" del /q "run\mods\lexiyaddons*.jar"

echo [2/3] Building...
call gradlew.bat build -x test
if errorlevel 1 (
    echo.
    echo *** BUILD FAILED ***
    pause
    exit /b 1
)

echo [3/3] Copying jar to run/mods...
if not exist "run\mods" mkdir "run\mods"
copy /y "build\libs\lexiyaddons-1.0.jar" "run\mods\" >nul

echo.
echo ========================================
echo   Build OK! Starting Minecraft...
echo ========================================
echo.

call gradlew.bat runClient --no-daemon
pause
