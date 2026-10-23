@echo off
if exist "bin" rd /s /q bin
if exist "velocity_x.db" del /f /q velocity_x.db
if exist "velocity_x.db-wal" del /f /q velocity_x.db-wal
if exist "velocity_x.db-shm" del /f /q velocity_x.db-shm
echo [CLEAN] Removed binaries and database artifacts.