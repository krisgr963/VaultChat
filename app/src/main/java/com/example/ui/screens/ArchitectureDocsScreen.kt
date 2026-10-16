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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ArchitectureDocsScreen(viewModel: ChatViewModel) {
    BackHandler {
        viewModel.navigateTo(Screen.ChatList)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Oversikt & Stack", "Mappestruktur", "E2EE Kryptografi", "Selvsletting & Kill")

    Scaffold(
        containerColor = CyberVoid,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Systemarkitektur & Kodebase",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.ChatList) },
                        modifier = Modifier.testTag("architecture_back_button")
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
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberDarkSurface,
                contentColor = CyberCyan,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab),
                        color = CyberCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) CyberCyan else TextSecondary
                            )
                        }
                    )
                }
            }

            // Tab Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> StackOverviewSection()
                    1 -> FolderStructureSection()
                    2 -> CryptographyArchitectureSection()
                    3 -> EphemeralAndKillSection()
                }
            }
        }
    }
}

@Composable
fun StackOverviewSection() {
    Text(
        text = "ANBEFALT FULLSTACK-ARKITEKTUR",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = CyberCyan
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StackItem(
                title = "Android-klient",
                badge = "Native Kotlin + Jetpack Compose",
                desc = "Material 3, AndroidKeyStore for maskinvare-isolering av nøkler, SecureRandom, FLAG_SECURE for skjermbildeblokkering, og minnesletting (RAM shredding)."
            )
            Spacer(modifier = Modifier.height(12.dp))
            StackItem(
                title = "Lokal Sikker Database",
                badge = "Room + SQLCipher (AES-256)",
                desc = "100 % kryptert på lagringsmediet (encryption-at-rest). SQLCipher beskytter alle tabeller og indekser med 256-bit passord utledet direkte fra hardware Keystore."
            )
            Spacer(modifier = Modifier.height(12.dp))
            StackItem(
                title = "Biometrisk Sikring",
                badge = "BiometricPrompt (Fingeravtrykk & Ansikt)",
                desc = "Adgangskontroll til meldinger forsegles bak hardware-sikret biometri før meldinger vises. Støtter fallback til sikker PIN eller Duress PIN ved tvang."
            )
            Spacer(modifier = Modifier.height(12.dp))
            StackItem(
                title = "Zero-Knowledge Blind Relay Backend",
                badge = "Go / Rust / Node.js Microservice",
                desc = "Serveren fungerer utelukkende som en blind formidler. Den mottar kun ugjennomtrengelige konvolutter (IV + Ciphertext + Tag). Ingen IP-logging, ingen metadata-historikk, ephemeral in-memory queues (RAM-only Redis med TTL)."
            )
            Spacer(modifier = Modifier.height(12.dp))
            StackItem(
                title = "Kryptografisk Protokoll",
                badge = "ECDH (secp256r1) + AES-256-GCM + SHA-256 KDF",
                desc = "Asymmetrisk Elliptic Curve Diffie-Hellman nøkkelutveksling med symmetrisk Galois/Counter Mode for integritets- og autentisitetsbeskyttelse."
            )
        }
    }
}

@Composable
fun StackItem(title: String, badge: String, desc: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberEmerald,
                modifier = Modifier
                    .background(CyberEmerald.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = desc, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
    }
}

@Composable
fun FolderStructureSection() {
    Text(
        text = "MAPPESTRUKTUR & SYSTEMKOMPONENTER",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = CyberCyan
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val structure = """
/app/src/main/java/com/example/
├── crypto/
│   └── CryptoEngine.kt          <- ECDH, AES-256-GCM, KeyStore vault, shredder
├── model/
│   └── ChatModels.kt            <- UserAccount, ChatMessage, EphemeralTimer, DuressAction
├── data/
│   └── EncryptedVaultStorage.kt <- Kryptert lokal fil/database, decimering, decoy logikk
├── ui/
│   ├── ChatViewModel.kt         <- MVVM orkestrering, tidsur for selvsletting, klippetavle-wipe
│   ├── components/
│   │   └── QrCodeView.kt        <- Deterministisk visualisering av Safety Numbers / Keys
│   ├── screens/
│   │   ├── OnboardingScreen.kt  <- Valg mellom Anonym vs Profilert modus
│   │   ├── PinLockScreen.kt     <- Tastatur med Duress PIN deteksjon
│   │   ├── ChatListScreen.kt    <- Kanaler, mapper, søk, hurtignødsletting
│   │   ├── ConversationScreen.kt<- Levende brennende meldinger, E2EE-inspeksjon
│   │   ├── SettingsScreen.kt    <- Kill Button, Duress-oppsett, FLAG_SECURE
│   │   └── ArchitectureDocs.kt  <- Interaktiv teknisk dokumentasjon
│   └── theme/
│       ├── Color.kt             <- Stealth cyber dark fargepalett
│       └── Theme.kt             <- M3 mørkt tema og kant-til-kant
└── MainActivity.kt              <- FLAG_SECURE håndtering, livssykluslås
            """.trimIndent()

            Text(
                text = structure,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberCyan,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CryptographyArchitectureSection() {
    Text(
        text = "E2EE & ZERO-KNOWLEDGE LOGIKK",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = CyberCyan
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "1. Elliptic Curve Diffie-Hellman (ECDH):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Begge parter genererer et EC P-256 nøkkelpar. Når Alice sender til Bob, beregnes et felles hemmelighetstall via Alice sin private nøkkel og Bob sin offentlige nøkkel: S = ECDH(Alice_priv, Bob_pub). Symmetrinøkkel utledes via SHA-256(S).",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "2. AES-256-GCM (Authenticated Encryption):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hver melding krypteres med en unik, tilfeldig 96-biters initsieringsvektor (IV). GCM-modusen produserer en 128-biters autentiseringstag som verifiserer at meldingen ikke er manipulert i transitt.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "3. Sikkerhetsnummer (Safety Number / Fingerprint):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "60 sifre (12 blokker à 5 sifre) genereres fra SHA-512 hashen av de sorterte offentlige nøklene. Verifiseres utenfor båndet (out-of-band) for å utelukke MitM-angrep.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun EphemeralAndKillSection() {
    Text(
        text = "SELVSLETTING, KILL BUTTON & DURESS PIN",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = CyberRed
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔥 Selvslettende Meldinger (Ephemeral Lifecycle):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyberAmber
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Timer-innstillinger: Av, 5s, 30s, 1m, 1t, 24t.\n" +
                        "• Trigger-punkter: Ved sending (avsender) eller ved åpning (mottaker).\n" +
                        "• Bakgrunns-scheduler sjekker expiresAt hvert sekund og overskriver minne- og diskinnhold når tidsfristen er nådd.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "💥 Kill Button (Nødsletting / Panic Wipe):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyberRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1. KeyStore Purge: Sletter master- og sesjonsnøkler fra maskinvare-sikret lagring.\n" +
                        "2. Database Shred: Overskriver hvelvfilen med null-bytes (0x00) før sletting.\n" +
                        "3. Blind Relay Revocation: Sender en hash-signatur til blind-relay om å slette alle pending innboks-køer.\n" +
                        "4. App State Reset: Tømmer all RAM og returnerer til oppstartsskjerm.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "🛡️ Duress PIN (Nød-PIN / Falsk Decoy):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dersom brukeren tvinges til å taste inn koden: Å taste Duress PIN åpner en helt uskyldig, forhåndsdefinert profil (f.eks. oppskrifter, melk-og-brød chatter med 'Mamma'), eller utfører sletting i bakgrunnen uten visuelle varsler.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
