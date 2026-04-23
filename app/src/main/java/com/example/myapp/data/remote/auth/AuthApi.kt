package com.example.myapp.data.remote.auth

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

// Estructura raíz de la respuesta del Worker
data class LoginApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("result") val result: LoginResult
)

data class RegisterApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("result") val result: RegisterResult? = null,
    @SerializedName("message") val message: String? = null
)

data class RegisterResult(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String
)

data class LoginResult(
    @SerializedName("user") val user: LoginUserDto,
    @SerializedName("token") val token: String?
)

// El usuario dentro del resultado
data class LoginUserDto(
    @SerializedName("id") val id: Int, // El Worker devuelve número
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
