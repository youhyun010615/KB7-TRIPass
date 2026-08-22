package com.tripass.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

//Access Token 재발급 응답 DTO
@Getter
@AllArgsConstructor
public class TokenRefreshResponse {

    // 새로 발급한 Access Token
    private String accessToken;

    // Authorization 헤더에서 사용하는 토큰 형식
    private String tokenType;

    // Access Token 만료까지 남은 시간(초)
    private long expiresIn;
}
