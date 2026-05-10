package com.example.myapp.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class ApiEnvelope<T>(
    val success: Boolean,
    val result: T
)

data class ExercicioDTO(
    val id: String,
    val nombre: String,
    val grupoMuscular: String,
    val descripcion: String? = null,
    val colorHex: String? = null,
    val icono: String? = null,
    val imageUrl: String? = null, // AÑADIDO: Campo para la URL de la imagen
    val sync_status: String? = null,
    val idCreador: String? = null
)

data class ExercisesBaseResponse(
    val items: List<ExercicioDTO>,
    val nextSince: Long,
    val hasMore: Boolean
)

interface ExercisesApi {
    @GET("/api/exercises/base")
    suspend fun getBaseExercises(
        @Query("since") since: Long = 0L,
        @Query("limit") limit: Int = 200
    ): ApiEnvelope<ExercisesBaseResponse>
}
