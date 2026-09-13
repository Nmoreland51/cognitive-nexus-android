package com.nmoreland.cognitivenexus.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsDataSource {
    val backendUrl: Flow<String>
    suspend fun saveBackendUrl(url: String)
}


private val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) : SettingsDataSource {
    private val backendUrlKey = stringPreferencesKey("backend_url")

    override val backendUrl: Flow<String> = context.dataStore.data.map { preferences ->
        normalizeUrl(preferences[backendUrlKey] ?: DEFAULT_BACKEND_URL)
    }

    override suspend fun saveBackendUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[backendUrlKey] = normalizeUrl(url)
        }
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim().ifEmpty { DEFAULT_BACKEND_URL }
        normalized = normalized.removeSuffix("/")
        if (normalized.endsWith("/api")) {
            normalized = normalized.removeSuffix("/api")
        }
        return "$normalized/"
    }

    companion object {
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000/"
    }
}
