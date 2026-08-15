package com.tripass.auth.dto.external.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 카카오 사용자 정보 조회 응답
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
public class KakaoUserResponse {

    // 카카오 회원 고유 ID
    private Long id;


    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {

        @JsonProperty("profile_nickname_needs_agreement")
        private Boolean profileNicknameNeedsAgreement;

        private Profile profile;

        @JsonProperty("has_email")
        private Boolean hasEmail;

        @JsonProperty("email_needs_agreement")
        private Boolean emailNeedsAgreement;

        @JsonProperty("is_email_valid")
        private Boolean emailValid;

        @JsonProperty("is_email_verified")
        private Boolean emailVerified;

        private String email;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @NoArgsConstructor
    public static class Profile {

        private String nickname;
    }
}