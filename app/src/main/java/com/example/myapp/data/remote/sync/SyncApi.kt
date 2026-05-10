package com.example.myapp.data.remote.sync

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class SyncPushItemDto(
    val entityType: String,
    val id: String,
    val updatedAt: Long,
    val syncStatus: String,
    val payload: JsonObject
)

data class SyncPushRequestDto(
    val items: List<SyncPushItemDto>
)

data class SyncRejectedItemDto(
    val id: String,
    val reason: String,
    val message: String
)

data class SyncPushResponseDto(
    val acceptedIds: List<String>? = emptyList(),
    val rejectedIds: List<String>? = emptyList(),
    val rejectedItems: List<SyncRejectedItemDto>? = emptyList()
)

data class SyncPullItemDto(
    val entityType: String,
    val id: String,
    val updatedAt: Long,
    val syncStatus: String,
    val deletedAt: Long?,
    val payload: JsonObject
)

data class SyncPullResponseDto(
    val items: List<SyncPullItemDto>,
    val nextSince: Long
)

// ========== TRAINERS ENDPOINTS DTOs ==========

data class CertificacionTrainerDto(
    val nombre: String,
    val institucion: String,
    val fechaObtencion: Long
)

data class TrainerResponseDto(
    val id: String,
    val nombre: String,
    val email: String,
    val telefonoContacto: String?,
    val fotoUrl: String?,
    val activo: Boolean,
    val fechaRegistro: Long,
    val especialidades: List<String>,
    val certificaciones: List<CertificacionTrainerDto>
)

data class TrainersListResultDto(
    val items: List<TrainerResponseDto>
)

data class TrainersListResponseDto(
    val success: Boolean,
    val result: TrainersListResultDto
)

// ========== USER IMAGES ENDPOINTS DTOs ==========

data class PresignedUrlRequestDto(
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long
)

data class PresignedUrlResultDto(
    val userId: String,
    val objectKey: String,
    val uploadUrl: String,
    val expiresAt: Long,
    val maxSizeBytes: Long
)

data class UploadImageResultDto(
    val userId: String,
    val objectKey: String,
    val contentType: String,
    val sizeBytes: Long,
    val publicUrl: String
)

data class DeleteImageRequestDto(
    val objectKey: String
)

data class DeleteImageResultDto(
    val userId: String,
    val objectKey: String
)

data class UserImageResponseDto<T>(
    val success: Boolean,
    val result: T
)

interface SyncApi {
    @POST("/api/sync/push")
    suspend fun pushChanges(@Body request: SyncPushRequestDto): SyncPushResponseDto

    @GET("/api/sync/pull")
    suspend fun pullChanges(
        @Query("entity") entityType: String,
        @Query("since") since: Long,
        @Query("limit") limit: Int = 200
    ): SyncPullResponseDto

    // ========== TRAINERS ENDPOINTS ==========
    
    @GET("/api/trainers")
    suspend fun getTrainers(@Query("limit") limit: Int = 100): TrainersListResponseDto

    // ========== USER IMAGES ENDPOINTS ==========
    
    @POST("/api/users/{userId}/images/presigned")
    suspend fun getPresignedUrl(
        @Path("userId") userId: String,
        @Body request: PresignedUrlRequestDto
    ): UserImageResponseDto<PresignedUrlResultDto>

    @PUT("/api/users/{userId}/images/upload")
    suspend fun uploadUserImage(
        @Path("userId") userId: String,
        @Body imageData: okhttp3.RequestBody
    ): UserImageResponseDto<UploadImageResultDto>

    @DELETE("/api/users/{userId}/images")
    suspend fun deleteUserImage(
        @Path("userId") userId: String,
        @Body request: DeleteImageRequestDto
    ): UserImageResponseDto<DeleteImageResultDto>
}
