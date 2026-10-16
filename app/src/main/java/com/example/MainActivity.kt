package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatViewModel
import com.example.ui.Screen
import com.example.ui.screens.ArchitectureDocsScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var inactivityMonitor: com.example.security.InactivityMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable screen capture prevention unconditionally on the main window
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        // Background & Foreground Inactivity Monitor
        inactivityMonitor = com.example.security.InactivityMonitor(
            scope = androidx.lifecycle.lifecycleScope,
            getThresholdSeconds = { viewModel.vaultStorage.securitySettings.value.autoLockSeconds },
            isLockEnabled = { viewModel.vaultStorage.account.value?.pinHash != null },
            onLockRequired = {
                runOnUiThread {
                    viewModel.triggerInactivityLock()
                }
            }
        )
        inactivityMonitor.start()

        setContent {
            MyApplicationTheme {
                val securitySettings by viewModel.vaultStorage.securitySettings.collectAsState()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val userFeedback by viewModel.userFeedback.collectAsState()

                // Dynamic FLAG_SECURE enforcement
                LaunchedEffect(securitySettings.flagSecureEnabled) {
                    if (securitySettings.flagSecureEnabled) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberVoid)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is Screen.Onboarding -> OnboardingScreen(viewModel)
                            is Screen.PinLock -> PinLockScreen(viewModel)
                            is Screen.ChatList -> ChatListScreen(viewModel)
                            is Screen.Conversation -> ConversationScreen(screen.conversationId, viewModel)
                            is Screen.Settings -> SettingsScreen(viewModel)
                            is Screen.ArchitectureDocs -> ArchitectureDocsScreen(viewModel)
                        }
                    }

                    // Floating feedback toast notification
                    if (userFeedback != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = CyberDarkSurface,
                            shadowElevation = 8.dp
                        ) {
                            Text(
                                text = userFeedback!!,
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::inactivityMonitor.isInitialized) {
            inactivityMonitor.recordUserInteraction()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::inactivityMonitor.isInitialized) {
            inactivityMonitor.onAppForegrounded()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::inactivityMonitor.isInitialized) {
            inactivityMonitor.onAppBackgrounded()
        }
        // Immediately purge clipboard if content from VaultChat was copied
        viewModel.clipboardLeakMonitor.clearIfVaultContentDetected()
    }
}
