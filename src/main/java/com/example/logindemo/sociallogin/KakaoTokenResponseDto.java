package com.example.logindemo.sociallogin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoTokenResponseDto {
    @JsonProperty("token_type")
    public String tokenType;

    @JsonProperty("access_token")
    public String accessToken;

    @JsonProperty("id_token")
    public String idToken;

    @JsonProperty("expires_in")
    public Integer expiresIn;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("refresh_token_expires_in")
    public Integer refreshTokenExpiresIn;

    @JsonProperty("scope")
    public String scope;
}
/**
 *
 *
 * ✅ 필수
 * ⛔ 선택/부가 정보
 *
 * tokenType               ✅  토큰 타입 (bearer로 고정)
 * accessToken             ✅  액세스 토큰
 * idToken                 ⛔  OpenID Connect에서 사용
 * expiresIn               ✅  액세스 토큰 만료 시간 (초)
 * refreshToken            ✅  리프레시 토큰
 * refreshTokenExpiresIn   ✅  리프레시 토큰 만료 시간 (초)
 * scope                   ⛔  요청한 인증 범위 (ex: profile, email)
 */
