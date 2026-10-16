package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DuressAction
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

@Composable
fun OnboardingScreen(viewModel: ChatViewModel) {
    var selectedMode by remember { mutableStateOf<UserMode?>(null) }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var duressPin by remember { mutableStateOf("") }
    var duressAction by remember { mutableStateOf(DuressAction.OPEN_DECOY_MODE) }
    var formError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberVoid
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CyberCyan.copy(alpha = 0.25f), CyberVoid)
                        )
                    )
                    .border(2.dp, CyberCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Aegis Shield",
                    tint = CyberCyan,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AEGIS CHAT",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = TextPrimary
            )

            Text(
                text = "Zero-Knowledge • E2EE • Ephemeral Messenger",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = CyberCyan
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ingen mellomledd. Ingen metadata. Kun ende-til-ende kryptografi direkte mellom noder.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "VELG SIKKERHETSMODUS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option A: Anonym Modus
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("anonymous_mode_card")
                    .clickable {
                        selectedMode = UserMode.ANONYMOUS
                        formError = null
                    }
                    .border(
                        width = if (selectedMode == UserMode.ANONYMOUS) 2.dp else 1.dp,
                        color = if (selectedMode == UserMode.ANONYMOUS) CyberCyan else CyberBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedMode == UserMode.ANONYMOUS) BubbleMine.copy(alpha = 0.7f) else CyberDarkSurface
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Anonym",
                            tint = CyberCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Anonym Modus",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero-ID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberEmerald,
                                modifier = Modifier
                                    .background(CyberEmerald.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ingen e-post, telefon eller navn. Identitet bindes utelukkende til et tilfeldig ECDH-nøkkelpar og QR-kode.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option B: Profilert Modus
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profiled_mode_card")
                    .clickable {
                        selectedMode = UserMode.PROFILED
                        formError = null
                    }
                    .border(
                        width = if (selectedMode == UserMode.PROFILED) 2.dp else 1.dp,
                        color = if (selectedMode == UserMode.PROFILED) CyberCyan else CyberBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedMode == UserMode.PROFILED) BubbleMine.copy(alpha = 0.7f) else CyberDarkSurface
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profilert",
                            tint = CyberEmerald,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Profilert Modus",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Avansert",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                modifier = Modifier
                                    .background(CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Valgfritt kallenavn, PIN-lås, mappelogikk og Duress PIN for å motvirke tvunget opplåsing.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Expandable details for Profiled Mode
            AnimatedVisibility(visible = selectedMode == UserMode.PROFILED) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OPPSETT FOR PROFILERT MODUS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Kallenavn / Alias (valgfritt)") },
                            placeholder = { Text("F.eks. Falcon eller Cipher99") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyberCyan) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } },
                            label = { Text("Hoved-PIN for hvelvlås (4–8 siffer)") },
                            placeholder = { Text("1234") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CyberEmerald) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("primary_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberEmerald,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = duressPin,
                            onValueChange = { if (it.length <= 8) duressPin = it.filter { c -> c.isDigit() } },
                            label = { Text("Duress PIN (Nød-PIN / Tvangskode)") },
                            placeholder = { Text("F.eks. 9999 (valgfri)") },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CyberAmber) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("duress_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberAmber,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Hva skal skje om Duress PIN tastes inn på låseskjermen?",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { duressAction = DuressAction.OPEN_DECOY_MODE }
                        ) {
                            RadioButton(
                                selected = duressAction == DuressAction.OPEN_DECOY_MODE,
                                onClick = { duressAction = DuressAction.OPEN_DECOY_MODE },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Åpne uskyldig decoy-profil (kamuflasje)",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { duressAction = DuressAction.STEALTH_WIPE_AND_RESET }
                        ) {
                            RadioButton(
                                selected = duressAction == DuressAction.STEALTH_WIPE_AND_RESET,
                                onClick = { duressAction = DuressAction.STEALTH_WIPE_AND_RESET },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Skjult panikksletting (Wipe alle data)",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            if (formError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formError!!,
                    color = CyberRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = {
                    when (selectedMode) {
                        UserMode.ANONYMOUS -> {
                            viewModel.createAnonymousAccount()
                        }
                        UserMode.PROFILED -> {
                            if (pin.length < 4) {
                                formError = "PIN må bestå av minst 4 siffer."
                                return@Button
                            }
                            if (duressPin.isNotBlank() && duressPin == pin) {
                                formError = "Duress PIN kan ikke være identisk med hoved-PIN!"
                                return@Button
                            }
                            val effectiveUsername = if (username.isBlank()) "Agent_${(100..999).random()}" else username
                            viewModel.createProfiledAccount(effectiveUsername, pin, duressPin, duressAction)
                        }
                        null -> {
                            formError = "Vennligst velg Anonym eller Profilert modus for å fortsette."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = CyberVoid
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedMode == UserMode.ANONYMOUS) "Generer nøkkelpar & start" else "Opprett sikkert hvelv",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tech Architecture link
            Text(
                text = "Les komplett fullstack-arkitektur & trusselmodell",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberCyan,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.ArchitectureDocs) }
                    .padding(8.dp)
            )
        }
    }
}
