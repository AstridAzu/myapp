package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

import com.example.myapp.data.remote.sync.SyncApiFactory

@Entity(
    tableName = "rutina_ejercicios",
    primaryKeys = ["idRutina", "idEjercicio"],
    foreignKeys = [
        ForeignKey(
            entity = RutinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idRutina"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EjercicioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idEjercicio"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idRutina"]),
        Index(value = ["idEjercicio"]),
        Index(value = ["updatedAt", "idRutina", "idEjercicio"]),
        Index(value = ["syncStatus"])
    ]
)
data class RutinaEjercicioEntity(
    val idRutina: String,
    val idEjercicio: String,
    val series: Int,
    val reps: Int,
    val orden: Int,
    val notas: String? = null,
    val updatedAt: Long = 0L,
    val syncStatus: String = "SYNCED",
    val deletedAt: Long? = null
)
