package com.tripass.auth.service;

// 인증번호 확인 실패 횟수 저장 기능
public interface PhoneVerificationAttemptService {
    // 인증번호 확인 실패 횟수를 별도 트랜잭션으로 증가
    void increaseFailedAttempt(String requestId);
}
