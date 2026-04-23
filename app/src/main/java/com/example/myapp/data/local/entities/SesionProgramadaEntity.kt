package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Materializa cada ocurrencia concreta de una ranura del plan ([PlanDiaEntity])
 * en una fecha calendario real.
 *
 * Se genera de forma lazy (al consultar una semana) por [PlanRepository.materializarSemana].
 * El insert usa OnConflictStrategy.IGNORE para ser idempotente: llamar al
 * materializador varias veces para la misma semana no duplica filas.
 *
 * Ciclo de vida de [completada] / [omitida]:
 *   - Pendiente  → completada = 0, omitida = 0, idSesion = null
 *   - En curso   → completada = 0, omitida = 0, idSesion = <id>
 *   - Completada → completada = 1, omitida = 0, idSesion = <id>
 *   - Omitida    → completada = 0, omitida = 1, idSesion = null
 *
 * [fechaProgramada] es epoch ms del inicio del día (medianoche local).
 */
@Entity(
    tableName = "sesiones_programadas",
    foreignKeys = [
        ForeignKey(
            entity = PlanDiaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idPlanDia"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idPlanDia"]),
        Index(value = ["fechaProgramada"]),
        Index(value = ["idPlanDia", "fechaProgramada"], unique = true),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class SesionProgramadaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idPlanDia: String,
    val fechaProgramada: Long,      // epoch ms — medianoche UTC del día programado
    val idSesion: String? = null,   // FK lógica a sesiones_rutina.id; null = aún no iniciada
    val completada: Int = 0,        // 1 = sesión finalizada y vinculada
    val omitida: Int = 0,           // 1 = usuario marcó el día como omitido
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
