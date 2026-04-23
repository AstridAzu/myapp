# Base de Datos — Atlas App

Documentación de la base de datos SQLite local gestionada con **Room** para la aplicación Android Atlas.

---

## Configuración General

| Parámetro | Valor |
|---|---|
| Nombre del archivo | `atlas_database` |
| Versión actual | `14` |
| Framework | Android Room (SQLite) |
| Estrategia de migración | Migraciones explícitas `9 -> 10`, `10 -> 11`, `11 -> 12`, `12 -> 13` y `13 -> 14` + `fallbackToDestructiveMigration()` para versiones no cubiertas |
| Exportar esquema | `true` |
| Singleton | `DatabaseBuilder.kt` — doble verificación de bloqueo (double-checked locking) |
| Semilla de datos | `onOpen` callback — idempotente vía estrategia `IGNORE` |

---

## Diagrama de Relaciones (ERD)

```
usuarios ──────────────< especialidades
    │                        (idUsuario FK)
    │
    ├──────────────────< certificaciones
    │                        (idUsuario FK)
    │
    ├──────────────────< objetivos
    │                        (idUsuario FK)
    │
    ├──────────────────< sesiones_rutina
    │                        (idUsuario FK, idRutina FK lógica)
    │
    ├──────────────────< planes_semana (idUsuario + idCreador)
    │                        │
    │                        └──< plan_dias
    │                                 │
    │                                 └──< sesiones_programadas
    │                                           (idPlanDia FK CASCADE)
    │                                           (idSesion FK lógica → sesiones_rutina)
    │
    └──────────────────< rutinas (via idCreador)
                              │
                              ├──< rutina_ejercicios >── ejercicios
                              │     (join M:N con series/reps/orden)
                              │
                              ├──< rutina_accesos >── usuarios
                              │     (join M:N de control de acceso)
                              │
                              └──< sesiones_rutina
                                        │
                                        └──< registros_series
                                              (idSesion FK, idEjercicio FK lógica)

asignaciones >── usuarios × 2
    (idUsuarioOrigen FK -> usuarios.id)
    (idUsuarioDestino FK -> usuarios.id)
```

---

## Esquema de Tablas

### `usuarios`

Tabla principal de usuarios del sistema. Soporta dos roles: `ENTRENADOR` y `ALUMNO`.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `email` | `TEXT` | No | UNIQUE | Índice único |
| `passwordHash` | `TEXT` | No | | Hash de la contraseña |
| `nombre` | `TEXT` | No | | Nombre visible |
| `rol` | `TEXT` | No | | `"ENTRENADOR"` o `"ALUMNO"` |
| `activo` | `INTEGER` | No | DEFAULT `1` | Boolean (0/1) |
| `fechaRegistro` | `INTEGER` | No | | Timestamp epoch ms |

**Índices:** `email` (UNIQUE)

---

### `rutinas`

Rutinas de entrenamiento creadas por entrenadores o por el sistema (presets).

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idCreador` | `INTEGER` | No | | FK lógica a `usuarios.id`; `0` = preset del sistema |
| `nombre` | `TEXT` | No | | |
| `descripcion` | `TEXT` | Sí | | |
| `fechaCreacion` | `INTEGER` | No | | Timestamp epoch ms |
| `activa` | `INTEGER` | No | DEFAULT `1` | Boolean (0/1) |
| `codigo` | `TEXT` | No | UNIQUE | Identificador alfanumérico, ej. `PRESET01` |
| `colorHex` | `TEXT` | Sí | | Color de acento en formato hex, ej. `#E53935`. Null = fallback por defecto |
| `icono` | `TEXT` | Sí | | Key del ícono, ej. `FITNESS_CENTER`. Null = fallback por defecto |

**Índices:** `idCreador`, `codigo` (UNIQUE)

---

### `rutina_ejercicios`

Tabla de unión M:N entre `rutinas` y `ejercicios`. Almacena los parámetros de cada ejercicio dentro de una rutina.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `idRutina` | `INTEGER` | No | PK (compuesta), FK → `rutinas.id` ON DELETE CASCADE | |
| `idEjercicio` | `INTEGER` | No | PK (compuesta), FK → `ejercicios.id` ON DELETE CASCADE | |
| `series` | `INTEGER` | No | | Número de series |
| `reps` | `INTEGER` | No | | Repeticiones por serie |
| `orden` | `INTEGER` | No | | Posición dentro de la rutina |
| `notas` | `TEXT` | Sí | | Instrucciones adicionales |

**PK compuesta:** (`idRutina`, `idEjercicio`)  
**Índices:** `idRutina`, `idEjercicio`  
**FK:** `idRutina` → `rutinas.id` CASCADE, `idEjercicio` → `ejercicios.id` CASCADE

---

### `rutina_accesos`

Tabla de unión M:N que controla qué usuarios tienen acceso a qué rutinas (asignación entrenador → alumno).

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `idRutina` | `INTEGER` | No | PK (compuesta), FK → `rutinas.id` ON DELETE CASCADE | |
| `idUsuario` | `INTEGER` | No | PK (compuesta), FK → `usuarios.id` ON DELETE CASCADE | |
| `fechaAcceso` | `INTEGER` | No | | Timestamp epoch ms de la asignación |

**PK compuesta:** (`idRutina`, `idUsuario`)  
**Índices:** `idRutina`, `idUsuario`  
**FK:** `idRutina` → `rutinas.id` CASCADE, `idUsuario` → `usuarios.id` CASCADE

---

### `objetivos`

Objetivos personales definidos por o para un usuario (alumno).

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idUsuario` | `INTEGER` | No | FK → `usuarios.id` ON DELETE CASCADE | Índice |
| `descripcion` | `TEXT` | No | | Texto libre del objetivo |

**Índices:** `idUsuario`  
**FK:** `idUsuario` → `usuarios.id` CASCADE

---

### `especialidades`

Especialidades profesionales declaradas por un entrenador.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idUsuario` | `INTEGER` | No | FK → `usuarios.id` ON DELETE CASCADE | Índice |
| `nombre` | `TEXT` | No | | Nombre de la especialidad |

**Índices:** `idUsuario`  
**FK:** `idUsuario` → `usuarios.id` CASCADE

---

### `ejercicios`

Catálogo de ejercicios disponibles en el sistema. No tiene claves foráneas.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `nombre` | `TEXT` | No | | Nombre del ejercicio |
| `grupoMuscular` | `TEXT` | No | | Ej. `"Pecho"`, `"Pierna"`, `"Espalda"` |
| `descripcion` | `TEXT` | Sí | | Instrucciones o descripción |
| `imageUrl` | `TEXT` | Sí | | URL remota de imagen del ejercicio. Null = usar imagen genérica local |
| `colorHex` | `TEXT` | Sí | | Override de color en hex. Null = usar color del `grupoMuscular` |
| `icono` | `TEXT` | Sí | | Key del ícono. Null = fallback por defecto |

---

### `sesiones_rutina`

Registra las sesiones de entrenamiento iniciadas por un alumno sobre una rutina. Persiste el estado (activa / completada) y las marcas de tiempo para historial y cronómetro.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idRutina` | `INTEGER` | No | | FK lógica a `rutinas.id` |
| `idUsuario` | `INTEGER` | No | | FK lógica a `usuarios.id` |
| `fechaInicio` | `INTEGER` | No | | Timestamp epoch ms de inicio de sesión |
| `fechaFin` | `INTEGER` | Sí | | Timestamp epoch ms de finalización. Null si la sesión está en curso |
| `completada` | `INTEGER` | No | DEFAULT `0` | Boolean 0 = en curso, 1 = completada |

**Índices sugeridos:** `idRutina`, `idUsuario`

---

### `registros_series`

Almacena el log de cada serie realizada dentro de una sesión: peso levantado, reps ejecutadas y marca de completado.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idSesion` | `INTEGER` | No | FK lógica a `sesiones_rutina.id` | |
| `idEjercicio` | `INTEGER` | No | FK lógica a `ejercicios.id` | |
| `numeroSerie` | `INTEGER` | No | | 1-based (1 = primera serie) |
| `pesoKg` | `REAL` | No | | Kilogramos levantados |
| `repsRealizadas` | `INTEGER` | No | | Repeticiones completadas |
| `completada` | `INTEGER` | No | DEFAULT `1` | Bool; siempre 1 al insertar (se inserta al marcar) |

Conflicto de inserción: `REPLACE` — permite actualizar peso/reps de una serie ya marcada.

**Índices sugeridos:** `idSesion`, `idEjercicio`

---

### `certificaciones`

Certificaciones profesionales obtenidas por un entrenador.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idUsuario` | `INTEGER` | No | FK → `usuarios.id` ON DELETE CASCADE | Índice |
| `nombre` | `TEXT` | No | | Nombre de la certificación |
| `institucion` | `TEXT` | No | | Institución emisora |
| `fechaObtencion` | `INTEGER` | No | | Timestamp epoch ms |

**Índices:** `idUsuario`  
**FK:** `idUsuario` → `usuarios.id` CASCADE

---

### `planes_semana`

Cabecera de un plan de entrenamiento semanal. Define el rango de fechas y a qué usuario aplica.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idCreador` | `INTEGER` | No | | FK lógica a `usuarios.id` — quien construyó el plan |
| `idUsuario` | `INTEGER` | No | | FK lógica a `usuarios.id` — alumno al que aplica |
| `nombre` | `TEXT` | No | | Ej. `"Plan Enero–Marzo"` |
| `fechaInicio` | `INTEGER` | No | | Epoch ms — primer día inclusivo del plan |
| `fechaFin` | `INTEGER` | No | | Epoch ms — último día inclusivo del plan |
| `activo` | `INTEGER` | No | DEFAULT `1` | Boolean; se permiten múltiples planes activos simultáneamente |
| `fechaCreacion` | `INTEGER` | No | | Timestamp epoch ms |

**Índices:** `idUsuario`, `idCreador`

---

### `plan_dias`

Plantilla semanal: cada fila representa una ranura de entrenamiento (rutina o descanso) en un día de la semana.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idPlan` | `INTEGER` | No | FK → `planes_semana.id` ON DELETE CASCADE | |
| `diaSemana` | `INTEGER` | No | | 1 = Lunes … 7 = Domingo (ISO 8601) |
| `tipo` | `TEXT` | No | | `"RUTINA"` o `"DESCANSO"` |
| `idRutina` | `INTEGER` | Sí | | FK lógica a `rutinas.id`; null cuando `tipo = "DESCANSO"` |
| `orden` | `INTEGER` | No | DEFAULT `1` | Posición dentro del día (permite múltiples rutinas) |
| `notas` | `TEXT` | Sí | | Indicaciones adicionales del día |

**Índices:** `idPlan`, `(idPlan, diaSemana)`  
**FK:** `idPlan` → `planes_semana.id` CASCADE

---

### `sesiones_programadas`

Materializa cada ocurrencia concreta de un `plan_dia` en una fecha calendario real. Se genera de forma lazy al consultar una semana. El índice UNIQUE garantiza idempotencia del materializador.

| Columna | Tipo SQLite | Nullable | Restricciones | Notas |
|---|---|---|---|---|
| `id` | `INTEGER` | No | PK, AUTOINCREMENT | |
| `idPlanDia` | `INTEGER` | No | FK → `plan_dias.id` ON DELETE CASCADE | |
| `fechaProgramada` | `INTEGER` | No | UNIQUE con `idPlanDia` | Epoch ms de medianoche UTC del día programado |
| `idSesion` | `INTEGER` | Sí | | FK lógica a `sesiones_rutina.id`; null = no iniciada |
| `completada` | `INTEGER` | No | DEFAULT `0` | 1 = sesión finalizada y vinculada |
| `omitida` | `INTEGER` | No | DEFAULT `0` | 1 = usuario marcó el día como omitido |

**Índices:** `idPlanDia`, `fechaProgramada`, `(idPlanDia, fechaProgramada)` (UNIQUE)  
**FK:** `idPlanDia` → `plan_dias.id` CASCADE

---

## `asignaciones`

Relación explícita usuario -> usuario para asignaciones origen -> destino.

| Columna | Tipo SQLite | Nullable | Restricciones |
|---|---|---|---|
| `idUsuarioOrigen` | `INTEGER` | No | PK (compuesta), FK → `usuarios.id` CASCADE |
| `idUsuarioDestino` | `INTEGER` | No | PK (compuesta), FK → `usuarios.id` CASCADE |
| `fechaAsignacion` | `INTEGER` | No | Timestamp epoch ms |

**PK compuesta:** (`idUsuarioOrigen`, `idUsuarioDestino`)  
**Índices:** `idUsuarioOrigen`, `idUsuarioDestino`

Notas de negocio:
- El usuario origen debe tener `rol = ENTRENADOR`.
- El usuario destino debe tener `rol = ALUMNO`.
- Se permiten múltiples usuarios origen por usuario destino.

---

## DAOs

### `UsuarioDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(usuario)` | `@Insert` | `ABORT` (error si duplicado) |
| `insertIgnore(usuario)` | `@Insert` | `IGNORE` (silencia duplicados) |
| `update(usuario)` | `@Update` | — |
| `delete(usuario)` | `@Delete` | — |
| `getUserByEmail(email)` | `@Query` | `SELECT * FROM usuarios WHERE email = :email LIMIT 1` |
| `getUserById(id)` | `@Query` | `SELECT * FROM usuarios WHERE id = :id LIMIT 1` |
| `getAllUsuarios()` | `@Query` | `SELECT * FROM usuarios` → `Flow<List<UsuarioEntity>>` |
| `searchByNombre(query)` | `@Query` | `SELECT * FROM usuarios WHERE nombre LIKE '%' \|\| :query \|\| '%'` |

---

### `RutinaDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insertRutina(rutina)` | `@Insert` | default |
| `insertRutinaIgnore(rutina)` | `@Insert` | `IGNORE` |
| `insertRutinaEjercicios(lista)` | `@Insert` | default |
| `insertRutinaEjerciciosIgnore(lista)` | `@Insert` | `IGNORE` — idempotente al sembrar presets |
| `deleteRutinaById(id)` | `@Query` | `DELETE FROM rutinas WHERE id = :id` — CASCADE borra `rutina_ejercicios` y `rutina_accesos` |
| `getRutinasByCreador(idCreador)` | `@Query` | `SELECT * FROM rutinas WHERE idCreador = :idCreador` → `Flow` |
| `getPresetRutinas()` | `@Query` | `SELECT * FROM rutinas WHERE idCreador = 0` → `Flow` |
| `getRutinaById(id)` | `@Query` | `SELECT * FROM rutinas WHERE id = :id LIMIT 1` → `Flow<RutinaEntity?>` |
| `getRutinaByCodigo(codigo)` | `@Query` | `SELECT * FROM rutinas WHERE codigo = :codigo LIMIT 1` |
| `getEjerciciosByRutina(idRutina)` | `@Query` | `SELECT * FROM rutina_ejercicios WHERE idRutina = :idRutina ORDER BY orden ASC` → `Flow` |
| `getEjerciciosConDetalle(idRutina)` | `@Query` | JOIN `rutina_ejercicios` + `ejercicios` → `Flow<List<EjercicioConDetalle>>` |
| `insertRutinaEjercicio(entity)` | `@Insert` | Inserta un `RutinaEjercicioEntity`, retorna `Long` (rowId) |
| `deleteRutinaEjercicio(idRutina, idEjercicio)` | `@Query` | `DELETE FROM rutina_ejercicios WHERE idRutina=:idRutina AND idEjercicio=:idEjercicio` |
| `getNextOrden(idRutina)` | `@Query` | `SELECT COALESCE(MAX(orden)+1, 1) FROM rutina_ejercicios WHERE idRutina=:idRutina` → `suspend Int` |
| `getRutinaEjerciciosRaw(idRutina)` | `@Query` | `SELECT * FROM rutina_ejercicios WHERE idRutina=:idRutina ORDER BY orden` → `suspend List<RutinaEjercicioEntity>` (para clonar) |
| `deactivateAllRutinasForCreador(idCreador)` | `@Query` | `UPDATE rutinas SET activa = 0 WHERE idCreador = :idCreador` |
| `activateRutina(idRutina)` | `@Query` | `UPDATE rutinas SET activa = 1 WHERE id = :idRutina` |
| `setRutinaActiva(idRutina, idCreador)` | `@Transaction` | llama `deactivateAllRutinasForCreador` → `activateRutina` |

**DTO interno:**
```kotlin
data class EjercicioConDetalle(
    val idEjercicio: Long,
    val nombre: String,
    val grupoMuscular: String,
    val series: Int,
    val reps: Int,
    val orden: Int,
    val notas: String?
)
```

---

### `RutinaAccesoDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(acceso)` | `@Insert` | `IGNORE` |
| `delete(acceso)` | `@Delete` | — |
| `getRutinasByUsuario(idUsuario)` | `@Query` | JOIN `rutinas` + `rutina_accesos` → `Flow<List<RutinaEntity>>` |
| `getUsuariosByRutina(idRutina)` | `@Query` | JOIN `usuarios` + `rutina_accesos` → `Flow<List<UsuarioEntity>>` |
| `tieneAcceso(idRutina, idUsuario)` | `@Query` | `SELECT COUNT(*) FROM rutina_accesos WHERE idRutina = :idRutina AND idUsuario = :idUsuario` |

---

### `ObjetivoDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(objetivo)` | `@Insert` | `REPLACE` (upsert) |
| `delete(objetivo)` | `@Delete` | — |
| `getObjetivosByUsuario(idUsuario)` | `@Query` | `SELECT * FROM objetivos WHERE idUsuario = :idUsuario` → `Flow` |

---

### `EspecialidadDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(especialidad)` | `@Insert` | `REPLACE` (upsert) |
| `delete(especialidad)` | `@Delete` | — |
| `getEspecialidadesByUsuario(idUsuario)` | `@Query` | `SELECT * FROM especialidades WHERE idUsuario = :idUsuario` → `Flow` |

---

### `EjercicioDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(ejercicio)` | `@Insert` | default |
| `insertIgnore(ejercicio)` | `@Insert` | `IGNORE` |
| `count()` | `@Query` | `SELECT COUNT(*) FROM ejercicios` |
| `getAllEjercicios()` | `@Query` | `SELECT * FROM ejercicios` → `Flow` |
| `getEjerciciosByGrupo(grupo)` | `@Query` | `SELECT * FROM ejercicios WHERE grupoMuscular = :grupo` → `Flow` |
| `getEjercicioById(id)` | `@Query` | `SELECT * FROM ejercicios WHERE id = :id LIMIT 1` → `suspend EjercicioEntity?` |
| `getByNombre(nombre)` | `@Query` | `SELECT * FROM ejercicios WHERE nombre = :nombre LIMIT 1` → `suspend EjercicioEntity?` — usado por el seed de presets |

---

### `CertificacionDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(certificacion)` | `@Insert` | `REPLACE` (upsert) |
| `delete(certificacion)` | `@Delete` | — |
| `getCertificacionesByUsuario(idUsuario)` | `@Query` | `SELECT * FROM certificaciones WHERE idUsuario = :idUsuario` → `Flow` |

---

### `SesionRutinaDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insertSesion(sesion)` | `@Insert` | Retorna el `Long` rowId de la sesión creada |
| `updateSesion(sesion)` | `@Update` | Actualiza estado, `fechaFin`, `completada` |
| `getSesionById(id)` | `@Query` | `SELECT * FROM sesiones_rutina WHERE id = :id LIMIT 1` → `suspend SesionRutinaEntity?` |
| `getSesionesByRutina(idRutina, idUsuario)` | `@Query` | `SELECT * FROM sesiones_rutina WHERE idRutina=:idRutina AND idUsuario=:idUsuario ORDER BY fechaInicio DESC` → `Flow` |
| `getSesionActiva(idRutina, idUsuario)` | `@Query` | `SELECT * … WHERE completada = 0 LIMIT 1` → `suspend SesionRutinaEntity?` |
| `getSesionesCompletadas(idRutina, idUsuario)` | `@Query` | `SELECT * … WHERE completada = 1` → `Flow` |
| `countSesionesCompletadas(idRutina, idUsuario)` | `@Query` | `SELECT COUNT(*) … WHERE completada = 1` → `Flow<Int>` |

---

### `RegistroSerieDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insertRegistro(registro)` | `@Insert(REPLACE)` | Upsert — permite corregir peso/reps |
| `updateRegistro(registro)` | `@Update` | Actualización directa |
| `deleteRegistro(idSesion, idEjercicio, numeroSerie)` | `@Query` | `DELETE FROM registros_series WHERE …` (desmarcar serie) |
| `getRegistrosBySesion(idSesion)` | `@Query` | `SELECT * … ORDER BY idEjercicio, numeroSerie` → `Flow` |
| `getRegistrosByEjercicio(idSesion, idEjercicio)` | `@Query` | Lista por ejercicio → `suspend List` |
| `countSeriesCompletadas(idSesion)` | `@Query` | `SELECT COUNT(*) FROM registros_series WHERE idSesion=:idSesion` → `Flow<Int>` |

---

### `PlanSemanaDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(plan)` | `@Insert` | `ABORT` |
| `update(plan)` | `@Update` | — |
| `delete(plan)` | `@Delete` | — |
| `deleteById(id)` | `@Query` | `DELETE FROM planes_semana WHERE id = :id` |
| `getPlanesActivosByUsuario(idUsuario)` | `@Query` | `SELECT * … WHERE idUsuario = :idUsuario AND activo = 1 ORDER BY fechaCreacion DESC` → `Flow<List<PlanSemanaEntity>>` |
| `getPlanesDeUsuario(idUsuario)` | `@Query` | `SELECT * … WHERE idUsuario = :idUsuario ORDER BY fechaCreacion DESC` → `Flow` |
| `getPlanesCreados(idCreador)` | `@Query` | `SELECT * … WHERE idCreador = :idCreador ORDER BY fechaCreacion DESC` → `Flow` |
| `getById(id)` | `@Query` | `SELECT * FROM planes_semana WHERE id = :id LIMIT 1` → `suspend PlanSemanaEntity?` |
| `activar(id)` | `@Query` | `UPDATE planes_semana SET activo = 1 WHERE id = :id` |
| `desactivar(id)` | `@Query` | `UPDATE planes_semana SET activo = 0 WHERE id = :id` |
| `getSeguimientoPlanesPorCreador(idCreador)` | `@Query` | Resumen agregado por plan (`totalProgramadas`, `totalCompletadas`, `totalOmitidas`, `ultimaActividad`) → `Flow<List<PlanSeguimientoRow>>` |

**DTO interno:**
```kotlin
data class PlanSeguimientoRow(
    val idPlan: Long,
    val nombrePlan: String,
    val idUsuario: Long,
    val nombreUsuario: String,
    val activo: Boolean,
    val totalProgramadas: Int,
    val totalCompletadas: Int,
    val totalOmitidas: Int,
    val ultimaActividad: Long?
)
```

---

### `PlanDiaDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(dia)` | `@Insert` | `ABORT` |
| `insertAll(dias)` | `@Insert` | `ABORT` |
| `update(dia)` | `@Update` | — |
| `delete(dia)` | `@Delete` | — |
| `deleteById(id)` | `@Query` | `DELETE FROM plan_dias WHERE id = :id` |
| `getDiasByPlan(idPlan)` | `@Query` | `SELECT * … ORDER BY diaSemana, orden` → `Flow` |
| `getDiasByPlanOnce(idPlan)` | `@Query` | Igual que el anterior → `suspend List` |
| `getDiasByPlanAndDia(idPlan, diaSemana)` | `@Query` | `SELECT * … WHERE idPlan = :idPlan AND diaSemana = :diaSemana ORDER BY orden` → `Flow` |
| `getNextOrden(idPlan, diaSemana)` | `@Query` | `SELECT COALESCE(MAX(orden)+1, 1) FROM plan_dias WHERE …` → `suspend Int` |
| `deleteDiasByPlan(idPlan)` | `@Query` | `DELETE FROM plan_dias WHERE idPlan = :idPlan` |

---

### `SesionProgramadaDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(sesion)` | `@Insert` | `IGNORE` — idempotente por índice UNIQUE |
| `insertAll(sesiones)` | `@Insert` | `IGNORE` |
| `update(sesion)` | `@Update` | — |
| `getByRangoFecha(desde, hasta)` | `@Query` | `SELECT * … WHERE fechaProgramada BETWEEN :desde AND :hasta` → `Flow` |
| `getByRangoFechaOnce(desde, hasta)` | `@Query` | Igual → `suspend List` |
| `getByPlanDia(idPlanDia)` | `@Query` | `SELECT * … WHERE idPlanDia = :idPlanDia` → `Flow` |
| `getById(id)` | `@Query` | `SELECT * FROM sesiones_programadas WHERE id = :id LIMIT 1` → `suspend` |
| `linkSesion(id, idSesion)` | `@Query` | `UPDATE sesiones_programadas SET idSesion = :idSesion WHERE id = :id` |
| `marcarCompletada(id)` | `@Query` | `UPDATE sesiones_programadas SET completada = 1 WHERE id = :id` |
| `marcarOmitida(id)` | `@Query` | `UPDATE … SET omitida = 1, idSesion = NULL WHERE id = :id` |
| `desmarcarOmitida(id)` | `@Query` | `UPDATE … SET omitida = 0 WHERE id = :id` |
| `getBySesionRutina(idSesion)` | `@Query` | `SELECT * FROM sesiones_programadas WHERE idSesion = :idSesion LIMIT 1` → `suspend` |

---

### `AsignacionDao`

| Método | Operación | SQL / Estrategia |
|---|---|---|
| `insert(asignacion)` | `@Insert` | `REPLACE` |
| `delete(asignacion)` | `@Delete` | — |
| `getAsignacion(idOrigen, idDestino)` | `@Query` | `SELECT * FROM asignaciones WHERE idUsuarioOrigen = :idOrigen AND idUsuarioDestino = :idDestino LIMIT 1` |
| `getDestinosByOrigen(idOrigen)` | `@Query` | `SELECT idUsuarioDestino FROM asignaciones WHERE idUsuarioOrigen = :idOrigen` → `Flow<List<Long>>` |
| `getOrigenesByDestino(idDestino)` | `@Query` | `SELECT idUsuarioOrigen FROM asignaciones WHERE idUsuarioDestino = :idDestino` → `suspend List<Long>` |

---

## Repositorios

| Repositorio | Archivo | DAOs / Dependencias |
|---|---|---|
| `AuthRepository` | [AuthRepository.kt](app/src/main/java/com/example/myapp/data/repository/AuthRepository.kt) | `UsuarioDao` |
| `AsignacionRepository` | [AsignacionRepository.kt](app/src/main/java/com/example/myapp/data/repository/AsignacionRepository.kt) | `AsignacionDao`, `UsuarioDao` |
| `RutinaRepository` | [RutinaRepository.kt](app/src/main/java/com/example/myapp/data/repository/RutinaRepository.kt) | `RutinaDao`, `RutinaAccesoDao`, `UsuarioDao`, `EjercicioDao` |
| `EntrenadorRepository` | [EntrenadorRepository.kt](app/src/main/java/com/example/myapp/data/repository/EntrenadorRepository.kt) | Delega a `RutinaRepository` |
| `AlumnoRepository` | [AlumnoRepository.kt](app/src/main/java/com/example/myapp/data/repository/AlumnoRepository.kt) | `UsuarioDao`, `ObjetivoDao` |
| `SeguimientoRepository` | [SeguimientoRepository.kt](app/src/main/java/com/example/myapp/data/repository/SeguimientoRepository.kt) | `SesionRutinaDao`, `RegistroSerieDao`, `PlanRepository?` (opcional) |
| `PlanRepository` | [PlanRepository.kt](app/src/main/java/com/example/myapp/data/repository/PlanRepository.kt) | `PlanSemanaDao`, `PlanDiaDao`, `SesionProgramadaDao` |

---

## Datos Semilla (`onOpen` callback)

Los datos se insertan cada vez que se abre la base de datos usando estrategia `IGNORE` — son idempotentes.

### Usuarios de prueba

| Email | Contraseña | Rol |
|---|---|---|
| `test@test.com` | `123456` | `ENTRENADOR` |
| `alumno@test.com` | `123456` | `ALUMNO` |

> ⚠️ Las contraseñas se almacenan como hash. Reemplazar antes de producción.

### Ejercicios predefinidos

48 ejercicios sembrados agrupados por músculo. Solo se insertan si la tabla está vacía (`count() == 0`).

#### Pecho (7)

| Nombre |
|---|
| Press de Banca |
| Press Inclinado con Barra |
| Press Declinado con Barra |
| Aperturas con Mancuernas |
| Fondos en Paralelas |
| Pullover con Mancuerna |
| Cruces en Polea |

#### Pierna (7)

| Nombre |
|---|
| Sentadilla |
| Sentadilla Búlgara |
| Prensa de Pierna |
| Extensión de Cuádriceps |
| Curl Femoral Tumbado |
| Zancadas |
| Elevación de Talones |

#### Espalda (7)

| Nombre |
|---|
| Peso Muerto |
| Dominadas |
| Remo con Barra |
| Remo con Mancuerna |
| Jalón al Pecho |
| Remo en Polea Baja |
| Hiperextensiones |

#### Hombro (6)

| Nombre |
|---|
| Press Militar |
| Arnold Press |
| Elevaciones Laterales |
| Vuelos Posteriores |
| Elevaciones Frontales |
| Encogimientos de Hombros |

#### Brazos (6)

| Nombre |
|---|
| Curl de Bíceps con Barra |
| Curl Martillo |
| Curl Concentrado |
| Press Francés |
| Tríceps en Polea |
| Fondos para Tríceps |

#### Core / Abdomen (6)

| Nombre |
|---|
| Plancha |
| Crunch Abdominal |
| Russian Twist |
| Elevación de Piernas |
| Rueda Abdominal |
| Bicicleta Abdominal |

#### Glúteos (4)

| Nombre |
|---|
| Hip Thrust con Barra |
| Patada de Glúteo |
| Abductor en Máquina |
| Sentadilla Sumo |

#### Cardio (4)

| Nombre |
|---|
| Burpees |
| Saltos de Cuerda |
| Mountain Climbers |
| Jumping Jacks |

### Rutinas preset del sistema (`idCreador = 0`)

| Código | Nombre | colorHex | icono |
|---|---|---|---|
| `PRESET01` | Fuerza | `#E53935` | `FITNESS_CENTER` |
| `PRESET02` | Resistencia | `#FF6F00` | `DIRECTIONS_RUN` |
| `PRESET03` | Flexibilidad | `#00897B` | `SELF_IMPROVEMENT` |
| `PRESET04` | Hipertrofia Funcional | `#31CAF8` | `BOLT` |

### Ejercicios asignados a rutinas preset

Sembrados por `seedRutinaEjerciciosPreset` en `onOpen`. Usa `insertRutinaEjerciciosIgnore` — idempotente.

#### PRESET01 — Fuerza (5×5, compuestos pesados)

| Orden | Ejercicio | Series | Reps |
|---|---|---|---|
| 1 | Sentadilla | 5 | 5 |
| 2 | Press de Banca | 5 | 5 |
| 3 | Peso Muerto | 5 | 5 |
| 4 | Press Militar | 5 | 5 |
| 5 | Dominadas | 5 | 5 |
| 6 | Remo con Barra | 5 | 5 |

#### PRESET02 — Resistencia (3×20, circuito cardio)

| Orden | Ejercicio | Series | Reps |
|---|---|---|---|
| 1 | Burpees | 3 | 20 |
| 2 | Saltos de Cuerda | 3 | 20 |
| 3 | Mountain Climbers | 3 | 20 |
| 4 | Jumping Jacks | 3 | 20 |
| 5 | Zancadas | 3 | 20 |
| 6 | Plancha | 3 | 45 |

#### PRESET03 — Flexibilidad (3×15–30, movilidad y core)

| Orden | Ejercicio | Series | Reps |
|---|---|---|---|
| 1 | Plancha | 3 | 30 |
| 2 | Hiperextensiones | 3 | 15 |
| 3 | Curl Femoral Tumbado | 3 | 15 |
| 4 | Russian Twist | 3 | 20 |
| 5 | Bicicleta Abdominal | 3 | 20 |
| 6 | Elevación de Piernas | 3 | 15 |

#### PRESET04 — Hipertrofia Funcional (4×10–12, volumen)

| Orden | Ejercicio | Series | Reps |
|---|---|---|---|
| 1 | Press de Banca | 4 | 10 |
| 2 | Sentadilla | 4 | 10 |
| 3 | Dominadas | 4 | 10 |
| 4 | Remo con Mancuerna | 4 | 10 |
| 5 | Arnold Press | 4 | 10 |
| 6 | Curl de Bíceps con Barra | 4 | 10 |
| 7 | Fondos en Paralelas | 4 | 10 |
| 8 | Elevaciones Laterales | 4 | 12 |

---

## Observaciones y Problemas Detectados

| # | Severidad | Descripción |
|---|---|---|
| 1 | ✅ Corregido | `AsignacionEntity` y `AsignacionDao` ya están registrados en `AppDatabase` (v14). Existen migraciones `9 -> 10`, `10 -> 11`, `11 -> 12`, `12 -> 13` y `13 -> 14` (`imageUrl` en `ejercicios`). |
| 2 | 🟡 Media | `fallbackToDestructiveMigration()` borra todos los datos en cada cambio de versión. Se recomienda implementar migraciones explícitas antes de lanzamiento a producción. |
| 3 | 🟡 Media | `exportSchema = false` impide generar archivos de esquema para auditoría y pruebas. Considerar cambiar a `true` y agregar la ruta de exportación en `build.gradle.kts`. |
| 4 | 🟠 Media | Los usuarios de prueba (`test@test.com`, `alumno@test.com`) se semillan en `onOpen`. Deben eliminarse o protegerse antes de producción. |
| 5 | 🔵 Info | `idCreador = 0` en `rutinas` es una convención implícita para identificar presets del sistema. No está reforzado por una restricción de base de datos. |
| 6 | 🔵 Info | `colorHex` e `icono` en `rutinas` y `ejercicios` se persisten como strings. Los íconos usan keys legibles (`FITNESS_CENTER`, `DIRECTIONS_RUN`, etc.) mapeadas en `IconoHelper.kt`. Al clonar una rutina preset, `colorHex` e `icono` se copian automáticamente. |
| 7 | 🔵 Info | `sesiones_rutina` y `registros_series` no usan FK declaradas (`@ForeignKey`) en Room — las relaciones son lógicas. Considerar añadir FK con `onDelete = CASCADE` para consistencia si se necesita limpieza automática al borrar rutinas/ejercicios. |
| 8 | ✅ Corregido | `SeguimientoRepository.finalizarSesion` ahora busca la sesión directamente por PK usando `getSesionById` y notifica a `PlanRepository` para actualizar `sesiones_programadas`. |
