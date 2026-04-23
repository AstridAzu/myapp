package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "registros_series",
    indices = [
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class RegistroSerieEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idSesion: String,
    val idEjercicio: String,
    val numeroSerie: Int,       // 1-based
    val pesoKg: Float,
    val repsRealizadas: Int,
    val completada: Int = 1,   // siempre 1 al insertar (se inserta al marcar)
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
