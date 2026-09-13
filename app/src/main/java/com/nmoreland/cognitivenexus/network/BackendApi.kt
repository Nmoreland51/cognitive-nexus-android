package com.nmoreland.cognitivenexus.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class HealthResponse(
    val ok: Boolean,
    val chat_model: String? = null,
    val time: String? = null
)

data class ChatRequest(
    val message: String,
    val session_id: String = "default",
    val model: String? = null
)

data class ChatResponse(
    val reply: String,
    val session_id: String,
    val model: String
)

interface BackendApi {
    @GET("api/health")
    suspend fun health(): Response<HealthResponse>

    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>
}
