import { LinearGradient } from "expo-linear-gradient";
import { ActivityIndicator, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useSensor } from "../Context/SensorContext";

export default function Index() {
  const { connected, scanning, scanAndConnect, disconnect } = useSensor();

  return (
    <LinearGradient colors={["#0a0015", "#0d0628", "#0a1628"]} style={styles.bg}>
      <View style={styles.container}>

        <LinearGradient colors={["#4f46e5", "#7c3aed"]} style={styles.logoCircle}>
          <Text style={styles.logoText}>S</Text>
        </LinearGradient>
        <Text style={styles.title}>Synapse</Text>
        <Text style={styles.subtitle}>Wearable awareness device</Text>

        <View style={styles.statusRow}>
          <View style={[styles.dot, { backgroundColor: connected ? "#22c55e" : "#4f46e5" }]} />
          <Text style={styles.statusText}>
            {scanning ? "Scanning..." : connected ? "Connected to Synapse" : "No device connected"}
          </Text>
        </View>

        <TouchableOpacity
          onPress={connected ? disconnect : scanAndConnect}
          disabled={scanning}
          style={styles.btnWrap}
        >
          <LinearGradient
            colors={connected ? ["#7f1d1d","#dc2626"] : ["#1e1b4b","#4f46e5"]}
            start={{x:0,y:0}} end={{x:1,y:0}}
            style={styles.button}
          >
            {scanning
              ? <ActivityIndicator color="#fff" />
              : <Text style={styles.buttonText}>
                  {connected ? "Disconnect" : "Scan for Device"}
                </Text>
            }
          </LinearGradient>
        </TouchableOpacity>

        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.card}>
          <Text style={styles.cardTitle}>Device Status</Text>
          {[
            { label: "Bluetooth",  value: connected ? "Connected"  : "Disconnected" },
            { label: "Ultrasonic", value: connected ? "Active"     : "Waiting" },
            { label: "IR Sensor",  value: connected ? "Active"     : "Waiting" },
            { label: "MPU6050",    value: connected ? "Active"     : "Waiting" },
            { label: "Motor",      value: connected ? "Ready"      : "Waiting" },
          ].map(({ label, value }) => (
            <View key={label} style={styles.statusItem}>
              <Text style={styles.statusLabel}>{label}</Text>
              <Text style={[styles.statusValue, { color: connected ? "#22c55e" : "#4f46e5" }]}>
                {value}
              </Text>
            </View>
          ))}
        </LinearGradient>

      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  bg:          { flex: 1 },
  container:   { flex: 1, alignItems: "center", justifyContent: "center", padding: 24 },
  logoCircle:  { width: 80, height: 80, borderRadius: 40, alignItems: "center", justifyContent: "center", marginBottom: 16 },
  logoText:    { fontSize: 40, fontWeight: "bold", color: "#fff" },
  title:       { fontSize: 32, fontWeight: "bold", color: "#fff", marginBottom: 4 },
  subtitle:    { fontSize: 13, color: "#6366f1", marginBottom: 32, letterSpacing: 1 },
  statusRow:   { flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 24 },
  dot:         { width: 10, height: 10, borderRadius: 5 },
  statusText:  { color: "#818cf8", fontSize: 14 },
  btnWrap:     { width: "100%", marginBottom: 32 },
  button:      { paddingVertical: 16, borderRadius: 14, alignItems: "center" },
  buttonText:  { color: "#fff", fontSize: 16, fontWeight: "700", letterSpacing: 0.5 },
  card:        { width: "100%", borderRadius: 16, padding: 20 },
  cardTitle:   { fontSize: 12, color: "#6366f1", textTransform: "uppercase", letterSpacing: 1, marginBottom: 16 },
  statusItem:  { flexDirection: "row", justifyContent: "space-between", paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: "#1e1b4b" },
  statusLabel: { color: "#818cf8", fontSize: 14 },
  statusValue: { fontSize: 14, fontWeight: "600" },
});