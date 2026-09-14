package com.nmoreland.cognitivenexus.data.api

import com.nmoreland.cognitivenexus.data.Connection
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).build()
    fun createApi(connection: Connection): CognitiveNexusApi {
        require(connection.token.isNotBlank()) { "Enter your backend access token in Settings." }
        return Retrofit.Builder().baseUrl(connection.url)
            .client(client.newBuilder().addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer ${connection.token}").build())
            }.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(CognitiveNexusApi::class.java)
    }
}
