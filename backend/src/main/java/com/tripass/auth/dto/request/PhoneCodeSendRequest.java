package com.tripass.auth.dto.request;

import com.tripass.auth.model.VerificationPurpose;
import lombok.Getter;
import lombok.NoArgsConstructor;

//휴대전화 인증번호 발송 요청 DTO
@Getter
@NoArgsConstructor
public class PhoneCodeSendRequest {
    //인증번호 받을 휴대전화번호
    private String phoneNumber;
    //휴대전화 인증 목적
    private VerificationPurpose purpose;
}
