package com.example.myapp.domain.models

data class Alumno(
    val usuario: Usuario,
    val objetivos: List<String> = emptyList()
)
