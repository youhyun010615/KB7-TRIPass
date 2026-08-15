package com.tripass.wallet.fx.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 환전 처리 완료 후 환전 거래와 변경된 월렛·트래블카드 잔액을 반환하는 DTO입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletExchangeResponseDto {

    private Long exchangeTransactionId;
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
    private BigDecimal walletBalanceAfter;
    private BigDecimal travelCardBalanceAfter;
    private Long walletLedgerId;
    private Long cardLedgerId;
    private LocalDateTime completedAt;
}
