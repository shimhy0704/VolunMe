// src/screens/HomeScreen.tsx
import React, { useMemo } from "react";
import {
  Image,
  StyleSheet,
  Text,
  View,
  Dimensions,
  Pressable,
  SafeAreaView,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Bell } from "lucide-react-native";

const ORANGE = "#FF8A00";
const BG = "#EAEAEA";
const BAR_BG = "#D9D9D9";
const TEXT_DARK = "#C2C2C2";

const { width: SCREEN_W } = Dimensions.get("window"); //기기의 화면 너비에 맞게 조정

// 🔹 로컬 이미지 경로
const BEAR = require("../../assets/images/bear.png");
const BEAR_HEAD = require("../../assets/images/bearhead.png");

type LevelHeaderProps = {
  level: number; // e.g., 18
  curExp: number; // e.g., 25
  maxExp: number; // e.g., 35
};

function LevelHeader({ level, curExp, maxExp }: LevelHeaderProps) {
  const progress = Math.min(1, Math.max(0, curExp / maxExp));
  const barW = SCREEN_W - 32 - 48 - 16; // 좌우 패딩 + 곰머리 여백 고려, 오른쪽 여백도 고려
  const fillW = Math.max(8, Math.round(barW * progress));

  return (
    <View style={styles.levelWrap}>
      {/* Lv 라벨 */}
      <Text style={styles.levelText}>{`Lv.${level}`}</Text>

      {/* 진행 바 */}
      <View style={styles.barRow}>
        {/* 곰머리 아이콘 */}
        <Image source={BEAR_HEAD} style={styles.bearHead} />

        <View style={[styles.barBg, { width: barW }]}>
          <View style={[styles.barFill, { width: fillW }]} />
        </View>
      </View>

      {/* 경험치 수치 */}
      <Text style={styles.expText}>{`${curExp}/${maxExp}`}</Text>
    </View>
  );
}

export default function HomeScreen() {
  const insets = useSafeAreaInsets();

  // 필요 시 서버/스토어 값으로 교체
  const level = 18;
  const curExp = 25;
  const maxExp = 35;

  // 곰 이미지는 화면 너비에 비례해 적당한 사이즈로
  const bearStyle = useMemo(() => {
    const w = Math.min(360, SCREEN_W * 0.75);
    const h = w * (1125 / 750); // 제공된 비율에 맞춰 대략 보정
    return { width: w, height: h };
  }, []);

  return (
    <SafeAreaView style={[styles.safe, { paddingTop: insets.top }]}>
      <View style={styles.container}>
        {/* 상단 레벨/경험치 */}
        <LevelHeader level={level} curExp={curExp} maxExp={maxExp} />

        {/* 중앙 곰 캐릭터 */}
        <View style={styles.bearBox}>
          <Image
            source={BEAR}
            style={[styles.bear, bearStyle]}
            resizeMode="contain"
          />
        </View>

        {/* 우하단 종 버튼 (탭바 위로 뜨게) */}
        <Pressable
          onPress={() => {
            // TODO: 알림 화면으로 이동 or 모달 오픈
            console.log("bell pressed");
          }}
          style={({ pressed }) => [
            styles.fab,
            {
              bottom: 150,
              opacity: pressed ? 0.85 : 1,
            },
          ]}
        >
          <Bell size={30} color="#fff" />
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: BG,
  },
  container: {
    flex: 1,
    backgroundColor: BG,
    paddingHorizontal: 16,
  },
  /* ----- Level Header ----- */
  levelWrap: {
    marginTop: 160, // exp bar 위치 조정
    marginBottom: -90, // 곰 캐릭터와 간격
  },
  levelText: {
    color: ORANGE,
    fontSize: 17,
    fontWeight: "600",
    marginLeft: 20, // 곰머리 폭 감안
    marginBottom: -7,
  },
  barRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  bearHead: {
    width: 80,
    height: 80,
    marginRight: -15, // 바와 딱 붙게
    zIndex: 2,
  },
  barBg: {
    height: 40,
    backgroundColor: BAR_BG,
    borderRadius: 12,
    overflow: "hidden",
    justifyContent: "center",
  },
  barFill: {
    height: 40,
    backgroundColor: ORANGE,
    borderRadius: 12,
  },
  expText: {
    color: ORANGE,
    fontSize: 17,
    fontWeight: "600",
    marginLeft: 18,
    marginTop: -7,
  },

  /* ----- Bear Image ----- */
  bearBox: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  bear: {
    // 그림자 느낌 (iOS)
    shadowColor: "#000",
    shadowOpacity: 0.15,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    // Android elevation
    elevation: 6,
  },

  /* ----- Floating Bell ----- */
  fab: {
    position: "absolute",
    right: 16,
    width: 66,
    height: 66,
    borderRadius: 33,
    backgroundColor: ORANGE,
    alignItems: "center",
    justifyContent: "center",
    shadowColor: "#000",
    shadowOpacity: 0.2,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 4 },
    elevation: 10,
  },
});
