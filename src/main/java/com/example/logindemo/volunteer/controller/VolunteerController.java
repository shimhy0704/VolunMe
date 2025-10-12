package com.example.logindemo.volunteer.controller;

import com.example.logindemo.member.Member;
import com.example.logindemo.member.MemberRepository;
import com.example.logindemo.volunteer.dto.MarkerBriefResponse;
import com.example.logindemo.volunteer.dto.VolunteerCreateRequest;
import com.example.logindemo.volunteer.dto.VolunteerDetailResponse;
import com.example.logindemo.volunteer.service.VolunteerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/recruits")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService service;
    private final MemberRepository memberRepository;

    /**
     * 모집/후기 공통 생성 (폼 → API)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createRecruitForm(
            @ModelAttribute VolunteerCreateRequest req,
            @RequestParam(value = "files", required = false) MultipartFile[] files,   // ★ 추가
            HttpSession session
    ) {
        Object m = session.getAttribute("loggedInMember");
        if (!(m instanceof Member me)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        Long id = service.createRecruit(me.getId(), req, files);  // ★ files 전달
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", id));
    }

    // 출석체크 시작
    // 출석체크 시작
    @PatchMapping(value = "/apps/{appId}/attend",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> attend(
            @PathVariable Long appId,
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {
        Object obj = session.getAttribute("loggedInMember");
        if (!(obj instanceof Member me)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인이 필요합니다."));
        }

        Object val = body.get("attended");
        boolean attended = (val instanceof Boolean b) ? b
                : (val instanceof String s) ? Boolean.parseBoolean(s)
                : false;
        if (!attended) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("ok", false, "message", "출석은 취소할 수 없습니다."));
        }

        try {
            boolean rewardedApplied = service.setAttendanceAndReward(appId, true, me.getId());

            // 🔁 [핵심] 세션 최신화: 로그인 사용자의 스냅샷을 DB 최신으로 교체
            //    (본인 출석이든, 호스트가 남을 체크하든 안전하며 비용도 낮음)
            session.setAttribute(
                    "loggedInMember",
                    memberRepository.findById(me.getId()).orElseThrow()
            );

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "attended", true,
                    "rewardedApplied", rewardedApplied
            ));
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            HttpStatus status;
            switch (msg) {
                case "FORBIDDEN":
                    status = HttpStatus.FORBIDDEN;
                    break;
                case "BEFORE_START":
                    status = HttpStatus.CONFLICT;
                    break;
                case "ALREADY_ATTENDED":
                    // (선택) 이미 출석되어 있어도 한번 세션을 최신화해 두면 안전
                    session.setAttribute(
                            "loggedInMember",
                            memberRepository.findById(me.getId()).orElseThrow()
                    );
                    return ResponseEntity.ok(Map.of(
                            "ok", true,
                            "attended", true,
                            "rewardedApplied", false,
                            "message", "ALREADY_ATTENDED"
                    ));
                default:
                    status = HttpStatus.BAD_REQUEST;
            }
            return ResponseEntity.status(status).body(Map.of(
                    "ok", false,
                    "message", msg
            ));
        }
    }

    /* -------------------- 공통: 세션 유틸 -------------------- */
    private Long meId(HttpSession session) {
        Object m = session.getAttribute("loggedInMember");
        if (m instanceof Member member) return member.getId();
        return null;
    }

    private Long resolveRecruitId(Long id) {
        return id;
    }

    /* ========== 지도 브리프(마커) ========== */
    @GetMapping(value = "/brief", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> brief(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        if (minLat == null || minLng == null || maxLat == null || maxLng == null) {
            double cLat = 37.456, cLng = 126.705;
            double d = 0.08;
            minLat = cLat - d;
            maxLat = cLat + d;
            minLng = cLng - d;
            maxLng = cLng + d;
        }
        String qq = (q != null && !q.isBlank()) ? q.trim() : null;
        String cat = (category != null && !category.isBlank()) ? category.trim() : null;
        String st = (status != null && !status.isBlank()) ? status.trim() : null;

        List<MarkerBriefResponse> markers = service.getMarkers(
                minLat, minLng, maxLat, maxLng, qq, cat, st
        );
        return ResponseEntity.ok(Map.of("markers", markers));
    }

    @GetMapping("/{postId}/applicants")
    public ResponseEntity<?> getApplicants(@PathVariable Long postId, HttpSession session) {
        Long viewerId = meId(session);
        var list = service.getApplicants(postId, viewerId);
        return ResponseEntity.ok(list);
    }

    /* ========== 지도 마커(필터) ========== */
    @GetMapping(value = "/markers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MarkerBriefResponse> markers(
            @RequestParam(required = false) Double south,
            @RequestParam(required = false) Double west,
            @RequestParam(required = false) Double north,
            @RequestParam(required = false) Double east,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(required = false) String status
    ) {
        if (south == null && west == null && north == null && east == null
                && (category == null || category.isBlank())
                && (keyword == null || keyword.isBlank())
                && (status == null || status.isBlank())) {
            return service.getMarkers();
        }
        double s = south != null ? south : -90d;
        double w = west != null ? west : -180d;
        double n = north != null ? north : 90d;
        double e = east != null ? east : 180d;
        String cat = (category != null) ? category : "";
        String q = (keyword != null) ? keyword : "";
        String st = (status != null) ? status : "";
        return service.getMarkers(s, w, n, e, cat, q, st);
    }

    /* ========== 지원하기 ========== */
    @PostMapping(value = "/{id}/apply", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> apply(
            @PathVariable("id") Long recruitId, HttpSession session
    ) {
        Map<String, Object> body = new HashMap<>();

        Object meObj = session.getAttribute("loggedInMember");
        if (!(meObj instanceof Member me)) {
            body.put("ok", false);
            body.put("reason", "NOT_LOGGED_IN");
            body.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(body);
        }

        final String raw;
        try {
            raw = service.apply(recruitId, me.getId());
        } catch (NoSuchElementException nf) {
            body.put("ok", false);
            body.put("reason", "NOT_FOUND");
            body.put("message", "모집글을 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(body);
        } catch (Exception e) {
            body.put("ok", false);
            body.put("reason", "SERVER_ERROR");
            body.put("message", "서버 오류로 지원에 실패했습니다.");
            return ResponseEntity.status(500).body(body);
        }

        String code = (raw == null || raw.isBlank()) ? "OK" : raw.trim().toUpperCase();
        switch (code) {
            case "DUP":
                body.put("ok", false);
                body.put("reason", "DUPLICATE");
                body.put("message", "이미 지원한 모집입니다.");
                return ResponseEntity.status(409).body(body);
            case "FULL":
                body.put("ok", false);
                body.put("reason", "FULL");
                body.put("message", "정원이 모두 찼습니다.");
                return ResponseEntity.status(409).body(body);
            case "CLOSED":
                body.put("ok", false);
                body.put("reason", "CLOSED");
                body.put("message", "모집이 마감되었거나 시간이 지났습니다.");
                return ResponseEntity.status(409).body(body);
            default:
                body.put("ok", true);
                body.put("message", "지원이 완료되었습니다.");
                return ResponseEntity.ok(body);
        }
    }

    // (참고) 뷰 반환은 @Controller 에서 처리하는 게 맞음. API 컨트롤러에서는 보통 제거.
    @GetMapping("/posts/new")
    public String newPost() {
        return "posts/new";
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> detail(@PathVariable Long id) {
        Long rid = resolveRecruitId(id);
        if (rid == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Recruit not found"));
        }
        try {
            VolunteerDetailResponse dto = service.getDetail(rid);
            if (dto == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Recruit not found"));
            }
            return ResponseEntity.ok(dto);
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            if (rse.getStatusCode() == HttpStatus.GONE) {
                return ResponseEntity.status(404).body(Map.of("message", "Recruit gone"));
            }
            if (rse.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.status(404).body(Map.of("message", "Recruit not found"));
            }
            throw rse;
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "지원 상세 조회 중 오류"));
        }
    }

    @GetMapping(value = "/{id}/removed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> removedDetail(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of(
                        "message", "상세페이지/상세 API는 제거되었습니다. 지도 팝업 API를 사용하세요.",
                        "use", "/api/recruits/{id}/popup"
                ));
    }

    /**
     * 후기 전용 생성 (강제 review=true)
     */
    @PostMapping(value = "/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createReview(
            @ModelAttribute VolunteerCreateRequest req,
            @RequestParam(value = "files", required = false) MultipartFile[] files,  // ★ 추가
            HttpSession session
    ) {
        Member me = (Member) session.getAttribute("loggedInMember");
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "로그인 필요"));
        }
        req.setReview(true); // ★ 강제 후기 플래그
        Long id = service.createRecruit(me.getId(), req, files); // ★ files 전달
        return ResponseEntity.ok(Map.of("ok", true, "id", id));
    }

    // ✅ 아래 중복/오경로 핸들러는 삭제했습니다.
    // @PostMapping(value = "/api/recruits", ...)  ← 클래스 레벨 prefix 때문에 실제 /api/recruits/api/recruits 가 되어 잘못된 매핑

    @PostMapping(path = "/form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createRecruitFormRedirect(
            @ModelAttribute VolunteerCreateRequest req,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            HttpSession session
    ) {
        Member login = (Member) session.getAttribute("loggedInMember");
        if (login == null) {
            return ResponseEntity.status(303).header(HttpHeaders.LOCATION, "/login").build();
        }
        MultipartFile[] fileArr = (files == null) ? new MultipartFile[0] : files.toArray(new MultipartFile[0]);
        // 시그니처: (hostUserId, req, files[])
        service.createRecruit(login.getId(), req, fileArr);

        // 화면 이동 목표: /posts
        return ResponseEntity.status(303).header(HttpHeaders.LOCATION, "/posts").build();
    }
}
