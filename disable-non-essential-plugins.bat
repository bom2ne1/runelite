@echo off
REM Disable non-essential RuneLite plugins for multi-boxing performance
REM Keeps only: BomGE Tracker, GE Filters, Entity Hider, Stretched Mode, FPS, Ultra Performance

echo Disabling non-essential plugins...
echo.

REM Essential plugins to keep (grep pattern)
set KEEP_PATTERN=bomgetracker\|gefilters\|entityhider\|stretchedmode\|fps\|ultraperformance\|screenshot

REM Change to plugin directory
cd runelite-client\src\main\java\net\runelite\client\plugins

REM Find and disable all non-essential plugins
for /d %%D in (*) do (
	if exist "%%D\%%DPlugin.java" (
		echo %%D | findstr /i "%KEEP_PATTERN%" >nul
		if errorlevel 1 (
			REM Not in keep list - disable it
			powershell -Command "(Get-Content '%%D\%%DPlugin.java') -replace 'enabledByDefault = true', 'enabledByDefault = false' | Set-Content '%%D\%%DPlugin.java'"
			echo   DISABLED: %%D
		) else (
			REM In keep list - leave enabled
			echo   KEEP: %%D
		)
	)
)

echo.
echo Done! Non-essential plugins disabled
echo Restart RuneLite to apply changes
pause
