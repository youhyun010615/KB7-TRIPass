package com.tripass.wallet.travelcard.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 트래블카드에 충전된 통화별 외화 잔액을 담는 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelCardBalance {

    private Long id;
    private Long walletTravelCardId;
    private String currencyCode;
    private BigDecimal balanceAmount;
    private BigDecimal krwEstimatedAmount;
    private LocalDateTime updatedAt;
}
