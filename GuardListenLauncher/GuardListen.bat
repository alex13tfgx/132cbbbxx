@echo off
cd /d "%~dp0"
if not exist GuardListen.jar call build.bat
start "" javaw -jar GuardListen.jar
