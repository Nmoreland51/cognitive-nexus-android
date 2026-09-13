package com.nmoreland.cognitivenexus.network

import com.nmoreland.cognitivenexus.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitFactory {
    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient = OkHttpClient.Builder().apply {
        connectTimeout(10, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            addInterceptor(logger)
        }
    }.build()

    private fun normalizeBaseUrl(raw: String): String {
        var normalized = raw.trim()
        if (normalized.isEmpty()) return "http://10.0.2.2:8000/"
        normalized = normalized.removeSuffix("/")
        if (normalized.endsWith("/api")) {
            normalized = normalized.removeSuffix("/api")
        }
        return "$normalized/"
    }

    fun backendApi(baseUrl: String): BackendApi {
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}
