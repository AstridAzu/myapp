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
import retrofit2.HttpException

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
            val apiResponse = authApi.login(LoginRequestDto(email, password))
            
            if (!apiResponse.success) {
                Log.e("AuthRepository", "Login remoto falló: success=false")
                return Result.failure(Exception("Credenciales inválidas"))
            }
            
            val userDto = apiResponse.result.user
            
            // El id ya es UUID (String) desde el servidor
            val userIdString = userDto.id
            
            Log.i("AuthRepository", "═══════════════════════════════════════════════════════")
            Log.i("AuthRepository", "✅ LOGIN EXITOSO")
            Log.i("AuthRepository", "   Email: ${userDto.nombre}")
            Log.i("AuthRepository", "   Rol desde servidor: ${userDto.rol}")
            Log.i("AuthRepository", "   ID/UUID desde servidor: $userIdString")
            
            // Mapear rol del Worker (TRAINER -> ENTRENADOR, ALUMNO -> ALUMNO, etc)
            val rolMapeado = mapearRolDelWorker(userDto.rol)
            
            Log.i("AuthRepository", "   Rol mapeado: $rolMapeado")
            
            // Convertir timestamp de segundos a milisegundos
            val fechaRegistroMs = userDto.fechaRegistro * 1000
            val updatedAtMs = (userDto.updatedAt ?: System.currentTimeMillis() / 1000) * 1000
            
            // Si el login remoto es exitoso, insertamos o actualizamos los datos básicos en la BD local
            val userEntity = UsuarioEntity(
                id = userIdString,
                nombre = userDto.nombre,
                email = userDto.email,
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
                Log.i("AuthRepository", "   ✓ Nuevo usuario insertado en BD local")
            } else {
                usuarioDao.update(userEntity)
                Log.i("AuthRepository", "   ✓ Usuario actualizado en BD local")
            }
            
            // Verificación: intentar recuperar el usuario de la BD para confirmar que se guardó
            val usuarioVerificacion = usuarioDao.getUserById(userEntity.id)
            if (usuarioVerificacion != null) {
                Log.i("AuthRepository", "   ✓✓ VERIFICACIÓN: Usuario encontrado en BD tras guardar")
                Log.i("AuthRepository", "      Nombre en BD: '${usuarioVerificacion.nombre}'")
            } else {
                Log.w("AuthRepository", "   ⚠⚠ VERIFICACIÓN: Usuario NO encontrado en BD tras guardar (¡¡¡PROBLEMA!!!)")
            }

            val rawToken = apiResponse.result.token
            // Limpiar el token de cualquier prefijo "Bearer " antes de guardarlo
            val cleanedToken = rawToken?.removePrefix("Bearer ")
            Log.i("AuthRepository", "   Token recibido del servidor: ${if (rawToken != null) rawToken.take(15) + "..." else "❌ NULL"}")
            Log.i("AuthRepository", "   Token limpio a guardar: ${if (cleanedToken != null) cleanedToken.take(15) + "..." else "❌ NULL"}")
            
            sessionManager.saveSession(userEntity.id, userEntity.rol, cleanedToken)
            Log.i("AuthRepository", "   SessionManager.saveSession() completado")
            Log.i("AuthRepository", "   SessionManager.getAuthToken() persistido: ${if (sessionManager.getAuthToken() != null) "✓ SÍ" else "❌ NO"}")
            Log.i("AuthRepository", "   SessionManager.getUserIdString() = ${sessionManager.getUserIdString()}")
            Log.i("AuthRepository", "   SessionManager.getUserRol() = ${sessionManager.getUserRol()}")
            Log.i("AuthRepository", "═══════════════════════════════════════════════════════")
            
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
            Log.d("AuthRepository", "Iniciando registro para: $email")
            val response = authApi.register(RegisterRequestDto(email, password, nombre))
            
            if (response.success && response.result != null) {
                val userDto = response.result.user
                
                // Mapear rol del Worker
                val rolMapeado = mapearRolDelWorker(userDto.rol)
                
                // Convertir timestamp de segundos a milisegundos
                val fechaRegistroMs = userDto.fechaRegistro * 1000
                
                // Guardar usuario en BD local
                val userEntity = UsuarioEntity(
                    id = userDto.id,
                    nombre = userDto.nombre,
                    rol = rolMapeado,
                    activo = userDto.activo,
                    email = userDto.email,
                    fechaRegistro = fechaRegistroMs,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "SYNCED",
                    deletedAt = null
                )
                
                val idLocal = usuarioDao.getUserById(userEntity.id)
                if (idLocal == null) {
                    usuarioDao.insertIgnore(userEntity)
                    Log.i("AuthRepository", "   ✓ Nuevo usuario insertado en BD local durante registro")
                } else {
                    usuarioDao.update(userEntity)
                    Log.i("AuthRepository", "   ✓ Usuario actualizado en BD local durante registro")
                }
                
                // Verificación: intentar recuperar el usuario de la BD para confirmar que se guardó
                val usuarioVerificacion = usuarioDao.getUserById(userEntity.id)
                if (usuarioVerificacion != null) {
                    Log.i("AuthRepository", "   ✓✓ VERIFICACIÓN REGISTRO: Usuario encontrado en BD tras guardar")
                    Log.i("AuthRepository", "      Nombre en BD: '${usuarioVerificacion.nombre}'")
                } else {
                    Log.w("AuthRepository", "   ⚠⚠ VERIFICACIÓN REGISTRO: Usuario NO encontrado en BD tras guardar (¡¡¡PROBLEMA!!!)")
                }
                
                val rawToken = response.result.token
                // Limpiar el token de cualquier prefijo "Bearer " antes de guardarlo
                val cleanedToken = rawToken?.removePrefix("Bearer ")
                sessionManager.saveSession(userEntity.id, userEntity.rol, cleanedToken)
                
                Log.d("AuthRepository", "Registro exitoso para usuario: ${userDto.email}")
                Result.success("Registro exitoso")
            } else {
                val errorMsg = response.message ?: "Error desconocido en el registro"
                Log.e("AuthRepository", "Error registro: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (httpException: HttpException) {
            val errorCode = httpException.code()
            val errorBody = httpException.response()?.errorBody()?.string() ?: "Sin cuerpo de error"
            Log.e("AuthRepository", "Error HTTP $errorCode en registro: $errorBody", httpException)
            
            val mensajeError = when {
                errorCode == 400 -> "Datos inválidos: verifica email, contraseña y nombre"
                errorCode == 409 -> "El email ya está registrado"
                errorCode == 500 -> "Error del servidor. Intenta más tarde"
                else -> "Error HTTP $errorCode"
            }
            Result.failure(Exception(mensajeError))
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
