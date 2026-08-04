package com.tripass.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

//일반 로그인 요청 DTO
@Getter
@NoArgsConstructor
public class LoginRequest {
    // 로그인 아이디
    private String loginId;

    //비밀번호
    private String password;
}
