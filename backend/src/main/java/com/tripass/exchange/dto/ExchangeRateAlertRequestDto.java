package com.tripass.exchange.dto;

import lombok.Data;

@Data
public class ExchangeRateAlertRequestDto {
    private Long id;
    private Long currencyId;
    private Double targetRate;
    private Double targetAmount;
}
