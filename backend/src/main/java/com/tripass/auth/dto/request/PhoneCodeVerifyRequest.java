package com.tripass.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

//휴대전화 인증번호 확인 요청 DTO
@Getter
@NoArgsConstructor
public class PhoneCodeVerifyRequest{

    //인증번호 발송 시 서버가 발급한 요청 ID
    private String requestId;

    //인증 대상 휴대전화번호
    private String phoneNumber;

    //사용자가 문자로 받은 인증번호
    private String code;
}
