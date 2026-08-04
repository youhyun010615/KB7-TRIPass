package com.tripass.exchange.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ExchangeRateHistoryResponseDto {
    private String currencyCode;
    private String currencyName;
    private List<RateInfo> rates;

    @Getter
    @Builder
    public static class RateInfo {
        private LocalDate rateDate;
        private BigDecimal dealBaseRate;
    }
}
