package com.tripass.saving.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * monthly_spending_analyses 테이블 매핑 DTO.
 */
@Getter
@Setter
public class MonthlySpendingAnalysisDto {

    private Long id;
    private Long userId;
    private String analysisYearMonth; // YYYY-MM (지난달)
    private String targetYearMonth;   // YYYY-MM (이번달, 미션 적용월)
    private BigDecimal totalSpending;
    private BigDecimal savingTargetAmount;
    private BigDecimal actualSavingAmount;
    private BigDecimal savingDifferenceAmount;
    private String savingResultMessage;
    private String reportStatus; // PENDING / VIEWED / CLOSED
    private LocalDateTime reportViewedAt;
    private LocalDateTime reportClosedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
