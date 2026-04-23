package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "asignaciones",
    primaryKeys = ["idUsuarioOrigen", "idUsuarioDestino"],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuarioOrigen"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuarioDestino"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idUsuarioOrigen"]),
        Index(value = ["idUsuarioDestino"]),
        Index(value = ["updatedAt", "idUsuarioOrigen", "idUsuarioDestino"]),
        Index(value = ["syncStatus"])
    ]
)
data class AsignacionEntity(
    val idUsuarioOrigen: String,
    val idUsuarioDestino: String,
    val fechaAsignacion: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
