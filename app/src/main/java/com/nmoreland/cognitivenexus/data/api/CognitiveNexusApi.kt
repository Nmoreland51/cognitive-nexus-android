package com.nmoreland.cognitivenexus.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.*
import java.util.UUID

/** Matches backend/models.py and the tested MOBILE_API_V2 contract. */
interface CognitiveNexusApi {
    @GET("api/health") suspend fun health(): HealthResponse
    @POST("api/jobs") suspend fun submit(@Body request: JobRequest): JobResponse
    @GET("api/jobs/{id}") suspend fun job(@Path("id") id: String): JobResponse
    @GET("api/sessions") suspend fun sessions(): SessionsResponse
    @GET("api/sessions/{id}") suspend fun messages(@Path("id") id: String): MessagesResponse
    @Streaming @GET("api/media/{id}") suspend fun image(@Path("id") id: String): ResponseBody
}

@Serializable data class HealthResponse(val ok: Boolean, val api_version: Int = 0)
@Serializable data class JobRequest(
    val request_id: String = UUID.randomUUID().toString(),
    val session_id: String,
    val action: String,
    val text: String = "",
    val name: String = "",
    val tags: String = "",
    val options: EngineOptions = EngineOptions(),
)
@Serializable data class JobResponse(val id: String, val action: String, val state: String, val result: JsonObject? = null, val error: String? = null)
@Serializable data class EngineOptions(
    val provider: String = "auto", val model: String = "",
    val use_memory: Boolean = true, val use_knowledge_for_chat: Boolean = true,
    val use_web_for_chat: Boolean = false, val auto_precision_mode: Boolean = true,
    val knowledge_use_ai: Boolean = false, val summarize: Boolean = false,
    val save_to_memory: Boolean = false, val depth: String = "Standard",
    val max_results: Int = 5, val follow_links: Boolean = false,
    val style: String = "realistic", val image_provider: String = "auto",
    val negative_prompt: String = "", val width: Int = 512, val height: Int = 512,
    val steps: Int = 25, val seed: Long? = null, val num_images: Int = 1,
)
@Serializable data class Message(val id: Long, val role: String, val content: String)
@Serializable data class MessagesResponse(val messages: List<Message>)
@Serializable data class Session(val id: String, val title: String? = null, val message_count: Int = 0)
@Serializable data class SessionsResponse(val sessions: List<Session>)
