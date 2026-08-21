package com.tripass.ocr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CurrencyAmountResponse {

    private String currencyCode;
    private String currencySymbol;
    private BigDecimal amount;
}
