package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 트래블카드 통화별 외화 잔액과 원화 추정 금액을 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelCardCurrencyBalanceResponseDto {

    private String currencyCode;
    private BigDecimal balanceAmount;
    private BigDecimal krwEstimatedAmount;
}
