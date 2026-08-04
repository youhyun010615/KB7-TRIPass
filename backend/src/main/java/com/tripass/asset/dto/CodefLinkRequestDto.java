package com.tripass.asset.dto;

import lombok.Getter;

@Getter
public class CodefLinkRequestDto {
    private String organizationCode; // "0004" (국민은행)
    private String businessType; //"BK" 은행
    private String loginType; // "0"(인터넷뱅킹)  / "1"(인증서)
    private String loginId;
    private String password;
}
