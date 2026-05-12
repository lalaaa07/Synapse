import AsyncStorage from "@react-native-async-storage/async-storage";
import { LinearGradient } from "expo-linear-gradient";
import * as SMS from "expo-sms";
import { useEffect, useRef, useState } from "react";
import { Alert, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import { useSensor } from "../Context/SensorContext";

interface Contact {
  id: string;
  name: string;
  phone: string;
}

export default function Emergency() {
  const { sensorData } = useSensor();
  const [contacts, setContacts]   = useState<Contact[]>([]);
  const [name, setName]           = useState("");
  const [phone, setPhone]         = useState("");
  const [sending, setSending]     = useState(false);

  // Load saved contacts on mount
  useEffect(() => {
    loadContacts();
  }, []);

  // Auto-send SMS on fall or high episode score
  const lastAlertRef = useRef<number>(0);

  useEffect(() => {
    const now = Date.now();
    if (now - lastAlertRef.current < 60000) return; // 1 min debounce

    if (sensorData.fall && contacts.length > 0) {
      lastAlertRef.current = now;
      sendEmergencySMS("🚨 FALL DETECTED", sensorData.score);
    } else if (sensorData.score >= 5 && contacts.length > 0) {
      lastAlertRef.current = now;
      sendEmergencySMS("⚠️ HIGH EPISODE RISK", sensorData.score);
    }
  }, [sensorData.fall, sensorData.score]);

  async function loadContacts() {
    try {
      const saved = await AsyncStorage.getItem("emergency_contacts");
      if (saved) setContacts(JSON.parse(saved));
    } catch (e) {
      console.error("Failed to load contacts:", e);
    }
  }

  async function saveContacts(updated: Contact[]) {
    try {
      await AsyncStorage.setItem("emergency_contacts", JSON.stringify(updated));
    } catch (e) {
      console.error("Failed to save contacts:", e);
    }
  }

  function addContact() {
    if (!name.trim() || !phone.trim()) {
      Alert.alert("Missing info", "Please enter both a name and phone number.");
      return;
    }
    if (phone.replace(/\D/g, "").length < 7) {
      Alert.alert("Invalid number", "Please enter a valid phone number.");
      return;
    }
    const newContact: Contact = {
      id:    Date.now().toString(),
      name:  name.trim(),
      phone: phone.trim(),
    };
    const updated = [...contacts, newContact];
    setContacts(updated);
    saveContacts(updated);
    setName("");
    setPhone("");
  }

  function removeContact(id: string) {
    Alert.alert("Remove contact", "Are you sure?", [
      { text: "Cancel", style: "cancel" },
      {
        text: "Remove", style: "destructive",
        onPress: () => {
          const updated = contacts.filter(c => c.id !== id);
          setContacts(updated);
          saveContacts(updated);
        }
      }
    ]);
  }

  async function sendEmergencySMS(reason: string, score: number) {
    if (sending) return;
    const now = Date.now();
    // Debounce — don't send more than once per minute

    const isAvailable = await SMS.isAvailableAsync();
    if (!isAvailable) {
      Alert.alert("SMS not available", "This device cannot send SMS messages.");
      return;
    }

    const numbers = contacts.map(c => c.phone);
    const time    = new Date().toLocaleTimeString();
    const message =
      `SYNAPSE ALERT\n` +
      `${reason}\n` +
      `Episode score: ${score}/6\n` +
      `Time: ${time}\n\n` +
      `Please check on the wearer immediately.`;

    try {
      setSending(true);
      await SMS.sendSMSAsync(numbers, message);
    } catch (e) {
      console.error("SMS error:", e);
    } finally {
      setSending(false);
    }
  }

  async function sendTestSMS() {
    if (contacts.length === 0) {
      Alert.alert("No contacts", "Please add an emergency contact first.");
      return;
    }
    await sendEmergencySMS("🔔 TEST ALERT", 0);
  }

  return (
    <LinearGradient colors={["#0a0015", "#0d0628", "#0a1628"]} style={styles.bg}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Emergency SOS</Text>
        <Text style={styles.subtitle}>Alert contacts automatically on fall or high risk</Text>

        {/* SOS button */}
        <TouchableOpacity onPress={() => sendEmergencySMS("🆘 MANUAL SOS", sensorData.score)}>
          <LinearGradient colors={["#7f1d1d", "#dc2626"]} style={styles.sosButton}>
            <Text style={styles.sosText}>🆘 SEND SOS NOW</Text>
            <Text style={styles.sosSubtext}>Sends to all emergency contacts</Text>
          </LinearGradient>
        </TouchableOpacity>

        {/* Auto alert status */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>Auto Alert Triggers</Text>
          <View style={styles.triggerRow}>
            <Text style={styles.triggerDot}>🟢</Text>
            <View>
              <Text style={styles.triggerLabel}>Fall detected</Text>
              <Text style={styles.triggerSub}>Sends SMS immediately</Text>
            </View>
          </View>
          <View style={styles.triggerRow}>
            <Text style={styles.triggerDot}>🟢</Text>
            <View>
              <Text style={styles.triggerLabel}>Episode score ≥ 5</Text>
              <Text style={styles.triggerSub}>Sends SMS immediately</Text>
            </View>
          </View>
        </LinearGradient>

        {/* Add contact */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>Add Emergency Contact</Text>

          <Text style={styles.inputLabel}>Name</Text>
          <TextInput
            style={styles.input}
            value={name}
            onChangeText={setName}
            placeholder="e.g. Mom"
            placeholderTextColor="#2d2b55"
            autoCorrect={false}
          />

          <Text style={styles.inputLabel}>Phone Number</Text>
          <TextInput
            style={styles.input}
            value={phone}
            onChangeText={setPhone}
            placeholder="e.g. +91 98765 43210"
            placeholderTextColor="#2d2b55"
            keyboardType="phone-pad"
            autoCorrect={false}
          />

          <TouchableOpacity onPress={addContact}>
            <LinearGradient
              colors={["#1e1b4b", "#4f46e5"]}
              start={{x:0,y:0}} end={{x:1,y:0}}
              style={styles.addButton}
            >
              <Text style={styles.addButtonText}>+ Add Contact</Text>
            </LinearGradient>
          </TouchableOpacity>
        </LinearGradient>

        {/* Contact list */}
        <LinearGradient colors={["#12002a", "#0d1b3e"]} style={styles.section}>
          <Text style={styles.sectionTitle}>
            Emergency Contacts ({contacts.length})
          </Text>

          {contacts.length === 0 ? (
            <Text style={styles.emptyText}>No contacts added yet</Text>
          ) : (
            contacts.map(contact => (
              <View key={contact.id} style={styles.contactRow}>
                <LinearGradient colors={["#1e1b4b", "#2d2b69"]} style={styles.contactAvatar}>
                  <Text style={styles.contactAvatarText}>
                    {contact.name.charAt(0).toUpperCase()}
                  </Text>
                </LinearGradient>
                <View style={styles.contactInfo}>
                  <Text style={styles.contactName}>{contact.name}</Text>
                  <Text style={styles.contactPhone}>{contact.phone}</Text>
                </View>
                <TouchableOpacity
                  style={styles.removeBtn}
                  onPress={() => removeContact(contact.id)}
                >
                  <Text style={styles.removeBtnText}>✕</Text>
                </TouchableOpacity>
              </View>
            ))
          )}
        </LinearGradient>

        {/* Test button */}
        <TouchableOpacity onPress={sendTestSMS}>
          <LinearGradient
            colors={["#1e1b4b", "#4f46e5"]}
            start={{x:0,y:0}} end={{x:1,y:0}}
            style={styles.testButton}
          >
            <Text style={styles.testButtonText}>📨 Send Test SMS</Text>
          </LinearGradient>
        </TouchableOpacity>

      </ScrollView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  bg:                { flex: 1 },
  content:           { padding: 24, paddingBottom: 48 },
  title:             { fontSize: 24, fontWeight: "bold", color: "#fff", marginBottom: 4 },
  subtitle:          { fontSize: 13, color: "#6366f1", marginBottom: 24, letterSpacing: 1 },
  sosButton:         { borderRadius: 16, padding: 24, alignItems: "center", marginBottom: 20 },
  sosText:           { fontSize: 22, fontWeight: "bold", color: "#fff" },
  sosSubtext:        { fontSize: 12, color: "rgba(255,255,255,0.6)", marginTop: 4 },
  section:           { borderRadius: 16, padding: 20, marginBottom: 16 },
  sectionTitle:      { fontSize: 12, color: "#6366f1", textTransform: "uppercase", letterSpacing: 1, marginBottom: 16 },
  triggerRow:        { flexDirection: "row", alignItems: "center", gap: 12, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: "#1e1b4b" },
  triggerDot:        { fontSize: 16 },
  triggerLabel:      { color: "#fff", fontSize: 14, fontWeight: "500" },
  triggerSub:        { color: "#4f46e5", fontSize: 12, marginTop: 2 },
  inputLabel:        { color: "#818cf8", fontSize: 12, marginBottom: 6, textTransform: "uppercase", letterSpacing: 1 },
  input:             { backgroundColor: "#0a0015", borderWidth: 1, borderColor: "#1e1b4b", borderRadius: 10, padding: 14, color: "#fff", fontSize: 15, marginBottom: 16 },
  addButton:         { borderRadius: 10, padding: 14, alignItems: "center" },
  addButtonText:     { color: "#fff", fontWeight: "600", fontSize: 15 },
  emptyText:         { color: "#2d2b55", fontSize: 14, textAlign: "center", paddingVertical: 16 },
  contactRow:        { flexDirection: "row", alignItems: "center", gap: 12, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: "#1e1b4b" },
  contactAvatar:     { width: 44, height: 44, borderRadius: 22, alignItems: "center", justifyContent: "center" },
  contactAvatarText: { color: "#fff", fontSize: 18, fontWeight: "bold" },
  contactInfo:       { flex: 1 },
  contactName:       { color: "#fff", fontSize: 15, fontWeight: "600" },
  contactPhone:      { color: "#6366f1", fontSize: 13, marginTop: 2 },
  removeBtn:         { backgroundColor: "#1e1b4b", width: 32, height: 32, borderRadius: 16, alignItems: "center", justifyContent: "center" },
  removeBtnText:     { color: "#ef4444", fontSize: 14, fontWeight: "bold" },
  testButton:        { borderRadius: 10, padding: 14, alignItems: "center" },
  testButtonText:    { color: "#fff", fontWeight: "600", fontSize: 15 },
});