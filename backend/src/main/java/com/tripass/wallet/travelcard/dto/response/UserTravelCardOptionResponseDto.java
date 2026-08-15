package com.tripass.wallet.travelcard.dto.response;

import lombok.*;

/** 사용자가 보유한 트래블카드 정보를 카드 연동 후보로 반환하는 DTO입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTravelCardOptionResponseDto {

    private Long userTravelCardId;
    private Long travelCardId;
    private String cardName;
    private String issuerName;
    private String maskedCardNumber;
    private String brandName;
    private String cardColor;
}
