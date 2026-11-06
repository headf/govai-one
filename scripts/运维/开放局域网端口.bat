@echo off
chcp 65001 >nul
echo ============================================================
echo   GovAI One 智慧政务系统 —— 局域网端口放行
echo   （需要管理员权限，请右键"以管理员身份运行"）
echo ============================================================
echo.

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 没有管理员权限！
    echo        请关闭本窗口，右键此文件 -^> "以管理员身份运行"
    echo.
    pause
    exit /b 1
)

echo [1/3] 放行前端端口 5173-5176 ...
netsh advfirewall firewall delete rule name="GovAI-One 前端(5173-5176)" >nul 2>&1
netsh advfirewall firewall add rule name="GovAI-One 前端(5173-5176)" dir=in action=allow protocol=TCP localport=5173-5176 >nul
if %errorlevel%==0 (echo      完成) else (echo      失败)

echo [2/3] 放行后端端口 8081-8084 ...
netsh advfirewall firewall delete rule name="GovAI-One 后端(8081-8084)" >nul 2>&1
netsh advfirewall firewall add rule name="GovAI-One 后端(8081-8084)" dir=in action=allow protocol=TCP localport=8081-8084 >nul
if %errorlevel%==0 (echo      完成) else (echo      失败)

echo [3/3] 放行数据库/中间件 3306,6379,5672 ...
netsh advfirewall firewall delete rule name="GovAI-One 中间件(3306,6379,5672)" >nul 2>&1
netsh advfirewall firewall add rule name="GovAI-One 中间件(3306,6379,5672)" dir=in action=allow protocol=TCP localport=3306,6379,5672 >nul
if %errorlevel%==0 (echo      完成) else (echo      失败)

echo.
echo ============================================================
echo   放行完成！局域网内其它设备可直接访问：
echo ------------------------------------------------------------
echo     审批端   http://10.25.135.241:5173
echo     群众端   http://10.25.135.241:5174
echo     企业端   http://10.25.135.241:5175
echo     监管端   http://10.25.135.241:5176
echo ------------------------------------------------------------
echo   演示账号（密码均 123456）：
echo     企业 yunqi_admin / 审批 approver1 / 监管 supervisor1 / 群众 citizen01
echo ============================================================
echo.
echo   提示：本机 IP 若变化（换网络/重连 WiFi），地址里的 10.25.135.241
echo         需要重新用 ipconfig 查看（通常是 10.x 或 192.168.x 开头）。
echo.
pause
