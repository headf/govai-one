@echo off
REM ===========================================================================
REM  GovAI-One Citizen Portal - backend (Spring Boot, port 8083)
REM ---------------------------------------------------------------------------
REM  Runtime: uses java from PATH. On this machine that is JDK 22
REM           (D:\download\jdk-22).
REM
REM  The previous version of this script hardcoded JDK 17 at
REM    C:\govai-tools\apps\jdk-17.0.20.1+1\bin\java.exe
REM  and a jar path under
REM    C:\Users\<other-user>\Desktop\...
REM  Neither exists on this machine, so the script failed immediately.
REM  Rewritten to use PATH + paths relative to this script (%~dp0).
REM
REM  See README.md, section "Runtime versions", for the Chinese explanation.
REM ===========================================================================

set "ROOT=%~dp0"
set "JAR=%ROOT%govai-citizen-server\target\govai-citizen-server-1.0.0.jar"

if not exist "%JAR%" (
  echo [ERROR] jar not found:
  echo         %JAR%
  echo         Build it first:  cd govai-citizen-server ^&^& mvn -DskipTests package
  exit /b 1
)

echo [start] citizen backend on port 8083 ...
start "govai-citizen-backend" java -jar "%JAR%"

echo [info] API docs : http://localhost:8083/doc.html
echo [info] make sure database govai_citizen is initialized (sql\init.sql)
