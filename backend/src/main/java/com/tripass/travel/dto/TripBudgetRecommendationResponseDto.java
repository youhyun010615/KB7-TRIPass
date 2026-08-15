package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripBudgetRecommendationResponseDto {
    private Long tripId;
    private BigDecimal prepaidExpenseTotal;
    private BigDecimal localTravelTargetTotal;
    private List<CountryBudgetRecommendationResponseDto> countries;
}
