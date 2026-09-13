package com.nmoreland.cognitivenexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nmoreland.cognitivenexus.data.ChatDataSource
import com.nmoreland.cognitivenexus.data.ChatRepository
import com.nmoreland.cognitivenexus.model.ChatMessage
import com.nmoreland.cognitivenexus.model.MessageRole
import com.nmoreland.cognitivenexus.settings.SettingsDataSource
import com.nmoreland.cognitivenexus.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showSettings: Boolean = false,
    val savedBackendUrl: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val backendUrlDraft: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val healthStatus: String = "Health check not run yet.",
    val setupNotice: String = "Setup note: /api/models is not yet guaranteed by backend contract. This app uses a fixed model hint and surfaces backend setup issues explicitly."
)

class AppViewModel(
    private val settingsRepository: SettingsDataSource,
    private val chatRepository: ChatDataSource = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.backendUrl.collect { savedUrl ->
                _uiState.update { state ->
                    val shouldSyncDraft = state.backendUrlDraft == state.savedBackendUrl
                    state.copy(
                        savedBackendUrl = savedUrl,
                        backendUrlDraft = if (shouldSyncDraft) savedUrl else state.backendUrlDraft
                    )
                }
            }
        }
    }

    fun updateInput(input: String) {
        _uiState.update { it.copy(currentInput = input, errorMessage = null) }
    }

    fun updateBackendUrlDraft(url: String) {
        _uiState.update { it.copy(backendUrlDraft = url, errorMessage = null) }
    }

    fun saveBackendUrl() {
        viewModelScope.launch {
            settingsRepository.saveBackendUrl(_uiState.value.backendUrlDraft)
            _uiState.update {
                it.copy(
                    successMessage = "Backend URL saved.",
                    healthStatus = "Health check not run yet."
                )
            }
        }
    }

    fun sendMessage() {
        val input = _uiState.value.currentInput.trim()
        if (input.isEmpty() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(MessageRole.USER, input)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                currentInput = "",
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
        }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                baseUrl = _uiState.value.savedBackendUrl,
                message = input
            )

            result.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage(MessageRole.ASSISTANT, response.reply),
                            isLoading = false
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Backend error: ${throwable.message}"
                        )
                    }
                }
            )
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(healthStatus = "Checking backend…") }
            val result = chatRepository.checkHealth(_uiState.value.backendUrlDraft)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { response ->
                        state.copy(
                            healthStatus = "Health OK: ok=${response.ok}, model=${response.chat_model ?: "unknown"}"
                        )
                    },
                    onFailure = { throwable ->
                        state.copy(healthStatus = "Health check failed: ${throwable.message}")
                    }
                )
            }
        }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun showSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }
}

class AppViewModelFactory(
    private val settingsRepository: SettingsDataSource
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
