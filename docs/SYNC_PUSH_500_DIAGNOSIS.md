# SYNC PUSH ENDPOINT - Diagnóstico de Error 500

## Resumen
El endpoint `/api/sync/push` está devolviendo HTTP 500 mientras que `/api/sync/pull` funciona correctamente (HTTP 200).

## Evidence

### Test 1: /api/sync/pull (GET) ✓
```
Request:
  GET https://ratita-gym--worker.azucenapolo6.workers.dev/api/sync/pull?entity=usuarios&since=0&limit=10
  Authorization: Bearer ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0
  x-user-id: 00000001-0000-5000-8000-000000000001

Response:
  HTTP 200 OK
  Content: {"items":[], "nextSince":0}
```

### Test 2: /api/sync/push (POST) ✗
```
Request:
  POST https://ratita-gym--worker.azucenapolo6.workers.dev/api/sync/push
  Authorization: Bearer ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0
  x-user-id: 00000001-0000-5000-8000-000000000001
  Content-Type: application/json
  Body: {"items":[]}

Response:
  HTTP 500 Internal Server Error
  Content: (empty)
```

## Impacto
- El cliente Android no puede sincronizar cambios de usuario (push)
- Solo puede recibir cambios remotos (pull)
- Todavía no hay datos de usuario, pero base routines/exercises sincronizados correctamente

## Acciones Recomendadas para Backend Team

### 1. Revisar Worker Handler
- Verificar que POST `/api/sync/push` puede procesar `{"items":[]}`
- Verificar que la validación no rechaza payload vacío con error 500
- Confirmar que el handler no asume presencia de al menos 1 item

### 2. Habilitar Logging
- Agregar logs a Cloudflare Worker para POST `/api/sync/push`
- Capturar request body completo
- Capturar stack trace del error 500

### 3. Testing
```bash
# Test con payload mínimo
curl -X POST https://ratita-gym--worker.azucenapolo6.workers.dev/api/sync/push \
  -H "Authorization: Bearer ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0" \
  -H "x-user-id: 00000001-0000-5000-8000-000000000001" \
  -H "Content-Type: application/json" \
  -d '{"items":[]}'

# Test con item ejemplo
curl -X POST https://ratita-gym--worker.azucenapolo6.workers.dev/api/sync/push \
  -H "Authorization: Bearer ca21131307bec2b966f9a26f3059aab0824ae169367c69704c8dc15a86e1f4c0" \
  -H "x-user-id: 00000001-0000-5000-8000-000000000001" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "entityType": "ejercicios",
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "updatedAt": 1713607800000,
        "syncStatus": "PENDING",
        "payload": {
          "nombre": "Test Ejercicio",
          "grupoMuscular": "pecho",
          "descripcion": "Test"
        }
      }
    ]
  }'
```

### 4. Respuesta Esperada
```json
HTTP 200 OK
{
  "acceptedIds": ["550e8400-e29b-41d4-a716-446655440000"],
  "rejectedIds": []
}
```

## Referencias Cliente
- **Push Handler**: [SyncManager.kt - pushPendingsRutinas()](app/src/main/java/com/example/myapp/data/sync/SyncManager.kt#L183)
- **DTO**: [SyncApi.kt - SyncPushRequestDto](app/src/main/java/com/example/myapp/data/remote/sync/SyncApi.kt#L16)
- **Test Script**: test_both_endpoints.ps1 (en workspace)

## Status
- ✓ Autenticación validada
- ✓ Pull endpoint funciona
- ✗ Push endpoint retorna 500
- ⏳ Bloqueante para sincronización de cambios de usuario

**Requested**: Revisar logs y stack trace del error 500 en el handler de POST `/api/sync/push`
