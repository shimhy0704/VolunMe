package com.example.logindemo.sociallogin;

import com.example.logindemo.member.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/OAuth2")
public class KakaoController {
    private final KakaoService kakaoService;

    @Value("${app.redirect.base-url:http://localhost:8080}")
    private String baseUrl; // 로그인 완료 후 보내줄 우리 서비스 주소

    @Value("${kakao.client_id}")
    private String clientId;

    @Value("${kakao.redirect_uri}")
    private String redirectUri; // 카카오 콘솔에 등록한 값과 100% 동일해야 함

    // 선택: 필요한 동의 항목(닉네임/프로필/이메일)
    @Value("${kakao.scope:profile_nickname,profile_image,account_email}")
    private String scope;

    public KakaoController(KakaoService kakaoService) {
        this.kakaoService = kakaoService;
    }

    @GetMapping("/kakao/login")
    public ResponseEntity<Void> loginPage(HttpSession session) {
        // CSRF 방지를 위한 state 사용 권장
        String state = UUID.randomUUID().toString();
        session.setAttribute("oauth_state", state);

        String authUrl =
                "https://kauth.kakao.com/oauth/authorize"
                        + "?response_type=code"
                        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8)
                        + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", authUrl); // ✅ 반드시 카카오 인증 URL로!
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpSession session) {

        // 실패 처리
        if (error != null) {
            HttpHeaders fail = new HttpHeaders();
            fail.add("Location", baseUrl + "/login?error=" + error);
            return new ResponseEntity<>(fail, HttpStatus.FOUND);
        }

        // state 검증(선택이지만 권장)
        Object saved = session.getAttribute("oauth_state");
        if (saved != null && state != null && !saved.equals(state)) {
            HttpHeaders bad = new HttpHeaders();
            bad.add("Location", baseUrl + "/login?error=bad_state");
            return new ResponseEntity<>(bad, HttpStatus.FOUND);
        }

        // 1) 인가코드로 토큰 발급
        String accessToken = kakaoService.getAccessTokenFromKakao(code);

        // 2) 토큰으로 사용자 정보 조회 및 회원 저장/조회
        Member member = kakaoService.saveOrGetMemberFromKakao(accessToken);

        // 3) 세션 저장
        session.setAttribute("loggedInMember", member);
        session.setMaxInactiveInterval(60 * 30); // 30분

        // 4) 우리 서비스로 리다이렉트 (환경 별 베이스 URL 사용)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", baseUrl + "/posts"); // ✅ 하드코드 금지, 프로퍼티 사용
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
