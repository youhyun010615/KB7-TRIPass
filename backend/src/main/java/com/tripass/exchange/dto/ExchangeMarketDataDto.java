package com.tripass.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
