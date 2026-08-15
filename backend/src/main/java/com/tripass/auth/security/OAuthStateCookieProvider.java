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

    // 예측 불가능한 OAuth state를 생성한다.
    public String generateState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // 로그인 제공자별 HttpOnly state 쿠키를 저장한다.
    public void addStateCookie(
            HttpServletResponse response,
            OAuthProvider provider,
            String state
    ) {
        ResponseCookie cookie =
                ResponseCookie
                        .from(
                                provider.getCookieName(),
                                state
                        )
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite("Lax")
                        .path(
                                provider.getCookiePath()
                        )
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

    // 요청에서 로그인 제공자별 OAuth state 쿠키를 조회한다.
    public String getState(
            HttpServletRequest request,
            OAuthProvider provider
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (
                    provider.getCookieName()
                            .equals(cookie.getName())
            ) {
                return cookie.getValue();
            }
        }

        return null;
    }

    // 쿠키에 저장된 state와 콜백으로 반환된 state를 비교한다.
    public void validateState(
            String expectedState,
            String returnedState,
            OAuthProvider provider
    ) {
        if (
                expectedState == null ||
                        returnedState == null ||
                        expectedState.isBlank() ||
                        returnedState.isBlank()
        ) {
            throw invalidStateException(
                    provider
            );
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
            throw invalidStateException(
                    provider
            );
        }
    }

    // 로그인 제공자별 OAuth state 쿠키를 삭제한다.
    public void deleteStateCookie(
            HttpServletResponse response,
            OAuthProvider provider
    ) {
        ResponseCookie cookie =
                ResponseCookie
                        .from(
                                provider.getCookieName(),
                                ""
                        )
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite("Lax")
                        .path(
                                provider.getCookiePath()
                        )
                        .maxAge(Duration.ZERO)
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    private CustomException invalidStateException(
            OAuthProvider provider
    ) {
        return new CustomException(
                HttpStatus.BAD_REQUEST,
                provider.getInvalidStateErrorCode(),
                provider.getInvalidStateMessage()
        );
    }
}