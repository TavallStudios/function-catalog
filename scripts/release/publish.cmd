@echo off
setlocal

where bash >nul 2>nul
if errorlevel 1 (
  echo Function Catalog publishing requires bash ^(Git Bash, WSL, or Tavall local execution^). 1>&2
  exit /b 1
)
bash "%~dp0publish" %*
exit /b %ERRORLEVEL%
