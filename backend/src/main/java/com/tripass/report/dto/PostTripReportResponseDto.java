package com.tripass.report.dto;

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
public class PostTripReportResponseDto {
    private Long tripId;
    private String tripName;
    private List<String> countryNames;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;

    private BigDecimal targetBudget;
    private BigDecimal spent;
    private BigDecimal remaining;
    private BigDecimal dailyAverage;
    private BigDecimal savingsRate;

    private List<DailySpendingDto> dailySpending;
    private List<CategorySpendingDto> categorySpending;
    private List<CountrySpendingDto> countrySpending;
    private List<CountryTopCategoryDto> countryTopCategories;

    private Integer receiptCount;
    private BigDecimal nextTripMonthlySuggestion;
    private Integer nextTripMonths;
}
