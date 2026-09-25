@echo off
REM Network Device Management System - one-click deploy (Windows)
REM Usage: deploy.bat [up^|logs^|stop^|down^|reset]

setlocal enabledelayedexpansion
cd /d "%~dp0"

set ACTION=%1
if "%ACTION%"=="" set ACTION=up

where docker >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker not found. Install Docker Desktop first: https://www.docker.com/products/docker-desktop
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker is not running. Please start Docker Desktop and retry.
  exit /b 1
)

set COMPOSE=docker compose
docker compose version >nul 2>&1
if errorlevel 1 set COMPOSE=docker-compose

if not exist ".env" (
  echo [INFO] .env not found, creating from .env.example ...
  copy /y ".env.example" ".env" >nul
  echo [WARN] Please change MYSQL_ROOT_PASSWORD and JWT_SECRET in .env for production.
)

if /i "%ACTION%"=="logs" (
  %COMPOSE% logs -f --tail=100
  goto :eof
)

if /i "%ACTION%"=="stop" (
  echo [INFO] Stopping services ...
  %COMPOSE% stop
  goto :eof
)

if /i "%ACTION%"=="down" (
  echo [INFO] Stopping and removing containers ^(volumes kept^) ...
  %COMPOSE% down
  goto :eof
)

if /i "%ACTION%"=="reset" (
  echo [WARN] This deletes the database and uploaded files permanently.
  set /p CONFIRM=Type yes to continue: 
  if /i "!CONFIRM!"=="yes" (
    %COMPOSE% down -v
    echo [INFO] Done. Run deploy.bat again for a clean deployment.
  ) else (
    echo [INFO] Cancelled.
  )
  goto :eof
)

if /i not "%ACTION%"=="up" (
  echo [ERROR] Unknown command: %ACTION% ^(available: up / logs / stop / down / reset^)
  exit /b 1
)

echo [INFO] Building and starting containers ^(first run takes 5-10 minutes^) ...
%COMPOSE% up -d --build
if errorlevel 1 (
  echo [ERROR] Deployment failed. Run "deploy.bat logs" to inspect.
  exit /b 1
)

echo [INFO] Waiting for MySQL to become ready ...
for /l %%i in (1,1,60) do (
  docker exec network-device-mysql mysqladmin ping -h 127.0.0.1 -u root -proot123 --silent >nul 2>&1
  if not errorlevel 1 goto :ready
  timeout /t 2 /nobreak >nul
)
echo [WARN] MySQL health check timed out, but containers may still be starting.

:ready
echo.
%COMPOSE% ps
echo.
echo [INFO] Deployment finished.
echo   Frontend : http://localhost
echo   API docs : http://localhost:8080/doc.html
echo   Account  : admin / admin123
echo.
echo   Logs  : deploy.bat logs
echo   Stop  : deploy.bat stop

endlocal
