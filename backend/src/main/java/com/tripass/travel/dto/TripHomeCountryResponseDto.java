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
public class TripHomeCountryResponseDto {
    private Long tripCountryId;
    private Long countryId;
    private String countryName;
    private String currencyCode;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private Integer displayOrder;
    private BigDecimal targetBudget;
    private BigDecimal targetSharePercent;
}
