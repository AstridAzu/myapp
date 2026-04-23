package com.example.myapp.data.remote.sync

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

data class SyncPushResponseDto(
    val acceptedIds: List<String>,
    val rejectedIds: List<String>
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

interface SyncApi {
    @POST("/api/sync/push")
    suspend fun pushChanges(@Body request: SyncPushRequestDto): SyncPushResponseDto

    @GET("/api/sync/pull")
    suspend fun pullChanges(
        @Query("entity") entityType: String,
        @Query("since") since: Long,
        @Query("limit") limit: Int = 200
    ): SyncPullResponseDto
}
