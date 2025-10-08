// app/_layout.tsx
import { Stack } from "expo-router";

export default function RootLayout() {
  return (
    <Stack>
      {/* 탭 네비게이션 묶음 */}
      <Stack.Screen name="(tabs)" options={{ headerShown: false }} />

      {/* 글쓰기: 탭이 아니라 모달/스택 화면으로 */}
      <Stack.Screen
        name="post/compose"
        options={{ presentation: "modal", title: "작성" }}
      />
    </Stack>
  );
}
