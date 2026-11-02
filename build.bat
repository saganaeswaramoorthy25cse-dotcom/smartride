@echo off
setlocal
echo =====================================================================
echo Compiling and Deploying SMARTRIDE to Apache Tomcat
echo =====================================================================

set TOMCAT_HOME=C:\xampp\tomcat
set APP_DIR=C:\smart new

if not exist "%APP_DIR%\web\WEB-INF\classes" (
    mkdir "%APP_DIR%\web\WEB-INF\classes"
)

echo [1/3] Compiling Java source files...
powershell -NoProfile -Command "$cp = '%TOMCAT_HOME%\lib\servlet-api.jar;%TOMCAT_HOME%\lib\jsp-api.jar;%APP_DIR%\web\WEB-INF\lib\sqlite-jdbc.jar'; $out = '%APP_DIR%\web\WEB-INF\classes'; $files = (Get-ChildItem -Path '%APP_DIR%\src\smartride\*.java').FullName; & javac -encoding UTF-8 -cp $cp -d $out $files"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)
echo [OK] Compilation successful.

echo [2/3] Syncing files to Tomcat webapps\smartride...
if not exist "%TOMCAT_HOME%\webapps\smartride" (
    mkdir "%TOMCAT_HOME%\webapps\smartride"
)
xcopy /E /Y /I "%APP_DIR%\web\*" "%TOMCAT_HOME%\webapps\smartride\" > nul
echo [OK] Deployment to Tomcat webapps complete.

echo [3/3] Ready! Access application at: http://localhost:8080/smartride/
echo =====================================================================
