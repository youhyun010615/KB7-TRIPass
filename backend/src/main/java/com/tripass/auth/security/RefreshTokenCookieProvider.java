package com.tripass.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

//Refresh Token HttpOnly 쿠키의 생성, 조회 및 삭제를 담당한다.
@Component
public class RefreshTokenCookieProvider {
    private static final String COOKIE_NAME = "refreshToken";

    private static final String COOKIE_PATH = "/api/v1/auth";

    private final JwtTokenProvider jwtTokenProvider;
    private final boolean secure;

    public RefreshTokenCookieProvider(
            JwtTokenProvider jwtTokenProvider,
            @Value("${auth.refresh-cookie.secure:true}")
            boolean secure
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.secure = secure;
    }
    //로그인 및 토큰 재발급 시 Refresh Token 쿠키를 생성
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie =
                ResponseCookie
                        .from(COOKIE_NAME, refreshToken)
                        .httpOnly(true)
                        .secure(secure)
                        .path(COOKIE_PATH)
                        .sameSite("Lax")
                        .maxAge(
                                Duration.ofSeconds(jwtTokenProvider.getRefreshExpirationSeconds()))
                        .build();
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }
    //요청 쿠키에서 RefreshToken 조회
    public String getRefreshToken(
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

    //로그아웃 시 Refresh Token 쿠키를 삭제한다.
    public void deleteRefreshTokenCookie(
            HttpServletResponse response
    ) {
        ResponseCookie cookie =
                ResponseCookie
                        .from(COOKIE_NAME, "")
                        .httpOnly(true)
                        .secure(secure)
                        .path(COOKIE_PATH)
                        .sameSite("Lax")
                        .maxAge(Duration.ZERO)
                        .build();
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }
}
