package com.tripass.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindIdRequest {

    // 가입 시 등록한 회원 이름
    private String name;

    // FIND_ID 목적으로 인증한 휴대전화번호
    private String phoneNumber;

    // 휴대전화 인증번호 발송 시 발급된 요청 ID
    private String phoneVerificationRequestId;
}