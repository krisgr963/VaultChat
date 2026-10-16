package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_messages")
data class EncryptedMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderPublicKey: String,
    val senderName: String,
    val isFromMe: Boolean,
    val ciphertextBase64: String,
    val ivBase64: String,
    val authTagBase64: String,
    val timestamp: Long,
    val expiresAt: Long?,
    val ephemeralTimerSeconds: Long
)

@Entity(tableName = "chat_threads")
data class ChatThreadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val remotePublicKeyBase64: String,
    val lastMessagePreview: String,
    val lastTimestamp: Long,
    val ephemeralTimerSeconds: Long,
    val isPinned: Boolean,
    val folderCategory: String,
    val isSafetyNumberVerified: Boolean
)
