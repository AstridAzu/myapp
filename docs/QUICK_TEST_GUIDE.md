# Guía de Ejecución de Tests - Ejercicios Base

**Rápida referencia para ejecutar todos los tests de sincronización de ejercicios base**

---

## 🚀 Quick Start (3 minutos)

### Paso 1: Compilar y instalar
```bash
# Windows
compile_and_test.bat

# macOS/Linux
./compile_and_test.sh

# O manualmente:
./gradlew clean build assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Paso 2: Abrir Logcat
```
Android Studio → View → Tool Windows → Logcat
Filtro: BaseExercisesSync
```

### Paso 3: Hacer login
```
Email: test@test.com (o alumno@test.com)
Password: 123456
```

### Paso 4: Validar en logs
Buscar línea:
```
D/BaseExercisesSync: Base exercises sync complete. Final count: 47
```

✅ **Si ves eso, FUNCIONÓ** ✅

---

## 📋 Test Detallado (10 minutos)

### TEST 1: Compilación ✅

**Objetivo:** Verificar que no hay errores de compilación

**Ejecución:**
```bash
./gradlew clean build --info
```

**Resultado esperado:**
```
BUILD SUCCESSFUL in XXs
```

**Si falla:** Ver sección Troubleshooting

---

### TEST 2: Primer Login (Sincronización) ⭐ MÁS IMPORTANTE

**Objetivo:** Validar que sincroniza 47 ejercicios en primer login

**Pasos:**
1. Desinstalar app: `adb uninstall com.example.myapp`
2. Reinstalar APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Abrir app → Login screen
4. Ingresar: `test@test.com` / `123456`
5. Presionar: "Iniciar Sesión" o "Login"

**Logcat esperado (en orden):**
```
D/BaseExercisesSync: Existing base exercises: 0
D/BaseExercisesSync: Fetching page: since=0, limit=200
D/BaseExercisesSync: Synced 47 exercises in this page. Total: 47
D/BaseExercisesSync: Base exercises sync complete. Final count: 47
D/SyncWorker: ✓ Base exercises synced successfully
```

**Database Inspector (opcional):**
1. Android Studio → View → Tool Windows → Database Inspector
2. Click: `com.example.myapp`
3. SQL: `SELECT COUNT(*) FROM ejercicios WHERE idCreador IS NULL`
4. Resultado esperado: `47`

**Resultado:**
- ✅ PASS si ves "Final count: 47" en logs
- ❌ FAIL si count < 47

---

### TEST 3: Cache (No resync en 7 días) ⏰

**Objetivo:** Validar que no re-sincroniza si pasó < 7 días

**Pasos:**
1. Ya hiciste TEST 2 (primer login OK)
2. Hacer logout
3. Hacer login nuevamente (sin esperar 7 días reales)

**Logcat esperado:**
```
D/SyncWorker: shouldSyncBaseExercises() returned false
(No aparece "Fetching page" ni "Starting base exercises sync")
```

**Resultado:**
- ✅ PASS si NO sincroniza (cache activo)
- ❌ FAIL si sincroniza nuevamente

---

### TEST 4: Funcionalidad Offline 📡

**Objetivo:** Validar que app funciona offline con datos cached

**Pasos:**
1. Hacer login exitoso (TEST 2)
2. Desconectar red:
   ```bash
   adb shell svc wifi disable
   adb shell svc data disable
   ```
3. Navegar a sección de ejercicios
4. Verificar que muestra 47 ejercicios
5. Reconectar red:
   ```bash
   adb shell svc wifi enable
   adb shell svc data enable
   ```

**Resultado:**
- ✅ PASS si funciona offline y online
- ❌ FAIL si muestra error offline

---

### TEST 5: Ejercicios Específicos 📊

**Objetivo:** Validar que los datos son correctos

**En Database Inspector:**
```sql
SELECT nombre, grupoMuscular, colorHex
FROM ejercicios
WHERE nombre IN ('Press de Banca', 'Sentadilla', 'Dominadas')
```

**Resultado esperado:**
```
Press de Banca  | Pecho   | #E53935
Sentadilla      | Pierna  | (algún color)
Dominadas       | Espalda | (algún color)
```

**Resultado:**
- ✅ PASS si ves los 3 ejercicios con datos completos
- ❌ FAIL si hay campos NULL

---

## 🔍 Verificación Rápida en Android Studio

### Database Inspector
```
View → Tool Windows → Database Inspector
Seleccionar: com.example.myapp
Click: ejercicios table
```

**Queries útiles:**
```sql
-- Total de ejercicios
SELECT COUNT(*) FROM ejercicios

-- Solo base exercises
SELECT COUNT(*) FROM ejercicios WHERE idCreador IS NULL

-- Por grupo
SELECT grupoMuscular, COUNT(*) FROM ejercicios 
WHERE idCreador IS NULL 
GROUP BY grupoMuscular

-- Verificar campos
SELECT id, nombre, colorHex, icono 
FROM ejercicios LIMIT 5
```

### Logcat Filter
```
Filter: BaseExercisesSync|SyncWorker
Log level: Verbose + Debug

Búsqueda rápida: "Final count"
```

---

## ⚠️ Troubleshooting

### ❌ BUILD FAILED
```
Error: Cannot resolve ExercisesApi

Solución:
1. Compilar desde Android Studio (no VS Code)
2. File → Invalidate Caches → Invalidate and Restart
3. ./gradlew clean build
```

### ❌ Logcat no muestra "BaseExercisesSync"
```
Error: No logs

Solución:
1. Verificar filtro: escribir "BaseExercisesSync"
2. Verificar: log level = DEBUG
3. Verificar: app se abrió correctamente
4. Revisar: adb logcat -c (limpiar)
```

### ❌ Count < 47
```
Error: Final count: 40 (o menos)

Solución:
1. Revisar: ¿Detiene en qué página? (buscar "Fetching page")
2. Revisar: timeout de conexión
3. Revisar: endpoint Worker retorna datos válidos
4. Revisar: D1 tiene los 47 ejercicios
```

### ❌ ERROR: countBaseExercises no existe
```
Error: No such method countBaseExercises

Solución:
1. Verificar: EjercicioDao.kt tiene el método
2. Limpiar: ./gradlew clean
3. Recompilar: ./gradlew build
4. Verificar imports
```

---

## 📝 Formato de Reporte

Cuando reportes un problema, incluye:

```
TEST: [nombre del test]
RESULTADO: [PASS/FAIL]
LOGCAT: [últimas 10 líneas relevantes]
DATABASE: [resultado de query si aplica]
PASOS: [qué hiciste exactamente]
ERROR: [mensaje de error si hay]
```

---

## ✅ Checklist de Tests Completados

- [ ] TEST 1: Compilación exitosa
- [ ] TEST 2: Primer login → 47 ejercicios
- [ ] TEST 3: Cache → no resync < 7 días
- [ ] TEST 4: Offline funciona
- [ ] TEST 5: Datos específicos correctos

**Todos PASS = ✅ IMPLEMENTACIÓN VALIDADA**

---

## 🚀 Próximos Pasos Post-Testing

Si todos los tests PASS:

1. **Limpiar logs**
   ```
   Cambiar log level a INFO (solo errores)
   ```

2. **Preparar para producción**
   ```
   Verificar BuildConfig.SYNC_API_BASE_URL
   Verificar BuildConfig.SYNC_API_TOKEN
   ```

3. **Release APK**
   ```
   ./gradlew clean build assembleRelease
   ```

4. **Monitoreo**
   ```
   Firbase Crashlytics
   Logcat remoto si aplica
   ```

---

**Mantenedor:** Equipo Atlas  
**Última actualización:** Abril 17, 2026
