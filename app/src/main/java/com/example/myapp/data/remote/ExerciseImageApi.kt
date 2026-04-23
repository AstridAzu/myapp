package com.example.myapp.data.remote

import android.util.Log
import com.example.myapp.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExerciseImagePresignedResult(
    val exerciseId: String,
    val objectKey: String,
    val uploadUrl: String,
    val expiresAt: Long,
    val maxSizeBytes: Long
)

data class ExerciseImageResult(
    val exerciseId: String,
    val objectKey: String,
    val contentType: String,
    val sizeBytes: Long,
    val publicUrl: String
)

class ExerciseImageApi(
    private val baseUrl: String,
    private val bearerToken: String
) {

    suspend fun createPresignedUpload(
        exerciseId: String,
        fileName: String,
        contentType: String,
        sizeBytes: Long
    ): ExerciseImagePresignedResult = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/exercises/$exerciseId/images/presigned"
        val payload = JSONObject()
            .put("fileName", fileName)
            .put("contentType", contentType)
            .put("sizeBytes", sizeBytes)

        val json = executeJsonRequest(
            url = url,
            method = "POST",
            body = payload.toString(),
            contentType = "application/json",
            requiresAuth = true
        )

        val result = json.getJSONObject("result")
        ExerciseImagePresignedResult(
            exerciseId = result.getString("exerciseId"),
            objectKey = result.getString("objectKey"),
            uploadUrl = result.getString("uploadUrl"),
            expiresAt = result.getLong("expiresAt"),
            maxSizeBytes = result.getLong("maxSizeBytes")
        )
    }

    suspend fun uploadBinary(
        uploadUrl: String,
        contentType: String,
        data: ByteArray
    ): ExerciseImageResult = withContext(Dispatchers.IO) {
        val json = executeBinaryUpload(
            url = uploadUrl,
            contentType = contentType,
            data = data
        )

        val result = json.getJSONObject("result")
        ExerciseImageResult(
            exerciseId = result.getString("exerciseId"),
            objectKey = result.getString("objectKey"),
            contentType = result.getString("contentType"),
            sizeBytes = result.getLong("sizeBytes"),
            publicUrl = result.getString("publicUrl")
        )
    }

    suspend fun confirmUpload(
        exerciseId: String,
        objectKey: String
    ): ExerciseImageResult = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/exercises/$exerciseId/images/confirm"
        val payload = JSONObject().put("objectKey", objectKey)
        val json = executeJsonRequest(
            url = url,
            method = "POST",
            body = payload.toString(),
            contentType = "application/json",
            requiresAuth = true
        )

        val result = json.getJSONObject("result")
        ExerciseImageResult(
            exerciseId = result.getString("exerciseId"),
            objectKey = result.getString("objectKey"),
            contentType = result.getString("contentType"),
            sizeBytes = result.getLong("sizeBytes"),
            publicUrl = result.getString("publicUrl")
        )
    }

    suspend fun deleteImage(
        exerciseId: String,
        objectKey: String
    ) = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/exercises/$exerciseId/images"
        val payload = JSONObject().put("objectKey", objectKey)
        executeJsonRequest(
            url = url,
            method = "DELETE",
            body = payload.toString(),
            contentType = "application/json",
            requiresAuth = true
        )
    }

    private fun executeJsonRequest(
        url: String,
        method: String,
        body: String?,
        contentType: String,
        requiresAuth: Boolean
    ): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", contentType)
            if (requiresAuth) {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "ConfigCheck",
                        "ExerciseImageApi auth check: IMAGE_API_TOKEN configured=${bearerToken.isNotBlank()}"
                    )
                }
                ensureAuthToken()
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            doInput = true
            doOutput = body != null
        }

        try {
            if (body != null) {
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(Charsets.UTF_8))
                }
            }
            val code = connection.responseCode
            val responseBody = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                throw IllegalStateException(parseApiError(responseBody, code))
            }

            return JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun executeBinaryUpload(
        url: String,
        contentType: String,
        data: ByteArray
    ): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 30_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", contentType)
            doInput = true
            doOutput = true
        }

        try {
            connection.outputStream.use { stream ->
                stream.write(data)
            }

            val code = connection.responseCode
            val responseBody = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                throw IllegalStateException(parseApiError(responseBody, code))
            }

            return JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseApiError(responseBody: String, code: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val message = json.optString("message")
            if (message.isBlank()) "HTTP $code" else message
        } catch (_: Exception) {
            if (responseBody.isBlank()) "HTTP $code" else responseBody
        }
    }

    private fun ensureAuthToken() {
        if (bearerToken.isBlank()) {
            throw IllegalStateException("IMAGE_API_TOKEN is not configured")
        }
    }
}
