package com.example.myapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapp.data.local.entities.NotificacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notificacion: NotificacionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(notificaciones: List<NotificacionEntity>): List<Long>

    @Update
    suspend fun update(notificacion: NotificacionEntity)

    @Query(
        """
        SELECT * FROM notificaciones
        WHERE estado = 'PENDIENTE'
          AND activo = 1
                    AND syncStatus != 'DELETED'
          AND fechaProgramada <= :hasta
        ORDER BY fechaProgramada ASC
        LIMIT :limite
        """
    )
    suspend fun getPendientesHasta(hasta: Long, limite: Int = 100): List<NotificacionEntity>

    @Query(
        """
        UPDATE notificaciones
        SET estado = 'ENVIADA',
            fechaEntrega = :fechaEntrega,
            intentos = intentos + 1,
            errorCodigo = NULL,
            syncStatus = 'PENDING',
            updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000
        WHERE id = :id
        """
    )
    suspend fun marcarEnviada(id: String, fechaEntrega: Long)

    @Query(
        """
        UPDATE notificaciones
        SET estado = 'FALLIDA',
            intentos = intentos + 1,
            errorCodigo = :errorCodigo,
            syncStatus = 'PENDING',
            updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000
        WHERE id = :id
        """
    )
    suspend fun marcarFallida(id: String, errorCodigo: String?)

    @Query(
        """
        UPDATE notificaciones
        SET estado = 'CANCELADA',
                        activo = 0,
                        syncStatus = 'PENDING',
                        updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000
        WHERE estado = 'PENDIENTE'
          AND idSesionProgramada = :idSesionProgramada
        """
    )
    suspend fun cancelarPendientesDeSesion(idSesionProgramada: String)

    @Query(
        """
        SELECT * FROM notificaciones
        WHERE idUsuario = :idUsuario
          AND syncStatus != 'DELETED'
        ORDER BY fechaProgramada DESC
        LIMIT :limite
        """
    )
    fun getHistorialPorUsuario(idUsuario: String, limite: Int = 200): Flow<List<NotificacionEntity>>

    @Query("SELECT * FROM notificaciones WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<NotificacionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NotificacionEntity>)

    @Query("UPDATE notificaciones SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE notificaciones SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
