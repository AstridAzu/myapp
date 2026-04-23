package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa una notificación local planificada y su estado de entrega.
 */
@Entity(
    tableName = "notificaciones",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SesionProgramadaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idSesionProgramada"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["idUsuario"]),
        Index(value = ["idSesionProgramada"]),
        Index(value = ["tipo", "activo"]),
        Index(value = ["estado", "fechaProgramada"]),
        Index(value = ["idUsuario", "tipo", "activo"]),
        Index(value = ["idSesionProgramada", "fechaProgramada"]),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class NotificacionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idUsuario: String,
    val idSesionProgramada: String? = null,
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    val fechaProgramada: Long,
    val fechaEntrega: Long? = null,
    val estado: String = "PENDIENTE", // PENDIENTE, ENVIADA, FALLIDA, CANCELADA
    val intentos: Int = 0,
    val errorCodigo: String? = null,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
