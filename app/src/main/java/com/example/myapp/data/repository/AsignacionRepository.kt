package com.example.myapp.data.repository

import androidx.room.withTransaction
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.AsignacionEntity
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import com.example.myapp.data.remote.sync.SyncPushItemDto
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow

/**
 * Gestiona la relación explícita usuario -> usuario para asignaciones de planes.
 *
 * Reglas de negocio aplicadas:
 *  - El asignador debe existir y tener rol ENTRENADOR.
 *  - No se permite auto-asignación.
 */
class AsignacionRepository(private val database: AppDatabase) {

    private val asignacionDao = database.asignacionDao()
    private val usuarioDao = database.usuarioDao()

    suspend fun crearAsignacion(idOrigen: String, idDestino: String): Result<Unit> {
        return try {
            database.withTransaction {
                require(idOrigen != idDestino) { "No se permite auto-asignacion." }

                val usuarioOrigen = usuarioDao.getUserById(idOrigen)
                    ?: throw IllegalArgumentException("El usuario asignador no existe.")
                if (usuarioOrigen.rol != "ENTRENADOR") {
                    throw IllegalArgumentException("Solo un usuario con rol ENTRENADOR puede asignar alumnos.")
                }

                val usuarioDestino = usuarioDao.getUserById(idDestino)
                    ?: throw IllegalArgumentException("El usuario asignado no existe.")
                if (usuarioDestino.rol != "ALUMNO") {
                    throw IllegalArgumentException("Solo se pueden asignar usuarios con rol ALUMNO.")
                }
                if (!usuarioDestino.activo) {
                    throw IllegalArgumentException("No se puede asignar un usuario inactivo.")
                }

                val draft = AsignacionEntity(
                    idUsuarioOrigen = idOrigen,
                    idUsuarioDestino = idDestino,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "PENDING",
                    deletedAt = null
                )
                
                val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
                    items = listOf(draft.toPushDto()),
                    preferredUserId = idOrigen
                )
                val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"
                
                asignacionDao.insert(draft.copy(syncStatus = finalStatus))
                if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarAsignacion(idOrigen: String, idDestino: String): Result<Unit> {
        return try {
            val existente = asignacionDao.getAsignacion(idOrigen, idDestino)
                ?: return Result.success(Unit)
            asignacionDao.softDelete(
                idUsuarioOrigen = existente.idUsuarioOrigen,
                idUsuarioDestino = existente.idUsuarioDestino,
                deletedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            SyncRuntimeDispatcher.requestSyncNow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDestinosByOrigen(idOrigen: String): Flow<List<String>> =
        asignacionDao.getDestinosByOrigen(idOrigen)

    suspend fun getOrigenesByDestino(idDestino: String): List<String> =
        asignacionDao.getOrigenesByDestino(idDestino)

    private fun AsignacionEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idUsuarioOrigen", idUsuarioOrigen)
            addProperty("idUsuarioDestino", idUsuarioDestino)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "asignaciones",
            id = "$idUsuarioOrigen::$idUsuarioDestino",
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }
}
