/*
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║            SYNAPSE WEARABLE — COMPLETE BLE FIRMWARE             ║
 * ║                                                                  ║
 * ║  WHAT THIS FILE DOES                                             ║
 * ║  ─────────────────────────────────────────────────────────────  ║
 * ║  TRANSMITS to app (every 400 ms via BLE NOTIFY):                ║
 * ║    • MPU-6050  → acceleration (ax/ay/az) + gyroscope (gx/gy/gz) ║
 * ║    • Pulse     → heart rate BPM (median-filtered)               ║
 * ║    • IR sensor → proximity boolean (object detected true/false)  ║
 * ║    • HC-SR04   → distance in cm                                  ║
 * ║    • Computed  → tremor flag, agitation flag, fall flag,         ║
 * ║                  episode risk score (0-6)                        ║
 * ║                                                                  ║
 * ║  RECEIVES from app (via BLE WRITE):                              ║
 * ║    • Motor commands → calm / pulse / breathe / alert / stop      ║
 * ║    • Custom PWM     → "pwm:180" sets exact motor power           ║
 * ║    • Speech result  → app sends back speech anomaly score        ║
 * ║    • Settings       → app can tune thresholds at runtime         ║
 * ║                                                                  ║
 * ║  HARDWARE                                                        ║
 * ║    GPIO 4  → IR sensor OUT                                       ║
 * ║    GPIO 5  → HC-SR04 TRIG                                        ║
 * ║    GPIO 18 → HC-SR04 ECHO (via 1kΩ+1kΩ voltage divider)         ║
 * ║    GPIO 21 → MPU-6050 SDA  (I2C)                                 ║
 * ║    GPIO 22 → MPU-6050 SCL  (I2C)                                 ║
 * ║    GPIO 34 → Pulse sensor signal  (ADC1 — input only)            ║
 * ║    GPIO 26 → BC547 base (via 1kΩ) → vibration motor             ║
 * ║                                                                  ║
 * ║  LIBRARIES — install via Arduino Library Manager                 ║
 * ║    • MPU6050_light  by rfetick                                    ║
 * ║    • ESP32 BLE Arduino (ships with ESP32 board package)          ║
 * ║                                                                  ║
 * ║  BOARD SETTING: ESP32 Dev Module                                 ║
 * ║  BAUD RATE:     115200                                           ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * ══════════════════════════════════════════════════════════════════
 * HOW BLE WORKS ON THE ESP32 — READ THIS BEFORE EDITING
 * ══════════════════════════════════════════════════════════════════
 *
 * BLE uses a GATT (Generic ATTribute) model. Think of it like a
 * small database running on the ESP32 that the phone can read/write:
 *
 *   SERVICE  (a named group of related data)
 *   └── CHARACTERISTIC  (one data field inside the service)
 *         ├── UUID        (unique ID — like a column name)
 *         ├── VALUE       (the actual data bytes)
 *         ├── PROPERTIES  (what the phone can do with it)
 *         │     READ   = phone asks for current value
 *         │     NOTIFY = ESP32 pushes value automatically
 *         │     WRITE  = phone sends data to ESP32
 *         └── DESCRIPTOR  BLE2902 enables NOTIFY subscription
 *
 * DATA FLOW:
 *   ESP32 → App  : charSensor  (NOTIFY every 400 ms — sensor JSON)
 *   ESP32 → App  : charAlert   (NOTIFY on events — alert JSON)
 *   App   → ESP32: charMotor   (WRITE — motor command string)
 *   App   → ESP32: charSpeech  (WRITE — speech analysis result JSON)
 *   App   → ESP32: charSettings(WRITE — runtime threshold changes)
 *
 * PACKET FORMAT (all JSON strings, max ~240 bytes each):
 *
 *   charSensor NOTIFY:
 *   {
 *     "bpm":72,              ← heart rate
 *     "ax":0.01,             ← accel X in g
 *     "ay":-0.02,            ← accel Y in g
 *     "az":0.99,             ← accel Z in g
 *     "gx":0.5,              ← gyro X in °/s
 *     "gy":-0.3,             ← gyro Y in °/s
 *     "gz":0.1,              ← gyro Z in °/s
 *     "dist":120.5,          ← ultrasonic distance cm (-1 = no reading)
 *     "ir":false,            ← IR sensor triggered
 *     "tremor":false,        ← tremor detected
 *     "agit":false,          ← sustained agitation
 *     "fall":false,          ← fall detected
 *     "still":false,         ← prolonged stillness
 *     "score":0,             ← episode risk score 0-6
 *     "motor":false,         ← motor currently running
 *     "uptime":12345         ← ms since boot
 *   }
 *
 *   charAlert NOTIFY:
 *   {"type":"FALL","detail":"sudden_accel","score":5,"ts":12345}
 *
 *   charMotor WRITE (app → ESP32):
 *   "calm"         → slow breathing pattern (2s on, 1.5s off, 6 reps)
 *   "pulse"        → short taps (300ms on, 300ms off, 8 reps)
 *   "breathe"      → smooth ramp up/down (blocking 15s sequence)
 *   "alert"        → rapid strong buzz (100ms on/off, 6 reps)
 *   "grounding"    → grounding rhythm (500ms on, 500ms off, 8 reps)
 *   "stop"         → immediately stop motor
 *   "pwm:NNN"      → set raw PWM 0-255 (e.g. "pwm:180")
 *   "ping"         → device responds with pong on charAlert
 *
 *   charSpeech WRITE (app → ESP32):
 *   {"anomaly":true,"keyword":"afraid","score":7}
 *
 *   charSettings WRITE (app → ESP32):
 *   {"hr_high":130,"hr_low":48,"prox_cm":60,"tremor":3.0}
 */

// ══════════════════════════════════════════════════════════════════
// INCLUDES
// ══════════════════════════════════════════════════════════════════
#include <Wire.h>            // I2C for MPU-6050
#include <MPU6050_light.h>   // MPU-6050 driver (rfetick library)
#include <BLEDevice.h>       // ESP32 BLE stack
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>         // CCCD descriptor — enables NOTIFY
#include <math.h>

// ══════════════════════════════════════════════════════════════════
// PIN DEFINITIONS
// ══════════════════════════════════════════════════════════════════
#define PIN_IR          4    // IR sensor digital output (active LOW)
#define PIN_TRIG        5    // HC-SR04 trigger
#define PIN_ECHO       18    // HC-SR04 echo (via 1kΩ+1kΩ divider → 2.5V)
#define PIN_PULSE      34    // Pulse sensor analog (ADC1 — GPIO 32-39 only)
#define PIN_MOTOR      26    // BC547 base resistor input (PWM)
#define PIN_SDA        21    // MPU-6050 SDA (ESP32 I2C default)
#define PIN_SCL        22    // MPU-6050 SCL (ESP32 I2C default)

// ══════════════════════════════════════════════════════════════════
// PWM / MOTOR SETTINGS
// ══════════════════════════════════════════════════════════════════
#define MOTOR_LEDC_CH   0    // LEDC channel 0 (ESP32 has 16 channels)
#define MOTOR_LEDC_FREQ 5000 // 5 kHz — above hearing range, smooth vibration
#define MOTOR_LEDC_RES  8    // 8-bit resolution → duty cycle 0–255

// ══════════════════════════════════════════════════════════════════
// BLE IDENTIFIERS
// These UUIDs must match EXACTLY in your React Native app.
// They look like GUIDs — you can generate your own at uuidgenerator.net
// ══════════════════════════════════════════════════════════════════
#define BLE_DEVICE_NAME     "Synapse"

// One service groups all characteristics together
#define UUID_SERVICE        "6e400001-b5a3-f393-e0a9-e50e24dcca9e"

// ESP32 → App  (NOTIFY — device pushes data automatically)
#define UUID_CHAR_SENSOR    "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
#define UUID_CHAR_ALERT     "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

// App → ESP32  (WRITE — app sends commands to device)
#define UUID_CHAR_MOTOR     "6e400004-b5a3-f393-e0a9-e50e24dcca9e"
#define UUID_CHAR_SPEECH    "6e400005-b5a3-f393-e0a9-e50e24dcca9e"
#define UUID_CHAR_SETTINGS  "6e400006-b5a3-f393-e0a9-e50e24dcca9e"

// ══════════════════════════════════════════════════════════════════
// DETECTION THRESHOLDS  (can be overridden at runtime via BLE)
// ══════════════════════════════════════════════════════════════════
float   THRESH_PROXIMITY_CM   = 60.0f;  // ultrasonic alert distance
float   THRESH_FALL_G         = 2.2f;   // total accel for fall detection
float   THRESH_TREMOR_DPS     = 3.0f;   // gyro delta for tremor
int     THRESH_HR_LOW         = 48;     // BPM — bradycardia
int     THRESH_HR_HIGH        = 130;    // BPM — tachycardia
unsigned long THRESH_STILL_MS = 25000;  // ms of no movement before alert
int     THRESH_EPISODE        = 3;      // signals needed to flag episode

// ══════════════════════════════════════════════════════════════════
// TIMING
// ══════════════════════════════════════════════════════════════════
#define TELEMETRY_INTERVAL_MS   400   // how often sensor data is sent
#define ALERT_CHECK_INTERVAL_MS 300   // how often alert conditions checked
#define ULTRASONIC_INTERVAL_MS   80   // HC-SR04 max ~12 Hz
#define EPISODE_DEBOUNCE_MS   60000   // minimum gap between episode alerts

// ══════════════════════════════════════════════════════════════════
// HAPTIC MOTOR PATTERN DEFINITION
// Each pattern has:
//   onMs      = how long motor runs per burst (milliseconds)
//   offMs     = how long motor pauses between bursts
//   reps      = how many on/off cycles
//   intensity = PWM duty cycle 0-255 (motor power)
//   name      = label for Serial debug
// ══════════════════════════════════════════════════════════════════

// Function Prototypes
struct HapticPattern {
  int         onMs;
  int         offMs;
  int         reps;
  int         intensity;
  const char *name;
};

// Function Prototypes
void startMotorPattern(const HapticPattern &p);
void playBreathingRamp(int cycles);
void stopMotor();
void recalcEpisodeScore();

// Pre-defined patterns — tuned for calming effect
const HapticPattern PAT_CALM = {
  2000, 1500, 6, 110, "calm"
  // 2 s on, 1.5 s off, 6 cycles — mimics slow breathing
  // Total duration ~21 s
};
const HapticPattern PAT_PULSE = {
  300, 300, 8, 160, "pulse"
  // Short rhythmic taps — grounding technique
  // Total duration ~4.8 s
};
const HapticPattern PAT_ALERT = {
  100, 100, 6, 255, "alert"
  // Sharp rapid buzz — attention signal
  // Total duration ~1.2 s
};
const HapticPattern PAT_GROUNDING = {
  500, 500, 8, 80, "grounding"
  // Gentle regular beat — focus anchor
  // Total duration ~8 s
};
const HapticPattern PAT_CHECKIN = {
  200, 800, 4, 60, "checkin"
  // Very soft brief taps — welfare check prompt
  // Total duration ~4 s
};
const HapticPattern PAT_FALL = {
  50, 50, 10, 255, "fall"
  // Rapid intense buzz — emergency alert
  // Total duration ~1 s
};

// ══════════════════════════════════════════════════════════════════
// GLOBAL STATE
// ══════════════════════════════════════════════════════════════════

// ── BLE handles ───────────────────────────────────────────────────
BLECharacteristic *charSensor;    // NOTIFY → app (sensor data)
BLECharacteristic *charAlert;     // NOTIFY → app (alert events)
BLECharacteristic *charMotor;     // WRITE  ← app (motor commands)
BLECharacteristic *charSpeech;    // WRITE  ← app (speech results)
BLECharacteristic *charSettings;  // WRITE  ← app (threshold settings)

bool bleConnected    = false;  // true when phone is connected
bool bleWasConnected = false;  // flag to restart advertising after disconnect

// ── MPU-6050 ──────────────────────────────────────────────────────
MPU6050 mpu(Wire);

// ── Pulse sensor state ─────────────────────────────────────────────
// We keep a ring buffer of the last 8 BPM readings and take the
// median — this rejects noise spikes far better than a simple average.
static int   bpmRingBuf[8]  = {72,72,72,72,72,72,72,72}; // pre-seed
static int   bpmRingIdx     = 0;
static int   bpmMedian      = 72;    // current best BPM estimate
static bool  pulseRising    = false; // true during rising edge of pulse wave
static int   pulseLastAvg   = 2048;  // rolling average of ADC reads
static unsigned long lastBeatMs = 0; // timestamp of last detected beat

// ── Motion state ──────────────────────────────────────────────────
static float   motionLastGx   = 0;    // previous gyro X (for delta calc)
static float   motionLastGy   = 0;    // previous gyro Y
static unsigned long lastMoveMs = 0;  // timestamp of last significant movement

// ── Agitation counter ─────────────────────────────────────────────
// Instead of a single-sample flag, we count consecutive frames of
// elevated (but sub-fall) acceleration. This prevents single bumps
// from triggering false agitation alerts.
static int agitCounter = 0;
#define AGIT_TRIGGER_COUNT 15  // ~120ms of sustained agitation at 8ms loop

// ── Episode scoring ───────────────────────────────────────────────
// Each sensor signal contributes a boolean flag. When enough flags
// are true simultaneously, we report an episode.
struct EpisodeState {
  int           score;              // total active signals (0–6)
  bool          tremorActive;       // gyro delta exceeded threshold
  bool          hrElevated;         // BPM too high or too low
  bool          agitationActive;    // sustained acceleration anomaly
  bool          proximityDistress;  // object too close (IR or ultrasonic)
  bool          speechAnomalous;    // set by app's speech analysis result
  bool          prolongedStill;     // no movement for STILL_MS
  unsigned long lastEpisodeMs;      // timestamp of last episode alert
};
EpisodeState episode = {0, false, false, false, false, false, false, 0};

// ── Ultrasonic cached reading ─────────────────────────────────────
static float   ultraDistCm    = -1.0f;  // -1 = no valid reading
static unsigned long lastUltraMs = 0;

// ── Haptic motor non-blocking state machine ────────────────────────
// The motor runs patterns without blocking the sensor loop.
// updateMotor() advances the state machine each loop iteration.
static bool          motorRunning  = false;  // pattern currently active
static HapticPattern motorPattern;            // which pattern is playing
static int           motorRepsLeft = 0;       // repetitions remaining
static bool          motorPhaseOn  = false;   // true during ON phase
static unsigned long motorPhaseStart = 0;     // when current phase started
static int           motorCurrentPWM = 0;     // current duty cycle

// ══════════════════════════════════════════════════════════════════
// SECTION 1: BLE CALLBACKS
// These classes handle events from the BLE stack.
// They run in a separate FreeRTOS task context — keep them short.
// ══════════════════════════════════════════════════════════════════

// Handles phone connect / disconnect
class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *server) override {
    bleConnected = true;
    Serial.println("[BLE] ✓ App connected");
    // Brief confirmation buzz so user knows it connected
    ledcWrite(MOTOR_LEDC_CH, 150); delay(80);
    ledcWrite(MOTOR_LEDC_CH, 0);
  }
  void onDisconnect(BLEServer *server) override {
    bleConnected    = false;
    bleWasConnected = true;   // main loop will restart advertising
    Serial.println("[BLE] ✗ App disconnected — will restart advertising");
  }
};

// ── Motor command handler (app → ESP32) ───────────────────────────
// The app writes a plain text command string to charMotor.
// This callback fires immediately when the write is received.
class MotorCommandCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *c) override {
    // getValue() returns std::string — convert to Arduino String
    String cmd = String(c->getValue().c_str());
    cmd.trim();
    Serial.printf("[MOTOR CMD] Received: \"%s\"\n", cmd.c_str());

    // ── Named patterns ───────────────────────────────────
    if (cmd == "calm") {
      // Slow breathing rhythm — primary calming response
      startMotorPattern(PAT_CALM);

    } else if (cmd == "pulse") {
      // Short rhythmic taps — grounding technique
      startMotorPattern(PAT_PULSE);

    } else if (cmd == "breathe") {
      // Smooth ramp up then ramp down (blocking — 15 s total)
      // Only call this when it's safe to block the loop briefly.
      // In production move this to a FreeRTOS task.
      Serial.println("[MOTOR] Playing breathing ramp (blocking 15 s)");
      playBreathingRamp(3);

    } else if (cmd == "alert") {
      // Rapid strong buzz — attention / warning
      startMotorPattern(PAT_ALERT);

    } else if (cmd == "grounding") {
      // Gentle steady rhythm — focus anchor
      startMotorPattern(PAT_GROUNDING);

    } else if (cmd == "checkin") {
      // Very soft prompt taps
      startMotorPattern(PAT_CHECKIN);

    } else if (cmd == "fall") {
      // Emergency alert buzz
      startMotorPattern(PAT_FALL);

    } else if (cmd == "stop") {
      // Immediately kill motor regardless of pattern state
      stopMotor();
      Serial.println("[MOTOR] Stopped by app command");

    } else if (cmd.startsWith("pwm:")) {
      // Raw PWM value — format: "pwm:180"
      // Lets the app set any exact intensity (0–255)
      int val = cmd.substring(4).toInt();
      val = constrain(val, 0, 255);
      stopMotor();  // cancel any running pattern first
      ledcWrite(MOTOR_LEDC_CH, val);
      motorCurrentPWM = val;
      Serial.printf("[MOTOR] Raw PWM set to %d\n", val);

    } else if (cmd == "ping") {
      // Connection health check — app expects pong response
      if (bleConnected) {
        const char *pong = "{\"type\":\"pong\",\"status\":\"ok\"}";
        charAlert->setValue((uint8_t*)pong, strlen(pong));
        charAlert->notify();
        Serial.println("[BLE] Pong sent");
      }

    } else if (cmd == "status") {
      // App requesting motor status — respond with current state
      char buf[80];
      snprintf(buf, sizeof(buf),
        "{\"type\":\"motor_status\",\"running\":%s,\"pwm\":%d,\"pattern\":\"%s\"}",
        motorRunning ? "true" : "false",
        motorCurrentPWM,
        motorRunning ? motorPattern.name : "none");
      charAlert->setValue((uint8_t*)buf, strlen(buf));
      charAlert->notify();

    } else {
      Serial.printf("[MOTOR CMD] Unknown command: \"%s\"\n", cmd.c_str());
    }
  }
};

// ── Speech analysis result handler (app → ESP32) ──────────────────
// The app analyses microphone audio and sends back:
//   {"anomaly":true,"keyword":"afraid","score":7}
// score is 0–10 (higher = more anomalous)
class SpeechCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *c) override {
    String val = String(c->getValue().c_str());
    Serial.printf("[SPEECH] Result received: %s\n", val.c_str());

    // Simple substring search — no JSON library needed on ESP32
    episode.speechAnomalous = (val.indexOf("\"anomaly\":true") >= 0);

    // Extract score if present (look for "score":N)
    int scoreIdx = val.indexOf("\"score\":");
    if (scoreIdx >= 0) {
      int scoreVal = val.substring(scoreIdx + 8).toInt();
      Serial.printf("[SPEECH] Score: %d/10  Anomaly: %s\n",
        scoreVal, episode.speechAnomalous ? "YES" : "NO");
    }

    // Recalculate episode score with new speech data
    recalcEpisodeScore();
  }
};

// ── Runtime settings handler (app → ESP32) ────────────────────────
// Lets the app tune detection thresholds without reflashing.
// Format: {"hr_high":130,"hr_low":48,"prox_cm":60,"tremor":3.0}
// All fields optional — only fields present are updated.
class SettingsCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *c) override {
    String val = String(c->getValue().c_str());
    Serial.printf("[SETTINGS] Received: %s\n", val.c_str());

    // Parse each optional field with simple substring search
    auto extractFloat = [&](const char *key) -> float {
      String k = String("\"") + key + "\":";
      int idx = val.indexOf(k);
      if (idx < 0) return -1;
      return val.substring(idx + k.length()).toFloat();
    };
    auto extractInt = [&](const char *key) -> int {
      String k = String("\"") + key + "\":";
      int idx = val.indexOf(k);
      if (idx < 0) return -1;
      return val.substring(idx + k.length()).toInt();
    };

    float fv; int iv;
    if ((fv = extractFloat("prox_cm"))  > 0) { THRESH_PROXIMITY_CM = fv; Serial.printf("  prox_cm  → %.1f\n", fv); }
    if ((fv = extractFloat("fall_g"))   > 0) { THRESH_FALL_G       = fv; Serial.printf("  fall_g   → %.2f\n", fv); }
    if ((fv = extractFloat("tremor"))   > 0) { THRESH_TREMOR_DPS   = fv; Serial.printf("  tremor   → %.2f\n", fv); }
    if ((iv = extractInt("hr_high"))    > 0) { THRESH_HR_HIGH      = iv; Serial.printf("  hr_high  → %d\n",  iv); }
    if ((iv = extractInt("hr_low"))     > 0) { THRESH_HR_LOW       = iv; Serial.printf("  hr_low   → %d\n",  iv); }
    if ((iv = extractInt("still_s"))    > 0) { THRESH_STILL_MS     = iv * 1000UL; Serial.printf("  still_s  → %d s\n", iv); }
    if ((iv = extractInt("episode"))    > 0) { THRESH_EPISODE      = iv; Serial.printf("  episode  → %d\n",  iv); }

    // Acknowledge
    const char *ack = "{\"type\":\"settings_ok\"}";
    charAlert->setValue((uint8_t*)ack, strlen(ack));
    charAlert->notify();
  }
};

// ══════════════════════════════════════════════════════════════════
// SECTION 2: BLE INITIALISATION
// ══════════════════════════════════════════════════════════════════
void initBLE() {
  Serial.println("[BLE] Initialising...");

  // Step 1: name the device (this is what appears in the phone's BT scan list)
  BLEDevice::init(BLE_DEVICE_NAME);

  // Step 2: set TX power to maximum for wearable range (~10 metres)
  BLEDevice::setPower(ESP_PWR_LVL_P9);

  // Step 3: create the server and attach our connection callbacks
  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  // Step 4: create a service — groups all our characteristics together
  // IMPORTANT: the service needs enough storage for 5 characteristics.
  // createService(UUID, numHandles) — each characteristic needs ~4 handles.
  BLEService *service = server->createService(
    BLEUUID(UUID_SERVICE),
    32  // allocate 32 attribute handles — enough for 5 characteristics
  );

  // Step 5: create each characteristic ─────────────────────────────

  // charSensor: ESP32 pushes sensor data to app automatically
  // PROPERTY_READ   = app can read the last value at any time
  // PROPERTY_NOTIFY = ESP32 can push new values without being asked
  charSensor = service->createCharacteristic(
    UUID_CHAR_SENSOR,
    BLECharacteristic::PROPERTY_READ  |
    BLECharacteristic::PROPERTY_NOTIFY
  );
  // BLE2902 is the CCCD descriptor — REQUIRED for NOTIFY to work.
  // Without it the app cannot subscribe to notifications.
  charSensor->addDescriptor(new BLE2902());
  charSensor->setValue("{}");  // set a default so READ doesn't return empty

  // charAlert: ESP32 pushes alert events (falls, episodes, etc.)
  charAlert = service->createCharacteristic(
    UUID_CHAR_ALERT,
    BLECharacteristic::PROPERTY_READ  |
    BLECharacteristic::PROPERTY_NOTIFY
  );
  charAlert->addDescriptor(new BLE2902());
  charAlert->setValue("{}");

  // charMotor: app writes motor commands to this
  // PROPERTY_WRITE      = app writes, ESP32 gets acknowledgement
  // PROPERTY_WRITE_NR   = app writes without acknowledgement (faster)
  // We use WRITE (with ack) so the app knows the command was received.
  charMotor = service->createCharacteristic(
    UUID_CHAR_MOTOR,
    BLECharacteristic::PROPERTY_WRITE
  );
  charMotor->setCallbacks(new MotorCommandCallbacks());

  // charSpeech: app writes back speech analysis results
  charSpeech = service->createCharacteristic(
    UUID_CHAR_SPEECH,
    BLECharacteristic::PROPERTY_WRITE
  );
  charSpeech->setCallbacks(new SpeechCallbacks());

  // charSettings: app can change detection thresholds at runtime
  charSettings = service->createCharacteristic(
    UUID_CHAR_SETTINGS,
    BLECharacteristic::PROPERTY_WRITE
  );
  charSettings->setCallbacks(new SettingsCallbacks());

  // Step 6: start the service (makes characteristics accessible)
  service->start();

  // Step 7: configure advertising so phones can find the device
  BLEAdvertising *adv = BLEDevice::getAdvertising();
  adv->addServiceUUID(UUID_SERVICE);  // include service UUID in ad packet
  adv->setScanResponse(true);          // respond to scan requests
  adv->setMinPreferred(0x06);          // connection interval hint (iOS fix)
  adv->setMaxPreferred(0x12);

  // Step 8: start broadcasting
  BLEDevice::startAdvertising();
  Serial.println("[BLE] ✓ Advertising as '" BLE_DEVICE_NAME "'");
  Serial.println("[BLE] UUIDs:");
  Serial.println("       Service:  " UUID_SERVICE);
  Serial.println("       Sensor:   " UUID_CHAR_SENSOR);
  Serial.println("       Alert:    " UUID_CHAR_ALERT);
  Serial.println("       Motor:    " UUID_CHAR_MOTOR);
  Serial.println("       Speech:   " UUID_CHAR_SPEECH);
  Serial.println("       Settings: " UUID_CHAR_SETTINGS);
}

// ══════════════════════════════════════════════════════════════════
// SECTION 3: SENSOR READING FUNCTIONS
// ══════════════════════════════════════════════════════════════════

// ── HC-SR04 Ultrasonic ────────────────────────────────────────────
// Sends a 10µs TRIG pulse, measures ECHO pulse width.
// Returns distance in cm. Returns -1.0 on timeout (nothing detected).
// Call at most every 80ms — otherwise the echo from previous pulse
// interferes with the next reading.
float readUltrasonic() {
  // Send trigger pulse
  digitalWrite(PIN_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(PIN_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_TRIG, LOW);

  // Measure echo pulse duration — timeout at 25ms (~430cm max range)
  long duration = pulseIn(PIN_ECHO, HIGH, 25000);

  // duration = 0 means timeout (nothing in range)
  if (duration == 0) return -1.0f;

  // Convert to cm: sound travels at 0.0343 cm/µs, divide by 2 (round trip)
  return duration * 0.0343f / 2.0f;
}

// ── Pulse sensor BPM ──────────────────────────────────────────────
// Reads the analog pulse sensor and detects heartbeat peaks.
// Uses a rolling 12-sample average to smooth ADC noise, then
// detects rising edges (heartbeats) and computes IBI (inter-beat
// interval) in milliseconds → converts to BPM.
// Stores last 8 BPM values and returns the median — this rejects
// noisy readings caused by movement artifacts.
int readPulseBPM() {
  // 12-sample rolling average buffer
  static int adcBuf[12] = {2048,2048,2048,2048,2048,2048,2048,2048,2048,2048,2048,2048};
  static int adcBufIdx  = 0;

  // Read ADC (0–4095 on ESP32's 12-bit ADC)
  adcBuf[adcBufIdx % 12] = analogRead(PIN_PULSE);
  adcBufIdx++;

  // Compute rolling average
  int avg = 0;
  for (int v : adcBuf) avg += v;
  avg /= 12;

  // ── Peak detection ──────────────────────────────────────────
  // A heartbeat creates a bump in the signal.
  // We look for a rising edge (signal goes up sharply from previous avg).
  // The threshold of +60 ADC counts filters out noise.

  if (avg > pulseLastAvg + 60 && !pulseRising) {
    // Rising edge detected — this is the start of a heartbeat peak
    pulseRising = true;
    unsigned long now = millis();
    unsigned long ibi = now - lastBeatMs;  // inter-beat interval in ms

    // Validate physiological range: 30–200 BPM = 300ms–2000ms IBI
    if (ibi > 300 && ibi < 2000) {
      int newBPM = 60000 / ibi;

      // Store in ring buffer
      bpmRingBuf[bpmRingIdx % 8] = newBPM;
      bpmRingIdx++;

      // Compute median of last 8 readings
      int sorted[8];
      memcpy(sorted, bpmRingBuf, sizeof(sorted));
      // Simple insertion sort (8 elements — negligible cost)
      for (int i = 1; i < 8; i++) {
        int key = sorted[i], j = i - 1;
        while (j >= 0 && sorted[j] > key) { sorted[j+1] = sorted[j]; j--; }
        sorted[j+1] = key;
      }
      bpmMedian = (sorted[3] + sorted[4]) / 2;
      lastBeatMs = now;
    }
  } else if (avg < pulseLastAvg - 40) {
    // Falling edge — reset rising edge flag so we catch the next beat
    pulseRising = false;
  }

  pulseLastAvg = avg;
  return bpmMedian;
}

// ══════════════════════════════════════════════════════════════════
// SECTION 4: MOTOR CONTROL
// All motor control uses the LEDC peripheral (ESP32's PWM engine).
// The BC547 transistor switches full motor current based on the
// PWM duty cycle from GPIO26. The non-blocking state machine means
// patterns play without stopping sensor reads.
// ══════════════════════════════════════════════════════════════════

// Start a named pattern — non-blocking.
// After calling this, call updateMotor() every loop iteration.
void startMotorPattern(const HapticPattern &p) {
  motorPattern    = p;
  motorRepsLeft   = p.reps;
  motorPhaseOn    = true;
  motorPhaseStart = millis();
  motorRunning    = true;
  motorCurrentPWM = p.intensity;
  ledcWrite(MOTOR_LEDC_CH, p.intensity);
  Serial.printf("[MOTOR] → Starting pattern: %s  intensity:%d  reps:%d\n",
    p.name, p.intensity, p.reps);
}

// Stop motor immediately — cancels any running pattern
void stopMotor() {
  motorRunning    = false;
  motorCurrentPWM = 0;
  ledcWrite(MOTOR_LEDC_CH, 0);
}

// Non-blocking motor state machine — call every loop()
// Advances the current pattern through its on/off phases.
void updateMotor() {
  if (!motorRunning) return;

  unsigned long elapsed = millis() - motorPhaseStart;
  unsigned long phaseDuration = motorPhaseOn ? motorPattern.onMs : motorPattern.offMs;

  // Not yet time to transition — return immediately
  if (elapsed < phaseDuration) return;

  if (motorPhaseOn) {
    // ON phase complete → switch to OFF phase
    ledcWrite(MOTOR_LEDC_CH, 0);
    motorCurrentPWM = 0;
    motorPhaseOn    = false;
    motorPhaseStart = millis();
  } else {
    // OFF phase complete → decrement rep counter
    motorRepsLeft--;
    if (motorRepsLeft <= 0) {
      // Pattern finished
      stopMotor();
      Serial.printf("[MOTOR] Pattern '%s' complete\n", motorPattern.name);

      // Notify app that pattern finished
      if (bleConnected) {
        char buf[80];
        snprintf(buf, sizeof(buf),
          "{\"type\":\"motor_done\",\"pattern\":\"%s\"}", motorPattern.name);
        charAlert->setValue((uint8_t*)buf, strlen(buf));
        charAlert->notify();
      }
      return;
    }
    // Start next ON phase
    ledcWrite(MOTOR_LEDC_CH, motorPattern.intensity);
    motorCurrentPWM = motorPattern.intensity;
    motorPhaseOn    = true;
    motorPhaseStart = millis();
  }
}

// Smooth breathing ramp — BLOCKING (uses delay internally).
// Ramps intensity 0→peak→0 over ~5 seconds per cycle.
// Only call this from a context where blocking is acceptable,
// or run it in a separate FreeRTOS task.
void playBreathingRamp(int cycles) {
  const int peak    = 150;   // max intensity (don't go full 255 — too jarring)
  const int stepMs  = 20;    // ms per step
  const int stepSz  = 3;     // PWM units per step

  Serial.printf("[MOTOR] Breathing ramp: %d cycles\n", cycles);
  motorRunning = true;

  for (int c = 0; c < cycles; c++) {
    // Inhale — ramp up over ~1 s
    for (int i = 0; i <= peak; i += stepSz) {
      ledcWrite(MOTOR_LEDC_CH, i);
      motorCurrentPWM = i;
      delay(stepMs);
    }
    // Hold at peak — 400 ms
    delay(400);
    // Exhale — ramp down over ~1 s
    for (int i = peak; i >= 0; i -= stepSz) {
      ledcWrite(MOTOR_LEDC_CH, i);
      motorCurrentPWM = i;
      delay(stepMs);
    }
    // Pause between breaths — ~1 s
    delay(1000);
    Serial.printf("[MOTOR] Breath cycle %d/%d done\n", c+1, cycles);
  }

  stopMotor();
  Serial.println("[MOTOR] Breathing ramp complete");
}

// ══════════════════════════════════════════════════════════════════
// SECTION 5: EPISODE SCORE & ALERT LOGIC
// ══════════════════════════════════════════════════════════════════

// Recompute the episode risk score from all active flags.
// Score = number of concurrent warning signals (0–6).
// Fires an EPISODE_DETECTED alert and starts calming motor
// if score reaches the threshold.
void recalcEpisodeScore() {
  int score = 0;
  if (episode.tremorActive)      score++;
  if (episode.hrElevated)        score++;
  if (episode.agitationActive)   score++;
  if (episode.proximityDistress) score++;
  if (episode.speechAnomalous)   score++;
  if (episode.prolongedStill)    score++;

  episode.score = score;

  if (score >= THRESH_EPISODE) {
    unsigned long now = millis();
    if (now - episode.lastEpisodeMs > EPISODE_DEBOUNCE_MS) {
      episode.lastEpisodeMs = now;

      const char *severity = score >= 5 ? "severe"
                           : score >= 4 ? "high"
                           : "moderate";

      // Send episode alert to app
      sendAlertPacket("EPISODE_DETECTED", severity, score);

      // Automatically start calming motor (unless already running)
      if (!motorRunning) {
        Serial.println("[EPISODE] Auto-triggering calming pattern");
        startMotorPattern(PAT_CALM);
      }
    }
  }
}

// Send a structured alert packet to the app via charAlert NOTIFY.
// The app displays this as a notification/alert card.
void sendAlertPacket(const char *type, const char *detail, int score) {
  if (!bleConnected) {
    Serial.printf("[ALERT] (not connected) %s | %s | score=%d\n", type, detail, score);
    return;
  }
  char buf[180];
  snprintf(buf, sizeof(buf),
    "{\"type\":\"%s\","
    "\"detail\":\"%s\","
    "\"score\":%d,"
    "\"signals\":{"
      "\"tremor\":%s,"
      "\"hr\":%s,"
      "\"agit\":%s,"
      "\"prox\":%s,"
      "\"speech\":%s,"
      "\"still\":%s"
    "},"
    "\"ts\":%lu}",
    type, detail, score,
    episode.tremorActive      ? "true" : "false",
    episode.hrElevated        ? "true" : "false",
    episode.agitationActive   ? "true" : "false",
    episode.proximityDistress ? "true" : "false",
    episode.speechAnomalous   ? "true" : "false",
    episode.prolongedStill    ? "true" : "false",
    millis()
  );
  charAlert->setValue((uint8_t*)buf, strlen(buf));
  charAlert->notify();
  Serial.printf("[ALERT] %s | %s | score=%d\n", type, detail, score);
}

// ══════════════════════════════════════════════════════════════════
// SECTION 6: TELEMETRY PACKET BUILDER
// Assembles the full sensor JSON and pushes it to charSensor.
// Called every TELEMETRY_INTERVAL_MS milliseconds.
// ══════════════════════════════════════════════════════════════════
void sendTelemetry(
  int bpm,
  float ax, float ay, float az,
  float gx, float gy, float gz,
  float distCm,
  bool irClose,
  bool fallNow
) {
  if (!bleConnected) return;

  // BLE MTU is 23 bytes by default but negotiates up to ~240 bytes.
  // This packet is ~220 bytes — safe for most phones.
  char buf[256];
  snprintf(buf, sizeof(buf),
    "{"
      "\"bpm\":%d,"
      "\"ax\":%.2f,\"ay\":%.2f,\"az\":%.2f,"
      "\"gx\":%.1f,\"gy\":%.1f,\"gz\":%.1f,"
      "\"dist\":%.1f,"
      "\"ir\":%s,"
      "\"tremor\":%s,"
      "\"agit\":%s,"
      "\"fall\":%s,"
      "\"still\":%s,"
      "\"score\":%d,"
      "\"motor\":%s,"
      "\"uptime\":%lu"
    "}",
    bpm,
    ax, ay, az,
    gx, gy, gz,
    distCm,
    irClose                   ? "true" : "false",
    episode.tremorActive      ? "true" : "false",
    episode.agitationActive   ? "true" : "false",
    fallNow                   ? "true" : "false",
    episode.prolongedStill    ? "true" : "false",
    episode.score,
    motorRunning              ? "true" : "false",
    millis()
  );

  charSensor->setValue((uint8_t*)buf, strlen(buf));
  charSensor->notify();
}

// ══════════════════════════════════════════════════════════════════
// SECTION 7: SETUP
// ══════════════════════════════════════════════════════════════════
void setup() {
  Serial.begin(115200);
  delay(300);

  Serial.println("\n╔══════════════════════════════════╗");
  Serial.println("║    SYNAPSE WEARABLE FIRMWARE     ║");
  Serial.println("║    v2.0 — Full BLE integration   ║");
  Serial.println("╚══════════════════════════════════╝\n");

  // ── GPIO setup ─────────────────────────────────────────────────
  pinMode(PIN_IR,   INPUT);
  pinMode(PIN_TRIG, OUTPUT);
  pinMode(PIN_ECHO, INPUT);
  // PIN_PULSE (GPIO34) is input-only — no pinMode needed
  // PIN_SDA, PIN_SCL are handled by Wire.begin()

  Serial.println("[GPIO] Pins configured");

  // ── Motor PWM setup ─────────────────────────────────────────────
  // ledcSetup(channel, frequency, resolution)
  ledcSetup(MOTOR_LEDC_CH, MOTOR_LEDC_FREQ, MOTOR_LEDC_RES);
  // ledcAttachPin(pin, channel) — links GPIO26 to LEDC channel 0
  ledcAttachPin(PIN_MOTOR, MOTOR_LEDC_CH);
  // Start at 0 — motor off
  ledcWrite(MOTOR_LEDC_CH, 0);
  Serial.println("[MOTOR] LEDC PWM ready on GPIO26");

  // ── MPU-6050 setup ──────────────────────────────────────────────
  // Wire.begin() uses PIN_SDA (21) and PIN_SCL (22) by default on ESP32
  Wire.begin();
  byte mpuStatus = mpu.begin();
  if (mpuStatus != 0) {
    Serial.printf("[MPU] ✗ ERROR: begin() returned %d — check wiring!\n", mpuStatus);
    Serial.println("[MPU]   SDA→GPIO21, SCL→GPIO22, VCC→3.3V, GND→GND");
    // Don't halt — continue without MPU so BLE still works
  } else {
    Serial.println("[MPU] ✓ Found — calibrating (keep wearable still for 3 s)...");
    // calcOffsets() takes ~3 seconds of still readings to zero the sensor
    // true,true = calibrate both accelerometer and gyroscope
    mpu.calcOffsets(true, true);
    Serial.println("[MPU] ✓ Calibration complete");
  }

  // ── BLE setup ───────────────────────────────────────────────────
  initBLE();

  // ── Init timers ─────────────────────────────────────────────────
  lastMoveMs  = millis();
  lastBeatMs  = millis();
  lastUltraMs = millis();

  // ── Startup confirmation ─────────────────────────────────────────
  // Three pulses = device ready
  for (int i = 0; i < 3; i++) {
    ledcWrite(MOTOR_LEDC_CH, 180); delay(100);
    ledcWrite(MOTOR_LEDC_CH, 0);   delay(100);
  }

  Serial.println("\n[SYS] ✓ Ready — waiting for BLE connection from Synapse app");
  Serial.println("[SYS] Thresholds:");
  Serial.printf("       HR:      %d–%d BPM\n",   THRESH_HR_LOW, THRESH_HR_HIGH);
  Serial.printf("       Prox:    %.0f cm\n",       THRESH_PROXIMITY_CM);
  Serial.printf("       Tremor:  %.1f °/s\n",      THRESH_TREMOR_DPS);
  Serial.printf("       Still:   %.0f s\n",        THRESH_STILL_MS / 1000.0);
  Serial.printf("       Episode: %d signals\n\n",  THRESH_EPISODE);
}

// ══════════════════════════════════════════════════════════════════
// SECTION 8: MAIN LOOP
// Runs continuously at ~125 Hz (8ms sleep at bottom).
// Each iteration:
//   1. Restart BLE advertising if needed
//   2. Tick the motor pattern state machine
//   3. Read all sensors
//   4. Run detection logic
//   5. Check alert conditions
//   6. Send telemetry packet if interval elapsed
// ══════════════════════════════════════════════════════════════════
void loop() {
  static unsigned long lastTelemetryMs  = 0;
  static unsigned long lastAlertCheckMs = 0;
  unsigned long now = millis();

  // ── 1. Reconnection handling ──────────────────────────────────
  // When the phone disconnects, bleWasConnected is set by the callback.
  // We restart advertising here (outside the callback context — safer).
  if (bleWasConnected && !bleConnected) {
    bleWasConnected = false;
    delay(100);  // small delay before restarting advertising
    BLEDevice::startAdvertising();
    Serial.println("[BLE] Restarted advertising");
  }

  // ── 2. Motor pattern tick ─────────────────────────────────────
  // Non-blocking — advances the state machine without delay()
  updateMotor();

  // ── 3. Read MPU-6050 ─────────────────────────────────────────
  // mpu.update() fetches latest register values over I2C.
  // getAccX/Y/Z() returns values in g (1.0 = normal gravity on Z at rest)
  // getGyroX/Y/Z() returns values in degrees/second
  mpu.update();
  float ax = mpu.getAccX();
  float ay = mpu.getAccY();
  float az = mpu.getAccZ();
  float gx = mpu.getGyroX();
  float gy = mpu.getGyroY();
  float gz = mpu.getGyroZ();

  // Total acceleration magnitude (vector length)
  // At rest, lying flat: totalAcc ≈ 1.0g (just gravity)
  // During movement: deviates from 1.0
  float totalAcc = sqrtf(ax*ax + ay*ay + az*az);

  // ── 4a. Tremor detection ──────────────────────────────────────
  // Tremor = rapid involuntary oscillation in the gyroscope.
  // We compute the CHANGE in gyro from last frame.
  // A calm arm produces near-zero delta; tremor produces large swings.
  float tremorMag = sqrtf(
    powf(gx - motionLastGx, 2) +
    powf(gy - motionLastGy, 2)
  );
  motionLastGx = gx;
  motionLastGy = gy;
  episode.tremorActive = (tremorMag > THRESH_TREMOR_DPS);

  // ── 4b. Fall detection ────────────────────────────────────────
  // A fall produces a large spike in totalAcc (impact) OR
  // a near-zero reading (free-fall phase before impact).
  bool fallNow = (totalAcc > THRESH_FALL_G || totalAcc < 0.25f);

  // ── 4c. Agitation detection ───────────────────────────────────
  // Agitation = sustained elevated movement (not just a fall).
  // We count consecutive frames where totalAcc is elevated but
  // below fall threshold. Requires 15 consecutive frames (~120ms).
  if (totalAcc > 1.30f && totalAcc < THRESH_FALL_G) {
    if (agitCounter < 100) agitCounter++;  // cap to prevent overflow
  } else {
    if (agitCounter > 0) agitCounter--;    // decay when movement stops
  }
  episode.agitationActive = (agitCounter >= AGIT_TRIGGER_COUNT);

  // ── 4d. Stillness detection ───────────────────────────────────
  // Reset the movement timer whenever there's significant motion.
  // A small dead-band (±0.06g) ignores tiny vibrations.
  if (totalAcc > 1.06f || totalAcc < 0.94f) {
    lastMoveMs = now;
  }
  episode.prolongedStill = (now - lastMoveMs > THRESH_STILL_MS);

  // ── 4e. IR sensor ─────────────────────────────────────────────
  // Most IR proximity sensors output LOW when object is detected.
  bool irClose = (digitalRead(PIN_IR) == LOW);

  // ── 4f. Ultrasonic (throttled to 12 Hz) ──────────────────────
  // Reading too fast causes the echo from one pulse to bleed into
  // the next measurement. 80ms gap = ~12 readings/second, plenty.
  if (now - lastUltraMs >= ULTRASONIC_INTERVAL_MS) {
    ultraDistCm = readUltrasonic();
    lastUltraMs = now;
  }
  episode.proximityDistress = irClose ||
    (ultraDistCm > 0 && ultraDistCm < THRESH_PROXIMITY_CM);

  // ── 4g. Pulse sensor ─────────────────────────────────────────
  int bpm = readPulseBPM();
  episode.hrElevated = (bpm > THRESH_HR_HIGH) ||
                       (bpm > 0 && bpm < THRESH_HR_LOW);

  // ── 5. Alert checks (every 300ms) ────────────────────────────
  if (now - lastAlertCheckMs >= ALERT_CHECK_INTERVAL_MS) {
    lastAlertCheckMs = now;

    // Individual sensor alerts — immediate, specific
    if (fallNow) {
      sendAlertPacket("FALL", "sudden_acceleration", 5);
      // Fall always triggers alert pattern
      if (!motorRunning) startMotorPattern(PAT_FALL);
    }
    if (episode.prolongedStill) {
      sendAlertPacket("STILL", "no_movement_detected", 1);
      if (!motorRunning) startMotorPattern(PAT_CHECKIN);
    }
    if (bpm > THRESH_HR_HIGH) {
      sendAlertPacket("HR_HIGH", "tachycardia", 2);
    }
    if (bpm > 0 && bpm < THRESH_HR_LOW) {
      sendAlertPacket("HR_LOW", "bradycardia", 2);
    }

    // Composite episode score — all signals together
    recalcEpisodeScore();
  }

  // ── 6. Send telemetry packet (every 400ms) ───────────────────
  if (now - lastTelemetryMs >= TELEMETRY_INTERVAL_MS) {
    lastTelemetryMs = now;
    sendTelemetry(bpm, ax, ay, az, gx, gy, gz, ultraDistCm, irClose, fallNow);

    // Serial debug — visible in Arduino Serial Monitor
    Serial.printf(
      "BPM:%-4d Dist:%5.1fcm IR:%-5s Acc:%.2fg "
      "Tremor:%-5s Agit:%-5s Fall:%-5s Score:%d Motor:%-5s\n",
      bpm,
      ultraDistCm,
      irClose                 ? "YES" : "NO",
      totalAcc,
      episode.tremorActive    ? "YES" : "NO",
      episode.agitationActive ? "YES" : "NO",
      fallNow                 ? "YES" : "NO",
      episode.score,
      motorRunning            ? "ON" : "OFF"
    );
  }

  // ── 7. Loop rate control ──────────────────────────────────────
  // 8ms delay → ~125 Hz loop rate.
  // Fast enough for smooth haptics and responsive sensor reads,
  // slow enough to not starve the BLE stack's background tasks.
  delay(8);
}
