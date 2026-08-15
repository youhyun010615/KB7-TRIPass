package com.tripass.wallet.travelcard.domain;

import lombok.*;

import java.time.LocalDateTime;

/** 월렛과 사용자가 선택한 트래블카드의 연동 정보를 담는 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTravelCard {

    private Long id;
    private Long walletId;
    private Long userTravelCardId;
    private Long travelCardId;
    private String cardName;
    private String issuerName;
    private String maskedCardNumber;
    private String status;
    private LocalDateTime linkedAt;
    private LocalDateTime unlinkedAt;
}
