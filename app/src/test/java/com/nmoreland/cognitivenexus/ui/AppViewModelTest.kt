package com.nmoreland.cognitivenexus.ui

import com.nmoreland.cognitivenexus.data.ChatDataSource
import com.nmoreland.cognitivenexus.network.ChatResponse
import com.nmoreland.cognitivenexus.network.HealthResponse
import com.nmoreland.cognitivenexus.settings.SettingsDataSource
import com.nmoreland.cognitivenexus.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendMessage_success_appendsAssistantReply() = runTest {
        val settings = FakeSettingsDataSource()
        val chat = FakeChatDataSource(
            sendResult = Result.success(
                ChatResponse(reply = "Hello from backend", session_id = "android-session", model = "llama3.1:8b")
            )
        )
        val viewModel = AppViewModel(settings, chat)

        viewModel.updateInput("Hi")
        viewModel.sendMessage()

        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals("Hello from backend", state.messages.last().text)
    }

    @Test
    fun sendMessage_failure_setsErrorAndStopsLoading() = runTest {
        val settings = FakeSettingsDataSource()
        val chat = FakeChatDataSource(sendResult = Result.failure(IllegalStateException("HTTP 502")))
        val viewModel = AppViewModel(settings, chat)

        viewModel.updateInput("Hi")
        viewModel.sendMessage()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage?.contains("Backend error") == true)
    }

    @Test
    fun sendMessage_blankInput_doesNotCallRepository() = runTest {
        val settings = FakeSettingsDataSource()
        val chat = FakeChatDataSource(sendResult = Result.success(ChatResponse("ok", "s", "m")))
        val viewModel = AppViewModel(settings, chat)

        viewModel.updateInput("   ")
        viewModel.sendMessage()

        advanceUntilIdle()

        assertEquals(0, chat.sendCalls)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun sendMessage_whileLoading_preventsDuplicateSend() = runTest {
        val settings = FakeSettingsDataSource()
        val chat = FakeChatDataSource(
            sendResult = Result.success(ChatResponse("ok", "s", "m")),
            sendDelayMs = 1_000
        )
        val viewModel = AppViewModel(settings, chat)

        viewModel.updateInput("First")
        viewModel.sendMessage()
        viewModel.updateInput("Second")
        viewModel.sendMessage()

        advanceUntilIdle()

        assertEquals(1, chat.sendCalls)
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun checkHealth_success_updatesStatus() = runTest {
        val settings = FakeSettingsDataSource()
        val chat = FakeChatDataSource(
            healthResult = Result.success(HealthResponse(ok = true, chat_model = "llama3.1:8b")),
            sendResult = Result.success(ChatResponse("ok", "s", "m"))
        )
        val viewModel = AppViewModel(settings, chat)

        viewModel.checkHealth()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.healthStatus.contains("Health OK"))
    }

    @Test
    fun checkHealth_failure_updatesStatus() = runTest {
        val settings = FakeSettingsDataSource()
        val chat = FakeChatDataSource(
            healthResult = Result.failure(IllegalStateException("offline")),
            sendResult = Result.success(ChatResponse("ok", "s", "m"))
        )
        val viewModel = AppViewModel(settings, chat)

        viewModel.checkHealth()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.healthStatus.contains("Health check failed"))
    }

    @Test
    fun checkHealth_usesDraftBackendUrl() = runTest {
        val settings = FakeSettingsDataSource()
        val chat = FakeChatDataSource(sendResult = Result.success(ChatResponse("ok", "s", "m")))
        val viewModel = AppViewModel(settings, chat)

        viewModel.updateBackendUrlDraft("https://draft.example.com")
        viewModel.checkHealth()
        advanceUntilIdle()

        assertEquals("https://draft.example.com", chat.lastHealthBaseUrl)
    }
}

private class FakeSettingsDataSource : SettingsDataSource {
    private val state = MutableStateFlow(SettingsRepository.DEFAULT_BACKEND_URL)

    override val backendUrl: Flow<String> = state.asStateFlow()

    override suspend fun saveBackendUrl(url: String) {
        state.value = url
    }
}

private class FakeChatDataSource(
    private val healthResult: Result<HealthResponse> = Result.success(HealthResponse(ok = true)),
    private val sendResult: Result<ChatResponse>,
    private val sendDelayMs: Long = 0
) : ChatDataSource {
    var sendCalls: Int = 0
        private set
    var lastHealthBaseUrl: String? = null
        private set

    override suspend fun checkHealth(baseUrl: String): Result<HealthResponse> {
        lastHealthBaseUrl = baseUrl
        return healthResult
    }

    override suspend fun sendMessage(
        baseUrl: String,
        message: String,
        sessionId: String,
        model: String
    ): Result<ChatResponse> {
        sendCalls += 1
        if (sendDelayMs > 0) {
            delay(sendDelayMs)
        }
        return sendResult
    }
}
