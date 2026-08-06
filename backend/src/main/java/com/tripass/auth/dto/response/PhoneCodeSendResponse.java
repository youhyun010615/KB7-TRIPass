package com.tripass.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

//휴대전화 인증번호 발송 응답 DTO
@Getter
@AllArgsConstructor
public class PhoneCodeSendResponse {
    //서버에서 생성한 인증 요청 식별값, 인증번호 확인과 회원가입 요청에서 다시 사용
    private String requestId;
    //인증번호 만료까지 남은 시간 - 단위: 초
    private int expireInSeconds;


}
