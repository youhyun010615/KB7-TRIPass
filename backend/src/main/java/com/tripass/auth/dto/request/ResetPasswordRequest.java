package com.tripass.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

    // 비밀번호를 재설정할 LOCAL 로그인 아이디
    private String loginId;

    // RESET_PASSWORD 목적으로 인증한 휴대전화번호
    private String phoneNumber;

    // 휴대전화 인증번호 발송 시 발급된 요청 ID
    private String phoneVerificationRequestId;

    // 새로 설정할 비밀번호
    private String newPassword;
}