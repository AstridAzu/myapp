package com.example.myapp.data.mapper


import com.example.myapp.data.local.entities.UsuarioEntity

object UsuarioSyncMapper {

    fun merge(
        local: UsuarioEntity?,
        remote: UsuarioEntity
    ): UsuarioEntity {

        if (local == null) return remote

        return remote.copy(
            // Preservar campos locales que el remote podría no incluir
            fotoUrl = remote.fotoUrl ?: local.fotoUrl,
            syncStatus = "SYNCED",
            updatedAt = maxOf(local.updatedAt, remote.updatedAt)
        )
    }
}