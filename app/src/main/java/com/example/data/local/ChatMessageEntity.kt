package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.util.DatabaseEncryptionAndSecurityManager
import com.example.util.SecuritySanitizer

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val isUser: Boolean,
    val content: String,
    val timestamp: String,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val quickSuggestionsRaw: String = "",
    val hasPhotoAttached: Boolean = false
) {
    fun toChatMessage(): ChatMessage {
        val aadPrefix = "local_room_db:chat_messages:$id"
        val decryptedContent = if (DatabaseEncryptionAndSecurityManager.isEncryptedPayload(content)) {
            try {
                DatabaseEncryptionAndSecurityManager.decryptSensitiveField(content, "$aadPrefix:content")
            } catch (_: Exception) {
                ""
            }
        } else {
            content
        }
        val decryptedSuggestionsRaw = if (DatabaseEncryptionAndSecurityManager.isEncryptedPayload(quickSuggestionsRaw)) {
            try {
                DatabaseEncryptionAndSecurityManager.decryptSensitiveField(quickSuggestionsRaw, "$aadPrefix:suggestions")
            } catch (_: Exception) {
                ""
            }
        } else {
            quickSuggestionsRaw
        }
        val suggestions = if (decryptedSuggestionsRaw.isNotBlank()) {
            decryptedSuggestionsRaw.split("||").filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        return ChatMessage(
            id = id,
            isUser = isUser,
            content = decryptedContent,
            timestamp = timestamp,
            isStreaming = false,
            quickSuggestions = suggestions,
            attachedBitmap = null
        )
    }

    companion object {
        fun fromChatMessage(chatMessage: ChatMessage): ChatMessageEntity {
            val cleanId = SecuritySanitizer.sanitizeText(chatMessage.id, maxLength = 64)
            val cleanContent = SecuritySanitizer.sanitizeRichDescription(
                chatMessage.content,
                maxLength = SecuritySanitizer.MAX_CHAT_MESSAGE_LENGTH
            )
            val cleanSuggestions = chatMessage.quickSuggestions
                .map { SecuritySanitizer.sanitizeText(it, maxLength = 120) }
                .joinToString("||")

            val aadPrefix = "local_room_db:chat_messages:$cleanId"
            val encryptedContent = DatabaseEncryptionAndSecurityManager.encryptSensitiveField(
                plaintext = cleanContent,
                associatedData = "$aadPrefix:content"
            )
            val encryptedSuggestions = if (cleanSuggestions.isNotBlank()) {
                DatabaseEncryptionAndSecurityManager.encryptSensitiveField(
                    plaintext = cleanSuggestions,
                    associatedData = "$aadPrefix:suggestions"
                )
            } else {
                ""
            }

            return ChatMessageEntity(
                id = cleanId,
                isUser = chatMessage.isUser,
                content = encryptedContent,
                timestamp = SecuritySanitizer.sanitizeText(chatMessage.timestamp, maxLength = 40),
                quickSuggestionsRaw = encryptedSuggestions,
                hasPhotoAttached = chatMessage.attachedBitmap != null
            )
        }
    }
}

