package com.nmoreland.cognitivenexus.model

enum class MessageRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val role: MessageRole,
    val text: String
)
