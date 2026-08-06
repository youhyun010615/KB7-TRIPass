package com.tripass.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

//휴대전화 인증 정보를 표시하는 모델
@Getter
@Setter
@NoArgsConstructor
public class PhoneVerification {
    //휴대전화 인증 PK
    private Long id;
    //외부에 전달할 인증 요청 식별값
    private String requestId;
    //인증 대상 휴대전화번호
    private String phoneNumber;
    //인증 목적
    private VerificationPurpose verificationPurpose;
    //해시한 인증번호
    private String verificationCodeHash;
    //인증번호 만료일시
    private Date expiresAt;
    //인증 성공일자
    private Date verifiedAt;
    //인증 결과 사용일시
    private Date usedAt;
    //인증 결과 확인 실패 횟수
    private int attemptCount;
    //인증번호 요청 생성일시
    private Date createdAt;
    // 수정일시
    private Date updatedAt;
}
