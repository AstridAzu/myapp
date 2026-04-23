@echo off
REM Script de Compilación y Testing - Sincronización de Ejercicios Base (Windows)
REM Ejecutar desde la raíz del proyecto

setlocal enabledelayedexpansion

echo ==========================================
echo STEP 1: Limpiar build previos
echo ==========================================
call gradlew.bat clean

if errorlevel 1 (
    echo ❌ ERROR en clean
    exit /b 1
)

echo.
echo ==========================================
echo STEP 2: Compilar proyecto (build)
echo ==========================================
call gradlew.bat build --info

if errorlevel 1 (
    echo ❌ ERROR en compilación
    exit /b 1
)

echo.
echo ✅ COMPILACIÓN EXITOSA
echo.
echo ==========================================
echo STEP 3: Generar APK Debug
echo ==========================================
call gradlew.bat assembleDebug

if errorlevel 1 (
    echo ❌ ERROR en assembleDebug
    exit /b 1
)

echo.
echo ✅ APK GENERADO EXITOSAMENTE
echo Ubicación: app\build\outputs\apk\debug\app-debug.apk
echo.
echo ==========================================
echo STEP 4: Detectar emulador/dispositivo
echo ==========================================

adb devices | find "emulator" >nul
if errorlevel 1 (
    echo ⚠️  No hay emulador/dispositivo conectado
    echo Instalar manualmente con:
    echo adb install -r app\build\outputs\apk\debug\app-debug.apk
) else (
    echo Emulador detectado. Instalando...
    call adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo ✅ APK instalado
)

echo.
echo ==========================================
echo ✅ SCRIPT COMPLETADO
echo ==========================================
echo.
echo Próximos pasos:
echo 1. Abrir Android Studio
echo 2. View → Tool Windows → Logcat
echo 3. Filtrar por: BaseExercisesSync o SyncWorker
echo 4. Abrir app y hacer login
echo 5. Validar logs: "Base exercises sync complete. Final count: 47"
echo 6. View → Tool Windows → Database Inspector
echo 7. Verificar tabla "ejercicios" con 47 registros
