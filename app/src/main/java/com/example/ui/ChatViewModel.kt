package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EncryptedVaultStorage
import com.example.model.ChatConversation
import com.example.model.ChatMessage
import com.example.model.DuressAction
import com.example.model.EphemeralTimer
import com.example.model.EphemeralTrigger
import com.example.model.SecuritySettings
import com.example.model.UserAccount
import com.example.model.UserMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen {
    object Onboarding : Screen()
    object PinLock : Screen()
    object ChatList : Screen()
    data class Conversation(val conversationId: String) : Screen()
    object Settings : Screen()
    object ArchitectureDocs : Screen()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    val vaultStorage = EncryptedVaultStorage(application)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Onboarding)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _selectedFolder = MutableStateFlow("Alle")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Countdown clock ticker
    private val _nowTick = MutableStateFlow(System.currentTimeMillis())
    val nowTick: StateFlow<Long> = _nowTick.asStateFlow()

    // UI Feedback toasts/notifications
    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    private var tickerJob: Job? = null
    val clipboardLeakMonitor = com.example.security.ClipboardLeakMonitor(application, viewModelScope) { reason ->
        showFeedback(reason)
    }

    init {
        // Start clipboard leak observer
        clipboardLeakMonitor.startMonitoring {
            vaultStorage.securitySettings.value.clipboardClearSeconds
        }

        // Evaluate initial screen based on account and lock state
        if (vaultStorage.account.value == null) {
            _currentScreen.value = Screen.Onboarding
        } else if (vaultStorage.isLocked.value) {
            _currentScreen.value = Screen.PinLock
        } else {
            _currentScreen.value = Screen.ChatList
        }

        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _nowTick.value = System.currentTimeMillis()
                vaultStorage.purgeExpiredMessages()
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Conversation) {
            _selectedConversationId.value = screen.conversationId
            vaultStorage.markConversationAsRead(screen.conversationId)
        }
    }

    fun setFolder(folder: String) {
        _selectedFolder.value = folder
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createAnonymousAccount() {
        vaultStorage.createAnonymousAccount()
        _currentScreen.value = Screen.ChatList
        showFeedback("Anonym modus aktivert: Ingen persondata lagret!")
    }

    fun createProfiledAccount(username: String, pin: String, duressPin: String?, action: DuressAction) {
        vaultStorage.createProfiledAccount(username, pin, duressPin, action)
        _currentScreen.value = Screen.ChatList
        showFeedback("Profilert modus aktivert med E2EE og Duress-støtte!")
    }

    fun unlockPin(pin: String): Boolean {
        val success = vaultStorage.unlockWithPin(pin)
        if (success) {
            _currentScreen.value = Screen.ChatList
        }
        return success
    }

    fun unlockWithBiometrics() {
        val success = vaultStorage.unlockWithBiometrics()
        if (success) {
            _currentScreen.value = Screen.ChatList
            showFeedback("Hvelv låst opp med biometri!")
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        vaultStorage.sendMessage(conversationId, text)
    }

    fun simulatePeerResponse(conversationId: String, text: String) {
        vaultStorage.simulatePeerReply(conversationId, text)
    }

    fun updateEphemeralTimer(conversationId: String, timer: EphemeralTimer, trigger: EphemeralTrigger) {
        vaultStorage.updateConversationTimer(conversationId, timer, trigger)
        showFeedback("Selvsletting: ${timer.label} (${trigger.label})")
    }

    fun togglePin(conversationId: String) {
        vaultStorage.togglePinConversation(conversationId)
    }

    fun setSafetyVerified(conversationId: String, verified: Boolean) {
        vaultStorage.setSafetyNumberVerified(conversationId, verified)
        showFeedback(if (verified) "Sikkerhetsnummer bekreftet (Zero-MITM)!" else "Verifisering nullstilt.")
    }

    fun createNewChat(title: String, remotePublicKey: String, folder: String = "Innboks"): ChatConversation {
        val conv = vaultStorage.addConversation(title, remotePublicKey, folder)
        showFeedback("Ny sikker chattråd opprettet.")
        return conv
    }

    fun updateSecuritySettings(settings: SecuritySettings) {
        vaultStorage.updateSecuritySettings(settings)
        showFeedback("Sikkerhetsinnstillinger oppdatert.")
    }

    fun triggerManualLock() {
        clipboardLeakMonitor.clearIfVaultContentDetected()
        vaultStorage.lockVault()
        _currentScreen.value = Screen.PinLock
    }

    fun triggerInactivityLock() {
        clipboardLeakMonitor.clearIfVaultContentDetected()
        vaultStorage.lockVault()
        if (_currentScreen.value !is Screen.Onboarding && _currentScreen.value !is Screen.PinLock) {
            _currentScreen.value = Screen.PinLock
            showFeedback("Hvelv låst etter inaktivitet. Biometri kreves for å dekryptere.")
        }
    }

    fun executePanicWipe() {
        clipboardLeakMonitor.purgeClipboard()
        vaultStorage.executePanicWipe()
        _currentScreen.value = Screen.Onboarding
        showFeedback("💥 PANIC WIPE UTFØRT: Nøkler, database og historikk slettet.")
    }

    fun copyToClipboardSecurely(text: String, label: String = "VaultChat_Secure") {
        val clearSeconds = vaultStorage.securitySettings.value.clipboardClearSeconds
        clipboardLeakMonitor.copyToClipboard(text, label, clearSeconds)
        showFeedback("Kopiert. Utklippstavle overvåkes og slettes automatisk om $clearSeconds s.")
    }

    fun showFeedback(msg: String) {
        _userFeedback.value = msg
        viewModelScope.launch {
            delay(3500)
            if (_userFeedback.value == msg) {
                _userFeedback.value = null
            }
        }
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }

    override fun onCleared() {
        super.onCleared()
        clipboardLeakMonitor.stopMonitoring()
    }
}
