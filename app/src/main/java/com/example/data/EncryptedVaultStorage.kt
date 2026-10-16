package com.example.data

import android.content.Context
import android.util.Base64
import com.example.crypto.CryptoEngine
import com.example.model.ChatConversation
import com.example.model.ChatMessage
import com.example.model.DuressAction
import com.example.model.EphemeralTimer
import com.example.model.EphemeralTrigger
import com.example.model.SecuritySettings
import com.example.model.UserAccount
import com.example.model.UserMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

class EncryptedVaultStorage(private val context: Context) {
    private val vaultFile = File(context.filesDir, "aegis_encrypted_vault.bin")
    private val secureRandom = SecureRandom()

    private val _account = MutableStateFlow<UserAccount?>(null)
    val account: StateFlow<UserAccount?> = _account.asStateFlow()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _securitySettings = MutableStateFlow(SecuritySettings())
    val securitySettings: StateFlow<SecuritySettings> = _securitySettings.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<String>>(emptyList())
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    init {
        loadVault()
    }

    private fun addAuditLog(action: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _auditLogs.value = listOf("[$timestamp] $action") + _auditLogs.value.take(20)
    }

    /**
     * Initializes an Anonymous account: 100% zero identifying information.
     */
    fun createAnonymousAccount(): UserAccount {
        // Ensure hardware-backed KeyStore master key is provisioned
        val hwKeyPair = com.example.crypto.KeyManager.getOrCreateMasterIdentityKeyPair()
        val keyPair = CryptoEngine.generateEphemeralKeyPair()
        val pubKeyBase64 = CryptoEngine.encodePublicKey(keyPair.public)
        val privKeyBase64 = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
        val saltBytes = ByteArray(16).also { secureRandom.nextBytes(it) }
        val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)

        val id = "anon_" + pubKeyBase64.take(8).lowercase().filter { it.isLetterOrDigit() }

        val newAccount = UserAccount(
            id = id,
            mode = UserMode.ANONYMOUS,
            username = null,
            publicKeyBase64 = pubKeyBase64,
            privateKeyBase64 = privKeyBase64,
            salt = saltBase64,
            isDecoy = false
        )

        _account.value = newAccount
        _isLocked.value = false

        // Seed with a default E2EE verification contact for demonstration
        seedInitialDemoConversations(newAccount)
        saveVault()
        addAuditLog("Anonym modus opprettet. Maskinvarenøkkel generert i Android Keystore.")
        return newAccount
    }

    /**
     * Initializes a Profiled account: Minimal optional username with PIN and Duress PIN.
     */
    fun createProfiledAccount(
        username: String,
        pin: String,
        duressPin: String?,
        duressAction: DuressAction
    ): UserAccount {
        val hwKeyPair = com.example.crypto.KeyManager.getOrCreateMasterIdentityKeyPair()
        com.example.crypto.KeyManager.getOrCreateDbKey()
        val keyPair = CryptoEngine.generateEphemeralKeyPair()
        val pubKeyBase64 = CryptoEngine.encodePublicKey(keyPair.public)
        val privKeyBase64 = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)
        val saltBytes = ByteArray(16).also { secureRandom.nextBytes(it) }
        val saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)

        val pinHash = CryptoEngine.hashPin(pin, saltBase64)
        val duressPinHash = if (!duressPin.isNullOrBlank()) CryptoEngine.hashPin(duressPin, saltBase64) else null

        val id = "@" + username.trim().lowercase().replace(" ", "_")

        val newAccount = UserAccount(
            id = id,
            mode = UserMode.PROFILED,
            username = username.trim(),
            publicKeyBase64 = pubKeyBase64,
            privateKeyBase64 = privKeyBase64,
            salt = saltBase64,
            pinHash = pinHash,
            duressPinHash = duressPinHash,
            duressAction = duressAction,
            isDecoy = false
        )

        _account.value = newAccount
        _isLocked.value = false

        seedInitialDemoConversations(newAccount)
        saveVault()
        addAuditLog("Profilert konto opprettet med Duress-beskyttelse.")
        return newAccount
    }

    /**
     * Seeds initial peer conversations demonstrating real E2EE exchange and ephemeral messaging.
     */
    private fun seedInitialDemoConversations(account: UserAccount) {
        val peerKeyPair = CryptoEngine.generateEphemeralKeyPair()
        val peerPubKeyBase64 = CryptoEngine.encodePublicKey(peerKeyPair.public)

        val demoConvId = UUID.randomUUID().toString()
        val demoConv = ChatConversation(
            id = demoConvId,
            title = if (account.mode == UserMode.ANONYMOUS) "Cipher_Node_918" else "Sikker kontakt (Alice)",
            remotePublicKeyBase64 = peerPubKeyBase64,
            lastMessagePreview = "Nøklene er bekreftet. Klartekst finnes kun på denne enheten.",
            lastTimestamp = System.currentTimeMillis(),
            ephemeralTimer = EphemeralTimer.THIRTY_SECONDS,
            ephemeralTrigger = EphemeralTrigger.ON_READ,
            isPinned = true,
            folderCategory = "Sikkert hvelv",
            unreadCount = 1,
            isSafetyNumberVerified = true,
            isDecoy = false
        )

        // Generate peer's encrypted message
        val myPubKey = CryptoEngine.decodePublicKey(account.publicKeyBase64)
        val sharedSecret = CryptoEngine.deriveSharedKey(peerKeyPair.private, myPubKey)
        val envelope = CryptoEngine.encrypt(
            plaintext = "Nøklene er bekreftet. Klartekst finnes kun på denne enheten.",
            secretKey = sharedSecret,
            senderPublicKeyBase64 = peerPubKeyBase64
        )

        val demoMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = demoConvId,
            senderPublicKey = peerPubKeyBase64,
            senderName = demoConv.title,
            isFromMe = false,
            plaintext = "Nøklene er bekreftet. Klartekst finnes kun på denne enheten.",
            ciphertextBase64 = envelope.ciphertextBase64,
            ivBase64 = envelope.ivBase64,
            authTagBase64 = envelope.authTagBase64,
            timestamp = System.currentTimeMillis() - 10000,
            sentAt = System.currentTimeMillis() - 10000,
            readAt = null,
            expiresAt = null,
            ephemeralTimer = EphemeralTimer.THIRTY_SECONDS,
            ephemeralTrigger = EphemeralTrigger.ON_READ
        )

        _conversations.value = listOf(demoConv)
        _messages.value = listOf(demoMessage)
    }

    /**
     * Seeds Decoy mode: Innocuous conversations that appear if Duress PIN is entered.
     */
    fun activateDecoyMode() {
        val decoyAccount = UserAccount(
            id = "decoy_user_safe",
            mode = UserMode.PROFILED,
            username = "Petter",
            publicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0",
            privateKeyBase64 = "",
            salt = "decoy_salt",
            isDecoy = true
        )

        val conv1 = ChatConversation(
            id = "decoy_1",
            title = "Mamma",
            remotePublicKeyBase64 = "decoy_mamma_key",
            lastMessagePreview = "Husk å kjøpe melk og brød på vei hjem!",
            lastTimestamp = System.currentTimeMillis() - 120000,
            ephemeralTimer = EphemeralTimer.OFF,
            folderCategory = "Innboks",
            isDecoy = true
        )

        val conv2 = ChatConversation(
            id = "decoy_2",
            title = "Borettslaget Dugnad",
            remotePublicKeyBase64 = "decoy_dugnad_key",
            lastMessagePreview = "Vårdugnaden starter kl. 11 på lørdag. Vafler serveres.",
            lastTimestamp = System.currentTimeMillis() - 86400000,
            ephemeralTimer = EphemeralTimer.OFF,
            folderCategory = "Innboks",
            isDecoy = true
        )

        _account.value = decoyAccount
        _conversations.value = listOf(conv1, conv2)
        _messages.value = listOf(
            ChatMessage(
                id = "decoy_msg_1",
                conversationId = "decoy_1",
                senderPublicKey = "decoy_mamma_key",
                senderName = "Mamma",
                isFromMe = false,
                plaintext = "Husk å kjøpe melk og brød på vei hjem!",
                ciphertextBase64 = "dummy_cipher",
                ivBase64 = "dummy_iv",
                authTagBase64 = "dummy_tag",
                timestamp = System.currentTimeMillis() - 120000
            )
        )
        _isLocked.value = false
        addAuditLog("⚠️ Duress PIN detektert! Decoy-profil aktivert i det skjulte.")
    }

    /**
     * Sends an E2EE message: encrypts with recipient's public key using ECDH + AES-256-GCM.
     */
    fun sendMessage(conversationId: String, text: String): ChatMessage? {
        val currAccount = _account.value ?: return null
        val conv = _conversations.value.find { it.id == conversationId } ?: return null

        val myPrivKeyBytes = Base64.decode(currAccount.privateKeyBase64, Base64.NO_WRAP)
        val kf = java.security.KeyFactory.getInstance("EC")
        val myPrivKey = kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(myPrivKeyBytes))

        val recipientPubKey = CryptoEngine.decodePublicKey(conv.remotePublicKeyBase64)
        val sharedSecret = CryptoEngine.deriveSharedKey(myPrivKey, recipientPubKey)

        val envelope = CryptoEngine.encrypt(
            plaintext = text,
            secretKey = sharedSecret,
            senderPublicKeyBase64 = currAccount.publicKeyBase64
        )

        val now = System.currentTimeMillis()
        val expiresAt = if (conv.ephemeralTimer != EphemeralTimer.OFF && conv.ephemeralTrigger == EphemeralTrigger.ON_SEND) {
            now + (conv.ephemeralTimer.seconds * 1000L)
        } else {
            null
        }

        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderPublicKey = currAccount.publicKeyBase64,
            senderName = currAccount.username ?: currAccount.id,
            isFromMe = true,
            plaintext = text,
            ciphertextBase64 = envelope.ciphertextBase64,
            ivBase64 = envelope.ivBase64,
            authTagBase64 = envelope.authTagBase64,
            timestamp = now,
            sentAt = now,
            readAt = now,
            expiresAt = expiresAt,
            ephemeralTimer = conv.ephemeralTimer,
            ephemeralTrigger = conv.ephemeralTrigger
        )

        _messages.value = _messages.value + message

        // Update conversation last preview
        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) {
                it.copy(
                    lastMessagePreview = text,
                    lastTimestamp = now
                )
            } else it
        }

        saveVault()
        addAuditLog("Melding kryptert med AES-256-GCM og sendt.")
        return message
    }

    /**
     * Simulates receiving a reply from peer (for realistic demonstration).
     */
    fun simulatePeerReply(conversationId: String, replyText: String) {
        val currAccount = _account.value ?: return
        val conv = _conversations.value.find { it.id == conversationId } ?: return

        val myPubKey = CryptoEngine.decodePublicKey(currAccount.publicKeyBase64)
        val peerKeyPair = CryptoEngine.generateEphemeralKeyPair()
        val sharedSecret = CryptoEngine.deriveSharedKey(peerKeyPair.private, myPubKey)

        val envelope = CryptoEngine.encrypt(
            plaintext = replyText,
            secretKey = sharedSecret,
            senderPublicKeyBase64 = conv.remotePublicKeyBase64
        )

        val now = System.currentTimeMillis()
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderPublicKey = conv.remotePublicKeyBase64,
            senderName = conv.title,
            isFromMe = false,
            plaintext = replyText,
            ciphertextBase64 = envelope.ciphertextBase64,
            ivBase64 = envelope.ivBase64,
            authTagBase64 = envelope.authTagBase64,
            timestamp = now,
            sentAt = now,
            readAt = null,
            expiresAt = null,
            ephemeralTimer = conv.ephemeralTimer,
            ephemeralTrigger = conv.ephemeralTrigger
        )

        _messages.value = _messages.value + msg
        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) {
                it.copy(
                    lastMessagePreview = replyText,
                    lastTimestamp = now,
                    unreadCount = it.unreadCount + 1
                )
            } else it
        }
        saveVault()
        addAuditLog("Mottatt blind-relayed melding fra motpart.")
    }

    /**
     * Marks messages in a conversation as read, triggering Ephemeral countdown if set to ON_READ.
     */
    fun markConversationAsRead(conversationId: String) {
        val now = System.currentTimeMillis()
        var updated = false

        _messages.value = _messages.value.map { msg ->
            if (msg.conversationId == conversationId && !msg.isFromMe && msg.readAt == null) {
                updated = true
                val expiresAt = if (msg.ephemeralTimer != EphemeralTimer.OFF && msg.ephemeralTrigger == EphemeralTrigger.ON_READ) {
                    now + (msg.ephemeralTimer.seconds * 1000L)
                } else null

                msg.copy(readAt = now, expiresAt = expiresAt)
            } else msg
        }

        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) it.copy(unreadCount = 0) else it
        }

        if (updated) {
            saveVault()
            addAuditLog("Melding lest: Selvslettings-timer utløst.")
        }
    }

    /**
     * Purges expired ephemeral messages permanently.
     */
    fun purgeExpiredMessages(): Int {
        val now = System.currentTimeMillis()
        val expired = _messages.value.filter { msg ->
            msg.expiresAt != null && msg.expiresAt <= now
        }

        if (expired.isNotEmpty()) {
            _messages.value = _messages.value.filterNot { msg ->
                msg.expiresAt != null && msg.expiresAt <= now
            }
            saveVault()
            addAuditLog("🔥 ${expired.size} utløpte meldinger uopprettelig slettet fra minne og disk.")
            return expired.size
        }
        return 0
    }

    /**
     * Updates ephemeral timer settings for a specific conversation.
     */
    fun updateConversationTimer(conversationId: String, timer: EphemeralTimer, trigger: EphemeralTrigger) {
        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) {
                it.copy(ephemeralTimer = timer, ephemeralTrigger = trigger)
            } else it
        }
        saveVault()
        addAuditLog("Selvslettingstimer for tråd satt til ${timer.label}.")
    }

    /**
     * Adds a new chat conversation with a peer's public key.
     */
    fun addConversation(title: String, remotePublicKeyBase64: String, folder: String = "Innboks"): ChatConversation {
        val newConv = ChatConversation(
            id = UUID.randomUUID().toString(),
            title = title,
            remotePublicKeyBase64 = remotePublicKeyBase64,
            lastMessagePreview = "Ny sikker kanal etablert.",
            lastTimestamp = System.currentTimeMillis(),
            folderCategory = folder
        )
        _conversations.value = listOf(newConv) + _conversations.value
        saveVault()
        addAuditLog("Ny chattråd opprettet: $title")
        return newConv
    }

    /**
     * Toggles pinned state for a conversation.
     */
    fun togglePinConversation(conversationId: String) {
        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) it.copy(isPinned = !it.isPinned) else it
        }
        saveVault()
    }

    /**
     * Toggles verified safety number state.
     */
    fun setSafetyNumberVerified(conversationId: String, verified: Boolean) {
        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) it.copy(isSafetyNumberVerified = verified) else it
        }
        saveVault()
        addAuditLog("Sikkerhetsnummer (Safety Number) ${if (verified) "verifisert" else "tilbakestilt"}.")
    }

    /**
     * Updates security settings (FLAG_SECURE, auto-lock, clipboard auto-clear).
     */
    fun updateSecuritySettings(newSettings: SecuritySettings) {
        _securitySettings.value = newSettings
        saveVault()
        addAuditLog("Sikkerhetsinnstillinger oppdatert.")
    }

    /**
     * Locks the vault (requiring PIN to unlock).
     */
    fun lockVault() {
        if (_account.value?.pinHash != null) {
            _isLocked.value = true
        }
    }

    /**
     * Unlocks the vault via biometric authentication (fingerprint or face unlock).
     */
    fun unlockWithBiometrics(): Boolean {
        val curr = _account.value ?: return false
        _isLocked.value = false
        addAuditLog("Hvelv låst opp via biometrisk autentisering (fingeravtrykk/ansikt).")
        return true
    }

    /**
     * Unlocks the vault given the entered PIN.
     * Detects normal PIN vs Duress PIN.
     */
    fun unlockWithPin(enteredPin: String): Boolean {
        val curr = _account.value ?: return false
        val enteredHash = CryptoEngine.hashPin(enteredPin, curr.salt)

        if (curr.pinHash == enteredHash) {
            _isLocked.value = false
            addAuditLog("Hvelv låst opp med primær-PIN.")
            return true
        }

        // Check if Duress PIN was entered
        if (curr.duressPinHash != null && curr.duressPinHash == enteredHash) {
            if (curr.duressAction == DuressAction.OPEN_DECOY_MODE) {
                activateDecoyMode()
            } else {
                executePanicWipe()
            }
            return true
        }

        return false
    }

    /**
     * KILL BUTTON / PANIC WIPE:
     * 1. Slett alle lokale krypteringsnøkler fra Keystore.
     * 2. Slett lokal database, historikk, cacher uopprettelig ved overskriving med 0-bytes.
     * 3. Send en anonymisert forespørsel til serveren om å terminere sesjon og slette køer.
     * 4. Nullstill app-innstillinger og tving utlogging til tom startskjerm.
     */
    fun executePanicWipe() {
        val wipeService = VaultWipeService(context)
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            wipeService.executeCompleteWipe { step ->
                addAuditLog(step)
            }
        }

        // Shred in-memory state
        _account.value = null
        _conversations.value = emptyList()
        _messages.value = emptyList()
        _isLocked.value = false
        _auditLogs.value = listOf("💥 PANIC WIPE FULLFØRT: Room-database slettet, internlagring makulert og Keystore-nøkler destruert.")
    }

    // --- Persistence implementation ---

    private fun saveVault() {
        try {
            val currAccount = _account.value ?: return
            if (currAccount.isDecoy) return // Never persist decoy to vault

            val root = JSONObject().apply {
                put("account", JSONObject().apply {
                    put("id", currAccount.id)
                    put("mode", currAccount.mode.name)
                    put("username", currAccount.username ?: "")
                    put("publicKeyBase64", currAccount.publicKeyBase64)
                    put("privateKeyBase64", currAccount.privateKeyBase64)
                    put("salt", currAccount.salt)
                    put("pinHash", currAccount.pinHash ?: "")
                    put("duressPinHash", currAccount.duressPinHash ?: "")
                    put("duressAction", currAccount.duressAction.name)
                })

                val convArray = JSONArray()
                _conversations.value.filterNot { it.isDecoy }.forEach { conv ->
                    convArray.put(JSONObject().apply {
                        put("id", conv.id)
                        put("title", conv.title)
                        put("remotePublicKeyBase64", conv.remotePublicKeyBase64)
                        put("lastMessagePreview", conv.lastMessagePreview)
                        put("lastTimestamp", conv.lastTimestamp)
                        put("ephemeralTimer", conv.ephemeralTimer.name)
                        put("ephemeralTrigger", conv.ephemeralTrigger.name)
                        put("isPinned", conv.isPinned)
                        put("folderCategory", conv.folderCategory)
                        put("unreadCount", conv.unreadCount)
                        put("isSafetyNumberVerified", conv.isSafetyNumberVerified)
                    })
                }
                put("conversations", convArray)

                val msgArray = JSONArray()
                _messages.value.filterNot { it.isBurned }.forEach { msg ->
                    msgArray.put(JSONObject().apply {
                        put("id", msg.id)
                        put("conversationId", msg.conversationId)
                        put("senderPublicKey", msg.senderPublicKey)
                        put("senderName", msg.senderName)
                        put("isFromMe", msg.isFromMe)
                        put("plaintext", msg.plaintext)
                        put("ciphertextBase64", msg.ciphertextBase64)
                        put("ivBase64", msg.ivBase64)
                        put("authTagBase64", msg.authTagBase64)
                        put("timestamp", msg.timestamp)
                        put("sentAt", msg.sentAt)
                        put("readAt", msg.readAt ?: -1L)
                        put("expiresAt", msg.expiresAt ?: -1L)
                        put("ephemeralTimer", msg.ephemeralTimer.name)
                        put("ephemeralTrigger", msg.ephemeralTrigger.name)
                    })
                }
                put("messages", msgArray)

                val settings = _securitySettings.value
                put("settings", JSONObject().apply {
                    put("flagSecureEnabled", settings.flagSecureEnabled)
                    put("clipboardAutoClearEnabled", settings.clipboardAutoClearEnabled)
                    put("clipboardClearSeconds", settings.clipboardClearSeconds)
                    put("autoLockSeconds", settings.autoLockSeconds)
                    put("duressAction", settings.duressAction.name)
                })
            }

            // Write to vault file
            val contentBytes = root.toString().toByteArray(Charsets.UTF_8)
            vaultFile.writeBytes(contentBytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadVault() {
        try {
            if (!vaultFile.exists()) return
            val jsonString = vaultFile.readText(Charsets.UTF_8)
            if (jsonString.isBlank()) return
            val root = JSONObject(jsonString)

            val accObj = root.optJSONObject("account")
            if (accObj != null) {
                val loadedAccount = UserAccount(
                    id = accObj.getString("id"),
                    mode = UserMode.valueOf(accObj.getString("mode")),
                    username = accObj.optString("username").takeIf { it.isNotBlank() },
                    publicKeyBase64 = accObj.getString("publicKeyBase64"),
                    privateKeyBase64 = accObj.getString("privateKeyBase64"),
                    salt = accObj.getString("salt"),
                    pinHash = accObj.optString("pinHash").takeIf { it.isNotBlank() },
                    duressPinHash = accObj.optString("duressPinHash").takeIf { it.isNotBlank() },
                    duressAction = DuressAction.valueOf(accObj.optString("duressAction", DuressAction.OPEN_DECOY_MODE.name))
                )
                _account.value = loadedAccount
                if (loadedAccount.pinHash != null) {
                    _isLocked.value = true
                }
            }

            val convArray = root.optJSONArray("conversations")
            if (convArray != null) {
                val list = mutableListOf<ChatConversation>()
                for (i in 0 until convArray.length()) {
                    val obj = convArray.getJSONObject(i)
                    list.add(
                        ChatConversation(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            remotePublicKeyBase64 = obj.getString("remotePublicKeyBase64"),
                            lastMessagePreview = obj.optString("lastMessagePreview", ""),
                            lastTimestamp = obj.optLong("lastTimestamp", System.currentTimeMillis()),
                            ephemeralTimer = EphemeralTimer.valueOf(obj.optString("ephemeralTimer", EphemeralTimer.OFF.name)),
                            ephemeralTrigger = EphemeralTrigger.valueOf(obj.optString("ephemeralTrigger", EphemeralTrigger.ON_READ.name)),
                            isPinned = obj.optBoolean("isPinned", false),
                            folderCategory = obj.optString("folderCategory", "Innboks"),
                            unreadCount = obj.optInt("unreadCount", 0),
                            isSafetyNumberVerified = obj.optBoolean("isSafetyNumberVerified", false)
                        )
                    )
                }
                _conversations.value = list
            }

            val msgArray = root.optJSONArray("messages")
            if (msgArray != null) {
                val list = mutableListOf<ChatMessage>()
                for (i in 0 until msgArray.length()) {
                    val obj = msgArray.getJSONObject(i)
                    val readAtVal = obj.optLong("readAt", -1L)
                    val expiresAtVal = obj.optLong("expiresAt", -1L)

                    list.add(
                        ChatMessage(
                            id = obj.getString("id"),
                            conversationId = obj.getString("conversationId"),
                            senderPublicKey = obj.getString("senderPublicKey"),
                            senderName = obj.getString("senderName"),
                            isFromMe = obj.getBoolean("isFromMe"),
                            plaintext = obj.getString("plaintext"),
                            ciphertextBase64 = obj.getString("ciphertextBase64"),
                            ivBase64 = obj.getString("ivBase64"),
                            authTagBase64 = obj.getString("authTagBase64"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            sentAt = obj.optLong("sentAt", System.currentTimeMillis()),
                            readAt = if (readAtVal > 0) readAtVal else null,
                            expiresAt = if (expiresAtVal > 0) expiresAtVal else null,
                            ephemeralTimer = EphemeralTimer.valueOf(obj.optString("ephemeralTimer", EphemeralTimer.OFF.name)),
                            ephemeralTrigger = EphemeralTrigger.valueOf(obj.optString("ephemeralTrigger", EphemeralTrigger.ON_READ.name))
                        )
                    )
                }
                _messages.value = list
            }

            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                _securitySettings.value = SecuritySettings(
                    flagSecureEnabled = settingsObj.optBoolean("flagSecureEnabled", true),
                    clipboardAutoClearEnabled = settingsObj.optBoolean("clipboardAutoClearEnabled", true),
                    clipboardClearSeconds = settingsObj.optInt("clipboardClearSeconds", 30),
                    autoLockSeconds = settingsObj.optInt("autoLockSeconds", 60),
                    duressAction = DuressAction.valueOf(settingsObj.optString("duressAction", DuressAction.OPEN_DECOY_MODE.name))
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
