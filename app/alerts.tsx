import { View, Text, StyleSheet, TouchableOpacity, Switch } from "react-native";
import { useState } from "react";
import * as Speech from "expo-speech";

export default function Alerts() {
  const [crowdAlert, setCrowdAlert]     = useState(true);
  const [distanceAlert, setDistanceAlert] = useState(true);
  const [irAlert, setIrAlert]           = useState(true);
  const [vibrate, setVibrate]           = useState(false);
  const [threshold, setThreshold]       = useState(100); // cm

  function testSpeech() {
    Speech.speak("Test alert. Crowd detected nearby. Object 45 centimeters away.", {
      rate: 1.0,
      pitch: 1.0,
    });
  }

  function testBuzzer() {
    // Will trigger real buzzer via BLE later
    Speech.speak("Buzzer triggered", { rate: 1.0 });
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Alerts & Controls</Text>
      <Text style={styles.subtitle}>Manage sensor alerts and device feedback</Text>

      {/* Alert settings */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Alert Settings</Text>

        <View style={styles.row}>
          <View>
            <Text style={styles.rowLabel}>Crowd Detection</Text>
            <Text style={styles.rowSub}>Speak alert when crowd nearby</Text>
          </View>
          <Switch value={crowdAlert} onValueChange={setCrowdAlert} trackColor={{ true: "#2563eb" }} />
        </View>

        <View style={styles.row}>
          <View>
            <Text style={styles.rowLabel}>Distance Warning</Text>
            <Text style={styles.rowSub}>Alert when object under {threshold}cm</Text>
          </View>
          <Switch value={distanceAlert} onValueChange={setDistanceAlert} trackColor={{ true: "#2563eb" }} />
        </View>

        <View style={styles.row}>
          <View>
            <Text style={styles.rowLabel}>IR Motion Alert</Text>
            <Text style={styles.rowSub}>Alert when motion detected</Text>
          </View>
          <Switch value={irAlert} onValueChange={setIrAlert} trackColor={{ true: "#2563eb" }} />
        </View>
      </View>

      {/* Distance threshold */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Distance Threshold</Text>
        <View style={styles.thresholdRow}>
          <TouchableOpacity
            style={styles.thresholdBtn}
            onPress={() => setThreshold(t => Math.max(20, t - 10))}
          >
            <Text style={styles.thresholdBtnText}>−</Text>
          </TouchableOpacity>
          <Text style={styles.thresholdValue}>{threshold} cm</Text>
          <TouchableOpacity
            style={styles.thresholdBtn}
            onPress={() => setThreshold(t => Math.min(300, t + 10))}
          >
            <Text style={styles.thresholdBtnText}>+</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Device controls */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Device Controls</Text>

        <View style={styles.row}>
          <View>
            <Text style={styles.rowLabel}>Vibration Motor</Text>
            <Text style={styles.rowSub}>Trigger coin motor on alerts</Text>
          </View>
          <Switch value={vibrate} onValueChange={setVibrate} trackColor={{ true: "#2563eb" }} />
        </View>

        <TouchableOpacity style={styles.button} onPress={testSpeech}>
          <Text style={styles.buttonText}>🔊 Test Audio Alert</Text>
        </TouchableOpacity>

        <TouchableOpacity style={[styles.button, styles.buttonOutline]} onPress={testBuzzer}>
          <Text style={styles.buttonTextOutline}>⚡ Test Buzzer</Text>
        </TouchableOpacity>
      </View>

    </View>
  );
}

const styles = StyleSheet.create({
  container:        { flex: 1, backgroundColor: "#0f0f0f", padding: 24 },
  title:            { fontSize: 24, fontWeight: "bold", color: "#fff", marginBottom: 4 },
  subtitle:         { fontSize: 13, color: "#555", marginBottom: 24 },
  section:          { backgroundColor: "#1a1a1a", borderRadius: 16, padding: 20, marginBottom: 16 },
  sectionTitle:     { fontSize: 12, color: "#555", textTransform: "uppercase", letterSpacing: 1, marginBottom: 16 },
  row:              { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: "#2a2a2a" },
  rowLabel:         { color: "#fff", fontSize: 15, fontWeight: "500" },
  rowSub:           { color: "#555", fontSize: 12, marginTop: 2 },
  thresholdRow:     { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 24 },
  thresholdBtn:     { backgroundColor: "#2563eb", width: 44, height: 44, borderRadius: 22, alignItems: "center", justifyContent: "center" },
  thresholdBtnText: { color: "#fff", fontSize: 24, fontWeight: "bold" },
  thresholdValue:   { color: "#fff", fontSize: 28, fontWeight: "bold", minWidth: 100, textAlign: "center" },
  button:           { backgroundColor: "#2563eb", borderRadius: 10, padding: 14, alignItems: "center", marginTop: 12 },
  buttonOutline:    { backgroundColor: "transparent", borderWidth: 1, borderColor: "#2563eb" },
  buttonText:       { color: "#fff", fontWeight: "600", fontSize: 15 },
  buttonTextOutline:{ color: "#2563eb", fontWeight: "600", fontSize: 15 },
});