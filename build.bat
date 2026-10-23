@echo off
echo ========================================================
echo   COMPILING VELOCITY X (JAVA SE 17+)
echo ========================================================

if not exist "bin" mkdir bin

javac -encoding UTF-8 -cp "lib\sqlite-jdbc-3.53.4.0-natives-all.jar" -d bin src\com\velocityx\model\*.java src\com\velocityx\db\*.java src\com\velocityx\security\*.java src\com\velocityx\util\*.java src\com\velocityx\repository\*.java src\com\velocityx\service\*.java src\com\velocityx\controller\*.java src\com\velocityx\Main.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed with status %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Build finished. Class files generated in bin\
pause