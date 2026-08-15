package com.tripass.wallet.travelcard.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 트래블카드에 보유 중인 통화별 외화 잔액과 원화 추정 금액을 반환하는 DTO입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelCardCurrencyBalanceResponseDto {

    private String currencyCode;
    private BigDecimal balanceAmount;
    private BigDecimal krwEstimatedAmount;
}
