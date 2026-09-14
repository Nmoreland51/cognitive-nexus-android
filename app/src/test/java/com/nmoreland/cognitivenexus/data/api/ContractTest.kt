package com.nmoreland.cognitivenexus.data.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ContractTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    @Test fun requestMatchesPythonContract() {
        val request = JobRequest(session_id = "8b7fbfb9-93b0-4d62-9f2e-d78c7b8a6693", action = "chat", text = "hello")
        val body = json.parseToJsonElement(json.encodeToString(request)).jsonObject
        assertEquals("chat", body["action"]?.jsonPrimitive?.content)
        assertTrue(body.containsKey("request_id"))
        assertEquals("auto", body["options"]?.jsonObject?.get("provider")?.jsonPrimitive?.content)
    }
    @Test fun legacyHealthIsNotV2() {
        assertEquals(0, json.decodeFromString<HealthResponse>("""{"ok":true,"chat_model":"example"}""").api_version)
    }
    @Test fun failedJobHasNoAssistantResult() {
        val job = json.decodeFromString<JobResponse>("""{"id":"id","action":"chat","state":"failed","result":null,"error":"No model"}""")
        assertNull(job.result)
        assertEquals("No model", job.error)
    }
    @Test fun sessionsTolerateAdditionalServerFields() {
        val payload = json.decodeFromString<SessionsResponse>("""{"sessions":[{"id":"id","title":"Hello","message_count":2,"updated":3}]}""")
        assertEquals(2, payload.sessions.first().message_count)
    }
}
