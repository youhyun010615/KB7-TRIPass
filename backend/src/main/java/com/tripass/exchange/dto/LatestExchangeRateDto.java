package com.tripass.exchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LatestExchangeRateDto {
    private String currencyCode;
    private String currencyName;
    private BigDecimal dealBaseRate;
    private BigDecimal prevRate;
    private BigDecimal changeAmount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate rateDate;
}
