package com.nmoreland.cognitivenexus.ui

import com.nmoreland.cognitivenexus.data.ChatDataSource
import com.nmoreland.cognitivenexus.network.ChatResponse
import com.nmoreland.cognitivenexus.network.HealthResponse
import com.nmoreland.cognitivenexus.settings.SettingsDataSource
import com.nmoreland.cognitivenexus.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val sendResult: Result<ChatResponse>
) : ChatDataSource {
    override suspend fun checkHealth(baseUrl: String): Result<HealthResponse> = healthResult

    override suspend fun sendMessage(
        baseUrl: String,
        message: String,
        sessionId: String,
        model: String
    ): Result<ChatResponse> = sendResult
}
