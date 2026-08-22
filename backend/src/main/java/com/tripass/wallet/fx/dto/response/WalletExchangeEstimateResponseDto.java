package com.tripass.wallet.fx.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 환전 예상 원화 금액, 외화 금액, 적용 환율과 수수료를 반환하는 DTO입니다. */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
