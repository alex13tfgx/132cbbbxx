@echo off
setlocal
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out\classes
dir /s /b src\*.java > out\sources.txt
javac -encoding UTF-8 -d out\classes @out\sources.txt || exit /b 1
copy /y res\icon.png out\classes\icon.png >nul
if exist res\background.png copy /y res\background.png out\classes\background.png >nul
if exist res\launcher.json copy /y res\launcher.json out\classes\launcher.json >nul
xcopy /y /e /i res\avatars out\classes\avatars >nul
(echo Main-Class: guardlisten.Main& echo Class-Path: lib/webp-imageio.jar webp-imageio.jar) > out\manifest.txt
jar cfm GuardListen.jar out\manifest.txt -C out\classes . || exit /b 1
echo Built GuardListen.jar
