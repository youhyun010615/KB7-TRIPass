package com.tripass.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

//일반 회원가입 요청 DTO
@Getter
@NoArgsConstructor
public class SignupRequest {

    // 회원 이름
    private String name;

    // 로그인 아이디
    private String loginId;

    // 비밀번호
    private String password;

    // 휴대전화번호
    private String phoneNumber;

    // 휴대전화 인증번호 발송 시 서버가 발급한 요청 ID
    private String phoneVerificationRequestId;
}
