package com.example.logindemo.volunteer.service;

import com.example.logindemo.volunteer.bridge.PostBridge;
import com.example.logindemo.volunteer.dto.MarkerBriefResponse;
import com.example.logindemo.volunteer.dto.VolunteerCreateRequest;
import com.example.logindemo.volunteer.dto.VolunteerDetailResponse;
import com.example.logindemo.volunteer.dto.ApplicantResponse;
import com.example.logindemo.volunteer.entity.Application;
import com.example.logindemo.volunteer.entity.ApplicationStatus;
import com.example.logindemo.volunteer.entity.VolunteerPost;
import com.example.logindemo.volunteer.entity.VolunteerStatus;
import com.example.logindemo.volunteer.repository.ApplicationRepository;
import com.example.logindemo.volunteer.repository.VolunteerPostRepository;
import com.example.logindemo.member.Member;
import com.example.logindemo.member.MemberRepository;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerPostRepository postRepo;
    private final ApplicationRepository appRepo;
    private final MemberRepository memberRepo;
    private final PostBridge postBridge;

    @Column(name = "is_review")
    private Boolean review = false;

    /** 컨트롤러에서 세션의 hostUserId를 주입해서 호출 */
    @Transactional
    public Long createRecruit(Long hostUserId,
                              VolunteerCreateRequest req,
                              MultipartFile[] files) {                // ★ files

        var now = java.time.LocalDateTime.now();
        boolean isReview = Boolean.TRUE.equals(req.getReview());

        if (!isReview && req.getStartTime() != null && req.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("시작 시간은 현재 시각 이후여야 합니다.");
        }

        var start = (req.getStartTime() != null) ? req.getStartTime() : now;

        // ★ 저장 후 변수명 saved 유지
        var saved = VolunteerPost.builder()
                .hostUserId(hostUserId)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(isReview ? "후기" : req.getCategory())
                .address(req.getAddress())
                .lat(req.getLat())
                .lng(req.getLng())
                .capacity(isReview ? null : req.getCapacity())
                .startTime(start)
                .status(isReview ? VolunteerStatus.closed : VolunteerStatus.open)
                .review(isReview)
                .build();
        saved = postRepo.save(saved);

        if (Boolean.TRUE.equals(req.getReview())) {
            // ★ 후기: 일반 게시글 + 첨부 연결
            postBridge.createReviewPost(hostUserId, saved.getTitle(), saved.getDescription(), files);
        } else {
            // 모집 공지 Post 생성 (기존 메서드 시그니처 그대로)
            postBridge.createRecruitAnnouncement(
                    hostUserId, saved.getId(), saved.getTitle(), saved.getDescription(), saved.getAddress());
            // (정원/마감 로직 기존대로 있으면 유지)
        }
        return saved.getId();
    }


    /* -------------------- 조회 -------------------- */

    /**
     * 지도 뷰포트/필터 기반 마커 조회 (BBox + 상태/카테고리/검색어)
     */
    @Transactional(readOnly = true)
    public List<MarkerBriefResponse> getMarkers(
            double minLat, double minLng, double maxLat, double maxLng,
            String q, String category, String status) {

        // 1) bbox 보정 (남/북, 서/동 순서 강제)
        double south = Math.min(minLat, maxLat);
        double north = Math.max(minLat, maxLat);
        double west  = Math.min(minLng, maxLng);
        double east  = Math.max(minLng, maxLng);

        // 2) 상태 파라미터: 대소문자 무시 + 잘못된 값이면 null 처리
        VolunteerStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = VolunteerStatus.valueOf(status.trim().toLowerCase()); // ← enum이 open/closed 등 소문자이므로 toLowerCase
            } catch (IllegalArgumentException ignore) {
                st = null; // 잘못된 값 들어오면 전체 검색
            }
        }

        // 3) 기타 파라미터 정리
        String cat = (category == null || category.isBlank()) ? null : category.trim();
        String query = (q == null || q.isBlank()) ? null : q.trim();

        return postRepo.findInBBox(south, north, west, east, st, cat, query)
                .stream()
                .filter(v -> v.getLat() != null && v.getLng() != null)
                .filter(v -> !v.isReview())   // ★ 후기글은 마커 제외
                .filter(v -> v.getLat() != null && v.getLng() != null)
                .map(v -> MarkerBriefResponse.builder()
                        .id(v.getId())
                        .title(v.getTitle())
                        .lat(v.getLat())
                        .lng(v.getLng())
                        // enum name 그대로 내려주면 프런트에서 대문자/소문자 비교 일관되게 가능
                        .status(v.getStatus() != null ? v.getStatus().name() : null)
                        .category(v.getCategory())
                        // .address(v.getAddress())
                        .build())
                .toList();

    }

    /**
     * 전체 마커 조회 (필터 없음) - 컨트롤러에서 무파라미터 호출할 때 사용
     */
    @Transactional(readOnly = true)
    public List<MarkerBriefResponse> getMarkers() {
        return postRepo.findAll().stream()
                .filter(v -> v.getLat() != null && v.getLng() != null)
                .filter(v -> !v.isReview())   // ★ 후기글은 마커 제외
                .map(v -> MarkerBriefResponse.builder()
                        .id(v.getId())
                        .title(v.getTitle())
                        .lat(v.getLat())
                        .lng(v.getLng())
                        .status(v.getStatus() != null ? v.getStatus().name() : null)
                        .category(v.getCategory())
                        // .address(v.getAddress()) // DTO에 address가 있으면 주석 해제
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VolunteerDetailResponse getDetail(Long id) {
        // 뷰어 정보 없이 호출되면 applied=false로 처리
        return getDetail(id, null);
    }

    /** viewerUserId: 현재 보고 있는 사용자 ID(세션에서 꺼내서 넘겨주면 applied 계산 가능) */
    @Transactional(readOnly = true)
    public VolunteerDetailResponse getDetail(Long id, Long viewerUserId) {
        VolunteerPost v = postRepo.findById(id).orElseThrow();

        long activeAppliedCount = appRepo.countActiveByPostId(id);

        // ✅ 내가 지원했는지 여부 계산 (승인/활성 상태만 true)
        boolean applied = false;
        if (viewerUserId != null) {
            Optional<Application> my = appRepo.findByPostIdAndApplicantUserId(id, viewerUserId);
            applied = my.isPresent() && my.get().getStatus() == ApplicationStatus.approved;
        }

        // ✅ 호스트 닉네임 채우기 (displayName 우선, 없으면 username)
        String hostNickname = null;
        String hostProfile = null; // 프로젝트에 필드 없으면 null 유지
        if (v.getHostUserId() != null) {
            Optional<Member> hostOpt = memberRepo.findById(v.getHostUserId());
            if (hostOpt.isPresent()) {
                Member h = hostOpt.get();
                if (h.getDisplayName() != null && !h.getDisplayName().isBlank()) {
                    hostNickname = h.getDisplayName();
                } else if (h.getUsername() != null && !h.getUsername().isBlank()) {
                    hostNickname = h.getUsername();
                }
                // 필요 시 프로필 이미지 필드 사용
                // hostProfile = h.getProfileImageUrl();
            }
        }
        VolunteerDetailResponse.Host host =
                new VolunteerDetailResponse.Host(v.getHostUserId(), hostNickname, hostProfile);

        // ✅ 레코드 순서에 맞춰서 반환 (startTime, applied, host)
        return new VolunteerDetailResponse(
                v.getId(),
                v.getTitle(),
                v.getDescription(),
                v.getAddress(),
                v.getLat() != null ? v.getLat() : 0.0,
                v.getLng() != null ? v.getLng() : 0.0,
                v.getCategory(),
                v.getStatus().name(),
                v.getCapacity(),
                activeAppliedCount,
                v.getStartTime(),   // ✅ detail.html에서 r.startTime 사용
                applied,            // ✅ detail.html에서 r.applied 사용
                host                // ✅ detail.html에서 r.host.nickname 사용
        );
    }

    /* -------------------- 지원/취소(선착순 자동 합격) -------------------- */

    /**
     * 선착순 자동 합격.
     * @return "ACCEPTED" | "FULL" | "DUP"
     */
    @Transactional
    public String apply(Long postId, Long userId) {
        VolunteerPost v = postRepo.findById(postId).orElseThrow();

        // 현재 승인(활성) 인원
        Long active = appRepo.countActiveByPostId(postId);
        Integer cap = v.getCapacity();

        // 정원 초과면 거절
        if (cap != null && active >= cap) return "FULL";

        // 중복 지원 방지
        Optional<Application> dup = appRepo.findByPostIdAndApplicantUserId(postId, userId);
        if (dup.isPresent()) return "DUP";

        // 자동 승인
        Application app = Application.builder()
                .postId(postId)
                .applicantUserId(userId)
                .status(ApplicationStatus.approved)
                .build();
        app.setLegacyPostId(postId);  // post_id 채움
        app.setLegacyUserId(userId);  // user_id 채움
        appRepo.save(app);

        // 방금 승인 반영 후 인원 계산하여 마감 처리
        if (cap != null) {
            long now = active + 1; // 이번 승인 포함
            if (now >= cap && v.getStatus() != VolunteerStatus.closed) {
                v.setStatus(VolunteerStatus.closed);
                postRepo.save(v);
            }
        }

        return "ACCEPTED";
    }

    @Transactional
    public boolean cancel(Long postId, Long userId) {
        Optional<Application> opt = appRepo.findByPostIdAndApplicantUserId(postId, userId);
        if (opt.isEmpty()) return false;
        Application app = opt.get();
        if (app.getStatus() == ApplicationStatus.canceled) return true;

        // 취소 처리
        app.setStatus(ApplicationStatus.canceled);
        appRepo.save(app);

        // 현재 활성 인원 다시 계산하여 재오픈 판단
        VolunteerPost v = postRepo.findById(postId).orElseThrow();
        Integer cap = v.getCapacity();
        if (cap != null) {
            long now = appRepo.countActiveByPostId(postId);
            if (now < cap && v.getStatus() == VolunteerStatus.closed) {
                v.setStatus(VolunteerStatus.open);
                postRepo.save(v);
            }
        }
        return true;
    }

    /* -------------------- 출석 체크 + 즉시 보상 -------------------- */

    /**
     * 주최자만 출석 토글 가능. 체크 ON & 미지급이면 즉시 경험치 +5 부여하고 rewarded=true.
     * 체크 OFF로 돌려도 경험치는 회수하지 않음(MVP 단순화).
     * @return true면 이번 호출 이후 rewarded 상태가 true (즉, 지급 완료 상태)
     */
    @Transactional
    public boolean setAttendanceAndReward(Long appId, boolean attended, Long hostUserId) {
        var app = appRepo.findById(appId)
                .orElseThrow(() -> new IllegalStateException("NOT_FOUND"));

        var post = postRepo.findById(app.getPostId())
                .orElseThrow(() -> new IllegalStateException("NOT_FOUND"));

        // 호스트만 가능
        if (hostUserId == null || !Objects.equals(hostUserId, post.getHostUserId())) {
            throw new IllegalStateException("FORBIDDEN");
        }

        // 시작 전 출석 금지
        LocalDateTime now = LocalDateTime.now();
        if (post.getStartTime() != null && post.getStartTime().isAfter(now)) {
            throw new IllegalStateException("BEFORE_START");
        }

        // 정책: 취소 불가
        if (!attended) {
            throw new IllegalStateException("CANNOT_UNATTEND");
        }

        // 이미 출석이면 멱등 처리(보상 X)
        if (app.isAttended()) {
            throw new IllegalStateException("ALREADY_ATTENDED");
        }

        // 출석 처리
        app.setAttended(true);

        // 보상: 1회만
        boolean rewardedApplied = false;
        if (!app.isRewarded()) {
            var member = memberRepo.findById(app.getApplicantUserId())
                    .orElseThrow(() -> new IllegalStateException("NOT_FOUND_MEMBER"));
            member.addExp(5);        // EXP +5 (레벨업 로직 포함)
            app.setRewarded(true);   // 보상 1회만
            rewardedApplied = true;
            memberRepo.save(member);
        }

        appRepo.save(app);
        return rewardedApplied;
    }

    /* -------------------- 지원자 정보(리스트) -------------------- */

    /**
     * /recruits/{postId}/apps 화면에서 사용할 지원자 목록 DTO.
     */
    @Transactional(readOnly = true)
    public List<ApplicantResponse> listApplicants(Long postId) {
        List<Application> apps = appRepo.findByPostId(postId);
        return apps.stream().map(a -> {
            // 지원자 표시명 얻기 (displayName 없으면 username, 둘 다 없으면 '익명')
            String name = "익명";
            Optional<Member> mo = memberRepo.findById(a.getApplicantUserId());
            if (mo.isPresent()) {
                Member m = mo.get();
                if (m.getDisplayName() != null && !m.getDisplayName().isBlank()) {
                    name = m.getDisplayName();
                } else if (m.getUsername() != null && !m.getUsername().isBlank()) {
                    name = m.getUsername();
                }
            }
            return new ApplicantResponse(
                    a.getId(),
                    a.getApplicantUserId(),
                    name,
                    a.getStatus().name(),
                    a.isAttended(),
                    a.isRewarded()
            );
        }).collect(Collectors.toList());
    }

    /**
     * 상태 자동 갱신:
     * - 시작시간이 지났고 아직 OPEN이면 CLOSED로 전환
     * - 정원이 다 찼으면 CLOSED
     * ※ 이 메서드는 상세/목록 조회 시점이나 스케줄러에서 호출하면 좋음.
     */
    @Transactional
    public void updateStatusIfNeeded(VolunteerPost post) {
        LocalDateTime now = LocalDateTime.now();

        // 시간이 지났는데 OPEN이면 -> CLOSED
        if (post.getStartTime() != null
                && now.isAfter(post.getStartTime())
                && post.getStatus() == VolunteerStatus.open) {
            post.setStatus(VolunteerStatus.closed);
        }

        // 정원이 다 찼으면 -> CLOSED
        Integer cap = post.getCapacity();
        if (cap != null) {
            long active = appRepo.countActiveByPostId(post.getId());
            if (active >= cap && post.getStatus() != VolunteerStatus.closed) {
                post.setStatus(VolunteerStatus.closed);
            }
        }

        postRepo.save(post);
    }

    /* -------------------- (선택) 간단 마커 바디/팝업 헬퍼 -------------------- */

    // BBox + 필터로 간단 JSON 바디 구성 (markers 배열만 담음)
    @Transactional(readOnly = true)
    public Map<String, Object> getBriefMarkers(
            double minLat, double minLng, double maxLat, double maxLng,
            String q, String category, String status
    ) {
        VolunteerStatus st = (status == null || status.isBlank()) ? null
                : VolunteerStatus.valueOf(status);

        List<VolunteerPost> rows = postRepo.findInBBox(minLat, maxLat, minLng, maxLng, st,
                (category == null || category.isBlank()) ? null : category,
                (q == null || q.isBlank()) ? null : q);

        List<Map<String, Object>> markers = new ArrayList<>();
        for (VolunteerPost v : rows) {
            if (v.getLat() == null || v.getLng() == null) continue; // 좌표 없는 글 제외
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("title", v.getTitle());
            m.put("lat", v.getLat());
            m.put("lng", v.getLng());
            m.put("status", v.getStatus() != null ? v.getStatus().name() : null);
            m.put("category", v.getCategory());
            markers.add(m);
        }

        Map<String,Object> body = new LinkedHashMap<>();
        body.put("markers", markers);
        return body;
    }

    /** d 객체에서 여러 후보 이름으로 게터/필드를 찾아 값을 꺼낸다. 타입 캐스팅도 시도 */


    /** 리스트 후보를 찾아서 List 로 반환 (없으면 빈 리스트) */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApplicants(Long postId, Long viewerId) {
        var apps = appRepo.findByPostId(postId);
        boolean hostView = isHost(postId, viewerId);

        List<Map<String, Object>> out = new ArrayList<>();
        for (var a : apps) {
            // === applicantId 계산 (기존 로직 유지) ===
            Long applicantId = null;
            try {
                for (String m : List.of("getApplicantUserId", "getUserId", "getMemberId", "getApplicantId")) {
                    try {
                        var mm = a.getClass().getMethod(m);
                        Object v = mm.invoke(a);
                        if (v instanceof Long l) { applicantId = l; break; }
                        if (v instanceof Number n) { applicantId = n.longValue(); break; }
                    } catch (NoSuchMethodException ignore) {}
                }
                if (applicantId == null) {
                    for (String m : List.of("getMember", "getUser", "getApplicant")) {
                        try {
                            var mm = a.getClass().getMethod(m);
                            Object ref = mm.invoke(a);
                            if (ref != null) {
                                var idM = ref.getClass().getMethod("getId");
                                Object idV = idM.invoke(ref);
                                if (idV instanceof Long l) { applicantId = l; break; }
                                if (idV instanceof Number n) { applicantId = n.longValue(); break; }
                            }
                        } catch (NoSuchMethodException ignore) {}
                    }
                }
            } catch (Exception ignore) {}

            // === 표시명 계산 (사람 이름/닉 우선) ===
            String username = null, email = null;
            String preferred = null;

            if (applicantId != null) {
                var memOpt = memberRepo.findById(applicantId);
                if (memOpt.isPresent()) {
                    var mem = memOpt.get();

                    // 1) 사람 이름 후보: displayName > name > nickname (있으면)
                    preferred = firstNonBlank(
                            safe(getOrNull(mem::getDisplayName)),
                            safe(getOrNull(() -> tryCallString(mem, "getName"))),
                            safe(getOrNull(() -> tryCallString(mem, "getNickname")))
                    );

                    // 2) 아이디/메일 후보
                    username = safe(getOrNull(mem::getUsername));
                    email    = safe(getOrNull(() -> tryCallString(mem, "getEmail")));

                    // 3) 최종 폴백
                    if (preferred == null) {
                        preferred = firstNonBlank(
                                localPart(username),
                                localPart(email),
                                (applicantId != null ? "사용자#" + applicantId : null),
                                "익명"
                        );
                    }
                }
            }

            if (preferred == null) {
                preferred = firstNonBlank(
                        (applicantId != null ? "사용자#" + applicantId : null),
                        "익명"
                );
            }

            // 호스트이면 실명/닉 그대로, 비호스트면 익명화
            String displayName = hostView
                    ? preferred
                    : ("익명" + (applicantId != null ? (applicantId % 1000) : 0));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("applicationId", a.getId()); // 호환용
            row.put("userId", applicantId);
            row.put("displayName", displayName);
            row.put("name", displayName);

            // status
            String status = null;
            try {
                var stM = a.getClass().getMethod("getStatus");
                Object st = stM.invoke(a);
                status = (st != null) ? st.toString() : null;
            } catch (Exception ignore) {}
            row.put("status", status);

            // 출석/보상
            row.put("attended", (a instanceof Application ap) ? ap.isAttended() : null);
            row.put("rewarded", (a instanceof Application ap) ? ap.isRewarded() : null);

            out.add(row);
        }
        return out;
    }



    // == postId와 viewerId로 "호스트 여부" 판단 ==
    // == 리플렉션으로 VolunteerPost의 소유자 ID를 최대한 안전하게 추출 ==
    // == 리플렉션으로 VolunteerPost의 소유자 ID를 최대한 안전하게 추출 ==
    private Long extractOwnerId(Object post) {
        if (post == null) return null;

        // 1) Long 직접 보유한 getter 우선 시도
        String[] idCandidates = {
                "getHostUserId", "getHostId", "getOwnerId", "getAuthorId", "getWriterId", "getCreatorId", "getCreatedById"
        };
        for (String m : idCandidates) {
            try {
                var method = post.getClass().getMethod(m);
                Object v = method.invoke(post);
                if (v instanceof Long l) return l;
                if (v instanceof Number n) return n.longValue();
            } catch (Exception ignore) {}
        }

        // 2) Member 같은 연관객체에서 꺼내기
        String[] refCandidates = {
                "getMember", "getOwner", "getAuthor", "getWriter", "getCreator", "getOrganizer", "getUser"
        };
        for (String m : refCandidates) {
            try {
                var method = post.getClass().getMethod(m);
                Object ref = method.invoke(post);
                if (ref != null) {
                    try {
                        var idM = ref.getClass().getMethod("getId");
                        Object idV = idM.invoke(ref);
                        if (idV instanceof Long l) return l;
                        if (idV instanceof Number n) return n.longValue();
                    } catch (Exception ignore2) {}
                }
            } catch (Exception ignore) {}
        }

        // 3) 최후: createdBy 같은 필드가 String(이메일)일 수도 있으니 username 비교를 위해 null 반환
        return null;
    }

    // == postId와 viewerId로 "호스트 여부" 판단 ==
    private boolean isHost(Long postId, Long viewerId) {
        if (postId == null || viewerId == null) return false;
        var postOpt = postRepo.findById(postId);
        if (postOpt.isEmpty()) return false;

        Object post = postOpt.get();
        Long ownerId = extractOwnerId(post);

        if (ownerId != null) {
            return ownerId.equals(viewerId);
        }

        // (옵션) ownerId를 못 찾았을 때의 보정: post에 username/이메일이 있을 수도 있지만
        // 스키마를 확정 못 하므로 여기서는 false로 두고, 필요하면 추가 보정 로직을 더 넣을 것.
        return false;
    }

    //출석체크
    // null/blank → null 로 정리
    private static String safe(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // N개 중 처음으로 “의미 있는 값” 반환
    private static String firstNonBlank(String... xs) {
        for (String x : xs) {
            if (x != null && !x.isBlank()) return x;
        }
        return null;
    }

    // "abc@naver.com" → "abc", 없으면 원문 또는 null
    private static String localPart(String s) {
        if (s == null) return null;
        String t = s.trim();
        int at = t.indexOf('@');
        return (at > 0) ? t.substring(0, at) : t;
    }

    /** 리플렉션 보조: mem.getClass().getMethod(name).invoke(mem) 를 String으로 안전 호출 */
    private static String tryCallString(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return (v != null) ? v.toString() : null;
        } catch (Throwable ignore) {
            return null;
        }
    }

    // Optional supplier 안전 호출용 (람다에서 예외 방지)
    @FunctionalInterface
    private interface ThrowingSupplier<T> { T get() throws Exception; }
    private static <T> T getOrNull(ThrowingSupplier<T> sup) {
        try { return sup.get(); } catch (Throwable t) { return null; }
    }




}
