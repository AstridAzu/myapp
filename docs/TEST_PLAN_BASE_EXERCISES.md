# Plan de Testing - Sincronización de Ejercicios Base

**Fecha:** Abril 2026  
**Versión:** 1.0  
**Estado:** En ejecución

---

## 📋 Checklist de Testing

### Fase 1: Verificación de Compilación ✅

#### 1.1 - Compilar proyecto
```bash
# En la raíz del proyecto
./gradlew clean build
```

**Verificar:**
- ✅ Proyecto compila sin errores
- ✅ Todos los imports están resueltos
- ✅ No hay warnings críticos

**Errores comunes a buscar:**
- `Unresolved reference ExercisesApi` → Revisar import en SyncWorker
- `Cannot find symbol countBaseExercises` → Revisar que se agregó a EjercicioDao
- `Type mismatch` → Verificar tipos en BaseExercisesSyncManager

#### 1.2 - Verificar imports en SyncWorker
```kotlin
// Debe tener:
import com.example.myapp.data.remote.ExercisesApiFactory
import com.example.myapp.data.sync.BaseExercisesSyncManager
```

#### 1.3 - Verificar que BaseExercisesSyncManager está en package correcto
```
app/src/main/java/com/example/myapp/data/sync/BaseExercisesSyncManager.kt
```

---

### Fase 2: Verificación en Emulador/Dispositivo 📱

#### 2.1 - Compilar APK en modo debug
```bash
./gradlew clean assembleDebug
```

**Output esperado:**
```
BUILD SUCCESSFUL in XXs
app-debug.apk created successfully
```

#### 2.2 - Instalar en emulador
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### 2.3 - Abrir Logcat en Android Studio
```
View → Tool Windows → Logcat
```

Filter: `BaseExercisesSync` o `SyncWorker`

---

### Fase 3: Test Funcional - Primer Login ✅

#### 3.1 - Preparar antes del test
1. Desinstalar app completamente (adb uninstall com.example.myapp)
2. Reinstalar APK limpio
3. Abrir Logcat con filtros

#### 3.2 - Ejecutar test
1. Abrir app → Pantalla de login
2. Ingresar credenciales de prueba:
   - Email: `test@test.com` O `alumno@test.com`
   - Password: `123456`
3. Presionar "Iniciar Sesión"

#### 3.3 - Verificar logs esperados en orden

```
DEBUG BaseExercisesSync: Existing base exercises: 0
DEBUG BaseExercisesSync: Fetching page: since=0, limit=200
DEBUG BaseExercisesSync: Synced 47 exercises in this page. Total: 47
DEBUG BaseExercisesSync: Base exercises sync complete. Final count: 47
DEBUG SyncWorker: ✓ Base exercises synced successfully
```

#### 3.4 - Validar en base de datos local

Abrir Android Studio Database Inspector:
```
View → Tool Windows → Database Inspector
(o Ctrl+Shift+D)
```

**Verificaciones:**
- Tabla `ejercicios` tiene 47 registros
- Todos tienen `idCreador = NULL` (base exercises)
- Campos tienen datos:
  - `nombre` (ej: "Press de Banca")
  - `grupoMuscular` (ej: "Pecho")
  - `colorHex` (ej: "#E53935")
  - `icono` (ej: "FITNESS_CENTER")

**Query en Database Inspector:**
```sql
SELECT COUNT(*) as total, 
       COUNT(CASE WHEN colorHex IS NOT NULL THEN 1 END) as con_color,
       COUNT(CASE WHEN icono IS NOT NULL THEN 1 END) as con_icono
FROM ejercicios 
WHERE idCreador IS NULL
```

Expected: `{total: 47, con_color: 47, con_icono: 47}`

#### 3.5 - Validar en SharedPreferences

```
Logcat con filter: adb logcat | grep "base_exercises_sync_time"
```

O ver en Device File Explorer:
```
data/data/com.example.myapp/shared_prefs/atlas_session.xml
```

Debe contener:
```xml
<long name="base_exercises_sync_time" value="1776460950000" />
```

---

### Fase 4: Test Funcional - Segunda Sincronización (Cache) ⏱️

#### 4.1 - Configurar reloj para simular 7 días
**Opción A - Simulación rápida (local.properties):**
```
FORCE_BASE_EXERCISES_RESYNC=true
```

**Opción B - Test manual:**
1. Esperar 7 días (no práctico)
2. Modificar timestamp manualmente en adb shell (ver paso 4.2)

#### 4.2 - Modificar timestamp en adb shell (simular sync anterior)

```bash
adb shell

# Entrar a contenedor de SharedPreferences
cd /data/data/com.example.myapp/shared_prefs

# Editar archivo
vi atlas_session.xml

# Cambiar valor de base_exercises_sync_time a hace 8 días
# Timestamp actual - 8 días en ms
# Ej: Si hoy es 1776460950000, poner 1775597350000 (8 días atrás)
```

#### 4.3 - Forzar sync manual
1. Ir a menú de Settings (si existe)
2. O ejecutar en adb shell:
```bash
adb shell am broadcast -a com.example.myapp.SYNC_MANUAL
```

#### 4.4 - Verificar logs
```
DEBUG BaseExercisesSync: Existing base exercises: 47
DEBUG BaseExercisesSync: Fetching page: since=0, limit=200
DEBUG BaseExercisesSync: Synced 47 exercises in this page. Total: 47
DEBUG SyncWorker: ✓ Base exercises synced successfully
```

(Nota: Re-sincroniza los mismos 47 con upsert, sin duplicados)

---

### Fase 5: Test de Funcionalidad Offline 📡

#### 5.1 - Preparar
1. Hacer login exitosamente (Fase 3)
2. Verificar 47 ejercicios en DB

#### 5.2 - Desconectar red
```bash
adb shell svc wifi disable
adb shell svc data disable
```

#### 5.3 - Navegar en app
- Ir a sección de ejercicios
- Debe mostrar los 47 ejercicios locales
- Debe funcionar sin errores

#### 5.4 - Reconectar red
```bash
adb shell svc wifi enable
adb shell svc data enable
```

#### 5.5 - Verificar
- App continúa funcionando
- Puede hacer nuevo login
- Sincroniza cambios si los hay

---

### Fase 6: Verificación de Logs Detallados 📊

#### 6.1 - Habilitar DEBUG logging
```
Logcat filter: "BaseExercisesSync|SyncWorker"
Log level: VERBOSE + DEBUG
```

#### 6.2 - Esperado en primer login
```
D/BaseExercisesSync: Existing base exercises: 0
D/BaseExercisesSync: Fetching page: since=0, limit=200
D/BaseExercisesSync: Synced 47 exercises in this page. Total: 47
D/BaseExercisesSync: Base exercises sync complete. Final count: 47
D/SyncWorker: ✓ Base exercises synced successfully
D/SyncWorker: Starting base exercises sync...
D/SyncWorker: ✓ Base exercises synced successfully
```

#### 6.3 - Esperado en segundo login (con cache)
```
D/BaseExercisesSync: Existing base exercises: 47
D/SyncWorker: shouldSyncBaseExercises() returned false
(No se hace sync)
```

#### 6.4 - Mensaje de error esperado (si algo falla)
```
W/SyncWorker: ⚠ Base exercises sync failed (non-blocking): {reason}
```

**Este log NO debe detener el sync normal.**

---

### Fase 7: Validación de Datos Específicos ✅

#### 7.1 - Verificar grupos musculares
Ejecutar en Database Inspector:
```sql
SELECT grupoMuscular, COUNT(*) as cantidad
FROM ejercicios
WHERE idCreador IS NULL
GROUP BY grupoMuscular
ORDER BY grupoMuscular
```

Expected:
```
Brazos            → 6
Cardio            → 4
Core / Abdomen    → 6
Espalda           → 7
Glúteos           → 4
Hombro            → 6
Pecho             → 7
Pierna            → 7
```

#### 7.2 - Verificar ejercicios específicos
```sql
SELECT nombre, grupoMuscular, colorHex, icono
FROM ejercicios
WHERE nombre IN ('Press de Banca', 'Sentadilla', 'Peso Muerto')
```

Expected:
```
Press de Banca    | Pecho     | #E53935 | FITNESS_CENTER
Sentadilla        | Pierna    | ...    | ...
Peso Muerto       | Espalda   | ...    | ...
```

---

### Fase 8: Test de Paginación (Opcional - para volúmenes grandes) 📄

**Nota:** Con 47 ejercicios, entra en 1 página (limit=200).  
Para testing de paginación:

#### 8.1 - Bajar limit en BaseExercisesSyncManager
```kotlin
private const val PAGE_SIZE = 10  // Cambiar de 200 a 10
```

#### 8.2 - Forzar resync
```bash
adb shell pm clear com.example.myapp  # Borra SharedPreferences
```

#### 8.3 - Login nuevamente
```
DEBUG BaseExercisesSync: Fetching page: since=0, limit=10
DEBUG BaseExercisesSync: Synced 10 exercises in this page. Total: 10
DEBUG BaseExercisesSync: Fetching page: since=XXXX, limit=10
DEBUG BaseExercisesSync: Synced 10 exercises in this page. Total: 20
... (5 páginas totales)
DEBUG BaseExercisesSync: Base exercises sync complete. Final count: 47
```

---

## 📊 Matriz de Validación

| Test | Estado | Resultado Esperado | Resultado Actual | ✅/❌ |
|------|--------|-------------------|-----------------|------|
| 1.1 Compilación | - | Éxito sin errores | - | - |
| 1.2 Imports | - | Resueltos | - | - |
| 2.1 APK Debug | - | Builds successful | - | - |
| 3.1-3.5 Primer login | - | 47 ejercicios + logs | - | - |
| 4.1-4.4 Cache 7 días | - | No resync | - | - |
| 5.1-5.5 Offline | - | Funciona con datos | - | - |
| 6.1-6.4 Logs | - | Debug info visible | - | - |
| 7.1-7.2 Datos | - | Grupos y detalles ok | - | - |
| 8.1-8.3 Paginación | Opt | 5 páginas × 10 | - | - |

---

## 🚨 Troubleshooting

### Error: "Cannot resolve symbol ExercisesApi"
```
✓ Verificar: ExercisesApi.kt existe en app/src/main/java/com/example/myapp/data/remote/
✓ Verificar imports en SyncWorker.kt
✓ Limpiar y reconstruir: ./gradlew clean
```

### Error: "No such method countBaseExercises"
```
✓ Verificar: countBaseExercises() está en EjercicioDao.kt
✓ Query: SELECT COUNT(*) FROM ejercicios WHERE idCreador IS NULL
✓ Limpiar caché: rm -rf .gradle/ app/build/
```

### Sync no comienza en login
```
✓ Verificar: SyncWorker recibe el llamado
✓ Revisar Logcat: "Starting base exercises sync..."
✓ Verificar permisos de red
✓ Verificar BuildConfig.SYNC_API_BASE_URL
```

### Base exercises count < 47
```
✓ Verificar: Respuesta de Workers tiene items
✓ Verificar: Endpoint retorna hasMore=false
✓ Revisar: Límite de conexión/timeout
✓ Revisar: Logs para ver en qué página se paró
```

### Datos no persisten offline
```
✓ Verificar: ejercicioDao.upsertAll() se llamó
✓ Verificar: Transaction completó sin errores
✓ Revisar: Permisos de escritura en DB
✓ Revisar: Logs para ver si hubo exception
```

---

## 📝 Notas para Tester

- ✅ Primer login = test más importante (validar 47 ejercicios)
- ✅ Cache de 7 días = importante para UX
- ✅ Offline = valida que sincronización persiste
- ✅ Logs = críticos para debugging
- ⚠️ Paginación = solo si cambia limit (57+ ejercicios)

---

**Mantenedor:** Equipo Atlas  
**Última actualización:** Abril 2026
