package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import com.example.myapp.data.remote.sync.SyncApiFactory

// idCreador = 0L reservado para rutinas del sistema (sin creador real)
@Entity(
    tableName = "rutinas",
    indices = [
        Index(value = ["idCreador"]),
        Index(value = ["codigo"], unique = true),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class RutinaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idCreador: String,
    val nombre: String,
    val descripcion: String? = null,
    val fechaCreacion: Long = 0L,
    val activa: Boolean = true,
    val codigo: String,
    /** Color de acento en formato hex, ej. "#E53935". Null = usar color por defecto. */
    val colorHex: String? = null,
    /** Key del ícono, ej. "FITNESS_CENTER". Null = usar ícono por defecto. */
    val icono: String? = null,
    val updatedAt: Long = 0L,
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
