package com.tripass.auth.service;

import com.tripass.auth.dto.response.CheckLoginIdResponse;
import com.tripass.auth.dto.request.SignupRequest;
import com.tripass.auth.dto.response.SignupResponse;
import com.tripass.auth.dto.request.LoginRequest;
import com.tripass.auth.dto.internal.LoginResult;
import com.tripass.auth.dto.internal.TokenRefreshResult;
import com.tripass.auth.dto.request.FindIdRequest;
import com.tripass.auth.dto.request.ResetPasswordRequest;
import com.tripass.auth.dto.response.FindIdResponse;
import com.tripass.auth.dto.request.ChangePasswordRequest;
import com.tripass.auth.dto.request.KakaoLoginRequest;
import com.tripass.auth.dto.request.GoogleLoginRequest;


// 회원 가입 및 로그인 기능 정의하는 service의 인터페이스
public interface AuthService {

    //회원가입 아이디의 중복 체크
    CheckLoginIdResponse checkLoginId(String loginId);

    //인증 후 회원가입 처리
    SignupResponse signup(SignupRequest request);

    //일반 로그인 처리
    LoginResult login(LoginRequest request);

    String createKakaoAuthorizationUrl(String state);

    // 카카오 인가 코드를 이용한 소셜 로그인
    LoginResult kakaoLogin(KakaoLoginRequest request);

    // Google OAuth 인가 URL 생성
    String createGoogleAuthorizationUrl(String state);

    // Google 인가 코드를 이용한 소셜 로그인
    LoginResult googleLogin(GoogleLoginRequest request);

    // Refresh Token을 이용한 토큰 재발급
    TokenRefreshResult refreshToken(String refreshToken);

    // 현재 브라우저의 Refresh Token을 폐기한다.
    void logout(String refreshToken);

    // 휴대전화 인증을 기반으로 마스킹된 로그인 아이디 조회
    FindIdResponse findId(FindIdRequest request);

    // 휴대전화 인증을 기반으로 LOCAL 계정 비밀번호 재설정
    void resetPassword(ResetPasswordRequest request);

    // 로그인한 LOCAL 회원의 비밀번호 변경
    void changePassword(Long userId, ChangePasswordRequest request);
}
