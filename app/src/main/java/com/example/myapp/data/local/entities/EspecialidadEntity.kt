package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "especialidades",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idUsuario"]),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class EspecialidadEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idUsuario: String,
    val nombre: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
