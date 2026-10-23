@echo off
echo ========================================================
echo   STARTING VELOCITY X SERVER
echo ========================================================

java -cp "bin;lib\sqlite-jdbc-3.53.4.0.jar" com.velocityx.Main
pause