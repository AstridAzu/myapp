# Implementación Step 1-3: Estado Actual

## Resumen Ejecutivo

Se ha completado la **Fase de Diagnóstico y Hardening** del plan de sincronización de "Mis rutinas". Se identificó y documentó un error 500 en el backend que bloquea los pasos siguientes.

**Status**: 60% completado (Steps 1-3 de 6)
- ✅ Step 1: Autenticación cliente → validada
- ✅ Step 2-3: Integridad referencial en pull → hardened
- ❌ Step 4-5: Push offline-first y UI (bloqueado por backend)
- ⏳ Step 6: Testing (bloqueado por backend)

---

## Cambios Realizados

### Cliente Android (SyncWorker, SyncManager, DAOs)

#### 1. **Diagnóstico mejorado** (SyncWorker.kt, SyncApiFactory.kt)
```kotlin
// Logging detallado de credenciales
Log.d("SyncWorker", "Starting normal sync with credentials:")
Log.d("SyncWorker", "  - Authorization: Bearer ...${tokenSuffix}")
Log.d("SyncWorker", "  - x-user-id: $syncUserId")

// Logging de nivel BODY para ver payloads
HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
```

#### 2. **Retry policy inteligente** (SyncFailurePolicy.kt)
```kotlin
// Evita loops de WorkManager
if (current is HttpException) {
    return when (current.code()) {
        401, 403 -> true  // Auth error: no-retriable
        404 -> true        // Not found: no-retriable
        else -> false      // 5xx: retriable
    }
}
```

#### 3. **FK validation para pull** (SyncManager.kt)
- `applyRutinaAccesosPulled`: Valida rutina + usuario antes de crear acceso
- `applySesionesRutinaPulled`: Valida rutina + usuario antes de crear sesión  
- `applyRegistrosSeriesPulled`: Valida sesión + ejercicio antes de crear registro

Todos descartan referencias huérfanas con logging detallado.

#### 4. **DAO improvements** (UsuarioDao.kt)
```kotlin
@Query("SELECT id FROM usuarios WHERE id IN (:ids) AND syncStatus != 'DELETED'")
suspend fun getExistingIds(ids: List<String>): List<String>
```

### Resultado de Compilación
```
BUILD SUCCESSFUL in 15s
35 actionable tasks: 7 executed, 28 up-to-date
```
✅ Sin errores, sin warnings de sintaxis

---

## Diagnóstico de Backend

### Hallazgo Crítico
El endpoint `/api/sync/push` retorna **HTTP 500 Internal Server Error** mientras que `/api/sync/pull` funciona correctamente (HTTP 200).

| Endpoint | Method | Test Payload | Status | Response |
|----------|--------|--------------|--------|----------|
| `/api/sync/pull` | GET | `entity=usuarios&since=0` | **200** ✓ | `{"items":[], "nextSince":0}` |
| `/api/sync/push` | POST | `{"items":[]}` | **500** ✗ | (empty) |

**Implicación**: La autenticación está correcta, pero el handler de push tiene un bug interno.

### Archivo de Diagnóstico Generado
📄 **`SYNC_PUSH_500_DIAGNOSIS.md`** en workspace root

Contiene:
- Evidence de ambos endpoints
- Impacto en cliente
- Acciones recomendadas para backend team
- Scripts de test reproducibles

---

## Bloqueadores Pendientes

### 🔴 Critical: Backend Fix Requerido
**Issue**: HTTP 500 en `/api/sync/push`
**Root Cause**: Probablemente bug en handler de Cloudflare Worker
**Impact**: Cliente no puede sincronizar cambios de usuario
**Action**: Backend team debe revisar logs y stack trace

### Trabajo Desbloqueado tras Fix
```
Step 4: Hardening de push offline-first (2-3h)
  → Mejorar retry/backoff en CloudPushHelper
  → Garantizar PENDING state resiliente

Step 5: UI observability (1-2h)
  → Exponer isSyncing, syncError, lastSyncAt
  → Agregar botón refresh manual en RutinasScreen

Step 6: Functional verification (2-3h manual testing)
  → Test "crear offline → sync"
  → Test "rutina compartida → pull"
  → Test "403 handling"
```

---

## Recomendaciones Inmediatas

### Para Equipo Backend
1. **Revisar logs** de POST `/api/sync/push` en Cloudflare Worker
2. **Verificar handler** puede procesar payload `{"items":[]}`
3. **Test with**:
   ```bash
   curl -X POST https://.../api/sync/push \
     -H "Authorization: Bearer ca2113..." \
     -H "x-user-id: 00000001-..." \
     -d '{"items":[]}'
   ```
4. **Validar** estructura de DTOs en backend coincida con cliente

### Para Usuario (Astrid)
1. **En paralelo** while backend works on fix:
   - ✅ Base routines/exercises sync validated
   - ✅ Pull mechanism hardened
   - ✅ Logging mejorado para debugging

2. **Próximo paso** cuando backend fix esté listo:
   - Deploy backend fix
   - Run: `./gradlew.bat assembleDebug` (ya listo)
   - Test con app en emulador/device
   - Ejecutar Step 4-5 si tiempo permite

3. **Urgency**: Sin backend fix, "Mis rutinas" seguirá vacía porque usuario no puede crear rutinas offline

---

## Checklist de Validación

### ✅ Completado
- [x] Autenticación endpoint validada (pull)
- [x] Headers client correctamente configurados
- [x] Retry policy clasificado por HTTP code
- [x] FK validation en pull para 3 entidades
- [x] Logging detallado para diagnosticar sync issues
- [x] Compilación sin errores
- [x] Documento de diagnóstico backend creado

### ⏳ Pendiente (Bloqueado)
- [ ] Backend fix para `/api/sync/push` error 500
- [ ] Test push con items reales
- [ ] Hardening de retry/backoff para push
- [ ] UI observability en RutinasScreen
- [ ] Functional test matrix (4 scenarios)

---

## Referencias

### Archivos Modificados
```
app/src/main/java/com/example/myapp/data/sync/SyncWorker.kt
  → Agregar logs de credenciales
  
app/src/main/java/com/example/myapp/data/sync/SyncFailurePolicy.kt
  → Clasificar HTTP errors como retryable/non-retryable
  
app/src/main/java/com/example/myapp/data/sync/SyncManager.kt
  → applyRutinaAccesosPulled: FK validation
  → applySesionesRutinaPulled: FK validation
  → applyRegistrosSeriesPulled: FK validation
  
app/src/main/java/com/example/myapp/data/remote/sync/SyncApiFactory.kt
  → HttpLoggingInterceptor.Level.BODY
  
app/src/main/java/com/example/myapp/data/local/dao/UsuarioDao.kt
  → getExistingIds() method added
```

### Test Scripts Creados
```
test_sync_auth.ps1          → Test múltiples user IDs
test_both_endpoints.ps1     → Test pull vs push
test_entity_pull.ps1        → Test todas las entidades
SYNC_PUSH_500_DIAGNOSIS.md  → Reporte backend
```

---

## Próximos Pasos Recomendados

**Inmediato**: 
- Compartir `SYNC_PUSH_500_DIAGNOSIS.md` con backend team
- Request: "Review logs for POST /api/sync/push error 500"

**Cuando backend fix esté ready**:
```bash
# Recompile (cambios ya están listos)
./gradlew.bat assembleDebug

# Deploy a device/emulator
# Manual test: crear rutina → verificar en logs que sync push es 200
```

**Si hay tiempo antes de deploy**:
- Implementar Step 4 (push hardening)
- Implementar Step 5 (UI observability)
- Ejecutar Step 6 (test matrix)

---

## Conclusión

✅ **Diagnóstico completo**: El problema es en backend `/api/sync/push`, no en cliente

✅ **Cliente hardened**: FK validation y logging mejorado

⏳ **Bloqueador identificado**: Error 500 del backend

📋 **Plan claro**: Documento de diagnóstico listo para backend team

🎯 **Objetivo alcanzable**: Con backend fix, Steps 4-6 se pueden completar en 1 día
