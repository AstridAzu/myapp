package com.example.myapp.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "userId") // Changed from id to userId
    val userId: Int = 0,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "password")
    val password: String,  // Guardará el hash, no la contraseña real

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)