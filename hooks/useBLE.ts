import * as Speech from "expo-speech";
import { useCallback, useState } from "react";
import { BleManager, Device } from "react-native-ble-plx";

// ── Change these to match your ESP32 firmware UUIDs ──
export const SERVICE_UUID      = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
export const DISTANCE_CHAR     = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
export const IR_CHAR           = "beb5483e-36e1-4688-b7f5-ea07361b26a9";
export const CONTROL_CHAR      = "beb5483e-36e1-4688-b7f5-ea07361b26ab";

const manager = new BleManager();

export interface SensorData {
  distance: number;   // cm from ultrasonic
  irDetected: boolean; // IR sensor
  crowdLevel: "Low" | "Medium" | "High";
}

export function useBLE() {
  const [device, setDevice]         = useState<Device | null>(null);
  const [connected, setConnected]   = useState(false);
  const [scanning, setScanning]     = useState(false);
  const [sensorData, setSensorData] = useState<SensorData>({
    distance: 200,
    irDetected: false,
    crowdLevel: "Low",
  });

  // ── Scan for ESP32 ──
  const scanAndConnect = useCallback(() => {
    setScanning(true);

    manager.startDeviceScan(null, null, async (error, scannedDevice) => {
      if (error) {
        console.error("Scan error:", error);
        setScanning(false);
        return;
      }

      if (scannedDevice?.name === "MyWearable") {
        manager.stopDeviceScan();
        setScanning(false);

        try {
          const conn = await scannedDevice.connect();
          await conn.discoverAllServicesAndCharacteristics();
          setDevice(conn);
          setConnected(true);
          subscribeToSensors(conn);

          // Handle disconnection
          conn.onDisconnected(() => {
            setConnected(false);
            setDevice(null);
          });
        } catch (e) {
          console.error("Connection error:", e);
        }
      }
    });

    // Stop scanning after 10 seconds
    setTimeout(() => {
      manager.stopDeviceScan();
      setScanning(false);
    }, 10000);
  }, []);

  // ── Subscribe to sensor notifications ──
  function subscribeToSensors(dev: Device) {
    // Distance from ultrasonic sensor
    dev.monitorCharacteristicForService(
      SERVICE_UUID, DISTANCE_CHAR,
      (error, char) => {
        if (error || !char?.value) return;
        const distance = parseInt(atob(char.value));
        const crowdLevel = distance < 50 ? "High" : distance < 100 ? "Medium" : "Low";

        setSensorData(prev => ({ ...prev, distance, crowdLevel }));

        // Speak alerts
        if (distance < 50) {
          Speech.speak(`Warning! Object ${distance} centimeters away`, { rate: 1.1 });
        } else if (distance < 100) {
          Speech.speak("Crowd detected nearby", { rate: 1.0 });
        }
      }
    );

    // IR sensor
    dev.monitorCharacteristicForService(
      SERVICE_UUID, IR_CHAR,
      (error, char) => {
        if (error || !char?.value) return;
        const irDetected = atob(char.value) === "1";
        setSensorData(prev => ({ ...prev, irDetected }));
        if (irDetected) Speech.speak("Motion detected", { rate: 1.0 });
      }
    );
  }

  // ── Send command to ESP32 (buzzer / motor) ──
  const sendCommand = useCallback(async (command: "buzz" | "vibrate" | "stop") => {
    if (!device || !connected) return;
    try {
      await device.writeCharacteristicWithResponseForService(
        SERVICE_UUID,
        CONTROL_CHAR,
        btoa(command)
      );
    } catch (e) {
      console.error("Command error:", e);
    }
  }, [device, connected]);

  const disconnect = useCallback(async () => {
    if (device) {
      await device.cancelConnection();
      setConnected(false);
      setDevice(null);
    }
  }, [device]);

  return {
    scanAndConnect,
    disconnect,
    sendCommand,
    connected,
    scanning,
    sensorData,
  };
}