package com.example.myapp.data.remote

import com.example.myapp.data.remote.sync.SyncApi
import com.example.myapp.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClientProvider {

    fun create(
        type: ApiType,
        baseUrl: String,
        sessionManager: SessionManager
    ): SyncApi {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()

            val token = when (type) {
                ApiType.SYNC -> com.example.myapp.BuildConfig.SYNC_API_TOKEN
                ApiType.IMAGE -> com.example.myapp.BuildConfig.IMAGE_API_TOKEN
            }

            request.header("Authorization", "Bearer $token")
            request.header("x-user-id", sessionManager.getUserIdString())

            chain.proceed(request.build())
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