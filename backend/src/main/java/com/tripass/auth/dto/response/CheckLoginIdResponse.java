package com.tripass.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

//일반 로그인 아이디 중복 확인 응답 DTO
@Getter
@AllArgsConstructor
public class CheckLoginIdResponse {
    //아이디 사용 가능 여부
    private boolean available;
}
