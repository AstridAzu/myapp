package com.example.myapp.domain.models

data class Usuario(
    val id: String,
    val nombre: String,
    val rol: Rol,
    val activo: Boolean
)
