import { useRouter } from "expo-router"; // ✅ 추가
import { PencilLine } from "lucide-react-native";
import React, { useMemo } from "react";
import {
  Dimensions,
  Image,
  Pressable,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

const ORANGE = "#FF8A00";
const BG = "#EAEAEA";
const BAR_BG = "#D9D9D9";
const TEXT_DARK = "#C2C2C2";

const { width: SCREEN_W } = Dimensions.get("window");

// 🔹 로컬 이미지 경로
const BEAR = require("../../assets/images/bear.png");
const BEAR_HEAD = require("../../assets/images/bearhead.png");

type LevelHeaderProps = {
  level: number;
  curExp: number;
  maxExp: number;
};

function LevelHeader({ level, curExp, maxExp }: LevelHeaderProps) {
  const progress = Math.min(1, Math.max(0, curExp / maxExp));
  const barW = SCREEN_W - 32 - 48 - 16; // 좌우 패딩 + 곰머리 여백 고려
  const fillW = Math.max(8, Math.round(barW * progress));

  return (
    <View style={styles.levelWrap}>
      <Text style={styles.levelText}>{`Lv.${level}`}</Text>

      <View style={styles.barRow}>
        <Image source={BEAR_HEAD} style={styles.bearHead} />

        <View style={[styles.barBg, { width: barW }]}>
          <View style={[styles.barFill, { width: fillW }]} />
        </View>
      </View>

      <Text style={styles.expText}>{`${curExp}/${maxExp}`}</Text>
    </View>
  );
}

export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  const router = useRouter(); // ✅ 추가

  // 필요 시 서버/스토어 값으로 교체
  const level = 18;
  const curExp = 25;
  const maxExp = 35;

  const bearStyle = useMemo(() => {
    const w = Math.min(360, SCREEN_W * 0.75);
    const h = w * (1125 / 750);
    return { width: w, height: h };
  }, []);

  return (
    <View
      style={[
        styles.safe,
        {
          paddingTop: insets.top,
          paddingBottom: Math.max(8, insets.bottom),
        },
      ]}
    >
      <StatusBar
        translucent
        backgroundColor="transparent"
        barStyle="dark-content"
      />

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

        {/* 우하단 연필 버튼 */}
        <Pressable
          onPress={() => router.push("/post/compose")} // ✅ 여기서 이동
          style={({ pressed }) => [
            styles.fab,
            {
              bottom: -73 + insets.bottom,
              opacity: pressed ? 0.85 : 1,
            },
          ]}
        >
          <PencilLine size={30} color="#fff" />
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: BG },
  container: { flex: 1, backgroundColor: BG, paddingHorizontal: 16 },

  /* ----- Level Header ----- */
  levelWrap: { marginTop: 160, marginBottom: -90 },
  levelText: {
    color: ORANGE,
    fontSize: 17,
    fontWeight: "600",
    marginLeft: 20,
    marginBottom: -7,
  },
  barRow: { flexDirection: "row", alignItems: "center" },
  bearHead: { width: 80, height: 80, marginRight: -19, zIndex: 2 },
  barBg: {
    height: 40,
    backgroundColor: BAR_BG,
    borderRadius: 12,
    overflow: "hidden",
    justifyContent: "center",
  },
  barFill: { height: 40, backgroundColor: ORANGE, borderRadius: 12 },
  expText: {
    color: ORANGE,
    fontSize: 17,
    fontWeight: "600",
    marginLeft: 18,
    marginTop: -7,
  },

  /* ----- Bear Image ----- */
  bearBox: { flex: 1, alignItems: "center", justifyContent: "center" },
  bear: {
    shadowColor: "#000",
    shadowOpacity: 0.15,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    elevation: 6,
    bottom: -80,
  },

  /* ----- Floating PencilLine ----- */
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
