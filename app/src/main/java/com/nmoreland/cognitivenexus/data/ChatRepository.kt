package com.nmoreland.cognitivenexus.data

import com.nmoreland.cognitivenexus.network.ChatRequest
import com.nmoreland.cognitivenexus.network.ChatResponse
import com.nmoreland.cognitivenexus.network.HealthResponse
import com.nmoreland.cognitivenexus.network.RetrofitFactory

interface ChatDataSource {
    suspend fun checkHealth(baseUrl: String): Result<HealthResponse>
    suspend fun sendMessage(
        baseUrl: String,
        message: String,
        sessionId: String = "android-session",
        model: String = "llama3.1:8b"
    ): Result<ChatResponse>
}

class ChatRepository : ChatDataSource {
    override suspend fun checkHealth(baseUrl: String): Result<HealthResponse> = runCatching {
        val response = RetrofitFactory.backendApi(baseUrl).health()
        if (!response.isSuccessful) {
            throw IllegalStateException("Health check failed: HTTP ${response.code()}")
        }
        response.body() ?: throw IllegalStateException("Health response was empty")
    }

    override suspend fun sendMessage(
        baseUrl: String,
        message: String,
        sessionId: String = "android-session",
        model: String = "llama3.1:8b"
    ): Result<ChatResponse> = runCatching {
        val response = RetrofitFactory.backendApi(baseUrl).chat(
            ChatRequest(
                message = message,
                session_id = sessionId,
                model = model
            )
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("Chat failed: HTTP ${response.code()}")
        }
        response.body() ?: throw IllegalStateException("Chat response was empty")
    }
}
