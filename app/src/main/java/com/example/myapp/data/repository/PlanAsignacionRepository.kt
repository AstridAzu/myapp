package com.example.myapp.data.repository

import androidx.room.withTransaction
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.PlanAsignacionEntity
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import kotlinx.coroutines.flow.Flow

/**
 * Gestiona la asignacion explicita de planes a usuarios alumnos.
 *
 * Nota: entrenador y alumno son usuarios diferenciados por su campo rol.
 */
class PlanAsignacionRepository(private val database: AppDatabase) {

    private val planAsignacionDao = database.planAsignacionDao()
    private val planDao = database.planSemanaDao()
    private val usuarioDao = database.usuarioDao()

    suspend fun crearAsignacionPlan(
        idUsuarioAsignador: String,
        idUsuarioAsignado: String,
        idPlan: String
    ): Result<String> {
        return try {
            val newId = database.withTransaction {
                require(idUsuarioAsignador != idUsuarioAsignado) {
                    "No se permite auto-asignacion de plan."
                }

                val asignador = usuarioDao.getUserById(idUsuarioAsignador)
                    ?: throw IllegalArgumentException("El usuario asignador no existe.")
                if (asignador.rol != "ENTRENADOR") {
                    throw IllegalArgumentException("Solo un usuario con rol ENTRENADOR puede asignar planes.")
                }

                val asignado = usuarioDao.getUserById(idUsuarioAsignado)
                    ?: throw IllegalArgumentException("El usuario asignado no existe.")
                if (asignado.rol != "ALUMNO") {
                    throw IllegalArgumentException("Solo se pueden asignar planes a usuarios con rol ALUMNO.")
                }
                if (!asignado.activo) {
                    throw IllegalArgumentException("No se puede asignar un plan a un usuario inactivo.")
                }

                val plan = planDao.getById(idPlan)
                    ?: throw IllegalArgumentException("El plan no existe.")

                if (plan.idCreador != idUsuarioAsignador) {
                    throw IllegalArgumentException("El plan no pertenece al usuario asignador.")
                }

                val existenteActiva = planAsignacionDao.getActivaByPlanYAsignado(idPlan, idUsuarioAsignado)
                if (existenteActiva != null) return@withTransaction existenteActiva.id

                val nueva = PlanAsignacionEntity(
                    idPlan = idPlan,
                    idUsuarioAsignador = idUsuarioAsignador,
                    idUsuarioAsignado = idUsuarioAsignado,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "PENDING",
                    deletedAt = null
                )
                planAsignacionDao.insert(nueva)
                nueva.id
            }
            SyncRuntimeDispatcher.requestSyncNow()
            Result.success(newId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelarAsignacionPlan(idAsignacion: String): Result<Unit> {
        return try {
            planAsignacionDao.cancelarAsignacion(
                idAsignacion = idAsignacion,
                fechaCancelacion = System.currentTimeMillis()
            )
            SyncRuntimeDispatcher.requestSyncNow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAsignacionesByUsuarioAsignador(idUsuarioAsignador: String): Flow<List<PlanAsignacionEntity>> =
        planAsignacionDao.getByUsuarioAsignador(idUsuarioAsignador)

    fun getAsignacionesByUsuarioAsignado(idUsuarioAsignado: String): Flow<List<PlanAsignacionEntity>> =
        planAsignacionDao.getByUsuarioAsignado(idUsuarioAsignado)

    fun getAsignacionesActivasByUsuarioAsignado(idUsuarioAsignado: String): Flow<List<PlanAsignacionEntity>> =
        planAsignacionDao.getActivasByUsuarioAsignado(idUsuarioAsignado)

    fun getAsignacionesActivasByPlan(idPlan: String): Flow<List<PlanAsignacionEntity>> =
        planAsignacionDao.getActivasByPlan(idPlan)
}