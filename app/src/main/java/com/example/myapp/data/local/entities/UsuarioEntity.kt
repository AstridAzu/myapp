package com.example.myapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val nombre: String,
    val rol: String, // "ENTRENADOR", "ALUMNO"
    val activo: Boolean = true,
    val fechaRegistro: Long = System.currentTimeMillis()
)
