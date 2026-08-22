package com.tripass.wallet.travelcard.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 트래블카드의 외화 충전·환전 원장과 실제 카드 승인내역을 하나의 타임라인으로 반환합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelCardTransactionResponseDto {

    private String transactionId;
    private String sourceType;
    private String transactionType;
    private String direction;
    private String currencyCode;
    private BigDecimal foreignAmount;
    private BigDecimal krwAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal appliedExchangeRate;
    private String merchantName;
    private String categoryName;
    private String countryName;
    private String memo;
    private LocalDateTime occurredAt;
}
