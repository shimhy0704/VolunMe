import { useBottomTabBarHeight } from "@react-navigation/bottom-tabs";
import { useLocalSearchParams, useRouter } from "expo-router";
import {
  Heart,
  MapPin,
  MessageCircle,
  PencilLine,
  Reply,
  Search as SearchIcon,
} from "lucide-react-native";
import React, { useEffect, useMemo, useState } from "react";
import {
  FlatList,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import {
  MOCK_POSTS,
  type FeedComment,
  type FeedPost,
} from "../../src/lib/data/feedMock";

const ORANGE = "#FF8A00";
const BG = "#EAEAEA";
const TEXT = "#333";
const SUBTEXT = "#666";
const BORDER = "#E5E5E5";
const CHIP_BG = "#EFEFEF";

const FONT_REGULAR = "Pretendard-Medium";
const FONT_BOLD = "Pretendard-Bold";

const TYPE_LABEL: Record<FeedPost["type"], string> = {
  recruit: "모집",
  review: "후기",
};

export default function FeedScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const tabBarHeight = useBottomTabBarHeight();

  // 🔗 map.tsx에서 넘어온 파라미터
  const { q, postId } = useLocalSearchParams<{
    q?: string;
    postId?: string;
  }>();

  const [posts, setPosts] = useState<FeedPost[]>(
    () => JSON.parse(JSON.stringify(MOCK_POSTS)) as FeedPost[]
  );

  // 검색어는 쿼리(q)로 초기화
  const [search, setSearch] = useState("");
  useEffect(() => {
    if (typeof q === "string" && q.trim()) setSearch(q);
  }, [q]);

  const [openCommentId, setOpenCommentId] = useState<string | null>(null);
  const [newComment, setNewComment] = useState("");
  const [replyTo, setReplyTo] = useState<FeedComment | null>(null);

  // 1) postId가 들어오면 해당 글만 강제 노출
  // 2) 아니면 기존 검색 로직 적용
  const filteredPosts = useMemo(() => {
    if (typeof postId === "string" && postId) {
      const only = posts.find((p) => p.id === postId);
      return only ? [only] : [];
    }
    const qText = search.trim().toLowerCase();
    if (!qText) return posts;
    return posts.filter((p) => {
      const haystack = [
        TYPE_LABEL[p.type],
        p.author,
        p.title,
        p.desc,
        p.location ?? "",
        p.date,
        p.volunteerDate ?? "",
        ...(p.categories ?? []),
        ...p.comments.flatMap((c) => [c.author, c.text]),
      ]
        .join(" ")
        .toLowerCase();
      return haystack.includes(qText);
    });
  }, [search, posts, postId]);

  const handleToggleLike = (id: string) => {
    setPosts((prev) =>
      prev.map((p) =>
        p.id === id
          ? {
              ...p,
              liked: !p.liked,
              likes: p.liked ? Math.max(0, p.likes - 1) : p.likes + 1,
            }
          : p
      )
    );
  };

  const handleAddComment = (postId_: string) => {
    if (!newComment.trim()) return;
    setPosts((prev) =>
      prev.map((p) => {
        if (p.id !== postId_) return p;
        const newC: FeedComment = {
          id: Math.random().toString(36).slice(2),
          author: "나",
          text: newComment.trim(),
          date: new Date().toISOString(),
          replyTo: replyTo?.author ?? undefined,
        };
        return { ...p, comments: [...p.comments, newC] };
      })
    );
    setNewComment("");
    setReplyTo(null);
  };

  const renderChip = (label: string) => (
    <View key={label} style={styles.chip}>
      <Text style={styles.chipText}>{label}</Text>
    </View>
  );

  const renderPost = ({ item }: { item: FeedPost }) => {
    const isCommentOpen = openCommentId === item.id;
    const isRecruit = item.type === "recruit";

    return (
      <View style={styles.postWrap}>
        {/* 작성자 + 제목 + 아바타 + 유형 배지(모집/후기) */}
        <View style={styles.headerRow}>
          <Image
            source={
              item.avatarUrl
                ? { uri: item.avatarUrl }
                : require("../../assets/images/bearhead.png")
            }
            style={styles.avatar}
          />
          <View style={{ flex: 1 }}>
            <View style={styles.nameRow}>
              <Text style={styles.author}>{item.author}</Text>
              <View style={styles.badge}>
                <Text style={styles.badgeText}>{TYPE_LABEL[item.type]}</Text>
              </View>
            </View>
            <Text style={styles.title}>{item.title}</Text>
          </View>
        </View>

        {/* 본문 */}
        <Text style={styles.desc}>{item.desc}</Text>

        {/* 모집 글일 때만 모집 요약 */}
        {isRecruit && (
          <View style={styles.summaryBox}>
            <View style={styles.summaryHeader}>
              <Text style={styles.summaryTitle}>모집 요약</Text>
              <View style={styles.badge}>
                <Text style={styles.badgeText}>모집</Text>
              </View>
            </View>

            <View
              style={[styles.summaryLine, { marginTop: 2, marginBottom: 6 }]}
            >
              <MapPin size={16} color={SUBTEXT} />
              <Text style={[styles.infoText, { marginLeft: 6 }]}>
                {item.location}
              </Text>
            </View>

            <View style={styles.chipsRow}>
              {renderChip(`정원: ${item.capacity}명`)}
              {renderChip(`현재: ${item.joined}명`)}
              {(item.categories ?? []).map((c) => renderChip(c))}
              {renderChip(`봉사 일시: ${item.volunteerDate}`)}
            </View>
          </View>
        )}

        {/* 이미지 */}
        {!!item.imageUrl && (
          <View style={styles.imageBox}>
            <Image
              source={{ uri: item.imageUrl }}
              style={styles.image}
              resizeMode="cover"
            />
          </View>
        )}
        {/* 날짜 — 이미지 아래 왼쪽 */}
        <Text style={styles.dateText}>{item.date}</Text>

        {/* 좋아요/댓글 */}
        <View style={styles.actionRow}>
          <Pressable
            onPress={() => handleToggleLike(item.id)}
            style={({ pressed }) => [
              styles.iconBtn,
              { opacity: pressed ? 0.7 : 1 },
            ]}
          >
            <Heart
              size={22}
              color={item.liked ? "#E53935" : SUBTEXT}
              fill={item.liked ? "#E53935" : "transparent"}
            />
            <Text style={styles.actionText}>{item.likes}</Text>
          </Pressable>

          <Pressable
            onPress={() => setOpenCommentId(isCommentOpen ? null : item.id)}
            style={({ pressed }) => [
              styles.iconBtn,
              { opacity: pressed ? 0.7 : 1 },
            ]}
          >
            <MessageCircle size={22} color={SUBTEXT} />
            <Text style={styles.actionText}>{item.comments.length}</Text>
          </Pressable>
        </View>

        {/* 댓글 */}
        {isCommentOpen && (
          <View style={styles.commentBox}>
            {(item.comments ?? []).map((c) => (
              <View key={c.id} style={styles.commentItem}>
                <View style={styles.commentAvatar} />
                <View style={{ flex: 1 }}>
                  <View
                    style={{
                      flexDirection: "row",
                      alignItems: "center",
                      flexWrap: "wrap",
                    }}
                  >
                    <Text style={styles.commentAuthor}>
                      {c.author}
                      {c.replyTo ? ` ↩︎ @${c.replyTo}` : ""}
                    </Text>
                    <Pressable
                      onPress={() => setReplyTo(c)}
                      style={({ pressed }) => [
                        { opacity: pressed ? 0.6 : 1, marginLeft: 6 },
                      ]}
                    >
                      <Reply size={16} color={SUBTEXT} />
                    </Pressable>
                  </View>
                  <Text style={styles.commentText}>{c.text}</Text>
                </View>
              </View>
            ))}
            {item.comments.length === 0 && (
              <Text style={styles.noComment}>아직 댓글이 없습니다.</Text>
            )}

            {/* 입력창 */}
            <KeyboardAvoidingView
              behavior={Platform.select({ ios: "padding", android: undefined })}
              keyboardVerticalOffset={80}
            >
              {replyTo && (
                <Text style={styles.replyHint}>
                  @{replyTo.author}에게 답글 작성 중…
                </Text>
              )}
              <View style={styles.inputRow}>
                <TextInput
                  placeholder="댓글을 입력하세요"
                  placeholderTextColor="#A1A1A1"
                  value={newComment}
                  onChangeText={setNewComment}
                  style={styles.input}
                />
                <Pressable
                  style={({ pressed }) => [
                    styles.sendBtn,
                    { opacity: pressed ? 0.8 : 1 },
                  ]}
                  onPress={() => handleAddComment(item.id)}
                >
                  <Text style={styles.sendText}>등록</Text>
                </Pressable>
              </View>
            </KeyboardAvoidingView>
          </View>
        )}

        {/* 하단 버튼들: 모집 글에만 표시 */}
        {isRecruit && (
          <View style={styles.bottomRow}>
            <Pressable
              style={({ pressed }) => [
                styles.btn,
                { backgroundColor: pressed ? "#f3f3f3" : "#fff" },
              ]}
              onPress={() => console.log("지도에서 보기 pressed")}
            >
              <Text style={styles.btnText}>지도에서 보기</Text>
            </Pressable>

            <Pressable
              style={({ pressed }) => [
                styles.btn,
                {
                  backgroundColor: pressed ? "#FFDDB8" : ORANGE,
                  borderColor: "transparent",
                },
              ]}
              onPress={() => console.log("지원하기 pressed")}
            >
              <Text style={[styles.btnText, { color: "#fff" }]}>지원하기</Text>
            </Pressable>
          </View>
        )}

        <View style={styles.separator} />
      </View>
    );
  };

  const listPaddingBottom = Math.max(12, tabBarHeight) + insets.bottom + 8;

  return (
    <View style={[styles.safe, { paddingTop: insets.top }]}>
      <StatusBar
        translucent
        backgroundColor="transparent"
        barStyle="dark-content"
      />

      {/* 🔍 검색창 (아이콘 + 입력창) */}
      <View style={styles.searchWrap}>
        <View style={styles.searchInputWrap}>
          <SearchIcon size={18} color={ORANGE} />
          <TextInput
            placeholder="검색어를 입력하세요"
            placeholderTextColor="#A1A1A1"
            value={search}
            onChangeText={setSearch}
            style={styles.searchInput}
          />
        </View>
      </View>

      <FlatList
        data={filteredPosts}
        keyExtractor={(it) => it.id}
        renderItem={renderPost}
        contentContainerStyle={{
          paddingHorizontal: 16,
          paddingBottom: listPaddingBottom,
        }}
        showsVerticalScrollIndicator={false}
      />

      {/* FAB */}
      <Pressable
        onPress={() => router.push("/post/compose")}
        style={({ pressed }) => [
          styles.fab,
          { bottom: tabBarHeight - 80, opacity: pressed ? 0.85 : 1 },
        ]}
      >
        <PencilLine size={30} color="#fff" />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: BG },

  // --- Search ---
  searchWrap: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 4 },
  searchInputWrap: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#fff",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: BORDER,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  searchInput: {
    flex: 1,
    fontFamily: FONT_REGULAR,
    fontSize: 15,
    paddingVertical: 0,
  },

  // --- Post ---
  postWrap: { paddingTop: 16 },
  headerRow: { flexDirection: "row", alignItems: "center", marginBottom: 8 },
  avatar: { width: 38, height: 38, borderRadius: 19, marginRight: 10 },
  nameRow: { flexDirection: "row", alignItems: "center", gap: 6 },
  author: { fontFamily: FONT_BOLD, fontSize: 14, color: TEXT },
  title: { fontFamily: FONT_BOLD, fontSize: 20, color: TEXT, marginTop: 2 },
  desc: {
    fontFamily: FONT_REGULAR,
    color: "#555",
    lineHeight: 20,
    marginTop: 8,
    marginBottom: 10,
  },

  // --- Badge ---
  badge: {
    backgroundColor: ORANGE,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 10,
  },
  badgeText: { color: "#fff", fontFamily: FONT_BOLD, fontSize: 12 },

  // --- Summary (모집만) ---
  summaryBox: { backgroundColor: "#F5F5F5", padding: 12, borderRadius: 14 },
  summaryHeader: { flexDirection: "row", alignItems: "center", gap: 8 },
  summaryTitle: { fontFamily: FONT_BOLD, color: "#111", fontSize: 16 },
  summaryLine: { flexDirection: "row", alignItems: "center" },
  infoText: { fontFamily: FONT_REGULAR, color: SUBTEXT, fontSize: 13 },

  chipsRow: { flexDirection: "row", flexWrap: "wrap", gap: 8, marginTop: 8 },
  chip: {
    backgroundColor: CHIP_BG,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 12,
  },
  chipText: { fontFamily: FONT_BOLD, color: "#555", fontSize: 12 },

  // --- Image & date ---
  imageBox: { marginTop: 12, borderRadius: 16, overflow: "hidden" },
  image: { width: "100%", height: 200 },
  dateText: {
    marginTop: 6,
    marginLeft: 2,
    fontFamily: FONT_REGULAR,
    fontSize: 12,
    color: "#888",
  },

  // --- Actions ---
  actionRow: {
    flexDirection: "row",
    gap: 18,
    paddingVertical: 10,
    alignItems: "center",
  },
  iconBtn: { flexDirection: "row", alignItems: "center" },
  actionText: {
    fontFamily: FONT_REGULAR,
    color: SUBTEXT,
    fontSize: 14,
    marginLeft: 6,
  },

  // --- Comments ---
  commentBox: { paddingTop: 4, paddingBottom: 8 },
  commentItem: { flexDirection: "row", gap: 10, paddingVertical: 8 },
  commentAvatar: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: "#EEE",
  },
  commentAuthor: {
    fontFamily: FONT_BOLD,
    color: "#222",
    marginBottom: 2,
    fontSize: 13,
  },
  commentText: {
    fontFamily: FONT_REGULAR,
    color: "#444",
    fontSize: 13,
    lineHeight: 18,
  },
  noComment: {
    fontFamily: FONT_REGULAR,
    color: "#888",
    textAlign: "center",
    paddingVertical: 14,
  },
  replyHint: {
    fontFamily: FONT_REGULAR,
    color: ORANGE,
    fontSize: 13,
    marginLeft: 8,
    marginBottom: 4,
  },
  inputRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    marginTop: 4,
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: BORDER,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontFamily: FONT_REGULAR,
    fontSize: 14,
    backgroundColor: "#fff",
  },
  sendBtn: {
    backgroundColor: ORANGE,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 10,
  },
  sendText: { color: "#fff", fontFamily: FONT_BOLD, fontSize: 14 },

  // --- Bottom buttons (모집만) ---
  bottomRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 6,
  },
  btn: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: BORDER,
    alignItems: "center",
    marginHorizontal: 4,
  },
  btnText: { fontFamily: FONT_BOLD, fontSize: 14 },

  separator: {
    height: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#00000010",
    marginTop: 12,
    marginBottom: 8,
  },

  // --- FAB ---
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
