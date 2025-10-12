package com.example.logindemo.sociallogin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfoResponseDto {
    private Long id;  // JSON: "id"

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;  // JSON: "kakao_account"

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {
        private Profile profile;  // JSON: "profile"
        private String email;     // JSON: "email"
        private String name;      // JSON: "name"
    }

    @Getter
    @NoArgsConstructor
    public static class Profile {
        private String nickname;           // JSON: "nickname"

        @JsonProperty("profile_image_url")
        private String profileImageUrl;  // JSON: "profile_image_url"
    }
}

