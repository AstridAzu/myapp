package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.ObjetivoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjetivoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(objetivo: ObjetivoEntity): Long

    @Delete
    suspend fun delete(objetivo: ObjetivoEntity)

    @Query("SELECT * FROM objetivos WHERE idUsuario = :idUsuario AND syncStatus != 'DELETED'")
    fun getObjetivosByUsuario(idUsuario: String): Flow<List<ObjetivoEntity>>

    @Query("SELECT * FROM objetivos WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<ObjetivoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ObjetivoEntity>)

    @Query("UPDATE objetivos SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE objetivos SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
