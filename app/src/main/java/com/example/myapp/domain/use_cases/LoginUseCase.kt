package com.example.myapp.domain.use_cases

import com.example.myapp.data.repository.AuthRepository
import com.example.myapp.domain.models.Usuario

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend fun executeWithEmail(email: String, pass: String): Result<Usuario> {
        val emailLimpio = email.trim()
        val passLimpio = pass.trim()
        
        if (emailLimpio.isBlank() || passLimpio.isBlank()) {
            return Result.failure(Exception("Campos obligatorios"))
        }
        return authRepository.loginWithEmail(emailLimpio, passLimpio)
    }
}
