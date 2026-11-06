@echo off
REM ===========================================================================
REM  GovAI-One Citizen Portal - frontend H5 dev server (port 5174)
REM ---------------------------------------------------------------------------
REM  Runtime: uses npm from PATH. On this machine that is Node 22
REM           (D:\Program Files\nodejs).
REM
REM  The previous version of this script hardcoded Node 20 at
REM    C:\govai-tools\apps\node-v20.18.0-win-x64\npm.cmd
REM  which does not exist on this machine. Rewritten to use PATH + paths
REM  relative to this script (%~dp0).
REM
REM  NOTE: this launches the uni-app CLI copy (govai-citizen-ui) for quick H5
REM        debugging. The officially maintained frontend project is
REM        govai-citizen-ui-hx (HBuilderX standard project) -- open that folder
REM        in HBuilderX to run on the WeChat mini-program simulator or a device.
REM
REM  See README.md, section "Runtime versions", for the Chinese explanation.
REM ===========================================================================

set "ROOT=%~dp0"
set "UI=%ROOT%govai-citizen-ui"

if not exist "%UI%\package.json" (
  echo [ERROR] frontend project not found:
  echo         %UI%
  exit /b 1
)

if not exist "%UI%\node_modules" (
  echo [install] installing frontend dependencies ...
  call npm --prefix "%UI%" install
  if errorlevel 1 (
    echo [ERROR] npm install failed
    exit /b 1
  )
)

echo [start] H5 dev server on port 5174 ...
echo [info] open http://localhost:5174   (/api is proxied to 8083)
call npm --prefix "%UI%" run dev:h5
