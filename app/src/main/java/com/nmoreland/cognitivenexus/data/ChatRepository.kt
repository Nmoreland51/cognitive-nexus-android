package com.nmoreland.cognitivenexus.data

import com.nmoreland.cognitivenexus.data.api.ChatRequest
import com.nmoreland.cognitivenexus.data.api.NetworkClient
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

class ChatRepository(private val settings: BackendSettingsRepository) {
    suspend fun sendMessage(message: String, sessionId: String): Result<String> = runCatching {
        val baseUrl = settings.backendUrl.first()
        if (baseUrl.isBlank()) error("Backend configuration required. Set a backend URL in Settings.")
        val response = NetworkClient.createApi(baseUrl).chat(ChatRequest(message, sessionId))
        response.reply.takeIf { it.isNotBlank() } ?: error("The backend returned an empty reply.")
    }.recoverCatching { throwable ->
        throw when (throwable) {
            is IOException -> IllegalStateException("Cannot connect to the configured backend. Check the URL and network.")
            is HttpException -> IllegalStateException("Backend unavailable (${throwable.code()}).")
            else -> throwable
        }
    }
}
