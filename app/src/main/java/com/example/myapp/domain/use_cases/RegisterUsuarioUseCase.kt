package com.example.myapp.domain.use_cases

import com.example.myapp.data.repository.AuthRepository
import com.example.myapp.domain.models.Rol

class RegisterUsuarioUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(nombre: String, email: String, pass: String): Result<String> {
        if (nombre.isBlank() || email.isBlank() || pass.length < 6) {
            return Result.failure(Exception("Datos inválidos o contraseña corta (min 6)"))
        }
        return authRepository.register(nombre, email, pass)
    }
}
