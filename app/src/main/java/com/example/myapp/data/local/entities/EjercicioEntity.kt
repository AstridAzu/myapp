package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "ejercicios",
    indices = [
        Index(value = ["idCreador"]),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class EjercicioEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val grupoMuscular: String,
    val descripcion: String? = null,
    /** Null = ejercicio base del sistema. Valor > 0 = creado por usuario. */
    val idCreador: String? = null,
    /** URL de imagen del ejercicio. Null = usar imagen generica local. */
    val imageUrl: String? = null,
    /** Override de color en hex. Null = usar color del grupoMuscular. */
    val colorHex: String? = null,
    /** Key del ícono. Null = usar ícono por defecto del grupo. */
    val icono: String? = null,
    /** Epoch ms usado para resolución de conflictos y paginación incremental. */
    val updatedAt: Long = System.currentTimeMillis(),
    /** Estado de sincronización local: PENDING, SYNCED, DELETED. */
    val syncStatus: String = "SYNCED",
    /** Epoch ms de borrado lógico; null cuando no está eliminado. */
    val deletedAt: Long? = null
)
