@echo off

echo Invoking Powershell to install chocolatey if not already installed,
echo and then to install command-line tools used by PA4...

powershell -Command "& {Set-ExecutionPolicy Bypass -Scope Process -Force ; %~dp0install-tools.ps1 %*}"
