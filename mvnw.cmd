@REM Maven Wrapper startup batch script
@echo off
setlocal
set "MAVEN_PROJECTBASEDIR=%~dp0."
if defined JAVA_HOME goto javaHomeSet
where java.exe >nul 2>&1
if errorlevel 1 goto noJavaHome
set "JAVA_EXE=java.exe"
goto javaReady
:javaHomeSet
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA_EXE%" goto noJava
:javaReady
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile '%WRAPPER_JAR%'"
  if errorlevel 1 exit /b 1
)
"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %errorlevel%
:noJavaHome
echo Java was not found. Install JDK 21 or set JAVA_HOME to its folder. 1>&2
exit /b 1
:noJava
echo JAVA_HOME does not point to a valid JDK. 1>&2
exit /b 1
