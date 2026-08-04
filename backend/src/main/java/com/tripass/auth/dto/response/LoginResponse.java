package com.tripass.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

//일반 로그인 성공 응답 DTO
@AllArgsConstructor
@Getter
public class LoginResponse {
    //API 인증에 사용할  JWT Access Token
    private String accessToken;
    //Authorization 헤더에서 사용하는 토큰 형식
    private String tokenType;
    //Access Token 만료까지 남은 시간 - 단위: 초
    private long expiresIn;
    //로그인 응답에 포함할 최소 회원 정보
    @Getter
    @AllArgsConstructor
    public static class UserInfo{
        //회원 PK
        private Long id;
        //로그인 아이디
        private String loginId;
        //회원 이름
        private String name;
        //로그인 제공자
        private String loginProvider;
    }

}
