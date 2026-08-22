package com.tripass.wallet.travelcard.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 트래블카드 외화 충전, 차감, 환불, 보정 내역을 기록하는 외화 원장 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelCardLedger {

    private Long id;
    private Long walletTravelCardId;
    private String currencyCode;
    private String direction;
    private String transactionType;
    private BigDecimal foreignAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String sourceType;
    private Long sourceId;
    private String targetType;
    private Long targetId;
    private String idempotencyKey;
    private String memo;
    private LocalDateTime createdAt;
}
