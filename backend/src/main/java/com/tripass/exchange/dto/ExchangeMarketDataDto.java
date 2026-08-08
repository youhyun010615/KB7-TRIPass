package com.tripass.exchange.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExchangeMarketDataDto {
    private Long id;
    private Long currencyId;
    private Integer unit;
    private BigDecimal baseRate;
    private BigDecimal buyRate;
    private BigDecimal buyFeeRate;
    private BigDecimal sellRate;
    private BigDecimal sellFeeRate;
}
