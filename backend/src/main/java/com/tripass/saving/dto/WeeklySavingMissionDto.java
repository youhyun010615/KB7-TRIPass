package com.tripass.saving.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** weekly_saving_missions 테이블 매핑 DTO. */
@Getter
@Setter
public class WeeklySavingMissionDto {
    private Long id;
    private Long monthlySavingMissionId;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Integer weekNumber;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Integer weeklyUsageLimit;
    private Integer weeklyExpectedSaving;
    private Integer eligibleDayCount;
    private Integer actualSpending;
    private Integer actualSaving;
    private Integer rewardAmount;
    private Long walletLedgerId;
    private String status;
    private LocalDateTime evaluatedAt;
    private LocalDateTime rewardedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
