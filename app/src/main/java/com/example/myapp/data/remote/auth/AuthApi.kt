package com.example.myapp.data.remote.auth

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("nombre") val nombre: String
)

// Estructura raíz de la respuesta del Worker
data class LoginApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("result") val result: LoginResult
)

data class RegisterApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("result") val result: RegisterResultWrapper? = null,
    @SerializedName("message") val message: String? = null
)

// Estructura correcta según el servidor: envuelve user y token
data class RegisterResultWrapper(
    @SerializedName("user") val user: RegisterUserDto,
    @SerializedName("token") val token: String? = null
)

data class RegisterUserDto(
    @SerializedName("id") val id: String, // UUID como string
    @SerializedName("email") val email: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("passwordHash") val passwordHash: String? = null,
    @SerializedName("activo") val activo: Boolean,
    @SerializedName("fechaRegistro") val fechaRegistro: Long
)

data class LoginResult(
    @SerializedName("user") val user: LoginUserDto,
    @SerializedName("token") val token: String?
)

// El usuario dentro del resultado
data class LoginUserDto(
    @SerializedName("id") val id: String, // UUID desde Worker
    @SerializedName("email") val email: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("rol") val rol: String, // "TRAINER", "ALUMNO", etc.
    @SerializedName("passwordHash") val passwordHash: String? = null,
    @SerializedName("activo") val activo: Boolean,
    @SerializedName("fechaRegistro") val fechaRegistro: Long, // En segundos desde Worker
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("syncStatus") val syncStatus: String? = null,
    @SerializedName("deletedAt") val deletedAt: Long? = null
)

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginApiResponse

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): RegisterApiResponse
}
