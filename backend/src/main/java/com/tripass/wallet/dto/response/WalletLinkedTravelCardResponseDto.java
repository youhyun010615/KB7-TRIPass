package com.tripass.wallet.dto.response;

import lombok.*;

/** 월렛 메인 화면에서 표시할 연동 트래블카드 정보를 반환하는 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletLinkedTravelCardResponseDto {

    private Boolean linked;
    private Long walletTravelCardId;
    private Long userTravelCardId;
    private Long travelCardId;
    private String cardName;
    private String issuerName;
    private String maskedCardNumber;
}
