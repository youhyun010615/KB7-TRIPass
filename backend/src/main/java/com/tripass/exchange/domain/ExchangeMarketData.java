package com.tripass.exchange.domain;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ToString
@Data
public class ExchangeMarketData {
    private Long id;
    private Long currencyId;
    private Integer unit;
    private BigDecimal baseRate;
    private BigDecimal buyRate;
    private BigDecimal buyFeeRate;
    private BigDecimal sellRate;
    private BigDecimal sellFeeRate;
    private LocalDateTime fetchedAt;
}
