package com.nmoreland.cognitivenexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nmoreland.cognitivenexus.data.*
import com.nmoreland.cognitivenexus.data.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.util.UUID

data class OperationState(val busy: Boolean = false, val status: String = "", val result: JsonObject? = null, val error: String? = null, val pendingId: String? = null)
data class NexusState(
    val backendUrl: String = DEFAULT_BACKEND_URL, val connected: Boolean = false,
    val connectionStatus: String = "Backend configuration required", val settingsMessage: String = "",
    val sessionId: String = "", val messages: List<Message> = emptyList(), val sessions: List<Session> = emptyList(),
    val options: EngineOptions = EngineOptions(), val darkTheme: Boolean = false,
    val operations: Map<String, OperationState> = emptyMap(),
)

class NexusViewModel(private val settings: BackendSettingsRepository) : ViewModel() {
    val repository = NexusRepository(settings)
    private val mutable = MutableStateFlow(NexusState())
    val state = mutable.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()
    private var connectionJob: Job? = null

    init {
        viewModelScope.launch { settings.options.collect { value -> mutable.update { it.copy(options = value) } } }
        viewModelScope.launch { settings.darkTheme.collect { value -> mutable.update { it.copy(darkTheme = value) } } }
        viewModelScope.launch {
            settings.connection.distinctUntilChanged().collectLatest { connection ->
                jobs.values.forEach { it.cancel() }; jobs.clear(); connectionJob?.cancel()
                val session = settings.session()
                mutable.update { NexusState(backendUrl = connection.url, options = it.options, sessionId = session) }
                refreshConnection()
            }
        }
    }

    private fun operation(route: String, value: OperationState) { mutable.update { it.copy(operations = it.operations + (route to value)) } }
    fun updateOptions(options: EngineOptions) {
        mutable.update { it.copy(options = options) }
        viewModelScope.launch { try { settings.saveOptions(options) } catch (e: Exception) { if (e is CancellationException) throw e; mutable.update { it.copy(settingsMessage = readableError(e)) } } }
    }
    fun setDarkTheme(enabled: Boolean) {
        mutable.update { it.copy(darkTheme = enabled) }
        viewModelScope.launch {
            try { settings.saveDarkTheme(enabled) }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(settingsMessage = readableError(e)) }
            }
        }
    }
    fun saveConnection(url: String, token: String) {
        viewModelScope.launch {
            try {
                settings.saveConnection(url, token)
                mutable.update { it.copy(settingsMessage = "Connection saved. Checking Mobile API v2…") }
                refreshConnection()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(settingsMessage = readableError(e)) }
            }
        }
    }
    fun refreshConnection() {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            mutable.update { it.copy(connectionStatus = "Checking backend…", connected = false) }
            try {
                val api = repository.api()
                val health = api.health()
                check(health.ok && health.api_version == 2) { "This is the old backend. Start the Mobile API v2 adapter included in this repository." }
                val session = settings.session()
                val messages = api.messages(session).messages
                val sessions = api.sessions().sessions
                mutable.update { it.copy(connected = true, connectionStatus = "Mobile API v2 connected", sessionId = session, messages = messages, sessions = sessions) }
                val pending = settings.pending()
                pending.forEach { (route, id) -> resume(route, id) }
                if (!pending.containsKey("overview")) run("overview", "overview")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(connected = false, connectionStatus = readableError(e)) }
            }
        }
    }
    fun run(route: String, action: String, text: String = "", name: String = "", tags: String = "") {
        if (jobs[route]?.isActive == true) return
        val request = JobRequest(session_id = state.value.sessionId.ifBlank { UUID.randomUUID().toString() }, action = action,
            text = text, name = name, tags = tags, options = state.value.options)
        operation(route, OperationState(busy = true, status = "Submitting $action…", pendingId = request.request_id))
        jobs[route] = viewModelScope.launch {
            try {
                val api = repository.api()
                settings.setPending(route, request.request_id)
                api.submit(request)
                poll(route, request.request_id, api)
            } catch (e: Exception) { failure(route, request.request_id, e) }
        }
    }
    fun resume(route: String, id: String) {
        if (jobs[route]?.isActive == true) return
        operation(route, OperationState(busy = true, status = "Reconnecting to saved task…", pendingId = id))
        jobs[route] = viewModelScope.launch {
            try { poll(route, id, repository.api()) } catch (e: Exception) { failure(route, id, e) }
        }
    }
    private suspend fun poll(route: String, id: String, api: CognitiveNexusApi) {
        while (currentCoroutineContext().isActive) {
            val job = api.job(id)
            if (job.state == "succeeded" || job.state == "failed") {
                settings.setPending(route, null)
                operation(route, OperationState(status = if (job.state == "succeeded") "Completed: ${job.action}" else "Failed: ${job.action}", result = job.result, error = job.error))
                if (job.action == "chat" && job.state == "succeeded") {
                    val messages = api.messages(state.value.sessionId).messages
                    val sessions = api.sessions().sessions
                    mutable.update { it.copy(messages = messages, sessions = sessions) }
                }
                return
            }
            operation(route, OperationState(busy = true, status = "${job.action}: ${job.state}. This runs on your backend.", pendingId = id))
            delay(1500)
        }
    }
    private suspend fun failure(route: String, id: String, e: Exception) {
        if (e is CancellationException) throw e
        val missing = e is retrofit2.HttpException && e.code() in listOf(404, 409, 413, 422, 429)
        if (missing) settings.setPending(route, null)
        operation(route, OperationState(error = readableError(e), pendingId = if (missing) null else id))
    }
    fun selectSession(id: String = UUID.randomUUID().toString()) {
        if (state.value.operations["chat"]?.busy == true) return
        viewModelScope.launch {
            try {
                val messages = repository.api().messages(id).messages
                settings.setSession(id)
                mutable.update { it.copy(sessionId = id, messages = messages) }
                operation("chat", OperationState())
            } catch (e: Exception) { if (e is CancellationException) throw e; operation("chat", OperationState(error = readableError(e))) }
        }
    }
    fun inputError(route: String, message: String) { operation(route, OperationState(error = message)) }

    companion object {
        fun factory(settings: BackendSettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = NexusViewModel(settings) as T
        }
    }
}
