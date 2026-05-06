import * as Speech from "expo-speech";
import { View, Text, StyleSheet, ScrollView } from "react-native";
import { useState, useEffect } from "react";

function MetricCard({ label, value, unit, color, sublabel }: {
  label: string; value: string; unit: string; color: string; sublabel?: string;
}) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardLabel}>{label}</Text>
      <View style={styles.cardValueRow}>
        <Text style={[styles.cardValue, { color }]}>{value}</Text>
        <Text style={styles.cardUnit}>{unit}</Text>
      </View>
      {sublabel && <Text style={styles.cardSublabel}>{sublabel}</Text>}
    </View>
  );
}

function AlertBanner({ message, color }: { message: string; color: string }) {
  return (
    <View style={[styles.banner, { borderLeftColor: color }]}>
      <Text style={styles.bannerText}>{message}</Text>
    </View>
  );
}

export default function Dashboard() {
  const [distance, setDistance]   = useState(120);  // cm, from ultrasonic
  const [irDetected, setIr]       = useState(false); // IR sensor
  const [crowdLevel, setCrowd]    = useState("Low"); // derived from ultrasonic
  const [alerts, setAlerts]       = useState<string[]>([]);

  // Simulate sensor updates (replace with real BLE data later)
  useEffect(() => {
    const interval = setInterval(() => {
      const newDist = Math.floor(Math.random() * 200) + 20;
      setDistance(newDist);
      setIr(Math.random() > 0.5);
      setCrowd(newDist < 50 ? "High" : newDist < 100 ? "Medium" : "Low");

      // Generate alerts
      const newAlerts: string[] = [];
      if (newDist < 50) {
        newAlerts.push("⚠️ Object very close — " + newDist + "cm");
        Speech.speak("Warning! Object very close, " + newDist + " centimeters away", {
          rate: 1.1,
          pitch: 1.0,
        });
      } else if (newDist < 100) {
        newAlerts.push("👥 Crowd detected nearby");
        Speech.speak("Crowd detected nearby", {
          rate: 1.0,
          pitch: 1.0,
        });
      }
      setAlerts(newAlerts);
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  const distanceColor = distance < 50 ? "#ef4444" : distance < 100 ? "#f59e0b" : "#22c55e";
  const crowdColor    = crowdLevel === "High" ? "#ef4444" : crowdLevel === "Medium" ? "#f59e0b" : "#22c55e";

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Surroundings</Text>
      <Text style={styles.subtitle}>Live environmental awareness</Text>

      {/* Alert banners */}
      {alerts.map((a, i) => (
        <AlertBanner key={i} message={a} color={distance < 50 ? "#ef4444" : "#f59e0b"} />
      ))}

      {/* Metric cards */}
      <View style={styles.grid}>
        <MetricCard
          label="Distance"
          value={String(distance)}
          unit="cm"
          color={distanceColor}
          sublabel={distance < 50 ? "Too close!" : distance < 100 ? "Nearby" : "Clear"}
        />
        <MetricCard
          label="Crowd Level"
          value={crowdLevel}
          unit=""
          color={crowdColor}
        />
        <MetricCard
          label="IR Sensor"
          value={irDetected ? "ON" : "OFF"}
          unit=""
          color={irDetected ? "#f59e0b" : "#444"}
          sublabel={irDetected ? "Motion detected" : "No motion"}
        />
        <MetricCard
          label="Status"
          value="OK"
          unit=""
          color="#22c55e"
          sublabel="Device active"
        />
      </View>

      {/* Distance bar */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Distance Indicator</Text>
        <View style={styles.barBg}>
          <View style={[styles.barFill, {
            width: `${Math.min(100, (200 - distance) / 2)}%` as any,
            backgroundColor: distanceColor
          }]} />
        </View>
        <View style={styles.barLabels}>
          <Text style={styles.barLabel}>Far (200cm)</Text>
          <Text style={styles.barLabel}>Close (0cm)</Text>
        </View>
      </View>

    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: "#0f0f0f" },
  content:       { padding: 24 },
  title:         { fontSize: 24, fontWeight: "bold", color: "#fff", marginBottom: 4 },
  subtitle:      { fontSize: 13, color: "#555", marginBottom: 16 },
  banner:        { backgroundColor: "#1a1a1a", borderLeftWidth: 4, borderRadius: 8, padding: 12, marginBottom: 10 },
  bannerText:    { color: "#fff", fontSize: 14 },
  grid:          { flexDirection: "row", flexWrap: "wrap", gap: 12, marginBottom: 24, marginTop: 8 },
  card:          { backgroundColor: "#1a1a1a", borderRadius: 16, padding: 20, width: "47%" },
  cardLabel:     { fontSize: 11, color: "#666", marginBottom: 8, textTransform: "uppercase", letterSpacing: 1 },
  cardValueRow:  { flexDirection: "row", alignItems: "flex-end", gap: 4 },
  cardValue:     { fontSize: 32, fontWeight: "bold" },
  cardUnit:      { fontSize: 13, color: "#555", marginBottom: 5 },
  cardSublabel:  { fontSize: 11, color: "#555", marginTop: 6 },
  section:       { backgroundColor: "#1a1a1a", borderRadius: 16, padding: 20, marginBottom: 20 },
  sectionTitle:  { fontSize: 13, color: "#555", textTransform: "uppercase", letterSpacing: 1, marginBottom: 16 },
  barBg:         { backgroundColor: "#2a2a2a", borderRadius: 8, height: 16, overflow: "hidden" },
  barFill:       { height: 16, borderRadius: 8 },
  barLabels:     { flexDirection: "row", justifyContent: "space-between", marginTop: 8 },
  barLabel:      { fontSize: 11, color: "#555" },
});