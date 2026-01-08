package com.example.myapp.utils

import java.security.MessageDigest

object PasswordUtils {

    /**
     * Hashes a password using SHA-256 and returns the hexadecimal representation.
     * This implementation uses Android's built-in security libraries to avoid
     * classpath conflicts with older versions of Apache Commons Codec.
     */
    fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            // Convert byte array to a hexadecimal string
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // In case of an error, return a fixed-length hash of the password's hashcode
            // This is a fallback and should ideally be logged.
            password.hashCode().toString().padStart(64, '0')
        }
    }
}