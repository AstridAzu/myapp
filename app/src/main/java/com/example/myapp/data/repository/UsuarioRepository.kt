package com.example.myapp.data.repository

import com.example.myapp.data.local.dao.UsuarioDao
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.mapper.UsuarioSyncMapper

class UsuarioRepository(
    private val dao: UsuarioDao
) {

     suspend fun upsertFromApi(remote: UsuarioEntity) {
        val local = dao.getById(remote.id)

        val merged = UsuarioSyncMapper.merge(local, remote)

        dao.upsert(merged)
    }

    suspend fun upsertBatch(list: List<UsuarioEntity>) {
        list.forEach { upsertFromApi(it) }
    }
}