package com.example.logindemo.mypage;

import com.example.logindemo.member.Member;
import com.example.logindemo.member.MemberRepository;
import com.example.logindemo.post.Post;
import com.example.logindemo.post.PostMedia;
import com.example.logindemo.post.PostRepository;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MyPageController {

    /** 기본 프로필(카카오/없는 경우) */
    private static final String DEFAULT_AVATAR = "/img/avatar-default.png";
    /** 곰돌이 아바타(토글 시 구분되도록 별도 이미지) */
    private static final String BEAR_AVATAR = "/img/bear.png";

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    @PersistenceContext
    private EntityManager em;

    /* =========================================================
     * 내 마이페이지: 후기글만 + 미디어(JSON) 제공
     * ========================================================= */
    @GetMapping("/mypage")
    public String myPage(HttpSession session, Model model) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) return "redirect:/login";

        // 후기글: 작성자 ID로 조회 (recruitId IS NOT NULL 조건은 Repository JPQL에 있음)
        List<Post> myReviews = postRepository.findReviewsByAuthorId(me.getId());
        model.addAttribute("myReviews", myReviews);
        model.addAttribute("mediaMap", buildMediaMapByBulkQueryAsJson(myReviews));

        // 모집글
        model.addAttribute("myRecruits", findRecruitsByHost(me.getId()));

        // 상단 카운트
        model.addAttribute("reviewCount", postRepository.countReviewsByAuthorId(me.getId()));
        model.addAttribute("recruitCount", countRecruits(me.getId()));

        // 프로필/상태
        String status = getStatusMessage(me.getId());
        String profileUrl = resolveProfileUrl(me);
        bindModel(model, me, true, status, profileUrl);

        return "mypage";
    }



    /** 후기(Post) 수 세기: author 기준 */
    private long countReviews(Long userId) {
        var author = memberRepository.findById(userId).orElse(null);
        if (author == null) return 0L;
        // 이미 있는 메서드: findReviewsByAuthor(Member)
        // (후기만 조회하는 리포지토리라면 이것의 size로 충분)
        return postRepository.findReviewsByAuthor(author).size();
    }

    /** 모집글 수 세기: 호스트 기준 + type='RECRUIT' */
    private long countRecruits(Long userId) {
        Object v = em.createNativeQuery(
                "select count(*) from volunteer_posts " +
                        "where host_user_id = ? and status in ('open','closed')"  // 필요 시
        ).setParameter(1, userId).getSingleResult();

        if (v instanceof java.math.BigInteger bi) return bi.longValue();
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }


    /* =========================================================
     * 다른 사람 프로필: 후기글만 + 미디어(JSON) 제공
     * ========================================================= */
    @GetMapping("/users/{userId}/mypage")
    public String othersPage(@PathVariable Long userId, HttpSession session, Model model) {
        Member target = memberRepository.findById(userId).orElse(null);
        if (target == null) return "redirect:/mypage";

        Member me = (Member) session.getAttribute("loggedInMember");
        boolean isOwner = (me != null && me.getId().equals(target.getId()));

        // 후기글: 대상 사용자 ID로 조회
        List<Post> myReviews = postRepository.findReviewsByAuthorId(userId);
        model.addAttribute("myReviews", myReviews);
        model.addAttribute("mediaMap", buildMediaMapByBulkQueryAsJson(myReviews));

        // 모집글
        model.addAttribute("myRecruits", findRecruitsByHost(userId));

        // 상단 카운트
        model.addAttribute("reviewCount", postRepository.countReviewsByAuthorId(userId));
        model.addAttribute("recruitCount", countRecruits(userId));

        // 프로필/상태
        String status = getStatusMessage(userId);
        String profileUrl = resolveProfileUrl(target);
        bindModel(model, target, isOwner, status, profileUrl);

        return "mypage";
    }






    // ──────────────────────────────────────────────────────────
    // 첨부 일괄 조회 → mediaMap(postId -> JSON 문자열 "[{url,type},...]") 생성
    private Map<Long, String> buildMediaMapByBulkQueryAsJson(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return Collections.emptyMap();

        List<Long> postIds = posts.stream().map(Post::getId).toList();

        List<PostMedia> medias = em.createQuery(
                        "select m from PostMedia m " +
                                "join fetch m.post " +
                                "join fetch m.media " +
                                "where m.post.id in :ids " +
                                "order by m.post.id asc, m.sortOrder asc", PostMedia.class)
                .setParameter("ids", postIds)
                .getResultList();

        // postId -> List<MediaItem>
        Map<Long, List<MediaItem>> grouped = new LinkedHashMap<>();

        for (PostMedia pm : medias) {
            if (pm == null || pm.getPost() == null) continue;
            Long pid = pm.getPost().getId();
            if (pid == null) continue;

            String path   = (pm.getMedia() != null) ? pm.getMedia().getPath()   : null;
            String bucket = (pm.getMedia() != null) ? pm.getMedia().getBucket() : null;
            String mime   = (pm.getMedia() != null) ? pm.getMedia().getMime()   : null;
            Long   mid    = (pm.getMedia() != null) ? pm.getMedia().getId()     : null;

            if (path == null || path.isBlank()) continue;
            // ✅ 이미지/영상만 노출
            if (mime != null && !(mime.startsWith("image/") || mime.startsWith("video/"))) continue;

            // 경로 정규화
            path = path.replace('\\', '/').trim();
            try {
                String[] parts = path.split("\\?", 2);
                String base = parts[0];
                String qs   = (parts.length > 1) ? ("?" + parts[1]) : "";
                String encoded = Arrays.stream(base.split("/"))
                        .filter(seg -> seg != null && !seg.isEmpty())
                        .map(seg -> java.net.URLEncoder.encode(seg, java.nio.charset.StandardCharsets.UTF_8))
                        .collect(java.util.stream.Collectors.joining("/"));
                path = (base.startsWith("/") ? "/" : "") + encoded + qs;
            } catch (Exception ignore) { /* 인코딩 실패 시 원본 사용 */ }

            String url;
            if (mid != null) {
                // 앱 라우트
                url = "/media/" + mid + "/raw";
            } else if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
                url = path;
            } else if (bucket != null && !bucket.isBlank() && !"local".equalsIgnoreCase(bucket)) {
                String b = bucket.trim();
                if (!(b.startsWith("http://") || b.startsWith("https://"))) {
                    b = "https://" + b;
                }
                url = b.endsWith("/") ? (b + (path.startsWith("/") ? path.substring(1) : path))
                        : (b + "/" + (path.startsWith("/") ? path.substring(1) : path));
            } else {
                url = path.startsWith("/") ? path : ("/" + path);
            }

            String type = (mime == null) ? guessTypeByPath(url) : (mime.startsWith("video/") ? "video" : "image");
            grouped.computeIfAbsent(pid, k -> new ArrayList<>()).add(new MediaItem(url, type));
        }

        // JSON 문자열로 직렬화 (간단 수동 직렬화)
        Map<Long, String> jsonMap = new LinkedHashMap<>();
        for (Map.Entry<Long, List<MediaItem>> e : grouped.entrySet()) {
            String json = toJsonArray(e.getValue());
            jsonMap.put(e.getKey(), json);
        }
        // 값이 비어 있는 postId도 빈 배열로 제공 (렌더러 단순화)
        for (Post p : posts) {
            jsonMap.putIfAbsent(p.getId(), "[]");
        }
        return jsonMap;
    }

    // 간단한 목록 표현용 DTO
    public record RecruitListItem(
            Long id, String title, String address,
            java.time.LocalDateTime startTime, Integer capacity, String status, String description   // ✨ 추가
    ) {}

    @SuppressWarnings("unchecked")
    private java.util.List<RecruitListItem> findRecruitsByHost(Long userId) {
        var rows = em.createNativeQuery("""
        select id, title, address, start_time, capacity, status, description
        from volunteer_posts
        where host_user_id = ?
        order by created_at desc
    """)
                .setParameter(1, userId)
                .getResultList();

        var out = new java.util.ArrayList<RecruitListItem>();
        for (Object row : rows) {
            Object[] a = (Object[]) row;

            Long id = (a[0] == null) ? null : ((Number) a[0]).longValue();
            String title = (String) a[1];
            String address = (String) a[2];

            // start_time: 드라이버 설정에 따라 Timestamp 또는 LocalDateTime으로 올 수 있음
            java.time.LocalDateTime start = null;
            if (a[3] instanceof java.sql.Timestamp ts) {
                start = ts.toLocalDateTime();
            } else if (a[3] instanceof java.time.LocalDateTime ldt) {
                start = ldt;
            }

            Integer cap = (a[4] == null) ? null : ((Number) a[4]).intValue();
            String status = (String) a[5];

            String description = (String) a[6]; // ✨ 추가된 컬럼 매핑

            out.add(new RecruitListItem(id, title, address, start, cap, status, description));
        }
        return out;
    }



    private static String guessTypeByPath(String url) {
        String u = (url == null) ? "" : url.toLowerCase(Locale.ROOT);
        if (u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".mov") || u.endsWith(".m4v")) return "video";
        return "image";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String toJsonArray(List<MediaItem> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            MediaItem m = list.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"url\":\"").append(escapeJson(m.url)).append("\",\"type\":\"").append(escapeJson(m.type)).append("\"}");
        }
        sb.append(']');
        return sb.toString();
    }

    private record MediaItem(String url, String type) {}

    private String normalizeUrl(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("/")) return t;
        return "/" + t;
    }

    // ──────────────────────────────────────────────────────────
    // 상태메세지 읽기 helper (DB → 문자열)
    private String getStatusMessage(Long userId) {
        try {
            Optional<String> byRepo = memberRepository.findStatusMessageByMemberId(userId);
            if (byRepo.isPresent() && hasText(byRepo.get())) {
                return byRepo.get();
            }
        } catch (Exception e) {
            log.warn("Failed to load status message via repository for userId={}", userId, e);
        }

        // Fallback: users JOIN 기반
        try {
            String email = memberRepository.findById(userId).map(Member::getUsername).orElse(null);
            if (email != null) {
                List<?> list = em.createNativeQuery(
                        "SELECT up.bio " +
                                "FROM user_profiles up " +
                                "JOIN users u ON u.id = up.user_id " +
                                "WHERE u.email = ? LIMIT 1"
                ).setParameter(1, email).getResultList();

                if (!list.isEmpty()) {
                    Object v = list.get(0);
                    return v != null ? String.valueOf(v) : "";
                }
            }
        } catch (Exception e) {
            log.warn("Fallback load status message failed for userId={}", userId, e);
        }

        return "";
    }

    /**
     * 모델 바인딩: resolveProfileUrl 재호출 없이 전달받은 profileUrl 사용
     */
    private void bindModel(Model model, Member user, boolean isOwner, String statusMessage, String profileUrl) {
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("userId", user.getId());
        model.addAttribute("nickname", user.getDisplayName());
        model.addAttribute("email", user.getUsername()); // username=이메일
        model.addAttribute("statusMessage", statusMessage);

        // 템플릿 호환 위해 두 키 모두 주입
        model.addAttribute("kakaoProfileUrl", profileUrl);
        model.addAttribute("profileImageUrl", profileUrl);

        // 곰돌이 아바타는 기본 프로필과 달라야 토글 체감 가능
        model.addAttribute("bearAvatarUrl", BEAR_AVATAR);
    }

    /**
     * 프로필 URL을 안전하게 결정 (예외/NULL 모두 기본 이미지로 폴백)
     */



    private String resolveProfileUrl(Member user) {
        // 1) users 테이블(이메일 기반)
        try {
            String email = user.getUsername();
            Optional<String> byEmail = memberRepository.findUsersProfileImageUrlByEmail(email);
            if (byEmail.isPresent() && hasText(byEmail.get())) {
                String url = normalizeUrl(byEmail.get());
                log.debug("[ProfileURL] byEmail username={} -> {}", email, url);
                return url;
            }
        } catch (Exception e) {
            log.warn("[ProfileURL] byEmail lookup failed for username={}", user.getUsername(), e);
        }

        // 2) memberId 직접 매핑
        try {
            Optional<String> byMemberId = memberRepository.findUsersProfileImageUrlByMemberId(user.getId());
            if (byMemberId.isPresent() && hasText(byMemberId.get())) {
                String url = normalizeUrl(byMemberId.get());
                log.debug("[ProfileURL] byMemberId memberId={} -> {}", user.getId(), url);
                return url;
            }
        } catch (Exception e) {
            log.warn("[ProfileURL] byMemberId lookup failed for memberId={}", user.getId(), e);
        }

        // 3) media 테이블 최신 프로필
        try {
            Optional<String> fromMedia = memberRepository.findLatestProfileMediaUrlByMemberId(user.getId());
            if (fromMedia.isPresent() && hasText(fromMedia.get())) {
                String url = normalizeUrl(fromMedia.get());
                log.debug("[ProfileURL] fromMedia memberId={} -> {}", user.getId(), url);
                return url;
            }
        } catch (Exception e) {
            log.warn("[ProfileURL] fromMedia lookup failed for memberId={} (fallback to default)", user.getId(), e);
        }

        log.debug("[ProfileURL] fallback DEFAULT -> {}", DEFAULT_AVATAR);
        return DEFAULT_AVATAR;
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // ──────────────────────────────────────────────────────────
    // 상태메세지 저장 API (세션 인증 + 부분수정 PATCH)
    @PatchMapping(
            value = "/api/mypage/{userId}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    @Transactional
    public ResponseEntity<ProfileResponse> updateStatusMessage(
            @PathVariable Long userId,
            @RequestBody UpdateStatusRequest req,
            HttpSession session
    ) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) {
            return ResponseEntity.status(401).body(ProfileResponse.error("로그인이 필요합니다."));
        }
        if (!me.getId().equals(userId)) {
            return ResponseEntity.status(403).body(ProfileResponse.error("본인만 수정할 수 있습니다."));
        }

        // 검증: 0~150자, 심플 XSS 제거
        String v = (req.getStatusMessage() == null) ? "" : req.getStatusMessage().trim();
        if (v.length() > 150) {
            return ResponseEntity.badRequest()
                    .body(ProfileResponse.error("상태메시지는 150자 이하여야 합니다."));
        }
        v = v.replaceAll("[<>]", "");

        // users.id를 FK로 쓰도록 보장하고 user_profiles upsert
        Long usersId = ensureUsersIdForEmail(me.getUsername(), /*profileUrl*/ null);
        if (usersId == null) {
            return ResponseEntity.internalServerError()
                    .body(ProfileResponse.error("users 매핑 실패"));
        }
        upsertUserProfileByUsersId(usersId, v);

        // 세션에도 반영(있다면)
        me.setStatusMessage(v);

        String nickname = Optional.ofNullable(me.getDisplayName()).orElse("");
        return ResponseEntity.ok(ProfileResponse.ok(userId, nickname, v));
    }

    // ===== 최소 파일화를 위한 내부 DTO =====
    @lombok.Getter @lombok.Setter
    public static class UpdateStatusRequest {
        private String statusMessage;
    }

    @lombok.Getter @lombok.Builder
    public static class ProfileResponse {
        private Long userId;
        private String nickname;
        private String statusMessage;
        private String message; // 오류 시 메시지 (정상이면 null)

        public static ProfileResponse ok(Long userId, String nickname, String statusMessage) {
            return ProfileResponse.builder()
                    .userId(userId)
                    .nickname(nickname)
                    .statusMessage(statusMessage)
                    .build();
        }
        public static ProfileResponse error(String msg) {
            return ProfileResponse.builder()
                    .message(msg)
                    .build();
        }
    }

    // ──────────────────────────────────────────────────────────
    // ▼▼▼ users/user_profiles FK 문제 해결 유틸 ▼▼▼

    @Transactional
    protected Long ensureUsersIdForEmail(String email, String profileUrl) {
        if (!hasText(email)) return null;

        // 1) 조회
        Long found = selectUsersIdByEmail(email);
        if (found != null) return found;

        // 2) 없으면 INSERT 시도
        try {
            em.createNativeQuery("INSERT INTO users (email) VALUES (?)")
                    .setParameter(1, email)
                    .executeUpdate();
        } catch (Exception e1) {
            log.info("INSERT users(email) only failed, retry with timestamps. cause={}", e1.getMessage());
            try {
                em.createNativeQuery("INSERT INTO users (email, created_at, updated_at) VALUES (?, NOW(), NOW())")
                        .setParameter(1, email)
                        .executeUpdate();
            } catch (Exception e2) {
                log.info("INSERT users(email, created_at, updated_at) failed, retry with profile_image_url. cause={}", e2.getMessage());
                try {
                    em.createNativeQuery("INSERT INTO users (email, profile_image_url, created_at, updated_at) VALUES (?, ?, NOW(), NOW())")
                            .setParameter(1, email)
                            .setParameter(2, profileUrl)
                            .executeUpdate();
                } catch (Exception e3) {
                    log.error("All INSERT attempts for users failed. email={}", email, e3);
                    return null;
                }
            }
        }

        // 3) 재조회
        return selectUsersIdByEmail(email);
    }

    private Long selectUsersIdByEmail(String email) {
        List<?> rs = em.createNativeQuery("SELECT id FROM users WHERE email = ? LIMIT 1")
                .setParameter(1, email)
                .getResultList();
        if (rs.isEmpty()) return null;
        Object v = rs.get(0);
        if (v == null) return null;
        if (v instanceof BigInteger bi) return bi.longValue();
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(v));
    }

    @Transactional
    protected void upsertUserProfileByUsersId(Long usersId, String bio) {
        if (usersId == null) return;

        int updated = 0;
        try {
            updated = em.createNativeQuery(
                            "UPDATE user_profiles SET bio = ?, updated_at = NOW() WHERE user_id = ?"
                    ).setParameter(1, bio)
                    .setParameter(2, usersId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("UPDATE user_profiles failed (will try INSERT). usersId={}", usersId, e);
        }

        if (updated == 0) {
            try {
                em.createNativeQuery(
                                "INSERT INTO user_profiles (user_id, bio, created_at, updated_at) VALUES (?, ?, NOW(), NOW())"
                        ).setParameter(1, usersId)
                        .setParameter(2, bio)
                        .executeUpdate();
            } catch (Exception e1) {
                // created_at/updated_at 없는 스키마 대응
                try {
                    em.createNativeQuery(
                                    "INSERT INTO user_profiles (user_id, bio) VALUES (?, ?)"
                            ).setParameter(1, usersId)
                            .setParameter(2, bio)
                            .executeUpdate();
                } catch (Exception e2) {
                    log.error("INSERT user_profiles failed. usersId={}", usersId, e2);
                }
            }
        }
    }
}
