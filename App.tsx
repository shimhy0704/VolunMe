import "react-native-gesture-handler";
import React from "react";
import { Platform } from "react-native";
import { NavigationContainer, DefaultTheme } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
// ✅ 여기 추가
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from "react-native-safe-area-context";

import { Home, Search, MapPin, User } from "lucide-react-native";

import HomeScreen from "./src/screens/HomeScreen";
import FeedScreen from "./src/screens/FeedScreen";
import MapScreen from "./src/screens/MapScreen";
import MyPageScreen from "./src/screens/MyPageScreen";

const Tab = createBottomTabNavigator();

const ORANGE = "#FF8A00";
const INACTIVE = "#C2C2C2";
const TAB_BAR_HEIGHT = 64; //하단 네비게이션 바 높이

function Tabs() {
  const theme = {
    ...DefaultTheme,
    colors: { ...DefaultTheme.colors, background: "#EAEAEA" },
  }; //배경

  const insets = useSafeAreaInsets();

  return (
    <NavigationContainer theme={theme}>
      <Tab.Navigator
        screenOptions={({ route }) => ({
          headerShown: false,
          tabBarShowLabel: true,
          tabBarActiveTintColor: ORANGE,
          tabBarInactiveTintColor: INACTIVE,
          tabBarLabelStyle: {
            fontSize: 11,
            marginBottom: Platform.OS === "ios" ? 0 : 3,
          },
          tabBarStyle: {
            position: "absolute",
            height: TAB_BAR_HEIGHT + insets.bottom,
            backgroundColor: "#fff", //하단 네비게이션 바
            borderTopWidth: 0,
            paddingTop: 6,
            paddingBottom: Math.max(insets.bottom, 6),
            borderTopLeftRadius: 16,
            borderTopRightRadius: 16,
            shadowColor: "#000",
            shadowOpacity: 0.06,
            shadowRadius: 8,
            shadowOffset: { width: 0, height: -2 },
            elevation: 12,
          },
          tabBarIcon: ({ color, size }) => {
            const s = size ?? 24;
            switch (route.name) {
              case "홈":
                return <Home size={s} color={color} />; //아이콘 바꿀때 home바꾸면 됨
              case "피드":
                return <Search size={s} color={color} />;
              case "지도":
                return <MapPin size={s} color={color} />;
              case "마이페이지":
                return <User size={s} color={color} />;
              default:
                return <Home size={s} color={color} />;
            }
          },
        })}
      >
        <Tab.Screen name="홈" component={HomeScreen} />
        <Tab.Screen name="피드" component={FeedScreen} />
        <Tab.Screen name="지도" component={MapScreen} />
        <Tab.Screen name="마이페이지" component={MyPageScreen} />
      </Tab.Navigator>
    </NavigationContainer>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <Tabs />
    </SafeAreaProvider>
  );
}
