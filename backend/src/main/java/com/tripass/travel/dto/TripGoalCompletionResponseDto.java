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
public class TripGoalCompletionResponseDto {
    private Long tripId;
    private BigDecimal prepaidExpenseTotal;
    private BigDecimal localTravelTargetTotal;
    private BigDecimal currentWalletBalance;
    private Integer remainingMonths;
    private BigDecimal monthlySavingTarget;
    private List<CountryBudgetRecommendationResponseDto> countries;
}
