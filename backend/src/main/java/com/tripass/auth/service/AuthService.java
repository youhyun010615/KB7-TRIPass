package com.tripass.auth.service;

import com.tripass.auth.dto.response.CheckLoginIdResponse;
import com.tripass.auth.dto.request.SignupRequest;
import com.tripass.auth.dto.response.SignupResponse;

// 회원 가입 및 로그인 기능 정의하는 service의 인터페이스
public interface AuthService {
    //회원가입 아이디의 중복 체크
    CheckLoginIdResponse checkLoginId(String loginId);
    //인증 후 회원가입 처리
    SignupResponse signup(SignupRequest request);
}
