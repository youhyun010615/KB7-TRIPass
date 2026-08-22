package com.tripass.wallet.dto.response;

import lombok.*;

/** 월렛에 연동된 트래블카드 정보를 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTravelCardResponseDto {

    private Long walletTravelCardId;
    private Long travelCardId;
    private String cardName;
    private String issuerName;
    private String maskedCardNumber;
    private String status;
}
