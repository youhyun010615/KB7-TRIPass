package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryBudgetBaselineDto {
    private Long countryId;
    private BigDecimal roundTripAirfare;
    private BigDecimal lodgingPerNight;
    private BigDecimal foodPerDay;
    private BigDecimal activityPerDay;
    private BigDecimal transportPerDay;
    private BigDecimal miscPerDay;
    private String dataSource;
    private LocalDate referenceDate;
}
