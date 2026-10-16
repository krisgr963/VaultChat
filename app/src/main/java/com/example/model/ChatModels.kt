package com.example.model

enum class UserMode {
    ANONYMOUS,
    PROFILED
}

enum class EphemeralTimer(val seconds: Long, val label: String) {
    OFF(0L, "Av (Ingen timer)"),
    FIVE_SECONDS(5L, "5 sekunder"),
    THIRTY_SECONDS(30L, "30 sekunder"),
    ONE_MINUTE(60L, "1 minutt"),
    ONE_HOUR(3600L, "1 time"),
    TWENTY_FOUR_HOURS(86400L, "24 timer")
}

enum class EphemeralTrigger(val label: String) {
    ON_SEND("Ved sending"),
    ON_READ("Ved mottak/lesing")
}

enum class DuressAction(val label: String) {
    OPEN_DECOY_MODE("Åpne decoy/falsk profil (skjult nødvern)"),
    STEALTH_WIPE_AND_RESET("Utfør skjult øyeblikkelig sletting")
}

data class UserAccount(
    val id: String,
    val mode: UserMode,
    val username: String? = null,
    val publicKeyBase64: String,
    val privateKeyBase64: String,
    val salt: String,
    val pinHash: String? = null,
    val duressPinHash: String? = null,
    val duressAction: DuressAction = DuressAction.OPEN_DECOY_MODE,
    val createdAt: Long = System.currentTimeMillis(),
    val isDecoy: Boolean = false
)

data class ChatConversation(
    val id: String,
    val title: String,
    val remotePublicKeyBase64: String,
    val lastMessagePreview: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val ephemeralTimer: EphemeralTimer = EphemeralTimer.OFF,
    val ephemeralTrigger: EphemeralTrigger = EphemeralTrigger.ON_READ,
    val isPinned: Boolean = false,
    val folderCategory: String = "Innboks",
    val unreadCount: Int = 0,
    val isSafetyNumberVerified: Boolean = false,
    val isDecoy: Boolean = false
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderPublicKey: String,
    val senderName: String,
    val isFromMe: Boolean,
    val plaintext: String,
    val ciphertextBase64: String,
    val ivBase64: String,
    val authTagBase64: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sentAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null,
    val expiresAt: Long? = null,
    val ephemeralTimer: EphemeralTimer = EphemeralTimer.OFF,
    val ephemeralTrigger: EphemeralTrigger = EphemeralTrigger.ON_READ,
    val isBurned: Boolean = false
)

data class SecuritySettings(
    val flagSecureEnabled: Boolean = true,
    val clipboardAutoClearEnabled: Boolean = true,
    val clipboardClearSeconds: Int = 30,
    val autoLockSeconds: Int = 60,
    val duressAction: DuressAction = DuressAction.OPEN_DECOY_MODE
)
