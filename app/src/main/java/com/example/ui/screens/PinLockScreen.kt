package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatViewModel
import com.example.ui.theme.BubbleMine
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PinLockScreen(viewModel: ChatViewModel) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showPanicDialog by remember { mutableStateOf(false) }

    val account by viewModel.vaultStorage.account.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Automatically prompt for biometric unlock (fingerprint / face) before showing messages
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (activity != null) {
            val avail = com.example.security.BiometricAuthManager.checkBiometricAvailability(context)
            if (avail == com.example.security.BiometricAvailability.AVAILABLE) {
                com.example.security.BiometricAuthManager.showBiometricPrompt(
                    activity = activity,
                    title = "Biometrisk tilgangskontroll",
                    subtitle = "Bekreft fingeravtrykk eller ansikt for å dekryptere meldinger",
                    onSuccess = {
                        viewModel.unlockWithBiometrics()
                    },
                    onError = { _, _ -> },
                    onFailed = { }
                )
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberVoid
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Quick Panic button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AEGIS VAULT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = CyberCyan
                    )
                }

                TextButton(
                    onClick = { showPanicDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Panic Wipe",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "NØDSLETTING", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Lock Icon & Instruction
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Hvelv låst",
                        tint = if (pinError != null) CyberRed else CyberCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "LÅST HVELV",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (account?.username != null) "Logget inn som ${account?.username}" else "Tast inn din sikkerhets-PIN for å dekryptere",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(maxOf(4, enteredPin.length)) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        pinError != null -> CyberRed
                                        isFilled -> CyberCyan
                                        else -> CyberSurfaceVariant
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) CyberCyan else CyberBorder,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dedicated Biometric Unlock Button
                Button(
                    onClick = {
                        if (activity != null) {
                            com.example.security.BiometricAuthManager.showBiometricPrompt(
                                activity = activity,
                                title = "Biometrisk tilgangskontroll",
                                subtitle = "Verifiser fingeravtrykk eller ansikt for å dekryptere meldinger",
                                onSuccess = {
                                    viewModel.unlockWithBiometrics()
                                },
                                onError = { _, msg ->
                                    pinError = msg
                                },
                                onFailed = {
                                    pinError = "Biometrisk autentisering mislyktes. Prøv igjen eller bruk PIN."
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSurfaceVariant,
                        contentColor = CyberCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("biometric_unlock_button")
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp), tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lås opp med biometri", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                if (pinError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = pinError!!,
                        color = CyberRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "del")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(CyberDarkSurface)
                                    .border(1.dp, CyberBorder.copy(alpha = 0.5f), CircleShape)
                                    .testTag("pin_key_$key")
                                    .clickable {
                                        pinError = null
                                        when (key) {
                                            "del" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                            "bio" -> {
                                                if (activity != null) {
                                                    com.example.security.BiometricAuthManager.showBiometricPrompt(
                                                        activity = activity,
                                                        title = "Biometrisk tilgangskontroll",
                                                        subtitle = "Verifiser fingeravtrykk eller ansikt for å dekryptere meldinger",
                                                        onSuccess = {
                                                            viewModel.unlockWithBiometrics()
                                                        },
                                                        onError = { _, msg ->
                                                            pinError = msg
                                                        },
                                                        onFailed = {
                                                            pinError = "Biometrisk autentisering mislyktes."
                                                        }
                                                    )
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 8) {
                                                    val newPin = enteredPin + key
                                                    enteredPin = newPin

                                                    // Auto-verify when at least 4 digits
                                                    if (newPin.length >= 4) {
                                                        val unlocked = viewModel.unlockPin(newPin)
                                                        if (!unlocked && newPin.length >= 6) {
                                                            pinError = "Feil PIN. Prøv igjen."
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "del" -> Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Slett",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    "bio" -> Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometri",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    else -> Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = { showPanicDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CyberRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NØDSLETTING (PANIC WIPE)", color = CyberRed, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Er du sikker på at du vil utføre umiddelbar sletting? Alle kryptografiske nøkler, meldinger og hvelvdata vil bli uopprettelig destruert.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPanicDialog = false
                        viewModel.executePanicWipe()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed)
                ) {
                    Text("Destruer alt nå", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicDialog = false }) {
                    Text("Avbryt", color = TextSecondary)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }
}
