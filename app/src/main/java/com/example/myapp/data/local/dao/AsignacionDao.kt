package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.AsignacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AsignacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asignacion: AsignacionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AsignacionEntity>)

    @Query(
        """
        UPDATE asignaciones
        SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE idUsuarioOrigen = :idUsuarioOrigen AND idUsuarioDestino = :idUsuarioDestino
        """
    )
    suspend fun softDelete(idUsuarioOrigen: String, idUsuarioDestino: String, deletedAt: Long, updatedAt: Long)

    @Query(
        """
        UPDATE asignaciones
        SET syncStatus = :syncStatus, updatedAt = :updatedAt
        WHERE idUsuarioOrigen = :idUsuarioOrigen AND idUsuarioDestino = :idUsuarioDestino
        """
    )
    suspend fun markSyncState(idUsuarioOrigen: String, idUsuarioDestino: String, syncStatus: String, updatedAt: Long)

    @Query(
        """
        SELECT * FROM asignaciones
        WHERE syncStatus = :syncStatus
        ORDER BY updatedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<AsignacionEntity>

    @Query("SELECT * FROM asignaciones WHERE idUsuarioOrigen = :idOrigen AND idUsuarioDestino = :idDestino AND syncStatus != 'DELETED' LIMIT 1")
    suspend fun getAsignacion(idOrigen: String, idDestino: String): AsignacionEntity?

    @Query("SELECT idUsuarioDestino FROM asignaciones WHERE idUsuarioOrigen = :idOrigen AND syncStatus != 'DELETED'")
    fun getDestinosByOrigen(idOrigen: String): Flow<List<String>>

    @Query("SELECT idUsuarioOrigen FROM asignaciones WHERE idUsuarioDestino = :idDestino AND syncStatus != 'DELETED'")
    suspend fun getOrigenesByDestino(idDestino: String): List<String>
}
