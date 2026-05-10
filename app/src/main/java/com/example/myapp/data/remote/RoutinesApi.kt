package com.example.myapp.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class RutinaDTO(
    val id: String,
    val nombre: String,
    val codigo: String,
    val colorHex: String? = null,
    val icono: String? = null,
    val descripcion: String? = null,
    val idCreador: String? = null,
    val activa: Int? = null,
    val fechaCreacion: Long? = null
)

data class RoutinesBaseResponse(
    val items: List<RutinaDTO>,
    val total: Int? = null
)

data class RutinaBaseLinkDTO(
    val idRutina: String,
    val idEjercicio: String,
    val series: Int,
    val reps: String,
    val orden: Int,
    val notas: String? = null,
    val updatedAt: Long? = null,
    val syncStatus: String? = null,
    val deletedAt: Long? = null
)

data class RoutinesBaseLinksResponse(
    val items: List<RutinaBaseLinkDTO>,
    val total: Int? = null
)

interface RoutinesApi {
    @GET("/api/routines/base")
    suspend fun getBaseRoutines(
        @Query("since") since: Long = 0L,
        @Query("limit") limit: Int = 200
    ): ApiEnvelope<RoutinesBaseResponse>

    @GET("/api/routines/base/links")
    suspend fun getBaseRoutineLinks(
        @Query("routineId") routineId: String? = null,
        @Query("limit") limit: Int = 500
    ): ApiEnvelope<RoutinesBaseLinksResponse>
}
