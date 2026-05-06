import { Tabs } from "expo-router";

export default function RootLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarStyle: { backgroundColor: "#0f0f0f", borderTopColor: "#222" },
        tabBarActiveTintColor: "#2563eb",
        tabBarInactiveTintColor: "#555",
        headerStyle: { backgroundColor: "#0f0f0f" },
        headerTintColor: "#fff",
      }}
    >
      <Tabs.Screen name="index" options={{ title: "Connect" }} />
      <Tabs.Screen name="dashboard" options={{ title: "Dashboard" }} />
      <Tabs.Screen name="alerts" options={{ title: "Alerts" }} />
    </Tabs>
  );
}