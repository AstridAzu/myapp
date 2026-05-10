package com.example.myapp.data.mapper

import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.remote.sync.TrainerResponseDto

fun TrainerResponseDto.toEntity(): UsuarioEntity {
    return UsuarioEntity(
        id = id,
        email = email,
        nombre = nombre,
        fotoUrl = fotoUrl,
        rol = "ENTRENADOR",
        activo = activo,
        updatedAt = System.currentTimeMillis(),
        syncStatus = "SYNCED"
    )
}