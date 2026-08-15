package com.tripass.wallet.fx.dto.response;

import lombok.Getter;
import lombok.Setter;

/** 외화 충전 드롭다운에 표시할 통화 기본 정보를 반환하는 DTO입니다. */

@Getter
@Setter
public class WalletCurrencyResponseDto {

    private String currencyCode;
    private String currencyName;
    private String symbol;
    private Integer unit;
}
