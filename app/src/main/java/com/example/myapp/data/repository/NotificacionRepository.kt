package com.example.myapp.data.repository

import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.NotificacionEntity
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import kotlinx.coroutines.flow.Flow

class NotificacionRepository(db: AppDatabase) {

    private val notificacionDao = db.notificacionDao()

    suspend fun crear(notificacion: NotificacionEntity) {
        notificacionDao.insert(
            notificacion.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun crearLote(notificaciones: List<NotificacionEntity>) {
        val now = System.currentTimeMillis()
        notificacionDao.insertAll(
            notificaciones.map {
                it.copy(
                    updatedAt = now,
                    syncStatus = "PENDING",
                    deletedAt = null
                )
            }
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun actualizar(notificacion: NotificacionEntity) =
        notificacionDao.update(
            notificacion.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        ).also {
            SyncRuntimeDispatcher.requestSyncNow()
        }

    suspend fun obtenerPendientesHasta(hasta: Long, limite: Int = 100): List<NotificacionEntity> =
        notificacionDao.getPendientesHasta(hasta, limite)

    suspend fun marcarEnviada(id: String, fechaEntrega: Long = System.currentTimeMillis()) =
        notificacionDao.marcarEnviada(id, fechaEntrega).also {
            SyncRuntimeDispatcher.requestSyncNow()
        }

    suspend fun marcarFallida(id: String, errorCodigo: String?) =
        notificacionDao.marcarFallida(id, errorCodigo).also {
            SyncRuntimeDispatcher.requestSyncNow()
        }

    suspend fun cancelarPendientesDeSesion(idSesionProgramada: String) =
        notificacionDao.cancelarPendientesDeSesion(idSesionProgramada).also {
            SyncRuntimeDispatcher.requestSyncNow()
        }

    fun getHistorialPorUsuario(idUsuario: String, limite: Int = 200): Flow<List<NotificacionEntity>> =
        notificacionDao.getHistorialPorUsuario(idUsuario, limite)
}
