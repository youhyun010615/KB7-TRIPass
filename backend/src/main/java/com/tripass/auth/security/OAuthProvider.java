package com.tripass.auth.security;

import lombok.Getter;

@Getter
public enum OAuthProvider {

    KAKAO(
            "kakaoOAuthState",
            "/api/v1/auth/social/kakao",
            "KAKAO_OAUTH_STATE_INVALID",
            "카카오 로그인 요청이 만료되었거나 유효하지 않습니다."
    ),

    GOOGLE(
            "googleOAuthState",
            "/api/v1/auth/social/google",
            "GOOGLE_OAUTH_STATE_INVALID",
            "Google 로그인 요청이 만료되었거나 유효하지 않습니다."
    );

    private final String cookieName;
    private final String cookiePath;
    private final String invalidStateErrorCode;
    private final String invalidStateMessage;

    OAuthProvider(
            String cookieName,
            String cookiePath,
            String invalidStateErrorCode,
            String invalidStateMessage
    ) {
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.invalidStateErrorCode =
                invalidStateErrorCode;
        this.invalidStateMessage =
                invalidStateMessage;
    }
}