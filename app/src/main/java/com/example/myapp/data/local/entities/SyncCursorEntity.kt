package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(
    @PrimaryKey val entityType: String,
    val since: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
