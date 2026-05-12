import { Audio } from "expo-av";
import { createContext, ReactNode, useContext, useEffect, useRef, useState } from "react";

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
  alertSettings: AlertSettings;
  setAlertSettings: (settings: AlertSettings) => void;
  alertHistory: AlertEvent[];
  addAlert: (alert: AlertEvent) => void;
  playAlert: () => void;
  stopAlert: () => void;
  isPlaying: boolean;
}

// ── Default sensor state ──────────────────────────────────────────
const defaultSensorData: SensorData = {
  bpm: 0,
  ax: 0, ay: 0, az: 0,
  gx: 0, gy: 0, gz: 0,
  dist: -1,
  ir: false,
  tremor: false,
  agit: false,
  fall: false,
  still: false,
  score: 0,
  motor: false,
  uptime: 0,
};

const SensorContext = createContext<SensorContextType | null>(null);

export function SensorProvider({ children }: { children: ReactNode }) {
  const [connected, setConnected]       = useState(false);
  const [isPlaying, setIsPlaying]       = useState(false);
  const [alertHistory, setAlertHistory] = useState<AlertEvent[]>([]);
  const soundRef                        = useRef<Audio.Sound | null>(null);
  const isPlayingRef                    = useRef(false);
  const [sensorData, setSensorDataRaw]  = useState<SensorData>(defaultSensorData);
  const [alertSettings, setAlertSettings] = useState<AlertSettings>({
    crowdAlert:    true,
    distanceAlert: true,
    irAlert:       true,
    fallAlert:     true,
    episodeAlert:  true,
    vibrate:       false,
    threshold:     100,
    hrHigh:        130,
    hrLow:         48,
  });

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

  async function playAlert() {
    if (!soundRef.current || isPlayingRef.current) return;
    try {
      isPlayingRef.current = true;
      setIsPlaying(true);
      await soundRef.current.setPositionAsync(0);
      await soundRef.current.setIsLoopingAsync(true);
      await soundRef.current.playAsync();
    } catch (e) {
      console.error("Playback error:", e);
    }
  }

  async function stopAlert() {
    if (!soundRef.current) return;
    isPlayingRef.current = false;
    setIsPlaying(false);
    await soundRef.current.setIsLoopingAsync(false);
    await soundRef.current.stopAsync();
  }

  // ── Add alert to history ──────────────────────────────────────
  function addAlert(alert: AlertEvent) {
    setAlertHistory(prev => [alert, ...prev].slice(0, 20)); // keep last 20
  }

  // ── Process incoming sensor data ──────────────────────────────
  function setSensorData(data: SensorData) {
    setSensorDataRaw(data);

    const shouldPlay =
      (alertSettings.fallAlert     && data.fall)        ||
      (alertSettings.episodeAlert  && data.score >= 3)  ||
      (alertSettings.distanceAlert && data.dist > 0 && data.dist < 50);

    if (shouldPlay) {
      playAlert();
    }
  }

  // ── Simulated data — remove when real BLE connected ───────────
  useEffect(() => {
    const interval = setInterval(() => {
      const dist  = Math.floor(Math.random() * 200) + 20;
      const bpm   = Math.floor(Math.random() * 60) + 60;
      const score = Math.floor(Math.random() * 5);
      setSensorData({
        bpm,
        ax: parseFloat((Math.random() * 0.1).toFixed(2)),
        ay: parseFloat((Math.random() * 0.1).toFixed(2)),
        az: parseFloat((0.9 + Math.random() * 0.1).toFixed(2)),
        gx: parseFloat((Math.random() * 2).toFixed(1)),
        gy: parseFloat((Math.random() * 2).toFixed(1)),
        gz: parseFloat((Math.random() * 0.5).toFixed(1)),
        dist,
        ir:     Math.random() > 0.7,
        tremor: Math.random() > 0.8,
        agit:   Math.random() > 0.85,
        fall:   Math.random() > 0.95,
        still:  Math.random() > 0.9,
        score,
        motor:  false,
        uptime: Date.now(),
      });
    }, 2000);
    return () => clearInterval(interval);
  }, [alertSettings]);

  return (
    <SensorContext.Provider value={{
      sensorData, setSensorData,
      connected, setConnected,
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