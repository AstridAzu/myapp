package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sesiones_rutina",
    indices = [
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class SesionRutinaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idRutina: String,
    val idUsuario: String,
    val fechaInicio: Long = System.currentTimeMillis(),
    val fechaFin: Long? = null,
    val completada: Int = 0,  // 0 = en curso, 1 = completada
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
