package com.nmoreland.cognitivenexus.model

import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val role: MessageRole,
    val text: String,
    val id: String = UUID.randomUUID().toString()
)
