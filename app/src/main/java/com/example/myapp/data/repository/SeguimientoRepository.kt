package com.example.myapp.data.repository

import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.RegistroSerieEntity
import com.example.myapp.data.local.entities.SesionRutinaEntity
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import com.example.myapp.data.remote.sync.SyncPushItemDto
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow

/**
 * @param planRepository Opcional — si se proporciona, [finalizarSesion] notificará
 * automáticamente al calendario marcando la [SesionProgramadaEntity] vinculada como completada.
 */
class SeguimientoRepository(
    db: AppDatabase,
    private val planRepository: PlanRepository? = null
) {

    private val sesionDao = db.sesionRutinaDao()
    private val registroDao = db.registroSerieDao()

    // ─── Sesiones ────────────────────────────────────────────────────────────

    /**
     * Devuelve la sesión activa (en curso) para la rutina+usuario dados, o crea una nueva
     * si no existe ninguna. Retorna el id de la sesión.
     */
    suspend fun crearOReanudarSesion(idRutina: String, idUsuario: String): String {
        val activa = sesionDao.getSesionActiva(idRutina, idUsuario)
        if (activa != null) return activa.id
        val nueva = SesionRutinaEntity(
            idRutina = idRutina,
            idUsuario = idUsuario,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING",
            deletedAt = null
        )
        
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(nueva.toPushDto()),
            preferredUserId = idUsuario
        )
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"
        
        sesionDao.insertSesion(nueva.copy(syncStatus = finalStatus))
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        return nueva.id
    }

    /**
     * Marca la sesión como completada y guarda la fecha de fin.
     *
     * Fix bug #8: ahora busca la sesión por [idSesion] directamente en lugar de
     * buscar la sesión "activa" por (idRutina, idUsuario), evitando ambigüedades
     * si hubiera más de una sesión en curso.
     *
     * Si existe un [PlanRepository] inyectado, marca también la
     * [SesionProgramadaEntity] vinculada como completada.
     */
    suspend fun finalizarSesion(idSesion: String) {
        val sesion = sesionDao.getSesionById(idSesion) ?: return
        sesionDao.updateSesion(
            sesion.copy(
                completada = 1,
                fechaFin = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING"
            )
        )
        // Notificar al calendario si está disponible
        planRepository?.completarSesionProgramada(idSesion)
        SyncRuntimeDispatcher.requestSyncNow()
    }

    /** Vincula una sesión real a una sesión programada del plan (si existe calendario). */
    suspend fun linkSesionProgramada(idSesionProgramada: String, idSesion: String) {
        if (idSesionProgramada.isBlank()) return
        planRepository?.linkSesion(idSesionProgramada, idSesion)
    }

    fun getSesionesByRutina(idRutina: String, idUsuario: String): Flow<List<SesionRutinaEntity>> =
        sesionDao.getSesionesByRutina(idRutina, idUsuario)

    fun countSesionesCompletadas(idRutina: String, idUsuario: String): Flow<Int> =
        sesionDao.countSesionesCompletadas(idRutina, idUsuario)

    // ─── Registros de series ──────────────────────────────────────────────────

    /** Inserta o reemplaza el registro de una serie. */
    suspend fun logSerie(
        idSesion: String,
        idEjercicio: String,
        numeroSerie: Int,
        pesoKg: Float,
        repsRealizadas: Int
    ) {
        registroDao.insertRegistro(
        RegistroSerieEntity(
            idSesion = idSesion,
            idEjercicio = idEjercicio,
            numeroSerie = numeroSerie,
            pesoKg = pesoKg,
            repsRealizadas = repsRealizadas,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING",
            deletedAt = null
        )
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    /** Elimina el registro de una serie (desmarcar check). */
    suspend fun deleteSerie(idSesion: String, idEjercicio: String, numeroSerie: Int) =
        registroDao.softDeleteByNaturalKey(
            idSesion = idSesion,
            idEjercicio = idEjercicio,
            numeroSerie = numeroSerie,
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ).also {
            SyncRuntimeDispatcher.requestSyncNow()
        }

    fun getRegistrosBySesion(idSesion: String): Flow<List<RegistroSerieEntity>> =
        registroDao.getRegistrosBySesion(idSesion)

    fun countSeriesCompletadas(idSesion: String): Flow<Int> =
        registroDao.countSeriesCompletadas(idSesion)

    private fun SesionRutinaEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idRutina", idRutina)
            addProperty("idUsuario", idUsuario)
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
}
