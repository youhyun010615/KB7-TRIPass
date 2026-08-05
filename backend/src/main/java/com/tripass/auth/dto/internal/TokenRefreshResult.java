package com.tripass.auth.dto.internal;

import com.tripass.auth.dto.response.TokenRefreshResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

//토큰 재발급 처리 결과를 Controller에 전달하는 DTO

@Getter
@AllArgsConstructor
public class TokenRefreshResult {

    // JSON 응답으로 반환할 새 Access Token 정보
    private TokenRefreshResponse tokenRefreshResponse;

    // HttpOnly 쿠키로 전달할 새 Refresh Token
    private String refreshToken;
}
