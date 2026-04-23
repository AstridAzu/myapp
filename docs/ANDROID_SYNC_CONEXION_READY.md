# Conexion Android <-> Worker: Estado de Implementacion

Este documento resume lo que ya esta terminado en el Worker para habilitar sincronizacion desde Android, junto con contrato final y checklist de prueba.

## 1. Puntos terminados en Worker

### 1.1 Endpoints de sync canonicos

- `POST /api/sync/push`
- `GET /api/sync/pull?entity=<tabla>&since=<ms>&limit=<n>`

### 1.2 Seguridad separada por dominio

- Sync usa `SYNC_API_TOKEN`.
- Imagenes usan `IMAGE_API_TOKEN`.
- Upload firmado usa `IMAGE_UPLOAD_SIGNING_SECRET`.

### 1.3 Aislamiento por usuario

- Todos los endpoints de sync requieren header `x-user-id` (UUID).
- Los cambios sync quedan particionados por usuario en D1 (`user_id`).
- En `ejercicios`, tambien se mantiene filtro por `idCreador = x-user-id`.

### 1.4 Persistencia sync en D1 (all-in)

- Store unificado: tabla `sync_items` para todas las entidades.
- LWW (last-write-wins) por timestamp de cambio entrante vs existente.
- Normalizacion de `updated_at` en servidor al confirmar escrituras.
- Tombstones soportados: `sync_status = DELETED` con `deleted_at`.
- Pull incremental ordenado: `updated_at ASC, id ASC`.
- Backfill inicial de `ejercicios` en `sync_items` ya aplicado en remoto.

### 1.5 Contrato de respuesta

- Push responde:
  - `acceptedIds: string[]`
  - `rejectedIds: string[]`
- Pull responde:
  - `items: SyncPullItem[]`
  - `nextSince: number`

### 1.6 Infra remota y despliegue

- Migracion estructural previa: `migrations/007_exercises_owner_sync_indexes.sql`.
- Migracion all-in: `migrations/008_sync_items_store.sql` (ejecutada en remoto).
- Worker desplegado en produccion.
- URL:
  - `https://ratita-gym--worker.azucenapolo6.workers.dev`

## 2. Headers requeridos desde Android

Para sync:

1. `Authorization: Bearer <SYNC_API_TOKEN>`
2. `x-user-id: <UUID usuario>`

Para imagenes:

1. `Authorization: Bearer <IMAGE_API_TOKEN>`

## 3. Contrato JSON para Android

### 3.1 Push (request)

```json
{
  "items": [
    {
      "entityType": "rutinas",
      "id": "uuid",
      "updatedAt": 1712922200000,
      "syncStatus": "PENDING",
      "payload": {
        "nombre": "Rutina Full Body",
        "frecuencia": 3,
        "activo": true
      }
    },
    {
      "entityType": "ejercicios",
      "id": "uuid",
      "updatedAt": 1712922205000,
      "syncStatus": "PENDING",
      "payload": {
        "nombre": "Sentadilla",
        "grupoMuscular": "Pierna",
        "descripcion": "texto opcional",
        "imageUrl": null,
        "colorHex": "#E85D04",
        "icono": "squat"
      }
    }
  ]
}
```

### 3.2 Push (response)

```json
{
  "acceptedIds": ["uuid-1", "uuid-2"],
  "rejectedIds": []
}
```

### 3.3 Pull (response)

```json
{
  "items": [
    {
      "entityType": "rutinas",
      "id": "uuid-1",
      "updatedAt": 1712922200000,
      "syncStatus": "SYNCED",
      "deletedAt": null,
      "payload": {
        "nombre": "Rutina Full Body",
        "frecuencia": 3,
        "activo": true
      }
    }
  ],
  "nextSince": 1712922200000
}
```

## 4. Entidades soportadas por sync

- `usuarios`
- `especialidades`
- `certificaciones`
- `objetivos`
- `ejercicios`
- `rutinas`
- `rutina_ejercicios`
- `rutina_accesos`
- `sesiones_rutina`
- `registros_series`
- `asignaciones`
- `plan_asignaciones`
- `planes_semana`
- `plan_dias`
- `plan_dias_fecha`
- `sesiones_programadas`
- `notificaciones`

## 5. Variables y secretos requeridos

### 5.1 Local (.dev.vars)

- `SYNC_API_TOKEN`
- `IMAGE_API_TOKEN`
- `IMAGE_UPLOAD_SIGNING_SECRET`

### 5.2 Remoto (Cloudflare secret)

- `SYNC_API_TOKEN`
- `IMAGE_API_TOKEN`
- `IMAGE_UPLOAD_SIGNING_SECRET`

## 6. Checklist de prueba Android

1. Configurar base URL del Worker desplegado.
2. Configurar `SYNC_API_TOKEN` en cliente de sync.
3. Enviar `x-user-id` UUID valido en cada request de sync.
4. Probar push/pull para al menos 2 entidades distintas.
5. Ejecutar pull con `since=0` por cada entidad y validar llegada de items.
6. Repetir push del mismo item y validar idempotencia (sin error, sin duplicado funcional).
7. Probar borrado logico (`syncStatus=DELETED`) y validar tombstone en pull.
8. Verificar avance monotono de `nextSince` por entidad.

## 7. Notas operativas

- Si se rota `SYNC_API_TOKEN`, Android debe actualizar token inmediatamente.
- Si falta `x-user-id` o es invalido, el Worker responde 403.
- Si `Authorization` no coincide, el Worker responde 401.
- `pull` es por entidad; Android debe iterar entidades para full refresh inicial.
