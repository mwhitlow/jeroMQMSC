@echo off
REM Command script to start MessageLogger 
REM Author:  Marc Whitlow 
REM 
REM Filename:  runMessageLogger.cmd 
REM Location:  C:\Users\tlims\jeroMQMSC\scripts
REM Input variables: 
REM 1: The URL that the logger will be bound to, e.g. tcp://127.0.0.1:5555 
REM 2: The topic that logger monitors, e.g. Project_Log 
REM 3: The URL of the log file, e.g. /var/log/zeroMQcore/project.log  

SETLOCAL ENABLEEXTENSIONS
SET me=%~n0
SET parent=%~pd0
SET socketURL=%1
SET topic=%2
SET logFileURL=%3

ECHO %me% PWD:          %parent%
ECHO.%me% Socket URL:   %socketURL% 
ECHO.%me% Topic:        %topic% 
ECHO.%me% Log File URL: %logFileURL% 
ECHO.%me%
ECHO.%me% Run MessageLogger

java -jar ../MessageLogger.jar %socketURL% %topic% %logFileURL%