@echo off
cd /d "%~dp0"
call "%~dp0build.bat" || exit /b 1
if exist rt_tmp rmdir /s /q rt_tmp
jlink --add-modules ALL-MODULE-PATH --strip-debug --compress zip-6 --no-header-files --no-man-pages --output rt_tmp || exit /b 1
mkdir out\input
copy /y GuardListen.jar out\input\ >nul
copy /y lib\webp-imageio.jar out\input\ >nul
copy /y ..\build\libs\wiosvisual-1.0.0.jar out\input\wios.jar >nul
jpackage --type app-image --name GuardListen --input out\input --main-jar GuardListen.jar --main-class guardlisten.Main --icon res\icon.ico --runtime-image rt_tmp --dest out\pkg --vendor GuardListen --app-version 1.0 || exit /b 1
rmdir /s /q rt_tmp
