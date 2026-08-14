package com.tripass.auth.security;

import com.tripass.common.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Component
public class OAuthStateCookieProvider {

    private static final String COOKIE_NAME =
            "kakaoOAuthState";

    private static final String COOKIE_PATH =
            "/api/v1/auth/social/kakao";

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final boolean secure;
    private final long maxAgeSeconds;

    public OAuthStateCookieProvider(
            @Value("${auth.oauth-state-cookie.secure:true}")
            boolean secure,

            @Value("${auth.oauth-state-cookie.max-age-seconds:300}")
            long maxAgeSeconds
    ) {
        this.secure = secure;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public String generateState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    public void addStateCookie(
            HttpServletResponse response,
            String state
    ) {
        ResponseCookie cookie =
                ResponseCookie
                        .from(COOKIE_NAME, state)
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite("Lax")
                        .path(COOKIE_PATH)
                        .maxAge(
                                Duration.ofSeconds(
                                        maxAgeSeconds
                                )
                        )
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public String getState(
            HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public void validateState(
            String expectedState,
            String returnedState
    ) {
        if (
                expectedState == null ||
                        returnedState == null ||
                        expectedState.isBlank() ||
                        returnedState.isBlank()
        ) {
            throw invalidStateException();
        }

        boolean matched =
                MessageDigest.isEqual(
                        expectedState.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        returnedState.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        if (!matched) {
            throw invalidStateException();
        }
    }

    public void deleteStateCookie(
            HttpServletResponse response
    ) {
        ResponseCookie cookie =
                ResponseCookie
                        .from(COOKIE_NAME, "")
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite("Lax")
                        .path(COOKIE_PATH)
                        .maxAge(Duration.ZERO)
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    private CustomException invalidStateException() {
        return new CustomException(
                HttpStatus.BAD_REQUEST,
                "KAKAO_OAUTH_STATE_INVALID",
                "카카오 로그인 요청이 만료되었거나 유효하지 않습니다."
        );
    }
}