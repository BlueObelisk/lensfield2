@REM Lensfield Start-up File
@REM Copyright (c) Sam Adams 2010
@REM Inspired by Apache Maven Project

@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir

@echo off
@setLocal

set ERROR_CODE=0

@REM ==== CHECK JAVA_HOME ====
if not "%JAVA_HOME%" == "" goto OkJHome
echo.
echo ERROR: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:OkJHome
@REM ==== CHECK java.exe ====
if exist "%JAVA_HOME%\bin\java.exe" goto OkJavaExe
echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = "%JAVA_HOME%"
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:OkJavaExe
if not "%LF_HOME%" == "" goto StripLfHome
@REM ==== Find lensfield installation ====
set "LF_HOME=%~dp0.."
goto CheckLfHome

:StripLfHome
@REM ==== Strip trailing slash ====
if not "_%LF_HOME:~-1%"=="_\" goto CheckLfHome
set "LF_HOME=%LF_HOME:~0,-1%"
goto stripLfHome


:CheckLfHome
@REM ==== CHECK LF_HOME ====
if exist "%LF_HOME%\bin\lf.bat" goto CheckLfOpts
echo.
echo ERROR: LF_HOME is set to an invalid directory.
echo LF_HOME = "%LF_HOME%"
echo Please set the LF_HOME variable in your environment to match the
echo location of the Lensfield installation
echo.
goto error


:CheckLfOpts
if not "%LF_OPTS%" == "" goto init
set "LF_OPTS=-Xmx512m"

:init
"%JAVA_HOME%/bin/java" %LF_OPTS% -classpath "%LF_HOME%\boot\boot-${lensfield.version}.jar" -Dlensfield.home="%LF_HOME%" org.lensfield.launcher.boot.Bootstrap %*

if ERRORLEVEL 1 goto error
goto exit


:error
SET ERROR_CODE=1
goto exit

:exit
@endlocal
cmd /C exit /B %ERROR_CODE%
