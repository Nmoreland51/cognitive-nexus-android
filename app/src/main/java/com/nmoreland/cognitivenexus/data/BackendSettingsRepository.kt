package com.nmoreland.cognitivenexus.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nmoreland.cognitivenexus.BuildConfig
import com.nmoreland.cognitivenexus.data.api.EngineOptions
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID

const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000/"
private val Context.backendSettingsDataStore by preferencesDataStore(name = "backend_settings")
data class Connection(val url: String, val token: String)

class BackendSettingsRepository(context: Context) {
    private val store = context.applicationContext.backendSettingsDataStore
    private val vault = TokenVault()
    private val urlKey = stringPreferencesKey("backend_url")
    private val tokenKey = stringPreferencesKey("encrypted_token")
    private val optionsKey = stringPreferencesKey("engine_options")
    private val sessionKey = stringPreferencesKey("mobile_session")
    private val pendingKey = stringPreferencesKey("pending_jobs")
    private val json = Json { ignoreUnknownKeys = true }
    val connection = store.data.map { Connection(it[urlKey] ?: DEFAULT_BACKEND_URL, vault.decrypt(it[tokenKey] ?: "")) }
    val options = store.data.map { prefs -> runCatching { json.decodeFromString<EngineOptions>(prefs[optionsKey] ?: "{}") }.getOrDefault(EngineOptions()) }
    suspend fun saveConnection(input: String, token: String) {
        val url = input.trim().toHttpUrlOrNull() ?: error("Enter a valid http:// or https:// backend address.")
        require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "Do not put credentials, queries, or fragments in the backend URL." }
        require(BuildConfig.DEBUG || url.isHttps || isPrivateLanHttp(url.host)) {
            "Release builds require HTTPS, except for a private LAN IPv4 backend."
        }
        val normalized = url.toString().let { if (it.endsWith('/')) it else "$it/" }
        require(token.isBlank() || (token.length >= 32 && token.all { it.code in 33..126 })) { "The access token must have at least 32 ASCII characters, without spaces." }
        val encrypted = if (token.isNotBlank()) vault.encrypt(token.trim()) else null
        store.edit {
            if ((it[urlKey] ?: DEFAULT_BACKEND_URL) != normalized) {
                it.remove(tokenKey); it.remove(pendingKey); it.remove(sessionKey)
            }
            it[urlKey] = normalized
            if (encrypted != null) it[tokenKey] = encrypted
        }
    }

    /**
     * A signed app may use HTTP only for a numeric address on a private/home
     * network. Public servers must use HTTPS; hostnames are not resolved here
     * so a public name cannot be smuggled through this local-development path.
     */
    private fun isPrivateLanHttp(host: String): Boolean {
        val octets = host.split('.').map { it.toIntOrNull() ?: return false }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168) ||
            octets[0] == 127
    }
    suspend fun saveOptions(value: EngineOptions) { store.edit { it[optionsKey] = json.encodeToString(value) } }
    suspend fun session(): String {
        val current = store.data.first()[sessionKey]
        if (current != null) return current
        return UUID.randomUUID().toString().also { setSession(it) }
    }
    suspend fun setSession(value: String) { store.edit { it[sessionKey] = value } }
    suspend fun pending(): Map<String, String> = runCatching {
        json.decodeFromString<Map<String, String>>(store.data.first()[pendingKey] ?: "{}")
    }.getOrDefault(emptyMap())
    suspend fun setPending(route: String, id: String?) {
        store.edit {
            val values = runCatching { json.decodeFromString<Map<String,String>>(it[pendingKey] ?: "{}") }.getOrDefault(emptyMap()).toMutableMap()
            if (id == null) values.remove(route) else values[route] = id
            it[pendingKey] = json.encodeToString(values)
        }
    }
}
