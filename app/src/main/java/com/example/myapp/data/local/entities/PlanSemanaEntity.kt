package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Cabecera de un plan de entrenamiento semanal.
 *
 * - [idCreador] = quien construyó el plan (entrenador o el propio alumno).
 * - [idUsuario] = usuario al que aplica el plan.
 *   Ambos pueden ser el mismo id cuando el usuario se auto-gestiona.
 * - Se permiten múltiples planes [activo] = true de forma simultánea.
 *   El filtrado por vigencia y prioridad se resuelve en la capa de aplicación.
 * - [fechaInicio] / [fechaFin] son epoch-ms del primer y último día del plan.
 */
@Entity(
    tableName = "planes_semana",
    indices = [
        Index(value = ["idUsuario"]),
        Index(value = ["idCreador"]),
        Index(value = ["idCreador", "idUsuario", "activo"]),
        Index(value = ["updatedAt", "id"]),
        Index(value = ["syncStatus"])
    ]
)
data class PlanSemanaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idCreador: String,
    val idUsuario: String,
    val nombre: String,
    val fechaInicio: Long,           // epoch ms — primer día inclusivo
    val fechaFin: Long,              // epoch ms — último día inclusivo
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
