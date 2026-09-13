package com.nmoreland.cognitivenexus.data.api

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

object NetworkClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun createApi(baseUrl: String): CognitiveNexusApi {
        val validatedUrl = baseUrl.toHttpUrl()
        require(validatedUrl.scheme == "http" || validatedUrl.scheme == "https") {
            "Backend URL must begin with http:// or https://"
        }
        return Retrofit.Builder()
            .baseUrl(validatedUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CognitiveNexusApi::class.java)
    }
}
