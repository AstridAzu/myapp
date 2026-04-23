package com.example.myapp.data.repository

import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.ObjetivoEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

class AlumnoRepository(private val database: AppDatabase) {
    private val usuarioDao = database.usuarioDao()
    private val objetivoDao = database.objetivoDao()

    suspend fun getUsuarioById(userId: String): UsuarioEntity? =
        usuarioDao.getUserById(userId)

    fun getObjetivosDeUsuario(idUsuario: String): Flow<List<ObjetivoEntity>> =
        objetivoDao.getObjetivosByUsuario(idUsuario)
}
