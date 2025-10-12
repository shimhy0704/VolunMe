package com.example.logindemo.sociallogin; // 프로젝트 패키지에 맞게 유지하세요.

import com.example.logindemo.member.Member;
import com.example.logindemo.member.MemberRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Service
public class KakaoService {
    private static final Logger log = LoggerFactory.getLogger(KakaoService.class);

    private final MemberRepository memberRepository;
    private final String clientId;
    private final String redirectUri;

    public KakaoService(MemberRepository memberRepository,
                        @Value("${kakao.client_id}") String clientId,
                        @Value("${kakao.redirect_uri}") String redirectUri) {
        this.memberRepository = memberRepository;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    public String getAccessTokenFromKakao(String code) {
        log.info("카카오 토큰 요청 시작 - code: {}", code);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        log.info("토큰 요청 파라미터 - client_id: {}, redirect_uri: {}", clientId, redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        ResponseEntity<KakaoTokenResponseDto> response = restTemplate.postForEntity(
                tokenUrl,
                request,
                KakaoTokenResponseDto.class
        );
        KakaoTokenResponseDto kakaoTokenResponseDto = response.getBody();
        if (kakaoTokenResponseDto == null) throw new RuntimeException("카카오 토큰 응답이 null입니다.");

        log.info("카카오 토큰 발급 성공 - accessToken: {}", kakaoTokenResponseDto.getAccessToken());
        return kakaoTokenResponseDto.getAccessToken();
    }

    /**
     * 1) 카카오 사용자 정보 조회
     * 2) email / nickname / profileImage 파싱
     * 3) users.profile_image_url을 email 기준으로 업데이트 (http -> https 정규화)
     * 4) Member 조회/생성 반환
     */
    public Member saveOrGetMemberFromKakao(String accessToken) {
        KakaoUserInfoResponseDto userInfo = getKakaoUserInfo(accessToken);
        if (userInfo == null) {
            throw new RuntimeException("카카오 사용자 정보가 null입니다.");
        }

        Long id = userInfo.getId();
        KakaoUserInfoResponseDto.KakaoAccount account = userInfo.getKakaoAccount();

        String email = (account != null) ? nullSafeTrim(account.getEmail()) : null;
        String name = (account != null) ? nullSafeTrim(account.getName()) : null;
        String nickname = (account != null && account.getProfile() != null)
                ? nullSafeTrim(account.getProfile().getNickname())
                : null;
        String profileImage = (account != null && account.getProfile() != null)
                ? nullSafeTrim(account.getProfile().getProfileImageUrl())
                : null;

        log.info("카카오 로그인 정보: id={}, name={}, email={}, nickname={}, profileImage={}",
                id, name, email, nickname, profileImage);

        if (email == null || email.isBlank()) {
            throw new RuntimeException("카카오 계정에 이메일이 없습니다. (동의 항목 확인 필요)");
        }

        // ── (핵심) users.profile_image_url 업데이트 ─────────────────────────────
        // - http → https 정규화
        // - 업데이트 결과 0이면 users row가 아직 없는 것 (최초 로그인 시 INSERT 로직 별도 고려)
        if (profileImage != null && !profileImage.isBlank()) {
            String normalizedProfile = normalizeHttps(profileImage);
            try {
                int updated = memberRepository.updateUsersProfileImageUrlByEmail(email, normalizedProfile);
                if (updated == 1) {
                    log.info("users.profile_image_url 업데이트 성공 - email={}, url={}", email, normalizedProfile);
                } else {
                    log.info("users.profile_image_url 업데이트 대상 없음 (users row 미존재 가능) - email={}", email);
                    // 필요 시 여기서 users INSERT 로직을 추가하세요.
                    // ex) memberRepository.insertUsersByEmail(email, normalizedProfile);
                }
            } catch (Exception e) {
                log.warn("users.profile_image_url 업데이트 중 오류 - email={}, url={}, err={}",
                        email, normalizedProfile, e.toString());
            }
        } else {
            log.info("카카오 profile_image_url 없음(또는 빈 문자열) - email={}", email);
        }
        // ─────────────────────────────────────────────────────────────────────

        // 기존 멤버 조회/생성
        Optional<Member> optionalMember = memberRepository.findByUsername(email);
        if (optionalMember.isPresent()) {
            return optionalMember.get();
        } else {
            Member member = new Member();
            member.setUsername(email);
            member.setPassword(UUID.randomUUID().toString());
            member.setDisplayName((nickname != null && !nickname.isBlank()) ? nickname : "kakao-user");
            return memberRepository.save(member);
        }
    }

    public KakaoUserInfoResponseDto getKakaoUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        String userUrl = "https://kapi.kakao.com/v2/user/me";

        ResponseEntity<KakaoUserInfoResponseDto> response = restTemplate.exchange(
                userUrl,
                HttpMethod.GET,
                entity,
                KakaoUserInfoResponseDto.class
        );
        return response.getBody();
    }

    // ── 유틸 ────────────────────────────────────────────────────────────────
    private String nullSafeTrim(String s) {
        return (s == null) ? null : s.trim();
    }

    /** http:// 로 오면 https:// 로 강제 변환 (혼합콘텐츠/리다이렉트 방지) */
    private String normalizeHttps(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("http://")) {
            return "https://" + s.substring("http://".length());
        }
        return s;
    }
    // ───────────────────────────────────────────────────────────────────────
}
