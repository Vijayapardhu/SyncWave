# Bootstrap the Gradle wrapper for the Android project.
# This is needed exactly once, after cloning or after this scaffold is generated.
#
# Usage (Windows PowerShell):
#     .\tools\bootstrap-wrapper.ps1
#
# Usage (POSIX):
#     ./tools/bootstrap-wrapper.sh

$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

if (Get-Command gradle -ErrorAction SilentlyContinue) {
    Write-Host "Running 'gradle wrapper'..."
    & gradle wrapper --gradle-version 8.7 --distribution-type bin
    Write-Host "Done. Try:  .\gradlew.bat --version"
    exit 0
}

Write-Host "No system 'gradle' on PATH."
Write-Host "Either install Gradle 8.7 (https://gradle.org/install/) and re-run,"
Write-Host "or open the project in Android Studio and let it sync once — that will"
Write-Host "generate the wrapper for you."
exit 1
