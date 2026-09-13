package com.nmoreland.cognitivenexus.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CognitiveNexusApi {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

@Serializable
data class HealthResponse(
    val ok: Boolean,
    @SerialName("chat_model") val chatModel: String? = null,
)

@Serializable
data class ChatRequest(
    val message: String,
    @SerialName("session_id") val sessionId: String,
)

@Serializable
data class ChatResponse(
    val reply: String,
    @SerialName("session_id") val sessionId: String,
    val model: String? = null,
)
