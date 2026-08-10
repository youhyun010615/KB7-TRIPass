package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardLinkRequestDto {
    private String organizationCode; // "카드사 코드 (예: 0301 신한카드)"
    private String organizationName; // "카드사명"
    private String cardType;         // "CF" (CODEF 카드 업권 코드)
    private String loginType;        // "0"(인터넷뱅킹) / "1"(인증서)
    private String loginId;
    private String password;
}
