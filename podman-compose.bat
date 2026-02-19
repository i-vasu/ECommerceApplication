@echo off
setlocal enabledelayedexpansion

:: Convert current path to WSL path
set "win_path=%~dp0"
set "win_path=!win_path:\=/!"
set "win_path=!win_path:C:=/mnt/c!"
set "win_path=!win_path:c:=/mnt/c!"
set "win_path=!win_path:D:=/mnt/d!"
set "win_path=!win_path:d:=/mnt/d!"

:: Run podman-compose inside the podman machine
podman machine ssh "cd !win_path! && podman-compose %*"
