package com.example.myapp.domain.models

data class Certificacion(
    val id: String,
    val nombre: String,
    val institucion: String,
    val fechaObtencion: Long
)

data class Entrenador(
    val usuario: Usuario,
    val especialidades: List<String> = emptyList(),
    val certificaciones: List<Certificacion> = emptyList()
)
