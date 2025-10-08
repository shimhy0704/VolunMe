// app/(tabs)/_layout.tsx
import { Tabs } from "expo-router";

export default function TabsLayout() {
  return (
    <Tabs screenOptions={{ headerShown: false }}>
      <Tabs.Screen name="home" options={{ title: "home" }} />
      <Tabs.Screen name="map" options={{ title: "map" }} />
      <Tabs.Screen name="feed" options={{ title: "feed" }} />
      <Tabs.Screen name="my" options={{ title: "my" }} />
      {/* 혹시 (tabs) 안에 다른 파일이 생기면, options={{ href: null }} 로 숨길 수 있음 */}
    </Tabs>
  );
}
