package com.tripass.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeEstimateDto {
    private BigDecimal inputAmount;
    private BigDecimal estimatedAmount;
    private BigDecimal buyRate;
    private BigDecimal buyFeeRate;
    private BigDecimal sellRate;
    private BigDecimal sellFeeRate;
    private BigDecimal baseRate;
    private Integer unit;
    private String currencyCode;
}
