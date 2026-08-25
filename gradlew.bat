@echo off
setlocal

set DIR=%~dp0
if "%JAVA_HOME%"=="" (
  echo JAVA_HOME is not set. Please install a JDK and set JAVA_HOME.
  exit /b 1
)

if exist "%DIR%gradle\wrapper\gradle-wrapper.jar" goto run
echo gradle-wrapper.jar is missing.
echo Run: gradle wrapper --gradle-version 8.10.2
echo from the project root once Gradle is installed.
exit /b 1

:run
"%JAVA_HOME%\bin\java.exe" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
