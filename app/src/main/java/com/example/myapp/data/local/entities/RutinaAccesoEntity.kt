package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "rutina_accesos",
    primaryKeys = ["idRutina", "idUsuario"],
    foreignKeys = [
        ForeignKey(
            entity = RutinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idRutina"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idRutina"]),
        Index(value = ["idUsuario"]),
        Index(value = ["updatedAt", "idRutina", "idUsuario"]),
        Index(value = ["syncStatus"])
    ]
)
data class RutinaAccesoEntity(
    val idRutina: String,
    val idUsuario: String,
    val fechaAcceso: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
