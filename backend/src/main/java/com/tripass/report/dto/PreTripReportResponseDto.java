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
public class PreTripReportResponseDto {
    private Long tripId;
    private String tripName;
    private List<String> countryNames;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long daysUntilTrip;

    private BigDecimal targetBudget;
    private BigDecimal securedFund;
    private Integer savingsPercent;
    private List<SavingHistoryDto> savingHistory;

    private List<ReportCountryBudgetDto> countryBudgets;

    private Integer checklistCompleted;
    private Integer checklistTotal;
    private Integer scheduleCount;
    private Integer prepaidScheduleCount;
    private Integer onsiteScheduleCount;
}
