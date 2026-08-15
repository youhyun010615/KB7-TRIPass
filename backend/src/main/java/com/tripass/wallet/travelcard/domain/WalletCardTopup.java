package com.tripass.wallet.travelcard.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 월렛 원화를 차감해 트래블카드에 외화를 충전한 요청 상태를 담는 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCardTopup {

    private Long id;
    private Long walletId;
    private Long walletTravelCardId;
    private String currencyCode;
    private BigDecimal krwAmount;
    private BigDecimal foreignAmount;
    private String status;
    private String idempotencyKey;
    private String externalTransactionId;
    private Integer retryCount;
    private LocalDateTime lastTriedAt;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private String failureReason;
    private Long walletLedgerId;
    private Long cardLedgerId;
    private Boolean refunded;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
