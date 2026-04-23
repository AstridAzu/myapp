package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Asignacion explicita de un plan a un usuario alumno realizada por un usuario con rol entrenador.
 *
 * Nota: no existe entidad Entrenador separada; ambos ids referencian la tabla usuarios.
 */
@Entity(
    tableName = "plan_asignaciones",
    foreignKeys = [
        ForeignKey(
            entity = PlanSemanaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idPlan"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuarioAsignador"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuarioAsignado"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idPlan"]),
        Index(value = ["idUsuarioAsignador"]),
        Index(value = ["idUsuarioAsignado"]),
        Index(value = ["idUsuarioAsignador", "idUsuarioAsignado"]),
        Index(value = ["idPlan", "idUsuarioAsignado", "activa"]),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class PlanAsignacionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idPlan: String,
    val idUsuarioAsignador: String,
    val idUsuarioAsignado: String,
    val activa: Boolean = true,
    val fechaAsignacion: Long = System.currentTimeMillis(),
    val fechaCancelacion: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)