package com.tripass.travel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripHomeDashboardResponseDto {
    private Long tripId;
    private String tripName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long daysUntilDeparture;
    private BigDecimal totalTargetAmount;
    private BigDecimal prepaidExpenseTotal;
    private BigDecimal walletBalance;
    private BigDecimal remainingTargetAmount;
    private BigDecimal savingProgressPercent;
    private Integer remainingMonths;
    private BigDecimal monthlySavingTarget;
    private BigDecimal currentMonthSaving;
    private BigDecimal currentMonthRemaining;
    private Integer currentMonthSavingPercent;
    private List<TripHomeCountryResponseDto> countries;
}
