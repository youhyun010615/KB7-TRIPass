package com.tripass.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

//일반 회원가입 성공 응답 DTO
@Getter
@AllArgsConstructor
public class SignupResponse {
    //회원가입으로 생성된 회원 pk
    private Long userid;
}
