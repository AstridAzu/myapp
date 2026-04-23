package com.example.myapp.domain.use_cases

import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.repository.RutinaRepository

/**
 * Casos de uso para acceder a rutinas:
 *  - Un alumno se une con código
 *  - Un creador agrega a alguien por búsqueda
 */
class GestionAsignacionesUseCase(private val rutinaRepository: RutinaRepository) {

    suspend fun unirseConCodigo(idUsuario: String, codigo: String): Result<RutinaEntity> {
        if (codigo.isBlank()) return Result.failure(Exception("El código no puede estar vacío"))
        return rutinaRepository.unirseARutinaPorCodigo(idUsuario, codigo)
    }

    suspend fun agregarUsuario(idRutina: String, idUsuario: String): Result<Unit> =
        rutinaRepository.agregarUsuarioARutina(idRutina, idUsuario)
}
