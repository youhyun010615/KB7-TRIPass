package com.tripass.exchange.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeRateAlertRequestDto {
    private Long currencyId;
    private Double targetRate;
    private Double targetAmount;
}
