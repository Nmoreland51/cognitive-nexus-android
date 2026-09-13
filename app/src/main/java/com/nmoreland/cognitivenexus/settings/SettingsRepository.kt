package com.nmoreland.cognitivenexus.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) {
    private val backendUrlKey = stringPreferencesKey("backend_url")

    val backendUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[backendUrlKey] ?: DEFAULT_BACKEND_URL
    }

    suspend fun saveBackendUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[backendUrlKey] = normalizeUrl(url)
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim().ifEmpty { DEFAULT_BACKEND_URL }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    companion object {
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000/"
    }
}
