package com.example.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * ClipboardLeakMonitor observes system clipboard changes and automatically clears
 * any sensitive content that originated from VaultChat / Aegis, preventing leaks to
 * external applications, third-party keyboards, or background listeners.
 */
class ClipboardLeakMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onLeakPrevented: (reason: String) -> Unit = {}
) {
    companion object {
        const val VAULT_CLIP_PREFIX = "VaultChat_"
        const val VAULT_EXTRA_KEY = "is_vault_chat_sensitive"
    }

    private val clipboardManager: ClipboardManager? by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    // Set of SHA-256 hashes of text recently copied from the app
    private val copiedHashes = mutableSetOf<String>()
    private var clipChangedListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var autoClearJob: Job? = null

    /**
     * Starts active observation of the system clipboard.
     */
    fun startMonitoring(getAutoClearDelaySeconds: () -> Int = { 30 }) {
        if (clipboardManager == null || clipChangedListener != null) return

        clipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
            val isVaultContent = checkCurrentClipIsVaultContent()
            if (isVaultContent) {
                val delaySeconds = getAutoClearDelaySeconds()
                scheduleAutoClear(delaySeconds)
            }
        }

        try {
            clipboardManager?.addPrimaryClipChangedListener(clipChangedListener)
        } catch (e: Exception) {
            Log.e("ClipboardLeakMonitor", "Failed to register clip listener", e)
        }
    }

    /**
     * Stops monitoring clipboard changes.
     */
    fun stopMonitoring() {
        clipChangedListener?.let {
            try {
                clipboardManager?.removePrimaryClipChangedListener(it)
            } catch (e: Exception) {
                // Ignore
            }
            clipChangedListener = null
        }
        autoClearJob?.cancel()
    }

    /**
     * Securely copies text to the system clipboard tagged with VaultChat origin identifiers.
     */
    fun copyToClipboard(text: String, label: String = "VaultChat_Secure", autoClearSeconds: Int = 30) {
        val cm = clipboardManager ?: return
        val hash = hashText(text)
        copiedHashes.add(hash)

        val fullLabel = if (label.startsWith(VAULT_CLIP_PREFIX)) label else "$VAULT_CLIP_PREFIX$label"
        val clip = ClipData.newPlainText(fullLabel, text)

        // Mark as sensitive on Android 13+ (Tiramisu) to hide preview in system clipboard UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras?.putBoolean("android.content.extra.IS_SENSITIVE", true)
        }

        cm.setPrimaryClip(clip)
        scheduleAutoClear(autoClearSeconds)
    }

    /**
     * Checks if current clipboard content contains VaultChat sensitive data.
     */
    fun checkCurrentClipIsVaultContent(): Boolean {
        val cm = clipboardManager ?: return false
        val clip = cm.primaryClip ?: return false
        if (clip.itemCount == 0) return false

        // Check 1: Label prefix
        val label = clip.description.label?.toString() ?: ""
        if (label.startsWith(VAULT_CLIP_PREFIX) || label.contains("VaultChat", ignoreCase = true) || label.contains("Aegis", ignoreCase = true)) {
            return true
        }

        // Check 2: Hash match
        val itemText = clip.getItemAt(0)?.text?.toString() ?: return false
        val hash = hashText(itemText)
        if (copiedHashes.contains(hash)) {
            return true
        }

        return false
    }

    /**
     * Immediately purges clipboard if VaultChat content is present.
     * Invoked when app goes to background or on inactivity timeout.
     */
    fun clearIfVaultContentDetected(): Boolean {
        if (checkCurrentClipIsVaultContent()) {
            purgeClipboard()
            onLeakPrevented("Utklippstavle tømt: Sensitivt VaultChat-innhold ble fjernet.")
            return true
        }
        return false
    }

    /**
     * Schedules auto-clearing after the specified duration.
     */
    private fun scheduleAutoClear(delaySeconds: Int) {
        autoClearJob?.cancel()
        if (delaySeconds <= 0) {
            purgeClipboard()
            return
        }

        autoClearJob = scope.launch(Dispatchers.Main) {
            delay(delaySeconds * 1000L)
            if (checkCurrentClipIsVaultContent()) {
                purgeClipboard()
                onLeakPrevented("Utklippstavle tømt etter $delaySeconds sekunder av sikkerhetshensyn.")
            }
        }
    }

    /**
     * Unconditionally overwrites and clears the system clipboard.
     */
    fun purgeClipboard() {
        try {
            val cm = clipboardManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cm.clearPrimaryClip()
            } else {
                val emptyClip = ClipData.newPlainText("", "")
                cm.setPrimaryClip(emptyClip)
            }
            copiedHashes.clear()
        } catch (e: Exception) {
            Log.e("ClipboardLeakMonitor", "Failed to clear clipboard", e)
        }
    }

    private fun hashText(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
