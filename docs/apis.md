# APIs de imagenes de ejercicios (R2)

Este documento describe las APIs implementadas en el Worker para subir, confirmar y borrar imagenes de ejercicios.

Base URL desplegada:
- https://ratita-gym--worker.azucenapolo6.workers.dev

## Resumen rapido

1. POST /api/exercises/:exerciseId/images/presigned
2. PUT /api/exercises/:exerciseId/images/upload?token=...
3. POST /api/exercises/:exerciseId/images/upload-form
4. POST /api/exercises/:exerciseId/images/confirm
5. DELETE /api/exercises/:exerciseId/images

## Reglas globales

- Tipos permitidos: image/jpeg, image/png
- Tamano maximo: 5MB
- Estructura de key en R2: exercises/{exerciseId}/{timestamp}_{uuid}.{ext}
- Varias rutas requieren Authorization Bearer

## Variables y secretos

- IMAGE_API_TOKEN (secret)
  - Token Bearer para presigned, confirm, delete y upload-form.
- IMAGE_UPLOAD_SIGNING_SECRET (secret)
  - Secreto HMAC para firmar tokens de upload temporal.
- R2_PUBLIC_BASE_URL (var)
  - URL base publica del bucket para armar publicUrl.
- JWT_SECRET (secret)
  - Secreto para firmar y verificar JWT de autenticación de usuarios.
- SYNC_API_TOKEN (secret)
  - Token Bearer para endpoints de sincronización y mutación de ejercicios.

## Autorización y Control de Acceso

### Ejercicios Base (Sistema)

Los ejercicios base son ejercicios del catálogo del sistema que todos los usuarios pueden ver, pero solo administradores pueden modificar.

**Identificación de ejercicio base:**
- UUID con patrón: `10000000-0000-4000-8000-*` (ej: `10000000-0000-4000-8000-000000000001`)

**Restricciones de acceso:**

| Operación | Rol Requerido | Detalles |
|-----------|---------------|---------|
| GET /api/exercises/base | Público | Lectura sin autenticación |
| POST /api/exercises (crear nuevo) | ADMIN | Endpoint protegido por middlewares de sync y rol ADMIN |
| PUT /api/exercises/:id (editar) | ADMIN | Endpoint protegido por middlewares de sync y rol ADMIN |
| DELETE /api/exercises/:id (borrar) | ADMIN | Endpoint protegido por middlewares de sync y rol ADMIN |
| POST /api/sync/push (mutación) | ADMIN (solo para base) | Si ejercicio es base, solo ADMIN puede crear/editar/eliminar via sync |

**Códigos de error de autorización:**

- `401 Unauthorized` — Token Bearer ausente, inválido, expirado o JWT malformado.
- `403 Forbidden` — Token válido pero rol es insuficiente (no es ADMIN) o usuario_id no coincide.

**Autenticación en headers (endpoints protegidos):**
- Header principal: `Authorization: Bearer <TOKEN>`
- Header de usuario: `x-user-id: <UUID>` en rutas de sync/mutación.
- Las rutas con control de rol validan acceso de ADMIN para operaciones sensibles.

### Usuarios de Desarrollo

Para pruebas locales/development:

| Email | Contraseña | Rol | Hash SHA256 |
|-------|-----------|-----|-------------|
| admin@test.com | 123456 | ADMIN | 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92 |
| test@test.com | 123456 | TRAINER | 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92 |

**Nota:** En producción, cambiar contraseñas y usar hashes seguros.

## 1) Crear URL temporal de subida

Endpoint:
- POST /api/exercises/:exerciseId/images/presigned

Auth:
- Requiere Authorization: Bearer <IMAGE_API_TOKEN>

Request body (application/json):
```json
{
  "fileName": "press.jpg",
  "contentType": "image/jpeg",
  "sizeBytes": 12345
}
```

Response 200:
```json
{
  "success": true,
  "result": {
    "exerciseId": "11111111-1111-4111-8111-111111111111",
    "objectKey": "exercises/11111111-1111-4111-8111-111111111111/1775344753960_60c5cd7b-80c9-4f84-9899-680ca84b1c2d.jpg",
    "uploadUrl": "https://.../api/exercises/11111111-1111-4111-8111-111111111111/images/upload?token=...",
    "expiresAt": 1775345053960,
    "maxSizeBytes": 5242880
  }
}
```

Errores comunes:
- 401 Unauthorized (sin token o token invalido)
- 500 Upload signing secret is not configured

## 2) Subir binario con token temporal

Endpoint:
- PUT /api/exercises/:exerciseId/images/upload?token=...

Auth:
- No requiere Bearer si se envia token valido en query.

Request:
- Body binario puro (application/octet-stream o image/jpeg o image/png)
- Debe coincidir con contentType y sizeBytes solicitados en presigned.

Response 200:
```json
{
  "success": true,
  "result": {
    "exerciseId": "11111111-1111-4111-8111-111111111111",
    "objectKey": "exercises/11111111-1111-4111-8111-111111111111/...jpg",
    "contentType": "image/jpeg",
    "sizeBytes": 12345,
    "publicUrl": "https://tu-dominio-publico/exercises/11111111-1111-4111-8111-111111111111/...jpg"
  }
}
```

Errores comunes:
- 401 Invalid or expired upload token
- 400 Upload content-type does not match token
- 400 Upload size does not match token
- 413 Image too large
- 415 Unsupported media type

## 3) Fallback multipart/form-data

Endpoint:
- POST /api/exercises/:exerciseId/images/upload-form

Auth:
- Requiere Authorization: Bearer <IMAGE_API_TOKEN>

Request:
- multipart/form-data
- Campo archivo esperado: file (tambien acepta image)

Response 200:
```json
{
  "success": true,
  "result": {
    "exerciseId": "11111111-1111-4111-8111-111111111111",
    "objectKey": "exercises/11111111-1111-4111-8111-111111111111/...jpg",
    "contentType": "image/jpeg",
    "sizeBytes": 12345,
    "publicUrl": "https://tu-dominio-publico/exercises/11111111-1111-4111-8111-111111111111/...jpg"
  }
}
```

Errores comunes:
- 400 Expected multipart field 'file' with a binary image
- 401 Unauthorized
- 413 Image too large
- 415 Unsupported media type

## 4) Confirmar imagen subida

Endpoint:
- POST /api/exercises/:exerciseId/images/confirm

Auth:
- Requiere Authorization: Bearer <IMAGE_API_TOKEN>

Request body (application/json):
```json
{
  "objectKey": "exercises/11111111-1111-4111-8111-111111111111/1775344753960_60c5cd7b-80c9-4f84-9899-680ca84b1c2d.jpg"
}
```

Response 200:
```json
{
  "success": true,
  "result": {
    "exerciseId": "11111111-1111-4111-8111-111111111111",
    "objectKey": "exercises/11111111-1111-4111-8111-111111111111/...jpg",
    "contentType": "image/jpeg",
    "sizeBytes": 12345,
    "publicUrl": "https://tu-dominio-publico/exercises/11111111-1111-4111-8111-111111111111/...jpg"
  }
}
```

Errores comunes:
- 400 Object key does not belong to this exercise
- 401 Unauthorized
- 404 Image not found
- 422 Stored object is not a supported image

## 5) Borrar imagen

Endpoint:
- DELETE /api/exercises/:exerciseId/images

Auth:
- Requiere Authorization: Bearer <IMAGE_API_TOKEN>

Request body (application/json):
```json
{
  "objectKey": "exercises/11111111-1111-4111-8111-111111111111/1775344753960_60c5cd7b-80c9-4f84-9899-680ca84b1c2d.jpg"
}
```

Response 200:
```json
{
  "success": true,
  "result": {
    "exerciseId": "11111111-1111-4111-8111-111111111111",
    "objectKey": "exercises/11111111-1111-4111-8111-111111111111/...jpg"
  }
}
```

Errores comunes:
- 400 Object key does not belong to this exercise
- 401 Unauthorized

## Flujo recomendado para app

1. Solicitar presigned
2. Subir binario al uploadUrl con token
3. Confirmar objectKey
4. Guardar objectKey/publicUrl en tu backend o estado de la app
5. Borrar con DELETE cuando se reemplace o elimine la imagen

## Ejemplos curl

1) Presigned
```bash
curl -X POST "https://ratita-gym--worker.azucenapolo6.workers.dev/api/exercises/11111111-1111-4111-8111-111111111111/images/presigned" \
  -H "Authorization: Bearer <IMAGE_API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fileName":"press.jpg","contentType":"image/jpeg","sizeBytes":12345}'
```

2) Upload con token
```bash
curl -X PUT "<uploadUrl>" \
  -H "Content-Type: image/jpeg" \
  --data-binary "@./press.jpg"
```

3) Confirm
```bash
curl -X POST "https://ratita-gym--worker.azucenapolo6.workers.dev/api/exercises/11111111-1111-4111-8111-111111111111/images/confirm" \
  -H "Authorization: Bearer <IMAGE_API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"objectKey":"exercises/11111111-1111-4111-8111-111111111111/<object-key>.jpg"}'
```

4) Delete
```bash
curl -X DELETE "https://ratita-gym--worker.azucenapolo6.workers.dev/api/exercises/11111111-1111-4111-8111-111111111111/images" \
  -H "Authorization: Bearer <IMAGE_API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"objectKey":"exercises/11111111-1111-4111-8111-111111111111/<object-key>.jpg"}'
```

## APIs publicas de catalogos base

Esta seccion documenta endpoints publicos (sin autenticacion) para descargar catalogos base.

### A) Ejercicios base

Endpoint:
- GET /api/exercises/base

Auth:
- No requiere Authorization.

Query params opcionales:
- since: timestamp ms para paginación incremental (default 0)
- limit: máximo de resultados (default 200, max 500)

Response 200:
```json
{
  "success": true,
  "result": {
    "items": [
      {
        "id": "10000000-0000-4000-8000-000000000001",
        "nombre": "Press de Banca",
        "grupoMuscular": "Pecho",
        "descripcion": "Levantamiento compuesto con barra en posición horizontal",
        "idCreador": null,
        "imageUrl": null,
        "colorHex": "#E53935",
        "icono": "bench_press",
        "updatedAt": 1776465403000,
        "syncStatus": "SYNCED",
        "deletedAt": null
      }
    ],
    "nextSince": 1776465403000,
    "hasMore": false
  }
}
```

Notas:
- Solo retorna ejercicios base activos (`id` patrón `10000000%` y `syncStatus = SYNCED`).
- `nextSince` y `hasMore` permiten paginación incremental desde app.

### B) Rutinas base

Endpoint:
- GET /api/routines/base

Auth:
- No requiere Authorization.

Response 200:
```json
{
  "success": true,
  "result": {
    "items": [
      {
        "id": "00000002-0000-5000-8000-000000000002",
        "legacyId": 2,
        "nombre": "Fuerza",
        "descripcion": "Rutina 5x5 con compuestos pesados.",
        "codigo": "PRESET01",
        "colorHex": "#E53935",
        "icono": "FITNESS_CENTER",
        "idCreador": "00000000-0000-0000-0000-000000000001",
        "activa": 1,
        "fechaCreacion": 1776465403000
      }
    ],
    "total": 4
  }
}
```

Notas:
- id es UUID de rutina.
- legacyId se mantiene temporalmente por compatibilidad con clientes antiguos.
- idCreador se expone como UUID (normalizado para compatibilidad post-migración de usuarios).

### C) Links rutina-ejercicio base

Endpoint:
- GET /api/routines/base/links

Auth:
- No requiere Authorization.

Query params opcionales:
- routineId: UUID de rutina para filtrar links de una sola rutina.
- limit: maximo de registros por respuesta (default 500, max 1000).

Response 200:
```json
{
  "success": true,
  "result": {
    "items": [
      {
        "idRutina": "00000002-0000-5000-8000-000000000002",
        "legacyIdRutina": 2,
        "idEjercicio": "10000000-0000-4000-8000-000000000008",
        "series": 5,
        "reps": 5,
        "orden": 1,
        "notas": "Movimiento base bilateral",
        "updatedAt": 0,
        "syncStatus": "SYNCED",
        "deletedAt": null
      }
    ],
    "total": 26
  }
}
```

Notas:
- idRutina ahora se expone como UUID.
- legacyIdRutina se mantiene temporalmente por compatibilidad de migracion.

## Nota importante

Si publicUrl sale con placeholder, actualiza R2_PUBLIC_BASE_URL con el dominio publico real del bucket y redeploy.
