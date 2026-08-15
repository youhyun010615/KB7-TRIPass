package com.tripass.wallet.travelcard.dto.response;

import lombok.*;

/** 월렛에 연동된 트래블카드의 카드명, 카드사, 마스킹 번호, 연동 상태를 반환하는 DTO입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTravelCardResponseDto {

    private Long walletTravelCardId;
    private Long userTravelCardId;
    private Long travelCardId;
    private String cardName;
    private String issuerName;
    private String maskedCardNumber;
    private String status;
}
