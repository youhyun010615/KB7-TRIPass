package com.tripass.wallet.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 월렛 메인 화면에서 표시할 트래블카드 보유 외화와 원화 환산 금액 DTO입니다. */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletForeignBalanceResponseDto {

    private String currencyCode;
    private String currencyName;
    private String symbol;
    private BigDecimal balanceAmount;
    private BigDecimal krwEstimatedAmount;
}
