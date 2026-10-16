package com.example.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * InactivityMonitor tracks user interactions in the foreground as well as background elapsed time.
 * If the application is in the background or inactive for longer than a user-defined threshold,
 * it locks the vault and forces biometric / PIN re-authentication before any messages are displayed.
 */
class InactivityMonitor(
    private val scope: CoroutineScope,
    private val getThresholdSeconds: () -> Int,
    private val isLockEnabled: () -> Boolean,
    private val onLockRequired: () -> Unit
) {
    @Volatile
    private var lastInteractionTime = System.currentTimeMillis()

    @Volatile
    private var backgroundedAt: Long? = null

    @Volatile
    private var isBackgrounded = false

    private var monitorJob: Job? = null

    /**
     * Starts the periodic inactivity checker.
     */
    fun start() {
        monitorJob?.cancel()
        lastInteractionTime = System.currentTimeMillis()

        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                checkInactivity()
            }
        }
    }

    /**
     * Stops the periodic monitor.
     */
    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * Called whenever a touch, key, or gesture occurs in the activity.
     */
    fun recordUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    /**
     * Called when the app transitions to the background (onPause / onStop).
     */
    fun onAppBackgrounded() {
        val now = System.currentTimeMillis()
        isBackgrounded = true
        backgroundedAt = now

        // Check if threshold is 0 (instant lock on background)
        val threshold = getThresholdSeconds()
        if (isLockEnabled() && threshold == 0) {
            onLockRequired()
        }
    }

    /**
     * Called when the app returns to the foreground (onResume / onStart).
     */
    fun onAppForegrounded() {
        val bgTime = backgroundedAt
        val now = System.currentTimeMillis()
        val threshold = getThresholdSeconds()

        if (isLockEnabled() && bgTime != null) {
            val elapsed = now - bgTime
            if (elapsed >= threshold * 1000L) {
                onLockRequired()
            }
        }

        isBackgrounded = false
        backgroundedAt = null
        lastInteractionTime = now
    }

    private fun checkInactivity() {
        if (!isLockEnabled()) return

        val threshold = getThresholdSeconds()
        val now = System.currentTimeMillis()

        if (isBackgrounded) {
            val bgTime = backgroundedAt ?: return
            val elapsed = now - bgTime
            if (elapsed >= threshold * 1000L) {
                onLockRequired()
            }
        } else {
            val elapsed = now - lastInteractionTime
            if (elapsed >= threshold * 1000L) {
                onLockRequired()
            }
        }
    }
}
