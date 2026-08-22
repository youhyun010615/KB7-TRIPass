package com.tripass.auth.service;
import com.tripass.auth.dto.request.PhoneCodeSendRequest;
import com.tripass.auth.dto.request.PhoneCodeVerifyRequest;
import com.tripass.auth.dto.response.PhoneCodeSendResponse;

//휴대전화 인증 기능을 정의하는 Service 인터페이스
public interface PhoneVerificationService {

    //인증번호 생성 후 문자로 발송
    PhoneCodeSendResponse sendVerificationCode(PhoneCodeSendRequest request);

    //사용자가 입력한 인증번호 확인
    void verifyCode(PhoneCodeVerifyRequest request);

    //회원가입 이전 번호인증 완료 여부 조회
    void validateSignupVerification(String requestId, String phoneNumber);

    //사용된 인증 결과를 사용 완료 처리한다.
    void markVerificationAsUsed(String requestId);

    // 아이디 찾기용 인증 결과 검증
    void validateFindIdVerification(String requestId, String phoneNumber);

    // 비밀번호 재설정용 인증 결과 검증
    void validateResetPasswordVerification(String requestId, String phoneNumber);
}
