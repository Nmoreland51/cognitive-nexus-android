package com.nmoreland.cognitivenexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nmoreland.cognitivenexus.data.BackendSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsUiState(val backendUrl: String = "", val saved: Boolean = false, val error: String? = null)

class SettingsViewModel(private val settings: BackendSettingsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init { viewModelScope.launch { settings.backendUrl.collectLatest { mutableState.value = SettingsUiState(backendUrl = it) } } }

    fun save(url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            mutableState.value = mutableState.value.copy(error = "Use a full URL beginning with http:// or https://.", saved = false)
            return
        }
        viewModelScope.launch {
            settings.saveBackendUrl(url)
            mutableState.value = mutableState.value.copy(saved = true, error = null)
        }
    }

    companion object {
        fun factory(settings: BackendSettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(settings) as T
        }
    }
}
