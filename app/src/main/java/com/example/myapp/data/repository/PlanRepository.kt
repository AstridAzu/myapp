package com.example.myapp.data.repository

import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.dao.PlanActivoResumenRow
import com.example.myapp.data.local.dao.PlanSeguimientoRow
import com.example.myapp.data.local.dao.SesionProgramadaPlanRow
import com.example.myapp.data.local.entities.PlanDiaEntity
import com.example.myapp.data.local.entities.PlanDiaFechaEntity
import com.example.myapp.data.local.entities.PlanSemanaEntity
import com.example.myapp.data.local.entities.SesionProgramadaEntity
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import com.example.myapp.data.remote.sync.SyncPushItemDto
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repositorio del calendario semanal.
 *
 * Responsabilidades:
 *  - CRUD de [PlanSemanaEntity] y [PlanDiaEntity].
 *  - Materializar sesiones programadas ([SesionProgramadaEntity]) para una semana
 *    dada a partir de la plantilla de días del plan.
 *  - Vincular y completar sesiones programadas cuando el alumno termina un entrenamiento.
 */
class PlanRepository(db: AppDatabase) {

    private val planDao      = db.planSemanaDao()
    private val diaDao       = db.planDiaDao()
    private val diaFechaDao  = db.planDiaFechaDao()
    private val programadaDao = db.sesionProgramadaDao()

    // ─── Planes ──────────────────────────────────────────────────────────────

    /**
     * Crea un plan nuevo.
     * Si [activarInmediatamente] es true, el plan se guarda como activo.
     * Se permiten múltiples planes activos en paralelo para el mismo usuario.
     */
    suspend fun crearPlan(plan: PlanSemanaEntity, activarInmediatamente: Boolean = true): String {
        val now = System.currentTimeMillis()
        val planAInsertar = if (activarInmediatamente) {
            plan.copy(activo = true, updatedAt = now, syncStatus = "PENDING", deletedAt = null)
        } else {
            plan.copy(activo = false, updatedAt = now, syncStatus = "PENDING", deletedAt = null)
        }
        
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(planAInsertar.toPushDto()),
            preferredUserId = planAInsertar.idCreador
        )
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"
        
        planDao.insert(planAInsertar.copy(syncStatus = finalStatus))
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        return planAInsertar.id
    }

    suspend fun actualizarPlan(plan: PlanSemanaEntity) = planDao.update(
        plan.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING"
        )
    ).also {
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun eliminarPlan(id: String) =
        planDao.softDelete(id, System.currentTimeMillis(), System.currentTimeMillis()).also {
            SyncRuntimeDispatcher.requestSyncNow()
        }

    fun getPlanesActivosDeUsuario(idUsuario: String): Flow<List<PlanSemanaEntity>> =
        planDao.getPlanesActivosByUsuario(idUsuario)

    fun getPlanesActivosResumenDeUsuario(idUsuario: String): Flow<List<PlanActivoResumenRow>> =
        planDao.getPlanesActivosResumenByUsuario(idUsuario)

    fun getPlanesDeUsuario(idUsuario: String): Flow<List<PlanSemanaEntity>> =
        planDao.getPlanesDeUsuario(idUsuario)

    fun getPlanesCreados(idCreador: String): Flow<List<PlanSemanaEntity>> =
        planDao.getPlanesCreados(idCreador)

    suspend fun getPlanById(idPlan: String): PlanSemanaEntity? =
        planDao.getById(idPlan)

    suspend fun activarPlan(idPlan: String) {
        planDao.activar(idPlan)
        planDao.markSyncState(idPlan, "PENDING", System.currentTimeMillis())
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun desactivarPlan(idPlan: String) {
        planDao.desactivar(idPlan)
        planDao.markSyncState(idPlan, "PENDING", System.currentTimeMillis())
        SyncRuntimeDispatcher.requestSyncNow()
    }

    fun getSeguimientoPlanesPorCreador(idCreador: String): Flow<List<PlanSeguimientoRow>> =
        planDao.getSeguimientoPlanesPorCreador(idCreador)

    // ─── Días del plan ───────────────────────────────────────────────────────

    /** Agrega una ranura (rutina o descanso) a un día de la semana dentro de un plan. */
    suspend fun agregarDia(dia: PlanDiaEntity): String {
        val nuevo = dia.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING",
            deletedAt = null
        )
        diaDao.insert(nuevo)
        SyncRuntimeDispatcher.requestSyncNow()
        return nuevo.id
    }

    /** Reemplaza todos los días de un plan con la lista proporcionada. */
    suspend fun reemplazarDias(idPlan: String, dias: List<PlanDiaEntity>) {
        diaDao.deleteDiasByPlan(idPlan)
        val now = System.currentTimeMillis()
        diaDao.insertAll(
            dias.map {
                it.copy(
                    updatedAt = now,
                    syncStatus = "PENDING",
                    deletedAt = null
                )
            }
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun eliminarDia(id: String) = diaDao.deleteById(id).also {
        SyncRuntimeDispatcher.requestSyncNow()
    }

    fun getDiasByPlan(idPlan: String): Flow<List<PlanDiaEntity>> =
        diaDao.getDiasByPlan(idPlan)

    fun getDiasByDiaSemana(idPlan: String, diaSemana: Int): Flow<List<PlanDiaEntity>> =
        diaDao.getDiasByPlanAndDia(idPlan, diaSemana)

    suspend fun reemplazarDiasPorFecha(idPlan: String, dias: List<PlanDiaFechaEntity>) {
        diaFechaDao.deleteDiasByPlan(idPlan)
        val now = System.currentTimeMillis()
        diaFechaDao.insertAll(
            dias.map {
                it.copy(
                    updatedAt = now,
                    syncStatus = "PENDING",
                    deletedAt = null
                )
            }
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    fun getDiasPorFechaByPlan(idPlan: String): Flow<List<PlanDiaFechaEntity>> =
        diaFechaDao.getDiasByPlan(idPlan)

    suspend fun getDiasPorFechaByPlanOnce(idPlan: String): List<PlanDiaFechaEntity> =
        diaFechaDao.getDiasByPlanOnce(idPlan)

    // ─── Materialización de semana ───────────────────────────────────────────

    /**
     * Genera filas en [sesiones_programadas] para todos los días de la semana
     * que contiene [cualquierDiaEnSemana] (epoch ms) según la plantilla del plan.
     *
     * La operación es completamente idempotente: el índice UNIQUE (idPlanDia, fechaProgramada)
     * hace que los inserts duplicados sean ignorados silenciosamente (IGNORE).
     *
     * @param idPlan Plan cuya plantilla se usará.
     * @param cualquierDiaEnSemana Cualquier timestamp dentro de la semana a materializar.
     * @return Lista de ids de [SesionProgramadaEntity] generadas (0 si ya existían).
     */
    suspend fun materializarSemana(idPlan: String, cualquierDiaEnSemana: Long): List<String> {
        val diasDelPlan = diaDao.getDiasByPlanOnce(idPlan)
        if (diasDelPlan.isEmpty()) return emptyList()

        // Calcular el inicio de la semana ISO (Lunes = 1)
        val cal = Calendar.getInstance().apply {
            timeInMillis = cualquierDiaEnSemana
            // Mover al lunes de esa semana
            val dow = get(Calendar.DAY_OF_WEEK)        // 1=Dom, 2=Lun, …, 7=Sab
            val diasDesdeElLunes = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
            add(Calendar.DAY_OF_YEAR, -diasDesdeElLunes)
            // Normalizar a medianoche
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lunesMs = cal.timeInMillis
        val msPerDia = 24L * 60 * 60 * 1000

        val entidades = diasDelPlan.map { dia ->
            // diaSemana: 1=Lun … 7=Dom  →  offset desde lunes = diaSemana - 1
            val fechaProgramada = lunesMs + (dia.diaSemana - 1) * msPerDia
            SesionProgramadaEntity(
                idPlanDia = dia.id,
                fechaProgramada = fechaProgramada,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        }

        programadaDao.insertAll(entidades)
        SyncRuntimeDispatcher.requestSyncNow()
        return entidades.map { it.id }
    }

    /**
     * Materializa la semana de [referenciaMs] y las [semanasAdicionales] siguientes.
     * Útil para pintar hoy + próxima rutina sin depender de sesiones previas.
     */
    suspend fun materializarSemanas(idPlan: String, referenciaMs: Long, semanasAdicionales: Int = 1) {
        val msPorSemana = 7L * 24L * 60L * 60L * 1000L
        for (offset in 0..semanasAdicionales.coerceAtLeast(0)) {
            materializarSemana(idPlan, referenciaMs + (offset * msPorSemana))
        }
    }

    // ─── Sesiones programadas ────────────────────────────────────────────────

    fun getSesionesProgramadasEnSemana(desde: Long, hasta: Long): Flow<List<SesionProgramadaEntity>> =
        programadaDao.getByRangoFecha(desde, hasta)

    fun getSesionesByPlanDia(idPlanDia: String): Flow<List<SesionProgramadaEntity>> =
        programadaDao.getByPlanDia(idPlanDia)

    fun getSesionesConRutinaByPlanEnRango(idPlan: String, desde: Long, hasta: Long): Flow<List<SesionProgramadaPlanRow>> =
        programadaDao.getSesionesConRutinaByPlanEnRango(idPlan, desde, hasta)

    /**
     * Vincula una sesión real al registro programado e inicia su progreso.
     * Debe llamarse justo después de crear/reanudar la sesión en [SeguimientoRepository].
     */
    suspend fun linkSesion(idSesionProgramada: String, idSesion: String) {
        programadaDao.linkSesion(idSesionProgramada, idSesion)
        SyncRuntimeDispatcher.requestSyncNow()
    }

    /**
     * Marca como completada la sesión programada asociada a una sesión real.
     * Debe llamarse al finalizar la sesión en [SeguimientoRepository].
     * Si no existe sesión programada vinculada, no hace nada.
     */
    suspend fun completarSesionProgramada(idSesion: String) =
        programadaDao.getBySesionRutina(idSesion)?.let {
            programadaDao.marcarCompletada(it.id)
            SyncRuntimeDispatcher.requestSyncNow()
        }

    /** El usuario decide saltarse el día programado. */
    suspend fun omitirSesionProgramada(id: String) =
        programadaDao.marcarOmitida(id).also {
            SyncRuntimeDispatcher.requestSyncNow()
        }

    /** El usuario deshace la omisión de un día. */
    suspend fun desomitirSesionProgramada(id: String) =
        programadaDao.desmarcarOmitida(id).also {
            SyncRuntimeDispatcher.requestSyncNow()
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
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }
}
