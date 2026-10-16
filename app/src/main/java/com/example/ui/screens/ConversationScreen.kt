package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.model.ChatMessage
import com.example.model.EphemeralTimer
import com.example.model.EphemeralTrigger
import com.example.ui.ChatViewModel
import com.example.ui.Screen
import com.example.ui.components.QrCodeView
import com.example.ui.theme.BubbleMine
import com.example.ui.theme.BubbleMineBorder
import com.example.ui.theme.BubbleOther
import com.example.ui.theme.BubbleOtherBorder
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    viewModel: ChatViewModel
) {
    BackHandler {
        viewModel.navigateTo(Screen.ChatList)
    }

    val account by viewModel.vaultStorage.account.collectAsState()
    val conversations by viewModel.vaultStorage.conversations.collectAsState()
    val messages by viewModel.vaultStorage.messages.collectAsState()
    val nowTick by viewModel.nowTick.collectAsState()

    val conversation = conversations.find { it.id == conversationId }
    val threadMessages = messages.filter { it.conversationId == conversationId }

    var inputText by remember { mutableStateOf("") }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showSafetyNumberDialog by remember { mutableStateOf(false) }
    var inspectedMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(threadMessages.size) {
        if (threadMessages.isNotEmpty()) {
            listState.animateScrollToItem(threadMessages.size - 1)
        }
    }

    Scaffold(
        containerColor = CyberVoid,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSafetyNumberDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CyberSurfaceVariant)
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (conversation?.title ?: "??").take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CyberCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conversation?.title ?: "Sikker kanal",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (conversation?.isSafetyNumberVerified == true) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = "E2EE Status",
                                    tint = if (conversation?.isSafetyNumberVerified == true) CyberEmerald else CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = if (conversation?.isSafetyNumberVerified == true) "Sikkerhetsnummer bekreftet (Zero-MITM)" else "Trykk for å verifisere nøkkel",
                                fontSize = 11.sp,
                                color = if (conversation?.isSafetyNumberVerified == true) CyberEmerald else TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.ChatList) },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tilbake",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Ephemeral Timer Configuration
                    IconButton(
                        onClick = { showTimerDialog = true },
                        modifier = Modifier.testTag("ephemeral_timer_button")
                    ) {
                        Icon(
                            imageVector = if (conversation?.ephemeralTimer != EphemeralTimer.OFF) Icons.Default.LocalFireDepartment else Icons.Default.Timer,
                            contentDescription = "Selvsletting",
                            tint = if (conversation?.ephemeralTimer != EphemeralTimer.OFF) CyberAmber else TextSecondary
                        )
                    }

                    // Key Exchange / Safety Number
                    IconButton(
                        onClick = { showSafetyNumberDialog = true },
                        modifier = Modifier.testTag("safety_number_button")
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Safety Number", tint = CyberCyan)
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
                .imePadding()
        ) {
            // Ephemeral Status Bar
            if (conversation?.ephemeralTimer != EphemeralTimer.OFF) {
                Surface(
                    color = CyberAmber.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimerDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = CyberAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Selvsletting aktiv: ${conversation?.ephemeralTimer?.label} (${conversation?.ephemeralTrigger?.label})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberAmber
                        )
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // E2EE Channel Encryption Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(CyberDarkSurface.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ENDE-TIL-ENDE KRYPTERT KANAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = CyberCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Protokoll: ECDH (P-256) + AES-256-GCM. Klartekst og nøkler forlater aldri denne enheten.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(threadMessages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        nowTick = nowTick,
                        onInspect = { inspectedMessage = msg },
                        onCopyText = { viewModel.copyToClipboardSecurely(msg.plaintext, "Sikker melding") }
                    )
                }
            }

            // Quick simulation button for peer reply (to easily demo two-way E2EE & burning countdown)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberDarkSurface.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Zero-Knowledge simulering:",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                TextButton(
                    onClick = {
                        val replies = listOf(
                            "Mottatt! Melding dekryptert med AES-256-GCM auth-tag match.",
                            "Konfidensiell fil bekreftet. Klartekst destrueres automatisk.",
                            "Møtet er bekreftet kl. 14:00. Ingen spor gjenstår i serverloggen.",
                            "Sikkerhetsnummer stemmer overens med min enhet."
                        )
                        viewModel.simulatePeerResponse(conversationId, replies.random())
                    }
                ) {
                    Text("⚡ Simuler svar fra motpart", fontSize = 11.sp, color = CyberCyan)
                }
            }

            // Bottom Input Row
            Surface(
                color = CyberDarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Skriv kryptert melding...", fontSize = 13.sp, color = TextMuted) },
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field"),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberVoid,
                            unfocusedContainerColor = CyberVoid
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(conversationId, inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) CyberCyan else CyberSurfaceVariant)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) CyberVoid else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Ephemeral Timer Setting Dialog
    if (showTimerDialog && conversation != null) {
        var selectedTimer by remember { mutableStateOf(conversation.ephemeralTimer) }
        var selectedTrigger by remember { mutableStateOf(conversation.ephemeralTrigger) }

        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = CyberAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Selvslettende Meldinger", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Meldinger destrueres permanent fra både avsender, mottaker og minne når timeren utløper.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("VARIGHET:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)

                    EphemeralTimer.values().forEach { timerOption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTimer = timerOption }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = selectedTimer == timerOption,
                                onClick = { selectedTimer = timerOption },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timerOption.label,
                                fontSize = 13.sp,
                                color = if (selectedTimer == timerOption) TextPrimary else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("UTLØSENDE HENDELSE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)

                    EphemeralTrigger.values().forEach { triggerOption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTrigger = triggerOption }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = selectedTrigger == triggerOption,
                                onClick = { selectedTrigger = triggerOption },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = triggerOption.label,
                                fontSize = 13.sp,
                                color = if (selectedTrigger == triggerOption) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateEphemeralTimer(conversationId, selectedTimer, selectedTrigger)
                        showTimerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberVoid)
                ) {
                    Text("Lagre innstilling", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Avbryt", color = TextSecondary)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }

    // Safety Number / Key Verification Dialog
    if (showSafetyNumberDialog && account != null && conversation != null) {
        val safetyNumber = remember(account!!.publicKeyBase64, conversation.remotePublicKeyBase64) {
            CryptoEngine.generateSafetyNumber(account!!.publicKeyBase64, conversation.remotePublicKeyBase64)
        }

        AlertDialog(
            onDismissRequest = { showSafetyNumberDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CyberEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sikkerhetsnummer (Safety Number)", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sammenlign disse 60 sifrene med motparten (f.eks. ved personlig oppmøte eller en annen sikker kanal) for å eliminere man-in-the-middle angrep.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    QrCodeView(
                        data = safetyNumber,
                        modifier = Modifier.size(160.dp),
                        primaryColor = CyberEmerald
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 60-digit formatted blocks
                    Text(
                        text = safetyNumber,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberEmerald,
                        letterSpacing = 1.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val newStatus = !conversation.isSafetyNumberVerified
                            viewModel.setSafetyVerified(conversationId, newStatus)
                            showSafetyNumberDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (conversation.isSafetyNumberVerified) CyberSurfaceVariant else CyberEmerald,
                            contentColor = if (conversation.isSafetyNumberVerified) TextPrimary else CyberVoid
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (conversation.isSafetyNumberVerified) "Merk som uverifisert" else "Bekreft at sifrene stemmer",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSafetyNumberDialog = false }) {
                    Text("Lukk", color = CyberCyan)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }

    // Inspect E2EE Envelope Dialog
    if (inspectedMessage != null) {
        val msg = inspectedMessage!!
        AlertDialog(
            onDismissRequest = { inspectedMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zero-Knowledge E2EE Konvolutt", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text("Hva blind-relay serveren observerer:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Kryptert Ciphertext (AES-256):", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = msg.ciphertextBase64.take(48) + "...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Initsieringsvektor (96-bit IV):", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = msg.ivBase64,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Autentiseringstag (128-bit GCM MAC):", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = msg.authTagBase64,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.copyToClipboardSecurely(msg.plaintext, "Klartekst melding")
                            inspectedMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant, contentColor = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kopier klartekst (30s auto-sletting)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedMessage = null }) {
                    Text("Lukk", color = CyberCyan)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    nowTick: Long,
    onInspect: () -> Unit,
    onCopyText: () -> Unit
) {
    val isMine = message.isFromMe
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    // Real-time ephemeral countdown calculation
    val remainingSeconds = if (message.expiresAt != null) {
        maxOf(0L, (message.expiresAt - nowTick) / 1000L)
    } else null

    // Subtle pulsing animation if less than 10 seconds remaining
    val isBurningUrgent = remainingSeconds != null && remainingSeconds <= 10
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 2.dp,
                        bottomEnd = if (isMine) 2.dp else 16.dp
                    )
                )
                .background(if (isMine) BubbleMine else BubbleOther)
                .border(
                    width = 1.dp,
                    color = when {
                        isBurningUrgent -> CyberAmber.copy(alpha = pulseAlpha)
                        isMine -> BubbleMineBorder.copy(alpha = 0.4f)
                        else -> BubbleOtherBorder
                    },
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 2.dp,
                        bottomEnd = if (isMine) 2.dp else 16.dp
                    )
                )
                .clickable { onInspect() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (!isMine) {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = message.plaintext,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ephemeral Burning Countdown Badge
                    if (remainingSeconds != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(CyberAmber.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Brenner",
                                tint = CyberAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${remainingSeconds}s",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberAmber
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (message.ephemeralTimer != EphemeralTimer.OFF) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(CyberSurfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Venter på lesing",
                                tint = TextMuted,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = message.ephemeralTimer.label.take(3),
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = TextMuted
                    )

                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kryptert",
                            tint = CyberCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
