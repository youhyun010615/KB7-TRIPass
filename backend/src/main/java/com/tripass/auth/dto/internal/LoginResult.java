package com.tripass.auth.dto.internal;

import com.tripass.auth.dto.response.LoginResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

//로그인 처리 결과를 Controller에 전달하는 내부 DTO,
// Refresh Token은 API 응답 JSON에 포함하지 않고 Controller에서 HttpOnly 쿠키로 전달한다.
@Getter
@AllArgsConstructor
public class LoginResult {

    // 클라이언트 응답 본문에 포함할 로그인 정보
    private LoginResponse loginResponse;

    // HttpOnly 쿠키로 전달할 Refresh Token
    private String refreshToken;
}