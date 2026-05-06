import { useState } from "react";
import { ActivityIndicator, FlatList, StyleSheet, Text, TouchableOpacity, View } from "react-native";

export default function Index() {
  const [scanning, setScanning] = useState(false);
  const [connected, setConnected] = useState(false);
  const [devices, setDevices] = useState([
    { id: "1", name: "MyWearable" },
  ]);

  function startScan() {
    setScanning(true);
    setTimeout(() => setScanning(false), 3000);
  }

  function connectToDevice(name: string) {
    setConnected(true);
  }

  return (
    <View style={styles.container}>

      {/* Header */}
      <Text style={styles.title}>Wearable App</Text>
      <Text style={styles.subtitle}>
        {connected ? "Connected to MyWearable" : "No device connected"}
      </Text>

      {/* Status dot */}
      <View style={[styles.dot, connected ? styles.dotGreen : styles.dotGray]} />

      {/* Scan button */}
      <TouchableOpacity
        style={[styles.button, scanning && styles.buttonDisabled]}
        onPress={startScan}
        disabled={scanning}
      >
        {scanning
          ? <ActivityIndicator color="#fff" />
          : <Text style={styles.buttonText}>Scan for Device</Text>
        }
      </TouchableOpacity>

      {/* Device list */}
      {devices.length > 0 && (
        <View style={styles.list}>
          <Text style={styles.listTitle}>Nearby Devices</Text>
          <FlatList
            data={devices}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <TouchableOpacity
                style={styles.deviceRow}
                onPress={() => connectToDevice(item.name)}
              >
                <Text style={styles.deviceName}>{item.name}</Text>
                <Text style={styles.deviceConnect}>Connect</Text>
              </TouchableOpacity>
            )}
          />
        </View>
      )}

    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: "#0f0f0f", alignItems: "center", justifyContent: "center", padding: 24 },
  title:         { fontSize: 28, fontWeight: "bold", color: "#ffffff", marginBottom: 8 },
  subtitle:      { fontSize: 14, color: "#888", marginBottom: 24 },
  dot:           { width: 16, height: 16, borderRadius: 8, marginBottom: 32 },
  dotGreen:      { backgroundColor: "#22c55e" },
  dotGray:       { backgroundColor: "#444" },
  button:        { backgroundColor: "#2563eb", paddingVertical: 14, paddingHorizontal: 40, borderRadius: 12, marginBottom: 32 },
  buttonDisabled:{ backgroundColor: "#1e3a6e" },
  buttonText:    { color: "#fff", fontSize: 16, fontWeight: "600" },
  list:          { width: "100%", backgroundColor: "#1a1a1a", borderRadius: 12, padding: 16 },
  listTitle:     { color: "#888", fontSize: 12, marginBottom: 12, textTransform: "uppercase", letterSpacing: 1 },
  deviceRow:     { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: "#2a2a2a" },
  deviceName:    { color: "#fff", fontSize: 16 },
  deviceConnect: { color: "#2563eb", fontSize: 14, fontWeight: "600" },
});