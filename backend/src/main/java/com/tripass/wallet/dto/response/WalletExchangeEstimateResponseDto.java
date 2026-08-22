package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 환전 또는 재환전 전 예상 금액, 적용 환율, 수수료를 반환하는 응답 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletExchangeEstimateResponseDto {

    private String exchangeType;
    private String currencyCode;
    private BigDecimal krwAmount;
    private BigDecimal foreignAmount;
    private BigDecimal baseExchangeRate;
    private BigDecimal appliedExchangeRate;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
}
