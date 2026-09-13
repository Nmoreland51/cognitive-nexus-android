package com.nmoreland.cognitivenexus.network

import java.net.URI

object BackendUrlNormalizer {
    const val DEFAULT_DEV_BACKEND_URL: String = "http://10.0.2.2:8000/"

    fun normalize(url: String, fallback: String = DEFAULT_DEV_BACKEND_URL): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return fallback

        return runCatching {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase() ?: return@runCatching fallback
            if (scheme != "http" && scheme != "https") return@runCatching fallback
            val authority = uri.rawAuthority ?: return@runCatching fallback

            var path = (uri.rawPath ?: "").trim()
            path = path.removeSuffix("/")
            if (path.endsWith("/api")) {
                path = path.removeSuffix("/api")
            }
            if (path.isNotEmpty() && !path.startsWith("/")) {
                path = "/$path"
            }

            "$scheme://$authority$path/"
        }.getOrElse { fallback }
    }
}
