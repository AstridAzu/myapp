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

### A) Rutinas base

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
        "idCreador": 0,
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

### B) Links rutina-ejercicio base

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
