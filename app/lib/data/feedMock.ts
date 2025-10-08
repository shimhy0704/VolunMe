// app/lib/data/feedMock.ts
export type FeedType = "recruit" | "review";

export type FeedComment = {
  id: string;
  author: string;      // "나" 또는 다른 사람 이름
  text: string;
  date: string;
  replyTo?: string;    // 답글 대상 작성자 이름
};

export type FeedPost = {
  id: string;
  type: FeedType;      // ✅ "recruit" | "review"
  author: string;
  avatarUrl?: string;
  title: string;
  desc: string;
  date: string;               // 게시글 작성일 (이미지 아래 왼쪽)
  // recruit 전용 필드
  location?: string;
  categories?: string[];
  capacity?: number;
  joined?: number;
  volunteerDate?: string;
  // 공통 미디어
  imageUrl?: string;
  likes: number;
  liked?: boolean;
  comments: FeedComment[];
};

export const MOCK_POSTS: FeedPost[] = [
  {
    id: "1",
    type: "recruit",
    author: "서현",
    avatarUrl:
      "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=256&auto=format&fit=crop&crop=faces",
    title: "길현절 청소부 구합니다",
    desc:
      "지원자격: 대학생이면 누구나 가능 (휴학생 불가)\n모집기간: 8월 28일~9월 30일\n봉사내용: 행사장 내 정리, 쓰레기 분리수거 등입니다. 좋은 추억 만들어가요!",
    date: "2025.10.07",
    location: "서울특별시 노원구 서현동 68-3",
    categories: ["돌봄", "청소"],
    capacity: 9,
    joined: 2,
    volunteerDate: "10월 8일 06:00",
    imageUrl:
      "https://images.unsplash.com/photo-1482192596544-9eb780fc7f66?q=80&w=1400&auto=format&fit=crop",
    likes: 16,
    liked: false,
    comments: [
      { id: "c1", author: "민지", text: "시간대가 어떻게 되나요?", date: "2025-10-07" },
      { id: "c2", author: "현수", text: "친구랑 둘이 신청해도 되나요?", date: "2025-10-07" },
    ],
  },
  {
    id: "2",
    type: "review", // ✅ 후기
    author: "호응",
    avatarUrl:
      "https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?w=256&auto=format&fit=crop&crop=faces",
    title: "최서현 코딱지 구합니다.", // 예시 텍스트 그대로 사용
    desc:
      "아이들을 엄청 좋아하는데 아이들과 하루종일 시간을 보낼 수 있어서 감사한 하루였습니다. 다음에도 또 참여하고 싶네요!",
    date: "2025.10.07",
    imageUrl:
      "https://images.unsplash.com/photo-1519681393784-d120267933ba?q=80&w=1400&auto=format&fit=crop",
    likes: 8,
    liked: true,
    comments: [
      { id: "c3", author: "유진", text: "후기 잘 봤어요! 다음에 함께해요 🙌", date: "2025-10-07" },
    ],
  },
  {
    id: "3",
    type: "recruit",
    author: "동호회연합",
    avatarUrl:
      "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=256&auto=format&fit=crop&crop=faces",
    title: "공원 환경정화 봉사 함께 하실 분",
    desc:
      "간단한 분리수거와 쓰레기 줍기 활동입니다.\n장갑과 집게는 제공됩니다.",
    date: "2025.10.06",
    location: "서울특별시 광진구 능동로 190",
    categories: ["청소"],
    capacity: 20,
    joined: 11,
    volunteerDate: "10월 15일 09:30",
    imageUrl:
      "https://images.unsplash.com/photo-1482192596544-9eb780fc7f66?q=80&w=1400&auto=format&fit=crop",
    likes: 4,
    liked: false,
    comments: [],
  },
];
