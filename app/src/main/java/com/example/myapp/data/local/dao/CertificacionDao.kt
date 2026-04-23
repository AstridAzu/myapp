package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.CertificacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(certificacion: CertificacionEntity): Long

    @Delete
    suspend fun delete(certificacion: CertificacionEntity)

    @Query("SELECT * FROM certificaciones WHERE idUsuario = :idUsuario AND syncStatus != 'DELETED'")
    fun getCertificacionesByUsuario(idUsuario: String): Flow<List<CertificacionEntity>>

    @Query("SELECT * FROM certificaciones WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<CertificacionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CertificacionEntity>)

    @Query("UPDATE certificaciones SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE certificaciones SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
