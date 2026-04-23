package com.example.myapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapp.data.local.entities.SyncCursorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncCursorDao {
    @Query("SELECT * FROM sync_cursors WHERE entityType = :entityType LIMIT 1")
    suspend fun getByEntityType(entityType: String): SyncCursorEntity?

    @Query("SELECT * FROM sync_cursors")
    fun observeAll(): Flow<List<SyncCursorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncCursorEntity)
}
