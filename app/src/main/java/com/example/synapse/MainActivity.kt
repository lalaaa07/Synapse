package com.example.synapse

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.synapse.ui.theme.SynapseTheme
import androidx.compose.material3.Slider
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.navigationBarsPadding

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

        bleManager = BleManager(applicationContext)
        musicManager = MusicManager(applicationContext)

        setContent {
            SynapseTheme {
                var permissionsGranted by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    permissionsGranted = results.values.all { it }
                    if (permissionsGranted) {
                        bleManager.startScan()
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(requiredPermissions)
                }

                val connectionState by bleManager.connectionState.collectAsState()
                val telemetry by bleManager.telemetry.collectAsState()
                val riskScore by bleManager.riskScore.collectAsState()
                val deviceAddress by bleManager.deviceAddress.collectAsState()
                val actionLog by bleManager.actionLog.collectAsState()
                val shouldPlayCalmingMusic by bleManager.shouldPlayCalmingMusic.collectAsState()

                var manualMusicEnabled by remember { mutableStateOf(true) } // user can disable auto-music entirely
                var musicVolume by remember { mutableStateOf(1.0f) }

                LaunchedEffect(shouldPlayCalmingMusic, manualMusicEnabled) {
                    if (shouldPlayCalmingMusic && manualMusicEnabled) {
                        musicManager.play()
                    } else {
                        musicManager.stop()
                    }
                }

                LaunchedEffect(musicVolume) {
                    musicManager.setVolume(musicVolume)
                }

                var selectedTab by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = com.example.synapse.ui.theme.BackgroundDark,
                    bottomBar = {
                        SynapseNavBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        com.example.synapse.ui.theme.BackgroundDark,
                                        com.example.synapse.ui.theme.SurfaceDark.copy(alpha = 0.4f)
                                    )
                                )
                            )
                            .fillMaxSize()
                    ) {
                        when (selectedTab) {
                            0 -> DeviceStatusTab(
                                state = connectionState,
                                telemetry = telemetry,
                                deviceAddress = deviceAddress
                            )
                            1 -> EpisodeMonitorTab(
                                telemetry = telemetry,
                                riskScore = riskScore,
                                actionLog = actionLog
                            )
                            2 -> SettingsTab(
                                onManualCommand = { command -> bleManager.sendVibrationCommand(command) },
                                initialName = bleManager.getEmergencyContactName(),
                                initialPhone = bleManager.getEmergencyContactPhone(),
                                initialMessage = bleManager.getEmergencyContactMessage(),
                                onSaveContact = { name, phone, message -> bleManager.saveEmergencyContact(name, phone, message) },
                                musicAutoEnabled = manualMusicEnabled,
                                onMusicAutoToggle = { manualMusicEnabled = it },
                                musicVolume = musicVolume,
                                onMusicVolumeChange = { musicVolume = it },
                                isMusicPlaying = musicManager.isPlaying()
                            )
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

data class NavTab(val label: String, val emoji: String)

@Composable
fun SynapseNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val labels = listOf("Device", "Monitor", "Settings")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(com.example.synapse.ui.theme.SurfaceDark)
                .border(
                    width = 1.dp,
                    color = com.example.synapse.ui.theme.BorderDark,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val selected = selectedTab == index
                val accentColor = com.example.synapse.ui.theme.SageAccent

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val iconColor = if (selected) accentColor else com.example.synapse.ui.theme.TextSecondary
                    when (index) {
                        0 -> SignalIcon(color = iconColor)
                        1 -> PulseIcon(color = iconColor)
                        2 -> SettingsIcon(color = iconColor)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = iconColor
                    )
                }
            }
        }
    }
}

// ---------------------- PAGE 1: DEVICE STATUS ----------------------

@Composable
fun DeviceStatusTab(state: BleConnectionState, telemetry: Telemetry?, deviceAddress: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Device Status", style = MaterialTheme.typography.headlineMedium)

        // BLE Connection Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BLE Connection", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val statusText = when (state) {
                    is BleConnectionState.Disconnected -> "Disconnected"
                    is BleConnectionState.Scanning -> "Scanning..."
                    is BleConnectionState.Connecting -> "Connecting..."
                    is BleConnectionState.RequestingMtu -> "Negotiating connection..."
                    is BleConnectionState.DiscoveringServices -> "Discovering services..."
                    is BleConnectionState.Connected -> "Connected"
                    is BleConnectionState.Error -> "Error: ${state.message}"
                }
                val statusColor = if (state is BleConnectionState.Connected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant

                Text(text = statusText, color = statusColor, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MAC: ${deviceAddress ?: "—"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // IR Placement Indicator
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("IR Placement", style = MaterialTheme.typography.titleMedium)
                val irOk = telemetry?.ir == true
                Badge(
                    containerColor = if (irOk) Color(0xFF2E7D32) else Color(0xFFB71C1C)
                ) {
                    Text(if (irOk) "OK" else "Not Placed")
                }
            }
        }

        // Obstacle Radar
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Obstacle Radar", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val dist = telemetry?.dist ?: 0.0
                val isClose = dist in 0.1..50.0
                Text(
                    text = "${dist} cm",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isClose) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurface
                )
                if (isClose) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⚠️ Obstacle nearby", color = Color(0xFFB71C1C))
                }
            }
        }

        // Self-Test Dashboard
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Self-Test", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SelfTestRow("Motion sensor", telemetry != null)
                SelfTestRow("Heart rate sensor", (telemetry?.bpm ?: 0) > 0)
                SelfTestRow("Distance sensor", telemetry != null)
            }
        }
    }
}

@Composable
fun SelfTestRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(if (ok) "✅ OK" else "❌ No data", color = if (ok) Color(0xFF2E7D32) else Color(0xFFB71C1C))
    }
}

// ---------------------- PAGE 2: EPISODE MONITOR ----------------------

@Composable
fun EpisodeMonitorTab(telemetry: Telemetry?, riskScore: Int, actionLog: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Episode Monitor", style = MaterialTheme.typography.headlineMedium)

        // Risk Score Gauge
        // Risk Score Gauge — signature element
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Episode Risk Score", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                RiskGauge(score = riskScore)
            }
        }

// BPM Card with animated heartbeat
        SynapseCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Heart Rate", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeartbeatPulse(bpm = telemetry?.bpm ?: 0)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${telemetry?.bpm ?: 0} bpm", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        // Motion Telemetry Cards
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MotionIndicator("Tremor", telemetry?.tremor == true, Modifier.weight(1f))
            MotionIndicator("Agitation", telemetry?.agit == true, Modifier.weight(1f))
            MotionIndicator("Fall", telemetry?.fall == true, Modifier.weight(1f))
        }

        // Status Feed
        Text("Status Feed", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (actionLog.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No activity yet", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    items(actionLog) { entry ->
                        Text(entry, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MotionIndicator(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val glowColor = com.example.synapse.ui.theme.CoralRisk
    SynapseCard(
        modifier = modifier.then(
            if (active) Modifier.border(
                width = 1.5.dp,
                color = glowColor.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium
            ) else Modifier
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (active) "ACTIVE" else "—",
                color = if (active) glowColor else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// ---------------------- PAGE 3: SETTINGS ----------------------

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
    isMusicPlaying: Boolean
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var message by remember { mutableStateOf(initialMessage) }
    var justSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Calming Settings", style = MaterialTheme.typography.headlineMedium)

        // Manual Vibrator Test Dashboard
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Manual Vibrator Test", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                val commands = listOf("calm", "pulse", "breathe", "grounding", "alert")
                commands.forEach { command ->
                    Button(
                        onClick = { onManualCommand(command) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(command.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }

        // Emergency Contact Form
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Emergency Contact", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; justSaved = false },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; justSaved = false },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1234567890") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; justSaved = false },
                    label = { Text("Custom Message (optional)") },
                    placeholder = { Text("SYNAPSE Alert: possible episode detected.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSaveContact(name, phone, message)
                        justSaved = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (justSaved) "✅ Saved" else "Save Contact")
                }

                if (phone.isBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ No phone number set — emergency SMS won't be sent until this is filled in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB71C1C)
                    )
                }
            }
        }

        // Audio Control Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Calming Audio", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-play during high risk")
                    Switch(checked = musicAutoEnabled, onCheckedChange = onMusicAutoToggle)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Track: Interstellar (calming_music.mp3)", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(12.dp))
                Text("Volume", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = musicVolume,
                    onValueChange = onMusicVolumeChange,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isMusicPlaying) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🎵 Now playing", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}