package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "certificaciones",
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
data class CertificacionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idUsuario: String,
    val nombre: String,
    val institucion: String,
    val fechaObtencion: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
