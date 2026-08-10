package com.tripass.exchange.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeRateAlertRequestDto {
    private String currencyCode;
    private Double targetRate;
    private Double targetAmount;
}
