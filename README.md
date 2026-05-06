# 🧠 Synapse

Synapse is an assistive wearable system designed to support individuals with schizophrenia and PTSD by improving environmental awareness and providing calming, real-time feedback. It combines a sensor-based wearable with a mobile application and Bluetooth audio integration to deliver grounding and reassurance during stressful or disorienting situations.

---

## 💙 Purpose

Individuals with schizophrenia or PTSD may experience sensory overload, anxiety, or difficulty interpreting their surroundings. Synapse aims to:

- Provide **real-world environmental feedback**
- Offer **grounding stimuli through vibration and sound**
- Reduce anxiety using **calming audio cues**
- Improve **confidence and independence in daily life**

> ⚠️ **Disclaimer:** Synapse is not a medical device and does not replace professional medical care. It is designed as a supportive assistive tool.

---

## 🚀 Key Features

- 🔍 Obstacle detection using IR / Ultrasonic sensors  
- 🧭 Motion tracking with IMU  
- ❤️ Heart rate monitoring (PPG sensor)  
- 📳 Haptic feedback via vibration motor  
- 🎧 **Bluetooth audio support for calming music / guided sounds**  
- 📱 **Mobile app integration for control and customization**  
- 📡 Wireless connectivity (ESP32 Bluetooth + WiFi)

---

## 📱 Mobile App Integration

The Synapse mobile application allows users to:

- Connect the wearable via Bluetooth  
- Stream **calming music or grounding audio** to any Bluetooth earphones/headphones  
- Customize alert intensity (vibration/sound)  
- Monitor basic sensor feedback  
- Trigger manual calming modes when needed  

---

## 🎧 Calming System

Synapse provides a dual-feedback calming mechanism:

- **Vibration Pulses** → rhythmic haptic feedback to ground the user  
- **Audio Playback** → soothing music or guided sounds via Bluetooth ear devices  

This combination helps:
- Reduce panic episodes  
- Improve focus during sensory overload  
- Provide reassurance in uncertain environments  

---

## 🧰 Hardware Components

- ESP32 Development Board  
- PPG Sensor (Heart Rate Monitoring)  
- IMU Sensor (e.g., MPU6050)  
- IR Sensor / Ultrasonic Sensor  
- Haptic Vibration Motor  
- Buzzer / Speaker  
- Li-ion Battery (18650)  
- Charging Module (TP4056)  
- Supporting electronics (resistors, wiring, PCB)

---

## ⚙️ Working Principle

1. Sensors collect real-time environmental and physiological data  
2. ESP32 processes inputs and detects events (obstacles, stress indicators, motion)  
3. System responds by:
   - Triggering vibration alerts  
   - Sending signals to the mobile app  
4. App activates:
   - Calming audio playback via Bluetooth earphones  
   - Custom user-defined responses  

---

## 🔌 Circuit Overview

- Sensors connected to ESP32 GPIO pins  
- I2C for IMU 
- Digital pins for IR/ultrasonic sensor  
- Output pins for vibration motor & buzzer  
- Bluetooth communication with mobile app  
- Battery-powered via charging module  

*(Detailed circuit diagram to be added)*

---

## 💻 Software Requirements

- Arduino IDE / PlatformIO  
- ESP32 Board Package  
- Required Libraries:
  - Wire.h  
  - Sensor libraries (IMU/PPG)  
  - BluetoothSerial.h / BLE libraries  

---

## 🛠️ Setup Instructions

1. Flash code to ESP32  
2. Assemble hardware components  
3. Install and open Synapse mobile app  
4. Pair ESP32 with the app via Bluetooth  
5. Connect Bluetooth earphones  
6. Start monitoring and assistance features  

---

## 📈 Future Improvements

- AI-based stress detection & prediction  
- Personalized audio therapy profiles  
- Caregiver alert system  
- Cloud data logging & analytics  
- Miniaturized wearable design  

---

## 🤝 Contributing

Open to contributions and improvements.
>>>>>>> 94f7f85b0f6a71bd70f0b19885d2675040c6bc2c
