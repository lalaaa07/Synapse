import { LinearGradient } from "expo-linear-gradient";
import { ScrollView, StyleSheet, Text, View } from "react-native";
import { useSensor } from "../Context/SensorContext";

function MetricCard({ label, value, unit, color, sublabel, gradient }: {
  label: string; value: string; unit: string; color: string; sublabel?: string;
  gradient: [string, string];
}) {
  return (
    <LinearGradient colors={gradient} start={{x:0,y:0}} end={{x:1,y:1}} style={styles.card}>
      <Text style={styles.cardLabel}>{label}</Text>
      <View style={styles.cardValueRow}>
        <Text style={[styles.cardValue, { color }]}>{value}</Text>
        <Text style={styles.cardUnit}>{unit}</Text>
      </View>
      {sublabel && <Text style={styles.cardSublabel}>{sublabel}</Text>}
    </LinearGradient>
  );
}

function AlertBanner({ message, color }: { message: string; color: string }) {
  return (
    <LinearGradient
      colors={["#1a1a2e", "#16213e"]}
      style={[styles.banner, { borderLeftColor: color }]}
    >
      <Text style={styles.bannerText}>{message}</Text>
    </LinearGradient>
  );
}

function ScoreBar({ score }: { score: number }) {
  const color = score >= 5 ? "#ef4444" : score >= 3 ? "#f59e0b" : "#22c55e";
  return (
    <View style={styles.scoreContainer}>
      <Text style={styles.sectionTitle}>Episode Risk Score</Text>
      <View style={styles.scoreBarBg}>
        <LinearGradient
          colors={score >= 5 ? ["#ef4444","#dc2626"] : score >= 3 ? ["#f59e0b","#d97706"] : ["#22c55e","#16a34a"]}
          start={{x:0,y:0}} end={{x:1,y:0}}
          style={[styles.scoreBarFill, { width: `${(score / 6) * 100}%` as any }]}
        />
      </View>
      <View style={styles.scoreLabels}>
        {[0,1,2,3,4,5,6].map(n => (
          <Text key={n} style={[styles.scoreLabel, score === n && { color }]}>{n}</Text>
        ))}
      </View>
      <Text style={[styles.scoreText, { color }]}>
        {score >= 5 ? "Severe" : score >= 3 ? "Elevated" : "Normal"}
      </Text>
    </View>
  );
}

export default function Dashboard() {
  const { sensorData } = useSensor();
  const { bpm, dist, ir, tremor, agit, fall, still, score, ax, ay, az, gx, gy, gz } = sensorData;

  const distColor = dist < 0 ? "#555" : dist < 50 ? "#ef4444" : dist < 100 ? "#f59e0b" : "#22c55e";
  const bpmColor  = bpm > 130 || (bpm > 0 && bpm < 48) ? "#ef4444" : bpm === 0 ? "#555" : "#22c55e";

  const alerts = [];
  if (fall)                        alerts.push({ msg: "🚨 Fall detected!", color: "#ef4444" });
  if (score >= 3)                  alerts.push({ msg: `⚠️ Episode risk: ${score}/6`, color: "#f59e0b" });
  if (dist > 0 && dist < 50)      alerts.push({ msg: `📡 Object very close — ${dist}cm`, color: "#ef4444" });
  if (tremor)                      alerts.push({ msg: "〰️ Tremor detected", color: "#f59e0b" });
  if (agit)                        alerts.push({ msg: "⚡ Agitation detected", color: "#f59e0b" });
  if (still)                       alerts.push({ msg: "🔴 No movement detected", color: "#f59e0b" });
  if (ir)                          alerts.push({ msg: "🔴 IR motion detected", color: "#f59e0b" });

  return (
    <LinearGradient colors={["#0a0015", "#0d0628", "#0a1628"]} style={styles.bg}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Synapse</Text>
        <Text style={styles.subtitle}>Live sensor data</Text>

        {alerts.map((a, i) => <AlertBanner key={i} message={a.msg} color={a.color} />)}

        {/* Episode score */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <ScoreBar score={score} />
        </LinearGradient>

        {/* Primary metrics */}
        <View style={styles.grid}>
          <MetricCard
            label="Heart Rate"
            value={bpm > 0 ? String(bpm) : "--"}
            unit="BPM"
            color={bpmColor}
            sublabel={bpm > 130 ? "Too high!" : bpm < 48 && bpm > 0 ? "Too low!" : "Normal"}
            gradient={["#0f0c29", "#302b63"]}
          />
          <MetricCard
            label="Distance"
            value={dist > 0 ? String(Math.round(dist)) : "--"}
            unit="cm"
            color={distColor}
            sublabel={dist < 0 ? "No reading" : dist < 50 ? "Too close!" : dist < 100 ? "Nearby" : "Clear"}
            gradient={["#0f0c29", "#24243e"]}
          />
          <MetricCard
            label="IR Sensor"
            value={ir ? "ON" : "OFF"}
            unit=""
            color={ir ? "#f59e0b" : "#444"}
            sublabel={ir ? "Motion detected" : "No motion"}
            gradient={["#1a0533", "#2d1b69"]}
          />
          <MetricCard
            label="Tremor"
            value={tremor ? "YES" : "NO"}
            unit=""
            color={tremor ? "#f59e0b" : "#22c55e"}
            sublabel={agit ? "Agitation too" : ""}
            gradient={["#0f0c29", "#302b63"]}
          />
        </View>

        {/* Motion data */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>Motion Data</Text>
          <View style={styles.motionGrid}>
            {[
              { label: "Accel X", value: `${ax.toFixed(2)}g` },
              { label: "Accel Y", value: `${ay.toFixed(2)}g` },
              { label: "Accel Z", value: `${az.toFixed(2)}g` },
              { label: "Gyro X",  value: `${gx.toFixed(1)}°/s` },
              { label: "Gyro Y",  value: `${gy.toFixed(1)}°/s` },
              { label: "Gyro Z",  value: `${gz.toFixed(1)}°/s` },
            ].map(({ label, value }) => (
              <LinearGradient key={label} colors={["#1a0a3e", "#0d1b3e"]} style={styles.motionItem}>
                <Text style={styles.motionLabel}>{label}</Text>
                <Text style={styles.motionValue}>{value}</Text>
              </LinearGradient>
            ))}
          </View>
        </LinearGradient>

      </ScrollView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  bg:             { flex: 1 },
  content:        { padding: 24, paddingBottom: 48 },
  title:          { fontSize: 28, fontWeight: "bold", color: "#fff", marginBottom: 4 },
  subtitle:       { fontSize: 13, color: "#6366f1", marginBottom: 16, letterSpacing: 1 },
  banner:         { borderLeftWidth: 4, borderRadius: 8, padding: 12, marginBottom: 8 },
  bannerText:     { color: "#fff", fontSize: 14 },
  section:        { borderRadius: 16, padding: 20, marginBottom: 16 },
  sectionTitle:   { fontSize: 12, color: "#6366f1", textTransform: "uppercase", letterSpacing: 1, marginBottom: 12 },
  grid:           { flexDirection: "row", flexWrap: "wrap", gap: 12, marginBottom: 16 },
  card:           { borderRadius: 16, padding: 20, width: "47%" },
  cardLabel:      { fontSize: 11, color: "#818cf8", marginBottom: 8, textTransform: "uppercase", letterSpacing: 1 },
  cardValueRow:   { flexDirection: "row", alignItems: "flex-end", gap: 4 },
  cardValue:      { fontSize: 32, fontWeight: "bold" },
  cardUnit:       { fontSize: 13, color: "#6366f1", marginBottom: 5 },
  cardSublabel:   { fontSize: 11, color: "#4f46e5", marginTop: 6 },
  scoreContainer: { width: "100%" },
  scoreBarBg:     { backgroundColor: "#1e1b4b", borderRadius: 8, height: 12, overflow: "hidden", marginBottom: 8 },
  scoreBarFill:   { height: 12, borderRadius: 8 },
  scoreLabels:    { flexDirection: "row", justifyContent: "space-between", marginBottom: 4 },
  scoreLabel:     { fontSize: 11, color: "#4f46e5" },
  scoreText:      { fontSize: 13, fontWeight: "600", textAlign: "center" },
  motionGrid:     { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  motionItem:     { width: "30%", borderRadius: 8, padding: 10 },
  motionLabel:    { fontSize: 10, color: "#6366f1", marginBottom: 4 },
  motionValue:    { fontSize: 14, fontWeight: "600", color: "#fff" },
});