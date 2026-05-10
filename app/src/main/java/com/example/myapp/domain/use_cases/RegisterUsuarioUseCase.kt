package com.example.myapp.domain.use_cases

import com.example.myapp.data.repository.AuthRepository
import com.example.myapp.domain.models.Rol

class RegisterUsuarioUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(nombre: String, email: String, pass: String): Result<String> {
        // Limpiar inputs
        val nombreLimpio = nombre.trim()
        val emailLimpio = email.trim().lowercase()
        val passLimpia = pass.trim()
        
        // Validar campos
        if (nombreLimpio.isBlank()) {
            return Result.failure(Exception("El nombre no puede estar vacío"))
        }
        if (nombreLimpio.length < 2) {
            return Result.failure(Exception("El nombre debe tener al menos 2 caracteres"))
        }
        if (nombreLimpio.length > 255) {
            return Result.failure(Exception("El nombre no puede exceder 255 caracteres"))
        }
        
        if (emailLimpio.isBlank()) {
            return Result.failure(Exception("El email no puede estar vacío"))
        }
        if (!isValidEmail(emailLimpio)) {
            return Result.failure(Exception("Email inválido: debe tener formato válido (ej: usuario@dominio.com)"))
        }
        if (emailLimpio.length > 255) {
            return Result.failure(Exception("El email no puede exceder 255 caracteres"))
        }
        
        if (passLimpia.isBlank()) {
            return Result.failure(Exception("La contraseña no puede estar vacía"))
        }
        if (passLimpia.length < 6) {
            return Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
        }
        if (passLimpia.length > 1024) {
            return Result.failure(Exception("La contraseña no puede exceder 1024 caracteres"))
        }
        
        return authRepository.register(nombreLimpio, emailLimpio, passLimpia)
    }
    
    /**
     * Validación básica de formato de email
     * Coincide con la validación típica de email en formularios
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex) && email.length <= 255
    }
}
