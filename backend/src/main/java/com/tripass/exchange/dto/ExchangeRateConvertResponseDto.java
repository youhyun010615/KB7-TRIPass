package com.tripass.exchange.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExchangeRateConvertResponseDto {
    private double fromAmount;
    private String fromCurrency;
    private double toAmount;
    private String toCurrency;
    private double appliedRate;
}
