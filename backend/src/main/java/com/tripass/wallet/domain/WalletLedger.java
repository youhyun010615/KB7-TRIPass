package com.tripass.wallet.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 월렛의 모든 입금, 출금, 충전, 환불, 보정 내역을 기록하는 원장 도메인 객체입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletLedger {

    private Long id;
    private Long walletId;
    private Long tripId;
    private String direction;
    private String transactionType;
    private String transferMethod;
    private BigDecimal amount;
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