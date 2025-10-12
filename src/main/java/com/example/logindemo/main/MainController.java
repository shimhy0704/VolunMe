package com.example.logindemo.main;

import com.example.logindemo.member.Member;
import com.example.logindemo.member.MemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;             // ✅ 중복 import 하나만 유지
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final MemberRepository memberRepository;

    @GetMapping("/main")
    public String main(
            @RequestParam(value = "userId", required = false) Long userId,
            HttpSession session,
            Model model
    ) {
        // 로그인 사용자 (세션 방식)
        Member me = (Member) session.getAttribute("loggedInMember");

        // “누구의 아바타를 보여줄지” 결정
        Member avatarOwner;
        boolean isOwnerView;

        if (userId != null) {
            // 다른 사람 아바타 보기 모드
            avatarOwner = memberRepository.findById(userId).orElse(null);
            if (avatarOwner == null) {
                // 잘못된 userId면 내 메인으로
                avatarOwner = me;
            }
            isOwnerView = (me != null && avatarOwner != null && me.getId().equals(avatarOwner.getId()));
        } else {
            // 기본: 내 아바타
            avatarOwner = me;
            isOwnerView = true;
        }

        // 아바타 이미지/레이어 URL 준비
        String profileUrl = resolveProfileUrl(avatarOwner);

        // level 계산 + 기본값 보정 (Lv.null 방지)
        Integer level = 1;
        if (avatarOwner != null) {
            try {
                // Member에 getLevel()이 있으면 사용 (다르면 실제 게터명으로 교체)
                var lm = avatarOwner.getClass().getMethod("getLevel");
                Object lv = lm.invoke(avatarOwner);
                if (lv instanceof Number n) level = Math.max(1, n.intValue());
            } catch (NoSuchMethodException ignore) {
                // 레벨 필드 없으면 기본 1 유지
            } catch (Exception ignore) {}
        }

        // profileUrl이 비거나 '.'이면 기본 아이콘으로 보정 (No static resource . 방지)
        if (profileUrl == null || profileUrl.trim().isEmpty() || ".".equals(profileUrl.trim())) {
            profileUrl = "/img/bearhead.png";
        }

        // EXP/임계치/퍼센트 계산
        long exp = 0;
        long expToLevel = Math.max(1, (long) level * 5L); // ★ 기본 규칙: 레벨*5

        try {
            if (avatarOwner != null) {
                var gm = avatarOwner.getClass().getMethod("getExp");
                Object val = gm.invoke(avatarOwner);
                if (val instanceof Number n) exp = Math.max(0, n.longValue());
            }
        } catch (NoSuchMethodException ignore) {
            // Member에 getExp가 없으면 기본 0 유지
        } catch (Exception ignore) {}

// getExpToLevel() 게터가 프로젝트에 실제로 있다면 그 값을 우선 사용
        try {
            if (avatarOwner != null) {
                var gm2 = avatarOwner.getClass().getMethod("getExpToLevel");
                Object val2 = gm2.invoke(avatarOwner);
                if (val2 instanceof Number n2) expToLevel = Math.max(1, n2.longValue());
            }
        } catch (NoSuchMethodException ignore) {
            // 없으면 레벨*5 유지
        } catch (Exception ignore) {}

// percent 계산 (0~100 클램프)
        int percent = 0;
        if (expToLevel > 0) {
            double p = (exp * 100.0) / expToLevel;
            if (p < 0) p = 0;
            if (p > 100) p = 100;
            percent = (int) Math.round(p);
        }

        // 모델 바인딩
        model.addAttribute("avatarOwner", avatarOwner);
        model.addAttribute("profileUrl", profileUrl);
        model.addAttribute("isOwnerView", isOwnerView);
        model.addAttribute("level", level);
        model.addAttribute("bearImg", bearLevelImage(level));
        model.addAttribute("exp", exp);
        model.addAttribute("expToLevel", expToLevel);
        model.addAttribute("percent", percent);

// 읽기 전용 플래그: 남의 아바타 볼 땐 편집 비활성화
        boolean readOnly = !isOwnerView;
        model.addAttribute("readOnly", readOnly);

        return "main"; // 기존 main.html 그대로 사용
    }

    // 레벨별 곰 이미지: 1=bear.png, 2~6=bear{n}.png, 7+=bear6.png
    private String bearLevelImage(int level) {
        if (level <= 1) return "/img/bear.png";
        if (level >= 7) return "/img/bear6.png";
        return "/img/bear" + level + ".png";
    }

    // 프로필 URL 해석
    private String resolveProfileUrl(Member m) {
        final String DEFAULT_URL = "/img/bearhead.png";
        if (m == null) return DEFAULT_URL;

        String[] candidates = new String[] {
                callGetterIfExists(m, "getKakaoProfileUrl"),
                callGetterIfExists(m, "getKakaoProfileImage"),
                callGetterIfExists(m, "getProfileImageUrl"),
                callGetterIfExists(m, "getAvatarUrl"),
                callGetterIfExists(m, "getImageUrl")
        };

        for (String url : candidates) {
            if (hasText(url)) return url;
        }
        return DEFAULT_URL;
    }

    /** 리플렉션으로 게터가 있으면 호출해서 String 반환, 없거나 실패하면 null */
    private String callGetterIfExists(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            Object val = m.invoke(target);
            return (val != null) ? String.valueOf(val) : null;
        } catch (NoSuchMethodException e) {
            return null; // 게터 없음
        } catch (Exception e) {
            return null; // 호출 실패 시 무시
        }
    }

    /** 문자열 공백 체크 */
    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/main";
    }
}
