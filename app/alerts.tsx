import { LinearGradient } from "expo-linear-gradient";
import { ScrollView, StyleSheet, Switch, Text, TouchableOpacity, View } from "react-native";
import { useSensor } from "../Context/SensorContext";

const MOTOR_PATTERNS = [
  { cmd: "calm",      label: "Calm",      desc: "Slow breathing",     colors: ["#1e3a5f","#2563eb"] as [string,string] },
  { cmd: "pulse",     label: "Pulse",     desc: "Rhythmic taps",      colors: ["#2d1b69","#7c3aed"] as [string,string] },
  { cmd: "breathe",   label: "Breathe",   desc: "Ramp up/down",       colors: ["#0c4a6e","#0891b2"] as [string,string] },
  { cmd: "grounding", label: "Grounding", desc: "Steady beat",        colors: ["#064e3b","#059669"] as [string,string] },
  { cmd: "alert",     label: "Alert",     desc: "Rapid buzz",         colors: ["#7f1d1d","#dc2626"] as [string,string] },
  { cmd: "stop",      label: "Stop",      desc: "Stop motor",         colors: ["#1f2937","#374151"] as [string,string] },
];

export default function Alerts() {
  const { alertSettings, setAlertSettings, alertHistory, playAlert, stopAlert, isPlaying } = useSensor();

  function update(key: string, value: any) {
    setAlertSettings({ ...alertSettings, [key]: value });
  }

  const { sendMotorCommand } = useSensor();

  return (
    <LinearGradient colors={["#0a0015", "#0d0628", "#0a1628"]} style={styles.bg}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Alerts & Controls</Text>
        <Text style={styles.subtitle}>Configure alerts and control the device</Text>

        {/* Alert history */}
        {alertHistory.length > 0 && (
          <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
            <Text style={styles.sectionTitle}>Recent Alerts</Text>
            {alertHistory.slice(0, 5).map((a, i) => (
              <View key={i} style={styles.historyRow}>
                <Text style={styles.historyType}>{a.type}</Text>
                <Text style={styles.historyDetail}>{a.detail}</Text>
                <Text style={styles.historyScore}>score: {a.score}</Text>
              </View>
            ))}
          </LinearGradient>
        )}

        {/* Alert toggles */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>Alert Settings</Text>
          {[
            { key: "fallAlert",     label: "Fall Detection",    sub: "Alert on sudden fall" },
            { key: "episodeAlert",  label: "Episode Detection", sub: "Alert when score ≥ 3" },
            { key: "distanceAlert", label: "Distance Warning",  sub: `Alert under ${alertSettings.threshold}cm` },
            { key: "irAlert",       label: "IR Motion Alert",   sub: "Alert on motion detected" },
            { key: "crowdAlert",    label: "Crowd Detection",   sub: "Alert when crowd nearby" },
          ].map(({ key, label, sub }) => (
            <View key={key} style={styles.row}>
              <View>
                <Text style={styles.rowLabel}>{label}</Text>
                <Text style={styles.rowSub}>{sub}</Text>
              </View>
              <Switch
                value={(alertSettings as any)[key]}
                onValueChange={v => update(key, v)}
                trackColor={{ true: "#6366f1" }}
                thumbColor="#818cf8"
              />
            </View>
          ))}
        </LinearGradient>

        {/* Distance threshold */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>Distance Threshold</Text>
          <View style={styles.thresholdRow}>
            <TouchableOpacity style={styles.thresholdBtn} onPress={() => update("threshold", Math.max(20, alertSettings.threshold - 10))}>
              <Text style={styles.thresholdBtnText}>−</Text>
            </TouchableOpacity>
            <Text style={styles.thresholdValue}>{alertSettings.threshold} cm</Text>
            <TouchableOpacity style={styles.thresholdBtn} onPress={() => update("threshold", Math.min(300, alertSettings.threshold + 10))}>
              <Text style={styles.thresholdBtnText}>+</Text>
            </TouchableOpacity>
          </View>
        </LinearGradient>

        {/* HR thresholds */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>Heart Rate Thresholds</Text>
          <View style={styles.hrRow}>
            {[
              { key: "hrLow",  label: "Low BPM",  min: 30, max: 60 },
              { key: "hrHigh", label: "High BPM", min: 100, max: 200 },
            ].map(({ key, label, min, max }) => (
              <View key={key} style={styles.hrItem}>
                <Text style={styles.rowSub}>{label}</Text>
                <View style={styles.thresholdRow}>
                  <TouchableOpacity style={styles.thresholdBtnSm} onPress={() => update(key, Math.max(min, (alertSettings as any)[key] - 1))}>
                    <Text style={styles.thresholdBtnText}>−</Text>
                  </TouchableOpacity>
                  <Text style={styles.thresholdValueSm}>{(alertSettings as any)[key]}</Text>
                  <TouchableOpacity style={styles.thresholdBtnSm} onPress={() => update(key, Math.min(max, (alertSettings as any)[key] + 1))}>
                    <Text style={styles.thresholdBtnText}>+</Text>
                  </TouchableOpacity>
                </View>
              </View>
            ))}
          </View>
        </LinearGradient>

        {/* Motor patterns */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>Haptic Motor Patterns</Text>
          <View style={styles.motorGrid}>
            {MOTOR_PATTERNS.map(p => (
              <TouchableOpacity key={p.cmd} onPress={() => sendMotorCommand(p.cmd)} style={styles.motorBtnWrap}>
                <LinearGradient colors={p.colors} start={{x:0,y:0}} end={{x:1,y:1}} style={styles.motorBtn}>
                  <Text style={styles.motorBtnLabel}>{p.label}</Text>
                  <Text style={styles.motorBtnDesc}>{p.desc}</Text>
                </LinearGradient>
              </TouchableOpacity>
            ))}
          </View>
        </LinearGradient>

        {/* Audio controls */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>Audio Controls</Text>
          <TouchableOpacity onPress={isPlaying ? stopAlert : playAlert}>
            <LinearGradient
              colors={isPlaying ? ["#7f1d1d","#dc2626"] : ["#1e1b4b","#4f46e5"]}
              start={{x:0,y:0}} end={{x:1,y:0}}
              style={styles.audioBtn}
            >
              <Text style={styles.audioBtnText}>
                {isPlaying ? "⏹ Stop Audio" : "▶️ Test Audio Alert"}
              </Text>
            </LinearGradient>
          </TouchableOpacity>
        </LinearGradient>

      </ScrollView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  bg:               { flex: 1 },
  content:          { padding: 24, paddingBottom: 48 },
  title:            { fontSize: 24, fontWeight: "bold", color: "#fff", marginBottom: 4 },
  subtitle:         { fontSize: 13, color: "#6366f1", marginBottom: 24, letterSpacing: 1 },
  section:          { borderRadius: 16, padding: 20, marginBottom: 16 },
  sectionTitle:     { fontSize: 12, color: "#6366f1", textTransform: "uppercase", letterSpacing: 1, marginBottom: 16 },
  row:              { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: "#1e1b4b" },
  rowLabel:         { color: "#fff", fontSize: 15, fontWeight: "500" },
  rowSub:           { color: "#4f46e5", fontSize: 12, marginTop: 2 },
  thresholdRow:     { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 16 },
  thresholdBtn:     { backgroundColor: "#4f46e5", width: 44, height: 44, borderRadius: 22, alignItems: "center", justifyContent: "center" },
  thresholdBtnSm:   { backgroundColor: "#4f46e5", width: 32, height: 32, borderRadius: 16, alignItems: "center", justifyContent: "center" },
  thresholdBtnText: { color: "#fff", fontSize: 20, fontWeight: "bold" },
  thresholdValue:   { color: "#fff", fontSize: 28, fontWeight: "bold", minWidth: 100, textAlign: "center" },
  thresholdValueSm: { color: "#fff", fontSize: 20, fontWeight: "bold", minWidth: 60, textAlign: "center" },
  hrRow:            { flexDirection: "row", justifyContent: "space-around" },
  hrItem:           { alignItems: "center", gap: 8 },
  motorGrid:        { flexDirection: "row", flexWrap: "wrap", gap: 10 },
  motorBtnWrap:     { width: "47%" },
  motorBtn:         { borderRadius: 12, padding: 14 },
  motorBtnLabel:    { fontSize: 15, fontWeight: "600", color: "#fff", marginBottom: 4 },
  motorBtnDesc:     { fontSize: 11, color: "rgba(255,255,255,0.6)" },
  audioBtn:         { borderRadius: 10, padding: 14, alignItems: "center" },
  audioBtnText:     { color: "#fff", fontWeight: "600", fontSize: 15 },
  historyRow:       { flexDirection: "row", justifyContent: "space-between", paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: "#1e1b4b" },
  historyType:      { color: "#ef4444", fontSize: 13, fontWeight: "600", width: "35%" },
  historyDetail:    { color: "#818cf8", fontSize: 12, width: "40%" },
  historyScore:     { color: "#4f46e5", fontSize: 12 },
});