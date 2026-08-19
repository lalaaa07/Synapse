package com.example.synapse

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.synapse.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager
    private lateinit var musicManager: MusicManager

    private val requiredPermissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bleManager   = BleManager(applicationContext)
        musicManager = MusicManager(applicationContext)

        val sharedPrefs = getSharedPreferences("synapse_session", MODE_PRIVATE)

        setContent {
            // Load saved session details
            val savedEmail = remember { sharedPrefs.getString("email", "") ?: "" }
            val savedUsername = remember { sharedPrefs.getString("username", "") ?: "" }

            var currentScreen by remember { mutableStateOf("splash") }
            var loggedInEmail by remember { mutableStateOf(savedEmail) }
            var loggedInUsername by remember { mutableStateOf(savedUsername) }

            SynapseTheme {
                Crossfade(
                    targetState = currentScreen,
                    animationSpec = tween(durationMillis = 800),
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        "splash" -> {
                            SynapseSplash(
                                onTransitionComplete = {
                                    val isUserLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)
                                    currentScreen = if (isUserLoggedIn) "main" else "signup"
                                }
                            )
                        }
                        "signup" -> {
                            SignUpScreen(
                                onSignUpSuccess = { email, username ->
                                    // Save session persistently
                                    sharedPrefs.edit().apply {
                                        putString("email", email)
                                        putString("username", username)
                                        putBoolean("is_logged_in", true)
                                        apply()
                                    }
                                    loggedInEmail = email
                                    loggedInUsername = username
                                    currentScreen = "main"
                                }
                            )
                        }
                        "main" -> {
                            var permissionsGranted by remember { mutableStateOf(false) }

                            val permissionLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestMultiplePermissions()
                            ) { results ->
                                permissionsGranted = results.values.all { it }
                                if (permissionsGranted) bleManager.startScan()
                            }

                            // Prompts permissions immediately after transition to main dashboard
                            LaunchedEffect(Unit) { permissionLauncher.launch(requiredPermissions) }

                            val connectionState    by bleManager.connectionState.collectAsState()
                            val telemetry          by bleManager.telemetry.collectAsState()
                            val riskScore          by bleManager.riskScore.collectAsState()
                            val deviceAddress      by bleManager.deviceAddress.collectAsState()
                            val actionLog          by bleManager.actionLog.collectAsState()
                            val shouldPlayCalmingMusic by bleManager.shouldPlayCalmingMusic.collectAsState()

                            var manualMusicEnabled by remember { mutableStateOf(true) }
                            var musicVolume        by remember { mutableStateOf(1.0f) }

                            LaunchedEffect(shouldPlayCalmingMusic, manualMusicEnabled) {
                                if (shouldPlayCalmingMusic && manualMusicEnabled) musicManager.play()
                                else musicManager.stop()
                            }
                            LaunchedEffect(musicVolume) { musicManager.setVolume(musicVolume) }

                            var selectedTab by remember { mutableStateOf(0) }

                            // ── Full-screen warm gradient background ─────────────────────
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                WarmWhite,
                                                Color(0xFFF4F1EC),
                                                Color(0xFFEDE8E1)
                                            )
                                        )
                                    )
                            ) {
                                // ── Tab content area ─────────────────────────────────────
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                        .padding(bottom = 88.dp)
                                ) {
                                    when (selectedTab) {
                                        0 -> DeviceStatusTab(
                                            state         = connectionState,
                                            telemetry     = telemetry,
                                            deviceAddress = deviceAddress
                                        )
                                        1 -> EpisodeMonitorTab(
                                            telemetry  = telemetry,
                                            riskScore  = riskScore,
                                            actionLog  = actionLog
                                        )
                                        2 -> SettingsTab(
                                            onManualCommand   = { cmd -> bleManager.sendVibrationCommand(cmd) },
                                            initialName       = bleManager.getEmergencyContactName(),
                                            initialPhone      = bleManager.getEmergencyContactPhone(),
                                            initialMessage    = bleManager.getEmergencyContactMessage(),
                                            onSaveContact     = { n, p, m -> bleManager.saveEmergencyContact(n, p, m) },
                                            musicAutoEnabled  = manualMusicEnabled,
                                            onMusicAutoToggle = { manualMusicEnabled = it },
                                            musicVolume       = musicVolume,
                                            onMusicVolumeChange = { musicVolume = it },
                                            isMusicPlaying    = musicManager.isPlaying(),
                                            onFallThresholdChange = { value -> bleManager.sendFallThreshold(value) }
                                        )
                                        3 -> ProfileTab(
                                            username        = loggedInUsername,
                                            email           = loggedInEmail,
                                            deviceConnected = connectionState is BleConnectionState.Connected,
                                            onLogOut = {
                                                // Clear session state
                                                sharedPrefs.edit().apply {
                                                    putBoolean("is_logged_in", false)
                                                    apply()
                                                }
                                                currentScreen = "signup"
                                            }
                                        )
                                    }
                                }

                                // ── Bottom navigation ────────────────────────────────────
                                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                                    SynapseNavBar(
                                        selectedTab  = selectedTab,
                                        onTabSelected = { selectedTab = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.disconnect()
        musicManager.release()
    }
}

// ── Navigation Bar ─────────────────────────────────────────────────────────────

@Composable
fun SynapseNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val labels = listOf("Device", "Monitor", "Settings", "Profile")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Nav bar container — white card with shadow
        Box(
            modifier = Modifier
                .shadow(
                    elevation    = 8.dp,
                    shape        = RoundedCornerShape(32.dp),
                    ambientColor = Color(0xFF1A1A2E).copy(alpha = 0.10f),
                    spotColor    = Color(0xFF1A1A2E).copy(alpha = 0.10f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(SurfaceWhite)
                .border(1.dp, BorderSilver, RoundedCornerShape(32.dp))
                .padding(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                labels.forEachIndexed { index, label ->
                    val selected    = selectedTab == index
                    val activeColor = GemSapphire
                    val inactiveColor = TextHint

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                if (selected) GemSapphire.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { onTabSelected(index) }
                            .padding(horizontal = 14.dp, vertical = 10.dp), // reduced horizontal spacing to fit 4 tabs
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val iconColor = if (selected) activeColor else inactiveColor
                            when (index) {
                                0 -> SignalIcon(color = iconColor)
                                1 -> PulseIcon(color = iconColor)
                                2 -> SettingsIcon(color = iconColor)
                                3 -> ProfileIcon(color = iconColor)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text  = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) activeColor else inactiveColor,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Page Header ───────────────────────────────────────────────────────────────

@Composable
fun GlassHeader(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text      = text,
            style     = MaterialTheme.typography.headlineMedium,
            color     = TextPrimary,
            modifier  = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        // Accent underline — gem sapphire
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GemSapphire)
                .padding(horizontal = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 1 — DEVICE STATUS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DeviceStatusTab(state: BleConnectionState, telemetry: Telemetry?, deviceAddress: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassHeader("Device Status")

        // BLE Connection card
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Connection status dot
                    val isConnected = state is BleConnectionState.Connected
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isConnected) CalmRisk else TextHint)
                    )
                    Text("BLE Connection", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                val statusText = when (state) {
                    is BleConnectionState.Disconnected      -> "Disconnected"
                    is BleConnectionState.Scanning          -> "Scanning..."
                    is BleConnectionState.Connecting        -> "Connecting..."
                    is BleConnectionState.RequestingMtu     -> "Negotiating connection..."
                    is BleConnectionState.DiscoveringServices -> "Discovering services..."
                    is BleConnectionState.Connected         -> "Connected"
                    is BleConnectionState.Error             -> "Error: ${state.message}"
                }
                val statusColor = if (state is BleConnectionState.Connected)
                    CalmRisk else MaterialTheme.colorScheme.onSurfaceVariant

                Text(
                    text  = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text  = "MAC  ${deviceAddress ?: "—"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // IR Placement card
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("IR Placement", style = MaterialTheme.typography.titleMedium)
                val irOk = telemetry?.ir == true
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (irOk) CalmRisk.copy(alpha = 0.12f) else HighRisk.copy(alpha = 0.10f),
                    modifier = Modifier.border(
                        1.dp,
                        if (irOk) CalmRisk.copy(alpha = 0.40f) else HighRisk.copy(alpha = 0.30f),
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Text(
                        text     = if (irOk) "Placed" else "Not Placed",
                        color    = if (irOk) CalmRisk else HighRisk,
                        style    = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Obstacle Radar card
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Obstacle Radar", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                val dist    = telemetry?.dist ?: 0.0
                val isClose = dist in 0.1..50.0
                Text(
                    text  = "%.1f cm".format(dist),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (isClose) HighRisk else GemSapphire
                )
                if (isClose) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = HighRisk.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text     = "[!]",
                                color    = HighRisk,
                                style    = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text  = "Obstacle nearby",
                            color = HighRisk,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Self-Test card
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Self-Test", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerTint, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SelfTestRow("Motion sensor",     telemetry != null)
                SelfTestRow("Heart rate sensor", (telemetry?.bpm ?: 0) > 0)
                SelfTestRow("Distance sensor",   telemetry != null)
            }
        }
    }
}

@Composable
fun SelfTestRow(label: String, ok: Boolean) {
    Row(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (ok) CalmRisk.copy(alpha = 0.10f) else HighRisk.copy(alpha = 0.08f)
        ) {
            Text(
                text      = if (ok) "[OK]" else "[--]",
                color     = if (ok) CalmRisk else HighRisk,
                style     = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier  = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 2 — EPISODE MONITOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EpisodeMonitorTab(telemetry: Telemetry?, riskScore: Int, actionLog: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassHeader("Episode Monitor")

        // Risk gauge card
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Episode Risk Score",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
                RiskGauge(score = riskScore)
            }
        }

        // Heart Rate card
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Heart Rate", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("live", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartbeatPulse(bpm = telemetry?.bpm ?: 0)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text  = "${telemetry?.bpm ?: 0}",
                        style = MaterialTheme.typography.displayMedium,
                        color = RoseQuartz
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = "bpm",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Bottom).padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Motion indicators row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MotionIndicator("Tremor",    telemetry?.tremor == true, Modifier.weight(1f))
            MotionIndicator("Agitation", telemetry?.agit   == true, Modifier.weight(1f))
            MotionIndicator("Fall",      telemetry?.fall   == true, Modifier.weight(1f))
        }

        // Status feed
        Text(
            "Status Feed",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        SynapseCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (actionLog.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No activity recorded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHint
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(actionLog) { entry ->
                        Row(
                            modifier = Modifier.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text  = ">",
                                style = MaterialTheme.typography.labelSmall,
                                color = GemSapphire,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                entry,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MotionIndicator(label: String, active: Boolean, modifier: Modifier = Modifier) {
    SynapseCard(
        modifier = modifier.then(
            if (active) Modifier.border(
                width = 1.5.dp,
                color = HighRisk.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) else Modifier
        )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = if (active) "ACTIVE" else "—",
                color = if (active) HighRisk else TextHint,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 3 — SETTINGS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsTab(
    onManualCommand: (String) -> Unit,
    initialName: String,
    initialPhone: String,
    initialMessage: String,
    onSaveContact: (String, String, String) -> Unit,
    musicAutoEnabled: Boolean,
    onMusicAutoToggle: (Boolean) -> Unit,
    musicVolume: Float,
    onMusicVolumeChange: (Float) -> Unit,
    isMusicPlaying: Boolean,
    onFallThresholdChange: (Double) -> Unit
) {
    var name      by remember { mutableStateOf(initialName) }
    var phone     by remember { mutableStateOf(initialPhone) }
    var message   by remember { mutableStateOf(initialMessage) }
    var justSaved by remember { mutableStateOf(false) }
    var fallG     by remember { mutableStateOf(2.2) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = GemSapphire,
        unfocusedBorderColor = BorderSilver,
        focusedLabelColor    = GemSapphire,
        cursorColor          = GemSapphire
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassHeader("Settings")

        // Manual Vibrator Test
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Manual Vibrator Test", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Send a test pattern to the wristband.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(14.dp))

                val commands = listOf("calm", "pulse", "breathe", "grounding", "alert")
                commands.forEach { command ->
                    OutlinedButton(
                        onClick = { onManualCommand(command) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape  = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GemSapphire
                        ),
                        border = BorderStroke(1.dp, GemSapphire.copy(alpha = 0.40f))
                    ) {
                        Text(
                            command.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Fall Sensitivity
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text("Fall Sensitivity", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
            "Lower = more sensitive, higher = fewer false alarms.",
            style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
              OutlinedButton(
                onClick = {
                    fallG = (fallG - 0.2).coerceAtLeast(1.5)
                    onFallThresholdChange(fallG)
                },
                shape  = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GemSapphire),
                border = BorderStroke(1.dp, GemSapphire.copy(alpha = 0.40f))
            ) { Text("−") }

            Spacer(modifier = Modifier.width(20.dp))
            Text(
                "%.1fg".format(fallG),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(20.dp))

            OutlinedButton(
                onClick = {
                    fallG = (fallG + 0.2).coerceAtMost(4.0)
                    onFallThresholdChange(fallG)
                },
                shape  = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GemSapphire),
                border = BorderStroke(1.dp, GemSapphire.copy(alpha = 0.40f))
              ) { Text("+") }
            }
          }
        }

        // Emergency Contact
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Emergency Contact", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Sent an SMS automatically when a high-risk episode is detected.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; justSaved = false },
                    label         = { Text("Contact Name") },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = fieldColors,
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value         = phone,
                    onValueChange = { phone = it; justSaved = false },
                    label         = { Text("Phone Number") },
                    placeholder   = { Text("+1 234 567 8900") },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = fieldColors,
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value         = message,
                    onValueChange = { message = it; justSaved = false },
                    label         = { Text("Custom Message (optional)") },
                    placeholder   = { Text("SYNAPSE Alert: possible episode detected.") },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = fieldColors,
                    minLines      = 2,
                    shape         = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onSaveContact(name, phone, message)
                        justSaved = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (justSaved) CalmRisk else GemSapphire,
                        contentColor   = SurfaceWhite
                    )
                ) {
                    Text(
                        if (justSaved) "[OK]  Saved" else "Save Contact",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (phone.isBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HighRisk.copy(alpha = 0.07f))
                            .border(1.dp, HighRisk.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("[!]", color = HighRisk, fontWeight = FontWeight.Bold)
                        Text(
                            "No phone number set — emergency SMS will not be sent until this is filled in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighRisk
                        )
                    }
                }
            }
        }

        // Calming Audio
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Calming Audio", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto-play during high risk",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Activates when episode risk score >= 5",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked         = musicAutoEnabled,
                        onCheckedChange = onMusicAutoToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = SurfaceWhite,
                            checkedTrackColor  = GemSapphire,
                            uncheckedThumbColor = TextHint,
                            uncheckedTrackColor = BorderSilver
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DividerTint, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Track — Interstellar (calming_music.mp3)",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Volume", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${(musicVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = GemSapphire,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value         = musicVolume,
                    onValueChange = onMusicVolumeChange,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = SliderDefaults.colors(
                        thumbColor             = GemSapphire,
                        activeTrackColor       = GemSapphire,
                        inactiveTrackColor     = BorderSilver
                    )
                )

                if (isMusicPlaying) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CalmRisk.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            ">>",
                            color = CalmRisk,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "Now playing",
                            color = CalmRisk,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// PAGE 4 — PROFILE TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileTab(
    username: String,
    email: String,
    deviceConnected: Boolean,
    onLogOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassHeader("Profile")

        // Profile Details Card
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Initials Avatar with deep-violet gradient
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GemSapphire, Color(0xFF818CF8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (username.isNotEmpty()) username.take(1).uppercase() else "U",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = username.ifEmpty { "Synapse Wearer" },
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email.ifEmpty { "user@synapse.com" },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Diagnostics Card
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("System Diagnostics", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerTint, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Device Connection", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (deviceConnected) "Connected" else "Disconnected",
                        color = if (deviceConnected) CalmRisk else TextHint,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Monitoring Status", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Active",
                        color = CalmRisk,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Log Out Button
        Button(
            onClick = onLogOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HighRisk,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Log Out",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}