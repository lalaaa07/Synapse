import { Ionicons } from "@expo/vector-icons";
import { Tabs } from "expo-router";
import { SensorProvider } from "../Context/SensorContext";

export default function RootLayout() {
  return (
    <SensorProvider>
      <Tabs
        screenOptions={{
          tabBarStyle: {
            backgroundColor: "#0a0015",
            borderTopColor: "#1e1b4b",
            borderTopWidth: 1,
          },
          tabBarActiveTintColor: "#818cf8",
          tabBarInactiveTintColor: "#2d2b55",
          headerStyle: { backgroundColor: "#0a0015" },
          headerTintColor: "#818cf8",
          headerShadowVisible: false,
        }}
      >
        <Tabs.Screen
          name="index"
          options={{
            title: "Connect",
            tabBarIcon: ({ color, size }) => (
              <Ionicons name="bluetooth" color={color} size={size} />
            ),
          }}
        />
        <Tabs.Screen
          name="dashboard"
          options={{
            title: "Dashboard",
            tabBarIcon: ({ color, size }) => (
              <Ionicons name="pulse" color={color} size={size} />
            ),
          }}
        />
        <Tabs.Screen
          name="alerts"
          options={{
            title: "Alerts",
            tabBarIcon: ({ color, size }) => (
              <Ionicons name="notifications" color={color} size={size} />
            ),
          }}
        />
        <Tabs.Screen
          name="emergency"
          options={{
            title: "SOS",
            tabBarIcon: ({ color, size }) => (
              <Ionicons name="warning" color={color} size={size} />
            ),
          }}
        />
      </Tabs>
    </SensorProvider>
  );
}