@echo off
setlocal enabledelayedexpansion

call gradlew.bat clean build -x test || exit /b

for %%f in (build\libs\*.jar) do (
    copy "%%f" git-conflict-detector.jar >nul
)

echo Build completed. JAR created: git-conflict-detector.jar
