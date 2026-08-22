package com.tripass.saving.dto;

import java.util.List;

public record MonthlyMissionResponseDto(
        Long id,
        Long categoryId,
        String categoryCode,
        String categoryName,
        Integer reductionRate,
        Integer baselineSpendingAmount,
        Integer monthlyReductionTarget,
        Integer monthlyUsageTarget,
        Integer plannedSavingAmount,
        Integer rewardAmount,
        Integer startWeek,
        String status,
        List<WeeklyMissionResponseDto> weeklyMissions
) {
}
