// app/(tabs)/home.tsx
import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";
export default function Home() {
  return (
    <View style={s.wrap}>
      <Text>피드</Text>
      <Pressable style={s.fab} onPress={() => router.push("/post/compose")}>
        <Text style={{ fontWeight: "bold" }}>✎</Text>
      </Pressable>
    </View>
  );
}
const s = StyleSheet.create({
  wrap: { flex: 1, alignItems: "center", justifyContent: "center" },
  fab: {
    position: "absolute",
    right: 24,
    bottom: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: "#FFA64D",
    alignItems: "center",
    justifyContent: "center",
  },
});
