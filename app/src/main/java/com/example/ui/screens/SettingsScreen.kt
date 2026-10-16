package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DuressAction
import com.example.ui.ChatViewModel
import com.example.ui.Screen
import com.example.ui.theme.BubbleMine
import com.example.ui.theme.CyberAmber
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ChatViewModel) {
    BackHandler {
        viewModel.navigateTo(Screen.ChatList)
    }

    val account by viewModel.vaultStorage.account.collectAsState()
    val settings by viewModel.vaultStorage.securitySettings.collectAsState()
    val auditLogs by viewModel.vaultStorage.auditLogs.collectAsState()

    var showPanicDialog by remember { mutableStateOf(false) }
    var showDuressConfigDialog by remember { mutableStateOf(false) }
    var showInactivityDialog by remember { mutableStateOf(false) }
    var showClipboardDelayDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CyberVoid,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sikkerhet & Innstillinger",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.ChatList) },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tilbake",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDarkSurface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // KILL BUTTON / PANIC WIPE SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, CyberRed.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CyberRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NØDSLETTING (KILL BUTTON)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = CyberRed
                            )
                            Text(
                                text = "Fullstendig og irreversibel destruksjon av alle data",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ved aktivering utføres umiddelbart:\n" +
                                "1. Sletting av alle private nøkler fra hardware Keystore.\n" +
                                "2. Overskriving av lokal kryptert database med null-bytes.\n" +
                                "3. Destruksjon av chathistorikk og ephemeral session-nøkler.\n" +
                                "4. Anonymisert server-avregistrering og umiddelbar utlogging.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPanicDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("panic_wipe_execute_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UTFØR NØDSLETTING NÅ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DURESS PIN & DECOY CONFIGURATION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Duress PIN & Nødvern",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        TextButton(onClick = { showDuressConfigDialog = true }) {
                            Text("Endre", color = CyberCyan, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (account?.duressPinHash != null) {
                            "Duress PIN er konfigurert. Handling ved tvang: ${settings.duressAction.label}"
                        } else {
                            "Ingen Duress PIN konfigurert. Trykk for å sette en nød-kode."
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PRIVACY HARDENING & LEAK PREVENTION
            Text(
                text = "LEAK-BESKYTTELSE & HARDENING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // FLAG_SECURE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NoPhotography, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "FLAG_SECURE Skjermlås",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Blokkerer skjermbilder, skjermopptak og skjuler forhåndsvisning i app-veksler.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.flagSecureEnabled,
                            onCheckedChange = {
                                viewModel.updateSecuritySettings(settings.copy(flagSecureEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberVoid,
                                checkedTrackColor = CyberCyan
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clipboard Auto-Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Auto-tømming av utklippstavle",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Sletter kopierte data automatisk etter ${settings.clipboardClearSeconds}s.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.clipboardAutoClearEnabled,
                            onCheckedChange = {
                                viewModel.updateSecuritySettings(settings.copy(clipboardAutoClearEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberVoid,
                                checkedTrackColor = CyberEmerald
                            )
                        )
                    }

                    if (settings.clipboardAutoClearEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showClipboardDelayDialog = true }) {
                                Text("Endre forsinkelse (${settings.clipboardClearSeconds}s)", color = CyberCyan, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    viewModel.clipboardLeakMonitor.purgeClipboard()
                                    viewModel.showFeedback("Utklippstavle tømt umiddelbart.")
                                }
                            ) {
                                Text("Tøm nå", color = CyberRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Inactivity Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Inaktivitetslås & Bakgrunnsvern",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                val thresholdText = when (settings.autoLockSeconds) {
                                    0 -> "Umiddelbart ved bakgrunn"
                                    60 -> "1 minutt"
                                    300 -> "5 minutter"
                                    else -> "${settings.autoLockSeconds} sekunder"
                                }
                                Text(
                                    text = "Låser appen etter inaktivitet ($thresholdText). Krever biometri.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        TextButton(onClick = { showInactivityDialog = true }) {
                            Text("Endre", color = CyberCyan, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ARCHITECTURE & WHITEPAPER LINK
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(Screen.ArchitectureDocs) }
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BubbleMine.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fullstack Arkitektur & Trusselmodell",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Se teknisk arkitektur for klient, backend, kryptering og Signal-protokoll.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // REAL-TIME AUDIT LOGS
            Text(
                text = "SIKKERHETSLOGG (AUDIT TRAILS)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (auditLogs.isEmpty()) {
                        Text("Ingen hendelser registrert enda.", fontSize = 11.sp, color = TextMuted)
                    } else {
                        auditLogs.take(8).forEach { log ->
                            Text(
                                text = log,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (log.contains("🔥") || log.contains("💥") || log.contains("⚠️")) CyberAmber else TextSecondary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Panic Wipe Dialog
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
                    "Dette vil umiddelbart og irreversibelt slette alle kryptonøkler, database og meldinger. Prosessen kan ikke avbrytes.",
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

    // Duress Action Config Dialog
    if (showDuressConfigDialog) {
        var selectedAction by remember { mutableStateOf(settings.duressAction) }

        AlertDialog(
            onDismissRequest = { showDuressConfigDialog = false },
            title = { Text("Konfigurer Duress-handling", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Velg hva appen skal gjøre dersom en tvangskode tastes inn på PIN-skjermen:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DuressAction.values().forEach { action ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAction = action }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedAction == action,
                                onClick = { selectedAction = action },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = action.label,
                                fontSize = 13.sp,
                                color = if (selectedAction == action) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSecuritySettings(settings.copy(duressAction = selectedAction))
                        showDuressConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberVoid)
                ) {
                    Text("Lagre", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuressConfigDialog = false }) {
                    Text("Avbryt", color = TextSecondary)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }

    // Inactivity Threshold Dialog
    if (showInactivityDialog) {
        val options = listOf(
            0 to "Umiddelbart ved bakgrunn",
            15 to "15 sekunder inaktivitet",
            30 to "30 sekunder inaktivitet",
            60 to "1 minutt inaktivitet",
            300 to "5 minutter inaktivitet"
        )
        var selectedSeconds by remember { mutableStateOf(settings.autoLockSeconds) }

        AlertDialog(
            onDismissRequest = { showInactivityDialog = false },
            title = { Text("Terskel for inaktivitetslås", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Velg hvor lenge appen kan ligge i bakgrunnen eller være inaktiv før biometrisk gjenautentisering kreves:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    options.forEach { (seconds, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSeconds = seconds }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedSeconds == seconds,
                                onClick = { selectedSeconds = seconds },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = if (selectedSeconds == seconds) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSecuritySettings(settings.copy(autoLockSeconds = selectedSeconds))
                        showInactivityDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = CyberVoid)
                ) {
                    Text("Bruk innstilling", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInactivityDialog = false }) {
                    Text("Avbryt", color = TextSecondary)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }

    // Clipboard Clear Delay Dialog
    if (showClipboardDelayDialog) {
        val options = listOf(
            10 to "10 sekunder",
            30 to "30 sekunder (anbefalt)",
            60 to "60 sekunder (1 min)",
            120 to "120 sekunder (2 min)"
        )
        var selectedSeconds by remember { mutableStateOf(settings.clipboardClearSeconds) }

        AlertDialog(
            onDismissRequest = { showClipboardDelayDialog = false },
            title = { Text("Forsinkelse for utklippstavle", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Kopiert innhold fra VaultChat slettes automatisk fra systemets utklippstavle etter denne tiden:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    options.forEach { (seconds, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSeconds = seconds }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedSeconds == seconds,
                                onClick = { selectedSeconds = seconds },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = if (selectedSeconds == seconds) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSecuritySettings(settings.copy(clipboardClearSeconds = selectedSeconds))
                        showClipboardDelayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald, contentColor = CyberVoid)
                ) {
                    Text("Lagre", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClipboardDelayDialog = false }) {
                    Text("Avbryt", color = TextSecondary)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }
}
