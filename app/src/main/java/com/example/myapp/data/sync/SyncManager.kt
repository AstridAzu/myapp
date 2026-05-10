package com.example.myapp.data.sync

import android.util.Log
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.CertificacionEntity
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.data.local.entities.EspecialidadEntity
import com.example.myapp.data.local.entities.AsignacionEntity
import com.example.myapp.data.local.entities.ObjetivoEntity
import com.example.myapp.data.local.entities.NotificacionEntity
import com.example.myapp.data.local.entities.PlanAsignacionEntity
import com.example.myapp.data.local.entities.PlanDiaEntity
import com.example.myapp.data.local.entities.PlanDiaFechaEntity
import com.example.myapp.data.local.entities.PlanSemanaEntity
import com.example.myapp.data.local.entities.RegistroSerieEntity
import com.example.myapp.data.local.entities.RutinaAccesoEntity
import com.example.myapp.data.local.entities.RutinaEjercicioEntity
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.local.entities.SesionRutinaEntity
import com.example.myapp.data.local.entities.SesionProgramadaEntity
import com.example.myapp.data.local.entities.SyncCursorEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.remote.sync.SyncPullItemDto
import com.example.myapp.data.remote.sync.SyncApi
import com.example.myapp.data.remote.sync.SyncPushItemDto
import com.example.myapp.data.remote.sync.SyncPushRequestDto
import com.google.gson.JsonObject
import androidx.room.withTransaction
import com.example.myapp.utils.SessionManager

class SyncManager(
    private val database: AppDatabase,
    private val syncApi: SyncApi,
    private val sessionManager: SessionManager? = null,
    private val remoteIdStrategy: LegacyRemoteIdStrategy = LegacyRemoteIdStrategy.STRICT
) {
    companion object {
        private const val TAG = "SyncManager"
        private val DEFAULT_ENTITIES = listOf(
            "usuarios",
            "especialidades",
            "certificaciones",
            "objetivos",
            "ejercicios",
            "rutinas",
            "planes_semana",
            "plan_dias",
            "plan_dias_fecha",
            "sesiones_programadas",
            "notificaciones",
            "asignaciones",
            "plan_asignaciones",
            "rutina_ejercicios",
            "rutina_accesos",
            "sesiones_rutina",
            "registros_series"
        )
        private const val PAGE_SIZE = 200
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_SYNCED = "SYNCED"
        private const val STATUS_DELETED = "DELETED"
    }

    suspend fun syncAll(): Result<Unit> {
        return runCatching {
            bootstrapCursors()
            pushPendings()
            pullIncrementalAllEntities()

            // Log.i(TAG, "Sync cycle complete") // Eliminado
        }
    }

    private suspend fun bootstrapCursors() {
        DEFAULT_ENTITIES.forEach { entityType ->
            val cursor = database.syncCursorDao().getByEntityType(entityType)
            if (cursor == null) {
                database.syncCursorDao().upsert(
                    SyncCursorEntity(
                        entityType = entityType,
                        since = 0L
                    )
                )
            }
        }
    }

    private suspend fun pushPendingsEjercicios() {
        val ejercicioDao = database.ejercicioDao()
        val pending = ejercicioDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(
            items = pending.map { it.toPushDto() }
        )
        val response = syncApi.pushChanges(request)

        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { ejercicio ->
            if (accepted.contains(ejercicio.id.toString())) {
                ejercicioDao.markSyncState(
                    id = ejercicio.id,
                    syncStatus = STATUS_SYNCED,
                    updatedAt = now
                )
            }
        }
    }

    private suspend fun pushPendingsUsuarios() {
        val usuarioDao = database.usuarioDao()
        val pending = usuarioDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { usuario ->
            if (accepted.contains(usuario.id)) {
                usuarioDao.markSyncState(usuario.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsEspecialidades() {
        val especialidadDao = database.especialidadDao()
        val pending = especialidadDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { especialidad ->
            if (accepted.contains(especialidad.id)) {
                especialidadDao.markSyncState(especialidad.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsCertificaciones() {
        val certificacionDao = database.certificacionDao()
        val pending = certificacionDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { certificacion ->
            if (accepted.contains(certificacion.id)) {
                certificacionDao.markSyncState(certificacion.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsObjetivos() {
        val objetivoDao = database.objetivoDao()
        val pending = objetivoDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { objetivo ->
            if (accepted.contains(objetivo.id)) {
                objetivoDao.markSyncState(objetivo.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsRutinas() {
        val rutinaDao = database.rutinaDao()
        val pending = rutinaDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { rutina ->
            if (accepted.contains(rutina.id.toString())) {
                rutinaDao.markSyncState(rutina.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsPlanesSemana() {
        val planDao = database.planSemanaDao()
        val pending = planDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { plan ->
            if (accepted.contains(plan.id.toString())) {
                planDao.markSyncState(plan.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsPlanDias() {
        val planDiaDao = database.planDiaDao()
        val pending = planDiaDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            if (accepted.contains(item.id)) {
                planDiaDao.markSyncState(item.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsPlanDiasFecha() {
        val planDiaFechaDao = database.planDiaFechaDao()
        val pending = planDiaFechaDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            if (accepted.contains(item.id)) {
                planDiaFechaDao.markSyncState(item.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsSesionesProgramadas() {
        val sesionProgramadaDao = database.sesionProgramadaDao()
        val pending = sesionProgramadaDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            if (accepted.contains(item.id)) {
                sesionProgramadaDao.markSyncState(item.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsNotificaciones() {
        val notificacionDao = database.notificacionDao()
        val pending = notificacionDao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            if (accepted.contains(item.id)) {
                notificacionDao.markSyncState(item.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsAsignaciones() {
        val dao = database.asignacionDao()
        val pending = dao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            val remoteId = compositeId(item.idUsuarioOrigen, item.idUsuarioDestino)
            if (accepted.contains(remoteId)) {
                dao.markSyncState(item.idUsuarioOrigen, item.idUsuarioDestino, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsPlanAsignaciones() {
        val dao = database.planAsignacionDao()
        val pending = dao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            if (accepted.contains(item.id)) {
                dao.markSyncState(item.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsRutinaEjercicios() {
        val dao = database.rutinaDao()
        val pending = dao.getRutinaEjerciciosBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        // Log.d(TAG, "pushPendingsRutinaEjercicios: pending=${pending.size}") // Eliminado

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        val rejected = response.rejectedIds?.toSet() ?: emptySet()
        if (rejected.isNotEmpty()) {
            Log.w(TAG, "pushPendingsRutinaEjercicios: rejectedIds=$rejected")
        }
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        var syncedCount = 0
        pending.forEach { item ->
            val remoteId = compositeId(item.idRutina, item.idEjercicio)
            if (accepted.contains(remoteId)) {
                dao.markRutinaEjercicioSyncState(item.idRutina, item.idEjercicio, STATUS_SYNCED, now)
                syncedCount++
            }
        }
        // Log.d(TAG, "pushPendingsRutinaEjercicios: synced=$syncedCount accepted=${accepted.size}") // Eliminado
    }

    private suspend fun pushPendingsRutinaAccesos() {
        val dao = database.rutinaAccesoDao()
        val pending = dao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            val remoteId = compositeId(item.idRutina, item.idUsuario)
            if (accepted.contains(remoteId)) {
                dao.markSyncState(item.idRutina, item.idUsuario, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsSesionesRutina() {
        val dao = database.sesionRutinaDao()
        val pending = dao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            if (accepted.contains(item.id)) {
                dao.markSyncState(item.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pushPendingsRegistrosSeries() {
        val dao = database.registroSerieDao()
        val pending = dao.getBySyncStatus(STATUS_PENDING, PAGE_SIZE)
        if (pending.isEmpty()) return

        val request = SyncPushRequestDto(items = pending.map { it.toPushDto() })
        val response = syncApi.pushChanges(request)
        val accepted = response.acceptedIds?.toSet() ?: emptySet()
        if (accepted.isEmpty()) return

        val now = System.currentTimeMillis()
        pending.forEach { item ->
            if (accepted.contains(item.id)) {
                dao.markSyncState(item.id, STATUS_SYNCED, now)
            }
        }
    }

    private suspend fun pullIncrementalAllEntities() {
        DEFAULT_ENTITIES.forEach { entityType ->
            var since = database.syncCursorDao().getByEntityType(entityType)?.since ?: 0L
            var keepPaging = true

            while (keepPaging) {
                val response = syncApi.pullChanges(entityType = entityType, since = since, limit = PAGE_SIZE)
                applyPulledItems(entityType, response.items)

                database.syncCursorDao().upsert(
                    SyncCursorEntity(
                        entityType = entityType,
                        since = response.nextSince,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                keepPaging = response.items.size >= PAGE_SIZE && response.nextSince > since
                since = response.nextSince
            }
        }
    }

    private suspend fun applyPulledItems(entityType: String, items: List<SyncPullItemDto>) {
        if (items.isEmpty()) return
        when (entityType) {
            "usuarios" -> applyUsuariosPulled(items)
            "especialidades" -> applyEspecialidadesPulled(items)
            "certificaciones" -> applyCertificacionesPulled(items)
            "objetivos" -> applyObjetivosPulled(items)
            "ejercicios" -> applyEjerciciosPulled(items)
            "rutinas" -> applyRutinasPulled(items)
            "planes_semana" -> applyPlanesSemanaPulled(items)
            "plan_dias" -> applyPlanDiasPulled(items)
            "plan_dias_fecha" -> applyPlanDiasFechaPulled(items)
            "sesiones_programadas" -> applySesionesProgramadasPulled(items)
            "notificaciones" -> applyNotificacionesPulled(items)
            "asignaciones" -> applyAsignacionesPulled(items)
            "plan_asignaciones" -> applyPlanAsignacionesPulled(items)
            "rutina_ejercicios" -> applyRutinaEjerciciosPulled(items)
            "rutina_accesos" -> applyRutinaAccesosPulled(items)
            "sesiones_rutina" -> applySesionesRutinaPulled(items)
            "registros_series" -> applyRegistrosSeriesPulled(items)
            else -> {
                throw NonRetryableSyncException("Unsupported entityType in pull: '$entityType'")
            }
        }
    }

    private suspend fun applyUsuariosPulled(items: List<SyncPullItemDto>) {
        val usuarioDao = database.usuarioDao()
        database.withTransaction {
            val toUpsert = mutableListOf<UsuarioEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    usuarioDao.softDelete(
                        id = id,
                        deletedAt = item.deletedAt ?: item.updatedAt,
                        updatedAt = item.updatedAt
                    )
                    return@forEach
                }

                val payload = item.payload
                val nombre = payload.requireString("nombre", "usuarios", item.id)
                val rol = payload.requireString("rol", "usuarios", item.id)
                var email=payload.requireString("email", "usuarios", item.id)
                toUpsert += UsuarioEntity(
                    id = id,
                    nombre = nombre,
                    email = email,
                    rol = rol,
                    activo = payload.booleanOrDefault("activo", true),
                    fechaRegistro = payload.longOrNull("fechaRegistro") ?: item.updatedAt,
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                usuarioDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyEspecialidadesPulled(items: List<SyncPullItemDto>) {
        val especialidadDao = database.especialidadDao()
        database.withTransaction {
            val toUpsert = mutableListOf<EspecialidadEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    especialidadDao.softDelete(
                        id = id,
                        deletedAt = item.deletedAt ?: item.updatedAt,
                        updatedAt = item.updatedAt
                    )
                    return@forEach
                }

                val payload = item.payload
                toUpsert += EspecialidadEntity(
                    id = id,
                    idUsuario = payload.requireString("idUsuario", "especialidades", item.id),
                    nombre = payload.requireString("nombre", "especialidades", item.id),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                especialidadDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyCertificacionesPulled(items: List<SyncPullItemDto>) {
        val certificacionDao = database.certificacionDao()
        database.withTransaction {
            val toUpsert = mutableListOf<CertificacionEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    certificacionDao.softDelete(
                        id = id,
                        deletedAt = item.deletedAt ?: item.updatedAt,
                        updatedAt = item.updatedAt
                    )
                    return@forEach
                }

                val payload = item.payload
                toUpsert += CertificacionEntity(
                    id = id,
                    idUsuario = payload.requireString("idUsuario", "certificaciones", item.id),
                    nombre = payload.requireString("nombre", "certificaciones", item.id),
                    institucion = payload.requireString("institucion", "certificaciones", item.id),
                    fechaObtencion = payload.requireLong("fechaObtencion", "certificaciones", item.id),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                certificacionDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyObjetivosPulled(items: List<SyncPullItemDto>) {
        val objetivoDao = database.objetivoDao()
        database.withTransaction {
            val toUpsert = mutableListOf<ObjetivoEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    objetivoDao.softDelete(
                        id = id,
                        deletedAt = item.deletedAt ?: item.updatedAt,
                        updatedAt = item.updatedAt
                    )
                    return@forEach
                }

                val payload = item.payload
                toUpsert += ObjetivoEntity(
                    id = id,
                    idUsuario = payload.requireString("idUsuario", "objetivos", item.id),
                    descripcion = payload.requireString("descripcion", "objetivos", item.id),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                objetivoDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyEjerciciosPulled(items: List<SyncPullItemDto>) {
        val ejercicioDao = database.ejercicioDao()
        val currentUserId = sessionManager?.getUserIdString()?.trim().orEmpty()
        
        database.withTransaction {
            val toUpsert = mutableListOf<EjercicioEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    ejercicioDao.softDelete(
                        id = id,
                        deletedAt = item.deletedAt ?: item.updatedAt,
                        updatedAt = item.updatedAt
                    )
                    return@forEach
                }

                val payload = item.payload
                val nombre = payload.requireString("nombre", "ejercicios", item.id)
                val grupo = payload.requireString("grupoMuscular", "ejercicios", item.id)
                val idCreador = payload.stringOrNull("idCreador")

                // VALIDACIÓN DEFENSIVA: Rechazar si idCreador no coincide con usuario actual
                // Excepto si idCreador es NULL (ejercicio base) o usuario está vacío
                if (idCreador != null && currentUserId.isNotEmpty() && idCreador != currentUserId) {
                    Log.w(
                        TAG, 
                        "Descartando ejercicio ${item.id} ($nombre): idCreador=$idCreador no coincide con usuario=$currentUserId"
                    )
                    return@forEach
                }

                toUpsert += EjercicioEntity(
                    id = id,
                    nombre = nombre,
                    grupoMuscular = grupo,
                    descripcion = payload.stringOrNull("descripcion"),
                    idCreador = idCreador,
                    imageUrl = payload.stringOrNull("imageUrl"),
                    colorHex = payload.stringOrNull("colorHex"),
                    icono = payload.stringOrNull("icono"),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                ejercicioDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyRutinasPulled(items: List<SyncPullItemDto>) {
        val rutinaDao = database.rutinaDao()
        database.withTransaction {
            val toUpsert = mutableListOf<RutinaEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    rutinaDao.softDelete(
                        id = id,
                        deletedAt = item.deletedAt ?: item.updatedAt,
                        updatedAt = item.updatedAt
                    )
                    return@forEach
                }

                val p = item.payload
                val idCreador = p.requireString("idCreador", "rutinas", item.id)
                val nombre = p.requireString("nombre", "rutinas", item.id)
                val codigo = p.requireString("codigo", "rutinas", item.id)

                toUpsert += RutinaEntity(
                    id = id,
                    idCreador = idCreador,
                    nombre = nombre,
                    descripcion = p.stringOrNull("descripcion"),
                    fechaCreacion = p.longOrNull("fechaCreacion") ?: item.updatedAt,
                    activa = p.booleanOrDefault("activa", true),
                    codigo = codigo,
                    colorHex = p.stringOrNull("colorHex"),
                    icono = p.stringOrNull("icono"),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                rutinaDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyPlanesSemanaPulled(items: List<SyncPullItemDto>) {
        val planDao = database.planSemanaDao()
        database.withTransaction {
            val toUpsert = mutableListOf<PlanSemanaEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    planDao.softDelete(
                        id = id,
                        deletedAt = item.deletedAt ?: item.updatedAt,
                        updatedAt = item.updatedAt
                    )
                    return@forEach
                }

                val p = item.payload
                val idCreador = p.requireString("idCreador", "planes_semana", item.id)
                val idUsuario = p.requireString("idUsuario", "planes_semana", item.id)
                val nombre = p.requireString("nombre", "planes_semana", item.id)
                val fechaInicio = p.requireLong("fechaInicio", "planes_semana", item.id)
                val fechaFin = p.requireLong("fechaFin", "planes_semana", item.id)

                toUpsert += PlanSemanaEntity(
                    id = id,
                    idCreador = idCreador,
                    idUsuario = idUsuario,
                    nombre = nombre,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin,
                    activo = p.booleanOrDefault("activo", true),
                    fechaCreacion = p.longOrNull("fechaCreacion") ?: item.updatedAt,
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                planDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyPlanDiasPulled(items: List<SyncPullItemDto>) {
        val planDiaDao = database.planDiaDao()
        database.withTransaction {
            val toUpsert = mutableListOf<PlanDiaEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    planDiaDao.softDelete(id, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                toUpsert += PlanDiaEntity(
                    id = id,
                    idPlan = p.requireString("idPlan", "plan_dias", item.id),
                    diaSemana = p.requireLong("diaSemana", "plan_dias", item.id).toInt(),
                    tipo = p.requireString("tipo", "plan_dias", item.id),
                    idRutina = p.stringOrNull("idRutina"),
                    orden = p.longOrNull("orden")?.toInt() ?: 1,
                    notas = p.stringOrNull("notas"),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                planDiaDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyPlanDiasFechaPulled(items: List<SyncPullItemDto>) {
        val planDiaFechaDao = database.planDiaFechaDao()
        database.withTransaction {
            val toUpsert = mutableListOf<PlanDiaFechaEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    planDiaFechaDao.softDelete(id, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                toUpsert += PlanDiaFechaEntity(
                    id = id,
                    idPlan = p.requireString("idPlan", "plan_dias_fecha", item.id),
                    fecha = p.requireLong("fecha", "plan_dias_fecha", item.id),
                    diaSemana = p.requireLong("diaSemana", "plan_dias_fecha", item.id).toInt(),
                    tipo = p.requireString("tipo", "plan_dias_fecha", item.id),
                    idRutina = p.stringOrNull("idRutina"),
                    orden = p.longOrNull("orden")?.toInt() ?: 1,
                    notas = p.stringOrNull("notas"),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                planDiaFechaDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applySesionesProgramadasPulled(items: List<SyncPullItemDto>) {
        val sesionProgramadaDao = database.sesionProgramadaDao()
        database.withTransaction {
            val toUpsert = mutableListOf<SesionProgramadaEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    sesionProgramadaDao.softDelete(id, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                toUpsert += SesionProgramadaEntity(
                    id = id,
                    idPlanDia = p.requireString("idPlanDia", "sesiones_programadas", item.id),
                    fechaProgramada = p.requireLong("fechaProgramada", "sesiones_programadas", item.id),
                    idSesion = p.stringOrNull("idSesion"),
                    completada = p.longOrNull("completada")?.toInt() ?: 0,
                    omitida = p.longOrNull("omitida")?.toInt() ?: 0,
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                sesionProgramadaDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyNotificacionesPulled(items: List<SyncPullItemDto>) {
        val notificacionDao = database.notificacionDao()
        database.withTransaction {
            val toUpsert = mutableListOf<NotificacionEntity>()
            items.forEach { item ->
                val id = item.id

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    notificacionDao.softDelete(id, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                toUpsert += NotificacionEntity(
                    id = id,
                    idUsuario = p.requireString("idUsuario", "notificaciones", item.id),
                    idSesionProgramada = p.stringOrNull("idSesionProgramada"),
                    tipo = p.requireString("tipo", "notificaciones", item.id),
                    titulo = p.requireString("titulo", "notificaciones", item.id),
                    mensaje = p.requireString("mensaje", "notificaciones", item.id),
                    fechaProgramada = p.requireLong("fechaProgramada", "notificaciones", item.id),
                    fechaEntrega = p.longOrNull("fechaEntrega"),
                    estado = p.stringOrNull("estado") ?: "PENDIENTE",
                    intentos = p.longOrNull("intentos")?.toInt() ?: 0,
                    errorCodigo = p.stringOrNull("errorCodigo"),
                    activo = p.booleanOrDefault("activo", true),
                    fechaCreacion = p.longOrNull("fechaCreacion") ?: item.updatedAt,
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }

            if (toUpsert.isNotEmpty()) {
                notificacionDao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyAsignacionesPulled(items: List<SyncPullItemDto>) {
        val dao = database.asignacionDao()
        database.withTransaction {
            val toUpsert = mutableListOf<AsignacionEntity>()
            items.forEach { item ->
                val (idUsuarioOrigen, idUsuarioDestino) = splitCompositeId(item.id, "asignaciones")

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    dao.softDelete(idUsuarioOrigen, idUsuarioDestino, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                toUpsert += AsignacionEntity(
                    idUsuarioOrigen = p.stringOrNull("idUsuarioOrigen") ?: idUsuarioOrigen,
                    idUsuarioDestino = p.stringOrNull("idUsuarioDestino") ?: idUsuarioDestino,
                    fechaAsignacion = p.longOrNull("fechaAsignacion") ?: item.updatedAt,
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }
            if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)
        }
    }

    private suspend fun applyPlanAsignacionesPulled(items: List<SyncPullItemDto>) {
        val dao = database.planAsignacionDao()
        database.withTransaction {
            val toUpsert = mutableListOf<PlanAsignacionEntity>()
            items.forEach { item ->
                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    dao.softDelete(item.id, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                toUpsert += PlanAsignacionEntity(
                    id = item.id,
                    idPlan = p.requireString("idPlan", "plan_asignaciones", item.id),
                    idUsuarioAsignador = p.requireString("idUsuarioAsignador", "plan_asignaciones", item.id),
                    idUsuarioAsignado = p.requireString("idUsuarioAsignado", "plan_asignaciones", item.id),
                    activa = p.booleanOrDefault("activa", true),
                    fechaAsignacion = p.longOrNull("fechaAsignacion") ?: item.updatedAt,
                    fechaCancelacion = p.longOrNull("fechaCancelacion"),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }
            if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)
        }
    }

    private suspend fun applyRutinaEjerciciosPulled(items: List<SyncPullItemDto>) {
        val dao = database.rutinaDao()
        val ejercicioDao = database.ejercicioDao()
        database.withTransaction {
            val toUpsert = mutableListOf<RutinaEjercicioEntity>()
            items.forEach { item ->
                val (idRutina, idEjercicio) = splitCompositeId(item.id, "rutina_ejercicios")

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    dao.softDeleteRutinaEjercicio(idRutina, idEjercicio, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                toUpsert += RutinaEjercicioEntity(
                    idRutina = p.stringOrNull("idRutina") ?: idRutina,
                    idEjercicio = p.stringOrNull("idEjercicio") ?: idEjercicio,
                    series = p.requireLong("series", "rutina_ejercicios", item.id).toInt(),
                    reps = p.requireLong("reps", "rutina_ejercicios", item.id).toInt(),
                    orden = p.requireLong("orden", "rutina_ejercicios", item.id).toInt(),
                    notas = p.stringOrNull("notas"),
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }
            if (toUpsert.isNotEmpty()) {
                val linkedExerciseIds = toUpsert.map { it.idEjercicio }.distinct()
                val existingIds = ejercicioDao.getExistingIds(linkedExerciseIds).toSet()
                val missingIds = linkedExerciseIds.filterNot { existingIds.contains(it) }
                if (missingIds.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val placeholders = missingIds.map { missingId ->
                        EjercicioEntity(
                            id = missingId,
                            nombre = "Ejercicio recuperado",
                            grupoMuscular = "General",
                            descripcion = "Placeholder generado durante pull para vínculo legacy.",
                            updatedAt = now,
                            syncStatus = STATUS_SYNCED,
                            deletedAt = null
                        )
                    }
                    ejercicioDao.upsertAll(placeholders)
                }
                dao.upsertRutinaEjercicios(toUpsert)
            }
        }
    }

    private suspend fun applyRutinaAccesosPulled(items: List<SyncPullItemDto>) {
        val dao = database.rutinaAccesoDao()
        val rutinaDao = database.rutinaDao()
        val usuarioDao = database.usuarioDao()
        
        database.withTransaction {
            val toUpsert = mutableListOf<RutinaAccesoEntity>()
            val droppedCount = mutableMapOf(
                "orphaned_routine" to 0,
                "orphaned_user" to 0
            )
            
            items.forEach { item ->
                val (idRutina, idUsuario) = splitCompositeId(item.id, "rutina_accesos")

                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    dao.softDelete(idRutina, idUsuario, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                val finalIdRutina = p.stringOrNull("idRutina") ?: idRutina
                val finalIdUsuario = p.stringOrNull("idUsuario") ?: idUsuario

                // Validate FKs exist before creating access
                val rutinaExists = rutinaDao.getExistingRutinaIds(listOf(finalIdRutina)).isNotEmpty()
                val usuarioExists = usuarioDao.getExistingIds(listOf(finalIdUsuario)).isNotEmpty()
                
                if (!rutinaExists) {
                    Log.w(TAG, "applyRutinaAccesosPulled: dropped access ${item.id} - routine not found: $finalIdRutina")
                    droppedCount["orphaned_routine"] = droppedCount["orphaned_routine"]!! + 1
                    return@forEach
                }
                
                if (!usuarioExists) {
                    Log.w(TAG, "applyRutinaAccesosPulled: dropped access ${item.id} - user not found: $finalIdUsuario")
                    droppedCount["orphaned_user"] = droppedCount["orphaned_user"]!! + 1
                    return@forEach
                }
                
                toUpsert += RutinaAccesoEntity(
                    idRutina = finalIdRutina,
                    idUsuario = finalIdUsuario,
                    fechaAcceso = p.longOrNull("fechaAcceso") ?: item.updatedAt,
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }
            
            if (droppedCount.values.any { it > 0 }) {
                Log.i(TAG, "applyRutinaAccesosPulled summary: " +
                    "accepted=${toUpsert.size}, " +
                    "dropped_orphaned_routine=${droppedCount["orphaned_routine"]}, " +
                    "dropped_orphaned_user=${droppedCount["orphaned_user"]}")
            }
            
            if (toUpsert.isNotEmpty()) {
                dao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applySesionesRutinaPulled(items: List<SyncPullItemDto>) {
        val dao = database.sesionRutinaDao()
        val rutinaDao = database.rutinaDao()
        val usuarioDao = database.usuarioDao()
        
        database.withTransaction {
            val toUpsert = mutableListOf<SesionRutinaEntity>()
            val droppedCount = mutableMapOf(
                "orphaned_routine" to 0,
                "orphaned_user" to 0
            )
            
            items.forEach { item ->
                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    dao.softDelete(item.id, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                val idRutina = p.requireString("idRutina", "sesiones_rutina", item.id)
                val idUsuario = p.requireString("idUsuario", "sesiones_rutina", item.id)
                
                // Validate FKs exist before creating session
                val rutinaExists = rutinaDao.getExistingRutinaIds(listOf(idRutina)).isNotEmpty()
                val usuarioExists = usuarioDao.getExistingIds(listOf(idUsuario)).isNotEmpty()
                
                if (!rutinaExists) {
                    Log.w(TAG, "applySesionesRutinaPulled: dropped session ${item.id} - routine not found: $idRutina")
                    droppedCount["orphaned_routine"] = droppedCount["orphaned_routine"]!! + 1
                    return@forEach
                }
                
                if (!usuarioExists) {
                    Log.w(TAG, "applySesionesRutinaPulled: dropped session ${item.id} - user not found: $idUsuario")
                    droppedCount["orphaned_user"] = droppedCount["orphaned_user"]!! + 1
                    return@forEach
                }
                
                toUpsert += SesionRutinaEntity(
                    id = item.id,
                    idRutina = idRutina,
                    idUsuario = idUsuario,
                    fechaInicio = p.longOrNull("fechaInicio") ?: item.updatedAt,
                    fechaFin = p.longOrNull("fechaFin"),
                    completada = p.longOrNull("completada")?.toInt() ?: 0,
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }
            
            if (droppedCount.values.any { it > 0 }) {
                Log.i(TAG, "applySesionesRutinaPulled summary: " +
                    "accepted=${toUpsert.size}, " +
                    "dropped_orphaned_routine=${droppedCount["orphaned_routine"]}, " +
                    "dropped_orphaned_user=${droppedCount["orphaned_user"]}")
            }
            
            if (toUpsert.isNotEmpty()) {
                dao.upsertAll(toUpsert)
            }
        }
    }

    private suspend fun applyRegistrosSeriesPulled(items: List<SyncPullItemDto>) {
        val dao = database.registroSerieDao()
        val sesionDao = database.sesionRutinaDao()
        val ejercicioDao = database.ejercicioDao()
        
        database.withTransaction {
            val toUpsert = mutableListOf<RegistroSerieEntity>()
            val droppedCount = mutableMapOf(
                "orphaned_session" to 0,
                "orphaned_exercise" to 0
            )
            
            items.forEach { item ->
                if (item.syncStatus == STATUS_DELETED || item.deletedAt != null) {
                    dao.softDelete(item.id, item.deletedAt ?: item.updatedAt, item.updatedAt)
                    return@forEach
                }

                val p = item.payload
                val idSesion = p.requireString("idSesion", "registros_series", item.id)
                val idEjercicio = p.requireString("idEjercicio", "registros_series", item.id)
                
                // Validate FKs exist before creating registration record
                val sesionExists = sesionDao.getSesionById(idSesion) != null
                val ejercicioExists = ejercicioDao.getExistingIds(listOf(idEjercicio)).isNotEmpty()
                
                if (!sesionExists) {
                    Log.w(TAG, "applyRegistrosSeriesPulled: dropped registro ${item.id} - session not found: $idSesion")
                    droppedCount["orphaned_session"] = droppedCount["orphaned_session"]!! + 1
                    return@forEach
                }
                
                if (!ejercicioExists) {
                    Log.w(TAG, "applyRegistrosSeriesPulled: dropped registro ${item.id} - exercise not found: $idEjercicio")
                    droppedCount["orphaned_exercise"] = droppedCount["orphaned_exercise"]!! + 1
                    return@forEach
                }
                
                toUpsert += RegistroSerieEntity(
                    id = item.id,
                    idSesion = idSesion,
                    idEjercicio = idEjercicio,
                    numeroSerie = p.requireLong("numeroSerie", "registros_series", item.id).toInt(),
                    pesoKg = p.floatOrNull("pesoKg") ?: 0f,
                    repsRealizadas = p.requireLong("repsRealizadas", "registros_series", item.id).toInt(),
                    completada = p.longOrNull("completada")?.toInt() ?: 1,
                    updatedAt = item.updatedAt,
                    syncStatus = item.syncStatus.ifBlank { STATUS_SYNCED },
                    deletedAt = item.deletedAt
                )
            }
            
            if (droppedCount.values.any { it > 0 }) {
                Log.i(TAG, "applyRegistrosSeriesPulled summary: " +
                    "accepted=${toUpsert.size}, " +
                    "dropped_orphaned_session=${droppedCount["orphaned_session"]}, " +
                    "dropped_orphaned_exercise=${droppedCount["orphaned_exercise"]}")
            }
            
            if (toUpsert.isNotEmpty()) {
                dao.upsertAll(toUpsert)
            }
        }
    }

    private fun EjercicioEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("grupoMuscular", grupoMuscular)
            addProperty("descripcion", descripcion)
            addProperty("imageUrl", imageUrl)
            addProperty("colorHex", colorHex)
            addProperty("icono", icono)
            // idCreador y deletedAt SE OMITEN según SYNC_API_SPEC.md
        }
        return SyncPushItemDto(
            entityType = "ejercicios",
            id = id.toString(),
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun UsuarioEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("rol", rol)
            addProperty("activo", activo)
            addProperty("fechaRegistro", fechaRegistro)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "usuarios",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun EspecialidadEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idUsuario", idUsuario)
            addProperty("nombre", nombre)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "especialidades",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun CertificacionEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idUsuario", idUsuario)
            addProperty("nombre", nombre)
            addProperty("institucion", institucion)
            addProperty("fechaObtencion", fechaObtencion)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "certificaciones",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun ObjetivoEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idUsuario", idUsuario)
            addProperty("descripcion", descripcion)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "objetivos",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun RutinaEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idCreador", idCreador)
            addProperty("nombre", nombre)
            addProperty("descripcion", descripcion)
            addProperty("fechaCreacion", fechaCreacion)
            addProperty("activa", activa)
            addProperty("codigo", codigo)
            addProperty("colorHex", colorHex)
            addProperty("icono", icono)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "rutinas",
            id = id.toString(),
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun PlanSemanaEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idCreador", idCreador)
            addProperty("idUsuario", idUsuario)
            addProperty("nombre", nombre)
            addProperty("fechaInicio", fechaInicio)
            addProperty("fechaFin", fechaFin)
            addProperty("activo", activo)
            addProperty("fechaCreacion", fechaCreacion)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "planes_semana",
            id = id.toString(),
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun PlanDiaEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idPlan", idPlan)
            addProperty("diaSemana", diaSemana)
            addProperty("tipo", tipo)
            addProperty("idRutina", idRutina)
            addProperty("orden", orden)
            addProperty("notas", notas)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "plan_dias",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun PlanDiaFechaEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idPlan", idPlan)
            addProperty("fecha", fecha)
            addProperty("diaSemana", diaSemana)
            addProperty("tipo", tipo)
            addProperty("idRutina", idRutina)
            addProperty("orden", orden)
            addProperty("notas", notas)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "plan_dias_fecha",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun SesionProgramadaEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idPlanDia", idPlanDia)
            addProperty("fechaProgramada", fechaProgramada)
            addProperty("idSesion", idSesion)
            addProperty("completada", completada)
            addProperty("omitida", omitida)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "sesiones_programadas",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun NotificacionEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idUsuario", idUsuario)
            addProperty("idSesionProgramada", idSesionProgramada)
            addProperty("tipo", tipo)
            addProperty("titulo", titulo)
            addProperty("mensaje", mensaje)
            addProperty("fechaProgramada", fechaProgramada)
            addProperty("fechaEntrega", fechaEntrega)
            addProperty("estado", estado)
            addProperty("intentos", intentos)
            addProperty("errorCodigo", errorCodigo)
            addProperty("activo", activo)
            addProperty("fechaCreacion", fechaCreacion)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "notificaciones",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun AsignacionEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idUsuarioOrigen", idUsuarioOrigen)
            addProperty("idUsuarioDestino", idUsuarioDestino)
            addProperty("fechaAsignacion", fechaAsignacion)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "asignaciones",
            id = compositeId(idUsuarioOrigen, idUsuarioDestino),
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun PlanAsignacionEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idPlan", idPlan)
            addProperty("idUsuarioAsignador", idUsuarioAsignador)
            addProperty("idUsuarioAsignado", idUsuarioAsignado)
            addProperty("activa", activa)
            addProperty("fechaAsignacion", fechaAsignacion)
            addProperty("fechaCancelacion", fechaCancelacion)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "plan_asignaciones",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun RutinaEjercicioEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idRutina", idRutina)
            addProperty("idEjercicio", idEjercicio)
            addProperty("series", series)
            addProperty("reps", reps)
            addProperty("orden", orden)
            addProperty("notas", notas)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "rutina_ejercicios",
            id = compositeId(idRutina, idEjercicio),
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun RutinaAccesoEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idRutina", idRutina)
            addProperty("idUsuario", idUsuario)
            addProperty("fechaAcceso", fechaAcceso)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "rutina_accesos",
            id = compositeId(idRutina, idUsuario),
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun SesionRutinaEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idRutina", idRutina)
            addProperty("idUsuario", idUsuario)
            addProperty("fechaInicio", fechaInicio)
            addProperty("fechaFin", fechaFin)
            addProperty("completada", completada)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "sesiones_rutina",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun RegistroSerieEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idSesion", idSesion)
            addProperty("idEjercicio", idEjercicio)
            addProperty("numeroSerie", numeroSerie)
            addProperty("pesoKg", pesoKg)
            addProperty("repsRealizadas", repsRealizadas)
            addProperty("completada", completada)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "registros_series",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun compositeId(a: String, b: String): String = "$a::$b"

    private fun splitCompositeId(value: String, entityType: String): Pair<String, String> {
        val parts = value.split("::", limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw NonRetryableSyncException("Invalid composite id for '$entityType': '$value'")
        }
        return parts[0] to parts[1]
    }

    private suspend fun pushPendings() {
        pushPendingsUsuarios()
        pushPendingsEspecialidades()
        pushPendingsCertificaciones()
        pushPendingsObjetivos()
        pushPendingsEjercicios()
        pushPendingsRutinas()
        pushPendingsPlanesSemana()
        pushPendingsPlanDias()
        pushPendingsPlanDiasFecha()
        pushPendingsSesionesProgramadas()
        pushPendingsNotificaciones()
        pushPendingsAsignaciones()
        pushPendingsPlanAsignaciones()
        pushPendingsRutinaEjercicios()
        pushPendingsRutinaAccesos()
        pushPendingsSesionesRutina()
        pushPendingsRegistrosSeries()
    }
}
