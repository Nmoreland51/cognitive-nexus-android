package com.nmoreland.cognitivenexus.data

import com.nmoreland.cognitivenexus.data.api.ChatRequest
import com.nmoreland.cognitivenexus.data.api.HealthResponse
import com.nmoreland.cognitivenexus.data.api.NetworkClient
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

class ChatRepository(private val settings: BackendSettingsRepository) {
    suspend fun sendMessage(message: String, sessionId: String): Result<String> = runCatching {
        api().chat(ChatRequest(message, sessionId)).reply.takeIf { it.isNotBlank() }
            ?: error("The backend returned an empty reply.")
    }.mapFailure()

    suspend fun checkHealth(): Result<HealthResponse> = runCatching {
        api().health()
    }.mapFailure()

    private suspend fun api() = NetworkClient.createApi(settings.backendUrl.first().also { baseUrl ->
        if (baseUrl.isBlank()) error("Backend configuration required. Set a backend URL in Settings.")
    })

    private fun <T> Result<T>.mapFailure(): Result<T> = recoverCatching { throwable ->
        throw when (throwable) {
            is IOException -> IllegalStateException("Cannot connect to the configured backend. Check the URL and network.")
            is HttpException -> IllegalStateException("Backend unavailable (${throwable.code()}).")
            else -> throwable
        }
    }
}
