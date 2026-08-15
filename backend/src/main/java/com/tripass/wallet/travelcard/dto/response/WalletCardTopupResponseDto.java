package com.tripass.wallet.travelcard.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 트래블카드 외화 충전 완료 후 충전 금액, 적용 환율, 변경 잔액을 반환하는 DTO입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCardTopupResponseDto {

    private Long topupId;
    private Long walletId;
    private Long walletTravelCardId;
    private String currencyCode;
    private BigDecimal krwAmount;
    private BigDecimal foreignAmount;
    private BigDecimal baseExchangeRate;
    private BigDecimal appliedExchangeRate;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private String status;
    private String externalTransactionId;
    private String failureReason;
    private Integer retryCount;
    private BigDecimal walletBalanceAfter;
    private BigDecimal travelCardBalanceAfter;
    private Long walletLedgerId;
    private Long cardLedgerId;
    private LocalDateTime completedAt;
}
