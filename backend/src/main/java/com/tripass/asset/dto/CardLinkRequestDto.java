package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardLinkRequestDto {
    private String organizationCode; // "카드사 코드 (예: 0301 신한카드)"
    private String organizationName; // "카드사명"
    private String cardType;         // "CF" (카드 개인 로그인 요청 형식)
    private String loginType;        // "1"(아이디/비밀번호 로그인)
    private String loginId;
    private String password;
}
