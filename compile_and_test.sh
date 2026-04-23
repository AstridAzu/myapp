#!/bin/bash

# Script de Compilación y Testing - Sincronización de Ejercicios Base
# Ejecutar desde la raíz del proyecto

set -e  # Exit on error

echo "=========================================="
echo "STEP 1: Limpiar build previos"
echo "=========================================="
./gradlew clean

echo ""
echo "=========================================="
echo "STEP 2: Compilar proyecto (build)"
echo "=========================================="
./gradlew build --info

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ COMPILACIÓN EXITOSA"
    echo ""
    echo "=========================================="
    echo "STEP 3: Generar APK Debug"
    echo "=========================================="
    ./gradlew assembleDebug
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ APK GENERADO EXITOSAMENTE"
        echo "Ubicación: app/build/outputs/apk/debug/app-debug.apk"
        echo ""
        echo "=========================================="
        echo "STEP 4: Instalar en emulador (si está corriendo)"
        echo "=========================================="
        
        if adb devices | grep -q "emulator"; then
            echo "Emulador detectado. Instalando..."
            adb install -r app/build/outputs/apk/debug/app-debug.apk
            echo "✅ APK instalado"
        else
            echo "⚠️  No hay emulador/dispositivo conectado"
            echo "Instalar manualmente con:"
            echo "adb install -r app/build/outputs/apk/debug/app-debug.apk"
        fi
    else
        echo "❌ ERROR en assembleDebug"
        exit 1
    fi
else
    echo "❌ ERROR en compilación"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ SCRIPT COMPLETADO"
echo "=========================================="
echo ""
echo "Próximos pasos:"
echo "1. Abrir Logcat: View → Tool Windows → Logcat"
echo "2. Filtrar por: BaseExercisesSync|SyncWorker"
echo "3. Hacer login en la app"
echo "4. Validar 47 ejercicios en logs y Database Inspector"
