@echo off
setlocal

if "%FUNCTION_CATALOG_GITHUB_TOKEN%"=="" (
  echo Set FUNCTION_CATALOG_GITHUB_TOKEN for the explicitly authorized GitHub staging scope. 1>&2
  exit /b 1
)
if "%FUNCTION_CATALOG_GITHUB_REPOSITORIES%"=="" (
  echo Set FUNCTION_CATALOG_GITHUB_REPOSITORIES to a comma-separated owner/repo allowlist. 1>&2
  exit /b 1
)

cd /d "%~dp0\..\.."
if not exist "distribution\application.jar" (
  echo Function Catalog staged runtime is missing. Run scripts\ci\verify.cmd after PR #12, or gradlew.bat stageRuntime before reconciliation. 1>&2
  exit /b 1
)

java -cp "distribution\application.jar;distribution\libs\*" org.tavall.ai.mcp.server.RepositoryStagingMcpServerLauncher %*
exit /b %ERRORLEVEL%
