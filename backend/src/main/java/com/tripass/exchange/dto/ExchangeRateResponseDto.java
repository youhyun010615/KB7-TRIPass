package com.tripass.exchange.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ExchangeRateResponseDto {
    private String currencyCode;
    private String currencyName;
    private BigDecimal dealBaseRate;
    private BigDecimal prevRate;
    private BigDecimal changeAmount;
    private LocalDate rateDate;
}
