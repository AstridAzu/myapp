package com.example.myapp.data.remote.sync

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SyncApiFactory {
    private var serverTimeOffset: Long = 0

    fun updateServerTime(dateStr: String?) {
        dateStr?.let {
            try {
                val serverDate = java.util.Date(it)
                serverTimeOffset = serverDate.time - System.currentTimeMillis()
            } catch (e: Exception) {}
        }
    }

    fun getServerTime(): Long = System.currentTimeMillis() + serverTimeOffset

    fun create(baseUrl: String, bearerToken: String? = null, userId: String? = null): SyncApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            if (!bearerToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $bearerToken")
            }
            if (!userId.isNullOrBlank()) {
                requestBuilder.header("x-user-id", userId)
            }
            
            val response = chain.proceed(requestBuilder.build())
            
            // Sincronización de reloj con el servidor mediante el header "Date"
            updateServerTime(response.header("Date"))
            
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(SyncApi::class.java)
    }
}
