package com.example.synapse

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "BleManager"
private const val TARGET_DEVICE_NAME = "SYNAPSE"
private const val PREFS_NAME = "synapse_prefs"
private const val HIGH_RISK_THRESHOLD = 6

val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
val TELEMETRY_RX_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
val VIBRATION_WRITE_UUID: UUID = UUID.fromString("6e400004-b5a3-f393-e0a9-e50e24dcca9e")
val SETTINGS_WRITE_UUID: UUID = UUID.fromString("6e400006-b5a3-f393-e0a9-e50e24dcca9e")
val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Scanning : BleConnectionState()
    object Connecting : BleConnectionState()
    object RequestingMtu : BleConnectionState()
    object DiscoveringServices : BleConnectionState()
    object Connected : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

@Serializable
data class Telemetry(
    val bpm: Int = 0,
    val ax: Double = 0.0,
    val ay: Double = 0.0,
    val az: Double = 0.0,
    val gx: Double = 0.0,
    val gy: Double = 0.0,
    val gz: Double = 0.0,
    val dist: Double = 0.0,
    val ir: Boolean = false,
    val tremor: Boolean = false,
    val agit: Boolean = false,
    val fall: Boolean = false,
    val still: Boolean = false,
    val score: Int = 0,
    val motor: Boolean = false,
    val upme: Long = 0
)

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner get() = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var vibrationCharacteristic: BluetoothGattCharacteristic? = null
    private var settingsCharacteristic: BluetoothGattCharacteristic? = null

    private val json = Json { ignoreUnknownKeys = true }
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val smsLocationHelper = SmsLocationHelper(context)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var vibrationSequenceJob: Job? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _telemetry = MutableStateFlow<Telemetry?>(null)
    val telemetry: StateFlow<Telemetry?> = _telemetry

    private val _riskScore = MutableStateFlow(0)
    val riskScore: StateFlow<Int> = _riskScore

    private val _deviceAddress = MutableStateFlow<String?>(null)
    val deviceAddress: StateFlow<String?> = _deviceAddress

    private val _actionLog = MutableStateFlow<List<String>>(emptyList())
    val actionLog: StateFlow<List<String>> = _actionLog

    private val _shouldPlayCalmingMusic = MutableStateFlow(false)
    val shouldPlayCalmingMusic: StateFlow<Boolean> = _shouldPlayCalmingMusic

    private var isCurrentlyHighRisk = false
    private var highRiskAlertSent = false
    private var pendingHighRiskSince: Long? = null
    private val SUSTAINED_THRESHOLD_MS = 3000L // signal must persist this long before we act (fall is exempt)
    private var isIntentionalDisconnect = false
    private var reconnectJob: Job? = null
    private val RECONNECT_DELAY_MS = 4000L

    private fun logAction(message: String) {
        val entry = "${timeFormat.format(Date())} — $message"
        _actionLog.value = (listOf(entry) + _actionLog.value).take(15)
    }

    // ---------------- Emergency contact persistence ----------------

    fun saveEmergencyContact(name: String, phone: String, message: String) {
        prefs.edit()
            .putString("contact_name", name)
            .putString("contact_phone", phone)
            .putString("contact_message", message)
            .apply()
    }

    fun getEmergencyContactName(): String = prefs.getString("contact_name", "") ?: ""
    fun getEmergencyContactPhone(): String = prefs.getString("contact_phone", "") ?: ""
    fun getEmergencyContactMessage(): String = prefs.getString("contact_message", "") ?: ""

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = managerScope.launch {
            delay(RECONNECT_DELAY_MS)
            Log.d(TAG, "Attempting auto-reconnect...")
            startScan()
        }
    }

    // ---------------- Scanning / Connection ----------------

    fun startScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            _connectionState.value = BleConnectionState.Error("Bluetooth is off")
            return
        }
        Log.d(TAG, "Starting scan for $TARGET_DEVICE_NAME")
        _connectionState.value = BleConnectionState.Scanning
        logAction("Scanning for $TARGET_DEVICE_NAME...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner?.startScan(null, settings, scanCallback)
    }

    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name ?: return
            if (deviceName.equals(TARGET_DEVICE_NAME, ignoreCase = true)) {
                Log.d(TAG, "Target device found! Connecting...")
                stopScan()
                connect(result.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
            _connectionState.value = BleConnectionState.Error("Scan failed: $errorCode")
        }
    }

    private fun connect(device: BluetoothDevice) {
        _connectionState.value = BleConnectionState.Connecting
        _deviceAddress.value = device.address
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.RequestingMtu
                    val mtuRequested = gatt.requestMtu(247)
                    if (!mtuRequested) {
                        _connectionState.value = BleConnectionState.DiscoveringServices
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.value = BleConnectionState.Disconnected
                    bluetoothGatt = null
                    vibrationCharacteristic = null
                    settingsCharacteristic = null
                    stopVibrationSequence()
                    isCurrentlyHighRisk = false
                    highRiskAlertSent = false
                    _deviceAddress.value = null

                    if (isIntentionalDisconnect) {
                        logAction("Disconnected")
                    } else {
                        logAction("[!] Connection lost — attempting to reconnect...")
                        scheduleReconnect()
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            _connectionState.value = BleConnectionState.DiscoveringServices
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Error("Service discovery failed")
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                _connectionState.value = BleConnectionState.Error("Service UUID not found")
                return
            }

            vibrationCharacteristic = service.getCharacteristic(VIBRATION_WRITE_UUID)
            settingsCharacteristic = service.getCharacteristic(SETTINGS_WRITE_UUID)

            val telemetryChar = service.getCharacteristic(TELEMETRY_RX_UUID)
            if (telemetryChar == null) {
                _connectionState.value = BleConnectionState.Error("Telemetry characteristic not found")
                return
            }

            gatt.setCharacteristicNotification(telemetryChar, true)
            val descriptor = telemetryChar.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            } else {
                _connectionState.value = BleConnectionState.Error("Notification descriptor not found")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Connected
                logAction("Connected to SYNAPSE")
            } else {
                _connectionState.value = BleConnectionState.Error("Failed to enable notifications")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == TELEMETRY_RX_UUID) {
                val rawBytes = characteristic.value
                val jsonString = String(rawBytes, Charsets.UTF_8)
                Log.d(TAG, "Raw telemetry: $jsonString")

                try {
                    val parsed = json.decodeFromString<Telemetry>(jsonString)
                    _telemetry.value = parsed

                    val score = calculateRiskScore(parsed)
                    _riskScore.value = score

                    evaluateHighRiskActions(score, parsed)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse telemetry JSON: $jsonString", e)
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.d(TAG, "onCharacteristicWrite: uuid=${characteristic.uuid} status=$status")
        }
    }

    // ---------------- High-risk detection: vibration sequence + SMS + Music ----------------

    /**
     * Triggers once per episode (edge-detected on isHighRisk transitioning false -> true):
     * - Starts the de-escalation vibration sequence
     * - Sends emergency SMS with GPS location
     * - Signals calming music to start
     *
     * When risk drops back below threshold, stops the vibration sequence and music.
     */
    private fun evaluateHighRiskActions(score: Int, telemetry: Telemetry) {
        val rawIsHighRisk = score >= HIGH_RISK_THRESHOLD || telemetry.fall

        // Fall detection is exempt from debouncing — always act immediately
        val confirmedHighRisk: Boolean = when {
            telemetry.fall -> true
            rawIsHighRisk -> {
                val now = System.currentTimeMillis()
                if (pendingHighRiskSince == null) {
                    pendingHighRiskSince = now
                    Log.d(TAG, "Elevated signal detected, waiting to confirm (debouncing)...")
                }
                val elapsed = now - (pendingHighRiskSince ?: now)
                elapsed >= SUSTAINED_THRESHOLD_MS
            }
            else -> {
                pendingHighRiskSince = null
                false
            }
        }

        if (confirmedHighRisk && !isCurrentlyHighRisk) {
            isCurrentlyHighRisk = true
            _shouldPlayCalmingMusic.value = true
            startVibrationSequence()

            if (!highRiskAlertSent) {
                val reason = if (telemetry.fall) "Fall detected" else "High risk score ($score/10)"
                val phone = getEmergencyContactPhone()
                val message = getEmergencyContactMessage()

                if (phone.isBlank()) {
                    logAction("[!] High risk ($reason) but no emergency contact configured")
                } else {
                    smsLocationHelper.sendEmergencySms(phone, message, reason)
                    logAction("[>>] Emergency SMS sent: $reason")
                }
                highRiskAlertSent = true
            }
        } else if (!rawIsHighRisk && isCurrentlyHighRisk) {
            isCurrentlyHighRisk = false
            highRiskAlertSent = false
            pendingHighRiskSince = null
            _shouldPlayCalmingMusic.value = false
            stopVibrationSequence()
            logAction("Risk score normalized — episode ended")
        }
    }

    /**
     * De-escalation sequence: alert -> calm -> pulse (each brief),
     * then breathe repeating until the episode ends (job gets cancelled).
     */
    private fun startVibrationSequence() {
        stopVibrationSequence() // safety: cancel any lingering job first

        vibrationSequenceJob = managerScope.launch {
            sendVibrationCommand("alert")
            delay(3000)

            sendVibrationCommand("calm")
            delay(3000)

            sendVibrationCommand("pulse")
            delay(3000)

            // Breathe cycle repeats until this coroutine is cancelled
            while (isActive) {
                sendVibrationCommand("breathe")
                delay(5000)
            }
        }
        logAction("Started de-escalation sequence")
    }

    private fun stopVibrationSequence() {
        vibrationSequenceJob?.cancel()
        vibrationSequenceJob = null
    }

    fun sendVibrationCommand(command: String) {
        val char = vibrationCharacteristic
        val gatt = bluetoothGatt
        if (char == null || gatt == null) {
            Log.w(TAG, "Cannot send command '$command' — not connected")
            return
        }
        char.value = command.toByteArray(Charsets.UTF_8)
        gatt.writeCharacteristic(char)
        Log.d(TAG, "sendVibrationCommand('$command')")
    }
    
    fun sendSettings(settings: Map<String, Double>) {
      val char = settingsCharacteristic
      val gatt = bluetoothGatt
      if (char == null || gatt == null) {
            Log.w(TAG, "Cannot send settings — not connected")
            return
    }
      val jsonString = Json.encodeToString(settings)
      char.value = jsonString.toByteArray(Charsets.UTF_8)
      gatt.writeCharacteristic(char)
      Log.d(TAG, "sendSettings($jsonString)")
      logAction("Settings updated: $jsonString")
    }

    fun sendFallThreshold(value: Double) {
        sendSettings(mapOf("fall_g" to value))
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        vibrationCharacteristic = null
        settingsCharacteristic = null
        stopVibrationSequence()
        managerScope.cancel()
        isCurrentlyHighRisk = false
        highRiskAlertSent = false
        _deviceAddress.value = null
    }
}
