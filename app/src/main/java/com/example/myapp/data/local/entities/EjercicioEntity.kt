package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ejercicios")
data class EjercicioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val grupoMuscular: String,
    val descripcion: String? = null,
    /** Override de color en hex. Null = usar color del grupoMuscular. */
    val colorHex: String? = null,
    /** Key del ícono. Null = usar ícono por defecto del grupo. */
    val icono: String? = null
)
