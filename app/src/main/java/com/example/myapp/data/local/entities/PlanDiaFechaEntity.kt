package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Día específico de calendario dentro de un plan.
 */
@Entity(
    tableName = "plan_dias_fecha",
    foreignKeys = [
        ForeignKey(
            entity = PlanSemanaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idPlan"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idPlan"]),
        Index(value = ["idPlan", "fecha"], unique = true),
        Index(value = ["idPlan", "diaSemana"]),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class PlanDiaFechaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idPlan: String,
    val fecha: Long,
    val diaSemana: Int,
    val tipo: String,
    val idRutina: String? = null,
    val orden: Int = 1,
    val notas: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
