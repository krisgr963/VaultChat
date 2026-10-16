package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.model.ChatConversation
import com.example.model.EphemeralTimer
import com.example.model.UserMode
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(viewModel: ChatViewModel) {
    val account by viewModel.vaultStorage.account.collectAsState()
    val conversations by viewModel.vaultStorage.conversations.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showNewChatDialog by remember { mutableStateOf(false) }
    var showPanicWipeDialog by remember { mutableStateOf(false) }
    var showMyQrDialog by remember { mutableStateOf(false) }

    val folders = listOf("Alle", "Sikkert hvelv", "Kontakter", "Arkiv")

    val filteredConversations = conversations.filter { conv ->
        val matchesSearch = conv.title.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessagePreview.contains(searchQuery, ignoreCase = true)
        val matchesFolder = if (account?.mode == UserMode.PROFILED) {
            selectedFolder == "Alle" || conv.folderCategory == selectedFolder
        } else true
        matchesSearch && matchesFolder
    }.sortedWith(compareByDescending<ChatConversation> { it.isPinned }.thenByDescending { it.lastTimestamp })

    Scaffold(
        containerColor = CyberVoid,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (account?.isDecoy == true) CyberAmber else CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AEGIS",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Badge indicating mode
                            val (badgeText, badgeColor) = when {
                                account?.isDecoy == true -> "DECOY AKTIV" to CyberAmber
                                account?.mode == UserMode.ANONYMOUS -> "ANONYM" to CyberCyan
                                else -> "PROFILERT" to CyberEmerald
                            }
                            Text(
                                text = badgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = account?.username ?: account?.id ?: "E2EE Aktiv",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // QR Code icon
                    IconButton(
                        onClick = { showMyQrDialog = true },
                        modifier = Modifier.testTag("qr_action_button")
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = "Min QR-kode", tint = CyberCyan)
                    }

                    // Architecture Documentation viewer icon
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.ArchitectureDocs) },
                        modifier = Modifier.testTag("architecture_action_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "Arkitektur", tint = TextSecondary)
                    }

                    // Manual Lock
                    if (account?.pinHash != null) {
                        IconButton(
                            onClick = { viewModel.triggerManualLock() },
                            modifier = Modifier.testTag("lock_action_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lås hvelv", tint = CyberEmerald)
                        }
                    }

                    // Settings
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("settings_action_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Innstillinger", tint = TextSecondary)
                    }

                    // Quick Kill Switch button
                    IconButton(
                        onClick = { showPanicWipeDialog = true },
                        modifier = Modifier.testTag("quick_kill_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Panic Wipe",
                            tint = CyberRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberDarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = CyberCyan,
                contentColor = CyberVoid,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ny samtale")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Decoy stealth banner if active
            if (account?.isDecoy == true) {
                Surface(
                    color = CyberAmber.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nødmodus aktiv (Decoy). Reelle krypteringsnøkler er isolert.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberAmber
                        )
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Søk i krypterte chatter...", fontSize = 13.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberDarkSurface,
                    unfocusedContainerColor = CyberDarkSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Folders for Profiled mode
            if (account?.mode == UserMode.PROFILED) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders) { folder ->
                        val isSelected = selectedFolder == folder
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFolder(folder) },
                            label = { Text(folder, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                containerColor = CyberDarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) CyberCyan else CyberBorder
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Conversation list
            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyberDarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ingen samtaler funnet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Trykk på + knappen nedenfor for å utveksle ECDH-nøkler og starte en kryptert samtale.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = { viewModel.navigateTo(Screen.Conversation(conv.id)) },
                            onTogglePin = { viewModel.togglePin(conv.id) }
                        )
                    }
                }
            }
        }
    }

    // New Chat Dialog
    if (showNewChatDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newKey by remember { mutableStateOf("") }
        var newFolder by remember { mutableStateOf("Innboks") }

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Ny E2EE Chattråd", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Legg til kontaktens offentlige ECDH-nøkkel for å initiere en ende-til-ende-kryptert kanal.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Kontaktnavn / Kallenavn") },
                        placeholder = { Text("F.eks. Bob eller Node_44") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Motpartens Public Key (Base64)") },
                        placeholder = { Text("Lim inn ECDH-nøkkel...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                // Generate a simulated valid ECDH peer key for quick testing
                                val testPeer = CryptoEngine.generateEphemeralKeyPair()
                                newKey = CryptoEngine.encodePublicKey(testPeer.public)
                                if (newTitle.isBlank()) newTitle = "Sikker kontakt #${(10..99).random()}"
                            }
                        ) {
                            Text("⚡ Generer testnøkkel", fontSize = 12.sp, color = CyberCyan)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val finalKey = if (newKey.isNotBlank()) newKey else {
                                val testPeer = CryptoEngine.generateEphemeralKeyPair()
                                CryptoEngine.encodePublicKey(testPeer.public)
                            }
                            viewModel.createNewChat(newTitle, finalKey, newFolder)
                            showNewChatDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberVoid)
                ) {
                    Text("Opprett kanal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Avbryt", color = TextSecondary)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }

    // My QR Code & Key Dialog
    if (showMyQrDialog && account != null) {
        val pubKey = account!!.publicKeyBase64
        AlertDialog(
            onDismissRequest = { showMyQrDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Min E2EE Identitet", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "La motparten skanne denne QR-koden for å etablere en verifisert, avlyttingssikker forbindelse.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    com.example.ui.components.QrCodeView(
                        data = pubKey,
                        modifier = Modifier.size(190.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Din offentlige nøkkel:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = pubKey.take(32) + "...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.copyToClipboardSecurely(pubKey, "Offentlig nøkkel") },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant, contentColor = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kopier Public Key (Auto-Clear)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMyQrDialog = false }) {
                    Text("Lukk", color = CyberCyan)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }

    // Panic Wipe Confirmation Dialog
    if (showPanicWipeDialog) {
        AlertDialog(
            onDismissRequest = { showPanicWipeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CyberRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PANIC WIPE / KILL BUTTON", color = CyberRed, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Advarsel! Dette vil utføre en fullstendig 4-trinns nødsletting:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Sletter alle lokale krypteringsnøkler fra Keystore.", fontSize = 12.sp, color = TextSecondary)
                    Text("2. Overskriver lokal database med 0-bytes og sletter historikk.", fontSize = 12.sp, color = TextSecondary)
                    Text("3. Terminerer server-sesjon og blind-relay køer.", fontSize = 12.sp, color = TextSecondary)
                    Text("4. Tvinger utlogging til blank startskjerm.", fontSize = 12.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPanicWipeDialog = false
                        viewModel.executePanicWipe()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed)
                ) {
                    Text("Utfør panikksletting nå", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicWipeDialog = false }) {
                    Text("Avbryt", color = TextSecondary)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
            containerColor = CyberDarkSurface
        )
    }
}

@Composable
fun ConversationItem(
    conversation: ChatConversation,
    onClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    val timeFormatted = remember(conversation.lastTimestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conversation.lastTimestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_item_${conversation.id}")
            .clickable { onClick() }
            .border(
                1.dp,
                if (conversation.isPinned) CyberCyan.copy(alpha = 0.5f) else CyberBorder,
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (conversation.isPinned) CyberDarkSurface else CyberDarkSurface.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with lock icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceVariant)
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.title.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = conversation.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.isSafetyNumberVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verifisert",
                                tint = CyberEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessagePreview.ifBlank { "Kanal klar for kryptert dialog" },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (conversation.ephemeralTimer != EphemeralTimer.OFF) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(CyberAmber.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = CyberAmber,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = conversation.ephemeralTimer.label.take(3),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberAmber
                                )
                            }
                        }

                        if (conversation.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conversation.unreadCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyberVoid
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
