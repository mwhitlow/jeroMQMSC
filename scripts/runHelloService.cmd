@echo off
REM Command script to start HellowService 
REM Author:  Marc Whitlow 
REM 
REM Filename:  runHelloService.cmd 
REM Location:  C:\Users\tlims\jeroMQMSC\scripts
REM Input variables: 
REM 1: The URL that the service will be bound to, e.g. tcp://127.0.0.1:5557
REM 2: The URL that the logger will be bound to, e.g. tcp://127.0.0.1:5555 
REM 3: The topic that logger monitors, e.g. Project_Log 

SETLOCAL ENABLEEXTENSIONS
SET me=%~n0
SET parent=%~pd0
SET socketURL=%1
SET loggerURL=%2
SET topic=%3

ECHO %me% PWD:          %parent%
ECHO.%me% Socket URL:   %socketURL% 
ECHO.%me% Logger URL:   %loggerURL% 
ECHO.%me% Topic:        %topic% 
ECHO.%me%
ECHO.%me% Run HelloService

java -jar ../HelloService.jar %socketURL% %loggerURL% %topic% 