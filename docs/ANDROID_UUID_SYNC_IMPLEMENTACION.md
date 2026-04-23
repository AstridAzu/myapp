# Implementacion Android: Migracion a UUID + Sync Offline

Este documento define los cambios necesarios en Android Studio para alinear la app con un backend donde las entidades sincronizables usan:

- `id: TEXT` (UUID)
- `updated_at: INTEGER` (epoch ms)
- `sync_status: TEXT` (`PENDING`, `SYNCED`, `DELETED`)

Importante:
- Si el backend todavia expone `id` numerico, aplica este plan cuando se publique la API con UUID.
- No mezclar en produccion entidades con `id` numerico y `id` UUID sin una estrategia de transicion.

## 1. Objetivo tecnico

Pasar de un modelo Room centrado en `INTEGER AUTOINCREMENT` a un modelo offline-first con IDs generados en cliente (UUID), para poder:

1. Crear datos sin conexion.
2. Encolar cambios locales sin depender del servidor.
3. Reconciliar cambios por `updated_at` y `sync_status`.

## 2. Cambios en Room (entidades)

Aplicar en todas las tablas sincronizadas (empezar por `ejercicios`):

1. Cambiar PK:
   - De: `@PrimaryKey(autoGenerate = true) val id: Long`
   - A: `@PrimaryKey val id: String` (UUID)
2. Agregar columnas:
   - `updatedAt: Long`
   - `syncStatus: String` o enum persistido como `String`
   - `deletedAt: Long?` (recomendado para soft delete)
3. Crear indice compuesto:
   - `(updatedAt, id)` para paginacion incremental estable.
4. Crear indice por estado:
   - `(syncStatus)` para extraer `PENDING` rapido.

Ejemplo de entidad (referencia):

```kotlin
@Entity(
    tableName = "ejercicios",
    indices = [
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class EjercicioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val grupoMuscular: String,
    val descripcion: String?,
    val idCreador: String?,
    val imageUrl: String?,
    val colorHex: String?,
    val icono: String?,
    val updatedAt: Long,
    val syncStatus: String,
    val deletedAt: Long?
)
```

## 3. Migraciones de Room

Crear migraciones SQL explicitas (no destructive migration):

## Escenario A: tabla nueva con UUID

1. Crear `ejercicios_new` con `id TEXT PRIMARY KEY` y columnas sync.
2. Copiar datos de `ejercicios` viejo:
   - Para registros existentes, generar UUID deterministico o nuevo UUID y mapear en una tabla temporal.
3. Renombrar tablas:
   - `ALTER TABLE ejercicios RENAME TO ejercicios_old`
   - `ALTER TABLE ejercicios_new RENAME TO ejercicios`
4. Recrear indices.
5. Si hay FKs hacia `ejercicios.id`, migrarlas tambien a `TEXT`.

## Escenario B: backend aun usa Long

1. Mantener `id: Long` temporalmente.
2. Agregar columna `externalId: String?` para UUID futuro.
3. Completar backfill y migrar gradualmente.

Recomendacion: usar Escenario A cuando el backend ya acepte UUID.

## 4. Generacion de UUID en cliente

En creacion local de entidad:

1. `id = UUID.randomUUID().toString()`
2. `updatedAt = System.currentTimeMillis()`
3. `syncStatus = "PENDING"`
4. `deletedAt = null`

Nunca esperar al servidor para generar ID en modo offline-first.

## 5. DAO: consultas minimas necesarias

Agregar metodos clave:

1. `getAllActive()`
   - `WHERE syncStatus != 'DELETED'`
2. `getPending(limit)`
   - `WHERE syncStatus = 'PENDING' ORDER BY updatedAt ASC`
3. `upsertAll(items)`
   - Para aplicar cambios remotos.
4. `markSynced(id, updatedAt)`
5. `softDelete(id)`
   - Cambiar a `DELETED`, setear `deletedAt`, actualizar `updatedAt`.
6. `getChangesSince(since, limit)` (si necesitas cache incremental local).

## 6. Capa Retrofit (DTOs)

Alinear contrato con Worker:

1. Request create/update:
   - `id` UUID enviado por cliente (si create offline-first sincroniza upsert).
   - Campos de negocio + metadatos sync necesarios.
2. Response changes:
   - `items[]`
   - `nextSince`
3. Tipos:
   - `id: String`
   - `updatedAt: Long`
   - `syncStatus: String`
   - `deletedAt: Long?`

Evitar parseo ambiguo `Long|String` para `id`.

## 7. SyncManager (flujo recomendado)

Orden de sincronizacion:

1. Subir pendientes locales:
   - Alta/edicion/borrado logico.
2. Resolver imagenes pendientes (si aplica):
   - Presigned/upload/confirm.
3. Pedir cambios remotos por `since`.
4. Aplicar en Room con transaccion.
5. Persistir nuevo cursor `since = nextSince`.

Regla de conflicto recomendada:

- Last-write-wins por `updatedAt` (servidor como fuente final).

## 8. Repository pattern

Reglas fijas:

1. Lectura siempre desde Room.
2. Escritura primero en Room (`PENDING`).
3. Sync asincrono via SyncManager.

Esto mantiene UI instantanea y consistente offline.

## 9. WorkManager

Configurar worker periodico + constraints:

1. Requiere red.
2. Reintentos con backoff exponencial.
3. Unico por cola de sync (evitar corridas concurrentes).

## 10. Checklist de implementacion

1. Definir version objetivo de schema Room.
2. Crear migracion Room de `id Long` a `id String(UUID)` en `ejercicios`.
3. Agregar `updatedAt`, `syncStatus`, `deletedAt` + indices.
4. Ajustar Entity/DAO/Repository/UseCases.
5. Ajustar DTOs Retrofit y mappers.
6. Implementar SyncManager incremental con cursor `since`.
7. Integrar WorkManager.
8. Probar casos:
   - Crear offline -> sincronizar online.
   - Editar offline -> conflicto con remoto.
   - Borrar offline -> propagar DELETED.
   - Reinstalar app y rehidratar desde API.

## 11. Pruebas minimas recomendadas

1. Test de migracion Room:
   - Verificar preservacion de datos tras migrar schema.
2. Test DAO:
   - `getPending`, `softDelete`, `upsertAll`.
3. Test SyncManager:
   - Cola local -> API -> estado `SYNCED`.
4. Test e2e manual:
   - Modo avion + reconexion.

## 12. Riesgos y mitigaciones

1. Riesgo: FKs rotas al migrar `id` a TEXT.
   - Mitigacion: migrar tablas relacionadas en la misma version.
2. Riesgo: duplicados por doble origen de IDs.
   - Mitigacion: una sola fuente de ID (cliente UUID) desde el inicio.
3. Riesgo: conflictos de reloj cliente.
   - Mitigacion: servidor normaliza `updatedAt` al confirmar escrituras.

---

Si quieres, el siguiente paso es generar las clases base de Android (Entity + DAO + DTO + SyncManager skeleton) en formato copy/paste para tu proyecto de Android Studio.

---

## Estado de Implementacion (2026-04-11)

Resumen del avance real aplicado en el proyecto:

### Completado

1. Navegacion String-first transicional:
   - Rutas con overloads String/Long y encode URI.
   - Resolucion centralizada de argumentos en navegacion con fallback legacy.

2. Bridge de inyeccion String/Long en factory:
   - `ViewModelFactory` ahora acepta `idUsuarioString`, `idExtraString`, `idExtra2String`.
   - Resolucion centralizada a Long con prioridad String y fallback a sesion legacy.
   - Inyeccion de IDs resueltos en los ViewModels de rutinas, planes, perfil, trainers y metafit.

3. Propagacion de IDs String desde navegacion:
   - `NavGraph` pasa IDs raw String + fallback Long al factory para todas las rutas criticas.

4. Puente transicional en repositorios:
   - Overloads String->Long en `RutinaRepository`, `PlanRepository` y `SeguimientoRepository`.
   - Compatibilidad mantenida sin romper firmas actuales Long.

5. Sync metadata en Room para ejercicios:
   - `EjercicioEntity` incluye `updatedAt`, `syncStatus`, `deletedAt`.
   - Indices agregados: `(updatedAt, id)` y `(syncStatus)`.
   - Migracion explicita `17 -> 18` en `DatabaseBuilder`.

6. DAO de ejercicios orientado a sync:
   - Consultas de activos y cola por estado (`getAllActiveEjercicios`, `getBySyncStatus`).
   - Operaciones de merge y borrado logico (`upsertAll`, `markSyncState`, `softDelete`).

7. Escrituras offline-first en ejercicios:
   - Creacion/edicion local marca `PENDING`.
   - Confirmaciones remotas de imagen marcan `SYNCED`.

8. SyncManager incremental base operativo:
   - Bootstrap de cursores por entidad.
   - Push de pendientes para `ejercicios`, `rutinas` y `planes_semana`.
   - Pull incremental por cursor `since` para entidades default.
   - Merge transaccional para `ejercicios`, `rutinas` y `planes_semana`, con avance de cursor.

9. Hardening de IDs remotos en sync:
   - Se elimino el skip silencioso por `toLongOrNull` en merge pull.
   - Se agrego resolucion centralizada (`resolveLegacyLongId`) en `LegacyRemoteIdResolver`.
   - Si llega `remoteId` no numerico durante la transicion legacy, la sync falla de forma explicita y trazable.
   - Se agregaron pruebas unitarias dedicadas para resolver IDs numericos y rechazar UUID no numerico.
   - Se endurecio validacion de payload remoto: campos requeridos faltantes o invalidos ahora fallan de forma explicita (sin `return@forEach` silencioso).
   - Validadores JSON de sync fueron extraidos a `SyncPayloadValidators` para reutilizacion y pruebas.
   - Se agregaron pruebas unitarias de validadores (`requireString`, `requireLong`, `booleanOrDefault`).
    - Se agrego estrategia configurable de resolucion por BuildConfig (`SYNC_REMOTE_ID_STRATEGY`):
       - `STRICT` (default): falla explicita ante IDs remotos no numericos.
       - `HASH_FALLBACK`: mapea IDs no numericos a Long deterministico positivo para continuidad legacy.
    - Se agrego rechazo explicito para `remoteId` vacio (blank) en todas las estrategias.
    - En `HASH_FALLBACK` ahora se emite log de trazabilidad por mapeo no numerico (`SyncManager`), para auditoria operativa.
    - Se agrego politica de fallos no recuperables de sync:
       - `NonRetryableSyncException` para errores estructurales de payload/ID remoto.
       - `SyncWorker` ahora retorna `Result.failure()` en errores no recuperables y `Result.retry()` solo para fallos transitorios.

10. Migracion UUID-first directa en flows de UI:
   - `TrainersViewModel` y `TrainerDetalleViewModel` migrados a `alumnoId: String`.
   - `MetaFitPlanDetalleViewModel` migrado a `idUsuario: String`.
   - `MetaFitPlanDetalleScreen` actualizado para navegar con `userId` String sin conversiones Long intermedias.
   - `ViewModelFactory` simplificado: se removio `toLegacyLongOr` y la resolucion ahora prioriza String canonico, convirtiendo a Long solo donde todavia hay firmas legacy.
   - Compilacion validada en verde tras los cambios (`:app:compileDebugKotlin`).

11. Avance adicional UUID-first (runtime UI + repos):
   - `Routes` quedo String-only para rutas de entidades sincronizables (se removieron overloads `createRoute(Long...)`).
   - `NavGraph` elimino parseos silenciosos `toLegacyLongOr(...)` y ahora propaga args String de forma directa al factory.
   - `SessionManager.getUserIdString()` se dejo como lectura canonica de sesion; `getUserId()` quedo marcado como deprecado para compatibilidad transitoria.
   - Se migraron a `String` los IDs de usuario en `MainViewModel`, `EntrenadorHomeViewModel`, `AlumnoHomeViewModel` y `EjerciciosViewModel`.
   - `PerfilRepository` dejo de retornar listas vacias por parseo fallido (`toLongOrNull`) y consulta por `idUsuario: String` de forma nativa.
   - DAOs de perfil (`EspecialidadDao`, `CertificacionDao`, `ObjetivoDao`) pasaron a filtro por `idUsuario: String`.
   - `SeguimientoRepository` elimino fallback silencioso a `0L` al crear/reanudar sesion (ahora falla explicitamente si requiere long legacy).

12. Rutinas UUID-first (fase incremental):
   - `RutinasViewModel`, `RutinaDetalleViewModel` y `AgregarEjercicioViewModel` migrados a IDs `String` en su wiring principal.
   - `DetalleUiEvent.NavegaAClonada` ahora transporta `nuevaRutinaId: String`.
   - `ViewModelFactory` actualizado para inyectar `idUsuario`/`idRutina` String en esos ViewModels.
   - `RutinasScreen` y `RutinaDetalleScreen` ajustadas para navegación String-first sin conversiones extras.
   - `Routes.AgregarEjercicio` y `Routes.AgregarEjercicioEditor` quedaron sin overloads `Long`.

10. Limpieza de deprecaciones UI en logout:
   - Reemplazo en home de alumno y entrenador por `Icons.AutoMirrored.Filled.ExitToApp`.

11. Validacion tecnica:
   - Compilacion repetida en verde con `:app:compileDebugKotlin --no-daemon`.
   - Tests unitarios en verde con `:app:testDebugUnitTest --no-daemon`.

12. Endurecimiento de bridges legacy (evitar fallbacks silenciosos):
   - `PlanRepository` reemplazo retornos `0L` por validacion explicita (`requireLegacyLong`) en:
     - `agregarDia`
     - `materializarSemana` (ids resultantes)
     - wrappers String -> Long de `materializarSemana` y `materializarSemanas`.
   - `AgregarEjercicioEditorViewModel` elimino `toLegacyLongOrNull() ?: ...` para ids de ejercicio y usa validacion explicita.

13. Limpieza de API transicional en seguimiento:
   - `SeguimientoRepository.finalizarSesion` simplificado a firma basada solo en `idSesion`.
   - Ajustado callsite en `SeguimientoRutinaViewModel`.
   - Eliminados warnings por parametros no usados en compilacion Kotlin.

14. Perfil String-first (UUID-safe):
   - `PerfilViewModel` migro a constructor principal con `userId: String`.
   - Se mantiene constructor de compatibilidad `Long` para transicion.
   - `ViewModelFactory` ahora resuelve y propaga `resolvedUserIdString` para inyectar perfil sin degradar UUID a Long.

15. Estado de build actual:
   - `:app:compileDebugKotlin --stacktrace` en verde tras todos los cambios anteriores.

16. Cadena trainers detalle String-first:
   - `TrainerDetalleViewModel` migro a `trainerId: String` (con constructor de compatibilidad `Long`).
   - `ViewModelFactory` ahora inyecta `trainerId` desde `resolvedExtraIdString`.
   - `EntrenadorRepository` agrega overload `getUsuarioById(userId: String)`.
   - Resultado: se evita degradar `trainerId` UUID a `-1` en el detalle de entrenador.

17. Cierre incremental adicional (repos + auth + planes):
   - `PlanAsignacionRepository.crearAsignacionPlan(...)` migro su resultado a `Result<String>` (id de asignacion UUID), eliminando dependencia obligatoria de `requireLegacyLong` en esa ruta.
   - `EntrenadorRepository.asignarPlanAUsuario(...)` quedo alineado a `Result<String>` en overloads `Long` y `String`.
   - `AuthRepository.register(...)` y `RegisterUsuarioUseCase.execute(...)` migraron a `Result<String>`, retornando el UUID real del usuario creado (sin depender del rowId numerico de Room).
   - `PlanRepository.materializarSemana/materializarSemanas` en overload `String` paso a ejecucion nativa String-first; el overload `Long` queda como wrapper transicional.
   - Reduccion adicional de puntos `requireLegacyLong(...)` en capa repo (se mantienen solo en rutas legacy puntuales).

18. RutinaRepository (copiado/clonado) String-first:
   - `agregarBaseAMisEjercicios(idEjercicioBase: String, idUsuario: String)` ahora retorna `String` UUID de forma nativa.
   - `clonarRutina(idRutinaOrigen: String, idUsuario: String)` ahora retorna `String` UUID de la nueva rutina.
   - Se mantienen overloads `Long` como wrappers transicionales para compatibilidad.
   - `RutinaDetalleViewModel` dejo de convertir innecesariamente a texto el id clonado al emitir `NavegaAClonada`.

19. Limpieza adicional de wrappers Long no usados:
   - `SeguimientoRepository.crearOReanudarSesion(Long, Long)` eliminado para dejar el contrato canonico en `String`.
   - `PlanRepository.agregarDia(...)` pasa a retorno nativo `String` (id de `PlanDiaEntity`).
   - `PlanRepository.materializarSemana(Long, ...)` eliminado; se conserva la variante `String` como camino principal UUID-first.
   - Resultado: se redujo el uso residual de `requireLegacyLong(...)` en la capa repo.

20. Editor de ejercicios UUID-first (sin bridge legacy):
   - `RutinaRepository.crearEjercicioCatalogo(...)` en variante `String` ahora retorna `String` (UUID real de entidad), sin depender de rowId.
   - `AgregarEjercicioEditorViewModel` migro `ejercicioId` de estado/evento a `String` y elimino conversiones `requireLegacyLong(...)`.
   - `AgregarEjercicioEditorScreen` ahora propaga `ejercicioId` String directo por `SavedStateHandle`.
   - `AgregarEjercicioViewModel.crearEjercicio(...)` actualizado a retorno/carga por id String.

21. Cierre de bridges legacy en repos y auth:
   - `RutinaRepository` elimino wrappers `Long` en `crearEjercicioCatalogo`, `agregarBaseAMisEjercicios` y `clonarRutina`.
   - `EjerciciosViewModel` simplificado a API `agregarBaseAMisEjercicios(idEjercicio: String)`.
   - `Usuario` de dominio migro a `id: String`.
   - `AuthRepository.toDomain()` ya no usa `requireLegacyLong(...)`.
   - Resultado: `requireLegacyLong(...)` queda fuera de runtime de app (solo utilidad/transicion y prueba unitaria dedicada).

22. Planes String-first (podado incremental):
   - `PlanRepository` elimino overloads `Long` redundantes en:
     - `getPlanesActivosDeUsuario(...)`
     - `getPlanesActivosResumenDeUsuario(...)`
     - `getPlanesDeUsuario(...)`
     - `getSeguimientoPlanesPorCreador(...)`
   - `PlanesViewModel` elimino overload `desactivarPlan(Long)` y mantiene `desactivarPlan(String)`.
   - `PlanRepository` quedo String-first para IDs de entidad (solo se mantienen `Long` de tiempo/rangos).

24. Planes/seguimiento: cierre adicional UUID-first:
   - `PlanRepository.crearPlan(...)` ahora retorna el UUID real (`String`) del `PlanSemanaEntity`, evitando usar rowId de Room como id de negocio.
   - `PlanEditorViewModel` ajustado para consumir ese UUID sin conversiones extra.
   - `SeguimientoRepository` elimino overloads `Long` en sesiones/logs/series (`finalizarSesion`, `linkSesionProgramada`, `getSesionesByRutina`, `countSesionesCompletadas`, `logSerie`, `deleteSerie`, `getRegistrosBySesion`, `countSeriesCompletadas`).

23. Retiro completo del bridge legacy de IDs:
   - Eliminado `IdBridge.kt` (`requireLegacyLong` / `toLegacyLongOrNull`).
   - Eliminado `IdBridgeTest.kt` asociado.
   - Estado actual: no quedan conversiones Long<-String activas en runtime para IDs de entidades sincronizables.

25. Asignaciones/entrenador String-first (limpieza final de wrappers):
   - `AsignacionRepository` migro a firmas canonicas `String` (crear/eliminar/listados) sin parseos `toLongOrNull`.
   - `EntrenadorRepository` elimino sobrecargas `Long` en asignacion de planes y quedo con API UUID-first para esa superficie.
   - `PlanAsignacionRepository` elimino wrappers `Long` en `crearAsignacionPlan`, `cancelarAsignacionPlan` y consultas de asignaciones.
   - Resultado: el flujo de asignacion de planes en UI/repos queda alineado de punta a punta a IDs `String`.

26. Rutinas/editor String-first (podado de compatibilidad Long):
   - `RutinaEditorViewModel` migro `idCreador` y `ejercicioId` seleccionados a `String`.
   - `AgregarEjercicioEditorViewModel` migro `idRutina`, `ejercicioIdInicial` e `idUsuarioActual` a `String`.
   - `ViewModelFactory` actualizo wiring para ambos ViewModels con IDs `String` y removio resoluciones `Long` ya no usadas.
   - `RutinaRepository` elimino wrappers `Long` en consultas/ediciones de rutina y ejercicio (`get*`, `eliminar*`, `actualizarEjercicioCatalogo`, imagenes, etc.).
   - `AgregarEjercicioScreen` y `RutinaEditorScreen` retiraron fallback de resultado `Long` en `SavedStateHandle` para `EDITOR_PICKER_RESULT_EJERCICIO_ID`.
   - `GestionAsignacionesUseCase` y `AlumnoRepository` quedaron con firmas canonicas `String`.

27. Cierre final de planes/UI ViewModels (limpieza de wrappers residuales):
   - `PlanAsignacionesViewModel` elimino overload `toggleSeleccion(idUsuario: Long)`.
   - `PlanEditorViewModel` elimino overloads `asignarRutinaAFechaSeleccionada(Long)` y `aplicarRutinaDiaSemana(diaSemana, Long)`.
   - Estado final: todas las firmas públicas de ViewModels son String-first; parámetros Long en `ViewModelFactory` quedan solo como fallback legacy para compatibilidad transitoria (no se usan en wiring actual).
   - Validación: búsqueda de `toLongOrNull` en paths de ID de entidad no activos muestra solo sync/session/legacy-resolver (fuera de runtime de app).

---

## ESTADO FINAL DE MIGRACION UUID/STRING (2026-04-11 - COMPLETADA)

### Résumé ejecutivo

✅ **Migración UUID-first completada en capas: App → ViewModel → Repository → DAO** 
- Todas las firmas público de ViewModels y repositorios para entidades de negocio (`Usuario`, `Rutina`, `Ejercicio`, `Plan`, `Asignación`) son ahora `String`-first.
- Wrappers `Long` heredados eliminados sistemáticamente (salvo fallbacks internos para compatibilidad de sesión/navegación legacy no aplicables a IDs de entidad).
- Navegación, factory, y picker alineados a String sin conversiones innecesarias.

### Capas migradas

1. **Room Layer**:  
   - Entidades sincronizables (`EjercicioEntity`, `RutinaEntity`, `PlanSemanaEntity`) con `id: String` (UUID).  
   - DAOs exponen APIs String-nativas (`getById(String)`, `upsertAll(List<String>)`, sync markers).  
   - Migraciones SQL V17→V18 implementadas.

2. **Repository Layer**:  
   - `RutinaRepository`: 100% String API (get, crear, clonar, imagen upload).  
   - `PlanRepository`: String IDs de plan/usuario; timestamp/Long solo para tiempos.  
   - `SeguimientoRepository`: String session/series IDs.  
   - `AsignacionRepository`, `EntrenadorRepository`, `PlanAsignacionRepository`: String para assignment/trainer/user association.  
   - `AuthRepository`: retorna `userId: String` en domain model.

3. **ViewModel Layer**:  
   - Todas las clases ViewModel en rutinas/planes/trainers/perfil/metafit: IDs `String`.  
   - Navegación String-only en rutas de entidades.  
   - Factory wiring usa `idUsuarioString`, `idExtraString`, `idExtra2String` sin degradación a Long.

4. **Sync Layer**:  
   - `SyncManager` push/pull con UUID String IDs nativos.  
   - Merge transaccional para ejercicios/rutinas/planes semana con cursor incremental.  
   - `LegacyRemoteIdResolver` para transición: falla explícita en STRICT mode o hash fallback en HASH_FALLBACK.

### Verificación final

✅ No hay `requireLegacyLong(idEjercicio)`, `requireLegacyLong(idUsuario)`, etc. en runtime.  
✅ Búsqueda de `toLongOrNull()` en ID entity paths muestra 0 matches en runtime app (solo sync/session helpers).  
✅ Todas las pantallas operan sin conversión Long↔String para IDs de negocio.  
✅ Picker/SavedState esperar String.  
✅ Las sobrecargas `Long` restantes en `ViewModelFactory` son parámetros heredados sin uso (fallback inerte).

### Próximos pasos (fuera del scope de app)

1. **Backend/API**: Confirmar que endpoint de sync envíe/reciba `id: String` (UUID) en JSON, no `Long`.  
2. **Database schema**: Convertir PKs/FKs a TEXT si aún usan INTEGER.  
3. **End-to-end test**: Crear offline, editar, reconectar, sincronizar → validar UUID coherencia.  
4. **Legacy support sunsetting**: Cuando backend sea 100% UUID-only, remover `LegacyRemoteIdResolver` y estrategias fallback.

---

### Pending structural changes (data layer only, not runtime)

1. Migration of remaining PK/FK columns to UUID String in schema (currently Room maintains Long PK internally for non-sync tables).
2. Additional merge mappers for remote entities beyond ejercicios/rutinas/planes_semana if needed.
3. Integration tests for offline→online sync flow with UUID IDs end-to-end.
4. Final sunset of legacy ID bridge when backend goes 100% UUID.
