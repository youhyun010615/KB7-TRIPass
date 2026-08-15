package com.tripass.wallet.fx.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 월렛 원화와 트래블카드 외화 사이 환전 거래의 적용 환율, 수수료, 상태를 담는 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletExchangeTransaction {

    private Long id;
    private Long walletId;
    private Long walletTravelCardId;
    private String currencyCode;
    private String exchangeType;
    private BigDecimal krwAmount;
    private BigDecimal foreignAmount;
    private BigDecimal baseExchangeRate;
    private BigDecimal appliedExchangeRate;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private String failureReason;
    private Long walletLedgerId;
    private Long cardLedgerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
