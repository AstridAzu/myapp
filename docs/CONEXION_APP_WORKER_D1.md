# Worker Cloudflare D1: Requisitos Especificos para Sync

Este documento describe lo que necesita el Worker especificamente para operar la sincronizacion con la app Android de forma estable, segura y observable.

## 1. Objetivo del Worker

El Worker debe:

- Recibir cambios locales de la app (push).
- Exponer cambios remotos incrementalmente (pull por cursor).
- Persistir en D1 con idempotencia.
- Resolver conflictos por `updatedAt`.
- Propagar borrado logico con `syncStatus=DELETED` y `deletedAt`.

## 2. Configuracion obligatoria en Cloudflare

### 2.1 Bindings requeridos

- D1 binding (por ejemplo `DB`) apuntando a la base correcta por entorno.
- R2 binding (si el mismo Worker tambien maneja imagenes).

### 2.2 Variables y secretos requeridos

Secretos:

- `SYNC_API_TOKEN` (Bearer para endpoints de sync).
- `IMAGE_API_TOKEN` (si se comparte Worker de imagenes).
- `IMAGE_UPLOAD_SIGNING_SECRET` (si hay presigned upload).

Variables:

- `R2_PUBLIC_BASE_URL` (si aplica imagenes).
- `ENVIRONMENT` (`dev`, `staging`, `prod`).

### 2.3 Endpoints que deben existir

- `POST /api/sync/push`
- `GET /api/sync/pull`

## 3. Esquema D1 minimo para sync

Cada tabla sincronizable debe incluir:

- `id` (TEXT UUID recomendado).
- `updated_at` (INTEGER epoch ms).
- `sync_status` (TEXT: `PENDING`, `SYNCED`, `DELETED`).
- `deleted_at` (INTEGER nullable).

Indices minimos por tabla:

- `(updated_at, id)` para pull incremental estable.
- `(sync_status)` para mantenimiento y auditoria.

Regla de consistencia:

- El Worker debe normalizar `updated_at` en servidor al confirmar escrituras.

## 4. Contrato de push que el Worker debe aceptar

Request:

```json
{
  "items": [
    {
      "entityType": "ejercicios",
      "id": "uuid-id",
      "updatedAt": 1712922200000,
      "syncStatus": "PENDING",
      "payload": {
        "nombre": "Sentadilla",
        "grupoMuscular": "Pierna"
      }
    }
  ]
}
```

Respuesta requerida:

```json
{
  "acceptedIds": ["uuid-id"],
  "rejectedIds": []
}
```

Reglas del Worker:

- `acceptedIds` solo incluye items persistidos correctamente.
- `rejectedIds` incluye items invalidos o no autorizados.
- Debe ser idempotente ante reintentos del mismo item.

## 5. Contrato de pull que el Worker debe devolver

Endpoint:

- `GET /api/sync/pull?entity=ejercicios&since=0&limit=200`

Respuesta requerida:

```json
{
  "items": [
    {
      "entityType": "ejercicios",
      "id": "uuid-id",
      "updatedAt": 1712922200000,
      "syncStatus": "SYNCED",
      "deletedAt": null,
      "payload": {
        "nombre": "Sentadilla",
        "grupoMuscular": "Pierna"
      }
    }
  ],
  "nextSince": 1712922200000
}
```

Reglas del Worker:

- `nextSince` debe ser monotono y no retroceder.
- Orden recomendado: `updated_at ASC, id ASC`.
- Soportar paginacion con `limit`.
- Incluir tombstones (`DELETED`) para borrar en cliente.

## 6. Validaciones que debe aplicar el Worker

Validaciones de autenticacion/autorizacion:

- Rechazar requests sin Bearer token valido.
- Filtrar datos por usuario/tenant para evitar fugas de informacion.

Validaciones de payload:

- `entityType` permitido.
- `id` no vacio.
- `updatedAt` valido.
- `syncStatus` permitido.
- Campos requeridos por entidad presentes.

Validaciones de integridad:

- Verificar relaciones antes de persistir (FK logica o fisica).
- Rechazar cambios que rompen consistencia.

## 7. Politica de conflicto en Worker

Recomendacion operativa:

- Last write wins por `updated_at` del servidor.

Algoritmo sugerido:

1. Buscar fila existente por `id`.
2. Si no existe, insertar.
3. Si existe y request es mas nueva, actualizar.
4. Si existe y request es mas vieja, ignorar o rechazar como stale.
5. Si estado es `DELETED`, marcar tombstone y conservar historial minimo.

## 8. Estandar de errores del Worker

Respuesta de error recomendada:

```json
{
  "success": false,
  "error": {
    "code": "SYNC_PAYLOAD_INVALID",
    "message": "Missing required field: nombre",
    "retryable": false
  }
}
```

Codigos minimos:

- `SYNC_UNAUTHORIZED` (401)
- `SYNC_FORBIDDEN` (403)
- `SYNC_PAYLOAD_INVALID` (400)
- `SYNC_RATE_LIMITED` (429)
- `SYNC_INTERNAL_ERROR` (500)

## 9. Observabilidad minima del Worker

Log estructurado por request:

- requestId
- endpoint
- userId (si aplica)
- entityType
- acceptedCount
- rejectedCount
- durationMs
- statusCode

Metricas recomendadas:

- tasa de exito de push/pull
- latencia p50/p95
- porcentaje de `rejectedIds`
- volumen de tombstones

## 10. Despliegue y entornos

Checklist por entorno (`dev`, `staging`, `prod`):

1. Binding D1 correcto.
2. Secretos configurados.
3. Endpoints de sync activos.
4. Prueba de health basica.
5. Prueba push/pull con dataset pequeno.

Politica de despliegue recomendada:

- Promover de `staging` a `prod` solo tras smoke test E2E.
- Mantener rollback rapido a version previa del Worker.

## 11. Pruebas minimas del Worker

Pruebas funcionales:

1. Push valido retorna IDs aceptados.
2. Push con payload invalido retorna rechazo estructurado.
3. Pull con `since` incremental no duplica ni pierde items.
4. Pull incluye borrados logicos.

Pruebas de resiliencia:

1. Reintentos de push duplicado no generan duplicados en D1.
2. Volumen alto (lotes grandes) mantiene latencia aceptable.

## 12. Compatibilidad con la App Android

La app espera:

- `POST /api/sync/push` con `acceptedIds` y `rejectedIds`.
- `GET /api/sync/pull` con `items` y `nextSince`.
- IDs String (UUID-first).

Si el Worker rompe este contrato, la sincronizacion fallara o quedara inconsistente en cliente.

## 13. Fuentes del proyecto

- `d1_schema_v13.sql`
- `ANDROID_UUID_SYNC_IMPLEMENTACION.md`
- `apis.md`
- `app/src/main/java/com/example/myapp/data/remote/sync/SyncApi.kt`
- `app/src/main/java/com/example/myapp/data/sync/SyncManager.kt`
- `app/src/main/java/com/example/myapp/data/sync/SyncWorker.kt`
