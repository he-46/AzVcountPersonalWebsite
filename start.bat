@echo off
setlocal
cd /d "%~dp0"

echo Starting AzV Personal Website...

if not exist "backend\pom.xml" (
  echo [ERROR] backend\pom.xml not found. Run this file from the project root.
  pause
  exit /b 1
)
if not exist "frontend\package.json" (
  echo [ERROR] frontend\package.json not found. Run this file from the project root.
  pause
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java was not found in PATH.
  pause
  exit /b 1
)
where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js was not found in PATH.
  pause
  exit /b 1
)
where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm was not found in PATH.
  pause
  exit /b 1
)

if not exist "backend\.env" if not exist "backend\src\main\resources\application.yml" if not exist "backend\src\main\resources\application.properties" (
  echo [WARN] No backend .env or application config found. Check required environment variables before startup.
)

echo Launching backend in a new window...
if exist "backend\mvnw.cmd" (
  start "AzV Website Backend" cmd /k "cd /d ""%~dp0backend"" && call mvnw.cmd spring-boot:run"
) else (
  where mvn >nul 2>nul
  if errorlevel 1 (
    echo [ERROR] Maven wrapper and Maven were not found.
    pause
    exit /b 1
  )
  start "AzV Website Backend" cmd /k "cd /d ""%~dp0backend"" && mvn spring-boot:run"
)

echo Launching frontend in a new window...
if not exist "frontend\node_modules" (
  echo Installing frontend dependencies...
  pushd "frontend"
  call npm install
  if errorlevel 1 (
    popd
    echo [ERROR] npm install failed. Backend is still running; see its window for details.
    pause
    exit /b 1
  )
  popd
)
start "AzV Website Frontend" cmd /k "cd /d ""%~dp0frontend"" && npm run dev"

echo.
echo Both services were launched in separate windows.
echo Close those windows to stop the services.
echo Check the frontend window for the local URL.
pause
endlocal
