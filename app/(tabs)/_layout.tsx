// app/(tabs)/_layout.tsx
import { Tabs } from "expo-router";
import { House, MapPin, Search, User } from "lucide-react-native";
import React from "react";
import { Platform } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

const ACTIVE = "#FF8A00";
const INACTIVE = "#C4C4C4";

export default function TabsLayout() {
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      initialRouteName="home"
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: ACTIVE,
        tabBarInactiveTintColor: INACTIVE,
        tabBarShowLabel: true,
        tabBarLabelStyle: { fontSize: 12, marginTop: -2 },
        tabBarHideOnKeyboard: true,

        // ✅ safe-area 하단 여백 반영 (직접 계산)
        tabBarStyle: {
          position: "relative",
          height: 80 + insets.bottom / 2, // 아래 공간 확보
          paddingTop: 6,
          paddingBottom: Platform.OS === "ios" ? 8 + insets.bottom / 3 : 6,
          backgroundColor: "#fff",
          borderTopWidth: 0.5,
          borderTopColor: "rgba(0,0,0,0.08)",
          elevation: 0,
          shadowColor: "transparent",
        },
      }}
    >
      <Tabs.Screen
        name="home"
        options={{
          title: "홈",
          tabBarIcon: ({ color, size }) => <House size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="feed"
        options={{
          title: "피드",
          tabBarIcon: ({ color, size }) => <Search size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="map"
        options={{
          title: "지도",
          tabBarIcon: ({ color, size }) => <MapPin size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="my"
        options={{
          title: "마이페이지",
          tabBarIcon: ({ color, size }) => <User size={size} color={color} />,
        }}
      />
    </Tabs>
  );
}
