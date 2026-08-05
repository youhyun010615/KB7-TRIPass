package com.tripass.exchange.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ExchangeRateHistoryResponseDto {
    private String currencyCode;
    private String currencyName;
    private List<RateInfo> rates;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RateInfo {
        private LocalDate rateDate;
        private BigDecimal dealBaseRate;
    }
}
