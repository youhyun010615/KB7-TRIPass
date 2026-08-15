package com.tripass.saving.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** monthly_saving_missions 테이블 매핑 DTO. */
@Getter
@Setter
public class MonthlySavingMissionDto {
    private Long id;
    private Long userId;
    private Long monthlySpendingAnalysisId;
    private Long missionCategorySelectionId;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String targetYearMonth;
    private Integer reductionRate;
    private Integer baselineSpendingAmount;
    private Integer monthlyReductionTarget;
    private Integer monthlyUsageTarget;
    private Integer plannedSavingAmount;
    private Integer startWeek;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
