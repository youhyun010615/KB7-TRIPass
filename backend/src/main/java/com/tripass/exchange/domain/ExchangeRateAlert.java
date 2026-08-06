package com.tripass.exchange.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeRateAlert {
    private Long id;
    private Long userId;
    private Long currencyId;
    private Double targetRate;
    private Double targetAmount;
    private Boolean isDeleted;
}
