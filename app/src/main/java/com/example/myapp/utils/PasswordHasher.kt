package com.example.myapp.utils

import java.security.MessageDigest
import java.util.Locale

/**
 * Utilidad para el hasheo de contraseñas.
 * Implementación base usando SHA-256 (se recomienda migrar a BCrypt si se añaden dependencias externas).
 */
object PasswordHasher {

    fun hash(password: String): String {
        val bytes = password.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    fun verify(password: String, hash: String): Boolean {
        return hash(password) == hash
    }
}
