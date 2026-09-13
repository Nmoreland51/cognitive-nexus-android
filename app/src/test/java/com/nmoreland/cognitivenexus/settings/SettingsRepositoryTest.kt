package com.nmoreland.cognitivenexus.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun normalizeBackendUrl_blank_usesDefault() {
        assertEquals(
            SettingsRepository.DEFAULT_BACKEND_URL,
            SettingsRepository.normalizeBackendUrl("   ")
        )
    }

    @Test
    fun normalizeBackendUrl_stripsTrailingApiSegment() {
        assertEquals(
            "http://10.0.2.2:8000/",
            SettingsRepository.normalizeBackendUrl("http://10.0.2.2:8000/api")
        )
    }

    @Test
    fun normalizeBackendUrl_addsTrailingSlash() {
        assertEquals(
            "https://example.com/",
            SettingsRepository.normalizeBackendUrl("https://example.com")
        )
    }

    @Test
    fun normalizeBackendUrl_dropsPathSegmentsToServerRoot() {
        assertEquals(
            "https://example.com/",
            SettingsRepository.normalizeBackendUrl("https://example.com/api/v1")
        )
    }
}
