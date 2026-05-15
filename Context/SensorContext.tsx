import { Audio } from "expo-av";
import { createContext, ReactNode, useContext, useEffect, useRef, useState } from "react";
import { atob } from "react-native-quick-base64";

// ── BLE UUIDs (must match firmware exactly) ───────────────────────
const SERVICE_UUID  = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
const SENSOR_CHAR   = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
const ALERT_CHAR    = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
const MOTOR_CHAR    = "6e400004-b5a3-f393-e0a9-e50e24dcca9e";

const bleManager = new BleManager();

// ── Types ─────────────────────────────────────────────────────────
export interface SensorData {
  bpm: number;
  ax: number; ay: number; az: number;
  gx: number; gy: number; gz: number;
  dist: number;
  ir: boolean;
  tremor: boolean;
  agit: boolean;
  fall: boolean;
  still: boolean;
  score: number;
  motor: boolean;
  uptime: number;
}

export interface AlertEvent {
  type: string;
  detail: string;
  score: number;
  ts: number;
}

export interface AlertSettings {
  crowdAlert: boolean;
  distanceAlert: boolean;
  irAlert: boolean;
  fallAlert: boolean;
  episodeAlert: boolean;
  vibrate: boolean;
  threshold: number;
  hrHigh: number;
  hrLow: number;
}

interface SensorContextType {
  sensorData: SensorData;
  setSensorData: (data: SensorData) => void;
  connected: boolean;
  setConnected: (val: boolean) => void;
  scanning: boolean;
  scanAndConnect: () => void;
  disconnect: () => void;
  sendMotorCommand: (cmd: string) => void;
  alertSettings: AlertSettings;
  setAlertSettings: (settings: AlertSettings) => void;
  alertHistory: AlertEvent[];
  addAlert: (alert: AlertEvent) => void;
  playAlert: () => void;
  stopAlert: () => void;
  isPlaying: boolean;
}

const defaultSensorData: SensorData = {
  bpm: 0, ax: 0, ay: 0, az: 0,
  gx: 0, gy: 0, gz: 0,
  dist: -1, ir: false, tremor: false,
  agit: false, fall: false, still: false,
  score: 0, motor: false, uptime: 0,
};

const SensorContext = createContext<SensorContextType | null>(null);

export function SensorProvider({ children }: { children: ReactNode }) {
  const [connected, setConnected]       = useState(false);
  const [scanning, setScanning]         = useState(false);
  const [isPlaying, setIsPlaying]       = useState(false);
  const [alertHistory, setAlertHistory] = useState<AlertEvent[]>([]);
  const [sensorData, setSensorDataRaw]  = useState<SensorData>(defaultSensorData);
  const [alertSettings, setAlertSettings] = useState<AlertSettings>({
    crowdAlert: true, distanceAlert: true, irAlert: true,
    fallAlert: true, episodeAlert: true, vibrate: false,
    threshold: 100, hrHigh: 130, hrLow: 48,
  });

  const soundRef      = useRef<Audio.Sound | null>(null);
  const isPlayingRef  = useRef(false);
  const deviceRef     = useRef<Device | null>(null);

  // ── Load audio ────────────────────────────────────────────────
  useEffect(() => {
    async function loadSound() {
      await Audio.setAudioModeAsync({
        playsInSilentModeIOS: true,
        staysActiveInBackground: true,
      });
      const { sound } = await Audio.Sound.createAsync(
        require("../assets/sounds/Interstellar.mp3")
      );
      soundRef.current = sound;
    }
    loadSound();
    return () => { soundRef.current?.unloadAsync(); };
  }, []);

  // ── Audio controls ────────────────────────────────────────────
  async function playAlert() {
    if (!soundRef.current || isPlayingRef.current) return;
    try {
      isPlayingRef.current = true;
      setIsPlaying(true);
      await soundRef.current.setPositionAsync(0);
      await soundRef.current.setIsLoopingAsync(true);
      await soundRef.current.playAsync();
    } catch (e) { console.error("Playback error:", e); }
  }

  async function stopAlert() {
    if (!soundRef.current) return;
    isPlayingRef.current = false;
    setIsPlaying(false);
    await soundRef.current.setIsLoopingAsync(false);
    await soundRef.current.stopAsync();
  }

  // ── Process incoming sensor data ──────────────────────────────
  function setSensorData(data: SensorData) {
    setSensorDataRaw(data);
    if (
      (alertSettings.fallAlert     && data.fall)       ||
      (alertSettings.episodeAlert  && data.score >= 3) ||
      (alertSettings.distanceAlert && data.dist > 0 && data.dist < 50)
    ) {
      playAlert();
    }
  }

  // ── Alert history ─────────────────────────────────────────────
  function addAlert(alert: AlertEvent) {
    setAlertHistory(prev => [alert, ...prev].slice(0, 20));
  }

  // ── BLE scan & connect ────────────────────────────────────────
  function scanAndConnect() {
    setScanning(true);
    bleManager.startDeviceScan(null, null, async (error, device) => {
      if (error) { console.error("Scan error:", error); setScanning(false); return; }
      if (device?.name === "Synapse") {
        bleManager.stopDeviceScan();
        setScanning(false);
        try {
          const conn = await device.connect();
          await conn.discoverAllServicesAndCharacteristics();
          deviceRef.current = conn;
          setConnected(true);

          // Subscribe to sensor data
          conn.monitorCharacteristicForService(
            SERVICE_UUID, SENSOR_CHAR,
            (err, char) => {
              if (err || !char?.value) return;
              try {
                const json = atob(char.value);
                const data = JSON.parse(json) as SensorData;
                setSensorData(data);
              } catch (e) { console.error("Parse error:", e); }
            }
          );

          // Subscribe to alert events
          conn.monitorCharacteristicForService(
            SERVICE_UUID, ALERT_CHAR,
            (err, char) => {
              if (err || !char?.value) return;
              try {
                const json = atob(char.value);
                const alert = JSON.parse(json) as AlertEvent;
                if (alert.type) addAlert(alert);
              } catch (e) { console.error("Alert parse error:", e); }
            }
          );

          // Handle disconnect
          conn.onDisconnected(() => {
            setConnected(false);
            deviceRef.current = null;
            setSensorDataRaw(defaultSensorData);
          });

        } catch (e) { console.error("Connection error:", e); }
      }
    });

    // Stop scan after 10 seconds
    setTimeout(() => {
      bleManager.stopDeviceScan();
      setScanning(false);
    }, 10000);
  }

  // ── Disconnect ────────────────────────────────────────────────
  async function disconnect() {
    if (deviceRef.current) {
      await deviceRef.current.cancelConnection();
      setConnected(false);
      deviceRef.current = null;
      setSensorDataRaw(defaultSensorData);
    }
  }

  // ── Send motor command ────────────────────────────────────────
  async function sendMotorCommand(cmd: string) {
    if (!deviceRef.current || !connected) return;
    try {
      const encoded = btoa(cmd);
      await deviceRef.current.writeCharacteristicWithResponseForService(
        SERVICE_UUID, MOTOR_CHAR, encoded
      );
      console.log("Motor command sent:", cmd);
    } catch (e) { console.error("Motor command error:", e); }
  }

  return (
    <SensorContext.Provider value={{
      sensorData, setSensorData,
      connected, setConnected,
      scanning, scanAndConnect, disconnect,
      sendMotorCommand,
      alertSettings, setAlertSettings,
      alertHistory, addAlert,
      playAlert, stopAlert, isPlaying,
    }}>
      {children}
    </SensorContext.Provider>
  );
}

export function useSensor() {
  const ctx = useContext(SensorContext);
  if (!ctx) throw new Error("useSensor must be used within SensorProvider");
  return ctx;
}