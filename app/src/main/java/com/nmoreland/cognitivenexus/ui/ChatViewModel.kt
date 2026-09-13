package com.nmoreland.cognitivenexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nmoreland.cognitivenexus.data.BackendSettingsRepository
import com.nmoreland.cognitivenexus.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(val id: String = UUID.randomUUID().toString(), val text: String, val isUser: Boolean)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val backendUrl: String = "",
    val backendStatus: String = "Checking backend…",
    val activeModel: String? = null,
    val isSending: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settings: BackendSettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()
    private val sessionId = UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            settings.backendUrl.collectLatest { url ->
                mutableState.update { it.copy(backendUrl = url, backendStatus = "Checking backend…", activeModel = null) }
                refreshBackendStatus()
            }
        }
    }

    fun updateDraft(value: String) = mutableState.update { it.copy(draft = value, error = null) }

    fun send() {
        val message = state.value.draft.trim()
        if (message.isEmpty() || state.value.isSending) return
        mutableState.update { it.copy(draft = "", isSending = true, error = null, messages = it.messages + ChatMessage(text = message, isUser = true)) }
        viewModelScope.launch {
            chatRepository.sendMessage(message, sessionId)
                .onSuccess { reply -> mutableState.update { it.copy(isSending = false, messages = it.messages + ChatMessage(text = reply, isUser = false)) } }
                .onFailure { error -> mutableState.update { it.copy(isSending = false, error = error.message ?: "Unable to reach the backend.") } }
        }
    }

    fun refreshBackendStatus() {
        viewModelScope.launch {
            chatRepository.checkHealth()
                .onSuccess { health ->
                    mutableState.update {
                        it.copy(
                            backendStatus = if (health.ok) "Connected" else "Unavailable",
                            activeModel = health.chatModel,
                        )
                    }
                }
                .onFailure { failure ->
                    mutableState.update { it.copy(backendStatus = "Backend configuration required", activeModel = null) }
                }
        }
    }

    companion object {
        fun factory(settings: BackendSettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(ChatRepository(settings), settings) as T
        }
    }
}
