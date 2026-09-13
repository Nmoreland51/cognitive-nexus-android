package com.nmoreland.cognitivenexus.network

object BackendUrlNormalizer {
    const val DEFAULT_DEV_BACKEND_URL: String = "http://10.0.2.2:8000/"

    fun normalize(url: String, fallback: String = DEFAULT_DEV_BACKEND_URL): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return fallback

        val root = Regex("^(https?://[^/]+)", RegexOption.IGNORE_CASE)
            .find(trimmed)
            ?.groupValues
            ?.getOrNull(1)

        return if (!root.isNullOrBlank()) "$root/" else fallback
    }
}
