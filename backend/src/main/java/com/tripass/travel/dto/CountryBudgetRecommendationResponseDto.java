package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryBudgetRecommendationResponseDto {
    private Long tripCountryId;
    private String countryName;
    private String currencyCode;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private BigDecimal recommendedAirfareAmount;
    private BigDecimal recommendedLodgingAmount;
    private BigDecimal recommendedActivityAmount;
    private BigDecimal recommendedTransportAmount;
    private BigDecimal recommendedFoodAmount;
    private BigDecimal recommendedOtherAmount;
    private BigDecimal confirmedAirfareAmount;
    private BigDecimal confirmedLodgingAmount;
    private BigDecimal confirmedActivityAmount;
    private BigDecimal confirmedTransportAmount;
    private BigDecimal confirmedFoodAmount;
    private BigDecimal confirmedOtherAmount;
    private String aiReason;
    private Boolean isConfirmed;
}
