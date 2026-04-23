# 🧹 Limpieza de Base de Datos Local

**Objetivo:** Preparar app para testing desde cero con sincronización de Workers

**Estado:** ✅ Código limpiado (seeds removidos)

---

## ✨ Cambios Realizados

### ✅ DatabaseBuilder.kt - LIMPIADO
```diff
- Removidos 3 seeds heredados (ya no se llaman):
  ❌ seedEjercicios()              → Ejercicios ahora vienen de D1/Workers
  ❌ seedRutinasPreset()           → Rutinas también vienen de D1
  ❌ seedRutinaEjerciciosPreset()  → Links también vienen de D1

- Deshabilitado seed de usuarios test:
  ❌ seedUsuariosTest() → Comentado en onOpen()

+ Conservado (sigue siendo necesario):
  ✅ repairMissingEjerciciosForRutinaLinks() → Para integridad de datos
```

### Antes (DatabaseBuilder.kt - onOpen):
```kotlin
override fun onOpen(db: SupportSQLiteDatabase) {
    super.onOpen(db)
    CoroutineScope(Dispatchers.IO).launch {
        INSTANCE?.let { database ->
            repairMissingEjerciciosForRutinaLinks(database)
            seedUsuariosTest(database)  // ← LLAMABA
        }
    }
}
```

### Después (DatabaseBuilder.kt - onOpen):
```kotlin
override fun onOpen(db: SupportSQLiteDatabase) {
    super.onOpen(db)
    CoroutineScope(Dispatchers.IO).launch {
        INSTANCE?.let { database ->
            repairMissingEjerciciosForRutinaLinks(database)
            // Seeds de test usuarios deshabilitado para testing limpio
            // seedUsuariosTest(database)  // ← DESHABILITADO
        }
    }
}
```

---

## 🚀 Pasos para Testing Limpio

### Opción 1: Desinstalar App (RECOMENDADO) ⭐

**Más limpio y completo. BD se borra totalmente.**

```bash
# Desinstalar app (borra BD local completamente)
adb uninstall com.example.myapp

# Compilar proyecto limpio
./gradlew clean build assembleDebug

# Instalar APK nuevo
adb install -r app/build/outputs/apk/debug/app-debug.apk

# O automatizado (Windows):
compile_and_test.bat

# O automatizado (macOS/Linux):
./compile_and_test.sh
```

**Resultado:** 
- ✅ BD completamente vacía
- ✅ Sin usuarios de test
- ✅ Sin ejercicios legacy
- ✅ Primer login sincroniza 47 ejercicios desde Workers
- ✅ Perfecto para testing

---

### Opción 2: Limpiar Datos Sin Desinstalar

**Si quieres mantener algún estado pero limpiar BD:**

```bash
# Opción A: Borrar solo datos de app (mantiene app)
adb shell pm clear com.example.myapp

# Opción B: Borrar Shared Preferences
adb shell "run-as com.example.myapp rm -rf /data/data/com.example.myapp/shared_prefs"

# Opción C: Borrar BD completamente
adb shell "run-as com.example.myapp rm -rf /data/data/com.example.myapp/databases"
```

**Luego:**
```bash
adb shell am start -n com.example.myapp/.MainActivity
```

---

## 📊 Estado de BD después de Limpieza

### Tablas Vacías (Se poblarán al login):
```
✅ ejercicios (se llena con 47 base ejercicios vía sync)
✅ rutinas (pendiente que usuario cree)
✅ rutinaEjercicios (pendiente que usuario cree)
✅ sesiones (pendiente que usuario cree)
```

### Tablas CON datos (Sistem):
```
⏳ usuarios (vacía - sesión usuario actual)
⏳ sincronización metadata (se crea durante sync)
```

---

## ✅ Validación Post-Limpieza

### 1️⃣ Verificar App Limpia
```bash
# Database Inspector en Android Studio
View → Tool Windows → Database Inspector
Seleccionar: com.example.myapp

SQL:
SELECT COUNT(*) as total_ejercicios FROM ejercicios;
Resultado esperado: 0
```

### 2️⃣ Verificar SharedPreferences Limpio
```bash
adb shell "run-as com.example.myapp cat /data/data/com.example.myapp/shared_prefs/SessionManager.xml"
Resultado esperado: <map></map> (vacío)
```

### 3️⃣ Compilar Proyecto
```bash
./gradlew clean build --info
Resultado esperado: BUILD SUCCESSFUL
```

### 4️⃣ Primer Login
```
Email: test@test.com
Password: 123456

Esperar logs en Logcat:
D/BaseExercisesSync: Synced 47 exercises in this page. Final count: 47 ✅
```

---

## 🚨 Posibles Problemas Post-Limpieza

### ❌ "App crashes on login"
```
Causa: BD corrupta durante limpieza
Solución: 
  1. Desinstalar completamente: adb uninstall com.example.myapp
  2. Recompilar: ./gradlew clean build
  3. Reinstalar: adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### ❌ "Base exercises no sincroniza (count = 0)"
```
Causa: Posible problema en ExercisesApi o BaseExercisesSyncManager
Solución:
  1. Ver Logcat: filter "BaseExercisesSync"
  2. Revisar logs de error
  3. Verificar conexión a Workers
```

### ❌ "Database Inspector shows locked"
```
Causa: App sigue corriendo mientras intentas ver DB
Solución:
  1. Pausar app: adb shell am force-stop com.example.myapp
  2. Esperar 2 segundos
  3. Abrir Database Inspector nuevamente
```

---

## 📋 Checklist Pre-Testing

- [ ] Código limpiado (seeds removidos) ✅ HECHO
- [ ] DatabaseBuilder.kt actualizado ✅ HECHO
- [ ] App desinstalada: `adb uninstall com.example.myapp`
- [ ] Proyecto compilado: `./gradlew clean build`
- [ ] APK instalado: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- [ ] Logcat abierto con filter "BaseExercisesSync"
- [ ] Emulador/dispositivo listo
- [ ] Conexión a internet activa

---

## 🎯 Flujo Recomendado

```
1. DESINSTALAR
   adb uninstall com.example.myapp
   ↓
2. COMPILAR
   ./gradlew clean build
   ↓
3. INSTALAR
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ↓
4. ABRIR LOGCAT
   Filter: BaseExercisesSync
   ↓
5. HACER LOGIN
   test@test.com / 123456
   ↓
6. VALIDAR LOGS
   Buscar: "Final count: 47" ✅
   ↓
7. VERIFICAR DB
   Database Inspector → ejercicios → COUNT(*) = 47
   ↓
8. ✅ TESTING LISTO
```

---

## 📝 Notas Importantes

1. **No hay usuarios de test por defecto:**
   - Ahora necesitas hacer login real (test@test.com / 123456)
   - O crear usuario desde app
   - Seeds de test usuarios fueron deshabilitados

2. **Ejercicios vienen de Workers:**
   - Primera sincronización: 47 ejercicios
   - Requiere internet
   - Cache de 7 días después

3. **Repair function sigue activo:**
   - `repairMissingEjerciciosForRutinaLinks()` se mantiene
   - Asegura integridad si hay rutinas legacy

4. **Datos de test legacy:**
   - Si había datos previos, serán borrados
   - Comenzarás con BD completamente limpia
   - Perfecto para reproducir bug reports

---

## 🔄 Reset Total de App

Si necesitas reset más agresivo:

```bash
# Script Windows:
adb uninstall com.example.myapp
adb shell "pm trim-caches 3G"
./gradlew clean build assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Script Linux/macOS:
adb uninstall com.example.myapp
adb shell "pm trim-caches 3G"
./gradlew clean build assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

**Status:** ✅ READY FOR TESTING
**Fecha:** Abril 17, 2026
**Próximo paso:** [QUICK_TEST_GUIDE.md](QUICK_TEST_GUIDE.md)
