package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Representa una ranura de entrenamiento dentro de un plan semanal.
 *
 * - [diaSemana]: 1 = Lunes … 7 = Domingo (ISO 8601).
 * - [tipo]: "RUTINA" cuando hay una rutina asignada, "DESCANSO" para días de
 *   recuperación explícitos. Si tipo == "DESCANSO", [idRutina] debe ser null.
 * - [orden]: permite asignar múltiples rutinas al mismo día (ej. mañana/tarde).
 *   Empieza en 1 para cada (idPlan, diaSemana).
 */
@Entity(
    tableName = "plan_dias",
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
        Index(value = ["idPlan", "diaSemana"]),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class PlanDiaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idPlan: String,
    val diaSemana: Int,             // 1 = Lun, 2 = Mar, …, 7 = Dom
    val tipo: String,               // "RUTINA" | "DESCANSO"
    val idRutina: String? = null,   // null cuando tipo == "DESCANSO"
    val orden: Int = 1,             // posición dentro del día
    val notas: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
