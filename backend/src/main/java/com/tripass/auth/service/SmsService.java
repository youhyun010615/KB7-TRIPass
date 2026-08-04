package com.tripass.auth.service;

//문자인증 문자 발송용 Service 인터페이스
public interface SmsService {
    //인증번호 발송
    void sendVerificationCode(String phoneNumber, String verificationCode);
}
