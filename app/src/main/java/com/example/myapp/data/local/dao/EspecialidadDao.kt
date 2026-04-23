package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.EspecialidadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EspecialidadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(especialidad: EspecialidadEntity): Long

    @Delete
    suspend fun delete(especialidad: EspecialidadEntity)

    @Query("SELECT * FROM especialidades WHERE idUsuario = :idUsuario AND syncStatus != 'DELETED'")
    fun getEspecialidadesByUsuario(idUsuario: String): Flow<List<EspecialidadEntity>>

    @Query("SELECT * FROM especialidades WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<EspecialidadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EspecialidadEntity>)

    @Query("UPDATE especialidades SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE especialidades SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
