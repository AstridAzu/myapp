package com.example.myapp.data.repository

import com.example.myapp.data.local.dao.UsuarioDao
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.utils.PasswordHasher
import com.example.myapp.utils.SessionManager
import com.example.myapp.domain.models.Usuario
import com.example.myapp.domain.models.Rol
import androidx.room.withTransaction
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import com.example.myapp.BuildConfig
import com.example.myapp.data.remote.auth.AuthApiFactory
import com.example.myapp.data.remote.auth.LoginRequestDto
import com.example.myapp.data.remote.auth.RegisterRequestDto
import android.util.Log

class AuthRepository(
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val usuarioDao = database.usuarioDao()
    
    private val authApi by lazy {
        AuthApiFactory.create(
            baseUrl = BuildConfig.SYNC_API_BASE_URL.trim(),
            bearerToken = BuildConfig.SYNC_API_TOKEN.trim()
        )
    }

    suspend fun loginWithEmail(email: String, password: String): Result<Usuario> {
        return try {
            Log.d("AuthRepository", "Realizando login remoto...")
            val apiResponse = authApi.login(LoginRequestDto(email, password))
            
            if (!apiResponse.success) {
                Log.e("AuthRepository", "Login remoto falló: success=false")
                return Result.failure(Exception("Credenciales inválidas"))
            }
            
            val userDto = apiResponse.result.user
            
            // Convertir id de Int a String
            val userIdString = userDto.id.toString()
            
            // Mapear rol del Worker (TRAINER -> ENTRENADOR, ALUMNO -> ALUMNO, etc)
            val rolMapeado = mapearRolDelWorker(userDto.rol)
            
            // Convertir timestamp de segundos a milisegundos
            val fechaRegistroMs = userDto.fechaRegistro * 1000
            val updatedAtMs = (userDto.updatedAt ?: System.currentTimeMillis() / 1000) * 1000
            
            // Si el login remoto es exitoso, insertamos o actualizamos los datos básicos en la BD local
            val userEntity = UsuarioEntity(
                id = userIdString,
                nombre = userDto.nombre,
                rol = rolMapeado,
                activo = userDto.activo,
                fechaRegistro = fechaRegistroMs,
                updatedAt = updatedAtMs,
                syncStatus = "SYNCED",
                deletedAt = userDto.deletedAt?.let { it * 1000 }
            )
            
            // Si el usuario ya existe con ese ID, lo ignorará por el momento O deberíamos usar un UPSERT
            // Con insertIgnore al menos garantizamos que exista localmente
            val idLocal = usuarioDao.getUserById(userEntity.id)
            if (idLocal == null) {
                usuarioDao.insertIgnore(userEntity)
            } else {
                usuarioDao.update(userEntity)
            }

            sessionManager.saveSession(userEntity.id, userEntity.rol)
            Result.success(userEntity.toDomain())
            
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en login: ${e.message}", e)
            Result.failure(Exception("Credenciales inválidas o error de red"))
        }
    }
    
    /**
     * Mapea el rol devuelto por el Worker al rol interno de la app.
     * Worker: TRAINER, ADMIN, ALUMNO
     * App: ENTRENADOR, ADMIN, ALUMNO
     */
    private fun mapearRolDelWorker(rolWorker: String): String {
        return when (rolWorker.uppercase()) {
            "TRAINER" -> "ENTRENADOR"
            "ADMIN" -> "ADMIN"
            "ALUMNO" -> "ALUMNO"
            else -> "ALUMNO" // Por defecto
        }
    }

    suspend fun register(nombre: String, email: String, password: String): Result<String> {
        return try {
            Log.d("AuthRepository", "Realizando registro remoto...")
            val response = authApi.register(RegisterRequestDto(nombre, email, password))
            
            if (response.success) {
                Result.success("Registro exitoso")
            } else {
                val errorMsg = response.message ?: "Error desconocido en el registro"
                Log.e("AuthRepository", "Error registro: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Excepción en registro: ${e.message}", e)
            Result.failure(Exception("Error de conexión al registrar usuario. Inténtalo de nuevo."))
        }
    }

    private fun UsuarioEntity.toDomain() = Usuario(
        id = id,
        nombre = nombre,
        rol = Rol.valueOf(rol),
        activo = activo
    )
}
